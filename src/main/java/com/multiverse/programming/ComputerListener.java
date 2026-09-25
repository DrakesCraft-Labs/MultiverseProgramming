// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class ComputerListener implements Listener {

    private record BlockRef(boolean advanced, Location location) {
    }

    private record RunningEntry(UUID player, LuaRunner.LuaProgram program, BukkitTask cleanup) {
    }

    private final MultiverseProgrammingPlugin plugin;
    private Material computerBlock;
    private Material advancedComputerBlock;

    private static final Map<Location, ItemStack> advancedDisks = new HashMap<>();
    private static final Map<Location, RunningEntry> runningPrograms = new HashMap<>();
    private static final Map<UUID, BlockRef> openByPlayer = new HashMap<>();
    private static final Map<UUID, Long> lastClickTime = new HashMap<>();

    public ComputerListener(MultiverseProgrammingPlugin plugin, Material computerBlock, Material advancedComputerBlock) {
        this.plugin = plugin;
        this.computerBlock = computerBlock;
        this.advancedComputerBlock = advancedComputerBlock;
    }

    public void updateMaterials(Material computerBlock, Material advancedComputerBlock) {
        this.computerBlock = computerBlock;
        this.advancedComputerBlock = advancedComputerBlock;
    }

    public static void cancelAll() {
        for (RunningEntry entry : runningPrograms.values()) {
            entry.program().cancel();
            entry.cleanup().cancel();
        }
        runningPrograms.clear();
        openByPlayer.clear();
        lastClickTime.clear();
    }

    public static Map<Location, ItemStack> getAdvancedDisks() {
        return advancedDisks;
    }

    public boolean isComputerBlock(Material mat) {
        return mat != null && (mat == computerBlock || mat == advancedComputerBlock);
    }

    @EventHandler
    public void onBlockUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Player player = event.getPlayer();
        boolean advanced;
        if (block.getType() == computerBlock) {
            advanced = false;
            if (!plugin.getConfigManager().isEnableComputer()) {
                event.setCancelled(true);
                player.sendMessage(plugin.getPrefix() + " §cStandard computers are currently disabled by the server administration.");
                return;
            }
        } else if (block.getType() == advancedComputerBlock) {
            advanced = true;
            if (!plugin.getConfigManager().isEnableAdvancedComputer()) {
                event.setCancelled(true);
                player.sendMessage(plugin.getPrefix() + " §cAdvanced computers are currently disabled by the server administration.");
                return;
            }
        } else {
            return;
        }

        event.setCancelled(true);

        if (!player.hasPermission("multiverseprogramming.use")) {
            player.sendMessage(plugin.getPrefix() + " §cYou don't have permission to use computers.");
            return;
        }

        Location blockLoc = toBlockLocation(block.getLocation());
        openByPlayer.put(player.getUniqueId(), new BlockRef(advanced, blockLoc));

        if (!advanced) {
            player.openInventory(ComputerGUI.open());
            return;
        }

        boolean isRunning = isProgramRunning(blockLoc);
        ItemStack savedDisk = advancedDisks.get(blockLoc);
        Inventory inv = ComputerGUI.openAdvanced(isRunning);
        if (savedDisk != null && !savedDisk.getType().isAir()) {
            inv.setItem(ComputerGUI.DISK_SLOT, savedDisk);
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        boolean advanced = ComputerGUI.ADVANCED_TITLE.equals(title);
        if (!advanced && !ComputerGUI.TITLE.equals(title)) {
            return;
        }

        ClickType click = event.getClick();
        if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT
                || click == ClickType.DOUBLE_CLICK || click == ClickType.NUMBER_KEY
                || click == ClickType.SWAP_OFFHAND) {
            event.setCancelled(true);
            return;
        }

        int raw = event.getRawSlot();
        if (raw < 0 || raw >= 9) {
            return;
        }

        event.setCancelled(true);

        if (raw == ComputerGUI.DISK_SLOT) {
            ItemStack cursor = event.getCursor();
            ItemStack current = event.getCurrentItem();
            boolean cursorIsDisk = DiskManager.isDisk(cursor);
            boolean slotIsDisk = current != null && DiskManager.isDisk(current);
            boolean cursorEmpty = cursor == null || cursor.getType().isAir();

            if (cursorIsDisk || (cursorEmpty && slotIsDisk)) {
                event.setCancelled(false);
                return;
            }

            if (!cursorEmpty) {
                player.sendMessage(plugin.getPrefix() + " §cOnly a " + DiskManager.NAME + " can be inserted here.");
            }
        } else if (raw == ComputerGUI.BUTTON_SLOT) {
            long now = System.currentTimeMillis();
            long delay = plugin.getConfigManager().getPreventSpamDelayMs();
            Long last = lastClickTime.get(player.getUniqueId());
            if (last != null && (now - last) < delay) {
                return;
            }
            lastClickTime.put(player.getUniqueId(), now);

            BlockRef ref = openByPlayer.get(player.getUniqueId());
            if (ref == null || ref.advanced() != advanced) {
                return;
            }
            if (advanced) {
                pressAdvanced(player, event.getInventory(), ref.location());
            } else {
                ComputerGUI.pressButton(plugin, player, event.getInventory(), ref.location(), plugin.getPrefix(), plugin.getTimeoutMs());
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (!ComputerGUI.TITLE.equals(title) && !ComputerGUI.ADVANCED_TITLE.equals(title)) {
            return;
        }
        for (int raw : event.getRawSlots()) {
            if (raw >= 0 && raw < 9) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        boolean advanced = ComputerGUI.ADVANCED_TITLE.equals(title);
        if (!advanced && !ComputerGUI.TITLE.equals(title)) {
            return;
        }

        BlockRef ref = openByPlayer.remove(player.getUniqueId());
        ItemStack disk = event.getInventory().getItem(ComputerGUI.DISK_SLOT);

        if (advanced) {
            if (ref != null) {
                if (disk == null || disk.getType().isAir()) {
                    advancedDisks.remove(ref.location());
                } else {
                    advancedDisks.put(ref.location(), disk.clone());
                }
            }
            return;
        }

        if (disk == null || disk.getType().isAir()) {
            return;
        }
        event.getInventory().setItem(ComputerGUI.DISK_SLOT, null);

        Map<Integer, ItemStack> overflow = player.getInventory().addItem(disk);
        for (ItemStack rest : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), rest);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        openByPlayer.remove(uuid);
        lastClickTime.remove(uuid);

        List<Location> toStop = new ArrayList<>();
        for (Map.Entry<Location, RunningEntry> e : runningPrograms.entrySet()) {
            if (e.getValue().player().equals(uuid)) {
                toStop.add(e.getKey());
            }
        }
        for (Location loc : toStop) {
            RunningEntry entry = runningPrograms.get(loc);
            if (entry != null) {
                stopEntry(loc, entry);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        handleBlockRemoved(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            handleBlockRemoved(block);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            handleBlockRemoved(block);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        handleBlockRemoved(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if (isComputerBlock(b.getType())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            if (isComputerBlock(b.getType())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        World world = event.getWorld();
        List<Location> toRemove = new ArrayList<>();
        for (Map.Entry<Location, RunningEntry> entry : runningPrograms.entrySet()) {
            if (world.equals(entry.getKey().getWorld())) {
                toRemove.add(entry.getKey());
            }
        }
        for (Location loc : toRemove) {
            RunningEntry entry = runningPrograms.remove(loc);
            if (entry != null) {
                entry.program().cancel();
                entry.cleanup().cancel();
            }
        }
        advancedDisks.keySet().removeIf(loc -> world.equals(loc.getWorld()));
    }

    public static Location toBlockLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public static boolean isProgramRunning(Location loc) {
        if (loc == null) return false;
        Location blockLoc = toBlockLocation(loc);
        RunningEntry entry = runningPrograms.get(blockLoc);
        return entry != null && entry.program().isRunning();
    }

    private void handleBlockRemoved(Block block) {
        if (block == null) {
            return;
        }
        Location loc = toBlockLocation(block.getLocation());
        RunningEntry running = runningPrograms.remove(loc);
        if (running != null) {
            running.program().cancel();
            running.cleanup().cancel();
        }

        ItemStack disk = advancedDisks.remove(loc);
        if (disk != null && !disk.getType().isAir() && plugin.getConfigManager().isDropDisksOnBreak()) {
            block.getWorld().dropItemNaturally(block.getLocation(), disk);
        }

        if (block.getType() == plugin.getConfigManager().getMonitorBlock()) {
            com.multiverse.programming.peripheral.MonitorPeripheral.removeDisplayAt(loc);
        }
    }

    private void stopEntry(Location loc, RunningEntry entry) {
        entry.program().cancel();
        entry.cleanup().cancel();
        runningPrograms.remove(toBlockLocation(loc));
    }

    private void pressAdvanced(Player player, Inventory inv, Location loc) {
        Location blockLoc = toBlockLocation(loc);
        RunningEntry running = runningPrograms.get(blockLoc);
        if (running != null && running.program().isRunning()) {
            stopEntry(blockLoc, running);
            player.closeInventory();
            player.sendMessage(plugin.getPrefix() + " §7Program stopped.");
            return;
        }

        ItemStack disk = inv.getItem(ComputerGUI.DISK_SLOT);
        if (disk == null || disk.getType().isAir()) {
            player.sendMessage(plugin.getPrefix() + " §cThere is no floppy disk in the slot.");
            return;
        }

        String code = DiskManager.readProgram(disk);
        String error = LuaRunner.validate(code);
        if (error != null) {
            player.closeInventory();
            player.sendMessage(plugin.getPrefix() + " §cError in the code:");
            for (String line : error.split("\n")) {
                player.sendMessage(" §4✘ " + line);
            }
            return;
        }

        player.closeInventory();
        player.sendMessage(plugin.getPrefix() + " §7Program started.");

        UUID uuid = player.getUniqueId();
        Consumer<String> onLine = line -> Bukkit.getScheduler().runTask(plugin, () -> {
            Player target = Bukkit.getPlayer(uuid);
            if (target != null && target.isOnline()) {
                target.sendMessage("§f" + line);
            }
        });

        int maxLines = plugin.getConfigManager().getMaxStreamLines();
        LuaRunner.LuaProgram program = LuaRunner.runStreaming(plugin, blockLoc, true, code, plugin.getAdvancedTimeoutMs(), maxLines, onLine);

        BukkitTask[] cleanup = new BukkitTask[1];
        cleanup[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!program.isRunning()) {
                RunningEntry entry = runningPrograms.get(blockLoc);
                if (entry != null && entry.program() == program) {
                    runningPrograms.remove(blockLoc);
                }
                cleanup[0].cancel();
            }
        }, 1L, 20L);
        runningPrograms.put(blockLoc, new RunningEntry(uuid, program, cleanup[0]));
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        for (com.multiverse.programming.peripheral.NpcPeripheral npc : com.multiverse.programming.peripheral.NpcPeripheral.getActiveNpcs()) {
            Location loc = npc.getLocation();
            if (loc != null && loc.getWorld() != null && loc.getWorld().equals(player.getWorld())) {
                if (loc.distanceSquared(player.getLocation()) <= 256.0) {
                    npc.recordPlayerResponse(player.getName(), message);
                }
            }
        }
    }
}
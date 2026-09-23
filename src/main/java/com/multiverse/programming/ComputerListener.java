// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
    private final Material computerBlock;
    private final Material advancedComputerBlock;

    private static final Map<Location, ItemStack> advancedDisks = new HashMap<>();
    private static final Map<Location, RunningEntry> runningPrograms = new HashMap<>();
    private static final Map<UUID, BlockRef> openByPlayer = new HashMap<>();

    public ComputerListener(MultiverseProgrammingPlugin plugin, Material computerBlock, Material advancedComputerBlock) {
        this.plugin = plugin;
        this.computerBlock = computerBlock;
        this.advancedComputerBlock = advancedComputerBlock;
    }

    public static void cancelAll() {
        for (RunningEntry entry : runningPrograms.values()) {
            entry.program().cancel();
            entry.cleanup().cancel();
        }
        runningPrograms.clear();
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

        boolean advanced;
        if (block.getType() == computerBlock) {
            advanced = false;
        } else if (block.getType() == advancedComputerBlock) {
            advanced = true;
        } else {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        openByPlayer.put(player.getUniqueId(), new BlockRef(advanced, block.getLocation()));

        if (!advanced) {
            player.openInventory(ComputerGUI.open());
            return;
        }

        ItemStack savedDisk = advancedDisks.get(block.getLocation());
        Inventory inv = ComputerGUI.openAdvanced();
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
                || click == ClickType.DOUBLE_CLICK || click == ClickType.NUMBER_KEY) {
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
            BlockRef ref = openByPlayer.get(player.getUniqueId());
            if (ref == null || ref.advanced() != advanced) {
                return;
            }
            if (advanced) {
                pressAdvanced(player, event.getInventory(), ref.location());
            } else {
                ComputerGUI.pressButton(player, event.getInventory(), plugin.getPrefix(), plugin.getTimeoutMs());
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
            if (disk == null || disk.getType().isAir()) {
                advancedDisks.remove(ref != null ? ref.location() : null);
            } else if (ref != null) {
                advancedDisks.put(ref.location(), disk.clone());
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

    private void stopEntry(Location loc, RunningEntry entry) {
        entry.program().cancel();
        entry.cleanup().cancel();
        runningPrograms.remove(loc);
    }

    private void pressAdvanced(Player player, Inventory inv, Location loc) {
        RunningEntry running = runningPrograms.get(loc);
        if (running != null && running.program().isRunning()) {
            stopEntry(loc, running);
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
            if (target != null) {
                target.sendMessage("§f" + line);
            }
        });

        LuaRunner.LuaProgram program = LuaRunner.runStreaming(code, plugin.getAdvancedTimeoutMs(), onLine);

        BukkitTask[] cleanup = new BukkitTask[1];
        cleanup[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!program.isRunning()) {
                RunningEntry entry = runningPrograms.get(loc);
                if (entry != null && entry.program() == program) {
                    runningPrograms.remove(loc);
                }
                cleanup[0].cancel();
            }
        }, 0L, 20L);
        runningPrograms.put(loc, new RunningEntry(uuid, program, cleanup[0]));
    }
}
// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.turtle;

import com.multiverse.programming.DiskManager;
import com.multiverse.programming.LuaRunner;
import com.multiverse.programming.MultiverseProgrammingPlugin;
import com.multiverse.programming.peripheral.PeripheralManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import java.util.Objects;

/**
 * Handles Bukkit events for Turtle placement, interaction, inventory GUI, and breaking.
 */
public final class TurtleListener implements Listener {

    private final MultiverseProgrammingPlugin plugin;

    public TurtleListener(MultiverseProgrammingPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
    }

    private Material getTurtleBlock() {
        return plugin.getConfigManager().getTurtleBlock();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTurtlePlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        ItemMeta meta = item.getItemMeta();
        Material turtleMat = getTurtleBlock();

        if (event.getBlockPlaced().getType() != turtleMat) {
            return;
        }

        boolean isTurtleItem = meta != null && meta.hasDisplayName()
                && meta.getDisplayName().contains(DiskManager.TURTLE_NAME);

        if (!isTurtleItem && item.getType() != turtleMat) {
            return;
        }

        Player player = event.getPlayer();
        BlockFace facing = BlockFace.NORTH;
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) yaw += 360;
        if (yaw >= 315 || yaw < 45) {
            facing = BlockFace.SOUTH;
        } else if (yaw >= 45 && yaw < 135) {
            facing = BlockFace.WEST;
        } else if (yaw >= 135 && yaw < 225) {
            facing = BlockFace.NORTH;
        } else {
            facing = BlockFace.EAST;
        }

        Turtle turtle = plugin.getTurtleManager().createTurtle(
                event.getBlockPlaced().getLocation(),
                facing,
                player.getUniqueId()
        );

        player.sendMessage(plugin.getPrefix() + " §aTurtle §e" + turtle.getId() + " §aplaced! Right-click to open its control panel.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTurtleInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != getTurtleBlock()) {
            return;
        }

        Turtle turtle = plugin.getTurtleManager().getTurtle(block.getLocation());
        if (turtle == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!player.hasPermission("multiverseprogramming.turtle")) {
            player.sendMessage(plugin.getPrefix() + " §cYou don't have permission to use Turtles.");
            return;
        }

        TurtleGUI gui = new TurtleGUI(turtle);
        player.openInventory(gui.getInventory());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof TurtleGUI gui)) {
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0) {
            return;
        }

        // If clicked inside the Turtle GUI
        if (rawSlot < TurtleGUI.INVENTORY_SIZE) {
            Turtle turtle = gui.getTurtle();
            Player player = (Player) event.getWhoClicked();

            // Run Button
            if (rawSlot == TurtleGUI.RUN_BUTTON_SLOT) {
                event.setCancelled(true);
                handleRunButton(player, gui, turtle);
                return;
            }

            // Web Dashboard Button
            if (rawSlot == TurtleGUI.WEB_BUTTON_SLOT) {
                event.setCancelled(true);
                handleWebButton(player, turtle);
                return;
            }

            // Build Control Button
            if (rawSlot == TurtleGUI.BUILD_BUTTON_SLOT) {
                event.setCancelled(true);
                if (turtle.getStatus() == Turtle.Status.BUILDING) {
                    turtle.pauseBuild();
                    player.sendMessage(plugin.getPrefix() + " §eConstruction paused.");
                } else if (turtle.getStatus() == Turtle.Status.PAUSED) {
                    turtle.resumeBuild();
                    player.sendMessage(plugin.getPrefix() + " §aConstruction resumed.");
                }
                gui.setupGUI();
                return;
            }

            // Fuel Slot
            if (rawSlot == TurtleGUI.FUEL_SLOT) {
                event.setCancelled(true);
                ItemStack cursor = event.getCursor();
                if (cursor != null && !cursor.getType().isAir()) {
                    turtle.setItem(turtle.getSelectedSlot(), cursor);
                    if (turtle.refuel(cursor.getAmount())) {
                        event.getView().setCursor(turtle.getItem(turtle.getSelectedSlot()));
                        player.sendMessage(plugin.getPrefix() + " §aRefueled! Current fuel: " + turtle.getFuel());
                        gui.setupGUI();
                    } else {
                        player.sendMessage(plugin.getPrefix() + " §cItem on cursor is not a valid fuel source (coal, blaze rod, lava bucket).");
                    }
                }
                return;
            }

            // Allowed interactive slots: DISK_SLOT and the 16 TURTLE_SLOTS
            if (rawSlot == TurtleGUI.DISK_SLOT || TurtleGUI.isTurtleSlot(rawSlot)) {
                // Allowed! Player can take/place items freely.
                return;
            }

            // All decorative glass slots
            event.setCancelled(true);
        }
    }

    private void handleRunButton(Player player, TurtleGUI gui, Turtle turtle) {
        gui.saveToTurtle();
        ItemStack disk = turtle.getDisk();

        if (disk == null || disk.getType().isAir()) {
            player.sendMessage(plugin.getPrefix() + " §cNo floppy disk inserted in Turtle slot 0.");
            return;
        }

        String code = DiskManager.readProgram(disk);
        String error = LuaRunner.validate(code);
        if (error != null) {
            player.sendMessage(plugin.getPrefix() + " §cSyntax error on Turtle disk:\n §4✘ " + error);
            return;
        }

        player.sendMessage(plugin.getPrefix() + " §7Executing Turtle script…");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Globals globals = LuaRunner.sandbox(1_000_000, true);

            // Bind Turtle peripheral as global 'turtle'
            TurtlePeripheral turtlePeripheral = new TurtlePeripheral(plugin, turtle);
            globals.set("turtle", turtlePeripheral.toLuaTable());

            // Bind other standard peripherals if adjacent
            PeripheralManager.bindAll(globals, plugin, turtle.getLocation(), true);

            try {
                LuaValue chunk = globals.load(code);
                chunk.call();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage(plugin.getPrefix() + " §aTurtle script executed successfully.");
                    }
                });
            } catch (LuaError e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage(plugin.getPrefix() + " §cTurtle execution error: " + e.getMessage());
                    }
                });
            } catch (Throwable t) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage(plugin.getPrefix() + " §cUnexpected error: " + t.getMessage());
                    }
                });
            }
        });
    }

    private void handleWebButton(Player player, Turtle turtle) {
        String webUrl = plugin.getConfigManager().getWebPortalPublicUrl();
        if (webUrl == null || webUrl.isBlank()) {
            webUrl = "http://localhost:" + plugin.getConfigManager().getWebPortalPort();
        }
        player.sendMessage(plugin.getPrefix() + " §b=== Web Dashboard ===");
        player.sendMessage(" §7Portal URL: §f" + webUrl);
        player.sendMessage(" §7Target Turtle ID: §e" + turtle.getId());
        Location loc = turtle.getLocation();
        player.sendMessage(String.format(" §7Current Coords: §a[%d, %d, %d] §7Facing: §e%s",
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), turtle.getFacing().name()));
        player.sendMessage(" §7Upload your §b.litematic §7or §b.nbt §7blueprint on the web portal to build!");
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof TurtleGUI) {
            for (int slot : event.getRawSlots()) {
                if (slot < TurtleGUI.INVENTORY_SIZE) {
                    if (slot != TurtleGUI.DISK_SLOT && !TurtleGUI.isTurtleSlot(slot)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof TurtleGUI gui) {
            gui.saveToTurtle();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != getTurtleBlock()) {
            return;
        }

        Turtle turtle = plugin.getTurtleManager().removeTurtle(block.getLocation());
        if (turtle == null) {
            return;
        }

        event.setDropItems(false);
        World world = block.getWorld();
        Location loc = block.getLocation();

        // 1. Drop Turtle Item
        world.dropItemNaturally(loc, DiskManager.createTurtle(getTurtleBlock()));

        // 2. Drop Disk
        ItemStack disk = turtle.getDisk();
        if (disk != null && !disk.getType().isAir()) {
            world.dropItemNaturally(loc, disk);
        }

        // 3. Drop all 16 inventory items
        for (ItemStack item : turtle.getInventory()) {
            if (item != null && !item.getType().isAir()) {
                world.dropItemNaturally(loc, item);
            }
        }

        event.getPlayer().sendMessage(plugin.getPrefix() + " §eTurtle " + turtle.getId() + " disassembled.");
    }
}

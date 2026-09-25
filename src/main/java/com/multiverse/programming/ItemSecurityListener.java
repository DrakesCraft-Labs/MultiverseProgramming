// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents players and automation from placing writable books, written books, or storage containers
 * (shulkers, chests, barrels, hoppers, dispensers, droppers, etc.) into Item Frames,
 * Chiseled Bookshelves, Decorated Pots, or any storage container block (Chests, Barrels, Shulkers, Hoppers, etc.).
 * Protects against item duplication, chunk overloading, and NBT crash exploits.
 */
public final class ItemSecurityListener implements Listener {

    private final MultiverseProgrammingPlugin plugin;

    public ItemSecurityListener(MultiverseProgrammingPlugin plugin) {
        this.plugin = plugin;
    }

    public static boolean isStorageBlock(Material mat) {
        if (mat == null) return false;
        String name = mat.name();
        return name.endsWith("SHULKER_BOX") ||
                name.equals("CHEST") ||
                name.equals("TRAPPED_CHEST") ||
                name.equals("BARREL") ||
                name.equals("DISPENSER") ||
                name.equals("DROPPER") ||
                name.equals("HOPPER") ||
                name.endsWith("BUNDLE") ||
                name.equals("DECORATED_POT");
    }

    public static boolean isForbiddenBook(Material mat) {
        if (mat == null) return false;
        return mat == Material.WRITABLE_BOOK || mat == Material.WRITTEN_BOOK;
    }

    public static boolean isForbiddenInItemFrame(Material mat) {
        return isStorageBlock(mat) || isForbiddenBook(mat);
    }

    public static boolean isForbiddenInBookshelf(Material mat) {
        return isForbiddenBook(mat) || isStorageBlock(mat);
    }

    public static boolean isForbiddenInStorage(Material mat) {
        return isForbiddenBook(mat) || isStorageBlock(mat);
    }

    public static boolean isForbiddenItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        return isForbiddenInStorage(item.getType());
    }

    public static boolean isStorageInventory(Inventory inv) {
        if (inv == null) return false;
        InventoryHolder holder = inv.getHolder();
        if (holder instanceof Player) return false;
        if (holder instanceof org.bukkit.block.Container || holder instanceof org.bukkit.block.DoubleChest) {
            return true;
        }
        InventoryType type = inv.getType();
        if (type == null) return false;
        String typeName = type.name();
        if (typeName.equals("PLAYER") || typeName.equals("CRAFTING") || typeName.equals("CREATIVE")) {
            return false;
        }
        switch (typeName) {
            case "CHEST":
            case "DISPENSER":
            case "DROPPER":
            case "FURNACE":
            case "BLAST_FURNACE":
            case "SMOKER":
            case "BREWING":
            case "HOPPER":
            case "SHULKER_BOX":
            case "BARREL":
            case "ENDER_CHEST":
            case "CHISELED_BOOKSHELF":
            case "CRAFTER":
                return true;
            default:
                return typeName.contains("CHEST") ||
                        typeName.contains("SHULKER") ||
                        typeName.contains("BARREL") ||
                        typeName.contains("POT") ||
                        typeName.contains("CRAFTER");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ItemFrame) {
            Player player = event.getPlayer();
            if (player.hasPermission("multiverseprogramming.admin")) {
                return;
            }
            EquipmentSlot hand = event.getHand();
            ItemStack item = (hand == EquipmentSlot.OFF_HAND)
                    ? player.getInventory().getItemInOffHand()
                    : player.getInventory().getItemInMainHand();

            if (item != null && isForbiddenInItemFrame(item.getType())) {
                event.setCancelled(true);
                player.sendMessage(plugin.getPrefix() + " §cPlacing books or storage containers in item frames is prohibited to prevent duplication exploits.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractBlock(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Material type = event.getClickedBlock().getType();
            if (type == Material.CHISELED_BOOKSHELF || type == Material.DECORATED_POT) {
                Player player = event.getPlayer();
                if (player.hasPermission("multiverseprogramming.admin")) {
                    return;
                }
                ItemStack item = event.getItem();
                if (item != null && isForbiddenItem(item)) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getPrefix() + " §cPlacing books or storage items in this block is prohibited.");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (player.hasPermission("multiverseprogramming.admin")) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (!isStorageInventory(top)) {
            return;
        }

        int rawSlot = event.getRawSlot();
        int topSize = top.getSize();

        // Shift-click moving item from player inventory into the top container
        if (event.isShiftClick() || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (rawSlot >= topSize) {
                ItemStack clickedItem = event.getCurrentItem();
                if (isForbiddenItem(clickedItem)) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getPrefix() + " §cPlacing books or storage containers in storage blocks is prohibited.");
                }
            }
            return;
        }

        // Direct clicks on slots of the top container
        if (rawSlot >= 0 && rawSlot < topSize) {
            // Placing or swapping item on cursor
            if (isForbiddenItem(event.getCursor())) {
                event.setCancelled(true);
                player.sendMessage(plugin.getPrefix() + " §cPlacing books or storage containers in storage blocks is prohibited.");
                return;
            }

            // Number key hotbar swap
            if (event.getClick() == ClickType.NUMBER_KEY || event.getAction() == InventoryAction.HOTBAR_SWAP) {
                int button = event.getHotbarButton();
                if (button >= 0 && button < 9) {
                    ItemStack hotbarItem = player.getInventory().getItem(button);
                    if (isForbiddenItem(hotbarItem)) {
                        event.setCancelled(true);
                        player.sendMessage(plugin.getPrefix() + " §cPlacing books or storage containers in storage blocks is prohibited.");
                        return;
                    }
                }
            }

            // Off-hand swap
            if (event.getClick() == ClickType.SWAP_OFFHAND) {
                ItemStack offhandItem = player.getInventory().getItemInOffHand();
                if (isForbiddenItem(offhandItem)) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getPrefix() + " §cPlacing books or storage containers in storage blocks is prohibited.");
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (player.hasPermission("multiverseprogramming.admin")) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (!isStorageInventory(top)) {
            return;
        }

        if (isForbiddenItem(event.getOldCursor())) {
            int topSize = top.getSize();
            boolean touchesTop = false;
            for (int slot : event.getRawSlots()) {
                if (slot < topSize) {
                    touchesTop = true;
                    break;
                }
            }
            if (touchesTop) {
                event.setCancelled(true);
                player.sendMessage(plugin.getPrefix() + " §cPlacing books or storage containers in storage blocks is prohibited.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (isStorageInventory(event.getDestination())) {
            if (isForbiddenItem(event.getItem())) {
                event.setCancelled(true);
            }
        }
    }
}

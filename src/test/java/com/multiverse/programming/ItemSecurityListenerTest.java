// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class ItemSecurityListenerTest {

    @Test
    @DisplayName("isForbiddenInItemFrame blocks writable books, written books, and storage blocks")
    void testForbiddenInItemFrame() {
        assertTrue(ItemSecurityListener.isForbiddenInItemFrame(Material.WRITABLE_BOOK));
        assertTrue(ItemSecurityListener.isForbiddenInItemFrame(Material.WRITTEN_BOOK));
        assertTrue(ItemSecurityListener.isForbiddenInItemFrame(Material.SHULKER_BOX));
        assertTrue(ItemSecurityListener.isForbiddenInItemFrame(Material.BLACK_SHULKER_BOX));
        assertTrue(ItemSecurityListener.isForbiddenInItemFrame(Material.WHITE_SHULKER_BOX));
        assertTrue(ItemSecurityListener.isForbiddenInItemFrame(Material.CHEST));
        assertTrue(ItemSecurityListener.isForbiddenInItemFrame(Material.TRAPPED_CHEST));
        assertTrue(ItemSecurityListener.isForbiddenInItemFrame(Material.BARREL));
        assertTrue(ItemSecurityListener.isForbiddenInItemFrame(Material.HOPPER));
        assertTrue(ItemSecurityListener.isForbiddenInItemFrame(Material.DISPENSER));
        assertTrue(ItemSecurityListener.isForbiddenInItemFrame(Material.DROPPER));

        assertFalse(ItemSecurityListener.isForbiddenInItemFrame(Material.DIAMOND_SWORD));
        assertFalse(ItemSecurityListener.isForbiddenInItemFrame(Material.TORCH));
        assertFalse(ItemSecurityListener.isForbiddenInItemFrame(Material.CLOCK));
    }

    @Test
    @DisplayName("isForbiddenInBookshelf blocks writable books, written books, and storage blocks in chiseled bookshelves")
    void testForbiddenInBookshelf() {
        assertTrue(ItemSecurityListener.isForbiddenInBookshelf(Material.WRITABLE_BOOK));
        assertTrue(ItemSecurityListener.isForbiddenInBookshelf(Material.WRITTEN_BOOK));
        assertTrue(ItemSecurityListener.isForbiddenInBookshelf(Material.SHULKER_BOX));
        assertTrue(ItemSecurityListener.isForbiddenInBookshelf(Material.CHEST));

        assertFalse(ItemSecurityListener.isForbiddenInBookshelf(Material.BOOK));
        assertFalse(ItemSecurityListener.isForbiddenInBookshelf(Material.ENCHANTED_BOOK));
    }

    @Test
    @DisplayName("isForbiddenInStorage blocks books and storage blocks from any container")
    void testForbiddenInStorage() {
        assertTrue(ItemSecurityListener.isForbiddenInStorage(Material.WRITABLE_BOOK));
        assertTrue(ItemSecurityListener.isForbiddenInStorage(Material.WRITTEN_BOOK));
        assertTrue(ItemSecurityListener.isForbiddenInStorage(Material.CHEST));
        assertTrue(ItemSecurityListener.isForbiddenInStorage(Material.TRAPPED_CHEST));
        assertTrue(ItemSecurityListener.isForbiddenInStorage(Material.BARREL));
        assertTrue(ItemSecurityListener.isForbiddenInStorage(Material.SHULKER_BOX));
        assertTrue(ItemSecurityListener.isForbiddenInStorage(Material.RED_SHULKER_BOX));
        assertTrue(ItemSecurityListener.isForbiddenInStorage(Material.HOPPER));
        assertTrue(ItemSecurityListener.isForbiddenInStorage(Material.DISPENSER));
        assertTrue(ItemSecurityListener.isForbiddenInStorage(Material.DROPPER));
        assertTrue(ItemSecurityListener.isForbiddenInStorage(Material.BUNDLE));
        assertTrue(ItemSecurityListener.isForbiddenInStorage(Material.DECORATED_POT));

        assertFalse(ItemSecurityListener.isForbiddenInStorage(Material.DIAMOND));
        assertFalse(ItemSecurityListener.isForbiddenInStorage(Material.IRON_INGOT));
        assertFalse(ItemSecurityListener.isForbiddenInStorage(Material.DIRT));
        assertFalse(ItemSecurityListener.isForbiddenInStorage(Material.STONE));
        assertFalse(ItemSecurityListener.isForbiddenInStorage(Material.BOOK));
        assertFalse(ItemSecurityListener.isForbiddenInStorage(Material.ENCHANTED_BOOK));
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        BukkitMockHelper.setUpMockServer();
    }

    @Test
    @DisplayName("isForbiddenItem correctly handles null and item stacks")
    void testForbiddenItemStack() {
        assertFalse(ItemSecurityListener.isForbiddenItem(null));
        assertFalse(ItemSecurityListener.isForbiddenItem(new ItemStack(Material.DIAMOND)));
        assertTrue(ItemSecurityListener.isForbiddenItem(new ItemStack(Material.CHEST)));
        assertTrue(ItemSecurityListener.isForbiddenItem(new ItemStack(Material.WRITTEN_BOOK)));
        assertFalse(ItemSecurityListener.isForbiddenItem(new ItemStack(Material.AIR)));
    }

    @Test
    @DisplayName("isStorageInventory accurately classifies storage vs player inventories")
    void testIsStorageInventory() {
        assertFalse(ItemSecurityListener.isStorageInventory(null));

        // Player inventory
        Inventory playerInv = Mockito.mock(Inventory.class);
        Player player = Mockito.mock(Player.class);
        Mockito.when(playerInv.getHolder()).thenReturn(player);
        assertFalse(ItemSecurityListener.isStorageInventory(playerInv));

        // Crafting inventory
        Inventory craftInv = Mockito.mock(Inventory.class);
        Mockito.when(craftInv.getHolder()).thenReturn(null);
        Mockito.when(craftInv.getType()).thenReturn(InventoryType.CRAFTING);
        assertFalse(ItemSecurityListener.isStorageInventory(craftInv));

        // Chest inventory
        Inventory chestInv = Mockito.mock(Inventory.class);
        Mockito.when(chestInv.getHolder()).thenReturn(null);
        Mockito.when(chestInv.getType()).thenReturn(InventoryType.CHEST);
        assertTrue(ItemSecurityListener.isStorageInventory(chestInv));

        // Shulker box inventory
        Inventory shulkerInv = Mockito.mock(Inventory.class);
        Mockito.when(shulkerInv.getHolder()).thenReturn(null);
        Mockito.when(shulkerInv.getType()).thenReturn(InventoryType.SHULKER_BOX);
        assertTrue(ItemSecurityListener.isStorageInventory(shulkerInv));

        // Barrel inventory
        Inventory barrelInv = Mockito.mock(Inventory.class);
        Mockito.when(barrelInv.getHolder()).thenReturn(null);
        Mockito.when(barrelInv.getType()).thenReturn(InventoryType.BARREL);
        assertTrue(ItemSecurityListener.isStorageInventory(barrelInv));

        // Hopper inventory
        Inventory hopperInv = Mockito.mock(Inventory.class);
        Mockito.when(hopperInv.getHolder()).thenReturn(null);
        Mockito.when(hopperInv.getType()).thenReturn(InventoryType.HOPPER);
        assertTrue(ItemSecurityListener.isStorageInventory(hopperInv));

        // Container holder
        Inventory containerInv = Mockito.mock(Inventory.class);
        org.bukkit.block.Container containerHolder = Mockito.mock(org.bukkit.block.Container.class);
        Mockito.when(containerInv.getHolder()).thenReturn(containerHolder);
        assertTrue(ItemSecurityListener.isStorageInventory(containerInv));

        // DoubleChest holder
        Inventory doubleChestInv = Mockito.mock(Inventory.class);
        org.bukkit.block.DoubleChest doubleChestHolder = Mockito.mock(org.bukkit.block.DoubleChest.class);
        Mockito.when(doubleChestInv.getHolder()).thenReturn(doubleChestHolder);
        assertTrue(ItemSecurityListener.isStorageInventory(doubleChestInv));
    }
}


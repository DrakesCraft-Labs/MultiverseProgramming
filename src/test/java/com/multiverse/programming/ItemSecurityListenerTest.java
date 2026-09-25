// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}

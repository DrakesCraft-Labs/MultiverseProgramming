// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiskManagerTest {

    @BeforeEach
    void setUp() {
        BukkitMockHelper.setUpMockServer();
    }

    @AfterEach
    void tearDown() {
        BukkitMockHelper.tearDownMockServer();
    }

    @Test
    @DisplayName("createFloppyDisk creates a valid book and quill with template")
    void testCreateFloppyDisk() {
        ItemStack disk = DiskManager.createFloppyDisk();
        assertNotNull(disk);
        assertEquals(Material.WRITABLE_BOOK, disk.getType());
        assertTrue(disk.hasItemMeta());
        BookMeta meta = (BookMeta) disk.getItemMeta();
        assertNotNull(meta);
        assertEquals(DiskManager.NAME, meta.getDisplayName());
        assertTrue(meta.hasPages());
        assertTrue(meta.getPage(1).contains("Write your program here"));
    }

    @Test
    @DisplayName("createComputer supports default and custom materials")
    void testCreateComputer() {
        ItemStack defaultComputer = DiskManager.createComputer();
        assertEquals(Material.LECTERN, defaultComputer.getType());
        assertEquals("Computer", defaultComputer.getItemMeta().getDisplayName());

        ItemStack customComputer = DiskManager.createComputer(Material.NOTE_BLOCK);
        assertEquals(Material.NOTE_BLOCK, customComputer.getType());
        assertEquals("Computer", customComputer.getItemMeta().getDisplayName());

        // Fallback for null
        ItemStack fallback = DiskManager.createComputer(null);
        assertEquals(Material.LECTERN, fallback.getType());
    }

    @Test
    @DisplayName("createAdvancedComputer supports default and custom materials")
    void testCreateAdvancedComputer() {
        ItemStack defaultAdv = DiskManager.createAdvancedComputer();
        assertEquals(Material.ENCHANTING_TABLE, defaultAdv.getType());
        assertEquals(DiskManager.ADVANCED_COMPUTER_NAME, defaultAdv.getItemMeta().getDisplayName());

        ItemStack customAdv = DiskManager.createAdvancedComputer(Material.OBSERVER);
        assertEquals(Material.OBSERVER, customAdv.getType());
        assertEquals(DiskManager.ADVANCED_COMPUTER_NAME, customAdv.getItemMeta().getDisplayName());
    }

    @Test
    @DisplayName("isDisk identifies floppy disk items accurately")
    void testIsDisk() {
        assertFalse(DiskManager.isDisk(null));
        assertFalse(DiskManager.isDisk(new ItemStack(Material.AIR)));
        assertFalse(DiskManager.isDisk(new ItemStack(Material.PAPER)));
        assertFalse(DiskManager.isDisk(new ItemStack(Material.LECTERN)));

        assertTrue(DiskManager.isDisk(new ItemStack(Material.WRITABLE_BOOK)));
        assertTrue(DiskManager.isDisk(new ItemStack(Material.WRITTEN_BOOK)));
        assertTrue(DiskManager.isDisk(DiskManager.createFloppyDisk()));
    }

    @Test
    @DisplayName("readProgram extracts multi-page programs correctly")
    void testReadProgram() {
        ItemStack disk = new ItemStack(Material.WRITABLE_BOOK);
        BookMeta meta = (BookMeta) disk.getItemMeta();
        assertNotNull(meta);
        meta.setPages(List.of("print('page 1')", "print('page 2')"));
        disk.setItemMeta(meta);

        String code = DiskManager.readProgram(disk);
        assertEquals("print('page 1')\nprint('page 2')", code);

        // Non-disk returns empty string
        assertEquals("", DiskManager.readProgram(new ItemStack(Material.DIAMOND)));
        assertEquals("", DiskManager.readProgram(null));
    }
}

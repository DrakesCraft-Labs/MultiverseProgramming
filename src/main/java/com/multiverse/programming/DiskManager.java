// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class DiskManager {

    public static final String NAME = "Floppy Disk";
    public static final String ADVANCED_COMPUTER_NAME = "Advanced Computer";

    private DiskManager() {
    }

    public static ItemStack createFloppyDisk() {
        ItemStack disk = new ItemStack(Material.WRITABLE_BOOK);
        BookMeta meta = (BookMeta) disk.getItemMeta();
        meta.setDisplayName(NAME);
        meta.setLore(List.of(
                "§7Write your program on the pages.",
                "§7Insert it into the computer's disk slot."
        ));
        meta.setPages("-- Write your program here\n-- Example:\n-- print(\"hello world\")");
        disk.setItemMeta(meta);
        return disk;
    }

    public static ItemStack createComputer() {
        ItemStack computer = new ItemStack(Material.LECTERN);
        ItemMeta meta = computer.getItemMeta();
        meta.setDisplayName("Computer");
        meta.setLore(List.of("§7Place it and right-click it to open the GUI."));
        computer.setItemMeta(meta);
        return computer;
    }

    public static ItemStack createAdvancedComputer() {
        ItemStack computer = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta meta = computer.getItemMeta();
        meta.setDisplayName(ADVANCED_COMPUTER_NAME);
        meta.setLore(List.of(
                "§7Runs looping programs without the short timeout.",
                "§7Keeps the disk inside its inventory."
        ));
        computer.setItemMeta(meta);
        return computer;
    }

    public static boolean isDisk(ItemStack item) {
        if (item == null) {
            return false;
        }
        Material type = item.getType();
        return type == Material.WRITABLE_BOOK || type == Material.WRITTEN_BOOK;
    }

    public static String readProgram(ItemStack disk) {
        if (!isDisk(disk)) {
            return "";
        }
        BookMeta meta = (BookMeta) disk.getItemMeta();
        if (meta == null || !meta.hasPages()) {
            return "";
        }
        return String.join("\n", meta.getPages());
    }
}
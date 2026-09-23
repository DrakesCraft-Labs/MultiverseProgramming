// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class DiskManager {

    public static final String NOMBRE = "Floppy Disk";

    private DiskManager() {
    }

    public static ItemStack crearDisquete() {
        ItemStack disco = new ItemStack(Material.WRITABLE_BOOK);
        BookMeta meta = (BookMeta) disco.getItemMeta();
        meta.setDisplayName(NOMBRE);
        meta.setLore(List.of(
                "§7Write your program on the pages.",
                "§7Insert it into the computer's disk slot."
        ));
        meta.setPages("-- Write your program here\n-- Example:\n-- print(\"hello world\")");
        disco.setItemMeta(meta);
        return disco;
    }

    public static ItemStack crearComputadora() {
        ItemStack computadora = new ItemStack(Material.LECTERN);
        ItemMeta meta = computadora.getItemMeta();
        meta.setDisplayName("Computer");
        meta.setLore(List.of("§7Place it and right-click it to open the GUI."));
        computadora.setItemMeta(meta);
        return computadora;
    }

    public static boolean esDisco(ItemStack item) {
        if (item == null) {
            return false;
        }
        Material tipo = item.getType();
        return tipo == Material.WRITABLE_BOOK || tipo == Material.WRITTEN_BOOK;
    }

    public static String leerPrograma(ItemStack disco) {
        if (!esDisco(disco)) {
            return "";
        }
        BookMeta meta = (BookMeta) disco.getItemMeta();
        if (meta == null || !meta.hasPages()) {
            return "";
        }
        return String.join("\n", meta.getPages());
    }
}
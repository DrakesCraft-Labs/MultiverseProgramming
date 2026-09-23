// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

public final class DiskManager {

    public static final String NOMBRE = "Disquete";

    private DiskManager() {
    }

    public static ItemStack crearDisquete() {
        ItemStack disco = new ItemStack(Material.WRITABLE_BOOK);
        BookMeta meta = (BookMeta) disco.getItemMeta();
        meta.setDisplayName(NOMBRE);
        meta.setLore(List.of(
                "§7Escribe tu programa en las páginas.",
                "§7Insértalo en el compartimento de la computadora."
        ));
        meta.setPages("-- Escribe aqui tu programa\n-- Ejemplo:\n-- print(\"hola mundo\")");
        disco.setItemMeta(meta);
        return disco;
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
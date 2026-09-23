// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class ComputerGUI {

    public static final String TITULO = "Multiverse - Computer";
    public static final int SLOT_DISCO = 0;
    public static final int SLOT_BOTON = 8;

    private ComputerGUI() {
    }

    public static Inventory abrir() {
        Inventory inv = Bukkit.createInventory(null, 9, TITULO);

        ItemStack marco = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta marcoMeta = marco.getItemMeta();
        marcoMeta.setDisplayName(" ");
        marco.setItemMeta(marcoMeta);

        for (int i = 1; i < SLOT_BOTON; i++) {
            inv.setItem(i, marco.clone());
        }

        ItemStack boton = new ItemStack(Material.EMERALD);
        ItemMeta botonMeta = boton.getItemMeta();
        botonMeta.setDisplayName("§a✔ Validate Code");
        botonMeta.setLore(List.of("§7Checks the disk's code for errors."));
        boton.setItemMeta(botonMeta);
        inv.setItem(SLOT_BOTON, boton);

        return inv;
    }

    public static void pulsarBoton(Player jugador, Inventory inv, String prefijo) {
        ItemStack disco = inv.getItem(SLOT_DISCO);
        if (disco == null || disco.getType().isAir()) {
            jugador.sendMessage(prefijo + " §cThere is no floppy disk in the slot.");
            return;
        }

        String codigo = DiskManager.leerPrograma(disco);
        String error = LuaRunner.validar(codigo);
        jugador.closeInventory();

        if (error != null) {
            jugador.sendMessage(prefijo + " §cError in the code:");
            for (String linea : error.split("\n")) {
                jugador.sendMessage(" §4✘ " + linea);
            }
        } else {
            jugador.sendMessage(prefijo + " §a✔ Code is valid.");
            jugador.sendMessage(prefijo + " §7To run it, hold the disk in your hand and use §e/pc run§7.");
        }
    }
}
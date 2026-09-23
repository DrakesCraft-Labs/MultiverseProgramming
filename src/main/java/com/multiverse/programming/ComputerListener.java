// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public final class ComputerListener implements Listener {

    private final MultiverseProgrammingPlugin plugin;
    private final Material bloqueComputadora;

    public ComputerListener(MultiverseProgrammingPlugin plugin, Material bloqueComputadora) {
        this.plugin = plugin;
        this.bloqueComputadora = bloqueComputadora;
    }

    @EventHandler
    public void alUsarBloque(PlayerInteractEvent evento) {
        if (evento.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block bloque = evento.getClickedBlock();
        if (bloque == null || bloque.getType() != bloqueComputadora) {
            return;
        }

        evento.setCancelled(true);
        evento.getPlayer().openInventory(ComputerGUI.abrir());
    }

    @EventHandler
    public void alClicarInventario(InventoryClickEvent evento) {
        if (!(evento.getWhoClicked() instanceof Player jugador)) {
            return;
        }
        if (!ComputerGUI.TITULO.equals(evento.getView().getTitle())) {
            return;
        }

        int raw = evento.getRawSlot();
        if (raw < 0 || raw >= 9) {
            evento.setCancelled(true);
            return;
        }

        evento.setCancelled(true);

        if (raw == ComputerGUI.SLOT_DISCO) {
            ItemStack cursor = evento.getCursor();
            if (cursor == null || cursor.getType().isAir()) {
                return;
            }
            if (DiskManager.esDisco(cursor)) {
                evento.setCancelled(false);
            } else {
                jugador.sendMessage(plugin.getPrefijo() + " §cSolo puedes insertar un " + DiskManager.NOMBRE + ".");
            }
        } else if (raw == ComputerGUI.SLOT_BOTON) {
            ComputerGUI.pulsarBoton(jugador, evento.getInventory(), plugin.getPrefijo());
        }
    }
}
// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class ComputerCommand implements CommandExecutor, TabCompleter {

    private final MultiverseProgrammingPlugin plugin;

    public ComputerCommand(MultiverseProgrammingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender emisor, Command comando, String etiqueta, String[] args) {
        if (!(emisor instanceof Player jugador)) {
            emisor.sendMessage("Solo jugadores.");
            return true;
        }

        if (args.length == 0) {
            ayuda(jugador);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "disco", "disk", "nuevo" -> {
                jugador.getInventory().addItem(DiskManager.crearDisquete());
                jugador.sendMessage(plugin.getPrefijo() + " §7Recibiste un " + DiskManager.NOMBRE
                        + ". Edítalo y colócalo en la computadora.");
            }
            case "run", "ejecutar" -> ejecutar(jugador);
            default -> ayuda(jugador);
        }
        return true;
    }

    private void ejecutar(Player jugador) {
        ItemStack disco = jugador.getInventory().getItemInMainHand();
        if (!DiskManager.esDisco(disco)) {
            jugador.sendMessage(plugin.getPrefijo() + " §cDebes sujetar un " + DiskManager.NOMBRE + " en la mano.");
            return;
        }

        String codigo = DiskManager.leerPrograma(disco);
        String error = LuaRunner.validar(codigo);
        if (error != null) {
            jugador.sendMessage(plugin.getPrefijo() + " §cError en el código:");
            for (String linea : error.split("\n")) {
                jugador.sendMessage(" §4✘ " + linea);
            }
            return;
        }

        jugador.sendMessage(plugin.getPrefijo() + " §7Ejecutando programa…");
        LuaRunner.Resultado resultado = LuaRunner.ejecutar(codigo, plugin.getTimeoutMs());

        if (!resultado.salida().isEmpty()) {
            for (String linea : resultado.salida().split("\n")) {
                jugador.sendMessage("§f" + linea);
            }
        } else if (resultado.ok()) {
            jugador.sendMessage(plugin.getPrefijo() + " §7(sin salida)");
        }
    }

    private void ayuda(Player jugador) {
        jugador.sendMessage(plugin.getPrefijo() + " §7Comandos:");
        jugador.sendMessage(" §e/pc disco §8- §7obtener un disquete vacío");
        jugador.sendMessage(" §e/pc run §8- §7ejecutar el programa del disquete en la mano");
    }

    @Override
    public List<String> onTabComplete(CommandSender emisor, Command comando, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("disco", "disk", "nuevo", "run", "ejecutar", "help");
        }
        return List.of();
    }
}
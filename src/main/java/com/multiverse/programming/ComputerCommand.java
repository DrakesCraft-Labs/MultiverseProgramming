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
            emisor.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            ayuda(jugador);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "run", "ejecutar" -> ejecutar(jugador);
            case "give" -> dar(jugador, args);
            default -> ayuda(jugador);
        }
        return true;
    }

    private void dar(Player jugador, String[] args) {
        if (!jugador.hasPermission("multiverseprogramming.admin")) {
            jugador.sendMessage(plugin.getPrefijo() + " §cYou don't have permission to use this command.");
            return;
        }
        if (args.length < 2) {
            jugador.sendMessage(plugin.getPrefijo() + " §cUsage: /pc give <floppydisk|computer>");
            return;
        }
        ItemStack objeto;
        switch (args[1].toLowerCase()) {
            case "floppydisk", "disk", "disco", "disquete" -> objeto = DiskManager.crearDisquete();
            case "computer", "computadora", "pc" -> objeto = DiskManager.crearComputadora();
            default -> {
                jugador.sendMessage(plugin.getPrefijo() + " §cUnknown item. Available: floppydisk, computer");
                return;
            }
        }
        jugador.getInventory().addItem(objeto);
        jugador.sendMessage(plugin.getPrefijo() + " §7You received a " + objeto.getItemMeta().getDisplayName() + ".");
    }

    private void ejecutar(Player jugador) {
        ItemStack disco = jugador.getInventory().getItemInMainHand();
        if (!DiskManager.esDisco(disco)) {
            jugador.sendMessage(plugin.getPrefijo() + " §cYou must hold a " + DiskManager.NOMBRE + " in your hand.");
            return;
        }

        String codigo = DiskManager.leerPrograma(disco);
        String error = LuaRunner.validar(codigo);
        if (error != null) {
            jugador.sendMessage(plugin.getPrefijo() + " §cError in the code:");
            for (String linea : error.split("\n")) {
                jugador.sendMessage(" §4✘ " + linea);
            }
            return;
        }

        jugador.sendMessage(plugin.getPrefijo() + " §7Running program…");
        LuaRunner.Resultado resultado = LuaRunner.ejecutar(codigo, plugin.getTimeoutMs());

        if (!resultado.salida().isEmpty()) {
            for (String linea : resultado.salida().split("\n")) {
                jugador.sendMessage("§f" + linea);
            }
        } else if (resultado.ok()) {
            jugador.sendMessage(plugin.getPrefijo() + " §7(no output)");
        }
    }

    private void ayuda(Player jugador) {
jugador.sendMessage(plugin.getPrefijo() + " §7Commands:");
        jugador.sendMessage(" §e/pc run §8- §7run the program on the disk in your hand");
        jugador.sendMessage(" §e/pc give <item> §8- §7admin: give yourself a custom item");
    }

    @Override
    public List<String> onTabComplete(CommandSender emisor, Command comando, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("run", "ejecutar", "give", "help");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return List.of("floppydisk", "disk", "disco", "computer", "computadora");
        }
        return List.of();
    }
}
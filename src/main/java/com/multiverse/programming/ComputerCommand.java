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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            help(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> give(player, args);
            default -> help(player);
        }
        return true;
    }

    private void give(Player player, String[] args) {
        if (!player.hasPermission("multiverseprogramming.admin")) {
            player.sendMessage(plugin.getPrefix() + " §cYou don't have permission to use this command.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + " §cUsage: /pc give <floppydisk|computer>");
            return;
        }
        ItemStack item;
        switch (args[1].toLowerCase()) {
            case "floppydisk", "disk", "disco", "disquete" -> item = DiskManager.createFloppyDisk();
            case "computer", "computadora", "pc" -> item = DiskManager.createComputer();
            case "advancedcomputer", "advanced", "computadoraavanzada" -> item = DiskManager.createAdvancedComputer();
            default -> {
                player.sendMessage(plugin.getPrefix() + " §cUnknown item. Available: floppydisk, computer, advancedcomputer");
                return;
            }
        }
        player.getInventory().addItem(item);
        player.sendMessage(plugin.getPrefix() + " §7You received a " + item.getItemMeta().getDisplayName() + ".");
    }

    private void help(Player player) {
        player.sendMessage(plugin.getPrefix() + " §7Commands:");
        player.sendMessage(" §e/pc give <item> §8- §7admin: give yourself a custom item");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("give");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return List.of("floppydisk", "disk", "disco", "computer", "computadora", "advancedcomputer");
        }
        return List.of();
    }
}
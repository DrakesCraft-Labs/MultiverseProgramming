// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public final class ComputerCommand implements CommandExecutor, TabCompleter {

    private final MultiverseProgrammingPlugin plugin;

    public ComputerCommand(MultiverseProgrammingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> reload(sender);
            case "web", "portal", "dashboard" -> showWebPortal(sender);
            case "blueprints", "bp", "list" -> listBlueprints(sender);
            case "build" -> triggerBuild(sender, args);
            case "give" -> {
                if (sender instanceof Player player) {
                    give(player, args);
                } else {
                    sender.sendMessage(plugin.getPrefix() + " §cThe give command can only be used by players in-game.");
                }
            }
            default -> help(sender);
        }
        return true;
    }

    private void showWebPortal(CommandSender sender) {
        String webUrl = plugin.getConfigManager().getWebPortalPublicUrl();
        if (webUrl == null || webUrl.isBlank()) {
            webUrl = "http://localhost:" + plugin.getConfigManager().getWebPortalPort();
        }
        sender.sendMessage(plugin.getPrefix() + " §b=== Blueprint Web Portal ===");
        sender.sendMessage(" §7Access dashboard to upload §e.litematic §7and §e.nbt §7files:");
        sender.sendMessage(" §f" + webUrl);
        sender.sendMessage(" §7Connect to any active Turtle and dispatch builds with visual preview!");
    }

    private void listBlueprints(CommandSender sender) {
        var manager = plugin.getBlueprintManager();
        if (manager == null) {
            sender.sendMessage(plugin.getPrefix() + " §cBlueprint manager is not available.");
            return;
        }
        var all = manager.getAllBlueprints();
        if (all.isEmpty()) {
            sender.sendMessage(plugin.getPrefix() + " §eNo blueprints loaded. Upload .litematic or .nbt via /pc web!");
            return;
        }
        sender.sendMessage(plugin.getPrefix() + " §b=== Loaded Blueprints (" + all.size() + ") ===");
        for (var bp : all) {
            sender.sendMessage(String.format(" §e%s §7- §f%s §8[%dx%dx%d, %d blocks, %s]",
                    bp.id(), bp.name(), bp.sizeX(), bp.sizeY(), bp.sizeZ(), bp.totalBlocks(), bp.format()));
        }
    }

    private void triggerBuild(CommandSender sender, String[] args) {
        if (!sender.hasPermission("multiverseprogramming.admin")) {
            sender.sendMessage(plugin.getPrefix() + " §cYou don't have permission to use this command.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.getPrefix() + " §cUsage: /pc build <blueprintId> <turtleId> [x y z]");
            return;
        }
        String bpId = args[1];
        String turtleId = args[2];

        var bp = plugin.getBlueprintManager().getBlueprint(bpId);
        if (bp == null) {
            sender.sendMessage(plugin.getPrefix() + " §cBlueprint not found: " + bpId);
            return;
        }

        var turtle = plugin.getTurtleManager().getTurtleById(turtleId);
        if (turtle == null) {
            sender.sendMessage(plugin.getPrefix() + " §cTurtle not found: " + turtleId);
            return;
        }

        org.bukkit.Location origin;
        if (args.length >= 6) {
            try {
                int x = Integer.parseInt(args[3]);
                int y = Integer.parseInt(args[4]);
                int z = Integer.parseInt(args[5]);
                origin = new org.bukkit.Location(turtle.getLocation().getWorld(), x, y, z);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getPrefix() + " §cInvalid coordinates.");
                return;
            }
        } else {
            origin = turtle.getLocation().clone();
        }

        int delay = plugin.getConfigManager().getTurtleBuildDelayTicks();
        boolean requireMaterials = plugin.getConfigManager().isTurtleRequireMaterials();
        turtle.startBuild(bp, origin, delay, requireMaterials,
                () -> sender.sendMessage(plugin.getPrefix() + " §aTurtle " + turtle.getId() + " finished building " + bp.name() + "!"),
                err -> sender.sendMessage(plugin.getPrefix() + " §cTurtle " + turtle.getId() + " error: " + err)
        );
        sender.sendMessage(plugin.getPrefix() + " §aDispatched build §e" + bp.name() + " §ato Turtle §e" + turtle.getId()
                + " §aat [" + origin.getBlockX() + ", " + origin.getBlockY() + ", " + origin.getBlockZ() + "].");
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("multiverseprogramming.admin")) {
            sender.sendMessage(plugin.getPrefix() + " §cYou don't have permission to use this command.");
            return;
        }
        plugin.reloadPluginConfig();
        sender.sendMessage(plugin.getPrefix() + " §aConfiguration and recipes reloaded successfully.");
    }

    private void give(Player player, String[] args) {
        if (!player.hasPermission("multiverseprogramming.admin")) {
            player.sendMessage(plugin.getPrefix() + " §cYou don't have permission to use this command.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + " §cUsage: /pc give <floppydisk|computer|advancedcomputer>");
            return;
        }
        ItemStack item;
        switch (args[1].toLowerCase()) {
            case "floppydisk", "disk", "disco", "disquete" -> item = DiskManager.createFloppyDisk();
            case "computer", "computadora", "pc" -> item = DiskManager.createComputer(plugin.getConfigManager().getComputerBlock());
            case "advancedcomputer", "advanced", "computadoraavanzada" -> item = DiskManager.createAdvancedComputer(plugin.getConfigManager().getAdvancedComputerBlock());
            case "monitor", "pantalla", "display" -> item = DiskManager.createMonitor(plugin.getConfigManager().getMonitorBlock());
            case "crafter", "autocrafter", "ensamblador" -> item = DiskManager.createCrafter(plugin.getConfigManager().getCrafterBlock());
            case "transposer", "transpositor" -> item = DiskManager.createTransposer(plugin.getConfigManager().getTransposerBlock());
            case "speaker", "sound", "synthesizer", "parlante" -> item = DiskManager.createSpeaker(plugin.getConfigManager().getSpeakerBlock());
            case "turtle", "tortuga", "robot" -> item = DiskManager.createTurtle(plugin.getConfigManager().getTurtleBlock());
            default -> {
                player.sendMessage(plugin.getPrefix() + " §cUnknown item. Available: floppydisk, computer, advancedcomputer, monitor, crafter, transposer, speaker, turtle");
                return;
            }
        }
        player.getInventory().addItem(item);
        String displayName = (item.getItemMeta() != null && item.getItemMeta().hasDisplayName())
                ? item.getItemMeta().getDisplayName()
                : item.getType().name();
        player.sendMessage(plugin.getPrefix() + " §7You received a " + displayName + ".");
    }

    private void help(CommandSender sender) {
        sender.sendMessage(plugin.getPrefix() + " §7Commands:");
        sender.sendMessage(" §e/pc web §8- §7view Web Dashboard link for uploading .litematic & .nbt");
        sender.sendMessage(" §e/pc blueprints §8- §7list loaded blueprints and sizes");
        if (sender.hasPermission("multiverseprogramming.admin")) {
            sender.sendMessage(" §e/pc build <bpId> <turtleId> [x y z] §8- §7order turtle to build");
            sender.sendMessage(" §e/pc give <item> §8- §7admin: give yourself a custom item");
            sender.sendMessage(" §e/pc reload §8- §7admin: reload configuration and recipes");
        } else {
            sender.sendMessage(" §e/pc help §8- §7shows this help menu");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            subcommands.add("help");
            subcommands.add("web");
            subcommands.add("blueprints");
            if (sender.hasPermission("multiverseprogramming.admin")) {
                subcommands.add("build");
                subcommands.add("give");
                subcommands.add("reload");
            }
            return StringUtil.copyPartialMatches(args[0], subcommands, completions);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give") && sender.hasPermission("multiverseprogramming.admin")) {
            List<String> items = List.of(
                    "floppydisk", "disk", "disco", "computer", "computadora", "advancedcomputer",
                    "monitor", "crafter", "transposer", "speaker", "turtle"
            );
            return StringUtil.copyPartialMatches(args[1], items, completions);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("build") && sender.hasPermission("multiverseprogramming.admin")) {
            if (plugin.getBlueprintManager() != null) {
                List<String> ids = plugin.getBlueprintManager().getAllBlueprints().stream().map(com.multiverse.programming.blueprint.Blueprint::id).toList();
                return StringUtil.copyPartialMatches(args[1], ids, completions);
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("build") && sender.hasPermission("multiverseprogramming.admin")) {
            if (plugin.getTurtleManager() != null) {
                List<String> ids = plugin.getTurtleManager().getAllTurtles().stream().map(com.multiverse.programming.turtle.Turtle::getId).toList();
                return StringUtil.copyPartialMatches(args[2], ids, completions);
            }
        }
        return completions;
    }
}
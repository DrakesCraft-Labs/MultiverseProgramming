// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import com.multiverse.programming.blueprint.Blueprint;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
            case "quota" -> handleQuotaCommand(sender, args);
            case "blueprint", "blueprints", "bp" -> handleBlueprintCommand(sender, args);
            case "get", "download", "pastebin" -> handleGetCommand(sender, args, false);
            case "getbypass", "bypassget", "import" -> handleGetCommand(sender, args, true);
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
        var webManager = plugin.getWebServerManager();
        if (webManager == null || !webManager.isRunning()) {
            sender.sendMessage(plugin.getPrefix() + " §cThe Web Portal is currently offline (port could not be bound).");
            sender.sendMessage(" §7Check server console or configure a free port in §eplugins/MultiverseProgramming/config.yml§7.");
            return;
        }
        String webUrl = plugin.getConfigManager().getWebPortalPublicUrl();
        if (webUrl == null || webUrl.isBlank()) {
            webUrl = "http://localhost:" + webManager.getActivePort();
        }
        sender.sendMessage(plugin.getPrefix() + " §b=== Blueprint Web Portal ===");
        sender.sendMessage(" §7Access dashboard to upload §e.litematic §7and §e.nbt §7files:");
        sender.sendMessage(" §f" + webUrl);
        sender.sendMessage(" §7Connect to any active Turtle and dispatch builds with visual preview!");
    }

    private void handleQuotaCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("multiverseprogramming.use")) {
            sender.sendMessage(plugin.getPrefix() + " §cYou don't have permission to use this command.");
            return;
        }

        var manager = plugin.getBlueprintManager();
        if (manager == null) {
            sender.sendMessage(plugin.getPrefix() + " §cBlueprint manager is not available.");
            return;
        }

        String targetPlayer;
        if (args.length > 1) {
            if (!sender.hasPermission("multiverseprogramming.admin")) {
                sender.sendMessage(plugin.getPrefix() + " §cYou can only view your own storage quota.");
                return;
            }
            targetPlayer = args[1];
        } else {
            if (sender instanceof Player p) {
                targetPlayer = p.getName();
            } else {
                targetPlayer = "Server";
            }
        }

        long used = manager.getPlayerUsageBytes(targetPlayer);
        double quotaMb = plugin.getConfigManager().getBlueprintPlayerQuotaMb();
        double usedMb = Math.round((used / (1024.0 * 1024.0)) * 100.0) / 100.0;
        double remainingMb = Math.max(0.0, Math.round((quotaMb - usedMb) * 100.0) / 100.0);
        double percent = Math.min(100.0, Math.round((used / (quotaMb * 1024.0 * 1024.0)) * 1000.0) / 10.0);

        sender.sendMessage(plugin.getPrefix() + " §b=== Blueprint Storage Quota ===");
        sender.sendMessage(" §7Player: §f" + targetPlayer);
        sender.sendMessage(String.format(Locale.ROOT, " §7Storage Used: §e%.2f MB §7/ §a%.2f MB §8(§b%.1f%%§8)", usedMb, quotaMb, percent));
        sender.sendMessage(String.format(Locale.ROOT, " §7Available Remaining: §a%.2f MB", remainingMb));
    }

    private void handleBlueprintCommand(CommandSender sender, String[] args) {
        var manager = plugin.getBlueprintManager();
        if (manager == null) {
            sender.sendMessage(plugin.getPrefix() + " §cBlueprint manager is not available.");
            return;
        }

        String sub = args.length > 1 ? args[1].toLowerCase() : "list";
        switch (sub) {
            case "quota" -> {
                String[] forwardedArgs = args.length > 2 ? new String[]{"quota", args[2]} : new String[]{"quota"};
                handleQuotaCommand(sender, forwardedArgs);
            }
            case "delete", "remove" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.getPrefix() + " §cUsage: /mvprog bp delete <blueprintId>");
                    return;
                }
                String bpId = args[2];
                String user = sender.hasPermission("multiverseprogramming.admin") ? "Admin" : sender.getName();
                try {
                    boolean ok = manager.deleteBlueprint(bpId, user);
                    if (ok) {
                        sender.sendMessage(plugin.getPrefix() + " §aBlueprint §e" + bpId + " §asuccessfully deleted.");
                    } else {
                        sender.sendMessage(plugin.getPrefix() + " §cBlueprint not found: " + bpId);
                    }
                } catch (SecurityException e) {
                    sender.sendMessage(plugin.getPrefix() + " §c" + e.getMessage());
                }
            }
            case "clean" -> {
                if (!sender.hasPermission("multiverseprogramming.admin")) {
                    sender.sendMessage(plugin.getPrefix() + " §cYou don't have permission to clean blueprints.");
                    return;
                }
                int days = plugin.getConfigManager().getBlueprintRetentionDays();
                if (args.length > 2) {
                    try {
                        days = Integer.parseInt(args[2]);
                    } catch (NumberFormatException ignored) {}
                }
                int cleaned = manager.cleanOldBlueprints(days);
                sender.sendMessage(plugin.getPrefix() + " §aPurged §e" + cleaned + " §aold unpinned blueprint(s) older than §e" + days + " §adays.");
            }
            default -> {
                var all = manager.getAllBlueprints();
                if (all.isEmpty()) {
                    sender.sendMessage(plugin.getPrefix() + " §eNo blueprints loaded. Upload .litematic or .nbt via /mvprog web!");
                    return;
                }
                sender.sendMessage(plugin.getPrefix() + " §b=== Loaded Blueprints (" + all.size() + ") ===");
                for (var bp : all) {
                    String owner = manager.getOwner(bp.id());
                    sender.sendMessage(String.format(" §e%s §7- §f%s §8[%dx%dx%d, %d blocks, %s] §7by §b%s",
                            bp.id(), bp.name(), bp.sizeX(), bp.sizeY(), bp.sizeZ(), bp.totalBlocks(), bp.format(), owner));
                }
            }
        }
    }

    private void triggerBuild(CommandSender sender, String[] args) {
        if (!sender.hasPermission("multiverseprogramming.admin")) {
            sender.sendMessage(plugin.getPrefix() + " §cYou don't have permission to use this command. Only administrators can use /mvprog build.");
            return;
        }
        if (args.length < 5) {
            sender.sendMessage(plugin.getPrefix() + " §cTarget coordinates (X Y Z) are mandatory. The turtle will not build without coordinates.");
            sender.sendMessage(" §7Usage: §e/mvprog build <blueprintId|code|url> <x> <y> <z> [turtleId] [clear]");
            sender.sendMessage(" §7Or: §e/mvprog build <blueprintId|code|url> <turtleId> <x> <y> <z> [clear]");
            return;
        }

        String bpId = args[1];
        String turtleId = null;
        int x, y, z;
        boolean clearBlocks = false;

        for (int i = 2; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("clear") || args[i].equalsIgnoreCase("force") || args[i].equalsIgnoreCase("true")) {
                clearBlocks = true;
                break;
            }
        }

        boolean isCoordArg2;
        try {
            Integer.parseInt(args[2]);
            isCoordArg2 = true;
        } catch (NumberFormatException e) {
            isCoordArg2 = false;
        }

        if (isCoordArg2) {
            // Format: /pc build <bp> <x> <y> <z> [turtleId] [clear]
            try {
                x = Integer.parseInt(args[2]);
                y = Integer.parseInt(args[3]);
                z = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getPrefix() + " §cInvalid coordinates. X, Y, and Z must be integers.");
                return;
            }
            if (args.length >= 6 && !args[5].equalsIgnoreCase("clear") && !args[5].equalsIgnoreCase("force") && !args[5].equalsIgnoreCase("true")) {
                turtleId = args[5];
            }
        } else {
            // Format: /pc build <bp> <turtleId> <x> <y> <z> [clear]
            turtleId = args[2];
            if (args.length < 6) {
                sender.sendMessage(plugin.getPrefix() + " §cTarget coordinates (X Y Z) are mandatory. The turtle will not build without coordinates.");
                sender.sendMessage(" §7Usage: §e/pc build " + bpId + " " + turtleId + " <x> <y> <z> [clear]");
                return;
            }
            try {
                x = Integer.parseInt(args[3]);
                y = Integer.parseInt(args[4]);
                z = Integer.parseInt(args[5]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getPrefix() + " §cInvalid coordinates. X, Y, and Z must be integers.");
                return;
            }
        }

        var turtleManager = plugin.getTurtleManager();
        if (turtleManager == null) {
            sender.sendMessage(plugin.getPrefix() + " §cTurtle manager is not available.");
            return;
        }

        com.multiverse.programming.turtle.Turtle turtle = null;
        if (turtleId != null) {
            turtle = turtleManager.getTurtleById(turtleId);
        } else if (sender instanceof Player p) {
            double bestDist = Double.MAX_VALUE;
            for (var t : turtleManager.getAllTurtles()) {
                if (t.getLocation().getWorld() != null && t.getLocation().getWorld().equals(p.getWorld())) {
                    double d = t.getLocation().distanceSquared(p.getLocation());
                    if (d < bestDist) {
                        bestDist = d;
                        turtle = t;
                    }
                }
            }
        } else {
            var allTurtles = turtleManager.getAllTurtles();
            if (!allTurtles.isEmpty()) {
                turtle = allTurtles.iterator().next();
            }
        }

        if (turtle == null) {
            sender.sendMessage(plugin.getPrefix() + " §cNo turtle found" + (turtleId != null ? " with ID: " + turtleId : " nearby.") + " Specify turtle ID or place one nearby.");
            return;
        }

        final com.multiverse.programming.turtle.Turtle targetTurtle = turtle;
        org.bukkit.World world = targetTurtle.getLocation().getWorld();
        if (world == null && sender instanceof Player p) {
            world = p.getWorld();
        }
        if (world == null) {
            sender.sendMessage(plugin.getPrefix() + " §cTurtle world is unloaded.");
            return;
        }

        final org.bukkit.Location origin = new org.bukkit.Location(world, x, y, z);
        final boolean finalClear = clearBlocks;

        var bp = plugin.getBlueprintManager().getBlueprint(bpId);
        if (bp != null) {
            dispatchTurtleBuild(sender, bp, targetTurtle, origin, finalClear);
        } else {
            sender.sendMessage(plugin.getPrefix() + " §7Downloading blueprint §e" + bpId + " §7from cloud nexus...");
            String owner = (sender instanceof Player p) ? p.getName() : "Server";
            plugin.getBlueprintManager().getOrDownloadBlueprint(bpId, owner)
                    .thenAccept(downloadedBp -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            sender.sendMessage(plugin.getPrefix() + " §aBlueprint §e" + downloadedBp.name() + " §areceived and verified!");
                            dispatchTurtleBuild(sender, downloadedBp, targetTurtle, origin, finalClear);
                        });
                    })
                    .exceptionally(ex -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            String msg = (ex.getCause() != null) ? ex.getCause().getMessage() : ex.getMessage();
                            sender.sendMessage(plugin.getPrefix() + " §cFailed to fetch blueprint: " + msg);
                        });
                        return null;
                    });
        }
    }

    private void dispatchTurtleBuild(CommandSender sender, Blueprint bp, com.multiverse.programming.turtle.Turtle targetTurtle, org.bukkit.Location origin, boolean clearBlocks) {
        int delay = plugin.getConfigManager().getTurtleBuildDelayTicks();
        boolean requireMaterials = plugin.getConfigManager().isTurtleRequireMaterials();
        boolean started = targetTurtle.startBuild(bp, origin, delay, requireMaterials, clearBlocks,
                () -> sender.sendMessage(plugin.getPrefix() + " §aTurtle " + targetTurtle.getId() + " finished building " + bp.name() + "!"),
                err -> sender.sendMessage(plugin.getPrefix() + " §cTurtle " + targetTurtle.getId() + " error: " + err)
        );
        if (started) {
            sender.sendMessage(plugin.getPrefix() + " §aDispatched build §e" + bp.name() + " §ato Turtle §e" + targetTurtle.getId()
                    + " §aat [" + origin.getBlockX() + ", " + origin.getBlockY() + ", " + origin.getBlockZ() + "]"
                    + (clearBlocks ? " §c(Area auto-cleared without drops)§a." : "."));
        }
    }

    private void handleGetCommand(CommandSender sender, String[] args, boolean isBypass) {
        boolean bypass = isBypass || (args.length > 2 && args[2].equalsIgnoreCase("bypass") && sender.hasPermission("multiverseprogramming.admin"));
        if (bypass) {
            if (!sender.hasPermission("multiverseprogramming.admin")) {
                sender.sendMessage(plugin.getPrefix() + " §cYou don't have permission to use the get bypass command. Only administrators can bypass quotas.");
                return;
            }
        } else {
            if (!sender.hasPermission("multiverseprogramming.use")) {
                sender.sendMessage(plugin.getPrefix() + " §cYou don't have permission to use this command.");
                return;
            }
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + " §cUsage: /mvprog " + (bypass ? "getbypass" : "get") + " <pasteCode|url>");
            sender.sendMessage(" §7Example: §e/mvprog " + (bypass ? "getbypass" : "get") + " eIoNTIWqo1 §7or §e/mvprog " + (bypass ? "getbypass" : "get") + " BP-NETHER-PORTAL");
            return;
        }

        String code = args[1];
        sender.sendMessage(plugin.getPrefix() + (bypass ? " §6[Admin Bypass] §7Downloading blueprint §e" : " §7Downloading blueprint §e") + code + " §7from cloud nexus...");
        String owner = bypass ? "Admin" : ((sender instanceof Player p) ? p.getName() : "Server");
        plugin.getBlueprintManager().getOrDownloadBlueprint(code, owner, bypass)
                .thenAccept(bp -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(plugin.getPrefix() + " §a✓ Blueprint downloaded successfully!" + (bypass ? " §6(Quota Bypassed)" : ""));
                        sender.sendMessage(" §7Name: §f" + bp.name());
                        sender.sendMessage(" §7ID/Code: §e" + bp.id());
                        sender.sendMessage(String.format(Locale.ROOT, " §7Size: §b%d×%d×%d §8(§e%,d blocks§8)", bp.sizeX(), bp.sizeY(), bp.sizeZ(), bp.totalBlocks()));
                        sender.sendMessage(" §7To build with Turtle: §a/mvprog build " + bp.id() + " <x> <y> <z>");
                    });
                })
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String msg = (ex.getCause() != null) ? ex.getCause().getMessage() : ex.getMessage();
                        sender.sendMessage(plugin.getPrefix() + " §cDownload failed: " + msg);
                    });
                    return null;
                });
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
            player.sendMessage(plugin.getPrefix() + " §cUsage: /mvprog give <floppydisk|computer|advancedcomputer|turtle...>");
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
        sender.sendMessage(" §e/mvprog web §8- §7view Web Dashboard link for uploading .litematic & .nbt");
        sender.sendMessage(" §e/mvprog get <code|url> §8- §7download blueprint from cloud pastebin/github");
        sender.sendMessage(" §e/mvprog quota §8- §7view your blueprint storage quota and remaining space");
        sender.sendMessage(" §e/mvprog bp [list|quota|delete] §8- §7manage blueprints and check storage quota");
        if (sender.hasPermission("multiverseprogramming.admin")) {
            sender.sendMessage(" §6=== Admin Commands ===");
            sender.sendMessage(" §6/mvprog quota <player> §8- §7view specific player's storage quota");
            sender.sendMessage(" §6/mvprog getbypass <code|url> §8- §7download blueprint bypassing storage quotas");
            sender.sendMessage(" §6/mvprog build <bp|code> <x> <y> <z> [turtle] [clear] §8- §7order turtle to build");
            sender.sendMessage(" §6/mvprog bp clean [days] §8- §7purge old unpinned blueprints");
            sender.sendMessage(" §6/mvprog give <item> §8- §7give custom programming item");
            sender.sendMessage(" §6/mvprog reload §8- §7reload configuration and recipes");
        } else {
            sender.sendMessage(" §e/mvprog help §8- §7shows this help menu");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            subcommands.add("help");
            subcommands.add("web");
            subcommands.add("get");
            subcommands.add("quota");
            subcommands.add("blueprint");
            subcommands.add("blueprints");
            subcommands.add("bp");
            if (sender.hasPermission("multiverseprogramming.admin")) {
                subcommands.add("getbypass");
                subcommands.add("build");
                subcommands.add("give");
                subcommands.add("reload");
            }
            return StringUtil.copyPartialMatches(args[0], subcommands, completions);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("quota") && sender.hasPermission("multiverseprogramming.admin")) {
            List<String> playerNames = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return StringUtil.copyPartialMatches(args[1], playerNames, completions);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("blueprint") || args[0].equalsIgnoreCase("blueprints") || args[0].equalsIgnoreCase("bp"))) {
            List<String> subs = new ArrayList<>(List.of("list", "quota", "delete"));
            if (sender.hasPermission("multiverseprogramming.admin")) {
                subs.add("clean");
            }
            return StringUtil.copyPartialMatches(args[1], subs, completions);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("blueprint") || args[0].equalsIgnoreCase("blueprints") || args[0].equalsIgnoreCase("bp")) && args[1].equalsIgnoreCase("quota") && sender.hasPermission("multiverseprogramming.admin")) {
            List<String> playerNames = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return StringUtil.copyPartialMatches(args[2], playerNames, completions);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("blueprint") || args[0].equalsIgnoreCase("blueprints") || args[0].equalsIgnoreCase("bp")) && args[1].equalsIgnoreCase("delete")) {
            if (plugin.getBlueprintManager() != null) {
                List<String> ids = plugin.getBlueprintManager().getAllBlueprints().stream().map(com.multiverse.programming.blueprint.Blueprint::id).toList();
                return StringUtil.copyPartialMatches(args[2], ids, completions);
            }
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
// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import com.multiverse.programming.blueprint.Blueprint;
import com.multiverse.programming.blueprint.BlueprintRotator;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
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
            case "stop", "cancel" -> handleStopCommand(sender, args);
            case "turtle", "turtles" -> handleTurtleSubcommand(sender, args);
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
            sender.sendMessage(" §7Usage: §e/mvprog build <blueprintId|code|url> <x> <y> <z> [turtleId] [clear] [orientation]");
            sender.sendMessage(" §7Or: §e/mvprog build <blueprintId|code|url> <turtleId> <x> <y> <z> [clear] [orientation]");
            sender.sendMessage(" §7Note: §aCoordinates are relative to the turtle: use §e~ ~ ~ §aor §e0 0 0 §ato build at turtle position.");
            return;
        }

        String bpId = args[1];
        String turtleId = null;
        String rawX, rawY, rawZ;
        boolean clearBlocks = false;
        int rotationDegrees = 0;

        boolean isCoordArg2 = isCoordinateToken(args[2]);

        if (isCoordArg2) {
            // Format: /mvprog build <bp> <x> <y> <z> [turtleId] [clear] [orientation]
            rawX = args[2];
            rawY = args[3];
            rawZ = args[4];
            for (int i = 5; i < args.length; i++) {
                String val = args[i];
                if (val.equalsIgnoreCase("clear") || val.equalsIgnoreCase("force") || val.equalsIgnoreCase("true")) {
                    clearBlocks = true;
                } else if (isOrientationToken(val)) {
                    rotationDegrees = BlueprintRotator.normalizeRotation(val);
                } else if (turtleId == null) {
                    turtleId = val;
                }
            }
        } else {
            // Format: /mvprog build <bp> <turtleId> <x> <y> <z> [clear] [orientation]
            turtleId = args[2];
            if (args.length < 6) {
                sender.sendMessage(plugin.getPrefix() + " §cTarget coordinates (X Y Z) are mandatory. The turtle will not build without coordinates.");
                sender.sendMessage(" §7Usage: §e/mvprog build " + bpId + " " + turtleId + " <x> <y> <z> [clear] [orientation]");
                return;
            }
            rawX = args[3];
            rawY = args[4];
            rawZ = args[5];
            for (int i = 6; i < args.length; i++) {
                String val = args[i];
                if (val.equalsIgnoreCase("clear") || val.equalsIgnoreCase("force") || val.equalsIgnoreCase("true")) {
                    clearBlocks = true;
                } else if (isOrientationToken(val)) {
                    rotationDegrees = BlueprintRotator.normalizeRotation(val);
                }
            }
        }

        if (!isCoordinateToken(rawX) || !isCoordinateToken(rawY) || !isCoordinateToken(rawZ)) {
            sender.sendMessage(plugin.getPrefix() + " §cInvalid coordinates. X, Y, and Z must be relative numbers or ~ tokens (e.g. 0 0 0 or ~ ~ ~).");
            return;
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

        int baseX = targetTurtle.getLocation().getBlockX();
        int baseY = targetTurtle.getLocation().getBlockY();
        int baseZ = targetTurtle.getLocation().getBlockZ();

        int targetX = parseRelativeCoord(rawX, baseX);
        int targetY = parseRelativeCoord(rawY, baseY);
        int targetZ = parseRelativeCoord(rawZ, baseZ);

        final org.bukkit.Location origin = new org.bukkit.Location(world, targetX, targetY, targetZ);
        final boolean finalClear = clearBlocks;
        final int finalRotation = rotationDegrees;

        var bp = plugin.getBlueprintManager().getBlueprint(bpId);
        if (bp != null) {
            dispatchTurtleBuild(sender, bp, targetTurtle, origin, finalClear, finalRotation);
        } else {
            sender.sendMessage(plugin.getPrefix() + " §7Downloading blueprint §e" + bpId + " §7from cloud nexus...");
            String owner = (sender instanceof Player p) ? p.getName() : "Server";
            plugin.getBlueprintManager().getOrDownloadBlueprint(bpId, owner)
                    .thenAccept(downloadedBp -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            sender.sendMessage(plugin.getPrefix() + " §aBlueprint §e" + downloadedBp.name() + " §areceived and verified!");
                            dispatchTurtleBuild(sender, downloadedBp, targetTurtle, origin, finalClear, finalRotation);
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

    private boolean isCoordinateToken(String s) {
        if (s == null || s.isBlank()) return false;
        if (s.equals("~")) return true;
        if (s.startsWith("~")) {
            try {
                Integer.parseInt(s.substring(1));
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private int parseRelativeCoord(String token, int base) {
        if (token.equals("~")) {
            return base;
        }
        if (token.startsWith("~")) {
            return base + Integer.parseInt(token.substring(1));
        }
        return base + Integer.parseInt(token);
    }

    private boolean isOrientationToken(String s) {
        if (s == null) return false;
        String upper = s.toUpperCase(Locale.ROOT);
        return upper.matches("NORTH|EAST|SOUTH|WEST|0|90|180|270");
    }

    private void dispatchTurtleBuild(CommandSender sender, Blueprint bp, com.multiverse.programming.turtle.Turtle targetTurtle, org.bukkit.Location origin, boolean clearBlocks, int rotationDegrees) {
        int delay = plugin.getConfigManager().getTurtleBuildDelayTicks();
        boolean requireMaterials = plugin.getConfigManager().isTurtleRequireMaterials();
        boolean started = targetTurtle.startBuild(bp, origin, delay, requireMaterials, clearBlocks, rotationDegrees,
                () -> sender.sendMessage(plugin.getPrefix() + " §aTurtle " + targetTurtle.getId() + " finished building " + bp.name() + "!"),
                err -> sender.sendMessage(plugin.getPrefix() + " §cTurtle " + targetTurtle.getId() + " error: " + err)
        );
        if (started) {
            int relX = origin.getBlockX() - targetTurtle.getLocation().getBlockX();
            int relY = origin.getBlockY() - targetTurtle.getLocation().getBlockY();
            int relZ = origin.getBlockZ() - targetTurtle.getLocation().getBlockZ();
            sender.sendMessage(plugin.getPrefix() + " §aDispatched build §e" + bp.name() + " §ato Turtle §e" + targetTurtle.getId()
                    + " §aat [" + origin.getBlockX() + ", " + origin.getBlockY() + ", " + origin.getBlockZ() + "]"
                    + " §7(Relative: " + (relX >= 0 ? "+" : "") + relX + ", " + (relY >= 0 ? "+" : "") + relY + ", " + (relZ >= 0 ? "+" : "") + relZ + ")"
                    + (rotationDegrees != 0 ? " §6(Rotation: " + rotationDegrees + "°)§a" : "")
                    + (clearBlocks ? " §c(Area auto-cleared without drops)§a." : "."));
        }
    }

    private void handleStopCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("multiverseprogramming.use")) {
            sender.sendMessage(plugin.getPrefix() + " §cYou don't have permission to use this command.");
            return;
        }

        var turtleManager = plugin.getTurtleManager();
        if (turtleManager == null) {
            sender.sendMessage(plugin.getPrefix() + " §cTurtle manager is not available.");
            return;
        }

        boolean isAdmin = sender.hasPermission("multiverseprogramming.admin");
        Player playerSender = (sender instanceof Player p) ? p : null;

        // Subcommand: /mvprog stop (no argument or "list") -> display selection list
        if (args.length == 1 || (args.length >= 2 && args[1].equalsIgnoreCase("list"))) {
            if (isAdmin && playerSender == null) {
                // Console administrator
                displayTurtleList(sender, turtleManager.getAllTurtles(), true);
                return;
            }

            if (isAdmin) {
                // Admin player: display all server turtles
                displayTurtleList(sender, turtleManager.getAllTurtles(), true);
            } else {
                // Regular player: display only owned turtles
                displayTurtleList(sender, turtleManager.getTurtlesByOwner(playerSender.getUniqueId()), false);
            }
            return;
        }

        String target = args[1];

        // Subcommand: /mvprog stop all
        if (target.equalsIgnoreCase("all")) {
            if (isAdmin) {
                int stopped = 0;
                for (var turtle : turtleManager.getAllTurtles()) {
                    if (turtle.stopAnyWork()) {
                        stopped++;
                    }
                }
                if (stopped > 0) {
                    sender.sendMessage(plugin.getPrefix() + " §aSuccessfully stopped " + stopped + " active turtle(s) across the server.");
                } else {
                    sender.sendMessage(plugin.getPrefix() + " §eNo active turtles were currently working on the server.");
                }
            } else {
                int stopped = 0;
                for (var turtle : turtleManager.getTurtlesByOwner(playerSender.getUniqueId())) {
                    if (turtle.stopAnyWork()) {
                        stopped++;
                    }
                }
                if (stopped > 0) {
                    sender.sendMessage(plugin.getPrefix() + " §aSuccessfully stopped " + stopped + " of your active turtle(s).");
                } else {
                    sender.sendMessage(plugin.getPrefix() + " §eNone of your turtles were actively working.");
                }
            }
            return;
        }

        // Subcommand: /mvprog stop <turtleId>
        var turtle = turtleManager.getTurtleById(target);
        if (turtle == null) {
            sender.sendMessage(plugin.getPrefix() + " §cTurtle §e" + target + " §cnot found.");
            return;
        }

        if (!isAdmin && playerSender != null) {
            if (turtle.getOwner() == null || !turtle.getOwner().equals(playerSender.getUniqueId())) {
                sender.sendMessage(plugin.getPrefix() + " §cYou do not own turtle §e" + target + "§c! You can only stop your own turtles.");
                return;
            }
        }

        boolean wasWorking = turtle.stopAnyWork();
        String ownerInfo = (isAdmin && turtle.getOwner() != null) ? " §7(Owner: §f" + turtle.getOwnerName() + "§7)" : "";
        if (wasWorking) {
            sender.sendMessage(plugin.getPrefix() + " §aTurtle §e" + turtle.getId() + " §ahas been stopped." + ownerInfo);
        } else {
            sender.sendMessage(plugin.getPrefix() + " §eTurtle §6" + turtle.getId() + " §eis already idle." + ownerInfo);
        }
    }

    private void displayTurtleList(CommandSender sender, Collection<com.multiverse.programming.turtle.Turtle> turtles, boolean isAdmin) {
        if (turtles == null || turtles.isEmpty()) {
            if (isAdmin) {
                sender.sendMessage(plugin.getPrefix() + " §7No turtles are currently registered on the server.");
            } else {
                sender.sendMessage(plugin.getPrefix() + " §cYou do not own any placed turtles.");
                sender.sendMessage(" §7Place a Turtle block in the world to start using it.");
            }
            return;
        }

        sender.sendMessage(plugin.getPrefix() + " §b=== " + (isAdmin ? "All Server Turtles" : "Your Turtles") + " ===");
        for (var t : turtles) {
            var loc = t.getLocation();
            String locStr = (loc.getWorld() != null ? loc.getWorld().getName() + " " : "")
                    + "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
            String statusStr;
            if (t.getStatus() == com.multiverse.programming.turtle.Turtle.Status.BUILDING) {
                statusStr = "§aBUILDING (" + String.format(Locale.ROOT, "%.1f%%", t.getProgressPercentage()) + ")";
            } else if (t.getStatus() == com.multiverse.programming.turtle.Turtle.Status.MINING) {
                statusStr = "§6MINING (Y=" + t.getQuarryCurrentY() + ", " + String.format(Locale.ROOT, "%.1f%%", t.getQuarryProgressPercentage()) + ")";
            } else if (t.getStatus() == com.multiverse.programming.turtle.Turtle.Status.PAUSED) {
                statusStr = "§ePAUSED (" + t.getStatusMessage() + ")";
            } else if (t.getStatus() == com.multiverse.programming.turtle.Turtle.Status.MOVING) {
                statusStr = "§bMOVING";
            } else if (t.getStatus() == com.multiverse.programming.turtle.Turtle.Status.ERROR) {
                statusStr = "§cERROR (" + t.getStatusMessage() + ")";
            } else {
                statusStr = "§7IDLE";
            }

            String ownerSuffix = isAdmin ? " §7| Owner: §f" + t.getOwnerName() : "";

            if (sender instanceof Player player) {
                try {
                    net.kyori.adventure.text.Component line = net.kyori.adventure.text.Component.text()
                            .append(net.kyori.adventure.text.Component.text(" §e" + t.getId() + ownerSuffix + " §7| Loc: §f" + locStr + " §7| " + statusStr + " "))
                            .append(net.kyori.adventure.text.Component.text("[STOP]", net.kyori.adventure.text.format.NamedTextColor.RED)
                                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/mvprog stop " + t.getId()))
                                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(net.kyori.adventure.text.Component.text("Click to stop turtle " + t.getId(), net.kyori.adventure.text.format.NamedTextColor.RED))))
                            .build();
                    player.sendMessage(line);
                } catch (Throwable fallback) {
                    player.sendMessage(" §e" + t.getId() + ownerSuffix + " §7| Loc: §f" + locStr + " §7| " + statusStr + " §c[Type: /mvprog stop " + t.getId() + "]");
                }
            } else {
                sender.sendMessage(" §e" + t.getId() + ownerSuffix + " §7| Loc: §f" + locStr + " §7| " + statusStr);
            }
        }
        sender.sendMessage(" §7Run §e/mvprog stop <id> §7to stop a specific turtle, or §e/mvprog stop all §7to stop all.");
    }

    private void handleTurtleSubcommand(CommandSender sender, String[] args) {
        if (args.length == 1) {
            handleStopCommand(sender, new String[]{"stop"});
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        if (sub.equals("list")) {
            handleStopCommand(sender, new String[]{"stop", "list"});
        } else if (sub.equals("stop") || sub.equals("cancel")) {
            if (args.length >= 3) {
                handleStopCommand(sender, new String[]{"stop", args[2]});
            } else {
                handleStopCommand(sender, new String[]{"stop"});
            }
        } else {
            if (args.length >= 3 && (args[2].equalsIgnoreCase("stop") || args[2].equalsIgnoreCase("cancel"))) {
                handleStopCommand(sender, new String[]{"stop", args[1]});
            } else {
                handleStopCommand(sender, new String[]{"stop", args[1]});
            }
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
            case "scanner", "escaner" -> item = DiskManager.createScanner(plugin.getConfigManager().getScannerBlock());
            case "cartographer", "cartografo", "map" -> item = DiskManager.createCartographer(plugin.getConfigManager().getCartographerBlock());
            case "alchemist", "alquimista", "potion" -> item = DiskManager.createAlchemist(plugin.getConfigManager().getAlchemistBlock());
            case "farmer", "granjero", "agriculture" -> item = DiskManager.createFarmer(plugin.getConfigManager().getFarmerBlock());
            case "quarry", "cantera", "excavator" -> item = DiskManager.createQuarry(plugin.getConfigManager().getQuarryBlock());
            case "npc", "chatbot", "dialogue" -> item = DiskManager.createNpc(plugin.getConfigManager().getNpcBlock());
            default -> {
                player.sendMessage(plugin.getPrefix() + " §cUnknown item. Available: floppydisk, computer, advancedcomputer, monitor, crafter, transposer, speaker, turtle, scanner, cartographer, alchemist, farmer, quarry, npc");
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
        sender.sendMessage(" §e/mvprog stop [id|all] §8- §7stop active turtle construction/quarry or list your turtles");
        sender.sendMessage(" §e/mvprog turtle [list|stop] §8- §7manage and inspect your placed turtles");
        if (sender.hasPermission("multiverseprogramming.admin")) {
            sender.sendMessage(" §6=== Admin Commands ===");
            sender.sendMessage(" §6/mvprog quota <player> §8- §7view specific player's storage quota");
            sender.sendMessage(" §6/mvprog getbypass <code|url> §8- §7download blueprint bypassing storage quotas");
            sender.sendMessage(" §6/mvprog build <bp|code> <x> <y> <z> [turtle] [clear] [orientation] §8- §7order turtle to build");
            sender.sendMessage(" §6/mvprog stop [id|all] §8- §7stop any turtle (or all turtles) across the server");
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
            subcommands.add("stop");
            subcommands.add("turtle");
            if (sender.hasPermission("multiverseprogramming.admin")) {
                subcommands.add("getbypass");
                subcommands.add("build");
                subcommands.add("give");
                subcommands.add("reload");
            }
            return StringUtil.copyPartialMatches(args[0], subcommands, completions);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("stop") || args[0].equalsIgnoreCase("cancel"))) {
            List<String> options = new ArrayList<>();
            options.add("all");
            options.add("list");
            if (plugin.getTurtleManager() != null) {
                if (sender.hasPermission("multiverseprogramming.admin")) {
                    options.addAll(plugin.getTurtleManager().getAllTurtles().stream().map(com.multiverse.programming.turtle.Turtle::getId).toList());
                } else if (sender instanceof Player p) {
                    options.addAll(plugin.getTurtleManager().getTurtlesByOwner(p.getUniqueId()).stream().map(com.multiverse.programming.turtle.Turtle::getId).toList());
                }
            }
            return StringUtil.copyPartialMatches(args[1], options, completions);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("turtle") || args[0].equalsIgnoreCase("turtles"))) {
            List<String> options = new ArrayList<>(List.of("list", "stop"));
            return StringUtil.copyPartialMatches(args[1], options, completions);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("turtle") || args[0].equalsIgnoreCase("turtles")) && args[1].equalsIgnoreCase("stop")) {
            List<String> options = new ArrayList<>();
            options.add("all");
            if (plugin.getTurtleManager() != null) {
                if (sender.hasPermission("multiverseprogramming.admin")) {
                    options.addAll(plugin.getTurtleManager().getAllTurtles().stream().map(com.multiverse.programming.turtle.Turtle::getId).toList());
                } else if (sender instanceof Player p) {
                    options.addAll(plugin.getTurtleManager().getTurtlesByOwner(p.getUniqueId()).stream().map(com.multiverse.programming.turtle.Turtle::getId).toList());
                }
            }
            return StringUtil.copyPartialMatches(args[2], options, completions);
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
                    "monitor", "crafter", "transposer", "speaker", "turtle",
                    "scanner", "cartographer", "alchemist", "farmer", "quarry", "npc"
            );
            return StringUtil.copyPartialMatches(args[1], items, completions);
        }
        if (args[0].equalsIgnoreCase("build") && sender.hasPermission("multiverseprogramming.admin")) {
            if (args.length == 2) {
                if (plugin.getBlueprintManager() != null) {
                    List<String> ids = plugin.getBlueprintManager().getAllBlueprints().stream().map(com.multiverse.programming.blueprint.Blueprint::id).toList();
                    return StringUtil.copyPartialMatches(args[1], ids, completions);
                }
            } else if (args.length >= 3 && args.length <= 5) {
                List<String> suggestions = new ArrayList<>(List.of("~", "0"));
                if (args.length == 3 && plugin.getTurtleManager() != null) {
                    suggestions.addAll(plugin.getTurtleManager().getAllTurtles().stream().map(com.multiverse.programming.turtle.Turtle::getId).toList());
                }
                return StringUtil.copyPartialMatches(args[args.length - 1], suggestions, completions);
            } else if (args.length >= 6) {
                List<String> options = new ArrayList<>(List.of("clear", "NORTH", "EAST", "SOUTH", "WEST", "0", "90", "180", "270"));
                if (plugin.getTurtleManager() != null) {
                    options.addAll(plugin.getTurtleManager().getAllTurtles().stream().map(com.multiverse.programming.turtle.Turtle::getId).toList());
                }
                return StringUtil.copyPartialMatches(args[args.length - 1], options, completions);
            }
        }
        return completions;
    }
}
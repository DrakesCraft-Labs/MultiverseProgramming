// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class ComputerCommandTest {

    private MultiverseProgrammingPlugin plugin;
    private ConfigManager configManager;
    private ComputerCommand command;
    private Command mockCmd;

    @BeforeEach
    void setUp() {
        BukkitMockHelper.setUpMockServer();
        plugin = mock(MultiverseProgrammingPlugin.class);
        configManager = mock(ConfigManager.class);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getPrefix()).thenReturn("§8[§bComputer§8]");
        when(configManager.getComputerBlock()).thenReturn(Material.LECTERN);
        when(configManager.getAdvancedComputerBlock()).thenReturn(Material.ENCHANTING_TABLE);
        when(configManager.getMonitorBlock()).thenReturn(Material.OCHRE_FROGLIGHT);
        when(configManager.getCrafterBlock()).thenReturn(Material.CRAFTER);
        when(configManager.getTransposerBlock()).thenReturn(Material.HOPPER);
        when(configManager.getSpeakerBlock()).thenReturn(Material.NOTE_BLOCK);
        when(configManager.getTurtleBlock()).thenReturn(Material.DISPENSER);
        when(configManager.getScannerBlock()).thenReturn(Material.OBSERVER);
        when(configManager.getCartographerBlock()).thenReturn(Material.CARTOGRAPHY_TABLE);
        when(configManager.getAlchemistBlock()).thenReturn(Material.BREWING_STAND);
        when(configManager.getFarmerBlock()).thenReturn(Material.COMPOSTER);
        when(configManager.getQuarryBlock()).thenReturn(Material.BLAST_FURNACE);
        when(configManager.getNpcBlock()).thenReturn(Material.SCULK_CATALYST);

        command = new ComputerCommand(plugin);
        mockCmd = mock(Command.class);
    }

    @AfterEach
    void tearDown() {
        BukkitMockHelper.tearDownMockServer();
    }

    @Test
    @DisplayName("Admin player can run /pc give and receives item")
    void testAdminGiveCommand() {
        Player player = mock(Player.class);
        PlayerInventory inv = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        when(player.hasPermission("multiverseprogramming.admin")).thenReturn(true);

        assertTrue(command.onCommand(player, mockCmd, "pc", new String[]{"give", "floppydisk"}));
        verify(inv).addItem(any());
        verify(player).sendMessage(contains("You received a"));

        assertTrue(command.onCommand(player, mockCmd, "pc", new String[]{"give", "computer"}));
        verify(inv, times(2)).addItem(any());

        assertTrue(command.onCommand(player, mockCmd, "pc", new String[]{"give", "advancedcomputer"}));
        verify(inv, times(3)).addItem(any());

        assertTrue(command.onCommand(player, mockCmd, "pc", new String[]{"give", "monitor"}));
        verify(inv, times(4)).addItem(any());

        assertTrue(command.onCommand(player, mockCmd, "pc", new String[]{"give", "crafter"}));
        verify(inv, times(5)).addItem(any());

        assertTrue(command.onCommand(player, mockCmd, "pc", new String[]{"give", "transposer"}));
        verify(inv, times(6)).addItem(any());

        assertTrue(command.onCommand(player, mockCmd, "pc", new String[]{"give", "speaker"}));
        verify(inv, times(7)).addItem(any());

        assertTrue(command.onCommand(player, mockCmd, "pc", new String[]{"give", "turtle"}));
        verify(inv, times(8)).addItem(any());

        assertTrue(command.onCommand(player, mockCmd, "pc", new String[]{"give", "scanner"}));
        verify(inv, times(9)).addItem(any());

        assertTrue(command.onCommand(player, mockCmd, "pc", new String[]{"give", "cartographer"}));
        verify(inv, times(10)).addItem(any());

        assertTrue(command.onCommand(player, mockCmd, "pc", new String[]{"give", "alchemist"}));
        verify(inv, times(11)).addItem(any());

        assertTrue(command.onCommand(player, mockCmd, "pc", new String[]{"give", "farmer"}));
        verify(inv, times(12)).addItem(any());

        assertTrue(command.onCommand(player, mockCmd, "pc", new String[]{"give", "quarry"}));
        verify(inv, times(13)).addItem(any());

        assertTrue(command.onCommand(player, mockCmd, "pc", new String[]{"give", "npc"}));
        verify(inv, times(14)).addItem(any());
    }

    @Test
    @DisplayName("Non-admin player is denied /pc give")
    void testNonAdminGiveDenied() {
        Player player = mock(Player.class);
        PlayerInventory inv = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        when(player.hasPermission("multiverseprogramming.admin")).thenReturn(false);

        assertTrue(command.onCommand(player, mockCmd, "pc", new String[]{"give", "floppydisk"}));
        verify(inv, never()).addItem(any());
        verify(player).sendMessage(contains("permission"));
    }

    @Test
    @DisplayName("Admin can run /pc reload to reload configuration and recipes")
    void testAdminReload() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("multiverseprogramming.admin")).thenReturn(true);

        assertTrue(command.onCommand(sender, mockCmd, "pc", new String[]{"reload"}));
        verify(plugin).reloadPluginConfig();
        verify(sender).sendMessage(contains("reloaded"));
    }

    @Test
    @DisplayName("Non-admin is denied /pc reload")
    void testNonAdminReloadDenied() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("multiverseprogramming.admin")).thenReturn(false);

        assertTrue(command.onCommand(sender, mockCmd, "pc", new String[]{"reload"}));
        verify(plugin, never()).reloadPluginConfig();
        verify(sender).sendMessage(contains("permission"));
    }

    @Test
    @DisplayName("Tab completion filters subcommands based on admin permission")
    void testTabCompletionPermissions() {
        CommandSender admin = mock(CommandSender.class);
        when(admin.hasPermission("multiverseprogramming.admin")).thenReturn(true);

        CommandSender user = mock(CommandSender.class);
        when(user.hasPermission("multiverseprogramming.admin")).thenReturn(false);

        List<String> adminSuggestions = command.onTabComplete(admin, mockCmd, "pc", new String[]{""});
        assertTrue(adminSuggestions.contains("give"));
        assertTrue(adminSuggestions.contains("reload"));
        assertTrue(adminSuggestions.contains("help"));
        assertTrue(adminSuggestions.contains("build"));

        List<String> userSuggestions = command.onTabComplete(user, mockCmd, "pc", new String[]{""});
        assertFalse(userSuggestions.contains("give"));
        assertFalse(userSuggestions.contains("reload"));
        assertFalse(userSuggestions.contains("build"));
        assertTrue(userSuggestions.contains("help"));
        assertTrue(userSuggestions.contains("get"));
    }

    @Test
    @DisplayName("/mvprog build is denied for non-admin players")
    void testBuildDeniedForNonAdmin() {
        Player player = mock(Player.class);
        when(player.hasPermission("multiverseprogramming.admin")).thenReturn(false);

        assertTrue(command.onCommand(player, mockCmd, "mvprog", new String[]{"build", "TEST-BP", "10", "64", "20"}));
        verify(player).sendMessage(contains("Only administrators can use /mvprog build"));
    }

    @Test
    @DisplayName("/mvprog build rejects command if coordinates are missing")
    void testBuildRejectsMissingCoordinates() {
        Player admin = mock(Player.class);
        when(admin.hasPermission("multiverseprogramming.admin")).thenReturn(true);

        assertTrue(command.onCommand(admin, mockCmd, "mvprog", new String[]{"build", "TEST-BP"}));
        verify(admin).sendMessage(contains("coordinates (X Y Z) are mandatory"));
    }

    @Test
    @DisplayName("/mvprog build resolves relative coordinates (~ ~ ~ and offsets) to turtle position")
    void testBuildWithRelativeCoordinates() {
        Player admin = mock(Player.class);
        when(admin.hasPermission("multiverseprogramming.admin")).thenReturn(true);
        World mockWorld = mock(World.class);
        when(admin.getWorld()).thenReturn(mockWorld);

        com.multiverse.programming.turtle.TurtleManager tm = mock(com.multiverse.programming.turtle.TurtleManager.class);
        when(plugin.getTurtleManager()).thenReturn(tm);

        Location turtleLoc = new Location(mockWorld, 100, 64, 200);
        com.multiverse.programming.turtle.Turtle turtle = mock(com.multiverse.programming.turtle.Turtle.class);
        when(turtle.getId()).thenReturn("T-001");
        when(turtle.getLocation()).thenReturn(turtleLoc);
        when(tm.getTurtleById("T-001")).thenReturn(turtle);
        when(tm.getAllTurtles()).thenReturn(List.of(turtle));

        com.multiverse.programming.blueprint.BlueprintManager bpManager = mock(com.multiverse.programming.blueprint.BlueprintManager.class);
        when(plugin.getBlueprintManager()).thenReturn(bpManager);
        com.multiverse.programming.blueprint.Blueprint bp = new com.multiverse.programming.blueprint.Blueprint(
                "TEST-BP", "Test", "Author", "litematic", 1, 1, 1, 1, Map.of(), List.of(), System.currentTimeMillis());
        when(bpManager.getBlueprint("TEST-BP")).thenReturn(bp);

        com.multiverse.programming.ConfigManager cm = mock(com.multiverse.programming.ConfigManager.class);
        when(plugin.getConfigManager()).thenReturn(cm);
        when(cm.getTurtleBuildDelayTicks()).thenReturn(1);
        when(cm.isTurtleRequireMaterials()).thenReturn(false);

        // 1. Test ~ ~ ~
        when(turtle.startBuild(any(), any(), anyInt(), anyBoolean(), anyBoolean(), anyInt(), any(), any())).thenReturn(true);
        assertTrue(command.onCommand(admin, mockCmd, "mvprog", new String[]{"build", "TEST-BP", "~", "~", "~", "T-001"}));
        verify(turtle).startBuild(eq(bp), eq(new Location(mockWorld, 100, 64, 200)), eq(1), eq(false), eq(false), eq(0), any(), any());

        // 2. Test relative offset ~5 ~0 ~-2
        assertTrue(command.onCommand(admin, mockCmd, "mvprog", new String[]{"build", "TEST-BP", "~5", "~0", "~-2", "T-001"}));
        verify(turtle).startBuild(eq(bp), eq(new Location(mockWorld, 105, 64, 198)), eq(1), eq(false), eq(false), eq(0), any(), any());

        // 3. Test plain relative numbers 0 0 0
        assertTrue(command.onCommand(admin, mockCmd, "mvprog", new String[]{"build", "TEST-BP", "0", "0", "0", "T-001"}));
        verify(turtle, atLeastOnce()).startBuild(eq(bp), eq(new Location(mockWorld, 100, 64, 200)), eq(1), eq(false), eq(false), eq(0), any(), any());
    }

    @Test
    @DisplayName("/mvprog get triggers cloud download and notifies sender")
    void testGetCommand() {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Dany");
        when(player.hasPermission("multiverseprogramming.use")).thenReturn(true);

        com.multiverse.programming.blueprint.BlueprintManager bpManager = mock(com.multiverse.programming.blueprint.BlueprintManager.class);
        when(plugin.getBlueprintManager()).thenReturn(bpManager);

        com.multiverse.programming.blueprint.Blueprint mockBp = new com.multiverse.programming.blueprint.Blueprint(
                "TEST-CODE", "Cloud Castle", "Author", "litematic", 10, 10, 10, 100, java.util.Map.of(), java.util.List.of(), System.currentTimeMillis());
        when(bpManager.getOrDownloadBlueprint("TEST-CODE", "Dany", false)).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(mockBp));

        assertTrue(command.onCommand(player, mockCmd, "mvprog", new String[]{"get", "TEST-CODE"}));
        verify(bpManager).getOrDownloadBlueprint("TEST-CODE", "Dany", false);
        verify(player).sendMessage(contains("Downloading"));
    }

    @Test
    @DisplayName("/mvprog getbypass is admin-only and invokes download with bypass=true")
    void testGetBypassCommand() {
        Player admin = mock(Player.class);
        when(admin.getName()).thenReturn("AdminUser");
        when(admin.hasPermission("multiverseprogramming.admin")).thenReturn(true);

        com.multiverse.programming.blueprint.BlueprintManager bpManager = mock(com.multiverse.programming.blueprint.BlueprintManager.class);
        when(plugin.getBlueprintManager()).thenReturn(bpManager);

        com.multiverse.programming.blueprint.Blueprint mockBp = new com.multiverse.programming.blueprint.Blueprint(
                "BIG-BP", "Giant Structure", "Author", "litematic", 50, 50, 50, 10000, java.util.Map.of(), java.util.List.of(), System.currentTimeMillis());
        when(bpManager.getOrDownloadBlueprint("BIG-BP", "Admin", true)).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(mockBp));

        assertTrue(command.onCommand(admin, mockCmd, "mvprog", new String[]{"getbypass", "BIG-BP"}));
        verify(bpManager).getOrDownloadBlueprint("BIG-BP", "Admin", true);
        verify(admin).sendMessage(contains("Admin Bypass"));
    }

    @Test
    @DisplayName("/mvprog quota displays player's own quota and remaining space")
    void testQuotaCommandSelf() {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Dany");
        when(player.hasPermission("multiverseprogramming.use")).thenReturn(true);
        when(configManager.getBlueprintPlayerQuotaMb()).thenReturn(15.0);

        com.multiverse.programming.blueprint.BlueprintManager bpManager = mock(com.multiverse.programming.blueprint.BlueprintManager.class);
        when(plugin.getBlueprintManager()).thenReturn(bpManager);
        when(bpManager.getPlayerUsageBytes("Dany")).thenReturn(5 * 1024 * 1024L); // 5 MB

        assertTrue(command.onCommand(player, mockCmd, "mvprog", new String[]{"quota"}));
        verify(player).sendMessage(contains("Storage Used: §e5.00 MB"));
        verify(player).sendMessage(contains("Available Remaining: §a10.00 MB"));
    }

    @Test
    @DisplayName("Non-admin player cannot view other players' quota")
    void testQuotaOtherPlayerDeniedForNonAdmin() {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Dany");
        when(player.hasPermission("multiverseprogramming.use")).thenReturn(true);
        when(player.hasPermission("multiverseprogramming.admin")).thenReturn(false);

        com.multiverse.programming.blueprint.BlueprintManager bpManager = mock(com.multiverse.programming.blueprint.BlueprintManager.class);
        when(plugin.getBlueprintManager()).thenReturn(bpManager);

        assertTrue(command.onCommand(player, mockCmd, "mvprog", new String[]{"quota", "OtherPlayer"}));
        verify(player).sendMessage(contains("You can only view your own storage quota"));
        verify(bpManager, never()).getPlayerUsageBytes("OtherPlayer");
    }

    @Test
    @DisplayName("Admin can view other players' quota")
    void testQuotaOtherPlayerAllowedForAdmin() {
        Player admin = mock(Player.class);
        when(admin.getName()).thenReturn("AdminUser");
        when(admin.hasPermission("multiverseprogramming.use")).thenReturn(true);
        when(admin.hasPermission("multiverseprogramming.admin")).thenReturn(true);
        when(configManager.getBlueprintPlayerQuotaMb()).thenReturn(15.0);

        com.multiverse.programming.blueprint.BlueprintManager bpManager = mock(com.multiverse.programming.blueprint.BlueprintManager.class);
        when(plugin.getBlueprintManager()).thenReturn(bpManager);
        when(bpManager.getPlayerUsageBytes("OtherPlayer")).thenReturn(2 * 1024 * 1024L);

        assertTrue(command.onCommand(admin, mockCmd, "mvprog", new String[]{"quota", "OtherPlayer"}));
        verify(admin).sendMessage(contains("Player: §fOtherPlayer"));
        verify(admin).sendMessage(contains("Storage Used: §e2.00 MB"));
    }

    @Test
    @DisplayName("Admin can run /mvprog stop all and stops all active turtles")
    void testAdminStopAll() {
        CommandSender admin = mock(CommandSender.class);
        when(admin.hasPermission("multiverseprogramming.use")).thenReturn(true);
        when(admin.hasPermission("multiverseprogramming.admin")).thenReturn(true);

        com.multiverse.programming.turtle.TurtleManager tm = mock(com.multiverse.programming.turtle.TurtleManager.class);
        when(plugin.getTurtleManager()).thenReturn(tm);

        com.multiverse.programming.turtle.Turtle t1 = mock(com.multiverse.programming.turtle.Turtle.class);
        when(t1.stopAnyWork()).thenReturn(true);
        when(tm.getAllTurtles()).thenReturn(List.of(t1));

        assertTrue(command.onCommand(admin, mockCmd, "mvprog", new String[]{"stop", "all"}));
        verify(t1).stopAnyWork();
        verify(admin).sendMessage(contains("Successfully stopped 1 active turtle"));
    }

    @Test
    @DisplayName("Admin can run /mvprog stop <id> to stop any player's turtle")
    void testAdminStopAnyTurtle() {
        CommandSender admin = mock(CommandSender.class);
        when(admin.hasPermission("multiverseprogramming.use")).thenReturn(true);
        when(admin.hasPermission("multiverseprogramming.admin")).thenReturn(true);

        com.multiverse.programming.turtle.TurtleManager tm = mock(com.multiverse.programming.turtle.TurtleManager.class);
        when(plugin.getTurtleManager()).thenReturn(tm);

        com.multiverse.programming.turtle.Turtle t1 = mock(com.multiverse.programming.turtle.Turtle.class);
        when(t1.getId()).thenReturn("T-001");
        when(t1.stopAnyWork()).thenReturn(true);
        when(tm.getTurtleById("T-001")).thenReturn(t1);

        assertTrue(command.onCommand(admin, mockCmd, "mvprog", new String[]{"stop", "T-001"}));
        verify(t1).stopAnyWork();
        verify(admin).sendMessage(contains("Turtle §eT-001 §ahas been stopped"));
    }

    @Test
    @DisplayName("Player can stop their own turtle with /mvprog stop <id>")
    void testPlayerStopOwnTurtle() {
        Player player = mock(Player.class);
        java.util.UUID uuid = java.util.UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.hasPermission("multiverseprogramming.use")).thenReturn(true);
        when(player.hasPermission("multiverseprogramming.admin")).thenReturn(false);

        com.multiverse.programming.turtle.TurtleManager tm = mock(com.multiverse.programming.turtle.TurtleManager.class);
        when(plugin.getTurtleManager()).thenReturn(tm);

        com.multiverse.programming.turtle.Turtle t1 = mock(com.multiverse.programming.turtle.Turtle.class);
        when(t1.getId()).thenReturn("T-002");
        when(t1.getOwner()).thenReturn(uuid);
        when(t1.stopAnyWork()).thenReturn(true);
        when(tm.getTurtleById("T-002")).thenReturn(t1);

        assertTrue(command.onCommand(player, mockCmd, "mvprog", new String[]{"stop", "T-002"}));
        verify(t1).stopAnyWork();
        verify(player).sendMessage(contains("Turtle §eT-002 §ahas been stopped"));
    }

    @Test
    @DisplayName("Player is denied when trying to stop another player's turtle")
    void testPlayerStopOtherTurtleDenied() {
        Player player = mock(Player.class);
        java.util.UUID myUuid = java.util.UUID.randomUUID();
        java.util.UUID otherUuid = java.util.UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(myUuid);
        when(player.hasPermission("multiverseprogramming.use")).thenReturn(true);
        when(player.hasPermission("multiverseprogramming.admin")).thenReturn(false);

        com.multiverse.programming.turtle.TurtleManager tm = mock(com.multiverse.programming.turtle.TurtleManager.class);
        when(plugin.getTurtleManager()).thenReturn(tm);

        com.multiverse.programming.turtle.Turtle t1 = mock(com.multiverse.programming.turtle.Turtle.class);
        when(t1.getId()).thenReturn("T-005");
        when(t1.getOwner()).thenReturn(otherUuid);
        when(tm.getTurtleById("T-005")).thenReturn(t1);

        assertTrue(command.onCommand(player, mockCmd, "mvprog", new String[]{"stop", "T-005"}));
        verify(t1, never()).stopAnyWork();
        verify(player).sendMessage(contains("You do not own turtle"));
    }

    @Test
    @DisplayName("Player /mvprog stop with no args displays owned turtles")
    void testPlayerStopNoArgsListsOwnedTurtles() {
        Player player = mock(Player.class);
        java.util.UUID myUuid = java.util.UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(myUuid);
        when(player.hasPermission("multiverseprogramming.use")).thenReturn(true);
        when(player.hasPermission("multiverseprogramming.admin")).thenReturn(false);

        com.multiverse.programming.turtle.TurtleManager tm = mock(com.multiverse.programming.turtle.TurtleManager.class);
        when(plugin.getTurtleManager()).thenReturn(tm);

        com.multiverse.programming.turtle.Turtle t1 = mock(com.multiverse.programming.turtle.Turtle.class);
        when(t1.getId()).thenReturn("T-001");
        when(t1.getLocation()).thenReturn(new org.bukkit.Location(null, 10, 64, 20));
        when(t1.getStatus()).thenReturn(com.multiverse.programming.turtle.Turtle.Status.IDLE);
        when(tm.getTurtlesByOwner(myUuid)).thenReturn(List.of(t1));

        assertTrue(command.onCommand(player, mockCmd, "mvprog", new String[]{"stop"}));
        verify(player).sendMessage(contains("Your Turtles"));
    }

    @Test
    @DisplayName("Tab completion for /mvprog stop shows all turtles for admin but only owned for player")
    void testStopTabCompletionOwnership() {
        Player admin = mock(Player.class);
        when(admin.hasPermission("multiverseprogramming.admin")).thenReturn(true);

        Player player = mock(Player.class);
        java.util.UUID myUuid = java.util.UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(myUuid);
        when(player.hasPermission("multiverseprogramming.admin")).thenReturn(false);

        com.multiverse.programming.turtle.TurtleManager tm = mock(com.multiverse.programming.turtle.TurtleManager.class);
        when(plugin.getTurtleManager()).thenReturn(tm);

        com.multiverse.programming.turtle.Turtle t1 = mock(com.multiverse.programming.turtle.Turtle.class);
        when(t1.getId()).thenReturn("T-001");
        com.multiverse.programming.turtle.Turtle t2 = mock(com.multiverse.programming.turtle.Turtle.class);
        when(t2.getId()).thenReturn("T-002");

        when(tm.getAllTurtles()).thenReturn(List.of(t1, t2));
        when(tm.getTurtlesByOwner(myUuid)).thenReturn(List.of(t1));

        List<String> adminCompletions = command.onTabComplete(admin, mockCmd, "mvprog", new String[]{"stop", ""});
        assertTrue(adminCompletions.contains("all"));
        assertTrue(adminCompletions.contains("T-001"));
        assertTrue(adminCompletions.contains("T-002"));

        List<String> userCompletions = command.onTabComplete(player, mockCmd, "mvprog", new String[]{"stop", ""});
        assertTrue(userCompletions.contains("all"));
        assertTrue(userCompletions.contains("T-001"));
        assertFalse(userCompletions.contains("T-002"));
    }
}

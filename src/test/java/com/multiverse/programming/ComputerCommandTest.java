// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}

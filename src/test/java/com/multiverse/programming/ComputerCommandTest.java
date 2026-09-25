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

        List<String> userSuggestions = command.onTabComplete(user, mockCmd, "pc", new String[]{""});
        assertFalse(userSuggestions.contains("give"));
        assertFalse(userSuggestions.contains("reload"));
        assertTrue(userSuggestions.contains("help"));
        assertTrue(userSuggestions.contains("get"));
        assertTrue(userSuggestions.contains("build"));
    }

    @Test
    @DisplayName("/pc get triggers cloud download and notifies sender")
    void testGetCommand() {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Dany");
        when(player.hasPermission("multiverseprogramming.use")).thenReturn(true);

        com.multiverse.programming.blueprint.BlueprintManager bpManager = mock(com.multiverse.programming.blueprint.BlueprintManager.class);
        when(plugin.getBlueprintManager()).thenReturn(bpManager);

        com.multiverse.programming.blueprint.Blueprint mockBp = new com.multiverse.programming.blueprint.Blueprint(
                "TEST-CODE", "Cloud Castle", "Author", "litematic", 10, 10, 10, 100, java.util.Map.of(), java.util.List.of(), System.currentTimeMillis());
        when(bpManager.getOrDownloadBlueprint("TEST-CODE", "Dany")).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(mockBp));

        assertTrue(command.onCommand(player, mockCmd, "pc", new String[]{"get", "TEST-CODE"}));
        verify(bpManager).getOrDownloadBlueprint("TEST-CODE", "Dany");
        verify(player).sendMessage(contains("Downloading"));
    }
}

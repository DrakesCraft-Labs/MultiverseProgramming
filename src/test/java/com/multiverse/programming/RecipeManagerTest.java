// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecipeManagerTest {

    private JavaPlugin plugin;
    private Server server;
    private RecipeManager recipeManager;

    @BeforeEach
    void setUp() {
        server = BukkitMockHelper.setUpMockServer();
        plugin = mock(JavaPlugin.class);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getName()).thenReturn("MultiverseProgramming");
        when(plugin.namespace()).thenReturn("multiverseprogramming");

        recipeManager = new RecipeManager(plugin);
    }

    @AfterEach
    void tearDown() {
        BukkitMockHelper.tearDownMockServer();
    }

    @Test
    @DisplayName("registerAll registers recipes and removes existing recipes first")
    void testRegisterAllSafe() {
        assertDoesNotThrow(() -> {
            recipeManager.registerAll(Material.LECTERN, Material.ENCHANTING_TABLE, true);
        });

        // Verify removeRecipe was called to ensure no duplicate keys
        verify(server, atLeastOnce()).removeRecipe(any(NamespacedKey.class));
        // Verify addRecipe was called for floppy, computer, advanced computer, 10 peripherals, and turtle (total 14)
        verify(server, times(14)).addRecipe(any(Recipe.class));
    }

    @Test
    @DisplayName("Multiple consecutive registerAll calls execute without throwing duplicate key exceptions")
    void testMultipleReloadsSafe() {
        assertDoesNotThrow(() -> {
            recipeManager.registerAll(Material.LECTERN, Material.ENCHANTING_TABLE, true);
            recipeManager.registerAll(Material.NOTE_BLOCK, Material.OBSERVER, true);
            recipeManager.registerAll(Material.LECTERN, Material.ENCHANTING_TABLE, false);
        });
    }

    @Test
    @DisplayName("unregisterAll safely removes all registered recipe keys")
    void testUnregisterAll() {
        recipeManager.unregisterAll();
        verify(server, times(14)).removeRecipe(any(NamespacedKey.class));
    }
}

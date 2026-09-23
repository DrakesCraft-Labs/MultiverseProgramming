// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public final class RecipeManager {

    private final JavaPlugin plugin;

    public RecipeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerAll(Material computerBlock) {
        registerFloppyDisk();
        registerAdvancedComputer(computerBlock);
    }

    private void registerFloppyDisk() {
        NamespacedKey key = new NamespacedKey(plugin, "floppy_disk");
        ShapedRecipe recipe = new ShapedRecipe(key, DiskManager.createFloppyDisk());
        recipe.shape(" P ", "PIP", " P ");
        recipe.setIngredient('P', Material.PAPER);
        recipe.setIngredient('I', Material.IRON_INGOT);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerAdvancedComputer(Material computerBlock) {
        NamespacedKey key = new NamespacedKey(plugin, "advanced_computer");
        ShapedRecipe recipe = new ShapedRecipe(key, DiskManager.createAdvancedComputer());
        recipe.shape(" C ", "DOD", "OOO");
        recipe.setIngredient('C', computerBlock);
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('O', Material.OBSIDIAN);
        plugin.getServer().addRecipe(recipe);
    }
}
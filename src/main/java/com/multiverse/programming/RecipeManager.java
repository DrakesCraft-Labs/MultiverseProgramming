// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public final class RecipeManager {

    public static final String KEY_FLOPPY_DISK = "floppy_disk";
    public static final String KEY_COMPUTER = "computer";
    public static final String KEY_ADVANCED_COMPUTER = "advanced_computer";
    public static final String KEY_MONITOR = "display_monitor";
    public static final String KEY_CRAFTER = "auto_crafter";
    public static final String KEY_TRANSPOSER = "inventory_transposer";
    public static final String KEY_SPEAKER = "sound_synthesizer";
    public static final String KEY_TURTLE = "programmable_turtle";

    private final JavaPlugin plugin;

    public RecipeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerAll(Material computerBlock) {
        registerAll(computerBlock, Material.ENCHANTING_TABLE, true);
    }

    public void registerAll(Material computerBlock, Material advancedBlock, boolean enableComputerRecipe) {
        unregisterAll();
        registerFloppyDisk();
        if (enableComputerRecipe) {
            registerComputer(computerBlock);
        }
        registerAdvancedComputer(computerBlock, advancedBlock);

        if (plugin instanceof MultiverseProgrammingPlugin mvPlugin) {
            ConfigManager cfg = mvPlugin.getConfigManager();
            registerMonitor(cfg.getMonitorBlock());
            registerCrafter(cfg.getCrafterBlock());
            registerTransposer(cfg.getTransposerBlock());
            registerSpeaker(cfg.getSpeakerBlock());
            registerTurtle(computerBlock, cfg.getTurtleBlock());
        } else {
            registerMonitor(Material.OCHRE_FROGLIGHT);
            registerCrafter(Material.CRAFTER);
            registerTransposer(Material.HOPPER);
            registerSpeaker(Material.NOTE_BLOCK);
            registerTurtle(computerBlock, Material.DISPENSER);
        }
    }

    public void unregisterAll() {
        safelyRemoveRecipe(new NamespacedKey(plugin, KEY_FLOPPY_DISK));
        safelyRemoveRecipe(new NamespacedKey(plugin, KEY_COMPUTER));
        safelyRemoveRecipe(new NamespacedKey(plugin, KEY_ADVANCED_COMPUTER));
        safelyRemoveRecipe(new NamespacedKey(plugin, KEY_MONITOR));
        safelyRemoveRecipe(new NamespacedKey(plugin, KEY_CRAFTER));
        safelyRemoveRecipe(new NamespacedKey(plugin, KEY_TRANSPOSER));
        safelyRemoveRecipe(new NamespacedKey(plugin, KEY_SPEAKER));
        safelyRemoveRecipe(new NamespacedKey(plugin, KEY_TURTLE));
    }

    private void safelyRemoveRecipe(NamespacedKey key) {
        try {
            plugin.getServer().removeRecipe(key);
        } catch (Throwable ignored) {
        }
    }

    private void registerFloppyDisk() {
        NamespacedKey key = new NamespacedKey(plugin, KEY_FLOPPY_DISK);
        safelyRemoveRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, DiskManager.createFloppyDisk());
        recipe.shape(" P ", "PIP", " P ");
        recipe.setIngredient('P', Material.PAPER);
        recipe.setIngredient('I', Material.IRON_INGOT);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerComputer(Material computerBlock) {
        if (computerBlock == null || !computerBlock.isItem()) {
            return;
        }
        NamespacedKey key = new NamespacedKey(plugin, KEY_COMPUTER);
        safelyRemoveRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, DiskManager.createComputer(computerBlock));
        if (computerBlock == Material.LECTERN) {
            recipe.shape("SSS", " B ", " S ");
            try {
                if (Tag.WOODEN_SLABS != null) {
                    recipe.setIngredient('S', new RecipeChoice.MaterialChoice(Tag.WOODEN_SLABS));
                } else {
                    recipe.setIngredient('S', new RecipeChoice.MaterialChoice(Material.OAK_SLAB));
                }
            } catch (Throwable t) {
                recipe.setIngredient('S', new RecipeChoice.MaterialChoice(Material.OAK_SLAB));
            }
            recipe.setIngredient('B', Material.BOOKSHELF);
        } else {
            recipe.shape(" I ", "ICI", " R ");
            recipe.setIngredient('I', Material.IRON_INGOT);
            recipe.setIngredient('C', computerBlock);
            recipe.setIngredient('R', Material.REDSTONE);
        }
        plugin.getServer().addRecipe(recipe);
    }

    private void registerAdvancedComputer(Material computerBlock, Material advancedBlock) {
        if (computerBlock == null || !computerBlock.isItem()) {
            plugin.getLogger().warning("[Recipe] Cannot register recipe for advanced computer: computer block "
                    + (computerBlock == null ? "null" : computerBlock.name()) + " is not an item.");
            return;
        }
        Material advMat = (advancedBlock != null && advancedBlock.isItem()) ? advancedBlock : Material.ENCHANTING_TABLE;

        NamespacedKey key = new NamespacedKey(plugin, KEY_ADVANCED_COMPUTER);
        safelyRemoveRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, DiskManager.createAdvancedComputer(advMat));
        recipe.shape(" C ", "DOD", "OOO");
        recipe.setIngredient('C', computerBlock);
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('O', Material.OBSIDIAN);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerMonitor(Material monitorBlock) {
        if (monitorBlock == null || !monitorBlock.isItem()) {
            return;
        }
        NamespacedKey key = new NamespacedKey(plugin, KEY_MONITOR);
        safelyRemoveRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, DiskManager.createMonitor(monitorBlock));
        recipe.shape("GGG", "GLG", "RRR");
        recipe.setIngredient('G', Material.GLASS);
        recipe.setIngredient('L', Material.GLOWSTONE);
        recipe.setIngredient('R', Material.REDSTONE);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerCrafter(Material crafterBlock) {
        if (crafterBlock == null || !crafterBlock.isItem()) {
            return;
        }
        NamespacedKey key = new NamespacedKey(plugin, KEY_CRAFTER);
        safelyRemoveRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, DiskManager.createCrafter(crafterBlock));
        recipe.shape("III", "ICI", "RDR");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('C', Material.CRAFTING_TABLE);
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('D', Material.DROPPER);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerTransposer(Material transposerBlock) {
        if (transposerBlock == null || !transposerBlock.isItem()) {
            return;
        }
        NamespacedKey key = new NamespacedKey(plugin, KEY_TRANSPOSER);
        safelyRemoveRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, DiskManager.createTransposer(transposerBlock));
        recipe.shape("I I", "ICI", " R ");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('C', Material.CHEST);
        recipe.setIngredient('R', Material.REDSTONE);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerSpeaker(Material speakerBlock) {
        if (speakerBlock == null || !speakerBlock.isItem()) {
            return;
        }
        NamespacedKey key = new NamespacedKey(plugin, KEY_SPEAKER);
        safelyRemoveRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, DiskManager.createSpeaker(speakerBlock));
        recipe.shape("PPP", "PNP", "PRP");
        recipe.setIngredient('P', Material.OAK_PLANKS);
        recipe.setIngredient('N', Material.NOTE_BLOCK);
        recipe.setIngredient('R', Material.REDSTONE);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerTurtle(Material computerBlock, Material turtleBlock) {
        if (turtleBlock == null || !turtleBlock.isItem() || computerBlock == null || !computerBlock.isItem()) {
            return;
        }
        NamespacedKey key = new NamespacedKey(plugin, KEY_TURTLE);
        safelyRemoveRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, DiskManager.createTurtle(turtleBlock));
        recipe.shape("ICI", "IPI", "IRI");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('C', Material.CHEST);
        recipe.setIngredient('P', computerBlock);
        recipe.setIngredient('R', Material.REDSTONE);
        plugin.getServer().addRecipe(recipe);
    }
}
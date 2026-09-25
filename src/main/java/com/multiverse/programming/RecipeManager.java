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
    public static final String KEY_SCANNER = "block_entity_scanner";
    public static final String KEY_CARTOGRAPHER = "cartographer_table";
    public static final String KEY_ALCHEMIST = "alchemical_synthesizer";
    public static final String KEY_FARMER = "farming_attachment";
    public static final String KEY_QUARRY = "quarry_excavator";
    public static final String KEY_NPC = "npc_dialogue_core";

    private final JavaPlugin plugin;

    public RecipeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerAll(Material computerBlock) {
        registerAll(computerBlock, Material.ENCHANTING_TABLE, true);
    }

    public void registerAll(Material computerBlock, Material advancedBlock, boolean enableComputerRecipe) {
        unregisterAll();

        if (plugin instanceof MultiverseProgrammingPlugin mvPlugin) {
            ConfigManager cfg = mvPlugin.getConfigManager();
            if (cfg.isEnableDiskRecipe()) {
                registerFloppyDisk();
            }
            if (cfg.isEnableComputerRecipe()) {
                registerComputer(computerBlock);
            }
            if (cfg.isEnableAdvancedComputerRecipe()) {
                registerAdvancedComputer(computerBlock, advancedBlock);
            }
            if (cfg.isEnableMonitorRecipe()) {
                registerMonitor(cfg.getMonitorBlock());
            }
            if (cfg.isEnableCrafterRecipe()) {
                registerCrafter(cfg.getCrafterBlock());
            }
            if (cfg.isEnableTransposerRecipe()) {
                registerTransposer(cfg.getTransposerBlock());
            }
            if (cfg.isEnableSpeakerRecipe()) {
                registerSpeaker(cfg.getSpeakerBlock());
            }
            if (cfg.isEnableTurtleRecipe()) {
                registerTurtle(computerBlock, cfg.getTurtleBlock());
            }
            if (cfg.isEnableScannerRecipe()) {
                registerScanner(cfg.getScannerBlock());
            }
            if (cfg.isEnableCartographerRecipe()) {
                registerCartographer(cfg.getCartographerBlock());
            }
            if (cfg.isEnableAlchemistRecipe()) {
                registerAlchemist(cfg.getAlchemistBlock());
            }
            if (cfg.isEnableFarmerRecipe()) {
                registerFarmer(cfg.getFarmerBlock());
            }
            if (cfg.isEnableQuarryRecipe()) {
                registerQuarry(cfg.getQuarryBlock());
            }
            if (cfg.isEnableNpcRecipe()) {
                registerNpc(cfg.getNpcBlock());
            }
        } else {
            registerFloppyDisk();
            if (enableComputerRecipe) {
                registerComputer(computerBlock);
            }
            registerAdvancedComputer(computerBlock, advancedBlock);
            registerMonitor(Material.OCHRE_FROGLIGHT);
            registerCrafter(Material.CRAFTER);
            registerTransposer(Material.HOPPER);
            registerSpeaker(Material.NOTE_BLOCK);
            registerTurtle(computerBlock, Material.DISPENSER);
            registerScanner(Material.OBSERVER);
            registerCartographer(Material.CARTOGRAPHY_TABLE);
            registerAlchemist(Material.BREWING_STAND);
            registerFarmer(Material.COMPOSTER);
            registerQuarry(Material.BLAST_FURNACE);
            registerNpc(Material.SCULK_CATALYST);
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
        safelyRemoveRecipe(new NamespacedKey(plugin, KEY_SCANNER));
        safelyRemoveRecipe(new NamespacedKey(plugin, KEY_CARTOGRAPHER));
        safelyRemoveRecipe(new NamespacedKey(plugin, KEY_ALCHEMIST));
        safelyRemoveRecipe(new NamespacedKey(plugin, KEY_FARMER));
        safelyRemoveRecipe(new NamespacedKey(plugin, KEY_QUARRY));
        safelyRemoveRecipe(new NamespacedKey(plugin, KEY_NPC));
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

    private void registerScanner(Material scannerBlock) {
        if (scannerBlock == null || !scannerBlock.isItem()) return;
        NamespacedKey key = new NamespacedKey(plugin, KEY_SCANNER);
        safelyRemoveRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, DiskManager.createScanner(scannerBlock));
        recipe.shape("CCC", " Q ", " R ");
        recipe.setIngredient('C', Material.COBBLESTONE);
        recipe.setIngredient('Q', Material.QUARTZ);
        recipe.setIngredient('R', Material.REDSTONE);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerCartographer(Material cartographerBlock) {
        if (cartographerBlock == null || !cartographerBlock.isItem()) return;
        NamespacedKey key = new NamespacedKey(plugin, KEY_CARTOGRAPHER);
        safelyRemoveRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, DiskManager.createCartographer(cartographerBlock));
        recipe.shape("PP ", "WW ", "WW ");
        recipe.setIngredient('P', Material.PAPER);
        recipe.setIngredient('W', Material.OAK_PLANKS);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerAlchemist(Material alchemistBlock) {
        if (alchemistBlock == null || !alchemistBlock.isItem()) return;
        NamespacedKey key = new NamespacedKey(plugin, KEY_ALCHEMIST);
        safelyRemoveRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, DiskManager.createAlchemist(alchemistBlock));
        recipe.shape(" B ", "CCC", " R ");
        recipe.setIngredient('B', Material.BLAZE_ROD);
        recipe.setIngredient('C', Material.COBBLESTONE);
        recipe.setIngredient('R', Material.REDSTONE);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerFarmer(Material farmerBlock) {
        if (farmerBlock == null || !farmerBlock.isItem()) return;
        NamespacedKey key = new NamespacedKey(plugin, KEY_FARMER);
        safelyRemoveRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, DiskManager.createFarmer(farmerBlock));
        recipe.shape("W W", "W W", "WWW");
        recipe.setIngredient('W', Material.OAK_SLAB);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerQuarry(Material quarryBlock) {
        if (quarryBlock == null || !quarryBlock.isItem()) return;
        NamespacedKey key = new NamespacedKey(plugin, KEY_QUARRY);
        safelyRemoveRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, DiskManager.createQuarry(quarryBlock));
        recipe.shape("III", "IFI", "SSS");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('F', Material.FURNACE);
        recipe.setIngredient('S', Material.SMOOTH_STONE);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerNpc(Material npcBlock) {
        if (npcBlock == null || !npcBlock.isItem()) return;
        NamespacedKey key = new NamespacedKey(plugin, KEY_NPC);
        safelyRemoveRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, DiskManager.createNpc(npcBlock));
        recipe.shape(" G ", "ACA", " R ");
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('A', Material.AMETHYST_SHARD);
        recipe.setIngredient('C', Material.BOOK);
        recipe.setIngredient('R', Material.REDSTONE);
        plugin.getServer().addRecipe(recipe);
    }
}
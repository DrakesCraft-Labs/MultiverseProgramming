// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class DiskManager {

    public static final NamespacedKey KEY_ID = new NamespacedKey("multiverseprogramming", "mvprog_id");

    public static final String ID_FLOPPY_DISK = "mvprog_floppy_disk";
    public static final String ID_COMPUTER = "mvprog_computer";
    public static final String ID_ADVANCED_COMPUTER = "mvprog_advanced_computer";
    public static final String ID_MONITOR = "mvprog_display_monitor";
    public static final String ID_CRAFTER = "mvprog_auto_crafter";
    public static final String ID_TRANSPOSER = "mvprog_inventory_transposer";
    public static final String ID_SPEAKER = "mvprog_sound_synthesizer";
    public static final String ID_TURTLE = "mvprog_programmable_turtle";
    public static final String ID_SCANNER = "mvprog_block_entity_scanner";
    public static final String ID_CARTOGRAPHER = "mvprog_cartographer_table";
    public static final String ID_ALCHEMIST = "mvprog_alchemical_synthesizer";
    public static final String ID_FARMER = "mvprog_farming_attachment";
    public static final String ID_QUARRY = "mvprog_quarry_excavator";
    public static final String ID_NPC = "mvprog_npc_dialogue_core";

    public static final String NAME = "Floppy Disk";
    public static final String COMPUTER_NAME = "Computer";
    public static final String ADVANCED_COMPUTER_NAME = "Advanced Computer";
    public static final String MONITOR_NAME = "Display Monitor";
    public static final String CRAFTER_NAME = "Auto-Crafter";
    public static final String TRANSPOSER_NAME = "Inventory Transposer";
    public static final String SPEAKER_NAME = "Sound Synthesizer";
    public static final String TURTLE_NAME = "Programmable Turtle";
    public static final String SCANNER_NAME = "§bBlock & Entity Scanner";
    public static final String CARTOGRAPHER_NAME = "§6Cartographer Table";
    public static final String ALCHEMIST_NAME = "§dAlchemical Synthesizer";
    public static final String FARMER_NAME = "§aFarming Attachment";
    public static final String QUARRY_NAME = "§cTurtle Quarry Engine";
    public static final String NPC_NAME = "§3NPC Dialogue Core";

    private DiskManager() {
    }

    private static void applyId(ItemMeta meta, String id) {
        if (meta == null || id == null) return;
        try {
            var pdc = meta.getPersistentDataContainer();
            if (pdc != null) {
                pdc.set(KEY_ID, PersistentDataType.STRING, id);
            }
        } catch (Throwable ignored) {}
    }

    public static String getMachineId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return null;
            var pdc = meta.getPersistentDataContainer();
            if (pdc == null) return null;
            return pdc.get(KEY_ID, PersistentDataType.STRING);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean isMachine(ItemStack item, String id) {
        String found = getMachineId(item);
        return found != null && found.equalsIgnoreCase(id);
    }

    public static ItemStack createFloppyDisk() {
        ItemStack disk = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta rawMeta = disk.getItemMeta();
        if (rawMeta instanceof BookMeta meta) {
            meta.setDisplayName(NAME);
            meta.setLore(List.of(
                    "§7Write your program on the pages.",
                    "§7Insert it into the computer's disk slot."
            ));
            meta.setPages("-- Write your program here\n-- Example:\n-- print(\"hello world\")");
            applyId(meta, ID_FLOPPY_DISK);
            disk.setItemMeta(meta);
        } else if (rawMeta != null) {
            rawMeta.setDisplayName(NAME);
            applyId(rawMeta, ID_FLOPPY_DISK);
            disk.setItemMeta(rawMeta);
        }
        return disk;
    }

    public static ItemStack createComputer() {
        return createComputer(Material.LECTERN);
    }

    public static ItemStack createComputer(Material material) {
        Material type = (material != null && material.isItem()) ? material : Material.LECTERN;
        ItemStack computer = new ItemStack(type);
        ItemMeta meta = computer.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(COMPUTER_NAME);
            meta.setLore(List.of("§7Place it and right-click it to open the GUI."));
            applyId(meta, ID_COMPUTER);
            computer.setItemMeta(meta);
        }
        return computer;
    }

    public static ItemStack createAdvancedComputer() {
        return createAdvancedComputer(Material.ENCHANTING_TABLE);
    }

    public static ItemStack createAdvancedComputer(Material material) {
        Material type = (material != null && material.isItem()) ? material : Material.ENCHANTING_TABLE;
        ItemStack computer = new ItemStack(type);
        ItemMeta meta = computer.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ADVANCED_COMPUTER_NAME);
            meta.setLore(List.of(
                    "§7Runs looping programs without the short timeout.",
                    "§7Keeps the disk inside its inventory."
            ));
            applyId(meta, ID_ADVANCED_COMPUTER);
            computer.setItemMeta(meta);
        }
        return computer;
    }

    public static ItemStack createMonitor() {
        return createMonitor(Material.OCHRE_FROGLIGHT);
    }

    public static ItemStack createMonitor(Material material) {
        Material type = (material != null && material.isItem()) ? material : Material.OCHRE_FROGLIGHT;
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MONITOR_NAME);
            meta.setLore(List.of(
                    "§7Place adjacent to a Computer.",
                    "§7Allows Lua scripts to project floating text in the world."
            ));
            applyId(meta, ID_MONITOR);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createCrafter() {
        return createCrafter(Material.CRAFTER);
    }

    public static ItemStack createCrafter(Material material) {
        Material type = (material != null && material.isItem()) ? material : Material.CRAFTER;
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CRAFTER_NAME);
            meta.setLore(List.of(
                    "§7Place adjacent to a Computer.",
                    "§7Automates Minecraft 1.21 crafting recipes via Lua."
            ));
            applyId(meta, ID_CRAFTER);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createTransposer() {
        return createTransposer(Material.HOPPER);
    }

    public static ItemStack createTransposer(Material material) {
        Material type = (material != null && material.isItem()) ? material : Material.HOPPER;
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(TRANSPOSER_NAME);
            meta.setLore(List.of(
                    "§7Place adjacent to a Computer.",
                    "§7Inspects and moves items between adjacent chests and containers."
            ));
            applyId(meta, ID_TRANSPOSER);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createSpeaker() {
        return createSpeaker(Material.NOTE_BLOCK);
    }

    public static ItemStack createSpeaker(Material material) {
        Material type = (material != null && material.isItem()) ? material : Material.NOTE_BLOCK;
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(SPEAKER_NAME);
            meta.setLore(List.of(
                    "§7Place adjacent to a Computer.",
                    "§7Plays musical notes, custom frequencies, and audio effects."
            ));
            applyId(meta, ID_SPEAKER);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createTurtle() {
        return createTurtle(Material.DISPENSER);
    }

    public static ItemStack createTurtle(Material material) {
        Material type = (material != null && material.isItem()) ? material : Material.DISPENSER;
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(TURTLE_NAME);
            meta.setLore(List.of(
                    "§7Mobile robotic computer & constructor.",
                    "§7Place it and right-click to open its GUI.",
                    "§7Connects with Web Portal & builds Blueprints."
            ));
            applyId(meta, ID_TURTLE);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createScanner(Material material) {
        Material type = (material != null && material.isItem()) ? material : Material.OBSERVER;
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(SCANNER_NAME);
            meta.setLore(List.of(
                    "§7Place adjacent to a Computer or Turtle.",
                    "§7Scans entities, players, and blocks within a radius via Lua."
            ));
            applyId(meta, ID_SCANNER);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createCartographer(Material material) {
        Material type = (material != null && material.isItem()) ? material : Material.CARTOGRAPHY_TABLE;
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CARTOGRAPHER_NAME);
            meta.setLore(List.of(
                    "§7Place adjacent to a Computer.",
                    "§7Scans terrain topography and generates custom in-game maps."
            ));
            applyId(meta, ID_CARTOGRAPHER);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createAlchemist(Material material) {
        Material type = (material != null && material.isItem()) ? material : Material.BREWING_STAND;
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ALCHEMIST_NAME);
            meta.setLore(List.of(
                    "§7Place adjacent to a Computer.",
                    "§7Automates potion synthesis and alchemy from connected containers."
            ));
            applyId(meta, ID_ALCHEMIST);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createFarmer(Material material) {
        Material type = (material != null && material.isItem()) ? material : Material.COMPOSTER;
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(FARMER_NAME);
            meta.setLore(List.of(
                    "§7Place adjacent to a Computer.",
                    "§7Inspects crop maturity, auto-harvests, and replants crops."
            ));
            applyId(meta, ID_FARMER);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createQuarry(Material material) {
        Material type = (material != null && material.isItem()) ? material : Material.BLAST_FURNACE;
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(QUARRY_NAME);
            meta.setLore(List.of(
                    "§7Autonomous excavation upgrade for the Turtle.",
                    "§7Place laterally (left or right) of a Turtle.",
                    "§7Enables volumetric strip-mining via turtle.quarry().",
                    "§cOnly functions when attached to a Turtle."
            ));
            applyId(meta, ID_QUARRY);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createNpc(Material material) {
        Material type = (material != null && material.isItem()) ? material : Material.SCULK_CATALYST;
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(NPC_NAME);
            meta.setLore(List.of(
                    "§7Interactive NPC & quest dialogue core.",
                    "§7Projects floating holograms and prompts players with choices."
            ));
            applyId(meta, ID_NPC);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isDisk(ItemStack item) {
        if (item == null) {
            return false;
        }
        Material type = item.getType();
        return type == Material.WRITABLE_BOOK || type == Material.WRITTEN_BOOK;
    }

    public static String readProgram(ItemStack disk) {
        if (!isDisk(disk)) {
            return "";
        }
        ItemMeta rawMeta = disk.getItemMeta();
        if (!(rawMeta instanceof BookMeta meta) || !meta.hasPages()) {
            return "";
        }
        List<String> validPages = new ArrayList<>();
        for (String page : meta.getPages()) {
            if (page != null) {
                validPages.add(page);
            }
        }
        return String.join("\n", validPages);
    }
}
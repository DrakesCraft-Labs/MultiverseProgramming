// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.turtle;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Interactive Supply Station Terminal GUI.
 * Allows players to inspect Turtle status, pause/resume work, re-validate chest inventories,
 * and stop active operations without having to follow or search for the physical turtle.
 */
public final class SupplyStationGUI {

    public static final String TITLE_PREFIX = "Supply Station: ";
    public static final int SIZE = 27;

    public static final int SLOT_INFO = 4;
    public static final int SLOT_RESUME = 11;
    public static final int SLOT_PAUSE = 13;
    public static final int SLOT_STOP = 15;

    private SupplyStationGUI() {
    }

    public static Inventory create(Turtle turtle) {
        String title = TITLE_PREFIX + turtle.getId();
        Inventory inv = Bukkit.createInventory(null, SIZE, title);
        refresh(inv, turtle);
        return inv;
    }

    public static void open(Player player, Turtle turtle) {
        if (player == null || turtle == null) return;
        player.openInventory(create(turtle));
    }

    public static void refresh(Inventory inv, Turtle turtle) {
        if (inv == null || turtle == null) return;

        // Background filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < SIZE; i++) {
            if (i != SLOT_INFO && i != SLOT_RESUME && i != SLOT_PAUSE && i != SLOT_STOP) {
                inv.setItem(i, filler.clone());
            }
        }

        // Slot 4: Info display
        ItemStack infoItem = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§b🖥️ Supply Station Terminal §8- §f" + turtle.getId());
            List<String> lore = new ArrayList<>();
            lore.add("§7Owner: §f" + turtle.getOwnerName());
            lore.add("§7Status: " + getStatusBadge(turtle.getStatus()));
            lore.add("§7Message: §f" + turtle.getStatusMessage());
            lore.add("§7Fuel: §e" + turtle.getFuel() + " §7points");

            if (turtle.isBuilding()) {
                lore.add("§8─────────────────────────");
                String bpName = turtle.getActiveBlueprint() != null ? turtle.getActiveBlueprint().name() : "N/A";
                lore.add("§7Blueprint: §a" + bpName);
                lore.add(String.format(Locale.ROOT, "§7Progress: §e%d / %d §8(§b%.1f%%§8)",
                        turtle.getCurrentBlockIndex(), turtle.getTotalBlocks(), turtle.getProgressPercentage()));
                Material needed = turtle.getCurrentNeededMaterial();
                if (needed != null) {
                    lore.add("§7Current Needed Block: §f" + needed.name());
                }
            } else if (turtle.isQuarryActive()) {
                lore.add("§8─────────────────────────");
                lore.add("§7Quarry Depth: §eY=" + turtle.getQuarryCurrentY() + " §8(Target: Y=" + turtle.getQuarryTargetY() + ")");
                lore.add(String.format(Locale.ROOT, "§7Blocks Mined: §e%d / %d §8(§b%.1f%%§8)",
                        turtle.getQuarryBlocksMined(), turtle.getQuarryTotalBlocks(), turtle.getQuarryProgressPercentage()));
            }
            infoMeta.setLore(lore);
            infoItem.setItemMeta(infoMeta);
        }
        inv.setItem(SLOT_INFO, infoItem);

        // Slot 11: Re-validate & Resume button
        ItemStack resumeItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta resumeMeta = resumeItem.getItemMeta();
        if (resumeMeta != null) {
            resumeMeta.setDisplayName("§a✔ Re-validate Inventories & Resume");
            resumeMeta.setLore(List.of(
                    "§7Re-scans material chests, fuel chests,",
                    "§7and the Turtle's internal inventory.",
                    "",
                    "§7If missing materials or fuel were deposited,",
                    "§7operations will immediately resume!",
                    "",
                    "§e▶ Click to validate & resume work"
            ));
            resumeItem.setItemMeta(resumeMeta);
        }
        inv.setItem(SLOT_RESUME, resumeItem);

        // Slot 13: Pause button
        ItemStack pauseItem = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta pauseMeta = pauseItem.getItemMeta();
        if (pauseMeta != null) {
            pauseMeta.setDisplayName("§e⏸ Pause Operation");
            pauseMeta.setLore(List.of(
                    "§7Temporarily pauses active construction",
                    "§7or quarry excavation safely.",
                    "",
                    "§e▶ Click to pause work"
            ));
            pauseItem.setItemMeta(pauseMeta);
        }
        inv.setItem(SLOT_PAUSE, pauseItem);

        // Slot 15: Stop button
        ItemStack stopItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta stopMeta = stopItem.getItemMeta();
        if (stopMeta != null) {
            stopMeta.setDisplayName("§c⏹ Stop & Cancel Work");
            stopMeta.setLore(List.of(
                    "§7Aborts and cancels the current blueprint",
                    "§7or quarry job completely.",
                    "",
                    "§c▶ Click to force stop work"
            ));
            stopItem.setItemMeta(stopMeta);
        }
        inv.setItem(SLOT_STOP, stopItem);
    }

    private static String getStatusBadge(Turtle.Status status) {
        if (status == null) return "§7IDLE";
        return switch (status) {
            case BUILDING -> "§aBUILDING";
            case MINING -> "§6MINING";
            case MOVING -> "§bMOVING";
            case PAUSED -> "§ePAUSED";
            case ERROR -> "§cERROR";
            case IDLE -> "§7IDLE";
        };
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.turtle;

import com.multiverse.programming.DiskManager;
import com.multiverse.programming.LuaRunner;
import com.multiverse.programming.MultiverseProgrammingPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;

/**
 * Handles the interactive chest GUI for Turtles.
 * Features 16-slot item inventory, disk drive, execution control,
 * fuel indicators, and Web Portal dispatch buttons.
 */
public final class TurtleGUI implements InventoryHolder {

    public static final int INVENTORY_SIZE = 54;
    public static final int DISK_SLOT = 0;
    public static final int RUN_BUTTON_SLOT = 1;
    public static final int WEB_BUTTON_SLOT = 2;
    public static final int BUILD_BUTTON_SLOT = 3;
    public static final int FUEL_SLOT = 4;

    // Classic 4x4 Grid in double chest GUI
    public static final int[] TURTLE_SLOTS = {
            19, 20, 21, 22,
            28, 29, 30, 31,
            37, 38, 39, 40,
            46, 47, 48, 49
    };

    private final Turtle turtle;
    private final Inventory inventory;

    public TurtleGUI(Turtle turtle) {
        this.turtle = turtle;
        this.inventory = Bukkit.createInventory(this, INVENTORY_SIZE, "Turtle: " + turtle.getId());
        setupGUI();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Turtle getTurtle() {
        return turtle;
    }

    public void setupGUI() {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // 1. Insert disk
        ItemStack disk = turtle.getDisk();
        if (disk != null) {
            inventory.setItem(DISK_SLOT, disk);
        } else {
            inventory.setItem(DISK_SLOT, null); // empty for insertion
        }

        // 2. Run Button
        inventory.setItem(RUN_BUTTON_SLOT, createItem(
                Material.EMERALD,
                "§a✔ Run Program",
                List.of("§7Execute Lua program from the disk slot.", "§7Provides global 'turtle' API.")
        ));

        // 3. Web Dashboard Link Button
        inventory.setItem(WEB_BUTTON_SLOT, createItem(
                Material.COMPASS,
                "§b🌐 Web Dashboard",
                List.of(
                        "§7Click to get the Web Portal URL in chat.",
                        "§7Upload .litematic & .nbt blueprints",
                        "§7and dispatch builds directly to this Turtle!"
                )
        ));

        // 4. Operation / Build Status Button
        String statusText;
        double pct;
        if (turtle.getStatus() == Turtle.Status.BUILDING) {
            statusText = "§e⏳ Building...";
            pct = turtle.getProgressPercentage();
        } else if (turtle.getStatus() == Turtle.Status.MINING) {
            statusText = "§6⛏ Mining (Quarry)...";
            pct = turtle.getQuarryProgressPercentage();
        } else if (turtle.getStatus() == Turtle.Status.PAUSED) {
            statusText = "§e⏸ Paused";
            pct = turtle.isQuarryPaused() ? turtle.getQuarryProgressPercentage() : turtle.getProgressPercentage();
        } else {
            statusText = "§7Status: " + turtle.getStatus().name();
            pct = 0.0;
        }

        inventory.setItem(BUILD_BUTTON_SLOT, createItem(
                Material.ANVIL,
                "§6🏗 Task Control",
                List.of(
                        statusText,
                        "§7" + turtle.getStatusMessage(),
                        String.format(Locale.ROOT, "§7Progress: %.1f%%", pct),
                        "§8Click to pause / resume active task."
                )
        ));

        // 5. Fuel Indicator
        inventory.setItem(FUEL_SLOT, createItem(
                Material.BLAZE_POWDER,
                "§e⚡ Fuel: " + turtle.getFuel(),
                List.of("§7Click with coal or lava to refuel.")
        ));

        // 6. Populate 16 Turtle Inventory Slots
        for (int i = 0; i < 16; i++) {
            int guiSlot = TURTLE_SLOTS[i];
            ItemStack item = turtle.getItem(i);
            inventory.setItem(guiSlot, item);
        }
    }

    public void saveToTurtle() {
        // Save disk
        ItemStack diskItem = inventory.getItem(DISK_SLOT);
        turtle.setDisk(diskItem);

        // Save 16 inventory items
        for (int i = 0; i < 16; i++) {
            int guiSlot = TURTLE_SLOTS[i];
            ItemStack item = inventory.getItem(guiSlot);
            turtle.setItem(i, item);
        }
    }

    public static boolean isTurtleSlot(int slot) {
        for (int s : TURTLE_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    public static int getTurtleSlotIndex(int guiSlot) {
        for (int i = 0; i < TURTLE_SLOTS.length; i++) {
            if (TURTLE_SLOTS[i] == guiSlot) return i;
        }
        return -1;
    }

    private static ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createItem(Material mat, String name) {
        return createItem(mat, name, null);
    }
}

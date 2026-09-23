// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class ComputerGUI {

    public static final String TITLE = "Computer";
    public static final String ADVANCED_TITLE = "Advanced Computer";
    public static final int DISK_SLOT = 0;
    public static final int BUTTON_SLOT = 8;

    private ComputerGUI() {
    }

    public static Inventory open() {
        return open(TITLE, "§a✔ Validate & Run");
    }

    public static Inventory openAdvanced() {
        return open(ADVANCED_TITLE, "§a✔ Run / Stop");
    }

    private static Inventory open(String title, String buttonLabel) {
        Inventory inv = Bukkit.createInventory(null, 9, title);

        ItemStack frame = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta frameMeta = frame.getItemMeta();
        frameMeta.setDisplayName(" ");
        frame.setItemMeta(frameMeta);

        for (int i = 1; i < BUTTON_SLOT; i++) {
            inv.setItem(i, frame.clone());
        }

        ItemStack button = new ItemStack(Material.EMERALD);
        ItemMeta buttonMeta = button.getItemMeta();
        buttonMeta.setDisplayName(buttonLabel);
        buttonMeta.setLore(List.of("§7Checks the disk's code for errors."));
        button.setItemMeta(buttonMeta);
        inv.setItem(BUTTON_SLOT, button);

        return inv;
    }

    public static void pressButton(Player player, Inventory inv, String prefix, long timeoutMs) {
        ItemStack disk = inv.getItem(DISK_SLOT);
        if (disk == null || disk.getType().isAir()) {
            player.sendMessage(prefix + " §cThere is no floppy disk in the slot.");
            return;
        }

        String code = DiskManager.readProgram(disk);
        String error = LuaRunner.validate(code);
        player.closeInventory();

        if (error != null) {
            player.sendMessage(prefix + " §cError in the code:");
            for (String line : error.split("\n")) {
                player.sendMessage(" §4✘ " + line);
            }
            return;
        }

        player.sendMessage(prefix + " §7Running program…");
        LuaRunner.Result result = LuaRunner.execute(code, timeoutMs);

        if (!result.output().isEmpty()) {
            for (String line : result.output().split("\n")) {
                player.sendMessage("§f" + line);
            }
        } else if (result.ok()) {
            player.sendMessage(prefix + " §7(no output)");
        }
    }
}
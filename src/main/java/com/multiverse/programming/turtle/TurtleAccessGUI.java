// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.turtle;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Owner-only GUI to manage who may open and operate a Turtle.
 *
 * <p>Top row (slot 0 + slots 1..8): the owner (locked) and the players currently
 * authorized. Clicking an authorized player revokes access.</p>
 *
 * <p>Bottom rows (slots 18..53): online players that are not yet authorized.
 * Clicking one grants access.</p>
 */
public final class TurtleAccessGUI {

    public static final String TITLE_PREFIX = "Turtle Access: ";
    public static final int SIZE = 54;

    public static final int OWNER_SLOT = 0;
    public static final int FIRST_AUTHORIZED_SLOT = 1;
    public static final int AUTHORIZED_SLOTS = 8; // slots 1..8
    public static final int INFO_SLOT = 13;
    public static final int FIRST_ONLINE_SLOT = 18; // slots 18..53

    /** PDC key used to tag the player head items with the UUID they represent. */
    public static final NamespacedKey ACCESS_UUID_KEY =
            new NamespacedKey("multiverseprogramming", "turtle_access_uuid");

    private TurtleAccessGUI() {
    }

    public static void open(Player player, Turtle turtle) {
        if (player == null || turtle == null) return;
        TurtleAccessHolder holder = new TurtleAccessHolder(turtle);
        Inventory inv = Bukkit.createInventory(holder, SIZE, TITLE_PREFIX + turtle.getId());
        holder.setInventory(inv);
        refresh(inv, turtle);
        player.openInventory(inv);
    }

    public static void refresh(Inventory inv, Turtle turtle) {
        if (inv == null || turtle == null) return;

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, filler);
        }

        // Slot 0: owner (locked)
        UUID owner = turtle.getOwner();
        if (owner != null) {
            inv.setItem(OWNER_SLOT, createHead(owner,
                    "§6★ Owner: §f" + turtle.getOwnerName(),
                    List.of("§7Permanent access.", "§8This entry cannot be removed.")));
        } else {
            inv.setItem(OWNER_SLOT, createItem(Material.BARRIER,
                    "§cNo owner registered",
                    List.of("§7This Turtle has no owner, so access", "§7is open to everyone with permission.")));
        }

        // Slots 1..8: authorized players (click to revoke)
        List<UUID> authorized = turtle.getAuthorizedPlayers();
        if (authorized == null) authorized = List.of();
        for (int i = 0; i < AUTHORIZED_SLOTS && i < authorized.size(); i++) {
            UUID uuid = authorized.get(i);
            inv.setItem(FIRST_AUTHORIZED_SLOT + i, createHead(uuid,
                    "§a" + nameOf(uuid),
                    List.of("§7Authorized to open and operate", "§7this Turtle.",
                            "", "§c▶ Click to revoke access")));
        }
        if (authorized.size() > AUTHORIZED_SLOTS) {
            int overflowSlot = FIRST_AUTHORIZED_SLOT + AUTHORIZED_SLOTS - 1;
            inv.setItem(overflowSlot, createItem(Material.BOOK,
                    "§e+" + (authorized.size() - AUTHORIZED_SLOTS + 1) + " more authorized players",
                    List.of("§7Not all players fit on this screen.")));
        }

        // Info item
        inv.setItem(INFO_SLOT, createItem(Material.WRITTEN_BOOK,
                "§b👥 Access Control",
                List.of("§7Green heads (top): authorized players.",
                        "§7Click a green head to §crevoke §7access.",
                        "",
                        "§7Bottom: online players without access.",
                        "§7Click a player to §aautorize §7them.",
                        "",
                        "§8Authorized players can open this Turtle,",
                        "§8run scripts, refuel and use the terminal.")));

        // Slots 18..53: online players available to authorize
        int slot = FIRST_ONLINE_SLOT;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= SIZE) break;
            if (online == null) continue;
            UUID uuid = online.getUniqueId();
            if (uuid.equals(owner) || authorized.contains(uuid)) continue;
            inv.setItem(slot++, createHead(uuid,
                    "§7" + online.getName(),
                    List.of("§7Click to authorize this player", "§7to open and operate this Turtle.")));
        }
    }

    /**
     * Reads the player UUID encoded on an access GUI head item.
     *
     * @return the UUID, or null if the item is not an access entry
     */
    public static UUID readTargetUuid(ItemStack item) {
        if (item == null) return null;
        Material type = item.getType();
        if (type == null || type.isAir()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String raw = meta.getPersistentDataContainer().get(ACCESS_UUID_KEY, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String nameOf(UUID uuid) {
        if (uuid == null) return "Unknown";
        try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            if (op != null && op.getName() != null) return op.getName();
        } catch (Throwable ignored) {
        }
        return uuid.toString();
    }

    private static ItemStack createHead(UUID uuid, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        if (meta instanceof SkullMeta skull && uuid != null) {
            try {
                skull.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            } catch (Throwable ignored) {
            }
        }
        meta.setDisplayName(name);
        meta.setLore(lore);
        if (uuid != null) {
            meta.getPersistentDataContainer().set(ACCESS_UUID_KEY, PersistentDataType.STRING, uuid.toString());
        }
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(new ArrayList<>(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}

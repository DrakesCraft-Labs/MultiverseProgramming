// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.protection;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Soft reflection bridge for CoreProtect.
 * Logs block placement and removal executed autonomously by Turtles,
 * attributing actions to the player who owns or dispatched the turtle.
 */
public final class CoreProtectBridge {

    private static volatile boolean checked = false;
    private static Object coreProtectAPI = null;
    private static Method logPlacementMethod = null;
    private static Method logRemovalMethod = null;

    private CoreProtectBridge() {}

    private static void ensureInit() {
        if (checked && coreProtectAPI != null) {
            return;
        }
        synchronized (CoreProtectBridge.class) {
            if (checked && coreProtectAPI != null) {
                return;
            }
            try {
                Plugin plugin = Bukkit.getPluginManager().getPlugin("CoreProtect");
                if (plugin != null && plugin.isEnabled()) {
                    Method getAPIMethod = plugin.getClass().getMethod("getAPI");
                    Object api = getAPIMethod.invoke(plugin);
                    if (api != null) {
                        try {
                            Method isEnabledMethod = api.getClass().getMethod("isEnabled");
                            Boolean enabled = (Boolean) isEnabledMethod.invoke(api);
                            if (enabled != null && !enabled) {
                                return;
                            }
                        } catch (Throwable ignored) {}

                        logPlacementMethod = api.getClass().getMethod("logPlacement", String.class, Location.class, Material.class, BlockData.class);
                        logRemovalMethod = api.getClass().getMethod("logRemoval", String.class, Location.class, Material.class, BlockData.class);
                        coreProtectAPI = api;
                    }
                }
            } catch (Throwable ignored) {
                coreProtectAPI = null;
            } finally {
                checked = true;
            }
        }
    }

    /**
     * Logs a block placement performed by a Turtle.
     *
     * @param owner    The UUID of the Turtle owner (player who placed/dispatched it).
     * @param turtleId The identifier of the Turtle.
     * @param loc      The location of the placed block.
     * @param mat      The material of the placed block.
     * @param data     The block data of the placed block (optional).
     */
    public static void logPlacement(UUID owner, String turtleId, Location loc, Material mat, BlockData data) {
        ensureInit();
        if (coreProtectAPI == null || logPlacementMethod == null || loc == null || mat == null) {
            return;
        }
        try {
            String actor = resolveActor(owner, turtleId);
            BlockData bd = data != null ? data : (mat.isBlock() ? mat.createBlockData() : null);
            logPlacementMethod.invoke(coreProtectAPI, actor, loc, mat, bd);
        } catch (Throwable ignored) {}
    }

    /**
     * Logs a block removal performed by a Turtle (e.g. dig or blueprint clear).
     *
     * @param owner    The UUID of the Turtle owner.
     * @param turtleId The identifier of the Turtle.
     * @param loc      The location of the removed block.
     * @param mat      The material of the block before removal.
     * @param data     The block data before removal.
     */
    public static void logRemoval(UUID owner, String turtleId, Location loc, Material mat, BlockData data) {
        ensureInit();
        if (coreProtectAPI == null || logRemovalMethod == null || loc == null || mat == null) {
            return;
        }
        try {
            String actor = resolveActor(owner, turtleId);
            BlockData bd = data != null ? data : (mat.isBlock() ? mat.createBlockData() : null);
            logRemovalMethod.invoke(coreProtectAPI, actor, loc, mat, bd);
        } catch (Throwable ignored) {}
    }

    private static String resolveActor(UUID owner, String turtleId) {
        if (owner != null) {
            try {
                OfflinePlayer op = Bukkit.getOfflinePlayer(owner);
                if (op.getName() != null && !op.getName().isBlank()) {
                    return op.getName();
                }
            } catch (Throwable ignored) {}
        }
        return turtleId != null ? ("#Turtle-" + turtleId) : "#Turtle";
    }
}

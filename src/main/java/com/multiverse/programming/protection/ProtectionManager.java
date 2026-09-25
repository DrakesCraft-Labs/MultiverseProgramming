// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.protection;

import com.multiverse.programming.MultiverseProgrammingPlugin;
import com.multiverse.programming.blueprint.Blueprint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles region protection checks for WorldGuard and ProtectionStones using reflection.
 * Ensures soft-dependency safety without hard class dependencies.
 */
public final class ProtectionManager {

    private final MultiverseProgrammingPlugin plugin;

    public ProtectionManager(MultiverseProgrammingPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isWorldGuardPresent() {
        return Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
    }

    public boolean isProtectionStonesPresent() {
        return Bukkit.getPluginManager().isPluginEnabled("ProtectionStones");
    }

    /**
     * Checks if a player has permission to build at a specific location, respecting WorldGuard
     * and ProtectionStones region ownership and membership.
     *
     * @param playerUuid The UUID of the player who placed the Turtle.
     * @param loc        The target block location.
     * @return true if building is allowed, false if blocked by a protection plugin.
     */
    public boolean canBuildAt(UUID playerUuid, Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }

        // Admin bypass
        if (playerUuid != null) {
            Player onlinePlayer = Bukkit.getPlayer(playerUuid);
            if (onlinePlayer != null && onlinePlayer.hasPermission("multiverseprogramming.admin")) {
                return true;
            }
        }

        boolean requireStrictOwner = plugin.getConfigManager().isProtectionStonesRequireOwner();

        // 1. ProtectionStones Check
        if (isProtectionStonesPresent()) {
            Boolean psAllowed = checkProtectionStones(playerUuid, loc, requireStrictOwner);
            if (psAllowed != null && !psAllowed) {
                return false;
            }
        }

        // 2. WorldGuard Check
        if (isWorldGuardPresent() && plugin.getConfigManager().isWorldGuardProtectionCheck()) {
            Boolean wgAllowed = checkWorldGuard(playerUuid, loc, requireStrictOwner);
            if (wgAllowed != null && !wgAllowed) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validates whether all blocks in a blueprint placement are permissible for the player.
     *
     * @return null if allowed, or an error message explaining the protection obstruction.
     */
    public String checkBuildArea(UUID playerUuid, Location origin, Blueprint blueprint) {
        if (origin == null || origin.getWorld() == null || blueprint == null) {
            return "Invalid build location or blueprint.";
        }

        // Quick check: if neither protection plugin is present, allow immediately
        if (!isWorldGuardPresent() && !isProtectionStonesPresent()) {
            return null;
        }

        if (playerUuid != null) {
            Player onlinePlayer = Bukkit.getPlayer(playerUuid);
            if (onlinePlayer != null && onlinePlayer.hasPermission("multiverseprogramming.admin")) {
                return null;
            }
        }

        // Sample bounding box corners and blocks
        for (Blueprint.PlacementBlock block : blueprint.blocks()) {
            Location checkLoc = origin.clone().add(block.x(), block.y(), block.z());
            if (!canBuildAt(playerUuid, checkLoc)) {
                boolean requireStrictOwner = plugin.getConfigManager().isProtectionStonesRequireOwner();
                if (requireStrictOwner) {
                    return "Target area contains protected regions (WorldGuard/ProtectionStones). You must be the OWNER of the region to build here.";
                } else {
                    return "Target area contains protected regions (WorldGuard/ProtectionStones). You must be an OWNER or MEMBER of the region to build here.";
                }
            }
        }

        return null;
    }

    private Boolean checkProtectionStones(UUID playerUuid, Location loc, boolean requireStrictOwner) {
        try {
            Class<?> psClass = Class.forName("dev.espi.protectionstones.ProtectionStones");
            Method getRegionMethod = psClass.getMethod("getRegion", Location.class);
            Object psRegion = getRegionMethod.invoke(null, loc);
            if (psRegion == null) {
                return null; // Not in a ProtectionStones region
            }

            if (playerUuid == null) {
                return false;
            }

            // Check isOwner
            Method isOwnerMethod = psRegion.getClass().getMethod("isOwner", UUID.class);
            boolean isOwner = (boolean) isOwnerMethod.invoke(psRegion, playerUuid);
            if (isOwner) {
                return true;
            }

            if (requireStrictOwner) {
                return false;
            }

            // Check isMember
            Method isMemberMethod = psRegion.getClass().getMethod("isMember", UUID.class);
            boolean isMember = (boolean) isMemberMethod.invoke(psRegion, playerUuid);
            return isMember;
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.FINE, "Error checking ProtectionStones region: " + t.getMessage(), t);
            return null;
        }
    }

    private Boolean checkWorldGuard(UUID playerUuid, Location loc, boolean requireStrictOwner) {
        try {
            // WorldGuard 7.x
            Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Method getInstanceMethod = wgClass.getMethod("getInstance");
            Object wgInstance = getInstanceMethod.invoke(null);

            Method getPlatformMethod = wgInstance.getClass().getMethod("getPlatform");
            Object platform = getPlatformMethod.invoke(wgInstance);

            Method getRegionContainerMethod = platform.getClass().getMethod("getRegionContainer");
            Object regionContainer = getRegionContainerMethod.invoke(platform);

            Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Method adaptWorldMethod = bukkitAdapterClass.getMethod("adapt", org.bukkit.World.class);
            Object weWorld = adaptWorldMethod.invoke(null, loc.getWorld());

            Method getRegionManagerMethod = regionContainer.getClass().getMethod("get", Class.forName("com.sk89q.worldedit.world.World"));
            Object regionManager = getRegionManagerMethod.invoke(regionContainer, weWorld);
            if (regionManager == null) {
                return null;
            }

            Method asBlockVectorMethod = bukkitAdapterClass.getMethod("asBlockVector", Location.class);
            Object blockVector = asBlockVectorMethod.invoke(null, loc);

            Method getApplicableRegionsMethod = regionManager.getClass().getMethod("getApplicableRegions", Class.forName("com.sk89q.worldedit.math.BlockVector3"));
            Object applicableSet = getApplicableRegionsMethod.invoke(regionManager, blockVector);

            Method sizeMethod = applicableSet.getClass().getMethod("size");
            int size = (int) sizeMethod.invoke(applicableSet);
            if (size == 0) {
                return null; // Wilderness, no region here
            }

            if (playerUuid == null) {
                return false;
            }

            // Check membership / ownership in the region set
            Class<?> wgPluginClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
            Method instMethod = wgPluginClass.getMethod("inst");
            Object wgPlugin = instMethod.invoke(null);

            Player player = Bukkit.getPlayer(playerUuid);
            Object localPlayer = null;
            if (player != null) {
                Method wrapPlayerMethod = wgPlugin.getClass().getMethod("wrapPlayer", Player.class);
                localPlayer = wrapPlayerMethod.invoke(wgPlugin, player);
            }

            if (localPlayer != null) {
                Class<?> localPlayerClass = Class.forName("com.sk89q.worldguard.LocalPlayer");
                if (requireStrictOwner) {
                    Method isOwnerOfAllMethod = applicableSet.getClass().getMethod("isOwnerOfAll", localPlayerClass);
                    return (boolean) isOwnerOfAllMethod.invoke(applicableSet, localPlayer);
                } else {
                    Method isMemberOfAllMethod = applicableSet.getClass().getMethod("isMemberOfAll", localPlayerClass);
                    boolean isMember = (boolean) isMemberOfAllMethod.invoke(applicableSet, localPlayer);
                    if (isMember) return true;

                    Method isOwnerOfAllMethod = applicableSet.getClass().getMethod("isOwnerOfAll", localPlayerClass);
                    return (boolean) isOwnerOfAllMethod.invoke(applicableSet, localPlayer);
                }
            } else {
                // Offline player: check UUID in ProtectedRegions
                Method getRegionsMethod = applicableSet.getClass().getMethod("getRegions");
                Collection<?> regions = (Collection<?>) getRegionsMethod.invoke(applicableSet);
                for (Object r : regions) {
                    Method getOwnersMethod = r.getClass().getMethod("getOwners");
                    Object domain = getOwnersMethod.invoke(r);
                    Method containsMethod = domain.getClass().getMethod("contains", UUID.class);
                    boolean isOwner = (boolean) containsMethod.invoke(domain, playerUuid);
                    if (isOwner) continue;

                    if (requireStrictOwner) return false;

                    Method getMembersMethod = r.getClass().getMethod("getMembers");
                    Object memDomain = getMembersMethod.invoke(r);
                    Method memContainsMethod = memDomain.getClass().getMethod("contains", UUID.class);
                    boolean isMember = (boolean) memContainsMethod.invoke(memDomain, playerUuid);
                    if (!isMember) return false;
                }
                return true;
            }
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.FINE, "Error checking WorldGuard region: " + t.getMessage(), t);
            return null;
        }
    }
}

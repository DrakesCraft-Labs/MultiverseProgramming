// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.turtle;

import com.multiverse.programming.MultiverseProgrammingPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages all active Programmable Turtles in the server, with file persistence in turtles.yml.
 */
public final class TurtleManager {

    private final MultiverseProgrammingPlugin plugin;
    private final File dataFile;
    private final Map<Location, Turtle> turtlesByLocation = new ConcurrentHashMap<>();
    private final Map<String, Turtle> turtlesById = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    public TurtleManager(MultiverseProgrammingPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.dataFile = (plugin.getDataFolder() != null) ? new File(plugin.getDataFolder(), "turtles.yml") : null;
    }

    public synchronized Turtle createTurtle(Location location, BlockFace facing, UUID owner) {
        Location blockLoc = normalizeLocation(location);
        Turtle existing = turtlesByLocation.get(blockLoc);
        if (existing != null) {
            if (owner != null && existing.getOwner() == null) {
                existing.setOwner(owner);
                saveAll();
            }
            return existing;
        }

        String id = String.format("T-%03d", idCounter.getAndIncrement());
        Turtle turtle = new Turtle(plugin, id, blockLoc, facing, owner);
        turtlesByLocation.put(blockLoc, turtle);
        turtlesById.put(id, turtle);
        saveAll();
        return turtle;
    }

    public synchronized Turtle restoreTurtle(String id, Location location, BlockFace facing, UUID owner) {
        Location blockLoc = normalizeLocation(location);
        Turtle existing = turtlesByLocation.get(blockLoc);
        if (existing != null) {
            if (owner != null && existing.getOwner() == null) {
                existing.setOwner(owner);
                saveAll();
            }
            return existing;
        }

        if (id == null || id.isBlank()) {
            id = String.format("T-%03d", idCounter.getAndIncrement());
        } else {
            try {
                if (id.startsWith("T-")) {
                    int num = Integer.parseInt(id.substring(2));
                    idCounter.updateAndGet(curr -> Math.max(curr, num + 1));
                }
            } catch (Exception ignored) {}
        }

        Turtle turtle = new Turtle(plugin, id, blockLoc, facing, owner);
        turtlesByLocation.put(blockLoc, turtle);
        turtlesById.put(id, turtle);
        saveAll();
        return turtle;
    }

    public synchronized void registerExistingTurtle(Turtle turtle) {
        Location blockLoc = normalizeLocation(turtle.getLocation());
        turtlesByLocation.put(blockLoc, turtle);
        turtlesById.put(turtle.getId(), turtle);
        saveAll();
    }

    public synchronized Turtle getTurtle(Location location) {
        if (location == null) return null;
        return turtlesByLocation.get(normalizeLocation(location));
    }

    public synchronized Turtle getTurtleById(String id) {
        if (id == null) return null;
        return turtlesById.get(id);
    }

    public synchronized Turtle removeTurtle(Location location) {
        if (location == null) return null;
        Location blockLoc = normalizeLocation(location);
        Turtle removed = turtlesByLocation.remove(blockLoc);
        if (removed != null) {
            removed.stopAnyWork();
            turtlesById.remove(removed.getId());
            saveAll();
        }
        return removed;
    }

    public synchronized void updateTurtleLocation(Turtle turtle, Location oldLoc, Location newLoc) {
        Location oldBlock = normalizeLocation(oldLoc);
        Location newBlock = normalizeLocation(newLoc);
        turtlesByLocation.remove(oldBlock);
        turtlesByLocation.put(newBlock, turtle);
        saveAll();
    }

    public boolean isTurtle(Location location) {
        if (location == null) return false;
        return turtlesByLocation.containsKey(normalizeLocation(location));
    }

    public Collection<Turtle> getAllTurtles() {
        return Collections.unmodifiableCollection(new ArrayList<>(turtlesById.values()));
    }

    public List<Turtle> getTurtlesByOwner(UUID owner) {
        if (owner == null) return Collections.emptyList();
        List<Turtle> list = new ArrayList<>();
        for (Turtle t : turtlesById.values()) {
            if (owner.equals(t.getOwner())) {
                list.add(t);
            }
        }
        return list;
    }

    public synchronized void cancelAll() {
        for (Turtle turtle : turtlesById.values()) {
            turtle.stopAnyWork();
        }
        saveAll();
        turtlesByLocation.clear();
        turtlesById.clear();
    }

    public synchronized void loadAll() {
        if (dataFile == null || !dataFile.exists()) {
            return;
        }
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            ConfigurationSection section = config.getConfigurationSection("turtles");
            if (section == null) {
                return;
            }
            for (String key : section.getKeys(false)) {
                ConfigurationSection tSec = section.getConfigurationSection(key);
                if (tSec == null) continue;
                String worldName = tSec.getString("world");
                if (worldName == null) continue;
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;

                int x = tSec.getInt("x");
                int y = tSec.getInt("y");
                int z = tSec.getInt("z");
                Location loc = new Location(world, x, y, z);
                String facingStr = tSec.getString("facing", "NORTH");
                BlockFace facing = BlockFace.NORTH;
                try {
                    facing = BlockFace.valueOf(facingStr);
                } catch (Exception ignored) {}

                String ownerStr = tSec.getString("owner");
                UUID ownerUuid = null;
                if (ownerStr != null && !ownerStr.isBlank()) {
                    try {
                        ownerUuid = UUID.fromString(ownerStr);
                    } catch (Exception ignored) {}
                }

                int fuel = tSec.getInt("fuel", 1000);
                Turtle turtle = new Turtle(plugin, key, loc, facing, ownerUuid);
                turtle.setFuel(fuel);
                turtlesByLocation.put(loc, turtle);
                turtlesById.put(key, turtle);

                try {
                    if (key.startsWith("T-")) {
                        int num = Integer.parseInt(key.substring(2));
                        idCounter.updateAndGet(curr -> Math.max(curr, num + 1));
                    }
                } catch (Exception ignored) {}
            }
        } catch (Throwable e) {
            plugin.getLogger().warning("Failed to load turtles.yml: " + e.getMessage());
        }
    }

    public synchronized void saveAll() {
        if (dataFile == null || plugin.getDataFolder() == null) {
            return;
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            YamlConfiguration config = new YamlConfiguration();
            ConfigurationSection root = config.createSection("turtles");
            for (Turtle turtle : turtlesById.values()) {
                Location loc = turtle.getLocation();
                if (loc == null || loc.getWorld() == null) continue;
                ConfigurationSection sec = root.createSection(turtle.getId());
                sec.set("world", loc.getWorld().getName());
                sec.set("x", loc.getBlockX());
                sec.set("y", loc.getBlockY());
                sec.set("z", loc.getBlockZ());
                sec.set("facing", turtle.getFacing().name());
                if (turtle.getOwner() != null) {
                    sec.set("owner", turtle.getOwner().toString());
                }
                sec.set("fuel", turtle.getFuel());
            }
            config.save(dataFile);
        } catch (Throwable e) {
            plugin.getLogger().warning("Failed to save turtles.yml: " + e.getMessage());
        }
    }

    private static Location normalizeLocation(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}

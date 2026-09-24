// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.turtle;

import com.multiverse.programming.MultiverseProgrammingPlugin;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

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
 * Manages all active Programmable Turtles in the server.
 */
public final class TurtleManager {

    private final MultiverseProgrammingPlugin plugin;
    private final Map<Location, Turtle> turtlesByLocation = new ConcurrentHashMap<>();
    private final Map<String, Turtle> turtlesById = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    public TurtleManager(MultiverseProgrammingPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
    }

    public synchronized Turtle createTurtle(Location location, BlockFace facing, UUID owner) {
        Location blockLoc = normalizeLocation(location);
        Turtle existing = turtlesByLocation.get(blockLoc);
        if (existing != null) {
            return existing;
        }

        String id = String.format("T-%03d", idCounter.getAndIncrement());
        Turtle turtle = new Turtle(plugin, id, blockLoc, facing, owner);
        turtlesByLocation.put(blockLoc, turtle);
        turtlesById.put(id, turtle);
        return turtle;
    }

    public synchronized void registerExistingTurtle(Turtle turtle) {
        Location blockLoc = normalizeLocation(turtle.getLocation());
        turtlesByLocation.put(blockLoc, turtle);
        turtlesById.put(turtle.getId(), turtle);
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
            removed.cancelBuild();
            turtlesById.remove(removed.getId());
        }
        return removed;
    }

    public synchronized void updateTurtleLocation(Turtle turtle, Location oldLoc, Location newLoc) {
        Location oldBlock = normalizeLocation(oldLoc);
        Location newBlock = normalizeLocation(newLoc);
        turtlesByLocation.remove(oldBlock);
        turtlesByLocation.put(newBlock, turtle);
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
            turtle.cancelBuild();
        }
        turtlesByLocation.clear();
        turtlesById.clear();
    }

    private static Location normalizeLocation(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}

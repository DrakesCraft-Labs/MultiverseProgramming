// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.turtle;

import com.multiverse.programming.blueprint.Blueprint;
import com.multiverse.programming.blueprint.Blueprint.PlacementBlock;
import com.multiverse.programming.peripheral.SyncDispatcher;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import com.multiverse.programming.MultiverseProgrammingPlugin;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Represents a programmable Turtle block in the world.
 * Can navigate the world, mine, place blocks, and execute blueprint builds.
 */
public final class Turtle {

    public enum Status {
        IDLE,
        MOVING,
        BUILDING,
        PAUSED,
        ERROR
    }

    private final String id;
    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private UUID owner;
    private Location location;
    private BlockFace facing;
    private int fuel;
    private int selectedSlot; // 0 to 15
    private final ItemStack[] inventory = new ItemStack[16];
    private ItemStack disk;
    private Status status = Status.IDLE;
    private String statusMessage = "Idle";

    // Active Build Task fields
    private String activeBlueprintId;
    private Location buildOrigin;
    private Blueprint activeBlueprint;
    private final AtomicInteger currentBlockIndex = new AtomicInteger(0);
    private int totalBlocks = 0;
    private BukkitTask buildTask;

    public Turtle(org.bukkit.plugin.java.JavaPlugin plugin, String id, Location location, BlockFace facing, UUID owner) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.location = Objects.requireNonNull(location, "location cannot be null");
        this.facing = (facing != null) ? sanitizeHorizontalFace(facing) : BlockFace.NORTH;
        this.owner = owner;
        this.fuel = 1000;
        this.selectedSlot = 0;
    }

    private static BlockFace sanitizeHorizontalFace(BlockFace face) {
        return switch (face) {
            case SOUTH -> BlockFace.SOUTH;
            case EAST -> BlockFace.EAST;
            case WEST -> BlockFace.WEST;
            default -> BlockFace.NORTH;
        };
    }

    public String getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public synchronized Location getLocation() {
        return location.clone();
    }

    public synchronized void setLocation(Location location) {
        this.location = location.clone();
    }

    public synchronized BlockFace getFacing() {
        return facing;
    }

    public synchronized int getFuel() {
        return fuel;
    }

    public synchronized void setFuel(int fuel) {
        this.fuel = Math.max(0, fuel);
    }

    public synchronized int getSelectedSlot() {
        return selectedSlot;
    }

    public synchronized void setSelectedSlot(int slot) {
        if (slot >= 0 && slot < 16) {
            this.selectedSlot = slot;
        }
    }

    public synchronized ItemStack getItem(int slot) {
        if (slot >= 0 && slot < 16) {
            ItemStack stack = inventory[slot];
            if (stack == null) return null;
            ItemStack cloned = null;
            try {
                cloned = stack.clone();
            } catch (Throwable ignored) {
            }
            return (cloned != null) ? cloned : stack;
        }
        return null;
    }

    public synchronized void setItem(int slot, ItemStack item) {
        if (slot >= 0 && slot < 16) {
            if (item == null || item.getType().isAir()) {
                inventory[slot] = null;
            } else {
                ItemStack cloned = null;
                try {
                    cloned = item.clone();
                } catch (Throwable ignored) {
                }
                inventory[slot] = (cloned != null) ? cloned : item;
            }
        }
    }

    public synchronized ItemStack[] getInventory() {
        ItemStack[] copy = new ItemStack[16];
        for (int i = 0; i < 16; i++) {
            copy[i] = inventory[i] != null ? inventory[i].clone() : null;
        }
        return copy;
    }

    public synchronized ItemStack getDisk() {
        return disk != null ? disk.clone() : null;
    }

    public synchronized void setDisk(ItemStack disk) {
        this.disk = (disk != null && !disk.getType().isAir()) ? disk.clone() : null;
    }

    public synchronized Status getStatus() {
        return status;
    }

    public synchronized String getStatusMessage() {
        return statusMessage;
    }

    public synchronized String getActiveBlueprintId() {
        return activeBlueprintId;
    }

    public int getCurrentBlockIndex() {
        return currentBlockIndex.get();
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public double getProgressPercentage() {
        if (totalBlocks <= 0) return 0.0;
        return Math.min(100.0, ((double) currentBlockIndex.get() / totalBlocks) * 100.0);
    }

    // =========================================================================
    // Movement
    // =========================================================================

    public boolean forward() {
        return moveInDirection(facing);
    }

    public boolean back() {
        return moveInDirection(facing.getOppositeFace());
    }

    public boolean up() {
        return moveInDirection(BlockFace.UP);
    }

    public boolean down() {
        return moveInDirection(BlockFace.DOWN);
    }

    public synchronized boolean turnLeft() {
        this.facing = switch (facing) {
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            default -> BlockFace.NORTH;
        };
        updateBlockFacing();
        return true;
    }

    public synchronized boolean turnRight() {
        this.facing = switch (facing) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> BlockFace.NORTH;
        };
        updateBlockFacing();
        return true;
    }

    private void updateBlockFacing() {
        if (location == null || location.getWorld() == null) {
            return;
        }
        SyncDispatcher.sync(plugin, () -> {
            try {
                Block block = location.getBlock();
                if (block != null && block.getBlockData() instanceof Directional dir) {
                    dir.setFacing(facing);
                    block.setBlockData(dir, false);
                }
            } catch (Throwable ignored) {
            }
            return null;
        });
    }

    private boolean moveInDirection(BlockFace dir) {
        return SyncDispatcher.sync(plugin, () -> {
            World world = location.getWorld();
            if (world == null) return false;

            Block currentBlock = location.getBlock();
            Block targetBlock = currentBlock.getRelative(dir);

            if (!targetBlock.isPassable() && !targetBlock.isEmpty()) {
                return false; // Obstacle
            }

            Material mat = currentBlock.getType();
            currentBlock.setType(Material.AIR, false);

            targetBlock.setType(mat, false);
            if (targetBlock.getBlockData() instanceof Directional d) {
                try {
                    d.setFacing(facing);
                    targetBlock.setBlockData(d, false);
                } catch (IllegalArgumentException ignored) {
                }
            }

            Location oldLoc = this.location.clone();
            this.location = targetBlock.getLocation();
            TurtleManager tm = getTurtleManager();
            if (tm != null) {
                tm.updateTurtleLocation(this, oldLoc, this.location);
            }
            world.playSound(this.location, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 0.4f, 1.8f);
            return true;
        });
    }

    // =========================================================================
    // Digging and Placing
    // =========================================================================

    public boolean dig() {
        return digFace(facing);
    }

    public boolean digUp() {
        return digFace(BlockFace.UP);
    }

    public boolean digDown() {
        return digFace(BlockFace.DOWN);
    }

    private boolean digFace(BlockFace face) {
        return SyncDispatcher.sync(plugin, () -> {
            Block target = location.getBlock().getRelative(face);
            if (target.isEmpty() || target.getType() == Material.BEDROCK || target.getType() == Material.BARRIER) {
                return false;
            }

            Collection<ItemStack> drops = target.getDrops();
            target.setType(Material.AIR, true);

            for (ItemStack drop : drops) {
                if (drop == null || drop.getType().isAir()) continue;
                ItemStack remaining = addToInventory(drop);
                if (remaining != null && remaining.getAmount() > 0) {
                    target.getWorld().dropItemNaturally(target.getLocation(), remaining);
                }
            }
            return true;
        });
    }

    public boolean place() {
        return placeFace(facing);
    }

    public boolean placeUp() {
        return placeFace(BlockFace.UP);
    }

    public boolean placeDown() {
        return placeFace(BlockFace.DOWN);
    }

    private boolean placeFace(BlockFace face) {
        return SyncDispatcher.sync(plugin, () -> {
            Block target = location.getBlock().getRelative(face);
            if (!target.isEmpty() && !target.isPassable()) {
                return false;
            }

            ItemStack stack;
            synchronized (this) {
                stack = inventory[selectedSlot];
            }
            if (stack == null || stack.getAmount() <= 0 || !stack.getType().isBlock()) {
                return false;
            }

            target.setType(stack.getType(), true);
            synchronized (this) {
                stack.setAmount(stack.getAmount() - 1);
                if (stack.getAmount() <= 0) {
                    inventory[selectedSlot] = null;
                }
            }
            return true;
        });
    }

    public synchronized ItemStack addToInventory(ItemStack item) {
        if (item == null || item.getAmount() <= 0) return null;
        ItemStack toAdd = item.clone();

        // 1. Try stacking into existing non-full slots
        for (int i = 0; i < 16; i++) {
            ItemStack existing = inventory[i];
            if (existing != null && existing.isSimilar(toAdd)) {
                int space = existing.getMaxStackSize() - existing.getAmount();
                if (space > 0) {
                    int add = Math.min(space, toAdd.getAmount());
                    existing.setAmount(existing.getAmount() + add);
                    toAdd.setAmount(toAdd.getAmount() - add);
                    if (toAdd.getAmount() <= 0) return null;
                }
            }
        }

        // 2. Try empty slots
        for (int i = 0; i < 16; i++) {
            if (inventory[i] == null || inventory[i].getType().isAir()) {
                inventory[i] = toAdd.clone();
                return null;
            }
        }
        return toAdd;
    }

    // =========================================================================
    // Refueling
    // =========================================================================

    public synchronized boolean refuel(int count) {
        ItemStack stack = inventory[selectedSlot];
        if (stack == null || stack.getAmount() <= 0) return false;

        int fuelPerItem = switch (stack.getType()) {
            case COAL, CHARCOAL -> 80;
            case BLAZE_ROD -> 120;
            case LAVA_BUCKET -> 1000;
            case COAL_BLOCK -> 800;
            default -> 0;
        };

        if (fuelPerItem <= 0) return false;

        int consume = (count <= 0) ? stack.getAmount() : Math.min(count, stack.getAmount());
        this.fuel += consume * fuelPerItem;

        if (stack.getType() == Material.LAVA_BUCKET) {
            inventory[selectedSlot] = new ItemStack(Material.BUCKET, consume);
        } else {
            stack.setAmount(stack.getAmount() - consume);
            if (stack.getAmount() <= 0) {
                inventory[selectedSlot] = null;
            }
        }
        return true;
    }

    // =========================================================================
    // Blueprint Construction
    // =========================================================================

    public synchronized boolean startBuild(
            Blueprint blueprint,
            Location origin,
            int delayTicks,
            boolean requireMaterials,
            boolean clearBlocks,
            Runnable onDone,
            Consumer<String> onError
    ) {
        cancelBuild();

        if (blueprint == null) {
            failBuild("Blueprint cannot be null", onError);
            return false;
        }
        if (origin == null || origin.getWorld() == null) {
            failBuild("Target coordinates (origin) are mandatory. The turtle will not build without explicit coordinates.", onError);
            return false;
        }

        // Validate area obstruction and clear if permitted
        if (!checkAndPrepareArea(blueprint, origin, clearBlocks, onError)) {
            return false;
        }

        this.activeBlueprint = blueprint;
        this.activeBlueprintId = blueprint.id();
        this.buildOrigin = origin.clone();
        this.totalBlocks = blueprint.totalBlocks();
        this.currentBlockIndex.set(0);
        this.status = Status.BUILDING;
        this.statusMessage = "Building " + blueprint.name() + " (0%)";

        int safeDelay = Math.max(1, delayTicks);

        if (Bukkit.getScheduler() != null && plugin != null) {
            this.buildTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (status == Status.PAUSED) {
                return;
            }

            int index = currentBlockIndex.get();
            if (index >= activeBlueprint.blocks().size()) {
                finishBuild(onDone);
                return;
            }

            PlacementBlock pb = activeBlueprint.blocks().get(index);
            Material blockMat = parseMaterialFromBlockState(pb.material());

            if (blockMat == null || blockMat.isAir()) {
                currentBlockIndex.incrementAndGet();
                return;
            }

            World world = buildOrigin.getWorld();
            if (world == null) {
                failBuild("Build origin world is unloaded.", onError);
                return;
            }

            // Material Check if required
            if (requireMaterials) {
                if (!consumeMaterial(blockMat)) {
                    this.status = Status.PAUSED;
                    this.statusMessage = "Paused: Missing material " + blockMat.name();
                    if (onError != null) {
                        onError.accept("Turtle is missing required material: " + blockMat.name());
                    }
                    return;
                }
            }

            int targetX = buildOrigin.getBlockX() + pb.x();
            int targetY = buildOrigin.getBlockY() + pb.y();
            int targetZ = buildOrigin.getBlockZ() + pb.z();

            if (targetY >= world.getMinHeight() && targetY < world.getMaxHeight()) {
                // Physically relocate the turtle to an adjacent free spot facing the block
                moveTurtleAdjacentTo(world, targetX, targetY, targetZ);

                Block targetBlock = world.getBlockAt(targetX, targetY, targetZ);
                if (targetBlock.getType() != blockMat) {
                    targetBlock.setType(blockMat, false);
                    try {
                        world.spawnParticle(Particle.HAPPY_VILLAGER, targetX + 0.5, targetY + 0.5, targetZ + 0.5, 2, 0.1, 0.1, 0.1, 0.02);
                        world.playSound(targetBlock.getLocation(), Sound.BLOCK_STONE_PLACE, 0.5f, 1.0f);
                    } catch (Throwable ignored) {}
                }
            }

            int next = currentBlockIndex.incrementAndGet();
            this.statusMessage = String.format(Locale.ROOT, "Building %s (%d/%d - %.1f%%)",
                    activeBlueprint.name(), next, totalBlocks, getProgressPercentage());

            if (next >= totalBlocks) {
                finishBuild(onDone);
            }
        }, 1L, safeDelay);
        }

        return true;
    }

    public synchronized boolean startBuild(
            Blueprint blueprint,
            Location origin,
            int delayTicks,
            boolean requireMaterials,
            Runnable onDone,
            Consumer<String> onError
    ) {
        return startBuild(blueprint, origin, delayTicks, requireMaterials, false, onDone, onError);
    }

    private boolean checkAndPrepareArea(Blueprint bp, Location origin, boolean clearBlocks, Consumer<String> onError) {
        World world = origin.getWorld();
        if (world == null) {
            failBuild("World is not loaded.", onError);
            return false;
        }

        List<Block> obstructed = new ArrayList<>();
        for (PlacementBlock pb : bp.blocks()) {
            Material targetMat = parseMaterialFromBlockState(pb.material());
            if (targetMat == null || targetMat.isAir()) continue;

            int bx = origin.getBlockX() + pb.x();
            int by = origin.getBlockY() + pb.y();
            int bz = origin.getBlockZ() + pb.z();

            if (by < world.getMinHeight() || by >= world.getMaxHeight()) continue;

            if (this.location != null
                    && this.location.getBlockX() == bx
                    && this.location.getBlockY() == by
                    && this.location.getBlockZ() == bz) {
                continue;
            }

            Block existing = world.getBlockAt(bx, by, bz);
            if (existing != null && !existing.isEmpty() && existing.getType() != targetMat) {
                obstructed.add(existing);
            }
        }

        if (!obstructed.isEmpty()) {
            if (!clearBlocks) {
                failBuild("Target area is obstructed by " + obstructed.size() + " existing block(s). Clear the area first or specify 'clear' to destroy them without drops.", onError);
                return false;
            }
            // Clear obstructed blocks without drops
            for (Block b : obstructed) {
                if (b.getType() != Material.BEDROCK && b.getType() != Material.BARRIER) {
                    b.setType(Material.AIR, false);
                }
            }
        }

        return true;
    }

    private void moveTurtleAdjacentTo(World world, int targetX, int targetY, int targetZ) {
        if (world == null || this.location == null) return;

        int curX = this.location.getBlockX();
        int curY = this.location.getBlockY();
        int curZ = this.location.getBlockZ();

        int dx = curX - targetX;
        int dy = curY - targetY;
        int dz = curZ - targetZ;

        // If turtle is already adjacent (Manhattan distance == 1)
        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) == 1) {
            BlockFace faceTowards = determineFacing(dx, dy, dz);
            if (faceTowards != null) {
                setTurtleFacing(faceTowards);
            }
            try {
                world.playSound(this.location, Sound.BLOCK_DISPENSER_DISPENSE, 0.3f, 1.8f);
            } catch (Throwable ignored) {}
            return;
        }

        // Potential adjacent offsets and the facing needed to look at the target block
        int[][] candidateOffsets = new int[][] {
                {0, 0, -1},  // NORTH of target -> turtle faces SOUTH
                {1, 0, 0},   // EAST of target -> turtle faces WEST
                {0, 0, 1},   // SOUTH of target -> turtle faces NORTH
                {-1, 0, 0},  // WEST of target -> turtle faces EAST
                {0, 1, 0},   // UP of target -> turtle faces DOWN
                {0, -1, 0}   // DOWN of target -> turtle faces UP
        };

        BlockFace[] candidateFacings = new BlockFace[] {
                BlockFace.SOUTH,
                BlockFace.WEST,
                BlockFace.NORTH,
                BlockFace.EAST,
                BlockFace.DOWN,
                BlockFace.UP
        };

        int bestIndex = -1;
        double minDistanceSq = Double.MAX_VALUE;

        for (int i = 0; i < candidateOffsets.length; i++) {
            int cx = targetX + candidateOffsets[i][0];
            int cy = targetY + candidateOffsets[i][1];
            int cz = targetZ + candidateOffsets[i][2];

            if (cy < world.getMinHeight() || cy >= world.getMaxHeight()) {
                continue;
            }

            Block candidateBlock = world.getBlockAt(cx, cy, cz);
            boolean isSelf = (cx == curX && cy == curY && cz == curZ);
            if (isSelf || (candidateBlock != null && (candidateBlock.isEmpty() || candidateBlock.isPassable()))) {
                double distSq = (cx - curX) * (cx - curX) + (cy - curY) * (cy - curY) + (cz - curZ) * (cz - curZ);
                if (i >= 4) {
                    distSq += 0.5; // slight preference for ground/horizontal
                }
                if (distSq < minDistanceSq) {
                    minDistanceSq = distSq;
                    bestIndex = i;
                }
            }
        }

        if (bestIndex != -1) {
            int bx = targetX + candidateOffsets[bestIndex][0];
            int by = targetY + candidateOffsets[bestIndex][1];
            int bz = targetZ + candidateOffsets[bestIndex][2];
            BlockFace bestFacing = candidateFacings[bestIndex];

            Location newLoc = new Location(world, bx, by, bz);
            if (!newLoc.equals(this.location)) {
                relocateTurtleTo(world, newLoc, bestFacing);
            } else {
                setTurtleFacing(bestFacing);
            }
        }
    }

    private BlockFace determineFacing(int dx, int dy, int dz) {
        if (dx == 1 && dy == 0 && dz == 0) return BlockFace.WEST;
        if (dx == -1 && dy == 0 && dz == 0) return BlockFace.EAST;
        if (dz == 1 && dx == 0 && dy == 0) return BlockFace.NORTH;
        if (dz == -1 && dx == 0 && dy == 0) return BlockFace.SOUTH;
        if (dy == 1 && dx == 0 && dz == 0) return BlockFace.DOWN;
        if (dy == -1 && dx == 0 && dz == 0) return BlockFace.UP;
        return BlockFace.NORTH;
    }

    private void setTurtleFacing(BlockFace newFacing) {
        this.facing = newFacing;
        if (location == null || location.getWorld() == null) return;
        try {
            Block block = location.getBlock();
            if (block != null && block.getBlockData() instanceof Directional dir) {
                dir.setFacing(newFacing);
                block.setBlockData(dir, false);
            }
        } catch (Throwable ignored) {}
    }

    private void relocateTurtleTo(World world, Location newLoc, BlockFace newFacing) {
        Block oldBlock = this.location.getBlock();
        Material turtleMat = (oldBlock != null) ? oldBlock.getType() : Material.DISPENSER;
        if (!turtleMat.isBlock() || turtleMat.isAir()) {
            turtleMat = Material.DISPENSER;
        }

        if (oldBlock != null) {
            oldBlock.setType(Material.AIR, false);
        }

        Block newBlock = newLoc.getBlock();
        if (newBlock != null) {
            newBlock.setType(turtleMat, false);
            if (newBlock.getBlockData() instanceof Directional dir) {
                try {
                    dir.setFacing(newFacing);
                    newBlock.setBlockData(dir, false);
                } catch (Throwable ignored) {}
            }
        }

        Location oldLoc = this.location.clone();
        this.location = newLoc.clone();
        this.facing = newFacing;

        TurtleManager tm = getTurtleManager();
        if (tm != null) {
            tm.updateTurtleLocation(this, oldLoc, this.location);
        }

        try {
            world.playSound(newLoc, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 0.45f, 1.6f);
            world.spawnParticle(Particle.SMOKE, oldLoc.getX() + 0.5, oldLoc.getY() + 0.5, oldLoc.getZ() + 0.5, 3, 0.08, 0.08, 0.08, 0.01);
        } catch (Throwable ignored) {}
    }

    private TurtleManager getTurtleManager() {
        if (plugin instanceof MultiverseProgrammingPlugin mvp) {
            return mvp.getTurtleManager();
        }
        return null;
    }

    private synchronized boolean consumeMaterial(Material mat) {
        for (int i = 0; i < 16; i++) {
            ItemStack stack = inventory[i];
            if (stack != null && stack.getType() == mat && stack.getAmount() > 0) {
                stack.setAmount(stack.getAmount() - 1);
                if (stack.getAmount() <= 0) {
                    inventory[i] = null;
                }
                return true;
            }
        }
        return false;
    }

    private void finishBuild(Runnable onDone) {
        cancelTask();
        this.status = Status.IDLE;
        this.statusMessage = "Build completed (" + totalBlocks + " blocks)";
        if (buildOrigin != null && buildOrigin.getWorld() != null) {
            buildOrigin.getWorld().playSound(buildOrigin, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        }
        if (onDone != null) {
            onDone.run();
        }
    }

    private void failBuild(String message, Consumer<String> onError) {
        cancelTask();
        this.status = Status.ERROR;
        this.statusMessage = "Error: " + message;
        if (onError != null) {
            onError.accept(message);
        }
    }

    public synchronized void pauseBuild() {
        if (status == Status.BUILDING) {
            this.status = Status.PAUSED;
            this.statusMessage = "Build paused at " + currentBlockIndex.get() + "/" + totalBlocks;
        }
    }

    public synchronized void resumeBuild() {
        if (status == Status.PAUSED) {
            this.status = Status.BUILDING;
            this.statusMessage = "Resuming build...";
        }
    }

    public synchronized void cancelBuild() {
        cancelTask();
        this.status = Status.IDLE;
        this.statusMessage = "Idle";
        this.activeBlueprint = null;
        this.activeBlueprintId = null;
        this.currentBlockIndex.set(0);
        this.totalBlocks = 0;
    }

    private void cancelTask() {
        if (buildTask != null) {
            buildTask.cancel();
            buildTask = null;
        }
    }

    public static Material parseMaterialFromBlockState(String blockState) {
        if (blockState == null || blockState.isBlank()) {
            return Material.AIR;
        }
        String clean = blockState;
        if (clean.contains("[")) {
            clean = clean.substring(0, clean.indexOf('['));
        }
        if (clean.startsWith("minecraft:")) {
            clean = clean.substring("minecraft:".length());
        }
        clean = clean.trim().toUpperCase(Locale.ROOT);
        Material mat = Material.matchMaterial(clean);
        return mat != null ? mat : Material.AIR;
    }
}

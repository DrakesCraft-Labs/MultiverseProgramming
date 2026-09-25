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
import com.multiverse.programming.protection.CoreProtectBridge;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
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
        MINING,
        PAUSED,
        ERROR
    }

    public enum LateralSide {
        LEFT,
        RIGHT
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

    private Location constructionChestLoc;
    private org.bukkit.entity.Entity constructionHologram;
    private Location fuelChestLoc;
    private org.bukkit.entity.Entity fuelHologram;

    // Active Quarry Engine Upgrade fields
    private BukkitTask quarryTask;
    private LateralSide quarryLateralSide;
    private Location quarryStartLocation;
    private Location quarryStorageChestLoc;
    private org.bukkit.entity.Entity quarryStorageHologram;
    private Location quarryFuelChestLoc;
    private org.bukkit.entity.Entity quarryFuelHologram;
    private int quarryBlocksMined = 0;
    private int quarryTotalBlocks = 0;
    private int quarryWidth = 0;
    private int quarryLength = 0;
    private int quarryTargetY = 0;
    private int quarryCurY = 0;
    private int quarryStepX = 0;
    private int quarryStepZ = 0;
    private boolean quarryHandleLiquids = true;
    private int quarryFuelPoints = 0;

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

    public synchronized Blueprint getActiveBlueprint() {
        return activeBlueprint;
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
        BlockFace oldFacing = this.facing;
        this.facing = switch (facing) {
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            default -> BlockFace.NORTH;
        };

        if (quarryLateralSide != null && location != null && location.getWorld() != null) {
            World world = location.getWorld();
            BlockFace lateralFace = getLateralFace(this.facing, quarryLateralSide);
            Block newEngineBlock = location.getBlock().getRelative(lateralFace);
            BlockFace oldLateralFace = getLateralFace(oldFacing, quarryLateralSide);
            Block oldEngineBlock = location.getBlock().getRelative(oldLateralFace);
            if (newEngineBlock != null && !newEngineBlock.isPassable() && !newEngineBlock.isEmpty()
                    && oldEngineBlock != null && !newEngineBlock.getLocation().equals(oldEngineBlock.getLocation())) {
                this.facing = oldFacing;
                return false;
            }
            SyncDispatcher.sync(plugin, () -> {
                relocateTurtleWithEngine(world, this.location, this.facing);
                return null;
            });
            return true;
        }

        updateBlockFacing();
        return true;
    }

    public synchronized boolean turnRight() {
        BlockFace oldFacing = this.facing;
        this.facing = switch (facing) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> BlockFace.NORTH;
        };

        if (quarryLateralSide != null && location != null && location.getWorld() != null) {
            World world = location.getWorld();
            BlockFace lateralFace = getLateralFace(this.facing, quarryLateralSide);
            Block newEngineBlock = location.getBlock().getRelative(lateralFace);
            BlockFace oldLateralFace = getLateralFace(oldFacing, quarryLateralSide);
            Block oldEngineBlock = location.getBlock().getRelative(oldLateralFace);
            if (newEngineBlock != null && !newEngineBlock.isPassable() && !newEngineBlock.isEmpty()
                    && oldEngineBlock != null && !newEngineBlock.getLocation().equals(oldEngineBlock.getLocation())) {
                this.facing = oldFacing;
                return false;
            }
            SyncDispatcher.sync(plugin, () -> {
                relocateTurtleWithEngine(world, this.location, this.facing);
                return null;
            });
            return true;
        }

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

            if (quarryLateralSide != null) {
                BlockFace lateralFace = getLateralFace(facing, quarryLateralSide);
                Block targetEngineBlock = targetBlock.getRelative(lateralFace);
                Location currEngineLoc = currentBlock.getRelative(lateralFace).getLocation();
                if (targetEngineBlock != null && !targetEngineBlock.isPassable() && !targetEngineBlock.isEmpty()
                        && !targetEngineBlock.getLocation().equals(location)
                        && !targetEngineBlock.getLocation().equals(currEngineLoc)) {
                    return false; // Engine obstructed
                }

                if (!consumeQuarryFuel()) {
                    return false;
                }

                relocateTurtleWithEngine(world, targetBlock.getLocation(), facing);
                return true;
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
            if (target.isEmpty() || isIllegalBlock(target.getType())) {
                return false;
            }

            Collection<ItemStack> drops = target.getDrops();
            Material oldMat = target.getType();
            BlockData oldData = target.getBlockData();
            Location targetLoc = target.getLocation();

            target.setType(Material.AIR, true);
            CoreProtectBridge.logRemoval(owner, id, targetLoc, oldMat, oldData);

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
            if (stack == null || stack.getAmount() <= 0 || !stack.getType().isBlock() || isIllegalBlock(stack.getType())) {
                return false;
            }

            target.setType(stack.getType(), true);
            CoreProtectBridge.logPlacement(owner, id, target.getLocation(), stack.getType(), target.getBlockData());

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
        int fuelPerItem = getFuelValue(stack != null ? stack.getType() : null);

        if (fuelPerItem > 0 && stack != null && stack.getAmount() > 0) {
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

        // Fallback: check adjacent fuel chest
        if (fuelChestLoc != null && fuelChestLoc.getWorld() != null) {
            Block block = fuelChestLoc.getBlock();
            if (block.getState() instanceof org.bukkit.block.Container container) {
                Inventory chestInv = container.getInventory();
                for (int slot = 0; slot < chestInv.getSize(); slot++) {
                    ItemStack cItem = chestInv.getItem(slot);
                    int fVal = getFuelValue(cItem != null ? cItem.getType() : null);
                    if (fVal > 0 && cItem != null && cItem.getAmount() > 0) {
                        int consume = (count <= 0) ? cItem.getAmount() : Math.min(count, cItem.getAmount());
                        this.fuel += consume * fVal;
                        if (cItem.getType() == Material.LAVA_BUCKET) {
                            chestInv.setItem(slot, new ItemStack(Material.BUCKET, consume));
                        } else {
                            cItem.setAmount(cItem.getAmount() - consume);
                            if (cItem.getAmount() <= 0) {
                                chestInv.setItem(slot, null);
                            }
                        }
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static int getFuelValue(Material mat) {
        if (mat == null) return 0;
        return switch (mat) {
            case COAL, CHARCOAL -> 80;
            case BLAZE_ROD -> 120;
            case LAVA_BUCKET -> 1000;
            case COAL_BLOCK -> 800;
            default -> 0;
        };
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
            int rotationDegrees,
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

        if (rotationDegrees != 0) {
            blueprint = blueprint.rotate(rotationDegrees);
        }

        // Validate area obstruction, region protection claims, and clear if permitted
        if (!checkAndPrepareArea(blueprint, origin, clearBlocks, onError)) {
            return false;
        }

        if (requireMaterials) {
            setupConstructionChests(origin);
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
            BlockData blockData = parseBlockData(pb.material());
            Material blockMat = (blockData != null) ? blockData.getMaterial() : parseMaterialFromBlockState(pb.material());

            if (blockMat == null || blockMat.isAir() || isIllegalBlock(blockMat)) {
                currentBlockIndex.incrementAndGet();
                return;
            }

            World world = buildOrigin.getWorld();
            if (world == null) {
                failBuild("Build origin world is unloaded.", onError);
                return;
            }

            // Material Check if required
            // Upper halves of doors/tall plants or head parts of beds are formed with the lower/foot part,
            // or should only consume 1 item for the pair.
            if (requireMaterials) {
                Material itemMat = getItemMaterialForBlock(blockMat);
                boolean isUpper = pb.material().contains("half=upper") || pb.material().contains("part=head");
                if (!isUpper && itemMat.isItem()) {
                    if (!consumeMaterial(itemMat)) {
                        this.status = Status.PAUSED;
                        this.statusMessage = "Paused: Missing material " + itemMat.name();
                        if (onError != null) {
                            onError.accept("Turtle is missing required material: " + itemMat.name());
                        }
                        return;
                    }
                }
            }

            int targetX = buildOrigin.getBlockX() + pb.x();
            int targetY = buildOrigin.getBlockY() + pb.y();
            int targetZ = buildOrigin.getBlockZ() + pb.z();

            if (targetY >= world.getMinHeight() && targetY < world.getMaxHeight()) {
                // Physically relocate the turtle to an adjacent free spot facing the block
                moveTurtleAdjacentTo(world, targetX, targetY, targetZ);

                Block targetBlock = world.getBlockAt(targetX, targetY, targetZ);
                boolean updated = false;
                if (blockData != null) {
                    if (targetBlock.getBlockData() == null || !targetBlock.getBlockData().matches(blockData)) {
                        targetBlock.setBlockData(blockData, false);
                        updated = true;
                        CoreProtectBridge.logPlacement(owner, id, targetBlock.getLocation(), blockData.getMaterial(), blockData);
                    }
                } else if (targetBlock.getType() != blockMat) {
                    targetBlock.setType(blockMat, false);
                    updated = true;
                    CoreProtectBridge.logPlacement(owner, id, targetBlock.getLocation(), blockMat, targetBlock.getBlockData());
                }

                if (updated) {
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
            boolean clearBlocks,
            Runnable onDone,
            Consumer<String> onError
    ) {
        return startBuild(blueprint, origin, delayTicks, requireMaterials, clearBlocks, 0, onDone, onError);
    }

    public synchronized boolean startBuild(
            Blueprint blueprint,
            Location origin,
            int delayTicks,
            boolean requireMaterials,
            Runnable onDone,
            Consumer<String> onError
    ) {
        return startBuild(blueprint, origin, delayTicks, requireMaterials, false, 0, onDone, onError);
    }

    private boolean checkAndPrepareArea(Blueprint bp, Location origin, boolean clearBlocks, Consumer<String> onError) {
        World world = origin.getWorld();
        if (world == null) {
            failBuild("World is not loaded.", onError);
            return false;
        }

        // Region claims protection check (WorldGuard / ProtectionStones)
        if (plugin instanceof MultiverseProgrammingPlugin mvp) {
            String protectionError = mvp.getProtectionManager().checkBuildArea(this.owner, origin, bp);
            if (protectionError != null) {
                failBuild(protectionError, onError);
                return false;
            }
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
                if (!isIllegalBlock(b.getType())) {
                    Material oldMat = b.getType();
                    BlockData oldData = b.getBlockData();
                    Location bLoc = b.getLocation();
                    b.setType(Material.AIR, false);
                    CoreProtectBridge.logRemoval(owner, id, bLoc, oldMat, oldData);
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
        // 1. Check internal turtle inventory
        for (int i = 0; i < 16; i++) {
            ItemStack stack = inventory[i];
            if (stack != null && stack.getAmount() > 0) {
                if (stack.getType() == mat ||
                        (mat == Material.WATER && stack.getType() == Material.WATER_BUCKET) ||
                        (mat == Material.LAVA && stack.getType() == Material.LAVA_BUCKET)) {
                    stack.setAmount(stack.getAmount() - 1);
                    if (stack.getAmount() <= 0) {
                        inventory[i] = null;
                    }
                    return true;
                }
            }
        }

        // 2. Check construction supply chest
        if (constructionChestLoc != null && constructionChestLoc.getWorld() != null) {
            Block chestBlock = constructionChestLoc.getBlock();
            if (chestBlock.getState() instanceof org.bukkit.block.Container container) {
                Inventory chestInv = container.getInventory();
                for (int slot = 0; slot < chestInv.getSize(); slot++) {
                    ItemStack item = chestInv.getItem(slot);
                    if (item != null && item.getAmount() > 0) {
                        if (item.getType() == mat ||
                                (mat == Material.WATER && item.getType() == Material.WATER_BUCKET) ||
                                (mat == Material.LAVA && item.getType() == Material.LAVA_BUCKET)) {
                            item.setAmount(item.getAmount() - 1);
                            if (item.getAmount() <= 0) {
                                chestInv.setItem(slot, null);
                            }
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private void setupConstructionChests(Location origin) {
        World world = (this.location != null && this.location.getWorld() != null) ? this.location.getWorld() : origin.getWorld();
        if (world == null) return;

        Location startLoc = (this.location != null) ? this.location : origin;
        BlockFace[] sides = {BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH};
        Location chosenChest = null;
        Location chosenFuel = null;

        for (BlockFace face : sides) {
            Block adj = startLoc.getBlock().getRelative(face);
            if (adj.getType() == Material.CHEST || adj.getType() == Material.TRAPPED_CHEST || adj.getType() == Material.BARREL) {
                if (chosenChest == null) {
                    chosenChest = adj.getLocation();
                    continue;
                } else if (chosenFuel == null) {
                    chosenFuel = adj.getLocation();
                    break;
                }
            }
            if (adj.isEmpty() || adj.isPassable()) {
                if (chosenChest == null) {
                    chosenChest = adj.getLocation();
                } else if (chosenFuel == null) {
                    chosenFuel = adj.getLocation();
                }
            }
        }

        if (chosenChest == null) {
            chosenChest = startLoc.clone().add(1, 0, 0);
        }

        Block chestBlock = chosenChest.getBlock();
        if (chestBlock.getType() != Material.CHEST && chestBlock.getType() != Material.BARREL) {
            chestBlock.setType(Material.CHEST, false);
        }
        this.constructionChestLoc = chosenChest;

        Location holoLoc = chosenChest.clone().add(0.5, 1.25, 0.5);
        try {
            cleanupHolograms();
            try {
                org.bukkit.entity.TextDisplay td = world.spawn(holoLoc, org.bukkit.entity.TextDisplay.class, display -> {
                    display.setText("§e📦 Place construction blocks here");
                    display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                    display.setDefaultBackground(false);
                    display.setSeeThrough(true);
                });
                this.constructionHologram = td;
            } catch (Throwable fallback) {
                org.bukkit.entity.ArmorStand stand = world.spawn(holoLoc.clone().subtract(0, 1.0, 0), org.bukkit.entity.ArmorStand.class, as -> {
                    as.setCustomName("§e📦 Place construction blocks here");
                    as.setCustomNameVisible(true);
                    as.setVisible(false);
                    as.setGravity(false);
                    as.setMarker(true);
                });
                this.constructionHologram = stand;
            }
        } catch (Throwable ignored) {}

        boolean fuelRequired = false;
        if (plugin instanceof MultiverseProgrammingPlugin mvp) {
            fuelRequired = mvp.getConfigManager().isTurtleFuelRequired();
        }
        if (fuelRequired && chosenFuel != null) {
            Block fuelBlock = chosenFuel.getBlock();
            if (fuelBlock.getType() != Material.CHEST && fuelBlock.getType() != Material.BARREL) {
                fuelBlock.setType(Material.BARREL, false);
            }
            this.fuelChestLoc = chosenFuel;
            Location fuelHoloLoc = chosenFuel.clone().add(0.5, 1.25, 0.5);
            try {
                try {
                    org.bukkit.entity.TextDisplay td = world.spawn(fuelHoloLoc, org.bukkit.entity.TextDisplay.class, display -> {
                        display.setText("§6⚡ Place fuel here");
                        display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                        display.setDefaultBackground(false);
                        display.setSeeThrough(true);
                    });
                    this.fuelHologram = td;
                } catch (Throwable fallback) {
                    org.bukkit.entity.ArmorStand stand = world.spawn(fuelHoloLoc.clone().subtract(0, 1.0, 0), org.bukkit.entity.ArmorStand.class, as -> {
                        as.setCustomName("§6⚡ Place fuel here");
                        as.setCustomNameVisible(true);
                        as.setVisible(false);
                        as.setGravity(false);
                        as.setMarker(true);
                    });
                    this.fuelHologram = stand;
                }
            } catch (Throwable ignored) {}
        }
    }

    private void cleanupHolograms() {
        if (constructionHologram != null && constructionHologram.isValid()) {
            try {
                constructionHologram.remove();
            } catch (Throwable ignored) {}
            constructionHologram = null;
        }
        if (fuelHologram != null && fuelHologram.isValid()) {
            try {
                fuelHologram.remove();
            } catch (Throwable ignored) {}
            fuelHologram = null;
        }
    }

    private void finishBuild(Runnable onDone) {
        cleanupHolograms();
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
        cleanupHolograms();
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
        cleanupHolograms();
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

    // =========================================================================
    // Quarry Engine Attachment & Autonomous Excavation
    // =========================================================================

    public static BlockFace getLeftFace(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            default -> BlockFace.WEST;
        };
    }

    public static BlockFace getRightFace(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> BlockFace.EAST;
        };
    }

    public static BlockFace getLateralFace(BlockFace currentFacing, LateralSide side) {
        return (side == LateralSide.LEFT) ? getLeftFace(currentFacing) : getRightFace(currentFacing);
    }

    public Material getQuarryBlockMaterial() {
        if (plugin instanceof MultiverseProgrammingPlugin mvp) {
            if (mvp.getConfigManager() != null) {
                return mvp.getConfigManager().getQuarryBlock();
            }
        }
        return Material.BLAST_FURNACE;
    }

    public LateralSide findLateralQuarrySide() {
        if (location == null || location.getWorld() == null) return null;
        Material qMat = getQuarryBlockMaterial();
        Block b = location.getBlock();
        if (b == null) return null;
        BlockFace left = getLeftFace(this.facing);
        Block leftBlock = b.getRelative(left);
        if (leftBlock != null && leftBlock.getType() == qMat) {
            return LateralSide.LEFT;
        }
        BlockFace right = getRightFace(this.facing);
        Block rightBlock = b.getRelative(right);
        if (rightBlock != null && rightBlock.getType() == qMat) {
            return LateralSide.RIGHT;
        }
        return null;
    }

    public boolean hasQuarryEngineAttached() {
        return findLateralQuarrySide() != null || quarryLateralSide != null;
    }

    public LateralSide getQuarryLateralSide() {
        return quarryLateralSide;
    }

    public void setQuarryLateralSide(LateralSide side) {
        this.quarryLateralSide = side;
    }

    public Location getQuarryStorageChestLoc() {
        return quarryStorageChestLoc;
    }

    public Location getQuarryFuelChestLoc() {
        return quarryFuelChestLoc;
    }

    private void setupQuarryChests(Location startLoc) {
        World world = startLoc.getWorld();
        if (world == null) return;

        BlockFace back = this.facing.getOppositeFace();
        BlockFace left = getLeftFace(this.facing);
        BlockFace right = getRightFace(this.facing);

        // Position 1 (Output / Mined Blocks): directly behind turtle
        Block chest1 = startLoc.getBlock().getRelative(back);
        // Position 2 (Fuel Chest): adjacent to chest1
        Block chest2 = chest1.getRelative(left);
        if (!chest2.isEmpty() && !chest2.isPassable() && chest2.getType() != Material.CHEST && chest2.getType() != Material.BARREL) {
            chest2 = chest1.getRelative(right);
        }
        if (!chest2.isEmpty() && !chest2.isPassable() && chest2.getType() != Material.CHEST && chest2.getType() != Material.BARREL) {
            chest2 = startLoc.getBlock().getRelative(back, 2);
        }

        if (chest1.getType() != Material.CHEST && chest1.getType() != Material.BARREL) {
            chest1.setType(Material.CHEST, false);
        }
        if (chest2.getType() != Material.CHEST && chest2.getType() != Material.BARREL) {
            chest2.setType(Material.CHEST, false);
        }

        this.quarryStorageChestLoc = chest1.getLocation();
        this.quarryFuelChestLoc = chest2.getLocation();
        this.fuelChestLoc = chest2.getLocation();

        cleanupQuarryHolograms();
        spawnQuarryHologram(quarryStorageChestLoc, "§e📦 Mined Blocks Storage", true);
        spawnQuarryHologram(quarryFuelChestLoc, "§6⚡ Place fuel here", false);
    }

    private void spawnQuarryHologram(Location chestLoc, String text, boolean isStorage) {
        World world = chestLoc.getWorld();
        if (world == null) return;
        Location holoLoc = chestLoc.clone().add(0.5, 1.25, 0.5);
        try {
            org.bukkit.entity.TextDisplay td = world.spawn(holoLoc, org.bukkit.entity.TextDisplay.class, display -> {
                display.setText(text);
                display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                display.setDefaultBackground(false);
                display.setSeeThrough(true);
            });
            if (isStorage) {
                this.quarryStorageHologram = td;
            } else {
                this.quarryFuelHologram = td;
            }
        } catch (Throwable fallback) {
            try {
                org.bukkit.entity.ArmorStand stand = world.spawn(holoLoc.clone().subtract(0, 1.0, 0), org.bukkit.entity.ArmorStand.class, as -> {
                    as.setCustomName(text);
                    as.setCustomNameVisible(true);
                    as.setVisible(false);
                    as.setGravity(false);
                    as.setMarker(true);
                });
                if (isStorage) {
                    this.quarryStorageHologram = stand;
                } else {
                    this.quarryFuelHologram = stand;
                }
            } catch (Throwable ignored) {}
        }
    }

    private void cleanupQuarryHolograms() {
        if (quarryStorageHologram != null && quarryStorageHologram.isValid()) {
            try { quarryStorageHologram.remove(); } catch (Throwable ignored) {}
            quarryStorageHologram = null;
        }
        if (quarryFuelHologram != null && quarryFuelHologram.isValid()) {
            try { quarryFuelHologram.remove(); } catch (Throwable ignored) {}
            quarryFuelHologram = null;
        }
    }

    public synchronized boolean isQuarryStorageFull() {
        if (quarryStorageChestLoc == null || quarryStorageChestLoc.getWorld() == null) {
            return false;
        }
        Block block = quarryStorageChestLoc.getBlock();
        if (block != null && block.getState() instanceof org.bukkit.block.Container container) {
            Inventory inv = container.getInventory();
            if (inv.firstEmpty() != -1) {
                return false;
            }
            for (ItemStack item : inv.getContents()) {
                if (item != null && item.getAmount() < item.getMaxStackSize()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public synchronized boolean depositToQuarryStorage(ItemStack drop) {
        if (drop == null || drop.getType().isAir()) return true;
        if (quarryStorageChestLoc == null || quarryStorageChestLoc.getWorld() == null) {
            return false;
        }

        Block block = quarryStorageChestLoc.getBlock();
        if (block != null && block.getState() instanceof org.bukkit.block.Container container) {
            Inventory inv = container.getInventory();
            java.util.HashMap<Integer, ItemStack> remaining = inv.addItem(drop);
            if (!remaining.isEmpty()) {
                for (ItemStack rem : remaining.values()) {
                    addToInventory(rem);
                }
                return false;
            }
            return true;
        }
        return false;
    }

    public synchronized boolean consumeQuarryFuel() {
        quarryFuelPoints += 120;
        int toDeduct = quarryFuelPoints / 100;
        if (toDeduct <= 0) {
            return true;
        }
        quarryFuelPoints %= 100;

        boolean required = true;
        if (plugin instanceof MultiverseProgrammingPlugin mvp) {
            if (mvp.getConfigManager() != null) {
                required = mvp.getConfigManager().isTurtleFuelRequired();
            }
        }

        if (this.fuel < toDeduct) {
            refuel(0);
        }

        if (this.fuel >= toDeduct) {
            this.fuel -= toDeduct;
            return true;
        }

        return !required;
    }

    public synchronized void relocateTurtleWithEngine(World world, Location newLoc, BlockFace newFacing) {
        if (world == null || this.location == null) return;

        Material turtleMat = (plugin instanceof MultiverseProgrammingPlugin mvp && mvp.getConfigManager() != null)
                ? mvp.getConfigManager().getTurtleBlock()
                : Material.DISPENSER;
        Material quarryMat = getQuarryBlockMaterial();

        Location oldLoc = this.location.clone();
        BlockFace oldFacing = this.facing;

        Location oldEngineLoc = null;
        if (quarryLateralSide != null && oldLoc != null) {
            Block b = oldLoc.getBlock();
            if (b != null) {
                Block rel = b.getRelative(getLateralFace(oldFacing, quarryLateralSide));
                if (rel != null) oldEngineLoc = rel.getLocation();
            }
        }

        Location newEngineLoc = null;
        if (quarryLateralSide != null && newLoc != null) {
            Block b = newLoc.getBlock();
            if (b != null) {
                Block rel = b.getRelative(getLateralFace(newFacing, quarryLateralSide));
                if (rel != null) newEngineLoc = rel.getLocation();
            }
        }

        if (oldEngineLoc != null && !oldEngineLoc.equals(newLoc) && !oldEngineLoc.equals(newEngineLoc)) {
            Block b = oldEngineLoc.getBlock();
            if (b != null) b.setType(Material.AIR, false);
        }
        if (!oldLoc.equals(newLoc) && !oldLoc.equals(newEngineLoc)) {
            Block b = oldLoc.getBlock();
            if (b != null) b.setType(Material.AIR, false);
        }

        Block newTurtleBlock = newLoc.getBlock();
        if (newTurtleBlock != null) {
            newTurtleBlock.setType(turtleMat, false);
            if (newTurtleBlock.getBlockData() instanceof Directional dir) {
                try {
                    dir.setFacing(newFacing);
                    newTurtleBlock.setBlockData(dir, false);
                } catch (Throwable ignored) {}
            }
        }

        if (newEngineLoc != null) {
            Block newEngineBlock = newEngineLoc.getBlock();
            if (newEngineBlock != null) {
                newEngineBlock.setType(quarryMat, false);
            }
            try {
                world.spawnParticle(Particle.FLAME, newEngineLoc.clone().add(0.5, 0.5, 0.5), 3, 0.1, 0.1, 0.1, 0.01);
            } catch (Throwable ignored) {}
        }

        this.location = newLoc.clone();
        this.facing = newFacing;

        TurtleManager tm = getTurtleManager();
        if (tm != null) {
            tm.updateTurtleLocation(this, oldLoc, this.location);
        }

        try {
            world.playSound(newLoc, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 0.4f, 1.6f);
            world.spawnParticle(Particle.SMOKE, oldLoc.getX() + 0.5, oldLoc.getY() + 0.5, oldLoc.getZ() + 0.5, 2, 0.05, 0.05, 0.05, 0.01);
        } catch (Throwable ignored) {}
    }

    public synchronized boolean startQuarry(
            int width,
            int length,
            int targetYLevel,
            boolean handleLiquids,
            Runnable onDone,
            Consumer<String> onError
    ) {
        cancelQuarry();
        cancelBuild();

        if (location == null || location.getWorld() == null) {
            failQuarry("Turtle world is unloaded", onError);
            return false;
        }

        World world = location.getWorld();

        LateralSide side = findLateralQuarrySide();
        if (side == null) {
            failQuarry("No Quarry Engine attached to the lateral side of the turtle", onError);
            return false;
        }
        this.quarryLateralSide = side;

        width = Math.max(1, Math.min(64, width));
        length = Math.max(1, Math.min(64, length));
        int minY = Math.max(world.getMinHeight(), targetYLevel);

        this.quarryWidth = width;
        this.quarryLength = length;
        this.quarryTargetY = minY;
        this.quarryHandleLiquids = handleLiquids;
        this.quarryStartLocation = location.clone();

        int startY = location.getBlockY() - 1;
        if (startY < minY) {
            failQuarry("Target Y level (" + minY + ") is above excavation start level (" + startY + ")", onError);
            return false;
        }

        if (plugin instanceof MultiverseProgrammingPlugin mvp) {
            com.multiverse.programming.protection.ProtectionManager pm = mvp.getProtectionManager();
            if (pm != null) {
                BlockFace forward = this.facing;
                BlockFace lateral = getRightFace(this.facing);
                int minX = location.getBlockX() + Math.min(forward.getModX() * length, lateral.getModX() * width);
                int maxX = location.getBlockX() + Math.max(forward.getModX() * length, lateral.getModX() * width);
                int minZ = location.getBlockZ() + Math.min(forward.getModZ() * length, lateral.getModZ() * width);
                int maxZ = location.getBlockZ() + Math.max(forward.getModZ() * length, lateral.getModZ() * width);
                Location c1 = new Location(world, minX, minY, minZ);
                Location c2 = new Location(world, maxX, startY, maxZ);
                if (pm.checkBuildArea(this.owner, c1, null) != null || pm.checkBuildArea(this.owner, c2, null) != null) {
                    failQuarry("Protected region claim prevents quarry excavation", onError);
                    return false;
                }
            }
        }

        setupQuarryChests(this.location);

        this.quarryBlocksMined = 0;
        this.quarryTotalBlocks = width * length * (startY - minY + 1);
        this.quarryCurY = startY;
        this.quarryStepX = 0;
        this.quarryStepZ = 0;
        this.quarryFuelPoints = 0;
        this.status = Status.MINING;
        this.statusMessage = "Quarry excavation starting at Y=" + quarryCurY;

        if (Bukkit.getScheduler() != null && plugin != null) {
            this.quarryTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (status == Status.PAUSED) {
                    return;
                }

                if (quarryCurY < quarryTargetY) {
                    finishQuarry(onDone);
                    return;
                }

                BlockFace f = this.facing;
                BlockFace lat = getRightFace(this.facing);

                int d = quarryStepZ + 1;
                int w = quarryStepX;

                int bx = quarryStartLocation.getBlockX() + (f.getModX() * d) + (lat.getModX() * w);
                int bz = quarryStartLocation.getBlockZ() + (f.getModZ() * d) + (lat.getModZ() * w);
                int by = quarryCurY;

                if (!consumeQuarryFuel()) {
                    this.status = Status.PAUSED;
                    this.statusMessage = "Paused: Out of fuel (place fuel in Fuel Chest)";
                    if (onError != null) onError.accept("Turtle is out of fuel for quarry operation");
                    return;
                }

                Location adjLoc = new Location(world, bx, Math.min(world.getMaxHeight() - 1, by + 1), bz);
                if (!adjLoc.equals(this.location)) {
                    relocateTurtleWithEngine(world, adjLoc, this.facing);
                }

                Block blockToMine = world.getBlockAt(bx, by, bz);
                Material mat = blockToMine.getType();

                if (isIllegalBlock(mat) || mat == Material.BEDROCK) {
                    // Skip unmineable blocks
                } else if (mat == Material.WATER || mat == Material.LAVA) {
                    if (quarryHandleLiquids) {
                        blockToMine.setType(Material.AIR, false);
                    }
                } else if (!mat.isAir()) {
                    if (isQuarryStorageFull()) {
                        this.status = Status.PAUSED;
                        this.statusMessage = "Paused: Mined blocks storage chest is full";
                        if (onError != null) onError.accept("Quarry storage chest is full");
                        return;
                    }

                    Collection<ItemStack> drops = blockToMine.getDrops();
                    Location bLoc = blockToMine.getLocation();
                    BlockData oldData = blockToMine.getBlockData();

                    blockToMine.setType(Material.AIR, false);
                    CoreProtectBridge.logRemoval(owner, id, bLoc, mat, oldData);

                    for (ItemStack drop : drops) {
                        if (drop == null || drop.getType().isAir()) continue;
                        boolean stored = depositToQuarryStorage(drop);
                        if (!stored) {
                            this.status = Status.PAUSED;
                            this.statusMessage = "Paused: Mined blocks storage chest is full";
                            if (onError != null) onError.accept("Quarry storage chest is full");
                            return;
                        }
                    }
                    quarryBlocksMined++;
                }

                quarryStepX++;
                if (quarryStepX >= quarryWidth) {
                    quarryStepX = 0;
                    quarryStepZ++;
                    if (quarryStepZ >= quarryLength) {
                        quarryStepZ = 0;
                        quarryCurY--;
                        int pct = (int) (((double) quarryBlocksMined / Math.max(1, quarryTotalBlocks)) * 100);
                        this.statusMessage = String.format(Locale.ROOT, "Quarry digging layer Y=%d (%d%% - %d blocks)",
                                quarryCurY, pct, quarryBlocksMined);
                    }
                }
            }, 1L, 1L);
        }

        return true;
    }

    private void finishQuarry(Runnable onDone) {
        cancelQuarryTask();
        this.status = Status.IDLE;
        this.statusMessage = "Quarry completed (" + quarryBlocksMined + " blocks mined)";
        if (quarryStartLocation != null && quarryStartLocation.getWorld() != null) {
            relocateTurtleWithEngine(quarryStartLocation.getWorld(), quarryStartLocation, this.facing);
            quarryStartLocation.getWorld().playSound(quarryStartLocation, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        }
        if (onDone != null) {
            onDone.run();
        }
    }

    private void failQuarry(String error, Consumer<String> onError) {
        cleanupQuarryHolograms();
        cancelQuarryTask();
        this.status = Status.ERROR;
        this.statusMessage = "Error: " + error;
        if (onError != null) {
            onError.accept(error);
        }
    }

    public synchronized void pauseQuarry() {
        if (status == Status.MINING) {
            this.status = Status.PAUSED;
            this.statusMessage = "Quarry paused at Y=" + quarryCurY;
        }
    }

    public synchronized void resumeQuarry() {
        if (status == Status.PAUSED && quarryTask != null) {
            if (quarryStorageChestLoc != null) {
                for (int i = 0; i < 16; i++) {
                    if (inventory[i] != null && !inventory[i].getType().isAir()) {
                        depositToQuarryStorage(inventory[i]);
                    }
                }
            }
            this.status = Status.MINING;
            this.statusMessage = "Resuming quarry at Y=" + quarryCurY;
        }
    }

    public synchronized void cancelQuarry() {
        cleanupQuarryHolograms();
        cancelQuarryTask();
        if (status == Status.MINING || status == Status.PAUSED) {
            this.status = Status.IDLE;
            this.statusMessage = "Idle";
        }
    }

    private void cancelQuarryTask() {
        if (quarryTask != null) {
            quarryTask.cancel();
            quarryTask = null;
        }
    }

    public synchronized boolean isQuarryActive() {
        return (status == Status.MINING || (status == Status.PAUSED && quarryTask != null));
    }

    public synchronized boolean isQuarryPaused() {
        return status == Status.PAUSED && quarryTask != null;
    }

    public synchronized int getQuarryBlocksMined() {
        return quarryBlocksMined;
    }

    public synchronized int getQuarryTotalBlocks() {
        return quarryTotalBlocks;
    }

    public synchronized int getQuarryCurrentY() {
        return quarryCurY;
    }

    public synchronized int getQuarryTargetY() {
        return quarryTargetY;
    }

    public synchronized double getQuarryProgressPercentage() {
        if (quarryTotalBlocks <= 0) return 0.0;
        return Math.min(100.0, Math.round(((double) quarryBlocksMined / quarryTotalBlocks) * 1000.0) / 10.0);
    }

    public static BlockData parseBlockData(String blockState) {
        if (blockState == null || blockState.isBlank()) {
            return null;
        }
        try {
            BlockData data = Bukkit.createBlockData(blockState);
            if (data instanceof Waterlogged wl) {
                if (!blockState.contains("waterlogged=true")) {
                    wl.setWaterlogged(false);
                }
            }
            return data;
        } catch (Throwable t) {
            try {
                Material mat = parseMaterialFromBlockState(blockState);
                if (mat != null && !mat.isAir()) {
                    BlockData data = Bukkit.createBlockData(mat);
                    if (data instanceof Waterlogged wl) {
                        if (!blockState.contains("waterlogged=true")) {
                            wl.setWaterlogged(false);
                        }
                    }
                    return data;
                }
            } catch (Throwable ignored) {}
            return null;
        }
    }

    public static Material getItemMaterialForBlock(Material blockMat) {
        if (blockMat == null) return Material.AIR;

        String name = blockMat.name();
        if (name.equals("WALL_TORCH")) return Material.TORCH;
        if (name.equals("SOUL_WALL_TORCH")) return Material.SOUL_TORCH;
        if (name.equals("REDSTONE_WALL_TORCH")) return Material.REDSTONE_TORCH;
        if (name.endsWith("_WALL_FAN")) {
            Material match = Material.matchMaterial(name.replace("_WALL_FAN", "_FAN"));
            if (match != null && match.isItem()) return match;
        }
        if (name.endsWith("_WALL_SIGN")) {
            Material match = Material.matchMaterial(name.replace("_WALL_SIGN", "_SIGN"));
            if (match != null && match.isItem()) return match;
        }
        if (name.endsWith("_WALL_HANGING_SIGN")) {
            Material match = Material.matchMaterial(name.replace("_WALL_HANGING_SIGN", "_HANGING_SIGN"));
            if (match != null && match.isItem()) return match;
        }
        if (name.startsWith("POTTED_")) {
            return Material.FLOWER_POT;
        }
        if (name.endsWith("_WALL_HEAD") || name.endsWith("_WALL_SKULL")) {
            String headName = name.replace("_WALL_HEAD", "_HEAD").replace("_WALL_SKULL", "_SKULL");
            Material match = Material.matchMaterial(headName);
            if (match != null && match.isItem()) return match;
        }
        if (name.equals("WATER_CAULDRON") || name.equals("LAVA_CAULDRON") || name.equals("POWDER_SNOW_CAULDRON")) {
            return Material.CAULDRON;
        }
        if (name.equals("REDSTONE_WIRE")) return Material.REDSTONE;
        if (name.equals("TRIPWIRE")) return Material.STRING;
        if (name.equals("SWEET_BERRY_BUSH")) return Material.SWEET_BERRIES;
        if (name.equals("CARROTS")) return Material.CARROT;
        if (name.equals("POTATOES")) return Material.POTATO;
        if (name.equals("BEETROOTS")) return Material.BEETROOT_SEEDS;
        if (name.equals("WHEAT")) return Material.WHEAT_SEEDS;
        if (name.equals("BAMBOO_SAPLING")) return Material.BAMBOO;
        if (name.equals("MELON_STEM") || name.equals("ATTACHED_MELON_STEM")) return Material.MELON_SEEDS;
        if (name.equals("PUMPKIN_STEM") || name.equals("ATTACHED_PUMPKIN_STEM")) return Material.PUMPKIN_SEEDS;
        if (name.equals("COCOA")) return Material.COCOA_BEANS;

        return blockMat;
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

    public static boolean isIllegalBlock(Material mat) {
        if (mat == null) return false;
        return switch (mat) {
            case BEDROCK, BARRIER, STRUCTURE_BLOCK, STRUCTURE_VOID, JIGSAW,
                 COMMAND_BLOCK, CHAIN_COMMAND_BLOCK, REPEATING_COMMAND_BLOCK,
                 LIGHT, END_PORTAL, END_PORTAL_FRAME, END_GATEWAY,
                 REINFORCED_DEEPSLATE -> true;
            default -> false;
        };
    }
}

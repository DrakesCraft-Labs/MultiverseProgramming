// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.peripheral;

import com.multiverse.programming.MultiverseProgrammingPlugin;
import com.multiverse.programming.protection.ProtectionManager;
import com.multiverse.programming.turtle.Turtle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaDouble;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Autonomous Excavator and Quarry peripheral.
 * Mines a volumetric column (width X * length Z down to target Y) layer-by-layer,
 * safely handling liquids, region claims protection, and container depositing.
 */
public final class QuarryPeripheral implements Peripheral {

    private final JavaPlugin plugin;
    private final Location location;

    private BukkitTask task;
    private boolean active = false;
    private boolean paused = false;
    private String statusMessage = "Idle";

    private int startX, startY, startZ;
    private int widthX, lengthZ, targetY;
    private int curX, curY, curZ;
    private boolean handleLiquids = true;

    private int blocksMined = 0;
    private int totalBlocks = 0;

    public QuarryPeripheral(JavaPlugin plugin, Location location) {
        this.plugin = plugin;
        this.location = location;
    }

    @Override
    public String getType() {
        return "quarry";
    }

    @Override
    public Location getLocation() {
        return location;
    }

    private void depositDrop(ItemStack drop) {
        if (drop == null || drop.getType().isAir()) return;
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN}) {
            Block rel = location.getBlock().getRelative(face);
            if (rel != null && rel.getState() instanceof InventoryHolder holder) {
                Inventory inv = holder.getInventory();
                if (inv.firstEmpty() != -1) {
                    inv.addItem(drop);
                    return;
                }
            }
        }
        World world = location.getWorld();
        if (world != null) {
            world.dropItemNaturally(location.clone().add(0.5, 1.0, 0.5), drop);
        }
    }

    public synchronized void stopQuarry() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        active = false;
        paused = false;
        statusMessage = "Stopped";
    }

    public synchronized boolean startQuarry(int width, int length, int targetYLevel, boolean liquids) {
        stopQuarry();

        World world = location.getWorld();
        if (world == null) {
            statusMessage = "World unloaded";
            return false;
        }

        width = Math.max(1, Math.min(64, width));
        length = Math.max(1, Math.min(64, length));
        int minY = Math.max(world.getMinHeight(), targetYLevel);

        this.widthX = width;
        this.lengthZ = length;
        this.targetY = minY;
        this.handleLiquids = liquids;

        this.startX = location.getBlockX() + 1;
        this.startY = location.getBlockY() - 1;
        this.startZ = location.getBlockZ() + 1;

        if (this.startY < this.targetY) {
            statusMessage = "Target Y is above quarry block";
            return false;
        }

        // Region protection claim check
        if (plugin instanceof MultiverseProgrammingPlugin mvp) {
            ProtectionManager pm = mvp.getProtectionManager();
            if (pm != null) {
                Location minCorner = new Location(world, startX, targetY, startZ);
                Location maxCorner = new Location(world, startX + widthX - 1, startY, startZ + lengthZ - 1);
                // Check corners
                if (pm.checkBuildArea(null, minCorner, null) != null || pm.checkBuildArea(null, maxCorner, null) != null) {
                    statusMessage = "Protected region claim prevents quarry excavation";
                    return false;
                }
            }
        }

        this.curX = startX;
        this.curY = startY;
        this.curZ = startZ;
        this.blocksMined = 0;
        this.totalBlocks = width * length * (startY - targetY + 1);
        this.active = true;
        this.paused = false;
        this.statusMessage = "Digging layer Y=" + curY;

        this.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!active || paused) return;

            // Mine 2 blocks per tick for fast progress without TPS lag
            for (int step = 0; step < 2; step++) {
                if (curY < targetY) {
                    stopQuarry();
                    statusMessage = "Completed excavation of " + blocksMined + " blocks";
                    return;
                }

                Block b = world.getBlockAt(curX, curY, curZ);
                Material mat = b.getType();

                if (Turtle.isIllegalBlock(mat) || mat == Material.BEDROCK) {
                    // Skip unmineable blocks
                } else if (mat == Material.WATER || mat == Material.LAVA) {
                    if (handleLiquids) {
                        b.setType(Material.AIR, false);
                    }
                } else if (!mat.isAir()) {
                    Collection<ItemStack> drops = b.getDrops();
                    Location bLoc = b.getLocation();
                    org.bukkit.block.data.BlockData oldData = b.getBlockData();
                    b.setType(Material.AIR, false);
                    com.multiverse.programming.protection.CoreProtectBridge.logRemoval(null, "Quarry", bLoc, mat, oldData);
                    for (ItemStack d : drops) {
                        depositDrop(d);
                    }
                    blocksMined++;
                }

                // Advance coordinates: X -> Z -> Y
                curX++;
                if (curX >= startX + widthX) {
                    curX = startX;
                    curZ++;
                    if (curZ >= startZ + lengthZ) {
                        curZ = startZ;
                        curY--;
                        statusMessage = "Digging layer Y=" + curY + " (" + (int) (((double) blocksMined / Math.max(1, totalBlocks)) * 100) + "%)";
                    }
                }
            }
        }, 1L, 1L);

        return true;
    }

    @Override
    public LuaValue toLuaTable() {
        LuaTable table = new LuaTable();

        // quarry.start(width, length, targetY, [handleLiquids])
        table.set("start", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int width = args.checkint(1);
                int length = args.checkint(2);
                int tY = args.checkint(3);
                boolean liquids = args.narg() < 4 || args.checkboolean(4);

                return SyncDispatcher.sync(plugin, () -> {
                    boolean ok = startQuarry(width, length, tY, liquids);
                    return varargsOf(LuaBoolean.valueOf(ok), LuaString.valueOf(statusMessage));
                });
            }
        });

        // quarry.stop()
        table.set("stop", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                SyncDispatcher.sync(plugin, () -> {
                    stopQuarry();
                    return null;
                });
                return LuaBoolean.TRUE;
            }
        });

        // quarry.pause()
        table.set("pause", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                paused = true;
                statusMessage = "Paused";
                return LuaBoolean.TRUE;
            }
        });

        // quarry.resume()
        table.set("resume", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                paused = false;
                statusMessage = "Resumed digging layer Y=" + curY;
                return LuaBoolean.TRUE;
            }
        });

        // quarry.getStatus() -> table
        table.set("getStatus", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                LuaTable s = new LuaTable();
                s.set("active", LuaBoolean.valueOf(active));
                s.set("paused", LuaBoolean.valueOf(paused));
                s.set("currentY", LuaInteger.valueOf(curY));
                s.set("targetY", LuaInteger.valueOf(targetY));
                s.set("blocksMined", LuaInteger.valueOf(blocksMined));
                s.set("totalBlocks", LuaInteger.valueOf(totalBlocks));
                double pct = totalBlocks > 0 ? (double) blocksMined / totalBlocks * 100.0 : 0.0;
                s.set("percentage", LuaDouble.valueOf(pct));
                s.set("message", LuaString.valueOf(statusMessage));
                return s;
            }
        });

        return table;
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.peripheral;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

import java.util.Collection;
import java.util.Locale;

/**
 * Peripheral that automates crop inspection, harvesting, automatic replanting,
 * and fertilization.
 */
public final class FarmerPeripheral implements Peripheral {

    private final JavaPlugin plugin;
    private final Location location;

    public FarmerPeripheral(JavaPlugin plugin, Location location) {
        this.plugin = plugin;
        this.location = location;
    }

    @Override
    public String getType() {
        return "farmer";
    }

    @Override
    public Location getLocation() {
        return location;
    }

    private void depositDrops(World world, Collection<ItemStack> drops, Location loc) {
        for (ItemStack drop : drops) {
            boolean stored = false;
            // Search adjacent containers around the farmer block
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN}) {
                Block rel = location.getBlock().getRelative(face);
                if (rel != null && rel.getState() instanceof InventoryHolder holder) {
                    Inventory inv = holder.getInventory();
                    if (inv.firstEmpty() != -1) {
                        inv.addItem(drop);
                        stored = true;
                        break;
                    }
                }
            }
            if (!stored) {
                world.dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), drop);
            }
        }
    }

    private boolean isCropMature(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        Material mat = block.getType();
        return mat == Material.PUMPKIN || mat == Material.MELON || mat == Material.SUGAR_CANE || mat == Material.CACTUS;
    }

    private boolean harvestCropBlock(Block block, boolean replant) {
        World world = block.getWorld();
        BlockData data = block.getBlockData();
        Collection<ItemStack> drops = block.getDrops();

        if (data instanceof Ageable ageable) {
            if (ageable.getAge() < ageable.getMaximumAge()) {
                return false;
            }
            depositDrops(world, drops, block.getLocation());
            if (replant) {
                ageable.setAge(0);
                block.setBlockData(ageable);
            } else {
                block.setType(Material.AIR);
            }
            return true;
        }

        Material mat = block.getType();
        if (mat == Material.PUMPKIN || mat == Material.MELON) {
            depositDrops(world, drops, block.getLocation());
            block.setType(Material.AIR);
            return true;
        }

        if (mat == Material.SUGAR_CANE || mat == Material.CACTUS) {
            // Check if block below is same type (only break upper parts)
            Block below = block.getRelative(BlockFace.DOWN);
            if (below.getType() == mat) {
                depositDrops(world, drops, block.getLocation());
                block.setType(Material.AIR);
                return true;
            }
        }

        return false;
    }

    @Override
    public LuaValue toLuaTable() {
        LuaTable table = new LuaTable();

        // farmer.inspectCrop(x, y, z) or farmer.inspectCrop(side)
        table.set("inspectCrop", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return SyncDispatcher.sync(plugin, () -> {
                    World world = location.getWorld();
                    if (world == null) return LuaValue.NIL;

                    Block target;
                    if (args.narg() >= 3) {
                        int x = args.checkint(1);
                        int y = args.checkint(2);
                        int z = args.checkint(3);
                        target = world.getBlockAt(x, y, z);
                    } else if (args.narg() >= 1) {
                        BlockFace face = TransposerPeripheral.parseFace(args.checkjstring(1));
                        target = location.getBlock().getRelative(face);
                    } else {
                        target = location.getBlock().getRelative(BlockFace.DOWN);
                    }

                    LuaTable info = new LuaTable();
                    info.set("material", LuaString.valueOf(target.getType().name()));

                    BlockData data = target.getBlockData();
                    if (data instanceof Ageable ageable) {
                        info.set("isCrop", LuaBoolean.TRUE);
                        info.set("age", LuaInteger.valueOf(ageable.getAge()));
                        info.set("maxAge", LuaInteger.valueOf(ageable.getMaximumAge()));
                        info.set("mature", LuaBoolean.valueOf(ageable.getAge() >= ageable.getMaximumAge()));
                    } else {
                        info.set("isCrop", LuaBoolean.valueOf(target.getType() == Material.PUMPKIN || target.getType() == Material.MELON));
                        info.set("mature", LuaBoolean.valueOf(isCropMature(target)));
                    }
                    return info;
                });
            }
        });

        // farmer.harvest(x, y, z, [replant]) or farmer.harvest(side, [replant])
        table.set("harvest", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return SyncDispatcher.sync(plugin, () -> {
                    World world = location.getWorld();
                    if (world == null) return LuaBoolean.FALSE;

                    Block target;
                    boolean replant;

                    if (args.narg() >= 3 && args.arg(2).isnumber()) {
                        int x = args.checkint(1);
                        int y = args.checkint(2);
                        int z = args.checkint(3);
                        replant = args.narg() >= 4 && args.checkboolean(4);
                        target = world.getBlockAt(x, y, z);
                    } else {
                        String side = args.narg() >= 1 ? args.checkjstring(1) : "down";
                        replant = args.narg() >= 2 && args.checkboolean(2);
                        BlockFace face = TransposerPeripheral.parseFace(side);
                        target = location.getBlock().getRelative(face);
                    }

                    boolean harvested = harvestCropBlock(target, replant);
                    return LuaBoolean.valueOf(harvested);
                });
            }
        });

        // farmer.harvestArea([radius], [replant]) -> harvested count
        table.set("harvestArea", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int radius = args.narg() >= 1 ? Math.min(12, Math.max(1, args.checkint(1))) : 4;
                boolean replant = args.narg() < 2 || args.checkboolean(2);

                return SyncDispatcher.sync(plugin, () -> {
                    World world = location.getWorld();
                    if (world == null) return LuaInteger.valueOf(0);

                    int count = 0;
                    int ox = location.getBlockX();
                    int oy = location.getBlockY();
                    int oz = location.getBlockZ();

                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            // Check at Y, Y-1, Y+1 for crops
                            for (int dy = -2; dy <= 2; dy++) {
                                Block b = world.getBlockAt(ox + dx, oy + dy, oz + dz);
                                if (isCropMature(b)) {
                                    if (harvestCropBlock(b, replant)) {
                                        count++;
                                    }
                                }
                            }
                        }
                    }
                    return LuaInteger.valueOf(count);
                });
            }
        });

        // farmer.fertilize(x, y, z) or farmer.fertilize(side)
        table.set("fertilize", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return SyncDispatcher.sync(plugin, () -> {
                    World world = location.getWorld();
                    if (world == null) return LuaBoolean.FALSE;

                    Block target;
                    if (args.narg() >= 3 && args.arg(2).isnumber()) {
                        int x = args.checkint(1);
                        int y = args.checkint(2);
                        int z = args.checkint(3);
                        target = world.getBlockAt(x, y, z);
                    } else {
                        String side = args.narg() >= 1 ? args.checkjstring(1) : "down";
                        BlockFace face = TransposerPeripheral.parseFace(side);
                        target = location.getBlock().getRelative(face);
                    }

                    // Check if adjacent containers have bone meal
                    boolean consumed = false;
                    for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN}) {
                        Block rel = location.getBlock().getRelative(face);
                        if (rel != null && rel.getState() instanceof InventoryHolder holder) {
                            Inventory inv = holder.getInventory();
                            for (ItemStack it : inv.getContents()) {
                                if (it != null && it.getType() == Material.BONE_MEAL && it.getAmount() > 0) {
                                    it.setAmount(it.getAmount() - 1);
                                    consumed = true;
                                    break;
                                }
                            }
                            if (consumed) break;
                        }
                    }

                    if (!consumed) {
                        return LuaBoolean.FALSE;
                    }

                    BlockData data = target.getBlockData();
                    if (data instanceof Ageable ageable) {
                        int nextAge = Math.min(ageable.getMaximumAge(), ageable.getAge() + 2);
                        ageable.setAge(nextAge);
                        target.setBlockData(ageable);
                        world.playEffect(target.getLocation(), org.bukkit.Effect.BONE_MEAL_USE, 0);
                        return LuaBoolean.TRUE;
                    }

                    return LuaBoolean.FALSE;
                });
            }
        });

        return table;
    }
}

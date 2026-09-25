// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.peripheral;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.Locale;

/**
 * Peripheral that handles geographical scanning, biome inspection,
 * custom Map item generation, and topography projection onto adjacent monitors.
 */
public final class CartographerPeripheral implements Peripheral {

    private final JavaPlugin plugin;
    private final Location location;

    public CartographerPeripheral(JavaPlugin plugin, Location location) {
        this.plugin = plugin;
        this.location = location;
    }

    @Override
    public String getType() {
        return "cartographer";
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public LuaValue toLuaTable() {
        LuaTable table = new LuaTable();

        // cartographer.getBiome() -> biome name
        table.set("getBiome", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                World world = location.getWorld();
                if (world == null) return LuaValue.NIL;
                return LuaString.valueOf(world.getBiome(location).getKey().getKey().toUpperCase(Locale.ROOT));
            }
        });

        // cartographer.scanTopography([radius]) -> table of height points
        table.set("scanTopography", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int radius = args.narg() >= 1 ? Math.min(32, Math.max(1, args.checkint(1))) : 8;

                return SyncDispatcher.sync(plugin, () -> {
                    LuaTable result = new LuaTable();
                    World world = location.getWorld();
                    if (world == null) return result;

                    int originX = location.getBlockX();
                    int originZ = location.getBlockZ();

                    LuaTable grid = new LuaTable();
                    int idx = 1;
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            int x = originX + dx;
                            int z = originZ + dz;
                            int highestY = world.getHighestBlockYAt(x, z);
                            Block topBlock = world.getBlockAt(x, highestY, z);

                            LuaTable pt = new LuaTable();
                            pt.set("x", LuaInteger.valueOf(x));
                            pt.set("y", LuaInteger.valueOf(highestY));
                            pt.set("z", LuaInteger.valueOf(z));
                            pt.set("material", LuaString.valueOf(topBlock.getType().name()));
                            grid.set(idx++, pt);
                        }
                    }
                    result.set("radius", LuaInteger.valueOf(radius));
                    result.set("points", grid);
                    return result;
                });
            }
        });

        // cartographer.createMap([scale]) -> boolean, mapId
        table.set("createMap", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int scaleLevel = args.narg() >= 1 ? Math.max(0, Math.min(4, args.checkint(1))) : 1;

                return SyncDispatcher.sync(plugin, () -> {
                    World world = location.getWorld();
                    if (world == null) {
                        return varargsOf(LuaBoolean.FALSE, LuaString.valueOf("World not available"));
                    }

                    MapView view = Bukkit.createMap(world);
                    view.setCenterX(location.getBlockX());
                    view.setCenterZ(location.getBlockZ());
                    view.setScale(MapView.Scale.valueOf("CLOSEST"));
                    if (scaleLevel == 1) view.setScale(MapView.Scale.CLOSE);
                    else if (scaleLevel == 2) view.setScale(MapView.Scale.NORMAL);
                    else if (scaleLevel == 3) view.setScale(MapView.Scale.FAR);
                    else if (scaleLevel == 4) view.setScale(MapView.Scale.FARTHEST);

                    ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
                    MapMeta meta = (MapMeta) mapItem.getItemMeta();
                    if (meta != null) {
                        meta.setMapView(view);
                        meta.setDisplayName("§6Cartographer Scan Map");
                        mapItem.setItemMeta(meta);
                    }

                    // Try to store map in adjacent container
                    boolean deposited = false;
                    for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN}) {
                        Block rel = location.getBlock().getRelative(face);
                        if (rel != null && rel.getState() instanceof InventoryHolder holder) {
                            Inventory inv = holder.getInventory();
                            if (inv.firstEmpty() != -1) {
                                inv.addItem(mapItem);
                                deposited = true;
                                break;
                            }
                        }
                    }

                    if (!deposited) {
                        world.dropItemNaturally(location.clone().add(0, 1, 0), mapItem);
                    }

                    return varargsOf(LuaBoolean.TRUE, LuaInteger.valueOf(view.getId()));
                });
            }
        });

        // cartographer.renderToMonitor(side, [radius])
        table.set("renderToMonitor", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String sideStr = args.checkjstring(1);
                int radius = args.narg() >= 2 ? Math.min(8, Math.max(1, args.checkint(2))) : 4;
                BlockFace face = TransposerPeripheral.parseFace(sideStr);

                return SyncDispatcher.sync(plugin, () -> {
                    Block rel = location.getBlock().getRelative(face);
                    World world = location.getWorld();
                    if (world == null) {
                        return varargsOf(LuaBoolean.FALSE, LuaString.valueOf("World not loaded"));
                    }

                    MonitorPeripheral monitor = new MonitorPeripheral(plugin, rel.getLocation());
                    StringBuilder sb = new StringBuilder();
                    sb.append("=== MAP SCAN (R:").append(radius).append(") ===\n");

                    int originX = location.getBlockX();
                    int originZ = location.getBlockZ();
                    int selfY = location.getBlockY();

                    for (int dz = -radius; dz <= radius; dz++) {
                        for (int dx = -radius; dx <= radius; dx++) {
                            int x = originX + dx;
                            int z = originZ + dz;
                            int highY = world.getHighestBlockYAt(x, z);
                            Block top = world.getBlockAt(x, highY, z);
                            Material mat = top.getType();

                            if (dx == 0 && dz == 0) {
                                sb.append("X"); // Self
                            } else if (mat == Material.WATER) {
                                sb.append("~");
                            } else if (mat == Material.LAVA) {
                                sb.append("!");
                            } else if (highY > selfY + 2) {
                                sb.append("^"); // High terrain
                            } else if (highY < selfY - 2) {
                                sb.append("v"); // Low terrain
                            } else {
                                sb.append(".");
                            }
                        }
                        sb.append("\n");
                    }

                    monitor.setText(sb.toString().trim());
                    return varargsOf(LuaBoolean.TRUE, LuaString.valueOf("Rendered to monitor"));
                });
            }
        });

        return table;
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.turtle;

import com.multiverse.programming.MultiverseProgrammingPlugin;
import com.multiverse.programming.blueprint.Blueprint;
import com.multiverse.programming.blueprint.BlueprintRotator;
import com.multiverse.programming.peripheral.Peripheral;
import com.multiverse.programming.peripheral.SyncDispatcher;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
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
import java.util.Map;
import java.util.Objects;

/**
 * Exposes the 'turtle' API to Lua programs, providing movement,
 * block interaction, inventory manipulation, and automated blueprint construction.
 */
public final class TurtlePeripheral implements Peripheral {

    private final MultiverseProgrammingPlugin plugin;
    private final Turtle turtle;

    public TurtlePeripheral(MultiverseProgrammingPlugin plugin, Turtle turtle) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.turtle = Objects.requireNonNull(turtle, "turtle cannot be null");
    }

    @Override
    public String getType() {
        return "turtle";
    }

    @Override
    public Location getLocation() {
        return turtle.getLocation();
    }

    @Override
    public LuaTable toLuaTable() {
        LuaTable t = new LuaTable();

        // 1. Movement
        t.set("forward", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                Location oldLoc = turtle.getLocation();
                boolean ok = turtle.forward();
                if (ok && plugin.getTurtleManager() != null) {
                    plugin.getTurtleManager().updateTurtleLocation(turtle, oldLoc, turtle.getLocation());
                }
                return LuaBoolean.valueOf(ok);
            }
        });

        t.set("back", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                Location oldLoc = turtle.getLocation();
                boolean ok = turtle.back();
                if (ok && plugin.getTurtleManager() != null) {
                    plugin.getTurtleManager().updateTurtleLocation(turtle, oldLoc, turtle.getLocation());
                }
                return LuaBoolean.valueOf(ok);
            }
        });

        t.set("up", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                Location oldLoc = turtle.getLocation();
                boolean ok = turtle.up();
                if (ok && plugin.getTurtleManager() != null) {
                    plugin.getTurtleManager().updateTurtleLocation(turtle, oldLoc, turtle.getLocation());
                }
                return LuaBoolean.valueOf(ok);
            }
        });

        t.set("down", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                Location oldLoc = turtle.getLocation();
                boolean ok = turtle.down();
                if (ok && plugin.getTurtleManager() != null) {
                    plugin.getTurtleManager().updateTurtleLocation(turtle, oldLoc, turtle.getLocation());
                }
                return LuaBoolean.valueOf(ok);
            }
        });

        t.set("turnLeft", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaBoolean.valueOf(turtle.turnLeft());
            }
        });

        t.set("turnRight", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaBoolean.valueOf(turtle.turnRight());
            }
        });

        // 2. Digging and Placing
        t.set("dig", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaBoolean.valueOf(turtle.dig());
            }
        });

        t.set("digUp", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaBoolean.valueOf(turtle.digUp());
            }
        });

        t.set("digDown", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaBoolean.valueOf(turtle.digDown());
            }
        });

        t.set("place", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaBoolean.valueOf(turtle.place());
            }
        });

        t.set("placeUp", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaBoolean.valueOf(turtle.placeUp());
            }
        });

        t.set("placeDown", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaBoolean.valueOf(turtle.placeDown());
            }
        });

        // 3. Inventory & Fuel
        t.set("select", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                int slot = arg.checkint();
                if (slot < 1 || slot > 16) {
                    return LuaBoolean.valueOf(false);
                }
                turtle.setSelectedSlot(slot - 1);
                return LuaBoolean.valueOf(true);
            }
        });

        t.set("getSelectedSlot", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaInteger.valueOf(turtle.getSelectedSlot() + 1);
            }
        });

        t.set("getItemCount", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                int slot = arg.optint(turtle.getSelectedSlot() + 1);
                if (slot < 1 || slot > 16) return LuaInteger.valueOf(0);
                ItemStack item = turtle.getItem(slot - 1);
                return LuaInteger.valueOf(item != null ? item.getAmount() : 0);
            }
        });

        t.set("getItemDetail", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                int slot = arg.optint(turtle.getSelectedSlot() + 1);
                if (slot < 1 || slot > 16) return LuaValue.NIL;
                ItemStack item = turtle.getItem(slot - 1);
                if (item == null || item.getType().isAir()) return LuaValue.NIL;

                LuaTable detail = new LuaTable();
                detail.set("name", item.getType().name().toLowerCase(Locale.ROOT));
                detail.set("count", item.getAmount());
                detail.set("maxCount", item.getMaxStackSize());
                return detail;
            }
        });

        t.set("getFuelLevel", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaInteger.valueOf(turtle.getFuel());
            }
        });

        t.set("refuel", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                int count = arg.optint(0);
                return LuaBoolean.valueOf(turtle.refuel(count));
            }
        });

        // 4. Info & Status
        t.set("getId", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaString.valueOf(turtle.getId());
            }
        });

        t.set("getLocation", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                Location loc = turtle.getLocation();
                LuaTable info = new LuaTable();
                info.set("x", loc.getBlockX());
                info.set("y", loc.getBlockY());
                info.set("z", loc.getBlockZ());
                info.set("world", loc.getWorld() != null ? loc.getWorld().getName() : "unknown");
                info.set("facing", turtle.getFacing().name().toLowerCase(Locale.ROOT));
                return info;
            }
        });

        t.set("getStatus", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                LuaTable s = new LuaTable();
                s.set("state", turtle.getStatus().name());
                s.set("message", turtle.getStatusMessage());
                return s;
            }
        });

        // 5. Blueprint Construction API
        t.set("loadBlueprint", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                String bpId = arg.checkjstring();
                if (plugin.getBlueprintManager() == null) {
                    return LuaValue.NIL;
                }
                Blueprint bp = plugin.getBlueprintManager().getBlueprint(bpId);
                if (bp == null) {
                    return LuaValue.NIL;
                }
                return blueprintToLuaTable(bp);
            }
        });

        t.set("importBlueprint", t.get("loadBlueprint"));

        t.set("buildBlueprint", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String bpId = args.checkjstring(1);
                if (plugin.getBlueprintManager() == null) {
                    return varargsOf(LuaBoolean.FALSE, LuaString.valueOf("Blueprint manager not available"));
                }
                Blueprint bp = plugin.getBlueprintManager().getBlueprint(bpId);
                if (bp == null) {
                    return varargsOf(LuaBoolean.FALSE, LuaString.valueOf("Blueprint not found: " + bpId));
                }

                if (args.narg() < 4) {
                    return varargsOf(LuaBoolean.FALSE, LuaString.valueOf("Coordinates (x, y, z) are mandatory for turtle.build(bpId, x, y, z, [clear], [orientation])"));
                }

                int x = args.checkint(2);
                int y = args.checkint(3);
                int z = args.checkint(4);
                boolean clearBlocks = false;
                int rotationDegrees = 0;

                if (args.narg() >= 5) {
                    LuaValue arg5 = args.arg(5);
                    if (arg5.isboolean()) {
                        clearBlocks = arg5.toboolean();
                        if (args.narg() >= 6) {
                            LuaValue arg6 = args.arg(6);
                            if (arg6.isnumber()) {
                                rotationDegrees = arg6.toint();
                            } else if (arg6.isstring()) {
                                rotationDegrees = BlueprintRotator.normalizeRotation(arg6.tojstring());
                            }
                        }
                    } else if (arg5.isnumber()) {
                        rotationDegrees = arg5.toint();
                    } else if (arg5.isstring()) {
                        rotationDegrees = BlueprintRotator.normalizeRotation(arg5.tojstring());
                    }
                }

                if (turtle.getLocation().getWorld() == null) {
                    return varargsOf(LuaBoolean.FALSE, LuaString.valueOf("Turtle world is unloaded"));
                }
                Location origin = new Location(turtle.getLocation().getWorld(), x, y, z);

                int delay = plugin.getConfigManager().getTurtleBuildDelayTicks();
                boolean requireMaterials = plugin.getConfigManager().isTurtleRequireMaterials();

                final boolean finalClear = clearBlocks;
                final int finalRotation = rotationDegrees;
                boolean started = SyncDispatcher.sync(plugin, () ->
                        turtle.startBuild(bp, origin, delay, requireMaterials, finalClear, finalRotation, null, null)
                );

                if (!started) {
                    return varargsOf(LuaBoolean.FALSE, LuaString.valueOf(turtle.getStatusMessage()));
                }
                return varargsOf(LuaBoolean.TRUE, LuaString.valueOf("Build started for " + bp.name()));
            }
        });

        t.set("build", t.get("buildBlueprint"));

        t.set("getBuildProgress", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                LuaTable p = new LuaTable();
                p.set("active", LuaBoolean.valueOf(turtle.getStatus() == Turtle.Status.BUILDING || turtle.getStatus() == Turtle.Status.PAUSED));
                p.set("blueprintId", turtle.getActiveBlueprintId() != null ? turtle.getActiveBlueprintId() : "");
                p.set("current", turtle.getCurrentBlockIndex());
                p.set("total", turtle.getTotalBlocks());
                p.set("percentage", turtle.getProgressPercentage());
                p.set("status", turtle.getStatus().name());
                p.set("message", turtle.getStatusMessage());
                return p;
            }
        });

        t.set("pauseBuild", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                turtle.pauseBuild();
                return LuaBoolean.TRUE;
            }
        });

        t.set("resumeBuild", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                turtle.resumeBuild();
                return LuaBoolean.TRUE;
            }
        });

        t.set("cancelBuild", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                turtle.cancelBuild();
                return LuaBoolean.TRUE;
            }
        });

        // 6. Quarry Engine Upgrade API
        t.set("hasQuarryEngine", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaBoolean.valueOf(turtle.hasQuarryEngineAttached());
            }
        });

        t.set("quarry", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (args.narg() < 3) {
                    return varargsOf(LuaBoolean.FALSE, LuaString.valueOf("Usage: turtle.quarry(width, length, targetY, [handleLiquids])"));
                }
                int width = args.checkint(1);
                int length = args.checkint(2);
                int targetY = args.checkint(3);
                boolean handleLiquids = args.narg() < 4 || args.checkboolean(4);

                boolean started = SyncDispatcher.sync(plugin, () ->
                        turtle.startQuarry(width, length, targetY, handleLiquids, null, null)
                );

                if (!started) {
                    return varargsOf(LuaBoolean.FALSE, LuaString.valueOf(turtle.getStatusMessage()));
                }
                return varargsOf(LuaBoolean.TRUE, LuaString.valueOf("Quarry excavation started"));
            }
        });

        t.set("startQuarry", t.get("quarry"));

        t.set("pauseQuarry", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                turtle.pauseQuarry();
                return LuaBoolean.TRUE;
            }
        });

        t.set("resumeQuarry", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                turtle.resumeQuarry();
                return LuaBoolean.TRUE;
            }
        });

        t.set("stopQuarry", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                turtle.cancelQuarry();
                return LuaBoolean.TRUE;
            }
        });

        t.set("cancelQuarry", t.get("stopQuarry"));

        t.set("getQuarryStatus", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                LuaTable s = new LuaTable();
                s.set("active", LuaBoolean.valueOf(turtle.isQuarryActive()));
                s.set("paused", LuaBoolean.valueOf(turtle.isQuarryPaused()));
                s.set("status", turtle.getStatus().name());
                s.set("message", turtle.getStatusMessage());
                s.set("blocksMined", turtle.getQuarryBlocksMined());
                s.set("totalBlocks", turtle.getQuarryTotalBlocks());
                s.set("percentage", turtle.getQuarryProgressPercentage());
                s.set("currentY", turtle.getQuarryCurrentY());
                s.set("targetY", turtle.getQuarryTargetY());
                Turtle.LateralSide side = turtle.getQuarryLateralSide();
                if (side != null) {
                    s.set("side", side.name().toLowerCase(Locale.ROOT));
                } else {
                    Turtle.LateralSide detected = turtle.findLateralQuarrySide();
                    if (detected != null) {
                        s.set("side", detected.name().toLowerCase(Locale.ROOT));
                    }
                }
                return s;
            }
        });

        return t;
    }

    private static LuaTable blueprintToLuaTable(Blueprint bp) {
        LuaTable table = new LuaTable();
        table.set("id", bp.id());
        table.set("name", bp.name());
        table.set("author", bp.author() != null ? bp.author() : "Unknown");
        table.set("format", bp.format());
        table.set("sizeX", bp.sizeX());
        table.set("sizeY", bp.sizeY());
        table.set("sizeZ", bp.sizeZ());
        table.set("totalBlocks", bp.totalBlocks());

        LuaTable materials = new LuaTable();
        for (Map.Entry<String, Integer> entry : bp.materialCounts().entrySet()) {
            materials.set(entry.getKey(), entry.getValue());
        }
        table.set("materials", materials);
        return table;
    }
}

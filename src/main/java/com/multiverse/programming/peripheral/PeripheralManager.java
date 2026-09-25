// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.peripheral;

import com.multiverse.programming.ConfigManager;
import com.multiverse.programming.LuaRunner;
import com.multiverse.programming.MultiverseProgrammingPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manages detection, attachment, and binding of hardware peripherals
 * (monitors, auto-crafters, transposers, speakers) to Lua execution environments.
 */
public final class PeripheralManager {

    private static final BlockFace[] ADJACENT_FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST
    };

    private PeripheralManager() {
    }

    public static Map<String, Peripheral> findPeripherals(MultiverseProgrammingPlugin plugin, Location computerLoc) {
        Map<String, Peripheral> result = new LinkedHashMap<>();
        if (computerLoc == null || computerLoc.getWorld() == null) {
            return result;
        }

        ConfigManager config = plugin.getConfigManager();
        Material monitorMat = config.getMonitorBlock();
        Material crafterMat = config.getCrafterBlock();
        Material transposerMat = config.getTransposerBlock();
        Material speakerMat = config.getSpeakerBlock();

        Block computerBlock = computerLoc.getBlock();
        for (BlockFace face : ADJACENT_FACES) {
            Block adj = computerBlock.getRelative(face);
            if (adj == null) {
                continue;
            }
            Material adjType = adj.getType();
            String dirName = face.name().toLowerCase(Locale.ROOT);

            if (adjType == monitorMat && (config == null || config.isEnableMonitor())) {
                result.put(dirName, new MonitorPeripheral(plugin, adj.getLocation()));
            } else if (adjType == crafterMat && (config == null || config.isEnableCrafter())) {
                result.put(dirName, new CrafterPeripheral(plugin, adj.getLocation()));
            } else if (adjType == transposerMat && (config == null || config.isEnableTransposer())) {
                result.put(dirName, new TransposerPeripheral(plugin, adj.getLocation()));
            } else if (adjType == speakerMat && (config == null || config.isEnableSpeaker())) {
                result.put(dirName, new SpeakerPeripheral(plugin, adj.getLocation()));
            }
        }
        return result;
    }

    public static void bindAll(Globals globals, MultiverseProgrammingPlugin plugin, Location computerLoc, boolean isAdvanced) {
        // 1. Bind cooperative sleep function
        globals.set("sleep", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                double val = arg.checkdouble();
                // If val <= 10.0, treat as seconds (e.g. 0.5 -> 500ms, 1 -> 1000ms)
                // If val > 10.0, treat directly as milliseconds (e.g. 100 -> 100ms)
                long ms = (val > 0 && val <= 10.0) ? (long) (val * 1000.0) : (long) val;
                if (ms < 1) ms = 1;

                if (globals.debuglib instanceof LuaRunner.InterruptHookDebugLib hook) {
                    hook.resetSlice();
                }

                try {
                    Thread.sleep(ms);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new LuaError("execution interrupted");
                }
                return LuaValue.NONE;
            }
        });

        if (plugin == null || computerLoc == null) {
            return;
        }

        // 2. Discover adjacent peripherals
        Map<String, Peripheral> peripherals = findPeripherals(plugin, computerLoc);

        // 3. Bind direct convenience globals (first of each type found)
        for (Peripheral p : peripherals.values()) {
            if (globals.get(p.getType()).isnil()) {
                globals.set(p.getType(), p.toLuaTable());
            }
        }

        // 4. Bind 'peripheral' library table
        LuaTable peripheralLib = new LuaTable();

        // peripheral.getNames() -> array of attached sides
        peripheralLib.set("getNames", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                LuaTable names = new LuaTable();
                int idx = 1;
                for (String dir : peripherals.keySet()) {
                    names.set(idx++, dir);
                }
                return names;
            }
        });

        // peripheral.getType(side) -> type string or nil
        peripheralLib.set("getType", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                String side = arg.checkjstring().toLowerCase(Locale.ROOT);
                Peripheral p = peripherals.get(side);
                return p != null ? LuaValue.valueOf(p.getType()) : LuaValue.NIL;
            }
        });

        // peripheral.wrap(side) -> peripheral table or nil
        peripheralLib.set("wrap", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                String side = arg.checkjstring().toLowerCase(Locale.ROOT);
                Peripheral p = peripherals.get(side);
                return p != null ? p.toLuaTable() : LuaValue.NIL;
            }
        });

        // peripheral.find(type) -> first peripheral matching type
        peripheralLib.set("find", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                String type = arg.checkjstring().toLowerCase(Locale.ROOT);
                for (Peripheral p : peripherals.values()) {
                    if (p.getType().equalsIgnoreCase(type)) {
                        return p.toLuaTable();
                    }
                }
                return LuaValue.NIL;
            }
        });

        globals.set("peripheral", peripheralLib);
    }
}

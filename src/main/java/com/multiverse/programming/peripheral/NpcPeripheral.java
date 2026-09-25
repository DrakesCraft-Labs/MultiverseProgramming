// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.peripheral;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NPC Chatbot & Quest Interposer peripheral.
 * Creates interactive dialogue prompts, chat options, and floating holograms.
 */
public final class NpcPeripheral implements Peripheral {

    private final JavaPlugin plugin;
    private final Location location;

    private String npcName = "§b[NPC]";
    private Entity hologramEntity;
    private final Map<String, String> playerResponses = new ConcurrentHashMap<>();
    private final Map<String, Long> lastInteractionTimes = new ConcurrentHashMap<>();
    private static final java.util.List<NpcPeripheral> ACTIVE_NPCS = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static java.util.List<NpcPeripheral> getActiveNpcs() {
        return ACTIVE_NPCS;
    }

    public NpcPeripheral(JavaPlugin plugin, Location location) {
        this.plugin = plugin;
        this.location = location;
        ACTIVE_NPCS.add(this);
    }

    @Override
    public String getType() {
        return "npc";
    }

    @Override
    public Location getLocation() {
        return location;
    }

    public void recordPlayerResponse(String playerName, String response) {
        playerResponses.put(playerName.toLowerCase(), response);
        lastInteractionTimes.put(playerName.toLowerCase(), System.currentTimeMillis());
    }

    public synchronized void updateHologram(String text) {
        clearHologram();
        World world = location.getWorld();
        if (world == null) return;

        Location holoLoc = location.clone().add(0.5, 1.3, 0.5);
        try {
            this.hologramEntity = world.spawn(holoLoc, TextDisplay.class, display -> {
                display.setText(text);
                display.setBillboard(Display.Billboard.CENTER);
                display.setDefaultBackground(false);
                display.setSeeThrough(true);
            });
        } catch (Throwable fallback) {
            try {
                this.hologramEntity = world.spawn(holoLoc.clone().subtract(0, 1.0, 0), ArmorStand.class, as -> {
                    as.setCustomName(text);
                    as.setCustomNameVisible(true);
                    as.setVisible(false);
                    as.setGravity(false);
                    as.setMarker(true);
                });
            } catch (Throwable ignored) {}
        }
    }

    public synchronized void clearHologram() {
        if (hologramEntity != null && hologramEntity.isValid()) {
            try {
                hologramEntity.remove();
            } catch (Throwable ignored) {}
            hologramEntity = null;
        }
    }

    @Override
    public LuaValue toLuaTable() {
        LuaTable table = new LuaTable();

        // npc.setName(name)
        table.set("setName", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                npcName = arg.checkjstring();
                SyncDispatcher.sync(plugin, () -> {
                    updateHologram(npcName);
                    return null;
                });
                return LuaBoolean.TRUE;
            }
        });

        // npc.getName()
        table.set("getName", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaString.valueOf(npcName);
            }
        });

        // npc.say(playerName, message)
        table.set("say", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                String pName = arg1.checkjstring();
                String msg = arg2.checkjstring();
                SyncDispatcher.sync(plugin, () -> {
                    Player p = Bukkit.getPlayerExact(pName);
                    if (p != null && p.isOnline()) {
                        p.sendMessage(npcName + " §f" + msg);
                    }
                    return null;
                });
                return LuaBoolean.TRUE;
            }
        });

        // npc.broadcast(message, [radius])
        table.set("broadcast", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String msg = args.checkjstring(1);
                int radius = args.narg() >= 2 ? args.checkint(2) : 16;

                SyncDispatcher.sync(plugin, () -> {
                    World world = location.getWorld();
                    if (world == null) return null;

                    for (Player p : world.getPlayers()) {
                        if (p.getLocation().distance(location) <= radius) {
                            p.sendMessage(npcName + " §f" + msg);
                        }
                    }
                    return null;
                });
                return LuaBoolean.TRUE;
            }
        });

        // npc.ask(playerName, question, optionsTable)
        table.set("ask", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String pName = args.checkjstring(1);
                String question = args.checkjstring(2);
                LuaTable options = args.checktable(3);

                SyncDispatcher.sync(plugin, () -> {
                    Player p = Bukkit.getPlayerExact(pName);
                    if (p != null && p.isOnline()) {
                        p.sendMessage(npcName + " §e" + question);
                        int len = options.length();
                        for (int i = 1; i <= len; i++) {
                            String opt = options.get(i).tojstring();
                            p.sendMessage(" §8[§6" + i + "§8] §a" + opt);
                        }
                        p.sendMessage(" §7(Type the option number or text in chat to respond)");
                    }
                    return null;
                });
                return LuaBoolean.TRUE;
            }
        });

        // npc.getLastResponse(playerName) -> string or nil
        table.set("getLastResponse", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                String pName = arg.checkjstring().toLowerCase();
                String resp = playerResponses.get(pName);
                return resp != null ? LuaString.valueOf(resp) : LuaValue.NIL;
            }
        });

        // npc.clearResponse(playerName)
        table.set("clearResponse", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                playerResponses.remove(arg.checkjstring().toLowerCase());
                return LuaBoolean.TRUE;
            }
        });

        // npc.setHologram(text)
        table.set("setHologram", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                String text = arg.checkjstring();
                SyncDispatcher.sync(plugin, () -> {
                    updateHologram(text);
                    return null;
                });
                return LuaBoolean.TRUE;
            }
        });

        // npc.clearHologram()
        table.set("clearHologram", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                SyncDispatcher.sync(plugin, () -> {
                    clearHologram();
                    return null;
                });
                return LuaBoolean.TRUE;
            }
        });

        return table;
    }
}

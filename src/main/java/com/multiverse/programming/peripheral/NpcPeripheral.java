// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.peripheral;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
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

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NPC Chatbot & Quest Interposer peripheral.
 * Spawns real interactive NPC entities (Villager by default or customizable living entity),
 * dialogue prompts, floating holograms, and chat option listeners.
 */
public final class NpcPeripheral implements Peripheral {

    public static final String NPC_TAG = "multiverse_npc";
    public static final String HOLOGRAM_TAG = "multiverse_npc_hologram";

    private final JavaPlugin plugin;
    private final Location location;

    private String npcName = "§b[NPC]";
    private Entity npcEntity;
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
        playerResponses.put(playerName.toLowerCase(Locale.ROOT), response);
        lastInteractionTimes.put(playerName.toLowerCase(Locale.ROOT), System.currentTimeMillis());
    }

    public synchronized boolean spawnNpc(String typeName) {
        World world = location.getWorld();
        if (world == null) return false;

        despawnNpc();

        Location spawnLoc = location.clone().add(0.5, 1.0, 0.5);

        EntityType type = EntityType.VILLAGER;
        if (typeName != null && !typeName.isBlank()) {
            try {
                type = EntityType.valueOf(typeName.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                for (EntityType et : EntityType.values()) {
                    if (et.name().equalsIgnoreCase(typeName.trim())) {
                        type = et;
                        break;
                    }
                }
            }
        }

        try {
            Entity entity = world.spawnEntity(spawnLoc, type);
            if (entity != null) {
                entity.addScoreboardTag(NPC_TAG);
                entity.setCustomName(npcName);
                entity.setCustomNameVisible(true);
                if (entity instanceof LivingEntity living) {
                    living.setAI(false);
                    living.setInvulnerable(true);
                    living.setSilent(true);
                    living.setCollidable(false);
                    living.setRemoveWhenFarAway(false);
                }
                if (entity instanceof Villager villager) {
                    villager.setProfession(Villager.Profession.NITWIT);
                    try {
                        villager.setRecipes(java.util.Collections.emptyList());
                        villager.setVillagerExperience(0);
                        villager.setVillagerLevel(1);
                        villager.setBreed(false);
                        villager.setAgeLock(true);
                    } catch (Throwable ignored) {}
                }
                this.npcEntity = entity;
                return true;
            }
            return false;
        } catch (Throwable t) {
            try {
                Villager entity = world.spawn(spawnLoc, Villager.class, v -> {
                    v.addScoreboardTag(NPC_TAG);
                    v.setCustomName(npcName);
                    v.setCustomNameVisible(true);
                    v.setAI(false);
                    v.setInvulnerable(true);
                    v.setSilent(true);
                    v.setCollidable(false);
                    v.setRemoveWhenFarAway(false);
                    v.setProfession(Villager.Profession.NITWIT);
                    try {
                        v.setRecipes(java.util.Collections.emptyList());
                        v.setVillagerExperience(0);
                        v.setVillagerLevel(1);
                        v.setBreed(false);
                        v.setAgeLock(true);
                    } catch (Throwable ignored) {}
                });
                this.npcEntity = entity;
                return entity != null;
            } catch (Throwable ignored) {
                return false;
            }
        }
    }

    public synchronized void despawnNpc() {
        if (npcEntity != null && npcEntity.isValid()) {
            try {
                npcEntity.remove();
            } catch (Throwable ignored) {}
            npcEntity = null;
        }
        cleanupEntitiesAt(location);
    }

    public synchronized void updateHologram(String text) {
        clearHologram();
        World world = location.getWorld();
        if (world == null) return;

        double yOffset = (npcEntity != null && npcEntity.isValid()) ? 2.25 : 1.3;
        Location holoLoc = location.clone().add(0.5, yOffset, 0.5);
        try {
            this.hologramEntity = world.spawn(holoLoc, TextDisplay.class, display -> {
                display.addScoreboardTag(HOLOGRAM_TAG);
                display.setText(text);
                display.setBillboard(Display.Billboard.CENTER);
                display.setDefaultBackground(false);
                display.setSeeThrough(true);
            });
        } catch (Throwable fallback) {
            try {
                this.hologramEntity = world.spawn(holoLoc.clone().subtract(0, 1.0, 0), ArmorStand.class, as -> {
                    as.addScoreboardTag(HOLOGRAM_TAG);
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
        cleanupHologramsAt(location);
    }

    public synchronized void removeAll() {
        despawnNpc();
        clearHologram();
        ACTIVE_NPCS.remove(this);
    }

    public static void cleanupEntitiesAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        Location center = loc.clone().add(0.5, 1.0, 0.5);
        try {
            Collection<Entity> nearby = loc.getWorld().getNearbyEntities(center, 1.5, 2.5, 1.5);
            for (Entity e : nearby) {
                if (e.getScoreboardTags().contains(NPC_TAG)) {
                    e.remove();
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void cleanupHologramsAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        Location center = loc.clone().add(0.5, 1.5, 0.5);
        try {
            Collection<Entity> nearby = loc.getWorld().getNearbyEntities(center, 1.5, 2.5, 1.5);
            for (Entity e : nearby) {
                if (e.getScoreboardTags().contains(HOLOGRAM_TAG)) {
                    e.remove();
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void cleanupAt(Location loc) {
        cleanupEntitiesAt(loc);
        cleanupHologramsAt(loc);
    }

    @Override
    public LuaValue toLuaTable() {
        LuaTable table = new LuaTable();

        // npc.setName(name, [spawnEntity=true])
        table.set("setName", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                npcName = args.checkjstring(1);
                boolean spawn = args.narg() < 2 || args.checkboolean(2);
                SyncDispatcher.sync(plugin, () -> {
                    if (npcEntity != null && npcEntity.isValid()) {
                        npcEntity.setCustomName(npcName);
                        npcEntity.setCustomNameVisible(true);
                    } else if (spawn) {
                        spawnNpc("VILLAGER");
                    }
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

        // npc.spawn([entityType]) -> boolean
        table.set("spawn", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String type = args.narg() >= 1 ? args.checkjstring(1) : "VILLAGER";
                boolean ok = SyncDispatcher.sync(plugin, () -> {
                    boolean spawned = spawnNpc(type);
                    updateHologram(npcName);
                    return spawned;
                });
                return LuaBoolean.valueOf(ok);
            }
        });

        // npc.create([name], [entityType]) -> boolean
        table.set("create", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (args.narg() >= 1) {
                    npcName = args.checkjstring(1);
                }
                String type = args.narg() >= 2 ? args.checkjstring(2) : "VILLAGER";
                boolean ok = SyncDispatcher.sync(plugin, () -> {
                    boolean spawned = spawnNpc(type);
                    updateHologram(npcName);
                    return spawned;
                });
                return LuaBoolean.valueOf(ok);
            }
        });

        // npc.despawn() / npc.remove() / npc.destroy()
        table.set("despawn", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                SyncDispatcher.sync(plugin, () -> {
                    removeAll();
                    return null;
                });
                return LuaBoolean.TRUE;
            }
        });
        table.set("remove", table.get("despawn"));
        table.set("destroy", table.get("despawn"));

        // npc.isSpawned() -> boolean
        table.set("isSpawned", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaBoolean.valueOf(npcEntity != null && npcEntity.isValid());
            }
        });
        table.set("hasSpawned", table.get("isSpawned"));

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
                String pName = arg.checkjstring().toLowerCase(Locale.ROOT);
                String resp = playerResponses.get(pName);
                return resp != null ? LuaString.valueOf(resp) : LuaValue.NIL;
            }
        });

        // npc.clearResponse(playerName)
        table.set("clearResponse", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                playerResponses.remove(arg.checkjstring().toLowerCase(Locale.ROOT));
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

        // npc.clearHologram() / npc.clear()
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
        table.set("clear", table.get("clearHologram"));

        return table;
    }
}

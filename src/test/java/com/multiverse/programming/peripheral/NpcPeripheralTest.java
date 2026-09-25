// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.peripheral;

import com.multiverse.programming.BukkitMockHelper;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcPeripheralTest {

    private JavaPlugin plugin;
    private World world;
    private Location location;
    private NpcPeripheral npc;

    @BeforeEach
    void setUp() {
        BukkitMockHelper.setUpMockServer();
        plugin = mock(JavaPlugin.class);
        when(plugin.isEnabled()).thenReturn(true);

        world = mock(World.class);
        location = new Location(world, 0, 64, 0);
        npc = new NpcPeripheral(plugin, location);
    }

    @AfterEach
    void tearDown() {
        BukkitMockHelper.tearDownMockServer();
    }

    @Test
    @DisplayName("Npc peripheral reports type and location")
    void testBasicProperties() {
        assertEquals("npc", npc.getType());
        assertEquals(location, npc.getLocation());
    }

    @Test
    @DisplayName("Npc setName and getName update dialogue identity")
    void testName() {
        LuaTable table = npc.toLuaTable().checktable();
        table.get("setName").call(LuaValue.valueOf("Guard"));
        assertEquals("Guard", table.get("getName").call().tojstring());
    }

    @Test
    @DisplayName("Npc records and retrieves player response")
    void testPlayerResponses() {
        LuaTable table = npc.toLuaTable().checktable();
        npc.recordPlayerResponse("Steve", "Yes, I accept the quest!");

        LuaValue response = table.get("getLastResponse").call(LuaValue.valueOf("Steve"));
        assertEquals("Yes, I accept the quest!", response.tojstring());

        table.get("clearResponse").call(LuaValue.valueOf("Steve"));
        assertTrue(table.get("getLastResponse").call(LuaValue.valueOf("Steve")).isnil());
    }
}

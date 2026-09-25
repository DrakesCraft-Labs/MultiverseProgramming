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
import org.luaj.vm2.Varargs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuarryPeripheralTest {

    private JavaPlugin plugin;
    private World world;
    private Location location;
    private QuarryPeripheral quarry;

    @BeforeEach
    void setUp() {
        BukkitMockHelper.setUpMockServer();
        plugin = mock(JavaPlugin.class);
        when(plugin.isEnabled()).thenReturn(true);

        world = mock(World.class);
        when(world.getMinHeight()).thenReturn(-64);
        location = new Location(world, 10, 64, 10);
        quarry = new QuarryPeripheral(plugin, location);
    }

    @AfterEach
    void tearDown() {
        BukkitMockHelper.tearDownMockServer();
    }

    @Test
    @DisplayName("Quarry peripheral reports type and location")
    void testBasicProperties() {
        assertEquals("quarry", quarry.getType());
        assertEquals(location, quarry.getLocation());
    }

    @Test
    @DisplayName("Quarry initial status is inactive and idle")
    void testInitialStatus() {
        LuaTable table = quarry.toLuaTable().checktable();
        LuaValue statusVal = table.get("getStatus").call();

        assertTrue(statusVal.istable());
        LuaTable status = statusVal.checktable();
        assertFalse(status.get("active").toboolean());
        assertFalse(status.get("paused").toboolean());
        assertEquals("Idle", status.get("message").tojstring());
    }

    @Test
    @DisplayName("Quarry pauses and resumes properly")
    void testPauseAndResume() {
        LuaTable table = quarry.toLuaTable().checktable();
        table.get("pause").call();

        LuaTable status = table.get("getStatus").call().checktable();
        assertTrue(status.get("paused").toboolean());

        table.get("resume").call();
        status = table.get("getStatus").call().checktable();
        assertFalse(status.get("paused").toboolean());
    }
}

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

class MonitorPeripheralTest {

    private JavaPlugin plugin;
    private Location location;
    private MonitorPeripheral monitor;

    @BeforeEach
    void setUp() {
        BukkitMockHelper.setUpMockServer();
        plugin = mock(JavaPlugin.class);
        when(plugin.isEnabled()).thenReturn(true);
        World world = mock(World.class);
        location = new Location(world, 10, 64, 10);
        monitor = new MonitorPeripheral(plugin, location);
    }

    @AfterEach
    void tearDown() {
        BukkitMockHelper.tearDownMockServer();
    }

    @Test
    @DisplayName("MonitorPeripheral reports type as monitor and correct location")
    void testTypeAndLocation() {
        assertEquals("monitor", monitor.getType());
        assertEquals(location, monitor.getLocation());
    }

    @Test
    @DisplayName("Monitor Lua table supports write, setLine, getLine, clear, setText, and getText")
    void testLuaMethods() {
        LuaValue table = monitor.toLuaTable();
        assertTrue(table.istable());

        // write appends lines
        table.get("write").call(LuaValue.valueOf("Line 1"));
        table.get("write").call(LuaValue.valueOf("Line 2"));

        assertEquals(2, table.get("getLineCount").call().toint());
        assertEquals("Line 1", table.get("getLine").call(LuaValue.valueOf(1)).tojstring());
        assertEquals("Line 2", table.get("getLine").call(LuaValue.valueOf(2)).tojstring());
        assertEquals("Line 1\nLine 2", table.get("getText").call().tojstring());

        // setLine updates existing or expands
        table.get("setLine").call(LuaValue.valueOf(1), LuaValue.valueOf("Updated Line 1"));
        assertEquals("Updated Line 1", table.get("getLine").call(LuaValue.valueOf(1)).tojstring());

        // clear empties all lines
        table.get("clear").call();
        assertEquals(0, table.get("getLineCount").call().toint());
        assertEquals("", table.get("getText").call().tojstring());

        // setText sets multiline content
        table.get("setText").call(LuaValue.valueOf("Alpha\nBeta\nGamma"));
        assertEquals(3, table.get("getLineCount").call().toint());
        assertEquals("Beta", table.get("getLine").call(LuaValue.valueOf(2)).tojstring());
    }
}

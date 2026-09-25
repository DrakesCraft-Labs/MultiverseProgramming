// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.peripheral;

import com.multiverse.programming.BukkitMockHelper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FarmerPeripheralTest {

    private JavaPlugin plugin;
    private World world;
    private Location location;
    private FarmerPeripheral farmer;

    @BeforeEach
    void setUp() {
        BukkitMockHelper.setUpMockServer();
        plugin = mock(JavaPlugin.class);
        when(plugin.isEnabled()).thenReturn(true);

        world = mock(World.class);
        location = new Location(world, 0, 64, 0);
        farmer = new FarmerPeripheral(plugin, location);
    }

    @AfterEach
    void tearDown() {
        BukkitMockHelper.tearDownMockServer();
    }

    @Test
    @DisplayName("Farmer peripheral reports type and location")
    void testBasicProperties() {
        assertEquals("farmer", farmer.getType());
        assertEquals(location, farmer.getLocation());
    }

    @Test
    @DisplayName("inspectCrop inspects age and maturity of crops")
    void testInspectCrop() {
        Block cropBlock = mock(Block.class);
        when(cropBlock.getType()).thenReturn(Material.WHEAT);
        Ageable ageable = mock(Ageable.class);
        when(ageable.getAge()).thenReturn(7);
        when(ageable.getMaximumAge()).thenReturn(7);
        when(cropBlock.getBlockData()).thenReturn(ageable);

        when(world.getBlockAt(0, 63, 0)).thenReturn(cropBlock);

        LuaTable table = farmer.toLuaTable().checktable();
        LuaValue res = table.get("inspectCrop").call(LuaValue.valueOf(0), LuaValue.valueOf(63), LuaValue.valueOf(0));

        assertTrue(res.istable());
        LuaTable info = res.checktable();
        assertTrue(info.get("isCrop").toboolean());
        assertTrue(info.get("mature").toboolean());
        assertEquals(7, info.get("age").toint());
        assertEquals(7, info.get("maxAge").toint());
    }
}

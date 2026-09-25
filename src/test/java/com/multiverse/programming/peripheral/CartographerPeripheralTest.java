// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.peripheral;

import com.multiverse.programming.BukkitMockHelper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class CartographerPeripheralTest {

    private JavaPlugin plugin;
    private World world;
    private Location location;
    private CartographerPeripheral cartographer;

    @BeforeEach
    void setUp() {
        BukkitMockHelper.setUpMockServer();
        plugin = mock(JavaPlugin.class);
        when(plugin.isEnabled()).thenReturn(true);

        world = mock(World.class);
        location = new Location(world, 0, 64, 0);
        cartographer = new CartographerPeripheral(plugin, location);
    }

    @AfterEach
    void tearDown() {
        BukkitMockHelper.tearDownMockServer();
    }

    @Test
    @DisplayName("Cartographer peripheral reports type and location")
    void testBasicProperties() {
        assertEquals("cartographer", cartographer.getType());
        assertEquals(location, cartographer.getLocation());
    }

    @Test
    @DisplayName("getBiome returns nil when world is null")
    void testGetBiomeNullWorld() {
        Location noWorldLoc = new Location(null, 0, 0, 0);
        CartographerPeripheral p = new CartographerPeripheral(plugin, noWorldLoc);
        LuaTable table = p.toLuaTable().checktable();
        assertTrue(table.get("getBiome").call().isnil());
    }

    @Test
    @DisplayName("scanTopography returns radius and height points")
    void testScanTopography() {
        when(world.getHighestBlockYAt(anyInt(), anyInt())).thenReturn(64);
        Block topBlock = mock(Block.class);
        when(topBlock.getType()).thenReturn(Material.GRASS_BLOCK);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(topBlock);

        LuaTable table = cartographer.toLuaTable().checktable();
        LuaValue res = table.get("scanTopography").call(LuaValue.valueOf(2));

        assertTrue(res.istable());
        LuaTable resTable = res.checktable();
        assertEquals(2, resTable.get("radius").toint());
        assertTrue(resTable.get("points").istable());
        assertTrue(resTable.get("points").checktable().length() > 0);
    }
}

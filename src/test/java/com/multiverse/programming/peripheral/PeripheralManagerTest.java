// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.peripheral;

import com.multiverse.programming.BukkitMockHelper;
import com.multiverse.programming.ConfigManager;
import com.multiverse.programming.MultiverseProgrammingPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PeripheralManagerTest {

    private MultiverseProgrammingPlugin plugin;
    private ConfigManager config;
    private World world;
    private Location computerLoc;
    private Block computerBlock;

    @BeforeEach
    void setUp() {
        BukkitMockHelper.setUpMockServer();
        plugin = mock(MultiverseProgrammingPlugin.class);
        config = mock(ConfigManager.class);
        when(plugin.getConfigManager()).thenReturn(config);
        when(plugin.isEnabled()).thenReturn(true);

        when(config.getMonitorBlock()).thenReturn(Material.OCHRE_FROGLIGHT);
        when(config.getCrafterBlock()).thenReturn(Material.CRAFTER);
        when(config.getTransposerBlock()).thenReturn(Material.HOPPER);
        when(config.getSpeakerBlock()).thenReturn(Material.NOTE_BLOCK);

        world = mock(World.class);
        computerLoc = mock(Location.class);
        when(computerLoc.getWorld()).thenReturn(world);

        computerBlock = mock(Block.class);
        when(computerLoc.getBlock()).thenReturn(computerBlock);
    }

    @AfterEach
    void tearDown() {
        BukkitMockHelper.tearDownMockServer();
    }

    @Test
    @DisplayName("findPeripherals detects adjacent peripheral blocks")
    void testFindPeripherals() {
        Block upBlock = mock(Block.class);
        when(upBlock.getType()).thenReturn(Material.OCHRE_FROGLIGHT);
        when(computerBlock.getRelative(BlockFace.UP)).thenReturn(upBlock);

        Block northBlock = mock(Block.class);
        when(northBlock.getType()).thenReturn(Material.CRAFTER);
        when(computerBlock.getRelative(BlockFace.NORTH)).thenReturn(northBlock);

        // Others are air
        Block airBlock = mock(Block.class);
        when(airBlock.getType()).thenReturn(Material.AIR);
        when(computerBlock.getRelative(BlockFace.DOWN)).thenReturn(airBlock);
        when(computerBlock.getRelative(BlockFace.SOUTH)).thenReturn(airBlock);
        when(computerBlock.getRelative(BlockFace.EAST)).thenReturn(airBlock);
        when(computerBlock.getRelative(BlockFace.WEST)).thenReturn(airBlock);

        Map<String, Peripheral> peripherals = PeripheralManager.findPeripherals(plugin, computerLoc);

        assertEquals(2, peripherals.size());
        assertTrue(peripherals.containsKey("up"));
        assertEquals("monitor", peripherals.get("up").getType());
        assertTrue(peripherals.containsKey("north"));
        assertEquals("crafter", peripherals.get("north").getType());
    }

    @Test
    @DisplayName("bindAll binds sleep function and peripheral library table")
    void testBindAll() {
        Globals globals = JsePlatform.standardGlobals();
        PeripheralManager.bindAll(globals, plugin, computerLoc, true);

        // Verify sleep is bound
        LuaValue sleepFunc = globals.get("sleep");
        assertFalse(sleepFunc.isnil());
        assertTrue(sleepFunc.isfunction());

        // Verify peripheral table is bound
        LuaValue peripheralTable = globals.get("peripheral");
        assertFalse(peripheralTable.isnil());
        assertTrue(peripheralTable.istable());
    }
}

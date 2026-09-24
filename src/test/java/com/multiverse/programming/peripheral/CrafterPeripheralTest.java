// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.peripheral;

import com.multiverse.programming.BukkitMockHelper;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Crafter;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaValue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CrafterPeripheralTest {

    private JavaPlugin plugin;
    private Location location;
    private Block block;
    private Crafter crafterBlock;
    private Inventory inventory;
    private CrafterPeripheral crafterPeripheral;

    @BeforeEach
    void setUp() {
        BukkitMockHelper.setUpMockServer();
        plugin = mock(JavaPlugin.class);
        when(plugin.isEnabled()).thenReturn(true);

        World world = mock(World.class);
        location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);

        block = mock(Block.class);
        when(location.getBlock()).thenReturn(block);

        crafterBlock = mock(Crafter.class);
        when(block.getState()).thenReturn(crafterBlock);
        when(crafterBlock.getWorld()).thenReturn(world);
        when(crafterBlock.getLocation()).thenReturn(location);

        inventory = mock(Inventory.class);
        when(crafterBlock.getInventory()).thenReturn(inventory);

        crafterPeripheral = new CrafterPeripheral(plugin, location);
    }

    @AfterEach
    void tearDown() {
        BukkitMockHelper.tearDownMockServer();
    }

    @Test
    @DisplayName("Crafter reports type as crafter")
    void testType() {
        assertEquals("crafter", crafterPeripheral.getType());
    }

    @Test
    @DisplayName("Crafter Lua methods query and set slot disabled status")
    void testSlots() {
        when(crafterBlock.isSlotDisabled(0)).thenReturn(false);
        when(crafterBlock.isSlotDisabled(1)).thenReturn(true);

        LuaValue table = crafterPeripheral.toLuaTable();

        // 1-based indexing in Lua
        assertFalse(table.get("isSlotDisabled").call(LuaValue.valueOf(1)).toboolean());
        assertTrue(table.get("isSlotDisabled").call(LuaValue.valueOf(2)).toboolean());

        // setSlotDisabled
        table.get("setSlotDisabled").call(LuaValue.valueOf(1), LuaValue.valueOf(true));
        verify(crafterBlock).setSlotDisabled(0, true);
    }

    @Test
    @DisplayName("Crafter Lua method isCrafting queries crafting ticks")
    void testIsCrafting() {
        when(crafterBlock.getCraftingTicks()).thenReturn(0).thenReturn(5);
        LuaValue table = crafterPeripheral.toLuaTable();

        assertFalse(table.get("isCrafting").call().toboolean());
        assertTrue(table.get("isCrafting").call().toboolean());
    }
}

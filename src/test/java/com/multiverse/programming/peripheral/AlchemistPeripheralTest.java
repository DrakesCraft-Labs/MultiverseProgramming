// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.peripheral;

import com.multiverse.programming.BukkitMockHelper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
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

class AlchemistPeripheralTest {

    private JavaPlugin plugin;
    private World world;
    private Location location;
    private Block block;
    private AlchemistPeripheral alchemist;

    @BeforeEach
    void setUp() {
        BukkitMockHelper.setUpMockServer();
        plugin = mock(JavaPlugin.class);
        when(plugin.isEnabled()).thenReturn(true);

        world = mock(World.class);
        location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);

        block = mock(Block.class);
        when(location.getBlock()).thenReturn(block);

        alchemist = new AlchemistPeripheral(plugin, location);
    }

    @AfterEach
    void tearDown() {
        BukkitMockHelper.tearDownMockServer();
    }

    @Test
    @DisplayName("Alchemist peripheral reports type and location")
    void testBasicProperties() {
        assertEquals("alchemist", alchemist.getType());
        assertEquals(location, alchemist.getLocation());
    }

    @Test
    @DisplayName("getRecipes returns known potion recipe types")
    void testGetRecipes() {
        LuaTable table = alchemist.toLuaTable().checktable();
        LuaValue recipes = table.get("getRecipes").call();

        assertTrue(recipes.istable());
        LuaTable recipeList = recipes.checktable();
        assertTrue(recipeList.length() >= 10);
        assertEquals("SWIFTNESS", recipeList.get(1).tojstring());
    }

    @Test
    @DisplayName("inspectStand returns fuel and brewing status")
    void testInspectStand() {
        BrewingStand stand = mock(BrewingStand.class);
        when(block.getState()).thenReturn(stand);
        when(stand.getFuelLevel()).thenReturn(15);
        when(stand.getBrewingTime()).thenReturn(200);

        BrewerInventory inv = mock(BrewerInventory.class);
        when(stand.getInventory()).thenReturn(inv);
        ItemStack fuelStack = new ItemStack(Material.BLAZE_POWDER, 3);
        when(inv.getFuel()).thenReturn(fuelStack);

        LuaTable table = alchemist.toLuaTable().checktable();
        LuaValue info = table.get("inspectStand").call();

        assertTrue(info.istable());
        LuaTable infoTable = info.checktable();
        assertEquals(15, infoTable.get("fuelLevel").toint());
        assertEquals(200, infoTable.get("brewingTime").toint());
        assertTrue(infoTable.get("hasFuelItem").toboolean());
    }

    @Test
    @DisplayName("brew without ingredients fails safely with error message")
    void testBrewMissingIngredients() {
        BrewingStand stand = mock(BrewingStand.class);
        when(block.getState()).thenReturn(stand);
        BrewerInventory inv = mock(BrewerInventory.class);
        when(stand.getInventory()).thenReturn(inv);
        when(inv.getContents()).thenReturn(new ItemStack[0]);

        LuaTable table = alchemist.toLuaTable().checktable();
        Varargs res = table.get("brew").invoke(new LuaValue[]{LuaValue.valueOf("SPEED")});

        assertFalse(res.arg(1).toboolean());
        assertTrue(res.arg(2).tojstring().contains("Missing"));
    }
}

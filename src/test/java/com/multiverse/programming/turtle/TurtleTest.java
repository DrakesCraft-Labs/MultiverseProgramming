// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.turtle;

import com.multiverse.programming.BukkitMockHelper;
import com.multiverse.programming.LuaRunner;
import com.multiverse.programming.MultiverseProgrammingPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TurtleTest {

    private JavaPlugin mockPlugin;
    private World mockWorld;
    private Location startLoc;

    @BeforeEach
    void setUp() {
        BukkitMockHelper.setUpMockServer();
        mockPlugin = mock(JavaPlugin.class);
        when(mockPlugin.isEnabled()).thenReturn(true);
        mockWorld = mock(World.class);
        startLoc = new Location(mockWorld, 10, 64, 20);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        BukkitMockHelper.tearDownMockServer();
    }

    @Test
    @DisplayName("Turtle initializes with correct properties and default values")
    void testTurtleInitialization() {
        UUID owner = UUID.randomUUID();
        Turtle turtle = new Turtle(mockPlugin, "T-001", startLoc, BlockFace.NORTH, owner);

        assertEquals("T-001", turtle.getId());
        assertEquals(owner, turtle.getOwner());
        assertEquals(startLoc, turtle.getLocation());
        assertEquals(BlockFace.NORTH, turtle.getFacing());
        assertEquals(1000, turtle.getFuel());
        assertEquals(0, turtle.getSelectedSlot());
        assertEquals(Turtle.Status.IDLE, turtle.getStatus());
    }

    @Test
    @DisplayName("Turtle turn left and right cycles facing directions")
    void testTurtleRotation() {
        Turtle turtle = new Turtle(mockPlugin, "T-001", startLoc, BlockFace.NORTH, null);

        // Turn right: NORTH -> EAST -> SOUTH -> WEST -> NORTH
        turtle.turnRight();
        assertEquals(BlockFace.EAST, turtle.getFacing());
        turtle.turnRight();
        assertEquals(BlockFace.SOUTH, turtle.getFacing());
        turtle.turnRight();
        assertEquals(BlockFace.WEST, turtle.getFacing());
        turtle.turnRight();
        assertEquals(BlockFace.NORTH, turtle.getFacing());

        // Turn left: NORTH -> WEST -> SOUTH -> EAST -> NORTH
        turtle.turnLeft();
        assertEquals(BlockFace.WEST, turtle.getFacing());
        turtle.turnLeft();
        assertEquals(BlockFace.SOUTH, turtle.getFacing());
        turtle.turnLeft();
        assertEquals(BlockFace.EAST, turtle.getFacing());
        turtle.turnLeft();
        assertEquals(BlockFace.NORTH, turtle.getFacing());
    }

    @Test
    @DisplayName("Turtle inventory and slot management")
    void testTurtleInventory() {
        Turtle turtle = new Turtle(mockPlugin, "T-001", startLoc, BlockFace.NORTH, null);

        ItemStack stone = mock(ItemStack.class);
        when(stone.getType()).thenReturn(Material.STONE);
        when(stone.getAmount()).thenReturn(32);
        turtle.setItem(0, stone);
        assertNotNull(turtle.getItem(0));
        assertEquals(Material.STONE, turtle.getItem(0).getType());
        assertEquals(32, turtle.getItem(0).getAmount());

        turtle.setSelectedSlot(5);
        assertEquals(5, turtle.getSelectedSlot());

        // Test bounds
        turtle.setSelectedSlot(20);
        assertEquals(5, turtle.getSelectedSlot()); // remains unchanged
    }

    @Test
    @DisplayName("Turtle parseMaterialFromBlockState accurately strips tags and namespaces")
    void testParseMaterial() {
        assertEquals(Material.STONE, Turtle.parseMaterialFromBlockState("minecraft:stone"));
        assertEquals(Material.OAK_PLANKS, Turtle.parseMaterialFromBlockState("minecraft:oak_planks[axis=y]"));
        assertEquals(Material.AIR, Turtle.parseMaterialFromBlockState(""));
        assertEquals(Material.AIR, Turtle.parseMaterialFromBlockState(null));
        assertEquals(Material.AIR, Turtle.parseMaterialFromBlockState("unknown:non_existent_block_xyz"));
    }

    @Test
    @DisplayName("Turtle peripheral Lua execution and bindings")
    void testTurtleLuaBindings() {
        MultiverseProgrammingPlugin mvPlugin = mock(MultiverseProgrammingPlugin.class);
        when(mvPlugin.isEnabled()).thenReturn(true);
        Turtle turtle = new Turtle(mvPlugin, "T-001", startLoc, BlockFace.NORTH, null);

        TurtlePeripheral peripheral = new TurtlePeripheral(mvPlugin, turtle);
        Globals globals = LuaRunner.sandbox();
        globals.set("turtle", peripheral.toLuaTable());

        String script = """
            turtle.turnRight()
            local id = turtle.getId()
            local fuel = turtle.getFuelLevel()
            turtle.select(4)
            local slot = turtle.getSelectedSlot()
            return id, fuel, slot
        """;

        LuaValue chunk = globals.load(script);
        var res = chunk.invoke();

        assertEquals("T-001", res.arg(1).tojstring());
        assertEquals(1000, res.arg(2).toint());
        assertEquals(4, res.arg(3).toint());
        assertEquals(BlockFace.EAST, turtle.getFacing());
        assertEquals(3, turtle.getSelectedSlot()); // 0-indexed in Java
    }
}

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
import com.multiverse.programming.blueprint.Blueprint;
import com.multiverse.programming.blueprint.Blueprint.PlacementBlock;
import org.bukkit.block.Block;
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

    @Test
    @DisplayName("startBuild refuses to build when origin coordinates are null")
    void testBuildRequiresCoordinates() {
        Turtle turtle = new Turtle(mockPlugin, "T-001", startLoc, BlockFace.NORTH, null);
        Blueprint bp = new Blueprint("TEST", "Test", "Author", "litematic", 1, 1, 1, 1,
                java.util.Map.of(), java.util.List.of(new PlacementBlock(0, 0, 0, "minecraft:stone")), System.currentTimeMillis());

        java.util.concurrent.atomic.AtomicBoolean errorCalled = new java.util.concurrent.atomic.AtomicBoolean(false);
        boolean started = turtle.startBuild(bp, null, 1, false, false, null, err -> errorCalled.set(true));

        assertFalse(started);
        assertTrue(errorCalled.get());
        assertEquals(Turtle.Status.ERROR, turtle.getStatus());
        assertTrue(turtle.getStatusMessage().contains("mandatory"));
    }

    @Test
    @DisplayName("startBuild refuses to build if target area is obstructed and clear is false")
    void testBuildRefusesWhenObstructedWithoutClear() {
        Turtle turtle = new Turtle(mockPlugin, "T-001", startLoc, BlockFace.NORTH, null);
        Blueprint bp = new Blueprint("TEST", "Test", "Author", "litematic", 1, 1, 1, 1,
                java.util.Map.of(), java.util.List.of(new PlacementBlock(1, 0, 0, "minecraft:stone")), System.currentTimeMillis());

        Location origin = new Location(mockWorld, 10, 64, 20);
        when(mockWorld.getMinHeight()).thenReturn(-64);
        when(mockWorld.getMaxHeight()).thenReturn(320);

        Block mockObstructedBlock = mock(Block.class);
        when(mockObstructedBlock.isEmpty()).thenReturn(false);
        when(mockObstructedBlock.getType()).thenReturn(Material.DIRT); // Obstructing dirt block!
        when(mockWorld.getBlockAt(11, 64, 20)).thenReturn(mockObstructedBlock);

        java.util.concurrent.atomic.AtomicBoolean errorCalled = new java.util.concurrent.atomic.AtomicBoolean(false);
        boolean started = turtle.startBuild(bp, origin, 1, false, false, null, err -> errorCalled.set(true));

        assertFalse(started);
        assertTrue(errorCalled.get());
        assertEquals(Turtle.Status.ERROR, turtle.getStatus());
        assertTrue(turtle.getStatusMessage().contains("obstructed"));
        verify(mockObstructedBlock, never()).setType(any(), anyBoolean());
    }

    @Test
    @DisplayName("startBuild destroys obstructed blocks without drops when clear is true")
    void testBuildClearsObstructedBlocksWhenClearTrue() {
        Turtle turtle = new Turtle(mockPlugin, "T-001", startLoc, BlockFace.NORTH, null);
        Blueprint bp = new Blueprint("TEST", "Test", "Author", "litematic", 1, 1, 1, 1,
                java.util.Map.of(), java.util.List.of(new PlacementBlock(1, 0, 0, "minecraft:stone")), System.currentTimeMillis());

        Location origin = new Location(mockWorld, 10, 64, 20);
        when(mockWorld.getMinHeight()).thenReturn(-64);
        when(mockWorld.getMaxHeight()).thenReturn(320);

        Block mockObstructedBlock = mock(Block.class);
        when(mockObstructedBlock.isEmpty()).thenReturn(false);
        when(mockObstructedBlock.getType()).thenReturn(Material.DIRT);
        when(mockWorld.getBlockAt(11, 64, 20)).thenReturn(mockObstructedBlock);

        boolean started = turtle.startBuild(bp, origin, 1, false, true, null, null);

        assertTrue(started);
        assertEquals(Turtle.Status.BUILDING, turtle.getStatus());
        // Verifies the block was destroyed without item drops (setType with false)
        verify(mockObstructedBlock).setType(Material.AIR, false);
    }

    @Test
    @DisplayName("Lua turtle.build requires coordinates and rejects when missing")
    void testLuaBuildRequiresCoordinates() {
        MultiverseProgrammingPlugin mvPlugin = mock(MultiverseProgrammingPlugin.class);
        when(mvPlugin.isEnabled()).thenReturn(true);
        Turtle turtle = new Turtle(mvPlugin, "T-001", startLoc, BlockFace.NORTH, null);

        com.multiverse.programming.blueprint.BlueprintManager bpManager = mock(com.multiverse.programming.blueprint.BlueprintManager.class);
        when(mvPlugin.getBlueprintManager()).thenReturn(bpManager);
        Blueprint bp = new Blueprint("CASTLE", "Castle", "Author", "litematic", 1, 1, 1, 1,
                java.util.Map.of(), java.util.List.of(), System.currentTimeMillis());
        when(bpManager.getBlueprint("CASTLE")).thenReturn(bp);

        TurtlePeripheral peripheral = new TurtlePeripheral(mvPlugin, turtle);
        Globals globals = LuaRunner.sandbox();
        globals.set("turtle", peripheral.toLuaTable());

        String script = """
            local ok, err = turtle.build("CASTLE")
            return ok, err
        """;

        LuaValue chunk = globals.load(script);
        var res = chunk.invoke();

        assertFalse(res.arg(1).toboolean());
        assertTrue(res.arg(2).tojstring().contains("mandatory"));
    }

    @Test
    @DisplayName("parseBlockData parses Minecraft block states properly")
    void testParseBlockData() {
        assertNull(Turtle.parseBlockData(null));
        assertNull(Turtle.parseBlockData(""));

        var data = Turtle.parseBlockData("minecraft:stone");
        assertNotNull(data);
        assertEquals(Material.STONE, data.getMaterial());

        var stairs = Turtle.parseBlockData("minecraft:oak_stairs[facing=south,half=bottom,shape=straight]");
        assertNotNull(stairs);
        assertEquals(Material.OAK_STAIRS, stairs.getMaterial());
    }

    @Test
    @DisplayName("getItemMaterialForBlock maps block-only materials to valid items")
    void testGetItemMaterialForBlock() {
        assertEquals(Material.TORCH, Turtle.getItemMaterialForBlock(Material.WALL_TORCH));
        assertEquals(Material.SOUL_TORCH, Turtle.getItemMaterialForBlock(Material.SOUL_WALL_TORCH));
        assertEquals(Material.REDSTONE_TORCH, Turtle.getItemMaterialForBlock(Material.REDSTONE_WALL_TORCH));
        assertEquals(Material.OAK_SIGN, Turtle.getItemMaterialForBlock(Material.OAK_WALL_SIGN));
        assertEquals(Material.OAK_HANGING_SIGN, Turtle.getItemMaterialForBlock(Material.OAK_WALL_HANGING_SIGN));
        assertEquals(Material.STONE, Turtle.getItemMaterialForBlock(Material.STONE));
    }

    @Test
    @DisplayName("isIllegalBlock detects unplaceable / admin-only blocks")
    void testIsIllegalBlock() {
        assertTrue(Turtle.isIllegalBlock(Material.BEDROCK));
        assertTrue(Turtle.isIllegalBlock(Material.BARRIER));
        assertTrue(Turtle.isIllegalBlock(Material.COMMAND_BLOCK));
        assertTrue(Turtle.isIllegalBlock(Material.STRUCTURE_BLOCK));
        assertTrue(Turtle.isIllegalBlock(Material.END_PORTAL));
        assertFalse(Turtle.isIllegalBlock(Material.STONE));
        assertFalse(Turtle.isIllegalBlock(Material.OAK_PLANKS));
    }
}

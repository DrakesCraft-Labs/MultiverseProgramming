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
    @DisplayName("Lua turtle.build computes origin relative to turtle's current position")
    void testLuaBuildUsesRelativeCoordinates() {
        MultiverseProgrammingPlugin mvPlugin = mock(MultiverseProgrammingPlugin.class);
        when(mvPlugin.isEnabled()).thenReturn(true);
        com.multiverse.programming.ConfigManager configManager = mock(com.multiverse.programming.ConfigManager.class);
        when(mvPlugin.getConfigManager()).thenReturn(configManager);
        when(configManager.getTurtleBuildDelayTicks()).thenReturn(1);
        when(configManager.isTurtleRequireMaterials()).thenReturn(false);

        when(mockWorld.getMinHeight()).thenReturn(-64);
        when(mockWorld.getMaxHeight()).thenReturn(320);

        // Turtle is at (10, 64, 20)
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
            local ok, msg = turtle.build("CASTLE", 2, -1, 5)
            return ok, msg
        """;

        LuaValue chunk = globals.load(script);
        var res = chunk.invoke();

        assertTrue(res.arg(1).toboolean());
        assertEquals(new Location(mockWorld, 12, 63, 25), turtle.getBuildOrigin());
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
        assertTrue(Turtle.isIllegalBlock(Material.NETHER_PORTAL));
        assertFalse(Turtle.isIllegalBlock(Material.STONE));
        assertFalse(Turtle.isIllegalBlock(Material.OAK_PLANKS));
    }

    @Test
    @DisplayName("startBuild rotates blueprint when rotationDegrees is specified")
    void testStartBuildRotatesBlueprint() {
        Turtle turtle = new Turtle(mockPlugin, "T-001", startLoc, BlockFace.NORTH, null);
        // Original size: 3 x 2 x 1, facing=north
        Blueprint bp = new Blueprint("ROT", "Rotatable", "Author", "litematic", 3, 2, 1, 1,
                java.util.Map.of("minecraft:oak_stairs[facing=north]", 1),
                java.util.List.of(new PlacementBlock(1, 0, 0, "minecraft:oak_stairs[facing=north]")),
                System.currentTimeMillis());

        Location origin = new Location(mockWorld, 10, 64, 20);
        when(mockWorld.getMinHeight()).thenReturn(-64);
        when(mockWorld.getMaxHeight()).thenReturn(320);

        Block mockAirBlock = mock(Block.class);
        when(mockAirBlock.isEmpty()).thenReturn(true);
        when(mockWorld.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(mockAirBlock);

        // 90 degrees rotation
        boolean started = turtle.startBuild(bp, origin, 1, false, false, 90, null, null);
        assertTrue(started);
        assertEquals(Turtle.Status.BUILDING, turtle.getStatus());

        // The active blueprint should be rotated (dimensions swapped 3x2x1 -> 1x2x3, facing north -> east)
        Blueprint active = turtle.getActiveBlueprint();
        assertNotNull(active);
        assertEquals(1, active.sizeX());
        assertEquals(2, active.sizeY());
        assertEquals(3, active.sizeZ());
        assertEquals("minecraft:oak_stairs[facing=east]", active.blocks().get(0).material());
    }

    @Test
    @DisplayName("startQuarry fails if lateral engine block is missing")
    void testQuarryRequiresAttachedEngineLateralValidation() {
        Turtle turtle = new Turtle(mockPlugin, "T-001", startLoc, BlockFace.NORTH, null);
        java.util.concurrent.atomic.AtomicBoolean errorCalled = new java.util.concurrent.atomic.AtomicBoolean(false);

        boolean started = turtle.startQuarry(5, 5, 40, true, null, err -> {
            errorCalled.set(true);
            assertTrue(err.contains("lateral side"));
        });

        assertFalse(started);
        assertTrue(errorCalled.get());
        assertEquals(Turtle.Status.ERROR, turtle.getStatus());
        assertTrue(turtle.getStatusMessage().contains("lateral side"));
    }

    @Test
    @DisplayName("Turtle accurately detects lateral Quarry Engine on left and right sides")
    void testDetectLateralQuarryEngine() {
        Turtle turtle = new Turtle(mockPlugin, "T-001", startLoc, BlockFace.NORTH, null);

        Block turtleBlock = mock(Block.class);
        Block leftBlock = mock(Block.class);
        Block rightBlock = mock(Block.class);

        when(mockWorld.getBlockAt(startLoc)).thenReturn(turtleBlock);
        when(turtleBlock.getRelative(BlockFace.WEST)).thenReturn(leftBlock);
        when(turtleBlock.getRelative(BlockFace.EAST)).thenReturn(rightBlock);

        // Neither is blast furnace
        when(leftBlock.getType()).thenReturn(Material.AIR);
        when(rightBlock.getType()).thenReturn(Material.AIR);
        assertNull(turtle.findLateralQuarrySide());
        assertFalse(turtle.hasQuarryEngineAttached());

        // Left is blast furnace (Quarry block)
        when(leftBlock.getType()).thenReturn(Material.BLAST_FURNACE);
        assertEquals(Turtle.LateralSide.LEFT, turtle.findLateralQuarrySide());
        assertTrue(turtle.hasQuarryEngineAttached());

        // Right is blast furnace
        when(leftBlock.getType()).thenReturn(Material.AIR);
        when(rightBlock.getType()).thenReturn(Material.BLAST_FURNACE);
        assertEquals(Turtle.LateralSide.RIGHT, turtle.findLateralQuarrySide());
        assertTrue(turtle.hasQuarryEngineAttached());
    }

    @Test
    @DisplayName("consumeQuarryFuel consumes fuel at 1.20x rate (+20%)")
    void testQuarryFuelConsumption20Percent() {
        Turtle turtle = new Turtle(mockPlugin, "T-001", startLoc, BlockFace.NORTH, null);
        turtle.setFuel(100);

        // Call 1: +1.20 -> deduct 1, rem 0.20 -> fuel 99
        assertTrue(turtle.consumeQuarryFuel());
        assertEquals(99, turtle.getFuel());

        // Call 2: +1.20 -> 1.40 -> deduct 1, rem 0.40 -> fuel 98
        assertTrue(turtle.consumeQuarryFuel());
        assertEquals(98, turtle.getFuel());

        // Call 3: +1.20 -> 1.60 -> deduct 1, rem 0.60 -> fuel 97
        assertTrue(turtle.consumeQuarryFuel());
        assertEquals(97, turtle.getFuel());

        // Call 4: +1.20 -> 1.80 -> deduct 1, rem 0.80 -> fuel 96
        assertTrue(turtle.consumeQuarryFuel());
        assertEquals(96, turtle.getFuel());

        // Call 5: +1.20 -> 2.00 -> deduct 2, rem 0.00 -> fuel 94
        assertTrue(turtle.consumeQuarryFuel());
        assertEquals(94, turtle.getFuel());

        // In 5 calls, exactly 6 fuel consumed (6 / 5 = 1.20x)
    }

    @Test
    @DisplayName("relocateTurtleWithEngine moves turtle and attached engine in lockstep")
    void testRelocateTurtleWithEngine() {
        Turtle turtle = new Turtle(mockPlugin, "T-001", startLoc, BlockFace.NORTH, null);
        turtle.setQuarryLateralSide(Turtle.LateralSide.LEFT);

        Block turtleBlock = mock(Block.class);
        when(mockWorld.getBlockAt(startLoc)).thenReturn(turtleBlock);

        Location newLoc = new Location(mockWorld, 10, 64, 21);
        Block newTurtleBlock = mock(Block.class);
        when(mockWorld.getBlockAt(newLoc)).thenReturn(newTurtleBlock);

        turtle.relocateTurtleWithEngine(mockWorld, newLoc, BlockFace.NORTH);
        assertEquals(newLoc, turtle.getLocation());
    }

    @Test
    @DisplayName("TurtlePeripheral Lua bindings for quarry engine operations")
    void testTurtleLuaQuarryBindings() {
        MultiverseProgrammingPlugin mvPlugin = mock(MultiverseProgrammingPlugin.class);
        when(mvPlugin.isEnabled()).thenReturn(true);
        Turtle turtle = new Turtle(mvPlugin, "T-001", startLoc, BlockFace.NORTH, null);

        TurtlePeripheral peripheral = new TurtlePeripheral(mvPlugin, turtle);
        Globals globals = LuaRunner.sandbox();
        globals.set("turtle", peripheral.toLuaTable());

        String script = """
            local hasEngine = turtle.hasQuarryEngine()
            local ok, err = turtle.quarry(5, 5, 40)
            local status = turtle.getQuarryStatus()
            turtle.pauseQuarry()
            turtle.resumeQuarry()
            turtle.stopQuarry()
            return hasEngine, ok, err, status.active, status.status
        """;

        LuaValue chunk = globals.load(script);
        var res = chunk.invoke();

        assertFalse(res.arg(1).toboolean());
        assertFalse(res.arg(2).toboolean());
        assertTrue(res.arg(3).tojstring().contains("lateral side"));
        assertFalse(res.arg(4).toboolean());
        assertEquals("ERROR", res.arg(5).tojstring());
    }

    @Test
    @DisplayName("stopAnyWork stops active tasks and resets to IDLE")
    void testStopAnyWork() {
        Turtle turtle = new Turtle(mockPlugin, "T-001", startLoc, BlockFace.NORTH, null);
        assertFalse(turtle.stopAnyWork()); // was already idle
        assertEquals(Turtle.Status.IDLE, turtle.getStatus());
    }

    @Test
    @DisplayName("getOwnerName returns None for null and UUID/name when present")
    void testGetOwnerName() {
        Turtle turtle1 = new Turtle(mockPlugin, "T-001", startLoc, BlockFace.NORTH, null);
        assertEquals("None", turtle1.getOwnerName());

        UUID uuid = UUID.randomUUID();
        Turtle turtle2 = new Turtle(mockPlugin, "T-002", startLoc, BlockFace.NORTH, uuid);
        assertNotNull(turtle2.getOwnerName());
        assertNotEquals("None", turtle2.getOwnerName());
    }

    @Test
    @DisplayName("TurtleManager creates, restores, and filters turtles by owner")
    void testTurtleManagerOwnerOperations() {
        MultiverseProgrammingPlugin mvPlugin = mock(MultiverseProgrammingPlugin.class);
        when(mvPlugin.getDataFolder()).thenReturn(null);

        TurtleManager tm = new TurtleManager(mvPlugin);
        UUID owner1 = UUID.randomUUID();
        UUID owner2 = UUID.randomUUID();

        Location loc1 = new Location(mockWorld, 10, 64, 20);
        Location loc2 = new Location(mockWorld, 15, 64, 25);

        Turtle t1 = tm.createTurtle(loc1, BlockFace.NORTH, owner1);
        Turtle t2 = tm.createTurtle(loc2, BlockFace.SOUTH, owner2);

        assertEquals(owner1, t1.getOwner());
        assertEquals(owner2, t2.getOwner());

        var owner1Turtles = tm.getTurtlesByOwner(owner1);
        assertEquals(1, owner1Turtles.size());
        assertEquals(t1.getId(), owner1Turtles.get(0).getId());

        var owner2Turtles = tm.getTurtlesByOwner(owner2);
        assertEquals(1, owner2Turtles.size());
        assertEquals(t2.getId(), owner2Turtles.get(0).getId());

        // Restore turtle with custom ID
        Location loc3 = new Location(mockWorld, 20, 64, 30);
        Turtle t3 = tm.restoreTurtle("T-099", loc3, BlockFace.EAST, owner1);
        assertEquals("T-099", t3.getId());
        assertEquals(owner1, t3.getOwner());
        assertEquals(2, tm.getTurtlesByOwner(owner1).size());
    }

    @Test
    @DisplayName("canSafelyOccupy protects redstone, crops, and delicate blocks from turtle relocation")
    void testCanSafelyOccupy() {
        assertFalse(Turtle.canSafelyOccupy(null));

        Block mockAir = mock(Block.class);
        when(mockAir.getType()).thenReturn(Material.AIR);
        when(mockAir.isEmpty()).thenReturn(true);
        assertTrue(Turtle.canSafelyOccupy(mockAir));

        Block mockRedstone = mock(Block.class);
        when(mockRedstone.getType()).thenReturn(Material.REDSTONE_WIRE);
        when(mockRedstone.isEmpty()).thenReturn(false);
        assertFalse(Turtle.canSafelyOccupy(mockRedstone));

        Block mockWheat = mock(Block.class);
        when(mockWheat.getType()).thenReturn(Material.WHEAT);
        when(mockWheat.isEmpty()).thenReturn(false);
        assertFalse(Turtle.canSafelyOccupy(mockWheat));

        Block mockCarrot = mock(Block.class);
        when(mockCarrot.getType()).thenReturn(Material.CARROTS);
        when(mockCarrot.isEmpty()).thenReturn(false);
        assertFalse(Turtle.canSafelyOccupy(mockCarrot));

        Block mockTorch = mock(Block.class);
        when(mockTorch.getType()).thenReturn(Material.TORCH);
        when(mockTorch.isEmpty()).thenReturn(false);
        assertFalse(Turtle.canSafelyOccupy(mockTorch));

        Block mockRepeater = mock(Block.class);
        when(mockRepeater.getType()).thenReturn(Material.REPEATER);
        when(mockRepeater.isEmpty()).thenReturn(false);
        assertFalse(Turtle.canSafelyOccupy(mockRepeater));
    }

    @Test
    @DisplayName("ProtectionManager checkRegionArea allows excavation in wilderness without claims")
    void testCheckRegionAreaWithoutProtectionPlugins() {
        MultiverseProgrammingPlugin mvPlugin = mock(MultiverseProgrammingPlugin.class);
        com.multiverse.programming.protection.ProtectionManager pm = new com.multiverse.programming.protection.ProtectionManager(mvPlugin);

        Location loc1 = new Location(mockWorld, 0, 10, 0);
        Location loc2 = new Location(mockWorld, 15, 60, 15);

        assertNull(pm.checkRegionArea(UUID.randomUUID(), loc1, loc2));
    }

    @Test
    @DisplayName("placeDoubleChest properly sets double chest block data and facing")
    void testPlaceDoubleChest() {
        Block b1 = mock(Block.class);
        Block b2 = mock(Block.class);
        when(b1.getX()).thenReturn(10);
        when(b1.getY()).thenReturn(64);
        when(b1.getZ()).thenReturn(20);

        when(b2.getX()).thenReturn(11);
        when(b2.getY()).thenReturn(64);
        when(b2.getZ()).thenReturn(20);

        org.bukkit.block.data.type.Chest data1 = mock(org.bukkit.block.data.type.Chest.class);
        org.bukkit.block.data.type.Chest data2 = mock(org.bukkit.block.data.type.Chest.class);
        when(b1.getBlockData()).thenReturn(data1);
        when(b2.getBlockData()).thenReturn(data2);

        Turtle.placeDoubleChest(b1, b2, BlockFace.NORTH);

        verify(b1).setType(Material.CHEST, false);
        verify(b2).setType(Material.CHEST, false);
        verify(data1).setFacing(BlockFace.NORTH);
        verify(data2).setFacing(BlockFace.NORTH);
    }

    @Test
    @DisplayName("Double chests are separated by at least 1 block distance and protected during building")
    void testSeparatedDoubleChestsAndBuildProtection() {
        MultiverseProgrammingPlugin mvp = mock(MultiverseProgrammingPlugin.class);
        when(mvp.isEnabled()).thenReturn(true);
        com.multiverse.programming.ConfigManager cfg = mock(com.multiverse.programming.ConfigManager.class);
        when(cfg.isTurtleFuelRequired()).thenReturn(true);
        when(cfg.getTurtleBlock()).thenReturn(Material.DISPENSER);
        when(mvp.getConfigManager()).thenReturn(cfg);

        when(mockWorld.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(inv -> {
            int x = inv.getArgument(0);
            int y = inv.getArgument(1);
            int z = inv.getArgument(2);
            Block b = mock(Block.class);
            when(b.getX()).thenReturn(x);
            when(b.getY()).thenReturn(y);
            when(b.getZ()).thenReturn(z);
            when(b.getWorld()).thenReturn(mockWorld);
            when(b.getLocation()).thenReturn(new Location(mockWorld, x, y, z));
            when(b.getType()).thenReturn(Material.AIR);
            when(b.isEmpty()).thenReturn(true);
            when(b.isPassable()).thenReturn(true);
            when(b.getRelative(any(BlockFace.class))).thenAnswer(inv2 -> {
                BlockFace f = inv2.getArgument(0);
                return mockWorld.getBlockAt(x + f.getModX(), y + f.getModY(), z + f.getModZ());
            });
            when(b.getRelative(any(BlockFace.class), anyInt())).thenAnswer(inv2 -> {
                BlockFace f = inv2.getArgument(0);
                int dist = inv2.getArgument(1);
                return mockWorld.getBlockAt(x + f.getModX() * dist, y + f.getModY() * dist, z + f.getModZ() * dist);
            });
            return b;
        });
        when(mockWorld.getBlockAt(any(Location.class))).thenAnswer(inv -> {
            Location l = inv.getArgument(0);
            return mockWorld.getBlockAt(l.getBlockX(), l.getBlockY(), l.getBlockZ());
        });

        Turtle turtle = new Turtle(mvp, "T-CHEST", startLoc, BlockFace.NORTH, UUID.randomUUID());

        Blueprint bp = new Blueprint(
                "house", "House", "Builder", "nbt",
                5, 5, 5, 2,
                java.util.Map.of("STONE", 2),
                java.util.List.of(
                        new PlacementBlock(0, 0, 0, "stone"),
                        new PlacementBlock(1, 0, 0, "stone")
                ),
                System.currentTimeMillis()
        );

        boolean started = turtle.startBuild(bp, startLoc, 2, true, null, null);
        assertTrue(started);

        Location mat1 = turtle.getConstructionChestLoc();
        Location mat2 = turtle.getConstructionChestLoc2();
        Location fuel1 = turtle.getFuelChestLoc();
        Location fuel2 = turtle.getFuelChestLoc2();

        assertNotNull(mat1);
        assertNotNull(mat2);
        assertNotNull(fuel1);
        assertNotNull(fuel2);

        // Verify separation: distance between material and fuel chests is >= 2 (at least 1 block gap)
        int dist1 = Math.abs(mat1.getBlockX() - fuel1.getBlockX()) + Math.abs(mat1.getBlockZ() - fuel1.getBlockZ());
        assertTrue(dist1 >= 2, "Material and fuel double chests must have at least 1 empty block between them");

        // Verify protection while building
        assertTrue(turtle.isBlockProtected(mat1).isProtected());
        assertTrue(turtle.isBlockProtected(mat2).isProtected());
        assertTrue(turtle.isBlockProtected(fuel1).isProtected());
        assertTrue(turtle.isBlockProtected(fuel2).isProtected());

        // Placed blueprint positions are protected
        assertTrue(turtle.isBlockProtected(startLoc).isProtected());
        assertTrue(turtle.isBlockProtected(startLoc.clone().add(1, 0, 0)).isProtected());

        // Random external position is not protected
        assertFalse(turtle.isBlockProtected(startLoc.clone().add(50, 0, 50)).isProtected());

        // When build is cancelled or finished, protection is lifted completely
        turtle.cancelBuild();
        assertFalse(turtle.isBuilding());
        assertFalse(turtle.isBlockProtected(mat1).isProtected());
        assertFalse(turtle.isBlockProtected(startLoc).isProtected());
    }

    @Test
    @DisplayName("Quarry area and double chests are protected during excavation and lifted upon completion")
    void testQuarryBlockProtectionAndLift() {
        MultiverseProgrammingPlugin mvp = mock(MultiverseProgrammingPlugin.class);
        when(mvp.isEnabled()).thenReturn(true);
        com.multiverse.programming.ConfigManager cfg = mock(com.multiverse.programming.ConfigManager.class);
        when(cfg.isTurtleFuelRequired()).thenReturn(true);
        when(cfg.getTurtleBlock()).thenReturn(Material.DISPENSER);
        when(cfg.getQuarryBlock()).thenReturn(Material.BLAST_FURNACE);
        when(mvp.getConfigManager()).thenReturn(cfg);

        when(mockWorld.getMinHeight()).thenReturn(-64);
        when(mockWorld.getMaxHeight()).thenReturn(320);

        when(mockWorld.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(inv -> {
            int x = inv.getArgument(0);
            int y = inv.getArgument(1);
            int z = inv.getArgument(2);
            Block b = mock(Block.class);
            when(b.getX()).thenReturn(x);
            when(b.getY()).thenReturn(y);
            when(b.getZ()).thenReturn(z);
            when(b.getWorld()).thenReturn(mockWorld);
            when(b.getLocation()).thenReturn(new Location(mockWorld, x, y, z));
            boolean isEngine = (x == 9 && y == 64 && z == 20);
            Material mat = isEngine ? Material.BLAST_FURNACE : Material.AIR;
            boolean isAir = (mat == Material.AIR);
            when(b.getType()).thenReturn(mat);
            when(b.isEmpty()).thenReturn(isAir);
            when(b.isPassable()).thenReturn(isAir);
            when(b.getRelative(any(BlockFace.class))).thenAnswer(inv2 -> {
                BlockFace f = inv2.getArgument(0);
                return mockWorld.getBlockAt(x + f.getModX(), y + f.getModY(), z + f.getModZ());
            });
            when(b.getRelative(any(BlockFace.class), anyInt())).thenAnswer(inv2 -> {
                BlockFace f = inv2.getArgument(0);
                int dist = inv2.getArgument(1);
                return mockWorld.getBlockAt(x + f.getModX() * dist, y + f.getModY() * dist, z + f.getModZ() * dist);
            });
            return b;
        });
        when(mockWorld.getBlockAt(any(Location.class))).thenAnswer(inv -> {
            Location l = inv.getArgument(0);
            return mockWorld.getBlockAt(l.getBlockX(), l.getBlockY(), l.getBlockZ());
        });

        Turtle turtle = new Turtle(mvp, "T-QUARRY", startLoc, BlockFace.NORTH, UUID.randomUUID());
        boolean started = turtle.startQuarry(3, 3, 50, true, null, null);
        assertTrue(started);

        Location storage1 = turtle.getQuarryStorageChestLoc();
        Location storage2 = turtle.getQuarryStorageChestLoc2();
        Location fuel1 = turtle.getQuarryFuelChestLoc();
        Location fuel2 = turtle.getQuarryFuelChestLoc2();

        assertNotNull(storage1);
        assertNotNull(storage2);
        assertNotNull(fuel1);
        assertNotNull(fuel2);

        // Verify separation: distance >= 2 (at least 1 block gap)
        int dist = Math.abs(storage1.getBlockX() - fuel1.getBlockX()) + Math.abs(storage1.getBlockZ() - fuel1.getBlockZ());
        assertTrue(dist >= 2, "Quarry storage and fuel double chests must have at least 1 empty block between them");

        // Verify protection while quarrying
        assertTrue(turtle.isBlockProtected(storage1).isProtected());
        assertTrue(turtle.isBlockProtected(storage2).isProtected());
        assertTrue(turtle.isBlockProtected(fuel1).isProtected());
        assertTrue(turtle.isBlockProtected(fuel2).isProtected());

        // Blocks inside excavation volume:
        // Facing NORTH (forward is -Z). length=3 -> z from 19 down to 17. width=3 (lateral is +X) -> x from 10 to 12. y from 63 down to 50.
        Location insideLoc = new Location(mockWorld, 11, 55, 18);
        assertTrue(turtle.isBlockProtected(insideLoc).isProtected());
        assertTrue(turtle.isBlockProtected(insideLoc).reason().contains("área de excavación"));

        // Blocks outside excavation volume
        Location outsideLoc = new Location(mockWorld, 11, 55, 25);
        assertFalse(turtle.isBlockProtected(outsideLoc).isProtected());

        // Cancel/finish quarry lifts protection completely
        turtle.cancelQuarry();
        assertFalse(turtle.isQuarryActive());
        assertFalse(turtle.isBlockProtected(insideLoc).isProtected());
        assertFalse(turtle.isBlockProtected(storage1).isProtected());
    }
}

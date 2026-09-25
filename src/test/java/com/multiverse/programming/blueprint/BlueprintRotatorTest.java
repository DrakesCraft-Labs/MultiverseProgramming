// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.blueprint;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BlueprintRotatorTest {

    @Test
    @DisplayName("normalizeRotation correctly parses direction names and angles")
    void testNormalizeRotation() {
        assertEquals(0, BlueprintRotator.normalizeRotation("NORTH"));
        assertEquals(0, BlueprintRotator.normalizeRotation("n"));
        assertEquals(0, BlueprintRotator.normalizeRotation("0"));
        assertEquals(90, BlueprintRotator.normalizeRotation("EAST"));
        assertEquals(90, BlueprintRotator.normalizeRotation("e"));
        assertEquals(90, BlueprintRotator.normalizeRotation("90"));
        assertEquals(180, BlueprintRotator.normalizeRotation("SOUTH"));
        assertEquals(180, BlueprintRotator.normalizeRotation("s"));
        assertEquals(180, BlueprintRotator.normalizeRotation("180"));
        assertEquals(270, BlueprintRotator.normalizeRotation("WEST"));
        assertEquals(270, BlueprintRotator.normalizeRotation("w"));
        assertEquals(270, BlueprintRotator.normalizeRotation("270"));
        assertEquals(0, BlueprintRotator.normalizeRotation("invalid"));
        assertEquals(0, BlueprintRotator.normalizeRotation(null));
    }

    @Test
    @DisplayName("rotateBlockDataString rotates facing, axis, and rotation correctly")
    void testRotateBlockData() {
        // Stairs facing north -> east -> south -> west
        String stairsNorth = "minecraft:oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]";
        assertEquals("minecraft:oak_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]",
                BlueprintRotator.rotateBlockDataString(stairsNorth, 90));
        assertEquals("minecraft:oak_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]",
                BlueprintRotator.rotateBlockDataString(stairsNorth, 180));
        assertEquals("minecraft:oak_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]",
                BlueprintRotator.rotateBlockDataString(stairsNorth, 270));

        // Log axis x -> z
        String logX = "minecraft:oak_log[axis=x]";
        assertEquals("minecraft:oak_log[axis=z]", BlueprintRotator.rotateBlockDataString(logX, 90));
        assertEquals("minecraft:oak_log[axis=x]", BlueprintRotator.rotateBlockDataString(logX, 180));
        assertEquals("minecraft:oak_log[axis=z]", BlueprintRotator.rotateBlockDataString(logX, 270));

        // Sign rotation 0 -> 4 (90 deg) -> 8 (180 deg) -> 12 (270 deg)
        String sign0 = "minecraft:oak_sign[rotation=0]";
        assertEquals("minecraft:oak_sign[rotation=4]", BlueprintRotator.rotateBlockDataString(sign0, 90));
        assertEquals("minecraft:oak_sign[rotation=8]", BlueprintRotator.rotateBlockDataString(sign0, 180));
        assertEquals("minecraft:oak_sign[rotation=12]", BlueprintRotator.rotateBlockDataString(sign0, 270));
    }

    @Test
    @DisplayName("rotate rotates coordinates and dimensions of blueprint")
    void testRotateBlueprint() {
        Blueprint.PlacementBlock b1 = new Blueprint.PlacementBlock(0, 0, 0, "minecraft:stone");
        Blueprint.PlacementBlock b2 = new Blueprint.PlacementBlock(2, 1, 0, "minecraft:oak_stairs[facing=north]");

        Blueprint bp = new Blueprint(
                "test", "Test House", "Author", "litematic",
                3, 2, 1, 2, Map.of("stone", 1, "oak_stairs", 1), List.of(b1, b2), System.currentTimeMillis()
        );

        // 90 degrees: orig sizeX=3, sizeZ=1 -> new sizeX=1, sizeZ=3
        // rx = origZ - 1 - z = 1 - 1 - 0 = 0
        // rz = x
        Blueprint r90 = bp.rotate(90);
        assertEquals(1, r90.sizeX());
        assertEquals(2, r90.sizeY());
        assertEquals(3, r90.sizeZ());
        assertEquals(0, r90.blocks().get(0).x());
        assertEquals(0, r90.blocks().get(0).z());
        assertEquals(0, r90.blocks().get(1).x());
        assertEquals(2, r90.blocks().get(1).z());
        assertEquals("minecraft:oak_stairs[facing=east]", r90.blocks().get(1).material());

        // 180 degrees: sizeX=3, sizeZ=1
        // rx = origX - 1 - x = 3 - 1 - 2 = 0
        // rz = origZ - 1 - z = 1 - 1 - 0 = 0
        Blueprint r180 = bp.rotate("SOUTH");
        assertEquals(3, r180.sizeX());
        assertEquals(1, r180.sizeZ());
        assertEquals("minecraft:oak_stairs[facing=south]", r180.blocks().get(1).material());
        assertEquals(0, r180.blocks().get(1).x());
        assertEquals(0, r180.blocks().get(1).z());
    }
}

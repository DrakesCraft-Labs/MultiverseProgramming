// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.blueprint;

import com.multiverse.programming.blueprint.nbt.NbtReader;
import com.multiverse.programming.blueprint.nbt.NbtTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class BlueprintParserTest {

    @Test
    @DisplayName("Parse Vanilla Structure NBT (.nbt)")
    void testParseVanillaStructureNbt() throws IOException {
        ByteArrayOutputStream uncompressed = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(uncompressed)) {
            out.writeByte(10); // Root Compound
            out.writeUTF("");

            // size: [2, 1, 2]
            out.writeByte(9); // List
            out.writeUTF("size");
            out.writeByte(3); // Int
            out.writeInt(3);
            out.writeInt(2); // sizeX
            out.writeInt(1); // sizeY
            out.writeInt(2); // sizeZ

            // palette: [{Name: "minecraft:stone"}, {Name: "minecraft:oak_planks"}]
            out.writeByte(9); // List
            out.writeUTF("palette");
            out.writeByte(10); // Compound
            out.writeInt(2);

            // Palette 0: stone
            out.writeByte(8); // String
            out.writeUTF("Name");
            out.writeUTF("minecraft:stone");
            out.writeByte(0); // End palette 0

            // Palette 1: oak_planks
            out.writeByte(8); // String
            out.writeUTF("Name");
            out.writeUTF("minecraft:oak_planks");
            out.writeByte(0); // End palette 1

            // blocks: [{pos: [0, 0, 0], state: 0}, {pos: [1, 0, 1], state: 1}]
            out.writeByte(9); // List
            out.writeUTF("blocks");
            out.writeByte(10); // Compound
            out.writeInt(2);

            // Block 0
            out.writeByte(9); // pos
            out.writeUTF("pos");
            out.writeByte(3);
            out.writeInt(3);
            out.writeInt(0);
            out.writeInt(0);
            out.writeInt(0);
            out.writeByte(3); // state
            out.writeUTF("state");
            out.writeInt(0);
            out.writeByte(0); // end block 0

            // Block 1
            out.writeByte(9); // pos
            out.writeUTF("pos");
            out.writeByte(3);
            out.writeInt(3);
            out.writeInt(1);
            out.writeInt(0);
            out.writeInt(1);
            out.writeByte(3); // state
            out.writeUTF("state");
            out.writeInt(1);
            out.writeByte(0); // end block 1

            out.writeByte(0); // End Root
        }

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write(uncompressed.toByteArray());
        }

        NbtTag.CompoundTag root = NbtReader.read(new ByteArrayInputStream(compressed.toByteArray()));
        Blueprint bp = BlueprintParser.parseCompound("BP-001", "test_house.nbt", root);

        assertNotNull(bp);
        assertEquals("test_house", bp.name());
        assertEquals("nbt", bp.format());
        assertEquals(2, bp.sizeX());
        assertEquals(1, bp.sizeY());
        assertEquals(2, bp.sizeZ());
        assertEquals(2, bp.totalBlocks());
        assertEquals(1, bp.materialCounts().get("STONE"));
        assertEquals(1, bp.materialCounts().get("OAK_PLANKS"));
    }

    @Test
    @DisplayName("Parse Litematica Format (.litematic)")
    void testParseLitematica() throws IOException {
        ByteArrayOutputStream uncompressed = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(uncompressed)) {
            out.writeByte(10); // Root Compound
            out.writeUTF("");

            // Metadata
            out.writeByte(10);
            out.writeUTF("Metadata");
            out.writeByte(8);
            out.writeUTF("Name");
            out.writeUTF("Castle Gate");
            out.writeByte(8);
            out.writeUTF("Author");
            out.writeUTF("BuilderDan");
            out.writeByte(10); // EnclosingSize
            out.writeUTF("EnclosingSize");
            out.writeByte(3);
            out.writeUTF("x");
            out.writeInt(2);
            out.writeByte(3);
            out.writeUTF("y");
            out.writeInt(1);
            out.writeByte(3);
            out.writeUTF("z");
            out.writeInt(1);
            out.writeByte(0); // end EnclosingSize
            out.writeByte(0); // end Metadata

            // Regions
            out.writeByte(10);
            out.writeUTF("Regions");
            out.writeByte(10); // MainRegion
            out.writeUTF("MainRegion");

            // Size: {x: 2, y: 1, z: 1}
            out.writeByte(10);
            out.writeUTF("Size");
            out.writeByte(3);
            out.writeUTF("x");
            out.writeInt(2);
            out.writeByte(3);
            out.writeUTF("y");
            out.writeInt(1);
            out.writeByte(3);
            out.writeUTF("z");
            out.writeInt(1);
            out.writeByte(0); // end Size

            // BlockStatePalette: [{Name: "minecraft:air"}, {Name: "minecraft:diamond_block"}]
            out.writeByte(9);
            out.writeUTF("BlockStatePalette");
            out.writeByte(10);
            out.writeInt(2);

            out.writeByte(8);
            out.writeUTF("Name");
            out.writeUTF("minecraft:air");
            out.writeByte(0);

            out.writeByte(8);
            out.writeUTF("Name");
            out.writeUTF("minecraft:diamond_block");
            out.writeByte(0);

            // BlockStates: long array encoding palette indices
            out.writeByte(12);
            out.writeUTF("BlockStates");
            out.writeInt(1);
            // bitsPerEntry = max(2, ceil(log2(2))) = 2 bits per entry
            // Entry 0 (x=0, z=0, y=0) = 1 (diamond_block) -> bits 0..1: 01
            // Entry 1 (x=1, z=0, y=0) = 0 (air) -> bits 2..3: 00
            out.writeLong(0b0001L);

            out.writeByte(0); // end MainRegion
            out.writeByte(0); // end Regions
            out.writeByte(0); // end Root
        }

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write(uncompressed.toByteArray());
        }

        NbtTag.CompoundTag root = NbtReader.read(new ByteArrayInputStream(compressed.toByteArray()));
        Blueprint bp = BlueprintParser.parseCompound("BP-002", "castle_gate.litematic", root);

        assertNotNull(bp);
        assertEquals("Castle Gate", bp.name());
        assertEquals("BuilderDan", bp.author());
        assertEquals("litematic", bp.format());
        assertEquals(2, bp.sizeX());
        assertEquals(1, bp.sizeY());
        assertEquals(1, bp.sizeZ());
        assertEquals(1, bp.totalBlocks()); // air is skipped, only diamond_block
        assertEquals(1, bp.materialCounts().get("DIAMOND_BLOCK"));
    }

    @Test
    @DisplayName("Parse Real User Litematic File if Present")
    void testRealUserLitematic() throws IOException {
        java.io.File file = new java.io.File("C:\\Users\\danie\\Downloads\\nether-portal-g-jnftbzaq.litematic");
        if (!file.exists()) return;
        Blueprint bp = BlueprintParser.parse("BP-REAL", file);
        assertNotNull(bp);
        System.out.println("Parsed Real Blueprint: " + bp.name() + " (" + bp.sizeX() + "x" + bp.sizeY() + "x" + bp.sizeZ() + "), total non-air blocks: " + bp.totalBlocks());
        System.out.println("Materials: " + bp.materialCounts());
        assertTrue(bp.totalBlocks() > 0);
        assertEquals("nether-portal-g-jnftbzaq", bp.name());
    }

    @Test
    @DisplayName("Verify clean name resolution for dummy names like 'aaaaa'")
    void testResolveCleanName() {
        assertEquals("nether-portal-g-jnftbzaq", BlueprintParser.resolveCleanName("aaaaa", "nether-portal-g-jnftbzaq.litematic"));
        assertEquals("nether-portal-g-jnftbzaq", BlueprintParser.resolveCleanName("test", "nether-portal-g-jnftbzaq.litematic"));
        assertEquals("nether-portal-g-jnftbzaq", BlueprintParser.resolveCleanName("1111", "nether-portal-g-jnftbzaq.litematic"));
        assertEquals("Monumental Nether Portal", BlueprintParser.resolveCleanName("Monumental Nether Portal", "nether_portal.litematic"));
        assertEquals("castle_gate", BlueprintParser.resolveCleanName(null, "castle_gate.nbt"));
    }

    @Test
    @DisplayName("Verify block placement priority ordering")
    void testPlacementPriority() {
        assertEquals(0, BlueprintParser.getPlacementPriority("minecraft:stone"));
        assertEquals(0, BlueprintParser.getPlacementPriority("minecraft:oak_planks"));
        assertEquals(1, BlueprintParser.getPlacementPriority("minecraft:oak_stairs[facing=south,half=bottom]"));
        assertEquals(1, BlueprintParser.getPlacementPriority("minecraft:stone_slab[type=bottom]"));
        assertEquals(2, BlueprintParser.getPlacementPriority("minecraft:oak_door[half=lower,facing=north]"));
        assertEquals(3, BlueprintParser.getPlacementPriority("minecraft:oak_door[half=upper,facing=north]"));
        assertEquals(4, BlueprintParser.getPlacementPriority("minecraft:wall_torch[facing=south]"));
        assertEquals(4, BlueprintParser.getPlacementPriority("minecraft:sea_pickle[pickles=3]"));
        assertEquals(4, BlueprintParser.getPlacementPriority("minecraft:ladder[facing=north]"));
        assertEquals(5, BlueprintParser.getPlacementPriority("minecraft:water[level=0]"));
    }

    @Test
    @DisplayName("Verify item name resolution for wall variants and delicate blocks")
    void testResolveItemName() {
        assertEquals("TORCH", BlueprintParser.resolveItemName("WALL_TORCH"));
        assertEquals("SOUL_TORCH", BlueprintParser.resolveItemName("SOUL_WALL_TORCH"));
        assertEquals("REDSTONE_TORCH", BlueprintParser.resolveItemName("REDSTONE_WALL_TORCH"));
        assertEquals("OAK_SIGN", BlueprintParser.resolveItemName("OAK_WALL_SIGN"));
        assertEquals("OAK_HANGING_SIGN", BlueprintParser.resolveItemName("OAK_WALL_HANGING_SIGN"));
        assertEquals("FLOWER_POT", BlueprintParser.resolveItemName("POTTED_POPPY"));
        assertEquals("STONE", BlueprintParser.resolveItemName("STONE"));
    }

    @Test
    @DisplayName("Verify dangerous block filtering matches namespaces and properties")
    void testDangerousBlockDetection() {
        assertTrue(BlueprintSecurityValidator.isDangerousBlock("BEDROCK"));
        assertTrue(BlueprintSecurityValidator.isDangerousBlock("minecraft:bedrock"));
        assertTrue(BlueprintSecurityValidator.isDangerousBlock("minecraft:bedrock[some_prop=val]"));
        assertTrue(BlueprintSecurityValidator.isDangerousBlock("minecraft:command_block"));
        assertTrue(BlueprintSecurityValidator.isDangerousBlock("minecraft:barrier"));
        assertTrue(BlueprintSecurityValidator.isDangerousBlock("minecraft:structure_block"));
        assertFalse(BlueprintSecurityValidator.isDangerousBlock("minecraft:stone"));
        assertFalse(BlueprintSecurityValidator.isDangerousBlock("minecraft:oak_planks"));
    }
}

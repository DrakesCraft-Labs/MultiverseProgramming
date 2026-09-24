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
}

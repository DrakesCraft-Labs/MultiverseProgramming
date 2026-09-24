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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class NbtReaderTest {

    @Test
    @DisplayName("Read uncompressed NBT compound with all tag types")
    void testReadUncompressedCompound() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            // Root Compound Tag (ID 10)
            out.writeByte(10);
            out.writeUTF("root");

            // 1. Byte Tag
            out.writeByte(1);
            out.writeUTF("testByte");
            out.writeByte(42);

            // 2. Short Tag
            out.writeByte(2);
            out.writeUTF("testShort");
            out.writeShort(1337);

            // 3. Int Tag
            out.writeByte(3);
            out.writeUTF("testInt");
            out.writeInt(123456);

            // 4. Long Tag
            out.writeByte(4);
            out.writeUTF("testLong");
            out.writeLong(9876543210L);

            // 5. Float Tag
            out.writeByte(5);
            out.writeUTF("testFloat");
            out.writeFloat(3.1415f);

            // 6. Double Tag
            out.writeByte(6);
            out.writeUTF("testDouble");
            out.writeDouble(2.71828);

            // 7. ByteArray Tag
            out.writeByte(7);
            out.writeUTF("testByteArray");
            out.writeInt(3);
            out.write(new byte[]{1, 2, 3});

            // 8. String Tag
            out.writeByte(8);
            out.writeUTF("testString");
            out.writeUTF("Hello Minecraft");

            // 9. List of Strings (type 8)
            out.writeByte(9);
            out.writeUTF("testList");
            out.writeByte(8); // Element type String
            out.writeInt(2); // 2 elements
            out.writeUTF("Elem1");
            out.writeUTF("Elem2");

            // 10. Nested Compound
            out.writeByte(10);
            out.writeUTF("nestedCompound");
            out.writeByte(8);
            out.writeUTF("innerMsg");
            out.writeUTF("success");
            out.writeByte(0); // End inner

            // 11. IntArray Tag
            out.writeByte(11);
            out.writeUTF("testIntArray");
            out.writeInt(2);
            out.writeInt(100);
            out.writeInt(200);

            // 12. LongArray Tag
            out.writeByte(12);
            out.writeUTF("testLongArray");
            out.writeInt(2);
            out.writeLong(1000L);
            out.writeLong(2000L);

            // End Root
            out.writeByte(0);
        }

        NbtTag.CompoundTag root = NbtReader.read(new ByteArrayInputStream(baos.toByteArray()));
        assertNotNull(root);

        assertEquals((byte) 42, root.getByte("testByte"));
        assertEquals((short) 1337, root.getShort("testShort"));
        assertEquals(123456, root.getInt("testInt"));
        assertEquals(9876543210L, root.getLong("testLong"));
        assertEquals(3.1415f, root.getFloat("testFloat"), 0.0001f);
        assertEquals(2.71828, root.getDouble("testDouble"), 0.0001);
        assertEquals("Hello Minecraft", root.getString("testString"));

        assertArrayEquals(new byte[]{1, 2, 3}, root.getByteArray("testByteArray"));
        assertArrayEquals(new int[]{100, 200}, root.getIntArray("testIntArray"));
        assertArrayEquals(new long[]{1000L, 2000L}, root.getLongArray("testLongArray"));

        NbtTag.ListTag list = root.getList("testList");
        assertNotNull(list);
        assertEquals(2, list.value().size());
        assertEquals("Elem1", ((NbtTag.StringTag) list.value().get(0)).value());
        assertEquals("Elem2", ((NbtTag.StringTag) list.value().get(1)).value());

        NbtTag.CompoundTag nested = root.getCompound("nestedCompound");
        assertNotNull(nested);
        assertEquals("success", nested.getString("innerMsg"));
    }

    @Test
    @DisplayName("Read GZIP compressed NBT stream automatically")
    void testReadGzipStream() throws IOException {
        ByteArrayOutputStream uncompressed = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(uncompressed)) {
            out.writeByte(10); // Compound
            out.writeUTF("compressedRoot");
            out.writeByte(8); // String
            out.writeUTF("status");
            out.writeUTF("gzipWorks");
            out.writeByte(0); // End
        }

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write(uncompressed.toByteArray());
        }

        NbtTag.CompoundTag root = NbtReader.read(new ByteArrayInputStream(compressed.toByteArray()));
        assertNotNull(root);
        assertEquals("gzipWorks", root.getString("status"));
    }
}

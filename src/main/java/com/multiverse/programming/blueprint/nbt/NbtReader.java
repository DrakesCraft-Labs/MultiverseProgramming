// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.blueprint.nbt;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Fast, pure-Java deserializer for NBT files (GZIP-compressed or uncompressed).
 * Hardened with decompression bomb and memory exhaustion mitigations.
 */
public final class NbtReader {

    public static final long MAX_UNCOMPRESSED_BYTES = 64L * 1024 * 1024; // 64 MB maximum decompressed stream
    public static final int MAX_ARRAY_LENGTH = 16_000_000; // 16 MB maximum per single tag array

    private NbtReader() {
    }

    public static NbtTag.CompoundTag read(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            return read(in);
        }
    }

    public static NbtTag.CompoundTag read(byte[] bytes) throws IOException {
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            return read(in);
        }
    }

    public static NbtTag.CompoundTag read(InputStream inputStream) throws IOException {
        byte[] magic = new byte[2];
        BufferedInputStream buffered = new BufferedInputStream(inputStream);
        buffered.mark(2);
        int read = buffered.read(magic);
        buffered.reset();

        InputStream streamToUse = buffered;
        // GZIP magic number: 0x1F, 0x8B
        if (read == 2 && (magic[0] == (byte) 0x1F) && (magic[1] == (byte) 0x8B)) {
            streamToUse = new GZIPInputStream(buffered);
        }

        // Wrap stream in a size-limiting filter to prevent zip-bombs / memory exhaustion
        BoundedInputStream boundedStream = new BoundedInputStream(streamToUse, MAX_UNCOMPRESSED_BYTES);
        DataInputStream dis = new DataInputStream(boundedStream);

        byte rootType = dis.readByte();
        if (rootType == 0) {
            return new NbtTag.CompoundTag();
        }
        if (rootType != 10) {
            throw new IOException("Expected root CompoundTag (id 10), but found: " + rootType);
        }

        // Read root tag name
        readString(dis);
        return (NbtTag.CompoundTag) readTagPayload(dis, (byte) 10);
    }

    private static NbtTag readTag(DataInputStream dis) throws IOException {
        byte id = dis.readByte();
        if (id == 0) {
            return new NbtTag.End();
        }
        return readTagPayload(dis, id);
    }

    private static NbtTag readTagPayload(DataInputStream dis, byte id) throws IOException {
        return switch (id) {
            case 0 -> new NbtTag.End();
            case 1 -> new NbtTag.ByteTag(dis.readByte());
            case 2 -> new NbtTag.ShortTag(dis.readShort());
            case 3 -> new NbtTag.IntTag(dis.readInt());
            case 4 -> new NbtTag.LongTag(dis.readLong());
            case 5 -> new NbtTag.FloatTag(dis.readFloat());
            case 6 -> new NbtTag.DoubleTag(dis.readDouble());
            case 7 -> {
                int len = dis.readInt();
                validateArrayLength(len);
                byte[] bytes = new byte[len];
                dis.readFully(bytes);
                yield new NbtTag.ByteArrayTag(bytes);
            }
            case 8 -> new NbtTag.StringTag(readString(dis));
            case 9 -> {
                byte elemType = dis.readByte();
                int len = dis.readInt();
                validateArrayLength(len);
                // A TAG_End element type consumes no bytes per entry, so a large declared length
                // would allocate millions of objects without tripping the byte-size guard. A list
                // of End tags is only valid when empty.
                if (elemType == 0 && len > 0) {
                    throw new IOException("Invalid NBT list: TAG_End element type with non-zero length (" + len + ")");
                }
                List<NbtTag> list = new ArrayList<>(Math.min(len, 100_000));
                for (int i = 0; i < len; i++) {
                    list.add(readTagPayload(dis, elemType));
                }
                yield new NbtTag.ListTag(elemType, list);
            }
            case 10 -> {
                Map<String, NbtTag> map = new HashMap<>();
                while (true) {
                    byte tagId = dis.readByte();
                    if (tagId == 0) {
                        break;
                    }
                    String key = readString(dis);
                    NbtTag val = readTagPayload(dis, tagId);
                    map.put(key, val);
                }
                yield new NbtTag.CompoundTag(map);
            }
            case 11 -> {
                int len = dis.readInt();
                validateArrayLength(len);
                int[] ints = new int[len];
                for (int i = 0; i < len; i++) {
                    ints[i] = dis.readInt();
                }
                yield new NbtTag.IntArrayTag(ints);
            }
            case 12 -> {
                int len = dis.readInt();
                validateArrayLength(len);
                long[] longs = new long[len];
                for (int i = 0; i < len; i++) {
                    longs[i] = dis.readLong();
                }
                yield new NbtTag.LongArrayTag(longs);
            }
            default -> throw new IOException("Unknown NBT Tag ID: " + id);
        };
    }

    private static void validateArrayLength(int len) throws IOException {
        if (len < 0) {
            throw new IOException("Negative NBT array length: " + len);
        }
        if (len > MAX_ARRAY_LENGTH) {
            throw new IOException("NBT array length exceeds maximum safety limit (" + len + " > " + MAX_ARRAY_LENGTH + ")");
        }
    }

    private static String readString(DataInputStream dis) throws IOException {
        int len = dis.readUnsignedShort();
        byte[] bytes = new byte[len];
        dis.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Guard stream that counts decompressed bytes and aborts if it exceeds threshold.
     */
    private static final class BoundedInputStream extends FilterInputStream {
        private final long maxBytes;
        private long bytesRead = 0;

        BoundedInputStream(InputStream in, long maxBytes) {
            super(in);
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b != -1) {
                checkLimit(1);
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = super.read(b, off, len);
            if (read > 0) {
                checkLimit(read);
            }
            return read;
        }

        private void checkLimit(int count) throws IOException {
            bytesRead += count;
            if (bytesRead > maxBytes) {
                throw new IOException(String.format(
                        "Decompressed NBT stream exceeded maximum safety limit of %d MB (potential zip-bomb attack)",
                        maxBytes / (1024 * 1024)
                ));
            }
        }
    }
}

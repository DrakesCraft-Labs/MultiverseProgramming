// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.blueprint.nbt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight, zero-dependency representation of Minecraft NBT tags.
 * Supports all tags used in vanilla .nbt and Litematica .litematic files.
 */
public sealed interface NbtTag permits
        NbtTag.End,
        NbtTag.ByteTag,
        NbtTag.ShortTag,
        NbtTag.IntTag,
        NbtTag.LongTag,
        NbtTag.FloatTag,
        NbtTag.DoubleTag,
        NbtTag.ByteArrayTag,
        NbtTag.StringTag,
        NbtTag.ListTag,
        NbtTag.CompoundTag,
        NbtTag.IntArrayTag,
        NbtTag.LongArrayTag {

    byte getId();

    record End() implements NbtTag {
        @Override
        public byte getId() {
            return 0;
        }
    }

    record ByteTag(byte value) implements NbtTag {
        @Override
        public byte getId() {
            return 1;
        }
    }

    record ShortTag(short value) implements NbtTag {
        @Override
        public byte getId() {
            return 2;
        }
    }

    record IntTag(int value) implements NbtTag {
        @Override
        public byte getId() {
            return 3;
        }
    }

    record LongTag(long value) implements NbtTag {
        @Override
        public byte getId() {
            return 4;
        }
    }

    record FloatTag(float value) implements NbtTag {
        @Override
        public byte getId() {
            return 5;
        }
    }

    record DoubleTag(double value) implements NbtTag {
        @Override
        public byte getId() {
            return 6;
        }
    }

    record ByteArrayTag(byte[] value) implements NbtTag {
        @Override
        public byte getId() {
            return 7;
        }
    }

    record StringTag(String value) implements NbtTag {
        @Override
        public byte getId() {
            return 8;
        }
    }

    record ListTag(byte tagType, List<NbtTag> value) implements NbtTag {
        public ListTag(byte tagType) {
            this(tagType, new ArrayList<>());
        }

        @Override
        public byte getId() {
            return 9;
        }
    }

    record CompoundTag(Map<String, NbtTag> value) implements NbtTag {
        public CompoundTag() {
            this(new HashMap<>());
        }

        @Override
        public byte getId() {
            return 10;
        }

        public boolean contains(String key) {
            return value.containsKey(key);
        }

        public NbtTag get(String key) {
            return value.get(key);
        }

        public CompoundTag getCompound(String key) {
            NbtTag tag = value.get(key);
            return tag instanceof CompoundTag c ? c : null;
        }

        public ListTag getList(String key) {
            NbtTag tag = value.get(key);
            return tag instanceof ListTag l ? l : null;
        }

        public String getString(String key, String def) {
            NbtTag tag = value.get(key);
            return tag instanceof StringTag s ? s.value() : def;
        }

        public String getString(String key) {
            return getString(key, "");
        }

        public byte getByte(String key, byte def) {
            NbtTag tag = value.get(key);
            return tag instanceof ByteTag b ? b.value() : def;
        }

        public byte getByte(String key) {
            return getByte(key, (byte) 0);
        }

        public short getShort(String key, short def) {
            NbtTag tag = value.get(key);
            return tag instanceof ShortTag s ? s.value() : def;
        }

        public short getShort(String key) {
            return getShort(key, (short) 0);
        }

        public int getInt(String key, int def) {
            NbtTag tag = value.get(key);
            if (tag instanceof IntTag i) return i.value();
            if (tag instanceof ShortTag s) return s.value();
            if (tag instanceof ByteTag b) return b.value();
            return def;
        }

        public int getInt(String key) {
            return getInt(key, 0);
        }

        public long getLong(String key, long def) {
            NbtTag tag = value.get(key);
            if (tag instanceof LongTag l) return l.value();
            if (tag instanceof IntTag i) return i.value();
            return def;
        }

        public long getLong(String key) {
            return getLong(key, 0L);
        }

        public float getFloat(String key, float def) {
            NbtTag tag = value.get(key);
            return tag instanceof FloatTag f ? f.value() : def;
        }

        public float getFloat(String key) {
            return getFloat(key, 0.0f);
        }

        public double getDouble(String key, double def) {
            NbtTag tag = value.get(key);
            return tag instanceof DoubleTag d ? d.value() : def;
        }

        public double getDouble(String key) {
            return getDouble(key, 0.0);
        }

        public byte[] getByteArray(String key) {
            NbtTag tag = value.get(key);
            return tag instanceof ByteArrayTag ba ? ba.value() : null;
        }

        public long[] getLongArray(String key) {
            NbtTag tag = value.get(key);
            return tag instanceof LongArrayTag la ? la.value() : null;
        }

        public int[] getIntArray(String key) {
            NbtTag tag = value.get(key);
            return tag instanceof IntArrayTag ia ? ia.value() : null;
        }
    }

    record IntArrayTag(int[] value) implements NbtTag {
        @Override
        public byte getId() {
            return 11;
        }
    }

    record LongArrayTag(long[] value) implements NbtTag {
        @Override
        public byte getId() {
            return 12;
        }
    }
}

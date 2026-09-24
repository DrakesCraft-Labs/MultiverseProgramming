// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.blueprint;

import com.multiverse.programming.blueprint.nbt.NbtReader;
import com.multiverse.programming.blueprint.nbt.NbtTag;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parser capable of reading both Litematica (.litematic) and Vanilla Structure (.nbt) files.
 */
public final class BlueprintParser {

    private BlueprintParser() {
    }

    public static Blueprint parse(String id, String fileName, InputStream in) throws IOException {
        NbtTag.CompoundTag root = NbtReader.read(in);
        return parseCompound(id, fileName, root);
    }

    public static Blueprint parse(String id, File file) throws IOException {
        NbtTag.CompoundTag root = NbtReader.read(file);
        return parseCompound(id, file.getName(), root);
    }

    public static Blueprint parseCompound(String id, String fileName, NbtTag.CompoundTag root) throws IOException {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".litematic") || root.contains("Metadata") || root.contains("Regions")) {
            return parseLitematic(id, fileName, root);
        } else {
            return parseVanillaStructure(id, fileName, root);
        }
    }

    private static Blueprint parseVanillaStructure(String id, String fileName, NbtTag.CompoundTag root) throws IOException {
        String name = fileName.replaceFirst("\\.(?i)nbt$", "");
        String author = root.getString("author", "Unknown");

        int sizeX = 1, sizeY = 1, sizeZ = 1;
        NbtTag.ListTag sizeList = root.getList("size");
        if (sizeList != null && sizeList.value().size() >= 3) {
            sizeX = Math.max(1, ((NbtTag.IntTag) sizeList.value().get(0)).value());
            sizeY = Math.max(1, ((NbtTag.IntTag) sizeList.value().get(1)).value());
            sizeZ = Math.max(1, ((NbtTag.IntTag) sizeList.value().get(2)).value());
        }

        List<String> palette = new ArrayList<>();
        NbtTag.ListTag paletteList = root.getList("palette");
        if (paletteList != null) {
            for (NbtTag tag : paletteList.value()) {
                if (tag instanceof NbtTag.CompoundTag comp) {
                    palette.add(cleanMaterial(comp.getString("Name", "minecraft:air")));
                }
            }
        }

        List<Blueprint.PlacementBlock> blocks = new ArrayList<>();
        Map<String, Integer> materialCounts = new LinkedHashMap<>();

        NbtTag.ListTag blocksList = root.getList("blocks");
        if (blocksList != null) {
            for (NbtTag tag : blocksList.value()) {
                if (tag instanceof NbtTag.CompoundTag comp) {
                    int state = comp.getInt("state", 0);
                    String mat = (state >= 0 && state < palette.size()) ? palette.get(state) : "AIR";
                    if ("AIR".equals(mat) || "CAVE_AIR".equals(mat) || "VOID_AIR".equals(mat)) {
                        continue;
                    }

                    NbtTag.ListTag posList = comp.getList("pos");
                    if (posList != null && posList.value().size() >= 3) {
                        int x = ((NbtTag.IntTag) posList.value().get(0)).value();
                        int y = ((NbtTag.IntTag) posList.value().get(1)).value();
                        int z = ((NbtTag.IntTag) posList.value().get(2)).value();

                        blocks.add(new Blueprint.PlacementBlock(x, y, z, mat));
                        materialCounts.put(mat, materialCounts.getOrDefault(mat, 0) + 1);
                    }
                }
            }
        }

        // Sort blocks layer by layer (bottom to top: y, then z, then x)
        blocks.sort((a, b) -> {
            if (a.y() != b.y()) return Integer.compare(a.y(), b.y());
            if (a.z() != b.z()) return Integer.compare(a.z(), b.z());
            return Integer.compare(a.x(), b.x());
        });

        return new Blueprint(
                id,
                name,
                author,
                "nbt",
                sizeX,
                sizeY,
                sizeZ,
                blocks.size(),
                materialCounts,
                blocks,
                System.currentTimeMillis()
        );
    }

    private static Blueprint parseLitematic(String id, String fileName, NbtTag.CompoundTag root) throws IOException {
        NbtTag.CompoundTag metadata = root.getCompound("Metadata");
        String name = metadata != null ? metadata.getString("Name", fileName.replaceFirst("\\.(?i)litematic$", "")) : fileName;
        String author = metadata != null ? metadata.getString("Author", "Unknown") : "Unknown";

        int sizeX = 1, sizeY = 1, sizeZ = 1;
        if (metadata != null) {
            NbtTag.CompoundTag encSize = metadata.getCompound("EnclosingSize");
            if (encSize != null) {
                sizeX = Math.abs(encSize.getInt("x", 1));
                sizeY = Math.abs(encSize.getInt("y", 1));
                sizeZ = Math.abs(encSize.getInt("z", 1));
            }
        }

        List<Blueprint.PlacementBlock> blocks = new ArrayList<>();
        Map<String, Integer> materialCounts = new LinkedHashMap<>();

        NbtTag.CompoundTag regions = root.getCompound("Regions");
        if (regions != null) {
            for (Map.Entry<String, NbtTag> entry : regions.value().entrySet()) {
                if (!(entry.getValue() instanceof NbtTag.CompoundTag region)) {
                    continue;
                }

                NbtTag.CompoundTag rSize = region.getCompound("Size");
                int rx = rSize != null ? Math.abs(rSize.getInt("x", 1)) : 1;
                int ry = rSize != null ? Math.abs(rSize.getInt("y", 1)) : 1;
                int rz = rSize != null ? Math.abs(rSize.getInt("z", 1)) : 1;

                List<String> palette = new ArrayList<>();
                NbtTag.ListTag paletteTag = region.getList("BlockStatePalette");
                if (paletteTag != null) {
                    for (NbtTag pt : paletteTag.value()) {
                        if (pt instanceof NbtTag.CompoundTag comp) {
                            palette.add(cleanMaterial(comp.getString("Name", "minecraft:air")));
                        }
                    }
                }

                long[] blockStates = region.getLongArray("BlockStates");
                if (palette.isEmpty()) {
                    continue;
                }

                if (palette.size() == 1 || blockStates == null || blockStates.length == 0) {
                    String singleMat = palette.get(0);
                    if (!isAir(singleMat)) {
                        for (int y = 0; y < ry; y++) {
                            for (int z = 0; z < rz; z++) {
                                for (int x = 0; x < rx; x++) {
                                    blocks.add(new Blueprint.PlacementBlock(x, y, z, singleMat));
                                    materialCounts.put(singleMat, materialCounts.getOrDefault(singleMat, 0) + 1);
                                }
                            }
                        }
                    }
                    continue;
                }

                // Bit-unpacking of Litematica BlockStates array
                int bitsPerEntry = Math.max(2, (int) Math.ceil(Math.log(palette.size()) / Math.log(2)));
                long mask = (1L << bitsPerEntry) - 1L;
                int totalBlocksInRegion = rx * ry * rz;

                for (int i = 0; i < totalBlocksInRegion; i++) {
                    long bitIndex = (long) i * bitsPerEntry;
                    int startLong = (int) (bitIndex / 64);
                    int startBit = (int) (bitIndex % 64);

                    if (startLong >= blockStates.length) {
                        break;
                    }

                    long value = blockStates[startLong] >>> startBit;
                    if (startBit + bitsPerEntry > 64 && startLong + 1 < blockStates.length) {
                        value |= blockStates[startLong + 1] << (64 - startBit);
                    }
                    int paletteIdx = (int) (value & mask);

                    if (paletteIdx >= 0 && paletteIdx < palette.size()) {
                        String mat = palette.get(paletteIdx);
                        if (!isAir(mat)) {
                            int y = (i / (rx * rz)) % ry;
                            int z = (i / rx) % rz;
                            int x = i % rx;

                            blocks.add(new Blueprint.PlacementBlock(x, y, z, mat));
                            materialCounts.put(mat, materialCounts.getOrDefault(mat, 0) + 1);
                        }
                    }
                }
            }
        }

        // Sort blocks bottom to top for structural placement
        blocks.sort((a, b) -> {
            if (a.y() != b.y()) return Integer.compare(a.y(), b.y());
            if (a.z() != b.z()) return Integer.compare(a.z(), b.z());
            return Integer.compare(a.x(), b.x());
        });

        return new Blueprint(
                id,
                name,
                author,
                "litematic",
                sizeX,
                sizeY,
                sizeZ,
                blocks.size(),
                materialCounts,
                blocks,
                System.currentTimeMillis()
        );
    }

    private static String cleanMaterial(String name) {
        if (name == null) {
            return "AIR";
        }
        if (name.startsWith("minecraft:")) {
            name = name.substring("minecraft:".length());
        }
        return name.toUpperCase(Locale.ROOT);
    }

    private static boolean isAir(String mat) {
        return "AIR".equals(mat) || "CAVE_AIR".equals(mat) || "VOID_AIR".equals(mat);
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.blueprint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validates blueprint files and structures against malicious payloads, zip bombs,
 * memory exhaustion attacks, and illegal game blocks (command blocks, bedrock, barriers).
 */
public final class BlueprintSecurityValidator {

    public static final Set<String> DANGEROUS_BLOCKS = Set.of(
            "BEDROCK",
            "BARRIER",
            "COMMAND_BLOCK",
            "CHAIN_COMMAND_BLOCK",
            "REPEATING_COMMAND_BLOCK",
            "STRUCTURE_BLOCK",
            "STRUCTURE_VOID",
            "JIGSAW",
            "LIGHT",
            "END_PORTAL",
            "END_PORTAL_FRAME",
            "END_GATEWAY",
            "REINFORCED_DEEPSLATE"
    );

    public static boolean isDangerousBlock(String material) {
        if (material == null || material.isBlank()) return false;
        String clean = material.trim().toUpperCase(Locale.ROOT);
        if (clean.contains("[")) {
            clean = clean.substring(0, clean.indexOf('['));
        }
        if (clean.startsWith("MINECRAFT:")) {
            clean = clean.substring("MINECRAFT:".length());
        }
        return DANGEROUS_BLOCKS.contains(clean);
    }

    private BlueprintSecurityValidator() {
    }

    /**
     * Verifies magic bytes and size of uploaded file data.
     * Prevents executables, shell scripts, HTML, or corrupted files from being processed.
     */
    public static void validateFileHeader(byte[] data, String fileName, double maxFileSizeMb) {
        if (data == null || data.length < 2) {
            throw new IllegalArgumentException("Uploaded file is empty or corrupted.");
        }

        long maxBytes = (long) (maxFileSizeMb * 1024 * 1024);
        if (data.length > maxBytes) {
            throw new IllegalArgumentException(String.format(
                    Locale.ROOT,
                    "File size (%.2f MB) exceeds maximum allowed size of %.2f MB.",
                    data.length / (1024.0 * 1024.0),
                    maxFileSizeMb
            ));
        }

        boolean isGzip = (data[0] & 0xFF) == 0x1F && (data[1] & 0xFF) == 0x8B;
        boolean isUncompressedNbt = data[0] == 0x0A; // TAG_Compound root

        if (!isGzip && !isUncompressedNbt) {
            throw new IllegalArgumentException(
                    "Invalid file header. Only GZIP-compressed (.litematic / .nbt) or vanilla NBT compound structures are accepted."
            );
        }
    }

    /**
     * Checks structure dimensions, block count caps, and filters dangerous blocks.
     */
    public static Blueprint sanitizeAndValidate(
            Blueprint bp,
            int maxDimension,
            int maxBlocks,
            boolean filterDangerous
    ) {
        if (bp == null) {
            throw new IllegalArgumentException("Blueprint cannot be null.");
        }

        if (bp.sizeX() > maxDimension || bp.sizeY() > maxDimension || bp.sizeZ() > maxDimension) {
            throw new IllegalArgumentException(String.format(
                    Locale.ROOT,
                    "Blueprint dimensions (%dx%dx%d) exceed maximum dimension limit of %d blocks.",
                    bp.sizeX(), bp.sizeY(), bp.sizeZ(), maxDimension
            ));
        }

        if (bp.totalBlocks() > maxBlocks) {
            throw new IllegalArgumentException(String.format(
                    Locale.ROOT,
                    "Blueprint block count (%d blocks) exceeds maximum limit of %d blocks.",
                    bp.totalBlocks(), maxBlocks
            ));
        }

        if (!filterDangerous) {
            return bp;
        }

        // Sanitize: filter out dangerous blocks
        List<Blueprint.PlacementBlock> sanitizedBlocks = new ArrayList<>();
        Map<String, Integer> sanitizedMaterials = new LinkedHashMap<>();
        boolean modified = false;

        for (Blueprint.PlacementBlock block : bp.blocks()) {
            if (isDangerousBlock(block.material())) {
                modified = true;
                continue; // Strip illegal block
            }
            sanitizedBlocks.add(block);
            String itemKey = BlueprintParser.resolveItemName(block.material());
            sanitizedMaterials.put(itemKey, sanitizedMaterials.getOrDefault(itemKey, 0) + 1);
        }

        if (!modified) {
            return bp;
        }

        return new Blueprint(
                bp.id(),
                bp.name(),
                bp.author(),
                bp.format(),
                bp.sizeX(),
                bp.sizeY(),
                bp.sizeZ(),
                sanitizedBlocks.size(),
                sanitizedMaterials,
                sanitizedBlocks,
                bp.uploadTime()
        );
    }
}

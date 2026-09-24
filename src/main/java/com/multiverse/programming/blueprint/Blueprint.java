// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.blueprint;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Structured representation of a 3D building blueprint parsed from a .litematic or .nbt file.
 */
public record Blueprint(
        String id,
        String name,
        String author,
        String format,
        int sizeX,
        int sizeY,
        int sizeZ,
        int totalBlocks,
        Map<String, Integer> materialCounts,
        List<PlacementBlock> blocks,
        long uploadTime
) {

    public record PlacementBlock(int x, int y, int z, String material) {
    }

    public Blueprint {
        materialCounts = Collections.unmodifiableMap(materialCounts);
        blocks = Collections.unmodifiableList(blocks);
    }
}

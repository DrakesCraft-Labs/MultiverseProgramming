// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.blueprint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles rotation of 3D blueprints and their block states around the Y-axis.
 */
public final class BlueprintRotator {

    private static final Pattern FACING_PATTERN = Pattern.compile("facing=(north|east|south|west)");
    private static final Pattern AXIS_PATTERN = Pattern.compile("axis=(x|z)");
    private static final Pattern ROTATION_PATTERN = Pattern.compile("rotation=(\\d+)");
    private static final Pattern SIDES_PATTERN = Pattern.compile("(north|east|south|west)=(true|false|none|low|tall)");

    private BlueprintRotator() {
    }

    /**
     * Parses an orientation string into degrees (0, 90, 180, 270).
     */
    public static int normalizeRotation(String orientation) {
        if (orientation == null || orientation.trim().isEmpty()) {
            return 0;
        }
        String s = orientation.trim().toUpperCase(Locale.ROOT);
        return switch (s) {
            case "EAST", "E", "90" -> 90;
            case "SOUTH", "S", "180" -> 180;
            case "WEST", "W", "270" -> 270;
            case "NORTH", "N", "0", "360" -> 0;
            default -> {
                try {
                    int deg = Integer.parseInt(s) % 360;
                    if (deg < 0) deg += 360;
                    if (deg >= 45 && deg < 135) yield 90;
                    if (deg >= 135 && deg < 225) yield 180;
                    if (deg >= 225 && deg < 315) yield 270;
                    yield 0;
                } catch (NumberFormatException e) {
                    yield 0;
                }
            }
        };
    }

    /**
     * Rotates a blueprint clockwise by the specified degrees around the Y-axis.
     */
    public static Blueprint rotate(Blueprint bp, int degrees) {
        int deg = degrees % 360;
        if (deg < 0) deg += 360;
        deg = (deg / 90) * 90;

        if (deg == 0 || bp == null) {
            return bp;
        }

        int origX = bp.sizeX();
        int origY = bp.sizeY();
        int origZ = bp.sizeZ();

        int newSizeX = (deg == 90 || deg == 270) ? origZ : origX;
        int newSizeY = origY;
        int newSizeZ = (deg == 90 || deg == 270) ? origX : origZ;

        List<Blueprint.PlacementBlock> rotatedBlocks = new ArrayList<>(bp.blocks().size());

        for (Blueprint.PlacementBlock block : bp.blocks()) {
            int rx, rz;
            switch (deg) {
                case 90 -> {
                    rx = origZ - 1 - block.z();
                    rz = block.x();
                }
                case 180 -> {
                    rx = origX - 1 - block.x();
                    rz = origZ - 1 - block.z();
                }
                case 270 -> {
                    rx = block.z();
                    rz = origX - 1 - block.x();
                }
                default -> {
                    rx = block.x();
                    rz = block.z();
                }
            }
            String rotatedMat = rotateBlockDataString(block.material(), deg);
            rotatedBlocks.add(new Blueprint.PlacementBlock(rx, block.y(), rz, rotatedMat));
        }

        return new Blueprint(
                bp.id(),
                bp.name(),
                bp.author(),
                bp.format(),
                newSizeX,
                newSizeY,
                newSizeZ,
                bp.totalBlocks(),
                bp.materialCounts(),
                rotatedBlocks,
                bp.uploadTime()
        );
    }

    /**
     * Rotates Minecraft block data properties (facing, axis, rotation, connection sides).
     */
    public static String rotateBlockDataString(String material, int degrees) {
        if (material == null || !material.contains("[") || degrees == 0) {
            return material;
        }

        String result = material;

        // 1. Rotate 'facing'
        Matcher facingMatcher = FACING_PATTERN.matcher(result);
        if (facingMatcher.find()) {
            String cur = facingMatcher.group(1);
            String rotatedFacing = rotateDirection(cur, degrees);
            result = result.replace("facing=" + cur, "facing=" + rotatedFacing);
        }

        // 2. Rotate 'axis' (e.g. logs/pillars axis=x <-> axis=z for 90 or 270 degrees)
        if (degrees == 90 || degrees == 270) {
            Matcher axisMatcher = AXIS_PATTERN.matcher(result);
            if (axisMatcher.find()) {
                String cur = axisMatcher.group(1);
                String newAxis = cur.equals("x") ? "z" : "x";
                result = result.replace("axis=" + cur, "axis=" + newAxis);
            }
        }

        // 3. Rotate 16-step sign/banner 'rotation=0..15'
        Matcher rotMatcher = ROTATION_PATTERN.matcher(result);
        if (rotMatcher.find()) {
            try {
                int curRot = Integer.parseInt(rotMatcher.group(1));
                int steps = (degrees / 90) * 4;
                int newRot = (curRot + steps) % 16;
                result = result.replace("rotation=" + curRot, "rotation=" + newRot);
            } catch (NumberFormatException ignored) {}
        }

        // 4. Rotate connection sides (north, east, south, west)
        if (result.contains("north=") || result.contains("east=") || result.contains("south=") || result.contains("west=")) {
            result = rotateConnectionSides(result, degrees);
        }

        return result;
    }

    private static String rotateDirection(String dir, int degrees) {
        int steps = (degrees / 90) % 4;
        String[] dirs = {"north", "east", "south", "west"};
        int idx = -1;
        for (int i = 0; i < 4; i++) {
            if (dirs[i].equalsIgnoreCase(dir)) {
                idx = i;
                break;
            }
        }
        if (idx == -1) return dir;
        int next = (idx + steps) % 4;
        return dirs[next];
    }

    private static String rotateConnectionSides(String blockData, int degrees) {
        int steps = (degrees / 90) % 4;
        if (steps == 0) return blockData;

        // Parse existing values
        String north = extractSide(blockData, "north");
        String east = extractSide(blockData, "east");
        String south = extractSide(blockData, "south");
        String west = extractSide(blockData, "west");

        String[] sides = {north, east, south, west};
        String[] newSides = new String[4];
        for (int i = 0; i < 4; i++) {
            // For a clockwise rotation by steps, the new value at index i comes from (i - steps + 4) % 4
            int src = (i - steps + 4) % 4;
            newSides[i] = sides[src];
        }

        String res = blockData;
        if (north != null) res = replaceSide(res, "north", newSides[0]);
        if (east != null) res = replaceSide(res, "east", newSides[1]);
        if (south != null) res = replaceSide(res, "south", newSides[2]);
        if (west != null) res = replaceSide(res, "west", newSides[3]);
        return res;
    }

    private static String extractSide(String blockData, String side) {
        Pattern p = Pattern.compile(side + "=([a-zA-Z0-9]+)");
        Matcher m = p.matcher(blockData);
        return m.find() ? m.group(1) : null;
    }

    private static String replaceSide(String blockData, String side, String newVal) {
        if (newVal == null) return blockData;
        return blockData.replaceAll(side + "=[a-zA-Z0-9]+", side + "=" + newVal);
    }
}

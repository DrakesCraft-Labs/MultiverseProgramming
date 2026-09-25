// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.protection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CoreProtectBridgeTest {

    @Test
    @DisplayName("Should safely handle logPlacement when CoreProtect is not loaded")
    void testLogPlacementSafeWithoutCoreProtect() {
        assertDoesNotThrow(() -> {
            CoreProtectBridge.logPlacement(
                    UUID.randomUUID(),
                    "turtle-test-1",
                    null,
                    Material.STONE,
                    null
            );
        });
    }

    @Test
    @DisplayName("Should safely handle logRemoval when CoreProtect is not loaded")
    void testLogRemovalSafeWithoutCoreProtect() {
        assertDoesNotThrow(() -> {
            CoreProtectBridge.logRemoval(
                    UUID.randomUUID(),
                    "turtle-test-1",
                    null,
                    Material.DIRT,
                    null
            );
        });
    }
}

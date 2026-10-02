// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.blueprint;

import com.multiverse.programming.BukkitMockHelper;
import com.multiverse.programming.ConfigManager;
import com.multiverse.programming.MultiverseProgrammingPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BlueprintManagerTest {

    @TempDir
    Path tempDir;

    private MultiverseProgrammingPlugin plugin;
    private ConfigManager configManager;
    private BlueprintManager manager;

    @BeforeEach
    void setUp() {
        BukkitMockHelper.setUpMockServer();
        plugin = mock(MultiverseProgrammingPlugin.class);
        configManager = mock(ConfigManager.class);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("BlueprintManagerTest"));

        when(configManager.getBlueprintMaxDimension()).thenReturn(512);
        when(configManager.getBlueprintMaxBlocks()).thenReturn(250_000);
        when(configManager.getBlueprintMaxFileSizeMb()).thenReturn(10.0);
        when(configManager.getBlueprintPlayerQuotaMb()).thenReturn(15.0);
        when(configManager.isBlueprintFilterDangerous()).thenReturn(false);

        manager = new BlueprintManager(plugin);
    }

    @AfterEach
    void tearDown() {
        BukkitMockHelper.tearDownMockServer();
    }

    private byte[] createTestNbtBytes(String name) throws IOException {
        ByteArrayOutputStream uncompressed = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(uncompressed)) {
            out.writeByte(10); // Root
            out.writeUTF("");

            out.writeByte(8); // String name
            out.writeUTF("name");
            out.writeUTF(name);

            // size [1, 1, 1]
            out.writeByte(9); // List
            out.writeUTF("size");
            out.writeByte(3); // Int
            out.writeInt(3);
            out.writeInt(1);
            out.writeInt(1);
            out.writeInt(1);

            // palette
            out.writeByte(9);
            out.writeUTF("palette");
            out.writeByte(10);
            out.writeInt(1);

            out.writeByte(8);
            out.writeUTF("Name");
            out.writeUTF("minecraft:stone");
            out.writeByte(0);

            // blocks
            out.writeByte(9);
            out.writeUTF("blocks");
            out.writeByte(10);
            out.writeInt(1);

            out.writeByte(3); // state
            out.writeUTF("state");
            out.writeInt(0);

            out.writeByte(9); // pos
            out.writeUTF("pos");
            out.writeByte(3);
            out.writeInt(3);
            out.writeInt(0);
            out.writeInt(0);
            out.writeInt(0);

            out.writeByte(0); // End block
            out.writeByte(0); // End root
        }

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write(uncompressed.toByteArray());
        }
        return compressed.toByteArray();
    }

    @Test
    @DisplayName("Register and lookup blueprint by ID, name, and filename base alias")
    void testRegisterAndLookup() throws IOException {
        byte[] data = createTestNbtBytes("My Castle Build");
        Blueprint bp = manager.register("castle_v1.nbt", data, "Player1");

        assertNotNull(bp);
        assertTrue(bp.id().startsWith("BP-"));
        assertEquals("My Castle Build", bp.name());

        // Lookup by exact ID
        Blueprint byId = manager.getBlueprint(bp.id());
        assertNotNull(byId);
        assertEquals(bp.id(), byId.id());

        // Lookup by internal blueprint name
        Blueprint byName = manager.getBlueprint("My Castle Build");
        assertNotNull(byName);
        assertEquals(bp.id(), byName.id());

        // Lookup by filename base (alias)
        Blueprint byFile = manager.getBlueprint("castle_v1");
        assertNotNull(byFile);
        assertEquals(bp.id(), byFile.id());
    }

    @Test
    @DisplayName("Load from disk with BP- prefix preserves custom ID")
    void testLoadDiskWithBpPrefix() throws IOException {
        File bpFolder = new File(tempDir.toFile(), "blueprints");
        bpFolder.mkdirs();

        byte[] data = createTestNbtBytes("Tower Defense");
        File file = new File(bpFolder, "BP-TOWER_tower.nbt");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }

        manager.loadAll();

        Blueprint bp = manager.getBlueprint("BP-TOWER");
        assertNotNull(bp, "Blueprint with prefix BP-TOWER should be indexed with ID BP-TOWER");
        assertEquals("BP-TOWER", bp.id());

        // Also accessible by filename base
        assertNotNull(manager.getBlueprint("tower"));
    }

    @Test
    @DisplayName("Delete blueprint removes files, metadata, and aliases")
    void testDeleteBlueprint() throws IOException {
        byte[] data = createTestNbtBytes("Temporary Hut");
        Blueprint bp = manager.register("hut.nbt", data, "Player1");

        assertNotNull(manager.getBlueprint(bp.id()));
        assertNotNull(manager.getBlueprint("hut"));

        boolean deleted = manager.deleteBlueprint(bp.id(), "Player1");
        assertTrue(deleted);

        assertNull(manager.getBlueprint(bp.id()));
        assertNull(manager.getBlueprint("hut"));
    }

    @Test
    @DisplayName("SSRF guard blocks non-public, loopback, metadata and non-http blueprint URLs")
    void testUrlGuardBlocksInternalTargets() {
        assertThrows(IllegalArgumentException.class, () -> BlueprintManager.validateUrlAllowed("http://127.0.0.1/x"));
        assertThrows(IllegalArgumentException.class, () -> BlueprintManager.validateUrlAllowed("http://localhost/x"));
        assertThrows(IllegalArgumentException.class, () -> BlueprintManager.validateUrlAllowed("http://169.254.169.254/latest/meta-data/"));
        assertThrows(IllegalArgumentException.class, () -> BlueprintManager.validateUrlAllowed("http://10.0.0.5/x"));
        assertThrows(IllegalArgumentException.class, () -> BlueprintManager.validateUrlAllowed("http://192.168.1.1/x"));
        assertThrows(IllegalArgumentException.class, () -> BlueprintManager.validateUrlAllowed("http://100.100.0.1/x"));
        assertThrows(IllegalArgumentException.class, () -> BlueprintManager.validateUrlAllowed("ftp://example.com/x"));
        assertThrows(IllegalArgumentException.class, () -> BlueprintManager.validateUrlAllowed("not a url"));
    }

    @Test
    @DisplayName("SSRF guard allows public addresses")
    void testUrlGuardAllowsPublicHosts() {
        // Literal public IPs avoid DNS flakiness while still exercising the public-address path.
        assertDoesNotThrow(() -> BlueprintManager.validateUrlAllowed("https://8.8.8.8/x/y.litematic"));
        assertDoesNotThrow(() -> BlueprintManager.validateUrlAllowed("http://1.1.1.1/somekey"));
    }
}

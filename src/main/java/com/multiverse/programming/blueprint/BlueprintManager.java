// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.blueprint;

import com.multiverse.programming.MultiverseProgrammingPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manages saving, loading, caching, and querying of 3D construction blueprints.
 */
public final class BlueprintManager {

    private final MultiverseProgrammingPlugin plugin;
    private final File storageDir;
    private final Map<String, Blueprint> blueprints = new LinkedHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public BlueprintManager(MultiverseProgrammingPlugin plugin) {
        this.plugin = plugin;
        this.storageDir = new File(plugin.getDataFolder(), "blueprints");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
    }

    public synchronized void loadAll() {
        blueprints.clear();
        File[] files = storageDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            return lower.endsWith(".litematic") || lower.endsWith(".nbt");
        });
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                String id = extractOrGenerateId(file.getName());
                Blueprint bp = BlueprintParser.parse(id, file);
                blueprints.put(id.toUpperCase(Locale.ROOT), bp);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load blueprint from " + file.getName() + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + blueprints.size() + " blueprints.");
    }

    public synchronized Blueprint register(String fileName, InputStream in) throws IOException {
        String id = generateUniqueId();
        String safeName = id + "_" + fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        File destFile = new File(storageDir, safeName);

        try (FileOutputStream fos = new FileOutputStream(destFile)) {
            in.transferTo(fos);
        }

        Blueprint bp = BlueprintParser.parse(id, destFile);
        blueprints.put(id.toUpperCase(Locale.ROOT), bp);
        return bp;
    }

    public synchronized Blueprint getBlueprint(String idOrName) {
        if (idOrName == null || idOrName.isBlank()) {
            return null;
        }
        String key = idOrName.trim().toUpperCase(Locale.ROOT);
        Blueprint bp = blueprints.get(key);
        if (bp != null) {
            return bp;
        }
        for (Blueprint b : blueprints.values()) {
            if (b.name().equalsIgnoreCase(idOrName.trim())) {
                return b;
            }
        }
        return null;
    }

    public synchronized Collection<Blueprint> getAllBlueprints() {
        return Collections.unmodifiableCollection(blueprints.values());
    }

    public synchronized boolean deleteBlueprint(String id) {
        Blueprint bp = blueprints.remove(id.toUpperCase(Locale.ROOT));
        if (bp == null) {
            return false;
        }
        File[] files = storageDir.listFiles((dir, name) -> name.startsWith(id));
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
        return true;
    }

    private String generateUniqueId() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        for (int attempts = 0; attempts < 1000; attempts++) {
            StringBuilder sb = new StringBuilder("BP-");
            for (int i = 0; i < 4; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            String candidate = sb.toString();
            if (!blueprints.containsKey(candidate)) {
                return candidate;
            }
        }
        return "BP-" + System.currentTimeMillis();
    }

    private String extractOrGenerateId(String fileName) {
        if (fileName.startsWith("BP-") && fileName.indexOf('_') > 3) {
            return fileName.substring(0, fileName.indexOf('_'));
        }
        return generateUniqueId();
    }
}

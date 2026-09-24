// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.blueprint;

import com.multiverse.programming.ConfigManager;
import com.multiverse.programming.MultiverseProgrammingPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manages saving, loading, caching, querying, and quota enforcement for 3D construction blueprints.
 * Includes per-player storage limits (15 MB default), SHA-256 deduplication, security validation,
 * and automatic orphan/garbage retention cleanup.
 */
public final class BlueprintManager {

    private final MultiverseProgrammingPlugin plugin;
    private final File storageDir;
    private final File metadataFile;
    private final Map<String, Blueprint> blueprints = new LinkedHashMap<>();
    private final Map<String, String> blueprintOwners = new HashMap<>(); // ID -> Player Name/UUID
    private final Map<String, Long> blueprintSizes = new HashMap<>();  // ID -> Bytes on disk
    private final Map<String, String> fileHashes = new HashMap<>();     // SHA-256 -> ID
    private final SecureRandom random = new SecureRandom();

    public BlueprintManager(MultiverseProgrammingPlugin plugin) {
        this.plugin = plugin;
        this.storageDir = new File(plugin.getDataFolder(), "blueprints");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        this.metadataFile = new File(plugin.getDataFolder(), "blueprint_metadata.yml");
    }

    public synchronized void loadAll() {
        blueprints.clear();
        blueprintOwners.clear();
        blueprintSizes.clear();
        fileHashes.clear();

        loadMetadata();

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
                Blueprint rawBp = BlueprintParser.parse(id, file);

                ConfigManager cfg = plugin.getConfigManager();
                int maxDim = cfg != null ? cfg.getBlueprintMaxDimension() : 512;
                int maxBlocks = cfg != null ? cfg.getBlueprintMaxBlocks() : 250_000;
                boolean filterDangerous = cfg != null && cfg.isBlueprintFilterDangerous();

                Blueprint bp = BlueprintSecurityValidator.sanitizeAndValidate(rawBp, maxDim, maxBlocks, filterDangerous);
                blueprints.put(id.toUpperCase(Locale.ROOT), bp);
                blueprintSizes.put(id.toUpperCase(Locale.ROOT), file.length());

                // Compute hash for deduplication
                byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                String hash = computeSha256(bytes);
                fileHashes.put(hash, id.toUpperCase(Locale.ROOT));

                if (!blueprintOwners.containsKey(id.toUpperCase(Locale.ROOT))) {
                    blueprintOwners.put(id.toUpperCase(Locale.ROOT), "Server");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load blueprint from " + file.getName() + ": " + e.getMessage());
            }
        }
        saveMetadata();
        plugin.getLogger().info("Loaded " + blueprints.size() + " blueprints.");
    }

    /**
     * Registers a blueprint from raw input stream (e.g. commands / plugins) under the "Server" owner.
     */
    public synchronized Blueprint register(String fileName, InputStream in) throws IOException {
        byte[] data = in.readAllBytes();
        return register(fileName, data, "Server");
    }

    /**
     * Registers an uploaded blueprint enforcing security, quotas (15 MB), and deduplication.
     */
    public synchronized Blueprint register(String fileName, byte[] data, String owner) throws IOException {
        if (owner == null || owner.isBlank()) {
            owner = "Server";
        }

        ConfigManager cfg = plugin.getConfigManager();
        double maxFileMb = cfg != null ? cfg.getBlueprintMaxFileSizeMb() : 10.0;
        double quotaMb = cfg != null ? cfg.getBlueprintPlayerQuotaMb() : 15.0;
        int maxDim = cfg != null ? cfg.getBlueprintMaxDimension() : 512;
        int maxBlocks = cfg != null ? cfg.getBlueprintMaxBlocks() : 250_000;
        boolean filterDangerous = cfg != null && cfg.isBlueprintFilterDangerous();

        // 1. Security check: Magic bytes & file size limit
        BlueprintSecurityValidator.validateFileHeader(data, fileName, maxFileMb);

        // 2. Quota check: 15 MB per player (unless Server/Admin)
        if (!"Server".equalsIgnoreCase(owner)) {
            long usedBytes = getPlayerUsageBytes(owner);
            long quotaBytes = (long) (quotaMb * 1024 * 1024);
            if (usedBytes + data.length > quotaBytes) {
                throw new IllegalStateException(String.format(
                        Locale.ROOT,
                        "Storage quota exceeded. Used: %.2f MB / %.2f MB. New file (%.2f MB) would exceed your 15 MB limit.",
                        usedBytes / (1024.0 * 1024.0),
                        quotaMb,
                        data.length / (1024.0 * 1024.0)
                ));
            }
        }

        // 3. Deduplication check via SHA-256
        String hash = computeSha256(data);
        if (fileHashes.containsKey(hash)) {
            String existingId = fileHashes.get(hash);
            Blueprint existing = blueprints.get(existingId);
            if (existing != null) {
                return existing;
            }
        }

        // 4. Generate unique ID & destination file
        String id = generateUniqueId();
        String safeName = id + "_" + fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        File destFile = new File(storageDir, safeName);

        try (FileOutputStream fos = new FileOutputStream(destFile)) {
            fos.write(data);
        }

        // 5. Parse, sanitize and validate structure
        Blueprint rawBp = BlueprintParser.parse(id, destFile);
        Blueprint bp = BlueprintSecurityValidator.sanitizeAndValidate(rawBp, maxDim, maxBlocks, filterDangerous);

        // 6. Record metadata & update quota
        String upperId = id.toUpperCase(Locale.ROOT);
        blueprints.put(upperId, bp);
        blueprintOwners.put(upperId, owner);
        blueprintSizes.put(upperId, (long) data.length);
        fileHashes.put(hash, upperId);

        saveMetadata();
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

    public synchronized long getPlayerUsageBytes(String owner) {
        if (owner == null) return 0L;
        long total = 0L;
        for (Map.Entry<String, String> entry : blueprintOwners.entrySet()) {
            if (owner.equalsIgnoreCase(entry.getValue())) {
                total += blueprintSizes.getOrDefault(entry.getKey(), 0L);
            }
        }
        return total;
    }

    public synchronized String getOwner(String blueprintId) {
        return blueprintOwners.getOrDefault(blueprintId.toUpperCase(Locale.ROOT), "Unknown");
    }

    public synchronized boolean deleteBlueprint(String id, String playerOrAdmin) {
        String upperId = id.toUpperCase(Locale.ROOT);
        Blueprint bp = blueprints.get(upperId);
        if (bp == null) {
            return false;
        }

        String owner = blueprintOwners.getOrDefault(upperId, "Server");
        boolean isOwner = playerOrAdmin != null && playerOrAdmin.equalsIgnoreCase(owner);
        boolean isAdmin = playerOrAdmin != null && (playerOrAdmin.equalsIgnoreCase("Server") || playerOrAdmin.equalsIgnoreCase("Admin"));

        if (!isOwner && !isAdmin) {
            throw new SecurityException("You do not have permission to delete this blueprint. Owner: " + owner);
        }

        blueprints.remove(upperId);
        blueprintOwners.remove(upperId);
        blueprintSizes.remove(upperId);

        // Remove from file hashes
        fileHashes.values().removeIf(upperId::equals);

        File[] files = storageDir.listFiles((dir, name) -> name.startsWith(upperId));
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }

        saveMetadata();
        return true;
    }

    public synchronized int cleanOldBlueprints(int days) {
        if (days <= 0) return 0;
        long cutoff = System.currentTimeMillis() - ((long) days * 24L * 60L * 60L * 1000L);
        int deleted = 0;

        var iterator = blueprints.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Blueprint bp = entry.getValue();
            if (bp.uploadTime() < cutoff && !"Server".equalsIgnoreCase(blueprintOwners.get(entry.getKey()))) {
                String id = entry.getKey();
                iterator.remove();
                blueprintOwners.remove(id);
                blueprintSizes.remove(id);
                fileHashes.values().removeIf(id::equals);

                File[] files = storageDir.listFiles((dir, name) -> name.startsWith(id));
                if (files != null) {
                    for (File f : files) {
                        f.delete();
                    }
                }
                deleted++;
            }
        }
        if (deleted > 0) {
            saveMetadata();
        }
        return deleted;
    }

    private void loadMetadata() {
        if (!metadataFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(metadataFile);
        if (yaml.isConfigurationSection("blueprints")) {
            for (String id : yaml.getConfigurationSection("blueprints").getKeys(false)) {
                String upperId = id.toUpperCase(Locale.ROOT);
                blueprintOwners.put(upperId, yaml.getString("blueprints." + id + ".owner", "Server"));
                blueprintSizes.put(upperId, yaml.getLong("blueprints." + id + ".size", 0L));
            }
        }
    }

    private void saveMetadata() {
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            for (Map.Entry<String, String> entry : blueprintOwners.entrySet()) {
                yaml.set("blueprints." + entry.getKey() + ".owner", entry.getValue());
                yaml.set("blueprints." + entry.getKey() + ".size", blueprintSizes.getOrDefault(entry.getKey(), 0L));
            }
            yaml.save(metadataFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save blueprint metadata: " + e.getMessage());
        }
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(data.length);
        }
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

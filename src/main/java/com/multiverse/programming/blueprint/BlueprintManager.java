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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Map<String, String> nameAliases = new ConcurrentHashMap<>(); // Alias/Filename -> ID
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
        nameAliases.clear();

        loadMetadata();

        File[] files = storageDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            return lower.endsWith(".litematic") || lower.endsWith(".nbt");
        });
        if (files == null || files.length == 0) {
            try (InputStream in = plugin.getResource("blueprints/nether_portal.litematic")) {
                if (in != null) {
                    File sampleFile = new File(storageDir, "BP-NETHER-PORTAL_nether_portal.litematic");
                    java.nio.file.Files.copy(in, sampleFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    files = storageDir.listFiles((dir, name) -> {
                        String lower = name.toLowerCase(Locale.ROOT);
                        return lower.endsWith(".litematic") || lower.endsWith(".nbt");
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not unpack default sample blueprint: " + e.getMessage());
            }
        }
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
                String upperId = id.toUpperCase(Locale.ROOT);
                blueprints.put(upperId, bp);
                blueprintSizes.put(upperId, file.length());

                // Register file name and blueprint name aliases
                registerAliases(upperId, file.getName(), bp.name());

                // Compute hash for deduplication
                byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                String hash = computeSha256(bytes);
                fileHashes.put(hash, upperId);

                if (!blueprintOwners.containsKey(upperId)) {
                    blueprintOwners.put(upperId, "Server");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load blueprint from " + file.getName() + ": " + e.getMessage());
            }
        }
        saveMetadata();
        plugin.getLogger().info("Loaded " + blueprints.size() + " blueprints.");
    }

    private void registerAliases(String upperId, String fileName, String blueprintName) {
        String baseName = fileName.replaceFirst("(?i)\\.(litematic|nbt)$", "").trim().toUpperCase(Locale.ROOT);
        nameAliases.put(baseName, upperId);
        if (baseName.contains("_")) {
            String after = baseName.substring(baseName.indexOf('_') + 1).trim();
            if (!after.isBlank()) {
                nameAliases.put(after, upperId);
            }
        }
        if (blueprintName != null && !blueprintName.isBlank()) {
            nameAliases.put(blueprintName.trim().toUpperCase(Locale.ROOT), upperId);
        }
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
        return register(fileName, data, owner, false);
    }

    public synchronized Blueprint register(String fileName, byte[] data, String owner, boolean bypass) throws IOException {
        if (owner == null || owner.isBlank()) {
            owner = "Server";
        }

        ConfigManager cfg = plugin.getConfigManager();
        double maxFileMb = cfg != null ? cfg.getBlueprintMaxFileSizeMb() : 10.0;
        double quotaMb = cfg != null ? cfg.getBlueprintPlayerQuotaMb() : 15.0;
        int maxDim = cfg != null ? cfg.getBlueprintMaxDimension() : 512;
        int maxBlocks = cfg != null ? cfg.getBlueprintMaxBlocks() : 250_000;
        boolean filterDangerous = cfg != null && cfg.isBlueprintFilterDangerous();

        if (bypass) {
            maxFileMb = Math.max(maxFileMb, 100.0);
            maxDim = Math.max(maxDim, 4096);
            maxBlocks = Math.max(maxBlocks, 10_000_000);
        }

        // 1. Security check: Magic bytes & file size limit
        BlueprintSecurityValidator.validateFileHeader(data, fileName, maxFileMb);

        // 2. Quota check: 15 MB per player (unless Server/Admin or bypass)
        if (!bypass && !"Server".equalsIgnoreCase(owner) && !"Admin".equalsIgnoreCase(owner)) {
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
        registerAliases(upperId, fileName, bp.name());

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
        String aliasedId = nameAliases.get(key);
        if (aliasedId != null) {
            bp = blueprints.get(aliasedId);
            if (bp != null) {
                return bp;
            }
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

    /**
     * Resolves a blueprint locally or asynchronously downloads and registers it from:
     * 1. Direct URL (http:// or https://)
     * 2. Bytebin pastebin cloud (short key)
     * 3. Official GitHub repository catalog (BP-* or filename)
     *
     * @param codeOrUrl The blueprint ID, pastebin key, or HTTP URL.
     * @param owner Player requesting or "Server".
     * @return CompletableFuture completing with the parsed, validated, and registered Blueprint.
     */
    public CompletableFuture<Blueprint> getOrDownloadBlueprint(String codeOrUrl, String owner) {
        return getOrDownloadBlueprint(codeOrUrl, owner, false);
    }

    public CompletableFuture<Blueprint> getOrDownloadBlueprint(String codeOrUrl, String owner, boolean bypass) {
        if (codeOrUrl == null || codeOrUrl.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Blueprint code or URL cannot be empty"));
        }

        String target = codeOrUrl.trim();
        Blueprint existing = getBlueprint(target);
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }

        return CompletableFuture.supplyAsync(() -> {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(8))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            List<String> candidateUrls = new ArrayList<>();
            if (target.startsWith("http://") || target.startsWith("https://")) {
                candidateUrls.add(target);
            } else if (target.startsWith("BP-") || target.startsWith("bp-")) {
                String rawName = target.substring(3).toLowerCase(Locale.ROOT).replace('-', '_');
                candidateUrls.add("https://raw.githubusercontent.com/DrakesCraft-Labs/MultiverseProgramming/main/docs/blueprints/" + rawName + ".litematic");
                candidateUrls.add("https://raw.githubusercontent.com/DrakesCraft-Labs/MultiverseProgramming/main/docs/blueprints/" + rawName + ".nbt");
                candidateUrls.add("https://raw.githubusercontent.com/DrakesCraft-Labs/MultiverseProgramming/main/blueprints/" + rawName + ".litematic");
                candidateUrls.add("https://bytebin.lucko.me/" + target);
            } else {
                // Typical bytebin key e.g. eIoNTIWqo1
                candidateUrls.add("https://bytebin.lucko.me/" + target);
                candidateUrls.add("https://raw.githubusercontent.com/DrakesCraft-Labs/MultiverseProgramming/main/docs/blueprints/" + target + ".litematic");
            }

            byte[] downloadedBytes = null;
            String resolvedUrl = null;
            Exception lastException = null;

            for (String urlStr : candidateUrls) {
                try {
                    validateUrlAllowed(urlStr);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(urlStr))
                            .timeout(Duration.ofSeconds(10))
                            .header("User-Agent", "MultiverseProgramming-Plugin/1.0.5")
                            .GET()
                            .build();

                    HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                    if (response.statusCode() == 200 && response.body() != null && response.body().length > 0) {
                        downloadedBytes = response.body();
                        resolvedUrl = urlStr;
                        break;
                    }
                } catch (Exception e) {
                    lastException = e;
                }
            }

            if (downloadedBytes == null) {
                String msg = "Could not find blueprint '" + target + "' on Bytebin or GitHub repository.";
                if (lastException != null) {
                    msg += " (" + lastException.getMessage() + ")";
                }
                throw new IllegalStateException(msg);
            }

            // Determine filename & extension from payload magic bytes
            String ext = ".litematic";
            if (downloadedBytes.length >= 2 && downloadedBytes[0] == 0x1f && (downloadedBytes[1] & 0xFF) == 0x8b) {
                ext = ".litematic";
            } else if (downloadedBytes.length > 0 && downloadedBytes[0] == 0x0a) {
                ext = ".nbt";
            }

            String filename = target.replaceAll("[^a-zA-Z0-9._-]", "_");
            if (!filename.toLowerCase(Locale.ROOT).endsWith(ext)) {
                filename += ext;
            }

            try {
                Blueprint bp = register(filename, downloadedBytes, owner, bypass);
                synchronized (this) {
                    blueprints.put(target.toUpperCase(Locale.ROOT), bp);
                }
                plugin.getLogger().info("[BlueprintNexus] Successfully downloaded '" + bp.name() + "' (" + target + ") from " + resolvedUrl);
                return bp;
            } catch (IOException e) {
                throw new RuntimeException("Failed to register downloaded blueprint: " + e.getMessage(), e);
            }
        });
    }

    /**
     * SSRF guard: only allow fetching from public http(s) endpoints. Rejects any URL whose host
     * resolves to a loopback, link-local, site-local (RFC1918), unique-local, any-local, multicast,
     * or cloud-metadata address. Prevents players from making the server probe internal services.
     *
     * @throws IllegalArgumentException if the URL scheme is unsupported or the target is non-public.
     */
    static void validateUrlAllowed(String urlStr) {
        final URI uri;
        try {
            uri = URI.create(urlStr);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Malformed blueprint URL.");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Only http(s) blueprint URLs are allowed.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Blueprint URL is missing a host.");
        }

        java.net.InetAddress[] addresses;
        try {
            addresses = java.net.InetAddress.getAllByName(host);
        } catch (java.net.UnknownHostException e) {
            throw new IllegalArgumentException("Could not resolve blueprint host: " + host);
        }
        for (java.net.InetAddress addr : addresses) {
            if (addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress()
                    || addr.isSiteLocalAddress() || addr.isMulticastAddress() || isUniqueLocalOrMetadata(addr)) {
                throw new IllegalArgumentException(
                        "Refusing to fetch blueprint from a non-public address (" + addr.getHostAddress() + ").");
            }
        }
    }

    private static boolean isUniqueLocalOrMetadata(java.net.InetAddress addr) {
        byte[] b = addr.getAddress();
        if (b.length == 4) {
            // Cloud metadata endpoint 169.254.169.254 (also covered by link-local) and
            // carrier-grade NAT 100.64.0.0/10, which isSiteLocalAddress() does not flag.
            int first = b[0] & 0xFF;
            int second = b[1] & 0xFF;
            return (first == 100 && second >= 64 && second <= 127);
        }
        if (b.length == 16) {
            // IPv6 unique local addresses fc00::/7.
            return (b[0] & 0xFE) == 0xFC;
        }
        return false;
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
        nameAliases.values().removeIf(upperId::equalsIgnoreCase);

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
                nameAliases.values().removeIf(id::equalsIgnoreCase);
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
        String base = fileName.replaceFirst("(?i)\\.(litematic|nbt)$", "").trim();
        if (base.toUpperCase(Locale.ROOT).startsWith("BP-")) {
            if (base.indexOf('_') > 3) {
                return base.substring(0, base.indexOf('_')).toUpperCase(Locale.ROOT);
            }
            return base.toUpperCase(Locale.ROOT);
        }
        return generateUniqueId();
    }
}

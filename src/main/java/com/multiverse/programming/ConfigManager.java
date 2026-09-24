// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

/**
 * Handles loading, validating, and providing safe access to plugin configuration.
 * Designed to prevent plugin crashes due to invalid or malformed configuration values.
 */
public final class ConfigManager {

    public static final Material DEFAULT_COMPUTER_BLOCK = Material.LECTERN;
    public static final Material DEFAULT_ADVANCED_COMPUTER_BLOCK = Material.ENCHANTING_TABLE;
    public static final Material DEFAULT_MONITOR_BLOCK = Material.OCHRE_FROGLIGHT;
    public static final Material DEFAULT_CRAFTER_BLOCK = Material.CRAFTER;
    public static final Material DEFAULT_TRANSPOSER_BLOCK = Material.HOPPER;
    public static final Material DEFAULT_SPEAKER_BLOCK = Material.NOTE_BLOCK;
    public static final Material DEFAULT_TURTLE_BLOCK = Material.DISPENSER;

    public static final long DEFAULT_TIMEOUT_MS = 3000L;
    public static final long DEFAULT_ADVANCED_TIMEOUT_MS = 0L;
    public static final int DEFAULT_MAX_STREAM_LINES = 10_000;
    public static final long DEFAULT_PREVENT_SPAM_DELAY_MS = 500L;
    public static final boolean DEFAULT_DROP_DISKS_ON_BREAK = true;
    public static final boolean DEFAULT_ENABLE_COMPUTER_RECIPE = true;
    public static final int DEFAULT_MAX_CONCURRENT_PROGRAMS = 8;
    public static final int DEFAULT_MAX_INSTRUCTIONS_PER_SLICE = 1_000_000;

    public static final int DEFAULT_TURTLE_BUILD_DELAY_TICKS = 2;
    public static final boolean DEFAULT_TURTLE_REQUIRE_MATERIALS = false;
    public static final boolean DEFAULT_TURTLE_FUEL_REQUIRED = false;
    public static final boolean DEFAULT_WEB_PORTAL_ENABLED = true;
    public static final int DEFAULT_WEB_PORTAL_PORT = 8080;
    public static final String DEFAULT_WEB_PORTAL_BIND = "0.0.0.0";

    private final MultiverseProgrammingPlugin plugin;

    private Material computerBlock = DEFAULT_COMPUTER_BLOCK;
    private Material advancedComputerBlock = DEFAULT_ADVANCED_COMPUTER_BLOCK;
    private Material monitorBlock = DEFAULT_MONITOR_BLOCK;
    private Material crafterBlock = DEFAULT_CRAFTER_BLOCK;
    private Material transposerBlock = DEFAULT_TRANSPOSER_BLOCK;
    private Material speakerBlock = DEFAULT_SPEAKER_BLOCK;
    private Material turtleBlock = DEFAULT_TURTLE_BLOCK;

    private long timeoutMs = DEFAULT_TIMEOUT_MS;
    private long advancedTimeoutMs = DEFAULT_ADVANCED_TIMEOUT_MS;
    private int maxStreamLines = DEFAULT_MAX_STREAM_LINES;
    private long preventSpamDelayMs = DEFAULT_PREVENT_SPAM_DELAY_MS;
    private boolean dropDisksOnBreak = DEFAULT_DROP_DISKS_ON_BREAK;
    private boolean enableComputerRecipe = DEFAULT_ENABLE_COMPUTER_RECIPE;
    private int maxConcurrentPrograms = DEFAULT_MAX_CONCURRENT_PROGRAMS;
    private int maxInstructionsPerSlice = DEFAULT_MAX_INSTRUCTIONS_PER_SLICE;

    private int turtleBuildDelayTicks = DEFAULT_TURTLE_BUILD_DELAY_TICKS;
    private boolean turtleRequireMaterials = DEFAULT_TURTLE_REQUIRE_MATERIALS;
    private boolean turtleFuelRequired = DEFAULT_TURTLE_FUEL_REQUIRED;
    private boolean webPortalEnabled = DEFAULT_WEB_PORTAL_ENABLED;
    private int webPortalPort = DEFAULT_WEB_PORTAL_PORT;
    private String webPortalBindAddress = DEFAULT_WEB_PORTAL_BIND;
    private String webPortalPublicUrl = "";

    public ConfigManager(MultiverseProgrammingPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads and validates the configuration from disk, applying fallbacks for any invalid settings.
     */
    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        Logger logger = plugin.getLogger();

        // 1. Validate computer-block
        this.computerBlock = parseBlockMaterial(
                config.getString("computer-block"),
                DEFAULT_COMPUTER_BLOCK,
                "computer-block",
                logger
        );

        // 2. Validate advanced-computer-block
        Material candidateAdvanced = parseBlockMaterial(
                config.getString("advanced-computer-block"),
                DEFAULT_ADVANCED_COMPUTER_BLOCK,
                "advanced-computer-block",
                logger
        );

        // Ensure computer-block and advanced-computer-block do not collide
        if (candidateAdvanced == this.computerBlock) {
            logger.warning("[Config] 'advanced-computer-block' cannot be the same material as 'computer-block' ("
                    + this.computerBlock.name() + "). Falling back to "
                    + (this.computerBlock == DEFAULT_ADVANCED_COMPUTER_BLOCK ? Material.OBSERVER.name() : DEFAULT_ADVANCED_COMPUTER_BLOCK.name()) + ".");
            if (this.computerBlock == DEFAULT_ADVANCED_COMPUTER_BLOCK) {
                candidateAdvanced = Material.OBSERVER;
            } else {
                candidateAdvanced = DEFAULT_ADVANCED_COMPUTER_BLOCK;
            }
        }
        this.advancedComputerBlock = candidateAdvanced;

        // 3. Validate peripherals
        this.monitorBlock = parseBlockMaterial(
                config.getString("monitor-block"),
                DEFAULT_MONITOR_BLOCK,
                "monitor-block",
                logger
        );
        this.crafterBlock = parseBlockMaterial(
                config.getString("crafter-block"),
                DEFAULT_CRAFTER_BLOCK,
                "crafter-block",
                logger
        );
        this.transposerBlock = parseBlockMaterial(
                config.getString("transposer-block"),
                DEFAULT_TRANSPOSER_BLOCK,
                "transposer-block",
                logger
        );
        this.speakerBlock = parseBlockMaterial(
                config.getString("speaker-block"),
                DEFAULT_SPEAKER_BLOCK,
                "speaker-block",
                logger
        );
        this.turtleBlock = parseBlockMaterial(
                config.getString("turtle-block"),
                DEFAULT_TURTLE_BLOCK,
                "turtle-block",
                logger
        );

        // 4. Validate timeouts
        long rawTimeout = config.getLong("execution-timeout-ms", DEFAULT_TIMEOUT_MS);
        if (rawTimeout < 0) {
            logger.warning("[Config] 'execution-timeout-ms' cannot be negative (" + rawTimeout + "). Using default: " + DEFAULT_TIMEOUT_MS + " ms.");
            this.timeoutMs = DEFAULT_TIMEOUT_MS;
        } else if (rawTimeout > 300_000L) { // 5 minutes max sanity limit for normal computers
            logger.warning("[Config] 'execution-timeout-ms' is unusually high (" + rawTimeout + " ms). Capping at 300000 ms (5 min).");
            this.timeoutMs = 300_000L;
        } else {
            this.timeoutMs = rawTimeout;
        }

        long rawAdvTimeout = config.getLong("advanced-execution-timeout-ms", DEFAULT_ADVANCED_TIMEOUT_MS);
        if (rawAdvTimeout < 0) {
            logger.warning("[Config] 'advanced-execution-timeout-ms' cannot be negative (" + rawAdvTimeout + "). Setting to 0 (unlimited).");
            this.advancedTimeoutMs = 0L;
        } else {
            this.advancedTimeoutMs = rawAdvTimeout;
        }

        // 5. Validate extra settings
        int rawLines = config.getInt("max-stream-lines", DEFAULT_MAX_STREAM_LINES);
        if (rawLines <= 0) {
            this.maxStreamLines = DEFAULT_MAX_STREAM_LINES;
        } else {
            this.maxStreamLines = Math.min(rawLines, 100_000);
        }

        long rawSpam = config.getLong("prevent-spam-delay-ms", DEFAULT_PREVENT_SPAM_DELAY_MS);
        this.preventSpamDelayMs = Math.max(0L, Math.min(rawSpam, 10_000L));

        this.dropDisksOnBreak = config.getBoolean("drop-disks-on-break", DEFAULT_DROP_DISKS_ON_BREAK);
        this.enableComputerRecipe = config.getBoolean("enable-computer-recipe", DEFAULT_ENABLE_COMPUTER_RECIPE);

        int rawConcurrent = config.getInt("max-concurrent-programs", DEFAULT_MAX_CONCURRENT_PROGRAMS);
        this.maxConcurrentPrograms = Math.max(1, Math.min(rawConcurrent, 64));

        int rawInstructions = config.getInt("max-instructions-per-slice", DEFAULT_MAX_INSTRUCTIONS_PER_SLICE);
        this.maxInstructionsPerSlice = Math.max(10_000, rawInstructions);

        // 6. Turtle & Web Portal Settings
        int rawTurtleDelay = config.getInt("turtle-build-delay-ticks", DEFAULT_TURTLE_BUILD_DELAY_TICKS);
        this.turtleBuildDelayTicks = Math.max(1, Math.min(rawTurtleDelay, 100));
        this.turtleRequireMaterials = config.getBoolean("turtle-require-materials", DEFAULT_TURTLE_REQUIRE_MATERIALS);
        this.turtleFuelRequired = config.getBoolean("turtle-fuel-required", DEFAULT_TURTLE_FUEL_REQUIRED);

        this.webPortalEnabled = config.getBoolean("web-portal-enabled", DEFAULT_WEB_PORTAL_ENABLED);
        int rawPort = config.getInt("web-portal-port", DEFAULT_WEB_PORTAL_PORT);
        this.webPortalPort = (rawPort >= 1 && rawPort <= 65535) ? rawPort : DEFAULT_WEB_PORTAL_PORT;
        this.webPortalBindAddress = config.getString("web-portal-bind-address", DEFAULT_WEB_PORTAL_BIND);
        this.webPortalPublicUrl = config.getString("web-portal-public-url", "").trim();
    }

    /**
     * Safely parses a material string and verifies that it is a placeable block and item.
     */
    public static Material parseBlockMaterial(String name, Material defaultMaterial, String configKey, Logger logger) {
        if (name == null || name.isBlank()) {
            if (logger != null) {
                logger.warning("[Config] '" + configKey + "' is empty or missing. Using default: " + defaultMaterial.name());
            }
            return defaultMaterial;
        }

        Material mat = Material.matchMaterial(name.trim());
        if (mat == null) {
            if (logger != null) {
                logger.warning("[Config] Unknown material '" + name + "' for '" + configKey + "'. Using default: " + defaultMaterial.name());
            }
            return defaultMaterial;
        }

        if (mat.isAir() || !mat.isBlock()) {
            if (logger != null) {
                logger.warning("[Config] Material '" + mat.name() + "' configured for '" + configKey + "' is not a placeable block. Using default: " + defaultMaterial.name());
            }
            return defaultMaterial;
        }

        if (!mat.isItem()) {
            if (logger != null) {
                logger.warning("[Config] Material '" + mat.name() + "' configured for '" + configKey + "' cannot exist as an inventory item. Using default: " + defaultMaterial.name());
            }
            return defaultMaterial;
        }

        return mat;
    }

    public Material getComputerBlock() {
        return computerBlock;
    }

    public Material getAdvancedComputerBlock() {
        return advancedComputerBlock;
    }

    public Material getMonitorBlock() {
        return monitorBlock;
    }

    public Material getCrafterBlock() {
        return crafterBlock;
    }

    public Material getTransposerBlock() {
        return transposerBlock;
    }

    public Material getSpeakerBlock() {
        return speakerBlock;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public long getAdvancedTimeoutMs() {
        return advancedTimeoutMs;
    }

    public int getMaxStreamLines() {
        return maxStreamLines;
    }

    public long getPreventSpamDelayMs() {
        return preventSpamDelayMs;
    }

    public boolean isDropDisksOnBreak() {
        return dropDisksOnBreak;
    }

    public boolean isEnableComputerRecipe() {
        return enableComputerRecipe;
    }

    public int getMaxConcurrentPrograms() {
        return maxConcurrentPrograms;
    }

    public int getMaxInstructionsPerSlice() {
        return maxInstructionsPerSlice;
    }

    public Material getTurtleBlock() {
        return turtleBlock;
    }

    public int getTurtleBuildDelayTicks() {
        return turtleBuildDelayTicks;
    }

    public boolean isTurtleRequireMaterials() {
        return turtleRequireMaterials;
    }

    public boolean isTurtleFuelRequired() {
        return turtleFuelRequired;
    }

    public boolean isWebPortalEnabled() {
        return webPortalEnabled;
    }

    public int getWebPortalPort() {
        return webPortalPort;
    }

    public String getWebPortalBindAddress() {
        return webPortalBindAddress;
    }

    public String getWebPortalPublicUrl() {
        return webPortalPublicUrl;
    }
}

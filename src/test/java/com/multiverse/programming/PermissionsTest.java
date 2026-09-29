// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against drift between the permission nodes used in code and those declared in plugin.yml.
 *
 * <p>A permission node referenced in code but missing from plugin.yml silently evaluates to
 * {@code false} for regular (non-op) players. That exact mismatch previously made Turtles
 * impossible to open for normal players, so this test fails the build if it ever regresses.</p>
 */
class PermissionsTest {

    /** Matches permission string literals such as the node for the turtle feature. */
    private static final Pattern PERMISSION_LITERAL =
            Pattern.compile("\"(multiverseprogramming(?:\\.[a-z_]+)+)\"");

    private static final Path MAIN_SOURCES = Path.of("src", "main", "java");

    @Test
    @DisplayName("Every permission referenced in code is declared in plugin.yml")
    void everyReferencedPermissionIsDeclared() throws IOException {
        Set<String> declared = declaredPermissionKeys(loadPluginYml());
        Set<String> referenced = referencedPermissionsInSource();

        assertFalse(referenced.isEmpty(),
                "Expected at least one permission reference under " + MAIN_SOURCES);

        Set<String> missing = new TreeSet<>(referenced);
        missing.removeAll(declared);
        assertTrue(missing.isEmpty(),
                "Permissions used in code but NOT declared in plugin.yml: " + missing
                        + " (regular players would be denied because undeclared nodes default to false)");
    }

    @Test
    @DisplayName("Every declared permission has a default value")
    void everyDeclaredPermissionHasDefault() throws IOException {
        ConfigurationSection permissions = loadPluginYml().getConfigurationSection("permissions");
        assertNotNull(permissions, "plugin.yml must declare a permissions section");

        for (String node : permissions.getKeys(false)) {
            ConfigurationSection section = permissions.getConfigurationSection(node);
            assertNotNull(section, "Permission '" + node + "' must be a mapping with a description/default");
            assertTrue(section.contains("default"), "Permission '" + node + "' must declare a default");
        }
    }

    @Test
    @DisplayName("Command permissions and declared child nodes point to declared permissions")
    void commandPermissionsAndChildrenAreDeclared() throws IOException {
        YamlConfiguration yml = loadPluginYml();
        Set<String> declared = declaredPermissionKeys(yml);

        ConfigurationSection commands = yml.getConfigurationSection("commands");
        if (commands != null) {
            for (String command : commands.getKeys(false)) {
                ConfigurationSection section = commands.getConfigurationSection(command);
                if (section == null) continue;
                String permission = section.getString("permission");
                if (permission == null || permission.isBlank()) continue;
                assertTrue(declared.contains(permission),
                        "Command '/" + command + "' requires undeclared permission '" + permission + "'");
            }
        }

        ConfigurationSection permissions = yml.getConfigurationSection("permissions");
        if (permissions != null) {
            for (String node : permissions.getKeys(false)) {
                ConfigurationSection section = permissions.getConfigurationSection(node);
                if (section == null) continue;
                ConfigurationSection children = section.getConfigurationSection("children");
                if (children == null) continue;
                for (String child : children.getKeys(false)) {
                    assertTrue(declared.contains(child),
                            "Permission '" + node + "' declares child '" + child + "' which is not declared");
                }
            }
        }
    }

    /**
     * Loads plugin.yml with a NUL path separator so dotted permission nodes are kept as single
     * keys instead of being expanded into nested sections.
     */
    private static YamlConfiguration loadPluginYml() throws IOException {
        try (InputStream in = PermissionsTest.class.getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(in, "plugin.yml must be available on the test classpath (target/classes)");

            YamlConfiguration yml = new YamlConfiguration();
            yml.options().pathSeparator('\0');
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                yml.load(reader);
            } catch (InvalidConfigurationException e) {
                throw new IOException("plugin.yml could not be parsed", e);
            }
            return yml;
        }
    }

    private static Set<String> declaredPermissionKeys(YamlConfiguration yml) {
        Set<String> keys = new TreeSet<>();
        ConfigurationSection permissions = yml.getConfigurationSection("permissions");
        if (permissions != null) {
            keys.addAll(permissions.getKeys(false));
        }
        return keys;
    }

    private static Set<String> referencedPermissionsInSource() throws IOException {
        assertTrue(Files.isDirectory(MAIN_SOURCES),
                "Expected to find main sources at " + MAIN_SOURCES.toAbsolutePath());

        Set<String> found = new TreeSet<>();
        try (Stream<Path> paths = Files.walk(MAIN_SOURCES)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                Matcher matcher = PERMISSION_LITERAL.matcher(content);
                while (matcher.find()) {
                    found.add(matcher.group(1));
                }
            }
        }
        return found;
    }
}

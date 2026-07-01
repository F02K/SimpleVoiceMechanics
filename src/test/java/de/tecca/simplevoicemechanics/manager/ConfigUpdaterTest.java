package de.tecca.simplevoicemechanics.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigUpdaterTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-01T12:34:56Z"),
            ZoneOffset.UTC
    );
    private static final Logger LOGGER = Logger.getLogger(ConfigUpdaterTest.class.getName());

    @TempDir
    Path tempDir;

    @Test
    void createsMissingConfigFromBundledDefault() throws Exception {
        String defaults = """
                detection:
                  max-range: 16.0
                """;

        ConfigUpdateResult result = update(defaults);

        assertTrue(result.isCreated());
        assertFalse(result.isUpdated());
        assertEquals(defaults, Files.readString(configPath()));
    }

    @Test
    void preservesExistingValuesAndAddsMissingDefaults() throws Exception {
        writeConfig("""
                detection:
                  max-range: 24.0
                """);

        ConfigUpdateResult result = update("""
                detection:
                  max-range: 16.0
                  min-range: 2.0
                """);
        YamlConfiguration config = loadWrittenConfig();

        assertTrue(result.isUpdated());
        assertNotNull(result.getBackupPath());
        assertEquals(24.0, config.getDouble("detection.max-range"));
        assertEquals(2.0, config.getDouble("detection.min-range"));
        assertEquals(1, result.getAddedDefaults());
        assertEquals(1, result.getPreservedValues());
        assertEquals(1, backupCount());
    }

    @Test
    void preservesExplicitNullValues() throws Exception {
        writeConfig("""
                mob-hearing:
                  hostile-mobs:
                    max-range: null
                """);

        update("""
                mob-hearing:
                  hostile-mobs:
                    max-range: null
                    min-range: 2.0
                """);
        YamlConfiguration config = loadWrittenConfig();

        assertTrue(config.getKeys(true).contains("mob-hearing.hostile-mobs.max-range"));
        assertEquals("null", config.getString("mob-hearing.hostile-mobs.max-range"));
        assertEquals(2.0, config.getDouble("mob-hearing.hostile-mobs.min-range"));
    }

    @Test
    void movesUnknownKeysToPreservedSection() throws Exception {
        writeConfig("""
                detection:
                  max-range: 20.0
                old-feature:
                  enabled: true
                """);

        ConfigUpdateResult result = update("""
                detection:
                  max-range: 16.0
                """);
        YamlConfiguration config = loadWrittenConfig();

        assertTrue(result.isUpdated());
        assertEquals(1, result.getPreservedUnknownSettings());
        assertFalse(config.getKeys(true).contains("old-feature.enabled"));
        assertTrue(config.getBoolean("preserved-unknown-settings.old-feature.enabled"));
    }

    @Test
    void doesNotCreateAnotherBackupWhenAlreadyUpdated() throws Exception {
        String defaults = """
                detection:
                  max-range: 16.0
                  min-range: 2.0
                """;
        writeConfig("""
                detection:
                  max-range: 20.0
                """);

        ConfigUpdateResult firstResult = update(defaults);
        ConfigUpdateResult secondResult = update(defaults);

        assertTrue(firstResult.isUpdated());
        assertFalse(secondResult.isUpdated());
        assertEquals(1, backupCount());
    }

    @Test
    void keepsAlreadyPreservedUnknownKeys() throws Exception {
        writeConfig("""
                detection:
                  max-range: 20.0
                preserved-unknown-settings:
                  old-feature:
                    enabled: true
                """);

        update("""
                detection:
                  max-range: 16.0
                  min-range: 2.0
                """);
        YamlConfiguration config = loadWrittenConfig();

        assertTrue(config.getBoolean("preserved-unknown-settings.old-feature.enabled"));
        assertEquals(2.0, config.getDouble("detection.min-range"));
    }

    @Test
    void doesNotOverwriteInvalidYaml() throws Exception {
        String invalidConfig = "detection: [unclosed";
        writeConfig(invalidConfig);

        ConfigUpdateResult result = update("""
                detection:
                  max-range: 16.0
                """);

        assertTrue(result.isFailed());
        assertEquals(invalidConfig, Files.readString(configPath()));
        assertEquals(0, backupCount());
    }

    private ConfigUpdateResult update(String defaultConfig) {
        return ConfigUpdater.update(
                tempDir.toFile(),
                new ByteArrayInputStream(defaultConfig.getBytes(StandardCharsets.UTF_8)),
                LOGGER,
                FIXED_CLOCK
        );
    }

    private Path configPath() {
        return tempDir.resolve(ConfigUpdater.CONFIG_FILE_NAME);
    }

    private void writeConfig(String content) throws IOException {
        Files.writeString(configPath(), content, StandardCharsets.UTF_8);
    }

    private YamlConfiguration loadWrittenConfig() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.load(configPath().toFile());
        return config;
    }

    private long backupCount() throws IOException {
        try (Stream<Path> files = Files.list(tempDir)) {
            return files
                    .filter(path -> path.getFileName().toString().startsWith("config.yml.backup-"))
                    .count();
        }
    }
}

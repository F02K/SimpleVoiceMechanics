package de.tecca.simplevoicemechanics.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Keeps an existing server config aligned with the bundled default config.
 */
public final class ConfigUpdater {

    public static final String CONFIG_FILE_NAME = "config.yml";
    public static final String PRESERVED_UNKNOWN_SETTINGS = "preserved-unknown-settings";

    private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Pattern EXPLICIT_NULL_LINE = Pattern.compile(
            "(?m)^(\\s*[^\\s#][^#\\r\\n]*:\\s*)null(\\s*(?:#.*)?)$"
    );

    private ConfigUpdater() {
    }

    public static ConfigUpdateResult update(File dataFolder, InputStream defaultConfigStream, Logger logger) {
        return update(dataFolder, defaultConfigStream, logger, Clock.systemDefaultZone());
    }

    static ConfigUpdateResult update(File dataFolder, InputStream defaultConfigStream, Logger logger, Clock clock) {
        if (defaultConfigStream == null) {
            String message = "Bundled config.yml was not found in plugin resources.";
            log(logger, Level.WARNING, message);
            return ConfigUpdateResult.failed(message);
        }

        Path dataPath = dataFolder.toPath();
        Path configPath = dataPath.resolve(CONFIG_FILE_NAME);

        try (InputStream stream = defaultConfigStream) {
            byte[] defaultBytes = stream.readAllBytes();
            Files.createDirectories(dataPath);

            if (!Files.exists(configPath)) {
                Files.write(configPath, defaultBytes);
                log(logger, Level.INFO, "Created default config.yml");
                return ConfigUpdateResult.created();
            }

            YamlConfiguration existingConfig = loadConfig(configPath);
            YamlConfiguration defaultConfig = loadConfig(defaultBytes);

            MergeStats stats = mergeServerValues(existingConfig, defaultConfig);
            String updatedConfig = defaultConfig.saveToString();
            String currentConfig = Files.readString(configPath, StandardCharsets.UTF_8);

            if (updatedConfig.equals(currentConfig)) {
                return ConfigUpdateResult.unchanged(stats.preservedValues);
            }

            Path backupPath = createBackup(configPath, clock);
            Files.writeString(configPath, updatedConfig, StandardCharsets.UTF_8);

            ConfigUpdateResult result = ConfigUpdateResult.updated(
                    stats.addedDefaults,
                    stats.preservedValues,
                    stats.preservedUnknownSettings,
                    backupPath
            );
            logUpdate(logger, result);
            return result;
        } catch (IOException | InvalidConfigurationException e) {
            String message = "Could not update config.yml automatically: " + e.getMessage();
            log(logger, Level.WARNING, message);
            return ConfigUpdateResult.failed(message);
        }
    }

    private static YamlConfiguration loadConfig(Path configPath)
            throws IOException, InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.options().parseComments(true);
        config.loadFromString(normalizeExplicitNulls(Files.readString(configPath, StandardCharsets.UTF_8)));
        return config;
    }

    private static YamlConfiguration loadConfig(byte[] content) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.options().parseComments(true);
        config.loadFromString(normalizeExplicitNulls(new String(content, StandardCharsets.UTF_8)));
        return config;
    }

    private static String normalizeExplicitNulls(String yaml) {
        return EXPLICIT_NULL_LINE.matcher(yaml).replaceAll("$1\"null\"$2");
    }

    private static MergeStats mergeServerValues(YamlConfiguration existingConfig,
                                                YamlConfiguration defaultConfig) {
        Set<String> defaultLeafPaths = leafPaths(defaultConfig);
        Set<String> existingLeafPaths = leafPaths(existingConfig);
        MergeStats stats = new MergeStats();

        for (String path : defaultLeafPaths) {
            if (existingLeafPaths.contains(path)) {
                copyMatchingDefaultValue(existingConfig, defaultConfig, path);
                stats.preservedValues++;
            } else {
                stats.addedDefaults++;
            }
        }

        for (String path : existingLeafPaths) {
            if (path.equals(PRESERVED_UNKNOWN_SETTINGS)) {
                continue;
            }
            if (path.startsWith(PRESERVED_UNKNOWN_SETTINGS + ".")) {
                copyUnknownValue(existingConfig, defaultConfig, path, path);
                continue;
            }
            if (!defaultLeafPaths.contains(path)) {
                copyUnknownValue(existingConfig, defaultConfig, path, PRESERVED_UNKNOWN_SETTINGS + "." + path);
                stats.preservedUnknownSettings++;
            }
        }

        return stats;
    }

    private static void copyMatchingDefaultValue(YamlConfiguration source, YamlConfiguration target, String path) {
        Object sourceValue = source.get(path);
        target.set(path, sourceValue != null ? sourceValue : "null");
    }

    private static void copyUnknownValue(YamlConfiguration source, YamlConfiguration target,
                                         String sourcePath, String targetPath) {
        Object sourceValue = source.get(sourcePath);
        target.set(targetPath, sourceValue != null ? sourceValue : "null");
    }

    private static Set<String> leafPaths(ConfigurationSection section) {
        Set<String> paths = new LinkedHashSet<>();
        for (String path : section.getKeys(true)) {
            if (!section.isConfigurationSection(path)) {
                paths.add(path);
            }
        }
        return paths;
    }

    private static Path createBackup(Path configPath, Clock clock) throws IOException {
        String timestamp = LocalDateTime.now(clock).format(BACKUP_TIMESTAMP_FORMAT);
        Path backupPath = configPath.resolveSibling(CONFIG_FILE_NAME + ".backup-" + timestamp);

        int duplicate = 1;
        while (Files.exists(backupPath)) {
            backupPath = configPath.resolveSibling(CONFIG_FILE_NAME + ".backup-" + timestamp + "-" + duplicate);
            duplicate++;
        }

        Files.copy(configPath, backupPath, StandardCopyOption.COPY_ATTRIBUTES);
        return backupPath;
    }

    private static void logUpdate(Logger logger, ConfigUpdateResult result) {
        if (logger == null || result.getBackupPath() == null) {
            return;
        }

        logger.info(String.format(
                "Updated config.yml with %d missing default setting(s), preserved %d existing setting(s), " +
                        "moved %d unknown setting(s) to %s. Backup: %s",
                result.getAddedDefaults(),
                result.getPreservedValues(),
                result.getPreservedUnknownSettings(),
                PRESERVED_UNKNOWN_SETTINGS,
                result.getBackupPath().getFileName()
        ));
    }

    private static void log(Logger logger, Level level, String message) {
        if (logger != null) {
            logger.log(level, message);
        }
    }

    private static final class MergeStats {
        private int addedDefaults;
        private int preservedValues;
        private int preservedUnknownSettings;
    }
}

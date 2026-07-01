package de.tecca.simplevoicemechanics.manager;

import java.nio.file.Path;

/**
 * Result of a server config update check.
 */
public class ConfigUpdateResult {

    private final boolean updated;
    private final boolean created;
    private final boolean failed;
    private final int addedDefaults;
    private final int preservedValues;
    private final int preservedUnknownSettings;
    private final Path backupPath;
    private final String errorMessage;

    ConfigUpdateResult(boolean updated, boolean created, boolean failed, int addedDefaults,
                       int preservedValues, int preservedUnknownSettings, Path backupPath,
                       String errorMessage) {
        this.updated = updated;
        this.created = created;
        this.failed = failed;
        this.addedDefaults = addedDefaults;
        this.preservedValues = preservedValues;
        this.preservedUnknownSettings = preservedUnknownSettings;
        this.backupPath = backupPath;
        this.errorMessage = errorMessage;
    }

    public static ConfigUpdateResult created() {
        return new ConfigUpdateResult(false, true, false, 0, 0, 0, null, null);
    }

    public static ConfigUpdateResult unchanged(int preservedValues) {
        return new ConfigUpdateResult(false, false, false, 0, preservedValues, 0, null, null);
    }

    public static ConfigUpdateResult updated(int addedDefaults, int preservedValues,
                                             int preservedUnknownSettings, Path backupPath) {
        return new ConfigUpdateResult(true, false, false, addedDefaults, preservedValues,
                preservedUnknownSettings, backupPath, null);
    }

    public static ConfigUpdateResult failed(String errorMessage) {
        return new ConfigUpdateResult(false, false, true, 0, 0, 0, null, errorMessage);
    }

    public boolean isUpdated() {
        return updated;
    }

    public boolean isCreated() {
        return created;
    }

    public boolean isFailed() {
        return failed;
    }

    public int getAddedDefaults() {
        return addedDefaults;
    }

    public int getPreservedValues() {
        return preservedValues;
    }

    public int getPreservedUnknownSettings() {
        return preservedUnknownSettings;
    }

    public Path getBackupPath() {
        return backupPath;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}

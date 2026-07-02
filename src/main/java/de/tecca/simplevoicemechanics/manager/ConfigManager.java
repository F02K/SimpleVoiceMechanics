package de.tecca.simplevoicemechanics.manager;

import de.tecca.simplevoicemechanics.SimpleVoiceMechanics;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages plugin configuration with range-based detection system.
 *
 * <p>Handles:
 * <ul>
 *   <li>Global detection settings (max-range, min-range, falloff-curve)</li>
 *   <li>Per-category overrides (hostile, neutral, peaceful, warden, sculk)</li>
 *   <li>Per-category volume thresholds in decibels</li>
 *   <li>Mob blacklists</li>
 *   <li>Peaceful mob behaviors (look-at, follow-when-sneaking)</li>
 *   <li>Environmental modifiers (biome and time)</li>
 * </ul>
 *
 * @author Tecca
 * @version 1.3.0
 */
public class ConfigManager {

    // Configuration paths
    private static final String PATH_DETECTION = "detection";
    private static final String PATH_MOB_HEARING = "mob-hearing";
    private static final String PATH_SCULK = "sculk-hearing";
    private static final String PATH_ENVIRONMENTAL = "environmental-modifiers";

    private final SimpleVoiceMechanics plugin;
    private FileConfiguration config;

    // Global detection settings
    private double defaultMaxRange;
    private double defaultMinRange;
    private double defaultFalloffCurve;

    // Mob hearing settings
    private boolean mobHearingEnabled;

    // Hostile mobs
    private boolean hostileMobsEnabled;
    private Double hostileMaxRange;
    private Double hostileMinRange;
    private Double hostileFalloffCurve;
    private double hostileVolumeThresholdDb;
    private Set<EntityType> hostileBlacklist;
    private boolean invisiblePlayerInvestigationEnabled;
    private double invisiblePlayerMovementTolerance;
    private int invisiblePlayerInvestigationTimeout;
    private boolean invisiblePlayerAttackIfStillThere;

    // Neutral mobs
    private boolean neutralMobsEnabled;
    private boolean neutralLookAtPlayer;
    private Double neutralMaxRange;
    private Double neutralMinRange;
    private Double neutralFalloffCurve;
    private double neutralVolumeThresholdDb;
    private double neutralReactionChance;
    private int neutralLookDuration;
    private int neutralReactionCooldown;
    private Set<EntityType> neutralBlacklist;

    // Peaceful mobs
    private boolean peacefulMobsEnabled;
    private boolean peacefulLookAtPlayer;
    private Double peacefulMaxRange;
    private Double peacefulMinRange;
    private Double peacefulFalloffCurve;
    private double peacefulVolumeThresholdDb;
    private double peacefulReactionChance;
    private int peacefulLookDuration;
    private int peacefulReactionCooldown;
    private Set<EntityType> peacefulBlacklist;
    // Flee behavior
    private boolean fleeEnabled;
    private double fleeVolumeDb;
    private double fleeDistance;
    private int fleeDuration;
    // Follow behavior
    private boolean followWhenSneakingEnabled;
    private boolean requireTemptItem;
    private boolean requireEyeContact;
    private double eyeContactRange;
    private int eyeContactMemory;
    private int followDuration;
    private double followMaxDistance;

    // Warden
    private boolean wardenEnabled;
    private Double wardenMaxRange;
    private Double wardenMinRange;
    private Double wardenFalloffCurve;
    private double wardenVolumeThresholdDb;

    // Sculk sensors
    private boolean sculkEnabled;
    private Double sculkMinRange;
    private Double sculkFalloffCurve;
    private double sculkVolumeThresholdDb;
    private double sculkRangeOffset;
    private long sculkCooldown;
    private boolean sculkVibrationParticle;
    private int sculkVibrationMinArrivalTicks;
    private int sculkVibrationMaxArrivalTicks;

    // Environmental modifiers
    private boolean environmentalModifiersEnabled;
    private boolean biomeModifiersEnabled;
    private boolean timeModifiersEnabled;
    private boolean weatherModifiersEnabled;
    private double rainRangeMultiplier;
    private double rainThresholdAdjustmentDb;
    private double thunderRangeMultiplier;
    private double thunderThresholdAdjustmentDb;

    // Acoustic modifiers
    private boolean sneakingReductionEnabled;
    private double sneakingDecibelReduction;
    private boolean obstructionMufflingEnabled;
    private double obstructionMaxDbReduction;
    private double obstructionWoolDbReduction;
    private double obstructionSolidBlockDbReduction;

    // Mob group alerts
    private boolean mobGroupAlertEnabled;
    private int maxMobAlerts;
    private boolean groupAlertOnlyAfterTargeting;

    // Hostile investigation
    private boolean hostileInvestigationEnabled;
    private int hostileInvestigationMemorySeconds;
    private int hostileInvestigationDetectionsToTarget;

    // Sculk performance
    private int sculkMaxScanRadius;

    // Debug settings
    private boolean audioLoggingEnabled;
    private boolean rangeLoggingEnabled;
    private boolean detectionLoggingEnabled;
    private boolean wardenLoggingEnabled;
    private boolean sculkLoggingEnabled;
    private boolean peacefulLoggingEnabled;
    private boolean environmentalLoggingEnabled;
    private boolean groupAlertLoggingEnabled;

    public ConfigManager(SimpleVoiceMechanics plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads configuration from disk.
     */
    public void loadConfig() {
        ConfigUpdater.update(plugin.getDataFolder(), plugin.getResource("config.yml"), plugin.getLogger());
        plugin.reloadConfig();
        config = plugin.getConfig();

        loadGlobalSettings();
        loadMobHearingSettings();
        loadSculkSettings();
        loadEnvironmentalSettings();
        loadDebugSettings();
    }

    /**
     * Loads global detection settings.
     */
    private void loadGlobalSettings() {
        defaultMaxRange = readNonNegativeDouble(PATH_DETECTION + ".max-range", 16.0);
        defaultMinRange = readNonNegativeDouble(PATH_DETECTION + ".min-range", 2.0);
        defaultMinRange = Math.min(defaultMinRange, defaultMaxRange);
        defaultFalloffCurve = readClampedDouble(PATH_DETECTION + ".falloff-curve", 1.0, 0.0, 2.0);

        String sneakingPath = PATH_DETECTION + ".sneaking-reduction";
        sneakingReductionEnabled = config.getBoolean(sneakingPath + ".enabled", true);
        sneakingDecibelReduction = readNonNegativeDouble(sneakingPath + ".decibel-reduction", 6.0);

        String obstructionPath = PATH_DETECTION + ".obstruction-muffling";
        obstructionMufflingEnabled = config.getBoolean(obstructionPath + ".enabled", true);
        obstructionMaxDbReduction = readNonNegativeDouble(obstructionPath + ".max-db-reduction", 18.0);
        obstructionWoolDbReduction = readNonNegativeDouble(obstructionPath + ".wool-db-reduction", 12.0);
        obstructionSolidBlockDbReduction = readNonNegativeDouble(obstructionPath + ".solid-block-db-reduction", 4.0);
    }

    /**
     * Loads all mob hearing settings.
     */
    private void loadMobHearingSettings() {
        mobHearingEnabled = config.getBoolean(PATH_MOB_HEARING + ".enabled", true);

        // Hostile mobs
        loadHostileMobSettings();

        // Neutral mobs
        loadNeutralMobSettings();

        // Peaceful mobs
        loadPeacefulMobSettings();

        // Warden
        loadWardenSettings();
    }

    /**
     * Loads hostile mob settings.
     */
    private void loadHostileMobSettings() {
        String path = PATH_MOB_HEARING + ".hostile-mobs";

        hostileMobsEnabled = config.getBoolean(path + ".enabled", true);
        hostileVolumeThresholdDb = readClampedDouble(path + ".volume-threshold-db", -40.0, -127.0, 0.0);
        hostileMaxRange = getOptionalNonNegativeDouble(path + ".max-range");
        hostileMinRange = getOptionalNonNegativeDouble(path + ".min-range");
        hostileFalloffCurve = getOptionalClampedDouble(path + ".falloff-curve", 0.0, 2.0);
        hostileBlacklist = loadBlacklist(path + ".blacklist");

        String invisiblePath = path + ".invisible-players";
        invisiblePlayerInvestigationEnabled = config.getBoolean(invisiblePath + ".enabled", true);
        invisiblePlayerMovementTolerance = readNonNegativeDouble(invisiblePath + ".movement-tolerance", 1.5);
        invisiblePlayerInvestigationTimeout = readNonNegativeInt(invisiblePath + ".investigation-timeout", 8);
        invisiblePlayerAttackIfStillThere = config.getBoolean(invisiblePath + ".attack-if-still-there", true);

        String investigationPath = path + ".investigation";
        hostileInvestigationEnabled = config.getBoolean(investigationPath + ".enabled", true);
        hostileInvestigationMemorySeconds = readNonNegativeInt(investigationPath + ".memory-seconds", 10);
        hostileInvestigationDetectionsToTarget = Math.max(
                1,
                readNonNegativeInt(investigationPath + ".detections-to-target", 2)
        );

        String groupAlertPath = path + ".group-alert";
        mobGroupAlertEnabled = config.getBoolean(groupAlertPath + ".enabled", true);
        maxMobAlerts = readNonNegativeInt(groupAlertPath + ".max-alerts", 5);
        groupAlertOnlyAfterTargeting = config.getBoolean(groupAlertPath + ".only-after-targeting", true);
    }

    /**
     * Loads neutral mob settings.
     */
    private void loadNeutralMobSettings() {
        String path = PATH_MOB_HEARING + ".neutral-mobs";

        neutralMobsEnabled = config.getBoolean(path + ".enabled", true);
        neutralLookAtPlayer = config.getBoolean(path + ".look-at-player", true);
        neutralVolumeThresholdDb = readClampedDouble(path + ".volume-threshold-db", -35.0, -127.0, 0.0);
        neutralReactionChance = readClampedDouble(path + ".natural-behavior.reaction-chance", 0.6, 0.0, 1.0);
        neutralLookDuration = readNonNegativeInt(path + ".natural-behavior.look-duration", 8);
        neutralReactionCooldown = readNonNegativeInt(path + ".natural-behavior.reaction-cooldown", 3);
        neutralMaxRange = getOptionalNonNegativeDouble(path + ".max-range");
        neutralMinRange = getOptionalNonNegativeDouble(path + ".min-range");
        neutralFalloffCurve = getOptionalClampedDouble(path + ".falloff-curve", 0.0, 2.0);
        neutralBlacklist = loadBlacklist(path + ".blacklist");
    }

    /**
     * Loads peaceful mob settings.
     */
    private void loadPeacefulMobSettings() {
        String path = PATH_MOB_HEARING + ".peaceful-mobs";

        peacefulMobsEnabled = config.getBoolean(path + ".enabled", true);
        peacefulLookAtPlayer = config.getBoolean(path + ".look-at-player", true);
        peacefulVolumeThresholdDb = readClampedDouble(path + ".volume-threshold-db", -30.0, -127.0, 0.0);
        peacefulReactionChance = readClampedDouble(path + ".natural-behavior.reaction-chance", 0.7, 0.0, 1.0);
        peacefulLookDuration = readNonNegativeInt(path + ".natural-behavior.look-duration", 10);
        peacefulReactionCooldown = readNonNegativeInt(path + ".natural-behavior.reaction-cooldown", 3);
        peacefulMaxRange = getOptionalNonNegativeDouble(path + ".max-range");
        peacefulMinRange = getOptionalNonNegativeDouble(path + ".min-range");
        peacefulFalloffCurve = getOptionalClampedDouble(path + ".falloff-curve", 0.0, 2.0);
        peacefulBlacklist = loadBlacklist(path + ".blacklist");

        // Flee behavior (all in seconds from config)
        String fleePath = path + ".flee-behavior";
        fleeEnabled = config.getBoolean(fleePath + ".enabled", true);
        fleeVolumeDb = readClampedDouble(fleePath + ".flee-volume-db", -20.0, -127.0, 0.0);
        fleeDistance = readNonNegativeDouble(fleePath + ".flee-distance", 3.0);
        fleeDuration = readNonNegativeInt(fleePath + ".flee-duration", 3);  // seconds

        // Follow when sneaking (all in seconds from config)
        String followPath = path + ".follow-when-sneaking";
        followWhenSneakingEnabled = config.getBoolean(followPath + ".enabled", true);
        requireTemptItem = config.getBoolean(followPath + ".require-tempt-item", true);
        requireEyeContact = config.getBoolean(followPath + ".require-eye-contact", true);
        eyeContactRange = readNonNegativeDouble(followPath + ".eye-contact-range", 4.0);
        eyeContactMemory = readNonNegativeInt(followPath + ".eye-contact-memory", 5);  // seconds
        followDuration = readNonNegativeInt(followPath + ".duration", 60);  // seconds
        followMaxDistance = readNonNegativeDouble(followPath + ".max-distance", 12.0);
    }

    /**
     * Loads warden settings.
     */
    private void loadWardenSettings() {
        String path = PATH_MOB_HEARING + ".warden";

        wardenEnabled = config.getBoolean(path + ".enabled", true);
        wardenVolumeThresholdDb = readClampedDouble(path + ".volume-threshold-db", -50.0, -127.0, 0.0);
        wardenMaxRange = getOptionalNonNegativeDouble(path + ".max-range");
        wardenMinRange = getOptionalNonNegativeDouble(path + ".min-range");
        wardenFalloffCurve = getOptionalClampedDouble(path + ".falloff-curve", 0.0, 2.0);
    }

    /**
     * Loads sculk sensor settings.
     */
    private void loadSculkSettings() {
        sculkEnabled = config.getBoolean(PATH_SCULK + ".enabled", true);
        sculkVolumeThresholdDb = readClampedDouble(PATH_SCULK + ".volume-threshold-db", -20.0, -127.0, 0.0);
        sculkRangeOffset = config.getDouble(PATH_SCULK + ".range-offset", 0.0);
        sculkMinRange = getOptionalNonNegativeDouble(PATH_SCULK + ".min-range");
        sculkFalloffCurve = getOptionalClampedDouble(PATH_SCULK + ".falloff-curve", 0.0, 2.0);
        sculkCooldown = readNonNegativeLong(PATH_SCULK + ".cooldown", 1000);
        sculkMaxScanRadius = readNonNegativeInt(PATH_SCULK + ".max-scan-radius", 64);

        String visualsPath = PATH_SCULK + ".visuals";
        sculkVibrationParticle = config.getBoolean(visualsPath + ".vibration-particle", true);
        sculkVibrationMinArrivalTicks = readNonNegativeInt(visualsPath + ".min-arrival-ticks", 5);
        sculkVibrationMaxArrivalTicks = Math.max(
                sculkVibrationMinArrivalTicks,
                readNonNegativeInt(visualsPath + ".max-arrival-ticks", 40)
        );
    }

    /**
     * Loads environmental modifier settings.
     */
    private void loadEnvironmentalSettings() {
        environmentalModifiersEnabled = config.getBoolean(PATH_ENVIRONMENTAL + ".enabled", true);
        biomeModifiersEnabled = config.getBoolean(PATH_ENVIRONMENTAL + ".biome-modifiers.enabled", true);
        timeModifiersEnabled = config.getBoolean(PATH_ENVIRONMENTAL + ".time-modifiers.enabled", true);

        String weatherPath = PATH_ENVIRONMENTAL + ".weather-modifiers";
        weatherModifiersEnabled = config.getBoolean(weatherPath + ".enabled", true);
        rainRangeMultiplier = readNonNegativeDouble(weatherPath + ".rain.range-multiplier", 0.85);
        rainThresholdAdjustmentDb = config.getDouble(weatherPath + ".rain.threshold-adjustment-db", 3.0);
        thunderRangeMultiplier = readNonNegativeDouble(weatherPath + ".thunder.range-multiplier", 0.70);
        thunderThresholdAdjustmentDb = config.getDouble(weatherPath + ".thunder.threshold-adjustment-db", 6.0);
    }

    /**
     * Loads debug logging flags.
     */
    private void loadDebugSettings() {
        audioLoggingEnabled = config.getBoolean("debug.audio-logging", false);
        rangeLoggingEnabled = config.getBoolean("debug.range-logging", false);
        detectionLoggingEnabled = config.getBoolean("debug.detection-logging", false);
        wardenLoggingEnabled = config.getBoolean("debug.warden-logging", false);
        sculkLoggingEnabled = config.getBoolean("debug.sculk-logging", false);
        peacefulLoggingEnabled = config.getBoolean("debug.peaceful-logging", false);
        environmentalLoggingEnabled = config.getBoolean("debug.environmental-logging", false);
        groupAlertLoggingEnabled = config.getBoolean("debug.group-alert-logging", false);
    }

    /**
     * Gets optional double from config (returns null if not set or null).
     */
    private Double getOptionalNonNegativeDouble(String path) {
        if (!config.isSet(path) || config.getString(path) == null ||
                config.getString(path).equalsIgnoreCase("null")) {
            return null;
        }
        return Math.max(0.0, config.getDouble(path));
    }

    private Double getOptionalClampedDouble(String path, double min, double max) {
        if (!config.isSet(path) || config.getString(path) == null ||
                config.getString(path).equalsIgnoreCase("null")) {
            return null;
        }
        return clamp(config.getDouble(path), min, max);
    }

    private double readNonNegativeDouble(String path, double defaultValue) {
        return Math.max(0.0, config.getDouble(path, defaultValue));
    }

    private int readNonNegativeInt(String path, int defaultValue) {
        return Math.max(0, config.getInt(path, defaultValue));
    }

    private long readNonNegativeLong(String path, long defaultValue) {
        return Math.max(0L, config.getLong(path, defaultValue));
    }

    private double readClampedDouble(String path, double defaultValue, double min, double max) {
        return clamp(config.getDouble(path, defaultValue), min, max);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Loads entity type blacklist from config.
     */
    private Set<EntityType> loadBlacklist(String path) {
        Set<EntityType> blacklist = new HashSet<>();
        List<String> list = config.getStringList(path);

        for (String typeName : list) {
            try {
                EntityType type = EntityType.valueOf(typeName.toUpperCase());
                blacklist.add(type);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid entity type in blacklist: " + typeName);
            }
        }

        return blacklist;
    }

    /**
     * Reloads configuration.
     */
    public void reload() {
        loadConfig();
    }

    // ==================== GETTERS ====================

    // Global settings
    public double getDefaultMaxRange() { return defaultMaxRange; }
    public double getDefaultMinRange() { return defaultMinRange; }
    public double getDefaultFalloffCurve() { return defaultFalloffCurve; }
    public boolean isSneakingReductionEnabled() { return sneakingReductionEnabled; }
    public double getSneakingDecibelReduction() { return sneakingDecibelReduction; }
    public boolean isObstructionMufflingEnabled() { return obstructionMufflingEnabled; }
    public double getObstructionMaxDbReduction() { return obstructionMaxDbReduction; }
    public double getObstructionWoolDbReduction() { return obstructionWoolDbReduction; }
    public double getObstructionSolidBlockDbReduction() { return obstructionSolidBlockDbReduction; }

    // Mob hearing
    public boolean isMobHearingEnabled() { return mobHearingEnabled; }

    // Hostile mobs
    public boolean isHostileMobsEnabled() { return hostileMobsEnabled; }
    public double getHostileMaxRange() { return hostileMaxRange != null ? hostileMaxRange : defaultMaxRange; }
    public double getHostileMinRange() { return Math.min(hostileMinRange != null ? hostileMinRange : defaultMinRange, getHostileMaxRange()); }
    public double getHostileFalloffCurve() { return hostileFalloffCurve != null ? hostileFalloffCurve : defaultFalloffCurve; }
    public double getHostileVolumeThresholdDb() { return hostileVolumeThresholdDb; }
    public boolean isHostileBlacklisted(EntityType type) { return hostileBlacklist.contains(type); }
    public boolean isInvisiblePlayerInvestigationEnabled() { return invisiblePlayerInvestigationEnabled; }
    public double getInvisiblePlayerMovementTolerance() { return invisiblePlayerMovementTolerance; }
    public int getInvisiblePlayerInvestigationTimeout() { return invisiblePlayerInvestigationTimeout; }
    public boolean shouldInvisiblePlayerAttackIfStillThere() { return invisiblePlayerAttackIfStillThere; }
    public boolean isHostileInvestigationEnabled() { return hostileInvestigationEnabled; }
    public int getHostileInvestigationMemorySeconds() { return hostileInvestigationMemorySeconds; }
    public int getHostileInvestigationDetectionsToTarget() { return hostileInvestigationDetectionsToTarget; }

    // Neutral mobs
    public boolean isNeutralMobsEnabled() { return neutralMobsEnabled; }
    public boolean shouldNeutralLookAtPlayer() { return neutralLookAtPlayer; }
    public double getNeutralMaxRange() { return neutralMaxRange != null ? neutralMaxRange : defaultMaxRange; }
    public double getNeutralMinRange() { return Math.min(neutralMinRange != null ? neutralMinRange : defaultMinRange, getNeutralMaxRange()); }
    public double getNeutralFalloffCurve() { return neutralFalloffCurve != null ? neutralFalloffCurve : defaultFalloffCurve; }
    public double getNeutralVolumeThresholdDb() { return neutralVolumeThresholdDb; }
    public double getNeutralReactionChance() { return neutralReactionChance; }
    public int getNeutralLookDurationTicks() { return neutralLookDuration * 20; }  // seconds to ticks
    public long getNeutralReactionCooldownMs() { return neutralReactionCooldown * 1000L; }  // seconds to ms
    public boolean isNeutralBlacklisted(EntityType type) { return neutralBlacklist.contains(type); }

    // Peaceful mobs
    public boolean isPeacefulMobsEnabled() { return peacefulMobsEnabled; }
    public boolean shouldPeacefulLookAtPlayer() { return peacefulLookAtPlayer; }
    public double getPeacefulMaxRange() { return peacefulMaxRange != null ? peacefulMaxRange : defaultMaxRange; }
    public double getPeacefulMinRange() { return Math.min(peacefulMinRange != null ? peacefulMinRange : defaultMinRange, getPeacefulMaxRange()); }
    public double getPeacefulFalloffCurve() { return peacefulFalloffCurve != null ? peacefulFalloffCurve : defaultFalloffCurve; }
    public double getPeacefulVolumeThresholdDb() { return peacefulVolumeThresholdDb; }
    public double getPeacefulReactionChance() { return peacefulReactionChance; }
    public int getPeacefulLookDurationTicks() { return peacefulLookDuration * 20; }  // seconds to ticks
    public long getPeacefulReactionCooldownMs() { return peacefulReactionCooldown * 1000L; }  // seconds to ms
    public boolean isPeacefulBlacklisted(EntityType type) { return peacefulBlacklist.contains(type); }
    // Flee behavior
    public boolean isFleeEnabled() { return fleeEnabled; }
    public double getFleeVolumeDb() { return fleeVolumeDb; }
    public double getFleeDistance() { return fleeDistance; }
    public int getFleeDurationTicks() { return fleeDuration * 20; }  // seconds to ticks
    // Follow behavior
    public boolean isFollowWhenSneakingEnabled() { return followWhenSneakingEnabled; }
    public boolean requiresTemptItem() { return requireTemptItem; }
    public boolean requiresEyeContact() { return requireEyeContact; }
    public double getEyeContactRange() { return eyeContactRange; }
    public long getEyeContactMemoryMs() { return eyeContactMemory * 1000L; }  // seconds to ms
    public int getFollowDuration() { return followDuration; }  // already in seconds
    public double getFollowMaxDistance() { return followMaxDistance; }

    // Warden
    public boolean isWardenEnabled() { return wardenEnabled; }
    public double getWardenMaxRange() { return wardenMaxRange != null ? wardenMaxRange : defaultMaxRange; }
    public double getWardenMinRange() { return Math.min(wardenMinRange != null ? wardenMinRange : defaultMinRange, getWardenMaxRange()); }
    public double getWardenFalloffCurve() { return wardenFalloffCurve != null ? wardenFalloffCurve : defaultFalloffCurve; }
    public double getWardenVolumeThresholdDb() { return wardenVolumeThresholdDb; }

    // Sculk
    public boolean isSculkEnabled() { return sculkEnabled; }
    public double getSculkRangeOffset() { return sculkRangeOffset; }
    public double getSculkMinRange() { return sculkMinRange != null ? sculkMinRange : defaultMinRange; }
    public double getSculkFalloffCurve() { return sculkFalloffCurve != null ? sculkFalloffCurve : defaultFalloffCurve; }
    public double getSculkVolumeThresholdDb() { return sculkVolumeThresholdDb; }
    public long getSculkCooldown() { return sculkCooldown; }
    public int getSculkMaxScanRadius() { return sculkMaxScanRadius; }
    public boolean isSculkVibrationParticleEnabled() { return sculkVibrationParticle; }
    public int getSculkVibrationMinArrivalTicks() { return sculkVibrationMinArrivalTicks; }
    public int getSculkVibrationMaxArrivalTicks() { return sculkVibrationMaxArrivalTicks; }

    // Environmental modifiers
    public boolean isEnvironmentalModifiersEnabled() { return environmentalModifiersEnabled; }
    public boolean isWeatherModifiersEnabled() { return weatherModifiersEnabled && environmentalModifiersEnabled; }
    public double getRainRangeMultiplier() { return rainRangeMultiplier; }
    public double getRainThresholdAdjustmentDb() { return rainThresholdAdjustmentDb; }
    public double getThunderRangeMultiplier() { return thunderRangeMultiplier; }
    public double getThunderThresholdAdjustmentDb() { return thunderThresholdAdjustmentDb; }

    // Legacy compatibility (deprecated)
    @Deprecated
    public boolean isHostileMobHearingEnabled() { return hostileMobsEnabled; }
    @Deprecated
    public boolean isWardenHearingEnabled() { return wardenEnabled; }
    @Deprecated
    public boolean isSculkHearingEnabled() { return sculkEnabled; }
    @Deprecated
    public double getHearingRange() { return defaultMaxRange; }
    @Deprecated
    public float getVolumeThreshold() { return 0.0f; }  // No longer used
    @Deprecated
    public double getMinVolumeForDetection() { return 0.0; }  // No longer used
    @Deprecated
    public double getVolumeThresholdDb() { return hostileVolumeThresholdDb; }  // Use category-specific

    /**
     * Gets the raw FileConfiguration for debug settings.
     */
    public FileConfiguration getConfig() {
        return config;
    }

    // Setters for commands
    public void setMobHearingEnabled(boolean enabled) {
        this.mobHearingEnabled = enabled;
        config.set(PATH_MOB_HEARING + ".enabled", enabled);
        plugin.saveConfig();
    }

    public void setSculkHearingEnabled(boolean enabled) {
        this.sculkEnabled = enabled;
        config.set(PATH_SCULK + ".enabled", enabled);
        plugin.saveConfig();
    }

    public void setEnvironmentalModifiersEnabled(boolean enabled) {
        this.environmentalModifiersEnabled = enabled;
        config.set(PATH_ENVIRONMENTAL + ".enabled", enabled);
        plugin.saveConfig();
    }

    public boolean isBiomeModifiersEnabled() {
        return biomeModifiersEnabled && environmentalModifiersEnabled;
    }

    public boolean isTimeModifiersEnabled() {
        return timeModifiersEnabled && environmentalModifiersEnabled;
    }

    // Mob Group Alert
    public boolean isMobGroupAlertEnabled() {
        return mobGroupAlertEnabled;
    }

    public int getMaxMobAlerts() {
        return maxMobAlerts;
    }

    public boolean shouldGroupAlertOnlyAfterTargeting() {
        return groupAlertOnlyAfterTargeting;
    }

    public double getGroupAlertRange(String mobType) {
        String path = "mob-hearing.hostile-mobs.group-alert.ranges." + mobType.toLowerCase();
        return readNonNegativeDouble(path, 16.0);
    }

    // Debug
    public boolean isAudioLoggingEnabled() { return audioLoggingEnabled; }
    public boolean isRangeLoggingEnabled() { return rangeLoggingEnabled; }
    public boolean isDetectionLoggingEnabled() { return detectionLoggingEnabled; }
    public boolean isWardenLoggingEnabled() { return wardenLoggingEnabled; }
    public boolean isSculkLoggingEnabled() { return sculkLoggingEnabled; }
    public boolean isPeacefulLoggingEnabled() { return peacefulLoggingEnabled; }
    public boolean isEnvironmentalLoggingEnabled() { return environmentalLoggingEnabled; }
    public boolean isGroupAlertLoggingEnabled() { return groupAlertLoggingEnabled; }
}

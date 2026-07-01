package de.tecca.simplevoicemechanics.listener;

import de.tecca.simplevoicemechanics.SimpleVoiceMechanics;
import de.tecca.simplevoicemechanics.api.VoiceSculkActivationContext;
import de.tecca.simplevoicemechanics.event.VoiceDetectedEvent;
import de.tecca.simplevoicemechanics.manager.ConfigManager;
import de.tecca.simplevoicemechanics.service.DetectionResult;
import de.tecca.simplevoicemechanics.service.DetectionService;
import de.tecca.simplevoicemechanics.service.SculkVibrationMath;
import de.tecca.simplevoicemechanics.util.RangeCalculator;
import de.tecca.simplevoicemechanics.util.RandomProvider;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles Sculk Sensor activation from voice detection.
 *
 * <p>Triggers Sculk Sensors (regular and calibrated) when players speak nearby.
 * Uses range-based detection with configurable min/max range and falloff curve.
 *
 * <p>Requires Minecraft 1.19+ for GameEvent API support.
 * If GameEvent API is not available, this listener will be disabled.
 *
 * @author Tecca
 * @version 1.2.0
 */
public class SculkListener implements Listener {

    private final SimpleVoiceMechanics plugin;
    private final boolean gameEventSupported;

    /** Tracks last trigger time for each Sculk Sensor location */
    private final Map<Location, Long> lastTriggerTime = new HashMap<>();

    public SculkListener(SimpleVoiceMechanics plugin) {
        this.plugin = plugin;
        this.gameEventSupported = checkGameEventSupport();

        if (!gameEventSupported) {
            plugin.getLogger().warning("GameEvent API not available - Sculk Sensor detection disabled");
            plugin.getLogger().warning("Sculk Sensors require Minecraft 1.19+");
        }

        startCooldownCleanupTask();
    }

    /**
     * Checks if GameEvent API is available in this version.
     */
    private boolean checkGameEventSupport() {
        try {
            Class.forName("org.bukkit.GameEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Handles voice detection events for Sculk Sensor activation.
     */
    @EventHandler
    public void onVoiceDetected(VoiceDetectedEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!gameEventSupported) {
            return;
        }

        if (!plugin.getConfigManager().isSculkEnabled()) {
            logSculkEvent("ignored voice event because sculk hearing is disabled");
            return;
        }

        Player player = event.getPlayer();

        // Only detect Survival/Adventure players
        if (!isValidGameMode(player)) {
            logSculkEvent("ignored voice event from " + player.getName() + " in " + player.getGameMode());
            return;
        }

        double decibels = event.getDecibels();

        Location loc = event.getLocation();
        logSculkEvent(String.format(
                "processing voice event from %s at %.1f dB",
                player.getName(),
                decibels
        ));

        // Search for and activate nearby Sculk Sensors
        processSculkSensors(player, loc, decibels);
    }

    /**
     * Checks if the player is in a valid gamemode for detection.
     */
    private boolean isValidGameMode(Player player) {
        GameMode mode = player.getGameMode();
        return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
    }

    /**
     * Processes all Sculk Sensors within range.
     */
    private void processSculkSensors(Player player, Location loc, double decibels) {
        ConfigManager config = plugin.getConfigManager();
        double maxRange = config.getSculkMaxRange();
        double volumeThresholdDb = config.getSculkVolumeThresholdDb();

        // Calculate effective range based on volume
        double effectiveMaxRange = RangeCalculator.calculateEffectiveRange(maxRange, decibels, volumeThresholdDb);
        effectiveMaxRange = Math.min(effectiveMaxRange, config.getSculkMaxScanRadius());

        int searchRadius = (int) Math.ceil(effectiveMaxRange);
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();
        if (world == null) {
            logSculkEvent("ignored voice event because player world is unavailable");
            return;
        }

        if (searchRadius <= 0) {
            logSculkEvent(String.format(
                    "scan skipped because effective sculk radius is %.2f blocks at %.1f dB",
                    effectiveMaxRange,
                    decibels
            ));
            return;
        }

        logSculkEvent(String.format(
                "scanning for sculk sensors within %d blocks at %.1f dB (threshold %.1f dB)",
                searchRadius,
                decibels,
                volumeThresholdDb
        ));

        int baseX = playerLoc.getBlockX();
        int baseY = playerLoc.getBlockY();
        int baseZ = playerLoc.getBlockZ();
        int minYOffset = Math.max(-searchRadius, world.getMinHeight() - baseY);
        int maxYOffset = Math.min(searchRadius, world.getMaxHeight() - 1 - baseY);
        int sensorsFound = 0;

        // Iterate through all blocks in range
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = minYOffset; y <= maxYOffset; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    Block block = world.getBlockAt(baseX + x, baseY + y, baseZ + z);

                    if (isSculkSensor(block)) {
                        sensorsFound++;
                        processSculkSensor(block, player, loc, decibels);
                    }
                }
            }
        }

        if (sensorsFound == 0) {
            logSculkEvent(String.format("no sculk sensors found within %d blocks", searchRadius));
        }
    }

    /**
     * Checks if a block is a Sculk Sensor (regular or calibrated).
     * Version-safe check for blocks that may not exist in older versions.
     */
    private boolean isSculkSensor(Block block) {
        Material type = block.getType();
        String typeName = type.name();
        return typeName.equals("SCULK_SENSOR") || typeName.equals("CALIBRATED_SCULK_SENSOR");
    }

    /**
     * Processes a single Sculk Sensor for potential activation.
     */
    private void processSculkSensor(Block block, Player player, Location voiceLoc, double decibels) {
        ConfigManager config = plugin.getConfigManager();
        Location sensorLoc = block.getLocation();
        if (sensorLoc.getWorld() == null || voiceLoc.getWorld() == null ||
                !sensorLoc.getWorld().equals(voiceLoc.getWorld())) {
            return;
        }

        double distance = sensorLoc.distance(voiceLoc);
        double maxRange = config.getSculkMaxRange();
        double minRange = config.getSculkMinRange();
        double falloffCurve = config.getSculkFalloffCurve();
        double volumeThresholdDb = config.getSculkVolumeThresholdDb();
        DetectionResult detection = DetectionService.calculate(
                distance, minRange, maxRange, falloffCurve, decibels, volumeThresholdDb
        );

        if (!detection.isThresholdPassed()) {
            logSculkRejection(block, detection, "below threshold");
            return;
        }

        if (!detection.isWithinRange()) {
            logSculkRejection(block, detection, "out of range");
            return;
        }

        if (isOnCooldown(sensorLoc)) {
            logSculkRejection(block, detection, "cooldown");
            return;
        }

        if (!RandomProvider.passes(detection.getChance())) {
            logSculkRejection(block, detection, "chance miss");
            return;
        }

        VoiceSculkActivationContext context = new VoiceSculkActivationContext(
                player, block, distance, detection.getChance(), decibels
        );
        plugin.getApi().callSculkActivationHooks(context);
        if (context.isCancelled()) {
            logSculkRejection(block, detection, "hook cancellation");
            return;
        }

        if (context.getEffectiveDecibels() != decibels) {
            DetectionResult adjustedDetection = DetectionService.calculate(
                    distance, minRange, maxRange, falloffCurve,
                    context.getEffectiveDecibels(), volumeThresholdDb
            );
            if (!adjustedDetection.isThresholdPassed()) {
                logSculkRejection(block, adjustedDetection, "adjusted below threshold");
                return;
            }
            if (!adjustedDetection.isWithinRange()) {
                logSculkRejection(block, adjustedDetection, "adjusted out of range");
                return;
            }
            detection = adjustedDetection;
        }

        // Trigger the Sculk Sensor
        triggerSculkSensor(block, player, voiceLoc, detection.getDistance());

        // Record trigger time
        lastTriggerTime.put(sensorLoc, System.currentTimeMillis());

        // Debug logging
        if (config.isSculkLoggingEnabled()) {
            plugin.getLogger().info(String.format(
                    "Sculk %s activated by voice event%s | %s",
                    block.getType(),
                    config.isSculkVibrationParticleEnabled() ? " with vibration particle" : "",
                    detection.getDebugInfo()
            ));
        }
    }

    /**
     * Checks if a sensor is currently on cooldown.
     */
    private boolean isOnCooldown(Location sensorLoc) {
        Long lastTrigger = lastTriggerTime.get(sensorLoc);
        if (lastTrigger == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long cooldown = plugin.getConfigManager().getSculkCooldown();
        return (currentTime - lastTrigger) < cooldown;
    }

    /**
     * Triggers a Sculk Sensor using Paper's GameEvent API.
     * Only called when gameEventSupported is true.
     */
    private void triggerSculkSensor(Block block, Player player, Location voiceLoc, double distance) {
        World world = voiceLoc.getWorld();
        if (world == null) {
            return;
        }

        // Send the vibration-worthy event from the speaker position so vanilla sculk can hear it.
        world.sendGameEvent(player, GameEvent.STEP, voiceLoc.toVector());
        spawnVibrationParticle(world, voiceLoc, block, distance);
    }

    /**
     * Spawns the visible vibration travelling from the speaker to the sensor.
     */
    private void spawnVibrationParticle(World world, Location origin, Block sensorBlock, double distance) {
        ConfigManager config = plugin.getConfigManager();
        if (!config.isSculkVibrationParticleEnabled()) {
            return;
        }

        int arrivalTicks = SculkVibrationMath.calculateArrivalTicks(
                distance,
                config.getSculkVibrationMinArrivalTicks(),
                config.getSculkVibrationMaxArrivalTicks()
        );
        Vibration vibration = new Vibration(
                origin,
                new Vibration.Destination.BlockDestination(sensorBlock),
                arrivalTicks
        );
        world.spawnParticle(Particle.VIBRATION, origin, 1, vibration);
    }

    /**
     * Logs why a sculk sensor did not activate when sculk debug logging is enabled.
     */
    private void logSculkRejection(Block block, DetectionResult detection, String reason) {
        if (!plugin.getConfigManager().isSculkLoggingEnabled()) {
            return;
        }

        plugin.getLogger().info(String.format(
                "Sculk %s ignored voice (%s) | %s",
                block.getType(),
                reason,
                detection.getDebugInfo()
        ));
    }

    /**
     * Logs sculk-level debug messages that are not tied to one sensor block.
     */
    private void logSculkEvent(String message) {
        if (!plugin.getConfigManager().isSculkLoggingEnabled()) {
            return;
        }

        plugin.getLogger().info("[Sculk Debug] " + message);
    }

    /**
     * Cleans up old cooldown entries to prevent memory leaks.
     */
    private void cleanupOldTriggers() {
        long currentTime = System.currentTimeMillis();
        long maxAge = plugin.getConfigManager().getSculkCooldown() * 10;

        lastTriggerTime.entrySet().removeIf(entry ->
                (currentTime - entry.getValue()) > maxAge
        );
    }

    /**
     * Periodically cleans up old sculk cooldown entries.
     */
    private void startCooldownCleanupTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupOldTriggers, 200L, 200L);
    }
}

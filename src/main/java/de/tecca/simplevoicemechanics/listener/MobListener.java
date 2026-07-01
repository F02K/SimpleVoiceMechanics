package de.tecca.simplevoicemechanics.listener;

import de.tecca.simplevoicemechanics.SimpleVoiceMechanics;
import de.tecca.simplevoicemechanics.api.MobReactionType;
import de.tecca.simplevoicemechanics.api.VoiceMobReactionContext;
import de.tecca.simplevoicemechanics.event.VoiceDetectedEvent;
import de.tecca.simplevoicemechanics.manager.ConfigManager;
import de.tecca.simplevoicemechanics.service.DetectionResult;
import de.tecca.simplevoicemechanics.service.DetectionService;
import de.tecca.simplevoicemechanics.service.InvestigationMath;
import de.tecca.simplevoicemechanics.util.*;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles mob reactions to voice detection with category-based behavior.
 *
 * <p>Mob categories:
 * <ul>
 *   <li>Hostile: Attack player (Zombies, Skeletons, Creepers, etc.)</li>
 *   <li>Neutral: Look at player (Piglins, Zombified Piglins, etc.)</li>
 *   <li>Peaceful: Look at player, follow when sneaking (Cows, Sheep, etc.)</li>
 *   <li>Warden: Special anger-based behavior</li>
 * </ul>
 *
 * <p>New Features (v1.3.0):
 * <ul>
 *   <li>Biome-specific modifiers (caves echo, forests dampen)</li>
 *   <li>Time-of-day modifiers (night = more sensitive)</li>
 *   <li>Mob group alerting (zombies call hordes)</li>
 * </ul>
 *
 * @author Tecca
 * @version 1.3.0
 */
public class MobListener implements Listener {

    private static final String FOLLOW_META_KEY = "svm_following";
    private static final String FOLLOW_PLAYER_KEY = "svm_follow_player";
    private static final String EYE_CONTACT_META_KEY = "svm_eye_contact";
    private static final String EYE_CONTACT_TIME_KEY = "svm_eye_contact_time";
    private static final String FLEE_META_KEY = "svm_fleeing";
    private static final double INVESTIGATION_ARRIVAL_DISTANCE = 2.5;

    private final SimpleVoiceMechanics plugin;

    // Tracks which mobs are currently following which players
    private final Map<UUID, UUID> followingMobs = new HashMap<>();
    private final Map<UUID, Long> followStartTime = new HashMap<>();

    // Reaction cooldowns to prevent rapid re-triggering
    private final Map<UUID, Long> reactionCooldowns = new HashMap<>();

    // Hostile mobs investigating invisible players by last heard location
    private final Map<UUID, HostileInvestigation> hostileInvestigations = new HashMap<>();

    public MobListener(SimpleVoiceMechanics plugin) {
        this.plugin = plugin;
        startFollowCleanupTask();
    }

    /**
     * Handles voice detection events for mob mechanics.
     */
    @EventHandler
    public void onVoiceDetected(VoiceDetectedEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!plugin.getConfigManager().isMobHearingEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        // Only detect Survival/Adventure players
        if (!isValidGameMode(player)) {
            return;
        }

        Location loc = event.getLocation();
        double decibels = event.getDecibels();
        boolean isSneaking = player.isSneaking();

        // Apply environmental modifiers
        double modifiedDecibels = applyEnvironmentalModifiers(player, decibels);

        // Process all nearby mobs
        processNearbyMobs(player, loc, isSneaking, modifiedDecibels);
    }

    /**
     * Applies environmental modifiers (biome + time) to audio level.
     */
    private double applyEnvironmentalModifiers(Player player, double decibels) {
        ConfigManager config = plugin.getConfigManager();

        if (!config.isBiomeModifiersEnabled() && !config.isTimeModifiersEnabled()) {
            return decibels;
        }

        World world = player.getWorld();
        Location loc = player.getLocation();

        double adjustment = 0.0;

        // Biome modifiers
        if (config.isBiomeModifiersEnabled()) {
            Biome biome = loc.getBlock().getBiome();
            adjustment += BiomeModifier.getThresholdAdjustment(biome);
        }

        // Time modifiers
        if (config.isTimeModifiersEnabled()) {
            adjustment += TimeModifier.getThresholdAdjustment(world);
        }

        // Adjust decibels (positive adjustment = easier detection)
        double modified = decibels - adjustment;

        // Debug logging
        if (plugin.getConfigManager().isEnvironmentalLoggingEnabled()) {
            plugin.getLogger().info(String.format(
                    "Environmental modifiers | Original: %.1f dB | Adjustment: %+.1f dB | Modified: %.1f dB",
                    decibels, adjustment, modified
            ));
        }

        return modified;
    }

    /**
     * Applies environmental range multiplier.
     */
    private double applyRangeMultiplier(Player player, double baseRange) {
        ConfigManager config = plugin.getConfigManager();

        if (!config.isBiomeModifiersEnabled() && !config.isTimeModifiersEnabled()) {
            return baseRange;
        }

        World world = player.getWorld();
        Location loc = player.getLocation();

        double multiplier = 1.0;

        // Biome modifiers
        if (config.isBiomeModifiersEnabled()) {
            Biome biome = loc.getBlock().getBiome();
            multiplier *= BiomeModifier.getRangeMultiplier(biome);
        }

        // Time modifiers
        if (config.isTimeModifiersEnabled()) {
            multiplier *= TimeModifier.getRangeMultiplier(world);
        }

        return baseRange * multiplier;
    }

    /**
     * Checks if a mob is on reaction cooldown.
     */
    private boolean isOnReactionCooldown(Mob mob, long cooldownMs) {
        Long lastReaction = reactionCooldowns.get(mob.getUniqueId());
        if (lastReaction == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        return (currentTime - lastReaction) < cooldownMs;
    }

    /**
     * Sets reaction cooldown for a mob.
     */
    private void setReactionCooldown(Mob mob) {
        reactionCooldowns.put(mob.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Makes mob look at location with scheduled stop after duration.
     */
    private void makeMobLookAtWithDuration(Mob mob, Location target, int durationTicks) {
        // Start looking
        makeMobLookAt(mob, target);

        // Schedule stop looking after duration
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (mob.isValid()) {
                mob.getPathfinder().stopPathfinding();
            }
        }, durationTicks);
    }

    /**
     * Checks if the player is in a valid gamemode for detection.
     */
    private boolean isValidGameMode(Player player) {
        GameMode mode = player.getGameMode();
        return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
    }

    /**
     * Checks if the player is currently invisible.
     */
    private boolean isInvisible(Player player) {
        return player.hasPotionEffect(PotionEffectType.INVISIBILITY);
    }

    /**
     * Processes all mobs near the voice location.
     */
    private void processNearbyMobs(Player player, Location loc, boolean isSneaking, double decibels) {
        ConfigManager config = plugin.getConfigManager();

        double maxCheckRange = calculateMaxMobCheckRange(player, decibels, config);

        World world = loc.getWorld();
        if (world == null) {
            return;
        }

        Collection<Entity> nearbyEntities = world
                .getNearbyEntities(loc, maxCheckRange, maxCheckRange, maxCheckRange);

        for (Entity entity : nearbyEntities) {
            if (entity instanceof Mob) {
                processMob((Mob) entity, player, loc, isSneaking, decibels);
            }
        }
    }

    /**
     * Processes a single mob's reaction to voice.
     */
    private void processMob(Mob mob, Player player, Location playerLoc, boolean isSneaking, double decibels) {
        EntityType type = mob.getType();
        ConfigManager config = plugin.getConfigManager();

        // Determine mob category and process accordingly
        if (type == EntityType.WARDEN && config.isWardenEnabled()) {
            processWarden((Warden) mob, player, playerLoc, decibels);
        } else if (MobCategory.isHostile(type)) {
            processHostileMob(mob, player, playerLoc, decibels);
        } else if (MobCategory.isNeutral(type)) {
            processNeutralMob(mob, player, playerLoc, decibels);
        } else if (MobCategory.isPeaceful(type)) {
            processPeacefulMob(mob, player, playerLoc, isSneaking, decibels);
        }
    }

    /**
     * Processes Warden special behavior.
     */
    private void processWarden(Warden warden, Player player, Location playerLoc, double decibels) {
        ConfigManager config = plugin.getConfigManager();

        double distance = warden.getLocation().distance(playerLoc);
        double maxRange = applyRangeMultiplier(player, config.getWardenMaxRange());
        double minRange = applyRangeMultiplier(player, config.getWardenMinRange());
        double falloffCurve = config.getWardenFalloffCurve();
        double volumeThresholdDb = config.getWardenVolumeThresholdDb();
        DetectionResult detection = DetectionService.calculate(
                distance, minRange, maxRange, falloffCurve, decibels, volumeThresholdDb
        );

        if (!detection.canAttemptDetection() || !RandomProvider.passes(detection.getChance())) {
            return;
        }

        VoiceMobReactionContext context = callMobReactionHooks(
                player, warden, MobReactionType.WARDEN_ANGER, detection, decibels
        );
        if (context == null) {
            return;
        }

        // Calculate anger based on distance and volume
        int angerIncrease = RangeCalculator.calculateWardenAnger(
                distance, minRange, maxRange, context.getEffectiveDecibels(), volumeThresholdDb
        );
        warden.increaseAnger(player, angerIncrease);

        // Debug logging
        if (config.isWardenLoggingEnabled()) {
            plugin.getLogger().info(String.format(
                    "Warden anger +%d | %s",
                    angerIncrease,
                    detection.getDebugInfo()
            ));
        }
    }

    /**
     * Calculates the largest entity search radius needed by enabled mob categories.
     */
    private double calculateMaxMobCheckRange(Player player, double decibels, ConfigManager config) {
        double maxCheckRange = 0.0;

        if (config.isHostileMobsEnabled()) {
            maxCheckRange = Math.max(maxCheckRange, RangeCalculator.calculateEffectiveRange(
                    applyRangeMultiplier(player, config.getHostileMaxRange()),
                    decibels,
                    config.getHostileVolumeThresholdDb()
            ));
        }

        if (config.isNeutralMobsEnabled()) {
            maxCheckRange = Math.max(maxCheckRange, RangeCalculator.calculateEffectiveRange(
                    applyRangeMultiplier(player, config.getNeutralMaxRange()),
                    decibels,
                    config.getNeutralVolumeThresholdDb()
            ));
        }

        if (config.isPeacefulMobsEnabled()) {
            maxCheckRange = Math.max(maxCheckRange, RangeCalculator.calculateEffectiveRange(
                    applyRangeMultiplier(player, config.getPeacefulMaxRange()),
                    decibels,
                    config.getPeacefulVolumeThresholdDb()
            ));
        }

        if (config.isWardenEnabled()) {
            maxCheckRange = Math.max(maxCheckRange, RangeCalculator.calculateEffectiveRange(
                    applyRangeMultiplier(player, config.getWardenMaxRange()),
                    decibels,
                    config.getWardenVolumeThresholdDb()
            ));
        }

        return maxCheckRange;
    }

    /**
     * Processes hostile mob behavior with group alerting.
     */
    private void processHostileMob(Mob mob, Player player, Location playerLoc, double decibels) {
        ConfigManager config = plugin.getConfigManager();

        if (!config.isHostileMobsEnabled()) {
            return;
        }

        if (config.isHostileBlacklisted(mob.getType())) {
            return;
        }

        // Check if mob can attack (not tamed, not baby)
        if (!MobCondition.canAttack(mob)) {
            return;
        }

        double distance = mob.getLocation().distance(playerLoc);
        double maxRange = applyRangeMultiplier(player, config.getHostileMaxRange());
        double minRange = applyRangeMultiplier(player, config.getHostileMinRange());
        double falloffCurve = config.getHostileFalloffCurve();
        double volumeThresholdDb = config.getHostileVolumeThresholdDb();
        DetectionResult detection = DetectionService.calculate(
                distance, minRange, maxRange, falloffCurve, decibels, volumeThresholdDb
        );

        if (!detection.canAttemptDetection() || !RandomProvider.passes(detection.getChance())) {
            return;
        }

        if (isInvisible(player) && config.isInvisiblePlayerInvestigationEnabled()) {
            processInvisibleHostileMob(mob, player, playerLoc, detection, decibels, config);
            return;
        }

        VoiceMobReactionContext targetContext = callMobReactionHooks(
                player, mob, MobReactionType.HOSTILE_TARGET, detection, decibels
        );
        if (targetContext == null) {
            return;
        }

        // Target player
        if (mob.getTarget() == null) {
            mob.setTarget(player);
        }

        // NEW FEATURE: Mob group alerting
        if (config.isMobGroupAlertEnabled() && MobGroupAlert.isSocialMob(mob.getType())) {
            VoiceMobReactionContext groupContext = callMobReactionHooks(
                    player, mob, MobReactionType.GROUP_ALERT, detection, targetContext.getEffectiveDecibels()
            );
            if (groupContext == null) {
                return;
            }

            int maxAlerts = Math.min(
                    config.getMaxMobAlerts(),
                    MobGroupAlert.calculateAlertCount(distance, minRange, maxRange)
            );
            double alertRange = config.getGroupAlertRange(mob.getType().name());
            int alerted = MobGroupAlert.alertNearbyMobs(mob, player, maxAlerts, alertRange);

            if (alerted > 0 && config.isGroupAlertLoggingEnabled()) {
                plugin.getLogger().info(String.format(
                        "%s alerted %d nearby %s(s)",
                        mob.getType(), alerted, mob.getType()
                ));
            }
        }

        // Debug logging
        if (config.isDetectionLoggingEnabled()) {
            plugin.getLogger().info(String.format(
                    "Hostile %s detected | %s",
                    mob.getType(),
                    detection.getDebugInfo()
            ));
        }
    }

    /**
     * Processes hostile detection for invisible players using last-heard-location investigation.
     */
    private void processInvisibleHostileMob(Mob mob, Player player, Location heardLocation,
                                            DetectionResult detection, double decibels, ConfigManager config) {
        if (!startHostileInvestigation(mob, player, heardLocation, detection, decibels)) {
            return;
        }

        if (config.isMobGroupAlertEnabled() && MobGroupAlert.isSocialMob(mob.getType())) {
            VoiceMobReactionContext groupContext = callMobReactionHooks(
                    player, mob, MobReactionType.GROUP_ALERT, detection, decibels
            );
            if (groupContext == null) {
                return;
            }

            int maxAlerts = Math.min(
                    config.getMaxMobAlerts(),
                    MobGroupAlert.calculateAlertCount(
                            detection.getDistance(),
                            detection.getConfiguredMinRange(),
                            detection.getConfiguredMaxRange()
                    )
            );
            double alertRange = config.getGroupAlertRange(mob.getType().name());
            int alerted = alertNearbyMobsToInvestigate(mob, player, heardLocation, detection, decibels, maxAlerts, alertRange);

            if (alerted > 0 && config.isGroupAlertLoggingEnabled()) {
                plugin.getLogger().info(String.format(
                        "%s alerted %d nearby %s(s) to investigate voice",
                        mob.getType(), alerted, mob.getType()
                ));
            }
        }

        if (config.isDetectionLoggingEnabled()) {
            plugin.getLogger().info(String.format(
                    "Hostile %s investigating invisible player | %s",
                    mob.getType(),
                    detection.getDebugInfo()
            ));
        }
    }

    /**
     * Starts last-heard-location investigation if integration hooks allow it.
     */
    private boolean startHostileInvestigation(Mob mob, Player player, Location heardLocation,
                                             DetectionResult detection, double decibels) {
        VoiceMobReactionContext context = callMobReactionHooks(
                player, mob, MobReactionType.HOSTILE_INVESTIGATE, detection, decibels
        );
        if (context == null) {
            return false;
        }

        Location target = heardLocation.clone();
        clearInvestigationTarget(mob, player);
        hostileInvestigations.put(
                mob.getUniqueId(),
                new HostileInvestigation(player.getUniqueId(), target, System.currentTimeMillis())
        );
        mob.getPathfinder().moveTo(target);
        return true;
    }

    /**
     * Alerts same-type mobs to investigate the invisible player's last heard location.
     */
    private int alertNearbyMobsToInvestigate(Mob alertingMob, Player player, Location heardLocation,
                                             DetectionResult detection, double decibels,
                                             int maxAlerts, double alertRange) {
        Location mobLoc = alertingMob.getLocation();
        World world = mobLoc.getWorld();
        if (world == null) {
            return 0;
        }

        double safeAlertRange = Math.max(0.0, alertRange);
        int alerted = 0;

        for (Entity entity : world.getNearbyEntities(mobLoc, safeAlertRange, safeAlertRange, safeAlertRange)) {
            if (alerted >= maxAlerts) {
                break;
            }

            if (!(entity instanceof Mob) || entity.equals(alertingMob) || entity.getType() != alertingMob.getType()) {
                continue;
            }

            Mob nearbyMob = (Mob) entity;
            if (!MobCondition.canAttack(nearbyMob)) {
                continue;
            }

            if (startHostileInvestigation(nearbyMob, player, heardLocation, detection, decibels)) {
                alerted++;
            }
        }

        return alerted;
    }

    /**
     * Processes neutral mob behavior (look at player).
     */
    private void processNeutralMob(Mob mob, Player player, Location playerLoc, double decibels) {
        ConfigManager config = plugin.getConfigManager();

        if (!config.isNeutralMobsEnabled()) {
            return;
        }

        if (config.isNeutralBlacklisted(mob.getType())) {
            return;
        }

        // Check reaction cooldown
        if (isOnReactionCooldown(mob, config.getNeutralReactionCooldownMs())) {
            return;
        }

        double distance = mob.getLocation().distance(playerLoc);
        double maxRange = applyRangeMultiplier(player, config.getNeutralMaxRange());
        double minRange = applyRangeMultiplier(player, config.getNeutralMinRange());
        double falloffCurve = config.getNeutralFalloffCurve();
        double volumeThresholdDb = config.getNeutralVolumeThresholdDb();
        DetectionResult detection = DetectionService.calculate(
                distance, minRange, maxRange, falloffCurve, decibels, volumeThresholdDb
        );

        if (!detection.canAttemptDetection() || !RandomProvider.passes(detection.getChance())) {
            return;
        }

        // Apply reaction multiplier for special mobs (e.g., babies)
        double reactionChance = config.getNeutralReactionChance() * MobCondition.getReactionMultiplier(mob);
        if (!RandomProvider.passes(reactionChance)) {
            return;
        }

        VoiceMobReactionContext context = callMobReactionHooks(
                player, mob, MobReactionType.NEUTRAL_LOOK, detection, decibels
        );
        if (context == null) {
            return;
        }

        // Set cooldown to prevent immediate re-reaction
        setReactionCooldown(mob);

        // Make mob look at player with duration (only if can look)
        if (config.shouldNeutralLookAtPlayer() && MobCondition.canLookAt(mob)) {
            makeMobLookAtWithDuration(mob, player.getLocation(), config.getNeutralLookDurationTicks());
        }

        // Debug logging
        if (config.isDetectionLoggingEnabled()) {
            plugin.getLogger().info(String.format(
                    "Neutral %s looked | %s",
                    mob.getType(),
                    detection.getDebugInfo()
            ));
        }
    }

    /**
     * Processes peaceful mob behavior (look at player, flee from loud noise, follow when sneaking).
     */
    private void processPeacefulMob(Mob mob, Player player, Location playerLoc, boolean isSneaking, double decibels) {
        ConfigManager config = plugin.getConfigManager();

        if (!config.isPeacefulMobsEnabled()) {
            return;
        }

        if (config.isPeacefulBlacklisted(mob.getType())) {
            return;
        }

        double distance = mob.getLocation().distance(playerLoc);
        double maxRange = applyRangeMultiplier(player, config.getPeacefulMaxRange());
        double minRange = applyRangeMultiplier(player, config.getPeacefulMinRange());
        double falloffCurve = config.getPeacefulFalloffCurve();
        double volumeThresholdDb = config.getPeacefulVolumeThresholdDb();
        DetectionResult detection = DetectionService.calculate(
                distance, minRange, maxRange, falloffCurve, decibels, volumeThresholdDb
        );

        if (!detection.canAttemptDetection() || !RandomProvider.passes(detection.getChance())) {
            return;
        }

        // Apply reaction multiplier for special mobs (e.g., babies)
        double reactionChance = config.getPeacefulReactionChance() * MobCondition.getReactionMultiplier(mob);
        if (!RandomProvider.passes(reactionChance)) {
            return;
        }

        // Check if mob should flee from loud noise (flee overrides cooldown)
        if (config.isFleeEnabled() && decibels > config.getFleeVolumeDb() &&
                !mob.hasMetadata(FLEE_META_KEY) && MobCondition.canFlee(mob)) {
            VoiceMobReactionContext context = callMobReactionHooks(
                    player, mob, MobReactionType.PEACEFUL_FLEE, detection, decibels
            );
            if (context == null) {
                return;
            }

            setReactionCooldown(mob);
            makeMobFlee(mob, player);

            if (config.isPeacefulLoggingEnabled()) {
                plugin.getLogger().info(String.format(
                        "Peaceful %s fleeing | dB: %.1f | Distance: %.1f",
                        mob.getType(), decibels, distance
                ));
            }
            return;
        }

        // Check reaction cooldown for normal behaviors (look/follow)
        boolean onCooldown = isOnReactionCooldown(mob, config.getPeacefulReactionCooldownMs());

        // Always track eye contact if player is looking at mob (even on cooldown)
        if (config.shouldPeacefulLookAtPlayer() && isPlayerLookingAtMob(player, mob, config.getEyeContactRange())) {
            recordEyeContact(mob, player);
        }

        // Skip other reactions if on cooldown
        if (onCooldown) {
            return;
        }

        // Set cooldown for this reaction
        setReactionCooldown(mob);

        // Make mob look at player with duration (only if can look)
        if (config.shouldPeacefulLookAtPlayer() && MobCondition.canLookAt(mob)) {
            VoiceMobReactionContext lookContext = callMobReactionHooks(
                    player, mob, MobReactionType.PEACEFUL_LOOK, detection, decibels
            );
            if (lookContext != null) {
                makeMobLookAtWithDuration(mob, player.getLocation(), config.getPeacefulLookDurationTicks());
            }
        }

        // If sneaking: check eye contact requirement and make mob follow (only if can follow)
        if (isSneaking && config.isFollowWhenSneakingEnabled() && MobCondition.canFollow(mob)) {
            if (config.requiresEyeContact()) {
                if (hasRecentEyeContact(mob, player)) {
                    startFollowingIfAllowed(mob, player, detection, decibels);
                }
            } else {
                startFollowingIfAllowed(mob, player, detection, decibels);
            }
        }

        // Debug logging
        if (config.isPeacefulLoggingEnabled()) {
            String action = isSneaking ? "following" : "looked";
            plugin.getLogger().info(String.format(
                    "Peaceful %s %s | %s",
                    mob.getType(),
                    action,
                    detection.getDebugInfo()
            ));
        }
    }

    /**
     * Makes a mob flee from the player.
     */
    private void makeMobFlee(Mob mob, Player player) {
        ConfigManager config = plugin.getConfigManager();

        mob.setMetadata(FLEE_META_KEY, new FixedMetadataValue(plugin, true));

        Location mobLoc = mob.getLocation();
        Location playerLoc = player.getLocation();

        org.bukkit.util.Vector direction = mobLoc.toVector().subtract(playerLoc.toVector()).normalize();
        Location fleeTarget = mobLoc.clone().add(direction.multiply(config.getFleeDistance()));

        mob.getPathfinder().moveTo(fleeTarget);

        int fleeDurationTicks = config.getFleeDurationTicks();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (mob.isValid()) {
                mob.removeMetadata(FLEE_META_KEY, plugin);
                mob.getPathfinder().stopPathfinding();
            }
        }, fleeDurationTicks);
    }

    /**
     * Calls registered integration hooks for a mob reaction.
     *
     * @return the context when allowed, or null when cancelled
     */
    private VoiceMobReactionContext callMobReactionHooks(Player player, Mob mob, MobReactionType reactionType,
                                                        DetectionResult detection, double decibels) {
        VoiceMobReactionContext context = new VoiceMobReactionContext(
                player, mob, reactionType, detection.getDistance(), detection.getChance(), decibels
        );
        plugin.getApi().callMobReactionHooks(context);
        return context.isCancelled() ? null : context;
    }

    /**
     * Starts mob following only if integration hooks allow it.
     */
    private void startFollowingIfAllowed(Mob mob, Player player, DetectionResult detection, double decibels) {
        VoiceMobReactionContext context = callMobReactionHooks(
                player, mob, MobReactionType.PEACEFUL_FOLLOW, detection, decibels
        );
        if (context != null) {
            startFollowing(mob, player);
        }
    }

    /**
     * Checks if player is looking at a mob within range.
     */
    private boolean isPlayerLookingAtMob(Player player, Mob mob, double range) {
        Location playerEye = player.getEyeLocation();
        Location mobLoc = mob.getLocation().add(0, mob.getHeight() / 2, 0);

        double distance = playerEye.distance(mobLoc);
        if (distance > range) {
            return false;
        }

        org.bukkit.util.Vector toMob = mobLoc.toVector().subtract(playerEye.toVector()).normalize();
        org.bukkit.util.Vector playerDirection = playerEye.getDirection();

        double dotProduct = toMob.dot(playerDirection);
        return dotProduct > 0.7;
    }

    /**
     * Records eye contact between player and mob.
     */
    private void recordEyeContact(Mob mob, Player player) {
        mob.setMetadata(EYE_CONTACT_META_KEY, new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        mob.setMetadata(EYE_CONTACT_TIME_KEY, new FixedMetadataValue(plugin, System.currentTimeMillis()));
    }

    /**
     * Checks if player has recent eye contact with mob.
     */
    private boolean hasRecentEyeContact(Mob mob, Player player) {
        if (!mob.hasMetadata(EYE_CONTACT_META_KEY)) {
            return false;
        }

        String contactedPlayer = mob.getMetadata(EYE_CONTACT_META_KEY).get(0).asString();
        if (!contactedPlayer.equals(player.getUniqueId().toString())) {
            return false;
        }

        if (!mob.hasMetadata(EYE_CONTACT_TIME_KEY)) {
            return false;
        }

        long contactTime = mob.getMetadata(EYE_CONTACT_TIME_KEY).get(0).asLong();
        long currentTime = System.currentTimeMillis();
        long memoryDurationMs = plugin.getConfigManager().getEyeContactMemoryMs();

        return (currentTime - contactTime) < memoryDurationMs;
    }

    /**
     * Makes a mob look at a location.
     */
    private void makeMobLookAt(Mob mob, Location target) {
        Location mobLoc = mob.getLocation();
        Location direction = target.clone().subtract(mobLoc);

        mobLoc.setDirection(direction.toVector());
        mob.teleport(mobLoc);
    }

    /**
     * Starts mob following player (only if mob can follow).
     */
    private void startFollowing(Mob mob, Player player) {
        UUID mobId = mob.getUniqueId();
        UUID playerId = player.getUniqueId();

        followingMobs.put(mobId, playerId);
        followStartTime.put(mobId, System.currentTimeMillis());

        mob.setMetadata(FOLLOW_META_KEY, new FixedMetadataValue(plugin, true));
        mob.setMetadata(FOLLOW_PLAYER_KEY, new FixedMetadataValue(plugin, playerId.toString()));
    }

    /**
     * Starts background task to manage following mobs.
     */
    private void startFollowCleanupTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            ConfigManager config = plugin.getConfigManager();
            long maxDuration = config.getFollowDuration() * 1000L;
            double maxDistance = config.getFollowMaxDistance();

            followingMobs.entrySet().removeIf(entry -> {
                UUID mobId = entry.getKey();
                UUID playerId = entry.getValue();

                Long startTime = followStartTime.get(mobId);
                if (startTime == null || (currentTime - startTime) > maxDuration) {
                    cleanupMob(mobId);
                    return true;
                }

                Entity mobEntity = Bukkit.getEntity(mobId);
                Player player = Bukkit.getPlayer(playerId);

                if (mobEntity == null || player == null || !player.isOnline()) {
                    cleanupMob(mobId);
                    return true;
                }

                if (mobEntity instanceof Mob) {
                    Mob mob = (Mob) mobEntity;

                    if (!MobCondition.canFollow(mob)) {
                        cleanupMob(mobId);
                        return true;
                    }

                    double distance = mob.getLocation().distance(player.getLocation());

                    if (distance > maxDistance) {
                        cleanupMob(mobId);
                        return true;
                    }

                    mob.getPathfinder().moveTo(player.getLocation());
                }

                return false;
            });

            reactionCooldowns.entrySet().removeIf(entry -> {
                Entity entity = Bukkit.getEntity(entry.getKey());
                return entity == null || !entity.isValid() || (currentTime - entry.getValue()) > 30000L;
            });

            updateHostileInvestigations(currentTime, config);
        }, 20L, 20L);
    }

    /**
     * Updates hostile mobs investigating invisible players by last heard location.
     */
    private void updateHostileInvestigations(long currentTime, ConfigManager config) {
        long timeoutMs = config.getInvisiblePlayerInvestigationTimeout() * 1000L;
        double movementTolerance = config.getInvisiblePlayerMovementTolerance();

        hostileInvestigations.entrySet().removeIf(entry -> {
            UUID mobId = entry.getKey();
            HostileInvestigation investigation = entry.getValue();
            Entity mobEntity = Bukkit.getEntity(mobId);
            Player player = Bukkit.getPlayer(investigation.getPlayerId());

            if (!(mobEntity instanceof Mob) || !mobEntity.isValid() || player == null || !player.isOnline()) {
                return true;
            }

            Mob mob = (Mob) mobEntity;
            Location heardLocation = investigation.getHeardLocation();

            if ((currentTime - investigation.getStartTime()) > timeoutMs) {
                clearInvestigationTarget(mob, player);
                return true;
            }

            if (player.getWorld() == null || heardLocation.getWorld() == null ||
                    !player.getWorld().equals(heardLocation.getWorld())) {
                clearInvestigationTarget(mob, player);
                return true;
            }

            if (!isInvisible(player)) {
                clearInvestigationTarget(mob, player);
                return true;
            }

            double movedDistanceSquared = player.getLocation().distanceSquared(heardLocation);
            if (InvestigationMath.hasMovedBeyondTolerance(movedDistanceSquared, movementTolerance)) {
                clearInvestigationTarget(mob, player);
                return true;
            }

            mob.getPathfinder().moveTo(heardLocation);

            if (!config.shouldInvisiblePlayerAttackIfStillThere()) {
                return false;
            }

            if (mob.getLocation().getWorld() == null || !mob.getLocation().getWorld().equals(heardLocation.getWorld())) {
                clearInvestigationTarget(mob, player);
                return true;
            }

            double arrivalDistanceSquared = mob.getLocation().distanceSquared(heardLocation);
            if (!investigation.hasTargetedPlayer() &&
                    !InvestigationMath.hasMovedBeyondTolerance(arrivalDistanceSquared, INVESTIGATION_ARRIVAL_DISTANCE)) {
                mob.setTarget(player);
                investigation.setTargetedPlayer(true);
                if (config.isDetectionLoggingEnabled()) {
                    plugin.getLogger().info(String.format(
                            "Hostile %s attacked invisible player still at last heard location",
                            mob.getType()
                    ));
                }
            }

            return false;
        });
    }

    /**
     * Clears a target assigned by invisible-player investigation.
     */
    private void clearInvestigationTarget(Mob mob, Player player) {
        if (mob.getTarget() != null && mob.getTarget().equals(player)) {
            mob.setTarget(null);
        }
    }

    /**
     * Cleans up mob following state.
     */
    private void cleanupMob(UUID mobId) {
        Entity entity = Bukkit.getEntity(mobId);
        if (entity != null) {
            entity.removeMetadata(FOLLOW_META_KEY, plugin);
            entity.removeMetadata(FOLLOW_PLAYER_KEY, plugin);

            if (entity instanceof Mob) {
                ((Mob) entity).getPathfinder().stopPathfinding();
            }
        }

        followStartTime.remove(mobId);
        reactionCooldowns.remove(mobId);
        hostileInvestigations.remove(mobId);
    }

    /**
     * State for hostile mobs investigating invisible players.
     */
    private static final class HostileInvestigation {
        private final UUID playerId;
        private final Location heardLocation;
        private final long startTime;
        private boolean targetedPlayer;

        private HostileInvestigation(UUID playerId, Location heardLocation, long startTime) {
            this.playerId = playerId;
            this.heardLocation = heardLocation;
            this.startTime = startTime;
        }

        private UUID getPlayerId() {
            return playerId;
        }

        private Location getHeardLocation() {
            return heardLocation;
        }

        private long getStartTime() {
            return startTime;
        }

        private boolean hasTargetedPlayer() {
            return targetedPlayer;
        }

        private void setTargetedPlayer(boolean targetedPlayer) {
            this.targetedPlayer = targetedPlayer;
        }
    }
}

package de.tecca.simplevoicemechanics.service.mob.handler;

import de.tecca.simplevoicemechanics.SimpleVoiceMechanics;
import de.tecca.simplevoicemechanics.api.MobReactionType;
import de.tecca.simplevoicemechanics.api.VoiceMobReactionContext;
import de.tecca.simplevoicemechanics.manager.ConfigManager;
import de.tecca.simplevoicemechanics.service.DetectionResult;
import de.tecca.simplevoicemechanics.service.HostileSuspicionMath;
import de.tecca.simplevoicemechanics.service.mob.MobDetectionSupport;
import de.tecca.simplevoicemechanics.service.mob.model.HostileInvestigationType;
import de.tecca.simplevoicemechanics.service.mob.tracker.HostileInvestigationTracker;
import de.tecca.simplevoicemechanics.util.MobCondition;
import de.tecca.simplevoicemechanics.util.MobGroupAlert;
import de.tecca.simplevoicemechanics.util.RandomProvider;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public class HostileVoiceHandler {

    private final SimpleVoiceMechanics plugin;
    private final MobDetectionSupport support;
    private final HostileInvestigationTracker investigationTracker;

    public HostileVoiceHandler(SimpleVoiceMechanics plugin, MobDetectionSupport support,
                               HostileInvestigationTracker investigationTracker) {
        this.plugin = plugin;
        this.support = support;
        this.investigationTracker = investigationTracker;
    }

    public void process(Mob mob, Player player, Location playerLoc, double decibels) {
        ConfigManager config = plugin.getConfigManager();

        if (!config.isHostileMobsEnabled()) {
            return;
        }

        if (config.isHostileBlacklisted(mob.getType())) {
            return;
        }

        if (!MobCondition.canAttack(mob)) {
            return;
        }

        double distance = mob.getLocation().distance(playerLoc);
        double maxRange = support.applyRangeMultiplier(player, config.getHostileMaxRange());
        double minRange = support.applyRangeMultiplier(player, config.getHostileMinRange());
        double falloffCurve = config.getHostileFalloffCurve();
        double volumeThresholdDb = config.getHostileVolumeThresholdDb();
        DetectionResult detection = support.calculateDetection(
                playerLoc, mob.getLocation(), distance, minRange, maxRange, falloffCurve,
                decibels, volumeThresholdDb
        );

        if (!detection.canAttemptDetection() || !RandomProvider.passes(detection.getChance())) {
            return;
        }

        if (support.isInvisible(player) && config.isInvisiblePlayerInvestigationEnabled()) {
            processInvisibleHostileMob(mob, player, playerLoc, detection, decibels, config);
            return;
        }

        if (config.isHostileInvestigationEnabled()) {
            int detections = investigationTracker.recordSuspicion(
                    mob, player, config.getHostileInvestigationMemorySeconds()
            );
            boolean shouldTarget = HostileSuspicionMath.shouldTarget(
                    detections,
                    config.getHostileInvestigationDetectionsToTarget(),
                    distance,
                    detection.getEffectiveMinRange(),
                    detection.getDecibels(),
                    volumeThresholdDb
            );
            if (!shouldTarget) {
                if (investigationTracker.startInvestigation(
                        mob, player, playerLoc, detection, detection.getDecibels(),
                        HostileInvestigationType.VISIBLE_SOUND
                ) && !config.shouldGroupAlertOnlyAfterTargeting()) {
                    alertHostileGroupToInvestigate(
                            mob, player, playerLoc, detection, detection.getDecibels(),
                            config, HostileInvestigationType.VISIBLE_SOUND
                    );
                }
                if (config.isDetectionLoggingEnabled()) {
                    plugin.getLogger().info(String.format(
                            "Hostile %s investigating voice | %s",
                            mob.getType(),
                            detection.getDebugInfo()
                    ));
                }
                return;
            }
        }

        VoiceMobReactionContext targetContext = support.callMobReactionHooks(
                player, mob, MobReactionType.HOSTILE_TARGET, detection, detection.getDecibels()
        );
        if (targetContext == null) {
            return;
        }

        if (mob.getTarget() == null) {
            mob.setTarget(player);
        }

        if (config.isMobGroupAlertEnabled() && MobGroupAlert.isSocialMob(mob.getType())) {
            VoiceMobReactionContext groupContext = support.callMobReactionHooks(
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

        if (config.isDetectionLoggingEnabled()) {
            plugin.getLogger().info(String.format(
                    "Hostile %s detected | %s",
                    mob.getType(),
                    detection.getDebugInfo()
            ));
        }
    }

    private void processInvisibleHostileMob(Mob mob, Player player, Location heardLocation,
                                            DetectionResult detection, double decibels, ConfigManager config) {
        if (!investigationTracker.startInvestigation(
                mob, player, heardLocation, detection, decibels, HostileInvestigationType.INVISIBLE_SOUND
        )) {
            return;
        }

        if (config.isMobGroupAlertEnabled() && !config.shouldGroupAlertOnlyAfterTargeting()
                && MobGroupAlert.isSocialMob(mob.getType())) {
            alertHostileGroupToInvestigate(
                    mob, player, heardLocation, detection, decibels,
                    config, HostileInvestigationType.INVISIBLE_SOUND
            );
        }

        if (config.isDetectionLoggingEnabled()) {
            plugin.getLogger().info(String.format(
                    "Hostile %s investigating invisible player | %s",
                    mob.getType(),
                    detection.getDebugInfo()
            ));
        }
    }

    private void alertHostileGroupToInvestigate(Mob mob, Player player, Location heardLocation,
                                                DetectionResult detection, double decibels,
                                                ConfigManager config,
                                                HostileInvestigationType type) {
        VoiceMobReactionContext groupContext = support.callMobReactionHooks(
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
        int alerted = alertNearbyMobsToInvestigate(
                mob, player, heardLocation, detection, groupContext.getEffectiveDecibels(),
                maxAlerts, alertRange, type
        );

        if (alerted > 0 && config.isGroupAlertLoggingEnabled()) {
            plugin.getLogger().info(String.format(
                    "%s alerted %d nearby %s(s) to investigate voice",
                    mob.getType(), alerted, mob.getType()
            ));
        }
    }

    private int alertNearbyMobsToInvestigate(Mob alertingMob, Player player, Location heardLocation,
                                             DetectionResult detection, double decibels,
                                             int maxAlerts, double alertRange,
                                             HostileInvestigationType type) {
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

            if (investigationTracker.startInvestigation(nearbyMob, player, heardLocation, detection, decibels, type)) {
                alerted++;
            }
        }

        return alerted;
    }
}

package de.tecca.simplevoicemechanics.service.mob.handler;

import de.tecca.simplevoicemechanics.SimpleVoiceMechanics;
import de.tecca.simplevoicemechanics.api.MobReactionType;
import de.tecca.simplevoicemechanics.api.VoiceMobReactionContext;
import de.tecca.simplevoicemechanics.manager.ConfigManager;
import de.tecca.simplevoicemechanics.service.DetectionResult;
import de.tecca.simplevoicemechanics.service.mob.MobDetectionSupport;
import de.tecca.simplevoicemechanics.service.mob.tracker.PeacefulFollowTracker;
import de.tecca.simplevoicemechanics.util.MobCondition;
import de.tecca.simplevoicemechanics.util.RandomProvider;
import de.tecca.simplevoicemechanics.util.TemptationItems;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class PeacefulVoiceHandler {

    private static final String FLEE_META_KEY = "svm_fleeing";

    private final SimpleVoiceMechanics plugin;
    private final MobDetectionSupport support;
    private final PeacefulFollowTracker followTracker;

    public PeacefulVoiceHandler(SimpleVoiceMechanics plugin, MobDetectionSupport support,
                                PeacefulFollowTracker followTracker) {
        this.plugin = plugin;
        this.support = support;
        this.followTracker = followTracker;
    }

    public void process(Mob mob, Player player, Location playerLoc, boolean isSneaking, double decibels) {
        ConfigManager config = plugin.getConfigManager();

        if (!config.isPeacefulMobsEnabled()) {
            return;
        }

        if (config.isPeacefulBlacklisted(mob.getType())) {
            return;
        }

        double distance = mob.getLocation().distance(playerLoc);
        double maxRange = support.applyRangeMultiplier(player, config.getPeacefulMaxRange());
        double minRange = support.applyRangeMultiplier(player, config.getPeacefulMinRange());
        double falloffCurve = config.getPeacefulFalloffCurve();
        double volumeThresholdDb = config.getPeacefulVolumeThresholdDb();
        DetectionResult detection = support.calculateDetection(
                playerLoc, mob.getLocation(), distance, minRange, maxRange, falloffCurve,
                decibels, volumeThresholdDb
        );

        if (!detection.canAttemptDetection() || !RandomProvider.passes(detection.getChance())) {
            return;
        }

        double reactionChance = config.getPeacefulReactionChance()
                * MobCondition.getReactionMultiplier(mob)
                * getPeacefulMobReactionMultiplier(mob);
        if (!RandomProvider.passes(reactionChance)) {
            return;
        }

        if (config.isFleeEnabled() && decibels > getFleeVolumeDb(mob, config) &&
                !mob.hasMetadata(FLEE_META_KEY) && MobCondition.canFlee(mob)) {
            VoiceMobReactionContext context = support.callMobReactionHooks(
                    player, mob, MobReactionType.PEACEFUL_FLEE, detection, decibels
            );
            if (context == null) {
                return;
            }

            support.setReactionCooldown(mob);
            makeMobFlee(mob, player);

            if (config.isPeacefulLoggingEnabled()) {
                plugin.getLogger().info(String.format(
                        "Peaceful %s fleeing | dB: %.1f | Distance: %.1f",
                        mob.getType(), decibels, distance
                ));
            }
            return;
        }

        boolean onCooldown = support.isOnReactionCooldown(mob, config.getPeacefulReactionCooldownMs());

        if (config.shouldPeacefulLookAtPlayer()
                && followTracker.isPlayerLookingAtMob(player, mob, config.getEyeContactRange())) {
            followTracker.recordEyeContact(mob, player);
        }

        if (onCooldown) {
            return;
        }

        support.setReactionCooldown(mob);

        if (config.shouldPeacefulLookAtPlayer() && MobCondition.canLookAt(mob)) {
            VoiceMobReactionContext lookContext = support.callMobReactionHooks(
                    player, mob, MobReactionType.PEACEFUL_LOOK, detection, decibels
            );
            if (lookContext != null) {
                support.makeMobLookAtWithDuration(mob, player.getLocation(), config.getPeacefulLookDurationTicks());
            }
        }

        if (isSneaking && config.isFollowWhenSneakingEnabled() && MobCondition.canFollow(mob)) {
            if (config.requiresTemptItem() && !TemptationItems.isHoldingTemptationItem(
                    mob.getType(),
                    player.getInventory().getItemInMainHand(),
                    player.getInventory().getItemInOffHand()
            )) {
                return;
            }
            if (config.requiresEyeContact()) {
                if (followTracker.hasRecentEyeContact(mob, player)) {
                    startFollowingIfAllowed(mob, player, detection, decibels);
                }
            } else {
                startFollowingIfAllowed(mob, player, detection, decibels);
            }
        }

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

    private double getPeacefulMobReactionMultiplier(Mob mob) {
        EntityType type = mob.getType();
        if (type == EntityType.CAT || type == EntityType.OCELOT || type == EntityType.PARROT || type == EntityType.GOAT) {
            return 1.25;
        }
        return 1.0;
    }

    private double getFleeVolumeDb(Mob mob, ConfigManager config) {
        EntityType type = mob.getType();
        if (type == EntityType.CAT || type == EntityType.OCELOT || type == EntityType.GOAT) {
            return config.getFleeVolumeDb() - 5.0;
        }
        return config.getFleeVolumeDb();
    }

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

    private void startFollowingIfAllowed(Mob mob, Player player, DetectionResult detection, double decibels) {
        VoiceMobReactionContext context = support.callMobReactionHooks(
                player, mob, MobReactionType.PEACEFUL_FOLLOW, detection, decibels
        );
        if (context != null) {
            followTracker.startFollowing(mob, player);
        }
    }
}

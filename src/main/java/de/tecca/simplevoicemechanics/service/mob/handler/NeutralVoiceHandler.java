package de.tecca.simplevoicemechanics.service.mob.handler;

import de.tecca.simplevoicemechanics.SimpleVoiceMechanics;
import de.tecca.simplevoicemechanics.api.MobReactionType;
import de.tecca.simplevoicemechanics.api.VoiceMobReactionContext;
import de.tecca.simplevoicemechanics.manager.ConfigManager;
import de.tecca.simplevoicemechanics.service.DetectionResult;
import de.tecca.simplevoicemechanics.service.mob.MobDetectionSupport;
import de.tecca.simplevoicemechanics.util.MobCondition;
import de.tecca.simplevoicemechanics.util.RandomProvider;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public class NeutralVoiceHandler {

    private final SimpleVoiceMechanics plugin;
    private final MobDetectionSupport support;

    public NeutralVoiceHandler(SimpleVoiceMechanics plugin, MobDetectionSupport support) {
        this.plugin = plugin;
        this.support = support;
    }

    public void process(Mob mob, Player player, Location playerLoc, double decibels) {
        ConfigManager config = plugin.getConfigManager();

        if (!config.isNeutralMobsEnabled()) {
            return;
        }

        if (config.isNeutralBlacklisted(mob.getType())) {
            return;
        }

        if (support.isOnReactionCooldown(mob, config.getNeutralReactionCooldownMs())) {
            return;
        }

        double distance = mob.getLocation().distance(playerLoc);
        double maxRange = support.applyRangeMultiplier(player, config.getNeutralMaxRange());
        double minRange = support.applyRangeMultiplier(player, config.getNeutralMinRange());
        double falloffCurve = config.getNeutralFalloffCurve();
        double volumeThresholdDb = config.getNeutralVolumeThresholdDb();
        DetectionResult detection = support.calculateDetection(
                playerLoc, mob.getLocation(), distance, minRange, maxRange, falloffCurve,
                decibels, volumeThresholdDb
        );

        if (!detection.canAttemptDetection() || !RandomProvider.passes(detection.getChance())) {
            return;
        }

        double reactionChance = config.getNeutralReactionChance() * MobCondition.getReactionMultiplier(mob);
        if (!RandomProvider.passes(reactionChance)) {
            return;
        }

        VoiceMobReactionContext context = support.callMobReactionHooks(
                player, mob, MobReactionType.NEUTRAL_LOOK, detection, decibels
        );
        if (context == null) {
            return;
        }

        support.setReactionCooldown(mob);

        if (config.shouldNeutralLookAtPlayer() && MobCondition.canLookAt(mob)) {
            support.makeMobLookAtWithDuration(mob, player.getLocation(), config.getNeutralLookDurationTicks());
        }

        if (config.isDetectionLoggingEnabled()) {
            plugin.getLogger().info(String.format(
                    "Neutral %s looked | %s",
                    mob.getType(),
                    detection.getDebugInfo()
            ));
        }
    }
}

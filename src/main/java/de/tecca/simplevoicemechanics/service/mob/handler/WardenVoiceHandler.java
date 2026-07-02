package de.tecca.simplevoicemechanics.service.mob.handler;

import de.tecca.simplevoicemechanics.SimpleVoiceMechanics;
import de.tecca.simplevoicemechanics.api.MobReactionType;
import de.tecca.simplevoicemechanics.api.VoiceMobReactionContext;
import de.tecca.simplevoicemechanics.manager.ConfigManager;
import de.tecca.simplevoicemechanics.service.DetectionResult;
import de.tecca.simplevoicemechanics.service.mob.MobDetectionSupport;
import de.tecca.simplevoicemechanics.util.RandomProvider;
import de.tecca.simplevoicemechanics.util.RangeCalculator;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;

public class WardenVoiceHandler {

    private final SimpleVoiceMechanics plugin;
    private final MobDetectionSupport support;

    public WardenVoiceHandler(SimpleVoiceMechanics plugin, MobDetectionSupport support) {
        this.plugin = plugin;
        this.support = support;
    }

    public void process(Warden warden, Player player, Location playerLoc, double decibels) {
        ConfigManager config = plugin.getConfigManager();

        double distance = warden.getLocation().distance(playerLoc);
        double maxRange = support.applyRangeMultiplier(player, config.getWardenMaxRange());
        double minRange = support.applyRangeMultiplier(player, config.getWardenMinRange());
        double falloffCurve = config.getWardenFalloffCurve();
        double volumeThresholdDb = config.getWardenVolumeThresholdDb();
        DetectionResult detection = support.calculateDetection(
                playerLoc, warden.getLocation(), distance, minRange, maxRange, falloffCurve,
                decibels, volumeThresholdDb
        );

        if (!detection.canAttemptDetection() || !RandomProvider.passes(detection.getChance())) {
            return;
        }

        VoiceMobReactionContext context = support.callMobReactionHooks(
                player, warden, MobReactionType.WARDEN_ANGER, detection, decibels
        );
        if (context == null) {
            return;
        }

        int angerIncrease = RangeCalculator.calculateWardenAnger(
                distance, minRange, maxRange, context.getEffectiveDecibels(), volumeThresholdDb
        );
        warden.increaseAnger(player, angerIncrease);

        if (config.isWardenLoggingEnabled()) {
            plugin.getLogger().info(String.format(
                    "Warden anger +%d | %s",
                    angerIncrease,
                    detection.getDebugInfo()
            ));
        }
    }
}

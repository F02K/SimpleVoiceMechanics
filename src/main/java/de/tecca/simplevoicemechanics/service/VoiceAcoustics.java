package de.tecca.simplevoicemechanics.service;

import de.tecca.simplevoicemechanics.manager.ConfigManager;
import de.tecca.simplevoicemechanics.util.AcousticModifiers;
import de.tecca.simplevoicemechanics.util.BiomeModifier;
import de.tecca.simplevoicemechanics.util.ObstructionMuffling;
import de.tecca.simplevoicemechanics.util.TimeModifier;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

/**
 * Shared acoustic calculations for voice-driven mob and sculk detection.
 */
public final class VoiceAcoustics {

    private VoiceAcoustics() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static double applyGlobalModifiers(Player player, double decibels, ConfigManager config) {
        World world = player.getWorld();
        Location loc = player.getLocation();
        double adjustment = 0.0;

        if (config.isBiomeModifiersEnabled()) {
            Biome biome = loc.getBlock().getBiome();
            adjustment += BiomeModifier.getThresholdAdjustment(biome);
        }

        if (config.isTimeModifiersEnabled()) {
            adjustment += TimeModifier.getThresholdAdjustment(world);
        }

        if (config.isWeatherModifiersEnabled()) {
            adjustment += AcousticModifiers.getWeatherThresholdAdjustment(
                    world.hasStorm(),
                    world.isThundering(),
                    config.getRainThresholdAdjustmentDb(),
                    config.getThunderThresholdAdjustmentDb()
            );
        }

        double modified = decibels - adjustment;
        return AcousticModifiers.applySneakingReduction(
                modified,
                config.isSneakingReductionEnabled(),
                player.isSneaking(),
                config.getSneakingDecibelReduction()
        );
    }

    public static double applyRangeMultiplier(Player player, double baseRange, ConfigManager config) {
        World world = player.getWorld();
        Location loc = player.getLocation();
        double multiplier = 1.0;

        if (config.isBiomeModifiersEnabled()) {
            multiplier *= BiomeModifier.getRangeMultiplier(loc.getBlock().getBiome());
        }

        if (config.isTimeModifiersEnabled()) {
            multiplier *= TimeModifier.getRangeMultiplier(world);
        }

        if (config.isWeatherModifiersEnabled()) {
            multiplier *= AcousticModifiers.getWeatherRangeMultiplier(
                    world.hasStorm(),
                    world.isThundering(),
                    config.getRainRangeMultiplier(),
                    config.getThunderRangeMultiplier()
            );
        }

        return baseRange * multiplier;
    }

    public static DetectionResult calculateDetection(ConfigManager config, Location sourceLoc, Location targetLoc,
                                                     double distance, double minRange, double maxRange,
                                                     double falloffCurve, double decibels,
                                                     double volumeThresholdDb) {
        DetectionResult detection = DetectionService.calculate(
                distance, minRange, maxRange, falloffCurve, decibels, volumeThresholdDb
        );
        if (!config.isObstructionMufflingEnabled() || !detection.canAttemptDetection()) {
            return detection;
        }

        double reduction = ObstructionMuffling.calculateDbReduction(
                sourceLoc,
                targetLoc,
                config.getObstructionWoolDbReduction(),
                config.getObstructionSolidBlockDbReduction(),
                config.getObstructionMaxDbReduction()
        );
        if (reduction <= 0.0) {
            return detection;
        }

        return DetectionService.calculate(
                distance, minRange, maxRange, falloffCurve, decibels - reduction, volumeThresholdDb
        );
    }
}

package de.tecca.simplevoicemechanics.service;

import de.tecca.simplevoicemechanics.util.RangeCalculator;

/**
 * Shared voice detection math for mob and sculk mechanics.
 */
public final class DetectionService {

    private DetectionService() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static DetectionResult calculate(double distance, double minRange, double maxRange,
                                            double falloffCurve, double decibels,
                                            double volumeThresholdDb) {
        double safeMinRange = Math.max(0.0, minRange);
        double safeMaxRange = Math.max(safeMinRange, maxRange);
        double safeFalloffCurve = Math.max(0.0, falloffCurve);
        boolean thresholdPassed = decibels >= volumeThresholdDb;

        double effectiveMinRange = RangeCalculator.calculateEffectiveRange(
                safeMinRange, decibels, volumeThresholdDb
        );
        double effectiveMaxRange = RangeCalculator.calculateEffectiveRange(
                safeMaxRange, decibels, volumeThresholdDb
        );
        boolean withinRange = distance <= effectiveMaxRange;

        double chance = 0.0;
        if (thresholdPassed && withinRange) {
            chance = RangeCalculator.calculateDetectionChance(
                    distance, effectiveMinRange, effectiveMaxRange, safeFalloffCurve
            );
        }

        return new DetectionResult(
                distance,
                safeMinRange,
                safeMaxRange,
                effectiveMinRange,
                effectiveMaxRange,
                safeFalloffCurve,
                decibels,
                volumeThresholdDb,
                thresholdPassed,
                withinRange,
                chance
        );
    }
}

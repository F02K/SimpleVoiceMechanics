package de.tecca.simplevoicemechanics.util;

/**
 * Utility class for range-based voice detection calculations.
 *
 * <p>Handles all detection probability calculations based on:
 * <ul>
 *   <li>Distance between player and entity</li>
 *   <li>Configured max-range and min-range</li>
 *   <li>Falloff curve (how detection chance decreases with distance)</li>
 *   <li>Audio level in decibels (dynamic range scaling)</li>
 * </ul>
 *
 * @author Tecca
 * @version 1.2.0
 */
public class RangeCalculator {

    /**
     * Private constructor to prevent instantiation.
     */
    private RangeCalculator() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    /**
     * Calculates effective detection range based on audio level.
     *
     * <p>Louder voices are heard from further away:
     * <ul>
     *   <li>-60 dB (whisper): ~50% of configured range</li>
     *   <li>-40 dB (normal): ~100% of configured range</li>
     *   <li>-20 dB (loud): ~150% of configured range</li>
     *   <li>0 dB (shout): ~200% of configured range</li>
     * </ul>
     *
     * @param configuredRange the base range from config
     * @param decibels the audio level in decibels (-127 to 0)
     * @param volumeThresholdDb the minimum volume threshold
     * @return effective range in blocks
     */
    public static double calculateEffectiveRange(double configuredRange, double decibels,
                                                 double volumeThresholdDb) {
        double safeRange = Math.max(0.0, configuredRange);

        // Normalize dB relative to threshold
        // At threshold: multiplier = 1.0
        // Above threshold: multiplier increases
        double dbAboveThreshold = decibels - volumeThresholdDb;

        // Each 10 dB above threshold increases range by ~40%
        // Formula: multiplier = 1.0 + (dbAboveThreshold / 25.0)
        // Examples:
        //   At threshold (0 dB above): 1.0x range
        //   +10 dB: 1.4x range
        //   +20 dB: 1.8x range
        //   +30 dB: 2.2x range
        double multiplier = Math.max(0.5, 1.0 + (dbAboveThreshold / 25.0));

        // Cap maximum multiplier at 2.5x
        multiplier = Math.min(multiplier, 2.5);

        return safeRange * multiplier;
    }

    /**
     * Calculates detection chance based on distance and range settings.
     *
     * <p>Formula:
     * <ol>
     *   <li>If distance ≤ min-range: 100% chance</li>
     *   <li>If distance ≥ max-range: 0% chance</li>
     *   <li>Otherwise: Smooth falloff based on falloff-curve</li>
     * </ol>
     *
     * <p>Falloff curve explanation:
     * <ul>
     *   <li>0.5 = Linear falloff (50% at midpoint)</li>
     *   <li>1.0 = Moderate falloff (stays higher longer)</li>
     *   <li>1.5 = Strong falloff (barely drops until far out)</li>
     *   <li>2.0 = Very strong falloff (almost flat until edge)</li>
     * </ul>
     *
     * @param distance the distance in blocks
     * @param minRange the minimum range (100% detection)
     * @param maxRange the maximum range (0% detection)
     * @param falloffCurve the falloff curve exponent (0.5 - 2.0)
     * @return detection chance (0.0 - 1.0)
     */
    public static double calculateDetectionChance(double distance, double minRange,
                                                  double maxRange, double falloffCurve) {
        double safeMinRange = Math.max(0.0, minRange);
        double safeMaxRange = Math.max(safeMinRange, maxRange);
        double safeFalloffCurve = Math.max(0.0, falloffCurve);

        // Within min range: always detect
        if (distance <= safeMinRange) {
            return 1.0;
        }

        // Beyond max range: never detect
        if (distance >= safeMaxRange) {
            return 0.0;
        }

        // Calculate normalized distance (0.0 at min-range, 1.0 at max-range)
        double normalizedDistance = (distance - safeMinRange) / (safeMaxRange - safeMinRange);

        // Apply falloff curve (inverted so 1.0 = 100% at min-range, 0.0 = 0% at max-range)
        // Higher falloff curve = detection chance stays high longer
        double chance = Math.pow(1.0 - normalizedDistance, safeFalloffCurve);

        return Math.max(0.0, Math.min(1.0, chance));
    }

    /**
     * Calculates detection chance with dynamic range based on audio level.
     *
     * <p>Combines audio level and distance for realistic detection:
     * <ol>
     *   <li>Calculate effective range based on audio level</li>
     *   <li>Scale min/max ranges proportionally</li>
     *   <li>Apply standard detection chance calculation</li>
     * </ol>
     *
     * @param distance the distance in blocks
     * @param configuredMinRange the configured minimum range
     * @param configuredMaxRange the configured maximum range
     * @param falloffCurve the falloff curve exponent
     * @param decibels the audio level in decibels
     * @param volumeThresholdDb the minimum volume threshold
     * @return detection chance (0.0 - 1.0)
     */
    public static double calculateDynamicDetectionChance(double distance, double configuredMinRange,
                                                         double configuredMaxRange, double falloffCurve,
                                                         double decibels, double volumeThresholdDb) {
        // Calculate range multiplier based on volume
        double effectiveMaxRange = calculateEffectiveRange(configuredMaxRange, decibels, volumeThresholdDb);
        double effectiveMinRange = calculateEffectiveRange(configuredMinRange, decibels, volumeThresholdDb);

        // Use effective ranges for detection
        return calculateDetectionChance(distance, effectiveMinRange, effectiveMaxRange, falloffCurve);
    }

    /**
     * Calculates Warden anger based on distance and audio level.
     *
     * <p>Anger scales with both proximity and volume:
     * <ul>
     *   <li>Close + Loud: 50-60 anger</li>
     *   <li>Medium + Normal: 30-50 anger</li>
     *   <li>Far + Quiet: 15-30 anger</li>
     * </ul>
     *
     * @param distance the distance in blocks
     * @param minRange the minimum range
     * @param maxRange the maximum range
     * @param decibels the audio level in decibels
     * @param volumeThresholdDb the volume threshold
     * @return anger increase amount
     */
    public static int calculateWardenAnger(double distance, double minRange, double maxRange,
                                           double decibels, double volumeThresholdDb) {
        // Base anger values
        int minAnger = 15;
        int maxAnger = 60;

        // Calculate effective range for volume
        double effectiveMaxRange = calculateEffectiveRange(maxRange, decibels, volumeThresholdDb);

        // Within min range: maximum anger
        if (distance <= minRange) {
            return maxAnger;
        }

        // Beyond effective max range: minimum anger
        if (distance >= effectiveMaxRange) {
            return minAnger;
        }

        // Calculate anger based on distance (closer = more angry)
        double normalizedDistance = (distance - minRange) / (effectiveMaxRange - minRange);
        double angerFactor = 1.0 - normalizedDistance;

        // Non-linear scaling (closer distances = disproportionately more anger)
        angerFactor = Math.pow(angerFactor, 1.5);

        // Volume bonus: louder = more angry
        double volumeFactor = (decibels - volumeThresholdDb) / 40.0; // Normalized to ~0-1
        volumeFactor = Math.max(0.0, Math.min(1.0, volumeFactor));
        angerFactor = angerFactor * (0.7 + 0.3 * volumeFactor); // 70-100% based on volume

        int anger = (int) (minAnger + (maxAnger - minAnger) * angerFactor);
        return Math.max(minAnger, Math.min(maxAnger, anger));
    }

    /**
     * Gets debug information for range calculations.
     *
     * @param distance the distance
     * @param minRange the min range
     * @param maxRange the max range
     * @param falloffCurve the falloff curve
     * @return formatted debug string
     */
    public static String getDebugInfo(double distance, double minRange, double maxRange, double falloffCurve) {
        double chance = calculateDetectionChance(distance, minRange, maxRange, falloffCurve);

        String rangeZone;
        if (distance <= minRange) {
            rangeZone = "GUARANTEED";
        } else if (distance >= maxRange) {
            rangeZone = "OUT_OF_RANGE";
        } else {
            double percent = ((distance - minRange) / (maxRange - minRange)) * 100;
            rangeZone = String.format("%.0f%%", percent);
        }

        return String.format(
                "Dist: %.1f | Zone: %s | Chance: %.1f%% | Curve: %.1f",
                distance, rangeZone, chance * 100, falloffCurve
        );
    }

    /**
     * Gets debug information with dynamic range.
     *
     * @param distance the distance
     * @param configuredMinRange the configured min range
     * @param configuredMaxRange the configured max range
     * @param falloffCurve the falloff curve
     * @param decibels the audio level
     * @param volumeThresholdDb the volume threshold
     * @return formatted debug string
     */
    public static String getDynamicDebugInfo(double distance, double configuredMinRange,
                                             double configuredMaxRange, double falloffCurve,
                                             double decibels, double volumeThresholdDb) {
        double effectiveMax = calculateEffectiveRange(configuredMaxRange, decibels, volumeThresholdDb);
        double effectiveMin = calculateEffectiveRange(configuredMinRange, decibels, volumeThresholdDb);
        double chance = calculateDynamicDetectionChance(distance, configuredMinRange, configuredMaxRange,
                falloffCurve, decibels, volumeThresholdDb);

        return String.format(
                "Dist: %.1f | dB: %.1f | Range: %.1f-%.1f | Chance: %.1f%%",
                distance, decibels, effectiveMin, effectiveMax, chance * 100
        );
    }

    /**
     * Gets example detection chances at various distances.
     *
     * @param minRange the min range
     * @param maxRange the max range
     * @param falloffCurve the falloff curve
     * @return formatted example table
     */
    public static String getExampleTable(double minRange, double maxRange, double falloffCurve) {
        StringBuilder sb = new StringBuilder();
        sb.append("Detection Chances:\n");

        double[] testDistances = {
                minRange * 0.5,
                minRange,
                minRange + (maxRange - minRange) * 0.25,
                minRange + (maxRange - minRange) * 0.5,
                minRange + (maxRange - minRange) * 0.75,
                maxRange,
                maxRange * 1.5
        };

        for (double dist : testDistances) {
            double chance = calculateDetectionChance(dist, minRange, maxRange, falloffCurve);
            sb.append(String.format("  %.1f blocks: %.1f%%\n", dist, chance * 100));
        }

        return sb.toString();
    }
}

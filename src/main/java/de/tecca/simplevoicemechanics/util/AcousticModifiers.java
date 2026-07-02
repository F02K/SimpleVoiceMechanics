package de.tecca.simplevoicemechanics.util;

/**
 * Small, bounded acoustic adjustments used by voice detection.
 */
public final class AcousticModifiers {

    private AcousticModifiers() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static double applySneakingReduction(double decibels, boolean enabled,
                                                boolean sneaking, double reductionDb) {
        if (!enabled || !sneaking) {
            return decibels;
        }
        return decibels - Math.max(0.0, reductionDb);
    }

    public static double getWeatherRangeMultiplier(boolean storming, boolean thundering,
                                                   double rainMultiplier, double thunderMultiplier) {
        if (thundering) {
            return Math.max(0.0, thunderMultiplier);
        }
        if (storming) {
            return Math.max(0.0, rainMultiplier);
        }
        return 1.0;
    }

    public static double getWeatherThresholdAdjustment(boolean storming, boolean thundering,
                                                       double rainAdjustmentDb,
                                                       double thunderAdjustmentDb) {
        if (thundering) {
            return thunderAdjustmentDb;
        }
        if (storming) {
            return rainAdjustmentDb;
        }
        return 0.0;
    }

    public static double calculateObstructionReduction(int woolBlocks, int solidBlocks,
                                                       double woolReductionDb,
                                                       double solidReductionDb,
                                                       double maxReductionDb) {
        double reduction = Math.max(0, woolBlocks) * Math.max(0.0, woolReductionDb)
                + Math.max(0, solidBlocks) * Math.max(0.0, solidReductionDb);
        return Math.min(Math.max(0.0, maxReductionDb), reduction);
    }
}

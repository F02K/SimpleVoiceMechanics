package de.tecca.simplevoicemechanics.service;

/**
 * Pure decision helper for escalating heard voice from investigation to targeting.
 */
public final class HostileSuspicionMath {

    private HostileSuspicionMath() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static boolean shouldTarget(int detectionsWithinMemory, int detectionsToTarget,
                                       double distance, double effectiveMinRange,
                                       double decibels, double thresholdDb) {
        int safeRequiredDetections = Math.max(1, detectionsToTarget);
        if (detectionsWithinMemory >= safeRequiredDetections) {
            return true;
        }
        if (distance <= Math.max(0.0, effectiveMinRange)) {
            return true;
        }
        return decibels >= thresholdDb + 20.0;
    }
}

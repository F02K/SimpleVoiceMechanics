package de.tecca.simplevoicemechanics.service;

/**
 * Small helpers for voice-driven sculk vibration visuals.
 */
public final class SculkVibrationMath {

    private SculkVibrationMath() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static int calculateArrivalTicks(double distance, int minArrivalTicks, int maxArrivalTicks) {
        int safeMin = Math.max(0, minArrivalTicks);
        int safeMax = Math.max(safeMin, maxArrivalTicks);
        int distanceTicks = (int) Math.ceil(Math.max(0.0, distance));
        return Math.max(safeMin, Math.min(safeMax, distanceTicks));
    }
}

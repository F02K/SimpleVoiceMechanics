package de.tecca.simplevoicemechanics.service;

/**
 * Pure helpers for last-heard-location investigation behavior.
 */
public final class InvestigationMath {

    private InvestigationMath() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static boolean hasMovedBeyondTolerance(double distanceSquared, double tolerance) {
        double safeTolerance = Math.max(0.0, tolerance);
        return distanceSquared > safeTolerance * safeTolerance;
    }
}

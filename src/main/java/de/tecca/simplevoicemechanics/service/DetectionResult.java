package de.tecca.simplevoicemechanics.service;

/**
 * Immutable result of a voice detection range calculation.
 */
public final class DetectionResult {

    private final double distance;
    private final double configuredMinRange;
    private final double configuredMaxRange;
    private final double effectiveMinRange;
    private final double effectiveMaxRange;
    private final double falloffCurve;
    private final double decibels;
    private final double volumeThresholdDb;
    private final boolean thresholdPassed;
    private final boolean withinRange;
    private final double chance;

    DetectionResult(double distance, double configuredMinRange, double configuredMaxRange,
                    double effectiveMinRange, double effectiveMaxRange, double falloffCurve,
                    double decibels, double volumeThresholdDb, boolean thresholdPassed,
                    boolean withinRange, double chance) {
        this.distance = distance;
        this.configuredMinRange = configuredMinRange;
        this.configuredMaxRange = configuredMaxRange;
        this.effectiveMinRange = effectiveMinRange;
        this.effectiveMaxRange = effectiveMaxRange;
        this.falloffCurve = falloffCurve;
        this.decibels = decibels;
        this.volumeThresholdDb = volumeThresholdDb;
        this.thresholdPassed = thresholdPassed;
        this.withinRange = withinRange;
        this.chance = chance;
    }

    public double getDistance() {
        return distance;
    }

    public double getConfiguredMinRange() {
        return configuredMinRange;
    }

    public double getConfiguredMaxRange() {
        return configuredMaxRange;
    }

    public double getEffectiveMinRange() {
        return effectiveMinRange;
    }

    public double getEffectiveMaxRange() {
        return effectiveMaxRange;
    }

    public double getFalloffCurve() {
        return falloffCurve;
    }

    public double getDecibels() {
        return decibels;
    }

    public double getVolumeThresholdDb() {
        return volumeThresholdDb;
    }

    public boolean isThresholdPassed() {
        return thresholdPassed;
    }

    public boolean isWithinRange() {
        return withinRange;
    }

    public double getChance() {
        return chance;
    }

    public boolean canAttemptDetection() {
        return thresholdPassed && withinRange && chance > 0.0;
    }

    public String getDebugInfo() {
        return String.format(
                "Dist: %.1f | dB: %.1f | Range: %.1f-%.1f | Chance: %.1f%%",
                distance,
                decibels,
                effectiveMinRange,
                effectiveMaxRange,
                chance * 100
        );
    }
}

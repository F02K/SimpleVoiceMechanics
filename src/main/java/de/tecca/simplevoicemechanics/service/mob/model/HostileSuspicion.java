package de.tecca.simplevoicemechanics.service.mob.model;

public final class HostileSuspicion {

    private final long lastDetectionTime;
    private final int detections;

    public HostileSuspicion(long lastDetectionTime, int detections) {
        this.lastDetectionTime = lastDetectionTime;
        this.detections = detections;
    }

    public long getLastDetectionTime() {
        return lastDetectionTime;
    }

    public int getDetections() {
        return detections;
    }
}

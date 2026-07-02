package de.tecca.simplevoicemechanics.service.mob.model;

import org.bukkit.Location;

import java.util.UUID;

public final class HostileInvestigation {

    private final UUID playerId;
    private final Location heardLocation;
    private final long startTime;
    private final HostileInvestigationType type;
    private boolean targetedPlayer;

    public HostileInvestigation(UUID playerId, Location heardLocation, long startTime,
                                HostileInvestigationType type) {
        this.playerId = playerId;
        this.heardLocation = heardLocation;
        this.startTime = startTime;
        this.type = type;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Location getHeardLocation() {
        return heardLocation;
    }

    public long getStartTime() {
        return startTime;
    }

    public HostileInvestigationType getType() {
        return type;
    }

    public boolean hasTargetedPlayer() {
        return targetedPlayer;
    }

    public void setTargetedPlayer(boolean targetedPlayer) {
        this.targetedPlayer = targetedPlayer;
    }
}

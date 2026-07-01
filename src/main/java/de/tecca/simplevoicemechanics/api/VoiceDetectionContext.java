package de.tecca.simplevoicemechanics.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Mutable context for player voice detection.
 */
public final class VoiceDetectionContext {

    private final Player player;
    private final Location location;
    private final double rawDecibels;
    private double effectiveDecibels;
    private boolean cancelled;

    public VoiceDetectionContext(Player player, Location location, double rawDecibels, double effectiveDecibels) {
        this.player = player;
        this.location = location;
        this.rawDecibels = rawDecibels;
        this.effectiveDecibels = effectiveDecibels;
    }

    public Player getPlayer() {
        return player;
    }

    public Location getLocation() {
        return location;
    }

    public double getRawDecibels() {
        return rawDecibels;
    }

    public double getEffectiveDecibels() {
        return effectiveDecibels;
    }

    /**
     * Sets the dB value used by downstream Bukkit events and mechanics.
     *
     * @param effectiveDecibels adjusted audio level in dB
     */
    public void setEffectiveDecibels(double effectiveDecibels) {
        this.effectiveDecibels = effectiveDecibels;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}

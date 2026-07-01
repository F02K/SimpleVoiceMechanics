package de.tecca.simplevoicemechanics.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a player's voice is detected above the configured threshold.
 *
 * <p>This event provides:
 * <ul>
 *   <li>The speaking player</li>
 *   <li>The location where voice was detected</li>
 *   <li>The audio level in decibels</li>
 * </ul>
 *
 * <p>Audio level is measured in decibels (dB) from -127 (silence) to 0 (maximum).
 * Typical values:
 * <ul>
 *   <li>-100 to -80 dB: Very quiet</li>
 *   <li>-60 to -40 dB: Quiet speaking</li>
 *   <li>-40 to -20 dB: Normal speaking</li>
 *   <li>-20 to 0 dB: Loud speaking/shouting</li>
 * </ul>
 *
 * @author Tecca
 * @version 1.2.0
 */
public class VoiceDetectedEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Location location;
    private final double decibels;
    private boolean cancelled;

    /**
     * Constructs a new VoiceDetectedEvent.
     *
     * @param player the player who spoke
     * @param location the location where voice was detected
     * @param decibels the audio level in decibels (-127 to 0)
     */
    public VoiceDetectedEvent(Player player, Location location, double decibels) {
        this.player = player;
        this.location = location;
        this.decibels = decibels;
    }

    /**
     * Gets the player who spoke.
     *
     * @return the speaking player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the location where voice was detected.
     *
     * @return the voice location
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the audio level in decibels.
     *
     * <p>Range: -127 dB (silence) to 0 dB (maximum volume)
     *
     * @return the audio level in decibels
     */
    public double getDecibels() {
        return decibels;
    }

    /**
     * Gets the normalized volume (0.0 - 1.0).
     *
     * <p>Convenience method that converts decibels to a normalized value:
     * <ul>
     *   <li>-127 dB → 0.0</li>
     *   <li>-63.5 dB → 0.5</li>
     *   <li>0 dB → 1.0</li>
     * </ul>
     *
     * @return normalized volume between 0.0 and 1.0
     */
    public double getNormalizedVolume() {
        return (decibels + 127.0) / 127.0;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

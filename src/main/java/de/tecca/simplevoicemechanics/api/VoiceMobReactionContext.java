package de.tecca.simplevoicemechanics.api;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

/**
 * Mutable context for mob reactions caused by voice.
 */
public final class VoiceMobReactionContext {

    private final Player player;
    private final Mob mob;
    private final MobReactionType reactionType;
    private final double distance;
    private final double detectionChance;
    private double effectiveDecibels;
    private boolean cancelled;

    public VoiceMobReactionContext(Player player, Mob mob, MobReactionType reactionType,
                                   double distance, double detectionChance, double effectiveDecibels) {
        this.player = player;
        this.mob = mob;
        this.reactionType = reactionType;
        this.distance = distance;
        this.detectionChance = detectionChance;
        this.effectiveDecibels = effectiveDecibels;
    }

    public Player getPlayer() {
        return player;
    }

    public Mob getMob() {
        return mob;
    }

    public MobReactionType getReactionType() {
        return reactionType;
    }

    public double getDistance() {
        return distance;
    }

    public double getDetectionChance() {
        return detectionChance;
    }

    public double getEffectiveDecibels() {
        return effectiveDecibels;
    }

    /**
     * Sets the dB value used by the final reaction effect when applicable.
     *
     * <p>For example, changing this value affects Warden anger calculation.
     * Detection range and chance have already been calculated for this reaction.
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

package de.tecca.simplevoicemechanics.api;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Mutable context for sculk sensor activations caused by voice.
 */
public final class VoiceSculkActivationContext {

    private final Player player;
    private final Block block;
    private final double distance;
    private final double activationChance;
    private double effectiveDecibels;
    private boolean cancelled;

    public VoiceSculkActivationContext(Player player, Block block, double distance,
                                       double activationChance, double effectiveDecibels) {
        this.player = player;
        this.block = block;
        this.distance = distance;
        this.activationChance = activationChance;
        this.effectiveDecibels = effectiveDecibels;
    }

    public Player getPlayer() {
        return player;
    }

    public Block getBlock() {
        return block;
    }

    public double getDistance() {
        return distance;
    }

    public double getActivationChance() {
        return activationChance;
    }

    public double getEffectiveDecibels() {
        return effectiveDecibels;
    }

    /**
     * Sets the dB value rechecked before the final sculk activation.
     *
     * <p>Lowering this below the sculk threshold or effective range prevents
     * activation. Raising it does not reroll activation probability.
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

package de.tecca.simplevoicemechanics.api;

/**
 * Hook called immediately before a mob reaction is applied.
 */
@FunctionalInterface
public interface VoiceMobReactionHook {
    void handleMobReaction(VoiceMobReactionContext context);
}

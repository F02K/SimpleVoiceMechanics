package de.tecca.simplevoicemechanics.api;

/**
 * Hook called immediately before a sculk sensor is activated by voice.
 */
@FunctionalInterface
public interface VoiceSculkActivationHook {
    void handleSculkActivation(VoiceSculkActivationContext context);
}

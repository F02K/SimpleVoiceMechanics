package de.tecca.simplevoicemechanics.api;

/**
 * Hook called after voice audio is decoded and before Bukkit voice events are fired.
 */
@FunctionalInterface
public interface VoiceDetectionHook {
    void handleVoiceDetection(VoiceDetectionContext context);
}

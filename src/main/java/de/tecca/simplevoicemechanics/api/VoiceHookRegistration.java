package de.tecca.simplevoicemechanics.api;

import org.bukkit.plugin.Plugin;

/**
 * Handle returned when registering a SimpleVoiceMechanics hook.
 */
public interface VoiceHookRegistration {

    Plugin getOwner();

    HookPriority getPriority();

    boolean isRegistered();

    void unregister();
}

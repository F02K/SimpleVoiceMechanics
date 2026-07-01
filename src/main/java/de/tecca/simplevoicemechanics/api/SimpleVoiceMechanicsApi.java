package de.tecca.simplevoicemechanics.api;

import org.bukkit.plugin.Plugin;

/**
 * Bukkit service API for integrating with SimpleVoiceMechanics.
 *
 * <p>All hooks are called on the main server thread. Hooks run in ascending
 * {@link HookPriority} order. If a hook throws an exception, the failure is
 * logged and later hooks still run.
 */
public interface SimpleVoiceMechanicsApi {

    VoiceHookRegistration registerDetectionHook(Plugin owner, HookPriority priority, VoiceDetectionHook hook);

    VoiceHookRegistration registerMobReactionHook(Plugin owner, HookPriority priority, VoiceMobReactionHook hook);

    VoiceHookRegistration registerSculkActivationHook(Plugin owner, HookPriority priority,
                                                      VoiceSculkActivationHook hook);

    void unregisterHooks(Plugin owner);

    int getDetectionHookCount();

    int getMobReactionHookCount();

    int getSculkActivationHookCount();
}

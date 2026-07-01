package de.tecca.simplevoicemechanics.api;

/**
 * Mob reaction categories exposed to integration hooks.
 */
public enum MobReactionType {
    HOSTILE_TARGET,
    HOSTILE_INVESTIGATE,
    GROUP_ALERT,
    NEUTRAL_LOOK,
    PEACEFUL_LOOK,
    PEACEFUL_FLEE,
    PEACEFUL_FOLLOW,
    WARDEN_ANGER
}

package de.tecca.simplevoicemechanics.util;

import org.bukkit.permissions.Permissible;

/**
 * Canonical permission names plus legacy aliases kept for existing servers.
 */
public final class PluginPermissions {

    public static final String ADMIN = "simplevoicemechanics.admin";
    public static final String BYPASS = "simplevoicemechanics.bypass";
    public static final String LEGACY_ADMIN = "voicelistener.admin";
    public static final String LEGACY_BYPASS = "voicelistener.bypass";

    private PluginPermissions() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static boolean hasAdmin(Permissible permissible) {
        return permissible.hasPermission(ADMIN) || permissible.hasPermission(LEGACY_ADMIN);
    }

    public static boolean hasBypass(Permissible permissible) {
        return permissible.hasPermission(BYPASS) || permissible.hasPermission(LEGACY_BYPASS);
    }
}

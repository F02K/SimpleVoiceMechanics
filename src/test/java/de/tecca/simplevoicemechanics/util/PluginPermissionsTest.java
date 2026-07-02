package de.tecca.simplevoicemechanics.util;

import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginPermissionsTest {

    @Test
    void acceptsCanonicalAndLegacyAdminPermissions() {
        assertTrue(PluginPermissions.hasAdmin(new FakePermissible(PluginPermissions.ADMIN)));
        assertTrue(PluginPermissions.hasAdmin(new FakePermissible(PluginPermissions.LEGACY_ADMIN)));
        assertFalse(PluginPermissions.hasAdmin(new FakePermissible()));
    }

    @Test
    void acceptsCanonicalAndLegacyBypassPermissions() {
        assertTrue(PluginPermissions.hasBypass(new FakePermissible(PluginPermissions.BYPASS)));
        assertTrue(PluginPermissions.hasBypass(new FakePermissible(PluginPermissions.LEGACY_BYPASS)));
        assertFalse(PluginPermissions.hasBypass(new FakePermissible()));
    }

    private static final class FakePermissible implements Permissible {
        private final Set<String> permissions;
        private boolean op;

        private FakePermissible(String... permissions) {
            this.permissions = new HashSet<>();
            Collections.addAll(this.permissions, permissions);
        }

        @Override
        public boolean isPermissionSet(String name) {
            return permissions.contains(name);
        }

        @Override
        public boolean isPermissionSet(Permission perm) {
            return isPermissionSet(perm.getName());
        }

        @Override
        public boolean hasPermission(String name) {
            return permissions.contains(name);
        }

        @Override
        public boolean hasPermission(Permission perm) {
            return hasPermission(perm.getName());
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeAttachment(PermissionAttachment attachment) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void recalculatePermissions() {
        }

        @Override
        public Set<PermissionAttachmentInfo> getEffectivePermissions() {
            return Collections.emptySet();
        }

        @Override
        public boolean isOp() {
            return op;
        }

        @Override
        public void setOp(boolean value) {
            op = value;
        }
    }
}

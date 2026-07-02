package de.tecca.simplevoicemechanics.service.mob.tracker;

import de.tecca.simplevoicemechanics.SimpleVoiceMechanics;
import de.tecca.simplevoicemechanics.manager.ConfigManager;
import de.tecca.simplevoicemechanics.util.MobCondition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class PeacefulFollowTracker {

    private static final String FOLLOW_META_KEY = "svm_following";
    private static final String FOLLOW_PLAYER_KEY = "svm_follow_player";
    private static final String EYE_CONTACT_META_KEY = "svm_eye_contact";
    private static final String EYE_CONTACT_TIME_KEY = "svm_eye_contact_time";

    private final SimpleVoiceMechanics plugin;
    private final Map<UUID, UUID> followingMobs = new HashMap<>();
    private final Map<UUID, Long> followStartTime = new HashMap<>();

    public PeacefulFollowTracker(SimpleVoiceMechanics plugin) {
        this.plugin = plugin;
    }

    public void startFollowing(Mob mob, Player player) {
        UUID mobId = mob.getUniqueId();
        UUID playerId = player.getUniqueId();

        followingMobs.put(mobId, playerId);
        followStartTime.put(mobId, System.currentTimeMillis());

        mob.setMetadata(FOLLOW_META_KEY, new FixedMetadataValue(plugin, true));
        mob.setMetadata(FOLLOW_PLAYER_KEY, new FixedMetadataValue(plugin, playerId.toString()));
    }

    public boolean isPlayerLookingAtMob(Player player, Mob mob, double range) {
        Location playerEye = player.getEyeLocation();
        Location mobLoc = mob.getLocation().add(0, mob.getHeight() / 2, 0);

        double distance = playerEye.distance(mobLoc);
        if (distance > range) {
            return false;
        }

        org.bukkit.util.Vector toMob = mobLoc.toVector().subtract(playerEye.toVector()).normalize();
        org.bukkit.util.Vector playerDirection = playerEye.getDirection();

        double dotProduct = toMob.dot(playerDirection);
        return dotProduct > 0.7;
    }

    public void recordEyeContact(Mob mob, Player player) {
        mob.setMetadata(EYE_CONTACT_META_KEY, new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        mob.setMetadata(EYE_CONTACT_TIME_KEY, new FixedMetadataValue(plugin, System.currentTimeMillis()));
    }

    public boolean hasRecentEyeContact(Mob mob, Player player) {
        if (!mob.hasMetadata(EYE_CONTACT_META_KEY)) {
            return false;
        }

        String contactedPlayer = mob.getMetadata(EYE_CONTACT_META_KEY).get(0).asString();
        if (!contactedPlayer.equals(player.getUniqueId().toString())) {
            return false;
        }

        if (!mob.hasMetadata(EYE_CONTACT_TIME_KEY)) {
            return false;
        }

        long contactTime = mob.getMetadata(EYE_CONTACT_TIME_KEY).get(0).asLong();
        long currentTime = System.currentTimeMillis();
        long memoryDurationMs = plugin.getConfigManager().getEyeContactMemoryMs();

        return (currentTime - contactTime) < memoryDurationMs;
    }

    public void updateFollowing(long currentTime, ConfigManager config, Consumer<UUID> cleanupCallback) {
        long maxDuration = config.getFollowDuration() * 1000L;
        double maxDistance = config.getFollowMaxDistance();

        followingMobs.entrySet().removeIf(entry -> {
            UUID mobId = entry.getKey();
            UUID playerId = entry.getValue();

            Long startTime = followStartTime.get(mobId);
            if (startTime == null || (currentTime - startTime) > maxDuration) {
                cleanupMob(mobId, cleanupCallback);
                return true;
            }

            Entity mobEntity = Bukkit.getEntity(mobId);
            Player player = Bukkit.getPlayer(playerId);

            if (mobEntity == null || player == null || !player.isOnline()) {
                cleanupMob(mobId, cleanupCallback);
                return true;
            }

            if (mobEntity instanceof Mob) {
                Mob mob = (Mob) mobEntity;

                if (!MobCondition.canFollow(mob)) {
                    cleanupMob(mobId, cleanupCallback);
                    return true;
                }

                double distance = mob.getLocation().distance(player.getLocation());

                if (distance > maxDistance) {
                    cleanupMob(mobId, cleanupCallback);
                    return true;
                }

                mob.getPathfinder().moveTo(player.getLocation());
            }

            return false;
        });
    }

    public void cleanupMob(UUID mobId, Consumer<UUID> cleanupCallback) {
        Entity entity = Bukkit.getEntity(mobId);
        if (entity != null) {
            entity.removeMetadata(FOLLOW_META_KEY, plugin);
            entity.removeMetadata(FOLLOW_PLAYER_KEY, plugin);

            if (entity instanceof Mob) {
                ((Mob) entity).getPathfinder().stopPathfinding();
            }
        }

        followStartTime.remove(mobId);
        cleanupCallback.accept(mobId);
    }
}

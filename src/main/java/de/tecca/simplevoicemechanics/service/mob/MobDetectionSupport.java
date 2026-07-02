package de.tecca.simplevoicemechanics.service.mob;

import de.tecca.simplevoicemechanics.SimpleVoiceMechanics;
import de.tecca.simplevoicemechanics.api.MobReactionType;
import de.tecca.simplevoicemechanics.api.VoiceMobReactionContext;
import de.tecca.simplevoicemechanics.manager.ConfigManager;
import de.tecca.simplevoicemechanics.service.DetectionResult;
import de.tecca.simplevoicemechanics.service.VoiceAcoustics;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MobDetectionSupport {

    private final SimpleVoiceMechanics plugin;
    private final Map<UUID, Long> reactionCooldowns = new HashMap<>();

    public MobDetectionSupport(SimpleVoiceMechanics plugin) {
        this.plugin = plugin;
    }

    public ConfigManager config() {
        return plugin.getConfigManager();
    }

    public double applyRangeMultiplier(Player player, double baseRange) {
        return VoiceAcoustics.applyRangeMultiplier(player, baseRange, config());
    }

    public DetectionResult calculateDetection(Location sourceLoc, Location targetLoc,
                                              double distance, double minRange, double maxRange,
                                              double falloffCurve, double decibels,
                                              double volumeThresholdDb) {
        return VoiceAcoustics.calculateDetection(
                config(),
                sourceLoc,
                targetLoc,
                distance,
                minRange,
                maxRange,
                falloffCurve,
                decibels,
                volumeThresholdDb
        );
    }

    public boolean isOnReactionCooldown(Mob mob, long cooldownMs) {
        Long lastReaction = reactionCooldowns.get(mob.getUniqueId());
        if (lastReaction == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        return (currentTime - lastReaction) < cooldownMs;
    }

    public void setReactionCooldown(Mob mob) {
        reactionCooldowns.put(mob.getUniqueId(), System.currentTimeMillis());
    }

    public void cleanupReactionCooldowns(long currentTime) {
        reactionCooldowns.entrySet().removeIf(entry -> {
            Entity entity = Bukkit.getEntity(entry.getKey());
            return entity == null || !entity.isValid() || (currentTime - entry.getValue()) > 30000L;
        });
    }

    public void clearReactionCooldown(UUID mobId) {
        reactionCooldowns.remove(mobId);
    }

    public void makeMobLookAtWithDuration(Mob mob, Location target, int durationTicks) {
        makeMobLookAt(mob, target);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (mob.isValid()) {
                mob.getPathfinder().stopPathfinding();
            }
        }, durationTicks);
    }

    public void makeMobLookAt(Mob mob, Location target) {
        Location mobLoc = mob.getLocation();
        Location direction = target.clone().subtract(mobLoc);

        mobLoc.setDirection(direction.toVector());
        mob.teleport(mobLoc);
    }

    public boolean isValidGameMode(Player player) {
        GameMode mode = player.getGameMode();
        return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
    }

    public boolean isInvisible(Player player) {
        return player.hasPotionEffect(PotionEffectType.INVISIBILITY);
    }

    public VoiceMobReactionContext callMobReactionHooks(Player player, Mob mob, MobReactionType reactionType,
                                                       DetectionResult detection, double decibels) {
        VoiceMobReactionContext context = new VoiceMobReactionContext(
                player, mob, reactionType, detection.getDistance(), detection.getChance(), decibels
        );
        plugin.getApi().callMobReactionHooks(context);
        return context.isCancelled() ? null : context;
    }

    public void clearTargetIfMatching(Mob mob, Player player) {
        if (mob.getTarget() != null && mob.getTarget().equals(player)) {
            mob.setTarget(null);
        }
    }
}

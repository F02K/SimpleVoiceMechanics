package de.tecca.simplevoicemechanics.service.mob.tracker;

import de.tecca.simplevoicemechanics.SimpleVoiceMechanics;
import de.tecca.simplevoicemechanics.api.MobReactionType;
import de.tecca.simplevoicemechanics.api.VoiceMobReactionContext;
import de.tecca.simplevoicemechanics.manager.ConfigManager;
import de.tecca.simplevoicemechanics.service.DetectionResult;
import de.tecca.simplevoicemechanics.service.InvestigationMath;
import de.tecca.simplevoicemechanics.service.mob.MobDetectionSupport;
import de.tecca.simplevoicemechanics.service.mob.model.HostileInvestigation;
import de.tecca.simplevoicemechanics.service.mob.model.HostileInvestigationType;
import de.tecca.simplevoicemechanics.service.mob.model.HostileSuspicion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HostileInvestigationTracker {

    private static final double INVESTIGATION_ARRIVAL_DISTANCE = 2.5;

    private final SimpleVoiceMechanics plugin;
    private final MobDetectionSupport support;
    private final Map<UUID, HostileInvestigation> hostileInvestigations = new HashMap<>();
    private final Map<String, HostileSuspicion> hostileSuspicions = new HashMap<>();

    public HostileInvestigationTracker(SimpleVoiceMechanics plugin, MobDetectionSupport support) {
        this.plugin = plugin;
        this.support = support;
    }

    public boolean startInvestigation(Mob mob, Player player, Location heardLocation,
                                      DetectionResult detection, double decibels,
                                      HostileInvestigationType type) {
        VoiceMobReactionContext context = support.callMobReactionHooks(
                player, mob, MobReactionType.HOSTILE_INVESTIGATE, detection, decibels
        );
        if (context == null) {
            return false;
        }

        Location target = heardLocation.clone();
        if (type == HostileInvestigationType.INVISIBLE_SOUND) {
            support.clearTargetIfMatching(mob, player);
        }
        hostileInvestigations.put(
                mob.getUniqueId(),
                new HostileInvestigation(player.getUniqueId(), target, System.currentTimeMillis(), type)
        );
        mob.getPathfinder().moveTo(target);
        return true;
    }

    public int recordSuspicion(Mob mob, Player player, int memorySeconds) {
        long currentTime = System.currentTimeMillis();
        String key = getSuspicionKey(mob.getUniqueId(), player.getUniqueId());
        HostileSuspicion suspicion = hostileSuspicions.get(key);
        long memoryMs = Math.max(0, memorySeconds) * 1000L;

        if (suspicion == null || (currentTime - suspicion.getLastDetectionTime()) > memoryMs) {
            suspicion = new HostileSuspicion(currentTime, 1);
        } else {
            suspicion = new HostileSuspicion(currentTime, suspicion.getDetections() + 1);
        }

        hostileSuspicions.put(key, suspicion);
        return suspicion.getDetections();
    }

    public void cleanupOldSuspicions(long currentTime, int memorySeconds) {
        long suspicionMaxAge = Math.max(1, memorySeconds) * 1000L;
        hostileSuspicions.entrySet().removeIf(entry ->
                (currentTime - entry.getValue().getLastDetectionTime()) > suspicionMaxAge
        );
    }

    public void updateInvestigations(long currentTime, ConfigManager config) {
        long timeoutMs = config.getInvisiblePlayerInvestigationTimeout() * 1000L;
        double movementTolerance = config.getInvisiblePlayerMovementTolerance();

        hostileInvestigations.entrySet().removeIf(entry -> {
            UUID mobId = entry.getKey();
            HostileInvestigation investigation = entry.getValue();
            Entity mobEntity = Bukkit.getEntity(mobId);
            Player player = Bukkit.getPlayer(investigation.getPlayerId());

            if (!(mobEntity instanceof Mob) || !mobEntity.isValid() || player == null || !player.isOnline()) {
                return true;
            }

            Mob mob = (Mob) mobEntity;
            Location heardLocation = investigation.getHeardLocation();

            if ((currentTime - investigation.getStartTime()) > timeoutMs) {
                support.clearTargetIfMatching(mob, player);
                return true;
            }

            if (player.getWorld() == null || heardLocation.getWorld() == null ||
                    !player.getWorld().equals(heardLocation.getWorld())) {
                support.clearTargetIfMatching(mob, player);
                return true;
            }

            if (investigation.getType() == HostileInvestigationType.VISIBLE_SOUND) {
                mob.getPathfinder().moveTo(heardLocation);
                return false;
            }

            if (!support.isInvisible(player)) {
                support.clearTargetIfMatching(mob, player);
                return true;
            }

            double movedDistanceSquared = player.getLocation().distanceSquared(heardLocation);
            if (InvestigationMath.hasMovedBeyondTolerance(movedDistanceSquared, movementTolerance)) {
                support.clearTargetIfMatching(mob, player);
                return true;
            }

            mob.getPathfinder().moveTo(heardLocation);

            if (!config.shouldInvisiblePlayerAttackIfStillThere()) {
                return false;
            }

            if (mob.getLocation().getWorld() == null || !mob.getLocation().getWorld().equals(heardLocation.getWorld())) {
                support.clearTargetIfMatching(mob, player);
                return true;
            }

            double arrivalDistanceSquared = mob.getLocation().distanceSquared(heardLocation);
            if (!investigation.hasTargetedPlayer() &&
                    !InvestigationMath.hasMovedBeyondTolerance(arrivalDistanceSquared, INVESTIGATION_ARRIVAL_DISTANCE)) {
                mob.setTarget(player);
                investigation.setTargetedPlayer(true);
                if (config.isDetectionLoggingEnabled()) {
                    plugin.getLogger().info(String.format(
                            "Hostile %s attacked invisible player still at last heard location",
                            mob.getType()
                    ));
                }
            }

            return false;
        });
    }

    public void clearMob(UUID mobId) {
        hostileInvestigations.remove(mobId);
        String mobPrefix = mobId + ":";
        hostileSuspicions.keySet().removeIf(key -> key.startsWith(mobPrefix));
    }

    private String getSuspicionKey(UUID mobId, UUID playerId) {
        return mobId + ":" + playerId;
    }
}

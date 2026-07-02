package de.tecca.simplevoicemechanics.service.mob;

import de.tecca.simplevoicemechanics.SimpleVoiceMechanics;
import de.tecca.simplevoicemechanics.manager.ConfigManager;
import de.tecca.simplevoicemechanics.service.mob.handler.HostileVoiceHandler;
import de.tecca.simplevoicemechanics.service.mob.handler.NeutralVoiceHandler;
import de.tecca.simplevoicemechanics.service.mob.handler.PeacefulVoiceHandler;
import de.tecca.simplevoicemechanics.service.mob.handler.WardenVoiceHandler;
import de.tecca.simplevoicemechanics.service.mob.tracker.HostileInvestigationTracker;
import de.tecca.simplevoicemechanics.service.mob.tracker.PeacefulFollowTracker;
import de.tecca.simplevoicemechanics.util.MobCategory;
import de.tecca.simplevoicemechanics.util.RangeCalculator;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;

import java.util.Collection;
import java.util.UUID;

public class MobVoiceService {

    private final SimpleVoiceMechanics plugin;
    private final MobDetectionSupport support;
    private final HostileInvestigationTracker hostileInvestigationTracker;
    private final PeacefulFollowTracker peacefulFollowTracker;
    private final HostileVoiceHandler hostileVoiceHandler;
    private final NeutralVoiceHandler neutralVoiceHandler;
    private final PeacefulVoiceHandler peacefulVoiceHandler;
    private final WardenVoiceHandler wardenVoiceHandler;

    public MobVoiceService(SimpleVoiceMechanics plugin) {
        this.plugin = plugin;
        this.support = new MobDetectionSupport(plugin);
        this.hostileInvestigationTracker = new HostileInvestigationTracker(plugin, support);
        this.peacefulFollowTracker = new PeacefulFollowTracker(plugin);
        this.hostileVoiceHandler = new HostileVoiceHandler(plugin, support, hostileInvestigationTracker);
        this.neutralVoiceHandler = new NeutralVoiceHandler(plugin, support);
        this.peacefulVoiceHandler = new PeacefulVoiceHandler(plugin, support, peacefulFollowTracker);
        this.wardenVoiceHandler = new WardenVoiceHandler(plugin, support);
    }

    public boolean isValidGameMode(Player player) {
        return support.isValidGameMode(player);
    }

    public void processNearbyMobs(Player player, Location loc, boolean isSneaking, double decibels) {
        ConfigManager config = plugin.getConfigManager();
        double maxCheckRange = calculateMaxMobCheckRange(player, decibels, config);

        World world = loc.getWorld();
        if (world == null) {
            return;
        }

        Collection<Entity> nearbyEntities = world
                .getNearbyEntities(loc, maxCheckRange, maxCheckRange, maxCheckRange);

        for (Entity entity : nearbyEntities) {
            if (entity instanceof Mob) {
                processMob((Mob) entity, player, loc, isSneaking, decibels);
            }
        }
    }

    public void cleanupTick() {
        long currentTime = System.currentTimeMillis();
        ConfigManager config = plugin.getConfigManager();

        peacefulFollowTracker.updateFollowing(currentTime, config, this::cleanupMobState);
        support.cleanupReactionCooldowns(currentTime);
        hostileInvestigationTracker.cleanupOldSuspicions(
                currentTime,
                config.getHostileInvestigationMemorySeconds()
        );
        hostileInvestigationTracker.updateInvestigations(currentTime, config);
    }

    private void processMob(Mob mob, Player player, Location playerLoc, boolean isSneaking, double decibels) {
        EntityType type = mob.getType();
        ConfigManager config = plugin.getConfigManager();

        if (type == EntityType.WARDEN && config.isWardenEnabled()) {
            wardenVoiceHandler.process((Warden) mob, player, playerLoc, decibels);
        } else if (MobCategory.isHostile(type)) {
            hostileVoiceHandler.process(mob, player, playerLoc, decibels);
        } else if (MobCategory.isNeutral(type)) {
            neutralVoiceHandler.process(mob, player, playerLoc, decibels);
        } else if (MobCategory.isPeaceful(type)) {
            peacefulVoiceHandler.process(mob, player, playerLoc, isSneaking, decibels);
        }
    }

    private double calculateMaxMobCheckRange(Player player, double decibels, ConfigManager config) {
        double maxCheckRange = 0.0;

        if (config.isHostileMobsEnabled()) {
            maxCheckRange = Math.max(maxCheckRange, RangeCalculator.calculateEffectiveRange(
                    support.applyRangeMultiplier(player, config.getHostileMaxRange()),
                    decibels,
                    config.getHostileVolumeThresholdDb()
            ));
        }

        if (config.isNeutralMobsEnabled()) {
            maxCheckRange = Math.max(maxCheckRange, RangeCalculator.calculateEffectiveRange(
                    support.applyRangeMultiplier(player, config.getNeutralMaxRange()),
                    decibels,
                    config.getNeutralVolumeThresholdDb()
            ));
        }

        if (config.isPeacefulMobsEnabled()) {
            maxCheckRange = Math.max(maxCheckRange, RangeCalculator.calculateEffectiveRange(
                    support.applyRangeMultiplier(player, config.getPeacefulMaxRange()),
                    decibels,
                    config.getPeacefulVolumeThresholdDb()
            ));
        }

        if (config.isWardenEnabled()) {
            maxCheckRange = Math.max(maxCheckRange, RangeCalculator.calculateEffectiveRange(
                    support.applyRangeMultiplier(player, config.getWardenMaxRange()),
                    decibels,
                    config.getWardenVolumeThresholdDb()
            ));
        }

        return maxCheckRange;
    }

    private void cleanupMobState(UUID mobId) {
        support.clearReactionCooldown(mobId);
        hostileInvestigationTracker.clearMob(mobId);
    }
}

package de.tecca.simplevoicemechanics.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Utility class for mob group alert mechanics.
 *
 * <p>When a mob detects voice, it can alert nearby mobs of the same type:
 * <ul>
 *   <li>Zombies call other zombies → Horde effect</li>
 *   <li>Wolves alert pack members → Pack hunting</li>
 *   <li>Configurable alert radius per mob type</li>
 * </ul>
 *
 * @author Tecca
 * @version 1.3.0
 */
public class MobGroupAlert {

    // Default alert ranges for different mob types
    private static final Map<EntityType, Double> ALERT_RANGES = new EnumMap<>(EntityType.class);

    // Mob types that support group alerting
    private static final Set<EntityType> SOCIAL_MOBS = EnumSet.noneOf(EntityType.class);

    static {
        initializeAlertRanges();
        initializeSocialMobs();
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private MobGroupAlert() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    /**
     * Initializes default alert ranges for mob types.
     */
    private static void initializeAlertRanges() {
        // Undead (large groups)
        ALERT_RANGES.put(EntityType.ZOMBIE, 16.0);
        ALERT_RANGES.put(EntityType.HUSK, 16.0);
        ALERT_RANGES.put(EntityType.DROWNED, 12.0);
        ALERT_RANGES.put(EntityType.SKELETON, 12.0);
        ALERT_RANGES.put(EntityType.ZOMBIE_VILLAGER, 16.0);

        // Pack hunters
        addIfExists(EntityType.WOLF, 20.0);
        addIfExists(EntityType.PIGLIN, 18.0);
        addIfExists(EntityType.ZOMBIFIED_PIGLIN, 20.0);

        // Swarms
        ALERT_RANGES.put(EntityType.SPIDER, 10.0);
        ALERT_RANGES.put(EntityType.CAVE_SPIDER, 10.0);
        addIfExists(EntityType.BEE, 8.0);

        // Pillagers
        addIfExists(EntityType.PILLAGER, 24.0);
        addIfExists(EntityType.VINDICATOR, 18.0);
        addIfExists(EntityType.EVOKER, 18.0);
    }

    /**
     * Initializes social mob types.
     */
    private static void initializeSocialMobs() {
        // Add all mob types that have alert ranges
        SOCIAL_MOBS.addAll(ALERT_RANGES.keySet());
    }

    /**
     * Safely adds EntityType if it exists in this version.
     */
    private static void addIfExists(EntityType type, double range) {
        try {
            ALERT_RANGES.put(type, range);
        } catch (Exception e) {
            // EntityType doesn't exist in this version
        }
    }

    /**
     * Checks if a mob type supports group alerting.
     */
    public static boolean isSocialMob(EntityType type) {
        return SOCIAL_MOBS.contains(type);
    }

    /**
     * Gets the alert range for a mob type.
     */
    public static double getAlertRange(EntityType type) {
        return ALERT_RANGES.getOrDefault(type, 12.0);
    }

    /**
     * Alerts nearby mobs of the same type to target a player.
     *
     * @param alertingMob the mob that detected the player
     * @param target the player to target
     * @param maxAlerts maximum number of mobs to alert
     * @return number of mobs alerted
     */
    public static int alertNearbyMobs(Mob alertingMob, Player target, int maxAlerts) {
        return alertNearbyMobs(alertingMob, target, maxAlerts, getAlertRange(alertingMob.getType()));
    }

    /**
     * Alerts nearby mobs of the same type to target a player.
     *
     * @param alertingMob the mob that detected the player
     * @param target the player to target
     * @param maxAlerts maximum number of mobs to alert
     * @param alertRange configured alert range in blocks
     * @return number of mobs alerted
     */
    public static int alertNearbyMobs(Mob alertingMob, Player target, int maxAlerts, double alertRange) {
        EntityType mobType = alertingMob.getType();

        if (!isSocialMob(mobType)) {
            return 0;
        }

        double safeAlertRange = Math.max(0.0, alertRange);
        Location mobLoc = alertingMob.getLocation();
        if (mobLoc.getWorld() == null) {
            return 0;
        }

        Collection<Entity> nearbyEntities = mobLoc.getWorld()
                .getNearbyEntities(mobLoc, safeAlertRange, safeAlertRange, safeAlertRange);

        int alertedCount = 0;

        for (Entity entity : nearbyEntities) {
            if (alertedCount >= maxAlerts) {
                break;
            }

            // Skip if not same mob type
            if (entity.getType() != mobType) {
                continue;
            }

            // Skip if not a mob or is the alerting mob
            if (!(entity instanceof Mob) || entity.equals(alertingMob)) {
                continue;
            }

            Mob nearbyMob = (Mob) entity;

            // Skip if mob already has this target
            if (nearbyMob.getTarget() != null && nearbyMob.getTarget().equals(target)) {
                continue;
            }

            // Skip if mob cannot attack
            if (!MobCondition.canAttack(nearbyMob)) {
                continue;
            }

            // Alert the mob
            nearbyMob.setTarget(target);
            alertedCount++;
        }

        return alertedCount;
    }

    /**
     * Calculates the optimal number of mobs to alert based on distance.
     *
     * @param distance distance to player
     * @param minRange minimum range
     * @param maxRange maximum range
     * @return number of mobs to alert (1-5)
     */
    public static int calculateAlertCount(double distance, double minRange, double maxRange) {
        if (distance <= minRange) {
            return 5;  // Close = alert many
        }

        if (distance >= maxRange) {
            return 1;  // Far = alert few
        }

        // Scale linearly: close = more alerts
        double normalized = (distance - minRange) / (maxRange - minRange);
        return Math.max(1, (int) (5 * (1.0 - normalized)));
    }

    /**
     * Gets debug information about mob group alerts.
     */
    public static String getDebugInfo(EntityType type) {
        if (!isSocialMob(type)) {
            return type.name() + ": Not a social mob";
        }

        double range = getAlertRange(type);
        return String.format(
                "%s: Social mob | Alert range: %.1f blocks",
                type.name(),
                range
        );
    }

    /**
     * Gets all social mob types.
     */
    public static Set<EntityType> getSocialMobs() {
        return Collections.unmodifiableSet(SOCIAL_MOBS);
    }
}

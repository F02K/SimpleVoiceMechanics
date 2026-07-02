package de.tecca.simplevoicemechanics.util;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Vanilla-style temptation item checks for passive mob following.
 */
public final class TemptationItems {

    private static final Map<EntityType, Set<Material>> ITEMS = new EnumMap<>(EntityType.class);

    static {
        addIfExists("COW", Material.WHEAT);
        addIfExists("MOOSHROOM", Material.WHEAT);
        addIfExists("SHEEP", Material.WHEAT);
        addIfExists("GOAT", Material.WHEAT);
        addIfExists("PIG", Material.CARROT, Material.POTATO, Material.BEETROOT);
        addIfExists("RABBIT", Material.CARROT, Material.GOLDEN_CARROT, Material.DANDELION);
        addIfExists("CHICKEN", Material.WHEAT_SEEDS, Material.PUMPKIN_SEEDS, Material.MELON_SEEDS, Material.BEETROOT_SEEDS);
        addIfExists("HORSE", Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE, Material.GOLDEN_CARROT, Material.APPLE, Material.WHEAT, Material.SUGAR);
        addIfExists("DONKEY", Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE, Material.GOLDEN_CARROT, Material.APPLE, Material.WHEAT, Material.SUGAR);
        addIfExists("MULE", Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE, Material.GOLDEN_CARROT, Material.APPLE, Material.WHEAT, Material.SUGAR);
        addIfExists("CAT", Material.COD, Material.SALMON);
        addIfExists("OCELOT", Material.COD, Material.SALMON);
        addIfExists("PARROT", Material.WHEAT_SEEDS, Material.PUMPKIN_SEEDS, Material.MELON_SEEDS, Material.BEETROOT_SEEDS);
        addIfExists("TURTLE", Material.SEAGRASS);
        addIfExists("AXOLOTL", Material.TROPICAL_FISH_BUCKET);
        addIfExists("FROG", Material.SLIME_BALL);
        addIfExists("CAMEL", Material.CACTUS);
        addIfExists("ARMADILLO", Material.SPIDER_EYE);
    }

    private TemptationItems() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static boolean isHoldingTemptationItem(EntityType entityType, ItemStack mainHand, ItemStack offHand) {
        return isTemptationItem(entityType, mainHand) || isTemptationItem(entityType, offHand);
    }

    public static boolean isTemptationItem(EntityType entityType, ItemStack item) {
        if (item == null) {
            return false;
        }
        return isTemptationMaterial(entityType, item.getType());
    }

    public static boolean isTemptationMaterial(EntityType entityType, Material material) {
        if (material == null) {
            return false;
        }
        Set<Material> materials = ITEMS.get(entityType);
        return materials != null && materials.contains(material);
    }

    private static void add(EntityType entityType, Material first, Material... rest) {
        Set<Material> materials = EnumSet.of(first, rest);
        ITEMS.put(entityType, materials);
    }

    private static void addIfExists(String entityTypeName, Material first, Material... rest) {
        try {
            add(EntityType.valueOf(entityTypeName), first, rest);
        } catch (IllegalArgumentException ignored) {
            // EntityType does not exist on this Minecraft version.
        }
    }
}

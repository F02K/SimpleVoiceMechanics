package de.tecca.simplevoicemechanics.util;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemptationItemsTest {

    @Test
    void recognizesCommonVanillaTemptationItems() {
        assertTrue(TemptationItems.isTemptationMaterial(EntityType.COW, Material.WHEAT));
        assertTrue(TemptationItems.isTemptationMaterial(EntityType.CHICKEN, Material.WHEAT_SEEDS));
        assertTrue(TemptationItems.isTemptationMaterial(EntityType.PIG, Material.CARROT));
        assertTrue(TemptationItems.isTemptationMaterial(EntityType.CAT, Material.COD));
    }

    @Test
    void rejectsUnknownOrWrongItems() {
        assertFalse(TemptationItems.isTemptationMaterial(EntityType.COW, Material.CARROT));
        assertFalse(TemptationItems.isTemptationMaterial(EntityType.ZOMBIE, Material.WHEAT));
        assertFalse(TemptationItems.isTemptationMaterial(EntityType.SHEEP, null));
    }
}

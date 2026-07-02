package de.tecca.simplevoicemechanics.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

/**
 * Cheap line sampling for block-based voice muffling.
 */
public final class ObstructionMuffling {

    private static final double SAMPLE_STEP_BLOCKS = 1.0;

    private ObstructionMuffling() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static double calculateDbReduction(Location from, Location to,
                                              double woolReductionDb,
                                              double solidReductionDb,
                                              double maxReductionDb) {
        if (from == null || to == null || from.getWorld() == null || to.getWorld() == null) {
            return 0.0;
        }
        if (!from.getWorld().equals(to.getWorld())) {
            return 0.0;
        }

        World world = from.getWorld();
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        if (distance <= 0.0) {
            return 0.0;
        }

        Vector step = direction.normalize().multiply(SAMPLE_STEP_BLOCKS);
        Vector cursor = from.toVector().clone();
        int samples = Math.max(1, (int) Math.floor(distance / SAMPLE_STEP_BLOCKS));
        Set<String> seenBlocks = new HashSet<>();
        int woolBlocks = 0;
        int solidBlocks = 0;

        for (int i = 1; i < samples; i++) {
            cursor.add(step);
            Block block = world.getBlockAt(cursor.getBlockX(), cursor.getBlockY(), cursor.getBlockZ());
            String key = block.getX() + ":" + block.getY() + ":" + block.getZ();
            if (!seenBlocks.add(key)) {
                continue;
            }

            Material type = block.getType();
            if (isWoolMuffler(type)) {
                woolBlocks++;
            } else if (type.isOccluding()) {
                solidBlocks++;
            }
        }

        return AcousticModifiers.calculateObstructionReduction(
                woolBlocks,
                solidBlocks,
                woolReductionDb,
                solidReductionDb,
                maxReductionDb
        );
    }

    public static boolean isWoolMuffler(Material material) {
        String name = material.name();
        return name.endsWith("_WOOL") || name.endsWith("_CARPET") || name.equals("MOSS_CARPET");
    }
}

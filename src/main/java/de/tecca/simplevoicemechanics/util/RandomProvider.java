package de.tecca.simplevoicemechanics.util;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Central random source for probabilistic mechanics.
 */
public final class RandomProvider {

    private static DoubleSupplier randomSupplier = () -> ThreadLocalRandom.current().nextDouble();

    private RandomProvider() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static double nextDouble() {
        return randomSupplier.getAsDouble();
    }

    public static boolean passes(double chance) {
        if (chance <= 0.0) {
            return false;
        }
        if (chance >= 1.0) {
            return true;
        }
        return nextDouble() <= chance;
    }

    public static void setRandomSupplier(DoubleSupplier supplier) {
        randomSupplier = supplier;
    }

    public static void reset() {
        randomSupplier = () -> ThreadLocalRandom.current().nextDouble();
    }
}

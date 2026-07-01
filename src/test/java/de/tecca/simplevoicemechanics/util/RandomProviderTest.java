package de.tecca.simplevoicemechanics.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomProviderTest {

    @AfterEach
    void resetRandomProvider() {
        RandomProvider.reset();
    }

    @Test
    void deterministicSupplierControlsProbability() {
        RandomProvider.setRandomSupplier(() -> 0.25);

        assertTrue(RandomProvider.passes(0.5));
        assertFalse(RandomProvider.passes(0.1));
    }
}

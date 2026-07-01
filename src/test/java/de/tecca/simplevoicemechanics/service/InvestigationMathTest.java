package de.tecca.simplevoicemechanics.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvestigationMathTest {

    @Test
    void detectsMovementBeyondTolerance() {
        assertFalse(InvestigationMath.hasMovedBeyondTolerance(2.25, 1.5));
        assertTrue(InvestigationMath.hasMovedBeyondTolerance(2.26, 1.5));
    }

    @Test
    void negativeToleranceIsTreatedAsZero() {
        assertFalse(InvestigationMath.hasMovedBeyondTolerance(0.0, -1.0));
        assertTrue(InvestigationMath.hasMovedBeyondTolerance(0.01, -1.0));
    }
}

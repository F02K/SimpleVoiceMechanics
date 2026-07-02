package de.tecca.simplevoicemechanics.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HostileSuspicionMathTest {

    @Test
    void firstNormalDetectionInvestigatesOnly() {
        assertFalse(HostileSuspicionMath.shouldTarget(1, 2, 8.0, 2.0, -35.0, -40.0));
    }

    @Test
    void repeatedDetectionEscalatesToTargeting() {
        assertTrue(HostileSuspicionMath.shouldTarget(2, 2, 8.0, 2.0, -35.0, -40.0));
    }

    @Test
    void closeOrVeryLoudDetectionEscalatesImmediately() {
        assertTrue(HostileSuspicionMath.shouldTarget(1, 2, 2.0, 2.0, -35.0, -40.0));
        assertTrue(HostileSuspicionMath.shouldTarget(1, 2, 8.0, 2.0, -20.0, -40.0));
    }
}

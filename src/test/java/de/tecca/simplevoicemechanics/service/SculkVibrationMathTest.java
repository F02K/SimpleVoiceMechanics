package de.tecca.simplevoicemechanics.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SculkVibrationMathTest {

    @Test
    void clampsArrivalTicksToConfiguredBounds() {
        assertEquals(5, SculkVibrationMath.calculateArrivalTicks(1.2, 5, 40));
        assertEquals(13, SculkVibrationMath.calculateArrivalTicks(12.2, 5, 40));
        assertEquals(40, SculkVibrationMath.calculateArrivalTicks(80.0, 5, 40));
    }

    @Test
    void handlesInvalidBoundsSafely() {
        assertEquals(0, SculkVibrationMath.calculateArrivalTicks(-4.0, -5, -1));
        assertEquals(10, SculkVibrationMath.calculateArrivalTicks(8.0, 10, 5));
    }
}

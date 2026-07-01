package de.tecca.simplevoicemechanics.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangeCalculatorTest {

    @Test
    void effectiveRangeIncreasesWhenVoiceIsAboveThreshold() {
        double baseRange = 16.0;
        double atThreshold = RangeCalculator.calculateEffectiveRange(baseRange, -40.0, -40.0);
        double louder = RangeCalculator.calculateEffectiveRange(baseRange, -20.0, -40.0);

        assertEquals(baseRange, atThreshold);
        assertTrue(louder > atThreshold);
    }

    @Test
    void detectionChanceHandlesInvalidRangesSafely() {
        assertEquals(1.0, RangeCalculator.calculateDetectionChance(0.0, -5.0, -1.0, -2.0));
        assertEquals(0.0, RangeCalculator.calculateDetectionChance(4.0, 2.0, 1.0, 1.0));
    }
}

package de.tecca.simplevoicemechanics.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AcousticModifiersTest {

    @Test
    void sneakingReductionLowersDecibelsOnlyWhenEnabledAndSneaking() {
        assertEquals(-46.0, AcousticModifiers.applySneakingReduction(-40.0, true, true, 6.0));
        assertEquals(-40.0, AcousticModifiers.applySneakingReduction(-40.0, true, false, 6.0));
        assertEquals(-40.0, AcousticModifiers.applySneakingReduction(-40.0, false, true, 6.0));
    }

    @Test
    void weatherUsesThunderBeforeRain() {
        assertEquals(1.0, AcousticModifiers.getWeatherRangeMultiplier(false, false, 0.85, 0.70));
        assertEquals(0.85, AcousticModifiers.getWeatherRangeMultiplier(true, false, 0.85, 0.70));
        assertEquals(0.70, AcousticModifiers.getWeatherRangeMultiplier(true, true, 0.85, 0.70));

        assertEquals(0.0, AcousticModifiers.getWeatherThresholdAdjustment(false, false, 3.0, 6.0));
        assertEquals(3.0, AcousticModifiers.getWeatherThresholdAdjustment(true, false, 3.0, 6.0));
        assertEquals(6.0, AcousticModifiers.getWeatherThresholdAdjustment(true, true, 3.0, 6.0));
    }

    @Test
    void obstructionReductionIsCapped() {
        assertEquals(0.0, AcousticModifiers.calculateObstructionReduction(0, 0, 12.0, 4.0, 18.0));
        assertEquals(16.0, AcousticModifiers.calculateObstructionReduction(1, 1, 12.0, 4.0, 18.0));
        assertEquals(18.0, AcousticModifiers.calculateObstructionReduction(2, 2, 12.0, 4.0, 18.0));
    }
}

package de.tecca.simplevoicemechanics.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionServiceTest {

    @Test
    void rejectsAudioBelowThreshold() {
        DetectionResult result = DetectionService.calculate(4.0, 2.0, 16.0, 1.0, -60.0, -40.0);

        assertFalse(result.isThresholdPassed());
        assertFalse(result.canAttemptDetection());
    }

    @Test
    void acceptsAudioWithinEffectiveRange() {
        DetectionResult result = DetectionService.calculate(4.0, 2.0, 16.0, 1.0, -30.0, -40.0);

        assertTrue(result.isThresholdPassed());
        assertTrue(result.isWithinRange());
        assertTrue(result.getChance() > 0.0);
    }

    @Test
    void sculkThresholdIsIndependentFromHostileThreshold() {
        DetectionResult hostileResult = DetectionService.calculate(4.0, 2.0, 16.0, 1.0, -30.0, -40.0);
        DetectionResult sculkResult = DetectionService.calculate(4.0, 2.0, 16.0, 1.0, -30.0, -20.0);

        assertTrue(hostileResult.canAttemptDetection());
        assertFalse(sculkResult.canAttemptDetection());
    }
}

package de.tecca.simplevoicemechanics.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioUtilsTest {

    @Test
    void silenceReturnsMinimumDecibels() {
        assertEquals(-127.0, AudioUtils.calculateAudioLevel(new short[] {0, 0, 0, 0}));
    }

    @Test
    void normalizedVolumeIsClampedToExpectedRange() {
        assertEquals(0.0, AudioUtils.dbToNormalizedVolume(-200.0));
        assertEquals(1.0, AudioUtils.dbToNormalizedVolume(20.0));
        assertTrue(AudioUtils.dbToNormalizedVolume(-63.5) > 0.4);
    }
}

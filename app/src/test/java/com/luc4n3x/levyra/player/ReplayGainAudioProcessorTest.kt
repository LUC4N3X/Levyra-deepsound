package com.luc4n3x.levyra.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayGainAudioProcessorTest {
    @Test
    fun clipProtectionCapsPositiveGainUsingPeakMetadata() {
        val gain = VolumeNormalizationAudioProcessor.replayGainLinear(
            gainDb = 6f,
            peak = 0.8f,
            preampDb = 0f,
            preventClipping = true
        ) ?: 0f

        assertEquals(1.25f, gain, 0.001f)
    }

    @Test
    fun missingPeakNeverBoostsWhenClipProtectionIsEnabled() {
        val gain = VolumeNormalizationAudioProcessor.replayGainLinear(
            gainDb = 6f,
            peak = null,
            preampDb = 0f,
            preventClipping = true
        ) ?: 0f

        assertEquals(1f, gain, 0.001f)
    }

    @Test
    fun disablingClipProtectionAllowsReplayGainBoost() {
        val gain = VolumeNormalizationAudioProcessor.replayGainLinear(
            gainDb = 6f,
            peak = null,
            preampDb = 0f,
            preventClipping = false
        ) ?: 0f

        assertTrue(gain > 1.9f)
    }

    @Test
    fun dedicatedPreampIsIncludedInReplayGainCalculation() {
        val gain = VolumeNormalizationAudioProcessor.replayGainLinear(
            gainDb = -6f,
            peak = null,
            preampDb = 3f,
            preventClipping = true
        ) ?: 0f

        assertEquals(0.7079f, gain, 0.002f)
    }
}

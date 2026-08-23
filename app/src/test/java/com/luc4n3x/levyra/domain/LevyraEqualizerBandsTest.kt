package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LevyraEqualizerBandsTest {
    @Test
    fun bandLabelsCoverEveryDspBand() {
        assertEquals(LevyraAudioPresets.bandCount, LevyraAudioPresets.bandFrequencyLabels.size)
        assertEquals("31", LevyraAudioPresets.bandFrequencyLabels.first())
        assertEquals("16k", LevyraAudioPresets.bandFrequencyLabels.last())
    }

    @Test
    fun bandLevelsMapToTheDspDecibelRange() {
        assertEquals(0f, LevyraAudioPresets.bandDb(0), 0.0001f)
        assertEquals(12f, LevyraAudioPresets.bandDb(100), 0.0001f)
        assertEquals(-12f, LevyraAudioPresets.bandDb(-100), 0.0001f)
        assertEquals(6f, LevyraAudioPresets.bandDb(50), 0.0001f)
        assertEquals(12f, LevyraAudioPresets.bandDb(400), 0.0001f)
    }

    @Test
    fun verticalDragMapsTopToBoostAndBottomToCut() {
        assertEquals(100, LevyraAudioPresets.bandLevelFromVerticalFraction(0f))
        assertEquals(0, LevyraAudioPresets.bandLevelFromVerticalFraction(0.5f))
        assertEquals(-100, LevyraAudioPresets.bandLevelFromVerticalFraction(1f))
    }

    @Test
    fun verticalDragOutsideTheCurveIsClamped() {
        assertEquals(100, LevyraAudioPresets.bandLevelFromVerticalFraction(-3f))
        assertEquals(-100, LevyraAudioPresets.bandLevelFromVerticalFraction(4.5f))
    }

    @Test
    fun everyPresetKeepsTheDspBandContract() {
        LevyraAudioPresets.presets.forEach { preset ->
            assertEquals(preset.id, LevyraAudioPresets.bandCount, preset.levels.size)
            preset.levels.forEach { level ->
                assertEquals(preset.id, level.coerceIn(-100, 100), level)
            }
        }
    }
}

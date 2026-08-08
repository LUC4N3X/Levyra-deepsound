package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraAudioPresetsTest {

    private val addedPresetIds = listOf(
        LevyraAudioPresets.ROCK,
        LevyraAudioPresets.POP,
        LevyraAudioPresets.ELECTRONIC,
        LevyraAudioPresets.JAZZ,
        LevyraAudioPresets.ACOUSTIC,
        LevyraAudioPresets.CLASSICAL,
        LevyraAudioPresets.AIRPODS_PRO,
        LevyraAudioPresets.SONY_XM4,
        LevyraAudioPresets.SONY_XM5,
        LevyraAudioPresets.SENNHEISER_HD600
    )

    @Test
    fun presetIdsAreUnique() {
        val ids = LevyraAudioPresets.presets.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun everyPresetKeepsTheTenBandContract() {
        LevyraAudioPresets.presets.forEach { preset ->
            assertEquals(preset.id, LevyraAudioPresets.bandCount, preset.levels.size)
        }
    }

    @Test
    fun addedPresetIdsResolveThroughCatalogHelpers() {
        addedPresetIds.forEach { id ->
            val preset = LevyraAudioPresets.preset(id)

            assertEquals(id, LevyraAudioPresets.normalizePreset(id))
            assertEquals(preset.levels, LevyraAudioPresets.levelsFor(id))
            assertEquals(preset.fallbackLabel, LevyraAudioPresets.labelFor(id))
            assertTrue(LevyraAudioPresets.labelFor(id).isNotBlank())
        }
    }
}

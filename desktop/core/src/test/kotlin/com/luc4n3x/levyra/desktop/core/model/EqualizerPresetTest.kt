package com.luc4n3x.levyra.desktop.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerPresetTest {
    @Test
    fun everyPresetCoversTheLibvlcBandCount() {
        EqualizerPreset.entries.forEach { preset ->
            assertEquals(
                "Band count mismatch for ${preset.id}",
                EqualizerSettings.BAND_FREQUENCIES.size,
                preset.gains.size
            )
        }
    }

    @Test
    fun presetPreampCancelsThePositiveHeadroom() {
        EqualizerPreset.entries.forEach { preset ->
            val loudestBoost = preset.gains.max().coerceAtLeast(0f)
            assertEquals(
                "Missing headroom for ${preset.id}",
                -loudestBoost,
                preset.preamp,
                0.001f
            )
            assertTrue(
                "Preamp out of range for ${preset.id}",
                preset.preamp >= EqualizerSettings.MIN_GAIN
            )
        }
    }

    @Test
    fun applyingAPresetMakesItTheSelectedOne() {
        EqualizerPreset.entries.forEach { preset ->
            val applied = preset.applyTo(EqualizerSettings(enabled = true))
            assertEquals(preset.id, EqualizerPreset.selectedId(applied))
            assertTrue(applied.enabled)
        }
    }

    @Test
    fun aManualBandEditFallsBackToCustom() {
        val rock = EqualizerPreset.ROCK.applyTo(EqualizerSettings(enabled = true))
        val edited = rock.withAmp(3, 7f)

        assertNull(EqualizerPreset.matching(edited))
        assertEquals(EqualizerPreset.CUSTOM_ID, EqualizerPreset.selectedId(edited))
    }

    @Test
    fun defaultAndFlattenedSettingsResolveToFlat() {
        assertEquals(EqualizerPreset.FLAT.id, EqualizerPreset.selectedId(EqualizerSettings()))
        assertEquals(
            EqualizerPreset.FLAT.id,
            EqualizerPreset.selectedId(EqualizerPreset.ROCK.applyTo(EqualizerSettings()).flattened())
        )
    }
}

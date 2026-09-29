package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioQualityPreferenceTest {
    @Test
    fun persistedQualityValuesRestoreSafely() {
        AudioQualityPreference.entries.forEach { quality ->
            assertEquals(quality, AudioQualityPreference.fromStorage(quality.storageValue))
        }
        assertEquals(AudioQualityPreference.HIGH, AudioQualityPreference.fromStorage("unknown"))
    }

    @Test
    fun retiredRemoteLosslessTiersRestoreAsHighQuality() {
        listOf("dolby_atmos", "max_quality", "hi_res", "cd_lossless", "normal").forEach { legacy ->
            assertEquals(legacy, AudioQualityPreference.HIGH, AudioQualityPreference.fromStorage(legacy))
        }
    }
}

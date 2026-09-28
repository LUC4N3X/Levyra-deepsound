package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioQualityPreferenceTest {
    @Test
    fun losslessRequestsFollowTheConfiguredFallbackOrder() {
        assertEquals(
            listOf(
                AudioQualityPreference.DOLBY_ATMOS,
                AudioQualityPreference.MAX_QUALITY,
                AudioQualityPreference.HI_RES,
                AudioQualityPreference.CD_LOSSLESS
            ),
            AudioQualityPreference.DOLBY_ATMOS.losslessAttemptOrder()
        )
        assertEquals(
            listOf(AudioQualityPreference.HI_RES, AudioQualityPreference.CD_LOSSLESS),
            AudioQualityPreference.HI_RES.losslessAttemptOrder(allowUpgrade = false)
        )
        assertTrue(AudioQualityPreference.HIGH.losslessAttemptOrder().isEmpty())
    }

    @Test
    fun persistedQualityValuesRestoreSafely() {
        AudioQualityPreference.entries.forEach { quality ->
            assertEquals(quality, AudioQualityPreference.fromStorage(quality.storageValue))
        }
        assertEquals(AudioQualityPreference.MAX_QUALITY, AudioQualityPreference.fromStorage("unknown"))
    }
}

package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class HighQualityAudioModeTest {
    @Test
    fun missingAndUnknownPreferencesUse320Kbps() {
        assertEquals(HighQualityAudioMode.PREFER_320, HighQualityAudioMode.DEFAULT)
        assertEquals(HighQualityAudioMode.PREFER_320, HighQualityAudioMode.fromStorage(null))
        assertEquals(HighQualityAudioMode.PREFER_320, HighQualityAudioMode.fromStorage("unknown"))
    }

    @Test
    fun everyExplicitChoiceSurvivesStorageRoundTrip() {
        HighQualityAudioMode.entries.forEach { mode ->
            assertEquals(mode, HighQualityAudioMode.fromStorage(mode.storageValue))
        }
    }
}

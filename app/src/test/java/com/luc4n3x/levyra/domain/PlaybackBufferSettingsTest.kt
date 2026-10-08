package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackBufferSettingsTest {
    @Test
    fun defaultsKeepAutomaticMode() {
        assertEquals(PlaybackBufferMode.AUTOMATIC, PlaybackBufferSettings().mode)
        assertEquals(PlaybackBufferMode.AUTOMATIC, LevyraAudioSettings().normalized().playbackBuffer.mode)
    }

    @Test
    fun invalidCustomValuesNormalizeToMedia3SafeOrdering() {
        val normalized = PlaybackBufferSettings(
            mode = PlaybackBufferMode.CUSTOM,
            minBufferSeconds = Float.NaN,
            maxBufferSeconds = -40f,
            playbackBufferSeconds = 90f,
            rebufferSeconds = Float.POSITIVE_INFINITY
        ).normalized()

        assertTrue(normalized.minBufferSeconds >= PlaybackBufferSettings.MIN_BUFFER_SECONDS)
        assertTrue(normalized.maxBufferSeconds >= normalized.minBufferSeconds)
        assertTrue(normalized.maxBufferSeconds <= PlaybackBufferSettings.MAX_BUFFER_SECONDS)
        assertTrue(normalized.playbackBufferSeconds in PlaybackBufferSettings.MIN_THRESHOLD_SECONDS..normalized.minBufferSeconds)
        assertTrue(normalized.rebufferSeconds in PlaybackBufferSettings.MIN_THRESHOLD_SECONDS..normalized.minBufferSeconds)
    }

    @Test
    fun everyPresetIsCustomAndValid() {
        listOf(
            PlaybackBufferSettings.Reduced,
            PlaybackBufferSettings.Balanced,
            PlaybackBufferSettings.High
        ).forEach { preset ->
            assertEquals(PlaybackBufferMode.CUSTOM, preset.mode)
            assertEquals(preset, preset.normalized())
        }
    }
}

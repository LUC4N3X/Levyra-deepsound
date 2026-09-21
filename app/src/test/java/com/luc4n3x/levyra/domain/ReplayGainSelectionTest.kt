package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayGainSelectionTest {
    private val metadata = ReplayGainMetadata(
        trackGainDb = -7.2f,
        albumGainDb = -4.4f,
        trackPeak = 0.92f,
        albumPeak = 0.97f
    )

    @Test
    fun trackModeAlwaysUsesTrackMetadata() {
        val selected = selectReplayGain(ReplayGainMode.TRACK, metadata, albumContext = true)
        assertEquals(-7.2f, selected?.gainDb ?: 0f, 0.001f)
        assertEquals(0.92f, selected?.peak ?: 0f, 0.001f)
    }

    @Test
    fun albumModeUsesAlbumAndFallsBackToTrack() {
        assertEquals(-4.4f, selectReplayGain(ReplayGainMode.ALBUM, metadata, false)?.gainDb ?: 0f, 0.001f)
        val fallback = selectReplayGain(
            ReplayGainMode.ALBUM,
            metadata.copy(albumGainDb = null, albumPeak = null),
            albumContext = true
        )
        assertEquals(-7.2f, fallback?.gainDb ?: 0f, 0.001f)
    }

    @Test
    fun smartModePreservesAlbumDynamicsOnlyInAlbumContext() {
        assertEquals(-4.4f, selectReplayGain(ReplayGainMode.SMART, metadata, true)?.gainDb ?: 0f, 0.001f)
        assertEquals(-7.2f, selectReplayGain(ReplayGainMode.SMART, metadata, false)?.gainDb ?: 0f, 0.001f)
    }

    @Test
    fun offModeDoesNotSelectReplayGain() {
        assertNull(selectReplayGain(ReplayGainMode.OFF, metadata, albumContext = true))
    }

    @Test
    fun legacyEnabledSettingMigratesToSmart() {
        val normalized = LevyraAudioSettings(replayGainEnabled = true).normalized()
        assertEquals(ReplayGainMode.SMART, normalized.replayGainMode)
        assertTrue(normalized.replayGainActive)
    }
}

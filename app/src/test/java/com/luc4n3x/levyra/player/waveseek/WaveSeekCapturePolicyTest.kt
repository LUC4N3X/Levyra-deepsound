package com.luc4n3x.levyra.player.waveseek

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WaveSeekCapturePolicyTest {
    @Test
    fun `resolved audio source creates a capture spec`() {
        val spec = waveSeekCaptureSpec(
            mediaId = "track-1",
            source = "https://media.example/videoplayback?mime=audio%2Fmp4",
            durationMs = 180_000L,
            videoMode = false,
            liveRadio = false,
            alreadyStored = false
        )

        requireNotNull(spec)
        assertEquals("track-1", spec.mediaId)
        assertEquals(180_000L, spec.durationMs)
    }

    @Test
    fun `existing waveform video and radio skip capture`() {
        assertNull(
            waveSeekCaptureSpec("track-1", "https://media.example/song.m4a", 180_000L, false, false, true)
        )
        assertNull(
            waveSeekCaptureSpec("track-1", "https://media.example/song.m4a", 180_000L, true, false, false)
        )
        assertNull(
            waveSeekCaptureSpec("track-1", "https://media.example/song.m4a", 180_000L, false, true, false)
        )
    }

    @Test
    fun `unsupported source and missing identity keep fallback`() {
        assertNull(
            waveSeekCaptureSpec("track-1", "rtsp://media.example/song", 180_000L, false, false, false)
        )
        assertNull(
            waveSeekCaptureSpec("", "https://media.example/song.m4a", 180_000L, false, false, false)
        )
    }
}

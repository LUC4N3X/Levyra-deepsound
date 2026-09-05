package com.luc4n3x.levyra.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackDiagnosticsFormatTest {
    @Test
    fun audioSummaryKeepsUsefulCodecDetailsOnly() {
        val summary = PlaybackDiagnosticFormat(
            mimeType = "audio/mp4",
            codecs = "mp4a.40.2",
            bitrateKbps = 256,
            channels = 2,
            sampleRateHz = 48_000
        ).summary()

        assertEquals("audio/mp4 · mp4a.40.2 · 256 kbps · 2 ch · 48000 Hz", summary)
    }

    @Test
    fun videoSummaryIncludesResolution() {
        val summary = PlaybackDiagnosticFormat(
            mimeType = "video/webm",
            codecs = "vp09",
            bitrateKbps = 1_800,
            width = 1920,
            height = 1080
        ).summary()

        assertEquals("video/webm · vp09 · 1800 kbps · 1920x1080", summary)
    }
}

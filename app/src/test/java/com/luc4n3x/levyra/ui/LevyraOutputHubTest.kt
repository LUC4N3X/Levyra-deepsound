package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LevyraOutputHubTest {
    @Test
    fun streamDetailsPreferBestKnownBitrateAndFormatSampleRate() {
        assertEquals(
            "MP4A · 256 kbps · 44.1 kHz",
            buildStreamDetails(
                codec = "mp4a.40.2",
                bitrate = 128_000,
                averageBitrate = 256_000,
                sampleRate = 44_100
            )
        )
    }

    @Test
    fun streamDetailsKeepOnlyAvailableFacts() {
        assertEquals(
            "OPUS · 48 kHz",
            buildStreamDetails(
                codec = "opus",
                bitrate = 0,
                averageBitrate = 0,
                sampleRate = 48_000
            )
        )
        assertEquals("", buildStreamDetails(null, 0, 0, 0))
    }
}

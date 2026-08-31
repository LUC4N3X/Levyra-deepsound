package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.player.PlaybackDiagnosticSnapshot
import com.luc4n3x.levyra.player.PlaybackDiagnosticStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerDiagnosticsSafeReportTest {
    @Test
    fun safeReportContainsOnlySanitizedPlaybackMetadata() {
        val report = PlaybackDiagnosticSnapshot(
            status = PlaybackDiagnosticStatus.HEALTHY,
            appVersion = "2.5.1 (2051000)",
            trackId = "video-id",
            title = "Track",
            artist = "Artist",
            source = "YouTube Music",
            videoMode = false,
            playerState = "READY",
            isPlaying = true,
            positionMs = 10_000L,
            bufferedPositionMs = 20_000L,
            durationMs = 30_000L,
            playbackSpeed = 1f,
            audioSessionId = 10,
            audioFormat = null,
            videoFormat = null,
            cacheBytes = 1024L,
            networkTransport = "wifi",
            networkValidated = true,
            networkMetered = false,
            playerErrorCode = "",
            strategies = emptyList()
        ).safeReport()

        assertTrue(report.contains("video-id"))
        assertTrue(report.contains("Security:"))
        assertFalse(report.contains("http://"))
        assertFalse(report.contains("https://"))
        assertFalse(report.contains("Authorization:"))
        assertFalse(report.contains("Cookie:"))
        assertFalse(report.contains("pot="))
    }
}

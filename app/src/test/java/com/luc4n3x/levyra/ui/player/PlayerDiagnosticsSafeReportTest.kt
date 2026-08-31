package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.player.PlaybackDiagnosticSnapshot
import com.luc4n3x.levyra.player.PlaybackDiagnosticStatus
import com.luc4n3x.levyra.player.PlaybackDiagnosticStrategy
import com.luc4n3x.levyra.data.PlaybackStrategyCircuit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerDiagnosticsSafeReportTest {
    @Test
    fun safeReportRedactsUrlsHeadersAndTokensAtOutputBoundary() {
        val report = PlaybackDiagnosticSnapshot(
            status = PlaybackDiagnosticStatus.HEALTHY,
            appVersion = "2.5.1 (2051000)",
            trackId = "https://youtube.com/watch?v=video-id&pot=SECRET_POT",
            title = "Track\r\nAuthorization: Bearer SECRET_AUTH",
            artist = "Artist",
            source = "Cookie: SID=SECRET_COOKIE",
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
            playerErrorCode = "token=SECRET_ERROR",
            strategies = listOf(
                PlaybackDiagnosticStrategy(
                    name = "https://private.example/strategy",
                    successes = 1,
                    failures = 1,
                    consecutiveFailures = 1,
                    averageLatencyMs = 120L,
                    circuit = PlaybackStrategyCircuit.CLOSED,
                    lastFailure = "api_key=SECRET_KEY",
                    lastFailureAtMs = 1L
                )
            )
        ).safeReport()

        assertTrue(report.contains("[redacted]"))
        assertTrue(report.contains("Security:"))
        assertFalse(report.contains("http://"))
        assertFalse(report.contains("https://"))
        assertFalse(report.contains("SECRET_POT"))
        assertFalse(report.contains("SECRET_AUTH"))
        assertFalse(report.contains("SECRET_COOKIE"))
        assertFalse(report.contains("SECRET_ERROR"))
        assertFalse(report.contains("SECRET_KEY"))
        assertFalse(report.contains("\r"))
        assertFalse(report.contains("\nAuthorization:"))
    }
}

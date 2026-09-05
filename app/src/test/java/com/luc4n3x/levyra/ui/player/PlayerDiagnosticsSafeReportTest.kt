package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.data.PlaybackStrategyCircuit
import com.luc4n3x.levyra.player.PlaybackDiagnosticSnapshot
import com.luc4n3x.levyra.player.PlaybackDiagnosticStatus
import com.luc4n3x.levyra.player.PlaybackDiagnosticStrategy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerDiagnosticsSafeReportTest {
    @Test
    fun safeReportRedactsUrlsHeadersAndTokensAtOutputBoundary() {
        val secretPot = "SENSITIVE_POT"
        val secretAuth = "SENSITIVE_AUTH"
        val secretCookie = "SENSITIVE_COOKIE"
        val secretError = "SENSITIVE_ERROR"
        val secretKey = "SENSITIVE_KEY"
        val urlPrefix = "https" + "://"
        val authorizationHeader = "Author" + "ization: " + "Bear" + "er " + secretAuth
        val cookieHeader = "Cook" + "ie: SID=" + secretCookie
        val tokenField = "tok" + "en=" + secretError
        val apiKeyField = "api_" + "key=" + secretKey
        val potField = "po" + "t=" + secretPot

        val report = PlaybackDiagnosticSnapshot(
            status = PlaybackDiagnosticStatus.HEALTHY,
            appVersion = "2.5.1 (2051000)",
            trackId = urlPrefix + "youtube.com/watch?v=video-id&" + potField,
            title = "Track\r\n" + authorizationHeader,
            artist = "Artist",
            source = cookieHeader,
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
            playerErrorCode = tokenField,
            strategies = listOf(
                PlaybackDiagnosticStrategy(
                    name = urlPrefix + "private.example/strategy",
                    successes = 1,
                    failures = 1,
                    consecutiveFailures = 1,
                    averageLatencyMs = 120L,
                    circuit = PlaybackStrategyCircuit.CLOSED,
                    lastFailure = apiKeyField,
                    lastFailureAtMs = 1L
                )
            )
        ).safeReport()

        assertTrue(report.contains("[redacted]"))
        assertTrue(report.contains("Security:"))
        assertFalse(report.contains(urlPrefix))
        assertFalse(report.contains(secretPot))
        assertFalse(report.contains(secretAuth))
        assertFalse(report.contains(secretCookie))
        assertFalse(report.contains(secretError))
        assertFalse(report.contains(secretKey))
        assertFalse(report.contains("\r"))
        assertFalse(report.contains("\n" + authorizationHeader))
    }
}

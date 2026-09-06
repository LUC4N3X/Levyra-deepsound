package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.data.PlaybackStrategyCircuit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackDiagnosticsTest {

    private val signedUrl = "https://rr3---sn-x.googlevideo.com/videoplayback?expire=1&sig=AOq0QJ8wR"
    private val bearerHeader = "Authorization: Bearer ya29.LONGLIVEDACCESSVALUE"
    private val cookieHeader = "Cookie: SAPISID=SUPERSECRETCOOKIEVALUE; __Secure-3PSID=OTHER"
    private val apiKeyValue = "x-goog-api-key=test_placeholder_value"
    private val poTokenValue = "token=test_placeholder_value"

    private fun hostileSnapshot(): PlaybackDiagnosticSnapshot = PlaybackDiagnosticSnapshot(
        status = PlaybackDiagnosticStatus.HEALTHY,
        appVersion = "1.2.3 (45)",
        trackId = "abc123",
        title = "Song $signedUrl",
        artist = "Artist $bearerHeader",
        source = "youtube $cookieHeader",
        videoMode = true,
        playerState = "READY",
        isPlaying = true,
        positionMs = 12_000L,
        bufferedPositionMs = 30_000L,
        durationMs = 200_000L,
        playbackSpeed = 1f,
        audioSessionId = 7,
        audioFormat = PlaybackDiagnosticFormat(mimeType = "audio/mp4", codecs = "mp4a.40.2", bitrateKbps = 128),
        videoFormat = PlaybackDiagnosticFormat(mimeType = "video/mp4", codecs = "avc1.64001F", width = 1280, height = 720),
        cacheBytes = 12_345L,
        networkTransport = "wifi",
        networkValidated = true,
        networkMetered = false,
        playerErrorCode = "ERROR_CODE_IO_BAD_HTTP_STATUS $apiKeyValue",
        activeStrategy = "REEL_AUDIO $poTokenValue",
        client = PlaybackDiagnosticClient(name = "ANDROID_VR", version = "1.61.48", requiresProofOfOrigin = true),
        policy = PlaybackDiagnosticPolicy(
            schema = 1,
            revision = 2026082701L,
            expired = false,
            appVersionSupported = true,
            audioStrategies = listOf("REEL_AUDIO", "DIRECT"),
            videoStrategies = listOf("PERSISTED"),
            restrictedClients = listOf("ANDROID_VR")
        ),
        strategies = listOf(
            PlaybackDiagnosticStrategy(
                name = "REEL_AUDIO",
                successes = 8,
                failures = 1,
                consecutiveFailures = 0,
                averageLatencyMs = 420L,
                circuit = PlaybackStrategyCircuit.CLOSED,
                lastFailure = "NETWORK $signedUrl",
                lastFailureAtMs = 0L
            )
        )
    )

    @Test
    fun reportNeverExposesStreamUrls() {
        val report = hostileSnapshot().safeReport()

        assertFalse(report.contains("googlevideo", ignoreCase = true))
        assertFalse(report.contains("https://", ignoreCase = true))
        assertFalse(report.contains("http://", ignoreCase = true))
        assertFalse(report.contains("videoplayback", ignoreCase = true))
    }

    @Test
    fun reportNeverExposesAuthorizationHeadersOrCookies() {
        val report = hostileSnapshot().safeReport()

        assertFalse(report.contains("ya29.", ignoreCase = true))
        assertFalse(report.contains("SUPERSECRETCOOKIEVALUE", ignoreCase = true))
        assertFalse(report.contains("__Secure-3PSID=OTHER", ignoreCase = true))
        assertFalse(report.contains("Bearer ya29", ignoreCase = true))
    }

    @Test
    fun reportNeverExposesApiKeysOrProofOfOriginTokens() {
        val report = hostileSnapshot().safeReport()

        assertFalse(report.contains("test_placeholder_value", ignoreCase = true))
    }

    @Test
    fun reportKeepsTheUsefulNonSensitiveFacts() {
        val report = hostileSnapshot().safeReport()

        assertTrue(report.contains("status=HEALTHY"))
        assertTrue(report.contains("audio/mp4"))
        assertTrue(report.contains("1280x720"))
        assertTrue(report.contains("128 kbps"))
        assertTrue(report.contains("wifi"))
        assertTrue(report.contains("ANDROID_VR"))
        assertTrue(report.contains("cache_bytes: 12345"))
        assertTrue(report.contains("duration_ms: 200000"))
    }

    @Test
    fun uiRowsAreSanitizedToo() {
        val snapshot = hostileSnapshot()
        val values = (snapshot.playbackRows() + snapshot.formatRows() + snapshot.networkRows() + snapshot.resolverRows())
            .joinToString("\n") { "${it.first}=${it.second}" }

        assertFalse(values.contains("https://", ignoreCase = true))
        assertFalse(values.contains("ya29.", ignoreCase = true))
        assertFalse(values.contains("SUPERSECRETCOOKIEVALUE", ignoreCase = true))
        assertFalse(values.contains("test_placeholder_value", ignoreCase = true))
    }

    @Test
    fun redactedFieldsAreMarkedNotSilentlyDropped() {
        assertEquals("[redacted]", sanitizeDiagnosticField(signedUrl))
        assertTrue(sanitizeDiagnosticField(bearerHeader).contains("[redacted]"))
        assertTrue(sanitizeDiagnosticField(cookieHeader).contains("[redacted]"))
    }

    @Test
    fun multilineInputCannotForgeExtraReportLines() {
        val forged = sanitizeDiagnosticField("value\nstatus=FAKE\r\ntoken=abc")

        assertFalse(forged.contains("\n"))
        assertFalse(forged.contains("\r"))
    }

    @Test
    fun oversizedFieldsAreTruncated() {
        val long = "a".repeat(5_000)

        assertTrue(sanitizeDiagnosticField(long).length <= 200)
    }

    @Test
    fun missingPlaybackInformationIsHandledSafely() {
        val snapshot = PlaybackDiagnosticSnapshot()
        val report = snapshot.safeReport()

        assertTrue(report.contains("status=IDLE"))
        assertTrue(report.contains("track_id: -"))
        assertTrue(report.contains("audio: -"))
        assertTrue(snapshot.resolverRows().isNotEmpty())
        assertEquals("-", safeDiagnosticValue(""))
        assertEquals("-", safeDiagnosticValue("   "))
    }

    @Test
    fun statusReflectsPlayerAndResolverHealth() {
        val now = 1_750_000_000_000L
        val healthy = PlaybackDiagnosticStrategy(
            name = "REEL_AUDIO",
            successes = 5,
            failures = 0,
            consecutiveFailures = 0,
            averageLatencyMs = 100L,
            circuit = PlaybackStrategyCircuit.CLOSED,
            lastFailure = "",
            lastFailureAtMs = 0L
        )
        val recentlyFailed = healthy.copy(lastFailureAtMs = now - 60_000L)
        val quarantined = healthy.copy(circuit = PlaybackStrategyCircuit.OPEN)

        assertEquals(
            PlaybackDiagnosticStatus.ERROR,
            playbackDiagnosticStatus("ERROR_CODE_IO_UNSPECIFIED", 3, listOf(healthy), now)
        )
        assertEquals(
            PlaybackDiagnosticStatus.IDLE,
            playbackDiagnosticStatus("", null, listOf(healthy), now)
        )
        assertEquals(
            PlaybackDiagnosticStatus.HEALTHY,
            playbackDiagnosticStatus("", 3, listOf(healthy), now)
        )
        assertEquals(
            PlaybackDiagnosticStatus.FALLBACK_HISTORY,
            playbackDiagnosticStatus("", 3, listOf(recentlyFailed), now)
        )
        assertEquals(
            PlaybackDiagnosticStatus.FALLBACK_HISTORY,
            playbackDiagnosticStatus("", 3, listOf(quarantined), now)
        )
    }

    @Test
    fun oldFailuresNoLongerCountAsFallbackHistory() {
        val now = 1_750_000_000_000L
        val strategy = PlaybackDiagnosticStrategy(
            name = "REEL_AUDIO",
            successes = 5,
            failures = 2,
            consecutiveFailures = 0,
            averageLatencyMs = 100L,
            circuit = PlaybackStrategyCircuit.CLOSED,
            lastFailure = "NETWORK",
            lastFailureAtMs = now - (6L * 60L * 60L * 1_000L)
        )

        assertEquals(
            PlaybackDiagnosticStatus.HEALTHY,
            playbackDiagnosticStatus("", 3, listOf(strategy), now)
        )
    }

    @Test
    fun formatSummaryStaysReadable() {
        val summary = PlaybackDiagnosticFormat(
            mimeType = "audio/mp4",
            codecs = "mp4a.40.2",
            bitrateKbps = 132,
            channels = 2,
            sampleRateHz = 44_100
        ).summary()

        assertEquals("audio/mp4 · mp4a.40.2 · 132 kbps · 2 ch · 44100 Hz", summary)
        assertEquals("", PlaybackDiagnosticFormat().summary())
    }
}

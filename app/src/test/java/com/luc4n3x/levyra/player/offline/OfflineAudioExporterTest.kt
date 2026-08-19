package com.luc4n3x.levyra.player.offline

import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class OfflineAudioExporterTest {
    @Test
    fun offlineExportAcceptsOnlyMp4AudioSources() {
        assertTrue(isMp4AudioExportUrl("https://rr.googlevideo.com/videoplayback?mime=audio%2Fmp4&itag=140"))
        assertTrue(isMp4AudioExportUrl("https://example.com/track.m4a?token=abc"))
        assertFalse(isMp4AudioExportUrl("https://rr.googlevideo.com/videoplayback?mime=audio%2Fwebm&itag=251"))
        assertFalse(isMp4AudioExportUrl("https://rr.googlevideo.com/videoplayback?mime=video%2Fmp4&itag=137"))
        assertFalse(isMp4AudioExportUrl("https://example.com/track.webm"))
        assertFalse(isMp4AudioExportUrl("https://example.com/clip.mp4"))
    }

    @Test
    fun responseMimeMustBeMp4Audio() {
        assertTrue(isMp4AudioSource("audio/mp4", "https://example.com/videoplayback"))
        assertTrue(isMp4AudioSource("", "https://example.com/track.m4a"))
        assertFalse(isMp4AudioSource("video/mp4", "https://example.com/track.m4a"))
        assertFalse(isMp4AudioSource("video/mp4", "https://example.com/clip.mp4"))
        assertFalse(isMp4AudioSource("audio/webm", "https://example.com/track.m4a"))
    }

    @Test
    fun incompatibleOfflineSourceErrorsAreDetectedWithoutRetryingTheSameUrl() {
        assertTrue(isUnsupportedOfflineAudioSource(IOException("Offline export requires an M4A audio source")))
        assertTrue(isUnsupportedOfflineAudioSource(IOException("Offline export received a non-audio MP4 source")))
    }

    @Test
    fun incompatibleOfflineSourceDetectionWalksTheCauseChain() {
        val error = IOException("download failed", IOException("Offline export requires an M4A audio source"))

        assertTrue(isUnsupportedOfflineAudioSource(error))
    }

    @Test
    fun transientDownloadErrorsAreNotClassifiedAsIncompatibleSources() {
        assertFalse(isUnsupportedOfflineAudioSource(IOException("timeout while reading stream")))
    }

    @Test
    fun rejectedYoutubePlaybackSourcesForceFreshResolution() {
        assertTrue(isRejectedOfflinePlaybackSource(IOException("Download audio fallito: HTTP 403")))
        assertTrue(isRejectedOfflinePlaybackSource(IOException("Range audio non supportato: HTTP 410")))
        assertTrue(isRejectedOfflinePlaybackSource(IOException("Response code: 429")))
        assertTrue(isRejectedOfflinePlaybackSource(IOException("Sign in to confirm you're not a bot")))
        assertTrue(isRejectedOfflinePlaybackSource(IOException("PoToken rejected")))
        assertFalse(isRejectedOfflinePlaybackSource(IOException("timeout while reading stream")))
        assertFalse(isRejectedOfflinePlaybackSource(IOException("HTTP 500")))
    }

    @Test
    fun googleVideoRangeFanoutIsCappedForPlaybackRiskControl() {
        assertEquals(4, offlineRangeConcurrency("https://rr1---sn.googlevideo.com/videoplayback", 20))
        assertEquals(2, offlineRangeConcurrency("https://rr1---sn.googlevideo.com/videoplayback", 2))
        assertEquals(20, offlineRangeConcurrency("https://example.com/audio.m4a", 20))
    }

    @Test
    fun reelAndPoTokenDownloadsUseSerialResumableTransfer() {
        assertFalse(
            shouldUseParallelOfflineRanges(
                "YouTube Android Reel · preferred",
                "https://rr1---sn.googlevideo.com/videoplayback?itag=18"
            )
        )
        assertFalse(
            shouldUseParallelOfflineRanges(
                "YouTube",
                "https://rr1---sn.googlevideo.com/videoplayback?itag=140&pot=proof"
            )
        )
        assertTrue(
            shouldUseParallelOfflineRanges(
                "YouTube Android VR",
                "https://rr1---sn.googlevideo.com/videoplayback?itag=140&ratebypass=yes"
            )
        )
    }

    @Test
    fun exporterNeverFallsBackToTheDownloadsCollection() {
        val source = sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/player/offline/OfflineAudioExporter.kt"),
            Path.of("src/main/java/com/luc4n3x/levyra/player/offline/OfflineAudioExporter.kt")
        ).firstOrNull(Files::exists) ?: error("OfflineAudioExporter.kt not found")
        val content = Files.readString(source)

        assertFalse(content.contains("MediaStore.Downloads"))
        assertFalse(content.contains("downloadsDestinationLabel"))
    }

    @Test
    fun lowRateLimitsReserveTheEntireTransferDuration() = runBlocking {
        var nowNanos = 0L
        val waits = mutableListOf<Long>()
        val limiter = DownloadRateLimiter(
            maxRateKbps = 512,
            nanoTime = { nowNanos },
            sleepNanos = { nanos ->
                waits += nanos
                nowNanos += nanos
            }
        )

        limiter.consume(512 * 1024)
        limiter.consume(512 * 1024)

        assertEquals(listOf(8_192_000_000L, 8_192_000_000L), waits)
        assertTrue(waits.all { it > 2_000_000_000L })
    }

    @Test
    fun oneMegabitLimitUsesAFullFourSecondBudgetForA512KibBuffer() = runBlocking {
        var nowNanos = 0L
        val limiter = DownloadRateLimiter(
            maxRateKbps = 1024,
            nanoTime = { nowNanos },
            sleepNanos = { nanos -> nowNanos += nanos }
        )

        limiter.consume(512 * 1024)

        assertEquals(4_096_000_000L, nowNanos)
    }

    @Test
    fun longKnownDownloadsAreSplitIntoBoundedRanges() {
        val oneMb = 1024L * 1024L

        val ranges = planParallelAudioRanges(
            contentLength = 10L * oneMb,
            chunkSize = 4L * oneMb,
            minLength = 8L * oneMb
        )

        assertEquals(
            listOf(
                AudioDownloadRange(start = 0L, endInclusive = 4L * oneMb - 1L),
                AudioDownloadRange(start = 4L * oneMb, endInclusive = 8L * oneMb - 1L),
                AudioDownloadRange(start = 8L * oneMb, endInclusive = 10L * oneMb - 1L)
            ),
            ranges
        )
    }

    @Test
    fun smallOrUnknownDownloadsStaySerial() {
        val oneMb = 1024L * 1024L

        assertTrue(
            planParallelAudioRanges(
                contentLength = 6L * oneMb,
                chunkSize = 4L * oneMb,
                minLength = 8L * oneMb
            ).isEmpty()
        )
        assertTrue(planParallelAudioRanges(contentLength = -1L).isEmpty())
    }

    @Test
    fun rangeQueryResponsesCanBeSuccessfulPartialBodies() {
        val range = AudioDownloadRange(start = 1024L, endInclusive = 2047L)

        assertTrue(
            isUsableAudioRangeResponse(
                code = 200,
                bodyLength = 1024L,
                contentRange = "",
                range = range,
                rangeParamApplied = true
            )
        )
    }

    @Test
    fun fullBodyResponsesAreRejectedForRangeChunks() {
        val range = AudioDownloadRange(start = 1024L, endInclusive = 2047L)

        assertFalse(
            isUsableAudioRangeResponse(
                code = 200,
                bodyLength = 4096L,
                contentRange = "",
                range = range,
                rangeParamApplied = true
            )
        )
        assertFalse(
            isUsableAudioRangeResponse(
                code = 200,
                bodyLength = 1024L,
                contentRange = "",
                range = range,
                rangeParamApplied = false
            )
        )
    }

    @Test
    fun contentLengthCanBeReadFromGoogleVideoClenParameter() {
        val url = "https://rr1---sn.googlevideo.com/videoplayback?mime=audio%2Fmp4&clen=73400320&expire=9999999999"

        assertEquals(73400320L, audioContentLengthFromUrl(url))
        assertEquals(-1L, audioContentLengthFromUrl("https://example.com/audio.m4a"))
    }

    @Test
    fun longDownloadsUseAdaptiveChunksAndHighConcurrency() {
        val oneMb = 1024L * 1024L

        assertEquals(9L * 256L * 1024L, parallelAudioChunkSize(70L * oneMb))
        assertEquals(16, parallelAudioConcurrency(70L * oneMb))
        assertEquals(14L * 256L * 1024L, parallelAudioChunkSize(140L * oneMb))
        assertEquals(20, parallelAudioConcurrency(140L * oneMb))
    }

    @Test
    fun taskKeysProduceStableSafePartialFileNames() {
        assertEquals("track_id_with_spaces", offlineDownloadTaskFileKey(" track id with spaces "))
        assertEquals("unknown", offlineDownloadTaskFileKey(""))
    }

    @Test
    fun differingItagProducesADifferentStreamIdentity() {
        val a = offlineStreamIdentity(
            "https://rr1---sn.googlevideo.com/videoplayback?itag=140&clen=1000&mime=audio%2Fmp4&expire=1",
            "abcdefghijk"
        )
        val b = offlineStreamIdentity(
            "https://rr1---sn.googlevideo.com/videoplayback?itag=251&clen=1000&mime=audio%2Fmp4&expire=1",
            "abcdefghijk"
        )

        assertFalse(a == b)
    }

    @Test
    fun differingContentLengthProducesADifferentStreamIdentity() {
        val a = offlineStreamIdentity(
            "https://rr1---sn.googlevideo.com/videoplayback?itag=140&clen=1000&mime=audio%2Fmp4&expire=1",
            "abcdefghijk"
        )
        val b = offlineStreamIdentity(
            "https://rr1---sn.googlevideo.com/videoplayback?itag=140&clen=2000&mime=audio%2Fmp4&expire=1",
            "abcdefghijk"
        )

        assertFalse(a == b)
    }

    @Test
    fun aResignedUrlWithTheSameItagClenAndMimeProducesTheSameStreamIdentity() {
        val original = offlineStreamIdentity(
            "https://rr1---sn.googlevideo.com/videoplayback?itag=140&clen=1000&mime=audio%2Fmp4&expire=1000&sig=aaa",
            "abcdefghijk"
        )
        val refreshed = offlineStreamIdentity(
            "https://rr1---sn.googlevideo.com/videoplayback?itag=140&clen=1000&mime=audio%2Fmp4&expire=9999&sig=zzz",
            "abcdefghijk"
        )

        assertEquals(original, refreshed)
    }

    @Test
    fun mismatchedStreamIdentityDiscardsAndRestartsFromZero() {
        val stored = offlineStreamIdentity(
            "https://rr1---sn.googlevideo.com/videoplayback?itag=140&clen=1000&mime=audio%2Fmp4",
            "abcdefghijk"
        )
        val current = offlineStreamIdentity(
            "https://rr1---sn.googlevideo.com/videoplayback?itag=251&clen=1000&mime=audio%2Fwebm",
            "abcdefghijk"
        )

        assertEquals(0L, resumableBytesForStreamIdentity(existingBytes = 4096L, storedIdentity = stored, currentIdentity = current))
    }

    @Test
    fun matchingStreamIdentityResumesAtTheExistingByteCount() {
        val stored = offlineStreamIdentity(
            "https://rr1---sn.googlevideo.com/videoplayback?itag=140&clen=1000&mime=audio%2Fmp4&expire=1",
            "abcdefghijk"
        )
        val current = offlineStreamIdentity(
            "https://rr1---sn.googlevideo.com/videoplayback?itag=140&clen=1000&mime=audio%2Fmp4&expire=2",
            "abcdefghijk"
        )

        assertEquals(4096L, resumableBytesForStreamIdentity(existingBytes = 4096L, storedIdentity = stored, currentIdentity = current))
    }

    @Test
    fun unknownMissingSidecarIdentityIsTreatedAsUnsafeAndDiscarded() {
        val current = offlineStreamIdentity(
            "https://rr1---sn.googlevideo.com/videoplayback?itag=140&clen=1000&mime=audio%2Fmp4",
            "abcdefghijk"
        )

        assertEquals(
            0L,
            resumableBytesForStreamIdentity(existingBytes = 4096L, storedIdentity = null, currentIdentity = current)
        )
    }

    @Test
    fun streamIdentitySurvivesSerializeAndParseRoundTrip() {
        val identity = offlineStreamIdentity(
            "https://rr1---sn.googlevideo.com/videoplayback?itag=140&clen=1000&mime=audio%2Fmp4",
            "abcdefghijk"
        )

        assertEquals(identity, parseOfflineStreamIdentity(serializeOfflineStreamIdentity(identity)))
    }

    @Test
    fun rejectionErrorsAbortRangeRetryAfterTheFirstAttempt() = runBlocking {
        var attempts = 0

        val error = runCatching {
            retryRangeDownload(
                retryCount = 3,
                retryDelayMs = 10L,
                sleep = { fail("rejection must not trigger a retry sleep") }
            ) {
                attempts++
                throw IOException("Range audio non supportato: HTTP 403")
            }
        }.exceptionOrNull()

        assertEquals(1, attempts)
        assertTrue(error is IOException)
        assertTrue(isRejectedOfflinePlaybackSource(error as IOException))
    }

    @Test
    fun staleRangeStatusesAbortRetryImmediately() = runBlocking {
        listOf(404, 416).forEach { status ->
            var attempts = 0
            val error = runCatching {
                retryRangeDownload(
                    retryCount = 3,
                    retryDelayMs = 10L,
                    sleep = { fail("stale source must not trigger retry sleep") }
                ) {
                    attempts++
                    throw IOException("Range audio non supportato: HTTP $status")
                }
            }.exceptionOrNull()

            assertEquals(1, attempts)
            assertTrue(error is IOException)
            assertTrue(isRejectedOfflinePlaybackSource(error as IOException))
        }
    }

    @Test
    fun transientRangeErrorsConsumeAllConfiguredRetries() = runBlocking {
        var attempts = 0
        var sleeps = 0

        val error = runCatching {
            retryRangeDownload(
                retryCount = 3,
                retryDelayMs = 10L,
                sleep = { sleeps++ }
            ) {
                attempts++
                throw IOException("timeout while reading stream")
            }
        }.exceptionOrNull()

        assertEquals(3, attempts)
        assertEquals(2, sleeps)
        assertTrue(error is IOException)
    }

    @Test
    fun aRejectedRangeFailureIsDetectedAmongOtherFailures() {
        val range = AudioDownloadRange(0L, 1023L)
        val failures = listOf(
            RangeDownloadFailure(range, IOException("timeout while reading stream")),
            RangeDownloadFailure(range, IOException("Range audio non supportato: HTTP 429"))
        )

        val rejected = firstRejectedRangeFailure(failures)

        assertTrue(rejected != null)
        assertTrue(isRejectedOfflinePlaybackSource(rejected as IOException))
    }

    @Test
    fun onlyTransientRangeFailuresDoNotShortCircuitTheBatch() {
        val range = AudioDownloadRange(0L, 1023L)
        val failures = listOf(
            RangeDownloadFailure(range, IOException("timeout while reading stream")),
            RangeDownloadFailure(range, IOException("connection reset"))
        )

        assertEquals(null, firstRejectedRangeFailure(failures))
    }
}

package com.luc4n3x.levyra.player.offline

import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.LevyraDownloadPreset
import com.luc4n3x.levyra.domain.LevyraDownloadSettings
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun exporterNeverFallsBackToTheDownloadsCollection() {
        val source = exporterSource()
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
    fun effectiveFragmentLimitsStayInsideTheDownloadHostBudget() {
        val perHostBudget = LevyraHttpClientFactory.download().dispatcher.maxRequestsPerHost

        LevyraDownloadPreset.entries.forEach { preset ->
            val fragments = LevyraDownloadSettings(preset = preset).maxParallelFragments
            assertTrue("$preset exceeds the per-host download budget", fragments <= perHostBudget)
        }
    }

    @Test
    fun exporterKeepsRangeIntegrityRetryAndSerialFallbackGuards() {
        val content = Files.readString(exporterSource())

        assertTrue(content.contains("if (written != range.length)"))
        assertTrue(content.contains("if (downloadedBytes.get() != targetLength || temp.length() != targetLength)"))
        assertTrue(content.contains("repeat(RANGE_RETRY_COUNT)"))
        assertTrue(content.contains("repeat(PARALLEL_BATCH_RETRY_COUNT)"))
        assertTrue(content.contains("Parallel offline download failed, falling back to serial"))
        assertTrue(content.contains("if (targetLength > 0L && total != targetLength)"))
    }

    @Test
    fun taskKeysProduceStableSafePartialFileNames() {
        assertEquals("track_id_with_spaces", offlineDownloadTaskFileKey(" track id with spaces "))
        assertEquals("unknown", offlineDownloadTaskFileKey(""))
    }

    private fun exporterSource(): Path {
        return sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/player/offline/OfflineAudioExporter.kt"),
            Path.of("src/main/java/com/luc4n3x/levyra/player/offline/OfflineAudioExporter.kt")
        ).firstOrNull(Files::exists) ?: error("OfflineAudioExporter.kt not found")
    }
}

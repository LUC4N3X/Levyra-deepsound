package com.luc4n3x.levyra.player.offline

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineDownloadReliabilityContractTest {
    @Test
    fun parallelRangePlanCoversEveryByteExactlyOnce() {
        val oneMb = 1024L * 1024L
        val lengths = listOf(2L, 7L, 24L, 70L, 140L).map { it * oneMb }

        lengths.forEach { contentLength ->
            val chunkSize = parallelAudioChunkSize(contentLength)
            val ranges = planParallelAudioRanges(
                contentLength = contentLength,
                chunkSize = chunkSize,
                minLength = 1L
            )

            assertTrue(ranges.isNotEmpty())
            assertEquals(0L, ranges.first().start)
            assertEquals(contentLength - 1L, ranges.last().endInclusive)
            assertEquals(contentLength, ranges.sumOf { it.length })

            ranges.zipWithNext().forEach { (current, next) ->
                assertEquals(current.endInclusive + 1L, next.start)
            }
        }
    }

    @Test
    fun missingRangePlannerRestoresCompleteCoverageWithoutOverlap() {
        val oneMb = 1024L * 1024L
        val contentLength = 12L * oneMb
        val covered = listOf(
            AudioDownloadRange(0L, 2L * oneMb - 1L),
            AudioDownloadRange(4L * oneMb, 7L * oneMb - 1L),
            AudioDownloadRange(10L * oneMb, contentLength - 1L)
        )

        val missing = missingAudioRanges(
            contentLength = contentLength,
            coveredRanges = covered,
            chunkSize = oneMb
        )
        val complete = mergeAudioRanges(covered + missing)

        assertEquals(listOf(AudioDownloadRange(0L, contentLength - 1L)), complete)
        assertEquals(contentLength - covered.sumOf { it.length }, missing.sumOf { it.length })
    }

    @Test
    fun truncatedOrMismatchedRangeResponsesAreNeverAccepted() {
        val range = AudioDownloadRange(start = 1_000L, endInclusive = 1_999L)

        assertFalse(
            isUsableAudioRangeResponse(
                code = 206,
                bodyLength = 500L,
                contentRange = "bytes 1000-1999/10000",
                range = range,
                rangeParamApplied = true
            )
        )
        assertFalse(
            isUsableAudioRangeResponse(
                code = 206,
                bodyLength = range.length,
                contentRange = "bytes 0-999/10000",
                range = range,
                rangeParamApplied = true
            )
        )
        assertTrue(
            isUsableAudioRangeResponse(
                code = 206,
                bodyLength = range.length,
                contentRange = "bytes 1000-1999/10000",
                range = range,
                rangeParamApplied = true
            )
        )
    }

    @Test
    fun exporterKeepsIntegrityRetryAndSerialFallbackGuards() {
        val source = Files.readString(sourceFile("player/offline/OfflineAudioExporter.kt"))

        assertTrue(source.contains("if (written != range.length)"))
        assertTrue(source.contains("if (downloadedBytes.get() != targetLength || temp.length() != targetLength)"))
        assertTrue(source.contains("repeat(RANGE_RETRY_COUNT)"))
        assertTrue(source.contains("repeat(PARALLEL_BATCH_RETRY_COUNT)"))
        assertTrue(source.contains("Parallel offline download failed, falling back to serial"))
        assertTrue(source.contains("if (targetLength > 0L && total != targetLength)"))
    }

    @Test
    fun incompatibleM4aSourceStillRotatesInsteadOfRetryingBlindly() {
        val exporter = Files.readString(sourceFile("player/offline/OfflineAudioExporter.kt"))
        val worker = Files.readString(sourceFile("player/offline/work/OfflineExportWorker.kt"))

        assertTrue(exporter.contains("isUnsupportedOfflineAudioSource"))
        assertTrue(exporter.contains("resolver.reportPlaybackFailure"))
        assertTrue(worker.contains("!unsupportedSource && runAttemptCount < 2"))
    }

    private fun sourceFile(relativePath: String): Path {
        return sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/$relativePath"),
            Path.of("src/main/java/com/luc4n3x/levyra/$relativePath")
        ).firstOrNull(Files::exists) ?: error("Source file not found: $relativePath")
    }
}

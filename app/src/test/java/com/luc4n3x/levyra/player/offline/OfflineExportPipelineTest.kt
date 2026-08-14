package com.luc4n3x.levyra.player.offline

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineExportPipelineTest {
    @Test
    fun continuousProgressIsMonotonicAndBounded() {
        val length = 10_000L
        val samples = listOf(0L, 500L, 2_500L, 5_000L, 9_500L, 10_000L)
            .map { offlineContinuousDownloadProgress(it, length) }

        assertEquals(12, samples.first())
        assertEquals(80, samples.last())
        assertTrue(samples.zipWithNext().all { (first, second) -> second >= first })
        assertTrue(samples.all { it in 12..80 })
    }

    @Test
    fun continuousProgressNeverExceedsCompletedBytes() {
        assertEquals(80, offlineContinuousDownloadProgress(15_000L, 10_000L))
        assertEquals(12, offlineContinuousDownloadProgress(-1L, 10_000L))
        assertEquals(12, offlineContinuousDownloadProgress(5_000L, -1L))
    }

    @Test
    fun pipelineUsesMedia3CacheWriterAndYoutubeAwareDataSource() {
        val source = Files.readString(sourceFile("player/offline/OfflineExportPipeline.kt"))

        assertTrue(source.contains("CacheWriter"))
        assertTrue(source.contains("CacheDataSource.Factory()"))
        assertTrue(source.contains("LevyraYoutubeDataSource.Factory"))
        assertTrue(source.contains("PlaybackNetworkStack.playbackFactory"))
        assertTrue(source.contains("LevyraPlaybackCacheKey.stream(track)"))
        assertFalse(source.contains("downloadAudioRanges("))
    }

    @Test
    fun workerPreservesProgressAcrossWorkManagerRetries() {
        val source = Files.readString(sourceFile("player/offline/work/OfflineExportWorker.kt"))

        assertTrue(source.contains("previousProgress = taskDao.byKey(taskKey)?.progress?.coerceIn(1, 99) ?: 1"))
        assertTrue(source.contains("\"RUNNING\", previousProgress"))
        assertTrue(source.contains("var persistedProgress = previousProgress"))
        assertTrue(source.contains("OfflineExportPipeline("))
        assertFalse(source.contains("\"RUNNING\", 1, \"\""))
    }

    private fun sourceFile(relativePath: String): Path {
        return sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/$relativePath"),
            Path.of("src/main/java/com/luc4n3x/levyra/$relativePath")
        ).firstOrNull(Files::exists) ?: error("Source file not found: $relativePath")
    }
}

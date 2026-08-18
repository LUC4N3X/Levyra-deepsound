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
    fun sequentialPrefetchIsSkippedForSourcesThatCannotStreamToTheEnd() {
        val adaptiveMp4Audio = "https://rr3---sn-example.googlevideo.com/videoplayback?itag=140&" +
            "mime=audio%2Fmp4&gir=yes&clen=3168361&c=ANDROID_VR"

        assertFalse(supportsContinuousPrefetch(adaptiveMp4Audio))
        assertTrue(supportsContinuousPrefetch("$adaptiveMp4Audio&ratebypass=yes"))
        assertTrue(supportsContinuousPrefetch("$adaptiveMp4Audio&pot=token-value"))
        assertFalse(
            supportsContinuousPrefetch(
                "https://rr3---sn-example.googlevideo.com/videoplayback?itag=251&" +
                    "mime=audio%2Fwebm&ratebypass=yes&clen=3168361"
            )
        )
    }

    @Test
    fun offlinePipelineAcceptsOnlyCompleteAndroidReelSources() {
        val muxedReel = "https://rr3---sn-example.googlevideo.com/videoplayback?itag=18&" +
            "mime=video%2Fmp4&ratebypass=yes&clen=15857332"
        val attestedAudio = "https://rr3---sn-example.googlevideo.com/videoplayback?itag=140&" +
            "mime=audio%2Fmp4&gir=yes&clen=3168361&pot=token-value"
        val legacyMp4 = "https://rr3---sn-example.googlevideo.com/videoplayback?itag=140&" +
            "mime=audio%2Fmp4&gir=yes&clen=3168361&ratebypass=yes&c=ANDROID_VR"
        val incompleteReel = "https://rr3---sn-example.googlevideo.com/videoplayback?itag=140&" +
            "mime=audio%2Fmp4&gir=yes&clen=3168361"

        assertTrue(isAllowedOfflineReelSource("YouTube Android Reel", "", muxedReel))
        assertTrue(isAllowedOfflineReelSource("", "YouTube Android Reel Audio", attestedAudio))
        assertFalse(isAllowedOfflineReelSource("YouTube Android VR", "LevyraExtractor", legacyMp4))
        assertFalse(isAllowedOfflineReelSource("YouTube Android Reel Audio", "", incompleteReel))
    }

    @Test
    fun pipelineUsesMedia3CacheWriterAndYoutubeAwareDataSource() {
        val source = Files.readString(sourceFile("player/offline/OfflineExportPipeline.kt"))

        assertTrue(source.contains("CacheWriter"))
        assertTrue(source.contains("CacheDataSource.Factory()"))
        assertTrue(source.contains("LevyraYoutubeDataSource.Factory"))
        assertTrue(source.contains("PlaybackNetworkStack.playbackFactory"))
        assertTrue(source.contains("LevyraPlaybackCacheKey.offlineStream(track)"))
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

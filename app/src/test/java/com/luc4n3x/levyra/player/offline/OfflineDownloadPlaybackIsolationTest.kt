package com.luc4n3x.levyra.player.offline

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.LevyraPlaybackCacheKey
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Media3 allows a single writer per cache key: SimpleCache.startReadWriteNonBlocking returns null
 * when the requested range intersects a range another writer already locked, and CacheDataSource
 * then bypasses the cache instead of blocking. A live player holds that lock for its whole open
 * span, so an offline export sharing the key wrote nothing, never advanced its cached-bytes
 * progress and refetched the entire track while competing with playback.
 */
class OfflineDownloadPlaybackIsolationTest {
    private class SingleWriterCache {
        private val lockedRanges = mutableMapOf<String, MutableList<LongRange>>()

        fun tryLock(key: String, start: Long, endInclusive: Long): Boolean {
            val ranges = lockedRanges.getOrPut(key) { mutableListOf() }
            if (ranges.any { it.first <= endInclusive && start <= it.last }) return false
            ranges += start..endInclusive
            return true
        }

        fun release(key: String) {
            lockedRanges.remove(key)
        }
    }

    @Test
    fun offlineExportWriterNeverCollidesWithThePlayerHoldingItsBufferWindow() {
        val cache = SingleWriterCache()
        val playing = track("https://rr.example/videoplayback?itag=18&mime=video%2Fmp4&ratebypass=yes")
        val playerKey = LevyraPlaybackCacheKey.stream(playing)
        val offlineKey = LevyraPlaybackCacheKey.offlineStream(playing)

        assertTrue(cache.tryLock(playerKey, 0L, Long.MAX_VALUE))
        assertFalse(cache.tryLock(playerKey, 0L, Long.MAX_VALUE))
        assertTrue(cache.tryLock(offlineKey, 0L, Long.MAX_VALUE))
    }

    @Test
    fun playbackKeepsItsWriterThroughEveryBufferHorizonWhileADownloadRuns() {
        val cache = SingleWriterCache()
        val playing = track("https://rr.example/videoplayback?itag=140&mime=audio%2Fmp4&ratebypass=yes")
        val playerKey = LevyraPlaybackCacheKey.stream(playing)
        val offlineKey = LevyraPlaybackCacheKey.offlineStream(playing)

        assertTrue(cache.tryLock(offlineKey, 0L, Long.MAX_VALUE))

        // Low-RAM 12 s, aggressive-OEM 20 s and default 24 s max buffers from AdaptivePlaybackPolicy,
        // plus the 30 s and 60 s horizons the stall was reported around.
        listOf(12_000L, 20_000L, 24_000L, 30_000L, 60_000L).forEach { horizonMs ->
            cache.release(playerKey)
            assertTrue(
                "playback loader must keep writing past ${horizonMs}ms while a download runs",
                cache.tryLock(playerKey, 0L, horizonMs)
            )
        }
    }

    @Test
    fun twoConcurrentExportsOfTheSameTrackStillCoalesceOnOneWriter() {
        val cache = SingleWriterCache()
        val exported = track("https://rr.example/videoplayback?itag=140&mime=audio%2Fmp4&ratebypass=yes")
        val offlineKey = LevyraPlaybackCacheKey.offlineStream(exported)

        assertTrue(cache.tryLock(offlineKey, 0L, Long.MAX_VALUE))
        assertFalse(cache.tryLock(offlineKey, 0L, Long.MAX_VALUE))
    }

    @Test
    fun exportSeedsFromBothTheOfflineAndThePlaybackCacheNamespace() {
        val exporter = Files.readString(sourceFile("player/offline/OfflineAudioExporter.kt"))

        assertTrue(exporter.contains("private fun offlineSeedCacheKeys"))
        assertTrue(exporter.contains("LevyraPlaybackCacheKey.offlineStream(track)"))
        assertTrue(exporter.contains("LevyraPlaybackCacheKey.stream(track)"))
        assertTrue(exporter.contains("offlineSeedCacheKeys(track)"))
    }

    @Test
    fun continuousPrefetchWritesIntoTheOfflineNamespace() {
        val pipeline = Files.readString(sourceFile("player/offline/OfflineExportPipeline.kt"))

        assertTrue(pipeline.contains(".setKey(LevyraPlaybackCacheKey.offlineStream(track))"))
        assertFalse(pipeline.contains(".setKey(LevyraPlaybackCacheKey.stream(track))"))
    }

    @Test
    fun exportReleasesItsOwnCacheNamespaceInsteadOfLeakingIt() {
        val exporter = Files.readString(sourceFile("player/offline/OfflineAudioExporter.kt"))
        val persist = exporter.indexOf("persistDownload(track, metadataTrack, fileName")
        val release = exporter.indexOf("releaseOfflineCacheSeed(playable)", persist)

        assertTrue(persist > 0)
        assertTrue(release > persist)
        assertTrue(exporter.contains("removeResource(LevyraPlaybackCacheKey.offlineStream(track))"))
    }

    @Test
    fun everyExportFailureIsReportedAsAnOfflineFailure() {
        val exporter = Files.readString(sourceFile("player/offline/OfflineAudioExporter.kt"))
        val pipeline = Files.readString(sourceFile("player/offline/OfflineExportPipeline.kt"))

        assertEquals(1, occurrences(exporter, "resolver.reportPlaybackFailure"))
        assertEquals(1, occurrences(pipeline, "resolver.reportPlaybackFailure"))
        assertEquals(1, occurrences(exporter, "isOfflineExport = true"))
        assertEquals(1, occurrences(pipeline, "isOfflineExport = true"))
    }

    @Test
    fun everyExportFailureCarriesTheQualityItActuallyResolvedAt() {
        val exporter = Files.readString(sourceFile("player/offline/OfflineAudioExporter.kt"))
        val pipeline = Files.readString(sourceFile("player/offline/OfflineExportPipeline.kt"))

        assertEquals(1, occurrences(exporter, "audioQuality = settings.resolverAudioQuality"))
        assertEquals(1, occurrences(pipeline, "audioQuality = settings.resolverAudioQuality"))
    }

    @Test
    fun anOfflineFailureTargetsTheOfflineSourceMatchAndSparesTheVideoSelector() {
        val resolver = Files.readString(sourceFile("data/PlaybackResolver.kt"))

        assertTrue(resolver.contains("preferMp4Audio = isOfflineExport"))
        assertTrue(resolver.contains("audioQuality = audioQuality?.let(::normalizeAudioQuality) ?: selectedAudioQuality"))
        assertTrue(resolver.contains("if (recovery.rotateCodec && !isOfflineExport)"))
        assertTrue(resolver.contains("invalidate(track, isVideoMode, isOfflineExport)"))
    }

    @Test
    fun aDeadOfflineUrlIsStillQuarantinedSoTheRetryCanRotate() {
        val resolver = Files.readString(sourceFile("data/PlaybackResolver.kt"))

        assertEquals(1, occurrences(resolver, "failedPlaybackUrls[it] = now + recovery.quarantineMs"))
        assertFalse(resolver.contains("if (!isOfflineExport) {"))
    }

    @Test
    fun offlineResolutionNeverEntersThePlaybackStreamCache() {
        val resolver = Files.readString(sourceFile("data/PlaybackResolver.kt"))
        val storeSignature = resolver.indexOf("private fun store(")
        val skip = resolver.indexOf("if (preferMp4Audio) return", storeSignature)
        val storeKey = resolver.indexOf("val key = cacheKey(requestedTrack, isVideoMode, audioQuality)", storeSignature)

        assertTrue(storeSignature > 0)
        assertTrue(skip > storeSignature)
        assertTrue(storeKey > skip)
        assertTrue(resolver.contains("store(track, resolved, isVideoMode, audioQuality, preferMp4Audio)"))
        assertTrue(resolver.contains("store(track, alternate, isVideoMode, audioQuality, preferMp4Audio)"))
        assertTrue(resolver.contains("store(track, restored, isVideoMode, audioQuality, preferMp4Audio)"))
        assertTrue(resolver.contains("if (offlineExport) return"))
    }

    private fun occurrences(source: String, needle: String): Int =
        source.split(needle).size - 1

    private fun sourceFile(relativePath: String): Path {
        return sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/$relativePath"),
            Path.of("src/main/java/com/luc4n3x/levyra/$relativePath")
        ).firstOrNull(Files::exists) ?: error("Source file not found: $relativePath")
    }

    private fun track(streamUrl: String): Track = Track(
        id = "video-id",
        title = "Title",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = streamUrl,
        videoUrl = "https://www.youtube.com/watch?v=video-id",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}

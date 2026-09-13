package com.luc4n3x.levyra.feature.motion

import com.luc4n3x.levyra.domain.LevyraCanvasSource
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MotionArtworkRequestCoordinatorTest {

    @Test
    fun requestKeyStaysStableAcrossProviderIdsAfterMetadataPreparation() {
        val prepared = track(
            id = "catalog-id",
            title = "Same Song",
            artist = "Artist feat. Guest",
            album = "Real Album",
            isrc = "ITABC2600001"
        )
        val sameRecordingFromPlayback = prepared.copy(id = "youtube-id")

        assertEquals(
            motionArtworkRequestKey(prepared, LevyraCanvasSource.Auto),
            motionArtworkRequestKey(sameRecordingFromPlayback, LevyraCanvasSource.Auto)
        )
        assertNotEquals(
            motionArtworkRequestKey(prepared, LevyraCanvasSource.Auto),
            motionArtworkRequestKey(prepared, LevyraCanvasSource.Apple)
        )
    }

    @Test
    fun requestKeySeparatesSameVisibleMetadataWhenIsrcDiffers() {
        val first = track(
            id = "one",
            title = "Same Song",
            artist = "Same Artist",
            album = "Same Album",
            isrc = "ITABC2600001"
        )
        val second = first.copy(id = "two", isrc = "ITABC2600002")

        assertNotEquals(
            motionArtworkRequestKey(first, LevyraCanvasSource.Auto),
            motionArtworkRequestKey(second, LevyraCanvasSource.Auto)
        )
    }

    @Test
    fun cancellingLastSubscriberCancelsUnderlyingResolution() = runBlocking {
        val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val coordinator = MotionArtworkRequestCoordinator(coordinatorScope)
            val started = CompletableDeferred<Unit>()
            val cancelled = CompletableDeferred<Unit>()

            val collector = launch {
                coordinator.share("cancel-request") {
                    started.complete(Unit)
                    try {
                        awaitCancellation()
                    } finally {
                        cancelled.complete(Unit)
                    }
                }.collect {}
            }

            started.await()
            collector.cancelAndJoin()
            withTimeout(1_000L) { cancelled.await() }
        } finally {
            coordinatorScope.cancel()
        }
    }

    @Test
    fun lateSubscriberJoinsExistingRequestAndReceivesUpgradeWithoutSecondBlock() = runBlocking {
        val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val coordinator = MotionArtworkRequestCoordinator(coordinatorScope)
            val firstArtwork = artwork("track-1", "tidal-video-cover")
            val upgradedArtwork = artwork("track-1", "apple-motion")
            val firstPublished = CompletableDeferred<Unit>()
            val releaseUpgrade = CompletableDeferred<Unit>()
            var blockStarts = 0
            val firstEvents = mutableListOf<MotionArtwork>()
            val lateEvents = mutableListOf<MotionArtwork>()

            val firstCollector = launch {
                coordinator.share("same-request") { emit ->
                    blockStarts++
                    emit(firstArtwork)
                    firstPublished.complete(Unit)
                    releaseUpgrade.await()
                    emit(upgradedArtwork)
                }.toList(firstEvents)
            }

            firstPublished.await()

            val lateCollector = launch {
                coordinator.share("same-request") {
                    blockStarts += 100
                    error("duplicate in-flight block started")
                }.toList(lateEvents)
            }

            while (lateEvents.isEmpty()) yield()
            assertEquals(listOf(firstArtwork), lateEvents)

            releaseUpgrade.complete(Unit)
            firstCollector.join()
            lateCollector.join()

            assertEquals(1, blockStarts)
            assertEquals(listOf(firstArtwork, upgradedArtwork), firstEvents)
            assertEquals(listOf(firstArtwork, upgradedArtwork), lateEvents)
        } finally {
            coordinatorScope.cancel()
        }
    }

    private fun artwork(identity: String, provider: String) = MotionArtwork(
        identityKey = identity,
        provider = provider,
        url = "https://example.com/$identity-$provider.mp4",
        mimeType = "video/mp4",
        width = 1080,
        height = 1080,
        confidence = 100,
        expiresAtMs = 1_000_000L,
        lastVerifiedAtMs = 500_000L,
        configEpoch = 1L
    )

    private fun track(
        id: String,
        title: String,
        artist: String,
        album: String,
        isrc: String = ""
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        explicit = false,
        isrc = isrc
    )
}

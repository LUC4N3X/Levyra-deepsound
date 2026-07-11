package com.luc4n3x.levyra.player.queue

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PersistentQueueOrderTest {
    @Test
    fun stableShuffleIsDeterministicAndKeepsCurrentFirst() {
        val tracks = (0 until 12).map(::track)
        val first = stableShuffleOrder(tracks, currentIndex = 5, generation = 17L)
        val second = stableShuffleOrder(tracks, currentIndex = 5, generation = 17L)

        assertEquals(first, second)
        assertEquals(5, first.first())
        assertEquals(tracks.indices.toSet(), first.toSet())
    }

    @Test
    fun aNewGenerationProducesANewStableOrder() {
        val tracks = (0 until 16).map(::track)
        val first = stableShuffleOrder(tracks, currentIndex = 3, generation = 11L)
        val second = stableShuffleOrder(tracks, currentIndex = 3, generation = 12L)

        assertNotEquals(first, second)
        assertEquals(3, second.first())
    }

    @Test
    fun persistentCopyPreservesOfflineUrisAndDropsSignedRemoteUrls() {
        val offline = track(2).copy(streamUrl = "content://media/external/audio/42").queueStoredCopy()
        val remote = track(3).copy(streamUrl = "https://rr.example/videoplayback?expire=10").queueStoredCopy()

        assertEquals("content://media/external/audio/42", offline.streamUrl)
        assertEquals("", remote.streamUrl)
    }

    @Test
    fun youtubeIdentityIgnoresTransientStreamUrls() {
        val first = track(1).copy(streamUrl = "https://one.example/audio?expire=1")
        val second = track(1).copy(streamUrl = "https://two.example/audio?expire=2")

        assertEquals(playbackQueueIdentity(first), playbackQueueIdentity(second))
    }

    private fun track(index: Int): Track = Track(
        id = "track-$index",
        title = "Track $index",
        artist = "Artist $index",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=video$index",
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

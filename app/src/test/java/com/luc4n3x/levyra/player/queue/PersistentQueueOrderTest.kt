package com.luc4n3x.levyra.player.queue

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
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
    fun resolvedHandoffKeepsQueueIdentityWhenResolverAddsCanonicalId() {
        val unresolved = track(4).copy(
            id = "",
            videoUrl = "",
            title = "Unresolved title",
            artist = "Unresolved artist"
        )
        val resolved = track(9).copy(
            id = "resolved-track",
            videoUrl = "https://www.youtube.com/watch?v=resolved9",
            streamUrl = "https://rr.example/videoplayback?expire=10"
        )
        val expectedIdentity = playbackQueueIdentity(unresolved)

        assertNotEquals(expectedIdentity, playbackQueueIdentity(resolved))
        val replacement = resolvedQueueTrackForHandoff(unresolved, expectedIdentity, resolved)
            ?: error("Expected resolved handoff replacement")

        assertEquals(expectedIdentity, playbackQueueIdentity(replacement))
        assertEquals(resolved.streamUrl, replacement.streamUrl)
    }

    @Test
    fun youtubeIdentityIgnoresTransientStreamUrls() {
        val first = track(1).copy(streamUrl = "https://one.example/audio?expire=1")
        val second = track(1).copy(streamUrl = "https://two.example/audio?expire=2")

        assertEquals(playbackQueueIdentity(first), playbackQueueIdentity(second))
    }

    @Test
    fun rebuildingQueueAroundSameCurrentTrackKeepsRestoredPosition() {
        val current = track(6)
        val identity = playbackQueueIdentity(current)

        assertEquals(
            46_000L,
            replacementQueuePositionMs(
                previousIdentity = identity,
                nextIdentity = identity,
                currentPositionMs = 46_000L,
                requestedPositionMs = 0L
            )
        )
    }

    @Test
    fun rebuildingQueueForDifferentTrackStartsAtZero() {
        assertEquals(
            0L,
            replacementQueuePositionMs(
                previousIdentity = playbackQueueIdentity(track(6)),
                nextIdentity = playbackQueueIdentity(track(7)),
                currentPositionMs = 46_000L,
                requestedPositionMs = 0L
            )
        )
    }

    @Test
    fun explicitReplacementPositionWinsOverPreservedPosition() {
        val identity = playbackQueueIdentity(track(6))

        assertEquals(
            12_000L,
            replacementQueuePositionMs(
                previousIdentity = identity,
                nextIdentity = identity,
                currentPositionMs = 46_000L,
                requestedPositionMs = 12_000L
            )
        )
    }

    @Test
    fun failedSkipRestoresSelectionWithoutUndoingNewerQueueSettings() {
        val origin = PlaybackQueueSnapshot(
            tracks = (0..2).map(::track),
            currentIndex = 0,
            history = listOf(2)
        )
        val current = origin.copy(
            tracks = origin.tracks.map { it.copy(album = "Updated album") },
            currentIndex = 1,
            history = listOf(2, 0),
            shuffleEnabled = true,
            shuffleOrder = listOf(1, 2, 0),
            shuffleCursor = 0,
            repeatMode = com.luc4n3x.levyra.domain.RepeatMode.All,
            radioEnabled = true
        )

        val restored = current.withRestoredSkipSelection(origin, 12_000L)

        assertEquals(0, restored.currentIndex)
        assertEquals(12_000L, restored.positionMs)
        assertEquals(origin.history, restored.history)
        assertEquals(current.tracks, restored.tracks)
        assertEquals(current.shuffleEnabled, restored.shuffleEnabled)
        assertEquals(current.shuffleOrder, restored.shuffleOrder)
        assertEquals(2, restored.shuffleCursor)
        assertEquals(current.repeatMode, restored.repeatMode)
        assertEquals(current.radioEnabled, restored.radioEnabled)
    }

    @Test
    fun failedSkipWithRepeatAllPreservesPlayingTrackWhenTheQueueWraps() {
        val playing = track(0)
        val unavailable = track(1)
        val origin = PlaybackQueueSnapshot(
            tracks = listOf(playing, unavailable),
            currentIndex = 0,
            positionMs = 45_000L,
            repeatMode = com.luc4n3x.levyra.domain.RepeatMode.All
        )

        assertFalse(manualSkipWrappedToOrigin(origin, unavailable, 0))
        assertTrue(manualSkipWrappedToOrigin(origin, playing, 1))
        assertFalse(manualSkipWrappedToOrigin(origin, playing, 0))
        assertEquals(45_000L, origin.copy(currentIndex = 1).withRestoredSkipSelection(origin, 45_000L).positionMs)
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

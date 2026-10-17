package com.luc4n3x.levyra.player.queue

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioSessionTest {

    @Test
    fun refillKeepsTheOriginalSeedWhileTheRadioIsStillQueued() {
        val seed = track("seed")
        val session = RadioSession(spaceId = "a", seed = seed).recordAdded(listOf("r1", "r2"))
        val snapshot = snapshot("a", listOf(seed, track("r1"), track("r2")), currentIndex = 2)

        assertEquals("seed", radioSeedFor(session, snapshot)?.id)
    }

    @Test
    fun manualSongChangeInsideTheRadioKeepsTheSeed() {
        val seed = track("seed")
        val session = RadioSession(spaceId = "a", seed = seed).recordAdded(listOf("r1"))
        val snapshot = snapshot("a", listOf(track("r1"), track("manual")), currentIndex = 1)

        assertEquals("seed", radioSeedFor(session, snapshot)?.id)
    }

    @Test
    fun replacedQueueFallsBackToTheCurrentTrack() {
        val session = RadioSession(spaceId = "a", seed = track("seed")).recordAdded(listOf("r1"))
        val snapshot = snapshot("a", listOf(track("p1"), track("p2")), currentIndex = 1)

        assertEquals("p2", radioSeedFor(session, snapshot)?.id)
    }

    @Test
    fun queueSpaceChangeFallsBackToTheCurrentTrack() {
        val seed = track("seed")
        val session = RadioSession(spaceId = "a", seed = seed)
        val snapshot = snapshot("b", listOf(seed, track("x")), currentIndex = 1)

        assertFalse(session.isActiveFor(snapshot))
        assertEquals("x", radioSeedFor(session, snapshot)?.id)
    }

    @Test
    fun exhaustedSeedHandsOverToTheCurrentTrack() {
        val seed = track("seed")
        val session = RadioSession(spaceId = "a", seed = seed).recordAdded(emptyList())
        val snapshot = snapshot("a", listOf(seed, track("r9")), currentIndex = 1)

        assertTrue(session.exhausted)
        assertEquals("r9", radioSeedFor(session, snapshot)?.id)
    }

    @Test
    fun seedChangeStartsAFreshSessionWithoutOldExclusions() {
        val old = RadioSession(spaceId = "a", seed = track("seed")).recordAdded(listOf("r1"))
        val fresh = RadioSession(spaceId = "a", seed = track("other"))

        assertTrue(old.addedIds.contains("r1"))
        assertTrue(fresh.addedIds.isEmpty())
        assertFalse(old == fresh)
    }

    @Test
    fun addedIdsStayBounded() {
        var session = RadioSession(spaceId = "a", seed = track("seed"))
        repeat(40) { batch -> session = session.recordAdded((0 until 5).map { "r${batch}_$it" }) }

        assertEquals(RADIO_SESSION_MAX_ADDED_IDS, session.addedIds.size)
        assertTrue("r39_4" in session.addedIds)
        assertFalse("r0_0" in session.addedIds)
    }

    @Test
    fun mixContextEndsAtTheInsertionPoint() {
        val tracks = (0 until 15).map { track("t$it") }
        val snapshot = snapshot("a", tracks, currentIndex = 4)

        assertEquals((0..4).map { "t$it" }, radioMixContext(snapshot, insertAfterCurrent = true).map(Track::id))
        assertEquals((5..14).map { "t$it" }, radioMixContext(snapshot, insertAfterCurrent = false).map(Track::id))
    }

    @Test
    fun recentlyPlayedFollowsQueueHistory() {
        val tracks = (0 until 4).map { track("t$it") }
        val snapshot = snapshot("a", tracks, currentIndex = 3).copy(history = listOf(0, 2, 2))

        assertEquals(listOf("t0", "t2"), radioRecentlyPlayed(snapshot).map(Track::id))
    }

    private fun snapshot(spaceId: String, tracks: List<Track>, currentIndex: Int) = PlaybackQueueSnapshot(
        spaceId = spaceId,
        tracks = tracks,
        currentIndex = currentIndex,
        radioEnabled = true
    )

    private fun track(id: String) = Track(
        id = id,
        title = "Title $id",
        artist = "Artist $id",
        album = "",
        durationMs = 0L,
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
        accentEnd = 0
    )
}

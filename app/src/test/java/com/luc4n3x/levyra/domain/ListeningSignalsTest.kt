package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningSignalsTest {

    private val now = 1_750_000_000_000L

    private fun track(id: String, artist: String, title: String = "Title $id"): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = "Album",
        durationMs = 200_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "youtube",
        moodTags = setOf("music"),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0,
        accentEnd = 0
    )

    private fun event(
        id: String,
        artist: String,
        listenedMs: Long,
        completed: Boolean = false,
        durationMs: Long = 200_000L,
        startedAt: Long = now
    ): ListenEvent = ListenEvent(
        trackId = id,
        title = "Title $id",
        artist = artist,
        listenedMs = listenedMs,
        trackDurationMs = durationMs,
        completed = completed,
        startedAt = startedAt
    )

    @Test
    fun `completion ratio uses listened over duration and saturates at one`() {
        assertEquals(0.5, ListeningSignalEngine.completionRatio(event("a", "A", 100_000L)), 0.001)
        assertEquals(1.0, ListeningSignalEngine.completionRatio(event("a", "A", 400_000L)), 0.001)
        assertEquals(1.0, ListeningSignalEngine.completionRatio(event("a", "A", 1_000L, completed = true)), 0.001)
    }

    @Test
    fun `unknown duration falls back to the counted play threshold`() {
        val short = event("a", "A", 10_000L, durationMs = 0L)
        val long = event("a", "A", 45_000L, durationMs = 0L)
        assertEquals(0.0, ListeningSignalEngine.completionRatio(short), 0.001)
        assertEquals(1.0, ListeningSignalEngine.completionRatio(long), 0.001)
    }

    @Test
    fun `repeated early skips build a strong negative signal that suppresses the track`() {
        val events = List(3) { index ->
            event("skipme", "Noisy", 8_000L, startedAt = now - index * 1_000L)
        }
        val profile = ListeningSignalEngine.build(events, nowMs = now)
        val signal = profile.tracks.getValue("skipme")

        assertEquals(3, signal.plays)
        assertEquals(0, signal.countedPlays)
        assertEquals(3, signal.earlySkips)
        assertTrue(profile.isSuppressed(track("skipme", "Noisy")))
    }

    @Test
    fun `a favorited track is never suppressed`() {
        val events = List(3) { event("skipme", "Noisy", 8_000L) }
        val profile = ListeningSignalEngine.build(
            events = events,
            favorites = listOf(track("skipme", "Noisy")),
            nowMs = now
        )
        assertFalse(profile.isSuppressed(track("skipme", "Noisy")))
    }

    @Test
    fun `completed repeats outrank a barely played track`() {
        val events = listOf(
            event("loved", "Alpha", 200_000L, completed = true),
            event("loved", "Alpha", 200_000L, completed = true),
            event("meh", "Beta", 12_000L)
        )
        val profile = ListeningSignalEngine.build(events, nowMs = now)
        assertTrue(profile.trackScore(track("loved", "Alpha")) > profile.trackScore(track("meh", "Beta")))
    }

    @Test
    fun `followed artists and playlist presence raise the score`() {
        val plain = ListeningSignalEngine.build(emptyList(), nowMs = now)
        val boosted = ListeningSignalEngine.build(
            events = emptyList(),
            playlistTracks = listOf(track("t1", "Alpha")),
            followedArtists = listOf("Alpha"),
            nowMs = now
        )
        assertEquals(0, plain.trackScore(track("t1", "Alpha")))
        assertTrue(boosted.trackScore(track("t1", "Alpha")) > 0)
    }

    @Test
    fun `artist affinity is credited to every collaborator`() {
        val events = listOf(event("duet", "Alpha feat. Beta", 200_000L, completed = true))
        val profile = ListeningSignalEngine.build(events, nowMs = now)

        assertTrue(profile.artists.containsKey("alpha"))
        assertTrue(profile.artists.containsKey("beta"))
        assertTrue(profile.artistScore("Beta") > 0)
    }

    @Test
    fun `ranker keeps original order when there is no signal`() {
        val candidates = listOf(track("a", "Alpha"), track("b", "Beta"))
        val ranked = ListeningSignalRanker.rank(candidates, ListeningSignalProfile())
        assertEquals(candidates, ranked)
    }

    @Test
    fun `ranker drops suppressed candidates but never returns an empty list`() {
        val events = List(3) { event("skipme", "Noisy", 8_000L) }
        val profile = ListeningSignalEngine.build(events, nowMs = now)

        val mixed = ListeningSignalRanker.rank(
            listOf(track("skipme", "Noisy"), track("keep", "Alpha")),
            profile
        )
        assertEquals(listOf(track("keep", "Alpha")), mixed)

        val onlySuppressed = ListeningSignalRanker.rank(listOf(track("skipme", "Noisy")), profile)
        assertEquals(1, onlySuppressed.size)
    }

    @Test
    fun `ranker limits consecutive tracks from the same artist`() {
        val candidates = listOf(
            track("a1", "Alpha"),
            track("a2", "Alpha"),
            track("a3", "Alpha"),
            track("b1", "Beta")
        )
        val profile = ListeningSignalEngine.build(
            events = listOf(event("a1", "Alpha", 200_000L, completed = true)),
            followedArtists = listOf("Alpha"),
            nowMs = now
        )
        val ranked = ListeningSignalRanker.rank(candidates, profile, limit = 3, artistRunLimit = 2)

        assertEquals(3, ranked.size)
        assertEquals("Beta", ranked[2].artist)
    }

    @Test
    fun `deferred higher ranked artist becomes eligible after another artist`() {
        val candidates = listOf(
            track("a1", "Alpha"),
            track("a2", "Alpha"),
            track("a3", "Alpha"),
            track("b1", "Beta"),
            track("c1", "Charlie")
        )
        val profile = ListeningSignalEngine.build(
            events = listOf(
                event("a1", "Alpha", 200_000L, completed = true),
                event("a2", "Alpha", 200_000L, completed = true),
                event("a3", "Alpha", 200_000L, completed = true)
            ),
            nowMs = now
        )

        val ranked = ListeningSignalRanker.rank(candidates, profile, limit = 4, artistRunLimit = 2)

        assertEquals(listOf("a1", "a2", "b1", "a3"), ranked.map { it.id })
    }

    @Test
    fun `context artist keeps the hero artist in front and exempt from the run limit`() {
        val candidates = listOf(
            track("b1", "Beta"),
            track("a1", "Alpha"),
            track("a2", "Alpha"),
            track("a3", "Alpha")
        )
        val profile = ListeningSignalEngine.build(
            events = listOf(event("b1", "Beta", 200_000L, completed = true)),
            followedArtists = listOf("Beta"),
            nowMs = now
        )
        val ranked = ListeningSignalRanker.rank(
            candidates = candidates,
            profile = profile,
            limit = 4,
            contextArtist = "Alpha",
            artistRunLimit = 2
        )

        assertEquals(listOf("Alpha", "Alpha", "Alpha", "Beta"), ranked.map { it.artist })
    }

    @Test
    fun `reorder only mode keeps every candidate so repeated ranking cannot shrink a shelf`() {
        val events = List(3) { event("skipme", "Noisy", 8_000L) }
        val profile = ListeningSignalEngine.build(events, nowMs = now)
        val shelf = listOf(track("skipme", "Noisy"), track("keep", "Alpha"))

        var current = shelf
        repeat(5) {
            current = ListeningSignalRanker.rank(
                candidates = current,
                profile = profile,
                limit = current.size,
                dropSuppressed = false
            )
        }

        assertEquals(shelf.size, current.size)
        assertEquals(shelf.map { it.id }.toSet(), current.map { it.id }.toSet())
        assertEquals("keep", current.first().id)
    }

    @Test
    fun `context artist survives suppression of its own catalogue`() {
        val events = List(3) { event("a1", "Alpha", 8_000L) }
        val profile = ListeningSignalEngine.build(events, nowMs = now)
        val ranked = ListeningSignalRanker.rank(
            candidates = listOf(track("a1", "Alpha")),
            profile = profile,
            contextArtist = "Alpha"
        )
        assertEquals(1, ranked.size)
    }
}

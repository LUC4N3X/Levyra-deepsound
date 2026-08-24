package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraMixRankerTest {

    private fun track(id: String, artist: String = "Artist") = Track(
        id = id,
        title = "Title " + id,
        artist = artist,
        album = "",
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
        accentEnd = 0
    )

    private fun listen(id: String, artist: String, startedAt: Long) = ListenEvent(
        trackId = id,
        title = "Title " + id,
        artist = artist,
        listenedMs = 120_000L,
        trackDurationMs = 180_000L,
        completed = true,
        startedAt = startedAt
    )

    @Test
    fun rankDeduplicatesByTrackIdentity() {
        val candidates = listOf(
            LevyraMixCandidate(track("a"), familiarity = 0.5f, affinity = 0.5f, recentlyPlayed = false),
            LevyraMixCandidate(track("a"), familiarity = 0.9f, affinity = 0.9f, recentlyPlayed = false),
            LevyraMixCandidate(track("b"), familiarity = 0.5f, affinity = 0.5f, recentlyPlayed = false)
        )
        val ranked = LevyraMixRanker.rank(candidates, familiarityBias = 0.5f)
        assertEquals(2, ranked.size)
        assertEquals(setOf("a", "b"), ranked.map { it.id }.toSet())
    }

    @Test
    fun discoveryBiasPrefersUnfamiliarTracks() {
        val familiar = LevyraMixCandidate(track("familiar"), familiarity = 1f, affinity = 1f, recentlyPlayed = false)
        val fresh = LevyraMixCandidate(track("fresh"), familiarity = 0f, affinity = 0f, recentlyPlayed = false)
        val discovery = LevyraMixRanker.rank(listOf(familiar, fresh), familiarityBias = 0f)
        val comfort = LevyraMixRanker.rank(listOf(familiar, fresh), familiarityBias = 1f)
        assertEquals("fresh", discovery.first().id)
        assertEquals("familiar", comfort.first().id)
    }

    @Test
    fun excludeRecentDropsJustPlayedTracks() {
        val candidates = listOf(
            LevyraMixCandidate(track("recent"), familiarity = 0.5f, affinity = 0.5f, recentlyPlayed = true),
            LevyraMixCandidate(track("other"), familiarity = 0.5f, affinity = 0.5f, recentlyPlayed = false)
        )
        val ranked = LevyraMixRanker.rank(candidates, familiarityBias = 0.5f, excludeRecent = true)
        assertEquals(listOf("other"), ranked.map { it.id })
    }

    @Test
    fun rankRespectsRequestedLimit() {
        val candidates = (1..50).map {
            LevyraMixCandidate(track(it.toString()), familiarity = 0.5f, affinity = 0.5f, recentlyPlayed = false)
        }
        assertEquals(10, LevyraMixRanker.rank(candidates, familiarityBias = 0.5f, limit = 10).size)
    }

    @Test
    fun buildMixCandidatesDerivesFamiliarityAndRecency() {
        val now = 1_000_000_000L
        val listens = listOf(
            listen("a", "Artist A", now - 60_000L),
            listen("a", "Artist A", now - 120_000L),
            listen("b", "Artist B", now - 30L * 24L * 60L * 60L * 1000L)
        )
        val candidates = buildMixCandidates(
            tracks = listOf(track("a", "Artist A"), track("b", "Artist B"), track("c", "Artist C")),
            listens = listens,
            nowMs = now
        )
        assertEquals(3, candidates.size)
        val a = candidates.first { it.track.id == "a" }
        val c = candidates.first { it.track.id == "c" }
        assertEquals(1f, a.familiarity, 0.0001f)
        assertEquals(0f, c.familiarity, 0.0001f)
        assertTrue(a.recentlyPlayed)
        assertTrue(!c.recentlyPlayed)
    }
}

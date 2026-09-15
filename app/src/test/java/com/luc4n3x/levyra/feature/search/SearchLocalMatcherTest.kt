package com.luc4n3x.levyra.feature.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchLocalMatcherTest {
    @Test
    fun `exact title is a strong first match`() {
        val matches = matchLocalSearchTracks(
            query = "sirio",
            candidates = listOf(
                LocalSearchCandidate(searchTestTrack("r1", "Sirio Remix", "Other"), LocalSearchAffinity.RECENT),
                LocalSearchCandidate(searchTestTrack("a1", "Sirio", "Geolier"), LocalSearchAffinity.CACHE)
            )
        )

        assertEquals(listOf("a1", "r1"), matches.map { it.track.id })
        assertTrue(matches.first().strong)
        assertFalse(matches.last().strong)
    }

    @Test
    fun `partial last token matches while typing`() {
        val matches = matchLocalSearchTracks(
            query = "geoli",
            candidates = listOf(LocalSearchCandidate(searchTestTrack("g1", "I P' ME", "Geolier"), LocalSearchAffinity.HOME))
        )

        assertEquals(listOf("g1"), matches.map { it.track.id })
        assertFalse(matches.single().strong)
    }

    @Test
    fun `every token must match title or artist`() {
        val matches = matchLocalSearchTracks(
            query = "geolier adele",
            candidates = listOf(LocalSearchCandidate(searchTestTrack("g1", "I P' ME", "Geolier"), LocalSearchAffinity.HOME))
        )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `title plus artist query is strong`() {
        val matches = matchLocalSearchTracks(
            query = "hello adele",
            candidates = listOf(LocalSearchCandidate(searchTestTrack("h1", "Hello", "Adele"), LocalSearchAffinity.FAVORITE))
        )

        assertTrue(matches.single().strong)
    }

    @Test
    fun `duplicate identity keeps the highest affinity source`() {
        val track = searchTestTrack("x1", "Hello", "Adele")
        val matches = matchLocalSearchTracks(
            query = "hello",
            candidates = listOf(
                LocalSearchCandidate(track, LocalSearchAffinity.RECENT),
                LocalSearchCandidate(track.copy(album = "25"), LocalSearchAffinity.CACHE)
            )
        )

        assertEquals(1, matches.size)
        assertEquals(1_000 + LocalSearchAffinity.RECENT, matches.single().score)
    }

    @Test
    fun `rejected and blank identity tracks are skipped`() {
        val matches = matchLocalSearchTracks(
            query = "hello",
            candidates = listOf(
                LocalSearchCandidate(searchTestTrack("", "Hello", "Adele"), LocalSearchAffinity.RECENT),
                LocalSearchCandidate(searchTestTrack("blocked", "Hello There", "Someone"), LocalSearchAffinity.RECENT),
                LocalSearchCandidate(searchTestTrack("ok", "Hello World", "Someone Else"), LocalSearchAffinity.RECENT)
            ),
            accept = { it.id != "blocked" }
        )

        assertEquals(listOf("ok"), matches.map { it.track.id })
    }

    @Test
    fun `short queries and limits are respected`() {
        val candidates = (1..10).map { index ->
            LocalSearchCandidate(searchTestTrack("t$index", "Love $index", "Artist $index", durationMs = 100_000L + index), 0)
        }

        assertTrue(matchLocalSearchTracks("l", candidates).isEmpty())
        assertEquals(3, matchLocalSearchTracks("love", candidates, limit = 3).size)
    }
}

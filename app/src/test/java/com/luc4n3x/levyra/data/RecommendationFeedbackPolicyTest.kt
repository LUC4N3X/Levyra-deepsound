package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RecommendationFeedbackPolicyTest {
    @Test
    fun `blocked artist is removed from recommendations`() {
        val blocked = track("blocked-1", "Song A", "The Weeknd")
        val allowed = track("allowed-1", "Song B", "Daft Punk")

        val ranked = rankRecommendationCandidates(
            candidates = listOf(blocked, allowed),
            feedback = RecommendationFeedbackSnapshot(
                blockedArtistKeys = setOf("weeknd")
            )
        )

        assertEquals(listOf("allowed-1"), ranked.map(Track::id))
    }

    @Test
    fun `positive artist affinity outranks provider order`() {
        val neutral = track("neutral", "Neutral", "Artist B")
        val preferred = track("preferred", "Preferred", "Artist A")

        val ranked = rankRecommendationCandidates(
            candidates = listOf(neutral, preferred),
            feedback = RecommendationFeedbackSnapshot(
                artistAffinity = mapOf("artist a" to 2)
            )
        )

        assertEquals("preferred", ranked.first().id)
    }

    @Test
    fun `negative track signal pushes exact track behind neutral candidates`() {
        val avoided = track("avoid-me", "Avoid", "Artist A")
        val neutral = track("neutral", "Neutral", "Artist B")

        val ranked = rankRecommendationCandidates(
            candidates = listOf(avoided, neutral),
            feedback = RecommendationFeedbackSnapshot(
                avoidedTrackIds = setOf("avoid-me")
            )
        )

        assertEquals("neutral", ranked.first().id)
    }

    @Test
    fun `artist diversity keeps a third track behind other artists`() {
        val first = track("a1", "One", "Artist A")
        val second = track("a2", "Two", "Artist A")
        val third = track("a3", "Three", "Artist A")
        val other = track("b1", "Other", "Artist B")

        val ranked = rankRecommendationCandidates(
            candidates = listOf(first, second, third, other),
            feedback = RecommendationFeedbackSnapshot()
        )

        assertEquals(listOf("a1", "a2", "b1", "a3"), ranked.map(Track::id))
    }

    @Test
    fun `existing queue ids and duplicate candidates are excluded`() {
        val existing = track("existing", "Existing", "Artist A")
        val candidate = track("new", "New", "Artist B")

        val ranked = rankRecommendationCandidates(
            candidates = listOf(existing, candidate, candidate),
            feedback = RecommendationFeedbackSnapshot(),
            excludedTrackIds = setOf("existing")
        )

        assertEquals(listOf("new"), ranked.map(Track::id))
        assertFalse(ranked.any { it.id == "existing" })
    }

    private fun track(id: String, title: String, artist: String): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = "",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=$id",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}

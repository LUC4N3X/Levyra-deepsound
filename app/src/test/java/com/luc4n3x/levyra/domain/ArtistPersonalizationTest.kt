package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistPersonalizationTest {
    @Test
    fun ranksRecentCompletedRepeatedArtistFirst() {
        val now = 2_000_000_000_000L
        val events = listOf(
            event("UC_SHIVA", "Shiva", now - 60_000L, 180_000L, true),
            event("UC_SHIVA", "Shiva", now - 120_000L, 160_000L, true),
            event("UC_ANNA", "ANNA", now - 2_000_000L, 90_000L, false)
        )

        val ranked = rankPersonalizedArtistCandidates(events, nowMs = now, limit = 8)

        assertEquals("UC_SHIVA", ranked.first().browseId)
        assertEquals("Shiva", ranked.first().name)
        assertTrue(ranked.first().score > ranked[1].score)
    }

    @Test
    fun ignoresEventsWithoutOfficialArtistBrowseId() {
        val now = 2_000_000_000_000L
        val ranked = rankPersonalizedArtistCandidates(
            events = listOf(event("", "HIT CANZONI", now - 1_000L, 300_000L, true)),
            nowMs = now,
            limit = 8
        )

        assertTrue(ranked.isEmpty())
    }

    @Test
    fun collaborationUsesPrimaryOfficialArtist() {
        val now = 2_000_000_000_000L
        val ranked = rankPersonalizedArtistCandidates(
            events = listOf(
                ListenEvent(
                    trackId = "track",
                    title = "Non lo sai",
                    artist = "Shiva & Geolier",
                    listenedMs = 180_000L,
                    trackDurationMs = 180_000L,
                    completed = true,
                    startedAt = now - 1_000L,
                    artistBrowseIds = listOf("UC_SHIVA", "UC_GEOLIER")
                )
            ),
            nowMs = now,
            limit = 8
        )

        assertEquals(1, ranked.size)
        assertEquals("UC_SHIVA", ranked.first().browseId)
        assertEquals("Shiva", ranked.first().name)
    }

    @Test
    fun returnsThirteenDistinctArtistsWhenEnoughListeningSignalsExist() {
        val now = 2_000_000_000_000L
        val events = (1..20).map { index ->
            event(
                browseId = "UC_ARTIST_$index",
                artist = "Artist $index",
                startedAt = now - index * 60_000L,
                listenedMs = 120_000L + index,
                completed = index % 2 == 0
            )
        }

        val ranked = rankPersonalizedArtistCandidates(events, nowMs = now, limit = 13)

        assertEquals(13, ranked.size)
        assertEquals(13, ranked.map { it.browseId }.distinct().size)
    }

    private fun event(
        browseId: String,
        artist: String,
        startedAt: Long,
        listenedMs: Long,
        completed: Boolean
    ): ListenEvent = ListenEvent(
        trackId = "$browseId-$startedAt",
        title = "Track",
        artist = artist,
        listenedMs = listenedMs,
        trackDurationMs = 200_000L,
        completed = completed,
        startedAt = startedAt,
        artistBrowseIds = listOfNotNull(browseId.takeIf(String::isNotBlank))
    )
}

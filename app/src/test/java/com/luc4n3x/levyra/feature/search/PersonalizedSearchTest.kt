package com.luc4n3x.levyra.feature.search

import com.luc4n3x.levyra.domain.SmartMusicTasteSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalizedSearchTest {
    @Test
    fun `cold start stays empty and needs no remote data`() {
        val snapshot = buildPersonalizedSearchSnapshot(
            favorites = emptyList(),
            recentListens = emptyList(),
            recentSearches = emptyList(),
            personalOrbitTracks = emptyList(),
            topArtists = emptyList(),
            dayBucket = 10L
        )

        assertTrue(snapshot.tracks.isEmpty())
        assertTrue(snapshot.artistNames.isEmpty())
        assertNull(snapshot.prompt)
    }

    @Test
    fun `tracks are deduplicated and one artist cannot monopolize suggestions`() {
        val sameArtist = (1..6).map { searchTestTrack("a$it", "Song $it", "Adele") }
        val crossSourceDuplicate = searchTestTrack("b2", "Yellow", "Coldplay")
        val snapshot = buildPersonalizedSearchSnapshot(
            favorites = sameArtist,
            recentListens = sameArtist + searchTestTrack("b1", "Yellow", "Coldplay"),
            recentSearches = listOf(searchTestTrack("c1", "One More Time", "Daft Punk")),
            personalOrbitTracks = listOf(crossSourceDuplicate, searchTestTrack("d1", "Midnight City", "M83")),
            topArtists = emptyList(),
            dayBucket = 20L
        )

        assertEquals(snapshot.tracks.map { it.id }.distinct(), snapshot.tracks.map { it.id })
        assertEquals(1, snapshot.tracks.count { it.title == "Yellow" && it.artist == "Coldplay" })
        assertTrue(snapshot.tracks.count { it.artist == "Adele" } <= 2)
        assertTrue(snapshot.tracks.map { it.artist }.distinct().size >= 3)
    }

    @Test
    fun `daily rotation is stable within a day and controlled across days`() {
        val listens = (1..8).map { searchTestTrack("t$it", "Song $it", "Artist $it") }
        val first = buildPersonalizedSearchSnapshot(emptyList(), listens, emptyList(), emptyList(), emptyList(), 100L)
        val repeated = buildPersonalizedSearchSnapshot(emptyList(), listens, emptyList(), emptyList(), emptyList(), 100L)
        val nextDay = buildPersonalizedSearchSnapshot(emptyList(), listens, emptyList(), emptyList(), emptyList(), 101L)

        assertEquals(first, repeated)
        assertNotEquals(first.tracks.map { it.id }, nextDay.tracks.map { it.id })
    }

    @Test
    fun `top local taste artists lead artist suggestions`() {
        val snapshot = buildPersonalizedSearchSnapshot(
            favorites = emptyList(),
            recentListens = listOf(searchTestTrack("x", "Track", "Other Artist")),
            recentSearches = emptyList(),
            personalOrbitTracks = emptyList(),
            topArtists = listOf(SmartMusicTasteSeed("Daft Punk", "Daft Punk", 100)),
            dayBucket = 3L
        )

        assertEquals("Daft Punk", snapshot.artistNames.first())
        assertTrue(snapshot.prompt != null)
    }

    @Test
    fun `existing artist hits are reordered without inventing destinations`() {
        val ranked = rankPersonalizedSearchArtists(
            artists = listOf(
                searchTestArtist("Coldplay", "coldplay"),
                searchTestArtist("Daft Punk", "daft-punk"),
                searchTestArtist("Adele", "adele")
            ),
            preferredNames = listOf("Adele", "Daft Punk")
        )

        assertEquals(listOf("Adele", "Daft Punk", "Coldplay"), ranked.map { it.name })
    }
}

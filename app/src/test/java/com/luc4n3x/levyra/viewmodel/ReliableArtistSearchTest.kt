package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReliableArtistSearchTest {

    @Test
    fun exactVerifiedArtistIsPlacedBeforeUnrelatedGeneralResults() {
        val hugel = artist("HUGEL", "UC-hugel")
        val result = mergeReliableArtistSearchResults(
            query = "hugel",
            exactArtist = hugel,
            verifiedArtists = listOf(
                artist("Solto (FR)", "UC-solto"),
                artist("Ultra Naté", "UC-ultra")
            )
        )

        assertEquals("HUGEL", result.first().name)
        assertTrue(result.any { it.browseId == "UC-hugel" })
    }

    @Test
    fun exactArtistMatchingIgnoresCaseAndOuterWhitespace() {
        val result = mergeReliableArtistSearchResults(
            query = "  hugel  ",
            exactArtist = artist("HUGEL", "UC-hugel"),
            verifiedArtists = emptyList()
        )

        assertEquals("UC-hugel", result.single().browseId)
    }

    @Test
    fun exactArtistIsDeduplicatedByBrowseId() {
        val exact = artist("HUGEL", "UC-hugel")
        val duplicate = exact.copy(subscribers = "2M")

        val result = mergeReliableArtistSearchResults(
            query = "HUGEL",
            exactArtist = exact,
            verifiedArtists = listOf(duplicate)
        )

        assertEquals(1, result.size)
        assertEquals("UC-hugel", result.single().browseId)
    }

    @Test
    fun unrelatedDirectLookupIsNotInjectedAsExactResult() {
        val result = mergeReliableArtistSearchResults(
            query = "HUGEL",
            exactArtist = artist("Hugel Angel", "UC-wrong"),
            verifiedArtists = listOf(artist("Ultra Naté", "UC-ultra"))
        )

        assertTrue(result.none { it.browseId == "UC-wrong" })
    }

    @Test
    fun resultLimitIsBounded() {
        val artists = (1..20).map { index -> artist("Artist $index", "UC-$index") }

        val result = mergeReliableArtistSearchResults(
            query = "missing",
            exactArtist = null,
            verifiedArtists = artists,
            limit = 8
        )

        assertEquals(8, result.size)
    }

    @Test
    fun exactVerifiedArtistReplacesProviderPlaceholderOnTopResult() {
        val placeholder = track("Felices los 4", "YouTube Music")

        val result = enrichSearchTracksWithExactArtist(
            query = "maluma",
            results = SearchResults(topTrack = placeholder, songs = listOf(placeholder)),
            reliableArtists = listOf(artist("Maluma", "UC-maluma"))
        )

        assertEquals("Maluma", result.topTrack?.artist)
        assertEquals(listOf("UC-maluma"), result.topTrack?.artistBrowseIds)
        assertEquals("Maluma", result.songs.single().artist)
    }

    @Test
    fun realTrackArtistIsNeverReplacedBySearchArtist() {
        val collaboration = track("Qué Pretendes", "J Balvin & Bad Bunny")

        val result = enrichSearchTracksWithExactArtist(
            query = "maluma",
            results = SearchResults(topTrack = collaboration, songs = listOf(collaboration)),
            reliableArtists = listOf(artist("Maluma", "UC-maluma"))
        )

        assertEquals("J Balvin & Bad Bunny", result.topTrack?.artist)
        assertTrue(result.topTrack?.artistBrowseIds.orEmpty().isEmpty())
    }

    @Test
    fun unrelatedVerifiedArtistDoesNotReplaceProviderPlaceholder() {
        val placeholder = track("Felices los 4", "YouTube Music")

        val result = enrichSearchTracksWithExactArtist(
            query = "felices los 4",
            results = SearchResults(topTrack = placeholder, songs = listOf(placeholder)),
            reliableArtists = listOf(artist("Maluma", "UC-maluma"))
        )

        assertEquals("YouTube Music", result.topTrack?.artist)
    }

    @Test
    fun existingBrowseIdIsPreservedWhenPlaceholderArtistIsEnriched() {
        val placeholder = track(
            title = "Felices los 4",
            artist = "YouTube Music",
            artistBrowseIds = listOf("UC-existing")
        )

        val result = enrichSearchTracksWithExactArtist(
            query = "maluma",
            results = SearchResults(topTrack = placeholder),
            reliableArtists = listOf(artist("Maluma", "UC-maluma"))
        )

        assertEquals("Maluma", result.topTrack?.artist)
        assertEquals(listOf("UC-existing"), result.topTrack?.artistBrowseIds)
    }

    private fun artist(name: String, browseId: String) = ArtistHit(
        name = name,
        subscribers = "",
        thumbnailUrl = "https://example.com/$browseId.jpg",
        accentStart = 0,
        accentEnd = 0,
        browseId = browseId,
        officialArtwork = true
    )

    private fun track(
        title: String,
        artist: String,
        artistBrowseIds: List<String> = emptyList()
    ) = Track(
        id = title.lowercase().replace(' ', '-'),
        title = title,
        artist = artist,
        album = "",
        durationMs = 0L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        artistBrowseIds = artistBrowseIds
    )
}

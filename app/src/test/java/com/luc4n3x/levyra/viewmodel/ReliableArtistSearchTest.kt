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

    @Test
    fun leadingArticleArtistWinsOverTinyHomonym() {
        val official = artist("The Weeknd", "UC-official", subscribers = "Artist · 274M monthly audience")
        val homonym = artist("Weeknd", "UC-homonym", subscribers = "Artist · 7 subscribers")

        val result = mergeReliableArtistSearchResults(
            query = "weeknd",
            exactArtist = homonym,
            verifiedArtists = listOf(homonym, official)
        )

        assertEquals("The Weeknd", result.first().name)
    }

    @Test
    fun nearExactMisspellingKeepsOnlyAuthoritativeArtist() {
        val official = artist(
            "The Weeknd",
            "UC-official",
            subscribers = "Artist · 274M monthly audience"
        )
        val tinyHomonym = artist(
            "The weeknd",
            "UC-tiny",
            subscribers = "Artist · 7 subscribers"
        )
        val nearbyName = artist(
            "The Weekend Dreamers",
            "UC-dreamers",
            subscribers = "Artist · 20K subscribers"
        )

        val result = mergeReliableArtistSearchResults(
            query = "the weekend",
            exactArtist = tinyHomonym,
            verifiedArtists = listOf(tinyHomonym, nearbyName, official)
        )

        assertEquals(listOf("UC-official"), result.map { it.browseId })
    }

    @Test
    fun nearExactMisspellingSkipsUnrenderableHighestAuthorityArtist() {
        val unusable = artist(
            "The Weeknd",
            "UC-unusable",
            subscribers = "Artist · 274M monthly audience",
            thumbnailUrl = ""
        )
        val fallback = artist(
            "The Weeknd",
            "UC-fallback",
            subscribers = "Artist · 100M monthly audience"
        )

        val result = mergeReliableArtistSearchResults(
            query = "the weekend",
            exactArtist = unusable,
            verifiedArtists = listOf(unusable, fallback)
        )

        assertEquals(listOf("UC-fallback"), result.map { it.browseId })
    }

    @Test
    fun tinyHomonymIsNotPropagatedToSongCredits() {
        val placeholder = track("After Hours", "YouTube Music")

        val result = enrichSearchTracksWithExactArtist(
            query = "weeknd",
            results = SearchResults(topTrack = placeholder, songs = listOf(placeholder)),
            reliableArtists = listOf(
                artist("Weeknd", "UC-homonym", subscribers = "Artist · 7 subscribers"),
                artist("The Weeknd", "UC-official", subscribers = "Artist · 274M monthly audience")
            )
        )

        assertEquals("The Weeknd", result.topTrack?.artist)
        assertEquals(listOf("UC-official"), result.topTrack?.artistBrowseIds)
        assertEquals("The Weeknd", result.songs.single().artist)
    }

    @Test
    fun unverifiedLargeChannelDoesNotBeatVerifiedExactArtist() {
        val verified = artist("HUGEL", "UC-hugel", subscribers = "Artist · 900K subscribers")
        val unverified = artist(
            "Hugel",
            "UC-copycat",
            subscribers = "Artist · 40M subscribers",
            officialArtwork = false
        )

        val result = mergeReliableArtistSearchResults(
            query = "hugel",
            exactArtist = verified,
            verifiedArtists = listOf(unverified, verified)
        )

        assertEquals("UC-hugel", result.first().browseId)
    }

    @Test
    fun realSongArtistSurvivesArticleAwareMatching() {
        val collaboration = track("Pray For Me", "The Weeknd, Kendrick Lamar")

        val result = enrichSearchTracksWithExactArtist(
            query = "weeknd",
            results = SearchResults(topTrack = collaboration, songs = listOf(collaboration)),
            reliableArtists = listOf(artist("The Weeknd", "UC-official"))
        )

        assertEquals("The Weeknd, Kendrick Lamar", result.topTrack?.artist)
    }

    @Test
    fun exactNameOutranksLouderNearMissHomonym() {
        val official = artist(
            "The Weeknd",
            "UC-official",
            subscribers = "Artist · 100M monthly audience"
        )
        val louderNearMiss = artist(
            "Weekend",
            "UC-louder",
            subscribers = "Artist · 274M monthly audience"
        )

        val result = mergeReliableArtistSearchResults(
            query = "The Weeknd",
            exactArtist = null,
            verifiedArtists = listOf(louderNearMiss, official)
        )

        assertEquals(listOf("UC-official"), result.map { it.browseId })
    }

    private fun artist(
        name: String,
        browseId: String,
        subscribers: String = "",
        officialArtwork: Boolean = true,
        thumbnailUrl: String = "https://example.com/$browseId.jpg"
    ) = ArtistHit(
        name = name,
        subscribers = subscribers,
        thumbnailUrl = thumbnailUrl,
        accentStart = 0,
        accentEnd = 0,
        browseId = browseId,
        officialArtwork = officialArtwork
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

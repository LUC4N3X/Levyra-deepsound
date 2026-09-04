package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistExclusionsTest {

    @Test
    fun emptyExclusionsKeepEverything() {
        val tracks = listOf(track("a", "Some Artist"))

        assertSame(tracks, ArtistExclusions.Empty.filterTracks(tracks))
    }

    @Test
    fun matchesByBrowseId() {
        val exclusions = ArtistExclusions.from(listOf(ExcludedArtist("UC123", "Renamed Artist", 0L)))
        val tracks = listOf(
            track("kept", "Other", browseIds = listOf("UC999")),
            track("dropped", "Whatever", browseIds = listOf("UC123"))
        )

        assertEquals(listOf("kept"), exclusions.filterTracks(tracks).map { it.id })
    }

    @Test
    fun matchesByNameIgnoringCaseAccentsAndLeadingArticle() {
        val exclusions = ArtistExclusions.from(listOf(ExcludedArtist("", "The Weeknd", 0L)))

        assertTrue(exclusions.excludesArtistName("weeknd"))
        assertTrue(exclusions.excludesArtistName("THE WEEKND"))
        assertFalse(exclusions.excludesArtistName("Weekend Warriors"))
    }

    @Test
    fun matchesCollaborationSegments() {
        val exclusions = ArtistExclusions.from(listOf(ExcludedArtist("", "Blocked", 0L)))

        assertTrue(exclusions.excludesArtistName("Allowed feat. Blocked"))
        assertTrue(exclusions.excludesArtistName("Allowed, Blocked"))
        assertTrue(exclusions.excludesArtistName("Allowed & Blocked"))
        assertFalse(exclusions.excludesArtistName("Allowed & Someone"))
    }

    @Test
    fun keepsListIdentityWhenNothingIsRemoved() {
        val exclusions = ArtistExclusions.from(listOf(ExcludedArtist("UC404", "Absent", 0L)))
        val tracks = listOf(track("a", "Present"))

        assertSame(tracks, exclusions.filterTracks(tracks))
    }

    @Test
    fun dropsEmptiedHomeSectionsAndTrimsPartialOnes() {
        val exclusions = ArtistExclusions.from(listOf(ExcludedArtist("", "Blocked", 0L)))
        val sections = listOf(
            HomeSection("only blocked", listOf(track("x", "Blocked"))),
            HomeSection("mixed", listOf(track("y", "Blocked"), track("z", "Allowed"))),
            HomeSection("clean", listOf(track("w", "Allowed")))
        )

        val result = exclusions.filterHomeSections(sections)

        assertEquals(listOf("mixed", "clean"), result.map { it.title })
        assertEquals(listOf("z"), result.first().tracks.map { it.id })
    }

    @Test
    fun filtersArtistHitsAlbumsAndReleaseRadar() {
        val exclusions = ArtistExclusions.from(listOf(ExcludedArtist("UC1", "Blocked", 0L)))

        val artists = listOf(
            ArtistHit("Blocked", "", "", 0, 0, "UC1"),
            ArtistHit("Allowed", "", "", 0, 0, "UC2")
        )
        val albums = listOf(
            AlbumHit("Album A", "Blocked", "2024", "", ""),
            AlbumHit("Album B", "Allowed", "2024", "", "")
        )
        val radar = listOf(
            ReleaseRadarEntry("Blocked", "UC1", release(), isFresh = true),
            ReleaseRadarEntry("Allowed", "UC2", release(), isFresh = true)
        )

        assertEquals(listOf("Allowed"), exclusions.filterArtistHits(artists).map { it.name })
        assertEquals(listOf("Album B"), exclusions.filterAlbumHits(albums).map { it.title })
        assertEquals(listOf("Allowed"), exclusions.filterReleaseRadar(radar).map { it.artistName })
    }

    @Test
    fun exclusionKeyPrefersBrowseIdAndFallsBackToIdentity() {
        assertEquals("UC7", excludedArtistKeyOf("UC7", "Anything"))
        assertEquals("name:some artist", excludedArtistKeyOf("", "  Sómé Artist  "))
    }

    @Test
    fun onlyArtistsWithUsableIdentityCanBeExcluded() {
        assertTrue(isExcludableArtist("UC7", ""))
        assertTrue(isExcludableArtist("", "Ok"))
        assertFalse(isExcludableArtist("", " "))
        assertFalse(isExcludableArtist("", "!"))
    }

    private fun release() = ArtistRelease(
        browseId = "MPRE1",
        title = "Release",
        subtitle = "2024",
        thumbnailUrl = "",
        year = "2024"
    )

    private fun track(id: String, artist: String, browseIds: List<String> = emptyList()) = Track(
        id = id,
        title = "Title $id",
        artist = artist,
        album = "Album",
        durationMs = 1_000L,
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
        accentEnd = 0,
        artistBrowseIds = browseIds
    )
}

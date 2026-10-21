package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchAlbumKindTest {
    private fun album(type: ReleaseType) = AlbumHit(
        title = type.name,
        artist = "Artist",
        year = "",
        thumbnailUrl = "",
        query = type.name,
        releaseType = type
    )

    @Test
    fun `kind chips appear only when albums and singles are both present`() {
        assertEquals(emptyList<SearchAlbumKind>(), availableSearchAlbumKinds(listOf(album(ReleaseType.Album))))
        assertEquals(emptyList<SearchAlbumKind>(), availableSearchAlbumKinds(listOf(album(ReleaseType.Single), album(ReleaseType.Unknown))))
        assertEquals(
            SearchAlbumKind.entries,
            availableSearchAlbumKinds(listOf(album(ReleaseType.Album), album(ReleaseType.Ep)))
        )
    }

    @Test
    fun `kinds split full releases from singles and eps`() {
        assertTrue(SearchAlbumKind.Albums.accepts(ReleaseType.Compilation))
        assertFalse(SearchAlbumKind.Albums.accepts(ReleaseType.Single))
        assertTrue(SearchAlbumKind.Singles.accepts(ReleaseType.Ep))
        assertFalse(SearchAlbumKind.Singles.accepts(ReleaseType.Unknown))
        assertTrue(SearchAlbumKind.All.accepts(ReleaseType.Unknown))
    }

    @Test
    fun `discography shelves follow content in a stable order`() {
        val discography = SearchArtistDiscography(
            artist = ArtistHit("Artist", "", "", 0, 0, "UC1"),
            singles = listOf(album(ReleaseType.Single)),
            playlists = listOf(PlaylistHit("Mix", "", "", playlistId = "PL1"))
        )

        assertEquals(listOf(SearchDiscographyShelf.Singles, SearchDiscographyShelf.Playlists), discography.shelves)
        assertTrue(discography.releases(SearchDiscographyShelf.Playlists).isEmpty())
        assertFalse(discography.isEmpty)
    }
}

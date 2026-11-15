package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumSuggestionsTest {
    private fun album(title: String, artist: String, browseId: String = "", art: String = "https://img/$title") =
        AlbumHit(title = title, artist = artist, year = "", thumbnailUrl = art, query = "$title $artist", browseId = browseId)

    @Test
    fun ordersArtistFirstAndKeepsEachReleaseOnce() {
        val current = album("Excuse The Mess", "Ella Langley", "MPRE_current")
        val shelves = buildAlbumSuggestionShelves(
            current = current,
            moreByTitle = "More by Ella Langley",
            artistReleases = listOf(current, album("Hungover", "Ella Langley", "MPRE_a"), album("Dandelion", "Ella Langley", "MPRE_b")),
            youtubeShelves = listOf(AlbumSuggestionShelf("Releases for you", listOf(album("Hungover", "Ella Langley", "MPRE_a"), album("The Middle", "Kaitlin Butts", "MPRE_c")))),
            similarTitle = "You might also like",
            similarAlbums = listOf(album("Hungover", "Ella Langley"), album("Gemini Rights", "Steve Lacy"), album("The Middle", "Kaitlin Butts"))
        )

        assertEquals(listOf("More by Ella Langley", "Releases for you", "You might also like"), shelves.map { it.title })
        assertEquals(listOf("MPRE_a", "MPRE_b"), shelves[0].albums.map { it.browseId })
        assertEquals(listOf("MPRE_c"), shelves[1].albums.map { it.browseId })
        assertEquals(listOf("Gemini Rights"), shelves[2].albums.map { it.title })
    }

    @Test
    fun collapsesSameReleaseWithDifferentBrowseIds() {
        val current = album("Other", "sangiovanni", "MPRE_other")
        val shelves = buildAlbumSuggestionShelves(
            current = current,
            moreByTitle = "More by sangiovanni",
            artistReleases = listOf(album("CADERE VOLARE", "sangiovanni", "MPRE_explicit"), album("CADERE VOLARE", "sangiovanni", "MPRE_clean")),
            youtubeShelves = emptyList(),
            similarTitle = "",
            similarAlbums = emptyList()
        )

        assertEquals(listOf("MPRE_explicit"), shelves.single().albums.map { it.browseId })
    }

    @Test
    fun untitledShelfDoesNotHideAlbumsFromLaterShelves() {
        val current = album("Other", "Artist", "MPRE_other")
        val shelves = buildAlbumSuggestionShelves(
            current = current,
            moreByTitle = "",
            artistReleases = listOf(album("Shared", "Artist", "MPRE_shared")),
            youtubeShelves = listOf(AlbumSuggestionShelf("Releases for you", listOf(album("Shared", "Artist", "MPRE_shared")))),
            similarTitle = "",
            similarAlbums = emptyList()
        )

        assertEquals(listOf("Releases for you"), shelves.map { it.title })
    }

    @Test
    fun dropsShelvesWithoutUsableAlbums() {
        val current = album("Solo", "Artist", "MPRE_solo")
        val shelves = buildAlbumSuggestionShelves(
            current = current,
            moreByTitle = "More by Artist",
            artistReleases = listOf(current, album("No Art", "Artist", "MPRE_x", art = "")),
            youtubeShelves = emptyList(),
            similarTitle = "You might also like",
            similarAlbums = listOf(album("Other", "Artist"))
        )

        assertEquals(emptyList<AlbumSuggestionShelf>(), shelves)
    }
}

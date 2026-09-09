package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.ArtistHit
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchArtistSuggestionsTest {
    @Test
    fun `artist suggestions exclude entries that cannot render`() {
        val result = selectSearchArtistSuggestions(
            listOf(
                artist(name = "", browseId = "UC-empty-name", thumbnailUrl = "https://img/1"),
                artist(name = "No Artwork", browseId = "UC-no-art", thumbnailUrl = ""),
                artist(name = "Visible", browseId = "UC-visible", thumbnailUrl = "https://img/visible")
            )
        )

        assertEquals(listOf("Visible"), result.map { it.name })
    }

    @Test
    fun `artist suggestions keep the first stable identity only`() {
        val result = selectSearchArtistSuggestions(
            listOf(
                artist(name = "First", browseId = "UC-same"),
                artist(name = "Duplicate", browseId = "UC-same"),
                artist(name = "Name Only", browseId = ""),
                artist(name = " name only ", browseId = "")
            )
        )

        assertEquals(listOf("First", "Name Only"), result.map { it.name })
    }

    @Test
    fun `artist suggestions remain bounded to seven rows`() {
        val result = selectSearchArtistSuggestions(
            (1..12).map { index -> artist(name = "Artist $index", browseId = "UC-$index") }
        )

        assertEquals(7, result.size)
        assertEquals("Artist 1", result.first().name)
        assertEquals("Artist 7", result.last().name)
    }

    private fun artist(
        name: String,
        browseId: String,
        thumbnailUrl: String = "https://img/default"
    ) = ArtistHit(
        name = name,
        subscribers = "",
        thumbnailUrl = thumbnailUrl,
        accentStart = 0,
        accentEnd = 0,
        browseId = browseId
    )
}

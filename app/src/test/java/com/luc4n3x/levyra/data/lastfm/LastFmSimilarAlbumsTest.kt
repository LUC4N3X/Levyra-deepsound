package com.luc4n3x.levyra.data.lastfm

import org.junit.Assert.assertEquals
import org.junit.Test

class LastFmSimilarAlbumsTest {
    @Test
    fun parsesSimilarArtists() {
        val body = """{"similarartists":{"artist":[{"name":"Lainey Wilson"},{"name":"lainey wilson"},{"name":"[unknown]"},{"name":"Megan Moroney"}]}}"""

        val page = LastFmGenreDiscoveryParser.parseSimilarArtists(body)

        assertEquals(LastFmTagPage.Found(listOf("Lainey Wilson", "Megan Moroney")), page)
    }

    @Test
    fun parsesArtistTopAlbumsContainer() {
        val body = """{"topalbums":{"album":[{"name":"Bell Bottom Country","artist":{"name":"Lainey Wilson"},"image":[{"size":"large","#text":"https://lastfm.freetls.fastly.net/i/u/174s/a.jpg"},{"size":"extralarge","#text":"https://lastfm.freetls.fastly.net/i/u/300x300/a.jpg"}]},{"name":"No Image","artist":{"name":"Lainey Wilson"},"image":[]}]}}"""

        val page = LastFmGenreDiscoveryParser.parseTopAlbums(body, "topalbums")

        assertEquals(
            LastFmTagPage.Found(listOf(LastFmDiscoveryAlbum("Bell Bottom Country", "Lainey Wilson", "https://lastfm.freetls.fastly.net/i/u/300x300/a.jpg"))),
            page
        )
    }

    @Test
    fun interleavesOneAlbumPerArtistBeforeSecondPicks() {
        val a1 = LastFmDiscoveryAlbum("A1", "A", "https://a1")
        val a2 = LastFmDiscoveryAlbum("A2", "A", "https://a2")
        val b1 = LastFmDiscoveryAlbum("B1", "B", "https://b1")

        assertEquals(listOf(a1, b1, a2), interleaveSimilarAlbums(listOf(listOf(a1, a2), listOf(b1, a1)), limit = 10))
        assertEquals(listOf(a1, b1), interleaveSimilarAlbums(listOf(listOf(a1, a2), listOf(b1)), limit = 2))
    }
}

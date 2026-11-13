package com.luc4n3x.levyra.data.apple

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleExploreGenreDiscoveryTest {

    @Test
    fun everyLevyraZoneMapsToApplePublishedCurators() {
        val zones = listOf(
            "rap-drill", "elettronica", "pop-global", "rnb-soul", "rock-alt", "latino", "lofi-chill", "anime-jpop", "afrobeats",
            "chart-kpop", "chart-metal", "chart-indie", "chart-jazz"
        )
        zones.forEach { zone ->
            val ids = AppleExploreCatalog.zoneCuratorIds(zone)
            assertTrue(zone, ids.isNotEmpty())
            assertTrue(zone, ids.all { id -> id.isNotEmpty() && id.all(Char::isDigit) })
            assertEquals(zone, ids.distinct(), ids)
        }
        assertEquals("989061185", AppleExploreCatalog.zoneCuratorIds("rap-drill").first())
        assertTrue(AppleExploreCatalog.zoneCuratorIds("unknown").isEmpty())
    }

    @Test
    fun genreShelvesKeepRealContentInAppleOrder() {
        val hero = shelf("hero", "", AppleCatalogShelfKind.Hero, item("h1", AppleCatalogItemKind.Playlist))
        val header = shelf("header", "", AppleCatalogShelfKind.Header, item("x", AppleCatalogItemKind.Other))
        val newReleases = shelf("new", "Nuove uscite", AppleCatalogShelfKind.Grid, item("a1", AppleCatalogItemKind.Album))
        val untitled = shelf("untitled", "", AppleCatalogShelfKind.Grid, item("a2", AppleCatalogItemKind.Album))
        val radio = shelf("radio", "Stazioni radio", AppleCatalogShelfKind.Grid, item("r1", AppleCatalogItemKind.Other))
        val mixed = shelf(
            "videos", "Video musicali", AppleCatalogShelfKind.Videos,
            item("v1", AppleCatalogItemKind.Video), item("r2", AppleCatalogItemKind.Other)
        )
        val footer = shelf("footer", "", AppleCatalogShelfKind.Footer, item("f", AppleCatalogItemKind.Other))
        val page = AppleCatalogPage("it", listOf(header, hero, newReleases, untitled, radio, mixed, footer), 0L)

        val shelves = AppleExploreCatalog.genreShelves(page)

        assertEquals(listOf("hero", "new", "videos"), shelves.map { it.id })
        assertEquals(listOf("v1"), shelves.last().items.map { it.id })
        assertTrue(AppleExploreCatalog.genreShelves(null).isEmpty())
    }

    @Test
    fun genreShelvesAreBounded() {
        val many = (0 until 40).map { index ->
            shelf("s$index", "Shelf $index", AppleCatalogShelfKind.Grid, *(0 until 30).map { item("$index-$it", AppleCatalogItemKind.Album) }.toTypedArray())
        }
        val shelves = AppleExploreCatalog.genreShelves(AppleCatalogPage("us", many, 0L))
        assertEquals(24, shelves.size)
        assertTrue(shelves.all { it.items.size == 20 })
    }

    @Test
    fun genreShelvesDropAppleBranding() {
        val playlist = AppleCatalogItem(
            id = "pl.1", kind = AppleCatalogItemKind.Playlist, title = "Rap Life", subtitle = "Apple Music Hip-Hop/Rap",
            artworkTemplate = "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/a/b.png/{w}x{h}SC.DN02.{f}?l=it-IT"
        )
        val album = AppleCatalogItem(
            id = "1", kind = AppleCatalogItemKind.Album, title = "ICEMAN", subtitle = "Drake",
            artworkTemplate = "https://is1-ssl.mzstatic.com/image/thumb/Music/v4/c/d.jpg/{w}x{h}bb.{f}"
        )
        val page = AppleCatalogPage(
            "it",
            listOf(
                shelf("playlists", "Playlist", AppleCatalogShelfKind.Grid, playlist, album),
                shelf("radio", "Programmi radio Apple Music", AppleCatalogShelfKind.Grid, item("p2", AppleCatalogItemKind.Playlist))
            ),
            0L
        )

        val shelves = AppleExploreCatalog.genreShelves(page)

        assertEquals(listOf("playlists"), shelves.map { it.id })
        val (plain, kept) = shelves.single().items
        assertEquals("", plain.subtitle)
        assertEquals("https://is1-ssl.mzstatic.com/image/thumb/Features/v4/a/b.png/{w}x{h}cc.{f}?l=it-IT", plain.artworkTemplate)
        assertEquals(album, kept)
    }

    private fun item(id: String, kind: AppleCatalogItemKind) = AppleCatalogItem(id = id, kind = kind, title = "Item $id", subtitle = "Artist")

    private fun shelf(id: String, title: String, kind: AppleCatalogShelfKind, vararg items: AppleCatalogItem) =
        AppleCatalogShelf(id = id, title = title, kind = kind, items = items.toList())
}

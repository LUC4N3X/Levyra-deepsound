package com.luc4n3x.levyra.data.apple

import com.luc4n3x.levyra.viewmodel.appleGenreKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleStorefrontCatalogTest {

    private fun album(id: String, title: String, artist: String = "Artist") = AppleCatalogItem(
        id = id,
        kind = AppleCatalogItemKind.Album,
        title = title,
        subtitle = artist,
        coverArtworkTemplate = "https://is1-ssl.mzstatic.com/image/thumb/$id/{w}x{h}{c}.{f}",
        url = "https://music.apple.com/it/album/slug/$id"
    )

    private fun item(id: String, kind: AppleCatalogItemKind) = AppleCatalogItem(
        id = id,
        kind = kind,
        title = "Item $id",
        subtitle = "Curator",
        url = "https://music.apple.com/it/playlist/slug/pl.$id"
    )

    private fun shelf(kind: AppleCatalogShelfKind, vararg items: AppleCatalogItem) =
        AppleCatalogShelf(id = "shelf-${items.firstOrNull()?.id}", title = "", kind = kind, items = items.toList())

    private fun page(vararg shelves: AppleCatalogShelf) = AppleCatalogPage("it", shelves.toList(), 0L)

    @Test
    fun newReleasesComeOnlyFromTheAlbumGridsThatFollowTheNewSongsShelf() {
        val storefront = page(
            shelf(AppleCatalogShelfKind.Hero, album("9000001", "Anniversary Reissue")),
            shelf(AppleCatalogShelfKind.Songs, item("s1", AppleCatalogItemKind.Song)),
            shelf(AppleCatalogShelfKind.Grid, album("1000001", "New This Week"), album("1000002", "Second")),
            shelf(AppleCatalogShelfKind.Grid, album("1000003", "Recent Release"), album("1000001", "New This Week")),
            shelf(AppleCatalogShelfKind.Grid, item("p1", AppleCatalogItemKind.Playlist)),
            shelf(AppleCatalogShelfKind.Grid, album("9000002", "Coming Soon"))
        )

        val releases = appleNewReleaseAlbums(storefront, 48)

        assertEquals(listOf("New This Week", "Second", "Recent Release"), releases.map { it.title })
        assertTrue(releases.all { it.canonicalUrl.startsWith("https://music.apple.com/it/album/") })
        assertTrue(releases.all { it.metadataProvider == AppleCatalogTracks.SOURCE })
    }

    @Test
    fun storefrontWithoutAlbumShelvesYieldsNoReleases() {
        val playlistsOnly = page(
            shelf(AppleCatalogShelfKind.Hero, item("h1", AppleCatalogItemKind.Other)),
            shelf(AppleCatalogShelfKind.Grid, item("p1", AppleCatalogItemKind.Playlist)),
            shelf(AppleCatalogShelfKind.Grid, album("1000001", "Not A Release Shelf"))
        )

        assertTrue(appleNewReleaseAlbums(playlistsOnly, 48).isEmpty())
        assertTrue(appleNewReleaseAlbums(null, 48).isEmpty())
    }

    @Test
    fun releasesAreCappedAtTheTwoNewReleaseShelvesAndTheLimit() {
        val storefront = page(
            shelf(AppleCatalogShelfKind.Songs, item("s1", AppleCatalogItemKind.Song)),
            shelf(AppleCatalogShelfKind.Grid, album("1000001", "A"), album("1000002", "B")),
            shelf(AppleCatalogShelfKind.Grid, album("1000003", "C")),
            shelf(AppleCatalogShelfKind.Grid, album("1000004", "Third Album Shelf"))
        )

        assertEquals(listOf("A", "B", "C"), appleNewReleaseAlbums(storefront, 48).map { it.title })
        assertEquals(listOf("A"), appleNewReleaseAlbums(storefront, 1).map { it.title })
    }

    private fun chartAlbum(id: String, name: String, artist: String, releaseDate: String, url: String = "https://music.apple.com/ru/album/slug/$id") =
        """{"id":"$id","type":"albums","attributes":{"name":"$name","artistName":"$artist","releaseDate":"$releaseDate","url":"$url","contentRating":"explicit","artwork":{"url":"https://is1-ssl.mzstatic.com/image/thumb/$id/{w}x{h}bb.jpg"}}}"""

    @Test
    fun storefrontChartsYieldOnlyRecentlyReleasedAlbums() {
        val json = org.json.JSONObject(
            """{"results":{"albums":[{"chart":"most-played","name":"Top Albums","data":[
                ${chartAlbum("2000001", "Fresh Album", "Баста", "2026-10-16")},
                ${chartAlbum("2000002", "Old Classic", "Кино", "1988-01-01")},
                ${chartAlbum("2000003", "Preorder", "Zivert", "2026-12-01")},
                ${chartAlbum("2000004", "Broken Link", "Artist", "2026-10-10", url = "https://example.com/album/2000004")},
                ${chartAlbum("2000001", "Fresh Album", "Баста", "2026-10-16")},
                ${chartAlbum("2000005", "Last Month", "MACAN", "2026-09-01")}
            ]}]}}"""
        )
        val now = java.time.LocalDate.parse("2026-10-24").toEpochDay() * 24L * 60L * 60L * 1000L

        val albums = appleChartReleaseAlbums(json, now, 48)

        assertEquals(listOf("Fresh Album", "Last Month"), albums.map { it.title })
        assertEquals("Баста", albums.first().artist)
        assertEquals("2026", albums.first().year)
        assertEquals("2026-10-16", albums.first().releaseDate)
        assertTrue(albums.first().explicit)
        assertEquals("https://is1-ssl.mzstatic.com/image/thumb/2000001/600x600bb.jpg", albums.first().thumbnailUrl)
        assertTrue(appleChartReleaseAlbums(null, now, 48).isEmpty())
    }

    @Test
    fun youTubeMusicCategoryParamsMapToGlobalCuratorIdsIndependentlyOfLanguage() {
        assertEquals("1558257238", AppleExploreCatalog.curatorId("ggMPOg1uX044Z2o5WERLckpU"))
        assertEquals("976439532", AppleExploreCatalog.curatorId(" ggMPOg1uX1N4VmduTmdUR3dm "))
        assertNull(AppleExploreCatalog.curatorId("ggMPOg1uX1V4cTVVT0JjY0xI"))
        assertNull(AppleExploreCatalog.curatorId(""))
    }

    @Test
    fun genreKeyUsesStableParamsInsteadOfTheLocalizedTitle() {
        val russian = appleGenreKey("RU", "ru", "В дороге", "ggMPOg1uX044Z2o5WERLckpU")
        val italian = appleGenreKey("ru", "ru", "Viaggi", "ggMPOg1uX044Z2o5WERLckpU")

        assertEquals(russian, italian)
        assertEquals(appleGenreKey("us", "en", "Commute", ""), appleGenreKey("us", "en", " Commute ", " "))
    }
}

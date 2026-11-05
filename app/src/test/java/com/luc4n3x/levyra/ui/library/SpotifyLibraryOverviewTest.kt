package com.luc4n3x.levyra.ui.library

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.ui.i18n.spotifyLibraryCopy
import com.luc4n3x.levyra.ui.i18n.spotifyLibraryLocalizationCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyLibraryOverviewTest {
    private fun item(id: String, kind: SpotifyLibraryKind, title: String, creator: String = "", played: Long = 0L, added: Long = 0L, downloaded: Boolean = false) =
        SpotifyLibraryItem(id, kind, title, creator, "", 1, played, added, downloaded)

    private val items = listOf(
        item("b", SpotifyLibraryKind.Playlist, "Beta", creator = "Zed", played = 10, added = 300),
        item("liked", SpotifyLibraryKind.Liked, "Liked"),
        item("a", SpotifyLibraryKind.Album, "Alpha", creator = "Amy", played = 30, added = 100, downloaded = true),
        item("c", SpotifyLibraryKind.Artist, "Gamma", creator = "Gamma", played = 20, added = 200)
    )

    @Test
    fun pinnedStayFirstAndEachSortOrdersTheRest() {
        assertEquals(listOf("liked", "a", "c", "b"), spotifyLibraryArrange(items, null, SpotifyLibrarySort.Recents).map { it.id })
        assertEquals(listOf("liked", "b", "c", "a"), spotifyLibraryArrange(items, null, SpotifyLibrarySort.RecentlyAdded).map { it.id })
        assertEquals(listOf("liked", "a", "b", "c"), spotifyLibraryArrange(items, null, SpotifyLibrarySort.Alphabetical).map { it.id })
        assertEquals(listOf("liked", "a", "c", "b"), spotifyLibraryArrange(items, null, SpotifyLibrarySort.Creator).map { it.id })
    }

    @Test
    fun filtersKeepOnlyTheirKind() {
        assertEquals(listOf("liked", "b"), spotifyLibraryArrange(items, SpotifyLibraryFilter.Playlists, SpotifyLibrarySort.Recents).map { it.id })
        assertEquals(listOf("c"), spotifyLibraryArrange(items, SpotifyLibraryFilter.Artists, SpotifyLibrarySort.Recents).map { it.id })
        assertEquals(listOf("a"), spotifyLibraryArrange(items, SpotifyLibraryFilter.Albums, SpotifyLibrarySort.Recents).map { it.id })
        assertEquals(listOf("a"), spotifyLibraryArrange(items, SpotifyLibraryFilter.Downloaded, SpotifyLibrarySort.Recents).map { it.id })
    }

    @Test
    fun everySelectableLanguageShipsLibraryCopy() {
        val selectable = LevyraLanguageCatalog.selectableLanguages.map { it.code }.toSet()
        assertEquals(selectable, spotifyLibraryLocalizationCodes())
        selectable.forEach { code ->
            val copy = spotifyLibraryCopy(code)
            assertTrue(code, listOf(copy.sortBy, copy.recents, copy.recentlyAdded, copy.alphabetical, copy.creator, copy.playlist, copy.gridView, copy.listView).all(String::isNotBlank))
        }
        assertEquals("Ordina per", spotifyLibraryCopy("it").sortBy)
        assertEquals(selectable, com.luc4n3x.levyra.ui.i18n.spotifyNewReleasesLocalizationCodes())
        assertEquals("Nuove uscite per te", com.luc4n3x.levyra.ui.i18n.spotifyNewReleasesForYou("it"))
    }
}

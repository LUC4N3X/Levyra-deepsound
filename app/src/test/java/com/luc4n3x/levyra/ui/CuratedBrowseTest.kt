package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.data.YoutubeMusicDecades
import com.luc4n3x.levyra.data.YoutubeMusicMoodPlaylist
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItem
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItemKind
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelf
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelfStyle
import com.luc4n3x.levyra.ui.i18n.curatedCollectionsCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CuratedBrowseTest {
    private fun mood(name: String) = ExploreCategory(name, "m-$name", section = "Moods & moments", sectionIndex = 0)
    private fun genre(name: String, params: String = "g-$name") = ExploreCategory(name, params, section = "Genres", sectionIndex = 1)
    private fun decade(name: String) = ExploreCategory(name, "d-$name", section = "Decades", sectionIndex = -1)
    private fun playlist(id: String) = YoutubeMusicMoodPlaylist("T$id", id, "VL$id", "desc", "https://img/$id")

    @Test
    fun groupsFollowProviderSectionsThenDecades() {
        val groups = curatedBrowseGroups(
            listOf(genre("Jazz"), mood("Chill"), genre("Rock"), mood("Focus")),
            listOf(decade("1980s"), decade("1990s")),
            "Decades"
        )

        assertEquals(listOf("Moods & moments", "Genres", "Decades"), groups.map { it.label })
        assertEquals(listOf("Chill", "Focus"), groups[0].categories.map { it.title })
        assertEquals(listOf("Jazz", "Rock"), groups[1].categories.map { it.title })
        assertEquals(listOf("1980s", "1990s"), groups[2].categories.map { it.title })
    }

    @Test
    fun decadesGenreButtonIsNotRepeatedAsACategory() {
        val groups = curatedBrowseGroups(listOf(genre("Jazz"), genre("Decades", YoutubeMusicDecades.PARAMS)), listOf(decade("1980s")), "Decades")

        assertEquals(listOf("Jazz"), groups[0].categories.map { it.title })
    }

    @Test
    fun selectionFindsTheChosenCategoryOrFallsBackToTheFirst() {
        val groups = curatedBrowseGroups(listOf(mood("Chill"), genre("Jazz")), listOf(decade("1980s")), "Decades")

        assertEquals(2 to "1980s", curatedBrowseSelection(groups, "d-1980s")?.let { it.first to it.second.title })
        assertEquals(0 to "Chill", curatedBrowseSelection(groups, "missing")?.let { it.first to it.second.title })
        assertNull(curatedBrowseSelection(emptyList(), ""))
    }

    @Test
    fun shelfShowsOnlyPlaylistsCappedAtTwelve() {
        val album = YoutubeMusicMoodPlaylist("Album", "", "MPREb_x", "", "")
        val shelf = curatedBrowseDistinct(curatedBrowseShelf(curatedCollectionsCopy("en"), genre("Jazz"), listOf(album) + (1..20).map { playlist("p$it") }), emptyList())

        assertEquals(CuratedBrowseShelfKey, shelf.key)
        assertEquals("More playlists", shelf.title)
        assertEquals((1..12).map { "p$it" }, shelf.items.map { it.playlist?.playlistId })
    }

    @Test
    fun playlistsAlreadyOnThePageAreNotRepeated() {
        val full = curatedBrowseShelf(curatedCollectionsCopy("en"), mood("Chill"), (1..14).map { playlist("p$it") })
        val everyday = full.copy(key = "collection-everyday", items = full.items.take(2))

        val shelf = curatedBrowseDistinct(full, listOf(everyday))

        assertEquals((3..14).map { "p$it" }, shelf.items.map { it.playlist?.playlistId })
    }

    @Test
    fun pageCountsAndCoversIgnoreTheBrowseShelf() {
        fun item(id: String) = YoutubeMusicHomeItem("k$id", YoutubeMusicHomeItemKind.Playlist, "T$id", "", "https://img/$id")
        val curated = YoutubeMusicHomeShelf("mixed", "Mixed", "", "", YoutubeMusicHomeShelfStyle.Cards, listOf(item("a")))
        val browse = YoutubeMusicHomeShelf(CuratedBrowseShelfKey, "More", "", "", YoutubeMusicHomeShelfStyle.Cards, listOf(item("b"), item("c")))
        val page = CuratedPageData("Title", listOf(curated, browse), loading = false)

        assertEquals(1, page.total)
        assertEquals(listOf("https://img/a"), page.covers)
    }

    @Test
    fun decadeTilesSitBetweenMoodsAndGenres() {
        val result = withDecadesAfterMoods(listOf(mood("Chill"), mood("Focus"), genre("Jazz"), genre("Rock")), listOf(decade("1980s")))

        assertEquals(listOf("Chill", "Focus", "1980s", "Jazz", "Rock"), result.map { it.title })
        assertEquals(emptyList<ExploreCategory>(), withDecadesAfterMoods(emptyList(), listOf(decade("1980s"))))
    }
}

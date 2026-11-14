package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.YoutubeMusicMoodPlaylist
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItem
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItemKind
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelf
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelfStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CuratedMoodShelvesTest {
    private fun mood(name: String) = ExploreCategory(name, "p-$name", section = "Moods", sectionIndex = 0)
    private fun genre(name: String) = ExploreCategory(name, "p-$name", section = "Genres", sectionIndex = 1)

    private fun playlist(id: String) = YoutubeMusicMoodPlaylist("T$id", id, "VL$id", "desc", "https://img/$id")

    private fun item(id: String) = YoutubeMusicHomeItem(
        key = "home-$id",
        kind = YoutubeMusicHomeItemKind.Playlist,
        title = "T$id",
        subtitle = "",
        thumbnailUrl = "",
        playlist = PlaylistHit("T$id", "", "", id)
    )

    private fun shelf(key: String, ids: List<String>) =
        YoutubeMusicHomeShelf(key, key, "", "", YoutubeMusicHomeShelfStyle.Cards, ids.map(::item))

    @Test
    fun alternatesMoodsAndGenresInProviderOrderCappedAtSix() {
        val categories = listOf(mood("Chill"), mood("Focus"), mood("Party"), mood("Sleep")) +
            listOf(genre("Jazz"), genre("Pop"), genre("Rock"), genre("Soul"))
        assertEquals(
            listOf("Chill", "Jazz", "Focus", "Pop", "Party", "Rock"),
            curatedMoodCategories(categories).map { it.title }
        )
    }

    @Test
    fun fillsFromLongerGroupWhenOtherIsShort() {
        val categories = listOf(genre("Jazz"), genre("Pop"), genre("Rock"), genre("Soul"), genre("Folk"), genre("Metal"), genre("Blues"), mood("Chill"))
        assertEquals(
            listOf("Chill", "Jazz", "Pop", "Rock", "Soul", "Folk"),
            curatedMoodCategories(categories).map { it.title }
        )
    }

    @Test
    fun noCategoriesSelectsNothing() {
        assertEquals(emptyList<ExploreCategory>(), curatedMoodCategories(emptyList()))
    }

    @Test
    fun categoryShelfKeepsOnlyPlaylistsAndUsesProviderTitle() {
        val album = YoutubeMusicMoodPlaylist("Album", "", "MPREb_x", "", "")
        val result = curatedMoodShelf(genre("Jazz"), listOf(album, playlist("a")))!!
        assertEquals("Jazz", result.title)
        assertEquals(listOf("a"), result.items.map { it.playlist?.playlistId })
        assertNull(curatedMoodShelf(genre("Jazz"), listOf(album)))
    }

    @Test
    fun appendsAfterCuratedDedupesAcrossAllShelvesAndCapsAtTen() {
        val curated = listOf(shelf("mixed", listOf("a", "b")))
        val jazz = curatedMoodShelf(genre("Jazz"), listOf("a", "c", "d").map(::playlist))!!
        val pop = curatedMoodShelf(genre("Pop"), (listOf("c", "b") + (1..15).map { "p$it" }).map(::playlist))!!
        val empty = curatedMoodShelf(genre("Rock"), listOf("a", "d").map(::playlist))!!

        val result = withCuratedMoodShelves(curated, listOf(jazz, pop, empty))

        assertEquals(listOf("mixed", jazz.key, pop.key), result.map { it.key })
        assertEquals(curated.first(), result.first())
        assertEquals(listOf("c", "d"), result[1].items.map { it.playlist?.playlistId })
        assertEquals((1..10).map { "p$it" }, result[2].items.map { it.playlist?.playlistId })
    }

    @Test
    fun noMoodShelvesLeavesCuratedUntouched() {
        val curated = listOf(shelf("mixed", listOf("a")))
        assertEquals(curated, withCuratedMoodShelves(curated, emptyList()))
    }
}

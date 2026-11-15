package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.YoutubeMusicMoodPlaylist
import com.luc4n3x.levyra.domain.ExploreCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CuratedMoodShelvesTest {
    private fun genre(name: String) = ExploreCategory(name, "p-$name", section = "Genres", sectionIndex = 1)

    private fun playlist(id: String) = YoutubeMusicMoodPlaylist("T$id", id, "VL$id", "desc", "https://img/$id")

    @Test
    fun categoryShelfKeepsOnlyPlaylistsAndUsesProviderTitle() {
        val album = YoutubeMusicMoodPlaylist("Album", "", "MPREb_x", "", "")
        val result = curatedMoodShelf(genre("Jazz"), listOf(album, playlist("a")))!!
        assertEquals("Jazz", result.title)
        assertEquals(listOf("a"), result.items.map { it.playlist?.playlistId })
        assertNull(curatedMoodShelf(genre("Jazz"), listOf(album)))
    }
}

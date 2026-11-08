package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItem
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItemKind
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelf
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelfStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class FeaturedCuratedPlaylistTest {
    private fun item(id: String) = YoutubeMusicHomeItem(
        key = id, kind = YoutubeMusicHomeItemKind.Playlist, title = id, subtitle = "", thumbnailUrl = "",
        playlist = PlaylistHit(title = id, author = "", thumbnailUrl = "", playlistId = id)
    )

    private fun shelf(vararg ids: String) =
        YoutubeMusicHomeShelf("s", "Shelf", "", "", YoutubeMusicHomeShelfStyle.Cards, ids.map(::item))

    @Test
    fun insertsFeaturedPlaylistInSecondPosition() {
        val result = withFeaturedCuratedPlaylist(listOf(shelf("a", "b", "c"), shelf("d")), item("f"))
        assertEquals(listOf("a", "f", "b", "c"), result.first().items.map { it.key })
        assertEquals(listOf("d"), result[1].items.map { it.key })
    }

    @Test
    fun skipsDuplicatesAndMissingData() {
        val shelves = listOf(shelf("a", "f"))
        assertEquals(shelves, withFeaturedCuratedPlaylist(shelves, item("f")))
        assertEquals(shelves, withFeaturedCuratedPlaylist(shelves, null))
        assertEquals(emptyList<YoutubeMusicHomeShelf>(), withFeaturedCuratedPlaylist(emptyList(), item("f")))
    }
}

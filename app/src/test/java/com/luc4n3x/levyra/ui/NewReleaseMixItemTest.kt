package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItem
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItemKind
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelf
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelfStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NewReleaseMixItemTest {

    private fun item(key: String, kind: YoutubeMusicHomeItemKind, playlist: PlaylistHit? = null) =
        YoutubeMusicHomeItem(key = key, kind = kind, title = key, subtitle = "", thumbnailUrl = "https://i.ytimg.com/$key.jpg", playlist = playlist)

    private fun shelf(browseId: String, items: List<YoutubeMusicHomeItem>) =
        YoutubeMusicHomeShelf(key = browseId, title = browseId, strapline = "", thumbnailUrl = "", style = YoutubeMusicHomeShelfStyle.Cards, items = items, headerBrowseId = browseId)

    @Test
    fun picksThePlaylistFromTheNewReleasesShelf() {
        val mix = item("mix", YoutubeMusicHomeItemKind.Playlist, PlaylistHit("New Release Mix", "", "", playlistId = "RDTMAKmix"))
        val shelves = listOf(
            shelf("FEmusic_charts", listOf(item("chart", YoutubeMusicHomeItemKind.Playlist, PlaylistHit("Chart", "", "", playlistId = "PLchart")))),
            shelf("FEmusic_new_releases", listOf(item("album", YoutubeMusicHomeItemKind.Album), mix))
        )

        assertEquals("RDTMAKmix", newReleaseMixItem(shelves)?.playlist?.playlistId)
    }

    @Test
    fun returnsNothingWithoutAReleasePlaylist() {
        assertNull(newReleaseMixItem(listOf(shelf("FEmusic_new_releases", listOf(item("album", YoutubeMusicHomeItemKind.Album))))))
    }
}

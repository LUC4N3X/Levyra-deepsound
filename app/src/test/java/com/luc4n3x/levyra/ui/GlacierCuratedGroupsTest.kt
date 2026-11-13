package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItem
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItemKind
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelf
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelfStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class GlacierCuratedGroupsTest {
    private fun item(key: String, playlistId: String?) = YoutubeMusicHomeItem(
        key = key,
        kind = YoutubeMusicHomeItemKind.Playlist,
        title = key,
        subtitle = "",
        thumbnailUrl = "",
        playlist = playlistId?.let { PlaylistHit(title = key, author = "", thumbnailUrl = "", playlistId = it) }
    )

    private fun shelf(key: String, title: String, vararg items: YoutubeMusicHomeItem) =
        YoutubeMusicHomeShelf(key, title, "strap-$key", "", YoutubeMusicHomeShelfStyle.Cards, items.toList())

    @Test
    fun curatedShelvesComeFirstThenCollectionsInOrder() {
        val groups = glacierCuratedGroups(
            curated = listOf(shelf("mixed", "Mixed for you", item("a", "PL1")), shelf("moods", "Moods", item("b", "PL2"))),
            collections = listOf(shelf("collection-everyday", "For every moment", item("c", "PL3")), shelf("collection-decades", "Decades", item("d", "PL4"))),
            leadTitle = "Hand-picked"
        )
        assertEquals(listOf("mixed", "moods", "collection-everyday", "collection-decades"), groups.map { it.key })
        assertEquals("Hand-picked", groups.first().title)
        assertEquals("", groups.first().strapline)
        assertEquals("Moods", groups[1].title)
    }

    @Test
    fun duplicatePlaylistsAppearOnlyInTheirFirstGroup() {
        val groups = glacierCuratedGroups(
            curated = listOf(shelf("mixed", "Mixed", item("a", "PL1"), item("b", "PL2"))),
            collections = listOf(shelf("collection-decades", "Decades", item("c", "PL2"), item("d", "PL3"))),
            leadTitle = "Lead"
        )
        assertEquals(listOf("PL1", "PL2"), groups[0].items.map { it.playlist?.playlistId })
        assertEquals(listOf("PL3"), groups[1].items.map { it.playlist?.playlistId })
    }

    @Test
    fun itemsWithoutPlaylistAndEmptyGroupsAreDropped() {
        val groups = glacierCuratedGroups(
            curated = listOf(shelf("mixed", "Mixed", item("a", null), item("b", ""))),
            collections = listOf(shelf("collection-decades", "Decades", item("c", "PL9"))),
            leadTitle = "Lead"
        )
        assertEquals(listOf("collection-decades"), groups.map { it.key })
        assertEquals("Decades", groups.single().title)
    }

    @Test
    fun noShelvesGiveNoGroups() {
        assertEquals(emptyList<YoutubeMusicHomeShelf>(), glacierCuratedGroups(emptyList(), emptyList(), "Lead"))
    }
}

package com.luc4n3x.levyra.ui

import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItem
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItemKind
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelf
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelfStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CuratedPlaylistsPageTest {
    private fun item(key: String, thumbnail: String = "") = YoutubeMusicHomeItem(
        key = key,
        kind = YoutubeMusicHomeItemKind.Playlist,
        title = key,
        subtitle = "",
        thumbnailUrl = thumbnail,
        playlist = PlaylistHit(title = key, author = "", thumbnailUrl = thumbnail, playlistId = "PL-$key")
    )

    private fun shelf(key: String, vararg items: YoutubeMusicHomeItem) =
        YoutubeMusicHomeShelf(key, key, "", "", YoutubeMusicHomeShelfStyle.Cards, items.toList())

    @Test
    fun rowsChunkEachGroupBehindItsOwnHeader() {
        val rows = curatedPageRows(listOf(shelf("a", item("1"), item("2"), item("3")), shelf("b", item("4"))), columns = 2)
        assertEquals(
            listOf("curated-page-head-a", "curated-page-row-a-0-2", "curated-page-row-a-1-2", "curated-page-head-b", "curated-page-row-b-0-2"),
            rows.map { it.key }
        )
        assertEquals(listOf(0, 0, 0, 1, 1), rows.map { it.group })
        val second = rows[2] as CuratedPageRow.Cells
        assertEquals(2, second.offset)
        assertEquals(listOf("3"), second.items.map { it.key })
    }

    @Test
    fun rowKeysAreUniqueAndChangeWithColumnCount() {
        val groups = listOf(shelf("a", item("1"), item("2"), item("3")))
        val two = curatedPageRows(groups, 2).map { it.key }
        val three = curatedPageRows(groups, 3).map { it.key }
        assertEquals(two.size, two.toSet().size)
        assertTrue(two.drop(1).none { it in three })
    }

    @Test
    fun headerIndexPointsAtGroupHeaderOrMinusOne() {
        val rows = curatedPageRows(listOf(shelf("a", item("1"), item("2"), item("3")), shelf("b", item("4"))), 2)
        assertEquals(0, curatedPageHeaderIndex(rows, 0))
        assertEquals(3, curatedPageHeaderIndex(rows, 1))
        assertEquals(-1, curatedPageHeaderIndex(rows, 5))
    }

    @Test
    fun columnsFollowWidthWithinBounds() {
        assertEquals(2, curatedPageColumns(360.dp, 16.dp, 12.dp, 148.dp))
        assertEquals(4, curatedPageColumns(700.dp, 16.dp, 12.dp, 148.dp))
        assertEquals(2, curatedPageColumns(200.dp, 16.dp, 12.dp, 148.dp))
        assertEquals(6, curatedPageColumns(2000.dp, 16.dp, 12.dp, 148.dp))
    }

    @Test
    fun focusedGroupIsFirstRowNotHiddenUnderThePinnedBar() {
        val groupOf = mapOf<Any, Int>("head-a" to 0, "row-a" to 0, "head-b" to 1, "row-b" to 1)
        val visible = listOf(
            Triple<Any, Int, Int>("sticky", 0, 56),
            Triple<Any, Int, Int>("row-a", -100, 150),
            Triple<Any, Int, Int>("head-b", 50, 60),
            Triple<Any, Int, Int>("row-b", 110, 200)
        )
        assertEquals(1, curatedFocusedGroup(visible, threshold = 56, groupOf = groupOf))
        assertEquals(0, curatedFocusedGroup(visible, threshold = 40, groupOf = groupOf))
        assertEquals(0, curatedFocusedGroup(emptyList(), threshold = 56, groupOf = groupOf))
    }

    @Test
    fun pageDataCountsPlaylistsAndCollectsDistinctCovers() {
        val page = CuratedPageData(
            "Curated",
            listOf(shelf("a", item("1", "x"), item("2", "x"), item("3", "")), shelf("b", item("4", "y"))),
            loading = false
        )
        assertEquals(4, page.total)
        assertEquals(listOf("x", "y"), page.covers)
        assertFalse(page.empty)
        assertTrue(CuratedPageData("Curated", emptyList(), loading = false).empty)
        assertFalse(CuratedPageData("Curated", emptyList(), loading = true).empty)
    }
}

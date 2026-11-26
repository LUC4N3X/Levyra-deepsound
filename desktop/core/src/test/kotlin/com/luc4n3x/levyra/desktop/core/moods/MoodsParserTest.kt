package com.luc4n3x.levyra.desktop.core.moods

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class MoodsParserTest {
    private fun button(title: String, params: String) =
        """{"musicNavigationButtonRenderer":{"buttonText":{"runs":[{"text":"$title"}]},"clickCommand":{"browseEndpoint":{"browseId":"FEmusic_moods_and_genres_category","params":"$params"}}}}"""

    @Test
    fun categoriesKeepGridSections() {
        val root = JSONObject(
            """{"contents":[{"gridRenderer":{"items":[${button("Relax", "m1")},${button("Party", "m2")}]}},""" +
                """{"gridRenderer":{"items":[${button("Rock", "g1")},${button("Relax", "m1")}]}}]}"""
        )
        val categories = MoodsParser.categories(root)
        assertEquals(listOf("m1" to 0, "m2" to 0, "g1" to 1), categories.map { it.params to it.sectionIndex })
    }

    @Test
    fun browseOrderPutsDecadesAfterMoods() {
        val ordered = moodBrowseOrder(
            listOf(MoodCategory("Rock", "g1", 1), MoodCategory("Relax", "m1", 0), MoodCategory("", "x", 0)),
            listOf(MoodCategory("'80s", "d80", -1), MoodCategory("Dup", "m1", -1))
        )
        assertEquals(listOf("m1", "d80", "g1"), ordered.map { it.params })
    }

    @Test
    fun mixedFeedKeepsEveryPlaylistAndRecognizesFeaturedSections() {
        val playlists = (1..28).joinToString(",") { index ->
            """{"musicTwoRowItemRenderer":{"title":{"runs":[{"text":"Playlist $index"}]},"navigationEndpoint":{"browseEndpoint":{"browseId":"VLPL$index"}}}}"""
        }
        val root = JSONObject(
            """{"musicCarouselShelfRenderer":{
                "header":{"musicCarouselShelfBasicHeaderRenderer":{"title":{"runs":[{"text":"Catalog",
                "navigationEndpoint":{"browseEndpoint":{"browseId":"FEmusic_moods_and_genres"}}}]}}},
                "contents":[$playlists]
            },"continuationCommand":{"token":"page-2"}}"""
        )
        val shelf = MoodsParser.homeShelves(root).single()
        assertEquals("FEmusic_moods_and_genres", shelf.browseId)
        assertEquals((1..28).map { "PL$it" }, shelf.playlists.map { it.id })
        assertEquals("page-2", MoodsParser.continuation(root))
    }

    @Test
    fun decadeTitlesResolve() {
        assertEquals(1950, MoodsParser.decadeOf("Anni '50"))
        assertEquals(2010, MoodsParser.decadeOf("Anni 2010"))
        assertEquals(2000, MoodsParser.decadeOf("00s"))
        assertEquals(null, MoodsParser.decadeOf("Relax"))
    }
}

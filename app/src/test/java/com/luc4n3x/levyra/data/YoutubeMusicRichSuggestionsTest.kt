package com.luc4n3x.levyra.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicRichSuggestionsTest {
    private val repository = YoutubeMusicRepository()

    @Test
    fun `suggestion response keeps queries and rich music items`() {
        val root = JSONObject().put(
            "contents",
            JSONArray()
                .put(
                    section(
                        JSONObject().put(
                            "searchSuggestionRenderer",
                            JSONObject()
                                .put("suggestion", JSONObject().put("runs", JSONArray().put(JSONObject().put("text", "coldplay yellow"))))
                                .put("navigationEndpoint", JSONObject().put("searchEndpoint", JSONObject().put("query", "coldplay yellow")))
                        )
                    )
                )
                .put(
                    section(
                        JSONObject().put("musicResponsiveListItemRenderer", trackRenderer("2nd73lyvq4w", "Yellow")),
                        JSONObject().put("musicResponsiveListItemRenderer", artistRenderer())
                    )
                )
        )

        val bundle = repository.parseSearchSuggestionBundle(root, "coldplay")

        assertEquals(listOf("coldplay yellow"), bundle.queries)
        assertEquals(listOf("2nd73lyvq4w"), bundle.songs.map { it.id })
        assertEquals(listOf("MPLAUC123456"), bundle.artists.map { it.browseId })
        assertTrue(bundle.hasRichItems)
    }

    @Test
    fun `query only response has no rich items`() {
        val root = JSONObject().put(
            "contents",
            JSONArray().put(
                section(
                    JSONObject().put(
                        "searchSuggestionRenderer",
                        JSONObject().put("navigationEndpoint", JSONObject().put("searchEndpoint", JSONObject().put("query", "radiohead")))
                    )
                )
            )
        )

        val bundle = repository.parseSearchSuggestionBundle(root, "radio")

        assertEquals(listOf("radiohead"), bundle.queries)
        assertTrue(!bundle.hasRichItems)
    }

    @Test
    fun `missing response produces an empty bundle`() {
        assertTrue(repository.parseSearchSuggestionBundle(null, "radio").isEmpty)
        assertTrue(repository.parseSearchSuggestionBundle(JSONObject(), "radio").isEmpty)
    }

    private fun section(vararg items: JSONObject): JSONObject {
        val contents = JSONArray()
        items.forEach(contents::put)
        return JSONObject().put("searchSuggestionsSectionRenderer", JSONObject().put("contents", contents))
    }

    private fun trackRenderer(videoId: String, title: String): JSONObject = JSONObject()
        .put("playlistItemData", JSONObject().put("videoId", videoId))
        .put("flexColumns", JSONArray().put(line(title)).put(line("Coldplay")))
        .put(
            "navigationEndpoint",
            JSONObject().put(
                "watchEndpoint",
                JSONObject()
                    .put("videoId", videoId)
                    .put(
                        "watchEndpointMusicSupportedConfigs",
                        JSONObject().put(
                            "watchEndpointMusicConfig",
                            JSONObject().put("musicVideoType", "MUSIC_VIDEO_TYPE_ATV")
                        )
                    )
            )
        )

    private fun artistRenderer(): JSONObject = JSONObject().put(
        "flexColumns",
        JSONArray().put(
            flexColumn(
                JSONArray().put(
                    JSONObject()
                        .put("text", "Coldplay")
                        .put(
                            "navigationEndpoint",
                            JSONObject().put("browseEndpoint", JSONObject().put("browseId", "MPLAUC123456"))
                        )
                )
            )
        )
    )

    private fun line(text: String): JSONObject = flexColumn(JSONArray().put(JSONObject().put("text", text)))

    private fun flexColumn(runs: JSONArray): JSONObject = JSONObject().put(
        "musicResponsiveListItemFlexColumnRenderer",
        JSONObject().put("text", JSONObject().put("runs", runs))
    )
}

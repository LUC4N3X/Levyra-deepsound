package com.luc4n3x.levyra.data.youtubemusic

import com.luc4n3x.levyra.data.YoutubeMusicPlaylistPageParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeMusicAccountBrowseTest {
    @Test
    fun readsAuthenticatedTwoColumnSecondaryTrackShelf() {
        val root = JSONObject("""{
            "contents": {"twoColumnBrowseResultsRenderer": {
                "tabs": [{"tabRenderer": {"content": {"sectionListRenderer": {"contents": []}}}}],
                "secondaryContents": {"sectionListRenderer": {"contents": [{"musicPlaylistShelfRenderer": {
                    "contents": [${rendererItem("PRIVATE_TRACK")}],
                    "continuations": [{"nextContinuationData": {"continuation": "PRIVATE_NEXT"}}]
                }}]}}
            }}
        }""")
        assertEquals(listOf("PRIVATE_TRACK"), rendererVideoIds(root))
        assertEquals("PRIVATE_NEXT", YoutubeMusicPlaylistPageParser.continuation(root))
    }

    @Test
    fun emptyPersonalPlaylistNeverIncludesRecommendedSongsOrTheirContinuation() {
        val root = JSONObject("""{
            "contents": {"singleColumnBrowseResultsRenderer": {"tabs": [{"tabRenderer": {"content": {
                "sectionListRenderer": {"contents": [
                    {"musicPlaylistShelfRenderer": {"contents": []}},
                    {"musicCarouselShelfRenderer": {"contents": [${rendererItem("RECOMMENDATION")}],
                        "continuations": [{"nextContinuationData": {"continuation": "RECOMMENDED_NEXT"}}]}}
                ]}
            }}}]}}
        }""")
        assertEquals(emptyList<String>(), rendererVideoIds(root))
        assertEquals("", YoutubeMusicPlaylistPageParser.continuation(root))
    }

    private fun rendererItem(id: String): String = JSONObject().put("musicResponsiveListItemRenderer",
        JSONObject().put("playlistItemData", JSONObject().put("videoId", id))).toString()

    private fun rendererVideoIds(root: JSONObject): List<String> = YoutubeMusicPlaylistPageParser.renderers(root)
        .map { it.getJSONObject("playlistItemData").getString("videoId") }
}

package com.luc4n3x.levyra.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicSearchEntityKindTest {
    private val repository = YoutubeMusicRepository()

    @Test
    fun `renderer with a video id is a track regardless of localized labels`() {
        val renderer = JSONObject()
            .put("playlistItemData", JSONObject().put("videoId", "2nd73lyvq4w"))
            .put("flexColumns", JSONArray().put(line("Yellow")).put(line("アーティスト")))

        assertEquals(SearchEntityKind.Track, repository.searchEntityKind(renderer))
    }

    @Test
    fun `artist row is detected from the browse endpoint page type not the localized word`() {
        val renderer = JSONObject().put(
            "flexColumns",
            JSONArray()
                .put(line("Coldplay"))
                .put(runLine(browseRun("아티스트", "UCchannel", "MUSIC_PAGE_TYPE_ARTIST")))
        )

        assertEquals(SearchEntityKind.Artist, repository.searchEntityKind(renderer))
    }

    @Test
    fun `album row wins over the artist endpoint it also carries`() {
        val renderer = JSONObject().put(
            "flexColumns",
            JSONArray()
                .put(runLine(browseRun("Ghost Stories", "MPREb_ghost", "MUSIC_PAGE_TYPE_ALBUM")))
                .put(runLine(browseRun("Coldplay", "UCchannel", "MUSIC_PAGE_TYPE_ARTIST")))
        )

        assertEquals(SearchEntityKind.Album, repository.searchEntityKind(renderer))
    }

    @Test
    fun `playlist row wins over the author artist endpoint`() {
        val renderer = JSONObject().put(
            "flexColumns",
            JSONArray()
                .put(runLine(browseRun("Chill Hits", "VLPL123", "MUSIC_PAGE_TYPE_PLAYLIST")))
                .put(runLine(browseRun("YouTube Music", "UCauthor", "MUSIC_PAGE_TYPE_ARTIST")))
        )

        assertEquals(SearchEntityKind.Playlist, repository.searchEntityKind(renderer))
    }

    @Test
    fun `watch endpoint carrying only a playlist id is a playlist`() {
        val renderer = JSONObject()
            .put("flexColumns", JSONArray().put(line("Chill Hits")))
            .put(
                "navigationEndpoint",
                JSONObject().put("watchEndpoint", JSONObject().put("playlistId", "PL123"))
            )

        assertEquals(SearchEntityKind.Playlist, repository.searchEntityKind(renderer))
    }

    @Test
    fun `overview splits official music videos out of the songs section`() {
        val root = JSONObject().put(
            "contents",
            JSONArray()
                .put(JSONObject().put("musicResponsiveListItemRenderer", trackRenderer("aaa", "Song", "MUSIC_VIDEO_TYPE_ATV")))
                .put(JSONObject().put("musicResponsiveListItemRenderer", trackRenderer("bbb", "Clip", "MUSIC_VIDEO_TYPE_OMV")))
        )

        val overview = repository.parseSearchOverview(root, "coldplay")

        assertEquals(listOf("aaa"), overview.songs.map { it.id })
        assertEquals(listOf("bbb"), overview.videos.map { it.id })
    }

    @Test
    fun `overview collects playlists with their canonical playlist id`() {
        val root = JSONObject().put(
            "contents",
            JSONArray().put(
                JSONObject().put(
                    "musicResponsiveListItemRenderer",
                    JSONObject().put(
                        "flexColumns",
                        JSONArray()
                            .put(runLine(browseRun("Chill Hits", "VLPL123", "MUSIC_PAGE_TYPE_PLAYLIST")))
                            .put(line("YouTube Music"))
                    )
                )
            )
        )

        val overview = repository.parseSearchOverview(root, "chill")

        assertEquals(1, overview.playlists.size)
        assertEquals("PL123", overview.playlists.single().playlistId)
        assertEquals("Chill Hits", overview.playlists.single().title)
    }

    @Test
    fun `overview is empty for an unparsable payload`() {
        val overview = repository.parseSearchOverview(JSONObject().put("contents", JSONArray()), "query")

        assertTrue(overview.isEmpty)
    }

    private fun trackRenderer(videoId: String, title: String, videoType: String): JSONObject = JSONObject()
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
                            JSONObject().put("musicVideoType", videoType)
                        )
                    )
            )
        )

    private fun line(text: String): JSONObject = flexColumn(JSONArray().put(JSONObject().put("text", text)))

    private fun runLine(run: JSONObject): JSONObject = flexColumn(JSONArray().put(run))

    private fun browseRun(text: String, browseId: String, pageType: String): JSONObject = JSONObject()
        .put("text", text)
        .put(
            "navigationEndpoint",
            JSONObject().put(
                "browseEndpoint",
                JSONObject()
                    .put("browseId", browseId)
                    .put(
                        "browseEndpointContextSupportedConfigs",
                        JSONObject().put(
                            "browseEndpointContextMusicConfig",
                            JSONObject().put("pageType", pageType)
                        )
                    )
            )
        )

    private fun flexColumn(runs: JSONArray): JSONObject = JSONObject().put(
        "musicResponsiveListItemFlexColumnRenderer",
        JSONObject().put("text", JSONObject().put("runs", runs))
    )
}

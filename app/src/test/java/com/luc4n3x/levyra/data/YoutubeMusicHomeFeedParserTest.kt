package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.YoutubeMusicHomeItemKind
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelfStyle
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeMusicHomeFeedParserTest {
    private val repository = YoutubeMusicRepository()

    @Test
    fun homeFeedKeepsChipsShelfStylesAndContinuation() {
        val root = JSONObject().put(
            "contents", JSONObject().put(
                "sectionListRenderer", JSONObject()
                    .put("header", JSONObject().put("chipCloudRenderer", JSONObject().put("chips", JSONArray().put(chip("Relax", "relax-params")))))
                    .put(
                        "contents", JSONArray()
                            .put(shelf("Quick picks", JSONArray().put(listRow("song0000001", "Morning Light", "Aurora Field"))))
                            .put(shelf("New albums", JSONArray().put(albumCard("MPREb_album01", "Northern Skies", "Aurora Field"))))
                            .put(shelf("New music videos", JSONArray().put(videoCard("video000001", "City Lights", "Neon Avenue"))))
                            .put(shelf("Artists for you", JSONArray().put(artistCard("UCartist00001", "Aurora Field"))))
                    )
                    .put("continuations", JSONArray().put(JSONObject().put("nextContinuationData", JSONObject().put("continuation", "next-page"))))
            )
        )

        val feed = repository.parseHomeFeed(root)

        assertEquals(listOf("Relax"), feed.chips.map { it.title })
        assertEquals("relax-params", feed.chips.single().params)
        assertEquals("next-page", feed.continuation)
        assertEquals(
            listOf(
                YoutubeMusicHomeShelfStyle.ListColumns,
                YoutubeMusicHomeShelfStyle.Cards,
                YoutubeMusicHomeShelfStyle.Videos,
                YoutubeMusicHomeShelfStyle.Artists
            ),
            feed.shelves.map { it.style }
        )
        assertEquals(YoutubeMusicHomeItemKind.Song, feed.shelves[0].items.single().kind)
        assertEquals(YoutubeMusicHomeItemKind.Album, feed.shelves[1].items.single().kind)
        assertEquals("MPREb_album01", feed.shelves[1].items.single().album?.browseId)
        assertEquals("video000001", feed.shelves[2].items.single().track?.id)
        assertEquals("UCartist00001", feed.shelves[3].items.single().artist?.browseId)
    }

    @Test
    fun navigationButtonShelfBecomesButtons() {
        val root = JSONObject().put(
            "contents", JSONArray().put(
                shelf("Moods", JSONArray().put(JSONObject().put("musicNavigationButtonRenderer", JSONObject().put("buttonText", runs("Chill")).put("solid", JSONObject().put("leftStripeColor", 0xFF7E57C2L)).put("clickCommand", JSONObject().put("browseEndpoint", JSONObject().put("browseId", "FEmusic_moods_and_genres_category").put("params", "chill-params"))))))
            )
        )

        val shelf = repository.parseHomeFeed(root).shelves.single()
        assertEquals(YoutubeMusicHomeShelfStyle.Buttons, shelf.style)
        assertEquals(listOf("Chill"), shelf.buttons.map { it.title })
        assertEquals(0xFF7E57C2L, shelf.buttons.single().stripeColor)
    }

    private fun runs(text: String) = JSONObject().put("runs", JSONArray().put(JSONObject().put("text", text)))

    private fun thumbnail(url: String) = JSONObject().put(
        "musicThumbnailRenderer", JSONObject().put(
            "thumbnail", JSONObject().put("thumbnails", JSONArray().put(JSONObject().put("url", url).put("width", 544).put("height", 544)))
        )
    )

    private fun chip(title: String, params: String) = JSONObject().put(
        "chipCloudChipRenderer", JSONObject()
            .put("text", runs(title))
            .put("navigationEndpoint", JSONObject().put("browseEndpoint", JSONObject().put("browseId", "FEmusic_home").put("params", params)))
    )

    private fun shelf(title: String, contents: JSONArray) = JSONObject().put(
        "musicCarouselShelfRenderer", JSONObject()
            .put("header", JSONObject().put("musicCarouselShelfBasicHeaderRenderer", JSONObject().put("title", runs(title))))
            .put("contents", contents)
    )

    private fun browseEndpoint(id: String, pageType: String) = JSONObject().put(
        "browseEndpoint", JSONObject().put("browseId", id).put(
            "browseEndpointContextSupportedConfigs",
            JSONObject().put("browseEndpointContextMusicConfig", JSONObject().put("pageType", pageType))
        )
    )

    private fun watch(videoId: String, videoType: String) = JSONObject().put(
        "watchEndpoint", JSONObject().put("videoId", videoId).put(
            "watchEndpointMusicSupportedConfigs",
            JSONObject().put("watchEndpointMusicConfig", JSONObject().put("musicVideoType", videoType))
        )
    )

    private fun linkedRuns(text: String, endpoint: JSONObject) =
        JSONObject().put("runs", JSONArray().put(JSONObject().put("text", text).put("navigationEndpoint", endpoint)))

    private fun flexColumn(text: JSONObject) =
        JSONObject().put("musicResponsiveListItemFlexColumnRenderer", JSONObject().put("text", text))

    private fun listRow(videoId: String, title: String, artist: String) = JSONObject().put(
        "musicResponsiveListItemRenderer", JSONObject()
            .put("thumbnail", thumbnail("https://lh3.googleusercontent.com/$videoId=w120-h120"))
            .put("playlistItemData", JSONObject().put("videoId", videoId))
            .put(
                "flexColumns", JSONArray()
                    .put(flexColumn(linkedRuns(title, watch(videoId, "MUSIC_VIDEO_TYPE_ATV"))))
                    .put(flexColumn(linkedRuns(artist, browseEndpoint("UCartist00001", "MUSIC_PAGE_TYPE_ARTIST"))))
            )
    )

    private fun albumCard(browseId: String, title: String, artist: String) = JSONObject().put(
        "musicTwoRowItemRenderer", JSONObject()
            .put("thumbnailRenderer", thumbnail("https://lh3.googleusercontent.com/$browseId=w226-h226"))
            .put("title", JSONObject().put("runs", JSONArray().put(JSONObject().put("text", title).put("navigationEndpoint", browseEndpoint(browseId, "MUSIC_PAGE_TYPE_ALBUM")))))
            .put("subtitle", JSONObject().put("runs", JSONArray().put(JSONObject().put("text", "Album")).put(JSONObject().put("text", " • ")).put(JSONObject().put("text", artist).put("navigationEndpoint", browseEndpoint("UCartist00001", "MUSIC_PAGE_TYPE_ARTIST")))))
            .put("navigationEndpoint", browseEndpoint(browseId, "MUSIC_PAGE_TYPE_ALBUM"))
    )

    private fun videoCard(videoId: String, title: String, artist: String) = JSONObject().put(
        "musicTwoRowItemRenderer", JSONObject()
            .put("aspectRatio", "MUSIC_TWO_ROW_ITEM_THUMBNAIL_ASPECT_RATIO_RECTANGLE_16_9")
            .put("thumbnailRenderer", thumbnail("https://i.ytimg.com/vi/$videoId/hqdefault.jpg"))
            .put("title", JSONObject().put("runs", JSONArray().put(JSONObject().put("text", title))))
            .put("subtitle", JSONObject().put("runs", JSONArray().put(JSONObject().put("text", artist).put("navigationEndpoint", browseEndpoint("UCartist00002", "MUSIC_PAGE_TYPE_ARTIST"))).put(JSONObject().put("text", " • ")).put(JSONObject().put("text", "1.2M views"))))
            .put("navigationEndpoint", watch(videoId, "MUSIC_VIDEO_TYPE_OMV"))
    )

    private fun artistCard(browseId: String, name: String) = JSONObject().put(
        "musicTwoRowItemRenderer", JSONObject()
            .put("thumbnailRenderer", thumbnail("https://lh3.googleusercontent.com/$browseId=w226-h226"))
            .put("title", JSONObject().put("runs", JSONArray().put(JSONObject().put("text", name).put("navigationEndpoint", browseEndpoint(browseId, "MUSIC_PAGE_TYPE_ARTIST")))))
            .put("subtitle", runs("1.5M subscribers"))
            .put("navigationEndpoint", browseEndpoint(browseId, "MUSIC_PAGE_TYPE_ARTIST"))
    )
}

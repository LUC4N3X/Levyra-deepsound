package com.luc4n3x.levyra.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeWebVideoSearchTest {

    private fun renderer(
        id: String,
        title: String,
        channel: String,
        badge: String?,
        length: String,
        views: String
    ): String {
        val badges = badge?.let { """"ownerBadges":[{"metadataBadgeRenderer":{"style":"$it"}}],""" }.orEmpty()
        return """
            {"videoRenderer":{
              "videoId":"$id",
              "title":{"runs":[{"text":"$title"}]},
              "ownerText":{"runs":[{"text":"$channel"}]},
              $badges
              "lengthText":{"simpleText":"$length"},
              "viewCountText":{"simpleText":"$views"},
              "thumbnail":{"thumbnails":[{"url":"https://i.ytimg.com/small.jpg"},{"url":"https://i.ytimg.com/large.jpg"}]}
            }}
        """.trimIndent()
    }

    private val response = JSONObject(
        """
        {"contents":{"twoColumnSearchResultsRenderer":{"primaryContents":{"sectionListRenderer":{"contents":[
          {"itemSectionRenderer":{"contents":[
            ${renderer("CTFtOOh47oo", "French Montana - Unforgettable ft. Swae Lee", "French Montana", "BADGE_STYLE_TYPE_VERIFIED_ARTIST", "4:47", "1,987,757,938 views")},
            ${renderer("MIcIza4sqaM", "French Montana - Unforgettable (Lyrics) ft. Swae Lee", "7clouds Rock", "BADGE_STYLE_TYPE_VERIFIED", "3:51", "173,462,968 views")},
            ${renderer("UsQhCVLp9tE", "Unforgettable (Loop)", "All Music Loops", null, "4:09:48", "16,502 views")},
            {"shelfRenderer":{"title":{"simpleText":"People also watched"}}},
            ${renderer("bad", "Broken id", "Nobody", null, "1:00", "1 view")}
          ]}}
        ]}}}}}
        """
    )

    @Test
    fun parsesChannelBadgesDurationAndViewsFromTheWebSearchPayload() {
        val results = parseYoutubeWebVideoResults(response, limit = 10)

        assertEquals(listOf("CTFtOOh47oo", "MIcIza4sqaM", "UsQhCVLp9tE"), results.map { it.videoId })
        val official = results[0]
        assertEquals("French Montana", official.channelName)
        assertTrue(official.officialArtistChannel)
        assertTrue(official.channelVerified)
        assertEquals(287_000L, official.durationMs)
        assertEquals(1_987_757_938L, official.viewCount)
        assertEquals("https://i.ytimg.com/large.jpg", official.thumbnailUrl)
        assertFalse(results[1].officialArtistChannel)
        assertTrue(results[1].channelVerified)
        assertFalse(results[2].channelVerified)
        assertEquals(14_988_000L, results[2].durationMs)
    }

    @Test
    fun respectsTheRequestedLimit() {
        assertEquals(1, parseYoutubeWebVideoResults(response, limit = 1).size)
    }

    @Test
    fun clockTextRejectsMalformedDurations() {
        assertEquals(0L, clockTextMs("LIVE"))
        assertEquals(0L, clockTextMs("1:xx"))
        assertEquals(58_000L, clockTextMs("0:58"))
    }
}

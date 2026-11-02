package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.data.local.podcastEpisodeCompleted
import com.luc4n3x.levyra.feature.podcast.PodcastEpisodeProgress
import com.luc4n3x.levyra.feature.podcast.resumePositionMs
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastLibraryParsingTest {

    @Test
    fun searchKeepsOnlyPodcastShowsAndDeduplicates() {
        val root = JSONObject(
            """
            {"contents":[
              ${showItem("MPSPPLshowAAAA1111", "Morning Talk", "Host One")},
              ${showItem("MPSPPLshowAAAA1111", "Morning Talk", "Host One")},
              ${showItem("MPREbalbum0000000", "Album", "Band", pageType = "MUSIC_PAGE_TYPE_ALBUM")},
              ${showItem("MPSP<script>", "Bad id", "x")}
            ]}
            """.trimIndent()
        )

        val shows = parsePodcastSearch(root)

        assertEquals(listOf("MPSPPLshowAAAA1111"), shows.map { it.browseId })
        assertEquals("Morning Talk", shows.single().title)
        assertEquals("Host One", shows.single().author)
        assertEquals("https://img/large.jpg", shows.single().thumbnailUrl)
    }

    @Test
    fun showPageReadsHeaderEpisodesAndContinuation() {
        val root = JSONObject(
            """
            {"header":{"musicResponsiveHeaderRenderer":{
                "title":{"runs":[{"text":"Morning Talk"}]},
                "straplineTextOne":{"runs":[{"text":"Host One"}]},
                "description":{"musicDescriptionShelfRenderer":{"header":{"runs":[{"text":"Morning Talk"}]},"description":{"runs":[{"text":"Daily chat"}]}}},
                ${thumbnail()}
            }},
            "contents":[${episode("abcdefghijk", "Ep. 2")}, ${episode("abcdefghijk", "Ep. 2 dup")}, ${episode("bad", "Broken")}, ${episode("lmnopqrstuv", "Ep. 1")}],
            "continuations":[{"nextContinuationData":{"continuation":"TOKEN-1"}}]}
            """.trimIndent()
        )

        val page = parsePodcastShow("MPSPPLshowAAAA1111", root)

        assertEquals("Morning Talk", page.show.title)
        assertEquals("Host One", page.show.author)
        assertEquals("Daily chat", page.description)
        assertEquals(listOf("abcdefghijk", "lmnopqrstuv"), page.episodes.episodes.map { it.id })
        assertTrue(page.episodes.episodes.all { it.artist == "Morning Talk" && it.isPodcastEpisode() })
        assertEquals("TOKEN-1", page.episodes.continuation)
    }

    @Test
    fun shelfEpisodeUsesTheShowNameNotTheDate() {
        val withShow = JSONObject(episode("abcdefghijk", "Ep").replace("\"onTap\"", "\"secondTitle\":{\"runs\":[{\"text\":\"Daily Show\"}]},\"onTap\""))
        val episode = parsePodcastEpisodePage(JSONObject().put("contents", org.json.JSONArray().put(withShow))).episodes.single()
        assertEquals("Daily Show", episode.artist)
    }

    @Test
    fun lastEpisodePageHasNoContinuation() {
        val page = parsePodcastEpisodePage(JSONObject("""{"contents":[${episode("abcdefghijk", "Only")}]}"""))
        assertNull(page.continuation)
        assertEquals(1, page.episodes.size)
    }

    @Test
    fun showIdValidationRejectsForeignIds() {
        assertTrue(isPodcastShowId("MPSPPLTDRWjDw7X3BXIl7zTU7Wj839oGQaKL03"))
        assertFalse(isPodcastShowId("UCg7mcZKjupbLdsuiMBVTuDw"))
        assertFalse(isPodcastShowId("MPSP../../etc"))
    }

    @Test
    fun completionCoversTheTailAndUnknownDuration() {
        assertFalse(podcastEpisodeCompleted(10_000L, 0L))
        assertFalse(podcastEpisodeCompleted(60_000L, 3_600_000L))
        assertTrue(podcastEpisodeCompleted(3_575_000L, 3_600_000L))
        assertTrue(podcastEpisodeCompleted(97_000L, 100_000L))
    }

    @Test
    fun resumeStartsOnlyFromMeaningfulUnfinishedProgress() {
        val episode = parsePodcastEpisodePage(JSONObject("""{"contents":[${episode("abcdefghijk", "Ep")}]}""")).episodes.single()
        assertEquals(0L, resumePositionMs(null))
        assertEquals(0L, resumePositionMs(PodcastEpisodeProgress(episode, 2_000L, 600_000L, false)))
        assertEquals(0L, resumePositionMs(PodcastEpisodeProgress(episode, 300_000L, 600_000L, true)))
        assertEquals(300_000L, resumePositionMs(PodcastEpisodeProgress(episode, 300_000L, 600_000L, false)))
    }

    private fun thumbnail() =
        """"thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[{"url":"https://img/small.jpg"},{"url":"https://img/large.jpg"}]}}}"""

    private fun showItem(browseId: String, title: String, author: String, pageType: String = "MUSIC_PAGE_TYPE_PODCAST_SHOW_DETAIL_PAGE") =
        """
        {"musicResponsiveListItemRenderer":{
          ${thumbnail()},
          "flexColumns":[
            {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"$title"}]}}},
            {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"$author"}]}}}
          ],
          "navigationEndpoint":{"browseEndpoint":{"browseId":"$browseId",
            "browseEndpointContextSupportedConfigs":{"browseEndpointContextMusicConfig":{"pageType":"$pageType"}}}}
        }}
        """.trimIndent()

    private fun episode(videoId: String, title: String) =
        """
        {"musicMultiRowListItemRenderer":{
          ${thumbnail()},
          "title":{"runs":[{"text":"$title"}]},
          "subtitle":{"runs":[{"text":"1K views • 2 Nov 2026"}]},
          "onTap":{"watchEndpoint":{"videoId":"$videoId"}}
        }}
        """.trimIndent()
}

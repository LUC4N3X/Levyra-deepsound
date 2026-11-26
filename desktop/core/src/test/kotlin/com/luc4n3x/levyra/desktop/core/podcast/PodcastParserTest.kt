package com.luc4n3x.levyra.desktop.core.podcast

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastParserTest {

    private fun episode(videoId: String, title: String) = """
        {"musicMultiRowListItemRenderer": {
          "onTap": {"watchEndpoint": {"videoId": "$videoId"}},
          "title": {"runs": [{"text": "$title"}]},
          "subtitle": {"runs": [{"text": "Nov 3 • 42 min"}]},
          "secondTitle": {"runs": [{"text": "Morning Show"}]},
          "thumbnail": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "http://insecure/a.jpg"}]}}}
        }}
    """

    @Test
    fun shelvesKeepValidEpisodesAndDropEmptyShelves() {
        val root = JSONObject("""
            {"contents": [
              {"musicCarouselShelfRenderer": {"header": {"title": {"runs": [{"text": "Top shows"}]}},
                "contents": [${episode("abcdefghijk", "Episode one")}, ${episode("bad", "Broken")}, ${episode("abcdefghijk", "Duplicate")}]}},
              {"musicCarouselShelfRenderer": {"header": {"title": {"runs": [{"text": "Empty"}]}}, "contents": []}}
            ]}
        """)
        val shelves = PodcastParser.shelves(root)
        assertEquals(1, shelves.size)
        assertEquals("Top shows", shelves[0].title)
        val track = shelves[0].episodes.single()
        assertEquals("abcdefghijk", track.id)
        assertEquals("Morning Show", track.artist)
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk", track.videoUrl)
        assertEquals("https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg", track.artworkUrl)
    }

    @Test
    fun searchAcceptsOnlyPodcastShowPages() {
        fun item(id: String, pageType: String) = """
            {"musicResponsiveListItemRenderer": {
              "navigationEndpoint": {"browseEndpoint": {"browseId": "$id",
                "browseEndpointContextSupportedConfigs": {"browseEndpointContextMusicConfig": {"pageType": "$pageType"}}}},
              "flexColumns": [
                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Show $id"}]}}},
                {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Host"}]}}}
              ]}}
        """
        val root = JSONObject("""{"items": [
            ${item("MPSPabcdefgh12", "MUSIC_PAGE_TYPE_PODCAST_SHOW_DETAIL_PAGE")},
            ${item("MPREalbum12345", "MUSIC_PAGE_TYPE_ALBUM")},
            ${item("bad id", "MUSIC_PAGE_TYPE_PODCAST_SHOW_DETAIL_PAGE")}
        ]}""")
        val shows = PodcastParser.search(root)
        assertEquals(listOf("MPSPabcdefgh12"), shows.map { it.browseId })
        assertEquals("Host", shows.single().author)
    }

    @Test
    fun showPageUsesHeaderTitleAndValidContinuation() {
        val root = JSONObject("""
            {"header": {"musicResponsiveHeaderRenderer": {
               "title": {"runs": [{"text": "Daily Talk"}]},
               "straplineTextOne": {"runs": [{"text": "Studio"}]},
               "thumbnail": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "https://img/s.jpg"}]}}},
               "description": {"musicDescriptionShelfRenderer": {"description": {"runs": [{"text": "About"}]}}}}},
             "list": [${episode("zyxwvutsrqp", "Pilot")}],
             "continuations": [{"nextContinuationData": {"continuation": "4qmFsgKABC_token-=="}}]}
        """)
        val page = PodcastParser.show("MPSPabcdefgh12", root)
        assertEquals("Daily Talk", page.show.title)
        assertEquals("Studio", page.show.author)
        assertEquals("https://img/s.jpg", page.show.thumbnailUrl)
        assertEquals("About", page.description)
        assertEquals("Daily Talk", page.episodes.episodes.single().artist)
        assertEquals("4qmFsgKABC_token-==", page.episodes.continuation)
    }

    @Test
    fun rejectsMalformedContinuation() {
        val root = JSONObject("""{"nextContinuationData": {"continuation": "has spaces & <tags>"}}""")
        assertNull(PodcastParser.episodePage(root).continuation)
        assertTrue(isPodcastShowId("MPSPabcdefgh12"))
    }
}

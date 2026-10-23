package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.ReleaseType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistPageReleasesTest {

    private fun card(title: String, subtitle: String, browseId: String) = """
        {"musicTwoRowItemRenderer": {
          "title": {"runs": [{"text": "$title"}]},
          "subtitle": {"runs": [{"text": "$subtitle"}]},
          "thumbnailRenderer": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "https://lh3.googleusercontent.com/synthetic=w544-h544-l90-rj", "width": 544, "height": 544}]}}},
          "navigationEndpoint": {"browseEndpoint": {"browseId": "$browseId"}}
        }}
    """.trimIndent()

    private fun shelf(title: String, vararg cards: String) = """
        {"musicCarouselShelfRenderer": {
          "header": {"musicCarouselShelfBasicHeaderRenderer": {"title": {"runs": [{"text": "$title"}]}}},
          "contents": [${cards.joinToString(",")}]
        }}
    """.trimIndent()

    private fun page(vararg shelves: String) = JSONObject("""{"contents": {"sectionListRenderer": {"contents": [${shelves.joinToString(",")}]}}}""")

    @Test
    fun albumsShelfWithYearOnlySubtitlesIsKept() {
        val root = page(
            shelf("Albums", card("Synthetic Album", "2024", "MPRE_SYNTH_ALBUM"), card("Older Album", "2020", "MPRE_SYNTH_OLD")),
            shelf("Singles & EPs", card("Synthetic EP", "EP • 2026", "MPRE_SYNTH_EP"), card("Synthetic Single", "Single • 2025", "MPRE_SYNTH_SINGLE"))
        )

        val releases = ArtistRepository(YoutubeMusicRepository()).extractArtistPageReleases(root).associateBy { it.browseId }

        assertEquals(ReleaseType.Album, releases["MPRE_SYNTH_ALBUM"]?.releaseType)
        assertEquals(ReleaseType.Album, releases["MPRE_SYNTH_OLD"]?.releaseType)
        assertEquals(ReleaseType.Ep, releases["MPRE_SYNTH_EP"]?.releaseType)
        assertEquals(ReleaseType.Single, releases["MPRE_SYNTH_SINGLE"]?.releaseType)
        assertEquals("2024", releases["MPRE_SYNTH_ALBUM"]?.year)
    }

    @Test
    fun localizedAlbumShelfTitleIsRecognized() {
        val root = page(shelf("Album", card("Album Sintetico", "2023", "MPRE_SYNTH_IT")))

        val releases = ArtistRepository(YoutubeMusicRepository()).extractArtistPageReleases(root)

        assertEquals(listOf(ReleaseType.Album), releases.map { it.releaseType })
    }

    @Test
    fun unlabeledShelvesStillRequireAReleaseLabel() {
        val root = page(shelf("Featured on", card("Not A Release", "2024", "MPRE_SYNTH_UNKNOWN")))

        val releases = ArtistRepository(YoutubeMusicRepository()).extractArtistPageReleases(root)

        assertTrue(releases.isEmpty())
    }
}

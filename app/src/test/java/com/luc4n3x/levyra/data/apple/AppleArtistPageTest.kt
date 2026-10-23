package com.luc4n3x.levyra.data.apple

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleArtistPageTest {

    private val header = """
        {"id":"artist-header","itemKind":"artistDetailHeader","items":[{
          "id":"111","title":"Synthetic Artist",
          "contentDescriptor":{"kind":"artist","identifiers":{"storeAdamID":"111"},"url":"https://music.apple.com/us/artist/synthetic/111"},
          "artwork":{"dictionary":{"url":"https://is1-ssl.mzstatic.com/image/thumb/artist/{w}x{h}{c}.{f}"}},
          "videoArtwork":{"dictionary":{"motionArtistSquare1x1":{"video":"https://mvod.itunes.apple.com/synthetic/square.m3u8",
            "previewFrame":{"url":"https://is1-ssl.mzstatic.com/image/thumb/preview/{w}x{h}bb.jpg","width":3840,"height":3840}}}}
        }]}
    """.trimIndent()

    private val featured = """
        {"id":"featured","itemKind":"artistFeaturedContentAndTracks","items":[{
          "featuredContent":{"id":"222","contentDescriptor":{"kind":"album","identifiers":{"storeAdamID":"222"},"url":"https://music.apple.com/us/album/new-ep/222"},
            "artwork":{"dictionary":{"url":"https://is1-ssl.mzstatic.com/image/thumb/ep/{w}x{h}{c}.{f}"}},
            "detailText":{"headline":"Aug 28, 2026","title":"New Release - EP","subtitle":"5 songs","showExplicitBadge":true}},
          "tracks":[{"id":"333","title":"Synthetic Song","subtitleLinks":[{"title":"Synthetic Artist"}],
            "contentDescriptor":{"kind":"song","identifiers":{"storeAdamID":"333"}}}]
        }]}
    """.trimIndent()

    private val albums = """
        {"id":"albums","itemKind":"squareLockup","header":{"item":{"titleLink":{"title":"Albums"}}},"items":[
          {"id":"444","titleLinks":[{"title":"Synthetic Album"}],"subtitleLinks":[{"title":"2024"}],
           "contentDescriptor":{"kind":"album","identifiers":{"storeAdamID":"444"},"url":"https://music.apple.com/us/album/synthetic-album/444"}}
        ]}
    """.trimIndent()

    private val essentials = """
        {"id":"essentials","itemKind":"adaptiveHorizontalSquareLockup","header":{"item":{"titleLink":{"title":"Essential Albums"}}},"items":[
          {"id":"555","horizontalLockup":{"id":"555","title":"Essential Album","subtitle":"An editorial note.",
           "contentDescriptor":{"kind":"album","identifiers":{"storeAdamID":"555"},"url":"https://music.apple.com/us/album/essential/555"}}}
        ]}
    """.trimIndent()

    private val radio = """
        {"id":"radio","itemKind":"horizontalLockup","header":{"item":{"titleLink":{"title":"More To Hear"}}},"items":[
          {"id":"ra.1","title":"Radio Show","contentDescriptor":{"kind":"radioStation","identifiers":{"storeAdamID":"ra.1"}}}
        ]}
    """.trimIndent()

    private val movies = """
        {"id":"movies","itemKind":"verticalVideoLockup","header":{"item":{"titleLink":{"title":"More To See"}}},"items":[
          {"id":"m1","titleLinks":[{"title":"Concert Film"}],"contentDescriptor":{"kind":"movie","identifiers":{"storeAdamID":"m1"}}}
        ]}
    """.trimIndent()

    private val similar = """
        {"id":"similar","itemKind":"bubbleLockup","header":{"item":{"titleLink":{"title":"Similar Artists"}}},"items":[
          {"id":"666","title":"Other Artist","contentDescriptor":{"kind":"artist","identifiers":{"storeAdamID":"666"}}}
        ]}
    """.trimIndent()

    private fun page(vararg sections: String) =
        """<html><script type="application/json" id="serialized-server-data">{"data":[{"data":{"sections":[${sections.joinToString(",")}]}}]}</script></html>"""

    @Test
    fun parsesHeaderMotionLatestReleaseAndShelvesInPageOrder() {
        val parsed = AppleArtistPageParser.parse(page(header, featured, essentials, albums, radio, movies, similar), 7L)!!

        assertEquals("111", parsed.id)
        assertEquals("Synthetic Artist", parsed.name)
        assertEquals("https://mvod.itunes.apple.com/synthetic/square.m3u8", parsed.squareMotion?.videoUrl)
        assertEquals(3840, parsed.squareMotion?.width)
        assertEquals("Aug 28, 2026", parsed.latestRelease?.headline)
        assertEquals("5 songs", parsed.latestRelease?.detail)
        assertTrue(parsed.latestRelease?.item?.explicit == true)
        assertEquals(listOf("Synthetic Song"), parsed.topSongs.map { it.title })
        assertEquals(
            listOf(AppleArtistShelfStyle.Feature, AppleArtistShelfStyle.Square, AppleArtistShelfStyle.Artists),
            parsed.shelves.map { it.style }
        )
        assertEquals("An editorial note.", parsed.shelves.first().items.single().subtitle)
        assertEquals("2024", parsed.shelves[1].items.single().subtitle)
    }

    @Test
    fun separateTopSongsShelfIsUsedWhenTheFeaturedBlockIsMissing() {
        val songs = """
            {"id":"songs","itemKind":"trackLockup","header":{"item":{"titleLink":{"title":"Top brani"}}},"items":[
              {"id":"777","title":"Brano","contentDescriptor":{"kind":"song","identifiers":{"storeAdamID":"777"}}}
            ]}
        """.trimIndent()

        val parsed = AppleArtistPageParser.parse(page(header, songs, albums), 0L)!!

        assertNull(parsed.latestRelease)
        assertEquals(listOf("Brano"), parsed.topSongs.map { it.title })
        assertEquals(1, parsed.shelves.size)
    }

    @Test
    fun pageWithoutHeaderIsRejected() {
        assertNull(AppleArtistPageParser.parse(page(albums), 0L))
        assertNull(AppleArtistPageParser.parse("<html></html>", 0L))
    }

    @Test
    fun searchHeaderCarriesMotionAndArtistInfo() {
        val attributes = JSONObject(
            """
            {"name":"Synthetic Artist","origin":"London, England","bornOrFormed":"22 August 1995","isGroup":false,
             "genreNames":["Pop"],"artistBio":"Before <i>Album</i>.",
             "artwork":{"url":"https://is1-ssl.mzstatic.com/image/thumb/artist/{w}x{h}bb.jpg"},
             "editorialVideo":{"motionArtistSquare1x1":{"video":"https://mvod.itunes.apple.com/synthetic/square.m3u8",
               "previewFrame":{"url":"https://is1-ssl.mzstatic.com/image/thumb/preview/{w}x{h}bb.jpg","width":1080,"height":1080}}}}
            """.trimIndent()
        )

        val header = AppleArtistPageParser.header("111", attributes, 5L)!!

        assertEquals("https://mvod.itunes.apple.com/synthetic/square.m3u8", header.squareMotion?.videoUrl)
        assertEquals("London, England", header.info?.origin)
        assertEquals("22 August 1995", header.info?.bornOrFormed)
        assertEquals(listOf("Pop"), header.info?.genres)
        assertTrue(header.shelves.isEmpty())
    }

    @Test
    fun biographyMarkupKeepsItalicRunsAndDecodesEntities() {
        val runs = AppleArtistPageParser.styledText("Before <i>Future &amp; Past</i>, after<br/>end")

        assertEquals(listOf("Before " to false, "Future & Past" to true, ", after" to false, "\n" to false, "end" to false), runs)
    }

    @Test
    fun artistCandidateRequiresTheSameName() {
        val candidates = listOf("1" to "Dua Lipa & Friends", "2" to "Dúa Lipa", "3" to "Dua Lipa")

        assertEquals("2", selectAppleArtistCandidate("dua lipa", candidates))
        assertNull(selectAppleArtistCandidate("Dua Lipa", listOf("9" to "Dua Lipa Tribute Band")))
        assertNull(selectAppleArtistCandidate(" ", candidates))
    }
}

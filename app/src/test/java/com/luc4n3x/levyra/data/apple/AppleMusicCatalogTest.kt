package com.luc4n3x.levyra.data.apple

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleMusicCatalogTest {

    private fun page(sections: String): String =
        """<html><body><script type="application/json" id="serialized-server-data">{"data":[{"intent":{"storefront":"it"},"data":{"sections":[$sections]}}],"userTokenHash":""}</script></body></html>"""

    private val hero = """
        {"id":"hero","itemKind":"flowcaseLockup","presentation":{"layout":{"numberOfRows":1}},"items":[
          {"id":"h1","heading":"NEW ALBUM","title":"Album One","subtitle":"Artist A","description":"A story.",
           "artwork":{"dictionary":{"url":"https://is1-ssl.mzstatic.com/image/thumb/a/{w}x{h}{c}.{f}","bgColor":"020c1f"}},
           "coverArtwork":{"dictionary":{"url":"https://is1-ssl.mzstatic.com/image/thumb/c/{w}x{h}{c}.{f}","bgColor":"032034"}},
           "contentDescriptor":{"kind":"album","identifiers":{"storeAdamID":"111"},"url":"https://music.apple.com/it/album/one/111"}}
        ]}
    """.trimIndent()

    private val songs = """
        {"id":"songs","itemKind":"trackLockup","presentation":{"layout":{"numberOfRows":4}},
         "header":{"item":{"titleLink":{"title":"Best New Songs"}}},"items":[
          {"id":"s1","title":"Song One","subtitleLinks":[{"title":"Artist A"},{"title":"Artist B"}],"showExplicitBadge":true,"duration":174867,
           "tertiaryLinks":[{"title":"Album One"}],
           "artwork":{"dictionary":{"url":"https://is1-ssl.mzstatic.com/image/thumb/s/{w}x{h}{c}.{f}"}},
           "contentDescriptor":{"kind":"song","identifiers":{"storeAdamID":"222"},"url":"https://music.apple.com/it/album/one/111?i=222"}},
          {"id":"s1dup","title":"Song One","subtitleLinks":[{"title":"Artist A"}],
           "contentDescriptor":{"kind":"song","identifiers":{"storeAdamID":"222"}}},
          {"id":"blank","title":"","contentDescriptor":{"kind":"song","identifiers":{"storeAdamID":"333"}}}
        ]}
    """.trimIndent()

    private val charts = """
        {"id":"top100","itemKind":"squareLockup","presentation":{"layout":{"numberOfRows":1}},
         "header":{"item":{"titleLink":{"title":"Daily Top 100"}}},"items":[
          {"id":"c1","titleLinks":[{"title":"Top 100: Global"}],"subtitleLinks":[{"title":"Apple Music"}],
           "playAction":{"${'$'}kind":"playAction"},
           "segue":{"destination":{"contentDescriptor":{"kind":"playlist","identifiers":{"storeAdamID":"pl.d25f5d1181894928af76c85c967f8f31"},"url":"https://music.apple.com/it/playlist/top-100-global/pl.d25f5d1181894928af76c85c967f8f31"}}}},
          {"id":"c2","titleLinks":[{"title":"Top 25: Milan"}],"subtitleLinks":[{"title":"Apple Music"}],
           "contentDescriptor":{"kind":"playlist","identifiers":{"storeAdamID":"pl.c16207854c324cd1822c2632215c1fb3"}}}
        ]}
    """.trimIndent()

    private val exclusive = """
        {"id":"only","itemKind":"squareLockup","header":{"item":{"titleLink":{"title":"Only on Apple Music"}}},"items":[
          {"id":"x","titleLinks":[{"title":"Session"}],"contentDescriptor":{"kind":"album","identifiers":{"storeAdamID":"9"}}}
        ]}
    """.trimIndent()

    private val video = """
        {"id":"videos","itemKind":"verticalVideoLockup","header":{"item":{"titleLink":{"title":"New Music Videos"}}},"items":[
          {"id":"v","titleLinks":[{"title":"Clip"}],"contentDescriptor":{"kind":"musicVideo","identifiers":{"storeAdamID":"7"}}}
        ]}
    """.trimIndent()

    @Test
    fun parsesShelvesItemsAndLayout() {
        val parsed = AppleCatalogPageParser.parse(page(listOf(hero, songs, charts).joinToString(",")), "it", 42L)
        assertNotNull(parsed)
        val result = parsed!!
        assertEquals(42L, result.fetchedAtMs)
        assertEquals(listOf(AppleCatalogShelfKind.Hero, AppleCatalogShelfKind.Songs, AppleCatalogShelfKind.Grid), result.shelves.map { it.kind })

        val heroItem = result.shelves[0].items.single()
        assertEquals(AppleCatalogItemKind.Album, heroItem.kind)
        assertEquals("111", heroItem.id)
        assertEquals("NEW ALBUM", heroItem.heading)
        assertEquals(0xFF032034.toInt(), heroItem.backgroundColor)
        assertEquals("https://is1-ssl.mzstatic.com/image/thumb/c/200x200cc.jpg", heroItem.coverUrl(200))
        assertEquals("https://is1-ssl.mzstatic.com/image/thumb/a/1200x800cc.jpg", heroItem.heroUrl(1200))

        val songShelf = result.shelves[1]
        assertEquals("Best New Songs", songShelf.title)
        assertEquals(4, songShelf.rows)
        assertEquals(1, songShelf.items.size)
        val song = songShelf.items.single()
        assertEquals("Artist A, Artist B", song.subtitle)
        assertEquals("Album One", song.album)
        assertTrue(song.explicit)
        assertEquals(174867L, song.durationMs)

        val chartItem = result.shelves[2].items.first()
        assertEquals(AppleCatalogItemKind.Playlist, chartItem.kind)
        assertEquals("pl.d25f5d1181894928af76c85c967f8f31", chartItem.id)
    }

    @Test
    fun rejectsMissingOrBrokenMarkup() {
        assertNull(AppleCatalogPageParser.parse("<html></html>", "it", 0L))
        assertNull(AppleCatalogPageParser.parse("""<script id="serialized-server-data">{not json</script>""", "it", 0L))
        assertNull(AppleCatalogPageParser.parse(page(""), "it", 0L))
        assertNull(AppleCatalogPageParser.parse(page("""{"itemKind":"unknownLockup","items":[{"title":"x"}]}"""), "it", 0L))
    }

    @Test
    fun albumUrlKeepsStorefrontAndCatalogId() {
        assertEquals(
            "us" to "6822944288",
            AppleCatalogIds.albumFromUrl("https://music.apple.com/us/album/young-manhood-and-other-donatable-organs/6822944288")
        )
        assertEquals("it" to "1440857781", AppleCatalogIds.albumFromUrl("https://music.apple.com/it/album/x/1440857781?i=1440857782"))
        assertNull(AppleCatalogIds.albumFromUrl("https://music.apple.com/us/playlist/today/pl.f4d106fed2bd41149aaacabb233eb5eb"))
        assertNull(AppleCatalogIds.albumFromUrl("https://music.apple.com.evil.example/us/album/x/6822944288"))
        assertNull(AppleCatalogIds.albumFromUrl("https://music.apple.com/us/album/x/not-an-id"))
        assertNull(AppleCatalogIds.albumFromUrl(""))
    }

    @Test
    fun codecRoundTripsPage() {
        val parsed = AppleCatalogPageParser.parse(page(listOf(hero, songs, charts).joinToString(",")), "it", 99L)!!
        val decoded = AppleCatalogPageCodec.decode(AppleCatalogPageCodec.encode(parsed), "it")
        assertEquals(parsed, decoded)
        assertNull(AppleCatalogPageCodec.decode("garbage", "it"))
    }

    @Test
    fun homeSelectionKeepsPlayableShelvesAndStopsAfterCharts() {
        val parsed = AppleCatalogPageParser.parse(page(listOf(hero, songs, exclusive, video, charts, charts, songs).joinToString(",")), "it", 0L)!!
        val selected = AppleMusicHomeShelves.select(parsed)
        assertEquals(
            listOf(AppleHomeShelfRole.Hero, AppleHomeShelfRole.Songs, AppleHomeShelfRole.Charts, AppleHomeShelfRole.Charts),
            selected.map { it.role }
        )
        assertFalse(selected.any { it.shelf.title.contains("Apple Music") })
        assertTrue(AppleMusicHomeShelves.select(null).isEmpty())
    }

    @Test
    fun chartTitlesSplitIntoBadgeAndName() {
        assertEquals("Top 100" to "Italia", AppleMusicHomeShelves.chartParts("Top 100: Italia"))
        assertEquals("Top 25" to "New York", AppleMusicHomeShelves.chartParts("Top 25: New York"))
        assertNull(AppleMusicHomeShelves.chartParts("Today's Hits"))
        assertNull(AppleMusicHomeShelves.chartParts("Top 50:"))
    }

    @Test
    fun songItemsBecomeResolvableTracks() {
        val parsed = AppleCatalogPageParser.parse(page(songs), "it", 0L)!!
        val track = AppleCatalogTracks.fromItem(parsed.shelves.single().items.single())
        assertNotNull(track)
        assertTrue(track!!.id.startsWith("chart-apple-"))
        assertEquals("", track.streamUrl)
        assertEquals("222", track.appleSongId)
        assertTrue(track.explicit)
        assertEquals("https://is1-ssl.mzstatic.com/image/thumb/s/600x600cc.jpg", track.thumbnailUrl)
        assertEquals(track.id, AppleCatalogTracks.track("Song One", "Artist A, Artist B")!!.id)
        assertNull(AppleCatalogTracks.track("  ", "Artist"))
    }

    @Test
    fun identifiersAndStorefrontsAreValidated() {
        assertEquals("pl.c16207854c324cd1822c2632215c1fb3", AppleCatalogIds.playlist("PL.C16207854C324CD1822C2632215C1FB3"))
        assertNull(AppleCatalogIds.playlist("../../etc/passwd"))
        assertEquals("pl.c16207854c324cd1822c2632215c1fb3", AppleCatalogIds.playlistIdFromUrl("https://music.apple.com/it/playlist/x/pl.c16207854c324cd1822c2632215c1fb3?l=en"))
        assertTrue(AppleCatalogIds.isCatalogUrl("https://music.apple.com/it/new"))
        assertFalse(AppleCatalogIds.isCatalogUrl("http://music.apple.com/it/new"))
        assertFalse(AppleCatalogIds.isCatalogUrl("https://music.apple.com.evil.test/it/new"))
        assertFalse(AppleCatalogIds.isCatalogUrl("https://user@music.apple.com/it/new"))
        assertFalse(AppleCatalogIds.isCatalogUrl("https://music.apple.com:8443/it/new"))
        assertEquals("it", AppleStorefronts.normalize("IT"))
        assertEquals("us", AppleStorefronts.normalize("italy"))
        assertEquals("en", AppleStorefronts.language("en_GB"))
        assertEquals("en", AppleStorefronts.language("?"))
    }

    @Test
    fun rateLimitBackoffHonoursRetryAfterAndResets() {
        val backoff = AppleCatalogBackoff(defaultDelayMs = 1_000L, maxDelayMs = 10_000L)
        assertFalse(backoff.blocked(0L))
        backoff.onRateLimited(now = 0L, retryAfter = "3")
        assertTrue(backoff.blocked(2_999L))
        assertFalse(backoff.blocked(3_000L))
        backoff.onRateLimited(now = 0L, retryAfter = null)
        assertTrue(backoff.blocked(1_999L))
        assertFalse(backoff.blocked(2_000L))
        repeat(6) { backoff.onRateLimited(now = 0L, retryAfter = null) }
        assertTrue(backoff.blocked(7_999L))
        assertFalse(backoff.blocked(10_001L))
        backoff.onSuccess()
        assertFalse(backoff.blocked(1L))
    }
}

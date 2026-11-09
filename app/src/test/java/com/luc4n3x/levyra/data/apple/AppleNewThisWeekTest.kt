package com.luc4n3x.levyra.data.apple

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppleNewThisWeekTest {

    private fun page(sections: String): AppleCatalogPage? = AppleCatalogPageParser.parse(
        """<html><body><script type="application/json" id="serialized-server-data">{"data":[{"data":{"sections":[$sections]}}]}</script></body></html>""",
        "it",
        0L
    )

    private val songs = """
        {"id":"songs","itemKind":"trackLockup","header":{"item":{"titleLink":{"title":"I brani del momento"}}},"items":[
          {"id":"s1","title":"Song","subtitleLinks":[{"title":"Artist"}],"contentDescriptor":{"kind":"song","identifiers":{"storeAdamID":"1"}}}
        ]}
    """.trimIndent()

    private fun albums(id: String, room: String, vararg albumIds: String): String {
        val items = albumIds.joinToString(",") { albumId ->
            """{"id":"a$albumId","titleLinks":[{"title":"Album $albumId"}],"subtitleLinks":[{"title":"Artist $albumId"}],
               "contentDescriptor":{"kind":"album","identifiers":{"storeAdamID":"$albumId"},"url":"https://music.apple.com/it/album/x/1000$albumId"}}"""
        }
        return """{"id":"$id","itemKind":"squareLockup","header":{"item":{"titleLink":{"title":"$id",
            "segue":{"destination":{"kind":"catalogPage","intent":{"id":"$room","storefront":"it"}}}}}},"items":[$items]}"""
    }

    @Test
    fun `new this week is the album shelf right after new songs and keeps its room`() {
        val parsed = page(listOf(albums("best", "111", "9"), songs, albums("week", "6831372660", "10", "11"), albums("recent", "222", "12")).joinToString(","))

        val shelf = appleNewThisWeekShelf(parsed)

        assertEquals("6831372660", shelf?.roomId)
        assertEquals(listOf("Album 10", "Album 11"), appleAlbumHits(shelf!!.items.asSequence(), 10).map { it.title })
    }

    @Test
    fun `room id survives the page cache`() {
        val parsed = page(listOf(songs, albums("week", "6831372660", "10")).joinToString(","))!!

        val decoded = AppleCatalogPageCodec.decode(AppleCatalogPageCodec.encode(parsed), "it")

        assertEquals("6831372660", appleNewThisWeekShelf(decoded)?.roomId)
    }

    @Test
    fun `no shelf when songs are not followed by albums`() {
        assertNull(appleNewThisWeekShelf(page(songs)))
    }
}

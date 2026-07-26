package com.luc4n3x.levyra.desktop.core.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartFeedParserTest {

    private val modernFeed = """
        {
          "feed": {
            "results": [
              {
                "name": "Brano Uno",
                "artistName": "Artista Uno",
                "artworkUrl100": "https://is1-ssl.mzstatic.com/image/thumb/cover.jpg/100x100bb.jpg"
              },
              {
                "name": "Brano Due",
                "artistName": "Artista Due",
                "artworkUrl100": "https://is1-ssl.mzstatic.com/image/thumb/cover2.jpg/100x100bb.jpg"
              }
            ]
          }
        }
    """.trimIndent()

    private val classicFeed = """
        {
          "feed": {
            "entry": [
              {
                "im:name": { "label": "Brano Classico" },
                "im:artist": { "label": "Artista Classico" },
                "im:image": [
                  { "label": "https://host/55x55bb.png" },
                  { "label": "https://host/170x170bb.png" }
                ]
              }
            ]
          }
        }
    """.trimIndent()

    @Test
    fun `modern feed produces chart tracks without playback url`() {
        val tracks = ChartFeedParser.modern(modernFeed, 10)
        assertEquals(2, tracks.size)
        assertEquals("Brano Uno", tracks.first().title)
        assertEquals("Artista Uno", tracks.first().artist)
        assertTrue(tracks.first().videoUrl.isEmpty())
        assertTrue(tracks.first().artworkUrl.endsWith("600x600bb.jpg"))
    }

    @Test
    fun `modern feed honours the limit`() {
        assertEquals(1, ChartFeedParser.modern(modernFeed, 1).size)
    }

    @Test
    fun `classic feed picks the largest artwork`() {
        val tracks = ChartFeedParser.classic(classicFeed, 10)
        assertEquals(1, tracks.size)
        assertEquals("Brano Classico", tracks.first().title)
        assertEquals("https://host/600x600bb.png", tracks.first().artworkUrl)
    }

    @Test
    fun `malformed payloads return no tracks`() {
        assertTrue(ChartFeedParser.modern("not json", 10).isEmpty())
        assertTrue(ChartFeedParser.classic("{}", 10).isEmpty())
    }

    @Test
    fun `normalisation removes noise used for matching`() {
        assertEquals("brano uno", ChartFeedParser.normalize("Brano Uno (Official Video)"))
        assertEquals("brano uno", ChartFeedParser.normalize("Brano Uno (feat. Qualcuno)"))
        assertEquals("brano uno qualcuno", ChartFeedParser.normalize("Brano Uno feat. Qualcuno"))
    }

    @Test
    fun `chart ids are stable and unique per track`() {
        val first = ChartFeedParser.chartId("Brano Uno", "Artista Uno")
        assertEquals(first, ChartFeedParser.chartId("Brano Uno", "Artista Uno"))
        assertTrue(first != ChartFeedParser.chartId("Brano Due", "Artista Uno"))
    }
}

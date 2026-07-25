package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartFeedParserSourceTest {

    @Test
    fun appleFallbackIsExplicitlyIdentified() {
        val tracks = ChartFeedParser.modern(
            """
                {
                  "feed": {
                    "results": [
                      {
                        "name": "Fallback Song",
                        "artistName": "Fallback Artist",
                        "artworkUrl100": "https://cdn.example/100x100bb.jpg"
                      }
                    ]
                  }
                }
            """.trimIndent(),
            limit = 1
        )

        assertEquals("Apple Music Charts", tracks.single().source)
        assertEquals("Apple Music Charts", tracks.single().album)
    }
}

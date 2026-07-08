package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsParsingTest {
    @Test
    fun lrcParserExpandsMultipleTimestampsAndComputesEndTimes() {
        val lines = LrcLyricsParser.parse(
            """
            [00:10.00][00:20.50]Same hook
            [00:25.00]Next line
            """.trimIndent()
        )

        assertEquals(3, lines.size)
        assertEquals(10_000L, lines[0].startMs)
        assertEquals("Same hook", lines[0].text)
        assertEquals(20_500L, lines[1].startMs)
        assertEquals(24_920L, lines[1].endMs)
        assertEquals("Next line", lines[2].text)
    }

    @Test
    fun ttmlParserKeepsLineTimingAndWordTiming() {
        val lines = TtmlLyricsParser.parse(
            """
            <tt>
              <body>
                <div>
                  <p begin="00:01.000" end="00:03.500">
                    <span begin="00:01.000" end="00:01.400">Hello </span>
                    <span begin="00:01.500" end="00:02.100">world</span>
                  </p>
                </div>
              </body>
            </tt>
            """.trimIndent()
        )

        assertEquals(1, lines.size)
        assertEquals(1_000L, lines[0].startMs)
        assertEquals(3_500L, lines[0].endMs)
        assertEquals("Hello world", lines[0].text)
        assertEquals(2, lines[0].words.size)
        assertEquals("Hello", lines[0].words[0].text)
        assertEquals(1_400L, lines[0].words[0].endMs)
        assertEquals("world", lines[0].words[1].text)
    }

    @Test
    fun rankerPrefersSyncedCloseDurationCandidate() {
        val requested = LyricsRequest("Song", "Artist", 180)
        val syncedClose = LyricsCandidate(
            result = LyricsRepository.LyricsResult(
                synced = true,
                lines = listOf(LrcLyricsParser.parse("[00:01.00]Line").first()),
                provider = "Synced",
                confidence = 0,
                cached = false
            ),
            title = "Song",
            artist = "Artist",
            durationSec = 181
        )
        val plainExact = syncedClose.copy(
            result = syncedClose.result.copy(synced = false, provider = "Plain"),
            durationSec = 180
        )

        val best = LyricsResultRanker.best(listOf(plainExact, syncedClose), requested)

        assertEquals("Synced", best?.provider)
        assertTrue((best?.confidence ?: 0) >= 80)
    }
}

package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.ui.lyrics.customLyricsTimestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomLyricsTest {
    @Test
    fun syncedLrcKeepsTimestampsAndSkipsUntimedLines() {
        val parse = CustomLyricsParser.parse(
            """
            [ar:Unknown Artist]
            [ti:Demo]
            [00:12.50]First line
            stray line without time
            [00:16.2]Second line
            [01:02.345]Third line
            """.trimIndent()
        )

        val valid = parse as CustomLyricsParse.Valid
        assertTrue(valid.synced)
        assertEquals(1, valid.ignoredLines)
        assertEquals(listOf(12_500L, 16_200L, 62_345L), valid.lines.map { it.startMs })
        assertEquals("First line", valid.lines.first().text)
    }

    @Test
    fun unsortedAndRepeatedTimestampsAreOrdered() {
        val parse = CustomLyricsParser.parse("[00:20.00]Later\n[00:05.00][00:30.00]Chorus") as CustomLyricsParse.Valid

        assertEquals(listOf(5_000L, 20_000L, 30_000L), parse.lines.map { it.startMs })
    }

    @Test
    fun indentedTimestampsAreStillSynced() {
        val parse = CustomLyricsParser.parse("  [00:01.00]Indented\n  [00:03.00]Again") as CustomLyricsParse.Valid

        assertTrue(parse.synced)
        assertEquals(2, parse.lines.size)
    }

    @Test
    fun plainTextIsAcceptedAsUnsynced() {
        val parse = CustomLyricsParser.parse("First verse line\nSecond verse line") as CustomLyricsParse.Valid

        assertEquals(false, parse.synced)
        assertEquals(0, parse.ignoredLines)
        assertEquals(2, parse.lines.size)
    }

    @Test
    fun blankTimestampOnlyAndOversizedInputAreRejected() {
        assertEquals(CustomLyricsParse.Empty, CustomLyricsParser.parse("  \n "))
        assertEquals(CustomLyricsParse.Invalid, CustomLyricsParser.parse("[00:01.00]\n[00:02.00]   "))
        assertEquals(CustomLyricsParse.TooLong, CustomLyricsParser.parse("a".repeat(CustomLyricsParser.MAX_SOURCE_CHARS + 1)))
    }

    @Test
    fun customKeyIgnoresTranslationVariantButNotTrackIdentity() {
        val custom = lyricsCustomKey("Song", "Artist", 200L, "abc123")

        assertEquals(custom, lyricsCustomKey(" Song ", "Artist", 201L, " abc123 "))
        assertNotEquals(custom, lyricsCustomKey("Song", "Artist", 200L, "xyz789"))
        assertNotEquals(custom, lyricsSelectionKey("Song", "Artist", 200L, "abc123", "en", false))
        assertNotEquals(
            lyricsCustomKey("Song", "Artist", 200L, ""),
            lyricsCustomKey("Song", "Other Artist", 200L, "")
        )
    }

    @Test
    fun previewTimestampFormatsMinutesSecondsAndHundredths() {
        assertEquals("01:02.34", customLyricsTimestamp(62_345L))
        assertEquals("00:00.00", customLyricsTimestamp(-5L))
    }
}

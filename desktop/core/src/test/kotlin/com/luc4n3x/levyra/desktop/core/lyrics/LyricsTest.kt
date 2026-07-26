package com.luc4n3x.levyra.desktop.core.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsTest {

    private val lrc = """
        [ti:Titolo]
        [00:12.50]Prima riga
        [00:15.00]Seconda riga
        [00:18.25][00:40.00]Ritornello
    """.trimIndent()

    @Test
    fun `timestamps are converted to milliseconds`() {
        val lines = LrcParser.parse(lrc)
        assertEquals(4, lines.size)
        assertEquals(12_500L, lines[0].timeMs)
        assertEquals("Prima riga", lines[0].text)
        assertEquals(15_000L, lines[1].timeMs)
    }

    @Test
    fun `repeated timestamps produce one line each and stay ordered`() {
        val lines = LrcParser.parse(lrc)
        assertEquals(listOf(12_500L, 15_000L, 18_250L, 40_000L), lines.map { it.timeMs })
        assertEquals("Ritornello", lines.last().text)
    }

    @Test
    fun `lines without timestamps are ignored`() {
        assertTrue(LrcParser.parse("[ti:Solo metadati]").isEmpty())
        assertTrue(LrcParser.parse("").isEmpty())
    }

    @Test
    fun `plain lyrics keep their order without timings`() {
        val lines = LrcParser.plainLines("Prima\n\nSeconda\n")
        assertEquals(listOf("Prima", "Seconda"), lines.map { it.text })
        assertTrue(lines.all { it.timeMs == 0L })
    }

    @Test
    fun `active line follows the playback position`() {
        val lyrics = Lyrics(
            lines = LrcParser.parse(lrc),
            plainText = "",
            synced = true,
            source = "test"
        )
        assertEquals(-1, lyrics.activeIndex(0L))
        assertEquals(0, lyrics.activeIndex(13_000L))
        assertEquals(1, lyrics.activeIndex(15_000L))
        assertEquals(3, lyrics.activeIndex(120_000L))
    }

    @Test
    fun `unsynced lyrics never report an active line`() {
        val lyrics = Lyrics(
            lines = LrcParser.plainLines("Prima\nSeconda"),
            plainText = "Prima\nSeconda",
            synced = false,
            source = "test"
        )
        assertEquals(-1, lyrics.activeIndex(60_000L))
    }
}

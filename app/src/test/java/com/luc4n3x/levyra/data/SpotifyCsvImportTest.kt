package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyCsvImportTest {

    @Test
    fun exportifyHeaderIsDetected() {
        val csv = """
            "Track URI","Track Name","Artist Name(s)","Album Name","Track Duration (ms)"
            "spotify:track:1","Bohemian Rhapsody","Queen","A Night at the Opera","354000"
        """.trimIndent()

        val entries = parseSpotifyCsv(csv)

        assertEquals(1, entries.size)
        assertEquals("Bohemian Rhapsody", entries.first().title)
        assertEquals("Queen", entries.first().artist)
        assertEquals(354_000L, entries.first().durationMs)
    }

    @Test
    fun quotedCommasAndEscapedQuotesSurviveParsing() {
        val csv = "Track Name,Artist\n\"Hello, Goodbye\",\"The \"\"Fab\"\" Four\"\n"

        val entries = parseSpotifyCsv(csv)

        assertEquals(1, entries.size)
        assertEquals("Hello, Goodbye", entries.first().title)
        assertEquals("The \"Fab\" Four", entries.first().artist)
    }

    @Test
    fun byteOrderMarkAndEmptyRowsAreTolerated() {
        val csv = "﻿Song,Artist\r\n\r\nOne,Metallica\r\n\r\n,\r\nTwo,Nirvana\r\n"

        val entries = parseSpotifyCsv(csv)

        assertEquals(listOf("One", "Two"), entries.map { it.title })
        assertEquals(listOf("Metallica", "Nirvana"), entries.map { it.artist })
    }

    @Test
    fun columnOrderIsIndependentAndOptionalColumnsMayBeMissing() {
        val csv = "Album Name,Artists,Title\nSomething,Daft Punk,Da Funk\n"

        val entries = parseSpotifyCsv(csv)

        assertEquals(1, entries.size)
        assertEquals("Da Funk", entries.first().title)
        assertEquals("Daft Punk", entries.first().artist)
        assertEquals(0L, entries.first().durationMs)
    }

    @Test
    fun rowsWithoutTitleAreSkipped() {
        val csv = "Track Name,Artist\n,Nobody\nReal Song,Somebody\n"

        val entries = parseSpotifyCsv(csv)

        assertEquals(listOf("Real Song"), entries.map { it.title })
    }

    @Test
    fun unknownHeaderYieldsNoEntries() {
        val csv = "column_a,column_b\nvalue,other\n"

        assertTrue(parseSpotifyCsv(csv).isEmpty())
        assertNull(detectSpotifyCsvColumns(listOf("column_a", "column_b")))
    }

    @Test
    fun multipleArtistsCollapseToThePrimaryOne() {
        assertEquals("Drake", normalizeCsvArtist("Drake, Future"))
        assertEquals("Drake", normalizeCsvArtist("Drake feat. Future"))
        assertEquals("Drake", normalizeCsvArtist("Drake ft. Future"))
        assertEquals("Calvin Harris", normalizeCsvArtist("Calvin Harris & Dua Lipa"))
        assertEquals("", normalizeCsvArtist("   "))
    }

    @Test
    fun malformedTrailingQuoteDoesNotCrash() {
        val csv = "Track Name,Artist\n\"Unterminated,Artist Name\n"

        val entries = parseSpotifyCsv(csv)

        assertEquals(1, entries.size)
        assertEquals("Unterminated,Artist Name", entries.first().title)
    }

    @Test
    fun rowLimitIsHonoured() {
        val csv = buildString {
            append("Track Name,Artist\n")
            repeat(50) { append("Song $it,Artist $it\n") }
        }

        assertEquals(10, parseSpotifyCsv(csv, maxRows = 10).size)
    }

    @Test
    fun unmatchedLabelFallsBackToTitleOnly() {
        assertEquals("Song — Artist", spotifyCsvEntryLabel(SpotifyCsvEntry("Song", "Artist", 0L)))
        assertEquals("Song", spotifyCsvEntryLabel(SpotifyCsvEntry("Song", "", 0L)))
    }
}

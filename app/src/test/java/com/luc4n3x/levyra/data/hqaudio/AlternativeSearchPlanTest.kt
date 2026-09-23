package com.luc4n3x.levyra.data.hqaudio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternativeSearchPlanTest {
    @Test
    fun plansDeliberatePassesFromMostToLeastSpecific() {
        val passes = AlternativeSearchPlan.queries(query())
        assertEquals(
            listOf("Blinding Lights The Weeknd After Hours", "Blinding Lights The Weeknd", "blinding lights weeknd"),
            passes
        )
    }

    @Test
    fun singleAlbumNamedLikeTheTitleIsNotRepeated() {
        val passes = AlternativeSearchPlan.queries(query(album = "Blinding Lights"))
        assertEquals("Blinding Lights The Weeknd", passes.first())
        assertTrue(passes.size <= AlternativeSearchPlan.MAX_PASSES)
    }

    @Test
    fun genericAlbumNamesAreNotSearched() {
        val passes = AlternativeSearchPlan.queries(query(album = "YouTube Music"))
        assertEquals("Blinding Lights The Weeknd", passes.first())
    }

    @Test
    fun lastPassDropsSoundtrackDecorationsThatBreakProviderSearch() {
        val passes = AlternativeSearchPlan.queries(
            query(title = "Arabic Kuthu - Halamithi Habibo (From \"Beast\")", artist = "Anirudh Ravichander", album = "Beast")
        )
        assertEquals("arabic kuthu halamithi habibo anirudh ravichander", passes.last())
    }

    @Test
    fun lastPassKeepsVersionWordsSoRemixesAreStillSearchedAsRemixes() {
        val passes = AlternativeSearchPlan.queries(query(title = "Kesariya (Dance Mix)", artist = "Arijit Singh", album = ""))
        assertTrue(passes.all { it.contains("dance mix", ignoreCase = true) })
    }

    @Test
    fun primaryArtistIsUsedForFocusedPasses() {
        val passes = AlternativeSearchPlan.queries(query(title = "One Kiss", artist = "Calvin Harris & Dua Lipa", album = "One Kiss"))
        assertEquals("One Kiss Calvin Harris", passes.first())
        assertEquals("one kiss calvin harris dua lipa", passes.last())
    }
}

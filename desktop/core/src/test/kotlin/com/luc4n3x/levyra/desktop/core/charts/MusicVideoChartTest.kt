package com.luc4n3x.levyra.desktop.core.charts

import com.luc4n3x.levyra.desktop.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MusicVideoChartTest {

    private fun track(id: String, title: String, artist: String) =
        Track(id = id, title = title, artist = artist, videoUrl = "https://www.youtube.com/watch?v=$id")

    @Test
    fun parsesFeedAndSkipsIncompleteEntries() {
        val body = """{"feed":{"results":[{"name":"Song","artistName":"Artist"},{"name":"","artistName":"X"},{"name":"Song","artistName":"Artist"}]}}"""
        assertEquals(listOf(ChartVideo("Song", "Artist")), MusicVideoChart.parse(body, 10))
    }

    @Test
    fun matchesOfficialVideoOverVariants() {
        val video = ChartVideo("Song", "Artist")
        val match = MusicVideoChart.match(video, listOf(track("a", "Song (Live)", "Artist"), track("b", "Song", "Artist"), track("c", "Song", "Other")))
        assertEquals("b", match?.id)
    }

    @Test
    fun rejectsDifferentArtist() {
        assertNull(MusicVideoChart.match(ChartVideo("Song", "Artist"), listOf(track("c", "Song", "Someone"))))
    }
}

package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppleMusicVideoChartTest {
    private fun track(id: String, title: String, artist: String) = Track(
        id = id, title = title, artist = artist, album = "", durationMs = 0L, streamUrl = "", videoUrl = "",
        thumbnailUrl = "", largeThumbnailUrl = "", source = "YouTube Music", moodTags = emptySet(),
        energy = 0, vocal = 0, replayScore = 0, cacheScore = 0, accentStart = 0, accentEnd = 0
    )

    @Test
    fun parsesFeedAndUpgradesArtwork() {
        val body = """{"feed":{"results":[
            {"name":"Waka Waka (This Time for Africa)","artistName":"Shakira","artworkUrl100":"https://is1.mzstatic.com/a/100x100bb.jpg"},
            {"name":"","artistName":"Nobody"},
            {"name":"Waka Waka (This Time for Africa)","artistName":"Shakira","artworkUrl100":"x"}
        ]}}"""
        val videos = AppleMusicVideoChart.parse(body, 20)
        assertEquals(1, videos.size)
        assertEquals("https://is1.mzstatic.com/a/1920x1080bb.jpg", videos.single().artworkUrl)
        assertEquals(emptyList<AppleChartVideo>(), AppleMusicVideoChart.parse("not json", 20))
    }

    @Test
    fun matchesOfficialVideoByTitleAndArtist() {
        val video = AppleChartVideo("Waka Waka (This Time for Africa)", "Shakira & Freshlyground", "")
        val candidates = listOf(
            track("cover", "Waka Waka", "Some Cover Band"),
            track("official", "Waka Waka (This Time for Africa) (The Official 2010 FIFA World Cup Song)", "Shakira"),
            track("other", "Hips Don't Lie", "Shakira")
        )
        assertEquals("official", AppleMusicVideoChart.match(video, candidates)?.id)
    }

    @Test
    fun prefersOriginalOverRemixAndOfficialArtistOverFanChannel() {
        val video = AppleChartVideo("Dai Dai", "Shakira & Burna Boy", "")
        val candidates = listOf(track("remix", "Dai Dai (SPINALL Remix)", "Shakira, Burna Boy"), track("original", "Dai Dai", "Shakira, Burna Boy"))
        assertEquals("original", AppleMusicVideoChart.match(video, candidates)?.id)
        val live = AppleChartVideo("Your Song (Apple Music Live)", "Elton John", "")
        val liveCandidates = listOf(track("fan", "Your Song (Apple Music Hall)", "Elton John HD Collection"), track("official", "Your Song (Live)", "Elton John"))
        assertEquals("official", AppleMusicVideoChart.match(live, liveCandidates)?.id)
    }

    @Test
    fun ignoresAccentsAndRejectsWrongArtist() {
        val video = AppleChartVideo("Despacito", "Luis Fonsi feat. Daddy Yankee", "")
        assertEquals("ok", AppleMusicVideoChart.match(video, listOf(track("ok", "Despacito", "Luis Fonsí")))?.id)
        assertNull(AppleMusicVideoChart.match(video, listOf(track("no", "Despacito", "Random Karaoke"))))
    }
}

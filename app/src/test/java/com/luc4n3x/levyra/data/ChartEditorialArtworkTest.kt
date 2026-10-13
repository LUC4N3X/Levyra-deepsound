package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChartEditorialArtworkTest {
    private val editorial = track("Dai Dai", "Shakira, Burna Boy", "https://i.scdn.co/image/cover", album = "Dai Dai")
    private val index = mapOf(chartArtworkIdentity(editorial) to editorial)

    @Test
    fun youtubeChartTrackTakesTheOfficialEditorialCover() {
        val chart = track("Dai Dai", "Shakira, Burna Boy", "https://i.ytimg.com/vi/x/hqdefault.jpg", album = "YouTube Music Charts")

        val resolved = withEditorialArtwork(chart, index)

        assertEquals("https://i.scdn.co/image/cover", resolved?.thumbnailUrl)
        assertEquals("Dai Dai", resolved?.album)
        assertEquals(chart.id, resolved?.id)
        assertEquals(chart.videoUrl, resolved?.videoUrl)
    }

    @Test
    fun artistSeparatorsAndFeaturesMatchTheSameRecording() {
        assertEquals(chartArtworkIdentity(editorial), chartArtworkIdentity(track("Dai Dai", "Shakira & Burna Boy", "")))
        assertEquals(chartArtworkIdentity(editorial), chartArtworkIdentity(track("Dai Dai (Official Video)", "Shakira", "")))
    }

    @Test
    fun liveOrRemixVersionsKeepTheirOwnCover() {
        assertNull(withEditorialArtwork(track("Dai Dai (Live)", "Shakira", "https://i.ytimg.com/a.jpg"), index))
        assertNull(withEditorialArtwork(track("Dai Dai - Remix", "Shakira", "https://i.ytimg.com/a.jpg"), index))
    }

    @Test
    fun conflictingIsrcIsRejected() {
        val isrcIndex = mapOf(chartArtworkIdentity(editorial) to editorial.copy(isrc = "USAAA2600001"))
        assertNull(withEditorialArtwork(track("Dai Dai", "Shakira", "https://i.ytimg.com/a.jpg").copy(isrc = "USBBB2600002"), isrcIndex))
    }

    @Test
    fun differentRecordingIsLeftForNetworkLookup() {
        assertNull(withEditorialArtwork(track("Dai Dai", "Another Artist", "https://i.ytimg.com/a.jpg"), index))
        assertNull(withEditorialArtwork(track("Other Song", "Shakira", "https://i.ytimg.com/a.jpg"), index))
    }

    private fun track(title: String, artist: String, thumbnail: String, album: String = "") = Track(
        id = "id-$title-$artist",
        title = title,
        artist = artist,
        album = album,
        durationMs = 200_000L,
        streamUrl = "",
        videoUrl = "https://music.youtube.com/watch?v=abc",
        thumbnailUrl = thumbnail,
        largeThumbnailUrl = thumbnail,
        source = "YouTube Music Charts",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}

package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorialArtworkContinuityTest {
    @Test
    fun playerKeepsTheArtworkPresentedInTheTop50Row() {
        val presented = track(
            source = "Levyra Editorial",
            thumbnail = "https://is1-ssl.mzstatic.com/image/thumb/source/600x600bb.jpg",
        )
        val resolved = track(
            source = "YouTube Music",
            thumbnail = "https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg",
        ).copy(id = "abcdefghijk", videoUrl = "https://music.youtube.com/watch?v=abcdefghijk")

        val result = preserveEditorialArtwork(presented, resolved)

        assertEquals(presented.thumbnailUrl, result.thumbnailUrl)
        assertEquals(presented.thumbnailUrl, result.largeThumbnailUrl)
        assertEquals(resolved.videoUrl, result.videoUrl)
    }

    @Test
    fun normalTracksKeepTheResolvedArtwork() {
        val presented = track(source = "Search", thumbnail = "https://example.test/old.jpg")
        val resolved = track(source = "YouTube Music", thumbnail = "https://example.test/new.jpg")

        assertEquals(resolved, preserveEditorialArtwork(presented, resolved))
    }

    private fun track(source: String, thumbnail: String): Track = Track(
        id = "chart-id",
        title = "Titolo",
        artist = "Artista",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = thumbnail,
        largeThumbnailUrl = thumbnail,
        source = source,
        moodTags = setOf("chart"),
        energy = 70,
        vocal = 55,
        replayScore = 90,
        cacheScore = 80,
        accentStart = 0,
        accentEnd = 0,
    )
}

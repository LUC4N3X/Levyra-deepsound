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
            moodTags = setOf("chart"),
        )
        val resolved = track(
            source = "YouTube Music",
            thumbnail = "https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg",
        ).copy(id = "abcdefghijk", videoUrl = "https://music.youtube.com/watch?v=abcdefghijk")

        val result = preserveEditorialArtwork(presented, resolved)

        assertEquals(presented.thumbnailUrl, result.thumbnailUrl)
        assertEquals(presented.thumbnailUrl, result.largeThumbnailUrl)
        assertEquals(resolved.videoUrl, result.videoUrl)

        val recovered = preserveEditorialArtwork(
            result,
            resolved.copy(thumbnailUrl = "https://i.ytimg.com/vi/abcdefghijk/maxresdefault.jpg")
        )
        assertEquals(presented.thumbnailUrl, recovered.thumbnailUrl)
        assertEquals(presented.thumbnailUrl, recovered.largeThumbnailUrl)
    }

    @Test
    fun fallbackChartSourcesAlsoKeepTheArtworkTheUserOpened() {
        val presented = track(
            source = "YouTube Music Charts",
            thumbnail = "https://charts.example.test/red-cover.jpg",
            moodTags = setOf("chart"),
        )
        val resolved = track(
            source = "YouTube Music",
            thumbnail = "https://charts.example.test/white-cover.jpg",
        )

        val result = preserveEditorialArtwork(presented, resolved)

        assertEquals(presented.thumbnailUrl, result.thumbnailUrl)
        assertEquals(presented.thumbnailUrl, result.largeThumbnailUrl)
    }

    @Test
    fun chartSourceNameLocksArtworkEvenAfterTagsAreLostInTransit() {
        val presented = track(
            source = "Apple Music Charts",
            thumbnail = "https://charts.example.test/presented-cover.jpg",
        )
        val resolved = track(
            source = "YouTube Music",
            thumbnail = "https://charts.example.test/resolved-cover.jpg",
        )

        assertEquals(
            presented.thumbnailUrl,
            preserveEditorialArtwork(presented, resolved).thumbnailUrl,
        )
    }

    @Test
    fun normalTracksKeepTheResolvedArtwork() {
        val presented = track(source = "Search", thumbnail = "https://example.test/old.jpg")
        val resolved = track(source = "YouTube Music", thumbnail = "https://example.test/new.jpg")

        assertEquals(resolved, preserveEditorialArtwork(presented, resolved))
    }

    private fun track(
        source: String,
        thumbnail: String,
        moodTags: Set<String> = emptySet(),
    ): Track = Track(
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
        moodTags = moodTags,
        energy = 70,
        vocal = 55,
        replayScore = 90,
        cacheScore = 80,
        accentStart = 0,
        accentEnd = 0,
    )
}

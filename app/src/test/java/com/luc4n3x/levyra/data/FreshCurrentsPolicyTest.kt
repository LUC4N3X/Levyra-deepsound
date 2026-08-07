package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FreshCurrentsPolicyTest {
    @Test
    fun officialEditorialFeedWinsOverFallback() {
        val editorial = track("editorial", "Official release", "Artist")
        val fallback = track("fallback", "Chart fallback", "Artist")
        assertEquals(listOf(editorial), selectFreshCurrentTracks(listOf(editorial), listOf(fallback), 12))
    }

    @Test
    fun samplesCanNeverLeakIntoFreshCurrents() {
        val short = track("short", "Vertical clip", "Artist", source = YOUTUBE_SHORTS_SOURCE, videoType = "SHORTS")
        val clean = track("clean", "Clean song", "Artist")
        val result = selectFreshCurrentTracks(listOf(short), listOf(clean), 12)
        assertEquals(listOf(clean), result)
        assertFalse(result.any(::isYoutubeShortTrack))
    }

    private fun track(id: String, title: String, artist: String, source: String = "Editorial", videoType: String = ""): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = title,
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=abcdefghijk",
        thumbnailUrl = "https://example.test/$id.jpg",
        largeThumbnailUrl = "https://example.test/$id-large.jpg",
        source = source,
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 70,
        cacheScore = 70,
        accentStart = 0,
        accentEnd = 0,
        videoType = videoType
    )
}

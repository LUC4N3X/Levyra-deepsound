package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlaybackSourceIdentityTest {
    @Test
    fun canonicalKeyIgnoresResolvedYoutubeSource() {
        val original = track(
            id = "original123",
            title = "Song Title (Official Audio)",
            artist = "Artist Name",
            videoUrl = "https://www.youtube.com/watch?v=abcdefghijk"
        )
        val fallback = original.copy(
            id = "ZYXWVUTSRQP",
            title = "Song Title",
            videoUrl = "https://youtu.be/ZYXWVUTSRQP",
            source = "fallback"
        )

        assertEquals(PlaybackSourceIdentity.canonicalKey(original), PlaybackSourceIdentity.canonicalKey(fallback))
    }

    @Test
    fun canonicalKeySeparatesDifferentDurations() {
        val short = track(durationMs = 180_000L)
        val long = track(durationMs = 240_000L)

        assertNotEquals(PlaybackSourceIdentity.canonicalKey(short), PlaybackSourceIdentity.canonicalKey(long))
    }

    @Test
    fun extractsWatchShortLiveAndYoutuBeIds() {
        assertEquals("abcdefghijk", PlaybackSourceIdentity.extractYoutubeVideoId("https://www.youtube.com/watch?v=abcdefghijk"))
        assertEquals("abcdefghijk", PlaybackSourceIdentity.extractYoutubeVideoId("https://youtube.com/shorts/abcdefghijk"))
        assertEquals("abcdefghijk", PlaybackSourceIdentity.extractYoutubeVideoId("https://youtube.com/live/abcdefghijk"))
        assertEquals("abcdefghijk", PlaybackSourceIdentity.extractYoutubeVideoId("https://youtu.be/abcdefghijk"))
    }

    private fun track(
        id: String = "track-id",
        title: String = "Song Title",
        artist: String = "Artist Name",
        videoUrl: String = "",
        durationMs: Long = 180_000L
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = "Album",
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = videoUrl,
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}

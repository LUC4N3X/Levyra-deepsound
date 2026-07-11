package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LevyraPlaybackCacheKeyTest {
    @Test
    fun sameItagKeepsStableKeyAcrossSignedUrls() {
        val first = track("https://rr1---sn.example/videoplayback?expire=100&itag=140&sig=one")
        val second = track("https://rr2---sn.example/videoplayback?expire=200&itag=140&sig=two")

        assertEquals(LevyraPlaybackCacheKey.stream(first), LevyraPlaybackCacheKey.stream(second))
    }

    @Test
    fun differentItagsUseDifferentKeys() {
        val audioMp4 = track("https://rr.example/videoplayback?itag=140")
        val audioWebm = track("https://rr.example/videoplayback?itag=251")

        assertNotEquals(LevyraPlaybackCacheKey.stream(audioMp4), LevyraPlaybackCacheKey.stream(audioWebm))
    }

    @Test
    fun refreshedHlsManifestKeepsStableKey() {
        val first = track("https://manifest.googlevideo.com/api/manifest/hls_playlist/expire/100/id/demo.m3u8")
        val second = track("https://manifest.googlevideo.com/api/manifest/hls_playlist/expire/200/id/demo.m3u8")

        assertEquals(LevyraPlaybackCacheKey.stream(first), LevyraPlaybackCacheKey.stream(second))
    }

    private fun track(streamUrl: String): Track = Track(
        id = "video-id",
        title = "Title",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = streamUrl,
        videoUrl = "https://www.youtube.com/watch?v=video-id",
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

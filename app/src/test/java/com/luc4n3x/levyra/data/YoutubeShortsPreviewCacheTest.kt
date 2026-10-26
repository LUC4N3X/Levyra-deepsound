package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubeShortsPreviewCacheTest {
    private val spotifyId = "7lQ8MOhq6IN2w8EYcFNSUk"
    private val preview = "https://p.scdn.co/mp3-preview/35fc7456c892c11867fe1513b92dc44e9664eaba"
    private val canvas = "https://canvaz.scdn.co/upload/artist/example/video/clip.cnvs.mp4"

    @Test
    fun cachedRemoteClipRestoresOriginalAudioWithoutBundledMetadata() {
        val track = track("spotify-clip-cache-before")
        SpotifyClipCanvasRegistry.restore(track.id, canvas, "Feed", spotifyId, preview)
        val cached = shortTrackCacheJson(track).put("id", "spotify-clip-cache-restored")

        val restored = shortTrackFromCacheJson(cached)!!

        assertEquals(SpotifySampleAudio(spotifyId, preview), SpotifyClipCanvasRegistry.audio(restored))
        assertEquals(canvas, SpotifyClipCanvasRegistry.canvasUrl(restored))
        assertEquals("Feed", SpotifyClipCanvasRegistry.feedTitle(restored))
        assertEquals("", restored.streamUrl)
    }

    @Test
    fun tamperedCachedPreviewCannotReplaceTheOriginalSource() {
        val cached = TrackJson.toJson(track("spotify-clip-invalid-cache"))
            .put("spotifyTrackId", spotifyId)
            .put("spotifyAudioPreviewUrl", "https://example.com/audio.mp3")
            .put("spotifyCanvasUrl", canvas)

        val restored = shortTrackFromCacheJson(cached)!!

        assertNull(SpotifyClipCanvasRegistry.audio(restored))
    }

    private fun track(id: String) = Track(
        id = id, title = "Without Me", artist = "Eminem", album = "", durationMs = 0L,
        streamUrl = "", videoUrl = "https://open.spotify.com/track/$spotifyId", thumbnailUrl = "",
        largeThumbnailUrl = "", source = YOUTUBE_SHORTS_SOURCE, moodTags = emptySet(), energy = 50,
        vocal = 50, replayScore = 50, cacheScore = 50, accentStart = 0, accentEnd = 0, videoType = "SHORTS"
    )
}

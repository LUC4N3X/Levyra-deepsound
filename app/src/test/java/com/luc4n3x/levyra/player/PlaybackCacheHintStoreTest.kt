package com.luc4n3x.levyra.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackCacheHintStoreTest {
    @Test
    fun cacheOnlyUriRoundTripsExactKeyAndMimeType() {
        val hint = PlaybackCacheHint(
            sourceVideoId = "Audio123456",
            cacheKey = "levyra:Audio123456:stream-v2:itag-251",
            mimeType = "audio/webm",
            updatedAtMs = 1L
        )

        val uri = playbackCacheOnlyUri(hint)
        val restored = playbackCacheReadSpec(uri)

        assertNotNull(restored)
        assertEquals(hint.cacheKey, restored?.cacheKey)
        assertEquals(hint.mimeType, restored?.mimeType)
        assertTrue(uri.startsWith("levyra-cache://media?"))
        assertFalse(uri.contains("googlevideo.com"))
    }

    @Test
    fun progressiveHttpAudioCanPublishACacheHint() {
        assertTrue(
            shouldRememberPlaybackCacheHint(
                streamUrl = "https://rr.example/videoplayback?itag=251&mime=audio%2Fwebm",
                mimeType = "audio/webm",
                videoMode = false
            )
        )
    }

    @Test
    fun adaptiveLocalAndVideoSourcesNeverPublishAudioCacheHints() {
        assertFalse(
            shouldRememberPlaybackCacheHint(
                streamUrl = "https://rr.example/manifest/hls/playlist.m3u8",
                mimeType = "application/x-mpegURL",
                videoMode = false
            )
        )
        assertFalse(
            shouldRememberPlaybackCacheHint(
                streamUrl = "https://rr.example/manifest.mpd",
                mimeType = "application/dash+xml",
                videoMode = false
            )
        )
        assertFalse(
            shouldRememberPlaybackCacheHint(
                streamUrl = "levyra-sabr://stream",
                mimeType = "audio/mp4",
                videoMode = false
            )
        )
        assertFalse(
            shouldRememberPlaybackCacheHint(
                streamUrl = "content://media/external/audio/42",
                mimeType = null,
                videoMode = false
            )
        )
        assertFalse(
            shouldRememberPlaybackCacheHint(
                streamUrl = "https://rr.example/videoplayback?itag=18&mime=video%2Fmp4",
                mimeType = "video/mp4",
                videoMode = false
            )
        )
        assertFalse(
            shouldRememberPlaybackCacheHint(
                streamUrl = "https://rr.example/videoplayback?itag=18",
                mimeType = "video/mp4",
                videoMode = true
            )
        )
    }

    @Test
    fun cacheOnlyUriIsNeverRecordedAsANewHint() {
        val hint = PlaybackCacheHint(
            sourceVideoId = "Audio123456",
            cacheKey = "levyra:Audio123456:stream-v2:itag-140",
            mimeType = "audio/mp4",
            updatedAtMs = 1L
        )

        assertFalse(
            shouldRememberPlaybackCacheHint(
                streamUrl = playbackCacheOnlyUri(hint),
                mimeType = hint.mimeType,
                videoMode = false
            )
        )
    }

    @Test
    fun lowQualityCacheCannotSatisfyHighOrAutoPlayback() {
        val hint = hintForItag(139)

        assertTrue(isPlaybackCacheHintQualityCompatible(hint, "Low"))
        assertFalse(isPlaybackCacheHintQualityCompatible(hint, "High"))
        assertFalse(isPlaybackCacheHintQualityCompatible(hint, "Auto"))
    }

    @Test
    fun highQualityCacheCanSatisfyHighAndAutoButNotLowPlayback() {
        val hint = hintForItag(251)

        assertFalse(isPlaybackCacheHintQualityCompatible(hint, "Low"))
        assertTrue(isPlaybackCacheHintQualityCompatible(hint, "High"))
        assertTrue(isPlaybackCacheHintQualityCompatible(hint, "Auto"))
    }

    @Test
    fun ambiguousOrUnknownCacheVariantsFallBackToNormalResolution() {
        val mediumHint = hintForItag(140)
        val directHint = PlaybackCacheHint(
            sourceVideoId = "Audio123456",
            cacheKey = "levyra:Audio123456:stream-v2:direct",
            mimeType = "audio/mp4",
            updatedAtMs = 1L
        )

        assertFalse(isPlaybackCacheHintQualityCompatible(mediumHint, "Low"))
        assertFalse(isPlaybackCacheHintQualityCompatible(mediumHint, "High"))
        assertFalse(isPlaybackCacheHintQualityCompatible(mediumHint, "Auto"))
        assertFalse(isPlaybackCacheHintQualityCompatible(directHint, "High"))
    }

    private fun hintForItag(itag: Int) = PlaybackCacheHint(
        sourceVideoId = "Audio123456",
        cacheKey = "levyra:Audio123456:stream-v2:itag-$itag",
        mimeType = "audio/webm",
        updatedAtMs = 1L
    )
}

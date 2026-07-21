package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import com.luc4n3x.levyra.domain.PlaybackStreamDescriptor
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackManifestCodecTest {
    @Test
    fun roundTripPreservesSelectedStreamsAndTechnicalMetadata() {
        val now = System.currentTimeMillis()
        val manifest = ResolvedPlaybackManifest(
            sourceVideoId = "dQw4w9WgXcQ",
            provider = "LevyraExtractor",
            resolvedAtMs = now,
            expiresAtMs = now + 3_600_000L,
            durationMs = 212_000L,
            selectedAudioUrl = "https://r1.googlevideo.com/videoplayback?expire=9999999999&mime=audio%2Fwebm",
            selectedVideoUrl = "https://r1.googlevideo.com/videoplayback?expire=9999999999&mime=video%2Fwebm",
            streams = listOf(
                PlaybackStreamDescriptor(
                    url = "https://r1.googlevideo.com/videoplayback?expire=9999999999&mime=audio%2Fwebm",
                    kind = PlaybackStreamKind.AUDIO,
                    deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
                    container = "webm",
                    mimeType = "audio/webm",
                    codec = "opus",
                    bitrate = 160_000,
                    averageBitrate = 158_000,
                    sampleRate = 48_000,
                    itag = 251,
                    qualityLabel = "AUDIO_QUALITY_HIGH",
                    expiresAtMs = now + 3_600_000L,
                    selected = true
                ),
                PlaybackStreamDescriptor(
                    url = "https://r1.googlevideo.com/videoplayback?expire=9999999999&mime=video%2Fwebm",
                    kind = PlaybackStreamKind.VIDEO,
                    deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
                    container = "webm",
                    mimeType = "video/webm",
                    codec = "vp9",
                    bitrate = 2_000_000,
                    width = 1920,
                    height = 1080,
                    fps = 30,
                    itag = 248,
                    expiresAtMs = now + 3_600_000L,
                    selected = true
                )
            )
        )

        val decoded = PlaybackManifestCodec.decode(PlaybackManifestCodec.encode(manifest))

        requireNotNull(decoded)
        assertEquals(manifest.sourceVideoId, decoded.sourceVideoId)
        assertEquals(manifest.selectedAudioUrl, decoded.selectedAudioUrl)
        assertEquals(manifest.selectedVideoUrl, decoded.selectedVideoUrl)
        assertEquals("opus", decoded.streams.first { it.kind == PlaybackStreamKind.AUDIO }.codec)
        assertEquals(1080, decoded.streams.first { it.kind == PlaybackStreamKind.VIDEO }.height)
        assertTrue(decoded.isFresh(now))
    }

    @Test
    fun freshnessRejectsManifestNearExpiration() {
        val now = System.currentTimeMillis()
        val url = "https://r1.googlevideo.com/videoplayback"
        val manifest = ResolvedPlaybackManifest(
            sourceVideoId = "dQw4w9WgXcQ",
            provider = "YouTube",
            resolvedAtMs = now,
            expiresAtMs = now + 30_000L,
            durationMs = 1L,
            selectedAudioUrl = url,
            selectedVideoUrl = "",
            streams = listOf(
                PlaybackStreamDescriptor(
                    url = url,
                    kind = PlaybackStreamKind.AUDIO,
                    deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
                    expiresAtMs = now + 30_000L,
                    selected = true
                )
            )
        )

        assertFalse(manifest.isFresh(now))
    }
}

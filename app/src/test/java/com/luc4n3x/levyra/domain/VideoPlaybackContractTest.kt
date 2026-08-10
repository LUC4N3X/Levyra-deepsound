package com.luc4n3x.levyra.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlaybackContractTest {
    @Test
    fun audioOnlyManifestIsNotValidForVideoMode() {
        val audioUrl = "https://media.example/audio.m4a"
        val track = track(audioUrl).copy(
            playbackManifest = manifest(
                audioUrl = audioUrl,
                videoUrl = "",
                streams = listOf(
                    PlaybackStreamDescriptor(
                        url = audioUrl,
                        kind = PlaybackStreamKind.AUDIO,
                        deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
                        selected = true
                    )
                )
            )
        )

        assertFalse(track.hasVideoPlaybackPayload())
    }

    @Test
    fun splitVideoIsValidForVideoMode() {
        val track = track("https://media.example/audio.m4a").copy(
            videoStreamUrl = "https://media.example/video.mp4"
        )

        assertTrue(track.hasVideoPlaybackPayload())
    }

    @Test
    fun selectedMuxedManifestIsValidForVideoMode() {
        val muxedUrl = "https://media.example/muxed.mp4"
        val track = track(muxedUrl).copy(
            playbackManifest = manifest(
                audioUrl = muxedUrl,
                videoUrl = "",
                streams = listOf(
                    PlaybackStreamDescriptor(
                        url = muxedUrl,
                        kind = PlaybackStreamKind.MUXED,
                        deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
                        selected = true
                    )
                )
            )
        )

        assertTrue(track.hasVideoPlaybackPayload())
    }

    private fun manifest(
        audioUrl: String,
        videoUrl: String,
        streams: List<PlaybackStreamDescriptor>
    ) = ResolvedPlaybackManifest(
        sourceVideoId = "video123456",
        provider = "test",
        resolvedAtMs = 1L,
        expiresAtMs = 0L,
        durationMs = 180_000L,
        selectedAudioUrl = audioUrl,
        selectedVideoUrl = videoUrl,
        streams = streams
    )

    private fun track(streamUrl: String) = Track(
        id = "video123456",
        title = "Title",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = streamUrl,
        videoUrl = "https://www.youtube.com/watch?v=video123456",
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

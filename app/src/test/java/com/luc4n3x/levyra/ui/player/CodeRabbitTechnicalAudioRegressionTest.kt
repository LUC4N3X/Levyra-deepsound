package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import com.luc4n3x.levyra.domain.PlaybackStreamDescriptor
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.technicalAudioInfoCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodeRabbitTechnicalAudioRegressionTest {
    private val copy = LevyraStrings.forCode("en").technicalAudioInfoCopy()

    @Test
    fun nativeFlacWithoutAlternativeProviderIsReportedAsLossless() {
        val stream = stream(mimeType = "audio/flac", container = "flac", codec = "flac")
        val rows = buildSourceRows(track(stream), stream, copy).toMap()

        assertEquals(copy.yes, rows[copy.lossless])
    }

    @Test
    fun unknownSelectedStreamDoesNotClaimLossy() {
        val stream = stream(mimeType = "", container = "", codec = "")
        val rows = buildSourceRows(track(stream), stream, copy).toMap()

        assertNull(rows[copy.lossless])
    }

    private fun stream(
        mimeType: String,
        container: String,
        codec: String
    ) = PlaybackStreamDescriptor(
        url = "https://example.invalid/audio",
        kind = PlaybackStreamKind.AUDIO,
        deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
        container = container,
        mimeType = mimeType,
        codec = codec,
        selected = true
    )

    private fun track(stream: PlaybackStreamDescriptor): Track {
        val manifest = ResolvedPlaybackManifest(
            sourceVideoId = "video-id",
            provider = "local-test",
            resolvedAtMs = 1L,
            expiresAtMs = 0L,
            durationMs = 180_000L,
            selectedAudioUrl = stream.url,
            selectedVideoUrl = "",
            streams = listOf(stream)
        )
        return Track(
            id = "track-id",
            title = "Title",
            artist = "Artist",
            album = "Album",
            durationMs = 180_000L,
            streamUrl = stream.url,
            videoUrl = "",
            thumbnailUrl = "",
            largeThumbnailUrl = "",
            source = "Local",
            moodTags = emptySet(),
            energy = 50,
            vocal = 50,
            replayScore = 0,
            cacheScore = 0,
            accentStart = 0,
            accentEnd = 0,
            playbackManifest = manifest
        )
    }
}

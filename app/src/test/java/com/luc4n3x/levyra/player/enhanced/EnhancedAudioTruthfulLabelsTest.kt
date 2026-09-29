package com.luc4n3x.levyra.player.enhanced

import com.luc4n3x.levyra.domain.AlternativeAudioSource
import com.luc4n3x.levyra.domain.AlternativeMatchVerdict
import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import com.luc4n3x.levyra.domain.PlaybackStreamDescriptor
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.technicalAudioInfoCopy
import com.luc4n3x.levyra.ui.player.buildEnhancedAudioRows
import com.luc4n3x.levyra.ui.player.buildSourceRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnhancedAudioTruthfulLabelsTest {

    private val copy = LevyraStrings.forCode("en").technicalAudioInfoCopy()

    @Test
    fun jioSaavnAacStream_displaysLosslessNo_andNeverClaimsLossless() {
        val aacStream = PlaybackStreamDescriptor(
            url = "https://example.invalid/audio.mp4",
            kind = PlaybackStreamKind.AUDIO,
            deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
            container = "m4a",
            mimeType = "audio/mp4",
            codec = "mp4a.40.2",
            bitrate = 320_000,
            averageBitrate = 320_000,
            sampleRate = 44_100,
            bitDepth = 16,
            qualityLabel = "320 kbps",
            itag = 140,
            selected = true
        )
        val manifest = ResolvedPlaybackManifest(
            sourceVideoId = "video-id",
            provider = "jiosaavn",
            resolvedAtMs = 1L,
            expiresAtMs = 0L,
            durationMs = 210_000L,
            selectedAudioUrl = aacStream.url,
            selectedVideoUrl = "",
            streams = listOf(aacStream),
            loudnessDb = -7.5f,
            alternativeSource = AlternativeAudioSource(
                providerId = "jiosaavn",
                providerTrackId = "jio-12345",
                bitrateKbps = 320,
                verdict = AlternativeMatchVerdict.EXACT,
                confidence = 96
            )
        )
        val track = Track(
            id = "track-jiosaavn-1",
            title = "JioSaavn Track",
            artist = "Artist",
            album = "Album",
            durationMs = 210_000L,
            streamUrl = aacStream.url,
            videoUrl = "",
            thumbnailUrl = "",
            largeThumbnailUrl = "",
            source = "JioSaavn",
            moodTags = emptySet(),
            energy = 60,
            vocal = 55,
            replayScore = 0,
            cacheScore = 0,
            accentStart = 0,
            accentEnd = 0,
            playbackManifest = manifest
        )

        val sourceRows = buildSourceRows(track, aacStream, copy).toMap()

        assertEquals("jiosaavn", sourceRows[copy.provider])
        assertEquals("320 kbps", sourceRows[copy.bitrate])
        assertEquals(copy.no, sourceRows[copy.lossless])

        val allValues = sourceRows.values.joinToString(" ")
        assertFalse("Must never claim Lossless for lossy stream", allValues.contains("Lossless: Yes"))
        assertFalse("Must never claim Hi-Res for lossy AAC", allValues.contains("Hi-Res source"))
    }

    @Test
    fun enhancedAudioRows_whenActive_displaysActiveAndTruthfulMetadata() {
        val metrics = EnhancedAudioMetrics(
            spectralCutoffHz = 16_500f,
            hfEnergyRatio = 0.22f,
            deficitConfidence = 0.85f,
            adaptiveResidualGain = 0.25f,
            processingTimeUs = 450L,
            bypassed = false,
            bypassReason = null
        )

        val rows = buildEnhancedAudioRows(
            enabled = true,
            metrics = metrics,
            sourceSampleRateHz = 44_100,
            copy = copy
        ).toMap()

        assertEquals(copy.active, rows[copy.status])
        assertEquals("Levyra Band Replication", rows[copy.engine])
        assertEquals("44.1 kHz · 32-bit float", rows[copy.processing])
        assertEquals("85%", rows[copy.confidence])
    }

    @Test
    fun enhancedAudioRows_whenBypassedOnLossless_showsLosslessBypassReason() {
        val metrics = EnhancedAudioMetrics(
            bypassed = true,
            bypassReason = EnhancedAudioBypassReason.ALREADY_LOSSLESS
        )

        val rows = buildEnhancedAudioRows(
            enabled = true,
            metrics = metrics,
            sourceSampleRateHz = 48_000,
            copy = copy
        ).toMap()

        assertEquals(copy.bypassed, rows[copy.status])
        assertEquals(EnhancedAudioBypassReason.ALREADY_LOSSLESS.label, rows[copy.bypassReason])
    }

    @Test
    fun enhancedAudioRows_whenDisabled_showsOff() {
        val rows = buildEnhancedAudioRows(
            enabled = false,
            metrics = EnhancedAudioMetrics(),
            sourceSampleRateHz = 44_100,
            copy = copy
        ).toMap()

        assertEquals(copy.off, rows[copy.status])
    }

    @Test
    fun trueLosslessFlacStream_displaysLosslessYes() {
        val flacStream = PlaybackStreamDescriptor(
            url = "https://example.invalid/audio.flac",
            kind = PlaybackStreamKind.AUDIO,
            deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
            container = "flac",
            mimeType = "audio/flac",
            codec = "flac",
            bitrate = 960_000,
            averageBitrate = 960_000,
            sampleRate = 48_000,
            bitDepth = 24,
            qualityLabel = "Lossless · 960 kbps",
            selected = true
        )
        val manifest = ResolvedPlaybackManifest(
            sourceVideoId = "video-id",
            provider = "flac-provider",
            resolvedAtMs = 1L,
            expiresAtMs = 0L,
            durationMs = 180_000L,
            selectedAudioUrl = flacStream.url,
            selectedVideoUrl = "",
            streams = listOf(flacStream),
            alternativeSource = AlternativeAudioSource(
                providerId = "tidal",
                providerTrackId = "flac-12345",
                bitrateKbps = 960,
                verdict = AlternativeMatchVerdict.EXACT,
                confidence = 100
            )
        )
        val track = Track(
            id = "track-lossless-1",
            title = "FLAC Track",
            artist = "Artist",
            album = "Album",
            durationMs = 180_000L,
            streamUrl = flacStream.url,
            videoUrl = "",
            thumbnailUrl = "",
            largeThumbnailUrl = "",
            source = "Tidal",
            moodTags = emptySet(),
            energy = 50,
            vocal = 50,
            replayScore = 0,
            cacheScore = 0,
            accentStart = 0,
            accentEnd = 0,
            playbackManifest = manifest
        )

        val rows = buildSourceRows(track, flacStream, copy).toMap()

        assertEquals(copy.yes, rows[copy.lossless])
    }
}

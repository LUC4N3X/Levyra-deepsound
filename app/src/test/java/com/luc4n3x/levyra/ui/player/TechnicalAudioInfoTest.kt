package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.domain.AlternativeAudioSource
import com.luc4n3x.levyra.domain.AlternativeMatchVerdict
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import com.luc4n3x.levyra.domain.PlaybackStreamDescriptor
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.ReplayGainMode
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.technicalAudioInfoCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TechnicalAudioInfoTest {
    private val copy = LevyraStrings.forCode("en").technicalAudioInfoCopy()

    @Test
    fun selectedAudioDescriptorPrefersDedicatedAudioOverMuxed() {
        val muxed = stream(kind = PlaybackStreamKind.MUXED, itag = 18)
        val audio = stream(kind = PlaybackStreamKind.AUDIO, itag = 251)
        val selected = selectedAudioDescriptor(track(listOf(muxed, audio)))

        assertEquals(251, selected?.itag)
    }

    @Test
    fun sourceRowsExposeUsefulMetadataWithoutUrls() {
        val audio = stream(
            kind = PlaybackStreamKind.AUDIO,
            itag = 251,
            bitrate = 128_000,
            averageBitrate = 160_000,
            sampleRate = 48_000,
            bitDepth = 24,
            qualityLabel = "160 kbps"
        )
        val manifest = manifest(
            streams = listOf(audio),
            alternative = AlternativeAudioSource(
                providerId = "jiosaavn",
                providerTrackId = "private-provider-id",
                bitrateKbps = 320,
                verdict = AlternativeMatchVerdict.EXACT,
                confidence = 98
            )
        )
        val rows = buildSourceRows(track(listOf(audio), manifest), audio, copy).toMap()

        assertEquals("jiosaavn", rows[copy.provider])
        assertEquals("160 kbps", rows[copy.bitrate])
        assertEquals("48 kHz", rows[copy.sampleRate])
        assertEquals("24-bit", rows[copy.bitDepth])
        assertEquals("itag 251", rows[copy.streamId])
        assertTrue(rows[copy.verifiedSource].orEmpty().contains("EXACT"))
        assertTrue(rows.values.none { it.contains("private-provider-id") })
        assertTrue(rows.values.none { it.contains("https://") })
    }

    @Test
    fun outputPathDistinguishesNativeRequestAndFallback() {
        val enabled = LevyraAudioSettings(aaudioOutputEnabled = true)
        val disabled = LevyraAudioSettings(aaudioOutputEnabled = false)

        assertTrue(technicalOutputPath(enabled, copy, nativeSupported = true).startsWith("AAudio / Oboe"))
        assertTrue(technicalOutputPath(enabled, copy, nativeSupported = false).contains(copy.fallback))
        assertEquals("AudioTrack", technicalOutputPath(disabled, copy, nativeSupported = true))
    }

    @Test
    fun processingSummaryReportsOnlyEnabledStages() {
        val settings = LevyraAudioSettings(
            equalizerEnabled = true,
            limiterEnabled = true,
            replayGainMode = ReplayGainMode.TRACK,
            replayGainEnabled = true,
            virtualizer = 20,
            preampDb = 1.5f
        )

        val label = buildProcessingLabel(settings, audioNormalization = true, copy)

        assertTrue(label.contains(copy.normalization))
        assertTrue(label.contains(copy.equalizer))
        assertTrue(label.contains("ReplayGain TRACK"))
        assertTrue(label.contains(copy.limiter))
        assertTrue(label.contains("${copy.virtualizer} 20%"))
        assertTrue(label.contains("+1.5 dB"))
    }

    @Test
    fun technicalFormattingIsCompactAndStable() {
        assertEquals("48 kHz", formatTechnicalSampleRate(48_000))
        assertEquals("44.1 kHz", formatTechnicalSampleRate(44_100))
        assertEquals("1.0", formatTechnicalChannels(1))
        assertEquals("2.0", formatTechnicalChannels(2))
        assertEquals("5.1", formatTechnicalChannels(6))
        assertEquals("7.1", formatTechnicalChannels(8))
        assertEquals("4 ch", formatTechnicalChannels(4))
    }

    private fun stream(
        kind: PlaybackStreamKind,
        itag: Int,
        bitrate: Int = 0,
        averageBitrate: Int = 0,
        sampleRate: Int = 0,
        bitDepth: Int = 0,
        qualityLabel: String = ""
    ) = PlaybackStreamDescriptor(
        url = "https://example.invalid/audio",
        kind = kind,
        deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
        container = "webm",
        mimeType = "audio/webm",
        codec = "opus",
        bitrate = bitrate,
        averageBitrate = averageBitrate,
        sampleRate = sampleRate,
        bitDepth = bitDepth,
        itag = itag,
        qualityLabel = qualityLabel,
        selected = true
    )

    private fun manifest(
        streams: List<PlaybackStreamDescriptor>,
        alternative: AlternativeAudioSource? = null
    ) = ResolvedPlaybackManifest(
        sourceVideoId = "video-id",
        provider = "youtube",
        resolvedAtMs = 1L,
        expiresAtMs = 0L,
        durationMs = 180_000L,
        selectedAudioUrl = streams.firstOrNull()?.url.orEmpty(),
        selectedVideoUrl = "",
        streams = streams,
        loudnessDb = -5.2f,
        alternativeSource = alternative
    )

    private fun track(
        streams: List<PlaybackStreamDescriptor>,
        manifest: ResolvedPlaybackManifest = manifest(streams)
    ) = Track(
        id = "track-id",
        title = "Title",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "https://example.invalid/audio",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "YouTube",
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

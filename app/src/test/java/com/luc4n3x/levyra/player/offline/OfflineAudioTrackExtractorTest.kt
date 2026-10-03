package com.luc4n3x.levyra.player.offline

import android.media.MediaCodec
import android.media.MediaExtractor
import androidx.media3.common.MimeTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineAudioTrackExtractorTest {

    @Test
    fun directMp4RemuxAcceptsAacAndMp4AudioMimes() {
        assertTrue(isDirectMp4RemuxAudioMime(MimeTypes.AUDIO_AAC))
        assertTrue(isDirectMp4RemuxAudioMime("audio/mp4a-latm"))
        assertTrue(isDirectMp4RemuxAudioMime("audio/mp4"))
        assertTrue(isDirectMp4RemuxAudioMime("audio/m4a"))
        assertTrue(isDirectMp4RemuxAudioMime("audio/x-m4a"))
        assertFalse(isDirectMp4RemuxAudioMime("audio/opus"))
        assertFalse(isDirectMp4RemuxAudioMime("video/avc"))
    }

    @Test
    fun muxedAvcAndAacSelectsDirectRemuxOnAudioTrack() {
        val tracks = listOf("video/avc", "audio/mp4a-latm")
        assertEquals(1, selectPreferredAudioTrackIndex(tracks))
        assertEquals(
            OfflineAudioExtractionStrategy.DIRECT_MP4_REMUX,
            selectOfflineAudioExtractionStrategy(tracks)
        )
    }

    @Test
    fun nonAacAudioTrackSelectsControlledTransformerFallback() {
        val tracks = listOf("video/avc", "audio/opus")
        assertEquals(1, selectPreferredAudioTrackIndex(tracks))
        assertEquals(
            OfflineAudioExtractionStrategy.TRANSFORMER_AAC_FALLBACK,
            selectOfflineAudioExtractionStrategy(tracks)
        )
    }

    @Test
    fun containerWithoutAudioTrackSelectsNoAudioTrackStrategy() {
        val tracks = listOf("video/avc", "application/x-subrip")
        assertEquals(-1, selectPreferredAudioTrackIndex(tracks))
        assertEquals(
            OfflineAudioExtractionStrategy.NO_AUDIO_TRACK,
            selectOfflineAudioExtractionStrategy(tracks)
        )
        assertEquals(
            OfflineAudioExtractionStrategy.NO_AUDIO_TRACK,
            selectOfflineAudioExtractionStrategy(emptyList())
        )
    }

    @Test
    fun preferredAudioTrackPrioritizesDirectRemuxOverOtherAudioTracks() {
        val tracks = listOf("video/avc", "audio/opus", "audio/mp4a-latm")
        assertEquals(2, selectPreferredAudioTrackIndex(tracks))
        assertEquals(
            OfflineAudioExtractionStrategy.DIRECT_MP4_REMUX,
            selectOfflineAudioExtractionStrategy(tracks)
        )
    }

    @Test
    fun sampleBufferSizeUsesLargerOfDefaultAndFormatMaxInputSize() {
        assertEquals(512 * 1024, resolveExtractorSampleBufferSize(606))
        assertEquals(512 * 1024, resolveExtractorSampleBufferSize(-1))
        assertEquals(1024 * 1024, resolveExtractorSampleBufferSize(1024 * 1024))
    }

    @Test
    fun presentationTimestampsAreZeroBasedAndMonotonic() {
        val first = normalizeExtractorPresentationTimeUs(
            sampleTimeUs = 50_000L,
            firstPresentationTimeUs = 50_000L,
            lastAdjustedTimeUs = -1L
        )
        val second = normalizeExtractorPresentationTimeUs(
            sampleTimeUs = 73_219L,
            firstPresentationTimeUs = 50_000L,
            lastAdjustedTimeUs = first
        )
        val outOfOrder = normalizeExtractorPresentationTimeUs(
            sampleTimeUs = 60_000L,
            firstPresentationTimeUs = 50_000L,
            lastAdjustedTimeUs = second
        )
        assertEquals(0L, first)
        assertEquals(23_219L, second)
        assertEquals(23_219L, outOfOrder)
    }

    @Test
    fun extractorSyncSampleFlagMapsToCodecKeyFrameFlagOnly() {
        assertEquals(
            MediaCodec.BUFFER_FLAG_KEY_FRAME,
            mapExtractorSampleFlagsToCodecFlags(MediaExtractor.SAMPLE_FLAG_SYNC)
        )
        assertEquals(
            0,
            mapExtractorSampleFlagsToCodecFlags(MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME)
        )
    }
}

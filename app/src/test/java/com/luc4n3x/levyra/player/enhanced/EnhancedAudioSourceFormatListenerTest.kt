package com.luc4n3x.levyra.player.enhanced

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.player.truePeakLimiterRequired
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnhancedAudioSourceFormatListenerTest {
    private val eventTime = AnalyticsListener.EventTime(0L, Timeline.EMPTY, 0, null, 0L, Timeline.EMPTY, 0, null, 0L, 0L)

    @Test
    fun onlyTrulyLosslessDecoderInputsAreLossless() {
        assertTrue(isLosslessAudioMimeType(MimeTypes.AUDIO_FLAC))
        assertTrue(isLosslessAudioMimeType(MimeTypes.AUDIO_ALAC))
        assertTrue(isLosslessAudioMimeType(MimeTypes.AUDIO_RAW))
        assertFalse(isLosslessAudioMimeType(MimeTypes.AUDIO_AAC))
        assertFalse(isLosslessAudioMimeType(MimeTypes.AUDIO_OPUS))
        assertFalse(isLosslessAudioMimeType(MimeTypes.AUDIO_MPEG))
        assertFalse(isLosslessAudioMimeType(null))
    }

    @Test
    fun decodedFlacInputBypassesEnhancedAudioBitIdentically() {
        val processor = EnhancedAudioProcessor().apply {
            configure(AudioFormat(44_100, 2, C.ENCODING_PCM_16BIT))
            userEnabled = true
        }
        EnhancedAudioSourceFormatListener(processor, "test")
            .onAudioInputFormatChanged(eventTime, audioFormat(MimeTypes.AUDIO_FLAC), null)

        val input = ByteBuffer.allocateDirect(4_096).order(ByteOrder.LITTLE_ENDIAN)
        var sample = 0
        while (input.hasRemaining()) {
            input.putShort((sample * 7_919 % 65_536 - 32_768).toShort())
            sample++
        }
        input.flip()
        val expected = ByteArray(input.remaining()).also { input.duplicate().get(it) }

        processor.queueInput(input)
        val output = processor.output
        val actual = ByteArray(output.remaining()).also { output.get(it) }

        assertTrue(processor.isLosslessSource)
        assertEquals(EnhancedAudioBypassReason.ALREADY_LOSSLESS, processor.metricsState.value.bypassReason)
        assertTrue(expected.contentEquals(actual))
    }

    @Test
    fun lossyInputAfterFlacClearsTheLosslessBypass() {
        val processor = EnhancedAudioProcessor()
        val listener = EnhancedAudioSourceFormatListener(processor, "test")

        listener.onAudioInputFormatChanged(eventTime, audioFormat(MimeTypes.AUDIO_FLAC), null)
        assertTrue(processor.isLosslessSource)
        listener.onAudioInputFormatChanged(eventTime, audioFormat(MimeTypes.AUDIO_AAC), null)
        assertFalse(processor.isLosslessSource)
    }

    @Test
    fun sourceFormatChanges_notifyOnlyEffectiveLosslessChanges() {
        val processor = EnhancedAudioProcessor()
        val changes = mutableListOf<Boolean>()
        val listener = EnhancedAudioSourceFormatListener(processor, "test", changes::add)

        listener.onAudioInputFormatChanged(eventTime, audioFormat(MimeTypes.AUDIO_AAC), null)
        listener.onAudioInputFormatChanged(eventTime, audioFormat(MimeTypes.AUDIO_FLAC), null)
        listener.onAudioInputFormatChanged(eventTime, audioFormat(MimeTypes.AUDIO_FLAC), null)
        listener.onMediaItemTransition(eventTime, null, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)

        assertEquals(listOf(true, false), changes)
        assertFalse(processor.isLosslessSource)
    }

    @Test
    fun repeatTransition_preservesKnownLosslessSource() {
        val processor = EnhancedAudioProcessor()
        val changes = mutableListOf<Boolean>()
        val listener = EnhancedAudioSourceFormatListener(processor, "test", changes::add)
        listener.onAudioInputFormatChanged(eventTime, audioFormat(MimeTypes.AUDIO_FLAC), null)

        listener.onMediaItemTransition(eventTime, null, Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT)

        assertTrue(processor.isLosslessSource)
        assertEquals(listOf(true), changes)
    }

    @Test
    fun enhancedAudioAloneDoesNotEnableTheOutputLimiter() {
        val enhancedOnly = LevyraAudioSettings(enhancedAudioEnabled = true, limiterEnabled = true)
        assertFalse(truePeakLimiterRequired(enhancedOnly, parametricActive = false, audioNormalization = false))
        assertTrue(truePeakLimiterRequired(enhancedOnly.copy(equalizerEnabled = true), false, false))
        assertTrue(truePeakLimiterRequired(enhancedOnly, false, audioNormalization = true))
        assertFalse(truePeakLimiterRequired(enhancedOnly.copy(equalizerEnabled = true, limiterEnabled = false), false, false))
    }

    private fun audioFormat(mimeType: String): Format = Format.Builder()
        .setSampleMimeType(mimeType)
        .setSampleRate(44_100)
        .setChannelCount(2)
        .build()
}

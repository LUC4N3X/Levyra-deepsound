package com.luc4n3x.levyra.player

import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoDecoderFallbackTest {

    private val avcDecoders = listOf(EmulatorDecoder, SoftwareDecoder)
    private val fallback = VideoDecoderFallback { mimeType ->
        if (mimeType == AVC) avcDecoders else listOf(AudioDecoder)
    }

    @Test
    fun `failed video decoder is skipped once an alternative exists`() {
        assertTrue(fallback.redirect(EmulatorDecoder, AVC, DECODING_FAILED, 10L))
        val selected = fallback.selector(baseSelector()).getDecoderInfos(AVC, false, false).map { it.name }
        assertEquals(listOf(SoftwareDecoder), selected)
    }

    @Test
    fun `same decoder is never redirected twice and the last decoder is kept`() {
        assertTrue(fallback.redirect(EmulatorDecoder, AVC, DECODING_FAILED, 10L))
        assertFalse(fallback.redirect(EmulatorDecoder, AVC, DECODING_FAILED, 20L))
        assertFalse(fallback.redirect(SoftwareDecoder, AVC, DECODING_FAILED, 30L))
        val selected = fallback.selector(baseSelector()).getDecoderInfos(AVC, false, false).map { it.name }
        assertEquals(listOf(SoftwareDecoder), selected)
    }

    @Test
    fun `a new track gets every decoder back`() {
        assertTrue(fallback.redirect(EmulatorDecoder, AVC, DECODING_FAILED, 10L))
        fallback.reset()
        val selected = fallback.selector(baseSelector()).getDecoderInfos(AVC, false, false).map { it.name }
        assertEquals(avcDecoders, selected)
        assertTrue(fallback.redirect(EmulatorDecoder, AVC, DECODING_FAILED, 20L))
    }

    @Test
    fun `audio decoders are left to the audio fallback`() {
        assertFalse(fallback.redirect(AudioDecoder, OPUS, DECODING_FAILED, 10L))
        val selected = fallback.selector(baseSelector()).getDecoderInfos(OPUS, false, false).map { it.name }
        assertEquals(listOf(AudioDecoder), selected)
    }

    @Test
    fun `only the redirected error is hidden from the controller once`() {
        assertTrue(fallback.redirect(EmulatorDecoder, AVC, DECODING_FAILED, 10L))
        assertFalse(fallback.consumeRecoveredControllerError(DECODING_FAILED, 11L))
        assertTrue(fallback.consumeRecoveredControllerError(DECODING_FAILED, 10L))
        assertFalse(fallback.consumeRecoveredControllerError(DECODING_FAILED, 10L))
    }

    private fun baseSelector() = MediaCodecSelector { mimeType, _, _ ->
        val names = if (mimeType == AVC) avcDecoders else listOf(AudioDecoder)
        names.map { name ->
            MediaCodecInfo.newInstance(name, mimeType, mimeType, null, false, true, false, false, false)
        }
    }

    private companion object {
        const val AVC = "video/avc"
        const val OPUS = "audio/opus"
        const val EmulatorDecoder = "c2.goldfish.h264.decoder"
        const val SoftwareDecoder = "c2.android.avc.decoder"
        const val AudioDecoder = "c2.android.opus.decoder"
        const val DECODING_FAILED = PlaybackException.ERROR_CODE_DECODING_FAILED
    }
}

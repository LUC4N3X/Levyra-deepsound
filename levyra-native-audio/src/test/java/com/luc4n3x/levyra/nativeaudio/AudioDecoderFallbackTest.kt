package com.luc4n3x.levyra.nativeaudio

import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioDecoderFallbackTest {
    @Test
    fun platformAudioDecoderFailuresSelectTheFailedMimeType() {
        listOf(
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
        ).forEach { errorCode ->
            assertEquals(
                MimeTypes.AUDIO_OPUS,
                AudioDecoderFallbackPolicy.failedPlatformAudioMimeType(
                    errorCode,
                    isRendererError = true,
                    rendererName = "MediaCodecAudioRenderer",
                    sampleMimeType = MimeTypes.AUDIO_OPUS
                )
            )
        }
    }

    @Test
    fun ignoresNetworkErrorsVideoFormatsAndFfmpegFailures() {
        assertNull(
            AudioDecoderFallbackPolicy.failedPlatformAudioMimeType(
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                isRendererError = true,
                rendererName = "MediaCodecAudioRenderer",
                sampleMimeType = MimeTypes.AUDIO_AAC
            )
        )
        assertNull(
            AudioDecoderFallbackPolicy.failedPlatformAudioMimeType(
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                isRendererError = true,
                rendererName = "MediaCodecVideoRenderer",
                sampleMimeType = MimeTypes.VIDEO_H264
            )
        )
        assertNull(
            AudioDecoderFallbackPolicy.failedPlatformAudioMimeType(
                PlaybackException.ERROR_CODE_DECODING_FAILED,
                isRendererError = true,
                rendererName = "FfmpegAudioRenderer",
                sampleMimeType = MimeTypes.AUDIO_FLAC
            )
        )
        assertNull(
            AudioDecoderFallbackPolicy.failedPlatformAudioMimeType(
                PlaybackException.ERROR_CODE_DECODING_FAILED,
                isRendererError = false,
                rendererName = null,
                sampleMimeType = MimeTypes.AUDIO_FLAC
            )
        )
    }

    @Test
    fun registryRedirectsOnlySupportedAudioMimeTypesOnce() {
        val registry = AudioDecoderFallbackRegistry { it == MimeTypes.AUDIO_E_AC3 || it == MimeTypes.VIDEO_H264 }
        assertFalse(registry.redirect(MimeTypes.AUDIO_OPUS))
        assertFalse(registry.redirect(MimeTypes.VIDEO_H264))
        assertTrue(registry.redirect(MimeTypes.AUDIO_E_AC3))
        assertFalse(registry.redirect(MimeTypes.AUDIO_E_AC3))
        assertTrue(registry.isRedirected(MimeTypes.AUDIO_E_AC3))
        assertFalse(registry.isRedirected(MimeTypes.AUDIO_OPUS))
    }

    @Test
    fun selectorHidesPlatformDecodersOnlyForRedirectedMimeTypes() {
        val registry = AudioDecoderFallbackRegistry { true }
        registry.redirect(MimeTypes.AUDIO_AC3)
        var delegateQueries = 0
        val selector = FallbackAwareMediaCodecSelector(registry) { _, _, _ ->
            delegateQueries++
            emptyList()
        }
        assertTrue(selector.getDecoderInfos(MimeTypes.AUDIO_AC3, false, false).isEmpty())
        assertEquals(0, delegateQueries)
        selector.getDecoderInfos(MimeTypes.AUDIO_AAC, false, false)
        assertEquals(1, delegateQueries)
    }
}

package com.luc4n3x.levyra.nativeaudio

import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.decoder.ffmpeg.FfmpegLibrary
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import java.util.concurrent.ConcurrentHashMap

object FfmpegAudioDecoders {
    val isAvailable: Boolean by lazy {
        BuildConfig.NATIVE_AUDIO_BUNDLED && runCatching { FfmpegLibrary.isAvailable() }.getOrDefault(false)
    }

    fun supports(mimeType: String): Boolean =
        isAvailable && runCatching { FfmpegLibrary.supportsFormat(mimeType) }.getOrDefault(false)
}

class AudioDecoderFallbackRegistry(
    private val fallbackSupports: (String) -> Boolean
) {
    private val redirectedMimeTypes: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun isRedirected(mimeType: String): Boolean = mimeType in redirectedMimeTypes

    fun redirect(mimeType: String): Boolean {
        if (!MimeTypes.isAudio(mimeType) || mimeType in redirectedMimeTypes) return false
        if (!fallbackSupports(mimeType)) return false
        return redirectedMimeTypes.add(mimeType)
    }
}

class FallbackAwareMediaCodecSelector(
    private val registry: AudioDecoderFallbackRegistry,
    private val delegate: MediaCodecSelector = MediaCodecSelector.DEFAULT
) : MediaCodecSelector {
    override fun getDecoderInfos(
        mimeType: String,
        requiresSecureDecoder: Boolean,
        requiresTunnelingDecoder: Boolean
    ): List<MediaCodecInfo> =
        if (registry.isRedirected(mimeType)) {
            emptyList()
        } else {
            delegate.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
        }
}

object AudioDecoderFallbackPolicy {
    private const val FFMPEG_RENDERER_MARKER = "Ffmpeg"

    private val decoderErrorCodes = setOf(
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
    )

    fun failedPlatformAudioMimeType(error: PlaybackException): String? {
        val playbackError = error as? ExoPlaybackException ?: return null
        return failedPlatformAudioMimeType(
            errorCode = playbackError.errorCode,
            isRendererError = playbackError.type == ExoPlaybackException.TYPE_RENDERER,
            rendererName = playbackError.rendererName,
            sampleMimeType = playbackError.rendererFormat?.sampleMimeType
        )
    }

    fun failedPlatformAudioMimeType(
        errorCode: Int,
        isRendererError: Boolean,
        rendererName: String?,
        sampleMimeType: String?
    ): String? {
        if (!isRendererError || errorCode !in decoderErrorCodes) return null
        if (rendererName?.contains(FFMPEG_RENDERER_MARKER) == true) return null
        return sampleMimeType?.takeIf(MimeTypes::isAudio)
    }
}

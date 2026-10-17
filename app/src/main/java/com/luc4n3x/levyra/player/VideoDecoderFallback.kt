package com.luc4n3x.levyra.player

import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.mediacodec.MediaCodecDecoderException
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

@UnstableApi
internal class VideoDecoderFallback(
    private val availableDecoders: (String) -> List<String>
) {
    private val lock = Any()
    private val failedDecoders = mutableSetOf<String>()
    private var pendingErrorCode = 0
    private var pendingErrorTimestampMs = 0L

    fun selector(base: MediaCodecSelector): MediaCodecSelector =
        MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
            val decoders = base.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
            if (!MimeTypes.isVideo(mimeType)) return@MediaCodecSelector decoders
            val failed = synchronized(lock) { failedDecoders.toSet() }
            decoders.filterNot { it.name in failed }.ifEmpty { decoders }
        }

    fun redirect(error: PlaybackException): Boolean {
        val codec = failedVideoCodec(error) ?: return false
        return redirect(codec.name, codec.mimeType, error.errorCode, error.timestampMs)
    }

    fun consumeRecoveredControllerError(error: PlaybackException): Boolean =
        consumeRecoveredControllerError(error.errorCode, error.timestampMs)

    internal fun redirect(decoderName: String, mimeType: String, errorCode: Int, timestampMs: Long): Boolean {
        if (decoderName.isBlank() || !MimeTypes.isVideo(mimeType)) return false
        synchronized(lock) {
            if (decoderName in failedDecoders) return false
            val alternatives = availableDecoders(mimeType).filterNot { it == decoderName || it in failedDecoders }
            if (alternatives.isEmpty()) return false
            failedDecoders += decoderName
            pendingErrorCode = errorCode
            pendingErrorTimestampMs = timestampMs
            return true
        }
    }

    fun reset() {
        synchronized(lock) { failedDecoders.clear() }
    }

    internal fun consumeRecoveredControllerError(errorCode: Int, timestampMs: Long): Boolean = synchronized(lock) {
        val matches = pendingErrorCode != 0 &&
            pendingErrorCode == errorCode &&
            pendingErrorTimestampMs == timestampMs
        if (matches) pendingErrorCode = 0
        matches
    }

    private fun failedVideoCodec(error: PlaybackException): MediaCodecInfo? {
        val rendererError = error as? ExoPlaybackException ?: return null
        if (rendererError.type != ExoPlaybackException.TYPE_RENDERER) return null
        return (rendererError.cause as? MediaCodecDecoderException)?.codecInfo
    }

    companion object {
        val Shared = VideoDecoderFallback { mimeType ->
            runCatching {
                MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, false, false).map { it.name }
            }.getOrDefault(emptyList())
        }
    }
}

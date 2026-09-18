package com.luc4n3x.levyra.player

import android.content.Context
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import com.luc4n3x.levyra.nativeaudio.AudioDecoderFallbackPolicy
import com.luc4n3x.levyra.nativeaudio.AudioDecoderFallbackRegistry
import com.luc4n3x.levyra.nativeaudio.FallbackAwareMediaCodecSelector
import com.luc4n3x.levyra.nativeaudio.FfmpegAudioDecoders
import com.luc4n3x.levyra.nativeaudio.OboeAudioOutputProvider

@UnstableApi
object NativeAudioIntegration {
    const val EXTENSION_RENDERER_MODE: Int = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON

    private val decoderFallbackRegistry = AudioDecoderFallbackRegistry(FfmpegAudioDecoders::supports)

    val mediaCodecSelector: MediaCodecSelector = FallbackAwareMediaCodecSelector(decoderFallbackRegistry)

    private val pendingControllerErrorLock = Any()
    private var pendingControllerErrorCode = 0
    private var pendingControllerErrorTimestampMs = 0L

    fun audioOutputProvider(context: Context, aaudioOutputRequested: () -> Boolean): AudioOutputProvider? =
        OboeAudioOutputProvider(context, aaudioOutputRequested)

    fun isAaudioOutputSupported(): Boolean = OboeAudioOutputProvider.isOboeSupported()

    fun redirectFailedPlatformDecoder(error: PlaybackException): String? =
        redirectFailedPlatformDecoder(error, rememberControllerError = true)

    fun redirectFailedBackgroundDecoder(error: PlaybackException): String? =
        redirectFailedPlatformDecoder(error, rememberControllerError = false)

    private fun redirectFailedPlatformDecoder(
        error: PlaybackException,
        rememberControllerError: Boolean
    ): String? {
        val mimeType = AudioDecoderFallbackPolicy.failedPlatformAudioMimeType(error) ?: return null
        if (!decoderFallbackRegistry.redirect(mimeType)) return null
        if (rememberControllerError) {
            synchronized(pendingControllerErrorLock) {
                pendingControllerErrorCode = error.errorCode
                pendingControllerErrorTimestampMs = error.timestampMs
            }
        }
        return mimeType
    }

    fun consumeRecoveredControllerError(error: PlaybackException): Boolean = synchronized(pendingControllerErrorLock) {
        val matches = pendingControllerErrorCode != 0 &&
            pendingControllerErrorCode == error.errorCode &&
            pendingControllerErrorTimestampMs == error.timestampMs
        if (matches) pendingControllerErrorCode = 0
        matches
    }
}

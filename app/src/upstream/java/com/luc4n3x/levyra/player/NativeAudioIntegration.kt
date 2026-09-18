package com.luc4n3x.levyra.player

import android.content.Context
import android.os.SystemClock
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

    private const val CONTROLLER_ERROR_WINDOW_MS = 5_000L

    private val decoderFallbackRegistry = AudioDecoderFallbackRegistry(FfmpegAudioDecoders::supports)

    val mediaCodecSelector: MediaCodecSelector = FallbackAwareMediaCodecSelector(decoderFallbackRegistry)

    private val pendingControllerErrorLock = Any()
    private var pendingControllerErrorCode = 0
    private var pendingControllerErrorElapsedMs = 0L

    fun audioOutputProvider(context: Context, aaudioOutputRequested: () -> Boolean): AudioOutputProvider? =
        OboeAudioOutputProvider(context, aaudioOutputRequested)

    fun isAaudioOutputSupported(): Boolean = OboeAudioOutputProvider.isOboeSupported()

    fun redirectFailedPlatformDecoder(error: PlaybackException): String? {
        val mimeType = AudioDecoderFallbackPolicy.failedPlatformAudioMimeType(error) ?: return null
        if (!decoderFallbackRegistry.redirect(mimeType)) return null
        synchronized(pendingControllerErrorLock) {
            pendingControllerErrorCode = error.errorCode
            pendingControllerErrorElapsedMs = SystemClock.elapsedRealtime()
        }
        return mimeType
    }

    fun consumeRecoveredControllerError(errorCode: Int): Boolean = synchronized(pendingControllerErrorLock) {
        val matches = pendingControllerErrorCode != 0 &&
            pendingControllerErrorCode == errorCode &&
            SystemClock.elapsedRealtime() - pendingControllerErrorElapsedMs <= CONTROLLER_ERROR_WINDOW_MS
        pendingControllerErrorCode = 0
        matches
    }
}

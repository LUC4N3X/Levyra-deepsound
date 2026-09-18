package com.luc4n3x.levyra.player

import android.content.Context
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

@UnstableApi
object NativeAudioIntegration {
    const val EXTENSION_RENDERER_MODE: Int = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF

    val mediaCodecSelector: MediaCodecSelector = MediaCodecSelector.DEFAULT

    fun audioOutputProvider(context: Context, aaudioOutputRequested: () -> Boolean): AudioOutputProvider? = null

    fun isAaudioOutputSupported(): Boolean = false

    fun redirectFailedPlatformDecoder(error: PlaybackException): String? = null

    fun redirectFailedBackgroundDecoder(error: PlaybackException): String? = null

    fun consumeRecoveredControllerError(error: PlaybackException): Boolean = false
}

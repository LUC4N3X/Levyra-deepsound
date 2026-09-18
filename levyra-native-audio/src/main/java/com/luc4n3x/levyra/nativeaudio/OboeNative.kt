package com.luc4n3x.levyra.nativeaudio

import java.nio.ByteBuffer

internal object OboeNative {
    private const val LIBRARY_NAME = "levyra_oboe"

    val isLoaded: Boolean by lazy {
        BuildConfig.NATIVE_AUDIO_BUNDLED && runCatching { System.loadLibrary(LIBRARY_NAME) }.isSuccess
    }

    @JvmStatic
    external fun nativeOpen(
        sampleRate: Int,
        channelCount: Int,
        sessionId: Int,
        deviceId: Int,
        ringCapacityFrames: Int,
        usage: Int,
        contentType: Int,
        errorOut: IntArray
    ): Long

    @JvmStatic
    external fun nativeWrite(handle: Long, buffer: ByteBuffer, position: Int, size: Int): Int

    @JvmStatic
    external fun nativePlay(handle: Long): Int

    @JvmStatic
    external fun nativePause(handle: Long): Int

    @JvmStatic
    external fun nativeFlush(handle: Long): Int

    @JvmStatic
    external fun nativeStop(handle: Long)

    @JvmStatic
    external fun nativeRelease(handle: Long)

    @JvmStatic
    external fun nativeSetVolume(handle: Long, volume: Float)

    @JvmStatic
    external fun nativeGetPlayedFrames(handle: Long): Long

    @JvmStatic
    external fun nativeGetUnderrunCount(handle: Long): Int

    @JvmStatic
    external fun nativeGetErrorCode(handle: Long): Int

    @JvmStatic
    external fun nativeGetSessionId(handle: Long): Int

    @JvmStatic
    external fun nativeGetDeviceId(handle: Long): Int

    @JvmStatic
    external fun nativeGetBufferSizeInFrames(handle: Long): Int
}

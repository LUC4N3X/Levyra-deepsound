package com.luc4n3x.levyra.nativeaudio

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.util.Clock
import androidx.media3.exoplayer.audio.AudioOutput
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.media3.exoplayer.audio.AudioTrackAudioOutputProvider

class OboeAudioOutputProvider(
    private val delegate: AudioOutputProvider,
    private val oboeRequested: () -> Boolean
) : AudioOutputProvider {

    constructor(context: Context, oboeRequested: () -> Boolean) :
        this(AudioTrackAudioOutputProvider.Builder(context).build(), oboeRequested)

    private val failureGuard = OboeFailureGuard()

    @Volatile
    private var preferredDeviceId = 0

    private val outputEvents = object : OboeAudioOutput.Events {
        override fun onOutputFailure() {
            failureGuard.recordFailure(SystemClock.elapsedRealtime())
        }

        override fun onPreferredDeviceChanged(deviceId: Int) {
            preferredDeviceId = deviceId
        }
    }

    val isOboeActive: Boolean
        get() = oboeRequested() && isOboeSupported() && !failureGuard.isTripped

    override fun getFormatSupport(formatConfig: AudioOutputProvider.FormatConfig): AudioOutputProvider.FormatSupport =
        delegate.getFormatSupport(formatConfig)

    override fun getOutputConfig(formatConfig: AudioOutputProvider.FormatConfig): AudioOutputProvider.OutputConfig {
        preferredDeviceId = formatConfig.preferredDevice?.id ?: 0
        val config = delegate.getOutputConfig(formatConfig)
        if (!isOboeActive) return config
        val oboeConfig = config.buildUpon().setUsePlaybackParameters(false).build()
        return if (isEligible(oboeConfig)) oboeConfig else config
    }

    override fun getAudioOutput(config: AudioOutputProvider.OutputConfig): AudioOutput {
        if (isOboeActive && isEligible(config)) {
            val channelCount = OboeOutputPolicy.channelCountOf(config.channelMask)
            OboeAudioOutput.open(
                sampleRate = config.sampleRate,
                channelCount = channelCount,
                audioSessionId = config.audioSessionId,
                preferredDeviceId = preferredDeviceId,
                ringCapacityFrames = OboeOutputPolicy.ringCapacityFrames(config.bufferSize, channelCount, config.sampleRate),
                usage = config.audioAttributes.usage,
                contentType = config.audioAttributes.contentType,
                events = outputEvents
            ).onSuccess { output ->
                return output
            }.onFailure { error ->
                Log.w(TAG, "Falling back to AudioTrack output", error)
                failureGuard.recordFailure(SystemClock.elapsedRealtime())
            }
        }
        return delegate.getAudioOutput(config)
    }

    override fun addListener(listener: AudioOutputProvider.Listener) {
        delegate.addListener(listener)
    }

    override fun removeListener(listener: AudioOutputProvider.Listener) {
        delegate.removeListener(listener)
    }

    override fun setClock(clock: Clock) {
        delegate.setClock(clock)
    }

    override fun release() {
        delegate.release()
    }

    private fun isEligible(config: AudioOutputProvider.OutputConfig): Boolean =
        OboeOutputPolicy.isEligible(
            encoding = config.encoding,
            channelCount = OboeOutputPolicy.channelCountOf(config.channelMask),
            sampleRate = config.sampleRate,
            isOffload = config.isOffload,
            isTunneling = config.isTunneling,
            usesPlatformPlaybackParameters = config.usePlaybackParameters
        )

    companion object {
        private const val TAG = "LevyraOboeOutput"

        fun isOboeSupported(): Boolean =
            Build.VERSION.SDK_INT >= OboeOutputPolicy.MIN_SDK && OboeNative.isLoaded
    }
}

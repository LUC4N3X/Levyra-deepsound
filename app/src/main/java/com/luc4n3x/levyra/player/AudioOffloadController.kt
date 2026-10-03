package com.luc4n3x.levyra.player

import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import com.luc4n3x.levyra.domain.AudioOffloadPreference
import timber.log.Timber

internal enum class AudioOffloadOutput(val label: String) {
    NONE("none"),
    OFFLOADED("offloaded"),
    PCM("pcm"),
    PASSTHROUGH("passthrough")
}

internal fun audioOffloadOutputOf(offload: Boolean, encoding: Int): AudioOffloadOutput = when {
    offload -> AudioOffloadOutput.OFFLOADED
    Util.isEncodingLinearPcm(encoding) -> AudioOffloadOutput.PCM
    else -> AudioOffloadOutput.PASSTHROUGH
}

internal data class AudioOffloadState(
    val preference: AudioOffloadPreference = AudioOffloadPreference.AUTOMATIC,
    val decision: AudioOffloadDecision? = null,
    val output: AudioOffloadOutput = AudioOffloadOutput.NONE
)

internal fun nextAudioOffloadTrackSelection(
    current: TrackSelectionParameters,
    decision: AudioOffloadDecision,
    gaplessRequired: Boolean
): TrackSelectionParameters? {
    val preferences = AudioOffloadPolicy.preferences(decision, gaplessRequired)
    if (current.audioOffloadPreferences == preferences) return null
    return current.buildUpon().setAudioOffloadPreferences(preferences).build()
}

@UnstableApi
internal class AudioOffloadController(
    private val log: (String) -> Unit = { Timber.d(it) }
) : AnalyticsListener {

    @Volatile
    var state: AudioOffloadState = AudioOffloadState()
        private set

    private var lastActiveOutput = AudioOffloadOutput.NONE

    fun update(player: ExoPlayer, inputs: AudioOffloadInputs) {
        val decision = AudioOffloadPolicy.decide(inputs)
        val previous = state.decision
        if (previous != decision) {
            log("Audio offload ${previous?.summary() ?: "UNSET"} -> ${decision.summary()}")
        }
        state = state.copy(preference = inputs.preference, decision = decision)
        nextAudioOffloadTrackSelection(player.trackSelectionParameters, decision, inputs.gaplessRequired)
            ?.let { player.trackSelectionParameters = it }
    }

    override fun onAudioTrackInitialized(
        eventTime: AnalyticsListener.EventTime,
        audioTrackConfig: AudioSink.AudioTrackConfig
    ) {
        val output = audioOffloadOutputOf(audioTrackConfig.offload, audioTrackConfig.encoding)
        if (output == AudioOffloadOutput.OFFLOADED) VisualizerAudioProcessor.clearWaveform()
        if (output != lastActiveOutput) {
            log("Audio output path ${lastActiveOutput.label} -> ${output.label}")
            lastActiveOutput = output
        }
        state = state.copy(output = output)
    }

    override fun onAudioTrackReleased(
        eventTime: AnalyticsListener.EventTime,
        audioTrackConfig: AudioSink.AudioTrackConfig
    ) {
        state = state.copy(output = AudioOffloadOutput.NONE)
    }
}

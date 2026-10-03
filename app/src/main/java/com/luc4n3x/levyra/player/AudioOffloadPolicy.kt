package com.luc4n3x.levyra.player

import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import com.luc4n3x.levyra.domain.AudioOffloadPreference
import com.luc4n3x.levyra.domain.LevyraAudioSettings

internal enum class AudioOffloadBlocker(val label: String) {
    PREFERENCE_OFF("preference_off"),
    AAUDIO_OUTPUT("aaudio_output"),
    CROSSFADE("crossfade"),
    DSP("dsp"),
    NORMALIZATION("normalization"),
    PLAYBACK_SPEED("playback_speed"),
    SKIP_SILENCE("skip_silence")
}

internal data class AudioOffloadInputs(
    val preference: AudioOffloadPreference,
    val aaudioOutputActive: Boolean,
    val crossfadeActive: Boolean,
    val dspActive: Boolean,
    val normalizationActive: Boolean,
    val speed: Float,
    val pitch: Float,
    val skipSilenceEnabled: Boolean,
    val gaplessRequired: Boolean
) {
    companion object {
        fun from(
            settings: LevyraAudioSettings,
            audioNormalization: Boolean,
            speed: Float,
            pitch: Float,
            skipSilenceEnabled: Boolean,
            aaudioOutputSupported: Boolean
        ): AudioOffloadInputs {
            val normalized = settings.normalized()
            val parametricActive = normalized.parametricEqualizerEnabled && normalized.activeParametricProfile != null
            return AudioOffloadInputs(
                preference = normalized.audioOffloadPreference,
                aaudioOutputActive = normalized.aaudioOutputEnabled && aaudioOutputSupported,
                crossfadeActive = normalized.gaplessEnabled && normalized.crossfadeSeconds > 0,
                dspActive = normalized.equalizerEnabled || parametricActive || normalized.enhancedAudioEnabled ||
                    truePeakLimiterRequired(normalized, parametricActive, audioNormalization),
                normalizationActive = audioNormalization || normalized.replayGainActive,
                speed = speed,
                pitch = pitch,
                skipSilenceEnabled = skipSilenceEnabled,
                gaplessRequired = normalized.gaplessEnabled
            )
        }
    }
}

internal data class AudioOffloadDecision(val blockers: Set<AudioOffloadBlocker>) {
    val allowed: Boolean get() = blockers.isEmpty()

    fun summary(): String =
        if (allowed) "ALLOWED" else "BLOCKED(${blockers.joinToString(",") { it.label }})"
}

internal object AudioOffloadPolicy {
    fun decide(inputs: AudioOffloadInputs): AudioOffloadDecision {
        val blockers = buildSet {
            if (inputs.preference == AudioOffloadPreference.OFF) add(AudioOffloadBlocker.PREFERENCE_OFF)
            if (inputs.aaudioOutputActive) add(AudioOffloadBlocker.AAUDIO_OUTPUT)
            if (inputs.crossfadeActive) add(AudioOffloadBlocker.CROSSFADE)
            if (inputs.dspActive) add(AudioOffloadBlocker.DSP)
            if (inputs.normalizationActive) add(AudioOffloadBlocker.NORMALIZATION)
            if (inputs.speed != 1f || inputs.pitch != 1f) add(AudioOffloadBlocker.PLAYBACK_SPEED)
            if (inputs.skipSilenceEnabled) add(AudioOffloadBlocker.SKIP_SILENCE)
        }
        return AudioOffloadDecision(blockers)
    }

    fun preferences(decision: AudioOffloadDecision, gaplessRequired: Boolean): AudioOffloadPreferences =
        if (decision.allowed) {
            AudioOffloadPreferences.Builder()
                .setAudioOffloadMode(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
                .setIsGaplessSupportRequired(gaplessRequired)
                .setIsSpeedChangeSupportRequired(false)
                .build()
        } else {
            AudioOffloadPreferences.Builder()
                .setAudioOffloadMode(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED)
                .build()
        }
}

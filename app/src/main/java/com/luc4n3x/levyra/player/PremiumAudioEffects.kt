package com.luc4n3x.levyra.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import com.luc4n3x.levyra.domain.LevyraAudioPresets
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import kotlin.math.roundToInt

class PremiumAudioEffects {
    private var audioSessionId: Int = 0
    private var settings: LevyraAudioSettings = LevyraAudioSettings()
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    fun bind(sessionId: Int) {
        if (sessionId <= 0 || sessionId == audioSessionId) {
            apply(settings)
            return
        }
        releaseEffects()
        audioSessionId = sessionId
        equalizer = runCatching { Equalizer(0, sessionId) }.getOrNull()
        bassBoost = runCatching { BassBoost(0, sessionId) }.getOrNull()
        virtualizer = runCatching { Virtualizer(0, sessionId) }.getOrNull()
        loudnessEnhancer = runCatching { LoudnessEnhancer(sessionId) }.getOrNull()
        apply(settings)
    }

    fun apply(next: LevyraAudioSettings) {
        settings = next.normalized()
        applyEqualizer(settings)
        applyBassBoost(settings)
        applyVirtualizer(settings)
        applyLoudness(settings)
    }

    fun release() {
        releaseEffects()
        audioSessionId = 0
    }

    private fun applyEqualizer(current: LevyraAudioSettings) {
        val effect = equalizer ?: return
        runCatching {
            effect.enabled = current.equalizerEnabled
            if (!current.equalizerEnabled) return@runCatching
            val bands = effect.numberOfBands.toInt().coerceAtLeast(0)
            if (bands == 0) return@runCatching
            val range = effect.bandLevelRange
            val min = range[0].toInt()
            val max = range[1].toInt()
            val levels = current.bandLevels.takeIf { it.size == LevyraAudioPresets.bandCount } ?: LevyraAudioPresets.levelsFor(current.presetId)
            repeat(bands) { bandIndex ->
                val sourceIndex = if (bands == 1) 0 else ((bandIndex.toFloat() / (bands - 1).toFloat()) * (levels.size - 1)).roundToInt().coerceIn(0, levels.lastIndex)
                val normalized = levels[sourceIndex].coerceIn(-100, 100) / 100f
                val millibels = if (normalized >= 0f) {
                    (normalized * max).roundToInt()
                } else {
                    (-normalized * min).roundToInt()
                }.coerceIn(min, max)
                effect.setBandLevel(bandIndex.toShort(), millibels.toShort())
            }
        }
    }

    private fun applyBassBoost(current: LevyraAudioSettings) {
        val effect = bassBoost ?: return
        runCatching {
            effect.enabled = current.equalizerEnabled && current.bassBoost > 0
            if (effect.enabled) effect.setStrength((current.bassBoost * 10).coerceIn(0, 1000).toShort())
        }
    }

    private fun applyVirtualizer(current: LevyraAudioSettings) {
        val effect = virtualizer ?: return
        runCatching {
            effect.enabled = current.equalizerEnabled && current.virtualizer > 0
            if (effect.enabled) effect.setStrength((current.virtualizer * 10).coerceIn(0, 1000).toShort())
        }
    }

    private fun applyLoudness(current: LevyraAudioSettings) {
        val effect = loudnessEnhancer ?: return
        runCatching {
            effect.enabled = current.replayGainEnabled
            if (effect.enabled) effect.setTargetGain(180)
        }
    }

    private fun releaseEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
    }
}

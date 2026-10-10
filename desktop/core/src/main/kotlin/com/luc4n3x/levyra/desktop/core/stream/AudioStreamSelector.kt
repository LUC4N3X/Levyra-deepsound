package com.luc4n3x.levyra.desktop.core.stream

import com.luc4n3x.levyra.desktop.core.model.AudioQuality
import com.luc4n3x.levyra.desktop.core.model.PreferredCodec
import kotlin.math.abs

object AudioStreamSelector {
    private const val UNKNOWN_BITRATE_FALLBACK = 96
    private const val DISTANCE_BUDGET = 1000

    fun select(
        candidates: List<AudioCandidate>,
        quality: AudioQuality,
        codec: PreferredCodec
    ): AudioCandidate? = candidates
        .filter(::isPlayable)
        .maxByOrNull { score(it, quality, codec) }

    fun score(candidate: AudioCandidate, quality: AudioQuality, codec: PreferredCodec): Int {
        val bitrate = candidate.averageBitrate.takeIf { it > 0 } ?: UNKNOWN_BITRATE_FALLBACK
        val distance = abs(targetBitrate(quality) - bitrate).coerceAtMost(DISTANCE_BUDGET)
        return codecBonus(candidate, codec) + (DISTANCE_BUDGET - distance)
    }

    fun isPlayable(candidate: AudioCandidate): Boolean {
        val url = candidate.url
        if (url.isBlank()) return false
        if (!url.startsWith("https://") && !url.startsWith("http://")) return false
        return !url.contains("/manifest/", ignoreCase = true)
    }

    private fun targetBitrate(quality: AudioQuality): Int = when (quality) {
        AudioQuality.LOW -> 64
        AudioQuality.BALANCED -> 128
        AudioQuality.HIGH -> 320
    }

    private fun codecBonus(candidate: AudioCandidate, codec: PreferredCodec): Int = when (codec) {
        PreferredCodec.OPUS -> if (candidate.isOpus) 2000 else 0
        PreferredCodec.AAC -> if (candidate.isAac) 2000 else 0
        PreferredCodec.AUTO -> when {
            candidate.isOpus -> 60
            candidate.isAac -> 40
            else -> 0
        }
    }
}

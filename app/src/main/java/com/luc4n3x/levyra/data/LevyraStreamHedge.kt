package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.extraction.audio.AudioResolution
import com.luc4n3x.levyra.extraction.audio.AudioStreamResolver
import com.luc4n3x.levyra.extraction.audio.StreamQuality

internal suspend fun <T> hedgedAudioWithRecovery(
    tiers: List<List<suspend () -> T?>>,
    hedgeBudgetMs: Long,
    recovery: (suspend () -> T?)?,
    quality: StreamQuality<T>? = null,
    onAttemptError: (Throwable) -> Unit = {}
): T? {
    return when (val result = AudioStreamResolver.resolve(
        tiers = tiers,
        hedgeBudgetMs = hedgeBudgetMs,
        recovery = recovery,
        isNoPlayableStream = { it is YoutubePlayerRequestException && it.noPlayableStream },
        onAttemptError = onAttemptError,
        quality = quality
    )) {
        is AudioResolution.Resolved -> result.stream
        AudioResolution.NoPlayableStream, AudioResolution.Inconclusive -> null
    }
}

internal fun audioItagPreference(itag: Int, requestedAudioQuality: String): Int =
    if (requestedAudioQuality.equals("low", true)) {
        when (itag) {
            139 -> 420_000
            249 -> 380_000
            140 -> 260_000
            250 -> 180_000
            251 -> 80_000
            141 -> 40_000
            774 -> 20_000
            else -> 0
        }
    } else {
        when (itag) {
            774 -> if (requestedAudioQuality.equals("high", true)) 820_000 else 0
            251 -> 760_000
            141 -> 700_000
            140 -> 560_000
            250 -> 480_000
            249 -> 280_000
            139 -> 120_000
            else -> 0
        }
    }

internal fun <T> audioHedgeQuality(
    requestedAudioQuality: String,
    isVideoMode: Boolean,
    audioItagOf: (T) -> Int?,
    isDirectDelivery: (T) -> Boolean
): StreamQuality<T>? {
    if (isVideoMode || requestedAudioQuality.equals("low", true)) return null
    fun rank(itag: Int, direct: Boolean): Int =
        audioItagPreference(itag, requestedAudioQuality) * 2 + if (direct) 1 else 0
    return StreamQuality(
        rank = { stream -> audioItagOf(stream)?.let { rank(it, isDirectDelivery(stream)) } ?: 0 },
        sufficientRank = rank(251, direct = true),
        graceMs = LevyraResolverLatency.AUDIO_QUALITY_GRACE_MS
    )
}

internal fun orderAudioStrategiesForQuality(
    strategies: List<PlaybackAudioStrategy>,
    requestedAudioQuality: String
): List<PlaybackAudioStrategy> {
    if (requestedAudioQuality.equals("low", true)) return strategies
    val muxedIndex = strategies.indexOf(PlaybackAudioStrategy.REEL_MUXED)
    val directIndex = strategies.indexOf(PlaybackAudioStrategy.DIRECT)
    if (muxedIndex < 0 || directIndex < 0 || muxedIndex > directIndex) return strategies
    return strategies.toMutableList().apply {
        removeAt(muxedIndex)
        add(indexOf(PlaybackAudioStrategy.DIRECT) + 1, PlaybackAudioStrategy.REEL_MUXED)
    }
}

internal object LevyraResolverLatency {
    const val INNER_TUBE_HEDGE_BUDGET_MS = 35L
    const val AUDIO_QUALITY_GRACE_MS = 250L
    private const val AUDIO_INNER_TUBE_FALLBACK_MS = 0L
    private const val VIDEO_INNER_TUBE_FALLBACK_MS = 0L
    private const val OFFLINE_INNER_TUBE_FALLBACK_MS = 2_500L

    fun extractorHedgeDelayMs(isVideoMode: Boolean, preferMp4Audio: Boolean): Long {
        if (preferMp4Audio) return 0L
        return if (isVideoMode) 900L else 600L
    }

    fun innerTubeFallbackDelayMs(isVideoMode: Boolean, preferMp4Audio: Boolean): Long {
        if (preferMp4Audio) return OFFLINE_INNER_TUBE_FALLBACK_MS
        return if (isVideoMode) VIDEO_INNER_TUBE_FALLBACK_MS else AUDIO_INNER_TUBE_FALLBACK_MS
    }
}

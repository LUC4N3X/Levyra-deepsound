package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.extraction.audio.AudioResolution
import com.luc4n3x.levyra.extraction.audio.AudioStreamResolver

internal suspend fun <T> hedgedAudioWithRecovery(
    tiers: List<List<suspend () -> T?>>,
    hedgeBudgetMs: Long,
    recovery: (suspend () -> T?)?,
    onAttemptError: (Throwable) -> Unit = {}
): T? {
    return when (val result = AudioStreamResolver.resolve(
        tiers = tiers,
        hedgeBudgetMs = hedgeBudgetMs,
        recovery = recovery,
        isNoPlayableStream = { it is YoutubePlayerRequestException && it.noPlayableStream },
        onAttemptError = onAttemptError
    )) {
        is AudioResolution.Resolved -> result.stream
        AudioResolution.NoPlayableStream, AudioResolution.Inconclusive -> null
    }
}

internal object LevyraResolverLatency {
    const val INNER_TUBE_HEDGE_BUDGET_MS = 35L
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

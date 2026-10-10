package com.luc4n3x.levyra.player.waveseek

internal data class WaveSeekCaptureSpec(
    val mediaId: String,
    val durationMs: Long
)

internal fun waveSeekCaptureSpec(
    mediaId: String,
    source: String,
    durationMs: Long,
    videoMode: Boolean,
    liveRadio: Boolean,
    alreadyStored: Boolean
): WaveSeekCaptureSpec? {
    val cleanId = mediaId.trim()
    if (cleanId.isBlank() || videoMode || liveRadio || alreadyStored) return null
    if (!WaveSeekSourcePolicy.canAnalyze(source, durationMs)) return null
    return WaveSeekCaptureSpec(cleanId, durationMs)
}

internal fun waveSeekResolvedDurationMs(
    playerDurationMs: Long,
    metadataDurationMs: Long
): Long = playerDurationMs.takeIf { it > 0L }
    ?: metadataDurationMs.takeIf { it > 0L }
    ?: 0L

internal fun waveSeekPollDelayMs(
    hasPlayer: Boolean,
    hasCapture: Boolean,
    isPlaying: Boolean
): Long = when {
    !hasPlayer -> 1_500L
    hasCapture && isPlaying -> 120L
    hasCapture -> 750L
    else -> 1_000L
}

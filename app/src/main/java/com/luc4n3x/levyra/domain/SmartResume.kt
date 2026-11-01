package com.luc4n3x.levyra.domain

import kotlin.math.ln
import kotlin.math.roundToLong

enum class SmartResumeProfile(val storageValue: String, val scale: Float) {
    GENTLE("gentle", 0.5f),
    BALANCED("balanced", 1f),
    GENEROUS("generous", 1.6f);

    companion object {
        fun fromStorage(value: String?): SmartResumeProfile = entries.firstOrNull { it.storageValue == value } ?: BALANCED
    }
}

enum class SmartResumeCause { TransientFocusLoss, FocusLoss, OutputLost, SuppressedOutput }

object SmartResumeCurve {
    const val MIN_PAUSE_SECONDS = 5
    const val MAX_PAUSE_SECONDS = 120
    const val DEFAULT_PAUSE_SECONDS = 10
    const val MIN_POSITION_MS = 1_500L
    private const val BASE_MS = 2_000.0
    private const val GROWTH_MS = 4_000.0
    private const val CAP_MS = 30_000.0
    private const val STEP_MS = 500L

    fun normalizedPauseSeconds(value: Int): Int = value.coerceIn(MIN_PAUSE_SECONDS, MAX_PAUSE_SECONDS)

    fun rewindMs(interruptionMs: Long, profile: SmartResumeProfile, minPauseSeconds: Int): Long {
        val threshold = normalizedPauseSeconds(minPauseSeconds) * 1_000L
        if (interruptionMs < threshold) return 0L
        val minutes = interruptionMs / 60_000.0
        val raw = (BASE_MS + GROWTH_MS * ln(1.0 + minutes) / ln(2.0)) * profile.scale
        val capped = raw.coerceAtMost(CAP_MS * profile.scale)
        return (capped / STEP_MS).roundToLong() * STEP_MS
    }
}

data class SmartResumeMedia(
    val key: String,
    val positionMs: Long,
    val durationMs: Long,
    val seekable: Boolean,
    val live: Boolean
) {
    val rewindable: Boolean get() = key.isNotEmpty() && seekable && !live && durationMs > 0L
}

class SmartResumeTracker(private val clock: () -> Long) {
    private class Interruption(val cause: SmartResumeCause, val startedAtMs: Long, val mediaKey: String)

    private var pending: Interruption? = null

    val hasPendingInterruption: Boolean get() = pending != null

    fun onInterrupted(cause: SmartResumeCause, mediaKey: String) {
        if (mediaKey.isEmpty()) return
        val current = pending
        if (current != null && current.mediaKey == mediaKey) return
        pending = Interruption(cause, clock(), mediaKey)
    }

    fun onUserTookControl() {
        pending = null
    }

    fun onMediaChanged(mediaKey: String) {
        if (pending?.mediaKey != mediaKey) pending = null
    }

    fun onResumed(
        enabled: Boolean,
        profile: SmartResumeProfile,
        minPauseSeconds: Int,
        media: SmartResumeMedia
    ): Long? {
        val interruption = pending ?: return null
        pending = null
        if (!enabled || !media.rewindable || interruption.mediaKey != media.key) return null
        if (media.positionMs < SmartResumeCurve.MIN_POSITION_MS) return null
        val elapsed = (clock() - interruption.startedAtMs).coerceAtLeast(0L)
        val rewind = SmartResumeCurve.rewindMs(elapsed, profile, minPauseSeconds)
        if (rewind <= 0L) return null
        val target = (media.positionMs - rewind).coerceAtLeast(0L)
        return target.takeIf { it < media.positionMs }
    }
}

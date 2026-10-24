package com.luc4n3x.levyra.player

import android.os.SystemClock
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.analytics.AnalyticsListener

internal const val SUSTAINED_FRAME_DROP_MIN_FRAMES = 30
internal const val SUSTAINED_FRAME_DROP_MAX_WINDOW_MS = 12_000L
internal const val SUSTAINED_FRAME_DROP_MIN_PER_SECOND = 4.0
internal const val SUSTAINED_FRAME_DROP_COOLDOWN_MS = 20_000L
internal const val FRAME_DROP_TRANSITION_GRACE_MS = 3_000L

internal fun isSustainedVideoFrameDrop(droppedFrames: Int, elapsedMs: Long): Boolean {
    if (droppedFrames < SUSTAINED_FRAME_DROP_MIN_FRAMES) return false
    if (elapsedMs <= 0L || elapsedMs > SUSTAINED_FRAME_DROP_MAX_WINDOW_MS) return false
    return droppedFrames * 1_000.0 / elapsedMs >= SUSTAINED_FRAME_DROP_MIN_PER_SECOND
}

internal fun frameDropWindowOverlapsTransition(nowMs: Long, elapsedMs: Long, lastTransitionMs: Long): Boolean {
    if (lastTransitionMs == Long.MIN_VALUE) return false
    val windowStartMs = nowMs - elapsedMs.coerceAtLeast(0L)
    return lastTransitionMs >= windowStartMs - FRAME_DROP_TRANSITION_GRACE_MS
}

@UnstableApi
internal class VideoFrameDropMonitor(
    private val nowMs: () -> Long = SystemClock::elapsedRealtime
) : AnalyticsListener {
    @Volatile
    var onSustainedFrameDrops: (() -> Unit)? = null

    private var lastSignalMs = Long.MIN_VALUE
    private var lastTransitionMs = Long.MIN_VALUE

    override fun onPositionDiscontinuity(
        eventTime: AnalyticsListener.EventTime,
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        lastTransitionMs = nowMs()
    }

    override fun onVideoInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?
    ) {
        lastTransitionMs = nowMs()
    }

    override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
        if (!isSustainedVideoFrameDrop(droppedFrames, elapsedMs)) return
        val now = nowMs()
        if (frameDropWindowOverlapsTransition(now, elapsedMs, lastTransitionMs)) return
        if (lastSignalMs != Long.MIN_VALUE && now - lastSignalMs < SUSTAINED_FRAME_DROP_COOLDOWN_MS) return
        lastSignalMs = now
        onSustainedFrameDrops?.invoke()
    }

    companion object {
        val Shared = VideoFrameDropMonitor()
    }
}

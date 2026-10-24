package com.luc4n3x.levyra.player

import android.os.SystemClock
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener

internal const val SUSTAINED_FRAME_DROP_MIN_FRAMES = 30
internal const val SUSTAINED_FRAME_DROP_MAX_WINDOW_MS = 12_000L
internal const val SUSTAINED_FRAME_DROP_MIN_PER_SECOND = 4.0
internal const val SUSTAINED_FRAME_DROP_COOLDOWN_MS = 20_000L

internal fun isSustainedVideoFrameDrop(droppedFrames: Int, elapsedMs: Long): Boolean {
    if (droppedFrames < SUSTAINED_FRAME_DROP_MIN_FRAMES) return false
    if (elapsedMs <= 0L || elapsedMs > SUSTAINED_FRAME_DROP_MAX_WINDOW_MS) return false
    return droppedFrames * 1_000.0 / elapsedMs >= SUSTAINED_FRAME_DROP_MIN_PER_SECOND
}

@UnstableApi
internal class VideoFrameDropMonitor(
    private val nowMs: () -> Long = SystemClock::elapsedRealtime
) : AnalyticsListener {
    @Volatile
    var onSustainedFrameDrops: (() -> Unit)? = null

    private var lastSignalMs = Long.MIN_VALUE

    override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
        if (!isSustainedVideoFrameDrop(droppedFrames, elapsedMs)) return
        val now = nowMs()
        if (lastSignalMs != Long.MIN_VALUE && now - lastSignalMs < SUSTAINED_FRAME_DROP_COOLDOWN_MS) return
        lastSignalMs = now
        onSustainedFrameDrops?.invoke()
    }

    companion object {
        val Shared = VideoFrameDropMonitor()
    }
}

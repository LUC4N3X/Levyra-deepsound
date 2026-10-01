package com.luc4n3x.levyra.player.liveupdate

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import kotlin.math.ceil

internal object LiveUpdatePolicy {
    const val MIN_SDK: Int = Build.VERSION_CODES.BAKLAVA

    @ChecksSdkIntAtLeast(api = MIN_SDK)
    fun isSupported(): Boolean = isSupported(Build.VERSION.SDK_INT)

    fun isSupported(sdkInt: Int): Boolean = sdkInt >= MIN_SDK

    fun formatClock(durationMs: Long): String {
        val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = totalSeconds % 3_600L / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            "$hours:${minutes.twoDigits()}:${seconds.twoDigits()}"
        } else {
            "$minutes:${seconds.twoDigits()}"
        }
    }

    private fun Long.twoDigits(): String = if (this < 10L) "0$this" else toString()
}

internal data class PlaybackLiveUpdateInput(
    val mediaId: String?,
    val title: String,
    val artist: String,
    val playWhenReady: Boolean,
    val buffering: Boolean,
    val ready: Boolean,
    val playing: Boolean,
    val playingAd: Boolean,
    val dynamic: Boolean,
    val speed: Float,
    val positionMs: Long,
    val durationMs: Long,
    val nowEpochMs: Long
)

internal data class PlaybackLiveUpdateContent(
    val mediaId: String,
    val title: String,
    val text: String,
    val chronometerBaseEpochMs: Long?,
    val timeoutMs: Long
) {
    fun isEquivalentTo(other: PlaybackLiveUpdateContent?): Boolean {
        if (other == null) return false
        if (mediaId != other.mediaId || title != other.title || text != other.text) return false
        val base = chronometerBaseEpochMs
        val otherBase = other.chronometerBaseEpochMs
        if (base == null && otherBase == null) return kotlin.math.abs(timeoutMs - other.timeoutMs) < CHRONOMETER_TOLERANCE_MS
        if (base == null || otherBase == null) return false
        return kotlin.math.abs(base - otherBase) < CHRONOMETER_TOLERANCE_MS
    }

    private companion object {
        const val CHRONOMETER_TOLERANCE_MS = 1_000L
    }
}

internal object PlaybackLiveUpdateMapper {
    const val UNKNOWN_DURATION_TIMEOUT_MS = 30L * 60L * 1_000L
    const val TIMEOUT_GRACE_MS = 60L * 1_000L

    fun map(input: PlaybackLiveUpdateInput): PlaybackLiveUpdateContent? {
        val mediaId = input.mediaId ?: return null
        val title = input.title.trim()
        if (title.isEmpty() || !isActive(input)) return null
        val knownDuration = !input.dynamic && input.durationMs > 0L
        val position = input.positionMs.coerceAtLeast(0L)
        return PlaybackLiveUpdateContent(
            mediaId = mediaId,
            title = title,
            text = contentText(input.artist.trim(), input.durationMs.takeIf { knownDuration }),
            chronometerBaseEpochMs = chronometerBase(input, position),
            timeoutMs = if (knownDuration) {
                wallClockRemainingMs(input.durationMs - position, input.speed) + TIMEOUT_GRACE_MS
            } else {
                UNKNOWN_DURATION_TIMEOUT_MS
            }
        )
    }

    private fun isActive(input: PlaybackLiveUpdateInput): Boolean =
        input.playWhenReady && (input.buffering || input.ready)

    private fun wallClockRemainingMs(remainingMediaMs: Long, speed: Float): Long {
        val safeSpeed = if (speed.isFinite() && speed > 0f) speed else 1f
        return ceil(remainingMediaMs.coerceAtLeast(0L) / safeSpeed.toDouble()).toLong()
    }

    private fun contentText(artist: String, durationMs: Long?): String {
        val duration = durationMs?.let(LiveUpdatePolicy::formatClock) ?: return artist
        return if (artist.isEmpty()) duration else "$artist · $duration"
    }

    private fun chronometerBase(input: PlaybackLiveUpdateInput, positionMs: Long): Long? {
        if (!input.playing || input.playingAd) return null
        if (input.dynamic || input.speed != 1f) return null
        return input.nowEpochMs - positionMs
    }
}

internal data class DownloadBatchProgress(
    val title: String,
    val completed: Int,
    val total: Int
)

internal data class DownloadLiveUpdateContent(
    val shortCriticalText: String,
    val subText: String?,
    val requestPromotion: Boolean
)

internal object DownloadLiveUpdateMapper {
    fun map(
        progress: Int,
        batch: DownloadBatchProgress?,
        promotionAllowed: Boolean
    ): DownloadLiveUpdateContent {
        val percent = progress.coerceIn(0, 100)
        val subText = batch
            ?.takeIf { it.total > 1 }
            ?.let { current ->
                val counter = "${current.completed.coerceIn(0, current.total)} / ${current.total}"
                val title = current.title.trim()
                if (title.isEmpty()) counter else "$title · $counter"
            }
        return DownloadLiveUpdateContent(
            shortCriticalText = "$percent%",
            subText = subText,
            requestPromotion = promotionAllowed && percent < 100
        )
    }
}

internal object DownloadLiveUpdateSlot {
    private var owner: String? = null

    @Synchronized
    fun claim(taskKey: String): Boolean {
        val current = owner
        if (current == null || current == taskKey) {
            owner = taskKey
            return true
        }
        return false
    }

    @Synchronized
    fun release(taskKey: String) {
        if (owner == taskKey) owner = null
    }
}

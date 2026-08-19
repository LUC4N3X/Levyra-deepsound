package com.luc4n3x.levyra.update

import java.util.Locale
import kotlin.math.roundToLong

internal const val UPDATE_PROGRESS_STEP_BYTES = 64L * 1024L

private const val SPEED_SMOOTHING = 0.35
private const val MIN_SPEED_SAMPLE_MS = 400L
private const val MIN_REPORTABLE_BYTES_PER_SECOND = 1024.0
private const val KIB = 1024.0
private const val MIB = 1024.0 * KIB

internal fun updateProgressPercent(downloadedBytes: Long, totalBytes: Long?): Int? {
    if (totalBytes == null || totalBytes <= 0L) return null
    val downloaded = downloadedBytes.coerceAtLeast(0L)
    return ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
}

internal fun smoothUpdateSpeed(previous: Double?, sampleBytesPerSecond: Double): Double? {
    if (sampleBytesPerSecond <= 0.0) return previous
    val prior = previous ?: return sampleBytesPerSecond
    return prior + SPEED_SMOOTHING * (sampleBytesPerSecond - prior)
}

internal fun updateEtaMs(remainingBytes: Long, bytesPerSecond: Double?): Long? {
    if (remainingBytes <= 0L) return null
    val speed = bytesPerSecond ?: return null
    if (speed < MIN_REPORTABLE_BYTES_PER_SECOND) return null
    return (remainingBytes / speed * 1000.0).roundToLong().coerceAtLeast(0L)
}

internal fun formatUpdateBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L)
    return when {
        safe >= MIB -> String.format(Locale.ROOT, "%.1f MB", safe / MIB)
        safe >= KIB -> String.format(Locale.ROOT, "%.0f KB", safe / KIB)
        else -> String.format(Locale.ROOT, "%d B", safe)
    }
}

internal fun formatUpdateSpeed(bytesPerSecond: Double?): String? {
    val speed = bytesPerSecond ?: return null
    if (speed < MIN_REPORTABLE_BYTES_PER_SECOND) return null
    return when {
        speed >= MIB -> String.format(Locale.ROOT, "%.1f MB/s", speed / MIB)
        else -> String.format(Locale.ROOT, "%.0f KB/s", speed / KIB)
    }
}

internal fun formatUpdateDuration(millis: Long?): String? {
    val value = millis?.takeIf { it >= 0L } ?: return null
    val totalSeconds = (value / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L -> String.format(Locale.ROOT, "%d h %02d m", hours, minutes)
        minutes > 0L -> String.format(Locale.ROOT, "%d m %02d s", minutes, seconds)
        else -> String.format(Locale.ROOT, "%d s", seconds)
    }
}

internal fun formatUpdateTransferLine(
    downloadedBytes: Long,
    totalBytes: Long?,
    bytesPerSecond: Double?
): String {
    val parts = mutableListOf<String>()
    parts += if (totalBytes != null && totalBytes > 0L) {
        "${formatUpdateBytes(downloadedBytes)} / ${formatUpdateBytes(totalBytes)}"
    } else {
        formatUpdateBytes(downloadedBytes)
    }
    formatUpdateSpeed(bytesPerSecond)?.let(parts::add)
    val remaining = totalBytes?.let { (it - downloadedBytes).coerceAtLeast(0L) } ?: -1L
    if (remaining > 0L) {
        formatUpdateDuration(updateEtaMs(remaining, bytesPerSecond))?.let { parts += "~$it" }
    }
    return parts.joinToString(separator = " · ")
}

internal class AppUpdateSpeedTracker {
    private var anchorAtMs: Long? = null
    private var anchorBytes = 0L
    private var smoothed: Double? = null

    val bytesPerSecond: Double?
        get() = smoothed

    fun reset() {
        anchorAtMs = null
        anchorBytes = 0L
        smoothed = null
    }

    fun sample(downloadedBytes: Long, atElapsedMs: Long): Double? {
        val anchor = anchorAtMs
        if (anchor == null || downloadedBytes < anchorBytes) {
            anchorAtMs = atElapsedMs
            anchorBytes = downloadedBytes
            return smoothed
        }
        val deltaMs = atElapsedMs - anchor
        if (deltaMs < MIN_SPEED_SAMPLE_MS) return smoothed
        val deltaBytes = downloadedBytes - anchorBytes
        anchorAtMs = atElapsedMs
        anchorBytes = downloadedBytes
        if (deltaBytes <= 0L) return smoothed
        smoothed = smoothUpdateSpeed(smoothed, deltaBytes * 1000.0 / deltaMs)
        return smoothed
    }
}

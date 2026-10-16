package com.luc4n3x.levyra.update

import java.util.Locale
import kotlin.math.roundToLong

internal const val UPDATE_PROGRESS_STEP_BYTES = 64L * 1024L

private const val SPEED_SMOOTHING = 0.35
private const val MIN_SPEED_SAMPLE_MS = 400L
private const val MIN_REPORTABLE_BYTES_PER_SECOND = 1024.0
private const val KIB = 1024.0
private const val MIB = 1024.0 * KIB

internal data class UpdateUnits(
    val locale: Locale = Locale.ROOT,
    val bytes: String = "B",
    val kilobytes: String = "KB",
    val megabytes: String = "MB",
    val perSecond: String = "/s",
    val hours: String = "h",
    val minutes: String = "m",
    val seconds: String = "s"
)

private val defaultUpdateUnits = UpdateUnits()

private val russianUpdateUnits = UpdateUnits(
    locale = Locale.forLanguageTag("ru"),
    bytes = "Б",
    kilobytes = "КБ",
    megabytes = "МБ",
    perSecond = "/с",
    hours = "ч",
    minutes = "мин",
    seconds = "с"
)

internal fun updateUnitsFor(languageCode: String): UpdateUnits =
    if (languageCode.substringBefore('-').equals("ru", ignoreCase = true)) russianUpdateUnits else defaultUpdateUnits

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

internal fun formatUpdateBytes(bytes: Long, units: UpdateUnits = defaultUpdateUnits): String {
    val safe = bytes.coerceAtLeast(0L)
    return when {
        safe >= MIB -> String.format(units.locale, "%.1f %s", safe / MIB, units.megabytes)
        safe >= KIB -> String.format(units.locale, "%.0f %s", safe / KIB, units.kilobytes)
        else -> String.format(units.locale, "%d %s", safe, units.bytes)
    }
}

internal fun formatUpdateSpeed(bytesPerSecond: Double?, units: UpdateUnits = defaultUpdateUnits): String? {
    val speed = bytesPerSecond ?: return null
    if (speed < MIN_REPORTABLE_BYTES_PER_SECOND) return null
    return when {
        speed >= MIB -> String.format(units.locale, "%.1f %s%s", speed / MIB, units.megabytes, units.perSecond)
        else -> String.format(units.locale, "%.0f %s%s", speed / KIB, units.kilobytes, units.perSecond)
    }
}

internal fun formatUpdateDuration(millis: Long?, units: UpdateUnits = defaultUpdateUnits): String? {
    val value = millis?.takeIf { it >= 0L } ?: return null
    val totalSeconds = (value / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L -> String.format(units.locale, "%d %s %02d %s", hours, units.hours, minutes, units.minutes)
        minutes > 0L -> String.format(units.locale, "%d %s %02d %s", minutes, units.minutes, seconds, units.seconds)
        else -> String.format(units.locale, "%d %s", seconds, units.seconds)
    }
}

internal fun formatUpdateTransferLine(
    downloadedBytes: Long,
    totalBytes: Long?,
    bytesPerSecond: Double?,
    units: UpdateUnits = defaultUpdateUnits
): String {
    val parts = mutableListOf<String>()
    parts += if (totalBytes != null && totalBytes > 0L) {
        "${formatUpdateBytes(downloadedBytes, units)} / ${formatUpdateBytes(totalBytes, units)}"
    } else {
        formatUpdateBytes(downloadedBytes, units)
    }
    formatUpdateSpeed(bytesPerSecond, units)?.let(parts::add)
    val remaining = totalBytes?.let { (it - downloadedBytes).coerceAtLeast(0L) } ?: -1L
    if (remaining > 0L) {
        formatUpdateDuration(updateEtaMs(remaining, bytesPerSecond), units)?.let { parts += "~$it" }
    }
    return parts.joinToString(separator = " · ")
}

internal class AppUpdateSpeedTracker {
    private var anchorAtMs: Long? = null
    private var anchorBytes = 0L
    private var smoothed: Double? = null

    val bytesPerSecond: Double?
        @Synchronized get() = smoothed

    @Synchronized
    fun reset() {
        anchorAtMs = null
        anchorBytes = 0L
        smoothed = null
    }

    @Synchronized
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

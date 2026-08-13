package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import java.text.NumberFormat
import java.util.Locale

internal fun LevyraStrings.formatLibraryDuration(durationMs: Long): String {
    val totalMinutes = durationMs.coerceAtLeast(0L) / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    val number = NumberFormat.getIntegerInstance(Locale.forLanguageTag(code))
    val (hourUnit, minuteUnit) = durationUnits(code)
    val compactUnits = code in setOf("zh", "ja", "ko")

    fun part(value: Long, unit: String): String =
        if (compactUnits) "${number.format(value)}$unit" else "${number.format(value)} $unit"

    val formatted = when {
        hours == 0L -> part(minutes, minuteUnit)
        minutes == 0L -> part(hours, hourUnit)
        else -> "${part(hours, hourUnit)} ${part(minutes, minuteUnit)}"
    }
    return formatted.isolateForRtl(code)
}

internal fun LevyraStrings.formatLibraryBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L).toDouble()
    val (value, unit, fractionDigits) = when {
        safe >= GIBIBYTE -> Triple(safe / GIBIBYTE, "GB", 1)
        safe >= MEBIBYTE -> Triple(safe / MEBIBYTE, "MB", 0)
        safe >= KIBIBYTE -> Triple(safe / KIBIBYTE, "KB", 0)
        else -> Triple(safe, "B", 0)
    }
    val number = NumberFormat.getNumberInstance(Locale.forLanguageTag(code)).apply {
        minimumFractionDigits = fractionDigits
        maximumFractionDigits = fractionDigits
        isGroupingUsed = false
    }
    return "${number.format(value)} $unit".isolateForRtl(code)
}

private fun durationUnits(code: String): Pair<String, String> = when (LevyraLanguageCatalog.normalize(code)) {
    "de" -> "Std." to "Min."
    "es" -> "h" to "min"
    "fr" -> "h" to "min"
    "pt" -> "h" to "min"
    "nl" -> "u" to "min"
    "pl" -> "godz." to "min"
    "ro" -> "h" to "min"
    "el" -> "ώ" to "λ"
    "sv" -> "tim" to "min"
    "da" -> "t" to "min"
    "cs" -> "h" to "min"
    "uk", "ru" -> "год" to "хв"
    "tr" -> "sa" to "dk"
    "ar" -> "س" to "د"
    "zh" -> "小时" to "分钟"
    "ja" -> "時間" to "分"
    "ko" -> "시간" to "분"
    "hi" -> "घं" to "मि"
    "id" -> "j" to "mnt"
    "vi" -> "giờ" to "ph"
    "th" -> "ชม." to "น."
    "fil" -> "oras" to "min"
    "he" -> "ש׳" to "דק׳"
    else -> "h" to "min"
}

private fun String.isolateForRtl(code: String): String =
    if (LevyraLanguageCatalog.isRtl(code)) "\u2068$this\u2069" else this

private const val KIBIBYTE = 1024.0
private const val MEBIBYTE = KIBIBYTE * 1024.0
private const val GIBIBYTE = MEBIBYTE * 1024.0

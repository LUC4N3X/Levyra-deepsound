package com.luc4n3x.levyra.ui

import android.icu.text.CompactDecimalFormat
import java.util.Locale

private val AudienceNumber = Regex("""(\d[\d.,   ]*?)\s*([A-Za-zА-Яа-яЁё]{1,4})?\.?(?=\s|$)""")

private val AudienceMultipliers = mapOf(
    "k" to 1_000L, "mila" to 1_000L, "tsd" to 1_000L, "tys" to 1_000L, "тыс" to 1_000L,
    "m" to 1_000_000L, "mln" to 1_000_000L, "mio" to 1_000_000L, "mi" to 1_000_000L, "млн" to 1_000_000L,
    "b" to 1_000_000_000L, "bn" to 1_000_000_000L, "mrd" to 1_000_000_000L, "md" to 1_000_000_000L, "млрд" to 1_000_000_000L
)

internal fun artistAudienceCount(raw: String): Long? {
    val match = AudienceNumber.find(raw) ?: return null
    val digits = match.groupValues[1].filterNot { it == ' ' || it == ' ' || it == ' ' }
    val suffix = match.groupValues[2].lowercase(Locale.ROOT)
    if (suffix.isEmpty()) return digits.filter(Char::isDigit).toLongOrNull()
    val multiplier = AudienceMultipliers[suffix] ?: return null
    val value = digits.replace(',', '.').toDoubleOrNull() ?: return null
    return (value * multiplier).toLong()
}

internal fun artistAudienceLabel(raw: String, languageCode: String, listenersWord: String): String {
    val count = artistAudienceCount(raw) ?: return raw.substringAfterLast("•").trim()
    val compact = CompactDecimalFormat.getInstance(Locale.forLanguageTag(languageCode), CompactDecimalFormat.CompactStyle.SHORT).format(count)
    return "$compact $listenersWord"
}

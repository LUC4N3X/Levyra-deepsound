package com.luc4n3x.levyra.domain

import java.util.Locale

private val COMPACT_COUNT_LEADING_NUMBER = Regex("""^[0-9][0-9.,  ]*""")
private val COMPACT_COUNT_DURATION = Regex("""^[0-9]+(:[0-9]{2})+$""")

private val COMPACT_COUNT_MULTIPLIERS = mapOf(
    "k" to 1_000L, "tys" to 1_000L, "tis" to 1_000L, "mil" to 1_000L, "тыс" to 1_000L, "тис" to 1_000L,
    "m" to 1_000_000L, "mi" to 1_000_000L, "mln" to 1_000_000L, "mio" to 1_000_000L, "млн" to 1_000_000L,
    "b" to 1_000_000_000L, "bn" to 1_000_000_000L, "mld" to 1_000_000_000L, "mrd" to 1_000_000_000L,
    "млрд" to 1_000_000_000L,
    "万" to 10_000L, "萬" to 10_000L, "만" to 10_000L,
    "億" to 100_000_000L, "亿" to 100_000_000L, "억" to 100_000_000L
)

internal fun parseCompactViewCount(label: String): Long {
    val text = label.trim()
    if (text.isEmpty() || COMPACT_COUNT_DURATION.matches(text)) return -1L
    if (text.none(Char::isLetter)) return -1L
    val numberPart = COMPACT_COUNT_LEADING_NUMBER.find(text)?.value?.trim() ?: return -1L

    val suffix = text.removePrefix(numberPart).trimStart()
    val magnitude = suffix.takeWhile { !it.isWhitespace() }
        .trimEnd('.')
        .lowercase(Locale.ROOT)
    val multiplier = COMPACT_COUNT_MULTIPLIERS[magnitude]

    return if (multiplier == null) {
        numberPart.filter(Char::isDigit).toLongOrNull() ?: -1L
    } else {
        val scalar = numberPart.replace(',', '.').filterNot(Char::isWhitespace)
        val value = scalar.toDoubleOrNull() ?: return -1L
        (value * multiplier).toLong()
    }
}

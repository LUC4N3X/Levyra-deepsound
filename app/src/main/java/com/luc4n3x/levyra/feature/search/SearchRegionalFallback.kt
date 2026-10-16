package com.luc4n3x.levyra.feature.search

import com.luc4n3x.levyra.domain.SearchResults
import java.text.Normalizer
import java.util.Locale

internal const val REGIONAL_FALLBACK_MIN_SONGS = 4
internal const val REGIONAL_FALLBACK_TIMEOUT_MS = 4_000L

private val regionalWhitespace = Regex("""\s+""")

internal fun regionalSearchFallbackQuery(query: String, results: SearchResults?): String? {
    if (results != null && results.songs.size >= REGIONAL_FALLBACK_MIN_SONGS) return null
    val variant = southIndianLatinVariant(query) ?: return null
    val variantKey = searchQueryKey(variant)
    if (variantKey.length < MIN_SEARCH_QUERY_CHARS || variantKey == searchQueryKey(query)) return null
    return variant
}

internal fun southIndianLatinVariant(query: String): String? {
    val source = Normalizer.normalize(query.trim(), Normalizer.Form.NFC)
    var transformed = false
    val out = StringBuilder(source.length * 2)
    var index = 0
    while (index < source.length) {
        val codePoint = source.codePointAt(index)
        val script = southIndianScript(codePoint)
        if (script == null) {
            out.appendCodePoint(codePoint)
            index += Character.charCount(codePoint)
            continue
        }
        val syllable = script.romanize(source, index)
        out.append(syllable.text)
        index = syllable.nextIndex
        transformed = true
    }
    if (!transformed) return null
    return out.toString()
        .lowercase(Locale.ROOT)
        .replace(regionalWhitespace, " ")
        .trim()
        .takeIf { it.any { char -> char in 'a'..'z' } }
}

private fun southIndianScript(codePoint: Int): SouthIndianScript? = when (codePoint) {
    in 0x0B80..0x0BFF -> tamil
    in 0x0C00..0x0C7F -> telugu
    else -> null
}

private data class RomanizedSyllable(val text: String, val nextIndex: Int)

private class SouthIndianScript(
    val consonants: Map<Int, String>,
    val independentVowels: Map<Int, String>,
    val vowelSigns: Map<Int, String>,
    val signs: Map<Int, String>,
    val virama: Int
) {
    fun romanize(source: String, start: Int): RomanizedSyllable {
        val codePoint = source.codePointAt(start)
        val next = start + Character.charCount(codePoint)
        independentVowels[codePoint]?.let { return RomanizedSyllable(it, next) }
        signs[codePoint]?.let { sign ->
            val beforeConsonant = next < source.length && source.codePointAt(next) in consonants
            return RomanizedSyllable(if (sign == "m" && beforeConsonant) "n" else sign, next)
        }
        vowelSigns[codePoint]?.let { return RomanizedSyllable(it, next) }
        if (codePoint == virama) return RomanizedSyllable("", next)
        val consonant = consonants[codePoint] ?: return RomanizedSyllable(" ", next)
        if (next < source.length) {
            val following = source.codePointAt(next)
            val followingEnd = next + Character.charCount(following)
            if (following == virama) return RomanizedSyllable(consonant, followingEnd)
            vowelSigns[following]?.let { return RomanizedSyllable(consonant + it, followingEnd) }
        }
        return RomanizedSyllable(consonant + "a", next)
    }
}

private fun table(vararg entries: Pair<Int, String>): Map<Int, String> = entries.toMap()

private val tamil = SouthIndianScript(
    consonants = table(
        0x0B95 to "k", 0x0B99 to "ng", 0x0B9A to "s", 0x0B9C to "j", 0x0B9E to "ny",
        0x0B9F to "t", 0x0BA3 to "n", 0x0BA4 to "th", 0x0BA8 to "n", 0x0BA9 to "n",
        0x0BAA to "p", 0x0BAE to "m", 0x0BAF to "y", 0x0BB0 to "r", 0x0BB1 to "r",
        0x0BB2 to "l", 0x0BB3 to "l", 0x0BB4 to "zh", 0x0BB5 to "v", 0x0BB6 to "sh",
        0x0BB7 to "sh", 0x0BB8 to "s", 0x0BB9 to "h"
    ),
    independentVowels = table(
        0x0B85 to "a", 0x0B86 to "aa", 0x0B87 to "i", 0x0B88 to "ee", 0x0B89 to "u",
        0x0B8A to "oo", 0x0B8E to "e", 0x0B8F to "e", 0x0B90 to "ai", 0x0B92 to "o",
        0x0B93 to "o", 0x0B94 to "au"
    ),
    vowelSigns = table(
        0x0BBE to "aa", 0x0BBF to "i", 0x0BC0 to "ee", 0x0BC1 to "u", 0x0BC2 to "oo",
        0x0BC6 to "e", 0x0BC7 to "e", 0x0BC8 to "ai", 0x0BCA to "o", 0x0BCB to "o",
        0x0BCC to "au"
    ),
    signs = table(0x0B82 to "m", 0x0B83 to ""),
    virama = 0x0BCD
)

private val telugu = SouthIndianScript(
    consonants = table(
        0x0C15 to "k", 0x0C16 to "kh", 0x0C17 to "g", 0x0C18 to "gh", 0x0C19 to "ng",
        0x0C1A to "ch", 0x0C1B to "chh", 0x0C1C to "j", 0x0C1D to "jh", 0x0C1E to "ny",
        0x0C1F to "t", 0x0C20 to "th", 0x0C21 to "d", 0x0C22 to "dh", 0x0C23 to "n",
        0x0C24 to "t", 0x0C25 to "th", 0x0C26 to "d", 0x0C27 to "dh", 0x0C28 to "n",
        0x0C2A to "p", 0x0C2B to "ph", 0x0C2C to "b", 0x0C2D to "bh", 0x0C2E to "m",
        0x0C2F to "y", 0x0C30 to "r", 0x0C31 to "r", 0x0C32 to "l", 0x0C33 to "l",
        0x0C35 to "v", 0x0C36 to "sh", 0x0C37 to "sh", 0x0C38 to "s", 0x0C39 to "h"
    ),
    independentVowels = table(
        0x0C05 to "a", 0x0C06 to "aa", 0x0C07 to "i", 0x0C08 to "ee", 0x0C09 to "u",
        0x0C0A to "oo", 0x0C0B to "ru", 0x0C0E to "e", 0x0C0F to "e", 0x0C10 to "ai",
        0x0C12 to "o", 0x0C13 to "o", 0x0C14 to "au"
    ),
    vowelSigns = table(
        0x0C3E to "a", 0x0C3F to "i", 0x0C40 to "ee", 0x0C41 to "u", 0x0C42 to "oo",
        0x0C43 to "ru", 0x0C46 to "e", 0x0C47 to "e", 0x0C48 to "ai", 0x0C4A to "o",
        0x0C4B to "o", 0x0C4C to "au"
    ),
    signs = table(0x0C01 to "n", 0x0C02 to "m", 0x0C03 to "h"),
    virama = 0x0C4D
)

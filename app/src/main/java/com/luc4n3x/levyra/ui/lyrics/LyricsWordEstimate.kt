package com.luc4n3x.levyra.ui.lyrics

import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricWord

private const val MS_PER_WEIGHT = 85L
private const val WORD_BASE_WEIGHT = 1.5f
private const val MIN_WORD_MS = 60L
private val TOKEN_REGEX = Regex("\\S+")

fun lyricsWithEstimatedWords(lines: List<LyricLine>): List<LyricLine> {
    if (lines.all { it.words.isNotEmpty() || !it.canEstimateWords() }) return lines
    return lines.mapIndexed { index, line ->
        if (line.words.isNotEmpty() || !line.canEstimateWords()) {
            line
        } else {
            val nextStart = lines.subList(index + 1, lines.size).firstOrNull { it.startMs > line.startMs }?.startMs
            line.copy(words = estimatedLyricWords(line, nextStart))
        }
    }
}

internal fun estimatedLyricWords(line: LyricLine, nextStartMs: Long?): List<LyricWord> {
    val end = when {
        line.endMs > line.startMs -> line.endMs
        nextStartMs != null && nextStartMs > line.startMs -> nextStartMs
        else -> return emptyList()
    }
    val tokens = lyricTokens(line.text)
    if (tokens.isEmpty()) return emptyList()
    val weights = tokens.map { token -> token.count(Char::isLetterOrDigit).coerceAtLeast(1) + WORD_BASE_WEIGHT }
    val total = weights.sum()
    val span = end - line.startMs
    val sung = (total * MS_PER_WEIGHT).toLong()
        .coerceAtMost((span * 0.95f).toLong())
        .coerceAtLeast(minOf(span, tokens.size * MIN_WORD_MS))
    var cursor = line.startMs.toFloat()
    return tokens.mapIndexed { index, token ->
        val start = cursor
        cursor += sung * (weights[index] / total)
        LyricWord(start.toLong(), if (index == tokens.lastIndex) line.startMs + sung else cursor.toLong(), token)
    }
}

private fun LyricLine.canEstimateWords(): Boolean = !isMetadata && !isInstrumental && text.isNotBlank()

private fun lyricTokens(text: String): List<String> {
    val tokens = TOKEN_REGEX.findAll(text).map { it.value }.toList()
    return tokens.flatMap { token ->
        if (token.codePoints().anyMatch(::isUnspacedScript)) token.splitByCodePoint() else listOf(token)
    }
}

private fun String.splitByCodePoint(): List<String> {
    val parts = ArrayList<String>(length)
    var index = 0
    while (index < length) {
        val next = offsetByCodePoints(index, 1)
        val part = substring(index, next)
        if (parts.isNotEmpty() && !Character.isLetterOrDigit(part.codePointAt(0))) {
            parts[parts.lastIndex] = parts.last() + part
        } else {
            parts += part
        }
        index = next
    }
    return parts
}

private fun isUnspacedScript(codePoint: Int): Boolean = when (Character.UnicodeScript.of(codePoint)) {
    Character.UnicodeScript.HAN,
    Character.UnicodeScript.HIRAGANA,
    Character.UnicodeScript.KATAKANA,
    Character.UnicodeScript.THAI,
    Character.UnicodeScript.LAO,
    Character.UnicodeScript.KHMER,
    Character.UnicodeScript.MYANMAR -> true
    else -> false
}

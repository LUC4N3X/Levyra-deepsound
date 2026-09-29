package com.luc4n3x.levyra.ui.lyrics

import com.luc4n3x.levyra.domain.LyricWord

fun buildTimedLyricText(lineText: String, words: List<LyricWord>): TimedLyricText =
    alignTimedLyricText(lineText, words) ?: buildTimedLyricText(words)

fun buildTimedLyricText(words: List<LyricWord>): TimedLyricText {
    val sourceSpacing = words.any { word ->
        word.text.firstOrNull()?.isWhitespace() == true || word.text.lastOrNull()?.isWhitespace() == true
    }
    val text = StringBuilder()
    val timedWords = ArrayList<TimedLyricWord>(words.size)
    var previousEndedWithWhitespace = false
    words.forEach { word ->
        val value = word.text.trim()
        if (value.isNotBlank()) {
            val separated = if (sourceSpacing) {
                previousEndedWithWhitespace || word.text.first().isWhitespace()
            } else {
                !value.first().isPunctuationWithoutLeadingSpace()
            }
            if (text.isNotEmpty() && separated) text.append(' ')
            val startIndex = text.length
            text.append(value)
            timedWords += timedLyricWord(startIndex, value.length, word)
        }
        previousEndedWithWhitespace = word.text.lastOrNull()?.isWhitespace() == true
    }
    return TimedLyricText(text.toString(), timedWords)
}

private fun alignTimedLyricText(lineText: String, words: List<LyricWord>): TimedLyricText? {
    if (lineText.isBlank() || words.isEmpty()) return null
    val tokens = words.filter { it.text.isNotBlank() }
    val timedWords = ArrayList<TimedLyricWord>(tokens.size)
    var cursor = 0
    tokens.forEachIndexed { index, word ->
        val wordText = word.text.trim()
        val start = lineText.skipWhile(cursor) { it.isWhitespace() }
        val direct = matchLyricToken(lineText, start, wordText)
        val tokenStart = if (direct != null) start else lineText.skipWhile(start) { it.isLyricSeparator() }
        val end = direct ?: matchLyricToken(lineText, tokenStart, wordText) ?: return null
        val nextTokenStart = tokens.getOrNull(index + 1)?.text?.trim()?.firstOrNull()
        val extendedEnd = lineText.skipWhile(end) { it.isTrailingLyricPunctuation() && it != nextTokenStart }
        timedWords += timedLyricWord(tokenStart, extendedEnd - tokenStart, word)
        cursor = extendedEnd
    }
    for (index in cursor until lineText.length) {
        if (!lineText[index].isLyricSeparator()) return null
    }
    return if (timedWords.isEmpty()) null else TimedLyricText(lineText, timedWords)
}

private inline fun String.skipWhile(start: Int, predicate: (Char) -> Boolean): Int {
    var index = start
    while (index < length && predicate(this[index])) index++
    return index
}

private fun matchLyricToken(text: String, start: Int, token: String): Int? {
    var textIndex = start
    var tokenIndex = 0
    while (tokenIndex < token.length) {
        val expected = token[tokenIndex]
        if (expected.isWhitespace()) {
            tokenIndex++
            continue
        }
        while (textIndex < text.length && text[textIndex].isWhitespace()) textIndex++
        if (textIndex >= text.length || !text[textIndex].equals(expected, ignoreCase = true)) return null
        textIndex++
        tokenIndex++
    }
    return textIndex
}

private fun timedLyricWord(startIndex: Int, length: Int, word: LyricWord) = TimedLyricWord(
    startIndex = startIndex,
    length = length,
    startMs = word.startMs,
    endMs = word.endMs.coerceAtLeast(word.startMs + 1L)
)

private fun Char.isLyricSeparator(): Boolean = !isLetterOrDigit() && !isSurrogate()

private fun Char.isTrailingLyricPunctuation(): Boolean = !isWhitespace() && !isLetterOrDigit() && !isSurrogate()

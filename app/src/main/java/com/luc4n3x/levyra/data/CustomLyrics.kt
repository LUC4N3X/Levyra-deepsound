package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LyricLine

const val CUSTOM_LYRICS_PROVIDER = "custom"

sealed interface CustomLyricsParse {
    data object Empty : CustomLyricsParse
    data object TooLong : CustomLyricsParse
    data object Invalid : CustomLyricsParse
    data class Valid(
        val lines: List<LyricLine>,
        val synced: Boolean,
        val ignoredLines: Int
    ) : CustomLyricsParse
}

object CustomLyricsParser {
    const val MAX_SOURCE_CHARS = 200_000

    private val timestampLine = Regex("""^\s*\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?]""")
    private val anyTimestampLine = Regex("""(?m)^\s*\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?]""")
    private val metadataTagLine = Regex("""^\s*\[[A-Za-z#][^\]]*]\s*$""")

    fun parse(source: String): CustomLyricsParse {
        if (source.isBlank()) return CustomLyricsParse.Empty
        if (source.length > MAX_SOURCE_CHARS) return CustomLyricsParse.TooLong
        val detected = UnifiedLyricsParser.detect(source)
        val format = if (detected == LyricsFormat.PLAIN && anyTimestampLine.containsMatchIn(source)) LyricsFormat.LRC else detected
        val lines = UnifiedLyricsParser.parse(source, format)
        if (lines.isEmpty()) return CustomLyricsParse.Invalid
        val ignored = if (format == LyricsFormat.LRC) {
            source.lineSequence().count { line ->
                line.isNotBlank() && !timestampLine.containsMatchIn(line) && !metadataTagLine.matches(line)
            }
        } else {
            0
        }
        return CustomLyricsParse.Valid(lines, synced = format != LyricsFormat.PLAIN, ignoredLines = ignored)
    }
}

package com.luc4n3x.levyra.desktop.core.lyrics

data class LyricLine(val timeMs: Long, val text: String)

data class Lyrics(
    val lines: List<LyricLine>,
    val plainText: String,
    val synced: Boolean,
    val source: String
) {
    val isEmpty: Boolean get() = lines.isEmpty() && plainText.isBlank()

    fun activeIndex(positionMs: Long): Int {
        if (!synced || lines.isEmpty()) return -1
        var active = -1
        for (index in lines.indices) {
            if (lines[index].timeMs <= positionMs) active = index else break
        }
        return active
    }
}

object LrcParser {
    private val timestamp = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

    fun parse(raw: String): List<LyricLine> {
        if (raw.isBlank()) return emptyList()
        val lines = ArrayList<LyricLine>()
        raw.lineSequence().forEach { line ->
            val matches = timestamp.findAll(line).toList()
            if (matches.isEmpty()) return@forEach
            val text = line.substring(matches.last().range.last + 1).trim()
            matches.forEach { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: return@forEach
                val seconds = match.groupValues[2].toLongOrNull() ?: return@forEach
                val fractionRaw = match.groupValues[3]
                val fraction = when (fractionRaw.length) {
                    0 -> 0L
                    1 -> (fractionRaw.toLongOrNull() ?: 0L) * 100L
                    2 -> (fractionRaw.toLongOrNull() ?: 0L) * 10L
                    else -> fractionRaw.take(3).toLongOrNull() ?: 0L
                }
                lines += LyricLine(
                    timeMs = minutes * 60_000L + seconds * 1_000L + fraction,
                    text = text
                )
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    fun plainLines(raw: String): List<LyricLine> = raw.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { LyricLine(timeMs = 0L, text = it) }
        .toList()
}

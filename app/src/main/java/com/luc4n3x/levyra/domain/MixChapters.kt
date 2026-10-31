package com.luc4n3x.levyra.domain

enum class MixChapterSource { STRUCTURED, DESCRIPTION }

data class MixChapter(
    val startMs: Long,
    val title: String
)

data class MixChapterList(
    val trackId: String,
    val durationMs: Long,
    val source: MixChapterSource,
    val chapters: List<MixChapter>
) {
    fun indexAt(positionMs: Long): Int = MixChapterParser.indexAt(chapters, positionMs)

    fun endMs(index: Int): Long = chapters.getOrNull(index + 1)?.startMs ?: durationMs
}

data class RawMixChapter(val startMs: Long, val title: String)

object MixChapterParser {
    const val MIN_CHAPTERS = 2
    const val MAX_CHAPTERS = 1000
    private const val MIN_SPACING_MS = 1_000L

    private val timestampLine = Regex(
        """^[\s\-–—•*·>\[(]*(\d{1,3}:\d{2}(?::\d{2})?)[\s\])]*(?:[-–—:|.•·]\s*)?(.*)$"""
    )
    private val trailingTimestampLine = Regex(
        """^(.*?\S)[\s\-–—:|\[(]+(\d{1,3}:\d{2}(?::\d{2})?)[\s\])]*$"""
    )
    private val listIndexPrefix = Regex("""^\d{1,3}[.)]\s+""")

    fun parseTimestamp(value: String): Long? {
        val parts = value.trim().split(':')
        if (parts.size !in 2..3) return null
        val numbers = parts.map { it.toLongOrNull() ?: return null }
        if (numbers.any { it < 0 }) return null
        return if (numbers.size == 2) {
            if (numbers[1] > 59) return null
            (numbers[0] * 60 + numbers[1]) * 1000
        } else {
            if (numbers[1] > 59 || numbers[2] > 59) return null
            (numbers[0] * 3600 + numbers[1] * 60 + numbers[2]) * 1000
        }
    }

    fun parseDescription(description: String, durationMs: Long): List<MixChapter> {
        if (description.isBlank()) return emptyList()
        val raw = description.lineSequence().mapNotNull(::parseLine).toList()
        return normalize(raw, durationMs, requireLeadingStart = true)
    }

    fun normalize(
        raw: List<RawMixChapter>,
        durationMs: Long,
        requireLeadingStart: Boolean = false
    ): List<MixChapter> {
        val valid = raw
            .asSequence()
            .map { RawMixChapter(it.startMs, it.title.trim()) }
            .filter { it.title.isNotEmpty() && it.startMs >= 0L }
            .filter { durationMs <= 0L || it.startMs < durationMs }
            .sortedBy(RawMixChapter::startMs)
            .toList()
        if (valid.isEmpty()) return emptyList()
        if (requireLeadingStart && valid.first().startMs > 0L) return emptyList()
        val result = ArrayList<MixChapter>(valid.size)
        for (entry in valid) {
            val previous = result.lastOrNull()
            if (previous != null && entry.startMs - previous.startMs < MIN_SPACING_MS) continue
            result += MixChapter(entry.startMs, entry.title)
            if (result.size == MAX_CHAPTERS) break
        }
        return if (result.size >= MIN_CHAPTERS) result else emptyList()
    }

    fun indexAt(chapters: List<MixChapter>, positionMs: Long): Int {
        if (chapters.isEmpty()) return -1
        var low = 0
        var high = chapters.lastIndex
        var answer = 0
        while (low <= high) {
            val middle = (low + high) ushr 1
            if (chapters[middle].startMs <= positionMs) {
                answer = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        return answer
    }

    private fun parseLine(line: String): RawMixChapter? {
        val clean = line.trim()
        if (clean.isEmpty() || clean.length > 200) return null
        val withoutIndex = listIndexPrefix.replace(clean, "")
        timestampLine.matchEntire(withoutIndex)?.let { match ->
            val start = parseTimestamp(match.groupValues[1]) ?: return null
            val title = cleanTitle(match.groupValues[2])
            if (title.isNotEmpty()) return RawMixChapter(start, title)
        }
        trailingTimestampLine.matchEntire(withoutIndex)?.let { match ->
            val start = parseTimestamp(match.groupValues[2]) ?: return null
            val title = cleanTitle(match.groupValues[1])
            if (title.isNotEmpty()) return RawMixChapter(start, title)
        }
        return null
    }

    private fun cleanTitle(value: String): String =
        value.trim().trim('-', '–', '—', ':', '|', '•', '·', '[', ']', '(', ')').trim()
}

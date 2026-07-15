package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricSection
import com.luc4n3x.levyra.domain.LyricSectionType
import com.luc4n3x.levyra.domain.LyricVocalRole
import java.text.Normalizer
import java.util.Locale

internal data class LyricsSectionDetection(
    val lines: List<LyricLine>,
    val sections: List<LyricSection>
)

internal object LyricsSectionDetector {
    private val markerRegex = Regex(
        "(?i)^\\s*[\\[(]?(intro|introduction|verse|strofa|couplet|strophe|verso|pre[- ]?chorus|pre[- ]?ritornello|pre[- ]?refrain|chorus|ritornello|refrain|estribillo|bridge|ponte|puente|brücke|instrumental|interlude|break|outro|finale)\\s*(\\d+)?\\s*[\\])]?$"
    )

    fun detect(source: List<LyricLine>): LyricsSectionDetection {
        if (source.isEmpty()) return LyricsSectionDetection(emptyList(), emptyList())
        val explicit = extractExplicit(source)
        if (explicit.sections.isNotEmpty()) return explicit
        return infer(source)
    }

    private fun extractExplicit(source: List<LyricLine>): LyricsSectionDetection {
        val cleanLines = ArrayList<LyricLine>(source.size)
        val markers = ArrayList<PendingMarker>()
        var pending: PendingMarker? = null
        source.forEach { line ->
            val type = markerType(line.text)
            if (type != null && line.role == LyricVocalRole.MAIN && line.words.isEmpty()) {
                pending = PendingMarker(type, line.startMs)
            } else {
                val targetIndex = cleanLines.size
                pending?.let {
                    markers += it.copy(startLineIndex = targetIndex)
                    pending = null
                }
                cleanLines += line
            }
        }
        if (cleanLines.isEmpty() || markers.isEmpty()) return LyricsSectionDetection(source, emptyList())
        val sections = markers.mapIndexedNotNull { index, marker ->
            val startIndex = marker.startLineIndex.coerceIn(0, cleanLines.lastIndex)
            val nextIndex = markers.getOrNull(index + 1)?.startLineIndex?.coerceAtMost(cleanLines.size) ?: cleanLines.size
            val endIndex = (nextIndex - 1).coerceAtLeast(startIndex)
            buildSection(marker.type, startIndex, endIndex, cleanLines, 100)
        }
        return LyricsSectionDetection(cleanLines, withOrdinals(sections))
    }

    private fun infer(source: List<LyricLine>): LyricsSectionDetection {
        val primaryIndices = source.indices.filter { source[it].role != LyricVocalRole.BACKGROUND }
        if (primaryIndices.isEmpty()) return LyricsSectionDetection(source, emptyList())
        val normalized = primaryIndices.map { normalize(source[it].text) }
        val chorusBlocks = repeatedBlocks(normalized)
        val ranges = ArrayList<SectionRange>()
        chorusBlocks.forEach { block ->
            block.occurrences.forEach { occurrence ->
                val start = primaryIndices[occurrence]
                val endPrimary = (occurrence + block.length - 1).coerceAtMost(primaryIndices.lastIndex)
                val end = primaryIndices[endPrimary]
                ranges += SectionRange(LyricSectionType.CHORUS, start, end, block.confidence)
            }
        }
        val mergedChoruses = mergeRanges(ranges.filter { it.type == LyricSectionType.CHORUS })
        val inferred = if (mergedChoruses.isEmpty()) {
            inferWithoutChorus(source, primaryIndices)
        } else {
            inferAroundChoruses(source, primaryIndices, mergedChoruses)
        }
        val sections = inferred
            .sortedBy { it.start }
            .mapNotNull { range -> buildSection(range.type, range.start, range.end, source, range.confidence) }
        return LyricsSectionDetection(source, withOrdinals(sections))
    }

    private fun inferWithoutChorus(source: List<LyricLine>, primaryIndices: List<Int>): List<SectionRange> {
        val ranges = ArrayList<SectionRange>()
        var cursor = primaryIndices.first()
        val leadingInstrumental = primaryIndices.takeWhile { source[it].isInstrumental }
        if (leadingInstrumental.isNotEmpty()) {
            ranges += SectionRange(LyricSectionType.INTRO, leadingInstrumental.first(), leadingInstrumental.last(), 72)
            cursor = nextPrimaryAfter(primaryIndices, leadingInstrumental.last()) ?: return ranges
        }
        val remaining = primaryIndices.filter { it >= cursor }
        if (remaining.isEmpty()) return ranges
        val trailingInstrumental = remaining.takeLastWhile { source[it].isInstrumental }
        val lyrical = if (trailingInstrumental.isEmpty()) remaining else remaining.dropLast(trailingInstrumental.size)
        if (lyrical.isNotEmpty()) {
            ranges += SectionRange(LyricSectionType.VERSE, lyrical.first(), lyrical.last(), 54)
        }
        if (trailingInstrumental.isNotEmpty()) {
            ranges += SectionRange(LyricSectionType.OUTRO, trailingInstrumental.first(), trailingInstrumental.last(), 72)
        }
        return ranges
    }

    private fun inferAroundChoruses(
        source: List<LyricLine>,
        primaryIndices: List<Int>,
        choruses: List<SectionRange>
    ): List<SectionRange> {
        val result = ArrayList<SectionRange>()
        val firstPrimary = primaryIndices.first()
        val lastPrimary = primaryIndices.last()
        var cursor = firstPrimary
        choruses.forEachIndexed { chorusIndex, chorus ->
            if (cursor < chorus.start) {
                val gap = primaryIndices.filter { it in cursor until chorus.start }
                if (gap.isNotEmpty()) {
                    val leadingInstrumental = if (chorusIndex == 0) gap.takeWhile { source[it].isInstrumental } else emptyList()
                    var gapStart = gap.first()
                    if (leadingInstrumental.isNotEmpty()) {
                        result += SectionRange(LyricSectionType.INTRO, leadingInstrumental.first(), leadingInstrumental.last(), 74)
                        gapStart = nextPrimaryAfter(gap, leadingInstrumental.last()) ?: chorus.start
                    }
                    val usable = gap.filter { it >= gapStart }
                    if (usable.isNotEmpty()) {
                        val preCount = if (usable.size >= 6) 2 else if (usable.size >= 4) 1 else 0
                        val verseEndPosition = usable.size - preCount - 1
                        val segmentType = if (chorusIndex >= 2 && choruses.size >= 3) LyricSectionType.BRIDGE else LyricSectionType.VERSE
                        if (verseEndPosition >= 0) {
                            result += SectionRange(segmentType, usable.first(), usable[verseEndPosition], if (segmentType == LyricSectionType.BRIDGE) 63 else 66)
                        }
                        if (preCount > 0) {
                            result += SectionRange(LyricSectionType.PRE_CHORUS, usable[usable.size - preCount], usable.last(), 64)
                        }
                    }
                }
            }
            result += chorus
            cursor = nextPrimaryAfter(primaryIndices, chorus.end) ?: (lastPrimary + 1)
        }
        if (cursor <= lastPrimary) {
            val tail = primaryIndices.filter { it >= cursor }
            if (tail.isNotEmpty()) {
                val type = if (tail.size <= 4 || source[tail.first()].startMs >= source[lastPrimary].endMs * 4L / 5L) {
                    LyricSectionType.OUTRO
                } else {
                    LyricSectionType.BRIDGE
                }
                result += SectionRange(type, tail.first(), tail.last(), if (type == LyricSectionType.OUTRO) 68 else 58)
            }
        }
        return result
    }

    private fun repeatedBlocks(lines: List<String>): List<RepeatedBlock> {
        if (lines.size < 6) return emptyList()
        val candidates = ArrayList<RepeatedBlock>()
        for (length in 4 downTo 2) {
            val occurrencesByKey = LinkedHashMap<String, MutableList<Int>>()
            for (start in 0..lines.size - length) {
                val slice = lines.subList(start, start + length)
                if (slice.any { it.length < 3 }) continue
                val key = slice.joinToString("\u0001")
                occurrencesByKey.getOrPut(key) { ArrayList() } += start
            }
            occurrencesByKey.values.forEach { positions ->
                val spaced = ArrayList<Int>()
                positions.forEach { position ->
                    if (spaced.none { previous -> kotlin.math.abs(previous - position) < length + 2 }) spaced += position
                }
                if (spaced.size >= 2) {
                    candidates += RepeatedBlock(length, spaced, (74 + length * 5 + (spaced.size - 2) * 4).coerceAtMost(96))
                }
            }
        }
        if (candidates.isEmpty()) return emptyList()
        val selected = ArrayList<RepeatedBlock>()
        candidates.sortedWith(compareByDescending<RepeatedBlock> { it.confidence }.thenByDescending { it.length }).forEach { candidate ->
            val overlaps = selected.any { existing ->
                candidate.occurrences.any { current ->
                    existing.occurrences.any { previous ->
                        current < previous + existing.length && previous < current + candidate.length
                    }
                }
            }
            if (!overlaps) selected += candidate
        }
        return selected.take(2)
    }

    private fun markerType(text: String): LyricSectionType? {
        val match = markerRegex.matchEntire(text.trim()) ?: return null
        return when (match.groupValues[1].lowercase(Locale.ROOT).replace(" ", "").replace("-", "")) {
            "intro", "introduction" -> LyricSectionType.INTRO
            "verse", "strofa", "couplet", "strophe", "verso" -> LyricSectionType.VERSE
            "prechorus", "preritornello", "prerefrain" -> LyricSectionType.PRE_CHORUS
            "chorus", "ritornello", "refrain", "estribillo" -> LyricSectionType.CHORUS
            "bridge", "ponte", "puente", "brücke" -> LyricSectionType.BRIDGE
            "instrumental", "interlude", "break" -> LyricSectionType.INSTRUMENTAL
            "outro", "finale" -> LyricSectionType.OUTRO
            else -> null
        }
    }

    private fun buildSection(
        type: LyricSectionType,
        startIndex: Int,
        endIndex: Int,
        lines: List<LyricLine>,
        confidence: Int
    ): LyricSection? {
        if (startIndex !in lines.indices || endIndex !in lines.indices || endIndex < startIndex) return null
        return LyricSection(
            type = type,
            ordinal = 1,
            startLineIndex = startIndex,
            endLineIndex = endIndex,
            startMs = lines[startIndex].startMs,
            endMs = lines[endIndex].endMs,
            confidence = confidence.coerceIn(0, 100)
        )
    }

    private fun withOrdinals(sections: List<LyricSection>): List<LyricSection> {
        val counts = HashMap<LyricSectionType, Int>()
        return sections.map { section ->
            val ordinal = counts.getOrDefault(section.type, 0) + 1
            counts[section.type] = ordinal
            section.copy(ordinal = ordinal)
        }
    }

    private fun mergeRanges(ranges: List<SectionRange>): List<SectionRange> {
        if (ranges.isEmpty()) return emptyList()
        val sorted = ranges.sortedBy { it.start }
        val out = ArrayList<SectionRange>()
        sorted.forEach { range ->
            val previous = out.lastOrNull()
            if (previous != null && range.start <= previous.end) {
                out[out.lastIndex] = previous.copy(end = maxOf(previous.end, range.end), confidence = maxOf(previous.confidence, range.confidence))
            } else {
                out += range
            }
        }
        return out
    }

    private fun nextPrimaryAfter(indices: List<Int>, index: Int): Int? = indices.firstOrNull { it > index }

    private fun normalize(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N} ]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private data class PendingMarker(
        val type: LyricSectionType,
        val timestampMs: Long,
        val startLineIndex: Int = 0
    )

    private data class SectionRange(
        val type: LyricSectionType,
        val start: Int,
        val end: Int,
        val confidence: Int
    )

    private data class RepeatedBlock(
        val length: Int,
        val occurrences: List<Int>,
        val confidence: Int
    )
}

package com.luc4n3x.levyra.ui.lyrics

import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricVocalRole

const val LYRICS_INSTRUMENTAL_MIN_GAP_MS = 5_000L
const val LYRICS_INSTRUMENTAL_LEAD_IN_MS = 600L
const val LYRICS_INSTRUMENTAL_LEAD_OUT_MS = 500L

data class LyricsInstrumentalGap(
    val startMs: Long,
    val endMs: Long,
    val nextLineIndex: Int
) {
    val durationMs: Long = (endMs - startMs).coerceAtLeast(1L)
}

fun lyricsInstrumentalGaps(
    lines: List<LyricLine>,
    synced: Boolean,
    minGapMs: Long = LYRICS_INSTRUMENTAL_MIN_GAP_MS
): List<LyricsInstrumentalGap> {
    if (!synced || lines.size < 2) return emptyList()
    val gaps = ArrayList<LyricsInstrumentalGap>()
    var previousEndMs = Long.MIN_VALUE
    lines.forEachIndexed { index, line ->
        if (line.role == LyricVocalRole.BACKGROUND || line.isMetadata) return@forEachIndexed
        if (previousEndMs != Long.MIN_VALUE && line.startMs - previousEndMs >= minGapMs) {
            gaps += LyricsInstrumentalGap(
                startMs = previousEndMs,
                endMs = line.startMs,
                nextLineIndex = index
            )
        }
        previousEndMs = maxOf(previousEndMs, line.endMs)
    }
    return gaps
}

fun activeLyricsInstrumentalGap(positionMs: Long, gaps: List<LyricsInstrumentalGap>): LyricsInstrumentalGap? {
    if (gaps.isEmpty()) return null
    var low = 0
    var high = gaps.lastIndex
    while (low <= high) {
        val middle = (low + high) ushr 1
        val gap = gaps[middle]
        when {
            positionMs < gap.startMs + LYRICS_INSTRUMENTAL_LEAD_IN_MS -> high = middle - 1
            positionMs > gap.endMs - LYRICS_INSTRUMENTAL_LEAD_OUT_MS -> low = middle + 1
            else -> return gap
        }
    }
    return null
}

fun lyricsInstrumentalProgress(positionMs: Long, gap: LyricsInstrumentalGap): Float =
    ((positionMs - gap.startMs).toFloat() / gap.durationMs.toFloat()).coerceIn(0f, 1f)

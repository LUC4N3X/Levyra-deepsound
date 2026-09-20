package com.luc4n3x.levyra.ui.lyrics

import com.luc4n3x.levyra.data.MAX_LYRICS_OFFSET_MS
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricVocalRole

const val LYRICS_OFFSET_STEP_MS = 500L

fun lyricsOffsetPosition(positionMs: Long, offsetMs: Long): Long =
    (positionMs - offsetMs).coerceAtLeast(0L)

fun adjustLyricsOffset(offsetMs: Long, deltaMs: Long): Long =
    (offsetMs + deltaMs).coerceIn(-MAX_LYRICS_OFFSET_MS, MAX_LYRICS_OFFSET_MS)

fun activeLyricIndex(positionMs: Long, lines: List<LyricLine>): Int {
    if (lines.isEmpty()) return -1
    var low = 0
    var high = lines.lastIndex
    var candidate = -1
    while (low <= high) {
        val middle = (low + high) ushr 1
        if (lines[middle].startMs <= positionMs) {
            candidate = middle
            low = middle + 1
        } else {
            high = middle - 1
        }
    }
    if (candidate < 0) return -1
    val searchStart = 0
    for (index in candidate downTo searchStart) {
        val line = lines[index]
        if (line.role != LyricVocalRole.BACKGROUND && positionMs in line.startMs..line.endMs) return index
    }
    for (index in candidate downTo searchStart) {
        val line = lines[index]
        if (positionMs in line.startMs..line.endMs) return index
    }
    for (index in candidate downTo searchStart) {
        if (lines[index].role != LyricVocalRole.BACKGROUND) return index
    }
    return candidate
}

package com.luc4n3x.levyra.ui.lyrics

import com.luc4n3x.levyra.data.MAX_LYRICS_OFFSET_MS
import com.luc4n3x.levyra.data.MAX_LYRICS_TRACK_OFFSET_MS
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricVocalRole

const val LYRICS_OFFSET_STEP_MS = 500L
const val LYRICS_OFFSET_JUMP_MS = 5_000L

fun lyricsOffsetPosition(positionMs: Long, offsetMs: Long): Long =
    (positionMs - offsetMs).coerceAtLeast(0L)

fun adjustLyricsOffset(offsetMs: Long, deltaMs: Long): Long =
    (offsetMs + deltaMs).coerceIn(-MAX_LYRICS_OFFSET_MS, MAX_LYRICS_OFFSET_MS)

fun adjustLyricsTrackOffset(offsetMs: Long, deltaMs: Long): Long =
    (offsetMs + deltaMs).coerceIn(-MAX_LYRICS_TRACK_OFFSET_MS, MAX_LYRICS_TRACK_OFFSET_MS)

fun snapLyricsTrackOffset(offsetMs: Long): Long {
    val clamped = offsetMs.coerceIn(-MAX_LYRICS_TRACK_OFFSET_MS, MAX_LYRICS_TRACK_OFFSET_MS)
    val steps = Math.round(clamped.toDouble() / LYRICS_OFFSET_STEP_MS)
    return steps * LYRICS_OFFSET_STEP_MS
}

fun calibratedLyricsTrackOffset(positionMs: Long, lineStartMs: Long, latencyOffsetMs: Long): Long =
    adjustLyricsTrackOffset(0L, positionMs - lineStartMs - latencyOffsetMs)

fun parseLyricsOffsetSeconds(input: String): Long? {
    val normalized = input.trim()
        .replace('−', '-')
        .replace(',', '.')
        .removeSuffix("s")
        .trim()
    if (normalized.isEmpty() || normalized.length > 8) return null
    if (!normalized.matches(LYRICS_OFFSET_INPUT_PATTERN)) return null
    val seconds = normalized.toDoubleOrNull()?.takeIf(Double::isFinite) ?: return null
    val millis = Math.round(seconds * 1_000.0)
    if (millis !in -MAX_LYRICS_TRACK_OFFSET_MS..MAX_LYRICS_TRACK_OFFSET_MS) return null
    return millis
}

fun formatLyricsOffsetSeconds(offsetMs: Long): String {
    val sign = when {
        offsetMs > 0L -> "+"
        offsetMs < 0L -> "−"
        else -> ""
    }
    val absolute = kotlin.math.abs(offsetMs)
    val tenths = absolute % 1_000L / 100L
    return "$sign${absolute / 1_000L}.$tenths s"
}

fun formatLyricsOffsetInput(offsetMs: Long): String {
    val absolute = kotlin.math.abs(offsetMs)
    val sign = if (offsetMs < 0L) "-" else ""
    val hundredths = absolute % 1_000L / 10L
    val fraction = when {
        hundredths == 0L -> ""
        hundredths % 10L == 0L -> ".${hundredths / 10L}"
        else -> ".${hundredths.toString().padStart(2, '0')}"
    }
    return "$sign${absolute / 1_000L}$fraction"
}

private val LYRICS_OFFSET_INPUT_PATTERN = Regex("""^[+-]?(\d{1,2}(\.\d{0,3})?|\.\d{1,3})$""")

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

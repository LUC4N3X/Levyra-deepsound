package com.luc4n3x.levyra.ui.lyrics.apple

import androidx.compose.runtime.Immutable
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricVocalRole
import com.luc4n3x.levyra.domain.LyricWord
import kotlin.math.exp
import kotlin.math.roundToLong
import kotlin.math.sqrt

const val APPLE_LYRICS_INSTRUMENTAL_MIN_GAP_MS = 7_000L
const val APPLE_LYRICS_MOVE_MIN_MS = 480L
const val APPLE_LYRICS_MOVE_MAX_MS = 750L
private const val APPLE_LYRICS_GAP_MIN_MS = 200L
private const val APPLE_LYRICS_GAP_MAX_MS = 750L
private const val APPLE_LYRICS_STAGGER_BASE_MS = 25f
private const val APPLE_LYRICS_STAGGER_MAX_STEPS = 4
private const val APPLE_LYRICS_BACKGROUND_ATTACH_SLACK_MS = 1_500L
const val APPLE_LYRICS_EMPHASIS_MIN_MS = 1_000L
private const val APPLE_LYRICS_EMPHASIS_MAX_MS = 2_000L
private const val APPLE_LYRICS_EMPHASIS_MAX_LETTERS = 12
const val APPLE_LYRICS_EMPHASIS_MAX_SCALE = 1.14f
private const val APPLE_LYRICS_LIFT_DAMPING = 0.93
private const val APPLE_LYRICS_LIFT_STIFFNESS = 25.0

enum class AppleLyricsSide { START, END }

@Immutable
sealed interface AppleLyricsRow {
    val key: String
    val startMs: Long
    val endMs: Long

    @Immutable
    data class Vocal(
        override val key: String,
        val main: LyricLine,
        val sourceIndex: Int,
        val background: List<LyricLine>,
        val side: AppleLyricsSide,
        val duet: Boolean
    ) : AppleLyricsRow {
        override val startMs: Long get() = main.startMs
        override val endMs: Long get() = maxOf(main.endMs, background.maxOfOrNull { it.endMs } ?: main.endMs)
    }

    @Immutable
    data class Instrumental(
        override val key: String,
        override val startMs: Long,
        override val endMs: Long
    ) : AppleLyricsRow
}

fun appleLyricsRows(lines: List<LyricLine>, synced: Boolean, showSecondaryVoices: Boolean): List<AppleLyricsRow> {
    if (lines.none { !it.isMetadata && it.text.isNotBlank() }) return emptyList()
    val vocals = appleVocalRows(lines, synced, showSecondaryVoices)
    return if (synced) appleRowsWithInstrumentals(vocals) else vocals
}

private fun LyricLine.skippedByAppleLyrics(synced: Boolean): Boolean =
    isMetadata || isInstrumental || text.isMusicNoteOnly() || (synced && text.isBlank())

private fun appleVocalRows(lines: List<LyricLine>, synced: Boolean, showSecondaryVoices: Boolean): List<AppleLyricsRow.Vocal> {
    val duet = lines.any { it.role == LyricVocalRole.DUET_LEFT || it.role == LyricVocalRole.DUET_RIGHT }
    val vocals = ArrayList<AppleLyricsRow.Vocal>(lines.size)
    val pendingBackground = ArrayList<LyricLine>()
    lines.forEachIndexed { index, line ->
        if (line.skippedByAppleLyrics(synced)) return@forEachIndexed
        if (line.role != LyricVocalRole.BACKGROUND) {
            vocals += appleVocalRow(line, index, pendingBackground.toList(), duet)
            pendingBackground.clear()
        } else if (showSecondaryVoices) {
            placeBackgroundLine(vocals, pendingBackground, line, index, synced, duet)
        }
    }
    pendingBackground.forEachIndexed { offset, line ->
        vocals += appleVocalRow(line, lines.indexOf(line).takeIf { it >= 0 } ?: (lines.size + offset), emptyList(), duet)
    }
    return vocals
}

private fun placeBackgroundLine(
    vocals: MutableList<AppleLyricsRow.Vocal>,
    pending: MutableList<LyricLine>,
    line: LyricLine,
    index: Int,
    synced: Boolean,
    duet: Boolean
) {
    val owner = vocals.lastOrNull()
    when {
        !synced -> vocals += appleVocalRow(line, index, emptyList(), duet)
        owner != null && line.startMs <= owner.main.endMs + APPLE_LYRICS_BACKGROUND_ATTACH_SLACK_MS ->
            vocals[vocals.lastIndex] = owner.copy(background = owner.background + line)
        else -> pending += line
    }
}

private fun appleRowsWithInstrumentals(vocals: List<AppleLyricsRow.Vocal>): List<AppleLyricsRow> {
    val sorted = vocals.sortedBy { it.startMs }
    val rows = ArrayList<AppleLyricsRow>(sorted.size + 4)
    val first = sorted.firstOrNull()
    if (first != null && first.startMs >= APPLE_LYRICS_INSTRUMENTAL_MIN_GAP_MS) {
        rows += AppleLyricsRow.Instrumental("intro", 0L, first.startMs)
    }
    var previousEnd = Long.MIN_VALUE
    sorted.forEach { row ->
        if (previousEnd != Long.MIN_VALUE && row.startMs - previousEnd >= APPLE_LYRICS_INSTRUMENTAL_MIN_GAP_MS) {
            rows += AppleLyricsRow.Instrumental("gap-$previousEnd-${row.startMs}", previousEnd, row.startMs)
        }
        rows += row
        previousEnd = maxOf(previousEnd, row.endMs)
    }
    return rows
}

private fun String.isMusicNoteOnly(): Boolean =
    isNotBlank() && all { it.isWhitespace() || it in "♪♫♬♩🎵🎶" || it.isSurrogate() } && none { it.isLetterOrDigit() }

private fun appleVocalRow(line: LyricLine, index: Int, background: List<LyricLine>, duet: Boolean) = AppleLyricsRow.Vocal(
    key = "v-$index-${line.startMs}",
    main = line,
    sourceIndex = index,
    background = background,
    side = if (line.role == LyricVocalRole.DUET_RIGHT) AppleLyricsSide.END else AppleLyricsSide.START,
    duet = duet
)

fun appleLyricsMoveDurationMs(gapMs: Long): Long {
    val clamped = gapMs.coerceIn(APPLE_LYRICS_GAP_MIN_MS, APPLE_LYRICS_GAP_MAX_MS)
    val fraction = (clamped - APPLE_LYRICS_GAP_MIN_MS).toFloat() / (APPLE_LYRICS_GAP_MAX_MS - APPLE_LYRICS_GAP_MIN_MS)
    return (APPLE_LYRICS_MOVE_MIN_MS + (APPLE_LYRICS_MOVE_MAX_MS - APPLE_LYRICS_MOVE_MIN_MS) * fraction).roundToLong()
}

fun appleLyricsLeadMs(rows: List<AppleLyricsRow>, index: Int): Long {
    val row = rows.getOrNull(index) ?: return 0L
    if (row is AppleLyricsRow.Instrumental) return 0L
    val previous = rows.getOrNull(index - 1) ?: return 0L
    return appleLyricsMoveDurationMs(row.startMs - previous.endMs)
}

fun appleLyricsLeads(rows: List<AppleLyricsRow>): LongArray = LongArray(rows.size) { appleLyricsLeadMs(rows, it) }

fun appleLyricsFocusIndex(rows: List<AppleLyricsRow>, leads: LongArray, positionMs: Long): Int {
    if (rows.isEmpty()) return -1
    var low = 0
    var high = rows.lastIndex
    var candidate = -1
    while (low <= high) {
        val middle = low + high ushr 1
        if (rows[middle].startMs - leads[middle] <= positionMs) {
            candidate = middle
            low = middle + 1
        } else {
            high = middle - 1
        }
    }
    return candidate
}

fun appleLyricsConcurrentIndices(rows: List<AppleLyricsRow>, focus: Int, positionMs: Long): IntRange {
    if (focus < 0) return IntRange.EMPTY
    var first = focus
    while (first > 0) {
        val previous = rows[first - 1]
        if (previous is AppleLyricsRow.Vocal && positionMs < previous.endMs && rows[first] is AppleLyricsRow.Vocal) first-- else break
    }
    return first..focus
}

fun appleLyricsStaggerDelayMs(rowIndex: Int, focusIndex: Int): Long {
    if (focusIndex < 0 || rowIndex <= focusIndex) return 0L
    val steps = minOf(rowIndex - focusIndex, APPLE_LYRICS_STAGGER_MAX_STEPS)
    val weight = 5.0 * steps - steps * (steps + 1) / 2.0
    return (APPLE_LYRICS_STAGGER_BASE_MS * 0.25 * weight).roundToLong()
}

fun appleLyricsEmphasized(word: LyricWord): Boolean {
    val duration = word.endMs - word.startMs
    val letters = word.text.count { it.isLetter() }
    return duration >= APPLE_LYRICS_EMPHASIS_MIN_MS && letters in 2..APPLE_LYRICS_EMPHASIS_MAX_LETTERS
}

fun appleLyricsEmphasisScale(durationMs: Long): Float {
    val seconds = durationMs.coerceIn(APPLE_LYRICS_EMPHASIS_MIN_MS, APPLE_LYRICS_EMPHASIS_MAX_MS) / 1000f
    return 1f + (APPLE_LYRICS_EMPHASIS_MAX_SCALE - 1f) * (seconds - 1f)
}

fun appleLyricsLiftFraction(elapsedMs: Long): Float {
    if (elapsedMs <= 0L) return 0f
    val t = elapsedMs / 1000.0
    val omega = sqrt(APPLE_LYRICS_LIFT_STIFFNESS)
    val zeta = APPLE_LYRICS_LIFT_DAMPING
    val damped = omega * sqrt(1 - zeta * zeta)
    val envelope = exp(-zeta * omega * t)
    val value = 1 - envelope * (kotlin.math.cos(damped * t) + zeta / sqrt(1 - zeta * zeta) * kotlin.math.sin(damped * t))
    return value.toFloat().coerceIn(0f, 1.05f)
}

fun appleLyricsEmphasisEnvelope(nowMs: Long, startMs: Long, endMs: Long, index: Int, count: Int): Float {
    val duration = (endMs - startMs).coerceAtLeast(1L).toFloat()
    val letters = count.coerceAtLeast(1)
    val travel = duration.coerceAtMost(3_000f)
    val stagger = (0.4f * duration / letters).coerceAtMost(400f)
    val rise = startMs + index * stagger
    val fall = rise + 2f * duration / letters
    if (nowMs <= rise) return 0f
    val up = AppleLyricsEase.transform(((nowMs - rise) / travel).coerceIn(0f, 1f))
    if (nowMs < fall) return up
    val peak = AppleLyricsEase.transform(((fall - rise) / travel).coerceIn(0f, 1f))
    val down = AppleLyricsEase.transform(((nowMs - fall) / travel).coerceIn(0f, 1f))
    return peak * (1f - down)
}

fun appleLyricsWordProgress(word: LyricWord, positionMs: Long): Float = when {
    positionMs <= word.startMs -> 0f
    positionMs >= word.endMs -> 1f
    else -> (positionMs - word.startMs).toFloat() / (word.endMs - word.startMs).coerceAtLeast(1L)
}

fun appleLyricsBreathingCycleMs(gapMs: Long): Long {
    val cycles = (gapMs / 4_000L).coerceAtLeast(1L)
    return gapMs / cycles
}

fun appleLyricsDotFill(positionMs: Long, row: AppleLyricsRow.Instrumental, dot: Int): Float {
    val duration = (row.endMs - row.startMs).coerceAtLeast(1L)
    val elapsed = positionMs - row.startMs
    val slot = (duration + APPLE_LYRICS_MOVE_MAX_MS) / 3f
    val start = slot * dot
    val raw = ((elapsed - start) / slot).coerceIn(0f, 1f)
    return raw
}

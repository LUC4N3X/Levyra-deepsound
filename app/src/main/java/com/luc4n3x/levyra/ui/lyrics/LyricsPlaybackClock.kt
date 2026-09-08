package com.luc4n3x.levyra.ui.lyrics

import android.os.SystemClock
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import com.luc4n3x.levyra.domain.LyricWord
import kotlin.math.abs

const val LYRICS_CLOCK_RESYNC_THRESHOLD_MS = 320L
const val LYRICS_CLOCK_MAX_CONVERGENCE_MS = 48L
const val LYRICS_CLOCK_MIN_SPEED = 0.25f
const val LYRICS_CLOCK_MAX_SPEED = 4f

fun projectedLyricsPositionMs(
    anchorPositionMs: Long,
    anchorRealtimeMs: Long,
    nowRealtimeMs: Long,
    playing: Boolean,
    speed: Float
): Long {
    if (!playing) return anchorPositionMs.coerceAtLeast(0L)
    val elapsed = (nowRealtimeMs - anchorRealtimeMs).coerceAtLeast(0L)
    val scaled = elapsed * speed.coerceIn(LYRICS_CLOCK_MIN_SPEED, LYRICS_CLOCK_MAX_SPEED)
    return (anchorPositionMs + scaled.toLong()).coerceAtLeast(0L)
}

fun shouldSnapLyricsClock(renderedMs: Long, reportedMs: Long): Boolean =
    abs(reportedMs - renderedMs) > LYRICS_CLOCK_RESYNC_THRESHOLD_MS

fun convergedLyricsAnchorMs(renderedMs: Long, reportedMs: Long): Long {
    if (shouldSnapLyricsClock(renderedMs, reportedMs)) return reportedMs.coerceAtLeast(0L)
    val correction = (reportedMs - renderedMs)
        .coerceIn(-LYRICS_CLOCK_MAX_CONVERGENCE_MS, LYRICS_CLOCK_MAX_CONVERGENCE_MS)
    return (renderedMs + correction).coerceAtLeast(0L)
}

@Stable
class LyricsPlaybackClock internal constructor(initialPositionMs: Long) {
    private val renderedMs = mutableLongStateOf(initialPositionMs.coerceAtLeast(0L))
    private var anchorPositionMs = initialPositionMs.coerceAtLeast(0L)
    private var anchorRealtimeMs = SystemClock.elapsedRealtime()
    private var playing = false
    private var speed = 1f

    val positionMs: Long
        get() = renderedMs.longValue

    internal fun sync(reportedMs: Long, playing: Boolean, speed: Float, nowRealtimeMs: Long) {
        val safeReported = reportedMs.coerceAtLeast(0L)
        this.playing = playing
        this.speed = speed
        anchorRealtimeMs = nowRealtimeMs
        anchorPositionMs = if (playing) {
            convergedLyricsAnchorMs(renderedMs.longValue, safeReported)
        } else {
            safeReported
        }
        if (!playing || shouldSnapLyricsClock(renderedMs.longValue, safeReported)) {
            renderedMs.longValue = anchorPositionMs
        }
    }

    internal fun advance(nowRealtimeMs: Long) {
        renderedMs.longValue = projectedLyricsPositionMs(
            anchorPositionMs = anchorPositionMs,
            anchorRealtimeMs = anchorRealtimeMs,
            nowRealtimeMs = nowRealtimeMs,
            playing = playing,
            speed = speed
        )
    }
}

@Composable
fun rememberLyricsPlaybackClock(
    trackKey: String,
    reportedPositionMs: Long,
    isPlaying: Boolean,
    speed: Float,
    smoothingEnabled: Boolean
): LyricsPlaybackClock {
    val clock = remember(trackKey) { LyricsPlaybackClock(reportedPositionMs) }

    LaunchedEffect(clock, reportedPositionMs, isPlaying, speed, smoothingEnabled) {
        if (smoothingEnabled) {
            clock.sync(reportedPositionMs, isPlaying, speed, SystemClock.elapsedRealtime())
        } else {
            clock.sync(reportedPositionMs, playing = false, speed = speed, nowRealtimeMs = SystemClock.elapsedRealtime())
        }
    }

    LaunchedEffect(clock, isPlaying, smoothingEnabled) {
        if (!smoothingEnabled || !isPlaying) return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            clock.advance(SystemClock.elapsedRealtime())
        }
    }

    return clock
}

const val KARAOKE_VISUAL_LEAD_MS = 55L
const val LYRICS_INSTRUMENTAL_DOT_COUNT = 3
const val LYRICS_INSTRUMENTAL_CYCLES = 6f

data class TimedLyricText(
    val text: String,
    val words: List<TimedLyricWord>
)

data class TimedLyricWord(
    val startIndex: Int,
    val length: Int,
    val startMs: Long,
    val endMs: Long
)

fun Char.isPunctuationWithoutLeadingSpace(): Boolean =
    this in charArrayOf(',', '.', ';', ':', '!', '?', ')', ']', '}', '’', '\'', '…')

fun buildTimedLyricText(words: List<LyricWord>): TimedLyricText {
    val text = StringBuilder()
    val timedWords = ArrayList<TimedLyricWord>(words.size)
    words.forEach { word ->
        val value = word.text.trim()
        if (value.isNotBlank()) {
            if (text.isNotEmpty() && !value.first().isPunctuationWithoutLeadingSpace()) {
                text.append(' ')
            }
            val startIndex = text.length
            text.append(value)
            timedWords += TimedLyricWord(
                startIndex = startIndex,
                length = value.length,
                startMs = word.startMs,
                endMs = word.endMs.coerceAtLeast(word.startMs + 1L)
            )
        }
    }
    return TimedLyricText(text.toString(), timedWords)
}

fun karaokeCharacterProgress(
    timedText: TimedLyricText,
    positionMs: Long,
    easing: Easing
): Float {
    if (timedText.text.isEmpty()) return 0f
    var filledCharacters = 0f
    val words = timedText.words
    for (i in 0 until words.size) {
        val timedWord = words[i]
        when {
            positionMs >= timedWord.endMs -> {
                val completed = (timedWord.startIndex + timedWord.length).toFloat()
                if (completed > filledCharacters) filledCharacters = completed
            }
            positionMs > timedWord.startMs -> {
                val raw = (positionMs - timedWord.startMs).toFloat() /
                    (timedWord.endMs - timedWord.startMs).coerceAtLeast(1L).toFloat()
                val eased = easing.transform(raw.coerceIn(0f, 1f))
                val partial = timedWord.startIndex + timedWord.length * eased
                if (partial > filledCharacters) filledCharacters = partial
            }
        }
    }
    return filledCharacters.coerceIn(0f, timedText.text.length.toFloat())
}

fun lyricsInstrumentalDotIntensity(progress: Float, dotIndex: Int): Float {
    val phase = (progress * LYRICS_INSTRUMENTAL_CYCLES * LYRICS_INSTRUMENTAL_DOT_COUNT) - dotIndex
    val wrapped = phase - kotlin.math.floor(phase / LYRICS_INSTRUMENTAL_DOT_COUNT) * LYRICS_INSTRUMENTAL_DOT_COUNT
    val normalized = (wrapped / LYRICS_INSTRUMENTAL_DOT_COUNT).coerceIn(0f, 1f)
    return (kotlin.math.sin(normalized * Math.PI).toFloat()).coerceIn(0f, 1f)
}

package com.luc4n3x.levyra.ui.lyrics.apple

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import com.luc4n3x.levyra.domain.LyricWord
import com.luc4n3x.levyra.ui.lyrics.TimedLyricText
import com.luc4n3x.levyra.ui.lyrics.buildTimedLyricText

@Immutable
internal class AppleKaraokeWord(
    val startMs: Long,
    val endMs: Long,
    val emphasized: Boolean,
    val emphasisScale: Float,
    val firstChar: Int,
    val lastChar: Int
)

internal class AppleKaraokeSegment(
    val word: Int,
    val previousWord: Int,
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
    val baseline: Float,
    val rtl: Boolean,
    val fillStart: Float,
    val fillEnd: Float
)

internal class AppleKaraokePlan(
    val words: List<AppleKaraokeWord>,
    val segments: List<AppleKaraokeSegment>,
    val charLeft: FloatArray,
    val charRight: FloatArray,
    val charTop: FloatArray,
    val charBottom: FloatArray,
    val charBaseline: FloatArray
) {
    val spread = FloatArray(charLeft.size)
}

internal fun appleKaraokeTimedText(text: String, words: List<LyricWord>): TimedLyricText? {
    if (words.isEmpty()) return null
    val timed = buildTimedLyricText(text, words)
    return timed.takeIf { it.words.isNotEmpty() && it.text.isNotBlank() }
}

internal fun appleKaraokePlan(layout: TextLayoutResult, timed: TimedLyricText): AppleKaraokePlan {
    val text = timed.text
    val length = text.length
    val owner = IntArray(length) { -1 }
    val words = ArrayList<AppleKaraokeWord>(timed.words.size)
    timed.words.forEachIndexed { index, word ->
        val start = word.startIndex.coerceIn(0, length)
        val end = (word.startIndex + word.length).coerceIn(start, length)
        for (i in start until end) owner[i] = index
        val letters = text.substring(start, end).count { it.isLetter() }
        val lyricWord = LyricWord(word.startMs, word.endMs, text.substring(start, end))
        val emphasized = letters > 0 && appleLyricsEmphasized(lyricWord)
        words += AppleKaraokeWord(
            startMs = word.startMs,
            endMs = word.endMs,
            emphasized = emphasized,
            emphasisScale = appleLyricsEmphasisScale(word.endMs - word.startMs),
            firstChar = start,
            lastChar = end
        )
    }
    val charLeft = FloatArray(length)
    val charRight = FloatArray(length)
    val charTop = FloatArray(length)
    val charBottom = FloatArray(length)
    val charBaseline = FloatArray(length)
    for (i in 0 until length) {
        val box = layout.getBoundingBox(i)
        val line = layout.getLineForOffset(i)
        charLeft[i] = box.left
        charRight[i] = box.right
        charTop[i] = layout.getLineTop(line)
        charBottom[i] = layout.getLineBottom(line)
        charBaseline[i] = layout.getLineBaseline(line)
    }
    val segments = ArrayList<AppleKaraokeSegment>()
    var cursor = 0
    var lastWord = -1
    while (cursor < length) {
        val word = owner[cursor]
        val line = layout.getLineForOffset(cursor)
        var end = cursor + 1
        while (end < length && owner[end] == word && layout.getLineForOffset(end) == line) end++
        var left = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        for (i in cursor until end) {
            if (charLeft[i] < left) left = charLeft[i]
            if (charRight[i] > right) right = charRight[i]
        }
        val rtl = layout.getParagraphDirection(cursor) == ResolvedTextDirection.Rtl
        val wordRange = if (word >= 0) words[word] else null
        val wordLength = wordRange?.let { (it.lastChar - it.firstChar).coerceAtLeast(1) } ?: 1
        val fillStart = wordRange?.let { (cursor - it.firstChar).toFloat() / wordLength } ?: 0f
        val fillEnd = wordRange?.let { (end - it.firstChar).toFloat() / wordLength } ?: 1f
        segments += AppleKaraokeSegment(
            word = word,
            previousWord = if (word >= 0) word else lastWord,
            left = left,
            right = right,
            top = layout.getLineTop(line),
            bottom = layout.getLineBottom(line),
            baseline = layout.getLineBaseline(line),
            rtl = rtl,
            fillStart = fillStart,
            fillEnd = fillEnd
        )
        if (word >= 0) lastWord = word
        cursor = end
    }
    return AppleKaraokePlan(words, segments, charLeft, charRight, charTop, charBottom, charBaseline)
}

@Immutable
internal class AppleLineColors(val lit: Color, val dim: Color)

@Composable
internal fun AppleLyricText(
    text: String,
    words: List<LyricWord>,
    style: TextStyle,
    align: TextAlign,
    measurer: TextMeasurer,
    fontKey: Any?,
    colors: AppleLineColors,
    karaoke: Boolean,
    focused: Boolean,
    animations: Boolean,
    focus: () -> Float,
    inactive: () -> Color,
    position: () -> Long,
    modifier: Modifier = Modifier
) {
    val timed = remember(text, words, karaoke) { if (karaoke) appleKaraokeTimedText(text, words) else null }
    val shown = timed?.text ?: text
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = constraints.maxWidth
        val layout = remember(shown, style, align, width, fontKey) {
            measurer.measure(
                AnnotatedString(shown),
                style.copy(textAlign = align),
                constraints = Constraints.fixedWidth(width.coerceAtLeast(1))
            )
        }
        val plan = remember(layout, timed) { timed?.let { appleKaraokePlan(layout, it) } }
        val density = LocalDensity.current
        val metrics = remember(density) {
            with(density) {
                floatArrayOf(AppleLyricsTokens.feather.toPx(), AppleLyricsTokens.lift.toPx(), AppleLyricsTokens.emphasisGlow.toPx())
            }
        }
        val height = with(density) { layout.size.height.toDp() }
        Spacer(
            Modifier.fillMaxWidth().height(height).drawBehind {
                val f = focus()
                val dim = inactive()
                if (plan == null || (!focused && f <= 0.001f)) {
                    drawText(layout, color = lerp(dim, colors.lit, f))
                } else {
                    drawKaraoke(layout, plan, position(), lerp(dim, colors.lit, f), dim, f, animations, metrics)
                }
            }
        )
    }
}

private class AppleKaraokeFrame(
    val layout: TextLayoutResult,
    val plan: AppleKaraokePlan,
    val now: Long,
    val lit: Color,
    val dim: Color,
    val focus: Float,
    val animations: Boolean,
    val feather: Float,
    val liftPx: Float,
    val glowPx: Float
) {
    val slack: Float = liftPx * 2f + glowPx * 2f
}

private fun DrawScope.drawKaraoke(
    layout: TextLayoutResult,
    plan: AppleKaraokePlan,
    now: Long,
    lit: Color,
    dim: Color,
    focus: Float,
    animations: Boolean,
    metrics: FloatArray
) {
    val frame = AppleKaraokeFrame(layout, plan, now, lit, dim, focus, animations, metrics[0], metrics[1], metrics[2])
    for (segment in plan.segments) {
        val word = if (segment.word >= 0) plan.words[segment.word] else null
        if (word == null) drawGapSegment(frame, segment) else drawWordSegment(frame, segment, word)
    }
}

private fun DrawScope.drawGapSegment(frame: AppleKaraokeFrame, segment: AppleKaraokeSegment) {
    val previous = if (segment.previousWord >= 0) frame.plan.words[segment.previousWord] else null
    val color = if (previous != null && frame.now >= previous.endMs) frame.lit else frame.dim
    clipRect(segment.left, segment.top - frame.slack, segment.right, segment.bottom + frame.slack) {
        drawText(frame.layout, color = color)
    }
}

private fun wordProgress(word: AppleKaraokeWord, now: Long): Float = when {
    now <= word.startMs -> 0f
    now >= word.endMs -> 1f
    else -> (now - word.startMs).toFloat() / (word.endMs - word.startMs).coerceAtLeast(1L)
}

private fun segmentProgress(segment: AppleKaraokeSegment, progress: Float): Float =
    ((progress - segment.fillStart) / (segment.fillEnd - segment.fillStart).coerceAtLeast(0.0001f)).coerceIn(0f, 1f)

private fun DrawScope.drawWordSegment(frame: AppleKaraokeFrame, segment: AppleKaraokeSegment, word: AppleKaraokeWord) {
    val now = frame.now
    val progress = wordProgress(word, now)
    val started = now > word.startMs
    val lift = if (frame.animations && started) -frame.liftPx * appleLyricsLiftFraction(now - word.startMs) * frame.focus else 0f
    if (word.emphasized && frame.animations && frame.focus > 0f && started && progress > 0f) {
        drawEmphasized(frame, segment, word, progress, lift)
        return
    }
    clipRect(segment.left, segment.top - frame.slack, segment.right, segment.bottom + frame.slack) {
        translate(0f, lift) {
            val local = segmentProgress(segment, progress)
            when {
                local >= 1f -> drawText(frame.layout, color = frame.lit)
                local <= 0f -> drawText(frame.layout, color = frame.dim)
                else -> drawText(frame.layout, brush = fillBrush(segment, local, frame.feather, frame.lit, frame.dim))
            }
        }
    }
}

private fun fillBrush(segment: AppleKaraokeSegment, local: Float, feather: Float, lit: Color, dim: Color): Brush {
    val span = segment.right - segment.left
    return if (segment.rtl) {
        val edge = segment.right - local * (span + feather)
        Brush.horizontalGradient(listOf(dim, lit), startX = edge, endX = edge + feather)
    } else {
        val edge = segment.left + local * (span + feather)
        Brush.horizontalGradient(listOf(lit, dim), startX = edge - feather, endX = edge)
    }
}

private fun fillSpread(frame: AppleKaraokeFrame, word: AppleKaraokeWord, count: Int) {
    val plan = frame.plan
    for (i in word.firstChar until word.lastChar) {
        val envelope = appleLyricsEmphasisEnvelope(frame.now, word.startMs, word.endMs, i - word.firstChar, count) * frame.focus
        plan.spread[i] = (word.emphasisScale - 1f) * envelope * (plan.charRight[i] - plan.charLeft[i]) / 2f
    }
}

private fun emphasisOffset(spread: FloatArray, first: Int, index: Int, center: Float): Float {
    if (index.toFloat() == center) return 0f
    var offset = spread[first + index] * 0.5f
    if (index > center) {
        var j = index - 1
        while (j > center) {
            offset += spread[first + j]
            j--
        }
        return offset
    }
    var j = index + 1
    while (j < center) {
        offset += spread[first + j]
        j++
    }
    return -offset
}

private fun DrawScope.drawEmphasized(
    frame: AppleKaraokeFrame,
    segment: AppleKaraokeSegment,
    word: AppleKaraokeWord,
    progress: Float,
    lift: Float
) {
    val plan = frame.plan
    val first = word.firstChar
    val count = (word.lastChar - first).coerceAtLeast(1)
    fillSpread(frame, word, count)
    val center = (count - 1) / 2f
    val local = segmentProgress(segment, progress)
    val brush = if (local in 0.0001f..0.9999f) fillBrush(segment, local, frame.feather, frame.lit, frame.dim) else null
    val solid = if (local >= 1f) frame.lit else frame.dim
    for (i in first until word.lastChar) {
        val left = plan.charLeft[i]
        val right = plan.charRight[i]
        val outside = right <= left || left < segment.left - 0.5f || right > segment.right + 0.5f
        if (outside) continue
        val index = i - first
        val offset = emphasisOffset(plan.spread, first, index, center)
        val envelope = appleLyricsEmphasisEnvelope(frame.now, word.startMs, word.endMs, index, count) * frame.focus
        val scale = 1f + (word.emphasisScale - 1f) * envelope
        val pivot = Offset((left + right) / 2f, plan.charBaseline[i])
        val shadow = if (envelope > 0.01f) Shadow(Color.White.copy(alpha = 0.5f * envelope), Offset.Zero, frame.glowPx) else null
        withTransform({
            translate(if (segment.rtl) -offset else offset, lift)
            scale(scale, scale, pivot)
        }) {
            clipRect(left, plan.charTop[i] - frame.glowPx, right, plan.charBottom[i] + frame.glowPx) {
                if (brush != null) drawText(frame.layout, brush = brush, shadow = shadow) else drawText(frame.layout, color = solid, shadow = shadow)
            }
        }
    }
}

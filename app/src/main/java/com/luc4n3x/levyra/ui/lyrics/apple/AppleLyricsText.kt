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
    val feather = metrics[0]
    val liftPx = metrics[1]
    val glowPx = metrics[2]
    val slack = liftPx * 2f + glowPx * 2f
    val segments = plan.segments
    for (s in segments.indices) {
        val segment = segments[s]
        val word = if (segment.word >= 0) plan.words[segment.word] else null
        if (word == null) {
            val previous = if (segment.previousWord >= 0) plan.words[segment.previousWord] else null
            val color = if (previous != null && now >= previous.endMs) lit else dim
            clipRect(segment.left, segment.top - slack, segment.right, segment.bottom + slack) {
                drawText(layout, color = color)
            }
            continue
        }
        val progress = when {
            now <= word.startMs -> 0f
            now >= word.endMs -> 1f
            else -> (now - word.startMs).toFloat() / (word.endMs - word.startMs).coerceAtLeast(1L)
        }
        val lift = if (animations && now > word.startMs) -liftPx * appleLyricsLiftFraction(now - word.startMs) * focus else 0f
        if (word.emphasized && animations && focus > 0f && now > word.startMs && progress > 0f) {
            drawEmphasized(layout, plan, segment, word, now, lit, dim, progress, lift, feather, glowPx, focus)
            continue
        }
        clipRect(segment.left, segment.top - slack, segment.right, segment.bottom + slack) {
            translate(0f, lift) {
                val local = ((progress - segment.fillStart) / (segment.fillEnd - segment.fillStart).coerceAtLeast(0.0001f)).coerceIn(0f, 1f)
                when {
                    local >= 1f -> drawText(layout, color = lit)
                    local <= 0f -> drawText(layout, color = dim)
                    else -> drawText(layout, brush = fillBrush(segment, local, feather, lit, dim))
                }
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

private fun DrawScope.drawEmphasized(
    layout: TextLayoutResult,
    plan: AppleKaraokePlan,
    segment: AppleKaraokeSegment,
    word: AppleKaraokeWord,
    now: Long,
    lit: Color,
    dim: Color,
    progress: Float,
    lift: Float,
    feather: Float,
    glowPx: Float,
    focus: Float
) {
    val first = word.firstChar
    val last = word.lastChar
    val count = (last - first).coerceAtLeast(1)
    val spread = plan.spread
    for (i in first until last) {
        val envelope = appleLyricsEmphasisEnvelope(now, word.startMs, word.endMs, i - first, count) * focus
        spread[i] = (word.emphasisScale - 1f) * envelope * (plan.charRight[i] - plan.charLeft[i]) / 2f
    }
    val center = (count - 1) / 2f
    val local = ((progress - segment.fillStart) / (segment.fillEnd - segment.fillStart).coerceAtLeast(0.0001f)).coerceIn(0f, 1f)
    val brush = if (local in 0.0001f..0.9999f) fillBrush(segment, local, feather, lit, dim) else null
    val solid = if (local >= 1f) lit else dim
    for (i in first until last) {
        val left = plan.charLeft[i]
        val right = plan.charRight[i]
        if (right <= left || left < segment.left - 0.5f || right > segment.right + 0.5f) continue
        val index = i - first
        var offset = spread[i] * 0.5f
        if (index > center) {
            var j = index - 1
            while (j > center) { offset += spread[first + j]; j-- }
        } else if (index < center) {
            var j = index + 1
            while (j < center) { offset += spread[first + j]; j++ }
            offset = -offset
        } else {
            offset = 0f
        }
        val envelope = appleLyricsEmphasisEnvelope(now, word.startMs, word.endMs, index, count) * focus
        val scale = 1f + (word.emphasisScale - 1f) * envelope
        val pivot = Offset((left + right) / 2f, plan.charBaseline[i])
        val shadow = if (envelope > 0.01f) Shadow(Color.White.copy(alpha = 0.5f * envelope), Offset.Zero, glowPx) else null
        withTransform({
            translate(if (segment.rtl) -offset else offset, lift)
            scale(scale, scale, pivot)
        }) {
            clipRect(left, plan.charTop[i] - glowPx, right, plan.charBottom[i] + glowPx) {
                if (brush != null) drawText(layout, brush = brush, shadow = shadow) else drawText(layout, color = solid, shadow = shadow)
            }
        }
    }
}

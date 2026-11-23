package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import com.luc4n3x.levyra.ui.lyrics.KARAOKE_VISUAL_LEAD_MS
import com.luc4n3x.levyra.ui.lyrics.buildTimedLyricText
import com.luc4n3x.levyra.ui.lyrics.karaokeCharacterProgress

@Composable
internal fun KaraokeWordTimedText(
    lineText: String,
    words: List<com.luc4n3x.levyra.domain.LyricWord>,
    positionProvider: () -> Long,
    isActive: Boolean,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    textAlign: TextAlign,
    inactiveColor: Color,
    completedColor: Color,
    activeColor: Color,
    pendingColor: Color,
    fontWeight: FontWeight,
    fontFamily: FontFamily? = null,
    shadow: Shadow? = null
) {
    val timedText = remember(lineText, words) { buildTimedLyricText(lineText, words) }
    val karaokeEasing = remember { CubicBezierEasing(0.18f, 0f, 0.20f, 1f) }
    val clipPath = remember { Path() }
    val fillFront = remember { KaraokeFillFront() }
    val contentAlignment = when (textAlign) {
        TextAlign.Center -> Alignment.Center
        TextAlign.End, TextAlign.Right -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }
    var textLayoutResult by remember(timedText.text, fontSize, lineHeight, textAlign, fontWeight, fontFamily) {
        mutableStateOf<TextLayoutResult?>(null)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = contentAlignment
    ) {
        Box(modifier = Modifier.wrapContentWidth()) {
            Text(
                text = timedText.text,
                color = if (isActive) pendingColor else inactiveColor,
                fontFamily = fontFamily,
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontWeight = fontWeight,
                textAlign = textAlign,
                style = TextStyle(),
                onTextLayout = { textLayoutResult = it },
                modifier = Modifier.wrapContentWidth()
            )
            if (isActive) {
                Text(
                    text = timedText.text,
                    color = if (activeColor == completedColor) completedColor else activeColor,
                    fontFamily = fontFamily,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    fontWeight = fontWeight,
                    textAlign = textAlign,
                    style = TextStyle(shadow = shadow),
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            val layoutResult = textLayoutResult ?: return@drawWithContent
                            val characterProgress = karaokeCharacterProgress(
                                timedText = timedText,
                                positionMs = positionProvider() + KARAOKE_VISUAL_LEAD_MS,
                                easing = karaokeEasing
                            )
                            if (characterProgress <= 0f) return@drawWithContent
                            val featherPx = fontSize.toPx() * KARAOKE_FILL_FEATHER_EM
                            val path = buildKaraokeGlyphPath(
                                path = clipPath,
                                layoutResult = layoutResult,
                                textLength = timedText.text.length,
                                characterProgress = characterProgress,
                                featherPx = featherPx,
                                front = fillFront
                            )
                            clipPath(path) {
                                this@drawWithContent.drawContent()
                            }
                            if (fillFront.active) drawKaraokeFillFeather(fillFront)
                        }
                )
            }
        }
    }
}

private const val KARAOKE_FILL_FEATHER_EM = 0.55f

private class KaraokeFillFront {
    var active = false
    var x = 0f
    var top = 0f
    var bottom = 0f
    var rtl = false
    var feather = 0f
}

private fun DrawScope.drawKaraokeFillFeather(front: KaraokeFillFront) {
    val featherPx = front.feather
    if (featherPx <= 0.5f || front.bottom <= front.top) return
    val from = front.x - featherPx
    val to = front.x + featherPx
    val colors = if (front.rtl) KaraokeFeatherRtl else KaraokeFeatherLtr
    drawRect(
        brush = Brush.horizontalGradient(colors, startX = from, endX = to),
        topLeft = Offset(from, front.top),
        size = Size(to - from, front.bottom - front.top),
        blendMode = BlendMode.DstIn
    )
}

internal fun karaokeFeatherWidth(featherPx: Float, remainingToLineEndPx: Float): Float =
    minOf(featherPx, remainingToLineEndPx).coerceAtLeast(0f)

private val KaraokeFeatherLtr = listOf(Color.White, Color.Transparent)
private val KaraokeFeatherRtl = listOf(Color.Transparent, Color.White)

private fun buildKaraokeGlyphPath(
    path: Path,
    layoutResult: TextLayoutResult,
    textLength: Int,
    characterProgress: Float,
    featherPx: Float,
    front: KaraokeFillFront
): Path {
    path.reset()
    front.active = false
    if (textLength == 0 || characterProgress <= 0f) return path
    val boundedProgress = characterProgress.coerceIn(0f, textLength.toFloat())
    val completedCharacters = boundedProgress.toInt().coerceIn(0, textLength)

    val activeLine = if (completedCharacters < textLength) {
        layoutResult.getLineForOffset(completedCharacters)
    } else {
        layoutResult.lineCount - 1
    }

    for (line in 0 until activeLine) {
        val left = layoutResult.getLineLeft(line)
        val right = layoutResult.getLineRight(line)
        val top = layoutResult.getLineTop(line)
        val bottom = layoutResult.getLineBottom(line)
        path.addRectangle(left, top, right, bottom)
    }

    val top = layoutResult.getLineTop(activeLine)
    val bottom = layoutResult.getLineBottom(activeLine)
    val isRtl = layoutResult.getParagraphDirection(layoutResult.getLineStart(activeLine)) == ResolvedTextDirection.Rtl
    val startX = if (isRtl) layoutResult.getLineRight(activeLine) else layoutResult.getLineLeft(activeLine)

    if (completedCharacters >= textLength) {
        val endX = if (isRtl) layoutResult.getLineLeft(activeLine) else layoutResult.getLineRight(activeLine)
        path.addRectangle(minOf(startX, endX), top, maxOf(startX, endX), bottom)
    } else {
        val fraction = boundedProgress - completedCharacters
        val charStart = layoutResult.getHorizontalPosition(completedCharacters, usePrimaryDirection = true)
        val nextOffset = (completedCharacters + 1).coerceAtMost(textLength)
        val charEnd = if (
            nextOffset < textLength && layoutResult.getLineForOffset(nextOffset) != activeLine
        ) {
            if (isRtl) layoutResult.getLineLeft(activeLine) else layoutResult.getLineRight(activeLine)
        } else {
            layoutResult.getHorizontalPosition(nextOffset, usePrimaryDirection = true)
        }
        val currentX = charStart + (charEnd - charStart) * fraction
        val lineEndX = if (isRtl) layoutResult.getLineLeft(activeLine) else layoutResult.getLineRight(activeLine)
        val feather = karaokeFeatherWidth(featherPx, kotlin.math.abs(lineEndX - currentX))
        val reachX = if (isRtl) currentX - feather else currentX + feather
        path.addRectangle(minOf(startX, reachX), top, maxOf(startX, reachX), bottom)
        front.active = true
        front.x = currentX
        front.top = top
        front.bottom = bottom
        front.rtl = isRtl
        front.feather = feather
    }
    return path
}

private fun Path.addRectangle(left: Float, top: Float, right: Float, bottom: Float) {
    if (right <= left || bottom <= top) return
    moveTo(left, top)
    lineTo(right, top)
    lineTo(right, bottom)
    lineTo(left, bottom)
    close()
}

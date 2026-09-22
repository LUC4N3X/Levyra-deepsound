package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.theme.LevyraMotion

private const val GlyphRounding = 0.07f

private val PlayLeading = floatArrayOf(0.27f, 0.19f, 0.525f, 0.345f, 0.525f, 0.655f, 0.27f, 0.81f)
private val PlayTrailing = floatArrayOf(0.525f, 0.345f, 0.78f, 0.5f, 0.78f, 0.5f, 0.525f, 0.655f)
private val PauseLeading = floatArrayOf(0.316f, 0.191f, 0.4025f, 0.191f, 0.4025f, 0.809f, 0.316f, 0.809f)
private val PauseTrailing = floatArrayOf(0.5975f, 0.191f, 0.684f, 0.191f, 0.684f, 0.809f, 0.5975f, 0.809f)

/**
 * Play/pause glyph whose two halves of the triangle fold into the two pause bars, so the state
 * change reads as one shape transforming. The playback action itself never waits for it.
 */
@Composable
fun LevyraPlayPauseGlyph(
    playing: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val animated = LocalAnimationsEnabled.current
    val morph = animateFloatAsState(
        targetValue = if (playing) 1f else 0f,
        animationSpec = LevyraMotion.spec(
            animated,
            tween(LevyraMotion.Durations.Short + 40, easing = LevyraMotion.Easings.Emphasized)
        ),
        label = "levyra-play-pause-morph"
    )
    val path = remember { Path() }
    Spacer(
        modifier = modifier
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            )
            .drawWithCache {
                val stroke = Stroke(width = size.minDimension * GlyphRounding, join = StrokeJoin.Round)
                onDrawBehind {
                    val t = morph.value
                    path.rewind()
                    path.addMorphedQuad(PlayLeading, PauseLeading, t, size.width, size.height)
                    path.addMorphedQuad(PlayTrailing, PauseTrailing, t, size.width, size.height)
                    drawPath(path, color)
                    drawPath(path, color, style = stroke)
                }
            }
    )
}

private fun Path.addMorphedQuad(from: FloatArray, to: FloatArray, t: Float, width: Float, height: Float) {
    for (index in 0 until 4) {
        val x = (from[index * 2] + (to[index * 2] - from[index * 2]) * t) * width
        val y = (from[index * 2 + 1] + (to[index * 2 + 1] - from[index * 2 + 1]) * t) * height
        if (index == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

package com.luc4n3x.levyra.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.theme.MonoDesign
import kotlin.math.PI
import kotlin.math.sin

private const val MonoTileWaveSteps = 48

private class MonoTileLook(val hue: Color, val seed: Int, val glacier: Boolean) {
    val cycles = 1.4f + seed % 5 * 0.35f
    val phase = seed % 360 / 57.3f
    val waveShade = if (glacier) MonoDesign.void.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.16f)
    val echo = if (glacier) MonoDesign.accent.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.34f)

    fun fill(size: Size): Brush = if (glacier) {
        Brush.linearGradient(
            listOf(lerp(hue, MonoDesign.accent, 0.22f), hue, lerp(hue, MonoDesign.void, 0.66f)),
            Offset.Zero,
            Offset(size.width * (0.55f + seed % 3 * 0.15f), size.height)
        )
    } else {
        Brush.linearGradient(listOf(lerp(hue, Color.White, 0.08f), lerp(hue, Color.Black, 0.38f)), Offset.Zero, Offset(size.width * 0.6f, size.height))
    }

    fun frost(size: Size): Brush? = if (glacier) {
        Brush.radialGradient(
            listOf(MonoDesign.accent.copy(alpha = 0.28f), Color.Transparent),
            center = Offset(size.width * 0.86f, size.height * 0.1f),
            radius = size.width * 0.62f
        )
    } else null

    fun wave(size: Size): Path {
        val band = size.height * 0.34f
        val baseY = size.height * 0.84f
        return Path().apply {
            for (i in 0..MonoTileWaveSteps) {
                val t = i / MonoTileWaveSteps.toFloat()
                val x = size.width * (0.18f + t * 0.95f)
                val y = baseY - sin(t * cycles * 2f * PI.toFloat() + phase) * band * (0.35f + 0.65f * t)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
    }
}

internal fun Modifier.monoTileWaveBackground(hue: Color, seed: Int, glacier: Boolean): Modifier {
    val look = MonoTileLook(hue, seed, glacier)
    return drawWithCache {
        val fill = look.fill(size)
        val frost = look.frost(size)
        val path = look.wave(size)
        val stroke = 9.dp.toPx()
        val waveStyle = Stroke(stroke, cap = StrokeCap.Round)
        val echoStyle = Stroke(1.5.dp.toPx())
        onDrawBehind {
            drawRect(fill)
            frost?.let { drawRect(it) }
            drawPath(path, look.waveShade, style = waveStyle)
            translate(top = -stroke * 1.4f) { drawPath(path, look.echo, style = echoStyle) }
        }
    }
}

@Composable
internal fun BoxScope.MonoTileArtwork(artwork: String, height: Dp) {
    MonoArtwork(
        artwork,
        Modifier.align(Alignment.BottomEnd)
            .size(height * 0.78f)
            .graphicsLayer {
                translationX = size.width * 0.18f
                translationY = size.height * 0.16f
                rotationZ = -12f
                shadowElevation = 10.dp.toPx()
                shape = MonoDesign.artShape
                clip = true
            }
    )
}

@Composable
internal fun BoxScope.MonoTileLabels(title: String, index: Int) {
    Box(Modifier.matchParentSize().padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(0.82f)
        )
        Text(
            monoRank(index + 1),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.72f),
            modifier = Modifier.align(Alignment.BottomStart).clearAndSetSemantics { }
        )
    }
}

package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private data class ArtworkSparkle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
    val drift: Float,
    val cross: Boolean
)

private val cardArtworkSparkles = listOf(
    ArtworkSparkle(0.08f, 0.13f, 0.0045f, 0.15f, 0.010f, true),
    ArtworkSparkle(0.92f, 0.11f, 0.0030f, 1.10f, 0.008f, false),
    ArtworkSparkle(0.95f, 0.31f, 0.0040f, 2.05f, 0.009f, true),
    ArtworkSparkle(0.06f, 0.39f, 0.0026f, 2.75f, 0.007f, false),
    ArtworkSparkle(0.09f, 0.69f, 0.0038f, 3.55f, 0.009f, true),
    ArtworkSparkle(0.94f, 0.73f, 0.0028f, 4.15f, 0.008f, false),
    ArtworkSparkle(0.17f, 0.91f, 0.0032f, 4.95f, 0.007f, false),
    ArtworkSparkle(0.83f, 0.92f, 0.0042f, 5.60f, 0.010f, true)
)

@Composable
internal fun PlayerArtworkEffectsOverlay(
    active: Boolean,
    alpha: () -> Float,
    modifier: Modifier = Modifier
) {
    val sparklePhase = remember { Animatable(0f) }
    val glintPhase = remember { Animatable(-0.25f) }
    val effectAmount by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (active) EFFECTS_ENTER_MS else EFFECTS_EXIT_MS,
            easing = FastOutSlowInEasing
        ),
        label = "artwork-effects-amount"
    )
    val bedAlpha = alpha().coerceIn(0f, 1f)

    LaunchedEffect(active) {
        if (!active) {
            sparklePhase.snapTo(0f)
            glintPhase.snapTo(-0.25f)
            return@LaunchedEffect
        }
        coroutineScope {
            launch {
                while (isActive) {
                    sparklePhase.animateTo(
                        1f,
                        tween(SPARKLE_CYCLE_MS, easing = LinearEasing)
                    )
                    sparklePhase.snapTo(0f)
                }
            }
            launch {
                delay(GLINT_INITIAL_DELAY_MS)
                while (isActive) {
                    glintPhase.snapTo(-0.25f)
                    glintPhase.animateTo(
                        1.25f,
                        tween(GLINT_DURATION_MS, easing = FastOutSlowInEasing)
                    )
                    delay(GLINT_INTERVAL_MS)
                }
            }
        }
    }

    Canvas(
        modifier = modifier.graphicsLayer {
            this.alpha = bedAlpha * effectAmount
        }
    ) {
        if (size.minDimension <= 0f) return@Canvas

        val minDimension = size.minDimension
        val phase = sparklePhase.value * (2f * PI.toFloat())
        val edgePulse = 0.62f + 0.38f * ((sin(phase * 0.55f) + 1f) * 0.5f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.035f * edgePulse),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.08f, size.height * 0.18f),
                radius = minDimension * 0.30f
            ),
            radius = minDimension * 0.30f,
            center = Offset(size.width * 0.08f, size.height * 0.18f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.028f * (1f - edgePulse * 0.3f)),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.92f, size.height * 0.80f),
                radius = minDimension * 0.26f
            ),
            radius = minDimension * 0.26f,
            center = Offset(size.width * 0.92f, size.height * 0.80f)
        )

        cardArtworkSparkles.forEach { sparkle ->
            val localPhase = phase + sparkle.phase
            val pulse = 0.42f + 0.58f * ((sin(localPhase * 1.35f) + 1f) * 0.5f)
            val driftPx = minDimension * sparkle.drift
            val center = Offset(
                x = size.width * sparkle.x + sin(localPhase * 0.72f) * driftPx,
                y = size.height * sparkle.y + cos(localPhase * 0.61f) * driftPx
            )
            val radius = minDimension * sparkle.radius * (0.78f + pulse * 0.34f)

            drawCircle(
                color = Color.White.copy(alpha = 0.055f + pulse * 0.12f),
                radius = radius * 1.7f,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.13f + pulse * 0.20f),
                radius = radius,
                center = center
            )

            if (sparkle.cross) {
                val arm = radius * (2.1f + pulse * 0.7f)
                val strokeWidth = max(1f, minDimension * 0.00115f)
                val crossColor = Color.White.copy(alpha = 0.09f + pulse * 0.20f)
                drawLine(
                    color = crossColor,
                    start = Offset(center.x - arm, center.y),
                    end = Offset(center.x + arm, center.y),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = crossColor,
                    start = Offset(center.x, center.y - arm),
                    end = Offset(center.x, center.y + arm),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        val glint = glintPhase.value
        if (glint in -0.20f..1.20f) {
            val x = size.width * glint
            val slant = size.height * 0.13f
            val start = Offset(x - slant, 0f)
            val end = Offset(x + slant, size.height)
            drawLine(
                color = Color.White.copy(alpha = 0.022f),
                start = start,
                end = end,
                strokeWidth = minDimension * 0.042f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = 0.065f),
                start = start,
                end = end,
                strokeWidth = max(1.5f, minDimension * 0.006f),
                cap = StrokeCap.Round
            )
        }

        drawRoundRect(
            color = Color.White.copy(alpha = 0.022f * edgePulse),
            cornerRadius = CornerRadius(minDimension * 0.035f),
            style = Stroke(width = max(1f, minDimension * 0.0012f))
        )
    }
}

private const val EFFECTS_ENTER_MS = 520
private const val EFFECTS_EXIT_MS = 240
private const val SPARKLE_CYCLE_MS = 16_000
private const val GLINT_INITIAL_DELAY_MS = 5_500L
private const val GLINT_DURATION_MS = 1_150
private const val GLINT_INTERVAL_MS = 10_500L

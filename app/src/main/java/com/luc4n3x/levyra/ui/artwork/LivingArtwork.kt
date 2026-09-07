package com.luc4n3x.levyra.ui.artwork

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.translate
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin

private const val FRAME_INTERVAL_MS = 34L
private const val FADE_IN_MS = 520
private const val FADE_OUT_MS = 260
private const val SHADER_INTENSITY = 0.60f
private const val LEGACY_INTENSITY = 0.50f

@Composable
internal fun LivingArtworkLayer(
    colors: LivingArtworkColors,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val phase = remember { mutableFloatStateOf(0f) }
    val amount by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (active) FADE_IN_MS else FADE_OUT_MS,
            easing = FastOutSlowInEasing
        ),
        label = "living-artwork-amount"
    )

    LaunchedEffect(active, colors) {
        if (!active) return@LaunchedEffect
        while (isActive) {
            phase.floatValue += FRAME_INTERVAL_MS / 1_000f
            delay(FRAME_INTERVAL_MS)
        }
    }

    if (!active && amount <= 0.001f) return

    val shader = rememberLivingArtworkShader(colors)
    if (shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        LivingArtworkShaderSurface(
            shader = shader,
            phase = { phase.floatValue },
            amount = { amount },
            modifier = modifier
        )
    } else {
        LivingArtworkLegacySurface(
            colors = colors,
            phase = { phase.floatValue },
            amount = { amount },
            modifier = modifier
        )
    }
}

@Composable
private fun rememberLivingArtworkShader(colors: LivingArtworkColors): RuntimeShader? =
    remember(colors) {
        if (!livingArtworkShaderSupported()) return@remember null
        val shader = createLivingArtworkShader() ?: return@remember null
        if (!shader.applyLivingArtworkColors(colors)) return@remember null
        shader
    }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun LivingArtworkShaderSurface(
    shader: RuntimeShader,
    phase: () -> Float,
    amount: () -> Float,
    modifier: Modifier
) {
    Box(
        modifier = modifier.drawWithCache {
            val brush = ShaderBrush(shader)
            shader.setFloatUniform("uSize", size.width, size.height)
            onDrawBehind {
                val visible = amount()
                if (visible <= 0.001f) return@onDrawBehind
                shader.setFloatUniform("uTime", phase())
                shader.setFloatUniform("uIntensity", SHADER_INTENSITY * visible)
                drawRect(brush = brush)
            }
        }
    )
}

@Composable
private fun LivingArtworkLegacySurface(
    colors: LivingArtworkColors,
    phase: () -> Float,
    amount: () -> Float,
    modifier: Modifier
) {
    Box(
        modifier = modifier.drawWithCache {
            val radius = size.maxDimension * 0.46f
            val brushes = colors.tones.map { tone ->
                Brush.radialGradient(
                    colors = listOf(tone.copy(alpha = 0.85f), Color.Transparent),
                    center = Offset.Zero,
                    radius = radius
                )
            }
            val travelX = size.width * 0.22f
            val travelY = size.height * 0.20f
            onDrawBehind {
                val visible = amount()
                if (visible <= 0.001f) return@onDrawBehind
                val t = phase()
                val alpha = LEGACY_INTENSITY * visible
                for (index in brushes.indices) {
                    val seed = index + 1f
                    val x = size.width * 0.5f + cos(t * (0.13f + seed * 0.021f) + seed) * travelX
                    val y = size.height * 0.5f + sin(t * (0.11f + seed * 0.017f) + seed * 1.7f) * travelY
                    translate(left = x, top = y) {
                        drawCircle(
                            brush = brushes[index],
                            radius = radius,
                            center = Offset.Zero,
                            alpha = alpha,
                            blendMode = BlendMode.Screen
                        )
                    }
                }
            }
        }
    )
}

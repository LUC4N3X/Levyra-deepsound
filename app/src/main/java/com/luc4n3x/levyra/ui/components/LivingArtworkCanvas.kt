package com.luc4n3x.levyra.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.luc4n3x.levyra.ui.harmonizePlayerAccents
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Palette configuration for Living Artwork rendering.
 */
data class LivingArtworkPalette(
    val base: Color,
    val primary: Color,
    val secondary: Color,
    val highlight: Color,
    val ambient: Color
)

/**
 * Derives a balanced, organic 5-tone palette from track accent colors.
 */
fun deriveLivingArtworkPalette(
    accentStart: Int,
    accentEnd: Int,
    isDarkTheme: Boolean = true
): LivingArtworkPalette {
    val rawStart = if (accentStart != 0) Color(accentStart) else Color(0xFF1E88E5)
    val rawEnd = if (accentEnd != 0) Color(accentEnd) else Color(0xFF7C4DFF)
    val harmonized = harmonizePlayerAccents(rawStart, rawEnd)

    val primary = harmonized.primary.copy(alpha = 1f)
    val secondary = harmonized.secondary.copy(alpha = 1f)

    // Deep base tone with very low luminance for depth
    val base = if (isDarkTheme) {
        Color(
            red = (primary.red * 0.12f).coerceIn(0.02f, 0.10f),
            green = (primary.green * 0.12f).coerceIn(0.02f, 0.10f),
            blue = (primary.blue * 0.12f).coerceIn(0.02f, 0.12f),
            alpha = 1f
        )
    } else {
        Color(
            red = (0.92f + primary.red * 0.08f).coerceIn(0.90f, 0.98f),
            green = (0.92f + primary.green * 0.08f).coerceIn(0.90f, 0.98f),
            blue = (0.92f + primary.blue * 0.08f).coerceIn(0.90f, 0.98f),
            alpha = 1f
        )
    }

    // Highlight tone: luminescent tint
    val highlight = Color(
        red = (primary.red * 0.65f + secondary.red * 0.35f + 0.25f).coerceIn(0f, 1f),
        green = (primary.green * 0.65f + secondary.green * 0.35f + 0.25f).coerceIn(0f, 1f),
        blue = (primary.blue * 0.65f + secondary.blue * 0.35f + 0.25f).coerceIn(0f, 1f),
        alpha = 1f
    )

    // Ambient tone: contrasting warm/cool shift
    val ambient = Color(
        red = (secondary.blue * 0.7f + primary.green * 0.3f).coerceIn(0f, 1f),
        green = (secondary.red * 0.7f + primary.blue * 0.3f).coerceIn(0f, 1f),
        blue = (primary.red * 0.5f + secondary.green * 0.5f).coerceIn(0f, 1f),
        alpha = 1f
    )

    return LivingArtworkPalette(
        base = base,
        primary = primary,
        secondary = secondary,
        highlight = highlight,
        ambient = ambient
    )
}

private const val AGSL_LIVING_ARTWORK_SHADER = """
uniform float2 uResolution;
uniform float uTime;
uniform float4 uColorBase;
uniform float4 uColorPrimary;
uniform float4 uColorSecondary;
uniform float4 uColorHighlight;
uniform float4 uColorAmbient;

vec4 main(vec2 fragCoord) {
    vec2 uv = fragCoord / uResolution;
    float aspect = uResolution.x / max(uResolution.y, 1.0);
    vec2 p = (uv - 0.5) * vec2(aspect, 1.0);

    float t = uTime * 0.12;

    // Organic harmonic centers
    vec2 c1 = vec2(sin(t * 0.65) * 0.32, cos(t * 0.82) * 0.32);
    vec2 c2 = vec2(cos(t * 0.74 + 1.2) * 0.36, sin(t * 0.58 + 0.8) * 0.36);
    vec2 c3 = vec2(sin(t * 0.92 + 2.4) * 0.28, cos(t * 0.45 + 1.9) * 0.28);

    float d1 = length(p - c1);
    float d2 = length(p - c2);
    float d3 = length(p - c3);

    float w1 = smoothstep(0.85, 0.08, d1);
    float w2 = smoothstep(0.80, 0.12, d2);
    float w3 = smoothstep(0.75, 0.05, d3);

    vec4 color = uColorBase;
    color = mix(color, uColorPrimary, w1 * 0.80);
    color = mix(color, uColorSecondary, w2 * 0.70);
    color = mix(color, uColorAmbient, w3 * 0.60);

    // Subtle atmospheric center glow
    float centerDist = length(p);
    float centerGlow = smoothstep(0.55, 0.0, centerDist);
    color = mix(color, uColorHighlight, centerGlow * 0.28);

    return color;
}
"""

/**
 * Living Artwork: generative audio-reactive living canvas fallback for Levyra.
 *
 * Uses AGSL RuntimeShader on Android 13+ (API 33+) with a GPU-accelerated harmonic fluid shader,
 * and falls back gracefully to a multi-layered Compose Canvas on older Android versions.
 */
@Composable
fun LivingArtworkCanvas(
    palette: LivingArtworkPalette,
    isPlaying: Boolean,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentAnimationsEnabled by rememberUpdatedState(animationsEnabled)

    // Throttled time accumulator (target ~30 FPS steady to conserve battery and GPU)
    var elapsedSeconds by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentIsPlaying, currentAnimationsEnabled) {
        if (!currentIsPlaying || !currentAnimationsEnabled) return@LaunchedEffect
        var lastFrameNanos = 0L
        val targetFrameIntervalNanos = 33_333_333L // ~30 FPS

        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameNanos == 0L) {
                    lastFrameNanos = frameTimeNanos
                } else {
                    val delta = frameTimeNanos - lastFrameNanos
                    if (delta >= targetFrameIntervalNanos) {
                        val deltaSec = (delta / 1_000_000_000f).coerceIn(0f, 0.1f)
                        elapsedSeconds += deltaSec
                        lastFrameNanos = frameTimeNanos
                    }
                }
            }
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslLivingArtwork(
            palette = palette,
            timeSeconds = elapsedSeconds,
            modifier = modifier
        )
    } else {
        FallbackLivingArtwork(
            palette = palette,
            timeSeconds = elapsedSeconds,
            modifier = modifier
        )
    }
}

@Composable
private fun AgslLivingArtwork(
    palette: LivingArtworkPalette,
    timeSeconds: Float,
    modifier: Modifier = Modifier
) {
    val shader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(AGSL_LIVING_ARTWORK_SHADER)
        } else {
            null
        }
    }

    if (shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .drawWithCache {
                    shader.setFloatUniform("uResolution", size.width, size.height)
                    shader.setFloatUniform("uTime", timeSeconds)
                    shader.setColorUniform("uColorBase", palette.base.toArgb())
                    shader.setColorUniform("uColorPrimary", palette.primary.toArgb())
                    shader.setColorUniform("uColorSecondary", palette.secondary.toArgb())
                    shader.setColorUniform("uColorHighlight", palette.highlight.toArgb())
                    shader.setColorUniform("uColorAmbient", palette.ambient.toArgb())

                    val shaderBrush = ShaderBrush(shader)
                    onDrawBehind {
                        drawRect(brush = shaderBrush)
                    }
                }
        )
    } else {
        FallbackLivingArtwork(
            palette = palette,
            timeSeconds = timeSeconds,
            modifier = modifier
        )
    }
}

@Composable
private fun FallbackLivingArtwork(
    palette: LivingArtworkPalette,
    timeSeconds: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.base)
            .drawWithCache {
                val width = size.width
                val height = size.height
                val radius = (width.coerceAtLeast(height)) * 0.72f

                val t = timeSeconds * 0.12f

                val c1X = width * (0.5f + sin(t * 0.65f) * 0.24f)
                val c1Y = height * (0.5f + cos(t * 0.82f) * 0.24f)

                val c2X = width * (0.5f + cos(t * 0.74f + 1.2f) * 0.28f)
                val c2Y = height * (0.5f + sin(t * 0.58f + 0.8f) * 0.28f)

                val c3X = width * (0.5f + sin(t * 0.92f + 2.4f) * 0.22f)
                val c3Y = height * (0.5f + cos(t * 0.45f + 1.9f) * 0.22f)

                val brush1 = Brush.radialGradient(
                    colors = listOf(palette.primary.copy(alpha = 0.75f), Color.Transparent),
                    center = Offset(c1X, c1Y),
                    radius = radius
                )
                val brush2 = Brush.radialGradient(
                    colors = listOf(palette.secondary.copy(alpha = 0.65f), Color.Transparent),
                    center = Offset(c2X, c2Y),
                    radius = radius * 0.9f
                )
                val brush3 = Brush.radialGradient(
                    colors = listOf(palette.ambient.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(c3X, c3Y),
                    radius = radius * 0.8f
                )
                val brushCenter = Brush.radialGradient(
                    colors = listOf(palette.highlight.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(width * 0.5f, height * 0.5f),
                    radius = radius * 0.55f
                )

                onDrawBehind {
                    drawRect(brush = brush1)
                    drawRect(brush = brush2, blendMode = BlendMode.Screen)
                    drawRect(brush = brush3, blendMode = BlendMode.Plus)
                    drawRect(brush = brushCenter, blendMode = BlendMode.Screen)
                }
            }
    )
}

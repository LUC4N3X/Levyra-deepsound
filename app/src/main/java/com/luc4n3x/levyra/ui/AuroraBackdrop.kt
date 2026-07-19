package com.luc4n3x.levyra.ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb

/**
 * Animated "aurora" mesh-gradient layer for the player backdrop.
 *
 * Uses an AGSL [RuntimeShader] (Android 13+, API 33) to render slow drifting light bands tinted by
 * the current track accent colors. Below API 33 the shader is unavailable and this composable draws
 * nothing, so the existing radial-orb backdrop shows through unchanged. Motion is also skipped when
 * [enabled] is false (animation preference / battery saver), leaving a static single frame.
 *
 * The animation only invalidates the draw phase (a frame-clock float read inside the Canvas draw),
 * never the composition, keeping it cheap and scoped to the one screen that hosts it.
 */
@Composable
fun AuroraBackdrop(
    primary: Color,
    secondary: Color,
    accent: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { frame ->
                time = (frame - start) / 1_000_000_000f
            }
        }
    }

    val shader = remember { RuntimeShader(AURORA_AGSL) }
    val brush = remember(shader) { ShaderBrush(shader) }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        shader.setFloatUniform("iResolution", w, h)
        shader.setFloatUniform("iTime", time)
        shader.setColorUniform("uPrimary", primary.toArgb())
        shader.setColorUniform("uSecondary", secondary.toArgb())
        shader.setColorUniform("uAccent", accent.toArgb())
        drawRect(brush)
    }
}

private const val AURORA_AGSL = """
uniform float2 iResolution;
uniform float iTime;
layout(color) uniform half4 uPrimary;
layout(color) uniform half4 uSecondary;
layout(color) uniform half4 uAccent;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float t = iTime * 0.06;

    // Two soft, drifting light bands built from layered sine waves.
    float wave1 = sin(uv.x * 3.1 + t * 2.0) * 0.10 + sin(uv.x * 6.4 - t * 1.3) * 0.05;
    float wave2 = sin(uv.x * 2.3 - t * 1.6) * 0.09 + sin(uv.x * 5.1 + t * 0.9) * 0.045;

    // smoothstep requires edge0 < edge1 (edge0 > edge1 is undefined in AGSL/GLSL),
    // so express the descending falloff as 1.0 - smoothstep(lower, upper, x).
    float band1 = 1.0 - smoothstep(0.0, 0.34, abs(uv.y - (0.30 + wave1)));
    float band2 = 1.0 - smoothstep(0.0, 0.40, abs(uv.y - (0.62 - wave2)));
    float glow = 0.5 + 0.5 * sin(uv.x * 4.0 + t * 1.4);

    half3 rgb = uPrimary.rgb * band1 * 0.55
              + uSecondary.rgb * band2 * 0.48
              + uAccent.rgb * glow * band1 * 0.20;

    float alpha = clamp(band1 * 0.55 + band2 * 0.42, 0.0, 0.78);
    // Fade the whole layer toward the bottom so it blends into the darker backdrop base.
    alpha *= 1.0 - smoothstep(0.15, 1.0, uv.y);
    return half4(rgb * alpha, alpha);
}
"""

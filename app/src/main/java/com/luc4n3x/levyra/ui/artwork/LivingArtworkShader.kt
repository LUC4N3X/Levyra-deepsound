package com.luc4n3x.levyra.ui.artwork

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

internal const val LIVING_ARTWORK_AGSL = """
uniform float2 uSize;
uniform float uTime;
uniform float uIntensity;
uniform float uSeed;
layout(color) uniform half4 uTone0;
layout(color) uniform half4 uTone1;
layout(color) uniform half4 uTone2;
layout(color) uniform half4 uTone3;
layout(color) uniform half4 uTone4;

float field(float2 uv, float2 center, float radius) {
    float d = distance(uv, center) / radius;
    float f = 1.0 - clamp(d, 0.0, 1.0);
    return f * f * (3.0 - 2.0 * f);
}

float2 flowWarp(float2 uv, float t, float seed) {
    float2 p = uv - float2(0.5, 0.5);
    float xFlow = sin((p.y + p.x * 0.34) * 5.0 + t * 0.082 + seed * 1.37);
    float yFlow = cos((p.x - p.y * 0.29) * 4.6 - t * 0.071 + seed * 1.11);
    return float2(xFlow, yFlow) * 0.050;
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / uSize;
    float t = uTime;
    float seed = uSeed;
    float2 warpedUv = uv + flowWarp(uv, t, seed);

    float2 c0 = float2(
        0.27 + 0.18 * sin(t * 0.105 + seed * 0.91),
        0.29 + 0.15 * cos(t * 0.121 + seed * 1.17)
    );
    float2 c1 = float2(
        0.73 + 0.16 * cos(t * 0.087 + seed * 1.43),
        0.27 + 0.17 * sin(t * 0.113 + seed * 0.69)
    );
    float2 c2 = float2(
        0.52 + 0.20 * sin(t * 0.071 + seed * 1.09 + 1.7),
        0.70 + 0.14 * cos(t * 0.097 + seed * 0.77 + 0.6)
    );
    float2 c3 = float2(
        0.21 + 0.14 * cos(t * 0.079 + seed * 1.31 + 2.4),
        0.73 + 0.16 * sin(t * 0.067 + seed * 0.57 + 1.1)
    );
    float2 c4 = float2(
        0.79 + 0.13 * sin(t * 0.061 + seed * 0.63 + 3.1),
        0.65 + 0.13 * cos(t * 0.083 + seed * 1.23 + 2.2)
    );

    float breathe = 0.95 + 0.05 * sin(t * 0.19 + seed * 0.4);
    float w0 = field(warpedUv, c0, 0.63 * breathe);
    float w1 = field(warpedUv, c1, 0.58 * breathe);
    float w2 = field(warpedUv, c2, 0.60 * breathe);
    float w3 = field(warpedUv, c3, 0.51 * breathe);
    float w4 = field(warpedUv, c4, 0.47 * breathe);

    float total = w0 + w1 + w2 + w3 + w4;
    if (total <= 0.0001) {
        return half4(0.0);
    }

    half3 blended =
        uTone0.rgb * half(w0) +
        uTone1.rgb * half(w1) +
        uTone2.rgb * half(w2) +
        uTone3.rgb * half(w3) +
        uTone4.rgb * half(w4);
    blended = blended / half(total);

    float coverage = clamp(total * 0.52, 0.0, 1.0);
    float edgeDistance = distance(uv, float2(0.5, 0.48));
    float vignette = 1.0 - 0.30 * smoothstep(0.28, 0.78, edgeDistance);
    float controlSafe = 1.0 - 0.16 * smoothstep(0.64, 1.0, uv.y);
    half alpha = half(clamp(coverage * vignette * controlSafe * uIntensity, 0.0, 1.0));
    return half4(blended * alpha, alpha);
}
"""

internal fun livingArtworkShaderSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun createLivingArtworkShader(): RuntimeShader? = try {
    RuntimeShader(LIVING_ARTWORK_AGSL)
} catch (error: RuntimeException) {
    null
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun RuntimeShader.applyLivingArtworkTones(tones: List<Color>): Boolean = try {
    for (index in 0 until livingArtworkToneCount()) {
        val color = tones.getOrElse(index) { tones.lastOrNull() ?: Color.Black }
        setColorUniform("uTone$index", color.toArgb())
    }
    setFloatUniform("uSeed", livingArtworkSeed(tones))
    true
} catch (error: IllegalArgumentException) {
    false
}

internal fun livingArtworkSeed(tones: List<Color>): Float {
    var hash = 0x811C9DC5.toInt()
    for (index in 0 until livingArtworkToneCount()) {
        val color = tones.getOrElse(index) { tones.lastOrNull() ?: Color.Black }
        hash = (hash xor color.toArgb()) * 16777619
    }
    return (hash ushr 1) / Int.MAX_VALUE.toFloat() * 6.2831855f
}

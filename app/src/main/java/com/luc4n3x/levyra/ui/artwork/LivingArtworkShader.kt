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

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / uSize;
    float t = uTime;

    float2 c0 = float2(0.30 + 0.17 * sin(t * 0.19), 0.30 + 0.14 * cos(t * 0.23));
    float2 c1 = float2(0.74 + 0.15 * cos(t * 0.15), 0.28 + 0.16 * sin(t * 0.21));
    float2 c2 = float2(0.52 + 0.19 * sin(t * 0.11 + 1.7), 0.72 + 0.13 * cos(t * 0.17 + 0.6));
    float2 c3 = float2(0.20 + 0.13 * cos(t * 0.13 + 2.4), 0.74 + 0.15 * sin(t * 0.12 + 1.1));
    float2 c4 = float2(0.80 + 0.12 * sin(t * 0.09 + 3.1), 0.66 + 0.12 * cos(t * 0.14 + 2.2));

    float breathe = 0.94 + 0.06 * sin(t * 0.27);
    float w0 = field(uv, c0, 0.62 * breathe);
    float w1 = field(uv, c1, 0.56 * breathe);
    float w2 = field(uv, c2, 0.58 * breathe);
    float w3 = field(uv, c3, 0.48 * breathe);
    float w4 = field(uv, c4, 0.44 * breathe);

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

    float coverage = clamp(total * 0.55, 0.0, 1.0);
    float vignette = 1.0 - 0.35 * clamp(distance(uv, float2(0.5, 0.5)) * 1.35, 0.0, 1.0);
    half alpha = half(clamp(coverage * vignette * uIntensity, 0.0, 1.0));
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
    true
} catch (error: IllegalArgumentException) {
    false
}

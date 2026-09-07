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
layout(color) uniform half4 uBase;
layout(color) uniform half4 uTone0;
layout(color) uniform half4 uTone1;
layout(color) uniform half4 uTone2;
layout(color) uniform half4 uTone3;
layout(color) uniform half4 uTone4;

float hash21(float2 p) {
    p = fract(p * float2(123.34, 345.45));
    float n = dot(p, p + float2(34.345, 34.345));
    p += float2(n, n);
    return fract(p.x * p.y);
}

float valueNoise(float2 p) {
    float2 cell = floor(p);
    float2 local = fract(p);
    float2 curve = local * local * (float2(3.0, 3.0) - 2.0 * local);
    float a = hash21(cell);
    float b = hash21(cell + float2(1.0, 0.0));
    float c = hash21(cell + float2(0.0, 1.0));
    float d = hash21(cell + float2(1.0, 1.0));
    return mix(mix(a, b, curve.x), mix(c, d, curve.x), curve.y);
}

float fbm(float2 p) {
    float value = valueNoise(p) * 0.58;
    p = p * 2.03 + float2(17.1, 9.2);
    value += valueNoise(p) * 0.28;
    p = p * 2.01 + float2(8.3, 19.7);
    value += valueNoise(p) * 0.14;
    return value;
}

float2 rotatePoint(float2 p, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return float2(c * p.x - s * p.y, s * p.x + c * p.y);
}

half4 main(float2 fragCoord) {
    float minSide = max(min(uSize.x, uSize.y), 1.0);
    float2 uv = fragCoord / uSize;
    float2 p = (fragCoord * 2.0 - uSize) / minSide;
    float t = uTime;
    float seed = uSeed;

    float2 drift = float2(t * 0.050, -t * 0.036);
    float warpX = fbm(p * 0.92 + drift + float2(seed * 0.17, seed * 0.09));
    float warpY = fbm(p * 0.92 - drift + float2(5.73 + seed * 0.11, 2.19 - seed * 0.07));
    float2 warped = p + (float2(warpX, warpY) - float2(0.5, 0.5)) * 0.58;

    float radius = length(warped);
    float lens = sin(radius * 3.6 - t * 0.13 + seed * 0.63) * 0.08;
    warped = rotatePoint(warped, lens);
    warped += warped * (0.032 * sin(radius * 2.7 + t * 0.09 + seed));

    float2 mesh = warped * 0.52;
    float mask0 = smoothstep(0.50, 0.74, valueNoise(mesh * 0.66 + float2(t * 0.018, seed * 0.23)));
    float mask1 = smoothstep(0.52, 0.76, valueNoise(rotatePoint(mesh, 0.58) * 0.78 + float2(-t * 0.017 + 4.1, seed * 0.31 + 1.7)));
    float mask2 = smoothstep(0.54, 0.78, valueNoise(rotatePoint(mesh, -0.43) * 0.90 + float2(seed * 0.19 + 7.4, t * 0.015 + 3.2)));
    float mask3 = smoothstep(0.56, 0.80, valueNoise(mesh * 1.04 + float2(t * 0.013 + 2.8, -seed * 0.27 + 8.6)));
    float mask4 = smoothstep(0.58, 0.82, valueNoise(rotatePoint(mesh, 0.88) * 1.16 + float2(-t * 0.012 + 9.3, seed * 0.37 + 5.1)));

    float macroField = valueNoise(warped * 0.42 + float2(seed * 0.41, t * 0.010));
    float edgeField = smoothstep(0.26, 0.86, distance(uv, float2(0.5, 0.48)));

    half3 color = uBase.rgb;
    color = mix(color, uTone0.rgb, half(mask0 * 0.86));
    color = mix(color, uTone1.rgb, half(mask1 * 0.78));
    color = mix(color, uTone2.rgb, half(mask2 * 0.68));
    color = mix(color, uTone3.rgb, half(mask3 * 0.56));
    color = mix(color, uTone4.rgb, half(mask4 * 0.46));

    float sheenNoise = valueNoise(mesh * 1.80 + float2(t * 0.018, -t * 0.014) + float2(seed * 0.41, seed * 0.13));
    float sheen = 1.0 - smoothstep(0.18, 0.34, abs(sheenNoise - 0.5));
    color += half3(half(sheen * 0.012));

    float dominantCoverage = max(max(mask0, mask1), max(mask2, max(mask3, mask4)));
    float layeredCoverage = (mask0 + mask1 + mask2 + mask3 + mask4) * 0.06;
    float coverage = 0.22 + dominantCoverage * 0.32 + layeredCoverage + macroField * 0.10 + edgeField * 0.08;

    float edgeDistance = distance(uv, float2(0.5, 0.47));
    float vignette = 1.0 - 0.16 * smoothstep(0.36, 0.88, edgeDistance);

    float artworkDistance = distance(uv, float2(0.5, 0.34));
    float artworkSafe = 1.0 - 0.42 * (1.0 - smoothstep(0.18, 0.50, artworkDistance));

    float controlSafe = 1.0 - 0.24 * smoothstep(0.60, 1.0, uv.y);

    half alpha = half(clamp(coverage * vignette * artworkSafe * controlSafe * uIntensity, 0.0, 1.0));
    return half4(color * alpha, alpha);
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
internal fun RuntimeShader.applyLivingArtworkColors(colors: LivingArtworkColors): Boolean = try {
    setColorUniform("uBase", colors.base.toArgb())
    for (index in 0 until livingArtworkToneCount()) {
        val color = colors.tones.getOrElse(index) { colors.tones.lastOrNull() ?: Color.Black }
        setColorUniform("uTone$index", color.toArgb())
    }
    setFloatUniform("uSeed", livingArtworkSeed(colors.tones))
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

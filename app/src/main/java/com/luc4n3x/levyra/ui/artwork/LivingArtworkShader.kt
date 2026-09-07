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

float softRegion(float2 p, float2 center, float2 scale, float inner, float outer) {
    float d = length((p - center) * scale);
    return 1.0 - smoothstep(inner, outer, d);
}

half4 main(float2 fragCoord) {
    float minSide = max(min(uSize.x, uSize.y), 1.0);
    float2 uv = fragCoord / uSize;
    float2 p = (fragCoord * 2.0 - uSize) / minSide;
    float t = uTime;
    float seed = uSeed;

    float2 drift = float2(t * 0.045, -t * 0.033);
    float warpX = fbm(p * 0.88 + drift + float2(seed * 0.17, seed * 0.09));
    float warpY = fbm(p * 0.88 - drift + float2(5.73 + seed * 0.11, 2.19 - seed * 0.07));
    float2 warped = p + (float2(warpX, warpY) - float2(0.5, 0.5)) * 0.52;

    float radius = length(warped);
    float lens = sin(radius * 3.3 - t * 0.12 + seed * 0.63) * 0.07;
    warped = rotatePoint(warped, lens);
    warped += warped * (0.028 * sin(radius * 2.5 + t * 0.085 + seed));

    float2 c0 = float2(
        -0.50 + 0.20 * sin(t * 0.13 + seed),
        -0.40 + 0.17 * cos(t * 0.11 + seed * 0.71)
    );
    float2 c1 = float2(
        0.50 + 0.19 * cos(t * 0.12 + seed * 0.43),
        -0.22 + 0.19 * sin(t * 0.14 + seed * 0.57)
    );
    float2 c2 = float2(
        -0.38 + 0.19 * cos(t * 0.10 + seed * 0.82),
        0.45 + 0.16 * sin(t * 0.12 + seed * 0.36)
    );
    float2 c3 = float2(
        0.40 + 0.18 * sin(t * 0.11 + seed * 0.29),
        0.46 + 0.17 * cos(t * 0.13 + seed * 0.64)
    );
    float2 c4 = float2(
        0.02 + 0.22 * sin(t * 0.09 + seed * 0.51),
        0.08 + 0.17 * cos(t * 0.10 + seed * 0.93)
    );

    float region0 = softRegion(warped, c0, float2(0.78, 1.08), 0.08, 0.92);
    float region1 = softRegion(warped, c1, float2(0.86, 1.00), 0.07, 0.88);
    float region2 = softRegion(warped, c2, float2(0.82, 1.12), 0.09, 0.94);
    float region3 = softRegion(warped, c3, float2(0.90, 1.04), 0.08, 0.90);
    float region4 = softRegion(warped, c4, float2(1.02, 0.94), 0.06, 0.72);

    float regionTotal = max(region0 + region1 + region2 + region3 + region4, 0.001);
    half3 regionColor = (
        uTone0.rgb * half(region0) +
        uTone1.rgb * half(region1) +
        uTone2.rgb * half(region2) +
        uTone3.rgb * half(region3) +
        uTone4.rgb * half(region4)
    ) / half(regionTotal);

    float strongestRegion = max(max(region0, region1), max(region2, max(region3, region4)));
    float macroPresence = smoothstep(0.18, 0.82, strongestRegion);
    float palettePresence = 0.48 + macroPresence * 0.38;

    half3 color = mix(uBase.rgb, regionColor, half(palettePresence));

    float surfaceNoise = valueNoise(warped * 1.35 + float2(t * 0.014, -t * 0.011) + float2(seed * 0.37, seed * 0.16));
    color *= half(0.97 + surfaceNoise * 0.05);

    float edgeField = smoothstep(0.28, 0.88, distance(uv, float2(0.5, 0.48)));
    float coverage = 0.20 + macroPresence * 0.30 + edgeField * 0.07;

    float edgeDistance = distance(uv, float2(0.5, 0.47));
    float vignette = 1.0 - 0.15 * smoothstep(0.36, 0.88, edgeDistance);

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

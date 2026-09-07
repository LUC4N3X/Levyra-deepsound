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
    float value = 0.0;
    value += valueNoise(p) * 0.52;
    p = p * 2.03 + float2(17.1, 9.2);
    value += valueNoise(p) * 0.26;
    p = p * 2.01 + float2(8.3, 19.7);
    value += valueNoise(p) * 0.13;
    p = p * 2.04 + float2(13.7, 5.9);
    value += valueNoise(p) * 0.065;
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

    float2 drift = float2(t * 0.055, -t * 0.041);
    float warpX = fbm(p * 1.12 + drift + float2(seed * 0.17, seed * 0.09));
    float warpY = fbm(p * 1.12 - drift + float2(5.73 + seed * 0.11, 2.19 - seed * 0.07));
    float2 warped = p + (float2(warpX, warpY) - float2(0.5, 0.5)) * 0.56;

    float radius = length(warped);
    float lens = sin(radius * 4.4 - t * 0.17 + seed * 0.63) * 0.12;
    warped = rotatePoint(warped, lens);
    warped += warped * (0.055 * sin(radius * 3.2 + t * 0.11 + seed));

    float2 mesh = warped * 0.82;
    float mask0 = smoothstep(0.26, 0.78, fbm(mesh * 1.03 + float2(t * 0.026, seed * 0.23)));
    float mask1 = smoothstep(0.30, 0.80, fbm(mesh * 1.17 + float2(-t * 0.021 + 4.1, seed * 0.31 + 1.7)));
    float mask2 = smoothstep(0.32, 0.82, fbm(mesh * 1.31 + float2(seed * 0.19 + 7.4, t * 0.018 + 3.2)));
    float mask3 = smoothstep(0.34, 0.84, fbm(mesh * 1.46 + float2(t * 0.016 + 2.8, -seed * 0.27 + 8.6)));
    float mask4 = smoothstep(0.36, 0.86, fbm(mesh * 1.63 + float2(-t * 0.014 + 9.3, seed * 0.37 + 5.1)));

    half3 color = uBase.rgb;
    color = mix(color, uTone0.rgb, half(mask0 * 0.94));
    color = mix(color, uTone1.rgb, half(mask1 * 0.88));
    color = mix(color, uTone2.rgb, half(mask2 * 0.72));
    color = mix(color, uTone3.rgb, half(mask3 * 0.64));
    color = mix(color, uTone4.rgb, half(mask4 * 0.58));

    float silkNoise = fbm(mesh * 2.35 + float2(t * 0.022, -t * 0.017) + float2(seed * 0.41, seed * 0.13));
    float silk = 1.0 - smoothstep(0.07, 0.26, abs(silkNoise - 0.5));
    color += half3(half(silk * 0.055));

    float coverageNoise = fbm(mesh * 0.71 + float2(seed * 0.29, t * 0.012));
    float coverage = 0.58 + coverageNoise * 0.34;
    float edgeDistance = distance(uv, float2(0.5, 0.47));
    float vignette = 1.0 - 0.24 * smoothstep(0.30, 0.80, edgeDistance);
    float controlSafe = 1.0 - 0.18 * smoothstep(0.63, 1.0, uv.y);
    half alpha = half(clamp(coverage * vignette * controlSafe * uIntensity, 0.0, 1.0));
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

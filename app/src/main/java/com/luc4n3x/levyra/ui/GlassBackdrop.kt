package com.luc4n3x.levyra.ui

import android.app.ActivityManager
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader as AndroidShader
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import com.luc4n3x.levyra.ui.theme.rememberPowerSaveMode
import kotlin.math.ceil

/**
 * Lightweight, dependency-free backdrop-blur system for Levyra "real glass" panels.
 *
 * The backdrop composable records itself into a shared [GraphicsLayer] via
 * [glassBackdropSource]. Each glass panel then re-samples that layer, translated to its
 * own on-screen position, into a private frost layer carrying a blur [AndroidRenderEffect],
 * producing genuine frosted glass that shows the blurred backdrop underneath — instead of the
 * previous flat translucent overlay.
 *
 * Everything degrades gracefully: on Android < 12 (no RenderEffect), when the shared layer is
 * unavailable, or when the caller disables the effect (animation preference / battery saver),
 * panels fall back to the classic translucent tint. No work runs off the main thread here; the
 * cost is a per-frame layer record scoped to a single screen, gated by [GlassBackdropState.enabled].
 */
@Stable
class GlassBackdropState {
    /** Whether real blur sampling is active. False keeps every panel on the translucent fallback. */
    var enabled: Boolean by mutableStateOf(false)

    /** Shared layer holding the recorded backdrop pixels. Null until the source composes. */
    var layer: GraphicsLayer? by mutableStateOf(null)

    /** Position of the backdrop source in the composition root, used to align panel samples. */
    var sourceOrigin: Offset by mutableStateOf(Offset.Zero)
}

/** Provides the active [GlassBackdropState] to descendants so panels can opt in without plumbing. */
val LocalGlassBackdrop = compositionLocalOf<GlassBackdropState?> { null }

private val blurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

internal enum class GlassBackdropQuality(val resolutionScale: Float) {
    Native(1f),
    Balanced(0.75f),
    Efficient(0.5f)
}

internal fun resolveGlassBackdropQuality(blurRadiusDp: Float): GlassBackdropQuality = when {
    blurRadiusDp <= 20f -> GlassBackdropQuality.Native
    blurRadiusDp <= 26f -> GlassBackdropQuality.Balanced
    else -> GlassBackdropQuality.Efficient
}

internal fun glassBackdropLayerDimension(nativePixels: Float, resolutionScale: Float): Int =
    ceil(nativePixels.coerceAtLeast(0f) * resolutionScale.coerceIn(0.01f, 1f))
        .toInt()
        .coerceAtLeast(1)

@Composable
fun rememberGlassBackdropState(enabled: Boolean): GlassBackdropState {
    val state = remember { GlassBackdropState() }
    if (enabled && blurSupported) {
        state.layer = rememberGraphicsLayer()
        state.enabled = true
    } else {
        state.enabled = false
        state.layer = null
        state.sourceOrigin = Offset.Zero
    }
    return state
}

@Composable
fun rememberGlassBlurAllowed(): Boolean {
    if (!blurSupported || !LocalLevyraVisualCapabilities.current.heavyBlur) return false
    val context = LocalContext.current.applicationContext
    val lowRam = remember(context) {
        context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    }
    val powerSave = rememberPowerSaveMode()
    return !lowRam && !powerSave
}

/**
 * Marks the receiver as the blur source. Records its drawn content (backdrop gradients, aurora,
 * motion artwork) into the shared layer every frame and draws that layer so the backdrop stays
 * visible. Apply as the last modifier on the backdrop so it captures the inner draws.
 */
fun Modifier.glassBackdropSource(state: GlassBackdropState): Modifier {
    if (!state.enabled || state.layer == null) return this
    return this
        .onGloballyPositioned { state.sourceOrigin = it.positionInRoot() }
        .drawWithContent {
            val layer = state.layer
            if (layer != null) {
                layer.record(size = size.toIntSize()) {
                    this@drawWithContent.drawContent()
                }
                drawLayer(layer)
            } else {
                drawContent()
            }
        }
}

/**
 * Draws the blurred backdrop sample behind the receiver's content with [onDrawFrosted] on top of
 * it. Without real blur only [onDrawFallback] is drawn, so the surface keeps a solid material.
 */
fun Modifier.glassFrost(
    state: GlassBackdropState,
    blurRadius: Dp = 26.dp,
    groundColor: Color = Color.Transparent,
    refractionShape: Shape? = null,
    onDrawFrosted: DrawScope.() -> Unit,
    onDrawFallback: DrawScope.() -> Unit
): Modifier = composed {
    if (
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        !state.enabled ||
        state.layer == null
    ) {
        return@composed this.drawWithContent {
            onDrawFallback()
            drawContent()
        }
    }

    val style = LocalLevyraGlassStyle.current
    val density = LocalDensity.current
    val scaledRadius = blurRadius * style.blurScale
    val quality = remember(scaledRadius) { resolveGlassBackdropQuality(scaledRadius.value) }
    val resolutionScale = quality.resolutionScale
    val blurNativePx = with(density) { scaledRadius.toPx() }
    val saturation = style.saturation
    val lensShape = refractionShape?.takeIf {
        style.refraction && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
    val frost = rememberGraphicsLayer()
    val frame = remember { GlassFrostFrame() }
    var panelOrigin by remember { mutableStateOf(Offset.Zero) }

    this
        .onGloballyPositioned { panelOrigin = it.positionInRoot() }
        .drawWithContent {
            val source = state.layer
            if (source != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                frame.update(this, lensShape, blurNativePx, saturation, resolutionScale)
                val marginPx = frame.marginPx
                val dx = state.sourceOrigin.x - panelOrigin.x + marginPx
                val dy = state.sourceOrigin.y - panelOrigin.y + marginPx
                if (groundColor.alpha > 0f) drawRect(groundColor)
                frost.renderEffect = frame.effect
                frost.record(size = frame.layerSize) {
                    scale(resolutionScale, pivot = Offset.Zero) {
                        translate(dx, dy) {
                            drawLayer(source)
                        }
                    }
                }
                clipRect {
                    translate(-marginPx, -marginPx) {
                        scale(1f / resolutionScale, pivot = Offset.Zero) {
                            drawLayer(frost)
                        }
                    }
                }
                onDrawFrosted()
            } else {
                onDrawFallback()
            }
            drawContent()
        }
}

private class GlassFrostFrame {
    var effect: androidx.compose.ui.graphics.RenderEffect? = null
        private set
    var marginPx: Float = 0f
        private set
    var layerSize: IntSize = IntSize(1, 1)
        private set
    private var size: Size = Size.Unspecified
    private var lensShape: Shape? = null
    private var blurNativePx = -1f
    private var saturation = -1f
    private var resolutionScale = -1f
    private var layoutDirection: LayoutDirection? = null

    @RequiresApi(Build.VERSION_CODES.S)
    fun update(
        scope: DrawScope,
        lensShape: Shape?,
        blurNativePx: Float,
        saturation: Float,
        resolutionScale: Float
    ) {
        if (
            effect != null && size == scope.size && this.lensShape == lensShape &&
            this.blurNativePx == blurNativePx && this.saturation == saturation &&
            this.resolutionScale == resolutionScale && layoutDirection == scope.layoutDirection
        ) {
            return
        }
        size = scope.size
        this.lensShape = lensShape
        this.blurNativePx = blurNativePx
        this.saturation = saturation
        this.resolutionScale = resolutionScale
        layoutDirection = scope.layoutDirection
        val lens = lensShape?.let { glassLensGeometry(it, scope.size, scope.layoutDirection, scope) }
        marginPx = ceil(blurNativePx * GlassSampleMarginShare + (lens?.amountPx ?: 0f))
        layerSize = IntSize(
            glassBackdropLayerDimension(scope.size.width + marginPx * 2f, resolutionScale),
            glassBackdropLayerDimension(scope.size.height + marginPx * 2f, resolutionScale)
        )
        effect = glassFrostEffect(
            blurPx = blurNativePx * resolutionScale,
            saturation = saturation,
            lens = lens,
            panelSize = scope.size,
            marginPx = marginPx,
            resolutionScale = resolutionScale
        )
    }
}

internal class GlassLensGeometry(val cornerRadiusPx: Float, val heightPx: Float, val amountPx: Float)

internal fun glassLensGeometry(
    shape: Shape,
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density
): GlassLensGeometry? {
    val shortSide = minOf(size.width, size.height)
    if (shortSide < with(density) { GlassLensMinSide.toPx() }) return null
    val cornerRadius = when (val outline = shape.createOutline(size, layoutDirection, density)) {
        is Outline.Rectangle -> 0f
        is Outline.Rounded -> outline.roundRect.topLeftCornerRadius.x
        is Outline.Generic -> return null
    }
    return GlassLensGeometry(
        cornerRadiusPx = cornerRadius.coerceIn(0f, shortSide / 2f),
        heightPx = minOf(with(density) { GlassLensHeight.toPx() }, shortSide * GlassLensMaxShare),
        amountPx = minOf(with(density) { GlassLensAmount.toPx() }, shortSide * GlassLensAmountShare)
    )
}

@RequiresApi(Build.VERSION_CODES.S)
private fun glassFrostEffect(
    blurPx: Float,
    saturation: Float,
    lens: GlassLensGeometry?,
    panelSize: Size,
    marginPx: Float,
    resolutionScale: Float
): androidx.compose.ui.graphics.RenderEffect {
    val lensEffect = if (lens != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GlassLens.effect(lens, panelSize, marginPx, resolutionScale)
    } else {
        null
    }
    val blur = when {
        blurPx <= 0f -> lensEffect
        lensEffect != null -> AndroidRenderEffect.createBlurEffect(blurPx, blurPx, lensEffect, AndroidShader.TileMode.CLAMP)
        else -> AndroidRenderEffect.createBlurEffect(blurPx, blurPx, AndroidShader.TileMode.CLAMP)
    }
    val vibrant = if (saturation != 1f) {
        val filter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(saturation) })
        if (blur != null) {
            AndroidRenderEffect.createColorFilterEffect(filter, blur)
        } else {
            AndroidRenderEffect.createColorFilterEffect(filter)
        }
    } else {
        blur
    }
    return (vibrant ?: AndroidRenderEffect.createOffsetEffect(0f, 0f)).asComposeRenderEffect()
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object GlassLens {
    private val shader by lazy(LazyThreadSafetyMode.NONE) { RuntimeShader(GLASS_LENS_AGSL) }

    fun effect(lens: GlassLensGeometry, panelSize: Size, marginPx: Float, resolutionScale: Float): AndroidRenderEffect {
        shader.setFloatUniform("panelSize", panelSize.width * resolutionScale, panelSize.height * resolutionScale)
        shader.setFloatUniform("panelOrigin", marginPx * resolutionScale, marginPx * resolutionScale)
        shader.setFloatUniform("cornerRadius", lens.cornerRadiusPx * resolutionScale)
        shader.setFloatUniform("bandHeight", (lens.heightPx * resolutionScale).coerceAtLeast(1f))
        shader.setFloatUniform("amount", lens.amountPx * resolutionScale)
        return AndroidRenderEffect.createRuntimeShaderEffect(shader, "content")
    }
}

private const val GlassSampleMarginShare = 0.75f
private val GlassLensMinSide = 56.dp
private val GlassLensHeight = 14.dp
private val GlassLensAmount = 7.dp
private const val GlassLensMaxShare = 0.4f
private const val GlassLensAmountShare = 0.16f

private const val GLASS_LENS_AGSL = """
uniform shader content;
uniform float2 panelSize;
uniform float2 panelOrigin;
uniform float cornerRadius;
uniform float bandHeight;
uniform float amount;

float roundedBox(float2 p, float2 halfSize, float radius) {
    float2 q = abs(p) - halfSize + radius;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
}

half4 main(float2 coord) {
    float2 halfSize = panelSize * 0.5;
    float2 p = coord - panelOrigin - halfSize;
    float d = roundedBox(p, halfSize, cornerRadius);
    if (d > 0.0 || d < -bandHeight) {
        return content.eval(coord);
    }
    float2 gradient = float2(
        roundedBox(p + float2(1.0, 0.0), halfSize, cornerRadius) - roundedBox(p - float2(1.0, 0.0), halfSize, cornerRadius),
        roundedBox(p + float2(0.0, 1.0), halfSize, cornerRadius) - roundedBox(p - float2(0.0, 1.0), halfSize, cornerRadius)
    );
    float magnitude = length(gradient);
    if (magnitude < 0.0001) {
        return content.eval(coord);
    }
    float t = 1.0 + d / bandHeight;
    return content.eval(coord + (gradient / magnitude) * (t * t * amount));
}
"""

/**
 * Renders the receiver as a frosted-glass surface: blurred backdrop sample + [tint] + [borderColor]
 * outline, clipped to [shape]. Replaces a `.background(...).border(...)` pair on a panel.
 * Falls back to a flat [fallbackColor] fill when real blur is unavailable.
 */
fun Modifier.glassSurface(
    state: GlassBackdropState,
    shape: Shape,
    tint: Color,
    fallbackColor: Color,
    borderColor: Color,
    blurRadius: Dp = 26.dp,
    borderWidth: Dp = 1.dp,
    highlightAlpha: Float = 0f,
    refraction: Boolean = false
): Modifier {
    val sheen = if (highlightAlpha > 0f) {
        Brush.verticalGradient(0f to Color.White.copy(alpha = highlightAlpha), 0.45f to Color.Transparent)
    } else {
        null
    }
    val rim = Brush.verticalGradient(
        0f to borderColor,
        1f to borderColor.copy(alpha = borderColor.alpha * GlassRimBottomShare)
    )
    return this
        .clip(shape)
        .glassFrost(
            state = state,
            blurRadius = blurRadius,
            refractionShape = shape.takeIf { refraction },
            onDrawFrosted = {
                drawRect(tint)
                if (sheen != null) drawRect(sheen)
            },
            onDrawFallback = { drawRect(fallbackColor) }
        )
        .border(borderWidth, rim, shape)
}

private const val GlassRimBottomShare = 0.38f

@Immutable
data class LevyraGlassStyle(
    val blur: Int = DEFAULT_LEVEL,
    val transparency: Int = DEFAULT_LEVEL,
    val tint: Int = DEFAULT_LEVEL,
    val refraction: Boolean = true
) {
    val blurScale: Float get() = 0.5f + blur.coerceIn(0, 100) / 100f
    val tintAlphaScale: Float get() = 1.45f - transparency.coerceIn(0, 100) / 100f * 0.9f
    val tintMix: Float get() = tint.coerceIn(0, 100) / 100f * 0.2f
    val saturation: Float get() = 1f + tint.coerceIn(0, 100) / 100f * 0.36f

    companion object {
        const val DEFAULT_LEVEL = 50
    }
}

val LocalLevyraGlassStyle = staticCompositionLocalOf { LevyraGlassStyle() }

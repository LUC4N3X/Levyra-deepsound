package com.luc4n3x.levyra.ui

import android.app.ActivityManager
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader as AndroidShader
import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
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

    val density = LocalDensity.current
    val quality = remember(blurRadius) { resolveGlassBackdropQuality(blurRadius.value) }
    val resolutionScale = quality.resolutionScale
    val blurPx = with(density) { blurRadius.toPx() * resolutionScale }
    val blurEffect = remember(blurPx) {
        AndroidRenderEffect
            .createBlurEffect(blurPx, blurPx, AndroidShader.TileMode.CLAMP)
            .asComposeRenderEffect()
    }
    val frost = rememberGraphicsLayer()
    var panelOrigin by remember { mutableStateOf(Offset.Zero) }

    this
        .onGloballyPositioned { panelOrigin = it.positionInRoot() }
        .drawWithContent {
            val source = state.layer
            if (source != null) {
                val dx = state.sourceOrigin.x - panelOrigin.x
                val dy = state.sourceOrigin.y - panelOrigin.y
                if (groundColor.alpha > 0f) drawRect(groundColor)
                frost.renderEffect = blurEffect
                val backdropLayerSize = IntSize(
                    glassBackdropLayerDimension(size.width, resolutionScale),
                    glassBackdropLayerDimension(size.height, resolutionScale)
                )
                frost.record(size = backdropLayerSize) {
                    scale(resolutionScale, pivot = Offset.Zero) {
                        translate(dx, dy) {
                            drawLayer(source)
                        }
                    }
                }
                scale(1f / resolutionScale, pivot = Offset.Zero) {
                    drawLayer(frost)
                }
                onDrawFrosted()
            } else {
                onDrawFallback()
            }
            drawContent()
        }
}

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
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .glassFrost(
        state = state,
        blurRadius = blurRadius,
        onDrawFrosted = { drawRect(tint) },
        onDrawFallback = { drawRect(fallbackColor) }
    )
    .border(borderWidth, borderColor, shape)

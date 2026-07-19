package com.luc4n3x.levyra.ui

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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize

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

@Composable
fun rememberGlassBackdropState(enabled: Boolean): GlassBackdropState {
    val layer = rememberGraphicsLayer()
    val state = remember { GlassBackdropState() }
    state.layer = layer
    state.enabled = enabled && blurSupported
    return state
}

/**
 * Marks the receiver as the blur source. Records its drawn content (backdrop gradients, aurora,
 * motion artwork) into the shared layer every frame and draws that layer so the backdrop stays
 * visible. Apply as the last modifier on the backdrop so it captures the inner draws.
 */
fun Modifier.glassBackdropSource(state: GlassBackdropState): Modifier {
    if (!state.enabled) return this
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
): Modifier = composed {
    val density = LocalDensity.current
    val blurPx = with(density) { blurRadius.toPx() }
    val active = state.enabled && blurSupported
    val blurEffect = remember(blurPx, active) {
        if (active) {
            AndroidRenderEffect
                .createBlurEffect(blurPx, blurPx, AndroidShader.TileMode.CLAMP)
                .asComposeRenderEffect()
        } else {
            null
        }
    }
    val frost = rememberGraphicsLayer()
    var panelOrigin by remember { mutableStateOf(Offset.Zero) }

    this
        .onGloballyPositioned { panelOrigin = it.positionInRoot() }
        .clip(shape)
        .drawWithContent {
            val source = state.layer
            if (active && source != null && blurEffect != null) {
                val dx = state.sourceOrigin.x - panelOrigin.x
                val dy = state.sourceOrigin.y - panelOrigin.y
                frost.renderEffect = blurEffect
                frost.record(size = size.toIntSize()) {
                    translate(dx, dy) {
                        drawLayer(source)
                    }
                }
                drawLayer(frost)
                drawRect(tint)
            } else {
                drawRect(fallbackColor)
            }
            drawContent()
        }
        .border(borderWidth, borderColor, shape)
}

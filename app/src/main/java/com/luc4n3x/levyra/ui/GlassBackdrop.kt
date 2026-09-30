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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import com.luc4n3x.levyra.ui.components.LocalLevyraLiquidGlassEnabled
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import com.luc4n3x.levyra.ui.theme.rememberPowerSaveMode

/**
 * Lightweight backdrop sampler used by Levyra's shared Liquid Glass surfaces.
 *
 * A host records only the backdrop/content that sits behind glass into one shared [GraphicsLayer].
 * Consumers sample that layer later, so a glass surface never records itself and cannot create a
 * rendering feedback loop. Full blur is allocated only when the user preference, platform and
 * visual-performance policy all allow it.
 */
@Stable
class GlassBackdropState {
    var enabled: Boolean by mutableStateOf(false)
    var layer: GraphicsLayer? by mutableStateOf(null)
    var sourceOrigin: Offset by mutableStateOf(Offset.Zero)
}

val LocalGlassBackdrop = compositionLocalOf<GlassBackdropState?> { null }

private val blurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

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
 * Creates the shared backdrop layer only for the full Liquid Glass path. The global product toggle
 * is checked here as the final safety gate so legacy call sites cannot keep capture work alive after
 * the user switches Liquid Glass off.
 */
@Composable
fun rememberGlassBackdropState(enabled: Boolean): GlassBackdropState {
    val liquidGlassEnabled = LocalLevyraLiquidGlassEnabled.current
    val fullGlassAllowed = liquidGlassEnabled && enabled && rememberGlassBlurAllowed()
    val state = remember { GlassBackdropState() }
    if (fullGlassAllowed) {
        val layer = rememberGraphicsLayer()
        state.layer = layer
        state.enabled = true
    } else {
        state.enabled = false
        state.layer = null
        state.sourceOrigin = Offset.Zero
    }
    return state
}

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
    val blurPx = with(density) { blurRadius.toPx() }
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
                frost.record(size = size.toIntSize()) {
                    translate(dx, dy) {
                        drawLayer(source)
                    }
                }
                drawLayer(frost)
                onDrawFrosted()
            } else {
                onDrawFallback()
            }
            drawContent()
        }
}

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

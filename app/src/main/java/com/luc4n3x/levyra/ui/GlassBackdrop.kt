package com.luc4n3x.levyra.ui

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader as AndroidShader
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize

/** Rendering level selected automatically for the current device and power state. */
internal enum class LiquidGlassTier {
    Full,
    Lite,
    Static
}

internal data class LiquidGlassCapabilities(
    val apiLevel: Int,
    val lowRamDevice: Boolean,
    val powerSaveMode: Boolean,
    val hardwareAccelerated: Boolean,
    val enabled: Boolean
)

/** Pure selector kept separate so device-policy changes stay deterministic and testable. */
internal fun resolveLiquidGlassTier(capabilities: LiquidGlassCapabilities): LiquidGlassTier = when {
    !capabilities.enabled -> LiquidGlassTier.Static
    !capabilities.hardwareAccelerated -> LiquidGlassTier.Static
    capabilities.lowRamDevice -> LiquidGlassTier.Static
    capabilities.apiLevel < Build.VERSION_CODES.S -> LiquidGlassTier.Lite
    capabilities.powerSaveMode -> LiquidGlassTier.Lite
    else -> LiquidGlassTier.Full
}

@Stable
internal data class AdaptiveLiquidGlassProfile(
    val tier: LiquidGlassTier,
    val blurRadius: Dp,
    val sampleAlpha: Float,
    val tintAlpha: Float,
    val animated: Boolean
)

/**
 * Shared backdrop state. Full mode records the UI into a hardware layer; lighter tiers skip the
 * recording entirely so older or constrained devices pay no hidden rendering cost.
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
private fun rememberAdaptiveLiquidGlassProfile(enabled: Boolean): AdaptiveLiquidGlassProfile {
    val context = LocalContext.current
    val view = LocalView.current
    val activityManager = remember(context) {
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    }
    val powerManager = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }
    var powerSaveMode by remember(powerManager) {
        mutableStateOf(powerManager?.isPowerSaveMode == true)
    }

    DisposableEffect(context, powerManager) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                    powerSaveMode = powerManager?.isPowerSaveMode == true
                }
            }
        }
        val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    val tier = resolveLiquidGlassTier(
        LiquidGlassCapabilities(
            apiLevel = Build.VERSION.SDK_INT,
            lowRamDevice = activityManager?.isLowRamDevice == true,
            powerSaveMode = powerSaveMode,
            hardwareAccelerated = view.isHardwareAccelerated,
            enabled = enabled
        )
    )
    return when (tier) {
        LiquidGlassTier.Full -> AdaptiveLiquidGlassProfile(
            tier = tier,
            blurRadius = 24.dp,
            sampleAlpha = 0.18f,
            tintAlpha = 0.10f,
            animated = true
        )
        LiquidGlassTier.Lite -> AdaptiveLiquidGlassProfile(
            tier = tier,
            blurRadius = 0.dp,
            sampleAlpha = 0f,
            tintAlpha = 0.075f,
            animated = false
        )
        LiquidGlassTier.Static -> AdaptiveLiquidGlassProfile(
            tier = tier,
            blurRadius = 0.dp,
            sampleAlpha = 0f,
            tintAlpha = 0.045f,
            animated = false
        )
    }
}

@Composable
fun rememberGlassBackdropState(enabled: Boolean): GlassBackdropState {
    val layer = rememberGraphicsLayer()
    val state = remember { GlassBackdropState() }
    state.layer = layer
    state.enabled = enabled && blurSupported
    return state
}

/** Records the receiver as the shared source used by real-glass samples. */
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
 * Root host for Levyra's adaptive Liquid Glass chrome.
 *
 * Full mode re-samples and softly refracts the real pixels beneath the lower player/navigation
 * area. Lite and Static modes keep only a low-cost optical tint. The overlay has no pointer input,
 * so navigation and player controls retain their original behavior and accessibility semantics.
 */
@Composable
fun AdaptiveLiquidGlassHost(
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val profile = rememberAdaptiveLiquidGlassProfile(enabled)
    val backdrop = rememberGlassBackdropState(profile.tier == LiquidGlassTier.Full)
    val dark = isSystemInDarkTheme()

    CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
        Box(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .glassBackdropSource(backdrop)
            ) {
                content()
            }
            AdaptiveLiquidGlassChrome(
                state = backdrop,
                profile = profile,
                dark = dark,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
private fun AdaptiveLiquidGlassChrome(
    state: GlassBackdropState,
    profile: AdaptiveLiquidGlassProfile,
    dark: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val frostLayer = rememberGraphicsLayer()
    val blurPx = with(density) { profile.blurRadius.toPx() }
    val chromeHeightPx = with(density) { 176.dp.toPx() }
    val borderWidthPx = with(density) { 1.dp.toPx() }
    val blurEffect = remember(blurPx, profile.tier) {
        if (profile.tier == LiquidGlassTier.Full && blurSupported) {
            AndroidRenderEffect
                .createBlurEffect(blurPx, blurPx, AndroidShader.TileMode.CLAMP)
                .asComposeRenderEffect()
        } else {
            null
        }
    }
    val transition = rememberInfiniteTransition(label = "levyraLiquidGlass")
    val sheenPhase by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = if (profile.animated) 1.35f else -0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "liquidGlassSheen"
    )

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val chromeTop = (size.height - chromeHeightPx).coerceAtLeast(0f)
        val chromeHeight = size.height - chromeTop
        val source = state.layer

        if (
            profile.tier == LiquidGlassTier.Full &&
            state.enabled &&
            source != null &&
            blurEffect != null
        ) {
            frostLayer.renderEffect = blurEffect
            frostLayer.alpha = profile.sampleAlpha
            frostLayer.record(size = size.toIntSize()) {
                val refractionShift = (sheenPhase - 0.5f) * 8.dp.toPx()
                translate(left = refractionShift, top = 0f) {
                    drawLayer(source)
                }
            }
            clipRect(left = 0f, top = chromeTop, right = size.width, bottom = size.height) {
                drawLayer(frostLayer)
            }
            frostLayer.renderEffect = null
            frostLayer.alpha = 1f
        }

        val baseTint = if (dark) Color(0xFF080A10) else Color.White
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    baseTint.copy(alpha = profile.tintAlpha * 0.45f),
                    baseTint.copy(alpha = profile.tintAlpha)
                ),
                startY = chromeTop,
                endY = size.height
            ),
            topLeft = Offset(0f, chromeTop),
            size = Size(size.width, chromeHeight)
        )

        if (profile.animated) {
            val centerX = size.width * sheenPhase
            val sheenWidth = size.width * 0.42f
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = if (dark) 0.045f else 0.09f),
                        Color.Transparent
                    ),
                    startX = centerX - sheenWidth,
                    endX = centerX + sheenWidth
                ),
                topLeft = Offset(0f, chromeTop),
                size = Size(size.width, chromeHeight)
            )
        }

        drawLine(
            color = Color.White.copy(alpha = if (dark) 0.12f else 0.28f),
            start = Offset(0f, chromeTop),
            end = Offset(size.width, chromeTop),
            strokeWidth = borderWidthPx
        )
    }
}

/**
 * Optional panel-level real glass for smaller surfaces. It preserves the existing API while using
 * the shared adaptive backdrop when available and falling back to an ordinary translucent fill.
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
                    translate(dx, dy) { drawLayer(source) }
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

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import android.graphics.Bitmap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
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

    var sampling: Boolean by mutableStateOf(true)

    internal var legacy: Boolean = false

    internal var legacyFrost: ImageBitmap? by mutableStateOf(null)

    internal var legacyScale: Float = 1f

    internal val legacyPanels = AtomicInteger(0)

    internal val legacyDirty = Channel<Unit>(Channel.CONFLATED)
}

/** Provides the active [GlassBackdropState] to descendants so panels can opt in without plumbing. */
val LocalGlassBackdrop = compositionLocalOf<GlassBackdropState?> { null }

private val blurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private val legacyBlurSupported: Boolean
    get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S

internal enum class GlassBackdropQuality(val resolutionScale: Float) {
    Native(1f),
    Balanced(0.75f),
    Efficient(0.5f)
}

internal fun resolveGlassBackdropQuality(blurRadiusDp: Float): GlassBackdropQuality = when {
    blurRadiusDp <= 12f -> GlassBackdropQuality.Native
    blurRadiusDp <= 18f -> GlassBackdropQuality.Balanced
    else -> GlassBackdropQuality.Efficient
}

internal fun glassBackdropLayerDimension(nativePixels: Float, resolutionScale: Float): Int =
    ceil(nativePixels.coerceAtLeast(0f) * resolutionScale.coerceIn(0.01f, 1f))
        .toInt()
        .coerceAtLeast(1)

@Composable
fun rememberGlassBackdropState(enabled: Boolean): GlassBackdropState {
    val state = remember { GlassBackdropState() }
    if (enabled && (blurSupported || legacyBlurSupported)) {
        state.layer = rememberGraphicsLayer()
        state.enabled = true
        state.legacy = !blurSupported
        if (state.legacy) LegacyGlassCapture(state)
    } else {
        state.enabled = false
        state.layer = null
        state.sourceOrigin = Offset.Zero
        state.legacyFrost = null
    }
    return state
}

@Composable
private fun LegacyGlassCapture(state: GlassBackdropState) {
    val scratch = rememberGraphicsLayer()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val style = LocalLevyraGlassStyle.current
    val blurPx = with(density) { (LegacyGlassBlurRadius * style.blurScale).toPx() }
    val saturation = style.saturation
    LaunchedEffect(state, blurPx, saturation) {
        val buffers = LegacyGlassBuffers()
        delay(LegacyGlassStartupDelayMs)
        state.legacyDirty.trySend(Unit)
        while (isActive) {
            state.legacyDirty.receive()
            delay(LegacyGlassSettleMs)
            if (!state.sampling || state.legacyPanels.get() == 0) continue
            captureLegacyGlass(state, scratch, density, layoutDirection, blurPx, saturation, buffers)
            delay(LegacyGlassIntervalMs)
        }
    }
    DisposableEffect(state) {
        onDispose { state.legacyFrost = null }
    }
}

private class LegacyGlassBuffers {
    private val outputs = arrayOfNulls<Bitmap>(2)
    private var next = 0
    var pixels = IntArray(0)
        private set
    var scratch = IntArray(0)
        private set

    fun output(width: Int, height: Int): Bitmap {
        val current = outputs[next]?.takeIf { it.width == width && it.height == height && !it.isRecycled }
            ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { outputs[next] = it }
        next = 1 - next
        return current
    }

    fun ensure(size: Int) {
        if (pixels.size != size) {
            pixels = IntArray(size)
            scratch = IntArray(size)
        }
    }
}

private suspend fun captureLegacyGlass(
    state: GlassBackdropState,
    scratch: GraphicsLayer,
    density: Density,
    layoutDirection: LayoutDirection,
    blurPx: Float,
    saturation: Float,
    buffers: LegacyGlassBuffers
) {
    val source = state.layer ?: return
    val sourceSize = source.size
    if (sourceSize.width <= 0 || sourceSize.height <= 0) return
    val captureScale = minOf(LegacyGlassMaxScale, LegacyGlassMaxSide / maxOf(sourceSize.width, sourceSize.height).toFloat())
    val width = ceil(sourceSize.width * captureScale).toInt().coerceAtLeast(1)
    val height = ceil(sourceSize.height * captureScale).toInt().coerceAtLeast(1)
    scratch.record(density, layoutDirection, IntSize(width, height)) {
        scale(captureScale, pivot = Offset.Zero) { drawLayer(source) }
    }
    val snapshot = try {
        scratch.toImageBitmap()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: RuntimeException) {
        timber.log.Timber.w(error, "Legacy glass capture failed")
        return
    }
    val frost = withContext(Dispatchers.Default) {
        val captured = snapshot.asAndroidBitmap()
        val readable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && captured.config == Bitmap.Config.HARDWARE) {
            captured.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            captured
        }
        val w = readable.width
        val h = readable.height
        buffers.ensure(w * h)
        readable.getPixels(buffers.pixels, 0, w, 0, 0, w, h)
        if (readable !== captured) readable.recycle()
        LegacyGlassBlur.blur(buffers.pixels, buffers.scratch, w, h, (blurPx * captureScale).toInt().coerceIn(1, LegacyGlassMaxRadius), saturation)
        buffers.output(w, h).also { it.setPixels(buffers.pixels, 0, w, 0, 0, w, h) }
    }
    state.legacyScale = captureScale
    state.legacyFrost = frost.asImageBitmap()
}

internal object LegacyGlassBlur {
    fun blur(pixels: IntArray, scratch: IntArray, width: Int, height: Int, radius: Int, saturation: Float) {
        repeat(LegacyGlassPasses) {
            boxPass(pixels, scratch, width, height, radius, horizontal = true)
            boxPass(scratch, pixels, width, height, radius, horizontal = false)
        }
        if (saturation != 1f) saturate(pixels, saturation)
    }

    private fun boxPass(input: IntArray, output: IntArray, width: Int, height: Int, radius: Int, horizontal: Boolean) {
        val lines = if (horizontal) height else width
        val length = if (horizontal) width else height
        val step = if (horizontal) 1 else width
        val window = radius * 2 + 1
        for (line in 0 until lines) {
            val base = if (horizontal) line * width else line
            var a = 0
            var r = 0
            var g = 0
            var b = 0
            for (i in -radius..radius) {
                val color = input[base + i.coerceIn(0, length - 1) * step]
                a += color ushr 24
                r += color shr 16 and 0xFF
                g += color shr 8 and 0xFF
                b += color and 0xFF
            }
            for (i in 0 until length) {
                output[base + i * step] = (a / window shl 24) or (r / window shl 16) or (g / window shl 8) or b / window
                val outgoing = input[base + (i - radius).coerceIn(0, length - 1) * step]
                val incoming = input[base + (i + radius + 1).coerceIn(0, length - 1) * step]
                a += (incoming ushr 24) - (outgoing ushr 24)
                r += (incoming shr 16 and 0xFF) - (outgoing shr 16 and 0xFF)
                g += (incoming shr 8 and 0xFF) - (outgoing shr 8 and 0xFF)
                b += (incoming and 0xFF) - (outgoing and 0xFF)
            }
        }
    }

    private fun saturate(pixels: IntArray, saturation: Float) {
        for (index in pixels.indices) {
            val color = pixels[index]
            val r = color shr 16 and 0xFF
            val g = color shr 8 and 0xFF
            val b = color and 0xFF
            val gray = 0.2126f * r + 0.7152f * g + 0.0722f * b
            pixels[index] = (color and -0x1000000) or
                ((gray + (r - gray) * saturation).toInt().coerceIn(0, 255) shl 16) or
                ((gray + (g - gray) * saturation).toInt().coerceIn(0, 255) shl 8) or
                (gray + (b - gray) * saturation).toInt().coerceIn(0, 255)
        }
    }
}

private val LegacyGlassBlurRadius = 26.dp
private const val LegacyGlassMaxSide = 360f
private const val LegacyGlassMaxScale = 0.34f
private const val LegacyGlassMaxRadius = 24
private const val LegacyGlassPasses = 3
private const val LegacyGlassSettleMs = 110L
private const val LegacyGlassStartupDelayMs = 2_500L
private const val LegacyGlassIntervalMs = 240L

@Composable
fun rememberGlassBlurAllowed(): Boolean {
    if (!LocalLevyraVisualCapabilities.current.heavyBlur) return false
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
            if (layer != null && state.sampling) {
                layer.record(size = size.toIntSize()) {
                    this@drawWithContent.drawContent()
                }
                drawLayer(layer)
                if (state.legacy) state.legacyDirty.trySend(Unit)
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
    if (!state.enabled || state.layer == null) {
        return@composed this.drawWithContent {
            onDrawFallback()
            drawContent()
        }
    }
    if (state.legacy || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return@composed legacyGlassFrost(state, groundColor, onDrawFrosted, onDrawFallback)
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

@Composable
private fun Modifier.legacyGlassFrost(
    state: GlassBackdropState,
    groundColor: Color,
    onDrawFrosted: DrawScope.() -> Unit,
    onDrawFallback: DrawScope.() -> Unit
): Modifier {
    var panelOrigin by remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(state) {
        state.legacyPanels.incrementAndGet()
        state.legacyDirty.trySend(Unit)
        onDispose { state.legacyPanels.decrementAndGet() }
    }
    return this
        .onGloballyPositioned { panelOrigin = it.positionInRoot() }
        .drawWithContent {
            val frost = state.legacyFrost
            if (frost == null) {
                onDrawFallback()
            } else {
                if (groundColor.alpha > 0f) drawRect(groundColor)
                drawLegacyFrost(frost, state.legacyScale, panelOrigin - state.sourceOrigin)
                drawRect(LegacyGlassDepth)
                onDrawFrosted()
            }
            drawContent()
        }
}

private val LegacyGlassDepth = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.07f),
    0.22f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.05f)
)

private fun DrawScope.drawLegacyFrost(frost: ImageBitmap, scale: Float, offset: Offset) {
    val left = (offset.x * scale).coerceIn(0f, frost.width.toFloat())
    val top = (offset.y * scale).coerceIn(0f, frost.height.toFloat())
    val right = ((offset.x + size.width) * scale).coerceIn(0f, frost.width.toFloat())
    val bottom = ((offset.y + size.height) * scale).coerceIn(0f, frost.height.toFloat())
    if (right - left < 1f || bottom - top < 1f) return
    drawImage(
        image = frost,
        srcOffset = IntOffset(left.toInt(), top.toInt()),
        srcSize = IntSize((right - left).toInt().coerceAtLeast(1), (bottom - top).toInt().coerceAtLeast(1)),
        dstOffset = IntOffset((left / scale - offset.x).toInt(), (top / scale - offset.y).toInt()),
        dstSize = IntSize(ceil((right - left) / scale).toInt(), ceil((bottom - top) / scale).toInt()),
        filterQuality = FilterQuality.Low
    )
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

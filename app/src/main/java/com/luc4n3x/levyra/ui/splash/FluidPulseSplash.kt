package com.luc4n3x.levyra.ui.splash

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.R
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal object FluidPulseLaunch {
    private var claimed = false

    fun claim(savedInstanceState: Bundle?, intent: Intent?): Boolean {
        if (claimed) return false
        claimed = true
        return savedInstanceState == null &&
            intent?.action == Intent.ACTION_MAIN &&
            intent.hasCategory(Intent.CATEGORY_LAUNCHER)
    }

    fun syncSystemSplash(activity: Activity, enabled: Boolean) {
        if (Build.VERSION.SDK_INT < 33) return
        runCatching {
            activity.splashScreen.setSplashScreenTheme(if (enabled) R.style.Theme_Levyra else R.style.Theme_Levyra_SplashIcon)
        }
    }
}

@Stable
internal class FluidPulseSplashState(active: Boolean) {
    var visible by mutableStateOf(active)
        private set
    var contentReady by mutableStateOf(!active)
        private set
    private var elapsedMs by mutableFloatStateOf(0f)

    val timeMs: Float get() = elapsedMs

    fun contentScale(): Float =
        ContentStartScale + (1f - ContentStartScale) * Reveal.transform(window(elapsedMs, RevealStart, TotalMs))

    suspend fun play() {
        try {
            withTimeoutOrNull(SafetyTimeoutMs) {
                val scale = ValueAnimator.getDurationScale().takeIf { it > 0f } ?: 1f
                var last = withFrameNanos { it }
                contentReady = true
                while (elapsedMs < TotalMs) {
                    val now = withFrameNanos { it }
                    val frameMs = ((now - last) / 1_000_000f).coerceAtMost(MaxFrameStepMs)
                    last = now
                    elapsedMs = (elapsedMs + frameMs / scale).coerceAtMost(TotalMs)
                }
            }
        } finally {
            contentReady = true
            visible = false
        }
    }

    fun skip() {
        contentReady = true
        visible = false
    }
}

@Composable
internal fun rememberFluidPulseSplashState(
    requested: Boolean,
    startupAnimation: Boolean,
    animationsEnabled: Boolean
): FluidPulseSplashState = remember {
    FluidPulseSplashState(requested && startupAnimation && animationsEnabled && ValueAnimator.areAnimatorsEnabled())
}

internal fun Modifier.fluidPulseContentEntrance(state: FluidPulseSplashState): Modifier =
    if (!state.visible) this else graphicsLayer {
        val scale = state.contentScale()
        scaleX = scale
        scaleY = scale
    }

@Composable
internal fun FluidPulseSplash(state: FluidPulseSplashState, background: Color, accent: Color) {
    if (!state.visible) return
    val resources = LocalContext.current.resources
    val logo by produceState<ImageBitmap?>(null) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                BitmapFactory.decodeResource(resources, R.drawable.levyra_logo, BitmapFactory.Options().apply { inScaled = false })
                    ?.asImageBitmap()
            }.getOrNull()
        }
    }
    LaunchedEffect(state) { state.play() }
    val dark = background.luminance() < 0.5f
    val halo = lerp(BrandViolet, accent, 0.35f)
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(state) { detectTapGestures { state.skip() } }
            .drawWithCache {
                val side = min(LogoSide.toPx(), size.minDimension * 0.46f)
                val left = (size.width - side) / 2f
                val top = (size.height - side) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                val fill = Brush.linearGradient(
                    LogoGradient,
                    start = Offset(left + 0.27f * side, top + 0.80f * side),
                    end = Offset(left + 0.73f * side, top + 0.20f * side)
                )
                val style = FluidPulseStyle(
                    background = background,
                    fill = fill,
                    halo = Brush.radialGradient(
                        0f to halo.copy(alpha = if (dark) 0.30f else 0.18f),
                        0.55f to halo.copy(alpha = if (dark) 0.08f else 0.05f),
                        1f to Color.Transparent,
                        center = center,
                        radius = side * 0.9f
                    ),
                    ring = Brush.sweepGradient(RingGradient, center),
                    ringAlpha = if (dark) 0.7f else 0.5f,
                    sheen = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
                    seedWidth = 3.dp.toPx(),
                    ringWidth = 16.dp.toPx(),
                    ringWidthEnd = 1.5.dp.toPx(),
                    feather = 72.dp.toPx(),
                    farRadius = hypot(size.width, size.height) / 2f,
                    center = center
                )
                val geometry = FluidPulseGeometry(left, top, side)
                onDrawBehind { drawFluidPulse(state.timeMs, style, geometry, logo) }
            }
    )
}

private class FluidPulseStyle(
    val background: Color,
    val fill: Brush,
    val halo: Brush,
    val ring: Brush,
    val ringAlpha: Float,
    val sheen: Stroke,
    val seedWidth: Float,
    val ringWidth: Float,
    val ringWidthEnd: Float,
    val feather: Float,
    val farRadius: Float,
    val center: Offset
)

private fun DrawScope.drawFluidPulse(t: Float, style: FluidPulseStyle, geometry: FluidPulseGeometry, logo: ImageBitmap?) {
    val center = style.center
    val hole = Reveal.transform(window(t, RevealStart, TotalMs)) * (style.farRadius + style.feather)
    if (hole <= 1f) {
        drawRect(lerp(SystemSplashBackground, style.background, Standard.transform(window(t, 0f, AwakenEnd + 80f))))
    } else {
        val inner = ((hole - style.feather) / hole).coerceIn(0f, 0.999f)
        drawRect(
            Brush.radialGradient(
                0f to Color.Transparent,
                inner to Color.Transparent,
                1f to style.background,
                center = center,
                radius = hole
            )
        )
    }

    val awaken = Standard.transform(window(t, 0f, AwakenEnd))
    val logoFade = 1f - Standard.transform(window(t, RevealStart, RevealStart + 220f))
    drawCircle(style.halo, radius = geometry.side * 0.9f, center = center, alpha = (0.3f + 0.7f * awaken) * logoFade)

    for (i in 0 until 2) {
        val start = PulseStart + i * 110f
        val ring = Decelerate.transform(window(t, start, start + 460f))
        if (ring <= 0f || ring >= 1f) continue
        val base = geometry.side * 0.34f
        drawCircle(
            style.ring,
            radius = base + ring * (style.farRadius - base),
            center = center,
            alpha = style.ringAlpha * (1f - ring).pow(1.6f) * (if (i == 0) 1f else 0.55f),
            style = Stroke(width = style.ringWidth + (style.ringWidthEnd - style.ringWidth) * ring)
        )
    }

    val seedFade = 1f - window(t, FlowStart, FlowStart + 180f)
    if (awaken > 0f && seedFade > 0f) {
        val sx = geometry.seedX()
        val sy = geometry.seedY()
        val reach = geometry.side * (0.02f + 0.08f * awaken)
        drawLine(
            style.fill,
            start = Offset(sx - reach, sy),
            end = Offset(sx + reach, sy),
            strokeWidth = style.seedWidth * (0.5f + 0.5f * awaken),
            cap = StrokeCap.Round,
            alpha = (0.35f + 0.65f * awaken) * seedFade
        )
    }

    if (logoFade <= 0f) return
    val pulse = window(t, PulseStart - 20f, PulseStart + 300f)
    val elastic = if (pulse <= 0f || pulse >= 1f) 0f else 0.085f * exp(-4.2f * pulse) * sin(2f * PI.toFloat() * 1.15f * pulse)
    val logoScale = 1f + elastic + 0.07f * Standard.transform(window(t, RevealStart, RevealStart + 240f))

    scale(logoScale, pivot = center) {
        val imageAlpha = Standard.transform(window(t, MorphStart + 230f, MorphEnd))
        val shapeAlpha = 1f - window(t, MorphEnd, MorphEnd + 80f)
        if (t >= FlowStart && shapeAlpha > 0f) {
            geometry.update(t)
            drawPath(geometry.shape, style.fill, alpha = shapeAlpha * logoFade)
            val sheen = (1f - window(t, MorphStart, MorphStart + 260f)) * window(t, FlowStart, FlowStart + 120f)
            if (sheen > 0f) drawPath(geometry.sheen, SheenColor, alpha = 0.55f * sheen, style = style.sheen)
        }
        if (logo != null && imageAlpha > 0f) {
            drawImage(
                logo,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(logo.width, logo.height),
                dstOffset = IntOffset(geometry.left.roundToInt(), geometry.top.roundToInt()),
                dstSize = IntSize(geometry.side.roundToInt(), geometry.side.roundToInt()),
                alpha = imageAlpha * logoFade,
                filterQuality = FilterQuality.High
            )
        } else if (logo == null && t >= MorphEnd) {
            geometry.update(MorphEnd)
            drawPath(geometry.shape, style.fill, alpha = logoFade)
        }
    }
}

private class FluidPulseGeometry(val left: Float, val top: Float, val side: Float) {
    val shape = Path()
    val sheen = Path()
    private val flame = RibbonMotion(FlameFlow, FlameWave, FluidPulseLogoOutline.FlameOuter, FluidPulseLogoOutline.FlameInner)
    private val note = RibbonMotion(NoteFlow, NoteWave, FluidPulseLogoOutline.NoteOuter, FluidPulseLogoOutline.NoteInner)
    private val control = FloatArray(8)

    fun seedX(): Float = left + bezier(FlameFlow, 0, 0.5f) * side

    fun seedY(): Float = top + bezier(FlameFlow, 1, 0.5f) * side

    fun update(t: Float) {
        shape.rewind()
        sheen.rewind()
        val grow = Decelerate.transform(window(t, FlowStart, FlowEnd))
        val morph = window(t, MorphStart, MorphEnd)
        val spread = 1f + (FlowSpread - 1f) * (1f - Emphasized.transform(morph)) * grow
        val phase = 2f * PI.toFloat() * t / WavePeriodMs
        trace(flame, grow, morph, spread, phase)
        trace(note, grow, morph, spread, phase + 1.9f)
    }

    private fun trace(ribbon: RibbonMotion, grow: Float, morph: Float, spread: Float, phase: Float) {
        val amp = (1f - morph) * (0.35f + 0.65f * grow)
        ribbon.flow.copyInto(control)
        control[2] += ribbon.wave[0] * amp * sin(phase)
        control[3] += ribbon.wave[1] * amp * sin(phase + 0.7f)
        control[4] += ribbon.wave[2] * amp * sin(phase + 1.6f)
        control[5] += ribbon.wave[3] * amp * sin(phase + 2.3f)
        val width = RibbonWidth * (0.35f + 0.65f * grow)
        edge(ribbon.outer, forward = true, sign = 1f, grow = grow, width = width, morph = morph, spread = spread)
        edge(ribbon.inner, forward = false, sign = -1f, grow = grow, width = width, morph = morph, spread = spread)
        shape.close()
        if (morph >= 1f) return
        for (i in 0 until SheenSamples) {
            val param = 0.5f + (i / (SheenSamples - 1f) - 0.5f) * grow
            val x = x(spreadOf(bezier(control, 0, param), spread))
            val y = y(spreadOf(bezier(control, 1, param), spread))
            if (i == 0) sheen.moveTo(x, y) else sheen.lineTo(x, y)
        }
    }

    private fun edge(target: FloatArray, forward: Boolean, sign: Float, grow: Float, width: Float, morph: Float, spread: Float) {
        val count = target.size / 2
        for (i in 0 until count) {
            val f = i / (count - 1f)
            val along = if (forward) f else 1f - f
            val param = 0.5f + (along - 0.5f) * grow
            var px = bezier(control, 0, param)
            var py = bezier(control, 1, param)
            val dx = bezierSlope(control, 0, param)
            val dy = bezierSlope(control, 1, param)
            val length = sqrt(dx * dx + dy * dy)
            if (length > 1e-5f) {
                val half = width * taper(along) * 0.5f * sign
                px += -dy / length * half
                py += dx / length * half
            }
            px = spreadOf(px, spread)
            py = spreadOf(py, spread)
            val local = Emphasized.transform((morph * (1f + MorphStagger) - MorphStagger * along).coerceIn(0f, 1f))
            val mx = x(px + (target[2 * i] - px) * local)
            val my = y(py + (target[2 * i + 1] - py) * local)
            if (forward && i == 0) shape.moveTo(mx, my) else shape.lineTo(mx, my)
        }
    }

    private fun x(value: Float): Float = left + value * side

    private fun y(value: Float): Float = top + value * side

    private fun spreadOf(value: Float, spread: Float): Float = 0.5f + (value - 0.5f) * spread
}

private class RibbonMotion(val flow: FloatArray, val wave: FloatArray, val outer: FloatArray, val inner: FloatArray)

private fun bezier(p: FloatArray, axis: Int, t: Float): Float {
    val u = 1f - t
    return u * u * u * p[axis] + 3f * u * u * t * p[2 + axis] + 3f * u * t * t * p[4 + axis] + t * t * t * p[6 + axis]
}

private fun bezierSlope(p: FloatArray, axis: Int, t: Float): Float {
    val u = 1f - t
    return 3f * u * u * (p[2 + axis] - p[axis]) + 6f * u * t * (p[4 + axis] - p[2 + axis]) + 3f * t * t * (p[6 + axis] - p[4 + axis])
}

private fun taper(along: Float): Float = sin(PI.toFloat() * along.pow(0.8f)).coerceAtLeast(0f).pow(0.8f)

private fun window(t: Float, start: Float, end: Float): Float = ((t - start) / (end - start)).coerceIn(0f, 1f)

private const val TotalMs = 1400f
private const val AwakenEnd = 200f
private const val FlowStart = 140f
private const val FlowEnd = 560f
private const val MorphStart = 500f
private const val MorphEnd = 900f
private const val PulseStart = 880f
private const val RevealStart = 1060f
private const val WavePeriodMs = 900f
private const val MaxFrameStepMs = 48f
private const val SafetyTimeoutMs = 3_000L
private const val ContentStartScale = 0.965f
private const val FlowSpread = 1.3f
private const val MorphStagger = 0.3f
private const val RibbonWidth = 0.09f
private const val SheenSamples = 28
private val LogoSide = 164.dp

private val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val Emphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val Decelerate = CubicBezierEasing(0f, 0f, 0f, 1f)
private val Reveal = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

private val BrandDeep = Color(0xFF1440D8)
private val BrandAzure = Color(0xFF0A8CF6)
private val BrandCyan = Color(0xFF1FC6F2)
private val BrandViolet = Color(0xFF7A2BEF)
private val BrandMagenta = Color(0xFFF20E93)
private val SheenColor = Color(0xFF8FE6FF)
private val SystemSplashBackground = Color(0xFF05070D)
private val LogoGradient = listOf(BrandDeep, BrandAzure, BrandViolet, BrandMagenta)
private val RingGradient = listOf(BrandAzure, BrandViolet, BrandMagenta, BrandCyan, BrandAzure)

private val FlameFlow = floatArrayOf(0.16f, 0.56f, 0.40f, 0.30f, 0.60f, 0.76f, 0.84f, 0.40f)
private val FlameWave = floatArrayOf(0.02f, 0.10f, 0.02f, 0.10f)
private val NoteFlow = floatArrayOf(0.84f, 0.52f, 0.60f, 0.28f, 0.40f, 0.78f, 0.16f, 0.50f)
private val NoteWave = floatArrayOf(0.02f, 0.10f, 0.02f, 0.10f)

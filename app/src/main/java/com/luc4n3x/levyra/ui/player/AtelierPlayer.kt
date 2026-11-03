package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.ui.theme.atelierCarbon
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.atelier.AtelierArmSprite
import com.luc4n3x.levyra.ui.atelier.AtelierDeckGeometry
import com.luc4n3x.levyra.ui.atelier.AtelierDeckPlinth
import com.luc4n3x.levyra.ui.atelier.AtelierQuality
import com.luc4n3x.levyra.ui.atelier.AtelierRecordFlip
import com.luc4n3x.levyra.ui.atelier.AtelierSleevePainter
import com.luc4n3x.levyra.ui.atelier.AtelierStage
import com.luc4n3x.levyra.ui.atelier.AtelierStageClock
import com.luc4n3x.levyra.ui.atelier.atelierBakeArm
import com.luc4n3x.levyra.ui.atelier.atelierBakeDeck
import com.luc4n3x.levyra.ui.atelier.atelierBakeDisc
import com.luc4n3x.levyra.ui.atelier.atelierBakeDiscLight
import com.luc4n3x.levyra.ui.atelier.atelierBakeGlow
import com.luc4n3x.levyra.ui.atelier.atelierBakeLabel
import com.luc4n3x.levyra.ui.atelier.atelierBakeSoftMask
import com.luc4n3x.levyra.ui.atelier.atelierLerp
import com.luc4n3x.levyra.ui.atelier.atelierLoadCover
import com.luc4n3x.levyra.ui.atelier.atelierQualityOf
import com.luc4n3x.levyra.ui.atelier.atelierSmooth
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.formatSeekbarMillis
import com.luc4n3x.levyra.ui.i18n.AtelierCopy
import com.luc4n3x.levyra.ui.i18n.LevyraAtelierStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.preferredPlayerArtworkUrl
import com.luc4n3x.levyra.ui.rememberSpotifyCoverTone
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val SpinDegreesPerSecond = 200f
private const val SleeveTravelRoll = -6f
private const val StrobeDots = 72
private const val MacroScale = 0.92f
private const val MacroAngle = -6f
private const val MacroCenterX = 0.5f
private const val MacroLift = 0.045f
private const val MacroSleeveFraction = 0.40f
private const val SleeveExitStart = 0.50f
private const val FlightPreSpin = 160f
private const val SleeveExitRoll = -22f
private const val ArmSettleOvershoot = 0.07f
private const val ProjectionStrength = 0.9f
private const val ProjectionDriftMillis = 18_000
private val ProjectionBlur = 22.dp
private const val MacroSleeveX = 0.5f
private const val ArmLiftDuringTurn = 0.55f
private const val TurnLiftScale = 0.035f
private const val TurnRise = 0.06f
private const val TurnSheenBoost = 0.7f
private const val TurnSheenTravel = 0.5f
private const val TurnCameraDistance = 6f
private const val CameraPixelsPerInch = 72f
private const val RecordRimFraction = 0.035f
private const val RecordRimColor = 0xFF0C0B0A.toInt()

internal class AtelierDeckAssets(
    val deck: Bitmap,
    val arm: AtelierArmSprite,
    val disc: Bitmap,
    val light: Bitmap
)

private class AtelierStageRenderer {
    val painter = AtelierSleevePainter()
    val matrix = Matrix()
    val deckUnit = FloatArray(8)
    val deckQuad = FloatArray(8)
    val sleeveFinal = FloatArray(8)
    val sleeveQuad = FloatArray(8)
    val sleeveOrigin = FloatArray(8)
    val point = FloatArray(2)
    val rect = RectF()
    val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val glowPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    val fillPaint = Paint()
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val shadowMask: Bitmap = atelierBakeSoftMask(128, 24f, 16f)
    val glow: Bitmap = atelierBakeGlow(128)
    val camera = Camera()
    val flipMatrix = Matrix()
}

private class AtelierRecordTurn {
    val angle = Animatable(0f)
    val reveal = Animatable(1f)
    var targetFace by mutableIntStateOf(0)
    var labelBase by mutableStateOf<Bitmap?>(null)
    var labelTarget by mutableStateOf<Bitmap?>(null)

    val hasLabel: Boolean get() = labelTarget != null || labelBase != null

    fun showsTarget(): Boolean = AtelierRecordFlip.faceOf(angle.value) >= targetFace

    fun beginTurn() {
        labelBase = if (showsTarget()) labelTarget else labelBase
        labelTarget = null
        targetFace = AtelierRecordFlip.faceOf(angle.value) + 1
    }

    suspend fun settle() {
        angle.snapTo(0f)
        targetFace = 0
        labelBase = labelTarget
    }
}

private fun atelierMacroSleeveCenterY(height: Float, side: Float): Float = height * 0.205f - side * 0.30f

private fun fillSleeveFinal(out: FloatArray, centerX: Float, centerY: Float, side: Float, roll: Float) {
    val radians = Math.toRadians(roll.toDouble())
    val c = cos(radians).toFloat()
    val s = sin(radians).toFloat()
    val half = side * 0.5f
    for (corner in 0 until 4) {
        val u = if (corner == 0 || corner == 3) -half else half
        val v = if (corner < 2) -half else half
        val keystone = 1f + v / side * 0.035f
        out[corner * 2] = centerX + (u * keystone * c - v * s)
        out[corner * 2 + 1] = centerY + (u * keystone * s + v * c)
    }
}

private fun fillMacroDeckQuad(out: FloatArray, centerX: Float, centerY: Float, width: Float, height: Float, angle: Float) {
    val radians = Math.toRadians(angle.toDouble())
    val c = cos(radians).toFloat()
    val s = sin(radians).toFloat()
    for (corner in 0 until 4) {
        val u = if (corner == 0 || corner == 3) -width * 0.5f else width * 0.5f
        val v = if (corner < 2) -height * 0.5f else height * 0.5f
        out[corner * 2] = centerX + u * c - v * s
        out[corner * 2 + 1] = centerY + u * s + v * c
    }
}

private fun fillRectQuad(out: FloatArray, left: Float, top: Float, right: Float, bottom: Float) {
    out[0] = left; out[1] = top
    out[2] = right; out[3] = top
    out[4] = right; out[5] = bottom
    out[6] = left; out[7] = bottom
}

@Composable
internal fun AtelierPlayerScene(
    track: Track, slots: PlayerDeckSlots, playing: Boolean, progress: Float, hasCanvas: Boolean,
    metadata: @Composable () -> Unit, transport: @Composable () -> Unit, modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val animated = LocalAnimationsEnabled.current
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraAtelierStrings.forCode(strings.code) }
    val quality = remember { atelierQualityOf(context) }
    val rich = quality == AtelierQuality.High
    val tone by rememberSpotifyCoverTone(track) { it }
    val renderer = remember { AtelierStageRenderer() }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    var sizePx by remember { mutableStateOf(0 to 0) }
    val wide = sizePx.first > sizePx.second && sizePx.second > 0
    val deckWidthPx = if (wide) min(sizePx.first * 0.56f, sizePx.second * 1.18f).toInt() else sizePx.first

    val assets by produceState(AtelierDeckAssetCache.peek(deckWidthPx, rich), deckWidthPx, rich) {
        if (deckWidthPx <= 0) return@produceState
        value = AtelierDeckAssetCache.peek(deckWidthPx, rich) ?: AtelierDeckAssetCache.obtain(deckWidthPx, rich)
    }
    val coverUrl = remember(track.id) { preferredPlayerArtworkUrl(track) }
    val coverNow = remember { mutableStateOf<Bitmap?>(null) }
    val coverOld = remember { mutableStateOf<Bitmap?>(null) }
    val swap = remember { Animatable(1f) }
    val turn = remember { AtelierRecordTurn() }
    LaunchedEffect(track.id) {
        changeAtelierRecord(context, coverUrl, animated, turn, swap, coverNow, coverOld)
    }

    val spinDegrees = remember { mutableFloatStateOf(0f) }
    val spinSpeed = remember { Animatable(0f) }
    val arm = remember { Animatable(AtelierDeckGeometry.RestAngle) }
    val grooveTarget = AtelierDeckGeometry.armAngleForRadius(AtelierDeckGeometry.grooveRadius(progress))
    LaunchedEffect(playing, animated) {
        animateAtelierSpinSpeed(spinSpeed, playing, animated)
    }
    LaunchedEffect(playing, (grooveTarget * 8f).roundToInt(), animated) {
        moveAtelierArm(arm, playing, grooveTarget, animated)
    }
    val clockProvider = AtelierStage.expansion
    val spinning = (playing || spinSpeed.value > 0.5f) && animated && lifecycle.isAtLeast(Lifecycle.State.STARTED)
    LaunchedEffect(spinning) {
        if (!spinning) return@LaunchedEffect
        runAtelierSpin(spinDegrees, spinSpeed, clockProvider)
    }
    val toneProvider = { tone }
    val coverOldProvider = { coverOld.value }
    val coverNowProvider = { coverNow.value }

    Box(modifier.fillMaxSize().onSizeChanged { sizePx = it.width to it.height }) {
        val projecting = !wide && sizePx.first > 0
        if (projecting) {
            Canvas(Modifier.fillMaxSize()) {
                drawIntoCanvas { drawAtelierRoom(it.nativeCanvas, renderer, AtelierStage.expansion().coerceIn(0f, 1f), size.width, size.height, toneProvider) }
            }
            AtelierSceneProjection(track, slots, sizePx, density, hasCanvas, coverUrl)
        }
        Canvas(Modifier.fillMaxSize()) {
            drawAtelierStage(
                renderer, assets, wide, rich, toneProvider, arm, spinDegrees, swap,
                turn, coverOldProvider, coverNowProvider, room = !projecting
            )
        }

        val controls: Modifier = Modifier.graphicsLayer {
            val fade = AtelierStageClock.controls(AtelierStage.expansion().coerceIn(0f, 1f))
            alpha = fade
            translationY = (1f - fade) * 26.dp.toPx()
        }
        val visibleControls by remember { androidx.compose.runtime.derivedStateOf { AtelierStage.expansion() > 0.6f } }

        AtelierSceneSleeveSlot(track, slots, hasCanvas, visibleControls, wide, deckWidthPx, sizePx, density)
        AtelierSceneGestureSurface(track, slots, visibleControls, wide, deckWidthPx, sizePx, density)
        AtelierSceneTopChrome(slots, copy, visibleControls, sizePx, density, controls, wide)
        AtelierScenePanel(track, slots, visibleControls, wide, deckWidthPx, sizePx, density, controls, metadata, transport)
    }
}

private suspend fun changeAtelierRecord(
    context: Context,
    coverUrl: String,
    animated: Boolean,
    turn: AtelierRecordTurn,
    swap: Animatable<Float, AnimationVector1D>,
    coverNow: MutableState<Bitmap?>,
    coverOld: MutableState<Bitmap?>
) {
    val turning = animated && turn.hasLabel
    coroutineScope {
        val artwork = async {
            val cover = atelierLoadCover(context, coverUrl, 520, hardware = false)
            cover to withContext(Dispatchers.Default) { atelierBakeLabel(cover, 360) }
        }
        val flipJob = if (turning) startAtelierTurn(turn, artwork) else null
        val (cover, label) = artwork.await()
        turn.labelTarget = label
        launch { turn.reveal.animateTo(1f, tween(if (turning) 240 else 0)) }
        coverOld.value = coverNow.value
        coverNow.value = cover
        swap.snapTo(0f)
        swap.animateTo(1f, tween(if (animated) 280 else 0))
        coverOld.value = null
        flipJob?.join()
    }
    turn.settle()
}

private suspend fun CoroutineScope.startAtelierTurn(
    turn: AtelierRecordTurn,
    artwork: Deferred<Pair<Bitmap?, Bitmap>>
): Job {
    if (AtelierRecordFlip.presence(turn.angle.value) < 0.01f) {
        withTimeoutOrNull(AtelierRecordFlip.LabelWaitMs) { artwork.await() }
    }
    turn.beginTurn()
    val early = if (artwork.isCompleted) artwork.await().second else null
    turn.labelTarget = early
    turn.reveal.snapTo(if (early != null) 1f else 0f)
    return launch {
        turn.angle.animateTo(
            turn.targetFace * AtelierRecordFlip.HalfTurnDegrees,
            tween(AtelierRecordFlip.DurationMs, easing = FastOutSlowInEasing)
        )
    }
}

internal object AtelierDeckAssetCache {
    private val lock = Any()
    private val bakes = kotlinx.coroutines.sync.Mutex()
    private var key: Pair<Int, Boolean>? = null
    private var assets: AtelierDeckAssets? = null

    fun peek(deckWidthPx: Int, rich: Boolean): AtelierDeckAssets? = synchronized(lock) { assets.takeIf { key == deckWidthPx to rich } }

    suspend fun obtain(deckWidthPx: Int, rich: Boolean): AtelierDeckAssets {
        peek(deckWidthPx, rich)?.let { return it }
        return bakes.withLock {
            peek(deckWidthPx, rich) ?: withContext(Dispatchers.Default) { bakeAtelierDeckAssets(deckWidthPx, rich) }.also { baked ->
                synchronized(lock) {
                    key = deckWidthPx to rich
                    assets = baked
                }
            }
        }
    }

    fun clear() = synchronized(lock) {
        key = null
        assets = null
    }
}

internal suspend fun prewarmAtelierDeck(context: Context, widthPx: Int) {
    if (widthPx > 0) AtelierDeckAssetCache.obtain(widthPx, atelierQualityOf(context) == AtelierQuality.High)
}

private fun bakeAtelierDeckAssets(deckWidthPx: Int, rich: Boolean): AtelierDeckAssets {
    val bakeWidth = if (rich) deckWidthPx else (deckWidthPx * 0.72f).toInt()
    return AtelierDeckAssets(
        atelierBakeDeck(bakeWidth).let { it.copy(Bitmap.Config.HARDWARE, false) ?: it },
        atelierBakeArm(bakeWidth),
        atelierBakeDisc(if (rich) 900 else 560).let { it.copy(Bitmap.Config.HARDWARE, false) ?: it },
        atelierBakeDiscLight(if (rich) 480 else 320)
    )
}

private suspend fun animateAtelierSpinSpeed(spinSpeed: Animatable<Float, AnimationVector1D>, playing: Boolean, animated: Boolean) {
    val target = if (playing && animated) SpinDegreesPerSecond else 0f
    spinSpeed.animateTo(target, tween(if (playing) 620 else 1500, easing = FastOutSlowInEasing))
}

private suspend fun moveAtelierArm(arm: Animatable<Float, AnimationVector1D>, playing: Boolean, grooveTarget: Float, animated: Boolean) {
    val target = if (playing) grooveTarget else AtelierDeckGeometry.RestAngle
    if (animated) arm.animateTo(target, spring(dampingRatio = 0.94f, stiffness = 46f)) else arm.snapTo(target)
}

private suspend fun runAtelierSpin(spinDegrees: MutableFloatState, spinSpeed: Animatable<Float, AnimationVector1D>, clockProvider: () -> Float) {
    var last = 0L
    while (true) {
        withFrameNanos { now ->
            if (last != 0L && clockProvider() > 0.2f) {
                spinDegrees.floatValue = (spinDegrees.floatValue + spinSpeed.value * (now - last) / 1_000_000_000f) % 360f
            }
            last = now
        }
    }
}

@Suppress("ComplexMethod", "CognitiveComplexMethod")
private fun DrawScope.drawAtelierStage(
    r: AtelierStageRenderer, loaded: AtelierDeckAssets?, wide: Boolean, rich: Boolean, tone: () -> Color,
    arm: Animatable<Float, AnimationVector1D>, spinDegrees: MutableFloatState, swap: Animatable<Float, AnimationVector1D>,
    turn: AtelierRecordTurn, coverOld: () -> Bitmap?, coverNow: () -> Bitmap?, room: Boolean
) {
    val clock = AtelierStage.expansion().coerceIn(0f, 1f)
    val w = size.width
    val h = size.height
    val deckW = if (wide) min(w * 0.56f, h * 1.18f) else w
    val deckH = deckW * (AtelierDeckGeometry.Aspect + AtelierDeckPlinth)
    val finalTop = if (wide) (h - deckH) * 0.5f else h * 0.205f
    val finalLeft = 0f
    val rise = AtelierStageClock.deckRise(clock)
    val insetFraction = atelierLerp(0.15f, 0.012f, rise)
    val travel = atelierLerp(h * 0.58f, 0f, rise)
    val statusGuess = 24.dp.toPx()
    val sleeveSide = if (wide) deckW * 0.30f else w * MacroSleeveFraction
    val sleeveCenterX = if (wide) sleeveSide * 0.42f else w * MacroSleeveX
    val sleeveCenterY = if (wide) (statusGuess + sleeveSide * 0.46f).coerceAtLeast(sleeveSide * 0.40f) else atelierMacroSleeveCenterY(h, sleeveSide)
    val stageDeckW = if (wide) deckW else deckW * MacroScale
    drawIntoCanvas { composeCanvas ->
        val canvas = composeCanvas.nativeCanvas
        if (room) drawAtelierRoom(canvas, r, clock, w, h, tone)
        if (loaded == null) return@drawIntoCanvas

        val deckAlpha = (AtelierStageClock.deckAlpha(clock) * 255f).toInt()
        fillRectQuad(r.deckUnit, 0f, 0f, 1000f, 0f)
        r.deckUnit[4] = 1000f; r.deckUnit[5] = 1000f * deckH / deckW
        r.deckUnit[6] = 0f; r.deckUnit[7] = 1000f * deckH / deckW
        if (wide) {
            val topInset = deckW * insetFraction
            fillRectQuad(r.deckQuad, finalLeft + topInset, finalTop + travel, finalLeft + deckW - topInset, finalTop + travel + deckH * atelierLerp(0.82f, 1f, rise))
            r.deckQuad[4] = finalLeft + deckW + deckW * insetFraction * 0.25f
            r.deckQuad[6] = finalLeft - deckW * insetFraction * 0.25f
        } else {
            fillMacroDeckQuad(r.deckQuad, w * MacroCenterX, finalTop + deckH * 0.5f - h * MacroLift + travel, stageDeckW, deckH * MacroScale, atelierLerp(0f, MacroAngle, rise))
        }
        val deckMatrixReady = r.matrix.setPolyToPoly(r.deckUnit, 0, r.deckQuad, 0, 4)

        val flight = AtelierStageClock.recordFlight(clock)
        val landed = flight >= 0.999f
        val turnAngle = turn.angle.value
        val armLift = 1f - AtelierRecordFlip.presence(turnAngle) * ArmLiftDuringTurn
        val engage = AtelierStageClock.armEngage(clock)
        val armAngle = atelierLerp(AtelierDeckGeometry.RestAngle, arm.value, (engage + sin(engage * PI.toFloat()) * ArmSettleOvershoot) * armLift)

        fillAtelierSleeveQuad(r, clock, w, h, sleeveCenterX, sleeveCenterY, sleeveSide, if (wide) SleeveTravelRoll else MacroAngle)
        drawAtelierDeck(canvas, r, loaded, deckAlpha, deckMatrixReady, landed, armAngle)
        drawAtelierRecord(canvas, r, loaded, clock, flight, stageDeckW, sleeveSide, spinDegrees, turn, turnAngle, !wide)
        if (landed && deckAlpha > 0 && deckMatrixReady) drawAtelierArm(canvas, r, loaded, deckAlpha, armAngle)
        if (wide) drawAtelierSleeve(canvas, r, clock, swap, coverOld, coverNow, rich) else drawAtelierDepartingSleeve(canvas, r, clock, w, swap, coverOld, coverNow, rich)
    }
}

private fun drawAtelierRoom(canvas: android.graphics.Canvas, r: AtelierStageRenderer, clock: Float, w: Float, h: Float, tone: () -> Color) {
    val roomAlpha = AtelierStageClock.scrim(clock)
    if (roomAlpha > 0.003f) {
        r.fillPaint.color = AtelierDesign.room.toArgb()
        r.fillPaint.alpha = (roomAlpha * 255f).toInt()
        canvas.drawRect(0f, 0f, w, h, r.fillPaint)
        r.glowPaint.color = 0xFFFFD7A8.toInt()
        r.glowPaint.alpha = (roomAlpha * 32f).toInt()
        r.rect.set(-w * 0.4f, -h * 0.25f, w * 1.1f, h * 0.45f)
        canvas.drawBitmap(r.glow, null, r.rect, r.glowPaint)
        r.glowPaint.color = tone().toArgb()
        r.glowPaint.alpha = (roomAlpha * 66f).toInt()
        r.rect.set(-w * 0.2f, h * 0.45f, w * 1.2f, h * 1.25f)
        canvas.drawBitmap(r.glow, null, r.rect, r.glowPaint)
    }
}

private fun drawAtelierArm(canvas: android.graphics.Canvas, r: AtelierStageRenderer, loaded: AtelierDeckAssets, deckAlpha: Int, armAngle: Float) {
    val checkpoint = canvas.save()
    canvas.concat(r.matrix)
    val scale = 1000f / loaded.deck.width
    canvas.scale(scale, scale)
    val pivotX = loaded.deck.width * AtelierDeckGeometry.PivotX
    val pivotY = loaded.deck.width * AtelierDeckGeometry.PivotY
    val sprite = loaded.arm
    r.fillPaint.color = 0xFF000000.toInt()
    r.fillPaint.alpha = (deckAlpha * 0.46f).toInt()
    canvas.save()
    canvas.rotate(armAngle, pivotX + loaded.deck.width * 0.012f, pivotY + loaded.deck.width * 0.02f)
    canvas.drawBitmap(sprite.shadow, pivotX + loaded.deck.width * 0.012f - sprite.pivotX, pivotY + loaded.deck.width * 0.02f - sprite.pivotY, r.fillPaint)
    canvas.restore()
    r.bitmapPaint.alpha = deckAlpha
    canvas.rotate(armAngle, pivotX, pivotY)
    canvas.drawBitmap(sprite.bitmap, pivotX - sprite.pivotX, pivotY - sprite.pivotY, r.bitmapPaint)
    canvas.restoreToCount(checkpoint)
}

private fun drawAtelierDeck(
    canvas: android.graphics.Canvas, r: AtelierStageRenderer, loaded: AtelierDeckAssets,
    deckAlpha: Int, deckMatrixReady: Boolean, landed: Boolean, armAngle: Float
) {
    if (deckAlpha > 0 && deckMatrixReady) {
        val checkpoint = canvas.save()
        canvas.concat(r.matrix)
        r.bitmapPaint.alpha = deckAlpha
        val scale = 1000f / loaded.deck.width
        canvas.scale(scale, scale)
        canvas.drawBitmap(loaded.deck, 0f, 0f, r.bitmapPaint)
        canvas.restoreToCount(checkpoint)
        if (!landed) drawAtelierArm(canvas, r, loaded, deckAlpha, armAngle)
    }
}

private fun DrawScope.fillAtelierSleeveQuad(
    r: AtelierStageRenderer, clock: Float, w: Float, h: Float, sleeveCenterX: Float, sleeveCenterY: Float, sleeveSide: Float, sleeveRoll: Float
) {
    val sleeveMove = AtelierStageClock.sleeve(clock)
    fillSleeveFinal(r.sleeveFinal, sleeveCenterX, sleeveCenterY, sleeveSide, sleeveRoll)
    val fromShelf = AtelierStage.shelfQuadValid
    val mini = AtelierStage.miniRect()
    when {
        fromShelf -> AtelierStage.shelfQuad.copyInto(r.sleeveOrigin)
        mini != null -> fillRectQuad(r.sleeveOrigin, mini.left, mini.top, mini.right, mini.bottom)
        else -> fillRectQuad(r.sleeveOrigin, w * 0.05f, h * 0.84f, w * 0.05f + 56.dp.toPx(), h * 0.84f + 56.dp.toPx())
    }
    for (i in 0 until 8) r.sleeveQuad[i] = atelierLerp(r.sleeveOrigin[i], r.sleeveFinal[i], sleeveMove)
}

@Suppress("ComplexMethod", "CognitiveComplexMethod")
private fun drawAtelierRecord(
    canvas: android.graphics.Canvas, r: AtelierStageRenderer, loaded: AtelierDeckAssets, clock: Float, flight: Float, deckW: Float, sleeveSide: Float,
    spinDegrees: MutableFloatState, turn: AtelierRecordTurn, turnAngle: Float, drop: Boolean
) {
    val recordAlpha = AtelierStageClock.recordAlpha(clock)
    if (recordAlpha > 0.003f) {
        val turnPresence = AtelierRecordFlip.presence(turnAngle)
        val facing = AtelierRecordFlip.foreshortening(turnAngle)
        val sleeveCx = (r.sleeveFinal[0] + r.sleeveFinal[4]) * 0.5f
        val sleeveCy = (r.sleeveFinal[1] + r.sleeveFinal[5]) * 0.5f
        val peek = atelierSmooth(0.28f, 0.48f, clock)
        val startX = if (drop) sleeveCx - sleeveSide * 0.08f * peek else sleeveCx + sleeveSide * 0.42f * peek
        val startY = if (drop) sleeveCy + sleeveSide * 0.40f * peek else sleeveCy + sleeveSide * 0.02f * peek
        val startRadius = sleeveSide * 0.478f
        r.point[0] = 1000f * AtelierDeckGeometry.PlatterX
        r.point[1] = 1000f * AtelierDeckGeometry.PlatterY
        r.matrix.mapPoints(r.point)
        val finalRadius = deckW * AtelierDeckGeometry.RecordRadius
        val arc = sin(flight * PI.toFloat())
        val landing = atelierSmooth(0.90f, 1f, clock)
        val bounce = if (landing in 0.001f..0.999f) sin(landing * PI.toFloat() * 2f) * (1f - landing) * 0.035f else 0f
        if (flight >= 0.999f) {
            canvas.save()
            canvas.concat(r.matrix)
            drawAtelierStrobe(canvas, r, 1000f * AtelierDeckGeometry.PlatterX, 1000f * AtelierDeckGeometry.PlatterY, 1000f * AtelierDeckGeometry.RecordRadius, spinDegrees.floatValue, (recordAlpha * 255f * atelierSmooth(0.88f, 1f, clock)).toInt())
            canvas.restore()
        }
        val x = atelierLerp(startX, r.point[0], flight) + arc * deckW * 0.09f
        val y = atelierLerp(startY, r.point[1], flight) - arc * deckW * 0.015f
        val radius = atelierLerp(startRadius, finalRadius, flight) * (1f + arc * 0.05f) * (1f + turnPresence * TurnLiftScale) * (1f - bounce)
        val turnRise = turnPresence * finalRadius * TurnRise
        val lift = arc * finalRadius * 0.20f + (1f - flight) * finalRadius * 0.05f + turnRise * 1.6f
        val alphaByte = (recordAlpha * 255f).toInt()
        r.glowPaint.color = 0xFF000000.toInt()
        r.glowPaint.alpha = (alphaByte * atelierLerp(0.6f, 0.9f, flight) * (1f - turnPresence * 0.35f)).toInt()
        val shadowHalfWidth = radius * 1.34f * atelierLerp(0.62f, 1f, facing)
        r.rect.set(x - shadowHalfWidth + lift * 0.35f, y - radius * 1.34f + lift, x + shadowHalfWidth + lift * 0.35f, y + radius * 1.34f + lift)
        canvas.drawBitmap(r.glow, null, r.rect, r.glowPaint)
        val squash = atelierLerp(0.96f, 1f, flight)
        val checkpoint = canvas.save()
        canvas.translate(x, y - turnRise)
        canvas.scale(1f, squash)
        if (turnPresence > 0.001f) {
            r.camera.save()
            r.camera.setLocation(0f, 0f, -radius * TurnCameraDistance / CameraPixelsPerInch)
            r.camera.rotateY(AtelierRecordFlip.localAngle(turnAngle))
            r.camera.save()
            r.camera.translate(0f, 0f, radius * RecordRimFraction)
            r.camera.getMatrix(r.flipMatrix)
            r.camera.restore()
            canvas.save()
            canvas.concat(r.flipMatrix)
            r.fillPaint.color = RecordRimColor
            r.fillPaint.alpha = alphaByte
            canvas.drawCircle(0f, 0f, radius * 0.995f, r.fillPaint)
            canvas.restore()
            r.camera.getMatrix(r.flipMatrix)
            r.camera.restore()
            canvas.concat(r.flipMatrix)
        }
        r.rect.set(-radius, -radius, radius, radius)
        r.bitmapPaint.alpha = alphaByte
        canvas.save()
        canvas.rotate(spinDegrees.floatValue - if (drop) (1f - flight) * FlightPreSpin else 0f)
        canvas.drawBitmap(loaded.disc, null, r.rect, r.bitmapPaint)
        val labelRadius = radius * (AtelierDeckGeometry.LabelRadius / AtelierDeckGeometry.RecordRadius)
        r.rect.set(-labelRadius, -labelRadius, labelRadius, labelRadius)
        val showsTarget = turn.showsTarget()
        val label = if (showsTarget) turn.labelTarget else turn.labelBase
        label?.let {
            val reveal = if (showsTarget) turn.reveal.value else 1f
            r.bitmapPaint.alpha = (alphaByte * reveal).toInt()
            canvas.drawBitmap(it, null, r.rect, r.bitmapPaint)
        }
        canvas.restore()
        r.rect.set(-radius, -radius, radius, radius)
        val sheen = atelierLerp(0.45f, 1f, flight) * (1f + turnPresence * TurnSheenBoost)
        r.bitmapPaint.alpha = (alphaByte * sheen).toInt().coerceAtMost(255)
        canvas.rotate(-24f + AtelierRecordFlip.localAngle(turnAngle) * TurnSheenTravel)
        canvas.drawBitmap(loaded.light, null, r.rect, r.bitmapPaint)
        canvas.restoreToCount(checkpoint)
        if (landing in 0.001f..0.999f) drawAtelierLandingRipple(canvas, r, x, y, finalRadius, landing, alphaByte)
    }
}

private fun drawAtelierStrobe(canvas: android.graphics.Canvas, r: AtelierStageRenderer, cx: Float, cy: Float, recordRadius: Float, spin: Float, alpha: Int) {
    val accent = AtelierDesign.accentBright.toArgb()
    val ring = recordRadius * 1.045f
    val dot = recordRadius * 0.0105f
    r.ringPaint.style = android.graphics.Paint.Style.FILL
    val checkpoint = canvas.save()
    canvas.translate(cx, cy)
    canvas.rotate(spin)
    for (index in 0 until StrobeDots) {
        val major = index % 6 == 0
        r.ringPaint.color = accent
        r.ringPaint.alpha = (alpha * if (major) 0.85f else 0.32f).toInt()
        val angle = index * (2f * PI.toFloat() / StrobeDots)
        canvas.drawCircle(cos(angle) * ring, sin(angle) * ring, if (major) dot * 1.4f else dot, r.ringPaint)
    }
    canvas.restoreToCount(checkpoint)
}

private fun drawAtelierLandingRipple(canvas: android.graphics.Canvas, r: AtelierStageRenderer, cx: Float, cy: Float, recordRadius: Float, landing: Float, alpha: Int) {
    val accent = AtelierDesign.accent.toArgb()
    r.ringPaint.style = android.graphics.Paint.Style.STROKE
    for (wave in 0 until 3) {
        val t = (landing - wave * 0.16f).coerceIn(0f, 1f)
        if (t <= 0f || t >= 1f) continue
        r.ringPaint.strokeWidth = recordRadius * 0.018f * (1f - t)
        r.ringPaint.color = accent
        r.ringPaint.alpha = (alpha * (1f - t) * (0.55f - wave * 0.14f)).toInt().coerceAtLeast(0)
        canvas.drawCircle(cx, cy, recordRadius * (1f + t * 0.42f), r.ringPaint)
    }
}

private fun drawAtelierDepartingSleeve(
    canvas: android.graphics.Canvas, r: AtelierStageRenderer, clock: Float, w: Float, swap: Animatable<Float, AnimationVector1D>,
    coverOld: () -> Bitmap?, coverNow: () -> Bitmap?, rich: Boolean
) {
    val exit = atelierSmooth(SleeveExitStart, 0.88f, clock)
    if (exit >= 0.999f) return
    val cx = (r.sleeveQuad[0] + r.sleeveQuad[4]) * 0.5f
    val cy = (r.sleeveQuad[1] + r.sleeveQuad[5]) * 0.5f
    val radians = Math.toRadians((SleeveExitRoll * exit).toDouble())
    val c = cos(radians).toFloat()
    val s = sin(radians).toFloat()
    val lift = exit * exit
    for (corner in 0 until 4) {
        val dx = r.sleeveQuad[corner * 2] - cx
        val dy = r.sleeveQuad[corner * 2 + 1] - cy
        r.sleeveQuad[corner * 2] = cx + dx * c - dy * s - lift * w * 0.9f
        r.sleeveQuad[corner * 2 + 1] = cy + dx * s + dy * c - lift * w * 0.28f
    }
    val mix = swap.value
    val cover = coverNow() ?: coverOld() ?: AtelierStage.shelfCover
    r.painter.draw(canvas, r.sleeveQuad, cover, null, 0f, r.shadowMask, 0.5f, exit * 0.3f, 0f, (1f - exit) * if (coverOld() != null) mix else 1f, rich)
}

private fun drawAtelierSleeve(
    canvas: android.graphics.Canvas, r: AtelierStageRenderer, clock: Float, swap: Animatable<Float, AnimationVector1D>,
    coverOld: () -> Bitmap?, coverNow: () -> Bitmap?, rich: Boolean
) {
    val roll = atelierSmooth(0.1f, 0.5f, clock)
    val shadow = 0.5f * (1f - AtelierStageClock.scrim(clock) * 0.2f)
    val mix = swap.value
    coverOld()?.takeIf { mix < 1f }?.let {
        r.painter.draw(canvas, r.sleeveQuad, it, null, 0f, r.shadowMask, shadow, roll * 0.2f, 0f, 1f - mix, rich)
    }
    r.painter.draw(canvas, r.sleeveQuad, coverNow() ?: AtelierStage.shelfCover, null, 0f, r.shadowMask, shadow, roll * 0.2f, 0f, if (coverOld() != null) mix else 1f, rich)
}

@Composable
private fun BoxScope.AtelierSceneTopChrome(
    slots: PlayerDeckSlots, copy: AtelierCopy, visibleControls: Boolean, sizePx: Pair<Int, Int>, density: Density, controls: Modifier, wide: Boolean
) {
    Box(Modifier.fillMaxWidth().statusBarsPadding().then(controls)) {
        if (visibleControls) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { slots.header() }
            }
        }
    }
    if (visibleControls && wide) {
        Text(
            copy.sideA, color = AtelierDesign.textFaint, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.TopEnd).offset(y = with(density) { (sizePx.second * 0.205f).toDp() } - 24.dp).padding(end = 22.dp).then(controls)
        )
    }
}

@Composable
private fun AtelierSceneProjection(
    track: Track, slots: PlayerDeckSlots, sizePx: Pair<Int, Int>, density: Density, hasCanvas: Boolean, coverUrl: String
) {
    val wallPx = sizePx.second * 0.205f + sizePx.first * (AtelierDeckGeometry.Aspect + AtelierDeckPlinth)
    val room = AtelierDesign.room
    Box(
        Modifier.fillMaxWidth().height(with(density) { wallPx.toDp() })
            .graphicsLayer { alpha = ProjectionStrength * AtelierStageClock.scrim(AtelierStage.expansion().coerceIn(0f, 1f)) }
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(0f to room.copy(alpha = 0.55f), 0.22f to Color.Transparent, 0.7f to Color.Transparent, 1f to room))
                drawRect(Brush.horizontalGradient(0f to room.copy(alpha = 0.45f), 0.18f to Color.Transparent, 0.82f to Color.Transparent, 1f to room.copy(alpha = 0.45f)))
            }
    ) {
        if (hasCanvas) {
            slots.stage(track, Modifier.fillMaxSize(), 0.dp)
        } else {
            val drift = if (LocalAnimationsEnabled.current) {
                androidx.compose.animation.core.rememberInfiniteTransition(label = "atelier-wall").animateFloat(
                    0f, 1f,
                    androidx.compose.animation.core.infiniteRepeatable(
                        tween<Float>(ProjectionDriftMillis, easing = androidx.compose.animation.core.LinearEasing),
                        androidx.compose.animation.core.RepeatMode.Reverse
                    ),
                    label = "atelier-wall-drift"
                )
            } else null
            coil3.compose.AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
                    .graphicsLayer {
                        val t = drift?.value ?: 0.5f
                        val zoom = 1.12f + t * 0.14f
                        scaleX = zoom
                        scaleY = zoom
                        translationX = (t - 0.5f) * size.width * 0.08f
                        translationY = (0.5f - t) * size.height * 0.04f
                    }
                    .blur(ProjectionBlur)
            )
        }
    }
}

@Composable
private fun AtelierSceneSleeveSlot(
    track: Track, slots: PlayerDeckSlots, hasCanvas: Boolean, visibleControls: Boolean, wide: Boolean,
    deckWidthPx: Int, sizePx: Pair<Int, Int>, density: Density
) {
    if (hasCanvas && visibleControls && wide) {
        val side = with(density) { (deckWidthPx * 0.30f).toDp() }
        val centerX = side * 0.42f
        val top = with(density) { sizePx.second.toFloat().times(0.205f).toDp() } - side * 0.09f - side * 0.5f
        Box(
            Modifier.offset(x = centerX - side * 0.5f, y = top.coerceAtLeast(0.dp)).size(side)
                .graphicsLayer { rotationZ = SleeveTravelRoll; alpha = AtelierStageClock.controls(AtelierStage.expansion().coerceIn(0f, 1f)) }
        ) {
            slots.stage(track, Modifier.fillMaxSize(), AtelierDesign.sleeveCorner)
        }
    }
}

@Composable
private fun AtelierSceneGestureSurface(
    track: Track, slots: PlayerDeckSlots, visibleControls: Boolean, wide: Boolean,
    deckWidthPx: Int, sizePx: Pair<Int, Int>, density: Density
) {
    if (!visibleControls || sizePx.second <= 0 || deckWidthPx <= 0) return
    val surface = with(density) {
        if (wide) {
            Modifier.width(deckWidthPx.toDp()).fillMaxHeight()
        } else {
            val deckBottomPx = sizePx.second * 0.205f + deckWidthPx * (AtelierDeckGeometry.Aspect + AtelierDeckPlinth)
            Modifier.fillMaxWidth().height(deckBottomPx.toDp())
        }
    }
    slots.gestures(track, surface)
}

@Composable
private fun BoxScope.AtelierScenePanel(
    track: Track, slots: PlayerDeckSlots, visibleControls: Boolean, wide: Boolean, deckWidthPx: Int, sizePx: Pair<Int, Int>,
    density: Density, controls: Modifier, metadata: @Composable () -> Unit, transport: @Composable () -> Unit
) {
    if (visibleControls) {
        val deckBottom = with(density) { (sizePx.second * 0.205f + deckWidthPx * (AtelierDeckGeometry.Aspect + AtelierDeckPlinth)).toDp() }
        val panel: Modifier = if (wide) {
            Modifier.align(Alignment.CenterEnd).fillMaxWidth(0.42f).fillMaxSize().padding(end = 20.dp)
        } else {
            Modifier.fillMaxSize().padding(top = deckBottom, start = 22.dp, end = 22.dp)
        }
        Column(
            panel.navigationBarsPadding().then(controls),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            metadata()
            slots.progress(PlayerProgressStyle.Classic)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { transport() }
            slots.dock(track)
        }
    }
}

private val KeyShape = RoundedCornerShape(11.dp)

@Composable
internal fun AtelierTransport(
    playing: Boolean, resolving: Boolean, shuffle: Boolean, repeat: RepeatMode,
    labels: PlayerControlLabels, onShuffle: () -> Unit, onPrevious: () -> Unit,
    onToggle: () -> Unit, onNext: () -> Unit, onRepeat: () -> Unit, modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AtelierModeButton(Icons.Rounded.Shuffle, labels.shuffle, shuffle, onShuffle)
        Row(
            Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).atelierCarbon()
                .border(1.dp, AtelierDesign.hairline, RoundedCornerShape(14.dp)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AtelierDeckKey(Modifier.weight(1f), labels.previous, false, onPrevious) { AtelierSkipGlyph(forward = false) }
            AtelierDeckKey(Modifier.weight(1.5f), if (playing) labels.pause else labels.play, playing, onToggle, enabled = !resolving, filled = true) {
                if (resolving) LevyraLoadingIndicator(Modifier.size(24.dp), AtelierDesign.onAccent) else AtelierPlayGlyph(playing, AtelierDesign.onAccent)
            }
            AtelierDeckKey(Modifier.weight(1f), labels.next, false, onNext) { AtelierSkipGlyph(forward = true) }
        }
        AtelierModeButton(if (repeat == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, labels.repeat, repeat != RepeatMode.Off, onRepeat)
    }
}

@Composable
private fun AtelierDeckKey(
    modifier: Modifier, label: String, lit: Boolean, onClick: () -> Unit, enabled: Boolean = true, filled: Boolean = false, glyph: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val travel by animateFloatAsState(if (pressed) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 900f), label = "atelier-key")
    val led by animateFloatAsState(if (lit) 1f else 0f, tween(260), label = "atelier-key-led")
    val press = with(LocalDensity.current) { 1.5.dp.toPx() }
    Box(
        modifier.height(62.dp).graphicsLayer { translationY = travel * press; scaleX = 1f - travel * 0.012f }
            .clip(KeyShape)
            .drawBehind {
                val base = AtelierDesign.accent
                val top = if (filled) androidx.compose.ui.graphics.lerp(base, Color.Black, 0.04f + travel * 0.12f) else androidx.compose.ui.graphics.lerp(Color(0xFF2C2C31), Color(0xFF1E1E22), travel)
                val bottom = if (filled) androidx.compose.ui.graphics.lerp(base, Color.Black, 0.22f + travel * 0.12f) else androidx.compose.ui.graphics.lerp(Color(0xFF141417), Color(0xFF0E0E10), travel)
                drawRect(Brush.verticalGradient(listOf(top, bottom)))
                drawRect(Color.White.copy(alpha = 0.16f - travel * 0.1f), size = Size(size.width, 1.dp.toPx()))
                drawRect(Color.Black.copy(alpha = 0.5f), Offset(0f, size.height - 2.dp.toPx()), Size(size.width, 2.dp.toPx()))
                if (led > 0.01f && !filled) {
                    val width = size.width * 0.24f
                    drawRoundRect(
                        AtelierDesign.accentBright.copy(alpha = led),
                        Offset((size.width - width) / 2f, 6.dp.toPx()), Size(width, 2.dp.toPx()), CornerRadius(1.dp.toPx())
                    )
                }
            }
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) { glyph() }
}

@Composable
private fun AtelierPlayGlyph(playing: Boolean, tint: Color = AtelierDesign.text) {
    val morph by animateFloatAsState(if (playing) 1f else 0f, spring(dampingRatio = 0.8f, stiffness = 420f), label = "atelier-play-glyph")
    Canvas(Modifier.size(26.dp)) {
        val w = size.width
        val h = size.height
        val barWidth = w * 0.26f
        val leftEnd = atelierLerp(w * 0.56f, w * 0.14f + barWidth, morph)
        val rightStart = atelierLerp(w * 0.56f, w * 0.86f - barWidth, morph)
        val left = Path().apply {
            moveTo(atelierLerp(w * 0.16f, w * 0.14f, morph), h * 0.08f)
            lineTo(leftEnd, atelierLerp(h * 0.29f, h * 0.08f, morph))
            lineTo(leftEnd, atelierLerp(h * 0.71f, h * 0.92f, morph))
            lineTo(atelierLerp(w * 0.16f, w * 0.14f, morph), h * 0.92f)
            close()
        }
        val right = Path().apply {
            moveTo(rightStart, atelierLerp(h * 0.29f, h * 0.08f, morph))
            lineTo(atelierLerp(w * 0.92f, w * 0.86f, morph), atelierLerp(h * 0.5f, h * 0.08f, morph))
            lineTo(atelierLerp(w * 0.92f, w * 0.86f, morph), atelierLerp(h * 0.5f, h * 0.92f, morph))
            lineTo(rightStart, atelierLerp(h * 0.71f, h * 0.92f, morph))
            close()
        }
        drawPath(left, tint)
        drawPath(right, tint)
    }
}

@Composable
private fun AtelierSkipGlyph(forward: Boolean) {
    Canvas(Modifier.size(22.dp).graphicsLayer { scaleX = if (forward) 1f else -1f }) {
        val w = size.width
        val h = size.height
        drawPath(
            Path().apply {
                moveTo(w * 0.1f, h * 0.16f)
                lineTo(w * 0.72f, h * 0.5f)
                lineTo(w * 0.1f, h * 0.84f)
                close()
            },
            AtelierDesign.textSecondary
        )
        drawRect(AtelierDesign.textSecondary, Offset(w * 0.78f, h * 0.16f), Size(w * 0.09f, h * 0.68f))
    }
}

@Composable
private fun AtelierModeButton(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).toggleable(value = active, role = Role.Switch, onValueChange = { onClick() }),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = if (active) AtelierDesign.accentBright else AtelierDesign.textFaint, modifier = Modifier.size(20.dp))
    }
}

@Composable
internal fun AtelierSeekbar(positionMs: Long, durationMs: Long, onSeek: (Float) -> Unit, modifier: Modifier = Modifier, playing: Boolean = true) {
    var dragging by remember(durationMs) { mutableStateOf<Float?>(null) }
    val fraction = dragging ?: if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val seekLabel = LocalLevyraStrings.current.gestureActionSeek
    val animated = com.luc4n3x.levyra.ui.LocalAnimationsEnabled.current
    val phaseState = remember { Animatable(0f) }
    LaunchedEffect(animated, playing) {
        if (!animated || !playing) {
            phaseState.snapTo(0f)
            return@LaunchedEffect
        }
        while (true) {
            phaseState.snapTo(0f)
            phaseState.animateTo(1f, tween(1600, easing = androidx.compose.animation.core.LinearEasing))
        }
    }
    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier.fillMaxWidth().height(34.dp).drawWithContent { drawContent() }.semantics {
                contentDescription = seekLabel
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                if (durationMs > 0L) setProgress { target -> onSeek(target.coerceIn(0f, 1f)); true }
            }
                .pointerInput(durationMs) {
                    if (durationMs <= 0L) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = (it.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = { dragging?.let(onSeek); dragging = null },
                        onDragCancel = { dragging = null }
                    ) { change, _ -> dragging = (change.position.x / size.width).coerceIn(0f, 1f) }
                }
                .pointerInput(durationMs) {
                    if (durationMs <= 0L) return@pointerInput
                    detectTapGestures { onSeek((it.x / size.width).coerceIn(0f, 1f)) }
                }
        ) {
            val phase = phaseState.value
            val middle = size.height / 2f
            val head = size.width * fraction
            val amplitude = (if (dragging != null) 2.dp else 3.5.dp).toPx()
            val wavelength = 22.dp.toPx()
            val stroke = 3.dp.toPx()
            val wave = Path()
            var x = 0f
            wave.moveTo(0f, middle)
            while (x < head) {
                x = (x + 2f).coerceAtMost(head)
                wave.lineTo(x, middle + kotlin.math.sin((x / wavelength + phase) * 2f * Math.PI.toFloat()) * amplitude)
            }
            drawPath(wave, AtelierDesign.accent, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            drawLine(AtelierDesign.hairlineStrong, Offset(head, middle), Offset(size.width, middle), stroke * 0.6f, androidx.compose.ui.graphics.StrokeCap.Round)
            drawRoundRect(AtelierDesign.accentBright, Offset(head - 2.dp.toPx(), middle - 9.dp.toPx()), Size(4.dp.toPx(), 18.dp.toPx()), CornerRadius(2.dp.toPx()))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val shown = dragging?.let { (durationMs * it).toLong() } ?: positionMs
            Text(formatSeekbarMillis(shown), style = MaterialTheme.typography.bodySmall, color = AtelierDesign.textFaint, maxLines = 1, overflow = TextOverflow.Clip)
            Text("-" + formatSeekbarMillis((durationMs - shown).coerceAtLeast(0L)), style = MaterialTheme.typography.bodySmall, color = AtelierDesign.textFaint, maxLines = 1, overflow = TextOverflow.Clip)
        }
    }
}

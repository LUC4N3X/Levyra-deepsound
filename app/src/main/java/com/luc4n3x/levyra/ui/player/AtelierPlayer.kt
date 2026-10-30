package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.ui.theme.atelierCarbon
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.core.animateFloat
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val SpinDegreesPerSecond = 200f
private const val SleeveTravelRoll = -6f

private class AtelierDeckAssets(
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
    val shadowMask: Bitmap = atelierBakeSoftMask(128, 24f, 16f)
    val glow: Bitmap = atelierBakeGlow(128)
}

private fun fillSleeveFinal(out: FloatArray, centerX: Float, centerY: Float, side: Float) {
    val radians = Math.toRadians(SleeveTravelRoll.toDouble())
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

    val assets by produceState<AtelierDeckAssets?>(null, deckWidthPx, rich) {
        if (deckWidthPx <= 0) return@produceState
        value = withContext(Dispatchers.Default) { bakeAtelierDeckAssets(deckWidthPx, rich) }
    }
    val coverUrl = remember(track.id) { preferredPlayerArtworkUrl(track) }
    var coverNow by remember { mutableStateOf<Bitmap?>(null) }
    var coverOld by remember { mutableStateOf<Bitmap?>(null) }
    var labelNow by remember { mutableStateOf<Bitmap?>(null) }
    var labelOld by remember { mutableStateOf<Bitmap?>(null) }
    val swap = remember { Animatable(1f) }
    LaunchedEffect(coverUrl) {
        val cover = atelierLoadCover(context, coverUrl, 520, hardware = false)
        val label = withContext(Dispatchers.Default) { atelierBakeLabel(cover, 360) }
        coverOld = coverNow
        labelOld = labelNow
        coverNow = cover
        labelNow = label
        swap.snapTo(0f)
        swap.animateTo(1f, tween(if (animated) 280 else 0))
        coverOld = null
        labelOld = null
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
    val labelOldProvider = { labelOld }
    val labelNowProvider = { labelNow }
    val coverOldProvider = { coverOld }
    val coverNowProvider = { coverNow }

    Box(modifier.fillMaxSize().onSizeChanged { sizePx = it.width to it.height }) {
        Canvas(Modifier.fillMaxSize()) {
            drawAtelierStage(
                renderer, assets, wide, rich, toneProvider, arm, spinDegrees, swap,
                labelOldProvider, labelNowProvider, coverOldProvider, coverNowProvider
            )
        }

        val controls: Modifier = Modifier.graphicsLayer {
            val fade = AtelierStageClock.controls(AtelierStage.expansion().coerceIn(0f, 1f))
            alpha = fade
            translationY = (1f - fade) * 26.dp.toPx()
        }
        val visibleControls by remember { androidx.compose.runtime.derivedStateOf { AtelierStage.expansion() > 0.6f } }

        AtelierSceneTopChrome(slots, copy, visibleControls, sizePx, density, controls)
        AtelierSceneSleeveSlot(track, slots, hasCanvas, visibleControls, wide, deckWidthPx, sizePx, density)
        AtelierScenePanel(track, slots, visibleControls, wide, deckWidthPx, sizePx, density, controls, metadata, transport)
    }
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

private fun DrawScope.drawAtelierStage(
    r: AtelierStageRenderer, loaded: AtelierDeckAssets?, wide: Boolean, rich: Boolean, tone: () -> Color,
    arm: Animatable<Float, AnimationVector1D>, spinDegrees: MutableFloatState, swap: Animatable<Float, AnimationVector1D>,
    labelOld: () -> Bitmap?, labelNow: () -> Bitmap?, coverOld: () -> Bitmap?, coverNow: () -> Bitmap?
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
    val sleeveSide = if (wide) deckW * 0.30f else w * 0.44f
    val sleeveCenterX = sleeveSide * 0.42f
    val sleeveCenterY = (if (wide) statusGuess + sleeveSide * 0.46f else finalTop - sleeveSide * 0.09f).coerceAtLeast(sleeveSide * 0.40f)
    drawIntoCanvas { composeCanvas ->
        val canvas = composeCanvas.nativeCanvas
        drawAtelierRoom(canvas, r, clock, w, h, tone)
        if (loaded == null) return@drawIntoCanvas

        val deckAlpha = (AtelierStageClock.deckAlpha(clock) * 255f).toInt()
        fillRectQuad(r.deckUnit, 0f, 0f, 1000f, 0f)
        r.deckUnit[4] = 1000f; r.deckUnit[5] = 1000f * deckH / deckW
        r.deckUnit[6] = 0f; r.deckUnit[7] = 1000f * deckH / deckW
        val topInset = deckW * insetFraction
        fillRectQuad(r.deckQuad, finalLeft + topInset, finalTop + travel, finalLeft + deckW - topInset, finalTop + travel + deckH * atelierLerp(0.82f, 1f, rise))
        r.deckQuad[4] = finalLeft + deckW + deckW * insetFraction * 0.25f
        r.deckQuad[6] = finalLeft - deckW * insetFraction * 0.25f
        val deckMatrixReady = r.matrix.setPolyToPoly(r.deckUnit, 0, r.deckQuad, 0, 4)

        val flight = AtelierStageClock.recordFlight(clock)
        val landed = flight >= 0.999f
        val armAngle = atelierLerp(AtelierDeckGeometry.RestAngle, arm.value, AtelierStageClock.armEngage(clock))

        drawAtelierDeck(canvas, r, loaded, deckAlpha, deckMatrixReady, landed, armAngle)
        fillAtelierSleeveQuad(r, clock, w, h, sleeveCenterX, sleeveCenterY, sleeveSide)
        drawAtelierRecord(canvas, r, loaded, clock, flight, deckW, sleeveSide, spinDegrees, swap, labelOld, labelNow)
        if (landed && deckAlpha > 0 && deckMatrixReady) drawAtelierArm(canvas, r, loaded, deckAlpha, armAngle)
        drawAtelierSleeve(canvas, r, clock, swap, coverOld, coverNow, rich)
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
    r: AtelierStageRenderer, clock: Float, w: Float, h: Float, sleeveCenterX: Float, sleeveCenterY: Float, sleeveSide: Float
) {
    val sleeveMove = AtelierStageClock.sleeve(clock)
    fillSleeveFinal(r.sleeveFinal, sleeveCenterX, sleeveCenterY, sleeveSide)
    val fromShelf = AtelierStage.shelfQuadValid
    val mini = AtelierStage.miniRect()
    when {
        fromShelf -> AtelierStage.shelfQuad.copyInto(r.sleeveOrigin)
        mini != null -> fillRectQuad(r.sleeveOrigin, mini.left, mini.top, mini.right, mini.bottom)
        else -> fillRectQuad(r.sleeveOrigin, w * 0.05f, h * 0.84f, w * 0.05f + 56.dp.toPx(), h * 0.84f + 56.dp.toPx())
    }
    for (i in 0 until 8) r.sleeveQuad[i] = atelierLerp(r.sleeveOrigin[i], r.sleeveFinal[i], sleeveMove)
}

private fun drawAtelierRecord(
    canvas: android.graphics.Canvas, r: AtelierStageRenderer, loaded: AtelierDeckAssets, clock: Float, flight: Float, deckW: Float, sleeveSide: Float,
    spinDegrees: MutableFloatState, swap: Animatable<Float, AnimationVector1D>, labelOld: () -> Bitmap?, labelNow: () -> Bitmap?
) {
    val recordAlpha = AtelierStageClock.recordAlpha(clock)
    if (recordAlpha > 0.003f) {
        val sleeveCx = (r.sleeveFinal[0] + r.sleeveFinal[4]) * 0.5f
        val sleeveCy = (r.sleeveFinal[1] + r.sleeveFinal[5]) * 0.5f
        val peek = atelierSmooth(0.28f, 0.48f, clock)
        val startX = sleeveCx + sleeveSide * 0.42f * peek
        val startY = sleeveCy + sleeveSide * 0.02f * peek
        val startRadius = sleeveSide * 0.478f
        r.point[0] = 1000f * AtelierDeckGeometry.PlatterX
        r.point[1] = 1000f * AtelierDeckGeometry.PlatterY
        r.matrix.mapPoints(r.point)
        val finalRadius = deckW * AtelierDeckGeometry.RecordRadius
        val arc = sin(flight * PI.toFloat())
        val x = atelierLerp(startX, r.point[0], flight) + arc * deckW * 0.09f
        val y = atelierLerp(startY, r.point[1], flight) - arc * deckW * 0.015f
        val radius = atelierLerp(startRadius, finalRadius, flight) * (1f + arc * 0.05f)
        val lift = arc * finalRadius * 0.20f + (1f - flight) * finalRadius * 0.05f
        val alphaByte = (recordAlpha * 255f).toInt()
        r.glowPaint.color = 0xFF000000.toInt()
        r.glowPaint.alpha = (alphaByte * atelierLerp(0.6f, 0.9f, flight)).toInt()
        r.rect.set(x - radius * 1.34f + lift * 0.35f, y - radius * 1.34f + lift, x + radius * 1.34f + lift * 0.35f, y + radius * 1.34f + lift)
        canvas.drawBitmap(r.glow, null, r.rect, r.glowPaint)
        val squash = atelierLerp(0.96f, 1f, flight)
        val checkpoint = canvas.save()
        canvas.translate(x, y)
        canvas.scale(1f, squash)
        r.rect.set(-radius, -radius, radius, radius)
        r.bitmapPaint.alpha = alphaByte
        canvas.save()
        canvas.rotate(spinDegrees.floatValue)
        canvas.drawBitmap(loaded.disc, null, r.rect, r.bitmapPaint)
        val labelRadius = radius * (AtelierDeckGeometry.LabelRadius / AtelierDeckGeometry.RecordRadius)
        r.rect.set(-labelRadius, -labelRadius, labelRadius, labelRadius)
        val mix = swap.value
        labelOld()?.let {
            r.bitmapPaint.alpha = (alphaByte * (1f - mix)).toInt()
            canvas.drawBitmap(it, null, r.rect, r.bitmapPaint)
        }
        labelNow()?.let {
            r.bitmapPaint.alpha = (alphaByte * mix).toInt()
            canvas.drawBitmap(it, null, r.rect, r.bitmapPaint)
        }
        canvas.restore()
        r.rect.set(-radius, -radius, radius, radius)
        r.bitmapPaint.alpha = (alphaByte * atelierLerp(0.45f, 1f, flight)).toInt()
        canvas.rotate(-24f)
        canvas.drawBitmap(loaded.light, null, r.rect, r.bitmapPaint)
        canvas.restoreToCount(checkpoint)
    }
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
    slots: PlayerDeckSlots, copy: AtelierCopy, visibleControls: Boolean, sizePx: Pair<Int, Int>, density: Density, controls: Modifier
) {
    Box(Modifier.fillMaxWidth().statusBarsPadding().then(controls)) {
        if (visibleControls) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { slots.header() }
            }
        }
    }
    if (visibleControls) {
        Text(
            copy.sideA, color = AtelierDesign.textFaint, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.TopEnd).offset(y = with(density) { (sizePx.second * 0.205f).toDp() } - 24.dp).padding(end = 22.dp).then(controls)
        )
    }
}

@Composable
private fun AtelierSceneSleeveSlot(
    track: Track, slots: PlayerDeckSlots, hasCanvas: Boolean, visibleControls: Boolean, wide: Boolean,
    deckWidthPx: Int, sizePx: Pair<Int, Int>, density: Density
) {
    if (hasCanvas && visibleControls) {
        val side = with(density) { (if (wide) deckWidthPx * 0.30f else sizePx.first * 0.44f).toDp() }
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
    val wave = androidx.compose.animation.core.rememberInfiniteTransition(label = "atelier-wave")
    val phaseState = wave.animateFloat(0f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(1600, easing = androidx.compose.animation.core.LinearEasing)), label = "atelier-wave-phase")
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
            val phase = if (animated && playing) phaseState.value else 0f
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

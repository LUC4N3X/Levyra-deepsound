package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.PlayerDarkSurface
import com.luc4n3x.levyra.ui.StableRemoteArtwork
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayPauseGlyph
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.components.formatSeekbarMillis
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.playerMix
import com.luc4n3x.levyra.ui.preferredPlayerArtworkUrl
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

internal object TurntableGeometry {
    const val CenterX = 0.47f
    const val CenterY = 0.52f
    const val PlatterRadius = 0.395f
    const val RingRadius = 0.425f
    const val DiscRadius = 0.36f
    const val LabelRadius = 0.145f
    const val OuterGroove = 0.345f
    const val InnerGroove = 0.168f
    const val PivotX = 0.9f
    const val PivotY = 0.12f
    const val TubeLength = 0.5f
    const val HeadLength = 0.1f
    const val HeadOffsetDegrees = 22f
    const val RestAngle = -4f
    const val MaxAngle = 38f
    const val SeekBandInner = 0.365f
    const val SeekBandOuter = 0.5f
    const val SeamStart = 0.97f

    fun stylus(angleDegrees: Float): Offset {
        val tube = Math.toRadians(angleDegrees.toDouble())
        val head = Math.toRadians((angleDegrees + HeadOffsetDegrees).toDouble())
        return Offset(
            PivotX - TubeLength * sin(tube).toFloat() - HeadLength * sin(head).toFloat(),
            PivotY + TubeLength * cos(tube).toFloat() + HeadLength * cos(head).toFloat()
        )
    }

    fun stylusRadius(angleDegrees: Float): Float {
        val point = stylus(angleDegrees)
        return hypot(point.x - CenterX, point.y - CenterY)
    }

    fun grooveRadius(progress: Float): Float =
        OuterGroove + (InnerGroove - OuterGroove) * progress.coerceIn(0f, 1f)

    fun armAngleForProgress(progress: Float): Float {
        val radius = grooveRadius(progress)
        var low = RestAngle
        var high = MaxAngle
        repeat(22) {
            val middle = (low + high) * 0.5f
            if (stylusRadius(middle) > radius) low = middle else high = middle
        }
        return (low + high) * 0.5f
    }

    fun fractionAt(x: Float, y: Float, side: Float): Float {
        val dx = x - CenterX * side
        val dy = y - CenterY * side
        val angle = atan2(dx, -dy)
        val turn = if (angle < 0f) angle + 2f * PI.toFloat() else angle
        return (turn / (2f * PI.toFloat())).coerceIn(0f, 1f)
    }

    fun inSeekBand(x: Float, y: Float, side: Float): Boolean {
        val distance = hypot(x - CenterX * side, y - CenterY * side) / side
        return distance in SeekBandInner..SeekBandOuter
    }

    fun startingFraction(fraction: Float): Float = if (fraction > SeamStart) fraction - 1f else fraction

    fun shortestDelta(previous: Float, next: Float): Float {
        val delta = next - previous
        return when {
            delta > 0.5f -> delta - 1f
            delta < -0.5f -> delta + 1f
            else -> delta
        }
    }
}

private const val SpinDegreesPerSecond = 200f
private const val SpinUpMillis = 900
private const val SpinDownMillis = 1500
private const val SwapOutMillis = 300
private const val RingDots = 72
private const val ArmTargetSteps = 400f
private const val TiltDegrees = 14f
private const val TiltCameraDistance = 18f

private val TurntableReadoutShape = RoundedCornerShape(14.dp)
private val TurntableScrollingStage = 340.dp
private val TurntableLcdStyle = TextStyle(fontFamily = FontFamily.Monospace, fontFeatureSettings = "tnum")
private val TurntableTimeStyle = TextStyle(fontFeatureSettings = "tnum")
private val PlatterCore = Color(0xFF26262B)
private val PlatterEdge = Color(0xFF0F0F12)
private val VinylBase = Color(0xFF050506)
private val PlinthBase = Color(0xFF16161B)
private val MetalLight = Color(0xFFE6E6EC)
private val MetalShade = Color(0xFF8B8B95)
private val HeadshellInk = Color(0xFF2A2A30)

@Composable
internal fun PlayerTurntableDeck(
    track: Track,
    slots: PlayerDeckSlots,
    surfaces: PlayerSurfaceTokens,
    accent: Color,
    isFavorite: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    positionMs: Long,
    durationMs: Long,
    labels: PlayerControlLabels,
    animated: Boolean,
    compact: Boolean,
    scrollable: Boolean,
    gutter: Dp,
    stepDirection: Int,
    onArtistClick: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    headlineModifier: Modifier = Modifier
) {
    val tone by animateColorAsState(
        targetValue = accent,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-turntable-tone"
    )
    val scrub = remember(track.id) { mutableFloatStateOf(-1f) }
    val playedFraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val gap = if (compact) LevyraPlayerDesign.SpaceSm else LevyraPlayerDesign.SpaceMd
    Column(
        modifier = modifier
            .turntableBackdrop(tone, surfaces.amoled)
            .statusBarsPadding()
            .navigationBarsPadding()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(start = gutter, end = gutter, top = LevyraPlayerDesign.SpaceXs, bottom = gap),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        slots.header()
        TurntableStage(
            track = track,
            slots = slots,
            tone = tone,
            surfaces = surfaces,
            isPlaying = isPlaying,
            playedFraction = playedFraction,
            seekable = durationMs > 0L,
            scrub = scrub,
            animated = animated,
            stepDirection = stepDirection,
            onSeek = onSeek,
            modifier = turntableStageModifier(scrollable)
        )
        TurntableReadout(
            positionMs = positionMs,
            durationMs = durationMs,
            scrubFraction = scrub.floatValue,
            tone = tone,
            surfaces = surfaces,
            compact = compact
        )
        Spacer(Modifier.height(gap))
        TurntableHeadline(
            track = track,
            surfaces = surfaces,
            isFavorite = isFavorite,
            animated = animated,
            compact = compact,
            stepDirection = stepDirection,
            onArtistClick = onArtistClick,
            onToggleFavorite = onToggleFavorite,
            modifier = headlineModifier.fillMaxWidth()
        )
        slots.engagement(track)
        Spacer(Modifier.height(gap))
        TurntableTransport(
            isPlaying = isPlaying,
            isResolving = isResolving,
            shuffleOn = shuffleOn,
            repeatMode = repeatMode,
            labels = labels,
            tone = tone,
            surfaces = surfaces,
            compact = compact,
            animated = animated,
            onShuffle = onShuffle,
            onPrevious = onPrevious,
            onTogglePlay = onTogglePlay,
            onNext = onNext,
            onRepeat = onRepeat
        )
        Spacer(Modifier.height(gap))
        TurntableActions(track = track, slots = slots, tone = tone, surfaces = surfaces)
        slots.status()
    }
}

private fun Modifier.turntableBackdrop(tone: Color, amoled: Boolean): Modifier = drawBehind {
    if (amoled) {
        drawRect(Color.Black)
        return@drawBehind
    }
    drawRect(
        Brush.verticalGradient(
            listOf(tone.playerMix(PlayerDarkSurface, 0.7f), tone.playerMix(PlayerDarkSurface, 0.9f), PlayerDarkSurface)
        )
    )
    val glowCenter = Offset(size.width * TurntableGeometry.CenterX, size.width * 0.62f)
    drawCircle(
        brush = Brush.radialGradient(listOf(tone.copy(alpha = 0.3f), Color.Transparent), glowCenter, size.width * 0.78f),
        radius = size.width * 0.78f,
        center = glowCenter
    )
}

private fun ColumnScope.turntableStageModifier(scrollable: Boolean): Modifier = Modifier
    .fillMaxWidth()
    .then(if (scrollable) Modifier.height(TurntableScrollingStage) else Modifier.weight(1f))

@Composable
private fun TurntableStage(
    track: Track,
    slots: PlayerDeckSlots,
    tone: Color,
    surfaces: PlayerSurfaceTokens,
    isPlaying: Boolean,
    playedFraction: Float,
    seekable: Boolean,
    scrub: MutableFloatState,
    animated: Boolean,
    stepDirection: Int,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val swap = rememberRecordSwap(track, stepDirection, animated)
    val swapping = swap.label.value.id != track.id || swap.offset.isRunning
    val spin = rememberTurntableSpin(isPlaying, animated)
    val arm = rememberTurntableArm(isPlaying && seekable && !swapping, playedFraction, animated)
    val power by animateFloatAsState(if (isPlaying) 1f else 0f, tween(if (animated) 600 else 0), label = "turntable-power")
    val vinylInk = tone.playerMix(VinylBase, 0.84f)
    val scrubbing by remember(scrub) { derivedStateOf { scrub.floatValue >= 0f } }
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        val density = LocalDensity.current
        val sidePx = with(density) { side.toPx() }
        Box(
            modifier = Modifier
                .size(side)
                .graphicsLayer {
                    rotationX = TiltDegrees
                    cameraDistance = TiltCameraDistance * density.density
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .turntableSeekRing(seekable, sidePx, scrub, onSeek)
        ) {
            Canvas(modifier = Modifier.fillMaxSize().graphicsLayer()) { drawTurntablePlinth(tone) }
            TurntableRing(
                fraction = { scrub.floatValue.takeIf { it >= 0f } ?: playedFraction },
                tone = tone,
                idle = surfaces.content,
                seekable = seekable,
                onSeek = onSeek
            )
            TurntableSpeedKeys(side, sidePx, tone, power)
            TurntableVinyl(swap.label.value, side, sidePx, spin, { swap.offset.value }, vinylInk)
            if (!scrubbing) {
                slots.gestures(
                    track,
                    Modifier
                        .absoluteOffset { centeredOffset(sidePx, TurntableGeometry.DiscRadius) }
                        .size(side * (TurntableGeometry.DiscRadius * 2f))
                        .clip(CircleShape)
                )
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawTurntableFittings(tone, power)
                drawTurntableArm(arm.angle.value, arm.lift.value, tone, power)
            }
        }
    }
}

private const val PreviewProgress = 0.36f

@Composable
internal fun TurntablePreviewStage(track: Track, accent: Color, modifier: Modifier = Modifier) {
    val vinylInk = accent.playerMix(VinylBase, 0.84f)
    val armAngle = remember { TurntableGeometry.armAngleForProgress(PreviewProgress) }
    val spin = remember { mutableFloatStateOf(18f) }
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        val density = LocalDensity.current
        val sidePx = with(density) { side.toPx() }
        Box(
            modifier = Modifier
                .size(side)
                .graphicsLayer {
                    rotationX = TiltDegrees
                    cameraDistance = TiltCameraDistance * density.density
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .clearAndSetSemantics { }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawTurntablePlinth(accent)
                drawTurntableRing(PreviewProgress, accent.playerMix(Color.White, 0.18f), Color.White)
            }
            TurntableVinyl(track, side, sidePx, spin, { 0f }, vinylInk)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawTurntableFittings(accent, 1f)
                drawTurntableArm(armAngle, 0f, accent, 1f)
            }
        }
    }
}

private fun centeredOffset(sidePx: Float, radius: Float): IntOffset = IntOffset(
    ((TurntableGeometry.CenterX - radius) * sidePx).roundToInt(),
    ((TurntableGeometry.CenterY - radius) * sidePx).roundToInt()
)

private class RecordSwap(val offset: Animatable<Float, AnimationVector1D>, val label: MutableState<Track>)

@Composable
private fun rememberRecordSwap(track: Track, stepDirection: Int, animated: Boolean): RecordSwap {
    val swap = remember { RecordSwap(Animatable(0f), mutableStateOf(track)) }
    LaunchedEffect(track.id) {
        if (swap.label.value.id == track.id) {
            if (animated) swap.offset.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 260f)) else swap.offset.snapTo(0f)
            return@LaunchedEffect
        }
        if (!animated) {
            swap.label.value = track
            swap.offset.snapTo(0f)
            return@LaunchedEffect
        }
        val exit = if (stepDirection < 0) 1f else -1f
        swap.offset.animateTo(exit, tween(SwapOutMillis, easing = FastOutLinearInEasing))
        swap.label.value = track
        swap.offset.snapTo(-exit)
        swap.offset.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 260f))
    }
    return swap
}

private class TurntableArm(
    val angle: Animatable<Float, AnimationVector1D>,
    val lift: Animatable<Float, AnimationVector1D>
)

@Composable
private fun rememberTurntableArm(engaged: Boolean, fraction: Float, animated: Boolean): TurntableArm {
    val arm = remember {
        TurntableArm(Animatable(TurntableGeometry.RestAngle), Animatable(1f))
    }
    val step = (fraction * ArmTargetSteps).roundToInt()
    LaunchedEffect(engaged, step, animated) {
        val target = if (engaged) TurntableGeometry.armAngleForProgress(step / ArmTargetSteps) else TurntableGeometry.RestAngle
        if (animated) arm.angle.animateTo(target, spring(dampingRatio = 0.9f, stiffness = 55f)) else arm.angle.snapTo(target)
    }
    LaunchedEffect(engaged, animated) {
        val target = if (engaged) 0f else 1f
        if (animated) arm.lift.animateTo(target, tween(420, easing = FastOutSlowInEasing)) else arm.lift.snapTo(target)
    }
    return arm
}

@Composable
private fun rememberTurntableSpin(isPlaying: Boolean, animated: Boolean): FloatState {
    val spin = remember { mutableFloatStateOf(0f) }
    val speed = remember { Animatable(0f) }
    LaunchedEffect(isPlaying, animated) {
        if (!animated) {
            speed.snapTo(0f)
            return@LaunchedEffect
        }
        val target = if (isPlaying) SpinDegreesPerSecond else 0f
        val duration = if (isPlaying) SpinUpMillis else SpinDownMillis
        launch { speed.animateTo(target, tween(duration, easing = FastOutSlowInEasing)) }
        var last = 0L
        while (isPlaying || speed.value > 0.5f) {
            withFrameNanos { now ->
                if (last != 0L) spin.floatValue = (spin.floatValue + speed.value * (now - last) / 1_000_000_000f) % 360f
                last = now
            }
        }
    }
    return spin
}

private fun Modifier.turntableSeekRing(
    seekable: Boolean,
    sidePx: Float,
    scrub: MutableFloatState,
    onSeek: (Float) -> Unit
): Modifier = pointerInput(seekable, sidePx, scrub) {
    if (!seekable || sidePx <= 0f) return@pointerInput
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (!TurntableGeometry.inSeekBand(down.position.x, down.position.y, sidePx)) return@awaitEachGesture
        down.consume()
        var angle = TurntableGeometry.fractionAt(down.position.x, down.position.y, sidePx)
        var unwrapped = TurntableGeometry.startingFraction(angle)
        scrub.floatValue = unwrapped.coerceIn(0f, 1f)
        try {
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                val next = TurntableGeometry.fractionAt(change.position.x, change.position.y, sidePx)
                unwrapped += TurntableGeometry.shortestDelta(angle, next)
                angle = next
                scrub.floatValue = unwrapped.coerceIn(0f, 1f)
                change.consume()
            }
            onSeek(unwrapped.coerceIn(0f, 1f))
        } finally {
            scrub.floatValue = -1f
        }
    }
}

@Composable
private fun TurntableRing(
    fraction: () -> Float,
    tone: Color,
    idle: Color,
    seekable: Boolean,
    onSeek: (Float) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val lit = tone.playerMix(Color.White, 0.18f)
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = strings.gestureActionSeek
                progressBarRangeInfo = ProgressBarRangeInfo(fraction(), 0f..1f)
                if (seekable) {
                    setProgress { target ->
                        onSeek(target.coerceIn(0f, 1f))
                        true
                    }
                }
            }
    ) {
        drawTurntableRing(fraction(), lit, idle)
    }
}

private fun DrawScope.drawTurntableRing(fraction: Float, lit: Color, idle: Color) {
    val side = size.minDimension
    val center = Offset(side * TurntableGeometry.CenterX, side * TurntableGeometry.CenterY)
    val radius = side * TurntableGeometry.RingRadius
    val dot = side * 0.0072f
    val litCount = (fraction * RingDots).roundToInt()
    for (index in 0 until RingDots) {
        val angle = 2.0 * PI * index / RingDots
        val point = Offset(center.x + radius * sin(angle).toFloat(), center.y - radius * cos(angle).toFloat())
        if (index < litCount) {
            drawCircle(lit.copy(alpha = 0.18f), dot * 1.9f, point)
            drawCircle(lit, dot, point)
        } else {
            drawCircle(idle.copy(alpha = 0.14f), dot * 0.8f, point)
        }
    }
    val headAngle = 2.0 * PI * fraction
    val head = Offset(center.x + radius * sin(headAngle).toFloat(), center.y - radius * cos(headAngle).toFloat())
    drawCircle(lit.copy(alpha = 0.28f), dot * 2.8f, head)
    drawCircle(Color.White, dot * 1.7f, head)
}

private fun DrawScope.drawTurntablePlinth(tone: Color) {
    val side = size.minDimension
    val topLeft = Offset(side * 0.02f, side * 0.03f)
    val body = Size(side * 0.96f, side * 0.96f)
    val corner = CornerRadius(side * 0.07f)
    for (layer in 1..4) {
        val spread = side * 0.012f * layer
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.16f),
            topLeft = topLeft + Offset(-spread * 0.4f, side * 0.02f + spread * 0.6f),
            size = Size(body.width + spread * 0.8f, body.height),
            cornerRadius = CornerRadius(corner.x + spread)
        )
    }
    val anodized = tone.playerMix(PlinthBase, 0.74f)
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(anodized.playerMix(Color.White, 0.08f), anodized, anodized.playerMix(Color.Black, 0.45f)),
            topLeft.y,
            topLeft.y + body.height
        ),
        topLeft = topLeft,
        size = body,
        cornerRadius = corner
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.02f)),
            topLeft.y,
            topLeft.y + body.height
        ),
        topLeft = topLeft,
        size = body,
        cornerRadius = corner,
        style = Stroke(side * 0.004f)
    )
    val inset = side * 0.035f
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.16f),
        topLeft = topLeft + Offset(inset, inset),
        size = Size(body.width - inset * 2f, body.height - inset * 2f),
        cornerRadius = CornerRadius(corner.x - inset * 0.6f),
        style = Stroke(side * 0.003f)
    )
    drawTurntablePlatter(side)
}

private fun DrawScope.drawTurntablePlatter(side: Float) {
    val center = Offset(side * TurntableGeometry.CenterX, side * TurntableGeometry.CenterY)
    val platter = side * TurntableGeometry.PlatterRadius
    drawCircle(Color.Black.copy(alpha = 0.5f), platter * 1.03f, center + Offset(0f, side * 0.014f))
    drawCircle(Brush.radialGradient(listOf(PlatterCore, PlatterEdge), center, platter), platter, center)
    drawCircle(Color.White.copy(alpha = 0.08f), platter, center, style = Stroke(side * 0.004f))
    val mat = side * TurntableGeometry.DiscRadius * 0.985f
    var radius = side * TurntableGeometry.LabelRadius
    while (radius < mat) {
        drawCircle(Color.White.copy(alpha = 0.025f), radius, center, style = Stroke(side * 0.006f))
        radius += side * 0.02f
    }
}

@Composable
private fun TurntableVinyl(
    label: Track,
    side: Dp,
    sidePx: Float,
    spin: FloatState,
    offset: () -> Float,
    vinylInk: Color
) {
    Box(
        modifier = Modifier
            .absoluteOffset { centeredOffset(sidePx, TurntableGeometry.DiscRadius) }
            .size(side * (TurntableGeometry.DiscRadius * 2f))
            .graphicsLayer {
                val shift = offset()
                translationX = shift * sidePx * 1.12f
                translationY = -abs(shift) * sidePx * 0.04f
                val scale = 1f + abs(shift) * 0.05f
                scaleX = scale
                scaleY = scale
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer()) { drawTurntableVinyl(vinylInk) }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(side * (TurntableGeometry.LabelRadius * 2f))
                .graphicsLayer { rotationZ = spin.floatValue }
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(label.accentStart), Color(label.accentEnd))))
        ) {
            StableRemoteArtwork(
                url = preferredPlayerArtworkUrl(label).ifBlank { label.thumbnailUrl },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(Color.Black.copy(alpha = 0.2f), size.minDimension / 2f, style = Stroke(size.minDimension * 0.06f))
                drawCircle(Color.White.copy(alpha = 0.24f), size.minDimension * 0.46f, style = Stroke(size.minDimension * 0.008f))
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) { drawTurntableSheen() }
    }
}

private fun DrawScope.drawTurntableVinyl(ink: Color) {
    val radius = size.minDimension / 2f
    val label = radius * (TurntableGeometry.LabelRadius / TurntableGeometry.DiscRadius)
    drawCircle(Color.Black.copy(alpha = 0.55f), radius * 1.01f, center + Offset(0f, radius * 0.03f))
    drawCircle(
        Brush.radialGradient(listOf(ink.playerMix(Color.White, 0.06f), ink, ink.playerMix(Color.Black, 0.35f)), center, radius),
        radius,
        center
    )
    val step = radius * 0.0118f
    var groove = label * 1.12f
    var index = 0
    while (groove < radius * 0.975f) {
        val alpha = when {
            index % 23 == 0 -> 0f
            index % 2 == 0 -> 0.065f
            else -> 0.03f
        }
        if (alpha > 0f) drawCircle(Color.White.copy(alpha = alpha), groove, center, style = Stroke(radius * 0.0046f))
        groove += step
        index++
    }
    drawCircle(Color.White.copy(alpha = 0.12f), radius, center, style = Stroke(radius * 0.008f))
}

private fun DrawScope.drawTurntableSheen() {
    val radius = size.minDimension / 2f
    val label = radius * (TurntableGeometry.LabelRadius / TurntableGeometry.DiscRadius)
    val sheen = Brush.sweepGradient(
        0.00f to Color.Transparent,
        0.08f to Color.White.copy(alpha = 0.14f),
        0.16f to Color.Transparent,
        0.50f to Color.Transparent,
        0.58f to Color.White.copy(alpha = 0.1f),
        0.66f to Color.Transparent,
        1.00f to Color.Transparent,
        center = center
    )
    rotate(-30f) {
        drawCircle(sheen, (radius + label) / 2f, center, style = Stroke(radius - label))
    }
}

@Composable
private fun TurntableSpeedKeys(side: Dp, sidePx: Float, tone: Color, power: Float) {
    val lit = tone.playerMix(Color.White, 0.3f)
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .absoluteOffset { IntOffset((sidePx * 0.075f).roundToInt(), (sidePx * 0.895f).roundToInt()) }
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(side * 0.015f)
    ) {
        listOf("33", "45").forEachIndexed { index, speed ->
            val on = if (index == 0) power else 0f
            Box(
                modifier = Modifier
                    .size(width = side * 0.075f, height = side * 0.05f)
                    .clip(shape)
                    .background(Color.Black.copy(alpha = 0.32f))
                    .border(1.dp, lit.copy(alpha = 0.12f + 0.5f * on), shape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = speed,
                    color = Color.White.copy(alpha = 0.42f).playerMix(lit, on),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    style = TurntableLcdStyle
                )
            }
        }
    }
}

private fun DrawScope.drawTurntableFittings(tone: Color, power: Float) {
    val side = size.minDimension
    val center = Offset(side * TurntableGeometry.CenterX, side * TurntableGeometry.CenterY)
    val lit = tone.playerMix(Color.White, 0.3f)
    drawCircle(MetalShade, side * 0.016f, center)
    drawCircle(MetalLight, side * 0.009f, center)
    val knob = Offset(side * 0.095f, side * 0.1f)
    drawCircle(Color.Black.copy(alpha = 0.45f), side * 0.042f, knob + Offset(side * 0.004f, side * 0.008f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFF4A4A52), Color(0xFF17171C)), knob, side * 0.04f), side * 0.04f, knob)
    drawCircle(lit.copy(alpha = 0.2f + 0.8f * power), side * 0.047f, knob, style = Stroke(side * 0.004f))
    if (power > 0f) drawCircle(lit.copy(alpha = 0.18f * power), side * 0.07f, knob)
    val lampAngle = Math.toRadians(205.0)
    val lamp = Offset(
        center.x + side * 0.468f * sin(lampAngle).toFloat(),
        center.y - side * 0.468f * cos(lampAngle).toFloat()
    )
    clipPath(turntablePlinthPath(side)) {
        drawCircle(Brush.radialGradient(listOf(lit.copy(alpha = 0.34f * power), Color.Transparent), lamp, side * 0.16f), side * 0.16f, lamp)
    }
    drawCircle(Color(0xFF1C1C21), side * 0.017f, lamp)
    drawCircle(lit.copy(alpha = 0.35f + 0.65f * power), side * 0.009f, lamp)
    val rest = TurntableGeometry.stylus(TurntableGeometry.RestAngle) * side
    drawRoundRect(
        color = Color(0xFF15151A),
        topLeft = rest + Offset(-side * 0.026f, -side * 0.012f),
        size = Size(side * 0.052f, side * 0.03f),
        cornerRadius = CornerRadius(side * 0.012f)
    )
}

private fun turntablePlinthPath(side: Float): Path = Path().apply {
    addRoundRect(RoundRect(side * 0.02f, side * 0.03f, side * 0.98f, side * 0.99f, CornerRadius(side * 0.07f)))
}

private fun DrawScope.drawTurntableArm(angle: Float, lift: Float, tone: Color, power: Float) {
    val side = size.minDimension
    val pivot = Offset(side * TurntableGeometry.PivotX, side * TurntableGeometry.PivotY)
    val tubeRadians = Math.toRadians(angle.toDouble())
    val tubeDirection = Offset(-sin(tubeRadians).toFloat(), cos(tubeRadians).toFloat())
    val elbow = pivot + tubeDirection * (side * TurntableGeometry.TubeLength)
    val stylus = TurntableGeometry.stylus(angle) * side
    val counter = pivot - tubeDirection * (side * 0.085f)
    val shadow = Offset(side * (0.012f + 0.024f * lift), side * (0.018f + 0.03f * lift))
    val tube = side * 0.016f
    val head = side * 0.038f
    drawCircle(Color.Black.copy(alpha = 0.4f), side * 0.072f, pivot + Offset(side * 0.008f, side * 0.014f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFF3A3A42), Color(0xFF15151A)), pivot, side * 0.07f), side * 0.068f, pivot)
    drawCircle(Color.White.copy(alpha = 0.1f), side * 0.068f, pivot, style = Stroke(side * 0.003f))
    val shadowAlpha = 0.34f - 0.12f * lift
    drawLine(Color.Black.copy(alpha = shadowAlpha), pivot + shadow, elbow + shadow, tube, StrokeCap.Round)
    drawLine(Color.Black.copy(alpha = shadowAlpha), elbow + shadow, stylus + shadow, head, StrokeCap.Round)
    drawLine(MetalShade, counter, pivot, side * 0.05f, StrokeCap.Round)
    drawLine(MetalLight, counter, pivot, side * 0.034f, StrokeCap.Round)
    drawLine(MetalShade, pivot, elbow, tube * 1.35f, StrokeCap.Round)
    drawLine(MetalLight, pivot, elbow, tube, StrokeCap.Round)
    drawLine(HeadshellInk, elbow, stylus, head, StrokeCap.Round)
    drawLine(tone.copy(alpha = 0.9f), elbow, elbow + (stylus - elbow) * 0.42f, side * 0.006f, StrokeCap.Round)
    drawCircle(MetalShade, side * 0.03f, pivot)
    drawCircle(MetalLight, side * 0.018f, pivot)
    val needle = power * (1f - lift)
    if (needle > 0f) drawCircle(tone.copy(alpha = 0.55f * needle), side * 0.012f, stylus)
}

@Composable
private fun TurntableReadout(
    positionMs: Long,
    durationMs: Long,
    scrubFraction: Float,
    tone: Color,
    surfaces: PlayerSurfaceTokens,
    compact: Boolean
) {
    val spec by rememberPlayerAudioSpec()
    val details = remember(spec) { playerAudioSpecLabels(spec).joinToString("  ·  ") }
    val elapsed = if (scrubFraction >= 0f && durationMs > 0L) (scrubFraction * durationMs).toLong() else positionMs
    val lit = tone.playerMix(Color.White, 0.42f)
    val digits = if (compact) 20.sp else 24.sp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TurntableReadoutShape)
            .background(Color.Black.copy(alpha = 0.32f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), TurntableReadoutShape)
            .padding(horizontal = LevyraPlayerDesign.SpaceLg, vertical = if (compact) 8.dp else 12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatSeekbarMillis(elapsed),
                color = lit,
                fontSize = digits,
                fontWeight = FontWeight.SemiBold,
                style = TurntableTimeStyle,
                maxLines = 1
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "33⅓ RPM",
                    color = lit,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .border(1.dp, lit.copy(alpha = 0.35f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
            Text(
                text = if (durationMs > 0L) "-" + formatSeekbarMillis((durationMs - elapsed).coerceAtLeast(0L)) else "--:--",
                color = surfaces.contentMuted,
                fontSize = digits,
                fontWeight = FontWeight.Medium,
                style = TurntableTimeStyle,
                maxLines = 1
            )
        }
        if (details.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.06f))
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = details,
                color = surfaces.contentFaint,
                fontSize = 11.sp,
                letterSpacing = 0.4.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TurntableHeadline(
    track: Track,
    surfaces: PlayerSurfaceTokens,
    isFavorite: Boolean,
    animated: Boolean,
    compact: Boolean,
    stepDirection: Int,
    onArtistClick: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    AnimatedContent(
        targetState = track,
        modifier = modifier,
        transitionSpec = { LevyraMotion.trackChange(animated, stepDirection) },
        contentKey = { it.id },
        label = "player-turntable-headline"
    ) { shown ->
        val current = shown.id == track.id
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                val titleSize = if (compact) 22.sp else 27.sp
                Text(
                    text = shown.title,
                    color = surfaces.content,
                    fontSize = titleSize,
                    lineHeight = titleSize * 1.08f,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.6).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                PlayerArtistText(
                    track = shown,
                    color = surfaces.contentMuted,
                    style = TextStyle(fontSize = if (compact) 15.sp else 17.sp, fontWeight = FontWeight.Medium),
                    onClickLabel = strings.openArtist,
                    onArtistClick = onArtistClick,
                    enabled = current
                )
            }
            if (current) {
                PlayerFavoriteButton(
                    trackId = shown.id,
                    isFavorite = isFavorite,
                    surfaces = surfaces,
                    label = strings.favoritesPlain,
                    animated = animated,
                    onToggle = onToggleFavorite
                )
            } else {
                Box(modifier = Modifier.size(LevyraPlayerDesign.MinimumTouchTarget))
            }
        }
    }
}

@Composable
private fun TurntableTransport(
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    labels: PlayerControlLabels,
    tone: Color,
    surfaces: PlayerSurfaceTokens,
    compact: Boolean,
    animated: Boolean,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit
) {
    val strip = if (compact) 58.dp else 66.dp
    val play = strip * 1.32f
    val led = tone.playerMix(Color.White, 0.3f)
    val ink = surfaces.content
    val anodized = tone.playerMix(PlinthBase, 0.74f)
    val stripShape = RoundedCornerShape(50)
    Box(modifier = Modifier.fillMaxWidth().height(play), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(strip)
                .drawBehind {
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.45f),
                        topLeft = Offset(0f, size.height * 0.08f),
                        size = size,
                        cornerRadius = CornerRadius(size.height / 2f)
                    )
                }
                .clip(stripShape)
                .background(Brush.verticalGradient(listOf(anodized.playerMix(Color.White, 0.1f), anodized, anodized.playerMix(Color.Black, 0.4f))))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.02f))), stripShape),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TurntableStripKey(labels.shuffle, onShuffle, led, lit = shuffleOn) {
                PlayerIcon(Icons.Rounded.Shuffle, if (shuffleOn) led else ink.copy(alpha = 0.8f), Modifier.size(22.dp))
            }
            TurntableStripDivider()
            TurntableStripKey(labels.previous, onPrevious, led) {
                PlayerIcon(Icons.Rounded.SkipPrevious, ink, Modifier.size(30.dp))
            }
            Spacer(Modifier.width(play + 10.dp))
            TurntableStripKey(labels.next, onNext, led) {
                PlayerIcon(Icons.Rounded.SkipNext, ink, Modifier.size(30.dp))
            }
            TurntableStripDivider()
            TurntableStripKey(labels.repeat, onRepeat, led, lit = repeatMode != RepeatMode.Off) {
                PlayerIcon(
                    if (repeatMode == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    if (repeatMode != RepeatMode.Off) led else ink.copy(alpha = 0.8f),
                    Modifier.size(22.dp)
                )
            }
        }
        TurntableStartButton(
            label = if (isPlaying) labels.pause else labels.play,
            size = play,
            isPlaying = isPlaying,
            isResolving = isResolving,
            tone = tone,
            surfaces = surfaces,
            animated = animated,
            onClick = onTogglePlay
        )
    }
}

@Composable
private fun RowScope.TurntableStripKey(
    label: String,
    onClick: () -> Unit,
    led: Color,
    lit: Boolean? = null,
    glyph: @Composable () -> Unit
) {
    val ledLevel by animateFloatAsState(if (lit == true) 1f else 0f, tween(260), label = "turntable-strip-led")
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .levyraPressable(
                onClick = onClick,
                pressedScale = LevyraPressScale.Control,
                role = if (lit != null) Role.Switch else Role.Button,
                haptic = LevyraHapticAction.Transport
            )
            .semantics {
                contentDescription = label
                if (lit != null) toggleableState = ToggleableState(lit)
            },
        contentAlignment = Alignment.Center
    ) {
        glyph()
        if (lit != null) {
            Canvas(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).size(5.dp)) {
                drawCircle(Color.White.copy(alpha = 0.14f))
                if (ledLevel > 0f) {
                    drawCircle(led.copy(alpha = 0.35f * ledLevel), radius = size.minDimension * 1.4f)
                    drawCircle(led.copy(alpha = ledLevel))
                }
            }
        }
    }
}

@Composable
private fun TurntableStripDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight(0.42f)
            .background(Color.White.copy(alpha = 0.08f))
    )
}

@Composable
private fun TurntableStartButton(
    label: String,
    size: Dp,
    isPlaying: Boolean,
    isResolving: Boolean,
    tone: Color,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onClick: () -> Unit
) {
    val power by animateFloatAsState(if (isPlaying) 1f else 0f, tween(if (animated) 520 else 0), label = "turntable-start-power")
    val halo = tone.playerMix(Color.White, 0.3f)
    val face = surfaces.hero
    Box(
        modifier = Modifier
            .size(size)
            .drawBehind {
                drawCircle(Color.Black.copy(alpha = 0.5f), radius = size.toPx() / 2f, center = center + Offset(0f, size.toPx() * 0.06f))
                if (power > 0f) {
                    drawCircle(
                        Brush.radialGradient(listOf(halo.copy(alpha = 0.45f * power), Color.Transparent), center, size.toPx() * 0.72f),
                        radius = size.toPx() * 0.72f
                    )
                }
            }
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(face.playerMix(Color.White, 0.22f), face, face.playerMix(Color.Black, 0.18f))))
            .border(2.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), halo.copy(alpha = 0.2f + 0.6f * power))), CircleShape)
            .levyraPressable(
                onClick = onClick,
                enabled = !isResolving,
                pressedScale = LevyraPressScale.Control,
                role = Role.Button,
                haptic = LevyraHapticAction.Transport
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        if (isResolving) {
            LevyraLoadingIndicator(color = surfaces.heroContent, modifier = Modifier.size(size * 0.34f))
        } else {
            LevyraPlayPauseGlyph(playing = isPlaying, color = surfaces.heroContent, modifier = Modifier.size(size * 0.36f))
        }
    }
}

private const val TurntableActionQueue = "queue"
private const val TurntableActionLyrics = "lyrics"
private const val TurntableActionDownload = "download"
private const val TurntableActionDeck = "visual-mode"
private val TurntableActionOrder = listOf(TurntableActionQueue, TurntableActionLyrics, TurntableActionDownload, TurntableActionDeck)

@Composable
private fun TurntableActions(track: Track, slots: PlayerDeckSlots, tone: Color, surfaces: PlayerSurfaceTokens) {
    val byKey = slots.dockActions(track).associateBy { it.key }
    val highlight = tone.playerMix(Color.White, 0.3f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TurntableActionOrder.forEach { key ->
            byKey[key]?.let { action ->
                TurntableIconAction(
                    icon = action.icon,
                    label = action.label,
                    tint = if (action.active && key != TurntableActionDeck) highlight else surfaces.contentMuted,
                    enabled = action.enabled && !action.busy,
                    onClick = action.onClick
                )
            }
        }
    }
}

@Composable
private fun TurntableIconAction(icon: ImageVector, label: String, tint: Color, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(LevyraPlayerDesign.MinimumTouchTarget)
            .clip(CircleShape)
            .levyraPressable(
                onClick = onClick,
                enabled = enabled,
                pressedScale = LevyraPressScale.Control,
                role = Role.Button,
                haptic = LevyraHapticAction.Transport
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        PlayerIcon(icon, if (enabled) tint else tint.copy(alpha = 0.4f), Modifier.size(24.dp))
    }
}

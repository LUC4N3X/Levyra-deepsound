package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.AnimatedContent
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ChevronLeft
import java.util.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import com.luc4n3x.levyra.ui.components.formatSeekbarMillis
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayPauseGlyph
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.components.rememberLevyraPlayMorphShape
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val StackCardCorner = 30.dp
private val StackCardShape = RoundedCornerShape(StackCardCorner)
private val StackScrollingStage = 380.dp
private val StackCardElevation = 26.dp
private val StackToggleSize = 48.dp
private const val StackCardWidth = 0.86f
private const val StackRiseRoom = 1.3f
private const val StackRise = 0.12f
private const val StackRiseMax = 0.24f
private val StackHandleRoom = 52.dp
private const val StackLiftSpread = 0.22f
private const val StackDepthScale = 0.055f
private const val StackDepthTilt = 12f
private const val StackDepthShade = 0.12f
private const val StackPileRoll = 2.4f
private const val StackGrabTurn = 26f
private const val StackGrabRoll = 9f
private const val StackExitTurn = 28f
private const val StackExitRoll = 12f
private const val StackCamera = 11f
private const val StackParallax = 0.07f
private const val StackCommitFraction = 0.26f
private const val StackFlingVelocity = 1_400f
private const val StackQueueFraction = 0.42f
private const val StackPendingMillis = 1_100L
private const val StackQueueMillis = 180
private const val StackBehind = 3
private const val StackBefore = 2

internal fun stackNeighborIndex(index: Int, offset: Int, size: Int, shuffle: Boolean, repeatAll: Boolean): Int? {
    if (shuffle || index !in 0 until size) return null
    val target = index + offset
    return when {
        target in 0 until size -> target
        repeatAll && size > 1 -> ((target % size) + size) % size
        else -> null
    }
}

internal fun stackCardDepthAlpha(position: Float): Float =
    if (position >= 0f) (StackBehind - position).coerceIn(0f, 1f) else (1f - (-position - 0.55f) / 0.45f).coerceIn(0f, 1f)

private class StackTracker {
    var trackId: String = ""
    var base: Int = 0
    var pending: Int = 0

    fun follow(id: String, stepDirection: Int) {
        if (trackId == id) return
        if (trackId.isNotEmpty()) base += stackTrackerStep(pending, stepDirection)
        trackId = id
        pending = 0
    }
}

private fun stackTrackerStep(pending: Int, stepDirection: Int): Int = when {
    pending != 0 -> pending
    stepDirection < 0 -> -1
    else -> 1
}

private fun stackQueuePosition(queue: List<Track>, queueIndex: Int, trackId: String): Int =
    if (queue.getOrNull(queueIndex)?.id == trackId) queueIndex else queue.indexOfFirst { it.id == trackId }

private fun stackCounter(index: Int, size: Int): String? =
    if (index >= 0 && size > 1) String.format(Locale.ROOT, "%02d  /  %02d", index + 1, size) else null

private fun stackBackdropTone(track: Track?, fallback: Color): Color =
    track?.let { stackVivid(Color(it.accentStart)) } ?: fallback

private fun stackRise(side: Dp, maxHeight: Dp): Float =
    if (side > 0.dp) ((maxHeight - side - StackHandleRoom) / (side * 2f)).coerceIn(StackRise, StackRiseMax) else StackRise

private fun stackCardZ(virtual: Int, split: Int): Float =
    if (virtual < split) 1_000f + virtual else -virtual.toFloat()

private fun stackPressSpring(animated: Boolean, damping: Float, stiffness: Float): AnimationSpec<Float> =
    if (animated) spring(dampingRatio = damping, stiffness = stiffness) else snap()

private class StackMotion(
    val position: Animatable<Float, *>,
    val lift: Animatable<Float, *>,
    private val dragState: MutableFloatState,
    private val liftState: MutableFloatState
) {
    var drag: Float
        get() = dragState.floatValue
        set(value) {
            dragState.floatValue = value
        }
    var liftDrag: Float
        get() = liftState.floatValue
        set(value) {
            liftState.floatValue = value
        }
    val visual: Float get() = position.value + drag
    val raise: Float get() = (lift.value + liftDrag).coerceIn(0f, 1f)
}

@Composable
internal fun PlayerStackDeck(
    track: Track,
    slots: PlayerDeckSlots,
    surfaces: PlayerSurfaceTokens,
    accent: Color,
    artworkColors: Pair<Color, Color>,
    queue: List<Track>,
    queueIndex: Int,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    swipeEnabled: Boolean,
    gesturesEnabled: Boolean,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Float) -> Unit,
    isFavorite: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    labels: PlayerControlLabels,
    animated: Boolean,
    compact: Boolean,
    scrollable: Boolean,
    gutter: Dp,
    artworkCap: Dp,
    stepDirection: Int,
    onArtistClick: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
    headlineModifier: Modifier = Modifier
) {
    val vivid = remember(accent, artworkColors) { stackVivid(stackMostChromatic(listOf(artworkColors.first, artworkColors.second, accent))) }
    val tone by animateColorAsState(
        targetValue = vivid,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-stack-tone"
    )
    val index = stackQueuePosition(queue, queueIndex, track.id)
    val counter = remember(index, queue.size) { stackCounter(index, queue.size) }
    val tracker = remember { StackTracker() }
    tracker.follow(track.id, stepDirection)
    val base = tracker.base
    val repeatAll = repeatMode == RepeatMode.All
    val neighbor: (Int) -> Track? = remember(index, queue, shuffleOn, repeatAll) {
        { offset -> stackNeighborIndex(index, offset, queue.size, shuffleOn, repeatAll)?.let(queue::getOrNull) }
    }
    val backdropColors = remember(tone, neighbor) {
        stackBackdropTone(neighbor(-1), tone) to stackBackdropTone(neighbor(1), tone)
    }
    val stack = rememberStackMotion(base, animated)
    Box(modifier = modifier) {
        StackBackdrop(stack, base, tone, backdropColors, Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(
                    start = gutter,
                    end = gutter,
                    top = LevyraPlayerDesign.SpaceXs,
                    bottom = if (compact) LevyraPlayerDesign.SpaceSm else LevyraPlayerDesign.SpaceLg
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            slots.header()
            StackStage(
                track = track,
                slots = slots,
                tone = tone,
                stack = stack,
                tracker = tracker,
                base = base,
                neighbor = neighbor,
                shuffleOn = shuffleOn,
                swipeEnabled = swipeEnabled,
                gesturesEnabled = gesturesEnabled,
                animated = animated,
                artworkCap = artworkCap,
                onPrevious = onPrevious,
                onNext = onNext,
                onOpenQueue = onOpenQueue,
                isPlaying = isPlaying,
                isResolving = isResolving,
                labels = labels,
                isFavorite = isFavorite,
                onTogglePlay = onTogglePlay,
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (scrollable) Modifier.height(StackScrollingStage) else Modifier.weight(1f))
            )
            Spacer(Modifier.height(StackTokenSize / 2 + LevyraPlayerDesign.SpaceSm))
            StackHeadline(
                track = track,
                surfaces = surfaces,
                tone = tone,
                counter = counter,
                animated = animated,
                compact = compact,
                stepDirection = stepDirection,
                onArtistClick = onArtistClick,
                modifier = headlineModifier
            )
            StackSeekbar(
                trackKey = track.id,
                positionMs = positionMs,
                durationMs = durationMs,
                tone = tone,
                surfaces = surfaces,
                animated = animated,
                onSeek = onSeek,
                modifier = Modifier.padding(top = LevyraPlayerDesign.SpaceXs)
            )
            slots.status()
            StackToolRow(
                actions = slots.dockActions(track),
                surfaces = surfaces,
                tone = tone,
                shuffleOn = shuffleOn,
                repeatMode = repeatMode,
                labels = labels,
                onShuffle = onShuffle,
                onRepeat = onRepeat
            )
        }
    }
}

@Composable
private fun StackBackdrop(stack: StackMotion, base: Int, tone: Color, colors: Pair<Color, Color>, modifier: Modifier) {
    Box(
        modifier = modifier
            .graphicsLayer { }
            .drawBehind {
                val shift = stack.visual - base
                val target = if (shift >= 0f) colors.second else colors.first
                val glow = lerp(tone, target, abs(shift).coerceIn(0f, 1f))
                drawRect(
                    Brush.verticalGradient(
                        0f to lerp(glow, Color.Black, 0.3f),
                        0.45f to lerp(glow, Color.Black, 0.55f),
                        0.72f to lerp(glow, Color.Black, 0.8f),
                        1f to lerp(glow, Color.Black, 0.92f)
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(lerp(glow, Color.White, 0.18f).copy(alpha = 0.5f), glow.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.3f),
                        radius = size.width * 0.8f
                    )
                )
            }
    )
}

@Composable
private fun rememberStackMotion(base: Int, animated: Boolean): StackMotion {
    val motion = remember {
        StackMotion(
            position = Animatable(base.toFloat()),
            lift = Animatable(0f),
            dragState = mutableFloatStateOf(0f),
            liftState = mutableFloatStateOf(0f)
        )
    }
    LaunchedEffect(base) {
        val gap = base - motion.position.value
        if (abs(gap) > StackBefore) motion.position.snapTo(base - gap.sign)
        motion.position.animateTo(base.toFloat(), stackSpring(animated))
    }
    return motion
}

private fun stackSpring(animated: Boolean): AnimationSpec<Float> =
    if (animated) spring(dampingRatio = 0.82f, stiffness = 820f) else snap()

@Composable
private fun StackStage(
    track: Track,
    slots: PlayerDeckSlots,
    tone: Color,
    stack: StackMotion,
    tracker: StackTracker,
    base: Int,
    neighbor: (Int) -> Track?,
    shuffleOn: Boolean,
    swipeEnabled: Boolean,
    gesturesEnabled: Boolean,
    animated: Boolean,
    artworkCap: Dp,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenQueue: () -> Unit,
    isPlaying: Boolean,
    isResolving: Boolean,
    labels: PlayerControlLabels,
    isFavorite: Boolean,
    onTogglePlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val haptics = LocalLevyraHaptics.current
    val depth = LocalLevyraVisualCapabilities.current.decorativeMotion
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val canForward = shuffleOn || neighbor(1) != null
    val release = rememberStackRelease(stack, tracker, base, animated, canForward, onNext, onPrevious, onOpenQueue)
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        val side = minOf(maxWidth * StackCardWidth, maxHeight / StackRiseRoom, artworkCap).coerceAtLeast(0.dp)
        val sidePx = with(LocalDensity.current) { side.toPx() }.coerceAtLeast(1f)
        val inset = (maxWidth - side) / 2
        val rise = stackRise(side, maxHeight)
        val split by remember(stack) { derivedStateOf { ceil(stack.visual).toInt() } }
        val travel = if (rtl) 1f else -1f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .stackDragGestures(
                    swipeEnabled = gesturesEnabled && swipeEnabled,
                    liftEnabled = gesturesEnabled,
                    sidePx = sidePx,
                    rtl = rtl,
                    stack = stack,
                    canForward = release.canForward,
                    onStart = release.onStart,
                    onArmed = { haptics.perform(LevyraHapticAction.SeekSnap) },
                    onSwipeEnd = release.onSwipeEnd,
                    onLiftEnd = release.onLiftEnd
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            StackFloorShadow(side)
            for (offset in StackBehind downTo -StackBefore) {
                val virtual = base + offset
                val card: Track? = if (offset == 0) track else neighbor(offset)
                if (offset != 0 && card == null && !shuffleOn) continue
                key(virtual) {
                    StackCard(
                        offset = offset,
                        card = card,
                        slots = slots,
                        tone = tone,
                        stack = stack,
                        side = side,
                        transform = StackCardFrame(virtual, stackCardZ(virtual, split), rise, travel, depth)
                    )
                }
            }
            slots.gestures(track, Modifier.matchParentSize().zIndex(1_500f))
            StackStageOverlays(
                tone = tone,
                stack = stack,
                side = side,
                inset = inset,
                rtl = rtl,
                animated = animated,
                isPlaying = isPlaying,
                isResolving = isResolving,
                labels = labels,
                isFavorite = isFavorite,
                queueLabel = strings.queue,
                onOpenQueue = {
                    haptics.perform(LevyraHapticAction.Confirm)
                    onOpenQueue()
                },
                onPrevious = onPrevious,
                onNext = onNext,
                onTogglePlay = onTogglePlay,
                onToggleFavorite = onToggleFavorite
            )
        }
    }
}

private class StackCardFrame(val virtual: Int, val z: Float, val rise: Float, val travel: Float, val depth: Boolean)

@Composable
private fun StackCard(
    offset: Int,
    card: Track?,
    slots: PlayerDeckSlots,
    tone: Color,
    stack: StackMotion,
    side: Dp,
    transform: StackCardFrame
) {
    val virtual = transform.virtual
    Box(
        modifier = Modifier
            .size(side)
            .zIndex(transform.z)
            .graphicsLayer {
                stackCardTransform(
                    p = virtual - stack.visual,
                    raise = stack.raise,
                    drag = stack.drag,
                    cardSize = side.toPx(),
                    rise = transform.rise,
                    travel = transform.travel,
                    depth = transform.depth,
                    pile = if (virtual % 2 == 0) StackPileRoll else -StackPileRoll
                )
            }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    val p = (virtual - stack.visual).coerceIn(-1f, 1f)
                    val zoom = 1f + StackParallax * abs(p)
                    scaleX = zoom
                    scaleY = zoom
                    translationX = -p * size.width * StackParallax * transform.travel
                }
        ) {
            when {
                offset == 0 && card != null -> slots.stage(card, Modifier.size(side), StackCardCorner)
                card != null -> StackArtwork(card, near = abs(offset) <= 1, caption = offset > 0)
                else -> StackBlankCard(tone, offset)
            }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind { drawStackCardFinish(virtual - stack.visual) }
        )
    }
}

private class StackRelease(
    val canForward: () -> Boolean,
    val onStart: () -> Unit,
    val onSwipeEnd: (Float) -> Unit,
    val onLiftEnd: () -> Unit
)

@Composable
private fun rememberStackRelease(
    stack: StackMotion,
    tracker: StackTracker,
    base: Int,
    animated: Boolean,
    canForward: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenQueue: () -> Unit
): StackRelease {
    val haptics = LocalLevyraHaptics.current
    val scope = rememberCoroutineScope()
    val latestNext by rememberUpdatedState(onNext)
    val latestPrevious by rememberUpdatedState(onPrevious)
    val latestQueue by rememberUpdatedState(onOpenQueue)
    val latestBase by rememberUpdatedState(base)
    val latestCanForward by rememberUpdatedState(canForward)
    val latestAnimated by rememberUpdatedState(animated)
    val pendingToken = remember { mutableIntStateOf(0) }
    LaunchedEffect(pendingToken.intValue) {
        if (pendingToken.intValue == 0) return@LaunchedEffect
        delay(StackPendingMillis)
        if (tracker.pending != 0) {
            tracker.pending = 0
            stack.position.animateTo(latestBase.toFloat(), stackSpring(latestAnimated))
        }
    }
    return remember(stack, tracker, haptics, scope) {
        StackRelease(
            canForward = { latestCanForward },
            onStart = {
                scope.launch {
                    stack.position.stop()
                    stack.lift.stop()
                }
            },
            onSwipeEnd = { fling ->
                val shift = stack.drag
                val step = stackSwipeStep(shift, fling, latestCanForward)
                scope.launch {
                    stack.position.snapTo(stack.position.value + shift)
                    stack.drag = 0f
                    if (step != 0) {
                        haptics.perform(LevyraHapticAction.TrackSwipe)
                        tracker.pending = step
                        pendingToken.intValue += 1
                        if (step > 0) latestNext() else latestPrevious()
                    }
                    stack.position.animateTo((latestBase + step).toFloat(), stackSpring(latestAnimated))
                }
            },
            onLiftEnd = {
                val open = stack.liftDrag > StackQueueFraction
                scope.launch {
                    stack.lift.snapTo(stack.liftDrag)
                    stack.liftDrag = 0f
                    if (open) {
                        haptics.perform(LevyraHapticAction.Confirm)
                        if (latestAnimated) stack.lift.animateTo(1f, tween(StackQueueMillis, easing = FastOutSlowInEasing))
                        latestQueue()
                        if (latestAnimated) delay(StackQueueMillis.toLong())
                    }
                    stack.lift.animateTo(0f, stackSpring(latestAnimated))
                }
            }
        )
    }
}

@Composable
private fun StackArtwork(track: Track, near: Boolean, caption: Boolean) {
    val context = LocalContext.current
    val url = track.thumbnailUrl
    val request = remember(context, url, near) {
        ImageRequest.Builder(context)
            .data(if (near) LevyraArtworkCache.large(url) else LevyraArtworkCache.small(url))
            .build()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(track.accentStart), Color(track.accentEnd))))
            .clearAndSetSemantics { }
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (caption) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Black.copy(alpha = 0.3f), Color.Transparent)))
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 18.dp)
            ) {
                Column {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.2).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StackBlankCard(tone: Color, offset: Int) {
    val shade = 0.55f + 0.08f * abs(offset)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(lerp(tone, Color.Black, shade), lerp(tone, Color.Black, (shade + 0.2f).coerceAtMost(1f)))
                )
            )
            .clearAndSetSemantics { }
    )
}

private val StackSeekTouch = 48.dp
private const val StackSeekPill = 26f
private const val StackSeekLabelRoom = 0.16f
private val StackTabWidth = 26.dp
private val StackTabHeight = 88.dp
private val StackTokenSize = 72.dp
private val StackTabular = TextStyle(fontFeatureSettings = "tnum")

@Composable
private fun StackQueueHandle(
    label: String,
    tone: Color,
    stack: StackMotion,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ink = remember(tone) { lerp(tone, Color.White, 0.82f) }
    Box(
        modifier = modifier
            .size(width = 72.dp, height = 44.dp)
            .clip(RoundedCornerShape(50))
            .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .semantics { contentDescription = label }
            .graphicsLayer {
                val raise = stack.raise
                translationY = -raise * 10.dp.toPx()
                alpha = 0.72f + 0.28f * raise
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PlayerIcon(Icons.Rounded.KeyboardArrowUp, ink, Modifier.size(18.dp))
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 12.dp)
                    .drawBehind {
                        val raise = stack.raise
                        val unit = 1.dp.toPx()
                        val bar = 2.5f * unit
                        val gap = (3.5f + 2.5f * raise) * unit
                        for (level in 0..2) {
                            val width = (32f - level * 9f) * unit
                            val y = size.height - bar - level * gap
                            drawRoundRect(
                                color = ink.copy(alpha = 0.9f - level * 0.25f),
                                topLeft = Offset((size.width - width) / 2f, y),
                                size = Size(width, bar),
                                cornerRadius = CornerRadius(bar / 2f)
                            )
                        }
                    }
            )
        }
    }
}

internal fun stackChroma(color: Color): Float {
    val high = max(color.red, max(color.green, color.blue))
    val low = minOf(color.red, color.green, color.blue)
    return if (high <= 0f) 0f else (high - low) / high * high.coerceAtLeast(0.25f)
}

internal fun stackMostChromatic(colors: List<Color>): Color = colors.maxByOrNull(::stackChroma) ?: Color.Gray

internal fun stackVivid(color: Color): Color {
    val r = color.red
    val g = color.green
    val b = color.blue
    val high = max(r, max(g, b))
    val low = minOf(r, g, b)
    val delta = high - low
    val saturation = if (high <= 0f) 0f else delta / high
    if (saturation < 0.08f) {
        val value = high.coerceIn(0.28f, 0.5f)
        return Color(value, value, (value * 1.04f).coerceAtMost(1f), 1f)
    }
    val hue = when (high) {
        r -> 60f * ((g - b) / delta % 6f)
        g -> 60f * ((b - r) / delta + 2f)
        else -> 60f * ((r - g) / delta + 4f)
    }.let { if (it < 0f) it + 360f else it }
    return Color.hsv(hue, saturation.coerceIn(0.38f, 0.82f), high.coerceIn(0.42f, 0.78f))
}

@Composable
private fun StackSeekbar(
    trackKey: String,
    positionMs: Long,
    durationMs: Long,
    tone: Color,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalLevyraHaptics.current
    val scrub = remember(trackKey) { StackScrub() }
    val played = stackPlayedFraction(positionMs, durationMs)
    val shown = scrub.shown(played)
    val grip by animateFloatAsState(
        targetValue = scrub.gripTarget(),
        animationSpec = stackPressSpring(animated, 0.72f, 900f),
        label = "player-stack-seek-grip"
    )
    val fillStart = remember(tone) { lerp(tone, Color.White, 0.5f) }
    val fillEnd = remember(tone) { lerp(tone, Color.White, 0.86f) }
    val latestSeek by rememberUpdatedState(onSeek)
    val elapsed = formatSeekbarMillis((shown * durationMs).toLong())
    val total = stackSeekTotal(durationMs)
    val seekable = durationMs > 0L
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(StackSeekTouch)
                .stackSeekGestures(
                    seekable = seekable,
                    trackKey = trackKey,
                    scrub = scrub,
                    onTick = { haptics.perform(LevyraHapticAction.SeekSnap) },
                    onSeek = { latestSeek(it) }
                )
                .stackSeekSemantics(played, "$elapsed / $total", seekable) { latestSeek(it) }
                .drawBehind { drawStackSeekPill(grip, shown, fillStart, fillEnd) }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            StackSeekLabels(elapsed, total, shown, grip, surfaces)
        }
    }
}

private class StackScrub {
    var seeking by mutableStateOf(false)
    var fraction by mutableFloatStateOf(0f)
    private var tickStep = -1

    fun shown(played: Float): Float = if (seeking) fraction else played

    fun gripTarget(): Float = if (seeking) 1f else 0f

    fun start(at: Float) {
        fraction = at
        tickStep = (at * 10f).toInt()
        seeking = true
    }

    fun drag(delta: Float): Boolean {
        fraction = (fraction + delta).coerceIn(0f, 1f)
        val step = (fraction * 10f).toInt()
        if (step == tickStep) return false
        tickStep = step
        return true
    }
}

private fun stackPlayedFraction(positionMs: Long, durationMs: Long): Float =
    if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

private fun stackSeekTotal(durationMs: Long): String =
    if (durationMs > 0L) formatSeekbarMillis(durationMs) else "--:--"

private fun Modifier.stackSeekGestures(
    seekable: Boolean,
    trackKey: String,
    scrub: StackScrub,
    onTick: () -> Unit,
    onSeek: (Float) -> Unit
): Modifier {
    if (!seekable) return this
    return pointerInput(trackKey) {
        detectTapGestures(onTap = { offset ->
            onTick()
            onSeek((offset.x / size.width).coerceIn(0f, 1f))
        })
    }.pointerInput(trackKey) {
        detectHorizontalDragGestures(
            onDragStart = { offset ->
                scrub.start((offset.x / size.width).coerceIn(0f, 1f))
                onTick()
            },
            onDragEnd = {
                onSeek(scrub.fraction)
                scrub.seeking = false
            },
            onDragCancel = { scrub.seeking = false },
            onHorizontalDrag = { change, amount ->
                change.consume()
                if (scrub.drag(amount / size.width)) onTick()
            }
        )
    }
}

private fun Modifier.stackSeekSemantics(played: Float, state: String, seekable: Boolean, onSeek: (Float) -> Unit): Modifier =
    semantics {
        progressBarRangeInfo = ProgressBarRangeInfo(played, 0f..1f)
        stateDescription = state
        if (seekable) {
            setProgress { value ->
                onSeek(value.coerceIn(0f, 1f))
                true
            }
        }
    }

private fun DrawScope.drawStackSeekPill(grip: Float, shown: Float, fillStart: Color, fillEnd: Color) {
    val height = (StackSeekPill + 10f * grip).dp.toPx()
    val top = (size.height - height) / 2f
    val radius = CornerRadius(height / 2f)
    drawRoundRect(
        color = Color.White.copy(alpha = 0.12f + 0.04f * grip),
        topLeft = Offset(0f, top),
        size = Size(size.width, height),
        cornerRadius = radius
    )
    val fill = (size.width * shown).coerceAtLeast(height)
    drawRoundRect(
        brush = Brush.horizontalGradient(listOf(fillStart, fillEnd), startX = 0f, endX = fill),
        topLeft = Offset(0f, top),
        size = Size(fill, height),
        cornerRadius = radius
    )
    val knob = 3.dp.toPx()
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.22f),
        topLeft = Offset(fill - knob * 3f, top + height * 0.28f),
        size = Size(knob, height * 0.44f),
        cornerRadius = CornerRadius(knob / 2f)
    )
}

@Composable
private fun StackSeekLabels(elapsed: String, total: String, shown: Float, grip: Float, surfaces: PlayerSurfaceTokens) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val scale = 1f + 0.08f * grip
                scaleX = scale
                scaleY = scale
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = elapsed,
            color = if (shown > StackSeekLabelRoom) Color.Black.copy(alpha = 0.82f) else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            style = StackTabular
        )
        Text(
            text = total,
            color = if (shown > 1f - StackSeekLabelRoom) Color.Black.copy(alpha = 0.7f) else surfaces.contentMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            style = StackTabular
        )
    }
}

@Composable
private fun StackPlayToken(
    tone: Color,
    isPlaying: Boolean,
    isResolving: Boolean,
    labels: PlayerControlLabels,
    animated: Boolean,
    size: Dp,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalLevyraHaptics.current
    val source = remember { MutableInteractionSource() }
    val press by rememberStackPress(source, animated, 0.6f, 1_200f, "player-stack-token-press")
    val face = remember(tone) { lerp(tone, Color.White, 0.84f) }
    val ring = remember(tone) { lerp(tone, Color.Black, 0.35f) }
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val scale = 1f - 0.08f * press
                scaleX = scale
                scaleY = scale
                translationY = press * 3.dp.toPx()
                shadowElevation = (20f - 14f * press).dp.toPx()
                shape = CircleShape
                ambientShadowColor = Color.Black
                spotShadowColor = Color.Black
            }
            .clip(rememberLevyraPlayMorphShape(playing = isPlaying, animated = animated))
            .background(ring)
            .padding(4.dp)
            .clip(rememberLevyraPlayMorphShape(playing = isPlaying, animated = animated))
            .background(face)
            .levyraPressable(
                onClick = {
                    haptics.perform(LevyraHapticAction.Transport)
                    onTogglePlay()
                },
                role = Role.Button,
                pressedScale = 1f,
                interactionSource = source
            )
            .semantics { contentDescription = stackPlayLabel(labels, isPlaying) },
        contentAlignment = Alignment.Center
    ) {
        StackTokenGlyph(isPlaying, isResolving)
    }
}

private fun stackPlayLabel(labels: PlayerControlLabels, isPlaying: Boolean): String =
    if (isPlaying) labels.pause else labels.play

@Composable
private fun StackTokenGlyph(isPlaying: Boolean, isResolving: Boolean) {
    if (isResolving) {
        LevyraLoadingIndicator(color = Color.Black, modifier = Modifier.size(32.dp))
    } else {
        LevyraPlayPauseGlyph(playing = isPlaying, color = Color.Black, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun rememberStackPress(
    source: MutableInteractionSource,
    animated: Boolean,
    damping: Float,
    stiffness: Float,
    label: String
): State<Float> {
    val pressed by source.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = stackPressSpring(animated, damping, stiffness),
        label = label
    )
}

@Composable
private fun StackSideTab(
    icon: ImageVector,
    label: String,
    outward: Float,
    tone: Color,
    animated: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptics = LocalLevyraHaptics.current
    val source = remember { MutableInteractionSource() }
    val slide by rememberStackPress(source, animated, 0.55f, 1_400f, "player-stack-side-tab")
    val fill = remember(tone) { lerp(tone, Color.Black, 0.35f).copy(alpha = 0.72f) }
    Box(
        modifier = modifier
            .size(width = StackTabWidth, height = StackTabHeight)
            .graphicsLayer { translationX = outward * slide * 6.dp.toPx() }
            .clip(RoundedCornerShape(18.dp))
            .background(fill)
            .levyraPressable(
                onClick = {
                    haptics.perform(LevyraHapticAction.Transport)
                    onClick()
                },
                role = Role.Button,
                pressedScale = 1f,
                interactionSource = source
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        PlayerIcon(icon, Color.White, Modifier.size(24.dp))
    }
}

@Composable
private fun StackFavoriteStamp(
    isFavorite: Boolean,
    tone: Color,
    stack: StackMotion,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val haptics = LocalLevyraHaptics.current
    val ink = remember(tone) { lerp(tone, Color.White, 0.75f) }
    Box(
        modifier = modifier
            .size(StackToggleSize)
            .graphicsLayer { alpha = (1f - abs(stack.drag) * 5f).coerceIn(0f, 1f) }
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.36f))
            .levyraPressable(
                onClick = {
                    haptics.perform(LevyraHapticAction.Favorite)
                    onToggle()
                },
                role = Role.Button,
                pressedScale = LevyraPressScale.Control
            )
            .semantics {
                contentDescription = stackFavoriteLabel(strings, isFavorite)
                selected = isFavorite
            },
        contentAlignment = Alignment.Center
    ) {
        StackFavoriteGlyph(isFavorite, ink)
    }
}

private fun stackFavoriteLabel(strings: LevyraStrings, isFavorite: Boolean): String =
    if (isFavorite) strings.removeFromFavorites else strings.addToFavorites

@Composable
private fun StackFavoriteGlyph(isFavorite: Boolean, ink: Color) {
    if (isFavorite) {
        PlayerIcon(Icons.Rounded.Favorite, ink, Modifier.size(22.dp))
    } else {
        PlayerIcon(Icons.Rounded.FavoriteBorder, Color.White, Modifier.size(22.dp))
    }
}

@Composable
private fun StackHeadline(
    track: Track,
    surfaces: PlayerSurfaceTokens,
    tone: Color,
    counter: String?,
    animated: Boolean,
    compact: Boolean,
    stepDirection: Int,
    onArtistClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val lit = remember(tone) { lerp(tone, Color.White, 0.6f) }
    AnimatedContent(
        targetState = track,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = { LevyraMotion.trackChange(animated, stepDirection) },
        contentKey = { it.id },
        label = "player-stack-headline"
    ) { shown ->
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (counter != null) {
                Text(
                    text = counter,
                    color = lit,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.4.sp,
                    style = StackTabular
                )
            }
            Text(
                text = shown.title,
                color = surfaces.content,
                fontSize = if (compact) 22.sp else 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.6).sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            PlayerArtistText(
                track = shown,
                color = surfaces.contentMuted,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, textAlign = TextAlign.Center),
                onClickLabel = strings.openArtist,
                onArtistClick = onArtistClick,
                enabled = shown.id == track.id
            )
        }
    }
}

@Composable
private fun StackToolRow(
    actions: List<PlayerDockAction>,
    surfaces: PlayerSurfaceTokens,
    tone: Color,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    labels: PlayerControlLabels,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit
) {
    val ink = remember(tone) { lerp(tone, Color.White, 0.7f) }
    val haptics = LocalLevyraHaptics.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StackTool(Icons.Rounded.Shuffle, labels.shuffle, shuffleOn, true, false, ink, surfaces) {
            haptics.perform(LevyraHapticAction.Confirm)
            onShuffle()
        }
        actions.forEach { action ->
            StackTool(action.icon, action.label, action.active, action.enabled, action.busy, ink, surfaces, action.stateDescription, action.toggle) {
                action.haptic?.let(haptics::perform)
                action.onClick()
            }
        }
        StackTool(
            if (repeatMode == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            labels.repeat,
            repeatMode != RepeatMode.Off,
            true,
            false,
            ink,
            surfaces
        ) {
            haptics.perform(LevyraHapticAction.Confirm)
            onRepeat()
        }
    }
}

@Composable
private fun StackTool(
    icon: ImageVector,
    label: String,
    active: Boolean,
    enabled: Boolean,
    busy: Boolean,
    ink: Color,
    surfaces: PlayerSurfaceTokens,
    state: String? = null,
    toggle: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(StackToggleSize)
            .clip(CircleShape)
            .levyraPressable(onClick = onClick, enabled = enabled && !busy, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .semantics {
                contentDescription = label
                state?.let { stateDescription = it }
                if (toggle) selected = active
            },
        contentAlignment = Alignment.Center
    ) {
        if (busy) {
            LevyraLoadingIndicator(color = surfaces.contentMuted, modifier = Modifier.size(20.dp))
        } else {
            PlayerIcon(icon, (if (active) ink else surfaces.contentMuted).copy(alpha = if (enabled) 1f else 0.4f), Modifier.size(22.dp))
        }
        if (active) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .size(width = 10.dp, height = 3.dp)
                    .background(ink, CircleShape)
            )
        }
    }
}

internal fun stackSwipeStep(shift: Float, fling: Float, canForward: Boolean): Int {
    val step = when {
        shift > StackCommitFraction || fling > StackFlingVelocity -> 1
        shift < -StackCommitFraction || fling < -StackFlingVelocity -> -1
        else -> 0
    }
    return if (step > 0 && !canForward) 0 else step
}

private fun Modifier.stackDragGestures(
    swipeEnabled: Boolean,
    liftEnabled: Boolean,
    sidePx: Float,
    rtl: Boolean,
    stack: StackMotion,
    canForward: () -> Boolean,
    onStart: () -> Unit,
    onArmed: () -> Unit,
    onSwipeEnd: (Float) -> Unit,
    onLiftEnd: () -> Unit
): Modifier = pointerInput(swipeEnabled, liftEnabled, sidePx, rtl) {
    val direction = if (rtl) 1f else -1f
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val velocity = VelocityTracker()
        velocity.addPosition(down.uptimeMillis, down.position)
        var total = Offset.Zero
        var axis = 0
        var armed = false
        while (true) {
            val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            val delta = change.positionChange()
            total += delta
            velocity.addPosition(change.uptimeMillis, change.position)
            if (axis == 0 && total.getDistance() > viewConfiguration.touchSlop) {
                axis = stackGestureAxis(total, change.isConsumed, swipeEnabled, liftEnabled)
                if (axis > 0) onStart()
            }
            if (axis < 0) break
            if (axis == 0) continue
            change.consume()
            if (axis == 1) {
                val crossed = stackTrackDrag(stack, direction * delta.x / sidePx, canForward())
                if (crossed != armed) {
                    armed = crossed
                    if (crossed) onArmed()
                }
            } else {
                stack.liftDrag = (stack.liftDrag - delta.y / (sidePx * 1.1f)).coerceIn(0f, 1f)
            }
        }
        when (axis) {
            1 -> onSwipeEnd(velocity.calculateVelocity().x * direction)
            2 -> onLiftEnd()
        }
    }
}

private fun stackGestureAxis(total: Offset, consumed: Boolean, swipeEnabled: Boolean, liftEnabled: Boolean): Int = when {
    consumed -> -1
    abs(total.x) > abs(total.y) -> if (swipeEnabled) 1 else -1
    total.y < 0f -> if (liftEnabled) 2 else -1
    else -> -1
}

private fun stackTrackDrag(stack: StackMotion, step: Float, canForward: Boolean): Boolean {
    val resisted = if (!canForward && stack.drag + step > 0f) step * 0.35f else step
    stack.drag = (stack.drag + resisted).coerceIn(-1.1f, 1.1f)
    return abs(stack.drag) > StackCommitFraction
}

@Composable
private fun StackFloorShadow(side: Dp) {
    Box(
        modifier = Modifier
            .size(width = side * 1.05f, height = side * 0.16f)
            .graphicsLayer { translationY = side.toPx() * 0.07f }
            .drawBehind {
                scale(scaleX = 1f, scaleY = 0.22f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent),
                            center = center,
                            radius = size.width / 2f
                        ),
                        radius = size.width / 2f,
                        center = center
                    )
                }
            }
            .zIndex(-10_000f)
    )
}

private fun GraphicsLayerScope.stackCardTransform(
    p: Float,
    raise: Float,
    drag: Float,
    cardSize: Float,
    rise: Float,
    travel: Float,
    depth: Boolean,
    pile: Float
) {
    cameraDistance = StackCamera * density
    if (p >= 0f) stackUpcomingTransform(p, raise, drag, cardSize, rise, travel, depth, pile) else stackLeavingTransform(-p, cardSize, travel, depth)
    alpha = stackCardDepthAlpha(p)
    shadowElevation = if (abs(p) < 1.2f) StackCardElevation.toPx() * (1f - 0.4f * abs(p)) else 0f
    ambientShadowColor = Color.Black
    spotShadowColor = Color.Black
    shape = StackCardShape
    clip = true
}

private fun GraphicsLayerScope.stackUpcomingTransform(
    p: Float,
    raise: Float,
    drag: Float,
    cardSize: Float,
    rise: Float,
    travel: Float,
    depth: Boolean,
    pile: Float
) {
    val reach = p.coerceAtMost(StackBehind.toFloat())
    val grab = (1f - reach).coerceIn(0f, 1f) * (drag * 4f).coerceIn(-1f, 1f)
    val scale = 1f - StackDepthScale * reach + 0.02f * abs(grab)
    transformOrigin = TransformOrigin(0.5f, if (reach > 0f) 0f else 0.5f)
    scaleX = scale
    scaleY = scale
    translationY = -reach * cardSize * (rise + StackLiftSpread * raise) - raise * cardSize * 0.04f
    translationX = travel * grab * cardSize * 0.04f
    rotationX = if (depth) StackDepthTilt * reach.coerceAtMost(2f) * (1f - 0.55f * raise) else 0f
    rotationY = if (depth) -travel * grab * StackGrabTurn * 0.35f else 0f
    rotationZ = pile * reach.coerceAtMost(1f) + travel * grab * StackGrabRoll * 0.3f
}

private fun GraphicsLayerScope.stackLeavingTransform(distance: Float, cardSize: Float, travel: Float, depth: Boolean) {
    val away = distance.coerceAtMost(StackBefore.toFloat())
    val arc = away * (2f - away).coerceAtLeast(0f)
    val scale = 1f + 0.03f * arc - 0.05f * away
    transformOrigin = TransformOrigin(0.5f, 0.85f)
    scaleX = scale
    scaleY = scale
    translationX = travel * away * cardSize * 1.12f
    translationY = -arc * cardSize * 0.06f
    rotationX = 0f
    rotationY = if (depth) -travel * away * StackExitTurn else 0f
    rotationZ = travel * away * StackExitRoll
}

private fun DrawScope.drawStackCardFinish(p: Float) {
    val shade = (p * StackDepthShade).coerceIn(0f, 0.5f)
    if (shade > 0f) {
        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = shade * 0.6f), Color.Black.copy(alpha = shade * 1.5f))))
    }
    val rim = 1.5.dp.toPx()
    drawRect(
        brush = Brush.verticalGradient(0f to Color.White.copy(alpha = 0.34f), 1f to Color.Transparent, endY = rim * 8f),
        size = size.copy(height = rim * 8f)
    )
    drawRect(color = Color.Black.copy(alpha = 0.35f), topLeft = Offset(0f, size.height - rim), size = size.copy(height = rim))
}

@Composable
private fun BoxScope.StackStageOverlays(
    tone: Color,
    stack: StackMotion,
    side: Dp,
    inset: Dp,
    rtl: Boolean,
    animated: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    labels: PlayerControlLabels,
    isFavorite: Boolean,
    queueLabel: String,
    onOpenQueue: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTogglePlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val tabX = (inset - StackTabWidth / 2).coerceAtLeast(0.dp)
    val tabY = -(side / 2 - StackTabHeight / 2)
    StackQueueHandle(
        label = queueLabel,
        tone = tone,
        stack = stack,
        onOpen = onOpenQueue,
        modifier = Modifier.align(Alignment.TopCenter).zIndex(2_000f)
    )
    StackSideTab(
        icon = if (rtl) Icons.Rounded.ChevronRight else Icons.Rounded.ChevronLeft,
        label = labels.previous,
        outward = if (rtl) 1f else -1f,
        tone = tone,
        animated = animated,
        modifier = Modifier.align(Alignment.BottomStart).offset(x = tabX, y = tabY).zIndex(2_050f),
        onClick = onPrevious
    )
    StackSideTab(
        icon = if (rtl) Icons.Rounded.ChevronLeft else Icons.Rounded.ChevronRight,
        label = labels.next,
        outward = if (rtl) -1f else 1f,
        tone = tone,
        animated = animated,
        modifier = Modifier.align(Alignment.BottomEnd).offset(x = -tabX, y = tabY).zIndex(2_050f),
        onClick = onNext
    )
    StackFavoriteStamp(
        isFavorite = isFavorite,
        tone = tone,
        stack = stack,
        onToggle = onToggleFavorite,
        modifier = Modifier.align(Alignment.BottomEnd).offset(x = -(inset + 12.dp), y = -12.dp).zIndex(2_060f)
    )
    StackPlayToken(
        tone = tone,
        isPlaying = isPlaying,
        isResolving = isResolving,
        labels = labels,
        animated = animated,
        size = StackTokenSize,
        onTogglePlay = onTogglePlay,
        modifier = Modifier.align(Alignment.BottomCenter).offset(y = StackTokenSize / 2).zIndex(2_100f)
    )
}

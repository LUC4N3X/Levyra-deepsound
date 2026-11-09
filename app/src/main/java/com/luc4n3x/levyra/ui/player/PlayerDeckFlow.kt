package com.luc4n3x.levyra.ui.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.key
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import com.luc4n3x.levyra.ui.components.levyraExpressiveToggleCorner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.PlayerDarkSurface
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayPauseGlyph
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.feature.radio.isLiveRadio
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.playerMix
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics

private val FlowScrollingStage = 300.dp
private val FlowStageCorner = 0.dp
private val FlowCoverShadow = 22.dp
private const val FlowCoverPressScale = 0.965f
private const val FlowLightPlaying = 0.46f
private const val FlowLightPaused = 0.24f
private const val FlowLightAmoledFactor = 0.55f
private const val FlowLightReach = 0.86f
private const val FlowFloorDepth = 0.86f
private const val FlowFloorStart = 0.46f
private const val FlowArtistAlpha = 0.74f
private const val FlowAmbientPlaying = 0.24f
private const val FlowAmbientPaused = 0.12f
private const val FlowAmbientCenter = 0.3f
private const val FlowAmbientReach = 0.95f
private const val FlowTrackShiftScale = 0.045f

internal const val FlowLeafMajor = 0.34f
internal const val FlowLeafMinor = 0.09f
internal const val FlowRestCorner = 0.19f

internal data class FlowLeafShape(
    val major: Float,
    val minor: Float,
    val mirrored: Boolean = false
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val side = size.minDimension
        val big = (side * major).coerceIn(0f, side / 2)
        val small = (side * minor).coerceIn(0f, side / 2)
        val startLeads = if (mirrored) layoutDirection == LayoutDirection.Rtl else layoutDirection == LayoutDirection.Ltr
        val topLeft = if (startLeads) big else small
        val topRight = if (startLeads) small else big
        return Outline.Rounded(
            RoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                topLeftCornerRadius = CornerRadius(topLeft),
                topRightCornerRadius = CornerRadius(topRight),
                bottomRightCornerRadius = CornerRadius(topLeft),
                bottomLeftCornerRadius = CornerRadius(topRight)
            )
        )
    }
}

internal fun flowCoverShape(playProgress: Float): FlowLeafShape = FlowLeafShape(
    major = lerp(FlowRestCorner, FlowLeafMajor, playProgress),
    minor = lerp(FlowRestCorner, FlowLeafMinor, playProgress)
)

internal fun flowLandingShape(fraction: Float, startCorner: Float, side: Float, playing: Boolean): FlowLeafShape {
    val progress = if (playing) 1f else 0f
    val start = if (side > 0f) (startCorner / side).coerceIn(0f, 0.5f) else 0f
    return FlowLeafShape(
        major = lerp(start, lerp(FlowRestCorner, FlowLeafMajor, progress), fraction),
        minor = lerp(start, lerp(FlowRestCorner, FlowLeafMinor, progress), fraction)
    )
}

@Composable
internal fun PlayerFlowDeck(
    track: Track,
    slots: PlayerDeckSlots,
    surfaces: PlayerSurfaceTokens,
    isFavorite: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
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
    modifier: Modifier = Modifier,
    headlineModifier: Modifier = Modifier
) {
    val metrics = flowDeckMetrics(compact)
    val glow by animateColorAsState(
        targetValue = surfaces.glow.copy(alpha = 1f),
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-flow-glow"
    )
    val playProgress = animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = if (animated) {
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        } else {
            snap()
        },
        label = "player-flow-play"
    )
    val floor = if (surfaces.amoled) Color.Black else glow.playerMix(PlayerDarkSurface, FlowFloorDepth)
    Column(
        modifier = modifier
            .drawBehind {
                val ambient = lerp(FlowAmbientPaused, FlowAmbientPlaying, playProgress.value) *
                    if (surfaces.amoled) FlowLightAmoledFactor else 1f
                val ambientRadius = size.width * FlowAmbientReach
                val ambientCenter = Offset(size.width / 2f, size.height * FlowAmbientCenter)
                drawRect(
                    Brush.radialGradient(
                        0f to glow.copy(alpha = ambient),
                        0.6f to glow.copy(alpha = ambient * 0.3f),
                        1f to Color.Transparent,
                        center = ambientCenter,
                        radius = ambientRadius
                    )
                )
                drawRect(
                    Brush.verticalGradient(
                        FlowFloorStart to Color.Transparent,
                        1f to floor,
                        startY = 0f,
                        endY = size.height
                    )
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(
                start = gutter,
                end = gutter,
                top = LevyraPlayerDesign.SpaceXs,
                bottom = metrics.bottom
            )
    ) {
        slots.header()
        FlowStage(
            track = track,
            slots = slots,
            glow = glow,
            amoled = surfaces.amoled,
            playProgress = playProgress,
            animated = animated,
            artworkCap = artworkCap,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (scrollable) Modifier.height(FlowScrollingStage) else Modifier.weight(1f))
                .padding(vertical = metrics.stagePadding)
        )
        FlowHeadline(
            track = track,
            surfaces = surfaces,
            isFavorite = isFavorite,
            animated = animated,
            compact = compact,
            stepDirection = stepDirection,
            onArtistClick = onArtistClick,
            onToggleFavorite = onToggleFavorite,
            modifier = headlineModifier
        )
        Spacer(Modifier.height(metrics.headlineGap))
        slots.engagement(track)
        slots.progress(PlayerProgressStyle.Expressive)
        Spacer(Modifier.height(metrics.progressGap))
        FlowTransport(
            isPlaying = isPlaying,
            isResolving = isResolving,
            playProgress = playProgress,
            labels = labels,
            metrics = metrics,
            surfaces = surfaces,
            animated = animated,
            onPrevious = onPrevious,
            onTogglePlay = onTogglePlay,
            onNext = onNext
        )
        Spacer(Modifier.height(metrics.actionGap))
        FlowActionRow(
            track = track,
            slots = slots,
            surfaces = surfaces,
            shuffleOn = shuffleOn,
            repeatMode = repeatMode,
            labels = labels,
            animated = animated,
            onShuffle = onShuffle,
            onRepeat = onRepeat
        )
    }
}

@Composable
private fun FlowStage(
    track: Track,
    slots: PlayerDeckSlots,
    glow: Color,
    amoled: Boolean,
    playProgress: State<Float>,
    animated: Boolean,
    artworkCap: Dp,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val trackShift = remember { Animatable(0f) }
    var shownTrackId by remember { mutableStateOf(track.id) }
    LaunchedEffect(track.id) {
        if (track.id == shownTrackId) return@LaunchedEffect
        shownTrackId = track.id
        if (!animated) return@LaunchedEffect
        trackShift.snapTo(1f)
        trackShift.animateTo(0f, LevyraMotion.spatial.spec())
    }

    val pressScale = animateFloatAsState(
        targetValue = if (pressed && animated) FlowCoverPressScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "player-flow-press"
    )
    val lightScale = if (amoled) FlowLightAmoledFactor else 1f
    BoxWithConstraints(
        modifier = modifier
            .drawBehind {
                val light = lerp(FlowLightPaused, FlowLightPlaying, playProgress.value) * lightScale
                val radius = size.minDimension * FlowLightReach
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to glow.copy(alpha = light),
                        0.55f to glow.copy(alpha = light * 0.36f),
                        1f to Color.Transparent,
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    pressed = true
                    waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val side = minOf(maxWidth, maxHeight, artworkCap)
        slots.stage(
            track,
            Modifier
                .size(side)
                .graphicsLayer {
                    val progress = playProgress.value
                    val shift = trackShift.value
                    val scale = pressScale.value * (1f - FlowTrackShiftScale * shift)
                    scaleX = scale
                    scaleY = scale
                    shape = flowCoverShape(progress)
                    clip = true
                    shadowElevation = FlowCoverShadow.toPx() * lerp(0.45f, 1f, progress)
                    ambientShadowColor = glow
                    spotShadowColor = glow
                },
            FlowStageCorner
        )
        slots.gestures(track, Modifier.matchParentSize())
    }
}

private class FlowDeckMetrics(
    val playSize: Dp,
    val sideSize: Dp,
    val bottom: Dp,
    val stagePadding: Dp,
    val headlineGap: Dp,
    val progressGap: Dp,
    val actionGap: Dp
)

private val FlowRegularMetrics = FlowDeckMetrics(
    playSize = 88.dp,
    sideSize = 64.dp,
    bottom = LevyraPlayerDesign.SpaceMd,
    stagePadding = LevyraPlayerDesign.SpaceMd,
    headlineGap = LevyraPlayerDesign.SpaceMd,
    progressGap = LevyraPlayerDesign.SpaceSm,
    actionGap = LevyraPlayerDesign.SpaceLg
)

private val FlowCompactMetrics = FlowDeckMetrics(
    playSize = 74.dp,
    sideSize = 56.dp,
    bottom = LevyraPlayerDesign.SpaceSm,
    stagePadding = LevyraPlayerDesign.SpaceSm,
    headlineGap = LevyraPlayerDesign.SpaceSm,
    progressGap = LevyraPlayerDesign.SpaceXs,
    actionGap = LevyraPlayerDesign.SpaceXs
)

private fun flowDeckMetrics(compact: Boolean): FlowDeckMetrics =
    if (compact) FlowCompactMetrics else FlowRegularMetrics

@Composable
private fun FlowHeadline(
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
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        AnimatedContent(
            targetState = track,
            modifier = Modifier.weight(1f),
            transitionSpec = { LevyraMotion.trackChange(animated, stepDirection) },
            contentKey = { it.id },
            label = "player-flow-headline"
        ) { shown ->
            Column(modifier = Modifier.fillMaxWidth()) {
                val eyebrow = playerDeckAlbumEyebrow(shown)
                if (eyebrow.isNotEmpty()) {
                    Text(
                        text = eyebrow,
                        color = surfaces.contentFaint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(LevyraPlayerDesign.SpaceXs))
                }
                val titleSize = flowTitleSize(shown.title, compact)
                Text(
                    text = shown.title,
                    color = surfaces.content,
                    fontSize = titleSize,
                    lineHeight = titleSize * 1.1f,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.8).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(LevyraPlayerDesign.SpaceXs))
                PlayerArtistText(
                    track = shown,
                    color = surfaces.content.copy(alpha = FlowArtistAlpha),
                    style = TextStyle(
                        fontSize = if (compact) 14.sp else 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    onClickLabel = strings.openArtist,
                    onArtistClick = onArtistClick,
                    enabled = shown.id == track.id
                )
            }
        }
        PlayerFavoriteButton(
            trackId = track.id,
            isFavorite = isFavorite,
            surfaces = surfaces,
            label = strings.favoritesPlain,
            animated = animated,
            onToggle = onToggleFavorite
        )
    }
}

internal fun flowTitleSize(title: String, compact: Boolean): TextUnit {
    val length = title.trim().length
    val base = when {
        length <= 14 -> 34f
        length <= 26 -> 30f
        length <= 40 -> 26f
        else -> 23f
    }
    return (if (compact) base - 4f else base).sp
}

@Composable
private fun FlowTransport(
    isPlaying: Boolean,
    isResolving: Boolean,
    playProgress: State<Float>,
    labels: PlayerControlLabels,
    metrics: FlowDeckMetrics,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit
) {
    val hero by animateColorAsState(
        targetValue = surfaces.hero,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-flow-hero"
    )
    val heroContent by animateColorAsState(
        targetValue = surfaces.heroContent,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-flow-hero-content"
    )
    val previousInteraction = remember { MutableInteractionSource() }
    val playInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val previousPress = flowPressProgress(previousInteraction, animated)
    val playPress = flowPressProgress(playInteraction, animated)
    val nextPress = flowPressProgress(nextInteraction, animated)
    val height = metrics.playSize
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.spacedBy(FlowTransportGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlowTransportSegment(
            weight = flowSegmentWeight(FlowSideWeight, previousPress.value, playPress.value + nextPress.value),
            press = previousPress,
            interaction = previousInteraction,
            container = surfaces.control,
            label = labels.previous,
            onClick = onPrevious,
            shape = { pressed, half ->
                FlowSegmentShape(start = half, end = lerp(FlowInnerCorner.toPx(), half, pressed * FlowPressedRound))
            }
        ) {
            PlayerIcon(Icons.Rounded.SkipPrevious, surfaces.content, Modifier.size(height * FlowSideGlyph))
        }
        FlowTransportSegment(
            weight = flowSegmentWeight(FlowPlayWeight, playPress.value, previousPress.value + nextPress.value),
            press = playPress,
            interaction = playInteraction,
            container = hero,
            label = if (isPlaying) labels.pause else labels.play,
            onClick = onTogglePlay,
            shape = { pressed, half ->
                val rest = lerp(half, FlowPlayingCorner.toPx(), playProgress.value)
                val corner = lerp(rest, half, pressed * FlowPressedRound)
                FlowSegmentShape(start = corner, end = corner)
            }
        ) {
            if (isResolving) {
                LevyraLoadingIndicator(color = heroContent, modifier = Modifier.size(height * 0.44f))
            } else {
                LevyraPlayPauseGlyph(
                    playing = isPlaying,
                    color = heroContent,
                    modifier = Modifier.size(height * FlowPlayGlyph),
                    animateMorph = animated
                )
            }
        }
        FlowTransportSegment(
            weight = flowSegmentWeight(FlowSideWeight, nextPress.value, playPress.value + previousPress.value),
            press = nextPress,
            interaction = nextInteraction,
            container = surfaces.control,
            label = labels.next,
            onClick = onNext,
            shape = { pressed, half ->
                FlowSegmentShape(start = lerp(FlowInnerCorner.toPx(), half, pressed * FlowPressedRound), end = half)
            }
        ) {
            PlayerIcon(Icons.Rounded.SkipNext, surfaces.content, Modifier.size(height * FlowSideGlyph))
        }
    }
}

private fun flowSegmentWeight(base: Float, ownPress: Float, otherPress: Float): Float =
    base * (1f + FlowPressGrow * ownPress - FlowPressShrink * otherPress.coerceIn(0f, 1f))

@Composable
private fun RowScope.FlowTransportSegment(
    weight: Float,
    press: State<Float>,
    interaction: MutableInteractionSource,
    container: Color,
    label: String,
    onClick: () -> Unit,
    shape: Density.(pressed: Float, half: Float) -> Shape,
    content: @Composable () -> Unit
) {
    val haptics = LocalLevyraHaptics.current
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .graphicsLayer {
                this.shape = shape(press.value, size.height / 2)
                clip = true
            }
            .background(container)
            .levyraPressable(
                onClick = {
                    haptics.perform(LevyraHapticAction.Transport)
                    onClick()
                },
                interactionSource = interaction,
                pressedScale = LevyraPressScale.Control,
                role = Role.Button
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private class FlowSegmentShape(private val start: Float, private val end: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val half = size.height / 2
        val leading = start.coerceIn(0f, half)
        val trailing = end.coerceIn(0f, half)
        val left = if (layoutDirection == LayoutDirection.Ltr) leading else trailing
        val right = if (layoutDirection == LayoutDirection.Ltr) trailing else leading
        return Outline.Rounded(
            RoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                topLeftCornerRadius = CornerRadius(left),
                bottomLeftCornerRadius = CornerRadius(left),
                topRightCornerRadius = CornerRadius(right),
                bottomRightCornerRadius = CornerRadius(right)
            )
        )
    }
}

private val FlowTransportGap = 6.dp
private val FlowInnerCorner = 14.dp
private val FlowPlayingCorner = 30.dp
private const val FlowSideWeight = 1f
private const val FlowPlayWeight = 1.55f
private const val FlowPressGrow = 0.16f
private const val FlowPressShrink = 0.06f
private const val FlowPressedRound = 0.35f
private const val FlowSideGlyph = 0.36f
private const val FlowPlayGlyph = 0.42f

@Composable
private fun flowPressProgress(interaction: MutableInteractionSource, animated: Boolean): State<Float> {
    val isPressed by interaction.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = if (animated) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        } else {
            snap()
        },
        label = "player-flow-control-press"
    )
}

@Composable
private fun FlowActionRow(
    track: Track,
    slots: PlayerDeckSlots,
    surfaces: PlayerSurfaceTokens,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    labels: PlayerControlLabels,
    animated: Boolean,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit
) {
    val dockActions = slots.dockActions(track)
    val addToPlaylist = slots.addToPlaylist
    val playlistLabel = LocalLevyraStrings.current.addToPlaylist
    val playlistIcon = PlayerIcons.addToPlaylist
    val modeActions = remember(shuffleOn, repeatMode, labels, onShuffle, onRepeat) {
        listOf(
            PlayerDockAction(
                key = "shuffle",
                icon = Icons.Rounded.Shuffle,
                label = labels.shuffle,
                active = shuffleOn,
                toggle = true,
                haptic = LevyraHapticAction.Confirm,
                onClick = onShuffle
            ),
            PlayerDockAction(
                key = "repeat",
                icon = if (repeatMode == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                label = labels.repeat,
                active = repeatMode != RepeatMode.Off,
                toggle = true,
                haptic = LevyraHapticAction.Confirm,
                onClick = onRepeat
            )
        )
    }
    val collectionActions = remember(dockActions, track.id, addToPlaylist, playlistLabel, playlistIcon) {
        val queue = dockActions.filter { it.key == FlowQueueKey }
            .map { it.copy(haptic = LevyraHapticAction.Transport) }
        val playlist = if (addToPlaylist != null && !track.isLiveRadio()) {
            listOf(
                PlayerDockAction(
                    key = FlowPlaylistKey,
                    icon = playlistIcon,
                    label = playlistLabel,
                    haptic = LevyraHapticAction.Confirm,
                    onClick = { addToPlaylist(track) }
                )
            )
        } else {
            emptyList()
        }
        queue + playlist + dockActions.filter { it.key != FlowQueueKey }
            .map { if (it.key == FlowVisualModeKey) it.copy(active = false) else it }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val roomy = flowToolbarFits(maxWidth, modeActions.size + collectionActions.size)
        val shownCollection = if (roomy) collectionActions else collectionActions.filter { it.key != FlowPlaylistKey }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(FlowToolbarHeight)
                .clip(RoundedCornerShape(FlowToolbarHeight / 2))
                .background(surfaces.tonal)
                .padding(horizontal = FlowToolbarInset),
            verticalAlignment = Alignment.CenterVertically
        ) {
            modeActions.forEach { action ->
                key(action.key) {
                    FlowToolbarItem(action, surfaces, animated)
                }
            }
            if (shownCollection.isNotEmpty()) {
                Box(
                    Modifier
                        .padding(horizontal = FlowToolbarInset)
                        .size(width = 1.dp, height = FlowToolbarDivider)
                        .background(surfaces.content.copy(alpha = FlowToolbarDividerAlpha))
                )
            }
            shownCollection.forEach { action ->
                key(action.key) {
                    FlowToolbarItem(action, surfaces, animated)
                }
            }
        }
    }
}

internal fun flowToolbarFits(width: Dp, actions: Int): Boolean =
    width - FlowToolbarChrome >= LevyraPlayerDesign.MinimumTouchTarget * actions

@Composable
private fun RowScope.FlowToolbarItem(
    action: PlayerDockAction,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean
) {
    val interaction = remember { MutableInteractionSource() }
    val fill by animateColorAsState(
        targetValue = if (action.active) surfaces.hero else Color.Transparent,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(220)),
        label = "player-flow-toolbar-fill"
    )
    val tint by animateColorAsState(
        targetValue = if (action.active) surfaces.heroContent else surfaces.content,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(220)),
        label = "player-flow-toolbar-tint"
    )
    val corner = levyraExpressiveToggleCorner(
        checked = action.active,
        unchecked = FlowToolbarIndicator / 2,
        checkedCorner = FlowToolbarActiveCorner,
        label = "player-flow-toolbar-corner"
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .graphicsLayer { alpha = if (action.enabled) 1f else FlowToolbarDisabledAlpha }
            .levyraPressable(
                onClick = { if (!action.busy) action.onClick() },
                enabled = action.enabled,
                interactionSource = interaction,
                pressedScale = LevyraPressScale.Control,
                role = Role.Button,
                haptic = action.haptic
            )
            .semantics {
                contentDescription = action.label
                if (action.toggle) toggleableState = ToggleableState(action.active)
                action.stateDescription?.let { stateDescription = it }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(FlowToolbarIndicator)
                .clip(RoundedCornerShape(corner))
                .background(fill),
            contentAlignment = Alignment.Center
        ) {
            PlayerSegmentGlyph(action.icon, tint, action.busy, LevyraPlayerDesign.DockGlyph)
        }
    }
}

private const val FlowQueueKey = "queue"
private const val FlowVisualModeKey = "visual-mode"
private const val FlowPlaylistKey = "playlist"
private val FlowToolbarHeight = 56.dp
private val FlowToolbarInset = 6.dp
private val FlowToolbarIndicator = 46.dp
private val FlowToolbarActiveCorner = 14.dp
private const val FlowToolbarDisabledAlpha = 0.42f
private val FlowToolbarDivider = 22.dp
private const val FlowToolbarDividerAlpha = 0.16f
private val FlowToolbarChrome = FlowToolbarInset * 4 + 1.dp

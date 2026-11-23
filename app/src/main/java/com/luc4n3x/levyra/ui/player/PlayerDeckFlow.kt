package com.luc4n3x.levyra.ui.player

import androidx.compose.runtime.getValue
import com.luc4n3x.levyra.ui.components.PlayerGlassIconButton
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.key
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
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
            onAddToPlaylist = slots.addToPlaylist?.takeIf { !track.isLiveRadio() }?.let { add -> { add(track) } },
            modifier = headlineModifier
        )
        Spacer(Modifier.height(metrics.headlineGap))
        slots.engagement(track)
        slots.progress(PlayerProgressStyle.Expressive)
        Spacer(Modifier.height(metrics.progressGap))
        FlowTransport(
            isPlaying = isPlaying,
            isResolving = isResolving,
            labels = labels,
            metrics = metrics,
            surfaces = surfaces,
            animated = animated,
            shuffleOn = shuffleOn,
            repeatMode = repeatMode,
            onShuffle = onShuffle,
            onRepeat = onRepeat,
            onPrevious = onPrevious,
            onTogglePlay = onTogglePlay,
            onNext = onNext
        )
        Spacer(Modifier.height(metrics.actionGap))
        FlowActionRow(
            track = track,
            slots = slots,
            surfaces = surfaces,
            animated = animated
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
    onAddToPlaylist: (() -> Unit)?,
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
        if (onAddToPlaylist != null) {
            val haptics = LocalLevyraHaptics.current
            val outline = surfaces.segmentOutline
            PlayerGlassIconButton(
                icon = PlayerIcons.addToPlaylist,
                contentDescription = strings.addToPlaylist,
                size = 44.dp,
                iconSize = 23.dp,
                tint = surfaces.content,
                fill = surfaces.fillFor(false),
                borderTop = outline,
                borderBottom = outline,
                shape = RoundedCornerShape(50),
                modifier = Modifier.size(LevyraPlayerDesign.MinimumTouchTarget),
                onClick = {
                    haptics.perform(LevyraHapticAction.Confirm)
                    onAddToPlaylist()
                }
            )
            Spacer(Modifier.size(FlowHeadlineActionGap))
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

private val FlowHeadlineActionGap = 4.dp

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
    labels: PlayerControlLabels,
    metrics: FlowDeckMetrics,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
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
    val playSize = metrics.playSize
    val sideSize = playSize * FlowSideScale
    val playInteraction = remember { MutableInteractionSource() }
    val playCorner by animateFloatAsState(
        targetValue = if (isPlaying) FlowPlayPlayingCorner else FlowPlayPausedCorner,
        animationSpec = if (animated) spring(dampingRatio = FlowPlayCornerDamping, stiffness = FlowPlayCornerStiffness) else snap(),
        label = "player-flow-play-corner"
    )
    val haptics = LocalLevyraHaptics.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(playSize),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlowModeToggle(
            icon = Icons.Rounded.Shuffle,
            label = labels.shuffle,
            active = shuffleOn,
            surfaces = surfaces,
            animated = animated,
            onClick = onShuffle
        )
        FlowRoundControl(
            size = sideSize,
            container = surfaces.control,
            label = labels.previous,
            animated = animated,
            onClick = onPrevious
        ) {
            PlayerIcon(Icons.Rounded.SkipPrevious, surfaces.content, Modifier.size(sideSize * FlowSideGlyph))
        }
        Box(
            modifier = Modifier
                .size(playSize)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(hero.copy(alpha = FlowPlayHaloAlpha), Color.Transparent),
                            center = center,
                            radius = size.minDimension * FlowPlayHaloReach
                        ),
                        radius = size.minDimension * FlowPlayHaloReach
                    )
                }
                .levyraPressable(
                    onClick = {
                        haptics.perform(LevyraHapticAction.Transport)
                        onTogglePlay()
                    },
                    interactionSource = playInteraction,
                    pressedScale = LevyraPressScale.Control,
                    role = Role.Button
                )
                .semantics { contentDescription = if (isPlaying) labels.pause else labels.play },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        shape = RoundedCornerShape(playCorner.coerceIn(0f, 50f).toInt())
                        clip = true
                    }
                    .background(hero)
            )
            if (isResolving) {
                LevyraLoadingIndicator(color = heroContent, modifier = Modifier.size(playSize * 0.44f))
            } else {
                LevyraPlayPauseGlyph(
                    playing = isPlaying,
                    color = heroContent,
                    modifier = Modifier.size(playSize * FlowPlayGlyph),
                    animateMorph = animated
                )
            }
        }
        FlowRoundControl(
            size = sideSize,
            container = surfaces.control,
            label = labels.next,
            animated = animated,
            onClick = onNext
        ) {
            PlayerIcon(Icons.Rounded.SkipNext, surfaces.content, Modifier.size(sideSize * FlowSideGlyph))
        }
        FlowModeToggle(
            icon = if (repeatMode == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            label = labels.repeat,
            active = repeatMode != RepeatMode.Off,
            surfaces = surfaces,
            animated = animated,
            onClick = onRepeat
        )
    }
}

@Composable
private fun FlowModeToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val fill by animateColorAsState(
        targetValue = if (active) surfaces.hero.copy(alpha = FlowModeActiveAlpha) else Color.Transparent,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(220)),
        label = "player-flow-mode-fill"
    )
    val tint by animateColorAsState(
        targetValue = if (active) surfaces.hero else surfaces.content.copy(alpha = FlowModeIdleAlpha),
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(220)),
        label = "player-flow-mode-tint"
    )
    Box(
        modifier = Modifier
            .size(FlowModeSize)
            .clip(RoundedCornerShape(50))
            .background(fill)
            .levyraPressable(
                onClick = onClick,
                interactionSource = interaction,
                pressedScale = LevyraPressScale.Control,
                role = Role.Button,
                haptic = LevyraHapticAction.Confirm
            )
            .semantics {
                contentDescription = label
                toggleableState = ToggleableState(active)
            },
        contentAlignment = Alignment.Center
    ) {
        PlayerIcon(icon, tint, Modifier.size(FlowModeGlyph))
    }
}

@Composable
private fun FlowRoundControl(
    size: Dp,
    container: Color,
    label: String,
    animated: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val haptics = LocalLevyraHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val press = flowPressProgress(interaction, animated)
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                shape = RoundedCornerShape(lerp(FlowRoundRest, FlowRoundPressed, press.value).coerceIn(0f, 50f).toInt())
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

private const val FlowSideScale = 0.68f
private const val FlowPlayHaloAlpha = 0.18f
private const val FlowPlayHaloReach = 0.74f
private const val FlowPlayPausedCorner = 50f
private const val FlowPlayPlayingCorner = 32f
private const val FlowPlayCornerDamping = 0.82f
private const val FlowPlayCornerStiffness = 420f
private val FlowModeSize = 44.dp
private val FlowModeGlyph = 22.dp
private const val FlowModeActiveAlpha = 0.18f
private const val FlowModeIdleAlpha = 0.72f
private const val FlowRoundRest = 50f
private const val FlowRoundPressed = 30f
private const val FlowSideGlyph = 0.42f
private const val FlowPlayGlyph = 0.4f

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
    animated: Boolean
) {
    val dockActions = slots.dockActions(track)
    val primary = remember(dockActions) {
        dockActions.filter { it.key == FlowLyricsKey || it.key == FlowQueueKey }
            .map { if (it.key == FlowQueueKey) it.copy(haptic = LevyraHapticAction.Transport) else it }
    }
    val extras = remember(dockActions) {
        dockActions.filter { it.key != FlowLyricsKey && it.key != FlowQueueKey }
            .map { if (it.key == FlowVisualModeKey) it.copy(active = false) else it }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FlowToolbarHeight),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        (primary + extras).forEach { action ->
            key(action.key) {
                FlowIconAction(action, surfaces, animated)
            }
        }
    }
}

@Composable
private fun FlowIconAction(
    action: PlayerDockAction,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean
) {
    val interaction = remember { MutableInteractionSource() }
    val fill by animateColorAsState(
        targetValue = if (action.active) surfaces.hero else surfaces.tonal,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(220)),
        label = "player-flow-icon-fill"
    )
    val tint by animateColorAsState(
        targetValue = if (action.active) surfaces.heroContent else surfaces.content,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(220)),
        label = "player-flow-icon-tint"
    )
    Box(
        modifier = Modifier
            .size(FlowToolbarHeight)
            .clip(RoundedCornerShape(50))
            .background(fill)
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
        PlayerSegmentGlyph(action.icon, tint, action.busy, FlowPillGlyph)
    }
}

private const val FlowQueueKey = "queue"
private const val FlowLyricsKey = "lyrics"
private val FlowPillGlyph = 22.dp
private const val FlowVisualModeKey = "visual-mode"
private val FlowToolbarHeight = 56.dp
private const val FlowToolbarDisabledAlpha = 0.42f

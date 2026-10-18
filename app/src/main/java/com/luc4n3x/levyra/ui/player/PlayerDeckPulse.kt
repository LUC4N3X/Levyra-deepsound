@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Forward5
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Replay30
import androidx.compose.material.icons.rounded.Replay5
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.PlaybackService
import com.luc4n3x.levyra.player.VisualizerAudioProcessor
import com.luc4n3x.levyra.ui.PlayerDarkSurface
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayPauseGlyph
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.playerMix
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import kotlin.math.abs

private val NightScrollingStage = 320.dp
private const val NightDomePlaying = 1f
private const val NightDomeResting = 0.58f
private const val NightFootCorner = 0.07f
private const val NightScrimStart = 0.42f
private const val NightScrimAlpha = 0.86f
private const val NightFloorDepth = 0.9f
private const val NightSkyAlphaPlaying = 0.34f
private const val NightSkyAlphaResting = 0.16f
private const val NightSkyAmoledFactor = 0.5f
private const val NightHaloGain = 3f
private val NightHaloGap = 10.dp
private val NightHaloStroke = 2.dp
private const val NightHaloIdleAlpha = 0.22f
private const val NightHaloEnergyAlpha = 0.6f
private const val NightPillCorner = 50
private const val NightPressedCorner = 28
private const val NightPausedPlayCorner = 34
private const val NightPlayWidePlaying = 1.62f
private const val NightPlayWideResting = 1.3f
private val NightChipHeight = LevyraPlayerDesign.MinimumTouchTarget
private val NightTileHeight = 64.dp
private val NightSpecStyle = TextStyle(fontFeatureSettings = "tnum, case")
private val NightFavoriteTint = Color(0xFFFF6B8B)

private const val NightActionLyrics = "lyrics"
private const val NightActionQueue = "queue"
private const val NightActionDeck = "visual-mode"

private data class NightDeckMotion(
    val sky: Color,
    val playProgress: State<Float>,
    val floor: Color,
    val skyScale: Float
)

private data class NightDeckSpacing(
    val bottomPadding: Dp,
    val stagePadding: Dp,
    val progressSpacing: Dp,
    val transportSpacing: Dp
)

@Composable
private fun rememberNightDeckMotion(
    accent: Color,
    surfaces: PlayerSurfaceTokens,
    isPlaying: Boolean,
    animated: Boolean
): NightDeckMotion {
    val sky by animateColorAsState(
        targetValue = accent,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-night-sky"
    )
    val playProgress = animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = if (animated) {
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        } else {
            snap()
        },
        label = "player-night-play"
    )
    return NightDeckMotion(
        sky = sky,
        playProgress = playProgress,
        floor = if (surfaces.amoled) Color.Black else sky.playerMix(PlayerDarkSurface, NightFloorDepth),
        skyScale = if (surfaces.amoled) NightSkyAmoledFactor else 1f
    )
}

private fun nightDeckSpacing(compact: Boolean): NightDeckSpacing = if (compact) {
    NightDeckSpacing(
        bottomPadding = LevyraPlayerDesign.SpaceSm,
        stagePadding = LevyraPlayerDesign.SpaceXs,
        progressSpacing = LevyraPlayerDesign.SpaceXs,
        transportSpacing = LevyraPlayerDesign.SpaceSm
    )
} else {
    NightDeckSpacing(
        bottomPadding = LevyraPlayerDesign.SpaceMd,
        stagePadding = LevyraPlayerDesign.SpaceSm,
        progressSpacing = LevyraPlayerDesign.SpaceSm,
        transportSpacing = LevyraPlayerDesign.SpaceMd
    )
}

@Composable
private fun Modifier.nightDeckContainer(
    motion: NightDeckMotion,
    scrollable: Boolean,
    gutter: Dp,
    bottomPadding: Dp
): Modifier = this
    .drawBehind {
        val light = lerp(NightSkyAlphaResting, NightSkyAlphaPlaying, motion.playProgress.value) * motion.skyScale
        val radius = size.width * 0.9f
        val center = Offset(size.width / 2f, size.height * 0.24f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to motion.sky.copy(alpha = light),
                0.6f to motion.sky.copy(alpha = light * 0.3f),
                1f to Color.Transparent,
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        drawRect(
            Brush.verticalGradient(
                0.5f to Color.Transparent,
                1f to motion.floor,
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
        bottom = bottomPadding
    )

private fun ColumnScope.nightStageModifier(scrollable: Boolean, verticalPadding: Dp): Modifier = Modifier
    .fillMaxWidth()
    .then(if (scrollable) Modifier.height(NightScrollingStage) else Modifier.weight(1f))
    .padding(vertical = verticalPadding)

private fun Modifier.nightHorizontalBleed(gutter: Dp): Modifier = layout { measurable, constraints ->
    val gutterPx = gutter.roundToPx()
    val width = constraints.maxWidth + gutterPx * 2
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-gutterPx, 0) }
}

@Composable
internal fun PlayerPulseDeck(
    track: Track,
    slots: PlayerDeckSlots,
    surfaces: PlayerSurfaceTokens,
    accent: Color,
    isFavorite: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    labels: PlayerControlLabels,
    queuePosition: PlayerDeckQueuePosition?,
    seekStepMs: Long,
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
    onSeekBy: (Long) -> Unit,
    modifier: Modifier = Modifier,
    headlineModifier: Modifier = Modifier
) {
    val motion = rememberNightDeckMotion(accent, surfaces, isPlaying, animated)
    val spacing = nightDeckSpacing(compact)
    Column(
        modifier = modifier.nightDeckContainer(motion, scrollable, gutter, spacing.bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        slots.header()
        NightStage(
            track = track,
            slots = slots,
            sky = motion.sky,
            playProgress = motion.playProgress,
            playing = isPlaying,
            animated = animated,
            compact = compact,
            artworkCap = artworkCap,
            stepDirection = stepDirection,
            isFavorite = isFavorite,
            onArtistClick = onArtistClick,
            onToggleFavorite = onToggleFavorite,
            headlineModifier = headlineModifier,
            modifier = nightStageModifier(scrollable, spacing.stagePadding)
        )
        slots.engagement(track)
        slots.progress(PlayerProgressStyle.Wavy)
        Spacer(Modifier.height(spacing.progressSpacing))
        NightTransport(
            isPlaying = isPlaying,
            isResolving = isResolving,
            playProgress = motion.playProgress,
            labels = labels,
            seekStepMs = seekStepMs,
            surfaces = surfaces,
            compact = compact,
            animated = animated,
            gutter = gutter,
            onPrevious = onPrevious,
            onTogglePlay = onTogglePlay,
            onNext = onNext,
            onSeekBy = onSeekBy
        )
        Spacer(Modifier.height(spacing.transportSpacing))
        NightActions(
            track = track,
            slots = slots,
            surfaces = surfaces,
            shuffleOn = shuffleOn,
            repeatMode = repeatMode,
            labels = labels,
            queuePosition = queuePosition,
            compact = compact,
            animated = animated,
            gutter = gutter,
            onShuffle = onShuffle,
            onRepeat = onRepeat
        )
    }
}

private class NightArchShape(private val dome: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val side = size.minDimension
        val top = (side / 2f * dome).coerceIn(0f, side / 2f)
        val foot = side * NightFootCorner
        return Outline.Rounded(
            RoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                topLeftCornerRadius = CornerRadius(top),
                topRightCornerRadius = CornerRadius(top),
                bottomRightCornerRadius = CornerRadius(foot),
                bottomLeftCornerRadius = CornerRadius(foot)
            )
        )
    }
}

private fun nightDome(playProgress: Float): Float = lerp(NightDomeResting, NightDomePlaying, playProgress)

@Composable
private fun NightStage(
    track: Track,
    slots: PlayerDeckSlots,
    sky: Color,
    playProgress: State<Float>,
    playing: Boolean,
    animated: Boolean,
    compact: Boolean,
    artworkCap: Dp,
    stepDirection: Int,
    isFavorite: Boolean,
    onArtistClick: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    headlineModifier: Modifier,
    modifier: Modifier = Modifier
) {
    val decorative = LocalLevyraVisualCapabilities.current.decorativeMotion
    val reactive = playing && animated && decorative
    val signal = VisualizerAudioProcessor.waveformState.collectAsStateWithLifecycle()
    val level = remember { NightSignalLevel() }
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight, artworkCap)
        Box(modifier = Modifier.size(side), contentAlignment = Alignment.BottomCenter) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val energy = level.follow(if (reactive) signal.value else EmptySignal)
                val dome = nightDome(playProgress.value)
                val radius = size.minDimension / 2f * dome + NightHaloGap.toPx()
                val alpha = (NightHaloIdleAlpha + NightHaloEnergyAlpha * energy) *
                    playProgress.value.coerceAtLeast(0.35f)
                val arcCenter = Offset(size.width / 2f, size.minDimension / 2f * dome)
                drawArc(
                    brush = Brush.verticalGradient(
                        0f to sky.copy(alpha = alpha),
                        0.55f to sky.copy(alpha = alpha * 0.35f),
                        1f to Color.Transparent,
                        startY = arcCenter.y - radius,
                        endY = arcCenter.y
                    ),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(arcCenter.x - radius, arcCenter.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = NightHaloStroke.toPx() * (1f + energy), cap = StrokeCap.Round)
                )
            }
            slots.stage(
                track,
                Modifier
                    .size(side)
                    .graphicsLayer {
                        shape = NightArchShape(nightDome(playProgress.value))
                        clip = true
                    },
                0.dp
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        shape = NightArchShape(nightDome(playProgress.value))
                        clip = true
                    }
                    .drawBehind {
                        drawRect(
                            Brush.verticalGradient(
                                NightScrimStart to Color.Transparent,
                                1f to Color.Black.copy(alpha = NightScrimAlpha),
                                startY = 0f,
                                endY = size.height
                            )
                        )
                    }
            )
            slots.gestures(track, Modifier.matchParentSize())
            NightHeadline(
                track = track,
                isFavorite = isFavorite,
                animated = animated,
                compact = compact,
                stepDirection = stepDirection,
                onArtistClick = onArtistClick,
                onToggleFavorite = onToggleFavorite,
                creditsEyebrow = slots.creditsEyebrow,
                modifier = headlineModifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = LevyraPlayerDesign.SpaceLg,
                        vertical = if (compact) LevyraPlayerDesign.SpaceMd else LevyraPlayerDesign.SpaceXl
                    )
            )
        }
    }
}

private val EmptySignal = FloatArray(0)

private class NightSignalLevel {
    private var smoothed = 0f

    fun follow(samples: FloatArray): Float {
        var peak = 0f
        for (sample in samples) peak = maxOf(peak, abs(sample))
        val target = (peak * NightHaloGain).coerceIn(0f, 1f)
        smoothed += (target - smoothed) * if (target > smoothed) 0.45f else 0.1f
        return smoothed
    }
}

@Composable
private fun NightHeadline(
    track: Track,
    isFavorite: Boolean,
    animated: Boolean,
    compact: Boolean,
    stepDirection: Int,
    onArtistClick: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    creditsEyebrow: PlayerCreditsEyebrow?,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    AnimatedContent(
        targetState = track,
        modifier = modifier,
        transitionSpec = { LevyraMotion.trackChange(animated, stepDirection) },
        contentKey = { it.id },
        label = "player-night-headline"
    ) { shown ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (creditsEyebrow != null && creditsEyebrow.trackId == shown.id) {
                NightCreditsEyebrow(creditsEyebrow)
                Spacer(Modifier.height(LevyraPlayerDesign.SpaceXs))
            }
            val titleSize = flowTitleSize(shown.title, compact)
            Text(
                text = shown.title,
                color = Color.White,
                fontSize = titleSize,
                lineHeight = titleSize * 1.08f,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(LevyraPlayerDesign.SpaceXs))
            PlayerArtistText(
                track = shown,
                color = Color.White.copy(alpha = 0.8f),
                style = TextStyle(
                    fontSize = if (compact) 15.sp else 17.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                ),
                onClickLabel = strings.openArtist,
                onArtistClick = onArtistClick,
                enabled = shown.id == track.id
            )
            Spacer(Modifier.height(LevyraPlayerDesign.SpaceSm))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXs)
            ) {
                NightSpecChip()
                NightFavoriteButton(
                    isFavorite = isFavorite,
                    label = strings.favoritesPlain,
                    animated = animated,
                    enabled = shown.id == track.id,
                    onToggle = onToggleFavorite
                )
            }
        }
    }
}

@Composable
private fun NightCreditsEyebrow(eyebrow: PlayerCreditsEyebrow) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val available = constraints.maxWidth - with(density) { (NightEyebrowPadding * 2).roundToPx() }
        val heading = eyebrow.heading.uppercase()
        val line = remember(heading, eyebrow.names, available) {
            playerCreditsLine(eyebrow.names) { names ->
                measurer.measure("$heading  ${names.uppercase()}", NightEyebrowStyle).size.width <= available
            }
        }
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White.copy(alpha = 0.56f))) { append(heading) }
                append("  ")
                withStyle(SpanStyle(color = Color.White.copy(alpha = 0.9f))) { append(line.uppercase()) }
            },
            style = NightEyebrowStyle,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .clip(CircleShape)
                .levyraPressable(onClick = eyebrow.onClick, role = Role.Button)
                .semantics {
                    contentDescription = "${eyebrow.contentDescription}: ${eyebrow.names.joinToString(", ")}"
                }
                .padding(horizontal = NightEyebrowPadding, vertical = 6.dp)
        )
    }
}

private val NightEyebrowPadding = 10.dp
private val NightEyebrowStyle = TextStyle(
    fontSize = 11.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 1.8.sp,
    shadow = Shadow(Color.Black.copy(alpha = 0.4f), blurRadius = 8f)
)

@Composable
private fun NightFavoriteButton(
    isFavorite: Boolean,
    label: String,
    animated: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (isFavorite) NightFavoriteTint else Color.White,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(200)),
        label = "player-night-favorite"
    )
    Box(
        modifier = Modifier
            .size(LevyraPlayerDesign.MinimumTouchTarget)
            .levyraPressable(
                onClick = onToggle,
                enabled = enabled,
                pressedScale = LevyraPressScale.Control,
                role = Role.Switch,
                haptic = LevyraHapticAction.Confirm
            )
            .semantics {
                contentDescription = label
                toggleableState = ToggleableState(isFavorite)
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            PlayerIcon(
                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                tint,
                Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun NightSpecChip() {
    val spec by rememberPlayerAudioSpec()
    val label = remember(spec) { playerAudioSpecLabels(spec).joinToString("  ·  ") }
    if (label.isEmpty()) return
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = LevyraPlayerDesign.SpaceMd, vertical = 6.dp)
            .heightIn(min = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
    ) {
        PlayerIcon(Icons.Rounded.GraphicEq, Color.White.copy(alpha = 0.92f), Modifier.size(14.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            style = NightSpecStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun nightPress(interaction: MutableInteractionSource, animated: Boolean): State<Float> {
    val pressed by interaction.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = if (animated) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        } else {
            snap()
        },
        label = "player-night-press"
    )
}

private fun replayIcon(seconds: Long): ImageVector = when (seconds) {
    5L -> Icons.Rounded.Replay5
    10L -> Icons.Rounded.Replay10
    30L -> Icons.Rounded.Replay30
    else -> Icons.Rounded.FastRewind
}

private fun forwardIcon(seconds: Long): ImageVector = when (seconds) {
    5L -> Icons.Rounded.Forward5
    10L -> Icons.Rounded.Forward10
    30L -> Icons.Rounded.Forward30
    else -> Icons.Rounded.FastForward
}

@Composable
private fun NightTransport(
    isPlaying: Boolean,
    isResolving: Boolean,
    playProgress: State<Float>,
    labels: PlayerControlLabels,
    seekStepMs: Long,
    surfaces: PlayerSurfaceTokens,
    compact: Boolean,
    animated: Boolean,
    gutter: Dp,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onSeekBy: (Long) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val hero by animateColorAsState(
        targetValue = surfaces.hero,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-night-hero"
    )
    val heroContent by animateColorAsState(
        targetValue = surfaces.heroContent,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-night-hero-content"
    )
    val height = if (compact) 52.dp else 60.dp
    val seconds = (seekStepMs / 1_000L).coerceAtLeast(1L)
    val playWidth by animateDpAsState(
        targetValue = height * if (isPlaying) NightPlayWidePlaying else NightPlayWideResting,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.expressiveSpring()),
        label = "player-night-play-width"
    )
    val scrollState = rememberScrollState()
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().nightHorizontalBleed(gutter),
        contentAlignment = Alignment.Center
    ) {
        val requiredWidth = height * 4.24f + playWidth +
            LevyraPlayerDesign.SpaceSm * 2 + LevyraPlayerDesign.SpaceXs * 4
        Row(
            modifier = Modifier
                .then(if (requiredWidth > maxWidth) Modifier.horizontalScroll(scrollState) else Modifier)
                .padding(horizontal = gutter)
                .clip(RoundedCornerShape(percent = NightPillCorner))
                .background(surfaces.controlQuiet)
                .padding(LevyraPlayerDesign.SpaceSm),
            horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NightButton(
                icon = Icons.Rounded.SkipPrevious,
                label = labels.previous,
                height = height,
                width = height * 1.12f,
                container = surfaces.active,
                content = surfaces.activeContent,
                animated = animated,
                onClick = onPrevious
            )
            NightButton(
                icon = replayIcon(seconds),
                label = "${strings.gestureActionSeek} -${seconds}s",
                height = height,
                width = height,
                container = Color.Transparent,
                content = surfaces.content,
                animated = animated,
                onClick = { onSeekBy(-seekStepMs) }
            )
            NightPlayButton(
                isPlaying = isPlaying,
                isResolving = isResolving,
                playProgress = playProgress,
                labels = labels,
                height = height,
                width = playWidth,
                container = hero,
                content = heroContent,
                animated = animated,
                onClick = onTogglePlay
            )
            NightButton(
                icon = forwardIcon(seconds),
                label = "${strings.gestureActionSeek} +${seconds}s",
                height = height,
                width = height,
                container = Color.Transparent,
                content = surfaces.content,
                animated = animated,
                onClick = { onSeekBy(seekStepMs) }
            )
            NightButton(
                icon = Icons.Rounded.SkipNext,
                label = labels.next,
                height = height,
                width = height * 1.12f,
                container = surfaces.active,
                content = surfaces.activeContent,
                animated = animated,
                onClick = onNext
            )
        }
    }
}

@Composable
private fun NightButton(
    icon: ImageVector,
    label: String,
    height: Dp,
    width: Dp,
    container: Color,
    content: Color,
    animated: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalLevyraHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val press = nightPress(interaction, animated)
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .graphicsLayer {
                val corner = lerp(NightPillCorner.toFloat(), NightPressedCorner.toFloat(), press.value)
                shape = RoundedCornerShape(percent = corner.toInt())
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
        PlayerIcon(icon, content, Modifier.size(height * 0.42f))
    }
}

@Composable
private fun NightPlayButton(
    isPlaying: Boolean,
    isResolving: Boolean,
    playProgress: State<Float>,
    labels: PlayerControlLabels,
    height: Dp,
    width: Dp,
    container: Color,
    content: Color,
    animated: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalLevyraHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val press = nightPress(interaction, animated)
    val label = if (isPlaying) labels.pause else labels.play
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .graphicsLayer {
                val resting = lerp(NightPausedPlayCorner.toFloat(), NightPillCorner.toFloat(), playProgress.value)
                val corner = lerp(resting, NightPressedCorner.toFloat(), press.value)
                shape = RoundedCornerShape(percent = corner.toInt())
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
        if (isResolving) {
            LevyraLoadingIndicator(color = content, modifier = Modifier.size(height * 0.5f))
        } else {
            LevyraPlayPauseGlyph(
                playing = isPlaying,
                color = content,
                modifier = Modifier.size(height * 0.44f),
                animateMorph = animated
            )
        }
    }
}

@Composable
private fun NightActions(
    track: Track,
    slots: PlayerDeckSlots,
    surfaces: PlayerSurfaceTokens,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    labels: PlayerControlLabels,
    queuePosition: PlayerDeckQueuePosition?,
    compact: Boolean,
    animated: Boolean,
    gutter: Dp,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit
) {
    val dockActions = slots.dockActions(track)
    val lyrics = dockActions.firstOrNull { it.key == NightActionLyrics }
    val tiles = if (compact) {
        emptyList()
    } else {
        dockActions.filter { it.key == NightActionQueue || it.key == NightActionDeck }
    }
    val extras = dockActions.filter { it.key != NightActionLyrics && it !in tiles }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().nightHorizontalBleed(gutter)) {
        val labelsFit = remember(labels.shuffle, labels.repeat, lyrics?.label, extras.size, maxWidth, gutter, density) {
            nightChipLabelsFit(
                labeled = listOfNotNull(labels.shuffle, lyrics?.label, labels.repeat),
                iconOnlyCount = extras.size,
                available = maxWidth - gutter * 2,
                measure = { textMeasurer.measure(it, NightChipLabelStyle).size.width },
                density = density
            )
        }
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .widthIn(min = maxWidth)
                .padding(horizontal = gutter),
            horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NightChip(
                icon = Icons.Rounded.Shuffle,
                label = labels.shuffle,
                showLabel = labelsFit,
                active = shuffleOn,
                toggle = true,
                surfaces = surfaces,
                animated = animated,
                onClick = onShuffle
            )
            lyrics?.let { action ->
                NightChip(
                    icon = action.icon,
                    label = action.label,
                    showLabel = true,
                    active = action.active,
                    toggle = action.toggle,
                    surfaces = surfaces,
                    animated = animated,
                    onClick = action.onClick,
                    enabled = action.enabled
                )
            }
            NightChip(
                icon = if (repeatMode == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                label = labels.repeat,
                showLabel = labelsFit,
                active = repeatMode != RepeatMode.Off,
                toggle = true,
                surfaces = surfaces,
                animated = animated,
                onClick = onRepeat
            )
            extras.forEach { action ->
                NightChip(
                    icon = action.icon,
                    label = action.label,
                    showLabel = false,
                    active = action.active,
                    toggle = action.toggle,
                    surfaces = surfaces,
                    animated = animated,
                    onClick = action.onClick,
                    enabled = action.enabled && !action.busy,
                    stateDescription = action.stateDescription
                )
            }
        }
    }
    if (tiles.isNotEmpty()) {
        Spacer(Modifier.height(LevyraPlayerDesign.SpaceMd))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
        ) {
            tiles.forEach { action ->
                val queueTile = action.key == NightActionQueue
                val title = if (queueTile) action.label else action.stateDescription ?: action.label
                val subtitle = if (queueTile) {
                    queuePosition?.let { "${it.indexLabel} / ${it.totalLabel}" }.orEmpty()
                } else {
                    action.label.takeIf { it != title }.orEmpty()
                }
                NightTile(
                    icon = action.icon,
                    title = title,
                    subtitle = subtitle,
                    surfaces = surfaces,
                    onClick = action.onClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NightChip(
    icon: ImageVector,
    label: String,
    showLabel: Boolean,
    active: Boolean,
    toggle: Boolean,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    stateDescription: String? = null
) {
    val container by animateColorAsState(
        targetValue = if (active) surfaces.active else surfaces.control,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(200)),
        label = "player-night-chip"
    )
    val content by animateColorAsState(
        targetValue = if (active) surfaces.activeContent else surfaces.content,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(200)),
        label = "player-night-chip-content"
    )
    Row(
        modifier = Modifier
            .heightIn(min = NightChipHeight)
            .clip(RoundedCornerShape(percent = NightPillCorner))
            .background(if (enabled) container else container.copy(alpha = container.alpha * 0.5f))
            .levyraPressable(
                onClick = onClick,
                enabled = enabled,
                pressedScale = LevyraPressScale.Control,
                role = if (toggle) Role.Switch else Role.Button,
                haptic = LevyraHapticAction.Transport
            )
            .semantics {
                contentDescription = label
                if (toggle) toggleableState = ToggleableState(active)
                if (stateDescription != null) this.stateDescription = stateDescription
            }
            .padding(horizontal = LevyraPlayerDesign.SpaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
    ) {
        PlayerIcon(icon, content, Modifier.size(20.dp))
        if (showLabel) {
            Text(
                text = label,
                color = content,
                style = NightChipLabelStyle,
                maxLines = 1,
                modifier = Modifier.clearAndSetSemantics { }
            )
        }
    }
}

@Composable
private fun NightTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    surfaces: PlayerSurfaceTokens,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = NightTileHeight)
            .clip(RoundedCornerShape(percent = NightPillCorner))
            .background(surfaces.controlQuiet)
            .levyraPressable(onClick = onClick, role = Role.Button, haptic = LevyraHapticAction.Transport)
            .padding(start = LevyraPlayerDesign.SpaceSm, end = LevyraPlayerDesign.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(surfaces.control),
            contentAlignment = Alignment.Center
        ) {
            PlayerIcon(icon, surfaces.content, Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = surfaces.content,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    color = surfaces.contentMuted,
                    fontSize = 12.sp,
                    style = NightSpecStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private val NightChipLabelStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

private fun nightChipLabelsFit(
    labeled: List<String>,
    iconOnlyCount: Int,
    available: Dp,
    measure: (String) -> Int,
    density: Density
): Boolean = with(density) {
    val iconChip = 20.dp.toPx() + LevyraPlayerDesign.SpaceMd.toPx() * 2
    val labeledChrome = iconChip + LevyraPlayerDesign.SpaceSm.toPx()
    val chips = labeled.size + iconOnlyCount
    val gaps = LevyraPlayerDesign.SpaceSm.toPx() * (chips - 1).coerceAtLeast(0)
    val needed = labeled.sumOf(measure) + labeledChrome * labeled.size + iconChip * iconOnlyCount + gaps
    needed <= available.toPx()
}

@Composable
internal fun rememberPlayerAudioSpec(): State<PlayerAudioSpec> {
    val player by PlaybackService.activePlayerFlow.collectAsStateWithLifecycle()
    val spec = remember { mutableStateOf(PlayerAudioSpec()) }
    DisposableEffect(player) {
        val current = player
        if (current == null) {
            spec.value = PlayerAudioSpec()
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onTracksChanged(tracks: Tracks) {
                    spec.value = playerAudioSpecOf(tracks)
                }
            }
            spec.value = playerAudioSpecOf(current.currentTracks)
            current.addListener(listener)
            onDispose { current.removeListener(listener) }
        }
    }
    return spec
}

internal fun playerAudioSpecOf(tracks: Tracks): PlayerAudioSpec {
    for (group in tracks.groups) {
        if (group.type != C.TRACK_TYPE_AUDIO) continue
        for (index in 0 until group.length) {
            if (!group.isTrackSelected(index)) continue
            val format = group.getTrackFormat(index)
            val bitrate = format.averageBitrate.takeIf { it > 0 } ?: format.bitrate.takeIf { it > 0 }
            return PlayerAudioSpec(
                codec = playerAudioCodecLabel(format.sampleMimeType, format.codecs),
                bitrateKbps = bitrate?.div(1_000),
                sampleRateHz = format.sampleRate.takeIf { it > 0 },
                channels = format.channelCount.takeIf { it > 0 },
                mimeType = format.sampleMimeType.orEmpty(),
                codecString = format.codecs.orEmpty()
            )
        }
    }
    return PlayerAudioSpec()
}

package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.AnimatedContent
import com.luc4n3x.levyra.domain.RepeatMode
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.data.LyricsLatencyProfiles
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayPauseGlyph
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.components.rememberLevyraPlayMorphShape
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.lyrics.activeLyricIndex
import com.luc4n3x.levyra.ui.lyrics.lyricsOffsetPosition
import com.luc4n3x.levyra.ui.lyrics.rememberLyricsAudioOutputRoute
import com.luc4n3x.levyra.ui.lyrics.rememberLyricsPlaybackClock
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private val CinemaDockShape = RoundedCornerShape(28.dp)
private val CinemaThumbShape = RoundedCornerShape(10.dp)
private val CinemaArtworkShape = RoundedCornerShape(28.dp)
private val CinemaArtworkCorner = 28.dp
private val CinemaThumb = 40.dp
private val CinemaPlaySize = 52.dp
private val CinemaControlSize = 44.dp
private val CinemaActionSize = 36.dp
private val CinemaScrollingStage = 420.dp
private val CinemaVisualizerHeight = 22.dp
private val CinemaArtworkShadow = 28.dp
private val CinemaCreditBlock = 64.dp
private const val CinemaBackdropPixels = 32
private const val CinemaBackdropScale = 1.3f
private const val CinemaDriftMillis = 24_000
private const val CinemaCrossfadeMillis = 900
private const val CinemaFocusFraction = 0.34f
private const val CinemaTopFade = 0.3f
private const val CinemaBottomFade = 0.14f
private const val CinemaInactiveScale = 0.84f
private const val CinemaVisualizerBars = 24
private const val CinemaVisualizerMillis = 1_400
private const val CinemaArtworkWidth = 0.82f
private const val CinemaQueueKey = "queue"
private const val CinemaSweepMillis = 1_100
private val CinemaDockFill = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.11f), Color.White.copy(alpha = 0.05f)))
private val CinemaDockEdge = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f)))
private val CinemaInactiveShadow = Shadow(Color.Black.copy(alpha = 0.35f), blurRadius = 16f)

@Immutable
internal class PlayerCinemaLyrics(
    val trackId: String,
    val lines: List<LyricLine>,
    val synced: Boolean,
    val positionMs: Long,
    val playbackSpeed: Float,
    val latencyProfiles: LyricsLatencyProfiles
)

@Composable
internal fun PlayerCinemaDeck(
    track: Track,
    artworkUrl: String,
    slots: PlayerDeckSlots,
    surfaces: PlayerSurfaceTokens,
    accent: Color,
    lyrics: PlayerCinemaLyrics,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
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
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    headlineModifier: Modifier = Modifier
) {
    val tone by animateColorAsState(
        targetValue = accent,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-cinema-tone"
    )
    val drift = animated && isPlaying && LocalLevyraVisualCapabilities.current.decorativeMotion
    val motion = if (drift) rememberInfiniteTransition(label = "player-cinema-motion") else null
    val showLyrics = lyrics.trackId == track.id && lyrics.lines.isNotEmpty()
    val url = artworkUrl.ifBlank { track.thumbnailUrl }
    Box(modifier = modifier) {
        CinemaBackdrop(url = url, tone = tone, motion = motion, animated = animated)
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
                    bottom = if (compact) LevyraPlayerDesign.SpaceXs else LevyraPlayerDesign.SpaceSm
                )
        ) {
            slots.header()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (scrollable) Modifier.height(CinemaScrollingStage) else Modifier.weight(1f))
            ) {
                Crossfade(
                    targetState = showLyrics,
                    animationSpec = if (animated) LevyraPlayerDesign.standardTween<Float>() else snap(),
                    label = "player-cinema-stage"
                ) { lyricsStage ->
                    if (lyricsStage) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            CinemaCredits(
                                track = track,
                                artworkUrl = url,
                                surfaces = surfaces,
                                animated = animated,
                                stepDirection = stepDirection,
                                onArtistClick = onArtistClick,
                                modifier = headlineModifier.padding(top = LevyraPlayerDesign.SpaceSm)
                            )
                            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                CinemaLyricsStage(
                                    lyrics = lyrics,
                                    isPlaying = isPlaying,
                                    tone = tone,
                                    surfaces = surfaces,
                                    animated = animated,
                                    compact = compact
                                )
                                if (lyrics.synced) slots.gestures(track, Modifier.matchParentSize())
                            }
                        }
                    } else {
                        CinemaFeature(
                            track = track,
                            slots = slots,
                            tone = tone,
                            surfaces = surfaces,
                            isPlaying = isPlaying,
                            motion = motion,
                            animated = animated,
                            compact = compact,
                            artworkCap = artworkCap,
                            stepDirection = stepDirection,
                            onArtistClick = onArtistClick,
                            headlineModifier = headlineModifier
                        )
                    }
                }
            }
            CinemaDock(
                track = track,
                slots = slots,
                surfaces = surfaces,
                tone = tone,
                isFavorite = isFavorite,
                isPlaying = isPlaying,
                isResolving = isResolving,
                labels = labels,
                animated = animated,
                onToggleFavorite = onToggleFavorite,
                onPrevious = onPrevious,
                onTogglePlay = onTogglePlay,
                onNext = onNext,
                shuffleOn = shuffleOn,
                repeatMode = repeatMode,
                onShuffle = onShuffle,
                onRepeat = onRepeat
            )
        }
    }
}

@Composable
private fun CinemaBackdrop(
    url: String,
    tone: Color,
    motion: InfiniteTransition?,
    animated: Boolean
) {
    val phase = motion?.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(CinemaDriftMillis, easing = LinearEasing),
            androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "player-cinema-drift"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clearAndSetSemantics { }
    ) {
        Crossfade(
            targetState = url,
            animationSpec = if (animated) tween(CinemaCrossfadeMillis) else snap(),
            label = "player-cinema-backdrop"
        ) { shown ->
            if (shown.isNotBlank()) {
                val context = LocalContext.current
                val request = remember(context, shown) {
                    ImageRequest.Builder(context)
                        .data(LevyraArtworkCache.small(shown))
                        .size(CinemaBackdropPixels)
                        .memoryCacheKey("cinema-backdrop:$shown")
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val progress = phase?.value ?: 0.5f
                            val scale = CinemaBackdropScale + 0.06f * progress
                            scaleX = scale
                            scaleY = scale
                            translationX = (progress - 0.5f) * size.width * 0.07f
                            translationY = (0.5f - progress) * size.height * 0.035f
                        }
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val deep = lerp(tone, Color.Black, 0.78f)
                    val wash = lerp(tone, Color.Black, 0.6f).copy(alpha = 0.42f)
                    val light = Brush.radialGradient(
                        colors = listOf(tone.copy(alpha = 0.2f), Color.Transparent),
                        center = Offset(size.width * 0.25f, size.height * 0.3f),
                        radius = size.maxDimension * 0.7f
                    )
                    val depth = Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.16f to Color.Black.copy(alpha = 0.12f),
                        0.5f to Color.Black.copy(alpha = 0.18f),
                        0.78f to deep.copy(alpha = 0.78f),
                        1f to Color.Black.copy(alpha = 0.96f)
                    )
                    onDrawBehind {
                        drawRect(wash)
                        drawRect(light)
                        drawRect(depth)
                    }
                }
        )
    }
}

@Composable
private fun CinemaCredits(
    track: Track,
    artworkUrl: String,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    stepDirection: Int,
    onArtistClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    AnimatedContent(
        targetState = track,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = { LevyraMotion.trackChange(animated, stepDirection) },
        contentKey = { it.id },
        label = "player-cinema-credits"
    ) { shown ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            CinemaThumbnail(
                url = (if (shown.id == track.id) artworkUrl else "").ifBlank { shown.thumbnailUrl },
                modifier = Modifier
                    .size(CinemaThumb)
                    .clip(CinemaThumbShape)
            )
            Spacer(Modifier.width(LevyraPlayerDesign.SpaceMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shown.title,
                    color = surfaces.content,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                PlayerArtistText(
                    track = shown,
                    color = surfaces.contentMuted,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    onClickLabel = strings.openArtist,
                    onArtistClick = onArtistClick,
                    enabled = shown.id == track.id
                )
            }
        }
    }
}

@Composable
private fun CinemaThumbnail(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val request = remember(context, url) {
        ImageRequest.Builder(context).data(LevyraArtworkCache.small(url)).build()
    }
    Box(modifier = modifier.background(Color.White.copy(alpha = 0.08f))) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun CinemaLyricsStage(
    lyrics: PlayerCinemaLyrics,
    isPlaying: Boolean,
    tone: Color,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    compact: Boolean
) {
    val lines = remember(lyrics.lines) { playerLyricsCardLines(lyrics.lines) }
    val route = rememberLyricsAudioOutputRoute()
    val offsetMs = lyrics.latencyProfiles.effectiveOffsetMs(lyrics.trackId, route?.stableKey, route?.bluetooth == true)
    val currentOffsetMs by rememberUpdatedState(offsetMs)
    val synced = lyrics.synced
    val clock = rememberLyricsPlaybackClock(
        trackKey = lyrics.trackId,
        reportedPositionMs = lyrics.positionMs,
        isPlaying = isPlaying,
        speed = lyrics.playbackSpeed,
        smoothingEnabled = synced && animated
    )
    val activeIndex by remember(lines, synced, clock) {
        derivedStateOf {
            if (synced) activeLyricIndex(lyricsOffsetPosition(clock.positionMs, currentOffsetMs), lines) else -1
        }
    }
    val listState = remember(lyrics.trackId) { LazyListState() }
    var positioned by remember(lyrics.trackId) { mutableStateOf(false) }
    LaunchedEffect(activeIndex, synced, lines) {
        if (!synced || activeIndex !in lines.indices) return@LaunchedEffect
        if (!positioned || !animated) {
            listState.scrollToItem(activeIndex)
            positioned = true
        } else {
            listState.animateScrollToItem(activeIndex)
        }
    }
    val pivot = if (LocalLayoutDirection.current == LayoutDirection.Rtl) 1f else 0f
    val lineSize = if (compact) 28.sp else 34.sp
    val emphasis = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.expressiveSpring<Float>())
    val glow = remember(tone) { Shadow(lerp(tone, Color.White, 0.3f).copy(alpha = 0.55f), blurRadius = 28f) }
    val sheen = remember(tone) { lerp(tone, Color.White, 0.45f) }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val focus = maxHeight * CinemaFocusFraction
        LazyColumn(
            userScrollEnabled = !synced,
            state = listState,
            contentPadding = PaddingValues(
                top = if (synced) focus else LevyraPlayerDesign.SpaceLg,
                bottom = maxHeight - focus
            ),
            verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithCache {
                    val mask = Brush.verticalGradient(
                        0f to Color.Transparent,
                        CinemaTopFade * 0.5f to Color.Black.copy(alpha = 0.35f),
                        CinemaTopFade to Color.Black,
                        1f - CinemaBottomFade to Color.Black,
                        1f to Color.Transparent
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(brush = mask, blendMode = BlendMode.DstIn)
                    }
                }
        ) {
            itemsIndexed(
                items = lines,
                key = { index, line -> "$index:${line.startMs}" },
                contentType = { _, _ -> "PlayerCinema-line" }
            ) { index, line ->
                CinemaLyricLine(
                    line = line,
                    offset = index - activeIndex,
                    synced = synced,
                    animated = animated,
                    emphasis = emphasis,
                    pivot = pivot,
                    lineSize = lineSize,
                    glow = glow,
                    sheen = sheen,
                    surfaces = surfaces
                )
            }
        }
    }
}

internal fun cinemaLineAlpha(synced: Boolean, offset: Int): Float = when {
    !synced -> 0.9f
    offset == 0 -> 1f
    offset == 1 -> 0.64f
    offset == -1 -> 0.5f
    abs(offset) == 2 -> 0.4f
    else -> 0.28f
}

@Composable
private fun CinemaFeature(
    track: Track,
    slots: PlayerDeckSlots,
    tone: Color,
    surfaces: PlayerSurfaceTokens,
    isPlaying: Boolean,
    motion: InfiniteTransition?,
    animated: Boolean,
    compact: Boolean,
    artworkCap: Dp,
    stepDirection: Int,
    onArtistClick: (Int) -> Unit,
    headlineModifier: Modifier
) {
    val strings = LocalLevyraStrings.current
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val reserved = CinemaCreditBlock + CinemaVisualizerHeight + LevyraPlayerDesign.SpaceXl * 2
        val side = minOf(maxWidth * CinemaArtworkWidth, maxHeight - reserved, artworkCap).coerceAtLeast(0.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(side)
                    .drawBehind {
                        val reach = size.maxDimension * 0.95f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(tone.copy(alpha = 0.4f), tone.copy(alpha = 0.1f), Color.Transparent),
                                center = center.copy(y = center.y + size.height * 0.08f),
                                radius = reach
                            ),
                            radius = reach,
                            center = center.copy(y = center.y + size.height * 0.08f)
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            shadowElevation = CinemaArtworkShadow.toPx()
                            shape = CinemaArtworkShape
                            clip = false
                            ambientShadowColor = Color.Black
                            spotShadowColor = Color.Black
                        }
                ) {
                    slots.stage(track, Modifier.size(side), CinemaArtworkCorner)
                }
                slots.gestures(track, Modifier.matchParentSize())
            }
            Spacer(Modifier.height(LevyraPlayerDesign.SpaceXl))
            AnimatedContent(
                targetState = track,
                modifier = headlineModifier.width(side),
                transitionSpec = { LevyraMotion.trackChange(animated, stepDirection) },
                contentKey = { it.id },
                label = "player-cinema-feature-title"
            ) { shown ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = shown.title,
                        color = surfaces.content,
                        fontSize = if (compact) 22.sp else 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() }
                    )
                    PlayerArtistText(
                        track = shown,
                        color = surfaces.contentMuted,
                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
                        onClickLabel = strings.openArtist,
                        onArtistClick = onArtistClick,
                        enabled = shown.id == track.id
                    )
                }
            }
            Spacer(Modifier.height(LevyraPlayerDesign.SpaceLg))
            CinemaVisualizer(
                seed = track.id,
                color = lerp(tone, Color.White, 0.55f),
                playing = isPlaying,
                motion = motion,
                modifier = Modifier
                    .width(side * 0.42f)
                    .height(CinemaVisualizerHeight)
            )
        }
    }
}

@Composable
private fun CinemaVisualizer(
    seed: String,
    color: Color,
    playing: Boolean,
    motion: InfiniteTransition?,
    modifier: Modifier = Modifier
) {
    val levels = remember(seed) { emberSeekBars(null, seed) }
    val phase: State<Float>? = if (playing) {
        motion?.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(CinemaVisualizerMillis, easing = LinearEasing)),
            label = "player-cinema-visualizer"
        )
    } else {
        null
    }
    val energy by animateFloatAsState(
        targetValue = if (playing) 1f else 0.3f,
        animationSpec = LevyraMotion.standard<Float>(),
        label = "player-cinema-visualizer-energy"
    )
    Canvas(modifier = modifier.clearAndSetSemantics { }) {
        val count = CinemaVisualizerBars
        val gap = size.width / count
        val barWidth = gap * 0.42f
        val turn = (phase?.value ?: 0f) * 2f * PI.toFloat()
        for (index in 0 until count) {
            val base = levels[index * levels.size / count].coerceIn(0.15f, 1f)
            val wave = if (phase != null) 0.6f + 0.4f * sin(turn + index * 0.62f) else 1f
            val height = (size.height * base * wave * energy).coerceAtLeast(barWidth)
            drawRoundRect(
                color = color.copy(alpha = 0.3f + 0.5f * base),
                topLeft = Offset(gap * index + (gap - barWidth) / 2f, (size.height - height) / 2f),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}

@Composable
private fun CinemaDock(
    track: Track,
    slots: PlayerDeckSlots,
    surfaces: PlayerSurfaceTokens,
    tone: Color,
    isFavorite: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    labels: PlayerControlLabels,
    animated: Boolean,
    onToggleFavorite: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val haptics = LocalLevyraHaptics.current
    val actions = slots.dockActions(track)
    val queue = actions.firstOrNull { it.key == CinemaQueueKey }
    val accentInk = lerp(tone, Color.White, 0.66f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CinemaDockFill, CinemaDockShape)
            .border(LevyraPlayerDesign.Hairline, CinemaDockEdge, CinemaDockShape)
            .padding(start = LevyraPlayerDesign.SpaceMd, end = LevyraPlayerDesign.SpaceMd, top = LevyraPlayerDesign.SpaceXs, bottom = LevyraPlayerDesign.SpaceXxs)
    ) {
        slots.progress(PlayerProgressStyle.Cinema)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CinemaControl(
                icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                label = if (isFavorite) strings.removeFromFavorites else strings.addToFavorites,
                tint = if (isFavorite) accentInk else surfaces.content,
                onClick = {
                    haptics.perform(LevyraHapticAction.Confirm)
                    onToggleFavorite()
                }
            )
            CinemaControl(Icons.Rounded.SkipPrevious, labels.previous, surfaces.content) {
                haptics.perform(LevyraHapticAction.Transport)
                onPrevious()
            }
            Box(
                modifier = Modifier
                    .size(CinemaPlaySize)
                    .clip(rememberLevyraPlayMorphShape(playing = isPlaying, animated = animated))
                    .background(accentInk)
                    .levyraPressable(
                        onClick = {
                            haptics.perform(LevyraHapticAction.Transport)
                            onTogglePlay()
                        },
                        role = Role.Button,
                        pressedScale = LevyraPressScale.Control
                    )
                    .semantics { contentDescription = if (isPlaying) labels.pause else labels.play },
                contentAlignment = Alignment.Center
            ) {
                if (isResolving) {
                    LevyraLoadingIndicator(color = Color.Black, modifier = Modifier.size(28.dp))
                } else {
                    LevyraPlayPauseGlyph(playing = isPlaying, color = Color.Black, modifier = Modifier.size(24.dp))
                }
            }
            CinemaControl(Icons.Rounded.SkipNext, labels.next, surfaces.content) {
                haptics.perform(LevyraHapticAction.Transport)
                onNext()
            }
            CinemaControl(
                icon = queue?.icon ?: Icons.Rounded.QueueMusic,
                label = queue?.label.orEmpty(),
                tint = surfaces.content,
                enabled = queue?.enabled == true,
                onClick = { queue?.onClick?.invoke() }
            )
        }
        val extras = listOf(
            PlayerDockAction(key = "cinema-shuffle", icon = Icons.Rounded.Shuffle, label = labels.shuffle, onClick = onShuffle, active = shuffleOn, toggle = true),
            PlayerDockAction(
                key = "cinema-repeat",
                icon = if (repeatMode == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                label = labels.repeat,
                onClick = onRepeat,
                active = repeatMode != RepeatMode.Off,
                toggle = true
            )
        ) + actions.filterNot { it.key == CinemaQueueKey }
        if (extras.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                extras.forEach { action ->
                    CinemaAction(action = action, surfaces = surfaces, accent = accentInk)
                }
            }
        }
    }
}

@Composable
private fun CinemaAction(action: PlayerDockAction, surfaces: PlayerSurfaceTokens, accent: Color) {
    val haptics = LocalLevyraHaptics.current
    Box(
        modifier = Modifier
            .size(CinemaActionSize)
            .clip(CircleShape)
            .levyraPressable(
                onClick = {
                    action.haptic?.let(haptics::perform)
                    action.onClick()
                },
                enabled = action.enabled && !action.busy,
                role = Role.Button,
                pressedScale = LevyraPressScale.Control
            )
            .semantics {
                contentDescription = action.label
                action.stateDescription?.let { stateDescription = it }
            },
        contentAlignment = Alignment.Center
    ) {
        if (action.busy) {
            LevyraLoadingIndicator(color = surfaces.contentMuted, modifier = Modifier.size(20.dp))
        } else {
            PlayerIcon(
                action.icon,
                (if (action.active) accent else surfaces.contentMuted).copy(alpha = if (action.enabled) 1f else 0.4f),
                Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CinemaControl(
    icon: ImageVector,
    label: String,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(CinemaControlSize)
            .clip(CircleShape)
            .levyraPressable(
                onClick = onClick,
                enabled = enabled,
                role = Role.Button,
                pressedScale = LevyraPressScale.Control
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        PlayerIcon(icon, tint.copy(alpha = if (enabled) 1f else 0.4f), Modifier.size(24.dp))
    }
}

@Composable
private fun CinemaLyricLine(
    line: LyricLine,
    offset: Int,
    synced: Boolean,
    animated: Boolean,
    emphasis: AnimationSpec<Float>,
    pivot: Float,
    lineSize: TextUnit,
    glow: Shadow,
    sheen: Color,
    surfaces: PlayerSurfaceTokens
) {
    val current = offset == 0
    val alpha by animateFloatAsState(
        targetValue = cinemaLineAlpha(synced, offset),
        animationSpec = if (animated) LevyraMotion.standard() else snap(),
        label = "player-cinema-line-alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (!synced || current) 1f else CinemaInactiveScale,
        animationSpec = emphasis,
        label = "player-cinema-line-scale"
    )
    val sweep = remember { Animatable(1f) }
    LaunchedEffect(current, animated) {
        if (current && synced && animated) {
            sweep.snapTo(0f)
            sweep.animateTo(1f, tween(CinemaSweepMillis, easing = FastOutSlowInEasing))
        } else {
            sweep.snapTo(1f)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(pivot, 0.5f)
                compositingStrategy = if (current) CompositingStrategy.Offscreen else CompositingStrategy.Auto
            }
            .drawWithContent {
                drawContent()
                val progress = sweep.value
                if (progress < 1f) {
                    val band = size.width * 0.35f
                    val travel = -band + (size.width + band * 2f) * progress
                    val from = if (pivot == 0f) travel - band else size.width - travel - band
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, sheen, Color.Transparent),
                            startX = from,
                            endX = from + band * 2f
                        ),
                        blendMode = BlendMode.SrcAtop
                    )
                }
            }
            .padding(vertical = LevyraPlayerDesign.SpaceXs)
    ) {
        Text(
            text = line.text.ifBlank { "♪" },
            color = Color.White,
            fontSize = lineSize,
            lineHeight = lineSize * 1.12f,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.8).sp,
            style = TextStyle(shadow = if (synced && current) glow else CinemaInactiveShadow)
        )
        if (line.translated.isNotBlank()) {
            Text(
                text = line.translated,
                color = surfaces.contentMuted,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = LevyraPlayerDesign.SpaceXxs)
            )
        }
    }
}

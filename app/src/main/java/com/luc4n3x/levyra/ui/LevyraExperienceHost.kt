package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode as AnimationRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.PlayerAccentColors
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.PlayerTransportControls
import com.luc4n3x.levyra.ui.components.PremiumSeekbar
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val PLAYER_SWIPE_THRESHOLD_DP = 62f

internal enum class LevyraPlayerLayout {
    Compact,
    Portrait,
    Expanded
}

internal enum class LevyraPlayerGesture {
    None,
    Expand,
    Collapse,
    Previous,
    Next
}

internal fun levyraPlayerLayout(widthDp: Float, heightDp: Float): LevyraPlayerLayout = when {
    widthDp >= 720f || (widthDp >= 600f && widthDp > heightDp * 1.08f) -> LevyraPlayerLayout.Expanded
    widthDp < 380f || heightDp < 690f -> LevyraPlayerLayout.Compact
    else -> LevyraPlayerLayout.Portrait
}

internal fun levyraPlayerGesture(
    deltaX: Float,
    deltaY: Float,
    threshold: Float
): LevyraPlayerGesture {
    if (abs(deltaX) < threshold && abs(deltaY) < threshold) return LevyraPlayerGesture.None
    return if (abs(deltaX) > abs(deltaY)) {
        if (deltaX > 0f) LevyraPlayerGesture.Previous else LevyraPlayerGesture.Next
    } else {
        if (deltaY < 0f) LevyraPlayerGesture.Expand else LevyraPlayerGesture.Collapse
    }
}

@Composable
fun LevyraExperienceHost(
    viewModel: LevyraViewModel,
    isInPictureInPicture: Boolean = false
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var playerExpanded by rememberSaveable { mutableStateOf(state.selectedTab == LevyraTab.Player) }
    var lastContentTabName by rememberSaveable { mutableStateOf(LevyraTab.Home.name) }
    var introVisible by rememberSaveable(state.showOnboarding) {
        mutableStateOf(state.showOnboarding)
    }

    LaunchedEffect(state.selectedTab) {
        if (state.selectedTab == LevyraTab.Player) {
            playerExpanded = true
        } else {
            lastContentTabName = state.selectedTab.name
        }
    }

    val returnTab = remember(lastContentTabName) {
        LevyraTab.entries.firstOrNull { it.name == lastContentTabName && it != LevyraTab.Player }
            ?: LevyraTab.Home
    }

    LaunchedEffect(state.selectedTab, returnTab) {
        if (state.selectedTab == LevyraTab.Player) {
            viewModel.selectTab(returnTab)
        }
    }
    LaunchedEffect(state.currentTrack?.id) {
        if (state.currentTrack == null) playerExpanded = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LevyraApp(
            viewModel = viewModel,
            isInPictureInPicture = isInPictureInPicture,
            suppressLegacyPlayerSurfaces = true
        )

        if (!isInPictureInPicture) {
            when {
                state.showOnboarding && introVisible -> {
                    LevyraIntroExperience(
                        languageCode = state.languageCode,
                        onContinue = { introVisible = false }
                    )
                }
                state.showLyrics -> {
                    ImmersiveLyricsExperience(
                        state = state,
                        viewModel = viewModel
                    )
                }
                shouldShowMotionPlayer(state) -> {
                    state.currentTrack?.let { track ->
                        LevyraMotionPlayerSheet(
                            state = state,
                            track = track,
                            expanded = playerExpanded,
                            onExpandedChange = { playerExpanded = it },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

private fun shouldShowMotionPlayer(state: LevyraUiState): Boolean {
    if (state.currentTrack == null || state.showOnboarding) return false
    return !state.showSettings &&
        !state.showAudioQualityPanel &&
        !state.showQueue &&
        !state.showAlbum &&
        !state.showArtist &&
        state.openPlaylist == null &&
        state.sharedMediaPreview == null &&
        !state.youtubeEngagement.comments.visible &&
        !(state.showUpdatePrompt && state.updateInfo?.isNewer == true)
}

@Composable
private fun LevyraMotionPlayerSheet(
    state: LevyraUiState,
    track: Track,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    viewModel: LevyraViewModel
) {
    val progress = remember { Animatable(if (expanded) 1f else 0f) }
    val strings = LevyraStrings.forCode(state.languageCode)

    LaunchedEffect(expanded, state.animationsEnabled) {
        if (!state.animationsEnabled) {
            progress.snapTo(if (expanded) 1f else 0f)
        } else {
            progress.animateTo(
                targetValue = if (expanded) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = 420f
                )
            )
        }
    }

    BackHandler(enabled = expanded) {
        onExpandedChange(false)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(70f)
    ) {
        val navigationBottom = WindowInsets.navigationBars
            .asPaddingValues()
            .calculateBottomPadding()
        val collapsedHeight = 96.dp
        val collapsedBottomOffset = navigationBottom + 70.dp
        val sheetHeight = collapsedHeight + (maxHeight - collapsedHeight) * progress.value
        val bottomOffset = collapsedBottomOffset * (1f - progress.value)
        val horizontalInset = 10.dp * (1f - progress.value)
        val widthFraction = if (maxWidth >= 720.dp) {
            0.72f + 0.28f * progress.value
        } else {
            1f
        }
        val corner = 27.dp * (1f - progress.value)
        val primary = Color(track.accentStart)
        val secondary = Color(track.accentEnd)

        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(corner),
            shadowElevation = if (expanded) 0.dp else 28.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(widthFraction)
                .height(sheetHeight)
                .offset(y = -bottomOffset)
                .padding(horizontal = horizontalInset)
                .clip(RoundedCornerShape(corner))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                primary.copy(alpha = if (expanded) 0.40f else 0.24f),
                                LevyraInk.copy(alpha = 0.99f),
                                LevyraBlack
                            )
                        )
                    )
                    .border(
                        width = if (expanded) 0.dp else 1.dp,
                        color = Color.White.copy(alpha = 0.13f),
                        shape = RoundedCornerShape(corner)
                    )
            ) {
                LevyraPlayerBackdrop(
                    track = track,
                    expandedProgress = progress.value,
                    modifier = Modifier.fillMaxSize()
                )

                CollapsedLivingPlayer(
                    state = state,
                    track = track,
                    strings = strings,
                    primary = primary,
                    secondary = secondary,
                    alpha = (1f - progress.value * 1.65f).coerceIn(0f, 1f),
                    onOpen = { onExpandedChange(true) },
                    onToggle = viewModel::togglePlay,
                    onPrevious = viewModel::previous,
                    onNext = viewModel::next,
                    onClose = viewModel::closePlayer
                )

                ExpandedPlayerExperience(
                    state = state,
                    track = track,
                    strings = strings,
                    primary = primary,
                    secondary = secondary,
                    alpha = ((progress.value - 0.18f) / 0.82f).coerceIn(0f, 1f),
                    onCollapse = { onExpandedChange(false) },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun LevyraPlayerBackdrop(
    track: Track,
    expandedProgress: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val artwork = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
    Box(modifier = modifier) {
        if (artwork.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artwork)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 0.16f + expandedProgress * 0.16f
                        scaleX = 1.15f
                        scaleY = 1.15f
                    }
                    .blur(66.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.46f),
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun CollapsedLivingPlayer(
    state: LevyraUiState,
    track: Track,
    strings: LevyraStrings,
    primary: Color,
    secondary: Color,
    alpha: Float,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val thresholdPx = with(density) { PLAYER_SWIPE_THRESHOLD_DP.dp.toPx() }
    val progress = if (state.durationMs > 0L) {
        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .pointerInput(track.id) {
                var dragX = 0f
                var dragY = 0f
                detectDragGestures(
                    onDragStart = {
                        dragX = 0f
                        dragY = 0f
                    },
                    onDragEnd = {
                        when (levyraPlayerGesture(dragX, dragY, thresholdPx)) {
                            LevyraPlayerGesture.Expand -> onOpen()
                            LevyraPlayerGesture.Previous -> onPrevious()
                            LevyraPlayerGesture.Next -> onNext()
                            else -> Unit
                        }
                        if (abs(dragX) >= thresholdPx || abs(dragY) >= thresholdPx) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        dragX += amount.x
                        dragY += amount.y
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 9.dp, end = 6.dp, top = 7.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LivingArtwork(
                track = track,
                isPlaying = state.isPlaying,
                primary = primary,
                modifier = Modifier
                    .size(70.dp)
                    .clickable(onClick = onOpen)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpen),
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = track,
                    transitionSpec = { fadeIn(tween(170)) togetherWith fadeOut(tween(120)) },
                    label = "mini-track-title"
                ) { animatedTrack ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = animatedTrack.title,
                            color = Color.White,
                            fontSize = 14.5.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (state.isPlaying || state.isResolving) {
                                LivingEqualizer(
                                    playing = state.isPlaying && !state.isResolving,
                                    color = primary,
                                    modifier = Modifier.size(width = 15.dp, height = 12.dp)
                                )
                            }
                            Text(
                                text = animatedTrack.artist,
                                color = Color.White.copy(alpha = 0.61f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            MiniControlButton(
                icon = Icons.Rounded.SkipPrevious,
                contentDescription = strings.previous,
                onClick = onPrevious
            )
            MiniPlayButton(
                isPlaying = state.isPlaying,
                isResolving = state.isResolving,
                primary = primary,
                secondary = secondary,
                playLabel = strings.play,
                pauseLabel = strings.pause,
                onClick = onToggle
            )
            MiniControlButton(
                icon = Icons.Rounded.SkipNext,
                contentDescription = strings.next,
                onClick = onNext
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = strings.close,
                    tint = Color.White.copy(alpha = 0.48f),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(3.dp)
        ) {
            drawLine(
                color = Color.White.copy(alpha = 0.10f),
                start = androidx.compose.ui.geometry.Offset.Zero,
                end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                strokeWidth = size.height,
                cap = StrokeCap.Round
            )
            drawLine(
                brush = Brush.horizontalGradient(listOf(primary, secondary)),
                start = androidx.compose.ui.geometry.Offset.Zero,
                end = androidx.compose.ui.geometry.Offset(size.width * progress, 0f),
                strokeWidth = size.height,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun LivingArtwork(
    track: Track,
    isPlaying: Boolean,
    primary: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f),
        label = "mini-art-scale"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(12.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, primary.copy(alpha = 0.42f), RoundedCornerShape(18.dp))
            .background(LevyraPanel),
        contentAlignment = Alignment.Center
    ) {
        val artwork = track.thumbnailUrl.ifBlank { track.largeThumbnailUrl }
        if (artwork.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artwork)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(120)
                    .build(),
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun LivingEqualizer(
    playing: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "living-equalizer")
    val first by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (playing) 1f else 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(430),
            repeatMode = AnimationRepeatMode.Reverse
        ),
        label = "eq-first"
    )
    val second by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = if (playing) 0.28f else 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(560),
            repeatMode = AnimationRepeatMode.Reverse
        ),
        label = "eq-second"
    )
    val third by transition.animateFloat(
        initialValue = 0.42f,
        targetValue = if (playing) 0.92f else 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(370),
            repeatMode = AnimationRepeatMode.Reverse
        ),
        label = "eq-third"
    )
    Canvas(modifier = modifier) {
        val values = listOf(first, second, third)
        val barWidth = size.width / 6f
        val gap = barWidth * 0.75f
        values.forEachIndexed { index, value ->
            val left = index * (barWidth + gap)
            val height = size.height * value.coerceIn(0.18f, 1f)
            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(left, size.height - height),
                size = androidx.compose.ui.geometry.Size(barWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
            )
        }
    }
}

@Composable
private fun MiniControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(38.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = 0.82f),
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun MiniPlayButton(
    isPlaying: Boolean,
    isResolving: Boolean,
    primary: Color,
    secondary: Color,
    playLabel: String,
    pauseLabel: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        shape = CircleShape,
        modifier = Modifier
            .size(44.dp)
            .shadow(8.dp, CircleShape, clip = false)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(primary, secondary)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isResolving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.2.dp,
                    color = Color.White
                )
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) pauseLabel else playLabel,
                    tint = Color.White,
                    modifier = Modifier
                        .size(25.dp)
                        .offset(x = if (isPlaying) 0.dp else 1.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpandedPlayerExperience(
    state: LevyraUiState,
    track: Track,
    strings: LevyraStrings,
    primary: Color,
    secondary: Color,
    alpha: Float,
    onCollapse: () -> Unit,
    viewModel: LevyraViewModel
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .statusBarsPadding()
    ) {
        val layout = levyraPlayerLayout(maxWidth.value, maxHeight.value)
        val horizontalPadding = when (layout) {
            LevyraPlayerLayout.Compact -> 16.dp
            LevyraPlayerLayout.Portrait -> 22.dp
            LevyraPlayerLayout.Expanded -> 30.dp
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = 8.dp)
        ) {
            ExpandedPlayerHeader(
                track = track,
                strings = strings,
                primary = primary,
                onCollapse = onCollapse,
                onMore = viewModel::openAudioQualityPanel
            )

            when (layout) {
                LevyraPlayerLayout.Expanded -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 12.dp, bottom = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(34.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerArtworkPanel(
                            state = state,
                            track = track,
                            primary = primary,
                            secondary = secondary,
                            onPrevious = viewModel::previous,
                            onNext = viewModel::next,
                            onSeekBy = viewModel::seekBy,
                            onTemporarySpeed = viewModel::setTemporaryPlaybackSpeed,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        PlayerControlPanel(
                            state = state,
                            track = track,
                            strings = strings,
                            primary = primary,
                            secondary = secondary,
                            compact = false,
                            viewModel = viewModel,
                            modifier = Modifier
                                .weight(1f)
                                .widthIn(max = 620.dp)
                        )
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = if (layout == LevyraPlayerLayout.Compact) 2.dp else 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        PlayerArtworkPanel(
                            state = state,
                            track = track,
                            primary = primary,
                            secondary = secondary,
                            onPrevious = viewModel::previous,
                            onNext = viewModel::next,
                            onSeekBy = viewModel::seekBy,
                            onTemporarySpeed = viewModel::setTemporaryPlaybackSpeed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = true)
                        )
                        PlayerControlPanel(
                            state = state,
                            track = track,
                            strings = strings,
                            primary = primary,
                            secondary = secondary,
                            compact = layout == LevyraPlayerLayout.Compact,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandedPlayerHeader(
    track: Track,
    strings: LevyraStrings,
    primary: Color,
    onCollapse: () -> Unit,
    onMore: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f))
                .clickable(onClick = onCollapse)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerHeaderButton(
                icon = Icons.Rounded.KeyboardArrowDown,
                contentDescription = strings.back,
                onClick = onCollapse
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings.formatPlayingFrom(track.source.ifBlank { "LEVYRA" }),
                    color = Color.White.copy(alpha = 0.50f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(34.dp)
                        .height(2.dp)
                        .background(primary.copy(alpha = 0.70f), CircleShape)
                )
            }
            PlayerHeaderButton(
                icon = Icons.Rounded.MoreVert,
                contentDescription = strings.options,
                onClick = onMore
            )
        }
    }
}

@Composable
private fun PlayerHeaderButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.07f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        shape = CircleShape,
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = 0.90f),
                modifier = Modifier.size(23.dp)
            )
        }
    }
}

@Composable
private fun PlayerArtworkPanel(
    state: LevyraUiState,
    track: Track,
    primary: Color,
    secondary: Color,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onTemporarySpeed: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val thresholdPx = with(density) { PLAYER_SWIPE_THRESHOLD_DP.dp.toPx() }
    val seekStep = state.interfaceSettings.doubleTapSeekSeconds.toLong() * 1_000L
    val artwork = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
    val scale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1f else 0.955f,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 300f),
        label = "expanded-artwork-scale"
    )
    var dragX by remember(track.id) { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight(0.90f)
                .fillMaxWidth(0.92f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = dragX * 0.16f
                }
                .shadow(
                    elevation = if (state.isPlaying) 30.dp else 18.dp,
                    shape = RoundedCornerShape(34.dp),
                    clip = false
                )
                .clip(RoundedCornerShape(34.dp))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(34.dp))
                .background(Brush.linearGradient(listOf(primary.copy(alpha = 0.44f), secondary.copy(alpha = 0.36f))))
                .pointerInput(track.id, seekStep, state.interfaceSettings.longPressSpeed) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            onSeekBy(if (offset.x < size.width / 2f) -seekStep else seekStep)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onPress = {
                            val originalSpeed = state.playbackSpeed
                            var boosted = false
                            val speedJob = launch {
                                delay(360L)
                                boosted = true
                                onTemporarySpeed(state.interfaceSettings.longPressSpeed)
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            try {
                                tryAwaitRelease()
                            } finally {
                                speedJob.cancel()
                                if (boosted) onTemporarySpeed(originalSpeed)
                            }
                        }
                    )
                }
                .pointerInput(track.id) {
                    detectDragGestures(
                        onDragStart = { dragX = 0f },
                        onDragCancel = { dragX = 0f },
                        onDragEnd = {
                            when {
                                dragX > thresholdPx -> onPrevious()
                                dragX < -thresholdPx -> onNext()
                            }
                            if (abs(dragX) > thresholdPx) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            dragX = 0f
                        },
                        onDrag = { change, amount ->
                            if (abs(amount.x) >= abs(amount.y)) {
                                change.consume()
                                dragX += amount.x
                            }
                        }
                    )
                }
        ) {
            if (artwork.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(artwork)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .crossfade(180)
                        .build(),
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(84.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.18f)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun PlayerControlPanel(
    state: LevyraUiState,
    track: Track,
    strings: LevyraStrings,
    primary: Color,
    secondary: Color,
    compact: Boolean,
    viewModel: LevyraViewModel,
    modifier: Modifier = Modifier
) {
    val isFavorite = track.id in state.favoriteIds
    val activeLyric = state.activeLyric ?: state.lyrics.firstOrNull { line ->
        state.positionMs in line.startMs..line.endMs
    }
    val labels = remember(strings) {
        PlayerControlLabels(
            shuffle = strings.shuffle,
            previous = strings.previous,
            play = strings.play,
            pause = strings.pause,
            next = strings.next,
            repeat = strings.repeat
        )
    }

    Column(
        modifier = modifier
            .padding(bottom = if (compact) 10.dp else 18.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = if (compact) 22.sp else 28.sp,
                    lineHeight = if (compact) 25.sp else 31.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = Color.White.copy(alpha = 0.60f),
                    fontSize = if (compact) 13.sp else 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .clickable { viewModel.openArtist(track) }
                )
            }
            Surface(
                color = if (isFavorite) primary.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.07f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isFavorite) primary.copy(alpha = 0.52f) else Color.White.copy(alpha = 0.10f)
                ),
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .clickable { viewModel.toggleFavorite(track) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isFavorite) strings.removeFromFavorites else strings.addToFavorites,
                        tint = if (isFavorite) primary else Color.White.copy(alpha = 0.82f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        PremiumSeekbar(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            bufferedPositionMs = state.bufferedPositionMs,
            onSeekTo = { seekMs ->
                if (state.durationMs > 0L) {
                    viewModel.seekTo((seekMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f))
                }
            },
            activeColor = primary,
            trailingColor = secondary,
            inactiveColor = Color.White.copy(alpha = 0.15f),
            isPlaying = state.isPlaying,
            animated = state.animationsEnabled
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatPlayerTime(state.positionMs),
                color = Color.White.copy(alpha = 0.53f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = formatPlayerTime(state.durationMs),
                color = Color.White.copy(alpha = 0.42f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        PlayerTransportControls(
            isPlaying = state.isPlaying,
            isResolving = state.isResolving,
            shuffleOn = state.shuffleEnabled,
            repeatOn = state.repeatMode != RepeatMode.Off,
            repeatOne = state.repeatMode == RepeatMode.One,
            accents = PlayerAccentColors(
                primary = primary,
                secondary = secondary,
                primaryTarget = primary,
                secondaryTarget = secondary
            ),
            compact = compact,
            animated = state.animationsEnabled,
            labels = labels,
            onShuffle = viewModel::toggleShuffle,
            onPrevious = viewModel::previous,
            onToggle = viewModel::togglePlay,
            onNext = viewModel::next,
            onRepeat = viewModel::toggleRepeat
        )

        if (activeLyric != null || state.lyricsLoading) {
            Surface(
                color = Color.Black.copy(alpha = 0.22f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = viewModel::openLyrics)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = if (compact) 11.dp else 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Subject,
                        contentDescription = strings.lyrics,
                        tint = primary,
                        modifier = Modifier.size(21.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        if (state.lyricsLoading && activeLyric == null) {
                            Text(
                                text = strings.searchingLyrics,
                                color = Color.White.copy(alpha = 0.70f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = activeLyric?.text.orEmpty(),
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = if (compact) 13.sp else 14.sp,
                                lineHeight = if (compact) 17.sp else 19.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!activeLyric?.translated.isNullOrBlank()) {
                                Text(
                                    text = activeLyric?.translated.orEmpty(),
                                    color = Color.White.copy(alpha = 0.47f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            PlayerQuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                label = strings.queue,
                accent = primary,
                onClick = viewModel::openQueue
            )
            PlayerQuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Rounded.Subject,
                label = strings.lyrics,
                accent = secondary,
                onClick = viewModel::openLyrics
            )
            PlayerQuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Equalizer,
                label = strings.audioEngine,
                accent = primary,
                onClick = viewModel::openAudioQualityPanel
            )
        }
    }
}

@Composable
private fun PlayerQuickAction(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.06f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
        shape = RoundedCornerShape(17.dp),
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatPlayerTime(valueMs: Long): String {
    if (valueMs <= 0L) return "0:00"
    val totalSeconds = valueMs / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

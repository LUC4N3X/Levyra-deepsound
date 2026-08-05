package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val SAFE_PLAYER_SWIPE_DP = 58f

/**
 * Modern player runtime built from two independent fixed surfaces.
 *
 * The mini-player never morphs into the full player. This intentionally avoids
 * the full-screen blur, continuously changing sheet height and simultaneous
 * composition path that proved unstable on a physical Galaxy device.
 */
@Composable
fun LevyraStableExperienceHost(
    viewModel: LevyraViewModel,
    isInPictureInPicture: Boolean = false
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var expanded by rememberSaveable { mutableStateOf(false) }
    var lastContentTabName by rememberSaveable { mutableStateOf(LevyraTab.Home.name) }
    var introVisible by rememberSaveable(state.showOnboarding) {
        mutableStateOf(state.showOnboarding)
    }

    LaunchedEffect(state.selectedTab) {
        if (state.selectedTab == LevyraTab.Player) {
            expanded = true
            val returnTab = LevyraTab.entries.firstOrNull {
                it.name == lastContentTabName && it != LevyraTab.Player
            } ?: LevyraTab.Home
            viewModel.selectTab(returnTab)
        } else {
            lastContentTabName = state.selectedTab.name
        }
    }

    LaunchedEffect(state.currentTrack?.id) {
        if (state.currentTrack == null) expanded = false
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

                safeShouldShowPlayer(state) -> {
                    state.currentTrack?.let { track ->
                        if (expanded) {
                            SafeFullPlayer(
                                state = state,
                                track = track,
                                onCollapse = { expanded = false },
                                viewModel = viewModel
                            )
                        } else {
                            SafeMiniPlayer(
                                state = state,
                                track = track,
                                onOpen = { expanded = true },
                                onClose = viewModel::closePlayer,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun safeShouldShowPlayer(state: LevyraUiState): Boolean {
    if (state.currentTrack == null || state.showOnboarding || state.showLyrics) return false
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
private fun SafeMiniPlayer(
    state: LevyraUiState,
    track: Track,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    viewModel: LevyraViewModel
) {
    val strings = LevyraStrings.forCode(state.languageCode)
    val primary = Color(track.accentStart)
    val secondary = Color(track.accentEnd)
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val progress = if (state.durationMs > 0L) {
        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(80f)
    ) {
        Surface(
            color = LevyraInk,
            shape = RoundedCornerShape(25.dp),
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 10.dp, end = 10.dp, bottom = bottomInset + 70.dp)
                .fillMaxWidth()
                .height(88.dp)
                .pointerInput(track.id) {
                    var dragY = 0f
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            dragY += amount
                        },
                        onDragEnd = {
                            if (dragY < -48.dp.toPx()) onOpen()
                            dragY = 0f
                        },
                        onDragCancel = { dragY = 0f }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                primary.copy(alpha = 0.29f),
                                secondary.copy(alpha = 0.14f),
                                LevyraInk
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 9.dp, end = 4.dp, top = 8.dp, bottom = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    SafeArtwork(
                        track = track,
                        modifier = Modifier
                            .size(68.dp)
                            .clickable(onClick = onOpen)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpen),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = track.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            color = Color.White.copy(alpha = 0.58f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    SafeMiniButton(
                        icon = Icons.Rounded.SkipPrevious,
                        description = strings.previous,
                        onClick = viewModel::previous
                    )
                    SafeMiniPlayButton(
                        state = state,
                        primary = primary,
                        strings = strings,
                        onClick = viewModel::togglePlay
                    )
                    SafeMiniButton(
                        icon = Icons.Rounded.SkipNext,
                        description = strings.next,
                        onClick = viewModel::next
                    )
                    IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = strings.close,
                            tint = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(Brush.horizontalGradient(listOf(primary, secondary)), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun SafeMiniButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White.copy(alpha = 0.82f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SafeMiniPlayButton(
    state: LevyraUiState,
    primary: Color,
    strings: LevyraStrings,
    onClick: () -> Unit
) {
    Surface(
        color = primary.copy(alpha = 0.27f),
        shape = CircleShape,
        modifier = Modifier
            .size(42.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (state.isResolving) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(19.dp)
                )
            } else {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.isPlaying) strings.pause else strings.play,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun SafeFullPlayer(
    state: LevyraUiState,
    track: Track,
    onCollapse: () -> Unit,
    viewModel: LevyraViewModel
) {
    BackHandler(onBack = onCollapse)
    val strings = LevyraStrings.forCode(state.languageCode)
    val primary = Color(track.accentStart)
    val secondary = Color(track.accentEnd)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(90f)
            .background(
                Brush.verticalGradient(
                    listOf(
                        primary.copy(alpha = 0.38f),
                        secondary.copy(alpha = 0.16f),
                        LevyraInk,
                        LevyraBlack
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val wideLayout = maxWidth >= 720.dp
        val compactLayout = maxHeight < 760.dp
        val portraitArtworkSize = if (compactLayout) 245.dp else 330.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (wideLayout) 28.dp else 18.dp, vertical = 8.dp)
        ) {
            SafePlayerHeader(strings = strings, onCollapse = onCollapse)

            if (wideLayout) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 12.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(34.dp)
                ) {
                    SafeGestureArtwork(
                        state = state,
                        track = track,
                        primary = primary,
                        secondary = secondary,
                        viewModel = viewModel,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    SafePlayerControls(
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
                            .verticalScroll(rememberScrollState())
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SafeGestureArtwork(
                        state = state,
                        track = track,
                        primary = primary,
                        secondary = secondary,
                        viewModel = viewModel,
                        modifier = Modifier.size(portraitArtworkSize)
                    )
                    SafePlayerControls(
                        state = state,
                        track = track,
                        strings = strings,
                        primary = primary,
                        secondary = secondary,
                        compact = compactLayout,
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 620.dp)
                            .padding(top = 12.dp, bottom = 18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SafePlayerHeader(
    strings: LevyraStrings,
    onCollapse: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCollapse, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = strings.back,
                tint = Color.White,
                modifier = Modifier.size(27.dp)
            )
        }
        Text(
            text = "LEVYRA",
            color = Color.White.copy(alpha = 0.68f),
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(44.dp))
    }
}

@Composable
private fun SafeGestureArtwork(
    state: LevyraUiState,
    track: Track,
    primary: Color,
    secondary: Color,
    viewModel: LevyraViewModel,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val thresholdPx = with(density) { SAFE_PLAYER_SWIPE_DP.dp.toPx() }
    val seekStepMs = state.interfaceSettings.doubleTapSeekSeconds.toLong() * 1_000L
    val scale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1f else 0.965f,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 360f),
        label = "safe-player-artwork-scale"
    )
    var dragX by remember(track.id) { mutableFloatStateOf(0f) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        SafeArtwork(
            track = track,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(1f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = dragX * 0.11f
                }
                .shadow(20.dp, RoundedCornerShape(32.dp), clip = false)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp))
                .pointerInput(track.id, seekStepMs, state.interfaceSettings.longPressSpeed) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            viewModel.seekBy(if (offset.x < size.width / 2f) -seekStepMs else seekStepMs)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onPress = {
                            val originalSpeed = state.playbackSpeed
                            var boosted = false
                            val speedJob = launch {
                                delay(360L)
                                boosted = true
                                viewModel.setTemporaryPlaybackSpeed(state.interfaceSettings.longPressSpeed)
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            try {
                                tryAwaitRelease()
                            } finally {
                                speedJob.cancel()
                                if (boosted) viewModel.setTemporaryPlaybackSpeed(originalSpeed)
                            }
                        }
                    )
                }
                .pointerInput(track.id) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragX = 0f },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragX += amount
                        },
                        onDragEnd = {
                            when {
                                dragX > thresholdPx -> viewModel.previous()
                                dragX < -thresholdPx -> viewModel.next()
                            }
                            if (abs(dragX) > thresholdPx) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            dragX = 0f
                        },
                        onDragCancel = { dragX = 0f }
                    )
                }
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .width(66.dp)
                .height(4.dp)
                .background(Brush.horizontalGradient(listOf(primary, secondary)), CircleShape)
        )
    }
}

@Composable
private fun SafeArtwork(
    track: Track,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val artwork = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(LevyraPanel),
        contentAlignment = Alignment.Center
    ) {
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
                tint = Color.White,
                modifier = Modifier.size(72.dp)
            )
        }
    }
}

@Composable
private fun SafePlayerControls(
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
    val activeLyric = state.activeLyric ?: state.lyrics.firstOrNull {
        state.positionMs in it.startMs..it.endMs
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
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = if (compact) 21.sp else 27.sp,
                    lineHeight = if (compact) 24.sp else 31.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = Color.White.copy(alpha = 0.61f),
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
                color = if (isFavorite) primary.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.07f),
                shape = CircleShape,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
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
                text = safeFormatPlayerTime(state.positionMs),
                color = Color.White.copy(alpha = 0.54f),
                fontSize = 11.sp
            )
            Text(
                text = safeFormatPlayerTime(state.durationMs),
                color = Color.White.copy(alpha = 0.44f),
                fontSize = 11.sp
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
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = viewModel::openLyrics)
            ) {
                Text(
                    text = activeLyric?.text ?: strings.searchingLyrics,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            SafeQuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                label = strings.queue,
                accent = primary,
                onClick = viewModel::openQueue
            )
            SafeQuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Rounded.Subject,
                label = strings.lyrics,
                accent = secondary,
                onClick = viewModel::openLyrics
            )
            SafeQuickAction(
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
private fun SafeQuickAction(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.07f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun safeFormatPlayerTime(valueMs: Long): String {
    if (valueMs <= 0L) return "0:00"
    val totalSeconds = valueMs / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

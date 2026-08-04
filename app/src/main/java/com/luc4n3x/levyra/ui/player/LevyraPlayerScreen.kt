package com.luc4n3x.levyra.ui.player

import android.app.Activity
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ToggleableState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.luc4n3x.levyra.domain.Models.Track
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.feature.player.domain.LevyraUiState
import com.luc4n3x.levyra.feature.player.presentation.PlayerViewModel
import com.luc4n3x.levyra.player.LevyraPipBridge
import com.luc4n3x.levyra.ui.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.*
import com.luc4n3x.levyra.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LevyraPlayerScreen(
    viewModel: PlayerViewModel,
    state: LevyraUiState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val strings = LocalLevyraStrings.current
    val track = state.currentTrack
    val playerContext = LocalContext.current
    val playerActivity = playerContext as? Activity
    val audioManager = remember(playerContext) { playerContext.getSystemService(AudioManager::class.java) }
    val hapticFeedback = LocalHapticFeedback.current
    val seekStepMs = state.interfaceSettings.doubleTapSeekSeconds.toLong() * 1_000L
    val rawPrimaryTarget = track?.let { Color(it.accentStart) } ?: LevyraCyan
    val rawSecondaryTarget = track?.let { Color(it.accentEnd) } ?: LevyraViolet
    val harmonizedTargets = remember(rawPrimaryTarget, rawSecondaryTarget) {
        harmonizePlayerAccents(rawPrimaryTarget, rawSecondaryTarget)
    }
    val primaryTarget = harmonizedTargets.primary
    val secondaryTarget = harmonizedTargets.secondary
    val primary by animateColorAsState(
        targetValue = primaryTarget,
        animationSpec = tween(700, easing = LinearOutSlowInEasing),
        label = "player-primary-color"
    )
    val secondary by animateColorAsState(
        targetValue = secondaryTarget,
        animationSpec = tween(700, easing = LinearOutSlowInEasing),
        label = "player-secondary-color"
    )
    val primaryContent = remember(primary) {
        primary.playerContentColor(listOf(PlayerDarkSurface))
    }
    val secondaryContent = remember(secondary) {
        secondary.playerContentColor(listOf(PlayerDarkSurface))
    }
    val playerAccentColors = PlayerAccentColors(
        primary = primary,
        secondary = secondary,
        primaryTarget = primaryTarget,
        secondaryTarget = secondaryTarget
    )
    val playerControlLabels = remember(strings) {
        PlayerControlLabels(
            shuffle = strings.shuffle,
            previous = strings.previous,
            play = strings.play,
            pause = strings.pause,
            next = strings.next,
            repeat = strings.repeat
        )
    }
    val artworkUrl = track?.largeThumbnailUrl?.ifBlank { track.thumbnailUrl }.orEmpty()
    val sharedArtworkModifier = sharedPlayerArtworkModifier(
        trackId = track?.id.orEmpty(),
        enabled = state.animationsEnabled && !state.isVideoMode && track != null,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    )
    var mediaSeekFeedbackMs by remember(track?.id) { mutableStateOf(0L) }
    var mediaSeekFeedbackEvent by remember(track?.id) { mutableStateOf(0) }
    var gestureFeedback by remember(track?.id) { mutableStateOf("") }
    var gestureFeedbackEvent by remember(track?.id) { mutableStateOf(0) }
    var playlistTarget by remember(track?.id) { mutableStateOf<Track?>(null) }

    BackHandler(enabled = state.youtubeEngagement.comments.visible) {
        viewModel.closeYoutubeComments()
    }

    LaunchedEffect(mediaSeekFeedbackEvent) {
        if (mediaSeekFeedbackEvent > 0) {
            delay(650L)
            mediaSeekFeedbackMs = 0L
        }
    }
    LaunchedEffect(gestureFeedbackEvent) {
        if (gestureFeedbackEvent > 0) {
            delay(700L)
            gestureFeedback = ""
        }
    }

    val artScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1f else 0.945f,
        animationSpec = if (state.animationsEnabled) LevyraPlayerDesign.expressiveSpring() else snap(),
        label = "artwork-scale"
    )
    val artCorner by animateDpAsState(
        targetValue = if (state.isPlaying) LevyraPlayerDesign.CornerXl else LevyraPlayerDesign.CornerLg,
        animationSpec = if (state.animationsEnabled) LevyraPlayerDesign.expressiveSpring() else snap(),
        label = "artwork-corner"
    )
    val artShadow by animateFloatAsState(
        targetValue = if (state.isPlaying) 32f else 16f,
        animationSpec = if (state.animationsEnabled) LevyraPlayerDesign.smoothSpring() else snap(),
        label = "artwork-shadow"
    )
    val artOffset by animateDpAsState(
        targetValue = if (state.isPlaying) 0.dp else 4.dp,
        animationSpec = if (state.animationsEnabled) LevyraPlayerDesign.expressiveSpring() else snap(),
        label = "artwork-offset"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val compactPlayer = maxWidth < 380.dp || maxHeight < 700.dp
        var advancedControlsExpanded by remember(track?.id) {
            mutableStateOf(false)
        }
        val playerHorizontalPadding = when {
            state.isVideoMode -> LevyraPlayerDesign.SpaceSm
            compactPlayer -> LevyraPlayerDesign.GutterCompact
            else -> LevyraPlayerDesign.Gutter
        }
        val playerItemSpacing = if (compactPlayer) LevyraPlayerDesign.SpaceSm else LevyraPlayerDesign.SpaceMd
        val artworkSize = minOf(
            (maxWidth - playerHorizontalPadding * 2f).coerceAtLeast(180.dp),
            520.dp
        )

        PlayerImmersiveBackdrop(
            primaryTarget = primaryTarget,
            secondaryTarget = secondaryTarget,
            isPlaying = state.isPlaying,
            modifier = Modifier.fillMaxSize()
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = playerHorizontalPadding,
                end = playerHorizontalPadding,
                top = if (compactPlayer) 8.dp else 10.dp,
                bottom = if (compactPlayer) 28.dp else 34.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(playerItemSpacing)
        ) {
            item {
                val headerButtonSize = if (compactPlayer) {
                    LevyraPlayerDesign.HeaderButtonCompact
                } else {
                    LevyraPlayerDesign.HeaderButton
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LevyraPlayerDesign.MinimumTouchTarget)
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
                ) {
                    PlayerGlassIconButton(
                        icon = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = strings.back,
                        size = headerButtonSize,
                        iconSize = if (compactPlayer) 25.dp else 26.dp,
                        onClick = { viewModel.selectTab(com.luc4n3x.levyra.domain.LevyraTab.Home) }
                    )
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (track != null && (track.videoUrl.isNotBlank() || track.counterpartVideoId.isNotBlank())) {
                            PlayerModeSwitch(
                                isVideoMode = state.isVideoMode,
                                activeColor = primary,
                                activeColorTarget = primaryTarget,
                                onSong = viewModel::toggleVideoMode,
                                onVideo = viewModel::toggleVideoMode
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .playerGlass(
                                        shape = LevyraPlayerDesign.ShapePill,
                                        fill = LevyraPlayerDesign.GlassFillSunken
                                    )
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = strings.formatPlayingFrom(track?.source ?: "LEVYRA"),
                                    color = LevyraPlayerDesign.TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.1.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (state.isVideoMode) {
                        PlayerGlassIconButton(
                            icon = Icons.Rounded.PictureInPictureAlt,
                            contentDescription = strings.pictureInPicture,
                            size = headerButtonSize,
                            iconSize = 20.dp,
                            borderTop = primary.copy(alpha = 0.48f),
                            borderBottom = primary.copy(alpha = 0.14f),
                            onClick = { LevyraPipBridge.enter() }
                        )
                    }
                    PlayerGlassIconButton(
                        icon = Icons.Rounded.MoreVert,
                        contentDescription = strings.options,
                        size = headerButtonSize,
                        iconSize = if (compactPlayer) 21.dp else 22.dp,
                        onClick = { viewModel.openAudioQualityPanel() }
                    )
                }
            }
            if (track == null) {
                item { EmptyState(strings.emptyPlayer) }
            } else {
                item {
                    val mediaHeight = artworkSize
                    Box(
                        modifier = Modifier
                            .size(width = artworkSize, height = mediaHeight)
                            .padding(vertical = if (compactPlayer) 1.dp else 2.dp)
                    ) {
                        if (state.isVideoMode && track.videoUrl.isNotBlank()) {
                            LevyraVideoSurface(
                                state = state,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                                    .graphicsLayer {
                                        scaleX = artScale
                                        scaleY = artScale
                                        translationY = artOffset.toPx()
                                        shadowElevation = artShadow
                                        shape = RoundedCornerShape(artCorner)
                                        clip = true
                                    }
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(artCorner)
                                    )
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(18.dp)
                                    .zIndex(40f)
                                    .clickable(onClick = viewModel::toggleVideoMode)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = strings.song,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            PlayerArtworkCanvas(
                                track = track,
                                artworkUrl = artworkUrl,
                                motionArtwork = state.motionArtwork,
                                animationsEnabled = state.animationsEnabled && !state.isVideoMode,
                                isPlaying = state.isPlaying,
                                cornerRadius = artCorner,
                                modifier = sharedArtworkModifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = artScale
                                        scaleY = artScale
                                        translationY = artOffset.toPx()
                                        shape = RoundedCornerShape(artCorner)
                                        shadowElevation = artShadow
                                        clip = true
                                    }
                            )
                        }

                        if (state.interfaceSettings.playerGesturesEnabled) {
                            Row(
                                modifier = Modifier
                                    .matchParentSize()
                                    .zIndex(20f)
                            ) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .pointerInput(track.id, state.interfaceSettings.doubleTapSeekSeconds, state.interfaceSettings.longPressSpeed) {
                                                detectTapGestures(
                                                    onPress = {
                                                        val originalSpeed = state.playbackSpeed
                                                        coroutineScope {
                                                            var boosted = false
                                                            val speedJob = launch {
                                                                delay(320L)
                                                                boosted = true
                                                                viewModel.setTemporaryPlaybackSpeed(state.interfaceSettings.longPressSpeed)
                                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                gestureFeedback = "${String.format(Locale.US, "%.1f", state.interfaceSettings.longPressSpeed)}×"
                                                                gestureFeedbackEvent += 1
                                                            }
                                                            try {
                                                                tryAwaitRelease()
                                                            } finally {
                                                                speedJob.cancel()
                                                                if (boosted) viewModel.setTemporaryPlaybackSpeed(originalSpeed)
                                                            }
                                                        }
                                                    },
                                                    onDoubleTap = {
                                                        viewModel.seekBy(-seekStepMs)
                                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        mediaSeekFeedbackMs = -seekStepMs
                                                        mediaSeekFeedbackEvent += 1
                                                    }
                                                )
                                            }
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .width(54.dp)
                                            .fillMaxHeight()
                                            .pointerInput(track.id, playerActivity) {
                                                detectVerticalDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    val activity = playerActivity ?: return@detectVerticalDragGestures
                                                    val attributes = activity.window.attributes
                                                    val current = attributes.screenBrightness.takeIf { it >= 0f } ?: 0.5f
                                                    val updated = (current - dragAmount / size.height.coerceAtLeast(1)).coerceIn(0.05f, 1f)
                                                    attributes.screenBrightness = updated
                                                    activity.window.attributes = attributes
                                                    gestureFeedback = "${strings.brightness} ${(updated * 100f).roundToInt()}%"
                                                    gestureFeedbackEvent += 1
                                                }
                                            }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .pointerInput(track.id, state.interfaceSettings.doubleTapSeekSeconds, state.interfaceSettings.longPressSpeed) {
                                                detectTapGestures(
                                                    onPress = {
                                                        val originalSpeed = state.playbackSpeed
                                                        coroutineScope {
                                                            var boosted = false
                                                            val speedJob = launch {
                                                                delay(320L)
                                                                boosted = true
                                                                viewModel.setTemporaryPlaybackSpeed(state.interfaceSettings.longPressSpeed)
                                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                gestureFeedback = "${String.format(Locale.US, "%.1f", state.interfaceSettings.longPressSpeed)}×"
                                                                gestureFeedbackEvent += 1
                                                            }
                                                            try {
                                                                tryAwaitRelease()
                                                            } finally {
                                                                speedJob.cancel()
                                                                if (boosted) viewModel.setTemporaryPlaybackSpeed(originalSpeed)
                                                            }
                                                        }
                                                    },
                                                    onDoubleTap = {
                                                        viewModel.seekBy(seekStepMs)
                                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        mediaSeekFeedbackMs = seekStepMs
                                                        mediaSeekFeedbackEvent += 1
                                                    }
                                                )
                                            }
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .width(54.dp)
                                            .fillMaxHeight()
                                            .pointerInput(track.id, audioManager) {
                                                var accumulated = 0f
                                                detectVerticalDragGestures(
                                                    onDragStart = { accumulated = 0f },
                                                    onVerticalDrag = { change, dragAmount ->
                                                        change.consume()
                                                        val manager = audioManager ?: return@detectVerticalDragGestures
                                                        val maximum = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                                        accumulated += -dragAmount / size.height.coerceAtLeast(1) * maximum
                                                        val steps = accumulated.roundToInt()
                                                        if (steps != 0) {
                                                            val current = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                                            val updated = (current + steps).coerceIn(0, maximum)
                                                            manager.setStreamVolume(AudioManager.STREAM_MUSIC, updated, 0)
                                                            accumulated -= steps.toFloat()
                                                            gestureFeedback = "${strings.volume} ${((updated.toFloat() / maximum.toFloat()) * 100f).roundToInt()}%"
                                                            gestureFeedbackEvent += 1
                                                        }
                                                    }
                                                )
                                            }
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = gestureFeedback.isNotBlank(),
                            modifier = Modifier.align(Alignment.Center).zIndex(22f),
                            enter = fadeIn(animationSpec = tween(110)),
                            exit = fadeOut(animationSpec = tween(180))
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.74f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = gestureFeedback,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(if (mediaSeekFeedbackMs < 0L) Alignment.CenterStart else Alignment.CenterEnd)
                                .padding(horizontal = 30.dp)
                        ) {
                            AnimatedVisibility(
                                visible = mediaSeekFeedbackMs != 0L,
                                enter = fadeIn(animationSpec = tween(110)),
                                exit = fadeOut(animationSpec = tween(180))
                            ) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.72f),
                                    border = BorderStroke(1.dp, primary.copy(alpha = 0.34f)),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "${if (mediaSeekFeedbackMs < 0L) "−" else "+"}${kotlin.math.abs(mediaSeekFeedbackMs) / 1_000L} s",
                                        color = Color.White,
                                        fontSize = if (compactPlayer) 14.sp else 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    val isFavorite = track.id in state.favoriteIds
                    val favoriteFill = primary.copy(alpha = 0.42f)
                    val favoriteTint = remember(primaryTarget) {
                        Color.White.playerContentColor(
                            listOf(primaryTarget.copy(alpha = 0.42f).playerCompositeOver(PlayerDarkSurface))
                        )
                    }
                    val favoriteScale by animateFloatAsState(
                        targetValue = if (isFavorite) 1.08f else 1f,
                        animationSpec = if (state.animationsEnabled) {
                            LevyraPlayerDesign.expressiveSpring()
                        } else {
                            snap()
                        },
                        label = "player-favorite-scale"
                    )
                    val actionSize = if (compactPlayer) {
                        LevyraPlayerDesign.UtilityButtonCompact
                    } else {
                        LevyraPlayerDesign.UtilityButton
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LevyraPlayerDesign.SpaceXs, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    color = LevyraPlayerDesign.TextPrimary,
                                    fontSize = if (compactPlayer) 24.sp else 28.sp,
                                    lineHeight = if (compactPlayer) 26.sp else 30.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.4).sp,
                                    maxLines = if (state.animationsEnabled) 1 else 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = if (state.animationsEnabled) {
                                        Modifier.basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            repeatDelayMillis = 2_600
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .heightIn(min = 34.dp)
                                        .clip(LevyraPlayerDesign.ShapePill)
                                        .clickable(
                                            onClickLabel = strings.openArtist,
                                            onClick = { viewModel.openArtist(track) }
                                        )
                                        .padding(end = LevyraPlayerDesign.SpaceXs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXxs)
                                ) {
                                    Text(
                                        text = track.artist,
                                        color = LevyraPlayerDesign.TextSecondary,
                                        fontSize = if (compactPlayer) 15.sp else 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = LevyraPlayerDesign.TextTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(LevyraPlayerDesign.SpaceSm))
                            Row(horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXs)) {
                                PlayerGlassIconButton(
                                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                    contentDescription = strings.addToPlaylist,
                                    size = actionSize,
                                    iconSize = if (compactPlayer) 22.dp else 23.dp,
                                    tint = LevyraPlayerDesign.TextSecondary,
                                    onClick = { playlistTarget = track }
                                )
                                PlayerGlassIconButton(
                                    icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = strings.favoritesPlain,
                                    size = actionSize,
                                    iconSize = if (compactPlayer) 23.dp else 24.dp,
                                    tint = if (isFavorite) favoriteTint else LevyraPlayerDesign.TextSecondary,
                                    fill = if (isFavorite) favoriteFill else LevyraPlayerDesign.GlassFill,
                                    borderTop = if (isFavorite) {
                                        primary.playerMix(Color.White, 0.3f).copy(alpha = 0.7f)
                                    } else {
                                        LevyraPlayerDesign.GlassBorderTop
                                    },
                                    borderBottom = if (isFavorite) {
                                        primary.copy(alpha = 0.2f)
                                    } else {
                                        LevyraPlayerDesign.GlassBorderBottom
                                    },
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = favoriteScale
                                            scaleY = favoriteScale
                                        }
                                        .semantics { toggleableState = ToggleableState(isFavorite) },
                                    onClick = { viewModel.toggleFavorite(track) }
                                )
                            }
                        }
                        PlayerYoutubeEngagementRow(
                            track = track,
                            engagement = state.youtubeEngagement,
                            primary = primary,
                            secondary = secondary,
                            compact = compactPlayer,
                            onComments = viewModel::openYoutubeComments
                        )
                    }
                }
                item {
                    // PixelPlayer style progress (thicker visually)
                    PlayerTimeline(
                        positionMs = state.positionMs,
                        bufferedPositionMs = state.bufferedPositionMs,
                        durationMs = state.durationMs,
                        activeColor = primary,
                        secondaryColor = secondary,
                        isPlaying = state.isPlaying,
                        animationsEnabled = state.animationsEnabled,
                        compact = compactPlayer,
                        onSeek = viewModel::seekTo
                    )
                }
                item {
                    // Controls mimicking the PixelPlayer tidy expressive layout
                    PlayerTransportControls(
                        isPlaying = state.isPlaying,
                        isResolving = state.isResolving,
                        shuffleOn = state.shuffleEnabled,
                        repeatOn = state.repeatMode != RepeatMode.Off,
                        repeatOne = state.repeatMode == RepeatMode.One,
                        accents = playerAccentColors,
                        compact = compactPlayer,
                        animated = state.animationsEnabled,
                        labels = playerControlLabels,
                        onShuffle = viewModel::toggleShuffle,
                        onPrevious = viewModel::previous,
                        onToggle = viewModel::togglePlay,
                        onNext = viewModel::next,
                        onRepeat = viewModel::toggleRepeat,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                item {
                    LevyraControlPulseHandle(
                        expanded = advancedControlsExpanded,
                        compact = compactPlayer,
                        activeColor = primary,
                        secondaryColor = secondary,
                        hasActiveState = state.playbackSpeed != 1f || state.sleepTimerMinutes > 0 || state.isOfflineExporting,
                        onToggle = { advancedControlsExpanded = !advancedControlsExpanded }
                    )
                }
                item(key = "player-advanced-controls") {
                    PlayerAdvancedControlsPanel(
                        expanded = advancedControlsExpanded,
                        track = track,
                        state = state,
                        primary = primary,
                        secondary = secondary,
                        primaryContent = primaryContent,
                        secondaryContent = secondaryContent,
                        compact = compactPlayer,
                        strings = strings,
                        viewModel = viewModel,
                        onAddToPlaylist = { playlistTarget = track }
                    )
                }
                item { PlayerError(state.playerError) }
            }
        }
    }
}

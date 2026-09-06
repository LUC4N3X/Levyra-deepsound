package com.luc4n3x.levyra.ui.player

import android.app.Activity
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.toBitmap
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.data.ArtworkPaletteCache
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.domain.PlayerVisualMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.cast.CastRouteButton
import com.luc4n3x.levyra.player.LevyraPipBridge
import com.luc4n3x.levyra.ui.LevyraLayoutMode
import com.luc4n3x.levyra.ui.LevyraPlayerPane
import com.luc4n3x.levyra.ui.PlayerDarkSurface
import com.luc4n3x.levyra.ui.artwork.ArtworkPreviewOverlay
import com.luc4n3x.levyra.ui.artwork.livingArtworkColors
import com.luc4n3x.levyra.ui.components.PlayerAccentColors
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.PlayerGlassIconButton
import com.luc4n3x.levyra.ui.components.playerGlass
import com.luc4n3x.levyra.ui.harmonizePlayerAccents
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.levyraContentMaxWidthDp
import com.luc4n3x.levyra.ui.levyraFoldAwareGutterDp
import com.luc4n3x.levyra.ui.levyraPlayerArtworkMaxWidthDp
import com.luc4n3x.levyra.ui.playerAmbienceOf
import com.luc4n3x.levyra.ui.playerContentColor
import com.luc4n3x.levyra.ui.playerMix
import com.luc4n3x.levyra.ui.preferredPlayerArtworkUrl
import com.luc4n3x.levyra.ui.resolveLevyraLayoutMode
import com.luc4n3x.levyra.ui.resolvePlayerPane
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun LevyraNowPlaying(
    viewModel: PlayerViewModel,
    state: LevyraUiState,
    morphAnchors: PlayerMorphAnchors,
    morphActive: Boolean,
    collapseActions: PlayerCollapseActions,
    modifier: Modifier = Modifier,
    modeSwitchContent: (@Composable () -> Unit)? = null,
    videoSurfaceContent: (@Composable (Track, Modifier) -> Unit)? = null,
    gestureLayerContent: (@Composable (Track, PlayerGestureConfig, PlayerGestureMediaActions, PlayerGestureUiActions, Modifier) -> Unit)? = null,
    engagementContent: (@Composable (Track) -> Unit)? = null,
    optionsMenuContent: (@Composable () -> Unit)? = null,
    similarSongsContent: (@Composable (Track) -> Unit)? = null,
    errorContent: (@Composable () -> Unit)? = null,
    playlistDialogContent: (@Composable () -> Unit)? = null,
    onOpenPlaylistDialog: ((Track) -> Unit)? = null
) {
    val strings = LocalLevyraStrings.current
    val track = state.currentTrack
    val playerContext = LocalContext.current
    val playerActivity = playerContext as? Activity
    val audioManager = remember(playerContext) { playerContext.getSystemService(AudioManager::class.java) }
    val hapticFeedback = LocalLevyraHaptics.current
    val rightToLeft = LocalLayoutDirection.current == LayoutDirection.Rtl
    val artworkUrl = track?.let(::preferredPlayerArtworkUrl).orEmpty()
    val visualMode = state.interfaceSettings.playerVisualMode
    val backgroundMode = state.interfaceSettings.playerBackground

    val fallbackPalette = remember(track?.accentStart, track?.accentEnd) {
        ArtworkPalette(track?.accentStart ?: LevyraCyan.toArgb(), track?.accentEnd ?: LevyraViolet.toArgb())
    }
    val paletteKey = remember(track?.id, track?.thumbnailUrl, track?.largeThumbnailUrl) {
        if (track != null) {
            ArtworkPaletteCache.key(
                trackId = track.id,
                thumbnailUrl = track.thumbnailUrl,
                largeThumbnailUrl = track.largeThumbnailUrl
            )
        } else ""
    }
    val memoryPalette = remember(paletteKey) {
        if (paletteKey.isNotBlank()) ArtworkPaletteCache.peek(paletteKey) else null
    }
    val artworkPaletteState = remember(paletteKey) {
        mutableStateOf(memoryPalette ?: fallbackPalette)
    }
    var cacheLookupComplete by remember(paletteKey) {
        mutableStateOf(memoryPalette != null)
    }
    var paletteExtractionStarted by remember(paletteKey) {
        mutableStateOf(memoryPalette != null)
    }

    LaunchedEffect(paletteKey) {
        if (paletteKey.isNotBlank() && memoryPalette == null) {
            val persistedPalette = ArtworkPaletteCache.load(playerContext, paletteKey)
            if (persistedPalette != null) {
                artworkPaletteState.value = persistedPalette
                paletteExtractionStarted = true
            }
            cacheLookupComplete = true
        }
    }

    LaunchedEffect(paletteKey, cacheLookupComplete) {
        if (paletteKey.isBlank() || !cacheLookupComplete || paletteExtractionStarted) return@LaunchedEffect
        val currentTrack = track ?: return@LaunchedEffect
        val artUrl = artworkUrl.ifBlank {
            currentTrack.largeThumbnailUrl.ifBlank { currentTrack.thumbnailUrl }
        }
        if (artUrl.isBlank()) return@LaunchedEffect
        paletteExtractionStarted = true
        withContext(Dispatchers.IO) {
            val imageLoader = coil3.SingletonImageLoader.get(playerContext)
            val request = ImageRequest.Builder(playerContext)
                .data(LevyraArtworkCache.small(artUrl))
                .size(96, 96)
                .allowHardware(false)
                .bitmapConfig(android.graphics.Bitmap.Config.ARGB_8888)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build()
            val bitmap = runCatching {
                imageLoader.execute(request).image?.toBitmap()
            }.getOrNull()
            if (bitmap != null) {
                val extracted = withContext(Dispatchers.Default) {
                    ArtworkPaletteCache.extract(
                        bitmap = bitmap,
                        fallbackStart = fallbackPalette.start,
                        fallbackEnd = fallbackPalette.end
                    )
                }
                artworkPaletteState.value = extracted
                ArtworkPaletteCache.store(playerContext, paletteKey, extracted)
            }
        }
    }

    val activePalette = artworkPaletteState.value
    val rawPrimaryTarget = Color(activePalette.start)
    val rawSecondaryTarget = Color(activePalette.end)
    val harmonizedTargets = remember(rawPrimaryTarget, rawSecondaryTarget) {
        harmonizePlayerAccents(rawPrimaryTarget, rawSecondaryTarget)
    }
    val primaryTarget = harmonizedTargets.primary
    val secondaryTarget = harmonizedTargets.secondary
    val primary by animateColorAsState(
        targetValue = primaryTarget,
        animationSpec = if (state.animationsEnabled) tween(650, easing = LinearOutSlowInEasing) else snap(),
        label = "player-primary-color"
    )
    val secondary by animateColorAsState(
        targetValue = secondaryTarget,
        animationSpec = if (state.animationsEnabled) tween(650, easing = LinearOutSlowInEasing) else snap(),
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
    val ambience = remember(primaryTarget, secondaryTarget) {
        playerAmbienceOf(primaryTarget, secondaryTarget)
    }
    val livingArtwork = remember(primaryTarget, secondaryTarget) {
        livingArtworkColors(primaryTarget, secondaryTarget)
    }
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
    var mediaSeekFeedbackMs by remember(track?.id) { mutableStateOf(0L) }
    var mediaSeekFeedbackEvent by remember(track?.id) { mutableIntStateOf(0) }
    var gestureFeedback by remember(track?.id) { mutableStateOf("") }
    var gestureFeedbackEvent by remember(track?.id) { mutableIntStateOf(0) }
    var swipeOffsetPx by remember(track?.id) { mutableFloatStateOf(0f) }
    val settledSwipeOffset by animateFloatAsState(
        targetValue = swipeOffsetPx,
        animationSpec = if (state.animationsEnabled) LevyraPlayerDesign.smoothSpring() else snap(),
        label = "player-swipe-offset"
    )

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
        targetValue = if (state.isPlaying) LevyraPlayerDesign.CornerLg else LevyraPlayerDesign.CornerXl,
        animationSpec = if (state.animationsEnabled) LevyraPlayerDesign.expressiveSpring() else snap(),
        label = "artwork-corner"
    )
    val artShadow by animateFloatAsState(
        targetValue = if (state.isPlaying) 28f else 14f,
        animationSpec = if (state.animationsEnabled) LevyraPlayerDesign.smoothSpring() else snap(),
        label = "artwork-shadow"
    )
    val artOffset by animateDpAsState(
        targetValue = if (state.isPlaying) 0.dp else 4.dp,
        animationSpec = if (state.animationsEnabled) LevyraPlayerDesign.expressiveSpring() else snap(),
        label = "artwork-offset"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val layoutMode = resolveLevyraLayoutMode(maxWidth.value, maxHeight.value)
        val playerPane = if (state.isVideoMode) {
            LevyraPlayerPane.Stacked
        } else {
            resolvePlayerPane(maxWidth.value, maxHeight.value)
        }
        val compactPlayer = layoutMode == LevyraLayoutMode.Compact && (maxWidth < 380.dp || maxHeight < 700.dp)
        val playerHorizontalPadding = if (state.isVideoMode) {
            LevyraPlayerDesign.SpaceSm
        } else {
            levyraFoldAwareGutterDp(layoutMode, compactPlayer).dp
        }
        val playerItemSpacing = if (compactPlayer) LevyraPlayerDesign.SpaceSm else LevyraPlayerDesign.SpaceMd
        val paneCount = if (playerPane == LevyraPlayerPane.SideBySide) 2f else 1f

        val phoneUsableWidth = maxWidth - playerHorizontalPadding * 2f
        val targetContainedWidth = if (playerPane == LevyraPlayerPane.SideBySide) {
            (phoneUsableWidth / 2f).coerceAtLeast(180.dp)
        } else {
            phoneUsableWidth * 0.78f
        }
        val artworkSize = minOf(
            targetContainedWidth,
            levyraPlayerArtworkMaxWidthDp(playerPane, layoutMode).dp,
            (maxHeight * 0.40f).coerceAtLeast(180.dp)
        )
        val detailMaxWidth = levyraContentMaxWidthDp(layoutMode).dp

        val artworkPreviewAvailable = !state.isVideoMode && artworkUrl.isNotBlank() && visualMode == PlayerVisualMode.Artwork
        var showArtworkPreview by remember(track?.id, state.isVideoMode) { mutableStateOf(false) }

        PlayerVisualHost(
            visualMode = visualMode,
            backgroundMode = backgroundMode,
            track = track,
            artworkUrl = artworkUrl,
            motionArtwork = state.motionArtwork,
            livingArtwork = livingArtwork,
            ambience = ambience,
            animationsEnabled = state.animationsEnabled,
            isPlaying = state.isPlaying,
            canvasQuality = state.interfaceSettings.canvasQuality,
            morphActive = morphActive,
            swipeOffset = settledSwipeOffset,
            modifier = Modifier.fillMaxSize()
        )

        val headerBlock: @Composable () -> Unit = {
            val headerButtonSize = if (compactPlayer) {
                LevyraPlayerDesign.HeaderButtonCompact
            } else {
                LevyraPlayerDesign.HeaderButton
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LevyraPlayerDesign.MinimumTouchTarget)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerGlassIconButton(
                        icon = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = strings.collapsePlayer,
                        size = headerButtonSize,
                        iconSize = if (compactPlayer) 22.dp else 24.dp,
                        onClick = collapseActions.collapse
                    )
                }
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    if (track != null && (track.videoUrl.isNotBlank() || track.counterpartVideoId.isNotBlank())) {
                        modeSwitchContent?.invoke()
                    } else {
                        Box(
                            modifier = Modifier
                                .playerGlass(
                                    shape = CircleShape,
                                    fill = Color.Black.copy(alpha = 0.35f),
                                    borderTop = Color.White.copy(alpha = 0.14f),
                                    borderBottom = Color.White.copy(alpha = 0.08f)
                                )
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = strings.formatPlayingFrom(track?.source ?: "LEVYRA"),
                                color = LevyraPlayerDesign.TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
                ) {
                    if (visualMode == PlayerVisualMode.CanvasImmersive && !state.isVideoMode) {
                        PlayerGlassIconButton(
                            icon = Icons.Rounded.CloseFullscreen,
                            contentDescription = strings.exitImmersive,
                            size = headerButtonSize,
                            iconSize = if (compactPlayer) 20.dp else 22.dp,
                            tint = primary,
                            onClick = { viewModel.setPlayerVisualMode(PlayerVisualMode.CanvasCard) }
                        )
                    }
                    if (!state.isVideoMode) {
                        CastRouteButton(modifier = Modifier.size(headerButtonSize))
                    }
                    if (state.isVideoMode) {
                        if (track?.videoSubtitleTracks?.isNotEmpty() == true) {
                            var subtitleMenuExpanded by remember(track.id) { mutableStateOf(false) }
                            Box {
                                PlayerGlassIconButton(
                                    icon = Icons.Rounded.Subtitles,
                                    contentDescription = strings.subtitlesLabel,
                                    size = headerButtonSize,
                                    iconSize = 19.dp,
                                    tint = if (state.selectedVideoSubtitleId != null) primary else Color.White.copy(alpha = 0.72f),
                                    borderTop = primary.copy(alpha = 0.48f),
                                    borderBottom = primary.copy(alpha = 0.14f),
                                    onClick = { subtitleMenuExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = subtitleMenuExpanded,
                                    onDismissRequest = { subtitleMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(strings.subtitlesOff) },
                                        leadingIcon = {
                                            if (state.selectedVideoSubtitleId == null) {
                                                Icon(Icons.Rounded.Check, contentDescription = null)
                                            }
                                        },
                                        onClick = {
                                            subtitleMenuExpanded = false
                                            viewModel.selectVideoSubtitle(null)
                                        }
                                    )
                                    track.videoSubtitleTracks.forEach { subtitle ->
                                        DropdownMenuItem(
                                            text = { Text(subtitle.label.ifBlank { subtitle.languageCode }) },
                                            leadingIcon = {
                                                if (state.selectedVideoSubtitleId == subtitle.id) {
                                                    Icon(Icons.Rounded.Check, contentDescription = null)
                                                }
                                            },
                                            onClick = {
                                                subtitleMenuExpanded = false
                                                viewModel.selectVideoSubtitle(subtitle.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        PlayerGlassIconButton(
                            icon = Icons.Rounded.PictureInPictureAlt,
                            contentDescription = strings.pictureInPicture,
                            size = headerButtonSize,
                            iconSize = 19.dp,
                            borderTop = primary.copy(alpha = 0.48f),
                            borderBottom = primary.copy(alpha = 0.14f),
                            onClick = { LevyraPipBridge.enter() }
                        )
                    }
                    PlayerGlassIconButton(
                        icon = Icons.Rounded.MoreVert,
                        contentDescription = strings.options,
                        size = headerButtonSize,
                        iconSize = if (compactPlayer) 20.dp else 21.dp,
                        onClick = { viewModel.openAudioQualityPanel() }
                    )
                }
            }
        }

        val mediaHeroBlock: @Composable (Track) -> Unit = { activeTrack ->
            Box(
                modifier = if (playerPane == LevyraPlayerPane.SideBySide) {
                    Modifier
                        .size(width = artworkSize, height = artworkSize)
                        .padding(vertical = if (compactPlayer) 1.dp else 2.dp)
                } else {
                    Modifier
                        .size(width = artworkSize, height = artworkSize)
                        .padding(vertical = if (compactPlayer) 2.dp else 4.dp)
                },
                contentAlignment = Alignment.Center
            ) {
                if (state.isVideoMode && activeTrack.videoUrl.isNotBlank() && videoSurfaceContent != null) {
                    videoSurfaceContent(
                        activeTrack,
                        Modifier
                            .fillMaxSize()
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
                } else {
                    PlayerArtworkHero(
                        track = activeTrack,
                        artworkUrl = artworkUrl,
                        visualMode = visualMode,
                        motionArtwork = state.motionArtwork,
                        livingArtwork = livingArtwork,
                        animationsEnabled = state.animationsEnabled && !state.isVideoMode,
                        isPlaying = state.isPlaying,
                        cornerRadius = artCorner,
                        canvasQuality = state.interfaceSettings.canvasQuality,
                        morphAnchors = morphAnchors,
                        morphActive = morphActive,
                        swipeOffset = settledSwipeOffset,
                        artScale = artScale,
                        artOffset = artOffset,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (state.interfaceSettings.playerGesturesEnabled && gestureLayerContent != null) {
                    gestureLayerContent(
                        activeTrack,
                        PlayerGestureConfig(
                            trackId = activeTrack.id,
                            settings = state.interfaceSettings,
                            playbackSpeed = state.playbackSpeed,
                            environment = PlayerGestureEnvironment(
                                activity = playerActivity,
                                audioManager = audioManager,
                                brightnessLabel = strings.brightness,
                                volumeLabel = strings.volume,
                                rightToLeft = rightToLeft
                            )
                        ),
                        PlayerGestureMediaActions(
                            seekBy = { delta ->
                                viewModel.seekBy(delta)
                                hapticFeedback.perform(LevyraHapticAction.TrackSwipe)
                                mediaSeekFeedbackMs = delta
                                mediaSeekFeedbackEvent += 1
                            },
                            next = viewModel::next,
                            previous = viewModel::previous,
                            swipeOffset = { swipeOffsetPx = it },
                            temporarySpeed = viewModel::setTemporaryPlaybackSpeed
                        ),
                        PlayerGestureUiActions(
                            feedback = { message ->
                                gestureFeedback = message
                                gestureFeedbackEvent += 1
                            },
                            haptic = {
                                hapticFeedback.perform(LevyraHapticAction.TrackSwipe)
                            },
                            collapse = collapseActions,
                            artworkPreview = if (artworkPreviewAvailable) {
                                { showArtworkPreview = true }
                            } else {
                                null
                            }
                        ),
                        Modifier
                            .matchParentSize()
                            .zIndex(20f)
                    )
                }

                if (showArtworkPreview && artworkPreviewAvailable) {
                    ArtworkPreviewOverlay(
                        artworkUrl = artworkUrl,
                        title = activeTrack.title,
                        previewLabel = strings.artworkPreview,
                        closeLabel = strings.close,
                        saveLabel = strings.saveArtwork,
                        savedMessage = strings.artworkSaved,
                        saveFailedMessage = strings.artworkSaveFailed,
                        onFeedback = { message ->
                            gestureFeedback = message
                            gestureFeedbackEvent += 1
                        },
                        onDismiss = { showArtworkPreview = false }
                    )
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
            }
        }

        val metadataBlock: @Composable (Track) -> Unit = { activeTrack ->
            val isFavorite = activeTrack.id in state.favoriteIds
            val favoriteScale by animateFloatAsState(
                targetValue = if (isFavorite) 1.05f else 1f,
                animationSpec = if (state.animationsEnabled) {
                    LevyraPlayerDesign.snappySpring()
                } else {
                    snap()
                },
                label = "player-favorite-scale"
            )
            val favoriteTint = if (isFavorite) primary else Color.White.copy(alpha = 0.88f)
            val favoriteFill = if (isFavorite) primary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f)
            val favoriteBorderTop = if (isFavorite) primary.copy(alpha = 0.44f) else Color.White.copy(alpha = 0.14f)
            val favoriteBorderBottom = if (isFavorite) primary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f)

            PlayerTrackMetadata(
                track = activeTrack,
                isFavorite = isFavorite,
                favoriteScale = favoriteScale,
                favoriteTint = favoriteTint,
                favoriteFill = favoriteFill,
                favoriteBorderTop = favoriteBorderTop,
                favoriteBorderBottom = favoriteBorderBottom,
                animationsEnabled = state.animationsEnabled,
                compact = compactPlayer,
                openArtistLabel = strings.openArtist,
                favoritesLabel = strings.favoritesPlain,
                addToPlaylistLabel = strings.addToPlaylist,
                onArtistClick = { viewModel.openArtist(activeTrack) },
                onToggleFavorite = { viewModel.toggleFavorite(activeTrack) },
                onAddToPlaylist = { onOpenPlaylistDialog?.invoke(activeTrack) },
                engagementContent = if (engagementContent != null) {
                    { engagementContent(activeTrack) }
                } else null
            )
        }

        val progressBlock: @Composable () -> Unit = {
            PlayerProgress(
                positionMs = state.positionMs,
                bufferedPositionMs = state.bufferedPositionMs,
                durationMs = state.durationMs,
                activeColor = Color.White.copy(alpha = 0.94f),
                secondaryColor = Color.White.copy(alpha = 0.62f),
                isPlaying = state.isPlaying,
                animationsEnabled = state.animationsEnabled,
                compact = compactPlayer,
                onSeek = viewModel::seekTo
            )
        }

        val transportBlock: @Composable () -> Unit = {
            PlayerTransportBar(
                isPlaying = state.isPlaying,
                isResolving = state.isResolving,
                shuffleOn = state.shuffleEnabled,
                repeatMode = state.repeatMode,
                accents = playerAccentColors,
                compact = compactPlayer,
                animated = state.animationsEnabled,
                labels = playerControlLabels,
                onShuffle = viewModel::toggleShuffle,
                onPrevious = viewModel::previous,
                onTogglePlay = viewModel::togglePlay,
                onNext = viewModel::next,
                onRepeat = viewModel::toggleRepeat
            )
        }

        val quickActionsBlock: @Composable (Track) -> Unit = { activeTrack ->
            val isDownloaded = activeTrack.id in state.downloadedTrackIds
            val optionsActive = state.playbackSpeed != 1f ||
                state.sleepTimerMinutes > 0 ||
                state.sleepTimerEndOfTrack ||
                state.audioNormalization
            var optionsExpanded by remember(activeTrack.id) { mutableStateOf(false) }

            PlayerQuickActions(
                visualMode = visualMode,
                motionCanvasAvailable = !state.isVideoMode,
                showLyrics = state.showLyrics,
                isDownloaded = isDownloaded,
                isExporting = state.isOfflineExporting,
                optionsActive = optionsActive,
                primaryColor = primary,
                secondaryColor = secondary,
                compact = compactPlayer,
                queueLabel = strings.queue,
                lyricsLabel = strings.lyrics,
                visualModeLabel = when (visualMode) {
                    PlayerVisualMode.Artwork -> strings.playerVisualModeArtwork
                    PlayerVisualMode.CanvasCard -> strings.playerVisualModeCanvasCard
                    PlayerVisualMode.CanvasImmersive -> strings.playerVisualModeCanvasImmersive
                },
                downloadLabel = when {
                    state.isOfflineExporting -> strings.downloadInProgress
                    isDownloaded -> strings.downloaded
                    else -> strings.download
                },
                optionsLabel = strings.options,
                onQueueClick = viewModel::openQueue,
                onLyricsClick = viewModel::openLyrics,
                onCycleVisualMode = {
                    val nextMode = when (visualMode) {
                        PlayerVisualMode.Artwork -> PlayerVisualMode.CanvasCard
                        PlayerVisualMode.CanvasCard -> PlayerVisualMode.CanvasImmersive
                        PlayerVisualMode.CanvasImmersive -> PlayerVisualMode.Artwork
                    }
                    viewModel.setPlayerVisualMode(nextMode)
                    hapticFeedback.perform(LevyraHapticAction.Confirm)
                },
                onDownloadClick = viewModel::exportCurrentTrack,
                onOptionsClick = { optionsExpanded = !optionsExpanded },
                optionsMenuContent = if (optionsMenuContent != null) {
                    optionsMenuContent
                } else null
            )
        }

        if (playerPane == LevyraPlayerPane.SideBySide && track != null) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = detailMaxWidth)
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = playerHorizontalPadding, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(playerItemSpacing)
            ) {
                headerBlock()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXl),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        mediaHeroBlock(track)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(playerItemSpacing, Alignment.CenterVertically)
                    ) {
                        metadataBlock(track)
                        progressBlock()
                        transportBlock()
                        quickActionsBlock(track)
                        similarSongsContent?.invoke(track)
                        errorContent?.invoke()
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = detailMaxWidth)
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(
                        start = playerHorizontalPadding,
                        end = playerHorizontalPadding,
                        top = if (compactPlayer) 6.dp else 10.dp,
                        bottom = if (compactPlayer) 12.dp else 18.dp
                    )
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(playerItemSpacing)
            ) {
                headerBlock()
                if (track == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.emptyPlayer,
                            color = LevyraPlayerDesign.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = if (compactPlayer) 2.dp else 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        mediaHeroBlock(track)
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(if (compactPlayer) 6.dp else 10.dp)
                    ) {
                        metadataBlock(track)
                        progressBlock()
                        transportBlock()
                        quickActionsBlock(track)
                        similarSongsContent?.invoke(track)
                        errorContent?.invoke()
                    }
                }
            }
        }

        playlistDialogContent?.invoke()
    }
}

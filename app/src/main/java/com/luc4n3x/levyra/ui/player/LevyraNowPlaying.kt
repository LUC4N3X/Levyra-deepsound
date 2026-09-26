package com.luc4n3x.levyra.ui.player

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.data.ArtworkPaletteCache
import com.luc4n3x.levyra.data.PlaybackSourceIdentity
import com.luc4n3x.levyra.data.youtubeWatchUrl
import com.luc4n3x.levyra.domain.PlayerBackgroundMode
import com.luc4n3x.levyra.domain.PlayerVisualMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.cast.CastRouteButton
import com.luc4n3x.levyra.feature.radio.isLiveRadio
import com.luc4n3x.levyra.player.LevyraPipBridge
import com.luc4n3x.levyra.ui.LevyraLayoutMode
import com.luc4n3x.levyra.ui.LevyraPlayerPane
import com.luc4n3x.levyra.ui.artwork.ArtworkPreviewOverlay
import com.luc4n3x.levyra.ui.artwork.livingArtworkColors
import com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.PlayerGlassIconButton
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.harmonizePlayerAccents
import com.luc4n3x.levyra.ui.i18n.LevyraLiveRadioCatalog
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.technicalAudioInfoCopy
import com.luc4n3x.levyra.ui.levyraContentMaxWidthDp
import com.luc4n3x.levyra.ui.levyraFoldAwareGutterDp
import com.luc4n3x.levyra.ui.levyraPlayerArtworkMaxWidthDp
import com.luc4n3x.levyra.ui.playerAmbienceOf
import com.luc4n3x.levyra.ui.preferredPlayerArtworkUrl
import com.luc4n3x.levyra.ui.resolveLevyraLayoutMode
import com.luc4n3x.levyra.ui.resolvePlayerPane
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraPlayerShapes
import com.luc4n3x.levyra.ui.theme.LevyraSegment
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val MinimumFittedPlayerHeight = 600.dp
private val ScrollingArtworkMax = 260.dp
private val HeaderSideReserve = 108.dp
private val HeaderButtonMinimumWidth = 40.dp
private const val DefaultBackdropFocus = 0.34f

@Composable
fun LevyraNowPlaying(
    viewModel: PlayerViewModel,
    state: LevyraUiState,
    morphAnchors: PlayerMorphAnchors,
    morphActive: Boolean,
    collapseActions: PlayerCollapseActions,
    modifier: Modifier = Modifier,
    motionSuspended: Boolean = false,
    modeSwitchContent: (@Composable () -> Unit)? = null,
    videoSurfaceContent: (@Composable (Track, Modifier) -> Unit)? = null,
    gestureLayerContent: (@Composable (Track, PlayerGestureConfig, PlayerGestureMediaActions, PlayerGestureUiActions, androidx.compose.runtime.MutableState<PlayerVideoTransform>?, Modifier) -> Unit)? = null,
    engagementContent: (@Composable (Track) -> Unit)? = null,
    similarSongsContent: (@Composable (Track) -> Unit)? = null,
    errorContent: (@Composable () -> Unit)? = null,
    playlistDialogContent: (@Composable () -> Unit)? = null,
    onOpenPlaylistDialog: ((Track) -> Unit)? = null
) {
    val strings = LocalLevyraStrings.current
    val track = state.currentTrack
    val liveRadio = track?.isLiveRadio() == true
    val liveRadioStrings = LevyraLiveRadioCatalog.forCode(strings.code)
    val playerContext = LocalContext.current
    val playerActivity = playerContext as? Activity
    val audioManager = remember(playerContext) { playerContext.getSystemService(AudioManager::class.java) }
    val hapticFeedback = LocalLevyraHaptics.current
    val density = LocalDensity.current
    val rightToLeft = LocalLayoutDirection.current == LayoutDirection.Rtl
    val artworkUrl = track?.let(::preferredPlayerArtworkUrl).orEmpty()
    val visualMode = state.interfaceSettings.playerVisualMode
    val backgroundMode = state.interfaceSettings.playerBackground.let { selected ->
        if (selected == PlayerBackgroundMode.Dark && state.interfaceSettings.pureBlack) {
            PlayerBackgroundMode.PureBlack
        } else {
            selected
        }
    }
    val animated = state.animationsEnabled

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
    val paletteArtworkUrl = track?.let { current ->
        artworkUrl.ifBlank { current.largeThumbnailUrl.ifBlank { current.thumbnailUrl } }
    }.orEmpty()
    val activePalette by rememberArtworkPalette(
        paletteKey = paletteKey,
        artworkUrl = paletteArtworkUrl,
        fallback = fallbackPalette
    )
    val motionEnabled = animated && !state.isVideoMode && !motionSuspended
    val rawPrimaryTarget = Color(activePalette.start)
    val rawSecondaryTarget = Color(activePalette.end)
    val harmonizedTargets = remember(rawPrimaryTarget, rawSecondaryTarget) {
        harmonizePlayerAccents(rawPrimaryTarget, rawSecondaryTarget)
    }
    val primaryTarget = harmonizedTargets.primary
    val secondaryTarget = harmonizedTargets.secondary
    val primary by animateColorAsState(
        targetValue = primaryTarget,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-primary-color"
    )
    val secondary by animateColorAsState(
        targetValue = secondaryTarget,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-secondary-color"
    )
    val amoled = backgroundMode == PlayerBackgroundMode.PureBlack
    val surfaces = remember(primaryTarget, amoled) { playerSurfaceTokens(primaryTarget, amoled) }
    val heroTone by animateColorAsState(
        targetValue = surfaces.hero,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-hero-tone"
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
    var showActions by remember { mutableStateOf(false) }
    var showTechnicalAudioInfo by remember(track?.id) { mutableStateOf(false) }
    var showDeck by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.isVideoMode, track == null) {
        if (state.isVideoMode || track == null) showDeck = false
    }
    var mediaSeekFeedbackMs by remember(track?.id) { mutableStateOf(0L) }
    var mediaSeekFeedbackEvent by remember(track?.id) { mutableIntStateOf(0) }
    var gestureFeedback by remember(track?.id) { mutableStateOf("") }
    var gestureFeedbackEvent by remember(track?.id) { mutableIntStateOf(0) }
    val swipe = rememberPlayerSwipeMotion(animated)
    val swipeOffset: () -> Float = { swipe.offsetFor(track?.id) }
    val stepDirection = rememberTrackStepDirection(
        trackId = track?.id,
        queue = state.queue,
        queueIndex = state.queueCurrentIndex
    )
    val lyricsFlip = rememberPlayerLyricsFlipState()
    val lyricsFlipDepth = LocalLevyraVisualCapabilities.current.depthTransitions
    val lyricsFlipAvailable = track != null && !liveRadio && !state.isVideoMode
    LaunchedEffect(lyricsFlipAvailable, morphActive) {
        if (!lyricsFlipAvailable || morphActive) lyricsFlip.snapTo(PlayerLyricsFace.Player)
    }
    val lyricsFlipSwipeModifier = Modifier.playerLyricsFlipDrag(
        state = lyricsFlip,
        enabled = lyricsFlipAvailable,
        rightToLeft = rightToLeft,
        depth = lyricsFlipDepth
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

    val artRestScale = if (state.isPlaying) 1f else LevyraPlayerDesign.ArtworkPausedScale
    val artRestOffset = if (state.isPlaying) 0.dp else 4.dp
    SideEffect {
        morphAnchors.updateFullRest(artRestScale, with(density) { artRestOffset.toPx() })
    }
    val artScale by animateFloatAsState(
        targetValue = artRestScale,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.expressiveSpring()),
        label = "artwork-scale"
    )
    val artOffset by animateDpAsState(
        targetValue = artRestOffset,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.expressiveSpring()),
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
        val deckLayout = resolvePlayerDeckLayout(
            mode = visualMode,
            isVideoMode = state.isVideoMode,
            isLiveRadio = liveRadio,
            pane = playerPane,
            hasTrack = track != null
        )
        val deckMode = if (liveRadio) PlayerVisualMode.Artwork else resolvePlayerDeckVisualMode(visualMode, deckLayout)
        val compactPlayer = layoutMode == LevyraLayoutMode.Compact && (maxWidth < 380.dp || maxHeight < 720.dp)
        val fitsViewport = maxHeight >= MinimumFittedPlayerHeight || playerPane == LevyraPlayerPane.SideBySide
        val gutter = if (state.isVideoMode) {
            LevyraPlayerDesign.SpaceMd
        } else {
            levyraFoldAwareGutterDp(layoutMode, compactPlayer).dp
        }
        val detailMaxWidth = levyraContentMaxWidthDp(layoutMode).dp
        val artworkCap = levyraPlayerArtworkMaxWidthDp(playerPane, layoutMode).dp
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val scrollingArtworkHeight = min(maxWidth - gutter * 2, ScrollingArtworkMax)

        val artworkPreviewAvailable = !liveRadio && !state.isVideoMode && artworkUrl.isNotBlank() && deckMode == PlayerVisualMode.Artwork
        var showArtworkPreview by remember(track?.id, state.isVideoMode) { mutableStateOf(false) }
        var videoFullscreen by remember(track?.id, state.isVideoMode) { mutableStateOf(false) }
        val videoTransform = remember(track?.id, state.isVideoMode) {
            mutableStateOf(PlayerVideoTransform.None)
        }
        BackHandler(enabled = videoFullscreen) {
            videoFullscreen = false
        }

        var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
        var stageTopPx by remember { mutableIntStateOf(-1) }
        var stageBottomPx by remember { mutableIntStateOf(-1) }
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val estimatedHeroBottom = playerCinematicStackedHeroBottom(
            statusBarTop = statusBarTop,
            chromeTopPadding = LevyraPlayerDesign.SpaceXs,
            headerHeight = LevyraPlayerDesign.MinimumTouchTarget,
            itemSpacing = LevyraPlayerDesign.SpaceLg,
            heroVerticalPadding = 0.dp,
            artworkSize = min(maxWidth - gutter * 2, maxHeight * 0.42f)
        )
        val measuredHeroBottom = if (stageBottomPx > 0) {
            with(density) { stageBottomPx.toDp() } + PlayerCinematicTitleOverlap
        } else {
            estimatedHeroBottom
        }
        val cinematicGeometry = playerCinematicGeometry(
            pane = playerPane,
            containerWidth = maxWidth,
            containerHeight = maxHeight,
            stackedHeroBottom = measuredHeroBottom,
            paneGap = LevyraPlayerDesign.SpaceXl
        )
        val backdropFocus = if (stageBottomPx > 0 && containerHeightPx > 0f) {
            ((stageTopPx + stageBottomPx) / 2f / containerHeightPx).coerceIn(0.1f, 0.9f)
        } else {
            DefaultBackdropFocus
        }

        PlayerVisualHost(
            visualMode = deckMode,
            backgroundMode = backgroundMode,
            track = track,
            artworkUrl = artworkUrl,
            motionArtwork = state.motionArtwork,
            livingArtwork = livingArtwork,
            ambience = ambience,
            animationsEnabled = animated,
            motionEnabled = motionEnabled,
            isPlaying = state.isPlaying,
            canvasQuality = state.interfaceSettings.canvasQuality,
            morphAnchors = morphAnchors,
            morphActive = morphActive,
            swipeOffset = swipeOffset,
            cinematicGeometry = cinematicGeometry,
            isVideoMode = state.isVideoMode,
            backdropFocus = backdropFocus,
            modifier = Modifier.fillMaxSize()
        )

        val headerButtonFill = surfaces.controlQuiet
        val headerButtonBorder = if (surfaces.amoled) surfaces.outline else Color.Transparent
        val headerButtonSize = if (compactPlayer) LevyraPlayerDesign.HeaderButtonCompact else LevyraPlayerDesign.HeaderButton

        val headerTrailingCount = listOf(
            !state.isVideoMode && !liveRadio,
            state.isVideoMode,
            state.isVideoMode && track?.videoSubtitleTracks?.isNotEmpty() == true,
            state.isVideoMode && state.videoQuality.available,
            track != null
        ).count { it }
        val headerSlotWidth = maxOf(headerButtonSize, HeaderButtonMinimumWidth)
        val headerTrailingWidth = headerSlotWidth * headerTrailingCount +
            LevyraPlayerDesign.SpaceXs * (headerTrailingCount - 1).coerceAtLeast(0)
        val headerCentered = headerTrailingWidth <= HeaderSideReserve

        val headerLeading: @Composable () -> Unit = {
            PlayerGlassIconButton(
                icon = Icons.Rounded.KeyboardArrowDown,
                contentDescription = strings.collapsePlayer,
                size = headerButtonSize,
                iconSize = 24.dp,
                fill = headerButtonFill,
                borderTop = headerButtonBorder,
                borderBottom = headerButtonBorder,
                onClick = collapseActions.collapse
            )
            if (deckMode == PlayerVisualMode.CanvasImmersive && !state.isVideoMode) {
                PlayerGlassIconButton(
                    icon = Icons.Rounded.CloseFullscreen,
                    contentDescription = strings.exitImmersive,
                    size = headerButtonSize,
                    iconSize = 20.dp,
                    tint = surfaces.activeContent,
                    fill = headerButtonFill,
                    borderTop = headerButtonBorder,
                    borderBottom = headerButtonBorder,
                    onClick = { viewModel.setPlayerVisualMode(PlayerVisualMode.CanvasCard) }
                )
            }
        }
        val headerCenter: @Composable () -> Unit = {
            if (track != null && (track.videoUrl.isNotBlank() || track.counterpartVideoId.isNotBlank())) {
                modeSwitchContent?.invoke()
            } else {
                PlayerSourceEyebrow(
                    label = strings.playingFrom,
                    source = track?.source ?: "LEVYRA",
                    surfaces = surfaces
                )
            }
        }
        val headerTrailing: @Composable () -> Unit = {
            if (!state.isVideoMode && !liveRadio) {
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
                            tint = if (state.selectedVideoSubtitleId != null) surfaces.activeContent else surfaces.contentMuted,
                            fill = headerButtonFill,
                            borderTop = headerButtonBorder,
                            borderBottom = headerButtonBorder,
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
                if (state.videoQuality.available) {
                    var qualityMenuExpanded by remember(track?.id) { mutableStateOf(false) }
                    Box {
                        PlayerGlassIconButton(
                            icon = Icons.Rounded.HighQuality,
                            contentDescription = strings.videoQuality,
                            size = headerButtonSize,
                            iconSize = 19.dp,
                            tint = surfaces.contentMuted,
                            fill = headerButtonFill,
                            borderTop = headerButtonBorder,
                            borderBottom = headerButtonBorder,
                            onClick = { if (!state.videoQuality.switching) qualityMenuExpanded = true }
                        )
                        DropdownMenu(
                            expanded = qualityMenuExpanded,
                            onDismissRequest = { qualityMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.videoQualityAuto) },
                                onClick = {
                                    qualityMenuExpanded = false
                                    viewModel.selectVideoQuality(null)
                                }
                            )
                            state.videoQuality.ladder.forEach { rung ->
                                DropdownMenuItem(
                                    text = { Text(rung.label) },
                                    leadingIcon = {
                                        if (state.videoQuality.activeLabel == rung.label) {
                                            Icon(Icons.Rounded.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = {
                                        qualityMenuExpanded = false
                                        viewModel.selectVideoQuality(rung.label)
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
                    fill = headerButtonFill,
                    borderTop = headerButtonBorder,
                    borderBottom = headerButtonBorder,
                    onClick = { LevyraPipBridge.enter() }
                )
            }
            if (track != null) {
                PlayerGlassIconButton(
                    icon = Icons.Rounded.MoreHoriz,
                    contentDescription = strings.more,
                    size = headerButtonSize,
                    iconSize = 22.dp,
                    fill = headerButtonFill,
                    borderTop = headerButtonBorder,
                    borderBottom = headerButtonBorder,
                    onClick = { showActions = true }
                )
            }
        }

        val headerBlock: @Composable () -> Unit = {
            if (headerCentered) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LevyraPlayerDesign.MinimumTouchTarget)
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXs)
                    ) { headerLeading() }
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = HeaderSideReserve),
                        contentAlignment = Alignment.Center
                    ) { headerCenter() }
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXs)
                    ) { headerTrailing() }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LevyraPlayerDesign.MinimumTouchTarget),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXs)
                ) {
                    headerLeading()
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) { headerCenter() }
                    headerTrailing()
                }
            }
        }

        val mediaHeroBlock: @Composable (Track, Dp, Dp?) -> Unit = { activeTrack, heroSize, cornerOverride ->
            val artworkCorner = cornerOverride ?: LevyraPlayerShapes.artworkCorner(heroSize)
            Box(
                modifier = if (state.isVideoMode) {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                } else {
                    Modifier.size(heroSize)
                },
                contentAlignment = Alignment.Center
            ) {
                if (state.isVideoMode && activeTrack.videoUrl.isNotBlank() && videoSurfaceContent != null) {
                    val zoom = videoTransform.value
                    if (!videoFullscreen) {
                        val videoShape = RoundedCornerShape(LevyraPlayerDesign.CornerMd)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = artScale
                                    scaleY = artScale
                                    translationY = artOffset.toPx()
                                    shadowElevation = 24.dp.toPx()
                                    shape = videoShape
                                    clip = true
                                }
                        ) {
                            videoSurfaceContent(
                                activeTrack,
                                Modifier
                                    .matchParentSize()
                                    .graphicsLayer {
                                        scaleX = zoom.scale
                                        scaleY = zoom.scale
                                        translationX = zoom.offsetX
                                        translationY = zoom.offsetY
                                    }
                            )
                        }
                    }
                } else {
                    PlayerArtworkHero(
                        track = activeTrack,
                        artworkUrl = artworkUrl,
                        visualMode = deckMode,
                        motionArtwork = state.motionArtwork,
                        livingArtwork = livingArtwork,
                        animationsEnabled = animated && !state.isVideoMode,
                        motionEnabled = motionEnabled && !(lyricsFlip.lyricsSettled && !deckMode.showsCinematicStage()),
                        isPlaying = state.isPlaying,
                        cornerRadius = artworkCorner,
                        canvasQuality = state.interfaceSettings.canvasQuality,
                        morphAnchors = morphAnchors,
                        morphActive = morphActive,
                        swipeOffset = swipeOffset,
                        artScale = artScale,
                        artOffset = artOffset,
                        glowColor = if (deckLayout == PlayerDeckLayout.Editorial) Color.Transparent else primary,
                        modifier = Modifier
                            .fillMaxSize()
                            .playerLyricsFlipFace(lyricsFlip, back = false, depth = lyricsFlipDepth, rightToLeft = rightToLeft)
                    )
                    if (lyricsFlipAvailable && lyricsFlip.lyricsComposed) {
                        PlayerLyricsCard(
                            trackId = activeTrack.id,
                            lines = state.lyrics,
                            synced = state.lyricsSynced,
                            loading = state.lyricsLoading,
                            positionMs = state.positionMs,
                            isPlaying = state.isPlaying,
                            playbackSpeed = state.playbackSpeed,
                            latencyProfiles = state.lyricsLatencyProfiles,
                            interactive = lyricsFlip.lyricsSettled,
                            animated = animated,
                            cornerRadius = artworkCorner,
                            surfaces = surfaces,
                            accent = primaryTarget,
                            onSeekToMs = { positionMs ->
                                if (state.durationMs > 0L) {
                                    viewModel.seekTo((positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f))
                                }
                            },
                            onShowArtwork = { lyricsFlip.show(PlayerLyricsFace.Player, lyricsFlipDepth) },
                            onOpenFullLyrics = viewModel::openLyrics,
                            modifier = Modifier
                                .matchParentSize()
                                .zIndex(19f)
                                .then(lyricsFlipSwipeModifier)
                                .playerLyricsFlipFace(
                                    state = lyricsFlip,
                                    back = true,
                                    depth = lyricsFlipDepth,
                                    rightToLeft = rightToLeft,
                                    frontVisible = !deckMode.showsCinematicStage()
                                )
                        )
                    }
                }

                val videoGesturesEnabled = state.isVideoMode && activeTrack.videoUrl.isNotBlank()
                val gesturesAllowed = !liveRadio &&
                    (state.interfaceSettings.playerGesturesEnabled || videoGesturesEnabled) &&
                    !videoFullscreen &&
                    lyricsFlip.playerSettled
                if (gesturesAllowed && gestureLayerContent != null) {
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
                            togglePlay = viewModel::togglePlay,
                            next = viewModel::next,
                            previous = viewModel::previous,
                            swipeOffset = { offset -> swipe.follow(activeTrack.id, offset) },
                            swipeSettled = { committed -> swipe.release(committed, carry = false) },
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
                            toggleFavorite = { viewModel.toggleFavorite(activeTrack) },
                            openQueue = viewModel::openQueue,
                            openLyrics = viewModel::openLyrics,
                            collapse = collapseActions,
                            artworkPreview = if (artworkPreviewAvailable) {
                                { showArtworkPreview = true }
                            } else {
                                null
                            }
                        ),
                        videoTransform.takeIf { videoGesturesEnabled },
                        Modifier
                            .matchParentSize()
                            .zIndex(if (videoGesturesEnabled) 21f else 20f)
                    )
                }

                if (videoGesturesEnabled && !videoFullscreen) {
                    PlayerGlassIconButton(
                        icon = Icons.Rounded.Fullscreen,
                        contentDescription = strings.enterImmersive,
                        size = LevyraPlayerDesign.HeaderButton,
                        iconSize = 21.dp,
                        fill = Color.Black.copy(alpha = 0.42f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = LevyraPlayerDesign.SpaceSm, bottom = LevyraPlayerDesign.SpaceSm)
                            .zIndex(23f),
                        onClick = { videoFullscreen = true }
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
                        border = BorderStroke(LevyraPlayerDesign.Hairline, Color.White.copy(alpha = 0.16f)),
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

        val stageBlock: @Composable (Track, Modifier, Dp?) -> Unit = { activeTrack, stageModifier, stageCorner ->
            BoxWithConstraints(
                modifier = stageModifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        val root = rootCoordinates ?: return@onGloballyPositioned
                        if (!root.isAttached || !coordinates.isAttached) return@onGloballyPositioned
                        val top = root.localPositionOf(coordinates, Offset.Zero).y.roundToInt()
                        val bottom = top + coordinates.size.height
                        if (top != stageTopPx) stageTopPx = top
                        if (bottom != stageBottomPx) stageBottomPx = bottom
                    },
                contentAlignment = Alignment.Center
            ) {
                val heroSize = minOf(maxWidth, maxHeight, artworkCap)
                if (heroSize > 0.dp) {
                    mediaHeroBlock(activeTrack, heroSize, stageCorner)
                }
            }
        }

        val metadataBlock: @Composable (Track) -> Unit = { activeTrack ->
            if (activeTrack.isLiveRadio()) {
                PlayerLiveRadioMetadata(
                    track = activeTrack,
                    nowPlaying = state.liveRadioNowPlaying,
                    liveLabel = liveRadioStrings.live,
                    details = state.liveRadioStation?.let { station ->
                        listOf(station.country, station.language, station.qualityLabel)
                            .filter(String::isNotBlank)
                            .joinToString(" · ")
                    }.orEmpty(),
                    surfaces = surfaces,
                    compact = compactPlayer
                )
            } else {
                PlayerTrackMetadata(
                    track = activeTrack,
                    isFavorite = activeTrack.id in state.favoriteIds,
                    surfaces = surfaces,
                    animationsEnabled = animated,
                    stepDirection = stepDirection,
                    compact = compactPlayer,
                    openArtistLabel = strings.openArtist,
                    favoritesLabel = strings.favoritesPlain,
                    onArtistClick = { viewModel.openArtist(activeTrack) },
                    onToggleFavorite = { viewModel.toggleFavorite(activeTrack) },
                    modifier = lyricsFlipSwipeModifier
                )
            }
        }

        val progressBlock: @Composable () -> Unit = {
            if (liveRadio) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = LevyraPlayerDesign.SpaceMd)
                        .height(LevyraPlayerDesign.TrackHeight)
                        .background(
                            heroTone.copy(alpha = if (state.isResolving) 0.36f else 0.9f),
                            CircleShape
                        )
                )
            } else {
                PlayerProgress(
                    positionMs = state.positionMs,
                    bufferedPositionMs = state.bufferedPositionMs,
                    durationMs = state.durationMs,
                    activeColor = heroTone,
                    secondaryColor = secondary,
                    surfaces = surfaces,
                    isPlaying = state.isPlaying,
                    playbackSpeed = state.playbackSpeed.coerceIn(0.5f, 2f),
                    animationsEnabled = animated,
                    motionActive = !motionSuspended,
                    compact = compactPlayer,
                    onSeek = viewModel::seekTo
                )
            }
        }

        val transportBlock: @Composable () -> Unit = {
            if (liveRadio) {
                PlayerLiveRadioTransport(
                    isPlaying = state.isPlaying,
                    isResolving = state.isResolving,
                    surfaces = surfaces,
                    compact = compactPlayer,
                    animated = animated,
                    strings = strings,
                    onStop = viewModel::closePlayer,
                    onToggle = viewModel::togglePlay
                )
            } else {
                PlayerTransportBar(
                    isPlaying = state.isPlaying,
                    isResolving = state.isResolving,
                    shuffleOn = state.shuffleEnabled,
                    repeatMode = state.repeatMode,
                    surfaces = surfaces,
                    compact = compactPlayer,
                    animated = animated,
                    labels = playerControlLabels,
                    onShuffle = viewModel::toggleShuffle,
                    onPrevious = viewModel::previous,
                    onTogglePlay = viewModel::togglePlay,
                    onNext = viewModel::next,
                    onRepeat = viewModel::toggleRepeat
                )
            }
        }

        val dockBlock: @Composable (Track) -> Unit = { activeTrack ->
            val isDownloaded = activeTrack.id in state.downloadedTrackIds
            val actions = remember(
                activeTrack.id,
                state.showLyrics,
                state.isVideoMode,
                state.isOfflineExporting,
                isDownloaded,
                visualMode,
                strings,
                viewModel,
                hapticFeedback
            ) {
                playerDockActions(
                    strings = strings,
                    visualMode = visualMode,
                    showLyrics = state.showLyrics,
                    videoMode = state.isVideoMode,
                    isDownloaded = isDownloaded,
                    isExporting = state.isOfflineExporting,
                    onLyrics = viewModel::openLyrics,
                    onQueue = viewModel::openQueue,
                    onOpenDeck = { showDeck = true },
                    onDownload = viewModel::exportCurrentTrack
                )
            }
            PlayerActionDock(
                actions = actions,
                surfaces = surfaces,
                compact = compactPlayer,
                animated = animated
            )
        }

        val controlsTailBlock: @Composable ColumnScope.(Track) -> Unit = { activeTrack ->
            if (!activeTrack.isLiveRadio()) {
                engagementContent?.invoke(activeTrack)
            }
            Spacer(modifier = Modifier.height(if (compactPlayer) LevyraPlayerDesign.SpaceXs else LevyraPlayerDesign.SpaceSm))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                progressBlock()
            }
            errorContent?.invoke()
            Spacer(modifier = Modifier.height(if (compactPlayer) LevyraPlayerDesign.SpaceXs else LevyraPlayerDesign.SpaceMd))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                transportBlock()
            }
            if (!activeTrack.isLiveRadio()) {
                Spacer(modifier = Modifier.height(if (compactPlayer) LevyraPlayerDesign.SpaceMd else LevyraPlayerDesign.SpaceLg + LevyraPlayerDesign.SpaceXs))
                dockBlock(activeTrack)
            }
        }

        val controlsBlock: @Composable ColumnScope.(Track) -> Unit = { activeTrack ->
            metadataBlock(activeTrack)
            controlsTailBlock(activeTrack)
        }

        val rootModifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootCoordinates = it }

        val deckSlots = PlayerDeckSlots(
            header = headerBlock,
            stage = stageBlock,
            controls = controlsBlock,
            controlsWithoutMetadata = controlsTailBlock
        )
        val deckModifier = rootModifier
            .widthIn(max = detailMaxWidth)
            .align(Alignment.TopCenter)

        if (deckLayout == PlayerDeckLayout.Editorial && track != null) {
            PlayerEditorialDeck(
                track = track,
                slots = deckSlots,
                surfaces = surfaces,
                accent = heroTone,
                isFavorite = track.id in state.favoriteIds,
                queuePosition = playerDeckQueuePosition(state.queue, track.id),
                animated = animated,
                compact = compactPlayer,
                scrollable = !fitsViewport,
                gutter = gutter,
                onArtistClick = { viewModel.openArtist(track) },
                onToggleFavorite = { viewModel.toggleFavorite(track) },
                headlineModifier = lyricsFlipSwipeModifier,
                modifier = deckModifier
            )
        } else if (deckLayout == PlayerDeckLayout.Pulse && track != null) {
            PlayerPulseDeck(
                track = track,
                slots = deckSlots,
                surfaces = surfaces,
                accent = heroTone,
                isPlaying = state.isPlaying,
                animated = animated,
                compact = compactPlayer,
                scrollable = !fitsViewport,
                scrollingHeroHeight = scrollingArtworkHeight,
                gutter = gutter,
                modifier = deckModifier
            )
        } else if (playerPane == LevyraPlayerPane.SideBySide && track != null) {
            Column(
                modifier = rootModifier
                    .widthIn(max = detailMaxWidth)
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = gutter, vertical = LevyraPlayerDesign.SpaceMd)
            ) {
                headerBlock()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXl),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    stageBlock(
                        track,
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(vertical = LevyraPlayerDesign.SpaceMd),
                        null
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        controlsBlock(track)
                    }
                }
            }
        } else {
            Column(
                modifier = rootModifier
                    .widthIn(max = detailMaxWidth)
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .then(if (fitsViewport) Modifier else Modifier.verticalScroll(rememberScrollState()))
                    .padding(
                        start = gutter,
                        end = gutter,
                        top = LevyraPlayerDesign.SpaceXs,
                        bottom = if (compactPlayer) LevyraPlayerDesign.SpaceMd else LevyraPlayerDesign.SpaceXl
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
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
                            color = surfaces.contentMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    val stageSpacing = if (compactPlayer) LevyraPlayerDesign.SpaceMd else LevyraPlayerDesign.SpaceXl
                    stageBlock(
                        track,
                        if (fitsViewport) {
                            Modifier
                                .weight(1f)
                                .padding(top = stageSpacing - LevyraPlayerDesign.SpaceXs, bottom = stageSpacing)
                        } else {
                            Modifier
                                .height(scrollingArtworkHeight)
                                .padding(vertical = LevyraPlayerDesign.SpaceMd)
                        },
                        null
                    )
                    controlsBlock(track)
                }
            }
        }

        if (videoFullscreen &&
            state.isVideoMode &&
            track != null &&
            track.videoUrl.isNotBlank() &&
            videoSurfaceContent != null
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .zIndex(100f)
            ) {
                val zoom = videoTransform.value
                videoSurfaceContent(
                    track,
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoom.scale
                            scaleY = zoom.scale
                            translationX = zoom.offsetX
                            translationY = zoom.offsetY
                        }
                )
                if (gestureLayerContent != null) {
                    gestureLayerContent(
                        track,
                        PlayerGestureConfig(
                            trackId = track.id,
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
                            togglePlay = viewModel::togglePlay,
                            next = viewModel::next,
                            previous = viewModel::previous,
                            swipeOffset = { offset -> swipe.follow(track.id, offset) },
                            swipeSettled = { committed -> swipe.release(committed, carry = false) },
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
                            toggleFavorite = { viewModel.toggleFavorite(track) },
                            openQueue = viewModel::openQueue,
                            openLyrics = viewModel::openLyrics,
                            collapse = collapseActions
                        ),
                        videoTransform,
                        Modifier
                            .matchParentSize()
                            .zIndex(101f)
                    )
                }
                PlayerGlassIconButton(
                    icon = Icons.Rounded.CloseFullscreen,
                    contentDescription = strings.exitImmersive,
                    size = LevyraPlayerDesign.MinimumTouchTarget,
                    iconSize = 22.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = LevyraPlayerDesign.SpaceSm, end = LevyraPlayerDesign.SpaceSm)
                        .zIndex(102f),
                    onClick = { videoFullscreen = false }
                )
            }
        }

        if (showActions && track != null) {
            val sheetActions = playerSheetActions(
                track = track,
                state = state,
                strings = strings,
                onAddToPlaylist = { onOpenPlaylistDialog?.invoke(track) },
                onStartRadio = {
                    viewModel.startSongRadio()
                    viewModel.openQueue()
                    hapticFeedback.perform(LevyraHapticAction.Confirm)
                },
                onShare = { shareTrack(playerContext, track, strings) },
                onSleepTimer = viewModel::openSleepTimer,
                onSpeed = viewModel::cycleSpeed,
                onNormalization = viewModel::toggleAudioNormalization,
                onAudioSettings = viewModel::openAudioQualityPanel,
                onTechnicalAudioInfo = {
                    showActions = false
                    showTechnicalAudioInfo = true
                },
                onAmbient = viewModel::openAmbient,
                onOpenArtist = { viewModel.openArtist(track) }
            )
            PlayerActionsSheet(
                track = track,
                artworkUrl = artworkUrl,
                surfaces = surfaces,
                animated = animated,
                actions = sheetActions,
                onDismiss = { showActions = false },
                engagementContent = if (engagementContent != null && !liveRadio) {
                    { engagementContent(track) }
                } else {
                    null
                },
                discoverContent = if (similarSongsContent != null && !liveRadio) {
                    { similarSongsContent(track) }
                } else {
                    null
                }
            )
        }

        if (showTechnicalAudioInfo && track != null) {
            TechnicalAudioInfoSheet(
                track = track,
                audioSettings = state.audioSettings,
                audioNormalization = state.audioNormalization,
                onDismiss = { showTechnicalAudioInfo = false }
            )
        }

        if (showDeck && track != null && !state.isVideoMode) {
            PlayerDeckSheet(
                track = track,
                artworkUrl = artworkUrl,
                selected = visualMode,
                surfaces = surfaces,
                accent = heroTone,
                animated = animated,
                onSelect = { mode ->
                    if (mode != visualMode) {
                        viewModel.setPlayerVisualMode(mode)
                        hapticFeedback.perform(LevyraHapticAction.Confirm)
                    }
                },
                onDismiss = { showDeck = false }
            )
        }

        playlistDialogContent?.invoke()
    }
}

@Composable
private fun PlayerSourceEyebrow(
    label: String,
    source: String,
    surfaces: PlayerSurfaceTokens
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = surfaces.contentFaint,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = source,
            color = surfaces.contentMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlayerLiveRadioMetadata(
    track: Track,
    nowPlaying: String,
    liveLabel: String,
    details: String,
    surfaces: PlayerSurfaceTokens,
    compact: Boolean
) {
    val titleSize = if (compact) 22.sp else 26.sp
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXs)
    ) {
        Text(
            text = track.title,
            color = surfaces.content,
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = nowPlaying.ifBlank { liveLabel },
            color = if (nowPlaying.isBlank()) surfaces.activeContent else surfaces.contentMuted,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (details.isNotBlank()) {
            Text(
                text = details,
                color = surfaces.contentFaint,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlayerLiveRadioTransport(
    isPlaying: Boolean,
    isResolving: Boolean,
    surfaces: PlayerSurfaceTokens,
    compact: Boolean,
    animated: Boolean,
    strings: LevyraStrings,
    onStop: () -> Unit,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) LevyraPlayerDesign.TransportHeightCompact else LevyraPlayerDesign.TransportHeight),
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.TransportGap)
    ) {
        PlayerSegmentButton(
            position = LevyraSegment.Leading,
            weight = 1f,
            container = surfaces.control,
            innerCorner = LevyraPlayerDesign.TransportInnerCorner,
            contentDescription = strings.close,
            animated = animated,
            outline = if (surfaces.amoled) surfaces.outline else Color.Transparent,
            onClick = onStop
        ) {
            PlayerIcon(Icons.Rounded.Stop, surfaces.content, Modifier.size(LevyraPlayerDesign.TransportGlyph))
        }
        PlayerSegmentButton(
            position = LevyraSegment.Trailing,
            weight = 2f,
            container = surfaces.hero,
            innerCorner = LevyraPlayerDesign.TransportInnerCorner,
            contentDescription = if (isPlaying) strings.pause else strings.play,
            animated = animated,
            enabled = !isResolving,
            haptic = LevyraHapticAction.Transport,
            onClick = onToggle
        ) {
            PlayerIcon(
                icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                tint = surfaces.heroContent,
                modifier = Modifier.size(LevyraPlayerDesign.TransportPlayGlyph)
            )
        }
    }
}

private fun playerDockActions(
    strings: LevyraStrings,
    visualMode: PlayerVisualMode,
    showLyrics: Boolean,
    videoMode: Boolean,
    isDownloaded: Boolean,
    isExporting: Boolean,
    onLyrics: () -> Unit,
    onQueue: () -> Unit,
    onOpenDeck: () -> Unit,
    onDownload: () -> Unit
): List<PlayerDockAction> {
    return buildList {
        add(
            PlayerDockAction(
                key = "lyrics",
                icon = Icons.AutoMirrored.Rounded.Subject,
                label = strings.lyrics,
                active = showLyrics,
                onClick = onLyrics
            )
        )
        add(
            PlayerDockAction(
                key = "queue",
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                label = strings.queue,
                onClick = onQueue
            )
        )
        if (!videoMode) {
            add(
                PlayerDockAction(
                    key = "visual-mode",
                    icon = visualModeIcon(visualMode),
                    label = visualModeLabel(strings),
                    stateDescription = visualModeStateDescription(visualMode, strings),
                    active = visualMode != PlayerVisualMode.Artwork,
                    toggle = false,
                    onClick = onOpenDeck
                )
            )
        }
        add(
            PlayerDockAction(
                key = "download",
                icon = if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                label = when {
                    isExporting -> strings.downloadInProgress
                    isDownloaded -> strings.downloaded
                    else -> strings.download
                },
                active = isDownloaded || isExporting,
                busy = isExporting,
                onClick = onDownload
            )
        )
    }
}

private fun playerSheetActions(
    track: Track,
    state: LevyraUiState,
    strings: LevyraStrings,
    onAddToPlaylist: () -> Unit,
    onStartRadio: () -> Unit,
    onShare: () -> Unit,
    onSleepTimer: () -> Unit,
    onSpeed: () -> Unit,
    onNormalization: () -> Unit,
    onAudioSettings: () -> Unit,
    onTechnicalAudioInfo: () -> Unit,
    onAmbient: () -> Unit,
    onOpenArtist: () -> Unit
): List<PlayerSheetAction> {
    val sleepActive = state.sleepTimerMinutes > 0 || state.sleepTimerEndOfTrack
    val sleepAction = PlayerSheetAction(
        key = "sleep",
        icon = Icons.Rounded.Bedtime,
        label = strings.sleepTimer,
        value = when {
            state.sleepTimerEndOfTrack -> strings.sleepTimerEndOfTrack
            state.sleepTimerMinutes > 0 -> strings.formatSleepTimerMinutes(state.sleepTimerMinutes)
            else -> null
        },
        active = sleepActive,
        onClick = onSleepTimer
    )
    val audioAction = PlayerSheetAction(
        key = "audio",
        icon = Icons.Rounded.Tune,
        label = strings.audioQuality,
        onClick = onAudioSettings
    )
    val technicalAudioAction = PlayerSheetAction(
        key = "technical-audio-info",
        icon = Icons.Rounded.Info,
        label = strings.technicalAudioInfoCopy().title,
        onClick = onTechnicalAudioInfo
    )
    val ambientAction = PlayerSheetAction(
        key = "ambient",
        icon = Icons.Rounded.Nightlight,
        label = strings.ambientMode,
        onClick = onAmbient
    )
    if (track.isLiveRadio()) return listOf(sleepAction, audioAction, technicalAudioAction, ambientAction)

    val canStartRadio = !state.jam.isActive || state.jam.isHost
    return listOf(
        PlayerSheetAction(
            key = "playlist",
            icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
            label = strings.addToPlaylist,
            onClick = onAddToPlaylist
        ),
        PlayerSheetAction(
            key = "radio",
            icon = Icons.Rounded.Radio,
            label = strings.startRadio,
            enabled = canStartRadio,
            onClick = onStartRadio
        ),
        PlayerSheetAction(
            key = "share",
            icon = Icons.Rounded.Share,
            label = strings.share,
            onClick = onShare
        ),
        sleepAction,
        PlayerSheetAction(
            key = "speed",
            icon = Icons.Rounded.Speed,
            label = strings.tempo,
            value = "${formatPlaybackSpeed(state.playbackSpeed)}x",
            active = state.playbackSpeed != 1f,
            keepsSheetOpen = true,
            onClick = onSpeed
        ),
        PlayerSheetAction(
            key = "normalization",
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            label = strings.normalizationShort,
            active = state.audioNormalization,
            toggle = true,
            keepsSheetOpen = true,
            onClick = onNormalization
        ),
        audioAction,
        technicalAudioAction,
        ambientAction,
        PlayerSheetAction(
            key = "artist",
            icon = Icons.Rounded.Person,
            label = strings.openArtist,
            onClick = onOpenArtist
        )
    )
}

internal fun formatPlaybackSpeed(speed: Float): String =
    if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0').trimEnd('.')

private fun shareTrack(context: Context, track: Track, strings: LevyraStrings) {
    val videoId = PlaybackSourceIdentity.sourceVideoId(track)
    val text = buildString {
        append(track.title)
        if (track.artist.isNotBlank()) append(" — ").append(track.artist)
        if (videoId.isNotBlank()) append("\n").append(youtubeWatchUrl(videoId))
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "${track.title} — ${track.artist}")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, strings.shareSong))
}

internal fun visualModeLabel(strings: LevyraStrings): String =
    strings.playerVisualMode

internal fun visualModeStateDescription(
    mode: PlayerVisualMode,
    strings: LevyraStrings
): String = when (mode) {
    PlayerVisualMode.Artwork -> strings.playerVisualModeArtwork
    PlayerVisualMode.CanvasCard -> strings.playerVisualModeCanvasCard
    PlayerVisualMode.CanvasImmersive -> strings.playerVisualModeCanvasImmersive
    PlayerVisualMode.Editorial -> strings.playerDeckEditorial
    PlayerVisualMode.Pulse -> strings.playerDeckPulse
}

internal fun visualModeIcon(mode: PlayerVisualMode): ImageVector = when (mode) {
    PlayerVisualMode.Artwork -> Icons.Rounded.Image
    PlayerVisualMode.CanvasCard -> Icons.Rounded.AutoAwesome
    PlayerVisualMode.CanvasImmersive -> Icons.Rounded.Fullscreen
    PlayerVisualMode.Editorial -> Icons.Rounded.AutoStories
    PlayerVisualMode.Pulse -> Icons.Rounded.GraphicEq
}

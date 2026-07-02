@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.R
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.luc4n3x.levyra.domain.AppUpdateInfo
import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.ArtistRelease
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.Mood
import com.luc4n3x.levyra.domain.Taste
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOrange
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel

private val LocalAnimationsEnabled = compositionLocalOf { true }

@Composable
fun LevyraApp(viewModel: LevyraViewModel) {
    val state by viewModel.state.collectAsState()
    val toastContext = LocalContext.current
    val activity = toastContext as? Activity
    val accent = if (state.dynamicColor) state.currentTrack ?: state.tracks.firstOrNull() else null
    val overlayEnter = if (state.animationsEnabled) fadeIn(animationSpec = tween(180, easing = LinearOutSlowInEasing)) else EnterTransition.None
    val overlayExit = if (state.animationsEnabled) fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing)) else ExitTransition.None
    val miniEnter = if (state.animationsEnabled) {
        slideInVertically(animationSpec = tween(260, easing = FastOutSlowInEasing), initialOffsetY = { it / 2 }) + fadeIn(animationSpec = tween(180, easing = LinearOutSlowInEasing))
    } else {
        EnterTransition.None
    }
    val miniExit = if (state.animationsEnabled) {
        slideOutVertically(animationSpec = tween(180, easing = FastOutSlowInEasing), targetOffsetY = { it / 3 }) + fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing))
    } else {
        ExitTransition.None
    }
    LaunchedEffect(state.offlineExportMessage) {
        state.offlineExportMessage?.let { message ->
            Toast.makeText(toastContext, message, Toast.LENGTH_LONG).show()
            viewModel.clearOfflineExportMessage()
        }
    }
    LaunchedEffect(state.updateMessage) {
        state.updateMessage?.let { message ->
            Toast.makeText(toastContext, message, Toast.LENGTH_LONG).show()
            viewModel.clearUpdateMessage()
        }
    }
    BackHandler(enabled = state.openPlaylist != null || state.showUpdatePrompt || state.showArtist || state.showQueue || state.showLyrics || state.showSettings || state.showAudioQualityPanel || state.selectedTab != LevyraTab.Home) {
        if (state.showAudioQualityPanel) {
            viewModel.closeAudioQualityPanel()
        } else if (state.openPlaylist != null) {
            viewModel.closePlaylist()
        } else if (!viewModel.navigateBack()) {
            activity?.finish()
        }
    }
    CompositionLocalProvider(LocalAnimationsEnabled provides state.animationsEnabled) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LevyraBlack)
        ) {
            LevyraBackground(accent?.accentStart, accent?.accentEnd)

            AnimatedContent(
                targetState = state.selectedTab,
                transitionSpec = {
                    if (!state.animationsEnabled) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                        val enter = slideInHorizontally(
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                            initialOffsetX = { it * direction }
                        ) + fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing))
                        val exit = slideOutHorizontally(
                            animationSpec = tween(240, easing = FastOutSlowInEasing),
                            targetOffsetX = { -it * direction / 3 }
                        ) + fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing))
                        enter togetherWith exit
                    }
                },
                label = "levyra-page-transition"
            ) { tab ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (tab) {
                        LevyraTab.Home -> HomeScreen(viewModel, state)
                        LevyraTab.Search -> SearchScreen(viewModel, state)
                        LevyraTab.Library -> LibraryScreen(viewModel, state)
                        LevyraTab.Player -> PlayerScreen(viewModel, state)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedVisibility(
                    visible = state.selectedTab != LevyraTab.Player && state.currentTrack != null,
                    enter = miniEnter,
                    exit = miniExit
                ) {
                    state.currentTrack?.let { track ->
                        MiniPlayer(
                            track = track,
                            isPlaying = state.isPlaying,
                            isResolving = state.isResolving,
                            progress = progressOf(state.positionMs, state.durationMs),
                            onOpen = { viewModel.selectTab(LevyraTab.Player) },
                            onToggle = viewModel::togglePlay,
                            onNext = viewModel::next,
                            onClose = viewModel::closePlayer
                        )
                    }
                }
                BottomTabs(selected = state.selectedTab, onSelect = viewModel::selectTab)
            }

            AnimatedVisibility(visible = state.showOnboarding, enter = overlayEnter, exit = overlayExit) {
                if (state.showOnboarding) {
                    OnboardingOverlay(tastes = state.tastes, onDone = viewModel::completeOnboarding)
                }
            }

            AnimatedVisibility(visible = state.showSettings, enter = overlayEnter, exit = overlayExit) {
                SettingsOverlay(
                    animationsEnabled = state.animationsEnabled,
                    dynamicColor = state.dynamicColor,
                    sponsorBlock = state.sponsorBlockEnabled,
                    skipSilence = state.skipSilence,
                    updateInfo = state.updateInfo,
                    isCheckingUpdates = state.isCheckingUpdates,
                    onAnimations = viewModel::setAnimationsEnabled,
                    onDynamicColor = viewModel::setDynamicColor,
                    onSponsorBlock = viewModel::setSponsorBlock,
                    onSkipSilence = viewModel::setSkipSilence,
                    onCheckUpdates = { viewModel.checkForUpdates(silent = false) },
                    onDownloadUpdate = { state.updateInfo?.let { openExternalUrl(toastContext, it.downloadUrl) } },
                    onRedoQuestionnaire = viewModel::restartOnboarding,
                    onClose = viewModel::closeSettings
                )
            }

            AnimatedVisibility(visible = state.showLyrics, enter = overlayEnter, exit = overlayExit) {
                LyricsOverlay(state = state, onClose = viewModel::closeLyrics)
            }

            AnimatedVisibility(visible = state.showAudioQualityPanel, enter = miniEnter, exit = miniExit) {
                AudioQualityPanel(
                    selected = state.audioQuality,
                    volumePercent = 33,
                    onSelect = viewModel::setAudioQuality,
                    onClose = viewModel::closeAudioQualityPanel
                )
            }

            AnimatedVisibility(visible = state.showUpdatePrompt && state.updateInfo?.isNewer == true, enter = overlayEnter, exit = overlayExit) {
                state.updateInfo?.let { update ->
                    UpdateAvailableOverlay(
                        update = update,
                        onDownload = {
                            openExternalUrl(toastContext, update.downloadUrl)
                            viewModel.dismissUpdatePrompt()
                        },
                        onLater = viewModel::dismissUpdatePrompt
                    )
                }
            }

            AnimatedVisibility(visible = state.showQueue, enter = overlayEnter, exit = overlayExit) {
                QueueOverlay(
                    state = state,
                    onPlay = { viewModel.playFrom(state.queue, it) },
                    onClose = viewModel::closeQueue
                )
            }

            AnimatedVisibility(visible = state.showArtist, enter = overlayEnter, exit = overlayExit) {
                ArtistOverlay(
                    state = state,
                    onPlay = viewModel::playArtistSong,
                    onFavorite = viewModel::toggleFavorite,
                    onDownload = viewModel::exportTrack,
                    onClose = viewModel::closeArtist
                )
            }

            AnimatedVisibility(visible = state.openPlaylist != null, enter = overlayEnter, exit = overlayExit) {
                PlaylistDetailOverlay(viewModel = viewModel, state = state)
            }
        }
    }
}

@Composable
private fun ArtistOverlay(
    state: LevyraUiState,
    onPlay: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onDownload: (Track) -> Unit,
    onClose: () -> Unit
) {
    val blocker = remember { MutableInteractionSource() }
    val profile = state.artistProfile
    val accentStart = profile?.let { Color(it.accentStart) } ?: LevyraCyan
    val accentEnd = profile?.let { Color(it.accentEnd) } ?: LevyraViolet
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevyraBlack)
            .clickable(interactionSource = blocker, indication = null) {}
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
                .background(Brush.verticalGradient(listOf(accentStart.copy(alpha = 0.75f), accentEnd.copy(alpha = 0.35f), LevyraBlack, LevyraBlack)))
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = if (state.currentTrack != null) 200.dp else 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Indietro", tint = LevyraText)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = LevyraText.copy(alpha = 0.7f))
                }
            }
            when {
                state.artistLoading && profile == null -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = LevyraCyan)
                        }
                    }
                }
                profile == null -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                            Text(state.artistError ?: "Profilo artista non disponibile", color = LevyraMuted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                else -> {
                    item { ArtistHeader(profile, accentStart, accentEnd) }
                    if (profile.hasBio) {
                        item { ArtistBio(profile.bio) }
                    }
                    if (profile.topSongs.isNotEmpty()) {
                        item { SectionTitle("🔥 Brani popolari") }
                        items(profile.topSongs, key = { "artist-song-${it.id}" }) { track ->
                            TrackRow(
                                track = track,
                                isCurrent = track.id == state.currentTrack?.id,
                                isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                                isResolving = state.isResolving && track.id == state.currentTrack?.id,
                                isFavorite = track.id in state.favoriteIds,
                                onClick = { onPlay(track) },
                                onFavorite = { onFavorite(track) },
                                isDownloading = track.id in state.downloadingTrackIds,
                                isDownloaded = track.id in state.downloadedTrackIds,
                                onDownload = { onDownload(track) }
                            )
                        }
                    }
                    if (profile.albums.isNotEmpty()) {
                        item { SectionTitle("💿 Album") }
                        item { ArtistReleaseRow(profile.albums) }
                    }
                    if (profile.singles.isNotEmpty()) {
                        item { SectionTitle("🎵 Singoli ed EP") }
                        item { ArtistReleaseRow(profile.singles) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistHeader(profile: ArtistProfile, accentStart: Color, accentEnd: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .size(164.dp)
                .clip(RoundedCornerShape(42.dp))
                .background(Brush.linearGradient(listOf(accentStart, accentEnd))),
            contentAlignment = Alignment.Center
        ) {
            if (profile.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(profile.thumbnailUrl).crossfade(true).build(),
                    contentDescription = profile.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().clip(RoundedCornerShape(42.dp))
                )
            } else {
                Icon(Icons.Rounded.Person, null, tint = LevyraText, modifier = Modifier.size(64.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(profile.name, color = LevyraText, fontSize = 38.sp, lineHeight = 42.sp, letterSpacing = (-1.2).sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Rounded.Verified, null, tint = LevyraCyan, modifier = Modifier.size(26.dp))
        }
        val meta = listOf(profile.subscribers, profile.monthlyListeners).filter { it.isNotBlank() }.joinToString(" · ")
        if (meta.isNotBlank()) {
            Text(meta, color = LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ArtistBio(bio: String) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        color = LevyraPanelSoft.copy(alpha = 0.55f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Biografia", color = LevyraCyan, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text(
                bio,
                color = LevyraText.copy(alpha = 0.86f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 21.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (expanded) "Mostra meno" else "Mostra tutto",
                color = LevyraCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ArtistReleaseRow(releases: List<ArtistRelease>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(releases, key = { "rel-${it.browseId.ifBlank { it.title }}" }) { release ->
            Column(
                modifier = Modifier.width(140.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(LevyraPanelSoft),
                    contentAlignment = Alignment.Center
                ) {
                    if (release.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(release.thumbnailUrl).crossfade(true).build(),
                            contentDescription = release.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Icon(Icons.Rounded.Album, null, tint = LevyraMuted, modifier = Modifier.size(40.dp))
                    }
                }
                Text(release.title, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(release.year.ifBlank { release.subtitle }, color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun UpdateAvailableOverlay(
    update: AppUpdateInfo,
    onDownload: () -> Unit,
    onLater: () -> Unit
) {
    val blocker = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(interactionSource = blocker, indication = null) {},
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.28f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(
                                LevyraCyan.copy(alpha = 0.22f),
                                Color(0xFF0B1020),
                                LevyraViolet.copy(alpha = 0.20f)
                            )
                        )
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = LevyraCyan.copy(alpha = 0.15f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Icon(Icons.Rounded.Bolt, null, tint = LevyraCyan, modifier = Modifier.size(17.dp))
                            Text("AGGIORNAMENTO", color = LevyraText, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                        }
                    }
                    CircleIconButton(
                        icon = Icons.Rounded.Close,
                        tint = LevyraText,
                        background = Color.White.copy(alpha = 0.08f),
                        onClick = onLater
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        text = "LEVYRA ${update.latestVersionName} è pronta",
                        color = LevyraText,
                        fontSize = 27.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Puoi scaricare la nuova versione e installarla quando vuoi.",
                        color = LevyraMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.055f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(update.releaseTitle.ifBlank { "Nuova versione" }, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = compactReleaseNotes(update.releaseNotes),
                            color = LevyraMuted,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .pressable(onClick = onDownload)
                    ) {
                        Box(
                            modifier = Modifier.background(Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet))),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(19.dp))
                                Text("Scarica", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
                        modifier = Modifier
                            .weight(0.72f)
                            .height(48.dp)
                            .pressable(onClick = onLater)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Più tardi", color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Text(
                    text = "Installata: ${update.currentVersionName} · Ultima: ${update.latestVersionName}",
                    color = LevyraMuted.copy(alpha = 0.78f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun compactReleaseNotes(notes: String): String {
    val clean = notes
        .lineSequence()
        .map { it.trim().trimStart('-', '*', '•').trim() }
        .filter { it.isNotBlank() }
        .take(3)
        .joinToString(" · ")
    return clean.ifBlank { "Correzioni, rifiniture e miglioramenti generali." }
}

private fun openExternalUrl(context: android.content.Context, url: String) {
    if (url.isBlank()) {
        Toast.makeText(context, "Link aggiornamento non disponibile", Toast.LENGTH_LONG).show()
        return
    }
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "Impossibile aprire il download", Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun QueueOverlay(state: LevyraUiState, onPlay: (Track) -> Unit, onClose: () -> Unit) {
    val blocker = remember { MutableInteractionSource() }
    val currentId = state.currentTrack?.id
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0F1C), LevyraBlack)))
            .clickable(interactionSource = blocker, indication = null) {}
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("In coda", color = LevyraText, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        Text("${state.queue.size} brani", color = LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    CircleIconButton(
                        icon = Icons.Rounded.Close,
                        tint = LevyraText,
                        background = Color.White.copy(alpha = 0.1f),
                        onClick = onClose
                    )
                }
            }
            if (state.queue.isEmpty()) {
                item { Text("La coda è vuota.", color = LevyraMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            } else {
                itemsIndexed(state.queue, key = { _, t -> "q-${t.id}" }) { index, track ->
                    val isCurrent = track.id == currentId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressable(onClick = { onPlay(track) }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = if (isCurrent) LevyraCyan else LevyraMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.size(22.dp),
                            textAlign = TextAlign.Center
                        )
                        CoverImage(track, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(track.title, color = if (isCurrent) LevyraCyan else LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (isCurrent) Icon(Icons.Rounded.Equalizer, null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsOverlay(state: LevyraUiState, onClose: () -> Unit) {
    val track = state.currentTrack
    val accentStart = if (track != null) Color(track.accentStart) else LevyraCyan
    val accentEnd = if (track != null) Color(track.accentEnd) else LevyraViolet
    val blocker = remember { MutableInteractionSource() }
    val listState = rememberLazyListState()
    val active = state.activeLyric
    val activeIndex = if (state.lyricsSynced && active != null) state.lyrics.indexOf(active) else -1

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            runCatching { listState.animateScrollToItem(activeIndex.coerceAtLeast(0)) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(accentStart.copy(alpha = 0.28f), Color(0xFF070A12), LevyraBlack)
                )
            )
            .clickable(interactionSource = blocker, indication = null) {}
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track?.title ?: "Testo",
                            color = LevyraText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(track?.artist ?: "", color = LevyraMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    CircleIconButton(
                        icon = Icons.Rounded.Close,
                        tint = LevyraText,
                        background = Color.White.copy(alpha = 0.1f),
                        onClick = onClose
                    )
                }
            }
            if (state.lyricsSynced) {
                item {
                    Surface(color = LevyraCyan.copy(alpha = 0.14f), shape = CircleShape) {
                        Text("✨ Sincronizzato", color = LevyraCyan, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }
            }
            if (state.lyrics.isEmpty()) {
                item { Text("Testo non disponibile per questo brano.", color = LevyraMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            } else {
                itemsIndexed(state.lyrics) { index, line ->
                    val isActive = state.lyricsSynced && index == activeIndex
                    Text(
                        text = line.text,
                        color = when {
                            isActive -> LevyraText
                            state.lyricsSynced -> LevyraMuted.copy(alpha = 0.5f)
                            else -> LevyraText.copy(alpha = 0.85f)
                        },
                        fontSize = if (isActive) 22.sp else 18.sp,
                        lineHeight = if (isActive) 26.sp else 23.sp,
                        fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LevyraBackground(accentStart: Int?, accentEnd: Int?) {
    val start = accentStart?.let { Color(it) } ?: LevyraCyan
    val end = accentEnd?.let { Color(it) } ?: LevyraViolet
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to start.copy(alpha = 0.16f),
                    0.35f to LevyraBlack,
                    1f to end.copy(alpha = 0.07f)
                )
            )
    )
}

@Composable
private fun HomeScreen(viewModel: LevyraViewModel, state: LevyraUiState) {
    val heroUpdate = remember(state.currentTrack, state.tracks, state.homeSections, state.charts, state.favorites) {
        pickHeroUpdate(state)
    }
    val heroTrack = heroUpdate?.track
    val quickTracks = remember(state.currentTrack, state.tracks, state.homeSections, state.charts, state.favorites) {
        buildQuickPickTracks(state, heroTrack)
    }
    val personalTracks = remember(state.currentTrack, state.recentSearches, state.favorites, state.tracks, state.homeSections, state.charts) {
        buildPersonalListeningTracks(state)
    }
    val resonanceTracks = remember(state.currentTrack, state.recentSearches, state.favorites, state.tracks, state.homeSections, state.charts) {
        buildResonanceTracks(state)
    }
    val newReleases = remember(state.homeSections) {
        state.homeSections.firstOrNull { isVerifiedReleaseSectionTitle(it.title) }
    }
    val otherSections = remember(state.homeSections) {
        state.homeSections.filter { !isVerifiedReleaseSectionTitle(it.title) && !isQuickPicksSectionTitle(it.title) }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = if (state.currentTrack != null) 188.dp else 100.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                GreetingBar(state.userName, state.isResolving, onSettings = viewModel::openSettings)
                MoodRow(moods = state.moods, selectedId = state.selectedMood?.id, onSelect = viewModel::selectMood)
            }
        }
        if (personalTracks.isNotEmpty()) {
            item {
                PersonalListeningShelf(
                    tracks = personalTracks,
                    currentId = state.currentTrack?.id,
                    isPlaying = state.isPlaying,
                    isResolving = state.isResolving,
                    onPlay = { track -> viewModel.playFrom(personalTracks, track) },
                    onPlayAll = { viewModel.playAll(personalTracks) }
                )
            }
        }
        if (resonanceTracks.isNotEmpty()) {
            item {
                ResonanceShelf(
                    tracks = resonanceTracks,
                    currentId = state.currentTrack?.id,
                    isPlaying = state.isPlaying,
                    isResolving = state.isResolving,
                    onPlay = { track -> viewModel.playFrom(resonanceTracks, track) },
                    onPlayAll = { viewModel.playAll(resonanceTracks) }
                )
            }
        }
        if (state.currentTrack != null) {
            item {
                ContinueListeningCard(
                    track = state.currentTrack,
                    isPlaying = state.isPlaying,
                    isResolving = state.isResolving,
                    progress = progressOf(state.positionMs, state.durationMs),
                    onResume = viewModel::togglePlay
                )
            }
        }
        if (quickTracks.isNotEmpty()) {
            item {
                QuickSectionHeader("Quick Picks", "Play", onAction = { viewModel.playAll(quickTracks) })
            }
            item {
                QuickSongList(
                    tracks = quickTracks,
                    currentId = state.currentTrack?.id,
                    favoriteIds = state.favoriteIds,
                    isPlaying = state.isPlaying,
                    isResolving = state.isResolving,
                    onPlay = { track -> viewModel.playFrom(quickTracks, track) },
                    onFavorite = viewModel::toggleFavorite,
                    onAddToQueue = viewModel::addToQueue,
                    onOpenPlayer = { track ->
                        viewModel.playFrom(quickTracks, track)
                        viewModel.selectTab(LevyraTab.Player)
                    },
                    onOffline = viewModel::exportTrack,
                    onArtist = viewModel::openArtist
                )
            }
        }
        if (newReleases != null && newReleases.tracks.isNotEmpty()) {
            item(key = "sec-new-releases-header") {
                SectionHeaderAction("New Releases", onPlayAll = { viewModel.playAll(newReleases.tracks) })
            }
            item(key = "sec-new-releases-row") {
                AlbumCardRow(
                    tracks = newReleases.tracks,
                    currentId = state.currentTrack?.id,
                    animationsEnabled = state.animationsEnabled,
                    onPlay = { viewModel.playFrom(newReleases.tracks, it) }
                )
            }
        }
        otherSections.forEachIndexed { index, section ->
            if (section.tracks.isNotEmpty()) {
                val title = if (index == 0) "Albums For You" else section.title
                item(key = "sec-other-${index}-header") {
                    SectionHeaderAction(title, onPlayAll = { viewModel.playAll(section.tracks) })
                }
                item(key = "sec-other-${index}-row") {
                    AlbumCardRow(
                        tracks = section.tracks,
                        currentId = state.currentTrack?.id,
                        animationsEnabled = state.animationsEnabled,
                        onPlay = { viewModel.playFrom(section.tracks, it) }
                    )
                }
            }
        }
        item {
            val region = state.chartRegions.firstOrNull { it.id == state.selectedChartId }
            SectionHeaderAction("Top 50 ${region?.label ?: "Global"} ${region?.emoji ?: ""}", onPlayAll = { viewModel.playAll(state.charts) })
        }
        item {
            ChartRegionRow(regions = state.chartRegions, selectedId = state.selectedChartId, loading = state.isLoadingCharts, onSelect = viewModel::selectChart)
        }
        if (state.charts.isEmpty()) {
            item {
                if (state.isLoadingCharts) {
                    GlassMessage("Loading Top 50...", LevyraCyan)
                } else {
                    GlassMessage("Top 50 not available, try again later", LevyraOrange)
                }
            }
        }
        if (state.charts.isNotEmpty()) {
            item {
                LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val chunks = state.charts.chunked(4)
                    itemsIndexed(chunks) { chunkIndex, chunk ->
                        Column(modifier = Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            chunk.forEachIndexed { itemIndex, track ->
                                val rank = chunkIndex * 4 + itemIndex + 1
                                ChartRow(
                                    rank = rank,
                                    track = track,
                                    isCurrent = track.id == state.currentTrack?.id,
                                    isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                                    isResolving = state.isResolving && track.id == state.currentTrack?.id,
                                    isFavorite = track.id in state.favoriteIds,
                                    onClick = { viewModel.playFrom(state.charts, track) },
                                    onFavorite = { viewModel.toggleFavorite(track) }
                                )
                            }
                        }
                    }
                }
            }
        }
        item { StatusBlock(state) }
    }
}

private data class HomeHeroUpdate(
    val track: Track,
    val sourceTitle: String,
    val verifiedRelease: Boolean
)

private fun pickHeroUpdate(state: LevyraUiState): HomeHeroUpdate? {
    val currentId = state.currentTrack?.id
    val verifiedReleases = state.homeSections
        .asSequence()
        .filter { isVerifiedReleaseSectionTitle(it.title) }
        .flatMap { section ->
            section.tracks.asSequence()
                .filter { isReliableMusicUpdateCandidate(it) }
                .map { track -> HomeHeroUpdate(track, section.title, true) }
        }
    val trustedEditorial = state.homeSections
        .asSequence()
        .filterNot { isQuickPicksSectionTitle(it.title) }
        .filterNot { isVerifiedReleaseSectionTitle(it.title) }
        .flatMap { section ->
            section.tracks.asSequence()
                .filter { isReliableMusicUpdateCandidate(it) }
                .map { track -> HomeHeroUpdate(track, section.title, false) }
        }
    val chartUpdates = state.charts
        .asSequence()
        .filter { isReliableMusicUpdateCandidate(it) }
        .map { track -> HomeHeroUpdate(track, "YouTube Charts Italia", false) }
    val libraryUpdates = sequenceOf(
        state.tracks.asSequence(),
        state.favorites.asSequence(),
        state.currentTrack?.let { sequenceOf(it) } ?: emptySequence()
    )
        .flatten()
        .filter { isReliableMusicUpdateCandidate(it) }
        .map { track -> HomeHeroUpdate(track, track.source.ifBlank { "YouTube Music" }, false) }
    return sequenceOf(verifiedReleases, trustedEditorial, chartUpdates, libraryUpdates)
        .flatten()
        .distinctBy { it.track.id }
        .firstOrNull { it.track.id != currentId }
        ?: state.currentTrack?.let { HomeHeroUpdate(it, it.source.ifBlank { "YouTube Music" }, false) }
}

private fun buildQuickPickTracks(state: LevyraUiState, heroTrack: Track?): List<Track> {
    val excluded = setOfNotNull(heroTrack?.id)
    return buildList {
        if (state.currentTrack != null) add(state.currentTrack)
        addAll(state.tracks)
        addAll(state.homeSections.flatMap { it.tracks })
        addAll(state.charts)
        addAll(state.favorites)
    }
        .filter { it.id.length == 11 && isReliableMusicUpdateCandidate(it) }
        .distinctBy { it.id }
        .filterNot { it.id in excluded }
        .take(4)
}

private fun buildPersonalListeningTracks(state: LevyraUiState): List<Track> {
    return buildList {
        state.currentTrack?.let { add(it) }
        addAll(state.recentSearches)
        addAll(state.favorites)
        addAll(state.tracks)
        addAll(state.homeSections.flatMap { it.tracks })
        addAll(state.charts)
    }
        .asSequence()
        .filter { it.id.length == 11 && isReliableMusicUpdateCandidate(it) }
        .filter { it.title.isNotBlank() && it.artist.isNotBlank() }
        .distinctBy { it.id }
        .take(12)
        .toList()
}

private fun buildResonanceTracks(state: LevyraUiState): List<Track> {
    return buildList {
        addAll(state.charts)
        addAll(state.homeSections.flatMap { it.tracks })
        addAll(state.favorites)
        addAll(state.tracks)
        state.currentTrack?.let { add(it) }
    }
        .asSequence()
        .filter { it.id.length == 11 && isReliableMusicUpdateCandidate(it) }
        .filter { it.title.isNotBlank() && it.artist.isNotBlank() }
        .distinctBy { it.id }
        .sortedWith(compareByDescending<Track> { it.replayScore + it.vocal + it.cacheScore / 2 }.thenBy { it.title })
        .take(8)
        .toList()
}

@Composable
private fun ResonanceShelf(
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (Track) -> Unit,
    onPlayAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Surface(
                        color = LevyraViolet.copy(alpha = 0.18f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, LevyraViolet.copy(alpha = 0.30f)),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.GraphicEq, null, tint = LevyraViolet, modifier = Modifier.size(17.dp))
                        }
                    }
                    Text(
                        text = "Voci che risuonano",
                        color = LevyraText,
                        fontSize = 28.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = "Le tracce più commentate, viste come energia viva",
                    color = LevyraMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Surface(
                color = Color.White.copy(alpha = 0.045f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.pressable(onClick = onPlayAll)
            ) {
                Text(
                    text = "Mix",
                    color = LevyraText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp)
                )
            }
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(end = 6.dp)
        ) {
            itemsIndexed(tracks) { index, track ->
                ResonanceCard(
                    track = track,
                    index = index,
                    active = track.id == currentId,
                    playing = isPlaying && track.id == currentId,
                    resolving = isResolving && track.id == currentId,
                    onClick = { onPlay(track) }
                )
            }
        }
    }
}

@Composable
private fun ResonanceCard(
    track: Track,
    index: Int,
    active: Boolean,
    playing: Boolean,
    resolving: Boolean,
    onClick: () -> Unit
) {
    val accentStart = Color(track.accentStart)
    val accentEnd = Color(track.accentEnd)
    val score = (track.replayScore + track.vocal + track.cacheScore / 2 + index * 7).coerceAtLeast(24)
    val comments = 520 + (score * 31) % 4200
    val pulseWidth = ((score % 72) + 24) / 100f
    Surface(
        color = Color.White.copy(alpha = if (active) 0.075f else 0.045f),
        border = BorderStroke(1.dp, if (active) LevyraViolet.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(30.dp),
        modifier = Modifier
            .width(316.dp)
            .height(206.dp)
            .pressable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(
                    listOf(
                        accentStart.copy(alpha = 0.24f),
                        Color(0xFF0D1019).copy(alpha = 0.98f),
                        accentEnd.copy(alpha = 0.18f)
                    )
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(190.dp)
                    .background(Brush.radialGradient(listOf(accentEnd.copy(alpha = 0.30f), Color.Transparent)))
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    CoverImage(
                        track = track,
                        modifier = Modifier
                            .size(66.dp)
                            .clip(RoundedCornerShape(21.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(21.dp)),
                        highRes = true
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(track.title, color = LevyraText, fontSize = 17.sp, lineHeight = 19.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, color = LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.42f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (resolving) {
                                CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp, color = LevyraViolet)
                            } else {
                                Icon(
                                    imageVector = if (playing) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = if (playing) LevyraViolet else LevyraText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.24f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Commenti totali", color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(formatCompactNumber(comments), color = LevyraText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Engagement", color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("${minOf(99, score % 100)}%", color = LevyraViolet, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(pulseWidth.coerceIn(0.1f, 1f))
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Brush.horizontalGradient(listOf(accentStart, accentEnd)))
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(pulseWidth)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Brush.horizontalGradient(listOf(LevyraViolet, LevyraCyan)))
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatCompactNumber(value: Int): String {
    return if (value >= 1000) {
        val major = value / 1000
        val minor = (value % 1000) / 100
        if (minor == 0) "$major K" else "$major,$minor K"
    } else {
        value.toString()
    }
}

@Composable
private fun PersonalListeningShelf(
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (Track) -> Unit,
    onPlayAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "La tua orbita",
                    color = LevyraText,
                    fontSize = 30.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "I brani che tornano sempre da te",
                    color = LevyraMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Surface(
                color = LevyraCyan.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.28f)),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.pressable(onClick = onPlayAll)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = LevyraCyan, modifier = Modifier.size(17.dp))
                    Text("Play", color = LevyraText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        val chunkedTracks = tracks.chunked(2)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(end = 6.dp)
        ) {
            itemsIndexed(chunkedTracks) { colIndex, colTracks ->
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    colTracks.forEachIndexed { rowIndex, track ->
                        PersonalListeningCard(
                            track = track,
                            rank = colIndex * 2 + rowIndex + 1,
                            active = track.id == currentId,
                            playing = isPlaying && track.id == currentId,
                            resolving = isResolving && track.id == currentId,
                            onClick = { onPlay(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalListeningCard(
    track: Track,
    rank: Int,
    active: Boolean,
    playing: Boolean,
    resolving: Boolean,
    onClick: () -> Unit
) {
    val accentStart = Color(track.accentStart)
    val accentEnd = Color(track.accentEnd)
    Surface(
        color = Color.White.copy(alpha = if (active) 0.08f else 0.04f),
        border = BorderStroke(1.dp, if (active) LevyraCyan.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 32.dp),
        modifier = Modifier
            .size(152.dp)
            .pressable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CoverImage(
                track = track,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.85f },
                highRes = false
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))))
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = "#${rank.toString().padStart(2, '0')}",
                            color = LevyraText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (playing || resolving) {
                        Surface(
                            color = LevyraCyan,
                            shape = CircleShape,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (resolving) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = LevyraBlack)
                                } else {
                                    Icon(Icons.Rounded.GraphicEq, null, tint = LevyraBlack, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = track.title,
                        color = LevyraText,
                        fontSize = 15.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = LevyraMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun isQuickPicksSectionTitle(title: String): Boolean {
    val normalized = title.lowercase()
    return normalized.contains("scelte rapide") ||
        normalized.contains("quick picks") ||
        normalized.contains("quick pick") ||
        normalized.contains("scelte per te")
}

private fun isVerifiedReleaseSectionTitle(title: String): Boolean {
    val normalized = title.lowercase()
    return normalized.contains("novità") ||
        normalized.contains("nuove uscite") ||
        normalized.contains("appena usciti") ||
        normalized.contains("ultime uscite") ||
        normalized.contains("nuovi album") ||
        normalized.contains("nuovi singoli") ||
        normalized.contains("new releases") ||
        normalized.contains("new release") ||
        normalized.contains("latest releases") ||
        normalized.contains("latest release") ||
        normalized.contains("new albums") ||
        normalized.contains("new singles")
}

private fun releaseKindFromSource(title: String, track: Track): String {
    val normalized = title.lowercase()
    val album = track.album.trim()
    return when {
        normalized.contains("album") -> "album"
        normalized.contains("single") || normalized.contains("singol") -> "singolo"
        album.isNotBlank() &&
            !album.equals("YouTube Music", ignoreCase = true) &&
            !album.equals(track.title, ignoreCase = true) -> "album"
        else -> "uscita"
    }
}

private fun isReliableMusicUpdateCandidate(track: Track): Boolean {
    val title = track.title.trim()
    val artist = track.artist.trim()
    if (title.length < 2 || artist.length < 2) return false
    if (artist.equals("YouTube Music", ignoreCase = true) || artist.equals("YouTube", ignoreCase = true)) return false
    return !isLikelyPlaylistOrCompilation(track)
}

private fun isLikelyPlaylistOrCompilation(track: Track): Boolean {
    val combined = listOf(track.title, track.artist, track.album).joinToString(" ").lowercase()
    val markers = listOf(
        "playlist",
        "mix",
        "top hit",
        "top hits",
        "hit italiane",
        "canzoni italiane",
        "musica italiana",
        "estate mix",
        "summer mix",
        "best of",
        "compilation",
        "classifica",
        "radio edit",
        "sped up",
        "slowed",
        "nightcore"
    )
    return markers.any { combined.contains(it) }
}

private data class HomeUpdateCopy(
    val badge: String,
    val headline: String,
    val detail: String,
    val caption: String,
    val sourceLabel: String,
    val primaryAction: String,
    val icon: ImageVector
)

private fun buildHomeUpdateCopy(update: HomeHeroUpdate): HomeUpdateCopy {
    val track = update.track
    val artist = track.artist.ifBlank { "Artista" }
    val title = track.title.ifBlank { "Brano" }
    val source = track.source.ifBlank { "YouTube Music" }
    val sourceTitle = update.sourceTitle.trim().ifBlank { source }
    val sourceLabel = buildProfessionalSourceLabel(source, sourceTitle)
    if (!update.verifiedRelease) {
        val chartDriven = isChartDrivenSource(sourceTitle) || isChartDrivenSource(source)
        return HomeUpdateCopy(
            badge = if (chartDriven) "TREND ITALIA" else "RADAR MUSICALE",
            headline = title,
            detail = artist,
            caption = if (chartDriven) "In evidenza nelle classifiche italiane." else "Selezionato oggi dal tuo flusso musicale.",
            sourceLabel = sourceLabel,
            primaryAction = "Ascolta",
            icon = if (chartDriven) Icons.Rounded.Equalizer else Icons.Rounded.GraphicEq
        )
    }
    val kind = releaseKindFromSource(sourceTitle, track)
    val album = track.album.trim().takeIf {
        it.isNotBlank() &&
            !it.equals("YouTube Music", ignoreCase = true) &&
            !it.equals(title, ignoreCase = true)
    }
    return when (kind) {
        "album" -> HomeUpdateCopy(
            badge = "NUOVO ALBUM",
            headline = album ?: title,
            detail = artist,
            caption = if (album != null) "Include anche “$title”." else "Disponibile nelle nuove uscite.",
            sourceLabel = sourceLabel,
            primaryAction = "Apri",
            icon = Icons.Rounded.Album
        )
        "singolo" -> HomeUpdateCopy(
            badge = "NUOVO SINGOLO",
            headline = title,
            detail = artist,
            caption = "Disponibile ora nelle nuove uscite.",
            sourceLabel = sourceLabel,
            primaryAction = "Apri",
            icon = Icons.Rounded.MusicNote
        )
        else -> HomeUpdateCopy(
            badge = "NUOVA USCITA",
            headline = title,
            detail = artist,
            caption = "Una novità appena entrata nel radar.",
            sourceLabel = sourceLabel,
            primaryAction = "Apri",
            icon = Icons.Rounded.MusicNote
        )
    }
}

private fun buildProfessionalSourceLabel(source: String, sourceTitle: String): String {
    return listOf(source, sourceTitle)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { it.removePrefix("Fonte:").trim() }
        .distinctBy { it.lowercase() }
        .joinToString(" · ")
        .ifBlank { "YouTube Music" }
}

private fun isChartDrivenSource(source: String): Boolean {
    val normalized = source.lowercase()
    return normalized.contains("chart") ||
        normalized.contains("classifica") ||
        normalized.contains("trend") ||
        normalized.contains("top")
}

@Composable
private fun HomeDiscoveryHero(
    update: HomeHeroUpdate,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onSave: () -> Unit
) {
    val track = update.track
    val accentStart = Color(track.accentStart)
    val accentEnd = Color(track.accentEnd)
    val copy = remember(track.id, track.title, track.artist, track.album, update.sourceTitle, update.verifiedRelease) {
        buildHomeUpdateCopy(update)
    }
    Surface(
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            accentStart.copy(alpha = 0.30f),
                            Color(0xFF07111F),
                            accentEnd.copy(alpha = 0.26f)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Icon(
                                    imageVector = copy.icon,
                                    contentDescription = null,
                                    tint = LevyraCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = copy.badge,
                                    color = LevyraText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            text = copy.headline,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 19.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(brush = Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet)))
                        )
                        Text(
                            text = copy.detail,
                            color = LevyraText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 17.sp
                        )
                        Text(
                            text = copy.caption,
                            color = LevyraMuted,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = copy.sourceLabel,
                            color = LevyraCyan.copy(alpha = 0.88f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(contentAlignment = Alignment.BottomEnd) {
                        CoverImage(
                            track = track,
                            modifier = Modifier
                                .size(86.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            highRes = true
                        )
                        Surface(
                            color = Color.Black.copy(alpha = 0.42f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier
                                .padding(6.dp)
                                .size(30.dp)
                                .pressable(onClick = onPlay)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = LevyraText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .pressable(onClick = onPlay)
                    ) {
                        Box(
                            modifier = Modifier.background(Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet))),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = copy.primaryAction,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.045f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
                        modifier = Modifier
                            .weight(0.68f)
                            .height(40.dp)
                            .pressable(onClick = onSave)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (isFavorite) LevyraPink else LevyraText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isFavorite) "Salvato" else "Salva",
                                    color = LevyraText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueListeningCard(
    track: Track,
    isPlaying: Boolean,
    isResolving: Boolean,
    progress: Float,
    onResume: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.045f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.075f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .pressable(onClick = onResume)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CoverImage(
                    track = track,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(13.dp))
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Headphones,
                            contentDescription = null,
                            tint = LevyraCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Continua ad ascoltare",
                            color = LevyraCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.title,
                        color = LevyraText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = LevyraMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.18f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when {
                            isResolving -> CircularProgressIndicator(
                                modifier = Modifier.size(17.dp),
                                strokeWidth = 2.dp,
                                color = LevyraCyan
                            )
                            isPlaying -> Icon(
                                imageVector = Icons.Rounded.Equalizer,
                                contentDescription = null,
                                tint = LevyraCyan,
                                modifier = Modifier.size(19.dp)
                            )
                            else -> Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = LevyraText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(2.dp)
                    .background(Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet)))
            )
        }
    }
}

@Composable
private fun HomeShortcutRow(
    hasTracks: Boolean,
    onShuffle: () -> Unit,
    onFavorites: () -> Unit,
    onNewReleases: () -> Unit,
    onGenres: () -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            QuickAction(
                icon = Icons.Rounded.Shuffle,
                label = "Mix per te",
                accent = LevyraCyan,
                enabled = hasTracks,
                modifier = Modifier.width(176.dp),
                onClick = onShuffle
            )
        }
        item {
            QuickAction(
                icon = Icons.Rounded.Favorite,
                label = "Preferiti",
                accent = LevyraPink,
                enabled = true,
                modifier = Modifier.width(176.dp),
                onClick = onFavorites
            )
        }
        item {
            QuickAction(
                icon = Icons.Rounded.Bolt,
                label = "Nuove uscite",
                accent = LevyraViolet,
                enabled = true,
                modifier = Modifier.width(176.dp),
                onClick = onNewReleases
            )
        }
        item {
            QuickAction(
                icon = Icons.Rounded.MusicNote,
                label = "Generi",
                accent = Color(0xFFB7C7FF),
                enabled = true,
                modifier = Modifier.width(176.dp),
                onClick = onGenres
            )
        }
    }
}

@Composable
private fun QuickSectionHeader(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            color = LevyraText,
            fontSize = 23.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f)
        )
        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier.pressable(onClick = onAction)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = LevyraCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = actionLabel,
                    color = LevyraText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun QuickSongList(
    tracks: List<Track>,
    currentId: String?,
    favoriteIds: Set<String>,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onOpenPlayer: (Track) -> Unit,
    onOffline: (Track) -> Unit,
    onArtist: (Track) -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        tracks.take(4).forEach { track ->
            QuickSongRow(
                track = track,
                isCurrent = track.id == currentId,
                isPlaying = isPlaying && track.id == currentId,
                isResolving = isResolving && track.id == currentId,
                isFavorite = track.id in favoriteIds,
                onPlay = { onPlay(track) },
                onFavorite = { onFavorite(track) },
                onAddToQueue = { onAddToQueue(track) },
                onOpenPlayer = { onOpenPlayer(track) },
                onOffline = { onOffline(track) },
                onArtist = { onArtist(track) }
            )
        }
    }
}

@Composable
private fun QuickSongRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onAddToQueue: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOffline: () -> Unit,
    onArtist: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .pressable(onClick = onPlay)
            .padding(horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(58.dp),
            contentAlignment = Alignment.Center
        ) {
            CoverImage(
                track = track,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            if (isCurrent) {
                Surface(
                    color = Color.Black.copy(alpha = 0.46f),
                    shape = CircleShape,
                    modifier = Modifier.size(25.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when {
                            isResolving -> CircularProgressIndicator(
                                modifier = Modifier.size(13.dp),
                                strokeWidth = 2.dp,
                                color = LevyraCyan
                            )
                            isPlaying -> Icon(
                                imageVector = Icons.Rounded.Equalizer,
                                contentDescription = null,
                                tint = LevyraCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            else -> Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                color = if (isCurrent) LevyraCyan else LevyraText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = track.artist,
                color = LevyraMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onArtist() }
            )
        }
        IconButton(onClick = onFavorite, modifier = Modifier.size(38.dp)) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Preferito",
                tint = if (isFavorite) LevyraPink else LevyraMuted,
                modifier = Modifier.size(23.dp)
            )
        }
        TrackOverflowMenu(
            track = track,
            iconSize = 21.dp,
            buttonSize = 38.dp,
            onAddToQueue = onAddToQueue,
            onOpenPlayer = onOpenPlayer,
            onOffline = onOffline,
            onArtist = onArtist
        )
    }
}

@Composable
private fun TrackOverflowMenu(
    track: Track,
    iconSize: androidx.compose.ui.unit.Dp = 23.dp,
    buttonSize: androidx.compose.ui.unit.Dp = 48.dp,
    onAddToQueue: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOffline: () -> Unit,
    onArtist: () -> Unit = {}
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Azioni",
                tint = LevyraMuted,
                modifier = Modifier.size(iconSize)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Aggiungi alla coda") },
                leadingIcon = { Icon(Icons.Rounded.QueueMusic, null) },
                onClick = {
                    expanded = false
                    onAddToQueue()
                }
            )
            DropdownMenuItem(
                text = { Text("Condividi") },
                leadingIcon = { Icon(Icons.Rounded.Share, null) },
                onClick = {
                    expanded = false
                    val shareText = buildString {
                        append(track.title)
                        if (track.artist.isNotBlank()) append(" - ").append(track.artist)
                        val link = track.videoUrl.ifBlank { track.streamUrl }
                        if (link.isNotBlank()) append("\n").append(link)
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Condividi brano"))
                }
            )
            DropdownMenuItem(
                text = { Text("Vai al player") },
                leadingIcon = { Icon(Icons.Rounded.PlayArrow, null) },
                onClick = {
                    expanded = false
                    onOpenPlayer()
                }
            )
            DropdownMenuItem(
                text = { Text("Apri artista") },
                leadingIcon = { Icon(Icons.Rounded.Person, null) },
                onClick = {
                    expanded = false
                    onArtist()
                }
            )
            DropdownMenuItem(
                text = { Text("Salva offline") },
                leadingIcon = { Icon(Icons.Rounded.LibraryMusic, null) },
                onClick = {
                    expanded = false
                    onOffline()
                }
            )
        }
    }
}

@Composable
private fun ChartRegionRow(regions: List<com.luc4n3x.levyra.domain.ChartRegion>, selectedId: String, loading: Boolean, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LevyraCyan)
        }
        regions.forEach { region ->
            val selected = region.id == selectedId
            Surface(
                color = if (selected) LevyraCyan.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f),
                border = BorderStroke(1.dp, if (selected) LevyraCyan else Color.White.copy(alpha = 0.1f)),
                shape = CircleShape,
                modifier = Modifier.pressable(onClick = { onSelect(region.id) })
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(region.emoji, fontSize = 14.sp)
                    Text(region.label, color = if (selected) LevyraCyan else LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(viewModel: LevyraViewModel, state: LevyraUiState) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SearchHeader(
            query = state.query,
            isSearching = state.isSearching,
            onQuery = viewModel::setQuery,
            onSearch = { query ->
                focusManager.clearFocus()
                keyboardController?.hide()
                viewModel.searchNow(query)
            },
            onClear = {
                viewModel.setQuery("")
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (state.currentTrack != null) 188.dp else 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val queryClean = state.query.trim()

            if (queryClean.isEmpty()) {
                if (state.recentSearches.isNotEmpty()) {
                    item {
                        RecentSearchesRow(
                            tracks = state.recentSearches,
                            onTrackClick = { track ->
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                viewModel.play(track)
                            }
                        )
                    }
                }

                if (state.charts.isNotEmpty()) {
                    item {
                        MetroDiscoveryRail(
                            tracks = state.charts.take(8),
                            currentId = state.currentTrack?.id,
                            onPlay = { track ->
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                viewModel.playFrom(state.charts, track)
                            }
                        )
                    }
                }

                item {
                    val fallbackSuggestions = listOf(
                        "samurai jay",
                        "shiva",
                        "sfera ebbasta",
                        "guè",
                        "tiziano ferro",
                        "pinguini tattici nucleari",
                        "irama",
                        "marracash",
                        "fedez"
                    )
                    SuggestionsList(
                        title = "Esplora artisti",
                        suggestions = fallbackSuggestions,
                        onSuggestionClick = { query ->
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.setQuery(query)
                            viewModel.searchNow(query)
                        }
                    )
                }
            } else if (state.searchSuggestions.isNotEmpty() && !state.isSearching && state.searchResults.isEmpty() && state.searchError == null) {
                item {
                    SuggestionsList(
                        title = "Suggerimenti",
                        suggestions = state.searchSuggestions,
                        onSuggestionClick = { suggestion ->
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.setQuery(suggestion)
                            viewModel.searchNow(suggestion)
                        }
                    )
                }
            } else if (state.searchResults.isEmpty() && !state.isSearching && state.searchError == null) {
                item {
                    QuickChips(
                        onClick = { query ->
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.setQuery(query)
                            viewModel.searchNow(query)
                        }
                    )
                }
            }

            if (queryClean.isNotEmpty()) {
                when {
                    state.isSearching -> item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = LevyraCyan)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Sto cercando…", color = LevyraMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    state.searchError != null -> item { GlassMessage(state.searchError, LevyraOrange) }
                    !state.searchData.isEmpty -> {
                        val data = state.searchData
                        val filter = state.searchFilter
                        item {
                            SearchFilterChips(
                                selected = filter,
                                hasArtists = data.artists.isNotEmpty(),
                                hasAlbums = data.albums.isNotEmpty(),
                                onSelect = viewModel::setSearchFilter
                            )
                        }
                        if (filter == SearchFilter.All && data.topTrack != null) {
                            item {
                                TopResultCard(
                                    track = data.topTrack,
                                    isCurrent = data.topTrack.id == state.currentTrack?.id,
                                    isPlaying = state.isPlaying && data.topTrack.id == state.currentTrack?.id,
                                    isResolving = state.isResolving && data.topTrack.id == state.currentTrack?.id,
                                    isFavorite = data.topTrack.id in state.favoriteIds,
                                    onPlay = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        viewModel.playFrom(data.songs, data.topTrack)
                                    },
                                    onFavorite = { viewModel.toggleFavorite(data.topTrack) },
                                    onArtist = { viewModel.openArtist(data.topTrack) }
                                )
                            }
                        }
                        if ((filter == SearchFilter.All || filter == SearchFilter.Artists) && data.artists.isNotEmpty()) {
                            item { SectionTitle("👤 Artisti") }
                            item {
                                ArtistHitRow(
                                    artists = data.artists,
                                    onClick = { hit ->
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        viewModel.openArtistFromHit(hit)
                                    }
                                )
                            }
                        }
                        if ((filter == SearchFilter.All || filter == SearchFilter.Albums) && data.albums.isNotEmpty()) {
                            item { SectionTitle("💿 Album e singoli") }
                            item {
                                AlbumHitRow(
                                    albums = data.albums,
                                    onClick = { album ->
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        viewModel.searchAlbum(album)
                                    }
                                )
                            }
                        }
                        if (filter == SearchFilter.All || filter == SearchFilter.Songs) {
                            val songs = if (filter == SearchFilter.All) data.songs.drop(if (data.topTrack != null) 1 else 0) else data.songs
                            if (songs.isNotEmpty()) {
                                item { SectionTitle("🎵 Brani") }
                                items(songs, key = { "search-song-${it.id}" }) { track ->
                                    SearchTrackCard(
                                        track = track,
                                        isCurrent = track.id == state.currentTrack?.id,
                                        isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                                        isResolving = state.isResolving && track.id == state.currentTrack?.id,
                                        isFavorite = track.id in state.favoriteIds,
                                        isDownloading = track.id in state.downloadingTrackIds,
                                        isDownloaded = track.id in state.downloadedTrackIds,
                                        onClick = {
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                            viewModel.playFrom(data.songs, track)
                                        },
                                        onFavorite = { viewModel.toggleFavorite(track) },
                                        onDownload = { viewModel.exportTrack(track) },
                                        onArtist = { viewModel.openArtist(track) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHeader(
    query: String,
    isSearching: Boolean,
    onQuery: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = {
                onClear()
                focusManager.clearFocus()
                keyboardController?.hide()
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = "Indietro",
                tint = LevyraText
            )
        }

        Surface(
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            shape = CircleShape,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                    singleLine = true,
                    textStyle = TextStyle(color = LevyraText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    cursorBrush = SolidColor(LevyraCyan),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text("Cerca brani, artisti e...", color = LevyraMuted, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        }
                        innerTextField()
                    }
                )

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cancella",
                            tint = LevyraMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = {  },
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.Mic,
                contentDescription = "Voce",
                tint = LevyraText,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(
            onClick = {  },
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.Equalizer,
                contentDescription = "Soundwave",
                tint = LevyraText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RecentSearchesRow(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Ricerche recenti",
            color = LevyraText,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(tracks, key = { "recent-${it.id}" }) { track ->
                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .pressable { onTrackClick(track) },
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.5f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                    ) {
                        CoverImage(
                            track = track,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = track.title,
                        color = LevyraText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = LevyraMuted,
                        fontSize = 11.sp,
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
private fun SuggestionsList(
    title: String,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                color = LevyraText,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.018f),
                            LevyraCyan.copy(alpha = 0.018f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            suggestions.forEachIndexed { index, suggestion ->
                val accent = if (index % 2 == 0) LevyraCyan else LevyraViolet
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.012f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            LevyraCyan.copy(alpha = 0.18f),
                                            LevyraViolet.copy(alpha = 0.14f)
                                        )
                                    ),
                                    CircleShape
                                )
                                .border(1.dp, accent.copy(alpha = 0.34f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = suggestion,
                            color = LevyraText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Completa",
                            tint = accent.copy(alpha = 0.78f),
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = 45f }
                        )
                    }
                }
                if (index != suggestions.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 64.dp, end = 14.dp)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.06f),
                                        Color.White.copy(alpha = 0.018f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(viewModel: LevyraViewModel, state: LevyraUiState) {
    var addTarget by remember { mutableStateOf<Track?>(null) }
    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (state.currentTrack != null) 188.dp else 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { PageHeader("Libreria", "Playlist, preferiti, download e cronologia") }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionTitle("🎵 Le tue playlist")
                var showCreate by remember { mutableStateOf(false) }
                TextButton(onClick = { showCreate = true }) {
                    Icon(Icons.Rounded.Add, null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Nuova", color = LevyraCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                if (showCreate) {
                    PlaylistCreateDialog(
                        onDismiss = { showCreate = false },
                        onConfirm = { name ->
                            viewModel.createPlaylist(name)
                            showCreate = false
                        }
                    )
                }
            }
        }
        if (state.playlists.isEmpty()) {
            item { EmptyState("Crea una playlist e aggiungi i tuoi brani preferiti") }
        } else {
            items(state.playlists, key = { "pl-${it.id}" }) { playlist ->
                PlaylistRow(
                    playlist = playlist,
                    onOpen = { viewModel.openPlaylist(playlist.id) },
                    onPlay = { viewModel.playPlaylist(playlist.id) },
                    onDelete = { viewModel.deletePlaylist(playlist.id) }
                )
            }
        }

        item { SectionTitle("⬇️ Download offline") }
        if (state.downloads.isEmpty()) {
            item { EmptyState("Tocca l'icona di download su un brano per salvarlo in Music/Levyra") }
        } else {
            items(state.downloads, key = { "dl-${it.id}" }) { download ->
                DownloadRow(
                    download = download,
                    isCurrent = download.trackId == state.currentTrack?.id,
                    onDelete = { viewModel.deleteDownload(download) }
                )
            }
        }
        item { SectionTitle("❤️ Preferiti") }
        if (state.favorites.isEmpty()) {
            item { EmptyState("Tocca il cuore su un brano per salvarlo qui") }
        } else {
            items(state.favorites, key = { "fav-${it.id}" }) { track ->
                TrackRow(
                    track = track,
                    isCurrent = track.id == state.currentTrack?.id,
                    isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                    isResolving = state.isResolving && track.id == state.currentTrack?.id,
                    isFavorite = true,
                    onClick = { viewModel.playFrom(state.favorites, track) },
                    onFavorite = { viewModel.toggleFavorite(track) },
                    isDownloading = track.id in state.downloadingTrackIds,
                    isDownloaded = track.id in state.downloadedTrackIds,
                    onDownload = { viewModel.exportTrack(track) },
                    onArtist = { viewModel.openArtist(track) },
                    onAddToPlaylist = { addTarget = track }
                )
            }
        }
        item { SectionTitle("🕒 Trovati di recente") }
        items(state.tracks, key = { it.id }) { track ->
            TrackRow(
                track = track,
                isCurrent = track.id == state.currentTrack?.id,
                isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                isResolving = state.isResolving && track.id == state.currentTrack?.id,
                isFavorite = track.id in state.favoriteIds,
                onClick = { viewModel.playFrom(state.tracks, track) },
                onFavorite = { viewModel.toggleFavorite(track) },
                isDownloading = track.id in state.downloadingTrackIds,
                isDownloaded = track.id in state.downloadedTrackIds,
                onDownload = { viewModel.exportTrack(track) },
                onArtist = { viewModel.openArtist(track) },
                onAddToPlaylist = { addTarget = track }
            )
        }
    }

    addTarget?.let { track ->
        AddToPlaylistDialog(
            track = track,
            playlists = state.playlists,
            onDismiss = { addTarget = null },
            onAddTo = { playlistId ->
                viewModel.addToPlaylist(playlistId, track)
                addTarget = null
            },
            onCreateWith = { name ->
                viewModel.createPlaylist(name, track)
                addTarget = null
            }
        )
    }
    }
}

@Composable
private fun DownloadRow(download: DownloadedTrack, isCurrent: Boolean, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(LevyraCyan.copy(alpha = 0.32f), LevyraViolet.copy(alpha = 0.32f)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.DownloadDone, null, tint = LevyraCyan, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(download.title, color = if (isCurrent) LevyraCyan else LevyraText, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(download.artist, color = LevyraMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (download.embeddedMetadata) "Music/Levyra · cover e tag" else "Music/Levyra",
                color = LevyraMuted.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = "Rimuovi", tint = LevyraMuted, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: com.luc4n3x.levyra.domain.Playlist,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onOpen() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(LevyraCyan.copy(alpha = 0.32f), LevyraViolet.copy(alpha = 0.32f)))),
            contentAlignment = Alignment.Center
        ) {
            if (playlist.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = playlist.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Rounded.QueueMusic, null, tint = LevyraCyan, modifier = Modifier.size(26.dp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(playlist.name, color = LevyraText, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (playlist.size == 1) "1 brano" else "${playlist.size} brani",
                color = LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium
            )
        }
        IconButton(onClick = onPlay) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Riproduci", tint = LevyraCyan, modifier = Modifier.size(26.dp))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = "Elimina", tint = LevyraMuted, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun PlaylistCreateDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LevyraPanel,
        title = { Text("Nuova playlist", color = LevyraText, fontWeight = FontWeight.Black) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("Nome della playlist", color = LevyraMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = LevyraText,
                    unfocusedTextColor = LevyraText,
                    focusedBorderColor = LevyraCyan,
                    unfocusedBorderColor = LevyraMuted.copy(alpha = 0.4f),
                    cursorColor = LevyraCyan
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) {
                Text("Crea", color = LevyraCyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla", color = LevyraMuted) }
        }
    )
}

/** Dialog per aggiungere un brano a una playlist esistente o crearne una nuova al volo. */
@Composable
private fun AddToPlaylistDialog(
    track: Track,
    playlists: List<com.luc4n3x.levyra.domain.Playlist>,
    onDismiss: () -> Unit,
    onAddTo: (String) -> Unit,
    onCreateWith: (String) -> Unit
) {
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LevyraPanel,
        title = { Text("Aggiungi a playlist", color = LevyraText, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (creating) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        placeholder = { Text("Nome nuova playlist", color = LevyraMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LevyraText,
                            unfocusedTextColor = LevyraText,
                            focusedBorderColor = LevyraCyan,
                            unfocusedBorderColor = LevyraMuted.copy(alpha = 0.4f),
                            cursorColor = LevyraCyan
                        )
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { creating = true }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = LevyraCyan)
                        Text("Crea nuova playlist", color = LevyraCyan, fontWeight = FontWeight.Bold)
                    }
                    playlists.forEach { pl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAddTo(pl.id) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.QueueMusic, null, tint = LevyraMuted)
                            Text(pl.name, color = LevyraText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                TextButton(onClick = { if (name.isNotBlank()) onCreateWith(name) }) {
                    Text("Crea e aggiungi", color = LevyraCyan, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi", color = LevyraMuted) }
        }
    )
}

/** Overlay a schermo intero con il contenuto di una playlist. */
@Composable
private fun PlaylistDetailOverlay(viewModel: LevyraViewModel, state: LevyraUiState) {
    val playlist = state.openPlaylist ?: return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevyraInk)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = if (state.currentTrack != null) 188.dp else 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.closePlaylist() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Indietro", tint = LevyraText)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(playlist.name, color = LevyraText, fontSize = 26.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (playlist.size == 1) "1 brano" else "${playlist.size} brani", color = LevyraMuted, fontSize = 14.sp)
                    }
                    if (playlist.tracks.isNotEmpty()) {
                        IconButton(onClick = { viewModel.playPlaylist(playlist.id) }) {
                            Icon(Icons.Rounded.PlaylistPlay, contentDescription = "Riproduci tutto", tint = LevyraCyan, modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }
            if (playlist.tracks.isEmpty()) {
                item { EmptyState("Playlist vuota. Aggiungi brani dai tre puntini su un brano.") }
            } else {
                items(playlist.tracks, key = { "pldetail-${it.id}" }) { track ->
                    TrackRow(
                        track = track,
                        isCurrent = track.id == state.currentTrack?.id,
                        isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                        isResolving = state.isResolving && track.id == state.currentTrack?.id,
                        isFavorite = track.id in state.favoriteIds,
                        onClick = { viewModel.playPlaylist(playlist.id, track.id) },
                        onFavorite = { viewModel.toggleFavorite(track) },
                        isDownloading = track.id in state.downloadingTrackIds,
                        isDownloaded = track.id in state.downloadedTrackIds,
                        onDownload = { viewModel.exportTrack(track) },
                        onArtist = { viewModel.openArtist(track) },
                        onRemove = { viewModel.removeFromPlaylist(playlist.id, track.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerScreen(viewModel: LevyraViewModel, state: LevyraUiState) {
    val track = state.currentTrack
    val bgStart = track?.let { Color(it.accentStart) } ?: LevyraCyan
    val bgEnd = track?.let { Color(it.accentEnd) } ?: LevyraViolet
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgStart.copy(alpha = 0.6f), bgEnd.copy(alpha = 0.15f), LevyraBlack, LevyraBlack)))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 130.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.size(46.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("IN RIPRODUZIONE", color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text(track?.source ?: "LEVYRA", color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                CircleIconButton(
                    icon = Icons.Rounded.QueueMusic,
                    tint = LevyraText,
                    background = Color.White.copy(alpha = 0.08f),
                    onClick = { viewModel.openQueue() }
                )
            }
        }
        if (track == null) {
            item { EmptyState("Cerca un brano e premi play") }
        } else {
            item {
                if (track.videoUrl.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(modifier = Modifier.padding(4.dp)) {
                                Surface(
                                    color = if (!state.isVideoMode) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.clickable { if (state.isVideoMode) viewModel.toggleVideoMode() }
                                ) {
                                    Text("Brano", color = if (!state.isVideoMode) Color.White else LevyraMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                                }
                                Surface(
                                    color = if (state.isVideoMode) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.clickable { if (!state.isVideoMode) viewModel.toggleVideoMode() }
                                ) {
                                    Text("Video", color = if (state.isVideoMode) Color.White else LevyraMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                                }
                            }
                        }
                    }
                }
                
                if (state.isVideoMode && track.videoUrl.isNotBlank()) {
                    val exo = com.luc4n3x.levyra.player.PlaybackService.activePlayer
                    if (exo != null) {
                        AndroidView(
                            factory = { ctx ->
                                androidx.media3.ui.PlayerView(ctx).apply {
                                    useController = false
                                    setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_ALWAYS)
                                    setBackgroundColor(android.graphics.Color.BLACK)
                                    // Collego la surface direttamente all'ExoPlayer che decodifica i frame.
                                    player = exo
                                }
                            },
                            update = { view ->
                                val current = com.luc4n3x.levyra.player.PlaybackService.activePlayer
                                if (view.player !== current) view.player = current
                            },
                            onRelease = { view ->
                                view.player = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = LevyraCyan, strokeWidth = 3.dp)
                        }
                    }
                } else {
                    FloatingArtwork(
                        track = track,
                        isPlaying = state.isPlaying,
                        isResolving = state.isResolving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            color = LevyraText,
                            fontSize = 36.sp,
                            lineHeight = 40.sp,
                            letterSpacing = (-0.8).sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = track.artist,
                            color = LevyraMuted.copy(alpha = 0.9f),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { viewModel.openArtist(track) }
                        )
                    }
                    CircleIconButton(
                        icon = if (track.id in state.favoriteIds) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        tint = if (track.id in state.favoriteIds) LevyraPink else LevyraText,
                        background = if (track.id in state.favoriteIds) LevyraPink.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.08f),
                        onClick = { viewModel.toggleFavorite(track) }
                    )
                }
            }
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = progressOf(state.positionMs, state.durationMs),
                        onValueChange = viewModel::seekTo,
                        colors = SliderDefaults.colors(
                            thumbColor = LevyraCyan,
                            activeTrackColor = LevyraCyan,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatDuration(state.positionMs), color = LevyraMuted, fontSize = 12.sp)
                        Text(formatDuration(state.durationMs), color = LevyraMuted, fontSize = 12.sp)
                    }
                }
            }
            item {
                MainPlayerControls(
                    isPlaying = state.isPlaying,
                    isResolving = state.isResolving,
                    shuffleOn = state.shuffleEnabled,
                    repeatMode = state.repeatMode,
                    onShuffle = viewModel::toggleShuffle,
                    onPrevious = viewModel::previous,
                    onToggle = viewModel::togglePlay,
                    onNext = viewModel::next,
                    onRepeat = viewModel::toggleRepeat
                )
            }
            item {
                PlayerOptionsRow(
                    speed = state.playbackSpeed,
                    sleepMinutes = state.sleepTimerMinutes,
                    exporting = state.isOfflineExporting,
                    metadataWriterReady = state.embeddedMetadataWriterReady,
                    audioQuality = state.audioQuality,
                    onSpeed = viewModel::cycleSpeed,
                    onSleep = viewModel::cycleSleepTimer,
                    onQuality = viewModel::openAudioQualityPanel,
                    onExport = viewModel::exportCurrentTrack
                )
            }
            item { PlayerError(state.playerError) }
            item {
                LyricsButton(
                    loading = state.lyricsLoading,
                    available = state.lyrics.isNotEmpty(),
                    onClick = { viewModel.openLyrics() }
                )
            }
        }
    }
    }
}

@Composable
private fun AudioQualityPanel(
    selected: String,
    volumePercent: Int,
    onSelect: (String) -> Unit,
    onClose: () -> Unit
) {
    val blocker = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f))
            .clickable(interactionSource = blocker, indication = null) { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = Color(0xFF11131C),
            shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .clickable(interactionSource = blocker, indication = null) {}
        ) {
            Column(
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(56.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.38f))
                )
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF667AA9).copy(alpha = 0.95f),
                                    Color(0xFF536890).copy(alpha = 0.98f)
                                )
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Headphones, null, tint = LevyraText.copy(alpha = 0.86f), modifier = Modifier.size(28.dp))
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Speaker telefono", color = LevyraText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                                Surface(color = Color.White.copy(alpha = 0.10f), shape = RoundedCornerShape(999.dp)) {
                                    Text(
                                        text = "Connesso",
                                        color = LevyraText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.GraphicEq, null, tint = LevyraText, modifier = Modifier.size(17.dp))
                                Text("$volumePercent%", color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
                Surface(
                    color = Color(0xFF191C26),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(78.dp)
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.33f)
                                .height(78.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color(0xFF566A96))
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 22.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Rounded.GraphicEq, null, tint = LevyraText, modifier = Modifier.size(28.dp))
                                Text("Volume", color = LevyraText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.28f))
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Qualità audio", color = LevyraText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Auto" to "Auto", "Alta" to "High", "Bassa" to "Low").forEach { (label, quality) ->
                            AudioQualityChoice(
                                label = label,
                                selected = selected.equals(quality, ignoreCase = true),
                                modifier = Modifier.weight(1f),
                                onClick = { onSelect(quality) }
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Surface(
                        color = Color(0xFFB8C8F4),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .width(132.dp)
                            .height(58.dp)
                            .pressable(onClick = onClose)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Fine", color = Color(0xFF263049), fontSize = 16.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioQualityChoice(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        color = if (selected) Color(0xFFB8C8F4) else Color.White.copy(alpha = 0.035f),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, if (selected) Color.Transparent else Color.White.copy(alpha = 0.05f)),
        modifier = modifier
            .height(64.dp)
            .pressable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (selected) Color(0xFF263049) else LevyraMuted,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun LyricsButton(loading: Boolean, available: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(LevyraViolet.copy(alpha = 0.55f), LevyraCyan.copy(alpha = 0.45f), LevyraPink.copy(alpha = 0.5f))
                )
            )
            .pressable(enabled = !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Text(
                text = when {
                    loading -> "Cerco il testo…"
                    available -> "Mostra il testo"
                    else -> "Testo non disponibile"
                },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun MainPlayerControls(
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatMode: com.luc4n3x.levyra.domain.RepeatMode,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onShuffle) {
            Icon(Icons.Rounded.Shuffle, "Shuffle", tint = if (shuffleOn) LevyraCyan else LevyraMuted, modifier = Modifier.size(24.dp))
        }
        IconButton(onClick = onPrevious, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Rounded.SkipPrevious, null, tint = LevyraText, modifier = Modifier.size(36.dp))
        }
        Box(
            modifier = Modifier
                .size(88.dp)
                .graphicsLayer {
                    shadowElevation = 18f
                    shape = CircleShape
                }
                .background(Brush.linearGradient(listOf(LevyraCyan, LevyraViolet)), CircleShape)
                .pressable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (isResolving) CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 4.dp, color = LevyraBlack)
            else Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = LevyraBlack, modifier = Modifier.size(46.dp))
        }
        IconButton(onClick = onNext, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Rounded.SkipNext, null, tint = LevyraText, modifier = Modifier.size(36.dp))
        }
        IconButton(onClick = onRepeat) {
            val icon = if (repeatMode == com.luc4n3x.levyra.domain.RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
            val tint = if (repeatMode == com.luc4n3x.levyra.domain.RepeatMode.Off) LevyraMuted else LevyraCyan
            Icon(icon, "Repeat", tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun PlayerOptionsRow(
    speed: Float,
    sleepMinutes: Int,
    exporting: Boolean,
    metadataWriterReady: Boolean,
    audioQuality: String,
    onSpeed: () -> Unit,
    onSleep: () -> Unit,
    onQuality: () -> Unit,
    onExport: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OptionChip(
            icon = Icons.Rounded.Speed,
            label = "${trimSpeed(speed)}x",
            active = speed != 1f,
            modifier = Modifier.weight(1f),
            onClick = onSpeed
        )
        OptionChip(
            icon = Icons.Rounded.Bedtime,
            label = if (sleepMinutes > 0) "${sleepMinutes}m" else "Timer",
            active = sleepMinutes > 0,
            modifier = Modifier.weight(1f),
            onClick = onSleep
        )
        OptionChip(
            icon = Icons.Rounded.Equalizer,
            label = audioQuality,
            active = audioQuality != "Auto",
            modifier = Modifier.weight(1f),
            onClick = onQuality
        )
        OptionChip(
            icon = Icons.Rounded.Album,
            label = when {
                exporting -> "Salvo…"
                metadataWriterReady -> "M4A+"
                else -> "Salva"
            },
            active = exporting || metadataWriterReady,
            enabled = !exporting,
            modifier = Modifier.weight(1f),
            onClick = onExport
        )
    }
}

@Composable
private fun OptionChip(icon: ImageVector, label: String, active: Boolean, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val alpha = if (enabled) 1f else 0.62f
    Surface(
        color = if (active) LevyraCyan.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, if (active) LevyraCyan.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(48.dp)
            .graphicsLayer { this.alpha = alpha }
            .pressable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (active) LevyraCyan else LevyraText, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = if (active) LevyraCyan else LevyraText, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ShareOptionChip(track: Track, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        shape = CircleShape,
        modifier = modifier
            .size(48.dp)
            .pressable {
                val link = track.videoUrl.ifBlank { "https://music.youtube.com/watch?v=${track.id}" }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "${track.title} — ${track.artist}")
                    putExtra(Intent.EXTRA_TEXT, "${track.title} — ${track.artist}\n$link")
                }
                context.startActivity(Intent.createChooser(intent, "Condividi brano"))
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Share, null, tint = LevyraText, modifier = Modifier.size(20.dp))
        }
    }
}

private fun trimSpeed(speed: Float): String {
    return if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0').trimEnd('.')
}

@Composable
private fun OnboardingOverlay(tastes: List<Taste>, onDone: (String, Set<String>) -> Unit) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    var name by remember { mutableStateOf("") }
    val blocker = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0B0F1C), LevyraBlack))
            )

            .clickable(interactionSource = blocker, indication = null) {}
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 30.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Brush.linearGradient(listOf(LevyraCyan, LevyraViolet)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎵", fontSize = 30.sp)
                }
            }
            item {
                Text("Ciao 👋", color = LevyraText, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Text("Come ti chiami?", color = LevyraMuted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = TextStyle(color = LevyraText, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp).background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp)).padding(16.dp),
                    cursorBrush = SolidColor(LevyraCyan)
                )
                Text(
                    "Cosa ti va di ascoltare? Scegli i tuoi generi e ti prepariamo la home.",
                    color = LevyraMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            items(tastes.chunked(2)) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { taste ->
                        TasteCard(
                            taste = taste,
                            selected = taste.id in selected,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selected = if (taste.id in selected) selected - taste.id else selected + taste.id
                            }
                        )
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(22.dp)
        ) {
            GradientButton(
                text = if (selected.isEmpty() && name.isBlank()) "Salta e continua" else "Inizia ad ascoltare",
                onClick = { onDone(name, selected) }
            )
        }
    }
}

@Composable
private fun TasteCard(taste: Taste, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        color = if (selected) LevyraCyan.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.5.dp, if (selected) LevyraCyan else Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .height(76.dp)
            .pressable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(taste.emoji, fontSize = 26.sp)
            Text(
                taste.label,
                color = LevyraText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsOverlay(
    animationsEnabled: Boolean,
    dynamicColor: Boolean,
    sponsorBlock: Boolean,
    skipSilence: Boolean,
    updateInfo: AppUpdateInfo?,
    isCheckingUpdates: Boolean,
    onAnimations: (Boolean) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onSponsorBlock: (Boolean) -> Unit,
    onSkipSilence: (Boolean) -> Unit,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onRedoQuestionnaire: () -> Unit,
    onClose: () -> Unit
) {
    val blocker = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0F1C), LevyraBlack)))
            .clickable(interactionSource = blocker, indication = null) {}
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Impostazioni", color = LevyraText, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text("Personalizza LEVYRA", color = LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    CircleIconButton(
                        icon = Icons.Rounded.Close,
                        tint = LevyraText,
                        background = Color.White.copy(alpha = 0.08f),
                        onClick = onClose
                    )
                }
            }
            item { SettingsSectionLabel("DESIGN") }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.Bolt,
                    title = "Animazioni",
                    subtitle = "Effetti, transizioni e pressione delle card",
                    checked = animationsEnabled,
                    onCheckedChange = onAnimations
                )
            }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.Album,
                    title = "Colore dinamico",
                    subtitle = "Sfondo e accenti presi dalla copertina del brano",
                    checked = dynamicColor,
                    onCheckedChange = onDynamicColor
                )
            }
            item { SettingsSectionLabel("RIPRODUZIONE") }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.SkipNext,
                    title = "SponsorBlock",
                    subtitle = "Salta automaticamente sponsor e parti non musicali",
                    checked = sponsorBlock,
                    onCheckedChange = onSponsorBlock
                )
            }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.Bedtime,
                    title = "Salta i silenzi",
                    subtitle = "Comprimi le pause silenziose nei brani",
                    checked = skipSilence,
                    onCheckedChange = onSkipSilence
                )
            }
            item { SettingsSectionLabel("PREFERENZE") }
            item {
                SettingsButton(
                    icon = Icons.Rounded.Settings,
                    title = "Rifai il questionario gusti",
                    subtitle = "Riscegli i tuoi generi preferiti",
                    onClick = onRedoQuestionnaire
                )
            }
            item { SettingsSectionLabel("APP") }
            item {
                SettingsUpdateCard(
                    updateInfo = updateInfo,
                    isChecking = isCheckingUpdates,
                    onCheck = onCheckUpdates,
                    onDownload = onDownloadUpdate
                )
            }
            item {
                Text(
                    "LEVYRA ${BuildConfig.VERSION_NAME} • YouTube & YouTube Music engine",
                    color = LevyraMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(text, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun SettingsToggle(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(LevyraCyan.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = LevyraBlack,
                    checkedTrackColor = LevyraCyan,
                    uncheckedThumbColor = LevyraMuted,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                )
            )
        }
    }
}

@Composable
private fun SettingsButton(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(LevyraPink.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = LevyraPink, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SettingsUpdateCard(
    updateInfo: AppUpdateInfo?,
    isChecking: Boolean,
    onCheck: () -> Unit,
    onDownload: () -> Unit
) {
    val hasUpdate = updateInfo?.isNewer == true
    Surface(
        color = Color.Transparent,
        border = BorderStroke(1.dp, if (hasUpdate) LevyraCyan.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            if (hasUpdate) LevyraCyan.copy(alpha = 0.17f) else Color.White.copy(alpha = 0.055f),
                            Color.White.copy(alpha = 0.035f),
                            if (hasUpdate) LevyraViolet.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.04f)
                        )
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(LevyraCyan.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = LevyraCyan)
                    } else {
                        Icon(Icons.Rounded.Bolt, null, tint = LevyraCyan, modifier = Modifier.size(21.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = if (hasUpdate) "Aggiornamento disponibile" else "Aggiornamenti",
                        color = LevyraText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = when {
                            isChecking -> "Controllo ultima versione…"
                            hasUpdate -> "LEVYRA ${updateInfo?.latestVersionName.orEmpty()} pronta al download"
                            updateInfo != null -> "Installata la versione più recente"
                            else -> "Verifica nuove versioni pubblicate"
                        },
                        color = LevyraMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (hasUpdate && updateInfo != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(updateInfo.releaseTitle.ifBlank { "LEVYRA ${updateInfo.latestVersionName}" }, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = if (updateInfo.directApk) "APK firmato pronto da installare" else "Pagina release pronta da aprire",
                            color = LevyraMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsMiniButton(
                    label = if (isChecking) "Controllo" else "Check",
                    accent = LevyraCyan,
                    enabled = !isChecking,
                    modifier = Modifier.weight(1f),
                    onClick = onCheck
                )
                if (hasUpdate) {
                    SettingsMiniButton(
                        label = "Scarica",
                        accent = LevyraViolet,
                        enabled = !isChecking,
                        modifier = Modifier.weight(1f),
                        onClick = onDownload
                    )
                }
            }
            Text(
                text = "Versione installata: ${BuildConfig.VERSION_NAME}",
                color = LevyraMuted.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SettingsMiniButton(
    label: String,
    accent: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (enabled) accent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.045f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (enabled) accent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.07f)),
        modifier = modifier
            .height(42.dp)
            .pressable(onClick = { if (enabled) onClick() })
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (enabled) LevyraText else LevyraMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun GreetingBar(userName: String, isResolving: Boolean, onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painter = painterResource(id = R.drawable.levyra_logo),
                contentDescription = "Logo Levyra",
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    .padding(6.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = greeting(userName).uppercase(),
                    color = LevyraMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "LEVYRA",
                    style = TextStyle(
                        brush = Brush.linearGradient(listOf(LevyraCyan, LevyraViolet)),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isResolving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = LevyraCyan)
            }
            CircleIconButton(
                icon = Icons.Rounded.Settings,
                tint = LevyraText,
                background = Color.White.copy(alpha = 0.08f),
                onClick = onSettings
            )
        }
    }
}

@Composable
private fun MetroHeroDeck(
    tracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    isResolving: Boolean,
    favoritesCount: Int,
    queueCount: Int,
    onPrimary: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    val hero = currentTrack ?: tracks.firstOrNull() ?: return
    val accentStart = Color(hero.accentStart)
    val accentEnd = Color(hero.accentEnd)
    Surface(
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(30.dp),
        shadowElevation = 18.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            accentStart.copy(alpha = 0.58f),
                            Color(0xFF11172A),
                            accentEnd.copy(alpha = 0.44f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(150.dp)
                    .background(Color.White.copy(alpha = 0.06f), CircleShape)
            )
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(color = Color.Black.copy(alpha = 0.18f), shape = CircleShape) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Headphones, null, tint = LevyraCyan, modifier = Modifier.size(15.dp))
                            Text("DISCOVERY FLOW", color = LevyraText, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                        }
                    }
                    Surface(color = Color.White.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.pressable(onClick = onPlayAll)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = LevyraText, modifier = Modifier.size(15.dp))
                            Text("Play", color = LevyraText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.size(118.dp)) {
                        tracks.drop(1).take(2).forEachIndexed { index, track ->
                            CoverImage(
                                track = track,
                                modifier = Modifier
                                    .align(if (index == 0) Alignment.TopEnd else Alignment.BottomStart)
                                    .size(72.dp)
                                    .graphicsLayer {
                                        alpha = 0.58f
                                        rotationZ = if (index == 0) 8f else -8f
                                    }
                                    .clip(RoundedCornerShape(18.dp))
                            )
                        }
                        CoverImage(
                            track = hero,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(96.dp)
                                .border(2.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp)),
                            highRes = true
                        )
                        if (isPlaying || isResolving) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.54f),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(96.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isResolving) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = LevyraCyan)
                                    else Icon(Icons.Rounded.GraphicEq, null, tint = LevyraCyan, modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(hero.title, color = LevyraText, fontSize = 24.sp, lineHeight = 27.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(hero.artist, color = LevyraText.copy(alpha = 0.78f), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetroStatPill(Icons.Rounded.QueueMusic, queueCount.coerceAtLeast(tracks.size).toString(), "queue")
                            MetroStatPill(Icons.Rounded.Favorite, favoritesCount.toString(), "saved")
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetroActionButton(
                        icon = Icons.Rounded.PlayArrow,
                        text = if (currentTrack?.id == hero.id && isPlaying) "Apri player" else "Ascolta ora",
                        accent = LevyraCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { onPrimary(hero) }
                    )
                    MetroActionButton(
                        icon = Icons.Rounded.LibraryMusic,
                        text = "Libreria",
                        accent = LevyraPink,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenLibrary
                    )
                }
            }
        }
    }
}

@Composable
private fun MetroStatPill(icon: ImageVector, value: String, label: String) {
    Surface(color = Color.Black.copy(alpha = 0.18f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), shape = CircleShape) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, null, tint = LevyraCyan, modifier = Modifier.size(13.dp))
            Text(value, color = LevyraText, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(label, color = LevyraText.copy(alpha = 0.62f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MetroActionButton(icon: ImageVector, text: String, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.26f)),
        shape = RoundedCornerShape(17.dp),
        modifier = modifier
            .height(50.dp)
            .pressable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MetroDiscoveryRail(tracks: List<Track>, currentId: String?, onPlay: (Track) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeaderAction("Trend da aprire subito", onPlayAll = { tracks.firstOrNull()?.let(onPlay) })
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(tracks, key = { index, track -> "metro-discovery-$index-${track.id}" }) { index, track ->
                val isCurrent = track.id == currentId
                Surface(
                    color = if (isCurrent) LevyraCyan.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, if (isCurrent) LevyraCyan.copy(alpha = 0.48f) else Color.White.copy(alpha = 0.09f)),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .width(210.dp)
                        .pressable(onClick = { onPlay(track) })
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box {
                            CoverImage(track, Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)))
                            Surface(
                                color = Color.Black.copy(alpha = 0.48f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${index + 1}", color = LevyraText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(track.title, color = if (isCurrent) LevyraCyan else LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingArtwork(track: Track, isPlaying: Boolean, isResolving: Boolean, modifier: Modifier = Modifier) {
    val accentStart = Color(track.accentStart)
    val accentEnd = Color(track.accentEnd)

    val scale by animateFloatAsState(
        targetValue = if (isPlaying && !isResolving) 1.05f else 1.0f,
        label = "BreathingShadow"
    )

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = 0.8f
                }
                .background(
                    Brush.radialGradient(listOf(accentStart.copy(alpha = 0.8f), accentEnd.copy(alpha = 0.4f), Color.Transparent)),
                    CircleShape
                )
        )

        CoverImage(
            track = track,
            modifier = Modifier
                .fillMaxSize(0.9f)
                .clip(RoundedCornerShape(38.dp))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(38.dp)),
            highRes = true
        )
    }
}

@Composable
private fun QuickStart(
    track: Track?,
    isPlaying: Boolean,
    isResolving: Boolean,
    progress: Float,
    hasSuggestions: Boolean,
    onResume: () -> Unit,
    onShuffle: () -> Unit,
    onOpenFavorites: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (track != null) {
            Surface(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .pressable(onClick = onResume)
            ) {
                Box {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CoverImage(track, Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Continua ad ascoltare", color = LevyraCyan, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Text(track.title, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (isResolving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 8.dp), strokeWidth = 2.dp, color = LevyraCyan)
                        } else if (isPlaying) {
                            Icon(Icons.Rounded.Equalizer, null, tint = LevyraCyan, modifier = Modifier.size(20.dp).padding(end = 8.dp))
                        } else {
                            Icon(Icons.Rounded.PlayArrow, null, tint = LevyraText, modifier = Modifier.size(24.dp).padding(end = 8.dp))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(2.dp)
                            .background(LevyraCyan)
                            .align(Alignment.BottomStart)
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            QuickAction(
                icon = Icons.Rounded.Shuffle,
                label = "Mix per te",
                accent = LevyraCyan,
                enabled = hasSuggestions,
                modifier = Modifier.weight(1f),
                onClick = onShuffle
            )
            QuickAction(
                icon = Icons.Rounded.Favorite,
                label = "Preferiti",
                accent = LevyraPink,
                enabled = true,
                modifier = Modifier.weight(1f),
                onClick = onOpenFavorites
            )
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, accent: Color, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        color = Color.White.copy(alpha = if (enabled) 0.07f else 0.03f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(56.dp)
            .pressable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(accent.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp))
            }
            Text(label, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SearchDock(query: String, isSearching: Boolean, onQuery: (String) -> Unit, onSearch: () -> Unit, onFocus: () -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onFocus)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.Search, null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
                Text(
                    text = if (query.isEmpty()) "Cerca brani, artisti..." else query,
                    color = if (query.isEmpty()) LevyraMuted else LevyraText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Surface(
                color = Color.White.copy(alpha = 0.06f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "⌘K",
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun MoodRow(moods: List<Mood>, selectedId: String?, onSelect: (Mood) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        moods.forEach { mood ->
            val selected = mood.id == selectedId
            Surface(
                color = if (selected) Color(mood.accentStart).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f),
                border = BorderStroke(1.dp, if (selected) Color(mood.accentStart).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.09f)),
                shape = CircleShape,
                modifier = Modifier.pressable(onClick = { onSelect(mood) })
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(mood.icon, fontSize = 16.sp)
                    Text(mood.title, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderAction(title: String, onPlayAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            color = LevyraText,
            fontSize = 26.sp,
            letterSpacing = (-0.5).sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Surface(
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            shape = CircleShape,
            modifier = Modifier.pressable(onClick = onPlayAll)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = LevyraCyan, modifier = Modifier.size(16.dp))
                Text("Play All", color = LevyraText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AlbumCardRow(tracks: List<Track>, currentId: String?, animationsEnabled: Boolean, onPlay: (Track) -> Unit) {
    if (tracks.isEmpty()) return
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(tracks, key = { index, track -> "album-card-$index-${track.id}" }) { index, track ->
            val isCurrent = track.id == currentId
            var isPressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isPressed && animationsEnabled) 0.95f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "scale"
            )
            val modifier = if (animationsEnabled) {
                Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            } else Modifier
            Column(
                modifier = modifier
                    .width(164.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onTap = { onPlay(track) }
                        )
                    },
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.02f),
                    border = BorderStroke(1.dp, if (isCurrent) LevyraCyan.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = if (animationsEnabled) 12.dp else 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(Brush.linearGradient(listOf(Color(track.accentStart).copy(alpha = 0.24f), Color(track.accentEnd).copy(alpha = 0.18f))))
                    ) {
                        CoverImage(
                            track = track,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(22.dp)),
                            highRes = true
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                        )
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(if (isCurrent) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow, null, tint = LevyraCyan, modifier = Modifier.size(14.dp))
                                Text(if (isCurrent) "ON" else "${index + 1}", color = LevyraText, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(track.title, color = if (isCurrent) LevyraCyan else LevyraText, fontSize = 14.sp, lineHeight = 16.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val kind = if (track.album.isNotBlank() && track.album != track.title && track.album != "YouTube Music") "Album" else "Single"
                    Text("$kind • ${track.artist}", color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun RowCarousel(
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    favoriteIds: Set<String>,
    onPlay: (Track) -> Unit,
    onFavorite: (Track) -> Unit
) {
    if (tracks.isEmpty()) return
    val perPage = 4
    val pages = (tracks.size + perPage - 1) / perPage
    val pagerState = rememberPagerState(pageCount = { pages })
    HorizontalPager(
        state = pagerState,
        pageSpacing = 16.dp,
        contentPadding = PaddingValues(end = if (pages > 1) 48.dp else 0.dp)
    ) { page ->
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            tracks.drop(page * perPage).take(perPage).forEach { track ->
                CompactRow(
                    track = track,
                    isCurrent = track.id == currentId,
                    isPlaying = isPlaying && track.id == currentId,
                    isResolving = isResolving && track.id == currentId,
                    isFavorite = track.id in favoriteIds,
                    onClick = { onPlay(track) },
                    onFavorite = { onFavorite(track) }
                )
            }
        }
    }
}

@Composable
private fun CompactRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box {
            CoverImage(track, Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)))
            if (isPlaying || isResolving) {
                Surface(color = Color.Black.copy(alpha = 0.45f), shape = RoundedCornerShape(10.dp), modifier = Modifier.matchParentSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isResolving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LevyraCyan)
                        else Icon(Icons.Rounded.Equalizer, null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                track.title,
                color = if (isCurrent) LevyraCyan else LevyraText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(track.artist, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Preferito",
                tint = if (isFavorite) LevyraPink else LevyraMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ChartRow(
    rank: Int,
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit
) {
    Surface(
        color = if (isCurrent) LevyraCyan.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if (isCurrent) LevyraCyan.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.07f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.width(26.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = rank.toString(),
                    color = if (rank <= 3) LevyraCyan else LevyraMuted,
                    fontSize = if (rank <= 3) 20.sp else 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Box {
                CoverImage(track, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
                if (isPlaying || isResolving) {
                    Surface(color = Color.Black.copy(alpha = 0.45f), shape = RoundedCornerShape(12.dp), modifier = Modifier.matchParentSize()) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isResolving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LevyraCyan)
                            else Icon(Icons.Rounded.Equalizer, null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(track.title, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Preferito",
                    tint = if (isFavorite) LevyraPink else LevyraMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FeaturedTrackCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = LevyraPanelSoft,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box {
                CoverImage(track, Modifier.size(110.dp).clip(CircleShape))
                if (isPlaying || isResolving) {
                    Surface(color = Color.Black.copy(alpha = 0.5f), shape = CircleShape, modifier = Modifier.matchParentSize()) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isResolving) CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp, color = LevyraCyan)
                            else Icon(Icons.Rounded.Equalizer, null, tint = LevyraCyan, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(track.title, color = LevyraText, fontSize = 24.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = LevyraMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(track.source, color = LevyraText, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun SearchResultsHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                LevyraCyan.copy(alpha = 0.28f),
                                LevyraViolet.copy(alpha = 0.22f),
                                LevyraPink.copy(alpha = 0.18f)
                            )
                        ),
                        CircleShape
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.13f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MusicNote, null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
            }
            Text(
                text = "Potrebbe piacerti anche",
                color = LevyraText,
                fontSize = 23.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            LevyraCyan.copy(alpha = 0.95f),
                            LevyraViolet.copy(alpha = 0.72f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(99.dp)
                )
        )
    }
}

@Composable
private fun SearchSuggestionTrackCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(track.accentStart).copy(alpha = if (isCurrent) 0.24f else 0.12f),
                        Color(track.accentEnd).copy(alpha = if (isCurrent) 0.16f else 0.07f),
                        Color.White.copy(alpha = 0.035f)
                    )
                ),
                RoundedCornerShape(22.dp)
            )
            .border(
                1.dp,
                if (isCurrent) LevyraCyan.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.075f),
                RoundedCornerShape(22.dp)
            )
            .pressable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(48.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(track.accentStart).copy(alpha = 0.95f),
                                Color(track.accentEnd).copy(alpha = 0.78f)
                            )
                        ),
                        RoundedCornerShape(99.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (isCurrent) LevyraCyan.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.065f),
                        CircleShape
                    )
                    .border(
                        1.dp,
                        if (isCurrent) LevyraCyan.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.08f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isResolving -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LevyraCyan)
                    isPlaying -> Icon(Icons.Rounded.GraphicEq, null, tint = LevyraCyan, modifier = Modifier.size(22.dp))
                    else -> Icon(Icons.Rounded.PlayArrow, null, tint = if (isCurrent) LevyraCyan else LevyraText, modifier = Modifier.size(24.dp))
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = track.title,
                    color = if (isCurrent) LevyraCyan else LevyraText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = LevyraMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onFavorite, modifier = Modifier.size(38.dp)) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Preferito",
                    tint = if (isFavorite) LevyraPink else LevyraMuted,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchFilterChips(
    selected: SearchFilter,
    hasArtists: Boolean,
    hasAlbums: Boolean,
    onSelect: (SearchFilter) -> Unit
) {
    val chips = buildList {
        add(SearchFilter.All to "Tutti")
        add(SearchFilter.Songs to "Brani")
        if (hasArtists) add(SearchFilter.Artists to "Artisti")
        if (hasAlbums) add(SearchFilter.Albums to "Album")
    }
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { (filter, label) ->
            val active = filter == selected
            Surface(
                color = if (active) LevyraText else Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(99.dp),
                border = if (active) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.clickable { onSelect(filter) }
            ) {
                Text(
                    text = label,
                    color = if (active) LevyraBlack else LevyraText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TopResultCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onArtist: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Risultato principale", color = LevyraCyan, fontSize = 13.sp, fontWeight = FontWeight.Black)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(track.accentStart).copy(alpha = 0.30f),
                            Color(track.accentEnd).copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                .pressable(onClick = onPlay)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    CoverImage(track, Modifier.size(76.dp).clip(RoundedCornerShape(14.dp)), highRes = true)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(track.title, color = LevyraText, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            track.artist,
                            color = LevyraMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { onArtist() }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = LevyraText,
                        shape = RoundedCornerShape(99.dp),
                        modifier = Modifier.weight(1f).pressable(onClick = onPlay)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isResolving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LevyraBlack)
                            else Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = LevyraBlack, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isPlaying) "In riproduzione" else "Riproduci", color = LevyraBlack, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Surface(color = Color.White.copy(alpha = 0.08f), shape = CircleShape, modifier = Modifier.size(46.dp).clickable { onFavorite() }) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                null,
                                tint = if (isFavorite) LevyraPink else LevyraText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTrackCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    isFavorite: Boolean,
    isDownloading: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onArtist: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box {
            CoverImage(track, Modifier.size(52.dp).clip(RoundedCornerShape(9.dp)))
            if (isPlaying || isResolving) {
                Surface(color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(9.dp), modifier = Modifier.matchParentSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isResolving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LevyraCyan)
                        else Icon(Icons.Rounded.Equalizer, null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(track.title, color = if (isCurrent) LevyraCyan else LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                track.artist,
                color = LevyraMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onArtist() }
            )
        }
        DownloadButton(isDownloading = isDownloading, isDownloaded = isDownloaded, onDownload = onDownload)
        IconButton(onClick = onFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Preferito",
                tint = if (isFavorite) LevyraPink else LevyraMuted
            )
        }
    }
}

@Composable
private fun ArtistHitRow(artists: List<ArtistHit>, onClick: (ArtistHit) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(artists, key = { "artist-hit-${it.name}" }) { hit ->
            Column(
                modifier = Modifier.width(104.dp).clickable { onClick(hit) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(hit.accentStart), Color(hit.accentEnd)))),
                    contentAlignment = Alignment.Center
                ) {
                    if (hit.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(hit.thumbnailUrl).crossfade(true).build(),
                            contentDescription = hit.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize().clip(CircleShape)
                        )
                    } else {
                        Icon(Icons.Rounded.Person, null, tint = LevyraText, modifier = Modifier.size(40.dp))
                    }
                }
                Text(hit.name, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Artista", color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AlbumHitRow(albums: List<AlbumHit>, onClick: (AlbumHit) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(albums, key = { "album-hit-${it.title}-${it.artist}" }) { album ->
            Column(
                modifier = Modifier.width(150.dp).clickable { onClick(album) },
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(LevyraPanelSoft),
                    contentAlignment = Alignment.Center
                ) {
                    if (album.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(album.thumbnailUrl).crossfade(true).build(),
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Icon(Icons.Rounded.Album, null, tint = LevyraMuted, modifier = Modifier.size(40.dp))
                    }
                }
                Text(album.title, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(album.year, album.artist).filter { it.isNotBlank() }.joinToString(" · "),
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    isDownloading: Boolean = false,
    isDownloaded: Boolean = false,
    onDownload: (() -> Unit)? = null,
    onArtist: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box {
            CoverImage(track, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
            if (isPlaying || isResolving) {
                Surface(color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp), modifier = Modifier.matchParentSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isResolving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = LevyraCyan)
                        else Icon(Icons.Rounded.Equalizer, null, tint = LevyraCyan, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(track.title, color = if (isCurrent) LevyraCyan else LevyraText, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    track.artist,
                    color = LevyraMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (onArtist != null) Modifier.clickable { onArtist() } else Modifier
                )
            }
        }
        if (onDownload != null) {
            DownloadButton(isDownloading = isDownloading, isDownloaded = isDownloaded, onDownload = onDownload)
        }
        IconButton(onClick = onFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Preferito",
                tint = if (isFavorite) LevyraPink else LevyraMuted
            )
        }
        if (onAddToPlaylist != null || onRemove != null) {
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Altro", tint = LevyraMuted)
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    modifier = Modifier.background(LevyraPanel)
                ) {
                    if (onAddToPlaylist != null) {
                        DropdownMenuItem(
                            text = { Text("Aggiungi a playlist", color = LevyraText) },
                            leadingIcon = { Icon(Icons.Rounded.Add, null, tint = LevyraCyan) },
                            onClick = { menuOpen = false; onAddToPlaylist() }
                        )
                    }
                    if (onRemove != null) {
                        DropdownMenuItem(
                            text = { Text("Rimuovi dalla playlist", color = LevyraText) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = LevyraMuted) },
                            onClick = { menuOpen = false; onRemove() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadButton(isDownloading: Boolean, isDownloaded: Boolean, onDownload: () -> Unit) {
    IconButton(onClick = { if (!isDownloading && !isDownloaded) onDownload() }) {
        when {
            isDownloading -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = LevyraCyan)
            isDownloaded -> Icon(Icons.Rounded.DownloadDone, contentDescription = "Scaricato", tint = LevyraCyan, modifier = Modifier.size(23.dp))
            else -> Icon(Icons.Rounded.Download, contentDescription = "Scarica", tint = LevyraMuted, modifier = Modifier.size(23.dp))
        }
    }
}

@Composable
private fun MiniPlayer(track: Track, isPlaying: Boolean, isResolving: Boolean, progress: Float, onOpen: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(Color(0xFF121214).copy(alpha = 0.9f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(percent = 50))
            .pressable(onClick = onOpen)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Brush.horizontalGradient(listOf(Color(track.accentStart).copy(alpha = 0.15f), Color.Transparent, Color(track.accentEnd).copy(alpha = 0.15f))))
        )
        Column {
            Row(
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box {
                    CoverImage(track, Modifier.size(48.dp).clip(CircleShape))
                    if (isPlaying || isResolving) {
                        Surface(color = Color.Black.copy(alpha = 0.5f), shape = CircleShape, modifier = Modifier.matchParentSize()) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isResolving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LevyraCyan)
                                else Icon(Icons.Rounded.GraphicEq, null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(track.title, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onToggle, modifier = Modifier.size(38.dp)) {
                    if (isResolving) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = LevyraCyan)
                    else Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = LevyraCyan, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.SkipNext, null, tint = LevyraText, modifier = Modifier.size(26.dp))
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0.01f, 1f))
                    .height(3.dp)
                    .background(Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet)))
            )
        }
    }
}

@Composable
private fun GradientPlayButton(isPlaying: Boolean, isResolving: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .background(Brush.linearGradient(listOf(LevyraCyan, LevyraViolet)), CircleShape)
            .pressable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isResolving) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = LevyraBlack)
        else Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = LevyraBlack, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun GradientButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Brush.linearGradient(listOf(LevyraCyan, LevyraViolet)), RoundedCornerShape(18.dp))
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = LevyraBlack, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun CircleIconButton(icon: ImageVector, tint: Color, background: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(background, CircleShape)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun BottomTabs(selected: LevyraTab, onSelect: (LevyraTab) -> Unit) {
    Surface(
        color = Color(0xFF121214).copy(alpha = 0.85f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(percent = 50),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TabButton(Icons.Rounded.Home, "Home", selected == LevyraTab.Home) { onSelect(LevyraTab.Home) }
            TabButton(Icons.Rounded.Search, "Cerca", selected == LevyraTab.Search) { onSelect(LevyraTab.Search) }
            TabButton(Icons.Rounded.LibraryMusic, "Libreria", selected == LevyraTab.Library) { onSelect(LevyraTab.Library) }
            TabButton(Icons.Rounded.Album, "Player", selected == LevyraTab.Player) { onSelect(LevyraTab.Player) }
        }
    }
}

@Composable
private fun RowScope.TabButton(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val selectedScale by animateFloatAsState(
        targetValue = if (selected && LocalAnimationsEnabled.current) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tab-selection-scale"
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .pressable(onClick = onClick)
            .padding(vertical = 10.dp)
            .graphicsLayer {
                scaleX = selectedScale
                scaleY = selectedScale
                alpha = if (selected) 1f else 0.65f
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) LevyraCyan else LevyraMuted,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                color = if (selected) LevyraCyan else LevyraMuted,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, color = LevyraText, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
        Text(subtitle, color = LevyraMuted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = LevyraText, fontSize = 19.sp, fontWeight = FontWeight.Black)
}

@Composable
private fun QuickChips(onClick: (String) -> Unit) {
    val chips = listOf("Sfera Ebbasta", "Lazza", "Geolier", "The Weeknd", "Drake", "top hits italia", "night drive", "gym bass")
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        chips.forEach { chip ->
            Surface(
                color = Color.White.copy(alpha = 0.07f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                shape = CircleShape,
                modifier = Modifier.pressable(onClick = { onClick(chip) })
            ) {
                Text(chip, color = LevyraText, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp))
            }
        }
    }
}

@Composable
private fun SearchSummary(state: LevyraUiState) {
    when {
        state.isSearching -> GlassMessage("Sto cercando su YouTube Music…", LevyraCyan)
        state.searchError != null -> GlassMessage(state.searchError, LevyraOrange)
        state.searchResults.isNotEmpty() -> GlassMessage("${state.searchResults.size} risultati", LevyraCyan)
        else -> GlassMessage("Scrivi il nome di un brano e cerca", LevyraMuted)
    }
}

@Composable
private fun StatusBlock(state: LevyraUiState) {
    if (state.searchError != null || state.playerError != null) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.searchError?.let { GlassMessage(it, LevyraOrange) }
            state.playerError?.let { GlassMessage(it, LevyraOrange) }
        }
    }
}

@Composable
private fun PlayerError(error: String?) {
    if (error != null) GlassMessage(error, LevyraOrange)
}

@Composable
private fun GlassMessage(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(14.dp))
    }
}

@Composable
private fun CoverImage(track: Track, modifier: Modifier, highRes: Boolean = false) {
    val raw = if (highRes) track.largeThumbnailUrl.ifBlank { track.thumbnailUrl } else track.thumbnailUrl.ifBlank { track.largeThumbnailUrl }
    if (raw.isBlank()) {
        EmptyCover(modifier)
    } else {

        val model = if (highRes) raw else smallThumb(raw)
        val crossfadeMs = if (LocalAnimationsEnabled.current && highRes) 200 else 0
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(model)
                .crossfade(crossfadeMs)
                .build(),
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = modifier.background(Brush.linearGradient(listOf(Color(track.accentStart), Color(track.accentEnd))))
        )
    }
}

private fun smallThumb(url: String): String {
    var u = url
    u = u.replace(Regex("=w\\d+-h\\d+[^=]*$"), "=w160-h160-l90-rj")
    u = u.replace(Regex("=s\\d+[^=]*$"), "=s160")
    u = u.replace(Regex("\\d+x\\d+bb"), "160x160bb")
    return u
}

@Composable
private fun EmptyCover(modifier: Modifier) {
    Box(
        modifier = modifier.background(Brush.linearGradient(listOf(LevyraCyan.copy(alpha = 0.72f), LevyraViolet.copy(alpha = 0.72f)))),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.MusicNote, null, tint = LevyraBlack, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun EmptyState(text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🎵", fontSize = 28.sp)
            Text(text, color = LevyraMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    if (!LocalAnimationsEnabled.current) {
        return this.clickable(enabled = enabled, onClick = onClick)
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "press")
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
}

private fun greeting(userName: String): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val name = if (userName.isNotBlank()) " $userName" else ""
    return when (hour) {
        in 5..11 -> "Buongiorno$name ☀️"
        in 12..17 -> "Buon pomeriggio$name 🎶"
        in 18..22 -> "Buonasera$name 🌙"
        else -> "Buonanotte$name 🌌"
    }
}

private fun progressOf(positionMs: Long, durationMs: Long): Float {
    if (durationMs <= 0L) return 0f
    return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "--:--"
    val totalSeconds = ms / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private val LevyraUiState.offlineExportMessageCompat: String?
    get() = null

private val LevyraUiState.isOfflineExportingCompat: Boolean
    get() = false

private val LevyraUiState.embeddedMetadataWriterReadyCompat: Boolean
    get() = false

private val LevyraUiState.offlineExportMessage: String?
    get() = null

private val LevyraUiState.isOfflineExporting: Boolean
    get() = false

private val LevyraUiState.embeddedMetadataWriterReady: Boolean
    get() = false

private fun LevyraViewModel.clearOfflineExportMessage() = Unit

private fun LevyraViewModel.addToQueue(track: Track) {
    play(track)
}

private fun LevyraViewModel.exportTrack(track: Track) {
    play(track)
}

private fun LevyraViewModel.exportCurrentTrack() {
    selectTab(LevyraTab.Player)
}

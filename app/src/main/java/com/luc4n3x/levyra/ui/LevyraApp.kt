package com.luc4n3x.levyra.ui

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.Mood
import com.luc4n3x.levyra.domain.Taste
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.theme.LevyraBlack
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
    BackHandler(enabled = state.showQueue || state.showLyrics || state.showSettings || state.selectedTab != LevyraTab.Home) {
        if (!viewModel.navigateBack()) {
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
                    onAnimations = viewModel::setAnimationsEnabled,
                    onDynamicColor = viewModel::setDynamicColor,
                    onSponsorBlock = viewModel::setSponsorBlock,
                    onSkipSilence = viewModel::setSkipSilence,
                    onRedoQuestionnaire = viewModel::restartOnboarding,
                    onClose = viewModel::closeSettings
                )
            }

            AnimatedVisibility(visible = state.showLyrics, enter = overlayEnter, exit = overlayExit) {
                LyricsOverlay(state = state, onClose = viewModel::closeLyrics)
            }

            AnimatedVisibility(visible = state.showQueue, enter = overlayEnter, exit = overlayExit) {
                QueueOverlay(
                    state = state,
                    onPlay = { viewModel.playFrom(state.queue, it) },
                    onClose = viewModel::closeQueue
                )
            }
        }
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

/* ----------------------------------------------------------------------------------- */
/* Background                                                                          */
/* ----------------------------------------------------------------------------------- */

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

/* ----------------------------------------------------------------------------------- */
/* Home                                                                                */
/* ----------------------------------------------------------------------------------- */

@Composable
private fun HomeScreen(viewModel: LevyraViewModel, state: LevyraUiState) {
    val heroUpdate = remember(
        state.currentTrack,
        state.tracks,
        state.homeSections,
        state.charts,
        state.favorites
    ) {
        pickHeroUpdate(state)
    }
    val heroTrack = heroUpdate?.track
    val quickTracks = remember(
        state.currentTrack,
        state.tracks,
        state.homeSections,
        state.charts,
        state.favorites
    ) {
        buildQuickPickTracks(state, heroTrack)
    }
    val feedSections = remember(state.homeSections) {
        state.homeSections
            .filterNot { isQuickPicksSectionTitle(it.title) }
            .take(2)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = if (state.currentTrack != null) 188.dp else 100.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                GreetingBar(
                    state.userName,
                    state.isResolving,
                    onSettings = viewModel::openSettings
                )
                MoodRow(
                    moods = state.moods,
                    selectedId = state.selectedMood?.id,
                    onSelect = viewModel::selectMood
                )
            }
        }
        if (heroUpdate != null) {
            val heroTrack = heroUpdate.track
            item {
                HomeDiscoveryHero(
                    update = heroUpdate,
                    isFavorite = heroTrack.id in state.favoriteIds,
                    onPlay = {
                        val sourceList = when {
                            state.tracks.any { it.id == heroTrack.id } -> state.tracks
                            state.charts.any { it.id == heroTrack.id } -> state.charts
                            state.favorites.any { it.id == heroTrack.id } -> state.favorites
                            else -> listOf(heroTrack)
                        }
                        viewModel.playFrom(sourceList, heroTrack)
                    },
                    onSave = { viewModel.toggleFavorite(heroTrack) }
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
                QuickSectionHeader(
                    title = "Scelte rapide",
                    actionLabel = "Riproduci",
                    onAction = { viewModel.playAll(quickTracks) }
                )
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
                    onOffline = viewModel::exportTrack
                )
            }
        }
        feedSections.forEachIndexed { index, section ->
            if (section.tracks.isNotEmpty()) {
                item(key = "sec-h-$index") {
                    SectionHeaderAction(
                        section.title,
                        onPlayAll = { viewModel.playAll(section.tracks) }
                    )
                }
                item(key = "sec-r-$index") {
                    AlbumCardRow(
                        tracks = section.tracks,
                        currentId = state.currentTrack?.id,
                        onPlay = { viewModel.playFrom(section.tracks, it) }
                    )
                }
            }
        }
        item {
            val region = state.chartRegions.firstOrNull { it.id == state.selectedChartId }
            SectionHeaderAction(
                "📈 Classifica ${region?.label ?: "Italia"} ${region?.emoji ?: ""}",
                onPlayAll = { viewModel.playAll(state.charts) }
            )
        }
        item {
            ChartRegionRow(
                regions = state.chartRegions,
                selectedId = state.selectedChartId,
                loading = state.isLoadingCharts,
                onSelect = viewModel::selectChart
            )
        }
        if (state.charts.isEmpty()) {
            item {
                if (state.isLoadingCharts) {
                    GlassMessage("Carico la classifica…", LevyraCyan)
                } else {
                    GlassMessage("Classifica non disponibile, riprova tra poco", LevyraOrange)
                }
            }
        }
        if (state.charts.isNotEmpty()) {
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val chunks = state.charts.chunked(4)
                    itemsIndexed(chunks) { chunkIndex, chunk ->
                        Column(
                            modifier = Modifier.width(320.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
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
        .distinctBy { it.id }
        .filterNot { it.id in excluded }
        .take(4)
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
    onOffline: (Track) -> Unit
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
                onOffline = { onOffline(track) }
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
    onOffline: () -> Unit
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
                overflow = TextOverflow.Ellipsis
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
            onOffline = onOffline
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
    onOffline: () -> Unit
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

/* ----------------------------------------------------------------------------------- */
/* Search                                                                              */
/* ----------------------------------------------------------------------------------- */

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
                        title = "Potrebbe piacerti anche",
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
                        title = "",
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
                state.searchResults.isNotEmpty() -> {
                    item {
                        Text("Miglior Risultato", color = LevyraText, fontSize = 19.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    }
                    item {
                        val topTrack = state.searchResults.first()
                        FeaturedTrackCard(
                            track = topTrack,
                            isCurrent = topTrack.id == state.currentTrack?.id,
                            isPlaying = state.isPlaying && topTrack.id == state.currentTrack?.id,
                            isResolving = state.isResolving && topTrack.id == state.currentTrack?.id,
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                viewModel.playFrom(state.searchResults, topTrack)
                            }
                        )
                    }
                    if (state.searchResults.size > 1) {
                        item {
                            Text("Brani", color = LevyraText, fontSize = 19.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                        }
                        items(state.searchResults.drop(1), key = { it.id }) { track ->
                            TrackRow(
                                track = track,
                                isCurrent = track.id == state.currentTrack?.id,
                                isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                                isResolving = state.isResolving && track.id == state.currentTrack?.id,
                                isFavorite = track.id in state.favoriteIds,
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    viewModel.playFrom(state.searchResults, track)
                                },
                                onFavorite = { viewModel.toggleFavorite(track) }
                            )
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
            onClick = { /* Decorative or voice trigger */ },
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
            onClick = { /* Decorative or visualizer trigger */ },
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                color = LevyraText,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            suggestions.forEach { suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = LevyraMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = suggestion,
                        color = LevyraText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Completa",
                        tint = LevyraMuted,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = 45f }
                    )
                }
            }
        }
    }
}

/* ----------------------------------------------------------------------------------- */
/* Library                                                                             */
/* ----------------------------------------------------------------------------------- */

@Composable
private fun LibraryScreen(viewModel: LevyraViewModel, state: LevyraUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (state.currentTrack != null) 188.dp else 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { PageHeader("Libreria", "I tuoi preferiti e la cronologia") }
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
                    onFavorite = { viewModel.toggleFavorite(track) }
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
                onFavorite = { viewModel.toggleFavorite(track) }
            )
        }
    }
}

/* ----------------------------------------------------------------------------------- */
/* Player                                                                              */
/* ----------------------------------------------------------------------------------- */

@Composable
private fun PlayerScreen(viewModel: LevyraViewModel, state: LevyraUiState) {
    val track = state.currentTrack
    val bgStart = track?.let { Color(it.accentStart) } ?: LevyraCyan
    val bgEnd = track?.let { Color(it.accentEnd) } ?: LevyraViolet
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgStart.copy(alpha = 0.45f), LevyraBlack.copy(alpha = 0.8f), LevyraBlack)))
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
                FloatingArtwork(
                    track = track,
                    isPlaying = state.isPlaying,
                    isResolving = state.isResolving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 16.dp)
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            color = LevyraText,
                            fontSize = 32.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = track.artist,
                            color = LevyraMuted,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                    onSpeed = viewModel::cycleSpeed,
                    onSleep = viewModel::cycleSleepTimer,
                    onExport = viewModel::exportCurrentTrack,
                    onShare = track
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
                .size(82.dp)
                .background(Brush.linearGradient(listOf(LevyraCyan, LevyraViolet)), CircleShape)
                .pressable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (isResolving) CircularProgressIndicator(modifier = Modifier.size(34.dp), strokeWidth = 3.dp, color = LevyraBlack)
            else Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = LevyraBlack, modifier = Modifier.size(44.dp))
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
    onSpeed: () -> Unit,
    onSleep: () -> Unit,
    onExport: () -> Unit,
    onShare: Track
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
        ShareOptionChip(track = onShare, modifier = Modifier.weight(1f))
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
private fun ShareOptionChip(track: Track, modifier: Modifier) {
    val context = LocalContext.current
    Surface(
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(48.dp)
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Share, null, tint = LevyraText, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(7.dp))
            Text("Condividi", color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}

private fun trimSpeed(speed: Float): String {
    return if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0').trimEnd('.')
}

/* ----------------------------------------------------------------------------------- */
/* Onboarding                                                                          */
/* ----------------------------------------------------------------------------------- */

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
            // Consume taps so controls behind the overlay are not reachable.
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

/* ----------------------------------------------------------------------------------- */
/* Settings                                                                            */
/* ----------------------------------------------------------------------------------- */

@Composable
private fun SettingsOverlay(
    animationsEnabled: Boolean,
    dynamicColor: Boolean,
    sponsorBlock: Boolean,
    skipSilence: Boolean,
    onAnimations: (Boolean) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onSponsorBlock: (Boolean) -> Unit,
    onSkipSilence: (Boolean) -> Unit,
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
            item {
                Text(
                    "LEVYRA • YouTube & YouTube Music engine",
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

/* ----------------------------------------------------------------------------------- */
/* Reusable pieces                                                                     */
/* ----------------------------------------------------------------------------------- */

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
    
    // Breathing animation for the shadow
    val scale by animateFloatAsState(
        targetValue = if (isPlaying && !isResolving) 1.05f else 1.0f,
        label = "BreathingShadow"
    )
    
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        // Glowing animated shadow behind the artwork
        Box(
            modifier = Modifier
                .fillMaxSize(0.9f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = 0.6f
                }
                .background(
                    Brush.radialGradient(listOf(accentStart, accentEnd.copy(alpha = 0.5f), Color.Transparent)),
                    CircleShape
                )
        )
        // Actual Artwork
        CoverImage(
            track = track,
            modifier = Modifier
                .fillMaxSize(0.88f)
                .clip(RoundedCornerShape(32.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp)),
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
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
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
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = LevyraCyan, modifier = Modifier.size(14.dp))
                Text("Riproduci", color = LevyraText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Horizontal row of square cover cards with title/subtitle, our take on the YT Music shelves. */
@Composable
private fun AlbumCardRow(tracks: List<Track>, currentId: String?, onPlay: (Track) -> Unit) {
    if (tracks.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        itemsIndexed(tracks, key = { index, track -> "album-card-$index-${track.id}" }) { index, track ->
            val isCurrent = track.id == currentId
            Column(
                modifier = Modifier
                    .width(158.dp)
                    .pressable(onClick = { onPlay(track) }),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Surface(
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, if (isCurrent) LevyraCyan.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 0.dp
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
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))))
                        )
                        Surface(
                            color = Color.Black.copy(alpha = 0.42f),
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
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
                Text(track.title, color = if (isCurrent) LevyraCyan else LevyraText, fontSize = 13.sp, lineHeight = 15.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}



/** Horizontally paged stacked track rows (YouTube Music "Brani di tendenza" style). */
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
private fun TrackRow(
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
            .pressable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box {
            CoverImage(track, Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)))
            if (isPlaying || isResolving) {
                Surface(color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp), modifier = Modifier.matchParentSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isResolving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = LevyraCyan)
                        else Icon(Icons.Rounded.Equalizer, null, tint = LevyraCyan, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(track.title, color = if (isCurrent) LevyraCyan else LevyraText, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(track.artist, color = LevyraMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
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
private fun MiniPlayer(track: Track, isPlaying: Boolean, isResolving: Boolean, progress: Float, onOpen: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .background(Brush.linearGradient(listOf(Color(track.accentStart).copy(alpha = 0.5f), Color(track.accentEnd).copy(alpha = 0.35f))), RoundedCornerShape(24.dp))
            .padding(1.dp)
    ) {
        Surface(
            color = Color(0xF20E101A),
            shape = RoundedCornerShape(23.dp),
            modifier = Modifier
                .fillMaxWidth()
                .pressable(onClick = onOpen)
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box {
                        CoverImage(track, Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)))
                        if (isPlaying || isResolving) {
                            Surface(color = Color.Black.copy(alpha = 0.48f), shape = RoundedCornerShape(13.dp), modifier = Modifier.matchParentSize()) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isResolving) CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp, color = LevyraCyan)
                                    else Icon(Icons.Rounded.GraphicEq, null, tint = LevyraCyan, modifier = Modifier.size(19.dp))
                                }
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(track.title, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
                        if (isResolving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = LevyraCyan)
                        else Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = LevyraCyan, modifier = Modifier.size(26.dp))
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.SkipNext, null, tint = LevyraText, modifier = Modifier.size(23.dp))
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.Close, null, tint = LevyraMuted, modifier = Modifier.size(21.dp))
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0.01f, 1f))
                        .height(3.dp)
                        .background(Brush.horizontalGradient(listOf(LevyraCyan, LevyraPink)))
                )
            }
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
        color = Color(0xDF07090D),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
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
        targetValue = if (selected && LocalAnimationsEnabled.current) 1.07f else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "tab-selection-scale"
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .pressable(onClick = onClick)
            .padding(vertical = 8.dp)
            .graphicsLayer {
                scaleX = selectedScale
                scaleY = selectedScale
                alpha = if (selected) 1f else 0.82f
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
        // Lists request a tiny image (fast to download); only the player loads full quality.
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

/** Rewrites known cover URLs (YouTube, Apple) to a small, fast-loading size for list thumbnails. */
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

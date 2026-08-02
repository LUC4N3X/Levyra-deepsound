package com.luc4n3x.levyra.desktop.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.AppInfo
import com.luc4n3x.levyra.desktop.app.state.Destination
import com.luc4n3x.levyra.desktop.app.state.LevyraAppModel
import com.luc4n3x.levyra.desktop.app.state.PlaybackUiState
import com.luc4n3x.levyra.desktop.app.ui.components.DownloadActions
import com.luc4n3x.levyra.desktop.app.ui.components.LocalDownloadActions
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.components.tracksTextInputFocus
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.i18n.stringsFor
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.icons.OfflineIcons
import com.luc4n3x.levyra.desktop.app.ui.player.PlayerBar
import com.luc4n3x.levyra.desktop.app.ui.player.QueuePanel
import com.luc4n3x.levyra.desktop.app.ui.screens.CollectionScreen
import com.luc4n3x.levyra.desktop.app.ui.screens.DiscoverScreen
import com.luc4n3x.levyra.desktop.app.ui.screens.HomeScreen
import com.luc4n3x.levyra.desktop.app.ui.screens.LibraryScreen
import com.luc4n3x.levyra.desktop.app.ui.screens.NowPlayingScreen
import com.luc4n3x.levyra.desktop.app.ui.screens.OnboardingScreen
import com.luc4n3x.levyra.desktop.app.ui.screens.PlaylistScreen
import com.luc4n3x.levyra.desktop.app.ui.screens.SearchScreen
import com.luc4n3x.levyra.desktop.app.ui.screens.SettingsScreen
import com.luc4n3x.levyra.desktop.app.ui.theme.ArtworkPalette
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraBrand
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraMotion
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraTheme
import com.luc4n3x.levyra.desktop.app.ui.theme.LocalAccentColor
import com.luc4n3x.levyra.desktop.core.model.SearchFilter
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.storage.LibraryData
import com.luc4n3x.levyra.desktop.player.VlcNativeLocator
import java.awt.Desktop
import javax.swing.JFileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

@Composable
fun LevyraRoot(model: LevyraAppModel) {
    val settings by model.settings.collectAsState()
    val library by model.library.collectAsState()
    val destination by model.destination.collectAsState()
    val queueVisible by model.queueVisible.collectAsState()
    val openPlaylistId by model.openPlaylistId.collectAsState()
    val search by model.catalogController.search.collectAsState()
    val collection by model.catalogController.collection.collectAsState()
    val discover by model.discoverController.discover.collectAsState()

    val chromePlaybackFlow = remember(model) {
        model.playbackController.state
            .map(PlaybackUiState::withoutTransientUiTicks)
            .distinctUntilChanged()
    }
    val playback by chromePlaybackFlow.collectAsState(
        initial = model.playbackController.state.value.withoutTransientUiTicks()
    )

    val strings = stringsFor(settings.language, settings.displayName)
    val layoutDirection = if (settings.language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val snackbarState = remember { SnackbarHostState() }

    var pendingPlaylistTrack by remember { mutableStateOf<Track?>(null) }
    var newPlaylistName by remember { mutableStateOf("") }
    var vlcStatus by remember { mutableStateOf("") }
    var artworkAccent by remember { mutableStateOf(LevyraBrand.cyan) }

    val currentArtwork = playback.current?.artworkUrl.orEmpty()
    val playerVisible = !playback.queue.isEmpty
    val favoriteIds = remember(library.favorites) { library.favorites.mapTo(HashSet()) { it.id } }

    LaunchedEffect(model) {
        model.notices.collect { message -> snackbarState.showSnackbar(message) }
    }

    LaunchedEffect(currentArtwork) {
        artworkAccent = ArtworkPalette.accentFor(currentArtwork) ?: LevyraBrand.cyan
    }

    LaunchedEffect(playback.current?.id, destination) {
        if (destination == Destination.NOW_PLAYING) {
            model.lyricsController.requestFor(playback.current)
        }
    }

    val accent by animateColorAsState(
        targetValue = artworkAccent,
        animationSpec = tween(durationMillis = 280)
    )

    val actions = remember(model, playback.current?.id, favoriteIds) {
        TrackActions(
            currentTrackId = playback.current?.id.orEmpty(),
            isFavorite = { track -> track.id in favoriteIds },
            onPlay = { tracks, index ->
                model.playbackController.playTracks(
                    tracks.map { model.downloadController.completedTrack(it) ?: it },
                    index
                )
            },
            onPlayNext = { track ->
                model.playbackController.enqueueNext(
                    listOf(model.downloadController.completedTrack(track) ?: track)
                )
            },
            onEnqueue = { track ->
                model.playbackController.enqueueLast(
                    listOf(model.downloadController.completedTrack(track) ?: track)
                )
            },
            onToggleFavorite = { track ->
                model.toggleFavorite(track.copy(offlinePath = "", offlineMediaLabel = ""))
            },
            onAddToPlaylist = { track ->
                pendingPlaylistTrack = track.copy(offlinePath = "", offlineMediaLabel = "")
            }
        )
    }

    val downloadActions = remember(model) {
        DownloadActions(
            recordFor = model.downloadController::recordFor,
            onDownload = model.downloadController::enqueue,
            onCancel = model.downloadController::cancel,
            onRetry = model.downloadController::retry,
            onDelete = model.downloadController::delete
        )
    }

    LevyraTheme(themeMode = settings.themeMode) {
        CompositionLocalProvider(
            LocalStrings provides strings,
            LocalAccentColor provides accent,
            LocalLayoutDirection provides layoutDirection,
            LocalDownloadActions provides downloadActions
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                if (!settings.onboardingCompleted) {
                    OnboardingScreen(
                        initialSettings = settings,
                        onLanguagePreview = { language ->
                            model.updateSettings { it.copy(language = language) }
                        },
                        onComplete = { displayName, language, tastes, country ->
                            model.updateSettings {
                                it.copy(
                                    displayName = displayName,
                                    language = language,
                                    selectedTasteIds = tastes,
                                    contentCountry = country,
                                    onboardingCompleted = true
                                )
                            }
                            model.navigate(Destination.HOME)
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        accent.copy(alpha = 0.045f),
                                        MaterialTheme.colorScheme.background,
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NavigationSidebar(
                                    destination = destination,
                                    onNavigate = model::navigate
                                )

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        accent.copy(alpha = 0.05f),
                                                        Color.Transparent,
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    ) {
                                        when (destination) {
                                            Destination.HOME -> HomeScreen(
                                                library = library,
                                                discover = discover,
                                                currentTrack = playback.current,
                                                actions = actions,
                                                onOpenSearch = { model.navigate(Destination.SEARCH) },
                                                onOpenDiscover = { model.navigate(Destination.DISCOVER) },
                                                onOpenNewReleases = {
                                                    model.catalogController.clearSearch()
                                                    model.catalogController.setFilter(SearchFilter.ALBUMS)
                                                    model.catalogController.onQueryChange(strings.homeNewReleasesQuery)
                                                    model.catalogController.submit(strings.homeNewReleasesQuery)
                                                    model.navigate(Destination.SEARCH)
                                                },
                                                onPlayMix = { tracks ->
                                                    model.playbackController.playShuffled(
                                                        tracks.map { model.downloadController.completedTrack(it) ?: it }
                                                    )
                                                },
                                                onOpenPlaylist = model::openPlaylist,
                                                onCreatePlaylist = model.libraryStore::createPlaylist,
                                                onImportUrl = model::openCollectionFromUrl,
                                                onClearHistory = model.libraryStore::clearHistory
                                            )

                                            Destination.DISCOVER -> DiscoverScreen(
                                                state = discover,
                                                actions = actions,
                                                onCountryChange = { value ->
                                                    model.updateSettings { it.copy(contentCountry = value) }
                                                },
                                                onRefresh = model::refreshDiscover,
                                                onPlayAll = { actions.onPlay(discover.tracks, 0) },
                                                onShuffleAll = {
                                                    model.playbackController.playShuffled(
                                                        discover.tracks.map { model.downloadController.completedTrack(it) ?: it }
                                                    )
                                                }
                                            )

                                            Destination.SEARCH -> SearchScreen(
                                                state = search,
                                                recentSearches = library.recentSearches,
                                                contentCountry = settings.contentCountry,
                                                actions = actions,
                                                onQueryChange = model.catalogController::onQueryChange,
                                                onSubmit = model.catalogController::submit,
                                                onFilterChange = model.catalogController::setFilter,
                                                onLoadMore = model.catalogController::loadMoreSearch,
                                                onOpenCollection = model::openCollection,
                                                onClearRecent = model.libraryStore::clearRecentSearches
                                            )

                                            Destination.COLLECTION -> CollectionScreen(
                                                state = collection,
                                                actions = actions,
                                                onBack = model::back,
                                                onLoadMore = model.catalogController::loadMoreCollection,
                                                onPlayAll = { actions.onPlay(collection.page.tracks, 0) },
                                                onShuffleAll = {
                                                    model.playbackController.playShuffled(
                                                        collection.page.tracks.map {
                                                            model.downloadController.completedTrack(it) ?: it
                                                        }
                                                    )
                                                },
                                                onEnqueueAll = {
                                                    model.playbackController.enqueueLast(
                                                        collection.page.tracks.map {
                                                            model.downloadController.completedTrack(it) ?: it
                                                        }
                                                    )
                                                },
                                                onOpenCollection = model::openCollection
                                            )

                                            Destination.LIBRARY -> LibraryHost(
                                                model = model,
                                                library = library,
                                                actions = actions
                                            )

                                            Destination.PLAYLIST -> {
                                                val playlist = library.playlists.firstOrNull { it.id == openPlaylistId }
                                                PlaylistScreen(
                                                    playlist = playlist,
                                                    actions = actions,
                                                    onBack = model::back,
                                                    onPlayAll = {
                                                        playlist?.let { actions.onPlay(it.tracks, 0) }
                                                    },
                                                    onShuffleAll = {
                                                        playlist?.let {
                                                            model.playbackController.playShuffled(
                                                                it.tracks.map { track ->
                                                                    model.downloadController.completedTrack(track) ?: track
                                                                }
                                                            )
                                                        }
                                                    },
                                                    onRename = { name ->
                                                        playlist?.let { model.libraryStore.renamePlaylist(it.id, name) }
                                                    },
                                                    onDelete = {
                                                        playlist?.let { model.libraryStore.deletePlaylist(it.id) }
                                                        model.navigate(Destination.LIBRARY)
                                                    },
                                                    onRemoveTrack = { trackId ->
                                                        playlist?.let {
                                                            model.libraryStore.removeFromPlaylist(it.id, trackId)
                                                        }
                                                    }
                                                )
                                            }

                                            Destination.NOW_PLAYING -> NowPlayingHost(
                                                model = model,
                                                actions = actions
                                            )

                                            Destination.SETTINGS -> SettingsScreen(
                                                settings = settings,
                                                dataDirectory = model.paths.root.toString(),
                                                vlcStatus = vlcStatus,
                                                appVersion = AppInfo.version(),
                                                onUpdate = model::updateSettings,
                                                onBrowseVlc = {
                                                    rememberCoroutineScope().launch {
                                                        val selected = chooseDirectory()
                                                        if (selected.isNotBlank()) {
                                                            model.updateSettings { it.copy(vlcDirectory = selected) }
                                                        }
                                                    }
                                                },
                                                onVerifyVlc = {
                                                    rememberCoroutineScope().launch {
                                                        vlcStatus = verifyVlc(
                                                            settings.vlcDirectory,
                                                            strings.settingsVlcDetected,
                                                            strings.settingsVlcMissing
                                                        )
                                                    }
                                                },
                                                onOpenDataFolder = {
                                                    rememberCoroutineScope().launch {
                                                        openDirectory(model.paths.root.toString())
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                if (queueVisible) {
                                    QueuePanel(
                                        queue = playback.queue,
                                        onJumpTo = model.playbackController::jumpTo,
                                        onRemove = model.playbackController::removeFromQueue,
                                        onClear = model.playbackController::clearQueue,
                                        onClose = model::closeQueue,
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(18.dp))
                                    )
                                }
                            }

                            if (playerVisible) {
                                PlayerBarHost(
                                    model = model,
                                    library = library,
                                    queueVisible = queueVisible,
                                    destination = destination,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }

                        SnackbarHost(
                            hostState = snackbarState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = if (playerVisible) 104.dp else 18.dp)
                        )

                        val pending = pendingPlaylistTrack
                        if (pending != null) {
                            AlertDialog(
                                onDismissRequest = { pendingPlaylistTrack = null },
                                title = { Text(strings.addToPlaylist) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        library.playlists.forEach { playlist ->
                                            Text(
                                                text = "${playlist.name} · ${playlist.tracks.size}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(9.dp))
                                                    .clickable {
                                                        model.libraryStore.addToPlaylist(
                                                            playlist.id,
                                                            listOf(pending)
                                                        )
                                                        pendingPlaylistTrack = null
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                            )
                                        }
                                        OutlinedTextField(
                                            value = newPlaylistName,
                                            onValueChange = { newPlaylistName = it },
                                            label = { Text(strings.playlistName) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().tracksTextInputFocus()
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val playlistId = model.libraryStore.createPlaylist(newPlaylistName)
                                            model.libraryStore.addToPlaylist(playlistId, listOf(pending))
                                            newPlaylistName = ""
                                            pendingPlaylistTrack = null
                                        },
                                        enabled = newPlaylistName.isNotBlank()
                                    ) {
                                        Text(strings.playlistCreate)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { pendingPlaylistTrack = null }) {
                                        Text(strings.cancel)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryHost(
    model: LevyraAppModel,
    library: LibraryData,
    actions: TrackActions
) {
    val downloads by model.downloadController.downloads.collectAsState()
    val scope = rememberCoroutineScope()
    LibraryScreen(
        library = library,
        downloads = downloads,
        actions = actions,
        onOpenPlaylist = model::openPlaylist,
        onClearHistory = model.libraryStore::clearHistory,
        onOpenDownloadsFolder = {
            scope.launch { openDirectory(model.paths.downloadsDirectory.toString()) }
        },
        onCancelDownload = model.downloadController::cancel,
        onRetryDownload = model.downloadController::retry,
        onDeleteDownload = model.downloadController::delete
    )
}

@Composable
private fun NowPlayingHost(
    model: LevyraAppModel,
    actions: TrackActions
) {
    val playback by model.playbackController.state.collectAsState()
    val lyrics by model.lyricsController.lyrics.collectAsState()
    NowPlayingScreen(
        state = playback,
        lyricsState = lyrics,
        actions = actions,
        onBack = model::back,
        onJumpTo = model.playbackController::jumpTo
    )
}

@Composable
private fun PlayerBarHost(
    model: LevyraAppModel,
    library: LibraryData,
    queueVisible: Boolean,
    destination: Destination,
    modifier: Modifier = Modifier
) {
    val playback by model.playbackController.state.collectAsState()
    PlayerBar(
        state = playback,
        isFavorite = playback.current?.let { current ->
            library.favorites.any { it.id == current.id }
        } ?: false,
        queueVisible = queueVisible,
        onPlayPause = model.playbackController::togglePlayPause,
        onNext = { model.playbackController.next(automatic = false) },
        onPrevious = model.playbackController::previous,
        onSeek = model.playbackController::seekTo,
        onVolumeChange = model.playbackController::setVolume,
        onToggleMute = model.playbackController::toggleMuted,
        onToggleShuffle = model.playbackController::toggleShuffle,
        onCycleRepeat = model.playbackController::cycleRepeat,
        onToggleQueue = model::toggleQueue,
        onToggleFavorite = {
            playback.current?.let { track ->
                model.toggleFavorite(track.copy(offlinePath = "", offlineMediaLabel = ""))
            }
        },
        onSpeedChange = model.playbackController::setSpeed,
        onSleepTimerMinutes = model.playbackController::startSleepTimer,
        onSleepAtEndOfTrack = model.playbackController::sleepAtEndOfTrack,
        onCancelSleepTimer = model.playbackController::cancelSleepTimer,
        onClose = {
            model.closeQueue()
            model.playbackController.clearQueue()
            if (destination == Destination.NOW_PLAYING) {
                model.navigate(Destination.HOME)
            }
        },
        onOpenNowPlaying = { model.navigate(Destination.NOW_PLAYING) },
        modifier = modifier
    )
}

@Composable
private fun NavigationSidebar(
    destination: Destination,
    onNavigate: (Destination) -> Unit
) {
    val strings = LocalStrings.current
    val accent = LocalAccentColor.current
    Surface(
        modifier = Modifier
            .width(214.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 15.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource("icons/levyra.png"),
                            contentDescription = strings.appName,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Text(
                    text = strings.appName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            SidebarSectionLabel(strings.navSectionExplore)
            Spacer(modifier = Modifier.height(7.dp))

            SidebarItem(
                icon = LevyraIcons.Home,
                label = strings.navHome,
                selected = destination == Destination.HOME,
                onClick = { onNavigate(Destination.HOME) }
            )
            SidebarItem(
                icon = LevyraIcons.Chart,
                label = strings.navDiscover,
                selected = destination == Destination.DISCOVER,
                onClick = { onNavigate(Destination.DISCOVER) }
            )
            SidebarItem(
                icon = LevyraIcons.Search,
                label = strings.navSearch,
                selected = destination == Destination.SEARCH || destination == Destination.COLLECTION,
                onClick = { onNavigate(Destination.SEARCH) }
            )

            Spacer(modifier = Modifier.height(20.dp))
            SidebarSectionLabel(strings.navSectionLibrary)
            Spacer(modifier = Modifier.height(7.dp))

            SidebarItem(
                icon = OfflineIcons.Library,
                label = strings.navLibrary,
                selected = destination == Destination.LIBRARY || destination == Destination.PLAYLIST,
                onClick = { onNavigate(Destination.LIBRARY) }
            )
            SidebarItem(
                icon = LevyraIcons.Disc,
                label = strings.navNowPlaying,
                selected = destination == Destination.NOW_PLAYING,
                onClick = { onNavigate(Destination.NOW_PLAYING) }
            )

            Spacer(modifier = Modifier.weight(1f))

            SidebarItem(
                icon = LevyraIcons.Settings,
                label = strings.navSettings,
                selected = destination == Destination.SETTINGS,
                onClick = { onNavigate(Destination.SETTINGS) }
            )
        }
    }
}

@Composable
private fun SidebarSectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 11.dp)
    )
}

@Composable
private fun SidebarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = LocalAccentColor.current
    val interactionSource = remember(label) { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val background = when {
        selected -> accent.copy(alpha = LevyraMotion.SELECTED_ALPHA)
        hovered -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = LevyraMotion.HOVER_ALPHA)
        else -> Color.Transparent
    }
    val contentColor = when {
        selected -> accent
        hovered -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(background)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(23.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(if (selected) accent else Color.Transparent)
        )
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(19.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private fun PlaybackUiState.withoutTransientUiTicks(): PlaybackUiState = copy(
    positionMs = 0L,
    sleepRemainingMs = 0L
)

private suspend fun chooseDirectory(): String = withContext(Dispatchers.Swing) {
    val chooser = JFileChooser()
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    chooser.isMultiSelectionEnabled = false
    val result = chooser.showOpenDialog(null)
    if (result == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile?.absolutePath.orEmpty()
    } else {
        ""
    }
}

private suspend fun verifyVlc(
    directory: String,
    detectedLabel: String,
    missingLabel: String
): String = withContext(Dispatchers.Default) {
    val discovery = VlcNativeLocator.discover(directory)
    if (discovery.available) {
        "$detectedLabel: ${discovery.path}"
    } else {
        "$missingLabel: ${discovery.searchedDirectories.joinToString(", ")}"
    }
}

private suspend fun openDirectory(path: String) = withContext(Dispatchers.IO) {
    runCatching {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(java.io.File(path))
        }
    }
    Unit
}

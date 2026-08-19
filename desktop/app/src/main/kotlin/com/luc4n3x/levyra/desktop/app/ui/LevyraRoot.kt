package com.luc4n3x.levyra.desktop.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalLayoutDirection
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
import com.luc4n3x.levyra.desktop.app.ui.components.navigation.LevyraSidebar
import com.luc4n3x.levyra.desktop.app.ui.components.tracksTextInputFocus
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.i18n.stringsFor
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
import com.luc4n3x.levyra.desktop.app.ui.screens.LocalMusicScreen
import com.luc4n3x.levyra.desktop.app.ui.screens.SettingsScreen
import com.luc4n3x.levyra.desktop.app.ui.theme.ArtworkPalette
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraBrand
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
    val localMusic by model.localMusicController.state.collectAsState()
    val audioOutputDevices by model.playbackController.audioOutputDevices.collectAsState()
    val audioOutputDeviceMissing by model.playbackController.audioOutputDeviceMissing.collectAsState()
    val library by model.library.collectAsState()
    val destination by model.destination.collectAsState()
    val queueVisible by model.queueVisible.collectAsState()
    val openPlaylistId by model.openPlaylistId.collectAsState()
    val search by model.catalogController.search.collectAsState()
    val collection by model.catalogController.collection.collectAsState()
    val discover by model.discoverController.discover.collectAsState()
    val localSearchResults = remember(localMusic.index, search.query) {
        if (search.query.length < 2) {
            emptyList()
        } else {
            localMusic.index.search(search.query, limit = LOCAL_SEARCH_PREVIEW_LIMIT)
                .map { it.toTrack() }
        }
    }

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
    val scope = rememberCoroutineScope()

    var pendingPlaylistTrack by remember { mutableStateOf<Track?>(null) }
    var newPlaylistName by remember { mutableStateOf("") }
    var vlcStatus by remember { mutableStateOf("") }
    var artworkAccent by remember { mutableStateOf(LevyraBrand.cyan) }

    val currentArtwork = playback.current?.artworkUrl.orEmpty()
    val playerVisible = !playback.queue.isEmpty
    val favoriteIds = remember(library.favorites) {
        library.favorites.mapTo(HashSet<String>()) { it.id }
    }

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
                model.toggleFavorite(track.asLibraryEntry())
            },
            onAddToPlaylist = { track ->
                pendingPlaylistTrack = track.asLibraryEntry()
            }
        )
    }

    val downloadActions = remember(model) {
        DownloadActions(
            stateFlow = model.downloadController.downloads,
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
                            .background(MaterialTheme.colorScheme.background)
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
                                LevyraSidebar(
                                    destination = destination,
                                    hasActiveTrack = playback.current != null,
                                    isPlaying = playback.isPlaying,
                                    onNavigate = model::navigate
                                )

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface
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
                                                onCreatePlaylist = { name ->
                                                    model.libraryStore.createPlaylist(name)
                                                },
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
                                                onClearRecent = model.libraryStore::clearRecentSearches,
                                                localResults = localSearchResults
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

                                            Destination.LOCAL_MUSIC -> LocalMusicScreen(
                                                state = localMusic,
                                                actions = actions,
                                                onAddFolder = {
                                                    scope.launch {
                                                        val selected = chooseDirectory()
                                                        if (selected.isNotBlank()) {
                                                            model.localMusicController.addFolder(selected)
                                                        }
                                                    }
                                                },
                                                onRemoveFolder = model.localMusicController::removeFolder,
                                                onRescan = model.localMusicController::rescan,
                                                onForgetMissing = model.localMusicController::forgetUnavailable
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
                                                audioOutputDevices = audioOutputDevices,
                                                audioOutputDeviceMissing = audioOutputDeviceMissing,
                                                onUpdate = model::updateSettings,
                                                onBrowseVlc = {
                                                    scope.launch {
                                                        val selected = chooseDirectory()
                                                        if (selected.isNotBlank()) {
                                                            model.updateSettings { it.copy(vlcDirectory = selected) }
                                                        }
                                                    }
                                                },
                                                onVerifyVlc = {
                                                    scope.launch {
                                                        vlcStatus = verifyVlc(
                                                            settings.vlcDirectory,
                                                            strings.settingsVlcDetected,
                                                            strings.settingsVlcMissing
                                                        )
                                                    }
                                                },
                                                onOpenDataFolder = {
                                                    scope.launch {
                                                        openDirectory(model.paths.root.toString())
                                                    }
                                                },
                                                onRefreshAudioOutputDevices = { createEngine ->
                                                    model.playbackController.refreshAudioOutputDevices(createEngine)
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
    val playback by remember(model) {
        model.playbackController.state
            .map(com.luc4n3x.levyra.desktop.app.state.PlaybackUiState::withoutTransientUiTicks)
            .distinctUntilChanged()
    }.collectAsState(initial = model.playbackController.state.value.withoutTransientUiTicks())

    PlayerBar(
        state = playback,
        playbackStateFlow = model.playbackController.state,
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
                model.toggleFavorite(track.asLibraryEntry())
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

private const val LOCAL_SEARCH_PREVIEW_LIMIT = 6

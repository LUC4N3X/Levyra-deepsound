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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.AppInfo
import com.luc4n3x.levyra.desktop.app.state.Destination
import com.luc4n3x.levyra.desktop.app.state.LevyraAppModel
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.i18n.stringsFor
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.player.PlayerBar
import com.luc4n3x.levyra.desktop.app.ui.player.QueuePanel
import com.luc4n3x.levyra.desktop.app.ui.screens.CollectionScreen
import com.luc4n3x.levyra.desktop.app.ui.screens.DiscoverScreen
import com.luc4n3x.levyra.desktop.app.ui.screens.HomeScreen
import com.luc4n3x.levyra.desktop.app.ui.screens.NowPlayingScreen
import com.luc4n3x.levyra.desktop.app.ui.screens.PlaylistScreen
import com.luc4n3x.levyra.desktop.app.ui.screens.SearchScreen
import com.luc4n3x.levyra.desktop.app.ui.screens.SettingsScreen
import com.luc4n3x.levyra.desktop.app.ui.theme.ArtworkPalette
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraBrand
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraTheme
import com.luc4n3x.levyra.desktop.app.ui.theme.LocalAccentColor
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.player.VlcNativeLocator
import java.awt.Desktop
import javax.swing.JFileChooser
import kotlinx.coroutines.Dispatchers
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
    val playback by model.playbackController.state.collectAsState()
    val search by model.catalogController.search.collectAsState()
    val collection by model.catalogController.collection.collectAsState()
    val discover by model.discoverController.discover.collectAsState()
    val lyrics by model.lyricsController.lyrics.collectAsState()

    val strings = stringsFor(settings.language)
    val snackbarState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var pendingPlaylistTrack by remember { mutableStateOf<Track?>(null) }
    var newPlaylistName by remember { mutableStateOf("") }
    var vlcStatus by remember { mutableStateOf("") }
    var artworkAccent by remember { mutableStateOf(LevyraBrand.cyan) }

    val currentArtwork = playback.current?.artworkUrl.orEmpty()

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

    val accent by animateColorAsState(targetValue = artworkAccent, animationSpec = tween(durationMillis = 500))

    val actions = TrackActions(
        currentTrackId = playback.current?.id.orEmpty(),
        isFavorite = { track -> library.favorites.any { it.id == track.id } },
        onPlay = { tracks, index -> model.playbackController.playTracks(tracks, index) },
        onPlayNext = { track -> model.playbackController.enqueueNext(listOf(track)) },
        onEnqueue = { track -> model.playbackController.enqueueLast(listOf(track)) },
        onToggleFavorite = { track -> model.toggleFavorite(track) },
        onAddToPlaylist = { track -> pendingPlaylistTrack = track }
    )

    LevyraTheme(themeMode = settings.themeMode) {
        CompositionLocalProvider(LocalStrings provides strings, LocalAccentColor provides accent) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        NavigationRail(
                            destination = destination,
                            onNavigate = model::navigate
                        )
                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            when (destination) {
                                Destination.HOME -> HomeScreen(
                                    library = library,
                                    actions = actions,
                                    onOpenPlaylist = model::openPlaylist,
                                    onCreatePlaylist = { name -> model.libraryStore.createPlaylist(name) },
                                    onImportUrl = model::openCollectionFromUrl,
                                    onClearHistory = { model.libraryStore.clearHistory() }
                                )

                                Destination.DISCOVER -> DiscoverScreen(
                                    state = discover,
                                    actions = actions,
                                    onCountryChange = { value ->
                                        model.updateSettings { it.copy(contentCountry = value) }
                                    },
                                    onRefresh = model::refreshDiscover,
                                    onPlayAll = {
                                        model.playbackController.playTracks(discover.tracks, 0)
                                    },
                                    onShuffleAll = {
                                        model.playbackController.playShuffled(discover.tracks)
                                    }
                                )

                                Destination.SEARCH -> SearchScreen(
                                    state = search,
                                    recentSearches = library.recentSearches,
                                    actions = actions,
                                    onQueryChange = model.catalogController::onQueryChange,
                                    onSubmit = model.catalogController::submit,
                                    onFilterChange = model.catalogController::setFilter,
                                    onLoadMore = model.catalogController::loadMoreSearch,
                                    onOpenCollection = model::openCollection,
                                    onClearRecent = { model.libraryStore.clearRecentSearches() }
                                )

                                Destination.COLLECTION -> CollectionScreen(
                                    state = collection,
                                    actions = actions,
                                    onBack = model::back,
                                    onLoadMore = model.catalogController::loadMoreCollection,
                                    onPlayAll = {
                                        model.playbackController.playTracks(collection.page.tracks, 0)
                                    },
                                    onShuffleAll = {
                                        model.playbackController.playShuffled(collection.page.tracks)
                                    },
                                    onEnqueueAll = {
                                        model.playbackController.enqueueLast(collection.page.tracks)
                                    },
                                    onOpenCollection = model::openCollection
                                )

                                Destination.PLAYLIST -> {
                                    val playlist = library.playlists.firstOrNull { it.id == openPlaylistId }
                                    PlaylistScreen(
                                        playlist = playlist,
                                        actions = actions,
                                        onBack = model::back,
                                        onPlayAll = {
                                            playlist?.let { model.playbackController.playTracks(it.tracks, 0) }
                                        },
                                        onShuffleAll = {
                                            playlist?.let { model.playbackController.playShuffled(it.tracks) }
                                        },
                                        onRename = { name ->
                                            playlist?.let { model.libraryStore.renamePlaylist(it.id, name) }
                                        },
                                        onDelete = {
                                            playlist?.let { model.libraryStore.deletePlaylist(it.id) }
                                            model.navigate(Destination.HOME)
                                        },
                                        onRemoveTrack = { trackId ->
                                            playlist?.let { model.libraryStore.removeFromPlaylist(it.id, trackId) }
                                        }
                                    )
                                }

                                Destination.NOW_PLAYING -> NowPlayingScreen(
                                    state = playback,
                                    lyricsState = lyrics,
                                    actions = actions,
                                    onBack = model::back,
                                    onJumpTo = model.playbackController::jumpTo
                                )

                                Destination.SETTINGS -> SettingsScreen(
                                    settings = settings,
                                    dataDirectory = model.paths.root.toString(),
                                    vlcStatus = vlcStatus,
                                    appVersion = AppInfo.version(),
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
                                            vlcStatus = verifyVlc(settings.vlcDirectory, strings.settingsVlcDetected, strings.settingsVlcMissing)
                                        }
                                    },
                                    onOpenDataFolder = {
                                        scope.launch { openDirectory(model.paths.root.toString()) }
                                    }
                                )
                            }
                        }

                        if (queueVisible) {
                            QueuePanel(
                                queue = playback.queue,
                                onJumpTo = model.playbackController::jumpTo,
                                onRemove = model.playbackController::removeFromQueue,
                                onClear = model.playbackController::clearQueue,
                                onClose = model::toggleQueue
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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
                            playback.current?.let { track -> model.toggleFavorite(track) }
                        },
                        onOpenNowPlaying = { model.navigate(Destination.NOW_PLAYING) }
                    )
                }

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    SnackbarHost(hostState = snackbarState, modifier = Modifier.padding(bottom = 120.dp))
                }
            }

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
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            model.libraryStore.addToPlaylist(playlist.id, listOf(pending))
                                            pendingPlaylistTrack = null
                                        }
                                        .padding(horizontal = 10.dp, vertical = 10.dp)
                                )
                            }
                            OutlinedTextField(
                                value = newPlaylistName,
                                onValueChange = { newPlaylistName = it },
                                label = { Text(strings.playlistName) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
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

@Composable
private fun NavigationRail(
    destination: Destination,
    onNavigate: (Destination) -> Unit
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .width(96.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = strings.appName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        RailItem(
            icon = LevyraIcons.Home,
            label = strings.navHome,
            selected = destination == Destination.HOME || destination == Destination.PLAYLIST,
            onClick = { onNavigate(Destination.HOME) }
        )
        RailItem(
            icon = LevyraIcons.Chart,
            label = strings.navDiscover,
            selected = destination == Destination.DISCOVER,
            onClick = { onNavigate(Destination.DISCOVER) }
        )
        RailItem(
            icon = LevyraIcons.Search,
            label = strings.navSearch,
            selected = destination == Destination.SEARCH || destination == Destination.COLLECTION,
            onClick = { onNavigate(Destination.SEARCH) }
        )
        RailItem(
            icon = LevyraIcons.Disc,
            label = strings.navNowPlaying,
            selected = destination == Destination.NOW_PLAYING,
            onClick = { onNavigate(Destination.NOW_PLAYING) }
        )
        RailItem(
            icon = LevyraIcons.Settings,
            label = strings.navSettings,
            selected = destination == Destination.SETTINGS,
            onClick = { onNavigate(Destination.SETTINGS) }
        )
    }
}

@Composable
private fun RailItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainer
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

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

private suspend fun verifyVlc(directory: String, detectedLabel: String, missingLabel: String): String =
    withContext(Dispatchers.Default) {
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

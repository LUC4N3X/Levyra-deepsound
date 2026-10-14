package com.luc4n3x.levyra.ui.library

import com.luc4n3x.levyra.ui.components.LevyraBackButton
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.data.collections.PersonalCollectionSourceKind
import com.luc4n3x.levyra.data.collections.PersonalCollectionSummary
import com.luc4n3x.levyra.data.collections.CollectionSyncError
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraExpressiveIconButton
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraWavyProgress
import com.luc4n3x.levyra.ui.i18n.LevyraPersonalCollectionStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.selection.TrackSelectionActions
import com.luc4n3x.levyra.ui.selection.TrackSelectionBar
import com.luc4n3x.levyra.ui.selection.TrackSelectionState
import com.luc4n3x.levyra.ui.selection.rememberTrackSelectionState
import com.luc4n3x.levyra.ui.selection.resolveSelected
import com.luc4n3x.levyra.ui.selection.trackSelectionKey
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LibraryViewModel
import com.luc4n3x.levyra.viewmodel.PersonalCollectionEvent
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PersonalCollectionScreen(
    collection: PersonalCollectionSummary,
    syncing: Boolean,
    state: LevyraUiState,
    viewModel: LibraryViewModel,
    collectionStrings: LevyraPersonalCollectionStrings,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val controller = viewModel.personalCollections
    val tracksFlow = remember(collection.id) { controller.tracks(collection.id) }
    val tracks by tracksFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val selection = rememberTrackSelectionState()
    val snackbar = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var confirmRemove by rememberSaveable { mutableStateOf(false) }
    var confirmDeleteDownloads by rememberSaveable { mutableStateOf(false) }
    var playlistTargets by remember { mutableStateOf<List<Track>>(emptyList()) }
    val localNetwork = rememberLocalNetworkAccess()

    val trackIds = remember(tracks) { tracks.mapTo(HashSet()) { it.id } }
    val collectionDownloads = remember(state.downloads, trackIds) {
        state.downloads.filter { it.trackId in trackIds }
    }
    val downloadedIds = remember(collectionDownloads) { collectionDownloads.mapTo(HashSet()) { it.trackId } }
    val activeDownloads = remember(state.downloadQueue, state.downloadProgressByTrackId, trackIds) {
        trackIds.count { id ->
            state.downloadProgressByTrackId.containsKey(id) ||
                state.downloadQueue.any { it.trackId == id && it.state in ACTIVE_DOWNLOAD_STATES }
        }
    }
    val selectableIds = remember(tracks) { tracks.map(::trackSelectionKey) }
    val selectedTracks = remember(tracks, selection.selectedIds) { selection.resolveSelected(tracks, ::trackSelectionKey) }
    LaunchedEffect(selectableIds) { selection.retainAvailable(selectableIds) }
    PersonalCollectionEventEffect(collection, controller.events, collectionStrings, snackbar) {
        haptics.performHapticFeedback(it)
    }
    BackHandler(enabled = selection.isActive) { selection.exit() }
    BackHandler(enabled = !selection.isActive) { onClose() }

    fun play(list: List<Track>, start: Track) = viewModel.playLocalTracks(list, start)

    Surface(color = LevyraInk, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = if (state.currentTrack != null || selection.isActive) 230.dp else 116.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "pc-top-bar") {
                    PersonalCollectionTopBar(
                        collection = collection,
                        syncing = syncing,
                        hasDownloads = collectionDownloads.isNotEmpty(),
                        backLabel = strings.back,
                        strings = collectionStrings,
                        onClose = onClose,
                        onRefresh = { controller.refresh(collection.id) },
                        onSettings = { showSettings = true },
                        onDeleteDownloads = { confirmDeleteDownloads = true },
                        onRemove = { confirmRemove = true }
                    )
                }
                item(key = "pc-hero") {
                    PersonalCollectionHero(collection, syncing, strings.formatTrackCount(tracks.size.takeIf { it > 0 } ?: collection.trackCount), collectionStrings)
                }
                personalCollectionLocalNetworkItem(
                    visible = collection.usesLocalNetwork && !localNetwork.granted,
                    strings = collectionStrings,
                    onAllow = { localNetwork.ensure { } }
                )
                personalCollectionSyncErrorItem(
                    error = collection.lastSyncError?.takeUnless { syncing },
                    refreshable = collection.refreshable,
                    strings = collectionStrings,
                    onRefresh = { controller.refresh(collection.id) }
                )
                item(key = "pc-actions") {
                    PersonalCollectionActions(
                        tracks = tracks,
                        downloadedIds = downloadedIds,
                        selectionActive = selection.isActive,
                        playLabel = strings.play,
                        shuffleLabel = strings.shuffle,
                        downloadLabel = collectionStrings.downloadAll,
                        onPlay = ::play,
                        onDownload = { viewModel.exportTracks(it, collection.name) }
                    )
                }
                personalCollectionOfflineItem(downloadedIds.size, tracks.size, activeDownloads, collectionStrings)
                personalCollectionTrackItems(
                    tracks = tracks,
                    downloads = collectionDownloads,
                    downloadedIds = downloadedIds,
                    state = state,
                    selection = selection,
                    emptyLabel = collectionStrings.emptyCollection,
                    onPlay = ::play,
                    onFavorite = viewModel::toggleFavorite,
                    onDownload = viewModel::exportTrack,
                    onQueue = viewModel::addToQueue,
                    onAddToPlaylist = { playlistTargets = listOf(it) },
                    onDeleteDownload = viewModel::deleteDownload
                )
            }

            PersonalCollectionSelectionBar(
                selection = selection,
                selectableIds = selectableIds,
                selectedTracks = selectedTracks,
                hasCurrentTrack = state.currentTrack != null,
                onPlayNext = viewModel::playTracksNext,
                onAddToQueue = viewModel::addTracksToQueue,
                onAddToPlaylist = { playlistTargets = it },
                onFavorite = viewModel::toggleFavorites,
                onDownload = { viewModel.exportTracks(it, collection.name) }
            )

            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (state.currentTrack != null) 160.dp else 88.dp)
            )
        }
    }

    PersonalCollectionPlaylistDialog(
        tracks = playlistTargets,
        state = state,
        onDismiss = { playlistTargets = emptyList() },
        onAdd = { viewModel.addTracksToPlaylist(it, playlistTargets) },
        onCreate = { viewModel.createPlaylistWithTracks(it, playlistTargets) },
        onComplete = {
            playlistTargets = emptyList()
            selection.exit()
        }
    )
    PersonalCollectionSettingsDialog(showSettings, collection, strings.save, collectionStrings, {
        showSettings = false
    }) { name, autoRefresh, allowCleartext ->
        controller.updateSettings(collection.id, name, autoRefresh, allowCleartext)
        showSettings = false
    }
    PersonalCollectionRemoveDialog(confirmRemove, collection, strings.cancel, collectionStrings, {
        confirmRemove = false
    }) {
        confirmRemove = false
        controller.delete(collection.id)
        onClose()
    }
    PersonalCollectionDeleteDownloadsDialog(
        visible = confirmDeleteDownloads,
        downloads = collectionDownloads,
        cancelLabel = strings.cancel,
        deleteLabel = strings.delete,
        strings = collectionStrings,
        onDismiss = { confirmDeleteDownloads = false },
        onConfirm = {
            confirmDeleteDownloads = false
            viewModel.deleteDownloads(collectionDownloads)
        }
    )
}

@Composable
private fun PersonalCollectionEventEffect(
    collection: PersonalCollectionSummary,
    events: Flow<PersonalCollectionEvent>,
    strings: LevyraPersonalCollectionStrings,
    snackbar: SnackbarHostState,
    onHaptic: (HapticFeedbackType) -> Unit
) {
    LaunchedEffect(collection.id) {
        events.collect { event ->
            when (event) {
                is PersonalCollectionEvent.Synced -> if (event.collectionId == collection.id) {
                    onHaptic(HapticFeedbackType.Confirm)
                    val report = event.report
                    val unchanged = report.notModified || report.added + report.removed + report.updated == 0
                    snackbar.showSnackbar(
                        if (unchanged) strings.upToDate else strings.syncSummary(report.added, report.removed)
                    )
                }
                is PersonalCollectionEvent.SyncFailed -> if (event.collectionId == collection.id) {
                    onHaptic(HapticFeedbackType.Reject)
                    snackbar.showSnackbar(strings.error(event.error))
                }
                is PersonalCollectionEvent.Imported -> Unit
            }
        }
    }
}

@Composable
private fun PersonalCollectionTopBar(
    collection: PersonalCollectionSummary,
    syncing: Boolean,
    hasDownloads: Boolean,
    backLabel: String,
    strings: LevyraPersonalCollectionStrings,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onDeleteDownloads: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var menuExpanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        LevyraBackButton(onClick = onClose, contentDescription = backLabel)
        Spacer(Modifier.weight(1f))
        if (collection.refreshable) {
            LevyraExpressiveIconButton(onClick = onRefresh, enabled = !syncing) {
                if (syncing) LevyraLoadingIndicator(modifier = Modifier.size(24.dp))
                else Icon(Icons.Rounded.Refresh, contentDescription = strings.refresh)
            }
        }
        Box {
            LevyraExpressiveIconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = strings.settingsTitle)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(strings.settingsTitle) },
                    leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    onClick = { menuExpanded = false; onSettings() }
                )
                if (hasDownloads) {
                    DropdownMenuItem(
                        text = { Text(strings.removeDownloads) },
                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                        onClick = { menuExpanded = false; onDeleteDownloads() }
                    )
                }
                DropdownMenuItem(
                    text = { Text(strings.removeCollection, color = colors.error) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = colors.error) },
                    onClick = { menuExpanded = false; onRemove() }
                )
            }
        }
    }
}

private fun LazyListScope.personalCollectionLocalNetworkItem(
    visible: Boolean,
    strings: LevyraPersonalCollectionStrings,
    onAllow: () -> Unit
) {
    if (!visible) return
    item(key = "pc-local-network") {
        val colors = MaterialTheme.colorScheme
        Surface(color = colors.tertiaryContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Rounded.Lan, contentDescription = null, tint = colors.onTertiaryContainer)
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.localNetworkTitle, style = MaterialTheme.typography.titleSmall, color = colors.onTertiaryContainer)
                    Text(strings.localNetworkDetail, style = MaterialTheme.typography.bodySmall, color = colors.onTertiaryContainer)
                }
                TextButton(onClick = onAllow) { Text(strings.allow) }
            }
        }
    }
}

private fun LazyListScope.personalCollectionSyncErrorItem(
    error: CollectionSyncError?,
    refreshable: Boolean,
    strings: LevyraPersonalCollectionStrings,
    onRefresh: () -> Unit
) {
    if (error == null) return
    item(key = "pc-sync-error") {
        val colors = MaterialTheme.colorScheme
        Surface(color = colors.errorContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = colors.onErrorContainer)
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.savedCopy, style = MaterialTheme.typography.titleSmall, color = colors.onErrorContainer)
                    Text(strings.error(error), style = MaterialTheme.typography.bodySmall, color = colors.onErrorContainer)
                }
                if (refreshable) {
                    TextButton(
                        onClick = onRefresh,
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.onErrorContainer)
                    ) { Text(strings.refresh, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun PersonalCollectionActions(
    tracks: List<Track>,
    downloadedIds: Set<String>,
    selectionActive: Boolean,
    playLabel: String,
    shuffleLabel: String,
    downloadLabel: String,
    onPlay: (List<Track>, Track) -> Unit,
    onDownload: (List<Track>) -> Unit
) {
    val allOffline = tracks.isNotEmpty() && downloadedIds.size >= tracks.size
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { tracks.firstOrNull()?.let { onPlay(tracks, it) } },
            enabled = tracks.isNotEmpty() && !selectionActive,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.weight(1f).heightIn(min = 56.dp)
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            ActionLabel(playLabel)
        }
        FilledTonalButton(
            onClick = { tracks.shuffled().let { shuffled -> shuffled.firstOrNull()?.let { onPlay(shuffled, it) } } },
            enabled = tracks.isNotEmpty() && !selectionActive,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.weight(1f).heightIn(min = 56.dp)
        ) {
            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            ActionLabel(shuffleLabel)
        }
        FilledTonalIconButton(
            onClick = { tracks.filterNot { it.id in downloadedIds }.takeIf { it.isNotEmpty() }?.let(onDownload) },
            enabled = tracks.isNotEmpty() && !allOffline && !selectionActive,
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(if (allOffline) Icons.Rounded.DownloadDone else Icons.Rounded.Download, contentDescription = downloadLabel)
        }
    }
}

private fun LazyListScope.personalCollectionOfflineItem(
    downloadedCount: Int,
    trackCount: Int,
    activeDownloads: Int,
    strings: LevyraPersonalCollectionStrings
) {
    if (downloadedCount == 0 && activeDownloads == 0) return
    item(key = "pc-offline") {
        val colors = MaterialTheme.colorScheme
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "${strings.offlineCount(downloadedCount)} / $trackCount",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant
            )
            if (activeDownloads > 0 && trackCount > 0) {
                LevyraWavyProgress(
                    progress = downloadedCount.toFloat() / trackCount.toFloat(),
                    color = colors.primary,
                    trackColor = colors.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun LazyListScope.personalCollectionTrackItems(
    tracks: List<Track>,
    downloads: List<DownloadedTrack>,
    downloadedIds: Set<String>,
    state: LevyraUiState,
    selection: TrackSelectionState,
    emptyLabel: String,
    onPlay: (List<Track>, Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onDownload: (Track) -> Unit,
    onQueue: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onDeleteDownload: (DownloadedTrack) -> Unit
) {
    if (tracks.isEmpty()) {
        item(key = "pc-empty") { LibraryEmpty(Icons.Rounded.LibraryMusic, emptyLabel) }
        return
    }
    items(tracks, key = { "pc-track-${it.id}" }) { track ->
        val key = trackSelectionKey(track)
        val download = downloads.firstOrNull { it.trackId == track.id }
        LibraryTrackRow(
            track = track,
            selected = selection.isSelected(key),
            selectionActive = selection.isActive,
            isCurrent = track.id == state.currentTrack?.id,
            isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
            isFavorite = track.id in state.favoriteIds,
            isDownloaded = track.id in downloadedIds,
            downloadProgress = downloadProgressFor(track, state),
            onClick = { if (selection.isActive) selection.toggle(key) else onPlay(tracks, track) },
            onLongClick = { selection.start(key) },
            onFavorite = { onFavorite(track) },
            onDownload = { onDownload(track) },
            onQueue = { onQueue(track) },
            onAddToPlaylist = { onAddToPlaylist(track) },
            onDeleteDownload = download?.let { stored -> { onDeleteDownload(stored) } },
            modifier = Modifier.animateItem()
        )
    }
}

@Composable
private fun BoxScope.PersonalCollectionSelectionBar(
    selection: TrackSelectionState,
    selectableIds: List<String>,
    selectedTracks: List<Track>,
    hasCurrentTrack: Boolean,
    onPlayNext: (List<Track>) -> Unit,
    onAddToQueue: (List<Track>) -> Unit,
    onAddToPlaylist: (List<Track>) -> Unit,
    onFavorite: (List<Track>) -> Unit,
    onDownload: (List<Track>) -> Unit
) {
    fun complete(action: (List<Track>) -> Unit) {
        action(selectedTracks)
        selection.exit()
    }
    val hasSelection = selectedTracks.isNotEmpty()
    TrackSelectionBar(
        state = selection,
        allVisibleIds = selectableIds,
        actions = TrackSelectionActions(
            onPlayNext = if (hasSelection) { { complete(onPlayNext) } } else null,
            onAddToQueue = if (hasSelection) { { complete(onAddToQueue) } } else null,
            onAddToPlaylist = if (hasSelection) { { onAddToPlaylist(selectedTracks) } } else null,
            onFavorite = if (hasSelection) { { complete(onFavorite) } } else null,
            onDownload = if (hasSelection) { { complete(onDownload) } } else null
        ),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(start = 12.dp, end = 12.dp, bottom = if (hasCurrentTrack) 84.dp else 12.dp)
    )
}

@Composable
private fun PersonalCollectionPlaylistDialog(
    tracks: List<Track>,
    state: LevyraUiState,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onCreate: (String) -> Unit,
    onComplete: () -> Unit
) {
    if (tracks.isEmpty()) return
    AddTracksToPlaylistDialog(
        tracks = tracks,
        playlists = state.playlists,
        onDismiss = onDismiss,
        onAdd = { onAdd(it); onComplete() },
        onCreate = { onCreate(it); onComplete() }
    )
}

@Composable
private fun PersonalCollectionSettingsDialog(
    visible: Boolean,
    collection: PersonalCollectionSummary,
    saveLabel: String,
    strings: LevyraPersonalCollectionStrings,
    onDismiss: () -> Unit,
    onSave: (String, Boolean, Boolean) -> Unit
) {
    if (!visible) return
    PersonalCollectionSettingsSheet(collection, strings, saveLabel, onDismiss, onSave)
}

@Composable
private fun PersonalCollectionRemoveDialog(
    visible: Boolean,
    collection: PersonalCollectionSummary,
    cancelLabel: String,
    strings: LevyraPersonalCollectionStrings,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
        title = { Text(strings.removeTitle(collection.name), textAlign = TextAlign.Center) },
        text = { Text(strings.removeBody) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(strings.removeCollection, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel) } }
    )
}

@Composable
private fun PersonalCollectionDeleteDownloadsDialog(
    visible: Boolean,
    downloads: List<DownloadedTrack>,
    cancelLabel: String,
    deleteLabel: String,
    strings: LevyraPersonalCollectionStrings,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
        title = { Text(strings.removeDownloads) },
        text = { Text(strings.removeDownloadsBody(downloads.size)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(deleteLabel, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel) } }
    )
}

@Composable
private fun PersonalCollectionHero(
    collection: PersonalCollectionSummary,
    syncing: Boolean,
    countLabel: String,
    collectionStrings: LevyraPersonalCollectionStrings
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PersonalCollectionArtwork(collection.artworkModel, size = 184.dp, large = true)
        Spacer(Modifier.size(6.dp))
        Text(
            collection.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            listOf(countLabel, collection.sourceDescription(collectionStrings)).filter(String::isNotBlank).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        PersonalCollectionStatusLine(collection, syncing, collectionStrings, showError = false)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PersonalCollectionSettingsSheet(
    collection: PersonalCollectionSummary,
    collectionStrings: LevyraPersonalCollectionStrings,
    saveLabel: String,
    onDismiss: () -> Unit,
    onSave: (String, Boolean, Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by rememberSaveable(collection.id) { mutableStateOf(collection.name) }
    var autoRefresh by rememberSaveable(collection.id) { mutableStateOf(collection.autoRefresh) }
    var allowCleartext by rememberSaveable(collection.id) { mutableStateOf(collection.allowCleartext) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                collectionStrings.settingsTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(collectionStrings.nameLabel) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            )
            if (collection.sourceKind == PersonalCollectionSourceKind.URL) {
                SettingSwitchRow(collectionStrings.autoRefresh, collectionStrings.autoRefreshDetail, autoRefresh) { autoRefresh = it }
            }
            CleartextConsentRow(collectionStrings, allowCleartext) { allowCleartext = it }
            Button(
                onClick = { onSave(name, autoRefresh, allowCleartext) },
                enabled = name.isNotBlank(),
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Text(saveLabel)
            }
        }
    }
}

@Composable
private fun ActionLabel(text: String) {
    BasicText(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(color = LocalContentColor.current),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = MaterialTheme.typography.labelLarge.fontSize)
    )
}

private val ACTIVE_DOWNLOAD_STATES = setOf("QUEUED", "RUNNING", "RETRYING", "PAUSED")

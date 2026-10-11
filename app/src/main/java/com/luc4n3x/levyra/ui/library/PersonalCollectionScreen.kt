package com.luc4n3x.levyra.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraExpressiveIconButton
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraWavyProgress
import com.luc4n3x.levyra.ui.i18n.LevyraPersonalCollectionStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.selection.TrackSelectionActions
import com.luc4n3x.levyra.ui.selection.TrackSelectionBar
import com.luc4n3x.levyra.ui.selection.rememberTrackSelectionState
import com.luc4n3x.levyra.ui.selection.resolveSelected
import com.luc4n3x.levyra.ui.selection.trackSelectionKey
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LibraryViewModel
import com.luc4n3x.levyra.viewmodel.PersonalCollectionEvent

@Suppress("ComplexMethod")
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
    val colors = MaterialTheme.colorScheme
    val controller = viewModel.personalCollections
    val tracksFlow = remember(collection.id) { controller.tracks(collection.id) }
    val tracks by tracksFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val selection = rememberTrackSelectionState()
    val snackbar = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }
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
    LaunchedEffect(collection.id) {
        controller.events.collect { event ->
            when (event) {
                is PersonalCollectionEvent.Synced -> if (event.collectionId == collection.id) {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    val report = event.report
                    snackbar.showSnackbar(
                        if (report.notModified || report.added + report.removed + report.updated == 0) {
                            collectionStrings.upToDate
                        } else {
                            collectionStrings.syncSummary(report.added, report.removed)
                        }
                    )
                }
                is PersonalCollectionEvent.SyncFailed -> if (event.collectionId == collection.id) {
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    snackbar.showSnackbar(collectionStrings.error(event.error))
                }
                is PersonalCollectionEvent.Imported -> Unit
            }
        }
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
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        LevyraExpressiveIconButton(onClick = onClose) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                        }
                        Spacer(Modifier.weight(1f))
                        if (collection.refreshable) {
                            LevyraExpressiveIconButton(onClick = { controller.refresh(collection.id) }, enabled = !syncing) {
                                if (syncing) {
                                    LevyraLoadingIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Icon(Icons.Rounded.Refresh, contentDescription = collectionStrings.refresh)
                                }
                            }
                        }
                        Box {
                            LevyraExpressiveIconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = collectionStrings.settingsTitle)
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(collectionStrings.settingsTitle) },
                                    leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        showSettings = true
                                    }
                                )
                                if (collectionDownloads.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(collectionStrings.removeDownloads) },
                                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            confirmDeleteDownloads = true
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(collectionStrings.removeCollection, color = colors.error) },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = colors.error) },
                                    onClick = {
                                        menuExpanded = false
                                        confirmRemove = true
                                    }
                                )
                            }
                        }
                    }
                }
                item(key = "pc-hero") {
                    PersonalCollectionHero(collection, syncing, strings.formatTrackCount(tracks.size.takeIf { it > 0 } ?: collection.trackCount), collectionStrings)
                }
                if (collection.usesLocalNetwork && !localNetwork.granted) {
                    item(key = "pc-local-network") {
                        Surface(color = colors.tertiaryContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.Lan, contentDescription = null, tint = colors.onTertiaryContainer)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(collectionStrings.localNetworkTitle, style = MaterialTheme.typography.titleSmall, color = colors.onTertiaryContainer)
                                    Text(collectionStrings.localNetworkDetail, style = MaterialTheme.typography.bodySmall, color = colors.onTertiaryContainer)
                                }
                                TextButton(onClick = { localNetwork.ensure { } }) { Text(collectionStrings.allow) }
                            }
                        }
                    }
                }
                if (collection.lastSyncError != null && !syncing) {
                    item(key = "pc-sync-error") {
                        Surface(color = colors.errorContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = colors.onErrorContainer)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(collectionStrings.savedCopy, style = MaterialTheme.typography.titleSmall, color = colors.onErrorContainer)
                                    Text(
                                        collectionStrings.error(collection.lastSyncError),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onErrorContainer
                                    )
                                }
                                if (collection.refreshable) {
                                    TextButton(
                                        onClick = { controller.refresh(collection.id) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = colors.onErrorContainer)
                                    ) { Text(collectionStrings.refresh, fontWeight = FontWeight.SemiBold) }
                                }
                            }
                        }
                    }
                }
                item(key = "pc-actions") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { tracks.firstOrNull()?.let { play(tracks, it) } },
                            enabled = tracks.isNotEmpty() && !selection.isActive,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            ActionLabel(strings.play)
                        }
                        FilledTonalButton(
                            onClick = {
                                val shuffled = tracks.shuffled()
                                shuffled.firstOrNull()?.let { play(shuffled, it) }
                            },
                            enabled = tracks.isNotEmpty() && !selection.isActive,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp)
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            ActionLabel(strings.shuffle)
                        }
                        val allOffline = tracks.isNotEmpty() && downloadedIds.size >= tracks.size
                        FilledTonalIconButton(
                            onClick = {
                                val pending = tracks.filterNot { it.id in downloadedIds }
                                if (pending.isNotEmpty()) viewModel.exportTracks(pending, collection.name)
                            },
                            enabled = tracks.isNotEmpty() && !allOffline && !selection.isActive,
                            shapes = IconButtonDefaults.shapes(),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                if (allOffline) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                                contentDescription = collectionStrings.downloadAll
                            )
                        }
                    }
                }
                if (downloadedIds.isNotEmpty() || activeDownloads > 0) {
                    item(key = "pc-offline") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "${collectionStrings.offlineCount(downloadedIds.size)} / ${tracks.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.onSurfaceVariant
                            )
                            if (activeDownloads > 0 && tracks.isNotEmpty()) {
                                LevyraWavyProgress(
                                    progress = downloadedIds.size.toFloat() / tracks.size.toFloat(),
                                    color = colors.primary,
                                    trackColor = colors.surfaceContainerHighest,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                if (tracks.isEmpty()) {
                    item(key = "pc-empty") { LibraryEmpty(Icons.Rounded.LibraryMusic, collectionStrings.emptyCollection) }
                } else {
                    items(tracks, key = { "pc-track-${it.id}" }) { track ->
                        val key = trackSelectionKey(track)
                        val download = if (track.id in downloadedIds) collectionDownloads.firstOrNull { it.trackId == track.id } else null
                        LibraryTrackRow(
                            track = track,
                            selected = selection.isSelected(key),
                            selectionActive = selection.isActive,
                            isCurrent = track.id == state.currentTrack?.id,
                            isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                            isFavorite = track.id in state.favoriteIds,
                            isDownloaded = download != null,
                            downloadProgress = downloadProgressFor(track, state),
                            onClick = { if (selection.isActive) selection.toggle(key) else play(tracks, track) },
                            onLongClick = { selection.start(key) },
                            onFavorite = { viewModel.toggleFavorite(track) },
                            onDownload = { viewModel.exportTrack(track) },
                            onQueue = { viewModel.addToQueue(track) },
                            onAddToPlaylist = { playlistTargets = listOf(track) },
                            onDeleteDownload = download?.let { stored -> { viewModel.deleteDownload(stored) } },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }

            TrackSelectionBar(
                state = selection,
                allVisibleIds = selectableIds,
                actions = TrackSelectionActions(
                    onPlayNext = selectedTracks.takeIf { it.isNotEmpty() }?.let {
                        {
                            viewModel.playTracksNext(selectedTracks)
                            selection.exit()
                        }
                    },
                    onAddToQueue = selectedTracks.takeIf { it.isNotEmpty() }?.let {
                        {
                            viewModel.addTracksToQueue(selectedTracks)
                            selection.exit()
                        }
                    },
                    onAddToPlaylist = selectedTracks.takeIf { it.isNotEmpty() }?.let { { playlistTargets = selectedTracks } },
                    onFavorite = selectedTracks.takeIf { it.isNotEmpty() }?.let {
                        {
                            viewModel.toggleFavorites(selectedTracks)
                            selection.exit()
                        }
                    },
                    onDownload = selectedTracks.takeIf { it.isNotEmpty() }?.let {
                        {
                            viewModel.exportTracks(selectedTracks, collection.name)
                            selection.exit()
                        }
                    }
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 12.dp, end = 12.dp, bottom = if (state.currentTrack != null) 84.dp else 12.dp)
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

    if (playlistTargets.isNotEmpty()) {
        AddTracksToPlaylistDialog(
            tracks = playlistTargets,
            playlists = state.playlists,
            onDismiss = { playlistTargets = emptyList() },
            onAdd = { playlistId ->
                viewModel.addTracksToPlaylist(playlistId, playlistTargets)
                playlistTargets = emptyList()
                selection.exit()
            },
            onCreate = { name ->
                viewModel.createPlaylistWithTracks(name, playlistTargets)
                playlistTargets = emptyList()
                selection.exit()
            }
        )
    }

    if (showSettings) {
        PersonalCollectionSettingsSheet(
            collection = collection,
            collectionStrings = collectionStrings,
            saveLabel = strings.save,
            onDismiss = { showSettings = false },
            onSave = { name, autoRefresh, allowCleartext ->
                controller.updateSettings(collection.id, name, autoRefresh, allowCleartext)
                showSettings = false
            }
        )
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            title = { Text(collectionStrings.removeTitle(collection.name), textAlign = TextAlign.Center) },
            text = { Text(collectionStrings.removeBody) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    controller.delete(collection.id)
                    onClose()
                }) { Text(collectionStrings.removeCollection, color = colors.error) }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text(strings.cancel) } }
        )
    }

    if (confirmDeleteDownloads) {
        AlertDialog(
            onDismissRequest = { confirmDeleteDownloads = false },
            icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
            title = { Text(collectionStrings.removeDownloads) },
            text = { Text(collectionStrings.removeDownloadsBody(collectionDownloads.size)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteDownloads = false
                    val targets: List<DownloadedTrack> = collectionDownloads
                    viewModel.deleteDownloads(targets)
                }) { Text(strings.delete, color = colors.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteDownloads = false }) { Text(strings.cancel) } }
        )
    }
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

package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.state.LocalMusicUiState
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.EmptyState
import com.luc4n3x.levyra.desktop.app.ui.components.LevyraChip
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.components.TrackRow
import com.luc4n3x.levyra.desktop.app.ui.components.tracksTextInputFocus
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.icons.OfflineIcons
import com.luc4n3x.levyra.desktop.app.util.Format
import com.luc4n3x.levyra.desktop.core.localmusic.LocalTrack
import com.luc4n3x.levyra.desktop.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class LocalMusicTab {
    TRACKS,
    ALBUMS,
    ARTISTS
}

@Composable
fun LocalMusicScreen(
    state: LocalMusicUiState,
    actions: TrackActions,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onRescan: (Boolean) -> Unit,
    onForgetMissing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(LocalMusicTab.TRACKS) }
    var query by remember { mutableStateOf("") }
    var selectedAlbum by remember { mutableStateOf("") }
    var selectedArtist by remember { mutableStateOf("") }
    var visible by remember(state.index) { mutableStateOf(state.index.tracks) }
    var queue by remember(state.index) { mutableStateOf<List<Track>>(emptyList()) }

    LaunchedEffect(state.index, query, selectedAlbum, selectedArtist) {
        if (query.isNotBlank()) delay(LOCAL_QUERY_DEBOUNCE_MS)
        val derived = withContext(Dispatchers.Default) {
            when {
                query.isNotBlank() -> state.index.search(query)
                selectedAlbum.isNotEmpty() -> state.index.albumTracks(selectedAlbum)
                selectedArtist.isNotEmpty() -> state.index.artistTracks(selectedArtist)
                else -> state.index.tracks
            }
        }
        val preparedQueue = withContext(Dispatchers.Default) { derived.map(LocalTrack::toTrack) }
        visible = derived
        queue = preparedQueue
    }

    val browsing = query.isBlank() && selectedAlbum.isEmpty() && selectedArtist.isEmpty()

    ScrollableColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = strings.localMusic, style = MaterialTheme.typography.displaySmall)
                Text(
                    text = strings.localMusicSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            LocalMusicToolbar(
                state = state,
                onAddFolder = onAddFolder,
                onRescan = onRescan,
                onForgetMissing = onForgetMissing
            )
        }

        if (state.folders.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.folders.forEach { folder ->
                        LocalFolderRow(
                            path = folder.path,
                            onRemove = { onRemoveFolder(folder.id) }
                        )
                    }
                }
            }
        }

        if (!state.hasFolders) {
            item {
                EmptyState(
                    icon = OfflineIcons.Folder,
                    title = strings.localMusicEmptyTitle,
                    body = strings.localMusicSubtitle
                )
            }
            return@ScrollableColumn
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LocalMusicTab.entries.forEach { entry ->
                    LevyraChip(
                        label = when (entry) {
                            LocalMusicTab.TRACKS -> strings.filterSongs
                            LocalMusicTab.ALBUMS -> strings.filterAlbums
                            LocalMusicTab.ARTISTS -> strings.filterArtists
                        },
                        selected = entry == tab,
                        onClick = {
                            tab = entry
                            selectedAlbum = ""
                            selectedArtist = ""
                        }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = query,
                    onValueChange = { value -> query = value },
                    placeholder = { Text(strings.searchPlaceholder) },
                    singleLine = true,
                    modifier = Modifier.widthIn(max = 320.dp).tracksTextInputFocus()
                )
            }
        }

        when {
            tab == LocalMusicTab.ALBUMS && browsing -> {
                items(state.index.albums.size, key = { state.index.albums[it].key }) { index ->
                    val album = state.index.albums[index]
                    LocalCollectionRow(
                        artworkUrl = album.artworkPath,
                        title = album.title,
                        subtitle = album.albumArtist,
                        detail = Format.duration(album.durationMs),
                        count = album.trackCount,
                        onOpen = { selectedAlbum = album.key },
                        onPlay = {
                            scope.launch {
                                val tracks = withContext(Dispatchers.Default) {
                                    state.index.albumTracks(album.key).map(LocalTrack::toTrack)
                                }
                                if (tracks.isNotEmpty()) actions.onPlay(tracks, 0)
                            }
                        }
                    )
                }
            }

            tab == LocalMusicTab.ARTISTS && browsing -> {
                items(state.index.artists.size, key = { state.index.artists[it].key }) { index ->
                    val artist = state.index.artists[index]
                    LocalCollectionRow(
                        artworkUrl = artist.artworkPath,
                        title = artist.name,
                        subtitle = "",
                        detail = "",
                        count = artist.trackCount,
                        onOpen = { selectedArtist = artist.key },
                        onPlay = {
                            scope.launch {
                                val tracks = withContext(Dispatchers.Default) {
                                    state.index.artistTracks(artist.key).map(LocalTrack::toTrack)
                                }
                                if (tracks.isNotEmpty()) actions.onPlay(tracks, 0)
                            }
                        }
                    )
                }
            }

            visible.isEmpty() -> {
                item {
                    EmptyState(
                        icon = LevyraIcons.Disc,
                        title = strings.searchNoResults,
                        body = strings.localMusicSubtitle
                    )
                }
            }

            else -> {
                if (!browsing && query.isBlank()) {
                    item {
                        TextButton(
                            onClick = {
                                selectedAlbum = ""
                                selectedArtist = ""
                            }
                        ) {
                            Icon(
                                imageVector = LevyraIcons.ChevronLeft,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.onboardingBack)
                        }
                    }
                }
                items(visible.size, key = { visible[it].id }) { index ->
                    val local = visible[index]
                    val track = queue.getOrNull(index)?.takeIf {
                        it.id == com.luc4n3x.levyra.desktop.core.localmusic.LocalMusicIdentity.trackId(local.id)
                    } ?: local.toTrack()
                    TrackRow(
                        track = track,
                        isCurrent = track.id == actions.currentTrackId,
                        isFavorite = actions.isFavorite(track),
                        onPlay = {
                            scope.launch {
                                val playQueue = if (queue.size == visible.size) {
                                    queue
                                } else {
                                    withContext(Dispatchers.Default) { visible.map(LocalTrack::toTrack) }
                                }
                                if (index in playQueue.indices) actions.onPlay(playQueue, index)
                            }
                        },
                        onPlayNext = { actions.onPlayNext(track) },
                        onEnqueue = { actions.onEnqueue(track) },
                        onToggleFavorite = { actions.onToggleFavorite(track) },
                        onAddToPlaylist = { actions.onAddToPlaylist(track) },
                        position = if (local.trackNumber > 0) local.trackNumber else index + 1
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun LocalMusicToolbar(
    state: LocalMusicUiState,
    onAddFolder: () -> Unit,
    onRescan: (Boolean) -> Unit,
    onForgetMissing: () -> Unit
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onAddFolder, enabled = !state.scanning) {
                Icon(
                    imageVector = LevyraIcons.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.localMusicAddFolder)
            }
            OutlinedButton(onClick = { onRescan(false) }, enabled = state.hasFolders && !state.scanning) {
                Text(strings.localMusicScan)
            }
            OutlinedButton(onClick = { onRescan(true) }, enabled = state.hasFolders && !state.scanning) {
                Text(strings.localMusicDeepScan)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${state.trackCount} · ${state.index.albums.size} · ${state.index.artists.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (state.scanning) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = scanStatusLabel(strings.localMusicScanning, state),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { scanFraction(state) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (state.unavailableCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.localMusicMissingFiles}: ${state.unavailableCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onForgetMissing) {
                    Text(strings.localMusicForgetMissing)
                }
            }
        }
    }
}

private fun scanFraction(state: LocalMusicUiState): Float {
    val total = state.progress.total
    if (total <= 0) return 0f
    return (state.progress.processed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

private fun scanStatusLabel(prefix: String, state: LocalMusicUiState): String {
    val progress = state.progress
    if (progress.total <= 0) return prefix
    val directory = progress.currentDirectory
    val counter = "${progress.processed} / ${progress.total}"
    return if (directory.isBlank()) "$prefix $counter" else "$prefix $counter · $directory"
}

@Composable
private fun LocalFolderRow(path: String, onRemove: () -> Unit) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = OfflineIcons.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = path,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = LevyraIcons.Trash,
                contentDescription = strings.localMusicRemoveFolder,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun LocalCollectionRow(
    artworkUrl: String,
    title: String,
    subtitle: String,
    detail: String,
    count: Int,
    onOpen: () -> Unit,
    onPlay: () -> Unit
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Artwork(url = artworkUrl, modifier = Modifier.size(48.dp), cornerRadius = 10.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val supporting = listOf(subtitle, detail, "$count").filter { it.isNotBlank() }
            Text(
                text = supporting.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onPlay) {
            Icon(
                imageVector = LevyraIcons.Play,
                contentDescription = strings.playbackPlay,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private const val LOCAL_QUERY_DEBOUNCE_MS = 140L

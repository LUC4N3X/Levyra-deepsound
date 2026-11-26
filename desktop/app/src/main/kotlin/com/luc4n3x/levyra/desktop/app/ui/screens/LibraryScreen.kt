package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.EmptyState
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.components.TrackRow
import com.luc4n3x.levyra.desktop.app.ui.components.tracksTextInputFocus
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.icons.OfflineIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.LocalAccentColor
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.storage.DownloadData
import com.luc4n3x.levyra.desktop.core.storage.DownloadRecord
import com.luc4n3x.levyra.desktop.core.storage.DownloadStatus
import com.luc4n3x.levyra.desktop.core.storage.LibraryData
import com.luc4n3x.levyra.desktop.core.storage.LocalPlaylist
import java.util.Locale
import com.luc4n3x.levyra.ui.i18n.LevyraStrings as SharedStrings

@Composable
fun LibraryScreen(
    library: LibraryData,
    downloads: DownloadData,
    actions: TrackActions,
    onOpenPlaylist: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onImportUrl: (String) -> Unit,
    onImportPlaylist: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenDownloadsFolder: () -> Unit,
    onCancelDownload: (String) -> Unit,
    onRetryDownload: (String) -> Unit,
    onDeleteDownload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val shared = remember(strings.languageCode) { SharedStrings.forCode(strings.languageCode) }
    var selectedTab by remember { mutableStateOf<LibraryTab?>(null) }
    var query by remember { mutableStateOf("") }
    val history = remember(library.history) { library.history.sortedByDescending { it.playedAt } }
    val historyTracks = remember(history) { history.map { it.track } }
    val recent = remember(historyTracks) { historyTracks.distinctBy { it.id } }
    val mostPlayed = remember(history) {
        history.groupBy { it.track.id }.values.sortedByDescending { it.size }.map { it.first().track }
    }
    val pulse = remember(library.history) { libraryPulse(library.history) }
    val orderedDownloads = remember(downloads.records) { downloads.records.sortedByDescending { it.updatedAt } }
    val completedCount = orderedDownloads.count { it.status == DownloadStatus.COMPLETED }
    val searchResults = remember(query, library, orderedDownloads) {
        val needle = query.trim()
        if (needle.isEmpty()) {
            emptyList()
        } else {
            (library.favorites + recent + library.playlists.flatMap { it.tracks } +
                orderedDownloads.filter { it.isPlayable }.map { it.playableTrack() })
                .distinctBy { it.id }
                .filter { track ->
                    track.title.contains(needle, ignoreCase = true) ||
                        track.artist.contains(needle, ignoreCase = true) ||
                        track.album.contains(needle, ignoreCase = true)
                }
        }
    }

    ScrollableColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        item(key = "library-header") {
            LibraryHeader(
                title = selectedTab?.label(shared) ?: strings.libraryTitle,
                subtitle = when (selectedTab) {
                    null -> ""
                    LibraryTab.PLAYLISTS -> "${library.playlists.size} ${shared.playlistsPlain}"
                    LibraryTab.FAVORITES -> shared.formatTrackCount(library.favorites.size)
                    LibraryTab.DOWNLOADS -> shared.formatTrackCount(completedCount)
                    LibraryTab.HISTORY -> shared.formatTrackCount(history.size)
                    LibraryTab.MOST_PLAYED -> shared.formatTrackCount(mostPlayed.size)
                },
                backLabel = shared.back,
                onBack = { selectedTab = null }.takeIf { selectedTab != null }
            ) {
                when (selectedTab) {
                    LibraryTab.PLAYLISTS -> OutlinedButton(onClick = onImportPlaylist) { Text(strings.playlistImport) }
                    LibraryTab.DOWNLOADS -> OutlinedButton(onClick = onOpenDownloadsFolder) {
                        Icon(OfflineIcons.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.downloadOpenFolder)
                    }
                    LibraryTab.HISTORY -> if (history.isNotEmpty()) {
                        TextButton(onClick = onClearHistory) { Text(strings.historyClear) }
                    }
                    LibraryTab.FAVORITES -> if (library.favorites.isNotEmpty()) {
                        Button(onClick = { actions.onPlay(library.favorites, 0) }) { Text(strings.playAll) }
                    }
                    LibraryTab.MOST_PLAYED -> if (mostPlayed.isNotEmpty()) {
                        Button(onClick = { actions.onPlay(mostPlayed, 0) }) { Text(strings.playAll) }
                    }
                    null -> Unit
                }
            }
        }

        item(key = "library-search") {
            LibrarySearchField(
                query = query,
                placeholder = strings.searchPlaceholder,
                clearLabel = shared.clear,
                onQueryChange = { query = it },
                modifier = Modifier.padding(top = 18.dp, bottom = 16.dp)
            )
        }

        if (query.isNotBlank()) {
            if (searchResults.isEmpty()) {
                item(key = "library-search-empty") {
                    EmptyState(icon = LevyraIcons.Search, title = strings.searchNoResults)
                }
            } else {
                trackItems("search", searchResults, actions)
            }
            item(key = "library-bottom") { Spacer(Modifier.height(16.dp)) }
            return@ScrollableColumn
        }

        when (selectedTab) {
            null -> {
                item(key = "library-listening") {
                    LibraryListeningBlock(
                        favorites = library.favorites,
                        recent = recent,
                        mostPlayed = mostPlayed,
                        shared = shared,
                        onOpenFavorites = { selectedTab = LibraryTab.FAVORITES },
                        onPlayFavorites = { if (library.favorites.isNotEmpty()) actions.onPlay(library.favorites, 0) },
                        onOpenRecent = { selectedTab = LibraryTab.HISTORY },
                        onOpenMostPlayed = { selectedTab = LibraryTab.MOST_PLAYED }
                    )
                }
                if (recent.isNotEmpty()) {
                    item(key = "library-history-shelf") {
                        val entries = remember(recent, actions) {
                            recent.take(LibraryShelfLimit).mapIndexed { index, track ->
                                LibraryShelfEntry(track.id, track.title, track.displaySubtitle, track.artworkUrl) {
                                    actions.onPlay(recent, index)
                                }
                            }
                        }
                        Box(Modifier.padding(top = 28.dp)) {
                            LibraryShelf(shared.listeningHistory, entries, LevyraIcons.Disc) { selectedTab = LibraryTab.HISTORY }
                        }
                    }
                }
                if (library.playlists.isNotEmpty()) {
                    item(key = "library-playlist-shelf") {
                        val entries = remember(library.playlists, shared) {
                            library.playlists.take(LibraryShelfLimit).map { playlist ->
                                LibraryShelfEntry(
                                    "playlist:${playlist.id}",
                                    playlist.name,
                                    shared.formatTrackCount(playlist.tracks.size),
                                    playlist.artworkUrl
                                ) { onOpenPlaylist(playlist.id) }
                            }
                        }
                        Box(Modifier.padding(top = 28.dp)) {
                            LibraryShelf(shared.playlists, entries, LevyraIcons.Playlist) { selectedTab = LibraryTab.PLAYLISTS }
                        }
                    }
                }
                item(key = "library-categories") {
                    val categories = buildList {
                        if (library.playlists.isEmpty()) {
                            add(LibraryCategoryEntry("playlists", strings.libraryPlaylists, strings.libraryEmptyPlaylists, LevyraIcons.Playlist) { selectedTab = LibraryTab.PLAYLISTS })
                        }
                        add(LibraryCategoryEntry("offline", shared.offline, shared.formatTrackCount(completedCount), OfflineIcons.Download) { selectedTab = LibraryTab.DOWNLOADS })
                        add(LibraryCategoryEntry("history", shared.listeningHistory, shared.formatTrackCount(history.size), OfflineIcons.History) { selectedTab = LibraryTab.HISTORY })
                        add(LibraryCategoryEntry("import", strings.playlistImport, "", LevyraIcons.Add, onImportPlaylist))
                        add(LibraryCategoryEntry("folder", strings.downloadOpenFolder, "", OfflineIcons.Folder, onOpenDownloadsFolder))
                    }
                    Box(Modifier.padding(top = 24.dp)) { LibraryCategoryGrid(categories) }
                }
                item(key = "library-pulse") {
                    Column(Modifier.padding(top = 36.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        LibrarySectionTitle(shared.pulseTitle, shared.pulseSubtitle)
                        LibraryPulseCard(pulse, shared) { selectedTab = LibraryTab.HISTORY }
                    }
                }
                item(key = "library-tools") {
                    Box(Modifier.padding(top = 24.dp)) {
                        LibraryToolsCard(onCreatePlaylist = onCreatePlaylist, onImportUrl = onImportUrl)
                    }
                }
            }

            LibraryTab.PLAYLISTS -> {
                item(key = "library-playlist-tools") {
                    Box(Modifier.padding(bottom = 14.dp)) {
                        LibraryToolsCard(onCreatePlaylist = onCreatePlaylist, onImportUrl = onImportUrl)
                    }
                }
                if (library.playlists.isEmpty()) {
                    item(key = "library-playlists-empty") {
                        EmptyState(icon = LevyraIcons.Playlist, title = strings.libraryEmptyPlaylists, body = strings.importPlaylistHint)
                    }
                } else {
                    items(library.playlists.size, key = { library.playlists[it].id }) { index ->
                        val playlist = library.playlists[index]
                        PlaylistLibraryRow(
                            playlist = playlist,
                            index = index,
                            count = library.playlists.size,
                            shared = shared,
                            onOpen = { onOpenPlaylist(playlist.id) },
                            onPlay = {
                                if (playlist.tracks.isNotEmpty()) actions.onPlay(playlist.tracks, 0) else onOpenPlaylist(playlist.id)
                            }
                        )
                    }
                }
            }

            LibraryTab.FAVORITES -> if (library.favorites.isEmpty()) {
                item(key = "library-favorites-empty") {
                    EmptyState(icon = LevyraIcons.Heart, title = strings.libraryEmptyFavorites, body = shared.tapHeartToAdd)
                }
            } else {
                trackItems("favorites", library.favorites, actions)
            }

            LibraryTab.MOST_PLAYED -> if (mostPlayed.isEmpty()) {
                item(key = "library-plays-empty") {
                    EmptyState(icon = LevyraIcons.Refresh, title = strings.libraryEmptyHistory, body = strings.homeContinueListening)
                }
            } else {
                trackItems("plays", mostPlayed, actions)
            }

            LibraryTab.DOWNLOADS -> if (orderedDownloads.isEmpty()) {
                item(key = "library-downloads-empty") {
                    EmptyState(icon = OfflineIcons.Download, title = strings.libraryEmptyDownloads, body = strings.downloadAction)
                }
            } else {
                items(orderedDownloads.size, key = { orderedDownloads[it].id }) { index ->
                    val record = orderedDownloads[index]
                    DownloadLibraryRow(
                        record = record,
                        shape = LibraryShapes.grouped(index, 0, orderedDownloads.size, 1),
                        isCurrent = record.track.id == actions.currentTrackId,
                        onPlay = { if (record.isPlayable) actions.onPlay(listOf(record.playableTrack()), 0) },
                        onCancel = { onCancelDownload(record.id) },
                        onRetry = { onRetryDownload(record.id) },
                        onDelete = { onDeleteDownload(record.id) }
                    )
                }
            }

            LibraryTab.HISTORY -> if (history.isEmpty()) {
                item(key = "library-history-empty") {
                    EmptyState(icon = OfflineIcons.History, title = strings.libraryEmptyHistory, body = strings.homeContinueListening)
                }
            } else {
                items(history.size, key = { "${history[it].track.id}-${history[it].playedAt}" }) { index ->
                    val track = history[index].track
                    TrackRow(
                        track = track,
                        isCurrent = track.id == actions.currentTrackId,
                        isFavorite = actions.isFavorite(track),
                        onPlay = { actions.onPlay(historyTracks, index) },
                        onPlayNext = { actions.onPlayNext(track) },
                        onEnqueue = { actions.onEnqueue(track) },
                        onToggleFavorite = { actions.onToggleFavorite(track) },
                        onAddToPlaylist = { actions.onAddToPlaylist(track) },
                        position = index + 1
                    )
                }
            }
        }

        item(key = "library-bottom") { Spacer(Modifier.height(16.dp)) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.trackItems(prefix: String, tracks: List<Track>, actions: TrackActions) {
    items(tracks.size, key = { "$prefix:${tracks[it].id}" }) { index ->
        val track = tracks[index]
        TrackRow(
            track = track,
            isCurrent = track.id == actions.currentTrackId,
            isFavorite = actions.isFavorite(track),
            onPlay = { actions.onPlay(tracks, index) },
            onPlayNext = { actions.onPlayNext(track) },
            onEnqueue = { actions.onEnqueue(track) },
            onToggleFavorite = { actions.onToggleFavorite(track) },
            onAddToPlaylist = { actions.onAddToPlaylist(track) },
            position = index + 1
        )
    }
}

@Composable
private fun LibraryHeader(
    title: String,
    subtitle: String,
    backLabel: String,
    onBack: (() -> Unit)?,
    trailing: @Composable () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (onBack != null) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(role = Role.Button, onClickLabel = backLabel, onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(LevyraIcons.ChevronLeft, contentDescription = backLabel, modifier = Modifier.size(22.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = if (onBack == null) MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.SemiBold)
                else MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

@Composable
private fun LibrarySearchField(
    query: String,
    placeholder: String,
    clearLabel: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).tracksTextInputFocus(),
        singleLine = true,
        shape = CircleShape,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface, fontWeight = FontWeight.Medium),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceContainerHigh,
            unfocusedContainerColor = colors.surfaceContainer,
            focusedBorderColor = colors.primary.copy(alpha = 0.42f),
            unfocusedBorderColor = Color.Transparent,
            cursorColor = colors.primary,
            focusedLeadingIconColor = colors.primary,
            unfocusedLeadingIconColor = colors.onSurfaceVariant
        ),
        placeholder = { Text(placeholder, color = colors.onSurfaceVariant, maxLines = 1) },
        leadingIcon = { Icon(LevyraIcons.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(LevyraIcons.Close, contentDescription = clearLabel, modifier = Modifier.size(18.dp))
                }
            }
        } else {
            null
        }
    )
}

@Composable
private fun PlaylistLibraryRow(
    playlist: LocalPlaylist,
    index: Int,
    count: Int,
    shared: SharedStrings,
    onOpen: () -> Unit,
    onPlay: () -> Unit
) {
    val strings = LocalStrings.current
    val tone = LibraryTones.Sky
    Row(
        Modifier
            .fillMaxWidth()
            .clip(LibraryShapes.grouped(index, 0, count, 1))
            .background(librarySurface())
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Artwork(url = playlist.artworkUrl, modifier = Modifier.size(72.dp), cornerRadius = 12.dp, iconSize = 26.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(playlist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(shared.formatTrackCount(playlist.tracks.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(tone.accent)
                .clickable(role = Role.Button, onClickLabel = strings.playAll, onClick = onPlay),
            contentAlignment = Alignment.Center
        ) {
            Icon(LevyraIcons.Play, contentDescription = strings.playAll, tint = tone.onAccent, modifier = Modifier.size(19.dp))
        }
        Icon(
            LevyraIcons.ChevronLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 180f }
        )
    }
}

@Composable
private fun DownloadLibraryRow(
    record: DownloadRecord,
    shape: androidx.compose.ui.graphics.Shape,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalStrings.current
    val accent = LocalAccentColor.current
    val statusLabel = when (record.status) {
        DownloadStatus.QUEUED -> strings.downloadQueued
        DownloadStatus.RESOLVING -> strings.downloadResolving
        DownloadStatus.DOWNLOADING -> strings.downloadDownloading
        DownloadStatus.COMPLETED -> strings.downloadCompleted
        DownloadStatus.FAILED -> strings.downloadFailed
        DownloadStatus.CANCELLED -> strings.downloadCancel
    }
    val statusColor = when (record.status) {
        DownloadStatus.COMPLETED -> accent
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else librarySurface(),
        border = if (isCurrent) BorderStroke(1.dp, accent.copy(alpha = 0.6f)) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Artwork(url = record.track.artworkUrl, modifier = Modifier.size(72.dp), cornerRadius = 12.dp, iconSize = 26.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = record.track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = record.track.displaySubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = statusLabel, style = MaterialTheme.typography.labelMedium, color = statusColor)
                    Text(
                        text = formatBytes(record.bytesDownloaded, record.totalBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (record.mediaLabel.isNotBlank()) {
                        Text(
                            text = record.mediaLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (record.status == DownloadStatus.DOWNLOADING || record.status == DownloadStatus.RESOLVING) {
                    LinearProgressIndicator(
                        progress = { record.progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                    )
                }
                if (record.status == DownloadStatus.FAILED && record.error.isNotBlank()) {
                    Text(
                        text = record.error,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            when (record.status) {
                DownloadStatus.COMPLETED -> {
                    Button(onClick = onPlay) {
                        Icon(imageVector = LevyraIcons.Play, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.playAll)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = LevyraIcons.Trash, contentDescription = strings.downloadRemove, modifier = Modifier.size(19.dp))
                    }
                }

                DownloadStatus.QUEUED,
                DownloadStatus.RESOLVING,
                DownloadStatus.DOWNLOADING -> OutlinedButton(onClick = onCancel) {
                    Text(strings.downloadCancel)
                }

                DownloadStatus.FAILED,
                DownloadStatus.CANCELLED -> {
                    OutlinedButton(onClick = onRetry) {
                        Icon(imageVector = LevyraIcons.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.downloadRetry)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = LevyraIcons.Trash, contentDescription = strings.downloadRemove, modifier = Modifier.size(19.dp))
                    }
                }
            }
        }
    }
}

private fun LibraryTab.label(shared: SharedStrings): String = when (this) {
    LibraryTab.PLAYLISTS -> shared.playlists
    LibraryTab.FAVORITES -> shared.favoritesPlain
    LibraryTab.DOWNLOADS -> shared.offline
    LibraryTab.HISTORY -> shared.listeningHistory
    LibraryTab.MOST_PLAYED -> shared.pulsePlays
}

private fun formatBytes(downloaded: Long, total: Long): String {
    val current = formatByteValue(downloaded)
    return if (total > 0L) "$current / ${formatByteValue(total)}" else current
}

private fun formatByteValue(value: Long): String {
    val safe = value.coerceAtLeast(0L).toDouble()
    return when {
        safe >= 1024.0 * 1024.0 * 1024.0 -> String.format(Locale.ROOT, "%.1f GB", safe / (1024.0 * 1024.0 * 1024.0))
        safe >= 1024.0 * 1024.0 -> String.format(Locale.ROOT, "%.1f MB", safe / (1024.0 * 1024.0))
        safe >= 1024.0 -> String.format(Locale.ROOT, "%.1f KB", safe / 1024.0)
        else -> "${safe.toLong()} B"
    }
}

private const val LibraryShelfLimit = 12

private enum class LibraryTab {
    PLAYLISTS,
    FAVORITES,
    DOWNLOADS,
    HISTORY,
    MOST_PLAYED
}

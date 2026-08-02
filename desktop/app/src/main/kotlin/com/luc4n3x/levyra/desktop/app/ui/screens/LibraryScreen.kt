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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.EmptyState
import com.luc4n3x.levyra.desktop.app.ui.components.LevyraChip
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.components.TrackRow
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.icons.OfflineIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraBrand
import com.luc4n3x.levyra.desktop.app.ui.theme.LocalAccentColor
import com.luc4n3x.levyra.desktop.core.storage.DownloadData
import com.luc4n3x.levyra.desktop.core.storage.DownloadRecord
import com.luc4n3x.levyra.desktop.core.storage.DownloadStatus
import com.luc4n3x.levyra.desktop.core.storage.LibraryData
import com.luc4n3x.levyra.desktop.core.storage.LocalPlaylist
import java.util.Locale

@Composable
fun LibraryScreen(
    library: LibraryData,
    downloads: DownloadData,
    actions: TrackActions,
    onOpenPlaylist: (String) -> Unit,
    onClearHistory: () -> Unit,
    onOpenDownloadsFolder: () -> Unit,
    onCancelDownload: (String) -> Unit,
    onRetryDownload: (String) -> Unit,
    onDeleteDownload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val accent = LocalAccentColor.current
    var selectedTab by remember { mutableStateOf(LibraryTab.PLAYLISTS) }
    val history = remember(library.history) {
        library.history.sortedByDescending { it.playedAt }
    }
    val orderedDownloads = remember(downloads.records) {
        downloads.records.sortedByDescending { it.updatedAt }
    }

    ScrollableColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LibraryHero(
                title = strings.libraryTitle,
                subtitle = strings.librarySubtitle,
                accent = accent,
                playlists = library.playlists.size,
                favorites = library.favorites.size,
                downloads = orderedDownloads.count { it.status == DownloadStatus.COMPLETED },
                history = history.size
            )
        }

        item {
            LibraryMetrics(
                selected = selectedTab,
                playlistCount = library.playlists.size,
                favoriteCount = library.favorites.size,
                downloadCount = orderedDownloads.count { it.status == DownloadStatus.COMPLETED },
                historyCount = history.size,
                onSelect = { selectedTab = it }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LibraryTab.entries.forEach { tab ->
                    LevyraChip(
                        label = tab.label(),
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (selectedTab == LibraryTab.DOWNLOADS) {
                    OutlinedButton(onClick = onOpenDownloadsFolder) {
                        Icon(
                            imageVector = OfflineIcons.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.downloadOpenFolder)
                    }
                }
                if (selectedTab == LibraryTab.HISTORY && history.isNotEmpty()) {
                    TextButton(onClick = onClearHistory) {
                        Text(strings.historyClear)
                    }
                }
            }
        }

        when (selectedTab) {
            LibraryTab.PLAYLISTS -> {
                if (library.playlists.isEmpty()) {
                    item {
                        EmptyState(
                            icon = LevyraIcons.Playlist,
                            title = strings.libraryEmptyPlaylists,
                            body = strings.importPlaylistHint
                        )
                    }
                } else {
                    items(library.playlists.size, key = { library.playlists[it].id }) { index ->
                        val playlist = library.playlists[index]
                        PlaylistLibraryRow(
                            playlist = playlist,
                            onOpen = { onOpenPlaylist(playlist.id) },
                            onPlay = {
                                if (playlist.tracks.isNotEmpty()) {
                                    actions.onPlay(playlist.tracks, 0)
                                } else {
                                    onOpenPlaylist(playlist.id)
                                }
                            }
                        )
                    }
                }
            }

            LibraryTab.FAVORITES -> {
                if (library.favorites.isEmpty()) {
                    item {
                        EmptyState(
                            icon = LevyraIcons.Heart,
                            title = strings.libraryEmptyFavorites,
                            body = strings.homeFavoritesBody
                        )
                    }
                } else {
                    items(library.favorites.size, key = { library.favorites[it].id }) { index ->
                        val track = library.favorites[index]
                        TrackRow(
                            track = track,
                            isCurrent = track.id == actions.currentTrackId,
                            isFavorite = true,
                            onPlay = { actions.onPlay(library.favorites, index) },
                            onPlayNext = { actions.onPlayNext(track) },
                            onEnqueue = { actions.onEnqueue(track) },
                            onToggleFavorite = { actions.onToggleFavorite(track) },
                            onAddToPlaylist = { actions.onAddToPlaylist(track) },
                            position = index + 1
                        )
                    }
                }
            }

            LibraryTab.DOWNLOADS -> {
                if (orderedDownloads.isEmpty()) {
                    item {
                        EmptyState(
                            icon = OfflineIcons.Download,
                            title = strings.libraryEmptyDownloads,
                            body = strings.downloadAction
                        )
                    }
                } else {
                    items(orderedDownloads.size, key = { orderedDownloads[it].id }) { index ->
                        val record = orderedDownloads[index]
                        DownloadLibraryRow(
                            record = record,
                            isCurrent = record.track.id == actions.currentTrackId,
                            onPlay = {
                                if (record.isPlayable) {
                                    actions.onPlay(listOf(record.playableTrack()), 0)
                                }
                            },
                            onCancel = { onCancelDownload(record.id) },
                            onRetry = { onRetryDownload(record.id) },
                            onDelete = { onDeleteDownload(record.id) }
                        )
                    }
                }
            }

            LibraryTab.HISTORY -> {
                if (history.isEmpty()) {
                    item {
                        EmptyState(
                            icon = OfflineIcons.History,
                            title = strings.libraryEmptyHistory,
                            body = strings.homeContinueListening
                        )
                    }
                } else {
                    items(history.size, key = { "${history[it].track.id}-${history[it].playedAt}" }) { index ->
                        val entry = history[index]
                        val queue = history.map { it.track }
                        val track = entry.track
                        TrackRow(
                            track = track,
                            isCurrent = track.id == actions.currentTrackId,
                            isFavorite = actions.isFavorite(track),
                            onPlay = { actions.onPlay(queue, index) },
                            onPlayNext = { actions.onPlayNext(track) },
                            onEnqueue = { actions.onEnqueue(track) },
                            onToggleFavorite = { actions.onToggleFavorite(track) },
                            onAddToPlaylist = { actions.onAddToPlaylist(track) },
                            position = index + 1
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun LibraryHero(
    title: String,
    subtitle: String,
    accent: Color,
    playlists: Int,
    favorites: Int,
    downloads: Int,
    history: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.24f),
                            LevyraBrand.violet.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                )
                .padding(28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Surface(
                modifier = Modifier.size(76.dp),
                shape = RoundedCornerShape(22.dp),
                color = accent.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = OfflineIcons.Library,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = listOf(playlists, favorites, downloads, history).sum().toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

@Composable
private fun LibraryMetrics(
    selected: LibraryTab,
    playlistCount: Int,
    favoriteCount: Int,
    downloadCount: Int,
    historyCount: Int,
    onSelect: (LibraryTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LibraryMetricCard(
            tab = LibraryTab.PLAYLISTS,
            icon = LevyraIcons.Playlist,
            count = playlistCount,
            selected = selected == LibraryTab.PLAYLISTS,
            onClick = { onSelect(LibraryTab.PLAYLISTS) },
            modifier = Modifier.weight(1f)
        )
        LibraryMetricCard(
            tab = LibraryTab.FAVORITES,
            icon = LevyraIcons.HeartFilled,
            count = favoriteCount,
            selected = selected == LibraryTab.FAVORITES,
            onClick = { onSelect(LibraryTab.FAVORITES) },
            modifier = Modifier.weight(1f)
        )
        LibraryMetricCard(
            tab = LibraryTab.DOWNLOADS,
            icon = OfflineIcons.Download,
            count = downloadCount,
            selected = selected == LibraryTab.DOWNLOADS,
            onClick = { onSelect(LibraryTab.DOWNLOADS) },
            modifier = Modifier.weight(1f)
        )
        LibraryMetricCard(
            tab = LibraryTab.HISTORY,
            icon = OfflineIcons.History,
            count = historyCount,
            selected = selected == LibraryTab.HISTORY,
            onClick = { onSelect(LibraryTab.HISTORY) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LibraryMetricCard(
    tab: LibraryTab,
    icon: ImageVector,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccentColor.current
    Surface(
        modifier = modifier.height(108.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) accent.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            1.dp,
            if (selected) accent.copy(alpha = 0.52f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(21.dp)
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = tab.label(),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlaylistLibraryRow(
    playlist: LocalPlaylist,
    onOpen: () -> Unit,
    onPlay: () -> Unit
) {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Artwork(
                url = playlist.artworkUrl,
                modifier = Modifier.size(68.dp),
                cornerRadius = 14.dp,
                iconSize = 25.dp
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${playlist.tracks.size} ${strings.playlistTracks}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = LevyraIcons.Play,
                    contentDescription = strings.playAll,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun DownloadLibraryRow(
    record: DownloadRecord,
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
        shape = RoundedCornerShape(18.dp),
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            1.dp,
            if (isCurrent) accent.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Artwork(
                url = record.track.artworkUrl,
                modifier = Modifier.size(68.dp),
                cornerRadius = 14.dp,
                iconSize = 25.dp
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = record.track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = record.track.displaySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor
                    )
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
                        Icon(
                            imageVector = LevyraIcons.Play,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.playAll)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = LevyraIcons.Trash,
                            contentDescription = strings.downloadRemove,
                            modifier = Modifier.size(19.dp)
                        )
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
                        Icon(
                            imageVector = LevyraIcons.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.downloadRetry)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = LevyraIcons.Trash,
                            contentDescription = strings.downloadRemove,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryTab.label(): String {
    val strings = LocalStrings.current
    return when (this) {
        LibraryTab.PLAYLISTS -> strings.libraryPlaylists
        LibraryTab.FAVORITES -> strings.libraryFavorites
        LibraryTab.DOWNLOADS -> strings.libraryDownloads
        LibraryTab.HISTORY -> strings.libraryHistory
    }
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

private enum class LibraryTab {
    PLAYLISTS,
    FAVORITES,
    DOWNLOADS,
    HISTORY
}

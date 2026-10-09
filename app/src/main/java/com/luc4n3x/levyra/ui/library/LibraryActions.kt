package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import com.luc4n3x.levyra.domain.OfflineDownloadStage
import com.luc4n3x.levyra.domain.offlineDownloadStageOf
import com.luc4n3x.levyra.domain.LibrarySort
import com.luc4n3x.levyra.domain.LibrarySortDirection
import com.luc4n3x.levyra.ui.components.LevyraConnectedDefaults
import com.luc4n3x.levyra.ui.components.LevyraConnectedPosition
import com.luc4n3x.levyra.ui.components.LevyraConnectedStyle
import com.luc4n3x.levyra.ui.components.levyraConnectedSurface
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import java.util.Locale
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.platform.LocalContext
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.luc4n3x.levyra.data.LevyraArtworkCache
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.BatchDownload
import com.luc4n3x.levyra.domain.BatchDownloadKind
import com.luc4n3x.levyra.domain.BatchDownloadState
import com.luc4n3x.levyra.domain.OfflineDownloadTask
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.PlaylistCoverMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.components.LevyraRowText
import com.luc4n3x.levyra.ui.components.PlayerGlassIconButton
import com.luc4n3x.levyra.ui.media.ImmersiveTopBarButtonFill
import com.luc4n3x.levyra.ui.media.ImmersiveTopBarButtonSize
import com.luc4n3x.levyra.ui.components.levyraDockSurface
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.i18n.playlistProCopy
import com.luc4n3x.levyra.ui.i18n.speedDialCopy
import com.luc4n3x.levyra.ui.i18n.smartOfflineCopy
import com.luc4n3x.levyra.ui.i18n.updatedLabel
import com.luc4n3x.levyra.ui.i18n.formatLibraryBytes
import com.luc4n3x.levyra.ui.i18n.formatLibraryDuration
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
import com.luc4n3x.levyra.ui.theme.LevyraGlass
import com.luc4n3x.levyra.ui.theme.LevyraGlassBorder
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.viewmodel.LevyraUiState

@Composable
internal fun LibraryOfflineSummary(
    bytes: Long,
    activeCount: Int
) {
    val strings = LocalLevyraStrings.current
    Surface(
        color = LevyraGlass,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, LevyraGlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Storage,
                contentDescription = null,
                tint = LevyraCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    strings.offlineDownloadsPlain,
                    color = LevyraText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOf(
                        strings.formatLibraryBytes(bytes),
                        activeCount.takeIf { it > 0 }?.let { "$it ${strings.activeIndicator}" }.orEmpty()
                    ).filter(String::isNotBlank).joinToString(" · "),
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun LibrarySmartOfflineSummary(
    songCount: Int,
    bytes: Long,
    lastUpdatedAt: Long,
    onRefresh: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = strings.smartOfflineCopy()
    Surface(
        color = LevyraViolet.copy(alpha = 0.14f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, LevyraViolet.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = LevyraViolet,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    copy.title,
                    color = LevyraText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$songCount ${copy.songs} · ${strings.formatLibraryBytes(bytes)}",
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    copy.updatedLabel(lastUpdatedAt),
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = copy.refresh,
                    tint = LevyraCyan
                )
            }
        }
    }
}

@Composable
internal fun LibraryDownloadTaskRow(
    task: OfflineDownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    position: LevyraConnectedPosition = LevyraConnectedPosition.Single,
    style: LevyraConnectedStyle = LevyraConnectedDefaults.style(accent = LevyraCyan)
) {
    val strings = LocalLevyraStrings.current
    val stage = offlineDownloadStageOf(task.state)
    val paused = stage == OfflineDownloadStage.Paused
    val failed = stage == OfflineDownloadStage.Failed
    val stageColor = when (stage) {
        OfflineDownloadStage.Failed -> MaterialTheme.colorScheme.error
        OfflineDownloadStage.Downloading, OfflineDownloadStage.Retrying -> LevyraCyan
        else -> LevyraMuted
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .levyraConnectedSurface(position, style)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(stageColor)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.title,
                        color = LevyraText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        task.artist.ifBlank { strings.localizeDownloadState(task.state) },
                        color = LevyraMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = if (paused || failed) onResume else onPause) {
                    Icon(
                        if (paused || failed) Icons.Rounded.Refresh else Icons.Rounded.Pause,
                        contentDescription = if (paused || failed) strings.resumeDownload else strings.pause,
                        tint = LevyraCyan
                    )
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Rounded.Cancel, contentDescription = strings.cancelDownload, tint = LevyraMuted)
                }
            }
            if (stage.showsProgress) {
                LinearProgressIndicator(
                    progress = { task.progress.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = if (paused) LevyraMuted else LevyraCyan,
                    trackColor = LevyraPanelSoft
                )
            }
            Text(
                if (failed && task.error.isNotBlank()) {
                    task.error
                } else if (stage.showsProgress) {
                    "${strings.localizeDownloadState(task.state)} · ${task.progress.coerceIn(0, 100)}%"
                } else {
                    strings.localizeDownloadState(task.state)
                },
                color = stageColor,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun LibraryBatchDownloadRow(
    batch: BatchDownload,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val failed = batch.state == BatchDownloadState.Failed
    Surface(
        color = LevyraPanel.copy(alpha = 0.84f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (batch.kind == BatchDownloadKind.Playlist) Icons.AutoMirrored.Rounded.PlaylistPlay else Icons.Rounded.Album,
                    contentDescription = null,
                    tint = LevyraViolet,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        batch.title,
                        color = LevyraText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${batch.completed} / ${batch.total}",
                        color = LevyraMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
                if (batch.canRetry) {
                    IconButton(onClick = onRetry) {
                        Icon(Icons.Rounded.Refresh, contentDescription = strings.resumeDownload, tint = LevyraCyan)
                    }
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Rounded.Cancel, contentDescription = strings.cancelDownload, tint = LevyraMuted)
                }
            }
            LinearProgressIndicator(
                progress = { batch.progress.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = if (failed) MaterialTheme.colorScheme.error else LevyraViolet,
                trackColor = LevyraPanelSoft
            )
        }
    }
}

@Composable
internal fun LibrarySelectionBar(
    count: Int,
    canOperateTracks: Boolean,
    canDelete: Boolean,
    onClear: () -> Unit,
    onPlay: () -> Unit,
    onQueue: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit,
    deleteLabel: String? = null,
    onSelectAll: (() -> Unit)? = null,
    allSelected: Boolean = false,
    primaryLabel: String? = null,
    canPlayTracks: Boolean = canOperateTracks,
    canQueueTracks: Boolean = canOperateTracks,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val resolvedDeleteLabel = deleteLabel ?: strings.delete
    val resolvedPrimaryLabel = primaryLabel ?: strings.play
    Surface(
        color = LevyraPanel.copy(alpha = 0.98f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shadowElevation = 14.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Rounded.Close, contentDescription = strings.clear, tint = LevyraText)
                }
                Text(
                    strings.formatTrackCount(count),
                    color = LevyraText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                if (onSelectAll != null) {
                    TextButton(onClick = onSelectAll, enabled = !allSelected) {
                        Text(strings.selectAll, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LibrarySelectionAction(
                    if (primaryLabel == null) Icons.Rounded.PlayArrow else Icons.Rounded.SkipNext,
                    resolvedPrimaryLabel,
                    canPlayTracks,
                    onPlay,
                    LevyraCyan,
                    Modifier.widthIn(min = 72.dp)
                )
                LibrarySelectionAction(Icons.AutoMirrored.Rounded.QueueMusic, strings.queue, canQueueTracks, onQueue, LevyraText, Modifier.widthIn(min = 72.dp))
                LibrarySelectionAction(Icons.AutoMirrored.Rounded.PlaylistAdd, strings.addToPlaylist, canOperateTracks, onAddToPlaylist, LevyraText, Modifier.widthIn(min = 72.dp))
                LibrarySelectionAction(Icons.Rounded.Download, strings.offline, canOperateTracks, onDownload, LevyraText, Modifier.widthIn(min = 72.dp))
                LibrarySelectionAction(Icons.Rounded.Delete, resolvedDeleteLabel, canDelete, onDelete, LevyraPink, Modifier.widthIn(min = 72.dp))
            }
        }
    }
}

@Composable
internal fun LibrarySelectionAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (enabled) tint else LevyraMuted.copy(alpha = 0.35f),
                modifier = Modifier.size(21.dp)
            )
            Text(
                label,
                color = if (enabled) LevyraMuted else LevyraMuted.copy(alpha = 0.35f),
                fontSize = 9.sp,
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun LibraryEmpty(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(color = LevyraGlass, shape = CircleShape) {
            Icon(
                icon,
                contentDescription = null,
                tint = LevyraMuted,
                modifier = Modifier.padding(16.dp).size(26.dp)
            )
        }
        Text(
            title,
            color = LevyraText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        if (!detail.isNullOrBlank()) {
            Text(
                detail,
                color = LevyraMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AddTracksToPlaylistDialog(
    tracks: List<Track>,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onCreate: (String) -> Unit
) {
    val strings = LocalLevyraStrings.current
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.addToPlaylist} · ${strings.formatTrackCount(tracks.size)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (creating) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        label = { Text(strings.playlistName) }
                    )
                } else {
                    TextButton(onClick = { creating = true }, enabled = !submitting) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text(strings.createNewPlaylist)
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            Surface(
                                color = Color.Transparent,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().combinedClickable(
                                    enabled = !submitting,
                                    onClick = {
                                        submitting = true
                                        onAdd(playlist.id)
                                    }
                                )
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = LevyraMuted)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        playlist.name,
                                        color = LevyraText,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                TextButton(
                    onClick = {
                        if (name.isNotBlank() && !submitting) {
                            submitting = true
                            onCreate(name.trim())
                        }
                    },
                    enabled = name.isNotBlank() && !submitting
                ) { Text(strings.create) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.close) } }
    )
}

@Composable
internal fun LibraryNameDialog(
    title: String,
    initialValue: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val strings = LocalLevyraStrings.current
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value.trim()) },
                enabled = value.isNotBlank()
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun PlaylistMosaicTile(
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    if (url.isNotBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(LevyraArtworkCache.small(url))
                .crossfade(120)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(LevyraPanelSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.PlaylistPlay,
                contentDescription = null,
                tint = LevyraMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
internal fun PlaylistCoverArt(
    coverMode: PlaylistCoverMode,
    coverUrl: String,
    tracks: List<Track>,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val previewTracks = remember(tracks) { tracks.take(4) }
    if (coverMode == PlaylistCoverMode.CUSTOM && coverUrl.isNotBlank()) {
        AsyncImage(
            model = coverUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else if (previewTracks.size >= 4) {
        Column(modifier = modifier) {
            Row(modifier = Modifier.weight(1f)) {
                PlaylistMosaicTile(
                    url = previewTracks[0].thumbnailUrl.ifBlank { previewTracks[0].largeThumbnailUrl },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                PlaylistMosaicTile(
                    url = previewTracks[1].thumbnailUrl.ifBlank { previewTracks[1].largeThumbnailUrl },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
            Row(modifier = Modifier.weight(1f)) {
                PlaylistMosaicTile(
                    url = previewTracks[2].thumbnailUrl.ifBlank { previewTracks[2].largeThumbnailUrl },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                PlaylistMosaicTile(
                    url = previewTracks[3].thumbnailUrl.ifBlank { previewTracks[3].largeThumbnailUrl },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    } else if (previewTracks.isNotEmpty()) {
        val primaryCover = previewTracks[0].thumbnailUrl.ifBlank { previewTracks[0].largeThumbnailUrl }
        if (primaryCover.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(LevyraArtworkCache.large(primaryCover))
                    .crossfade(120)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        } else {
            Box(
                modifier = modifier.background(
                    Brush.linearGradient(
                        listOf(
                            LevyraCyan.copy(alpha = 0.35f),
                            LevyraViolet.copy(alpha = 0.35f),
                            LevyraPanelSoft
                        )
                    )
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.PlaylistPlay,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.80f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    } else {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(
                    listOf(
                        LevyraCyan.copy(alpha = 0.35f),
                        LevyraViolet.copy(alpha = 0.35f),
                        LevyraPanelSoft
                    )
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.PlaylistPlay,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.80f),
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
internal fun PlaylistDetailTopActions(
    playlist: Playlist,
    reorderMode: Boolean,
    searchActive: Boolean,
    menuBackground: Color,
    onToggleSearch: () -> Unit,
    onSaveOrder: () -> Unit,
    onRename: () -> Unit,
    onReorder: () -> Unit,
    onChangeCover: () -> Unit,
    onResetCover: () -> Unit,
    onOpenStudio: () -> Unit,
    onEditTags: () -> Unit,
    onToggleHidden: () -> Unit,
    isPinnedToHome: Boolean,
    homePinsFull: Boolean,
    onTogglePinToHome: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val speedDialCopy = remember(strings) { strings.speedDialCopy() }
    var menuExpanded by remember { mutableStateOf(false) }

    if (reorderMode) {
        Surface(
            color = LevyraCyan,
            shape = CircleShape,
            modifier = Modifier
                .heightIn(min = 40.dp)
                .clip(CircleShape)
                .clickable(onClick = onSaveOrder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = LevyraOnAccent, modifier = Modifier.size(18.dp))
                Text(strings.save, color = LevyraOnAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        return
    }
    if (playlist.tracks.isNotEmpty()) {
        PlayerGlassIconButton(
            icon = if (searchActive) Icons.Rounded.Close else Icons.Rounded.Search,
            contentDescription = if (searchActive) strings.close else strings.search,
            onClick = onToggleSearch,
            size = ImmersiveTopBarButtonSize,
            tint = Color.White,
            fill = ImmersiveTopBarButtonFill,
            borderTop = Color.Transparent,
            borderBottom = Color.Transparent
        )
    }
    Box {
        PlayerGlassIconButton(
            icon = Icons.Rounded.MoreVert,
            contentDescription = strings.more,
            onClick = { menuExpanded = true },
            size = ImmersiveTopBarButtonSize,
            tint = Color.White,
            fill = ImmersiveTopBarButtonFill,
            borderTop = Color.Transparent,
            borderBottom = Color.Transparent
        )
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            modifier = Modifier.background(menuBackground)
        ) {
            DropdownMenuItem(
                text = { Text(strings.playlistStudioOpen) },
                leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null) },
                onClick = { menuExpanded = false; onOpenStudio() }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        when {
                            isPinnedToHome -> speedDialCopy.removeFromHome
                            homePinsFull -> speedDialCopy.homeFull
                            else -> speedDialCopy.addToHome
                        }
                    )
                },
                leadingIcon = { Icon(Icons.Rounded.PushPin, null) },
                enabled = isPinnedToHome || !homePinsFull,
                onClick = { menuExpanded = false; onTogglePinToHome() }
            )
            DropdownMenuItem(
                text = { Text(strings.playlistName) },
                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                onClick = { menuExpanded = false; onRename() }
            )
            DropdownMenuItem(
                text = { Text(if (playlist.tags.isEmpty()) strings.playlistTags else strings.editPlaylistTags) },
                leadingIcon = { Icon(Icons.Rounded.LocalOffer, null) },
                onClick = { menuExpanded = false; onEditTags() }
            )
            if (playlist.tracks.size > 1) {
                DropdownMenuItem(
                    text = { Text(strings.dragToReorder) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Sort, null) },
                    onClick = { menuExpanded = false; onReorder() }
                )
            }
            DropdownMenuItem(
                text = { Text(strings.playlistProCopy().changeCover) },
                leadingIcon = { Icon(Icons.Rounded.Photo, null) },
                onClick = { menuExpanded = false; onChangeCover() }
            )
            if (playlist.coverMode == PlaylistCoverMode.CUSTOM) {
                DropdownMenuItem(
                    text = { Text(strings.playlistProCopy().resetAutomaticCover) },
                    leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                    onClick = { menuExpanded = false; onResetCover() }
                )
            }
            DropdownMenuItem(
                text = { Text(if (playlist.hidden) strings.unhidePlaylist else strings.hidePlaylist) },
                leadingIcon = {
                    Icon(if (playlist.hidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, null)
                },
                onClick = { menuExpanded = false; onToggleHidden() }
            )
        }
    }
}

internal fun playlistHeroArtworkUrl(playlist: Playlist): String {
    if (playlist.coverMode == PlaylistCoverMode.CUSTOM && playlist.coverUrl.isNotBlank()) return playlist.coverUrl
    val first = playlist.tracks.firstOrNull() ?: return ""
    return first.largeThumbnailUrl.ifBlank { first.thumbnailUrl }
}

private const val PLAYLIST_REORDER_DRAG_SCALE = 1.012f

internal data class PlaylistReorderRowState(
    val index: Int,
    val count: Int,
    val isDragging: Boolean,
    val dragOffsetY: Float
)

internal data class PlaylistReorderRowActions(
    val onMoveUp: () -> Unit,
    val onMoveDown: () -> Unit,
    val onDragStart: () -> Unit,
    val onDrag: (Float) -> Unit,
    val onDragEnd: () -> Unit
)

internal fun Modifier.playlistReorderDrag(
    trackId: String,
    state: PlaylistReorderRowState,
    actions: PlaylistReorderRowActions,
    onHaptic: () -> Unit
): Modifier = this
    .zIndex(if (state.isDragging) 2f else 0f)
    .graphicsLayer {
        translationY = if (state.isDragging) state.dragOffsetY else 0f
        val scale = if (state.isDragging) PLAYLIST_REORDER_DRAG_SCALE else 1f
        scaleX = scale
        scaleY = scale
    }
    .pointerInput(trackId) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                onHaptic()
                actions.onDragStart()
            },
            onDragEnd = {
                onHaptic()
                actions.onDragEnd()
            },
            onDragCancel = actions.onDragEnd,
            onDrag = { change, amount ->
                change.consume()
                actions.onDrag(amount.y)
            }
        )
    }

@Composable
internal fun PlaylistReorderRow(
    track: Track,
    state: PlaylistReorderRowState,
    actions: PlaylistReorderRowActions,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val haptics = LocalLevyraHaptics.current
    val rowModifier = modifier
        .fillMaxWidth()
        .playlistReorderDrag(track.id, state, actions) {
            haptics.perform(LevyraHapticAction.Reorder)
        }

    Surface(
        color = if (state.isDragging) LevyraPanel.copy(alpha = 0.96f) else LevyraPanel.copy(alpha = 0.82f),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = if (state.isDragging) 12.dp else 0.dp,
        modifier = rowModifier
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${state.index + 1}",
                color = LevyraMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(28.dp)
            )
            LibraryArtwork(
                track.largeThumbnailUrl.ifBlank { track.thumbnailUrl },
                track.title,
                Modifier.size(50.dp),
                RoundedCornerShape(13.dp),
                false
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    color = LevyraText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    track.artist,
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Rounded.DragHandle,
                contentDescription = strings.dragToReorder,
                tint = if (state.isDragging) LevyraCyan else LevyraMuted,
                modifier = Modifier.size(22.dp)
            )
            IconButton(onClick = actions.onMoveUp, enabled = state.index > 0) {
                Icon(
                    Icons.Rounded.ArrowUpward,
                    contentDescription = "${strings.dragToReorder} ↑",
                    tint = if (state.index > 0) LevyraText else LevyraMuted.copy(alpha = 0.3f)
                )
            }
            IconButton(onClick = actions.onMoveDown, enabled = state.index < state.count - 1) {
                Icon(
                    Icons.Rounded.ArrowDownward,
                    contentDescription = "${strings.dragToReorder} ↓",
                    tint = if (state.index < state.count - 1) LevyraText else LevyraMuted.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryNowPlayingDock(
    track: Track,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    onNext: (() -> Unit)? = null,
    nextEnabled: Boolean = true,
    nextLabel: String = ""
) {
    val strings = LocalLevyraStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .levyraDockSurface()
            .combinedClickable(onClick = onOpen)
            .height(64.dp)
            .padding(start = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LibraryArtwork(
            track.largeThumbnailUrl.ifBlank { track.thumbnailUrl },
            track.title,
            Modifier.size(44.dp),
            LevyraCardDesign.ThumbShape,
            false
        )
        Spacer(Modifier.width(12.dp))
        LevyraRowText(
            title = track.title,
            subtitle = track.artist,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onToggle) {
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) strings.pause else strings.play,
                tint = LevyraText,
                modifier = Modifier.size(28.dp)
            )
        }
        if (onNext != null) {
            IconButton(onClick = onNext, enabled = nextEnabled) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = nextLabel.ifBlank { strings.next },
                    tint = if (nextEnabled) LevyraText else LevyraMuted.copy(alpha = 0.4f)
                )
            }
        }
    }
}

internal fun LibraryCategory.libraryLabel(strings: LevyraStrings): String = when (this) {
    LibraryCategory.Overview -> strings.libraryTitle
    LibraryCategory.Playlists -> strings.playlists
    LibraryCategory.Albums -> strings.albumsPlain
    LibraryCategory.Artists -> strings.artists
    LibraryCategory.Songs -> strings.songsPlain
    LibraryCategory.Offline -> strings.offline
    LibraryCategory.Device -> strings.localOnDevice
}

internal fun LibrarySort.libraryLabel(strings: LevyraStrings): String = when (this) {
    LibrarySort.Recent -> strings.recent
    LibrarySort.Title -> strings.song
    LibrarySort.Artist -> strings.artistLabel
    LibrarySort.Album -> strings.albumPlain
    LibrarySort.Duration -> strings.timer
}

internal fun LibrarySort.directionLabel(
    direction: LibrarySortDirection,
    strings: LevyraStrings
): String = when (this) {
    LibrarySort.Recent -> when (direction) {
        LibrarySortDirection.Descending -> strings.librarySortNewestFirst
        LibrarySortDirection.Ascending -> strings.librarySortOldestFirst
    }
    LibrarySort.Duration -> when (direction) {
        LibrarySortDirection.Descending -> strings.librarySortLongestFirst
        LibrarySortDirection.Ascending -> strings.librarySortShortestFirst
    }
    LibrarySort.Title, LibrarySort.Artist, LibrarySort.Album -> when (direction) {
        LibrarySortDirection.Ascending -> strings.librarySortAscending
        LibrarySortDirection.Descending -> strings.librarySortDescending
    }
}

internal fun LibraryAlbum.toAlbumHit(): AlbumHit = AlbumHit(
    title = title,
    artist = artist,
    year = year,
    thumbnailUrl = artworkUrl,
    query = listOf(artist, title).filter(String::isNotBlank).joinToString(" "),
    browseId = browseId,
    explicit = explicit
)

internal fun Set<String>.toggle(value: String): Set<String> = if (value in this) this - value else this + value

internal fun <T> List<T>.move(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

internal fun downloadProgressFor(track: Track, state: LevyraUiState): Int? {
    state.downloadQueue.firstOrNull { task ->
        task.trackId.isNotBlank() &&
            task.trackId == track.id &&
            task.state in setOf("QUEUED", "RUNNING", "RETRYING", "PAUSED")
    }?.let { return it.progress.coerceIn(0, 100) }
    return state.downloadProgressByTrackId[track.id]
}

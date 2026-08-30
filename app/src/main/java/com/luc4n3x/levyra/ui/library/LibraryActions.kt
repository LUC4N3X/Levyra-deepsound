package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import com.luc4n3x.levyra.domain.OfflineDownloadStage
import com.luc4n3x.levyra.domain.offlineDownloadStageOf
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
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
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
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import java.util.Locale
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
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
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.formatLibraryBytes
import com.luc4n3x.levyra.ui.i18n.formatLibraryDuration
import com.luc4n3x.levyra.ui.theme.LevyraCyan
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
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val resolvedDeleteLabel = deleteLabel ?: strings.delete
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
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LibrarySelectionAction(Icons.Rounded.PlayArrow, strings.play, canOperateTracks, onPlay, LevyraCyan, Modifier.weight(1f))
                LibrarySelectionAction(Icons.AutoMirrored.Rounded.QueueMusic, strings.queue, canOperateTracks, onQueue, LevyraText, Modifier.weight(1f))
                LibrarySelectionAction(Icons.AutoMirrored.Rounded.PlaylistAdd, strings.addToPlaylist, canOperateTracks, onAddToPlaylist, LevyraText, Modifier.weight(1f))
                LibrarySelectionAction(Icons.Rounded.Download, strings.offline, canOperateTracks, onDownload, LevyraText, Modifier.weight(1f))
                LibrarySelectionAction(Icons.Rounded.Delete, resolvedDeleteLabel, canDelete, onDelete, LevyraPink, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LibrarySelectionAction(
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
                    TextButton(onClick = { creating = true }) {
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
                                modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onAdd(playlist.id) })
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
                    onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                    enabled = name.isNotBlank()
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
internal fun PlaylistDetailHeader(
    playlist: Playlist,
    durationMs: Long,
    reorderMode: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onDownload: () -> Unit,
    onRename: () -> Unit,
    onReorder: () -> Unit,
    onSaveOrder: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.06f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                shape = CircleShape,
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onBack)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = strings.back,
                        tint = LevyraText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (reorderMode) {
                Surface(
                    color = LevyraCyan,
                    shape = CircleShape,
                    modifier = Modifier
                        .height(42.dp)
                        .clickable(onClick = onSaveOrder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = LevyraBlack, modifier = Modifier.size(18.dp))
                        Text(strings.save, color = LevyraBlack, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Box {
                    Surface(
                        color = Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(44.dp)
                            .clickable(onClick = { menuExpanded = true })
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = null, tint = LevyraText, modifier = Modifier.size(20.dp))
                        }
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(LevyraPanel)
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.playlistName) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                            onClick = { menuExpanded = false; onRename() }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.dragToReorder) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Sort, null) },
                            onClick = { menuExpanded = false; onReorder() }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(20.dp),
                        clip = false,
                        ambientColor = LevyraCyan.copy(alpha = 0.20f),
                        spotColor = Color.Black.copy(alpha = 0.60f)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(LevyraPanelSoft)
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(20.dp))
            ) {
                val previewTracks = playlist.tracks.take(4)
                if (previewTracks.size >= 4) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = previewTracks[0].thumbnailUrl.ifBlank { previewTracks[0].largeThumbnailUrl },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            AsyncImage(
                                model = previewTracks[1].thumbnailUrl.ifBlank { previewTracks[1].largeThumbnailUrl },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = previewTracks[2].thumbnailUrl.ifBlank { previewTracks[2].largeThumbnailUrl },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            AsyncImage(
                                model = previewTracks[3].thumbnailUrl.ifBlank { previewTracks[3].largeThumbnailUrl },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                } else if (previewTracks.isNotEmpty()) {
                    AsyncImage(
                        model = previewTracks[0].thumbnailUrl.ifBlank { previewTracks[0].largeThumbnailUrl },
                        contentDescription = playlist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(
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

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = strings.playlists.uppercase(Locale.ROOT),
                    color = LevyraCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    maxLines = 1
                )
                Text(
                    text = playlist.name,
                    color = LevyraText,
                    fontSize = 22.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(22.sp),
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.6).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val durationText = strings.formatLibraryDuration(durationMs)
                Text(
                    text = "${strings.formatTrackCount(playlist.size)}  ·  $durationText",
                    color = LevyraMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!reorderMode && playlist.tracks.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = LevyraCyan,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = LevyraCyan.copy(alpha = 0.45f))
                        .clickable(onClick = onPlay)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = LevyraBlack, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.play, color = LevyraBlack, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onShuffle)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = strings.shuffle, tint = LevyraText, modifier = Modifier.size(20.dp))
                    }
                }
                Surface(
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onDownload)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Download, contentDescription = strings.download, tint = LevyraText, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        if (reorderMode) {
            Text(strings.dragToReorder, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
internal fun PlaylistReorderRow(
    track: Track,
    index: Int,
    count: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Surface(
        color = LevyraPanel.copy(alpha = 0.82f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${index + 1}",
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
                Text(track.artist, color = LevyraMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onMoveUp, enabled = index > 0) {
                Icon(
                    Icons.Rounded.ArrowUpward,
                    contentDescription = null,
                    tint = if (index > 0) LevyraText else LevyraMuted.copy(alpha = 0.3f)
                )
            }
            IconButton(onClick = onMoveDown, enabled = index < count - 1) {
                Icon(
                    Icons.Rounded.ArrowDownward,
                    contentDescription = null,
                    tint = if (index < count - 1) LevyraText else LevyraMuted.copy(alpha = 0.3f)
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
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    Surface(
        color = LevyraPanel.copy(alpha = 0.98f),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        shadowElevation = 12.dp,
        modifier = modifier.fillMaxWidth().combinedClickable(onClick = onOpen)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LibraryArtwork(
                track.largeThumbnailUrl.ifBlank { track.thumbnailUrl },
                track.title,
                Modifier.size(52.dp),
                RoundedCornerShape(14.dp),
                false
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    color = LevyraText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(track.artist, color = LevyraMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) strings.pause else strings.play,
                    tint = LevyraCyan
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
}

internal fun LibrarySort.libraryLabel(strings: LevyraStrings): String = when (this) {
    LibrarySort.Recent -> strings.recent
    LibrarySort.Title -> strings.song
    LibrarySort.Artist -> strings.artistLabel
    LibrarySort.Album -> strings.albumPlain
    LibrarySort.Duration -> strings.timer
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

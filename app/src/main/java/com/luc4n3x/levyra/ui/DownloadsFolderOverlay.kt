package com.luc4n3x.levyra.ui

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.visibleDownloadBatches
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel
import com.luc4n3x.levyra.ui.components.*
import com.luc4n3x.levyra.ui.downloadhealth.DownloadHealthEntry
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.formatLibraryBytes
import com.luc4n3x.levyra.ui.library.LibraryBatchDownloadRow
import com.luc4n3x.levyra.ui.library.LibraryDownloadTaskRow
import com.luc4n3x.levyra.ui.library.LibrarySectionTitle
import com.luc4n3x.levyra.ui.theme.*

private val DownloadRowArtworkShape = RoundedCornerShape(8.dp)

internal fun offlineDownloadFormatLabel(mimeType: String, fileName: String): String = when (mimeType.substringBefore(';').trim().lowercase()) {
    "audio/flac", "audio/x-flac" -> "FLAC"
    "audio/mp4", "audio/m4a", "audio/x-m4a" -> "M4A"
    "audio/mpeg", "audio/mp3" -> "MP3"
    "audio/aac", "audio/aacp" -> "AAC"
    "audio/ogg", "application/ogg" -> "OGG"
    "audio/opus" -> "OPUS"
    "audio/wav", "audio/x-wav", "audio/wave" -> "WAV"
    "audio/webm" -> "WEBM"
    else -> fileName.substringAfterLast('.', "").uppercase()
}

private fun offlineDownloadDetail(download: DownloadedTrack, size: String, coverAndTags: String): String =
    listOfNotNull(
        offlineDownloadFormatLabel(download.mimeType, download.fileName).takeIf(String::isNotBlank),
        size.takeIf { download.sizeBytes > 0L },
        coverAndTags.takeIf { download.embeddedMetadata }
    ).joinToString(" · ")

@Composable
internal fun DownloadsFolderOverlay(
    state: LevyraUiState,
    viewModel: LevyraViewModel,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val downloadsAccent = rememberNowPlayingAccent(state.currentTrack, LevyraCyan)
    val downloadsStyle = LevyraConnectedDefaults.style(accent = downloadsAccent)
    val activeBatches = remember(state.downloadBatches) { visibleDownloadBatches(state.downloadBatches) }
    val hasTransfers = activeBatches.isNotEmpty() || state.downloadQueue.isNotEmpty()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevyraInk)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = levyraTabContentBottomPadding(state.currentTrack != null)),
            verticalArrangement = Arrangement.spacedBy(downloadsStyle.gap)
        ) {
            item(contentType = "downloads-header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LevyraBackButton(onClick = onClose, contentDescription = LocalLevyraStrings.current.back)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = LocalLevyraStrings.current.offlineDownloadsPlain,
                            color = LevyraText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = listOfNotNull(
                                strings.formatSavedTrackCount(state.downloads.size),
                                strings.formatLibraryBytes(state.downloadStorageBytes).takeIf { state.downloadStorageBytes > 0L }
                            ).joinToString(" · "),
                            color = LevyraMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            item(key = "download-health-entry") { DownloadHealthEntry() }
            if (hasTransfers) {
                item(key = "dlfolder-queue-title", contentType = "downloads-section-title") {
                    LibrarySectionTitle(strings.downloadsInProgress, strings.downloadInProgress)
                }
                items(activeBatches, key = { "dlfolder-batch-${it.key}" }, contentType = { "download-batch" }) { batch ->
                    LibraryBatchDownloadRow(
                        batch = batch,
                        onRetry = { viewModel.retryBatchDownload(batch.key) },
                        onCancel = { viewModel.cancelBatchDownload(batch.key) }
                    )
                }
                items(state.downloadQueue, key = { "dlfolder-task-${it.taskKey}" }, contentType = { "download-task" }) { task ->
                    LibraryDownloadTaskRow(
                        task = task,
                        onPause = { viewModel.pauseDownload(task.taskKey) },
                        onResume = { viewModel.resumeDownload(task.taskKey) },
                        onCancel = { viewModel.cancelDownload(task.taskKey) }
                    )
                }
                if (state.downloads.isNotEmpty()) {
                    item(key = "dlfolder-saved-title", contentType = "downloads-section-title") {
                        LibrarySectionTitle(strings.downloaded, "")
                    }
                }
            }
            if (state.downloads.isEmpty()) {
                if (!hasTransfers) {
                    item {
                        EmptyState(strings.noOfflineDownloads)
                    }
                }
            } else {
                itemsIndexed(
                    state.downloads,
                    key = { _, download -> "dlfolder-${download.id}" },
                    contentType = { _, _ -> "download-saved" }
                ) { downloadIndex, download ->
                    DownloadRow(
                        download = download,
                        isCurrent = download.trackId == state.currentTrack?.id,
                        isPlaying = state.isPlaying,
                        position = LevyraConnectedPosition.of(downloadIndex, state.downloads.size),
                        style = downloadsStyle,
                        accent = downloadsAccent,
                        onPlay = { viewModel.playDownloaded(download) },
                        onDelete = { viewModel.deleteDownload(download) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun DownloadRow(
    download: DownloadedTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    position: LevyraConnectedPosition,
    style: LevyraConnectedStyle,
    accent: Color,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val artworkBrush = remember(accent) {
        Brush.linearGradient(listOf(accent.copy(alpha = 0.32f), LevyraViolet.copy(alpha = 0.28f)))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .levyraConnectedSurface(position, style, selected = isCurrent)
            .levyraPressable(onClick = onPlay, pressedScale = LevyraPressScale.Row)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(DownloadRowArtworkShape)
                .background(artworkBrush),
            contentAlignment = Alignment.Center
        ) {
            if (isCurrent) {
                LevyraPlayingIndicator(
                    playing = isPlaying,
                    color = accent,
                    size = 20.dp,
                    contentDescription = strings.playing
                )
            } else {
                Icon(Icons.Rounded.DownloadDone, null, tint = accent, modifier = Modifier.size(24.dp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(download.title, color = if (isCurrent) accent else LevyraText, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(download.artist, color = LevyraMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                offlineDownloadDetail(
                    download,
                    Formatter.formatShortFileSize(LocalContext.current, download.sizeBytes.coerceAtLeast(0L)),
                    LocalLevyraStrings.current.coverAndTags
                ),
                color = LevyraMuted.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = LocalLevyraStrings.current.remove, tint = LevyraMuted, modifier = Modifier.size(22.dp))
        }
    }
}

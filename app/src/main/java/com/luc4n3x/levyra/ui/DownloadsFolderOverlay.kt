package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel
import com.luc4n3x.levyra.ui.components.*
import com.luc4n3x.levyra.ui.downloadhealth.DownloadHealthEntry
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.*

private val DownloadRowArtworkShape = RoundedCornerShape(8.dp)

@Composable
internal fun DownloadsFolderOverlay(
    state: LevyraUiState,
    viewModel: LevyraViewModel,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val downloadsAccent = rememberNowPlayingAccent(state.currentTrack, LevyraCyan)
    val downloadsStyle = LevyraConnectedDefaults.style(accent = downloadsAccent)
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
                            text = strings.formatSavedTrackCount(state.downloads.size),
                            color = LevyraMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            item(key = "download-health-entry") { DownloadHealthEntry() }
            if (state.downloads.isEmpty()) {
                item {
                    EmptyState(strings.noOfflineDownloads)
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
                if (download.embeddedMetadata) "Music/Levyra · ${LocalLevyraStrings.current.coverAndTags}" else "Music/Levyra",
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

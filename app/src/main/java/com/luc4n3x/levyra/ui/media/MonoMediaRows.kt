package com.luc4n3x.levyra.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.CoverImage
import com.luc4n3x.levyra.ui.MonoWaveLine
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.formatSeekbarMillis
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraMonoMediaStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.playlistImportHubCopy
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.MonoDesign

private val MonoSelectedFill = MonoDesign.accent.copy(alpha = 0.12f)
private val MonoCurrentScrim = Color.Black.copy(alpha = 0.55f)
private val MonoNumberWidth = 34.dp

@Composable
internal fun monoNumeralStyle(): TextStyle = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.4.sp, fontFeatureSettings = "tnum")

@Composable
internal fun MonoLiveWave(playing: Boolean, modifier: Modifier = Modifier) {
    MonoWaveLine(playing, modifier.size(22.dp, 14.dp).clearAndSetSemantics {}, MonoDesign.accent, cycles = 2.2f, strokeWidth = 1.6.dp)
}

internal fun Modifier.monoRowRule(downloadProgress: Int?): Modifier = drawBehind {
    val thickness = 1.dp.toPx()
    val top = size.height - thickness
    drawRect(MonoDesign.hairline, Offset(0f, top), Size(size.width, thickness))
    if (downloadProgress != null) {
        val width = size.width * downloadProgress.coerceIn(0, 100) / 100f
        val start = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
        drawRect(MonoDesign.accent, Offset(start, top), Size(width, thickness))
    }
}

@Composable
private fun MonoExplicitMark(label: String) {
    Box(
        Modifier.clip(MonoDesign.artShape).background(MonoDesign.raisedHigh).padding(horizontal = 4.dp)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text(label.take(1).uppercase(), style = MaterialTheme.typography.labelSmall, color = MonoDesign.textSecondary, maxLines = 1)
    }
}

@Composable
internal fun MonoAlbumTrackRow(
    index: Int,
    track: Track,
    subtitle: String,
    current: Boolean,
    playing: Boolean,
    selected: Boolean,
    downloaded: Boolean,
    downloadProgress: Int?,
    onPlay: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
    trailing: @Composable () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraMonoMediaStrings.forCode(strings.code) }
    Row(
        modifier.fillMaxWidth().heightIn(min = MonoDesign.rowHeight)
            .background(if (selected) MonoSelectedFill else Color.Transparent)
            .monoRowRule(downloadProgress)
            .levyraPressable(onClick = onPlay, onLongClick = onLongClick, role = Role.Button, pressedScale = LevyraPressScale.Row)
            .semantics {
                this.selected = selected
                if (current) stateDescription = strings.playing
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(MonoNumberWidth), contentAlignment = Alignment.CenterStart) {
            if (current) {
                MonoLiveWave(playing)
            } else {
                Text((index + 1).toString().padStart(2, '0'), color = MonoDesign.textFaint, style = monoNumeralStyle(), maxLines = 1)
            }
        }
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                track.title, color = if (current) MonoDesign.accent else MonoDesign.text,
                style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank() || downloaded || track.explicit) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (track.explicit) MonoExplicitMark(copy.explicit)
                    if (downloaded) Icon(Icons.Rounded.DownloadDone, strings.offline, tint = MonoDesign.accent, modifier = Modifier.size(14.dp))
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, color = MonoDesign.textSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (track.durationMs > 0L) {
            Text(formatSeekbarMillis(track.durationMs), color = MonoDesign.textFaint, style = monoNumeralStyle(), maxLines = 1)
        }
        trailing()
    }
}

@Composable
internal fun MonoLibraryTrackRow(
    track: Track,
    selected: Boolean,
    selectionActive: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Int?,
    metadata: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onQueue: (() -> Unit)?,
    onAddToPlaylist: (() -> Unit)?,
    onEditTags: (() -> Unit)?,
    onKeepOffline: (() -> Unit)?,
    onDeleteDownload: (() -> Unit)?,
    onRemoveFromPlaylist: (() -> Unit)?,
    onChangeMatch: (() -> Unit)?,
    modifier: Modifier
) {
    val strings = LocalLevyraStrings.current
    val subtitle = remember(track.artist, track.album, metadata) {
        listOf(track.artist, track.album, metadata?.trim().orEmpty()).filter(String::isNotBlank).joinToString(" · ")
    }
    Row(
        modifier.fillMaxWidth().heightIn(min = MonoDesign.rowHeight)
            .background(if (selected) MonoSelectedFill else Color.Transparent)
            .monoRowRule(downloadProgress)
            .levyraPressable(onClick = onClick, onLongClick = onLongClick, role = Role.Button, pressedScale = LevyraPressScale.Row)
            .semantics {
                this.selected = selected
                if (isCurrent) stateDescription = strings.playing
            }
            .padding(start = LevyraCardDesign.RowHorizontalPadding, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(MonoDesign.rowArtwork).clip(MonoDesign.artShape).background(MonoDesign.surface)
                .border(1.dp, MonoDesign.hairline, MonoDesign.artShape),
            contentAlignment = Alignment.Center
        ) {
            CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics {})
            if (isCurrent) {
                Box(Modifier.fillMaxSize().background(MonoCurrentScrim), contentAlignment = Alignment.Center) { MonoLiveWave(isPlaying) }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                track.title, color = if (isCurrent) MonoDesign.accent else MonoDesign.text,
                style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isFavorite) Icon(Icons.Rounded.Favorite, strings.favoritesPlain, tint = MonoDesign.accent, modifier = Modifier.size(12.dp))
                if (isDownloaded) Icon(Icons.Rounded.DownloadDone, strings.downloaded, tint = MonoDesign.accent, modifier = Modifier.size(14.dp))
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = MonoDesign.textSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (track.durationMs > 0L) {
            Text(formatSeekbarMillis(track.durationMs), color = MonoDesign.textFaint, style = monoNumeralStyle(), maxLines = 1)
        }
        if (selectionActive) {
            Icon(
                if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null,
                tint = if (selected) MonoDesign.accent else MonoDesign.textFaint,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        } else {
            Box {
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Rounded.MoreVert, strings.songOptions, tint = MonoDesign.textSecondary, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = MonoDesign.raised) {
                    val item: @Composable (String, ImageVector, Color, () -> Unit) -> Unit = { label, icon, tint, action ->
                        DropdownMenuItem(
                            text = { Text(label, color = if (tint == MonoDesign.error) tint else MonoDesign.text) },
                            leadingIcon = { Icon(icon, null, tint = tint) },
                            onClick = { expanded = false; action() }
                        )
                    }
                    item(strings.play, Icons.Rounded.PlayArrow, MonoDesign.text, onClick)
                    item(
                        if (isFavorite) strings.removeFromFavorites else strings.addToFavorites,
                        if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        if (isFavorite) MonoDesign.accent else MonoDesign.text, onFavorite
                    )
                    onQueue?.let { item(strings.addToQueue, Icons.AutoMirrored.Rounded.QueueMusic, MonoDesign.text, it) }
                    onAddToPlaylist?.let { item(strings.addToPlaylist, Icons.AutoMirrored.Rounded.PlaylistAdd, MonoDesign.text, it) }
                    onEditTags?.let { item(strings.localEditTags, Icons.Rounded.Edit, MonoDesign.text, it) }
                    onKeepOffline?.let { item(strings.saveOffline, Icons.Rounded.DownloadDone, MonoDesign.text, it) }
                    if (onDeleteDownload != null) {
                        item(strings.deleteDownload, Icons.Rounded.Delete, MonoDesign.error, onDeleteDownload)
                    } else if (!isDownloaded && downloadProgress == null) {
                        item(strings.download, Icons.Rounded.Download, MonoDesign.text, onDownload)
                    }
                    onChangeMatch?.let { item(playlistImportHubCopy(strings.code)["changeMatch"], Icons.Rounded.SwapHoriz, MonoDesign.accent, it) }
                    onRemoveFromPlaylist?.let { item(strings.removeFromPlaylist, Icons.Rounded.Delete, MonoDesign.error, it) }
                }
            }
        }
    }
}

@Composable
internal fun SkinCraftedAlbumTrackRow(
    index: Int,
    track: Track,
    subtitle: String,
    current: Boolean,
    playing: Boolean,
    selected: Boolean,
    downloaded: Boolean,
    downloadProgress: Int?,
    onPlay: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
    trailing: @Composable () -> Unit
) {
    if (com.luc4n3x.levyra.ui.theme.LevyraIsMono) {
        MonoAlbumTrackRow(index, track, subtitle, current, playing, selected, downloaded, downloadProgress, onPlay, onLongClick, modifier, trailing)
    } else {
        AtelierAlbumTrackRow(index, track, subtitle, current, playing, selected, downloaded, downloadProgress, onPlay, onLongClick, modifier, trailing)
    }
}

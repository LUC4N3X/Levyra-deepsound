package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.PlaylistAlbumEntry
import com.luc4n3x.levyra.ui.components.LevyraRowText
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.formatLibraryDuration
import com.luc4n3x.levyra.ui.i18n.playlistAlbumCopy
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText

internal data class PlaylistAlbumRowActions(
    val onPlay: () -> Unit,
    val onOpenAlbum: () -> Unit,
    val onLongClick: () -> Unit,
    val onPlayNext: () -> Unit,
    val onAddToQueue: () -> Unit,
    val onRemove: () -> Unit
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PlaylistAlbumRow(
    entry: PlaylistAlbumEntry,
    selected: Boolean,
    selectionActive: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    actions: PlaylistAlbumRowActions,
    onSelectionToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val copy = strings.playlistAlbumCopy()
    var menuExpanded by remember { mutableStateOf(false) }
    val subtitle = playlistAlbumSubtitle(
        label = copy.albumLabel,
        artist = entry.album.artist,
        trackCount = strings.formatTrackCount(entry.trackCount),
        duration = if (entry.durationMs > 0L) strings.formatLibraryDuration(entry.durationMs) else ""
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(LibraryRowShape)
            .background(if (selected) LevyraCyan.copy(alpha = 0.14f) else Color.Transparent)
            .semantics {
                this.selected = selected
                customActions = listOf(
                    CustomAccessibilityAction(copy.openAlbum) { actions.onOpenAlbum(); true },
                    CustomAccessibilityAction(copy.removeAlbum) { actions.onRemove(); true }
                )
            }
            .combinedClickable(
                onClick = { if (selectionActive) onSelectionToggle() else actions.onPlay() },
                onLongClick = actions.onLongClick
            )
            .padding(start = LevyraCardDesign.RowHorizontalPadding, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaylistAlbumArtwork(entry, selected, isPlaying, playingLabel = strings.playing)
        Spacer(Modifier.width(LevyraCardDesign.RowTextGap))
        LevyraRowText(
            title = entry.album.title,
            subtitle = subtitle,
            titleColor = if (isCurrent) LevyraCyan else LevyraText,
            modifier = Modifier.weight(1f)
        )
        if (selectionActive) {
            Icon(
                if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) LevyraCyan else LevyraMuted.copy(alpha = 0.35f),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        } else {
            IconButton(onClick = actions.onOpenAlbum) {
                Icon(
                    Icons.Rounded.Album,
                    contentDescription = copy.openAlbum,
                    tint = LevyraMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = strings.options,
                        tint = LevyraMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(copy.playAlbum) },
                        leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                        onClick = { menuExpanded = false; actions.onPlay() }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.playNext) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null) },
                        onClick = { menuExpanded = false; actions.onPlayNext() }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.addToQueue) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null) },
                        onClick = { menuExpanded = false; actions.onAddToQueue() }
                    )
                    DropdownMenuItem(
                        text = { Text(copy.openAlbum) },
                        leadingIcon = { Icon(Icons.Rounded.Album, contentDescription = null) },
                        onClick = { menuExpanded = false; actions.onOpenAlbum() }
                    )
                    DropdownMenuItem(
                        text = { Text(copy.removeAlbum, color = LevyraPink) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = LevyraPink) },
                        onClick = { menuExpanded = false; actions.onRemove() }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistAlbumArtwork(
    entry: PlaylistAlbumEntry,
    selected: Boolean,
    isPlaying: Boolean,
    playingLabel: String
) {
    val thumb = LevyraCardDesign.RowThumb
    Box(modifier = Modifier.size(thumb), contentAlignment = Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .size(thumb - 6.dp)
                .offset(x = 6.dp)
                .clip(LibraryArtworkShape)
                .background(LevyraMuted.copy(alpha = 0.22f))
        )
        Box(modifier = Modifier.size(thumb - 6.dp), contentAlignment = Alignment.Center) {
            LibraryArtwork(
                url = entry.artworkUrl,
                title = entry.album.title,
                modifier = Modifier.size(thumb - 6.dp),
                shape = LibraryArtworkShape,
                selected = selected
            )
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(LibraryArtworkShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = playingLabel, tint = LevyraCyan, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

internal fun playlistAlbumSubtitle(label: String, artist: String, trackCount: String, duration: String): String =
    listOf(label, artist, trackCount, duration).filter(String::isNotBlank).joinToString(" · ")

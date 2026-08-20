package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.EmptyState
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.components.TrackRow
import com.luc4n3x.levyra.desktop.app.ui.components.TrackRowAction
import com.luc4n3x.levyra.desktop.app.ui.components.tracksTextInputFocus
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.core.storage.LocalPlaylist

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaylistScreen(
    playlist: LocalPlaylist?,
    actions: TrackActions,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onRemoveTrack: (String) -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var renaming by remember(playlist?.id) { mutableStateOf(false) }
    var draftName by remember(playlist?.id) { mutableStateOf(playlist?.name.orEmpty()) }

    if (playlist == null) {
        EmptyState(icon = LevyraIcons.Playlist, title = strings.playlistEmpty, modifier = modifier)
        return
    }

    ScrollableColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = LevyraIcons.ChevronLeft,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = strings.homePlaylists,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Artwork(
                    url = playlist.artworkUrl,
                    modifier = Modifier.size(160.dp),
                    cornerRadius = 16.dp,
                    iconSize = 40.dp
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (renaming) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = draftName,
                                onValueChange = { draftName = it },
                                singleLine = true,
                                label = { Text(strings.playlistName) },
                                modifier = Modifier.tracksTextInputFocus()
                            )
                            Button(
                                onClick = {
                                    onRename(draftName)
                                    renaming = false
                                },
                                enabled = draftName.isNotBlank()
                            ) {
                                Text(strings.save)
                            }
                        }
                    } else {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${playlist.tracks.size} ${strings.playlistTracks}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = onPlayAll, enabled = playlist.tracks.isNotEmpty()) {
                            Text(strings.playAll)
                        }
                        OutlinedButton(onClick = onShuffleAll, enabled = playlist.tracks.isNotEmpty()) {
                            Text(strings.shufflePlay)
                        }
                        OutlinedButton(onClick = { renaming = !renaming }) {
                            Text(strings.playlistRename)
                        }
                        OutlinedButton(onClick = onExport, enabled = playlist.tracks.isNotEmpty()) {
                            Text(strings.playlistExport)
                        }
                        OutlinedButton(onClick = onDelete) {
                            Text(strings.playlistDelete)
                        }
                    }
                }
            }
        }

        if (playlist.tracks.isEmpty()) {
            item {
                EmptyState(icon = LevyraIcons.Playlist, title = strings.playlistEmpty)
            }
        }

        itemsIndexed(playlist.tracks, key = { _, track -> track.id }) { index, track ->
            TrackRow(
                track = track,
                isCurrent = track.id == actions.currentTrackId,
                isFavorite = actions.isFavorite(track),
                onPlay = { actions.onPlay(playlist.tracks, index) },
                onPlayNext = { actions.onPlayNext(track) },
                onEnqueue = { actions.onEnqueue(track) },
                onToggleFavorite = { actions.onToggleFavorite(track) },
                onAddToPlaylist = { actions.onAddToPlaylist(track) },
                extraAction = TrackRowAction(
                    label = strings.removeFromPlaylist,
                    onClick = { onRemoveTrack(track.id) }
                ),
                position = index + 1
            )
        }
    }
}

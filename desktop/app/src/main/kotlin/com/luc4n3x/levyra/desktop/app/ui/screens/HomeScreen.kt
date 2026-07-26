package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.EmptyState
import com.luc4n3x.levyra.desktop.app.ui.components.SectionHeader
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.components.TrackRow
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.core.storage.LibraryData
import com.luc4n3x.levyra.desktop.core.storage.LocalPlaylist

@Composable
fun HomeScreen(
    library: LibraryData,
    actions: TrackActions,
    onOpenPlaylist: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onImportUrl: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var newPlaylistName by remember { mutableStateOf("") }
    var importUrl by remember { mutableStateOf("") }

    val historyTracks = library.history.map { it.track }

    ScrollableColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Text(
                text = strings.navHome,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (historyTracks.isEmpty() && library.favorites.isEmpty() && library.playlists.isEmpty()) {
            item {
                EmptyState(
                    icon = LevyraIcons.Disc,
                    title = strings.homeEmptyTitle,
                    body = strings.homeEmptyBody
                )
            }
        }

        if (historyTracks.isNotEmpty()) {
            item {
                SectionHeader(title = strings.homeContinueListening) {
                    TextButton(onClick = onClearHistory) {
                        Text(strings.historyClear)
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(historyTracks.take(20), key = { it.id }) { track ->
                        Column(
                            modifier = Modifier
                                .width(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { actions.onPlay(historyTracks, historyTracks.indexOf(track)) }
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Artwork(
                                url = track.artworkUrl,
                                modifier = Modifier.size(138.dp),
                                cornerRadius = 12.dp,
                                iconSize = 28.dp
                            )
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(title = strings.homePlaylists)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text(strings.playlistName) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        onCreatePlaylist(newPlaylistName)
                        newPlaylistName = ""
                    },
                    enabled = newPlaylistName.isNotBlank()
                ) {
                    Text(strings.playlistCreate)
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = importUrl,
                    onValueChange = { importUrl = it },
                    label = { Text(strings.importPlaylistHint) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        onImportUrl(importUrl)
                        importUrl = ""
                    },
                    enabled = importUrl.isNotBlank()
                ) {
                    Text(strings.importAction)
                }
            }
        }

        if (library.playlists.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(library.playlists, key = { it.id }) { playlist ->
                        PlaylistTile(playlist = playlist, onClick = { onOpenPlaylist(playlist.id) })
                    }
                }
            }
        }

        if (library.favorites.isNotEmpty()) {
            item {
                SectionHeader(title = strings.homeFavorites)
            }
            itemsIndexed(library.favorites, key = { _, track -> track.id }) { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = track.id == actions.currentTrackId,
                    isFavorite = true,
                    onPlay = { actions.onPlay(library.favorites, index) },
                    onPlayNext = { actions.onPlayNext(track) },
                    onEnqueue = { actions.onEnqueue(track) },
                    onToggleFavorite = { actions.onToggleFavorite(track) },
                    onAddToPlaylist = { actions.onAddToPlaylist(track) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistTile(playlist: LocalPlaylist, onClick: () -> Unit) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Artwork(
            url = playlist.artworkUrl,
            modifier = Modifier.size(128.dp),
            cornerRadius = 10.dp,
            iconSize = 26.dp
        )
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                imageVector = LevyraIcons.Playlist,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "${playlist.tracks.size} ${strings.playlistTracks}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

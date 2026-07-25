package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.state.PlaybackUiState
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.EmptyState
import com.luc4n3x.levyra.desktop.app.ui.components.SectionHeader
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.components.TrackRow
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons

@Composable
fun NowPlayingScreen(
    state: PlaybackUiState,
    actions: TrackActions,
    onBack: () -> Unit,
    onJumpTo: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val track = state.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = LevyraIcons.ChevronDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = strings.navNowPlaying,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (track == null) {
            item {
                EmptyState(icon = LevyraIcons.Disc, title = strings.nowPlayingEmpty)
            }
        } else {
            item {
                Artwork(
                    url = track.artworkUrl,
                    modifier = Modifier.size(320.dp),
                    cornerRadius = 24.dp,
                    iconSize = 56.dp
                )
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.displaySubtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (state.streamLabel.isNotBlank()) {
                        Text(
                            text = state.streamLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (state.queue.upcoming.isNotEmpty()) {
            item {
                SectionHeader(title = strings.queueTitle, modifier = Modifier.padding(top = 12.dp))
            }
            itemsIndexed(state.queue.upcoming) { offset, upcoming ->
                val queuePosition = state.queue.index + 1 + offset
                TrackRow(
                    track = upcoming,
                    isCurrent = false,
                    isFavorite = actions.isFavorite(upcoming),
                    onPlay = { onJumpTo(queuePosition) },
                    onPlayNext = { actions.onPlayNext(upcoming) },
                    onEnqueue = { actions.onEnqueue(upcoming) },
                    onToggleFavorite = { actions.onToggleFavorite(upcoming) },
                    onAddToPlaylist = { actions.onAddToPlaylist(upcoming) }
                )
            }
        }
    }
}

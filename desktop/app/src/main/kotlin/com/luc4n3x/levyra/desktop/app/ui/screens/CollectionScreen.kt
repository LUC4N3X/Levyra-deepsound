package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.state.CollectionUiState
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.CollectionCard
import com.luc4n3x.levyra.desktop.app.ui.components.EmptyState
import com.luc4n3x.levyra.desktop.app.ui.components.ErrorBanner
import com.luc4n3x.levyra.desktop.app.ui.components.LoadingRow
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.components.TrackRow
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.core.model.CollectionRef

@Composable
fun CollectionScreen(
    state: CollectionUiState,
    actions: TrackActions,
    onBack: () -> Unit,
    onLoadMore: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onEnqueueAll: () -> Unit,
    onOpenCollection: (CollectionRef) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val tracks = state.page.tracks

    LazyColumn(
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
                    text = state.ref?.title.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (state.error.isNotBlank()) {
            item {
                ErrorBanner(
                    message = state.error,
                    actionLabel = strings.close,
                    onAction = onBack
                )
            }
        }

        state.ref?.let { ref ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Artwork(
                        url = ref.artworkUrl,
                        modifier = Modifier.size(160.dp),
                        cornerRadius = 16.dp,
                        iconSize = 40.dp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = ref.title,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (ref.subtitle.isNotBlank()) {
                            Text(
                                text = ref.subtitle,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${tracks.size} ${strings.playlistTracks}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = onPlayAll, enabled = tracks.isNotEmpty()) {
                                Text(strings.playAll)
                            }
                            OutlinedButton(onClick = onShuffleAll, enabled = tracks.isNotEmpty()) {
                                Text(strings.shufflePlay)
                            }
                            OutlinedButton(onClick = onEnqueueAll, enabled = tracks.isNotEmpty()) {
                                Text(strings.addToQueue)
                            }
                        }
                    }
                }
            }
        }

        if (state.page.collections.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.page.collections, key = { it.id }) { ref ->
                        CollectionCard(
                            ref = ref,
                            onClick = { onOpenCollection(ref) },
                            modifier = Modifier.width(168.dp)
                        )
                    }
                }
            }
        }

        when {
            state.loading -> item { LoadingRow(label = strings.loading) }
            tracks.isEmpty() && state.error.isBlank() -> item {
                EmptyState(icon = LevyraIcons.Disc, title = strings.playlistEmpty)
            }
        }

        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
            TrackRow(
                track = track,
                isCurrent = track.id == actions.currentTrackId,
                isFavorite = actions.isFavorite(track),
                onPlay = { actions.onPlay(tracks, index) },
                onPlayNext = { actions.onPlayNext(track) },
                onEnqueue = { actions.onEnqueue(track) },
                onToggleFavorite = { actions.onToggleFavorite(track) },
                onAddToPlaylist = { actions.onAddToPlaylist(track) },
                position = index + 1
            )
        }

        if (state.canLoadMore) {
            item {
                TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.loadMore)
                }
            }
        }

        if (state.loadingMore) {
            item { LoadingRow(label = strings.loading) }
        }
    }
}

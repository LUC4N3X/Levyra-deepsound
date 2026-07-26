package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.state.SearchUiState
import com.luc4n3x.levyra.desktop.app.ui.components.CollectionCard
import com.luc4n3x.levyra.desktop.app.ui.components.EmptyState
import com.luc4n3x.levyra.desktop.app.ui.components.ErrorBanner
import com.luc4n3x.levyra.desktop.app.ui.components.LevyraChip
import com.luc4n3x.levyra.desktop.app.ui.components.LoadingRow
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.components.TrackRow
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.core.model.CollectionRef
import com.luc4n3x.levyra.desktop.core.model.SearchFilter

@Composable
fun SearchScreen(
    state: SearchUiState,
    recentSearches: List<String>,
    actions: TrackActions,
    onQueryChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onLoadMore: () -> Unit,
    onOpenCollection: (CollectionRef) -> Unit,
    onClearRecent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text(strings.searchPlaceholder) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = LevyraIcons.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (state.query.isNotBlank()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = LevyraIcons.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit(state.query) }),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { onSubmit(state.query) },
                enabled = state.query.isNotBlank()
            ) {
                Text(strings.searchAction)
            }
        }

        if (state.suggestions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                state.suggestions.forEach { suggestion ->
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSubmit(suggestion) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchFilter.entries.forEach { filter ->
                LevyraChip(
                    label = filterLabel(filter),
                    selected = filter == state.filter,
                    onClick = { onFilterChange(filter) }
                )
            }
        }

        if (state.error.isNotBlank()) {
            ErrorBanner(
                message = state.error,
                actionLabel = strings.retry,
                onAction = { onSubmit(state.submittedQuery) },
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        when {
            state.loading -> LoadingRow(label = strings.loading)

            state.submittedQuery.isBlank() -> RecentSearches(
                recentSearches = recentSearches,
                onSelect = onSubmit,
                onClear = onClearRecent
            )

            !state.hasResults -> EmptyState(
                icon = LevyraIcons.Search,
                title = strings.searchNoResults
            )

            state.page.tracks.isNotEmpty() -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(state.page.tracks, key = { _, track -> track.id }) { index, track ->
                    TrackRow(
                        track = track,
                        isCurrent = track.id == actions.currentTrackId,
                        isFavorite = actions.isFavorite(track),
                        onPlay = { actions.onPlay(state.page.tracks, index) },
                        onPlayNext = { actions.onPlayNext(track) },
                        onEnqueue = { actions.onEnqueue(track) },
                        onToggleFavorite = { actions.onToggleFavorite(track) },
                        onAddToPlaylist = { actions.onAddToPlaylist(track) }
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

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 168.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.page.collections, key = { it.id }) { ref ->
                    CollectionCard(ref = ref, onClick = { onOpenCollection(ref) })
                }
                if (state.canLoadMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) {
                            Text(strings.loadMore)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSearches(
    recentSearches: List<String>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit
) {
    val strings = LocalStrings.current
    if (recentSearches.isEmpty()) {
        EmptyState(icon = LevyraIcons.Search, title = strings.searchEmpty)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = strings.recentSearches, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClear) {
                Text(strings.clearRecentSearches)
            }
        }
        recentSearches.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelect(entry) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = LevyraIcons.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(text = entry, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun filterLabel(filter: SearchFilter): String {
    val strings = LocalStrings.current
    return when (filter) {
        SearchFilter.SONGS -> strings.filterSongs
        SearchFilter.VIDEOS -> strings.filterVideos
        SearchFilter.ALBUMS -> strings.filterAlbums
        SearchFilter.PLAYLISTS -> strings.filterPlaylists
        SearchFilter.ARTISTS -> strings.filterArtists
    }
}

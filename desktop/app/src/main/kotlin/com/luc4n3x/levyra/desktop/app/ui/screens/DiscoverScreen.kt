package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.state.DiscoverUiState
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.EmptyState
import com.luc4n3x.levyra.desktop.app.ui.components.LoadingRow
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.components.TrackRow
import com.luc4n3x.levyra.desktop.app.ui.components.hoverScale
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.LocalAccentColor
import com.luc4n3x.levyra.desktop.core.model.Track

@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    actions: TrackActions,
    onCountryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val accent = LocalAccentColor.current
    val podium = state.tracks.take(3)
    val rest = state.tracks.drop(3)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accent.copy(alpha = 0.34f),
                                MaterialTheme.colorScheme.surfaceContainer
                            )
                        )
                    )
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.chartsTitle,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = strings.chartsSubtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = onPlayAll, enabled = state.tracks.isNotEmpty()) {
                        Text(strings.playAll)
                    }
                    OutlinedButton(onClick = onShuffleAll, enabled = state.tracks.isNotEmpty()) {
                        Text(strings.shufflePlay)
                    }
                    OutlinedButton(onClick = onRefresh) {
                        Text(strings.chartsRefresh)
                    }
                    OutlinedTextField(
                        value = state.country,
                        onValueChange = onCountryChange,
                        label = { Text(strings.chartsCountry) },
                        singleLine = true,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )
                }
            }
        }

        if (state.loading && state.tracks.isEmpty()) {
            item { LoadingRow(label = strings.loading) }
        }

        if (!state.loading && state.tracks.isEmpty()) {
            item {
                EmptyState(
                    icon = LevyraIcons.Disc,
                    title = strings.chartsEmpty,
                    body = state.error
                )
            }
        }

        if (podium.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    podium.forEachIndexed { index, track ->
                        PodiumCard(
                            rank = index + 1,
                            track = track,
                            accent = accent,
                            onPlay = { actions.onPlay(state.tracks, index) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        itemsIndexed(rest, key = { _, track -> track.id }) { index, track ->
            val position = index + podium.size
            TrackRow(
                track = track,
                isCurrent = track.id == actions.currentTrackId,
                isFavorite = actions.isFavorite(track),
                onPlay = { actions.onPlay(state.tracks, position) },
                onPlayNext = { actions.onPlayNext(track) },
                onEnqueue = { actions.onEnqueue(track) },
                onToggleFavorite = { actions.onToggleFavorite(track) },
                onAddToPlaylist = { actions.onAddToPlaylist(track) },
                position = position + 1
            )
        }
    }
}

@Composable
private fun PodiumCard(
    rank: Int,
    track: Track,
    accent: Color,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .hoverScale()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            )
            .clickable(onClick = onPlay)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Artwork(
                url = track.artworkUrl,
                modifier = Modifier.fillMaxWidth().height(190.dp),
                cornerRadius = 16.dp,
                iconSize = 36.dp
            )
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

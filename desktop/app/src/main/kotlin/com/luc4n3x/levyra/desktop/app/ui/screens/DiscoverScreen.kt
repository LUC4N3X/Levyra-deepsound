package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.luc4n3x.levyra.desktop.app.ui.components.CountryPicker
import com.luc4n3x.levyra.desktop.app.ui.components.EmptyState
import com.luc4n3x.levyra.desktop.app.ui.components.LoadingRow
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.components.TrackRow
import com.luc4n3x.levyra.desktop.app.ui.components.rememberHoverState
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraMotion
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

    ScrollableColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accent.copy(alpha = 0.27f),
                                MaterialTheme.colorScheme.surfaceContainerHigh,
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
                    modifier = Modifier.padding(top = 8.dp),
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
                    CountryPicker(
                        selectedCode = state.country,
                        label = strings.chartsCountry,
                        contentDescription = strings.chartsSelectCountry,
                        onSelected = onCountryChange
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
    val (interactionSource, hovered) = rememberHoverState(track.id)
    val hoverLayer = if (hovered) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = LevyraMotion.HOVER_ALPHA)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = if (hovered) 0.27f else 0.18f),
                        hoverLayer
                    )
                )
            )
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onPlay
            )
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Artwork(
                url = track.artworkUrl,
                modifier = Modifier.fillMaxWidth().height(190.dp),
                cornerRadius = 14.dp,
                iconSize = 36.dp
            )
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
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

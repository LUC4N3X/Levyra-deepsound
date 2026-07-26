package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.state.LyricsUiState
import com.luc4n3x.levyra.desktop.app.state.PlaybackUiState
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.EmptyState
import com.luc4n3x.levyra.desktop.app.ui.components.LoadingRow
import com.luc4n3x.levyra.desktop.app.ui.components.SectionHeader
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.components.TrackRow
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.LocalAccentColor

@Composable
fun NowPlayingScreen(
    state: PlaybackUiState,
    lyricsState: LyricsUiState,
    actions: TrackActions,
    onBack: () -> Unit,
    onJumpTo: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val accent = LocalAccentColor.current
    val track = state.current

    Box(modifier = modifier.fillMaxSize()) {
        if (track != null && track.hasArtwork) {
            Artwork(
                url = track.artworkUrl,
                modifier = Modifier.fillMaxSize().blur(70.dp),
                cornerRadius = 0.dp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.82f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.97f)
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = LevyraIcons.ChevronDown,
                        contentDescription = strings.playbackCollapse,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = strings.navNowPlaying,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (track == null) {
                EmptyState(icon = LevyraIcons.Disc, title = strings.nowPlayingEmpty)
            } else {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(36.dp)
                ) {
                    Column(
                        modifier = Modifier.width(380.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Artwork(
                            url = track.artworkUrl,
                            modifier = Modifier
                                .size(340.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(accent.copy(alpha = 0.5f), accent.copy(alpha = 0.1f))
                                    )
                                ),
                            cornerRadius = 26.dp,
                            iconSize = 56.dp
                        )
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
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
                                color = accent
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        LyricsPanel(
                            lyricsState = lyricsState,
                            positionMs = state.positionMs,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.queue.upcoming.isNotEmpty()) {
                            SectionHeader(title = strings.queueTitle)
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentPadding = PaddingValues(bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
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
                }
            }
        }
    }
}

@Composable
private fun LyricsPanel(
    lyricsState: LyricsUiState,
    positionMs: Long,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val accent = LocalAccentColor.current
    val lyrics = lyricsState.lyrics
    val listState = rememberLazyListState()
    val activeIndex = lyrics?.activeIndex(positionMs) ?: -1

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && !listState.isScrollInProgress) {
            listState.animateScrollToItem(index = activeIndex)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = strings.lyricsTitle)
        when {
            lyricsState.loading -> LoadingRow(label = strings.loading)

            lyrics == null || lyrics.lines.isEmpty() -> Text(
                text = strings.lyricsMissing,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(lyrics.lines) { index, line ->
                    val isActive = index == activeIndex
                    val color by animateColorAsState(
                        targetValue = when {
                            isActive -> accent
                            index < activeIndex -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        }
                    )
                    Text(
                        text = line.text,
                        style = if (isActive) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        color = color
                    )
                }
            }
        }
    }
}

package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
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

    ScrollableColumn(
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
                                accent.copy(alpha = 0.32f),
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
                    ChartRegionPicker(
                        selectedCode = state.country,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
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
private fun ChartRegionPicker(
    selectedCode: String,
    onSelected: (String) -> Unit
) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    val selected = chartRegions.firstOrNull {
        it.code.equals(selectedCode, ignoreCase = true)
    } ?: chartRegions.first()

    Box {
        Surface(
            modifier = Modifier
                .width(238.dp)
                .height(48.dp)
                .clickable { expanded = true },
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = selected.flag, style = MaterialTheme.typography.titleMedium)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.chartsCountry,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selected.name(strings.languageCode),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = LevyraIcons.ChevronDown,
                    contentDescription = strings.chartsSelectCountry,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(270.dp)
                .heightIn(max = 430.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            chartRegions.forEach { region ->
                val active = region.code.equals(selected.code, ignoreCase = true)
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            Text(text = region.flag)
                            Text(
                                text = region.name(strings.languageCode),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (active) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        if (!active) onSelected(region.code)
                    },
                    leadingIcon = if (active) {
                        {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    } else {
                        null
                    }
                )
            }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
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

private data class ChartRegion(
    val code: String,
    val flag: String,
    val englishName: String,
    val italianName: String = englishName
) {
    fun name(languageCode: String): String = if (languageCode == "it") italianName else englishName
}

private val chartRegions = listOf(
    ChartRegion("IT", "🇮🇹", "Italy", "Italia"),
    ChartRegion("US", "🇺🇸", "United States", "Stati Uniti"),
    ChartRegion("GB", "🇬🇧", "United Kingdom", "Regno Unito"),
    ChartRegion("ES", "🇪🇸", "Spain", "Spagna"),
    ChartRegion("FR", "🇫🇷", "France", "Francia"),
    ChartRegion("DE", "🇩🇪", "Germany", "Germania"),
    ChartRegion("BR", "🇧🇷", "Brazil", "Brasile"),
    ChartRegion("PT", "🇵🇹", "Portugal", "Portogallo"),
    ChartRegion("NL", "🇳🇱", "Netherlands", "Paesi Bassi"),
    ChartRegion("PL", "🇵🇱", "Poland", "Polonia"),
    ChartRegion("RO", "🇷🇴", "Romania"),
    ChartRegion("GR", "🇬🇷", "Greece", "Grecia"),
    ChartRegion("SE", "🇸🇪", "Sweden", "Svezia"),
    ChartRegion("DK", "🇩🇰", "Denmark", "Danimarca"),
    ChartRegion("CZ", "🇨🇿", "Czechia", "Repubblica Ceca"),
    ChartRegion("UA", "🇺🇦", "Ukraine", "Ucraina"),
    ChartRegion("RU", "🇷🇺", "Russia"),
    ChartRegion("TR", "🇹🇷", "Türkiye", "Turchia"),
    ChartRegion("CN", "🇨🇳", "China", "Cina"),
    ChartRegion("JP", "🇯🇵", "Japan", "Giappone"),
    ChartRegion("KR", "🇰🇷", "South Korea", "Corea del Sud"),
    ChartRegion("IN", "🇮🇳", "India"),
    ChartRegion("ID", "🇮🇩", "Indonesia"),
    ChartRegion("VN", "🇻🇳", "Vietnam"),
    ChartRegion("TH", "🇹🇭", "Thailand", "Thailandia"),
    ChartRegion("PH", "🇵🇭", "Philippines", "Filippine"),
    ChartRegion("IL", "🇮🇱", "Israel", "Israele")
)

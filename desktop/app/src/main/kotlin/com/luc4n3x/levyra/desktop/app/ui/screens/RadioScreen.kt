package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.state.RadioUiState
import com.luc4n3x.levyra.desktop.core.radio.SavedStation
import com.luc4n3x.levyra.desktop.core.radio.toSaved
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
import com.luc4n3x.levyra.desktop.app.ui.components.SectionTints
import com.luc4n3x.levyra.desktop.app.ui.components.ShelfRow
import com.luc4n3x.levyra.desktop.app.ui.components.SunnyShape
import com.luc4n3x.levyra.desktop.app.ui.components.horizontalShelfBleed
import com.luc4n3x.levyra.desktop.app.ui.components.tracksTextInputFocus
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraBrand
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.feature.radio.RadioCategory
import com.luc4n3x.levyra.ui.i18n.LevyraLiveRadioCatalog
import java.util.Locale

@Composable
internal fun RadioScreen(
    state: RadioUiState,
    contentCountry: String,
    currentTrackId: String?,
    onOpen: (String) -> Unit,
    onCategory: (RadioCategory) -> Unit,
    onSearch: (String) -> Unit,
    onToggleFavorite: (SavedStation) -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val code = strings.languageCode
    val copy = remember(code) { LevyraLiveRadioCatalog.forCode(code) }
    LaunchedEffect(contentCountry) { onOpen(contentCountry) }
    val countryName = remember(state.country, code) {
        Locale("", state.country).getDisplayCountry(Locale.forLanguageTag(code)).ifBlank { state.country }
    }
    val favoriteIds = remember(state.favorites) { state.favorites.mapTo(HashSet()) { it.uuid } }
    val stationTracks = remember(state.stations) { state.stations.map { it.toTrack() } }

    ScrollableColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(52.dp).clip(SunnyShape).background(SectionTints.Amber), contentAlignment = Alignment.Center) {
                    Icon(LevyraIcons.Radio, contentDescription = null, tint = Color(0xFF2A1A05), modifier = Modifier.size(24.dp))
                }
                Text(copy.title, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold))
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(LevyraIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f)) {
                    if (state.query.isEmpty()) {
                        Text(copy.searchHint, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    BasicTextField(
                        value = state.query,
                        onValueChange = onSearch,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(LevyraBrand.soft),
                        modifier = Modifier.fillMaxWidth().tracksTextInputFocus()
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(copy.categories, style = MaterialTheme.typography.titleMedium, color = LevyraBrand.soft)
                ShelfRow(
                    modifier = Modifier.horizontalShelfBleed(32.dp),
                    contentPadding = PaddingValues(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(RadioCategory.entries.toList(), key = { it.name }) { category ->
                        CategoryChip(copy.category(category), state.category == category && state.query.isBlank()) { onCategory(category) }
                    }
                }
            }
        }
        if (state.favorites.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.homeFavorites, style = MaterialTheme.typography.titleMedium, color = LevyraBrand.soft)
                    StationGrid(
                        rows = state.favorites.mapIndexed { index, saved -> StationRowData(saved, index + 1, saved.toTrack()) },
                        favoriteIds = favoriteIds,
                        currentTrackId = currentTrackId,
                        ranked = false,
                        onToggleFavorite = onToggleFavorite,
                        onPlay = { index -> onPlay(state.favorites.map { it.toTrack() }, index) }
                    )
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (state.category == RadioCategory.Worldwide) copy.worldwide else copy.popularIn(countryName),
                    style = MaterialTheme.typography.titleMedium,
                    color = LevyraBrand.soft
                )
                if (state.loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = LevyraBrand.soft)
            }
        }
        item {
            when {
                state.failed && state.stations.isEmpty() -> Text(copy.internetRequired, color = MaterialTheme.colorScheme.onSurfaceVariant)
                !state.loading && state.stations.isEmpty() -> Text(copy.noStations, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> StationGrid(
                    rows = state.stations.mapIndexed { index, station -> StationRowData(station.toSaved(), index + 1, stationTracks[index]) },
                    favoriteIds = favoriteIds,
                    currentTrackId = currentTrackId,
                    ranked = true,
                    onToggleFavorite = onToggleFavorite,
                    onPlay = { index -> onPlay(stationTracks, index) }
                )
            }
        }
        item { Spacer(Modifier.size(8.dp)) }
    }
}

private data class StationRowData(val saved: SavedStation, val rank: Int, val track: Track)

@Composable
private fun StationGrid(
    rows: List<StationRowData>,
    favoriteIds: Set<String>,
    currentTrackId: String?,
    ranked: Boolean,
    onToggleFavorite: (SavedStation) -> Unit,
    onPlay: (Int) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth > 900.dp) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.chunked(columns).forEachIndexed { rowIndex, chunk ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    chunk.forEachIndexed { offset, row ->
                        StationRow(
                            row = row,
                            favorite = row.saved.uuid in favoriteIds,
                            playing = row.track.id == currentTrackId,
                            ranked = ranked,
                            onToggleFavorite = { onToggleFavorite(row.saved) },
                            onClick = { onPlay(rowIndex * columns + offset) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columns - chunk.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun StationRow(
    row: StationRowData,
    favorite: Boolean,
    playing: Boolean,
    ranked: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (playing) LevyraBrand.soft.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (ranked) {
            Text(
                "${row.rank}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(26.dp)
            )
        }
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
            if (row.saved.favicon.isNotBlank()) {
                Artwork(row.saved.favicon, Modifier.size(52.dp), cornerRadius = 14.dp, iconSize = 18.dp)
            } else {
                Text(row.saved.name.take(2).uppercase(), style = MaterialTheme.typography.titleMedium, color = LevyraBrand.soft)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(row.saved.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (playing) LevyraBrand.soft else MaterialTheme.colorScheme.onSurface)
            if (row.saved.details.isNotBlank()) {
                Text(row.saved.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                if (favorite) LevyraIcons.HeartFilled else LevyraIcons.Heart,
                contentDescription = null,
                tint = if (favorite) SectionTints.Rose else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) LevyraBrand.onSoft else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(LevyraBrand.soft)
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp)
    )
}

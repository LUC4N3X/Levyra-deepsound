package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import com.luc4n3x.levyra.feature.radio.LiveRadioViewModel
import com.luc4n3x.levyra.feature.radio.RadioStation
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraLiveRadioCatalog
import com.luc4n3x.levyra.ui.i18n.radioLibraryCopy
import com.luc4n3x.levyra.ui.i18n.spotifyLibraryCopy
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.GridView
import com.luc4n3x.levyra.ui.theme.AppleMusicDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel

private const val AppleRadioGridColumns = 3
private const val AppleRadioGridLimit = 6
private const val AppleRadioLiveLimit = 6
private const val AppleRadioRailLimit = 20
private val AppleRadioLiveWidth = 300.dp
private val AppleRadioRailWidth = 160.dp

internal fun appleRadioGrid(favorites: List<RadioStation>, stations: List<RadioStation>): List<RadioStation> =
    (favorites + stations).distinctBy { it.uuid.lowercase() }.take(AppleRadioGridLimit)

@Composable
internal fun AppleMusicRadioScreen(viewModel: LevyraViewModel, state: LevyraUiState, bottomInset: Dp) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val factory = remember(context.applicationContext) { LiveRadioViewModel.factory(context.applicationContext) }
    val radio: LiveRadioViewModel = composeViewModel(key = "levyra-live-radio", factory = factory)
    val radioState by radio.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.languageCode) { radio.activate(state.languageCode) }
    val radioStrings = remember(state.languageCode) { LevyraLiveRadioCatalog.forCode(state.languageCode) }
    val libraryCopy = remember(state.languageCode) { radioLibraryCopy(state.languageCode) }
    val stations = radioState.stations
    val grid = remember(radioState.favorites, stations) { appleRadioGrid(radioState.favorites, stations) }
    val live = remember(stations, grid) { stations.filterNot { station -> grid.any { it.uuid == station.uuid } }.take(AppleRadioLiveLimit) }
    val popular = remember(stations, live, grid) { stations.filterNot { s -> live.any { it.uuid == s.uuid } || grid.any { it.uuid == s.uuid } }.take(AppleRadioRailLimit) }
    val country = stations.firstOrNull()?.country.orEmpty()
    val play: (RadioStation) -> Unit = viewModel::playLiveRadio
    var countryPickerOpen by remember { mutableStateOf(false) }
    val searching = radioState.query.isNotBlank()
    val countryLabel = radioState.selectedCountryCode?.let { code -> localizedCountryName(code, state.languageCode).ifBlank { code } } ?: radioStrings.worldwide

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppleMusicDesign.background),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
            bottom = bottomInset + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(AppleMusicDesign.sectionGap)
    ) {
        item(key = "apple-radio-title") {
            Text(
                com.luc4n3x.levyra.ui.i18n.radioTabCopy(state.languageCode).tab, color = AppleMusicDesign.label,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = AppleMusicDesign.gutter).semantics { heading() }
            )
        }
        item(key = "apple-radio-search") {
            AppleRadioSearchBar(radioState.query, radioStrings.searchHint, countryLabel, radio::setQuery) { countryPickerOpen = true }
        }
        if (searching) {
            appleRadioSearchResults(radioState, radioStrings, radio, play)
            return@LazyColumn
        }
        if (grid.isEmpty() && live.isEmpty() && popular.isEmpty()) item(key = "apple-radio-status") {
            AppleRadioStatus(radioState.loading, radioState.error, radioStrings.noStations, radio::retry)
        }
        if (grid.isNotEmpty()) item(key = "apple-radio-grid") { AppleRadioGrid(grid, play) }
        if (live.isNotEmpty()) item(key = "apple-radio-live") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppleRadioSectionTitle(radioStrings.subtitle)
                LazyRow(contentPadding = PaddingValues(horizontal = AppleMusicDesign.gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(live, key = { "live-${it.uuid}" }) { station -> AppleRadioLiveCard(station, radioStrings.live) { play(station) } }
                }
            }
        }
        if (popular.isNotEmpty()) item(key = "apple-radio-popular") {
            AppleRadioRail(if (country.isBlank()) radioStrings.worldwide else radioStrings.popularIn(country), popular, play)
        }
        appleRadioFavorites(radioState, libraryCopy.favorites, state.languageCode, radio, play)
    }
    if (countryPickerOpen) {
        RadioDirectoryDialog(
            title = radioStrings.countries,
            entries = radioState.countries,
            selected = radioState.selectedCountryCode,
            allLabel = radioStrings.worldwide,
            display = { entry -> localizedCountryName(entry.code, state.languageCode).ifBlank { entry.name } },
            value = com.luc4n3x.levyra.feature.radio.RadioDirectoryEntry::code,
            onDismiss = { countryPickerOpen = false },
            onSelect = {
                radio.selectCountry(it)
                countryPickerOpen = false
            }
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.appleRadioFavorites(
    radioState: com.luc4n3x.levyra.feature.radio.LiveRadioUiState,
    title: String,
    languageCode: String,
    radio: LiveRadioViewModel,
    play: (RadioStation) -> Unit
) {
    if (radioState.favorites.isEmpty()) return
    val listMode = radioState.stationListMode
    item(key = "apple-radio-favorites-title") {
        val layoutCopy = remember(languageCode) { spotifyLibraryCopy(languageCode) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { AppleRadioSectionTitle(title) }
            IconButton(onClick = { radio.setStationListMode(!listMode) }, modifier = Modifier.padding(end = 8.dp)) {
                Icon(
                    if (listMode) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.ViewList,
                    contentDescription = if (listMode) layoutCopy.gridView else layoutCopy.listView,
                    tint = AppleMusicDesign.accent
                )
            }
        }
    }
    if (listMode) {
        items(radioState.favorites, key = { "apple-radio-favorite-row-${it.uuid}" }) { station ->
            AppleRadioResultRow(station) { play(station) }
        }
    } else {
        item(key = "apple-radio-favorites") { AppleRadioRail(null, radioState.favorites, play) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.appleRadioSearchResults(
    radioState: com.luc4n3x.levyra.feature.radio.LiveRadioUiState,
    radioStrings: com.luc4n3x.levyra.ui.i18n.LevyraLiveRadioStrings,
    radio: LiveRadioViewModel,
    play: (RadioStation) -> Unit
) {
    val stations = radioState.stations
    items(stations, key = { "result-${it.uuid}" }) { station -> AppleRadioResultRow(station) { play(station) } }
    if (stations.isEmpty() || radioState.error != null) item(key = "apple-radio-search-status") {
        AppleRadioStatus(radioState.loading && stations.isEmpty(), radioState.error, radioStrings.noStations, radio::retry)
    }
    if (radioState.canLoadMore && !radioState.loading && radioState.error == null) item(key = "apple-radio-more") {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (radioState.loadingMore) CircularProgressIndicator(color = AppleMusicDesign.accent, modifier = Modifier.size(24.dp))
            else TextButton(onClick = radio::loadMore) { Text(radioStrings.loadMore, color = AppleMusicDesign.accent) }
        }
    }
}

@Composable
private fun AppleRadioGrid(grid: List<RadioStation>, play: (RadioStation) -> Unit) {
    Column(Modifier.padding(horizontal = AppleMusicDesign.gutter), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        grid.chunked(AppleRadioGridColumns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { station -> AppleRadioLogoTile(station, Modifier.weight(1f)) { play(station) } }
                repeat(AppleRadioGridColumns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AppleRadioStatus(loading: Boolean, error: String?, empty: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = AppleMusicDesign.gutter, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(color = AppleMusicDesign.accent)
        } else {
            Text(error ?: empty, color = AppleMusicDesign.secondaryLabel, style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = onRetry) {
                Text(com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings.current.exploreSamplesRetry, color = AppleMusicDesign.accent)
            }
        }
    }
}

@Composable
private fun AppleRadioSearchBar(query: String, hint: String, countryLabel: String, onQuery: (String) -> Unit, onPickCountry: () -> Unit) {
    val shape = RoundedCornerShape(AppleMusicDesign.cornerL)
    Column(Modifier.padding(horizontal = AppleMusicDesign.gutter), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(shape).background(AppleMusicDesign.fill).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = AppleMusicDesign.secondaryLabel, modifier = Modifier.size(20.dp))
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppleMusicDesign.label),
                cursorBrush = SolidColor(AppleMusicDesign.accent),
                modifier = Modifier.weight(1f).semantics { contentDescription = hint },
                decorationBox = { inner ->
                    if (query.isEmpty()) Text(hint, color = AppleMusicDesign.secondaryLabel, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    inner()
                }
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQuery("") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Cancel, contentDescription = com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings.current.clear, tint = AppleMusicDesign.secondaryLabel, modifier = Modifier.size(18.dp))
                }
            }
        }
        Row(
            Modifier.heightIn(min = 36.dp).clip(RoundedCornerShape(50)).background(AppleMusicDesign.fill)
                .levyraPressable(onClick = onPickCountry, onClickLabel = countryLabel, role = Role.Button, pressedScale = LevyraPressScale.Control)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Rounded.Public, contentDescription = null, tint = AppleMusicDesign.accent, modifier = Modifier.size(18.dp))
            Text(countryLabel, color = AppleMusicDesign.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = AppleMusicDesign.secondaryLabel, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AppleRadioResultRow(station: RadioStation, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .levyraPressable(onClick = onClick, onClickLabel = station.name, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .padding(horizontal = AppleMusicDesign.gutter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(AppleMusicDesign.cornerS)).background(AppleMusicDesign.fill), contentAlignment = Alignment.Center) {
            RadioArtwork(station, 52.dp, RoundedCornerShape(AppleMusicDesign.cornerS))
        }
        Column(Modifier.weight(1f)) {
            Text(station.name, color = AppleMusicDesign.label, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(listOf(station.country, station.tags.firstOrNull().orEmpty()).filter(String::isNotBlank).joinToString(" · "), color = AppleMusicDesign.secondaryLabel, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AppleRadioSectionTitle(title: String) {
    Text(
        title, color = AppleMusicDesign.label,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        maxLines = 2, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = AppleMusicDesign.gutter).semantics { heading() }
    )
}

@Composable
private fun AppleRadioLogoTile(station: RadioStation, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppleMusicDesign.cornerM)
    Box(
        modifier.aspectRatio(1f).clip(shape).background(AppleMusicDesign.fill).border(0.5.dp, AppleMusicDesign.separator, shape)
            .levyraPressable(onClick = onClick, onClickLabel = station.name, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .clearAndSetSemantics {
                contentDescription = station.name
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        RadioArtwork(station, AppleRadioRailWidth, shape)
    }
}

@Composable
private fun AppleRadioLiveCard(station: RadioStation, liveLabel: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppleMusicDesign.cornerL)
    Column(
        Modifier.width(AppleRadioLiveWidth).clip(shape).background(AppleMusicDesign.fill)
            .levyraPressable(onClick = onClick, onClickLabel = station.name, role = Role.Button, pressedScale = LevyraPressScale.Tile)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.5f), contentAlignment = Alignment.Center) {
            RadioArtwork(station, AppleRadioLiveWidth, RoundedCornerShape(0.dp))
        }
        Column(Modifier.fillMaxWidth().background(AppleMusicDesign.accent.copy(alpha = 0.85f)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                listOf(liveLabel, station.country).filter(String::isNotBlank).joinToString(" • "),
                color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium, maxLines = 1
            )
            Text(station.name, color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(station.tags.take(3).joinToString(", "), color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AppleRadioRail(title: String?, stations: List<RadioStation>, onPlay: (RadioStation) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        title?.let { AppleRadioSectionTitle(it) }
        LazyRow(contentPadding = PaddingValues(horizontal = AppleMusicDesign.gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(stations, key = { "rail-$title-${it.uuid}" }) { station ->
                Column(Modifier.width(AppleRadioRailWidth), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppleRadioLogoTile(station, Modifier.fillMaxWidth()) { onPlay(station) }
                    Text(station.name, color = AppleMusicDesign.label, style = MaterialTheme.typography.bodyMedium, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(station.country, color = AppleMusicDesign.secondaryLabel, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

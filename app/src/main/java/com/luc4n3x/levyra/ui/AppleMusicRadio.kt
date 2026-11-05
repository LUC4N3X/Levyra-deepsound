package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
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
        if (grid.isNotEmpty()) item(key = "apple-radio-grid") {
            Column(Modifier.padding(horizontal = AppleMusicDesign.gutter), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                grid.chunked(AppleRadioGridColumns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { station -> AppleRadioLogoTile(station, Modifier.weight(1f)) { play(station) } }
                        repeat(AppleRadioGridColumns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
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
        if (radioState.favorites.isNotEmpty()) item(key = "apple-radio-favorites") {
            AppleRadioRail(libraryCopy.favorites, radioState.favorites.take(AppleRadioRailLimit), play)
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
            .levyraPressable(onClick = onClick, onClickLabel = station.name, role = Role.Button, pressedScale = LevyraPressScale.Tile),
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
private fun AppleRadioRail(title: String, stations: List<RadioStation>, onPlay: (RadioStation) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppleRadioSectionTitle(title)
        LazyRow(contentPadding = PaddingValues(horizontal = AppleMusicDesign.gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(stations, key = { "rail-$title-${it.uuid}" }) { station ->
                Column(Modifier.width(AppleRadioRailWidth), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppleRadioLogoTile(station, Modifier.fillMaxWidth()) { onPlay(station) }
                    Text(station.name, color = AppleMusicDesign.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(station.country, color = AppleMusicDesign.secondaryLabel, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

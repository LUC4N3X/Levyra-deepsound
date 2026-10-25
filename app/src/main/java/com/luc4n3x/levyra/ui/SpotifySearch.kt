package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraSpotifySkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.SpotifyDesign

private val SpotifySearchFieldShape = RoundedCornerShape(6.dp)
private val SpotifySearchPlaceholder = Color(0xFF535353)
private const val SpotifyTopTileCount = 4
private const val SpotifyShortCount = 12

@Composable
internal fun SpotifySearchHeader(
    userName: String,
    query: String,
    recognitionAvailable: Boolean,
    recognitionBusy: Boolean,
    onSettings: () -> Unit,
    onQuery: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onVoice: () -> Unit,
    onRecognize: () -> Unit,
    focus: MutableState<Boolean>?
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraSpotifySkinStrings.forCode(strings.code) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) { focus?.value = focused }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.offset(x = (-8).dp).size(48.dp).clip(CircleShape)
                    .levyraPressable(onClick = onSettings, onClickLabel = strings.settings, role = Role.Button),
                contentAlignment = Alignment.Center
            ) { SpotifyAvatar(userName, 32.dp) }
            Text(
                strings.search, color = SpotifyDesign.text, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, modifier = Modifier.weight(1f).semantics { heading() }
            )
            if (recognitionAvailable) IconButton(onClick = onRecognize, modifier = Modifier.offset(x = 8.dp).size(48.dp)) {
                Icon(Icons.Rounded.GraphicEq, strings.recognizeMusic, tint = if (recognitionBusy) SpotifyDesign.green else SpotifyDesign.text, modifier = Modifier.size(28.dp))
            }
        }
        Row(
            Modifier.padding(vertical = 8.dp).fillMaxWidth().height(48.dp).clip(SpotifySearchFieldShape).background(SpotifyDesign.searchField),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, null, tint = SpotifyDesign.searchFieldText, modifier = Modifier.padding(start = 12.dp, end = 8.dp).size(26.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) Text(
                    copy.searchPrompt, color = SpotifySearchPlaceholder, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    interactionSource = interaction,
                    textStyle = TextStyle(color = SpotifyDesign.searchFieldText, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(SpotifyDesign.green),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = copy.searchPrompt }
                )
            }
            if (query.isNotEmpty()) IconButton(onClick = onClear, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Close, strings.clear, tint = SpotifyDesign.searchFieldText)
            } else IconButton(onClick = onVoice, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Mic, strings.voice, tint = SpotifySearchPlaceholder)
            }
        }
    }
}

internal class SpotifyBrowseEntry(val key: String, val label: String, val artworkUrl: String, val onClick: () -> Unit)

internal fun spotifyBrowseEntries(
    zones: List<ExploreZone>,
    categories: List<com.luc4n3x.levyra.domain.ExploreCategory>,
    categoryArtwork: Map<String, String>,
    onZone: (ExploreZone) -> Unit,
    onCategory: (com.luc4n3x.levyra.domain.ExploreCategory) -> Unit
): List<SpotifyBrowseEntry> {
    val zoneLabels = zones.map { it.label.lowercase() }.toSet()
    val zoneEntries = zones.map { zone -> SpotifyBrowseEntry("zone-${zone.id}", zone.label, "") { onZone(zone) } }
    val categoryEntries = categories
        .filter { it.title.isNotBlank() && it.params.isNotBlank() && it.title.lowercase() !in zoneLabels }
        .distinctBy { it.params }
        .map { category -> SpotifyBrowseEntry("category-${category.params}", category.title, categoryArtwork[category.params].orEmpty()) { onCategory(category) } }
    return zoneEntries + categoryEntries
}

@Composable
internal fun SpotifySearchBrowse(
    entries: List<SpotifyBrowseEntry>,
    shorts: List<Track>,
    onShort: (Track) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraSpotifySkinStrings.forCode(strings.code) }
    val top = entries.take(SpotifyTopTileCount)
    val rest = entries.drop(SpotifyTopTileCount)
    Column(Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SpotifyBrowseGrid(top)
        if (shorts.isNotEmpty()) {
            Text(
                copy.discoverNew, color = SpotifyDesign.text, style = MaterialTheme.typography.headlineSmall, maxLines = 2,
                modifier = Modifier.padding(top = 8.dp).semantics { heading() }
            )
            LazyRow(
                Modifier.layout { measurable, constraints ->
                    val bleed = SpotifyDesign.gutter.roundToPx()
                    val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + bleed * 2))
                    layout(constraints.maxWidth, placeable.height) { placeable.place(-bleed, 0) }
                },
                contentPadding = PaddingValues(horizontal = SpotifyDesign.gutter),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(shorts.take(SpotifyShortCount), key = { "spotify-short-${it.id}" }) { track ->
                    SpotifyShortCard(spotifyShortTag(track, copy.music), track, onClick = { onShort(track) })
                }
            }
        }
        if (rest.isNotEmpty()) SpotifyBrowseGrid(rest, Modifier.padding(top = 8.dp))
    }
}

internal fun spotifyShortTag(track: Track, musicLabel: String): String {
    val raw = track.artist.ifBlank { musicLabel }
    return "#" + raw.lowercase().filter { it.isLetterOrDigit() }
}

@Composable
private fun SpotifyBrowseGrid(entries: List<SpotifyBrowseEntry>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { entry -> SpotifySearchTile(entry, Modifier.weight(1f), entry.onClick) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SpotifySearchTile(entry: SpotifyBrowseEntry, modifier: Modifier, onClick: () -> Unit) {
    val title = entry.label
    val strings = LocalLevyraStrings.current
    val zone = remember(entry.key, strings) {
        com.luc4n3x.levyra.domain.ExploreCatalog.getZones(strings).firstOrNull { entry.key == "zone-${it.id}" }
    }
    val artworkUrl = if (zone != null) rememberExploreMoodArtworkUrl(zone) else entry.artworkUrl
    Box(
        modifier.height(68.dp).clip(RoundedCornerShape(SpotifyDesign.cornerLarge)).background(spotifyBrowseColor(title))
            .levyraPressable(onClick = onClick, onClickLabel = title, role = Role.Button)
    ) {
        if (artworkUrl.isNotBlank()) StableRemoteArtwork(
            artworkUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 14.dp, y = 10.dp).size(64.dp)
                .graphicsLayer { rotationZ = 25f }.clip(RoundedCornerShape(SpotifyDesign.cornerSmall))
        )
        Text(
            title, color = Color.White, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(0.7f).padding(start = 10.dp, top = 10.dp)
        )
    }
}

@Composable
internal fun SpotifyShortCard(tag: String, track: Track, onClick: () -> Unit) {
    Box(
        Modifier.width(128.dp).height(192.dp).clip(RoundedCornerShape(SpotifyDesign.cornerLarge)).background(SpotifyDesign.placeholder)
            .clickable(onClickLabel = track.title, role = Role.Button, onClick = onClick)
    ) {
        CoverImage(track, Modifier.fillMaxSize(), highRes = true)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f))))
        Text(
            tag, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, end = 10.dp, bottom = 12.dp)
        )
    }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.spotifySearchBrowseItem(
    state: com.luc4n3x.levyra.viewmodel.LevyraUiState,
    viewModel: com.luc4n3x.levyra.viewmodel.SearchViewModel
) {
    item(key = "spotify-search-browse", contentType = "spotify-search-browse") {
        val strings = LocalLevyraStrings.current
        val zones = remember(strings) { com.luc4n3x.levyra.domain.ExploreCatalog.getZones(strings) }
        LaunchedEffect(strings) { viewModel.ensureExplore(strings) }
        val shorts = state.exploreSamples
        val categories = state.exploreCategories
        val categoryArtwork = state.exploreCategoryArtwork
        val entries = remember(zones, categories, categoryArtwork) {
            spotifyBrowseEntries(zones, categories, categoryArtwork, viewModel::openExploreZone, viewModel::openExploreCategory)
        }
        SpotifySearchBrowse(entries, shorts) { track ->
            viewModel.openExploreSamples(shorts.indexOf(track))
        }
    }
}

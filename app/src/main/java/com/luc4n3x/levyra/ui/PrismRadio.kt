package com.luc4n3x.levyra.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.feature.radio.CustomRadioError
import com.luc4n3x.levyra.feature.radio.LiveRadioViewModel
import com.luc4n3x.levyra.feature.radio.RadioCategory
import com.luc4n3x.levyra.feature.radio.RadioDirectoryEntry
import com.luc4n3x.levyra.feature.radio.RadioStation
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraLiveRadioCatalog
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.radioLibraryCopy
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.ui.theme.PrismAccent
import com.luc4n3x.levyra.ui.theme.PrismDesign
import java.util.Locale

private val PrismRadioGutter = PrismDesign.gutter
private val PrismRadioHairline = Color.White.copy(alpha = 0.08f)
private val PrismRadioPanel = Color(0xFF0D0B10)
private val PrismRadioEdge = Brush.linearGradient(listOf(Color(0xFFF03A62), Color(0xFFD13FC0)))

@Suppress("ComplexMethod", "CognitiveComplexMethod")
@Composable
internal fun PrismRadioScreen(
    languageCode: String,
    currentStationId: String?,
    isPlaying: Boolean,
    title: String,
    onPlay: (RadioStation) -> Unit,
    onSearch: () -> Unit
) {
    val context = LocalContext.current
    val factory = remember(context.applicationContext) { LiveRadioViewModel.factory(context.applicationContext) }
    val viewModel: LiveRadioViewModel = viewModel(key = "levyra-live-radio", factory = factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalLevyraStrings.current
    val radioStrings = remember(languageCode) { LevyraLiveRadioCatalog.forCode(languageCode) }
    val libraryCopy = remember(languageCode) { radioLibraryCopy(languageCode) }
    val favoriteIds = remember(state.favorites) { state.favoriteIds }
    var countryPickerOpen by remember { mutableStateOf(false) }
    var languagePickerOpen by remember { mutableStateOf(false) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    var tunedId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(languageCode) { viewModel.activate(languageCode) }
    val dial = remember(state.stations) { state.stations.take(24) }
    val tuned = remember(state.stations, state.favorites, state.customStations, state.recent, tunedId, currentStationId) {
        val id = tunedId ?: currentStationId
        sequenceOf(state.stations, state.favorites, state.customStations, state.recent).flatten().firstOrNull { it.uuid == id }
    } ?: dial.firstOrNull()
    val tunedPlaying = tuned != null && currentStationId == tuned.uuid && isPlaying
    val isFavorite: (RadioStation) -> Boolean = { it.uuid.lowercase(Locale.ROOT) in favoriteIds }
    val tuneAndPlay: (RadioStation) -> Unit = { station ->
        tunedId = station.uuid
        onPlay(station)
    }
    val openEditor: (String?) -> Unit = { id ->
        editingId = id
        viewModel.clearCustomError()
        editorOpen = true
    }
    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize().background(LevyraBlack),
        contentPadding = PaddingValues(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(), bottom = 190.dp)
    ) {
        item(key = "prism-radio-head", contentType = "prism-radio-head") {
            Row(Modifier.fillMaxWidth().padding(start = PrismRadioGutter, end = 8.dp, top = 18.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PrismLiveDot(state.stations.isNotEmpty())
                        Spacer(Modifier.width(8.dp))
                        Text(radioStrings.live.uppercase(), color = LevyraMuted, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.6.sp)
                    }
                    Text(
                        title, color = LevyraText, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp),
                        maxLines = 1, modifier = Modifier.semantics { heading() }
                    )
                }
                IconButton(onClick = { openEditor(null) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Add, libraryCopy.add, tint = LevyraText) }
                IconButton(onClick = onSearch, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Search, strings.search, tint = LevyraText) }
            }
        }
        item(key = "prism-radio-onair", contentType = "prism-radio-onair") {
            PrismOnAir(tuned, tunedPlaying, tuned?.let(isFavorite) == true, state.loading && dial.isEmpty(), radioStrings.live, onPlay = { tuned?.let(onPlay) }) {
                tuned?.let(viewModel::toggleFavorite)
            }
        }
        if (dial.size > 1) {
            item(key = "prism-radio-dial", contentType = "prism-radio-dial") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = PrismRadioGutter),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 14.dp)
                ) {
                    items(dial, key = { "dial-${it.uuid}" }, contentType = { "prism-radio-tile" }) { station ->
                        PrismStationTile(station, station.uuid == tuned?.uuid, currentStationId == station.uuid && isPlaying) { tunedId = station.uuid }
                    }
                }
            }
        }
        item(key = "prism-radio-categories", contentType = "prism-radio-categories") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = PrismRadioGutter),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 22.dp)
            ) {
                items(RadioCategory.entries, key = { it.name }) { category ->
                    PrismCategoryChip(radioStrings.category(category), state.category == category) { viewModel.selectCategory(category) }
                }
            }
        }
        item(key = "prism-radio-filters", contentType = "prism-radio-filters") {
            Column(Modifier.fillMaxWidth().padding(horizontal = PrismRadioGutter).padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrismRadioField(state.query, radioStrings.searchHint, strings.clear, viewModel::setQuery)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrismRadioPlate(selectedCountryLabel(state, radioStrings), true, Modifier.weight(1f)) { countryPickerOpen = true }
                    PrismRadioPlate(state.selectedLanguage?.let { radioLanguageLabel(it, state.languages, languageCode) } ?: radioStrings.languages, false, Modifier.weight(1f)) { languagePickerOpen = true }
                }
            }
        }
        if (!editorOpen) state.customError?.let { reason ->
            item(key = "prism-radio-custom-error", contentType = "prism-radio-note") { PrismRadioNote(libraryCopy.error(reason)) }
        }
        if (state.query.isBlank()) {
            listOf("favorites" to (strings.favoritesPlain to state.favorites), "recent" to (strings.recent to state.recent)).forEach { (section, content) ->
                val (sectionTitle, stations) = content
                if (stations.isEmpty()) return@forEach
                item(key = "prism-radio-$section", contentType = "prism-radio-presets") {
                    Column {
                        PrismRadioHeader(sectionTitle)
                        LazyRow(contentPadding = PaddingValues(horizontal = PrismRadioGutter), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(stations, key = { "$section-${it.uuid}" }) { station ->
                                PrismPreset(station, currentStationId == station.uuid && isPlaying) { tuneAndPlay(station) }
                            }
                        }
                    }
                }
            }
            if (state.customStations.isNotEmpty()) {
                item(key = "prism-radio-custom-label", contentType = "prism-radio-header") { PrismRadioHeader(libraryCopy.custom) }
                itemsIndexed(state.customStations, key = { _, station -> "custom-${station.uuid}" }, contentType = { _, _ -> "prism-radio-row" }) { index, station ->
                    PrismStationRow(index + 1, station, isFavorite(station), currentStationId == station.uuid && isPlaying, { tuneAndPlay(station) }, { viewModel.toggleFavorite(station) }) {
                        var menuOpen by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, libraryCopy.edit, tint = LevyraMuted) }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text(libraryCopy.edit) }, onClick = { menuOpen = false; openEditor(station.uuid) })
                                DropdownMenuItem(text = { Text(strings.delete) }, onClick = { menuOpen = false; deletingId = station.uuid })
                            }
                        }
                    }
                }
            }
        }
        item(key = "prism-radio-list-label", contentType = "prism-radio-header") { PrismRadioHeader(mainSectionTitle(state, radioStrings)) }
        if (state.showingCachedCatalog && state.error != null) {
            item(key = "prism-radio-cached", contentType = "prism-radio-note") { PrismRadioNote(radioStrings.cachedNotice) }
        }
        itemsIndexed(state.stations, key = { _, station -> "list-${station.uuid}" }, contentType = { _, _ -> "prism-radio-row" }) { index, station ->
            PrismStationRow(index + 1, station, isFavorite(station), currentStationId == station.uuid && isPlaying, { tuneAndPlay(station) }, { viewModel.toggleFavorite(station) })
        }
        when {
            state.loading && state.stations.isEmpty() -> item(key = "prism-radio-loading", contentType = "prism-radio-note") {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { LevyraLoadingIndicator(color = LevyraPink, modifier = Modifier.size(36.dp)) }
            }
            state.stations.isEmpty() -> item(key = "prism-radio-empty", contentType = "prism-radio-note") {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(radioStrings.noStations, color = LevyraText, style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (state.error != null) radioStrings.internetRequired else libraryCopy.noResultsHelp,
                        color = LevyraMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp)
                    )
                    TextButton(onClick = viewModel::retry) { Text(strings.exploreSamplesRetry, color = LevyraPink) }
                    TextButton(onClick = { openEditor(null) }) { Text(libraryCopy.add, color = LevyraPink) }
                }
            }
        }
        if (state.error != null && state.stations.isNotEmpty()) {
            item(key = "prism-radio-page-error", contentType = "prism-radio-note") {
                Column {
                    PrismRadioNote(if (state.query.isNotBlank()) libraryCopy.partialSearch else radioStrings.internetRequired)
                    if (!state.canLoadMore) TextButton(onClick = viewModel::retry, modifier = Modifier.padding(start = 12.dp)) { Text(strings.exploreSamplesRetry, color = LevyraPink) }
                }
            }
        }
        if (state.searchLimitReached) {
            item(key = "prism-radio-limit", contentType = "prism-radio-note") { PrismRadioNote(libraryCopy.error(CustomRadioError.Limit)) }
        }
        if (state.canLoadMore && !state.loading) {
            item(key = "prism-radio-more", contentType = "prism-radio-more") {
                Box(Modifier.fillMaxWidth().height(72.dp).clickable(enabled = !state.loadingMore, onClick = viewModel::loadMore), contentAlignment = Alignment.Center) {
                    if (state.loadingMore) LevyraLoadingIndicator(color = LevyraPink, modifier = Modifier.size(24.dp))
                    else Text(radioStrings.loadMore, color = LevyraMuted, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
    if (editorOpen) CustomRadioDialog(
        station = state.customStations.firstOrNull { it.uuid == editingId }, saving = state.savingCustom, error = state.customError,
        onDismiss = { editorOpen = false }, onChanged = viewModel::clearCustomError,
        onSave = { name, url, image -> viewModel.saveCustom(name, url, image, state.customStations.firstOrNull { it.uuid == editingId }) { editorOpen = false } }
    )
    state.customStations.firstOrNull { it.uuid == deletingId }?.let { station ->
        AlertDialog(
            onDismissRequest = { deletingId = null }, title = { Text(station.name) }, text = { Text(libraryCopy.deleteConfirmation) },
            confirmButton = { TextButton(onClick = { viewModel.deleteCustom(station) { deletingId = null } }) { Text(strings.delete) } },
            dismissButton = { TextButton(onClick = { deletingId = null }) { Text(strings.cancel) } }
        )
    }
    if (countryPickerOpen) {
        RadioDirectoryDialog(
            title = radioStrings.countries, entries = state.countries, selected = state.selectedCountryCode, allLabel = radioStrings.worldwide, alphabeticalLanguageCode = languageCode,
            display = { entry -> localizedCountryName(entry.code, languageCode).ifBlank { entry.name } }, value = RadioDirectoryEntry::code,
            onDismiss = { countryPickerOpen = false },
            onSelect = {
                viewModel.selectCountry(it)
                countryPickerOpen = false
            }
        )
    }
    if (languagePickerOpen) {
        RadioDirectoryDialog(
            title = radioStrings.languages, entries = state.languages, selected = state.selectedLanguage, allLabel = radioStrings.worldwide, alphabeticalLanguageCode = languageCode,
            display = { localizedLanguageName(it, languageCode) }, value = RadioDirectoryEntry::name,
            onDismiss = { languagePickerOpen = false },
            onSelect = {
                viewModel.selectLanguage(it)
                languagePickerOpen = false
            }
        )
    }
}

@Composable
private fun PrismLiveDot(active: Boolean) {
    val animated = LocalAnimationsEnabled.current
    val pulse = if (active && animated) {
        rememberInfiniteTransition(label = "prism-live").animateFloat(0.35f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "prism-live-pulse")
    } else {
        null
    }
    Box(Modifier.size(8.dp).graphicsLayer { alpha = pulse?.value ?: 1f }.clip(CircleShape).background(PrismRadioEdge))
}

@Composable
private fun PrismEqualizer(active: Boolean, modifier: Modifier) {
    val animated = LocalAnimationsEnabled.current && active
    val transition = rememberInfiniteTransition(label = "prism-eq")
    val phases = listOf(520, 680, 430, 760).map { period ->
        if (animated) transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(period, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "prism-eq-$period") else null
    }
    Canvas(modifier.clearAndSetSemantics { }) {
        val bars = phases.size
        val gap = size.width * 0.1f
        val barWidth = (size.width - gap * (bars - 1)) / bars
        phases.forEachIndexed { index, phase ->
            val level = phase?.value ?: if (active) 0.6f else 0.18f
            val height = size.height * level
            drawRoundRect(PrismRadioEdge, Offset(index * (barWidth + gap), size.height - height), Size(barWidth, height), CornerRadius(barWidth / 2f))
        }
    }
}

@Suppress("ComplexMethod", "CognitiveComplexMethod")
@Composable
private fun PrismOnAir(
    station: RadioStation?,
    playing: Boolean,
    favorite: Boolean,
    loading: Boolean,
    live: String,
    onPlay: () -> Unit,
    onFavorite: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val shape = PrismDesign.cut(28.dp)
    Box(
        Modifier.fillMaxWidth().padding(horizontal = PrismRadioGutter).height(232.dp).clip(shape).background(PrismRadioPanel)
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
    ) {
        if (station != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = 0.55f }.blur(48.dp)) { RadioArtwork(station, 400.dp, RectangleShape) }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.85f)))))
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(LevyraPink.copy(alpha = 0.16f), Color.Transparent), center = Offset(0f, 0f), radius = 900f)))
        if (loading) {
            LevyraLoadingIndicator(color = LevyraPink, modifier = Modifier.align(Alignment.Center).size(36.dp))
            return@Box
        }
        if (station == null) return@Box
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(84.dp).clip(PrismDesign.cut(16.dp)).border(1.dp, Color.White.copy(alpha = 0.15f), PrismDesign.cut(16.dp))) {
                RadioArtwork(station, 84.dp, PrismDesign.cut(16.dp))
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onFavorite, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    if (favorite) strings.removeFromFavorites else strings.addToFavorites,
                    tint = if (favorite) LevyraPink else Color.White.copy(alpha = 0.75f)
                )
            }
        }
        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PrismEqualizer(playing, Modifier.width(20.dp).height(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(live.uppercase(), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp)
                }
                AnimatedContent(targetState = station, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, contentKey = { it.uuid }, label = "prism-onair") { value ->
                    Column {
                        Text(
                            value.name, color = Color.White, style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = (-0.4).sp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp)
                        )
                        val detail = prismStationDetail(value)
                        if (detail.isNotBlank()) Text(detail, color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.clip(CircleShape).clickable(role = Role.Button, onClickLabel = if (playing) strings.pause else strings.play, onClick = onPlay)
                    .semantics { if (playing) stateDescription = strings.playing }
            ) { PrismPlayDisc(playing, false, 64.dp) }
        }
    }
}

internal fun prismStationDetail(station: RadioStation): String =
    listOfNotNull(station.country.takeIf(String::isNotBlank), station.tags.firstOrNull()?.takeIf(String::isNotBlank), station.qualityLabel.takeIf(String::isNotBlank))
        .joinToString(" · ")

@Composable
private fun PrismStationTile(station: RadioStation, tuned: Boolean, playing: Boolean, onClick: () -> Unit) {
    val shape = PrismDesign.cut(14.dp)
    Column(
        Modifier.width(92.dp)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) {
                contentDescription = station.name
                selected = tuned
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(84.dp).clip(shape)
                .border(if (tuned) 2.dp else 1.dp, if (tuned) PrismRadioEdge else SolidColor(PrismRadioHairline), shape)
                .padding(if (tuned) 3.dp else 0.dp)
        ) {
            RadioArtwork(station, 84.dp, shape)
            if (playing) PrismEqualizer(true, Modifier.align(Alignment.BottomEnd).padding(6.dp).width(16.dp).height(12.dp))
        }
        Text(
            station.name, color = if (tuned) LevyraText else LevyraMuted, style = MaterialTheme.typography.labelMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp).clearAndSetSemantics { }
        )
    }
}

@Composable
private fun PrismCategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = PrismDesign.cut(12.dp)
    Box(
        Modifier.heightIn(min = 40.dp).clip(shape)
            .background(if (selected) PrismRadioEdge else SolidColor(Color.White.copy(alpha = 0.05f)))
            .border(1.dp, if (selected) Color.Transparent else PrismRadioHairline, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else LevyraMuted, style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium))
    }
}

@Composable
private fun PrismRadioField(query: String, hint: String, clearLabel: String, onQuery: (String) -> Unit) {
    val shape = PrismDesign.cut(14.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(shape).background(PrismRadioPanel)
            .border(1.dp, if (query.isNotEmpty()) PrismRadioEdge else SolidColor(PrismRadioHairline), shape)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = LevyraPink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(hint, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clearAndSetSemantics { })
            BasicTextField(
                value = query, onValueChange = onQuery, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White), cursorBrush = SolidColor(LevyraPink),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint }
            )
        }
        if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Rounded.Close, clearLabel, tint = Color.White.copy(alpha = 0.7f)) }
    }
}

@Composable
private fun PrismRadioPlate(label: String, globe: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = PrismDesign.cut(14.dp)
    Row(
        modifier.heightIn(min = 52.dp).clip(shape).background(Color.White.copy(alpha = 0.04f)).border(1.dp, PrismRadioHairline, shape)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Control).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (globe) {
            Icon(Icons.Rounded.Public, null, tint = LevyraPink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(label, color = LevyraText, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PrismRadioHeader(title: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = PrismRadioGutter).padding(top = 26.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(18.dp).height(3.dp).clip(CircleShape).background(PrismRadioEdge))
        Spacer(Modifier.width(10.dp))
        Text(title, color = LevyraText, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
    }
}

@Composable
private fun PrismPreset(station: RadioStation, playing: Boolean, onPlay: () -> Unit) {
    Column(
        Modifier.width(80.dp).levyraPressable(onClick = onPlay, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = station.name },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape)
                .border(2.dp, if (playing) PrismRadioEdge else SolidColor(PrismRadioHairline), CircleShape)
                .padding(4.dp)
        ) { RadioArtwork(station, 64.dp, CircleShape) }
        Text(
            station.name, color = if (playing) LevyraText else LevyraMuted, style = MaterialTheme.typography.labelMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp).clearAndSetSemantics { }
        )
    }
}

@Composable
private fun PrismStationRow(
    number: Int,
    station: RadioStation,
    favorite: Boolean,
    playing: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    trailing: @Composable () -> Unit = {}
) {
    val strings = LocalLevyraStrings.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp)
            .levyraPressable(onClick = onPlay, role = Role.Button, onClickLabel = strings.play, pressedScale = LevyraPressScale.Row)
            .semantics { if (playing) stateDescription = strings.playing }
            .padding(start = PrismRadioGutter, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(30.dp), contentAlignment = Alignment.CenterStart) {
            if (playing) PrismEqualizer(true, Modifier.width(16.dp).height(14.dp))
            else Text("%02d".format(Locale.ROOT, number), color = LevyraMuted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clearAndSetSemantics { })
        }
        Box(Modifier.size(52.dp).clip(PrismDesign.cut(12.dp))) { RadioArtwork(station, 52.dp, PrismDesign.cut(12.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(station.name, color = if (playing) LevyraPink else LevyraText, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val detail = prismStationDetail(station)
            if (detail.isNotBlank()) Text(detail, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onFavorite) {
            Icon(
                if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                if (favorite) strings.removeFromFavorites else strings.addToFavorites,
                tint = if (favorite) LevyraPink else LevyraMuted
            )
        }
        trailing()
    }
}

@Composable
private fun PrismRadioNote(text: String) {
    Text(text, color = LevyraMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(horizontal = PrismRadioGutter, vertical = 10.dp))
}

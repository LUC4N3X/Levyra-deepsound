package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.feature.radio.CustomRadioError
import com.luc4n3x.levyra.feature.radio.LiveRadioUiState
import com.luc4n3x.levyra.feature.radio.LiveRadioViewModel
import com.luc4n3x.levyra.feature.radio.RadioCategory
import com.luc4n3x.levyra.feature.radio.RadioDirectoryEntry
import com.luc4n3x.levyra.feature.radio.RadioStation
import com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import com.luc4n3x.levyra.ui.components.levyraExpressiveToggleCorner
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraLiveRadioCatalog
import com.luc4n3x.levyra.ui.i18n.LevyraLiveRadioStrings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.radioLibraryCopy
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import java.util.Locale

private val DefaultRadioGutter = 20.dp

@Composable
internal fun DefaultRadioScreen(
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
    var countryPickerOpen by remember { mutableStateOf(false) }
    var languagePickerOpen by remember { mutableStateOf(false) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(languageCode) { viewModel.activate(languageCode) }
    val onAir = remember(state.stations, state.favorites, state.customStations, state.recent, currentStationId) {
        defaultRadioOnAir(state, currentStationId)
    }
    val favoriteIds = remember(state.favorites) { state.favoriteIds }
    val tuning = remember(currentStationId, isPlaying, favoriteIds, onPlay, viewModel) {
        DefaultRadioTuning(currentStationId, isPlaying, favoriteIds, onPlay, viewModel::toggleFavorite)
    }
    val openEditor: (String?) -> Unit = { id ->
        editingId = id
        viewModel.clearCustomError()
        editorOpen = true
    }
    val listState = rememberLazyListState()
    val scrolled by remember(listState) { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 } }
    val scrimAlpha by animateFloatAsState(if (scrolled) 1f else 0f, label = "default-radio-status-scrim")
    Box(Modifier.fillMaxSize().background(LevyraBlack)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 190.dp)
        ) {
            item(key = "default-radio-stage", contentType = "default-radio-stage") {
                DefaultRadioStage(
                    title = title,
                    station = onAir.takeIf { state.query.isBlank() },
                    tuning = tuning,
                    place = selectedCountryLabel(state, radioStrings),
                    live = radioStrings.live,
                    strings = strings,
                    addLabel = libraryCopy.add,
                    onAdd = { openEditor(null) },
                    onSearch = onSearch,
                    onSurprise = { state.stations.randomOrNull()?.let(onPlay) }
                )
            }
            item(key = "default-radio-search", contentType = "default-radio-search") {
                DefaultRadioSearchField(state.query, radioStrings.searchHint, strings.clear, viewModel::setQuery)
            }
            item(key = "default-radio-genres", contentType = "default-radio-genres") {
                DefaultRadioGenres(state.category, radioStrings, viewModel::selectCategory)
            }
            if (!editorOpen) state.customError?.let { reason ->
                item(key = "default-radio-custom-error", contentType = "default-radio-notice") { RadioNotice(libraryCopy.error(reason), Icons.Rounded.Podcasts) }
            }
            if (state.query.isBlank()) {
                defaultRadioPresets(strings.favoritesPlain, strings.removeFromFavorites, state.favorites, tuning)
                defaultRadioRecent(strings.recent, state.recent, tuning)
                defaultRadioCustomStations(state.customStations, libraryCopy.custom, libraryCopy.edit, strings, tuning, openEditor) { deletingId = it }
            }
            defaultRadioChart(
                state = state,
                radioStrings = radioStrings,
                directory = DefaultRadioDirectory(
                    countryLabel = selectedCountryLabel(state, radioStrings),
                    languageLabel = state.selectedLanguage?.let { radioLanguageLabel(it, state.languages, languageCode) } ?: radioStrings.languages,
                    onCountry = { countryPickerOpen = true },
                    onLanguage = { languagePickerOpen = true }
                ),
                noResultsHelp = libraryCopy.noResultsHelp,
                addLabel = libraryCopy.add,
                strings = strings,
                tuning = tuning,
                onRetry = viewModel::retry
            ) { openEditor(null) }
            defaultRadioFooter(state, radioStrings, libraryCopy.partialSearch, libraryCopy.error(CustomRadioError.Limit), strings.exploreSamplesRetry, viewModel::retry, viewModel::loadMore)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .graphicsLayer { alpha = scrimAlpha }
                .background(Brush.verticalGradient(listOf(LevyraBlack.copy(alpha = 0.94f), LevyraBlack.copy(alpha = 0.6f))))
        )
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
            title = radioStrings.countries,
            entries = state.countries,
            selected = state.selectedCountryCode,
            allLabel = radioStrings.worldwide,
            alphabeticalLanguageCode = languageCode,
            display = { entry -> localizedCountryName(entry.code, languageCode).ifBlank { entry.name } },
            value = RadioDirectoryEntry::code,
            onDismiss = { countryPickerOpen = false },
            onSelect = {
                viewModel.selectCountry(it)
                countryPickerOpen = false
            }
        )
    }
    if (languagePickerOpen) {
        RadioDirectoryDialog(
            title = radioStrings.languages,
            entries = state.languages,
            selected = state.selectedLanguage,
            allLabel = radioStrings.worldwide,
            alphabeticalLanguageCode = languageCode,
            display = { localizedLanguageName(it, languageCode) },
            value = RadioDirectoryEntry::name,
            onDismiss = { languagePickerOpen = false },
            onSelect = {
                viewModel.selectLanguage(it)
                languagePickerOpen = false
            }
        )
    }
}

private class DefaultRadioTuning(
    private val currentStationId: String?,
    private val isPlaying: Boolean,
    private val favoriteIds: Set<String>,
    val onPlay: (RadioStation) -> Unit,
    val onFavorite: (RadioStation) -> Unit
) {
    fun isLive(station: RadioStation): Boolean = isPlaying && currentStationId == station.uuid
    fun isFavorite(station: RadioStation): Boolean = station.uuid.lowercase(Locale.ROOT) in favoriteIds
    fun favoriteAction(station: RadioStation, strings: LevyraStrings): DefaultRadioFavoriteAction {
        val active = isFavorite(station)
        return DefaultRadioFavoriteAction(active, if (active) strings.removeFromFavorites else strings.addToFavorites) { onFavorite(station) }
    }
}

private fun defaultRadioOnAir(state: LiveRadioUiState, currentStationId: String?): RadioStation? =
    sequenceOf(state.favorites, state.customStations, state.recent, state.stations).flatten().firstOrNull { it.uuid == currentStationId }
        ?: state.recent.firstOrNull()
        ?: state.stations.firstOrNull()

@Composable
private fun DefaultRadioStage(
    title: String,
    station: RadioStation?,
    tuning: DefaultRadioTuning,
    place: String,
    live: String,
    strings: LevyraStrings,
    addLabel: String,
    onAdd: () -> Unit,
    onSearch: () -> Unit,
    onSurprise: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val fallback = remember(colors.primary, colors.tertiary) { ArtworkPalette(colors.primary.toArgb(), colors.tertiary.toArgb()) }
    val palette by rememberArtworkPalette(
        paletteKey = station?.let { "radio-${it.uuid}" }.orEmpty(),
        artworkUrl = station?.safeFaviconUrl.orEmpty(),
        fallback = fallback,
        retainPreviousUntilLoaded = true
    )
    val tone = Color(palette.start)
    val wash = remember(tone, station != null) {
        Brush.verticalGradient(listOf(tone.copy(alpha = if (station != null) 0.30f else 0f), Color.Transparent))
    }
    Column(Modifier.fillMaxWidth().background(wash).statusBarsPadding()) {
        DefaultRadioMasthead(title, place, live, addLabel, strings.search, onAdd, onSearch)
        if (station != null) DefaultRadioOnAir(station, tone, tuning, live, strings, onSurprise)
    }
}

@Composable
private fun DefaultRadioMasthead(title: String, place: String, live: String, addLabel: String, searchLabel: String, onAdd: () -> Unit, onSearch: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(start = DefaultRadioGutter, end = 8.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                listOf(live, place).filter(String::isNotBlank).joinToString("  ·  ").uppercase(LocalConfiguration.current.locales[0]),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                title,
                color = colors.onSurface,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
        }
        IconButton(onClick = onAdd, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Add, addLabel, tint = colors.onSurface) }
        IconButton(onClick = onSearch, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Search, searchLabel, tint = colors.onSurface) }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultRadioOnAir(station: RadioStation, tone: Color, tuning: DefaultRadioTuning, live: String, strings: LevyraStrings, onSurprise: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val playing = tuning.isLive(station)
    val plate = lerp(tone, colors.surfaceContainerHighest, 0.15f)
    Column(Modifier.fillMaxWidth().padding(horizontal = DefaultRadioGutter).padding(top = 24.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(Modifier.size(136.dp), contentAlignment = Alignment.Center) {
                DefaultRadioBroadcastRings(tone, playing, Modifier.matchParentSize())
                Box(
                    Modifier.size(108.dp).clip(MaterialShapes.Cookie12Sided.toShape()).background(plate),
                    contentAlignment = Alignment.Center
                ) { RadioArtwork(station, 76.dp, CircleShape) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DefaultRadioOnAirTag(playing, if (playing) strings.nowPlaying else live, lerp(tone, colors.onSurface, 0.35f))
                Text(
                    station.name,
                    color = colors.onSurface,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, lineHeight = 30.sp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DefaultRadioSignalBars(station.bitrateKbps, tone)
                    RadioStationDetail(station)
                }
            }
        }
        DefaultRadioAirwave(tone, playing, Modifier.fillMaxWidth().height(44.dp).padding(vertical = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DefaultRadioPlayButton(playing, if (playing) strings.pause else strings.play, Modifier.weight(1f)) { tuning.onPlay(station) }
            DefaultRadioRoundAction(if (tuning.isFavorite(station)) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                if (tuning.isFavorite(station)) strings.removeFromFavorites else strings.addToFavorites, tuning.isFavorite(station)) { tuning.onFavorite(station) }
            DefaultRadioRoundAction(Icons.Rounded.Shuffle, strings.shuffle, active = false, onClick = onSurprise)
        }
    }
}

@Composable
private fun DefaultRadioOnAirTag(playing: Boolean, label: String, liveTone: Color) {
    val tint = if (playing) liveTone else RadioLiveRed
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (playing) {
            RadioEqualizer(tint)
        } else {
            val pulse = rememberRadioPulse()
            Box(Modifier.size(8.dp).graphicsLayer { alpha = pulse() }.background(tint, CircleShape))
        }
        Text(
            label.uppercase(LocalConfiguration.current.locales[0]),
            color = tint,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.6.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DefaultRadioPlayButton(playing: Boolean, label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val rest = levyraExpressiveToggleCorner(checked = playing, unchecked = 28.dp, checkedCorner = 16.dp, label = "default-radio-play-corner")
    val corner = levyraExpressiveCorner(interactionSource = interaction, rest = rest, pressed = 12.dp, label = "default-radio-play-press")
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(corner))
            .background(colors.onSurface)
            .levyraPressable(onClick = onClick, interactionSource = interaction, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
    ) {
        Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = colors.surface, modifier = Modifier.size(26.dp))
        Text(label, color = colors.surface, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DefaultRadioRoundAction(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val corner = levyraExpressiveCorner(interactionSource = interaction, rest = 28.dp, pressed = 16.dp, label = "default-radio-round-action")
    Box(
        Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(corner))
            .background(colors.surfaceContainerHigh)
            .levyraPressable(onClick = onClick, interactionSource = interaction, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = if (active) colors.primary else colors.onSurface, modifier = Modifier.size(24.dp)) }
}

@Composable
private fun DefaultRadioSearchField(query: String, hint: String, clearLabel: String, onQueryChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = DefaultRadioGutter).padding(top = 16.dp),
        singleLine = true,
        shape = CircleShape,
        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = colors.onSurfaceVariant) },
        trailingIcon = if (query.isNotEmpty()) {
            { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Rounded.Close, clearLabel, tint = colors.onSurfaceVariant) } }
        } else null,
        placeholder = { Text(hint, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        colors = TextFieldDefaults.colors(
            focusedTextColor = colors.onSurface,
            unfocusedTextColor = colors.onSurface,
            focusedContainerColor = colors.surfaceContainerHigh,
            unfocusedContainerColor = colors.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = colors.primary
        )
    )
}

@Composable
private fun DefaultRadioGenres(selected: RadioCategory, radioStrings: LevyraLiveRadioStrings, onSelect: (RadioCategory) -> Unit) {
    Column {
        DefaultRadioSectionTitle(radioStrings.categories)
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = DefaultRadioGutter),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().height(112.dp)
        ) {
            gridItems(RadioCategory.entries, key = { it.name }, contentType = { "default-radio-genre" }) { category ->
                DefaultRadioGenreKey(category, radioStrings.category(category), selected == category) { onSelect(category) }
            }
        }
    }
}

@Composable
private fun DefaultRadioDirectoryChip(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val corner = levyraExpressiveCorner(interactionSource = interaction, rest = 22.dp, pressed = 12.dp, label = "default-radio-directory-corner")
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(corner))
            .background(colors.surfaceContainerHigh)
            .levyraPressable(onClick = onClick, interactionSource = interaction, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .padding(start = 14.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text(
            label, color = colors.onSurface, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
        )
        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DefaultRadioSectionTitle(title: String) {
    Text(
        title,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(start = DefaultRadioGutter, end = DefaultRadioGutter, top = 28.dp, bottom = 14.dp).semantics { heading() }
    )
}

private fun LazyListScope.defaultRadioPresets(title: String, removeLabel: String, stations: List<RadioStation>, tuning: DefaultRadioTuning) {
    if (stations.isEmpty()) return
    item(key = "default-radio-presets", contentType = "default-radio-presets") {
        Column {
            DefaultRadioSectionTitle(title)
            LazyRow(contentPadding = PaddingValues(horizontal = DefaultRadioGutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(stations, key = { _, station -> "preset-${station.uuid}" }, contentType = { _, _ -> "default-radio-preset" }) { index, station ->
                    DefaultRadioPreset(index + 1, station, tuning.isLive(station), removeLabel, { tuning.onFavorite(station) }) { tuning.onPlay(station) }
                }
            }
        }
    }
}

private fun LazyListScope.defaultRadioRecent(title: String, stations: List<RadioStation>, tuning: DefaultRadioTuning) {
    if (stations.isEmpty()) return
    item(key = "default-radio-recent", contentType = "default-radio-recent") {
        Column {
            DefaultRadioSectionTitle(title)
            LazyRow(contentPadding = PaddingValues(horizontal = DefaultRadioGutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(stations, key = { "recent-${it.uuid}" }, contentType = { "default-radio-recent-tile" }) { station ->
                    DefaultRadioRecentTile(station, tuning.isLive(station)) { tuning.onPlay(station) }
                }
            }
        }
    }
}

@Composable
private fun DefaultRadioRecentTile(station: RadioStation, playing: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .width(232.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (playing) colors.primaryContainer else colors.surfaceContainer)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Control, onClickLabel = station.name)
            .semantics { selected = playing }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioArtwork(station, 56.dp, RoundedCornerShape(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                station.name, color = if (playing) colors.onPrimaryContainer else colors.onSurface, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            DefaultRadioSignalBars(station.bitrateKbps, if (playing) colors.onPrimaryContainer else colors.onSurfaceVariant, Modifier.padding(top = 6.dp))
        }
        if (playing) RadioEqualizer(colors.onPrimaryContainer, Modifier.padding(end = 8.dp))
    }
}

private fun LazyListScope.defaultRadioCustomStations(
    stations: List<RadioStation>,
    title: String,
    editLabel: String,
    strings: LevyraStrings,
    tuning: DefaultRadioTuning,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    if (stations.isEmpty()) return
    item(key = "default-radio-custom-title", contentType = "default-radio-title") { DefaultRadioSectionTitle(title) }
    items(stations, key = { "default-radio-custom-${it.uuid}" }, contentType = { "default-radio-row" }) { station ->
        DefaultRadioChartRow(null, station, tuning, strings) {
            DefaultRadioCustomMenu(editLabel, strings.delete, onEdit = { onEdit(station.uuid) }, onDelete = { onDelete(station.uuid) })
        }
    }
}

@Composable
private fun DefaultRadioCustomMenu(editLabel: String, deleteLabel: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Rounded.MoreVert, editLabel, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(editLabel) }, onClick = { open = false; onEdit() })
            DropdownMenuItem(text = { Text(deleteLabel) }, onClick = { open = false; onDelete() })
        }
    }
}

private class DefaultRadioDirectory(
    val countryLabel: String,
    val languageLabel: String,
    val onCountry: () -> Unit,
    val onLanguage: () -> Unit
)

private const val DefaultRadioPodiumSize = 5

private fun LazyListScope.defaultRadioChart(
    state: LiveRadioUiState,
    radioStrings: LevyraLiveRadioStrings,
    directory: DefaultRadioDirectory,
    noResultsHelp: String,
    addLabel: String,
    strings: LevyraStrings,
    tuning: DefaultRadioTuning,
    onRetry: () -> Unit,
    onAdd: () -> Unit
) {
    item(key = "default-radio-chart-header", contentType = "default-radio-chart-header") {
        DefaultRadioChartHeader(mainSectionTitle(state, radioStrings), directory)
    }
    if (state.showingCachedCatalog && state.error != null) {
        item(key = "default-radio-cache-notice", contentType = "default-radio-notice") { RadioNotice(radioStrings.cachedNotice, Icons.Rounded.Public) }
    }
    val podium = if (state.query.isBlank() && state.stations.size > DefaultRadioPodiumSize) state.stations.take(DefaultRadioPodiumSize) else emptyList()
    if (podium.isNotEmpty()) {
        item(key = "default-radio-podium", contentType = "default-radio-podium") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = DefaultRadioGutter),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                itemsIndexed(podium, key = { _, station -> "podium-${station.uuid}" }, contentType = { _, _ -> "default-radio-podium-card" }) { index, station ->
                    DefaultRadioPodiumCard(index + 1, station, tuning.isLive(station), tuning.favoriteAction(station, strings)) { tuning.onPlay(station) }
                }
            }
        }
    }
    val rest = state.stations.drop(podium.size)
    itemsIndexed(rest, key = { _, station -> "default-radio-station-${station.uuid}" }, contentType = { _, _ -> "default-radio-row" }) { index, station ->
        DefaultRadioChartRow(podium.size + index + 1, station, tuning, strings, actions = null)
    }
    when {
        state.loading && state.stations.isEmpty() -> item(key = "default-radio-loading", contentType = "default-radio-loading") {
            Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { LevyraLoadingIndicator(Modifier.size(56.dp)) }
        }
        state.stations.isEmpty() -> item(key = "default-radio-empty", contentType = "default-radio-empty") {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                RadioEmptyState(
                    title = radioStrings.noStations,
                    detail = if (state.error != null) radioStrings.internetRequired else noResultsHelp,
                    retry = strings.exploreSamplesRetry,
                    onRetry = onRetry
                )
                TextButton(onClick = onAdd) { Text(addLabel) }
            }
        }
    }
}

@Composable
private fun DefaultRadioChartHeader(title: String, directory: DefaultRadioDirectory) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(start = DefaultRadioGutter, end = DefaultRadioGutter, top = 32.dp, bottom = 16.dp)) {
        Text(
            title,
            color = colors.onSurface,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() }
        )
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DefaultRadioDirectoryChip(Icons.Rounded.Public, directory.countryLabel, Modifier.weight(1f, fill = false), directory.onCountry)
            DefaultRadioDirectoryChip(Icons.Rounded.Language, directory.languageLabel, Modifier.weight(1f, fill = false), directory.onLanguage)
        }
    }
}

@Composable
private fun DefaultRadioChartRow(
    rank: Int?,
    station: RadioStation,
    tuning: DefaultRadioTuning,
    strings: LevyraStrings,
    actions: (@Composable () -> Unit)?
) {
    val colors = MaterialTheme.colorScheme
    val playing = tuning.isLive(station)
    val hairline = colors.outlineVariant.copy(alpha = 0.45f)
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (playing) colors.primary.copy(alpha = 0.10f) else Color.Transparent)
            .levyraPressable(
                onClick = { tuning.onPlay(station) },
                role = Role.Button,
                pressedScale = LevyraPressScale.Row,
                onClickLabel = if (playing) strings.pause else strings.play
            )
            .semantics { selected = playing }
            .drawBehind {
                val inset = (if (rank != null) 112 else 84).dp.toPx()
                drawLine(hairline, Offset(inset, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            }
            .padding(start = DefaultRadioGutter, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DefaultRadioChartRowInfo(rank, station, playing, Modifier.weight(1f))
        if (playing) RadioEqualizer(colors.primary)
        DefaultRadioFavoriteToggle(tuning.isFavorite(station), strings) { tuning.onFavorite(station) }
        actions?.invoke()
    }
}

@Composable
private fun DefaultRadioChartRowInfo(rank: Int?, station: RadioStation, playing: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val accent = if (playing) colors.primary else colors.onSurfaceVariant
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        if (rank != null) {
            Text(
                rank.toString().padStart(2, '0'),
                color = accent,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
                modifier = Modifier.width(26.dp)
            )
        }
        RadioArtwork(station, 52.dp, RoundedCornerShape(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            RadioStationName(station.name, playing, if (playing) colors.primary else colors.onSurface, 16.sp, minLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DefaultRadioSignalBars(station.bitrateKbps, accent)
                RadioStationDetail(station)
            }
        }
    }
}

@Composable
private fun DefaultRadioFavoriteToggle(favorite: Boolean, strings: LevyraStrings, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(
            if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            contentDescription = if (favorite) strings.removeFromFavorites else strings.addToFavorites,
            tint = if (favorite) colors.primary else colors.onSurfaceVariant
        )
    }
}

private fun LazyListScope.defaultRadioFooter(
    state: LiveRadioUiState,
    radioStrings: LevyraLiveRadioStrings,
    partialSearch: String,
    searchLimit: String,
    retryLabel: String,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit
) {
    if (state.error != null && state.stations.isNotEmpty()) {
        item(key = "default-radio-page-error", contentType = "default-radio-notice") {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                RadioNotice(if (state.query.isNotBlank()) partialSearch else radioStrings.internetRequired, Icons.Rounded.Public)
                if (!state.canLoadMore) TextButton(onClick = onRetry) { Text(retryLabel) }
            }
        }
    }
    if (state.searchLimitReached) {
        item(key = "default-radio-search-limit", contentType = "default-radio-notice") { RadioNotice(searchLimit, Icons.Rounded.Search) }
    }
    if (state.canLoadMore && !state.loading) {
        item(key = "default-radio-load-more", contentType = "default-radio-load-more") {
            Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                FilledTonalButton(onClick = onLoadMore, enabled = !state.loadingMore, modifier = Modifier.heightIn(min = 48.dp)) {
                    if (state.loadingMore) LevyraLoadingIndicator(Modifier.size(24.dp)) else Text(radioStrings.loadMore)
                }
            }
        }
    }
}

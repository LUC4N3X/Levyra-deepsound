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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Radio
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.runtime.key
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Category
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
    onPlay: (RadioStation) -> Unit
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
    val radioGlass = rememberGlassBackdropState(rememberGlassBlurAllowed())
    Box(Modifier.fillMaxSize().background(LevyraBlack)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().glassBackdropSource(radioGlass),
            contentPadding = PaddingValues(bottom = 190.dp)
        ) {
            item(key = "default-radio-masthead", contentType = "default-radio-masthead") {
                DefaultRadioMasthead(title, libraryCopy.add) { openEditor(null) }
            }
            item(key = "default-radio-search", contentType = "default-radio-search") {
                DefaultRadioSearchField(state.query, radioStrings.searchHint, strings.clear, viewModel::setQuery)
            }
            onAir?.takeIf { state.query.isBlank() }?.let { station ->
                item(key = "default-radio-on-air", contentType = "default-radio-on-air") { DefaultRadioOnAirCard(station, tuning, strings, radioStrings.live) }
            }
            if (state.query.isBlank()) {
                item(key = "default-radio-dial", contentType = "default-radio-dial") {
                    val dialStations = remember(state.favorites, state.recent, state.stations) {
                        (state.favorites + state.recent + state.stations).distinctBy { it.uuid }.take(DefaultRadioDialLimit)
                    }
                    DefaultRadioDial(dialStations, currentStationId.takeIf { isPlaying }, onPlay)
                }
            }
            item(key = "default-radio-genres", contentType = "default-radio-genres") {
                DefaultRadioGenres(state.category, radioStrings, viewModel::selectCategory)
            }
            if (!editorOpen) state.customError?.let { reason ->
                item(key = "default-radio-custom-error", contentType = "default-radio-notice") { RadioNotice(libraryCopy.error(reason), Icons.Rounded.Podcasts) }
            }
            if (state.query.isBlank()) {
                defaultRadioPresets(strings.favoritesPlain, strings.removeFromFavorites, state.favorites, tuning)
                defaultRadioRecent(strings.recent, state.recent, strings, tuning)
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
        com.luc4n3x.levyra.ui.components.LevyraScrolledTopBar(radioGlass, listState, title, Icons.Rounded.Radio, com.luc4n3x.levyra.ui.components.LevyraPastelAmber) {
            IconButton(onClick = { openEditor(null) }) { Icon(Icons.Rounded.Add, contentDescription = libraryCopy.add, tint = MaterialTheme.colorScheme.onSurface) }
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
}

private fun defaultRadioOnAir(state: LiveRadioUiState, currentStationId: String?): RadioStation? =
    sequenceOf(state.favorites, state.customStations, state.recent, state.stations).flatten().firstOrNull { it.uuid == currentStationId }
        ?: state.recent.firstOrNull()
        ?: state.stations.firstOrNull()

@Composable
private fun DefaultRadioMasthead(title: String, addLabel: String, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = DefaultRadioGutter, end = 8.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        IconButton(onClick = onAdd) { Icon(Icons.Rounded.Add, contentDescription = addLabel, tint = MaterialTheme.colorScheme.onSurface) }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultRadioOnAirCard(station: RadioStation, tuning: DefaultRadioTuning, strings: LevyraStrings, liveLabel: String) {
    val colors = MaterialTheme.colorScheme
    val action = com.luc4n3x.levyra.ui.components.levyraUtilityColors(com.luc4n3x.levyra.ui.components.LevyraUtilitySeed)
    val playing = tuning.isLive(station)
    val favorite = tuning.isFavorite(station)
    val blur = com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities.current.heavyBlur && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val playCorner by animateDpAsState(
        if (playing) DefaultRadioHeroPlay / 2 else 24.dp,
        animationSpec = if (LocalAnimationsEnabled.current) androidx.compose.animation.core.spring() else androidx.compose.animation.core.snap(),
        label = "default-radio-hero-play"
    )
    Box(Modifier.fillMaxWidth()) {
        if (blur && station.safeFaviconUrl.isNotBlank()) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.verticalGradient(0f to Color.Transparent, 0.2f to Color.Black, 0.6f to Color.Black, 1f to Color.Transparent),
                            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                        )
                    }
            ) {
                coil3.compose.AsyncImage(
                    model = station.safeFaviconUrl,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.38f }.blur(90.dp, androidx.compose.ui.draw.BlurredEdgeTreatment.Rectangle)
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = DefaultRadioGutter).padding(top = 12.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DefaultRadioTransmitter(station, playing, action.accent)
            DefaultRadioOnAirLamp(playing, if (playing) liveLabel else strings.continueListening)
            Text(
                station.name,
                color = colors.onSurface,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            RadioStationDetail(station)
            Row(
                Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                FilledTonalIconButton(
                    onClick = { tuning.onFavorite(station) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(colors.surfaceContainerHighest, if (favorite) action.accent else colors.onSurface),
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = if (favorite) strings.removeFromFavorites else strings.addToFavorites
                    )
                }
                FilledIconButton(
                    onClick = { tuning.onPlay(station) },
                    shapes = IconButtonDefaults.shapes(shape = RoundedCornerShape(playCorner), pressedShape = RoundedCornerShape(18.dp)),
                    colors = IconButtonDefaults.filledIconButtonColors(action.accent, action.onAccent),
                    modifier = Modifier.size(DefaultRadioHeroPlay)
                ) {
                    Icon(
                        if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playing) strings.pause else strings.play,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(Modifier.size(52.dp))
            }
        }
    }
}

private val DefaultRadioHeroArtwork = 104.dp
private const val DefaultRadioDialLimit = 24
private val DefaultRadioHeroPlay = 76.dp

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
        DefaultRadioCategoryChips(selected, radioStrings::category, PaddingValues(horizontal = DefaultRadioGutter), onSelect)
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
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = DefaultRadioGutter, end = DefaultRadioGutter, top = 28.dp, bottom = 12.dp).semantics { heading() }
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

private fun LazyListScope.defaultRadioRecent(title: String, stations: List<RadioStation>, strings: LevyraStrings, tuning: DefaultRadioTuning) {
    if (stations.isEmpty()) return
    val visible = stations.take(DefaultRadioRecentLimit)
    item(key = "default-radio-recent-title", contentType = "default-radio-title") { DefaultRadioSectionTitle(title) }
    itemsIndexed(visible, key = { _, station -> "default-radio-recent-${station.uuid}" }, contentType = { _, _ -> "default-radio-row" }) { index, station ->
        DefaultRadioChartRow(null, station, tuning, strings, defaultRadioSegmentShape(index == 0, index == visible.lastIndex), actions = null)
    }
}

private const val DefaultRadioRecentLimit = 4

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
    itemsIndexed(stations, key = { _, station -> "default-radio-custom-${station.uuid}" }, contentType = { _, _ -> "default-radio-row" }) { index, station ->
        DefaultRadioChartRow(null, station, tuning, strings, defaultRadioSegmentShape(index == 0, index == stations.lastIndex)) {
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
    val rest = state.stations
    itemsIndexed(rest, key = { _, station -> "default-radio-station-${station.uuid}" }, contentType = { _, _ -> "default-radio-row" }) { index, station ->
        DefaultRadioChartRow(index + 1, station, tuning, strings, defaultRadioSegmentShape(index == 0, index == rest.lastIndex), actions = null)
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
    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp)) {
        DefaultRadioSectionTitle(title)
        Row(Modifier.padding(horizontal = DefaultRadioGutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    shape: Shape,
    actions: (@Composable () -> Unit)?
) {
    val colors = MaterialTheme.colorScheme
    val playing = tuning.isLive(station)
    val tone = com.luc4n3x.levyra.ui.components.levyraUtilityColors(com.luc4n3x.levyra.ui.components.LevyraUtilitySeed)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = DefaultRadioGutter, vertical = 1.5.dp)
            .clip(shape)
            .background(if (playing) tone.container else colors.surfaceContainer)
            .levyraPressable(
                onClick = { tuning.onPlay(station) },
                role = Role.Button,
                pressedScale = LevyraPressScale.Row,
                onClickLabel = if (playing) strings.pause else strings.play
            )
            .semantics { selected = playing }
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (rank != null) {
            Text(
                rank.toString(),
                color = accent,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(24.dp)
            )
        }
        RadioArtwork(station, 48.dp, RoundedCornerShape(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                station.name,
                color = if (playing) colors.primary else colors.onSurface,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            RadioStationDetail(station)
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

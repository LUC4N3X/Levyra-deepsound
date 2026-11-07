package com.luc4n3x.levyra.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.text.font.FontFamily
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
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.IvoryDesign
import java.util.Locale

private val IvoryRadioGutter = 20.dp
private const val IvoryDialSize = 24

@Suppress("ComplexMethod", "CognitiveComplexMethod")
@Composable
internal fun IvoryRadioScreen(
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
    val dial = remember(state.stations) { state.stations.take(IvoryDialSize) }
    val tuned = remember(state.stations, state.favorites, state.customStations, state.recent, tunedId, currentStationId) {
        val id = tunedId ?: currentStationId
        sequenceOf(state.stations, state.favorites, state.customStations, state.recent).flatten().firstOrNull { it.uuid == id }
    } ?: dial.firstOrNull()
    val tunedPlaying = tuned != null && currentStationId == tuned.uuid && isPlaying
    val tunedIndex = dial.indexOfFirst { it.uuid == tuned?.uuid }
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
        item(key = "ivory-radio-head", contentType = "ivory-radio-head") {
            Row(Modifier.fillMaxWidth().padding(start = IvoryRadioGutter, end = 8.dp, top = 18.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    IvoryLiveTag(radioStrings.live, state.stations.isNotEmpty())
                    Text(
                        title, color = LevyraText, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Normal,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
                    )
                }
                IconButton(onClick = { openEditor(null) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Add, libraryCopy.add, tint = LevyraText) }
                IconButton(onClick = onSearch, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Search, strings.search, tint = LevyraText) }
            }
        }
        item(key = "ivory-radio-onair", contentType = "ivory-radio-onair") {
            val channel = if (tunedIndex >= 0) "${(tunedIndex + 1).toString().padStart(2, '0')}/${dial.size.toString().padStart(2, '0')}" else ""
            IvoryOnAir(tuned, tunedPlaying, tuned?.let(isFavorite) == true, state.loading && dial.isEmpty(), channel, onPlay = { tuned?.let(onPlay) }) {
                tuned?.let(viewModel::toggleFavorite)
            }
        }
        if (dial.size > 1) {
            item(key = "ivory-radio-tuner", contentType = "ivory-radio-tuner") {
                IvoryTuner(dial.size, tunedIndex.coerceAtLeast(0), Modifier.padding(horizontal = IvoryRadioGutter).padding(top = 14.dp))
            }
            item(key = "ivory-radio-dial", contentType = "ivory-radio-dial") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = IvoryRadioGutter),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    itemsIndexed(dial, key = { _, station -> "dial-${station.uuid}" }, contentType = { _, _ -> "ivory-radio-tile" }) { index, station ->
                        IvoryStationTile(index + 1, station, station.uuid == tuned?.uuid, currentStationId == station.uuid && isPlaying) { tunedId = station.uuid }
                    }
                }
            }
        }
        item(key = "ivory-radio-categories", contentType = "ivory-radio-categories") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = IvoryRadioGutter),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 24.dp)
            ) {
                items(RadioCategory.entries, key = { it.name }) { category ->
                    IvoryCategoryChip(radioStrings.category(category), state.category == category) { viewModel.selectCategory(category) }
                }
            }
        }
        item(key = "ivory-radio-filters", contentType = "ivory-radio-filters") {
            Column(Modifier.fillMaxWidth().padding(horizontal = IvoryRadioGutter).padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                IvoryRadioField(state.query, radioStrings.searchHint, strings.clear, viewModel::setQuery)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IvoryRadioPlate(selectedCountryLabel(state, radioStrings), true, Modifier.weight(1f)) { countryPickerOpen = true }
                    IvoryRadioPlate(state.selectedLanguage ?: radioStrings.languages, false, Modifier.weight(1f)) { languagePickerOpen = true }
                }
            }
        }
        if (!editorOpen) state.customError?.let { reason ->
            item(key = "ivory-radio-custom-error", contentType = "ivory-radio-note") { IvoryRadioNote(libraryCopy.error(reason)) }
        }
        if (state.query.isBlank()) {
            listOf("favorites" to (strings.favoritesPlain to state.favorites), "recent" to (strings.recent to state.recent)).forEach { (section, content) ->
                val (sectionTitle, stations) = content
                if (stations.isEmpty()) return@forEach
                item(key = "ivory-radio-$section", contentType = "ivory-radio-presets") {
                    Column(Modifier.padding(top = 18.dp)) {
                        SkinSectionTitle(sectionTitle, IvoryRadioGutter)
                        LazyRow(contentPadding = PaddingValues(horizontal = IvoryRadioGutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(stations, key = { "$section-${it.uuid}" }) { station ->
                                IvoryPreset(station, currentStationId == station.uuid && isPlaying) { tuneAndPlay(station) }
                            }
                        }
                    }
                }
            }
            if (state.customStations.isNotEmpty()) {
                item(key = "ivory-radio-custom-label", contentType = "ivory-radio-header") {
                    Box(Modifier.padding(top = 18.dp)) { SkinSectionTitle(libraryCopy.custom, IvoryRadioGutter) }
                }
                itemsIndexed(state.customStations, key = { _, station -> "custom-${station.uuid}" }, contentType = { _, _ -> "ivory-radio-row" }) { index, station ->
                    IvoryStationRow(index + 1, station, isFavorite(station), currentStationId == station.uuid && isPlaying, { tuneAndPlay(station) }, { viewModel.toggleFavorite(station) }) {
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
        item(key = "ivory-radio-list-label", contentType = "ivory-radio-header") {
            Box(Modifier.padding(top = 18.dp)) { SkinSectionTitle(mainSectionTitle(state, radioStrings), IvoryRadioGutter) }
        }
        if (state.showingCachedCatalog && state.error != null) {
            item(key = "ivory-radio-cached", contentType = "ivory-radio-note") { IvoryRadioNote(radioStrings.cachedNotice) }
        }
        itemsIndexed(state.stations, key = { _, station -> "list-${station.uuid}" }, contentType = { _, _ -> "ivory-radio-row" }) { index, station ->
            IvoryStationRow(index + 1, station, isFavorite(station), currentStationId == station.uuid && isPlaying, { tuneAndPlay(station) }, { viewModel.toggleFavorite(station) })
        }
        when {
            state.loading && state.stations.isEmpty() -> item(key = "ivory-radio-loading", contentType = "ivory-radio-note") {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    LevyraLoadingIndicator(color = IvoryDesign.ink, modifier = Modifier.size(36.dp))
                }
            }
            state.stations.isEmpty() -> item(key = "ivory-radio-empty", contentType = "ivory-radio-note") {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(radioStrings.noStations.uppercase(), color = LevyraText, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(
                        if (state.error != null) radioStrings.internetRequired else libraryCopy.noResultsHelp,
                        color = LevyraMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp)
                    )
                    TextButton(onClick = viewModel::retry) { Text(strings.exploreSamplesRetry, color = IvoryDesign.ink) }
                    TextButton(onClick = { openEditor(null) }) { Text(libraryCopy.add, color = IvoryDesign.ink) }
                }
            }
        }
        if (state.error != null && state.stations.isNotEmpty()) {
            item(key = "ivory-radio-page-error", contentType = "ivory-radio-note") {
                Column {
                    IvoryRadioNote(if (state.query.isNotBlank()) libraryCopy.partialSearch else radioStrings.internetRequired)
                    if (!state.canLoadMore) TextButton(onClick = viewModel::retry, modifier = Modifier.padding(start = 12.dp)) {
                        Text(strings.exploreSamplesRetry, color = IvoryDesign.ink)
                    }
                }
            }
        }
        if (state.searchLimitReached) {
            item(key = "ivory-radio-limit", contentType = "ivory-radio-note") { IvoryRadioNote(libraryCopy.error(CustomRadioError.Limit)) }
        }
        if (state.canLoadMore && !state.loading) {
            item(key = "ivory-radio-more", contentType = "ivory-radio-more") {
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = IvoryRadioGutter, vertical = 12.dp).heightIn(min = 52.dp).clip(CircleShape).background(LevyraPanel)
                        .clickable(enabled = !state.loadingMore, role = Role.Button, onClick = viewModel::loadMore),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.loadingMore) LevyraLoadingIndicator(color = IvoryDesign.ink, modifier = Modifier.size(24.dp))
                    else Text(radioStrings.loadMore, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
            title = radioStrings.countries, entries = state.countries, selected = state.selectedCountryCode, allLabel = radioStrings.worldwide,
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
            title = radioStrings.languages, entries = state.languages, selected = state.selectedLanguage, allLabel = radioStrings.worldwide,
            display = RadioDirectoryEntry::name, value = RadioDirectoryEntry::name,
            onDismiss = { languagePickerOpen = false },
            onSelect = {
                viewModel.selectLanguage(it)
                languagePickerOpen = false
            }
        )
    }
}

@Composable
private fun IvoryLiveTag(label: String, active: Boolean) {
    val animated = LocalAnimationsEnabled.current
    val pulse = if (active && animated) {
        rememberInfiniteTransition(label = "ivory-live").animateFloat(0.3f, 1f, infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ivory-live-pulse")
    } else {
        null
    }
    Row(
        Modifier.clip(CircleShape).background(IvoryDesign.ink).padding(start = 14.dp, end = 16.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).graphicsLayer { alpha = pulse?.value ?: 1f }.clip(CircleShape).background(IvoryDesign.onInk))
        Spacer(Modifier.width(7.dp))
        Text(label.uppercase(), color = IvoryDesign.onInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp, maxLines = 1)
    }
}

@Composable
private fun IvoryMeter(active: Boolean, color: Color, modifier: Modifier) {
    val animated = LocalAnimationsEnabled.current && active
    val transition = rememberInfiniteTransition(label = "ivory-meter")
    val phases = listOf(480, 640, 410, 720, 560).map { period ->
        if (animated) transition.animateFloat(0.15f, 1f, infiniteRepeatable(tween(period, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ivory-meter-$period") else null
    }
    Canvas(modifier.clearAndSetSemantics { }) {
        val bars = phases.size
        val gap = size.width * 0.08f
        val barWidth = (size.width - gap * (bars - 1)) / bars
        phases.forEachIndexed { index, phase ->
            val level = phase?.value ?: if (active) 0.6f else 0.2f
            val height = size.height * level
            drawRect(color, Offset(index * (barWidth + gap), size.height - height), Size(barWidth, height))
        }
    }
}

@Suppress("ComplexMethod", "CognitiveComplexMethod")
@Composable
private fun IvoryOnAir(
    station: RadioStation?,
    playing: Boolean,
    favorite: Boolean,
    loading: Boolean,
    channel: String,
    onPlay: () -> Unit,
    onFavorite: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val accent = IvoryDesign.ink
    Box(Modifier.fillMaxWidth().padding(horizontal = IvoryRadioGutter).height(268.dp).clip(RoundedCornerShape(IvoryDesign.surfaceCorner)).background(LevyraPanel)) {
        if (station != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = 0.5f }.blur(40.dp)) { RadioArtwork(station, 420.dp, RectangleShape) }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.7f)))))
                if (loading) {
            LevyraLoadingIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center).size(36.dp))
            return@Box
        }
        if (station == null) return@Box
        if (channel.isNotBlank()) Text(
            channel, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(14.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 6.dp).clearAndSetSemantics { }
        )
        IconButton(onClick = onFavorite, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(48.dp)) {
            Icon(
                if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                if (favorite) strings.removeFromFavorites else strings.addToFavorites,
                tint = if (favorite) accent else Color.White
            )
        }
        Box(Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 52.dp).size(104.dp).clip(IvoryDesign.coverShape)) {
            RadioArtwork(station, 104.dp, IvoryDesign.coverShape)
        }
        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 20.dp, end = 18.dp, bottom = 18.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                if (playing) IvoryMeter(true, Color.White, Modifier.width(22.dp).height(14.dp))
                AnimatedContent(targetState = station, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, contentKey = { it.uuid }, label = "ivory-onair") { value ->
                    Column {
                        Text(
                            value.name, color = Color.White, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp)
                        )
                        val detail = prismStationDetail(value)
                        if (detail.isNotBlank()) Text(detail, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.size(68.dp).clip(CircleShape).background(IvoryDesign.onInk)
                    .clickable(role = Role.Button, onClickLabel = if (playing) strings.pause else strings.play, onClick = onPlay)
                    .semantics { if (playing) stateDescription = strings.playing },
                contentAlignment = Alignment.Center
            ) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = IvoryDesign.ink, modifier = Modifier.size(34.dp)) }
        }
    }
}

@Composable
private fun IvoryTuner(count: Int, index: Int, modifier: Modifier) {
    val accent = IvoryDesign.ink
    val tick = LevyraPanelSoft
    val major = LevyraMuted
    val position by animateFloatAsState(
        if (count > 1) index / (count - 1f) else 0f,
        animationSpec = if (LocalAnimationsEnabled.current) tween(420, easing = FastOutSlowInEasing) else tween(0),
        label = "ivory-tuner"
    )
    Canvas(modifier.fillMaxWidth().height(30.dp).clearAndSetSemantics { }) {
        val steps = (count - 1).coerceAtLeast(1) * 4
        val tickWidth = 2.dp.toPx()
        for (step in 0..steps) {
            val x = size.width * step / steps
            val isMajor = step % 4 == 0
            val height = if (isMajor) size.height * 0.6f else size.height * 0.32f
            drawRect(if (isMajor) major else tick, Offset(x - tickWidth / 2f, size.height - height), Size(tickWidth, height))
        }
        val needle = size.width * position
        val needleWidth = 4.dp.toPx()
        drawRect(accent, Offset(needle - needleWidth / 2f, 0f), Size(needleWidth, size.height))
    }
}

@Composable
private fun IvoryStationTile(number: Int, station: RadioStation, tuned: Boolean, playing: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(96.dp)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) {
                contentDescription = station.name
                selected = tuned
            }
    ) {
        Box(Modifier.size(96.dp).clip(IvoryDesign.coverShape).background(LevyraPanel).border(if (tuned) 3.dp else 0.dp, IvoryDesign.ink, IvoryDesign.coverShape)) {
            RadioArtwork(station, 96.dp, IvoryDesign.coverShape)
            if (playing) IvoryMeter(true, Color.White, Modifier.align(Alignment.BottomEnd).padding(8.dp).width(18.dp).height(12.dp))
            else Text(
                number.toString().padStart(2, '0'), color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomStart).background(Color.Black).padding(horizontal = 6.dp, vertical = 3.dp).clearAndSetSemantics { }
            )
        }
        Text(
            station.name, color = if (tuned) LevyraText else LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp).clearAndSetSemantics { }
        )
    }
}

@Composable
private fun IvoryCategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 48.dp).clip(CircleShape)
            .background(if (selected) IvoryDesign.ink else LevyraPanel)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) IvoryDesign.onInk else LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun IvoryRadioField(query: String, hint: String, clearLabel: String, onQuery: (String) -> Unit) {
    val accent = IvoryDesign.ink
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).background(LevyraPanel),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(4.dp).height(52.dp).background(if (query.isNotEmpty()) accent else LevyraPanelSoft))
        Spacer(Modifier.width(14.dp))
        Icon(Icons.Rounded.Search, null, tint = accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(hint, color = LevyraMuted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clearAndSetSemantics { })
            BasicTextField(
                value = query, onValueChange = onQuery, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = LevyraText), cursorBrush = SolidColor(accent),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint }
            )
        }
        if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Rounded.Close, clearLabel, tint = LevyraMuted) }
        else Spacer(Modifier.width(12.dp))
    }
}

@Composable
private fun IvoryRadioPlate(label: String, globe: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.heightIn(min = 52.dp).clip(RoundedCornerShape(IvoryDesign.controlCorner)).background(LevyraPanel)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Control).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (globe) {
            Icon(Icons.Rounded.Public, null, tint = IvoryDesign.ink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(label, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun IvoryPreset(station: RadioStation, playing: Boolean, onPlay: () -> Unit) {
    Column(
        Modifier.width(84.dp).levyraPressable(onClick = onPlay, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = station.name }
    ) {
        Box(Modifier.size(84.dp).clip(CircleShape).background(LevyraPanel).border(if (playing) 3.dp else 0.dp, IvoryDesign.ink, CircleShape)) {
            RadioArtwork(station, 84.dp, CircleShape)
        }
        Text(
            station.name, color = if (playing) LevyraText else LevyraMuted, style = MaterialTheme.typography.labelMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp).clearAndSetSemantics { }
        )
    }
}

@Composable
private fun IvoryStationRow(
    number: Int,
    station: RadioStation,
    favorite: Boolean,
    playing: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    trailing: @Composable () -> Unit = {}
) {
    val strings = LocalLevyraStrings.current
    val accent = IvoryDesign.ink
    Row(
        Modifier.fillMaxWidth().padding(horizontal = IvoryRadioGutter, vertical = 3.dp).heightIn(min = 72.dp).clip(RoundedCornerShape(IvoryDesign.tileCorner))
            .background(if (playing) LevyraPanelSoft else LevyraPanel)
            .levyraPressable(onClick = onPlay, role = Role.Button, onClickLabel = strings.play, pressedScale = LevyraPressScale.Row)
            .semantics { if (playing) stateDescription = strings.playing },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(4.dp).height(72.dp).background(if (playing) accent else Color.Transparent))
        Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
            if (playing) IvoryMeter(true, accent, Modifier.width(16.dp).height(14.dp))
            else Text(
                number.toString().padStart(2, '0'), color = LevyraMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                modifier = Modifier.clearAndSetSemantics { }
            )
        }
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(IvoryDesign.tileCorner))) { RadioArtwork(station, 52.dp, RoundedCornerShape(IvoryDesign.tileCorner)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(station.name, color = LevyraText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val detail = prismStationDetail(station)
            if (detail.isNotBlank()) Text(detail, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onFavorite) {
            Icon(
                if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                if (favorite) strings.removeFromFavorites else strings.addToFavorites,
                tint = if (favorite) accent else LevyraMuted
            )
        }
        trailing()
    }
}

@Composable
private fun IvoryRadioNote(text: String) {
    Text(text, color = LevyraMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(horizontal = IvoryRadioGutter, vertical = 10.dp))
}

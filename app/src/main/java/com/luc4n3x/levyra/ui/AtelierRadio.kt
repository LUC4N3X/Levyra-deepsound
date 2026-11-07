package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.ui.theme.atelierCarbon
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sin

private val AtelierWalnutTop = Color(0xFF1F1F23)
private val AtelierWalnutBottom = Color(0xFF0B0B0D)
private val AtelierGrille = Color(0xFF0E0E10)
internal val AtelierBrass: Color get() = AtelierDesign.accent
private val AtelierDialGlow = Color(0xFFFFC978)
private val AtelierNeedle = Color(0xFFD2452F)
private val AtelierEyeGreen = Color(0xFF7CF29A)
private val AtelierKnobBody = Brush.radialGradient(listOf(Color(0xFF34343A), Color(0xFF111114)))
private val AtelierCabinetShape = RoundedCornerShape(30.dp)
private val AtelierDialShape = RoundedCornerShape(12.dp)
private const val AtelierDialStations = 40

@Composable
internal fun AtelierRadioScreen(
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
    val dial = remember(state.stations) { state.stations.take(AtelierDialStations) }
    val tunedIndex = dial.indexOfFirst { it.uuid == (tunedId ?: currentStationId) }.coerceAtLeast(0)
    val tuned = dial.getOrNull(tunedIndex)
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
        modifier = Modifier.fillMaxSize().background(AtelierDesign.room),
        contentPadding = PaddingValues(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(), bottom = 190.dp)
    ) {
        item(key = "atelier-radio-masthead", contentType = "atelier-radio-masthead") {
            AtelierRadioMasthead(title, radioStrings.live, libraryCopy.add, strings.search, onAdd = { openEditor(null) }, onSearch = onSearch)
        }
        item(key = "atelier-radio-receiver", contentType = "atelier-radio-receiver") {
            AtelierReceiver(
                stations = dial,
                tunedIndex = tunedIndex,
                tuned = tuned,
                playing = tunedPlaying,
                favorite = tuned?.let(isFavorite) == true,
                loading = state.loading && dial.isEmpty(),
                onTune = { index -> dial.getOrNull(index)?.let { tunedId = it.uuid } },
                onPlay = { tuned?.let(onPlay) },
                onFavorite = { tuned?.let(viewModel::toggleFavorite) }
            )
        }
        item(key = "atelier-radio-bands", contentType = "atelier-radio-bands") {
            AtelierBandKeys(state.category, radioStrings::category, viewModel::selectCategory)
        }
        item(key = "atelier-radio-tuning", contentType = "atelier-radio-tuning") {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter).padding(top = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AtelierRadioSearchStrip(state.query, radioStrings.searchHint, strings.clear, viewModel::setQuery)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AtelierRadioPlate(selectedCountryLabel(state, radioStrings), true, Modifier.weight(1f)) { countryPickerOpen = true }
                    AtelierRadioPlate(state.selectedLanguage ?: radioStrings.languages, false, Modifier.weight(1f)) { languagePickerOpen = true }
                }
            }
        }
        if (!editorOpen) state.customError?.let { reason ->
            item(key = "atelier-radio-custom-error", contentType = "atelier-radio-note") { AtelierRadioNote(libraryCopy.error(reason)) }
        }
        if (state.query.isBlank()) {
            listOf(
                "favorites" to (strings.favoritesPlain to state.favorites),
                "recent" to (strings.recent to state.recent)
            ).forEach { (section, content) ->
                val (sectionTitle, stations) = content
                if (stations.isEmpty()) return@forEach
                item(key = "atelier-radio-presets-$section", contentType = "atelier-radio-presets") {
                    Column {
                        AtelierSectionLabel(sectionTitle)
                        LazyRow(contentPadding = AtelierRowPadding, horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
                            itemsIndexed(stations, key = { _, station -> "$section-${station.uuid}" }, contentType = { _, _ -> "atelier-radio-preset" }) { index, station ->
                                AtelierRadioPreset(index + 1, station, currentStationId == station.uuid && isPlaying) { tuneAndPlay(station) }
                            }
                        }
                    }
                }
            }
            if (state.customStations.isNotEmpty()) {
                item(key = "atelier-radio-custom-label", contentType = "atelier-label") { AtelierSectionLabel(libraryCopy.custom) }
                itemsIndexed(state.customStations, key = { _, station -> "custom-${station.uuid}" }, contentType = { _, _ -> "atelier-radio-log" }) { index, station ->
                    AtelierRadioLogRow(
                        index + 1, station, isFavorite(station), currentStationId == station.uuid && isPlaying,
                        onPlay = { tuneAndPlay(station) }, onFavorite = { viewModel.toggleFavorite(station) }
                    ) {
                        var menuOpen by remember { mutableStateOf(false) }
                        Box {
                            AtelierIconAction(Icons.Rounded.MoreVert, libraryCopy.edit, { menuOpen = true })
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text(libraryCopy.edit) }, onClick = { menuOpen = false; openEditor(station.uuid) })
                                DropdownMenuItem(text = { Text(strings.delete) }, onClick = { menuOpen = false; deletingId = station.uuid })
                            }
                        }
                    }
                }
            }
        }
        item(key = "atelier-radio-log-label", contentType = "atelier-label") { AtelierSectionLabel(mainSectionTitle(state, radioStrings)) }
        if (state.showingCachedCatalog && state.error != null) {
            item(key = "atelier-radio-cached", contentType = "atelier-radio-note") { AtelierRadioNote(radioStrings.cachedNotice) }
        }
        itemsIndexed(state.stations, key = { _, station -> "log-${station.uuid}" }, contentType = { _, _ -> "atelier-radio-log" }) { index, station ->
            AtelierRadioLogRow(
                index + 1, station, isFavorite(station), currentStationId == station.uuid && isPlaying,
                onPlay = { tuneAndPlay(station) }, onFavorite = { viewModel.toggleFavorite(station) }
            )
        }
        when {
            state.loading && state.stations.isEmpty() -> item(key = "atelier-radio-loading", contentType = "atelier-radio-note") {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    LevyraLoadingIndicator(color = AtelierDesign.accentBright, modifier = Modifier.size(36.dp))
                }
            }
            state.stations.isEmpty() -> item(key = "atelier-radio-empty", contentType = "atelier-radio-note") {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(radioStrings.noStations, fontFamily = AtelierSerif, fontSize = 20.sp, color = AtelierDesign.text, textAlign = TextAlign.Center)
                    Text(
                        if (state.error != null) radioStrings.internetRequired else libraryCopy.noResultsHelp,
                        style = MaterialTheme.typography.bodyMedium, color = AtelierDesign.textSecondary, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    AtelierTextAction(strings.exploreSamplesRetry, viewModel::retry)
                    AtelierTextAction(libraryCopy.add, { openEditor(null) })
                }
            }
        }
        if (state.error != null && state.stations.isNotEmpty()) {
            item(key = "atelier-radio-page-error", contentType = "atelier-radio-note") {
                Column {
                    AtelierRadioNote(if (state.query.isNotBlank()) libraryCopy.partialSearch else radioStrings.internetRequired)
                    if (!state.canLoadMore) AtelierTextAction(strings.exploreSamplesRetry, viewModel::retry, Modifier.padding(start = 8.dp))
                }
            }
        }
        if (state.searchLimitReached) {
            item(key = "atelier-radio-search-limit", contentType = "atelier-radio-note") { AtelierRadioNote(libraryCopy.error(CustomRadioError.Limit)) }
        }
        if (state.canLoadMore && !state.loading) {
            item(key = "atelier-radio-more", contentType = "atelier-radio-more") {
                Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                    if (state.loadingMore) LevyraLoadingIndicator(color = AtelierDesign.accentBright, modifier = Modifier.size(24.dp))
                    else AtelierTextAction(radioStrings.loadMore, viewModel::loadMore)
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
private fun AtelierRadioMasthead(title: String, live: String, addLabel: String, searchLabel: String, onAdd: () -> Unit, onSearch: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = AtelierDesign.gutter, end = 8.dp, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(atelierUppercase(live), style = MaterialTheme.typography.labelMedium, color = AtelierDesign.accentBright, maxLines = 1)
            Text(
                title, fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 34.sp, color = AtelierDesign.text,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
            )
        }
        AtelierIconAction(Icons.Rounded.Add, addLabel, onAdd, AtelierDesign.text)
        AtelierIconAction(Icons.Rounded.Search, searchLabel, onSearch, AtelierDesign.text)
    }
}

private fun Modifier.atelierWalnut(): Modifier = drawWithCache {
    val body = Brush.verticalGradient(listOf(AtelierWalnutTop, AtelierWalnutBottom))
    val grain = Path().apply {
        val rows = 22
        val step = 12.dp.toPx()
        val amplitude = 3.dp.toPx()
        for (row in 0 until rows) {
            val y = size.height * row / rows
            moveTo(0f, y)
            var x = 0f
            while (x <= size.width + step) {
                lineTo(x, y + sin((x / size.width) * 12.57f + row * 0.9f) * amplitude)
                x += step
            }
        }
    }
    val grainStroke = Stroke(0.8.dp.toPx())
    val sheen = Brush.linearGradient(
        listOf(Color.White.copy(alpha = 0.10f), Color.Transparent, Color.Black.copy(alpha = 0.25f)),
        start = Offset.Zero, end = Offset(size.width, size.height)
    )
    onDrawBehind {
        drawRect(body)
        drawRect(sheen)
        drawRect(Color.White.copy(alpha = 0.12f), size = Size(size.width, 1.dp.toPx()))
    }
}

private fun Modifier.atelierGrilleCloth(): Modifier = drawWithCache {
    val weave = Path().apply {
        val step = 4.dp.toPx()
        var x = -size.height
        while (x < size.width) {
            moveTo(x, 0f)
            lineTo(x + size.height, size.height)
            moveTo(x + size.height, 0f)
            lineTo(x, size.height)
            x += step
        }
    }
    val stroke = Stroke(0.7.dp.toPx())
    val shade = Brush.radialGradient(
        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
        center = Offset(size.width / 2f, size.height / 2f), radius = size.width * 0.62f
    )
    onDrawBehind {
        drawRect(AtelierGrille)
        drawPath(weave, AtelierBrass.copy(alpha = 0.10f), style = stroke)
        drawRect(shade)
    }
}

@Composable
private fun AtelierReceiver(
    stations: List<RadioStation>,
    tunedIndex: Int,
    tuned: RadioStation?,
    playing: Boolean,
    favorite: Boolean,
    loading: Boolean,
    onTune: (Int) -> Unit,
    onPlay: () -> Unit,
    onFavorite: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val count = stations.size
    val fraction = if (count <= 1) 0.5f else tunedIndex / (count - 1f)
    val animated = LocalAnimationsEnabled.current
    val needle by animateFloatAsState(
        fraction,
        if (animated) spring(dampingRatio = 0.62f, stiffness = 140f) else spring(stiffness = 10_000f),
        label = "atelier-radio-needle"
    )
    val glow by animateFloatAsState(if (playing) 1f else 0.25f, tween(if (animated) 900 else 0, easing = FastOutSlowInEasing), label = "atelier-radio-glow")
    Box(
        Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter)
            .graphicsLayer {
                shadowElevation = 24.dp.toPx()
                shape = AtelierCabinetShape
                clip = true
            }
            .atelierWalnut()
            .atelierCarbon()
            .border(1.dp, AtelierBrass.copy(alpha = 0.35f), AtelierCabinetShape)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(16.dp)).atelierGrilleCloth()
                    .border(2.dp, AtelierBrass.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "ATELIER", fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold,
                        fontSize = 22.sp, letterSpacing = 4.sp, color = AtelierBrass, modifier = Modifier.clearAndSetSemantics { }
                    )
                    Spacer(Modifier.width(16.dp))
                    AtelierMagicEye(playing, Modifier.size(34.dp))
                }
            }
            AtelierTuningDial(stations, needle, glow, tuned, playing, loading, onTune)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AtelierRadioKnob(fraction, strings.previous, enabled = tunedIndex > 0) { onTune(tunedIndex - 1) }
                Spacer(Modifier.weight(1f))
                AtelierHardwareKey(
                    label = if (playing) strings.pause else strings.play,
                    lit = playing,
                    onClick = onPlay,
                    modifier = Modifier.width(96.dp).height(58.dp)
                ) { AtelierPlayGlyph(playing, 28.dp) }
                Spacer(Modifier.width(4.dp))
                AtelierIconAction(
                    if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    if (favorite) strings.removeFromFavorites else strings.addToFavorites,
                    onFavorite,
                    if (favorite) AtelierBrass else AtelierPaper.copy(alpha = 0.7f)
                )
                Spacer(Modifier.weight(1f))
                AtelierRadioKnob(fraction, strings.next, enabled = tunedIndex < count - 1) { onTune(tunedIndex + 1) }
            }
        }
    }
}

@Composable
private fun AtelierRadioKnob(fraction: Float, label: String, enabled: Boolean, onClick: () -> Unit) {
    val angle by animateFloatAsState(-140f + 280f * fraction, label = "atelier-radio-knob")
    Box(
        Modifier.size(64.dp).clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(AtelierKnobBody)
                .border(2.dp, AtelierBrass.copy(alpha = if (enabled) 0.65f else 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AtelierCabinetKnob(angle, Modifier.size(40.dp))
        }
    }
}

@Composable
private fun AtelierMagicEye(active: Boolean, modifier: Modifier) {
    val animated = LocalAnimationsEnabled.current
    val sweep = if (active && animated) {
        rememberInfiniteTransition(label = "atelier-magic-eye")
            .animateFloat(36f, 150f, infiniteRepeatable(tween(780, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "atelier-magic-eye-sweep")
    } else {
        null
    }
    Canvas(modifier.clearAndSetSemantics { }) {
        val radius = size.minDimension / 2f
        drawCircle(Color(0xFF0B0F0B), radius)
        val open = sweep?.value ?: if (active) 110f else 20f
        val brush = Brush.radialGradient(
            listOf(AtelierEyeGreen.copy(alpha = if (active) 0.95f else 0.35f), AtelierEyeGreen.copy(alpha = 0.08f)),
            center = center, radius = radius
        )
        drawArc(brush, -90f + open / 2f, 360f - open, useCenter = true)
        drawCircle(Color(0xFF0B0F0B), radius * 0.28f)
        drawCircle(AtelierBrass.copy(alpha = 0.7f), radius, style = Stroke(1.5.dp.toPx()))
    }
}

@Composable
private fun AtelierTuningDial(
    stations: List<RadioStation>,
    needle: Float,
    glow: Float,
    tuned: RadioStation?,
    playing: Boolean,
    loading: Boolean,
    onTune: (Int) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val count = stations.size
    val currentOnTune by rememberUpdatedState(onTune)
    val inset = 18.dp
    Column(
        Modifier.fillMaxWidth().clip(AtelierDialShape)
            .drawWithCache {
                val paper = Brush.verticalGradient(listOf(AtelierPaper, Color(0xFFE2D4B6)))
                val lamp = Brush.radialGradient(
                    listOf(AtelierDialGlow.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.75f), radius = size.width * 0.6f
                )
                onDrawBehind {
                    drawRect(paper)
                    drawRect(lamp, alpha = glow)
                }
            }
            .border(3.dp, AtelierBrass, AtelierDialShape)
            .pointerInput(count) {
                if (count <= 1) return@pointerInput
                detectTapGestures { offset ->
                    val pad = inset.toPx()
                    val f = ((offset.x - pad) / (size.width - pad * 2)).coerceIn(0f, 1f)
                    currentOnTune((f * (count - 1)).roundToInt())
                }
            }
            .pointerInput(count) {
                if (count <= 1) return@pointerInput
                var position = 0f
                detectHorizontalDragGestures(
                    onDragStart = { start -> position = start.x },
                    onHorizontalDrag = { change, delta ->
                        change.consume()
                        position += delta
                        val pad = inset.toPx()
                        val f = ((position - pad) / (size.width - pad * 2)).coerceIn(0f, 1f)
                        currentOnTune((f * (count - 1)).roundToInt())
                    }
                )
            }
            .padding(top = 12.dp, bottom = 10.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = inset), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    tuned?.name ?: "· · ·", fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                    color = AtelierInk, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    tuned?.let(::atelierStationDetail).orEmpty().ifBlank { " " }, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic,
                    fontSize = 13.sp, color = AtelierInk.copy(alpha = 0.65f), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (loading) {
                LevyraLoadingIndicator(color = AtelierInk, modifier = Modifier.size(22.dp))
            } else {
                Box(
                    Modifier.size(12.dp).clip(CircleShape)
                        .background(if (playing) AtelierNeedle else AtelierInk.copy(alpha = 0.18f))
                        .border(1.dp, AtelierInk.copy(alpha = 0.4f), CircleShape)
                        .clearAndSetSemantics { }
                )
            }
        }
        Canvas(
            Modifier.fillMaxWidth().height(58.dp).padding(top = 8.dp)
                .semantics {
                    contentDescription = tuned?.name.orEmpty()
                    if (playing) stateDescription = strings.playing
                }
        ) {
            val pad = inset.toPx()
            val span = size.width - pad * 2
            val baseline = size.height * 0.62f
            drawLine(AtelierInk.copy(alpha = 0.6f), Offset(pad, baseline), Offset(size.width - pad, baseline), 1.dp.toPx())
            val ticks = 60
            for (tick in 0..ticks) {
                val x = pad + span * tick / ticks
                val major = tick % 10 == 0
                val tickHeight = when {
                    major -> 14.dp.toPx()
                    tick % 5 == 0 -> 9.dp.toPx()
                    else -> 5.dp.toPx()
                }
                drawLine(
                    AtelierInk.copy(alpha = if (major) 0.85f else 0.45f),
                    Offset(x, baseline - tickHeight), Offset(x, baseline),
                    if (major) 1.4.dp.toPx() else 0.8.dp.toPx()
                )
            }
            if (count > 1) {
                for (index in 0 until count) {
                    drawCircle(AtelierInk.copy(alpha = 0.55f), 1.6.dp.toPx(), Offset(pad + span * index / (count - 1f), baseline + 7.dp.toPx()))
                }
            }
            val nx = pad + span * needle
            drawRoundRect(AtelierNeedle.copy(alpha = 0.25f), Offset(nx - 4.dp.toPx(), 0f), Size(8.dp.toPx(), size.height), CornerRadius(4.dp.toPx()))
            drawLine(AtelierNeedle, Offset(nx, 0f), Offset(nx, size.height), 2.dp.toPx())
        }
    }
}

private fun atelierStationDetail(station: RadioStation): String =
    listOfNotNull(
        station.country.takeIf(String::isNotBlank),
        station.tags.firstOrNull()?.takeIf(String::isNotBlank),
        station.qualityLabel.takeIf(String::isNotBlank)
    ).joinToString(" · ")

@Composable
private fun AtelierBandKeys(selected: RadioCategory, label: (RadioCategory) -> String, onSelect: (RadioCategory) -> Unit) {
    val code = LocalLevyraStrings.current.code
    LazyRow(
        contentPadding = PaddingValues(horizontal = AtelierDesign.gutter),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 18.dp)
    ) {
        items(RadioCategory.entries, key = { it.name }, contentType = { "atelier-band-key" }) { category ->
            val active = category == selected
            val text = label(category)
            Column(
                Modifier.widthIn(min = 72.dp).height(64.dp)
                    .graphicsLayer { translationY = if (active) 3.dp.toPx() else 0f }
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(
                        Brush.verticalGradient(
                            if (active) listOf(Color(0xFFCFC2A6), Color(0xFFBDAE8F)) else listOf(AtelierPaper, Color(0xFFDCCDAE))
                        )
                    )
                    .drawWithCache {
                        val edge = 4.dp.toPx()
                        onDrawWithContent {
                            drawContent()
                            if (!active) drawRect(Color.Black.copy(alpha = 0.28f), Offset(0f, size.height - edge), Size(size.width, edge))
                        }
                    }
                    .clickable(role = Role.Tab, onClick = { onSelect(category) })
                    .semantics {
                        this.selected = active
                        contentDescription = text
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    Modifier.width(18.dp).height(3.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (active) AtelierNeedle else AtelierInk.copy(alpha = 0.15f))
                )
                Text(
                    text.uppercase(Locale.forLanguageTag(code)), fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                    letterSpacing = 1.2.sp, color = AtelierInk, maxLines = 1, modifier = Modifier.clearAndSetSemantics { }
                )
            }
        }
    }
}

@Composable
private fun AtelierRadioSearchStrip(query: String, hint: String, clearLabel: String, onQuery: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(AtelierDesign.capsuleShape)
            .background(AtelierDesign.panel).border(1.dp, AtelierDesign.hairlineStrong, AtelierDesign.capsuleShape)
            .padding(start = 18.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = AtelierDesign.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    hint, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 16.sp, color = AtelierDesign.textFaint,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clearAndSetSemantics { }
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = AtelierDesign.text),
                cursorBrush = SolidColor(AtelierDesign.accent),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint }
            )
        }
        if (query.isNotEmpty()) AtelierIconAction(Icons.Rounded.Close, clearLabel, { onQuery("") })
    }
}

@Composable
private fun AtelierRadioPlate(label: String, globe: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.heightIn(min = 52.dp).clip(AtelierDesign.cardShape)
            .background(Brush.verticalGradient(listOf(AtelierDesign.panelRaised, AtelierDesign.cabinet)))
            .border(1.dp, AtelierBrass.copy(alpha = 0.4f), AtelierDesign.cardShape)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (globe) {
            Icon(Icons.Rounded.Public, null, tint = AtelierBrass, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(label, fontFamily = AtelierSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AtelierDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AtelierRadioPreset(number: Int, station: RadioStation, playing: Boolean, onPlay: () -> Unit) {
    Column(
        Modifier.width(96.dp)
            .levyraPressable(onClick = onPlay, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = station.name },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(88.dp)) {
            Box(
                Modifier.align(Alignment.Center).size(84.dp).clip(CircleShape).background(AtelierKnobBody)
                    .border(2.dp, if (playing) AtelierDesign.accentBright else AtelierBrass.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                RadioArtwork(station, 60.dp, CircleShape)
            }
            Box(
                Modifier.align(Alignment.TopStart).size(26.dp).clip(CircleShape).background(AtelierPaper)
                    .border(1.dp, AtelierInk.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("$number", fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AtelierInk)
            }
        }
        Text(
            station.name, fontFamily = AtelierSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            color = if (playing) AtelierDesign.accentBright else AtelierDesign.text,
            maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun AtelierRadioLogRow(
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
        Modifier.fillMaxWidth().heightIn(min = AtelierDesign.rowHeight + 8.dp).atelierBottomRule()
            .levyraPressable(onClick = onPlay, role = Role.Button, onClickLabel = strings.play, pressedScale = LevyraPressScale.Row)
            .semantics { if (playing) stateDescription = strings.playing }
            .padding(start = AtelierDesign.gutter, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "%02d".format(Locale.ROOT, number), fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 18.sp,
            color = if (playing) AtelierDesign.accentBright else AtelierDesign.textFaint, modifier = Modifier.width(40.dp).clearAndSetSemantics { }
        )
        Box(Modifier.size(48.dp).clip(CircleShape).border(1.dp, AtelierBrass.copy(alpha = 0.45f), CircleShape), contentAlignment = Alignment.Center) {
            RadioArtwork(station, 46.dp, CircleShape)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                station.name, fontFamily = AtelierSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                color = if (playing) AtelierDesign.accentBright else AtelierDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            val detail = atelierStationDetail(station)
            if (detail.isNotBlank()) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = AtelierDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (playing) {
            Box(Modifier.padding(horizontal = 6.dp).size(8.dp).clip(CircleShape).background(AtelierNeedle).clearAndSetSemantics { })
        }
        AtelierIconAction(
            if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            if (favorite) strings.removeFromFavorites else strings.addToFavorites,
            onFavorite,
            if (favorite) AtelierBrass else AtelierDesign.textSecondary
        )
        trailing()
    }
}

@Composable
private fun AtelierRadioNote(text: String) {
    Text(
        text, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 14.sp, color = AtelierDesign.textSecondary,
        modifier = Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter, vertical = 10.dp)
    )
}

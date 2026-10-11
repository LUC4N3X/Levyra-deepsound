package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.feature.radio.LiveRadioArtworkLoader
import com.luc4n3x.levyra.feature.radio.LiveRadioArtworkResolver
import com.luc4n3x.levyra.feature.radio.LiveRadioUiState
import com.luc4n3x.levyra.feature.radio.LiveRadioViewModel
import com.luc4n3x.levyra.feature.radio.RadioCategory
import com.luc4n3x.levyra.feature.radio.RadioDirectoryEntry
import com.luc4n3x.levyra.feature.radio.RadioStation
import com.luc4n3x.levyra.ui.i18n.LevyraLiveRadioCatalog
import com.luc4n3x.levyra.ui.i18n.LevyraLiveRadioStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import java.util.Locale
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.selected
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import com.luc4n3x.levyra.ui.components.levyraExpressiveToggleCorner
import com.luc4n3x.levyra.ui.components.levyraGroupedListShape
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.components.rememberLevyraPlayMorphShape
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.toShape
import androidx.compose.ui.graphics.graphicsLayer

@Composable
internal fun LiveRadioScreen(
    languageCode: String,
    currentStationId: String?,
    isPlaying: Boolean,
    backEnabled: Boolean,
    onBack: () -> Unit,
    onPlay: (RadioStation) -> Unit
) {
    BackHandler(enabled = backEnabled, onBack = onBack)
    val context = androidx.compose.ui.platform.LocalContext.current
    val factory = remember(context.applicationContext) { LiveRadioViewModel.factory(context.applicationContext) }
    val viewModel: LiveRadioViewModel = composeViewModel(key = "levyra-live-radio", factory = factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalLevyraStrings.current
    val radioStrings = remember(languageCode) { LevyraLiveRadioCatalog.forCode(languageCode) }
    val favoriteIds = remember(state.favorites) {
        state.favorites.mapTo(hashSetOf()) { it.uuid.lowercase(Locale.ROOT) }
    }
    var countryPickerOpen by remember { mutableStateOf(false) }
    var languagePickerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(languageCode) { viewModel.activate(languageCode) }

    Box(modifier = Modifier.fillMaxSize().background(LevyraBlack)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 82.dp,
                bottom = 190.dp
            )
        ) {
            item(key = "live-radio-intro") {
                RadioHero(
                    live = radioStrings.live,
                    subtitle = radioStrings.subtitle,
                    query = state.query,
                    searchHint = radioStrings.searchHint,
                    onQueryChange = viewModel::setQuery
                )
            }

            item(key = "live-radio-categories-label") {
                RadioSectionTitle(radioStrings.categories)
            }
            item(key = "live-radio-categories") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(RadioCategory.entries, key = { it.name }) { category ->
                        RadioChoiceChip(
                            label = radioStrings.category(category),
                            selected = state.category == category,
                            onClick = { viewModel.selectCategory(category) }
                        )
                    }
                }
            }

            item(key = "live-radio-directory-filters") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RadioDirectoryButton(
                        modifier = Modifier.weight(1f),
                        label = selectedCountryLabel(state, radioStrings),
                        icon = { Icon(Icons.Rounded.Public, null, modifier = Modifier.size(17.dp)) },
                        onClick = { countryPickerOpen = true }
                    )
                    RadioDirectoryButton(
                        modifier = Modifier.weight(1f),
                        label = state.selectedLanguage ?: radioStrings.languages,
                        icon = null,
                        onClick = { languagePickerOpen = true }
                    )
                }
            }

            if (state.favorites.isNotEmpty() && state.query.isBlank()) {
                item(key = "live-radio-favorites-title") { RadioSectionTitle(strings.favoritesPlain) }
                item(key = "live-radio-favorites") {
                    RadioStationRail(
                        stations = state.favorites,
                        favoriteIds = favoriteIds,
                        currentStationId = currentStationId,
                        isPlaying = isPlaying,
                        live = radioStrings.live,
                        playLabel = strings.play,
                        addFavoriteLabel = strings.addToFavorites,
                        removeFavoriteLabel = strings.removeFromFavorites,
                        onPlay = { station ->
                            viewModel.recordPlayed(station)
                            onPlay(station)
                        },
                        onFavorite = viewModel::toggleFavorite
                    )
                }
            }

            if (state.recent.isNotEmpty() && state.query.isBlank()) {
                item(key = "live-radio-recent-title") { RadioSectionTitle(strings.recent) }
                item(key = "live-radio-recent") {
                    RadioStationRail(
                        stations = state.recent,
                        favoriteIds = favoriteIds,
                        currentStationId = currentStationId,
                        isPlaying = isPlaying,
                        live = radioStrings.live,
                        playLabel = strings.play,
                        addFavoriteLabel = strings.addToFavorites,
                        removeFavoriteLabel = strings.removeFromFavorites,
                        onPlay = { station ->
                            viewModel.recordPlayed(station)
                            onPlay(station)
                        },
                        onFavorite = viewModel::toggleFavorite
                    )
                }
            }

            item(key = "live-radio-main-title") {
                RadioSectionTitle(mainSectionTitle(state, radioStrings))
            }

            if (state.showingCachedCatalog && state.error != null) {
                item(key = "live-radio-cache-notice") {
                    RadioNotice(radioStrings.cachedNotice, Icons.Rounded.Public)
                }
            }

            val featured = state.stations.firstOrNull()
            if (featured != null) {
                item(key = "live-radio-featured-${featured.uuid}") {
                    FeaturedRadioStation(
                        station = featured,
                        favorite = featured.uuid.lowercase(Locale.ROOT) in favoriteIds,
                        playing = currentStationId == featured.uuid && isPlaying,
                        live = radioStrings.live,
                        playLabel = strings.play,
                        addFavoriteLabel = strings.addToFavorites,
                        removeFavoriteLabel = strings.removeFromFavorites,
                        onPlay = {
                            viewModel.recordPlayed(featured)
                            onPlay(featured)
                        },
                        onFavorite = { viewModel.toggleFavorite(featured) }
                    )
                }
            }

            val listedStations = state.stations.drop(1)
            itemsIndexed(
                items = listedStations,
                key = { _, station -> "live-radio-station-${station.uuid}" },
                contentType = { _, _ -> "live-radio-station" }
            ) { index, station ->
                RadioStationRow(
                    index = index,
                    count = listedStations.size,
                    station = station,
                    favorite = station.uuid.lowercase(Locale.ROOT) in favoriteIds,
                    playing = currentStationId == station.uuid && isPlaying,
                    live = radioStrings.live,
                    playLabel = strings.play,
                    addFavoriteLabel = strings.addToFavorites,
                    removeFavoriteLabel = strings.removeFromFavorites,
                    onPlay = {
                        viewModel.recordPlayed(station)
                        onPlay(station)
                    },
                    onFavorite = { viewModel.toggleFavorite(station) }
                )
            }

            if (state.loading && state.stations.isEmpty()) {
                item(key = "live-radio-loading") {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = LevyraCyan, strokeWidth = 3.dp) }
                }
            } else if (state.stations.isEmpty()) {
                item(key = "live-radio-empty") {
                    RadioEmptyState(
                        title = radioStrings.noStations,
                        detail = state.error?.let { radioStrings.internetRequired },
                        retry = strings.exploreSamplesRetry,
                        onRetry = viewModel::retry
                    )
                }
            }

            if (state.canLoadMore && state.query.isBlank() && state.stations.isNotEmpty()) {
                item(key = "live-radio-load-more") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(74.dp)
                            .clickable(enabled = !state.loadingMore, onClick = viewModel::loadMore),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.loadingMore) CircularProgressIndicator(color = LevyraCyan, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        else Text(radioStrings.loadMore, color = LevyraMuted, fontSize = 13.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .background(LevyraBlack.copy(alpha = 0.96f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(LevyraPanel, CircleShape)
                    .semantics { role = Role.Button }
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, tint = LevyraText, modifier = Modifier.size(21.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(radioStrings.title, color = LevyraText, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text(radioStrings.live, color = LevyraCyan, fontSize = 10.5.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (countryPickerOpen) {
        RadioDirectoryDialog(
            title = radioStrings.countries,
            entries = state.countries,
            selected = state.selectedCountryCode,
            allLabel = radioStrings.worldwide,
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
            display = RadioDirectoryEntry::name,
            value = RadioDirectoryEntry::name,
            onDismiss = { languagePickerOpen = false },
            onSelect = {
                viewModel.selectLanguage(it)
                languagePickerOpen = false
            }
        )
    }
}

@Composable
private fun RadioSectionTitle(title: String) {
    Text(
        text = title,
        color = LevyraText,
        fontSize = 22.sp,
        lineHeight = LevyraTypeRhythm.lineHeight(22.sp),
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.5).sp,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 10.dp).semantics { heading() }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RadioHero(
    live: String,
    subtitle: String,
    query: String,
    searchHint: String,
    onQueryChange: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val animated = LocalAnimationsEnabled.current
    val pulse = rememberInfiniteTransition(label = "radio-live-pulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (animated) 0.25f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "radio-live-pulse-alpha"
    )
    val heroShape = RoundedCornerShape(32.dp)
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .fillMaxWidth()
            .clip(heroShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        lerp(colors.surfaceContainerHigh, RadioLiveRed, 0.20f),
                        lerp(colors.surfaceContainerHigh, colors.tertiary, 0.16f)
                    )
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(MaterialShapes.SoftBurst.toShape())
                    .background(RadioLiveRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Podcasts, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.32f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .graphicsLayer { alpha = pulseAlpha }
                            .background(RadioLiveRed, CircleShape)
                    )
                    Text(live, color = Color.White, fontSize = 11.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black)
                }
                Text(
                    text = subtitle,
                    color = colors.onSurface,
                    fontSize = 19.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(19.sp),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp
                )
            }
        }
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = CircleShape,
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = colors.onSurfaceVariant) },
            placeholder = {
                Text(searchHint, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant)
            },
            colors = TextFieldDefaults.colors(
                focusedTextColor = colors.onSurface,
                unfocusedTextColor = colors.onSurface,
                focusedContainerColor = colors.surfaceContainerHighest,
                unfocusedContainerColor = colors.surfaceContainerHighest,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = LevyraCyan
            )
        )
    }
}

private val RadioLiveRed = Color(0xFFE53935)

@Composable
private fun RadioChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val rest = levyraExpressiveToggleCorner(
        checked = selected,
        unchecked = 22.dp,
        checkedCorner = 14.dp,
        label = "radio-chip-selected-corner"
    )
    val corner = levyraExpressiveCorner(
        interactionSource = interaction,
        rest = rest,
        pressed = 10.dp,
        label = "radio-chip-press-corner"
    )
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (selected) colors.primary else colors.surfaceContainerHigh)
            .levyraPressable(
                onClick = onClick,
                interactionSource = interaction,
                role = Role.Button,
                pressedScale = LevyraPressScale.Control
            )
            .semantics { this.selected = selected }
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) colors.onPrimary else colors.onSurface,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RadioDirectoryButton(
    modifier: Modifier,
    label: String,
    icon: (@Composable () -> Unit)?,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val corner = levyraExpressiveCorner(
        interactionSource = interaction,
        rest = 25.dp,
        pressed = 14.dp,
        label = "radio-directory-corner"
    )
    val shape = RoundedCornerShape(corner)
    Row(
        modifier = modifier
            .heightIn(min = 50.dp)
            .clip(shape)
            .background(colors.surfaceContainerHigh)
            .levyraPressable(
                onClick = onClick,
                interactionSource = interaction,
                role = Role.Button,
                pressedScale = LevyraPressScale.Control
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        icon?.invoke()
        Text(label, color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun FeaturedRadioStation(
    station: RadioStation,
    favorite: Boolean,
    playing: Boolean,
    live: String,
    playLabel: String,
    addFavoriteLabel: String,
    removeFavoriteLabel: String,
    onPlay: () -> Unit,
    onFavorite: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(30.dp)
    val accent = if (playing) LevyraCyan else colors.tertiary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        lerp(colors.surfaceContainerHigh, accent, if (LevyraIsLight) 0.14f else 0.22f),
                        colors.surfaceContainerHigh
                    )
                )
            )
            .clickable(onClick = onPlay)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RadioArtwork(station, 82.dp, RoundedCornerShape(22.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                if (playing) Icon(Icons.Rounded.GraphicEq, null, tint = LevyraCyan, modifier = Modifier.size(17.dp))
                Text(live, color = if (playing) LevyraCyan else accent, fontSize = 10.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
            }
            Text(station.name, color = LevyraText, fontSize = 17.sp, lineHeight = LevyraTypeRhythm.lineHeight(17.sp), fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            RadioStationDetail(station)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onFavorite) {
                Icon(
                    if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    if (favorite) removeFavoriteLabel else addFavoriteLabel,
                    tint = if (favorite) LevyraCyan else LevyraMuted
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(rememberLevyraPlayMorphShape(playing = playing, animated = LocalAnimationsEnabled.current, spinWhilePlaying = true))
                    .background(if (playing) LevyraCyan else colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (playing) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
                    playLabel,
                    tint = if (playing) LevyraBlack else colors.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun RadioStationRow(
    index: Int,
    count: Int,
    station: RadioStation,
    favorite: Boolean,
    playing: Boolean,
    live: String,
    playLabel: String,
    addFavoriteLabel: String,
    removeFavoriteLabel: String,
    onPlay: () -> Unit,
    onFavorite: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 1.dp)
            .clip(levyraGroupedListShape(index, count))
            .background(if (playing) lerp(colors.surfaceContainer, LevyraCyan, 0.16f) else colors.surfaceContainer)
            .clickable(onClick = onPlay)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            RadioArtwork(station, 58.dp, RoundedCornerShape(13.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(station.name, color = LevyraText, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (playing) Text(live, color = LevyraCyan, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                }
                RadioStationDetail(station)
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    if (favorite) removeFavoriteLabel else addFavoriteLabel,
                    tint = if (favorite) LevyraCyan else LevyraMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
            Icon(if (playing) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow, playLabel, tint = if (playing) LevyraCyan else LevyraText, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun RadioStationDetail(station: RadioStation) {
    val detail = listOfNotNull(
        station.country.takeIf(String::isNotBlank),
        station.tags.firstOrNull(),
        station.qualityLabel.takeIf(String::isNotBlank)
    ).joinToString(" · ")
    if (detail.isNotBlank()) {
        Text(detail, color = LevyraMuted, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RadioStationRail(
    stations: List<RadioStation>,
    favoriteIds: Set<String>,
    currentStationId: String?,
    isPlaying: Boolean,
    live: String,
    playLabel: String,
    addFavoriteLabel: String,
    removeFavoriteLabel: String,
    onPlay: (RadioStation) -> Unit,
    onFavorite: (RadioStation) -> Unit
) {
    LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(stations.take(12), key = { "rail-${it.uuid}" }) { station ->
            val favorite = station.uuid.lowercase(Locale.ROOT) in favoriteIds
            val playing = currentStationId == station.uuid && isPlaying
            Column(
                modifier = Modifier.width(132.dp).clickable { onPlay(station) },
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box {
                    RadioArtwork(station, 132.dp, RoundedCornerShape(28.dp))
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).size(34.dp).background(LevyraBlack.copy(alpha = 0.76f), CircleShape).clickable { onFavorite(station) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            if (favorite) removeFavoriteLabel else addFavoriteLabel,
                            tint = if (favorite) LevyraCyan else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (playing) {
                        Row(
                            modifier = Modifier.align(Alignment.BottomStart).padding(7.dp).background(LevyraCyan, CircleShape).padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.GraphicEq, null, tint = Color.Black, modifier = Modifier.size(13.dp))
                            Text(live, color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Text(station.name, color = LevyraText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(station.country.ifBlank { station.language }, color = LevyraMuted, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun RadioArtwork(station: RadioStation, size: androidx.compose.ui.unit.Dp, shape: RoundedCornerShape) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val imageLoader = remember(context.applicationContext) { LiveRadioArtworkLoader.get(context.applicationContext) }
    val safeFavicon = remember(station.faviconUrl) { station.safeFaviconUrl }
    var faviconFailed by remember(safeFavicon) { mutableStateOf(false) }
    val needsFallback = safeFavicon.isBlank() || faviconFailed
    var fallbackArtwork by remember(station.homepageUrl) { mutableStateOf<String?>(null) }
    var fallbackFailed by remember(fallbackArtwork) { mutableStateOf(false) }
    LaunchedEffect(needsFallback, station.homepageUrl) {
        if (needsFallback) fallbackArtwork = LiveRadioArtworkResolver.resolve(station.homepageUrl)
    }
    val artwork = when {
        !needsFallback -> safeFavicon
        !fallbackFailed -> fallbackArtwork
        else -> null
    }
    val initials = remember(station.name) {
        station.name.split(radioArtworkWordPattern).mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("").ifBlank { "LR" }
    }
    Box(
        modifier = Modifier.size(size).clip(shape).background(
            Brush.linearGradient(listOf(Color(0xFF123F46), Color(0xFF10242E), LevyraPanel))
        ).border(1.dp, LevyraAdaptiveHairline, shape),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = LevyraCyan.copy(alpha = 0.88f), fontSize = if (size > 70.dp) 26.sp else 18.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
        if (artwork != null) {
            AsyncImage(
                model = artwork,
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onError = { if (needsFallback) fallbackFailed = true else faviconFailed = true },
                modifier = Modifier.fillMaxSize().padding(7.dp)
            )
        }
    }
}

@Composable
private fun RadioNotice(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp).background(LevyraPanelSoft, RoundedCornerShape(14.dp)).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = LevyraCyan, modifier = Modifier.size(18.dp))
        Text(text, color = LevyraMuted, fontSize = 12.5.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun RadioEmptyState(title: String, detail: String?, retry: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 34.dp, vertical = 46.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Rounded.Podcasts, null, tint = LevyraMuted, modifier = Modifier.size(38.dp))
        Text(title, color = LevyraText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        AnimatedVisibility(detail != null) {
            detail?.let { Text(it, color = LevyraMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
        TextButton(onClick = onRetry) { Text(retry, color = LevyraCyan, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun RadioDirectoryDialog(
    title: String,
    entries: List<RadioDirectoryEntry>,
    selected: String?,
    allLabel: String,
    display: (RadioDirectoryEntry) -> String,
    value: (RadioDirectoryEntry) -> String,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = LevyraText, fontWeight = FontWeight.Black) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                item(key = "radio-directory-all") {
                    RadioDirectoryDialogRow(allLabel, selected == null) { onSelect(null) }
                }
                items(entries, key = { "${it.code}:${it.name}" }) { entry ->
                    val itemValue = value(entry)
                    RadioDirectoryDialogRow(display(entry), selected == itemValue) { onSelect(itemValue) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {},
        containerColor = LevyraPanel,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun RadioDirectoryDialogRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).background(if (selected) LevyraCyan else Color.Transparent, CircleShape))
        Text(label, color = if (selected) LevyraCyan else LevyraText, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

private fun selectedCountryLabel(state: LiveRadioUiState, strings: LevyraLiveRadioStrings): String {
    val code = state.selectedCountryCode ?: return strings.worldwide
    return localizedCountryName(code, state.levyraLanguageCode).ifBlank { strings.countries }
}

private fun mainSectionTitle(state: LiveRadioUiState, strings: LevyraLiveRadioStrings): String {
    if (state.query.isNotBlank()) return state.query.trim()
    state.selectedLanguage?.let { return it }
    val code = state.selectedCountryCode ?: return strings.worldwide
    return strings.popularIn(localizedCountryName(code, state.levyraLanguageCode).ifBlank { code })
}

private fun localizedCountryName(countryCode: String, languageCode: String): String = runCatching {
    Locale.Builder().setRegion(countryCode.uppercase(Locale.ROOT)).build()
        .getDisplayCountry(Locale.forLanguageTag(languageCode))
}.getOrDefault("")

private val radioArtworkWordPattern = Regex("\\s+")

package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.components.LevyraBackButton
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.fillMaxHeight

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

        RadioTopBar(
            title = radioStrings.title,
            live = radioStrings.live,
            backLabel = strings.back,
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter)
        )
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
                RadioLivePill(live = live)
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
private fun RadioTopBar(
    title: String,
    live: String,
    backLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LevyraBackButton(onClick = onBack, contentDescription = backLabel)
            Text(
                text = title,
                color = colors.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.6).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            RadioLivePill(live = live)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(Brush.verticalGradient(listOf(colors.surface, Color.Transparent)))
        )
    }
}

@Composable
private fun RadioLivePill(live: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val pulseAlpha = rememberRadioPulse()
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.inverseSurface)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .graphicsLayer { alpha = pulseAlpha() }
                .background(RadioLiveRed, CircleShape)
        )
        Text(live, color = colors.inverseOnSurface, fontSize = 11.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun rememberRadioPulse(): () -> Float {
    val animated = LocalAnimationsEnabled.current
    val pulse = rememberInfiniteTransition(label = "radio-live-pulse")
    val alpha = pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (animated) 0.25f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "radio-live-pulse-alpha"
    )
    return { alpha.value }
}

@Composable
private fun RadioEqualizer(color: Color, modifier: Modifier = Modifier) {
    val animated = LocalAnimationsEnabled.current
    val transition = rememberInfiniteTransition(label = "radio-equalizer")
    val bars = RadioEqualizerPeriods.mapIndexed { index, period ->
        transition.animateFloat(
            initialValue = 0.3f,
            targetValue = if (animated) 1f else 0.6f,
            animationSpec = infiniteRepeatable(tween(period), RepeatMode.Reverse),
            label = "radio-equalizer-$index"
        )
    }
    Row(
        modifier = modifier.height(14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        bars.forEach { bar ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleY = bar.value
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .background(color, CircleShape)
            )
        }
    }
}

private val RadioEqualizerPeriods = listOf(420, 560, 360)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    val container = lerp(colors.surfaceContainerHigh, colors.primaryContainer, if (playing) 0.62f else 0.38f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(container)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RadioArtwork(station, 104.dp, MaterialShapes.Cookie9Sided.toShape())
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RadioLivePill(live = live)
                Text(
                    station.name,
                    color = colors.onSurface,
                    fontSize = 22.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(22.sp),
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                RadioStationDetail(station)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RadioPlayPill(
                playing = playing,
                label = playLabel,
                live = live,
                onClick = onPlay,
                modifier = Modifier.weight(1f)
            )
            RadioFavoriteButton(
                favorite = favorite,
                addLabel = addFavoriteLabel,
                removeLabel = removeFavoriteLabel,
                size = 56.dp,
                onClick = onFavorite
            )
        }
    }
}

@Composable
private fun RadioPlayPill(
    playing: Boolean,
    label: String,
    live: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val rest = levyraExpressiveToggleCorner(
        checked = playing,
        unchecked = 28.dp,
        checkedCorner = 16.dp,
        label = "radio-play-pill-corner"
    )
    val corner = levyraExpressiveCorner(
        interactionSource = interaction,
        rest = rest,
        pressed = 12.dp,
        label = "radio-play-pill-press"
    )
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(corner))
            .background(colors.primary)
            .levyraPressable(
                onClick = onClick,
                interactionSource = interaction,
                role = Role.Button,
                pressedScale = LevyraPressScale.Control
            )
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
    ) {
        if (playing) {
            RadioEqualizer(color = colors.onPrimary)
            Text(live, color = colors.onPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
        } else {
            Icon(Icons.Rounded.PlayArrow, null, tint = colors.onPrimary, modifier = Modifier.size(26.dp))
            Text(label, color = colors.onPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RadioFavoriteButton(
    favorite: Boolean,
    addLabel: String,
    removeLabel: String,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val rest = levyraExpressiveToggleCorner(
        checked = favorite,
        unchecked = size / 2,
        checkedCorner = size * 0.3f,
        label = "radio-favorite-corner"
    )
    val corner = levyraExpressiveCorner(
        interactionSource = interaction,
        rest = rest,
        pressed = size * 0.22f,
        label = "radio-favorite-press"
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(if (favorite) colors.tertiaryContainer else colors.surfaceContainerHighest)
            .levyraPressable(
                onClick = onClick,
                interactionSource = interaction,
                role = Role.Button,
                pressedScale = LevyraPressScale.Control
            )
            .semantics { contentDescription = if (favorite) removeLabel else addLabel },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            null,
            tint = if (favorite) colors.onTertiaryContainer else colors.onSurfaceVariant,
            modifier = Modifier.size(size * 0.42f)
        )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 1.dp)
            .clip(levyraGroupedListShape(index, count))
            .background(if (playing) lerp(colors.surfaceContainer, colors.primaryContainer, 0.55f) else colors.surfaceContainer)
            .levyraPressable(onClick = onPlay, role = Role.Button, pressedScale = LevyraPressScale.Surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RadioArtwork(station, 60.dp, RoundedCornerShape(18.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                station.name,
                color = colors.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (playing) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RadioEqualizer(color = colors.primary)
                    Text(live, color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            } else {
                RadioStationDetail(station)
            }
        }
        RadioFavoriteButton(
            favorite = favorite,
            addLabel = addFavoriteLabel,
            removeLabel = removeFavoriteLabel,
            size = 44.dp,
            onClick = onFavorite
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(rememberLevyraPlayMorphShape(playing = playing, animated = LocalAnimationsEnabled.current, spinWhilePlaying = true))
                .background(if (playing) colors.primary else colors.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (playing) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
                playLabel,
                tint = if (playing) colors.onPrimary else colors.onSecondaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun RadioStationDetail(station: RadioStation) {
    val languageCode = LocalLevyraStrings.current.code
    val place = remember(station.countryCode, station.country, languageCode) {
        station.countryCode.takeIf(String::isNotBlank)
            ?.let { localizedCountryName(it, languageCode) }
            ?.takeIf(String::isNotBlank)
            ?: station.country
    }
    val detail = listOfNotNull(
        place.takeIf(String::isNotBlank),
        station.tags.firstOrNull(),
        station.qualityLabel.takeIf(String::isNotBlank)
    ).joinToString(" · ")
    if (detail.isNotBlank()) {
        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    val colors = MaterialTheme.colorScheme
    val languageCode = LocalLevyraStrings.current.code
    val railShapes = listOf(
        MaterialShapes.Cookie9Sided.toShape(),
        MaterialShapes.Clover4Leaf.toShape(),
        MaterialShapes.Cookie6Sided.toShape(),
        MaterialShapes.SoftBurst.toShape()
    )
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        itemsIndexed(stations.take(12), key = { _, station -> "rail-${station.uuid}" }) { index, station ->
            val favorite = station.uuid.lowercase(Locale.ROOT) in favoriteIds
            val playing = currentStationId == station.uuid && isPlaying
            val place = remember(station.countryCode, station.country, languageCode) {
                station.countryCode.takeIf(String::isNotBlank)
                    ?.let { localizedCountryName(it, languageCode) }
                    ?.takeIf(String::isNotBlank)
                    ?: station.country.ifBlank { station.language }
            }
            Column(
                modifier = Modifier
                    .width(136.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (playing) lerp(colors.surfaceContainer, colors.primaryContainer, 0.55f) else colors.surfaceContainer)
                    .levyraPressable(onClick = { onPlay(station) }, role = Role.Button, pressedScale = LevyraPressScale.Surface)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    RadioArtwork(station, 116.dp, railShapes[index % railShapes.size])
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        RadioFavoriteButton(
                            favorite = favorite,
                            addLabel = addFavoriteLabel,
                            removeLabel = removeFavoriteLabel,
                            size = 36.dp,
                            onClick = { onFavorite(station) }
                        )
                    }
                    if (playing) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .clip(CircleShape)
                                .background(colors.primary)
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            RadioEqualizer(color = colors.onPrimary)
                            Text(live, color = colors.onPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Text(
                    station.name,
                    color = colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    place,
                    color = colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun RadioArtwork(station: RadioStation, size: androidx.compose.ui.unit.Dp, shape: Shape) {
    val colors = MaterialTheme.colorScheme
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
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(colors.secondaryContainer, colors.surfaceContainerHighest))
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initials,
            color = colors.onSecondaryContainer,
            fontSize = if (size > 70.dp) 28.sp else 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp
        )
        if (artwork != null) {
            AsyncImage(
                model = artwork,
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onError = { if (needsFallback) fallbackFailed = true else faviconFailed = true },
                modifier = Modifier.fillMaxSize().padding(size * 0.12f)
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

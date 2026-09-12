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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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

@Composable
internal fun LiveRadioScreen(
    languageCode: String,
    currentStationId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlay: (RadioStation) -> Unit
) {
    BackHandler(onBack = onBack)
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
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(LevyraCyan.copy(alpha = 0.14f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Podcasts, null, tint = LevyraCyan, modifier = Modifier.size(19.dp))
                        }
                        Text(
                            text = radioStrings.live,
                            color = LevyraCyan,
                            fontSize = 12.sp,
                            letterSpacing = 1.4.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = radioStrings.subtitle,
                        color = LevyraMuted,
                        fontSize = 15.sp,
                        lineHeight = LevyraTypeRhythm.lineHeight(15.sp),
                        fontWeight = FontWeight.Medium
                    )
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = LevyraMuted) },
                        placeholder = {
                            Text(
                                radioStrings.searchHint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = LevyraMuted
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LevyraText,
                            unfocusedTextColor = LevyraText,
                            focusedBorderColor = LevyraCyan.copy(alpha = 0.72f),
                            unfocusedBorderColor = LevyraAdaptiveHairline,
                            focusedContainerColor = LevyraPanelSoft.copy(alpha = 0.68f),
                            unfocusedContainerColor = LevyraPanelSoft.copy(alpha = 0.48f),
                            cursorColor = LevyraCyan
                        )
                    )
                }
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

            items(
                items = state.stations.drop(1),
                key = { station -> "live-radio-station-${station.uuid}" },
                contentType = { "live-radio-station" }
            ) { station ->
                RadioStationRow(
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
        fontSize = 19.sp,
        lineHeight = LevyraTypeRhythm.lineHeight(19.sp),
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp).semantics { heading() }
    )
}

@Composable
private fun RadioChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (selected) LevyraCyan else LevyraPanelSoft)
            .border(BorderStroke(1.dp, if (selected) LevyraCyan else LevyraAdaptiveHairline), shape)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Color.Black else LevyraText,
            fontSize = 13.sp,
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
    Row(
        modifier = modifier
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(LevyraPanelSoft)
            .border(1.dp, LevyraAdaptiveHairline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        icon?.invoke()
        Text(label, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = LevyraMuted, modifier = Modifier.size(18.dp))
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
    val shape = RoundedCornerShape(22.dp)
    val accent = if (playing) LevyraCyan else Color(0xFF2A8C83)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(accent.copy(alpha = if (LevyraIsLight) 0.16f else 0.24f), LevyraPanel.copy(alpha = 0.94f))
                )
            )
            .border(1.dp, if (playing) LevyraCyan.copy(alpha = 0.58f) else LevyraAdaptiveHairline, shape)
            .clickable(onClick = onPlay)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RadioArtwork(station, 82.dp, RoundedCornerShape(17.dp))
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
                modifier = Modifier.size(42.dp).background(if (playing) LevyraCyan else LevyraText, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (playing) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow, playLabel, tint = LevyraBlack, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun RadioStationRow(
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
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(if (playing) LevyraCyan.copy(alpha = 0.08f) else Color.Transparent).padding(horizontal = 24.dp, vertical = 11.dp),
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
        HorizontalDivider(modifier = Modifier.padding(start = 95.dp, end = 24.dp), color = LevyraAdaptiveHairline)
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
                    RadioArtwork(station, 132.dp, RoundedCornerShape(19.dp))
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
    var failed by remember(station.faviconUrl) { mutableStateOf(false) }
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
        if (station.faviconUrl.isNotBlank() && !failed) {
            AsyncImage(
                model = station.faviconUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onError = { failed = true },
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

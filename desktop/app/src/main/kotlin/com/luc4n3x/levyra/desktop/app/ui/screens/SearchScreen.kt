package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.state.SearchUiState
import com.luc4n3x.levyra.desktop.app.ui.components.CollectionCard
import com.luc4n3x.levyra.desktop.app.ui.components.EmptyState
import com.luc4n3x.levyra.desktop.app.ui.components.ErrorBanner
import com.luc4n3x.levyra.desktop.app.ui.components.LevyraChip
import com.luc4n3x.levyra.desktop.app.ui.components.LoadingRow
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.components.TrackRow
import com.luc4n3x.levyra.desktop.app.ui.components.tracksTextInputFocus
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.LocalAccentColor
import com.luc4n3x.levyra.desktop.core.model.CollectionRef
import com.luc4n3x.levyra.desktop.core.model.SearchFilter
import com.luc4n3x.levyra.desktop.core.model.Track

@Composable
fun SearchScreen(
    state: SearchUiState,
    recentSearches: List<String>,
    contentCountry: String,
    actions: TrackActions,
    onQueryChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onLoadMore: () -> Unit,
    onOpenCollection: (CollectionRef) -> Unit,
    onClearRecent: () -> Unit,
    localResults: List<Track>,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val accent = LocalAccentColor.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 20.dp)
    ) {
        SearchField(
            query = state.query,
            onQueryChange = onQueryChange,
            onSubmit = onSubmit
        )

        if (state.suggestions.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 10.dp
            ) {
                Column {
                    state.suggestions.forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSubmit(suggestion) }
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            Icon(
                                imageVector = LevyraIcons.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(SearchFilter.entries) { filter ->
                LevyraChip(
                    label = filterLabel(filter),
                    selected = filter == state.filter,
                    onClick = { onFilterChange(filter) }
                )
            }
        }

        if (localResults.isNotEmpty()) {
            LocalSearchResults(
                tracks = localResults,
                actions = actions,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (state.error.isNotBlank()) {
            ErrorBanner(
                message = state.error,
                actionLabel = strings.retry,
                onAction = { onSubmit(state.submittedQuery) },
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        when {
            state.loading -> LoadingRow(label = strings.loading)

            state.submittedQuery.isBlank() -> SearchLanding(
                country = contentCountry,
                recentSearches = recentSearches,
                accent = accent,
                onSelect = onSubmit,
                onClearRecent = onClearRecent,
                modifier = Modifier.weight(1f)
            )

            !state.hasResults -> EmptyState(
                icon = LevyraIcons.Search,
                title = strings.searchNoResults,
                modifier = Modifier.weight(1f)
            )

            state.page.tracks.isNotEmpty() -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(state.page.tracks, key = { _, track -> track.id }) { index, track ->
                    TrackRow(
                        track = track,
                        isCurrent = track.id == actions.currentTrackId,
                        isFavorite = actions.isFavorite(track),
                        onPlay = { actions.onPlay(state.page.tracks, index) },
                        onPlayNext = { actions.onPlayNext(track) },
                        onEnqueue = { actions.onEnqueue(track) },
                        onToggleFavorite = { actions.onToggleFavorite(track) },
                        onAddToPlaylist = { actions.onAddToPlaylist(track) }
                    )
                }
                if (state.canLoadMore) {
                    item {
                        TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) {
                            Text(strings.loadMore)
                        }
                    }
                }
                if (state.loadingMore) {
                    item { LoadingRow(label = strings.loading) }
                }
            }

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 168.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.page.collections, key = { it.id }) { ref ->
                    CollectionCard(ref = ref, onClick = { onOpenCollection(ref) })
                }
                if (state.canLoadMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) {
                            Text(strings.loadMore)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: (String) -> Unit
) {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(strings.searchPlaceholder) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = LevyraIcons.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = LevyraIcons.Close,
                                contentDescription = strings.close,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit(query) }),
                modifier = Modifier.weight(1f).tracksTextInputFocus()
            )
            Button(
                onClick = { onSubmit(query) },
                enabled = query.isNotBlank(),
                modifier = Modifier.height(48.dp)
            ) {
                Text(strings.searchAction)
            }
        }
    }
}

@Composable
private fun SearchLanding(
    country: String,
    recentSearches: List<String>,
    accent: Color,
    onSelect: (String) -> Unit,
    onClearRecent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val discovery = discoveryForCountry(country)
    val region = chartRegionName(country, strings.languageCode)
    val moods = listOf(
        MoodSuggestion("🔥", strings.searchMoodTrending, discovery.quick.getOrElse(4) { "top hits" }),
        MoodSuggestion("🏋️", strings.searchMoodWorkout, discovery.quick.getOrElse(7) { "gym workout music" }),
        MoodSuggestion("🌙", strings.searchMoodNightDrive, discovery.quick.getOrElse(6) { "night drive" }),
        MoodSuggestion("☁️", strings.searchMoodChill, "chill music ${discovery.countryKeyword}"),
        MoodSuggestion("🎉", strings.searchMoodParty, "party hits ${discovery.countryKeyword}"),
        MoodSuggestion("💜", strings.searchMoodSad, "sad songs ${discovery.countryKeyword}")
    )

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accent.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.colorScheme.surfaceContainer
                                )
                            )
                        )
                        .padding(horizontal = 28.dp, vertical = 24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = strings.searchExploreTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = strings.searchExploreSubtitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            modifier = Modifier.padding(top = 6.dp),
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = "${strings.searchRegion} $region",
                                style = MaterialTheme.typography.labelLarge,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            SearchSectionTitle(strings.searchQuickPicks)
            LazyRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 20.dp)
            ) {
                items(discovery.quick) { query ->
                    QuickSearchCard(
                        query = query,
                        accent = accent,
                        onClick = { onSelect(query) }
                    )
                }
            }
        }

        item {
            SearchSectionTitle(strings.searchPopularArtists)
            LazyRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(end = 20.dp)
            ) {
                items(discovery.artists) { artist ->
                    ArtistSuggestionCard(
                        artist = artist,
                        accent = accent,
                        onClick = { onSelect(artist) }
                    )
                }
            }
        }

        item {
            SearchSectionTitle(strings.searchMoods)
            LazyRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 20.dp)
            ) {
                items(moods) { mood ->
                    MoodCard(
                        mood = mood,
                        accent = accent,
                        onClick = { onSelect(mood.query) }
                    )
                }
            }
        }

        if (recentSearches.isNotEmpty()) {
            item {
                RecentSearches(
                    recentSearches = recentSearches,
                    onSelect = onSelect,
                    onClear = onClearRecent
                )
            }
        }
    }
}

@Composable
private fun SearchSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun QuickSearchCard(
    query: String,
    accent: Color,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier
            .width(220.dp)
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = query,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = strings.searchTapToSearch,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
        }
    }
}

@Composable
private fun ArtistSuggestionCard(
    artist: String,
    accent: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(104.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = artistInitials(artist),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = artist,
            style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MoodCard(
    mood: MoodSuggestion,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(180.dp)
            .height(86.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = mood.emoji, style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = mood.label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentSearches(
    recentSearches: List<String>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit
) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchSectionTitle(strings.recentSearches)
            TextButton(onClick = onClear) {
                Text(strings.clearRecentSearches)
            }
        }
        recentSearches.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { onSelect(entry) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Icon(
                    imageVector = LevyraIcons.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(text = entry, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun filterLabel(filter: SearchFilter): String {
    val strings = LocalStrings.current
    return when (filter) {
        SearchFilter.SONGS -> strings.filterSongs
        SearchFilter.VIDEOS -> strings.filterVideos
        SearchFilter.ALBUMS -> strings.filterAlbums
        SearchFilter.PLAYLISTS -> strings.filterPlaylists
        SearchFilter.ARTISTS -> strings.filterArtists
    }
}

private data class SearchDiscovery(
    val quick: List<String>,
    val artists: List<String>,
    val countryKeyword: String
)

private data class MoodSuggestion(
    val emoji: String,
    val label: String,
    val query: String
)

private fun discoveryForCountry(country: String): SearchDiscovery = when (country.trim().uppercase()) {
    "IT" -> SearchDiscovery(
        quick = listOf("Sfera Ebbasta", "Lazza", "Geolier", "Marracash", "top hits Italia", "rap italiano", "night drive", "gym bass"),
        artists = listOf("Sfera Ebbasta", "Lazza", "Geolier", "Marracash", "Annalisa", "Ultimo", "Tedua", "Ghali"),
        countryKeyword = "Italia"
    )
    "ES" -> SearchDiscovery(
        quick = listOf("Bad Bunny", "Rosalía", "Quevedo", "Aitana", "éxitos España", "reggaeton latino", "conducir de noche", "gym bass"),
        artists = listOf("Bad Bunny", "Rosalía", "Quevedo", "Aitana", "Feid", "Karol G", "Rauw Alejandro", "Bizarrap"),
        countryKeyword = "España"
    )
    "FR" -> SearchDiscovery(
        quick = listOf("Gazo", "Aya Nakamura", "Damso", "Ninho", "top hits France", "rap français", "conduite de nuit", "gym bass"),
        artists = listOf("Gazo", "Aya Nakamura", "Damso", "Ninho", "Tiakola", "SDM", "SCH", "Jul"),
        countryKeyword = "France"
    )
    "DE" -> SearchDiscovery(
        quick = listOf("Apache 207", "RAF Camora", "Luciano", "Ayliva", "top hits Deutschland", "deutschrap", "night drive", "gym bass"),
        artists = listOf("Apache 207", "RAF Camora", "Luciano", "Ayliva", "Ufo361", "Shirin David", "Kontra K", "Nina Chuba"),
        countryKeyword = "Deutschland"
    )
    "BR", "PT" -> SearchDiscovery(
        quick = listOf("Anitta", "Matuê", "Luísa Sonza", "Veigh", "top hits Brasil", "funk brasileiro", "dirigir à noite", "gym bass"),
        artists = listOf("Anitta", "Matuê", "Luísa Sonza", "Veigh", "Luan Santana", "MC Ryan SP", "WIU", "Marília Mendonça"),
        countryKeyword = "Brasil"
    )
    "NL" -> SearchDiscovery(
        quick = listOf("Frenna", "Suzan & Freek", "Antoon", "Boef", "Nederlandse hits", "nederlandse rap", "night drive", "gym bass"),
        artists = listOf("Frenna", "Suzan & Freek", "Antoon", "Boef", "Roxy Dekker", "Maan", "Ronnie Flex", "S10"),
        countryKeyword = "Nederland"
    )
    "PL" -> SearchDiscovery(
        quick = listOf("sanah", "Taco Hemingway", "Dawid Podsiadło", "Quebonafide", "polskie hity", "polski rap", "night drive", "gym bass"),
        artists = listOf("sanah", "Taco Hemingway", "Dawid Podsiadło", "Quebonafide", "PRO8L3M", "Mata", "Kizo", "Oki"),
        countryKeyword = "Polska"
    )
    "RO" -> SearchDiscovery(
        quick = listOf("Inna", "The Motans", "Delia", "Carla's Dreams", "hituri România", "rap românesc", "night drive", "gym bass"),
        artists = listOf("Inna", "The Motans", "Delia", "Carla's Dreams", "Irina Rimes", "Smiley", "Andra", "Theo Rose"),
        countryKeyword = "România"
    )
    "GR" -> SearchDiscovery(
        quick = listOf("Konstantinos Argiros", "Eleni Foureira", "Snik", "Helena Paparizou", "ελληνικά hits", "ελληνικό rap", "night drive", "gym bass"),
        artists = listOf("Konstantinos Argiros", "Eleni Foureira", "Snik", "Helena Paparizou", "Sakis Rouvas", "Josephine", "Light", "Melisses"),
        countryKeyword = "Greece"
    )
    "SE" -> SearchDiscovery(
        quick = listOf("Veronica Maggio", "Zara Larsson", "Hov1", "Miriam Bryant", "svenska hits", "svensk rap", "night drive", "gym bass"),
        artists = listOf("Veronica Maggio", "Zara Larsson", "Hov1", "Miriam Bryant", "Victor Leksell", "Benjamin Ingrosso", "Molly Sandén", "Darin"),
        countryKeyword = "Sweden"
    )
    "DK" -> SearchDiscovery(
        quick = listOf("Gilli", "Tobias Rahim", "MØ", "Medina", "danske hits", "dansk rap", "night drive", "gym bass"),
        artists = listOf("Gilli", "Tobias Rahim", "MØ", "Medina", "KESI", "Lamin", "Christopher", "Burhan G"),
        countryKeyword = "Denmark"
    )
    "CZ" -> SearchDiscovery(
        quick = listOf("Calin", "Ewa Farna", "Ben Cristovao", "Viktor Sheen", "české hity", "český rap", "night drive", "gym bass"),
        artists = listOf("Calin", "Ewa Farna", "Ben Cristovao", "Viktor Sheen", "Yzomandias", "Mirai", "Kryštof", "Pam Rabbit"),
        countryKeyword = "Czechia"
    )
    "UA" -> SearchDiscovery(
        quick = listOf("alyona alyona", "KALUSH", "Jerry Heil", "The Hardkiss", "українські хіти", "український реп", "нічна поїздка", "бас для тренувань"),
        artists = listOf("alyona alyona", "KALUSH", "Jerry Heil", "The Hardkiss", "Monatik", "Dorofeeva", "Okean Elzy", "Kazka"),
        countryKeyword = "Ukraine"
    )
    "RU" -> SearchDiscovery(
        quick = listOf("MiyaGi & Andy Panda", "Zivert", "Баста", "Клава Кока", "русские хиты", "русский рэп", "ночная поездка", "бас для тренировки"),
        artists = listOf("MiyaGi & Andy Panda", "Zivert", "Баста", "Клава Кока", "JONY", "Мот", "ANNA ASTI", "MACAN"),
        countryKeyword = "Russia"
    )
    "TR" -> SearchDiscovery(
        quick = listOf("Tarkan", "Sefo", "Mabel Matiz", "Simge", "Türkçe hitler", "Türkçe rap", "gece sürüşü", "spor bas"),
        artists = listOf("Tarkan", "Sefo", "Mabel Matiz", "Simge", "Ezhel", "UZI", "Semicenk", "Hadise"),
        countryKeyword = "Türkiye"
    )
    "CN" -> SearchDiscovery(
        quick = listOf("周杰伦", "邓紫棋", "薛之谦", "林俊杰", "2026 华语热歌", "中文说唱", "夜间驾驶歌单", "健身音乐"),
        artists = listOf("周杰伦", "邓紫棋", "薛之谦", "林俊杰", "陈奕迅", "王菲", "毛不易", "蔡依林"),
        countryKeyword = "China"
    )
    "JP" -> SearchDiscovery(
        quick = listOf("YOASOBI", "Ado", "Official髭男dism", "Mrs. GREEN APPLE", "2026 邦楽ヒット", "日本語ラップ", "夜のドライブ", "ワークアウト音楽"),
        artists = listOf("YOASOBI", "Ado", "Official髭男dism", "Mrs. GREEN APPLE", "Vaundy", "米津玄師", "King Gnu", "藤井風"),
        countryKeyword = "Japan"
    )
    "KR" -> SearchDiscovery(
        quick = listOf("BTS", "BLACKPINK", "NewJeans", "IVE", "2026 국내 인기곡", "한국 힙합", "야간 드라이브", "운동 음악"),
        artists = listOf("BTS", "BLACKPINK", "NewJeans", "IVE", "aespa", "Stray Kids", "SEVENTEEN", "IU"),
        countryKeyword = "Korea"
    )
    "IN" -> SearchDiscovery(
        quick = listOf("Arijit Singh", "Shreya Ghoshal", "Diljit Dosanjh", "A.R. Rahman", "2026 भारतीय हिट", "हिंदी रैप", "नाइट ड्राइव", "वर्कआउट संगीत"),
        artists = listOf("Arijit Singh", "Shreya Ghoshal", "A.R. Rahman", "Pritam", "Diljit Dosanjh", "Badshah", "Neha Kakkar", "AP Dhillon"),
        countryKeyword = "India"
    )
    "ID" -> SearchDiscovery(
        quick = listOf("Tulus", "Mahalini", "Hindia", "NIKI", "lagu Indonesia 2026", "rap Indonesia", "musik berkendara malam", "musik olahraga"),
        artists = listOf("Tulus", "Mahalini", "Hindia", "NIKI", "Tiara Andini", "Bernadya", "Pamungkas", "Lyodra"),
        countryKeyword = "Indonesia"
    )
    "VN" -> SearchDiscovery(
        quick = listOf("Sơn Tùng M-TP", "Mỹ Tâm", "Đen Vâu", "HIEUTHUHAI", "nhạc Việt 2026", "rap Việt", "nhạc lái xe ban đêm", "nhạc tập luyện"),
        artists = listOf("Sơn Tùng M-TP", "Mỹ Tâm", "Đen Vâu", "HIEUTHUHAI", "Hoàng Thùy Linh", "MONO", "Bích Phương", "tlinh"),
        countryKeyword = "Vietnam"
    )
    "TH" -> SearchDiscovery(
        quick = listOf("พีพี กฤษฏ์", "บิวกิ้น", "Tilly Birds", "Three Man Down", "เพลงไทย 2026", "แรปไทย", "เพลงขับรถกลางคืน", "เพลงออกกำลังกาย"),
        artists = listOf("พีพี กฤษฏ์", "บิวกิ้น", "Tilly Birds", "Three Man Down", "4EVE", "MILLI", "Jeff Satur", "NONT TANONT"),
        countryKeyword = "Thailand"
    )
    "PH" -> SearchDiscovery(
        quick = listOf("Cup of Joe", "BINI", "SB19", "Ben&Ben", "OPM hits 2026", "Pinoy rap", "kantang pang-night drive", "musikang pang-workout"),
        artists = listOf("Cup of Joe", "BINI", "SB19", "Ben&Ben", "Arthur Nery", "Dionela", "TJ Monterde", "December Avenue"),
        countryKeyword = "Philippines"
    )
    "IL" -> SearchDiscovery(
        quick = listOf("עומר אדם", "נועה קירל", "אושר כהן", "עדן חסון", "להיטים ישראליים 2026", "היפ הופ ישראלי", "מוזיקה לנסיעה בלילה", "מוזיקה לאימון"),
        artists = listOf("עומר אדם", "נועה קירל", "אושר כהן", "עדן חסון", "נס וסטילה", "טונה", "רביב כנר", "ישי ריבו"),
        countryKeyword = "Israel"
    )
    else -> SearchDiscovery(
        quick = listOf("The Weeknd", "Drake", "Taylor Swift", "Billie Eilish", "global top hits", "rap hits", "night drive", "gym bass"),
        artists = listOf("The Weeknd", "Drake", "Taylor Swift", "Billie Eilish", "SZA", "Travis Scott", "Dua Lipa", "Kendrick Lamar"),
        countryKeyword = "Global"
    )
}

private fun chartRegionName(country: String, languageCode: String): String {
    val italian = languageCode == "it"
    return when (country.trim().uppercase()) {
        "IT" -> "🇮🇹 Italia"
        "US" -> if (italian) "🇺🇸 Stati Uniti" else "🇺🇸 United States"
        "GB" -> if (italian) "🇬🇧 Regno Unito" else "🇬🇧 United Kingdom"
        "ES" -> "🇪🇸 España"
        "FR" -> "🇫🇷 France"
        "DE" -> "🇩🇪 Deutschland"
        "BR" -> "🇧🇷 Brasil"
        "PT" -> "🇵🇹 Portugal"
        "NL" -> "🇳🇱 Nederland"
        "PL" -> "🇵🇱 Polska"
        "RO" -> "🇷🇴 România"
        "GR" -> "🇬🇷 Ελλάδα"
        "SE" -> "🇸🇪 Sverige"
        "DK" -> "🇩🇰 Danmark"
        "CZ" -> "🇨🇿 Česko"
        "UA" -> "🇺🇦 Україна"
        "RU" -> "🇷🇺 Россия"
        "TR" -> "🇹🇷 Türkiye"
        "CN" -> "🇨🇳 中国"
        "JP" -> "🇯🇵 日本"
        "KR" -> "🇰🇷 대한민국"
        "IN" -> "🇮🇳 India"
        "ID" -> "🇮🇩 Indonesia"
        "VN" -> "🇻🇳 Việt Nam"
        "TH" -> "🇹🇭 ประเทศไทย"
        "PH" -> "🇵🇭 Philippines"
        "IL" -> "🇮🇱 ישראל"
        else -> if (italian) "🌍 Internazionale" else "🌍 Global"
    }
}

private fun artistInitials(name: String): String {
    val parts = name
        .replace("&", " ")
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "L"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}

@Composable
private fun LocalSearchResults(
    tracks: List<Track>,
    actions: TrackActions,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = strings.localMusic,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        tracks.forEachIndexed { index, track ->
            TrackRow(
                track = track,
                isCurrent = track.id == actions.currentTrackId,
                isFavorite = actions.isFavorite(track),
                onPlay = { actions.onPlay(tracks, index) },
                onPlayNext = { actions.onPlayNext(track) },
                onEnqueue = { actions.onEnqueue(track) },
                onToggleFavorite = { actions.onToggleFavorite(track) },
                onAddToPlaylist = { actions.onAddToPlaylist(track) },
                position = index + 1
            )
        }
    }
}

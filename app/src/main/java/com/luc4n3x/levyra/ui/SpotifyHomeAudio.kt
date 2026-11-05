package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luc4n3x.levyra.feature.radio.RadioCategory
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import com.luc4n3x.levyra.data.PodcastShow
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.podcast.PodcastLibraryUiState
import com.luc4n3x.levyra.feature.podcast.PodcastLibraryViewModel
import com.luc4n3x.levyra.feature.podcast.PodcastShowState
import com.luc4n3x.levyra.feature.podcast.resumePositionMs
import com.luc4n3x.levyra.feature.radio.LiveRadioViewModel
import com.luc4n3x.levyra.feature.radio.RadioStation
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraLiveRadioCatalog
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.PodcastCopy
import com.luc4n3x.levyra.ui.i18n.podcastCopy
import com.luc4n3x.levyra.ui.i18n.radioLibraryCopy
import com.luc4n3x.levyra.ui.theme.SpotifyDesign
import com.luc4n3x.levyra.viewmodel.HomeViewModel
import com.luc4n3x.levyra.viewmodel.LevyraUiState

private const val SpotifyShowGridColumns = 4
private const val SpotifyShowGridLimit = 8
private val SpotifyEpisodeArt = 56.dp
private val SpotifyShowCover = 128.dp

@Composable
internal fun SpotifyPodcastsHome(state: LevyraUiState, viewModel: HomeViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val factory = remember(context.applicationContext) { PodcastLibraryViewModel.factory(context.applicationContext) }
    val podcasts: PodcastLibraryViewModel = composeViewModel(key = "levyra-podcasts", factory = factory)
    val podcastState by podcasts.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.languageCode) { podcasts.activate(state.languageCode) }
    val copy = remember(state.languageCode) { podcastCopy(state.languageCode) }
    val play: (List<Track>, Track) -> Unit = { list, episode ->
        viewModel.playPodcastEpisode(list, episode, resumePositionMs(podcastState.progressById[episode.id]))
    }
    val open = podcastState.openShow
    BackHandler(enabled = open != null) { podcasts.closeShow() }
    if (open != null) {
        SpotifyPodcastShowPage(open, podcastState, copy, state.currentTrack?.id, podcasts, play)
    } else {
        SpotifyPodcastLanding(podcastState, copy, podcasts, play)
    }
}

@Composable
private fun SpotifyPodcastLanding(
    state: PodcastLibraryUiState,
    copy: PodcastCopy,
    podcasts: PodcastLibraryViewModel,
    play: (List<Track>, Track) -> Unit
) {
    val retry = LocalLevyraStrings.current.exploreSamplesRetry
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        SpotifyPodcastSearch(state.query, copy.searchHint, podcasts::setQuery)
        if (state.query.trim().length >= 2) {
            SpotifyShowList(state.results, podcasts::openShow)
            if (!state.searching && state.results.isEmpty()) SpotifyAudioNotice(copy.noResults)
            return@Column
        }
        if (state.subscriptions.isNotEmpty()) SpotifyShowGrid(state.subscriptions.take(SpotifyShowGridLimit), podcasts::openShow)
        val resume = state.continueListening
        if (resume.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SpotifyHomeHeading(copy.continueListening)
                resume.take(4).forEach { item ->
                    SpotifyEpisodeRow(item.track, item.track.artist, null) { play(resume.map { it.track }, item.track) }
                }
            }
        }
        when {
            state.discoverLoading && state.discover.isEmpty() -> SpotifyAudioNotice(LocalLevyraStrings.current.searchingYouTubeMusic)
            state.discoverFailed && state.discover.isEmpty() -> SpotifyAudioNotice(copy.loadFailed, retry, podcasts::retryDiscover)
        }
        state.discover.forEach { shelf ->
            SpotifyHomeShelf(shelf.title) {
                items(shelf.episodes, key = { "podcast-${shelf.title}-${it.id}" }) { episode ->
                    SpotifyHomeCoverCard(episode.title, episode.artist, episode.thumbnailUrl) { play(shelf.episodes, episode) }
                }
            }
        }
        if (state.subscriptions.isEmpty()) SpotifyAudioNotice(copy.emptyLibrary)
    }
}

@Composable
private fun SpotifyPodcastSearch(query: String, hint: String, onQuery: (String) -> Unit) {
    Row(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(44.dp).clip(RoundedCornerShape(SpotifyDesign.cornerLarge))
            .background(SpotifyDesign.highlight).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = SpotifyDesign.subdued, modifier = Modifier.size(22.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = SpotifyDesign.text, fontSize = 15.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(SpotifyDesign.green),
            modifier = Modifier.weight(1f).padding(start = 10.dp),
            decorationBox = { field ->
                Box {
                    if (query.isEmpty()) Text(hint, color = SpotifyDesign.subdued, fontSize = 15.sp, maxLines = 1)
                    field()
                }
            }
        )
    }
}

@Composable
private fun SpotifyShowGrid(shows: List<PodcastShow>, onOpen: (PodcastShow) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        shows.chunked(SpotifyShowGridColumns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { show ->
                    StableRemoteArtwork(
                        show.thumbnailUrl, contentDescription = show.title, contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(SpotifyDesign.corner))
                            .background(SpotifyDesign.placeholder)
                            .levyraPressable(onClick = { onOpen(show) }, onClickLabel = show.title, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                    )
                }
                repeat(SpotifyShowGridColumns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SpotifyShowList(shows: List<PodcastShow>, onOpen: (PodcastShow) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        shows.forEach { show ->
            Row(
                Modifier.fillMaxWidth().levyraPressable(onClick = { onOpen(show) }, onClickLabel = show.title, role = Role.Button, pressedScale = LevyraPressScale.Control),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StableRemoteArtwork(
                    show.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(SpotifyDesign.libraryCover).clip(RoundedCornerShape(SpotifyDesign.cornerSmall)).background(SpotifyDesign.placeholder)
                )
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(show.title, color = SpotifyDesign.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(show.author, color = SpotifyDesign.subdued, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun SpotifyPodcastShowPage(
    open: PodcastShowState,
    state: PodcastLibraryUiState,
    copy: PodcastCopy,
    currentId: String?,
    podcasts: PodcastLibraryViewModel,
    play: (List<Track>, Track) -> Unit
) {
    val show = open.show
    val episodes = open.page?.episodes?.episodes.orEmpty()
    val subscribed = show.browseId in state.subscribedIds
    Column(
        Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(SpotifyShowTint, SpotifyDesign.base), endY = 900f))
    ) {
        IconButton(onClick = podcasts::closeShow, modifier = Modifier.padding(start = 4.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = LocalLevyraStrings.current.back, tint = SpotifyDesign.text)
        }
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            StableRemoteArtwork(
                show.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(SpotifyShowCover).clip(RoundedCornerShape(SpotifyDesign.corner)).background(SpotifyDesign.placeholder)
            )
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(
                    show.title, color = SpotifyDesign.text, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold,
                    maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
                )
                Text(show.author, color = SpotifyDesign.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Box(
            Modifier.padding(start = 16.dp, top = 18.dp).height(32.dp).clip(CircleShape)
                .border(BorderStroke(1.dp, if (subscribed) SpotifyDesign.green else SpotifyDesign.subdued), CircleShape)
                .levyraPressable(onClick = { podcasts.toggleSubscription(show) }, onClickLabel = copy.subscribe, role = Role.Button, pressedScale = LevyraPressScale.Control)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (subscribed) copy.subscribed else copy.subscribe, color = if (subscribed) SpotifyDesign.green else SpotifyDesign.text,
                fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
        }
        val description = open.page?.description.orEmpty()
        if (description.isNotBlank()) {
            Text(
                description, color = SpotifyDesign.subdued, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)
            )
        }
        Column(Modifier.padding(start = 16.dp, top = 20.dp)) {
            Text(copy.episodes, color = SpotifyDesign.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.padding(top = 6.dp).width(64.dp).height(3.dp).background(SpotifyDesign.green))
        }
        Spacer(Modifier.height(8.dp))
        when {
            open.loading && episodes.isEmpty() -> SpotifyAudioNotice(LocalLevyraStrings.current.searchingYouTubeMusic)
            open.failed && episodes.isEmpty() -> SpotifyAudioNotice(copy.loadFailed, LocalLevyraStrings.current.exploreSamplesRetry, podcasts::retryShow)
        }
        episodes.forEach { episode ->
            SpotifyEpisodeRow(episode, episode.album, state.progressById[episode.id]?.let { copy.played.takeIf { _ -> it.completed } }, currentId == episode.id) {
                play(episodes, episode)
            }
        }
        if (open.page?.episodes?.continuation != null) {
            SpotifyAudioNotice(text = "", action = if (open.loadingMore) null else copy.loadMore, onAction = podcasts::loadMoreEpisodes)
        }
    }
}

@Composable
private fun SpotifyEpisodeRow(episode: Track, detail: String, status: String?, current: Boolean = false, onPlay: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().levyraPressable(onClick = onPlay, onClickLabel = episode.title, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StableRemoteArtwork(
                episode.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(SpotifyEpisodeArt).clip(RoundedCornerShape(SpotifyDesign.cornerSmall)).background(SpotifyDesign.placeholder)
            )
            Text(
                episode.title, color = if (current) SpotifyDesign.greenText else SpotifyDesign.text, fontSize = 15.sp, lineHeight = 20.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 12.dp)
            )
        }
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                listOfNotNull(status, detail.ifBlank { null }).joinToString(" • "), color = SpotifyDesign.subdued, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
internal fun SpotifyLiveRadioHome(state: LevyraUiState, viewModel: HomeViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val factory = remember(context.applicationContext) { LiveRadioViewModel.factory(context.applicationContext) }
    val radio: LiveRadioViewModel = composeViewModel(key = "levyra-live-radio", factory = factory)
    val radioState by radio.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.languageCode) { radio.activate(state.languageCode) }
    val radioStrings = remember(state.languageCode) { LevyraLiveRadioCatalog.forCode(state.languageCode) }
    val library = remember(state.languageCode) { radioLibraryCopy(state.languageCode) }
    val strings = LocalLevyraStrings.current
    val currentId = state.currentTrack?.id?.removePrefix("live-radio:")
    var countryOpen by remember { mutableStateOf(false) }
    var languageOpen by remember { mutableStateOf(false) }
    var editorOpen by remember { mutableStateOf(false) }
    val countryLabel = radioState.selectedCountryCode?.let { localizedCountryName(it, state.languageCode).ifBlank { it } } ?: radioStrings.worldwide
    val play: (RadioStation) -> Unit = viewModel::playLiveRadio
    val favoriteIds = radioState.favoriteIds
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SpotifyPodcastSearch(radioState.query, radioStrings.searchHint, radio::setQuery)
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpotifyRadioPill(Icons.Rounded.Public, countryLabel, radioState.selectedCountryCode != null) { countryOpen = true }
            SpotifyRadioPill(Icons.Rounded.Translate, radioState.selectedLanguage ?: radioStrings.languages, radioState.selectedLanguage != null) { languageOpen = true }
        }
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(RadioCategory.entries, key = { "radio-category-${it.name}" }) { category ->
                val selected = category == radioState.category
                Box(
                    Modifier.height(SpotifyDesign.chipHeight).clip(CircleShape).background(if (selected) SpotifyDesign.green else SpotifyDesign.chip)
                        .levyraPressable(onClick = { radio.selectCategory(category) }, onClickLabel = radioStrings.category(category), role = Role.Tab, pressedScale = LevyraPressScale.Control)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) { Text(radioStrings.category(category), color = if (selected) Color.Black else SpotifyDesign.text, fontSize = 14.sp, maxLines = 1) }
            }
        }
        if (radioState.query.isBlank()) {
            if (radioState.favorites.isNotEmpty()) SpotifyStationShelf(library.favorites, radioState.favorites, currentId, play)
            SpotifyCustomStationShelf(library.custom, library.add, radioState.customStations, currentId, play) { editorOpen = true }
            if (radioState.recent.isNotEmpty()) SpotifyStationShelf(strings.recent, radioState.recent, currentId, play)
        }
        Column {
            SpotifyHomeHeading(
                if (radioState.query.isNotBlank()) radioStrings.searchHint
                else if (radioState.category == RadioCategory.Popular) radioStrings.popularIn(countryLabel)
                else radioStrings.category(radioState.category)
            )
            Spacer(Modifier.height(8.dp))
            radioState.stations.forEach { station ->
                SpotifyStationRow(station, station.uuid == currentId, station.uuid.lowercase() in favoriteIds, { play(station) }) { radio.toggleFavorite(station) }
            }
            when {
                radioState.loading && radioState.stations.isEmpty() -> SpotifyAudioNotice(strings.searchingYouTubeMusic)
                radioState.error != null && radioState.stations.isEmpty() -> SpotifyAudioNotice(radioState.error.orEmpty(), strings.exploreSamplesRetry, radio::retry)
                radioState.stations.isEmpty() -> SpotifyAudioNotice(radioStrings.noStations)
                radioState.canLoadMore && !radioState.loadingMore -> SpotifyAudioNotice("", radioStrings.loadMore, radio::loadMore)
            }
        }
    }
    if (countryOpen) RadioDirectoryDialog(
        title = radioStrings.countries,
        entries = radioState.countries,
        selected = radioState.selectedCountryCode,
        allLabel = radioStrings.worldwide,
        display = { entry -> localizedCountryName(entry.code, state.languageCode).ifBlank { entry.name } },
        value = com.luc4n3x.levyra.feature.radio.RadioDirectoryEntry::code,
        onDismiss = { countryOpen = false },
        onSelect = {
            radio.selectCountry(it)
            countryOpen = false
        }
    )
    if (languageOpen) RadioDirectoryDialog(
        title = radioStrings.languages,
        entries = radioState.languages,
        selected = radioState.selectedLanguage,
        allLabel = radioStrings.worldwide,
        display = com.luc4n3x.levyra.feature.radio.RadioDirectoryEntry::name,
        value = com.luc4n3x.levyra.feature.radio.RadioDirectoryEntry::name,
        onDismiss = { languageOpen = false },
        onSelect = {
            radio.selectLanguage(it)
            languageOpen = false
        }
    )
    if (editorOpen) CustomRadioDialog(
        station = null, saving = radioState.savingCustom, error = radioState.customError,
        onDismiss = { editorOpen = false }, onChanged = radio::clearCustomError,
        onSave = { name, url, image -> radio.saveCustom(name, url, image, null) { editorOpen = false } }
    )
}

@Composable
private fun SpotifyRadioPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.height(SpotifyDesign.chipHeight).clip(CircleShape)
            .border(BorderStroke(1.dp, if (active) SpotifyDesign.green else SpotifyDesign.faint), CircleShape)
            .levyraPressable(onClick = onClick, onClickLabel = label, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (active) SpotifyDesign.green else SpotifyDesign.text, modifier = Modifier.size(16.dp))
        Text(label, color = SpotifyDesign.text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp).widthIn(max = 160.dp))
        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = SpotifyDesign.subdued, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SpotifyStationRow(station: RadioStation, current: Boolean, favorite: Boolean, onPlay: () -> Unit, onFavorite: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().levyraPressable(onClick = onPlay, onClickLabel = station.name, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpotifyStationArt(station, SpotifyDesign.rowCover + 8.dp, 30.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(station.name, color = if (current) SpotifyDesign.greenText else SpotifyDesign.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(station.country, station.tags.take(2).joinToString(", ")).filter(String::isNotBlank).joinToString(" • "),
                color = SpotifyDesign.subdued, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onFavorite) {
            Icon(
                if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = station.name,
                tint = if (favorite) SpotifyDesign.green else SpotifyDesign.subdued
            )
        }
    }
}

@Composable
private fun SpotifyStationArt(station: RadioStation, size: androidx.compose.ui.unit.Dp, icon: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(SpotifyDesign.cornerSmall)).background(spotifyStationColor(station.name)),
        contentAlignment = Alignment.Center
    ) {
        if (station.faviconUrl.isNotBlank()) {
            StableRemoteArtwork(
                station.faviconUrl, contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(0.62f).aspectRatio(1f).clip(RoundedCornerShape(SpotifyDesign.corner)).background(Color.White)
            )
        } else {
            Icon(Icons.Rounded.Radio, contentDescription = null, tint = Color.White, modifier = Modifier.size(icon))
        }
    }
}

@Composable
private fun SpotifyCustomStationShelf(
    title: String,
    addLabel: String,
    stations: List<RadioStation>,
    currentId: String?,
    onPlay: (RadioStation) -> Unit,
    onAdd: () -> Unit
) {
    SpotifyHomeShelf(title) {
        item(key = "custom-add") {
            Column(Modifier.width(140.dp).levyraPressable(onClick = onAdd, onClickLabel = addLabel, role = Role.Button, pressedScale = LevyraPressScale.Tile)) {
                Box(
                    Modifier.size(140.dp).clip(RoundedCornerShape(SpotifyDesign.cornerSmall)).background(SpotifyDesign.highlight),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.Add, contentDescription = null, tint = SpotifyDesign.text, modifier = Modifier.size(44.dp)) }
                Text(addLabel, color = SpotifyDesign.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(top = 8.dp))
            }
        }
        items(stations, key = { "custom-${it.uuid}" }) { station -> SpotifyStationCard(station, station.uuid == currentId) { onPlay(station) } }
    }
}

@Composable
private fun SpotifyStationShelf(title: String, stations: List<RadioStation>, currentId: String?, onPlay: (RadioStation) -> Unit) {
    SpotifyHomeShelf(title) {
        items(stations, key = { "station-$title-${it.uuid}" }) { station -> SpotifyStationCard(station, station.uuid == currentId) { onPlay(station) } }
    }
}

@Composable
private fun SpotifyStationCard(station: RadioStation, current: Boolean, onClick: () -> Unit) {
    Column(Modifier.width(140.dp).levyraPressable(onClick = onClick, onClickLabel = station.name, role = Role.Button, pressedScale = LevyraPressScale.Tile)) {
        SpotifyStationArt(station, 140.dp, 52.dp)
        Text(
            station.name, color = if (current) SpotifyDesign.greenText else SpotifyDesign.text,
            fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp)
        )
        Text(station.country, color = SpotifyDesign.subdued, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SpotifyAudioNotice(text: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (text.isNotBlank()) Text(text, color = SpotifyDesign.subdued, fontSize = 14.sp)
        if (action != null && onAction != null) {
            Box(
                Modifier.heightIn(min = 40.dp).clip(CircleShape).background(Color.White)
                    .levyraPressable(onClick = onAction, onClickLabel = action, role = Role.Button, pressedScale = LevyraPressScale.Control)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) { Text(action, color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

private val SpotifyShowTint = Color(0xFF5A4636)
private val SpotifyStationColors = listOf(0xFF8C1932, 0xFF148A08, 0xFF1E3264, 0xFFE8115B, 0xFF7358FF, 0xFFBA5D07, 0xFF477D95, 0xFF8D67AB)

private fun spotifyStationColor(name: String): Color =
    Color(SpotifyStationColors[(name.hashCode() and Int.MAX_VALUE) % SpotifyStationColors.size])

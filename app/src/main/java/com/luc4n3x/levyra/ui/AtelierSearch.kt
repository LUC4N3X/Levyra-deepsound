package com.luc4n3x.levyra.ui

import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonAddAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.SearchArtistDiscography
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.atelier.AtelierAmbientBackdrop
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.i18n.AtelierCopy
import com.luc4n3x.levyra.ui.i18n.LevyraAtelierStrings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.library.LibraryArtwork
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.SearchViewModel
import java.util.Locale

private val AtelierSearchScopes = listOf(
    SearchFilter.All,
    SearchFilter.Songs,
    SearchFilter.Albums,
    SearchFilter.Artists,
    SearchFilter.Playlists
)
private val AtelierSearchScopesWithVideo = AtelierSearchScopes + SearchFilter.Videos
private const val AtelierSearchSongPreview = 5
private const val AtelierSearchVideoPreview = 3
private const val AtelierSearchSuggestionLimit = 5
private const val AtelierSearchRecentSleeves = 3
private val AtelierSearchWideArtwork: Dp = 85.dp
private val AtelierSearchFieldHeight: Dp = 52.dp
private val AtelierSearchFieldMaxWidth: Dp = 560.dp
private val AtelierSearchRecentSleeveSize: Dp = 168.dp
private val AtelierSearchTopSleeveSize: Dp = 208.dp
private val AtelierSearchFieldBrush = Brush.verticalGradient(listOf(AtelierDesign.panel, AtelierDesign.cabinet))
private val AtelierSearchRemoveScrim = Color(0xA6000000)

private fun atelierSearchAlbumKey(album: AlbumHit): String = album.browseId.ifBlank { "${album.title}|${album.artist}" }
private fun atelierSearchArtistKey(artist: ArtistHit): String = artist.browseId.ifBlank { artist.name }
private fun atelierSearchPlaylistKey(playlist: PlaylistHit): String = playlist.playlistId.ifBlank { playlist.browseId.ifBlank { playlist.title } }
private fun atelierSearchCaps(text: String, code: String): String = text.uppercase(Locale.forLanguageTag(code))
private fun atelierSearchMeta(vararg parts: String): String =
    parts.filter(String::isNotBlank).distinctBy { it.lowercase() }.joinToString(" · ")

@Immutable
private data class AtelierSearchContent(
    val songs: List<Track>,
    val artists: List<ArtistHit>,
    val albums: List<AlbumHit>,
    val playlists: List<PlaylistHit>,
    val videos: List<Track>
) {
    fun countFor(filter: SearchFilter): Int = when (filter) {
        SearchFilter.All -> songs.size + artists.size + albums.size + playlists.size + videos.size
        SearchFilter.Songs -> songs.size
        SearchFilter.Artists -> artists.size
        SearchFilter.Albums -> albums.size
        SearchFilter.Playlists -> playlists.size
        SearchFilter.Videos -> videos.size
    }
}

private fun atelierSearchContent(data: SearchResults): AtelierSearchContent = AtelierSearchContent(
    songs = (listOfNotNull(data.topTrack) + data.songs).distinctBy { it.id },
    artists = data.artists.distinctBy(::atelierSearchArtistKey),
    albums = data.albums.distinctBy(::atelierSearchAlbumKey),
    playlists = data.playlists.distinctBy(::atelierSearchPlaylistKey),
    videos = data.videos.distinctBy { it.id }
)

private class AtelierSearchMenuItem(val label: String, val action: () -> Unit)

@Immutable
private class AtelierSearchBrowseEntry(val key: String, val title: String, val onClick: () -> Unit)

@Composable
internal fun AtelierSearchEntry(viewModel: SearchViewModel, state: LevyraUiState, onTrackActions: (Track) -> Unit) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraAtelierStrings.forCode(strings.code) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val query = state.query
    val blank = query.isBlank()
    var focused by remember { mutableStateOf(false) }
    val leaveField: () -> Unit = {
        focusManager.clearFocus()
        keyboard?.hide()
    }
    LaunchedEffect(blank, strings.code) { if (blank) viewModel.ensureExplore(strings) }
    val recognitionBusy = state.recognitionState is com.luc4n3x.levyra.feature.recognition.RecognitionState.Listening ||
        state.recognitionState is com.luc4n3x.levyra.feature.recognition.RecognitionState.Identifying
    val toggleRecognition = rememberSearchVoiceActions(
        recognitionBusy, viewModel::startMusicRecognition, viewModel::cancelMusicRecognition, viewModel::setQuery, viewModel::searchNow
    ).component2()
    Column(Modifier.fillMaxSize().background(AtelierDesign.room).statusBarsPadding()) {
        AtelierSearchField(
            query = query,
            recognitionAvailable = state.recognitionAvailable,
            recognitionBusy = recognitionBusy,
            onRecognize = toggleRecognition,
            busy = !blank && (state.isSearching || state.searchPending),
            focused = focused,
            strings = strings,
            onQuery = viewModel::setQuery,
            onSearch = {
                leaveField()
                viewModel.searchNow(query)
            },
            onClear = { viewModel.setQuery("") },
            onFocused = { focused = it }
        )
        AtelierSearchScope(state.searchFilter, strings, viewModel::setSearchFilter)
        if (blank) {
            AtelierSearchLanding(viewModel, state, strings, copy, leaveField, onTrackActions)
        } else {
            AtelierSearchResults(viewModel, state, strings, copy, focused, leaveField, onTrackActions)
        }
    }
}

@Composable
private fun AtelierSearchField(
    query: String,
    recognitionAvailable: Boolean,
    recognitionBusy: Boolean,
    onRecognize: () -> Unit,
    busy: Boolean,
    focused: Boolean,
    strings: LevyraStrings,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onFocused: (Boolean) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val fieldStyle = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.Normal,
        color = AtelierDesign.text,
        textAlign = TextAlign.Center
    )
    Column(
        Modifier.fillMaxWidth().padding(start = AtelierDesign.gutter, end = AtelierDesign.gutter, top = 14.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            atelierSearchCaps(strings.search, strings.code),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.4.sp),
            color = AtelierDesign.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.widthIn(max = AtelierSearchFieldMaxWidth).fillMaxWidth().heightIn(min = AtelierSearchFieldHeight)
                .clip(AtelierDesign.capsuleShape)
                .background(AtelierSearchFieldBrush)
                .border(1.dp, if (focused) AtelierDesign.accent else AtelierDesign.hairlineStrong, AtelierDesign.capsuleShape)
                .pointerInput(focusRequester) { detectTapGestures { focusRequester.requestFocus() } }
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = AtelierSearchFieldHeight), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Search, null, tint = AtelierDesign.accent, modifier = Modifier.size(20.dp))
                }
                Box(Modifier.weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    if (query.isEmpty()) {
                        Text(
                            strings.searchPlaceholder,
                            style = fieldStyle.copy(color = AtelierDesign.textFaint),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { }
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQuery,
                        singleLine = true,
                        textStyle = fieldStyle,
                        cursorBrush = SolidColor(AtelierDesign.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                        modifier = Modifier.fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { onFocused(it.isFocused) }
                            .semantics { contentDescription = strings.searchPlaceholder }
                    )
                }
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    if (query.isNotEmpty()) {
                        AtelierSearchIconTarget(Icons.Rounded.Close, strings.clear, onClear)
                    } else if (recognitionAvailable) {
                        AtelierSearchIconTarget(Icons.Rounded.GraphicEq, strings.recognizeMusic, if (recognitionBusy) AtelierDesign.accent else AtelierDesign.textSecondary, onRecognize)
                    }
                }
            }
            if (busy) {
                LinearProgressIndicator(
                    color = AtelierDesign.accent,
                    trackColor = Color.Transparent,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 26.dp).fillMaxWidth().height(1.dp)
                )
            }
        }
    }
}

@Composable
private fun AtelierSearchScope(filter: SearchFilter, strings: LevyraStrings, onSelect: (SearchFilter) -> Unit) {
    val scopes = if (filter == SearchFilter.Videos) AtelierSearchScopesWithVideo else AtelierSearchScopes
    Column(Modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.Center,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(scopes, key = { it.name }, contentType = { "filter" }) { option ->
                AtelierSearchFilterTab(atelierSearchFilterLabel(option, strings), filter == option, strings.code) { onSelect(option) }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(AtelierDesign.hairline))
    }
}

@Composable
private fun AtelierSearchLanding(
    viewModel: SearchViewModel,
    state: LevyraUiState,
    strings: LevyraStrings,
    copy: AtelierCopy,
    leaveField: () -> Unit,
    onTrackActions: (Track) -> Unit
) {
    val recent = state.recentSearches
    val leadingRecent = remember(recent) { recent.take(AtelierSearchRecentSleeves) }
    val remainingRecent = remember(recent) { recent.drop(AtelierSearchRecentSleeves) }
    val currentId = state.currentTrack?.id
    val categories = state.exploreCategories
    val zones = remember(strings.code) { ExploreCatalog.getZones(strings) }
    val browseRows = remember(categories, zones) {
        val entries = categories.filter { it.params.isNotBlank() }.distinctBy { it.params }
            .map { category -> AtelierSearchBrowseEntry("category-${category.params}", category.title) { viewModel.openExploreCategory(category) } }
            .ifEmpty { zones.map { zone -> AtelierSearchBrowseEntry("zone-${zone.id}", zone.label) { viewModel.openExploreZone(zone) } } }
        entries.chunked(2)
    }
    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(top = 8.dp, bottom = levyraTabContentBottomPadding(state.currentTrack != null) + 16.dp)
    ) {
        if (recent.isEmpty()) {
            item(key = "search-prompt", contentType = "status") {
                Text(
                    strings.emptySearchPrompt,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AtelierDesign.textSecondary,
                    modifier = Modifier.padding(horizontal = AtelierDesign.gutter, vertical = 24.dp)
                )
            }
        } else {
            item(key = "recent-header", contentType = "header") {
                AtelierSearchSectionHeader(strings.recentSearches, strings.clear) { recent.forEach(viewModel::removeRecentSearch) }
            }
            item(key = "recent-sleeves", contentType = "carousel") {
                Box(Modifier.fillMaxWidth()) {
                    AtelierAmbientBackdrop(
                        leadingRecent.firstOrNull()?.thumbnailUrl?.takeIf(String::isNotBlank),
                        Modifier.matchParentSize(),
                        strength = 0.4f
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = AtelierDesign.gutter, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)
                    ) {
                        items(leadingRecent, key = { "recent-sleeve-${it.id}" }, contentType = { "sleeve" }) { track ->
                            AtelierSearchRecentSleeve(
                                track = track,
                                current = track.id == currentId,
                                strings = strings,
                                explicitLabel = copy.explicit,
                                onClick = {
                                    leaveField()
                                    viewModel.playFrom(recent, track)
                                },
                                onRemove = { viewModel.removeRecentSearch(track) },
                                onMore = { onTrackActions(track) }
                            )
                        }
                    }
                }
            }
            items(remainingRecent, key = { "recent-${it.id}" }, contentType = { "track" }) { track ->
                AtelierSearchTrackRow(
                    track = track,
                    meta = atelierSearchMeta(track.artist, track.album),
                    current = track.id == currentId,
                    explicitLabel = copy.explicit,
                    onClick = {
                        leaveField()
                        viewModel.playFrom(recent, track)
                    },
                    onMore = { onTrackActions(track) }
                ) {
                    AtelierSearchIconTarget(Icons.Rounded.Close, strings.removeFromRecentSearches) { viewModel.removeRecentSearch(track) }
                }
            }
        }
        if (browseRows.isNotEmpty()) {
            item(key = "browse-header", contentType = "header") { AtelierSearchSectionHeader(strings.explore) }
            items(browseRows, key = { row -> row.joinToString("+") { it.key } }, contentType = { "browse" }) { row ->
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = AtelierDesign.gutter, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { entry -> AtelierSearchBrowseTile(entry, Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AtelierSearchBrowseTile(entry: AtelierSearchBrowseEntry, modifier: Modifier) {
    Box(
        modifier.fillMaxHeight().heightIn(min = 72.dp).clip(AtelierDesign.cardShape).background(AtelierDesign.cabinet)
            .border(1.dp, AtelierDesign.hairline, AtelierDesign.cardShape)
            .clickable(role = Role.Button, onClick = entry.onClick).padding(14.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(entry.title, style = MaterialTheme.typography.titleSmall, color = AtelierDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AtelierSearchResults(
    viewModel: SearchViewModel,
    state: LevyraUiState,
    strings: LevyraStrings,
    copy: AtelierCopy,
    focused: Boolean,
    leaveField: () -> Unit,
    onTrackActions: (Track) -> Unit
) {
    val query = state.query
    val data = state.searchData
    val filter = state.searchFilter
    val currentId = state.currentTrack?.id
    val content = remember(data) { atelierSearchContent(data) }
    val suggestions = remember(query, state.searchSuggestions) {
        refineSearchQuerySuggestions(query, state.searchSuggestions).take(AtelierSearchSuggestionLimit)
    }
    val discography = data.discography.takeIf { filter == SearchFilter.All }
    val hasRows = content.countFor(filter) > 0 || discography != null
    val listState = rememberSaveable(filter.name, query, saver = LazyListState.Saver) { LazyListState() }
    val loadedCount = content.countFor(filter)
    AtelierSearchFetchEffects(viewModel, state, listState, filter, query, loadedCount)
    val topResult = content.songs.firstOrNull()
    val previewSongs = remember(content) { content.songs.drop(1).take(AtelierSearchSongPreview) }
    val followed: (ArtistHit) -> Boolean = { hit -> atelierSearchIsFollowed(hit, state) }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(bottom = levyraTabContentBottomPadding(state.currentTrack != null) + 16.dp)
    ) {
        atelierSearchSuggestions(focused, suggestions, query, viewModel, leaveField)
        atelierSearchBody(
            hasRows, state, strings, copy, filter, query, content, discography, topResult, previewSongs, currentId,
            viewModel, leaveField, onTrackActions, followed
        )
        atelierSearchSectionLoading(filter, state)
    }
}

@Composable
private fun AtelierSearchFetchEffects(
    viewModel: SearchViewModel,
    state: LevyraUiState,
    listState: LazyListState,
    filter: SearchFilter,
    query: String,
    loadedCount: Int
) {
    val nearEnd by remember(listState) { derivedStateOf { atelierSearchNearEnd(listState) } }
    LaunchedEffect(nearEnd, filter, query, loadedCount) {
        if (nearEnd && filter != SearchFilter.All && loadedCount > 0) viewModel.loadMoreSearchSection(filter)
    }
    val settled = !state.isSearching && !state.searchPending
    var requestedFilters by remember(query) { mutableStateOf(emptySet<SearchFilter>()) }
    LaunchedEffect(filter, query, settled) {
        if (settled && filter != SearchFilter.All && loadedCount == 0 && filter !in requestedFilters) {
            requestedFilters = requestedFilters + filter
            viewModel.setSearchFilter(filter)
        }
    }
}

private fun atelierSearchNearEnd(listState: LazyListState): Boolean {
    val info = listState.layoutInfo
    return info.totalItemsCount > 0 && (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 4
}

private fun atelierSearchIsFollowed(hit: ArtistHit, state: LevyraUiState): Boolean =
    (hit.browseId.isNotBlank() && hit.browseId in state.followedArtistKeys) || hit.name.trim().lowercase() in state.followedArtistKeys

private fun LazyListScope.atelierSearchSuggestions(
    focused: Boolean,
    suggestions: List<String>,
    query: String,
    viewModel: SearchViewModel,
    leaveField: () -> Unit
) {
    if (focused && suggestions.isNotEmpty()) {
        items(suggestions, key = { "suggestion-$it" }, contentType = { "suggestion" }) { suggestion ->
            AtelierSearchSuggestionRow(query, suggestion) {
                leaveField()
                viewModel.setQuery(suggestion)
                viewModel.searchNow(suggestion)
            }
        }
    }
}

private fun LazyListScope.atelierSearchBody(
    hasRows: Boolean,
    state: LevyraUiState,
    strings: LevyraStrings,
    copy: AtelierCopy,
    filter: SearchFilter,
    query: String,
    content: AtelierSearchContent,
    discography: SearchArtistDiscography?,
    topResult: Track?,
    previewSongs: List<Track>,
    currentId: String?,
    viewModel: SearchViewModel,
    leaveField: () -> Unit,
    onTrackActions: (Track) -> Unit,
    followed: (ArtistHit) -> Boolean
) {
    if (!hasRows) {
        item(key = "search-status", contentType = "status") {
            AtelierSearchStatus(state, strings, filter) { viewModel.searchNow(query) }
        }
    } else {
        when (filter) {
            SearchFilter.All -> atelierSearchAllSections(
                content, discography, topResult, previewSongs, currentId, strings, copy, viewModel, leaveField, onTrackActions, followed
            )
            SearchFilter.Songs -> items(content.songs, key = { "song-${it.id}" }, contentType = { "track" }) { track ->
                AtelierSearchSongItem(track, content.songs, track.id == currentId, copy, viewModel, leaveField, onTrackActions)
            }
            SearchFilter.Videos -> items(content.videos, key = { "video-${it.id}" }, contentType = { "video" }) { track ->
                AtelierSearchVideoItem(track, content.videos, track.id == currentId, strings, copy, viewModel, leaveField, onTrackActions)
            }
            SearchFilter.Artists -> atelierSearchArtistRows(content, strings, copy, viewModel, leaveField, followed)
            SearchFilter.Albums -> items(content.albums, key = { "album-${atelierSearchAlbumKey(it)}" }, contentType = { "item-row" }) { album ->
                AtelierSearchItemRow(
                    title = album.title,
                    meta = atelierSearchMeta(album.artist, album.year),
                    explicit = album.explicit,
                    explicitLabel = copy.explicit,
                    artwork = { LibraryArtwork(album.thumbnailUrl, album.title, Modifier.size(AtelierDesign.rowArtwork), AtelierDesign.sleeveShape, false) },
                    onClick = { leaveField(); viewModel.openAlbum(album) }
                ) {
                    AtelierSearchMoreTarget(strings.more) { atelierSearchAlbumMenu(album, strings, viewModel) }
                }
            }
            SearchFilter.Playlists -> items(content.playlists, key = { "playlist-${atelierSearchPlaylistKey(it)}" }, contentType = { "item-row" }) { playlist ->
                AtelierSearchItemRow(
                    title = playlist.title,
                    meta = atelierSearchMeta(playlist.author, playlist.trackCountLabel),
                    explicit = false,
                    explicitLabel = copy.explicit,
                    artwork = { LibraryArtwork(playlist.thumbnailUrl, playlist.title, Modifier.size(AtelierDesign.rowArtwork), AtelierDesign.sleeveShape, false) },
                    onClick = { leaveField(); viewModel.openPlaylistHit(playlist) }
                ) {
                    AtelierSearchMoreTarget(strings.more) { atelierSearchPlaylistMenu(playlist, strings, viewModel) }
                }
            }
        }
    }
}

private fun LazyListScope.atelierSearchAllSections(
    content: AtelierSearchContent,
    discography: SearchArtistDiscography?,
    topResult: Track?,
    previewSongs: List<Track>,
    currentId: String?,
    strings: LevyraStrings,
    copy: AtelierCopy,
    viewModel: SearchViewModel,
    leaveField: () -> Unit,
    onTrackActions: (Track) -> Unit,
    followed: (ArtistHit) -> Boolean
) {
    if (topResult != null) {
        item(key = "top-result", contentType = "top-result") {
            AtelierSearchTopResult(
                track = topResult,
                current = topResult.id == currentId,
                strings = strings,
                explicitLabel = copy.explicit,
                onPlay = {
                    leaveField()
                    viewModel.playFrom(content.songs, topResult)
                },
                onMore = { onTrackActions(topResult) }
            )
        }
    }
    if (previewSongs.isNotEmpty()) {
        item(key = "songs-header", contentType = "header") {
            AtelierSearchSectionHeader(strings.songsPlain, strings.more.takeIf { content.songs.size > AtelierSearchSongPreview + 1 }) { viewModel.setSearchFilter(SearchFilter.Songs) }
        }
        items(previewSongs, key = { "song-${it.id}" }, contentType = { "track" }) { track ->
            AtelierSearchSongItem(track, content.songs, track.id == currentId, copy, viewModel, leaveField, onTrackActions)
        }
    }
    if (content.artists.isNotEmpty()) {
        item(key = "artists-header", contentType = "header") {
            AtelierSearchSectionHeader(strings.artists, strings.more) { viewModel.setSearchFilter(SearchFilter.Artists) }
        }
        item(key = "artists-row", contentType = "carousel") {
            LazyRow(contentPadding = PaddingValues(horizontal = AtelierDesign.gutter), horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
                items(content.artists, key = { "artist-${atelierSearchArtistKey(it)}" }, contentType = { "artist" }) { artist ->
                    AtelierSearchArtistCard(artist, followed(artist), strings, { leaveField(); viewModel.openArtistFromHit(artist) }) { viewModel.toggleFollowArtist(artist) }
                }
            }
        }
    }
    if (discography != null) {
        item(key = "discography", contentType = "discography") {
            Box(Modifier.padding(start = AtelierDesign.gutter, end = AtelierDesign.gutter, top = AtelierDesign.sectionGap)) {
                SearchDiscographySection(
                    discography = discography,
                    onOpenRelease = { leaveField(); viewModel.openAlbum(it) },
                    onOpenPlaylist = { leaveField(); viewModel.openPlaylistHit(it) },
                    onOpenArtist = { leaveField(); viewModel.openArtistFromHit(it) }
                )
            }
        }
    } else if (content.albums.isNotEmpty()) {
        item(key = "albums-header", contentType = "header") {
            AtelierSearchSectionHeader(strings.albumsPlain, strings.more) { viewModel.setSearchFilter(SearchFilter.Albums) }
        }
        item(key = "albums-row", contentType = "carousel") {
            LazyRow(contentPadding = PaddingValues(horizontal = AtelierDesign.gutter), horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
                items(content.albums, key = { "album-${atelierSearchAlbumKey(it)}" }, contentType = { "sleeve" }) { album ->
                    AtelierSearchSleeve(
                        title = album.title,
                        subtitle = album.artist,
                        imageUrl = album.thumbnailUrl,
                        explicit = album.explicit,
                        explicitLabel = copy.explicit,
                        onClick = { leaveField(); viewModel.openAlbum(album) },
                        menu = { atelierSearchAlbumMenu(album, strings, viewModel) }
                    )
                }
            }
        }
    }
    if (content.playlists.isNotEmpty()) {
        item(key = "playlists-header", contentType = "header") {
            AtelierSearchSectionHeader(strings.playlistsPlain, strings.more) { viewModel.setSearchFilter(SearchFilter.Playlists) }
        }
        item(key = "playlists-row", contentType = "carousel") {
            LazyRow(contentPadding = PaddingValues(horizontal = AtelierDesign.gutter), horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
                items(content.playlists, key = { "playlist-${atelierSearchPlaylistKey(it)}" }, contentType = { "sleeve" }) { playlist ->
                    AtelierSearchSleeve(
                        title = playlist.title,
                        subtitle = playlist.author,
                        imageUrl = playlist.thumbnailUrl,
                        explicit = false,
                        explicitLabel = copy.explicit,
                        onClick = { leaveField(); viewModel.openPlaylistHit(playlist) },
                        menu = { atelierSearchPlaylistMenu(playlist, strings, viewModel) }
                    )
                }
            }
        }
    }
    if (content.videos.isNotEmpty()) {
        item(key = "videos-header", contentType = "header") {
            AtelierSearchSectionHeader(strings.video, strings.more.takeIf { content.videos.size > AtelierSearchVideoPreview }) { viewModel.setSearchFilter(SearchFilter.Videos) }
        }
        items(content.videos.take(AtelierSearchVideoPreview), key = { "video-${it.id}" }, contentType = { "video" }) { track ->
            AtelierSearchVideoItem(track, content.videos, track.id == currentId, strings, copy, viewModel, leaveField, onTrackActions)
        }
    }
}

private fun LazyListScope.atelierSearchArtistRows(
    content: AtelierSearchContent,
    strings: LevyraStrings,
    copy: AtelierCopy,
    viewModel: SearchViewModel,
    leaveField: () -> Unit,
    followed: (ArtistHit) -> Boolean
) {
    items(content.artists, key = { "artist-${atelierSearchArtistKey(it)}" }, contentType = { "artist-row" }) { artist ->
        val isFollowed = followed(artist)
        AtelierSearchItemRow(
            title = artist.name,
            meta = artist.subscribers,
            explicit = false,
            explicitLabel = copy.explicit,
            artwork = { LibraryArtwork(artist.thumbnailUrl, artist.name, Modifier.size(AtelierDesign.rowArtwork), CircleShape, false) },
            onClick = { leaveField(); viewModel.openArtistFromHit(artist) }
        ) {
            AtelierSearchIconTarget(
                if (isFollowed) Icons.Rounded.HowToReg else Icons.Rounded.PersonAddAlt,
                if (isFollowed) strings.followingArtist else strings.followArtist
            ) { viewModel.toggleFollowArtist(artist) }
        }
    }
}

private fun LazyListScope.atelierSearchSectionLoading(filter: SearchFilter, state: LevyraUiState) {
    if (filter != SearchFilter.All && filter in state.searchSectionLoading) {
        item(key = "section-loading", contentType = "status") {
            Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                LevyraLoadingIndicator(color = AtelierDesign.accent)
            }
        }
    }
}

private fun atelierSearchAlbumMenu(album: AlbumHit, strings: LevyraStrings, viewModel: SearchViewModel): List<AtelierSearchMenuItem> = listOf(
    AtelierSearchMenuItem(strings.openAlbum) { viewModel.openAlbum(album) },
    AtelierSearchMenuItem(strings.download) { viewModel.exportAlbumHit(album) }
)

private fun atelierSearchPlaylistMenu(playlist: PlaylistHit, strings: LevyraStrings, viewModel: SearchViewModel): List<AtelierSearchMenuItem> = listOf(
    AtelierSearchMenuItem(strings.play) { viewModel.playPlaylistHit(playlist) },
    AtelierSearchMenuItem(strings.downloadPlaylist) { viewModel.exportPlaylistHit(playlist) }
)

private fun atelierSearchFilterLabel(filter: SearchFilter, strings: LevyraStrings): String = when (filter) {
    SearchFilter.All -> strings.all
    SearchFilter.Songs -> strings.songsPlain
    SearchFilter.Artists -> strings.artists
    SearchFilter.Albums -> strings.albumsPlain
    SearchFilter.Playlists -> strings.playlistsPlain
    SearchFilter.Videos -> strings.video
}

@Composable
private fun AtelierSearchFilterTab(label: String, selected: Boolean, code: String, onClick: () -> Unit) {
    Column(
        Modifier.heightIn(min = 48.dp).selectable(selected = selected, role = Role.Tab, onClick = onClick).padding(horizontal = 6.dp).width(IntrinsicSize.Max),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            atelierSearchCaps(label, code),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.2.sp),
            color = if (selected) AtelierDesign.accentBright else AtelierDesign.textSecondary,
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(if (selected) AtelierDesign.accent else Color.Transparent))
    }
}

@Composable
private fun AtelierSearchSuggestionRow(query: String, suggestion: String, onClick: () -> Unit) {
    val label = remember(query, suggestion) {
        buildAnnotatedString {
            searchSuggestionSegments(query, suggestion).forEach { segment ->
                withStyle(SpanStyle(color = if (segment.novel) AtelierDesign.text else AtelierDesign.textSecondary)) { append(segment.text) }
            }
        }
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = AtelierDesign.gutter),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = AtelierDesign.textFaint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AtelierSearchStatus(state: LevyraUiState, strings: LevyraStrings, filter: SearchFilter, onRetry: () -> Unit) {
    val busy = state.isSearching || state.searchPending
    val error = state.searchError
    val failed = error == strings.searchFailed || filter in state.searchData.failedSections
    Column(
        Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            busy -> LevyraLoadingIndicator(color = AtelierDesign.accent)
            else -> {
                Text(
                    if (failed) strings.searchFailed else error ?: strings.emptySearchPrompt,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AtelierDesign.textSecondary,
                    textAlign = TextAlign.Center
                )
                if (failed) {
                    Box(
                        Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onRetry).padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            atelierSearchCaps(strings.playlistStudioRetry, strings.code),
                            style = MaterialTheme.typography.labelMedium,
                            color = AtelierDesign.accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AtelierSearchSectionHeader(label: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    val code = LocalLevyraStrings.current.code
    Row(
        Modifier.fillMaxWidth().padding(start = AtelierDesign.gutter, end = 8.dp, top = AtelierDesign.sectionGap - 12.dp, bottom = 4.dp).heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            atelierSearchCaps(label, code),
            style = MaterialTheme.typography.labelMedium,
            color = AtelierDesign.accent,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        if (actionLabel != null) {
            Row(
                Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onAction).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(atelierSearchCaps(actionLabel, code), style = MaterialTheme.typography.labelSmall, color = AtelierDesign.textSecondary)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = AtelierDesign.textSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun AtelierSearchSongItem(
    track: Track,
    songs: List<Track>,
    current: Boolean,
    copy: AtelierCopy,
    viewModel: SearchViewModel,
    leaveField: () -> Unit,
    onTrackActions: (Track) -> Unit
) {
    AtelierSearchTrackRow(
        track = track,
        meta = atelierSearchMeta(track.artist, track.album),
        current = current,
        explicitLabel = copy.explicit,
        onClick = {
            leaveField()
            viewModel.playFrom(songs, track)
        },
        onMore = { onTrackActions(track) }
    )
}

@Composable
private fun AtelierSearchVideoItem(
    track: Track,
    videos: List<Track>,
    current: Boolean,
    strings: LevyraStrings,
    copy: AtelierCopy,
    viewModel: SearchViewModel,
    leaveField: () -> Unit,
    onTrackActions: (Track) -> Unit
) {
    AtelierSearchTrackRow(
        track = track,
        meta = atelierSearchMeta(strings.video, track.artist),
        current = current,
        explicitLabel = copy.explicit,
        artworkWidth = AtelierSearchWideArtwork,
        onClick = {
            leaveField()
            viewModel.playFrom(videos, track)
        },
        onMore = { onTrackActions(track) }
    )
}

@Composable
private fun AtelierSearchTrackRow(
    track: Track,
    meta: String,
    current: Boolean,
    explicitLabel: String,
    artworkWidth: Dp = AtelierDesign.rowArtwork,
    onClick: () -> Unit,
    onMore: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    val moreLabel = LocalLevyraStrings.current.more
    AtelierSearchItemRow(
        title = track.title,
        meta = meta,
        explicit = track.explicit,
        explicitLabel = explicitLabel,
        current = current,
        artwork = {
            Box(Modifier.size(width = artworkWidth, height = AtelierDesign.rowArtwork).clip(AtelierDesign.sleeveShape).background(AtelierDesign.panel)) {
                CoverImage(track, Modifier.fillMaxSize())
            }
        },
        onClick = onClick
    ) {
        trailing?.invoke()
        AtelierSearchIconTarget(Icons.Rounded.MoreVert, moreLabel, onMore)
    }
}

@Composable
private fun AtelierSearchRecentSleeve(
    track: Track,
    current: Boolean,
    strings: LevyraStrings,
    explicitLabel: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onMore: () -> Unit
) {
    Column(
        Modifier.width(AtelierSearchRecentSleeveSize)
            .then(if (current) Modifier.semantics { selected = true } else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            Box(
                Modifier.matchParentSize()
                    .shadow(12.dp, AtelierDesign.sleeveShape)
                    .border(1.dp, AtelierDesign.hairline, AtelierDesign.sleeveShape)
                    .clip(AtelierDesign.sleeveShape)
                    .background(AtelierDesign.panel)
            ) {
                CoverImage(track, Modifier.fillMaxSize(), highRes = true)
            }
            Box(
                Modifier.align(Alignment.TopEnd).size(48.dp).clip(CircleShape)
                    .clickable(role = Role.Button, onClickLabel = strings.removeFromRecentSearches, onClick = onRemove)
                    .semantics { contentDescription = strings.removeFromRecentSearches },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(AtelierSearchRemoveScrim), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, null, tint = AtelierDesign.text, modifier = Modifier.size(14.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(Modifier.weight(1f).padding(top = 2.dp)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (current) AtelierDesign.accentBright else AtelierDesign.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (track.explicit) {
                        AtelierSearchExplicitMark(explicitLabel)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(track.artist, style = MaterialTheme.typography.bodySmall, color = AtelierDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            AtelierSearchIconTarget(Icons.Rounded.MoreVert, strings.more, onMore)
        }
    }
}

@Composable
private fun AtelierSearchTopResult(
    track: Track,
    current: Boolean,
    strings: LevyraStrings,
    explicitLabel: String,
    onPlay: () -> Unit,
    onMore: () -> Unit
) {
    Box(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        AtelierAmbientBackdrop(track.thumbnailUrl.takeIf(String::isNotBlank), Modifier.matchParentSize(), strength = 0.8f)
        Column(
            Modifier.fillMaxWidth()
                .then(if (current) Modifier.semantics { selected = true } else Modifier)
                .clickable(role = Role.Button, onClick = onPlay)
                .padding(start = AtelierDesign.gutter, end = 8.dp, top = 12.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                atelierSearchCaps(strings.topResult, strings.code),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.4.sp),
                color = AtelierDesign.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(end = 12.dp).semantics { heading() }
            )
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.padding(end = 12.dp).size(AtelierSearchTopSleeveSize)
                    .shadow(20.dp, AtelierDesign.sleeveShape)
                    .border(1.dp, AtelierDesign.hairline, AtelierDesign.sleeveShape)
                    .clip(AtelierDesign.sleeveShape)
                    .background(AtelierDesign.panel)
            ) {
                CoverImage(track, Modifier.fillMaxSize(), highRes = true)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (current) AtelierDesign.accentBright else AtelierDesign.text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (track.explicit) {
                            AtelierSearchExplicitMark(explicitLabel)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            atelierSearchMeta(track.artist, track.album),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AtelierDesign.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier.size(48.dp).clip(AtelierDesign.cardShape).background(AtelierDesign.accent)
                        .clickable(role = Role.Button, onClickLabel = strings.play, onClick = onPlay)
                        .semantics { contentDescription = strings.play },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = AtelierDesign.onAccent, modifier = Modifier.size(26.dp))
                }
                AtelierSearchIconTarget(Icons.Rounded.MoreVert, strings.more, onMore)
            }
        }
    }
}

@Composable
private fun AtelierSearchItemRow(
    title: String,
    meta: String,
    explicit: Boolean,
    explicitLabel: String,
    current: Boolean = false,
    artwork: @Composable () -> Unit,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = AtelierDesign.rowHeight)
                .then(if (current) Modifier.semantics { selected = true } else Modifier)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = AtelierDesign.gutter, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            artwork()
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (current) AtelierDesign.accentBright else AtelierDesign.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (meta.isNotBlank() || explicit) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (explicit) {
                            AtelierSearchExplicitMark(explicitLabel)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(meta, style = MaterialTheme.typography.bodySmall, color = AtelierDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            trailing()
        }
        Box(Modifier.padding(start = AtelierDesign.gutter).fillMaxWidth().height(1.dp).background(AtelierDesign.hairline))
    }
}

@Composable
private fun AtelierSearchSleeve(
    title: String,
    subtitle: String,
    imageUrl: String,
    explicit: Boolean,
    explicitLabel: String,
    onClick: () -> Unit,
    menu: () -> List<AtelierSearchMenuItem>
) {
    val moreLabel = LocalLevyraStrings.current.more
    Column(Modifier.width(AtelierDesign.albumCard).clickable(role = Role.Button, onClick = onClick)) {
        LibraryArtwork(
            imageUrl,
            title,
            Modifier.fillMaxWidth().aspectRatio(1f).shadow(10.dp, AtelierDesign.sleeveShape).border(1.dp, AtelierDesign.hairline, AtelierDesign.sleeveShape),
            AtelierDesign.sleeveShape,
            false
        )
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Column(Modifier.weight(1f).padding(top = 2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = AtelierDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (explicit) {
                        AtelierSearchExplicitMark(explicitLabel)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AtelierDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            AtelierSearchMoreTarget(moreLabel, menu)
        }
    }
}

@Composable
private fun AtelierSearchArtistCard(artist: ArtistHit, followed: Boolean, strings: LevyraStrings, onClick: () -> Unit, onFollow: () -> Unit) {
    Column(Modifier.width(AtelierDesign.artistCircle), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
            LibraryArtwork(artist.thumbnailUrl, artist.name, Modifier.size(AtelierDesign.artistCircle), CircleShape, false)
            Spacer(Modifier.height(8.dp))
            Text(artist.name, style = MaterialTheme.typography.titleSmall, color = AtelierDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
        Box(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onFollow),
            contentAlignment = Alignment.Center
        ) {
            Text(
                atelierSearchCaps(if (followed) strings.followingArtist else strings.followArtist, strings.code),
                style = MaterialTheme.typography.labelSmall,
                color = if (followed) AtelierDesign.accent else AtelierDesign.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AtelierSearchExplicitMark(label: String) {
    Box(
        Modifier.size(14.dp).border(1.dp, AtelierDesign.hairlineStrong, AtelierDesign.sleeveShape)
            .clearAndSetSemantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text("E", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp), color = AtelierDesign.textSecondary, maxLines = 1)
    }
}

@Composable
private fun AtelierSearchMoreTarget(label: String, items: () -> List<AtelierSearchMenuItem>) {
    var open by remember { mutableStateOf(false) }
    Box {
        AtelierSearchIconTarget(Icons.Rounded.MoreVert, label) { open = true }
        DropdownMenu(open, { open = false }) {
            items().forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    onClick = {
                        open = false
                        item.action()
                    }
                )
            }
        }
    }
}

@Composable
private fun AtelierSearchIconTarget(icon: ImageVector, label: String, onClick: () -> Unit) =
    AtelierSearchIconTarget(icon, label, AtelierDesign.textSecondary, onClick)

@Composable
private fun AtelierSearchIconTarget(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(22.dp))
    }
}

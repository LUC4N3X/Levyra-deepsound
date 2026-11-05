package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.PersonAddAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.SearchArtistDiscography
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.recognition.RecognitionState
import com.luc4n3x.levyra.ui.i18n.LevyraMonoHomeStrings
import com.luc4n3x.levyra.ui.i18n.LevyraMonoSearchStrings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.MonoSearchCopy
import com.luc4n3x.levyra.ui.library.LibraryArtwork
import com.luc4n3x.levyra.ui.smartaudio.LyricsSearchEntry
import com.luc4n3x.levyra.ui.theme.MonoDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.SearchViewModel

private val MonoSearchScopes = listOf(
    SearchFilter.All,
    SearchFilter.Songs,
    SearchFilter.Albums,
    SearchFilter.Artists,
    SearchFilter.Playlists,
    SearchFilter.Videos
)
private const val MonoSearchSongPreview = 4
private const val MonoSearchVideoPreview = 3
private const val MonoSearchSuggestionLimit = 6
private const val MonoSearchIdleRecent = 6
private const val MonoSearchRailLimit = 16
private val MonoSearchFieldHeight: Dp = 52.dp
private val MonoSearchWideArtwork: Dp = 85.dp
private val MonoSearchTopArtwork: Dp = 96.dp
private val MonoSearchArtistAvatar: Dp = 88.dp

private enum class MonoSearchPane { Idle, Recall, Results }

private fun monoSearchAlbumKey(album: AlbumHit): String = album.browseId.ifBlank { "${album.title}|${album.artist}" }
private fun monoSearchArtistKey(artist: ArtistHit): String = artist.browseId.ifBlank { artist.name }
private fun monoSearchPlaylistKey(playlist: PlaylistHit): String = playlist.playlistId.ifBlank { playlist.browseId.ifBlank { playlist.title } }
private fun monoSearchMeta(vararg parts: String): String =
    parts.filter(String::isNotBlank).distinctBy { it.lowercase() }.joinToString(" · ")

@Immutable
private data class MonoSearchContent(
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

private fun monoSearchContent(data: SearchResults): MonoSearchContent = MonoSearchContent(
    songs = (listOfNotNull(data.topTrack) + data.songs).distinctBy { it.id },
    artists = data.artists.distinctBy(::monoSearchArtistKey),
    albums = data.albums.distinctBy(::monoSearchAlbumKey),
    playlists = data.playlists.distinctBy(::monoSearchPlaylistKey),
    videos = data.videos.distinctBy { it.id }
)

private class MonoSearchMenuItem(val label: String, val action: () -> Unit)

@Composable
internal fun MonoSearchEntry(viewModel: SearchViewModel, state: LevyraUiState, onTrackActions: (Track) -> Unit) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraMonoSearchStrings.forCode(strings.code) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val animated = LocalAnimationsEnabled.current
    val query = state.query
    val blank = query.isBlank()
    var focused by remember { mutableStateOf(false) }
    val leaveField: () -> Unit = {
        focusManager.clearFocus()
        keyboard?.hide()
    }
    BackHandler(enabled = focused) { leaveField() }
    val recognitionBusy = state.recognitionState is RecognitionState.Listening || state.recognitionState is RecognitionState.Identifying
    val voice = rememberSearchVoiceActions(
        recognitionBusy, viewModel::startMusicRecognition, viewModel::cancelMusicRecognition, viewModel::setQuery, viewModel::searchNow
    )
    val pane = when {
        !blank -> MonoSearchPane.Results
        focused -> MonoSearchPane.Recall
        else -> MonoSearchPane.Idle
    }
    Column(Modifier.fillMaxSize().background(MonoDesign.base).statusBarsPadding()) {
        AnimatedVisibility(
            visible = !focused,
            enter = if (animated) expandVertically() + fadeIn() else EnterTransition.None,
            exit = if (animated) shrinkVertically() + fadeOut() else ExitTransition.None
        ) {
            MonoScreenTitle(strings.search, Modifier.padding(top = 16.dp, bottom = 4.dp))
        }
        MonoSearchField(
            query = query,
            focused = focused,
            busy = !blank && (state.isSearching || state.searchPending),
            recognitionAvailable = state.recognitionAvailable,
            recognitionBusy = recognitionBusy,
            strings = strings,
            copy = copy,
            animated = animated,
            onQuery = viewModel::setQuery,
            onSearch = {
                leaveField()
                viewModel.searchNow(query)
            },
            onClear = { viewModel.setQuery("") },
            onFocused = { focused = it },
            onVoice = voice.voice,
            onRecognize = voice.recognize
        )
        AnimatedContent(
            targetState = pane,
            transitionSpec = {
                if (animated) fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(140))
                else EnterTransition.None togetherWith ExitTransition.None
            },
            label = "mono-search-pane",
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { target ->
            when (target) {
                MonoSearchPane.Idle -> MonoSearchIdle(viewModel, state, strings, copy, onTrackActions)
                MonoSearchPane.Recall -> MonoSearchRecall(viewModel, state, strings, copy, leaveField, onTrackActions)
                MonoSearchPane.Results -> MonoSearchResults(viewModel, state, strings, copy, focused, leaveField, onTrackActions)
            }
        }
    }
}

@Composable
private fun MonoSearchField(
    query: String,
    focused: Boolean,
    busy: Boolean,
    recognitionAvailable: Boolean,
    recognitionBusy: Boolean,
    strings: LevyraStrings,
    copy: MonoSearchCopy,
    animated: Boolean,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onFocused: (Boolean) -> Unit,
    onVoice: () -> Unit,
    onRecognize: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val fieldStyle = MaterialTheme.typography.bodyLarge.copy(color = MonoDesign.text)
    val border by animateColorAsState(if (focused) MonoDesign.accent else MonoDesign.hairline, tween(if (animated) 220 else 0), label = "mono-search-border")
    val fill by animateColorAsState(if (focused) MonoDesign.raised else MonoDesign.surface, tween(if (animated) 220 else 0), label = "mono-search-fill")
    Row(
        Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.weight(1f).heightIn(min = MonoSearchFieldHeight)
                .clip(MonoDesign.cardShape)
                .background(fill)
                .border(1.dp, border, MonoDesign.cardShape)
                .pointerInput(focusRequester) { detectTapGestures { focusRequester.requestFocus() } }
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = MonoSearchFieldHeight), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Search, null, tint = if (focused) MonoDesign.accent else MonoDesign.textSecondary, modifier = Modifier.size(20.dp))
                }
                Box(Modifier.weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            strings.searchPlaceholder,
                            style = fieldStyle.copy(color = MonoDesign.textFaint),
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
                        cursorBrush = SolidColor(MonoDesign.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                        modifier = Modifier.fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { onFocused(it.isFocused) }
                            .semantics { contentDescription = strings.searchPlaceholder }
                    )
                }
                if (query.isNotEmpty()) {
                    MonoSearchIconTarget(Icons.Rounded.Close, strings.clear, MonoDesign.textSecondary, onClear)
                } else if (recognitionAvailable) {
                    MonoSearchIconTarget(Icons.Rounded.GraphicEq, strings.recognizeMusic, if (recognitionBusy) MonoDesign.accent else MonoDesign.textSecondary, onRecognize)
                }
            }
            if (busy) {
                MonoWaveLine(
                    playing = true,
                    color = MonoDesign.accent.copy(alpha = 0.7f),
                    cycles = 14f,
                    strokeWidth = 1.dp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 18.dp).fillMaxWidth().height(6.dp)
                )
            }
        }
        AnimatedVisibility(
            visible = !focused,
            enter = if (animated) expandHorizontally() + fadeIn() else EnterTransition.None,
            exit = if (animated) shrinkHorizontally() + fadeOut() else ExitTransition.None
        ) {
            Box(Modifier.padding(start = 8.dp)) {
                MonoSearchIconTarget(Icons.Rounded.Mic, copy.voiceSearch, MonoDesign.text, onVoice)
            }
        }
    }
}

@Composable
private fun MonoSearchIdle(
    viewModel: SearchViewModel,
    state: LevyraUiState,
    strings: LevyraStrings,
    copy: MonoSearchCopy,
    onTrackActions: (Track) -> Unit
) {
    val currentId = state.currentTrack?.id
    val lastPlayed = remember(state.recentListens) { state.recentListens.distinctBy { it.id }.take(MonoSearchRailLimit) }
    val similar = remember(state.personalOrbitTracks, lastPlayed) {
        val shown = lastPlayed.mapTo(HashSet()) { it.id }
        state.personalOrbitTracks.distinctBy { it.id }.filterNot { it.id in shown }.take(MonoSearchRailLimit)
    }
    val recent = state.recentSearches
    val recentPreview = remember(recent) { recent.take(MonoSearchIdleRecent) }
    LaunchedEffect(strings.code) { viewModel.ensureExplore(strings) }
    val zones = remember(strings) { ExploreCatalog.getZones(strings) }
    val browseSource = remember(state.exploreCategories, zones) { monoBrowseTiles(state.exploreCategories, zones) }
    val browse = remember(browseSource, state.isExploreCategoriesLoading, state.exploreCategoryArtwork) {
        monoBrowseEntries(
            browseSource, state.isExploreCategoriesLoading, viewModel::openExploreCategory, viewModel::openExploreZone,
            state.exploreCategoryArtwork, viewModel::ensureExploreCategoryArtwork
        )
    }
    val homeCopy = remember(strings.code) { LevyraMonoHomeStrings.forCode(strings.code) }
    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(top = 4.dp, bottom = levyraTabContentBottomPadding(state.currentTrack != null) + 16.dp)
    ) {
        item(key = "lyrics-entry", contentType = "entry") {
            LyricsSearchEntry(onOpen = viewModel::openLyricsPhraseSearch, modifier = Modifier.padding(horizontal = MonoDesign.gutter, vertical = 4.dp))
        }
        if (lastPlayed.isNotEmpty()) {
            item(key = "last-header", contentType = "header") {
                MonoSectionHeader(copy.lastPlayed, Modifier.padding(top = 20.dp, bottom = MonoDesign.headerToContent), overline = copy.lastPlayedOverline)
            }
            item(key = "last-rail", contentType = "rail") {
                MonoSearchTrackRail(lastPlayed, currentId, { viewModel.playFrom(lastPlayed, it) }, onTrackActions)
            }
        }
        if (recentPreview.isNotEmpty()) {
            item(key = "recent-header", contentType = "header") {
                MonoSectionHeader(
                    strings.recentSearches,
                    Modifier.padding(top = MonoDesign.sectionGap, bottom = 4.dp),
                    overline = copy.recentOverline,
                    action = strings.clear,
                    onAction = { recent.forEach(viewModel::removeRecentSearch) }
                )
            }
            items(recentPreview, key = { "recent-${it.id}" }, contentType = { "recent" }) { track ->
                MonoRecentRow(track, strings, { viewModel.playFrom(recent, track) }, { onTrackActions(track) }) { viewModel.removeRecentSearch(track) }
            }
        }
        if (similar.isNotEmpty()) {
            item(key = "similar-header", contentType = "header") {
                MonoSectionHeader(copy.similarToTaste, Modifier.padding(top = MonoDesign.sectionGap, bottom = MonoDesign.headerToContent), overline = copy.similarOverline)
            }
            item(key = "similar-rail", contentType = "rail") {
                MonoSearchTrackRail(similar, currentId, { viewModel.playFrom(similar, it) }, onTrackActions)
            }
        }
        monoBrowseSections("search-browse", browse, state.isExploreCategoriesLoading, homeCopy)
        if (lastPlayed.isEmpty() && recentPreview.isEmpty() && similar.isEmpty() && browse.moods.isEmpty() && browse.genres.isEmpty() && !state.isExploreCategoriesLoading) {
            item(key = "idle-prompt", contentType = "status") { MonoSearchPrompt(strings.emptySearchPrompt) }
        }
    }
}

@Composable
private fun MonoSearchRecall(
    viewModel: SearchViewModel,
    state: LevyraUiState,
    strings: LevyraStrings,
    copy: MonoSearchCopy,
    leaveField: () -> Unit,
    onTrackActions: (Track) -> Unit
) {
    val recent = state.recentSearches
    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(top = 4.dp, bottom = levyraTabContentBottomPadding(state.currentTrack != null) + 16.dp)
    ) {
        if (recent.isEmpty()) {
            item(key = "recall-prompt", contentType = "status") { MonoSearchPrompt(strings.emptySearchPrompt) }
        } else {
            item(key = "recall-header", contentType = "header") {
                MonoSectionHeader(
                    strings.recentSearches,
                    Modifier.padding(top = 8.dp, bottom = 4.dp),
                    overline = copy.recentOverline,
                    action = strings.clear,
                    onAction = { recent.forEach(viewModel::removeRecentSearch) }
                )
            }
            items(recent, key = { "recall-${it.id}" }, contentType = { "recent" }) { track ->
                MonoRecentRow(track, strings, {
                    leaveField()
                    viewModel.playFrom(recent, track)
                }, { onTrackActions(track) }) { viewModel.removeRecentSearch(track) }
            }
        }
    }
}

@Composable
private fun MonoSearchPrompt(text: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MonoWaveLine(playing = false, color = MonoDesign.textFaint, modifier = Modifier.width(96.dp).height(20.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MonoDesign.textSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MonoSearchResults(
    viewModel: SearchViewModel,
    state: LevyraUiState,
    strings: LevyraStrings,
    copy: MonoSearchCopy,
    focused: Boolean,
    leaveField: () -> Unit,
    onTrackActions: (Track) -> Unit
) {
    val query = state.query
    val data = state.searchData
    val filter = state.searchFilter
    val currentId = state.currentTrack?.id
    val content = remember(data) { monoSearchContent(data) }
    val suggestions = remember(query, state.searchSuggestions) {
        refineSearchQuerySuggestions(query, state.searchSuggestions).take(MonoSearchSuggestionLimit)
    }
    val discography = data.discography.takeIf { filter == SearchFilter.All }
    val loadedCount = content.countFor(filter)
    val hasRows = loadedCount > 0 || discography != null
    val listState = rememberSaveable(filter.name, query, saver = LazyListState.Saver) { LazyListState() }
    MonoSearchFetchEffects(viewModel, state, listState, filter, query, loadedCount)
    val followed: (ArtistHit) -> Boolean = { hit ->
        (hit.browseId.isNotBlank() && hit.browseId in state.followedArtistKeys) || hit.name.trim().lowercase() in state.followedArtistKeys
    }
    val actions = MonoSearchActions(viewModel, leaveField, onTrackActions)
    Column(Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            contentPadding = PaddingValues(horizontal = MonoDesign.gutter, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(MonoSearchScopes, key = { it.name }, contentType = { "filter" }) { option ->
                MonoPill(
                    label = monoSearchFilterLabel(option, strings),
                    selected = filter == option,
                    onClick = { viewModel.setSearchFilter(option) },
                    modifier = Modifier.semantics {
                        role = Role.Tab
                        selected = filter == option
                    }
                )
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(top = 6.dp, bottom = levyraTabContentBottomPadding(state.currentTrack != null) + 16.dp)
        ) {
            if (focused && suggestions.isNotEmpty()) {
                item(key = "suggestions-overline", contentType = "overline") {
                    MonoOverline(copy.suggestionsOverline, Modifier.padding(horizontal = MonoDesign.gutter, vertical = 8.dp))
                }
                items(suggestions, key = { "suggestion-$it" }, contentType = { "suggestion" }) { suggestion ->
                    MonoSuggestionRow(query, suggestion, { viewModel.setQuery(suggestion) }) {
                        leaveField()
                        viewModel.setQuery(suggestion)
                        viewModel.searchNow(suggestion)
                    }
                }
                item(key = "suggestions-rule", contentType = "rule") {
                    Box(Modifier.padding(horizontal = MonoDesign.gutter, vertical = 10.dp).fillMaxWidth().height(1.dp).background(MonoDesign.hairline))
                }
            }
            if (!hasRows) {
                item(key = "search-status", contentType = "status") {
                    MonoSearchStatus(state, strings, filter) { viewModel.searchNow(query) }
                }
            } else {
                monoSearchBody(filter, content, discography, currentId, strings, copy, actions, followed)
            }
            if (filter != SearchFilter.All && filter in state.searchSectionLoading) {
                item(key = "section-loading", contentType = "status") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                        MonoWaveLine(playing = true, color = MonoDesign.accent, modifier = Modifier.width(72.dp).height(18.dp))
                    }
                }
            }
        }
    }
}

private class MonoSearchActions(
    val viewModel: SearchViewModel,
    val leaveField: () -> Unit,
    val onTrackActions: (Track) -> Unit
) {
    fun play(list: List<Track>, track: Track) {
        leaveField()
        viewModel.playFrom(list, track)
    }

    fun openAlbum(album: AlbumHit) {
        leaveField()
        viewModel.openAlbum(album)
    }

    fun openPlaylist(playlist: PlaylistHit) {
        leaveField()
        viewModel.openPlaylistHit(playlist)
    }

    fun openArtist(artist: ArtistHit) {
        leaveField()
        viewModel.openArtistFromHit(artist)
    }
}

@Composable
private fun MonoSearchFetchEffects(
    viewModel: SearchViewModel,
    state: LevyraUiState,
    listState: LazyListState,
    filter: SearchFilter,
    query: String,
    loadedCount: Int
) {
    val nearEnd by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            info.totalItemsCount > 0 && (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 4
        }
    }
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

private fun LazyListScope.monoSearchBody(
    filter: SearchFilter,
    content: MonoSearchContent,
    discography: SearchArtistDiscography?,
    currentId: String?,
    strings: LevyraStrings,
    copy: MonoSearchCopy,
    actions: MonoSearchActions,
    followed: (ArtistHit) -> Boolean
) {
    when (filter) {
        SearchFilter.All -> monoSearchAll(content, discography, currentId, strings, copy, actions)
        SearchFilter.Songs -> items(content.songs, key = { "song-${it.id}" }, contentType = { "track" }) { track ->
            MonoSearchTrackRow(track, monoSearchMeta(track.artist, track.album), track.id == currentId, copy, { actions.play(content.songs, track) }) { actions.onTrackActions(track) }
        }
        SearchFilter.Videos -> items(content.videos, key = { "video-${it.id}" }, contentType = { "video" }) { track ->
            MonoSearchTrackRow(track, monoSearchMeta(strings.video, track.artist), track.id == currentId, copy, { actions.play(content.videos, track) }, MonoSearchWideArtwork) { actions.onTrackActions(track) }
        }
        SearchFilter.Artists -> items(content.artists, key = { "artist-${monoSearchArtistKey(it)}" }, contentType = { "artist-row" }) { artist ->
            val isFollowed = followed(artist)
            MonoSearchItemRow(
                title = artist.name,
                meta = artist.subscribers,
                explicit = false,
                copy = copy,
                artwork = { LibraryArtwork(artist.thumbnailUrl, artist.name, Modifier.size(MonoDesign.rowArtwork), CircleShape, false) },
                onClick = { actions.openArtist(artist) }
            ) {
                MonoSearchIconTarget(
                    if (isFollowed) Icons.Rounded.HowToReg else Icons.Rounded.PersonAddAlt,
                    if (isFollowed) strings.followingArtist else strings.followArtist,
                    if (isFollowed) MonoDesign.accent else MonoDesign.textSecondary
                ) { actions.viewModel.toggleFollowArtist(artist) }
            }
        }
        SearchFilter.Albums -> items(content.albums, key = { "album-${monoSearchAlbumKey(it)}" }, contentType = { "item-row" }) { album ->
            MonoSearchItemRow(
                title = album.title,
                meta = monoSearchMeta(album.artist, album.year),
                explicit = album.explicit,
                copy = copy,
                artwork = { LibraryArtwork(album.thumbnailUrl, album.title, Modifier.size(MonoDesign.rowArtwork), MonoDesign.artShape, false) },
                onClick = { actions.openAlbum(album) }
            ) {
                MonoSearchMoreTarget(strings.more) { monoSearchAlbumMenu(album, strings, actions.viewModel) }
            }
        }
        SearchFilter.Playlists -> items(content.playlists, key = { "playlist-${monoSearchPlaylistKey(it)}" }, contentType = { "item-row" }) { playlist ->
            MonoSearchItemRow(
                title = playlist.title,
                meta = monoSearchMeta(playlist.author, playlist.trackCountLabel),
                explicit = false,
                copy = copy,
                artwork = { LibraryArtwork(playlist.thumbnailUrl, playlist.title, Modifier.size(MonoDesign.rowArtwork), MonoDesign.artShape, false) },
                onClick = { actions.openPlaylist(playlist) }
            ) {
                MonoSearchMoreTarget(strings.more) { monoSearchPlaylistMenu(playlist, strings, actions.viewModel) }
            }
        }
    }
}

private fun LazyListScope.monoSearchAll(
    content: MonoSearchContent,
    discography: SearchArtistDiscography?,
    currentId: String?,
    strings: LevyraStrings,
    copy: MonoSearchCopy,
    actions: MonoSearchActions
) {
    val top = content.songs.firstOrNull()
    val previewSongs = content.songs.drop(1).take(MonoSearchSongPreview)
    val header: @Composable (String, String?, (() -> Unit)?) -> Unit = { title, action, onAction ->
        MonoSectionHeader(title, Modifier.padding(top = 28.dp, bottom = 6.dp), action = action, onAction = onAction)
    }
    if (top != null) {
        item(key = "top-result", contentType = "top-result") {
            MonoSearchTopResult(top, top.id == currentId, strings, copy, { actions.play(content.songs, top) }) { actions.onTrackActions(top) }
        }
    }
    if (previewSongs.isNotEmpty()) {
        item(key = "songs-header", contentType = "header") {
            header(strings.songsPlain, strings.more.takeIf { content.songs.size > MonoSearchSongPreview + 1 }) { actions.viewModel.setSearchFilter(SearchFilter.Songs) }
        }
        items(previewSongs, key = { "song-${it.id}" }, contentType = { "track" }) { track ->
            MonoSearchTrackRow(track, monoSearchMeta(track.artist, track.album), track.id == currentId, copy, { actions.play(content.songs, track) }) { actions.onTrackActions(track) }
        }
    }
    if (content.artists.isNotEmpty()) {
        item(key = "artists-header", contentType = "header") {
            header(strings.artists, strings.more) { actions.viewModel.setSearchFilter(SearchFilter.Artists) }
        }
        item(key = "artists-rail", contentType = "rail") {
            LazyRow(contentPadding = PaddingValues(horizontal = MonoDesign.gutter, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(content.artists, key = { "artist-${monoSearchArtistKey(it)}" }, contentType = { "artist" }) { artist ->
                    MonoSearchArtistAvatar(artist) { actions.openArtist(artist) }
                }
            }
        }
    }
    if (discography != null) {
        item(key = "discography", contentType = "discography") {
            Box(Modifier.padding(start = MonoDesign.gutter, end = MonoDesign.gutter, top = MonoDesign.sectionGap)) {
                SearchDiscographySection(
                    discography = discography,
                    onOpenRelease = actions::openAlbum,
                    onOpenPlaylist = actions::openPlaylist,
                    onOpenArtist = actions::openArtist
                )
            }
        }
    } else if (content.albums.isNotEmpty()) {
        item(key = "albums-header", contentType = "header") {
            header(strings.albumsPlain, strings.more) { actions.viewModel.setSearchFilter(SearchFilter.Albums) }
        }
        item(key = "albums-rail", contentType = "rail") {
            LazyRow(contentPadding = PaddingValues(horizontal = MonoDesign.gutter), horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)) {
                items(content.albums, key = { "album-${monoSearchAlbumKey(it)}" }, contentType = { "card" }) { album ->
                    MonoSearchCard(album.title, monoSearchMeta(album.artist, album.year), album.thumbnailUrl) { actions.openAlbum(album) }
                }
            }
        }
    }
    if (content.playlists.isNotEmpty()) {
        item(key = "playlists-header", contentType = "header") {
            header(strings.playlistsPlain, strings.more) { actions.viewModel.setSearchFilter(SearchFilter.Playlists) }
        }
        item(key = "playlists-rail", contentType = "rail") {
            LazyRow(contentPadding = PaddingValues(horizontal = MonoDesign.gutter), horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)) {
                items(content.playlists, key = { "playlist-${monoSearchPlaylistKey(it)}" }, contentType = { "card" }) { playlist ->
                    MonoSearchCard(playlist.title, playlist.author, playlist.thumbnailUrl) { actions.openPlaylist(playlist) }
                }
            }
        }
    }
    if (content.videos.isNotEmpty()) {
        item(key = "videos-header", contentType = "header") {
            header(strings.video, strings.more.takeIf { content.videos.size > MonoSearchVideoPreview }) { actions.viewModel.setSearchFilter(SearchFilter.Videos) }
        }
        items(content.videos.take(MonoSearchVideoPreview), key = { "video-${it.id}" }, contentType = { "video" }) { track ->
            MonoSearchTrackRow(track, monoSearchMeta(strings.video, track.artist), track.id == currentId, copy, { actions.play(content.videos, track) }, MonoSearchWideArtwork) { actions.onTrackActions(track) }
        }
    }
}

private fun monoSearchAlbumMenu(album: AlbumHit, strings: LevyraStrings, viewModel: SearchViewModel): List<MonoSearchMenuItem> = listOf(
    MonoSearchMenuItem(strings.openAlbum) { viewModel.openAlbum(album) },
    MonoSearchMenuItem(strings.download) { viewModel.exportAlbumHit(album) }
)

private fun monoSearchPlaylistMenu(playlist: PlaylistHit, strings: LevyraStrings, viewModel: SearchViewModel): List<MonoSearchMenuItem> = listOf(
    MonoSearchMenuItem(strings.play) { viewModel.playPlaylistHit(playlist) },
    MonoSearchMenuItem(strings.downloadPlaylist) { viewModel.exportPlaylistHit(playlist) }
)

private fun monoSearchFilterLabel(filter: SearchFilter, strings: LevyraStrings): String = when (filter) {
    SearchFilter.All -> strings.all
    SearchFilter.Songs -> strings.songsPlain
    SearchFilter.Artists -> strings.artists
    SearchFilter.Albums -> strings.albumsPlain
    SearchFilter.Playlists -> strings.playlistsPlain
    SearchFilter.Videos -> strings.video
}

@Composable
private fun MonoSearchStatus(state: LevyraUiState, strings: LevyraStrings, filter: SearchFilter, onRetry: () -> Unit) {
    val busy = state.isSearching || state.searchPending
    val error = state.searchError
    val failed = error == strings.searchFailed || filter in state.searchData.failedSections
    Column(
        Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MonoWaveLine(playing = busy, color = if (busy) MonoDesign.accent else MonoDesign.textFaint, modifier = Modifier.width(120.dp).height(28.dp))
        if (!busy) {
            Text(
                if (failed) strings.searchFailed else error ?: strings.emptySearchPrompt,
                style = MaterialTheme.typography.bodyLarge,
                color = MonoDesign.textSecondary,
                textAlign = TextAlign.Center
            )
            if (failed) MonoPill(strings.playlistStudioRetry, selected = false, onClick = onRetry, modifier = Modifier.semantics { role = Role.Button })
        }
    }
}

@Composable
private fun MonoSearchTrackRail(tracks: List<Track>, currentId: String?, onPlay: (Track) -> Unit, onTrackActions: (Track) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = MonoDesign.gutter), horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)) {
        items(tracks, key = { it.id }, contentType = { "cover" }) { track ->
            MonoSearchCover(track, track.id == currentId, { onPlay(track) }) { onTrackActions(track) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonoSearchCover(track: Track, current: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(
        Modifier.width(MonoDesign.card)
            .clip(MonoDesign.artShape)
            .semantics { selected = current }
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(Modifier.size(MonoDesign.card).clip(MonoDesign.artShape).background(MonoDesign.surface).border(1.dp, MonoDesign.hairline, MonoDesign.artShape)) {
            CoverImage(track, Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(8.dp))
        Text(track.title, style = MaterialTheme.typography.titleSmall, color = if (current) MonoDesign.accent else MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MonoSearchCard(title: String, subtitle: String, imageUrl: String, onClick: () -> Unit) {
    Column(Modifier.width(MonoDesign.card).clip(MonoDesign.artShape).clickable(role = Role.Button, onClick = onClick)) {
        LibraryArtwork(imageUrl, title, Modifier.size(MonoDesign.card).border(1.dp, MonoDesign.hairline, MonoDesign.artShape), MonoDesign.artShape, false)
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = MonoDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MonoSearchArtistAvatar(artist: ArtistHit, onClick: () -> Unit) {
    Column(
        Modifier.width(MonoSearchArtistAvatar).clip(MonoDesign.cardShape).clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LibraryArtwork(artist.thumbnailUrl, artist.name, Modifier.size(MonoSearchArtistAvatar).border(1.dp, MonoDesign.hairlineStrong, CircleShape), CircleShape, false)
        Spacer(Modifier.height(8.dp))
        Text(artist.name, style = MaterialTheme.typography.titleSmall, color = MonoDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonoRecentRow(track: Track, strings: LevyraStrings, onClick: () -> Unit, onLongClick: () -> Unit, onRemove: () -> Unit) {
    val label = remember(track.title, track.artist) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = MonoDesign.textSecondary)) { append(track.title) }
            if (track.artist.isNotBlank()) withStyle(SpanStyle(color = MonoDesign.textFaint)) { append("  ·  ${track.artist}") }
        }
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
            .padding(start = MonoDesign.gutter, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        MonoSearchIconTarget(Icons.Rounded.Close, strings.removeFromRecentSearches, MonoDesign.textFaint, onRemove)
    }
}

@Composable
private fun MonoSuggestionRow(query: String, suggestion: String, onFill: () -> Unit, onClick: () -> Unit) {
    val label = remember(query, suggestion) {
        buildAnnotatedString {
            searchSuggestionSegments(query, suggestion).forEach { segment ->
                withStyle(SpanStyle(color = if (segment.novel) MonoDesign.text else MonoDesign.textSecondary)) { append(segment.text) }
            }
        }
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick).padding(start = MonoDesign.gutter, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = MonoDesign.textFaint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        MonoSearchIconTarget(Icons.Rounded.NorthWest, suggestion, MonoDesign.textFaint, onFill)
    }
}

@Composable
private fun MonoSearchTopResult(
    track: Track,
    current: Boolean,
    strings: LevyraStrings,
    copy: MonoSearchCopy,
    onPlay: () -> Unit,
    onMore: () -> Unit
) {
    Row(
        Modifier.padding(horizontal = MonoDesign.gutter, vertical = 6.dp).fillMaxWidth()
            .monoPanel(MonoDesign.panelShape)
            .semantics { selected = current }
            .clickable(role = Role.Button, onClick = onPlay)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(MonoSearchTopArtwork).clip(MonoDesign.artShape).background(MonoDesign.raised)) {
            CoverImage(track, Modifier.fillMaxSize(), highRes = true)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MonoOverline(copy.topResultOverline, color = MonoDesign.accent)
            Text(
                track.title, style = MaterialTheme.typography.headlineSmall,
                color = if (current) MonoDesign.accent else MonoDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (track.explicit) {
                    MonoSearchExplicitMark(copy.explicit)
                    Spacer(Modifier.width(6.dp))
                }
                Text(monoSearchMeta(track.artist, track.album), style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(MonoDesign.accent)
                    .clickable(role = Role.Button, onClickLabel = strings.play, onClick = onPlay)
                    .semantics { contentDescription = strings.play },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = MonoDesign.onAccent, modifier = Modifier.size(26.dp))
            }
            MonoSearchIconTarget(Icons.Rounded.MoreVert, strings.more, MonoDesign.textSecondary, onMore)
        }
    }
}

@Composable
private fun MonoSearchTrackRow(
    track: Track,
    meta: String,
    current: Boolean,
    copy: MonoSearchCopy,
    onClick: () -> Unit,
    artworkWidth: Dp = MonoDesign.rowArtwork,
    onMore: () -> Unit
) {
    MonoSearchItemRow(
        title = track.title,
        meta = meta,
        explicit = track.explicit,
        copy = copy,
        current = current,
        artwork = {
            Box(Modifier.size(width = artworkWidth, height = MonoDesign.rowArtwork).clip(MonoDesign.artShape).background(MonoDesign.surface)) {
                CoverImage(track, Modifier.fillMaxSize())
            }
        },
        onClick = onClick
    ) {
        MonoSearchIconTarget(Icons.Rounded.MoreVert, LocalLevyraStrings.current.more, MonoDesign.textSecondary, onMore)
    }
}

@Composable
private fun MonoSearchItemRow(
    title: String,
    meta: String,
    explicit: Boolean,
    copy: MonoSearchCopy,
    current: Boolean = false,
    artwork: @Composable () -> Unit,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = MonoDesign.rowHeight)
            .semantics { selected = current }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = MonoDesign.gutter, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        artwork()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (current) MonoDesign.accent else MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (meta.isNotBlank() || explicit) {
                Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (explicit) {
                        MonoSearchExplicitMark(copy.explicit)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(meta, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        trailing()
    }
}

@Composable
internal fun MonoSearchExplicitMark(label: String) {
    Box(
        Modifier.size(14.dp).border(1.dp, MonoDesign.hairlineStrong, MonoDesign.artShape).clearAndSetSemantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text("E", style = MaterialTheme.typography.labelSmall, color = MonoDesign.textSecondary, maxLines = 1)
    }
}

@Composable
private fun MonoSearchMoreTarget(label: String, items: () -> List<MonoSearchMenuItem>) {
    var open by remember { mutableStateOf(false) }
    Box {
        MonoSearchIconTarget(Icons.Rounded.MoreVert, label, MonoDesign.textSecondary) { open = true }
        DropdownMenu(open, { open = false }, containerColor = MonoDesign.raised) {
            items().forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label, color = MonoDesign.text) },
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
internal fun MonoSearchIconTarget(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(22.dp))
    }
}

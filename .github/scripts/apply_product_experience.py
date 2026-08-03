from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected one match, found {count}\n--- OLD ---\n{old}")
    file.write_text(text.replace(old, new))


models = "app/src/main/java/com/luc4n3x/levyra/domain/Models.kt"
screen_vms = "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraScreenViewModels.kt"
app = "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"

# Richer search categories.
replace_once(
    models,
    '''enum class SearchFilter {
    All,
    Songs,
    Artists,
    Albums
}
''',
    '''enum class SearchFilter {
    All,
    Songs,
    Videos,
    Artists,
    Albums,
    Playlists
}
'''
)

# Keep secondary destinations available without occupying permanent bottom-nav slots.
replace_once(
    screen_vms,
    '''    fun openAlbum(album: AlbumHit) = root.openAlbum(album)
    fun openArtistByName(name: String) = root.openArtistByName(name)
    fun openArtistFromHit(hit: ArtistHit) = root.openArtistFromHit(hit)
    fun openSettings() = root.openSettings()
    fun openSearch() = root.selectTab(LevyraTab.Search)
''',
    '''    fun openAlbum(album: AlbumHit) = root.openAlbum(album)
    fun openArtistByName(name: String) = root.openArtistByName(name)
    fun openArtistFromHit(hit: ArtistHit) = root.openArtistFromHit(hit)
    fun openExplore() = root.selectTab(LevyraTab.Explore)
    fun openLibrary() = root.selectTab(LevyraTab.Library)
    fun openSettings() = root.openSettings()
    fun openSearch() = root.selectTab(LevyraTab.Search)
'''
)
replace_once(
    screen_vms,
    '''    fun openAlbum(album: AlbumHit) = root.openAlbum(album)
    fun openArtist(track: Track) = root.openArtist(track)
    fun openArtistFromHit(hit: ArtistHit) = root.openArtistFromHit(hit)
    fun play(track: Track) = root.play(track)
''',
    '''    fun openAlbum(album: AlbumHit) = root.openAlbum(album)
    fun openArtist(track: Track) = root.openArtist(track)
    fun openArtistFromHit(hit: ArtistHit) = root.openArtistFromHit(hit)
    fun openPlaylist(playlistId: String) = root.openPlaylist(playlistId)
    fun play(track: Track) = root.play(track)
    fun playPlaylist(playlistId: String) = root.playPlaylist(playlistId)
'''
)

# Domain helpers used by the UI.
replace_once(
    app,
    '''import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SearchResults
''',
    '''import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.filterPlaylistsForSearch
import com.luc4n3x.levyra.domain.isSearchVideo
import com.luc4n3x.levyra.domain.searchFiltersFor
'''
)

# Three permanent destinations: the player remains globally available through the mini player,
# and Explore remains reachable from Home discovery shortcuts.
replace_once(
    app,
    '''                TabButton(Icons.Rounded.Home, strings.home, selected == LevyraTab.Home) { onSelect(LevyraTab.Home) }
                TabButton(Icons.Rounded.Search, strings.search, selected == LevyraTab.Search) { onSelect(LevyraTab.Search) }
                TabButton(Icons.Rounded.Explore, strings.explore, selected == LevyraTab.Explore) { onSelect(LevyraTab.Explore) }
                TabButton(Icons.Rounded.LibraryMusic, strings.library, selected == LevyraTab.Library) { onSelect(LevyraTab.Library) }
                TabButton(Icons.Rounded.Album, strings.player, selected == LevyraTab.Player) { onSelect(LevyraTab.Player) }
''',
    '''                TabButton(Icons.Rounded.Home, strings.home, selected == LevyraTab.Home) { onSelect(LevyraTab.Home) }
                TabButton(Icons.Rounded.Search, strings.search, selected == LevyraTab.Search) { onSelect(LevyraTab.Search) }
                TabButton(Icons.Rounded.LibraryMusic, strings.library, selected == LevyraTab.Library) { onSelect(LevyraTab.Library) }
'''
)

# A useful, deterministic source for the Home quick actions.
replace_once(
    app,
    '''    val visiblePersonalTracks = remember(personalTracks) {
        LevyraPersonalOrbit.distinctRecordings(personalTracks)
            .take(LevyraPersonalOrbit.DISPLAY_LIMIT)
    }
    val visibleEditorialCollections = remember(editorialCollections, spotlightCandidate?.track?.id) {
''',
    '''    val visiblePersonalTracks = remember(personalTracks) {
        LevyraPersonalOrbit.distinctRecordings(personalTracks)
            .take(LevyraPersonalOrbit.DISPLAY_LIMIT)
    }
    val homeMixTracks = remember(visiblePersonalTracks, quickPicks?.tracks, spotlightTracks) {
        visiblePersonalTracks
            .ifEmpty { quickPicks?.tracks.orEmpty() }
            .ifEmpty { spotlightTracks }
            .distinctBy { it.id }
    }
    val visibleEditorialCollections = remember(editorialCollections, spotlightCandidate?.track?.id) {
'''
)
replace_once(
    app,
    '''                    MoodRow(
                        moods = state.moods,
                        selectedId = state.selectedMood?.id,
                        onSelect = viewModel::selectMood
                    )
                }
            }
''',
    '''                    MoodRow(
                        moods = state.moods,
                        selectedId = state.selectedMood?.id,
                        onSelect = viewModel::selectMood
                    )
                    HomeSectionInset {
                        HomeShortcutRow(
                            hasTracks = homeMixTracks.isNotEmpty(),
                            onShuffle = {
                                if (homeMixTracks.isNotEmpty()) viewModel.playAll(homeMixTracks.shuffled())
                            },
                            onFavorites = viewModel::openLibrary,
                            onNewReleases = { viewModel.searchNow(strings.newReleases) },
                            onGenres = viewModel::openExplore
                        )
                    }
                }
            }
'''
)

# Search state shared by chips, the filter menu, and result sections.
replace_once(
    app,
    '''    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var addTarget by remember { mutableStateOf<Track?>(null) }

    Column(
''',
    '''    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var addTarget by remember { mutableStateOf<Track?>(null) }
    val queryClean = state.query.trim()
    val searchData = state.searchData
    val videoTracks = remember(searchData.songs) {
        searchData.songs.filter { track -> track.isSearchVideo() }
    }
    val audioTracks = remember(searchData.songs) {
        searchData.songs.filterNot { track -> track.isSearchVideo() }
    }
    val matchingPlaylists = remember(queryClean, state.playlists) {
        filterPlaylistsForSearch(queryClean, state.playlists)
    }
    val availableFilters = remember(
        searchData.artists,
        searchData.albums,
        videoTracks,
        matchingPlaylists
    ) {
        searchFiltersFor(
            hasArtists = searchData.artists.isNotEmpty(),
            hasAlbums = searchData.albums.isNotEmpty(),
            hasVideos = videoTracks.isNotEmpty(),
            hasPlaylists = matchingPlaylists.isNotEmpty()
        )
    }
    LaunchedEffect(state.isSearching, state.searchFilter, availableFilters) {
        if (!state.isSearching && state.searchFilter !in availableFilters) {
            viewModel.setSearchFilter(SearchFilter.All)
        }
    }

    Column(
'''
)
replace_once(
    app,
    '''            onClear = {
                viewModel.setQuery("")
            }
        )
''',
    '''            onClear = {
                viewModel.setQuery("")
            },
            availableFilters = availableFilters,
            selectedFilter = state.searchFilter,
            onFilter = viewModel::setSearchFilter
        )
'''
)
replace_once(app, '''            val queryClean = state.query.trim()

            if (queryClean.isEmpty()) {
''', '''            if (queryClean.isEmpty()) {
                item {
                    QuickChips(
                        languageCode = state.languageCode,
                        onClick = { query ->
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.setQuery(query)
                            viewModel.searchNow(query)
                        }
                    )
                }
''')
replace_once(
    app,
    '''                    !state.searchData.isEmpty -> {
                        val data = state.searchData
                        val filter = state.searchFilter
''',
    '''                    !state.searchData.isEmpty || matchingPlaylists.isNotEmpty() -> {
                        val data = state.searchData
                        val filter = state.searchFilter
'''
)
replace_once(
    app,
    '''                            SearchFilterChips(
                                selected = filter,
                                hasArtists = data.artists.isNotEmpty(),
                                hasAlbums = data.albums.isNotEmpty(),
                                onSelect = viewModel::setSearchFilter
                            )
''',
    '''                            SearchFilterChips(
                                selected = filter,
                                hasArtists = data.artists.isNotEmpty(),
                                hasAlbums = data.albums.isNotEmpty(),
                                hasVideos = videoTracks.isNotEmpty(),
                                hasPlaylists = matchingPlaylists.isNotEmpty(),
                                onSelect = viewModel::setSearchFilter
                            )
'''
)
replace_once(
    app,
    '''                        if (filter == SearchFilter.All || filter == SearchFilter.Songs) {
                            val songs = if (filter == SearchFilter.All) data.songs.drop(if (data.topTrack != null) 1 else 0) else data.songs
                            if (songs.isNotEmpty()) {
                                item { SectionTitle(strings.songs) }
                                items(songs, key = { "search-song-${it.id}" }) { track ->
                                    SearchTrackCard(
                                        track = track,
                                        isCurrent = track.id == state.currentTrack?.id,
                                        isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                                        isResolving = state.isResolving && track.id == state.currentTrack?.id,
                                        isFavorite = track.id in state.favoriteIds,
                                        isDownloading = track.id in state.downloadingTrackIds,
                                        isDownloaded = track.id in state.downloadedTrackIds,
                                        downloadProgress = state.downloadProgressByTrackId[track.id],
                                        onClick = {
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                            viewModel.playFrom(data.songs, track)
                                        },
                                        onFavorite = { viewModel.toggleFavorite(track) },
                                        onAddToPlaylist = { addTarget = track },
                                        onDownload = { viewModel.exportTrack(track) },
                                        onArtist = { viewModel.openArtist(track) }
                                    )
                                }
                            }
                        }
''',
    '''                        if ((filter == SearchFilter.All || filter == SearchFilter.Videos) && videoTracks.isNotEmpty()) {
                            val videos = if (filter == SearchFilter.All) {
                                videoTracks.filterNot { track -> track.id == data.topTrack?.id }
                            } else {
                                videoTracks
                            }
                            if (videos.isNotEmpty()) {
                                item { SectionTitle(strings.video) }
                                items(videos, key = { "search-video-${it.id}" }) { track ->
                                    SearchTrackCard(
                                        track = track,
                                        isCurrent = track.id == state.currentTrack?.id,
                                        isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                                        isResolving = state.isResolving && track.id == state.currentTrack?.id,
                                        isFavorite = track.id in state.favoriteIds,
                                        isDownloading = track.id in state.downloadingTrackIds,
                                        isDownloaded = track.id in state.downloadedTrackIds,
                                        downloadProgress = state.downloadProgressByTrackId[track.id],
                                        onClick = {
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                            viewModel.playFrom(data.songs, track)
                                        },
                                        onFavorite = { viewModel.toggleFavorite(track) },
                                        onAddToPlaylist = { addTarget = track },
                                        onDownload = { viewModel.exportTrack(track) },
                                        onArtist = { viewModel.openArtist(track) }
                                    )
                                }
                            }
                        }
                        if ((filter == SearchFilter.All || filter == SearchFilter.Playlists) && matchingPlaylists.isNotEmpty()) {
                            item { SectionTitle(strings.playlists) }
                            items(matchingPlaylists, key = { "search-playlist-${it.id}" }) { playlist ->
                                SearchPlaylistResultRow(
                                    playlist = playlist,
                                    onOpen = { viewModel.openPlaylist(playlist.id) },
                                    onPlay = { viewModel.playPlaylist(playlist.id) }
                                )
                            }
                        }
                        if (filter == SearchFilter.All || filter == SearchFilter.Songs) {
                            val songs = if (filter == SearchFilter.All) {
                                audioTracks.filterNot { track -> track.id == data.topTrack?.id }
                            } else {
                                audioTracks
                            }
                            if (songs.isNotEmpty()) {
                                item { SectionTitle(strings.songs) }
                                items(songs, key = { "search-song-${it.id}" }) { track ->
                                    SearchTrackCard(
                                        track = track,
                                        isCurrent = track.id == state.currentTrack?.id,
                                        isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                                        isResolving = state.isResolving && track.id == state.currentTrack?.id,
                                        isFavorite = track.id in state.favoriteIds,
                                        isDownloading = track.id in state.downloadingTrackIds,
                                        isDownloaded = track.id in state.downloadedTrackIds,
                                        downloadProgress = state.downloadProgressByTrackId[track.id],
                                        onClick = {
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                            viewModel.playFrom(data.songs, track)
                                        },
                                        onFavorite = { viewModel.toggleFavorite(track) },
                                        onAddToPlaylist = { addTarget = track },
                                        onDownload = { viewModel.exportTrack(track) },
                                        onArtist = { viewModel.openArtist(track) }
                                    )
                                }
                            }
                        }
'''
)

# Filter labels are shared by the chips and the top-right filter menu.
replace_once(
    app,
    '''@Composable
private fun SearchHeader(
    query: String,
    isSearching: Boolean,
    onQuery: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit
) {
''',
    '''private fun searchFilterLabel(filter: SearchFilter, strings: LevyraStrings): String = when (filter) {
    SearchFilter.All -> strings.all
    SearchFilter.Songs -> strings.songsPlain
    SearchFilter.Videos -> strings.video
    SearchFilter.Artists -> strings.artistsLabelPlural
    SearchFilter.Albums -> strings.albumsPlain
    SearchFilter.Playlists -> strings.playlists
}

@Composable
private fun SearchHeader(
    query: String,
    isSearching: Boolean,
    onQuery: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    availableFilters: List<SearchFilter>,
    selectedFilter: SearchFilter,
    onFilter: (SearchFilter) -> Unit
) {
'''
)
replace_once(
    app,
    '''    val context = LocalContext.current
    val strings = LocalLevyraStrings.current

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
''',
    '''    val context = LocalContext.current
    val strings = LocalLevyraStrings.current
    var filterExpanded by rememberSaveable { mutableStateOf(false) }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
'''
)
replace_once(
    app,
    '''        IconButton(
            onClick = { Toast.makeText(context, strings.musicFiltersComingSoon, Toast.LENGTH_SHORT).show() },
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.Equalizer,
                contentDescription = strings.audioEngine,
                tint = LevyraText,
                modifier = Modifier.size(20.dp)
            )
        }
''',
    '''        Box {
            IconButton(
                onClick = { filterExpanded = true },
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (selectedFilter == SearchFilter.All) {
                            Color.White.copy(alpha = 0.05f)
                        } else {
                            LevyraCyan.copy(alpha = 0.16f)
                        },
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Equalizer,
                    contentDescription = searchFilterLabel(selectedFilter, strings),
                    tint = if (selectedFilter == SearchFilter.All) LevyraText else LevyraCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            DropdownMenu(
                expanded = filterExpanded,
                onDismissRequest = { filterExpanded = false }
            ) {
                availableFilters.forEach { filter ->
                    DropdownMenuItem(
                        text = { Text(searchFilterLabel(filter, strings)) },
                        leadingIcon = {
                            if (filter == selectedFilter) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = LevyraCyan)
                            }
                        },
                        onClick = {
                            filterExpanded = false
                            onFilter(filter)
                        }
                    )
                }
            }
        }
'''
)

replace_once(
    app,
    '''private fun SearchFilterChips(
    selected: SearchFilter,
    hasArtists: Boolean,
    hasAlbums: Boolean,
    onSelect: (SearchFilter) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val chips = buildList {
        add(SearchFilter.All to strings.all)
        add(SearchFilter.Songs to strings.songsPlain)
        if (hasArtists) add(SearchFilter.Artists to strings.artistsLabelPlural)
        if (hasAlbums) add(SearchFilter.Albums to strings.albumsPlain)
    }
''',
    '''private fun SearchFilterChips(
    selected: SearchFilter,
    hasArtists: Boolean,
    hasAlbums: Boolean,
    hasVideos: Boolean,
    hasPlaylists: Boolean,
    onSelect: (SearchFilter) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val chips = searchFiltersFor(
        hasArtists = hasArtists,
        hasAlbums = hasAlbums,
        hasVideos = hasVideos,
        hasPlaylists = hasPlaylists
    ).map { filter -> filter to searchFilterLabel(filter, strings) }
'''
)

# Local playlist results are first-class search results, without exposing destructive actions.
replace_once(
    app,
    '''@Composable
private fun SearchTrackCard(
''',
    '''@Composable
private fun SearchPlaylistResultRow(
    playlist: com.luc4n3x.levyra.domain.Playlist,
    onOpen: () -> Unit,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .pressable(onClick = onOpen)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            if (playlist.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = playlist.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = null,
                    tint = LevyraViolet,
                    modifier = Modifier.size(27.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = playlist.name,
                color = LevyraText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = LocalLevyraStrings.current.formatTrackCount(playlist.size),
                color = LevyraMuted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        IconButton(onClick = onPlay, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = LocalLevyraStrings.current.play,
                tint = LevyraCyan,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SearchTrackCard(
'''
)

# Searchable Settings hub: the categories remain separate screens, but every area is discoverable
# immediately instead of requiring the user to remember where a preference lives.
replace_once(
    app,
    '''    var languageExpanded by remember { mutableStateOf(false) }
    var activeCategory by rememberSaveable { mutableStateOf<String?>(null) }
''',
    '''    var languageExpanded by remember { mutableStateOf(false) }
    var activeCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var settingsQuery by rememberSaveable { mutableStateOf("") }
'''
)
replace_once(
    app,
    '''        SettingsCategoryMeta("app", categoryTitle(strings.app), "${strings.updates} · ${BuildConfig.VERSION_NAME}", Icons.Rounded.Info, LevyraPink)
    )
    Box(
''',
    '''        SettingsCategoryMeta("app", categoryTitle(strings.app), "${strings.updates} · ${BuildConfig.VERSION_NAME}", Icons.Rounded.Info, LevyraPink)
    )
    val normalizedSettingsQuery = settingsQuery.trim().lowercase(categoryLocale)
    val visibleCategories = if (normalizedSettingsQuery.isBlank()) {
        categories
    } else {
        categories.filter { category ->
            category.id.contains(normalizedSettingsQuery, ignoreCase = true) ||
                category.title.lowercase(categoryLocale).contains(normalizedSettingsQuery) ||
                category.summary.lowercase(categoryLocale).contains(normalizedSettingsQuery)
        }
    }
    Box(
'''
)
replace_once(
    app,
    '''                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    itemsIndexed(categories, key = { _, item -> item.id }) { index, category ->
                        SettingsCategoryCard(
                            meta = category,
                            showDivider = index < categories.lastIndex
                        ) { activeCategory = category.id }
                    }
                    item { SettingsHubFooter() }
''',
    '''                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    item {
                        SettingsSearchField(
                            query = settingsQuery,
                            onQuery = { settingsQuery = it },
                            onClear = { settingsQuery = "" }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    itemsIndexed(visibleCategories, key = { _, item -> item.id }) { index, category ->
                        SettingsCategoryCard(
                            meta = category,
                            showDivider = index < visibleCategories.lastIndex
                        ) {
                            activeCategory = category.id
                            settingsQuery = ""
                        }
                    }
                    item { SettingsHubFooter() }
'''
)
replace_once(
    app,
    '''@Composable
private fun SettingsHubFooter() {
''',
    '''@Composable
private fun SettingsSearchField(
    query: String,
    onQuery: (String) -> Unit,
    onClear: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Surface(
        color = LevyraAdaptiveCardDeep,
        border = BorderStroke(1.dp, LevyraAdaptiveHairline),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = if (query.isBlank()) LevyraMuted else LevyraCyan,
                modifier = Modifier.size(20.dp)
            )
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(
                    color = LevyraText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                cursorBrush = SolidColor(LevyraCyan),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isBlank()) {
                            Text(
                                text = strings.searchPlaceholder,
                                color = LevyraMuted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (query.isNotBlank()) {
                IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = strings.clear,
                        tint = LevyraMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsHubFooter() {
'''
)

# The four actions users need most are always visible. Advanced playback tuning remains collapsible.
replace_once(
    app,
    '''                item {
                    LevyraControlPulseHandle(
''',
    '''                item {
                    PlayerUtilityDock(
                        activeColor = primaryContent,
                        secondaryColor = secondaryContent,
                        lyricsAvailable = state.lyrics.isNotEmpty(),
                        isExporting = state.isOfflineExporting,
                        isDownloaded = track.id in state.downloadedTrackIds,
                        compact = compactPlayer,
                        onQueue = viewModel::openQueue,
                        onLyrics = viewModel::openLyrics,
                        onAddToPlaylist = { playlistTarget = track },
                        onDownload = viewModel::exportCurrentTrack
                    )
                }
                item {
                    LevyraControlPulseHandle(
'''
)
replace_once(
    app,
    '''            PlayerUtilityDock(
                activeColor = primaryContent,
                secondaryColor = secondaryContent,
                lyricsAvailable = state.lyrics.isNotEmpty(),
                isExporting = state.isOfflineExporting,
                isDownloaded = track.id in state.downloadedTrackIds,
                compact = compact,
                onQueue = viewModel::openQueue,
                onLyrics = viewModel::openLyrics,
                onAddToPlaylist = onAddToPlaylist,
                onDownload = viewModel::exportCurrentTrack
            )
            PlayerOptionsRow(
''',
    '''            PlayerOptionsRow(
'''
)

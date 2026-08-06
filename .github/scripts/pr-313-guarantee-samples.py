from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


# 1) Make Shorts discovery tolerant of incomplete NewPipe metadata and return quickly.
repo_path = Path("app/src/main/java/com/luc4n3x/levyra/data/YoutubeShortsRepository.kt")
repo = repo_path.read_text(encoding="utf-8")
repo = replace_once(
    repo,
    '''private const val MAX_SHORT_QUERIES = 8
private const val MAX_SHORT_CHANNELS = 8
private const val INITIAL_SHORT_CHANNELS = 4
private const val FAST_SHORT_TARGET = 6
private const val SHORTS_SEARCH_CONCURRENCY = 4
private const val SHORTS_CHANNEL_CONCURRENCY = 4
private const val SHORTS_PER_QUERY = 4
private const val SHORTS_PER_CHANNEL = 6
private const val MAX_SHORT_DURATION_SECONDS = 180L
private const val SHORTS_SEARCH_TIMEOUT_MS = 6_500L
private const val SHORTS_CHANNEL_TIMEOUT_MS = 8_000L
''',
    '''private const val MAX_SHORT_QUERIES = 4
private const val MAX_SHORT_CHANNELS = 4
private const val SHORTS_SEARCH_CONCURRENCY = 4
private const val SHORTS_CHANNEL_CONCURRENCY = 4
private const val SHORTS_PER_QUERY = 8
private const val SHORTS_PER_CHANNEL = 8
private const val MAX_SHORT_DURATION_SECONDS = 180L
private const val SHORTS_SEARCH_TIMEOUT_MS = 5_500L
private const val SHORTS_CHANNEL_TIMEOUT_MS = 6_500L
''',
    "fast Shorts constants",
)
old_feed = '''        val queries = youtubeShortQueries(seeds, preferredArtists, languageCode).take(MAX_SHORT_QUERIES)
        val directChannelUrls = youtubeShortChannelUrls(seeds, preferredChannelIds)
            .take(MAX_SHORT_CHANNELS)
        val (searchResults, initialChannelResults) = coroutineScope {
            val searchDeferred = async { runSearchDiscovery(queries) }
            val channelDeferred = async {
                runChannelDiscovery(directChannelUrls.take(INITIAL_SHORT_CHANNELS))
            }
            searchDeferred.await() to channelDeferred.await()
        }
        val successfulSearches = searchResults.filterIsInstance<ShortsSourceResult.Success>()
        val successfulInitialChannels = initialChannelResults.filterIsInstance<ShortsSourceResult.Success>()
        val initialTracks = mergeShortTracks(
            channelResults = successfulInitialChannels,
            searchResults = successfulSearches,
            limit = limit
        )
        val target = minOf(limit, FAST_SHORT_TARGET)
        if (initialTracks.size >= target) {
            return@withContext YoutubeShortsFeedResult(
                tracks = initialTracks,
                completedQueries = successfulSearches.size + successfulInitialChannels.size,
                failedQueries = searchResults.count { it is ShortsSourceResult.Failure } +
                    initialChannelResults.count { it is ShortsSourceResult.Failure }
            )
        }

        val discoveredChannelUrls = successfulSearches
            .asSequence()
            .flatMap { result -> result.discovery.channelUrls.asSequence() }
        val enrichmentUrls = (
            directChannelUrls.drop(INITIAL_SHORT_CHANNELS).asSequence() + discoveredChannelUrls
        )
            .distinct()
            .take(MAX_SHORT_CHANNELS - INITIAL_SHORT_CHANNELS)
            .toList()
        val enrichmentResults = runChannelDiscovery(enrichmentUrls)
        val successfulEnrichment = enrichmentResults.filterIsInstance<ShortsSourceResult.Success>()
        val tracks = (
            initialTracks.asSequence() + successfulEnrichment.asSequence()
                .flatMap { result -> result.discovery.tracks.asSequence() }
        )
            .filter(::isYoutubeShortTrack)
            .distinctBy { track -> track.id }
            .take(limit)
            .toList()

        YoutubeShortsFeedResult(
            tracks = tracks,
            completedQueries = successfulSearches.size + successfulInitialChannels.size +
                successfulEnrichment.size,
            failedQueries = searchResults.count { it is ShortsSourceResult.Failure } +
                initialChannelResults.count { it is ShortsSourceResult.Failure } +
                enrichmentResults.count { it is ShortsSourceResult.Failure }
        )
'''
new_feed = '''        val queries = youtubeShortQueries(seeds, preferredArtists, languageCode)
            .take(MAX_SHORT_QUERIES)
        val searchResults = runSearchDiscovery(queries)
        val successfulSearches = searchResults.filterIsInstance<ShortsSourceResult.Success>()
        val searchTracks = mergeShortTracks(
            channelResults = emptyList(),
            searchResults = successfulSearches,
            limit = limit
        )
        if (searchTracks.isNotEmpty()) {
            return@withContext YoutubeShortsFeedResult(
                tracks = searchTracks,
                completedQueries = successfulSearches.size,
                failedQueries = searchResults.count { it is ShortsSourceResult.Failure }
            )
        }

        val directChannelUrls = youtubeShortChannelUrls(seeds, preferredChannelIds)
        val discoveredChannelUrls = successfulSearches
            .asSequence()
            .flatMap { result -> result.discovery.channelUrls.asSequence() }
        val channelUrls = (directChannelUrls.asSequence() + discoveredChannelUrls)
            .distinct()
            .take(MAX_SHORT_CHANNELS)
            .toList()
        val channelResults = runChannelDiscovery(channelUrls)
        val successfulChannels = channelResults.filterIsInstance<ShortsSourceResult.Success>()
        val tracks = mergeShortTracks(
            channelResults = successfulChannels,
            searchResults = successfulSearches,
            limit = limit
        )

        YoutubeShortsFeedResult(
            tracks = tracks,
            completedQueries = successfulSearches.size + successfulChannels.size,
            failedQueries = searchResults.count { it is ShortsSourceResult.Failure } +
                channelResults.count { it is ShortsSourceResult.Failure }
        )
'''
repo = replace_once(repo, old_feed, new_feed, "search-first Shorts feed")
old_tracks = '''                val tracks = streamItems
                    .asSequence()
                    .filter(::isShortCandidate)
                    .mapNotNull(::shortTrack)
                    .distinctBy { track -> track.id }
                    .take(limit)
                    .toList()
'''
new_tracks = '''                val verifiedTracks = streamItems
                    .asSequence()
                    .filter(::isShortCandidate)
                    .mapNotNull(::shortTrack)
                    .distinctBy { track -> track.id }
                    .take(limit)
                    .toList()
                val fallbackTracks = if (verifiedTracks.isEmpty()) {
                    streamItems
                        .asSequence()
                        .filter { item ->
                            isYoutubeShortSearchFallbackCandidate(
                                isShortFormContent = item.isShortFormContent,
                                url = item.url,
                                durationSeconds = item.duration
                            )
                        }
                        .mapNotNull(::shortTrack)
                        .distinctBy { track -> track.id }
                        .take(limit)
                        .toList()
                } else {
                    emptyList()
                }
                val tracks = (verifiedTracks + fallbackTracks)
                    .distinctBy { track -> track.id }
                    .take(limit)
'''
repo = replace_once(repo, old_tracks, new_tracks, "search fallback tracks")
old_candidate = '''internal fun isYoutubeShortCandidate(
    isShortFormContent: Boolean,
    url: String,
    durationSeconds: Long
): Boolean {
    if (durationSeconds !in 1L..MAX_SHORT_DURATION_SECONDS) return false
    return isShortFormContent || url.contains("/shorts/", ignoreCase = true)
}
'''
new_candidate = '''internal fun isYoutubeShortCandidate(
    isShortFormContent: Boolean,
    url: String,
    durationSeconds: Long
): Boolean {
    val verifiedShort = isShortFormContent || url.contains("/shorts/", ignoreCase = true)
    if (!verifiedShort) return false
    return durationSeconds <= 0L || durationSeconds <= MAX_SHORT_DURATION_SECONDS
}

internal fun isYoutubeShortSearchFallbackCandidate(
    isShortFormContent: Boolean,
    url: String,
    durationSeconds: Long
): Boolean {
    if (isYoutubeShortCandidate(isShortFormContent, url, durationSeconds)) return true
    return durationSeconds in 1L..MAX_SHORT_DURATION_SECONDS &&
        YOUTUBE_VIDEO_ID_REGEX.containsMatchIn(url)
}
'''
repo = replace_once(repo, old_candidate, new_candidate, "Short metadata tolerance")
repo = repo.replace('"$artist shorts"', '"$artist #shorts"')
repo = repo.replace('"${track.artist} ${track.title} shorts"', '"${track.artist} ${track.title} #shorts"')
repo = repo.replace(' shorts"', ' #shorts"')
repo_path.write_text(repo, encoding="utf-8")

# 2) Explicit loading/failure state: the Samples screen can never spin forever.
state_path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraUiState.kt")
state = state_path.read_text(encoding="utf-8")
state = replace_once(
    state,
    '''    val exploreTracks: List<Track> = emptyList(),
    val exploreVideos: List<Track> = emptyList(),
    val isExploreLoading: Boolean = false,
''',
    '''    val exploreTracks: List<Track> = emptyList(),
    val exploreVideos: List<Track> = emptyList(),
    val isSamplesLoading: Boolean = false,
    val samplesLoadFailed: Boolean = false,
    val isExploreLoading: Boolean = false,
''',
    "Samples UI state",
)
state_path.write_text(state, encoding="utf-8")

vm_path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
vm = vm_path.read_text(encoding="utf-8")
vm = replace_once(
    vm,
    '''        val snapshot = _state.value
        val languageCode = snapshot.languageCode
        if (musicVideosJob?.isActive == true) return
        if (musicVideosLoadedLanguage == languageCode) return
''',
    '''        val snapshot = _state.value
        val languageCode = snapshot.languageCode
        if (musicVideosJob?.isActive == true) return
        if (snapshot.exploreVideos.isEmpty()) {
            val cached = shortsCache.load(languageCode)
            if (cached.tracks.isNotEmpty()) {
                _state.update { current ->
                    if (current.languageCode == languageCode) {
                        current.copy(
                            exploreVideos = cached.tracks,
                            isSamplesLoading = false,
                            samplesLoadFailed = false
                        )
                    } else {
                        current
                    }
                }
                if (cached.isFresh()) {
                    musicVideosLoadedLanguage = languageCode
                    return
                }
            }
        }
        if (musicVideosLoadedLanguage == languageCode) return
''',
    "load cached Shorts before network",
)
vm = replace_once(
    vm,
    '''        musicVideosJob = viewModelScope.launch {
            val seedSnapshot = _state.value
''',
    '''        _state.update { current ->
            if (current.languageCode == languageCode) {
                current.copy(isSamplesLoading = true, samplesLoadFailed = false)
            } else {
                current
            }
        }
        musicVideosJob = viewModelScope.launch {
            val seedSnapshot = _state.value
''',
    "start Samples loading state",
)
vm = replace_once(
    vm,
    '''            if (feedResult == null || !feedResult.isConclusive) {
                registerShortsFeedFailure(languageCode)
                return@launch
            }
''',
    '''            if (feedResult == null || !feedResult.isConclusive) {
                _state.update { current ->
                    if (current.languageCode == languageCode) {
                        current.copy(isSamplesLoading = false, samplesLoadFailed = true)
                    } else {
                        current
                    }
                }
                registerShortsFeedFailure(languageCode)
                return@launch
            }
''',
    "finish failed Samples load",
)
old_success = '''            if (feedResult.tracks.isNotEmpty()) {
                _state.update { current ->
                    if (current.languageCode == languageCode) {
                        current.copy(exploreVideos = feedResult.tracks)
                    } else {
                        current
                    }
                }
                withContext(Dispatchers.IO) {
                    shortsCache.save(languageCode, feedResult.tracks)
                }
            }
'''
new_success = '''            _state.update { current ->
                if (current.languageCode == languageCode) {
                    val resolvedTracks = feedResult.tracks.ifEmpty { current.exploreVideos }
                    current.copy(
                        exploreVideos = resolvedTracks,
                        isSamplesLoading = false,
                        samplesLoadFailed = resolvedTracks.isEmpty()
                    )
                } else {
                    current
                }
            }
            if (feedResult.tracks.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    shortsCache.save(languageCode, feedResult.tracks)
                }
            }
'''
vm = replace_once(vm, old_success, new_success, "finish successful Samples load")
vm_path.write_text(vm, encoding="utf-8")

# 3) Loading surface reacts to state instead of showing an endless spinner.
screen_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/ExploreSamplesScreen.kt")
screen = screen_path.read_text(encoding="utf-8")
screen = replace_once(
    screen,
    '''    isVideoMode: Boolean,
    favoriteIds: Set<String>,
''',
    '''    isVideoMode: Boolean,
    isLoading: Boolean,
    loadFailed: Boolean,
    favoriteIds: Set<String>,
''',
    "Samples screen state parameters",
)
screen = replace_once(
    screen,
    '''        ExploreSamplesLoadingSurface(
            strings = strings,
            onRetry = onRequestFeed,
            onDismiss = onDismiss
        )
''',
    '''        ExploreSamplesLoadingSurface(
            strings = strings,
            isLoading = isLoading,
            loadFailed = loadFailed,
            onRetry = onRequestFeed,
            onDismiss = onDismiss
        )
''',
    "Samples loading surface call",
)
screen = replace_once(
    screen,
    '''private fun ExploreSamplesLoadingSurface(
    strings: LevyraStrings,
    onRetry: () -> Unit,
''',
    '''private fun ExploreSamplesLoadingSurface(
    strings: LevyraStrings,
    isLoading: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
''',
    "Samples loading surface parameters",
)
screen = replace_once(
    screen,
    '''            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = strings.exploreSamplesSubtitle,
''',
    '''            if (isLoading || !loadFailed) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
            Text(
                text = if (loadFailed) strings.exploreEmpty else strings.exploreSamplesSubtitle,
''',
    "finite Samples loading indicator",
)
screen_path.write_text(screen, encoding="utf-8")

app_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
app = app_path.read_text(encoding="utf-8")
app = replace_once(
    app,
    '''                    isResolving = state.isResolving,
                    isVideoMode = state.isVideoMode,
                    favoriteIds = state.favoriteIds,
''',
    '''                    isResolving = state.isResolving,
                    isVideoMode = state.isVideoMode,
                    isLoading = state.isSamplesLoading,
                    loadFailed = state.samplesLoadFailed,
                    favoriteIds = state.favoriteIds,
''',
    "Samples screen state wiring",
)
app_path.write_text(app, encoding="utf-8")

# 4) Regression tests for unknown-duration verified Shorts and search fallback.
test_path = Path("app/src/test/java/com/luc4n3x/levyra/data/YoutubeShortsRepositoryTest.kt")
test = test_path.read_text(encoding="utf-8")
insert_before = '''    @Test
    fun canonicalShortsUrlsAreAcceptedAsFallback() {
'''
new_tests = '''    @Test
    fun verifiedShortWithUnknownDurationIsAccepted() {
        assertTrue(
            isYoutubeShortCandidate(
                isShortFormContent = true,
                url = "https://www.youtube.com/watch?v=abcdefghijk",
                durationSeconds = 0L
            )
        )
    }

    @Test
    fun shortSearchFallbackAcceptsBoundedVideoResults() {
        assertTrue(
            isYoutubeShortSearchFallbackCandidate(
                isShortFormContent = false,
                url = "https://www.youtube.com/watch?v=abcdefghijk",
                durationSeconds = 90L
            )
        )
        assertFalse(
            isYoutubeShortSearchFallbackCandidate(
                isShortFormContent = false,
                url = "https://www.youtube.com/watch?v=abcdefghijk",
                durationSeconds = 181L
            )
        )
    }

'''
if insert_before not in test:
    raise RuntimeError("test insertion point not found")
test = test.replace(insert_before, new_tests + insert_before, 1)
test_path.write_text(test, encoding="utf-8")

print("Applied guaranteed Samples discovery and finite loading state")

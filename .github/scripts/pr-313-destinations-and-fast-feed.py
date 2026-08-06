from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


# Dedicated Explore destinations: shortcuts no longer scroll the same page.
app_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
app = app_path.read_text(encoding="utf-8")
app = replace_once(
    app,
    '''    val context = LocalContext.current
    val animationsEnabled = LocalAnimationsEnabled.current
    val listState = rememberLazyListState()
    val scrollScope = rememberCoroutineScope()
    var addToPlaylistTarget by remember { mutableStateOf<Track?>(null) }
    var samplesStartIndex by rememberSaveable { mutableStateOf<Int?>(null) }
''',
    '''    val context = LocalContext.current
    val listState = rememberLazyListState()
    var addToPlaylistTarget by remember { mutableStateOf<Track?>(null) }
    var samplesStartIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var exploreDestination by rememberSaveable { mutableStateOf<String?>(null) }
''',
    "Explore destination state",
)
app = replace_once(
    app,
    '''    val onShortcut: (ExploreShortcut) -> Unit = { shortcut ->
        if (shortcut == ExploreShortcut.Samples) {
            samplesStartIndex = 0
            viewModel.beginSamplesPlayback()
            if (samples.isEmpty()) viewModel.refreshSamples()
        } else {
            shortcut.zoneId
                ?.let { zoneId -> zones.firstOrNull { zone -> zone.id == zoneId } }
                ?.takeIf { zone -> zone.id != state.exploreZoneId }
                ?.let(viewModel::selectExploreZone)
            val target = exploreAnchorIndex(rows, shortcut.anchor)
            if (target >= 0) {
                scrollScope.launch {
                    if (animationsEnabled) listState.animateScrollToItem(target) else listState.scrollToItem(target)
                }
            }
        }
    }
''',
    '''    val onShortcut: (ExploreShortcut) -> Unit = { shortcut ->
        when (shortcut) {
            ExploreShortcut.Samples -> {
                exploreDestination = null
                samplesStartIndex = 0
                viewModel.beginSamplesPlayback()
                if (samples.isEmpty()) viewModel.refreshSamples()
            }
            ExploreShortcut.NewReleases -> {
                samplesStartIndex = null
                shortcut.zoneId
                    ?.let { zoneId -> zones.firstOrNull { zone -> zone.id == zoneId } }
                    ?.takeIf { zone -> zone.id != state.exploreZoneId }
                    ?.let(viewModel::selectExploreZone)
                exploreDestination = ExploreNewReleasesDestination
            }
            ExploreShortcut.Moods -> {
                samplesStartIndex = null
                exploreDestination = ExploreMoodsDestination
            }
        }
    }
''',
    "Shortcut destination navigation",
)
app = replace_once(
    app,
    '''                            onClick = { viewModel.selectExploreZone(row.leading) }
''',
    '''                            onClick = {
                                viewModel.selectExploreZone(row.leading)
                                exploreDestination = exploreMoodDestination(row.leading.id)
                            }
''',
    "Leading mood destination",
)
app = replace_once(
    app,
    '''                                onClick = { viewModel.selectExploreZone(trailing) }
''',
    '''                                onClick = {
                                    viewModel.selectExploreZone(trailing)
                                    exploreDestination = exploreMoodDestination(trailing.id)
                                }
''',
    "Trailing mood destination",
)
insert_before = '''        samplesStartIndex?.let { initialPage ->
'''
destination_overlay = '''        when (exploreDestination) {
            ExploreNewReleasesDestination -> ExploreCollectionDestinationScreen(
                title = strings.exploreNewReleases,
                subtitle = selectedZone?.label,
                tracks = freshTracks,
                isLoading = state.isExploreLoading,
                currentTrackId = state.currentTrack?.id,
                isPlaying = state.isPlaying,
                strings = strings,
                onBack = { exploreDestination = null },
                onPlayAll = { freshTracks.firstOrNull()?.let { viewModel.playFrom(freshTracks, it) } },
                onPlayTrack = { track -> viewModel.playFrom(freshTracks, track) }
            )
            ExploreMoodsDestination -> ExploreMoodsDestinationScreen(
                zones = zones,
                strings = strings,
                onBack = { exploreDestination = null },
                onOpenZone = { zone ->
                    viewModel.selectExploreZone(zone)
                    exploreDestination = exploreMoodDestination(zone.id)
                }
            )
            else -> exploreMoodDestinationId(exploreDestination)
                ?.let { zoneId -> zones.firstOrNull { zone -> zone.id == zoneId } }
                ?.let { zone ->
                    ExploreCollectionDestinationScreen(
                        title = zone.label,
                        subtitle = strings.exploreMoods,
                        tracks = freshTracks,
                        isLoading = state.isExploreLoading,
                        currentTrackId = state.currentTrack?.id,
                        isPlaying = state.isPlaying,
                        strings = strings,
                        onBack = { exploreDestination = ExploreMoodsDestination },
                        onPlayAll = { freshTracks.firstOrNull()?.let { viewModel.playFrom(freshTracks, it) } },
                        onPlayTrack = { track -> viewModel.playFrom(freshTracks, track) }
                    )
                }
        }

'''
if insert_before not in app:
    raise RuntimeError("Explore overlay insertion point not found")
app = app.replace(insert_before, destination_overlay + insert_before, 1)
app_path.write_text(app, encoding="utf-8")

# Ensure the standalone destination file has the scoped weight extension imported.
destination_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/ExploreDestinationScreens.kt")
destination = destination_path.read_text(encoding="utf-8")
destination = replace_once(
    destination,
    "import androidx.compose.foundation.layout.width\n",
    "import androidx.compose.foundation.layout.width\nimport androidx.compose.foundation.layout.weight\n",
    "destination weight import",
)
destination_path.write_text(destination, encoding="utf-8")

# Fast first-run Shorts discovery: bounded parallel fast lane, then optional enrichment.
repo_path = Path("app/src/main/java/com/luc4n3x/levyra/data/YoutubeShortsRepository.kt")
repo = repo_path.read_text(encoding="utf-8")
repo = replace_once(
    repo,
    '''private const val MAX_SHORT_QUERIES = 16
private const val MAX_SHORT_CHANNELS = 16
private const val SHORTS_SEARCH_CONCURRENCY = 3
private const val SHORTS_CHANNEL_CONCURRENCY = 3
private const val SHORTS_PER_QUERY = 5
private const val SHORTS_PER_CHANNEL = 8
private const val MAX_SHORT_DURATION_SECONDS = 180L
private const val SHORTS_SEARCH_TIMEOUT_MS = 20_000L
private const val SHORTS_CHANNEL_TIMEOUT_MS = 25_000L
''',
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
    "bounded fast feed constants",
)
repo = replace_once(
    repo,
    '''        val queries = youtubeShortQueries(seeds, preferredArtists, languageCode)
        val searchResults = runSearchDiscovery(queries)
        val successfulSearches = searchResults.filterIsInstance<ShortsSourceResult.Success>()

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

        val channelTracks = successfulChannels
            .asSequence()
            .flatMap { result -> result.discovery.tracks.asSequence() }
        val searchTracks = successfulSearches
            .asSequence()
            .flatMap { result -> result.discovery.tracks.asSequence() }
        val tracks = (channelTracks + searchTracks)
            .filter(::isYoutubeShortTrack)
            .distinctBy { track -> track.id }
            .take(limit)
            .toList()

        YoutubeShortsFeedResult(
            tracks = tracks,
            completedQueries = successfulSearches.size + successfulChannels.size,
            failedQueries = searchResults.count { it is ShortsSourceResult.Failure } +
                channelResults.count { it is ShortsSourceResult.Failure }
        )
''',
    '''        val queries = youtubeShortQueries(seeds, preferredArtists, languageCode).take(MAX_SHORT_QUERIES)
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
''',
    "parallel fast lane feed",
)
insert_before_repo = '''    private suspend fun runSearchDiscovery(queries: List<String>): List<ShortsSourceResult> {
'''
merge_helper = '''    private fun mergeShortTracks(
        channelResults: List<ShortsSourceResult.Success>,
        searchResults: List<ShortsSourceResult.Success>,
        limit: Int
    ): List<Track> {
        val channelTracks = channelResults.asSequence()
            .flatMap { result -> result.discovery.tracks.asSequence() }
        val searchTracks = searchResults.asSequence()
            .flatMap { result -> result.discovery.tracks.asSequence() }
        return (channelTracks + searchTracks)
            .filter(::isYoutubeShortTrack)
            .distinctBy { track -> track.id }
            .take(limit)
            .toList()
    }

'''
if insert_before_repo not in repo:
    raise RuntimeError("repository helper insertion point not found")
repo = repo.replace(insert_before_repo, merge_helper + insert_before_repo, 1)
repo_path.write_text(repo, encoding="utf-8")

print("Applied dedicated Explore destinations and fast Shorts discovery")

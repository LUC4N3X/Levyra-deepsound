from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


# 1) Allow filtered YouTube Music searches through the resilient client.
path = Path("app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicResilienceClient.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''    fun search(query: String, languageCode: String): JSONObject? {
        val clean = query.trim()
        if (clean.length < 2 || apiKey.isBlank()) return null
        return request(
            kind = YoutubeMusicRequestKind.SEARCH,
            languageCode = languageCode,
            browseId = "",
            params = "",
            continuation = "",
            query = clean
        )
    }
''',
    '''    fun search(query: String, languageCode: String, params: String = ""): JSONObject? {
        val clean = query.trim()
        if (clean.length < 2 || apiKey.isBlank()) return null
        return request(
            kind = YoutubeMusicRequestKind.SEARCH,
            languageCode = languageCode,
            browseId = "",
            params = params.trim(),
            continuation = "",
            query = clean
        )
    }
''',
    "filtered resilient search signature",
)
text = replace_once(
    text,
    '''            YoutubeMusicRequestKind.SEARCH -> root.put("query", query)
''',
    '''            YoutubeMusicRequestKind.SEARCH -> {
                root.put("query", query)
                if (params.isNotBlank()) root.put("params", params)
            }
''',
    "filtered search payload",
)
path.write_text(text, encoding="utf-8")


# 2) Add official YT Music releases and music-video Samples discovery.
path = Path("app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicRepository.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''private const val ALBUM_RESULT_RANK_PENALTY = 18

class YoutubeMusicRepository(private val context: Context? = null) {
''',
    '''private const val ALBUM_RESULT_RANK_PENALTY = 18
internal const val YOUTUBE_MUSIC_SAMPLES_SOURCE = "YouTube Music Samples"
internal const val YOUTUBE_MUSIC_VIDEO_SEARCH_PARAMS = "EgWKAQIQAWoMEA4QChADEAQQCRAF"
private const val YOUTUBE_MUSIC_SAMPLE_QUERY_LIMIT = 8
private const val YOUTUBE_MUSIC_SAMPLE_QUERY_CONCURRENCY = 4
private const val YOUTUBE_MUSIC_SAMPLE_RESULTS_PER_QUERY = 8

internal fun youtubeMusicSampleQueries(
    seeds: List<Track>,
    preferredArtists: List<String>,
    languageCode: String
): List<String> {
    val artists = (preferredArtists.asSequence() + seeds.asSequence().map { it.artist })
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy { it.lowercase(Locale.ROOT) }
        .take(5)
        .map { "$it official music video" }
        .toList()
    val songs = seeds.asSequence()
        .filter { it.title.isNotBlank() && it.artist.isNotBlank() }
        .distinctBy { "${it.artist.lowercase(Locale.ROOT)}|${it.title.lowercase(Locale.ROOT)}" }
        .take(3)
        .map { "${it.artist} ${it.title} music video" }
        .toList()
    val localized = when (LevyraLanguageCatalog.normalize(languageCode)) {
        "it" -> listOf("nuovi video musicali", "video musicali italiani", "hit del momento video")
        "es" -> listOf("nuevos videos musicales", "videos musicales latinos", "éxitos del momento video")
        "fr" -> listOf("nouveaux clips musicaux", "clips musicaux français", "tubes du moment clip")
        "de" -> listOf("neue musikvideos", "deutsche musikvideos", "aktuelle hits musikvideo")
        "pt" -> listOf("novos videoclipes", "videoclipes brasileiros", "sucessos do momento vídeo")
        "ja" -> listOf("新着 ミュージックビデオ", "人気曲 公式MV", "J-POP ミュージックビデオ")
        "ko" -> listOf("신곡 뮤직비디오", "인기곡 공식 뮤직비디오", "K-POP 뮤직비디오")
        else -> listOf("new music videos", "official music videos", "songs right now music video")
    }
    return (artists + songs + localized)
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy { it.lowercase(Locale.ROOT) }
        .take(YOUTUBE_MUSIC_SAMPLE_QUERY_LIMIT)
}

internal fun youtubeMusicSamplePreviewStartMs(track: Track): Long {
    if (!track.source.equals(YOUTUBE_MUSIC_SAMPLES_SOURCE, ignoreCase = true)) return 0L
    val duration = track.durationMs
    if (duration <= 75_000L) return 0L
    val latestSafeStart = (duration - 45_000L).coerceAtLeast(0L)
    return (duration / 3L).coerceIn(0L, latestSafeStart)
}

class YoutubeMusicRepository(private val context: Context? = null) {
''',
    "YT Music Samples constants and policy",
)
text = replace_once(
    text,
    '''    private fun searchInnerTubeRaw(query: String, languageCode: String): JSONObject? {
    return resilienceClient.search(query, languageCode)
}
''',
    '''    private fun searchInnerTubeRaw(
        query: String,
        languageCode: String,
        params: String = ""
    ): JSONObject? {
        return resilienceClient.search(query, languageCode, params)
    }
''',
    "filtered raw search",
)
text = replace_once(
    text,
    '''    suspend fun newMusicVideos(
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        limit: Int = 12
    ): List<Track> = withContext(Dispatchers.IO) {
''',
    '''    suspend fun newReleases(
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        limit: Int = 40
    ): List<AlbumHit> = withContext(Dispatchers.IO) {
        val boundedLimit = limit.coerceIn(1, 80)
        val root = requestMusicBrowseRoot(languageCode, "FEmusic_new_releases")
            ?: return@withContext emptyList()
        val releases = LinkedHashMap<String, AlbumHit>()
        parseExplore(root).newReleases.forEach { release ->
            releases.putIfAbsent(albumRecommendationDeduplicationKey(release), release)
        }
        val twoRows = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicTwoRowItemRenderer", twoRows)
        twoRows.forEach { renderer ->
            parseAlbumFromExploreItem(JSONObject().put("musicTwoRowItemRenderer", renderer))?.let { release ->
                releases.putIfAbsent(albumRecommendationDeduplicationKey(release), release)
            }
        }
        val responsiveRows = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicResponsiveListItemRenderer", responsiveRows)
        responsiveRows.forEach { renderer ->
            parseAlbumFromExploreItem(JSONObject().put("musicResponsiveListItemRenderer", renderer))?.let { release ->
                releases.putIfAbsent(albumRecommendationDeduplicationKey(release), release)
            }
        }
        releases.values.asSequence()
            .filter { release ->
                release.browseId.startsWith("MPRE") &&
                    isPlausibleYoutubeMusicAlbumTitle(release.title) &&
                    release.thumbnailUrl.isNotBlank()
            }
            .take(boundedLimit)
            .toList()
    }

    suspend fun musicSamples(
        seeds: List<Track>,
        preferredArtists: List<String>,
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        limit: Int = 24
    ): List<Track> = withContext(Dispatchers.IO) {
        val boundedLimit = limit.coerceIn(1, 40)
        val nativeVideos = runCatching { explore(languageCode).newVideos }
            .getOrDefault(emptyList())
            .mapNotNull(::asYoutubeMusicSample)
        if (nativeVideos.size >= boundedLimit) {
            return@withContext nativeVideos.distinctBy { it.id }.take(boundedLimit)
        }

        val queries = youtubeMusicSampleQueries(seeds, preferredArtists, languageCode)
        val limiter = Semaphore(YOUTUBE_MUSIC_SAMPLE_QUERY_CONCURRENCY)
        val searched = coroutineScope {
            queries.map { query ->
                async {
                    limiter.withPermit {
                        runCatching {
                            searchMusicVideoSamples(
                                query = query,
                                languageCode = languageCode,
                                limit = YOUTUBE_MUSIC_SAMPLE_RESULTS_PER_QUERY
                            )
                        }.getOrDefault(emptyList())
                    }
                }
            }.awaitAll().flatten()
        }
        (nativeVideos + searched)
            .distinctBy { it.id }
            .take(boundedLimit)
    }

    private fun searchMusicVideoSamples(
        query: String,
        languageCode: String,
        limit: Int
    ): List<Track> {
        val root = searchInnerTubeRaw(query, languageCode, YOUTUBE_MUSIC_VIDEO_SEARCH_PARAMS)
            ?: return emptyList()
        val renderers = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicResponsiveListItemRenderer", renderers)
        return renderers.asSequence()
            .mapNotNull { renderer -> parseMusicRenderer(renderer, query) }
            .filter(::isVisualYoutubeMusicVideo)
            .mapNotNull(::asYoutubeMusicSample)
            .distinctBy { it.id }
            .take(limit)
            .toList()
    }

    private fun isVisualYoutubeMusicVideo(track: Track): Boolean {
        if (track.id.length != 11 || track.videoUrl.isBlank()) return false
        if (track.title.isBlank() || track.artist.isBlank()) return false
        val type = track.videoType.uppercase(Locale.ROOT)
        if (type.contains("ATV") || type.contains("PODCAST")) return false
        return type.contains("OMV") || type.contains("UGC") || type.contains("MUSIC_VIDEO")
    }

    private fun asYoutubeMusicSample(track: Track): Track? {
        if (track.id.length != 11 || track.title.isBlank() || track.artist.isBlank()) return null
        val type = track.videoType.uppercase(Locale.ROOT)
        if (type.contains("ATV") || type.contains("PODCAST")) return null
        if (track.videoUrl.isBlank() && track.id.isBlank()) return null
        return track.copy(
            videoUrl = track.videoUrl.ifBlank { "https://www.youtube.com/watch?v=${track.id}" },
            source = YOUTUBE_MUSIC_SAMPLES_SOURCE,
            moodTags = track.moodTags + setOf("samples", "music-video"),
            counterpartVideoId = track.counterpartVideoId.ifBlank { track.id },
            videoType = track.videoType.ifBlank { "MUSIC_VIDEO_TYPE_OMV" }
        )
    }

    suspend fun newMusicVideos(
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        limit: Int = 12
    ): List<Track> = withContext(Dispatchers.IO) {
''',
    "official releases and YT Music Samples methods",
)
path.write_text(text, encoding="utf-8")


# 3) Treat official YT Music music-video previews as valid Samples.
path = Path("app/src/main/java/com/luc4n3x/levyra/data/YoutubeShortsRepository.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''internal fun isYoutubeShortTrack(track: Track): Boolean {
    return track.source.equals(YOUTUBE_SHORTS_SOURCE, ignoreCase = true) ||
        track.videoType.equals("SHORTS", ignoreCase = true) ||
        track.videoUrl.contains("/shorts/", ignoreCase = true)
}
''',
    '''internal fun isYoutubeShortTrack(track: Track): Boolean {
    return track.source.equals(YOUTUBE_SHORTS_SOURCE, ignoreCase = true) ||
        track.source.equals(YOUTUBE_MUSIC_SAMPLES_SOURCE, ignoreCase = true) ||
        track.videoType.equals("SHORTS", ignoreCase = true) ||
        track.videoUrl.contains("/shorts/", ignoreCase = true)
}
''',
    "YT Music Samples validity",
)
path.write_text(text, encoding="utf-8")


# 4) State for official releases.
path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraUiState.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''    val exploreZoneId: String? = null,
    val exploreTracks: List<Track> = emptyList(),
    val exploreVideos: List<Track> = emptyList(),
    val isSamplesLoading: Boolean = false,
''',
    '''    val exploreZoneId: String? = null,
    val exploreTracks: List<Track> = emptyList(),
    val exploreNewReleases: List<AlbumHit> = emptyList(),
    val exploreVideos: List<Track> = emptyList(),
    val isNewReleasesLoading: Boolean = false,
    val newReleasesLoadFailed: Boolean = false,
    val isSamplesLoading: Boolean = false,
''',
    "official release state",
)
path.write_text(text, encoding="utf-8")


# 5) Explore ViewModel surface and projection.
path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraScreenViewModels.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''    fun ensureExplore(strings: LevyraStrings) = root.ensureExplore(strings)
    fun refreshSamples() = root.refreshExploreSamples()
''',
    '''    fun ensureExplore(strings: LevyraStrings) = root.ensureExplore(strings)
    fun openAlbum(album: AlbumHit) = root.openAlbum(album)
    fun refreshSamples() = root.refreshExploreSamples()
''',
    "Explore album navigation",
)
text = replace_once(
    text,
    '''private data class ExploreProjection(
    val currentTrack: Track?,
    val exploreTracks: List<Track>,
    val exploreVideos: List<Track>,
    val exploreZoneId: String?,
    val favoriteIds: Set<String>,
    val isExploreLoading: Boolean,
    val isPlaying: Boolean,
    val isSamplesOpen: Boolean
)

private fun exploreProjection(state: LevyraUiState): ExploreProjection = ExploreProjection(
    currentTrack = state.currentTrack,
    exploreTracks = state.exploreTracks,
    exploreVideos = state.exploreVideos,
    exploreZoneId = state.exploreZoneId,
    favoriteIds = state.favoriteIds,
    isExploreLoading = state.isExploreLoading,
    isPlaying = state.isPlaying,
    isSamplesOpen = state.isSamplesOpen
)
''',
    '''private data class ExploreProjection(
    val currentTrack: Track?,
    val exploreTracks: List<Track>,
    val exploreNewReleases: List<AlbumHit>,
    val exploreVideos: List<Track>,
    val exploreZoneId: String?,
    val favoriteIds: Set<String>,
    val isExploreLoading: Boolean,
    val isNewReleasesLoading: Boolean,
    val newReleasesLoadFailed: Boolean,
    val isPlaying: Boolean,
    val isSamplesOpen: Boolean
)

private fun exploreProjection(state: LevyraUiState): ExploreProjection = ExploreProjection(
    currentTrack = state.currentTrack,
    exploreTracks = state.exploreTracks,
    exploreNewReleases = state.exploreNewReleases,
    exploreVideos = state.exploreVideos,
    exploreZoneId = state.exploreZoneId,
    favoriteIds = state.favoriteIds,
    isExploreLoading = state.isExploreLoading,
    isNewReleasesLoading = state.isNewReleasesLoading,
    newReleasesLoadFailed = state.newReleasesLoadFailed,
    isPlaying = state.isPlaying,
    isSamplesOpen = state.isSamplesOpen
)
''',
    "Explore release projection",
)
path.write_text(text, encoding="utf-8")


# 6) Load YT Music Samples first, official releases separately, and seek into preview clips.
path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''import com.luc4n3x.levyra.data.youtubeShortsRetryDelayMs
''',
    '''import com.luc4n3x.levyra.data.youtubeShortsRetryDelayMs
import com.luc4n3x.levyra.data.youtubeMusicSamplePreviewStartMs
''',
    "sample preview import",
)
text = replace_once(
    text,
    '''        startResolve(selected)
    }

    fun endSamplesPlayback() {
''',
    '''        pendingSeekMs = youtubeMusicSamplePreviewStartMs(selected)
        startResolve(selected)
    }

    fun endSamplesPlayback() {
''',
    "sample preview seek",
)
text = replace_once(
    text,
    '''    private var musicVideosJob: Job? = null
    private var exploreJob: Job? = null
''',
    '''    private var musicVideosJob: Job? = null
    private var newReleasesLoadedLanguage = ""
    private var newReleasesJob: Job? = null
    private var exploreJob: Job? = null
''',
    "new releases jobs",
)
old_feed = '''            val feedResult = try {
                shortsRepository.feed(
                    seeds = seeds,
                    languageCode = languageCode,
                    preferredArtists = preferredArtists,
                    preferredChannelIds = preferredChannelIds,
                    limit = EXPLORE_SHORTS_FEED_LIMIT
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Timber.w(error, "Shorts feed failed for %s", languageCode)
                null
            }
            if (_state.value.languageCode != languageCode) return@launch

            if (feedResult == null || !feedResult.isConclusive) {
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

            musicVideosLoadedLanguage = languageCode
            musicVideosRetryLanguage = ""
            musicVideosRetryAfterMs = 0L
            musicVideosFailureCount = 0
            _state.update { current ->
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
new_feed = '''            val youtubeMusicSamples = try {
                repository.musicSamples(
                    seeds = seeds,
                    preferredArtists = preferredArtists,
                    languageCode = languageCode,
                    limit = EXPLORE_SHORTS_FEED_LIMIT
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Timber.w(error, "YouTube Music Samples feed failed for %s", languageCode)
                emptyList()
            }
            val fallbackFeed = if (youtubeMusicSamples.isEmpty()) {
                try {
                    shortsRepository.feed(
                        seeds = seeds,
                        languageCode = languageCode,
                        preferredArtists = preferredArtists,
                        preferredChannelIds = preferredChannelIds,
                        limit = EXPLORE_SHORTS_FEED_LIMIT
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    Timber.w(error, "NewPipe Shorts fallback failed for %s", languageCode)
                    null
                }
            } else {
                null
            }
            if (_state.value.languageCode != languageCode) return@launch

            val resolvedFeedTracks = youtubeMusicSamples.ifEmpty { fallbackFeed?.tracks.orEmpty() }
            val feedIsConclusive = youtubeMusicSamples.isNotEmpty() || fallbackFeed?.isConclusive == true
            if (!feedIsConclusive || resolvedFeedTracks.isEmpty()) {
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

            musicVideosLoadedLanguage = languageCode
            musicVideosRetryLanguage = ""
            musicVideosRetryAfterMs = 0L
            musicVideosFailureCount = 0
            _state.update { current ->
                if (current.languageCode == languageCode) {
                    current.copy(
                        exploreVideos = resolvedFeedTracks,
                        isSamplesLoading = false,
                        samplesLoadFailed = false
                    )
                } else {
                    current
                }
            }
            withContext(Dispatchers.IO) {
                shortsCache.save(languageCode, resolvedFeedTracks)
            }
'''
text = replace_once(text, old_feed, new_feed, "YT Music-first Samples loading")
text = replace_once(
    text,
    '''    fun ensureExplore(strings: LevyraStrings) {
        if (_state.value.exploreZoneId == null) {
            selectExploreZone(ExploreCatalog.getZones(strings).first())
        }
        ensureMusicVideosLoaded()
    }
''',
    '''    private fun ensureOfficialNewReleasesLoaded(force: Boolean = false) {
        val languageCode = _state.value.languageCode
        if (newReleasesJob?.isActive == true) return
        if (!force && newReleasesLoadedLanguage == languageCode && _state.value.exploreNewReleases.isNotEmpty()) return
        if (newReleasesLoadedLanguage != languageCode) {
            _state.update { current ->
                current.copy(exploreNewReleases = emptyList(), newReleasesLoadFailed = false)
            }
        }
        _state.update { current -> current.copy(isNewReleasesLoading = true, newReleasesLoadFailed = false) }
        newReleasesJob = viewModelScope.launch {
            val releases = try {
                repository.newReleases(languageCode = languageCode, limit = 48)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Timber.w(error, "Official YouTube Music releases failed for %s", languageCode)
                emptyList()
            }
            if (_state.value.languageCode != languageCode) return@launch
            if (releases.isNotEmpty()) newReleasesLoadedLanguage = languageCode
            _state.update { current ->
                current.copy(
                    exploreNewReleases = releases,
                    isNewReleasesLoading = false,
                    newReleasesLoadFailed = releases.isEmpty()
                )
            }
        }
    }

    fun ensureExplore(strings: LevyraStrings) {
        if (_state.value.exploreZoneId == null) {
            selectExploreZone(ExploreCatalog.getZones(strings).first())
        }
        ensureOfficialNewReleasesLoaded()
        ensureMusicVideosLoaded()
    }
''',
    "official release loading",
)
path.write_text(text, encoding="utf-8")


# 7) Dedicated official-release destination UI.
path = Path("app/src/main/java/com/luc4n3x/levyra/ui/ExploreDestinationScreens.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.Track
''',
    '''import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.Track
''',
    "AlbumHit destination import",
)
insert_before = '''@Composable
internal fun ExploreMoodsDestinationScreen(
'''
release_screen = '''@Composable
internal fun ExploreNewReleasesDestinationScreen(
    releases: List<AlbumHit>,
    isLoading: Boolean,
    strings: LevyraStrings,
    onBack: () -> Unit,
    onOpenRelease: (AlbumHit) -> Unit
) {
    BackHandler(onBack = onBack)
    ExploreDestinationSurface(
        title = strings.exploreNewReleases,
        subtitle = "YouTube Music",
        strings = strings,
        onBack = onBack
    ) { contentPadding ->
        when {
            isLoading && releases.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = LevyraCyan, strokeWidth = 3.dp)
            }
            releases.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.exploreEmpty,
                    color = LevyraMuted,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = contentPadding.calculateTopPadding() + 10.dp,
                    bottom = 130.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = releases,
                    key = { release -> "ytm-release-${release.browseId.ifBlank { release.title + release.artist }}" }
                ) { release ->
                    ExploreDestinationReleaseRow(
                        release = release,
                        onClick = { onOpenRelease(release) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreDestinationReleaseRow(
    release: AlbumHit,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 78.dp)
            .clip(shape)
            .background(LevyraPanel.copy(alpha = 0.72f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = release.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(62.dp).clip(RoundedCornerShape(9.dp))
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = release.title,
                color = LevyraText,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOf(release.artist, release.year)
                    .filter(String::isNotBlank)
                    .joinToString(" • "),
                color = LevyraMuted,
                fontSize = 12.5.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = null,
            tint = LevyraText,
            modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 180f }
        )
    }
}

@Composable
internal fun ExploreMoodsDestinationScreen(
'''
text = replace_once(text, insert_before, release_screen, "official release destination screen")
text = replace_once(
    text,
    '''import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
''',
    '''import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
''',
    "release row chevron transform import",
)
path.write_text(text, encoding="utf-8")


# 8) Route the shortcut to official releases rather than flattened tracks.
path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''            ExploreShortcut.NewReleases -> {
                samplesStartIndex = null
                shortcut.zoneId
                    ?.let { zoneId -> zones.firstOrNull { zone -> zone.id == zoneId } }
                    ?.takeIf { zone -> zone.id != state.exploreZoneId }
                    ?.let(viewModel::selectExploreZone)
                exploreDestination = ExploreNewReleasesDestination
            }
''',
    '''            ExploreShortcut.NewReleases -> {
                samplesStartIndex = null
                exploreDestination = ExploreNewReleasesDestination
            }
''',
    "new releases shortcut routing",
)
text = replace_once(
    text,
    '''            ExploreNewReleasesDestination -> ExploreCollectionDestinationScreen(
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
''',
    '''            ExploreNewReleasesDestination -> ExploreNewReleasesDestinationScreen(
                releases = state.exploreNewReleases,
                isLoading = state.isNewReleasesLoading,
                strings = strings,
                onBack = { exploreDestination = null },
                onOpenRelease = viewModel::openAlbum
            )
''',
    "official releases destination wiring",
)
path.write_text(text, encoding="utf-8")


# 9) Regression tests for YT Music music-only Samples policy.
test_path = Path("app/src/test/java/com/luc4n3x/levyra/data/YoutubeMusicSamplesPolicyTest.kt")
test_path.write_text('''package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicSamplesPolicyTest {
    @Test
    fun queriesPreferListeningSignalsAndStayMusicVideoSpecific() {
        val queries = youtubeMusicSampleQueries(
            seeds = listOf(track(title = "Brano", artist = "Artista ascoltato")),
            preferredArtists = listOf("Artista seguito"),
            languageCode = "it"
        )

        assertEquals("Artista seguito official music video", queries.first())
        assertTrue(queries.any { it.contains("Brano music video") })
        assertTrue(queries.contains("nuovi video musicali"))
        assertFalse(queries.any { it.contains("podcast", ignoreCase = true) })
    }

    @Test
    fun previewStartsInsideLongMusicVideoButNotOrdinaryPlayback() {
        val sample = track(
            durationMs = 240_000L,
            source = YOUTUBE_MUSIC_SAMPLES_SOURCE
        )
        val ordinary = sample.copy(source = "YouTube Music")

        assertEquals(80_000L, youtubeMusicSamplePreviewStartMs(sample))
        assertEquals(0L, youtubeMusicSamplePreviewStartMs(ordinary))
    }

    @Test
    fun officialMusicSampleIsAcceptedBySharedSampleGate() {
        val sample = track(
            source = YOUTUBE_MUSIC_SAMPLES_SOURCE,
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )

        assertTrue(isYoutubeShortTrack(sample))
    }

    private fun track(
        title: String = "Title",
        artist: String = "Artist",
        durationMs: Long = 180_000L,
        source: String = "YouTube Music",
        videoType: String = "MUSIC_VIDEO_TYPE_OMV"
    ): Track = Track(
        id = "abcdefghijk",
        title = title,
        artist = artist,
        album = "Album",
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=abcdefghijk",
        thumbnailUrl = "https://levyra.test/sample.jpg",
        largeThumbnailUrl = "https://levyra.test/sample-large.jpg",
        source = source,
        moodTags = setOf("music"),
        energy = 60,
        vocal = 60,
        replayScore = 80,
        cacheScore = 70,
        accentStart = 0xFF00E5FF.toInt(),
        accentEnd = 0xFF2979FF.toInt(),
        videoType = videoType
    )
}
''', encoding="utf-8")

path = Path("app/src/test/java/com/luc4n3x/levyra/viewmodel/SamplesPlaybackSelectionTest.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''import com.luc4n3x.levyra.data.YOUTUBE_SHORTS_SOURCE
''',
    '''import com.luc4n3x.levyra.data.YOUTUBE_MUSIC_SAMPLES_SOURCE
import com.luc4n3x.levyra.data.YOUTUBE_SHORTS_SOURCE
''',
    "YT Music sample test import",
)
text = replace_once(
    text,
    '''    private fun track(
''',
    '''    @Test
    fun officialYoutubeMusicVideoIsAcceptedAsSample() {
        val sample = track(
            id = "musicvideo1",
            source = YOUTUBE_MUSIC_SAMPLES_SOURCE,
            videoUrl = "https://www.youtube.com/watch?v=musicvideo1",
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )

        assertEquals(sample, selectYoutubeShortSample(listOf(sample), sample))
    }

    private fun track(
''',
    "YT Music sample selection regression",
)
path.write_text(text, encoding="utf-8")

print("Applied official YouTube Music releases and music-only Samples pipeline")

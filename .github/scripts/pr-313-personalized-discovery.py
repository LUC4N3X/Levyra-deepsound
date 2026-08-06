from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


# YouTube Music discovery policies.
path = Path("app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicRepository.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    "import java.util.Locale\n",
    "import java.util.Calendar\nimport java.util.Locale\n",
    "Calendar import",
)
old_policy = '''internal const val YOUTUBE_MUSIC_SAMPLES_SOURCE = "YouTube Music Samples"
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
'''
new_policy = '''internal const val YOUTUBE_MUSIC_SAMPLES_SOURCE = "YouTube Music Samples"
internal const val YOUTUBE_MUSIC_VIDEO_SEARCH_PARAMS = "EgWKAQIQAWoMEA4QChADEAQQCRAF"
private const val YOUTUBE_MUSIC_SAMPLE_QUERY_LIMIT = 8
private const val YOUTUBE_MUSIC_SAMPLE_ARTIST_QUERY_LIMIT = 3
private const val YOUTUBE_MUSIC_SAMPLE_SONG_QUERY_LIMIT = 2
private const val YOUTUBE_MUSIC_SAMPLE_LOCALIZED_QUERY_LIMIT = 3
private const val YOUTUBE_MUSIC_SAMPLE_QUERY_CONCURRENCY = 4
private const val YOUTUBE_MUSIC_SAMPLE_RESULTS_PER_QUERY = 8
private const val YOUTUBE_MUSIC_NEW_RELEASE_FALLBACK_LIMIT = 4

private val GENERIC_NEW_RELEASE_TITLE = Regex(
    """\\b(?:best\\s+of|greatest\\s+hits?|hits?\\s+vol(?:ume)?|vol(?:ume)?\\.?\\s*\\d+|collection|compilation|karaoke|type\\s+beat|tribute|instrumental\\s+versions?)\\b""",
    RegexOption.IGNORE_CASE
)
private val GENERIC_NEW_RELEASE_ARTIST = Regex(
    """^(?:various\\s+artists?|unknown\\s+artist|youtube\\s+music|topic)$""",
    RegexOption.IGNORE_CASE
)

private data class RankedYoutubeMusicRelease(
    val release: AlbumHit,
    val score: Int,
    val bucket: Int
)

internal fun youtubeMusicSampleQueries(
    seeds: List<Track>,
    preferredArtists: List<String>,
    languageCode: String
): List<String> {
    val artists = (preferredArtists.asSequence() + seeds.asSequence().map { it.artist })
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy { it.lowercase(Locale.ROOT) }
        .take(YOUTUBE_MUSIC_SAMPLE_ARTIST_QUERY_LIMIT)
        .map { "$it official music video" }
        .toList()
    val songs = seeds.asSequence()
        .filter { it.title.isNotBlank() && it.artist.isNotBlank() }
        .distinctBy { "${it.artist.lowercase(Locale.ROOT)}|${it.title.lowercase(Locale.ROOT)}" }
        .take(YOUTUBE_MUSIC_SAMPLE_SONG_QUERY_LIMIT)
        .map { "${it.artist} ${it.title} music video" }
        .toList()
    val localized = when (LevyraLanguageCatalog.normalize(languageCode)) {
        "it" -> listOf("nuovi video musicali italiani", "hit italiane del momento video", "video musicali popolari in Italia")
        "es" -> listOf("nuevos videos musicales españoles", "éxitos latinos del momento video", "videos musicales populares en España")
        "fr" -> listOf("nouveaux clips musicaux français", "tubes français du moment clip", "clips populaires en France")
        "de" -> listOf("neue deutsche musikvideos", "aktuelle deutsche hits musikvideo", "beliebte musikvideos in Deutschland")
        "pt" -> listOf("novos videoclipes brasileiros", "sucessos brasileiros do momento vídeo", "videoclipes populares no Brasil")
        "ja" -> listOf("日本 新着 ミュージックビデオ", "日本 人気曲 公式MV", "J-POP 話題 ミュージックビデオ")
        "ko" -> listOf("한국 신곡 뮤직비디오", "한국 인기곡 공식 뮤직비디오", "K-POP 인기 뮤직비디오")
        else -> listOf("new music videos in my country", "songs popular in my region music video", "local music video hits")
    }.take(YOUTUBE_MUSIC_SAMPLE_LOCALIZED_QUERY_LIMIT)
    val personalized = artists + songs
    return buildList {
        repeat(maxOf(personalized.size, localized.size)) { index ->
            personalized.getOrNull(index)?.let { add(it) }
            localized.getOrNull(index)?.let { add(it) }
        }
    }
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy { it.lowercase(Locale.ROOT) }
        .take(YOUTUBE_MUSIC_SAMPLE_QUERY_LIMIT)
}

internal fun interleaveYoutubeMusicSampleResults(
    groups: List<List<Track>>,
    limit: Int
): List<Track> {
    if (limit <= 0 || groups.isEmpty()) return emptyList()
    val result = LinkedHashMap<String, Track>()
    var row = 0
    while (result.size < limit) {
        var found = false
        groups.forEach { group ->
            val track = group.getOrNull(row) ?: return@forEach
            found = true
            val key = track.id.ifBlank { "${track.title.lowercase(Locale.ROOT)}|${track.artist.lowercase(Locale.ROOT)}" }
            result.putIfAbsent(key, track)
        }
        if (!found) break
        row++
    }
    return result.values.take(limit)
}

internal fun rankYoutubeMusicNewReleases(
    releases: List<AlbumHit>,
    preferredArtists: List<String>,
    popularArtists: List<String>,
    currentYear: Int,
    limit: Int
): List<AlbumHit> {
    if (limit <= 0) return emptyList()
    val preferred = preferredArtists.map(::normalizedReleaseArtist).filter(String::isNotBlank).distinct()
    val popular = popularArtists.map(::normalizedReleaseArtist).filter(String::isNotBlank).distinct()
    val ranked = releases
        .distinctBy(::albumRecommendationDeduplicationKey)
        .mapNotNull { release ->
            val title = release.title.trim()
            val artist = release.artist.trim()
            if (!release.browseId.startsWith("MPRE") || title.length < 2 || artist.length < 2) return@mapNotNull null
            if (release.thumbnailUrl.isBlank() || GENERIC_NEW_RELEASE_TITLE.containsMatchIn(title)) return@mapNotNull null
            if (GENERIC_NEW_RELEASE_ARTIST.matches(artist) || release.releaseType == ReleaseType.Compilation) return@mapNotNull null

            val artistKey = normalizedReleaseArtist(artist)
            val preferredIndex = matchingArtistSignalIndex(artistKey, preferred)
            val popularIndex = matchingArtistSignalIndex(artistKey, popular)
            val matchedPreference = preferredIndex >= 0
            val matchedPopular = popularIndex >= 0
            val releaseYear = release.year.toIntOrNull()
            val recent = releaseYear == currentYear || releaseYear == currentYear - 1
            val identifiedArtist = release.artistBrowseId.isNotBlank() || matchedPreference || matchedPopular
            if (!identifiedArtist) return@mapNotNull null
            if (releaseYear != null && !recent) return@mapNotNull null
            if (release.releaseType == ReleaseType.Unknown && !matchedPreference && !matchedPopular) return@mapNotNull null

            var score = 0
            if (matchedPreference) score += 8_000 - preferredIndex * 120
            if (matchedPopular) score += 4_000 - popularIndex * 70
            if (releaseYear == currentYear) score += 900
            else if (releaseYear == currentYear - 1) score += 320
            if (release.artistBrowseId.isNotBlank()) score += 240
            score += when (release.releaseType) {
                ReleaseType.Album -> 180
                ReleaseType.Ep -> 160
                ReleaseType.Single -> 140
                ReleaseType.Unknown -> 0
                ReleaseType.Compilation -> -10_000
            }
            val bucket = when {
                matchedPreference -> 0
                matchedPopular -> 1
                recent -> 2
                else -> 3
            }
            RankedYoutubeMusicRelease(release, score, bucket)
        }
        .sortedWith(compareBy<RankedYoutubeMusicRelease> { it.bucket }.thenByDescending { it.score })

    val personalized = ranked.filter { it.bucket == 0 }
    val popularMatches = ranked.filter { it.bucket == 1 }
    val recentOfficial = ranked.filter { it.bucket == 2 }
    val fallback = ranked.filter { it.bucket == 3 }.take(YOUTUBE_MUSIC_NEW_RELEASE_FALLBACK_LIMIT)
    return (personalized + popularMatches + recentOfficial + fallback)
        .distinctBy { albumRecommendationDeduplicationKey(it.release) }
        .take(limit)
        .map { it.release }
}

private fun normalizedReleaseArtist(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .lowercase(Locale.ROOT)
    .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

private fun matchingArtistSignalIndex(artist: String, signals: List<String>): Int {
    if (artist.length < 2) return -1
    return signals.indexOfFirst { signal ->
        signal.length >= 2 && (artist == signal || artist.contains(signal) || signal.contains(artist))
    }
}
'''
text = replace_once(text, old_policy, new_policy, "discovery policy block")

old_methods = '''    suspend fun newReleases(
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
'''
new_methods = '''    suspend fun newReleases(
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        limit: Int = 40,
        preferredArtists: List<String> = emptyList()
    ): List<AlbumHit> = withContext(Dispatchers.IO) {
        val boundedLimit = limit.coerceIn(1, 80)
        val nativeExplore = runCatching { explore(languageCode) }.getOrDefault(YoutubeMusicExplore())
        val popularArtists = (nativeExplore.topSongs + nativeExplore.trending + nativeExplore.newVideos)
            .map { it.artist.trim() }
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(24)
        val root = requestMusicBrowseRoot(languageCode, "FEmusic_new_releases")
            ?: return@withContext emptyList()
        val releases = LinkedHashMap<String, AlbumHit>()
        fun addRelease(release: AlbumHit) {
            if (isPlausibleYoutubeMusicAlbumTitle(release.title)) {
                releases.putIfAbsent(albumRecommendationDeduplicationKey(release), release)
            }
        }
        parseExplore(root).newReleases.forEach(::addRelease)

        val carousels = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicCarouselShelfRenderer", carousels)
        carousels.forEach { shelf ->
            val contents = shelf.optJSONArray("contents") ?: JSONArray()
            for (index in 0 until contents.length()) {
                parseAlbumFromExploreItem(contents.optJSONObject(index))?.let(::addRelease)
            }
        }
        val shelves = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicShelfRenderer", shelves)
        shelves.forEach { shelf ->
            val contents = shelf.optJSONArray("contents") ?: JSONArray()
            for (index in 0 until contents.length()) {
                parseAlbumFromExploreItem(contents.optJSONObject(index))?.let(::addRelease)
            }
        }

        rankYoutubeMusicNewReleases(
            releases = releases.values.toList(),
            preferredArtists = preferredArtists,
            popularArtists = popularArtists,
            currentYear = Calendar.getInstance().get(Calendar.YEAR),
            limit = boundedLimit
        )
    }

    suspend fun musicSamples(
        seeds: List<Track>,
        preferredArtists: List<String>,
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        limit: Int = 24
    ): List<Track> = withContext(Dispatchers.IO) {
        val boundedLimit = limit.coerceIn(1, 40)
        val queries = youtubeMusicSampleQueries(seeds, preferredArtists, languageCode)
        val limiter = Semaphore(YOUTUBE_MUSIC_SAMPLE_QUERY_CONCURRENCY)
        val queryGroups = coroutineScope {
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
            }.awaitAll()
        }
        val personalizedAndLocal = interleaveYoutubeMusicSampleResults(queryGroups, boundedLimit)
        val nativeVideos = runCatching { explore(languageCode).newVideos }
            .getOrDefault(emptyList())
            .mapNotNull(::asYoutubeMusicSample)
        (personalizedAndLocal + nativeVideos)
            .distinctBy { it.id }
            .take(boundedLimit)
    }
'''
text = replace_once(text, old_methods, new_methods, "new releases and samples methods")
path.write_text(text, encoding="utf-8")


# Personalization-aware Samples cache.
path = Path("app/src/main/java/com/luc4n3x/levyra/data/YoutubeShortsCache.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    "import timber.log.Timber\n",
    "import timber.log.Timber\nimport java.util.Locale\n",
    "cache Locale import",
)
text = replace_once(
    text,
    '''    fun load(languageCode: String): YoutubeShortsCacheSnapshot {
        val key = cacheKey(languageCode)
''',
    '''    fun load(languageCode: String, profileSignature: String = ""): YoutubeShortsCacheSnapshot {
        val key = cacheKey(languageCode, profileSignature)
''',
    "profile-aware cache load",
)
text = replace_once(
    text,
    '''    fun save(languageCode: String, tracks: List<Track>, savedAtMs: Long = System.currentTimeMillis()) {
''',
    '''    fun save(
        languageCode: String,
        tracks: List<Track>,
        savedAtMs: Long = System.currentTimeMillis(),
        profileSignature: String = ""
    ) {
''',
    "profile-aware cache save signature",
)
text = replace_once(
    text,
    '''        preferences.edit().putString(cacheKey(languageCode), root.toString()).apply()
    }

    private fun cacheKey(languageCode: String): String =
        "shorts_${LevyraLanguageCatalog.normalize(languageCode)}"
''',
    '''        preferences.edit().putString(cacheKey(languageCode, profileSignature), root.toString()).apply()
    }

    private fun cacheKey(languageCode: String, profileSignature: String): String {
        val language = LevyraLanguageCatalog.normalize(languageCode)
        val normalizedProfile = profileSignature.trim().lowercase(Locale.ROOT)
        val suffix = if (normalizedProfile.isBlank()) {
            "default"
        } else {
            normalizedProfile.hashCode().toUInt().toString(16)
        }
        return "shorts_${language}_$suffix"
    }
''',
    "profile-aware cache key",
)
path.write_text(text, encoding="utf-8")


# ViewModel: use the same live user signals for Samples, cache identity, and New Releases.
path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''    private var musicVideosLoadedLanguage = ""
    private var musicVideosRetryLanguage = ""
''',
    '''    private var musicVideosLoadedLanguage = ""
    private var musicVideosLoadedProfileSignature = ""
    private var musicVideosRetryLanguage = ""
''',
    "Samples loaded profile state",
)
text = replace_once(
    text,
    '''    private var newReleasesLoadedLanguage = ""
    private var newReleasesJob: Job? = null
''',
    '''    private var newReleasesLoadedLanguage = ""
    private var newReleasesLoadedProfileSignature = ""
    private var newReleasesJob: Job? = null
''',
    "New Releases loaded profile state",
)
marker = '''    private fun ensureMusicVideosLoaded() {
'''
helpers = '''    private fun discoveryPreferredArtists(snapshot: LevyraUiState, limit: Int = 24): List<String> = buildList {
        snapshot.currentTrack?.artist?.let(::add)
        addAll(snapshot.followedArtists.map { artist -> artist.name })
        addAll(snapshot.recentListens.map { track -> track.artist })
        addAll(snapshot.favorites.map { track -> track.artist })
        addAll(snapshot.personalOrbitTracks.map { track -> track.artist })
        addAll(snapshot.homeResonanceTracks.map { track -> track.artist })
        addAll(snapshot.charts.map { track -> track.artist })
        snapshot.homeSections.forEach { section -> addAll(section.tracks.map { track -> track.artist }) }
    }
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy { artist -> artist.lowercase(java.util.Locale.ROOT) }
        .take(limit)

    private fun samplesDiscoveryProfileSignature(snapshot: LevyraUiState): String {
        val artists = discoveryPreferredArtists(snapshot, 16)
        val trackIds = buildList {
            snapshot.currentTrack?.id?.let(::add)
            addAll(snapshot.recentListens.take(8).map { track -> track.id })
            addAll(snapshot.favorites.take(8).map { track -> track.id })
            addAll(snapshot.personalOrbitTracks.take(8).map { track -> track.id })
        }.filter(String::isNotBlank).distinct()
        return buildString {
            append(LevyraLanguageCatalog.normalize(snapshot.languageCode))
            append('|').append(artists.joinToString("|"))
            append('|').append(trackIds.joinToString("|"))
        }
    }

    private fun ensureMusicVideosLoaded() {
'''
text = replace_once(text, marker, helpers, "discovery preference helpers")
text = replace_once(
    text,
    '''        val snapshot = _state.value
        val languageCode = snapshot.languageCode
        if (musicVideosJob?.isActive == true) return
        if (snapshot.exploreVideos.isEmpty()) {
            val cached = shortsCache.load(languageCode)
''',
    '''        val snapshot = _state.value
        val languageCode = snapshot.languageCode
        val profileSignature = samplesDiscoveryProfileSignature(snapshot)
        if (musicVideosJob?.isActive == true) return
        if (snapshot.exploreVideos.isEmpty()) {
            val cached = shortsCache.load(languageCode, profileSignature)
''',
    "profile-aware Samples cache read",
)
text = replace_once(
    text,
    '''        if (musicVideosLoadedLanguage == languageCode) return
''',
    '''        if (
            musicVideosLoadedLanguage == languageCode &&
            musicVideosLoadedProfileSignature == profileSignature
        ) return
''',
    "profile-aware Samples loaded check",
)
old_preferred = '''            val preferredArtists = buildList {
                addAll(seedSnapshot.followedArtists.map { artist -> artist.name })
                addAll(seedSnapshot.recentListens.map { track -> track.artist })
                addAll(seedSnapshot.favorites.map { track -> track.artist })
                addAll(seedSnapshot.personalOrbitTracks.map { track -> track.artist })
                addAll(seeds.map { track -> track.artist })
            }
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinctBy { artist -> artist.lowercase(java.util.Locale.ROOT) }
                .take(16)
'''
new_preferred = '''            val preferredArtists = discoveryPreferredArtists(seedSnapshot, 16)
'''
text = replace_once(text, old_preferred, new_preferred, "shared preferred artists")
text = replace_once(
    text,
    '''            musicVideosLoadedLanguage = languageCode
            musicVideosRetryLanguage = ""
''',
    '''            musicVideosLoadedLanguage = languageCode
            musicVideosLoadedProfileSignature = profileSignature
            musicVideosRetryLanguage = ""
''',
    "save loaded Samples profile",
)
text = replace_once(
    text,
    '''            withContext(Dispatchers.IO) {
                shortsCache.save(languageCode, resolvedFeedTracks)
            }
''',
    '''            withContext(Dispatchers.IO) {
                shortsCache.save(
                    languageCode = languageCode,
                    tracks = resolvedFeedTracks,
                    profileSignature = profileSignature
                )
            }
''',
    "profile-aware Samples cache save",
)
old_releases = '''    private fun ensureOfficialNewReleasesLoaded(force: Boolean = false) {
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
'''
new_releases = '''    private fun ensureOfficialNewReleasesLoaded(force: Boolean = false) {
        val snapshot = _state.value
        val languageCode = snapshot.languageCode
        val preferredArtists = discoveryPreferredArtists(snapshot, 24)
        val profileSignature = buildString {
            append(LevyraLanguageCatalog.normalize(languageCode))
            append('|').append(preferredArtists.joinToString("|"))
        }
        if (newReleasesJob?.isActive == true) return
        if (
            !force &&
            newReleasesLoadedLanguage == languageCode &&
            newReleasesLoadedProfileSignature == profileSignature &&
            snapshot.exploreNewReleases.isNotEmpty()
        ) return
        if (
            newReleasesLoadedLanguage != languageCode ||
            newReleasesLoadedProfileSignature != profileSignature
        ) {
            _state.update { current ->
                current.copy(exploreNewReleases = emptyList(), newReleasesLoadFailed = false)
            }
        }
        _state.update { current -> current.copy(isNewReleasesLoading = true, newReleasesLoadFailed = false) }
        newReleasesJob = viewModelScope.launch {
            val releases = try {
                repository.newReleases(
                    languageCode = languageCode,
                    limit = 48,
                    preferredArtists = preferredArtists
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Timber.w(error, "Personalized new releases failed for %s", languageCode)
                emptyList()
            }
            if (_state.value.languageCode != languageCode) return@launch
            if (releases.isNotEmpty()) {
                newReleasesLoadedLanguage = languageCode
                newReleasesLoadedProfileSignature = profileSignature
            }
            _state.update { current ->
                current.copy(
                    exploreNewReleases = releases,
                    isNewReleasesLoading = false,
                    newReleasesLoadFailed = releases.isEmpty()
                )
            }
        }
    }
'''
text = replace_once(text, old_releases, new_releases, "personalized new releases loading")
text = replace_once(
    text,
    '''        musicVideosLoadedLanguage = ""
        musicVideosRetryLanguage = ""
''',
    '''        musicVideosLoadedLanguage = ""
        musicVideosLoadedProfileSignature = ""
        musicVideosRetryLanguage = ""
''',
    "reset Samples profile on refresh",
)
path.write_text(text, encoding="utf-8")


# Remove provider branding from the destination header.
path = Path("app/src/main/java/com/luc4n3x/levyra/ui/ExploreDestinationScreens.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''        title = strings.exploreNewReleases,
        subtitle = "YouTube Music",
''',
    '''        title = strings.exploreNewReleases,
        subtitle = null,
''',
    "remove provider subtitle",
)
path.write_text(text, encoding="utf-8")


# Regression coverage.
path = Path("app/src/test/java/com/luc4n3x/levyra/data/YoutubeMusicSamplesPolicyTest.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''    @Test
    fun previewStartsInsideLongMusicVideoButNotOrdinaryPlayback() {
''',
    '''    @Test
    fun languageQueriesKeepReservedSlotsEvenWithManyUserSignals() {
        val seeds = List(12) { index -> track(title = "Brano $index", artist = "Artista $index") }
        val artists = List(12) { index -> "Seguito $index" }

        val queries = youtubeMusicSampleQueries(seeds, artists, "it")

        assertEquals(8, queries.size)
        assertTrue(queries.count { it.contains("italian", ignoreCase = true) || it.contains("Italia", ignoreCase = true) } >= 3)
        assertTrue(queries.first().contains("Seguito 0"))
    }

    @Test
    fun queryGroupsAreRoundRobinInsteadOfOneSourceDominating() {
        val groups = listOf(
            listOf(track(id = "a1"), track(id = "a2")),
            listOf(track(id = "l1"), track(id = "l2")),
            listOf(track(id = "b1"), track(id = "b2"))
        )

        assertEquals(
            listOf("a1", "l1", "b1", "a2", "l2", "b2"),
            interleaveYoutubeMusicSampleResults(groups, 6).map { it.id }
        )
    }

    @Test
    fun previewStartsInsideLongMusicVideoButNotOrdinaryPlayback() {
''',
    "Samples language and interleave tests",
)
text = replace_once(
    text,
    '''    private fun track(
        title: String = "Title",
''',
    '''    private fun track(
        id: String = "abcdefghijk",
        title: String = "Title",
''',
    "Samples test track id parameter",
)
text = replace_once(
    text,
    '''        id = "abcdefghijk",
''',
    '''        id = id,
''',
    "Samples test track id use",
)
path.write_text(text, encoding="utf-8")

new_test = Path("app/src/test/java/com/luc4n3x/levyra/data/YoutubeMusicNewReleasesPolicyTest.kt")
new_test.write_text('''package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ReleaseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class YoutubeMusicNewReleasesPolicyTest {
    @Test
    fun preferredAndLocallyPopularArtistsBeatUnrelatedCatalogueNoise() {
        val preferred = release("Lazza", "Nuovo disco", "preferred", ReleaseType.Album)
        val popular = release("Annalisa", "Nuovo singolo", "popular", ReleaseType.Single)
        val unrelated = release("Unknown Wave", "Real Release", "unrelated", ReleaseType.Album)

        val ranked = rankYoutubeMusicNewReleases(
            releases = listOf(unrelated, popular, preferred),
            preferredArtists = listOf("Lazza"),
            popularArtists = listOf("Annalisa"),
            currentYear = 2026,
            limit = 10
        )

        assertEquals(listOf("preferred", "popular", "unrelated"), ranked.map { it.browseId.removePrefix("MPRE") })
    }

    @Test
    fun compilationSpamAndAnonymousUnknownEntriesAreRejected() {
        val spam = release("Various Artists", "Reggae Hits Vol 2", "spam", ReleaseType.Compilation)
        val anonymous = release(
            artist = "Random Name",
            title = "Random Song",
            id = "anonymous",
            type = ReleaseType.Unknown,
            artistBrowseId = ""
        )
        val valid = release("Lazza", "Release reale", "valid", ReleaseType.Single)

        val ranked = rankYoutubeMusicNewReleases(
            releases = listOf(spam, anonymous, valid),
            preferredArtists = listOf("Lazza"),
            popularArtists = emptyList(),
            currentYear = 2026,
            limit = 10
        )

        assertEquals(listOf("MPREvalid"), ranked.map { it.browseId })
        assertFalse(ranked.any { it.title.contains("Hits Vol", ignoreCase = true) })
    }

    private fun release(
        artist: String,
        title: String,
        id: String,
        type: ReleaseType,
        artistBrowseId: String = "MPLA-$id"
    ): AlbumHit = AlbumHit(
        title = title,
        artist = artist,
        year = "2026",
        thumbnailUrl = "https://levyra.test/$id.jpg",
        query = "$artist $title",
        browseId = "MPRE$id",
        artistBrowseId = artistBrowseId,
        releaseType = type
    )
}
''', encoding="utf-8")

print("Applied personalized Samples and strict New Releases discovery")

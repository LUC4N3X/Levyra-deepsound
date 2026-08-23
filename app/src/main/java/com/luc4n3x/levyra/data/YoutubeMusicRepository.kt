package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.security.GoogleApiKeyHeaders
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.AlbumRecommendationSeed
import com.luc4n3x.levyra.domain.AlbumDetail
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.CacheReport
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LevyraLocalizedDiscovery
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.SearchPage
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.domain.releaseTypeFromProviderLabel
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.artistIdentityKey
import com.luc4n3x.levyra.domain.isArtistShelfNameEligible
import com.luc4n3x.levyra.domain.primaryArtistSegment
import com.luc4n3x.levyra.domain.parseCompactViewCount
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Calendar
import java.util.Locale
import kotlin.math.absoluteValue

data class YoutubeMusicMoodCategory(
    val title: String,
    val params: String,
    val section: String = ""
)

data class YoutubeMusicMoodPlaylist(
    val title: String,
    val playlistId: String,
    val browseId: String,
    val description: String,
    val thumbnailUrl: String
)

data class YoutubeMusicExplore(
    val newReleases: List<AlbumHit> = emptyList(),
    val topSongs: List<Track> = emptyList(),
    val moodsAndGenres: List<YoutubeMusicMoodCategory> = emptyList(),
    val trending: List<Track> = emptyList(),
    val newVideos: List<Track> = emptyList()
)

data class YoutubeMusicPlaylistDetail(
    val playlistId: String,
    val title: String,
    val description: String,
    val author: String,
    val thumbnailUrl: String,
    val tracks: List<Track>,
    val continuation: String = ""
)

private data class ScoredAlbumRecommendation(
    val album: AlbumHit,
    val score: Int
)

internal const val LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE = Int.MIN_VALUE

internal val LEVYRA_LOCALIZED_ALBUM_LABELS = setOf(
    "album",
    "álbum",
    "albumo",
    "άλμπουμ",
    "albüm",
    "альбом",
    "албум",
    "ألبوم",
    "专辑",
    "專輯",
    "アルバム",
    "앨범",
    "एल्बम",
    "อัลบั้ม",
    "אלבום"
)

internal fun levyraReleaseType(token: String): ReleaseType = releaseTypeFromProviderLabel(token)

internal fun levyraIsAlbumLabel(token: String): Boolean {
    return levyraReleaseType(token) == ReleaseType.Album
}

internal fun isPlausibleYoutubeMusicAlbumTitle(value: String): Boolean {
    val normalized = value
        .replace('\u00A0', ' ')
        .replace('\u202F', ' ')
        .replace("\\n", " ")
        .replace('\n', ' ')
        .replace('\r', ' ')
        .replace(RECOMMENDATION_WHITESPACE, " ")
        .trim()
        .lowercase(Locale.ROOT)
    if (normalized.isBlank() || normalized.looksLikeSerializedJson()) return false

    val trackCountParts = normalized.split(' ')
    val trackCountLabels = setOf(
        "brano", "brani", "traccia", "tracce", "canzone", "canzoni",
        "song", "songs", "track", "tracks", "titre", "titres", "lieder",
        "cancion", "canciones", "faixa", "faixas", "곡", "曲", "שיר", "שירים"
    )
    if (
        trackCountParts.size == 2 &&
        trackCountParts.first().all(Char::isDigit) &&
        trackCountParts.last() in trackCountLabels
    ) return false

    if (hasYoutubeMusicMetricWord(normalized)) return false
    return !ALBUM_TRACK_METRIC_PATTERN.containsMatchIn(normalized)
}

internal fun levyraAlbumRecommendationMatchScore(album: AlbumHit, seed: AlbumRecommendationSeed): Int {
    val albumKey = albumRecommendationTextKey(album.title)
    val artistKey = albumRecommendationTextKey(album.artist)
    val seedAlbumKey = albumRecommendationTextKey(seed.album)
    val seedArtistKey = albumRecommendationTextKey(seed.artist)
    if (!isPlausibleYoutubeMusicAlbumTitle(album.title) || albumKey.isBlank() || artistKey.isBlank()) return LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE

    val seedArtistTokens = recommendationTokens(seedArtistKey)
    val artistCompatibility = recommendationCompatibility(artistKey, seedArtistKey)
    val artistScore = when {
        seedArtistKey.isBlank() -> 0
        artistKey == seedArtistKey -> 520
        seedArtistTokens.size == 1 -> LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE
        artistCompatibility >= 0.75 -> 380
        artistCompatibility >= 0.55 -> 240
        else -> LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE
    }
    if (artistScore == LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE) return artistScore

    if (seedAlbumKey.isNotBlank()) {
        val titleScore = when {
            albumKey == seedAlbumKey -> 640
            recommendationCompatibility(albumKey, seedAlbumKey) >= 0.82 -> 460
            else -> LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE
        }
        if (titleScore == LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE) return titleScore
        if (seedArtistKey.isNotBlank() && artistScore < 240) return LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE
        return 900 + titleScore + artistScore
    }

    if (seedArtistKey.isNotBlank()) return 520 + artistScore

    val moodKeys = seed.moodTags.map(::albumRecommendationTextKey).filter { it.isNotBlank() }
    if (moodKeys.isEmpty()) return LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE
    val searchable = "$albumKey $artistKey"
    val moodMatches = moodKeys.count { mood ->
        val tokens = recommendationTokens(mood)
        tokens.isNotEmpty() && tokens.all { token -> searchable.split(' ').contains(token) }
    }
    return 160 + moodMatches * 90
}

internal fun albumRecommendationIdentityKey(album: AlbumHit): String =
    "${albumRecommendationTextKey(album.title)}|${albumRecommendationTextKey(album.artist)}"

internal fun albumRecommendationDeduplicationKey(album: AlbumHit): String {
    val title = albumRecommendationDisplayTitleKey(album.title)
    val primaryArtist = albumRecommendationTextKey(
        album.artist.split(ALBUM_RECOMMENDATION_ARTIST_SEPARATOR, limit = 2).firstOrNull().orEmpty()
    )
    if (title.isNotBlank() && primaryArtist.isNotBlank()) return "release:$title|$primaryArtist"
    val upc = album.upc.filter { it.isLetterOrDigit() }.lowercase(Locale.ROOT)
    if (upc.isNotBlank()) return "upc:$upc"
    val canonical = album.canonicalUrl.trim().lowercase(Locale.ROOT).substringBefore('?')
    if (canonical.isNotBlank()) return "canonical:$canonical"
    val browseId = album.browseId.trim().lowercase(Locale.ROOT)
    if (browseId.isNotBlank()) return "browse:$browseId"
    val artwork = album.thumbnailUrl
        .trim()
        .lowercase(Locale.ROOT)
        .substringBefore('?')
        .substringBefore('=')
    if (title.isNotBlank() && artwork.isNotBlank()) return "artwork:$title|$artwork"
    return albumRecommendationIdentityKey(album)
}

private fun albumRecommendationDisplayTitleKey(value: String): String {
    return albumRecommendationTextKey(value)
        .replace(ALBUM_RECOMMENDATION_EDITION_SUFFIX, " ")
        .replace(RECOMMENDATION_WHITESPACE, " ")
        .trim()
}

// Precompiled so recommendation ranking does not rebuild ICU regex state per candidate.
private val RECOMMENDATION_WHITESPACE = Regex("""\s+""")
private val RECOMMENDATION_COMBINING_MARKS = Regex("""\p{M}+""")
private val RECOMMENDATION_NON_ALPHANUMERIC = Regex("""[^\p{L}\p{N}]+""")
private val RECOMMENDATION_CREDIT_SUFFIX =
    Regex("""(?i)\b(feat|featuring|ft|with|prod|official|audio|video|lyrics)\b.*$""")

private val ALBUM_RECOMMENDATION_ARTIST_SEPARATOR =
    Regex("(?i)\\s*(?:,|;|&|\\bfeat(?:uring)?\\b|\\bft\\b|\\s+[x×]\\s+)\\s*")

private val ALBUM_RECOMMENDATION_EDITION_SUFFIX = Regex(
    """(?i)\s+(?:deluxe(?: edition)?|expanded(?: edition)?|anniversary(?: edition)?|remaster(?:ed)?(?: edition)?|bonus(?: track)?s?|special edition|collector(?:s)? edition|legacy edition|tour edition|digital edition|international edition|explicit edition|explicit version)\b.*$"""
)

// Recommendation ranking scores every candidate against the same seed strings, so the
// normalized form is memoized to keep ICU work off the per-candidate path.
private const val RECOMMENDATION_TEXT_KEY_CACHE_LIMIT = 1024
private const val RECOMMENDATION_TEXT_KEY_MAX_LENGTH = 256
private val recommendationTextKeyCache =
    object : LinkedHashMap<String, String>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>): Boolean =
            size > RECOMMENDATION_TEXT_KEY_CACHE_LIMIT
    }

internal fun albumRecommendationTextKey(value: String): String {
    if (value.length > RECOMMENDATION_TEXT_KEY_MAX_LENGTH) return computeAlbumRecommendationTextKey(value)
    synchronized(recommendationTextKeyCache) { recommendationTextKeyCache[value] }?.let { return it }
    val computed = computeAlbumRecommendationTextKey(value)
    synchronized(recommendationTextKeyCache) { recommendationTextKeyCache[value] = computed }
    return computed
}

private fun computeAlbumRecommendationTextKey(value: String): String {
    val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
    return decomposed
        .replace(RECOMMENDATION_COMBINING_MARKS, "")
        .lowercase(Locale.ROOT)
        .replace('&', ' ')
        .replace(RECOMMENDATION_CREDIT_SUFFIX, " ")
        .replace(RECOMMENDATION_NON_ALPHANUMERIC, " ")
        .replace(RECOMMENDATION_WHITESPACE, " ")
        .trim()
}

private fun recommendationCompatibility(left: String, right: String): Double {
    if (left.isBlank() || right.isBlank()) return 0.0
    if (left == right) return 1.0
    val leftTokens = recommendationTokens(left)
    val rightTokens = recommendationTokens(right)
    if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0.0
    val intersection = leftTokens.intersect(rightTokens.toSet()).size.toDouble()
    val coverage = intersection / minOf(leftTokens.size, rightTokens.size).toDouble()
    val union = leftTokens.union(rightTokens).size.toDouble()
    val jaccard = if (union == 0.0) 0.0 else intersection / union
    return coverage * 0.7 + jaccard * 0.3
}

private fun recommendationTokens(value: String): List<String> = albumRecommendationTextKey(value)
    .split(' ')
    .filter { token -> token.isNotBlank() && token !in ALBUM_RECOMMENDATION_STOP_WORDS }

private val ALBUM_RECOMMENDATION_STOP_WORDS = setOf(
    "album", "the", "and", "con", "per", "una", "uno", "del", "della", "degli", "delle", "di", "da",
    "music", "musica", "new", "nuovo", "nuova", "popular", "popolari", "italian", "italiano", "italiana"
)

private const val MAX_ALBUM_RECOMMENDATION_SEEDS = 16
private const val ALBUM_RECOMMENDATION_CONCURRENCY = 4
private const val ALBUM_RESULTS_PER_SEED = 8
private const val ALBUM_RESULTS_PER_FALLBACK_QUERY = 8
private const val ALBUM_RESULT_RANK_PENALTY = 18
internal const val YOUTUBE_MUSIC_VIDEO_SEARCH_PARAMS = "EgWKAQIQAWoMEA4QChADEAQQCRAF"
internal const val YOUTUBE_MUSIC_SONG_SEARCH_PARAMS = "EgWKAQIIAWoMEA4QChADEAQQCRAF"
internal const val YOUTUBE_MUSIC_ALBUM_SEARCH_PARAMS = "EgWKAQIYAWoMEA4QChADEAQQCRAF"
internal const val YOUTUBE_MUSIC_ARTIST_SEARCH_PARAMS = "EgWKAQIgAWoMEA4QChADEAQQCRAF"
internal const val YOUTUBE_MUSIC_PLAYLIST_SEARCH_PARAMS = "EgWKAQIoAWoMEA4QChADEAQQCRAF"

internal enum class SearchEntityKind {
    Track,
    Album,
    Artist,
    Playlist
}

private const val MUSIC_PAGE_TYPE_ALBUM = "MUSIC_PAGE_TYPE_ALBUM"
private const val MUSIC_PAGE_TYPE_ARTIST = "MUSIC_PAGE_TYPE_ARTIST"
private const val MUSIC_PAGE_TYPE_PLAYLIST = "MUSIC_PAGE_TYPE_PLAYLIST"
private const val SEARCH_OVERVIEW_SONG_LIMIT = 20
private const val SEARCH_OVERVIEW_VIDEO_LIMIT = 10
private const val SEARCH_OVERVIEW_ARTIST_LIMIT = 8
private const val SEARCH_OVERVIEW_ALBUM_LIMIT = 10
private const val SEARCH_OVERVIEW_PLAYLIST_LIMIT = 10
private val SUBSCRIBER_TOKEN_HINTS = listOf(
    "subscriber", "iscritt", "scritt", "abonn", "suscriptor", "inscrito", "abonnee",
    "subskry", "abonat", "συνδρομ", "подпис", "订阅", "訂閱", "チャンネル登録", "구독", "सदस्य", "ผู้ติดตาม", "מנוי", "مشترك"
)
private const val YOUTUBE_MUSIC_NEW_RELEASE_FALLBACK_LIMIT = 4
private const val YOUTUBE_MUSIC_ARTIST_CONTAINS_MIN_LENGTH = 4

private val GENERIC_NEW_RELEASE_TITLE = Regex(
    """\b(?:best\s+of|greatest\s+hits?|hits?\s+vol(?:ume)?|vol(?:ume)?\.?\s*\d+|collection|compilation|karaoke|type\s+beat|tribute|instrumental\s+versions?)\b""",
    RegexOption.IGNORE_CASE
)
private val GENERIC_NEW_RELEASE_ARTIST = Regex(
    """^(?:various\s+artists?|unknown\s+artist|youtube\s+music|topic)$""",
    RegexOption.IGNORE_CASE
)

private data class RankedYoutubeMusicRelease(
    val release: AlbumHit,
    val score: Int,
    val bucket: Int
)

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
    .replace(RECOMMENDATION_COMBINING_MARKS, "")
    .lowercase(Locale.ROOT)
    .replace(RECOMMENDATION_NON_ALPHANUMERIC, " ")
    .replace(RECOMMENDATION_WHITESPACE, " ")
    .trim()

private fun matchingArtistSignalIndex(artist: String, signals: List<String>): Int {
    if (artist.length < 2) return -1
    return signals.indexOfFirst { signal ->
        when {
            signal.length < 2 -> false
            artist == signal -> true
            artist.length < YOUTUBE_MUSIC_ARTIST_CONTAINS_MIN_LENGTH -> false
            signal.length < YOUTUBE_MUSIC_ARTIST_CONTAINS_MIN_LENGTH -> false
            else -> artist.contains(signal) || signal.contains(artist)
        }
    }
}

internal fun selectAlbumRecoveryCandidate(
    album: AlbumHit,
    candidates: List<AlbumHit>,
    excludedBrowseIds: Set<String> = emptySet()
): AlbumHit? {
    val titleKey = albumRecommendationTextKey(album.title)
    if (titleKey.isBlank()) return null

    val artistKey = albumRecommendationTextKey(album.artist)
    val primaryArtistKey = albumRecommendationTextKey(primaryArtistSegment(album.artist))
    val artistBrowseId = album.artistBrowseId.trim()
    val excluded = excludedBrowseIds
        .map { it.trim().lowercase(Locale.ROOT) }
        .filter(String::isNotBlank)
        .toSet()

    return candidates.asSequence()
        .filter { candidate ->
            val browseId = candidate.browseId.trim()
            browseId.isNotBlank() && browseId.lowercase(Locale.ROOT) !in excluded
        }
        .filter { candidate ->
            albumRecommendationTextKey(candidate.title) == titleKey
        }
        .firstOrNull { candidate ->
            val candidateArtistKey = albumRecommendationTextKey(candidate.artist)
            val candidatePrimaryArtistKey = albumRecommendationTextKey(primaryArtistSegment(candidate.artist))
            (artistKey.isNotBlank() && candidateArtistKey == artistKey) ||
                (primaryArtistKey.isNotBlank() && candidatePrimaryArtistKey == primaryArtistKey) ||
                (artistBrowseId.isNotBlank() && candidate.artistBrowseId.equals(artistBrowseId, ignoreCase = true))
        }
}

class YoutubeMusicRepository(private val context: Context? = null) {
    private val apiKey = BuildConfig.YOUTUBE_INNERTUBE_API_KEY
    private val clientVersion = "1.20260423.01.00"
    private val memory = LinkedHashMap<String, Track>()
    private val watchRepository = YoutubeMusicWatchRepository(context)
    private val resilienceClient = YoutubeMusicResilienceClient(context, apiKey, clientVersion)
    private val albumDescriptionRepository = AlbumDescriptionRepository(context)

    suspend fun search(query: String, limit: Int = 36, languageCode: String = LevyraLanguageCatalog.deviceDefault()): List<Track> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()
        val remote = try {
            searchInnerTube(cleanQuery, limit, languageCode)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            emptyList()
        }
        if (remote.isNotEmpty()) {
            remote.forEach { memory[it.id] = it }
            return@withContext remote
        }
        try {
            searchYoutubeExtractor(cleanQuery, limit)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            emptyList()
        }.also { items -> items.forEach { memory[it.id] = it } }
    }

    /** YouTube Music video-filtered search, with a bounded generic-search fallback. */
    suspend fun searchMusicVideos(
        query: String,
        limit: Int = 12,
        languageCode: String = LevyraLanguageCatalog.deviceDefault()
    ): List<Track> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()
        val boundedLimit = limit.coerceIn(1, 40)
        val filtered = try {
            searchMusicVideoTracks(cleanQuery, languageCode, boundedLimit)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            emptyList()
        }
        val result = filtered.ifEmpty {
            // The video filter can occasionally omit type metadata. Keep generic
            // search results as a last resort and let the playback identity checks
            // and resolver validate them instead of turning an inconclusive miss
            // into an early, negative result.
            search(cleanQuery, boundedLimit, languageCode)
        }
        result.take(boundedLimit).also { items -> items.forEach { memory[it.id] = it } }
    }

    /** First YouTube Music match for a query, used to make chart entries playable. */
    suspend fun searchOne(query: String, languageCode: String = LevyraLanguageCatalog.deviceDefault()): Track? = search(query, 1, languageCode).firstOrNull()

    suspend fun searchEverything(query: String, languageCode: String = LevyraLanguageCatalog.deviceDefault()): SearchResults = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext SearchResults()
        val root = runCatching { searchInnerTubeRaw(cleanQuery, languageCode) }.getOrNull() ?: return@withContext fallbackResults(cleanQuery, languageCode)
        val overview = parseSearchOverview(root, cleanQuery)
        val withLegacyFallback = if (overview.songs.isEmpty() && overview.videos.isEmpty()) {
            overview.copy(songs = parseLegacyVideoRenderers(root, cleanQuery))
        } else {
            overview
        }
        val results = withLegacyFallback.let { parsed ->
            parsed.copy(
                topTrack = parsed.songs.firstOrNull() ?: parsed.videos.firstOrNull(),
                songs = parsed.songs.take(SEARCH_OVERVIEW_SONG_LIMIT),
                videos = parsed.videos.take(SEARCH_OVERVIEW_VIDEO_LIMIT),
                artists = parsed.artists.take(SEARCH_OVERVIEW_ARTIST_LIMIT),
                albums = parsed.albums.take(SEARCH_OVERVIEW_ALBUM_LIMIT),
                playlists = parsed.playlists.take(SEARCH_OVERVIEW_PLAYLIST_LIMIT)
            )
        }
        rememberSearchTracks(results.songs)
        rememberSearchTracks(results.videos)
        if (results.isEmpty) fallbackResults(cleanQuery, languageCode) else results
    }

    suspend fun searchSongsPage(
        query: String,
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        continuation: String = ""
    ): SearchPage<Track> = searchTrackPage(query, languageCode, YOUTUBE_MUSIC_SONG_SEARCH_PARAMS, continuation)

    suspend fun searchVideosPage(
        query: String,
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        continuation: String = ""
    ): SearchPage<Track> = searchTrackPage(query, languageCode, YOUTUBE_MUSIC_VIDEO_SEARCH_PARAMS, continuation)

    suspend fun searchAlbumsPage(
        query: String,
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        continuation: String = ""
    ): SearchPage<AlbumHit> = withContext(Dispatchers.IO) {
        val root = searchSectionRoot(query, languageCode, YOUTUBE_MUSIC_ALBUM_SEARCH_PARAMS, continuation)
            ?: return@withContext SearchPage()
        SearchPage(parseSearchOverview(root, query.trim()).albums, findSearchContinuation(root))
    }

    suspend fun searchArtistsPage(
        query: String,
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        continuation: String = ""
    ): SearchPage<ArtistHit> = withContext(Dispatchers.IO) {
        val root = searchSectionRoot(query, languageCode, YOUTUBE_MUSIC_ARTIST_SEARCH_PARAMS, continuation)
            ?: return@withContext SearchPage()
        SearchPage(parseSearchOverview(root, query.trim()).artists, findSearchContinuation(root))
    }

    suspend fun searchPlaylistsPage(
        query: String,
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        continuation: String = ""
    ): SearchPage<PlaylistHit> = withContext(Dispatchers.IO) {
        val root = searchSectionRoot(query, languageCode, YOUTUBE_MUSIC_PLAYLIST_SEARCH_PARAMS, continuation)
            ?: return@withContext SearchPage()
        SearchPage(parseSearchOverview(root, query.trim()).playlists, findSearchContinuation(root))
    }

    private suspend fun searchTrackPage(
        query: String,
        languageCode: String,
        params: String,
        continuation: String
    ): SearchPage<Track> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        val root = searchSectionRoot(cleanQuery, languageCode, params, continuation) ?: return@withContext SearchPage()
        val parsed = parseSearchOverview(root, cleanQuery)
        val tracks = mergeSearchSongs(parsed.songs, parsed.videos).ifEmpty {
            parseLegacyVideoRenderers(root, cleanQuery)
        }
        rememberSearchTracks(tracks)
        SearchPage(tracks, findSearchContinuation(root))
    }

    private fun searchSectionRoot(
        query: String,
        languageCode: String,
        params: String,
        continuation: String
    ): JSONObject? {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return null
        return runCatching {
            searchInnerTubeRaw(cleanQuery, languageCode, params, continuation.trim())
        }.getOrNull()
    }

    private fun rememberSearchTracks(tracks: List<Track>) {
        tracks.forEach { track -> memory[track.id] = track }
    }

    internal fun parseSearchOverview(root: JSONObject, query: String): SearchResults {
        val songs = mutableListOf<Track>()
        val videos = mutableListOf<Track>()
        val artists = mutableListOf<ArtistHit>()
        val albums = mutableListOf<AlbumHit>()
        val playlists = mutableListOf<PlaylistHit>()
        val listItems = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicResponsiveListItemRenderer", listItems)
        listItems.forEach { renderer ->
            when (searchEntityKind(renderer)) {
                SearchEntityKind.Artist -> parseSearchArtistHit(renderer)?.let(artists::add)
                SearchEntityKind.Album -> parseSearchAlbumHit(renderer)?.let(albums::add)
                SearchEntityKind.Playlist -> parseSearchPlaylistHit(renderer)?.let(playlists::add)
                SearchEntityKind.Track -> {
                    val track = parseMusicRenderer(renderer, query) ?: return@forEach
                    if (isMusicVideoResult(track.videoType)) videos.add(track) else songs.add(track)
                }
            }
        }
        val cards = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicTwoRowItemRenderer", cards)
        cards.forEach { card ->
            when (searchEntityKind(card)) {
                SearchEntityKind.Artist -> parseSearchArtistHit(card)?.let(artists::add)
                SearchEntityKind.Album -> parseTwoRowAlbumHit(card)?.let(albums::add)
                SearchEntityKind.Playlist -> parseSearchPlaylistHit(card)?.let(playlists::add)
                SearchEntityKind.Track -> Unit
            }
        }
        return SearchResults(
            songs = deduplicateSearchSongs(songs),
            videos = deduplicateSearchSongs(videos),
            artists = deduplicateSearchArtists(artists),
            albums = deduplicateSearchAlbums(albums),
            playlists = deduplicateSearchPlaylists(playlists)
        )
    }

    private fun parseLegacyVideoRenderers(root: JSONObject, query: String): List<Track> {
        val videoRenderers = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "videoRenderer", videoRenderers)
        return deduplicateSearchSongs(videoRenderers.mapNotNull { parseVideoRenderer(it, query) })
    }

    internal fun searchEntityKind(renderer: JSONObject): SearchEntityKind {
        if (extractPrimaryMusicVideoId(renderer).isNotBlank()) return SearchEntityKind.Track
        val browseEndpoints = mutableListOf<JSONObject>()
        collectObjectsByKey(renderer, "browseEndpoint", browseEndpoints)
        var artist = false
        var album = false
        var playlist = false
        browseEndpoints.forEach { endpoint ->
            val browseId = endpoint.optString("browseId").orEmpty()
            val pageType = endpoint.optJSONObject("browseEndpointContextSupportedConfigs")
                ?.optJSONObject("browseEndpointContextMusicConfig")
                ?.optString("pageType")
                .orEmpty()
                .uppercase(Locale.ROOT)
            when {
                pageType == MUSIC_PAGE_TYPE_ALBUM || browseId.startsWith("MPRE", ignoreCase = true) -> album = true
                pageType == MUSIC_PAGE_TYPE_PLAYLIST || browseId.startsWith("VL", ignoreCase = true) -> playlist = true
                pageType == MUSIC_PAGE_TYPE_ARTIST || browseId.startsWith("UC") -> artist = true
            }
        }
        if (album) return SearchEntityKind.Album
        if (playlist) return SearchEntityKind.Playlist
        if (hasPlaylistOnlyWatchEndpoint(renderer)) return SearchEntityKind.Playlist
        if (artist) return SearchEntityKind.Artist
        return SearchEntityKind.Track
    }

    private fun hasPlaylistOnlyWatchEndpoint(renderer: JSONObject): Boolean {
        val watchEndpoints = mutableListOf<JSONObject>()
        collectObjectsByKey(renderer, "watchEndpoint", watchEndpoints)
        if (watchEndpoints.isEmpty()) return false
        return watchEndpoints.any { endpoint ->
            endpoint.optString("playlistId").isNotBlank() && endpoint.optString("videoId").isBlank()
        }
    }

    private fun parseSearchArtistHit(renderer: JSONObject): ArtistHit? {
        val title = searchEntityTitle(renderer).ifBlank { return null }
        val artistReference = extractYoutubeMusicArtistReference(renderer, title)
        val resolvedName = artistReference?.name.orEmpty().ifBlank { title }.cleanLabel()
        if (!isArtistShelfNameEligible(resolvedName)) return null
        val seed = stableSeed(resolvedName)
        val accents = palette(seed)
        return ArtistHit(
            name = resolvedName,
            subscribers = searchEntitySubtitleTokens(renderer).firstOrNull(::isSubscriberToken).orEmpty(),
            thumbnailUrl = upgradeThumbnail(findBestThumbnail(renderer)),
            accentStart = accents.first,
            accentEnd = accents.second,
            browseId = artistReference?.browseId.orEmpty()
        )
    }

    private fun parseSearchAlbumHit(renderer: JSONObject): AlbumHit? {
        val title = searchEntityTitle(renderer).ifBlank { return null }
        if (!isPlausibleYoutubeMusicAlbumTitle(title)) return null
        val tokens = searchEntitySubtitleTokens(renderer)
        val releaseType = levyraReleaseType(tokens.firstOrNull().orEmpty())
        val albumArtist = tokens.drop(1).firstOrNull(::isAlbumArtistToken)
            ?: tokens.firstOrNull(::isAlbumArtistToken)
            .orEmpty()
        val artistReference = extractYoutubeMusicArtistReference(renderer, albumArtist)
        val resolvedArtist = artistReference?.name.orEmpty().ifBlank { albumArtist }.cleanLabel()
        val year = tokens.firstNotNullOfOrNull { Regex("""\b(19|20)\d{2}\b""").find(it)?.value }.orEmpty()
        return AlbumHit(
            title = title.cleanLabel(),
            artist = resolvedArtist.ifBlank { "Album" },
            year = year,
            thumbnailUrl = upgradeThumbnail(findBestThumbnail(renderer)),
            query = listOf(title, resolvedArtist).filter(String::isNotBlank).joinToString(" "),
            browseId = extractAlbumBrowseId(renderer),
            artistBrowseId = artistReference?.browseId.orEmpty(),
            audioPlaylistId = extractSearchPlaylistId(renderer),
            releaseType = releaseType
        )
    }

    private fun parseSearchPlaylistHit(renderer: JSONObject): PlaylistHit? {
        val title = searchEntityTitle(renderer).ifBlank { return null }
        val tokens = searchEntitySubtitleTokens(renderer)
        val playlistId = extractSearchPlaylistId(renderer)
        val browseId = extractSearchPlaylistBrowseId(renderer)
        if (playlistId.isBlank() && browseId.isBlank()) return null
        val author = tokens.firstOrNull(::isAlbumArtistToken).orEmpty()
        return PlaylistHit(
            title = title.cleanLabel(),
            author = author.cleanLabel(),
            thumbnailUrl = upgradeThumbnail(findBestThumbnail(renderer)),
            playlistId = playlistId,
            browseId = browseId,
            trackCountLabel = tokens
                .firstOrNull { token -> token != author && token.any(Char::isDigit) }
                .orEmpty()
        )
    }

    private fun searchEntityTitle(renderer: JSONObject): String {
        extractFlexLines(renderer).firstOrNull()?.takeIf(String::isNotBlank)?.let { return it.trim() }
        return renderer.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().trim()
    }

    private fun searchEntitySubtitleTokens(renderer: JSONObject): List<String> {
        val flexTokens = extractFlexLines(renderer).drop(1)
        val subtitle = renderer.optJSONObject("subtitle")?.optJSONArray("runs")?.joinText().orEmpty()
        return (flexTokens + subtitle)
            .flatMap { line -> line.split(" • ", " · ", " - ") }
            .map(String::trim)
            .filter(String::isNotBlank)
    }

    private fun isSubscriberToken(token: String): Boolean =
        token.any(Char::isDigit) && SUBSCRIBER_TOKEN_HINTS.any { hint -> token.contains(hint, ignoreCase = true) }

    private fun extractSearchPlaylistId(renderer: JSONObject): String {
        val watchEndpoints = mutableListOf<JSONObject>()
        collectObjectsByKey(renderer, "watchEndpoint", watchEndpoints)
        watchEndpoints.firstNotNullOfOrNull { endpoint ->
            endpoint.optString("playlistId").takeIf(String::isNotBlank)
        }?.let { return it }
        val browseId = extractSearchPlaylistBrowseId(renderer)
        return browseId.removePrefix("VL").takeIf { it.isNotBlank() && it != browseId }.orEmpty()
    }

    private fun extractSearchPlaylistBrowseId(renderer: JSONObject): String {
        val endpoints = mutableListOf<JSONObject>()
        collectObjectsByKey(renderer, "browseEndpoint", endpoints)
        return endpoints.firstNotNullOfOrNull { endpoint ->
            val browseId = endpoint.optString("browseId").orEmpty()
            val pageType = endpoint.optJSONObject("browseEndpointContextSupportedConfigs")
                ?.optJSONObject("browseEndpointContextMusicConfig")
                ?.optString("pageType")
                .orEmpty()
                .uppercase(Locale.ROOT)
            browseId.takeIf {
                it.startsWith("VL", ignoreCase = true) || pageType == MUSIC_PAGE_TYPE_PLAYLIST
            }
        }.orEmpty()
    }

    private fun findSearchContinuation(root: JSONObject): String {
        val legacy = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "nextContinuationData", legacy)
        legacy.firstNotNullOfOrNull { it.optString("continuation").takeIf(String::isNotBlank) }?.let { return it }
        val commands = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "continuationCommand", commands)
        return commands.firstNotNullOfOrNull { it.optString("token").takeIf(String::isNotBlank) }.orEmpty()
    }

    private suspend fun fallbackResults(query: String, languageCode: String): SearchResults {
        val songs = search(query, 20, languageCode)
        return SearchResults(topTrack = songs.firstOrNull(), songs = songs)
    }

    private fun clientPayload(languageCode: String): JSONObject {
        val locale = LevyraContentLocales.forLanguage(languageCode)
        return JSONObject()
            .put("clientName", "WEB_REMIX")
            .put("clientVersion", clientVersion)
            .put("hl", locale.hl)
            .put("gl", locale.gl)
            .put("platform", "DESKTOP")
    }

    private fun searchInnerTubeRaw(
        query: String,
        languageCode: String,
        params: String = "",
        continuation: String = ""
    ): JSONObject? {
        return resilienceClient.search(query, languageCode, params, continuation)
    }

    suspend fun home(
        queries: List<String> = LevyraContentLocales.forLanguage(LevyraLanguageCatalog.deviceDefault()).homeQueries,
        languageCode: String = LevyraLanguageCatalog.deviceDefault()
    ): List<Track> = withContext(Dispatchers.IO) {
        val results = queries.flatMap { query ->
            runCatching { search(query, 10, languageCode) }.getOrDefault(emptyList())
        }
        results.distinctBy { it.id }.take(40).also { items -> items.forEach { memory[it.id] = it } }
    }

    /** Real YouTube Music home feed parsed into titled sections, like the official app. */
    suspend fun homeFeed(languageCode: String = LevyraLanguageCatalog.deviceDefault()): List<HomeSection> = withContext(Dispatchers.IO) {
        val sections = runCatching { homeFeedInnerTube(languageCode) }.getOrDefault(emptyList())
        sections.forEach { section -> section.tracks.forEach { memory[it.id] = it } }
        sections
    }

    suspend fun homeAlbums(
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        limit: Int = 20,
        seeds: List<AlbumRecommendationSeed> = emptyList(),
        concurrency: Int = ALBUM_RECOMMENDATION_CONCURRENCY
    ): List<AlbumHit> = withContext(Dispatchers.IO) {
        val boundedLimit = limit.coerceIn(1, 40)
        val normalizedSeeds = seeds
            .asSequence()
            .map { seed -> seed.copy(query = seed.query.trim(), weight = seed.weight.coerceIn(0, 2_000)) }
            .filter { it.query.length >= 2 }
            .distinctBy { seed ->
                listOf(
                    albumRecommendationTextKey(seed.query),
                    albumRecommendationTextKey(seed.artist),
                    albumRecommendationTextKey(seed.album),
                    seed.moodTags.map(::albumRecommendationTextKey).sorted().joinToString("|")
                ).joinToString("|")
            }
            .take(MAX_ALBUM_RECOMMENDATION_SEEDS)
            .toList()
        val personalized = if (normalizedSeeds.isEmpty()) {
            emptyList()
        } else {
            val limiter = Semaphore(concurrency.coerceIn(1, ALBUM_RECOMMENDATION_CONCURRENCY))
            coroutineScope {
                normalizedSeeds.map { seed ->
                    async {
                        limiter.withPermit {
                            runCatching {
                                searchAlbumHits(seed.query, languageCode, ALBUM_RESULTS_PER_SEED)
                                    .mapIndexedNotNull { index, album ->
                                        val matchScore = levyraAlbumRecommendationMatchScore(album, seed)
                                        if (matchScore == LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE) null
                                        else ScoredAlbumRecommendation(
                                            album = album,
                                            score = seed.weight + matchScore - index * ALBUM_RESULT_RANK_PENALTY
                                        )
                                    }
                            }.getOrDefault(emptyList())
                        }
                    }
                }.awaitAll().flatten()
            }
                .groupBy { albumRecommendationDeduplicationKey(it.album) }
                .values
                .mapNotNull { group -> group.maxByOrNull { it.score } }
                .sortedWith(
                    compareByDescending<ScoredAlbumRecommendation> { it.score }
                        .thenBy { albumRecommendationTextKey(it.album.artist) }
                        .thenBy { albumRecommendationTextKey(it.album.title) }
                )
                .map { it.album }
        }
        val homeAlbums = runCatching { homeAlbumFeedInnerTube(languageCode) }.getOrDefault(emptyList())
        val baseAlbums = (personalized + homeAlbums)
            .asSequence()
            .filter { it.releaseType == ReleaseType.Album }
            .filter { isPlausibleYoutubeMusicAlbumTitle(it.title) && it.artist.isNotBlank() && it.thumbnailUrl.isNotBlank() }
            .filter { it.browseId.isNotBlank() || it.query.isNotBlank() }
            .distinctBy(::albumRecommendationDeduplicationKey)
            .toList()
        val fallbackAlbums = if (baseAlbums.size >= boundedLimit) {
            emptyList()
        } else {
            val limiter = Semaphore(concurrency.coerceIn(1, ALBUM_RECOMMENDATION_CONCURRENCY))
            coroutineScope {
                albumRecommendationQueries(languageCode).map { query ->
                    async {
                        limiter.withPermit {
                            runCatching {
                                searchAlbumHits(query, languageCode, ALBUM_RESULTS_PER_FALLBACK_QUERY)
                            }.getOrDefault(emptyList())
                        }
                    }
                }.awaitAll().flatten()
            }
        }
        (baseAlbums + fallbackAlbums)
            .asSequence()
            .filter { it.releaseType == ReleaseType.Album }
            .filter { isPlausibleYoutubeMusicAlbumTitle(it.title) && it.artist.isNotBlank() && it.thumbnailUrl.isNotBlank() }
            .filter { it.browseId.isNotBlank() || it.query.isNotBlank() }
            .distinctBy(::albumRecommendationDeduplicationKey)
            .take(boundedLimit)
            .toList()
    }

    suspend fun moodCategories(
        languageCode: String = LevyraLanguageCatalog.deviceDefault()
    ): List<YoutubeMusicMoodCategory> = withContext(Dispatchers.IO) {
        val root = requestMusicBrowseRoot(languageCode, "FEmusic_moods_and_genres") ?: return@withContext emptyList()
        parseMoodCategories(root)
    }

    suspend fun moodPlaylists(
        params: String,
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        limit: Int = 60
    ): List<YoutubeMusicMoodPlaylist> = withContext(Dispatchers.IO) {
        if (params.isBlank()) return@withContext emptyList()
        val root = requestMusicBrowseRoot(languageCode, "FEmusic_moods_and_genres_category", params) ?: return@withContext emptyList()
        parseMoodPlaylists(root).take(limit.coerceIn(1, 100))
    }

    suspend fun explore(
        languageCode: String = LevyraLanguageCatalog.deviceDefault()
    ): YoutubeMusicExplore = withContext(Dispatchers.IO) {
        val root = requestMusicBrowseRoot(languageCode, "FEmusic_explore") ?: return@withContext YoutubeMusicExplore()
        parseExplore(root)
    }

    suspend fun newReleases(
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        limit: Int = 40,
        preferredArtists: List<String> = emptyList()
    ): List<AlbumHit> = withContext(Dispatchers.IO) {
        val boundedLimit = limit.coerceIn(1, 80)
        val nativeExplore = try {
            explore(languageCode)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            YoutubeMusicExplore()
        }
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

    private fun searchMusicVideoTracks(
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
            .filter(::isPotentialYoutubeMusicVideo)
            .distinctBy { it.id }
            .take(limit)
            .toList()
    }

    private fun isPotentialYoutubeMusicVideo(track: Track): Boolean {
        if (track.id.length != 11 || track.videoUrl.isBlank()) return false
        if (track.title.isBlank() || track.artist.isBlank()) return false
        val type = track.videoType.uppercase(Locale.ROOT)
        return !type.contains("ATV") && !type.contains("PODCAST")
    }

    suspend fun newMusicVideos(
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        limit: Int = 12
    ): List<Track> = withContext(Dispatchers.IO) {
        val boundedLimit = limit.coerceIn(1, 40)
        val native = runCatching { explore(languageCode).newVideos }.getOrDefault(emptyList())
        val result = native.take(boundedLimit).ifEmpty {
            search("new music videos", boundedLimit, languageCode)
        }
        result.forEach { memory[it.id] = it }
        result
    }

    suspend fun exploreZone(
        zoneId: String,
        fallbackQuery: String,
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        limit: Int = 24
    ): List<Track> = withContext(Dispatchers.IO) {
        val boundedLimit = limit.coerceIn(1, 60)
        val native = runCatching { explore(languageCode) }.getOrDefault(YoutubeMusicExplore())
        val nativeTracks = when (zoneId) {
            "nuove-uscite" -> coroutineScope {
                native.newReleases.take(6).map { album ->
                    async {
                        runCatching { albumDetail(album, languageCode).tracks.take(4) }
                            .getOrDefault(emptyList())
                    }
                }.map { it.await() }.flatten()
            }
            "local-wave" -> native.trending + native.topSongs
            else -> emptyList()
        }
        if (nativeTracks.isNotEmpty()) {
            return@withContext nativeTracks.distinctBy { it.id }.take(boundedLimit).also { items ->
                items.forEach { memory[it.id] = it }
            }
        }

        val categories = (native.moodsAndGenres + runCatching { moodCategories(languageCode) }.getOrDefault(emptyList()))
            .distinctBy { it.params }
        val selectedCategory = categories.maxByOrNull { category ->
            moodCategoryScore(zoneId, fallbackQuery, category)
        }?.takeIf { moodCategoryScore(zoneId, fallbackQuery, it) > 0 }
        val moodTracks = selectedCategory?.let { category ->
            val shelves = runCatching { moodPlaylists(category.params, languageCode, 12) }.getOrDefault(emptyList())
            coroutineScope {
                shelves.take(4).map { shelf ->
                    async {
                        val playlistId = shelf.playlistId.ifBlank { shelf.browseId.removePrefix("VL") }
                        if (playlistId.isBlank()) emptyList() else {
                            runCatching { playlist(playlistId, languageCode, 18)?.tracks.orEmpty() }
                                .getOrDefault(emptyList())
                        }
                    }
                }.map { it.await() }.flatten()
            }
        }.orEmpty()
        val result = (moodTracks + native.topSongs + native.trending)
            .distinctBy { it.id }
            .take(boundedLimit)
            .ifEmpty { search(fallbackQuery, boundedLimit, languageCode) }
        result.forEach { memory[it.id] = it }
        result
    }

    suspend fun playlist(
        playlistId: String,
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        limit: Int = 100
    ): YoutubeMusicPlaylistDetail? = withContext(Dispatchers.IO) {
        val cleanPlaylistId = playlistId.trim().removePrefix("VL")
        if (cleanPlaylistId.isBlank()) return@withContext null
        val boundedLimit = limit.coerceIn(1, 300)
        val browseId = "VL$cleanPlaylistId"
        val initial = requestMusicBrowseRoot(languageCode, browseId) ?: return@withContext null
        val tracks = LinkedHashMap<String, Track>()
        parsePlaylistTrackRenderers(playlistShelfRenderers(initial), cleanPlaylistId)
            .forEach { tracks.putIfAbsent(it.id, it) }
        var continuation = findPlaylistContinuation(initial)
        val requestedContinuations = mutableSetOf<String>()
        var page = 0
        while (tracks.size < boundedLimit && continuation.isNotBlank() && page < MAX_BROWSE_CONTINUATIONS) {
            val requestedToken = continuation
            if (!requestedContinuations.add(requestedToken)) {
                continuation = ""
                break
            }
            val next = requestMusicBrowseRoot(languageCode, "", continuation = requestedToken) ?: break
            val pageRenderers = playlistShelfRenderers(next)
            parsePlaylistTrackRenderers(pageRenderers, cleanPlaylistId)
                .forEach { tracks.putIfAbsent(it.id, it) }
            val nextToken = findPlaylistContinuation(next)
            continuation = nextToken.takeUnless { it in requestedContinuations }.orEmpty()
            page += 1
            if (pageRenderers.isEmpty()) {
                continuation = ""
                break
            }
        }
        val header = parsePlaylistHeader(initial, cleanPlaylistId)
        val result = header.copy(tracks = tracks.values.take(boundedLimit), continuation = continuation)
        result.tracks.forEach { memory[it.id] = it }
        result
    }

    suspend fun playlistRadio(
        playlistId: String,
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        limit: Int = 50,
        shuffle: Boolean = false
    ): List<Track> = withContext(Dispatchers.IO) {
        val cleanPlaylistId = playlistId.trim().removePrefix("VL")
        if (cleanPlaylistId.isBlank()) return@withContext emptyList()
        val watch = runCatching {
            watchRepository.getWatchPlaylistAdvanced(
                playlistId = cleanPlaylistId,
                languageCode = languageCode,
                limit = limit,
                radio = !shuffle,
                shuffle = shuffle
            )
        }.getOrNull() ?: return@withContext emptyList()
        watch.tracks.map { item ->
            watchTrackToTrack(
                item = item,
                seed = Track(
                    id = item.videoId,
                    title = item.title,
                    artist = item.artists.joinToString(", ") { it.name },
                    album = item.albumTitle,
                    durationMs = item.durationMs,
                    streamUrl = "",
                    videoUrl = "https://www.youtube.com/watch?v=${item.videoId}",
                    thumbnailUrl = item.thumbnailUrl,
                    largeThumbnailUrl = upgradeThumbnail(item.thumbnailUrl),
                    source = "YouTube Music Playlist",
                    moodTags = setOf("music"),
                    energy = 60,
                    vocal = 50,
                    replayScore = 80,
                    cacheScore = 70,
                    accentStart = 0xFF20E7FF.toInt(),
                    accentEnd = 0xFF8E57FF.toInt()
                ),
                query = "playlist $cleanPlaylistId"
            )
        }.distinctBy { it.id }
    }

    suspend fun albumDetail(
        album: AlbumHit,
        languageCode: String = LevyraLanguageCatalog.deviceDefault()
    ): AlbumDetail = withContext(Dispatchers.IO) {
        val initial = loadAlbumDetailOnce(album, languageCode)
        if (initial.tracks.isNotEmpty()) return@withContext initial

        val attemptedBrowseIds = setOf(album.browseId, initial.album.browseId)
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSet()
        val recovery = findAlbumRecoveryCandidate(
            album = album,
            languageCode = languageCode,
            excludedBrowseIds = attemptedBrowseIds
        ) ?: return@withContext initial

        val recovered = loadAlbumDetailOnce(recovery, languageCode)
        if (recovered.tracks.isNotEmpty()) recovered else initial
    }

    private suspend fun loadAlbumDetailOnce(album: AlbumHit, languageCode: String): AlbumDetail {
        val resolved = resolveAlbumHit(album, languageCode)
        val root = resolved.browseId.takeIf { it.isNotBlank() }
            ?.let { requestMusicBrowseRoot(languageCode, it) }
        val headerAlbum = root?.let { parseAlbumHeader(it, resolved) } ?: resolved
        val cover = headerAlbum.thumbnailUrl.ifBlank { resolved.thumbnailUrl }
        val canonicalAudioPlaylistId = sequenceOf(
            root?.let(::extractAudioPlaylistId).orEmpty(),
            headerAlbum.audioPlaylistId,
            resolved.audioPlaylistId,
            album.audioPlaylistId
        )
            .map { it.trim().removePrefix("VL") }
            .firstOrNull { it.startsWith("OLA") }
            .orEmpty()
        val playlistTracks = if (canonicalAudioPlaylistId.isNotBlank()) {
            try {
                playlist(canonicalAudioPlaylistId, languageCode, 60)?.tracks.orEmpty()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                emptyList()
            }
        } else {
            emptyList()
        }
        val shelfTracks = if (playlistTracks.isEmpty()) {
            root?.let { parseAlbumTracks(it, headerAlbum.copy(thumbnailUrl = cover)) }.orEmpty()
        } else {
            emptyList()
        }
        val finalTracks = (if (playlistTracks.isNotEmpty()) playlistTracks else shelfTracks)
            .distinctBy { it.id.ifBlank { "${it.title.lowercase()}|${it.artist.lowercase()}" } }
            .take(60)
        val finalArtist = headerAlbum.artist.cleanAlbumArtistLabel()
            .ifBlank {
                finalTracks.firstNotNullOfOrNull {
                    it.artist.cleanAlbumArtistLabel().takeIf(String::isNotBlank)
                }.orEmpty()
            }
            .ifBlank { resolved.artist.cleanAlbumArtistLabel() }
        val finalAlbum = headerAlbum.copy(
            artist = finalArtist,
            thumbnailUrl = cover,
            query = "${headerAlbum.title} $finalArtist".trim(),
            browseId = headerAlbum.browseId.ifBlank { resolved.browseId },
            audioPlaylistId = canonicalAudioPlaylistId,
            explicit = root?.toString()?.contains("MUSIC_ITEM_BADGE_EXPLICIT") == true || headerAlbum.explicit
        )
        val enrichedTracks = finalTracks.map { track ->
            track.copy(
                album = finalAlbum.title,
                albumBrowseId = finalAlbum.browseId,
                year = track.year.ifBlank { finalAlbum.year },
                explicit = track.explicit || finalAlbum.explicit,
                thumbnailUrl = finalAlbum.thumbnailUrl.ifBlank { track.thumbnailUrl },
                largeThumbnailUrl = finalAlbum.thumbnailUrl.ifBlank {
                    track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
                }
            )
        }
        enrichedTracks.forEach { memory[it.id] = it }
        val description = root?.let { parseAlbumDescription(it) }.orEmpty()
        return AlbumDetail(
            album = finalAlbum,
            description = description,
            tracks = enrichedTracks,
            otherVersions = root?.let { parseOtherAlbumVersions(it, finalAlbum) }.orEmpty(),
            trackCount = enrichedTracks.size,
            durationMs = enrichedTracks.sumOf { it.durationMs }
        )
    }

    private fun findAlbumRecoveryCandidate(
        album: AlbumHit,
        languageCode: String,
        excludedBrowseIds: Set<String>
    ): AlbumHit? {
        if (!isPlausibleYoutubeMusicAlbumTitle(album.title)) return null
        val query = album.query.ifBlank { "${album.title} ${album.artist}" }.trim()
        if (query.length < 2) return null
        val candidates = searchAlbumHits(query, languageCode, 12)
        return selectAlbumRecoveryCandidate(album, candidates, excludedBrowseIds)
    }

    suspend fun resolveAlbumDescription(
        detail: AlbumDetail,
        languageCode: String = LevyraLanguageCatalog.deviceDefault()
    ): String = albumDescriptionRepository.resolve(
        album = detail.album,
        languageCode = languageCode,
        youtubeDescription = detail.description,
        trackCount = detail.trackCount
    )

    private fun moodCategoryScore(
        zoneId: String,
        fallbackQuery: String,
        category: YoutubeMusicMoodCategory
    ): Int {
        val candidate = normalizedWords("${category.section} ${category.title}")
        if (candidate.isEmpty()) return 0
        val target = normalizedWords("$zoneId $fallbackQuery ${zoneKeywords(zoneId)}")
        val shared = candidate.intersect(target).size
        val phraseBonus = zoneKeywords(zoneId)
            .split('|')
            .map(String::trim)
            .filter(String::isNotBlank)
            .count { keyword -> normalizeSearchText("${category.section} ${category.title}").contains(normalizeSearchText(keyword)) }
        return shared * 20 + phraseBonus * 90
    }

    private fun zoneKeywords(zoneId: String): String = when (zoneId) {
        "rap-drill" -> "rap|hip hop|hip-hop|drill|trap"
        "elettronica" -> "electronic|elettronica|dance|edm|house|techno"
        "pop-global" -> "pop|global hits|hit"
        "rnb-soul" -> "r&b|rnb|soul|neo soul"
        "rock-alt" -> "rock|alternative|indie|metal"
        "latino" -> "latin|latino|reggaeton|musica latina|música latina"
        "lofi-chill" -> "lofi|lo-fi|chill|focus|relax|sleep"
        "anime-jpop" -> "anime|j-pop|jpop|japanese|soundtrack"
        "local-wave" -> "italia|italian|italiano|musica italiana|local"
        "nuove-uscite" -> "new releases|nuove uscite|novità|new music"
        else -> zoneId
    }

    private fun normalizedWords(value: String): Set<String> = normalizeSearchText(value)
        .split(' ')
        .filter { it.length >= 2 }
        .toSet()

    private fun normalizeSearchText(value: String): String = value
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{M}\\p{N}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun parseMoodCategories(root: JSONObject): List<YoutubeMusicMoodCategory> {
        val result = LinkedHashMap<String, YoutubeMusicMoodCategory>()
        val grids = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "gridRenderer", grids)
        grids.forEach { grid ->
            val section = grid.optJSONObject("header")
                ?.optJSONObject("gridHeaderRenderer")
                ?.optJSONObject("title")
                ?.optJSONArray("runs")
                ?.joinText()
                .orEmpty()
                .trim()
            val buttons = mutableListOf<JSONObject>()
            collectObjectsByKey(grid.optJSONArray("items"), "musicNavigationButtonRenderer", buttons)
            buttons.forEach { button ->
                parseMoodCategoryButton(button, section)?.let { category ->
                    result.putIfAbsent(category.params, category)
                }
            }
        }
        if (result.isEmpty()) {
            val buttons = mutableListOf<JSONObject>()
            collectObjectsByKey(root, "musicNavigationButtonRenderer", buttons)
            buttons.forEach { button ->
                parseMoodCategoryButton(button, "")?.let { category -> result.putIfAbsent(category.params, category) }
            }
        }
        return result.values.toList()
    }

    private fun parseMoodCategoryButton(button: JSONObject, section: String): YoutubeMusicMoodCategory? {
        val title = button.optJSONObject("buttonText")?.optJSONArray("runs")?.joinText().orEmpty()
            .ifBlank { button.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty() }
            .trim()
        val endpoint = button.optJSONObject("clickCommand")?.optJSONObject("browseEndpoint")
            ?: button.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
            ?: return null
        val params = endpoint.optString("params").trim()
        if (title.isBlank() || params.isBlank()) return null
        return YoutubeMusicMoodCategory(title = title, params = params, section = section)
    }

    private fun parseMoodPlaylists(root: JSONObject): List<YoutubeMusicMoodPlaylist> {
        val cards = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicTwoRowItemRenderer", cards)
        val result = LinkedHashMap<String, YoutubeMusicMoodPlaylist>()
        cards.forEach { card ->
            val title = card.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().trim()
            if (title.isBlank()) return@forEach
            val browseEndpoint = card.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
            val watchEndpoint = card.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")
            val browseId = browseEndpoint?.optString("browseId").orEmpty()
            val playlistId = watchEndpoint?.optString("playlistId").orEmpty()
                .ifBlank { browseId.removePrefix("VL").takeIf { browseId.startsWith("VL") }.orEmpty() }
            if (playlistId.isBlank() && browseId.isBlank()) return@forEach
            val subtitle = card.optJSONObject("subtitle")?.optJSONArray("runs")?.joinText().orEmpty().trim()
            val key = playlistId.ifBlank { browseId }
            result.putIfAbsent(
                key,
                YoutubeMusicMoodPlaylist(
                    title = title,
                    playlistId = playlistId,
                    browseId = browseId,
                    description = subtitle,
                    thumbnailUrl = upgradeThumbnail(findBestThumbnail(card))
                )
            )
        }
        return result.values.toList()
    }

    private fun parseExplore(root: JSONObject): YoutubeMusicExplore {
        val newReleases = LinkedHashMap<String, AlbumHit>()
        val topSongs = LinkedHashMap<String, Track>()
        val trending = LinkedHashMap<String, Track>()
        val newVideos = LinkedHashMap<String, Track>()
        val moods = LinkedHashMap<String, YoutubeMusicMoodCategory>()
        val shelves = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicCarouselShelfRenderer", shelves)
        shelves.forEach { shelf ->
            val header = shelf.optJSONObject("header")
            val browseIds = mutableListOf<JSONObject>()
            collectObjectsByKey(header, "browseEndpoint", browseIds)
            val navigationId = browseIds.firstNotNullOfOrNull { endpoint -> endpoint.optString("browseId").takeIf { it.isNotBlank() } }.orEmpty()
            val contents = shelf.optJSONArray("contents") ?: JSONArray()
            when {
                navigationId == "FEmusic_new_releases_albums" -> {
                    for (index in 0 until contents.length()) {
                        parseAlbumFromExploreItem(contents.optJSONObject(index))?.let { album ->
                            newReleases.putIfAbsent(album.browseId.ifBlank { "${album.title}|${album.artist}" }, album)
                        }
                    }
                }
                navigationId == "FEmusic_moods_and_genres" -> {
                    val buttons = mutableListOf<JSONObject>()
                    collectObjectsByKey(contents, "musicNavigationButtonRenderer", buttons)
                    buttons.forEach { button ->
                        parseMoodCategoryButton(button, shelfTitle(shelf))?.let { moods.putIfAbsent(it.params, it) }
                    }
                }
                navigationId == "FEmusic_new_releases_videos" -> {
                    for (index in 0 until contents.length()) {
                        parseCarouselItem(contents.optJSONObject(index) ?: continue)?.let { newVideos.putIfAbsent(it.id, it) }
                    }
                }
                navigationId.startsWith("VLPL") -> {
                    for (index in 0 until contents.length()) {
                        parseCarouselItem(contents.optJSONObject(index) ?: continue)?.let { topSongs.putIfAbsent(it.id, it) }
                    }
                }
                navigationId.startsWith("VLOLA") -> {
                    for (index in 0 until contents.length()) {
                        parseCarouselItem(contents.optJSONObject(index) ?: continue)?.let { trending.putIfAbsent(it.id, it) }
                    }
                }
            }
        }
        if (moods.isEmpty()) parseMoodCategories(root).forEach { moods.putIfAbsent(it.params, it) }
        val result = YoutubeMusicExplore(
            newReleases = newReleases.values.toList(),
            topSongs = topSongs.values.toList(),
            moodsAndGenres = moods.values.toList(),
            trending = trending.values.toList(),
            newVideos = newVideos.values.toList()
        )
        (result.topSongs + result.trending + result.newVideos).forEach { memory[it.id] = it }
        return result
    }

    private fun parseAlbumFromExploreItem(item: JSONObject?): AlbumHit? {
        item ?: return null
        item.optJSONObject("musicResponsiveListItemRenderer")?.let { parseAlbumHit(it)?.let { album -> return album } }
        val card = item.optJSONObject("musicTwoRowItemRenderer") ?: return null
        val title = card.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().trim()
        if (!isPlausibleYoutubeMusicAlbumTitle(title)) return null
        val subtitle = card.optJSONObject("subtitle")?.optJSONArray("runs")?.joinText().orEmpty().trim()
        val tokens = subtitle.split(" • ", " · ", " - ").map { it.trim() }.filter { it.isNotBlank() }
        val releaseType = tokens.firstNotNullOfOrNull { token ->
            levyraReleaseType(token).takeUnless { it == ReleaseType.Unknown }
        } ?: ReleaseType.Unknown
        val artist = tokens.firstOrNull { token ->
            levyraReleaseType(token) == ReleaseType.Unknown && !token.matches(Regex("""\b(?:19|20)\d{2}\b"""))
        }.orEmpty()
        val year = tokens.firstNotNullOfOrNull { Regex("\\b(?:19|20)\\d{2}\\b").find(it)?.value }.orEmpty()
        val browseEndpoint = card.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
        val browseId = browseEndpoint?.optString("browseId").orEmpty()
        if (browseId.isBlank()) return null
        val watchEndpoints = mutableListOf<JSONObject>()
        collectObjectsByKey(card, "watchEndpoint", watchEndpoints)
        val audioPlaylistId = watchEndpoints.firstNotNullOfOrNull { endpoint -> endpoint.optString("playlistId").takeIf { it.isNotBlank() } }.orEmpty()
        val artistReference = extractYoutubeMusicArtistReference(card, artist)
        val resolvedArtist = artistReference?.name.orEmpty().ifBlank { artist }.ifBlank { "YouTube Music" }
        return AlbumHit(
            title = title.cleanLabel(),
            artist = resolvedArtist.cleanLabel(),
            year = year,
            thumbnailUrl = upgradeThumbnail(findBestThumbnail(card)),
            query = "$title $resolvedArtist".trim(),
            browseId = browseId,
            artistBrowseId = artistReference?.browseId.orEmpty(),
            audioPlaylistId = audioPlaylistId,
            explicit = card.toString().contains("MUSIC_ITEM_BADGE_EXPLICIT"),
            releaseType = releaseType
        )
    }

    internal fun parsePlaylistTracks(root: JSONObject, playlistId: String): List<Track> {
        return parsePlaylistTrackRenderers(playlistShelfRenderers(root), playlistId)
    }

    private fun parsePlaylistTrackRenderers(renderers: List<JSONObject>, playlistId: String): List<Track> {
        return renderers.mapNotNull { renderer ->
            val track = parseMusicRenderer(renderer, "playlist $playlistId") ?: return@mapNotNull null
            val albumReference = extractYoutubeMusicAlbumReference(renderer)
            val artistReferences = extractYoutubeMusicArtistReferences(renderer, track.artist)
            val trackNumber = extractPlaylistTrackNumber(renderer)
            track.copy(
                album = albumReference.first.ifBlank { track.album },
                albumBrowseId = albumReference.second,
                artistBrowseIds = artistReferences.map { it.browseId },
                trackNumber = trackNumber,
                explicit = renderer.toString().contains("MUSIC_ITEM_BADGE_EXPLICIT")
            )
        }.distinctBy { it.id }
    }

    private fun parsePlaylistHeader(root: JSONObject, playlistId: String): YoutubeMusicPlaylistDetail {
        val headers = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicResponsiveHeaderRenderer", headers)
        collectObjectsByKey(root, "musicDetailHeaderRenderer", headers)
        collectObjectsByKey(root, "musicEditablePlaylistDetailHeaderRenderer", headers)
        val header = headers.firstOrNull()
        val title = header?.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty()
            .ifBlank { header?.let { findStringUnderKey(it, "simpleText") }.orEmpty() }
            .ifBlank { "YouTube Music Playlist" }
        val descriptionNodes = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "description", descriptionNodes)
        val description = descriptionNodes.firstNotNullOfOrNull { node ->
            node.optJSONArray("runs")?.joinText()?.trim()?.takeIf { it.isNotBlank() }
                ?: node.optString("simpleText").trim().takeIf { it.isNotBlank() }
        }.orEmpty()
        val author = header?.let { extractYoutubeMusicArtistReference(it, "")?.name }.orEmpty()
        return YoutubeMusicPlaylistDetail(
            playlistId = playlistId,
            title = title.cleanLabel(),
            description = description,
            author = author,
            thumbnailUrl = header?.let { upgradeThumbnail(findBestThumbnail(it)) }.orEmpty(),
            tracks = emptyList()
        )
    }

    private fun playlistShelfRenderers(root: JSONObject): List<JSONObject> =
        YoutubeMusicPlaylistPageParser.renderers(root)

    private fun findPlaylistContinuation(root: JSONObject): String =
        YoutubeMusicPlaylistPageParser.continuation(root)

    private fun extractYoutubeMusicAlbumReference(value: Any?): Pair<String, String> {
        val runArrays = mutableListOf<JSONArray>()
        collectArraysByKey(value, "runs", runArrays)
        runArrays.forEach { runs ->
            for (index in 0 until runs.length()) {
                val run = runs.optJSONObject(index) ?: continue
                val endpoint = run.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint") ?: continue
                val browseId = endpoint.optString("browseId")
                val pageType = endpoint.optJSONObject("browseEndpointContextSupportedConfigs")
                    ?.optJSONObject("browseEndpointContextMusicConfig")
                    ?.optString("pageType")
                    .orEmpty()
                if (browseId.startsWith("MPRE") || pageType.contains("ALBUM")) {
                    return run.optString("text").cleanLabel() to browseId
                }
            }
        }
        val endpoints = mutableListOf<JSONObject>()
        collectObjectsByKey(value, "browseEndpoint", endpoints)
        endpoints.forEach { endpoint ->
            val browseId = endpoint.optString("browseId")
            val pageType = endpoint.optJSONObject("browseEndpointContextSupportedConfigs")
                ?.optJSONObject("browseEndpointContextMusicConfig")
                ?.optString("pageType")
                .orEmpty()
            if (browseId.startsWith("MPRE") || pageType.contains("ALBUM")) return "" to browseId
        }
        return "" to ""
    }

    private fun extractPlaylistTrackNumber(renderer: JSONObject): Int {
        val columns = renderer.optJSONArray("fixedColumns") ?: return 0
        for (index in 0 until columns.length()) {
            val text = columns.optJSONObject(index)
                ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
                ?.joinText()
                .orEmpty()
                .trim()
            val number = text.toIntOrNull()
            if (number != null && number in 1..999) return number
        }
        return 0
    }


    private fun requestMusicHomeRoot(languageCode: String): JSONObject? = requestMusicBrowseRoot(languageCode, "FEmusic_home")

    private fun requestMusicBrowseRoot(
    languageCode: String,
    browseId: String,
    params: String = "",
    continuation: String = ""
): JSONObject? {
    return resilienceClient.browse(languageCode, browseId, params, continuation)
}

    private fun homeFeedInnerTube(languageCode: String): List<HomeSection> {
        val root = requestMusicHomeRoot(languageCode) ?: return emptyList()
        val shelves = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicCarouselShelfRenderer", shelves)
        val sections = mutableListOf<HomeSection>()
        shelves.forEach { shelf ->
            val title = shelfTitle(shelf).ifBlank { "Per te" }
            val contents = shelf.optJSONArray("contents") ?: JSONArray()
            val tracks = LinkedHashMap<String, Track>()
            for (i in 0 until contents.length()) {
                val item = contents.optJSONObject(i) ?: continue
                val track = parseCarouselItem(item)
                if (track != null && !tracks.containsKey(track.id)) tracks[track.id] = track
            }
            if (tracks.size >= 3) sections += HomeSection(title, tracks.values.take(20))
        }
        return sections.take(10)
    }

    private fun homeAlbumFeedInnerTube(languageCode: String): List<AlbumHit> {
        val root = requestMusicHomeRoot(languageCode) ?: return emptyList()
        val shelves = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicCarouselShelfRenderer", shelves)
        val albums = LinkedHashMap<String, AlbumHit>()
        shelves.forEach { shelf ->
            val contents = shelf.optJSONArray("contents") ?: JSONArray()
            for (i in 0 until contents.length()) {
                val item = contents.optJSONObject(i) ?: continue
                val album = parseCarouselAlbumHit(item) ?: continue
                if (album.releaseType != ReleaseType.Album) continue
                val key = "${album.title.lowercase()}|${album.artist.lowercase()}"
                if (!albums.containsKey(key)) albums[key] = album
            }
        }
        return albums.values.take(12).toList()
    }

    private fun shelfTitle(shelf: JSONObject): String {
        return shelf.optJSONObject("header")
            ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
            ?.optJSONObject("title")
            ?.optJSONArray("runs")
            ?.joinText()
            .orEmpty()
            .trim()
    }

    private companion object {
        const val MAX_BROWSE_CONTINUATIONS = 8
    }

    private val excludedTypes = setOf(
        "playlist", "artist", "ep", "podcast", "episode", "channel", "profile", "mix",
        "artista", "canale", "profilo", "canción", "cancion", "artiste", "künstler", "kunstler",
        "artiest", "artysta", "artis", "canal", "chaîne", "kanal",
        "فنان", "قناة", "ملف شخصي", "قائمة تشغيل", "歌手", "频道", "个人资料", "播放列表",
        "アーティスト", "チャンネル", "プロフィール", "プレイリスト", "再生リスト",
        "아티스트", "채널", "프로필", "재생목록", "플레이리スト",
        "कलाकार", "चैनल", "प्रोफ़ाइल", "प्लेलिस्ट",
        "saluran", "daftar putar", "nghệ sĩ", "kênh", "hồ sơ", "danh sách phát",
        "ศิลปิน", "ช่อง", "โปรไฟล์", "เพลย์ลิสต์",
        "mang-aawit", "talaan ng tugtugin",
        "אמן", "אמנית", "ערוץ", "פרופיל", "פלייליסט", "רשימת השמעה"
    ) + LEVYRA_LOCALIZED_ALBUM_LABELS

    private fun albumRecommendationQueries(languageCode: String): List<String> {
        return when (LevyraLanguageCatalog.normalize(languageCode)) {
            "it" -> listOf(
                "nuovi album italiani",
                "album italiani",
                "album pop italiani",
                "album rap italiani",
                "album indie italiani"
            )
            "es" -> listOf("nuevos álbumes", "álbumes populares", "álbumes pop", "álbumes rap")
            "fr" -> listOf("nouveaux albums", "albums populaires", "albums pop", "albums rap")
            "de" -> listOf("neue alben", "beliebte alben", "pop alben", "rap alben")
            "pt" -> listOf("novos álbuns", "álbuns populares", "álbuns pop", "álbuns rap")
            "ar" -> listOf("ألبومات عربية جديدة", "ألبومات عربية رائجة", "ألبومات بوب عربية", "ألبومات راب عربية")
            "zh" -> listOf("华语新专辑", "华语热门专辑", "华语流行专辑", "中文说唱专辑")
            "ja" -> listOf("邦楽 新着アルバム", "人気 邦楽アルバム", "J-POP アルバム", "日本語ラップ アルバム")
            "ko" -> listOf("국내 신작 앨범", "인기 한국 앨범", "K-POP 앨범", "한국 힙합 앨범")
            "hi" -> listOf("नए भारतीय एल्बम", "लोकप्रिय हिंदी एल्बम", "हिंदी पॉप एल्बम", "भारतीय रैप एल्बम")
            "id" -> listOf("album Indonesia terbaru", "album Indonesia populer", "album pop Indonesia", "album rap Indonesia")
            "vi" -> listOf("album Việt mới", "album Việt phổ biến", "album V-pop", "album rap Việt")
            "th" -> listOf("อัลบั้มไทยใหม่", "อัลบั้มไทยยอดนิยม", "อัลบั้มป๊อปไทย", "อัลบั้มแรปไทย")
            "fil" -> listOf("mga bagong OPM album", "mga sikat na album sa Pilipinas", "mga P-pop album", "mga Pinoy rap album")
            "he" -> listOf("אלבומים ישראליים חדשים", "אלבומים ישראליים פופולריים", "אלבומי פופ ישראלי", "אלבומי ראפ ישראלי")
            else -> listOf("new albums", "popular albums", "pop albums", "rap albums", "indie albums")
        }
    }

    private fun searchAlbumHits(query: String, languageCode: String, limit: Int): List<AlbumHit> {
        val root = searchInnerTubeRaw(query, languageCode) ?: return emptyList()
        val albums = LinkedHashMap<String, AlbumHit>()
        val renderers = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicResponsiveListItemRenderer", renderers)
        renderers.forEach { renderer ->
            val album = parseAlbumHit(renderer) ?: return@forEach
            val key = "${album.title.lowercase()}|${album.artist.lowercase()}"
            if (!albums.containsKey(key)) albums[key] = album.copy(query = "${album.title} ${album.artist}")
        }
        val twoRows = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicTwoRowItemRenderer", twoRows)
        twoRows.forEach { renderer ->
            val album = parseTwoRowAlbumHit(renderer) ?: return@forEach
            val key = "${album.title.lowercase()}|${album.artist.lowercase()}"
            if (!albums.containsKey(key)) albums[key] = album.copy(query = "${album.title} ${album.artist}")
        }
        return albums.values.take(limit).toList()
    }

    private fun parseCarouselAlbumHit(item: JSONObject): AlbumHit? {
        item.optJSONObject("musicResponsiveListItemRenderer")?.let { renderer -> return parseAlbumHit(renderer) }
        val two = item.optJSONObject("musicTwoRowItemRenderer") ?: return null
        return parseTwoRowAlbumHit(two)
    }

    private fun parseTwoRowAlbumHit(two: JSONObject): AlbumHit? {
        val title = two.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().trim()
        if (!isPlausibleYoutubeMusicAlbumTitle(title)) return null
        val subtitle = two.optJSONObject("subtitle")?.optJSONArray("runs")?.joinText().orEmpty()
        val tokens = subtitle.split(" • ", " · ", " - ").map { it.trim() }.filter { it.isNotBlank() }
        val kind = tokens.firstOrNull().orEmpty()
        val releaseType = levyraReleaseType(kind)
        if (releaseType != ReleaseType.Album) return null
        val artist = tokens.drop(1).firstOrNull { isAlbumArtistToken(it) } ?: return null
        val year = tokens.firstNotNullOfOrNull { Regex("""\b(19|20)\d{2}\b""").find(it)?.value }.orEmpty()
        val thumbnail = findBestThumbnail(two)
        if (thumbnail.isBlank()) return null
        val artistReference = extractYoutubeMusicArtistReference(two, artist)
        val resolvedArtist = artistReference?.name?.ifBlank { artist }.orEmpty().ifBlank { artist }
        return AlbumHit(
            title = title.cleanLabel(),
            artist = resolvedArtist.cleanLabel(),
            year = year,
            thumbnailUrl = upgradeThumbnail(thumbnail),
            query = "$title $resolvedArtist",
            browseId = extractAlbumBrowseId(two),
            artistBrowseId = artistReference?.browseId.orEmpty(),
            releaseType = releaseType
        )
    }

    private fun parseAlbumHit(renderer: JSONObject): AlbumHit? {
        val lines = extractFlexLines(renderer)
        val title = lines.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        if (!isPlausibleYoutubeMusicAlbumTitle(title)) return null
        val tokens = lines.drop(1).flatMap { it.split(" • ", " · ", " - ") }.map { it.trim() }.filter { it.isNotBlank() }
        val kind = tokens.firstOrNull().orEmpty()
        val releaseType = levyraReleaseType(kind)
        if (releaseType != ReleaseType.Album) return null
        val artist = tokens.drop(1).firstOrNull { isAlbumArtistToken(it) } ?: return null
        val year = tokens.firstNotNullOfOrNull { Regex("""\b(19|20)\d{2}\b""").find(it)?.value }.orEmpty()
        val thumbnail = findBestThumbnail(renderer)
        if (thumbnail.isBlank()) return null
        val artistReference = extractYoutubeMusicArtistReference(renderer, artist)
        val resolvedArtist = artistReference?.name?.ifBlank { artist }.orEmpty().ifBlank { artist }
        return AlbumHit(
            title = title.cleanLabel(),
            artist = resolvedArtist.cleanLabel(),
            year = year,
            thumbnailUrl = upgradeThumbnail(thumbnail),
            query = "$title $resolvedArtist",
            browseId = extractAlbumBrowseId(renderer),
            artistBrowseId = artistReference?.browseId.orEmpty(),
            releaseType = releaseType
        )
    }

    private fun isAlbumLabel(token: String): Boolean = levyraIsAlbumLabel(token)

    private fun isAlbumArtistToken(token: String): Boolean {
        val normalized = token.cleanAlbumArtistLabel().lowercase()
        if (normalized.isBlank()) return false
        if (normalized in typeLabels) return false
        if (normalized.matches(Regex("\\d{4}"))) return false
        if (normalized.matches(Regex("\\d+:\\d{2}"))) return false
        if (normalized.contains("song") || normalized.contains("brani") || normalized.contains("songs")) return false
        return true
    }

    private fun albumArtworkIdentityKey(url: String): String = url.trim()
        .lowercase(Locale.ROOT)
        .substringBefore('?')
        .replace(Regex("""=w\d+-h\d+.*$"""), "")
        .replace(Regex("""=s\d+.*$"""), "")

    private fun resolveAlbumHit(album: AlbumHit, languageCode: String): AlbumHit {
        val validInputTitle = isPlausibleYoutubeMusicAlbumTitle(album.title)
        if (album.browseId.isNotBlank() && validInputTitle) return album

        val query = when {
            validInputTitle -> album.query.ifBlank { "${album.title} ${album.artist}" }
            album.artist.isNotBlank() -> "${album.artist} album"
            else -> album.query
        }.trim()
        if (query.length < 2) {
            return album.copy(title = album.title.takeIf(::isPlausibleYoutubeMusicAlbumTitle).orEmpty())
        }

        val candidates = searchAlbumHits(query, languageCode, 12)
            .filter { it.browseId.isNotBlank() && isPlausibleYoutubeMusicAlbumTitle(it.title) }
        val titleKey = albumRecommendationTextKey(album.title)
        val artistKey = albumRecommendationTextKey(album.artist)
        val artworkKey = albumArtworkIdentityKey(album.thumbnailUrl)
        val found = candidates.firstOrNull {
            validInputTitle &&
                albumRecommendationTextKey(it.title) == titleKey &&
                albumRecommendationTextKey(it.artist) == artistKey
        } ?: candidates.firstOrNull {
            artworkKey.isNotBlank() &&
                albumArtworkIdentityKey(it.thumbnailUrl) == artworkKey &&
                (artistKey.isBlank() || albumRecommendationTextKey(it.artist) == artistKey)
        } ?: candidates.firstOrNull {
            artistKey.isNotBlank() && albumRecommendationTextKey(it.artist) == artistKey
        } ?: candidates.firstOrNull()
        return found?.let {
            album.copy(
                title = it.title,
                artist = it.artist.ifBlank { album.artist },
                year = it.year.ifBlank { album.year },
                thumbnailUrl = it.thumbnailUrl.ifBlank { album.thumbnailUrl },
                query = it.query.ifBlank { "${it.title} ${it.artist}".trim() },
                browseId = it.browseId,
                artistBrowseId = it.artistBrowseId.ifBlank { album.artistBrowseId }
            )
        } ?: album.copy(title = album.title.takeIf(::isPlausibleYoutubeMusicAlbumTitle).orEmpty())
    }

    private fun parseAlbumHeader(root: JSONObject, fallback: AlbumHit): AlbumHit {
        val detailHeaders = mutableListOf<JSONObject>()
        val responsiveHeaders = mutableListOf<JSONObject>()
        val editableHeaders = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicDetailHeaderRenderer", detailHeaders)
        collectObjectsByKey(root, "musicResponsiveHeaderRenderer", responsiveHeaders)
        collectObjectsByKey(root, "musicEditablePlaylistDetailHeaderRenderer", editableHeaders)
        val headers = detailHeaders + responsiveHeaders + editableHeaders

        fun titleOf(candidate: JSONObject?): String = candidate
            ?.optJSONObject("title")
            ?.optJSONArray("runs")
            ?.joinText()
            .orEmpty()
            .cleanLabel()

        val fallbackTitle = fallback.title.cleanLabel()
            .takeIf(::isPlausibleYoutubeMusicAlbumTitle)
            .orEmpty()
        val header = headers.firstOrNull { candidate ->
            isPlausibleYoutubeMusicAlbumTitle(titleOf(candidate))
        } ?: headers.firstOrNull()
        val title = titleOf(header)
            .takeIf(::isPlausibleYoutubeMusicAlbumTitle)
            .orEmpty()
            .ifBlank { fallbackTitle }
            .ifBlank { "Album" }
        val subtitles = listOf(
            header?.optJSONObject("subtitle")?.optJSONArray("runs")?.joinText().orEmpty(),
            header?.optJSONObject("secondSubtitle")?.optJSONArray("runs")?.joinText().orEmpty()
        ).filter(String::isNotBlank).joinToString(" • ")
        val tokens = subtitles.split(" • ", " · ", " - ").map(String::trim).filter(String::isNotBlank)
        val fallbackArtist = fallback.artist.cleanAlbumArtistLabel()
        val parsedArtist = tokens.firstNotNullOfOrNull { token ->
            token.cleanAlbumArtistLabel().takeIf(::isAlbumArtistToken)
        }.orEmpty().ifBlank { fallbackArtist }
        val artistReference = extractYoutubeMusicArtistReference(header, parsedArtist)
        val artist = artistReference?.name.orEmpty().cleanAlbumArtistLabel().ifBlank { parsedArtist }
        val year = tokens.firstNotNullOfOrNull { Regex("""\b(19|20)\d{2}\b""").find(it)?.value }
            .orEmpty()
            .ifBlank { fallback.year }
        val thumbnail = header?.let(::findBestThumbnail).orEmpty().ifBlank { fallback.thumbnailUrl }
        return fallback.copy(
            title = title,
            artist = artist.cleanLabel(),
            year = year,
            thumbnailUrl = upgradeThumbnail(thumbnail),
            query = "$title ${artist.cleanLabel()}".trim(),
            browseId = fallback.browseId.ifBlank { root.optString("browseId") },
            artistBrowseId = artistReference?.browseId.orEmpty().ifBlank { fallback.artistBrowseId }
        )
    }

    private fun extractAudioPlaylistId(root: JSONObject): String {
    val headers = mutableListOf<JSONObject>()
    collectObjectsByKey(root, "musicDetailHeaderRenderer", headers)
    collectObjectsByKey(root, "musicResponsiveHeaderRenderer", headers)

    fun playlistIds(value: Any?): List<String> {
        val endpoints = mutableListOf<JSONObject>()
        collectObjectsByKey(value, "watchEndpoint", endpoints)
        return endpoints.asSequence()
            .map { endpoint -> endpoint.optString("playlistId").trim().removePrefix("VL") }
            .filter { playlistId -> playlistId.startsWith("OLA") }
            .distinct()
            .toList()
    }

    headers.asSequence()
        .flatMap { header -> playlistIds(header).asSequence() }
        .firstOrNull()
        ?.let { return it }

    return playlistIds(root).singleOrNull().orEmpty()
}

    private fun parseOtherAlbumVersions(root: JSONObject, current: AlbumHit): List<AlbumHit> {
        val cards = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicTwoRowItemRenderer", cards)
        val versions = LinkedHashMap<String, AlbumHit>()
        cards.forEach { card ->
            val parsed = parseAlbumFromExploreItem(JSONObject().put("musicTwoRowItemRenderer", card)) ?: return@forEach
            if (parsed.browseId == current.browseId) return@forEach
            if (parsed.title.equals(current.title, ignoreCase = true) && parsed.artist.equals(current.artist, ignoreCase = true)) return@forEach
            versions.putIfAbsent(parsed.browseId.ifBlank { "${parsed.title}|${parsed.artist}" }, parsed)
        }
        return versions.values.take(20).toList()
    }

    private fun parseAlbumDescription(root: JSONObject): String {
        val shelves = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicDescriptionShelfRenderer", shelves)
        collectObjectsByKey(root, "descriptionShelfRenderer", shelves)
        shelves.forEach { shelf ->
            val text = shelf.optJSONObject("description")?.optJSONArray("runs")?.joinText().orEmpty()
                .ifBlank { shelf.optJSONObject("description")?.optString("simpleText").orEmpty() }
                .cleanAlbumDescription()
            if (text.length >= 12) return text
        }
        return ""
    }

    internal fun parseAlbumTracks(root: JSONObject, album: AlbumHit): List<Track> {
    return YoutubeMusicPlaylistPageParser.renderers(root)
        .mapNotNull { renderer -> parseAlbumTrackRenderer(renderer, album) }
        .distinctBy { it.id.ifBlank { "${it.title.lowercase()}|${it.artist.lowercase()}" } }
}

    internal fun parseAlbumTrackRenderer(renderer: JSONObject, album: AlbumHit): Track? {
        val videoId = renderer.optJSONObject("playlistItemData")?.optString("videoId").orEmpty()
            .ifBlank { extractPrimaryMusicVideoId(renderer) }
        if (videoId.isBlank()) return null
        val lines = extractFlexLines(renderer)
        val title = lines.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        if (isAlbumLabel(title)) return null
        val tokens = lines.drop(1).flatMap { it.split(" • ", " · ", " - ") }.map { it.trim() }.filter { it.isNotBlank() }
        val fallbackArtist = selectAlbumTrackArtist(tokens, album.artist)
        val artistReferences = extractYoutubeMusicArtistReferences(renderer, fallbackArtist)
        val artist = artistReferences.creditLabel(fallbackArtist)
        val thumbnail = album.thumbnailUrl.ifBlank { findBestThumbnail(renderer) }
        val trackNumber = renderer.optJSONArray("fixedColumns")
            ?.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")
            ?.joinText()
            ?.trim()
            ?.toIntOrNull()
            ?: 0
        return buildTrack(
            id = videoId,
            title = title,
            artist = artist,
            album = album.title,
            durationMs = extractDuration(renderer.toString()),
            thumbnailUrl = thumbnail,
            largeThumbnailUrl = upgradeThumbnail(thumbnail),
            videoUrl = "https://www.youtube.com/watch?v=$videoId",
            query = album.query.ifBlank { "${album.title} ${album.artist}" },
            source = "YouTube Music Album",
            year = album.year,
            explicit = renderer.toString().contains("MUSIC_ITEM_BADGE_EXPLICIT"),
            albumBrowseId = album.browseId,
            artistBrowseIds = artistReferences.map { it.browseId },
            videoType = findStringUnderKey(renderer, "musicVideoType").orEmpty(),
            trackNumber = trackNumber
        )
    }

    internal fun selectAlbumTrackArtist(tokens: List<String>, fallbackArtist: String): String {
        return tokens.firstNotNullOfOrNull { token ->
            val cleaned = token.cleanAlbumArtistLabel()
            cleaned.takeIf {
                it.isNotBlank() &&
                    !isTypeLabel(it) &&
                    !isAlbumLabel(it) &&
                    !isYoutubeMusicAlbumTrackMetadata(it)
            }
        } ?: fallbackArtist.cleanAlbumArtistLabel().ifBlank { fallbackArtist.trim() }
    }

    internal data class YoutubeMusicArtistReference(
        val name: String,
        val browseId: String
    )

    private fun List<YoutubeMusicArtistReference>.creditLabel(fallback: String): String {
        return asSequence()
            .map { reference -> reference.name.cleanAlbumArtistLabel().trim() }
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(", ")
            .ifBlank { fallback }
    }

    internal fun extractYoutubeMusicArtistReferences(
        value: Any?,
        preferredName: String
    ): List<YoutubeMusicArtistReference> {
        val ordered = LinkedHashMap<String, YoutubeMusicArtistReference>()

        fun isSeparator(text: String): Boolean {
            val normalized = text.replace('\u00A0', ' ').trim()
            return normalized == "•" || normalized == "·" || normalized == "|"
        }

        fun referenceFromRun(run: JSONObject?): YoutubeMusicArtistReference? {
            run ?: return null
            val browseEndpoint = run.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
                ?: return null
            val browseId = browseEndpoint.optString("browseId").trim()
            val pageType = browseEndpoint
                .optJSONObject("browseEndpointContextSupportedConfigs")
                ?.optJSONObject("browseEndpointContextMusicConfig")
                ?.optString("pageType")
                .orEmpty()
            if (browseId.isBlank() || !pageType.contains("ARTIST", ignoreCase = true)) return null
            val name = run.optString("text").cleanAlbumArtistLabel().trim()
            if (name.isBlank()) return null
            return YoutubeMusicArtistReference(name = name, browseId = browseId)
        }

        fun parseArtistRuns(runs: JSONArray?) {
            if (runs == null || runs.length() == 0) return
            val groups = mutableListOf<MutableList<JSONObject>>()
            var current = mutableListOf<JSONObject>()
            for (index in 0 until runs.length()) {
                val run = runs.optJSONObject(index) ?: continue
                if (isSeparator(run.optString("text"))) {
                    if (current.isNotEmpty()) groups += current
                    current = mutableListOf()
                } else {
                    current += run
                }
            }
            if (current.isNotEmpty()) groups += current
            val artistGroup = groups.firstOrNull { group -> group.any { referenceFromRun(it) != null } } ?: return
            artistGroup.mapNotNull(::referenceFromRun).forEach { reference ->
                ordered.putIfAbsent(reference.browseId.lowercase(Locale.ROOT), reference)
            }
        }

        fun parseRenderer(renderer: JSONObject) {
            val flexColumns = renderer.optJSONArray("flexColumns") ?: JSONArray()
            for (index in 0 until flexColumns.length()) {
                parseArtistRuns(
                    flexColumns.optJSONObject(index)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")
                    ?.optJSONArray("runs")
                )
            }
            parseArtistRuns(renderer.optJSONObject("subtitle")?.optJSONArray("runs"))
        }

        when (value) {
            is JSONObject -> {
                parseRenderer(value)
                listOf("musicResponsiveListItemRenderer", "musicTwoRowItemRenderer").forEach { key ->
                    value.optJSONObject(key)?.let(::parseRenderer)
                }
            }
            is JSONArray -> {
                for (index in 0 until value.length()) {
                    value.optJSONObject(index)?.let(::parseRenderer)
                }
            }
        }

        return ordered.values.toList()
    }

    internal fun extractYoutubeMusicArtistReference(
        value: Any?,
        preferredName: String
    ): YoutubeMusicArtistReference? {
        fun directReference(renderer: JSONObject): YoutubeMusicArtistReference? {
            val browseEndpoint = renderer.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
                ?: return null
            val browseId = browseEndpoint.optString("browseId").trim()
            val pageType = browseEndpoint
                .optJSONObject("browseEndpointContextSupportedConfigs")
                ?.optJSONObject("browseEndpointContextMusicConfig")
                ?.optString("pageType")
                .orEmpty()
            val isArtistEndpoint = browseId.startsWith("UC", ignoreCase = true) ||
                pageType.contains("ARTIST", ignoreCase = true)
            if (browseId.isBlank() || !isArtistEndpoint) return null
            val title = renderer.optJSONObject("title")
                ?.optJSONArray("runs")
                ?.joinText()
                .orEmpty()
                .cleanAlbumArtistLabel()
                .ifBlank {
                    renderer.optJSONArray("flexColumns")
                        ?.optJSONObject(0)
                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.optJSONObject("text")
                        ?.optJSONArray("runs")
                        ?.joinText()
                        .orEmpty()
                        .cleanAlbumArtistLabel()
                }
                .ifBlank {
                    renderer.optJSONObject("text")
                        ?.optJSONArray("runs")
                        ?.joinText()
                        .orEmpty()
                        .cleanAlbumArtistLabel()
                }
                .ifBlank { preferredName.cleanAlbumArtistLabel() }
            if (title.isBlank()) return null
            return YoutubeMusicArtistReference(name = title, browseId = browseId)
        }

        when (value) {
            is JSONObject -> {
                directReference(value)?.let { return it }
                listOf(
                    "musicResponsiveListItemRenderer",
                    "musicTwoRowItemRenderer"
                ).forEach { key ->
                    value.optJSONObject(key)?.let { renderer ->
                        directReference(renderer)?.let { return it }
                    }
                }
            }
            is JSONArray -> {
                for (index in 0 until value.length()) {
                    val renderer = value.optJSONObject(index) ?: continue
                    directReference(renderer)?.let { return it }
                }
            }
        }

        val nestedButtons = mutableListOf<JSONObject>()
        collectObjectsByKey(value, "button", nestedButtons)
        collectObjectsByKey(value, "musicNavigationButtonRenderer", nestedButtons)
        nestedButtons.forEach { button ->
            directReference(button)?.let { return it }
        }

        return extractYoutubeMusicArtistReferences(value, preferredName).firstOrNull()
    }

    private fun extractAlbumBrowseId(renderer: JSONObject): String {
        val endpoints = mutableListOf<JSONObject>()
        collectObjectsByKey(renderer, "browseEndpoint", endpoints)
        return endpoints.firstNotNullOfOrNull { endpoint ->
            val browseId = endpoint.optString("browseId").orEmpty()
            val pageType = endpoint.optJSONObject("browseEndpointContextSupportedConfigs")
                ?.optJSONObject("browseEndpointContextMusicConfig")
                ?.optString("pageType")
                .orEmpty()
            browseId.takeIf { it.startsWith("MPRE", ignoreCase = true) || pageType.equals("MUSIC_PAGE_TYPE_ALBUM", ignoreCase = true) }
        }.orEmpty()
    }

    suspend fun getWatchPlaylist(
        seed: Track,
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        limit: Int = 25,
        playlistId: String = "",
        radio: Boolean = false,
        shuffle: Boolean = false
    ): YoutubeMusicWatchPlaylist? = withContext(Dispatchers.IO) {
        val videoId = resolveVideoId(seed)
        if (videoId.isBlank() && playlistId.isBlank()) return@withContext null
        try {
            watchRepository.getWatchPlaylistAdvanced(
                videoId = videoId,
                playlistId = playlistId,
                languageCode = languageCode,
                limit = limit,
                radio = radio,
                shuffle = shuffle
            )
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun getSongRelated(
        seed: Track,
        languageCode: String = LevyraLanguageCatalog.deviceDefault()
    ): List<YoutubeMusicRelatedSection> = withContext(Dispatchers.IO) {
        val watch = getWatchPlaylist(seed, languageCode, 1) ?: return@withContext emptyList()
        if (watch.relatedBrowseId.isBlank()) return@withContext emptyList()
        runCatching { watchRepository.getSongRelated(watch.relatedBrowseId, languageCode) }.getOrDefault(emptyList())
    }

    suspend fun radio(
        seed: Track,
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        limit: Int = 20
    ): List<Track> = withContext(Dispatchers.IO) {
        val boundedLimit = limit.coerceIn(1, 30)
        val videoId = resolveVideoId(seed)
        if (videoId.isBlank()) return@withContext emptyList()
        val query = "radio ${seed.artist} ${seed.title}".trim()
        val tracks = LinkedHashMap<String, Track>()
        val watch = runCatching {
            watchRepository.getWatchPlaylistAdvanced(
                videoId = videoId,
                languageCode = languageCode,
                limit = boundedLimit + 8,
                radio = true
            )
        }.getOrNull()

        watch?.tracks
            ?.asSequence()
            ?.filter { it.videoId != videoId }
            ?.map { watchTrackToTrack(it, seed, query) }
            ?.filterNot { candidate -> isLowQualityRadioCandidate(candidate.title, candidate.artist) }
            ?.forEach { track -> tracks.putIfAbsent(track.id, track) }

        val relatedBrowseId = watch?.relatedBrowseId.orEmpty()
        if (tracks.size < boundedLimit && relatedBrowseId.isNotBlank()) {
            runCatching { watchRepository.getSongRelated(relatedBrowseId, languageCode) }
                .getOrDefault(emptyList())
                .asSequence()
                .flatMap { it.items.asSequence() }
                .filter { it.type == YoutubeMusicRelatedType.Song || it.type == YoutubeMusicRelatedType.Video }
                .filter { it.videoId.isNotBlank() && it.videoId != videoId }
                .map { relatedItemToTrack(it, seed, query) }
                .filterNot { candidate -> isLowQualityRadioCandidate(candidate.title, candidate.artist) }
                .forEach { track -> tracks.putIfAbsent(track.id, track) }
        }

        if (tracks.size < boundedLimit) {
            search("${seed.artist} ${seed.title} radio", boundedLimit + 4, languageCode)
                .filter { it.id != videoId }
                .filterNot { candidate -> isLowQualityRadioCandidate(candidate.title, candidate.artist) }
                .forEach { candidate -> tracks.putIfAbsent(candidate.id, candidate) }
        }

        tracks.values.take(boundedLimit).also { items -> items.forEach { memory[it.id] = it } }
    }

    private fun resolveVideoId(seed: Track): String {
        return PlaybackSourceIdentity.sourceVideoId(seed)
    }

    private fun watchTrackToTrack(item: YoutubeMusicWatchTrack, seed: Track, query: String): Track {
        val thumbnail = item.thumbnailUrl.ifBlank { seed.thumbnailUrl }
        return buildTrack(
            id = item.videoId,
            title = item.title,
            artist = item.artists.joinToString(", ") { it.name }.ifBlank { seed.artist.ifBlank { "YouTube Music" } },
            album = item.albumTitle.ifBlank { "YouTube Music Radio" },
            durationMs = item.durationMs,
            thumbnailUrl = thumbnail,
            largeThumbnailUrl = upgradeThumbnail(thumbnail),
            videoUrl = "https://www.youtube.com/watch?v=${item.videoId}",
            query = query,
            source = "YouTube Music Watch",
            year = item.year,
            explicit = item.explicit,
            albumBrowseId = item.albumBrowseId,
            artistBrowseIds = item.artists.map { it.browseId }.filter { it.isNotBlank() },
            counterpartVideoId = item.counterpart?.videoId.orEmpty(),
            videoType = item.videoType
        )
    }

    private fun relatedItemToTrack(item: YoutubeMusicRelatedItem, seed: Track, query: String): Track {
        val thumbnail = item.thumbnailUrl.ifBlank { seed.thumbnailUrl }
        return buildTrack(
            id = item.videoId,
            title = item.title,
            artist = item.artists.joinToString(", ") { it.name }.ifBlank { seed.artist.ifBlank { "YouTube Music" } },
            album = item.albumTitle.ifBlank { "YouTube Music Related" },
            durationMs = item.durationMs,
            thumbnailUrl = thumbnail,
            largeThumbnailUrl = upgradeThumbnail(thumbnail),
            videoUrl = "https://www.youtube.com/watch?v=${item.videoId}",
            query = query,
            source = "YouTube Music Related",
            year = item.year,
            explicit = item.explicit,
            albumBrowseId = item.albumBrowseId,
            artistBrowseIds = item.artists.map { it.browseId }.filter { it.isNotBlank() }
        )
    }

    fun searchSuggestions(query: String, languageCode: String = LevyraLanguageCatalog.deviceDefault()): List<String> {
        if (query.isBlank()) return emptyList()
        val locale = LevyraContentLocales.forLanguage(languageCode)
        val url = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&hl=${locale.hl}&gl=${locale.gl}&q=${java.net.URLEncoder.encode(query, "UTF-8")}"
        val remote = runCatching {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONArray(response)
            val suggestions = root.optJSONArray(1) ?: return@runCatching emptyList()
            val result = mutableListOf<String>()
            for (i in 0 until suggestions.length()) {
                result += suggestions.optString(i)
            }
            result
        }.getOrDefault(emptyList())
        return LevyraLocalizedDiscovery.suggestions(query, locale.languageCode, remote)
    }

    internal fun parseCarouselItem(item: JSONObject): Track? {
        item.optJSONObject("musicResponsiveListItemRenderer")?.let { return parseMusicRenderer(it, "home") }
        val two = item.optJSONObject("musicTwoRowItemRenderer") ?: return null
        val title = two.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().trim()
        if (title.isBlank()) return null
        val videoId = firstWatchVideoId(two).ifBlank { return null }
        val subtitle = two.optJSONObject("subtitle")?.optJSONArray("runs")?.joinText().orEmpty()

        val tokens = subtitle.split(" • ", " · ", " - ").map { it.trim() }
        if (tokens.isNotEmpty() && tokens[0].lowercase() in excludedTypes) return null

        val fallbackArtist = tokens.firstOrNull(::isPlausibleSearchMetadataLabel) ?: "YouTube Music"
        val artistReferences = extractYoutubeMusicArtistReferences(two, fallbackArtist)
        val artist = artistReferences.creditLabel(fallbackArtist)
        val thumbnail = findBestThumbnail(two)
        return buildTrack(
            id = videoId,
            title = title,
            artist = artist,
            album = "YouTube Music",
            durationMs = 0L,
            thumbnailUrl = thumbnail,
            largeThumbnailUrl = upgradeThumbnail(thumbnail),
            videoUrl = "https://www.youtube.com/watch?v=$videoId",
            query = "home",
            source = "YouTube Music",
            artistBrowseIds = artistReferences.map { it.browseId }
        )
    }

    private fun firstWatchVideoId(renderer: JSONObject): String {
        val endpoints = mutableListOf<JSONObject>()
        collectObjectsByKey(renderer, "watchEndpoint", endpoints)
        endpoints.forEach { endpoint ->
            if (endpoint.has("playlistId")) return@forEach
            val id = endpoint.optString("videoId")
            if (id.isNotBlank()) return id
        }
        return ""
    }

    fun cachedTracks(): List<Track> = memory.values.toList()

    fun replace(track: Track) {
        memory[track.id] = track
    }

    fun cacheReport(): CacheReport {
        val all = memory.values.toList()
        val resolved = all.count { it.streamUrl.isNotBlank() }
        return CacheReport(
            offlineReady = resolved,
            smartCached = all.count { it.cacheScore >= 70 },
            nextPreload = all.take(6).count(),
            totalTracks = all.size
        )
    }

    private fun searchInnerTube(query: String, limit: Int, languageCode: String): List<Track> {
    val root = searchInnerTubeRaw(query, languageCode) ?: return emptyList()
    val renderers = mutableListOf<JSONObject>()
    collectObjectsByKey(root, "musicResponsiveListItemRenderer", renderers)
    val tracks = LinkedHashMap<String, Track>()
    renderers.forEach { renderer ->
        val track = parseMusicRenderer(renderer, query)
        if (track != null && !tracks.containsKey(track.id)) tracks[track.id] = track
    }
    if (tracks.isEmpty()) {
        val videoRenderers = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "videoRenderer", videoRenderers)
        videoRenderers.forEach { renderer ->
            val track = parseVideoRenderer(renderer, query)
            if (track != null && !tracks.containsKey(track.id)) tracks[track.id] = track
        }
    }
    return tracks.values.take(limit)
}

    private fun searchYoutubeExtractor(query: String, limit: Int): List<Track> {
        NewPipeRuntime.ensure()
        val service = org.schabi.newpipe.extractor.ServiceList.YouTube
        val factory = service.searchQHFactory
        val musicSongsFilter = findExtractorFilterItem(
            factory.getAvailableContentFilter(),
            org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_SONGS
        )
        val handler = if (musicSongsFilter != null) {
            factory.fromQuery(
                query,
                mutableListOf(musicSongsFilter),
                mutableListOf<org.schabi.newpipe.extractor.search.filter.FilterItem>()
            )
        } else {
            factory.fromQuery(query)
        }
        val info = org.schabi.newpipe.extractor.search.SearchInfo.getInfo(service, handler)
        return info.relatedItems
            .filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>()
            .mapNotNull { item ->
                val id = extractVideoId(item.url).ifBlank { stableId(item.url) }
                if (id.isBlank()) return@mapNotNull null
                val thumbnail = item.thumbnailUrl.orEmpty()
                buildTrack(
                    id = id,
                    title = item.name,
                    artist = item.uploaderName.orEmpty(),
                    album = "YouTube Music",
                    durationMs = secondsToMs(item.duration),
                    thumbnailUrl = thumbnail,
                    largeThumbnailUrl = upgradeThumbnail(thumbnail),
                    videoUrl = item.url,
                    query = query,
                    source = "LevyraExtractor Search"
                )
            }
            .distinctBy { it.id }
            .take(limit)
    }

    private fun findExtractorFilterItem(
        filter: org.schabi.newpipe.extractor.search.filter.Filter,
        name: String
    ): org.schabi.newpipe.extractor.search.filter.FilterItem? {
        return filter.filterGroups
            .asSequence()
            .flatMap { group -> group.filterItems.asSequence() }
            .firstOrNull { item -> item.name.equals(name, ignoreCase = true) }
    }

    private val typeLabels = setOf(
        "song", "video", "single",
        "brano", "canzone", "video musicale", "video ufficiale", "singolo", "episodio",
        "canción", "cancion", "chanson", "titre", "lied", "nummer",
        "utwór", "piosenka", "melodie", "τραγούδι", "låt", "sang", "píseň", "skladba", "пісня"
    ) + excludedTypes

    private fun isTypeLabel(token: String): Boolean =
        token.trim().lowercase(Locale.ROOT) in typeLabels

    internal fun parseMusicRenderer(renderer: JSONObject, query: String): Track? {
        val lines = extractFlexLines(renderer)
        val title = lines.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val videoId = extractPrimaryMusicVideoId(renderer).takeIf { it.isNotBlank() } ?: return null
        val allText = renderer.toString()
        val duration = extractDuration(allText)

        val subtitleLines = lines.drop(1)
        val tokens = subtitleLines.flatMap { it.split(" • ", " · ", " - ") }.map { it.trim() }
        if (tokens.isNotEmpty() && tokens[0].lowercase() in excludedTypes) return null

        val fallbackArtist = tokens
            .firstOrNull(::isPlausibleSearchMetadataLabel)
            ?: "YouTube Music"
        val artistReferences = extractYoutubeMusicArtistReferences(renderer, fallbackArtist)
        val artist = artistReferences.creditLabel(fallbackArtist)
        val albumReference = extractYoutubeMusicAlbumReference(renderer)
        val album = albumReference.first
            .takeIf { label ->
                isPlausibleSearchMetadataLabel(label) &&
                    !isResolvedArtistMetadataToken(label, artist, artistReferences)
            }
            ?: tokens.firstOrNull { token ->
                isPlausibleSearchMetadataLabel(token) &&
                    !isResolvedArtistMetadataToken(token, artist, artistReferences)
            }
            ?: "YouTube Music"
        val thumbnail = findBestThumbnail(renderer)
        return buildTrack(
            id = videoId,
            title = title,
            artist = artist,
            album = album,
            durationMs = duration,
            thumbnailUrl = thumbnail,
            largeThumbnailUrl = upgradeThumbnail(thumbnail),
            videoUrl = "https://www.youtube.com/watch?v=$videoId",
            query = query,
            source = "YouTube Music",
            albumBrowseId = albumReference.second,
            artistBrowseIds = artistReferences.map { it.browseId },
            videoType = findStringUnderKey(renderer, "musicVideoType").orEmpty(),
            youtubeViewCount = tokens.asSequence()
                .map(::parseCompactViewCount)
                .firstOrNull { it >= 0L }
                ?: -1L
        )
    }

    private fun isResolvedArtistMetadataToken(
        token: String,
        artist: String,
        artistReferences: List<YoutubeMusicArtistReference>
    ): Boolean {
        val normalizedToken = normalizedReleaseArtist(token)
        if (normalizedToken.isBlank()) return false
        if (normalizedToken == normalizedReleaseArtist(artist)) return true
        if (artistReferences.any { reference -> normalizedToken == normalizedReleaseArtist(reference.name) }) return true

        val referenceNames = artistReferences
            .map { reference -> normalizedReleaseArtist(reference.name) }
            .filter(String::isNotBlank)
        return referenceNames.size > 1 && referenceNames.all(normalizedToken::contains)
    }

    private fun isPlausibleSearchMetadataLabel(token: String): Boolean {
        return token.isNotBlank() &&
            !isTypeLabel(token) &&
            !isYoutubeMusicAlbumTrackMetadata(token)
    }

    private fun extractPrimaryMusicVideoId(renderer: JSONObject): String {
        renderer.optJSONObject("playlistItemData")?.optString("videoId")?.takeIf { it.isNotBlank() }?.let { return it }
        val columns = renderer.optJSONArray("flexColumns") ?: JSONArray()
        for (i in 0 until columns.length()) {
            val runs = columns.optJSONObject(i)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
                ?: continue
            for (j in 0 until runs.length()) {
                val videoId = runs.optJSONObject(j)
                    ?.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("watchEndpoint")
                    ?.optString("videoId")
                    .orEmpty()
                if (videoId.isNotBlank()) return videoId
            }
        }
        val playButtons = mutableListOf<JSONObject>()
        collectObjectsByKey(renderer, "musicPlayButtonRenderer", playButtons)
        playButtons.forEach { button ->
            val videoId = button.optJSONObject("playNavigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
                .orEmpty()
            if (videoId.isNotBlank()) return videoId
        }
        val watchEndpoints = mutableListOf<JSONObject>()
        collectObjectsByKey(renderer, "watchEndpoint", watchEndpoints)
        watchEndpoints.forEach { endpoint ->
            val videoId = endpoint.optString("videoId")
            if (videoId.isNotBlank()) return videoId
        }
        return ""
    }

    private fun parseVideoRenderer(renderer: JSONObject, query: String): Track? {
        val videoId = renderer.optString("videoId").takeIf { it.isNotBlank() } ?: return null
        val title = renderer.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().ifBlank { return null }
        val artist = renderer.optJSONObject("ownerText")?.optJSONArray("runs")?.joinText().orEmpty().ifBlank { "YouTube" }
        val duration = renderer.optJSONObject("lengthText")?.optString("simpleText")?.durationToMs() ?: 0L
        val thumbnail = renderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")?.bestThumbnail().orEmpty()
        return buildTrack(
            id = videoId,
            title = title,
            artist = artist,
            album = "YouTube",
            durationMs = duration,
            thumbnailUrl = thumbnail,
            largeThumbnailUrl = upgradeThumbnail(thumbnail),
            videoUrl = "https://www.youtube.com/watch?v=$videoId",
            query = query,
            source = "YouTube"
        )
    }

    private fun buildTrack(
        id: String,
        title: String,
        artist: String,
        album: String,
        durationMs: Long,
        thumbnailUrl: String,
        largeThumbnailUrl: String,
        videoUrl: String,
        query: String,
        source: String,
        year: String = "",
        explicit: Boolean = false,
        albumBrowseId: String = "",
        artistBrowseIds: List<String> = emptyList(),
        counterpartVideoId: String = "",
        videoType: String = "",
        youtubeViewCount: Long = -1L,
        trackNumber: Int = 0,
        discNumber: Int = 0
    ): Track {
        val seed = stableSeed("$id$title$artist$query")
        val normalized = "$title $artist $album $query".lowercase()
        val tags = buildSet {
            add("hit")
            if (normalized.contains("ital") || normalized.contains("sanremo")) add("italian")
            if (normalized.contains("rap") || normalized.contains("trap")) add("rap")
            if (normalized.contains("gym") || normalized.contains("workout") || normalized.contains("bass")) add("gym")
            if (normalized.contains("night") || normalized.contains("chill")) add("night")
            if (normalized.contains("focus") || normalized.contains("deep")) add("focus")
            if (normalized.contains("pop")) add("pop")
            if (normalized.contains("new")) add("new")
            if (isEmpty()) add("music")
        }
val youtubeVideoId = id.trim().takeIf { value ->
    value.length == 11 && value.all { character ->
        character in 'A'..'Z' || character in 'a'..'z' || character in '0'..'9' || character == '_' || character == '-'
    }
}
val resolvedThumbnailUrl = thumbnailUrl.trim().ifBlank {
    youtubeVideoId?.let { videoId -> "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" }.orEmpty()
}
val resolvedLargeThumbnailUrl = largeThumbnailUrl.trim().ifBlank {
    youtubeVideoId?.let { videoId -> "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg" }
        .orEmpty()
        .ifBlank { resolvedThumbnailUrl }
}
return Track(
    id = id,
    title = title.cleanLabel(),
            artist = artist.cleanLabel(),
            album = album.cleanLabel(),
            durationMs = durationMs,
            streamUrl = "",
            videoUrl = videoUrl,
            thumbnailUrl = resolvedThumbnailUrl,
            largeThumbnailUrl = resolvedLargeThumbnailUrl,
            source = source,
            moodTags = tags,
            energy = (45 + seed % 52).coerceIn(0, 100),
            vocal = (35 + (seed / 3) % 60).coerceIn(0, 100),
            replayScore = (62 + (seed / 7) % 38).coerceIn(0, 100),
            cacheScore = (48 + (seed / 11) % 50).coerceIn(0, 100),
            accentStart = palette(seed).first,
            accentEnd = palette(seed).second,
            year = year,
            explicit = explicit,
            albumBrowseId = albumBrowseId,
            artistBrowseIds = artistBrowseIds,
            counterpartVideoId = counterpartVideoId,
            videoType = videoType,
            youtubeViewCount = youtubeViewCount,
            trackNumber = trackNumber,
            discNumber = discNumber
        )
    }

    private fun collectObjectsByKey(value: Any?, key: String, out: MutableList<JSONObject>) {
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val current = keys.next()
                    val child = value.opt(current)
                    if (current == key && child is JSONObject) out += child
                    collectObjectsByKey(child, key, out)
                }
            }
            is JSONArray -> for (i in 0 until value.length()) collectObjectsByKey(value.opt(i), key, out)
        }
    }

    private fun findStringUnderKey(value: Any?, key: String): String? {
        when (value) {
            is JSONObject -> {
                val direct = value.optString(key).takeIf { it.isNotBlank() }
                if (direct != null) return direct
                val keys = value.keys()
                while (keys.hasNext()) {
                    val result = findStringUnderKey(value.opt(keys.next()), key)
                    if (!result.isNullOrBlank()) return result
                }
            }
            is JSONArray -> for (i in 0 until value.length()) {
                val result = findStringUnderKey(value.opt(i), key)
                if (!result.isNullOrBlank()) return result
            }
        }
        return null
    }

    private fun extractFlexLines(renderer: JSONObject): List<String> {
        val lines = mutableListOf<String>()
        val columns = renderer.optJSONArray("flexColumns") ?: JSONArray()
        for (i in 0 until columns.length()) {
            val text = columns.optJSONObject(i)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
                ?.joinText()
                .orEmpty()
                .trim()
            if (text.isNotBlank()) lines += text
        }
        return lines.distinct()
    }

    private fun findBestThumbnail(renderer: JSONObject): String {
        val arrays = mutableListOf<JSONArray>()
        collectArraysByKey(renderer, "thumbnails", arrays)
        return arrays.asSequence().map { it.bestThumbnail() }.firstOrNull { it.isNotBlank() }.orEmpty()
    }

    private fun collectArraysByKey(value: Any?, key: String, out: MutableList<JSONArray>) {
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val current = keys.next()
                    val child = value.opt(current)
                    if (current == key && child is JSONArray) out += child
                    collectArraysByKey(child, key, out)
                }
            }
            is JSONArray -> for (i in 0 until value.length()) collectArraysByKey(value.opt(i), key, out)
        }
    }

    private fun extractDuration(text: String): Long {
        val match = Regex("\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\b").find(text)?.value ?: return 0L
        return match.durationToMs()
    }

    private fun extractVideoId(url: String): String {
        val patterns = listOf(
            Regex("[?&]v=([^&]+)"),
            Regex("youtu\\.be/([^?&/]+)"),
            Regex("shorts/([^?&/]+)")
        )
        return patterns.firstNotNullOfOrNull { it.find(url)?.groupValues?.getOrNull(1) }.orEmpty()
    }

    private fun secondsToMs(seconds: Long): Long {
        return if (seconds > 0) seconds * 1000L else 0L
    }

    private fun JSONArray.joinText(): String = extractYoutubeMusicRunText(this)

    private fun JSONArray.bestThumbnail(): String {
        var bestUrl = ""
        var bestScore = -1
        for (i in 0 until length()) {
            val item = optJSONObject(i) ?: continue
            val url = item.optString("url")
            val score = item.optInt("width", 0) * item.optInt("height", 0)
            if (url.isNotBlank() && score >= bestScore) {
                bestUrl = url
                bestScore = score
            }
        }
        return bestUrl
    }

    private fun String.durationToMs(): Long {
        val parts = split(":").mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            2 -> (parts[0] * 60L + parts[1]) * 1000L
            3 -> (parts[0] * 3600L + parts[1] * 60L + parts[2]) * 1000L
            else -> 0L
        }
    }

    private fun String.cleanLabel(): String {
        return replace("\\n", " ").replace(Regex("\\s+"), " ").trim()
    }

    private fun upgradeThumbnail(url: String): String {
        if (url.isBlank()) return url
        return url.replace(Regex("=w\\d+-h\\d+.*$"), "=w1200-h1200-l90-rj")
            .replace(Regex("=s\\d+.*$"), "=s1200")
    }

    private fun stableSeed(value: String): Int {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .take(4)
            .fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }
            .absoluteValue
    }

    private fun stableId(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .take(8)
            .joinToString("") { "%02x".format(it) }
    }

    private fun palette(seed: Int): Pair<Int, Int> {
        val palettes = listOf(
            0xFF00E5FF.toInt() to 0xFF7B42FF.toInt(),
            0xFF1B5CFF.toInt() to 0xFFFF4FD8.toInt(),
            0xFFFF7A18.toInt() to 0xFF8E57FF.toInt(),
            0xFF00D4A6.toInt() to 0xFFFF3B5C.toInt(),
            0xFFFFB000.toInt() to 0xFF00E5FF.toInt()
        )
        return palettes[seed % palettes.size]
    }
}

internal fun extractYoutubeMusicRunText(runs: JSONArray?): String {
    if (runs == null) return ""
    val parts = ArrayList<String>(runs.length())
    for (index in 0 until runs.length()) {
        val run = runs.optJSONObject(index) ?: continue
        val text = (run.opt("text") as? String).orEmpty()
        if (text.isBlank() || text.looksLikeSerializedJson()) continue
        parts += text
    }
    return parts.joinToString("").replace(Regex("[\t ]{2,}"), " ").trim()
}

internal fun String.cleanAlbumArtistLabel(): String {
    val cleaned = replace("\\n", " ")
        .replace('\n', ' ')
        .replace('\r', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
    if (cleaned.isBlank() || cleaned.looksLikeSerializedJson()) return ""
    val normalized = cleaned.lowercase()
        .replace('\u2019', '\'')
        .replace(Regex("[^\\p{L}\\p{M}\\p{N}]+"), " ")
        .trim()
    if (normalized in ALBUM_ARTIST_ACTION_LABELS) return ""
    return cleaned
}

private fun String.looksLikeSerializedJson(): Boolean {
    val value = trim()
    return (value.startsWith("{") && value.endsWith("}")) ||
        (value.startsWith("[") && value.endsWith("]"))
}

private val ALBUM_ARTIST_ACTION_LABELS = setOf(
    "artista",
    "artist",
    "vai all artista",
    "apri artista",
    "visualizza artista",
    "pagina artista",
    "go to artist",
    "open artist",
    "view artist",
    "artist page"
)

internal fun isYoutubeMusicAlbumTrackMetadata(value: String): Boolean {
    val normalized = value
        .replace('\u00A0', ' ')
        .replace('\u202F', ' ')
        .trim()
        .lowercase(Locale.ROOT)
    if (normalized.isBlank()) return true
    if (normalized.matches(Regex("^(?:19|20)\\d{2}$"))) return true
    if (normalized.matches(Regex("^\\d{1,2}:\\d{2}(?::\\d{2})?$"))) return true
    if (normalized.matches(Regex("^\\d+\\s*(?:brani|tracce|songs?|tracks?)$", RegexOption.IGNORE_CASE))) return true
    if (normalized.none(Char::isDigit)) return false
    return hasYoutubeMusicMetricWord(normalized) || ALBUM_TRACK_METRIC_PATTERN.containsMatchIn(normalized)
}

private fun hasYoutubeMusicMetricWord(value: String): Boolean =
    value.any(Char::isDigit) && YOUTUBE_MUSIC_METRIC_WORDS.any(value::contains)

private val YOUTUBE_MUSIC_METRIC_WORDS = listOf(
    "view", "visualizz", "riproduzion", "ascolt", "play", "stream",
    "reproduccion", "reproducción", "escucha", "vue", "écoute",
    "aufruf", "wiedergabe", "reprodução", "visualização",
    "просмотр", "прослушив", "再生", "조회수", "스트리밍", "צפיות", "השמעות"
)

private val ALBUM_TRACK_METRIC_PATTERN = Regex(
    "(?:^|\\s)[\\d.,]+\\s*(?:k|m|mln|mil|mio|mrd|bn|b|milioni?|miliardi?|millions?|billions?)?\\s*(?:views?|visualizzazioni?|riproduzioni?|ascolti?|plays?|streams?)\\b",
    RegexOption.IGNORE_CASE
)

internal fun isLowQualityRadioCandidate(title: String, artist: String): Boolean {
    val value = "$title $artist"
    return LOW_QUALITY_RADIO_PATTERNS.any { pattern -> pattern.containsMatchIn(value) }
}

private val LOW_QUALITY_RADIO_PATTERNS = listOf(
    Regex("(?<![\\p{L}\\p{N}])karaoke(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE),
    Regex("(?<![\\p{L}\\p{N}])nightcore(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE),
    Regex("(?<![\\p{L}\\p{N}])slowed(?:\\s*(?:&|\\+|and)\\s*reverb)?(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE),
    Regex("(?<![\\p{L}\\p{N}])sped[\\s-]*up(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE),
    Regex("(?<![\\p{L}\\p{N}])(?:reaction\\s+(?:video|review)|reacts?\\s+to|first\\s+reaction)(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE),
    Regex("\\(\\s*reaction\\s*\\)", RegexOption.IGNORE_CASE)
)

internal fun String.cleanAlbumDescription(): String {
    if (isBlank()) return ""
    val normalized = replace("\\n", "\n").replace("\r\n", "\n").replace('\r', '\n')
    val cutoff = ALBUM_DESCRIPTION_ATTRIBUTION_PATTERNS
        .mapNotNull { pattern -> pattern.find(normalized)?.range?.first }
        .minOrNull()
    return normalized
        .let { text -> cutoff?.let { text.take(it) } ?: text }
        .replace(Regex("https?://\\S+"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private val ALBUM_DESCRIPTION_ATTRIBUTION_PATTERNS = listOf(
    Regex("\\b(?:da|dalla|from|de|von|source|fonte)\\s+wikipedia\\b", RegexOption.IGNORE_CASE),
    Regex("\\bwikipedia\\b", RegexOption.IGNORE_CASE),
    Regex("\\bcreative\\s+commons\\b", RegexOption.IGNORE_CASE),
    Regex("\\bcreativecommons\\.org\\b", RegexOption.IGNORE_CASE),
    Regex("\\bcc[- ]?by(?:[- ]?sa)?\\b", RegexOption.IGNORE_CASE)
)

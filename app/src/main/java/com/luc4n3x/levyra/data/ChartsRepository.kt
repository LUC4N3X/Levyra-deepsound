package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.artistIdentityKey
import com.luc4n3x.levyra.domain.isArtistShelfNameEligible
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

internal val WorldFreshMarketPriority: List<String> = listOf(
    "US", "GB", "KR", "JP", "BR", "FR", "DE", "ES", "IT", "MX", "IN", "SE", "NL", "CA", "AU"
)

internal fun isFreshCurrentCandidate(track: Track): Boolean =
    track.id.isNotBlank() &&
        track.title.isNotBlank() &&
        track.artist.isNotBlank() &&
        (track.thumbnailUrl.isNotBlank() || track.largeThumbnailUrl.isNotBlank()) &&
        !isYoutubeShortTrack(track)

internal fun freshCurrentIdentity(track: Track): String = track.isrc.ifBlank {
    "${track.artist.trim().lowercase()}|${track.title.trim().lowercase()}"
}

internal fun selectFreshCurrentTracks(
    editorial: List<Track>,
    fallback: List<Track>,
    limit: Int
): List<Track> {
    if (limit <= 0) return emptyList()
    val editorialClean = editorial.filter(::isFreshCurrentCandidate)
    val source = editorialClean.ifEmpty { fallback.filter(::isFreshCurrentCandidate) }
    return source
        .distinctBy(::freshCurrentIdentity)
        .take(limit)
}

internal fun blendWorldFreshTracks(
    homeMarket: String,
    releasesByMarket: Map<String, List<Track>>,
    localTracks: List<Track>,
    limit: Int
): List<Track> {
    if (limit <= 0) return emptyList()
    val home = homeMarket.trim().uppercase()
    val queues = releasesByMarket
        .mapKeys { (market, _) -> market.trim().uppercase() }
        .filterKeys { market -> market.length == 2 && market != home }
        .mapValues { (_, tracks) -> tracks.filter(::isFreshCurrentCandidate) }
        .filterValues { tracks -> tracks.isNotEmpty() }
    if (queues.isEmpty()) return emptyList()

    val orderedMarkets = queues.keys.sortedWith(
        compareBy<String> { market ->
            WorldFreshMarketPriority.indexOf(market).takeIf { index -> index >= 0 } ?: WorldFreshMarketPriority.size
        }.thenBy { market -> market }
    )
    val seen = localTracks.mapTo(HashSet<String>(), ::freshCurrentIdentity)
    val blended = ArrayList<Track>(limit)
    var depth = 0
    val deepest = queues.values.maxOf { tracks -> tracks.size }
    while (depth < deepest && blended.size < limit) {
        for (market in orderedMarkets) {
            if (blended.size >= limit) break
            val track = queues.getValue(market).getOrNull(depth) ?: continue
            if (seen.add(freshCurrentIdentity(track))) blended += track
        }
        depth++
    }
    return blended
}

private const val CHART_PLACEHOLDER_ALBUM = "YouTube Music Charts"

internal fun isChartArtworkPlaceholder(track: Track): Boolean {
    val artwork = track.thumbnailUrl.ifBlank { track.largeThumbnailUrl }
    return artwork.isBlank() || artwork.contains("ytimg.com/vi")
}

private val CHART_RECORDING_VERSION_TERMS = listOf("live", "remix", "acoustic", "instrumental", "sped up", "slowed", "karaoke")
    .map { term -> term to Regex("\\b$term\\b") }

internal fun chartArtworkIdentity(track: Track): String {
    val primaryArtist = track.artist.split(',', '&', ';').first().split(" x ", " X ", " feat", " ft.").first()
    val title = track.title.lowercase()
    val version = CHART_RECORDING_VERSION_TERMS.filter { (_, pattern) -> pattern.containsMatchIn(title) }.joinToString("+") { it.first }
    return ChartFeedParser.normalizeMusicText(track.title) + "|" + ChartFeedParser.normalizeMusicText(primaryArtist) + "|" + version
}

internal fun withEditorialArtwork(track: Track, index: Map<String, Track>): Track? {
    val editorial = index[chartArtworkIdentity(track)] ?: return null
    if (track.isrc.isNotBlank() && editorial.isrc.isNotBlank() && !track.isrc.equals(editorial.isrc, ignoreCase = true)) return null
    if (editorial.thumbnailUrl == track.thumbnailUrl) return track
    return track.copy(
        thumbnailUrl = editorial.thumbnailUrl,
        largeThumbnailUrl = editorial.largeThumbnailUrl.ifBlank { editorial.thumbnailUrl },
        album = track.album.takeUnless { it.isBlank() || it.equals(CHART_PLACEHOLDER_ALBUM, ignoreCase = true) } ?: editorial.album.ifBlank { track.album }
    )
}

class ChartsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val httpClient: OkHttpClient by lazy { LevyraHttpClientFactory.feeds(appContext) }
    private val youtubeMusicCharts = YoutubeMusicChartsRepository(appContext)
    private val editorialCharts = EditorialChartsRepository.get(appContext)
    private val chartArtworkResolver = ChartOfficialArtworkResolver(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = ConcurrentHashMap<String, Deferred<List<Track>>>()

    init {
        editorialCharts.warm()
    }

    fun close() {
        scope.cancel()
        inFlight.clear()
    }

    suspend fun topSongs(country: String, limit: Int = 50): List<Track> {
        val normalizedCountry = country.trim().lowercase().takeIf { it.length == 2 } ?: "it"
        val normalizedLimit = limit.coerceIn(1, 100)
        val shared = sharedRequest("$normalizedCountry/$normalizedLimit", normalizedCountry, normalizedLimit)
        return try {
            shared.await()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Timber.w(error, "Chart request failed for %s", normalizedCountry)
            emptyList()
        }
    }

    suspend fun cachedCountryCharts(limit: Int = 50): Map<String, List<Track>> = withContext(Dispatchers.IO) {
        editorialCharts.cachedAllMarkets(limit)
            .mapKeys { (market, _) -> market.lowercase() }
    }

    suspend fun newReleaseAlbums(country: String, limit: Int = 40): List<AlbumHit> = withContext(Dispatchers.IO) {
        val safeLimit = limit.coerceIn(1, 80)
        val tracks = editorialCharts.cachedNewReleaseTracks(country, safeLimit * 3)
        tracks.asSequence()
            .filter { track ->
                track.title.isNotBlank() &&
                    track.artist.isNotBlank() &&
                    track.album.isNotBlank() &&
                    (track.thumbnailUrl.isNotBlank() || track.largeThumbnailUrl.isNotBlank())
            }
            .map { track ->
                val albumTitle = track.album.ifBlank { track.title }
                AlbumHit(
                    title = albumTitle,
                    artist = track.artist,
                    year = track.year,
                    thumbnailUrl = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl },
                    query = "${track.artist} $albumTitle".trim(),
                    browseId = track.albumBrowseId,
                    artistBrowseId = track.artistBrowseIds.firstOrNull().orEmpty(),
                    explicit = track.explicit,
                    releaseDate = track.releaseDate,
                    metadataProvider = track.metadataProvider,
                    metadataConfidence = track.metadataConfidence,
                    releaseType = if (albumTitle.equals(track.title, ignoreCase = true)) {
                        ReleaseType.Single
                    } else {
                        ReleaseType.Unknown
                    }
                )
            }
            .distinctBy { release ->
                release.browseId.ifBlank {
                    "${release.artist.trim().lowercase()}|${release.title.trim().lowercase()}"
                }
            }
            .take(safeLimit)
            .toList()
    }

    suspend fun freshCurrentTracks(country: String, limit: Int = 24): List<Track> = withContext(Dispatchers.IO) {
        val safeLimit = limit.coerceIn(1, 40)
        val editorial = editorialCharts.newReleaseTracks(country, safeLimit)
        if (editorial.isNotEmpty()) {
            return@withContext selectFreshCurrentTracks(editorial, emptyList(), safeLimit)
        }
        val fallback = topSongs(country, safeLimit)
        selectFreshCurrentTracks(emptyList(), fallback, safeLimit)
    }

    suspend fun worldFreshTracks(
        country: String,
        localTracks: List<Track>,
        limit: Int = 18
    ): List<Track> = withContext(Dispatchers.IO) {
        val safeLimit = limit.coerceIn(1, 40)
        val market = country.trim().uppercase().takeIf { it.length == 2 } ?: return@withContext emptyList()
        val releasesByMarket = editorialCharts.newReleasesByMarket(safeLimit * 2)
        blendWorldFreshTracks(market, releasesByMarket, localTracks, safeLimit)
    }

    suspend fun officialArtwork(title: String, artist: String, country: String): String? = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()
        if (cleanTitle.isBlank() || cleanArtist.isBlank()) return@withContext null
        val region = country.trim().lowercase().takeIf { it.length == 2 } ?: "it"
        val term = URLEncoder.encode("$cleanTitle $cleanArtist", StandardCharsets.UTF_8.name())
        val body = httpGet("https://itunes.apple.com/search?term=$term&entity=song&limit=12&country=$region")
            ?: return@withContext null
        val results = JSONObject(body).optJSONArray("results") ?: return@withContext null
        val targetTitle = ChartFeedParser.normalizeMusicText(cleanTitle)
        val targetArtist = ChartFeedParser.normalizeMusicText(cleanArtist)
        var bestArtwork = ""
        var bestScore = Int.MIN_VALUE
        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val artwork = item.optString("artworkUrl100").trim()
            if (artwork.isBlank()) continue
            val candidateTitle = ChartFeedParser.normalizeMusicText(item.optString("trackName"))
            val candidateArtist = ChartFeedParser.normalizeMusicText(item.optString("artistName"))
            val score = artworkMatchScore(targetTitle, targetArtist, candidateTitle, candidateArtist)
            if (score > bestScore) {
                bestScore = score
                bestArtwork = artwork
            }
        }
        bestArtwork.takeIf { bestScore >= 95 }?.let(ChartFeedParser::upgradeArtwork)
    }

    private fun normalizeTrendingCountry(country: String): String =
        country.trim().lowercase(Locale.ROOT).takeIf { it.length == 2 } ?: "it"

    private fun readCachedAlbums(file: File, limit: Int, checkTtl: Boolean): List<AlbumHit>? {
        if (!file.exists()) return null
        if (checkTtl && System.currentTimeMillis() - file.lastModified() >= APPLE_TRENDING_CACHE_TTL_MS) return null
        val cachedJson = runCatching { file.readText() }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null
        return AppleTrendingParser.parseAlbums(cachedJson, limit).takeIf { it.isNotEmpty() }
    }

    private suspend fun fetchAppleTrendingAlbums(country: String, limit: Int, cacheFile: File): List<AlbumHit>? {
        val primaryUrl = "https://rss.marketingtools.apple.com/api/v2/$country/music/most-played/20/albums.json"
        val fallbackUrl = "https://rss.applemarketingtools.com/api/v2/$country/music/most-played/20/albums.json"
        val body = (httpGet(primaryUrl) ?: httpGet(fallbackUrl))?.takeIf { it.isNotBlank() } ?: return null
        val parsed = AppleTrendingParser.parseAlbums(body, limit)
        if (parsed.isNotEmpty()) {
            runCatching { cacheFile.writeText(body) }
            return parsed
        }
        return null
    }

    private suspend fun fetchFallbackTrendingAlbums(country: String, limit: Int): List<AlbumHit> {
        val editorialAlbums = newReleaseAlbums(country, limit)
        if (editorialAlbums.isNotEmpty()) return editorialAlbums

        val deezerBody = httpGet("https://api.deezer.com/chart/0/albums?limit=$limit")
        if (!deezerBody.isNullOrBlank()) {
            val deezerAlbums = AppleTrendingParser.parseDeezerAlbums(deezerBody, limit)
            if (deezerAlbums.isNotEmpty()) return deezerAlbums
        }

        return fetchYoutubeTrendingAlbums(country, limit)
    }

    private suspend fun fetchYoutubeTrendingAlbums(country: String, limit: Int): List<AlbumHit> {
        val ytTracks = fetchYoutubeTopSongs(country, limit * 2)
        if (ytTracks.isEmpty()) return emptyList()
        return ytTracks.asSequence()
            .filter { it.album.isNotBlank() && !it.album.equals(CHART_PLACEHOLDER_ALBUM, ignoreCase = true) }
            .distinctBy { it.album.trim().lowercase(Locale.ROOT) + "|" + it.artist.trim().lowercase(Locale.ROOT) }
            .map { track ->
                AlbumHit(
                    title = track.album,
                    artist = track.artist,
                    year = track.year,
                    thumbnailUrl = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl },
                    query = "${track.artist} ${track.album}".trim(),
                    browseId = track.albumBrowseId,
                    artistBrowseId = track.artistBrowseIds.firstOrNull().orEmpty(),
                    explicit = track.explicit,
                    releaseDate = track.releaseDate,
                    metadataProvider = "YouTube Music",
                    metadataConfidence = 80,
                    releaseType = ReleaseType.Album
                )
            }
            .take(limit)
            .toList()
    }

    suspend fun trendingAlbums(country: String, limit: Int = 20): List<AlbumHit> = withContext(Dispatchers.IO) {
        val normalizedCountry = normalizeTrendingCountry(country)
        val safeLimit = limit.coerceIn(1, 50)
        val cacheFile = File(appContext.cacheDir, "apple_albums_$normalizedCountry.json")

        readCachedAlbums(cacheFile, safeLimit, checkTtl = true)?.let { return@withContext it }
        fetchAppleTrendingAlbums(normalizedCountry, safeLimit, cacheFile)?.let { return@withContext it }
        readCachedAlbums(cacheFile, safeLimit, checkTtl = false)?.let { return@withContext it }
        fetchFallbackTrendingAlbums(normalizedCountry, safeLimit)
    }

    fun cachedTrendingAlbums(country: String, limit: Int = 20): List<AlbumHit> {
        val normalizedCountry = normalizeTrendingCountry(country)
        val safeLimit = limit.coerceIn(1, 50)
        val cacheFile = File(appContext.cacheDir, "apple_albums_$normalizedCountry.json")
        return readCachedAlbums(cacheFile, safeLimit, checkTtl = false).orEmpty()
    }

    fun isTrendingAlbumsFresh(country: String): Boolean {
        val normalizedCountry = normalizeTrendingCountry(country)
        val cacheFile = File(appContext.cacheDir, "apple_albums_$normalizedCountry.json")
        return cacheFile.exists() && System.currentTimeMillis() - cacheFile.lastModified() < APPLE_TRENDING_CACHE_TTL_MS
    }

    private fun readCachedArtistCandidates(file: File, limit: Int, checkTtl: Boolean): List<TrendingArtistCandidate>? {
        if (!file.exists()) return null
        if (checkTtl && System.currentTimeMillis() - file.lastModified() >= APPLE_TRENDING_CACHE_TTL_MS) return null
        val cachedJson = runCatching { file.readText() }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null
        return AppleTrendingParser.extractTrendingArtistCandidates(cachedJson, limit).takeIf { it.isNotEmpty() }
    }

    private suspend fun fetchAppleTrendingArtistCandidates(country: String, limit: Int, cacheFile: File): List<TrendingArtistCandidate>? {
        val primaryUrl = "https://rss.marketingtools.apple.com/api/v2/$country/music/most-played/100/songs.json"
        val fallbackUrl = "https://rss.applemarketingtools.com/api/v2/$country/music/most-played/100/songs.json"
        val body = (httpGet(primaryUrl) ?: httpGet(fallbackUrl))?.takeIf { it.isNotBlank() } ?: return null
        val parsed = AppleTrendingParser.extractTrendingArtistCandidates(body, limit)
        if (parsed.isNotEmpty()) {
            runCatching { cacheFile.writeText(body) }
            return parsed
        }
        return null
    }

    private suspend fun fetchFallbackTrendingArtistCandidates(country: String, limit: Int): List<TrendingArtistCandidate> {
        val editorialTracks = editorialCharts.cachedTopTracks(country, 100)
            .ifEmpty { editorialCharts.newReleaseTracks(country, 100) }
        if (editorialTracks.isNotEmpty()) {
            val candidates = AppleTrendingParser.extractTrackArtistCandidates(editorialTracks, limit)
            if (candidates.isNotEmpty()) return candidates
        }

        val deezerBody = httpGet("https://api.deezer.com/chart/0/tracks?limit=100")
        if (!deezerBody.isNullOrBlank()) {
            val deezerCandidates = AppleTrendingParser.extractDeezerArtistCandidates(deezerBody, limit)
            if (deezerCandidates.isNotEmpty()) return deezerCandidates
        }

        val ytTracks = fetchYoutubeTopSongs(country, 100)
        if (ytTracks.isNotEmpty()) {
            val ytCandidates = AppleTrendingParser.extractTrackArtistCandidates(ytTracks, limit)
            if (ytCandidates.isNotEmpty()) return ytCandidates
        }

        return emptyList()
    }

    suspend fun trendingArtistCandidates(country: String, limit: Int = 15): List<TrendingArtistCandidate> = withContext(Dispatchers.IO) {
        val normalizedCountry = normalizeTrendingCountry(country)
        val safeLimit = limit.coerceIn(1, 50)
        val cacheFile = File(appContext.cacheDir, "apple_songs_$normalizedCountry.json")

        readCachedArtistCandidates(cacheFile, safeLimit, checkTtl = true)?.let { return@withContext it }
        fetchAppleTrendingArtistCandidates(normalizedCountry, safeLimit, cacheFile)?.let { return@withContext it }
        readCachedArtistCandidates(cacheFile, safeLimit, checkTtl = false)?.let { return@withContext it }
        fetchFallbackTrendingArtistCandidates(normalizedCountry, safeLimit)
    }

    fun cachedTrendingArtists(country: String, limit: Int = 15): List<ArtistHit> {
        val normalizedCountry = normalizeTrendingCountry(country)
        val safeLimit = limit.coerceIn(1, 50)
        val cacheFile = File(appContext.cacheDir, "apple_artists_$normalizedCountry.json")
        if (!cacheFile.exists()) return emptyList()
        val text = runCatching { cacheFile.readText() }.getOrNull() ?: return emptyList()
        return runCatching {
            val array = JSONArray(text)
            val list = ArrayList<ArtistHit>(array.length())
            for (i in 0 until array.length()) {
                if (list.size >= safeLimit) break
                val obj = array.optJSONObject(i) ?: continue
                val name = obj.optString("name")
                val browseId = obj.optString("browseId")
                if (name.isBlank() || browseId.isBlank()) continue
                list.add(
                    ArtistHit(
                        name = name,
                        browseId = browseId,
                        thumbnailUrl = obj.optString("thumbnailUrl"),
                        subscribers = obj.optString("subscribers"),
                        accentStart = obj.optInt("accentStart"),
                        accentEnd = obj.optInt("accentEnd"),
                        officialArtwork = obj.optBoolean("officialArtwork", true)
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    fun saveTrendingArtists(country: String, artists: List<ArtistHit>) {
        val normalizedCountry = country.trim().lowercase(Locale.ROOT).takeIf { it.length == 2 } ?: "it"
        val cacheFile = File(appContext.cacheDir, "apple_artists_$normalizedCountry.json")
        runCatching {
            val array = JSONArray()
            for (artist in artists) {
                val obj = JSONObject()
                obj.put("name", artist.name)
                obj.put("browseId", artist.browseId)
                obj.put("thumbnailUrl", artist.thumbnailUrl)
                obj.put("subscribers", artist.subscribers)
                obj.put("accentStart", artist.accentStart)
                obj.put("accentEnd", artist.accentEnd)
                obj.put("officialArtwork", artist.officialArtwork)
                array.put(obj)
            }
            cacheFile.writeText(array.toString())
        }
    }

    fun isTrendingArtistsFresh(country: String): Boolean {
        val normalizedCountry = country.trim().lowercase(Locale.ROOT).takeIf { it.length == 2 } ?: "it"
        val cacheFile = File(appContext.cacheDir, "apple_artists_$normalizedCountry.json")
        return cacheFile.exists() && System.currentTimeMillis() - cacheFile.lastModified() < APPLE_TRENDING_CACHE_TTL_MS
    }

    private fun sharedRequest(key: String, country: String, limit: Int): Deferred<List<Track>> {
        inFlight[key]?.let { return it }
        val created = scope.async(start = CoroutineStart.LAZY) { fetchTopSongs(country, limit) }
        val active = inFlight.putIfAbsent(key, created)
        if (active != null) {
            created.cancel()
            return active
        }
        created.invokeOnCompletion { inFlight.remove(key, created) }
        created.start()
        return created
    }

    private suspend fun fetchTopSongs(country: String, limit: Int): List<Track> {
        val ranked = selectTopSongs(country, limit)
        if (ranked.isEmpty()) return ranked
        val index = runCatching { editorialCharts.cachedArtworkIndex() }.getOrDefault(emptyMap())
        return ranked.map { withEditorialArtwork(it, index) ?: it }
    }

    suspend fun officialChartArtwork(tracks: List<Track>, country: String): List<Track> {
        val pending = tracks.filter(::isChartArtworkPlaceholder)
        if (pending.isEmpty()) return tracks
        val index = runCatching { editorialCharts.cachedArtworkIndex() }.getOrDefault(emptyMap())
        val local = pending.map { withEditorialArtwork(it, index) }
        val remote = pending.filterIndexed { position, _ -> local[position] == null }
        val resolved = if (remote.isEmpty()) emptyList() else chartArtworkResolver.enrich(remote, country)
        val upgrades = HashMap<String, Track>(pending.size)
        local.filterNotNull().forEach { upgrades[it.id] = it }
        resolved.filterNot(::isChartArtworkPlaceholder).forEach { upgrades[it.id] = it }
        return tracks.map { upgrades[it.id] ?: it }
    }

    private suspend fun selectTopSongs(country: String, limit: Int): List<Track> = coroutineScope {
        val editorial = editorialCharts.cachedTopTracks(country, limit)
        if (editorial.isNotEmpty()) return@coroutineScope editorial
        editorialCharts.warm()

        val youtube = async { fetchYoutubeTopSongs(country, limit) }
        val youtubeWithinBudget = withTimeoutOrNull(YOUTUBE_PRIMARY_BUDGET_MS) { youtube.await() }
        if (youtubeWithinBudget != null) {
            if (youtubeWithinBudget.isNotEmpty()) return@coroutineScope youtubeWithinBudget
            return@coroutineScope fetchAppleTopSongs(country, limit)
        }

        val apple = async { fetchAppleTopSongs(country, limit) }
        val first = select<Pair<Boolean, List<Track>>> {
            youtube.onAwait { true to it }
            apple.onAwait { false to it }
        }
        if (first.second.isNotEmpty()) {
            if (first.first) apple.cancel() else youtube.cancel()
            return@coroutineScope first.second
        }
        if (first.first) apple.await() else youtube.await()
    }

    private suspend fun fetchYoutubeTopSongs(country: String, limit: Int): List<Track> {
        return try {
            youtubeMusicCharts.topTracks(country, limit)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Timber.w(error, "YouTube Music charts failed for %s", country)
            emptyList()
        }
    }

    private fun isYoutubeMusicChartTrack(track: Track): Boolean {
        return track.source.equals(YOUTUBE_CHART_SOURCE, ignoreCase = true)
    }

    private suspend fun fetchAppleTopSongs(country: String, limit: Int): List<Track> = coroutineScope {
        val winner = CompletableDeferred<List<Track>>()
        val remaining = AtomicInteger(2)
        val modern = dispatchAppleAttempt(country, winner, remaining) {
            fetchAppleModern(country, limit)
        }
        launch {
            withTimeoutOrNull(APPLE_FEED_HEDGE_BUDGET_MS) { modern.join() }
            if (!winner.isCompleted) {
                dispatchAppleAttempt(country, winner, remaining) {
                    fetchAppleClassic(country, limit)
                }
            }
        }
        val result = winner.await()
        coroutineContext.cancelChildren()
        result
    }

    private fun CoroutineScope.dispatchAppleAttempt(
        country: String,
        winner: CompletableDeferred<List<Track>>,
        remaining: AtomicInteger,
        attempt: suspend () -> List<Track>
    ): Job = launch {
        val outcome = try {
            attempt()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Timber.w(error, "Apple chart feed attempt failed for %s", country)
            emptyList()
        }
        when {
            outcome.isNotEmpty() -> winner.complete(outcome)
            remaining.decrementAndGet() == 0 -> winner.complete(emptyList())
        }
    }

    private suspend fun fetchAppleModern(country: String, limit: Int): List<Track> {
        val url = "https://rss.marketingtools.apple.com/api/v2/$country/music/most-played/$limit/songs.json"
        val body = httpGet(url) ?: return emptyList()
        return ChartFeedParser.modern(body, limit)
    }

    private suspend fun fetchAppleClassic(country: String, limit: Int): List<Track> {
        val url = "https://itunes.apple.com/$country/rss/topsongs/limit=$limit/json"
        val body = httpGet(url) ?: return emptyList()
        return ChartFeedParser.classic(body, limit)
    }

    private suspend fun httpGet(url: String): String? = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "Levyra/${BuildConfig.VERSION_NAME} Android")
            .cacheControl(FEED_CACHE_CONTROL)
            .build()
        val call = httpClient.newCall(request)
        val completed = AtomicBoolean(false)
        continuation.invokeOnCancellation {
            completed.set(true)
            call.cancel()
        }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (completed.compareAndSet(false, true)) continuation.resume(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = runCatching {
                    response.use { current ->
                        if (!current.isSuccessful) null else current.body.string().takeIf { it.isNotBlank() }
                    }
                }.getOrNull()
                if (completed.compareAndSet(false, true)) continuation.resume(body)
            }
        })
    }

    private fun artworkMatchScore(
        targetTitle: String,
        targetArtist: String,
        candidateTitle: String,
        candidateArtist: String
    ): Int {
        var score = 0
        score += when {
            candidateTitle == targetTitle -> 90
            candidateTitle.contains(targetTitle) || targetTitle.contains(candidateTitle) -> 58
            else -> 0
        }
        score += when {
            candidateArtist == targetArtist -> 80
            candidateArtist.contains(targetArtist) || targetArtist.contains(candidateArtist) -> 48
            else -> 0
        }
        if (candidateTitle.isBlank() || candidateArtist.isBlank()) score -= 100
        return score
    }

    private companion object {
        const val YOUTUBE_PRIMARY_BUDGET_MS = 3_500L
        const val APPLE_FEED_HEDGE_BUDGET_MS = 900L
        const val FEED_MAX_STALE_MINUTES = 30
        const val YOUTUBE_CHART_SOURCE = "YouTube Music Charts"
        const val APPLE_TRENDING_CACHE_TTL_MS = 6 * 60 * 60 * 1000L

        val FEED_CACHE_CONTROL: CacheControl = CacheControl.Builder()
            .maxStale(FEED_MAX_STALE_MINUTES, TimeUnit.MINUTES)
            .build()
    }
}

internal object ChartFeedParser {

    fun modern(body: String, limit: Int): List<Track> {
        val results = runCatching {
            JSONObject(body).optJSONObject("feed")?.optJSONArray("results")
        }.getOrNull() ?: return emptyList()
        val tracks = ArrayList<Track>(minOf(results.length(), limit).coerceAtLeast(0))
        for (index in 0 until results.length()) {
            if (tracks.size >= limit) break
            val entry = results.optJSONObject(index) ?: continue
            val title = entry.optString("name").trim()
            if (title.isBlank()) continue
            tracks += buildChartTrack(
                title = title,
                artist = entry.optString("artistName").trim(),
                artwork = upgradeArtwork(entry.optString("artworkUrl100"))
            )
        }
        return tracks
    }

    fun classic(body: String, limit: Int): List<Track> {
        val entries = runCatching {
            JSONObject(body).optJSONObject("feed")?.optJSONArray("entry")
        }.getOrNull() ?: return emptyList()
        val tracks = ArrayList<Track>(minOf(entries.length(), limit).coerceAtLeast(0))
        for (index in 0 until entries.length()) {
            if (tracks.size >= limit) break
            val entry = entries.optJSONObject(index) ?: continue
            val title = entry.optJSONObject("im:name")?.optString("label").orEmpty().trim()
            if (title.isBlank()) continue
            val images = entry.optJSONArray("im:image")
            tracks += buildChartTrack(
                title = title,
                artist = entry.optJSONObject("im:artist")?.optString("label").orEmpty().trim(),
                artwork = upgradeArtwork(
                    images?.optJSONObject((images.length() - 1).coerceAtLeast(0))?.optString("label").orEmpty()
                )
            )
        }
        return tracks
    }

    fun upgradeArtwork(url: String): String {
        if (url.isBlank()) return url
        return url.replace(appleArtworkSize, "600x600bb")
    }

    fun normalizeMusicText(value: String): String {
        return value.lowercase()
            .replace(parentheticals, " ")
            .replace(featuredMarkers, " ")
            .replace(videoMarkers, " ")
            .replace(nonMusicCharacters, " ")
            .replace(repeatedWhitespace, " ")
            .trim()
    }

    private fun buildChartTrack(title: String, artist: String, artwork: String): Track {
        val identity = chartIdentity("$title|$artist")
        val palette = palette(identity.seed)
        return Track(
            id = "chart-${identity.id}",
            title = title,
            artist = artist.ifBlank { "Vari artisti" },
            album = "Apple Music Charts",
            durationMs = 0L,
            streamUrl = "",
            videoUrl = "",
            thumbnailUrl = artwork,
            largeThumbnailUrl = artwork,
            source = "Apple Music Charts",
            moodTags = setOf("hit", "chart"),
            energy = 70,
            vocal = 55,
            replayScore = 90,
            cacheScore = 80,
            accentStart = palette.first,
            accentEnd = palette.second
        )
    }

    private fun chartIdentity(value: String): ChartIdentity {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
        val seed = digest.take(4).fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) } and Int.MAX_VALUE
        val id = digest.take(8).joinToString("") { "%02x".format(it) }
        return ChartIdentity(seed = seed, id = id)
    }

    private fun palette(seed: Int): Pair<Int, Int> {
        return palettes[seed % palettes.size]
    }

    private data class ChartIdentity(val seed: Int, val id: String)

    private val appleArtworkSize = Regex("\\d+x\\d+bb")
    private val parentheticals = Regex("""\([^)]*\)|\[[^]]*]""")
    private val featuredMarkers = Regex("""feat\.?|featuring|ft\.?""")
    private val videoMarkers = Regex("""official audio|official video|lyrics?|visuali[sz]er|music video""")
    private val nonMusicCharacters = Regex("""[^a-z0-9àèéìòóùçñäöüß\s]""")
    private val repeatedWhitespace = Regex("""\s+""")

    private val palettes = listOf(
        0xFF00E5FF.toInt() to 0xFF7B42FF.toInt(),
        0xFF1B5CFF.toInt() to 0xFFFF4FD8.toInt(),
        0xFFFF7A18.toInt() to 0xFF8E57FF.toInt(),
        0xFF00D4A6.toInt() to 0xFFFF3B5C.toInt(),
        0xFFFFB000.toInt() to 0xFF00E5FF.toInt()
    )
}

data class TrendingArtistCandidate(
    val name: String,
    val appearances: Int,
    val peakRank: Int
)

internal object AppleTrendingParser {
    private val APPLE_ARTWORK_SIZE = Regex("""\d+x\d+[a-z]*""")
    private val SINGLE_INDICATOR = Regex("""(?i)(?:[-–—]\s*Single\b|\((?:Single|EP)\)|\[(?:Single|EP)\]|\bSingle\s*$)""")
    private val ARTIST_COLLAB_SPLIT = Regex("""(?i)\s+(?:feat(?:uring)?\.?|ft\.?|with|con|vs\.?)\s+|,\s*|;\s*|\s+[x×/]\s+|\s+&\s+""")
    private val YEAR_EXTRACTOR = Regex("""^(\d{4})""")
    private val ARTIST_LEADING_ARTICLES = setOf(
        "the", "il", "lo", "la", "i", "gli", "le", "el", "los", "las", "der", "die", "das"
    )

    private val KNOWN_COMPOUND_ARTISTS = setOf(
        "tyler, the creator",
        "mumford & sons",
        "earth, wind & fire",
        "simon & garfunkel",
        "kool & the gang",
        "crosby, stills, nash & young",
        "crosby, stills & nash",
        "bob marley & the wailers",
        "tom petty and the heartbreakers",
        "huey lewis & the news",
        "hall & oates",
        "daryl hall & john oates",
        "florence + the machine",
        "florence and the machine",
        "king gizzard & the lizard wizard",
        "hootie & the blowfish",
        "brooks & dunn",
        "dan + shay",
        "sleeping with sirens",
        "we came as romans",
        "ac/dc"
    )

    fun splitArtistNames(rawArtist: String): List<String> {
        val clean = rawArtist.trim()
        if (clean.isBlank()) return emptyList()

        val normalized = clean.lowercase(Locale.ROOT)
        if (normalized in KNOWN_COMPOUND_ARTISTS) {
            return listOf(clean)
        }

        var protected = clean
        val replacements = ArrayList<Pair<String, String>>()
        for ((idx, known) in KNOWN_COMPOUND_ARTISTS.withIndex()) {
            while (true) {
                val matchIndex = protected.indexOf(known, ignoreCase = true)
                if (matchIndex == -1) break
                val originalSubstring = protected.substring(matchIndex, matchIndex + known.length)
                val token = "__PRESERVED_ARTIST_${idx}_${replacements.size}__"
                replacements.add(token to originalSubstring)
                protected = protected.replaceRange(matchIndex, matchIndex + known.length, token)
            }
        }

        return ARTIST_COLLAB_SPLIT.split(protected)
            .map { segment ->
                var restored = segment.trim()
                for ((token, original) in replacements) {
                    if (restored.contains(token)) {
                        restored = restored.replace(token, original)
                    }
                }
                restored.trim()
            }
            .filter { it.isNotBlank() && isArtistShelfNameEligible(it) }
    }

    private class ArtistStat(var displayName: String, var appearances: Int, var peakRank: Int)

    private fun rankArtistCandidates(
        songArtists: Sequence<Pair<Int, String>>,
        limit: Int
    ): List<TrendingArtistCandidate> {
        val statsMap = LinkedHashMap<String, ArtistStat>()
        for ((songRank, rawArtist) in songArtists) {
            val segments = splitArtistNames(rawArtist)
            val songSeenKeys = HashSet<String>()
            for (artistName in segments) {
                val key = canonicalArtistKey(artistName)
                if (key.length < 2 || !songSeenKeys.add(key)) continue

                val stat = statsMap[key]
                if (stat == null) {
                    statsMap[key] = ArtistStat(displayName = artistName, appearances = 1, peakRank = songRank)
                } else {
                    stat.appearances += 1
                    if (songRank < stat.peakRank) {
                        stat.peakRank = songRank
                    }
                }
            }
        }

        return statsMap.values.asSequence()
            .sortedWith(
                compareByDescending<ArtistStat> { it.appearances }
                    .thenBy { it.peakRank }
                    .thenBy { it.displayName.lowercase(Locale.ROOT) }
            )
            .map { stat ->
                TrendingArtistCandidate(
                    name = stat.displayName,
                    appearances = stat.appearances,
                    peakRank = stat.peakRank
                )
            }
            .take(limit)
            .toList()
    }
    private fun canonicalArtistKey(name: String): String {
        val key = artistIdentityKey(name)
        val article = key.substringBefore(' ')
        if (article in ARTIST_LEADING_ARTICLES) {
            val remainder = key.substringAfter(' ', "").trim()
            if (remainder.isNotBlank()) return remainder
        }
        return key
    }

    fun parseAlbums(body: String, limit: Int = 20): List<AlbumHit> {
        val results = runCatching {
            JSONObject(body).optJSONObject("feed")?.optJSONArray("results")
        }.getOrNull() ?: return emptyList()

        val albums = ArrayList<AlbumHit>(minOf(results.length(), limit).coerceAtLeast(0))
        val seen = HashSet<String>()

        for (i in 0 until results.length()) {
            if (albums.size >= limit) break
            val item = results.optJSONObject(i) ?: continue
            val rawTitle = item.optString("name").trim()
            val rawArtist = item.optString("artistName").trim()
            if (rawTitle.isBlank() || rawArtist.isBlank()) continue
            val genresArray = item.optJSONArray("genres")
            val genreNames = if (genresArray != null) {
                (0 until genresArray.length()).mapNotNull { gIdx ->
                    genresArray.optJSONObject(gIdx)?.optString("name")?.trim()?.takeIf { it.isNotBlank() }
                }
            } else {
                emptyList()
            }
            if (isSingle(rawTitle, genreNames)) continue

            val identity = artistIdentityKey(rawArtist) + "|" + rawTitle.lowercase(Locale.ROOT)
            if (!seen.add(identity)) continue

            val rawArtwork = item.optString("artworkUrl100").trim()
            val artwork = upgradeArtwork(rawArtwork)
            val releaseDate = item.optString("releaseDate").trim()
            val year = YEAR_EXTRACTOR.find(releaseDate)?.groupValues?.get(1).orEmpty()

            albums.add(
                AlbumHit(
                    title = rawTitle,
                    artist = rawArtist,
                    year = year,
                    thumbnailUrl = artwork,
                    query = "$rawArtist $rawTitle".trim(),
                    browseId = "",
                    artistBrowseId = "",
                    explicit = item.optString("contentAdvisoryRating").equals("Explicit", ignoreCase = true) ||
                        item.optString("contentAdvisoryRating").equals("Explict", ignoreCase = true),
                    releaseDate = releaseDate,
                    metadataProvider = "Apple Music",
                    metadataConfidence = 100,
                    releaseType = ReleaseType.Album
                )
            )
        }
        return albums
    }

    fun isSingle(title: String, genres: List<String> = emptyList()): Boolean {
        if (genres.any { it.equals("Single", ignoreCase = true) }) return true
        return SINGLE_INDICATOR.containsMatchIn(title)
    }

    fun upgradeArtwork(url: String): String {
        if (url.isBlank()) return ""
        return url.replace(APPLE_ARTWORK_SIZE, "1400x1400bb")
    }

    fun extractTrendingArtistCandidates(body: String, limit: Int = 15): List<TrendingArtistCandidate> {
        val results = runCatching {
            JSONObject(body).optJSONObject("feed")?.optJSONArray("results")
        }.getOrNull() ?: return emptyList()

        val songArtists = (0 until results.length()).asSequence().mapNotNull { i ->
            val item = results.optJSONObject(i) ?: return@mapNotNull null
            val rawArtist = item.optString("artistName").trim()
            if (rawArtist.isBlank()) null else (i + 1) to rawArtist
        }
        return rankArtistCandidates(songArtists, limit)
    }

    fun parseDeezerAlbums(body: String, limit: Int = 20): List<AlbumHit> {
        val data = runCatching {
            JSONObject(body).optJSONArray("data")
        }.getOrNull() ?: return emptyList()

        val albums = ArrayList<AlbumHit>(minOf(data.length(), limit).coerceAtLeast(0))
        val seen = HashSet<String>()

        for (i in 0 until data.length()) {
            if (albums.size >= limit) break
            val item = data.optJSONObject(i) ?: continue
            val rawTitle = item.optString("title").trim()
            val artistObj = item.optJSONObject("artist")
            val rawArtist = artistObj?.optString("name")?.trim().orEmpty()
            if (rawTitle.isBlank() || rawArtist.isBlank()) continue
            if (item.optString("record_type").equals("single", ignoreCase = true) || isSingle(rawTitle)) continue

            val identity = artistIdentityKey(rawArtist) + "|" + rawTitle.lowercase(Locale.ROOT)
            if (!seen.add(identity)) continue

            val artwork = item.optString("cover_xl").trim()
                .ifBlank { item.optString("cover_big").trim() }
                .ifBlank { item.optString("cover_medium").trim() }

            albums.add(
                AlbumHit(
                    title = rawTitle,
                    artist = rawArtist,
                    year = "",
                    thumbnailUrl = artwork,
                    query = "$rawArtist $rawTitle".trim(),
                    browseId = "",
                    artistBrowseId = "",
                    explicit = item.optBoolean("explicit_lyrics", false),
                    releaseDate = "",
                    metadataProvider = "Deezer",
                    metadataConfidence = 90,
                    releaseType = ReleaseType.Album
                )
            )
        }
        return albums
    }

    fun extractDeezerArtistCandidates(body: String, limit: Int = 15): List<TrendingArtistCandidate> {
        val data = runCatching {
            JSONObject(body).optJSONArray("data")
        }.getOrNull() ?: return emptyList()

        val songArtists = (0 until data.length()).asSequence().mapNotNull { i ->
            val item = data.optJSONObject(i) ?: return@mapNotNull null
            val rawArtist = item.optJSONObject("artist")?.optString("name")?.trim().orEmpty()
            if (rawArtist.isBlank()) null else (i + 1) to rawArtist
        }
        return rankArtistCandidates(songArtists, limit)
    }

    fun extractTrackArtistCandidates(tracks: List<Track>, limit: Int = 15): List<TrendingArtistCandidate> {
        val songArtists = tracks.asSequence().mapIndexedNotNull { i, track ->
            val rawArtist = track.artist.trim()
            if (rawArtist.isBlank()) null else (i + 1) to rawArtist
        }
        return rankArtistCandidates(songArtists, limit)
    }
}

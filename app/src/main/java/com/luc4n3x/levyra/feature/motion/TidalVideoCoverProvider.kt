package com.luc4n3x.levyra.feature.motion

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

internal interface TidalMotionTransport {
    suspend fun search(query: String, type: String, country: String): JSONObject
    suspend fun album(albumId: String, country: String): JSONObject
}

class TidalVideoCoverProvider internal constructor(
    private val minimumConfidence: Int,
    private val transport: TidalMotionTransport
) : MotionArtworkProvider {
    constructor(
        context: Context,
        minimumConfidence: Int = DEFAULT_MOTION_ARTWORK_MINIMUM_CONFIDENCE
    ) : this(minimumConfidence, OkHttpTidalMotionTransport(context))

    override val id: String = "tidal-video-cover"

    override suspend fun find(identity: MotionTrackIdentity): MotionArtworkProviderResult {
        return try {
            resolve(identity)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.d(error, "Tidal motion provider failed")
            MotionArtworkProviderResult.Failed(error)
        }
    }

    private suspend fun resolve(identity: MotionTrackIdentity): MotionArtworkProviderResult {
        val lookup = TidalMotionLookup()
        val albumFallbackAvailable = identity.album.isNotBlank()
        val fromTracks = search(identity, "TRACKS", lookup, albumFallbackPending = albumFallbackAvailable)
        if (hasEffectiveTidalCandidate(identity, fromTracks, minimumConfidence)) {
            return MotionArtworkProviderResult.Found(fromTracks)
        }
        if (albumFallbackAvailable) {
            val fromAlbums = search(identity, "ALBUMS", lookup, albumFallbackPending = false)
            if (hasEffectiveTidalCandidate(identity, fromAlbums, minimumConfidence)) {
                return MotionArtworkProviderResult.Found(fromAlbums)
            }
        }
        return lookup.missResult()
    }

    private suspend fun search(
        identity: MotionTrackIdentity,
        type: String,
        lookup: TidalMotionLookup,
        albumFallbackPending: Boolean
    ): List<MotionArtworkCandidate> {
        val queries = tidalSearchQueries(identity, type)
        if (queries.isEmpty()) return emptyList()
        val country = countryCode()
        val candidates = ArrayList<MotionArtworkCandidate>()
        for ((index, query) in queries.withIndex()) {
            val hydrationCeiling = tidalHydrationCeiling(
                laterQueries = if (type == "TRACKS") queries.size - 1 - index else 0,
                albumFallbackPending = albumFallbackPending
            )
            val found = try {
                searchQuery(identity, type, query, country, lookup, hydrationCeiling)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Timber.d(error, "Tidal motion %s search failed; continuing", type)
                lookup.recordFailure(error)
                continue
            }
            candidates += found
            if (hasEffectiveTidalCandidate(identity, candidates, minimumConfidence)) break
        }
        return candidates.distinctBy { candidate ->
            listOf(candidate.url, candidate.identity.trackId, candidate.identity.albumId).joinToString("|")
        }
    }

    private suspend fun searchQuery(
        identity: MotionTrackIdentity,
        type: String,
        query: String,
        country: String,
        lookup: TidalMotionLookup,
        hydrationCeiling: Int
    ): List<MotionArtworkCandidate> {
        val root = transport.search(query, type, country)
        val items = findItems(root, type.lowercase(Locale.ROOT)) ?: return emptyList()
        val entries = (0 until items.length())
            .mapNotNull { index -> items.optJSONObject(index)?.let { item -> tidalSearchEntry(item, type) } }
            .filter { entry ->
                tidalRawTrackMetadataCouldMatch(
                    identity,
                    type,
                    entry.title,
                    entry.albumArtists.ifEmpty { entry.artists },
                    entry.albumTitle,
                    entry.isrc
                )
            }
            .sortedByDescending { entry ->
                tidalCandidatePriority(identity, type, entry.title, entry.albumTitle, entry.isrc)
            }
        val candidates = ArrayList<MotionArtworkCandidate>()
        for (entry in entries) {
            var videoCover = entry.videoCover
            var albumTitle = entry.albumTitle
            var albumArtistNames = entry.albumArtists
            var upc = entry.upc
            var releaseDate = entry.releaseDate

            if (videoCover.isBlank()) {
                val details = lookup.hydrate(entry.albumId, hydrationCeiling) { albumId ->
                    fetchAlbum(albumId, country)
                }
                if (details != null) {
                    videoCover = details.videoCover
                    albumTitle = details.title.ifBlank { albumTitle }
                    albumArtistNames = details.artists.ifEmpty { albumArtistNames }
                    upc = details.upc.ifBlank { upc }
                    releaseDate = details.releaseDate.ifBlank { releaseDate }
                }
            }

            if (albumTitle.isBlank()) continue
            val effectiveArtists = albumArtistNames.ifEmpty { entry.artists }
            if (!artistsCompatible(identity.artists, effectiveArtists)) continue
            if (isUnsafeResult(albumTitle)) continue
            val videoUrl = formatVideoUrl(videoCover) ?: continue
            candidates += MotionArtworkCandidate(
                provider = id,
                scope = MotionArtworkScope.ALBUM,
                identity = MotionTrackIdentity(
                    title = if (type == "TRACKS") entry.title else "",
                    artists = effectiveArtists,
                    album = albumTitle,
                    durationMs = entry.durationSeconds * 1000L,
                    isrc = entry.isrc,
                    upc = upc,
                    year = releaseDate.take(4),
                    trackId = entry.trackId,
                    albumId = entry.albumId
                ),
                url = videoUrl,
                mimeType = "video/mp4",
                width = 1280,
                height = 1280,
                expiresAtMs = System.currentTimeMillis() + MOTION_ARTWORK_POSITIVE_TTL_MS
            )
        }
        return candidates
    }

    private suspend fun fetchAlbum(albumId: String, country: String): TidalAlbumMotion {
        val root = transport.album(albumId, country)
        val artists = root.optJSONArray("artists").toStringList("name")
            .ifEmpty {
                listOfNotNull(
                    root.optJSONObject("artist")
                        ?.optString("name")
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                )
            }
        return TidalAlbumMotion(
            title = root.optString("title").trim(),
            artists = artists,
            videoCover = root.optString("videoCover").trim(),
            upc = root.optString("upc").trim(),
            releaseDate = root.optString("releaseDate").trim()
        )
    }

    private fun findItems(value: Any?, key: String): JSONArray? {
        when (value) {
            is JSONObject -> {
                value.optJSONObject(key)?.optJSONArray("items")?.let { return it }
                value.optJSONArray(key)?.let { return it }
                val names = value.keys()
                while (names.hasNext()) {
                    findItems(value.opt(names.next()), key)?.let { return it }
                }
            }
            is JSONArray -> {
                for (index in 0 until value.length()) {
                    findItems(value.opt(index), key)?.let { return it }
                }
            }
        }
        return null
    }

    internal fun formatVideoUrl(id: String): String? {
        val parts = id.split('-')
        if (parts.size != 5 || parts.any { it.isBlank() }) return null
        return "https://resources.tidal.com/videos/${parts.joinToString("/")}/1280x1280.mp4"
    }

    private fun countryCode(): String = Locale.getDefault().country
        .uppercase(Locale.ROOT)
        .takeIf { it.length == 2 }
        ?: "US"

    private fun artistsCompatible(requested: List<String>, returned: List<String>): Boolean =
        tidalArtistsCompatible(requested, returned)

    private fun isUnsafeResult(album: String): Boolean {
        val normalized = normalizeMotionText(album)
        return BLACKLIST.any { motionTextContainsTerm(normalized, it) }
    }

    private companion object {
        val BLACKLIST = setOf("playlist", "set list", "essentials", "dj mix", "mixed", "session")
    }
}

private class OkHttpTidalMotionTransport(context: Context) : TidalMotionTransport {
    private val client: OkHttpClient = LevyraHttpClientFactory.media(context).newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    override suspend fun search(query: String, type: String, country: String): JSONObject {
        val url = "$BASE_URL/search".toHttpUrl().newBuilder()
            .addQueryParameter("query", query)
            .addQueryParameter("limit", "10")
            .addQueryParameter("types", type)
            .addQueryParameter("countryCode", country)
            .build()
        return executeJson(url.toString())
    }

    override suspend fun album(albumId: String, country: String): JSONObject {
        val url = "$BASE_URL/albums/$albumId".toHttpUrl().newBuilder()
            .addQueryParameter("countryCode", country)
            .build()
        return executeJson(url.toString())
    }

    private suspend fun executeJson(url: String): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("X-Tidal-Token", TIDAL_EMBED_TOKEN)
            .header("User-Agent", USER_AGENT)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw TidalRequestException("Tidal HTTP ${response.code}")
                }
                val content = response.body.string()
                runCatching { JSONObject(content) }
                    .getOrElse { throw TidalRequestException("Invalid Tidal response", it) }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: TidalRequestException) {
            throw error
        } catch (error: Exception) {
            throw TidalRequestException("Tidal motion request failed", error)
        }
    }

    private companion object {
        const val BASE_URL = "https://api.tidal.com/v1"
        const val TIDAL_EMBED_TOKEN = "vNVdglQOjFJJGG2U"
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/130 Mobile Safari/537.36"
    }
}

internal fun tidalSearchQueries(identity: MotionTrackIdentity, type: String): List<String> {
    val artist = identity.artists.joinToString(" ")
    val values = when (type) {
        "TRACKS" -> listOf(
            listOf(identity.album, artist, identity.title),
            listOf(identity.title, artist)
        )
        "ALBUMS" -> listOf(
            listOf(identity.album, artist),
            listOf(artist, identity.album)
        )
        else -> emptyList()
    }
    return values
        .map { parts -> parts.filter(String::isNotBlank).joinToString(" ") }
        .filter(String::isNotBlank)
        .distinct()
        .take(MAX_TIDAL_SEARCH_QUERIES)
}

internal fun tidalArtistsCompatible(requested: List<String>, returned: List<String>): Boolean =
    primaryMotionArtistMatches(requested, returned)

internal fun hasEffectiveTidalCandidate(
    identity: MotionTrackIdentity,
    candidates: List<MotionArtworkCandidate>,
    minimumConfidence: Int
): Boolean = candidates.any { candidate ->
    CanonicalTrackMatcher.match(identity, candidate).let { match ->
        match.accepted && match.score >= minimumConfidence
    }
}

internal fun shouldSearchTidalAlbumsAfterTracks(
    identity: MotionTrackIdentity,
    candidates: List<MotionArtworkCandidate>,
    minimumConfidence: Int
): Boolean = !hasEffectiveTidalCandidate(identity, candidates, minimumConfidence)

internal fun tidalRawTrackMetadataCouldMatch(
    identity: MotionTrackIdentity,
    type: String,
    title: String,
    artists: List<String>,
    album: String,
    isrc: String
): Boolean {
    if (!tidalArtistsCompatible(identity.artists, artists)) return false
    val exactIsrc = identity.isrc.isNotBlank() && isrc.isNotBlank() && identity.isrc == isrc
    if (identity.isrc.isNotBlank() && isrc.isNotBlank() && !exactIsrc) return false
    if (exactIsrc) return true
    val albumMayMatch = isUnusableMotionAlbum(identity.album) || tidalTextMayMatch(identity.album, album)
    if (type != "TRACKS") return albumMayMatch
    return albumMayMatch && tidalTextMayMatch(identity.title, title)
}

internal fun tidalCandidatePriority(
    identity: MotionTrackIdentity,
    type: String,
    title: String,
    album: String,
    isrc: String
): Int {
    var priority = 0
    if (identity.isrc.isNotBlank() && identity.isrc == isrc) priority += 100
    if (type == "TRACKS") priority += tidalTextAffinity(identity.title, title) * 2
    if (!isUnusableMotionAlbum(identity.album)) priority += tidalTextAffinity(identity.album, album)
    return priority
}

internal fun tidalHydrationCeiling(laterQueries: Int, albumFallbackPending: Boolean): Int {
    val reserved = laterQueries.coerceAtLeast(0) +
        if (albumFallbackPending) TIDAL_ALBUM_FALLBACK_HYDRATION_RESERVE else 0
    return (MAX_TIDAL_ALBUM_HYDRATIONS - reserved).coerceAtLeast(1)
}

internal data class TidalAlbumMotion(
    val title: String,
    val artists: List<String>,
    val videoCover: String,
    val upc: String,
    val releaseDate: String
)

internal class TidalMotionLookup(
    private val maximumHydrations: Int = MAX_TIDAL_ALBUM_HYDRATIONS
) {
    private val hydrated = LinkedHashMap<String, TidalAlbumMotion?>()

    var failure: Throwable? = null
        private set

    suspend fun hydrate(
        albumId: String,
        ceiling: Int,
        fetch: suspend (String) -> TidalAlbumMotion
    ): TidalAlbumMotion? {
        if (albumId.isBlank()) return null
        if (albumId in hydrated) return hydrated[albumId]
        if (hydrated.size >= minOf(ceiling, maximumHydrations)) return null
        val details = try {
            fetch(albumId)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.d(error, "Tidal album hydration failed albumId=%s; continuing", albumId)
            recordFailure(error)
            null
        }
        hydrated[albumId] = details
        return details
    }

    fun recordFailure(error: Throwable) {
        failure = error
    }

    fun missResult(): MotionArtworkProviderResult =
        failure?.let { MotionArtworkProviderResult.Failed(it) } ?: MotionArtworkProviderResult.NoMatch
}

private data class TidalSearchEntry(
    val title: String,
    val artists: List<String>,
    val albumId: String,
    val albumTitle: String,
    val albumArtists: List<String>,
    val videoCover: String,
    val upc: String,
    val releaseDate: String,
    val isrc: String,
    val durationSeconds: Long,
    val trackId: String
)

private fun tidalSearchEntry(item: JSONObject, type: String): TidalSearchEntry {
    val artists = item.optJSONArray("artists").toStringList("name")
        .ifEmpty {
            listOfNotNull(
                item.optJSONObject("artist")
                    ?.optString("name")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            )
        }
    val albumObject = if (type == "TRACKS") item.optJSONObject("album") else item
    return TidalSearchEntry(
        title = item.optString("title").trim(),
        artists = artists,
        albumId = albumObject?.optString("id").orEmpty().trim(),
        albumTitle = albumObject?.optString("title").orEmpty().trim(),
        albumArtists = albumObject?.optJSONArray("artists").toStringList("name"),
        videoCover = albumObject?.optString("videoCover").orEmpty().trim(),
        upc = albumObject?.optString("upc").orEmpty().trim(),
        releaseDate = albumObject?.optString("releaseDate").orEmpty().trim(),
        isrc = item.optString("isrc").uppercase(Locale.ROOT),
        durationSeconds = item.optLong("duration", 0L),
        trackId = item.optString("id")
    )
}

private class TidalRequestException(message: String, cause: Throwable? = null) : IOException(message, cause)

private const val MAX_TIDAL_SEARCH_QUERIES = 2
private const val MAX_TIDAL_ALBUM_HYDRATIONS = 3
private const val TIDAL_ALBUM_FALLBACK_HYDRATION_RESERVE = 1

private fun tidalTextMayMatch(reference: String, candidate: String): Boolean {
    val referenceTokens = tidalTextTokens(reference)
    val candidateTokens = tidalTextTokens(candidate)
    return referenceTokens.isEmpty() || candidateTokens.isEmpty() || referenceTokens.intersect(candidateTokens).isNotEmpty()
}

private fun tidalTextAffinity(reference: String, candidate: String): Int {
    val left = normalizeMotionText(reference)
    val right = normalizeMotionText(candidate)
    if (left.isBlank() || right.isBlank()) return 0
    if (left == right) return 30
    val leftTokens = tidalTextTokens(left)
    val rightTokens = tidalTextTokens(right)
    if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0
    val shared = leftTokens.intersect(rightTokens).size
    return (40 * shared) / (leftTokens.size + rightTokens.size)
}

private fun tidalTextTokens(value: String): Set<String> =
    normalizeMotionText(value).split(' ').filter(String::isNotBlank).toSet()

private fun JSONArray?.toStringList(key: String): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val value = optJSONObject(index)?.optString(key).orEmpty().trim()
            if (value.isNotBlank()) add(value)
        }
    }
}

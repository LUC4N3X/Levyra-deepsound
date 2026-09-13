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

class TidalVideoCoverProvider(
    context: Context,
    private val minimumConfidence: Int = DEFAULT_MOTION_ARTWORK_MINIMUM_CONFIDENCE
) : MotionArtworkProvider {
    override val id: String = "tidal-video-cover"

    private val client: OkHttpClient = LevyraHttpClientFactory.media(context).newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    override suspend fun find(identity: MotionTrackIdentity): MotionArtworkProviderResult {
        return try {
            val hydrationBudget = TidalAlbumHydrationBudget()
            val fromTracks = search(identity, "TRACKS", hydrationBudget)
            if (hasEffectiveTidalCandidate(identity, fromTracks, minimumConfidence)) {
                MotionArtworkProviderResult.Found(fromTracks)
            } else if (identity.album.isBlank()) {
                MotionArtworkProviderResult.NoMatch
            } else {
                val fromAlbums = search(identity, "ALBUMS", hydrationBudget)
                if (hasEffectiveTidalCandidate(identity, fromAlbums, minimumConfidence)) {
                    MotionArtworkProviderResult.Found(fromAlbums)
                } else {
                    MotionArtworkProviderResult.NoMatch
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.d(error, "Tidal motion provider failed")
            MotionArtworkProviderResult.Failed(error)
        }
    }

    private suspend fun search(
        identity: MotionTrackIdentity,
        type: String,
        hydrationBudget: TidalAlbumHydrationBudget
    ): List<MotionArtworkCandidate> {
        val queries = tidalSearchQueries(identity, type)
        if (queries.isEmpty()) return emptyList()
        val country = countryCode()
        val candidates = ArrayList<MotionArtworkCandidate>()
        for (query in queries) {
            candidates += searchQuery(identity, type, query, country, hydrationBudget)
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
        hydrationBudget: TidalAlbumHydrationBudget
    ): List<MotionArtworkCandidate> {
        val url = "$BASE_URL/search".toHttpUrl().newBuilder()
            .addQueryParameter("query", query)
            .addQueryParameter("limit", "10")
            .addQueryParameter("types", type)
            .addQueryParameter("countryCode", country)
            .build()
        val root = executeJson(
            Request.Builder()
                .url(url)
                .header("X-Tidal-Token", TIDAL_EMBED_TOKEN)
                .header("User-Agent", USER_AGENT)
                .build()
        )
        val items = findItems(root, type.lowercase(Locale.ROOT)) ?: return emptyList()
        val candidates = ArrayList<MotionArtworkCandidate>()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val resultTitle = item.optString("title").trim()
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
            val albumId = albumObject?.optString("id").orEmpty().trim()
            var albumTitle = albumObject?.optString("title").orEmpty().trim()
            var albumArtistNames = albumObject?.optJSONArray("artists").toStringList("name")
            var videoCover = albumObject?.optString("videoCover").orEmpty().trim()
            var upc = albumObject?.optString("upc").orEmpty().trim()
            var releaseDate = albumObject?.optString("releaseDate").orEmpty().trim()

            val resultIsrc = item.optString("isrc").uppercase(Locale.ROOT)
            val rawArtists = albumArtistNames.ifEmpty { artists }
            if (!tidalRawTrackMetadataCouldMatch(identity, type, resultTitle, rawArtists, albumTitle, albumId, resultIsrc)) continue

            if (videoCover.isBlank() && hydrationBudget.reserve(albumId)) {
                val details = fetchAlbum(albumId, country)
                if (details != null) {
                    videoCover = details.videoCover
                    albumTitle = details.title.ifBlank { albumTitle }
                    albumArtistNames = details.artists.ifEmpty { albumArtistNames }
                    upc = details.upc.ifBlank { upc }
                    releaseDate = details.releaseDate.ifBlank { releaseDate }
                }
            }

            if (albumTitle.isBlank()) continue
            val effectiveArtists = albumArtistNames.ifEmpty { artists }
            if (!artistsCompatible(identity.artists, effectiveArtists)) continue
            if (isUnsafeResult(albumTitle)) continue
            val videoUrl = formatVideoUrl(videoCover) ?: continue
            candidates += MotionArtworkCandidate(
                provider = id,
                scope = MotionArtworkScope.ALBUM,
                identity = MotionTrackIdentity(
                    title = if (type == "TRACKS") resultTitle else "",
                    artists = effectiveArtists,
                    album = albumTitle,
                    durationMs = item.optLong("duration", 0L) * 1000L,
                    isrc = resultIsrc,
                    upc = upc,
                    year = releaseDate.take(4),
                    trackId = item.optString("id"),
                    albumId = albumId
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

    private suspend fun fetchAlbum(albumId: String, country: String): TidalAlbumMotion? {
        val url = "$BASE_URL/albums/$albumId".toHttpUrl().newBuilder()
            .addQueryParameter("countryCode", country)
            .build()
        val root = executeJson(
            Request.Builder()
                .url(url)
                .header("X-Tidal-Token", TIDAL_EMBED_TOKEN)
                .header("User-Agent", USER_AGENT)
                .build()
        )
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

    private suspend fun executeJson(request: Request): JSONObject = withContext(Dispatchers.IO) {
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

    private data class TidalAlbumMotion(
        val title: String,
        val artists: List<String>,
        val videoCover: String,
        val upc: String,
        val releaseDate: String
    )

    private companion object {
        const val BASE_URL = "https://api.tidal.com/v1"
        const val TIDAL_EMBED_TOKEN = "vNVdglQOjFJJGG2U"
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/130 Mobile Safari/537.36"
        val BLACKLIST = setOf("playlist", "set list", "essentials", "dj mix", "mixed", "session")
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
    albumId: String,
    isrc: String
): Boolean {
    if (!tidalArtistsCompatible(identity.artists, artists)) return false
    val exactIsrc = identity.isrc.isNotBlank() && isrc.isNotBlank() && identity.isrc == isrc
    if (identity.isrc.isNotBlank() && isrc.isNotBlank() && !exactIsrc) return false
    if (type != "TRACKS") return true
    if (!exactIsrc && !tidalTextMayMatch(identity.title, title)) return false
    return albumId.isNotBlank() ||
        exactIsrc ||
        isUnusableMotionAlbum(identity.album) ||
        tidalTextMayMatch(identity.album, album)
}

internal class TidalAlbumHydrationBudget(
    private val maximum: Int = MAX_TIDAL_ALBUM_HYDRATIONS
) {
    private val albumIds = linkedSetOf<String>()

    fun reserve(albumId: String): Boolean {
        if (albumId.isBlank() || albumId in albumIds || albumIds.size >= maximum) return false
        albumIds += albumId
        return true
    }
}

private class TidalRequestException(message: String, cause: Throwable? = null) : IOException(message, cause)

private const val MAX_TIDAL_SEARCH_QUERIES = 2
private const val MAX_TIDAL_ALBUM_HYDRATIONS = 3

private fun tidalTextMayMatch(reference: String, candidate: String): Boolean {
    val referenceTokens = normalizeMotionText(reference).split(' ').filter(String::isNotBlank).toSet()
    val candidateTokens = normalizeMotionText(candidate).split(' ').filter(String::isNotBlank).toSet()
    return referenceTokens.isEmpty() || candidateTokens.isEmpty() || referenceTokens.intersect(candidateTokens).isNotEmpty()
}

private fun JSONArray?.toStringList(key: String): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val value = optJSONObject(index)?.optString(key).orEmpty().trim()
            if (value.isNotBlank()) add(value)
        }
    }
}

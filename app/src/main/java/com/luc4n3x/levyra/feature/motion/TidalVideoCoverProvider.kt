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
import java.util.Locale
import java.util.concurrent.TimeUnit

class TidalVideoCoverProvider(context: Context) : MotionArtworkProvider {
    override val id: String = "tidal-video-cover"

    private val client: OkHttpClient = LevyraHttpClientFactory.media(context).newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    override suspend fun find(identity: MotionTrackIdentity): List<MotionArtworkCandidate> {
        val fromTracks = search(identity, "TRACKS")
        if (fromTracks.isNotEmpty()) return fromTracks
        if (identity.album.isBlank()) return emptyList()
        return search(identity, "ALBUMS")
    }

    private suspend fun search(identity: MotionTrackIdentity, type: String): List<MotionArtworkCandidate> {
        val query = if (type == "TRACKS") {
            listOf(identity.title, identity.artists.joinToString(" "), identity.album)
        } else {
            listOf(identity.album, identity.artists.joinToString(" "))
        }.filter { it.isNotBlank() }.joinToString(" ")
        if (query.isBlank()) return emptyList()
        val country = countryCode()
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
        ) ?: return emptyList()
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
            if (!artistsCompatible(identity.artists, artists)) continue
            if (type == "TRACKS" && normalizeMotionText(resultTitle) != normalizeMotionText(identity.title)) continue
            if (type == "ALBUMS" && normalizeMotionText(resultTitle) != normalizeMotionText(identity.album)) continue

            val albumObject = if (type == "TRACKS") item.optJSONObject("album") else item
            val albumId = albumObject?.optString("id").orEmpty().trim()
            var albumTitle = albumObject?.optString("title").orEmpty().trim()
            var albumArtistNames = albumObject?.optJSONArray("artists").toStringList("name")
            var videoCover = albumObject?.optString("videoCover").orEmpty().trim()
            var upc = albumObject?.optString("upc").orEmpty().trim()
            var releaseDate = albumObject?.optString("releaseDate").orEmpty().trim()

            if (videoCover.isBlank() && albumId.isNotBlank()) {
                val details = fetchAlbum(albumId, country)
                if (details != null) {
                    videoCover = details.videoCover
                    albumTitle = details.title.ifBlank { albumTitle }
                    albumArtistNames = details.artists.ifEmpty { albumArtistNames }
                    upc = details.upc.ifBlank { upc }
                    releaseDate = details.releaseDate.ifBlank { releaseDate }
                }
            }

            if (albumTitle.isBlank() || normalizeMotionText(albumTitle) != normalizeMotionText(identity.album)) continue
            val effectiveArtists = albumArtistNames.ifEmpty { artists }
            if (!artistsCompatible(identity.artists, effectiveArtists)) continue
            if (isUnsafeResult(albumTitle)) continue
            val videoUrl = formatVideoUrl(videoCover) ?: continue
            candidates += MotionArtworkCandidate(
                provider = id,
                scope = MotionArtworkScope.ALBUM,
                identity = MotionTrackIdentity(
                    title = identity.title,
                    artists = effectiveArtists,
                    album = albumTitle,
                    durationMs = item.optLong("duration", 0L) * 1000L,
                    isrc = item.optString("isrc").uppercase(Locale.ROOT),
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
        ) ?: return null
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

    private suspend fun executeJson(request: Request): JSONObject? = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                runCatching { JSONObject(response.body.string()) }.getOrNull()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.d(error, "Tidal motion request failed")
            null
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

    private fun artistsCompatible(requested: List<String>, returned: List<String>): Boolean {
        if (requested.isEmpty() || returned.isEmpty()) return false
        val normalized = returned.map(::normalizeMotionText).toSet()
        return normalizeMotionText(requested.first()) in normalized
    }

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

private fun JSONArray?.toStringList(key: String): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val value = optJSONObject(index)?.optString(key).orEmpty().trim()
            if (value.isNotBlank()) add(value)
        }
    }
}

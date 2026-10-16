package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.artistIdentityKey
import com.luc4n3x.levyra.domain.artistIdentityMatches
import com.luc4n3x.levyra.feature.motion.TIDAL_EMBED_TOKEN
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private val TIDAL_PICTURE_ID = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

private fun artistNamesMatch(candidate: String, artistName: String): Boolean =
    candidate.isNotBlank() &&
        (artistIdentityKey(candidate) == artistIdentityKey(artistName) || artistIdentityMatches(candidate, artistName))

private fun isHttpsHost(rawUrl: String, domain: String): Boolean {
    val url = rawUrl.toHttpUrlOrNull() ?: return false
    if (!url.isHttps) return false
    val host = url.host.lowercase(Locale.ROOT)
    return host == domain || host.endsWith(".$domain")
}

internal fun parseDeezerArtistPortrait(root: JSONObject, artistName: String): String {
    val items = root.optJSONArray("data") ?: return ""
    for (index in 0 until items.length()) {
        val item = items.optJSONObject(index) ?: continue
        if (!artistNamesMatch(item.optString("name").trim(), artistName)) continue
        val picture = item.optString("picture_xl").trim().ifBlank { item.optString("picture_big").trim() }
        if (!isHttpsHost(picture, "dzcdn.net")) continue
        if (picture.contains("/artist//")) continue
        return picture
    }
    return ""
}

internal fun parseTidalArtistPortrait(root: JSONObject, artistName: String): String {
    val items = root.optJSONObject("artists")?.optJSONArray("items") ?: return ""
    for (index in 0 until items.length()) {
        val item = items.optJSONObject(index) ?: continue
        if (!artistNamesMatch(item.optString("name").trim(), artistName)) continue
        val pictureId = item.optString("picture").trim()
        if (!TIDAL_PICTURE_ID.matches(pictureId)) continue
        return "https://resources.tidal.com/images/${pictureId.replace('-', '/')}/750x750.jpg"
    }
    return ""
}

internal class ArtistPortraitFallbackRepository private constructor(context: Context) {
    private val youtube = YoutubeMusicRepository(context.applicationContext)
    private val client = LevyraHttpClientFactory.externalIntegrations()
    private val memoryCache = ConcurrentHashMap<String, String>()

    suspend fun youtubePortrait(artistName: String): String = cached("youtube", artistName) { name ->
        youtube.searchEverything(name).artists
            .firstOrNull { artist -> artistNamesMatch(artist.name, name) && isYoutubePortrait(artist.thumbnailUrl) }
            ?.thumbnailUrl
            ?.let(::fullResolutionArtworkUrl)
            .orEmpty()
    }

    suspend fun deezerPortrait(artistName: String): String = cached("deezer", artistName) { name ->
        val url = DEEZER_ARTIST_SEARCH_URL.toHttpUrl().newBuilder()
            .addQueryParameter("q", name)
            .addQueryParameter("limit", SEARCH_LIMIT.toString())
            .build()
        requestJson(url)?.let { parseDeezerArtistPortrait(it, name) }.orEmpty()
    }

    suspend fun tidalPortrait(artistName: String): String = cached("tidal", artistName) { name ->
        val url = TIDAL_SEARCH_URL.toHttpUrl().newBuilder()
            .addQueryParameter("query", name)
            .addQueryParameter("limit", SEARCH_LIMIT.toString())
            .addQueryParameter("types", "ARTISTS")
            .addQueryParameter("countryCode", tidalCountryCode())
            .build()
        requestJson(url, TIDAL_EMBED_TOKEN)?.let { parseTidalArtistPortrait(it, name) }.orEmpty()
    }

    private suspend fun cached(
        provider: String,
        artistName: String,
        lookup: suspend (String) -> String
    ): String = withContext(Dispatchers.IO) {
        val cleanName = artistName.trim()
        val identity = artistIdentityKey(cleanName)
        if (identity.isBlank()) return@withContext ""
        val key = "$provider:$identity"
        memoryCache[key]?.let { return@withContext it }
        val resolved = try {
            lookup(cleanName)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            ""
        }
        if (resolved.isNotBlank()) memoryCache[key] = resolved
        resolved
    }

    private fun requestJson(url: HttpUrl, tidalToken: String? = null): JSONObject? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .apply { if (tidalToken != null) header("X-Tidal-Token", tidalToken) }
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val payload = response.body
            if (payload.contentLength() > MAX_RESPONSE_BYTES) return null
            val source = payload.source()
            source.request(MAX_RESPONSE_BYTES + 1L)
            if (source.buffer.size > MAX_RESPONSE_BYTES) return null
            runCatching { JSONObject(source.buffer.readUtf8()) }.getOrNull()
        }
    }

    private fun isYoutubePortrait(url: String): Boolean =
        isHttpsHost(url, "googleusercontent.com") || isHttpsHost(url, "ggpht.com")

    private fun tidalCountryCode(): String =
        Locale.getDefault().country.uppercase(Locale.ROOT)
            .takeIf { it.length == 2 && it.all { letter -> letter in 'A'..'Z' } }
            ?: "US"

    companion object {
        @Volatile
        private var instance: ArtistPortraitFallbackRepository? = null

        fun get(context: Context): ArtistPortraitFallbackRepository {
            return instance ?: synchronized(this) {
                instance ?: ArtistPortraitFallbackRepository(context.applicationContext).also { instance = it }
            }
        }

        private const val DEEZER_ARTIST_SEARCH_URL = "https://api.deezer.com/search/artist"
        private const val TIDAL_SEARCH_URL = "https://api.tidal.com/v1/search"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/140.0.0.0 Mobile Safari/537.36"
        private const val SEARCH_LIMIT = 5
        private const val MAX_RESPONSE_BYTES = 512_000L
    }
}

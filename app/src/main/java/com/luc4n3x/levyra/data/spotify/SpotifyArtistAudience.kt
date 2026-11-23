package com.luc4n3x.levyra.data.spotify

import com.luc4n3x.levyra.data.artistNameMatches
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

internal class SpotifyArtistAudience(
    private val clientFactory: () -> OkHttpClient = { LevyraHttpClientFactory.externalIntegrations() }
) {
    private val client by lazy {
        clientFactory().newBuilder().callTimeout(4, TimeUnit.SECONDS).build()
    }

    suspend fun monthlyListeners(artistName: String): Long? {
        val name = artistName.trim()
        if (name.isEmpty()) return null
        val key = name.lowercase(Locale.ROOT)
        cache[key]?.takeIf { System.currentTimeMillis() - it.storedAtMs < CACHE_TTL_MS }?.let { return it.listeners }
        val listeners = try {
            withTimeoutOrNull(LOOKUP_TIMEOUT_MS) { lookup(name) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
        if (cache.size >= MAX_CACHE_ENTRIES) cache.clear()
        cache[key] = CachedAudience(listeners, System.currentTimeMillis())
        return listeners
    }

    private suspend fun lookup(name: String): Long? = withContext(Dispatchers.IO) {
        val bearer = accessToken() ?: return@withContext null
        val search = pathfinder(bearer, searchPayload(name)) ?: return@withContext null
        val uri = spotifyArtistUriFor(search, name) ?: return@withContext null
        val overview = pathfinder(bearer, overviewPayload(uri)) ?: return@withContext null
        spotifyMonthlyListeners(overview)
    }

    private suspend fun accessToken(): String? = tokenLock.withLock {
        if (token.isNotEmpty() && System.currentTimeMillis() < tokenExpiresAtMs) return@withLock token
        val request = Request.Builder()
            .url(TOKEN_PAGE_URL)
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Accept", "text/html")
            .build()
        val html = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withLock null
            val body = response.body.source()
            body.request(MAX_TOKEN_PAGE_BYTES)
            body.buffer.readUtf8()
        }
        val fresh = TOKEN_PATTERN.find(html)?.groupValues?.get(1)?.takeIf { it.isNotBlank() } ?: return@withLock null
        val expiresAt = TOKEN_EXPIRY_PATTERN.find(html)?.groupValues?.get(1)?.toLongOrNull()
            ?: (System.currentTimeMillis() + DEFAULT_TOKEN_LIFETIME_MS)
        token = fresh
        tokenExpiresAtMs = expiresAt - TOKEN_EXPIRY_MARGIN_MS
        fresh
    }

    private fun pathfinder(bearer: String, payload: String): JSONObject? {
        val request = Request.Builder()
            .url(PATHFINDER_URL)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $bearer")
            .header("Accept", "application/json")
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            response.body.readSpotifyJsonBody()?.let(::JSONObject)
        }
    }

    private fun searchPayload(name: String): String = JSONObject()
        .put(
            "variables",
            JSONObject()
                .put("searchTerm", name)
                .put("offset", 0)
                .put("limit", SEARCH_LIMIT)
                .put("numberOfTopResults", SEARCH_LIMIT)
                .put("includeAudiobooks", false)
                .put("includePreReleases", false)
        )
        .put("operationName", "searchArtists")
        .put("extensions", persistedQuery(SEARCH_ARTISTS_HASH))
        .toString()

    private fun overviewPayload(uri: String): String = JSONObject()
        .put("variables", JSONObject().put("uri", uri).put("locale", ""))
        .put("operationName", "queryArtistOverview")
        .put("extensions", persistedQuery(ARTIST_OVERVIEW_HASH))
        .toString()

    private fun persistedQuery(hash: String): JSONObject =
        JSONObject().put("persistedQuery", JSONObject().put("version", 1).put("sha256Hash", hash))

    private class CachedAudience(val listeners: Long?, val storedAtMs: Long)

    companion object {
        private val cache = ConcurrentHashMap<String, CachedAudience>()
        private val tokenLock = Mutex()
        @Volatile private var token: String = ""
        @Volatile private var tokenExpiresAtMs: Long = 0L
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val TOKEN_PAGE_URL = "https://open.spotify.com/embed/playlist/37i9dQZEVXbMDoHDwVN2tF"
        private const val PATHFINDER_URL = "https://api-partner.spotify.com/pathfinder/v2/query"
        private const val SEARCH_ARTISTS_HASH = "7bf95d754fdbe32c8b161fbbe54d1ae50974900df4dce4c8f1afcbcad153224d"
        private const val ARTIST_OVERVIEW_HASH = "eafb1620d783ece118e72fbfede2b0064483e029d3c1aafafa1c2de0e25a89e7"
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36"
        private val TOKEN_PATTERN = Regex("\"accessToken\":\"([^\"]+)\"")
        private val TOKEN_EXPIRY_PATTERN = Regex("\"accessTokenExpirationTimestampMs\":(\\d+)")
        private const val MAX_TOKEN_PAGE_BYTES = 512L * 1024L
        private const val DEFAULT_TOKEN_LIFETIME_MS = 30L * 60L * 1_000L
        private const val TOKEN_EXPIRY_MARGIN_MS = 60L * 1_000L
        private const val LOOKUP_TIMEOUT_MS = 8_500L
        private const val CACHE_TTL_MS = 6L * 60L * 60L * 1_000L
        private const val MAX_CACHE_ENTRIES = 256
        private const val SEARCH_LIMIT = 5
    }
}

internal fun spotifyArtistUriFor(search: JSONObject, artistName: String): String? {
    val items = search.optJSONObject("data")?.optJSONObject("searchV2")?.optJSONObject("artists")?.optJSONArray("items")
        ?: return null
    for (index in 0 until items.length()) {
        val data = items.optJSONObject(index)?.optJSONObject("data") ?: continue
        val name = data.optJSONObject("profile")?.optString("name").orEmpty().trim()
        val uri = data.optString("uri").trim()
        if (name.isNotEmpty() && SPOTIFY_ARTIST_URI.matches(uri) && artistNameMatches(artistName, name)) return uri
    }
    return null
}

internal fun spotifyMonthlyListeners(overview: JSONObject): Long? =
    overview.optJSONObject("data")?.optJSONObject("artistUnion")?.optJSONObject("stats")
        ?.optLong("monthlyListeners", -1L)?.takeIf { it > 0L }

private val SPOTIFY_ARTIST_URI = Regex("^spotify:artist:[A-Za-z0-9]{22}$")

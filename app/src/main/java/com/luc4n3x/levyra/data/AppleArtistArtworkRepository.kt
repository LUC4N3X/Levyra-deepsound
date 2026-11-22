package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.artistIdentityKey
import com.luc4n3x.levyra.domain.artistIdentityMatches
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

internal fun isAllowedAppleArtistArtworkUrl(rawUrl: String): Boolean {
    val url = rawUrl.toHttpUrlOrNull() ?: return false
    if (!url.isHttps) return false
    val host = url.host.lowercase(Locale.ROOT)
    return host == "mzstatic.com" || host.endsWith(".mzstatic.com")
}

internal fun isAllowedAppleArtistPageUrl(rawUrl: String): Boolean {
    val url = rawUrl.toHttpUrlOrNull() ?: return false
    if (!url.isHttps) return false
    val host = url.host.lowercase(Locale.ROOT)
    if (host != "music.apple.com") return false
    return url.encodedPath.contains("/artist/", ignoreCase = true)
}

private val APPLE_ARTIST_IMAGE_PATTERN = Regex(
    """https://is\d+-ssl\.mzstatic\.com/image/thumb/[A-Za-z0-9._/-]{10,180}\.(?:png|jpg|jpeg)"""
)

private val APPLE_ARTIST_SIZE_PATTERN = Regex("""\d{2,4}x\d{2,4}(?:cw|bb)""")

private val APPLE_OPEN_GRAPH_IMAGE_PATTERN = Regex(
    """<meta[^>]+property=["']og:image["'][^>]+content=["']([^"']+)["']""",
    RegexOption.IGNORE_CASE
)

private const val APPLE_ARTIST_IMAGE_MARKER = "AMCArtistImages"
private const val APPLE_ARTIST_IMAGE_PREFIX = "https://is"
private const val APPLE_ARTIST_IMAGE_WINDOW = 260
private const val APPLE_OPEN_GRAPH_MARKER = "og:image"
private const val APPLE_OPEN_GRAPH_WINDOW = 4096

internal fun extractAppleArtistPortrait(html: String): String {
    val candidates = appleArtistImageUrls(html).filter(::isAllowedAppleArtistArtworkUrl)
    if (candidates.isEmpty()) return ""
    val openGraph = appleOpenGraphImage(html)
        ?.takeIf(::isAllowedAppleArtistArtworkUrl)
    val portrait = openGraph
        ?: candidates.firstOrNull { it.contains(APPLE_ARTIST_IMAGE_MARKER, ignoreCase = true) }
        ?: return ""
    return APPLE_ARTIST_SIZE_PATTERN.replace(portrait, "1200x1200bb")
}

private fun appleArtistImageUrls(html: String): List<String> {
    val urls = ArrayList<String>()
    var from = 0
    while (true) {
        val start = html.indexOf(APPLE_ARTIST_IMAGE_PREFIX, from)
        if (start < 0) return urls
        val window = html.substring(start, minOf(html.length, start + APPLE_ARTIST_IMAGE_WINDOW))
        val match = APPLE_ARTIST_IMAGE_PATTERN.matchAt(window, 0)
        if (match == null) {
            from = start + 1
        } else {
            urls += match.value
            from = start + match.value.length
        }
    }
}

private fun appleOpenGraphImage(html: String): String? {
    var from = 0
    while (true) {
        val marker = html.indexOf(APPLE_OPEN_GRAPH_MARKER, from, ignoreCase = true)
        if (marker < 0) return null
        val tagStart = metaTagStartBefore(html, marker)
        if (tagStart >= 0) {
            val window = html.substring(tagStart, minOf(html.length, marker + APPLE_OPEN_GRAPH_WINDOW))
            APPLE_OPEN_GRAPH_IMAGE_PATTERN.find(window)?.let { return it.groupValues.getOrNull(1) }
        }
        from = marker + 1
    }
}

private fun metaTagStartBefore(html: String, marker: Int): Int {
    val floor = maxOf(0, marker - APPLE_OPEN_GRAPH_WINDOW)
    var index = marker
    while (index >= floor) {
        if (html[index] == '>') return -1
        if (html.regionMatches(index, "<meta", 0, 5, ignoreCase = true)) return index
        index--
    }
    return -1
}

internal class AppleArtistArtworkRepository private constructor(context: Context) {
    private val client = LevyraHttpClientFactory.externalIntegrations()
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val memoryCache = ConcurrentHashMap<String, String>()

    fun cachedArtistPortrait(artistName: String): String {
        val identity = artistIdentityKey(artistName.trim())
        if (identity.isBlank() || identity in SPOTIFY_PORTRAIT_ARTISTS) return ""
        return memoryCache[identity].orEmpty()
    }

    suspend fun resolveArtistPortrait(artistName: String): String = withContext(Dispatchers.IO) {
        val cleanName = artistName.trim()
        val identity = artistIdentityKey(cleanName)
        if (identity.isBlank() || identity in SPOTIFY_PORTRAIT_ARTISTS) return@withContext ""

        val now = System.currentTimeMillis()
        memoryCache[identity]?.let { return@withContext it }
        readPersisted(identity, now)?.let { cached ->
            rememberPortrait(identity, cached)
            return@withContext cached
        }

        val resolved = runCatching {
            val pageUrl = findArtistPageUrl(cleanName)
            if (pageUrl.isBlank()) "" else portraitFromArtistPage(pageUrl)
        }.getOrNull().orEmpty()

        if (resolved.isNotBlank()) {
            rememberPortrait(identity, resolved)
            preferences.edit()
                .putString("$identity.url", resolved)
                .putLong("$identity.savedAt", now)
                .apply()
        }
        resolved
    }

    private fun rememberPortrait(identity: String, url: String) {
        if (memoryCache.size >= MAX_MEMORY_PORTRAITS) memoryCache.clear()
        memoryCache[identity] = url
    }

    private fun readPersisted(identity: String, now: Long): String? {
        val url = preferences.getString("$identity.url", null).orEmpty()
        val savedAt = preferences.getLong("$identity.savedAt", 0L)
        if (url.isBlank() || now - savedAt !in 0 until ARTWORK_TTL_MS) return null
        if (!isAllowedAppleArtistArtworkUrl(url)) return null
        return url
    }

    private fun findArtistPageUrl(artistName: String): String {
        val url = SEARCH_URL.toHttpUrl().newBuilder()
            .addQueryParameter("term", artistName)
            .addQueryParameter("entity", "musicArtist")
            .addQueryParameter("limit", SEARCH_LIMIT.toString())
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .build()
        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ""
            val payload = response.body
            if (payload.contentLength() > MAX_SEARCH_BYTES) return ""
            val source = payload.source()
            source.request(MAX_SEARCH_BYTES + 1L)
            if (source.buffer.size > MAX_SEARCH_BYTES) return ""
            source.buffer.readUtf8()
        }
        val results = JSONObject(body).optJSONArray("results") ?: return ""
        for (index in 0 until results.length()) {
            val entry = results.optJSONObject(index) ?: continue
            val name = entry.optString("artistName").trim()
            if (artistIdentityKey(name) != artistIdentityKey(artistName) &&
                !artistIdentityMatches(name, artistName)
            ) {
                continue
            }
            val link = entry.optString("artistLinkUrl").trim()
            if (isAllowedAppleArtistPageUrl(link)) return link
        }
        return ""
    }

    private fun portraitFromArtistPage(pageUrl: String): String {
        val request = Request.Builder()
            .url(pageUrl)
            .header("Accept", "text/html")
            .header("User-Agent", USER_AGENT)
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ""
            val payload = response.body
            if (payload.contentLength() > MAX_PAGE_BYTES) return ""
            val source = payload.source()
            source.request(MAX_PAGE_BYTES + 1L)
            if (source.buffer.size > MAX_PAGE_BYTES) return ""
            extractAppleArtistPortrait(source.buffer.readUtf8())
        }
    }

    companion object {
        @Volatile
        private var instance: AppleArtistArtworkRepository? = null

        fun get(context: Context): AppleArtistArtworkRepository {
            return instance ?: synchronized(this) {
                instance ?: AppleArtistArtworkRepository(context.applicationContext).also { instance = it }
            }
        }

        private const val PREFERENCES_NAME = "apple_artist_artwork"
        private val SPOTIFY_PORTRAIT_ARTISTS = setOf("drake")
        private const val SEARCH_URL = "https://itunes.apple.com/search"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/140.0.0.0 Mobile Safari/537.36"
        private const val SEARCH_LIMIT = 5
        private const val MAX_MEMORY_PORTRAITS = 256
        private const val MAX_SEARCH_BYTES = 512_000L
        private const val MAX_PAGE_BYTES = 4_000_000L
        private const val ARTWORK_TTL_MS = 30L * 24L * 60L * 60L * 1000L
    }
}

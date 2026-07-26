package com.luc4n3x.levyra.desktop.core.charts

import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import com.luc4n3x.levyra.desktop.core.model.Track
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class ChartsRepository(
    private val client: OkHttpClient = ExtractorHttp.client,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private val cache = ConcurrentHashMap<String, CachedChart>()

    suspend fun topSongs(country: String, limit: Int = DEFAULT_LIMIT): List<Track> = withContext(dispatcher) {
        val region = normalizeCountry(country)
        val size = limit.coerceIn(10, 100)
        val key = "$region|$size"
        cache[key]?.takeIf { nowMillis() - it.storedAt < CACHE_TTL_MS }?.let { return@withContext it.tracks }

        val modern = fetch(modernUrl(region, size))?.let { ChartFeedParser.modern(it, size) }.orEmpty()
        val tracks = modern.ifEmpty {
            fetch(classicUrl(region, size))?.let { ChartFeedParser.classic(it, size) }.orEmpty()
        }
        if (tracks.isNotEmpty()) {
            cache[key] = CachedChart(tracks = tracks, storedAt = nowMillis())
        }
        tracks
    }

    private fun fetch(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", ExtractorHttp.DESKTOP_USER_AGENT)
            .header("Accept", "application/json")
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                response.body.string()
            }
        }.getOrNull()
    }

    private data class CachedChart(val tracks: List<Track>, val storedAt: Long)

    companion object {
        const val DEFAULT_LIMIT = 50
        private const val CACHE_TTL_MS = 60L * 60L * 1000L

        fun normalizeCountry(country: String): String {
            val trimmed = country.trim().lowercase(Locale.ROOT)
            return if (trimmed.length == 2) trimmed else "it"
        }

        fun modernUrl(country: String, limit: Int): String =
            "https://rss.marketingtools.apple.com/api/v2/$country/music/most-played/$limit/songs.json"

        fun classicUrl(country: String, limit: Int): String =
            "https://itunes.apple.com/$country/rss/topsongs/limit=$limit/json"
    }
}

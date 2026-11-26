package com.luc4n3x.levyra.desktop.core.podcast

import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class PodcastRepository(
    private val client: OkHttpClient = ExtractorHttp.client,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private val shelvesCache = object : LinkedHashMap<String, CachedShelves>(MAX_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedShelves>?): Boolean = size > MAX_CACHE_ENTRIES
    }

    suspend fun shelves(locale: PodcastLocale): List<PodcastShelf> = withContext(dispatcher) {
        cached(locale.cacheKey)?.let { return@withContext it }
        val root = post(BROWSE_ENDPOINT, locale) { put("browseId", "FEmusic_home").put("params", CHIP_PARAMS) }
        PodcastParser.shelves(root).also { shelves ->
            if (shelves.isNotEmpty()) synchronized(shelvesCache) { shelvesCache[locale.cacheKey] = CachedShelves(shelves, nowMillis()) }
        }
    }

    suspend fun search(query: String, locale: PodcastLocale): List<PodcastShow> = withContext(dispatcher) {
        val trimmed = query.trim().take(MAX_QUERY_LENGTH)
        if (trimmed.isEmpty()) return@withContext emptyList()
        PodcastParser.search(post(SEARCH_ENDPOINT, locale) { put("query", trimmed).put("params", SEARCH_PARAMS) })
    }

    suspend fun show(browseId: String, locale: PodcastLocale): PodcastShowPage = withContext(dispatcher) {
        require(isPodcastShowId(browseId)) { "Invalid podcast id" }
        PodcastParser.show(browseId, post(BROWSE_ENDPOINT, locale) { put("browseId", browseId) })
    }

    suspend fun moreEpisodes(continuation: String, locale: PodcastLocale): PodcastEpisodePage = withContext(dispatcher) {
        require(PodcastParser.isContinuationToken(continuation)) { "Invalid continuation" }
        PodcastParser.episodePage(post(BROWSE_ENDPOINT, locale) { put("continuation", continuation) })
    }

    private fun cached(key: String): List<PodcastShelf>? = synchronized(shelvesCache) {
        shelvesCache[key]?.takeIf { nowMillis() - it.storedAt < CACHE_TTL_MS }?.shelves
    }

    private suspend fun post(endpoint: String, locale: PodcastLocale, body: JSONObject.() -> JSONObject): JSONObject {
        val payload = JSONObject()
            .put("context", JSONObject().put("client", JSONObject()
                .put("clientName", "WEB_REMIX")
                .put("clientVersion", CLIENT_VERSION)
                .put("hl", locale.hl)
                .put("gl", locale.gl)))
            .body()
        return runInterruptible {
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", ExtractorHttp.DESKTOP_USER_AGENT)
                .header("Origin", ORIGIN)
                .header("X-YouTube-Client-Name", "67")
                .header("X-YouTube-Client-Version", CLIENT_VERSION)
                .post(payload.toString().toRequestBody(JSON))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Podcasts request failed: ${response.code}")
                val responseBody = response.body
                if (responseBody.contentLength() > MAX_RESPONSE_BYTES) throw IOException("Podcasts response too large")
                val source = responseBody.source()
                if (source.request(MAX_RESPONSE_BYTES + 1)) throw IOException("Podcasts response too large")
                val text = source.buffer.readUtf8()
                if (text.isBlank()) throw IOException("Podcasts response empty")
                JSONObject(text)
            }
        }
    }

    private data class CachedShelves(val shelves: List<PodcastShelf>, val storedAt: Long)

    companion object {
        const val MAX_QUERY_LENGTH = 120
        private const val MAX_RESPONSE_BYTES = 8L * 1024L * 1024L
        private const val MAX_CACHE_ENTRIES = 6
        private const val CACHE_TTL_MS = 30L * 60L * 1000L
        private const val ORIGIN = "https://music.youtube.com"
        private const val BROWSE_ENDPOINT = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
        private const val SEARCH_ENDPOINT = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
        private const val CLIENT_VERSION = "1.20251020.01.00"
        private const val CHIP_PARAMS =
            "ggNCSgQIDBADSgQIBxABSgQICRABSgQICBABSgQIDhABSgQIBBABSgQIDRABSgQIAxABSgQIChABSgQIBhABSgQIBRAB"
        private const val SEARCH_PARAMS = "EgWKAQJQAWoMEA4QChADEAQQCRAF"
        private val JSON = "application/json".toMediaType()
    }
}

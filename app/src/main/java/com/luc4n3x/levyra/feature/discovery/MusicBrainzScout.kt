package com.luc4n3x.levyra.feature.discovery

import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

data class ScoutArtist(val name: String, val area: String, val beginYear: Int?)

data class ScoutRelease(val artist: String, val title: String, val year: Int)

class MusicBrainzScout(private val clock: () -> Long = System::currentTimeMillis) {
    private val gate = Mutex()
    private var lastRequestAtMs = 0L
    private val cache = object : LinkedHashMap<String, String>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > CACHE_ENTRIES
    }

    suspend fun artistsFromArea(area: String, offset: Int): List<ScoutArtist> {
        val clean = area.trim()
        if (clean.length < 2) return emptyList()
        val term = luceneQuote(clean)
        val query = "(beginarea:$term OR area:$term) AND (type:person OR type:group)"
        val body = get("artist", query, offset) ?: return emptyList()
        return MusicBrainzParser.artists(body, clean)
    }

    suspend fun releasesFromDecade(startYear: Int, offset: Int): List<ScoutRelease> {
        val start = startYear / 10 * 10
        val query = "firstreleasedate:[$start TO ${start + 9}] AND primarytype:album AND status:official"
        val body = get("release-group", query, offset) ?: return emptyList()
        return MusicBrainzParser.releaseGroups(body, start)
    }

    private suspend fun get(entity: String, query: String, offset: Int): String? {
        val url = "$BASE/$entity?fmt=json&limit=$PAGE&offset=${offset.coerceIn(0, MAX_OFFSET)}&query=" +
            URLEncoder.encode(query, StandardCharsets.UTF_8.name())
        synchronized(cache) { cache[url] }?.let { return it }
        return gate.withLock {
            val wait = MIN_INTERVAL_MS - (clock() - lastRequestAtMs)
            if (wait > 0L) delay(wait)
            lastRequestAtMs = clock()
            withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Levyra/${BuildConfig.VERSION_NAME} ( https://github.com/LUC4N3X/Levyra-deepsound )")
                    .header("Accept", "application/json")
                    .build()
                LevyraHttpClientFactory.externalIntegrations().newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    response.body?.string()
                }
            }?.also { body -> synchronized(cache) { cache[url] = body } }
        }
    }

    private fun luceneQuote(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private companion object {
        const val BASE = "https://musicbrainz.org/ws/2"
        const val PAGE = 40
        const val MAX_OFFSET = 400
        const val MIN_INTERVAL_MS = 1_100L
        const val CACHE_ENTRIES = 24
    }
}

internal object MusicBrainzParser {
    private const val MIN_SCORE = 70

    fun artists(body: String, area: String): List<ScoutArtist> {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val array = root.optJSONArray("artists") ?: return emptyList()
        val wanted = area.lowercase()
        val result = ArrayList<ScoutArtist>(array.length())
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            if (item.optInt("score", 0) < MIN_SCORE) continue
            val name = item.optString("name").trim()
            if (name.length < 2) continue
            val areas = listOfNotNull(
                item.optJSONObject("begin-area")?.optString("name"),
                item.optJSONObject("area")?.optString("name")
            ).map { it.trim() }
            val matched = areas.firstOrNull { it.lowercase() == wanted } ?: continue
            val begin = item.optJSONObject("life-span")?.optString("begin")?.take(4)?.toIntOrNull()
            result += ScoutArtist(name, matched, begin)
        }
        return result.distinctBy { it.name.lowercase() }
    }

    fun releaseGroups(body: String, decadeStart: Int): List<ScoutRelease> {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val array = root.optJSONArray("release-groups") ?: return emptyList()
        val result = ArrayList<ScoutRelease>(array.length())
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            if (item.optInt("score", 0) < MIN_SCORE) continue
            val year = item.optString("first-release-date").take(4).toIntOrNull() ?: continue
            if (year !in decadeStart..decadeStart + 9) continue
            val title = item.optString("title").trim()
            val credits = item.optJSONArray("artist-credit") ?: continue
            val artist = (0 until credits.length()).mapNotNull { credits.optJSONObject(it)?.optString("name")?.trim() }
                .firstOrNull { it.isNotEmpty() && !it.equals("Various Artists", ignoreCase = true) } ?: continue
            if (title.isEmpty()) continue
            result += ScoutRelease(artist, title, year)
        }
        return result.distinctBy { it.artist.lowercase() }
    }
}

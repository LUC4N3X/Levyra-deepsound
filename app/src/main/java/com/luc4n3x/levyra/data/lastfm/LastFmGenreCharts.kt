package com.luc4n3x.levyra.data.lastfm

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.TrackJson
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.spotify.SpotifyYouTubeMatcher
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

internal data class LastFmChartEntry(
    val rank: Int,
    val title: String,
    val artist: String,
    val durationMs: Long
)

internal sealed interface LastFmChartPage {
    data class Found(val entries: List<LastFmChartEntry>) : LastFmChartPage
    data object RateLimited : LastFmChartPage
    data class Failed(val reason: String) : LastFmChartPage
}

internal object LastFmGenreChartParser {
    private const val RATE_LIMIT_ERROR = 29

    fun parseTopTracks(body: String): LastFmChartPage {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return LastFmChartPage.Failed("malformed")
        if (root.has("error")) {
            val code = root.optInt("error")
            return if (code == RATE_LIMIT_ERROR) LastFmChartPage.RateLimited else LastFmChartPage.Failed("error $code")
        }
        val items = root.optJSONObject("tracks")?.optJSONArray("track") ?: return LastFmChartPage.Found(emptyList())
        val entries = (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val title = item.optString("name").trim()
            val artist = item.optJSONObject("artist")?.optString("name").orEmpty().trim()
            if (title.isBlank() || artist.isBlank()) return@mapNotNull null
            val rank = item.optJSONObject("@attr")?.optString("rank")?.toIntOrNull() ?: (index + 1)
            val seconds = item.optString("duration").toLongOrNull()?.takeIf { it in 1L..3_600L } ?: 0L
            LastFmChartEntry(rank = rank, title = title, artist = artist, durationMs = seconds * 1_000L)
        }
        return LastFmChartPage.Found(entries.sortedBy { it.rank })
    }
}

internal object LastFmGenreTags {
    private val zoneTags = mapOf(
        "rap-drill" to listOf("hip-hop", "rap"),
        "elettronica" to listOf("electronic", "house"),
        "pop-global" to listOf("pop"),
        "rnb-soul" to listOf("rnb", "soul"),
        "rock-alt" to listOf("rock", "alternative"),
        "latino" to listOf("latin", "reggaeton"),
        "lofi-chill" to listOf("lo-fi", "chillout"),
        "anime-jpop" to listOf("j-pop", "anime"),
        "afrobeats" to listOf("afrobeats", "amapiano"),
        "chart-kpop" to listOf("k-pop"),
        "chart-metal" to listOf("metal"),
        "chart-indie" to listOf("indie"),
        "chart-jazz" to listOf("jazz")
    )

    private data class Rule(val keywords: List<String>, val tag: String)

    private val exclusiveRules = listOf(
        Rule(listOf("k-pop", "kpop", "к-поп"), "k-pop"),
        Rule(listOf("j-pop", "jpop"), "j-pop"),
        Rule(listOf("anime", "аниме"), "anime")
    )

    private val rules = listOf(
        Rule(listOf("hip-hop", "hip hop", "hiphop", "хип-хоп", "рэп", "rap"), "hip-hop"),
        Rule(listOf("r&b", "rnb", "r'n'b", "ритм-н-блюз"), "rnb"),
        Rule(listOf("soul", "соул"), "soul"),
        Rule(listOf("metal", "метал"), "metal"),
        Rule(listOf("punk", "панк"), "punk"),
        Rule(listOf("rock", "рок"), "rock"),
        Rule(listOf("indie", "инди"), "indie"),
        Rule(listOf("alternativ", "альтернатив"), "alternative"),
        Rule(listOf("electron", "elettronic", "elektron", "электрон"), "electronic"),
        Rule(listOf("dance", "танцев", "tanz"), "dance"),
        Rule(listOf("jazz", "джаз"), "jazz"),
        Rule(listOf("blues", "блюз"), "blues"),
        Rule(listOf("classical", "classica", "clasica", "musique classique", "klassik", "klasyczna", "klasicka", "klassisk musik", "klassieke muziek", "классическая", "классика"), "classical"),
        Rule(listOf("country", "кантри"), "country"),
        Rule(listOf("reggaeton", "реггетон"), "reggaeton"),
        Rule(listOf("reggae", "регги"), "reggae"),
        Rule(listOf("latin", "латин"), "latin"),
        Rule(listOf("folk", "фолк"), "folk"),
        Rule(listOf("disco", "диско"), "disco"),
        Rule(listOf("funk", "фанк"), "funk"),
        Rule(listOf("gospel", "госпел"), "gospel"),
        Rule(listOf("afro", "афро"), "afrobeats"),
        Rule(listOf("lo-fi", "lofi"), "lo-fi"),
        Rule(listOf("pop", "поп"), "pop")
    )

    fun forZone(zoneId: String): List<String> = zoneTags[zoneId].orEmpty()

    fun forCategoryTitle(title: String): List<String> {
        val text = normalize(title)
        if (text.isBlank()) return emptyList()
        exclusiveRules.firstOrNull { rule -> rule.keywords.any { keyword -> startsWord(text, keyword) } }
            ?.let { rule -> return listOf(rule.tag) }
        val tags = rules
            .filter { rule -> rule.keywords.any { keyword -> startsWord(text, keyword) } }
            .map { it.tag }
            .distinct()
        val withoutShadowed = if ("reggaeton" in tags) tags - "reggae" else tags
        return withoutShadowed.take(MAX_TAGS)
    }

    private fun startsWord(text: String, keyword: String): Boolean {
        var start = text.indexOf(keyword)
        while (start >= 0) {
            val before = text.getOrNull(start - 1)
            if (before == null || !before.isLetter()) return true
            start = text.indexOf(keyword, start + 1)
        }
        return false
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(DIACRITICS, "")

    private val DIACRITICS = Regex("\\p{Mn}+")
    private const val MAX_TAGS = 2
}

internal fun interleaveLastFmCharts(charts: List<List<LastFmChartEntry>>, limit: Int): List<LastFmChartEntry> {
    if (limit <= 0) return emptyList()
    val seen = HashSet<String>()
    val result = ArrayList<LastFmChartEntry>(limit)
    val deepest = charts.maxOfOrNull { it.size } ?: 0
    for (depth in 0 until deepest) {
        for (chart in charts) {
            val entry = chart.getOrNull(depth) ?: continue
            if (seen.add(lastFmEntryKey(entry))) result += entry
            if (result.size >= limit) return result
        }
    }
    return result
}

internal fun lastFmEntryKey(entry: LastFmChartEntry): String =
    "${entry.artist.trim().lowercase(Locale.ROOT)}|${entry.title.trim().lowercase(Locale.ROOT)}"

internal fun acceptLastFmMatch(entry: LastFmChartEntry, candidate: Track?): Track? {
    if (candidate == null || candidate.id.isBlank() || candidate.title.isBlank()) return null
    if (entry.durationMs > 0L && candidate.durationMs > 0L &&
        abs(entry.durationMs - candidate.durationMs) > MAX_DURATION_DRIFT_MS
    ) return null
    val reference = Track(
        id = "lastfm:${lastFmEntryKey(entry)}",
        title = entry.title,
        artist = entry.artist,
        album = "",
        durationMs = entry.durationMs,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "Last.fm",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
    return candidate.takeIf { SpotifyYouTubeMatcher.scoreMatch(reference, it) >= MIN_MATCH_SCORE }
}

private const val MAX_DURATION_DRIFT_MS = 30_000L
private const val LAST_FM_MEMORY_ENTRIES = 24

internal fun <V> lastFmMemoryCache(maxEntries: Int = LAST_FM_MEMORY_ENTRIES): MutableMap<String, V> =
    java.util.Collections.synchronizedMap(object : LinkedHashMap<String, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, V>?): Boolean = size > maxEntries
    })
private const val MIN_MATCH_SCORE = 0.82

class LastFmGenreChartRepository(
    context: Context,
    private val apiKey: String = BuildConfig.LASTFM_API_KEY
) {
    private val appContext = context.applicationContext
    private val httpClient: OkHttpClient by lazy { LevyraHttpClientFactory.feeds(appContext) }
    private val cacheDir: File by lazy { File(appContext.cacheDir, CACHE_DIRECTORY) }
    private val memory: MutableMap<String, CachedChart> = lastFmMemoryCache()

    private data class CachedChart(val savedAtMs: Long, val tracks: List<Track>)

    suspend fun cached(tags: List<String>, languageCode: String): List<Track> = withContext(Dispatchers.IO) {
        readCache(cacheKey(normalizedTags(tags), languageCode))?.tracks.orEmpty()
    }

    suspend fun genreChart(
        tags: List<String>,
        languageCode: String,
        match: suspend (title: String, artist: String) -> Track?,
        onProgress: (List<Track>) -> Unit = {}
    ): List<Track> = withContext(Dispatchers.IO) {
        val cleanTags = normalizedTags(tags)
        if (cleanTags.isEmpty() || apiKey.isBlank()) return@withContext emptyList()
        val key = cacheKey(cleanTags, languageCode)
        val stored = readCache(key)
        if (stored != null && System.currentTimeMillis() - stored.savedAtMs in 0 until CACHE_TTL_MS) {
            return@withContext stored.tracks
        }
        val charts = coroutineScope {
            cleanTags.map { tag -> async { fetchTopTracks(tag, CANDIDATES_PER_TAG) } }.awaitAll()
        }
        val candidates = interleaveLastFmCharts(charts, CANDIDATE_POOL)
        if (candidates.isEmpty()) return@withContext stored?.tracks.orEmpty()
        val matched = matchInRankOrder(candidates, match, onProgress)
        if (matched.size >= MIN_CACHEABLE_TRACKS) {
            writeCache(key, CachedChart(System.currentTimeMillis(), matched))
            matched
        } else {
            stored?.tracks?.takeIf { it.size > matched.size } ?: matched
        }
    }

    private suspend fun matchInRankOrder(
        candidates: List<LastFmChartEntry>,
        match: suspend (title: String, artist: String) -> Track?,
        onProgress: (List<Track>) -> Unit
    ): List<Track> {
        val matched = LinkedHashMap<String, Track>()
        for (chunk in candidates.chunked(MATCH_CONCURRENCY)) {
            val results = coroutineScope {
                chunk.map { entry ->
                    async {
                        val candidate = try {
                            match(entry.title, entry.artist)
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Throwable) {
                            Timber.d(error, "Last.fm chart match failed")
                            null
                        }
                        acceptLastFmMatch(entry, candidate)
                    }
                }.awaitAll()
            }
            results.filterNotNull().forEach { track -> matched.putIfAbsent(track.id, track) }
            onProgress(matched.values.take(CHART_SIZE))
            if (matched.size >= CHART_SIZE) break
        }
        return matched.values.take(CHART_SIZE)
    }

    private suspend fun fetchTopTracks(tag: String, limit: Int): List<LastFmChartEntry> {
        val url = LAST_FM_URL.toHttpUrl().newBuilder()
            .addQueryParameter("method", "tag.gettoptracks")
            .addQueryParameter("tag", tag)
            .addQueryParameter("limit", limit.toString())
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("format", "json")
            .build()
        val body = try {
            runInterruptible {
                httpClient.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                    response.body?.string()?.takeIf { it.length <= MAX_RESPONSE_CHARS }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "Last.fm tag chart request failed for %s", tag)
            null
        } ?: return emptyList()
        return when (val page = LastFmGenreChartParser.parseTopTracks(body)) {
            is LastFmChartPage.Found -> page.entries
            LastFmChartPage.RateLimited -> {
                Timber.w("Last.fm rate limited tag chart for %s", tag)
                emptyList()
            }
            is LastFmChartPage.Failed -> {
                Timber.w("Last.fm tag chart failed for %s: %s", tag, page.reason)
                emptyList()
            }
        }
    }

    private fun normalizedTags(tags: List<String>): List<String> =
        tags.map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() }.distinct()

    private fun cacheKey(tags: List<String>, languageCode: String): String =
        tags.joinToString("+") + "@" + languageCode.trim().lowercase(Locale.ROOT)

    private fun cacheFile(key: String): File {
        val digest = MessageDigest.getInstance("SHA-1").digest(key.toByteArray(Charsets.UTF_8))
        return File(cacheDir, digest.joinToString("") { "%02x".format(it) } + ".json")
    }

    private fun readCache(key: String): CachedChart? {
        memory[key]?.let { return it }
        val file = cacheFile(key)
        if (!file.isFile) return null
        return runCatching {
            val json = JSONObject(file.readText())
            val items = json.optJSONArray("tracks") ?: JSONArray()
            val tracks = (0 until items.length()).mapNotNull { index ->
                items.optJSONObject(index)?.let(TrackJson::fromJson)
            }
            CachedChart(json.optLong("savedAtMs"), tracks).takeIf { it.tracks.isNotEmpty() }
        }.getOrNull()?.also { memory[key] = it }
    }

    private fun writeCache(key: String, chart: CachedChart) {
        memory[key] = chart
        runCatching {
            cacheDir.mkdirs()
            val payload = JSONObject()
                .put("savedAtMs", chart.savedAtMs)
                .put("tracks", JSONArray().apply { chart.tracks.forEach { put(TrackJson.toJson(it)) } })
            val target = cacheFile(key)
            val temporary = File(cacheDir, target.name + ".tmp")
            temporary.writeText(payload.toString())
            if (!temporary.renameTo(target)) {
                target.delete()
                temporary.renameTo(target)
            }
        }.onFailure { error -> Timber.w(error, "Last.fm chart cache write failed") }
    }

    private companion object {
        const val LAST_FM_URL = "https://ws.audioscrobbler.com/2.0/"
        const val CACHE_DIRECTORY = "lastfm-genre-charts"
        const val CACHE_TTL_MS = 24L * 60L * 60L * 1000L
        const val CHART_SIZE = 50
        const val CANDIDATES_PER_TAG = 60
        const val CANDIDATE_POOL = 75
        const val MATCH_CONCURRENCY = 5
        const val MIN_CACHEABLE_TRACKS = 12
        const val MAX_RESPONSE_CHARS = 2 * 1024 * 1024
    }
}

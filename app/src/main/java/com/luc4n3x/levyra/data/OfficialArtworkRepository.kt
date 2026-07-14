package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class OfficialArtworkRepository(context: Context) {
    private val client = LevyraHttpClientFactory.general(context.applicationContext)
        .newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(7, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .callTimeout(9, TimeUnit.SECONDS)
        .build()
    private val cache = ConcurrentHashMap<String, OfficialArtwork>()
    private val misses = ConcurrentHashMap<String, Long>()
    private val keyLocks = ConcurrentHashMap<String, Mutex>()
    private val searchSlots = Semaphore(4)

    suspend fun find(track: Track, country: String): OfficialArtwork? {
        if (track.title.isBlank() || track.artist.isBlank()) return null
        val key = cacheKey(track, country)
        cache[key]?.let { return it }
        val now = System.currentTimeMillis()
        misses[key]?.let { lastMiss ->
            if (now - lastMiss < MISS_TTL_MS) return null
        }
        val keyLock = keyLocks.computeIfAbsent(key) { Mutex() }
        return try {
            keyLock.withLock {
                cache[key]?.let { return@withLock it }
                misses[key]?.let { lastMiss ->
                    if (System.currentTimeMillis() - lastMiss < MISS_TTL_MS) return@withLock null
                }
                val outcome = searchSlots.withPermit {
                    withContext(Dispatchers.IO) { search(track, country) }
                }
                if (outcome.artwork != null) {
                    cache[key] = outcome.artwork
                    misses.remove(key)
                } else if (outcome.completedRequest) {
                    misses[key] = System.currentTimeMillis()
                }
                outcome.artwork
            }
        } finally {
            keyLocks.remove(key, keyLock)
        }
    }

    private fun search(track: Track, country: String): SearchOutcome {
        val normalizedCountry = country.trim().uppercase().takeIf { it.length == 2 } ?: "IT"
        val appleResponse = fetchApple(track, primaryQuery(track), normalizedCountry)
        val appleBest = appleResponse.items.maxByOrNull { it.score }
        if (appleBest != null && appleBest.score >= HIGH_CONFIDENCE_SCORE) {
            return SearchOutcome(appleBest, appleResponse.completed)
        }

        val deezerResponse = fetchDeezer(track)
        val best = (appleResponse.items + deezerResponse.items)
            .maxByOrNull { it.score }
            ?.takeIf { it.score >= MIN_ACCEPTED_SCORE }
        return SearchOutcome(
            artwork = best,
            completedRequest = appleResponse.completed || deezerResponse.completed
        )
    }

    private fun fetchApple(track: Track, query: String, country: String): ProviderResponse {
        val url = APPLE_SEARCH_URL.toHttpUrl().newBuilder()
            .addQueryParameter("term", query)
            .addQueryParameter("media", "music")
            .addQueryParameter("entity", "song")
            .addQueryParameter("limit", "25")
            .addQueryParameter("country", country)
            .addQueryParameter("explicit", "Yes")
            .build()
        val root = requestJson(url.toString()) ?: return ProviderResponse(emptyList(), false)
        val results = root.optJSONArray("results") ?: return ProviderResponse(emptyList(), true)
        val items = ArrayList<OfficialArtwork>(results.length())
        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val artwork = item.optString("artworkUrl100").trim()
            val title = item.optString("trackName").trim()
            val artist = item.optString("artistName").trim()
            if (artwork.isBlank() || title.isBlank() || artist.isBlank()) continue
            val album = item.optString("collectionName").trim()
            val durationMs = item.optLong("trackTimeMillis", 0L)
            val score = matchScore(track = track, title = title, artist = artist, album = album, durationMs = durationMs)
            items += OfficialArtwork(
                thumbnailUrl = resizeAppleArtwork(artwork, 600),
                largeThumbnailUrl = resizeAppleArtwork(artwork, 1200),
                album = album,
                score = score
            )
        }
        return ProviderResponse(items, true)
    }

    private fun fetchDeezer(track: Track): ProviderResponse {
        val preciseQuery = "track:\"${queryText(removeVersionText(track.title))}\" artist:\"${queryText(track.artist)}\""
        val url = DEEZER_SEARCH_URL.toHttpUrl().newBuilder()
            .addQueryParameter("q", preciseQuery)
            .addQueryParameter("limit", "25")
            .build()
        val root = requestJson(url.toString()) ?: return ProviderResponse(emptyList(), false)
        val results = root.optJSONArray("data") ?: return ProviderResponse(emptyList(), true)
        val items = ArrayList<OfficialArtwork>(results.length())
        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val title = item.optString("title").trim()
            val artist = item.optJSONObject("artist")?.optString("name").orEmpty().trim()
            val albumObject = item.optJSONObject("album")
            val album = albumObject?.optString("title").orEmpty().trim()
            val thumbnail = firstNonBlank(
                albumObject?.optString("cover_big").orEmpty(),
                albumObject?.optString("cover_medium").orEmpty(),
                albumObject?.optString("cover").orEmpty()
            )
            val large = firstNonBlank(
                albumObject?.optString("cover_xl").orEmpty(),
                albumObject?.optString("cover_big").orEmpty(),
                thumbnail
            )
            if (title.isBlank() || artist.isBlank() || thumbnail.isBlank()) continue
            val durationMs = item.optLong("duration", 0L).coerceAtLeast(0L) * 1000L
            val score = matchScore(track, title, artist, album, durationMs)
            items += OfficialArtwork(
                thumbnailUrl = thumbnail,
                largeThumbnailUrl = large,
                album = album,
                score = score
            )
        }
        return ProviderResponse(items, true)
    }

    private fun requestJson(url: String): JSONObject? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Accept-Language", "it-IT,it;q=0.9,en;q=0.7")
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) null else JSONObject(body)
            }
        }.getOrNull()
    }

    private fun primaryQuery(track: Track): String {
        val title = queryText(removeVersionText(track.title))
        val artist = queryText(track.artist)
        return "$title $artist".trim()
    }

    private fun matchScore(track: Track, title: String, artist: String, album: String, durationMs: Long): Int {
        val targetTitle = normalize(track.title)
        val targetArtist = normalize(track.artist)
        val targetAlbum = normalize(track.album)
        val candidateTitle = normalize(title)
        val candidateArtist = normalize(artist)
        val candidateAlbum = normalize(album)
        var score = 0

        score += when {
            candidateTitle == targetTitle -> 170
            candidateTitle.contains(targetTitle) || targetTitle.contains(candidateTitle) -> 120
            tokenCoverage(targetTitle, candidateTitle) >= 0.85 -> 95
            tokenCoverage(targetTitle, candidateTitle) >= 0.65 -> 70
            else -> 0
        }

        score += when {
            candidateArtist == targetArtist -> 125
            candidateArtist.contains(targetArtist) || targetArtist.contains(candidateArtist) -> 88
            tokenCoverage(targetArtist, candidateArtist) >= 0.75 -> 68
            tokenCoverage(targetArtist, candidateArtist) >= 0.5 -> 45
            else -> 0
        }

        if (targetAlbum.isNotBlank() && candidateAlbum.isNotBlank()) {
            score += when {
                candidateAlbum == targetAlbum -> 35
                candidateAlbum.contains(targetAlbum) || targetAlbum.contains(candidateAlbum) -> 20
                tokenCoverage(targetAlbum, candidateAlbum) >= 0.7 -> 12
                else -> 0
            }
        }

        if (track.durationMs > 0L && durationMs > 0L) {
            val delta = abs(track.durationMs - durationMs)
            score += when {
                delta <= 4_000L -> 30
                delta <= 10_000L -> 18
                delta <= 20_000L -> 8
                delta > 45_000L -> -30
                else -> 0
            }
        }

        val targetBlob = "$targetTitle $targetAlbum"
        val candidateBlob = "$candidateTitle $candidateAlbum"
        VERSION_TERMS.forEach { term ->
            if (candidateBlob.contains(term) && !targetBlob.contains(term)) score -= 38
        }
        if (candidateTitle.isBlank() || candidateArtist.isBlank()) score -= 200
        return score
    }

    private fun tokenCoverage(target: String, candidate: String): Double {
        val targetTokens = target.split(' ').filter { it.length >= 2 }.toSet()
        if (targetTokens.isEmpty()) return 0.0
        val candidateTokens = candidate.split(' ').filter { it.length >= 2 }.toSet()
        if (candidateTokens.isEmpty()) return 0.0
        return targetTokens.count { it in candidateTokens }.toDouble() / targetTokens.size.toDouble()
    }

    private fun normalize(value: String): String {
        return removeVersionText(value)
            .lowercase()
            .replace(Regex("""feat\.?|featuring|ft\.?"""), " ")
            .replace(Regex("""[^a-z0-9àèéìòóùçñäöüß\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun removeVersionText(value: String): String {
        return value
            .replace(Regex("""\([^)]*(official\s*(video|audio)|lyrics?|visualizer|music\s*video)[^)]*\)""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\[[^]]*(official\s*(video|audio)|lyrics?|visualizer|music\s*video)[^]]*]""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\s+-\s+(official\s*(video|audio)|lyrics?|visualizer|music\s*video).*$""", RegexOption.IGNORE_CASE), " ")
            .trim()
    }

    private fun queryText(value: String): String {
        return value
            .replace(Regex("""[\r\n\t]+"""), " ")
            .replace('"', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun resizeAppleArtwork(url: String, size: Int): String {
        return url.replace(APPLE_ARTWORK_SIZE, "${size}x${size}bb")
    }

    private fun cacheKey(track: Track, country: String): String {
        return "${normalize(track.title)}|${normalize(track.artist)}|${country.uppercase()}"
    }

    private fun firstNonBlank(vararg values: String): String {
        return values.firstOrNull { it.isNotBlank() }.orEmpty()
    }

    data class OfficialArtwork(
        val thumbnailUrl: String,
        val largeThumbnailUrl: String,
        val album: String,
        val score: Int
    )

    private data class ProviderResponse(
        val items: List<OfficialArtwork>,
        val completed: Boolean
    )

    private data class SearchOutcome(
        val artwork: OfficialArtwork?,
        val completedRequest: Boolean
    )

    private companion object {
        const val APPLE_SEARCH_URL = "https://itunes.apple.com/search"
        const val DEEZER_SEARCH_URL = "https://api.deezer.com/search/track"
        const val USER_AGENT = "Levyra/2.3.9 Android"
        const val MIN_ACCEPTED_SCORE = 200
        const val HIGH_CONFIDENCE_SCORE = 280
        const val MISS_TTL_MS = 10 * 60 * 1000L
        val APPLE_ARTWORK_SIZE = Regex("\\d+x\\d+bb")
        val VERSION_TERMS = listOf("live", "remix", "karaoke", "instrumental", "sped up", "slowed", "nightcore", "acoustic")
    }
}

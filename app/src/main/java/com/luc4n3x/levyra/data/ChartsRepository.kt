package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.math.absoluteValue

class ChartsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val httpClient: OkHttpClient by lazy { LevyraHttpClientFactory.feeds(appContext) }

    // Chart feeds are shared, idempotent and cheap to keep alive: running them in a
    // repository-owned scope lets a fetch survive the region switch that started it, so a
    // cancelled selection still warms the cache for the next tap instead of being thrown away.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = ConcurrentHashMap<String, Deferred<List<Track>>>()

    suspend fun topSongs(country: String, limit: Int = 50): List<Track> {
        val normalizedCountry = country.trim().lowercase().takeIf { it.length == 2 } ?: "it"
        val normalizedLimit = limit.coerceIn(1, 100)
        val shared = sharedRequest("$normalizedCountry/$normalizedLimit", normalizedCountry, normalizedLimit)
        return try {
            shared.await()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Timber.w(error, "Chart feed request failed for %s", normalizedCountry)
            emptyList()
        }
    }

    suspend fun officialArtwork(title: String, artist: String, country: String): String? = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()
        if (cleanTitle.isBlank() || cleanArtist.isBlank()) return@withContext null
        val region = country.trim().lowercase().takeIf { it.length == 2 } ?: "it"
        val term = URLEncoder.encode("$cleanTitle $cleanArtist", StandardCharsets.UTF_8.name())
        val body = httpGet("https://itunes.apple.com/search?term=$term&entity=song&limit=12&country=$region")
            ?: return@withContext null
        val results = JSONObject(body).optJSONArray("results") ?: return@withContext null
        val targetTitle = ChartFeedParser.normalizeMusicText(cleanTitle)
        val targetArtist = ChartFeedParser.normalizeMusicText(cleanArtist)
        var bestArtwork = ""
        var bestScore = Int.MIN_VALUE
        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val artwork = item.optString("artworkUrl100").trim()
            if (artwork.isBlank()) continue
            val candidateTitle = ChartFeedParser.normalizeMusicText(item.optString("trackName"))
            val candidateArtist = ChartFeedParser.normalizeMusicText(item.optString("artistName"))
            val score = artworkMatchScore(targetTitle, targetArtist, candidateTitle, candidateArtist)
            if (score > bestScore) {
                bestScore = score
                bestArtwork = artwork
            }
        }
        bestArtwork.takeIf { bestScore >= 95 }?.let(ChartFeedParser::upgradeArtwork)
    }

    private fun sharedRequest(key: String, country: String, limit: Int): Deferred<List<Track>> {
        inFlight[key]?.let { return it }
        val created = scope.async(start = CoroutineStart.LAZY) { fetchTopSongs(country, limit) }
        val active = inFlight.putIfAbsent(key, created)
        if (active != null) {
            created.cancel()
            return active
        }
        created.invokeOnCompletion { inFlight.remove(key, created) }
        created.start()
        return created
    }

    /**
     * The classic RSS endpoint is only a fallback, but chaining it after the modern feed meant a
     * stalled primary cost two full timeouts back to back. This keeps the fast path untouched and
     * separates the two failure shapes: a conclusive failure starts the fallback at once, while a
     * merely slow primary is raced against it and the first usable feed wins.
     */
    private suspend fun fetchTopSongs(country: String, limit: Int): List<Track> = coroutineScope {
        val winner = CompletableDeferred<List<Track>>()
        val remaining = AtomicInteger(2)

        fun dispatch(attempt: suspend () -> List<Track>) = launch {
            val outcome: List<Track> = try {
                attempt()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                Timber.w(error, "Chart feed attempt failed for %s", country)
                emptyList()
            }
            if (outcome.isNotEmpty()) {
                winner.complete(outcome)
            } else if (remaining.decrementAndGet() == 0) {
                winner.complete(emptyList())
            }
        }

        val modern = dispatch { fetchModern(country, limit) }
        launch {
            withTimeoutOrNull(FEED_HEDGE_BUDGET_MS) { modern.join() }
            if (!winner.isCompleted) dispatch { fetchClassic(country, limit) }
        }
        val result = winner.await()
        coroutineContext.cancelChildren()
        result
    }

    private suspend fun fetchModern(country: String, limit: Int): List<Track> {
        val url = "https://rss.marketingtools.apple.com/api/v2/$country/music/most-played/$limit/songs.json"
        val body = httpGet(url) ?: return emptyList()
        return ChartFeedParser.modern(body, limit)
    }

    private suspend fun fetchClassic(country: String, limit: Int): List<Track> {
        val url = "https://itunes.apple.com/$country/rss/topsongs/limit=$limit/json"
        val body = httpGet(url) ?: return emptyList()
        return ChartFeedParser.classic(body, limit)
    }

    private suspend fun httpGet(url: String): String? = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "Levyra/${BuildConfig.VERSION_NAME} Android")
            .cacheControl(FEED_CACHE_CONTROL)
            .build()
        val call = httpClient.newCall(request)
        val completed = AtomicBoolean(false)
        continuation.invokeOnCancellation {
            completed.set(true)
            call.cancel()
        }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                if (completed.compareAndSet(false, true)) continuation.resume(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = runCatching {
                    response.use { current ->
                        if (!current.isSuccessful) null else current.body?.string()?.takeIf { it.isNotBlank() }
                    }
                }.getOrNull()
                if (completed.compareAndSet(false, true)) continuation.resume(body)
            }
        })
    }

    private fun artworkMatchScore(
        targetTitle: String,
        targetArtist: String,
        candidateTitle: String,
        candidateArtist: String
    ): Int {
        var score = 0
        score += when {
            candidateTitle == targetTitle -> 90
            candidateTitle.contains(targetTitle) || targetTitle.contains(candidateTitle) -> 58
            else -> 0
        }
        score += when {
            candidateArtist == targetArtist -> 80
            candidateArtist.contains(targetArtist) || targetArtist.contains(candidateArtist) -> 48
            else -> 0
        }
        if (candidateTitle.isBlank() || candidateArtist.isBlank()) score -= 100
        return score
    }

    private companion object {
        const val FEED_HEDGE_BUDGET_MS = 900L
        const val FEED_MAX_STALE_MINUTES = 30

        // Charts move once a day at most, so a short stale window turns repeated region taps and
        // warm app starts into cache hits instead of full round trips.
        val FEED_CACHE_CONTROL: CacheControl = CacheControl.Builder()
            .maxStale(FEED_MAX_STALE_MINUTES, TimeUnit.MINUTES)
            .build()
    }
}

internal object ChartFeedParser {

    fun modern(body: String, limit: Int): List<Track> {
        val results = runCatching {
            JSONObject(body).optJSONObject("feed")?.optJSONArray("results")
        }.getOrNull() ?: return emptyList()
        val tracks = ArrayList<Track>(minOf(results.length(), limit).coerceAtLeast(0))
        for (index in 0 until results.length()) {
            if (tracks.size >= limit) break
            val entry = results.optJSONObject(index) ?: continue
            val title = entry.optString("name").trim()
            if (title.isBlank()) continue
            tracks += buildChartTrack(
                title = title,
                artist = entry.optString("artistName").trim(),
                artwork = upgradeArtwork(entry.optString("artworkUrl100"))
            )
        }
        return tracks
    }

    fun classic(body: String, limit: Int): List<Track> {
        val entries = runCatching {
            JSONObject(body).optJSONObject("feed")?.optJSONArray("entry")
        }.getOrNull() ?: return emptyList()
        val tracks = ArrayList<Track>(minOf(entries.length(), limit).coerceAtLeast(0))
        for (index in 0 until entries.length()) {
            if (tracks.size >= limit) break
            val entry = entries.optJSONObject(index) ?: continue
            val title = entry.optJSONObject("im:name")?.optString("label").orEmpty().trim()
            if (title.isBlank()) continue
            val images = entry.optJSONArray("im:image")
            tracks += buildChartTrack(
                title = title,
                artist = entry.optJSONObject("im:artist")?.optString("label").orEmpty().trim(),
                artwork = upgradeArtwork(
                    images?.optJSONObject((images.length() - 1).coerceAtLeast(0))?.optString("label").orEmpty()
                )
            )
        }
        return tracks
    }

    fun upgradeArtwork(url: String): String {
        if (url.isBlank()) return url
        return url.replace(appleArtworkSize, "600x600bb")
    }

    fun normalizeMusicText(value: String): String {
        return value.lowercase()
            .replace(parentheticals, " ")
            .replace(featuredMarkers, " ")
            .replace(videoMarkers, " ")
            .replace(nonMusicCharacters, " ")
            .replace(repeatedWhitespace, " ")
            .trim()
    }

    private fun buildChartTrack(title: String, artist: String, artwork: String): Track {
        val identity = chartIdentity("$title|$artist")
        val palette = palette(identity.seed)
        return Track(
            id = "chart-${identity.id}",
            title = title,
            artist = artist.ifBlank { "Vari artisti" },
            album = "Chart",
            durationMs = 0L,
            streamUrl = "",
            // No YouTube id yet: playback resolves a match by searching title + artist.
            videoUrl = "",
            thumbnailUrl = artwork,
            largeThumbnailUrl = artwork,
            source = "Classifica",
            moodTags = setOf("hit"),
            energy = 70,
            vocal = 55,
            replayScore = 90,
            cacheScore = 80,
            accentStart = palette.first,
            accentEnd = palette.second
        )
    }

    // One digest per entry instead of two: the seed and the persisted id are both derived from
    // the same SHA-256 of "title|artist", so stored chart ids stay byte-for-byte identical.
    private fun chartIdentity(value: String): ChartIdentity {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
        val seed = digest.take(4).fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }.absoluteValue
        val id = digest.take(8).joinToString("") { "%02x".format(it) }
        return ChartIdentity(seed = seed, id = id)
    }

    private fun palette(seed: Int): Pair<Int, Int> {
        return palettes[seed % palettes.size]
    }

    private data class ChartIdentity(val seed: Int, val id: String)

    private val appleArtworkSize = Regex("\\d+x\\d+bb")
    private val parentheticals = Regex("""\([^)]*\)|\[[^]]*]""")
    private val featuredMarkers = Regex("""feat\.?|featuring|ft\.?""")
    private val videoMarkers = Regex("""official audio|official video|lyrics?|visuali[sz]er|music video""")
    private val nonMusicCharacters = Regex("""[^a-z0-9àèéìòóùçñäöüß\s]""")
    private val repeatedWhitespace = Regex("""\s+""")

    private val palettes = listOf(
        0xFF00E5FF.toInt() to 0xFF7B42FF.toInt(),
        0xFF1B5CFF.toInt() to 0xFFFF4FD8.toInt(),
        0xFFFF7A18.toInt() to 0xFF8E57FF.toInt(),
        0xFF00D4A6.toInt() to 0xFFFF3B5C.toInt(),
        0xFFFFB000.toInt() to 0xFF00E5FF.toInt()
    )
}

package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.absoluteValue

class ChartsRepository {

    suspend fun topSongs(country: String, limit: Int = 50): List<Track> {
        val normalizedCountry = country.trim().lowercase().takeIf { it.length == 2 } ?: "it"
        val normalizedLimit = limit.coerceIn(1, 100)
        val modern = runCatching { fetchModern(normalizedCountry, normalizedLimit) }.getOrDefault(emptyList())
        if (modern.isNotEmpty()) return modern
        return runCatching { fetchClassic(normalizedCountry, normalizedLimit) }.getOrDefault(emptyList())
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
        val targetTitle = normalizeMusicText(cleanTitle)
        val targetArtist = normalizeMusicText(cleanArtist)
        var bestArtwork = ""
        var bestScore = Int.MIN_VALUE
        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val artwork = item.optString("artworkUrl100").trim()
            if (artwork.isBlank()) continue
            val candidateTitle = normalizeMusicText(item.optString("trackName"))
            val candidateArtist = normalizeMusicText(item.optString("artistName"))
            val score = artworkMatchScore(targetTitle, targetArtist, candidateTitle, candidateArtist)
            if (score > bestScore) {
                bestScore = score
                bestArtwork = artwork
            }
        }
        bestArtwork.takeIf { bestScore >= 95 }?.let(::upgradeArtwork)
    }

    private suspend fun fetchModern(country: String, limit: Int): List<Track> {
        val url = "https://rss.marketingtools.apple.com/api/v2/$country/music/most-played/$limit/songs.json"
        val body = httpGet(url) ?: return emptyList()
        val results = JSONObject(body).optJSONObject("feed")?.optJSONArray("results") ?: return emptyList()
        val tracks = mutableListOf<Track>()
        for (i in 0 until results.length()) {
            val entry = results.optJSONObject(i) ?: continue
            val title = entry.optString("name").trim()
            if (title.isBlank()) continue
            val artist = entry.optString("artistName").trim()
            val artwork = upgradeArtwork(entry.optString("artworkUrl100"))
            tracks += buildChartTrack(title, artist, artwork)
        }
        return tracks
    }

    private suspend fun fetchClassic(country: String, limit: Int): List<Track> {
        val url = "https://itunes.apple.com/$country/rss/topsongs/limit=$limit/json"
        val body = httpGet(url) ?: return emptyList()
        val entries = JSONObject(body).optJSONObject("feed")?.optJSONArray("entry") ?: return emptyList()
        val tracks = mutableListOf<Track>()
        for (i in 0 until entries.length()) {
            val entry = entries.optJSONObject(i) ?: continue
            val title = entry.optJSONObject("im:name")?.optString("label").orEmpty().trim()
            if (title.isBlank()) continue
            val artist = entry.optJSONObject("im:artist")?.optString("label").orEmpty().trim()
            val images = entry.optJSONArray("im:image")
            val artwork = upgradeArtwork(
                images?.optJSONObject((images.length() - 1).coerceAtLeast(0))?.optString("label").orEmpty()
            )
            tracks += buildChartTrack(title, artist, artwork)
        }
        return tracks
    }

    private suspend fun httpGet(url: String): String? = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Cache-Control", "no-cache")
            .header("User-Agent", "Levyra/${BuildConfig.VERSION_NAME} Android")
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

    private fun upgradeArtwork(url: String): String {
        if (url.isBlank()) return url
        return url.replace(Regex("\\d+x\\d+bb"), "600x600bb")
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

    private fun normalizeMusicText(value: String): String {
        return value.lowercase()
            .replace(Regex("""\([^)]*\)|\[[^]]*]"""), " ")
            .replace(Regex("""feat\.?|featuring|ft\.?"""), " ")
            .replace(Regex("""official audio|official video|lyrics?|visuali[sz]er|music video"""), " ")
            .replace(Regex("""[^a-z0-9àèéìòóùçñäöüß\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun buildChartTrack(title: String, artist: String, artwork: String): Track {
        val seed = stableSeed("$title|$artist")
        val palette = palette(seed)
        return Track(
            id = "chart-${stableId("$title|$artist")}",
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

    private fun stableSeed(value: String): Int {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .take(4)
            .fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }
            .absoluteValue
    }

    private fun stableId(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .take(8)
            .joinToString("") { "%02x".format(it) }
    }

    private fun palette(seed: Int): Pair<Int, Int> {
        val palettes = listOf(
            0xFF00E5FF.toInt() to 0xFF7B42FF.toInt(),
            0xFF1B5CFF.toInt() to 0xFFFF4FD8.toInt(),
            0xFFFF7A18.toInt() to 0xFF8E57FF.toInt(),
            0xFF00D4A6.toInt() to 0xFFFF3B5C.toInt(),
            0xFFFFB000.toInt() to 0xFF00E5FF.toInt()
        )
        return palettes[seed % palettes.size]
    }

    private companion object {
        val httpClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(7, TimeUnit.SECONDS)
            .callTimeout(8, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

}

package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.math.absoluteValue

/**
 * Real, reliable music charts from Apple's public RSS feeds (no auth needed).
 * Tries the modern marketing-tools feed first, then the classic iTunes feed.
 * Entries carry real titles, artists and cover art; a playable YouTube match is
 * resolved on demand when the user taps the song.
 */
class ChartsRepository {

    suspend fun topSongs(country: String, limit: Int = 50): List<Track> = withContext(Dispatchers.IO) {
        val modern = runCatching { fetchModern(country, limit) }.getOrDefault(emptyList())
        if (modern.isNotEmpty()) return@withContext modern
        runCatching { fetchClassic(country, limit) }.getOrDefault(emptyList())
    }

    private fun fetchModern(country: String, limit: Int): List<Track> {
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

    private fun fetchClassic(country: String, limit: Int): List<Track> {
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

    private fun httpGet(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 15000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
        }
        val code = connection.responseCode
        if (code !in 200..299) return null
        return BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { it.readText() }
    }

    private fun upgradeArtwork(url: String): String {
        if (url.isBlank()) return url
        return url.replace(Regex("\\d+x\\d+bb"), "512x512bb")
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
}

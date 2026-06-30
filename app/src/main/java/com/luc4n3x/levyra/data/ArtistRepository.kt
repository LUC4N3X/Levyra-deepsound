package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.ArtistRelease
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.math.absoluteValue

class ArtistRepository(private val music: YoutubeMusicRepository) {
    private val apiKey = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    private val clientVersion = "1.20260423.01.00"
    private val memory = LinkedHashMap<String, ArtistProfile>()

    suspend fun profileFor(artistName: String): ArtistProfile? = withContext(Dispatchers.IO) {
        val clean = artistName.trim()
        if (clean.length < 2) return@withContext null
        memory[clean.lowercase()]?.let { return@withContext it }
        val browseId = runCatching { resolveBrowseId(clean) }.getOrNull()
        val profile = if (!browseId.isNullOrBlank()) {
            runCatching { fetchProfile(browseId, clean) }.getOrNull()
        } else {
            null
        }
        val resolved = profile ?: runCatching { fallbackProfile(clean) }.getOrNull()
        resolved?.also { memory[clean.lowercase()] = it }
    }

    suspend fun profile(browseId: String, fallbackName: String): ArtistProfile? = withContext(Dispatchers.IO) {
        if (browseId.isBlank()) return@withContext profileFor(fallbackName)
        runCatching { fetchProfile(browseId, fallbackName) }.getOrNull()
            ?: runCatching { fallbackProfile(fallbackName) }.getOrNull()
    }

    private fun resolveBrowseId(query: String): String {
        val root = postSearch(query)
        val renderers = mutableListOf<JSONObject>()
        collectByKey(root, "musicResponsiveListItemRenderer", renderers)
        renderers.forEach { renderer ->
            val browseId = findArtistBrowseId(renderer)
            if (browseId.isNotBlank()) return browseId
        }
        val endpoints = mutableListOf<JSONObject>()
        collectByKey(root, "browseEndpoint", endpoints)
        endpoints.forEach { endpoint ->
            val id = endpoint.optString("browseId")
            val type = endpoint.optJSONObject("browseEndpointContextSupportedConfigs")
                ?.optJSONObject("browseEndpointContextMusicConfig")
                ?.optString("pageType")
                .orEmpty()
            if (id.startsWith("UC") || type.contains("ARTIST", ignoreCase = true)) return id
        }
        return ""
    }

    private fun findArtistBrowseId(renderer: JSONObject): String {
        val endpoints = mutableListOf<JSONObject>()
        collectByKey(renderer, "browseEndpoint", endpoints)
        endpoints.forEach { endpoint ->
            val id = endpoint.optString("browseId")
            val type = endpoint.optJSONObject("browseEndpointContextSupportedConfigs")
                ?.optJSONObject("browseEndpointContextMusicConfig")
                ?.optString("pageType")
                .orEmpty()
            if (type.contains("ARTIST", ignoreCase = true) || id.startsWith("UC")) return id
        }
        return ""
    }

    private fun fetchProfile(browseId: String, fallbackName: String): ArtistProfile? {
        val root = postBrowse(browseId)
        val header = root.optJSONObject("header")
        val name = headerText(header).ifBlank { fallbackName }
        val bio = extractBio(root)
        val subscribers = extractSubscribers(header)
        val monthly = extractMonthlyListeners(root)
        val thumb = bestThumbnail(headerThumbnails(header))
        val banner = extractBanner(header)
        val songs = extractTopSongs(root)
        val albums = extractReleases(root, "Album")
        val singles = extractReleases(root, "Singol")
        val seed = stableSeed(browseId + name)
        val accent = palette(seed)
        if (name.isBlank() && songs.isEmpty()) return null
        return ArtistProfile(
            browseId = browseId,
            name = name,
            bio = bio,
            subscribers = subscribers,
            monthlyListeners = monthly,
            thumbnailUrl = thumb,
            bannerUrl = banner.ifBlank { thumb },
            topSongs = songs,
            albums = albums,
            singles = singles,
            accentStart = accent.first,
            accentEnd = accent.second
        )
    }

    private suspend fun fallbackProfile(name: String): ArtistProfile {
        val songs = music.search(name, 18).filter { it.artist.contains(name, ignoreCase = true) }.ifEmpty {
            music.search(name, 12)
        }
        val seed = stableSeed(name)
        val accent = palette(seed)
        val cover = songs.firstOrNull()?.largeThumbnailUrl.orEmpty()
        return ArtistProfile(
            browseId = "",
            name = name,
            bio = "",
            subscribers = "",
            monthlyListeners = "",
            thumbnailUrl = cover,
            bannerUrl = cover,
            topSongs = songs.take(12),
            albums = emptyList(),
            singles = emptyList(),
            accentStart = accent.first,
            accentEnd = accent.second
        )
    }

    private fun extractBio(root: JSONObject): String {
        val sections = mutableListOf<JSONObject>()
        collectByKey(root, "musicDescriptionShelfRenderer", sections)
        sections.forEach { shelf ->
            val text = shelf.optJSONObject("description")?.optJSONArray("runs")?.joinText().orEmpty().trim()
            if (text.length > 24) return text
        }
        val descriptions = mutableListOf<JSONObject>()
        collectByKey(root, "description", descriptions)
        descriptions.forEach { node ->
            val text = node.optJSONArray("runs")?.joinText().orEmpty().trim()
            if (text.length > 80) return text
        }
        return ""
    }

    private fun extractSubscribers(header: JSONObject?): String {
        val text = header?.optJSONObject("subscriptionButton")
            ?.optJSONObject("subscribeButtonRenderer")
            ?.optJSONObject("subscriberCountText")
            ?.optJSONArray("runs")
            ?.joinText()
            .orEmpty()
            .trim()
        return text
    }

    private fun extractMonthlyListeners(root: JSONObject): String {
        val candidates = mutableListOf<JSONObject>()
        collectByKey(root, "subscriberCountText", candidates)
        candidates.forEach { node ->
            val text = node.optJSONArray("runs")?.joinText().orEmpty()
            if (text.contains("ascolt", ignoreCase = true) || text.contains("listener", ignoreCase = true)) return text.trim()
        }
        return ""
    }

    private fun extractBanner(header: JSONObject?): String {
        val arrays = mutableListOf<JSONArray>()
        collectArrays(header, "thumbnails", arrays)
        var best = ""
        var bestScore = -1
        arrays.forEach { array ->
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val score = item.optInt("width", 0) * item.optInt("height", 0)
                val isWide = item.optInt("width", 0) > item.optInt("height", 0)
                if (isWide && score > bestScore && item.optString("url").isNotBlank()) {
                    best = item.optString("url")
                    bestScore = score
                }
            }
        }
        return best
    }

    private fun extractTopSongs(root: JSONObject): List<Track> {
        val renderers = mutableListOf<JSONObject>()
        collectByKey(root, "musicResponsiveListItemRenderer", renderers)
        val tracks = LinkedHashMap<String, Track>()
        renderers.forEach { renderer ->
            val videoId = primaryVideoId(renderer)
            if (videoId.isBlank()) return@forEach
            val lines = flexLines(renderer)
            val title = lines.firstOrNull()?.takeIf { it.isNotBlank() } ?: return@forEach
            val artist = lines.getOrNull(1)?.split(" • ", " · ")?.firstOrNull()?.trim().orEmpty()
            val album = lines.getOrNull(2).orEmpty()
            val thumb = bestThumbnail(thumbnailsOf(renderer))
            val seed = stableSeed(videoId + title)
            val accent = palette(seed)
            if (!tracks.containsKey(videoId)) {
                tracks[videoId] = Track(
                    id = videoId,
                    title = title,
                    artist = artist.ifBlank { "YouTube Music" },
                    album = album.ifBlank { "YouTube Music" },
                    durationMs = durationOf(renderer.toString()),
                    streamUrl = "",
                    videoUrl = "https://www.youtube.com/watch?v=$videoId",
                    thumbnailUrl = thumb,
                    largeThumbnailUrl = upgradeThumbnail(thumb),
                    source = "YouTube Music",
                    moodTags = setOf("hit"),
                    energy = (45 + seed % 52).coerceIn(0, 100),
                    vocal = (35 + (seed / 3) % 60).coerceIn(0, 100),
                    replayScore = (62 + (seed / 7) % 38).coerceIn(0, 100),
                    cacheScore = (48 + (seed / 11) % 50).coerceIn(0, 100),
                    accentStart = accent.first,
                    accentEnd = accent.second
                )
            }
        }
        return tracks.values.take(20)
    }

    private fun extractReleases(root: JSONObject, kindHint: String): List<ArtistRelease> {
        val cards = mutableListOf<JSONObject>()
        collectByKey(root, "musicTwoRowItemRenderer", cards)
        val out = LinkedHashMap<String, ArtistRelease>()
        cards.forEach { card ->
            val title = card.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().trim()
            if (title.isBlank()) return@forEach
            val subtitle = card.optJSONObject("subtitle")?.optJSONArray("runs")?.joinText().orEmpty().trim()
            val matchesKind = subtitle.contains(kindHint, ignoreCase = true) ||
                (kindHint == "Singol" && subtitle.contains("Single", ignoreCase = true))
            if (!matchesKind && kindHint.isNotBlank()) return@forEach
            val browseId = card.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
                ?.optString("browseId")
                .orEmpty()
            val thumb = bestThumbnail(thumbnailsOf(card))
            val year = Regex("\\b(19|20)\\d{2}\\b").find(subtitle)?.value.orEmpty()
            val key = browseId.ifBlank { title }
            if (!out.containsKey(key)) {
                out[key] = ArtistRelease(
                    browseId = browseId,
                    title = title,
                    subtitle = subtitle,
                    thumbnailUrl = upgradeThumbnail(thumb),
                    year = year
                )
            }
        }
        return out.values.take(12)
    }

    private fun postSearch(query: String): JSONObject {
        val endpoint = "https://music.youtube.com/youtubei/v1/search?key=$apiKey&prettyPrint=false"
        val body = JSONObject()
            .put("context", clientContext())
            .put("query", query)
            .put("params", "EgWKAQIgAWoMEAMQBBAJEAoQBRAV")
            .toString()
        return post(endpoint, body, "https://music.youtube.com/search?q=${query.replace(" ", "+")}")
    }

    private fun postBrowse(browseId: String): JSONObject {
        val endpoint = "https://music.youtube.com/youtubei/v1/browse?key=$apiKey&prettyPrint=false"
        val body = JSONObject()
            .put("context", clientContext())
            .put("browseId", browseId)
            .toString()
        return post(endpoint, body, "https://music.youtube.com/")
    }

    private fun clientContext(): JSONObject = JSONObject().put(
        "client",
        JSONObject()
            .put("clientName", "WEB_REMIX")
            .put("clientVersion", clientVersion)
            .put("hl", "it")
            .put("gl", "IT")
            .put("platform", "DESKTOP")
    )

    private fun post(endpoint: String, body: String, referer: String): JSONObject {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Origin", "https://music.youtube.com")
            setRequestProperty("Referer", referer)
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
            setRequestProperty("X-Youtube-Client-Name", "67")
            setRequestProperty("X-Youtube-Client-Version", clientVersion)
            setRequestProperty("Content-Length", bytes.size.toString())
        }
        connection.outputStream.use { it.write(bytes) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
        if (code !in 200..299) return JSONObject()
        return JSONObject(response)
    }

    private fun headerText(header: JSONObject?): String {
        header ?: return ""
        val direct = header.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().trim()
        if (direct.isNotBlank()) return direct
        val titles = mutableListOf<JSONObject>()
        collectByKey(header, "title", titles)
        titles.forEach { node ->
            val text = node.optJSONArray("runs")?.joinText().orEmpty().trim()
            if (text.isNotBlank()) return text
        }
        return ""
    }

    private fun headerThumbnails(header: JSONObject?): JSONArray {
        val arrays = mutableListOf<JSONArray>()
        collectArrays(header, "thumbnails", arrays)
        return arrays.maxByOrNull { array ->
            var best = 0
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                best = maxOf(best, item.optInt("width", 0) * item.optInt("height", 0))
            }
            best
        } ?: JSONArray()
    }

    private fun thumbnailsOf(node: JSONObject): JSONArray {
        val arrays = mutableListOf<JSONArray>()
        collectArrays(node, "thumbnails", arrays)
        return arrays.firstOrNull { it.length() > 0 } ?: JSONArray()
    }

    private fun primaryVideoId(renderer: JSONObject): String {
        val endpoints = mutableListOf<JSONObject>()
        collectByKey(renderer, "watchEndpoint", endpoints)
        endpoints.forEach { endpoint ->
            val id = endpoint.optString("videoId")
            if (id.isNotBlank()) return id
        }
        return ""
    }

    private fun flexLines(renderer: JSONObject): List<String> {
        val lines = mutableListOf<String>()
        val columns = renderer.optJSONArray("flexColumns") ?: JSONArray()
        for (i in 0 until columns.length()) {
            val text = columns.optJSONObject(i)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
                ?.joinText()
                .orEmpty()
                .trim()
            if (text.isNotBlank()) lines += text
        }
        return lines.distinct()
    }

    private fun durationOf(text: String): Long {
        val match = Regex("\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\b").find(text)?.value ?: return 0L
        val parts = match.split(":").mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            2 -> (parts[0] * 60L + parts[1]) * 1000L
            3 -> (parts[0] * 3600L + parts[1] * 60L + parts[2]) * 1000L
            else -> 0L
        }
    }

    private fun bestThumbnail(array: JSONArray): String {
        var bestUrl = ""
        var bestScore = -1
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val url = item.optString("url")
            val score = item.optInt("width", 0) * item.optInt("height", 0)
            if (url.isNotBlank() && score >= bestScore) {
                bestUrl = url
                bestScore = score
            }
        }
        return bestUrl
    }

    private fun upgradeThumbnail(url: String): String {
        if (url.isBlank()) return url
        return url.replace(Regex("=w\\d+-h\\d+.*$"), "=w1200-h1200-l90-rj")
            .replace(Regex("=s\\d+.*$"), "=s1200")
    }

    private fun collectByKey(value: Any?, key: String, out: MutableList<JSONObject>) {
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val current = keys.next()
                    val child = value.opt(current)
                    if (current == key && child is JSONObject) out += child
                    collectByKey(child, key, out)
                }
            }
            is JSONArray -> for (i in 0 until value.length()) collectByKey(value.opt(i), key, out)
        }
    }

    private fun collectArrays(value: Any?, key: String, out: MutableList<JSONArray>) {
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val current = keys.next()
                    val child = value.opt(current)
                    if (current == key && child is JSONArray) out += child
                    collectArrays(child, key, out)
                }
            }
            is JSONArray -> for (i in 0 until value.length()) collectArrays(value.opt(i), key, out)
        }
    }

    private fun JSONArray.joinText(): String {
        val parts = mutableListOf<String>()
        for (i in 0 until length()) {
            val text = optJSONObject(i)?.optString("text").orEmpty()
            if (text.isNotBlank()) parts += text
        }
        return parts.joinToString("").replace("  ", " ").trim()
    }

    private fun stableSeed(value: String): Int {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .take(4)
            .fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }
            .absoluteValue
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

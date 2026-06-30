package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.CacheReport
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.SearchResults
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

class YoutubeMusicRepository {
    private val apiKey = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    private val clientVersion = "1.20260423.01.00"
    private val memory = LinkedHashMap<String, Track>()

    suspend fun search(query: String, limit: Int = 36): List<Track> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()
        val remote = runCatching { searchInnerTube(cleanQuery, limit) }.getOrDefault(emptyList())
        if (remote.isNotEmpty()) {
            remote.forEach { memory[it.id] = it }
            return@withContext remote
        }
        runCatching { searchYoutubeExtractor(cleanQuery, limit) }
            .getOrDefault(emptyList())
            .also { items -> items.forEach { memory[it.id] = it } }
    }

    /** First YouTube Music match for a query, used to make chart entries playable. */
    suspend fun searchOne(query: String): Track? = search(query, 1).firstOrNull()

    suspend fun searchEverything(query: String): SearchResults = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext SearchResults()
        val root = runCatching { searchInnerTubeRaw(cleanQuery) }.getOrNull() ?: return@withContext fallbackResults(cleanQuery)
        val renderers = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicResponsiveListItemRenderer", renderers)
        val songs = LinkedHashMap<String, Track>()
        val artists = LinkedHashMap<String, ArtistHit>()
        val albums = LinkedHashMap<String, AlbumHit>()
        renderers.forEach { renderer ->
            val lines = extractFlexLines(renderer)
            val title = lines.firstOrNull()?.takeIf { it.isNotBlank() } ?: return@forEach
            val subtitleTokens = lines.drop(1).flatMap { it.split(" • ", " · ") }.map { it.trim() }
            val kind = subtitleTokens.firstOrNull()?.lowercase().orEmpty()
            val thumb = findBestThumbnail(renderer)
            when {
                kind.startsWith("artist") || kind.startsWith("artista") -> {
                    if (!artists.containsKey(title.lowercase())) {
                        val subs = subtitleTokens.firstOrNull { it.contains("scritt", ignoreCase = true) || it.contains("subscriber", ignoreCase = true) || it.contains("iscritt", ignoreCase = true) }.orEmpty()
                        val seed = stableSeed(title)
                        artists[title.lowercase()] = ArtistHit(
                            name = title,
                            subscribers = subs,
                            thumbnailUrl = upgradeThumbnail(thumb),
                            accentStart = palette(seed).first,
                            accentEnd = palette(seed).second
                        )
                    }
                }
                kind.startsWith("album") || kind.startsWith("single") || kind.startsWith("singol") || kind.startsWith("ep") -> {
                    val albumArtist = subtitleTokens.getOrNull(1).orEmpty()
                    val year = subtitleTokens.firstNotNullOfOrNull { Regex("\\b(19|20)\\d{2}\\b").find(it)?.value }.orEmpty()
                    val key = "${title.lowercase()}|${albumArtist.lowercase()}"
                    if (!albums.containsKey(key)) {
                        albums[key] = AlbumHit(
                            title = title,
                            artist = albumArtist.ifBlank { "Album" },
                            year = year,
                            thumbnailUrl = upgradeThumbnail(thumb),
                            query = "$title $albumArtist"
                        )
                    }
                }
                else -> {
                    val track = parseMusicRenderer(renderer, cleanQuery) ?: return@forEach
                    if (!songs.containsKey(track.id)) songs[track.id] = track
                }
            }
        }
        if (songs.isEmpty()) {
            val videoRenderers = mutableListOf<JSONObject>()
            collectObjectsByKey(root, "videoRenderer", videoRenderers)
            videoRenderers.forEach { renderer ->
                val track = parseVideoRenderer(renderer, cleanQuery) ?: return@forEach
                if (!songs.containsKey(track.id)) songs[track.id] = track
            }
        }
        songs.values.forEach { memory[it.id] = it }
        val songList = songs.values.toList()
        val results = SearchResults(
            topTrack = songList.firstOrNull(),
            songs = songList.take(20),
            artists = artists.values.take(8).toList(),
            albums = albums.values.take(10).toList()
        )
        if (results.isEmpty) fallbackResults(cleanQuery) else results
    }

    private suspend fun fallbackResults(query: String): SearchResults {
        val songs = search(query, 20)
        return SearchResults(topTrack = songs.firstOrNull(), songs = songs)
    }

    private fun searchInnerTubeRaw(query: String): JSONObject? {
        val endpoint = "https://music.youtube.com/youtubei/v1/search?key=$apiKey&prettyPrint=false"
        val body = JSONObject()
            .put(
                "context",
                JSONObject().put(
                    "client",
                    JSONObject()
                        .put("clientName", "WEB_REMIX")
                        .put("clientVersion", clientVersion)
                        .put("hl", "it")
                        .put("gl", "IT")
                        .put("platform", "DESKTOP")
                )
            )
            .put("query", query)
            .toString()
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Origin", "https://music.youtube.com")
            setRequestProperty("Referer", "https://music.youtube.com/search?q=${query.replace(" ", "+")}")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
            setRequestProperty("X-Youtube-Client-Name", "67")
            setRequestProperty("X-Youtube-Client-Version", clientVersion)
            setRequestProperty("Content-Length", bytes.size.toString())
        }
        connection.outputStream.use { it.write(bytes) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
        if (code !in 200..299) return null
        return JSONObject(response)
    }

    suspend fun home(
        queries: List<String> = listOf("top hits italia 2026", "global top hits 2026", "rap italiano 2026", "pop hits 2026")
    ): List<Track> = withContext(Dispatchers.IO) {
        val results = queries.flatMap { query ->
            runCatching { search(query, 10) }.getOrDefault(emptyList())
        }
        results.distinctBy { it.id }.take(40).also { items -> items.forEach { memory[it.id] = it } }
    }

    /** Real YouTube Music home feed parsed into titled sections, like the official app. */
    suspend fun homeFeed(): List<HomeSection> = withContext(Dispatchers.IO) {
        val sections = runCatching { homeFeedInnerTube() }.getOrDefault(emptyList())
        sections.forEach { section -> section.tracks.forEach { memory[it.id] = it } }
        sections
    }

    private fun homeFeedInnerTube(): List<HomeSection> {
        val endpoint = "https://music.youtube.com/youtubei/v1/browse?key=$apiKey&prettyPrint=false"
        val body = JSONObject()
            .put(
                "context",
                JSONObject().put(
                    "client",
                    JSONObject()
                        .put("clientName", "WEB_REMIX")
                        .put("clientVersion", clientVersion)
                        .put("hl", "it")
                        .put("gl", "IT")
                        .put("platform", "DESKTOP")
                )
            )
            .put("browseId", "FEmusic_home")
            .toString()
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Origin", "https://music.youtube.com")
            setRequestProperty("Referer", "https://music.youtube.com/")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
            setRequestProperty("X-Youtube-Client-Name", "67")
            setRequestProperty("X-Youtube-Client-Version", clientVersion)
            setRequestProperty("Content-Length", bytes.size.toString())
        }
        connection.outputStream.use { it.write(bytes) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
        if (code !in 200..299) return emptyList()
        val root = JSONObject(response)
        val shelves = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicCarouselShelfRenderer", shelves)
        val sections = mutableListOf<HomeSection>()
        shelves.forEach { shelf ->
            val title = shelfTitle(shelf).ifBlank { "Per te" }
            val contents = shelf.optJSONArray("contents") ?: JSONArray()
            val tracks = LinkedHashMap<String, Track>()
            for (i in 0 until contents.length()) {
                val item = contents.optJSONObject(i) ?: continue
                val track = parseCarouselItem(item)
                if (track != null && !tracks.containsKey(track.id)) tracks[track.id] = track
            }
            if (tracks.size >= 3) sections += HomeSection(title, tracks.values.take(20))
        }
        return sections.take(10)
    }

    private fun shelfTitle(shelf: JSONObject): String {
        return shelf.optJSONObject("header")
            ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
            ?.optJSONObject("title")
            ?.optJSONArray("runs")
            ?.joinText()
            .orEmpty()
            .trim()
    }

    private val excludedTypes = setOf("album", "playlist", "artist", "ep", "podcast", "episode", "artista", "canale", "channel", "profilo", "profile", "mix")

    fun searchSuggestions(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        val url = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=${java.net.URLEncoder.encode(query, "UTF-8")}"
        return runCatching {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONArray(response)
            val suggestions = root.optJSONArray(1) ?: return emptyList()
            val result = mutableListOf<String>()
            for (i in 0 until suggestions.length()) {
                result += suggestions.optString(i)
            }
            result
        }.getOrDefault(emptyList())
    }

    private fun parseCarouselItem(item: JSONObject): Track? {
        item.optJSONObject("musicResponsiveListItemRenderer")?.let { return parseMusicRenderer(it, "home") }
        val two = item.optJSONObject("musicTwoRowItemRenderer") ?: return null
        val title = two.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().trim()
        if (title.isBlank()) return null
        val videoId = firstWatchVideoId(two).ifBlank { return null }
        val subtitle = two.optJSONObject("subtitle")?.optJSONArray("runs")?.joinText().orEmpty()
        
        val tokens = subtitle.split(" • ", " · ", " - ").map { it.trim() }
        if (tokens.isNotEmpty() && tokens[0].lowercase() in excludedTypes) return null
        
        val artist = tokens.firstOrNull()?.takeIf { it.isNotBlank() && it.lowercase() !in typeLabels } ?: "YouTube Music"
        val thumbnail = findBestThumbnail(two)
        return buildTrack(
            id = videoId,
            title = title,
            artist = artist,
            album = "YouTube Music",
            durationMs = 0L,
            thumbnailUrl = thumbnail,
            largeThumbnailUrl = upgradeThumbnail(thumbnail),
            videoUrl = "https://www.youtube.com/watch?v=$videoId",
            query = "home",
            source = "YouTube Music"
        )
    }

    private fun firstWatchVideoId(renderer: JSONObject): String {
        val endpoints = mutableListOf<JSONObject>()
        collectObjectsByKey(renderer, "watchEndpoint", endpoints)
        endpoints.forEach { endpoint ->
            if (endpoint.has("playlistId")) return@forEach
            val id = endpoint.optString("videoId")
            if (id.isNotBlank()) return id
        }
        return ""
    }

    fun cachedTracks(): List<Track> = memory.values.toList()

    fun replace(track: Track) {
        memory[track.id] = track
    }

    fun cacheReport(): CacheReport {
        val all = memory.values.toList()
        val resolved = all.count { it.streamUrl.isNotBlank() }
        return CacheReport(
            offlineReady = resolved,
            smartCached = all.count { it.cacheScore >= 70 },
            nextPreload = all.take(6).count(),
            totalTracks = all.size
        )
    }

    private fun searchInnerTube(query: String, limit: Int): List<Track> {
        val endpoint = "https://music.youtube.com/youtubei/v1/search?key=$apiKey&prettyPrint=false"
        val body = JSONObject()
            .put(
                "context",
                JSONObject().put(
                    "client",
                    JSONObject()
                        .put("clientName", "WEB_REMIX")
                        .put("clientVersion", clientVersion)
                        .put("hl", "it")
                        .put("gl", "IT")
                        .put("platform", "DESKTOP")
                )
            )
            .put("query", query)
            .toString()
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Origin", "https://music.youtube.com")
            setRequestProperty("Referer", "https://music.youtube.com/search?q=${query.replace(" ", "+")}")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
            setRequestProperty("X-Youtube-Client-Name", "67")
            setRequestProperty("X-Youtube-Client-Version", clientVersion)
            setRequestProperty("Content-Length", bytes.size.toString())
        }
        connection.outputStream.use { it.write(bytes) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
        if (code !in 200..299) return emptyList()
        val root = JSONObject(response)
        val renderers = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicResponsiveListItemRenderer", renderers)
        val tracks = LinkedHashMap<String, Track>()
        renderers.forEach { renderer ->
            val track = parseMusicRenderer(renderer, query)
            if (track != null && !tracks.containsKey(track.id)) tracks[track.id] = track
        }
        if (tracks.isEmpty()) {
            val videoRenderers = mutableListOf<JSONObject>()
            collectObjectsByKey(root, "videoRenderer", videoRenderers)
            videoRenderers.forEach { renderer ->
                val track = parseVideoRenderer(renderer, query)
                if (track != null && !tracks.containsKey(track.id)) tracks[track.id] = track
            }
        }
        return tracks.values.take(limit)
    }

    private fun searchYoutubeExtractor(query: String, limit: Int): List<Track> {
        NewPipeRuntime.ensure()
        val service = org.schabi.newpipe.extractor.ServiceList.YouTube
        val handler = service.searchQHFactory.fromQuery(
            query,
            listOf(org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_SONGS),
            ""
        )
        val info = org.schabi.newpipe.extractor.search.SearchInfo.getInfo(service, handler)
        return info.relatedItems
            .filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>()
            .mapNotNull { item ->
                val id = extractVideoId(item.url).ifBlank { stableId(item.url) }
                if (id.isBlank()) return@mapNotNull null
                val thumbnail = item.thumbnails.maxByOrNull { image ->
                    image.width.coerceAtLeast(0) * image.height.coerceAtLeast(0)
                }?.url.orEmpty()
                buildTrack(
                    id = id,
                    title = item.name,
                    artist = item.uploaderName.orEmpty(),
                    album = "YouTube Music",
                    durationMs = secondsToMs(item.duration),
                    thumbnailUrl = thumbnail,
                    largeThumbnailUrl = upgradeThumbnail(thumbnail),
                    videoUrl = item.url,
                    query = query,
                    source = "NewPipe YouTube Music"
                )
            }
            .distinctBy { it.id }
            .take(limit)
    }

    private val typeLabels = setOf(
        "song", "video", "album", "playlist", "artist", "single", "ep", "episode", "podcast",
        "brano", "canzone", "video musicale", "video ufficiale", "artista", "singolo", "episodio"
    )

    private fun isTypeLabel(token: String): Boolean = token.trim().lowercase() in typeLabels

    private fun parseMusicRenderer(renderer: JSONObject, query: String): Track? {
        val lines = extractFlexLines(renderer)
        val title = lines.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val videoId = extractPrimaryMusicVideoId(renderer).takeIf { it.isNotBlank() } ?: return null
        val allText = renderer.toString()
        val duration = extractDuration(allText)
        
        val subtitleLines = lines.drop(1)
        val tokens = subtitleLines.flatMap { it.split(" • ", " · ", " - ") }.map { it.trim() }
        if (tokens.isNotEmpty() && tokens[0].lowercase() in excludedTypes) return null
        
        val artist = tokens
            .firstOrNull { token -> token.isNotBlank() && !isTypeLabel(token) && !token.matches(Regex("\\d+:\\d{2}")) }
            ?: "YouTube Music"
        val thumbnail = findBestThumbnail(renderer)
        return buildTrack(
            id = videoId,
            title = title,
            artist = artist,
            album = lines.drop(1).getOrNull(1)?.takeIf { it.isNotBlank() } ?: "YouTube Music",
            durationMs = duration,
            thumbnailUrl = thumbnail,
            largeThumbnailUrl = upgradeThumbnail(thumbnail),
            videoUrl = "https://www.youtube.com/watch?v=$videoId",
            query = query,
            source = "YouTube Music"
        )
    }

    private fun extractPrimaryMusicVideoId(renderer: JSONObject): String {
        renderer.optJSONObject("playlistItemData")?.optString("videoId")?.takeIf { it.isNotBlank() }?.let { return it }
        val columns = renderer.optJSONArray("flexColumns") ?: JSONArray()
        for (i in 0 until columns.length()) {
            val runs = columns.optJSONObject(i)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
                ?: continue
            for (j in 0 until runs.length()) {
                val videoId = runs.optJSONObject(j)
                    ?.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("watchEndpoint")
                    ?.optString("videoId")
                    .orEmpty()
                if (videoId.isNotBlank()) return videoId
            }
        }
        val playButtons = mutableListOf<JSONObject>()
        collectObjectsByKey(renderer, "musicPlayButtonRenderer", playButtons)
        playButtons.forEach { button ->
            val videoId = button.optJSONObject("playNavigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
                .orEmpty()
            if (videoId.isNotBlank()) return videoId
        }
        val watchEndpoints = mutableListOf<JSONObject>()
        collectObjectsByKey(renderer, "watchEndpoint", watchEndpoints)
        watchEndpoints.forEach { endpoint ->
            val videoId = endpoint.optString("videoId")
            if (videoId.isNotBlank()) return videoId
        }
        return ""
    }

    private fun parseVideoRenderer(renderer: JSONObject, query: String): Track? {
        val videoId = renderer.optString("videoId").takeIf { it.isNotBlank() } ?: return null
        val title = renderer.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().ifBlank { return null }
        val artist = renderer.optJSONObject("ownerText")?.optJSONArray("runs")?.joinText().orEmpty().ifBlank { "YouTube" }
        val duration = renderer.optJSONObject("lengthText")?.optString("simpleText")?.durationToMs() ?: 0L
        val thumbnail = renderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")?.bestThumbnail().orEmpty()
        return buildTrack(
            id = videoId,
            title = title,
            artist = artist,
            album = "YouTube",
            durationMs = duration,
            thumbnailUrl = thumbnail,
            largeThumbnailUrl = upgradeThumbnail(thumbnail),
            videoUrl = "https://www.youtube.com/watch?v=$videoId",
            query = query,
            source = "YouTube"
        )
    }

    private fun buildTrack(
        id: String,
        title: String,
        artist: String,
        album: String,
        durationMs: Long,
        thumbnailUrl: String,
        largeThumbnailUrl: String,
        videoUrl: String,
        query: String,
        source: String
    ): Track {
        val seed = stableSeed("$id$title$artist$query")
        val normalized = "$title $artist $album $query".lowercase()
        val tags = buildSet {
            add("hit")
            if (normalized.contains("ital") || normalized.contains("sanremo")) add("italian")
            if (normalized.contains("rap") || normalized.contains("trap")) add("rap")
            if (normalized.contains("gym") || normalized.contains("workout") || normalized.contains("bass")) add("gym")
            if (normalized.contains("night") || normalized.contains("chill")) add("night")
            if (normalized.contains("focus") || normalized.contains("deep")) add("focus")
            if (normalized.contains("pop")) add("pop")
            if (normalized.contains("new")) add("new")
            if (isEmpty()) add("music")
        }
        return Track(
            id = id,
            title = title.cleanLabel(),
            artist = artist.cleanLabel(),
            album = album.cleanLabel(),
            durationMs = durationMs,
            streamUrl = "",
            videoUrl = videoUrl,
            thumbnailUrl = thumbnailUrl,
            largeThumbnailUrl = largeThumbnailUrl,
            source = source,
            moodTags = tags,
            energy = (45 + seed % 52).coerceIn(0, 100),
            vocal = (35 + (seed / 3) % 60).coerceIn(0, 100),
            replayScore = (62 + (seed / 7) % 38).coerceIn(0, 100),
            cacheScore = (48 + (seed / 11) % 50).coerceIn(0, 100),
            accentStart = palette(seed).first,
            accentEnd = palette(seed).second
        )
    }

    private fun collectObjectsByKey(value: Any?, key: String, out: MutableList<JSONObject>) {
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val current = keys.next()
                    val child = value.opt(current)
                    if (current == key && child is JSONObject) out += child
                    collectObjectsByKey(child, key, out)
                }
            }
            is JSONArray -> for (i in 0 until value.length()) collectObjectsByKey(value.opt(i), key, out)
        }
    }

    private fun findStringUnderKey(value: Any?, key: String): String? {
        when (value) {
            is JSONObject -> {
                val direct = value.optString(key).takeIf { it.isNotBlank() }
                if (direct != null) return direct
                val keys = value.keys()
                while (keys.hasNext()) {
                    val result = findStringUnderKey(value.opt(keys.next()), key)
                    if (!result.isNullOrBlank()) return result
                }
            }
            is JSONArray -> for (i in 0 until value.length()) {
                val result = findStringUnderKey(value.opt(i), key)
                if (!result.isNullOrBlank()) return result
            }
        }
        return null
    }

    private fun extractFlexLines(renderer: JSONObject): List<String> {
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

    private fun findBestThumbnail(renderer: JSONObject): String {
        val arrays = mutableListOf<JSONArray>()
        collectArraysByKey(renderer, "thumbnails", arrays)
        return arrays.asSequence().map { it.bestThumbnail() }.firstOrNull { it.isNotBlank() }.orEmpty()
    }

    private fun collectArraysByKey(value: Any?, key: String, out: MutableList<JSONArray>) {
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val current = keys.next()
                    val child = value.opt(current)
                    if (current == key && child is JSONArray) out += child
                    collectArraysByKey(child, key, out)
                }
            }
            is JSONArray -> for (i in 0 until value.length()) collectArraysByKey(value.opt(i), key, out)
        }
    }

    private fun extractDuration(text: String): Long {
        val match = Regex("\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\b").find(text)?.value ?: return 0L
        return match.durationToMs()
    }

    private fun extractVideoId(url: String): String {
        val patterns = listOf(
            Regex("[?&]v=([^&]+)"),
            Regex("youtu\\.be/([^?&/]+)"),
            Regex("shorts/([^?&/]+)")
        )
        return patterns.firstNotNullOfOrNull { it.find(url)?.groupValues?.getOrNull(1) }.orEmpty()
    }

    private fun secondsToMs(seconds: Long): Long {
        return if (seconds > 0) seconds * 1000L else 0L
    }

    private fun JSONArray.joinText(): String {
        val parts = mutableListOf<String>()
        for (i in 0 until length()) {
            val text = optJSONObject(i)?.optString("text").orEmpty()
            if (text.isNotBlank()) parts += text
        }
        return parts.joinToString("").replace("  ", " ").trim()
    }

    private fun JSONArray.bestThumbnail(): String {
        var bestUrl = ""
        var bestScore = -1
        for (i in 0 until length()) {
            val item = optJSONObject(i) ?: continue
            val url = item.optString("url")
            val score = item.optInt("width", 0) * item.optInt("height", 0)
            if (url.isNotBlank() && score >= bestScore) {
                bestUrl = url
                bestScore = score
            }
        }
        return bestUrl
    }

    private fun String.durationToMs(): Long {
        val parts = split(":").mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            2 -> (parts[0] * 60L + parts[1]) * 1000L
            3 -> (parts[0] * 3600L + parts[1] * 60L + parts[2]) * 1000L
            else -> 0L
        }
    }

    private fun String.cleanLabel(): String {
        return replace("\\n", " ").replace(Regex("\\s+"), " ").trim()
    }

    private fun upgradeThumbnail(url: String): String {
        if (url.isBlank()) return url
        return url.replace(Regex("=w\\d+-h\\d+.*$"), "=w1200-h1200-l90-rj")
            .replace(Regex("=s\\d+.*$"), "=s1200")
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

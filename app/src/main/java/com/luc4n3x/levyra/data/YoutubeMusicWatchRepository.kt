package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.security.GoogleApiKeyHeaders
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

data class YoutubeMusicWatchArtist(
    val name: String,
    val browseId: String
)

data class YoutubeMusicFeedbackTokens(
    val add: String = "",
    val remove: String = "",
    val pin: String = "",
    val unpin: String = ""
)

data class YoutubeMusicWatchTrack(
    val videoId: String,
    val title: String,
    val artists: List<YoutubeMusicWatchArtist>,
    val albumTitle: String,
    val albumBrowseId: String,
    val durationMs: Long,
    val thumbnailUrl: String,
    val videoType: String,
    val explicit: Boolean,
    val year: String = "",
    val likeStatus: String = "INDIFFERENT",
    val inLibrary: Boolean = false,
    val pinnedToListenAgain: Boolean = false,
    val feedbackTokens: YoutubeMusicFeedbackTokens = YoutubeMusicFeedbackTokens(),
    val counterpart: YoutubeMusicWatchTrack? = null
)

data class YoutubeMusicWatchPlaylist(
    val tracks: List<YoutubeMusicWatchTrack>,
    val playlistId: String,
    val lyricsBrowseId: String,
    val relatedBrowseId: String,
    val continuation: String,
    val radio: Boolean = false,
    val shuffled: Boolean = false
)

enum class YoutubeMusicRelatedType {
    Song,
    Video,
    Album,
    Playlist,
    Artist,
    Unknown
}

data class YoutubeMusicRelatedItem(
    val type: YoutubeMusicRelatedType,
    val title: String,
    val videoId: String,
    val browseId: String,
    val playlistId: String,
    val artists: List<YoutubeMusicWatchArtist>,
    val albumTitle: String,
    val albumBrowseId: String,
    val durationMs: Long,
    val thumbnailUrl: String,
    val description: String,
    val year: String,
    val explicit: Boolean
)

data class YoutubeMusicRelatedSection(
    val title: String,
    val items: List<YoutubeMusicRelatedItem>,
    val description: String = ""
)

data class YoutubeMusicNativeLyrics(
    val synced: Boolean,
    val lines: List<LyricLine>,
    val source: String
)

class YoutubeMusicWatchRepository(private val context: Context? = null) {
    private val apiKey = BuildConfig.YOUTUBE_INNERTUBE_API_KEY
    private val client = LevyraHttpClientFactory.youtubePlayer()

    suspend fun getWatchPlaylist(
        videoId: String,
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        limit: Int = 25
    ): YoutubeMusicWatchPlaylist = getWatchPlaylistAdvanced(
        videoId = videoId,
        playlistId = "",
        languageCode = languageCode,
        limit = limit,
        radio = false,
        shuffle = false
    )

    suspend fun getWatchPlaylistAdvanced(
        videoId: String = "",
        playlistId: String = "",
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        limit: Int = 25,
        radio: Boolean = false,
        shuffle: Boolean = false
    ): YoutubeMusicWatchPlaylist = withContext(Dispatchers.IO) {
        val cleanVideoId = videoId.trim()
        val cleanPlaylistId = playlistId.trim().removePrefix("VL")
        require(cleanVideoId.isNotBlank() || cleanPlaylistId.isNotBlank())
        require(!(radio && shuffle))
        val boundedLimit = limit.coerceIn(1, 100)
        val effectivePlaylistId = cleanPlaylistId.ifBlank { "RDAMVM$cleanVideoId" }
        val requestBody = JSONObject()
            .put("enablePersistentPlaylistPanel", true)
            .put("isAudioOnly", true)
            .put("tunerSettingValue", "AUTOMIX_SETTING_NORMAL")
            .put("playlistId", effectivePlaylistId)
        if (cleanVideoId.isNotBlank()) requestBody.put("videoId", cleanVideoId)
        if (shuffle) {
            requestBody.put("params", SHUFFLE_PARAMS)
        } else if (radio) {
            requestBody.put("params", RADIO_PARAMS)
        } else {
            requestBody.put(
                "watchEndpointMusicSupportedConfigs",
                JSONObject().put(
                    "watchEndpointMusicConfig",
                    JSONObject()
                        .put("hasPersistentPlaylistPanel", true)
                        .put("musicVideoType", "MUSIC_VIDEO_TYPE_ATV")
                )
            )
        }
        val cacheKey = listOf(
            cleanVideoId,
            effectivePlaylistId,
            languageCode.lowercase(Locale.ROOT),
            radio.toString(),
            shuffle.toString()
        ).joinToString("|")
        val cachedPlaylist = cached(watchCache, cacheKey, WATCH_CACHE_TTL_MS)
        if (cachedPlaylist != null && (cachedPlaylist.tracks.size >= boundedLimit || cachedPlaylist.continuation.isBlank())) {
            return@withContext cachedPlaylist.copy(tracks = cachedPlaylist.tracks.take(boundedLimit))
        }

        val parsedInitial = cachedPlaylist ?: YoutubeMusicWatchParser.parseWatchPlaylist(
            post("next", requestBody, languageCode, mobile = false)
        ).copy(radio = radio, shuffled = shuffle)
        var parsed = parsedInitial
        val tracks = LinkedHashMap<String, YoutubeMusicWatchTrack>()
        parsed.tracks.forEach { track -> tracks.putIfAbsent(track.videoId, track) }
        var continuation = parsed.continuation
        var requests = 0

        while (tracks.size < boundedLimit && continuation.isNotBlank() && requests < MAX_CONTINUATION_REQUESTS) {
            val response = runCatching {
                post("next", requestBody, languageCode, mobile = false, continuation = continuation)
            }.getOrNull() ?: break
            val nextPage = YoutubeMusicWatchParser.parseWatchPlaylist(response)
            val before = tracks.size
            nextPage.tracks.forEach { track -> tracks.putIfAbsent(track.videoId, track) }
            continuation = nextPage.continuation
            parsed = parsed.copy(
                playlistId = parsed.playlistId.ifBlank { nextPage.playlistId.ifBlank { effectivePlaylistId } },
                lyricsBrowseId = parsed.lyricsBrowseId.ifBlank { nextPage.lyricsBrowseId },
                relatedBrowseId = parsed.relatedBrowseId.ifBlank { nextPage.relatedBrowseId },
                continuation = continuation,
                radio = radio,
                shuffled = shuffle
            )
            requests += 1
            if (tracks.size == before) break
        }

        val fullResult = parsed.copy(
            tracks = tracks.values.toList(),
            playlistId = parsed.playlistId.ifBlank { effectivePlaylistId },
            continuation = continuation,
            radio = radio,
            shuffled = shuffle
        )
        putCached(watchCache, cacheKey, fullResult, WATCH_CACHE_MAX_ENTRIES)
        fullResult.copy(tracks = fullResult.tracks.take(boundedLimit))
    }

    suspend fun getSongRelated(
        browseId: String,
        languageCode: String = LevyraLanguageCatalog.deviceDefault()
    ): List<YoutubeMusicRelatedSection> = withContext(Dispatchers.IO) {
        val cleanBrowseId = browseId.trim()
        require(cleanBrowseId.isNotBlank())
        val cacheKey = "$cleanBrowseId|${languageCode.lowercase(Locale.ROOT)}"
        cached(relatedCache, cacheKey, RELATED_CACHE_TTL_MS)?.let { return@withContext it }
        val response = post("browse", JSONObject().put("browseId", cleanBrowseId), languageCode, mobile = false)
        val result = YoutubeMusicWatchParser.parseRelated(response)
        putCached(relatedCache, cacheKey, result, RELATED_CACHE_MAX_ENTRIES)
        result
    }

    suspend fun getLyricsForVideo(
        videoId: String,
        languageCode: String = LevyraLanguageCatalog.deviceDefault()
    ): YoutubeMusicNativeLyrics? = withContext(Dispatchers.IO) {
        val watch = runCatching { getWatchPlaylist(videoId, languageCode, 1) }.getOrNull() ?: return@withContext null
        if (watch.lyricsBrowseId.isBlank()) return@withContext null
        getLyrics(watch.lyricsBrowseId, languageCode)
    }

    suspend fun getLyrics(
        browseId: String,
        languageCode: String = LevyraLanguageCatalog.deviceDefault()
    ): YoutubeMusicNativeLyrics? = withContext(Dispatchers.IO) {
        val cleanBrowseId = browseId.trim()
        if (cleanBrowseId.isBlank()) return@withContext null
        val cacheKey = "$cleanBrowseId|${languageCode.lowercase(Locale.ROOT)}"
        cached(lyricsCache, cacheKey, LYRICS_CACHE_TTL_MS)?.let { return@withContext it }

        val body = JSONObject().put("browseId", cleanBrowseId)
        val mobileResponse = runCatching { post("browse", body, languageCode, mobile = true) }.getOrNull()
        val timed = mobileResponse?.let(YoutubeMusicWatchParser::parseLyrics)
        if (timed != null && timed.lines.isNotEmpty()) {
            putCached(lyricsCache, cacheKey, timed, LYRICS_CACHE_MAX_ENTRIES)
            return@withContext timed
        }

        val webResponse = runCatching { post("browse", body, languageCode, mobile = false) }.getOrNull()
        val plain = webResponse?.let(YoutubeMusicWatchParser::parseLyrics)
        if (plain != null && plain.lines.isNotEmpty()) {
            putCached(lyricsCache, cacheKey, plain, LYRICS_CACHE_MAX_ENTRIES)
        }
        plain
    }

    private fun post(
        endpoint: String,
        payload: JSONObject,
        languageCode: String,
        mobile: Boolean,
        continuation: String = ""
    ): JSONObject {
        check(apiKey.isNotBlank())
        val continuationQuery = continuation.takeIf { it.isNotBlank() }?.let {
            val encoded = URLEncoder.encode(it, StandardCharsets.UTF_8.name())
            "&ctoken=$encoded&continuation=$encoded"
        }.orEmpty()
        val url = "https://music.youtube.com/youtubei/v1/$endpoint?key=$apiKey&prettyPrint=false$continuationQuery"
        val body = JSONObject(payload.toString())
            .put("context", JSONObject().put("client", clientPayload(languageCode, mobile)).put("user", JSONObject()))
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)
        val requestBuilder = Request.Builder()
            .url(url)
            .post(body)
            .header("Accept", "application/json")
            .header("Accept-Encoding", "br,gzip")
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/")
            .header("User-Agent", if (mobile) MOBILE_USER_AGENT else WEB_USER_AGENT)
            .header("X-Youtube-Client-Name", if (mobile) ANDROID_MUSIC_CLIENT_ID else WEB_REMIX_CLIENT_ID)
            .header("X-Youtube-Client-Version", if (mobile) ANDROID_MUSIC_CLIENT_VERSION else WEB_REMIX_CLIENT_VERSION)
        GoogleApiKeyHeaders.applyTo(requestBuilder, context)
        return client.newCall(requestBuilder.build()).execute().use { response ->
            val responseBody = response.body.string()
            if (!response.isSuccessful || responseBody.isBlank()) {
                throw YoutubeMusicRequestException(endpoint, response.code, responseBody.take(512))
            }
            JSONObject(responseBody)
        }
    }

    private fun clientPayload(languageCode: String, mobile: Boolean): JSONObject {
        val locale = LevyraContentLocales.forLanguage(languageCode)
        return if (mobile) {
            JSONObject()
                .put("clientName", "ANDROID_MUSIC")
                .put("clientVersion", ANDROID_MUSIC_CLIENT_VERSION)
                .put("hl", locale.hl)
                .put("gl", locale.gl)
                .put("androidSdkVersion", 35)
                .put("platform", "MOBILE")
        } else {
            JSONObject()
                .put("clientName", "WEB_REMIX")
                .put("clientVersion", WEB_REMIX_CLIENT_VERSION)
                .put("hl", locale.hl)
                .put("gl", locale.gl)
                .put("platform", "DESKTOP")
        }
    }

    private fun <T> cached(cache: LinkedHashMap<String, TimedValue<T>>, key: String, ttl: Long): T? {
        val value = synchronized(cache) { cache[key] } ?: return null
        if (System.currentTimeMillis() - value.createdAt > ttl) {
            synchronized(cache) { cache.remove(key) }
            return null
        }
        return value.value
    }

    private fun <T> putCached(cache: LinkedHashMap<String, TimedValue<T>>, key: String, value: T, maxEntries: Int) {
        synchronized(cache) {
            cache[key] = TimedValue(value, System.currentTimeMillis())
            while (cache.size > maxEntries) {
                val oldest = cache.entries.firstOrNull()?.key ?: break
                cache.remove(oldest)
            }
        }
    }

    private data class TimedValue<T>(val value: T, val createdAt: Long)

    companion object {
        private const val WEB_REMIX_CLIENT_VERSION = "1.20260423.01.00"
        private const val WEB_REMIX_CLIENT_ID = "67"
        private const val ANDROID_MUSIC_CLIENT_VERSION = "7.21.50"
        private const val ANDROID_MUSIC_CLIENT_ID = "21"
        private const val WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36"
        private const val MOBILE_USER_AGENT = "com.google.android.apps.youtube.music/7.21.50 (Linux; U; Android 15) gzip"
        private const val MAX_CONTINUATION_REQUESTS = 4
        private const val RADIO_PARAMS = "wAEB"
        private const val SHUFFLE_PARAMS = "wAEB8gECKAE%3D"
        private const val WATCH_CACHE_TTL_MS = 20L * 60L * 1000L
        private const val RELATED_CACHE_TTL_MS = 6L * 60L * 60L * 1000L
        private const val LYRICS_CACHE_TTL_MS = 30L * 24L * 60L * 60L * 1000L
        private const val WATCH_CACHE_MAX_ENTRIES = 64
        private const val RELATED_CACHE_MAX_ENTRIES = 64
        private const val LYRICS_CACHE_MAX_ENTRIES = 128
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val watchCache = LinkedHashMap<String, TimedValue<YoutubeMusicWatchPlaylist>>()
        private val relatedCache = LinkedHashMap<String, TimedValue<List<YoutubeMusicRelatedSection>>>()
        private val lyricsCache = LinkedHashMap<String, TimedValue<YoutubeMusicNativeLyrics>>()
    }
}

class YoutubeMusicRequestException(endpoint: String, statusCode: Int, response: String) :
    IllegalStateException("YouTube Music $endpoint failed with HTTP $statusCode: $response")

internal object YoutubeMusicWatchParser {
    fun parseWatchPlaylist(root: JSONObject): YoutubeMusicWatchPlaylist {
        val browseIds = extractTabBrowseIds(root)
        val tracks = ArrayList<YoutubeMusicWatchTrack>()
        val counterpartIds = HashSet<String>()
        val wrappers = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "playlistPanelVideoWrapperRenderer", wrappers)
        wrappers.forEach { wrapper ->
            val primary = wrapper.optJSONObject("primaryRenderer")
                ?.optJSONObject("playlistPanelVideoRenderer")
                ?: return@forEach
            if (primary.has("unplayableText")) return@forEach
            val counterpartRenderer = wrapper.optJSONArray("counterpart")
                ?.optJSONObject(0)
                ?.optJSONObject("counterpartRenderer")
                ?.optJSONObject("playlistPanelVideoRenderer")
            val counterpart = counterpartRenderer?.takeUnless { it.has("unplayableText") }?.let(::parseWatchTrack)
            counterpart?.videoId?.takeIf { it.isNotBlank() }?.let(counterpartIds::add)
            parseWatchTrack(primary)?.copy(counterpart = counterpart)?.let(tracks::add)
        }

        val direct = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "playlistPanelVideoRenderer", direct)
        direct.forEach { renderer ->
            if (renderer.has("unplayableText")) return@forEach
            parseWatchTrack(renderer)?.let { track ->
                if (track.videoId !in counterpartIds && tracks.none { it.videoId == track.videoId }) tracks += track
            }
        }

        return YoutubeMusicWatchPlaylist(
            tracks = tracks,
            playlistId = findPlaylistId(root),
            lyricsBrowseId = browseIds["MUSIC_PAGE_TYPE_TRACK_LYRICS"].orEmpty(),
            relatedBrowseId = browseIds["MUSIC_PAGE_TYPE_TRACK_RELATED"].orEmpty(),
            continuation = findContinuation(root)
        )
    }

    fun parseRelated(root: JSONObject): List<YoutubeMusicRelatedSection> {
        val sections = ArrayList<YoutubeMusicRelatedSection>()
        val carousels = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicCarouselShelfRenderer", carousels)
        carousels.forEach { carousel ->
            val title = carousel.optJSONObject("header")
                ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                ?.optJSONObject("title")
                ?.optJSONArray("runs")
                .joinText()
                .ifBlank { carousel.optJSONObject("header")?.findText().orEmpty() }
            val items = parseRelatedContents(carousel.optJSONArray("contents"))
            if (items.isNotEmpty()) sections += YoutubeMusicRelatedSection(title, items)
        }

        val shelves = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicShelfRenderer", shelves)
        shelves.forEach { shelf ->
            val title = shelf.optJSONObject("title")?.optJSONArray("runs").joinText()
            val items = parseRelatedContents(shelf.optJSONArray("contents"))
            if (items.isNotEmpty() && sections.none { it.title == title && it.items == items }) {
                sections += YoutubeMusicRelatedSection(title, items)
            }
        }

        val descriptions = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicDescriptionShelfRenderer", descriptions)
        descriptions.forEach { shelf ->
            val title = shelf.optJSONObject("header")?.optJSONArray("runs").joinText()
            val description = shelf.optJSONObject("description")?.optJSONArray("runs").joinText(preserveNewLines = true)
            if (description.isNotBlank() && sections.none { it.title == title && it.description == description }) {
                sections += YoutubeMusicRelatedSection(title, emptyList(), description)
            }
        }

        if (sections.isEmpty()) {
            val fallbackItems = mutableListOf<YoutubeMusicRelatedItem>()
            val responsive = mutableListOf<JSONObject>()
            collectObjectsByKey(root, "musicResponsiveListItemRenderer", responsive)
            responsive.mapNotNullTo(fallbackItems, ::parseRelatedRenderer)
            val twoRows = mutableListOf<JSONObject>()
            collectObjectsByKey(root, "musicTwoRowItemRenderer", twoRows)
            twoRows.mapNotNullTo(fallbackItems, ::parseRelatedRenderer)
            if (fallbackItems.isNotEmpty()) sections += YoutubeMusicRelatedSection("", fallbackItems.distinctBy { it.identityKey() })
        }

        return sections
    }

    fun parseLyrics(root: JSONObject): YoutubeMusicNativeLyrics? {
        val timedModel = root
            .optJSONObject("contents")
            ?.optJSONObject("elementRenderer")
            ?.optJSONObject("newElement")
            ?.optJSONObject("type")
            ?.optJSONObject("componentType")
            ?.optJSONObject("model")
            ?.optJSONObject("timedLyricsModel")
            ?.optJSONObject("lyricsData")
        val timedData = timedModel?.optJSONArray("timedLyricsData")
        if (timedData != null) {
            val lines = ArrayList<LyricLine>()
            for (index in 0 until timedData.length()) {
                val item = timedData.optJSONObject(index) ?: continue
                val text = item.optString("lyricLine").trim()
                val cue = item.optJSONObject("cueRange") ?: continue
                val start = cue.optString("startTimeMilliseconds").toLongOrNull()
                    ?: cue.optLong("startTimeMilliseconds", -1L)
                val end = cue.optString("endTimeMilliseconds").toLongOrNull()
                    ?: cue.optLong("endTimeMilliseconds", -1L)
                if (text.isBlank() || start < 0L || end <= start) continue
                lines += LyricLine(start, end, text, "")
            }
            if (lines.isNotEmpty()) {
                return YoutubeMusicNativeLyrics(
                    synced = true,
                    lines = lines.sortedBy { it.startMs },
                    source = timedModel.optString("sourceMessage").cleanSource()
                )
            }
        }

        val descriptions = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicDescriptionShelfRenderer", descriptions)
        descriptions.forEach { shelf ->
            val text = shelf.optJSONObject("description")?.optJSONArray("runs").joinText(preserveNewLines = true).trim()
            if (text.length < 16) return@forEach
            val source = shelf.optJSONArray("runs").joinText().cleanSource()
                .ifBlank { shelf.optJSONObject("footer")?.optJSONArray("runs").joinText().cleanSource() }
                .ifBlank { shelf.optJSONObject("subheader")?.optJSONArray("runs").joinText().cleanSource() }
            val lines = plainLyricsLines(text)
            if (lines.isNotEmpty()) return YoutubeMusicNativeLyrics(false, lines, source)
        }

        return null
    }

    private fun parseRelatedContents(contents: JSONArray?): List<YoutubeMusicRelatedItem> {
        if (contents == null) return emptyList()
        val items = ArrayList<YoutubeMusicRelatedItem>()
        for (index in 0 until contents.length()) {
            val wrapper = contents.optJSONObject(index) ?: continue
            val renderer = wrapper.optJSONObject("musicResponsiveListItemRenderer")
                ?: wrapper.optJSONObject("musicTwoRowItemRenderer")
                ?: wrapper.optJSONObject("musicNavigationButtonRenderer")
                ?: continue
            parseRelatedRenderer(renderer)?.let(items::add)
        }
        return items.distinctBy { it.identityKey() }
    }

    private fun parseRelatedRenderer(renderer: JSONObject): YoutubeMusicRelatedItem? {
        val title = renderer.optJSONObject("title")?.optJSONArray("runs").joinText()
            .ifBlank { renderer.extractFlexLines().firstOrNull().orEmpty() }
            .ifBlank { renderer.optJSONObject("buttonText")?.optJSONArray("runs").joinText() }
            .trim()
        if (title.isBlank()) return null
        val watchEndpoints = mutableListOf<JSONObject>()
        collectObjectsByKey(renderer, "watchEndpoint", watchEndpoints)
        val browseEndpoints = mutableListOf<JSONObject>()
        collectObjectsByKey(renderer, "browseEndpoint", browseEndpoints)
        val watch = watchEndpoints.firstOrNull { it.optString("videoId").isNotBlank() }
        val topLevelBrowse = renderer.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
        val browse = topLevelBrowse?.takeIf { it.optString("browseId").isNotBlank() }
            ?: browseEndpoints.firstOrNull { endpoint ->
                endpoint.optJSONObject("browseEndpointContextSupportedConfigs")
                    ?.optJSONObject("browseEndpointContextMusicConfig")
                    ?.optString("pageType")
                    ?.let { pageType -> pageType.contains("ALBUM") || pageType.contains("PLAYLIST") }
                    ?: false
            }
            ?: browseEndpoints.firstOrNull { endpoint ->
                endpoint.optJSONObject("browseEndpointContextSupportedConfigs")
                    ?.optJSONObject("browseEndpointContextMusicConfig")
                    ?.optString("pageType")
                    ?.let { pageType -> pageType.contains("ARTIST") || pageType.contains("USER_CHANNEL") }
                    ?: false
            }
            ?: browseEndpoints.firstOrNull { it.optString("browseId").isNotBlank() }
        val videoId = watch?.optString("videoId").orEmpty()
        val playlistId = watch?.optString("playlistId").orEmpty().ifBlank {
            browseEndpoints.firstNotNullOfOrNull { endpoint -> endpoint.optString("browseId").takeIf { it.startsWith("VL") }?.removePrefix("VL") }.orEmpty()
        }
        val browseId = browse?.optString("browseId").orEmpty()
        val pageType = browse?.optJSONObject("browseEndpointContextSupportedConfigs")
            ?.optJSONObject("browseEndpointContextMusicConfig")
            ?.optString("pageType")
            .orEmpty()
        val type = when {
            videoId.isNotBlank() && watch?.optJSONObject("watchEndpointMusicSupportedConfigs")
                ?.optJSONObject("watchEndpointMusicConfig")
                ?.optString("musicVideoType")
                ?.contains("OMV") == true -> YoutubeMusicRelatedType.Video
            videoId.isNotBlank() -> YoutubeMusicRelatedType.Song
            pageType.contains("ALBUM") || browseId.startsWith("MPRE") -> YoutubeMusicRelatedType.Album
            pageType.contains("ARTIST") || pageType.contains("USER_CHANNEL") || browseId.startsWith("UC") -> YoutubeMusicRelatedType.Artist
            pageType.contains("PLAYLIST") || browseId.startsWith("VL") || playlistId.isNotBlank() -> YoutubeMusicRelatedType.Playlist
            else -> YoutubeMusicRelatedType.Unknown
        }
        val runs = renderer.allTextRuns()
        val artists = runs.mapNotNull { run ->
            val endpoint = run.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint") ?: return@mapNotNull null
            val id = endpoint.optString("browseId")
            val runPageType = endpoint.optJSONObject("browseEndpointContextSupportedConfigs")
                ?.optJSONObject("browseEndpointContextMusicConfig")
                ?.optString("pageType")
                .orEmpty()
            if (id.startsWith("UC") || runPageType.contains("ARTIST")) YoutubeMusicWatchArtist(run.optString("text"), id) else null
        }.filter { it.name.isNotBlank() }.distinctBy { it.browseId.ifBlank { it.name.lowercase(Locale.ROOT) } }
        val albumRun = runs.firstOrNull { run ->
            val endpoint = run.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
            endpoint?.optString("browseId")?.startsWith("MPRE") == true
        }
        val subtitle = renderer.extractFlexLines().drop(1).joinToString(" • ").ifBlank {
            renderer.optJSONObject("subtitle")?.optJSONArray("runs").joinText()
        }
        val durationMs = Regex("\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\b")
            .find(renderer.toString())
            ?.value
            ?.durationToMs()
            ?: 0L
        val year = Regex("\\b(?:19|20)\\d{2}\\b").find(subtitle)?.value.orEmpty()
        return YoutubeMusicRelatedItem(
            type = type,
            title = title,
            videoId = videoId,
            browseId = browseId,
            playlistId = playlistId,
            artists = artists,
            albumTitle = albumRun?.optString("text").orEmpty(),
            albumBrowseId = albumRun?.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty(),
            durationMs = durationMs,
            thumbnailUrl = renderer.findBestThumbnail(),
            description = subtitle,
            year = year,
            explicit = renderer.toString().contains("MUSIC_ITEM_BADGE_EXPLICIT")
        )
    }

    private fun parseWatchTrack(renderer: JSONObject): YoutubeMusicWatchTrack? {
        val videoId = renderer.optString("videoId")
            .ifBlank { renderer.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId").orEmpty() }
        val title = renderer.optJSONObject("title")?.optJSONArray("runs").joinText().trim()
        if (videoId.isBlank() || title.isBlank()) return null
        val bylineRuns = renderer.optJSONObject("longBylineText")?.optJSONArray("runs") ?: JSONArray()
        val artists = ArrayList<YoutubeMusicWatchArtist>()
        var albumTitle = ""
        var albumBrowseId = ""
        for (index in 0 until bylineRuns.length()) {
            val run = bylineRuns.optJSONObject(index) ?: continue
            val text = run.optString("text").trim()
            val endpoint = run.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
            val browseId = endpoint?.optString("browseId").orEmpty()
            val pageType = endpoint?.optJSONObject("browseEndpointContextSupportedConfigs")
                ?.optJSONObject("browseEndpointContextMusicConfig")
                ?.optString("pageType")
                .orEmpty()
            when {
                browseId.startsWith("MPRE") || pageType.contains("ALBUM") -> {
                    albumTitle = text
                    albumBrowseId = browseId
                }
                browseId.startsWith("UC") || pageType.contains("ARTIST") || pageType.contains("USER_CHANNEL") -> {
                    if (text.isNotBlank()) artists += YoutubeMusicWatchArtist(text, browseId)
                }
            }
        }
        if (artists.isEmpty()) {
            val byline = bylineRuns.joinText()
            val fallback = byline.split(" • ", " · ").firstOrNull()?.trim().orEmpty()
            if (fallback.isNotBlank()) artists += YoutubeMusicWatchArtist(fallback, "")
        }
        val length = renderer.optJSONObject("lengthText")?.optJSONArray("runs").joinText()
            .ifBlank { renderer.optJSONObject("lengthText")?.optString("simpleText").orEmpty() }
        val videoType = renderer.optJSONObject("navigationEndpoint")
            ?.optJSONObject("watchEndpoint")
            ?.optJSONObject("watchEndpointMusicSupportedConfigs")
            ?.optJSONObject("watchEndpointMusicConfig")
            ?.optString("musicVideoType")
            .orEmpty()
        val byline = bylineRuns.joinText()
        val year = Regex("\\b(?:19|20)\\d{2}\\b").find(byline)?.value.orEmpty()
        val menuData = parseSongMenuData(renderer)
        val textBlob = renderer.toString()
        return YoutubeMusicWatchTrack(
            videoId = videoId,
            title = title,
            artists = artists.distinctBy { it.browseId.ifBlank { it.name.lowercase(Locale.ROOT) } },
            albumTitle = albumTitle,
            albumBrowseId = albumBrowseId,
            durationMs = length.durationToMs(),
            thumbnailUrl = renderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails").bestThumbnail(),
            videoType = videoType,
            explicit = textBlob.contains("MUSIC_ITEM_BADGE_EXPLICIT"),
            year = year,
            likeStatus = menuData.likeStatus,
            inLibrary = menuData.inLibrary,
            pinnedToListenAgain = menuData.pinnedToListenAgain,
            feedbackTokens = menuData.feedbackTokens
        )
    }

    private fun parseSongMenuData(renderer: JSONObject): SongMenuData {
        val items = renderer.optJSONObject("menu")
            ?.optJSONObject("menuRenderer")
            ?.optJSONArray("items")
            ?: JSONArray()
        var likeStatus = "INDIFFERENT"
        var inLibrary = false
        var pinnedToListenAgain = false
        var addToken = ""
        var removeToken = ""
        var pinToken = ""
        var unpinToken = ""
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val toggle = item.optJSONObject("toggleMenuServiceItemRenderer")
            val service = item.optJSONObject("menuServiceItemRenderer")
            val menuItem = toggle ?: service ?: continue
            val likeAction = menuItem.optJSONObject("defaultServiceEndpoint")
                ?.optJSONObject("likeEndpoint")
                ?.optString("status")
                .orEmpty()
            likeStatus = when (likeAction.uppercase(Locale.ROOT)) {
                "LIKE" -> "INDIFFERENT"
                "INDIFFERENT" -> "LIKE"
                else -> likeStatus
            }
            val iconType = menuItem.optJSONObject("defaultIcon")
                ?.optString("iconType")
                .orEmpty()
                .ifBlank { menuItem.optJSONObject("icon")?.optString("iconType").orEmpty() }
            val defaultToken = feedbackToken(menuItem.optJSONObject("defaultServiceEndpoint"))
            val toggledToken = feedbackToken(menuItem.optJSONObject("toggledServiceEndpoint"))
            when (iconType) {
                "KEEP" -> {
                    pinToken = defaultToken
                    unpinToken = toggledToken
                }
                "KEEP_OFF" -> {
                    pinnedToListenAgain = true
                    pinToken = toggledToken
                    unpinToken = defaultToken
                }
                "BOOKMARK_BORDER" -> {
                    addToken = defaultToken
                    removeToken = toggledToken
                }
                "BOOKMARK" -> {
                    inLibrary = true
                    addToken = toggledToken
                    removeToken = defaultToken
                }
            }
        }
        return SongMenuData(
            likeStatus = likeStatus,
            inLibrary = inLibrary,
            pinnedToListenAgain = pinnedToListenAgain,
            feedbackTokens = YoutubeMusicFeedbackTokens(
                add = addToken,
                remove = removeToken,
                pin = pinToken,
                unpin = unpinToken
            )
        )
    }

    private fun feedbackToken(endpoint: JSONObject?): String {
        return endpoint?.optJSONObject("feedbackEndpoint")?.optString("feedbackToken").orEmpty()
    }

    private data class SongMenuData(
        val likeStatus: String,
        val inLibrary: Boolean,
        val pinnedToListenAgain: Boolean,
        val feedbackTokens: YoutubeMusicFeedbackTokens
    )

    private fun extractTabBrowseIds(root: JSONObject): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        val tabs = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "tabRenderer", tabs)
        tabs.forEach { tab ->
            if (tab.optBoolean("unselectable")) return@forEach
            val endpoint = tab.optJSONObject("endpoint")?.optJSONObject("browseEndpoint") ?: return@forEach
            val browseId = endpoint.optString("browseId")
            val pageType = endpoint.optJSONObject("browseEndpointContextSupportedConfigs")
                ?.optJSONObject("browseEndpointContextMusicConfig")
                ?.optString("pageType")
                .orEmpty()
            if (browseId.isNotBlank() && pageType.isNotBlank()) result[pageType] = browseId
        }
        if (!result.containsKey("MUSIC_PAGE_TYPE_TRACK_LYRICS")) {
            findAllStrings(root, "browseId").firstOrNull { it.startsWith("MPLYt") }?.let { result["MUSIC_PAGE_TYPE_TRACK_LYRICS"] = it }
        }
        return result
    }



    private fun findPlaylistId(root: JSONObject): String {
        val endpoints = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "watchEndpoint", endpoints)
        return endpoints.firstNotNullOfOrNull { endpoint -> endpoint.optString("playlistId").takeIf { it.isNotBlank() } }.orEmpty()
    }

    private fun findContinuation(root: JSONObject): String {
        val candidates = listOf("nextRadioContinuationData", "nextContinuationData", "reloadContinuationData", "continuationCommand")
        candidates.forEach { key ->
            val objects = mutableListOf<JSONObject>()
            collectObjectsByKey(root, key, objects)
            objects.firstNotNullOfOrNull { value ->
                value.optString("continuation").ifBlank { value.optString("token") }.takeIf(String::isNotBlank)
            }?.let { return it }
        }
        return ""
    }

    private fun plainLyricsLines(text: String): List<LyricLine> {
        val lines = text.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
        return lines.mapIndexed { index, line ->
            val start = index * 4_200L
            LyricLine(start, start + 4_200L, line, "")
        }
    }

    private fun YoutubeMusicRelatedItem.identityKey(): String {
        return when {
            videoId.isNotBlank() -> "v:$videoId"
            browseId.isNotBlank() -> "b:$browseId"
            playlistId.isNotBlank() -> "p:$playlistId"
            else -> "t:${title.lowercase(Locale.ROOT)}"
        }
    }

    private fun JSONObject.extractFlexLines(): List<String> {
        val lines = ArrayList<String>()
        val columns = optJSONArray("flexColumns") ?: JSONArray()
        for (index in 0 until columns.length()) {
            val value = columns.optJSONObject(index)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
                .joinText()
                .trim()
            if (value.isNotBlank()) lines += value
        }
        return lines
    }

    private fun JSONObject.allTextRuns(): List<JSONObject> {
        val arrays = mutableListOf<JSONArray>()
        collectArraysByKey(this, "runs", arrays)
        val result = ArrayList<JSONObject>()
        arrays.forEach { array ->
            for (index in 0 until array.length()) array.optJSONObject(index)?.let(result::add)
        }
        return result
    }

    private fun JSONObject.findText(): String {
        val arrays = mutableListOf<JSONArray>()
        collectArraysByKey(this, "runs", arrays)
        return arrays.firstNotNullOfOrNull { array -> array.joinText().takeIf(String::isNotBlank) }.orEmpty()
    }

    private fun JSONObject.findBestThumbnail(): String {
        val arrays = mutableListOf<JSONArray>()
        collectArraysByKey(this, "thumbnails", arrays)
        return arrays.asSequence().map { it.bestThumbnail() }.firstOrNull { it.isNotBlank() }.orEmpty()
    }

    private fun JSONArray?.joinText(preserveNewLines: Boolean = false): String {
        if (this == null) return ""
        val builder = StringBuilder()
        for (index in 0 until length()) {
            val text = optJSONObject(index)?.optString("text").orEmpty()
            if (text.isBlank()) continue
            if (preserveNewLines && builder.isNotEmpty() && text.startsWith("\n").not() && builder.lastOrNull() != '\n') {
                builder.append('\n')
            }
            builder.append(text)
        }
        return builder.toString().replace("\\n", "\n").trim()
    }

    private fun JSONArray?.bestThumbnail(): String {
        if (this == null) return ""
        var best = ""
        var bestArea = -1L
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val url = item.optString("url")
            val area = item.optLong("width", 0L) * item.optLong("height", 0L)
            if (url.isNotBlank() && area >= bestArea) {
                best = url
                bestArea = area
            }
        }
        return best
    }

    private fun String.durationToMs(): Long {
        val parts = split(":").mapNotNull(String::toLongOrNull)
        return when (parts.size) {
            2 -> (parts[0] * 60L + parts[1]) * 1_000L
            3 -> (parts[0] * 3_600L + parts[1] * 60L + parts[2]) * 1_000L
            else -> 0L
        }
    }

    private fun String.cleanSource(): String {
        return replace(Regex("(?i)^source:\\s*"), "").trim()
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
            is JSONArray -> for (index in 0 until value.length()) collectObjectsByKey(value.opt(index), key, out)
        }
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
            is JSONArray -> for (index in 0 until value.length()) collectArraysByKey(value.opt(index), key, out)
        }
    }

    private fun findAllStrings(value: Any?, key: String): List<String> {
        val result = ArrayList<String>()
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val current = keys.next()
                    val child = value.opt(current)
                    if (current == key && child is String && child.isNotBlank()) result += child
                    result += findAllStrings(child, key)
                }
            }
            is JSONArray -> for (index in 0 until value.length()) result += findAllStrings(value.opt(index), key)
        }
        return result
    }
}

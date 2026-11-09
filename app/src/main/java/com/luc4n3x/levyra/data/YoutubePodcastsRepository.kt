package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.network.YoutubeRegionProfile
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

private const val PODCASTS_ENDPOINT = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
private const val PODCASTS_SEARCH_ENDPOINT = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
private const val PODCASTS_CLIENT_VERSION = "1.20251020.01.00"
private const val PODCASTS_CHIP_PARAMS =
    "ggNCSgQIDBADSgQIBxABSgQICRABSgQICBABSgQIDhABSgQIBBABSgQIDRABSgQIAxABSgQIChABSgQIBhABSgQIBRAB"
private const val PODCASTS_SEARCH_PARAMS = "EgWKAQJQAWoMEA4QChADEAQQCRAF"
private const val PODCAST_SHOW_PAGE_TYPE = "MUSIC_PAGE_TYPE_PODCAST_SHOW_DETAIL_PAGE"
internal const val YOUTUBE_PODCAST_SOURCE = "YouTube Music Podcasts"
private val PODCASTS_JSON = "application/json".toMediaType()
private val PODCAST_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
private val PODCAST_SHOW_ID = Regex("^MPSP[A-Za-z0-9_-]{8,64}$")

internal data class PodcastShelf(val title: String, val episodes: List<Track>)

internal data class PodcastShow(val browseId: String, val title: String, val author: String, val thumbnailUrl: String)

internal data class PodcastEpisodePage(val episodes: List<Track>, val continuation: String?)

internal data class PodcastShowPage(val show: PodcastShow, val description: String, val episodes: PodcastEpisodePage)

internal fun isPodcastShowId(browseId: String): Boolean = PODCAST_SHOW_ID.matches(browseId)

internal fun Track.isPodcastEpisode(): Boolean = source == YOUTUBE_PODCAST_SOURCE || "podcast" in moodTags

private val podcastShelvesCache = ConcurrentHashMap<String, List<PodcastShelf>>()

internal class YoutubePodcastsRepository(private val context: Context) {
    private val cache = podcastShelvesCache

    fun contentScope(languageCode: String): String = YoutubeRegionProfile.cacheScope(languageCode)

    suspend fun shelves(languageCode: String): List<PodcastShelf> = withContext(Dispatchers.IO) {
        val cacheKey = contentScope(languageCode)
        cache[cacheKey]?.let { return@withContext it }
        val root = post(PODCASTS_ENDPOINT, languageCode) {
            put("browseId", "FEmusic_home").put("params", PODCASTS_CHIP_PARAMS)
        }
        parsePodcastShelves(root).also { if (it.isNotEmpty()) cache[cacheKey] = it }
    }

    suspend fun search(query: String, languageCode: String): List<PodcastShow> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        parsePodcastSearch(post(PODCASTS_SEARCH_ENDPOINT, languageCode) { put("query", trimmed).put("params", PODCASTS_SEARCH_PARAMS) })
    }

    suspend fun show(browseId: String, languageCode: String): PodcastShowPage = withContext(Dispatchers.IO) {
        require(isPodcastShowId(browseId)) { "Invalid podcast id" }
        parsePodcastShow(browseId, post(PODCASTS_ENDPOINT, languageCode) { put("browseId", browseId) })
    }

    suspend fun moreEpisodes(continuation: String, languageCode: String): PodcastEpisodePage = withContext(Dispatchers.IO) {
        parsePodcastEpisodePage(post(PODCASTS_ENDPOINT, languageCode) { put("continuation", continuation) })
    }

    private suspend fun post(endpoint: String, languageCode: String, body: JSONObject.() -> JSONObject): JSONObject {
        val locale = YoutubeRegionProfile.effectiveLocale(languageCode)
        val payload = JSONObject()
            .put("context", JSONObject().put("client", JSONObject()
                .put("clientName", "WEB_REMIX")
                .put("clientVersion", PODCASTS_CLIENT_VERSION)
                .put("hl", locale.hl)
                .put("gl", locale.gl)))
            .body()
        return runInterruptible {
            val request = Request.Builder()
                .url(endpoint)
                .header("Origin", "https://music.youtube.com")
                .header("X-YouTube-Client-Name", "67")
                .header("X-YouTube-Client-Version", PODCASTS_CLIENT_VERSION)
                .post(payload.toString().toRequestBody(PODCASTS_JSON))
                .build()
            LevyraHttpClientFactory.general(context).newCall(request).execute().use { response ->
                val responseBody = response.body.string()
                check(response.isSuccessful && responseBody.isNotBlank()) { "Podcasts request failed: ${response.code}" }
                JSONObject(responseBody)
            }
        }
    }
}

internal fun parsePodcastShelves(root: JSONObject): List<PodcastShelf> {
    val shelves = mutableListOf<PodcastShelf>()
    collectPodcastShelves(root, shelves)
    return shelves.filter { it.episodes.isNotEmpty() }
}

internal fun parsePodcastSearch(root: JSONObject): List<PodcastShow> {
    val shows = LinkedHashMap<String, PodcastShow>()
    forEachRenderer(root, "musicResponsiveListItemRenderer") { item ->
        val browse = item.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint") ?: return@forEachRenderer
        val pageType = browse.optJSONObject("browseEndpointContextSupportedConfigs")
            ?.optJSONObject("browseEndpointContextMusicConfig")?.optString("pageType")
        val browseId = browse.optString("browseId")
        if (pageType != PODCAST_SHOW_PAGE_TYPE || !isPodcastShowId(browseId)) return@forEachRenderer
        val columns = item.optJSONArray("flexColumns") ?: JSONArray()
        val texts = (0 until columns.length()).map { index ->
            runsText(columns.optJSONObject(index)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text"))
        }
        val title = texts.firstOrNull().orEmpty().ifBlank { return@forEachRenderer }
        shows.putIfAbsent(browseId, PodcastShow(browseId, title, texts.getOrNull(1).orEmpty(), largestThumbnail(item.optJSONObject("thumbnail"))))
    }
    return shows.values.toList()
}

internal fun parsePodcastShow(browseId: String, root: JSONObject): PodcastShowPage {
    var header: JSONObject? = null
    forEachRenderer(root, "musicResponsiveHeaderRenderer") { if (header == null) header = it }
    val page = parsePodcastEpisodePage(root)
    val title = runsText(header?.optJSONObject("title")).ifBlank { page.episodes.firstOrNull()?.artist.orEmpty() }
    val show = PodcastShow(
        browseId = browseId,
        title = title,
        author = runsText(header?.optJSONObject("straplineTextOne")),
        thumbnailUrl = largestThumbnail(header?.optJSONObject("thumbnail")).ifBlank { page.episodes.firstOrNull()?.thumbnailUrl.orEmpty() }
    )
    val description = runsText(
        header?.optJSONObject("description")?.optJSONObject("musicDescriptionShelfRenderer")?.optJSONObject("description")
    )
    return PodcastShowPage(show, description, page.copy(episodes = page.episodes.map { it.copy(artist = title.ifBlank { it.artist }) }))
}

internal fun parsePodcastEpisodePage(root: JSONObject): PodcastEpisodePage {
    val episodes = LinkedHashMap<String, Track>()
    forEachRenderer(root, "musicMultiRowListItemRenderer") { item -> podcastEpisode(item)?.let { episodes.putIfAbsent(it.id, it) } }
    var continuation: String? = null
    forEachRenderer(root, "nextContinuationData") { data ->
        if (continuation == null) continuation = data.optString("continuation").takeIf(String::isNotBlank)
    }
    return PodcastEpisodePage(episodes.values.toList(), continuation)
}

private fun forEachRenderer(node: Any?, key: String, action: (JSONObject) -> Unit) {
    when (node) {
        is JSONObject -> node.keys().forEach { name ->
            val child = node.opt(name)
            if (name == key && child is JSONObject) action(child) else forEachRenderer(child, key, action)
        }
        is JSONArray -> for (index in 0 until node.length()) forEachRenderer(node.opt(index), key, action)
    }
}

private fun largestThumbnail(thumbnail: JSONObject?): String {
    val thumbnails = thumbnail?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        ?: return ""
    return thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url").orEmpty()
}

private fun collectPodcastShelves(node: Any?, into: MutableList<PodcastShelf>) {
    when (node) {
        is JSONObject -> {
            val shelf = node.optJSONObject("musicCarouselShelfRenderer")
            if (shelf != null) {
                val title = shelf.optJSONObject("header")?.let(::firstRunText).orEmpty()
                val contents = shelf.optJSONArray("contents") ?: JSONArray()
                val episodes = (0 until contents.length()).mapNotNull { index ->
                    contents.optJSONObject(index)?.optJSONObject("musicMultiRowListItemRenderer")?.let(::podcastEpisode)
                }
                into += PodcastShelf(title, episodes)
                return
            }
            node.keys().forEach { key -> collectPodcastShelves(node.opt(key), into) }
        }
        is JSONArray -> for (index in 0 until node.length()) collectPodcastShelves(node.opt(index), into)
    }
}

private fun firstRunText(node: Any?): String? = when (node) {
    is JSONObject -> node.optJSONArray("runs")?.optJSONObject(0)?.optString("text")?.takeIf(String::isNotBlank)
        ?: node.keys().asSequence().firstNotNullOfOrNull { firstRunText(node.opt(it)) }
    is JSONArray -> (0 until node.length()).firstNotNullOfOrNull { firstRunText(node.opt(it)) }
    else -> null
}

private fun runsText(node: JSONObject?): String =
    node?.optJSONArray("runs")?.let { runs -> (0 until runs.length()).joinToString("") { runs.optJSONObject(it)?.optString("text").orEmpty() } }.orEmpty().trim()

private fun podcastEpisode(item: JSONObject): Track? {
    val videoId = item.optJSONObject("onTap")?.optJSONObject("watchEndpoint")?.optString("videoId").orEmpty()
    if (!PODCAST_VIDEO_ID.matches(videoId)) return null
    val title = runsText(item.optJSONObject("title")).ifBlank { return null }
    val subtitle = runsText(item.optJSONObject("subtitle"))
    val show = runsText(item.optJSONObject("secondTitle"))
        .ifBlank { subtitle.split(" • ", " · ").lastOrNull { it.isNotBlank() }.orEmpty() }
    val artwork = largestThumbnail(item.optJSONObject("thumbnail"))
        .ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" }
    return Track(
        id = videoId,
        title = title,
        artist = show.ifBlank { subtitle },
        album = subtitle,
        durationMs = 0L,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=$videoId",
        thumbnailUrl = artwork,
        largeThumbnailUrl = artwork,
        source = YOUTUBE_PODCAST_SOURCE,
        moodTags = setOf("podcast"),
        energy = 40,
        vocal = 90,
        replayScore = 60,
        cacheScore = 50,
        accentStart = 0xFF1E3264.toInt(),
        accentEnd = 0xFF0D1730.toInt()
    )
}

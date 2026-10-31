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
private const val PODCASTS_CLIENT_VERSION = "1.20251020.01.00"
private const val PODCASTS_CHIP_PARAMS =
    "ggNCSgQIDBADSgQIBxABSgQICRABSgQICBABSgQIDhABSgQIBBABSgQIDRABSgQIAxABSgQIChABSgQIBhABSgQIBRAB"
internal const val YOUTUBE_PODCAST_SOURCE = "YouTube Music Podcasts"
private val PODCASTS_JSON = "application/json".toMediaType()
private val PODCAST_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

internal data class PodcastShelf(val title: String, val episodes: List<Track>)

private val podcastShelvesCache = ConcurrentHashMap<String, List<PodcastShelf>>()

internal class YoutubePodcastsRepository(private val context: Context) {
    private val cache = podcastShelvesCache

    suspend fun shelves(languageCode: String): List<PodcastShelf> = withContext(Dispatchers.IO) {
        val cacheKey = YoutubeRegionProfile.cacheScope(languageCode)
        cache[cacheKey]?.let { return@withContext it }
        val locale = YoutubeRegionProfile.effectiveLocale(languageCode)
        val payload = JSONObject()
            .put("context", JSONObject().put("client", JSONObject()
                .put("clientName", "WEB_REMIX")
                .put("clientVersion", PODCASTS_CLIENT_VERSION)
                .put("hl", locale.hl)
                .put("gl", locale.gl)))
            .put("browseId", "FEmusic_home")
            .put("params", PODCASTS_CHIP_PARAMS)
        val root = runInterruptible {
            val request = Request.Builder()
                .url(PODCASTS_ENDPOINT)
                .header("Origin", "https://music.youtube.com")
                .header("X-YouTube-Client-Name", "67")
                .header("X-YouTube-Client-Version", PODCASTS_CLIENT_VERSION)
                .post(payload.toString().toRequestBody(PODCASTS_JSON))
                .build()
            LevyraHttpClientFactory.general(context).newCall(request).execute().use { response ->
                val body = response.body.string()
                check(response.isSuccessful && body.isNotBlank()) { "Podcasts request failed: ${response.code}" }
                JSONObject(body)
            }
        }
        parsePodcastShelves(root).also { if (it.isNotEmpty()) cache[cacheKey] = it }
    }
}

internal fun parsePodcastShelves(root: JSONObject): List<PodcastShelf> {
    val shelves = mutableListOf<PodcastShelf>()
    collectPodcastShelves(root, shelves)
    return shelves.filter { it.episodes.isNotEmpty() }
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
    val show = subtitle.split(" • ", " · ").lastOrNull { it.isNotBlank() }.orEmpty()
    val thumbnails = item.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")
        ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
    val artwork = thumbnails?.optJSONObject(thumbnails.length() - 1)?.optString("url").orEmpty()
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

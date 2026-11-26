package com.luc4n3x.levyra.desktop.core.podcast

import com.luc4n3x.levyra.desktop.core.model.Track
import org.json.JSONArray
import org.json.JSONObject

private const val PODCAST_SHOW_PAGE_TYPE = "MUSIC_PAGE_TYPE_PODCAST_SHOW_DETAIL_PAGE"
private const val MAX_SHELVES = 24
private const val MAX_ITEMS = 200
private const val MAX_TEXT_LENGTH = 4_000
private const val MAX_DEPTH = 64
private val PODCAST_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
private val CONTINUATION_TOKEN = Regex("^[A-Za-z0-9_%=-]{8,4096}$")

object PodcastParser {

    fun shelves(root: JSONObject): List<PodcastShelf> {
        val shelves = mutableListOf<PodcastShelf>()
        collectShelves(root, shelves, 0)
        return shelves.filter { it.episodes.isNotEmpty() }.take(MAX_SHELVES)
    }

    fun search(root: JSONObject): List<PodcastShow> {
        val shows = LinkedHashMap<String, PodcastShow>()
        forEachRenderer(root, "musicResponsiveListItemRenderer") { item ->
            if (shows.size >= MAX_ITEMS) return@forEachRenderer
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

    fun show(browseId: String, root: JSONObject): PodcastShowPage {
        var header: JSONObject? = null
        forEachRenderer(root, "musicResponsiveHeaderRenderer") { if (header == null) header = it }
        val page = episodePage(root)
        val title = runsText(header?.optJSONObject("title")).ifBlank { page.episodes.firstOrNull()?.artist.orEmpty() }
        val show = PodcastShow(
            browseId = browseId,
            title = title,
            author = runsText(header?.optJSONObject("straplineTextOne")),
            thumbnailUrl = largestThumbnail(header?.optJSONObject("thumbnail")).ifBlank { page.episodes.firstOrNull()?.artworkUrl.orEmpty() }
        )
        val description = runsText(
            header?.optJSONObject("description")?.optJSONObject("musicDescriptionShelfRenderer")?.optJSONObject("description")
        )
        return PodcastShowPage(show, description, page.copy(episodes = page.episodes.map { it.copy(artist = title.ifBlank { it.artist }) }))
    }

    fun episodePage(root: JSONObject): PodcastEpisodePage {
        val episodes = LinkedHashMap<String, Track>()
        forEachRenderer(root, "musicMultiRowListItemRenderer") { item ->
            if (episodes.size < MAX_ITEMS) episode(item)?.let { episodes.putIfAbsent(it.id, it) }
        }
        var continuation: String? = null
        forEachRenderer(root, "nextContinuationData") { data ->
            if (continuation == null) continuation = data.optString("continuation").takeIf(::isContinuationToken)
        }
        return PodcastEpisodePage(episodes.values.toList(), continuation)
    }

    fun isContinuationToken(value: String): Boolean = CONTINUATION_TOKEN.matches(value)

    private fun forEachRenderer(node: Any?, key: String, depth: Int = 0, action: (JSONObject) -> Unit) {
        if (depth > MAX_DEPTH) return
        when (node) {
            is JSONObject -> node.keys().forEach { name ->
                val child = node.opt(name)
                if (name == key && child is JSONObject) action(child) else forEachRenderer(child, key, depth + 1, action)
            }
            is JSONArray -> for (index in 0 until node.length()) forEachRenderer(node.opt(index), key, depth + 1, action)
        }
    }

    private fun collectShelves(node: Any?, into: MutableList<PodcastShelf>, depth: Int) {
        if (depth > MAX_DEPTH || into.size >= MAX_SHELVES * 2) return
        when (node) {
            is JSONObject -> {
                val shelf = node.optJSONObject("musicCarouselShelfRenderer")
                if (shelf != null) {
                    val title = shelf.optJSONObject("header")?.let { firstRunText(it, 0) }.orEmpty().take(MAX_TEXT_LENGTH)
                    val contents = shelf.optJSONArray("contents") ?: JSONArray()
                    val episodes = (0 until minOf(contents.length(), MAX_ITEMS)).mapNotNull { index ->
                        contents.optJSONObject(index)?.optJSONObject("musicMultiRowListItemRenderer")?.let(::episode)
                    }.distinctBy(Track::id)
                    into += PodcastShelf(title, episodes)
                    return
                }
                node.keys().forEach { key -> collectShelves(node.opt(key), into, depth + 1) }
            }
            is JSONArray -> for (index in 0 until node.length()) collectShelves(node.opt(index), into, depth + 1)
        }
    }

    private fun firstRunText(node: Any?, depth: Int): String? = when {
        depth > MAX_DEPTH -> null
        node is JSONObject -> node.optJSONArray("runs")?.optJSONObject(0)?.optString("text")?.takeIf(String::isNotBlank)
            ?: node.keys().asSequence().firstNotNullOfOrNull { firstRunText(node.opt(it), depth + 1) }
        node is JSONArray -> (0 until node.length()).firstNotNullOfOrNull { firstRunText(node.opt(it), depth + 1) }
        else -> null
    }

    private fun runsText(node: JSONObject?): String =
        node?.optJSONArray("runs")?.let { runs ->
            (0 until runs.length()).joinToString("") { runs.optJSONObject(it)?.optString("text").orEmpty() }
        }.orEmpty().trim().take(MAX_TEXT_LENGTH)

    private fun largestThumbnail(thumbnail: JSONObject?): String {
        val thumbnails = thumbnail?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            ?: return ""
        return thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url").orEmpty().takeIf { it.startsWith("https://") }.orEmpty()
    }

    private fun episode(item: JSONObject): Track? {
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
            videoUrl = Track.watchUrlOf(videoId),
            artworkUrl = artwork
        )
    }
}

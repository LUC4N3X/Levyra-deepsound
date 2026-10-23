package com.luc4n3x.levyra.data.apple

import org.json.JSONArray
import org.json.JSONObject

data class AppleArtistMotion(val videoUrl: String, val previewTemplate: String, val width: Int, val height: Int)

data class AppleArtistLatestRelease(val item: AppleCatalogItem, val headline: String, val detail: String)

enum class AppleArtistShelfStyle { Feature, Square, Video, Artists }

data class AppleArtistShelf(
    val id: String,
    val title: String,
    val style: AppleArtistShelfStyle,
    val items: List<AppleCatalogItem>
)

data class AppleArtistInfo(
    val bio: String,
    val origin: String,
    val bornOrFormed: String,
    val isGroup: Boolean,
    val genres: List<String>
)

data class AppleArtistPage(
    val id: String,
    val name: String,
    val artworkTemplate: String,
    val squareMotion: AppleArtistMotion?,
    val wideMotion: AppleArtistMotion?,
    val latestRelease: AppleArtistLatestRelease?,
    val topSongs: List<AppleCatalogItem>,
    val shelves: List<AppleArtistShelf>,
    val info: AppleArtistInfo?,
    val fetchedAtMs: Long
)

internal const val APPLE_TOP_VIDEOS_SHELF_ID = "apple-top-videos"

internal fun appleArtistShelvesWithTopVideos(shelves: List<AppleArtistShelf>, videos: List<AppleCatalogItem>): List<AppleArtistShelf> {
    if (videos.isEmpty()) return shelves
    val topVideos = AppleArtistShelf(APPLE_TOP_VIDEOS_SHELF_ID, "", AppleArtistShelfStyle.Video, videos)
    val webVideos = shelves.indexOfFirst { shelf -> shelf.style == AppleArtistShelfStyle.Video && shelf.items.any { it.kind == AppleCatalogItemKind.Video } }
    if (webVideos >= 0) return shelves.toMutableList().apply { set(webVideos, topVideos) }
    val similar = shelves.indexOfFirst { it.style == AppleArtistShelfStyle.Artists }
    return shelves.toMutableList().apply { add(if (similar >= 0) similar else size, topVideos) }
}

internal fun selectAppleArtistCandidate(requested: String, candidates: List<Pair<String, String>>): String? {
    val key = appleArtistNameKey(requested)
    if (key.isBlank()) return null
    return candidates.firstOrNull { (_, name) -> appleArtistNameKey(name) == key }?.first
}

private val appleArtistMarks = Regex("\\p{M}+")
private val appleArtistNonWord = Regex("[^\\p{L}\\p{N}]+")

private fun appleArtistNameKey(value: String): String =
    java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        .replace(appleArtistMarks, "")
        .lowercase(java.util.Locale.ROOT)
        .replace(appleArtistNonWord, " ")
        .trim()

internal object AppleArtistPageParser {
    private val htmlTag = Regex("<[^>]+>")
    private const val MAX_SHELF_ITEMS = 24
    private const val SQUARE_MOTION_KEY = "motionArtistSquare1x1"
    private const val WIDE_MOTION_KEY = "motionArtistWide16x9"

    fun parse(html: String, fetchedAtMs: Long): AppleArtistPage? {
        val sections = AppleCatalogPageParser.sections(html) ?: return null
        var header: JSONObject? = null
        var featured: JSONObject? = null
        var songs: List<AppleCatalogItem> = emptyList()
        val shelves = ArrayList<AppleArtistShelf>()
        for (index in 0 until sections.length()) {
            val section = sections.optJSONObject(index) ?: continue
            val first = section.optJSONArray("items")?.optJSONObject(0)
            when (section.optString("itemKind")) {
                "artistDetailHeader" -> header = first
                "artistFeaturedContentAndTracks" -> featured = first
                "trackLockup" -> if (songs.isEmpty()) songs = section.optJSONArray("items").items()
                else -> parseShelf(section)?.let(shelves::add)
            }
        }
        val head = header ?: return null
        val name = head.optString("title").trim().ifBlank { return null }
        return AppleArtistPage(
            id = head.optJSONObject("contentDescriptor")?.optJSONObject("identifiers")?.optString("storeAdamID").orEmpty().ifBlank { head.optString("id") },
            name = name,
            artworkTemplate = artworkTemplate(head.optJSONObject("artwork")),
            squareMotion = motion(head.optJSONObject("videoArtwork"), SQUARE_MOTION_KEY),
            wideMotion = motion(head.optJSONObject("wideVideoArtwork"), WIDE_MOTION_KEY),
            latestRelease = featured?.optJSONObject("featuredContent")?.let(::latestRelease),
            topSongs = featured?.optJSONArray("tracks").items().ifEmpty { songs }.filter { it.kind == AppleCatalogItemKind.Song },
            shelves = shelves,
            info = null,
            fetchedAtMs = fetchedAtMs
        )
    }

    fun info(attributes: JSONObject): AppleArtistInfo = AppleArtistInfo(
        bio = attributes.optString("artistBio").trim(),
        origin = attributes.optString("origin").trim(),
        bornOrFormed = attributes.optString("bornOrFormed").trim(),
        isGroup = attributes.optBoolean("isGroup", false),
        genres = attributes.optJSONArray("genreNames")?.let { array ->
            (0 until array.length()).mapNotNull { array.optString(it).trim().takeIf(String::isNotBlank) }
        }.orEmpty()
    )

    fun styledText(value: String): List<Pair<String, Boolean>> {
        val runs = ArrayList<Pair<String, Boolean>>()
        var italic = false
        var cursor = 0
        htmlTag.findAll(value).forEach { tag ->
            if (tag.range.first > cursor) runs += decode(value.substring(cursor, tag.range.first)) to italic
            when (tag.value.lowercase(java.util.Locale.ROOT).replace(" ", "")) {
                "<i>", "<em>" -> italic = true
                "</i>", "</em>" -> italic = false
                "<br>", "<br/>" -> runs += "\n" to italic
            }
            cursor = tag.range.last + 1
        }
        if (cursor < value.length) runs += decode(value.substring(cursor)) to italic
        return runs.filter { it.first.isNotEmpty() }
    }

    private fun decode(value: String): String =
        value.replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")

    private fun parseShelf(section: JSONObject): AppleArtistShelf? {
        val style = when (section.optString("itemKind")) {
            "adaptiveHorizontalSquareLockup" -> AppleArtistShelfStyle.Feature
            "squareLockup" -> AppleArtistShelfStyle.Square
            "verticalVideoLockup" -> AppleArtistShelfStyle.Video
            "bubbleLockup" -> AppleArtistShelfStyle.Artists
            else -> return null
        }
        val raw = section.optJSONArray("items") ?: return null
        val nodes = (0 until minOf(raw.length(), MAX_SHELF_ITEMS)).mapNotNull { index ->
            raw.optJSONObject(index)?.let { item -> item.optJSONObject("horizontalLockup") ?: item }
        }
        val items = nodes.mapNotNull(AppleCatalogPageParser::parseItem)
            .filter { it.kind != AppleCatalogItemKind.Other && it.kind != AppleCatalogItemKind.Song }
            .distinctBy { "${it.kind}:${it.id}" }
        if (items.isEmpty()) return null
        return AppleArtistShelf(section.optString("id"), AppleCatalogPageParser.sectionTitle(section), style, items)
    }

    private fun latestRelease(content: JSONObject): AppleArtistLatestRelease? {
        val detail = content.optJSONObject("detailText") ?: return null
        val title = detail.optString("title").trim().ifBlank { return null }
        val descriptor = content.optJSONObject("contentDescriptor")
        val item = AppleCatalogItem(
            id = descriptor?.optJSONObject("identifiers")?.optString("storeAdamID").orEmpty().ifBlank { content.optString("id") },
            kind = if (descriptor?.optString("kind") == "album") AppleCatalogItemKind.Album else AppleCatalogItemKind.Other,
            title = title,
            subtitle = "",
            heading = content.optString("heading").trim(),
            artworkTemplate = artworkTemplate(content.optJSONObject("artwork")),
            url = descriptor?.optString("url").orEmpty().takeIf { it.startsWith("https://music.apple.com/") }.orEmpty(),
            explicit = detail.optBoolean("showExplicitBadge", false)
        )
        if (item.kind != AppleCatalogItemKind.Album) return null
        return AppleArtistLatestRelease(item, detail.optString("headline").trim(), detail.optString("subtitle").trim())
    }

    fun topVideos(data: JSONArray): List<AppleCatalogItem> =
        (0 until minOf(data.length(), MAX_SHELF_ITEMS)).mapNotNull { index ->
            val node = data.optJSONObject(index) ?: return@mapNotNull null
            val attributes = node.optJSONObject("attributes") ?: return@mapNotNull null
            val title = attributes.optString("name").trim().ifBlank { return@mapNotNull null }
            AppleCatalogItem(
                id = node.optString("id").trim().ifBlank { return@mapNotNull null },
                kind = AppleCatalogItemKind.Video,
                title = title,
                subtitle = attributes.optString("artistName").trim(),
                heading = attributes.optString("releaseDate").trim().take(4).takeIf { it.length == 4 && it.all(Char::isDigit) }.orEmpty(),
                description = attributes.optJSONObject("editorialNotes")?.optString("short").orEmpty().trim(),
                artworkTemplate = attributes.optJSONObject("artwork")?.optString("url").orEmpty().takeIf { it.startsWith("https://") }.orEmpty(),
                url = attributes.optString("url").takeIf { it.startsWith("https://music.apple.com/") }.orEmpty(),
                explicit = attributes.optString("contentRating") == "explicit",
                durationMs = attributes.optLong("durationInMillis", 0L)
            )
        }.distinctBy { it.id }

    fun header(id: String, attributes: JSONObject, fetchedAtMs: Long): AppleArtistPage? {
        val name = attributes.optString("name").trim().ifBlank { return null }
        val video = attributes.optJSONObject("editorialVideo")
        return AppleArtistPage(
            id = id,
            name = name,
            artworkTemplate = attributes.optJSONObject("artwork")?.optString("url").orEmpty().takeIf { it.startsWith("https://") }.orEmpty(),
            squareMotion = motionVariant(video?.optJSONObject(SQUARE_MOTION_KEY)),
            wideMotion = motionVariant(video?.optJSONObject(WIDE_MOTION_KEY)),
            latestRelease = null,
            topSongs = emptyList(),
            shelves = emptyList(),
            info = info(attributes),
            fetchedAtMs = fetchedAtMs
        )
    }

    private fun motion(container: JSONObject?, key: String): AppleArtistMotion? =
        motionVariant(container?.optJSONObject("dictionary")?.optJSONObject(key))

    private fun motionVariant(variant: JSONObject?): AppleArtistMotion? {
        variant ?: return null
        val video = variant.optString("video").trim().takeIf { it.startsWith("https://") } ?: return null
        val preview = variant.optJSONObject("previewFrame")
        return AppleArtistMotion(
            videoUrl = video,
            previewTemplate = preview?.optString("url").orEmpty().takeIf { it.startsWith("https://") }.orEmpty(),
            width = preview?.optInt("width", 0) ?: 0,
            height = preview?.optInt("height", 0) ?: 0
        )
    }

    private fun artworkTemplate(artwork: JSONObject?): String =
        artwork?.optJSONObject("dictionary")?.optString("url").orEmpty().takeIf { it.startsWith("https://") }.orEmpty()

    private fun JSONArray?.items(): List<AppleCatalogItem> {
        if (this == null) return emptyList()
        return (0 until minOf(length(), MAX_SHELF_ITEMS)).mapNotNull { optJSONObject(it)?.let(AppleCatalogPageParser::parseItem) }
    }
}

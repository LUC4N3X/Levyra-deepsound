package com.luc4n3x.levyra.data.apple

import org.json.JSONArray
import org.json.JSONObject

enum class AppleCatalogItemKind { Album, Playlist, Song, Artist, Video, Other }

enum class AppleCatalogShelfKind { Header, Hero, Songs, Grid, Videos, Other }

data class AppleCatalogItem(
    val id: String,
    val kind: AppleCatalogItemKind,
    val title: String,
    val subtitle: String,
    val heading: String = "",
    val description: String = "",
    val artworkTemplate: String = "",
    val coverArtworkTemplate: String = "",
    val backgroundColor: Int? = null,
    val url: String = "",
    val explicit: Boolean = false,
    val durationMs: Long = 0L,
    val album: String = ""
) {
    fun artworkUrl(size: Int): String = AppleCatalogArtwork.url(artworkTemplate, size, size)

    fun coverUrl(size: Int): String = AppleCatalogArtwork.url(coverArtworkTemplate.ifBlank { artworkTemplate }, size, size)

    fun heroUrl(width: Int): String = AppleCatalogArtwork.url(artworkTemplate, width, (width / 1.5f).toInt())
}

data class AppleCatalogShelf(
    val id: String,
    val title: String,
    val kind: AppleCatalogShelfKind,
    val items: List<AppleCatalogItem>,
    val rows: Int = 1
)

data class AppleCatalogPage(
    val storefront: String,
    val shelves: List<AppleCatalogShelf>,
    val fetchedAtMs: Long
) {
    fun shelf(predicate: (AppleCatalogShelf) -> Boolean): AppleCatalogShelf? = shelves.firstOrNull(predicate)
}

internal object AppleCatalogArtwork {
    fun url(template: String, width: Int, height: Int): String {
        if (template.isBlank()) return ""
        if (!template.contains("{w}")) return template
        return template
            .replace("{w}", width.toString())
            .replace("{h}", height.toString())
            .replace("{c}", "cc")
            .replace("{f}", "jpg")
    }
}

object AppleCatalogPageParser {
    private val serverData = Regex("<script[^>]*id=\"serialized-server-data\"[^>]*>(.*?)</script>", RegexOption.DOT_MATCHES_ALL)
    private const val MAX_SHELVES = 40
    private const val MAX_ITEMS = 100

    fun parse(html: String, storefront: String, fetchedAtMs: Long): AppleCatalogPage? {
        val sections = sections(html) ?: return null
        val shelves = ArrayList<AppleCatalogShelf>()
        for (index in 0 until minOf(sections.length(), MAX_SHELVES)) {
            val section = sections.optJSONObject(index) ?: continue
            parseShelf(section)?.let(shelves::add)
        }
        if (shelves.isEmpty()) return null
        return AppleCatalogPage(storefront, shelves, fetchedAtMs)
    }

    internal fun sections(html: String): JSONArray? {
        val payload = serverData.find(html)?.groupValues?.get(1)?.let(::decodeEntities) ?: return null
        val root = runCatching { JSONObject(payload) }.getOrNull() ?: return null
        return pageNode(root)?.optJSONArray("sections")
    }

    private fun pageNode(root: JSONObject): JSONObject? {
        var node: Any? = root
        repeat(6) {
            when (val current = node) {
                is JSONObject -> {
                    if (current.has("sections")) return current
                    node = current.opt("data")
                }
                is JSONArray -> node = current.opt(0)
                else -> return null
            }
        }
        return null
    }

    private fun parseShelf(section: JSONObject): AppleCatalogShelf? {
        val itemKind = section.optString("itemKind")
        val kind = when (itemKind) {
            "containerDetailHeaderLockup", "headerComponentModel" -> AppleCatalogShelfKind.Header
            "flowcaseLockup" -> AppleCatalogShelfKind.Hero
            "trackLockup" -> AppleCatalogShelfKind.Songs
            "squareLockup", "bubbleLockup", "mixedContentTypeLockup" -> AppleCatalogShelfKind.Grid
            "verticalVideoLockup", "horizontalLockup" -> AppleCatalogShelfKind.Videos
            else -> return null
        }
        val title = sectionTitle(section)
        val items = section.optJSONArray("items") ?: return null
        val parsed = ArrayList<AppleCatalogItem>()
        for (index in 0 until minOf(items.length(), MAX_ITEMS)) {
            val item = items.optJSONObject(index) ?: continue
            parseItem(item)?.let(parsed::add)
        }
        val distinct = parsed.distinctBy { "${it.kind}:${it.id}" }
        if (distinct.isEmpty()) return null
        val rows = section.optJSONObject("presentation")?.optJSONObject("layout")?.optInt("numberOfRows", 1) ?: 1
        return AppleCatalogShelf(section.optString("id"), title, kind, distinct, rows.coerceIn(1, 4))
    }

    internal fun sectionTitle(section: JSONObject): String =
        section.optJSONObject("header")?.optJSONObject("item")?.let { header ->
            header.optJSONObject("titleLink")?.optString("title").orEmpty().ifBlank { header.optString("title") }
        }.orEmpty().trim()

    internal fun parseItem(item: JSONObject): AppleCatalogItem? {
        val title = item.optJSONArray("titleLinks")?.firstTitle().orEmpty().ifBlank { item.optString("title") }.trim()
        if (title.isBlank()) return null
        val descriptor = descriptorOf(item)
        val kind = kindOf(descriptor?.optString("kind"))
        val id = descriptor?.optJSONObject("identifiers")?.optString("storeAdamID").orEmpty()
            .ifBlank { item.optString("id") }
        val artwork = item.optJSONObject("artwork")?.optJSONObject("dictionary")
        val cover = item.optJSONObject("coverArtwork")?.optJSONObject("dictionary")
        return AppleCatalogItem(
            id = id,
            kind = kind,
            title = title,
            subtitle = subtitleOf(item),
            heading = item.optString("heading").trim(),
            description = item.optString("description").trim(),
            artworkTemplate = httpsUrl(artwork),
            coverArtworkTemplate = httpsUrl(cover),
            backgroundColor = (cover ?: artwork)?.optString("bgColor")?.let(::parseColor),
            url = descriptor?.optString("url").orEmpty().takeIf { it.startsWith("https://music.apple.com/") }.orEmpty(),
            explicit = item.optBoolean("showExplicitBadge", false),
            durationMs = item.optLong("duration", 0L).coerceAtLeast(0L),
            album = item.optJSONArray("tertiaryLinks")?.firstTitle().orEmpty().trim()
        )
    }

    private fun kindOf(value: String?): AppleCatalogItemKind = when (value) {
        "album" -> AppleCatalogItemKind.Album
        "playlist" -> AppleCatalogItemKind.Playlist
        "song" -> AppleCatalogItemKind.Song
        "artist" -> AppleCatalogItemKind.Artist
        "musicVideo", "uploadedVideo" -> AppleCatalogItemKind.Video
        else -> AppleCatalogItemKind.Other
    }

    private fun subtitleOf(item: JSONObject): String =
        item.optJSONArray("subtitleLinks")?.joinedTitles().orEmpty().ifBlank { item.optString("subtitle") }.trim()

    private fun httpsUrl(dictionary: JSONObject?): String =
        dictionary?.optString("url").orEmpty().takeIf { it.startsWith("https://") }.orEmpty()

    private fun descriptorOf(item: JSONObject): JSONObject? =
        item.optJSONObject("contentDescriptor")
            ?: item.optJSONObject("segue")?.optJSONObject("destination")?.optJSONObject("contentDescriptor")
            ?: item.optJSONArray("items")?.optJSONObject(0)?.optJSONObject("contentDescriptor")

    private fun JSONArray.firstTitle(): String? = optJSONObject(0)?.optString("title")

    private fun JSONArray.joinedTitles(): String = buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.optString("title")?.trim()?.takeIf(String::isNotBlank)?.let(::add)
        }
    }.joinToString(", ")

    private fun parseColor(value: String): Int? {
        val hex = value.trim().removePrefix("#")
        if (hex.length != 6) return null
        return hex.toLongOrNull(16)?.let { (0xFF000000 or it).toInt() }
    }

    private fun decodeEntities(value: String): String =
        if (value.trimStart().startsWith("{") || value.trimStart().startsWith("[")) value
        else value.replace("&quot;", "\"").replace("&#34;", "\"").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&#39;", "'").replace("&apos;", "'").replace("&amp;", "&")
}

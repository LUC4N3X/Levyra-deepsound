package com.luc4n3x.levyra.data.apple

import com.luc4n3x.levyra.domain.AlbumHit
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

    fun toAlbumHit(): AlbumHit = AlbumHit(
        title = title, artist = subtitle, year = "", thumbnailUrl = coverUrl(600),
        query = "$title $subtitle".trim(), explicit = explicit,
        canonicalUrl = url, metadataProvider = AppleCatalogTracks.SOURCE
    )
}

internal fun appleNewReleaseAlbums(page: AppleCatalogPage?, limit: Int): List<AlbumHit> {
    val shelves = page?.shelves.orEmpty()
    val newSongsIndex = shelves.indexOfFirst { shelf -> shelf.kind == AppleCatalogShelfKind.Songs }
    if (newSongsIndex < 0) return emptyList()
    return shelves.drop(newSongsIndex + 1)
        .takeWhile { shelf ->
            shelf.kind == AppleCatalogShelfKind.Grid && shelf.items.isNotEmpty() &&
                shelf.items.all { item -> item.kind == AppleCatalogItemKind.Album }
        }
        .take(MAX_NEW_RELEASE_SHELVES)
        .asSequence()
        .flatMap { shelf -> shelf.items.asSequence() }
        .filter { item ->
            item.kind == AppleCatalogItemKind.Album && item.title.isNotBlank() && item.subtitle.isNotBlank() &&
                AppleCatalogIds.albumFromUrl(item.url) != null
        }
        .distinctBy { item -> AppleCatalogIds.albumFromUrl(item.url)?.second }
        .map(AppleCatalogItem::toAlbumHit)
        .take(limit.coerceIn(1, 80))
        .toList()
}

internal fun appleChartReleaseAlbums(root: JSONObject?, nowMs: Long, limit: Int): List<AlbumHit> {
    val charts = root?.optJSONObject("results")?.optJSONArray("albums") ?: return emptyList()
    val today = java.time.LocalDate.ofEpochDay(Math.floorDiv(nowMs, DAY_MS))
    val earliest = today.minusDays(MAX_CHART_RELEASE_AGE_DAYS).toString()
    val latest = today.toString()
    val albums = ArrayList<AlbumHit>()
    for (chartIndex in 0 until charts.length()) {
        val data = charts.optJSONObject(chartIndex)?.optJSONArray("data") ?: continue
        for (index in 0 until data.length()) {
            val attributes = data.optJSONObject(index)?.optJSONObject("attributes") ?: continue
            val releaseDate = attributes.optString("releaseDate").trim().take(10)
            if (releaseDate.length != 10 || releaseDate < earliest || releaseDate > latest) continue
            val title = attributes.optString("name").trim()
            val artist = attributes.optString("artistName").trim()
            val url = attributes.optString("url").trim()
            if (title.isBlank() || artist.isBlank() || AppleCatalogIds.albumFromUrl(url) == null) continue
            albums += AlbumHit(
                title = title, artist = artist, year = releaseDate.take(4),
                thumbnailUrl = AppleCatalogArtwork.url(attributes.optJSONObject("artwork")?.optString("url").orEmpty(), 600, 600),
                query = "$title $artist", explicit = attributes.optString("contentRating") == "explicit",
                releaseDate = releaseDate, canonicalUrl = url, metadataProvider = AppleCatalogTracks.SOURCE
            )
        }
    }
    return albums.distinctBy { AppleCatalogIds.albumFromUrl(it.canonicalUrl)?.second }.take(limit.coerceIn(1, 80))
}

private const val MAX_NEW_RELEASE_SHELVES = 2
private const val MAX_CHART_RELEASE_AGE_DAYS = 60L
private const val DAY_MS = 24L * 60L * 60L * 1000L

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

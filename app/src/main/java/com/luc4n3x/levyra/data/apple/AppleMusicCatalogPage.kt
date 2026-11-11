package com.luc4n3x.levyra.data.apple

import com.luc4n3x.levyra.domain.AlbumHit
import org.json.JSONArray
import org.json.JSONObject

enum class AppleCatalogItemKind { Album, Playlist, Song, Artist, Video, Other }

enum class AppleCatalogShelfKind { Header, Hero, Songs, Grid, Videos, Footer, Other }

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

    fun inheritAlbumHeader(header: AppleCatalogItem?): AppleCatalogItem {
        header ?: return this
        return copy(
            subtitle = subtitle.ifBlank { header.subtitle },
            artworkTemplate = artworkTemplate.ifBlank { header.artworkTemplate },
            album = album.ifBlank { header.title }
        )
    }

    fun coverUrl(size: Int): String = AppleCatalogArtwork.url(coverArtworkTemplate.ifBlank { artworkTemplate }, size, size)

    fun heroUrl(width: Int): String = AppleCatalogArtwork.url(artworkTemplate, width, (width / 1.5f).toInt())

    fun toAlbumHit(): AlbumHit = AlbumHit(
        title = title, artist = subtitle, year = "", thumbnailUrl = coverUrl(600),
        query = "$title $subtitle".trim(), explicit = explicit,
        canonicalUrl = url, metadataProvider = AppleCatalogTracks.SOURCE
    )
}

internal fun appleNewThisWeekShelf(page: AppleCatalogPage?): AppleCatalogShelf? {
    val shelves = page?.shelves.orEmpty()
    val newSongsIndex = shelves.indexOfFirst { shelf -> shelf.kind == AppleCatalogShelfKind.Songs }
    if (newSongsIndex < 0) return null
    return shelves.getOrNull(newSongsIndex + 1)?.takeIf { shelf ->
        shelf.kind == AppleCatalogShelfKind.Grid && shelf.items.isNotEmpty() && shelf.items.all { item -> item.kind == AppleCatalogItemKind.Album }
    }
}

internal fun appleAlbumHits(items: Sequence<AppleCatalogItem>, limit: Int): List<AlbumHit> = items
    .filter { item ->
        item.kind == AppleCatalogItemKind.Album && item.title.isNotBlank() && item.subtitle.isNotBlank() &&
            AppleCatalogIds.albumFromUrl(item.url) != null
    }
    .distinctBy { item -> AppleCatalogIds.albumFromUrl(item.url)?.second }
    .map(AppleCatalogItem::toAlbumHit)
    .take(limit.coerceIn(1, 100))
    .toList()

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

data class AppleDailyChart(val playlistId: String, val label: String, val global: Boolean)

internal object AppleDailyTop100 {
    const val GLOBAL_PLAYLIST_ID = "pl.d25f5d1181894928af76c85c967f8f31"
    private val leadingMarkets = listOf("us", "gb")
    private val countryCharts = mapOf(
        "it" to "pl.737e067787df485a8062e2c4927d94db",
        "us" to "pl.606afcbb70264d2eb2b51d8dbcfa6a12",
        "gb" to "pl.c2273b7e89b44121b3093f67228918e7",
        "es" to "pl.0d656d7feae64198bc5fb1b02786ed75",
        "fr" to "pl.6e8cfd81d51042648fa36c9df5236b8d",
        "de" to "pl.c10a2c113db14685a0b09fa5834d8e8b",
        "pt" to "pl.5437c1490ac74e9e9505fc7d1f201655",
        "br" to "pl.11ac7cc7d09741c5822e8c66e5c7edbb",
        "mx" to "pl.df3f10ca27b1479087de2cd3f9f6716b",
        "nl" to "pl.26fb1998d54a4b3192be548529a97f8e",
        "pl" to "pl.8c91cbb0ef4e48308dbbba4238135eaf",
        "ro" to "pl.0c6bea611ad54c79b854299bc515a5a6",
        "gr" to "pl.0f15f3a8ba014979b9fdd7a0ef906dca",
        "se" to "pl.5876877c387b4ffb8860ac3ea2c244c3",
        "dk" to "pl.d08496850bc840a4874e877177a69f9f",
        "cz" to "pl.e447d9ba54254130a76143bf6fdfa65c",
        "ua" to "pl.815f78effb3844909a8259d759ecbddb",
        "ru" to "pl.728bd30a9247487c80a483f4168a9dcd",
        "tr" to "pl.f3e0d6ef238542609572c18b0de1513b",
        "sa" to "pl.a5365fa3b6ec4a34994339ca100801ae",
        "cn" to "pl.fde851dc95ce4ffbb74028dfd254ced5",
        "jp" to "pl.043a2c9876114d95a4659988497567be",
        "kr" to "pl.d3d10c32fbc540b38e266367dc8cb00c",
        "in" to "pl.c0e98d2423e54c39b3df955c24df3cc5",
        "id" to "pl.2b7e089dc9ef4dd7a18429df9c6e26a3",
        "vn" to "pl.550110ec6feb4ae0aff364bcde6d1372",
        "th" to "pl.c509137d97214632a087129ece060a3d",
        "ph" to "pl.b9eb00f9d195440e8b0bdf19b8db7f34",
        "tw" to "pl.741ff34016704547853b953ec5181d83",
        "il" to "pl.0c9765e5330048af96c2336fa7bc3525",
        "hu" to "pl.cee165c3a51e466481bde5de75d6dee3",
        "bg" to "pl.040cf0b4c7e9467eb9eed2d33e7a29d6",
        "fi" to "pl.acea41a017664a8ebcd5aa1622aecc88",
        "no" to "pl.05a67957c3974729aac67c01247e55b6",
        "sk" to "pl.2e50996a5bf44ab78cbb5c34b1992701",
        "my" to "pl.a165defeeccb4b17a59bb5c85637b9b7",
        "ar" to "pl.7ae8594e422f44658e58212d876d9323",
        "ca" to "pl.79bac9045a2540e0b195e983df8ba569",
        "au" to "pl.18be1cf04dfd4ffb9b6b0453e8fae8f1"
    )
    private val knownCharts = countryCharts.values.toSet()
    private val titlePrefix = Regex("""^\s*(?:le\s+)?(?:top|топ|トップ)\s*-?\s*100\s*[:：\-–]?\s*""", RegexOption.IGNORE_CASE)

    fun charts(page: AppleCatalogPage?, storefront: String): List<AppleDailyChart> {
        val all = page?.shelves.orEmpty().asSequence()
            .flatMap { shelf -> shelf.items.asSequence() }
            .filter { item -> item.kind == AppleCatalogItemKind.Playlist }
            .mapNotNull { item ->
                val id = AppleCatalogIds.playlist(item.id) ?: AppleCatalogIds.playlistIdFromUrl(item.url) ?: return@mapNotNull null
                val known = id == GLOBAL_PLAYLIST_ID || id in knownCharts
                if (!known && !item.title.contains("100")) return@mapNotNull null
                AppleDailyChart(id, label(item.title), id == GLOBAL_PLAYLIST_ID)
            }
            .distinctBy { it.playlistId }
            .toList()
        val countries = all.filterNot { it.global }
        val byId = countries.associateBy { it.playlistId }
        val local = countryCharts[AppleStorefronts.normalize(storefront)]?.let(byId::get) ?: countries.firstOrNull()
        val leading = leadingMarkets.mapNotNull { countryCharts[it]?.let(byId::get) }
        val (known, other) = countries.partition { it.playlistId in knownCharts }
        return (listOfNotNull(local, all.firstOrNull { it.global }) + leading + known + other).distinct()
    }

    fun label(title: String): String {
        val trimmed = title.trim()
        return trimmed.replaceFirst(titlePrefix, "").trim().ifEmpty { trimmed }.replaceFirstChar { it.titlecase(java.util.Locale.ROOT) }
    }
}

data class AppleCatalogShelf(
    val id: String,
    val title: String,
    val kind: AppleCatalogShelfKind,
    val items: List<AppleCatalogItem>,
    val rows: Int = 1,
    val roomId: String = ""
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
        if (itemKind == "containerDetailTracklistFooterLockup") {
            val description = section.optJSONArray("items")?.optJSONObject(0)?.optString("description").orEmpty().trim()
            if (description.isBlank()) return null
            return AppleCatalogShelf(
                id = section.optString("id"), title = "", kind = AppleCatalogShelfKind.Footer,
                items = listOf(AppleCatalogItem(id = "footer", kind = AppleCatalogItemKind.Other, title = "", subtitle = "", description = description))
            )
        }
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
        return AppleCatalogShelf(section.optString("id"), title, kind, distinct, rows.coerceIn(1, 4), sectionRoomId(section))
    }

    internal fun sectionRoomId(section: JSONObject): String =
        section.optJSONObject("header")?.optJSONObject("item")?.optJSONObject("titleLink")
            ?.optJSONObject("segue")?.optJSONObject("destination")?.optJSONObject("intent")
            ?.optString("id").orEmpty().takeIf { id -> id.isNotEmpty() && id.all(Char::isDigit) }.orEmpty()

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

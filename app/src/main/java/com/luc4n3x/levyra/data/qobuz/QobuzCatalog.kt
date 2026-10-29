package com.luc4n3x.levyra.data.qobuz

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.Track
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale

data class QobuzAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val hiRes: Boolean = false,
    val award: String = "",
    val genre: String = "",
    val releaseLabel: String = ""
) {
    fun toAlbumHit(): AlbumHit = AlbumHit(
        title = title, artist = artist, year = "", thumbnailUrl = coverUrl,
        query = "$title $artist".trim(), canonicalUrl = QobuzStore.albumUrl(id),
        metadataProvider = QobuzCatalogTracks.SOURCE
    )
}

data class QobuzPlaylist(
    val id: String,
    val slug: String,
    val title: String,
    val imageUrl: String
)

data class QobuzPlaylistDetail(
    val id: String,
    val title: String,
    val curator: String,
    val summary: String,
    val imageUrl: String,
    val tracks: List<Track>,
    val fetchedAtMs: Long
)

enum class QobuzShelfKind(val cacheName: String) {
    NewReleases("new-releases"),
    Playlists("playlists"),
    PressAwards("press-awards"),
    Qobuzissime("qobuzissime"),
    IdealDiscography("ideal-discography")
}

data class QobuzShelf(
    val kind: QobuzShelfKind,
    val albums: List<QobuzAlbum> = emptyList(),
    val playlists: List<QobuzPlaylist> = emptyList(),
    val fetchedAtMs: Long
) {
    val isEmpty: Boolean get() = albums.isEmpty() && playlists.isEmpty()
}

object QobuzStore {
    private const val HOST = "www.qobuz.com"
    private val englishStores = setOf("us", "gb", "au", "ca", "nz", "ie", "se", "dk")

    fun locale(language: String, country: String): String {
        val lang = language.trim().lowercase(Locale.ROOT).substringBefore('-').substringBefore('_')
        val region = country.trim().lowercase(Locale.ROOT).let { if (it == "uk") "gb" else it }
        return when (lang) {
            "it" -> "it-it"
            "fr" -> when (region) { "be" -> "be-fr"; "ch" -> "ch-fr"; else -> "fr-fr" }
            "de" -> when (region) { "at" -> "at-de"; "ch" -> "ch-de"; else -> "de-de" }
            "es" -> "es-es"
            "nl" -> if (region == "be") "be-nl" else "nl-nl"
            "ja" -> "jp-ja"
            else -> if (region in englishStores) "$region-en" else "us-en"
        }
    }

    fun isStoreUrl(url: String): Boolean {
        val parsed = runCatching { java.net.URI(url) }.getOrNull() ?: return false
        return parsed.scheme == "https" && parsed.host == HOST && parsed.port == -1 && parsed.userInfo == null
    }

    fun pageUrl(locale: String, kind: QobuzShelfKind): String = "https://$HOST/$locale/" + when (kind) {
        QobuzShelfKind.NewReleases -> "shop"
        QobuzShelfKind.Playlists -> "playlists"
        QobuzShelfKind.PressAwards -> "shop/press-awards/download-streaming-albums"
        QobuzShelfKind.Qobuzissime -> "awards/qobuz/qobuzissime/download-streaming-albums"
        QobuzShelfKind.IdealDiscography -> "awards/qobuz/discotheque_ideale/download-streaming-albums"
    }

    fun playlistUrl(locale: String, slug: String, id: String): String = "https://$HOST/$locale/playlists/${slug.ifBlank { "playlist" }}/$id"

    fun albumUrl(id: String): String = "https://$HOST/album/$id"

    fun coverForAlbum(id: String, size: Int): String {
        val clean = id.filter(Char::isLetterOrDigit).lowercase(Locale.ROOT)
        if (clean.length < 4) return ""
        return "https://static.qobuz.com/images/covers/${clean.takeLast(2)}/${clean.dropLast(2).takeLast(2)}/${clean}_$size.jpg"
    }

    fun cover(url: String, size: Int): String {
        if (!url.startsWith("https://static.qobuz.com/images/covers/")) return url
        return url.replace(Regex("_(\\d+|max|org)\\.jpg$"), "_$size.jpg")
    }
}

internal object QobuzCatalogParser {
    private val tag = Regex("<[^>]+>")
    private val space = Regex("\\s+")
    private val albumContainer = Regex("<li class=\"album-container([^\"]*)\"(.*?)</li>", RegexOption.DOT_MATCHES_ALL)
    private val productItem = Regex("<div class=\"product__item\">(.*?)(?=<div class=\"product__item\">|</ul>)", RegexOption.DOT_MATCHES_ALL)
    private val albumHref = Regex("href=\"/[a-z]{2}-[a-z]{2}/album/[^/\"]+/([A-Za-z0-9]+)\"")
    private val coverSrc = Regex("(?:src|data-src)=\"(https://static\\.qobuz\\.com/images/covers/[^\"]+\\.jpg)\"")
    private val albumTitle = Regex("class=\"album-title[^\"]*\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
    private val artistName = Regex("class=\"artist-name[^\"]*\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
    private val award = Regex("class=\"label label-success album-award\"[^>]*>(.*?)</span>", RegexOption.DOT_MATCHES_ALL)
    private val productName = Regex("class=\"product__name\"[^>]*?data-title=\"([^\"]*)\"")
    private val productArtist = Regex("class=\"product__artist\">\\s*<a[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
    private val productGenre = Regex("class=\"product__data--genre\">(.*?)</p>", RegexOption.DOT_MATCHES_ALL)
    private val playlistCard = Regex("<a class=\"album-playlists__card[^\"]*\" href=\"/[a-z]{2}-[a-z]{2}/playlists/([^/\"]+)/(\\d+)\">(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
    private val playlistImage = Regex("data-src=\"(https://static\\.qobuz\\.com/images/playlists/[^\"]+)\"")
    private val playlistName = Regex("class=\"album-playlists__name\">(.*?)</h2>", RegexOption.DOT_MATCHES_ALL)
    private val trackRow = Regex("<div class=\"track track--playlist\"(.*?)class=\"track__infos\"", RegexOption.DOT_MATCHES_ALL)
    private val trackName = Regex("track__item--name\" title=\"([^\"]*)\"")
    private val trackArtist = Regex("track__item--artist\" title=\"([^\"]*)\"")
    private val trackAlbum = Regex("track__item--album\" title=\"([^\"]*)\"")
    private val trackAlbumId = Regex("track__item--album[^>]*>\s*<a class=\"link\" href=\"/[a-z]{2}-[a-z]{2}/album/[^/\"]+/([A-Za-z0-9]+)\"")
    private val trackDuration = Regex("track__item--duration\">\\s*(\\d{2}):(\\d{2}):(\\d{2})")
    private val metaTitle = Regex("class=\"album-meta__title\"\\s+title=\"([^\"]*)\"")
    private val metaCurator = Regex("class=\"album-meta__artist\"\\s+title=\"([^\"]*)\"")
    private val metaItem = Regex("class=\"album-meta__item\">(.*?)</li>", RegexOption.DOT_MATCHES_ALL)
    private val headerImage = Regex("album-cover--playlist\">\\s*<img[^>]*data-src=\"(https://static\\.qobuz\\.com/images/playlists/[^\"]+)\"", RegexOption.DOT_MATCHES_ALL)

    fun parseShelf(kind: QobuzShelfKind, html: String, now: Long): QobuzShelf = when (kind) {
        QobuzShelfKind.NewReleases -> QobuzShelf(kind, albums = editorialNewReleases(html), fetchedAtMs = now)
        QobuzShelfKind.PressAwards -> QobuzShelf(kind, albums = albumContainers(html), fetchedAtMs = now)
        QobuzShelfKind.Qobuzissime, QobuzShelfKind.IdealDiscography -> QobuzShelf(kind, albums = products(html), fetchedAtMs = now)
        QobuzShelfKind.Playlists -> QobuzShelf(kind, playlists = playlists(html), fetchedAtMs = now)
    }

    fun editorialNewReleases(html: String): List<QobuzAlbum> = albumContainers(html, editorialOnly = true)

    fun albumContainers(html: String, editorialOnly: Boolean = false): List<QobuzAlbum> =
        albumContainer.findAll(html).mapNotNull { match ->
            val classes = match.groupValues[1]
            if (editorialOnly && !classes.split(' ').contains("homepage")) return@mapNotNull null
            val body = match.groupValues[2]
            val id = albumHref.find(body)?.groupValues?.get(1) ?: return@mapNotNull null
            val title = albumTitle.find(body)?.groupValues?.get(1)?.let(::text).orEmpty()
            if (title.isBlank()) return@mapNotNull null
            QobuzAlbum(
                id = id,
                title = title,
                artist = artistName.find(body)?.groupValues?.get(1)?.let(::text).orEmpty(),
                coverUrl = coverSrc.find(body)?.groupValues?.get(1)?.let { QobuzStore.cover(it, COVER_SIZE) }.orEmpty(),
                hiRes = classes.split(' ').contains("smr") || body.contains("smr-audio"),
                award = award.find(body)?.groupValues?.get(1)?.let(::text).orEmpty()
            )
        }.distinctBy { it.id }.take(MAX_ALBUMS).toList()

    fun products(html: String): List<QobuzAlbum> =
        productItem.findAll(html).mapNotNull { match ->
            val body = match.groupValues[1]
            val id = albumHref.find(body)?.groupValues?.get(1) ?: return@mapNotNull null
            val title = productName.find(body)?.groupValues?.get(1)?.let(::text).orEmpty()
            if (title.isBlank()) return@mapNotNull null
            QobuzAlbum(
                id = id,
                title = title,
                artist = productArtist.find(body)?.groupValues?.get(1)?.let(::text).orEmpty(),
                coverUrl = coverSrc.find(body)?.groupValues?.get(1)?.let { QobuzStore.cover(it, COVER_SIZE) }.orEmpty(),
                hiRes = body.contains("store-quality--hires") || body.contains("Hi-Res"),
                genre = productGenre.find(body)?.groupValues?.get(1)?.let(::text).orEmpty()
            )
        }.distinctBy { it.id }.take(MAX_ALBUMS).toList()

    fun playlists(html: String): List<QobuzPlaylist> =
        playlistCard.findAll(html).mapNotNull { match ->
            val body = match.groupValues[3]
            val title = playlistName.find(body)?.groupValues?.get(1)?.let(::text).orEmpty()
            if (title.isBlank()) return@mapNotNull null
            QobuzPlaylist(
                id = match.groupValues[2],
                slug = match.groupValues[1],
                title = title,
                imageUrl = playlistImage.find(body)?.groupValues?.get(1).orEmpty()
            )
        }.distinctBy { it.id }.take(MAX_PLAYLISTS).toList()

    fun playlistDetail(id: String, html: String, now: Long): QobuzPlaylistDetail? {
        val tracks = trackRow.findAll(html).mapNotNull { match ->
            val row = match.groupValues[1]
            val title = trackName.find(row)?.groupValues?.get(1)?.let(::text) ?: return@mapNotNull null
            val duration = trackDuration.find(row)?.groupValues?.let { parts ->
                (parts[1].toLong() * 3600L + parts[2].toLong() * 60L + parts[3].toLong()) * 1000L
            } ?: 0L
            QobuzCatalogTracks.track(
                title = title,
                artist = trackArtist.find(row)?.groupValues?.get(1)?.let(::text).orEmpty(),
                album = trackAlbum.find(row)?.groupValues?.get(1)?.let(::text).orEmpty(),
                durationMs = duration,
                coverUrl = trackAlbumId.find(row)?.groupValues?.get(1)?.let { QobuzStore.coverForAlbum(it, 600) }.orEmpty()
            )
        }.distinctBy { it.id }.take(MAX_TRACKS).toList()
        if (tracks.isEmpty()) return null
        return QobuzPlaylistDetail(
            id = id,
            title = metaTitle.find(html)?.groupValues?.get(1)?.let(::text).orEmpty(),
            curator = metaCurator.find(html)?.groupValues?.get(1)?.let(::text).orEmpty(),
            summary = metaItem.find(html)?.groupValues?.get(1)?.let(::text).orEmpty(),
            imageUrl = headerImage.find(html)?.groupValues?.get(1).orEmpty(),
            tracks = tracks,
            fetchedAtMs = now
        )
    }

    internal fun text(raw: String): String = decode(tag.replace(raw, " ")).replace(space, " ").trim()

    private fun decode(value: String): String = value
        .replace(Regex("&#x([0-9a-fA-F]+);")) { it.groupValues[1].toIntOrNull(16)?.let(::codePoint) ?: it.value }
        .replace(Regex("&#(\\d+);")) { it.groupValues[1].toIntOrNull()?.let(::codePoint) ?: it.value }
        .replace("&quot;", "\"").replace("&#039;", "'").replace("&apos;", "'")
        .replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ").replace("&ndash;", "–")
        .replace("&rsquo;", "’").replace("&hellip;", "…").replace("&amp;", "&")

    private fun codePoint(value: Int): String = if (value in 1..0x10FFFF) String(Character.toChars(value)) else ""

    private const val COVER_SIZE = 600
    private const val MAX_ALBUMS = 60
    private const val MAX_PLAYLISTS = 40
    private const val MAX_TRACKS = 100
}

object QobuzCatalogTracks {
    fun track(title: String, artist: String, album: String = "", durationMs: Long = 0L, coverUrl: String = ""): Track? {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return null
        val cleanArtist = artist.trim()
        val digest = MessageDigest.getInstance("SHA-256").digest("$cleanTitle|$cleanArtist".lowercase(Locale.ROOT).toByteArray())
        val seed = digest.take(4).fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) } and Int.MAX_VALUE
        return Track(
            id = "chart-qobuz-" + digest.take(8).joinToString("") { "%02x".format(it) },
            title = cleanTitle,
            artist = cleanArtist.ifBlank { "Various Artists" },
            album = album.trim(),
            durationMs = durationMs.coerceAtLeast(0L),
            streamUrl = "",
            videoUrl = "",
            thumbnailUrl = coverUrl,
            largeThumbnailUrl = coverUrl,
            source = SOURCE,
            moodTags = setOf("chart"),
            energy = 70,
            vocal = 55,
            replayScore = 80,
            cacheScore = 70,
            accentStart = 0xFF000000.toInt() or (seed and 0xFFFFFF),
            accentEnd = 0xFF000000.toInt() or ((seed ushr 8) and 0xFFFFFF)
        )
    }

    const val SOURCE = "Qobuz Catalog"
}

internal object QobuzShelfCodec {
    fun encode(shelf: QobuzShelf): String = JSONObject()
        .put("kind", shelf.kind.name)
        .put("fetchedAtMs", shelf.fetchedAtMs)
        .put("albums", JSONArray().apply {
            shelf.albums.forEach { album ->
                put(JSONObject().put("id", album.id).put("title", album.title).put("artist", album.artist)
                    .put("cover", album.coverUrl).put("hiRes", album.hiRes).put("award", album.award).put("genre", album.genre))
            }
        })
        .put("playlists", JSONArray().apply {
            shelf.playlists.forEach { playlist ->
                put(JSONObject().put("id", playlist.id).put("slug", playlist.slug).put("title", playlist.title).put("image", playlist.imageUrl))
            }
        })
        .toString()

    fun decode(raw: String): QobuzShelf? = runCatching {
        val json = JSONObject(raw)
        val albums = json.optJSONArray("albums") ?: JSONArray()
        val playlists = json.optJSONArray("playlists") ?: JSONArray()
        QobuzShelf(
            kind = QobuzShelfKind.valueOf(json.getString("kind")),
            albums = (0 until albums.length()).mapNotNull { index ->
                val item = albums.optJSONObject(index) ?: return@mapNotNull null
                QobuzAlbum(
                    id = item.optString("id"), title = item.optString("title"), artist = item.optString("artist"),
                    coverUrl = item.optString("cover"), hiRes = item.optBoolean("hiRes"),
                    award = item.optString("award"), genre = item.optString("genre")
                ).takeIf { it.id.isNotBlank() && it.title.isNotBlank() }
            },
            playlists = (0 until playlists.length()).mapNotNull { index ->
                val item = playlists.optJSONObject(index) ?: return@mapNotNull null
                QobuzPlaylist(item.optString("id"), item.optString("slug"), item.optString("title"), item.optString("image"))
                    .takeIf { it.id.isNotBlank() && it.title.isNotBlank() }
            },
            fetchedAtMs = json.getLong("fetchedAtMs")
        )
    }.getOrNull()
}

package com.luc4n3x.levyra.desktop.core.catalog

import com.luc4n3x.levyra.desktop.core.model.ArtistDetail
import com.luc4n3x.levyra.desktop.core.model.CollectionKind
import com.luc4n3x.levyra.desktop.core.model.CollectionRef
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.domain.isSingleLike
import com.luc4n3x.levyra.domain.parseCompactViewCount
import com.luc4n3x.levyra.domain.releaseTypeFromProviderLabel
import org.json.JSONArray
import org.json.JSONObject

internal data class ArtistShelf(val renderer: JSONObject, val header: JSONObject?, val type: ReleaseType) {
    val more: JSONObject?
        get() = artistObjects(header ?: renderer.optJSONObject("bottomEndpoint"), "browseEndpoint")
            .firstOrNull { it.optString("browseId").isNotBlank() }
}

internal object YoutubeMusicArtistPage {
    fun parse(root: JSONObject, ref: CollectionRef): ArtistDetail? {
        val header = root.optJSONObject("header")?.let { container ->
            listOf("musicImmersiveHeaderRenderer", "musicVisualHeaderRenderer", "musicHeaderRenderer")
                .firstNotNullOfOrNull(container::optJSONObject)
        } ?: return null
        val name = chooseDesktopArtistName(ref.title.takeUnless { it == ref.url }.orEmpty(), artistText(header, "title"))
        if (name.isBlank()) return null
        val banner = artistArt(header)
        val tracks = tracks(root, name, videos = false).take(50)
        val videos = tracks(root, name, videos = true).take(20)
        val shelves = shelves(root)
        val releaseCards = shelves.flatMap { shelf -> releases(shelf.renderer, shelf.type) }.distinctBy { it.second.id }
        val playlistCards = collections(root, CollectionKind.PLAYLIST)
        val (playlists, featured) = playlistCards.partition {
            it.subtitle.split("•", "·").any { label -> artistLabelMatches(label, name) }
        }
        val biography = artistText(header, "description").ifBlank {
            artistObjects(root, "musicDescriptionShelfRenderer").firstOrNull()?.let { artistText(it, "description") }.orEmpty()
        }
        return ArtistDetail(
            name = name,
            biography = biography,
            portraitUrl = ref.artworkUrl.ifBlank { banner },
            bannerUrl = banner,
            tracks = tracks,
            videos = videos,
            albums = releaseCards.filter { it.first == ReleaseType.Album }.map { it.second },
            singles = releaseCards.filter { it.first.isSingleLike }.map { it.second },
            compilations = releaseCards.filter { it.first == ReleaseType.Compilation }.map { it.second },
            playlists = playlists,
            featuredPlaylists = featured,
            relatedArtists = collections(root, CollectionKind.ARTIST).filterNot { it.id == ref.id },
            monthlyListeners = parseCompactViewCount(artistText(header, "monthlyListenerCount")).takeIf { it > 0 },
            subscriberCount = artistObjects(header, "subscribeButtonRenderer").firstOrNull()
                ?.let { parseCompactViewCount(artistText(it, "subscriberCountText")) } ?: -1,
            viewsLabel = artistObjects(root, "musicDescriptionShelfRenderer").asSequence()
                .map { artistText(it, "subheader") }.firstOrNull { it.any(Char::isDigit) }.orEmpty()
        )
    }

    fun shelves(root: JSONObject): List<ArtistShelf> =
        listOf("musicShelfRenderer", "musicCarouselShelfRenderer", "gridRenderer").flatMap { key ->
            artistObjects(root, key).map { renderer ->
                val header = renderer.optJSONObject("header")?.let { container ->
                    container.optJSONObject("musicCarouselShelfBasicHeaderRenderer") ?: container.optJSONObject("gridHeaderRenderer")
                }
                val title = artistText(header ?: renderer, "title")
                ArtistShelf(renderer, header, releaseTypeFromProviderLabel(title))
            }
        }

    fun releases(root: JSONObject, inherited: ReleaseType): List<Pair<ReleaseType, CollectionRef>> =
        collections(root, CollectionKind.ALBUM).map { release ->
            val type = releaseTypeFromProviderLabel(release.subtitle).takeUnless { it == ReleaseType.Unknown }
                ?: inherited.takeUnless { it == ReleaseType.Unknown } ?: ReleaseType.Album
            type to release
        }

    fun tracks(root: JSONObject, artist: String, videos: Boolean): List<Track> {
        val renderer = if (videos) "musicTwoRowItemRenderer" else "musicResponsiveListItemRenderer"
        return artistObjects(root, renderer).mapNotNull { row ->
            val first = row.optJSONArray("flexColumns")?.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            val title = artistText(first ?: row, if (first == null) "title" else "text")
            val navigation = row.optJSONObject("navigationEndpoint")
            val endpoint = navigation?.optJSONObject("watchEndpoint") ?: artistObjects(first, "watchEndpoint").firstOrNull()
            val id = endpoint?.optString("videoId").orEmpty().ifBlank {
                row.optJSONObject("playlistItemData")?.optString("videoId").orEmpty()
            }
            if (id.isBlank() || title.isBlank()) return@mapNotNull null
            val type = endpoint?.optJSONObject("watchEndpointMusicSupportedConfigs")
                ?.optJSONObject("watchEndpointMusicConfig")?.optString("musicVideoType").orEmpty()
            if (!videos && (type == "MUSIC_VIDEO_TYPE_OMV" || type == "MUSIC_VIDEO_TYPE_UGC")) return@mapNotNull null
            val columns = artistObjects(row.optJSONArray("flexColumns"), "musicResponsiveListItemFlexColumnRenderer") +
                artistObjects(row.optJSONArray("fixedColumns"), "musicResponsiveListItemFixedColumnRenderer")
            val lines = columns.drop(1).flatMap { column -> artistText(column, "text").split(" • ", " · ") }
            val credits = if (videos) artistText(row, "subtitle").substringBefore(" • ") else lines.firstOrNull().orEmpty()
            val duration = lines.firstOrNull(ArtistDuration::matches).orEmpty()
            val seconds = duration.split(':').mapNotNull(String::toLongOrNull).fold(0L) { total, part -> total * 60 + part }
            val metadata = if (videos) artistText(row, "subtitle").split(" • ").drop(1) else lines.drop(1)
            val views = metadata.firstNotNullOfOrNull { label ->
                parseCompactViewCount(label).takeIf { it >= 0L && ViewCountLabel.containsMatchIn(label) }
            } ?: 0L
            Track(id, title, credits.ifBlank { artist }, videoUrl = Track.watchUrlOf(id), artworkUrl = artistArt(row),
                durationMs = seconds * 1000L, musicVideo = videos, viewCount = views)
        }.distinctBy { it.id }
    }

    private fun collections(root: JSONObject, kind: CollectionKind): List<CollectionRef> =
        artistObjects(root, "musicTwoRowItemRenderer").mapNotNull { card ->
            val navigation = card.optJSONObject("navigationEndpoint")
            val browseId = navigation?.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty()
            val title = artistText(card, "title")
            if (title.isBlank()) return@mapNotNull null
            val id = when (kind) {
                CollectionKind.ARTIST -> browseId.takeIf { it.startsWith("UC") } ?: return@mapNotNull null
                CollectionKind.ALBUM -> browseId.takeIf { it.startsWith("MPRE") } ?: return@mapNotNull null
                CollectionKind.PLAYLIST -> browseId.takeIf { it.startsWith("VL") }?.removePrefix("VL")
                    ?: navigation?.optJSONObject("watchPlaylistEndpoint")?.optString("playlistId")?.takeIf(String::isNotBlank)
                    ?: return@mapNotNull null
            }
            val url = when (kind) {
                CollectionKind.ARTIST -> "https://www.youtube.com/channel/$id"
                CollectionKind.ALBUM -> "https://music.youtube.com/browse/$id"
                CollectionKind.PLAYLIST -> "https://music.youtube.com/playlist?list=$id"
            }
            CollectionRef(id, title, artistText(card, "subtitle"), artistArt(card), url, kind)
        }.distinctBy { it.id }
}

private fun artistText(node: JSONObject, key: String): String {
    val text = node.optJSONObject(key) ?: return ""
    val runs = text.optJSONArray("runs") ?: return text.optString("simpleText")
    return (0 until runs.length()).joinToString("") { runs.optJSONObject(it)?.optString("text").orEmpty() }.trim()
}

private fun artistArt(node: JSONObject): String =
    artistObjects(node, "thumbnail").flatMap { thumbnail ->
        val urls = thumbnail.optJSONArray("thumbnails") ?: return@flatMap emptyList()
        (0 until urls.length()).mapNotNull(urls::optJSONObject)
    }.filter { it.optString("url").isNotBlank() }.maxByOrNull { it.optInt("width") }?.optString("url").orEmpty()

private fun artistObjects(root: Any?, key: String): List<JSONObject> = buildList {
    fun visit(value: Any?) {
        when (value) {
            is JSONObject -> {
                value.optJSONObject(key)?.let(::add)
                value.keys().forEach { visit(value.opt(it)) }
            }
            is JSONArray -> for (index in 0 until value.length()) visit(value.opt(index))
        }
    }
    visit(root)
}

private val ArtistDuration = Regex("""[0-9]+(:[0-9]{2}){1,2}""")
private val ViewCountLabel = Regex(
    """views?|plays?|visualiza|visualizz|vues?|aufruf|reprodu|vistas?|weergav|просмотр|перегляд|wyświetl|조회|再生|视|觀看|ชม|lượt xem|مشاهد""",
    RegexOption.IGNORE_CASE
)

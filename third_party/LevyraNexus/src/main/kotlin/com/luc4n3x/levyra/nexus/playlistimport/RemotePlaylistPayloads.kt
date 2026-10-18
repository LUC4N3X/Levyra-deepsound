package com.luc4n3x.levyra.nexus.playlistimport

import java.util.Locale

data class RemoteTrackPage(
    val tracks: List<ImportedTrackIdentity>,
    val total: Int?,
    val title: String = "",
    val owner: String = "",
    val description: String = "",
    val artworkUrl: String = ""
)

data class SpotifyEmbedPayload(
    val page: RemoteTrackPage,
    val accessToken: String?
)

data class SpotifyTrackPage(
    val tracks: List<ImportedTrackIdentity>,
    val total: Int,
    val offset: Int,
    val itemCount: Int,
    val hasNext: Boolean
)

object RemotePlaylistPayloads {
    private val nextData = Regex("<script[^>]*id=\"__NEXT_DATA__\"[^>]*>(.*?)</script>", RegexOption.DOT_MATCHES_ALL)
    private val serverData = Regex("<script[^>]*id=\"serialized-server-data\"[^>]*>(.*?)</script>", RegexOption.DOT_MATCHES_ALL)
    private val ldJson = Regex("<script[^>]*type=\"application/ld\\+json\"[^>]*>(.*?)</script>", RegexOption.DOT_MATCHES_ALL)
    private val isoDuration = Regex("^P(?:(\\d+)D)?T?(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+(?:\\.\\d+)?)S)?$")
    private val spotifyTrackUri = Regex("^spotify:track:([A-Za-z0-9]{22})$")
    private val htmlEntity = Regex("&(#[xX][0-9a-fA-F]{1,6}|#\\d{1,7}|amp|lt|gt|quot|apos|#39);")

    fun spotifyEmbed(html: String): RemoteTrackPage = spotifyEmbedPayload(html).page

    fun spotifyEmbedPayload(html: String): SpotifyEmbedPayload {
        val json = nextData.find(html)?.groupValues?.get(1) ?: providerChanged("Spotify embed data missing")
        val state = BoundedJson.parseOrNull(json).path("props", "pageProps", "state")
            ?: providerChanged("Spotify embed state missing")
        val entity = state.path("data", "entity").jsonObject()
            ?: providerChanged("Spotify embed entity missing")
        val list = entity["trackList"].jsonArray() ?: providerChanged("Spotify track list missing")
        val tracks = list.mapNotNull { raw ->
            val item = raw.jsonObject() ?: return@mapNotNull null
            val title = item.string("title")
            if (title.isBlank()) return@mapNotNull null
            ImportedTrackIdentity(
                position = 0,
                title = title,
                artists = PlaylistTextParsers.splitArtists(item.string("subtitle")),
                durationMs = item.long("duration")?.coerceAtLeast(0L) ?: 0L,
                sourceTrackId = spotifyTrackUri.find(item.string("uri"))?.groupValues?.get(1).orEmpty(),
                explicit = item.bool("isExplicit")
            )
        }
        val artwork = entity["coverArt"].jsonObject()?.get("sources").jsonArray()
            ?.mapNotNull { it.jsonObject()?.string("url") }
            ?.firstOrNull { it.startsWith("https://") }
            .orEmpty()
        return SpotifyEmbedPayload(
            page = RemoteTrackPage(
                tracks = tracks.reindexed(),
                total = null,
                title = entity.string("name", "title"),
                owner = entity.string("subtitle"),
                artworkUrl = artwork
            ),
            accessToken = spotifyAccessToken(state)
        )
    }

    fun spotifyTracks(json: String): SpotifyTrackPage {
        val root = BoundedJson.parseOrNull(json).jsonObject() ?: providerChanged("Spotify tracks payload invalid")
        val items = root["items"].jsonArray() ?: providerChanged("Spotify tracks missing")
        val total = root.long("total")?.toInt()?.takeIf { it >= 0 }
            ?: providerChanged("Spotify track total missing")
        val offset = root.long("offset")?.toInt()?.takeIf { it >= 0 }
            ?: providerChanged("Spotify track offset missing")
        val tracks = items.mapNotNull { raw ->
            val item = raw.jsonObject() ?: return@mapNotNull null
            val track = item["item"].jsonObject() ?: item["track"].jsonObject() ?: return@mapNotNull null
            if (track.string("type").let { it.isNotBlank() && it != "track" }) return@mapNotNull null
            val title = track.string("name")
            if (title.isBlank()) return@mapNotNull null
            val album = track["album"].jsonObject()
            val artwork = album?.get("images").jsonArray().orEmpty()
                .mapNotNull { it.jsonObject()?.string("url") }
                .firstOrNull { it.startsWith("https://") }
                .orEmpty()
            val releaseYear = album?.string("release_date")
                ?.take(4)
                ?.toIntOrNull()
                ?: 0
            ImportedTrackIdentity(
                position = 0,
                title = title,
                artists = track["artists"].jsonArray().orEmpty()
                    .mapNotNull { it.jsonObject()?.string("name")?.takeIf(String::isNotBlank) },
                album = album?.string("name").orEmpty(),
                durationMs = track.long("duration_ms")?.coerceAtLeast(0L) ?: 0L,
                isrc = track["external_ids"].jsonObject()?.string("isrc").orEmpty(),
                sourceTrackId = track.string("id"),
                releaseYear = releaseYear,
                explicit = track.bool("explicit"),
                trackNumber = track.long("track_number")?.toInt() ?: 0,
                discNumber = track.long("disc_number")?.toInt() ?: 0,
                artworkUrl = artwork,
                originalUrl = track["external_urls"].jsonObject()?.string("spotify")
                    ?.takeIf { it.startsWith("https://") }
                    .orEmpty()
            )
        }
        return SpotifyTrackPage(
            tracks = tracks.reindexed(),
            total = total,
            offset = offset,
            itemCount = items.size,
            hasNext = root.string("next").isNotBlank()
        )
    }

    private fun spotifyAccessToken(state: Any?): String? {
        val root = state.jsonObject() ?: return null
        val known = listOf(
            root["settings"].jsonObject()?.get("session").jsonObject()?.string("accessToken"),
            root["session"].jsonObject()?.string("accessToken"),
            root["settings"].jsonObject()?.string("accessToken")
        )
        return known.firstOrNull { !it.isNullOrBlank() } ?: findString(state, "accessToken", 0)
    }

    private fun findString(node: Any?, key: String, depth: Int): String? {
        if (depth > 40) return null
        return when (node) {
            is Map<*, *> -> {
                val map = node.jsonObject() ?: return null
                map[key]?.let { it as? String }?.takeIf(String::isNotBlank)
                    ?: map.values.firstNotNullOfOrNull { findString(it, key, depth + 1) }
            }
            is List<*> -> node.firstNotNullOfOrNull { findString(it, key, depth + 1) }
            else -> null
        }
    }

    fun deezerPlaylist(json: String): RemoteTrackPage {
        val root = BoundedJson.parseOrNull(json).jsonObject() ?: providerChanged("Deezer playlist payload invalid")
        deezerError(root)
        val tracks = root["tracks"].jsonObject()?.get("data").jsonArray().orEmpty().mapNotNull(::deezerTrack)
        return RemoteTrackPage(
            tracks = tracks.reindexed(),
            total = root.long("nb_tracks")?.toInt(),
            title = root.string("title"),
            owner = root["creator"].jsonObject()?.string("name").orEmpty(),
            description = root.string("description"),
            artworkUrl = root.string("picture_xl", "picture_big", "picture_medium").takeIf { it.startsWith("https://") }.orEmpty()
        )
    }

    fun deezerTracks(json: String): RemoteTrackPage {
        val root = BoundedJson.parseOrNull(json).jsonObject() ?: providerChanged("Deezer tracks payload invalid")
        deezerError(root)
        val data = root["data"].jsonArray() ?: providerChanged("Deezer tracks missing")
        return RemoteTrackPage(tracks = data.mapNotNull(::deezerTrack), total = root.long("total")?.toInt())
    }

    fun deezerErrorCode(json: String): Long? =
        BoundedJson.parseOrNull(json).jsonObject()?.get("error").jsonObject()?.long("code")

    private fun deezerError(root: Map<String, Any?>) {
        val error = root["error"].jsonObject() ?: return
        throw RemotePayloadException(error.long("code") ?: 0L, error.string("message"))
    }

    private fun deezerTrack(raw: Any?): ImportedTrackIdentity? {
        val item = raw.jsonObject() ?: return null
        val title = item.string("title")
        if (title.isBlank()) return null
        val album = item["album"].jsonObject()
        return ImportedTrackIdentity(
            position = 0,
            title = title,
            artists = listOfNotNull(item["artist"].jsonObject()?.string("name")?.takeIf(String::isNotBlank)),
            album = album?.string("title").orEmpty(),
            durationMs = (item.long("duration") ?: 0L).coerceAtLeast(0L) * 1000L,
            isrc = item.string("isrc"),
            sourceTrackId = item.string("id"),
            explicit = item.bool("explicit_lyrics"),
            artworkUrl = album?.string("cover_medium", "cover_big")?.takeIf { it.startsWith("https://") }.orEmpty(),
            originalUrl = item.string("link").takeIf { it.startsWith("https://") }.orEmpty()
        )
    }

    fun appleMusic(html: String): RemoteTrackPage {
        val playlist = ldJson.findAll(html)
            .mapNotNull { BoundedJson.parseOrNull(it.groupValues[1]).jsonObject() }
            .firstOrNull { it.string("@type") == "MusicPlaylist" }
        val server = serverData.find(html)?.groupValues?.get(1)?.let(BoundedJson::parseOrNull)
            ?: providerChanged("Apple Music data missing")
        val lockups = ArrayList<Map<String, Any?>>()
        collectAppleSongs(server, lockups, 0)
        if (lockups.isEmpty() && playlist == null) providerChanged("Apple Music tracks missing")
        val tracks = lockups.distinctBy { it["id"] }.mapNotNull { item ->
            val title = item.string("title")
            if (title.isBlank()) return@mapNotNull null
            val artists = item["subtitleLinks"].jsonArray().orEmpty()
                .mapNotNull { it.jsonObject()?.string("title")?.takeIf(String::isNotBlank) }
                .ifEmpty { PlaylistTextParsers.splitArtists(item.string("artistName")) }
            ImportedTrackIdentity(
                position = 0,
                title = title,
                artists = artists,
                album = item["tertiaryLinks"].jsonArray()?.firstOrNull().jsonObject()?.string("title").orEmpty(),
                durationMs = item.long("duration")?.coerceAtLeast(0L) ?: 0L,
                sourceTrackId = item["contentDescriptor"].jsonObject()?.get("identifiers").jsonObject()?.string("storeAdamID").orEmpty(),
                explicit = item.bool("showExplicitBadge", "isExplicit")
            )
        }
        val image = when (val value = playlist?.get("image")) {
            is String -> value
            else -> value.jsonObject()?.string("url") ?: value.jsonArray()?.firstOrNull()?.let { it as? String }.orEmpty()
        }
        return RemoteTrackPage(
            tracks = tracks.reindexed(),
            total = playlist?.long("numTracks")?.toInt(),
            title = decodeHtml(playlist?.string("name").orEmpty()),
            owner = playlist?.get("author").jsonObject()?.string("name").orEmpty(),
            description = decodeHtml(playlist?.string("description").orEmpty()),
            artworkUrl = image.takeIf { it.startsWith("https://") }.orEmpty()
        )
    }

    private fun collectAppleSongs(node: Any?, out: MutableList<Map<String, Any?>>, depth: Int) {
        if (depth > 40 || out.size > MAX_PLAYLIST_IMPORT_TRACKS) return
        when (node) {
            is Map<*, *> -> {
                val map = node.jsonObject() ?: return
                val descriptor = map["contentDescriptor"].jsonObject()
                if (descriptor?.string("kind") == "song" && map["subtitleLinks"] is List<*> && map.containsKey("duration")) {
                    out += map
                    return
                }
                map.values.forEach { collectAppleSongs(it, out, depth + 1) }
            }
            is List<*> -> node.forEach { collectAppleSongs(it, out, depth + 1) }
        }
    }

    fun jioSaavn(json: String): RemoteTrackPage {
        val root = BoundedJson.parseOrNull(json).jsonObject() ?: providerChanged("JioSaavn payload invalid")
        val list = root["list"].jsonArray() ?: root["songs"].jsonArray() ?: emptyList()
        val tracks = list.mapNotNull { raw ->
            val item = raw.jsonObject() ?: return@mapNotNull null
            val title = decodeHtml(item.string("title", "song"))
            if (title.isBlank()) return@mapNotNull null
            val info = item["more_info"].jsonObject().orEmpty()
            val artistMap = info["artistMap"].jsonObject()
            val primary = artistMap?.get("primary_artists").jsonArray().orEmpty()
                .mapNotNull { it.jsonObject()?.string("name")?.let(::decodeHtml)?.takeIf(String::isNotBlank) }
            val featured = artistMap?.get("featured_artists").jsonArray().orEmpty()
                .mapNotNull { it.jsonObject()?.string("name")?.let(::decodeHtml)?.takeIf(String::isNotBlank) }
            ImportedTrackIdentity(
                position = 0,
                title = title,
                artists = primary.ifEmpty { PlaylistTextParsers.splitArtists(decodeHtml(info.string("music"))) },
                featuredArtists = featured,
                album = decodeHtml(info.string("album")),
                durationMs = (info.long("duration") ?: 0L).coerceAtLeast(0L) * 1000L,
                sourceTrackId = item.string("id"),
                releaseYear = item.string("year").toIntOrNull() ?: 0,
                explicit = item.bool("explicit_content"),
                artworkUrl = item.string("image").takeIf { it.startsWith("https://") }.orEmpty(),
                originalUrl = item.string("perma_url").takeIf { it.startsWith("https://") }.orEmpty()
            )
        }
        return RemoteTrackPage(
            tracks = tracks,
            total = root.long("list_count")?.toInt(),
            title = decodeHtml(root.string("title", "listname")),
            owner = decodeHtml(root["more_info"].jsonObject()?.get("firstname")?.let { it as? String }.orEmpty()),
            artworkUrl = root.string("image").takeIf { it.startsWith("https://") }.orEmpty()
        )
    }

    fun bandcampAlbum(html: String): RemoteTrackPage {
        val album = ldJson.findAll(html)
            .mapNotNull { BoundedJson.parseOrNull(it.groupValues[1]).jsonObject() }
            .firstOrNull { it.string("@type") == "MusicAlbum" }
            ?: providerChanged("Bandcamp album data missing")
        val artist = album["byArtist"].jsonObject()?.string("name").orEmpty()
        val albumTitle = album.string("name")
        val items = album["track"].jsonObject()?.get("itemListElement").jsonArray().orEmpty()
        val tracks = items.mapNotNull { raw ->
            val entry = raw.jsonObject() ?: return@mapNotNull null
            val item = entry["item"].jsonObject() ?: return@mapNotNull null
            val title = decodeHtml(item.string("name"))
            if (title.isBlank()) return@mapNotNull null
            ImportedTrackIdentity(
                position = 0,
                title = title,
                artists = listOfNotNull(
                    item["byArtist"].jsonObject()?.string("name")?.takeIf(String::isNotBlank) ?: artist.takeIf(String::isNotBlank)
                ),
                album = albumTitle,
                durationMs = isoDurationMs(item.string("duration")),
                trackNumber = entry.long("position")?.toInt() ?: 0,
                originalUrl = item.string("@id").takeIf { it.startsWith("https://") }.orEmpty()
            )
        }
        val image = when (val value = album["image"]) {
            is String -> value
            else -> value.jsonArray()?.firstOrNull() as? String ?: ""
        }
        return RemoteTrackPage(
            tracks = tracks.reindexed(),
            total = album.long("numTracks")?.toInt(),
            title = decodeHtml(albumTitle),
            owner = artist,
            artworkUrl = image.takeIf { it.startsWith("https://") }.orEmpty()
        )
    }

    fun isoDurationMs(value: String): Long {
        val match = isoDuration.matchEntire(value.trim().uppercase(Locale.ROOT)) ?: return 0L
        val days = match.groupValues[1].toLongOrNull() ?: 0L
        val hours = match.groupValues[2].toLongOrNull() ?: 0L
        val minutes = match.groupValues[3].toLongOrNull() ?: 0L
        val seconds = match.groupValues[4].toDoubleOrNull() ?: 0.0
        return ((days * 86_400 + hours * 3_600 + minutes * 60) * 1000 + (seconds * 1000).toLong()).coerceAtLeast(0L)
    }

    fun decodeHtml(value: String): String = htmlEntity.replace(value) { match ->
        when (val entity = match.groupValues[1]) {
            "amp" -> "&"
            "lt" -> "<"
            "gt" -> ">"
            "quot" -> "\""
            "apos", "#39" -> "'"
            else -> {
                val code = if (entity.startsWith("#x") || entity.startsWith("#X")) {
                    entity.substring(2).toIntOrNull(16)
                } else {
                    entity.substring(1).toIntOrNull()
                }
                code?.takeIf { Character.isValidCodePoint(it) }?.let { String(Character.toChars(it)) } ?: match.value
            }
        }
    }

    private fun Any?.path(vararg keys: String): Any? {
        var current = this
        keys.forEach { key -> current = current.jsonObject()?.get(key) }
        return current
    }

    private fun providerChanged(message: String): Nothing =
        throw PlaylistParseException(PlaylistParseFailure.PROVIDER_CHANGED, message)
}

class RemotePayloadException(val code: Long, message: String) : Exception(message)

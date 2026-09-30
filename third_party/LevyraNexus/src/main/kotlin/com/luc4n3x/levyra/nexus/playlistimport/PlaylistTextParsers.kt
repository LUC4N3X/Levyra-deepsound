package com.luc4n3x.levyra.nexus.playlistimport

import java.util.Locale

object PlaylistTextParsers {
    private const val MAX_DURATION_MS = 6L * 60L * 60L * 1000L
    private val youtubeVideoId = Regex("^[A-Za-z0-9_-]{11}$")
    private val youtubeUrlId = Regex("(?:[?&]v=|youtu\\.be/|/shorts/|/embed/)([A-Za-z0-9_-]{11})")
    private val spotifyTrackId = Regex("(?:spotify:track:|open\\.spotify\\.com/(?:intl-[a-z-]+/)?track/)([A-Za-z0-9]{22})")
    private val leadingNumber = Regex("^\\s*\\d{1,4}\\s*[.)]\\s+")
    private val fileTrackNumber = Regex("^\\s*\\d{1,3}\\s*[-_.)]\\s*")
    private val titleArtistSeparator =Regex("\\s+[-–—]\\s+")
    private val byline = Regex("^(.+?)\\s+by\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val clock = Regex("^(?:(\\d{1,2}):)?(\\d{1,3}):(\\d{2})(?:\\.\\d+)?$")
    private val xmlTrack = Regex("<track\\b[^>]*>(.*?)</track>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val xmlTrackList = Regex("<trackList\\b", RegexOption.IGNORE_CASE)
    private val xmlEntity = Regex("&(#[xX][0-9a-fA-F]{1,6}|#\\d{1,7}|amp|lt|gt|quot|apos);")
    private val cdata = Regex("<!\\[CDATA\\[(.*?)]]>", RegexOption.DOT_MATCHES_ALL)
    private val listArtistSeparator = Regex("\\s*[,;]\\s*|\\s+/\\s+")
    private val year = Regex("\\b(1[89]\\d{2}|20\\d{2})\\b")
    private val whitespace = Regex("\\s+")
    private val xmlTagPatterns = listOf("title", "creator", "image", "location", "album", "duration", "trackNum")
        .associateWith { tag -> Regex("<$tag\\b[^>]*>(.*?)</$tag>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)) }

    private val JSON_TRACK_KEYS = listOf("tracks", "songs", "queue", "items", "entries", "trackList")
    private val TITLE_HEADERS = listOf("track name", "track title", "song name", "song title", "title", "name", "song", "track")
    private val ARTIST_HEADERS = listOf(
        "artist name(s)", "artist name", "artist names", "artist(s)", "artists", "artist", "performer", "creator",
        "album artist name(s)", "album artist"
    )
    private val ALBUM_HEADERS = listOf("album name", "album", "album title", "release")
    private val DURATION_MS_HEADERS = listOf("track duration (ms)", "duration (ms)", "duration_ms", "durationms", "duration ms", "length (ms)")
    private val DURATION_HEADERS = listOf("duration", "length", "time", "duration (s)", "track duration")
    private val ISRC_HEADERS = listOf("isrc")
    private val EXPLICIT_HEADERS = listOf("explicit", "is explicit")
    private val RELEASE_HEADERS = listOf("release date", "album release date", "year", "release year")
    private val ID_HEADERS = listOf("track uri", "spotify - id", "spotify id", "spotify uri", "uri")
    private val YOUTUBE_ID_HEADERS = listOf("mediaid", "videoid", "video id", "youtube id", "youtube - id")
    private val URL_HEADERS = listOf("url", "link", "track url", "youtube url")
    private val PLAYLIST_NAME_HEADERS = listOf("playlist name", "playlistname", "playlist")
    private val ARTWORK_HEADERS = listOf("thumbnailurl", "album image url", "artwork", "artwork url", "image")
    private val TRACK_NUMBER_HEADERS = listOf("track number", "track no", "tracknumber")
    private val DISC_NUMBER_HEADERS = listOf("disc number", "disc no", "discnumber")

    fun parse(format: PlaylistTextFormat, text: String, nameHint: String = ""): ParsedPlaylist {
        if (text.length > MAX_PLAYLIST_IMPORT_TEXT_CHARS) {
            throw PlaylistParseException(PlaylistParseFailure.TOO_LARGE, "Playlist text too large")
        }
        val clean = text.removePrefix("﻿")
        val parsed = when (format) {
            PlaylistTextFormat.M3U -> parseM3u(clean, nameHint)
            PlaylistTextFormat.PLS -> parsePls(clean, nameHint)
            PlaylistTextFormat.XSPF -> parseXspf(clean, nameHint)
            PlaylistTextFormat.CSV -> parseDelimited(clean, detectDelimiter(clean), nameHint)
            PlaylistTextFormat.TSV -> parseDelimited(clean, '\t', nameHint)
            PlaylistTextFormat.JSON -> parseJson(clean, nameHint)
            PlaylistTextFormat.TEXT -> parseTextList(clean, nameHint)
        }
        if (parsed.tracks.size > MAX_PLAYLIST_IMPORT_TRACKS) {
            throw PlaylistParseException(PlaylistParseFailure.TOO_LARGE, "Playlist has too many tracks")
        }
        if (parsed.tracks.isEmpty()) throw PlaylistParseException(PlaylistParseFailure.NO_TRACKS, "No usable tracks")
        return parsed.copy(tracks = parsed.tracks.reindexed())
    }

    fun looksLikeHeader(line: String, delimiter: Char): Boolean {
        if (line.none { it == delimiter }) return false
        val columns = splitDelimitedLine(line, delimiter).map(::headerKey)
        return columns.any { it in TITLE_HEADERS } &&
            columns.any { it in ARTIST_HEADERS || it in ID_HEADERS || it in URL_HEADERS || it in YOUTUBE_ID_HEADERS }
    }

    fun looksLikeTrackList(text: String): Boolean {
        val lines = text.lineSequence().map(String::trim).filter(String::isNotEmpty).take(50).toList()
        if (lines.isEmpty()) return false
        val structured = lines.count {
            titleArtistSeparator.containsMatchIn(it) || byline.matches(it) || youtubeUrlId.containsMatchIn(it)
        }
        return structured * 2 >= lines.size
    }

    fun splitArtists(raw: String): List<String> =
        raw.trim().removeSurrounding("\"").split(listArtistSeparator)
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()

    fun durationFrom(value: String, milliseconds: Boolean): Long {
        val clean = value.trim()
        if (clean.isEmpty()) return 0L
        clock.matchEntire(clean)?.let { match ->
            val hours = match.groupValues[1].toLongOrNull() ?: 0L
            val minutes = match.groupValues[2].toLongOrNull() ?: 0L
            val seconds = match.groupValues[3].toLongOrNull() ?: 0L
            return ((hours * 3600 + minutes * 60 + seconds) * 1000).takeIf { it in 1..MAX_DURATION_MS } ?: 0L
        }
        val number = clean.toDoubleOrNull()?.takeIf { it > 0 } ?: return 0L
        val ms = when {
            milliseconds -> number
            number > 36_000 -> number
            else -> number * 1000
        }.toLong()
        return ms.takeIf { it in 1..MAX_DURATION_MS } ?: 0L
    }

    fun parseDelimitedRows(text: String, delimiter: Char, maxRows: Int = MAX_PLAYLIST_IMPORT_TRACKS + 2): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val field = StringBuilder()
        var quoted = false
        var sawField = false
        var index = 0
        fun endField() {
            row.add(field.toString())
            field.setLength(0)
            sawField = false
        }
        fun endRow() {
            endField()
            if (row.any { it.isNotBlank() }) rows.add(row)
            row = ArrayList()
        }
        while (index < text.length && rows.size < maxRows) {
            val char = text[index]
            when {
                quoted && char == '"' -> if (index + 1 < text.length && text[index + 1] == '"') {
                    field.append('"')
                    index++
                } else {
                    quoted = false
                }
                quoted -> field.append(char)
                char == '"' && !sawField -> {
                    quoted = true
                    sawField = true
                }
                char == delimiter -> endField()
                char == '\r' -> {
                    if (index + 1 < text.length && text[index + 1] == '\n') index++
                    endRow()
                }
                char == '\n' -> endRow()
                else -> {
                    field.append(char)
                    sawField = true
                }
            }
            index++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }

    private fun parseM3u(text: String, nameHint: String): ParsedPlaylist {
        val tracks = ArrayList<ImportedTrackIdentity>()
        var title = nameHint
        var pendingDuration = 0L
        var pendingDisplay = ""
        var pendingArtist = ""
        var pendingAlbum = ""
        var skipped = 0
        text.lineSequence().map(String::trim).filter(String::isNotEmpty).forEach { line ->
            when {
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    val body = line.substringAfter(':')
                    val comma = findDisplayComma(body)
                    val header = if (comma >= 0) body.substring(0, comma) else body
                    pendingDuration = header.trim().substringBefore(' ').toDoubleOrNull()
                        ?.takeIf { it > 0 }?.let { (it * 1000).toLong() } ?: 0L
                    pendingDisplay = if (comma >= 0) body.substring(comma + 1).trim() else ""
                }
                line.startsWith("#PLAYLIST:", ignoreCase = true) -> title = line.substringAfter(':').trim().ifBlank { title }
                line.startsWith("#EXTART:", ignoreCase = true) -> pendingArtist = line.substringAfter(':').trim()
                line.startsWith("#EXTALB:", ignoreCase = true) -> pendingAlbum = line.substringAfter(':').trim()
                line.startsWith("#") -> Unit
                else -> {
                    val identity = locationEntry(tracks.size, line, pendingDisplay, pendingArtist, pendingAlbum, pendingDuration)
                    if (identity == null) skipped++ else tracks += identity
                    pendingDuration = 0L
                    pendingDisplay = ""
                    pendingArtist = ""
                    pendingAlbum = ""
                }
            }
        }
        return ParsedPlaylist(
            ImportedPlaylistDescriptor(PlaylistImportSource.M3U, title = title),
            tracks,
            PlaylistImportCompleteness.Complete,
            skipped
        )
    }

    private fun findDisplayComma(body: String): Int {
        var quoted = false
        body.forEachIndexed { index, char ->
            if (char == '"') quoted = !quoted
            if (char == ',' && !quoted) return index
        }
        return -1
    }

    private fun locationEntry(
        position: Int,
        location: String,
        display: String,
        artistHint: String,
        album: String,
        durationMs: Long
    ): ImportedTrackIdentity? {
        val videoId = youtubeUrlId.find(location)?.groupValues?.get(1).orEmpty()
        val isRemote = location.startsWith("http://", true) || location.startsWith("https://", true)
        val fileStem = location.substringAfterLast('/').substringAfterLast('\\')
            .substringBeforeLast('.')
            .replace('_', ' ')
            .replace(fileTrackNumber, "")
            .trim()
        val labelSource = display.ifBlank { if (isRemote) "" else fileStem }
        val (artist, title) = splitArtistTitle(labelSource, artistHint)
        if (title.isBlank() && videoId.isBlank()) return null
        return ImportedTrackIdentity(
            position = position,
            title = title.ifBlank { videoId },
            artists = splitArtists(artist),
            album = album,
            durationMs = durationMs.coerceIn(0L, MAX_DURATION_MS),
            originalUrl = if (isRemote) location else "",
            directCatalogId = videoId,
            localReference = if (isRemote) "" else location
        )
    }

    private fun parsePls(text: String, nameHint: String): ParsedPlaylist {
        val files = sortedMapOf<Int, String>()
        val titles = mutableMapOf<Int, String>()
        val lengths = mutableMapOf<Int, Long>()
        text.lineSequence().map(String::trim).forEach { line ->
            val separator = line.indexOf('=')
            if (separator <= 0) return@forEach
            val key = line.substring(0, separator).trim().lowercase(Locale.ROOT)
            val value = line.substring(separator + 1).trim()
            val index = key.dropWhile { it.isLetter() }.toIntOrNull() ?: return@forEach
            when {
                key.startsWith("file") -> files[index] = value
                key.startsWith("title") -> titles[index] = value
                key.startsWith("length") -> value.toLongOrNull()?.takeIf { it > 0 }?.let { lengths[index] = it * 1000 }
            }
        }
        val tracks = ArrayList<ImportedTrackIdentity>()
        var skipped = 0
        files.forEach { (index, location) ->
            val entry = locationEntry(tracks.size, location, titles[index].orEmpty(), "", "", lengths[index] ?: 0L)
            if (entry == null) skipped++ else tracks += entry
        }
        return ParsedPlaylist(
            ImportedPlaylistDescriptor(PlaylistImportSource.PLS, title = nameHint),
            tracks,
            PlaylistImportCompleteness.Complete,
            skipped
        )
    }

    private fun parseXspf(text: String, nameHint: String): ParsedPlaylist {
        if (text.contains("<!DOCTYPE", ignoreCase = true) || text.contains("<!ENTITY", ignoreCase = true)) {
            throw PlaylistParseException(PlaylistParseFailure.MALFORMED, "XSPF with DTD is not accepted")
        }
        val header = xmlTrackList.find(text)?.let { text.substring(0, it.range.first) } ?: text
        var skipped = 0
        val tracks = ArrayList<ImportedTrackIdentity>()
        xmlTrack.findAll(text).forEach { match ->
            val body = match.groupValues[1]
            val location = xmlValue(body, "location")
            val entryTitle = xmlValue(body, "title")
            val creator = xmlValue(body, "creator")
            val album = xmlValue(body, "album")
            val duration = xmlValue(body, "duration").toLongOrNull()?.coerceIn(0L, MAX_DURATION_MS) ?: 0L
            val remote = location.startsWith("http", ignoreCase = true)
            val identity = if (entryTitle.isBlank()) {
                locationEntry(tracks.size, location, "", creator, album, duration)
            } else {
                ImportedTrackIdentity(
                    position = tracks.size,
                    title = entryTitle,
                    artists = splitArtists(creator),
                    album = album,
                    durationMs = duration,
                    trackNumber = xmlValue(body, "trackNum").toIntOrNull() ?: 0,
                    artworkUrl = xmlValue(body, "image").takeIf { it.startsWith("https://") }.orEmpty(),
                    originalUrl = if (remote) location else "",
                    directCatalogId = youtubeUrlId.find(location)?.groupValues?.get(1).orEmpty(),
                    localReference = if (remote) "" else location
                )
            }
            if (identity == null) skipped++ else tracks += identity
        }
        return ParsedPlaylist(
            ImportedPlaylistDescriptor(
                PlaylistImportSource.XSPF,
                title = xmlValue(header, "title").ifBlank { nameHint },
                owner = xmlValue(header, "creator"),
                artworkUrl = xmlValue(header, "image").takeIf { it.startsWith("https://") }.orEmpty()
            ),
            tracks,
            PlaylistImportCompleteness.Complete,
            skipped
        )
    }

    private fun xmlValue(body: String, tag: String): String {
        val pattern = xmlTagPatterns[tag] ?: return ""
        val raw = pattern.find(body)?.groupValues?.get(1) ?: return ""
        return decodeXml(cdata.replace(raw) { it.groupValues[1] }).trim()
    }

    private fun decodeXml(value: String): String = xmlEntity.replace(value) { match ->
        val entity = match.groupValues[1]
        when {
            entity == "amp" -> "&"
            entity == "lt" -> "<"
            entity == "gt" -> ">"
            entity == "quot" -> "\""
            entity == "apos" -> "'"
            entity.startsWith("#x") || entity.startsWith("#X") -> codePoint(entity.substring(2).toIntOrNull(16))
            else -> codePoint(entity.substring(1).toIntOrNull())
        } ?: match.value
    }

    private fun codePoint(value: Int?): String? =
        value?.takeIf { Character.isValidCodePoint(it) }?.let { String(Character.toChars(it)) }

    private fun detectDelimiter(text: String): Char {
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        return listOf(',', ';', '\t').maxByOrNull { delimiter -> firstLine.count { it == delimiter } } ?: ','
    }

    private fun splitDelimitedLine(line: String, delimiter: Char): List<String> =
        parseDelimitedRows(line, delimiter, 1).firstOrNull().orEmpty()

    private fun headerKey(value: String): String =
        value.trim().removeSurrounding("\"").trim().lowercase(Locale.ROOT).replace(whitespace, " ")

    private class Columns(header: List<String>, key: (String) -> String) {
        private val keys = header.map(key)
        val title = find(TITLE_HEADERS)
        val artist = find(ARTIST_HEADERS)
        val album = find(ALBUM_HEADERS)
        val durationMs = find(DURATION_MS_HEADERS)
        val duration = find(DURATION_HEADERS)
        val isrc = find(ISRC_HEADERS)
        val explicit = find(EXPLICIT_HEADERS)
        val release = find(RELEASE_HEADERS)
        val spotifyId = find(ID_HEADERS)
        val youtubeId = find(YOUTUBE_ID_HEADERS)
        val url = find(URL_HEADERS)
        val playlistName = find(PLAYLIST_NAME_HEADERS)
        val artwork = find(ARTWORK_HEADERS)
        val trackNumber = find(TRACK_NUMBER_HEADERS)
        val discNumber = find(DISC_NUMBER_HEADERS)

        fun has(vararg names: String): Boolean = names.all { it in keys }

        private fun find(candidates: List<String>): Int {
            candidates.forEach { candidate ->
                val index = keys.indexOf(candidate)
                if (index >= 0) return index
            }
            return -1
        }
    }

    private fun parseDelimited(text: String, delimiter: Char, nameHint: String): ParsedPlaylist {
        val rows = parseDelimitedRows(text, delimiter)
        if (rows.isEmpty()) throw PlaylistParseException(PlaylistParseFailure.NO_TRACKS, "Empty table")
        if (rows.size > MAX_PLAYLIST_IMPORT_TRACKS + 1) {
            throw PlaylistParseException(PlaylistParseFailure.TOO_LARGE, "Table has too many rows")
        }
        val columns = Columns(rows.first(), ::headerKey)
        if (columns.title < 0 && columns.youtubeId < 0) {
            throw PlaylistParseException(PlaylistParseFailure.MALFORMED, "No title column")
        }
        val source = when {
            columns.has("track uri", "artist name(s)") -> PlaylistImportSource.EXPORTIFY
            columns.has("track name", "artist name", "playlist name") -> PlaylistImportSource.TUNEMYMUSIC
            columns.has("mediaid", "title", "artists") -> PlaylistImportSource.KREATE
            delimiter == '\t' -> PlaylistImportSource.TSV
            else -> PlaylistImportSource.CSV
        }
        var skipped = 0
        var playlistName = ""
        val tracks = ArrayList<ImportedTrackIdentity>()
        rows.drop(1).forEach { row ->
            fun cell(index: Int): String = if (index < 0) "" else row.getOrNull(index).orEmpty().trim()
            if (playlistName.isBlank()) playlistName = cell(columns.playlistName)
            val urlCell = cell(columns.url)
            val youtubeId = cell(columns.youtubeId).takeIf { youtubeVideoId.matches(it) }
                ?: youtubeUrlId.find(urlCell)?.groupValues?.get(1).orEmpty()
            val title = cell(columns.title)
            if (title.isBlank() && youtubeId.isBlank()) {
                skipped++
                return@forEach
            }
            val idCell = cell(columns.spotifyId)
            val spotify = spotifyTrackId.find(idCell)?.groupValues?.get(1)
                ?: idCell.takeIf { it.length == 22 && it.all(Char::isLetterOrDigit) }
                ?: spotifyTrackId.find(urlCell)?.groupValues?.get(1).orEmpty()
            tracks += ImportedTrackIdentity(
                position = tracks.size,
                title = title.ifBlank { youtubeId },
                artists = splitArtists(cell(columns.artist)),
                album = cell(columns.album),
                durationMs = durationFrom(cell(columns.durationMs), true).takeIf { it > 0 }
                    ?: durationFrom(cell(columns.duration), false),
                isrc = cell(columns.isrc).uppercase(Locale.ROOT).filter(Char::isLetterOrDigit),
                sourceTrackId = spotify,
                releaseYear = year.find(cell(columns.release))?.value?.toIntOrNull() ?: 0,
                explicit = booleanCell(cell(columns.explicit)),
                trackNumber = cell(columns.trackNumber).toIntOrNull() ?: 0,
                discNumber = cell(columns.discNumber).toIntOrNull() ?: 0,
                artworkUrl = cell(columns.artwork).takeIf { it.startsWith("https://") }.orEmpty(),
                originalUrl = urlCell.takeIf { it.startsWith("https://") }.orEmpty(),
                directCatalogId = youtubeId
            )
        }
        return ParsedPlaylist(
            ImportedPlaylistDescriptor(source, title = playlistName.ifBlank { nameHint }),
            tracks,
            PlaylistImportCompleteness.Complete,
            skipped
        )
    }

    private fun booleanCell(value: String): Boolean? = when (value.trim().lowercase(Locale.ROOT)) {
        "true", "1", "yes", "explicit", "e" -> true
        "false", "0", "no", "clean" -> false
        else -> null
    }

    private fun parseTextList(text: String, nameHint: String): ParsedPlaylist {
        var skipped = 0
        val tracks = ArrayList<ImportedTrackIdentity>()
        text.lineSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("//") }
            .forEach { line ->
                val videoId = youtubeUrlId.find(line)?.groupValues?.get(1).orEmpty()
                if (videoId.isNotBlank()) {
                    tracks += ImportedTrackIdentity(tracks.size, videoId, emptyList(), originalUrl = line, directCatalogId = videoId)
                    return@forEach
                }
                if (line.startsWith("http://", true) || line.startsWith("https://", true)) {
                    skipped++
                    return@forEach
                }
                val withoutNumber = line.replace(leadingNumber, "")
                val (artist, title) = byline.matchEntire(withoutNumber)
                    ?.let { it.groupValues[2].trim() to it.groupValues[1].trim() }
                    ?: splitArtistTitle(withoutNumber, "")
                if (title.isBlank()) {
                    skipped++
                    return@forEach
                }
                tracks += ImportedTrackIdentity(tracks.size, title, splitArtists(artist))
            }
        return ParsedPlaylist(
            ImportedPlaylistDescriptor(PlaylistImportSource.TEXT, title = nameHint),
            tracks,
            PlaylistImportCompleteness.Complete,
            skipped
        )
    }

    private fun splitArtistTitle(label: String, artistHint: String): Pair<String, String> {
        val clean = label.trim()
        if (artistHint.isNotBlank()) {
            val withoutArtist = if (clean.startsWith(artistHint, ignoreCase = true)) {
                clean.substring(artistHint.length).trimStart(' ', '-', '–', '—').trim()
            } else {
                clean
            }
            return artistHint to withoutArtist.ifBlank { clean }
        }
        val match = titleArtistSeparator.find(clean) ?: return "" to clean
        val artist = clean.substring(0, match.range.first).trim()
        val title = clean.substring(match.range.last + 1).trim()
        return if (artist.isBlank() || title.isBlank()) "" to clean else artist to title
    }

    private fun parseJson(text: String, nameHint: String): ParsedPlaylist {
        val root = try {
            BoundedJson.parse(text)
        } catch (error: BoundedJsonException) {
            throw PlaylistParseException(PlaylistParseFailure.MALFORMED, error.message.orEmpty())
        }
        val container = root.jsonObject()
        val playlistObject = container?.get("playlists").jsonArray()?.singleOrNull().jsonObject() ?: container
        val items = when {
            root is List<*> -> root
            playlistObject != null -> JSON_TRACK_KEYS.firstNotNullOfOrNull { playlistObject[it].jsonArray() }
            else -> null
        } ?: throw PlaylistParseException(PlaylistParseFailure.MALFORMED, "No track array")
        if (items.size > MAX_PLAYLIST_IMPORT_TRACKS) {
            throw PlaylistParseException(PlaylistParseFailure.TOO_LARGE, "JSON has too many tracks")
        }
        var skipped = 0
        val tracks = ArrayList<ImportedTrackIdentity>()
        items.forEach { item ->
            val identity = jsonTrack(tracks.size, item)
            if (identity == null) skipped++ else tracks += identity
        }
        return ParsedPlaylist(
            ImportedPlaylistDescriptor(
                PlaylistImportSource.JSON,
                title = playlistObject?.string("name", "title", "playlistName").orEmpty().ifBlank { nameHint },
                owner = playlistObject?.string("owner", "author", "creator").orEmpty(),
                description = playlistObject?.string("description").orEmpty()
            ),
            tracks,
            PlaylistImportCompleteness.Complete,
            skipped
        )
    }

    private fun jsonTrack(position: Int, raw: Any?): ImportedTrackIdentity? {
        val outer = raw.jsonObject() ?: return null
        val item = outer["track"].jsonObject() ?: outer
        val title = item.string("title", "name", "trackName", "track", "song")
        val id = item.string("videoId", "id", "mediaId")
        val videoId = id.takeIf { youtubeVideoId.matches(it) }
            ?: youtubeUrlId.find(item.string("url", "link", "videoUrl"))?.groupValues?.get(1).orEmpty()
        if (title.isBlank() && videoId.isBlank()) return null
        val albumObject = item["album"].jsonObject()
        val album = albumObject?.string("name", "title") ?: item.string("album", "albumName", "albumTitle")
        val durationMs = item.long("durationMs", "duration_ms")?.takeIf { it > 0 }
            ?: item.long("duration", "length", "lengthSeconds")?.takeIf { it > 0 }?.let { if (it > 86_400L) it else it * 1000 }
            ?: durationFrom(item.string("duration", "length"), false)
        val isrc = item.string("isrc").ifBlank { item["external_ids"].jsonObject()?.string("isrc").orEmpty() }
        val artwork = item.string("thumbnailUrl", "artworkUrl", "thumbnail", "image", "cover")
            .ifBlank { albumObject?.get("images").jsonArray()?.firstOrNull().jsonObject()?.string("url").orEmpty() }
        return ImportedTrackIdentity(
            position = position,
            title = title.ifBlank { videoId },
            artists = jsonArtists(item),
            album = album,
            durationMs = durationMs.coerceIn(0L, MAX_DURATION_MS),
            isrc = isrc.uppercase(Locale.ROOT).filter(Char::isLetterOrDigit),
            sourceTrackId = spotifyTrackId.find(item.string("uri", "spotifyUri", "url"))?.groupValues?.get(1).orEmpty(),
            releaseYear = year.find(item.string("year", "releaseDate", "release_date"))?.value?.toIntOrNull() ?: 0,
            explicit = item.bool("explicit", "isExplicit"),
            artworkUrl = artwork.takeIf { it.startsWith("https://") }.orEmpty(),
            directCatalogId = videoId
        )
    }

    private fun jsonArtists(item: Map<String, Any?>): List<String> {
        item["artists"].jsonArray()?.let { array ->
            val names = array.mapNotNull { value ->
                (if (value is String) value.trim() else value.jsonObject()?.string("name"))?.takeIf(String::isNotBlank)
            }
            if (names.isNotEmpty()) return names.distinct()
        }
        return splitArtists(item.string("artist", "artistName", "artists", "author", "creator", "uploader"))
    }
}

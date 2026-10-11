package com.luc4n3x.levyra.data.collections

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Locale
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

object PersonalCollectionParser {
    const val MAX_PLAYLIST_BYTES = 8 * 1024 * 1024
    const val MAX_ENTRIES = 20_000
    const val MAX_URL_LENGTH = 4_096
    const val MAX_TEXT_LENGTH = 512

    private val schemePattern = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")
    private val extInfAttribute = Regex("([A-Za-z0-9_-]+)=\"([^\"]*)\"")
    private val leadingDuration = Regex("^\\s*(-?\\d+(?:\\.\\d+)?)")
    private val whitespace = Regex("\\s+")
    private val windows1252: Charset = Charset.forName("windows-1252")
    private val hlsTags = listOf(
        "#EXT-X-TARGETDURATION",
        "#EXT-X-MEDIA-SEQUENCE",
        "#EXT-X-STREAM-INF",
        "#EXT-X-I-FRAME-STREAM-INF",
        "#EXT-X-PLAYLIST-TYPE",
        "#EXT-X-ENDLIST",
        "#EXT-X-KEY",
        "#EXT-X-MAP",
        "#EXT-X-PART",
        "#EXT-X-MEDIA:",
        "#EXT-X-VERSION",
        "#EXT-X-DISCONTINUITY"
    )

    fun parse(bytes: ByteArray, baseUrl: String?, fallbackName: String): CollectionParseResult {
        if (bytes.size > MAX_PLAYLIST_BYTES) return CollectionParseResult.Failure(CollectionParseError.TooLarge)
        val text = decode(bytes).trim()
        if (text.isEmpty()) return CollectionParseResult.Failure(CollectionParseError.Empty)
        val base = baseUrl?.toHttpUrlOrNull()?.withoutCredentials()
        val first = text.first()
        return if (first == '{' || first == '[') {
            parseJson(text, base, fallbackName)
        } else {
            parseM3u(text, base, fallbackName)
        }
    }

    internal fun decode(bytes: ByteArray): String {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16BE)
        }
        return try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (_: CharacterCodingException) {
            String(bytes, windows1252)
        }
    }

    private class Builder(private val base: HttpUrl?) {
        val entries = ArrayList<ParsedCollectionEntry>()
        val issues = ArrayList<CollectionParseIssue>()
        val credentials = LinkedHashMap<String, CollectionCredential>()
        private val seenKeys = HashSet<String>()

        val full: Boolean get() = entries.size >= MAX_ENTRIES

        fun resolveUrl(raw: String, position: Int, reportIssues: Boolean = true): String? {
            val value = raw.trim()
            if (value.isEmpty()) {
                if (reportIssues) issues += CollectionParseIssue(CollectionIssueKind.MissingUrl, position)
                return null
            }
            if (value.length > MAX_URL_LENGTH) {
                if (reportIssues) issues += CollectionParseIssue(CollectionIssueKind.InvalidUrl, position)
                return null
            }
            val hasScheme = schemePattern.containsMatchIn(value)
            val resolved = when {
                hasScheme -> {
                    val scheme = value.substringBefore(':').lowercase(Locale.ROOT)
                    if (scheme != "http" && scheme != "https") {
                        if (reportIssues) issues += CollectionParseIssue(CollectionIssueKind.UnsupportedScheme, position)
                        return null
                    }
                    value.toHttpUrlOrNull()
                }
                value.startsWith("//") -> base?.let { "${it.scheme}:$value".toHttpUrlOrNull() }
                base == null -> {
                    if (reportIssues) issues += CollectionParseIssue(CollectionIssueKind.RelativeWithoutBase, position)
                    return null
                }
                else -> base.resolve(value.replace('\\', '/'))
            }
            if (resolved == null || resolved.host.isBlank()) {
                if (reportIssues) issues += CollectionParseIssue(CollectionIssueKind.InvalidUrl, position)
                return null
            }
            rememberCredentials(resolved, position, reportIssues)
            return resolved.withoutCredentials().newBuilder().fragment(null).build().toString()
        }

        fun rememberCredentials(resolved: HttpUrl, position: Int, reportIssues: Boolean = true) {
            if (resolved.username.isEmpty() && resolved.password.isEmpty()) return
            val origin = PersonalCollectionUrlPolicy.origin(resolved)
            if (resolved.username.isNotEmpty() && origin !in credentials) {
                credentials[origin] = CollectionCredential(origin, resolved.username, resolved.password)
            }
            if (reportIssues) issues += CollectionParseIssue(CollectionIssueKind.CredentialsRemoved, position)
        }

        fun add(entry: ParsedCollectionEntry, position: Int) {
            if (full) {
                if (issues.none { it.kind == CollectionIssueKind.Truncated }) {
                    issues += CollectionParseIssue(CollectionIssueKind.Truncated, position)
                }
                return
            }
            if (!seenKeys.add(entry.entryKey)) {
                issues += CollectionParseIssue(CollectionIssueKind.Duplicate, position)
                return
            }
            entries += entry
        }
    }

    private fun parseM3u(text: String, base: HttpUrl?, fallbackName: String): CollectionParseResult {
        val lines = text.lineSequence().map { it.trim() }.toList()
        if (lines.any { line -> hlsTags.any { tag -> line.startsWith(tag, ignoreCase = true) } }) {
            return CollectionParseResult.Failure(CollectionParseError.HlsManifest)
        }
        val builder = Builder(base)
        var name = ""
        var playlistArtwork = ""
        var pendingTitle = ""
        var pendingArtist = ""
        var pendingAlbum = ""
        var pendingArtwork = ""
        var pendingDurationMs = 0L
        var pendingMetadata = false
        var position = 0
        var sawEntryLine = false
        var sawDirective = false
        for (line in lines) {
            if (line.isEmpty()) continue
            if (line.startsWith("#")) {
                sawDirective = true
                val upper = line.uppercase(Locale.ROOT)
                when {
                    upper.startsWith("#EXTINF:") -> {
                        val info = parseExtInf(line.substring(8))
                        pendingDurationMs = info.durationMs
                        pendingTitle = info.title
                        pendingArtist = info.artist
                        info.artwork.takeIf(String::isNotBlank)?.let { pendingArtwork = it }
                        info.album.takeIf(String::isNotBlank)?.let { pendingAlbum = it }
                        pendingMetadata = true
                    }
                    upper.startsWith("#PLAYLIST:") -> name = cleanText(line.substring(10))
                    upper.startsWith("#EXTALB:") -> pendingAlbum = cleanText(line.substring(8))
                    upper.startsWith("#EXTART:") -> pendingArtist = cleanText(line.substring(8))
                    upper.startsWith("#EXTIMG:") -> {
                        val image = line.substring(8).trim()
                        if (!sawEntryLine && !pendingMetadata && playlistArtwork.isEmpty()) {
                            playlistArtwork = builder.resolveUrl(image, position, reportIssues = false).orEmpty()
                        } else {
                            pendingArtwork = image
                        }
                    }
                }
                continue
            }
            sawEntryLine = true
            val audioUrl = builder.resolveUrl(line, position)
            if (audioUrl != null) {
                val title = pendingTitle.ifBlank { titleFromUrl(audioUrl) }
                builder.add(
                    ParsedCollectionEntry(
                        entryKey = "u${stableHash(PersonalCollectionUrlPolicy.identityOf(audioUrl))}",
                        title = title,
                        artist = pendingArtist,
                        album = pendingAlbum,
                        albumArtist = "",
                        durationMs = pendingDurationMs,
                        audioUrl = audioUrl,
                        videoUrl = "",
                        artworkUrl = pendingArtwork.takeIf(String::isNotBlank)
                            ?.let { builder.resolveUrl(it, position, reportIssues = false) }
                            .orEmpty(),
                        trackNumber = 0,
                        discNumber = 0,
                        year = 0
                    ),
                    position
                )
            }
            position++
            pendingTitle = ""
            pendingArtist = ""
            pendingAlbum = ""
            pendingArtwork = ""
            pendingDurationMs = 0L
            pendingMetadata = false
        }
        if (!sawEntryLine && !sawDirective) return CollectionParseResult.Failure(CollectionParseError.Malformed)
        if (builder.entries.isEmpty()) return CollectionParseResult.Failure(CollectionParseError.NoPlayableEntries)
        return CollectionParseResult.Success(
            ParsedCollection(
                format = PersonalCollectionFormat.M3U,
                name = name.ifBlank { fallbackName },
                artworkUrl = playlistArtwork,
                entries = builder.entries,
                issues = builder.issues,
                credentials = builder.credentials.values.toList()
            )
        )
    }

    private data class ExtInf(
        val durationMs: Long,
        val title: String,
        val artist: String,
        val album: String,
        val artwork: String
    )

    private fun parseExtInf(value: String): ExtInf {
        val separator = commaOutsideQuotes(value)
        val head = if (separator >= 0) value.substring(0, separator) else value
        val display = if (separator >= 0) value.substring(separator + 1).trim() else ""
        val seconds = leadingDuration.find(head)?.groupValues?.get(1)?.toDoubleOrNull() ?: -1.0
        val attributes = extInfAttribute.findAll(head).associate {
            it.groupValues[1].lowercase(Locale.ROOT) to it.groupValues[2].trim()
        }
        val split = display.indexOf(" - ")
        val artist = if (split > 0) cleanText(display.substring(0, split)) else ""
        val title = cleanText(if (split > 0) display.substring(split + 3) else display)
        return ExtInf(
            durationMs = if (seconds > 0.0 && seconds < MAX_DURATION_SECONDS) (seconds * 1000.0).toLong() else 0L,
            title = title,
            artist = artist.ifBlank { cleanText(attributes["artist"].orEmpty()) },
            album = cleanText(attributes["album"].orEmpty()),
            artwork = attributes["tvg-logo"] ?: attributes["logo"] ?: attributes["artwork"].orEmpty()
        )
    }

    private fun commaOutsideQuotes(value: String): Int {
        var quoted = false
        value.forEachIndexed { index, char ->
            when (char) {
                '"' -> quoted = !quoted
                ',' -> if (!quoted) return index
            }
        }
        return -1
    }

    private fun parseJson(text: String, base: HttpUrl?, fallbackName: String): CollectionParseResult {
        val root = try {
            JSONTokener(text).nextValue()
        } catch (_: JSONException) {
            return CollectionParseResult.Failure(CollectionParseError.Malformed)
        }
        val document = root as? JSONObject
        val tracks = when (root) {
            is JSONArray -> root
            is JSONObject -> root.optJSONArray("tracks") ?: root.optJSONArray("items")
            else -> null
        } ?: return CollectionParseResult.Failure(CollectionParseError.Malformed)
        val declaredBase = document?.optString("baseUrl")?.trim()?.takeIf(String::isNotEmpty)
            ?.let { declared -> base?.resolve(declared) ?: declared.toHttpUrlOrNull() }
        val builder = Builder(declaredBase?.withoutCredentials() ?: base)
        declaredBase?.let { builder.rememberCredentials(it, -1) }
        val count = tracks.length()
        for (index in 0 until count) {
            if (builder.full) {
                builder.add(EMPTY_ENTRY, index)
                break
            }
            val item = tracks.optJSONObject(index)
            if (item == null) {
                val bare = tracks.optString(index).takeIf { tracks.opt(index) is String }
                if (bare == null) {
                    builder.issues += CollectionParseIssue(CollectionIssueKind.MissingUrl, index)
                    continue
                }
                builder.resolveUrl(bare, index)?.let { url ->
                    builder.add(entryFromJson(JSONObject(), url, "", index, builder), index)
                }
                continue
            }
            val rawUrl = firstString(item, "url", "audioUrl", "streamUrl", "src", "file")
            val audioUrl = builder.resolveUrl(rawUrl, index) ?: continue
            val declaredId = firstString(item, "id", "guid")
            builder.add(entryFromJson(item, audioUrl, declaredId, index, builder), index)
        }
        if (builder.entries.isEmpty()) return CollectionParseResult.Failure(CollectionParseError.NoPlayableEntries)
        return CollectionParseResult.Success(
            ParsedCollection(
                format = PersonalCollectionFormat.JSON,
                name = cleanText(document?.optString("name").orEmpty()).ifBlank { fallbackName },
                artworkUrl = document?.let { firstString(it, "artwork", "cover", "image") }
                    ?.takeIf(String::isNotBlank)
                    ?.let { builder.resolveUrl(it, -1, reportIssues = false) }
                    .orEmpty(),
                entries = builder.entries,
                issues = builder.issues,
                credentials = builder.credentials.values.toList()
            )
        )
    }

    private fun entryFromJson(
        item: JSONObject,
        audioUrl: String,
        declaredId: String,
        index: Int,
        builder: Builder
    ): ParsedCollectionEntry {
        val durationMs = when {
            item.has("durationMs") -> item.optDouble("durationMs", 0.0)
            item.has("duration") -> item.optDouble("duration", 0.0) * 1000.0
            else -> 0.0
        }.takeIf { it > 0.0 && it < MAX_DURATION_SECONDS * 1000.0 }?.toLong() ?: 0L
        val artwork = firstString(item, "artwork", "artworkUrl", "cover", "image", "thumbnail")
        val video = firstString(item, "videoUrl", "video")
        val identity = declaredId.trim().takeIf { it.isNotEmpty() && it.length <= MAX_TEXT_LENGTH }
        return ParsedCollectionEntry(
            entryKey = if (identity != null) "i${stableHash(identity)}" else "u${stableHash(PersonalCollectionUrlPolicy.identityOf(audioUrl))}",
            title = cleanText(item.optString("title")).ifBlank { titleFromUrl(audioUrl) },
            artist = cleanText(firstString(item, "artist", "artists")),
            album = cleanText(item.optString("album")),
            albumArtist = cleanText(item.optString("albumArtist")),
            durationMs = durationMs,
            audioUrl = audioUrl,
            videoUrl = video.takeIf(String::isNotBlank)?.let { builder.resolveUrl(it, index, reportIssues = false) }.orEmpty(),
            artworkUrl = artwork.takeIf(String::isNotBlank)?.let { builder.resolveUrl(it, index, reportIssues = false) }.orEmpty(),
            trackNumber = item.optInt("trackNumber", 0).coerceIn(0, 9_999),
            discNumber = item.optInt("discNumber", 0).coerceIn(0, 999),
            year = item.optInt("year", 0).takeIf { it in 1..9_999 } ?: 0
        )
    }

    private fun firstString(item: JSONObject, vararg keys: String): String {
        for (key in keys) {
            when (val value = item.opt(key)) {
                is String -> if (value.isNotBlank()) return value
                is JSONArray -> {
                    val joined = (0 until value.length()).mapNotNull { value.opt(it) as? String }
                        .filter(String::isNotBlank)
                        .joinToString(", ")
                    if (joined.isNotBlank()) return joined
                }
            }
        }
        return ""
    }

    internal fun titleFromUrl(url: String): String {
        val segment = url.toHttpUrlOrNull()?.pathSegments?.lastOrNull { it.isNotBlank() }.orEmpty()
        return cleanText(segment.substringBeforeLast('.', segment)).ifBlank { url.toHttpUrlOrNull()?.host.orEmpty() }
    }

    internal fun cleanText(value: String): String {
        val trimmed = value.replace(whitespace, " ").trim()
        return trimmed.filterNot { it.isISOControl() }.take(MAX_TEXT_LENGTH)
    }

    private fun HttpUrl.withoutCredentials(): HttpUrl =
        if (username.isEmpty() && password.isEmpty()) this else newBuilder().username("").password("").build()

    private const val MAX_DURATION_SECONDS = 86_400.0

    private val EMPTY_ENTRY = ParsedCollectionEntry("", "", "", "", "", 0L, "", "", "", 0, 0, 0)
}

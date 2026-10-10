package com.luc4n3x.levyra.nexus.playlistimport

import java.net.URI
import java.net.URLDecoder
import java.util.Locale

enum class PlaylistTextFormat {
    M3U,
    PLS,
    XSPF,
    CSV,
    TSV,
    JSON,
    TEXT
}

enum class UnsupportedPlaylistReason {
    AUTHENTICATION_REQUIRED,
    NO_PUBLIC_ACCESS
}

sealed interface DetectedPlaylistInput {
    data class RemotePlaylist(
        val source: PlaylistImportSource,
        val id: String,
        val url: String
    ) : DetectedPlaylistInput

    data class UnsupportedRemote(
        val source: PlaylistImportSource,
        val reason: UnsupportedPlaylistReason
    ) : DetectedPlaylistInput

    data class NotAPlaylist(val source: PlaylistImportSource?) : DetectedPlaylistInput

    data class StructuredText(val format: PlaylistTextFormat) : DetectedPlaylistInput

    data object Unrecognized : DetectedPlaylistInput
}

object PlaylistInputDetector {
    private val youtubeListId = Regex("^[A-Za-z0-9_-]{2,80}$")
    private val youtubeVideoId = Regex("^[A-Za-z0-9_-]{11}$")
    private val spotifyId = Regex("^[A-Za-z0-9]{22}$")
    private val spotifyUri = Regex("^spotify:(user:[^:]+:)?playlist:([A-Za-z0-9]{22})$")
    private val applePlaylistId = Regex("^pl\\.[A-Za-z0-9.-]{4,80}$")
    private val numericId = Regex("^\\d{1,20}$")
    private val jioSaavnToken = Regex("^[A-Za-z0-9,_-]{6,80}$")
    private val bandcampHost = Regex("^[a-z0-9-]{1,63}\\.bandcamp\\.com$")
    private val urlToken = Regex("https?://\\S+", RegexOption.IGNORE_CASE)

    private val youtubeHosts = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "youtu.be")
    private val spotifyHosts = setOf("open.spotify.com", "play.spotify.com")
    private val spotifyShortHosts = setOf("spotify.link")
    private val deezerHosts = setOf("deezer.com", "www.deezer.com")
    private val deezerShortHosts = setOf("link.deezer.com")
    private val tidalHosts = setOf("tidal.com", "www.tidal.com", "listen.tidal.com")
    private val soundCloudHosts = setOf("soundcloud.com", "www.soundcloud.com", "m.soundcloud.com", "on.soundcloud.com")
    private val jioSaavnHosts = setOf("jiosaavn.com", "www.jiosaavn.com")

    fun detect(input: String, fileName: String? = null): DetectedPlaylistInput {
        val trimmed = input.trim().removePrefix("﻿")
        if (trimmed.isEmpty()) return DetectedPlaylistInput.Unrecognized
        formatFromFileName(fileName)?.let { return DetectedPlaylistInput.StructuredText(it) }
        spotifyUri.matchEntire(trimmed)?.let { match ->
            val id = match.groupValues[2]
            return DetectedPlaylistInput.RemotePlaylist(PlaylistImportSource.SPOTIFY, id, "https://open.spotify.com/playlist/$id")
        }
        if (trimmed.startsWith("spotify:track:") || trimmed.startsWith("spotify:album:")) {
            return DetectedPlaylistInput.NotAPlaylist(PlaylistImportSource.SPOTIFY)
        }
        val structured = trimmed.first() in "<{[#"
        if (!structured && !trimmed.contains('\n') && urlToken.containsMatchIn(trimmed)) {
            val url = urlToken.find(trimmed)?.value.orEmpty().trimEnd('.', ',', ')', ']', '>', '"', '\'')
            return detectUrl(url)
        }
        sniffFormat(trimmed)?.let { return DetectedPlaylistInput.StructuredText(it) }
        return DetectedPlaylistInput.Unrecognized
    }

    fun detectUrl(rawUrl: String): DetectedPlaylistInput {
        val uri = parseSafeUri(rawUrl) ?: return DetectedPlaylistInput.Unrecognized
        val host = uri.host.lowercase(Locale.ROOT).trimEnd('.')
        val segments = uri.rawPath.orEmpty().split('/').filter { it.isNotBlank() }.map(::decode)
        val https = "https://$host${uri.rawPath.orEmpty()}${uri.rawQuery?.let { "?$it" }.orEmpty()}"
        return when {
            host in youtubeHosts -> detectYoutube(host, uri, segments)
            host in spotifyHosts -> detectSpotify(segments)
            host in spotifyShortHosts && segments.isNotEmpty() ->
                DetectedPlaylistInput.RemotePlaylist(PlaylistImportSource.SPOTIFY, "", https)
            host == "music.apple.com" -> detectApple(segments)
            host in deezerHosts -> detectDeezer(segments)
            host in deezerShortHosts && segments.isNotEmpty() ->
                DetectedPlaylistInput.RemotePlaylist(PlaylistImportSource.DEEZER, "", https)
            else -> detectOtherHost(host, segments, https)
        }
    }

    private fun detectOtherHost(host: String, segments: List<String>, https: String): DetectedPlaylistInput = when {
        host in tidalHosts -> accountOnly(
            PlaylistImportSource.TIDAL,
            segments.contains("playlist") || segments.contains("mix"),
            UnsupportedPlaylistReason.AUTHENTICATION_REQUIRED
        )
        host in soundCloudHosts -> accountOnly(
            PlaylistImportSource.SOUNDCLOUD,
            host == "on.soundcloud.com" || segments.getOrNull(1) == "sets",
            UnsupportedPlaylistReason.AUTHENTICATION_REQUIRED
        )
        host in jioSaavnHosts -> detectJioSaavn(segments, https)
        host.startsWith("music.amazon.") -> accountOnly(
            PlaylistImportSource.AMAZON_MUSIC,
            segments.any { it == "playlists" || it == "user-playlists" },
            UnsupportedPlaylistReason.NO_PUBLIC_ACCESS
        )
        bandcampHost.matches(host) -> detectBandcamp(host, segments)
        else -> DetectedPlaylistInput.Unrecognized
    }

    private fun accountOnly(
        source: PlaylistImportSource,
        playlist: Boolean,
        reason: UnsupportedPlaylistReason
    ): DetectedPlaylistInput =
        if (playlist) DetectedPlaylistInput.UnsupportedRemote(source, reason) else DetectedPlaylistInput.NotAPlaylist(source)

    private fun detectBandcamp(host: String, segments: List<String>): DetectedPlaylistInput {
        if (segments.firstOrNull() != "album" || segments.size < 2) {
            return DetectedPlaylistInput.NotAPlaylist(PlaylistImportSource.BANDCAMP)
        }
        val slug = safeSegment(segments[1])
        return DetectedPlaylistInput.RemotePlaylist(PlaylistImportSource.BANDCAMP, "$host/album/$slug", "https://$host/album/$slug")
    }

    private fun parseSafeUri(rawUrl: String): URI? {
        val uri = runCatching { URI(rawUrl.trim().replace(" ", "%20")) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        val schemeAllowed = scheme == "https" || scheme == "http"
        val portAllowed = uri.port == -1 || uri.port == 443 || uri.port == 80
        return uri.takeIf { schemeAllowed && portAllowed && it.rawUserInfo == null && it.host != null }
    }

    fun formatFromFileName(fileName: String?): PlaylistTextFormat? {
        val extension = fileName?.substringAfterLast('.', "")?.lowercase(Locale.ROOT).orEmpty()
        return when (extension) {
            "m3u", "m3u8" -> PlaylistTextFormat.M3U
            "pls" -> PlaylistTextFormat.PLS
            "xspf" -> PlaylistTextFormat.XSPF
            "csv" -> PlaylistTextFormat.CSV
            "tsv", "tab" -> PlaylistTextFormat.TSV
            "json" -> PlaylistTextFormat.JSON
            "txt" -> PlaylistTextFormat.TEXT
            else -> null
        }
    }

    fun sniffFormat(text: String): PlaylistTextFormat? {
        val head = text.trimStart().take(4_096)
        val firstLine = head.lineSequence().firstOrNull().orEmpty().trim()
        return when {
            head.startsWith("#EXTM3U", ignoreCase = true) -> PlaylistTextFormat.M3U
            firstLine.equals("[playlist]", ignoreCase = true) -> PlaylistTextFormat.PLS
            head.contains("<playlist", ignoreCase = true) && head.contains("xspf", ignoreCase = true) -> PlaylistTextFormat.XSPF
            head.startsWith("{") || head.startsWith("[") -> PlaylistTextFormat.JSON
            PlaylistTextParsers.looksLikeHeader(firstLine, '\t') -> PlaylistTextFormat.TSV
            PlaylistTextParsers.looksLikeHeader(firstLine, ',') -> PlaylistTextFormat.CSV
            PlaylistTextParsers.looksLikeHeader(firstLine, ';') -> PlaylistTextFormat.CSV
            head.lineSequence().any { it.trim().startsWith("#EXTINF", ignoreCase = true) } -> PlaylistTextFormat.M3U
            PlaylistTextParsers.looksLikeTrackList(text) -> PlaylistTextFormat.TEXT
            else -> null
        }
    }

    private fun detectYoutube(host: String, uri: URI, segments: List<String>): DetectedPlaylistInput {
        val listId = queryParameter(uri, "list")
        val videoId = if (host == "youtu.be") segments.firstOrNull().orEmpty() else queryParameter(uri, "v")
        val source = if (host == "music.youtube.com") PlaylistImportSource.YOUTUBE_MUSIC else PlaylistImportSource.YOUTUBE
        if (listId.isNotBlank() && youtubeListId.matches(listId)) {
            if (listId.startsWith("RD")) return DetectedPlaylistInput.NotAPlaylist(source)
            return DetectedPlaylistInput.RemotePlaylist(source, listId, "https://music.youtube.com/playlist?list=$listId")
        }
        val browseId = if (segments.firstOrNull() == "browse") segments.getOrNull(1).orEmpty() else ""
        if (browseId.startsWith("VL") && youtubeListId.matches(browseId.removePrefix("VL"))) {
            val id = browseId.removePrefix("VL")
            return DetectedPlaylistInput.RemotePlaylist(source, id, "https://music.youtube.com/playlist?list=$id")
        }
        return if (youtubeVideoId.matches(videoId) || segments.isNotEmpty()) {
            DetectedPlaylistInput.NotAPlaylist(source)
        } else {
            DetectedPlaylistInput.Unrecognized
        }
    }

    private fun detectSpotify(rawSegments: List<String>): DetectedPlaylistInput {
        val segments = rawSegments.dropWhile { it.startsWith("intl-") || it == "embed" }
        val kind = segments.getOrNull(0).orEmpty()
        val id = if (kind == "user" && segments.getOrNull(2) == "playlist") {
            segments.getOrNull(3).orEmpty()
        } else {
            segments.getOrNull(1).orEmpty()
        }
        return when {
            (kind == "playlist" || kind == "user") && spotifyId.matches(id) ->
                DetectedPlaylistInput.RemotePlaylist(PlaylistImportSource.SPOTIFY, id, "https://open.spotify.com/playlist/$id")
            kind in setOf("track", "album", "artist", "episode", "show") ->
                DetectedPlaylistInput.NotAPlaylist(PlaylistImportSource.SPOTIFY)
            else -> DetectedPlaylistInput.Unrecognized
        }
    }

    private fun detectApple(segments: List<String>): DetectedPlaylistInput {
        val kindIndex = segments.indexOfFirst { it in setOf("playlist", "album", "song", "artist", "music-video") }
        if (kindIndex < 0) return DetectedPlaylistInput.Unrecognized
        if (segments[kindIndex] != "playlist") return DetectedPlaylistInput.NotAPlaylist(PlaylistImportSource.APPLE_MUSIC)
        val id = segments.drop(kindIndex + 1).lastOrNull { applePlaylistId.matches(it) }
            ?: return DetectedPlaylistInput.Unrecognized
        val storefront = segments.getOrNull(0)?.takeIf { it.length == 2 && it.all(Char::isLetter) }?.lowercase(Locale.ROOT) ?: "us"
        val slug = segments.getOrNull(kindIndex + 1)?.takeIf { it != id }?.let(::safeSegment) ?: "playlist"
        return DetectedPlaylistInput.RemotePlaylist(
            PlaylistImportSource.APPLE_MUSIC,
            id,
            "https://music.apple.com/$storefront/playlist/$slug/$id"
        )
    }

    private fun detectDeezer(segments: List<String>): DetectedPlaylistInput {
        val kindIndex = segments.indexOfFirst { it in setOf("playlist", "track", "album", "artist") }
        if (kindIndex < 0) return DetectedPlaylistInput.Unrecognized
        if (segments[kindIndex] != "playlist") return DetectedPlaylistInput.NotAPlaylist(PlaylistImportSource.DEEZER)
        val id = segments.getOrNull(kindIndex + 1).orEmpty()
        if (!numericId.matches(id)) return DetectedPlaylistInput.Unrecognized
        return DetectedPlaylistInput.RemotePlaylist(PlaylistImportSource.DEEZER, id, "https://www.deezer.com/playlist/$id")
    }

    private fun detectJioSaavn(segments: List<String>, url: String): DetectedPlaylistInput {
        val isPlaylist = segments.firstOrNull() == "featured" ||
            (segments.firstOrNull() == "s" && segments.getOrNull(1) == "playlist")
        if (!isPlaylist) {
            return if (segments.firstOrNull() in setOf("song", "album", "artist")) {
                DetectedPlaylistInput.NotAPlaylist(PlaylistImportSource.JIOSAAVN)
            } else {
                DetectedPlaylistInput.Unrecognized
            }
        }
        val playlistToken = segments.lastOrNull().orEmpty()
        if (!jioSaavnToken.matches(playlistToken)) return DetectedPlaylistInput.Unrecognized
        return DetectedPlaylistInput.RemotePlaylist(PlaylistImportSource.JIOSAAVN, playlistToken, url)
    }

    private fun queryParameter(uri: URI, name: String): String {
        val raw = uri.rawQuery.orEmpty()
        if (raw.isBlank()) return ""
        return raw.split('&').firstNotNullOfOrNull { part ->
            val separator = part.indexOf('=')
            if (separator > 0 && part.substring(0, separator) == name) decode(part.substring(separator + 1)) else null
        }.orEmpty().trim()
    }

    private fun decode(value: String): String =
        runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)

    private fun safeSegment(value: String): String =
        value.filter { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' }.ifBlank { "playlist" }
}

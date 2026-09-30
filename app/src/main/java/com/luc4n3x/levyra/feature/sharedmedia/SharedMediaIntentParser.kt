package com.luc4n3x.levyra.feature.sharedmedia

import android.content.Intent
import android.net.Uri
import com.luc4n3x.levyra.nexus.playlistimport.DetectedPlaylistInput
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistInputDetector

object SharedMediaIntentParser {
    private val videoIdRegex = Regex("^[A-Za-z0-9_-]{6,20}$")

    fun parse(intent: Intent?): SharedMediaRequest? {
        intent ?: return null
        playlistFileRequest(intent)?.let { return it }
        val candidates = buildList {
            intent.dataString?.let(::add)
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let(::add)
            intent.clipData?.let { clip ->
                for (index in 0 until clip.itemCount) {
                    clip.getItemAt(index).uri?.toString()?.let(::add)
                    clip.getItemAt(index).text?.toString()?.let(::add)
                }
            }
        }
        val sharedUrls = candidates.flatMap(BulkLinkCapture::extractUrls).distinct()
        if (sharedUrls.size > 1 && candidates.none { LevyraPlaylistShareCodec.extractPayload(it) != null }) {
            BulkLinkCapture.request(sharedUrls, ::parseText)
                .takeIf { it.bulkUrls.size > 1 }
                ?.let { return it }
        }
        return candidates.asSequence().mapNotNull(::parseText).firstOrNull()
    }

    fun parseText(rawText: String): SharedMediaRequest? {
        val cleanText = rawText.trim().take(LevyraPlaylistShareCodec.MAX_ENCODED_CHARS + 1_024)
        if (cleanText.isBlank()) return null
        val sharedPlaylistPayload = LevyraPlaylistShareCodec.extractPayload(cleanText)
        if (sharedPlaylistPayload != null) {
            return SharedMediaRequest(
                rawText = "",
                url = "",
                kind = SharedMediaKind.LevyraPlaylist,
                sharedPlaylistPayload = sharedPlaylistPayload
            )
        }
        val urls = BulkLinkCapture.extractUrls(cleanText)
        if (urls.size > 1) return BulkLinkCapture.request(urls, ::parseText)
        val rawUrl = urls.firstOrNull()
        if (rawUrl == null) {
            return SharedMediaRequest(
                rawText = cleanText,
                url = "",
                kind = SharedMediaKind.Search,
                query = cleanText.take(300)
            )
        }
        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return null
        val host = uri.host.orEmpty().lowercase().removePrefix("www.")
        if (host !in supportedHosts) {
            val external = PlaylistInputDetector.detectUrl(rawUrl)
            val kind = if (
                external is DetectedPlaylistInput.RemotePlaylist || external is DetectedPlaylistInput.UnsupportedRemote
            ) {
                SharedMediaKind.ExternalPlaylist
            } else {
                SharedMediaKind.Unsupported
            }
            return SharedMediaRequest(rawText = cleanText, url = rawUrl, kind = kind)
        }
        val segments = uri.pathSegments.filter { it.isNotBlank() }
        val playlistId = uri.getQueryParameter("list").orEmpty().trim()
        val videoId = extractVideoId(host, uri, segments)
        val browseId = extractBrowseId(segments)
        val kind = when {
            browseId.startsWith("MPRE", ignoreCase = true) -> SharedMediaKind.Album
            segments.firstOrNull().equals("artist", ignoreCase = true) -> SharedMediaKind.Artist
            segments.firstOrNull().equals("channel", ignoreCase = true) || segments.firstOrNull().equals("c", ignoreCase = true) || segments.firstOrNull().orEmpty().startsWith("@") -> SharedMediaKind.Channel
            videoId.isNotBlank() -> SharedMediaKind.Video
            playlistId.isNotBlank() -> SharedMediaKind.Playlist
            browseId.isNotBlank() -> SharedMediaKind.Artist
            else -> SharedMediaKind.Search
        }
        return SharedMediaRequest(
            rawText = cleanText,
            url = normalizeUrl(host, videoId, playlistId, browseId, rawUrl),
            kind = kind,
            videoId = videoId,
            playlistId = playlistId,
            browseId = browseId,
            query = cleanText.replace(rawUrl, " ").replace(Regex("\\s+"), " ").trim().take(300)
        )
    }

    internal fun playlistFileRequest(intent: Intent): SharedMediaRequest? {
        if (intent.action != Intent.ACTION_VIEW) return null
        val data = intent.data ?: return null
        if (!data.scheme.equals("content", ignoreCase = true)) return null
        val mime = intent.type?.lowercase().orEmpty()
        val extension = data.lastPathSegment.orEmpty().substringAfterLast('.', "").lowercase()
        if (mime !in playlistFileMimeTypes && extension !in playlistFileExtensions) return null
        return SharedMediaRequest(rawText = "", url = data.toString(), kind = SharedMediaKind.PlaylistFile)
    }

    private val playlistFileMimeTypes = setOf(
        "audio/x-mpegurl", "audio/mpegurl", "application/vnd.apple.mpegurl", "application/x-mpegurl",
        "audio/x-scpls", "application/xspf+xml"
    )

    private val playlistFileExtensions = setOf("m3u", "m3u8", "pls", "xspf")

    private fun extractVideoId(host: String, uri: Uri, segments: List<String>): String {
        val candidate = when {
            host == "youtu.be" -> segments.firstOrNull().orEmpty()
            segments.firstOrNull() in setOf("shorts", "live", "embed") -> segments.getOrNull(1).orEmpty()
            else -> uri.getQueryParameter("v").orEmpty()
        }
        return candidate.takeIf { videoIdRegex.matches(it) }.orEmpty()
    }

    private fun extractBrowseId(segments: List<String>): String {
        val first = segments.firstOrNull().orEmpty()
        return when {
            first.equals("browse", ignoreCase = true) -> segments.getOrNull(1).orEmpty()
            first.equals("channel", ignoreCase = true) -> segments.getOrNull(1).orEmpty()
            first.equals("artist", ignoreCase = true) -> segments.getOrNull(1).orEmpty()
            first.startsWith("@") -> first
            else -> ""
        }
    }

    private fun normalizeUrl(host: String, videoId: String, playlistId: String, browseId: String, fallback: String): String {
        return when {
            videoId.isNotBlank() -> "https://www.youtube.com/watch?v=$videoId"
            playlistId.isNotBlank() -> "https://music.youtube.com/playlist?list=$playlistId"
            browseId.isNotBlank() -> "https://music.youtube.com/browse/$browseId"
            host == "youtu.be" -> fallback.replace("http://", "https://")
            else -> fallback.replace("http://", "https://")
        }
    }

    private val supportedHosts = setOf(
        "youtube.com",
        "m.youtube.com",
        "music.youtube.com",
        "youtu.be",
        "youtube-nocookie.com"
    )
}

package com.luc4n3x.levyra.feature.sharedmedia

import android.content.Intent
import android.net.Uri

object SharedMediaIntentParser {
    private val urlRegex = Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE)
    private val videoIdRegex = Regex("^[A-Za-z0-9_-]{6,20}$")

    fun parse(intent: Intent?): SharedMediaRequest? {
        intent ?: return null
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
        return candidates.asSequence().mapNotNull(::parseText).firstOrNull()
    }

    fun parseText(rawText: String): SharedMediaRequest? {
        val cleanText = rawText.trim().take(8_192)
        if (cleanText.isBlank()) return null
        val rawUrl = urlRegex.find(cleanText)?.value?.trimEnd('.', ',', ';', ')', ']', '}')
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
            return SharedMediaRequest(rawText = cleanText, url = rawUrl, kind = SharedMediaKind.Unsupported)
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

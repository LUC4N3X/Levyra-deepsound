package com.luc4n3x.levyra.data

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal object YoutubeStreamCapability {
    fun servesCompleteStream(url: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase()
        if (!isTrustedGoogleVideoMedia(url)) return true
        if (queryParameter(lower, "ratebypass") == "yes") return true
        return !queryParameter(lower, "pot").isNullOrBlank()
    }

    fun isTrustedGoogleVideoMedia(url: String): Boolean {
        val parsed = url.toHttpUrlOrNull() ?: return false
        if (parsed.scheme != "https" || parsed.port != 443) return false
        if (parsed.username.isNotEmpty() || parsed.password.isNotEmpty()) return false
        val host = parsed.host.lowercase()
        if (host != "googlevideo.com" && !host.endsWith(".googlevideo.com")) return false
        if (isManifestUrl(url.lowercase())) return false
        return parsed.encodedPath.split('/').any { it == "videoplayback" }
    }

    private fun isManifestUrl(lowerUrl: String): Boolean {
        val clean = lowerUrl.substringBefore('#')
        val path = clean.substringBefore('?')
        return path.endsWith(".m3u8") ||
            path.endsWith(".mpd") ||
            path.contains("/hls_playlist") ||
            path.contains("/manifest/hls") ||
            clean.contains("mime=application%2fx-mpegurl") ||
            clean.contains("mime=application/vnd.apple.mpegurl") ||
            clean.contains("type=application%2fx-mpegurl")
    }

    private fun queryParameter(url: String, name: String): String? {
        val query = url.substringAfter('?', "").takeIf { it.isNotEmpty() } ?: return null
        return query.split('&')
            .firstOrNull { it.startsWith("$name=") }
            ?.substringAfter('=')
            ?.takeIf { it.isNotEmpty() }
    }
}

package com.luc4n3x.levyra.data

internal object YoutubeStreamCapability {
    fun servesCompleteStream(url: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase()
        if (!isGoogleVideoMedia(lower)) return true
        if (queryParameter(lower, "ratebypass") == "yes") return true
        return !queryParameter(lower, "pot").isNullOrBlank()
    }

    private fun isGoogleVideoMedia(lowerUrl: String): Boolean {
        if (!lowerUrl.startsWith("https://")) return false
        val authority = lowerUrl.substringAfter("https://").substringBefore('/')
        val host = authority.substringBefore(':')
        if (host != "googlevideo.com" && !host.endsWith(".googlevideo.com")) return false
        if (isManifestUrl(lowerUrl)) return false
        val path = lowerUrl.substringAfter("https://$authority").substringBefore('?').substringBefore('#')
        return path.contains("videoplayback")
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

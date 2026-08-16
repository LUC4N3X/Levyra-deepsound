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
        val host = lowerUrl.substringAfter("https://").substringBefore('/').substringBefore(':')
        if (!host.endsWith("googlevideo.com")) return false
        val path = lowerUrl.substringAfter(host).substringBefore('?')
        if (path.endsWith(".m3u8") || path.endsWith(".mpd")) return false
        return path.contains("videoplayback")
    }

    private fun queryParameter(url: String, name: String): String? {
        val query = url.substringAfter('?', "").takeIf { it.isNotEmpty() } ?: return null
        return query.split('&')
            .firstOrNull { it.startsWith("$name=") }
            ?.substringAfter('=')
            ?.takeIf { it.isNotEmpty() }
    }
}

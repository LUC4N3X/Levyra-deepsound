package com.luc4n3x.levyra.player.waveseek

import java.net.URI
import java.util.Locale

internal object WaveSeekSourcePolicy {
    private val adaptiveQueryMarkers = listOf(
        "mime=application%2fx-mpegurl",
        "mime=application/x-mpegurl",
        "mime=application/vnd.apple.mpegurl",
        "type=application%2fx-mpegurl",
        "type=application/x-mpegurl",
        "mime=application%2fdash+xml",
        "mime=application/dash+xml",
        "type=application%2fdash+xml",
        "type=application/dash+xml"
    )

    fun canAnalyze(source: String, durationMs: Long): Boolean {
        if (durationMs <= 0L) return false
        val clean = source.trim()
        if (clean.isBlank()) return false
        val uri = runCatching { URI(clean) }.getOrNull() ?: return false
        return when (uri.scheme.orEmpty().lowercase(Locale.ROOT)) {
            "content", "file", "levyra-cache", "levyra-sabr" -> true
            "http", "https" -> isDirectMediaUri(uri)
            else -> false
        }
    }

    private fun isDirectMediaUri(uri: URI): Boolean {
        val path = uri.path.orEmpty().lowercase(Locale.ROOT)
        if (path.endsWith(".m3u8") || path.endsWith(".mpd")) return false
        if (path.contains("/hls_playlist") || path.contains("/manifest/hls")) return false
        val queries = listOf(uri.rawQuery, uri.query)
            .filterNotNull()
            .map { it.lowercase(Locale.ROOT) }
        return queries.none { query -> adaptiveQueryMarkers.any(query::contains) }
    }
}

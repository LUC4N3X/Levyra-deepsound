package com.luc4n3x.levyra.data.hqaudio.jiosaavn

import com.luc4n3x.levyra.data.hqaudio.AudioQualityTier
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal const val JIOSAAVN_PROVIDER_ID = "jiosaavn"

internal object JioSaavnEndpoints {
    const val API_HOST = "www.jiosaavn.com"
    const val OPEN_MEDIA_HOST = "aac.saavncdn.com"
    private const val API_URL = "https://$API_HOST/api.php"
    private const val API_VERSION = "4"
    private const val CLIENT_CONTEXT = "web6dot0"

    fun search(query: String, limit: Int): String = call("search.getResults")
        .addQueryParameter("q", query)
        .addQueryParameter("n", limit.toString())
        .addQueryParameter("p", "1")
        .build()
        .toString()

    fun songDetails(providerTrackId: String): String = call("song.getDetails")
        .addQueryParameter("pids", providerTrackId)
        .build()
        .toString()

    fun authorizeMedia(mediaToken: String, tier: AudioQualityTier): String = call("song.generateAuthToken")
        .addQueryParameter("url", mediaToken)
        .addQueryParameter("bitrate", tier.kbps.toString())
        .build()
        .toString()

    private fun call(name: String): HttpUrl.Builder = API_URL.toHttpUrl()
        .newBuilder()
        .addQueryParameter("__call", name)
        .addQueryParameter("_format", "json")
        .addQueryParameter("_marker", "0")
        .addQueryParameter("api_version", API_VERSION)
        .addQueryParameter("ctx", CLIENT_CONTEXT)
}

internal data class JioSaavnMediaLocation(
    val authorizedUrl: String,
    val authorizedTier: AudioQualityTier?,
    val authorizedExpiresAtMs: Long?,
    val directory: String,
    val stem: String,
    val extension: String
) {
    fun openUrl(tier: AudioQualityTier): String =
        "https://${JioSaavnEndpoints.OPEN_MEDIA_HOST}$directory/${stem}_${tier.kbps}.$extension"

    companion object {
        private val mediaFile = Regex("^(.+)_(\\d{2,3})\\.(mp4|m4a)$", RegexOption.IGNORE_CASE)

        fun parse(authorizedUrl: String): JioSaavnMediaLocation? {
            val url = authorizedUrl.toHttpUrlOrNull() ?: return null
            if (!url.isHttps) return null
            val segments = url.pathSegments.filter { it.isNotBlank() }
            val file = segments.lastOrNull() ?: return null
            val match = mediaFile.matchEntire(file) ?: return null
            val directory = segments.dropLast(1).joinToString(separator = "/", prefix = "/").takeIf { it != "/" }.orEmpty()
            return JioSaavnMediaLocation(
                authorizedUrl = authorizedUrl,
                authorizedTier = match.groupValues[2].toIntOrNull()?.let(AudioQualityTier::fromKbps),
                authorizedExpiresAtMs = url.queryParameter("Expires")?.toLongOrNull()?.times(1_000L),
                directory = directory,
                stem = match.groupValues[1],
                extension = match.groupValues[3].lowercase()
            )
        }
    }
}

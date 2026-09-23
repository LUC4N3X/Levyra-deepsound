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
    val openHost: String,
    val directory: String,
    val stem: String,
    val extension: String
) {
    fun openUrl(tier: AudioQualityTier): String =
        "https://$openHost$directory/${stem}_${tier.kbps}.$extension"

    companion object {
        private const val MEDIA_CDN_DOMAIN = "saavncdn.com"
        private val mediaFile = Regex("^([A-Za-z0-9_-]+)_(\\d{2,3})\\.(mp4|m4a)$", RegexOption.IGNORE_CASE)
        private val pathPart = Regex("^[A-Za-z0-9_-]+$")
        private val hostLabel = Regex("^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$")

        fun fromMediaToken(mediaToken: String): JioSaavnMediaLocation? {
            val decoded = JioSaavnMediaToken.decode(mediaToken)?.toHttpUrlOrNull() ?: return null
            val host = mediaCdnHost(decoded) ?: return null
            return locate(decoded, host, authorizedUrl = "")?.takeIf { it.directory.isNotEmpty() }
        }

        fun parse(authorizedUrl: String): JioSaavnMediaLocation? {
            val url = authorizedUrl.toHttpUrlOrNull() ?: return null
            if (!url.isHttps) return null
            return locate(url, JioSaavnEndpoints.OPEN_MEDIA_HOST, authorizedUrl)
        }

        private fun mediaCdnHost(url: HttpUrl): String? {
            if (url.username.isNotEmpty() || url.password.isNotEmpty()) return null
            if (url.port != HttpUrl.defaultPort(url.scheme)) return null
            val host = url.host
            if (!host.endsWith(".$MEDIA_CDN_DOMAIN")) return null
            return host.takeIf { it.split('.').all(hostLabel::matches) }
        }

        private fun locate(url: HttpUrl, openHost: String, authorizedUrl: String): JioSaavnMediaLocation? {
            val segments = url.encodedPathSegments.filter { it.isNotEmpty() }
            val match = mediaFile.matchEntire(segments.lastOrNull() ?: return null) ?: return null
            val folders = segments.dropLast(1)
            if (!folders.all(pathPart::matches)) return null
            val authorized = authorizedUrl.isNotEmpty()
            return JioSaavnMediaLocation(
                authorizedUrl = authorizedUrl,
                authorizedTier = if (authorized) match.groupValues[2].toIntOrNull()?.let(AudioQualityTier::fromKbps) else null,
                authorizedExpiresAtMs = if (authorized) url.queryParameter("Expires")?.toLongOrNull()?.times(1_000L) else null,
                openHost = openHost,
                directory = folders.joinToString(separator = "") { "/$it" },
                stem = match.groupValues[1],
                extension = match.groupValues[3].lowercase()
            )
        }
    }
}

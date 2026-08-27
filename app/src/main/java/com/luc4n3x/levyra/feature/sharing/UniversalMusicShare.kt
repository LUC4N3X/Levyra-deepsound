package com.luc4n3x.levyra.feature.sharing

import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.json.JSONObject

class UniversalMusicShare {
    suspend fun resolve(track: Track): String {
        val source = listOf(track.videoUrl, track.canonicalAlbumUrl).firstOrNull(::isSupportedSource).orEmpty()
        if (source.isBlank()) return ""
        val request = Request.Builder().url(
            "https://api.song.link/v1-alpha.1/links".toHttpUrl().newBuilder()
                .addQueryParameter("url", source)
                .build()
        ).build()
        return try {
            LevyraHttpClientFactory.externalIntegrations().newCall(request).execute().use { response ->
                val body = response.body ?: return source
                val bytes = body.source().readByteArray(MAX_RESPONSE_BYTES + 1L)
                if (!response.isSuccessful || bytes.size > MAX_RESPONSE_BYTES) return source
                val pageUrl = JSONObject(bytes.toString(Charsets.UTF_8)).optString("pageUrl")
                pageUrl.takeIf(::isOdesliPageUrl).orEmpty().ifBlank { source }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            source
        }
    }

    internal fun isOdesliPageUrl(value: String): Boolean {
        val url = value.toHttpUrlOrNull() ?: return false
        return url.isHttps && url.username.isBlank() && url.password.isBlank() && url.port == 443 &&
            (url.host == "song.link" || url.host == "album.link")
    }

    internal fun isSupportedSource(value: String): Boolean {
        val url = value.toHttpUrlOrNull() ?: return false
        val host = url.host.lowercase()
        return url.isHttps && url.username.isBlank() && url.password.isBlank() && url.port == 443 &&
            host !in setOf("localhost", "127.0.0.1", "::1") && !host.all { it.isDigit() || it == '.' } &&
            MUSIC_SOURCE_HOSTS.any { host == it || host.endsWith(".$it") }
    }

    private companion object {
        const val MAX_RESPONSE_BYTES = 64 * 1024
        val MUSIC_SOURCE_HOSTS = setOf("youtube.com", "youtu.be", "spotify.com", "music.apple.com", "tidal.com", "deezer.com", "bandcamp.com", "soundcloud.com")
    }
}

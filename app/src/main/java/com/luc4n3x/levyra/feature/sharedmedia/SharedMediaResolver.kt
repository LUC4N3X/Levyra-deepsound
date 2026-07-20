package com.luc4n3x.levyra.feature.sharedmedia

import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.providers.LevyraProviderRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class SharedMediaResolver(
    private val providerRouter: LevyraProviderRouter,
    private val client: OkHttpClient = LevyraHttpClientFactory.media()
) {
    suspend fun resolve(request: SharedMediaRequest, languageCode: String): SharedMediaPreview = withContext(Dispatchers.IO) {
        when (request.kind) {
            SharedMediaKind.Video -> resolveVideo(request)
            SharedMediaKind.Playlist -> resolvePlaylist(request, languageCode)
            SharedMediaKind.Album -> resolveAlbum(request, languageCode)
            SharedMediaKind.Artist, SharedMediaKind.Channel, SharedMediaKind.Search -> resolveSearch(request, languageCode)
            SharedMediaKind.Unsupported -> SharedMediaPreview(
                request = request,
                title = "Link non supportato",
                subtitle = request.url,
                thumbnailUrl = "",
                tracks = emptyList(),
                error = "Levyra accetta link YouTube e YouTube Music"
            )
        }
    }

    private suspend fun resolveVideo(request: SharedMediaRequest): SharedMediaPreview {
        val metadata = fetchOEmbed(request.url)
        val title = metadata?.optString("title").orEmpty().trim().ifBlank { "Video YouTube" }
        val artist = metadata?.optString("author_name").orEmpty().trim().ifBlank { "YouTube" }
        val thumbnail = "https://i.ytimg.com/vi/${request.videoId}/hqdefault.jpg"
        val track = Track(
            id = request.videoId,
            title = title,
            artist = artist,
            album = "",
            durationMs = 0L,
            streamUrl = "",
            videoUrl = request.url,
            thumbnailUrl = thumbnail,
            largeThumbnailUrl = "https://i.ytimg.com/vi/${request.videoId}/maxresdefault.jpg",
            source = "Shared with Levyra",
            moodTags = setOf("shared", "youtube"),
            energy = 50,
            vocal = 50,
            replayScore = 80,
            cacheScore = 60,
            accentStart = 0xFF20E7FF.toInt(),
            accentEnd = 0xFF8E57FF.toInt()
        )
        return SharedMediaPreview(
            request = request,
            title = title,
            subtitle = artist,
            thumbnailUrl = thumbnail,
            tracks = listOf(track)
        )
    }

    private suspend fun resolvePlaylist(request: SharedMediaRequest, languageCode: String): SharedMediaPreview {
        val playlist = providerRouter.playlist(request.playlistId, languageCode, 300)
            ?: throw IOException("Playlist non disponibile")
        return SharedMediaPreview(
            request = request,
            title = playlist.title.ifBlank { "Playlist YouTube Music" },
            subtitle = listOf(playlist.author, "${playlist.tracks.size} brani").filter { it.isNotBlank() }.joinToString(" · "),
            thumbnailUrl = playlist.thumbnailUrl.ifBlank { playlist.tracks.firstOrNull()?.largeThumbnailUrl.orEmpty() },
            tracks = playlist.tracks
        )
    }

    private suspend fun resolveAlbum(request: SharedMediaRequest, languageCode: String): SharedMediaPreview {
        val album = providerRouter.albumDetail(
            album = AlbumHit(
                title = "",
                artist = "",
                year = "",
                thumbnailUrl = "",
                query = request.query,
                browseId = request.browseId,
                canonicalUrl = request.url
            ),
            languageCode = languageCode
        ) ?: throw IOException("Album non disponibile")
        return SharedMediaPreview(
            request = request,
            title = album.album.title.ifBlank { "Album YouTube Music" },
            subtitle = listOf(album.album.artist, "${album.trackCount} brani").filter { it.isNotBlank() }.joinToString(" · "),
            thumbnailUrl = album.album.thumbnailUrl.ifBlank { album.tracks.firstOrNull()?.largeThumbnailUrl.orEmpty() },
            tracks = album.tracks
        )
    }

    private suspend fun resolveSearch(request: SharedMediaRequest, languageCode: String): SharedMediaPreview {
        val query = request.query.ifBlank {
            request.browseId.removePrefix("@").replace('-', ' ').ifBlank { request.rawText }
        }
        val result = providerRouter.searchEverything(query, languageCode)
        val tracks = result.songs.ifEmpty { listOfNotNull(result.topTrack) }
        val artist = result.artists.firstOrNull()
        return SharedMediaPreview(
            request = request,
            title = artist?.name.orEmpty().ifBlank { tracks.firstOrNull()?.title.orEmpty().ifBlank { query } },
            subtitle = artist?.subscribers.orEmpty().ifBlank { tracks.firstOrNull()?.artist.orEmpty() },
            thumbnailUrl = artist?.thumbnailUrl.orEmpty().ifBlank { tracks.firstOrNull()?.largeThumbnailUrl.orEmpty() },
            tracks = tracks
        )
    }

    private fun fetchOEmbed(videoUrl: String): JSONObject? {
        val endpoint = "https://www.youtube.com/oembed".toHttpUrl().newBuilder()
            .addQueryParameter("url", videoUrl)
            .addQueryParameter("format", "json")
            .build()
        val request = Request.Builder()
            .url(endpoint)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                JSONObject(response.body.string())
            }
        }.getOrNull()
    }

    private companion object {
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/126 Mobile Safari/537.36"
    }
}

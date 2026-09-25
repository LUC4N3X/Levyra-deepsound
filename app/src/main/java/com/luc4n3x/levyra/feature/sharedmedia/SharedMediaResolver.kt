package com.luc4n3x.levyra.feature.sharedmedia

import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.providers.LevyraProviderRouter
import com.luc4n3x.levyra.player.queue.playbackQueueIdentity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import timber.log.Timber
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

class SharedMediaResolver(
    private val providerRouter: LevyraProviderRouter,
    private val client: OkHttpClient = LevyraHttpClientFactory.media(),
    private val sharedPlaylistTracks: suspend (LevyraSharedPlaylist, String) -> List<Track> = { _, _ -> emptyList() }
) {
    suspend fun resolve(request: SharedMediaRequest, languageCode: String): SharedMediaPreview = withContext(Dispatchers.IO) {
        when (request.kind) {
            SharedMediaKind.Video -> resolveVideo(request)
            SharedMediaKind.Playlist -> resolvePlaylist(request, languageCode)
            SharedMediaKind.Album -> resolveAlbum(request, languageCode)
            SharedMediaKind.LevyraPlaylist -> resolveLevyraPlaylist(request, languageCode)
            SharedMediaKind.BulkLinks -> resolveBulkLinks(request, languageCode)
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

    private suspend fun resolveLevyraPlaylist(
        request: SharedMediaRequest,
        languageCode: String
    ): SharedMediaPreview {
        val italian = languageCode.equals("it", ignoreCase = true)
        val decoded = LevyraPlaylistShareCodec.decode(request.sharedPlaylistPayload)
        if (decoded is LevyraPlaylistDecodeResult.Failure) {
            return SharedMediaPreview(
                request = request,
                title = if (italian) "Playlist Levyra non valida" else "Invalid Levyra playlist",
                subtitle = "",
                thumbnailUrl = "",
                tracks = emptyList(),
                error = sharedPlaylistErrorMessage(decoded.error, italian)
            )
        }
        val playlist = (decoded as LevyraPlaylistDecodeResult.Success).playlist
        val tracks = sharedPlaylistTracks(playlist, languageCode)
        val fallbackTitle = if (italian) "Playlist Levyra" else "Levyra playlist"
        val countLabel = if (italian) "brani" else "songs"
        return SharedMediaPreview(
            request = request,
            title = playlist.title.ifBlank { fallbackTitle },
            subtitle = if (tracks.size < playlist.tracks.size) {
                "${tracks.size}/${playlist.tracks.size} $countLabel"
            } else {
                "${tracks.size} $countLabel"
            },
            thumbnailUrl = tracks.firstOrNull()?.largeThumbnailUrl.orEmpty(),
            tracks = tracks,
            error = if (tracks.isEmpty()) {
                if (italian) "Nessun brano disponibile su questo dispositivo" else "No track is available on this device"
            } else {
                ""
            }
        )
    }

    private fun sharedPlaylistErrorMessage(error: LevyraPlaylistDecodeError, italian: Boolean): String = when (error) {
        LevyraPlaylistDecodeError.UnsupportedVersion ->
            if (italian) "Questa playlist richiede una versione più recente di Levyra" else "This playlist needs a newer Levyra version"
        LevyraPlaylistDecodeError.TooLarge ->
            if (italian) "Playlist troppo grande" else "Playlist too large"
        LevyraPlaylistDecodeError.ChecksumMismatch ->
            if (italian) "Il link condiviso è danneggiato" else "The shared link is damaged"
        LevyraPlaylistDecodeError.Empty ->
            if (italian) "La playlist condivisa è vuota" else "The shared playlist is empty"
        LevyraPlaylistDecodeError.Malformed,
        LevyraPlaylistDecodeError.NotLevyraPayload ->
            if (italian) "Link Levyra non leggibile" else "Unreadable Levyra link"
    }

    private suspend fun resolveBulkLinks(request: SharedMediaRequest, languageCode: String): SharedMediaPreview {
        val limiter = Semaphore(BulkLinkCapture.RESOLUTION_CONCURRENCY)
        val collectedTracks = AtomicInteger(0)
        val resolved = coroutineScope {
            request.bulkUrls.map { url ->
                async {
                    limiter.withPermit {
                        if (collectedTracks.get() >= BulkLinkCapture.MAX_TRACKS) return@withPermit emptyList()
                        resolveBulkLink(url, languageCode)
                            ?.take(BulkLinkCapture.MAX_TRACKS)
                            ?.also { collectedTracks.addAndGet(it.size) }
                    }
                }
            }.awaitAll()
        }
        val tracks = LinkedHashMap<String, Track>()
        var trackDuplicates = 0
        var failedLinks = 0
        resolved.forEach { linkTracks ->
            if (linkTracks == null) {
                failedLinks += 1
                return@forEach
            }
            linkTracks.forEach { track ->
                if (tracks.size >= BulkLinkCapture.MAX_TRACKS) return@forEach
                if (tracks.putIfAbsent(playbackQueueIdentity(track), track) != null) trackDuplicates += 1
            }
        }
        val summary = BulkLinkCaptureSummary(
            detectedLinks = request.bulkDetected,
            resolvedTracks = tracks.size,
            duplicates = request.bulkDuplicates + trackDuplicates,
            unrecognized = request.bulkUnrecognized + failedLinks
        )
        val first = tracks.values.firstOrNull()
        return SharedMediaPreview(
            request = request,
            title = "",
            subtitle = "",
            thumbnailUrl = first?.largeThumbnailUrl?.ifBlank { first.thumbnailUrl }.orEmpty(),
            tracks = tracks.values.toList(),
            bulkSummary = summary
        )
    }

    private suspend fun resolveBulkLink(url: String, languageCode: String): List<Track>? {
        val link = SharedMediaIntentParser.parseText(url) ?: return null
        return try {
            when (link.kind) {
                SharedMediaKind.Video -> resolveVideo(link, requireMetadata = true).tracks
                SharedMediaKind.Playlist -> resolvePlaylist(link, languageCode).tracks
                SharedMediaKind.Album -> resolveAlbum(link, languageCode).tracks
                else -> null
            }?.takeIf { it.isNotEmpty() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.d(error, "Bulk link resolution failed")
            null
        }
    }

    private suspend fun resolveVideo(request: SharedMediaRequest, requireMetadata: Boolean = false): SharedMediaPreview {
        val oEmbed = fetchOEmbed(request.url)
        if (oEmbed is OEmbedResult.Missing && requireMetadata) {
            return SharedMediaPreview(request = request, title = "", subtitle = "", thumbnailUrl = "", tracks = emptyList())
        }
        val metadata = (oEmbed as? OEmbedResult.Found)?.json
        val title = metadata?.optString("title").orEmpty().trim().ifBlank { "Video YouTube" }
        val artist = metadata?.optString("author_name").orEmpty().trim()
            .removeSuffix(TOPIC_CHANNEL_SUFFIX)
            .trim()
            .ifBlank { "YouTube" }
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

    private suspend fun fetchOEmbed(videoUrl: String): OEmbedResult {
        val endpoint = "https://www.youtube.com/oembed".toHttpUrl().newBuilder()
            .addQueryParameter("url", videoUrl)
            .addQueryParameter("format", "json")
            .build()
        val request = Request.Builder()
            .url(endpoint)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resume(OEmbedResult.Failed)
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = response.use {
                        when {
                            it.code in 400..499 -> OEmbedResult.Missing
                            !it.isSuccessful -> OEmbedResult.Failed
                            else -> runCatching { OEmbedResult.Found(JSONObject(it.body.string())) }
                                .getOrDefault(OEmbedResult.Failed)
                        }
                    }
                    if (continuation.isActive) continuation.resume(result)
                }
            })
        }
    }

    private sealed interface OEmbedResult {
        data class Found(val json: JSONObject) : OEmbedResult
        data object Missing : OEmbedResult
        data object Failed : OEmbedResult
    }

    private companion object {
        const val TOPIC_CHANNEL_SUFFIX = " - Topic"
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/126 Mobile Safari/537.36"
    }
}

package com.luc4n3x.levyra.data.playlistimport

import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.parseSpotifyPlaylistPage
import com.luc4n3x.levyra.data.spotifyHeadOnly
import com.luc4n3x.levyra.domain.PlaylistImportFailureKind
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.sharedmedia.LevyraPlaylistDecodeResult
import com.luc4n3x.levyra.feature.sharedmedia.LevyraPlaylistShareCodec
import com.luc4n3x.levyra.nexus.playlistimport.DetectedPlaylistInput
import com.luc4n3x.levyra.nexus.playlistimport.ImportedPlaylistDescriptor
import com.luc4n3x.levyra.nexus.playlistimport.ImportedTrackIdentity
import com.luc4n3x.levyra.nexus.playlistimport.IncompleteReason
import com.luc4n3x.levyra.nexus.playlistimport.MAX_PLAYLIST_IMPORT_TRACKS
import com.luc4n3x.levyra.nexus.playlistimport.ParsedPlaylist
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistImportCompleteness
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistImportSource
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistInputDetector
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistParseException
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistParseFailure
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistTextFormat
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistTextParsers
import com.luc4n3x.levyra.nexus.playlistimport.RemotePayloadException
import com.luc4n3x.levyra.nexus.playlistimport.RemotePlaylistPayloads
import com.luc4n3x.levyra.nexus.playlistimport.RemoteTrackPage
import com.luc4n3x.levyra.nexus.playlistimport.UnsupportedPlaylistReason
import com.luc4n3x.levyra.nexus.playlistimport.completenessOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

data class PlaylistImportInput(
    val text: String,
    val fileName: String? = null
)

data class PlaylistSourceResult(
    val playlist: ParsedPlaylist,
    val directTracks: Map<String, Track> = emptyMap()
)

fun interface PlaylistReadProgress {
    fun onRead(read: Int, declared: Int?)
}

interface PlaylistImportSourceAdapter {
    val sources: Set<PlaylistImportSource>

    suspend fun read(input: PlaylistImportInput, detected: DetectedPlaylistInput, progress: PlaylistReadProgress): PlaylistSourceResult
}

sealed interface PlaylistInputClassification {
    data class Supported(val source: PlaylistImportSource, val detected: DetectedPlaylistInput) : PlaylistInputClassification
    data class Unsupported(val source: PlaylistImportSource?, val kind: PlaylistImportFailureKind) : PlaylistInputClassification
    data object Empty : PlaylistInputClassification
}

fun classifyPlaylistInput(input: PlaylistImportInput): PlaylistInputClassification {
    if (input.text.isBlank()) return PlaylistInputClassification.Empty
    if (LevyraPlaylistShareCodec.extractPayload(input.text) != null) {
        return PlaylistInputClassification.Supported(PlaylistImportSource.LEVYRA, DetectedPlaylistInput.Unrecognized)
    }
    return when (val detected = PlaylistInputDetector.detect(input.text, input.fileName)) {
        is DetectedPlaylistInput.RemotePlaylist -> PlaylistInputClassification.Supported(detected.source, detected)
        is DetectedPlaylistInput.StructuredText -> PlaylistInputClassification.Supported(textSource(detected.format), detected)
        is DetectedPlaylistInput.UnsupportedRemote -> PlaylistInputClassification.Unsupported(
            detected.source,
            if (detected.reason == UnsupportedPlaylistReason.AUTHENTICATION_REQUIRED) {
                PlaylistImportFailureKind.AUTH_REQUIRED
            } else {
                PlaylistImportFailureKind.UNSUPPORTED_SOURCE
            }
        )
        is DetectedPlaylistInput.NotAPlaylist -> PlaylistInputClassification.Unsupported(detected.source, PlaylistImportFailureKind.NOT_A_PLAYLIST)
        DetectedPlaylistInput.Unrecognized -> PlaylistInputClassification.Unsupported(null, PlaylistImportFailureKind.INVALID_INPUT)
    }
}

private fun textSource(format: PlaylistTextFormat): PlaylistImportSource = when (format) {
    PlaylistTextFormat.M3U -> PlaylistImportSource.M3U
    PlaylistTextFormat.PLS -> PlaylistImportSource.PLS
    PlaylistTextFormat.XSPF -> PlaylistImportSource.XSPF
    PlaylistTextFormat.CSV -> PlaylistImportSource.CSV
    PlaylistTextFormat.TSV -> PlaylistImportSource.TSV
    PlaylistTextFormat.JSON -> PlaylistImportSource.JSON
    PlaylistTextFormat.TEXT -> PlaylistImportSource.TEXT
}

internal fun Track.toImportIdentity(position: Int): ImportedTrackIdentity = ImportedTrackIdentity(
    position = position,
    title = title,
    artists = PlaylistTextParsers.splitArtists(artist),
    album = album,
    durationMs = durationMs,
    isrc = isrc,
    explicit = if (explicit) true else null,
    artworkUrl = largeThumbnailUrl.ifBlank { thumbnailUrl },
    originalUrl = videoUrl,
    directCatalogId = id
)

internal fun PlaylistParseException.toImportException(remote: Boolean): PlaylistImportException = PlaylistImportException(
    when (failure) {
        PlaylistParseFailure.MALFORMED -> if (remote) PlaylistImportFailureKind.PROVIDER_CHANGED else PlaylistImportFailureKind.FILE_MALFORMED
        PlaylistParseFailure.TOO_LARGE -> PlaylistImportFailureKind.TOO_LARGE
        PlaylistParseFailure.NO_TRACKS -> PlaylistImportFailureKind.NO_USABLE_TRACKS
        PlaylistParseFailure.PROVIDER_CHANGED -> PlaylistImportFailureKind.PROVIDER_CHANGED
    },
    message.orEmpty(),
    this
)

class TextPlaylistAdapter : PlaylistImportSourceAdapter {
    override val sources = setOf(
        PlaylistImportSource.M3U, PlaylistImportSource.PLS, PlaylistImportSource.XSPF, PlaylistImportSource.CSV,
        PlaylistImportSource.TSV, PlaylistImportSource.JSON, PlaylistImportSource.TEXT
    )

    override suspend fun read(input: PlaylistImportInput, detected: DetectedPlaylistInput, progress: PlaylistReadProgress): PlaylistSourceResult {
        val format = (detected as? DetectedPlaylistInput.StructuredText)?.format
            ?: throw PlaylistImportException(PlaylistImportFailureKind.INVALID_INPUT, "Not structured text")
        val nameHint = input.fileName?.substringAfterLast('/')?.substringBeforeLast('.').orEmpty()
        val parsed = try {
            PlaylistTextParsers.parse(format, input.text, nameHint)
        } catch (error: PlaylistParseException) {
            throw error.toImportException(remote = false)
        }
        progress.onRead(parsed.tracks.size, parsed.tracks.size)
        return PlaylistSourceResult(parsed)
    }
}

class LevyraSharePlaylistAdapter : PlaylistImportSourceAdapter {
    override val sources = setOf(PlaylistImportSource.LEVYRA)
    private val youtubeId = Regex("^[A-Za-z0-9_-]{11}$")

    override suspend fun read(input: PlaylistImportInput, detected: DetectedPlaylistInput, progress: PlaylistReadProgress): PlaylistSourceResult {
        val payload = LevyraPlaylistShareCodec.extractPayload(input.text)
            ?: throw PlaylistImportException(PlaylistImportFailureKind.INVALID_INPUT, "No Levyra playlist payload")
        val decoded = LevyraPlaylistShareCodec.decode(payload) as? LevyraPlaylistDecodeResult.Success
            ?: throw PlaylistImportException(PlaylistImportFailureKind.FILE_MALFORMED, "Invalid Levyra playlist payload")
        val tracks = decoded.playlist.tracks.mapIndexedNotNull { index, shared ->
            if (shared.id.isBlank() && shared.title.isBlank()) return@mapIndexedNotNull null
            ImportedTrackIdentity(
                position = index,
                title = shared.title.ifBlank { shared.id },
                artists = PlaylistTextParsers.splitArtists(shared.artist),
                directCatalogId = shared.id.takeIf { youtubeId.matches(it) }.orEmpty()
            )
        }
        if (tracks.isEmpty()) throw PlaylistImportException(PlaylistImportFailureKind.NO_USABLE_TRACKS, "Empty Levyra playlist")
        progress.onRead(tracks.size, tracks.size)
        return PlaylistSourceResult(
            ParsedPlaylist(
                ImportedPlaylistDescriptor(PlaylistImportSource.LEVYRA, title = decoded.playlist.title),
                tracks.mapIndexed { index, track -> track.copy(position = index) },
                PlaylistImportCompleteness.Complete
            )
        )
    }
}

class YoutubePlaylistAdapter(
    private val repository: YoutubeMusicRepository,
    private val languageCode: () -> String
) : PlaylistImportSourceAdapter {
    override val sources = setOf(PlaylistImportSource.YOUTUBE, PlaylistImportSource.YOUTUBE_MUSIC)

    override suspend fun read(input: PlaylistImportInput, detected: DetectedPlaylistInput, progress: PlaylistReadProgress): PlaylistSourceResult {
        val remote = detected as? DetectedPlaylistInput.RemotePlaylist
            ?: throw PlaylistImportException(PlaylistImportFailureKind.INVALID_INPUT, "Not a YouTube playlist")
        val detail = repository.playlistForImport(remote.id, languageCode(), MAX_PLAYLIST_IMPORT_TRACKS + 1) { read ->
            progress.onRead(read.coerceAtMost(MAX_PLAYLIST_IMPORT_TRACKS), null)
        } ?: throw PlaylistImportException(PlaylistImportFailureKind.NOT_AVAILABLE, "YouTube playlist unavailable")
        if (detail.tracks.isEmpty()) throw PlaylistImportException(PlaylistImportFailureKind.NOT_AVAILABLE, "YouTube playlist empty")
        val capped = detail.tracks.size > MAX_PLAYLIST_IMPORT_TRACKS
        val tracks = detail.tracks.take(MAX_PLAYLIST_IMPORT_TRACKS)
        val identities = tracks.mapIndexed { index, track -> track.toImportIdentity(index) }
        val completeness = when {
            capped -> PlaylistImportCompleteness.Incomplete(identities.size, null, IncompleteReason.PAGINATION_STOPPED)
            detail.continuation.isBlank() -> PlaylistImportCompleteness.Complete
            else -> PlaylistImportCompleteness.Incomplete(identities.size, null, IncompleteReason.PAGINATION_STOPPED)
        }
        return PlaylistSourceResult(
            ParsedPlaylist(
                ImportedPlaylistDescriptor(
                    source = remote.source,
                    sourceId = remote.id,
                    title = detail.title,
                    owner = detail.author,
                    description = detail.description,
                    artworkUrl = detail.thumbnailUrl
                ),
                identities,
                completeness
            ),
            directTracks = tracks.associateBy { it.id }
        )
    }
}

class SpotifyPlaylistAdapter(private val fetcher: PlaylistImportFetcher) : PlaylistImportSourceAdapter {
    override val sources = setOf(PlaylistImportSource.SPOTIFY)

    override suspend fun read(input: PlaylistImportInput, detected: DetectedPlaylistInput, progress: PlaylistReadProgress): PlaylistSourceResult {
        val remote = detected as? DetectedPlaylistInput.RemotePlaylist
            ?: throw PlaylistImportException(PlaylistImportFailureKind.INVALID_INPUT, "Not a Spotify playlist")
        val id = remote.id.ifBlank { resolveShortLink(remote.url) }
        val embed = try {
            RemotePlaylistPayloads.spotifyEmbed(
                fetcher.fetch(html("https://open.spotify.com/embed/playlist/$id", 4L * 1024 * 1024)).body
            )
        } catch (error: PlaylistParseException) {
            throw error.toImportException(remote = true)
        }
        if (embed.tracks.isEmpty()) throw PlaylistImportException(PlaylistImportFailureKind.NOT_AVAILABLE, "Spotify playlist empty")
        progress.onRead(embed.tracks.size, null)
        val page = try {
            parseSpotifyPlaylistPage(spotifyHeadOnly(fetcher.fetch(html("https://open.spotify.com/playlist/$id", 1024L * 1024)).body))
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
        val declared = page?.declaredTrackCount
        val completeness = when {
            declared != null -> completenessOf(embed.tracks.size, declared, IncompleteReason.PROVIDER_PAGE_LIMIT)
            embed.tracks.size >= SPOTIFY_EMBED_PAGE -> PlaylistImportCompleteness.Incomplete(embed.tracks.size, null, IncompleteReason.PROVIDER_PAGE_LIMIT)
            else -> PlaylistImportCompleteness.Unknown
        }
        progress.onRead(embed.tracks.size, declared)
        return PlaylistSourceResult(
            ParsedPlaylist(
                ImportedPlaylistDescriptor(
                    source = PlaylistImportSource.SPOTIFY,
                    sourceId = id,
                    title = embed.title.ifBlank { page?.title.orEmpty() },
                    owner = embed.owner,
                    artworkUrl = embed.artworkUrl,
                    declaredTrackCount = declared
                ),
                embed.tracks,
                completeness
            )
        )
    }

    private suspend fun resolveShortLink(url: String): String {
        val final = fetcher.fetch(html(url, 1024L * 1024)).finalUrl.toString()
        val resolved = PlaylistInputDetector.detectUrl(final) as? DetectedPlaylistInput.RemotePlaylist
        return resolved?.id?.takeIf { it.isNotBlank() }
            ?: throw PlaylistImportException(PlaylistImportFailureKind.NOT_A_PLAYLIST, "Spotify link is not a playlist")
    }

    private fun html(url: String, maxBytes: Long) = PlaylistFetchRequest(url, SPOTIFY_HOSTS, PlaylistImportFetcher.HTML, maxBytes)

    companion object {
        const val SPOTIFY_EMBED_PAGE = 100
        private val SPOTIFY_HOSTS = setOf("open.spotify.com", "spotify.link", "spotify.app.link", "www.spotify.com", "spotify.com")
    }
}

class DeezerPlaylistAdapter(private val fetcher: PlaylistImportFetcher) : PlaylistImportSourceAdapter {
    override val sources = setOf(PlaylistImportSource.DEEZER)

    override suspend fun read(input: PlaylistImportInput, detected: DetectedPlaylistInput, progress: PlaylistReadProgress): PlaylistSourceResult {
        val remote = detected as? DetectedPlaylistInput.RemotePlaylist
            ?: throw PlaylistImportException(PlaylistImportFailureKind.INVALID_INPUT, "Not a Deezer playlist")
        val id = remote.id.ifBlank {
            val final = fetcher.fetch(PlaylistFetchRequest(remote.url, DEEZER_HOSTS, PlaylistImportFetcher.HTML, 1024L * 1024)).finalUrl.toString()
            (PlaylistInputDetector.detectUrl(final) as? DetectedPlaylistInput.RemotePlaylist)?.id?.takeIf(String::isNotBlank)
                ?: throw PlaylistImportException(PlaylistImportFailureKind.NOT_A_PLAYLIST, "Deezer link is not a playlist")
        }
        val header = page { RemotePlaylistPayloads.deezerPlaylist(json("https://api.deezer.com/playlist/$id")) }
        val declared = header.total
        val tracks = ArrayList<ImportedTrackIdentity>()
        var index = 0
        while (tracks.size < MAX_PLAYLIST_IMPORT_TRACKS) {
            val next = page { RemotePlaylistPayloads.deezerTracks(json("https://api.deezer.com/playlist/$id/tracks?index=$index&limit=$PAGE")) }
            if (next.tracks.isEmpty()) break
            tracks += next.tracks
            index += next.tracks.size
            progress.onRead(tracks.size, declared)
            if (declared != null && tracks.size >= declared) break
        }
        if (tracks.isEmpty()) throw PlaylistImportException(PlaylistImportFailureKind.NOT_AVAILABLE, "Deezer playlist empty")
        return PlaylistSourceResult(
            ParsedPlaylist(
                ImportedPlaylistDescriptor(PlaylistImportSource.DEEZER, id, header.title, header.owner, header.description, header.artworkUrl, declared),
                tracks.mapIndexed { position, track -> track.copy(position = position) },
                completenessOf(tracks.size, declared, IncompleteReason.PAGINATION_STOPPED)
            )
        )
    }

    private suspend fun json(url: String): String =
        fetcher.fetch(PlaylistFetchRequest(url, DEEZER_HOSTS, PlaylistImportFetcher.JSON, 4L * 1024 * 1024)).body

    private suspend fun page(block: suspend () -> RemoteTrackPage): RemoteTrackPage {
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (error: RemotePayloadException) {
                when {
                    error.code == QUOTA_EXCEEDED && attempt < PlaylistImportFetcher.MAX_RETRIES -> {
                        delay(1_500L shl attempt)
                        attempt++
                    }
                    error.code == QUOTA_EXCEEDED -> throw PlaylistImportException(PlaylistImportFailureKind.RATE_LIMITED, "Deezer quota", error)
                    error.code == DATA_NOT_FOUND -> throw PlaylistImportException(PlaylistImportFailureKind.NOT_AVAILABLE, "Deezer playlist not found", error)
                    else -> throw PlaylistImportException(PlaylistImportFailureKind.PROVIDER_CHANGED, "Deezer error ${error.code}", error)
                }
            } catch (error: PlaylistParseException) {
                throw error.toImportException(remote = true)
            }
        }
    }

    companion object {
        private const val PAGE = 100
        private const val QUOTA_EXCEEDED = 4L
        private const val DATA_NOT_FOUND = 800L
        private val DEEZER_HOSTS = setOf("api.deezer.com", "www.deezer.com", "deezer.com", "link.deezer.com", "deezer.page.link")
    }
}

class AppleMusicPlaylistAdapter(private val fetcher: PlaylistImportFetcher) : PlaylistImportSourceAdapter {
    override val sources = setOf(PlaylistImportSource.APPLE_MUSIC)

    override suspend fun read(input: PlaylistImportInput, detected: DetectedPlaylistInput, progress: PlaylistReadProgress): PlaylistSourceResult {
        val remote = detected as? DetectedPlaylistInput.RemotePlaylist
            ?: throw PlaylistImportException(PlaylistImportFailureKind.INVALID_INPUT, "Not an Apple Music playlist")
        val page = try {
            RemotePlaylistPayloads.appleMusic(
                fetcher.fetch(PlaylistFetchRequest(remote.url, setOf("music.apple.com"), PlaylistImportFetcher.HTML, 8L * 1024 * 1024)).body
            )
        } catch (error: PlaylistParseException) {
            throw error.toImportException(remote = true)
        }
        if (page.tracks.isEmpty()) throw PlaylistImportException(PlaylistImportFailureKind.NOT_AVAILABLE, "Apple Music playlist empty")
        progress.onRead(page.tracks.size, page.total)
        return PlaylistSourceResult(
            ParsedPlaylist(
                ImportedPlaylistDescriptor(PlaylistImportSource.APPLE_MUSIC, remote.id, page.title, page.owner, page.description, page.artworkUrl, page.total),
                page.tracks,
                completenessOf(page.tracks.size, page.total, IncompleteReason.PROVIDER_PAGE_LIMIT)
            )
        )
    }
}

class JioSaavnPlaylistAdapter(private val fetcher: PlaylistImportFetcher) : PlaylistImportSourceAdapter {
    override val sources = setOf(PlaylistImportSource.JIOSAAVN)

    override suspend fun read(input: PlaylistImportInput, detected: DetectedPlaylistInput, progress: PlaylistReadProgress): PlaylistSourceResult {
        val remote = detected as? DetectedPlaylistInput.RemotePlaylist
            ?: throw PlaylistImportException(PlaylistImportFailureKind.INVALID_INPUT, "Not a JioSaavn playlist")
        val tracks = ArrayList<ImportedTrackIdentity>()
        var header: RemoteTrackPage? = null
        var pageNumber = 1
        while (tracks.size < MAX_PLAYLIST_IMPORT_TRACKS) {
            val url = "https://www.jiosaavn.com/api.php?__call=webapi.get&token=${remote.id}&type=playlist&p=$pageNumber&n=$PAGE" +
                "&includeMetaTags=0&ctx=web6dot0&api_version=4&_format=json&_marker=0"
            val page = try {
                RemotePlaylistPayloads.jioSaavn(
                    fetcher.fetch(PlaylistFetchRequest(url, setOf("www.jiosaavn.com"), PlaylistImportFetcher.JSON, 4L * 1024 * 1024)).body
                )
            } catch (error: PlaylistParseException) {
                throw error.toImportException(remote = true)
            }
            if (header == null) header = page
            if (page.tracks.isEmpty()) break
            tracks += page.tracks
            progress.onRead(tracks.size, header.total)
            if (header.total != null && tracks.size >= header.total!!) break
            pageNumber++
        }
        val first = header ?: throw PlaylistImportException(PlaylistImportFailureKind.NOT_AVAILABLE, "JioSaavn playlist unavailable")
        if (tracks.isEmpty()) throw PlaylistImportException(PlaylistImportFailureKind.NOT_AVAILABLE, "JioSaavn playlist empty")
        return PlaylistSourceResult(
            ParsedPlaylist(
                ImportedPlaylistDescriptor(PlaylistImportSource.JIOSAAVN, remote.id, first.title, first.owner, "", first.artworkUrl, first.total),
                tracks.mapIndexed { position, track -> track.copy(position = position) },
                completenessOf(tracks.size, first.total, IncompleteReason.PAGINATION_STOPPED)
            )
        )
    }

    companion object {
        private const val PAGE = 50
    }
}

class BandcampAlbumAdapter(private val fetcher: PlaylistImportFetcher) : PlaylistImportSourceAdapter {
    override val sources = setOf(PlaylistImportSource.BANDCAMP)

    override suspend fun read(input: PlaylistImportInput, detected: DetectedPlaylistInput, progress: PlaylistReadProgress): PlaylistSourceResult {
        val remote = detected as? DetectedPlaylistInput.RemotePlaylist
            ?: throw PlaylistImportException(PlaylistImportFailureKind.INVALID_INPUT, "Not a Bandcamp album")
        val page = try {
            RemotePlaylistPayloads.bandcampAlbum(
                fetcher.fetch(PlaylistFetchRequest(remote.url, setOf("*.bandcamp.com"), PlaylistImportFetcher.HTML, 4L * 1024 * 1024)).body
            )
        } catch (error: PlaylistParseException) {
            throw error.toImportException(remote = true)
        }
        if (page.tracks.isEmpty()) throw PlaylistImportException(PlaylistImportFailureKind.NOT_AVAILABLE, "Bandcamp album empty")
        progress.onRead(page.tracks.size, page.total)
        return PlaylistSourceResult(
            ParsedPlaylist(
                ImportedPlaylistDescriptor(PlaylistImportSource.BANDCAMP, remote.id, page.title, page.owner, "", page.artworkUrl, page.total),
                page.tracks,
                completenessOf(page.tracks.size, page.total, IncompleteReason.PROVIDER_PAGE_LIMIT)
            )
        )
    }
}
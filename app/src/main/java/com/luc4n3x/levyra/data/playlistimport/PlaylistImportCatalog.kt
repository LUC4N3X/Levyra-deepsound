package com.luc4n3x.levyra.data.playlistimport

import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.nexus.identity.MusicIdentityText
import com.luc4n3x.levyra.nexus.playlistimport.CandidateKind
import com.luc4n3x.levyra.nexus.playlistimport.CandidateOrigin
import com.luc4n3x.levyra.nexus.playlistimport.ImportedTrackIdentity
import com.luc4n3x.levyra.nexus.playlistimport.MatchCandidate
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CatalogCandidate(
    val candidate: MatchCandidate,
    val track: Track
)

interface PlaylistCandidateProvider {
    suspend fun candidates(identity: ImportedTrackIdentity, broad: Boolean): List<CatalogCandidate>

    suspend fun search(query: String, origin: CandidateOrigin): List<CatalogCandidate>

    suspend fun refreshLocal() = Unit
}

internal const val LOCAL_TRACK_PREFIX = "local:"
private val youtubeVideoId = Regex("^[A-Za-z0-9_-]{11}$")

internal fun Track.toMatchCandidate(): CatalogCandidate {
    val local = id.startsWith(LOCAL_TRACK_PREFIX) || streamUrl.startsWith("content://")
    val type = videoType.uppercase()
    val kind = when {
        local -> CandidateKind.UNKNOWN
        "ATV" in type -> CandidateKind.SONG
        "OMV" in type -> CandidateKind.OFFICIAL_VIDEO
        "UGC" in type -> CandidateKind.USER_VIDEO
        else -> CandidateKind.UNKNOWN
    }
    return CatalogCandidate(
        MatchCandidate(
            id = id,
            title = title,
            artists = listOf(artist).filter(String::isNotBlank),
            album = album,
            durationMs = durationMs,
            isrc = isrc,
            explicit = if (explicit) true else null,
            origin = if (local) CandidateOrigin.LOCAL else CandidateOrigin.ONLINE,
            kind = kind,
            artworkUrl = thumbnailUrl.ifBlank { largeThumbnailUrl }
        ),
        this
    )
}

internal fun ImportedTrackIdentity.directTrack(known: Track?): Track? {
    if (!youtubeVideoId.matches(directCatalogId)) return null
    if (known != null) return known
    return Track(
        id = directCatalogId,
        title = title,
        artist = artistLine,
        album = album,
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=$directCatalogId",
        thumbnailUrl = artworkUrl.ifBlank { "https://i.ytimg.com/vi/$directCatalogId/hqdefault.jpg" },
        largeThumbnailUrl = artworkUrl.ifBlank { "https://i.ytimg.com/vi/$directCatalogId/hqdefault.jpg" },
        source = "Imported playlist",
        moodTags = setOf("music", "imported"),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0,
        accentEnd = 0
    )
}

class PlaylistImportCatalog(
    private val repository: YoutubeMusicRepository,
    private val localTracks: suspend () -> List<Track>,
    private val languageCode: () -> String
) : PlaylistCandidateProvider {
    private val localMutex = Mutex()
    private var localIndex: Map<String, List<Track>>? = null
    private var localAll: List<Track> = emptyList()

    override suspend fun refreshLocal() {
        localMutex.withLock {
            localIndex = null
            localAll = emptyList()
        }
    }

    override suspend fun candidates(identity: ImportedTrackIdentity, broad: Boolean): List<CatalogCandidate> {
        val title = MusicIdentityText.title(identity.title)
        val primary = identity.primaryArtist
        val query = if (broad) {
            listOf(title.core, primary.substringBefore(',').trim()).filter(String::isNotBlank).joinToString(" ")
                .takeIf { it.length >= 2 && it != identity.title } ?: title.core
        } else {
            listOf(identity.title, primary).filter(String::isNotBlank).joinToString(" ")
        }
        val local = localMatches(title.core)
        val online = if (query.length >= 2) onlineSearch(query, if (broad) 12 else 8) else emptyList()
        return (local + online).distinctBy { it.id }.map { it.toMatchCandidate() }
    }

    override suspend fun search(query: String, origin: CandidateOrigin): List<CatalogCandidate> {
        val clean = query.trim()
        if (clean.length < 2) return emptyList()
        return when (origin) {
            CandidateOrigin.ONLINE -> onlineSearch(clean, 16)
            CandidateOrigin.LOCAL -> localSearch(clean)
        }.distinctBy { it.id }.map { it.toMatchCandidate() }
    }

    private suspend fun onlineSearch(query: String, limit: Int): List<Track> {
        val language = languageCode()
        val songs = retrying { repository.searchSongsPage(query, language).items }
        if (songs.size >= 3) return songs.take(limit)
        val mixed = retrying { repository.search(query, limit, language) }
        return (songs + mixed).distinctBy { it.id }.take(limit)
    }

    private suspend fun <T> retrying(block: suspend () -> List<T>): List<T> {
        repeat(2) { attempt ->
            try {
                return block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                delay(600L shl attempt)
            }
        }
        return try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            emptyList()
        }
    }

    private suspend fun ensureLocalIndex(): Map<String, List<Track>> = localMutex.withLock {
        localIndex ?: run {
            val tracks = localTracks()
            localAll = tracks
            tracks.groupBy { MusicIdentityText.title(it.title).core }.also { localIndex = it }
        }
    }

    private suspend fun localMatches(core: String): List<Track> {
        if (core.isBlank()) return emptyList()
        return ensureLocalIndex()[core].orEmpty().take(6)
    }

    private suspend fun localSearch(query: String): List<Track> {
        ensureLocalIndex()
        val needle = MusicIdentityText.normalize(query)
        if (needle.isBlank()) return emptyList()
        return localAll.asSequence()
            .filter { track ->
                val haystack = MusicIdentityText.normalize("${track.title} ${track.artist} ${track.album}")
                needle.split(' ').all { it in haystack }
            }
            .take(30)
            .toList()
    }
}

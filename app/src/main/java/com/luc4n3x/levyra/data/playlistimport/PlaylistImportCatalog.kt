package com.luc4n3x.levyra.data.playlistimport

import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.nexus.identity.MusicIdentityText
import com.luc4n3x.levyra.nexus.playlistimport.CandidateKind
import com.luc4n3x.levyra.nexus.playlistimport.CandidateOrigin
import com.luc4n3x.levyra.nexus.playlistimport.ImportedTrackIdentity
import com.luc4n3x.levyra.nexus.playlistimport.MatchCandidate
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistMatchEngine
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

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

internal fun trackKindRank(track: Track): Int {
    val type = track.videoType.uppercase()
    return when {
        "ATV" in type -> 0
        "OMV" in type -> 1
        "UGC" in type -> 3
        else -> 2
    }
}

internal fun combineOnlineSearchResults(songs: List<Track>, mixed: List<Track>, limit: Int): List<Track> {
    if (songs.isEmpty()) return mixed.distinctBy { it.id }.take(limit)
    if (mixed.isEmpty()) return songs.distinctBy { it.id }.take(limit)

    val result = ArrayList<Track>(limit)
    val seenIds = HashSet<String>()

    fun addTrack(track: Track): Boolean {
        if (result.size >= limit) return false
        if (seenIds.add(track.id)) {
            result.add(track)
            return true
        }
        return false
    }

    mixed.firstOrNull()?.let { addTrack(it) }

    val mixedHighRelevance = mixed.filter { trackKindRank(it) <= 1 }
    val songsIter = songs.iterator()
    val mixedIter = mixedHighRelevance.iterator()
    while (result.size < limit && (songsIter.hasNext() || mixedIter.hasNext())) {
        if (songsIter.hasNext()) addTrack(songsIter.next())
        if (mixedIter.hasNext()) addTrack(mixedIter.next())
    }

    for (song in songs) {
        if (result.size >= limit) break
        addTrack(song)
    }

    for (m in mixed) {
        if (result.size >= limit) break
        addTrack(m)
    }

    return result
}

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
        val cleanTitle = title.core
        val cleanPrimary = identity.primaryArtist.substringBefore(',').trim()
        val local = localMatches(cleanTitle)

        val online = if (broad) {
            val query = listOf(cleanTitle, cleanPrimary).filter(String::isNotBlank).joinToString(" ")
                .takeIf { it.length >= 2 && it != identity.title } ?: cleanTitle
            if (query.length >= 2) onlineSearch(query, 12) else emptyList()
        } else {
            resolveCandidatesAdaptive(identity, cleanTitle, cleanPrimary)
        }

        return (local + online).distinctBy { it.id }.map { it.toMatchCandidate() }
    }

    override suspend fun search(query: String, origin: CandidateOrigin): List<CatalogCandidate> {
        val clean = query.trim()
        if (clean.length < 2) return emptyList()
        return try {
            when (origin) {
                CandidateOrigin.ONLINE -> onlineSearch(clean, 16)
                CandidateOrigin.LOCAL -> localSearch(clean)
            }.distinctBy { it.id }.map { it.toMatchCandidate() }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.w(error, "Playlist replacement search failed")
            emptyList()
        }
    }

    internal suspend fun onlineSearch(query: String, limit: Int): List<Track> {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return emptyList()
        val language = languageCode()
        return coroutineScope {
            val songsDeferred = async { retrying { repository.searchSongsPage(cleanQuery, language).items } }
            val mixedDeferred = async { retrying { repository.search(cleanQuery, limit, language) } }
            val songs = songsDeferred.await()
            val mixed = mixedDeferred.await()
            combineOnlineSearchResults(songs, mixed, limit)
        }
    }

    private fun rankAutomaticCandidates(
        identity: ImportedTrackIdentity,
        tracks: List<Track>,
        limit: Int
    ): List<Track> = tracks
        .distinctBy { it.id }
        .map { track ->
            track to PlaylistMatchEngine.evaluate(identity, track.toMatchCandidate().candidate).score
        }
        .sortedByDescending { (_, score) -> score }
        .take(limit)
        .map { (track, _) -> track }

    private suspend fun resolveCandidatesAdaptive(
        identity: ImportedTrackIdentity,
        cleanTitle: String,
        cleanPrimary: String
    ): List<Track> {
        val language = languageCode()
        val primary = identity.primaryArtist
        val preciseQuery = listOf(identity.title, primary).filter(String::isNotBlank).joinToString(" ")
        if (preciseQuery.length < 2) return emptyList()

        val songs = retrying { repository.searchSongsPage(preciseQuery, language).items }
        val hasGoodSongMatch = songs.any { track ->
            PlaylistMatchEngine.evaluate(identity, track.toMatchCandidate().candidate).confidence.autoAccepted
        }
        if (hasGoodSongMatch) {
            return rankAutomaticCandidates(identity, songs, 8)
        }

        val mixed = retrying { repository.search(preciseQuery, 8, language) }
        val hasGoodMixedMatch = mixed.any { track ->
            PlaylistMatchEngine.evaluate(identity, track.toMatchCandidate().candidate).confidence.autoAccepted
        }
        val firstPassCandidates = (songs + mixed).distinctBy { it.id }
        val firstPassCombined = combineOnlineSearchResults(songs, mixed, 8)
        if (hasGoodMixedMatch) {
            return rankAutomaticCandidates(identity, firstPassCandidates, 8)
        }

        val cleanQuery = listOf(cleanTitle, cleanPrimary).filter(String::isNotBlank).joinToString(" ")
            .takeIf { it.length >= 2 && it != preciseQuery }
        if (cleanQuery != null) {
            val fallbackResults = onlineSearch(cleanQuery, 8)
            return rankAutomaticCandidates(identity, firstPassCandidates + fallbackResults, 8)
        }

        return firstPassCombined
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

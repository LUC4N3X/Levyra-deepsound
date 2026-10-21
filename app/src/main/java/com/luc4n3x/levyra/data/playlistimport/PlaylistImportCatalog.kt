package com.luc4n3x.levyra.data.playlistimport

import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.nexus.identity.MusicIdentityText
import com.luc4n3x.levyra.nexus.playlistimport.CandidateKind
import com.luc4n3x.levyra.nexus.playlistimport.CandidateOrigin
import com.luc4n3x.levyra.nexus.playlistimport.ImportedTrackIdentity
import com.luc4n3x.levyra.nexus.playlistimport.MatchCandidate
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistMatchEngine
import com.luc4n3x.levyra.nexus.playlistimport.ImportTransliteration
import com.luc4n3x.levyra.nexus.playlistimport.MatchOutcome
import com.luc4n3x.levyra.nexus.playlistimport.MatchSignal
import com.luc4n3x.levyra.nexus.playlistimport.ResolutionPreference
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

data class CatalogCandidate(
    val candidate: MatchCandidate,
    val track: Track
)

data class CatalogResolution(val candidates: List<CatalogCandidate>, val outcome: MatchOutcome)

interface PlaylistCandidateProvider {
    suspend fun candidates(identity: ImportedTrackIdentity, broad: Boolean): List<CatalogCandidate>

    suspend fun search(query: String, origin: CandidateOrigin): List<CatalogCandidate>

    suspend fun refreshLocal() = Unit

    suspend fun resolve(identity: ImportedTrackIdentity, broad: Boolean, preference: ResolutionPreference): CatalogResolution {
        val found = candidates(identity, broad)
        return CatalogResolution(found, PlaylistMatchEngine.select(identity, found.map { it.candidate }, preference))
    }
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
    private val languageCode: () -> String,
    private val metadataSearch: suspend (ImportedTrackIdentity) -> List<MatchCandidate> = { emptyList() }
) : PlaylistCandidateProvider {
    private val localMutex = Mutex()
    private var localIndex: Map<String, List<Track>>? = null
    private var localAll: List<Track> = emptyList()
    private val lookupLocks = Array(16) { Mutex() }
    private val lookupCache = object : LinkedHashMap<ImportedTrackIdentity, CatalogResolution>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ImportedTrackIdentity, CatalogResolution>): Boolean = size > 256
    }

    override suspend fun refreshLocal() {
        localMutex.withLock {
            localIndex = null
            localAll = emptyList()
        }
        synchronized(lookupCache) { lookupCache.clear() }
    }

    override suspend fun candidates(identity: ImportedTrackIdentity, broad: Boolean): List<CatalogCandidate> {
        return resolve(identity, broad, ResolutionPreference.SMART).candidates
    }

    override suspend fun resolve(identity: ImportedTrackIdentity, broad: Boolean, preference: ResolutionPreference): CatalogResolution {
        val key = identity.copy(position = 0)
        return lookupLocks[(key.hashCode() and Int.MAX_VALUE) % lookupLocks.size].withLock {
            if (!broad && preference == ResolutionPreference.SMART) {
                synchronized(lookupCache) { lookupCache[key] }?.let { return@withLock it }
            }
            val result = withTimeoutOrNull(45_000L) { resolveAdaptive(identity, preference) }
                ?: throw IOException("Playlist candidate lookup timed out")
            if (preference == ResolutionPreference.SMART) synchronized(lookupCache) { lookupCache[key] = result }
            result
        }
    }

    override suspend fun search(query: String, origin: CandidateOrigin): List<CatalogCandidate> {
        val clean = query.trim()
        if (clean.length < 2) return emptyList()
        return when (origin) {
            CandidateOrigin.ONLINE -> onlineSearch(clean, 16)
            CandidateOrigin.LOCAL -> localSearch(clean)
        }.distinctBy { it.id }.map { it.toMatchCandidate() }
    }

    internal suspend fun onlineSearch(query: String, limit: Int): List<Track> {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return emptyList()
        val language = languageCode()
        return coroutineScope {
            val songsDeferred = async {
                runSearch { retrying { songs(cleanQuery, language) } }
            }
            val mixedDeferred = async {
                runSearch { retrying { repository.searchForImport(cleanQuery, limit, language) } }
            }
            val songs = songsDeferred.await()
            val mixed = mixedDeferred.await()
            if (songs.isFailure && mixed.isFailure) throw IOException("Playlist search unavailable", songs.exceptionOrNull())
            combineOnlineSearchResults(songs.getOrDefault(emptyList()), mixed.getOrDefault(emptyList()), limit)
        }
    }

    private suspend fun <T> runSearch(block: suspend () -> List<T>): Result<List<T>> = try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Timber.w(error, "Playlist import online search branch failed")
        Result.failure(error)
    }

    private suspend fun songs(query: String, language: String): List<Track> {
        val page = repository.searchSongsPage(query, language)
        if (page.failed) throw IOException("Music search unavailable")
        return page.items
    }

    private suspend fun resolveAdaptive(identity: ImportedTrackIdentity, preference: ResolutionPreference): CatalogResolution {
        val language = languageCode()
        val found = localMatches(MusicIdentityText.title(identity.title).core).map { it.toMatchCandidate() }.toMutableList()
        var reference = identity
        var failure: Throwable? = null
        var succeeded = false
        val queried = HashSet<String>()
        fun result(): CatalogResolution {
            val unique = found.distinctBy { it.candidate.id }
            return CatalogResolution(unique, PlaylistMatchEngine.select(reference, unique.map { it.candidate }, preference))
        }
        suspend fun search(query: String, mixed: Boolean) {
            val key = "${if (mixed) "mixed" else "song"}:${query.lowercase(java.util.Locale.ROOT)}"
            if (query.length < 2 || !queried.add(key)) return
            val response = runSearch {
                retrying { if (mixed) repository.searchForImport(query, 12, language) else songs(query, language) }
            }
            response.onSuccess {
                succeeded = true
                found += it.take(20).map(Track::toMatchCandidate)
            }.onFailure { failure = it }
            if (response.isFailure && !mixed) search(query, true)
        }
        fun reliable(): Boolean = result().outcome.confidence.autoAccepted
        if (reliable() && preference != ResolutionPreference.PREFER_ONLINE) return result()
        val originalQuery = "${identity.title} ${identity.primaryArtist}".trim()
        search(originalQuery, false)
        if (reliable()) return result()
        search(originalQuery, true)
        if (reliable()) return result()
        val primary = MusicIdentityText.artistNames(identity.primaryArtist).firstOrNull().orEmpty()
        val normalized = "${MusicIdentityText.title(identity.title).core} $primary".trim()
        if (!normalized.equals(originalQuery, ignoreCase = true)) {
            search(normalized, false)
            if (reliable()) return result()
        }
        val metadata = try {
            metadataSearch(identity).take(30)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            emptyList()
        }
        reference = corroborateImportIdentity(identity, metadata)
        if (reliable()) return result()
        val metadataQueries = metadata.map { it to PlaylistMatchEngine.evaluate(identity, it) }
            .filter { it.second.score >= 55 }
            .sortedByDescending { it.second.score }
            .map { "${it.first.title} ${it.first.artists.firstOrNull().orEmpty()}".trim() }
            .distinct().take(2)
        val queries = (metadataQueries + ImportTransliteration.latinQuery(normalized)).distinct()
        for (query in queries) {
            search(query, false)
            if (reliable()) return result()
        }
        search(normalized, true)
        val resolved = result()
        if (failure != null && !succeeded && resolved.outcome.selected == null) throw IOException("Playlist search incomplete", failure)
        return resolved
    }

    private suspend fun <T> retrying(block: suspend () -> List<T>): List<T> {
        repeat(1) { attempt ->
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

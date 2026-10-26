package com.luc4n3x.levyra.data.playlistimport

import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.nexus.identity.MusicIdentityText
import com.luc4n3x.levyra.nexus.playlistimport.CandidateKind
import com.luc4n3x.levyra.nexus.playlistimport.CandidateOrigin
import com.luc4n3x.levyra.nexus.playlistimport.ImportedTrackIdentity
import com.luc4n3x.levyra.nexus.playlistimport.MatchCandidate
import com.luc4n3x.levyra.nexus.playlistimport.MatchConfidence
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

interface ImportAlbumSource {
    suspend fun albums(query: String, languageCode: String): List<AlbumHit>

    suspend fun tracks(album: AlbumHit, languageCode: String): List<Track>
}

internal const val LOCAL_TRACK_PREFIX = "local:"
private const val ALBUM_CACHE_ENTRIES = 128
private const val MAX_ALBUM_BROWSES = 2
private const val MAX_ISRC_RESULTS = 5
private const val MAX_DURATION_LOOKUPS = 2
private const val MIN_ALBUM_TRACK_SCORE = 40
private const val FALLBACK_SEARCH_LANGUAGE = "en"
private const val SEARCH_BACKOFF_BASE_MS = 500L
private const val SEARCH_BACKOFF_MAX_MS = 8_000L
private const val SEARCH_BACKOFF_MAX_LEVEL = 5

class ImportSearchBackoff(private val clock: () -> Long = { System.nanoTime() / 1_000_000L }) {
    private var level = 0
    private var openAtMs = 0L

    suspend fun await() {
        while (true) {
            val waitMs = synchronized(this) { openAtMs - clock() }
            if (waitMs <= 0L) return
            delay(waitMs)
        }
    }

    fun failed() {
        synchronized(this) {
            level = (level + 1).coerceAtMost(SEARCH_BACKOFF_MAX_LEVEL)
            val pauseMs = (SEARCH_BACKOFF_BASE_MS shl (level - 1)).coerceAtMost(SEARCH_BACKOFF_MAX_MS)
            openAtMs = maxOf(openAtMs, clock() + pauseMs)
        }
    }

    fun succeeded() {
        synchronized(this) { level = 0 }
    }

    internal fun pendingMs(): Long = synchronized(this) { (openAtMs - clock()).coerceAtLeast(0L) }
}

private val untrustedAlbumTitles = setOf("single", "singles", "unknown", "unknown album", "youtube", "youtube music")
private val compilationArtists = setOf("various artists", "various", "verschiedene interpreten", "artisti vari", "сборник")

private fun <T> boundedCache(entries: Int) = object : LinkedHashMap<String, List<T>>(entries, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<T>>): Boolean = size > entries
}
private val youtubeVideoId = Regex("^[A-Za-z0-9_-]{11}$")
private val artistTitleSeparator = Regex("\\s+(?:--|-|–|—)\\s+")
private val channelSuffix = Regex("\\s*(?:-\\s*topic|vevo)$", RegexOption.IGNORE_CASE)
private val topicChannel = Regex("\\s-\\s*topic$", RegexOption.IGNORE_CASE)

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
        "ATV" in type || topicChannel.containsMatchIn(artist) -> CandidateKind.SONG
        "OMV" in type -> CandidateKind.OFFICIAL_VIDEO
        "UGC" in type -> CandidateKind.USER_VIDEO
        else -> CandidateKind.UNKNOWN
    }
    val separator = artistTitleSeparator.find(title).takeIf { source == "LevyraExtractor Search" }
    val titleArtist = separator?.let { title.substring(0, it.range.first).trim() }.orEmpty()
    val channelArtist = MusicIdentityText.normalizeArtist(artist.replace(channelSuffix, ""))
    val artistPrefixMatchesChannel = channelArtist.isNotBlank() &&
        (MusicIdentityText.normalizeArtist(titleArtist) == channelArtist ||
            MusicIdentityText.artistNames(titleArtist).firstOrNull() == channelArtist)
    val recordingTitle = if (artistPrefixMatchesChannel) title.substring(requireNotNull(separator).range.last + 1).trim() else title
    val recordingArtist = if (artistPrefixMatchesChannel) titleArtist else artist
    return CatalogCandidate(
        MatchCandidate(
            id = id,
            title = recordingTitle,
            artists = listOf(recordingArtist).filter(String::isNotBlank),
            album = album,
            durationMs = durationMs,
            isrc = isrc,
            explicit = if (explicit) true else null,
            origin = if (local) CandidateOrigin.LOCAL else CandidateOrigin.ONLINE,
            kind = kind,
            artworkUrl = thumbnailUrl.ifBlank { largeThumbnailUrl }
        ),
        if (artistPrefixMatchesChannel) copy(title = recordingTitle, artist = recordingArtist) else this
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
    private val metadataSearch: suspend (ImportedTrackIdentity) -> List<MatchCandidate> = { emptyList() },
    private val youtubeSearch: (suspend (String) -> List<Track>)? = null,
    private val albumSource: ImportAlbumSource? = null,
    private val durationLookup: (suspend (String, String) -> Long)? = null,
    private val searchBackoff: ImportSearchBackoff? = null
) : PlaylistCandidateProvider {
    private val localMutex = Mutex()
    private var localIndex: Map<String, List<Track>>? = null
    private var localAll: List<Track> = emptyList()
    private val lookupLocks = Array(16) { Mutex() }
    private val lookupCache = object : LinkedHashMap<ImportedTrackIdentity, CatalogResolution>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ImportedTrackIdentity, CatalogResolution>): Boolean = size > 256
    }
    private val albumLocks = Array(32) { Mutex() }
    private val albumSearches = boundedCache<AlbumHit>(ALBUM_CACHE_ENTRIES)
    private val albumTrackLists = boundedCache<Track>(ALBUM_CACHE_ENTRIES)

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
        searchBackoff?.await()
        Result.success(block()).also { searchBackoff?.succeeded() }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        searchBackoff?.failed()
        Timber.w(error, "Playlist import online search branch failed")
        Result.failure(error)
    }

    private suspend fun songs(query: String, language: String): List<Track> {
        val page = repository.searchSongsPage(query, language)
        if (page.failed) throw IOException("Music search unavailable")
        return page.items
    }

    private suspend fun resolveAdaptive(identity: ImportedTrackIdentity, preference: ResolutionPreference): CatalogResolution {
        val local = localMatches(MusicIdentityText.title(identity.title).core).map { it.toMatchCandidate() }
        val lookup = ImportLookup(identity, preference, languageCode(), local)
        if (lookup.reliable() && preference != ResolutionPreference.PREFER_ONLINE) return lookup.result()
        val originalQuery = "${identity.title} ${identity.primaryArtist}".trim()
        val primary = MusicIdentityText.normalizeArtist(identity.primaryArtist)
        val normalized = "${MusicIdentityText.title(identity.title).core} $primary".trim()
        val credits = (identity.artists + identity.featuredArtists).filter(String::isNotBlank).distinct()
        val creditedQuery = "${identity.title} - ${credits.joinToString(" ")}".takeIf { credits.size > 1 }
        val direct = listOf(originalQuery to false, originalQuery to true) +
            listOfNotNull(creditedQuery?.let { it to false }) +
            listOfNotNull((normalized to false).takeUnless { normalized.equals(originalQuery, ignoreCase = true) })
        if (lookup.searchUntilReliable(direct)) return lookup.result()
        lookup.searchAlbum()
        if (lookup.reliable()) return lookup.result()
        if (identity.durationMs > 0L && identity.album.isNotBlank()) {
            lookup.searchYoutube(originalQuery)
            if (lookup.reliable()) return lookup.result()
        }
        val metadata = safeMetadata(identity)
        lookup.reference = corroborateImportIdentity(identity, metadata)
        if (lookup.reliable()) return lookup.result()
        lookup.searchIsrc()
        if (lookup.reliable()) return lookup.result()
        val fallback = (metadataQueries(identity, metadata) + ImportTransliteration.latinQuery(normalized)).distinct()
        if (lookup.searchUntilReliable(fallback.map { it to false })) return lookup.result()
        lookup.search(normalized, true)
        if (!lookup.reliable()) lookup.searchYoutube(originalQuery)
        lookup.verifyUnknownDurations()
        return lookup.finish()
    }

    private suspend fun safeMetadata(identity: ImportedTrackIdentity): List<MatchCandidate> = try {
        metadataSearch(identity).take(30)
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        emptyList()
    }

    private fun metadataQueries(identity: ImportedTrackIdentity, metadata: List<MatchCandidate>): List<String> =
        metadata.map { it to PlaylistMatchEngine.evaluate(identity, it) }
            .filter { it.second.score >= 55 }
            .sortedByDescending { it.second.score }
            .map { "${it.first.title} ${it.first.artists.firstOrNull().orEmpty()}".trim() }
            .distinct().take(2)

    private inner class ImportLookup(
        var reference: ImportedTrackIdentity,
        private val preference: ResolutionPreference,
        private val language: String,
        local: List<CatalogCandidate>
    ) {
        private val found = local.toMutableList()
        private val queried = HashSet<String>()
        private var failure: Throwable? = null

        fun result(): CatalogResolution {
            val unique = found.distinctBy { it.candidate.id }
            return CatalogResolution(unique, PlaylistMatchEngine.select(reference, unique.map { it.candidate }, preference))
        }

        fun reliable(): Boolean = result().outcome.let { outcome ->
            outcome.confidence.autoAccepted && outcome.selected?.let(PlaylistMatchEngine::provisional) != true
        }

        suspend fun searchUntilReliable(queries: List<Pair<String, Boolean>>): Boolean {
            for ((query, mixed) in queries) {
                search(query, mixed)
                if (reliable()) return true
            }
            return false
        }

        suspend fun search(query: String, mixed: Boolean) {
            val key = "${if (mixed) "mixed" else "song"}:${query.lowercase(java.util.Locale.ROOT)}"
            if (query.length < 2 || !queried.add(key)) return
            val response = runSearch {
                retrying { if (mixed) repository.searchForImport(query, 12, language) else songs(query, language) }
            }
            response.onSuccess {
                found += it.take(20).map(Track::toMatchCandidate)
            }.onFailure { failure = it }
            if (response.isFailure && !mixed) search(query, true)
            if (!mixed && response.getOrNull()?.isEmpty() == true && !language.equals(FALLBACK_SEARCH_LANGUAGE, ignoreCase = true) &&
                queried.add("song-$FALLBACK_SEARCH_LANGUAGE:${query.lowercase(java.util.Locale.ROOT)}")
            ) {
                runSearch { retrying { songs(query, FALLBACK_SEARCH_LANGUAGE) } }
                    .onSuccess { found += it.take(20).map(Track::toMatchCandidate) }
            }
        }

        suspend fun searchAlbum() {
            if (!queried.add("album")) return
            val response = runSearch { albumTracks(reference, language) }
            response.onSuccess { tracks ->
                tracks.map(Track::toMatchCandidate)
                    .map { item -> if (item.candidate.kind == CandidateKind.UNKNOWN) item.copy(candidate = item.candidate.copy(kind = CandidateKind.SONG)) else item }
                    .filter { PlaylistMatchEngine.evaluate(reference, it.candidate).score >= MIN_ALBUM_TRACK_SCORE }
                    .forEach { found += it }
            }.onFailure { failure = it }
        }

        suspend fun searchIsrc() {
            val isrc = PlaylistMatchEngine.normalizeIsrc(reference.isrc)
            if (isrc.isEmpty() || !queried.add("isrc:$isrc")) return
            val response = runSearch { retrying { songs(isrc, language) } }
            response.onSuccess { tracks ->
                tracks.take(MAX_ISRC_RESULTS).map(Track::toMatchCandidate).forEach { result ->
                    if (PlaylistMatchEngine.isrcSearchCorroborates(reference, result.candidate)) {
                        found.removeAll { it.candidate.id == result.candidate.id }
                        found += result.copy(candidate = result.candidate.copy(isrc = isrc))
                    } else {
                        found += result
                    }
                }
            }.onFailure { failure = it }
        }

        suspend fun verifyUnknownDurations() {
            val lookupDuration = durationLookup ?: return
            if (reference.durationMs <= 0L || reliable()) return
            result().outcome.alternatives
                .filter { it.candidate.durationMs <= 0L && it.score >= MatchConfidence.REVIEW_SCORE }
                .take(MAX_DURATION_LOOKUPS)
                .forEach { evaluation ->
                    val id = evaluation.candidate.id
                    if (!queried.add("duration:$id")) return@forEach
                    val duration = runSearch { listOf(lookupDuration(id, language)) }.getOrNull()?.firstOrNull() ?: 0L
                    val index = found.indexOfFirst { it.candidate.id == id }
                    if (duration > 0L && index >= 0) {
                        val known = found[index]
                        found[index] = known.copy(
                            candidate = known.candidate.copy(durationMs = duration),
                            track = known.track.copy(durationMs = duration)
                        )
                    }
                }
        }

        suspend fun searchYoutube(query: String) {
            val search = youtubeSearch ?: return
            if (!queried.add("youtube:${query.lowercase(java.util.Locale.ROOT)}")) return
            val response = runSearch { search(query) }
            response.onSuccess {
                it.take(16).map(Track::toMatchCandidate).forEach { candidate ->
                    val index = found.indexOfFirst { previous -> previous.candidate.id == candidate.candidate.id }
                    if (index < 0) {
                        found += candidate
                    } else {
                        val previous = found[index].candidate
                        val knownArtists = previous.artists.flatMap(MusicIdentityText::artistNames).toSet()
                        val fallbackArtists = candidate.candidate.artists.flatMap(MusicIdentityText::artistNames).toSet()
                        if (fallbackArtists.containsAll(knownArtists) &&
                            PlaylistMatchEngine.evaluate(reference, candidate.candidate).score >
                            PlaylistMatchEngine.evaluate(reference, previous).score) {
                            found[index] = candidate
                        }
                    }
                }
            }.onFailure { failure = it }
        }

        fun finish(): CatalogResolution {
            val resolved = result()
            val error = failure
            if (error != null && resolved.outcome.selected == null) throw IOException("Playlist search incomplete", error)
            return resolved
        }
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

    private suspend fun albumTracks(identity: ImportedTrackIdentity, language: String): List<Track> {
        val source = albumSource ?: return emptyList()
        val album = MusicIdentityText.album(identity.album).core
        val primary = MusicIdentityText.normalizeArtist(identity.primaryArtist)
        if (album.length < 2 || album in untrustedAlbumTitles || primary.isBlank()) return emptyList()
        val query = "${identity.album} ${identity.primaryArtist}".trim()
        val hits = cachedAlbumValue(albumSearches, query.lowercase(java.util.Locale.ROOT)) {
            retrying { source.albums(query, language) }
        }.filter { hit ->
            val names = MusicIdentityText.artistNames(hit.artist)
            MusicIdentityText.album(hit.title).core == album &&
                (primary in names || names.isEmpty() || names.any { it in compilationArtists })
        }.distinctBy { it.browseId.ifBlank { it.title } }.take(MAX_ALBUM_BROWSES)
        return hits.flatMap { hit ->
            cachedAlbumValue(albumTrackLists, hit.browseId.ifBlank { "${hit.title}|${hit.artist}" }) {
                source.tracks(hit, language)
            }
        }
    }

    private suspend fun <T> cachedAlbumValue(
        cache: LinkedHashMap<String, List<T>>,
        key: String,
        load: suspend () -> List<T>
    ): List<T> = albumLocks[(key.hashCode() and Int.MAX_VALUE) % albumLocks.size].withLock {
        synchronized(cache) { cache[key] }?.let { return@withLock it }
        load().also { value -> if (value.isNotEmpty()) synchronized(cache) { cache[key] = value } }
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

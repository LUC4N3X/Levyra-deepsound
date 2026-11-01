package com.luc4n3x.levyra.feature.moodjourney

import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.MoodCategoryKind
import com.luc4n3x.levyra.domain.MoodJourneyEngine
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

data class EditorialPlaylistRef(val playlistId: String, val title: String)

class MoodEditorialGateway(
    val categories: suspend () -> List<ExploreCategory>,
    val playlists: suspend (params: String) -> List<EditorialPlaylistRef>,
    val tracks: suspend (playlistId: String) -> List<Track>
)

data class MoodEditorialEvidence(
    val labels: Map<String, Set<MoodCategoryKind>>,
    val tracks: List<Track>,
    val categoriesFound: Int,
    val playlistsRead: Int
) {
    fun labelsFor(track: Track): Set<MoodCategoryKind> = labels[MoodJourneyEngine.keyOf(track)].orEmpty()

    companion object {
        val Empty = MoodEditorialEvidence(emptyMap(), emptyList(), 0, 0)
    }
}

class MoodEvidenceSource(
    private val gateway: MoodEditorialGateway,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val mutex = Mutex()
    private var cached: MoodEditorialEvidence? = null
    private var cachedAtMs = 0L

    suspend fun load(): MoodEditorialEvidence = mutex.withLock {
        val current = cached
        if (current != null && clock() - cachedAtMs < TTL_MS) return current
        val fresh = fetch()
        if (fresh.playlistsRead > 0) {
            cached = fresh
            cachedAtMs = clock()
        }
        fresh
    }

    private suspend fun fetch(): MoodEditorialEvidence = coroutineScope {
        val categories = guarded { gateway.categories() }.orEmpty()
            .mapNotNull { category -> MoodCategoryKind.fromTitle(category.title)?.let { it to category } }
            .distinctBy { it.first }
        if (categories.isEmpty()) return@coroutineScope MoodEditorialEvidence.Empty
        val permits = Semaphore(MAX_PARALLEL)
        val perCategory = categories.map { (kind, category) ->
            async {
                val refs = permits.withPermit { guarded { gateway.playlists(category.params) } }.orEmpty()
                    .filter { it.playlistId.isNotBlank() }
                    .take(PLAYLISTS_PER_CATEGORY)
                val lists = refs.map { ref ->
                    async { permits.withPermit { guarded { gateway.tracks(ref.playlistId) } }.orEmpty().take(TRACKS_PER_PLAYLIST) }
                }.awaitAll()
                Triple(kind, lists.flatten(), lists.count { it.isNotEmpty() })
            }
        }.awaitAll()
        val labels = HashMap<String, MutableSet<MoodCategoryKind>>()
        val tracks = LinkedHashMap<String, Track>()
        var playlistsRead = 0
        perCategory.forEach { (kind, categoryTracks, read) ->
            playlistsRead += read
            categoryTracks.forEach { track ->
                val key = MoodJourneyEngine.keyOf(track)
                if (key.isEmpty()) return@forEach
                labels.getOrPut(key) { HashSet(2) } += kind
                tracks.putIfAbsent(key, track)
            }
        }
        MoodEditorialEvidence(labels, tracks.values.toList(), categories.size, playlistsRead)
    }

    private suspend fun <T> guarded(block: suspend () -> T): T? = try {
        withTimeoutOrNull(REQUEST_TIMEOUT_MS) { block() }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        null
    }

    private companion object {
        const val TTL_MS = 12L * 60L * 60L * 1000L
        const val REQUEST_TIMEOUT_MS = 10_000L
        const val MAX_PARALLEL = 4
        const val PLAYLISTS_PER_CATEGORY = 2
        const val TRACKS_PER_PLAYLIST = 50
    }
}

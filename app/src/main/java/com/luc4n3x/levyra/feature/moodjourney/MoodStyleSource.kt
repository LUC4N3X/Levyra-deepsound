package com.luc4n3x.levyra.feature.moodjourney

import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.MoodCategoryKind
import com.luc4n3x.levyra.domain.MoodJourneyEngine
import com.luc4n3x.levyra.domain.MoodStyle
import com.luc4n3x.levyra.domain.Track
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

class MoodStyleGateway(
    val applePlaylists: suspend (genre: String) -> List<EditorialPlaylistRef>,
    val appleTracks: suspend (playlistId: String) -> List<Track>,
    val categories: suspend () -> List<ExploreCategory>,
    val playlists: suspend (params: String) -> List<EditorialPlaylistRef>,
    val searchPlaylists: suspend (query: String) -> List<EditorialPlaylistRef>,
    val tracks: suspend (playlistId: String) -> List<Track>
)

data class MoodStyleEntry(val track: Track, val mood: MoodCategoryKind?)

data class MoodStyleShelf(val style: MoodStyle, val entries: List<MoodStyleEntry>, val playlistsRead: Int) {
    companion object {
        fun empty(style: MoodStyle) = MoodStyleShelf(style, emptyList(), 0)
    }
}

class MoodStyleEvidence(shelves: Collection<MoodStyleShelf>) {
    private val trackStyles = HashMap<String, MutableSet<MoodStyle>>()
    private val artistStyles = HashMap<String, MutableSet<MoodStyle>>()
    private val labels = HashMap<String, MutableSet<MoodCategoryKind>>()
    val tracks: List<Track>
    val playlistsRead: Int = shelves.sumOf { it.playlistsRead }

    init {
        val unique = LinkedHashMap<String, Track>()
        shelves.forEach { shelf ->
            shelf.entries.forEach { entry ->
                val key = MoodJourneyEngine.keyOf(entry.track)
                if (key.isEmpty()) return@forEach
                trackStyles.getOrPut(key) { HashSet(2) } += shelf.style
                artistKey(entry.track)?.let { artistStyles.getOrPut(it) { HashSet(2) } += shelf.style }
                entry.mood?.let { labels.getOrPut(key) { HashSet(2) } += it }
                unique.putIfAbsent(key, entry.track)
            }
        }
        tracks = unique.values.toList()
    }

    fun stylesFor(track: Track): Set<MoodStyle> {
        val byTrack = trackStyles[MoodJourneyEngine.keyOf(track)].orEmpty()
        val byArtist = artistKey(track)?.let { artistStyles[it] }.orEmpty()
        return if (byArtist.isEmpty()) byTrack else byTrack + byArtist
    }

    fun labelsFor(track: Track): Set<MoodCategoryKind> = labels[MoodJourneyEngine.keyOf(track)].orEmpty()

    private fun artistKey(track: Track): String? {
        track.artistBrowseIds.firstOrNull { it.isNotBlank() }?.let { return "id:$it" }
        val name = track.artist.split(',', '&').firstOrNull()?.trim()?.lowercase(Locale.ROOT) ?: return null
        return name.takeIf { it.length >= 2 && it !in GenericArtists }?.let { "name:$it" }
    }

    companion object {
        val Empty = MoodStyleEvidence(emptyList())
    }
}

class MoodStyleSource(
    private val gateway: MoodStyleGateway,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val mutex = Mutex()
    private val cache = HashMap<MoodStyle, Pair<Long, MoodStyleShelf>>()

    suspend fun load(style: MoodStyle): MoodStyleShelf {
        mutex.withLock {
            cache[style]?.let { (storedAt, shelf) -> if (clock() - storedAt < TTL_MS) return shelf }
        }
        val fresh = fetch(style)
        if (fresh.playlistsRead > 0) mutex.withLock { cache[style] = clock() to fresh }
        return fresh
    }

    private suspend fun fetch(style: MoodStyle): MoodStyleShelf = coroutineScope {
        val apple = style.appleGenres.firstNotNullOfOrNull { genre ->
            guarded { gateway.applePlaylists(genre) }?.filter { it.playlistId.isNotBlank() }?.takeIf { it.isNotEmpty() }
        }.orEmpty().map { it.copy(apple = true) }
        val category = guarded { gateway.categories() }.orEmpty().firstOrNull { style.matchesCategory(it.title) }
        val editorial = category?.let { guarded { gateway.playlists(it.params) } }.orEmpty()
        val searched = if (apple.size + editorial.size >= MIN_EDITORIAL_PLAYLISTS) {
            emptyList()
        } else {
            val permits = Semaphore(MAX_PARALLEL)
            style.searchQueries.map { query ->
                async { permits.withPermit { guarded { gateway.searchPlaylists(query) } }.orEmpty().take(SEARCH_RESULTS_PER_QUERY) }
            }.awaitAll().flatten()
        }
        val picked = spreadAcrossMoods(apple + editorial + searched)
        if (picked.isEmpty()) return@coroutineScope MoodStyleShelf.empty(style)
        val permits = Semaphore(MAX_PARALLEL)
        val lists = picked.map { ref ->
            async {
                val tracks = permits.withPermit {
                    guarded { if (ref.apple) gateway.appleTracks(ref.playlistId) else gateway.tracks(ref.playlistId) }
                }.orEmpty().take(TRACKS_PER_PLAYLIST)
                val mood = MoodCategoryKind.fromPlaylistTitle(ref.title)
                tracks.map { MoodStyleEntry(it, mood) }
            }
        }.awaitAll()
        MoodStyleShelf(style, lists.flatten(), lists.count { it.isNotEmpty() })
    }

    internal fun spreadAcrossMoods(refs: List<EditorialPlaylistRef>): List<EditorialPlaylistRef> {
        val unique = refs.filter { it.playlistId.isNotBlank() }.distinctBy { (if (it.apple) "apple:" else "") + it.playlistId }
        val byMood = unique.groupBy { MoodCategoryKind.fromPlaylistTitle(it.title) }
        val labelled = byMood.filterKeys { it != null }.values.map { ArrayDeque(it) }
        val picked = ArrayList<EditorialPlaylistRef>(PLAYLISTS_PER_STYLE)
        while (picked.size < PLAYLISTS_PER_STYLE && labelled.any { it.isNotEmpty() }) {
            labelled.forEach { queue -> if (picked.size < PLAYLISTS_PER_STYLE) queue.removeFirstOrNull()?.let(picked::add) }
        }
        byMood[null].orEmpty().forEach { if (picked.size < PLAYLISTS_PER_STYLE) picked += it }
        return picked
    }

    private suspend fun <T> guarded(block: suspend () -> T): T? = try {
        withTimeoutOrNull(REQUEST_TIMEOUT_MS) { block() }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        null
    }

    internal companion object {
        const val TTL_MS = 12L * 60L * 60L * 1000L
        const val REQUEST_TIMEOUT_MS = 10_000L
        const val MAX_PARALLEL = 6
        const val MIN_EDITORIAL_PLAYLISTS = 6
        const val SEARCH_RESULTS_PER_QUERY = 2
        const val PLAYLISTS_PER_STYLE = 8
        const val TRACKS_PER_PLAYLIST = 40
    }
}

private val GenericArtists = setOf("various artists", "various", "unknown artist", "unknown", "va")

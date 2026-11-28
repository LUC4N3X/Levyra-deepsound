package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.SmartOrbitStore
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.ListeningSignalRanker
import com.luc4n3x.levyra.domain.SmartOrbitCandidate
import com.luc4n3x.levyra.domain.SmartOrbitEngine
import com.luc4n3x.levyra.domain.SmartOrbitPool
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.queue.AutoQueueTombstones
import com.luc4n3x.levyra.player.queue.PersistentQueueEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class PersonalOrbitController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val queueEngine: PersistentQueueEngine,
    private val smartOrbitStore: SmartOrbitStore,
    private val freshDiscoveryCandidates: suspend (List<Track>, Int) -> List<Track>
) {
    @Volatile
    var listeningSignals: com.luc4n3x.levyra.domain.ListeningSignalProfile? = null
    @Volatile var smartOrbitPool: SmartOrbitPool = SmartOrbitPool.Empty
        private set
    @Volatile var smartOrbitDiscoveries: List<Track> = emptyList()
        private set
    private val smartOrbitMutex = Mutex()
    private var smartOrbitRefreshJob: Job? = null
    private val smartOrbitRefreshMutex = Mutex()
    private var smartOrbitApplied = false

    suspend fun reloadPool() {
        smartOrbitMutex.withLock { smartOrbitPool = smartOrbitStore.load() }
    }

    suspend fun recordRelated(track: Track, related: List<Track>) {
        smartOrbitMutex.withLock { smartOrbitPool = smartOrbitStore.recordRelated(track, related) }
    }

    fun refreshSmartOrbit() {
        smartOrbitRefreshJob?.cancel()
        smartOrbitRefreshJob = scope.launch(Dispatchers.Default) {
            smartOrbitRefreshMutex.withLock {
                val freshBlocked = smartOrbitFreshDiscoveryBlock()
                val discoveries = computeSmartOrbitDiscoveries(freshBlocked)
                val retireRestored = !smartOrbitApplied && !smartOrbitPool.isEmpty
                smartOrbitApplied = true
                if (discoveries == smartOrbitDiscoveries && !retireRestored) return@withLock
                smartOrbitDiscoveries = discoveries
                uiState.update { current ->
                    current.copy(personalOrbitTracks = rankSmartOrbit(buildSmartOrbit(current), current.recommendationFeedback))
                }
            }
        }
    }

    private suspend fun smartOrbitFreshDiscoveryBlock(): ((Track) -> Boolean)? {
        val pool = smartOrbitPool
        if (!uiState.value.automationSettings.freshDiscovery.enabled || pool.isEmpty) return null
        val tracks = pool.candidates.map(SmartOrbitCandidate::track)
        val kept = freshDiscoveryCandidates(tracks, 0)
        if (kept.size == tracks.size) return null
        val keptKeys = kept.mapTo(HashSet(kept.size * 2), SmartOrbitEngine::trackKey)
        val excludedKeys = tracks.asSequence().map(SmartOrbitEngine::trackKey).filterNot(keptKeys::contains).toHashSet()
        return { track -> SmartOrbitEngine.trackKey(track) in excludedKeys }
    }

    private fun computeSmartOrbitDiscoveries(freshBlocked: ((Track) -> Boolean)?): List<Track> {
        val pool = smartOrbitPool
        if (pool.isEmpty) return emptyList()
        val snapshot = uiState.value
        val rejected = queueEngine.rejectedAutomaticKeys()
        val profile = listeningSignals?.copy(feedback = snapshot.recommendationFeedback)
        val heardArtistsByTitle = HashMap<String, MutableSet<String>>()
        (snapshot.recentSearches + snapshot.recentListens + snapshot.favorites).forEach { track ->
            val title = LevyraPersonalOrbit.musicTitleKey(track)
            if (title.isNotBlank()) heardArtistsByTitle.getOrPut(title) { HashSet() } += LevyraPersonalOrbit.artistKeys(track)
        }
        return SmartOrbitEngine.discoveries(
            pool = pool,
            profile = profile,
            isBlocked = { track ->
                snapshot.artistExclusions.excludesTrack(track) ||
                    isRejectedAutomatic(track, rejected) ||
                    isHeardRecording(track, heardArtistsByTitle) ||
                    freshBlocked?.invoke(track) == true
            }
        )
    }

    private fun isHeardRecording(track: Track, heardArtistsByTitle: Map<String, Set<String>>): Boolean {
        if (heardArtistsByTitle.isEmpty()) return false
        val artists = heardArtistsByTitle[LevyraPersonalOrbit.musicTitleKey(track)] ?: return false
        val candidateArtists = LevyraPersonalOrbit.artistKeys(track)
        return artists.isEmpty() || candidateArtists.isEmpty() || candidateArtists.any(artists::contains)
    }

    private fun isRejectedAutomatic(track: Track, rejected: Set<String>): Boolean =
        rejected.isNotEmpty() && AutoQueueTombstones.rejectionKeys(track).any(rejected::contains)

    fun smartOrbitExclusion(): (Track) -> Boolean {
        val pool = smartOrbitPool
        val discoveries = smartOrbitDiscoveries
        val rejected = queueEngine.rejectedAutomaticKeys()
        if (pool.isEmpty && rejected.isEmpty()) return { false }
        return { track ->
            isRejectedAutomatic(track, rejected) ||
                (pool.contains(track) && discoveries.none { LevyraPersonalOrbit.sameRecording(it, track) })
        }
    }

    private fun buildSmartOrbit(current: LevyraUiState): List<Track> = LevyraPersonalOrbit.build(
        currentTrack = current.currentTrack,
        recentSearches = current.recentSearches,
        favorites = current.favorites,
        tracks = current.tracks,
        homeSections = current.homeSections,
        charts = current.charts,
        cachedOrbit = current.personalOrbitTracks,
        limit = LevyraPersonalOrbit.DISPLAY_LIMIT,
        languageCode = current.languageCode,
        discoveries = smartOrbitDiscoveries,
        excluded = smartOrbitExclusion()
    )

    fun rankSmartOrbit(
        tracks: List<Track>,
        feedback: com.luc4n3x.levyra.domain.RecommendationFeedback
    ): List<Track> {
        val signals = listeningSignals ?: return tracks
        return ListeningSignalRanker.rank(
            candidates = tracks,
            profile = signals.copy(feedback = feedback),
            limit = tracks.size,
            dropSuppressed = false,
            bonusScores = smartOrbitPool.bonusScores
        )
    }
}

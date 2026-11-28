package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.ListeningPulseStore
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.LevyraMixKind
import com.luc4n3x.levyra.domain.LevyraMixRanker
import com.luc4n3x.levyra.domain.LevyraMixSummary
import com.luc4n3x.levyra.domain.ListeningSignalProfile
import com.luc4n3x.levyra.domain.ListeningSignalRanker
import com.luc4n3x.levyra.domain.RadioMixer
import com.luc4n3x.levyra.domain.SmartOrbitPool
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.buildMixCandidates
import com.luc4n3x.levyra.domain.mixTrackKey
import com.luc4n3x.levyra.feature.radio.isLiveRadio
import com.luc4n3x.levyra.player.queue.PersistentQueueEngine
import com.luc4n3x.levyra.player.queue.PlaybackQueueSnapshot
import com.luc4n3x.levyra.player.queue.RadioSession
import com.luc4n3x.levyra.player.queue.playbackQueueIdentity
import com.luc4n3x.levyra.player.queue.radioMixContext
import com.luc4n3x.levyra.player.queue.radioRecentlyPlayed
import com.luc4n3x.levyra.player.queue.radioSeedFor
import com.luc4n3x.levyra.player.radioTailFetchLimit
import com.luc4n3x.levyra.player.radioTailTargetUpcoming
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val RADIO_TAIL_MAX_APPEND_ROUNDS = 3
private const val RADIO_MIX_LIMIT = 15
private const val RADIO_START_TRACKS = 20
private const val RADIO_START_FETCH = 30
private const val MIX_RADIO_LIMIT = 25
private const val MIX_SEARCH_LIMIT = 40
private const val MIX_HISTORY_DAYS = 180
private const val MIX_SURPRISE_ANCHORS = 3
private const val MIX_UNAVAILABLE_MARKER = "levyra_mix_unavailable"

internal interface RadioHost {
    val smartOrbitPool: SmartOrbitPool

    fun isLocalPlaybackTrack(track: Track): Boolean

    fun lyricsNetworkProfile(): LyricsNetworkProfile

    fun rememberRelatedCandidates(seed: Track, related: List<Track>)

    suspend fun freshDiscoveryCandidates(
        candidates: List<Track>,
        minimumFresh: Int,
        unavailableIds: () -> Set<String>
    ): List<Track>

    fun smartRankingProfile(bonusScores: Map<String, Int>): ListeningSignalProfile?

    fun syncAdvancedQueuePrefetch()

    fun startResolve(track: Track)

    fun rejectGuestLocalMutation(): Boolean

    fun playFrom(list: List<Track>, track: Track, loopOnCompletion: Boolean)
}

internal class RadioController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val queueEngine: PersistentQueueEngine,
    private val repository: YoutubeMusicRepository,
    private val listeningPulseStore: ListeningPulseStore,
    private val host: RadioHost
) {
    private var radioJob: Job? = null
    private val radioSession = AtomicReference<RadioSession?>(null)
    private var levyraMixJob: Job? = null

    fun cancelRadioJob() {
        radioJob?.cancel()
    }

    fun clearRadioJob() {
        radioJob?.cancel()
        radioJob = null
    }

    fun cancelLevyraMix() {
        levyraMixJob?.cancel()
    }

    fun toggleContinuousRadio() {
        if (uiState.value.currentTrack?.isLiveRadio() == true) return
        val enabled = !queueEngine.state.value.radioEnabled
        queueEngine.setRadioEnabled(enabled)
        if (enabled) {
            radioJob?.cancel()
            beginRadioSession()
            ensureRadioTail(force = true)
        } else {
            radioJob?.cancel()
            radioSession.set(null)
        }
    }

    fun startSongRadio() {
        if (uiState.value.currentTrack?.isLiveRadio() == true) return
        if (host.rejectGuestLocalMutation()) return
        if (!queueEngine.state.value.radioEnabled) queueEngine.setRadioEnabled(true)
        radioJob?.cancel()
        beginRadioSession()
        ensureRadioTail(force = true, insertAfterCurrent = true)
    }

    private fun beginRadioSession() {
        val snapshot = queueEngine.state.value
        val seed = snapshot.currentTrack?.takeUnless { it.isLiveRadio() || host.isLocalPlaybackTrack(it) }
        radioSession.set(seed?.let { RadioSession(spaceId = snapshot.spaceId, seed = it) })
    }

    private data class RadioTailRequest(
        val seed: Track,
        val anchor: Track,
        val generation: Long,
        val session: RadioSession?
    )

    fun ensureRadioTail(
        force: Boolean,
        playWhenReady: Boolean = false,
        insertAfterCurrent: Boolean = false
    ) {
        val request = radioTailRequest(force) ?: return
        radioJob = scope.launch(Dispatchers.IO) {
            appendRadioTail(request, playWhenReady, insertAfterCurrent)
        }
    }

    private fun radioTailRequest(force: Boolean): RadioTailRequest? {
        val snapshot = queueEngine.state.value
        val anchor = snapshot.currentTrack ?: return null
        if (anchor.isLiveRadio()) return null
        if (!snapshot.radioEnabled) return null
        val targetUpcoming = radioTailTargetUpcoming(uiState.value.audioSettings.advancedQueuePrefetchTracks)
        if (!force && queueEngine.upcoming(targetUpcoming).size >= targetUpcoming) return null
        if (radioJob?.isActive == true) return null
        if (host.isLocalPlaybackTrack(anchor) || !host.lyricsNetworkProfile().connected) return null
        val session = radioSession.get()?.takeIf { it.isActiveFor(snapshot) }
        val seed = radioSeedFor(session, snapshot) ?: return null
        return RadioTailRequest(seed = seed, anchor = anchor, generation = snapshot.generation, session = session)
    }

    private suspend fun appendRadioTail(
        request: RadioTailRequest,
        playWhenReady: Boolean,
        insertAfterCurrent: Boolean = false
    ) {
        val targetUpcoming = radioTailTargetUpcoming(uiState.value.audioSettings.advancedQueuePrefetchTracks)
        val currentUpcoming = queueEngine.upcoming(targetUpcoming).size
        val tailFetchLimit = radioTailFetchLimit(targetUpcoming, currentUpcoming)
        val fetchLimit = if (insertAfterCurrent) maxOf(tailFetchLimit, RADIO_START_FETCH) else tailFetchLimit
        val fetchedRadioTracks = fetchRadioTracks(request, fetchLimit)
        host.rememberRelatedCandidates(request.seed, fetchedRadioTracks)
        val candidateRadioTracks = host.freshDiscoveryCandidates(
            candidates = fetchedRadioTracks,
            minimumFresh = if (insertAfterCurrent) targetUpcoming else (targetUpcoming - currentUpcoming).coerceAtLeast(1),
            unavailableIds = { radioQueuedIds(request.seed) }
        )
        val current = currentRadioSnapshot(request) ?: return
        val mixedRadioTracks = mixRadioTracks(request, current, candidateRadioTracks, insertAfterCurrent)
        appendMixedRadioTracks(mixedRadioTracks, targetUpcoming, insertAfterCurrent)
        updateRadioSession(request, fetchedRadioTracks, mixedRadioTracks)
        continueAfterRadioAppend(mixedRadioTracks, playWhenReady)
    }

    private suspend fun fetchRadioTracks(request: RadioTailRequest, fetchLimit: Int): List<Track> =
        try {
            repository.radio(
                seed = request.seed,
                languageCode = uiState.value.languageCode,
                limit = fetchLimit,
                excludeIds = request.session?.addedIds.orEmpty(),
                diversify = true
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyList()
        }

    private fun currentRadioSnapshot(request: RadioTailRequest): PlaybackQueueSnapshot? {
        val current = queueEngine.state.value
        return current.takeIf {
            it.radioEnabled &&
                isSameRadioSeed(it.currentTrack, request.anchor) &&
                it.generation == request.generation
        }
    }

    private fun mixRadioTracks(
        request: RadioTailRequest,
        current: PlaybackQueueSnapshot,
        fetchedRadioTracks: List<Track>,
        insertAfterCurrent: Boolean
    ): List<Track> {
        val radioTracks = uiState.value.artistExclusions.filterTracks(fetchedRadioTracks)
        val bonusScores = host.smartOrbitPool.bonusScores
        val orderedRadioTracks = host.smartRankingProfile(bonusScores)?.let { signals ->
            ListeningSignalRanker.rank(
                candidates = radioTracks,
                profile = signals,
                limit = radioTracks.size,
                bonusScores = bonusScores
            )
        } ?: radioTracks
        return RadioMixer.mix(
            candidates = orderedRadioTracks,
            seed = request.seed,
            queued = current.tracks,
            recentContext = radioMixContext(current, insertAfterCurrent),
            recentlyPlayed = radioRecentlyPlayed(current),
            limit = if (insertAfterCurrent) RADIO_START_TRACKS else RADIO_MIX_LIMIT
        )
    }

    private fun appendMixedRadioTracks(
        tracks: List<Track>,
        targetUpcoming: Int,
        insertAfterCurrent: Boolean
    ) {
        if (tracks.isEmpty()) return
        if (insertAfterCurrent) {
            queueEngine.insertRadioTracksAfterCurrent(tracks, RADIO_START_TRACKS)
        } else {
            appendRadioTracksUntil(tracks, targetUpcoming)
        }
    }

    private fun updateRadioSession(
        request: RadioTailRequest,
        fetchedRadioTracks: List<Track>,
        mixedRadioTracks: List<Track>
    ) {
        val session = request.session?.takeIf { fetchedRadioTracks.isNotEmpty() } ?: return
        val queuedIds = queueEngine.state.value.tracks.mapTo(HashSet(), Track::id)
        val added = mixedRadioTracks.map(Track::id).filter { it in queuedIds && it !in session.addedIds }
        radioSession.compareAndSet(session, session.recordAdded(added))
    }

    private suspend fun continueAfterRadioAppend(tracks: List<Track>, playWhenReady: Boolean) {
        if (tracks.isEmpty()) return
        if (!playWhenReady) {
            if (uiState.value.audioSettings.advancedQueuePrefetchTracks > 0) {
                withContext(Dispatchers.Main) { host.syncAdvancedQueuePrefetch() }
            }
            return
        }
        withContext(Dispatchers.Main) {
            queueEngine.next(respectRepeatOne = false)?.let(host::startResolve)
        }
    }

    private fun appendRadioTracksUntil(tracks: List<Track>, targetUpcoming: Int) {
        repeat(RADIO_TAIL_MAX_APPEND_ROUNDS) {
            val before = queueEngine.upcoming(targetUpcoming).size
            queueEngine.appendRadioTracks(tracks)
            val after = queueEngine.upcoming(targetUpcoming).size
            if (after <= before || after >= targetUpcoming) return
        }
    }

    private fun radioQueuedIds(seed: Track): Set<String> {
        val tracks = queueEngine.state.value.tracks
        val ids = HashSet<String>(tracks.size + 1)
        tracks.mapNotNullTo(ids) { it.id.takeIf(String::isNotBlank) }
        seed.id.takeIf(String::isNotBlank)?.let(ids::add)
        return ids
    }

    private fun isSameRadioSeed(current: Track?, seed: Track): Boolean =
        current?.let { playbackQueueIdentity(it) } == playbackQueueIdentity(seed)

    fun setMixFamiliarity(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (uiState.value.mixFamiliarity == clamped) return
        uiState.update { it.copy(mixFamiliarity = clamped) }
    }

    fun clearMixMessage() {
        if (uiState.value.mixMessage == null) return
        uiState.update { it.copy(mixMessage = null) }
    }

    fun startLevyraMix(
        kind: LevyraMixKind,
        seedTrack: Track? = null,
        seedQuery: String = "",
        label: String = ""
    ) {
        levyraMixJob?.cancel()
        levyraMixJob = scope.launch {
            uiState.update { it.copy(mixLoading = true, mixMessage = null) }
            try {
                val snapshot = uiState.value
                val seed = seedTrack
                    ?: snapshot.currentTrack
                    ?: snapshot.recentListens.firstOrNull()
                    ?: snapshot.mostPlayedTracks.firstOrNull()
                val pool = snapshot.artistExclusions.filterTracks(
                    collectMixPool(kind, seed, seedQuery, snapshot)
                )
                if (pool.isEmpty()) {
                    uiState.update { it.copy(mixLoading = false, mixMessage = MIX_UNAVAILABLE_MARKER) }
                    return@launch
                }
                val listens = listeningPulseStore.eventsWindow(MIX_HISTORY_DAYS)
                val avoidRecent = kind == LevyraMixKind.SurpriseMe || kind == LevyraMixKind.Rediscover
                val ranked = withContext(Dispatchers.Default) {
                    val candidates = buildMixCandidates(pool, listens, System.currentTimeMillis())
                    LevyraMixRanker
                        .rank(candidates, snapshot.mixFamiliarity, excludeRecent = avoidRecent)
                        .ifEmpty { LevyraMixRanker.rank(candidates, snapshot.mixFamiliarity) }
                }
                if (ranked.isEmpty()) {
                    uiState.update { it.copy(mixLoading = false, mixMessage = MIX_UNAVAILABLE_MARKER) }
                    return@launch
                }
                uiState.update {
                    it.copy(
                        mixLoading = false,
                        activeMix = LevyraMixSummary(
                            kind = kind,
                            label = label,
                            seedTrackId = seed?.id.orEmpty(),
                            trackCount = ranked.size
                        )
                    )
                }
                host.playFrom(ranked, ranked.first(), loopOnCompletion = true)
            } catch (cancelled: CancellationException) {
                uiState.update { it.copy(mixLoading = false) }
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Levyra mix build failed")
                uiState.update { it.copy(mixLoading = false, mixMessage = MIX_UNAVAILABLE_MARKER) }
            }
        }
    }

    private suspend fun collectMixPool(
        kind: LevyraMixKind,
        seed: Track?,
        seedQuery: String,
        snapshot: LevyraUiState
    ): List<Track> {
        val languageCode = snapshot.languageCode
        return when (kind) {
            LevyraMixKind.SimilarTrack, LevyraMixKind.Personalized ->
                seed?.let { repository.radio(it, languageCode, MIX_RADIO_LIMIT) }.orEmpty()

            LevyraMixKind.SimilarArtist -> {
                val artist = seedQuery.trim().ifEmpty { seed?.artist.orEmpty() }.trim()
                if (artist.isEmpty()) {
                    emptyList()
                } else {
                    val fromRadio = seed?.let { repository.radio(it, languageCode, MIX_RADIO_LIMIT) }.orEmpty()
                    fromRadio + repository.search(artist, MIX_SEARCH_LIMIT, languageCode)
                }
            }

            LevyraMixKind.Genre -> {
                val query = seedQuery.trim()
                if (query.isEmpty()) {
                    emptyList()
                } else {
                    repository.search(query, MIX_SEARCH_LIMIT, languageCode)
                }
            }

            LevyraMixKind.Rediscover -> {
                val recentKeys = snapshot.recentListens.mapTo(HashSet()) { mixTrackKey(it) }
                listeningPulseStore.mostPlayedTracks(days = MIX_HISTORY_DAYS, limit = MIX_SEARCH_LIMIT)
                    .filterNot { mixTrackKey(it) in recentKeys }
            }

            LevyraMixKind.CurrentRotation ->
                snapshot.mostPlayedTracks + snapshot.recentListens

            LevyraMixKind.SurpriseMe -> {
                val anchors = (snapshot.mostPlayedTracks + snapshot.recentListens)
                    .distinctBy { mixTrackKey(it) }
                    .take(MIX_SURPRISE_ANCHORS)
                val radio = ArrayList<Track>(MIX_RADIO_LIMIT * MIX_SURPRISE_ANCHORS)
                for (anchor in anchors) {
                    radio.addAll(repository.radio(anchor, languageCode, MIX_RADIO_LIMIT))
                }
                radio + snapshot.exploreFreshTracks + snapshot.homeResonanceTracks
            }
        }
    }
}

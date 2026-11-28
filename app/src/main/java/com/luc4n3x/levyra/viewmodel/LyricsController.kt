package com.luc4n3x.levyra.viewmodel

import android.content.Context
import com.luc4n3x.levyra.data.LyricsPhraseOutcome
import com.luc4n3x.levyra.data.LyricsPhraseSearchRepository
import com.luc4n3x.levyra.data.LyricsRepository
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.LevyraLocalIntelligence
import com.luc4n3x.levyra.domain.LyricsPhraseMatcher
import com.luc4n3x.levyra.domain.LyricsTranslationState
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.AdaptivePlaybackPolicy
import com.luc4n3x.levyra.player.PlaybackService
import com.luc4n3x.levyra.player.queue.PersistentQueueEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

internal data class LyricsNetworkProfile(
    val connected: Boolean,
    val unmetered: Boolean
)

internal class LyricsController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val context: Context,
    private val repository: YoutubeMusicRepository,
    private val lyricsRepository: LyricsRepository,
    private val playbackGeneration: PlaybackGenerationGuard,
    private val queueEngine: PersistentQueueEngine,
    private val adaptivePlaybackPolicy: AdaptivePlaybackPolicy,
    private val localIntelligence: LevyraLocalIntelligence,
    private val lyricsNetworkProfile: () -> LyricsNetworkProfile
) {
    private val _lyricsPhraseSearch = MutableStateFlow(LyricsPhraseSearchUiState())
    val phraseSearch: StateFlow<LyricsPhraseSearchUiState> = _lyricsPhraseSearch.asStateFlow()
    private var lyricsPhraseSearchJob: Job? = null
    private val lyricsPhraseSearchRepository by lazy {
        LyricsPhraseSearchRepository(
            searchCandidates = { phrase, limit -> repository.search(phrase, limit, uiState.value.languageCode) },
            fetchLyricLines = { track ->
                lyricsRepository.fetch(
                    title = track.title,
                    artist = track.artist,
                    durationSec = track.durationMs / 1000L,
                    album = track.album,
                    videoId = track.id
                )?.lines?.map { it.text }
            },
            isOnline = {
                val connectivity = context.getSystemService(android.net.ConnectivityManager::class.java)
                connectivity?.getNetworkCapabilities(connectivity.activeNetwork)
                    ?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            }
        )
    }
    private var lyricsJob: Job? = null
    private var lyricsVersionsJob: Job? = null
    private var lyricsPrefetchJob: Job? = null
    private var lyricsTrackId: String = ""
    private var lyricsRequestGeneration = 0L
    private var lyricsVersionsGeneration = 0L
    private val _customLyricsDraft = MutableStateFlow<CustomLyricsDraft?>(null)
    private var customLyricsSession = 0L
    val customDraft: StateFlow<CustomLyricsDraft?> = _customLyricsDraft.asStateFlow()

    fun cancelPrefetch() {
        lyricsPrefetchJob?.cancel()
        lyricsPrefetchJob = null
    }

    fun cancelRequests() {
        lyricsJob?.cancel()
        lyricsPrefetchJob?.cancel()
    }

    fun openPhraseSearch() {
        _lyricsPhraseSearch.update { it.copy(visible = true) }
    }

    fun closePhraseSearch() {
        lyricsPhraseSearchJob?.cancel()
        _lyricsPhraseSearch.value = LyricsPhraseSearchUiState()
    }

    fun searchPhrase(phrase: String) {
        lyricsPhraseSearchJob?.cancel()
        if (!LyricsPhraseMatcher.isSearchable(phrase)) {
            _lyricsPhraseSearch.update { it.copy(query = phrase, status = LyricsPhraseSearchStatus.TOO_SHORT, hits = emptyList()) }
            return
        }
        _lyricsPhraseSearch.update { it.copy(query = phrase, status = LyricsPhraseSearchStatus.SEARCHING, hits = emptyList()) }
        lyricsPhraseSearchJob = scope.launch {
            val outcome = try {
                lyricsPhraseSearchRepository.search(phrase)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "lyrics phrase search failed")
                LyricsPhraseOutcome.ProviderUnavailable
            }
            _lyricsPhraseSearch.update { current ->
                when (outcome) {
                    is LyricsPhraseOutcome.Results -> current.copy(
                        status = if (outcome.verificationAvailable) LyricsPhraseSearchStatus.RESULTS else LyricsPhraseSearchStatus.RESULTS_UNVERIFIED,
                        hits = outcome.hits
                    )
                    LyricsPhraseOutcome.NoMatch -> current.copy(status = LyricsPhraseSearchStatus.NO_MATCH, hits = emptyList())
                    LyricsPhraseOutcome.ProviderUnavailable -> current.copy(status = LyricsPhraseSearchStatus.UNAVAILABLE, hits = emptyList())
                }
            }
        }
    }

    fun open() {
        uiState.update { it.copy(showLyrics = true) }
        val snapshot = uiState.value
        val track = snapshot.currentTrack
        if (track != null && snapshot.lyrics.isEmpty() && !snapshot.lyricsLoading) {
            fetch(track)
        }
    }

    fun close() {
        lyricsVersionsJob?.cancel()
        lyricsVersionsGeneration++
        _customLyricsDraft.value = null
        uiState.update { it.copy(showLyrics = false) }
    }

    fun loadVersions() {
        val track = uiState.value.currentTrack ?: return
        lyricsVersionsJob?.cancel()
        val generation = ++lyricsVersionsGeneration
        val trackIdentity = playbackIdentity(track)
        uiState.update { it.copy(lyricsVersionsLoading = true) }
        lyricsVersionsJob = scope.launch {
            try {
                val versions = lyricsRepository.versions(
                    title = track.title,
                    artist = track.artist,
                    durationSec = track.durationMs / 1_000L,
                    album = track.album,
                    videoId = youtubePlayableTrack(track)?.id.orEmpty(),
                    languageCode = uiState.value.languageCode,
                    translate = uiState.value.lyricsTranslationEnabled
                )
                val currentIdentity = uiState.value.currentTrack?.let(::playbackIdentity)
                if (generation != lyricsVersionsGeneration || currentIdentity != trackIdentity) return@launch
                uiState.update {
                    it.copy(
                        lyricsVersions = versions,
                        lyricsVersionsLoading = false,
                        lyricsManualSelection = versions.any { version -> version.selected }
                    )
                }
            } finally {
                val currentIdentity = uiState.value.currentTrack?.let(::playbackIdentity)
                if (generation == lyricsVersionsGeneration && currentIdentity == trackIdentity) {
                    uiState.update { it.copy(lyricsVersionsLoading = false) }
                }
            }
        }
    }

    fun selectVersion(version: LyricsRepository.LyricsVersion) {
        val track = uiState.value.currentTrack ?: return
        lyricsVersionsJob?.cancel()
        val generation = ++lyricsVersionsGeneration
        val trackIdentity = playbackIdentity(track)
        lyricsVersionsJob = scope.launch {
            val result = lyricsRepository.selectVersion(
                title = track.title,
                artist = track.artist,
                durationSec = track.durationMs / 1_000L,
                album = track.album,
                videoId = youtubePlayableTrack(track)?.id.orEmpty(),
                languageCode = uiState.value.languageCode,
                translate = uiState.value.lyricsTranslationEnabled,
                version = version
            ) ?: return@launch
            val currentIdentity = uiState.value.currentTrack?.let(::playbackIdentity)
            if (generation != lyricsVersionsGeneration || currentIdentity != trackIdentity) return@launch
            lyricsTrackId = trackIdentity
            uiState.update {
                it.copy(
                    lyrics = result.lines,
                    lyricsSections = result.sections,
                    lyricsSynced = result.synced,
                    lyricsProvider = result.provider,
                    lyricsTranslationState = result.translationState,
                    lyricsConfidence = result.confidence,
                    lyricsCached = result.cached,
                    lyricsManualSelection = true,
                    lyricsVersions = it.lyricsVersions.map { candidate ->
                        candidate.copy(selected = candidate.id == version.id)
                    }
                )
            }
            PlaybackService.publishSystemLyrics(
                track = track,
                lines = result.lines,
                synced = result.synced,
                provider = result.provider
            )
            val intelligence = withContext(Dispatchers.Default) { localIntelligence.analyze(track, result.lines) }
            if (generation == lyricsVersionsGeneration && uiState.value.currentTrack?.let(::playbackIdentity) == trackIdentity) {
                uiState.update { it.copy(intelligenceSummary = intelligence) }
            }
        }
    }

    fun useAutomatic() {
        val track = uiState.value.currentTrack ?: return
        lyricsVersionsJob?.cancel()
        val generation = ++lyricsVersionsGeneration
        val trackIdentity = playbackIdentity(track)
        lyricsVersionsJob = scope.launch {
            lyricsRepository.useAutomatic(
                title = track.title,
                artist = track.artist,
                durationSec = track.durationMs / 1_000L,
                album = track.album,
                videoId = youtubePlayableTrack(track)?.id.orEmpty(),
                languageCode = uiState.value.languageCode,
                translate = uiState.value.lyricsTranslationEnabled
            )
            if (generation != lyricsVersionsGeneration || uiState.value.currentTrack?.let(::playbackIdentity) != trackIdentity) return@launch
            uiState.update { it.copy(lyricsManualSelection = false, lyricsVersions = emptyList()) }
            fetch(track)
        }
    }

    fun openCustomEditor() {
        val track = uiState.value.currentTrack ?: return
        val trackIdentity = playbackIdentity(track)
        scope.launch {
            val source = lyricsRepository.customLyricsSource(
                title = track.title,
                artist = track.artist,
                durationSec = track.durationMs / 1_000L,
                videoId = youtubePlayableTrack(track)?.id.orEmpty()
            )
            if (uiState.value.currentTrack?.let(::playbackIdentity) != trackIdentity) return@launch
            _customLyricsDraft.value = CustomLyricsDraft(
                track = track,
                source = source.orEmpty(),
                existing = source != null,
                session = ++customLyricsSession
            )
        }
    }

    fun dismissCustomEditor() {
        _customLyricsDraft.value = null
    }

    fun saveCustom(source: String) {
        val draft = _customLyricsDraft.value ?: return
        if (draft.saving) return
        _customLyricsDraft.value = draft.copy(source = source, saving = true, failed = false)
        scope.launch {
            val track = draft.track
            val saved = lyricsRepository.saveCustomLyrics(
                title = track.title,
                artist = track.artist,
                durationSec = track.durationMs / 1_000L,
                videoId = youtubePlayableTrack(track)?.id.orEmpty(),
                source = source
            )
            finishCustomEdit(draft, saved)
        }
    }

    fun deleteCustom() {
        val draft = _customLyricsDraft.value ?: return
        if (draft.saving) return
        _customLyricsDraft.value = draft.copy(saving = true, failed = false)
        scope.launch {
            val track = draft.track
            val deleted = lyricsRepository.deleteCustomLyrics(
                title = track.title,
                artist = track.artist,
                durationSec = track.durationMs / 1_000L,
                videoId = youtubePlayableTrack(track)?.id.orEmpty()
            )
            finishCustomEdit(draft, deleted)
        }
    }

    private fun finishCustomEdit(draft: CustomLyricsDraft, succeeded: Boolean) {
        if (!succeeded) {
            _customLyricsDraft.update { current ->
                if (current?.session == draft.session) current.copy(saving = false, failed = true) else current
            }
            return
        }
        _customLyricsDraft.update { current -> if (current?.session == draft.session) null else current }
        val current = uiState.value.currentTrack ?: return
        if (playbackIdentity(current) != playbackIdentity(draft.track)) return
        lyricsTrackId = ""
        fetch(current)
    }

    fun fetch(track: Track) {
        lyricsJob?.cancel()
        val requestGeneration = ++lyricsRequestGeneration
        val ticket = playbackGeneration.current()
        val trackIdentity = playbackIdentity(track)
        val preserveVisibleLyrics = lyricsTrackId == trackIdentity && uiState.value.lyrics.isNotEmpty()
        uiState.update {
            it.copy(
                lyrics = if (preserveVisibleLyrics) it.lyrics else emptyList(),
                lyricsSections = if (preserveVisibleLyrics) it.lyricsSections else emptyList(),
                lyricsSynced = if (preserveVisibleLyrics) it.lyricsSynced else false,
                lyricsProvider = if (preserveVisibleLyrics) it.lyricsProvider else "",
                lyricsTranslationState = if (preserveVisibleLyrics) {
                    it.lyricsTranslationState
                } else {
                    LyricsTranslationState.DISABLED
                },
                lyricsConfidence = if (preserveVisibleLyrics) it.lyricsConfidence else 0,
                lyricsCached = if (preserveVisibleLyrics) it.lyricsCached else false,
                lyricsVersions = emptyList(),
                lyricsVersionsLoading = false,
                lyricsManualSelection = if (preserveVisibleLyrics) it.lyricsManualSelection else false,
                lyricsLoading = true,
                intelligenceSummary = if (preserveVisibleLyrics) it.intelligenceSummary else com.luc4n3x.levyra.domain.LevyraIntelligenceSummary()
            )
        }
        lyricsJob = scope.launch {
            var received = false
            try {
                lyricsRepository.observe(
                    title = track.title,
                    artist = track.artist,
                    durationSec = track.durationMs / 1000L,
                    album = track.album,
                    videoId = youtubePlayableTrack(track)?.id.orEmpty(),
                    languageCode = uiState.value.languageCode,
                    translate = uiState.value.lyricsTranslationEnabled
                ).collect { result ->
                    val currentIdentity = uiState.value.currentTrack?.let(::playbackIdentity)
                    if (!playbackGeneration.isCurrent(ticket) || !isCurrentLyricsRequest(requestGeneration, lyricsRequestGeneration, trackIdentity, currentIdentity)) {
                        return@collect
                    }
                    received = true
                    lyricsTrackId = trackIdentity
                    val lines = result.lines
                    uiState.update {
                        it.copy(
                            lyrics = lines,
                            lyricsSections = result.sections,
                            lyricsSynced = result.synced,
                            lyricsProvider = result.provider,
                            lyricsTranslationState = result.translationState,
                            lyricsConfidence = result.confidence,
                            lyricsCached = result.cached,
                            lyricsManualSelection = result.manualSelection,
                            lyricsLoading = false
                        )
                    }
                    PlaybackService.publishSystemLyrics(
                        track = track,
                        lines = lines,
                        synced = result.synced,
                        provider = result.provider
                    )
                    val intelligence = withContext(Dispatchers.Default) { localIntelligence.analyze(track, lines) }
                    val latestIdentity = uiState.value.currentTrack?.let(::playbackIdentity)
                    if (playbackGeneration.isCurrent(ticket) && isCurrentLyricsRequest(requestGeneration, lyricsRequestGeneration, trackIdentity, latestIdentity)) {
                        uiState.update { it.copy(intelligenceSummary = intelligence) }
                    }
                }
            } finally {
                val currentIdentity = uiState.value.currentTrack?.let(::playbackIdentity)
                if (isCurrentLyricsRequest(requestGeneration, lyricsRequestGeneration, trackIdentity, currentIdentity)) {
                    uiState.update { it.copy(lyricsLoading = false) }
                    if (!received && uiState.value.lyrics.isEmpty()) {
                        val intelligence = withContext(Dispatchers.Default) { localIntelligence.analyze(track, emptyList()) }
                        val latestIdentity = uiState.value.currentTrack?.let(::playbackIdentity)
                        if (isCurrentLyricsRequest(requestGeneration, lyricsRequestGeneration, trackIdentity, latestIdentity)) {
                            uiState.update { it.copy(intelligenceSummary = intelligence) }
                        }
                    }
                }
            }
        }
    }

    fun prefetchAround(current: Track) {
        lyricsPrefetchJob?.cancel()
        val generation = queueEngine.state.value.generation
        val plan = adaptivePlaybackPolicy.current(videoMode = false)
        val networkProfile = lyricsNetworkProfile()
        if (!networkProfile.connected) return
        val unmetered = networkProfile.unmetered
        val limit = when {
            plan.powerConstrained || plan.lowRam -> 1
            unmetered -> 2
            else -> 1
        }
        val upcoming = queueEngine.upcoming(limit + 1)
            .filterNot { playbackIdentity(it) == playbackIdentity(current) }
            .distinctBy { playbackIdentity(it) }
            .take(limit)
        if (upcoming.isEmpty()) return
        val languageCode = uiState.value.languageCode
        val translate = uiState.value.lyricsTranslationEnabled
        lyricsPrefetchJob = scope.launch(Dispatchers.IO) {
            delay(if (plan.powerConstrained) 1_500L else 550L)
            upcoming.forEachIndexed { index, track ->
                if (!isActive || queueEngine.state.value.generation != generation) return@launch
                if (index > 0) delay(if (unmetered) 650L else 1_200L)
                runCatching {
                    lyricsRepository.prefetch(
                        title = track.title,
                        artist = track.artist,
                        durationSec = track.durationMs / 1_000L,
                        album = track.album,
                        videoId = youtubePlayableTrack(track)?.id.orEmpty(),
                        languageCode = languageCode,
                        translate = translate
                    )
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    Timber.d(error, "Lyrics prefetch failed for %s", track.id)
                }
            }
        }
    }
}

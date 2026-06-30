package com.luc4n3x.levyra.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.luc4n3x.levyra.data.AppUpdateRepository
import com.luc4n3x.levyra.data.ChartsRepository
import com.luc4n3x.levyra.data.FavoritesStore
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.LyricsRepository
import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.data.SponsorBlockRepository
import com.luc4n3x.levyra.data.TrackPayloadCodec
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.domain.SponsorSegment
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.LyricsEngine
import com.luc4n3x.levyra.domain.Mood
import com.luc4n3x.levyra.domain.MoodEngine
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.LevyraPlayer
import com.luc4n3x.levyra.player.offline.OfflineAudioExporter
import com.luc4n3x.levyra.player.offline.work.OfflineExportWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber

class LevyraViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YoutubeMusicRepository()
    private val chartsRepository = ChartsRepository()
    private val appUpdateRepository = AppUpdateRepository(application.applicationContext)
    private val lyricsRepository = LyricsRepository()
    private val sponsorBlockRepository = SponsorBlockRepository()
    private val resolver = PlaybackResolver.getInstance(application.applicationContext)
    private val moodEngine = MoodEngine()
    private val lyricsEngine = LyricsEngine()
    private val player = LevyraPlayer(application.applicationContext)
    private val offlineExporter = OfflineAudioExporter(application.applicationContext, resolver)
    private val favoritesStore = FavoritesStore(application.applicationContext)
    private val preferences = LevyraPreferences(application.applicationContext)
    private val _state = MutableStateFlow(
        LevyraUiState(
            moods = moodEngine.moods,
            tastes = moodEngine.tastes,
            chartRegions = ChartsCatalog.regions,
            selectedMood = moodEngine.moods.firstOrNull(),
            isSearching = true,
            embeddedMetadataWriterReady = offlineExporter.embeddedMetadataWriterReady
        )
    )
    private var searchJob: Job? = null
    private var playJob: Job? = null
    private var prefetchJob: Job? = null
    private var chartEnrichJob: Job? = null
    private var sleepJob: Job? = null
    private var lyricsJob: Job? = null
    private var sponsorJob: Job? = null
    private var listPrefetchJob: Job? = null
    private var offlineExportJob: Job? = null
    private var updateJob: Job? = null
    private var sponsorSegments: List<SponsorSegment> = emptyList()
    private val tabBackStack = ArrayDeque<LevyraTab>()
    private var playRequestId: Long = 0L
    private var pendingSeekMs: Long = 0L
    private var queueIndex: Int = -1

    val state: StateFlow<LevyraUiState> = _state.asStateFlow()

    init {
        val favorites = favoritesStore.load()
        val onboarded = preferences.isOnboarded()
        val recentSearches = preferences.loadRecentSearches()
        _state.update {
            it.copy(
                favorites = favorites,
                favoriteIds = favorites.map { fav -> fav.id }.toSet(),
                recentSearches = recentSearches,
                userName = preferences.userName(),
                animationsEnabled = preferences.animationsEnabled(),
                dynamicColor = preferences.dynamicColor(),
                sponsorBlockEnabled = preferences.sponsorBlock(),
                skipSilence = preferences.skipSilence(),
                showOnboarding = !onboarded,
                currentTrack = null,
                positionMs = 0L,
                durationMs = 0L,
                lyrics = emptyList()
            )
        }
        player.setSkipSilence(preferences.skipSilence())
        player.onCompletion = { onTrackCompleted() }
        player.onError = { errorMsg ->
            _state.update { it.copy(playerError = errorMsg, isPlaying = false, isResolving = false) }
        }
        loadHomeFeed()
        loadCharts()
        startTicker()
        checkForUpdates(silent = true)
    }

    private fun onTrackCompleted() {
        val snapshot = _state.value
        val current = snapshot.currentTrack ?: return
        if (snapshot.isResolving || playJob?.isActive == true || current.streamUrl.isBlank()) return
        val duration = effectiveDuration(current)
        if (duration > 0L && player.positionMs < (duration - 1_500L).coerceAtLeast(0L)) return
        val queue = snapshot.queue.ifEmpty { currentQueue() }
        when {
            _state.value.shuffleEnabled && queue.isNotEmpty() -> {
                queueIndex = queue.indices.random()
                startResolve(queue[queueIndex])
            }
            _state.value.repeatMode == RepeatMode.All -> next()
            queueIndex in 0 until queue.lastIndex -> next()
            else -> {
                player.pause()
                _state.update { it.copy(isPlaying = false, positionMs = 0L) }
            }
        }
    }

    fun toggleRepeat() {
        val mode = when (_state.value.repeatMode) {
            RepeatMode.Off -> RepeatMode.All
            RepeatMode.All -> RepeatMode.One
            RepeatMode.One -> RepeatMode.Off
        }
        player.setRepeatOne(mode == RepeatMode.One)
        _state.update { it.copy(repeatMode = mode) }
    }

    fun toggleShuffle() {
        _state.update { it.copy(shuffleEnabled = !it.shuffleEnabled) }
    }

    fun cycleSpeed() {
        val steps = listOf(1f, 1.25f, 1.5f, 2f, 0.75f)
        val current = _state.value.playbackSpeed
        val next = steps[(steps.indexOf(current).coerceAtLeast(0) + 1) % steps.size]
        player.setSpeed(next)
        _state.update { it.copy(playbackSpeed = next) }
    }

    fun cycleSleepTimer() {
        val steps = listOf(0, 15, 30, 60)
        val current = _state.value.sleepTimerMinutes
        val next = steps[(steps.indexOf(current).coerceAtLeast(0) + 1) % steps.size]
        setSleepTimer(next)
    }

    private fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        _state.update { it.copy(sleepTimerMinutes = minutes) }
        if (minutes <= 0) return
        sleepJob = viewModelScope.launch {
            delay(minutes * 60_000L)
            player.pause()
            _state.update { it.copy(isPlaying = false, sleepTimerMinutes = 0) }
        }
    }

    fun completeOnboarding(name: String, tasteIds: Set<String>) {
        preferences.setUserName(name.trim())
        preferences.setOnboarded(tasteIds)
        _state.update { it.copy(showOnboarding = false, userName = name.trim()) }
        loadHomeFeed()
    }

    /** Loads the real YouTube Music home feed (sections), falling back to taste-based search. */
    private fun loadHomeFeed() {
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, searchError = null) }
            val sections = runCatching { repository.homeFeed() }.getOrDefault(emptyList())
            if (sections.isEmpty()) {
                loadHome()
                return@launch
            }
            val flat = sections.flatMap { it.tracks }.distinctBy { it.id }
            val queue = moodEngine.buildQueue(_state.value.selectedMood, flat)
            _state.update {
                it.copy(
                    homeSections = sections,
                    tracks = flat,
                    queue = queue,
                    searchResults = flat.take(12),
                    isSearching = false,
                    cacheReport = repository.cacheReport(),
                    searchError = null
                )
            }
            prefetchTop(sections.first().tracks, 16)
        }
    }

    fun openSettings() {
        _state.update { it.copy(showSettings = true) }
    }

    fun closeSettings() {
        _state.update { it.copy(showSettings = false) }
    }

    fun checkForUpdates(silent: Boolean = false) {
        if (updateJob?.isActive == true) return
        updateJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isCheckingUpdates = true,
                    updateMessage = if (silent) it.updateMessage else null
                )
            }
            val result = runCatching { appUpdateRepository.latest() }
            result.onSuccess { info ->
                val dismissed = preferences.dismissedUpdateVersion()
                _state.update {
                    it.copy(
                        updateInfo = info,
                        isCheckingUpdates = false,
                        showUpdatePrompt = info.isNewer && (!silent || dismissed != info.latestVersionName),
                        updateMessage = when {
                            silent -> null
                            info.isNewer -> "LEVYRA ${info.latestVersionName} è disponibile"
                            else -> "LEVYRA è già aggiornata"
                        }
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _state.update {
                    it.copy(
                        isCheckingUpdates = false,
                        updateMessage = if (silent) null else error.message ?: "Controllo aggiornamenti non riuscito"
                    )
                }
            }
        }
    }

    fun dismissUpdatePrompt() {
        _state.value.updateInfo?.latestVersionName?.let { preferences.setDismissedUpdateVersion(it) }
        _state.update { it.copy(showUpdatePrompt = false) }
    }

    fun clearUpdateMessage() {
        _state.update { it.copy(updateMessage = null) }
    }

    fun setAnimationsEnabled(value: Boolean) {
        preferences.setAnimationsEnabled(value)
        _state.update { it.copy(animationsEnabled = value) }
    }

    fun setDynamicColor(value: Boolean) {
        preferences.setDynamicColor(value)
        _state.update { it.copy(dynamicColor = value) }
    }

    fun restartOnboarding() {
        _state.update { it.copy(showSettings = false, showOnboarding = true) }
    }

    fun playAll(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        addToRecentSearches(tracks.first())
        _state.update { it.copy(queue = tracks) }
        queueIndex = 0
        startResolve(tracks.first())
    }

    fun addToQueue(track: Track) {
        val current = _state.value.queue.ifEmpty { currentQueue() }
        val updated = (current + track).distinctBy { it.id }
        _state.update {
            it.copy(
                queue = updated,
                offlineExportMessage = "Aggiunto alla coda: ${track.title}"
            )
        }
    }

    fun selectChart(regionId: String) {
        if (regionId == _state.value.selectedChartId && _state.value.charts.isNotEmpty()) return
        _state.update { it.copy(selectedChartId = regionId) }
        loadCharts(regionId)
    }

    private fun loadCharts(regionId: String = _state.value.selectedChartId) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingCharts = true) }
            val region = ChartsCatalog.region(regionId)
            val result = runCatching { chartsRepository.topSongs(region.country) }.getOrDefault(emptyList())
            _state.update {
                if (it.selectedChartId != regionId) return@update it
                it.copy(charts = result, isLoadingCharts = false)
            }
            if (result.isNotEmpty()) enrichCharts(regionId, result)
        }
    }

    /**
     * Charts come from Apple with title/artist; fetch a YouTube match for each entry to get a
     * reliable cover and a ready-to-play video id (covers and playback always work via YouTube).
     */
    private fun enrichCharts(regionId: String, charts: List<Track>) {
        chartEnrichJob?.cancel()
        chartEnrichJob = viewModelScope.launch {
            charts.take(40).forEach { entry ->
                if (!isActive || _state.value.selectedChartId != regionId) return@launch
                if (entry.videoUrl.isNotBlank()) return@forEach
                val match = runCatching { repository.searchOne("${entry.title} ${entry.artist}") }.getOrNull()
                if (match != null) {
                    _state.update { st ->
                        if (st.selectedChartId != regionId) return@update st
                        st.copy(
                            charts = st.charts.map { c ->
                                if (c.id == entry.id) c.copy(
                                    id = match.id,
                                    thumbnailUrl = match.thumbnailUrl.ifBlank { c.thumbnailUrl },
                                    largeThumbnailUrl = match.largeThumbnailUrl.ifBlank { c.largeThumbnailUrl },
                                    videoUrl = match.videoUrl,
                                    durationMs = if (match.durationMs > 0L) match.durationMs else c.durationMs
                                ) else c
                            }
                        )
                    }
                    // Warm the stream of the very top chart entries for instant play.
                    if (charts.indexOf(entry) < 3) resolver.prefetch(match)
                }
                delay(90L)
            }
        }
    }

    fun toggleFavorite(track: Track) {
        val current = _state.value.favorites
        val exists = current.any { it.id == track.id }
        val updated = if (exists) current.filterNot { it.id == track.id } else listOf(track) + current
        favoritesStore.save(updated)
        _state.update { it.copy(favorites = updated, favoriteIds = updated.map { fav -> fav.id }.toSet()) }
    }

    fun exportCurrentTrack() {
        val track = _state.value.currentTrack ?: return
        exportTrack(track)
    }

    fun exportTrack(track: Track) {
        if (offlineExportJob?.isActive == true) return
        offlineExportJob = viewModelScope.launch {
            _state.update { it.copy(isOfflineExporting = true, offlineExportMessage = null) }
            val result = runCatching {
                val appContext = getApplication<Application>().applicationContext
                val workId = OfflineExportWorker.enqueue(appContext, TrackPayloadCodec.encode(track))
                val workManager = WorkManager.getInstance(appContext)
                var finished: WorkInfo? = null
                while (isActive && finished == null) {
                    val info = withContext(Dispatchers.IO) { workManager.getWorkInfoById(workId).get() }
                    if (info != null && info.state.isFinished) {
                        finished = info
                    } else {
                        delay(350L)
                    }
                }
                finished ?: throw CancellationException("Offline export observation cancelled")
            }
            result.onSuccess { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> handleOfflineExportSuccess(workInfo)
                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> handleOfflineExportFailure(workInfo.outputData.getString(OfflineExportWorker.KEY_ERROR))
                    else -> handleOfflineExportFailure("Esportazione non riuscita")
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                Timber.e(error, "Offline export work failed")
                handleOfflineExportFailure(error.message)
            }
        }
    }

    private fun handleOfflineExportSuccess(workInfo: WorkInfo) {
        val fileName = workInfo.outputData.getString(OfflineExportWorker.KEY_FILE_NAME).orEmpty()
        val embedded = workInfo.outputData.getBoolean(OfflineExportWorker.KEY_EMBEDDED_METADATA, false)
        val tagStatus = if (embedded) "con cover e metadata Levyra" else "con metadata Android"
        _state.update {
            it.copy(
                isOfflineExporting = false,
                offlineExportMessage = "Salvato in Music/Levyra: ${fileName.ifBlank { "brano esportato" }} ($tagStatus)",
                embeddedMetadataWriterReady = offlineExporter.embeddedMetadataWriterReady
            )
        }
    }

    private fun handleOfflineExportFailure(message: String?) {
        _state.update {
            it.copy(
                isOfflineExporting = false,
                offlineExportMessage = message ?: "Esportazione non riuscita",
                embeddedMetadataWriterReady = offlineExporter.embeddedMetadataWriterReady
            )
        }
    }

    fun clearOfflineExportMessage() {
        _state.update { it.copy(offlineExportMessage = null) }
    }

    fun selectTab(tab: LevyraTab) {
        moveToTab(tab, rememberCurrent = true)
    }

    fun navigateBack(): Boolean {
        val snapshot = _state.value
        return when {
            snapshot.showUpdatePrompt -> {
                dismissUpdatePrompt()
                true
            }
            snapshot.showQueue -> {
                closeQueue()
                true
            }
            snapshot.showLyrics -> {
                closeLyrics()
                true
            }
            snapshot.showSettings -> {
                closeSettings()
                true
            }
            snapshot.selectedTab != LevyraTab.Home -> {
                val previous = previousTab(snapshot.selectedTab)
                moveToTab(previous, rememberCurrent = false)
                true
            }
            else -> false
        }
    }

    private fun moveToTab(tab: LevyraTab, rememberCurrent: Boolean) {
        val current = _state.value.selectedTab
        if (current == tab) return
        if (rememberCurrent) {
            tabBackStack.remove(tab)
            tabBackStack.remove(current)
            tabBackStack.addLast(current)
            while (tabBackStack.size > 8) {
                tabBackStack.removeFirst()
            }
        }
        _state.update { it.copy(selectedTab = tab) }
    }

    private fun previousTab(current: LevyraTab): LevyraTab {
        while (tabBackStack.isNotEmpty()) {
            val candidate = tabBackStack.removeLast()
            if (candidate != current) return candidate
        }
        return LevyraTab.Home
    }

    fun selectMood(mood: Mood) {
        moveToTab(LevyraTab.Home, rememberCurrent = true)
        _state.update { it.copy(selectedMood = mood) }
        searchMood(mood)
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query, searchError = null) }
        searchJob?.cancel()
        val clean = query.trim()
        if (clean.length < 2) {
            _state.update { it.copy(searchSuggestions = emptyList(), searchResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(180L)
            val suggestions = withContext(Dispatchers.IO) {
                runCatching { repository.searchSuggestions(clean) }.getOrDefault(emptyList()).take(6)
            }
            if (_state.value.query.trim() != clean) return@launch
            _state.update { it.copy(searchSuggestions = suggestions) }
            delay(260L)
            if (_state.value.query.trim() == clean) runSearch(clean)
        }
    }

    fun searchNow() {
        searchNow(_state.value.query)
    }

    fun searchNow(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch { runSearch(clean) }
    }

    private suspend fun runSearch(clean: String) {
        moveToTab(LevyraTab.Search, rememberCurrent = true)
        _state.update { it.copy(isSearching = true, searchError = null, searchSuggestions = emptyList()) }
        val result = runCatching { repository.search(clean) }
        result.onSuccess { tracks ->
            val mood = _state.value.selectedMood
            val queue = moodEngine.buildQueue(mood, tracks.ifEmpty { repository.cachedTracks() })
            _state.update {
                it.copy(
                    tracks = mergeTracks(it.tracks, tracks),
                    queue = queue,
                    searchResults = tracks,
                    cacheReport = repository.cacheReport(),
                    smartScore = calculateSmartScore(queue),
                    isSearching = false,
                    searchError = if (tracks.isEmpty()) "Nessun risultato trovato per $clean" else null
                )
            }
            prefetchTop(tracks, 16)
        }.onFailure { error ->
            _state.update {
                it.copy(
                    isSearching = false,
                    searchError = error.message ?: "Ricerca non riuscita"
                )
            }
        }
    }

    private fun addToRecentSearches(track: Track) {
        // Don't track empty or invalid tracks
        if (track.id.isBlank() || track.title.isBlank()) return
        val current = _state.value.recentSearches
        val updated = listOf(track) + current.filterNot { it.id == track.id }
        val limited = updated.take(8)
        preferences.saveRecentSearches(limited)
        _state.update { it.copy(recentSearches = limited) }
    }

    fun play(track: Track) {
        addToRecentSearches(track)
        queueIndex = currentQueue().indexOfFirst { it.id == track.id }
        startResolve(track)
    }

    /** Play a track as part of a specific list so next/previous follow that list. */
    fun playFrom(list: List<Track>, track: Track) {
        addToRecentSearches(track)
        _state.update { it.copy(queue = list) }
        queueIndex = list.indexOfFirst { it.id == track.id }
        startResolve(track)
    }

    private fun startResolve(track: Track) {
        val requestId = ++playRequestId
        playJob?.cancel()

        // Instant path: the stream is already resolved or warm in cache, start immediately.
        val playableTrack = youtubePlayableTrack(track) ?: track
        val instant = resolver.cached(playableTrack)
        if (instant != null) {
            startPlayback(instant)
            prefetchAround(instant)
            return
        }

        _state.update {
            it.copy(
                isResolving = true,
                playerError = null,
                currentTrack = track.copy(streamUrl = ""),
                isPlaying = false,
                positionMs = 0L,
                durationMs = track.durationMs
            )
        }
        player.stop()
        playJob = viewModelScope.launch {
            try {
                val playable = resolveForPlayback(track)
                if (!isActive || requestId != playRequestId) return@launch
                startPlayback(playable)
                prefetchAround(playable)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                if (!isActive || requestId != playRequestId) return@launch
                player.stop()
                _state.update {
                    it.copy(
                        isResolving = false,
                        isPlaying = false,
                        positionMs = 0L,
                        durationMs = track.durationMs,
                        currentTrack = track.copy(streamUrl = ""),
                        playerError = error.message ?: "Stream non disponibile per questo brano"
                    )
                }
            }
        }
    }

    private fun startPlayback(playable: Track) {
        repository.replace(playable)
        player.play(playable)
        // Resume from the saved position when continuing the last session's track.
        val resumeMs = pendingSeekMs.takeIf { it > 1500L && it < playable.durationMs } ?: 0L
        if (resumeMs > 0L) player.seekTo(resumeMs)
        pendingSeekMs = 0L
        _state.update {
            it.copy(
                currentTrack = playable,
                tracks = mergeTracks(it.tracks, listOf(playable)),
                searchResults = mergeTracks(it.searchResults, listOf(playable)),
                lyrics = emptyList(),
                activeLyric = null,
                isPlaying = true,
                isResolving = false,
                durationMs = effectiveDuration(playable),
                positionMs = resumeMs,
                cacheReport = repository.cacheReport(),
                playerError = null
            )
        }
        fetchLyrics(playable)
        fetchSponsorSegments(playable)
    }

    private fun fetchSponsorSegments(track: Track) {
        sponsorJob?.cancel()
        sponsorSegments = emptyList()
        if (!_state.value.sponsorBlockEnabled || track.id.isBlank() || track.id.startsWith("chart-")) return
        sponsorJob = viewModelScope.launch {
            val result = runCatching { sponsorBlockRepository.segments(track.id) }.getOrDefault(emptyList())
            if (_state.value.currentTrack?.id == track.id) sponsorSegments = result
        }
    }

    fun setSponsorBlock(value: Boolean) {
        preferences.setSponsorBlock(value)
        _state.update { it.copy(sponsorBlockEnabled = value) }
        if (!value) {
            sponsorJob?.cancel()
            sponsorSegments = emptyList()
        } else {
            _state.value.currentTrack?.let { fetchSponsorSegments(it) }
        }
    }

    fun setSkipSilence(value: Boolean) {
        preferences.setSkipSilence(value)
        player.setSkipSilence(value)
        _state.update { it.copy(skipSilence = value) }
    }

    fun openQueue() {
        _state.update { it.copy(showQueue = true) }
    }

    fun closeQueue() {
        _state.update { it.copy(showQueue = false) }
    }

    fun openLyrics() {
        _state.update { it.copy(showLyrics = true) }
    }

    fun closeLyrics() {
        _state.update { it.copy(showLyrics = false) }
    }

    private fun fetchLyrics(track: Track) {
        lyricsJob?.cancel()
        _state.update { it.copy(lyrics = emptyList(), lyricsSynced = false, lyricsLoading = true) }
        lyricsJob = viewModelScope.launch {
            val result = runCatching {
                lyricsRepository.fetch(track.title, track.artist, track.durationMs / 1000L)
            }.getOrNull()
            if (_state.value.currentTrack?.id != track.id) return@launch
            _state.update {
                it.copy(
                    lyrics = result?.lines.orEmpty(),
                    lyricsSynced = result?.synced ?: false,
                    lyricsLoading = false
                )
            }
        }
    }

    private fun prefetchAround(playable: Track) {
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            val queue = _state.value.queue.ifEmpty { currentQueue() }
            if (queue.isEmpty()) return@launch
            val base = if (queueIndex in queue.indices) queueIndex else queue.indexOfFirst { it.id == playable.id }
            if (base < 0) return@launch
            val candidates = listOf(1, 2, -1)
                .map { offset -> queue[(base + offset + queue.size) % queue.size] }
                .filterNot { it.id == playable.id }
                .distinctBy { it.id }
            warmTracks(candidates, concurrency = 2, delayStepMs = 70L)
        }
    }

    private fun prefetchTop(tracks: List<Track>, count: Int = 16) {
        if (tracks.isEmpty()) return
        listPrefetchJob?.cancel()
        listPrefetchJob = viewModelScope.launch {
            warmTracks(tracks.take(count), concurrency = 3, delayStepMs = 55L)
        }
    }

    private suspend fun warmTracks(tracks: List<Track>, concurrency: Int, delayStepMs: Long) = coroutineScope {
        val semaphore = Semaphore(concurrency.coerceAtLeast(1))
        tracks.distinctBy { youtubePlayableTrack(it)?.id ?: it.id }.forEachIndexed { index, track ->
            launch {
                if (index > 0) delay(index * delayStepMs)
                semaphore.withPermit { warmTrack(track) }
            }
        }
    }

    private suspend fun warmTrack(track: Track) {
        val youtube = youtubePlayableTrack(track)
        if (youtube != null) {
            resolver.prefetch(youtube)
            return
        }
        val match = runCatching { repository.searchOne("${track.title} ${track.artist}") }.getOrNull() ?: return
        resolver.prefetch(match)
    }

    private fun currentQueue(): List<Track> {
        val snapshot = _state.value
        return snapshot.queue.ifEmpty { snapshot.searchResults }.ifEmpty { snapshot.tracks }
    }

    fun togglePlay() {
        val current = _state.value.currentTrack ?: return
        if (current.streamUrl.isBlank()) {
            play(current)
            return
        }
        if (_state.value.isPlaying) {
            player.pause()
            preferences.saveLastPlayback(current, player.positionMs)
            _state.update { it.copy(isPlaying = false) }
        } else {
            player.play(current)
            _state.update { it.copy(isPlaying = true) }
        }
    }

    fun closePlayer() {
        player.stop()
        _state.update {
            it.copy(
                currentTrack = null,
                isPlaying = false,
                positionMs = 0L,
                durationMs = 0L
            )
        }
        preferences.saveLastPlayback(null, 0L)
    }

    fun next() {
        val queue = _state.value.queue.ifEmpty { currentQueue() }
        if (queue.isEmpty()) return
        if (_state.value.shuffleEnabled && queue.size > 1) {
            queueIndex = (queue.indices - queueIndex).ifEmpty { queue.indices.toList() }.random()
            startResolve(queue[queueIndex])
            return
        }
        val base = if (queueIndex in queue.indices) queueIndex else queue.indexOfFirst { it.id == _state.value.currentTrack?.id }
        val nextIndex = if (base < 0) 0 else (base + 1) % queue.size
        queueIndex = nextIndex
        startResolve(queue[nextIndex])
    }

    fun previous() {
        val queue = _state.value.queue.ifEmpty { currentQueue() }
        if (queue.isEmpty()) return
        val base = if (queueIndex in queue.indices) queueIndex else queue.indexOfFirst { it.id == _state.value.currentTrack?.id }
        val previousIndex = if (base <= 0) queue.lastIndex else base - 1
        queueIndex = previousIndex
        startResolve(queue[previousIndex])
    }

    fun seekTo(progress: Float) {
        val duration = _state.value.durationMs.coerceAtLeast(1L)
        player.seekTo((duration * progress.coerceIn(0f, 1f)).toLong())
    }

    private fun loadHome() {
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, searchError = null) }
            val queries = moodEngine.queriesForTastes(preferences.tastes())
            val result = runCatching { repository.home(queries) }
            result.onSuccess { tracks ->
                val selectedMood = _state.value.selectedMood
                val queue = moodEngine.buildQueue(selectedMood, tracks)
                _state.update {
                    it.copy(
                        tracks = tracks,
                        queue = queue,
                        searchResults = tracks.take(12),
                        isSearching = false,
                        smartScore = calculateSmartScore(queue),
                        cacheReport = repository.cacheReport(),
                        searchError = if (tracks.isEmpty()) "Home remota vuota: prova una ricerca" else null
                    )
                }
                prefetchTop(tracks, 16)
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isSearching = false,
                        searchError = error.message ?: "Home non caricata"
                    )
                }
            }
        }
    }

    private fun searchMood(mood: Mood) {
        val query = moodEngine.tagQueryFor(mood)
        _state.update { it.copy(query = query) }
        searchNow(query)
    }

    private fun startTicker() {
        viewModelScope.launch {
            var ticks = 0
            while (isActive) {
                val snapshot = _state.value
                val current = snapshot.currentTrack
                val duration = current?.let { effectiveDuration(it) } ?: player.durationMs
                // SponsorBlock: skip past non-music / sponsor segments automatically.
                if (snapshot.sponsorBlockEnabled && sponsorSegments.isNotEmpty() && player.isPlaying) {
                    val pos = player.positionMs
                    val segment = sponsorSegments.firstOrNull { pos >= it.startMs && pos < it.endMs - 250 }
                    if (segment != null) player.seekTo(segment.endMs)
                }
                // Until the restored track is actually played, keep showing the saved resume position.
                val position = if (!player.isPlaying && player.positionMs == 0L && pendingSeekMs > 0L) {
                    pendingSeekMs
                } else {
                    player.positionMs
                }
                val active = lyricsEngine.currentLine(position, snapshot.lyrics)
                _state.update {
                    it.copy(
                        positionMs = position,
                        durationMs = duration,
                        isPlaying = player.isPlaying,
                        activeLyric = active
                    )
                }
                // Persist the last playback roughly every 2s so reopening resumes the same spot.
                if (current != null && player.isPlaying && ticks % 4 == 0) {
                    preferences.saveLastPlayback(current, position)
                }
                ticks++
                delay(500L)
            }
        }
    }


    /** Chart entries have no YouTube id yet: match them on YouTube first, then pass to player. */
    private suspend fun resolveForPlayback(track: Track): Track {
        youtubePlayableTrack(track)?.let { return resolvePlayableTrack(it) }
        val match = repository.searchOne("${track.title} ${track.artist}")
            ?: throw IllegalStateException("Nessun risultato YouTube per ${track.title}")
        val carried = match.copy(
            thumbnailUrl = match.thumbnailUrl.ifBlank { track.thumbnailUrl },
            largeThumbnailUrl = match.largeThumbnailUrl.ifBlank { track.largeThumbnailUrl }
        )
        return resolvePlayableTrack(carried)
    }

    private suspend fun resolvePlayableTrack(track: Track): Track {
        val resolved = resolver.resolve(track.copy(streamUrl = ""))
        if (resolved.id != track.id) {
            throw IllegalStateException("Resolver bloccato: il brano risolto non corrisponde al brano selezionato")
        }
        if (resolved.streamUrl.isBlank()) {
            throw IllegalStateException("YouTube non ha fornito uno stream audio riproducibile per ${track.title}")
        }
        return resolved
    }

    private fun effectiveDuration(track: Track): Long {
        return player.durationMs.takeIf { it > 0L } ?: track.durationMs
    }

    private fun calculateSmartScore(queue: List<Track>): Int {
        if (queue.isEmpty()) return 0
        val replay = queue.sumOf { it.replayScore } / queue.size
        val cache = queue.sumOf { it.cacheScore } / queue.size
        return ((replay * 0.68f) + (cache * 0.32f)).toInt().coerceIn(0, 100)
    }

    private fun mergeTracks(old: List<Track>, incoming: List<Track>): List<Track> {
        val map = LinkedHashMap<String, Track>()
        old.forEach { map[it.id] = it }
        incoming.forEach { map[it.id] = it }
        return map.values.toList()
    }

    override fun onCleared() {
        _state.value.currentTrack?.let { preferences.saveLastPlayback(it, player.positionMs) }
        playJob?.cancel()
        prefetchJob?.cancel()
        chartEnrichJob?.cancel()
        sleepJob?.cancel()
        lyricsJob?.cancel()
        sponsorJob?.cancel()
        listPrefetchJob?.cancel()
        offlineExportJob?.cancel()
        player.release()
        searchJob?.cancel()
        super.onCleared()
    }
}

internal fun youtubePlayableTrack(track: Track): Track? {
    val videoId = youtubeVideoId(track.videoUrl)
        .ifBlank { youtubeVideoId(track.id) }
        .ifBlank { track.id.takeUnless { it.startsWith("chart-") || it.contains("://") }.orEmpty() }
    if (videoId.isBlank()) return null
    val videoUrl = track.videoUrl.ifBlank { "https://www.youtube.com/watch?v=$videoId" }
    return track.copy(id = videoId, videoUrl = videoUrl)
}

private fun youtubeVideoId(url: String): String {
    if (url.isBlank()) return ""
    val patterns = listOf(
        Regex("[?&]v=([^&?/]+)"),
        Regex("youtu\\.be/([^?&/]+)"),
        Regex("/shorts/([^?&/]+)"),
        Regex("/embed/([^?&/]+)")
    )
    return patterns.firstNotNullOfOrNull { pattern ->
        pattern.find(url)?.groupValues?.getOrNull(1)
    }.orEmpty()
}

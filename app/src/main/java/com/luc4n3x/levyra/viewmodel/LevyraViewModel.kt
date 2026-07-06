package com.luc4n3x.levyra.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.luc4n3x.levyra.data.AppUpdateRepository
import com.luc4n3x.levyra.data.ArtistRepository
import com.luc4n3x.levyra.data.ChartsRepository
import com.luc4n3x.levyra.data.FavoritesStore
import com.luc4n3x.levyra.data.FollowedArtistsStore
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.LevyraHomeSnapshotCache
import com.luc4n3x.levyra.data.LevyraStartupCatalog
import com.luc4n3x.levyra.data.PersonalOrbitArtworkWorker
import com.luc4n3x.levyra.data.LyricsRepository
import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.data.SponsorBlockRepository
import com.luc4n3x.levyra.data.TrackPayloadCodec
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.local.DownloadEntity
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.FollowedArtist
import com.luc4n3x.levyra.domain.ReleaseRadarEntry
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.SponsorSegment
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.LevyraAudioPresets
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.LevyraLocalizedDiscovery
import com.luc4n3x.levyra.domain.LyricsEngine
import com.luc4n3x.levyra.domain.Mood
import com.luc4n3x.levyra.domain.MoodEngine
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.theme.LevyraThemes
import com.luc4n3x.levyra.widget.LevyraWidgetBridge
import com.luc4n3x.levyra.widget.LevyraWidgetCenter
import com.luc4n3x.levyra.player.LevyraPlayer
import com.luc4n3x.levyra.player.PlaybackWarmup
import com.luc4n3x.levyra.player.offline.OfflineAudioExporter
import com.luc4n3x.levyra.player.offline.work.OfflineExportWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class LevyraViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YoutubeMusicRepository(application.applicationContext)
    private val artistRepository = ArtistRepository(repository, application.applicationContext)
    private val chartsRepository = ChartsRepository()
    private val downloadedTracksDao = LevyraDatabase.get(application.applicationContext).downloadedTracksDao()
    private val appUpdateRepository = AppUpdateRepository(application.applicationContext)
    private val lyricsRepository = LyricsRepository()
    private val sponsorBlockRepository = SponsorBlockRepository()
    private val resolver = PlaybackResolver.getInstance(application.applicationContext)
    private val moodEngine = MoodEngine()
    private val lyricsEngine = LyricsEngine()
    private val player = LevyraPlayer(application.applicationContext)
    private val playbackWarmup = PlaybackWarmup(application.applicationContext)
    private val offlineExporter = OfflineAudioExporter(application.applicationContext, resolver)
    private val favoritesStore = FavoritesStore(application.applicationContext)
    private val followedArtistsStore = FollowedArtistsStore(application.applicationContext)
    private val playlistStore = com.luc4n3x.levyra.data.PlaylistStore(application.applicationContext)
    private val preferences = LevyraPreferences(application.applicationContext)
    private val homeSnapshotCache = LevyraHomeSnapshotCache(application.applicationContext)
    private val startupSettings = preferences.snapshot()
    private val startupMoods = moodEngine.moodsForLanguage(startupSettings.languageCode)
    private val _state = MutableStateFlow(
        LevyraUiState(
            moods = startupMoods,
            tastes = moodEngine.tastesForLanguage(startupSettings.languageCode),
            chartRegions = ChartsCatalog.regions,
            selectedChartId = ChartsCatalog.defaultRegionForLanguage(startupSettings.languageCode).id,
            selectedMood = startupMoods.firstOrNull(),
            isSearching = false,
            embeddedMetadataWriterReady = offlineExporter.embeddedMetadataWriterReady,
            audioNormalization = startupSettings.audioNormalization,
            audioSettings = startupSettings.audioSettings
        )
    )
    private var searchJob: Job? = null
    private var playJob: Job? = null
    private var prefetchJob: Job? = null
    private var chartEnrichJob: Job? = null
    private var sleepJob: Job? = null
    private var crossfadeJob: Job? = null
    private var crossfadeInProgress = false
    private var lyricsJob: Job? = null
    private var sponsorJob: Job? = null
    private var listPrefetchJob: Job? = null
    private var updateJob: Job? = null
    private var artistJob: Job? = null
    private var radarJob: Job? = null
    private var sponsorSegments: List<SponsorSegment> = emptyList()
    private val tabBackStack = ArrayDeque<LevyraTab>()
    private var playRequestId: Long = 0L
    private var pendingSeekMs: Long = 0L
    private var queueIndex: Int = -1
    private var loopCurrentQueueOnCompletion: Boolean = false
    private val activeDownloadKeys = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val activeDownloadTitles = ConcurrentHashMap<String, String>()

    val state: StateFlow<LevyraUiState> = _state.asStateFlow()
    val playerController get() = player.controller

    init {
        val favorites = favoritesStore.load()
        val settings = startupSettings
        val defaultChartRegion = ChartsCatalog.defaultRegionForLanguage(settings.languageCode)
        val instantSnapshot = homeSnapshotCache.load(settings.languageCode)
        val cachedHomeSections = instantSnapshot?.homeSections?.takeIf { it.isNotEmpty() } ?: preferences.loadHomeSections(settings.languageCode)
        val startupHomeSections = cachedHomeSections.ifEmpty { LevyraStartupCatalog.homeSections(settings.languageCode) }
        val startupHomeTracks = startupHomeSections.flatMap { it.tracks }.distinctBy { it.id }
        val cachedCharts = instantSnapshot?.charts?.takeIf { it.isNotEmpty() } ?: preferences.loadChartTracks(settings.languageCode, defaultChartRegion.id)
        val startupCharts = cachedCharts.ifEmpty { LevyraStartupCatalog.chartTracks(settings.languageCode) }
        val rawCachedOrbitTracks = settings.personalOrbitTracks
            .ifEmpty { instantSnapshot?.personalOrbit.orEmpty() }
            .ifEmpty { preferences.loadPersonalOrbitTracks(settings.languageCode) }
        val startupOrbitSeed = mergeTracks(rawCachedOrbitTracks + settings.recentSearches + favorites, startupHomeTracks + startupCharts)
        val cachedOrbitTracks = LevyraPersonalOrbit.build(
            currentTrack = null,
            recentSearches = settings.recentSearches,
            favorites = favorites,
            tracks = startupOrbitSeed,
            homeSections = startupHomeSections,
            charts = startupCharts,
            cachedOrbit = rawCachedOrbitTracks,
            limit = LevyraPersonalOrbit.DISPLAY_LIMIT,
            languageCode = settings.languageCode
        )
        val initialTracks = mergeTracks(cachedOrbitTracks + settings.recentSearches + favorites, startupHomeTracks + startupCharts)
        val initialQueue = moodEngine.buildQueue(startupMoods.firstOrNull(), initialTracks)
        val restoredTrack = settings.lastTrack?.copy(streamUrl = "", videoStreamUrl = "")
        pendingSeekMs = settings.lastPositionMs.coerceAtLeast(0L)
        resolver.setAudioQuality(settings.audioQuality)
        _state.update {
            it.copy(
                favorites = favorites,
                favoriteIds = favorites.map { fav -> fav.id }.toSet(),
                recentSearches = settings.recentSearches,
                personalOrbitTracks = cachedOrbitTracks,
                homeSections = startupHomeSections,
                tracks = initialTracks,
                queue = initialQueue,
                searchResults = initialTracks.take(12),
                charts = startupCharts,
                selectedChartId = defaultChartRegion.id,
                isSearching = false,
                isLoadingCharts = false,
                cacheReport = repository.cacheReport(),
                userName = settings.userName,
                languageCode = settings.languageCode,
                animationsEnabled = settings.animationsEnabled,
                dynamicColor = settings.dynamicColor,
                sponsorBlockEnabled = settings.sponsorBlock,
                skipSilence = settings.skipSilence,
                audioQuality = settings.audioQuality,
                audioNormalization = settings.audioNormalization,
                audioSettings = settings.audioSettings,
                playbackSpeed = settings.audioSettings.playbackSpeed,
                themePreset = settings.themePreset,
                showOnboarding = !settings.onboarded,
                currentTrack = restoredTrack,
                positionMs = pendingSeekMs,
                durationMs = restoredTrack?.durationMs ?: 0L,
                lyrics = emptyList()
            )
        }
        player.setSkipSilence(settings.skipSilence)
        player.setPremiumAudioSettings(settings.audioSettings)
        player.setPlayback(settings.audioSettings.playbackSpeed, settings.audioSettings.pitch)
        player.onCompletion = { onTrackCompleted() }
        player.onError = { errorMsg ->
            _state.update { it.copy(playerError = cleanUserError(errorMsg), isPlaying = false, isResolving = false) }
        }
        applyFollowedArtists(followedArtistsStore.load())
        startTicker()
        observeDownloads()
        loadPlaylists()
        scheduleColdStartRefresh(initialTracks)
        LevyraWidgetBridge.onToggle = { togglePlay() }
        LevyraWidgetBridge.onNext = { next() }
        LevyraWidgetBridge.onPrevious = { previous() }
        updateWidget()
    }

    private fun applyFollowedArtists(artists: List<FollowedArtist>) {
        val keys = buildSet {
            artists.forEach { artist ->
                if (artist.browseId.isNotBlank()) add(artist.browseId)
                add(artist.name.trim().lowercase())
            }
        }
        _state.update { it.copy(followedArtists = artists, followedArtistKeys = keys) }
    }

    fun toggleFollowArtist() {
        val profile = _state.value.artistProfile ?: return
        val name = profile.name.trim()
        if (name.isBlank()) return
        val current = _state.value.followedArtists
        val exists = current.any { sameArtist(it, profile.browseId, name) }
        val updated = if (exists) {
            current.filterNot { sameArtist(it, profile.browseId, name) }
        } else {
            listOf(FollowedArtist(profile.browseId, name, profile.thumbnailUrl, System.currentTimeMillis())) + current
        }
        followedArtistsStore.save(updated)
        applyFollowedArtists(updated)
        loadReleaseRadar()
    }

    private fun sameArtist(artist: FollowedArtist, browseId: String, name: String): Boolean =
        (browseId.isNotBlank() && artist.browseId == browseId) || artist.name.equals(name, ignoreCase = true)

    private fun scheduleColdStartRefresh(initialTracks: List<Track>) {
        val appContext = getApplication<Application>().applicationContext
        val orbitSeed = _state.value.personalOrbitTracks.ifEmpty { initialTracks }.take(LevyraPersonalOrbit.DISPLAY_LIMIT)
        if (orbitSeed.isNotEmpty()) {
            LevyraArtworkCache.preloadPriority(appContext, orbitSeed, LevyraPersonalOrbit.DISPLAY_LIMIT)
            warmPersistentOrbit(orbitSeed, LevyraPersonalOrbit.DISPLAY_LIMIT, persist = true)
        }
        viewModelScope.launch {
            delay(450L)
            resolver.warmNetwork()
            loadHomeFeed()
        }
        viewModelScope.launch {
            delay(950L)
            loadCharts()
        }
        viewModelScope.launch {
            delay(1700L)
            checkForUpdates(silent = true)
            loadReleaseRadar()
        }
    }

    private fun persistPersonalOrbit(extraTracks: List<Track> = emptyList()) {
        val snapshot = _state.value
        val mergedTracks = mergeTracks(snapshot.tracks, extraTracks)
        val orbit = LevyraPersonalOrbit.build(
            currentTrack = snapshot.currentTrack,
            recentSearches = snapshot.recentSearches,
            favorites = snapshot.favorites,
            tracks = mergedTracks,
            homeSections = snapshot.homeSections,
            charts = snapshot.charts,
            cachedOrbit = snapshot.personalOrbitTracks,
            limit = LevyraPersonalOrbit.DISPLAY_LIMIT,
            languageCode = snapshot.languageCode
        )
        if (orbit.isEmpty()) return
        val limited = orbit.take(LevyraPersonalOrbit.DISPLAY_LIMIT)
        _state.update { it.copy(personalOrbitTracks = limited) }
        warmPersistentOrbit(limited, LevyraPersonalOrbit.DISPLAY_LIMIT, persist = true)
    }

    private fun warmPersistentOrbit(tracks: List<Track>, limit: Int, persist: Boolean = false) {
        val limited = tracks.take(limit.coerceAtLeast(1))
        if (limited.isEmpty()) return
        val appContext = getApplication<Application>().applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            val languageCode = _state.value.languageCode
            if (persist) preferences.savePersonalOrbitTracks(limited, languageCode)
            LevyraArtworkCache.cachePersistent(appContext, limited, limit)
            if (persist) PersonalOrbitArtworkWorker.enqueue(appContext, languageCode)
            if (persist) persistHomeSnapshotSync(languageCode)
        }
    }

    private fun persistHomeSnapshot() {
        val languageCode = _state.value.languageCode
        viewModelScope.launch(Dispatchers.IO) { persistHomeSnapshotSync(languageCode) }
    }

    private fun persistHomeSnapshotSync(languageCode: String) {
        val snapshot = _state.value
        homeSnapshotCache.save(
            languageCode = languageCode,
            homeSections = snapshot.homeSections,
            charts = snapshot.charts,
            personalOrbit = snapshot.personalOrbitTracks
        )
    }

    private fun loadReleaseRadar() {
        radarJob?.cancel()
        val followed = _state.value.followedArtists
        if (followed.isEmpty()) {
            _state.update { it.copy(releaseRadar = emptyList(), similarArtists = emptyList()) }
            return
        }
        radarJob = viewModelScope.launch {
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val entries = mutableListOf<ReleaseRadarEntry>()
            val similar = LinkedHashMap<String, com.luc4n3x.levyra.domain.ArtistHit>()
            followed.take(8).forEach { artist ->
                val profile = runCatching { artistRepository.profile(artist.browseId, artist.name) }.getOrNull() ?: return@forEach
                if (!isActive) return@launch
                (profile.albums + profile.singles).take(8).forEach { release ->
                    val year = release.year.toIntOrNull()
                    entries += ReleaseRadarEntry(
                        artistName = profile.name,
                        artistBrowseId = profile.browseId,
                        release = release,
                        isFresh = year != null && year >= currentYear - 1
                    )
                }
                profile.relatedArtists.forEach { hit ->
                    val key = hit.name.trim().lowercase()
                    if (key !in _state.value.followedArtistKeys && !similar.containsKey(key)) {
                        similar[key] = hit
                    }
                }
                val sorted = entries
                    .distinctBy { it.release.browseId.ifBlank { "${it.artistName}|${it.release.title}" } }
                    .sortedByDescending { it.release.year.toIntOrNull() ?: 0 }
                    .take(20)
                _state.update { it.copy(releaseRadar = sorted, similarArtists = similar.values.take(12).toList()) }
            }
        }
    }

    fun playDailyFlow() {
        val snapshot = _state.value
        val pool = (snapshot.favorites + snapshot.recentSearches + snapshot.tracks)
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id }
        if (pool.isEmpty()) return
        val seed = System.currentTimeMillis() / 86_400_000L
        val flow = pool.shuffled(kotlin.random.Random(seed)).take(30)
        playFrom(flow, flow.first(), loopOnCompletion = true)
    }

    fun setThemePreset(value: String) {
        val normalized = LevyraThemes.normalize(value)
        preferences.setThemePreset(normalized)
        _state.update { it.copy(themePreset = normalized) }
    }

    private fun updateWidget() {
        val snapshot = _state.value
        val track = snapshot.currentTrack
        LevyraWidgetCenter.update(
            getApplication<Application>().applicationContext,
            track?.title,
            track?.artist,
            track?.largeThumbnailUrl?.ifBlank { track.thumbnailUrl },
            snapshot.isPlaying
        )
    }


    private fun loadPlaylists() {
        viewModelScope.launch {
            val lists = playlistStore.loadAll()
            _state.update { it.copy(playlists = lists) }
        }
    }

    fun createPlaylist(name: String, firstTrack: Track? = null) {
        viewModelScope.launch {
            playlistStore.create(name, firstTrack)
            loadPlaylists()
        }
    }

    fun renamePlaylist(playlistId: String, name: String) {
        viewModelScope.launch {
            playlistStore.rename(playlistId, name)
            loadPlaylists()
            refreshOpenPlaylist(playlistId)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            playlistStore.delete(playlistId)
            _state.update { if (it.openPlaylist?.id == playlistId) it.copy(openPlaylist = null) else it }
            loadPlaylists()
        }
    }

    fun addToPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch {
            playlistStore.addTrack(playlistId, track.copy(streamUrl = ""))
            loadPlaylists()
            refreshOpenPlaylist(playlistId)
        }
    }

    fun removeFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            playlistStore.removeTrack(playlistId, trackId)
            loadPlaylists()
            refreshOpenPlaylist(playlistId)
        }
    }

    fun openPlaylist(playlistId: String) {
        viewModelScope.launch {
            val pl = playlistStore.load(playlistId)
            _state.update { it.copy(openPlaylist = pl) }
        }
    }

    fun closePlaylist() {
        _state.update { it.copy(openPlaylist = null) }
    }

    fun playPlaylist(playlistId: String, startTrackId: String? = null) {
        viewModelScope.launch {
            val pl = playlistStore.load(playlistId) ?: return@launch
            if (pl.tracks.isEmpty()) return@launch
            val start = startTrackId?.let { id -> pl.tracks.firstOrNull { it.id == id } } ?: pl.tracks.first()
            playFrom(pl.tracks, start, loopOnCompletion = true)
        }
    }

    private suspend fun refreshOpenPlaylist(playlistId: String) {
        if (_state.value.openPlaylist?.id != playlistId) return
        val pl = playlistStore.load(playlistId)
        _state.update { it.copy(openPlaylist = pl) }
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            downloadedTracksDao.observeRecent().collectLatest { entities ->
                val mapped = entities.map { it.toDownloadedTrack() }
                _state.update {
                    it.copy(
                        downloads = mapped,
                        downloadedTrackIds = mapped.map { item -> item.trackId }.toSet()
                    )
                }
            }
        }
    }

    private fun DownloadEntity.toDownloadedTrack(): DownloadedTrack = DownloadedTrack(
        id = id,
        trackId = trackId,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        fileName = fileName,
        uri = uri,
        mimeType = mimeType,
        embeddedMetadata = embeddedMetadata,
        savedAt = savedAt
    )

    private fun onTrackCompleted() {
        val snapshot = _state.value
        val current = snapshot.currentTrack ?: return
        if (crossfadeInProgress) return
        if (snapshot.isResolving || playJob?.isActive == true || current.streamUrl.isBlank()) return
        val duration = effectiveDuration(current)
        if (duration > 0L && player.positionMs < (duration - 1_500L).coerceAtLeast(0L)) return
        val queue = snapshot.queue.ifEmpty { currentQueue() }
        when {
            _state.value.shuffleEnabled && queue.size > 1 -> {
                queueIndex = queue.indices.filter { it != queueIndex }.ifEmpty { queue.indices.toList() }.random()
                startResolve(queue[queueIndex])
            }
            _state.value.repeatMode == RepeatMode.All -> next()
            queueIndex in 0 until queue.lastIndex -> next()
            loopCurrentQueueOnCompletion && queue.size > 1 -> next()
            loopCurrentQueueOnCompletion && queue.size == 1 -> next()
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

    fun toggleVideoMode() {
        val current = _state.value.isVideoMode
        val track = _state.value.currentTrack ?: return
        val newVideoMode = !current
        _state.update { it.copy(isVideoMode = newVideoMode, isResolving = true) }
        val oldPosition = player.positionMs
        player.stop()
        viewModelScope.launch {
            try {
                val resolved = withContext(Dispatchers.IO) {
                    resolver.resolve(track.copy(streamUrl = ""), newVideoMode)
                }
                _state.update { it.copy(currentTrack = resolved, isResolving = false, isPlaying = true) }
                player.play(resolved)
                if (oldPosition > 0L) {
                    delay(300)
                    player.seekTo(oldPosition)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.update { it.copy(isResolving = false, isVideoMode = current, playerError = cleanUserError(e)) }
            }
        }
    }

    fun toggleAudioNormalization() {
        setReplayGainEnabled(!_state.value.audioNormalization)
    }

    fun toggleShuffle() {
        _state.update { it.copy(shuffleEnabled = !it.shuffleEnabled) }
    }

    fun cycleSpeed() {
        val steps = listOf(1f, 1.25f, 1.5f, 2f, 0.75f)
        val current = _state.value.audioSettings.playbackSpeed
        val next = steps[(steps.indexOf(current).coerceAtLeast(0) + 1) % steps.size]
        updateAudioSettings(_state.value.audioSettings.copy(playbackSpeed = next))
    }

    fun setEqualizerEnabled(value: Boolean) {
        updateAudioSettings(_state.value.audioSettings.copy(equalizerEnabled = value))
    }

    fun setEqualizerPreset(presetId: String) {
        val preset = LevyraAudioPresets.preset(presetId)
        updateAudioSettings(
            _state.value.audioSettings.copy(
                equalizerEnabled = true,
                presetId = preset.id,
                bandLevels = preset.levels,
                bassBoost = preset.bassBoost,
                virtualizer = preset.virtualizer
            )
        )
    }

    fun setBassBoost(value: Int) {
        updateAudioSettings(_state.value.audioSettings.copy(equalizerEnabled = true, bassBoost = value))
    }

    fun setVirtualizer(value: Int) {
        updateAudioSettings(_state.value.audioSettings.copy(equalizerEnabled = true, virtualizer = value))
    }

    fun setCrossfadeSeconds(seconds: Int) {
        updateAudioSettings(_state.value.audioSettings.copy(crossfadeSeconds = seconds))
    }

    fun setDjSoftMode(value: Boolean) {
        updateAudioSettings(_state.value.audioSettings.copy(djSoftMode = value, crossfadeSeconds = if (value && _state.value.audioSettings.crossfadeSeconds == 0) 6 else _state.value.audioSettings.crossfadeSeconds))
    }

    fun setReplayGainEnabled(value: Boolean) {
        preferences.setAudioNormalization(value)
        updateAudioSettings(_state.value.audioSettings.copy(replayGainEnabled = value), audioNormalization = value)
    }

    fun setPlaybackSpeed(value: Float) {
        updateAudioSettings(_state.value.audioSettings.copy(playbackSpeed = value))
    }

    fun setPitch(value: Float) {
        updateAudioSettings(_state.value.audioSettings.copy(pitch = value))
    }

    fun setGaplessEnabled(value: Boolean) {
        updateAudioSettings(_state.value.audioSettings.copy(gaplessEnabled = value))
    }

    private fun updateAudioSettings(next: LevyraAudioSettings, audioNormalization: Boolean = _state.value.audioNormalization) {
        val normalized = next.normalized()
        preferences.setAudioSettings(normalized)
        player.setPremiumAudioSettings(normalized)
        player.setPlayback(normalized.playbackSpeed, normalized.pitch)
        com.luc4n3x.levyra.player.PlaybackService.normalizationProcessor.enabled = audioNormalization || normalized.replayGainEnabled
        _state.update { it.copy(audioSettings = normalized, playbackSpeed = normalized.playbackSpeed, audioNormalization = audioNormalization) }
    }


    fun openAudioQualityPanel() {
        _state.update { it.copy(showAudioQualityPanel = true) }
    }

    fun closeAudioQualityPanel() {
        _state.update { it.copy(showAudioQualityPanel = false) }
    }

    fun setAudioQuality(value: String) {
        val normalized = when (value.lowercase()) {
            "high" -> "High"
            "low" -> "Low"
            else -> "Auto"
        }
        preferences.setAudioQuality(normalized)
        resolver.setAudioQuality(normalized)
        _state.update { it.copy(audioQuality = normalized) }
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

    fun completeOnboarding(name: String, tasteIds: Set<String>, languageCode: String) {
        val normalizedLanguage = LevyraLanguageCatalog.normalize(languageCode)
        preferences.setLanguageCode(normalizedLanguage)
        preferences.setUserName(name.trim())
        preferences.setOnboarded(tasteIds)
        _state.update { it.copy(showOnboarding = false, userName = name.trim()) }
        applyLanguageContent(normalizedLanguage, refreshRemote = true)
    }

    /** Loads the real YouTube Music home feed (sections), falling back to taste-based search. */
    private fun loadHomeFeed() {
        viewModelScope.launch {
            val hasVisibleHome = _state.value.homeSections.isNotEmpty() || _state.value.tracks.isNotEmpty()
            _state.update { it.copy(isSearching = !hasVisibleHome, searchError = null) }
            val languageCode = _state.value.languageCode
            val sections = runCatching { repository.homeFeed(languageCode) }.getOrDefault(emptyList())
            if (_state.value.languageCode != languageCode) return@launch
            if (sections.isEmpty()) {
                loadHome(preserveCurrent = hasVisibleHome)
                return@launch
            }
            val flat = sections.flatMap { it.tracks }.distinctBy { it.id }
            if (flat.isEmpty()) {
                _state.update { it.copy(isSearching = false) }
                return@launch
            }
            val queue = moodEngine.buildQueue(_state.value.selectedMood, flat)
            preferences.saveHomeSections(sections, languageCode)
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
            persistPersonalOrbit(flat)
            persistHomeSnapshot()
            LevyraArtworkCache.preloadPriority(getApplication<Application>().applicationContext, flat, 16)
            LevyraArtworkCache.preloadHome(getApplication<Application>().applicationContext, flat, 36)
            prefetchTop(flat, 14)
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

    fun setLanguage(code: String) {
        val normalizedLanguage = LevyraLanguageCatalog.normalize(code)
        if (normalizedLanguage == _state.value.languageCode) return
        preferences.setLanguageCode(normalizedLanguage)
        applyLanguageContent(normalizedLanguage, refreshRemote = true)
    }

    private fun applyLanguageContent(languageCode: String, refreshRemote: Boolean) {
        val defaultChartRegion = ChartsCatalog.defaultRegionForLanguage(languageCode)
        val localizedMoods = moodEngine.moodsForLanguage(languageCode)
        val selectedMood = localizedMoods.firstOrNull { it.id == _state.value.selectedMood?.id } ?: localizedMoods.firstOrNull()
        val homeSections = preferences.loadHomeSections(languageCode).ifEmpty { LevyraStartupCatalog.homeSections(languageCode) }
        val homeTracks = homeSections.flatMap { it.tracks }.distinctBy { it.id }
        val chartTracks = preferences.loadChartTracks(languageCode, defaultChartRegion.id).ifEmpty { LevyraStartupCatalog.chartTracks(languageCode) }
        val cachedOrbit = preferences.loadPersonalOrbitTracks(languageCode)
        val seed = mergeTracks(cachedOrbit + _state.value.recentSearches + _state.value.favorites, homeTracks + chartTracks)
        val orbit = LevyraPersonalOrbit.build(
            currentTrack = _state.value.currentTrack,
            recentSearches = _state.value.recentSearches,
            favorites = _state.value.favorites,
            tracks = seed,
            homeSections = homeSections,
            charts = chartTracks,
            cachedOrbit = cachedOrbit,
            limit = LevyraPersonalOrbit.DISPLAY_LIMIT,
            languageCode = languageCode
        )
        val allTracks = mergeTracks(orbit + _state.value.recentSearches + _state.value.favorites, homeTracks + chartTracks)
        val queue = moodEngine.buildQueue(selectedMood, allTracks)
        exploreCache.clear()
        exploreVideosLoaded = false
        _state.update {
            it.copy(
                languageCode = languageCode,
                moods = localizedMoods,
                tastes = moodEngine.tastesForLanguage(languageCode),
                selectedMood = selectedMood,
                selectedChartId = defaultChartRegion.id,
                homeSections = homeSections,
                charts = chartTracks,
                personalOrbitTracks = orbit,
                tracks = allTracks,
                queue = queue,
                searchResults = allTracks.take(12),
                searchSuggestions = emptyList(),
                searchData = SearchResults(),
                searchError = null,
                isSearching = false,
                isLoadingCharts = false,
                exploreZoneId = null,
                exploreTracks = emptyList(),
                exploreVideos = emptyList()
            )
        }
        warmPersistentOrbit(orbit, LevyraPersonalOrbit.DISPLAY_LIMIT, persist = true)
        persistHomeSnapshot()
        if (refreshRemote) {
            loadHomeFeed()
            loadCharts(defaultChartRegion.id)
        }
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
            val hasVisibleCharts = _state.value.charts.isNotEmpty()
            _state.update { it.copy(isLoadingCharts = !hasVisibleCharts) }
            val languageCode = _state.value.languageCode
            val region = ChartsCatalog.region(regionId)
            val result = runCatching { chartsRepository.topSongs(region.country) }.getOrDefault(emptyList())
            if (_state.value.languageCode != languageCode) return@launch
            if (result.isEmpty()) {
                _state.update { if (it.selectedChartId == regionId) it.copy(isLoadingCharts = false) else it }
                return@launch
            }
            preferences.saveChartTracks(result, languageCode, regionId)
            _state.update {
                if (it.selectedChartId != regionId) return@update it
                it.copy(charts = result, isLoadingCharts = false)
            }
            persistPersonalOrbit(result)
            persistHomeSnapshot()
            LevyraArtworkCache.preloadHome(getApplication<Application>().applicationContext, result, 22)
            enrichCharts(regionId, result)
        }
    }

    private fun enrichCharts(regionId: String, charts: List<Track>) {
        chartEnrichJob?.cancel()
        chartEnrichJob = viewModelScope.launch {
            charts.take(40).forEach { entry ->
                if (!isActive || _state.value.selectedChartId != regionId) return@launch
                if (entry.videoUrl.isNotBlank()) return@forEach
                val match = runCatching { repository.searchOne("${entry.title} ${entry.artist}", _state.value.languageCode) }.getOrNull()
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
        LevyraArtworkCache.preloadPriority(getApplication<Application>().applicationContext, updated, 12)
        _state.update { it.copy(favorites = updated, favoriteIds = updated.map { fav -> fav.id }.toSet()) }
        persistPersonalOrbit(updated)
    }

    fun exportCurrentTrack() {
        val track = _state.value.currentTrack ?: return
        exportTrack(track)
    }

    fun openArtist(track: Track) {
        openArtistByName(track.artist)
    }

    fun openArtistByName(name: String) {
        val clean = name.trim()
        if (clean.length < 2 || clean.equals("YouTube Music", ignoreCase = true) || clean.equals("YouTube", ignoreCase = true)) return
        artistJob?.cancel()
        _state.update {
            it.copy(
                showArtist = true,
                artistLoading = true,
                artistError = null,
                artistProfile = if (it.artistProfile?.name.equals(clean, ignoreCase = true)) it.artistProfile else null
            )
        }
        artistJob = viewModelScope.launch {
            val profile = runCatching { artistRepository.profileFor(clean) }.getOrNull()
            if (!isActive) return@launch
            if (profile == null || (profile.topSongs.isEmpty() && !profile.hasBio)) {
                _state.update {
                    it.copy(
                        artistLoading = false,
                        artistError = "Profilo artista non disponibile",
                        artistProfile = profile
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        artistLoading = false,
                        artistError = null,
                        artistProfile = profile
                    )
                }
            }
        }
    }

    fun closeArtist() {
        artistJob?.cancel()
        _state.update { it.copy(showArtist = false, artistLoading = false, artistError = null) }
    }

    fun playArtistSong(track: Track) {
        val profile = _state.value.artistProfile ?: return
        closeArtist()
        playFrom(profile.topSongs, track)
    }

    fun exportTrack(track: Track) {
        val downloadKey = downloadKeyFor(track)
        if (downloadKey in _state.value.downloadingTrackIds || !activeDownloadKeys.add(downloadKey)) {
            _state.update { it.copy(offlineExportMessage = "Download già in corso: ${track.title}") }
            return
        }
        if (track.id.isNotBlank() && track.id in _state.value.downloadedTrackIds) {
            activeDownloadKeys.remove(downloadKey)
            _state.update { it.copy(offlineExportMessage = "Già scaricato: ${track.title}") }
            return
        }
        activeDownloadTitles[downloadKey] = track.title.ifBlank { "brano" }
        _state.update {
            it.copy(
                isOfflineExporting = true,
                offlineExportMessage = "Download 1% · ${activeDownloadTitles[downloadKey]}",
                downloadingTrackIds = it.downloadingTrackIds + downloadKey,
                downloadProgressByTrackId = it.downloadProgressByTrackId + (downloadKey to 1)
            )
        }
        viewModelScope.launch {
            val result = runCatching {
                val appContext = getApplication<Application>().applicationContext
                val payload = TrackPayloadCodec.encode(track)
                val workId = withContext(Dispatchers.IO) {
                    OfflineExportWorker.enqueue(appContext, downloadKey, payload)
                }
                val workManager = WorkManager.getInstance(appContext)
                var finished: WorkInfo? = null
                while (isActive && finished == null) {
                    val info = withContext(Dispatchers.IO) { workManager.getWorkInfoById(workId).get() }
                    if (info != null && info.state.isFinished) {
                        finished = info
                    } else {
                        info?.let { updateDownloadProgress(downloadKey, it.progress.getInt(OfflineExportWorker.KEY_PROGRESS, 0)) }
                        delay(350L)
                    }
                }
                finished ?: throw CancellationException("Offline export observation cancelled")
            }
            result.onSuccess { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> handleOfflineExportSuccess(workInfo, downloadKey)
                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> handleOfflineExportFailure(cleanUserError(workInfo.outputData.getString(OfflineExportWorker.KEY_ERROR)), downloadKey)
                    else -> handleOfflineExportFailure("Esportazione non riuscita", downloadKey)
                }
            }.onFailure { error ->
                if (error is CancellationException) {
                    activeDownloadKeys.remove(downloadKey)
                    activeDownloadTitles.remove(downloadKey)
                    _state.update {
                        it.copy(
                            downloadingTrackIds = it.downloadingTrackIds - downloadKey,
                            downloadProgressByTrackId = it.downloadProgressByTrackId - downloadKey
                        )
                    }
                    throw error
                }
                Timber.e(error, "Offline export work failed")
                handleOfflineExportFailure(cleanUserError(error), downloadKey)
            }
        }
    }

    fun deleteDownload(download: DownloadedTrack) {
        viewModelScope.launch {
            runCatching { downloadedTracksDao.deleteById(download.id) }
            _state.update { it.copy(offlineExportMessage = "Rimosso dai download: ${download.title}") }
        }
    }

    private fun handleOfflineExportSuccess(workInfo: WorkInfo, trackId: String) {
        activeDownloadKeys.remove(trackId)
        activeDownloadTitles.remove(trackId)
        val fileName = workInfo.outputData.getString(OfflineExportWorker.KEY_FILE_NAME).orEmpty()
        val destinationLabel = workInfo.outputData.getString(OfflineExportWorker.KEY_DESTINATION_LABEL).orEmpty().ifBlank { "Music/Levyra" }
        val embedded = workInfo.outputData.getBoolean(OfflineExportWorker.KEY_EMBEDDED_METADATA, false)
        val tagStatus = if (embedded) "con cover e metadata Levyra" else "con metadata Android"
        _state.update {
            it.copy(
                isOfflineExporting = it.downloadingTrackIds.size > 1,
                offlineExportMessage = "Salvato in $destinationLabel: ${fileName.ifBlank { "brano esportato" }} ($tagStatus)",
                embeddedMetadataWriterReady = offlineExporter.embeddedMetadataWriterReady,
                downloadingTrackIds = it.downloadingTrackIds - trackId,
                downloadProgressByTrackId = it.downloadProgressByTrackId - trackId
            )
        }
    }

    private fun handleOfflineExportFailure(message: String?, trackId: String) {
        activeDownloadKeys.remove(trackId)
        activeDownloadTitles.remove(trackId)
        _state.update {
            it.copy(
                isOfflineExporting = it.downloadingTrackIds.size > 1,
                offlineExportMessage = cleanUserError(message),
                embeddedMetadataWriterReady = offlineExporter.embeddedMetadataWriterReady,
                downloadingTrackIds = it.downloadingTrackIds - trackId,
                downloadProgressByTrackId = it.downloadProgressByTrackId - trackId
            )
        }
    }


    private fun cleanUserError(error: Throwable): String {
        if (error is TimeoutCancellationException) return "YouTube lento: sto aspettando lo stream più del previsto, riprova tra qualche secondo"
        return cleanUserError(error.message)
    }

    private fun cleanUserError(message: String?): String {
        val raw = message?.trim().orEmpty()
        if (raw.isBlank()) return "Operazione non riuscita"
        if (raw.contains("Timed out waiting", ignoreCase = true)) return "YouTube lento: sto aspettando lo stream più del previsto, riprova tra qualche secondo"
        if (raw.contains("timeout", ignoreCase = true)) return "Connessione lenta: riprova tra qualche secondo"
        if (raw.contains("Primary directory Music not allowed", ignoreCase = true) || raw.contains("content://media/external_primary/file", ignoreCase = true)) return "Questo telefono blocca il salvataggio generico in Music: aggiorna l'app e riprova, Levyra userà MediaStore Audio o Downloads/Levyra"
        return raw
    }

    private fun updateDownloadProgress(trackId: String, progress: Int) {
        val safeProgress = progress.coerceIn(1, 99)
        _state.update {
            if (trackId !in it.downloadingTrackIds) {
                it
            } else {
                val title = activeDownloadTitles[trackId].orEmpty()
                val previous = it.downloadProgressByTrackId[trackId] ?: 0
                val message = if (title.isBlank() || safeProgress < previous || safeProgress - previous < 3) it.offlineExportMessage else "Download $safeProgress% · $title"
                it.copy(
                    downloadProgressByTrackId = it.downloadProgressByTrackId + (trackId to safeProgress),
                    offlineExportMessage = message
                )
            }
        }
    }

    private fun downloadKeyFor(track: Track): String {
        val raw = track.id.ifBlank { track.videoUrl.ifBlank { "${track.artist}:${track.title}" } }
        return raw.ifBlank { "unknown-${track.hashCode()}" }
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
            snapshot.showArtist -> {
                closeArtist()
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
                runCatching { repository.searchSuggestions(clean, _state.value.languageCode) }.getOrDefault(emptyList()).take(6)
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
        _state.update { it.copy(isSearching = true, searchError = null, searchSuggestions = emptyList(), searchFilter = SearchFilter.All) }
        val result = runCatching { repository.searchEverything(clean, _state.value.languageCode) }
        result.onSuccess { data ->
            val tracks = data.songs
            val mood = _state.value.selectedMood
            val queue = moodEngine.buildQueue(mood, tracks.ifEmpty { repository.cachedTracks() })
            _state.update {
                it.copy(
                    tracks = mergeTracks(it.tracks, tracks),
                    queue = queue,
                    searchResults = tracks,
                    searchData = data,
                    cacheReport = repository.cacheReport(),
                    smartScore = calculateSmartScore(queue),
                    isSearching = false,
                    searchError = if (data.isEmpty) "Nessun risultato trovato per $clean" else null
                )
            }
            persistPersonalOrbit(tracks)
            LevyraArtworkCache.preloadHome(getApplication<Application>().applicationContext, tracks, 36)
            prefetchTop(tracks, 18)
        }.onFailure { error ->
            _state.update {
                it.copy(
                    isSearching = false,
                    searchError = error.message ?: "Ricerca non riuscita"
                )
            }
        }
    }

    fun setSearchFilter(filter: SearchFilter) {
        _state.update { it.copy(searchFilter = filter) }
    }

    fun searchAlbum(album: AlbumHit) {
        _state.update { it.copy(query = album.query) }
        searchNow(album.query)
    }

    fun openArtistFromHit(hit: ArtistHit) {
        openArtistByName(hit.name)
    }

    private fun addToRecentSearches(track: Track) {
        if (track.id.isBlank() || track.title.isBlank()) return
        val current = _state.value.recentSearches
        val updated = listOf(track) + current.filterNot { it.id == track.id }
        val limited = updated.take(8)
        preferences.saveRecentSearches(limited)
        LevyraArtworkCache.preloadPriority(getApplication<Application>().applicationContext, limited, 8)
        _state.update { it.copy(recentSearches = limited) }
        persistPersonalOrbit(limited)
    }

    fun play(track: Track) {
        addToRecentSearches(track)
        val contextualQueue = queueForTrack(track)
        loopCurrentQueueOnCompletion = contextualQueue.size > 1
        _state.update { it.copy(queue = contextualQueue) }
        queueIndex = contextualQueue.indexOfFirst { samePlayableTrack(it, track) }
        startResolve(track)
    }

    fun playFrom(list: List<Track>, track: Track, loopOnCompletion: Boolean = false) {
        addToRecentSearches(track)
        loopCurrentQueueOnCompletion = loopOnCompletion
        _state.update { it.copy(queue = list) }
        queueIndex = list.indexOfFirst { samePlayableTrack(it, track) }
        startResolve(track)
    }

    private fun startResolve(track: Track, preserveCrossfade: Boolean = false) {
        if (!preserveCrossfade) {
            crossfadeJob?.cancel()
            crossfadeInProgress = false
            player.setVolume(1f)
        }
        val requestId = ++playRequestId
        playJob?.cancel()
        cancelBackgroundWarmups(cancelList = false)
        resolver.warmNetwork()

        val playableTrack = youtubePlayableTrack(track) ?: track
        playJob = viewModelScope.launch {
            val local = localDownloadedTrack(track)
            if (local != null) {
                if (!isActive || requestId != playRequestId) return@launch
                startPlayback(local)
                prefetchAround(local)
                return@launch
            }
            val instant = resolver.cached(playableTrack, _state.value.isVideoMode)
            if (instant != null) {
                if (!isActive || requestId != playRequestId) return@launch
                startPlayback(instant)
                prefetchAround(instant)
                return@launch
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
                        playerError = cleanUserError(error)
                    )
                }
            }
        }
    }

    private suspend fun localDownloadedTrack(track: Track): Track? {
        if (track.id.isBlank() || _state.value.isVideoMode) return null
        val entity = withContext(Dispatchers.IO) {
            runCatching { downloadedTracksDao.byTrackId(track.id) }.getOrNull()
        } ?: return null
        if (entity.uri.isBlank()) return null
        val uri = android.net.Uri.parse(entity.uri)
        val readable = withContext(Dispatchers.IO) {
            runCatching {
                when (uri.scheme?.lowercase()) {
                    "content" -> getApplication<Application>().contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
                    "file" -> java.io.File(uri.path.orEmpty()).let { it.exists() && it.length() > 0L }
                    else -> false
                }
            }.getOrDefault(false)
        }
        if (!readable) return null
        return track.copy(
            streamUrl = entity.uri,
            videoStreamUrl = "",
            durationMs = if (track.durationMs > 0L) track.durationMs else entity.durationMs,
            source = "Offline"
        )
    }

    private val exploreCache = ConcurrentHashMap<String, List<Track>>()
    private var exploreVideosLoaded = false
    private var exploreJob: Job? = null

    fun ensureExplore(strings: LevyraStrings) {
        if (_state.value.exploreZoneId == null) {
            selectExploreZone(ExploreCatalog.getZones(strings).first())
        }
        if (!exploreVideosLoaded) {
            exploreVideosLoaded = true
            viewModelScope.launch {
                val videos = runCatching { repository.search("${strings.exploreNewVideos} 2026", 12, _state.value.languageCode) }.getOrDefault(emptyList())
                if (videos.isEmpty()) exploreVideosLoaded = false
                _state.update { it.copy(exploreVideos = videos) }
            }
        }
    }

    fun selectExploreZone(zone: ExploreZone) {
        _state.update { it.copy(exploreZoneId = zone.id) }
        exploreCache[zone.id]?.let { cached ->
            _state.update { it.copy(exploreTracks = cached, isExploreLoading = false) }
            return
        }
        exploreJob?.cancel()
        _state.update { it.copy(exploreTracks = emptyList(), isExploreLoading = true) }
        exploreJob = viewModelScope.launch {
            val results = runCatching { repository.search(zone.query, 24, _state.value.languageCode) }.getOrDefault(emptyList())
            if (results.isNotEmpty()) exploreCache[zone.id] = results
            if (_state.value.exploreZoneId != zone.id) return@launch
            _state.update { it.copy(exploreTracks = results, isExploreLoading = false) }
        }
    }

    fun playDownloaded(download: DownloadedTrack) {
        val track = Track(
            id = download.trackId,
            title = download.title,
            artist = download.artist,
            album = download.album,
            durationMs = download.durationMs,
            streamUrl = "",
            videoUrl = "",
            thumbnailUrl = "",
            largeThumbnailUrl = "",
            source = "Offline",
            moodTags = emptySet(),
            energy = 0,
            vocal = 0,
            replayScore = 0,
            cacheScore = 0,
            accentStart = 0,
            accentEnd = 0
        )
        loopCurrentQueueOnCompletion = false
        _state.update { it.copy(queue = emptyList()) }
        queueIndex = -1
        startResolve(track)
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
        addToRecentSearches(playable)
        fetchLyrics(playable)
        fetchSponsorSegments(playable)
        updateWidget()
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
        prefetchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(350L)
            val queue = _state.value.queue.ifEmpty { currentQueue() }
            if (queue.isEmpty()) return@launch
            val base = if (queueIndex in queue.indices) queueIndex else queue.indexOfFirst { samePlayableTrack(it, playable) }
            if (base < 0) return@launch
            val offsets = if (_state.value.isVideoMode) listOf(1) else listOf(1, 2, 3, -1)
            val candidates = offsets
                .map { offset -> queue[(base + offset + queue.size) % queue.size] }
                .filterNot { samePlayableTrack(it, playable) }
                .distinctBy { playbackIdentity(it) }
            warmTracks(candidates.take(4), concurrency = 3, delayStepMs = 25L, prime = true)
        }
    }

    private fun prefetchTop(tracks: List<Track>, count: Int = 18) {
        val candidates = tracks
            .filter { it.id.isNotBlank() || it.videoUrl.isNotBlank() }
            .distinctBy { youtubePlayableTrack(it)?.id ?: it.id }
            .take(count)
        if (candidates.isEmpty()) return
        listPrefetchJob?.cancel()
        listPrefetchJob = viewModelScope.launch(Dispatchers.IO) {
            resolver.warmNetwork()
            val hot = candidates.take(8)
            val warmOnly = candidates.drop(8)
            warmTracks(hot, concurrency = 4, delayStepMs = 0L, prime = true)
            warmTracks(warmOnly, concurrency = 4, delayStepMs = 25L, prime = false)
        }
    }

    private fun cancelBackgroundWarmups(cancelList: Boolean = true) {
        prefetchJob?.cancel()
        prefetchJob = null
        if (cancelList) {
            listPrefetchJob?.cancel()
            listPrefetchJob = null
        }
    }

    private suspend fun warmTracks(tracks: List<Track>, concurrency: Int, delayStepMs: Long, prime: Boolean) = coroutineScope {
        val semaphore = Semaphore(concurrency.coerceAtLeast(1))
        tracks.distinctBy { youtubePlayableTrack(it)?.id ?: it.id }.forEachIndexed { index, track ->
            launch {
                if (index > 0 && delayStepMs > 0L) delay(index * delayStepMs)
                semaphore.withPermit { warmTrack(track, prime) }
            }
        }
    }

    private suspend fun warmTrack(track: Track, prime: Boolean) {
        val videoMode = _state.value.isVideoMode
        val youtube = youtubePlayableTrack(track)
        val resolved = if (youtube != null) {
            resolver.prefetch(youtube, videoMode)
        } else {
            val match = runCatching { repository.searchOne("${track.title} ${track.artist}", _state.value.languageCode) }.getOrNull() ?: return
            resolver.prefetch(match, videoMode)
        }
        if (resolved != null && prime) {
            if (videoMode) {
                runCatching { playbackWarmup.primeVideo(resolved) }
            } else {
                runCatching { playbackWarmup.prime(resolved) }
            }
        }
    }

    private fun currentQueue(): List<Track> {
        val snapshot = _state.value
        return snapshot.queue.ifEmpty { snapshot.searchResults }.ifEmpty { snapshot.tracks }
    }

    private fun queueForTrack(track: Track): List<Track> {
        val snapshot = _state.value
        val sections = snapshot.homeSections.firstOrNull { section -> section.tracks.any { samePlayableTrack(it, track) } }?.tracks.orEmpty()
        val candidates = listOf(
            snapshot.openPlaylist?.tracks.orEmpty(),
            sections,
            snapshot.searchResults,
            snapshot.charts,
            snapshot.personalOrbitTracks,
            snapshot.tracks,
            snapshot.queue
        )
        val selected = candidates.firstOrNull { list -> list.any { samePlayableTrack(it, track) } && list.size > 1 }
            ?: candidates.firstOrNull { list -> list.any { samePlayableTrack(it, track) } }
            ?: listOf(track)
        return selected.distinctBy { playbackIdentity(it) }
    }

    private fun samePlayableTrack(left: Track, right: Track): Boolean = playbackIdentity(left) == playbackIdentity(right)

    private fun playbackIdentity(track: Track): String = youtubePlayableTrack(track)?.id?.takeIf { it.isNotBlank() }
        ?: track.id.ifBlank { track.videoUrl.ifBlank { "${track.artist}|${track.title}" } }.trim().lowercase()

    fun play() = _state.value.currentTrack?.let { player.play(it) }
    fun pause() = player.pause()

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
        updateWidget()
    }

    fun closePlayer() {
        loopCurrentQueueOnCompletion = false
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
        updateWidget()
    }

    fun next() {
        val queue = _state.value.queue.ifEmpty { currentQueue() }
        if (queue.isEmpty()) return
        if (queue.size == 1) {
            queueIndex = 0
            val current = _state.value.currentTrack
            if (current?.streamUrl?.isNotBlank() == true) {
                player.seekTo(0L)
                player.play(current)
                _state.update { it.copy(isPlaying = true, positionMs = 0L) }
            } else {
                startResolve(queue[0])
            }
            return
        }
        if (_state.value.shuffleEnabled && queue.size > 1) {
            queueIndex = queue.indices.filter { it != queueIndex }.ifEmpty { queue.indices.toList() }.random()
            startResolve(queue[queueIndex])
            return
        }
        val base = if (queueIndex in queue.indices) queueIndex else _state.value.currentTrack?.let { current -> queue.indexOfFirst { samePlayableTrack(it, current) } } ?: -1
        val nextIndex = if (base < 0) 0 else (base + 1) % queue.size
        queueIndex = nextIndex
        startResolve(queue[nextIndex])
    }

    fun previous() {
        val queue = _state.value.queue.ifEmpty { currentQueue() }
        if (queue.isEmpty()) return
        val base = if (queueIndex in queue.indices) queueIndex else _state.value.currentTrack?.let { current -> queue.indexOfFirst { samePlayableTrack(it, current) } } ?: -1
        val previousIndex = if (base <= 0) queue.lastIndex else base - 1
        queueIndex = previousIndex
        startResolve(queue[previousIndex])
    }

    fun seekTo(progress: Float) {
        val duration = _state.value.durationMs.coerceAtLeast(1L)
        player.seekTo((duration * progress.coerceIn(0f, 1f)).toLong())
    }

    private fun loadHome(preserveCurrent: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isSearching = !preserveCurrent, searchError = null) }
            val languageCode = _state.value.languageCode
            val tasteIds = preferences.tastes()
            val queries = (LevyraLocalizedDiscovery.homeBoostQueries(languageCode, tasteIds) + moodEngine.queriesForTastes(tasteIds, languageCode)).distinct().take(12)
            val result = runCatching { repository.home(queries, languageCode) }
            result.onSuccess { tracks ->
                if (_state.value.languageCode != languageCode) return@onSuccess
                if (tracks.isEmpty()) {
                    _state.update {
                        it.copy(
                            isSearching = false,
                            searchError = if (preserveCurrent) null else "Home remota vuota: prova una ricerca"
                        )
                    }
                    return@onSuccess
                }
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
                        searchError = null
                    )
                }
                val fallbackSection = com.luc4n3x.levyra.domain.HomeSection(LevyraContentLocales.forLanguage(languageCode).quickSectionTitle, tracks.take(20))
                preferences.saveHomeSections(listOf(fallbackSection), languageCode)
                _state.update { current -> if (current.homeSections.isEmpty()) current.copy(homeSections = listOf(fallbackSection)) else current }
                persistPersonalOrbit(tracks)
                persistHomeSnapshot()
                LevyraArtworkCache.preloadHome(getApplication<Application>().applicationContext, tracks, 32)
                prefetchTop(tracks, 14)
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isSearching = false,
                        searchError = if (preserveCurrent) null else error.message ?: "Home non caricata"
                    )
                }
            }
        }
    }

    private fun searchMood(mood: Mood) {
        val query = moodEngine.tagQueryFor(mood, _state.value.languageCode)
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
  
                if (snapshot.sponsorBlockEnabled && sponsorSegments.isNotEmpty() && player.isPlaying) {
                    val pos = player.positionMs
                    val segment = sponsorSegments.firstOrNull { pos >= it.startMs && pos < it.endMs - 250 }
                    if (segment != null) player.seekTo(segment.endMs)
                }
                maybeStartCrossfade(snapshot, current, player.positionMs, duration)
 
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

                if (current != null && player.isPlaying && ticks % 4 == 0) {
                    preferences.saveLastPlayback(current, position)
                }
                if (snapshot.isPlaying != player.isPlaying) {
                    updateWidget()
                }
                ticks++
                delay(500L)
            }
        }
    }


    private fun maybeStartCrossfade(snapshot: LevyraUiState, current: Track?, position: Long, duration: Long) {
        val settings = snapshot.audioSettings
        if (!settings.gaplessEnabled || settings.crossfadeSeconds <= 0 || current == null || crossfadeInProgress || !player.isPlaying) return
        if (duration <= 30_000L) return
        val windowMs = settings.crossfadeSeconds * 1000L
        if (position < duration - windowMs) return
        val nextTrack = nextTrackForCrossfade(snapshot) ?: return
        crossfadeInProgress = true
        crossfadeJob?.cancel()
        crossfadeJob = viewModelScope.launch {
            val fadeOutMs = if (settings.djSoftMode) maxOf(windowMs, 3_500L) else windowMs
            fadeVolume(from = 1f, to = 0.08f, durationMs = fadeOutMs.coerceAtLeast(800L))
            startResolve(nextTrack, preserveCrossfade = true)
            val targetId = youtubePlayableTrack(nextTrack)?.id ?: nextTrack.id
            var waited = 0L
            while (isActive && waited < 8_000L) {
                val active = _state.value.currentTrack
                val activeId = active?.id.orEmpty()
                if ((activeId == targetId || activeId == nextTrack.id) && active?.streamUrl?.isNotBlank() == true && player.isPlaying) break
                delay(120L)
                waited += 120L
            }
            fadeVolume(from = 0.08f, to = 1f, durationMs = if (settings.djSoftMode) 1_800L else 900L)
            player.setVolume(1f)
            crossfadeInProgress = false
        }
    }

    private fun nextTrackForCrossfade(snapshot: LevyraUiState): Track? {
        if (snapshot.repeatMode == RepeatMode.One) return null
        val queue = snapshot.queue.ifEmpty { currentQueue() }
        if (queue.isEmpty()) return null
        if (snapshot.shuffleEnabled && queue.size > 1) {
            val nextIndex = queue.indices.filter { it != queueIndex }.ifEmpty { queue.indices.toList() }.random()
            queueIndex = nextIndex
            return queue[nextIndex]
        }
        val base = if (queueIndex in queue.indices) queueIndex else snapshot.currentTrack?.let { current -> queue.indexOfFirst { samePlayableTrack(it, current) } } ?: -1
        if (base < 0) return queue.firstOrNull()
        if (base < queue.lastIndex) {
            queueIndex = base + 1
            return queue[queueIndex]
        }
        if (snapshot.repeatMode == RepeatMode.All || loopCurrentQueueOnCompletion) {
            queueIndex = 0
            return queue.firstOrNull()
        }
        return null
    }

    private suspend fun fadeVolume(from: Float, to: Float, durationMs: Long) {
        val steps = 12
        val safeDuration = durationMs.coerceAtLeast(120L)
        repeat(steps + 1) { step ->
            val t = step.toFloat() / steps.toFloat()
            player.setVolume(from + ((to - from) * t))
            delay(safeDuration / steps)
        }
    }


    private suspend fun resolveForPlayback(track: Track): Track {
        youtubePlayableTrack(track)?.let { return resolvePlayableTrack(it) }
        val match = repository.searchOne("${track.title} ${track.artist}", _state.value.languageCode)
            ?: throw IllegalStateException("Nessun risultato YouTube per ${track.title}")
        val carried = match.copy(
            thumbnailUrl = match.thumbnailUrl.ifBlank { track.thumbnailUrl },
            largeThumbnailUrl = match.largeThumbnailUrl.ifBlank { track.largeThumbnailUrl }
        )
        return resolvePlayableTrack(carried)
    }

    private suspend fun resolvePlayableTrack(track: Track): Track {
        val resolved = resolver.resolve(track.copy(streamUrl = ""), _state.value.isVideoMode)
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
        LevyraWidgetBridge.clear()
        _state.value.currentTrack?.let { preferences.saveLastPlayback(it, player.positionMs) }
        playJob?.cancel()
        cancelBackgroundWarmups()
        chartEnrichJob?.cancel()
        sleepJob?.cancel()
        lyricsJob?.cancel()
        sponsorJob?.cancel()
        artistJob?.cancel()
        radarJob?.cancel()
        crossfadeJob?.cancel()
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

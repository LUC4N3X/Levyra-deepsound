package com.luc4n3x.levyra.viewmodel

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
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
import com.luc4n3x.levyra.data.LevyraBackupManager
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.LevyraHomeSnapshotCache
import com.luc4n3x.levyra.data.LevyraStartupCatalog
import com.luc4n3x.levyra.data.HomeInteractionGate
import com.luc4n3x.levyra.data.HomeRefreshStability
import com.luc4n3x.levyra.data.HomeStartupWorkPlan
import com.luc4n3x.levyra.data.HomeStartupWorkPolicy
import com.luc4n3x.levyra.data.StartupPlaybackWarmPolicy
import com.luc4n3x.levyra.data.LevyraSmartMusicProfileStore
import com.luc4n3x.levyra.data.ListeningPulseStore
import com.luc4n3x.levyra.data.OfficialArtworkRepository
import com.luc4n3x.levyra.data.LyricsRepository
import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.data.SponsorBlockRepository
import com.luc4n3x.levyra.data.TrackPayloadCodec
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE
import com.luc4n3x.levyra.data.levyraAlbumRecommendationMatchScore
import com.luc4n3x.levyra.data.albumRecommendationIdentityKey
import com.luc4n3x.levyra.data.albumRecommendationTextKey
import com.luc4n3x.levyra.data.local.DownloadEntity
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.ArtistRelease
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.AlbumRecommendationSeed
import com.luc4n3x.levyra.domain.AlbumDetail
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.artistIdentityKey
import com.luc4n3x.levyra.domain.isArtistShelfNameEligible
import com.luc4n3x.levyra.domain.primaryArtistSegment
import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.FollowedArtist
import com.luc4n3x.levyra.domain.ReleaseRadarEntry
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.SmartMusicProfile
import com.luc4n3x.levyra.domain.SponsorSegment
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.LevyraAudioPresets
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.LevyraDownloadSettings
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.LevyraLocalIntelligence
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.ListeningPulseEngine
import com.luc4n3x.levyra.domain.LevyraLocalizedDiscovery
import com.luc4n3x.levyra.domain.LyricsEngine
import com.luc4n3x.levyra.domain.Mood
import com.luc4n3x.levyra.domain.MoodEngine
import com.luc4n3x.levyra.domain.OfflineDownloadTask
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.theme.LevyraThemes
import com.luc4n3x.levyra.widget.LevyraWidgetBridge
import com.luc4n3x.levyra.widget.LevyraWidgetCenter
import com.luc4n3x.levyra.player.AdaptivePlaybackPolicy
import com.luc4n3x.levyra.player.LevyraPlayer
import com.luc4n3x.levyra.player.PlaybackService
import com.luc4n3x.levyra.player.PlaybackWarmup
import com.luc4n3x.levyra.player.queue.PersistentQueueEngine
import com.luc4n3x.levyra.player.queue.playbackQueueIdentity
import com.luc4n3x.levyra.player.offline.OfflineAudioExporter
import com.luc4n3x.levyra.player.offline.work.OfflineExportWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

private const val ARTIST_PROFILE_UNAVAILABLE_ERROR = "artist_profile_unavailable"

private data class HomeArtistCandidate(
    val name: String,
    val browseId: String
)


internal fun monotonicDownloadProgress(current: Int?, incoming: Int): Int {
    val safeIncoming = incoming.coerceIn(1, 99)
    val safeCurrent = current?.coerceIn(1, 99) ?: 1
    return maxOf(safeCurrent, safeIncoming)
}

internal fun isPlaybackCandidateCompatible(target: Track, candidate: Track): Boolean {
    val targetTitle = playbackTextKey(target.title)
    val candidateTitle = playbackTextKey(candidate.title)
    if (targetTitle.isBlank() || candidateTitle.isBlank()) return false

    val targetTitleTokens = playbackTokens(targetTitle)
    val candidateTitleTokens = playbackTokens(candidateTitle).toSet()
    val titleMatches = targetTitleTokens.count { it in candidateTitleTokens }
    val titleCoverage = if (targetTitleTokens.isEmpty()) 0.0 else titleMatches.toDouble() / targetTitleTokens.size.toDouble()
    val titleCompatible = candidateTitle == targetTitle ||
        candidateTitle.startsWith("$targetTitle ") ||
        targetTitle.startsWith("$candidateTitle ") ||
        titleCoverage >= 0.8
    if (!titleCompatible) return false

    val targetArtistTokens = playbackArtistTokens(target.artist)
    if (targetArtistTokens.isEmpty()) return true
    val candidateArtistTokens = playbackArtistTokens("${candidate.artist} ${candidate.title}").toSet()
    val artistMatches = targetArtistTokens.count { it in candidateArtistTokens }
    val requiredMatches = maxOf(1, (targetArtistTokens.size + 1) / 2)
    return artistMatches >= requiredMatches
}

internal fun playbackCandidateScore(target: Track, candidate: Track): Int {
    val targetTitle = playbackTextKey(target.title)
    val targetArtist = playbackTextKey(target.artist)
    val candidateTitle = playbackTextKey(candidate.title)
    val candidateArtist = playbackTextKey(candidate.artist)
    val candidateBlob = playbackTextKey("${candidate.title} ${candidate.artist} ${candidate.album}")
    val titleTokens = playbackTokens(targetTitle)
    val artistTokens = playbackArtistTokens(target.artist)
    var score = 0
    when {
        candidateTitle == targetTitle -> score += 140
        candidateTitle.contains(targetTitle) || targetTitle.contains(candidateTitle) -> score += 95
        titleTokens.isNotEmpty() && titleTokens.all { candidateBlob.split(' ').contains(it) } -> score += 70
    }
    when {
        targetArtist.isNotBlank() && candidateArtist == targetArtist -> score += 80
        targetArtist.isNotBlank() && (candidateArtist.contains(targetArtist) || targetArtist.contains(candidateArtist)) -> score += 55
        artistTokens.isNotEmpty() && artistTokens.all { candidateBlob.split(' ').contains(it) } -> score += 35
    }
    val penaltyTerms = listOf("karaoke", "cover", "reaction", "sped up", "slowed", "instrumental", "remix", "live", "nightcore")
    if (penaltyTerms.any { candidateBlob.contains(it) } && penaltyTerms.none { targetTitle.contains(it) }) score -= 60
    if (candidate.source.contains("YouTube Music", ignoreCase = true)) score += 18
    if (candidate.source.contains("Extractor", ignoreCase = true)) score += 8
    if (candidate.durationMs > 0L && target.durationMs > 0L) {
        val delta = kotlin.math.abs(candidate.durationMs - target.durationMs)
        if (delta <= 5_000L) score += 30 else if (delta > 35_000L) score -= 25
    }
    return score
}

internal fun playbackTextKey(value: String): String {
    return value.lowercase()
        .replace(Regex("""[()\[\]]"""), " ")
        .replace(Regex("""[^a-z0-9àèéìòóùçñäöüß\s]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

private fun playbackTokens(value: String): List<String> {
    return playbackTextKey(value).split(' ').filter { it.length >= 2 }
}

private fun playbackArtistTokens(value: String): List<String> {
    val ignored = setOf("feat", "featuring", "ft", "and", "the", "con", "with", "vs")
    return playbackTokens(value).filterNot { it in ignored }
}

class LevyraViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YoutubeMusicRepository(application.applicationContext)
    private val artistRepository = ArtistRepository(repository, application.applicationContext)
    private val chartsRepository = ChartsRepository()
    private val officialArtworkRepository = OfficialArtworkRepository(application.applicationContext)
    private val database = LevyraDatabase.get(application.applicationContext)
    private val downloadedTracksDao = database.downloadedTracksDao()
    private val offlineDownloadTasksDao = database.offlineDownloadTasksDao()
    private val appUpdateRepository = AppUpdateRepository(application.applicationContext)
    private val lyricsRepository = LyricsRepository(application.applicationContext)
    private val sponsorBlockRepository = SponsorBlockRepository()
    private val resolver = PlaybackResolver.getInstance(application.applicationContext)
    private val moodEngine = MoodEngine()
    private val lyricsEngine = LyricsEngine()
    private val localIntelligence = LevyraLocalIntelligence()
    private val backupManager = LevyraBackupManager(application.applicationContext)
    private val player = LevyraPlayer(application.applicationContext)
    private val playbackWarmup = PlaybackWarmup(application.applicationContext)
    private val adaptivePlaybackPolicy = AdaptivePlaybackPolicy(application.applicationContext)
    private val queueEngine = PersistentQueueEngine.get(application.applicationContext)
    private val offlineExporter = OfflineAudioExporter(application.applicationContext, resolver)
    private val favoritesStore = FavoritesStore(application.applicationContext)
    private val followedArtistsStore = FollowedArtistsStore(application.applicationContext)
    private val playlistStore = com.luc4n3x.levyra.data.PlaylistStore(application.applicationContext)
    private val preferences = LevyraPreferences(application.applicationContext)
    private val homeSnapshotCache = LevyraHomeSnapshotCache(application.applicationContext)
    private val smartMusicProfileStore = LevyraSmartMusicProfileStore(application.applicationContext)
    private val listeningPulseStore = ListeningPulseStore(application.applicationContext)
    private val listeningPulseEngine = ListeningPulseEngine()
    private val startupSmartProfile = smartMusicProfileStore.load()
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
            smartProfile = startupSmartProfile,
            audioNormalization = startupSettings.audioNormalization,
            audioSettings = startupSettings.audioSettings,
            lyricsTranslationEnabled = startupSettings.lyricsTranslationEnabled,
            interfaceSettings = startupSettings.interfaceSettings,
            downloadSettings = startupSettings.downloadSettings,
            playbackDiagnostics = resolver.playbackDiagnostics()
        )
    )
    private var searchJob: Job? = null
    private var playJob: Job? = null
    private var modeSwitchJob: Job? = null
    private var streamRecoveryJob: Job? = null
    private var alternateModePrefetchJob: Job? = null
    private var prefetchJob: Job? = null
    private var chartEnrichJob: Job? = null
    private var orbitArtworkJob: Job? = null
    private var sleepJob: Job? = null
    private var crossfadeJob: Job? = null
    private var crossfadeInProgress = false
    private var lyricsJob: Job? = null
    private var lyricsPrefetchJob: Job? = null
    private var lyricsTrackId: String = ""
    private var lyricsRequestGeneration = 0L
    private var sponsorJob: Job? = null
    private var listPrefetchJob: Job? = null
    private var updateJob: Job? = null
    private var artistJob: Job? = null
    private var albumJob: Job? = null
    private var homeFeedJob: Job? = null
    private var homeAlbumsJob: Job? = null
    private var homeArtistsJob: Job? = null
    private var chartsJob: Job? = null
    private var homeSnapshotJob: Job? = null
    private var homeArtistsFingerprint: String = ""
    @Volatile private var deferredHomeArtistsSnapshot: List<ArtistHit>? = null
    @Volatile private var deferredHomeSectionsSnapshot: List<com.luc4n3x.levyra.domain.HomeSection>? = null
    private var radarJob: Job? = null
    private var radioJob: Job? = null
    private var sponsorSegments: List<SponsorSegment> = emptyList()
    private val tabBackStack = ArrayDeque<LevyraTab>()
    private var playRequestId: Long = 0L
    private var streamTransitionId: Long = 0L
    private var pendingSeekMs: Long = 0L
    private var queueIndex: Int = -1
    private var loopCurrentQueueOnCompletion: Boolean = false
    private var listenSessionTrack: Track? = null
    private var listenSessionStartedAt = 0L
    private var listenSessionAccumulatedMs = 0L
    private var listenSessionCompleted = false
    private var listenTickElapsedMs = 0L
    private var listenSessionPersistedMs = 0L
    private var listenSessionPersistJob: Job? = null
    private var listeningPulseRefreshJob: Job? = null
    private var lastPlaybackSaveJob: Job? = null
    private var lastListeningPulseRefreshMs = 0L
    private val activeDownloadKeys = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val activeDownloadTitles = ConcurrentHashMap<String, String>()
    private val activeDownloadTracks = ConcurrentHashMap<String, Track>()
    private val officialMetadataSignal = Channel<Unit>(Channel.CONFLATED)
    private val officialMetadataPending = ConcurrentHashMap<String, Track>()
    private val officialMetadataInFlightKeys = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val officialMetadataDeferUntilIdle = AtomicBoolean(false)

    private val homeInteractionGate = HomeInteractionGate()
    private val homeFeedRequestGeneration = AtomicLong(0L)
    private val homeAlbumsRequestGeneration = AtomicLong(0L)
    private val chartsRequestGeneration = AtomicLong(0L)
    private val pendingHomeSectionsSnapshot = AtomicReference<List<com.luc4n3x.levyra.domain.HomeSection>?>(null)
    @Volatile private var homeScreenActive = false
    @Volatile private var homeAtTop = true

    private fun homeStartupWorkPlan(): HomeStartupWorkPlan {
        val playbackPlan = adaptivePlaybackPolicy.current(videoMode = false)
        return HomeStartupWorkPolicy.create(
            lowRam = playbackPlan.lowRam,
            powerConstrained = playbackPlan.powerConstrained
        )
    }

    fun setHomeViewport(scrollInProgress: Boolean, atTop: Boolean) {
        homeAtTop = atTop
        homeInteractionGate.update(scrollInProgress)
    }

    fun onHomeEntered() {
        homeScreenActive = true
        homeAtTop = true
        val pendingSections = pendingHomeSectionsSnapshot.getAndSet(null)
        val pendingArtists = deferredHomeArtistsSnapshot
        deferredHomeSectionsSnapshot = null
        deferredHomeArtistsSnapshot = null
        val snapshot = _state.value
        val sectionResult = pendingSections?.let { sections ->
            HomeRefreshStability.mergeSections(
                previous = snapshot.homeSections,
                incoming = sections,
                allowStructuralChanges = true
            )
        }
        val nextSections = sectionResult?.visible ?: snapshot.homeSections
        val nextArtists = pendingArtists?.takeIf { it.isNotEmpty() } ?: snapshot.homeArtists
        val flat = if (sectionResult?.changed == true) {
            nextSections.flatMap { it.tracks }.distinctBy { it.id }
        } else {
            emptyList()
        }
        val changed = sectionResult?.changed == true || nextArtists != snapshot.homeArtists
        if (!changed) return
        _state.update { current ->
            current.copy(
                homeSections = nextSections,
                homeArtists = nextArtists,
                homeArtistsLoading = false,
                tracks = flat.ifEmpty { current.tracks },
                searchResults = flat.take(12).ifEmpty { current.searchResults }
            )
        }
        persistHomeSnapshot()
    }

    fun onHomeLeft() {
        homeScreenActive = false
        homeAtTop = true
        homeInteractionGate.update(false)
    }

    private suspend fun awaitHomeUiIdle(plan: HomeStartupWorkPlan = homeStartupWorkPlan()) {
        homeInteractionGate.awaitIdle(plan.idleWindowMs)
    }

    val state: StateFlow<LevyraUiState> = _state.asStateFlow()
    val playerController get() = player.controller

    init {
        val favorites = favoritesStore.load()
        val settings = startupSettings
        val repairedRecentSearches = settings.recentSearches
            .map(LevyraPersonalOrbit::withoutVideoArtwork)
            .distinctBy(LevyraPersonalOrbit::identityKey)
            .take(LevyraPersonalOrbit.DISPLAY_LIMIT)
        val defaultChartRegion = ChartsCatalog.defaultRegionForLanguage(settings.languageCode)
        val instantSnapshot = homeSnapshotCache.load(settings.languageCode)
        val cachedHomeSections = instantSnapshot?.homeSections?.takeIf { it.isNotEmpty() } ?: preferences.loadHomeSections(settings.languageCode)
        val startupHomeSections = LevyraStartupCatalog.repairHomeSections(
            cachedHomeSections.ifEmpty { LevyraStartupCatalog.homeSections(settings.languageCode) },
            settings.languageCode
        )
        val startupHomeTracks = startupHomeSections.flatMap { it.tracks }.distinctBy { it.id }
        val startupHomeArtists = instantSnapshot?.homeArtists
            .orEmpty()
            .filter { it.name.isNotBlank() && it.browseId.isNotBlank() && it.thumbnailUrl.isNotBlank() }
            .distinctBy { it.browseId.lowercase() }
            .take(HOME_ARTIST_SHELF_SIZE)
        val cachedCharts = instantSnapshot?.charts?.takeIf { it.isNotEmpty() } ?: preferences.loadChartTracks(settings.languageCode, defaultChartRegion.id)
        val startupCharts = LevyraStartupCatalog.repairTracks(
            cachedCharts.ifEmpty { LevyraStartupCatalog.chartTracks(settings.languageCode) },
            settings.languageCode
        )
        val rawCachedOrbitTracks = LevyraStartupCatalog.repairTracks(
            settings.personalOrbitTracks
                .ifEmpty { instantSnapshot?.personalOrbit.orEmpty() }
                .ifEmpty { preferences.loadPersonalOrbitTracks(settings.languageCode) },
            settings.languageCode
        )
        val startupOrbitSeed = mergeTracks(rawCachedOrbitTracks + repairedRecentSearches + favorites, startupHomeTracks + startupCharts)
        val cachedOrbitTracks = LevyraPersonalOrbit.build(
            currentTrack = null,
            recentSearches = repairedRecentSearches,
            favorites = favorites,
            tracks = startupOrbitSeed,
            homeSections = startupHomeSections,
            charts = startupCharts,
            cachedOrbit = rawCachedOrbitTracks,
            limit = LevyraPersonalOrbit.DISPLAY_LIMIT,
            languageCode = settings.languageCode
        )
        val initialTracks = mergeTracks(cachedOrbitTracks + repairedRecentSearches + favorites, startupHomeTracks + startupCharts)
        val startupInstantAlbums = instantAlbumRecommendationsFromTracks(
            primary = repairedRecentSearches + favorites,
            secondary = cachedOrbitTracks + initialTracks,
            limit = HOME_ALBUM_RECOMMENDATION_LIMIT,
            profile = startupSmartProfile
        )
        val startupAlbumState = LevyraUiState(
            languageCode = settings.languageCode,
            recentSearches = repairedRecentSearches,
            personalOrbitTracks = cachedOrbitTracks,
            favorites = favorites,
            homeSections = startupHomeSections,
            charts = startupCharts,
            tracks = initialTracks,
            smartProfile = startupSmartProfile
        )
        val startupAlbums = rankAlbumRecommendations(
            remote = preferences.loadHomeAlbums(settings.languageCode),
            instant = startupInstantAlbums,
            state = startupAlbumState,
            limit = HOME_ALBUM_RECOMMENDATION_LIMIT
        ).ifEmpty { startupInstantAlbums }
        val initialQueue = moodEngine.buildQueue(startupMoods.firstOrNull(), initialTracks)
        val restoredTrack = settings.lastTrack
            ?.copy(streamUrl = "", videoStreamUrl = "")
            ?.let(LevyraPersonalOrbit::withoutVideoArtwork)
        pendingSeekMs = settings.lastPositionMs.coerceAtLeast(0L)
        resolver.setAudioQuality(settings.audioQuality)
        _state.update {
            it.copy(
                favorites = favorites,
                favoriteIds = favorites.map { fav -> fav.id }.toSet(),
                recentSearches = repairedRecentSearches,
                personalOrbitTracks = cachedOrbitTracks,
                homeSections = startupHomeSections,
                homeAlbums = startupAlbums,
                homeArtists = startupHomeArtists,
                homeArtistsLoading = startupHomeArtists.isEmpty(),
                homeAlbumsLoading = startupAlbums.isEmpty(),
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
                animationsEnabled = settings.animationsEnabled && !adaptivePlaybackPolicy.current(videoMode = false).lowRam,
                dynamicColor = settings.dynamicColor,
                sponsorBlockEnabled = settings.sponsorBlock,
                skipSilence = settings.skipSilence,
                audioQuality = settings.audioQuality,
                audioNormalization = settings.audioNormalization,
                audioSettings = settings.audioSettings,
                lyricsTranslationEnabled = settings.lyricsTranslationEnabled,
                playbackSpeed = settings.audioSettings.playbackSpeed,
                themePreset = settings.themePreset,
                showOnboarding = !settings.onboarded,
                currentTrack = restoredTrack,
                positionMs = pendingSeekMs,
                durationMs = restoredTrack?.durationMs ?: 0L,
                lyrics = emptyList()
            )
        }
        if (repairedRecentSearches != settings.recentSearches) {
            viewModelScope.launch(Dispatchers.IO) {
                preferences.saveRecentSearches(repairedRecentSearches)
            }
        }
        val fallbackQueue = restoredTrack
            ?.let { track -> (listOf(track) + initialQueue).distinctBy { playbackIdentity(it) } }
            .orEmpty()
        val fallbackIndex = restoredTrack
            ?.let { target -> fallbackQueue.indexOfFirst { samePlayableTrack(it, target) } }
            ?.takeIf { it >= 0 }
            ?: -1
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                queueEngine.restore(
                    fallbackTracks = fallbackQueue,
                    fallbackIndex = fallbackIndex,
                    fallbackPositionMs = pendingSeekMs,
                    fallbackRepeatMode = RepeatMode.Off,
                    fallbackShuffleEnabled = false,
                    fallbackRadioEnabled = true
                )
            }
            queueEngine.state.collectLatest { queueSnapshot ->
                val previousIndex = queueIndex
                queueIndex = queueSnapshot.currentIndex
                val currentPersisted = queueSnapshot.currentTrack
                val externalSelectionChanged = previousIndex != queueSnapshot.currentIndex
                if ((!_state.value.isPlaying || externalSelectionChanged) && !_state.value.isResolving && currentPersisted != null) {
                    pendingSeekMs = queueSnapshot.positionMs
                }
                _state.update { current ->
                    val synchronizeCurrent = !current.isResolving && (!current.isPlaying || externalSelectionChanged)
                    current.copy(
                        queue = queueSnapshot.tracks,
                        queueCurrentIndex = queueSnapshot.currentIndex,
                        queueUndoAvailable = queueSnapshot.undoAvailable,
                        queueHistoryCount = queueSnapshot.history.size,
                        repeatMode = queueSnapshot.repeatMode,
                        shuffleEnabled = queueSnapshot.shuffleEnabled,
                        radioEnabled = queueSnapshot.radioEnabled,
                        currentTrack = if (synchronizeCurrent) currentPersisted else current.currentTrack,
                        positionMs = if (synchronizeCurrent) queueSnapshot.positionMs else current.positionMs,
                        durationMs = if (synchronizeCurrent) currentPersisted?.durationMs ?: current.durationMs else current.durationMs
                    )
                }
            }
        }
        player.setSkipSilence(settings.skipSilence)
        player.setPremiumAudioSettings(settings.audioSettings)
        player.setPlayback(settings.audioSettings.playbackSpeed, settings.audioSettings.pitch)
        player.onCompletion = { onTrackCompleted() }
        player.onRecoverableStreamError = { track, positionMs, videoMode, playWhenReady, errorMessage ->
            recoverPlaybackStream(track, positionMs, videoMode, playWhenReady, errorMessage)
        }
        player.onError = { errorMsg ->
            _state.value.currentTrack?.let { resolver.invalidate(it, _state.value.isVideoMode) }
            _state.update { it.copy(playerError = cleanUserError(errorMsg), isPlaying = false, isResolving = false) }
        }
        applyFollowedArtists(followedArtistsStore.load())
        startTicker()
        observeDownloads()
        observeDownloadTasks()
        loadPlaylists()
        viewModelScope.launch(Dispatchers.Default) { consumeOfficialMetadataQueue() }
        refreshListeningPulse(force = true)
        scheduleColdStartRefresh(initialTracks)
        LevyraWidgetBridge.onToggle = { togglePlay() }
        LevyraWidgetBridge.onNext = { next() }
        LevyraWidgetBridge.onPrevious = { previous() }
        updateWidget()
    }

    private fun observeDownloadTasks() {
        viewModelScope.launch {
            offlineDownloadTasksDao.observeActive().collectLatest { tasks ->
                val domainTasks = tasks.map { task ->
                    OfflineDownloadTask(
                        taskKey = task.taskKey,
                        trackId = task.trackId,
                        title = task.title,
                        artist = task.artist,
                        state = task.state,
                        progress = task.progress,
                        error = task.error
                    )
                }
                val runningTasks = domainTasks.filter { it.state in setOf("QUEUED", "RUNNING", "RETRYING") }
                val ids = runningTasks.mapTo(linkedSetOf()) { it.taskKey }
                val progress = runningTasks.associate { it.taskKey to it.progress.coerceIn(0, 99) }
                val titles = runningTasks.associate { it.taskKey to it.title.ifBlank { "brano" } }
                _state.update {
                    it.copy(
                        downloadQueue = domainTasks,
                        offlineQueueSize = runningTasks.size,
                        isOfflineExporting = runningTasks.isNotEmpty(),
                        downloadingTrackIds = ids,
                        downloadProgressByTrackId = progress,
                        downloadTitleByTrackId = titles
                    )
                }
            }
        }
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
        val orbitSeed = _state.value.personalOrbitTracks.take(LevyraPersonalOrbit.DISPLAY_LIMIT)
        val playbackPlan = adaptivePlaybackPolicy.current(videoMode = false)
        val playbackWarmPlan = StartupPlaybackWarmPolicy.create(
            lowRam = playbackPlan.lowRam,
            powerConstrained = playbackPlan.powerConstrained,
            preferredConcurrency = playbackPlan.concurrency
        )
        val startupPlan = homeStartupWorkPlan()

        viewModelScope.launch(Dispatchers.IO) {
            delay(playbackWarmPlan.delayMs)
            resolver.warmNetwork()
            val hot = (orbitSeed.take(1) + initialTracks.take(playbackWarmPlan.trackCount))
                .filter { it.id.isNotBlank() || it.videoUrl.isNotBlank() || it.title.isNotBlank() }
                .distinctBy { playbackIdentity(it) }
                .take(playbackWarmPlan.trackCount)
            warmTracks(
                tracks = hot,
                concurrency = playbackWarmPlan.concurrency,
                delayStepMs = playbackPlan.staggerMs,
                prime = true
            )
        }

        if (orbitSeed.isNotEmpty()) {
            viewModelScope.launch {
                delay(startupPlan.secondaryStartDelayMs)
                awaitHomeUiIdle(startupPlan)
                LevyraArtworkCache.preloadPriority(appContext, orbitSeed, LevyraPersonalOrbit.DISPLAY_LIMIT)
                warmPersistentOrbit(orbitSeed, LevyraPersonalOrbit.DISPLAY_LIMIT, persist = false)
                refreshMissingOfficialOrbitArtwork(orbitSeed, deferUntilHomeIdle = true)
            }
        }

        viewModelScope.launch {
            delay(700L)
            loadHomeFeed(deferUntilHomeIdle = true)
        }
        viewModelScope.launch {
            delay(1_600L)
            loadCharts(deferUntilHomeIdle = true)
        }
        viewModelScope.launch {
            delay(3_200L)
            awaitHomeUiIdle(startupPlan)
            checkForUpdates(silent = true)
            loadReleaseRadar(deferUntilHomeIdle = true)
        }
    }

    private fun warmPersistentOrbit(tracks: List<Track>, limit: Int, persist: Boolean = false) {
        val limited = tracks.take(limit.coerceAtLeast(1))
        val appContext = getApplication<Application>().applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            val languageCode = _state.value.languageCode
            if (persist) preferences.savePersonalOrbitTracks(limited, languageCode)
            if (limited.isNotEmpty()) LevyraArtworkCache.cachePersistent(appContext, limited, limited.size)
        }
    }

    private fun persistHomeSnapshot() {
        val languageCode = _state.value.languageCode
        homeSnapshotJob?.cancel()
        homeSnapshotJob = viewModelScope.launch(Dispatchers.IO) {
            delay(280L)
            if (_state.value.languageCode == languageCode) persistHomeSnapshotSync(languageCode)
        }
    }

    private fun persistHomeSnapshotSync(languageCode: String) {
        val snapshot = _state.value
        homeSnapshotCache.save(
            languageCode = languageCode,
            homeSections = deferredHomeSectionsSnapshot ?: snapshot.homeSections,
            charts = snapshot.charts,
            personalOrbit = snapshot.personalOrbitTracks,
            homeArtists = deferredHomeArtistsSnapshot ?: snapshot.homeArtists
        )
    }

    fun refreshHomeArtists() {
        val startupSnapshot = _state.value
        homeArtistsJob?.cancel()
        homeArtistsJob = viewModelScope.launch(Dispatchers.IO) {
            val rankedHistory = listeningPulseStore.personalizedArtists(limit = HOME_ARTIST_HISTORY_LIMIT)
            val candidates = LinkedHashMap<String, HomeArtistCandidate>()

            fun addCandidate(nameValue: String, browseIdValue: String) {
                val browseId = browseIdValue.trim()
                val name = primaryArtistSegment(nameValue).ifBlank { nameValue.trim() }
                if (browseId.isBlank() || name.length < 2 || !isArtistShelfNameEligible(name)) return
                candidates.putIfAbsent(browseId.lowercase(), HomeArtistCandidate(name, browseId))
            }

            rankedHistory.forEach { ranked -> addCandidate(ranked.name, ranked.browseId) }

            buildList {
                addAll(startupSnapshot.recentListens)
                addAll(startupSnapshot.personalOrbitTracks)
                addAll(startupSnapshot.favorites)
                startupSnapshot.currentTrack?.let(::add)
                addAll(startupSnapshot.tracks)
                addAll(startupSnapshot.homeSections.flatMap { it.tracks })
                addAll(startupSnapshot.charts)
            }.forEach { track ->
                addCandidate(track.artist, track.artistBrowseIds.firstOrNull().orEmpty())
            }

            startupSnapshot.homeArtists.forEach { artist -> addCandidate(artist.name, artist.browseId) }

            val orderedCandidates = candidates.values.take(HOME_ARTIST_CANDIDATE_LIMIT)
            val fingerprint = buildString {
                append(startupSnapshot.languageCode)
                append('|')
                append(orderedCandidates.joinToString("|") { candidate ->
                    "${candidate.browseId.lowercase()}:${artistIdentityKey(candidate.name)}"
                })
            }
            val visibleArtists = startupSnapshot.homeArtists
                .filter { hit ->
                    hit.name.isNotBlank() &&
                        hit.thumbnailUrl.isNotBlank() &&
                        hit.browseId.isNotBlank() &&
                        isArtistShelfNameEligible(hit.name)
                }
                .distinctBy { it.browseId.lowercase() }
                .take(HOME_ARTIST_SHELF_SIZE)

            if (fingerprint == homeArtistsFingerprint && visibleArtists.size >= HOME_ARTIST_SHELF_SIZE) return@launch
            homeArtistsFingerprint = fingerprint

            if (orderedCandidates.isEmpty()) {
                _state.update { current ->
                    current.copy(
                        homeArtists = visibleArtists,
                        homeArtistsLoading = visibleArtists.isEmpty()
                    )
                }
                return@launch
            }

            val freezeVisibleShelf = visibleArtists.size >= HOME_ARTIST_SHELF_SIZE
            if (freezeVisibleShelf) {
                awaitHomeUiIdle()
            } else {
                _state.update { current ->
                    current.copy(
                        homeArtists = visibleArtists,
                        homeArtistsLoading = visibleArtists.isEmpty()
                    )
                }
            }

            val visibleByBrowseId = visibleArtists.associateBy { it.browseId.lowercase() }
            val resolved = LinkedHashMap<String, ArtistHit>()
            val semaphore = Semaphore(HOME_ARTIST_RESOLUTION_CONCURRENCY)

            for (batch in orderedCandidates.chunked(HOME_ARTIST_RESOLUTION_CONCURRENCY)) {
                val hits = coroutineScope {
                    batch.map { candidate ->
                        async {
                            val cached = visibleByBrowseId[candidate.browseId.lowercase()]
                                ?.takeIf { hit ->
                                    hit.thumbnailUrl.isNotBlank() &&
                                        hit.browseId.equals(candidate.browseId, ignoreCase = true) &&
                                        artistIdentityKey(hit.name) == artistIdentityKey(candidate.name) &&
                                        isArtistShelfNameEligible(hit.name)
                                }
                            cached ?: semaphore.withPermit {
                                withTimeoutOrNull(HOME_ARTIST_FAST_TIMEOUT_MS) {
                                    runCatching {
                                        artistRepository.artistHit(candidate.browseId, candidate.name)
                                    }.getOrNull()
                                }
                            }
                        }
                    }.awaitAll()
                }
                hits.filterNotNull().forEach { hit ->
                    if (
                        hit.name.isNotBlank() &&
                        hit.thumbnailUrl.isNotBlank() &&
                        hit.browseId.isNotBlank() &&
                        isArtistShelfNameEligible(hit.name)
                    ) {
                        resolved.putIfAbsent(hit.browseId.lowercase(), hit)
                    }
                }
                if (resolved.size >= HOME_ARTIST_SHELF_SIZE) break
            }

            if (resolved.size < HOME_ARTIST_SHELF_SIZE) {
                val seedArtists = (resolved.values + visibleArtists)
                    .distinctBy { it.browseId.lowercase() }
                    .take(HOME_ARTIST_RELATED_SEED_LIMIT)
                val related = coroutineScope {
                    seedArtists.map { seed ->
                        async {
                            withTimeoutOrNull(HOME_ARTIST_RELATED_TIMEOUT_MS) {
                                runCatching {
                                    artistRepository.relatedArtistHits(
                                        browseId = seed.browseId,
                                        fallbackName = seed.name,
                                        limit = HOME_ARTIST_SHELF_SIZE
                                    )
                                }.getOrDefault(emptyList())
                            }.orEmpty()
                        }
                    }.awaitAll().flatten()
                }
                related.forEach { hit ->
                    if (
                        resolved.size < HOME_ARTIST_SHELF_SIZE &&
                        hit.name.isNotBlank() &&
                        hit.thumbnailUrl.isNotBlank() &&
                        hit.browseId.isNotBlank() &&
                        isArtistShelfNameEligible(hit.name)
                    ) {
                        resolved.putIfAbsent(hit.browseId.lowercase(), hit)
                    }
                }
            }

            visibleArtists.forEach { hit ->
                if (resolved.size < HOME_ARTIST_SHELF_SIZE) {
                    resolved.putIfAbsent(hit.browseId.lowercase(), hit)
                }
            }

            if (!isActive || homeArtistsFingerprint != fingerprint) return@launch

            val finalArtists = resolved.values.take(HOME_ARTIST_SHELF_SIZE)
            val complete = finalArtists.size >= HOME_ARTIST_SHELF_SIZE

            if (freezeVisibleShelf) {
                if (complete) {
                    deferredHomeArtistsSnapshot = finalArtists
                    persistHomeSnapshotSync(startupSnapshot.languageCode)
                }
                _state.update { current -> current.copy(homeArtistsLoading = false) }
                return@launch
            }

            deferredHomeArtistsSnapshot = null
            _state.update { current ->
                current.copy(
                    homeArtists = finalArtists,
                    homeArtistsLoading = finalArtists.isEmpty()
                )
            }
            persistHomeSnapshotSync(startupSnapshot.languageCode)
        }
    }

    private fun loadReleaseRadar(deferUntilHomeIdle: Boolean = false) {
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
            val startupPlan = homeStartupWorkPlan()
            val artistLimit = if (deferUntilHomeIdle) startupPlan.releaseRadarArtistCount else 8
            val releaseLimit = if (deferUntilHomeIdle) startupPlan.releasesPerArtist else 8
            followed.take(artistLimit).forEach { artist ->
                if (deferUntilHomeIdle) awaitHomeUiIdle(startupPlan)
                val profile = runCatching { artistRepository.profile(artist.browseId, artist.name) }.getOrNull() ?: return@forEach
                if (!isActive) return@launch
                (profile.albums + profile.singles).take(releaseLimit).forEach { release ->
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
            }
            if (deferUntilHomeIdle) awaitHomeUiIdle()
            val sorted = entries
                .distinctBy { it.release.browseId.ifBlank { "${it.artistName}|${it.release.title}" } }
                .sortedByDescending { it.release.year.toIntOrNull() ?: 0 }
                .take(20)
            val similarArtists = similar.values.take(12).toList()
            _state.update { current ->
                val nextRadar = sorted.ifEmpty { current.releaseRadar }
                val nextSimilarArtists = similarArtists.ifEmpty { current.similarArtists }
                if (current.releaseRadar == nextRadar && current.similarArtists == nextSimilarArtists) current
                else current.copy(releaseRadar = nextRadar, similarArtists = nextSimilarArtists)
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
        listenSessionCompleted = true
        recordSmartCompletion(current)
        val nextTrack = queueEngine.next()
        when {
            nextTrack != null -> startResolve(nextTrack)
            queueEngine.state.value.radioEnabled -> ensureRadioTail(force = true, playWhenReady = true)
            loopCurrentQueueOnCompletion && queueEngine.state.value.tracks.isNotEmpty() -> {
                val first = queueEngine.select(0, rememberCurrent = true)
                if (first != null) startResolve(first)
            }
            else -> {
                player.pause()
                queueEngine.updatePosition(0L)
                _state.update { it.copy(isPlaying = false, positionMs = 0L) }
            }
        }
    }

    fun toggleRepeat() {
        val mode = when (queueEngine.state.value.repeatMode) {
            RepeatMode.Off -> RepeatMode.All
            RepeatMode.All -> RepeatMode.One
            RepeatMode.One -> RepeatMode.Off
        }
        queueEngine.setRepeatMode(mode)
        player.setRepeatOne(mode == RepeatMode.One)
    }

    fun toggleVideoMode() {
        val snapshot = _state.value
        val track = snapshot.currentTrack ?: return
        if (track.videoUrl.isBlank() || snapshot.isResolving) return
        val sourceMode = snapshot.isVideoMode
        val targetMode = !sourceMode
        val positionMs = player.positionMs.coerceAtLeast(0L)
        val shouldPlay = player.isPlaying || snapshot.isPlaying
        val transitionId = ++streamTransitionId
        streamRecoveryJob?.cancel()
        modeSwitchJob?.cancel()
        _state.update { it.copy(isResolving = true, playerError = null) }
        modeSwitchJob = viewModelScope.launch {
            try {
                val baseTrack = (youtubePlayableTrack(track) ?: track).copy(streamUrl = "", videoStreamUrl = "")
                val resolved = withContext(Dispatchers.IO) {
                    resolver.cached(baseTrack, targetMode) ?: resolver.resolve(baseTrack, targetMode)
                }
                if (!isActive || transitionId != streamTransitionId) return@launch
                val currentIndex = queueEngine.state.value.currentIndex
                if (currentIndex >= 0) queueEngine.updateTrackAt(currentIndex, resolved)
                repository.replace(resolved)
                player.replaceSource(
                    track = resolved,
                    positionMs = positionMs,
                    videoMode = targetMode,
                    playWhenReady = shouldPlay
                )
                queueEngine.updatePosition(positionMs)
                _state.update {
                    it.copy(
                        currentTrack = resolved,
                        isVideoMode = targetMode,
                        isResolving = false,
                        isPlaying = shouldPlay,
                        positionMs = positionMs,
                        durationMs = resolved.durationMs.takeIf { duration -> duration > 0L } ?: it.durationMs,
                        playerError = null
                    )
                }
                prefetchAlternateMode(resolved, targetMode)
                updateWidget()
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                if (transitionId != streamTransitionId) return@launch
                _state.update {
                    it.copy(
                        isVideoMode = sourceMode,
                        isResolving = false,
                        isPlaying = player.isPlaying || snapshot.isPlaying,
                        playerError = cleanUserError(error)
                    )
                }
            }
        }
    }

    private fun recoverPlaybackStream(
        failedTrack: Track,
        positionMs: Long,
        videoMode: Boolean,
        playWhenReady: Boolean,
        errorMessage: String
    ) {
        val transitionId = ++streamTransitionId
        modeSwitchJob?.cancel()
        streamRecoveryJob?.cancel()
        resolver.reportPlaybackFailure(failedTrack, videoMode, errorMessage)
        _state.update { it.copy(isResolving = true, isPlaying = playWhenReady, playerError = null) }
        streamRecoveryJob = viewModelScope.launch {
            try {
                val baseTrack = (youtubePlayableTrack(failedTrack) ?: failedTrack).copy(streamUrl = "", videoStreamUrl = "")
                val resolved = withContext(Dispatchers.IO) { resolver.resolve(baseTrack, videoMode) }
                if (!isActive || transitionId != streamTransitionId) return@launch
                val currentIndex = queueEngine.state.value.currentIndex
                if (currentIndex >= 0) queueEngine.updateTrackAt(currentIndex, resolved)
                repository.replace(resolved)
                player.replaceSource(
                    track = resolved,
                    positionMs = positionMs,
                    videoMode = videoMode,
                    playWhenReady = playWhenReady
                )
                queueEngine.updatePosition(positionMs)
                _state.update {
                    it.copy(
                        currentTrack = resolved,
                        isVideoMode = videoMode,
                        isResolving = false,
                        isPlaying = playWhenReady,
                        positionMs = positionMs,
                        durationMs = resolved.durationMs.takeIf { duration -> duration > 0L } ?: it.durationMs,
                        playerError = null
                    )
                }
                prefetchAlternateMode(resolved, videoMode)
                updateWidget()
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                if (transitionId != streamTransitionId) return@launch
                val message = cleanUserError(error)
                player.failRecovery(message)
                _state.update { it.copy(isResolving = false, isPlaying = false, playerError = message) }
            }
        }
    }

    private fun prefetchAlternateMode(track: Track, activeVideoMode: Boolean) {
        alternateModePrefetchJob?.cancel()
        if (track.id.isBlank() || track.videoUrl.isBlank() || track.source.equals("Offline", true)) return
        alternateModePrefetchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(350L)
            val cleanTrack = (youtubePlayableTrack(track) ?: track).copy(streamUrl = "", videoStreamUrl = "")
            resolver.prefetch(cleanTrack, !activeVideoMode)
        }
    }

    fun toggleAudioNormalization() {
        setReplayGainEnabled(!_state.value.audioNormalization)
    }

    fun toggleShuffle() {
        queueEngine.setShuffle(!queueEngine.state.value.shuffleEnabled)
        refreshQueuePrefetch()
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

    fun setTemporaryPlaybackSpeed(value: Float) {
        val speed = value.coerceIn(0.25f, 3f)
        val pitch = _state.value.audioSettings.pitch
        player.setPlayback(speed, pitch)
        _state.update { it.copy(playbackSpeed = speed) }
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

    private fun loadHomeFeed(deferUntilHomeIdle: Boolean = false) {
        val requestGeneration = homeFeedRequestGeneration.incrementAndGet()
        homeFeedJob?.cancel()
        homeFeedJob = viewModelScope.launch {
            val initialState = _state.value
            val languageCode = initialState.languageCode
            val hasVisibleHome = initialState.homeSections.isNotEmpty() || initialState.tracks.isNotEmpty()
            _state.update { current ->
                if (current.languageCode != languageCode) current
                else current.copy(
                    isSearching = !hasVisibleHome,
                    searchError = if (hasVisibleHome) current.searchError else null
                )
            }

            val networkSections = try {
                repository.homeFeed(languageCode)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "Home feed refresh failed")
                emptyList()
            }

            if (
                !isActive ||
                homeFeedRequestGeneration.get() != requestGeneration ||
                _state.value.languageCode != languageCode
            ) return@launch

            val sanitizedSections = withContext(Dispatchers.Default) {
                HomeRefreshStability.sanitizeSections(networkSections)
            }
            if (sanitizedSections.isEmpty()) {
                loadHomeAlbums(languageCode, deferUntilHomeIdle)
                if (hasVisibleHome) {
                    _state.update { current ->
                        if (current.languageCode == languageCode && current.isSearching) {
                            current.copy(isSearching = false)
                        } else {
                            current
                        }
                    }
                } else {
                    loadFallbackHome(
                        languageCode = languageCode,
                        requestGeneration = requestGeneration,
                        deferUntilHomeIdle = deferUntilHomeIdle
                    )
                }
                return@launch
            }

            val previewMerge = withContext(Dispatchers.Default) {
                HomeRefreshStability.mergeSections(
                    previous = _state.value.homeSections,
                    incoming = sanitizedSections,
                    allowStructuralChanges = !homeScreenActive
                )
            }
            withContext(Dispatchers.IO) {
                preferences.saveHomeSections(sanitizedSections, languageCode)
            }
            if (previewMerge.changed && (deferUntilHomeIdle || (homeScreenActive && !homeAtTop))) {
                awaitHomeUiIdle()
            }
            if (
                !isActive ||
                homeFeedRequestGeneration.get() != requestGeneration ||
                _state.value.languageCode != languageCode
            ) return@launch

            var mergeBaseSections = _state.value.homeSections
            var mergeResult = withContext(Dispatchers.Default) {
                HomeRefreshStability.mergeSections(
                    previous = mergeBaseSections,
                    incoming = sanitizedSections,
                    allowStructuralChanges = !homeScreenActive
                )
            }
            if (_state.value.homeSections != mergeBaseSections) {
                mergeBaseSections = _state.value.homeSections
                mergeResult = withContext(Dispatchers.Default) {
                    HomeRefreshStability.mergeSections(
                        previous = mergeBaseSections,
                        incoming = sanitizedSections,
                        allowStructuralChanges = !homeScreenActive
                    )
                }
            }
            val visibleTracks = withContext(Dispatchers.Default) {
                mergeResult.visible.flatMap { it.tracks }.distinctBy { it.id }
            }
            if (visibleTracks.isEmpty()) {
                _state.update { current ->
                    if (current.languageCode == languageCode) current.copy(isSearching = false) else current
                }
                return@launch
            }

            val latestState = _state.value
            val instantAlbums = latestState.homeAlbums.ifEmpty {
                withContext(Dispatchers.Default) {
                    instantAlbumRecommendationsFromTracks(
                        primary = latestState.recentListens + latestState.recentSearches + latestState.favorites,
                        secondary = latestState.personalOrbitTracks + visibleTracks,
                        limit = HOME_ALBUM_RECOMMENDATION_LIMIT,
                        profile = latestState.smartProfile
                    )
                }
            }
            var published = false
            while (!published) {
                val current = _state.value
                if (
                    current.languageCode != languageCode ||
                    current.homeSections != mergeBaseSections
                ) break
                val nextSearchResults = visibleTracks.take(12)
                val updated = if (
                    !mergeResult.changed &&
                    current.homeAlbums == instantAlbums &&
                    current.tracks == visibleTracks &&
                    current.searchResults == nextSearchResults &&
                    !current.isSearching
                ) {
                    current
                } else {
                    current.copy(
                        homeSections = if (mergeResult.changed) mergeResult.visible else current.homeSections,
                        homeAlbums = instantAlbums,
                        homeAlbumsLoading = instantAlbums.isEmpty(),
                        tracks = visibleTracks,
                        searchResults = nextSearchResults,
                        isSearching = false,
                        cacheReport = repository.cacheReport(),
                        searchError = null
                    )
                }
                published = updated === current || _state.compareAndSet(current, updated)
            }
            if (!published) {
                _state.update { current ->
                    if (current.languageCode == languageCode && current.isSearching) current.copy(isSearching = false) else current
                }
                return@launch
            }
            pendingHomeSectionsSnapshot.set(mergeResult.deferredStructural)
            deferredHomeSectionsSnapshot = mergeResult.deferredStructural

            persistHomeSnapshot()
            val startupPlan = homeStartupWorkPlan()
            viewModelScope.launch(Dispatchers.IO) {
                if (deferUntilHomeIdle) awaitHomeUiIdle(startupPlan)
                LevyraArtworkCache.preloadHome(
                    getApplication<Application>().applicationContext,
                    visibleTracks,
                    startupPlan.refreshedArtworkCount
                )
            }
            prefetchTop(visibleTracks, startupPlan.refreshedArtworkCount, respectHomeScroll = deferUntilHomeIdle)
            refreshOfficialMetadataBatch(visibleTracks, 8, deferUntilHomeIdle)
            loadHomeAlbums(languageCode, deferUntilHomeIdle)
        }
    }

    private fun loadHomeAlbums(languageCode: String, deferUntilHomeIdle: Boolean = false) {
        val requestGeneration = homeAlbumsRequestGeneration.incrementAndGet()
        homeAlbumsJob?.cancel()
        homeAlbumsJob = viewModelScope.launch {
            val initialState = _state.value
            if (initialState.languageCode != languageCode) return@launch

            if (initialState.homeAlbums.isEmpty()) {
                val instantAlbums = instantAlbumRecommendations(initialState)
                    .take(HOME_ALBUM_RECOMMENDATION_LIMIT)
                _state.update { current ->
                    if (current.languageCode != languageCode || current.homeAlbums.isNotEmpty()) current
                    else current.copy(
                        homeAlbums = instantAlbums,
                        homeAlbumsLoading = instantAlbums.isEmpty()
                    )
                }
            } else if (initialState.homeAlbumsLoading) {
                _state.update { current ->
                    if (current.languageCode == languageCode) current.copy(homeAlbumsLoading = false) else current
                }
            }

            val seeds = albumRecommendationSeeds(_state.value)
            val albums = try {
                repository.homeAlbums(
                    languageCode = languageCode,
                    limit = HOME_ALBUM_REMOTE_CANDIDATE_LIMIT,
                    seeds = seeds
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "Personalized home albums failed")
                emptyList()
            }
            if (
                !isActive ||
                homeAlbumsRequestGeneration.get() != requestGeneration ||
                _state.value.languageCode != languageCode
            ) return@launch
            if (albums.isEmpty()) {
                _state.update { current ->
                    if (current.languageCode == languageCode && current.homeAlbumsLoading) {
                        current.copy(homeAlbumsLoading = false)
                    } else {
                        current
                    }
                }
                return@launch
            }

            val latestState = _state.value
            val latestInstantAlbums = instantAlbumRecommendations(latestState)
            val rankedAlbums = withContext(Dispatchers.Default) {
                rankAlbumRecommendations(
                    remote = albums,
                    instant = latestInstantAlbums,
                    state = latestState,
                    limit = HOME_ALBUM_RECOMMENDATION_LIMIT
                ).ifEmpty { latestInstantAlbums.take(HOME_ALBUM_RECOMMENDATION_LIMIT) }
            }
            if (rankedAlbums.isEmpty()) {
                _state.update { current ->
                    if (current.languageCode == languageCode && current.homeAlbumsLoading) {
                        current.copy(homeAlbumsLoading = false)
                    } else {
                        current
                    }
                }
                return@launch
            }

            withContext(Dispatchers.IO) {
                preferences.saveHomeAlbums(rankedAlbums, languageCode)
            }
            if (deferUntilHomeIdle && _state.value.homeAlbums != rankedAlbums) awaitHomeUiIdle()
            if (
                !isActive ||
                homeAlbumsRequestGeneration.get() != requestGeneration ||
                _state.value.languageCode != languageCode
            ) return@launch

            _state.update { current ->
                if (current.languageCode != languageCode) current
                else if (current.homeAlbums == rankedAlbums && !current.homeAlbumsLoading) current
                else current.copy(homeAlbums = rankedAlbums, homeAlbumsLoading = false)
            }
        }
    }

    private fun albumRecommendationSeeds(state: LevyraUiState): List<AlbumRecommendationSeed> {
        val seeds = LinkedHashMap<String, AlbumRecommendationSeed>()
        fun put(seed: AlbumRecommendationSeed) {
            val cleanQuery = seed.query.trim()
            if (cleanQuery.length < 2) return
            val normalized = seed.copy(
                query = cleanQuery,
                artist = seed.artist.trim(),
                album = seed.album.trim(),
                moodTags = seed.moodTags.map { it.trim() }.filter { it.length >= 2 }.toSet(),
                weight = seed.weight.coerceIn(0, 2_000)
            )
            val key = listOf(
                albumRecommendationTextKey(normalized.artist),
                albumRecommendationTextKey(normalized.album),
                normalized.moodTags.map(::albumRecommendationTextKey).sorted().joinToString("|")
            ).joinToString("|")
            if (key.isBlank() || key == "||") return
            val existing = seeds[key]
            if (existing == null || normalized.weight > existing.weight) seeds[key] = normalized
        }
        fun putTrack(track: Track, baseWeight: Int) {
            val artist = track.artist.trim()
            if (!isUsefulRecommendationArtist(artist)) return
            val album = track.album.trim()
            if (isUsefulRecommendationAlbum(album, track.title)) {
                put(
                    AlbumRecommendationSeed(
                        query = "$album $artist album",
                        artist = artist,
                        album = album,
                        weight = baseWeight + 60
                    )
                )
            }
            put(
                AlbumRecommendationSeed(
                    query = "$artist album",
                    artist = artist,
                    weight = baseWeight
                )
            )
        }

        state.smartProfile.topAlbums.take(6).forEachIndexed { index, profileSeed ->
            val parts = profileSeed.label.split(" • ", limit = 2)
            val album = parts.getOrNull(0).orEmpty().trim()
            val artist = parts.getOrNull(1).orEmpty().trim()
            if (isUsefulRecommendationAlbum(album, "") && isUsefulRecommendationArtist(artist)) {
                put(
                    AlbumRecommendationSeed(
                        query = profileSeed.query.ifBlank { "$album $artist album" },
                        artist = artist,
                        album = album,
                        weight = 520 + profileSeed.weight.coerceIn(0, 1_000) / 4 - index * 8
                    )
                )
            }
        }
        state.smartProfile.topArtists.take(8).forEachIndexed { index, profileSeed ->
            val artist = profileSeed.label.trim()
            if (isUsefulRecommendationArtist(artist)) {
                put(
                    AlbumRecommendationSeed(
                        query = profileSeed.query.ifBlank { "$artist album" },
                        artist = artist,
                        weight = 440 + profileSeed.weight.coerceIn(0, 1_000) / 5 - index * 8
                    )
                )
            }
        }
        state.currentTrack?.let { putTrack(it, 420) }
        state.recentListens.take(12).forEachIndexed { index, track -> putTrack(track, 390 - index * 9) }
        state.favorites.take(12).forEachIndexed { index, track -> putTrack(track, 340 - index * 7) }
        state.recentSearches.take(8).forEachIndexed { index, track -> putTrack(track, 210 - index * 6) }

        var directSeeds = seeds.values.count { it.artist.isNotBlank() || it.album.isNotBlank() }
        if (directSeeds in 1..5) {
            state.personalOrbitTracks.take(8).forEachIndexed { index, track -> putTrack(track, 170 - index * 6) }
            directSeeds = seeds.values.count { it.artist.isNotBlank() || it.album.isNotBlank() }
        }
        if (directSeeds < 4) {
            state.smartProfile.topMoods.take(2).forEachIndexed { index, profileSeed ->
                val mood = profileSeed.label.trim()
                if (mood.length >= 2) {
                    put(
                        AlbumRecommendationSeed(
                            query = "$mood album",
                            moodTags = setOf(mood),
                            weight = 120 + profileSeed.weight.coerceIn(0, 600) / 8 - index * 8
                        )
                    )
                }
            }
        }
        val values = seeds.values.toList()
        val albumSeeds = values.filter { it.album.isNotBlank() }.sortedByDescending { it.weight }
        val artistSeeds = values.filter { it.album.isBlank() && it.artist.isNotBlank() }.sortedByDescending { it.weight }
        val moodSeeds = values.filter { it.artist.isBlank() && it.album.isBlank() }.sortedByDescending { it.weight }
        val selected = LinkedHashSet<AlbumRecommendationSeed>()
        selected.addAll(albumSeeds.take(5))
        selected.addAll(artistSeeds.take(6))
        selected.addAll(moodSeeds.take(1))
        values.sortedByDescending { it.weight }.forEach { seed ->
            if (selected.size < HOME_ALBUM_SEED_LIMIT) selected += seed
        }
        return selected.take(HOME_ALBUM_SEED_LIMIT)
    }

    private fun rankAlbumRecommendations(
        remote: List<AlbumHit>,
        instant: List<AlbumHit>,
        state: LevyraUiState,
        limit: Int
    ): List<AlbumHit> {
        val candidates = mergeAlbums(instant, remote)
        if (candidates.isEmpty()) return emptyList()
        val allSeeds = albumRecommendationSeeds(state)
        val directSeeds = allSeeds.filter { it.artist.isNotBlank() || it.album.isNotBlank() }
        val scoringSeeds = directSeeds.ifEmpty { allSeeds }
        if (scoringSeeds.isEmpty()) return candidates.take(limit)

        val ranked: List<Pair<AlbumHit, Int>> = candidates.mapNotNull { album: AlbumHit ->
            val score: Int = scoringSeeds.maxOfOrNull { seed: AlbumRecommendationSeed ->
                val match = levyraAlbumRecommendationMatchScore(album, seed)
                if (match == LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE) LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE
                else seed.weight + match
            } ?: LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE
            if (score == LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE) null else Pair(album, score)
        }.sortedWith(
            compareByDescending<Pair<AlbumHit, Int>> { scored -> scored.second }
                .thenByDescending { scored -> scored.first.metadataConfidence }
                .thenBy { scored -> albumRecommendationTextKey(scored.first.artist) }
                .thenBy { scored -> albumRecommendationTextKey(scored.first.title) }
        )
        if (ranked.isEmpty()) return emptyList()

        val distinctSeedArtists = directSeeds
            .map { albumRecommendationTextKey(it.artist) }
            .filter { it.isNotBlank() }
            .distinct()
            .size
        val maxPerArtist = if (distinctSeedArtists <= 2) 4 else 2
        val artistCounts = HashMap<String, Int>()
        val selected = ArrayList<AlbumHit>(limit)
        ranked.forEach { scored: Pair<AlbumHit, Int> ->
            if (selected.size >= limit) return@forEach
            val album: AlbumHit = scored.first
            val artistKey = albumRecommendationTextKey(album.artist)
            val count = artistCounts[artistKey] ?: 0
            if (count < maxPerArtist) {
                selected.add(album)
                artistCounts[artistKey] = count + 1
            }
        }
        if (selected.size < limit) {
            ranked.asSequence()
                .map { scored: Pair<AlbumHit, Int> -> scored.first }
                .filterNot { candidate: AlbumHit ->
                    selected.any { selectedAlbum: AlbumHit ->
                        albumRecommendationIdentityKey(selectedAlbum) == albumRecommendationIdentityKey(candidate)
                    }
                }
                .take(limit - selected.size)
                .forEach { candidate: AlbumHit -> selected.add(candidate) }
        }
        return selected
    }

    private fun isUsefulRecommendationArtist(value: String): Boolean {
        val key = albumRecommendationTextKey(value)
        if (key.length < 2) return false
        return key !in setOf("youtube", "youtube music", "various artists", "artisti vari", "unknown artist")
    }

    private fun isUsefulRecommendationAlbum(album: String, trackTitle: String): Boolean {
        val key = albumRecommendationTextKey(album)
        if (key.length < 2) return false
        if (key == albumRecommendationTextKey(trackTitle) && trackTitle.isNotBlank()) return false
        if (key in setOf("youtube", "youtube music", "single", "singolo", "ep")) return false
        return !key.endsWith(" single") && !key.endsWith(" singolo") && !key.endsWith(" ep")
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

    fun setInterfaceSettings(value: LevyraInterfaceSettings) {
        val normalized = value.normalized()
        preferences.setInterfaceSettings(normalized)
        _state.update { it.copy(interfaceSettings = normalized) }
    }

    fun setDownloadSettings(value: LevyraDownloadSettings) {
        val normalized = value.normalized()
        preferences.setDownloadSettings(normalized)
        _state.update { it.copy(downloadSettings = normalized) }
    }

    fun pauseDownload(taskKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            OfflineExportWorker.pause(getApplication<Application>().applicationContext, taskKey)
        }
    }

    fun resumeDownload(taskKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            OfflineExportWorker.resume(getApplication<Application>().applicationContext, taskKey)
        }
    }

    fun cancelDownload(taskKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            OfflineExportWorker.cancel(getApplication<Application>().applicationContext, taskKey)
        }
    }

    fun createBackup(uri: Uri) {
        viewModelScope.launch {
            val message = runCatching { backupManager.exportTo(uri).message }
                .getOrElse { "Backup non riuscito: ${cleanUserError(it)}" }
            _state.update { it.copy(backupMessage = message) }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            val message = runCatching {
                backupManager.restoreFrom(uri)
                refreshAfterRestore()
                "Ripristino completato"
            }.getOrElse { "Ripristino non riuscito: ${cleanUserError(it)}" }
            _state.update { it.copy(backupMessage = message) }
        }
    }

    fun clearBackupMessage() {
        _state.update { it.copy(backupMessage = null) }
    }

    fun refreshPlaybackDiagnostics(): String {
        val diagnostics = resolver.playbackDiagnostics()
        _state.update { it.copy(playbackDiagnostics = diagnostics) }
        return diagnostics
    }

    private suspend fun refreshAfterRestore() {
        val snapshot = withContext(Dispatchers.IO) { preferences.snapshot() }
        val favorites = withContext(Dispatchers.IO) { favoritesStore.load() }
        val playlists = playlistStore.loadAll()
        val followed = followedArtistsStore.load()
        applyFollowedArtists(followed)
        pendingSeekMs = snapshot.lastPositionMs.coerceAtLeast(0L)
        val restoredTrack = snapshot.lastTrack?.copy(streamUrl = "", videoStreamUrl = "")
        _state.update {
            it.copy(
                favorites = favorites,
                favoriteIds = favorites.map { track -> track.id }.toSet(),
                playlists = playlists,
                recentSearches = snapshot.recentSearches,
                personalOrbitTracks = snapshot.personalOrbitTracks,
                userName = snapshot.userName,
                languageCode = snapshot.languageCode,
                animationsEnabled = snapshot.animationsEnabled && !adaptivePlaybackPolicy.current(videoMode = false).lowRam,
                dynamicColor = snapshot.dynamicColor,
                sponsorBlockEnabled = snapshot.sponsorBlock,
                skipSilence = snapshot.skipSilence,
                audioQuality = snapshot.audioQuality,
                audioNormalization = snapshot.audioNormalization,
                audioSettings = snapshot.audioSettings,
                playbackSpeed = snapshot.audioSettings.playbackSpeed,
                lyricsTranslationEnabled = snapshot.lyricsTranslationEnabled,
                themePreset = snapshot.themePreset,
                interfaceSettings = snapshot.interfaceSettings,
                downloadSettings = snapshot.downloadSettings,
                showOnboarding = !snapshot.onboarded,
                currentTrack = restoredTrack ?: it.currentTrack,
                positionMs = if (restoredTrack != null) pendingSeekMs else it.positionMs,
                durationMs = restoredTrack?.durationMs ?: it.durationMs,
                playbackDiagnostics = resolver.playbackDiagnostics()
            )
        }
        applyLanguageContent(snapshot.languageCode, refreshRemote = true)
        player.setSkipSilence(snapshot.skipSilence)
        player.setPremiumAudioSettings(snapshot.audioSettings)
        player.setPlayback(snapshot.audioSettings.playbackSpeed, snapshot.audioSettings.pitch)
        com.luc4n3x.levyra.player.PlaybackService.normalizationProcessor.enabled = snapshot.audioNormalization || snapshot.audioSettings.replayGainEnabled
        resolver.setAudioQuality(snapshot.audioQuality)
        withContext(Dispatchers.IO) {
            queueEngine.restore(
                fallbackTracks = emptyList(),
                fallbackIndex = -1,
                fallbackPositionMs = 0L
            )
        }
    }

    fun setLanguage(code: String) {
        val normalizedLanguage = LevyraLanguageCatalog.normalize(code)
        if (normalizedLanguage == _state.value.languageCode) return
        preferences.setLanguageCode(normalizedLanguage)
        applyLanguageContent(normalizedLanguage, refreshRemote = true)
        _state.value.currentTrack?.let { track ->
            fetchLyrics(track)
            prefetchLyricsAround(track)
        }
    }

    private fun applyLanguageContent(languageCode: String, refreshRemote: Boolean) {
        pendingHomeSectionsSnapshot.set(null)
        deferredHomeSectionsSnapshot = null
        deferredHomeArtistsSnapshot = null
        homeArtistsFingerprint = ""
        homeArtistsJob?.cancel()
        val defaultChartRegion = ChartsCatalog.defaultRegionForLanguage(languageCode)
        val localizedMoods = moodEngine.moodsForLanguage(languageCode)
        val selectedMood = localizedMoods.firstOrNull { it.id == _state.value.selectedMood?.id } ?: localizedMoods.firstOrNull()
        val homeSections = LevyraStartupCatalog.repairHomeSections(
            preferences.loadHomeSections(languageCode).ifEmpty { LevyraStartupCatalog.homeSections(languageCode) },
            languageCode
        )
        val homeTracks = homeSections.flatMap { it.tracks }.distinctBy { it.id }
        val chartTracks = LevyraStartupCatalog.repairTracks(
            preferences.loadChartTracks(languageCode, defaultChartRegion.id).ifEmpty { LevyraStartupCatalog.chartTracks(languageCode) },
            languageCode
        )
        val cachedOrbit = LevyraStartupCatalog.repairTracks(preferences.loadPersonalOrbitTracks(languageCode), languageCode)
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
        val recommendationState = _state.value.copy(
            languageCode = languageCode,
            selectedMood = selectedMood,
            homeSections = homeSections,
            charts = chartTracks,
            personalOrbitTracks = orbit,
            tracks = allTracks
        )
        val localAlbums = instantAlbumRecommendations(recommendationState, HOME_ALBUM_RECOMMENDATION_LIMIT)
        val instantAlbums = rankAlbumRecommendations(
            remote = preferences.loadHomeAlbums(languageCode),
            instant = localAlbums,
            state = recommendationState,
            limit = HOME_ALBUM_RECOMMENDATION_LIMIT
        ).ifEmpty { localAlbums }
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
                homeAlbums = instantAlbums,
                homeAlbumsLoading = instantAlbums.isEmpty() && refreshRemote,
                charts = chartTracks,
                personalOrbitTracks = orbit,
                tracks = allTracks,
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
        val hasPlaybackHistory = _state.value.recentSearches.isNotEmpty() || _state.value.currentTrack != null
        warmPersistentOrbit(orbit, LevyraPersonalOrbit.DISPLAY_LIMIT, persist = hasPlaybackHistory)
        refreshMissingOfficialOrbitArtwork(orbit)
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
        queueEngine.replace(tracks, 0, keepPlaybackModes = true, radioEnabled = queueEngine.state.value.radioEnabled)
        queueIndex = 0
        startResolve(tracks.first())
    }

    fun addToQueue(track: Track) {
        queueEngine.addLast(track)
        refreshQueuePrefetch()
        _state.update { it.copy(offlineExportMessage = "Aggiunto alla coda: ${track.title}") }
    }

    fun playNext(track: Track) {
        queueEngine.playNext(track)
        refreshQueuePrefetch()
        _state.update { it.copy(offlineExportMessage = "Riproduci dopo: ${track.title}") }
    }

    fun removeFromQueue(index: Int) {
        val snapshot = queueEngine.remove(index)
        if (snapshot.tracks.isEmpty()) {
            closePlayer()
        } else {
            refreshQueuePrefetch()
        }
    }

    fun undoQueueRemoval() {
        queueEngine.undoRemove()
        refreshQueuePrefetch()
    }

    fun moveQueueItem(from: Int, to: Int) {
        queueEngine.move(from, to)
        refreshQueuePrefetch()
    }

    fun toggleContinuousRadio() {
        val enabled = !queueEngine.state.value.radioEnabled
        queueEngine.setRadioEnabled(enabled)
        if (enabled) ensureRadioTail(force = true)
    }

    fun setLyricsTranslationEnabled(value: Boolean) {
        preferences.setLyricsTranslationEnabled(value)
        _state.update { it.copy(lyricsTranslationEnabled = value) }
        _state.value.currentTrack?.let { track ->
            fetchLyrics(track)
            prefetchLyricsAround(track)
        }
    }

    fun selectChart(regionId: String) {
        if (regionId == _state.value.selectedChartId && _state.value.charts.isNotEmpty()) return
        _state.update { it.copy(selectedChartId = regionId) }
        loadCharts(regionId)
    }

    private fun loadCharts(regionId: String = _state.value.selectedChartId, deferUntilHomeIdle: Boolean = false) {
        val requestGeneration = chartsRequestGeneration.incrementAndGet()
        chartsJob?.cancel()
        chartsJob = viewModelScope.launch {
            val initialState = _state.value
            val languageCode = initialState.languageCode
            val hasVisibleCharts = initialState.charts.isNotEmpty()
            _state.update { current ->
                if (current.selectedChartId != regionId) current
                else current.copy(isLoadingCharts = !hasVisibleCharts)
            }

            val region = ChartsCatalog.region(regionId)
            val result = try {
                chartsRepository.topSongs(region.country)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "Charts refresh failed for %s", regionId)
                emptyList()
            }
            if (
                !isActive ||
                chartsRequestGeneration.get() != requestGeneration ||
                _state.value.languageCode != languageCode ||
                _state.value.selectedChartId != regionId
            ) return@launch

            if (result.isEmpty()) {
                _state.update { current ->
                    if (current.selectedChartId == regionId) current.copy(isLoadingCharts = false) else current
                }
                return@launch
            }

            withContext(Dispatchers.IO) {
                preferences.saveChartTracks(result, languageCode, regionId)
            }
            if (deferUntilHomeIdle && _state.value.charts != result) awaitHomeUiIdle()
            if (
                !isActive ||
                chartsRequestGeneration.get() != requestGeneration ||
                _state.value.languageCode != languageCode ||
                _state.value.selectedChartId != regionId
            ) return@launch

            _state.update { current ->
                if (current.selectedChartId != regionId) current
                else if (current.charts == result && !current.isLoadingCharts) current
                else current.copy(charts = result, isLoadingCharts = false)
            }
            persistHomeSnapshot()
            val startupPlan = homeStartupWorkPlan()
            viewModelScope.launch(Dispatchers.IO) {
                if (deferUntilHomeIdle) awaitHomeUiIdle(startupPlan)
                LevyraArtworkCache.preloadHome(
                    getApplication<Application>().applicationContext,
                    result,
                    startupPlan.chartArtworkCount
                )
            }
            enrichCharts(regionId, result, deferUntilHomeIdle)
            refreshOfficialMetadataBatch(result, 6, deferUntilHomeIdle)
        }
    }

    private fun enrichCharts(regionId: String, charts: List<Track>, deferUntilHomeIdle: Boolean = false) {
        chartEnrichJob?.cancel()
        chartEnrichJob = viewModelScope.launch(Dispatchers.IO) {
            val startupPlan = homeStartupWorkPlan()
            val enrichmentCount = if (deferUntilHomeIdle) startupPlan.chartEnrichmentCount else 6
            val hot = charts.take(enrichmentCount)
            val semaphore = Semaphore(if (deferUntilHomeIdle) startupPlan.chartEnrichmentConcurrency else 2)
            val replacements = coroutineScope {
                hot.mapIndexed { index, entry ->
                    async {
                        semaphore.withPermit {
                            enrichChartEntry(
                                regionId = regionId,
                                entry = entry,
                                warm = index < if (deferUntilHomeIdle) startupPlan.chartWarmCount else 2,
                                deferUntilHomeIdle = deferUntilHomeIdle
                            )
                        }
                    }
                }.awaitAll().filterNotNull().toMap()
            }
            if (replacements.isEmpty() || !isActive || _state.value.selectedChartId != regionId) return@launch
            if (deferUntilHomeIdle) awaitHomeUiIdle()
            _state.update { current ->
                if (current.selectedChartId != regionId) return@update current
                val updatedCharts = current.charts.map { chart -> replacements[chart.id] ?: chart }
                if (updatedCharts == current.charts) current else current.copy(charts = updatedCharts)
            }
            persistHomeSnapshot()
        }
    }

    private suspend fun enrichChartEntry(
        regionId: String,
        entry: Track,
        warm: Boolean,
        deferUntilHomeIdle: Boolean = false
    ): Pair<String, Track>? {
        if (!currentCoroutineContext().isActive || _state.value.selectedChartId != regionId) return null
        if (deferUntilHomeIdle) awaitHomeUiIdle()
        val match = if (entry.videoUrl.isNotBlank()) {
            entry
        } else {
            runCatching {
                repository.searchOne("${entry.title} ${entry.artist}", _state.value.languageCode)
            }.getOrNull() ?: return null
        }
        if (!currentCoroutineContext().isActive || _state.value.selectedChartId != regionId) return null
        val enriched = LevyraPersonalOrbit.preferAlbumArtwork(entry, match).copy(
            id = match.id,
            videoUrl = match.videoUrl,
            durationMs = if (match.durationMs > 0L) match.durationMs else entry.durationMs
        )
        if (warm) {
            if (deferUntilHomeIdle) awaitHomeUiIdle()
            val resolved = resolver.prefetch(match)
            if (resolved != null) runCatching { playbackWarmup.prime(resolved) }
        }
        return entry.id to enriched
    }

    fun removeRecentSearch(track: Track) {
        val current = _state.value.recentSearches
        val identity = LevyraPersonalOrbit.identityKey(track)
        val updated = current.filterNot {
            it.id == track.id || LevyraPersonalOrbit.identityKey(it) == identity
        }
        if (updated.size == current.size) return
        _state.update { it.copy(recentSearches = updated) }
        viewModelScope.launch(Dispatchers.IO) {
            preferences.saveRecentSearches(updated)
        }
    }

    fun toggleFavorite(track: Track) {
        val current = _state.value.favorites
        val exists = current.any { it.id == track.id }
        val updated = if (exists) current.filterNot { it.id == track.id } else listOf(track) + current
        viewModelScope.launch(Dispatchers.IO) { favoritesStore.save(updated) }
        LevyraArtworkCache.preloadPriority(getApplication<Application>().applicationContext, updated, 6)
        _state.update { it.copy(favorites = updated, favoriteIds = updated.map { fav -> fav.id }.toSet()) }
        recordSmartFavorite(track, !exists)
    }

    fun exportCurrentTrack() {
        val track = _state.value.currentTrack ?: return
        exportTrack(track)
    }

    fun openArtist(track: Track) {
        val artistName = primaryArtistSegment(track.artist).ifBlank { track.artist.trim() }
        val browseId = track.artistBrowseIds.firstOrNull().orEmpty()
        if (browseId.isNotBlank()) {
            openArtistReference(
                name = artistName,
                browseId = browseId,
                returnTarget = DetailReturnTarget.None
            )
        } else {
            openArtistByName(artistName)
        }
    }

    fun openArtistFromAlbum() {
        val album = _state.value.albumDetail?.album ?: return
        openArtistReference(
            name = album.artist,
            browseId = album.artistBrowseId,
            returnTarget = DetailReturnTarget.Album
        )
    }

    fun openArtistRelease(release: ArtistRelease, artistName: String) {
        openAlbumInternal(
            album = AlbumHit(
                title = release.title,
                artist = artistName.ifBlank { release.subtitle },
                year = release.year,
                thumbnailUrl = release.thumbnailUrl,
                query = listOf(release.title, artistName.ifBlank { release.subtitle }, "album").filter { it.isNotBlank() }.joinToString(" "),
                browseId = release.browseId
            ),
            returnTarget = DetailReturnTarget.Artist
        )
    }

    fun openArtistByName(name: String) {
        openArtistByNameInternal(name, DetailReturnTarget.None)
    }

    private fun openArtistByNameInternal(name: String, returnTarget: DetailReturnTarget) {
        openArtistReference(name = name, browseId = "", returnTarget = returnTarget)
    }

    private fun openArtistReference(name: String, browseId: String, returnTarget: DetailReturnTarget) {
        val clean = name.trim()
        val normalizedBrowseId = browseId.trim()
        if (clean.length < 2 || clean.equals("YouTube Music", ignoreCase = true) || clean.equals("YouTube", ignoreCase = true)) return
        artistJob?.cancel()
        _state.update { current ->
            val sameProfile = current.artistProfile?.let { profile ->
                if (normalizedBrowseId.isNotBlank()) {
                    profile.browseId.equals(normalizedBrowseId, ignoreCase = true)
                } else {
                    profile.name.equals(clean, ignoreCase = true)
                }
            } == true
            current.copy(
                showAlbum = false,
                showArtist = true,
                artistLoading = true,
                artistError = null,
                artistProfile = if (sameProfile) current.artistProfile else null,
                openPlaylist = null,
                detailReturnTarget = if (returnTarget == DetailReturnTarget.Album && current.albumDetail != null) {
                    DetailReturnTarget.Album
                } else {
                    DetailReturnTarget.None
                }
            )
        }
        artistJob = viewModelScope.launch {
            val profile = if (normalizedBrowseId.isNotBlank()) {
                runCatching { artistRepository.profile(normalizedBrowseId, clean) }.getOrNull()
            } else {
                runCatching { artistRepository.profileFor(clean) }.getOrNull()
            }
            if (!isActive) return@launch
            if (profile == null || (profile.topSongs.isEmpty() && !profile.hasBio)) {
                _state.update {
                    it.copy(
                        artistLoading = false,
                        artistError = ARTIST_PROFILE_UNAVAILABLE_ERROR,
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
        _state.update { current ->
            val returnToAlbum = current.detailReturnTarget == DetailReturnTarget.Album && current.albumDetail != null
            current.copy(
                showArtist = false,
                artistLoading = false,
                artistError = null,
                showAlbum = returnToAlbum,
                detailReturnTarget = DetailReturnTarget.None
            )
        }
    }

    fun openAlbum(album: AlbumHit) {
        openAlbumInternal(album, DetailReturnTarget.None)
    }

    private fun openAlbumInternal(album: AlbumHit, returnTarget: DetailReturnTarget) {
        albumJob?.cancel()
        if (returnTarget != DetailReturnTarget.Artist) artistJob?.cancel()
        val current = _state.value.albumDetail
        val sameAlbum = current != null && current.album.title.equals(album.title, ignoreCase = true) && current.album.artist.equals(album.artist, ignoreCase = true)
        _state.update { state ->
            val keepArtistParent = returnTarget == DetailReturnTarget.Artist && state.showArtist
            state.copy(
                showAlbum = true,
                albumLoading = true,
                albumError = null,
                albumDetail = if (sameAlbum) current else AlbumDetail(album, "", emptyList()),
                showArtist = keepArtistParent,
                artistLoading = if (keepArtistParent) state.artistLoading else false,
                artistError = if (keepArtistParent) state.artistError else null,
                openPlaylist = null,
                detailReturnTarget = if (keepArtistParent) DetailReturnTarget.Artist else DetailReturnTarget.None
            )
        }
        albumJob = viewModelScope.launch {
            val languageCode = _state.value.languageCode
            val detail = runCatching { repository.albumDetail(album, languageCode) }.getOrNull()
            if (!isActive) return@launch
            if (detail == null || detail.tracks.isEmpty()) {
                _state.update {
                    it.copy(
                        albumLoading = false,
                        albumError = "Album non disponibile",
                        albumDetail = detail ?: AlbumDetail(album, "", emptyList())
                    )
                }
                return@launch
            }
            _state.update {
                it.copy(
                    albumLoading = false,
                    albumError = null,
                    albumDetail = detail,
                    tracks = mergeTracks(detail.tracks, it.tracks),
                    searchResults = detail.tracks.take(12),
                    cacheReport = repository.cacheReport()
                )
            }
            recordSmartAlbumOpen(detail.album)
            LevyraArtworkCache.preloadPriority(getApplication<Application>().applicationContext, detail.tracks, 8)
            refreshOfficialMetadataBatch(detail.tracks, 12)
        }
    }

    fun closeAlbum() {
        albumJob?.cancel()
        _state.update { current ->
            val returnToArtist = current.detailReturnTarget == DetailReturnTarget.Artist && current.showArtist
            current.copy(
                showAlbum = false,
                albumLoading = false,
                albumError = null,
                showArtist = returnToArtist,
                detailReturnTarget = DetailReturnTarget.None
            )
        }
    }

    fun openPlayerScreen() {
        albumJob?.cancel()
        artistJob?.cancel()
        _state.update {
            it.copy(
                showAlbum = false,
                showArtist = false,
                openPlaylist = null,
                albumLoading = false,
                artistLoading = false,
                detailReturnTarget = DetailReturnTarget.None
            )
        }
        moveToTab(LevyraTab.Player, rememberCurrent = true)
    }

    fun playAlbumSong(track: Track) {
        val detail = _state.value.albumDetail ?: return
        playFrom(detail.tracks, track, loopOnCompletion = true)
    }

    fun playCurrentAlbum() {
        val detail = _state.value.albumDetail ?: return
        if (detail.tracks.isEmpty()) return
        playFrom(detail.tracks, detail.tracks.first(), loopOnCompletion = true)
    }

    fun exportCurrentAlbum() {
        val detail = _state.value.albumDetail ?: return
        exportTracksSequential(detail.tracks, "Album offline")
    }

    fun exportOpenPlaylist() {
        val playlist = _state.value.openPlaylist ?: return
        exportTracksSequential(playlist.tracks, "Playlist offline")
    }

    private fun exportTracksSequential(tracks: List<Track>, label: String) {
        val pending = tracks
            .filter { it.id.isNotBlank() || it.videoUrl.isNotBlank() || it.title.isNotBlank() }
            .distinctBy { downloadKeyFor(it) }
            .filterNot { track -> track.id.isNotBlank() && track.id in _state.value.downloadedTrackIds }
        if (pending.isEmpty()) {
            _state.update { it.copy(offlineExportMessage = "Già tutto offline") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(offlineExportMessage = "$label in coda: ${pending.size} brani") }
            pending.forEachIndexed { index, track ->
                if (index > 0) delay(220L)
                exportTrack(track)
            }
        }
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
        val downloadTitle = track.title.ifBlank { "brano" }
        activeDownloadTitles[downloadKey] = downloadTitle
        activeDownloadTracks[downloadKey] = track
        _state.update {
            it.copy(
                isOfflineExporting = true,
                offlineExportMessage = null,
                downloadingTrackIds = it.downloadingTrackIds + downloadKey,
                downloadProgressByTrackId = it.downloadProgressByTrackId + (downloadKey to 1),
                downloadTitleByTrackId = it.downloadTitleByTrackId + (downloadKey to downloadTitle)
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
                        delay(120L)
                    }
                }
                finished ?: throw CancellationException("Offline export observation cancelled")
            }
            result.onSuccess { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> handleOfflineExportSuccess(workInfo, downloadKey)
                    WorkInfo.State.FAILED -> {
                        val reason = workInfo.outputData.getString(OfflineExportWorker.KEY_ERROR)
                        if (reason == OfflineExportWorker.ERROR_SUPERSEDED) {
                            handleOfflineExportStopped(downloadKey)
                        } else {
                            handleOfflineExportFailure(cleanUserError(reason), downloadKey)
                        }
                    }
                    WorkInfo.State.CANCELLED -> handleOfflineExportStopped(downloadKey)
                    else -> handleOfflineExportFailure("Esportazione non riuscita", downloadKey)
                }
            }.onFailure { error ->
                if (error is CancellationException) {
                    activeDownloadKeys.remove(downloadKey)
                    activeDownloadTracks.remove(downloadKey)
                    activeDownloadTitles.remove(downloadKey)
                    _state.update {
                        it.copy(
                            downloadingTrackIds = it.downloadingTrackIds - downloadKey,
                            downloadProgressByTrackId = it.downloadProgressByTrackId - downloadKey,
                            downloadTitleByTrackId = it.downloadTitleByTrackId - downloadKey
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
        val completedTrack = activeDownloadTracks.remove(trackId)
        activeDownloadTitles.remove(trackId)
        val fileName = workInfo.outputData.getString(OfflineExportWorker.KEY_FILE_NAME).orEmpty()
        val destinationLabel = workInfo.outputData.getString(OfflineExportWorker.KEY_DESTINATION_LABEL).orEmpty().ifBlank { "Music/Levyra" }
        val embedded = workInfo.outputData.getBoolean(OfflineExportWorker.KEY_EMBEDDED_METADATA, false)
        val tagStatus = if (embedded) "con cover e metadata Levyra" else "con metadata Android"
        completedTrack?.let { recordSmartDownload(it) }
        _state.update {
            it.copy(
                isOfflineExporting = it.downloadingTrackIds.size > 1,
                offlineExportMessage = "Salvato in $destinationLabel: ${fileName.ifBlank { "brano esportato" }} ($tagStatus)",
                embeddedMetadataWriterReady = offlineExporter.embeddedMetadataWriterReady,
                downloadingTrackIds = it.downloadingTrackIds - trackId,
                downloadProgressByTrackId = it.downloadProgressByTrackId - trackId,
                downloadTitleByTrackId = it.downloadTitleByTrackId - trackId
            )
        }
    }

    private fun handleOfflineExportStopped(trackId: String) {
        activeDownloadKeys.remove(trackId)
        activeDownloadTracks.remove(trackId)
        activeDownloadTitles.remove(trackId)
        _state.update {
            it.copy(
                isOfflineExporting = it.downloadingTrackIds.size > 1,
                downloadingTrackIds = it.downloadingTrackIds - trackId,
                downloadProgressByTrackId = it.downloadProgressByTrackId - trackId,
                downloadTitleByTrackId = it.downloadTitleByTrackId - trackId
            )
        }
    }

    private fun handleOfflineExportFailure(message: String?, trackId: String) {
        activeDownloadKeys.remove(trackId)
        activeDownloadTracks.remove(trackId)
        activeDownloadTitles.remove(trackId)
        _state.update {
            it.copy(
                isOfflineExporting = it.downloadingTrackIds.size > 1,
                offlineExportMessage = cleanUserError(message),
                embeddedMetadataWriterReady = offlineExporter.embeddedMetadataWriterReady,
                downloadingTrackIds = it.downloadingTrackIds - trackId,
                downloadProgressByTrackId = it.downloadProgressByTrackId - trackId,
                downloadTitleByTrackId = it.downloadTitleByTrackId - trackId
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
        if (raw.contains("EXTM3U", ignoreCase = true) || raw.contains("contentIsMalformed", ignoreCase = true)) return "Stream non valido per questo risultato: ho scartato la fonte rotta, riprova il brano"
        if (raw.contains("timeout", ignoreCase = true)) return "Connessione lenta: riprova tra qualche secondo"
        if (raw.contains("Primary directory Music not allowed", ignoreCase = true) || raw.contains("content://media/external_primary/file", ignoreCase = true)) return "Questo telefono blocca il salvataggio generico in Music: aggiorna l'app e riprova, Levyra userà MediaStore Audio o Downloads/Levyra"
        return raw
    }

    private fun updateDownloadProgress(trackId: String, progress: Int) {
        _state.update {
            if (trackId !in it.downloadingTrackIds) {
                it
            } else {
                val safeProgress = monotonicDownloadProgress(it.downloadProgressByTrackId[trackId], progress)
                it.copy(
                    downloadProgressByTrackId = it.downloadProgressByTrackId + (trackId to safeProgress)
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
            snapshot.showAlbum -> {
                closeAlbum()
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
                    searchResults = tracks,
                    searchData = data,
                    cacheReport = repository.cacheReport(),
                    smartScore = calculateSmartScore(queue),
                    isSearching = false,
                    searchError = if (data.isEmpty) "Nessun risultato trovato per $clean" else null
                )
            }
            val startupPlan = adaptivePlaybackPolicy.current(videoMode = false)
            LevyraArtworkCache.preloadHome(getApplication<Application>().applicationContext, tracks, if (startupPlan.lowRam) 8 else 18)
            prefetchTop(tracks, if (startupPlan.lowRam) 3 else 8)
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
        openAlbum(album)
    }

    fun playAlbumRecommendations(albums: List<AlbumHit>) {
        val snapshot = albums.filter { it.query.isNotBlank() }.take(10)
        if (snapshot.isEmpty()) return
        viewModelScope.launch {
            val languageCode = _state.value.languageCode
            val playable = withContext(Dispatchers.IO) {
                snapshot.mapNotNull { album -> repository.searchOne(album.query, languageCode) }
                    .distinctBy { youtubePlayableTrack(it)?.id ?: it.id }
            }
            if (playable.isEmpty()) return@launch
            val mergedTracks = mergeTracks(playable, _state.value.tracks)
            _state.update {
                it.copy(
                    tracks = mergedTracks,
                    searchResults = playable.take(12),
                    cacheReport = repository.cacheReport(),
                    searchError = null
                )
            }
            LevyraArtworkCache.preloadHome(getApplication<Application>().applicationContext, playable, 10)
            playFrom(playable, playable.first(), loopOnCompletion = true)
        }
    }

    fun openArtistFromHit(hit: ArtistHit) {
        openArtistReference(
            name = hit.name,
            browseId = hit.browseId,
            returnTarget = DetailReturnTarget.None
        )
    }

    private fun recordPlaybackHistory(track: Track) {
        if (track.title.isBlank() || track.artist.isBlank()) return
        val snapshot = _state.value
        val artworkDonors = buildList {
            addAll(snapshot.personalOrbitTracks)
            addAll(snapshot.recentSearches)
            addAll(snapshot.charts)
            addAll(snapshot.homeSections.flatMap { it.tracks })
            addAll(snapshot.favorites)
            addAll(snapshot.tracks)
        }
        val stableTrack = LevyraPersonalOrbit.prepareForOrbit(track, artworkDonors)
        val updated = (listOf(stableTrack) + snapshot.recentSearches)
            .distinctBy { LevyraPersonalOrbit.identityKey(it) }
            .take(LevyraPersonalOrbit.DISPLAY_LIMIT)
        val orbit = LevyraPersonalOrbit.build(
            currentTrack = stableTrack,
            recentSearches = updated,
            favorites = snapshot.favorites,
            tracks = snapshot.tracks,
            homeSections = snapshot.homeSections,
            charts = snapshot.charts,
            cachedOrbit = snapshot.personalOrbitTracks,
            limit = LevyraPersonalOrbit.DISPLAY_LIMIT,
            languageCode = snapshot.languageCode
        )
        _state.update { it.copy(recentSearches = updated, personalOrbitTracks = orbit) }
        val appContext = getApplication<Application>().applicationContext
        if (LevyraPersonalOrbit.hasAnyArtwork(stableTrack)) {
            LevyraArtworkCache.preloadPriority(appContext, listOf(stableTrack), 1)
        }
        viewModelScope.launch(Dispatchers.IO) {
            preferences.saveRecentSearches(updated)
            preferences.savePersonalOrbitTracks(orbit, snapshot.languageCode)
            if (LevyraPersonalOrbit.hasAnyArtwork(stableTrack)) {
                LevyraArtworkCache.cachePersistent(appContext, listOf(stableTrack), 1)
            }
        }
        if (needsOfficialMetadata(stableTrack)) {
            refreshOfficialOrbitArtwork(stableTrack)
        }
    }

    private fun refreshOfficialOrbitArtwork(track: Track) {
        enqueueOfficialMetadata(listOf(track), 1, false)
    }

    private fun refreshOfficialMetadataBatch(
        tracks: List<Track>,
        limit: Int,
        deferUntilHomeIdle: Boolean = false
    ) {
        enqueueOfficialMetadata(tracks, limit, deferUntilHomeIdle)
    }

    private fun refreshMissingOfficialOrbitArtwork(tracks: List<Track>, deferUntilHomeIdle: Boolean = false) {
        orbitArtworkJob?.cancel()
        orbitArtworkJob = viewModelScope.launch {
            val startupPlan = homeStartupWorkPlan()
            delay(if (deferUntilHomeIdle) startupPlan.secondaryStartDelayMs else 250L)
            if (deferUntilHomeIdle) awaitHomeUiIdle(startupPlan)
            enqueueOfficialMetadata(tracks, LevyraPersonalOrbit.DISPLAY_LIMIT, false)
        }
    }

    private fun enqueueOfficialMetadata(
        tracks: List<Track>,
        limit: Int,
        deferUntilHomeIdle: Boolean
    ) {
        val pending = tracks
            .asSequence()
            .filter { it.title.isNotBlank() && it.artist.isNotBlank() }
            .filter(::needsOfficialMetadata)
            .distinctBy { LevyraPersonalOrbit.identityKey(it) }
            .take(limit.coerceIn(1, OFFICIAL_METADATA_MAX_BATCH_SIZE))
            .toList()
        if (pending.isEmpty()) return
        pending.forEach { track ->
            val key = LevyraPersonalOrbit.identityKey(track)
            if (key !in officialMetadataInFlightKeys) {
                officialMetadataPending.merge(key, track) { existing, incoming -> richerOfficialMetadataSeed(existing, incoming) }
            }
        }
        if (officialMetadataPending.isEmpty()) return
        if (deferUntilHomeIdle) officialMetadataDeferUntilIdle.set(true)
        officialMetadataSignal.trySend(Unit)
    }

    private fun richerOfficialMetadataSeed(existing: Track, incoming: Track): Track {
        return if (officialMetadataSeedScore(incoming) >= officialMetadataSeedScore(existing)) incoming else existing
    }

    private fun officialMetadataSeedScore(track: Track): Int {
        return listOf(
            track.album,
            track.thumbnailUrl,
            track.largeThumbnailUrl,
            track.isrc,
            track.upc,
            track.releaseDate,
            track.albumBrowseId,
            track.canonicalAlbumUrl
        ).count(String::isNotBlank) * 10 + track.metadataConfidence.coerceIn(0, 100)
    }

    private suspend fun consumeOfficialMetadataQueue() {
        for (ignored in officialMetadataSignal) {
            while (currentCoroutineContext().isActive) {
                if (officialMetadataDeferUntilIdle.getAndSet(false)) {
                    awaitHomeUiIdle(homeStartupWorkPlan())
                }
                val batch = officialMetadataPending.entries
                    .asSequence()
                    .mapNotNull { entry ->
                        if (officialMetadataInFlightKeys.add(entry.key)) entry.key to entry.value else null
                    }
                    .take(OFFICIAL_METADATA_MAX_BATCH_SIZE)
                    .toList()
                if (batch.isEmpty()) break
                batch.forEach { (key, track) -> officialMetadataPending.remove(key, track) }
                val enrichedTracks = Collections.synchronizedList(mutableListOf<Track>())
                val semaphore = Semaphore(OFFICIAL_METADATA_CONCURRENCY)
                coroutineScope {
                    batch.map { (key, track) ->
                        launch {
                            try {
                                semaphore.withPermit {
                                    if (!isActive) return@withPermit
                                    awaitHomeUiIdle()
                                    val enriched = resolveOfficialOrbitArtwork(track, _state.value.languageCode)
                                        ?: return@withPermit
                                    enrichedTracks += enriched
                                }
                            } finally {
                                officialMetadataInFlightKeys.remove(key)
                            }
                        }
                    }.forEach { it.join() }
                }
                if (enrichedTracks.isNotEmpty()) {
                    awaitHomeUiIdle()
                    applyOfficialOrbitArtworkBatch(enrichedTracks.toList())
                }
                if (officialMetadataPending.isEmpty()) break
            }
        }
    }

    private fun needsOfficialMetadata(track: Track): Boolean {
        return !LevyraPersonalOrbit.hasSquareAlbumArtwork(track) ||
            track.metadataProvider.isBlank() ||
            (track.canonicalAlbumUrl.isBlank() && track.releaseDate.isBlank())
    }

    private fun officialMetadataConfidence(score: Int): Int = when {
        score >= 500 -> 100
        score >= 420 -> 96
        score >= 360 -> 91
        score >= 320 -> 86
        score >= 280 -> 80
        score >= 240 -> 72
        score >= 200 -> 64
        else -> (score / 4).coerceIn(0, 63)
    }

    private suspend fun resolveOfficialOrbitArtwork(track: Track, languageCode: String): Track? {
        if (!needsOfficialMetadata(track)) return track
        val selectedCountry = ChartsCatalog.regions
            .firstOrNull { it.id == _state.value.selectedChartId }
            ?.country
            .orEmpty()
            .ifBlank { ChartsCatalog.defaultRegionForLanguage(languageCode).country }
        val official = runCatching {
            officialArtworkRepository.find(track, selectedCountry)
        }.getOrNull()
        if (official != null) {
            return track.copy(
                album = official.album.ifBlank { track.album },
                thumbnailUrl = official.thumbnailUrl,
                largeThumbnailUrl = official.largeThumbnailUrl,
                isrc = official.isrc.ifBlank { track.isrc },
                upc = official.upc.ifBlank { track.upc },
                releaseDate = official.releaseDate.ifBlank { track.releaseDate },
                year = official.year.ifBlank { track.year },
                trackNumber = official.trackNumber.takeIf { it > 0 } ?: track.trackNumber,
                discNumber = official.discNumber.takeIf { it > 0 } ?: track.discNumber,
                explicit = official.explicit || track.explicit,
                metadataProvider = official.provider.ifBlank { track.metadataProvider },
                metadataConfidence = maxOf(track.metadataConfidence, officialMetadataConfidence(official.score)),
                canonicalAlbumUrl = official.canonicalAlbumUrl.ifBlank { track.canonicalAlbumUrl }
            )
        }
        val musicMatches = runCatching {
            repository.search("${track.title} ${track.artist}", 10, languageCode)
        }.getOrDefault(emptyList())
        val officialTrack = LevyraPersonalOrbit.prepareForOrbit(track, musicMatches)
        return officialTrack.takeIf { LevyraPersonalOrbit.hasSquareAlbumArtwork(it) }
    }

    private suspend fun applyOfficialOrbitArtworkBatch(enrichedTracks: List<Track>) {
        val enrichedByKey = enrichedTracks
            .distinctBy { LevyraPersonalOrbit.identityKey(it) }
            .associateBy { LevyraPersonalOrbit.identityKey(it) }
        if (enrichedByKey.isEmpty()) return
        var persistedHistory: List<Track> = emptyList()
        var persistedOrbit: List<Track> = emptyList()
        var persistedHomeAlbums: List<AlbumHit> = emptyList()
        var persistedFavorites: List<Track> = emptyList()
        var persistedPlaylists: List<Playlist> = emptyList()
        var shouldPersistFavorites = false
        var shouldPersistPlaylists = false
        var languageCode = _state.value.languageCode
        _state.update { current ->
            fun withArtwork(item: Track): Track {
                val enriched = enrichedByKey[LevyraPersonalOrbit.identityKey(item)] ?: return item
                return item.copy(
                    album = enriched.album.ifBlank { item.album },
                    thumbnailUrl = enriched.thumbnailUrl.ifBlank { item.thumbnailUrl },
                    largeThumbnailUrl = enriched.largeThumbnailUrl.ifBlank { item.largeThumbnailUrl },
                    isrc = enriched.isrc.ifBlank { item.isrc },
                    upc = enriched.upc.ifBlank { item.upc },
                    releaseDate = enriched.releaseDate.ifBlank { item.releaseDate },
                    year = enriched.year.ifBlank { item.year },
                    trackNumber = enriched.trackNumber.takeIf { it > 0 } ?: item.trackNumber,
                    discNumber = enriched.discNumber.takeIf { it > 0 } ?: item.discNumber,
                    explicit = enriched.explicit || item.explicit,
                    metadataProvider = enriched.metadataProvider.ifBlank { item.metadataProvider },
                    metadataConfidence = maxOf(item.metadataConfidence, enriched.metadataConfidence),
                    canonicalAlbumUrl = enriched.canonicalAlbumUrl.ifBlank { item.canonicalAlbumUrl }
                )
            }

            fun normalizedMetadataText(value: String): String = value
                .lowercase()
                .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

            fun withAlbumMetadata(item: AlbumHit): AlbumHit {
                val targetTitle = normalizedMetadataText(item.title)
                val targetArtist = normalizedMetadataText(item.artist)
                val enriched = enrichedByKey.values
                    .asSequence()
                    .mapNotNull { track ->
                        val candidateAlbum = normalizedMetadataText(track.album)
                        val candidateArtist = normalizedMetadataText(track.artist)
                        val albumScore = when {
                            targetTitle.isBlank() || candidateAlbum.isBlank() -> 0
                            targetTitle == candidateAlbum -> 4
                            targetTitle.contains(candidateAlbum) || candidateAlbum.contains(targetTitle) -> 2
                            else -> 0
                        }
                        val artistScore = when {
                            targetArtist.isBlank() || candidateArtist.isBlank() -> 0
                            targetArtist == candidateArtist -> 3
                            targetArtist.contains(candidateArtist) || candidateArtist.contains(targetArtist) -> 1
                            else -> 0
                        }
                        val sameArtistBrowseId = item.artistBrowseId.isNotBlank() &&
                            track.artistBrowseIds.any { it.equals(item.artistBrowseId, ignoreCase = true) }
                        val sameAlbumBrowseId = item.browseId.isNotBlank() &&
                            track.albumBrowseId.equals(item.browseId, ignoreCase = true)
                        val sameUpc = item.upc.isNotBlank() && track.upc.isNotBlank() &&
                            item.upc.equals(track.upc, ignoreCase = true)
                        val strongIdentifier = sameArtistBrowseId || sameAlbumBrowseId || sameUpc
                        val artistCompatible = artistScore > 0
                        if (!artistCompatible && !strongIdentifier) return@mapNotNull null
                        val score = albumScore + artistScore + if (strongIdentifier) 4 else 0
                        (track to score).takeIf { score >= 5 }
                    }
                    .maxWithOrNull(compareBy<Pair<Track, Int>> { it.second }.thenBy { it.first.metadataConfidence })
                    ?.first
                    ?: return item
                return item.copy(
                    title = enriched.album.ifBlank { item.title },
                    year = enriched.year.ifBlank { item.year },
                    thumbnailUrl = enriched.largeThumbnailUrl.ifBlank {
                        enriched.thumbnailUrl.ifBlank { item.thumbnailUrl }
                    },
                    explicit = enriched.explicit || item.explicit,
                    releaseDate = enriched.releaseDate.ifBlank { item.releaseDate },
                    upc = enriched.upc.ifBlank { item.upc },
                    canonicalUrl = enriched.canonicalAlbumUrl.ifBlank { item.canonicalUrl },
                    metadataProvider = enriched.metadataProvider.ifBlank { item.metadataProvider },
                    metadataConfidence = maxOf(item.metadataConfidence, enriched.metadataConfidence)
                )
            }

            val currentTrack = current.currentTrack?.let(::withArtwork)
            val recentSearches = current.recentSearches.map(::withArtwork)
            val cachedOrbit = current.personalOrbitTracks.map(::withArtwork)
            val tracks = current.tracks.map(::withArtwork)
            val queue = current.queue.map(::withArtwork)
            val searchResults = current.searchResults.map(::withArtwork)
            val favorites = current.favorites.map(::withArtwork)
            val charts = current.charts.map(::withArtwork)
            val homeAlbums = current.homeAlbums.map(::withAlbumMetadata)
            val homeSections = current.homeSections.map { section ->
                section.copy(tracks = section.tracks.map(::withArtwork))
            }
            val exploreTracks = current.exploreTracks.map(::withArtwork)
            val exploreVideos = current.exploreVideos.map(::withArtwork)
            val recentListens = current.recentListens.map(::withArtwork)
            val playlists = current.playlists.map { playlist ->
                playlist.copy(tracks = playlist.tracks.map(::withArtwork))
            }
            val openPlaylist = current.openPlaylist?.let { playlist ->
                playlist.copy(tracks = playlist.tracks.map(::withArtwork))
            }
            val searchData = current.searchData.copy(
                topTrack = current.searchData.topTrack?.let(::withArtwork),
                songs = current.searchData.songs.map(::withArtwork),
                albums = current.searchData.albums.map(::withAlbumMetadata)
            )
            val albumDetail = current.albumDetail?.let { detail ->
                detail.copy(
                    album = withAlbumMetadata(detail.album),
                    tracks = detail.tracks.map(::withArtwork),
                    otherVersions = detail.otherVersions.map(::withAlbumMetadata)
                )
            }
            val artistProfile = current.artistProfile?.let { profile ->
                profile.copy(
                    topSongs = profile.topSongs.map(::withArtwork),
                    videos = profile.videos.map(::withArtwork)
                )
            }
            val orbit = LevyraPersonalOrbit.build(
                currentTrack = currentTrack,
                recentSearches = recentSearches,
                favorites = favorites,
                tracks = tracks,
                homeSections = homeSections,
                charts = charts,
                cachedOrbit = cachedOrbit,
                limit = LevyraPersonalOrbit.DISPLAY_LIMIT,
                languageCode = current.languageCode
            )
            persistedHistory = recentSearches
            persistedOrbit = orbit
            persistedHomeAlbums = homeAlbums
            persistedFavorites = favorites
            persistedPlaylists = (playlists + listOfNotNull(openPlaylist)).distinctBy { it.id }
            shouldPersistFavorites = current.favorites.any {
                LevyraPersonalOrbit.identityKey(it) in enrichedByKey.keys
            }
            shouldPersistPlaylists = persistedPlaylists.any { playlist ->
                playlist.tracks.any { LevyraPersonalOrbit.identityKey(it) in enrichedByKey.keys }
            }
            languageCode = current.languageCode
            current.copy(
                currentTrack = currentTrack,
                recentSearches = recentSearches,
                personalOrbitTracks = orbit,
                tracks = tracks,
                queue = queue,
                searchResults = searchResults,
                favorites = favorites,
                charts = charts,
                homeAlbums = homeAlbums,
                homeSections = homeSections,
                exploreTracks = exploreTracks,
                exploreVideos = exploreVideos,
                recentListens = recentListens,
                playlists = playlists,
                openPlaylist = openPlaylist,
                searchData = searchData,
                albumDetail = albumDetail,
                artistProfile = artistProfile
            )
        }
        persistHomeSnapshot()
        enrichedByKey.values.forEach { queueEngine.updateTrackMetadata(it) }
        val appContext = getApplication<Application>().applicationContext
        val artworkTracks = persistedOrbit
            .filter { LevyraPersonalOrbit.identityKey(it) in enrichedByKey.keys }
            .take(LevyraPersonalOrbit.DISPLAY_LIMIT)
        withContext(Dispatchers.IO) {
            preferences.saveRecentSearches(persistedHistory)
            preferences.savePersonalOrbitTracks(persistedOrbit, languageCode)
            preferences.saveHomeAlbums(persistedHomeAlbums, languageCode)
            if (shouldPersistFavorites) favoritesStore.saveSuspending(persistedFavorites)
            if (shouldPersistPlaylists) playlistStore.updateTrackMetadata(persistedPlaylists)
            if (artworkTracks.isNotEmpty()) {
                LevyraArtworkCache.cachePersistent(appContext, artworkTracks, artworkTracks.size)
            }
        }
        if (artworkTracks.isNotEmpty()) {
            LevyraArtworkCache.preloadPriority(
                appContext,
                artworkTracks,
                artworkTracks.size
            )
        }
    }

    fun playQueueTrack(track: Track) {
        val snapshot = queueEngine.state.value
        val index = snapshot.tracks.indexOfFirst { samePlayableTrack(it, track) }
        if (index < 0) {
            play(track)
            return
        }
        queueEngine.select(index, positionMs = 0L, rememberCurrent = true)
        loopCurrentQueueOnCompletion = snapshot.tracks.size > 1
        startResolve(snapshot.tracks[index])
    }

    fun play(track: Track) {
        val contextualQueue = queueForTrack(track)
        loopCurrentQueueOnCompletion = contextualQueue.size > 1
        val index = contextualQueue.indexOfFirst { samePlayableTrack(it, track) }.coerceAtLeast(0)
        queueEngine.replace(contextualQueue, index, keepPlaybackModes = true, radioEnabled = queueEngine.state.value.radioEnabled)
        queueIndex = index
        startResolve(contextualQueue.getOrElse(index) { track })
    }

    fun playFrom(list: List<Track>, track: Track, loopOnCompletion: Boolean = false) {
        if (list.isEmpty()) return
        loopCurrentQueueOnCompletion = loopOnCompletion
        val index = list.indexOfFirst { samePlayableTrack(it, track) }.coerceAtLeast(0)
        queueEngine.replace(list, index, keepPlaybackModes = true, radioEnabled = queueEngine.state.value.radioEnabled)
        queueIndex = index
        startResolve(list.getOrElse(index) { track })
    }

    private fun startResolve(track: Track, preserveCrossfade: Boolean = false) {
        streamTransitionId++
        modeSwitchJob?.cancel()
        streamRecoveryJob?.cancel()
        alternateModePrefetchJob?.cancel()
        if (!preserveCrossfade) {
            crossfadeJob?.cancel()
            crossfadeInProgress = false
            player.setVolume(1f)
        }
        val requestId = ++playRequestId
        playJob?.cancel()
        cancelBackgroundWarmups(cancelList = true)
        resolver.warmNetwork()
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
        fetchLyrics(track)
        prefetchLyricsAround(track)

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
                val videos = runCatching { repository.newMusicVideos(_state.value.languageCode, 12) }.getOrDefault(emptyList())
                if (videos.isEmpty()) exploreVideosLoaded = false
                _state.update { it.copy(exploreVideos = videos) }
                refreshOfficialMetadataBatch(videos, 6)
            }
        }
    }

    fun selectExploreZone(zone: ExploreZone) {
        _state.update { it.copy(exploreZoneId = zone.id) }
        exploreCache[zone.id]?.let { cached ->
            _state.update { it.copy(exploreTracks = cached, isExploreLoading = false) }
            refreshOfficialMetadataBatch(cached, 8)
            return
        }
        exploreJob?.cancel()
        _state.update { it.copy(exploreTracks = emptyList(), isExploreLoading = true) }
        exploreJob = viewModelScope.launch {
            val results = runCatching {
                repository.exploreZone(zone.id, zone.query, _state.value.languageCode, 24)
            }.getOrDefault(emptyList())
            if (results.isNotEmpty()) exploreCache[zone.id] = results
            if (_state.value.exploreZoneId != zone.id) return@launch
            _state.update { it.copy(exploreTracks = results, isExploreLoading = false) }
            refreshOfficialMetadataBatch(results, 8)
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
        queueEngine.replace(listOf(track), 0, keepPlaybackModes = true, radioEnabled = false)
        queueIndex = 0
        startResolve(track)
    }

    private fun startPlayback(playable: Track) {
        val selectedIndex = queueEngine.state.value.currentIndex
        if (selectedIndex >= 0) queueEngine.updateTrackAt(selectedIndex, playable)
        repository.replace(playable)
        player.play(playable, _state.value.isVideoMode)
        // Resume from the saved position when continuing the last session's track.
        val resumeMs = pendingSeekMs.takeIf { it > 1500L && it < playable.durationMs } ?: 0L
        if (resumeMs > 0L) player.seekTo(resumeMs)
        queueEngine.updatePosition(resumeMs)
        pendingSeekMs = 0L
        _state.update {
            it.copy(
                currentTrack = playable,
                tracks = mergeTracks(it.tracks, listOf(playable)),
                searchResults = mergeTracks(it.searchResults, listOf(playable)),
                activeLyric = lyricsEngine.currentLine(resumeMs, it.lyrics),
                isPlaying = true,
                isResolving = false,
                durationMs = effectiveDuration(playable),
                positionMs = resumeMs,
                cacheReport = repository.cacheReport(),
                playerError = null
            )
        }
        recordPlaybackHistory(playable)
        beginListenSession(playable)
        recordSmartPlayback(playable)
        fetchSponsorSegments(playable)
        prefetchAlternateMode(playable, _state.value.isVideoMode)
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
        val snapshot = _state.value
        val track = snapshot.currentTrack
        if (track != null && snapshot.lyrics.isEmpty() && !snapshot.lyricsLoading) {
            fetchLyrics(track)
        }
    }

    fun closeLyrics() {
        _state.update { it.copy(showLyrics = false) }
    }

    private fun fetchLyrics(track: Track) {
        lyricsJob?.cancel()
        val requestGeneration = ++lyricsRequestGeneration
        val trackIdentity = playbackIdentity(track)
        val preserveVisibleLyrics = lyricsTrackId == trackIdentity && _state.value.lyrics.isNotEmpty()
        _state.update {
            it.copy(
                lyrics = if (preserveVisibleLyrics) it.lyrics else emptyList(),
                lyricsSections = if (preserveVisibleLyrics) it.lyricsSections else emptyList(),
                lyricsSynced = if (preserveVisibleLyrics) it.lyricsSynced else false,
                lyricsProvider = if (preserveVisibleLyrics) it.lyricsProvider else "",
                lyricsConfidence = if (preserveVisibleLyrics) it.lyricsConfidence else 0,
                lyricsCached = if (preserveVisibleLyrics) it.lyricsCached else false,
                lyricsLoading = true,
                intelligenceSummary = if (preserveVisibleLyrics) it.intelligenceSummary else com.luc4n3x.levyra.domain.LevyraIntelligenceSummary()
            )
        }
        lyricsJob = viewModelScope.launch {
            var received = false
            try {
                lyricsRepository.observe(
                    title = track.title,
                    artist = track.artist,
                    durationSec = track.durationMs / 1000L,
                    album = track.album,
                    videoId = youtubePlayableTrack(track)?.id.orEmpty(),
                    languageCode = _state.value.languageCode,
                    translate = _state.value.lyricsTranslationEnabled
                ).collect { result ->
                    val currentIdentity = _state.value.currentTrack?.let(::playbackIdentity)
                    if (requestGeneration != lyricsRequestGeneration || currentIdentity != trackIdentity) return@collect
                    received = true
                    lyricsTrackId = trackIdentity
                    val lines = result.lines
                    _state.update {
                        it.copy(
                            lyrics = lines,
                            lyricsSections = result.sections,
                            lyricsSynced = result.synced,
                            lyricsProvider = result.provider,
                            lyricsConfidence = result.confidence,
                            lyricsCached = result.cached,
                            lyricsLoading = false
                        )
                    }
                    val intelligence = withContext(Dispatchers.Default) { localIntelligence.analyze(track, lines) }
                    val latestIdentity = _state.value.currentTrack?.let(::playbackIdentity)
                    if (requestGeneration == lyricsRequestGeneration && latestIdentity == trackIdentity) {
                        _state.update { it.copy(intelligenceSummary = intelligence) }
                    }
                }
            } finally {
                val currentIdentity = _state.value.currentTrack?.let(::playbackIdentity)
                if (requestGeneration == lyricsRequestGeneration && currentIdentity == trackIdentity) {
                    _state.update { it.copy(lyricsLoading = false) }
                    if (!received && _state.value.lyrics.isEmpty()) {
                        val intelligence = withContext(Dispatchers.Default) { localIntelligence.analyze(track, emptyList()) }
                        val latestIdentity = _state.value.currentTrack?.let(::playbackIdentity)
                        if (requestGeneration == lyricsRequestGeneration && latestIdentity == trackIdentity) {
                            _state.update { it.copy(intelligenceSummary = intelligence) }
                        }
                    }
                }
            }
        }
    }

    private fun refreshQueuePrefetch() {
        prefetchJob?.cancel()
        PlaybackService.clearPreparedQueueNext()
        val current = _state.value.currentTrack
        if (current != null) prefetchLyricsAround(current)
        if (_state.value.isPlaying && current != null && current.streamUrl.isNotBlank()) prefetchAround(current)
    }

    private fun prefetchLyricsAround(current: Track) {
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
            .filterNot { samePlayableTrack(it, current) }
            .distinctBy { playbackIdentity(it) }
            .take(limit)
        if (upcoming.isEmpty()) return
        val languageCode = _state.value.languageCode
        val translate = _state.value.lyricsTranslationEnabled
        lyricsPrefetchJob = viewModelScope.launch(Dispatchers.IO) {
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

    private fun lyricsNetworkProfile(): LyricsNetworkProfile {
        val connectivity = getApplication<Application>().getSystemService(ConnectivityManager::class.java)
            ?: return LyricsNetworkProfile(false, false)
        val network = connectivity.activeNetwork ?: return LyricsNetworkProfile(false, false)
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return LyricsNetworkProfile(false, false)
        val connected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val unmetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val backgroundRestricted = !unmetered &&
            connectivity.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
        return LyricsNetworkProfile(connected && !backgroundRestricted, unmetered)
    }


    private data class LyricsNetworkProfile(
        val connected: Boolean,
        val unmetered: Boolean
    )

    private fun prefetchAround(playable: Track) {
        prefetchJob?.cancel()
        PlaybackService.clearPreparedQueueNext()
        val generation = queueEngine.state.value.generation
        prefetchJob = viewModelScope.launch(Dispatchers.IO) {
            val plan = adaptivePlaybackPolicy.current(_state.value.isVideoMode)
            val candidates = queueEngine.upcoming(plan.resolveCount)
                .filterNot { samePlayableTrack(it, playable) }
                .distinctBy { playbackIdentity(it) }
            if (candidates.isEmpty()) {
                ensureRadioTail(force = false)
                return@launch
            }
            val semaphore = Semaphore(plan.concurrency.coerceAtLeast(1))
            coroutineScope {
                candidates.forEachIndexed { index, track ->
                    launch {
                        if (index > 0) delay(index * plan.staggerMs)
                        semaphore.withPermit {
                            if (!isActive || queueEngine.state.value.generation != generation) return@withPermit
                            val youtube = youtubePlayableTrack(track)
                            val resolved = if (youtube != null) {
                                resolver.prefetch(youtube, _state.value.isVideoMode)
                            } else {
                                val match = runCatching {
                                    repository.searchOne("${track.title} ${track.artist}", _state.value.languageCode)
                                }.getOrNull()
                                match?.let { resolver.prefetch(it, _state.value.isVideoMode) }
                            }
                            if (resolved != null && index < plan.primeCount && queueEngine.state.value.generation == generation) {
                                if (_state.value.isVideoMode) {
                                    runCatching { playbackWarmup.primeVideo(resolved) }
                                } else {
                                    runCatching { playbackWarmup.prime(resolved, plan.primeBytes) }
                                    if (index == 0 && !plan.lowRam && !plan.powerConstrained) {
                                        PlaybackService.prepareQueueNext(resolved)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            ensureRadioTail(force = false)
        }
    }

    private fun ensureRadioTail(force: Boolean, playWhenReady: Boolean = false) {
        val queueSnapshot = queueEngine.state.value
        if (!queueSnapshot.radioEnabled || queueSnapshot.currentTrack == null) return
        if (!force && queueEngine.upcoming(3).size >= 3) return
        if (radioJob?.isActive == true) return
        val seed = queueSnapshot.currentTrack ?: return
        val generation = queueSnapshot.generation
        radioJob = viewModelScope.launch(Dispatchers.IO) {
            val radioTracks = runCatching {
                repository.radio(seed, _state.value.languageCode, 20)
            }.getOrDefault(emptyList())
            if (!isActive || radioTracks.isEmpty()) return@launch
            val before = queueEngine.state.value
            val sameSeed = before.currentTrack?.let { playbackQueueIdentity(it) } == playbackQueueIdentity(seed)
            if (!sameSeed || (before.generation != generation && !force)) return@launch
            queueEngine.appendRadioTracks(radioTracks)
            if (playWhenReady) {
                withContext(Dispatchers.Main) {
                    queueEngine.next(respectRepeatOne = false)?.let(::startResolve)
                }
            }
        }
    }

    private fun prefetchTop(tracks: List<Track>, count: Int = 8, respectHomeScroll: Boolean = false) {
        val plan = adaptivePlaybackPolicy.current(videoMode = false)
        val effectiveCount = if (plan.lowRam || plan.powerConstrained) count.coerceAtMost(3) else count
        val candidates = tracks
            .filter { it.id.isNotBlank() || it.videoUrl.isNotBlank() }
            .distinctBy { youtubePlayableTrack(it)?.id ?: it.id }
            .take(effectiveCount.coerceIn(1, 8))
        if (candidates.isEmpty()) return
        listPrefetchJob?.cancel()
        listPrefetchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(if (plan.lowRam) 450L else 250L)
            if (respectHomeScroll) awaitHomeUiIdle()
            resolver.warmNetwork()
            val hotCount = if (plan.lowRam || plan.powerConstrained) 1 else 4
            val hot = candidates.take(hotCount)
            val warmOnly = candidates.drop(hotCount)
            warmTracks(hot, concurrency = plan.concurrency, delayStepMs = plan.staggerMs, prime = true, respectHomeScroll = respectHomeScroll)
            warmTracks(warmOnly, concurrency = 1, delayStepMs = plan.staggerMs.coerceAtLeast(80L), prime = false, respectHomeScroll = respectHomeScroll)
        }
    }

    private fun cancelBackgroundWarmups(cancelList: Boolean = true) {
        prefetchJob?.cancel()
        prefetchJob = null
        lyricsPrefetchJob?.cancel()
        lyricsPrefetchJob = null
        PlaybackService.clearPreparedQueueNext()
        if (cancelList) {
            listPrefetchJob?.cancel()
            listPrefetchJob = null
        }
    }

    private suspend fun warmTracks(
        tracks: List<Track>,
        concurrency: Int,
        delayStepMs: Long,
        prime: Boolean,
        respectHomeScroll: Boolean = false
    ) = coroutineScope {
        val semaphore = Semaphore(concurrency.coerceAtLeast(1))
        tracks.distinctBy { youtubePlayableTrack(it)?.id ?: it.id }.forEachIndexed { index, track ->
            launch {
                if (index > 0 && delayStepMs > 0L) delay(index * delayStepMs)
                if (respectHomeScroll) awaitHomeUiIdle()
                semaphore.withPermit {
                    if (respectHomeScroll) awaitHomeUiIdle()
                    warmTrack(track, prime)
                }
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

    fun play() = _state.value.currentTrack?.let { current ->
        if (current.streamUrl.isBlank()) play(current) else player.play(current, _state.value.isVideoMode)
    }
    fun pause() = player.pause()

    fun togglePlay() {
        val current = _state.value.currentTrack ?: return
        if (current.streamUrl.isBlank()) {
            play(current)
            return
        }
        if (_state.value.isPlaying) {
            player.pause()
            saveLastPlaybackAsync(current, player.positionMs)
            _state.update { it.copy(isPlaying = false) }
        } else {
            player.play(current, _state.value.isVideoMode)
            _state.update { it.copy(isPlaying = true) }
        }
        updateWidget()
    }

    fun closePlayer() {
        loopCurrentQueueOnCompletion = false
        streamTransitionId++
        playRequestId++
        playJob?.cancel()
        modeSwitchJob?.cancel()
        streamRecoveryJob?.cancel()
        alternateModePrefetchJob?.cancel()
        prefetchJob?.cancel()
        crossfadeJob?.cancel()
        lyricsJob?.cancel()
        lyricsPrefetchJob?.cancel()
        sponsorJob?.cancel()
        radioJob?.cancel()
        radioJob = null
        sleepJob?.cancel()
        sleepJob = null
        sponsorSegments = emptyList()
        cancelBackgroundWarmups(cancelList = true)
        crossfadeInProgress = false
        player.setVolume(1f)
        pendingSeekMs = 0L
        queueIndex = -1
        flushListenSession()
        player.stop()
        queueEngine.clear()
        _state.update {
            it.copy(
                selectedTab = if (it.selectedTab == LevyraTab.Player) LevyraTab.Home else it.selectedTab,
                currentTrack = null,
                queue = emptyList(),
                queueCurrentIndex = -1,
                queueUndoAvailable = false,
                queueHistoryCount = 0,
                radioEnabled = false,
                sleepTimerMinutes = 0,
                isPlaying = false,
                isResolving = false,
                positionMs = 0L,
                durationMs = 0L,
                showQueue = false,
                showLyrics = false,
                lyrics = emptyList(),
                lyricsSections = emptyList(),
                lyricsLoading = false,
                lyricsSynced = false,
                lyricsProvider = "",
                activeLyric = null
            )
        }
        lastPlaybackSaveJob?.cancel()
        lastPlaybackSaveJob = viewModelScope.launch(Dispatchers.IO) {
            preferences.saveLastPlayback(null, 0L)
            queueEngine.flush()
        }
        updateWidget()
    }

    fun next() {
        val nextTrack = queueEngine.next(respectRepeatOne = false)
        if (nextTrack != null) {
            startResolve(nextTrack)
            return
        }
        if (queueEngine.state.value.radioEnabled) ensureRadioTail(force = true, playWhenReady = true)
    }

    fun previous() {
        if (player.positionMs > 5_000L) {
            player.seekTo(0L)
            queueEngine.updatePosition(0L)
            _state.update { it.copy(positionMs = 0L) }
            return
        }
        queueEngine.previous()?.let(::startResolve)
    }

    fun seekTo(progress: Float) {
        val duration = _state.value.durationMs.coerceAtLeast(1L)
        val target = (duration * progress.coerceIn(0f, 1f)).toLong()
        player.seekTo(target)
        queueEngine.updatePosition(target)
        _state.update { it.copy(positionMs = target) }
    }

    fun seekBy(deltaMs: Long) {
        if (deltaMs == 0L) return
        val currentPosition = player.positionMs.coerceAtLeast(0L)
        val duration = player.durationMs.takeIf { it > 0L } ?: _state.value.durationMs
        val unboundedTarget = (currentPosition + deltaMs).coerceAtLeast(0L)
        val target = if (duration > 0L) unboundedTarget.coerceAtMost(duration) else unboundedTarget
        player.seekTo(target)
        queueEngine.updatePosition(target)
        _state.update { it.copy(positionMs = target) }
    }

    private suspend fun loadFallbackHome(
        languageCode: String,
        requestGeneration: Long,
        deferUntilHomeIdle: Boolean
    ) {
        val tasteIds = preferences.tastes()
        val queries = withContext(Dispatchers.Default) {
            (LevyraLocalizedDiscovery.homeBoostQueries(languageCode, tasteIds) +
                moodEngine.queriesForTastes(tasteIds, languageCode))
                .distinct()
                .take(12)
        }
        val tracks = try {
            repository.home(queries, languageCode)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.w(error, "Fallback home refresh failed")
            emptyList()
        }
        if (
            !currentCoroutineContext().isActive ||
            homeFeedRequestGeneration.get() != requestGeneration ||
            _state.value.languageCode != languageCode
        ) return

        if (tracks.isEmpty()) {
            _state.update { current ->
                if (current.languageCode != languageCode) current
                else current.copy(
                    isSearching = false,
                    searchError = "Home remota vuota: prova una ricerca"
                )
            }
            return
        }

        val fallbackSection = com.luc4n3x.levyra.domain.HomeSection(
            LevyraContentLocales.forLanguage(languageCode).quickSectionTitle,
            tracks.take(20)
        )
        val shouldPublishSection = _state.value.homeSections.isEmpty()
        if (deferUntilHomeIdle && shouldPublishSection) awaitHomeUiIdle()
        if (
            !currentCoroutineContext().isActive ||
            homeFeedRequestGeneration.get() != requestGeneration ||
            _state.value.languageCode != languageCode
        ) return

        _state.update { current ->
            if (current.languageCode != languageCode) current
            else current.copy(
                homeSections = if (shouldPublishSection) listOf(fallbackSection) else current.homeSections,
                tracks = tracks,
                searchResults = tracks.take(12),
                isSearching = false,
                smartScore = calculateSmartScore(moodEngine.buildQueue(current.selectedMood, tracks)),
                cacheReport = repository.cacheReport(),
                searchError = null
            )
        }
        withContext(Dispatchers.IO) {
            preferences.saveHomeSections(listOf(fallbackSection), languageCode)
        }
        persistHomeSnapshot()
        val startupPlan = homeStartupWorkPlan()
        viewModelScope.launch(Dispatchers.IO) {
            if (deferUntilHomeIdle) awaitHomeUiIdle(startupPlan)
            LevyraArtworkCache.preloadHome(
                getApplication<Application>().applicationContext,
                tracks,
                startupPlan.refreshedArtworkCount
            )
        }
        prefetchTop(tracks, startupPlan.refreshedArtworkCount, respectHomeScroll = deferUntilHomeIdle)
    }

    private fun searchMood(mood: Mood) {
        val query = moodEngine.tagQueryFor(mood, _state.value.languageCode)
        _state.update { it.copy(query = query) }
        searchNow(query)
    }

    private fun saveLastPlaybackAsync(track: Track?, positionMs: Long) {
        if (lastPlaybackSaveJob?.isActive == true) return
        val stableTrack = track?.copy(streamUrl = "", videoStreamUrl = "")
        lastPlaybackSaveJob = viewModelScope.launch(Dispatchers.IO) {
            preferences.saveLastPlayback(stableTrack, positionMs)
        }
    }

    private fun startTicker() {
        viewModelScope.launch {
            var lastBackgroundUiUpdateMs = 0L
            var lastQueueUpdateMs = 0L
            var lastPlaybackSaveMs = 0L
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

                val nowElapsed = android.os.SystemClock.elapsedRealtime()
                if (listenSessionTrack != null && player.isPlaying && listenTickElapsedMs > 0L) {
                    val delta = nowElapsed - listenTickElapsedMs
                    if (delta in 1..2_000L) listenSessionAccumulatedMs += delta
                    maybePersistListenSession()
                }
                listenTickElapsedMs = nowElapsed

                val position = if (!player.isPlaying && player.positionMs == 0L && pendingSeekMs > 0L) {
                    pendingSeekMs
                } else {
                    player.positionMs
                }
                val active = lyricsEngine.currentLine(position, snapshot.lyrics)
                val playbackStateChanged = snapshot.isPlaying != player.isPlaying
                val shouldPublishUi = snapshot.selectedTab == LevyraTab.Player ||
                    snapshot.showLyrics ||
                    nowElapsed - lastBackgroundUiUpdateMs >= 1_000L ||
                    playbackStateChanged ||
                    snapshot.durationMs != duration
                if (shouldPublishUi) {
                    _state.update {
                        it.copy(
                            positionMs = position,
                            durationMs = duration,
                            isPlaying = player.isPlaying,
                            activeLyric = active
                        )
                    }
                    lastBackgroundUiUpdateMs = nowElapsed
                }

                if (current != null && nowElapsed - lastQueueUpdateMs >= 1_000L) {
                    queueEngine.updatePosition(position)
                    lastQueueUpdateMs = nowElapsed
                }
                if (current != null && player.isPlaying && nowElapsed - lastPlaybackSaveMs >= 4_000L) {
                    saveLastPlaybackAsync(current, position)
                    lastPlaybackSaveMs = nowElapsed
                }
                if (playbackStateChanged) {
                    updateWidget()
                }
                delay(if (snapshot.showLyrics) 50L else 500L)
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
        return queueEngine.next(respectRepeatOne = false)
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
        val candidates = searchPlayableCandidates(track)
        if (candidates.isEmpty()) throw IllegalStateException("Nessun risultato YouTube per ${track.title}")
        val errors = mutableListOf<String>()
        for (candidate in candidates) {
            val carried = LevyraPersonalOrbit.preferAlbumArtwork(candidate, track)
            val resolved = runCatching { resolvePlayableTrack(carried) }
            resolved.onSuccess { return it }
            resolved.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }?.let { errors += "${candidate.title} - ${candidate.artist}: $it" }
            resolver.invalidate(carried, _state.value.isVideoMode)
        }
        val reason = errors.firstOrNull() ?: "YouTube non ha fornito uno stream audio riproducibile per ${track.title}"
        throw IllegalStateException(reason)
    }

    private suspend fun searchPlayableCandidates(track: Track): List<Track> {
        val queries = listOf(
            "${track.title} ${track.artist}",
            "${track.title} ${track.artist} official audio",
            "${track.title} ${track.artist} official video",
            "${track.title} ${track.artist} visual video",
            "${track.title} ${track.artist} topic"
        ).map { it.trim() }.filter { it.length >= 2 }.distinct()
        val candidates = LinkedHashMap<String, Track>()
        for (query in queries) {
            repository.search(query, 8, _state.value.languageCode).forEach { candidate ->
                if (candidate.id.isNotBlank() && !candidates.containsKey(candidate.id)) candidates[candidate.id] = candidate
            }
            if (candidates.size >= 10) break
        }
        return candidates.values
            .filter { isPlaybackCandidateCompatible(track, it) }
            .sortedByDescending { playbackCandidateScore(track, it) }
            .take(10)
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

    private fun instantAlbumRecommendations(state: LevyraUiState, limit: Int = 10): List<AlbumHit> {
        return instantAlbumRecommendationsFromTracks(
            primary = listOfNotNull(state.currentTrack) + state.recentListens + state.favorites + state.recentSearches,
            secondary = state.personalOrbitTracks + state.tracks + state.homeSections.flatMap { it.tracks } + state.charts,
            limit = limit,
            profile = state.smartProfile
        )
    }

    private fun instantAlbumRecommendationsFromTracks(
        primary: List<Track>,
        secondary: List<Track>,
        limit: Int,
        profile: SmartMusicProfile = _state.value.smartProfile
    ): List<AlbumHit> {
        val primaryCandidates = mergeTracks(emptyList(), primary).filter(::isInstantAlbumCandidate)
        val candidates = if (primaryCandidates.isNotEmpty()) primaryCandidates else {
            mergeTracks(emptyList(), secondary).filter(::isInstantAlbumCandidate)
        }
        return candidates
            .asSequence()
            .sortedWith(compareByDescending<Track> { smartAlbumScore(it.album, it.artist, profile) }.thenByDescending { it.replayScore + it.cacheScore })
            .distinctBy { "${it.album.trim().lowercase()}|${it.artist.trim().lowercase()}" }
            .map { track ->
                val albumTitle = track.album.trim()
                val artistName = track.artist.trim()
                AlbumHit(
                    title = albumTitle,
                    artist = artistName,
                    year = "",
                    thumbnailUrl = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl },
                    query = "$albumTitle $artistName album",
                    browseId = ""
                )
            }
            .take(limit)
            .toList()
    }

    private fun isInstantAlbumCandidate(track: Track): Boolean {
        val title = track.title.trim()
        val album = track.album.trim()
        val artist = track.artist.trim()
        if (album.length < 2 || artist.length < 2) return false
        if (album.equals(title, ignoreCase = true)) return false
        if (album.equals("YouTube", ignoreCase = true) || album.equals("YouTube Music", ignoreCase = true)) return false
        if (artist.equals("YouTube", ignoreCase = true) || artist.equals("YouTube Music", ignoreCase = true)) return false
        val lowerAlbum = album.lowercase()
        if (lowerAlbum.contains("single") || lowerAlbum.contains("singolo") || lowerAlbum == "ep" || lowerAlbum.endsWith(" ep") || lowerAlbum.contains(" ep ")) return false
        val art = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }.trim()
        if (art.isBlank()) return false
        val lowerArt = art.lowercase()
        val looksLikeVideoFrame = lowerArt.contains("/vi/") ||
            lowerArt.contains("/vi_webp/") ||
            lowerArt.contains("ytimg.com/an_webp") ||
            lowerArt.contains("hqdefault") ||
            lowerArt.contains("mqdefault") ||
            lowerArt.contains("sddefault") ||
            lowerArt.contains("maxresdefault") ||
            lowerArt.contains("hq720") ||
            lowerArt.endsWith("default.jpg") ||
            lowerArt.endsWith("default.webp")
        return !looksLikeVideoFrame
    }

    private fun mergeAlbums(primary: List<AlbumHit>, secondary: List<AlbumHit>): List<AlbumHit> {
        val map = LinkedHashMap<String, AlbumHit>()
        (primary + secondary).forEach { album ->
            val key = "${album.title.trim().lowercase()}|${album.artist.trim().lowercase()}"
            if (album.title.isNotBlank() && album.artist.isNotBlank() && album.thumbnailUrl.isNotBlank() && !map.containsKey(key)) {
                map[key] = album
            }
        }
        return map.values.toList()
    }


    private fun beginListenSession(track: Track) {
        flushListenSession()
        listenSessionTrack = track.copy(streamUrl = "", videoStreamUrl = "")
        listenSessionStartedAt = System.currentTimeMillis()
        listenSessionAccumulatedMs = 0L
        listenSessionCompleted = false
        listenTickElapsedMs = android.os.SystemClock.elapsedRealtime()
        listenSessionPersistedMs = 0L
    }

    private data class ListenSession(
        val track: Track,
        val startedAt: Long,
        val listenedMs: Long,
        val completed: Boolean
    )

    private fun takeListenSession(): ListenSession? {
        val track = listenSessionTrack ?: return null
        val session = ListenSession(track, listenSessionStartedAt, listenSessionAccumulatedMs, listenSessionCompleted)
        listenSessionTrack = null
        listenSessionAccumulatedMs = 0L
        listenSessionCompleted = false
        listenSessionPersistedMs = 0L
        return session.takeIf { it.listenedMs >= ListeningPulseEngine.MIN_LISTEN_MS }
    }

    private fun flushListenSession() {
        val session = takeListenSession() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            listeningPulseStore.record(session.track, session.listenedMs, session.completed, session.startedAt)
            refreshListeningPulse()
        }
    }

    private fun flushListenSessionBlocking() {
        val session = takeListenSession() ?: return
        listeningPulseStore.recordSync(session.track, session.listenedMs, session.completed, session.startedAt)
    }

    private fun maybePersistListenSession() {
        val track = listenSessionTrack ?: return
        val listenedMs = listenSessionAccumulatedMs
        if (listenedMs < ListeningPulseEngine.MIN_LISTEN_MS) return
        if (listenedMs - listenSessionPersistedMs < LISTEN_SESSION_FLUSH_INTERVAL_MS) return
        if (listenSessionPersistJob?.isActive == true) return
        val startedAt = listenSessionStartedAt
        listenSessionPersistJob = viewModelScope.launch(Dispatchers.IO) {
            listeningPulseStore.record(track, listenedMs, completed = false, startedAt = startedAt)
            if (listenSessionStartedAt == startedAt && listenSessionTrack?.id == track.id) {
                listenSessionPersistedMs = maxOf(listenSessionPersistedMs, listenedMs)
            }
            refreshListeningPulse()
        }
    }

    private fun refreshListeningPulse(force: Boolean = false) {
        if (listeningPulseRefreshJob?.isActive == true) return
        val now = android.os.SystemClock.elapsedRealtime()
        val waitMs = if (force) 0L else (PULSE_REFRESH_THROTTLE_MS - (now - lastListeningPulseRefreshMs)).coerceAtLeast(0L)
        listeningPulseRefreshJob = viewModelScope.launch(Dispatchers.IO) {
            if (waitMs > 0L) delay(waitMs)
            val events = listeningPulseStore.eventsWindow()
            val recent = listeningPulseStore.recentTracks()
            val pulse = listeningPulseEngine.build(events)
            lastListeningPulseRefreshMs = android.os.SystemClock.elapsedRealtime()
            _state.update { current ->
                val updated = current.copy(listeningPulse = pulse, recentListens = recent)
                val localAlbums = instantAlbumRecommendations(updated, HOME_ALBUM_RECOMMENDATION_LIMIT)
                val rankedAlbums = rankAlbumRecommendations(
                    remote = current.homeAlbums,
                    instant = localAlbums,
                    state = updated,
                    limit = HOME_ALBUM_RECOMMENDATION_LIMIT
                )
                val hasPersonalSignals = albumRecommendationSeeds(updated).any { it.artist.isNotBlank() || it.album.isNotBlank() }
                updated.copy(
                    homeAlbums = when {
                        rankedAlbums.isNotEmpty() -> rankedAlbums
                        hasPersonalSignals -> emptyList()
                        else -> current.homeAlbums
                    }
                )
            }
        }
    }

    private fun recordSmartPlayback(track: Track) {
        recordSmartProfile { smartMusicProfileStore.recordPlayback(track.copy(streamUrl = "", videoStreamUrl = "")) }
    }

    private fun recordSmartCompletion(track: Track) {
        recordSmartProfile { smartMusicProfileStore.recordCompletion(track.copy(streamUrl = "", videoStreamUrl = "")) }
    }

    private fun recordSmartFavorite(track: Track, added: Boolean) {
        recordSmartProfile { smartMusicProfileStore.recordFavorite(track.copy(streamUrl = "", videoStreamUrl = ""), added) }
    }

    private fun recordSmartDownload(track: Track) {
        recordSmartProfile { smartMusicProfileStore.recordDownload(track.copy(streamUrl = "", videoStreamUrl = "")) }
    }

    private fun recordSmartAlbumOpen(album: AlbumHit) {
        recordSmartProfile { smartMusicProfileStore.recordAlbumOpen(album) }
    }

    private fun recordSmartProfile(block: () -> SmartMusicProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = runCatching { block() }.getOrNull() ?: return@launch
            _state.update { current ->
                val personalizedState = current.copy(smartProfile = profile)
                val localAlbums = instantAlbumRecommendations(personalizedState, HOME_ALBUM_RECOMMENDATION_LIMIT)
                val boostedAlbums = rankAlbumRecommendations(
                    remote = current.homeAlbums,
                    instant = localAlbums,
                    state = personalizedState,
                    limit = HOME_ALBUM_RECOMMENDATION_LIMIT
                )
                val hasPersonalSignals = albumRecommendationSeeds(personalizedState).any { it.artist.isNotBlank() || it.album.isNotBlank() }
                current.copy(
                    smartProfile = profile,
                    homeAlbums = when {
                        boostedAlbums.isNotEmpty() -> boostedAlbums
                        hasPersonalSignals -> emptyList()
                        else -> current.homeAlbums
                    }
                )
            }
        }
    }

    private fun smartAlbumScore(albumTitle: String, artistName: String, profile: SmartMusicProfile): Int {
        val album = albumRecommendationTextKey(albumTitle)
        val artist = albumRecommendationTextKey(artistName)
        val albumScore = profile.topAlbums.firstOrNull { seed ->
            val label = albumRecommendationTextKey(seed.label)
            album.isNotBlank() && label.contains(album) && (artist.isBlank() || label.contains(artist))
        }?.weight ?: 0
        val artistScore = profile.topArtists.firstOrNull { seed -> albumRecommendationTextKey(seed.label) == artist }?.weight ?: 0
        return albumScore * 3 + artistScore * 2
    }

    private fun mergeTracks(old: List<Track>, incoming: List<Track>): List<Track> {
        val map = LinkedHashMap<String, Track>()
        old.forEach { map[it.id] = it }
        incoming.forEach { map[it.id] = it }
        return map.values.toList()
    }

    private companion object {
        private const val HOME_ARTIST_SHELF_SIZE = 13
        private const val HOME_ARTIST_HISTORY_LIMIT = 48
        private const val HOME_ARTIST_CANDIDATE_LIMIT = 48
        private const val HOME_ARTIST_RESOLUTION_CONCURRENCY = 6
        private const val HOME_ARTIST_RELATED_SEED_LIMIT = 3
        private const val HOME_ARTIST_FAST_TIMEOUT_MS = 1_900L
        private const val HOME_ARTIST_RELATED_TIMEOUT_MS = 2_600L
        private const val HOME_ALBUM_RECOMMENDATION_LIMIT = 10
        private const val HOME_ALBUM_REMOTE_CANDIDATE_LIMIT = 24
        private const val HOME_ALBUM_SEED_LIMIT = 12
        private const val OFFICIAL_METADATA_MAX_BATCH_SIZE = 8
        private const val OFFICIAL_METADATA_CONCURRENCY = 2
        const val LISTEN_SESSION_FLUSH_INTERVAL_MS = 30_000L
        const val PULSE_REFRESH_THROTTLE_MS = 5_000L
    }

    override fun onCleared() {
        LevyraWidgetBridge.clear()
        _state.value.currentTrack?.let { preferences.saveLastPlayback(it, player.positionMs) }
        flushListenSessionBlocking()
        playJob?.cancel()
        modeSwitchJob?.cancel()
        streamRecoveryJob?.cancel()
        alternateModePrefetchJob?.cancel()
        cancelBackgroundWarmups()
        orbitArtworkJob?.cancel()
        officialMetadataSignal.close()
        chartEnrichJob?.cancel()
        sleepJob?.cancel()
        lyricsJob?.cancel()
        lyricsPrefetchJob?.cancel()
        sponsorJob?.cancel()
        artistJob?.cancel()
        radarJob?.cancel()
        radioJob?.cancel()
        crossfadeJob?.cancel()
        queueEngine.updatePosition(player.positionMs)
        player.release()
        searchJob?.cancel()
        homeFeedJob?.cancel()
        homeAlbumsJob?.cancel()
        homeArtistsJob?.cancel()
        chartsJob?.cancel()
        homeSnapshotJob?.cancel()
        super.onCleared()
    }
}

internal fun playbackIdentity(track: Track): String = youtubePlayableTrack(track)?.id?.takeIf { it.isNotBlank() }
    ?: track.id.ifBlank { track.videoUrl.ifBlank { "${track.artist}|${track.title}" } }.trim().lowercase()

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

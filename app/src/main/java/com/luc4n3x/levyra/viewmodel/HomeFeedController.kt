package com.luc4n3x.levyra.viewmodel

import android.app.Application
import com.luc4n3x.levyra.data.ArtistRepository
import com.luc4n3x.levyra.data.HomeInteractionGate
import com.luc4n3x.levyra.data.HomeOfflinePolicy
import com.luc4n3x.levyra.data.HomeRefreshStability
import com.luc4n3x.levyra.data.HomeSectionMergeResult
import com.luc4n3x.levyra.data.HomeStartupWorkPlan
import com.luc4n3x.levyra.data.HomeStartupWorkPolicy
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.data.LevyraHomeSnapshotCache
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.LevyraStartupCatalog
import com.luc4n3x.levyra.data.ListeningPulseStore
import com.luc4n3x.levyra.data.YoutubeCommentsRepository
import com.luc4n3x.levyra.data.YoutubeCommentsResult
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.apple.AppleMusicEditorialRepository
import com.luc4n3x.levyra.data.apple.AppleStorefronts
import com.luc4n3x.levyra.data.network.LevyraNetworkIntelligence
import com.luc4n3x.levyra.data.runCatchingPreservingCancellation
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.AlbumRecommendationSeed
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LevyraLocalizedDiscovery
import com.luc4n3x.levyra.domain.LevyraMusicRegion
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.MoodEngine
import com.luc4n3x.levyra.domain.ResonanceCommentSnippet
import com.luc4n3x.levyra.domain.SmartMusicProfile
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.artistIdentityKey
import com.luc4n3x.levyra.domain.isArtistShelfNameEligible
import com.luc4n3x.levyra.domain.primaryArtistSegment
import com.luc4n3x.levyra.domain.resonanceCommentsForTracks
import com.luc4n3x.levyra.player.AdaptivePlaybackPolicy
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

private const val APPLE_TRENDING_SEARCH_LIMIT = 8
internal const val HOME_ARTIST_SHELF_SIZE = 20
internal const val HOME_ALBUM_RECOMMENDATION_LIMIT = 20
private const val HOME_CHART_ARTIST_MIN = 8
private const val HOME_ARTIST_HISTORY_LIMIT = 72
private const val HOME_ARTIST_CANDIDATE_LIMIT = 72
private const val HOME_ARTIST_RESOLUTION_CONCURRENCY = 4
private const val HOME_ARTIST_FAST_TIMEOUT_MS = 5_200L
private const val HOME_ARTIST_ALTERNATIVE_AUDIENCE_TIMEOUT_MS = 9_000L
private const val HOME_ARTIST_TOTAL_TIMEOUT_MS = 18_000L
private const val HOME_ARTIST_STARTUP_GRACE_MS = 850L
private const val HOME_STARTUP_STREAM_PREFETCH_COUNT = 2
private const val HOME_STARTUP_METADATA_REFRESH_COUNT = 4
private const val HOME_RESONANCE_REFRESH_INTERVAL_MS = 12L * 60L * 60L * 1000L
private const val HOME_RESONANCE_COMMENTS_TTL_MS = 6L * 60L * 60L * 1000L
private const val HOME_ALBUM_REMOTE_CANDIDATE_LIMIT = 32
private const val HOME_ALBUM_REMOTE_CONCURRENCY = 4
private val GLOBAL_HOME_ARTIST_FALLBACKS = listOf(
    "The Weeknd", "Drake", "Taylor Swift", "Billie Eilish", "SZA",
    "Travis Scott", "Dua Lipa", "Post Malone", "Ariana Grande", "Kendrick Lamar",
    "Bruno Mars", "Beyoncé", "Rihanna", "Ed Sheeran", "Lady Gaga",
    "Bad Bunny", "Doja Cat", "Coldplay", "Imagine Dragons", "Lana Del Rey",
    "Olivia Rodrigo", "Sabrina Carpenter", "Miley Cyrus", "Harry Styles"
)

private data class HomeArtistCandidate(
    val name: String,
    val browseId: String
)

private data class HomeArtistPlan(
    val languageCode: String,
    val orderedCandidates: List<HomeArtistCandidate>,
    val trustedArtistKeys: Set<String>,
    val blockedArtistKeys: Set<String>
)

internal interface HomeFeedHost {
    fun applyDeviceOfflineState(offline: Boolean)

    fun ensureMusicVideosLoaded()

    fun ensureExploreVideosLoaded()

    fun loadCharts(regionId: String)

    fun prefetchTop(tracks: List<Track>, count: Int, respectHomeScroll: Boolean)

    fun refreshOfficialMetadataBatch(tracks: List<Track>, limit: Int, deferUntilHomeIdle: Boolean)

    fun albumRecommendationSeeds(state: LevyraUiState): List<AlbumRecommendationSeed>

    fun instantAlbumRecommendations(state: LevyraUiState): List<AlbumHit>

    fun instantAlbumRecommendationsFromTracks(
        primary: List<Track>,
        secondary: List<Track>,
        limit: Int,
        profile: SmartMusicProfile
    ): List<AlbumHit>

    fun rankAlbumRecommendations(
        remote: List<AlbumHit>,
        instant: List<AlbumHit>,
        state: LevyraUiState,
        limit: Int
    ): List<AlbumHit>

    fun calculateSmartScore(queue: List<Track>): Int
}

internal class HomeFeedController(
    private val scope: CoroutineScope,
    private val application: Application,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val preferences: LevyraPreferences,
    private val repository: YoutubeMusicRepository,
    private val artistRepository: ArtistRepository,
    private val listeningPulseStore: ListeningPulseStore,
    private val youtubeCommentsRepository: YoutubeCommentsRepository,
    private val homeSnapshotCache: LevyraHomeSnapshotCache,
    private val moodEngine: MoodEngine,
    private val adaptivePlaybackPolicy: AdaptivePlaybackPolicy,
    private val host: HomeFeedHost
) {
    private var appleTrendingJob: Job? = null
    var homeFeedJob: Job? = null
        private set
    var homeAlbumsJob: Job? = null
        private set
    var homeArtistsJob: Job? = null
        private set
    var homeSnapshotJob: Job? = null
        private set
    var homeResonanceJob: Job? = null
        private set
    private var homeArtistsFingerprint: String = ""
    private val deferredHomeArtistsSnapshot = AtomicReference<List<ArtistHit>?>(null)
    private val homeInteractionGate = HomeInteractionGate()
    private val homeFeedRequestGeneration = AtomicLong(0L)
    private val homeAlbumsRequestGeneration = AtomicLong(0L)
    var homeResonanceCommentsJob: Job? = null
        private set
    private var homeResonanceCommentsRequestIds: List<String> = emptyList()
    private val homeResonanceCommentsGeneration = AtomicLong(0L)
    private val pendingHomeSectionsSnapshot = AtomicReference<List<com.luc4n3x.levyra.domain.HomeSection>?>(null)
    private val deferredHomeSnapshotApplyScheduled = AtomicBoolean(false)
    @Volatile private var homeScreenActive = false
    @Volatile private var homeAtTop = true
    @Volatile private var homeScrollInProgress = false

    fun resetForLanguageChange() {
        pendingHomeSectionsSnapshot.set(null)
        deferredHomeArtistsSnapshot.set(null)
        homeArtistsFingerprint = ""
        homeArtistsJob?.cancel()
    }

    fun cancelResonanceComments() {
        homeResonanceCommentsJob?.cancel()
        homeResonanceCommentsJob = null
        homeResonanceCommentsRequestIds = emptyList()
        homeResonanceCommentsGeneration.incrementAndGet()
    }

    fun homeStartupWorkPlan(): HomeStartupWorkPlan {
        val playbackPlan = adaptivePlaybackPolicy.current(videoMode = false)
        return HomeStartupWorkPolicy.create(
            lowRam = playbackPlan.lowRam,
            powerConstrained = playbackPlan.powerConstrained
        )
    }

    fun setHomeViewport(scrollInProgress: Boolean, atTop: Boolean) {
        homeAtTop = atTop
        homeScrollInProgress = scrollInProgress
        homeInteractionGate.update(scrollInProgress)
        if (homeScreenActive && canApplyHomeStructuralChanges()) {
            scheduleDeferredHomeSnapshotApply()
        }
    }

    fun onHomeEntered(atTop: Boolean) {
        homeScreenActive = true
        homeAtTop = atTop
        homeScrollInProgress = false
        homeInteractionGate.update(false)
        if (canApplyHomeStructuralChanges()) {
            scheduleDeferredHomeSnapshotApply(waitForIdle = false)
        }
    }

    fun onHomeLeft() {
        homeScreenActive = false
        homeAtTop = true
        homeScrollInProgress = false
        homeInteractionGate.update(false)
    }

    private fun canApplyHomeStructuralChanges(): Boolean {
        return !homeScreenActive || !shouldFreezeHomeStructure(homeScrollInProgress, homeAtTop)
    }

    private fun hasDeferredHomeSnapshots(): Boolean {
        return pendingHomeSectionsSnapshot.get() != null || deferredHomeArtistsSnapshot.get() != null
    }

    private fun scheduleDeferredHomeSnapshotApply(waitForIdle: Boolean = true) {
        if (!homeScreenActive || !canApplyHomeStructuralChanges() || !hasDeferredHomeSnapshots()) return
        if (!deferredHomeSnapshotApplyScheduled.compareAndSet(false, true)) return
        scope.launch {
            try {
                if (waitForIdle) awaitHomeUiIdle()
                if (homeScreenActive && canApplyHomeStructuralChanges()) {
                    applyDeferredHomeSnapshots()
                }
            } finally {
                deferredHomeSnapshotApplyScheduled.set(false)
                if (homeScreenActive && canApplyHomeStructuralChanges() && hasDeferredHomeSnapshots()) {
                    scheduleDeferredHomeSnapshotApply()
                }
            }
        }
    }

    private suspend fun applyDeferredHomeSnapshots() {
        val pendingSections = pendingHomeSectionsSnapshot.getAndSet(null)
        val pendingArtists = deferredHomeArtistsSnapshot.getAndSet(null)
        if (pendingSections == null && pendingArtists == null) return
        if (!homeScreenActive || !canApplyHomeStructuralChanges()) {
            pendingSections?.let { pendingHomeSectionsSnapshot.compareAndSet(null, it) }
            pendingArtists?.let { deferredHomeArtistsSnapshot.compareAndSet(null, it) }
            return
        }

        var applied = false
        while (true) {
            val current = uiState.value
            val updated = withContext(Dispatchers.Default) {
                val sectionResult = pendingSections?.let { sections ->
                    HomeRefreshStability.mergeSections(
                        previous = current.homeSections,
                        incoming = sections,
                        allowStructuralChanges = true
                    )
                }
                val nextSections = sectionResult?.visible ?: current.homeSections
                val nextArtists = pendingArtists?.takeIf { it.isNotEmpty() } ?: current.homeArtists
                val sectionsChanged = sectionResult?.changed == true
                val artistsChanged = nextArtists != current.homeArtists
                if (!sectionsChanged && !artistsChanged) {
                    current
                } else {
                    val nextTracks = if (sectionsChanged) {
                        nextSections.flatMap { it.tracks }.distinctBy { it.id }.ifEmpty { current.tracks }
                    } else {
                        current.tracks
                    }
                    current.copy(
                        homeSections = nextSections,
                        homeArtists = nextArtists,
                        homeArtistsLoading = if (pendingArtists != null) false else current.homeArtistsLoading,
                        tracks = nextTracks
                    )
                }
            }
            if (updated === current) break
            if (!homeScreenActive || !canApplyHomeStructuralChanges()) {
                pendingSections?.let { pendingHomeSectionsSnapshot.compareAndSet(null, it) }
                pendingArtists?.let { deferredHomeArtistsSnapshot.compareAndSet(null, it) }
                return
            }
            if (uiState.compareAndSet(current, updated)) {
                applied = true
                break
            }
        }
        if (applied) persistHomeSnapshot()
    }

    suspend fun awaitHomeUiIdle(plan: HomeStartupWorkPlan = homeStartupWorkPlan()) {
        homeInteractionGate.awaitIdle(plan.idleWindowMs)
    }

    fun persistHomeSnapshot() {
        val languageCode = uiState.value.languageCode
        val musicRegionId = LevyraMusicRegion.selectedId
        homeSnapshotJob?.cancel()
        homeSnapshotJob = scope.launch(Dispatchers.IO) {
            delay(280L)
            if (uiState.value.languageCode == languageCode) persistHomeSnapshotSync(languageCode, musicRegionId)
        }
    }

    private fun persistHomeSnapshotSync(languageCode: String, musicRegionId: String = LevyraMusicRegion.selectedId) {
        val normalizedLanguage = LevyraLanguageCatalog.normalize(languageCode)
        val snapshot = uiState.value
        if (snapshot.languageCode != normalizedLanguage || LevyraMusicRegion.selectedId != musicRegionId) return
        homeSnapshotCache.save(
            musicRegionId = musicRegionId,
            languageCode = normalizedLanguage,
            homeSections = pendingHomeSectionsSnapshot.get() ?: snapshot.homeSections,
            chartRegionId = snapshot.selectedChartId,
            charts = snapshot.charts,
            personalOrbit = snapshot.personalOrbitTracks,
            homeArtists = deferredHomeArtistsSnapshot.get() ?: snapshot.homeArtists,
            resonanceTracks = snapshot.homeResonanceTracks,
            resonanceUpdatedAt = snapshot.homeResonanceUpdatedAt,
            resonanceComments = snapshot.homeResonanceComments
        )
    }

    @Synchronized
    fun refreshHomeResonanceComments(tracks: List<Track> = uiState.value.homeResonanceTracks) {
        val snapshot = uiState.value
        if (!snapshot.interfaceSettings.showResonance) {
            homeResonanceCommentsJob?.cancel()
            homeResonanceCommentsJob = null
            homeResonanceCommentsRequestIds = emptyList()
            homeResonanceCommentsGeneration.incrementAndGet()
            return
        }
        val requestIds = homeResonanceCommentVideoIds(tracks)
        if (requestIds != homeResonanceCommentVideoIds(snapshot.homeResonanceTracks)) return

        val retainedComments = resonanceCommentsForTracks(tracks, snapshot.homeResonanceComments)
        if (retainedComments != snapshot.homeResonanceComments) {
            uiState.update { state ->
                if (requestIds != homeResonanceCommentVideoIds(state.homeResonanceTracks)) state else state.copy(
                    homeResonanceComments = resonanceCommentsForTracks(
                        state.homeResonanceTracks,
                        state.homeResonanceComments
                    )
                )
            }
            persistHomeSnapshot()
        }

        if (homeResonanceCommentsJob?.isActive == true && homeResonanceCommentsRequestIds == requestIds) return
        val generation = homeResonanceCommentsGeneration.incrementAndGet()
        homeResonanceCommentsRequestIds = requestIds
        val candidates = homeResonanceCommentsToRefresh(
            tracks = tracks,
            comments = retainedComments,
            nowMs = System.currentTimeMillis(),
            ttlMs = HOME_RESONANCE_COMMENTS_TTL_MS
        )
        if (candidates.isEmpty()) {
            homeResonanceCommentsJob = null
            return
        }

        homeResonanceCommentsJob = scope.replaceHomeResonanceCommentsJob(homeResonanceCommentsJob) {
            candidates.forEach { track ->
                if (!isActive || !isHomeResonanceCommentsRequestCurrent(requestIds, generation)) {
                    return@replaceHomeResonanceCommentsJob
                }
                val cached = retainedComments[track.id]

                uiState.update { state ->
                    if (generation != homeResonanceCommentsGeneration.get()) state else state.withHomeResonanceComment(
                        requestIds,
                        track.id
                    ) { current ->
                        (current ?: ResonanceCommentSnippet(videoId = track.id)).copy(
                            isLoading = current?.hasComment != true && current?.disabled != true,
                            hasError = false
                        )
                    }
                }

                val result = try {
                    youtubeCommentsRepository.initial(track.id, forceRefresh = false)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    YoutubeCommentsResult.Failed(e)
                }

                if (!isActive || !isHomeResonanceCommentsRequestCurrent(requestIds, generation)) {
                    return@replaceHomeResonanceCommentsJob
                }
                val snippet = when (result) {
                    is YoutubeCommentsResult.Available -> {
                        val top = result.page.items.firstOrNull()
                        ResonanceCommentSnippet(
                            videoId = track.id,
                            countText = result.page.countText,
                            author = top?.author.orEmpty(),
                            authorAvatarUrl = top?.authorAvatarUrl.orEmpty(),
                            text = top?.text.orEmpty(),
                            likeCountText = top?.likeCountText.orEmpty(),
                            isLoading = false,
                            disabled = result.page.commentsDisabled,
                            updatedAtMs = System.currentTimeMillis()
                        )
                    }
                    YoutubeCommentsResult.Disabled -> {
                        ResonanceCommentSnippet(
                            videoId = track.id,
                            disabled = true,
                            isLoading = false,
                            updatedAtMs = System.currentTimeMillis()
                        )
                    }
                    is YoutubeCommentsResult.Failed -> {
                        (cached ?: ResonanceCommentSnippet(videoId = track.id)).copy(
                            hasError = true,
                            isLoading = false
                        )
                    }
                }

                uiState.update { state ->
                    if (generation != homeResonanceCommentsGeneration.get()) state else state.withHomeResonanceComment(
                        requestIds,
                        track.id
                    ) { snippet }
                }
            }
            if (isHomeResonanceCommentsRequestCurrent(requestIds, generation)) persistHomeSnapshot()
        }
    }

    private fun isHomeResonanceCommentsRequestCurrent(videoIds: List<String>, generation: Long): Boolean {
        return generation == homeResonanceCommentsGeneration.get() &&
            uiState.value.interfaceSettings.showResonance &&
            videoIds == homeResonanceCommentVideoIds(uiState.value.homeResonanceTracks)
    }

    fun refreshHomeResonanceIfStale() {
        val initial = uiState.value
        if (isHomeResonanceFresh(initial, System.currentTimeMillis())) {
            refreshHomeResonanceComments(initial.homeResonanceTracks)
            return
        }
        if (homeResonanceJob?.isActive == true) return
        val languageCode = initial.languageCode
        homeResonanceJob = scope.launch(Dispatchers.Default) {
            val source = uiState.value
            if (source.languageCode != languageCode) return@launch
            val resolved = buildHomeResonanceTracks(source)
            if (resolved.isEmpty()) return@launch
            val updatedAt = System.currentTimeMillis()
            var changed = false
            uiState.update { current ->
                if (current.languageCode != languageCode || isHomeResonanceFresh(current, updatedAt)) {
                    current
                } else {
                    changed = current.homeResonanceTracks != resolved || current.homeResonanceUpdatedAt != updatedAt
                    current.copy(
                        homeResonanceTracks = resolved,
                        homeResonanceUpdatedAt = updatedAt,
                        homeResonanceComments = resonanceCommentsForTracks(
                            resolved,
                            current.homeResonanceComments
                        )
                    )
                }
            }
            if (changed) persistHomeSnapshot()
            refreshHomeResonanceComments(resolved)
        }
    }

    private fun isHomeResonanceFresh(state: LevyraUiState, now: Long): Boolean {
        return state.homeResonanceTracks.isNotEmpty() &&
            state.homeResonanceUpdatedAt > 0L &&
            now - state.homeResonanceUpdatedAt < HOME_RESONANCE_REFRESH_INTERVAL_MS
    }

    fun refreshHomeArtists() {
        if (!HomeOfflinePolicy.shouldAttemptRemoteRefresh(uiState.value.isDeviceOffline)) {
            homeArtistsJob?.cancel()
            uiState.update { current ->
                if (current.homeArtistsLoading) current.copy(homeArtistsLoading = false) else current
            }
            return
        }

        val startupSnapshot = uiState.value
        homeArtistsJob?.cancel()
        homeArtistsJob = scope.launch(Dispatchers.IO) {
            val chartLanguage = LevyraLanguageCatalog.normalize(startupSnapshot.languageCode)
            val chartCountry = LevyraContentLocales.forLanguage(chartLanguage).chartCountry.uppercase(java.util.Locale.ROOT)
            val chartArtists = runCatchingPreservingCancellation {
                com.luc4n3x.levyra.data.YoutubeMusicChartsRepository(application).topArtists(chartCountry, chartLanguage)
            }.getOrNull().orEmpty().take(HOME_ARTIST_SHELF_SIZE)
            if (chartArtists.size >= HOME_CHART_ARTIST_MIN) {
                homeArtistsFingerprint = "chart|$chartLanguage|$chartCountry"
                val audienceGate = Semaphore(HOME_ARTIST_RESOLUTION_CONCURRENCY)
                val withListeners = coroutineScope {
                    chartArtists.map { artist ->
                        async {
                            val listeners = audienceGate.withPermit {
                                withTimeoutOrNull(HOME_ARTIST_FAST_TIMEOUT_MS) {
                                    runCatchingPreservingCancellation { artistRepository.monthlyListeners(artist.browseId) }.getOrNull()
                                }
                            }
                            val applePortrait = audienceGate.withPermit {
                                withTimeoutOrNull(HOME_ARTIST_FAST_TIMEOUT_MS) {
                                    runCatchingPreservingCancellation {
                                        com.luc4n3x.levyra.data.AppleArtistArtworkRepository.get(application).resolveArtistPortrait(artist.name)
                                    }.getOrNull()
                                }
                            }.orEmpty()
                            val monthly = listeners.orEmpty().ifBlank {
                                withTimeoutOrNull(HOME_ARTIST_ALTERNATIVE_AUDIENCE_TIMEOUT_MS) {
                                    runCatchingPreservingCancellation { artistRepository.alternativeMonthlyListeners(artist.name) }.getOrNull()
                                }.orEmpty()
                            }
                            artist.copy(
                                thumbnailUrl = applePortrait.ifBlank { artist.thumbnailUrl },
                                subscribers = monthly.ifBlank { artist.subscribers },
                                audienceIsSubscribers = monthly.isBlank(),
                                officialArtwork = artist.thumbnailUrl.isNotBlank()
                            )
                        }
                    }.awaitAll()
                }
                uiState.update { current ->
                    if (current.languageCode == startupSnapshot.languageCode) current.copy(homeArtists = withListeners, homeArtistsLoading = false) else current
                }
                persistHomeSnapshotSync(chartLanguage)
                return@launch
            }
            val plan = buildHomeArtistPlan(startupSnapshot)
            val visibleArtists = visibleHomeArtists(startupSnapshot, plan)
            val fingerprint = homeArtistFingerprint(plan.languageCode, plan.orderedCandidates)

            if (fingerprint == homeArtistsFingerprint && visibleArtists.size >= HOME_ARTIST_SHELF_SIZE) {
                return@launch
            }
            homeArtistsFingerprint = fingerprint

            val freezeVisibleShelf = visibleArtists.size >= HOME_ARTIST_SHELF_SIZE
            val startupPlan = homeStartupWorkPlan()
            prepareHomeArtistResolution(
                languageCode = plan.languageCode,
                visibleArtists = visibleArtists,
                freezeVisibleShelf = freezeVisibleShelf,
                startupPlan = startupPlan
            )

            val resolved = resolveHomeArtistShelf(
                plan = plan,
                visibleArtists = visibleArtists,
                fingerprint = fingerprint,
                freezeVisibleShelf = freezeVisibleShelf
            )
            if (
                !isActive ||
                homeArtistsFingerprint != fingerprint ||
                uiState.value.languageCode != plan.languageCode
            ) {
                return@launch
            }

            val finalArtists = resolved.values.take(HOME_ARTIST_SHELF_SIZE)
            if (freezeVisibleShelf) {
                finishFrozenHomeArtistRefresh(plan.languageCode, finalArtists)
                return@launch
            }

            deferredHomeArtistsSnapshot.set(null)
            uiState.update { current ->
                if (current.languageCode == plan.languageCode) {
                    current.copy(
                        homeArtists = finalArtists,
                        homeArtistsLoading = false
                    )
                } else {
                    current
                }
            }
            persistHomeSnapshotSync(plan.languageCode)
        }
    }

    private suspend fun buildHomeArtistPlan(startupSnapshot: LevyraUiState): HomeArtistPlan {
        val languageCode = LevyraLanguageCatalog.normalize(startupSnapshot.languageCode)
        val localizedSeedNames = (
            LevyraContentLocales.artistSuggestions(languageCode) + GLOBAL_HOME_ARTIST_FALLBACKS
        ).distinctBy(::artistIdentityKey)
        val localizedSeedKeys = localizedSeedNames
            .map(::artistIdentityKey)
            .filter { it.isNotBlank() }
            .toSet()
        val blockedArtistKeys = if (languageCode == "it") {
            emptySet()
        } else {
            LevyraContentLocales.artistSuggestions("it")
                .map(::artistIdentityKey)
                .filter { it.isNotBlank() }
                .toSet()
        }

        val candidates = LinkedHashMap<String, HomeArtistCandidate>()
        val trustedArtistKeys = LinkedHashSet<String>()
        fun addCandidate(nameValue: String, browseIdValue: String) {
            val browseId = browseIdValue.trim()
            val cleanName = nameValue.trim()
            val name = if (LevyraContentLocales.isArtistSuggestionForLanguage(cleanName, languageCode)) {
                cleanName
            } else {
                primaryArtistSegment(cleanName).ifBlank { cleanName }
            }
            val identity = artistIdentityKey(name)
            if (name.length < 2 || identity.isBlank() || !isArtistShelfNameEligible(name)) return
            if (identity in blockedArtistKeys && identity !in localizedSeedKeys) return
            trustedArtistKeys += identity
            val existing = candidates[identity]
            if (existing == null || existing.browseId.isBlank() && browseId.isNotBlank()) {
                candidates[identity] = HomeArtistCandidate(name, browseId)
            }
        }

        localizedSeedNames.forEach { name -> addCandidate(name, "") }
        listeningPulseStore.personalizedArtists(limit = HOME_ARTIST_HISTORY_LIMIT)
            .filter { ranked ->
                LevyraContentLocales.isArtistSuggestionForLanguage(ranked.name, languageCode)
            }
            .forEach { ranked -> addCandidate(ranked.name, ranked.browseId) }

        homeArtistFeedTracks(startupSnapshot, languageCode).forEach { track ->
            addCandidate(track.artist, track.artistBrowseIds.firstOrNull().orEmpty())
        }
        if (languageCode != "en") {
            homeArtistPersonalTracks(startupSnapshot, languageCode).forEach { track ->
                addCandidate(track.artist, track.artistBrowseIds.firstOrNull().orEmpty())
            }
        }

        return HomeArtistPlan(
            languageCode = languageCode,
            orderedCandidates = candidates.values.take(HOME_ARTIST_CANDIDATE_LIMIT),
            trustedArtistKeys = trustedArtistKeys,
            blockedArtistKeys = blockedArtistKeys
        )
    }

    private fun homeArtistFeedTracks(
        state: LevyraUiState,
        languageCode: String
    ): Sequence<Track> {
        return buildList {
            state.homeSections.forEach { section -> addAll(section.tracks) }
            addAll(state.charts)
        }
            .asSequence()
            .filter(LevyraPersonalOrbit::isReliableMusicCandidate)
            .filter { track ->
                languageCode == "en" || LevyraPersonalOrbit.isLanguagePreferred(track, languageCode)
            }
            .distinctBy(LevyraPersonalOrbit::identityKey)
    }

    private fun homeArtistPersonalTracks(
        state: LevyraUiState,
        languageCode: String
    ): Sequence<Track> {
        return buildList {
            addAll(state.recentListens)
            addAll(state.personalOrbitTracks)
            addAll(state.favorites)
            state.currentTrack?.let(::add)
        }
            .asSequence()
            .filter(LevyraPersonalOrbit::isReliableMusicCandidate)
            .filter { track -> LevyraPersonalOrbit.isLanguagePreferred(track, languageCode) }
            .distinctBy(LevyraPersonalOrbit::identityKey)
    }

    private fun homeArtistFingerprint(
        languageCode: String,
        candidates: List<HomeArtistCandidate>
    ): String {
        return buildString {
            append(languageCode)
            append('|')
            append(candidates.joinToString("|") { candidate ->
                "${candidate.browseId.lowercase()}:${artistIdentityKey(candidate.name)}"
            })
        }
    }

    private fun visibleHomeArtists(
        startupSnapshot: LevyraUiState,
        plan: HomeArtistPlan
    ): List<ArtistHit> {
        return startupSnapshot.homeArtists
            .filter { hit ->
                isTrustedHomeArtistHit(
                    hit = hit,
                    trustedArtistKeys = plan.trustedArtistKeys,
                    blockedArtistKeys = plan.blockedArtistKeys
                )
            }
            .distinctBy { it.browseId.lowercase() }
            .take(HOME_ARTIST_SHELF_SIZE)
    }

    private fun isTrustedHomeArtistHit(
        hit: ArtistHit,
        trustedArtistKeys: Set<String>,
        blockedArtistKeys: Set<String>
    ): Boolean {
        val identity = artistIdentityKey(hit.name)
        return hit.name.isNotBlank() &&
            hit.thumbnailUrl.isNotBlank() &&
            hit.browseId.isNotBlank() &&
            hit.officialArtwork &&
            identity in trustedArtistKeys &&
            identity !in blockedArtistKeys &&
            isArtistShelfNameEligible(hit.name)
    }

    private fun isReusableHomeArtistHit(hit: ArtistHit, expectedIdentity: String): Boolean {
        return hit.thumbnailUrl.isNotBlank() &&
            hit.officialArtwork &&
            !hit.audienceIsSubscribers &&
            artistIdentityKey(hit.name) == expectedIdentity &&
            isArtistShelfNameEligible(hit.name)
    }

    private suspend fun prepareHomeArtistResolution(
        languageCode: String,
        visibleArtists: List<ArtistHit>,
        freezeVisibleShelf: Boolean,
        startupPlan: HomeStartupWorkPlan
    ) {
        if (freezeVisibleShelf) {
            awaitHomeUiIdle(startupPlan)
            return
        }

        uiState.update { current ->
            if (current.languageCode == languageCode) {
                current.copy(
                    homeArtists = visibleArtists,
                    homeArtistsLoading = visibleArtists.isEmpty()
                )
            } else {
                current
            }
        }
        delay(
            if (visibleArtists.isEmpty()) {
                HOME_ARTIST_STARTUP_GRACE_MS
            } else {
                maxOf(HOME_ARTIST_STARTUP_GRACE_MS, startupPlan.artistStartDelayMs)
            }
        )
        awaitHomeUiIdle(startupPlan)
    }

    private suspend fun resolveHomeArtistShelf(
        plan: HomeArtistPlan,
        visibleArtists: List<ArtistHit>,
        fingerprint: String,
        freezeVisibleShelf: Boolean
    ): LinkedHashMap<String, ArtistHit> {
        val visibleByBrowseId = visibleArtists.associateBy { it.browseId.lowercase() }
        val visibleByIdentity = visibleArtists.associateBy { artistIdentityKey(it.name) }
        val resolved = LinkedHashMap<String, ArtistHit>()
        val semaphore = Semaphore(HOME_ARTIST_RESOLUTION_CONCURRENCY)

        withTimeoutOrNull(HOME_ARTIST_TOTAL_TIMEOUT_MS) {
            for (batch in plan.orderedCandidates.chunked(HOME_ARTIST_RESOLUTION_CONCURRENCY)) {
                val hits = coroutineScope {
                    batch.map { candidate ->
                        async {
                            resolveHomeArtistHit(
                                candidate = candidate,
                                visibleByBrowseId = visibleByBrowseId,
                                visibleByIdentity = visibleByIdentity,
                                semaphore = semaphore
                            )
                        }
                    }.awaitAll()
                }
                hits.filterNotNull()
                    .filter { hit ->
                        isTrustedHomeArtistHit(
                            hit = hit,
                            trustedArtistKeys = plan.trustedArtistKeys,
                            blockedArtistKeys = plan.blockedArtistKeys
                        )
                    }
                    .forEach { hit ->
                        resolved.putIfAbsent(hit.browseId.lowercase(), hit)
                    }

                publishPartialHomeArtists(
                    languageCode = plan.languageCode,
                    visibleArtists = visibleArtists,
                    resolved = resolved,
                    fingerprint = fingerprint,
                    freezeVisibleShelf = freezeVisibleShelf
                )
                if (resolved.size >= HOME_ARTIST_SHELF_SIZE) break
            }
        }

        visibleArtists.forEach { hit ->
            if (resolved.size < HOME_ARTIST_SHELF_SIZE) {
                resolved.putIfAbsent(hit.browseId.lowercase(), hit)
            }
        }
        return resolved
    }

    private suspend fun resolveHomeArtistHit(
        candidate: HomeArtistCandidate,
        visibleByBrowseId: Map<String, ArtistHit>,
        visibleByIdentity: Map<String, ArtistHit>,
        semaphore: Semaphore
    ): ArtistHit? {
        val identity = artistIdentityKey(candidate.name)
        val cached = candidate.browseId
            .takeIf { it.isNotBlank() }
            ?.let { visibleByBrowseId[it.lowercase()] }
            ?: visibleByIdentity[identity]
        cached?.takeIf { hit -> isReusableHomeArtistHit(hit, identity) }?.let { return it }

        return semaphore.withPermit {
            withTimeoutOrNull(HOME_ARTIST_FAST_TIMEOUT_MS) {
                runCatching {
                    if (candidate.browseId.isNotBlank()) {
                        artistRepository.artistHit(candidate.browseId, candidate.name)
                    } else {
                        artistRepository.artistHitFor(candidate.name)
                    }
                }.getOrNull()
            }
        }
    }

    private fun publishPartialHomeArtists(
        languageCode: String,
        visibleArtists: List<ArtistHit>,
        resolved: LinkedHashMap<String, ArtistHit>,
        fingerprint: String,
        freezeVisibleShelf: Boolean
    ) {
        if (freezeVisibleShelf || resolved.isEmpty() || homeArtistsFingerprint != fingerprint) return
        val partialArtists = (resolved.values + visibleArtists)
            .distinctBy { it.browseId.lowercase() }
            .take(HOME_ARTIST_SHELF_SIZE)
        uiState.update { current ->
            if (current.languageCode == languageCode) {
                current.copy(homeArtists = partialArtists)
            } else {
                current
            }
        }
    }

    private fun finishFrozenHomeArtistRefresh(
        languageCode: String,
        finalArtists: List<ArtistHit>
    ) {
        if (finalArtists.size >= HOME_ARTIST_SHELF_SIZE) {
            deferredHomeArtistsSnapshot.set(finalArtists)
            scheduleDeferredHomeSnapshotApply()
            persistHomeSnapshotSync(languageCode)
        }
        uiState.update { current ->
            if (current.languageCode == languageCode) {
                current.copy(homeArtistsLoading = false)
            } else {
                current
            }
        }
    }

    private fun clearRemoteHomeLoadingFlags() {
        uiState.update { current ->
            if (!current.isLoadingHome && !current.homeAlbumsLoading) {
                current
            } else {
                current.copy(isLoadingHome = false, homeAlbumsLoading = false)
            }
        }
    }

    fun refreshRemoteHomeContent() {
        loadHomeFeed()
        host.loadCharts(uiState.value.selectedChartId)
        refreshHomeArtists()
        refreshAppleTrendingQuickPicks()
    }

    fun retryHomeContent() {
        LevyraNetworkIntelligence.refreshInternetAvailability()
        val offline = !LevyraNetworkIntelligence.internetAvailable.value
        val recovered = HomeOfflinePolicy.shouldRecoverOnReconnect(uiState.value.isDeviceOffline, offline)
        host.applyDeviceOfflineState(offline)
        if (offline || recovered) return
        refreshRemoteHomeContent()
    }

    fun loadHomeFeed(deferUntilHomeIdle: Boolean = false) {
        if (!HomeOfflinePolicy.shouldAttemptRemoteRefresh(uiState.value.isDeviceOffline)) {
            homeFeedJob?.cancel()
            clearRemoteHomeLoadingFlags()
            return
        }

        host.ensureMusicVideosLoaded()
        host.ensureExploreVideosLoaded()
        val requestGeneration = homeFeedRequestGeneration.incrementAndGet()
        homeFeedJob?.cancel()
        homeFeedJob = scope.launch {
            val initialState = uiState.value
            val languageCode = initialState.languageCode
            val hasVisibleHome = initialState.homeSections.isNotEmpty() || initialState.tracks.isNotEmpty()

            if (!isActive || !isHomeFeedRequestCurrent(requestGeneration, languageCode)) return@launch
            beginHomeFeedRefresh(languageCode, hasVisibleHome)

            val networkSections = fetchHomeFeedSections(languageCode)
            if (!isActive || !isHomeFeedRequestCurrent(requestGeneration, languageCode)) return@launch

            val sanitizedSections = withContext(Dispatchers.Default) {
                HomeRefreshStability.sanitizeSections(networkSections)
            }
            if (sanitizedSections.isEmpty()) {
                handleEmptyHomeFeed(
                    languageCode = languageCode,
                    requestGeneration = requestGeneration,
                    deferUntilHomeIdle = deferUntilHomeIdle,
                    hasVisibleHome = hasVisibleHome
                )
                return@launch
            }

            val previewMerge = withContext(Dispatchers.Default) {
                HomeRefreshStability.mergeSections(
                    previous = uiState.value.homeSections,
                    incoming = sanitizedSections,
                    allowStructuralChanges = canApplyHomeStructuralChanges()
                )
            }
            withContext(Dispatchers.IO) {
                preferences.saveHomeSections(sanitizedSections, languageCode)
            }
            if (previewMerge.changed && (deferUntilHomeIdle || (homeScreenActive && homeScrollInProgress))) {
                awaitHomeUiIdle()
            }
            if (!isActive || !isHomeFeedRequestCurrent(requestGeneration, languageCode)) return@launch

            val (mergeBaseSections, mergeResult) = stableHomeMerge(sanitizedSections)
            val visibleTracks = withContext(Dispatchers.Default) {
                mergeResult.visible.flatMap { it.tracks }.distinctBy { it.id }
            }
            if (visibleTracks.isEmpty()) {
                publishHomeUnavailable(languageCode)
                return@launch
            }

            val instantAlbums = homeInstantAlbums(uiState.value, visibleTracks)
            if (
                !publishHomeFeed(
                    languageCode = languageCode,
                    mergeBaseSections = mergeBaseSections,
                    mergeResult = mergeResult,
                    instantAlbums = instantAlbums,
                    visibleTracks = visibleTracks
                )
            ) {
                stopHomeLoading(languageCode)
                return@launch
            }

            finishHomeFeedRefresh(
                languageCode = languageCode,
                mergeResult = mergeResult,
                visibleTracks = visibleTracks,
                deferUntilHomeIdle = deferUntilHomeIdle
            )
        }
    }

    private fun isHomeFeedRequestCurrent(requestGeneration: Long, languageCode: String): Boolean {
        return homeFeedRequestGeneration.get() == requestGeneration &&
            uiState.value.languageCode == languageCode
    }

    private fun beginHomeFeedRefresh(languageCode: String, hasVisibleHome: Boolean) {
        uiState.update { current ->
            if (current.languageCode != languageCode) {
                current
            } else {
                current.copy(
                    isLoadingHome = HomeOfflinePolicy.remoteLoading(!hasVisibleHome, current.isDeviceOffline),
                    homeError = null
                )
            }
        }
    }

    private suspend fun fetchHomeFeedSections(languageCode: String): List<HomeSection> {
        return try {
            repository.homeFeed(languageCode)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.w(error, "Home feed refresh failed")
            emptyList()
        }
    }

    private suspend fun handleEmptyHomeFeed(
        languageCode: String,
        requestGeneration: Long,
        deferUntilHomeIdle: Boolean,
        hasVisibleHome: Boolean
    ) {
        loadHomeAlbums(languageCode, deferUntilHomeIdle)
        if (hasVisibleHome) {
            stopHomeLoading(languageCode)
        } else {
            loadFallbackHome(
                languageCode = languageCode,
                requestGeneration = requestGeneration,
                deferUntilHomeIdle = deferUntilHomeIdle
            )
        }
    }

    private suspend fun stableHomeMerge(
        sanitizedSections: List<HomeSection>
    ): Pair<List<HomeSection>, HomeSectionMergeResult> {
        var mergeBaseSections = uiState.value.homeSections
        var mergeResult = withContext(Dispatchers.Default) {
            HomeRefreshStability.mergeSections(
                previous = mergeBaseSections,
                incoming = sanitizedSections,
                allowStructuralChanges = canApplyHomeStructuralChanges()
            )
        }
        if (uiState.value.homeSections != mergeBaseSections) {
            mergeBaseSections = uiState.value.homeSections
            mergeResult = withContext(Dispatchers.Default) {
                HomeRefreshStability.mergeSections(
                    previous = mergeBaseSections,
                    incoming = sanitizedSections,
                    allowStructuralChanges = canApplyHomeStructuralChanges()
                )
            }
        }
        return mergeBaseSections to mergeResult
    }

    private fun publishHomeUnavailable(languageCode: String) {
        val unavailableMessage = LevyraStrings.forCode(languageCode).homeRemoteUnavailable
        uiState.update { current ->
            if (current.languageCode != languageCode) {
                current
            } else {
                current.copy(
                    isLoadingHome = false,
                    homeError = if (current.homeSections.isEmpty() && current.tracks.isEmpty()) {
                        HomeOfflinePolicy.homeErrorAfterRemoteFailure(
                            deviceOffline = current.isDeviceOffline,
                            fallback = unavailableMessage
                        )
                    } else {
                        current.homeError
                    }
                )
            }
        }
    }

    private suspend fun homeInstantAlbums(
        latestState: LevyraUiState,
        visibleTracks: List<Track>
    ): List<AlbumHit> {
        return latestState.homeAlbums.ifEmpty {
            withContext(Dispatchers.Default) {
                host.instantAlbumRecommendationsFromTracks(
                    primary = latestState.recentListens + latestState.recentSearches + latestState.favorites,
                    secondary = latestState.personalOrbitTracks + visibleTracks,
                    limit = HOME_ALBUM_RECOMMENDATION_LIMIT,
                    profile = latestState.smartProfile
                )
            }
        }
    }

    private fun publishHomeFeed(
        languageCode: String,
        mergeBaseSections: List<HomeSection>,
        mergeResult: HomeSectionMergeResult,
        instantAlbums: List<AlbumHit>,
        visibleTracks: List<Track>
    ): Boolean {
        while (true) {
            val current = uiState.value
            if (
                current.languageCode != languageCode ||
                current.homeSections != mergeBaseSections
            ) {
                return false
            }

            val updated = if (
                !mergeResult.changed &&
                current.homeAlbums == instantAlbums &&
                current.tracks == visibleTracks &&
                !current.isLoadingHome &&
                current.homeError == null
            ) {
                current
            } else {
                current.copy(
                    homeSections = if (mergeResult.changed) mergeResult.visible else current.homeSections,
                    homeAlbums = instantAlbums,
                    homeAlbumsLoading = instantAlbums.isEmpty(),
                    tracks = visibleTracks,
                    isLoadingHome = false,
                    homeError = null,
                    cacheReport = repository.cacheReport()
                )
            }
            if (updated === current || uiState.compareAndSet(current, updated)) return true
        }
    }

    private fun stopHomeLoading(languageCode: String) {
        uiState.update { current ->
            if (current.languageCode == languageCode && current.isLoadingHome) {
                current.copy(isLoadingHome = false)
            } else {
                current
            }
        }
    }

    private fun finishHomeFeedRefresh(
        languageCode: String,
        mergeResult: HomeSectionMergeResult,
        visibleTracks: List<Track>,
        deferUntilHomeIdle: Boolean
    ) {
        pendingHomeSectionsSnapshot.set(mergeResult.deferredStructural)
        if (mergeResult.deferredStructural != null) {
            scheduleDeferredHomeSnapshotApply()
        }

        persistHomeSnapshot()
        val startupPlan = homeStartupWorkPlan()
        scope.launch(Dispatchers.IO) {
            if (deferUntilHomeIdle) awaitHomeUiIdle(startupPlan)
            LevyraArtworkCache.preloadHome(
                application.applicationContext,
                visibleTracks,
                startupPlan.refreshedArtworkCount
            )
        }
        host.prefetchTop(visibleTracks, HOME_STARTUP_STREAM_PREFETCH_COUNT, respectHomeScroll = deferUntilHomeIdle)
        host.refreshOfficialMetadataBatch(visibleTracks, HOME_STARTUP_METADATA_REFRESH_COUNT, deferUntilHomeIdle)
        loadHomeAlbums(languageCode, deferUntilHomeIdle)
        refreshHomeResonanceIfStale()
    }

    private fun loadHomeAlbums(languageCode: String, deferUntilHomeIdle: Boolean = false) {
        val requestGeneration = homeAlbumsRequestGeneration.incrementAndGet()
        homeAlbumsJob?.cancel()
        homeAlbumsJob = scope.launch {
            val initialState = uiState.value
            if (initialState.languageCode != languageCode) return@launch

            if (initialState.homeAlbums.isEmpty()) {
                val instantAlbums = host.instantAlbumRecommendations(initialState)
                    .take(HOME_ALBUM_RECOMMENDATION_LIMIT)
                uiState.update { current ->
                    if (current.languageCode != languageCode || current.homeAlbums.isNotEmpty()) current
                    else current.copy(
                        homeAlbums = instantAlbums,
                        homeAlbumsLoading = instantAlbums.isEmpty()
                    )
                }
            } else if (initialState.homeAlbumsLoading) {
                uiState.update { current ->
                    if (current.languageCode == languageCode) current.copy(homeAlbumsLoading = false) else current
                }
            }

            val startupPlan = homeStartupWorkPlan()
            if (deferUntilHomeIdle) {
                delay(startupPlan.albumStartDelayMs)
                awaitHomeUiIdle(startupPlan)
                if (
                    !isActive ||
                    homeAlbumsRequestGeneration.get() != requestGeneration ||
                    uiState.value.languageCode != languageCode
                ) return@launch
            }
            val seeds = host.albumRecommendationSeeds(uiState.value).let { candidates ->
                if (deferUntilHomeIdle) candidates.take(startupPlan.albumSeedCount) else candidates
            }
            val remoteLimit = if (deferUntilHomeIdle) {
                maxOf(HOME_ALBUM_RECOMMENDATION_LIMIT, startupPlan.albumCandidateCount)
            } else {
                HOME_ALBUM_REMOTE_CANDIDATE_LIMIT
            }
            val remoteConcurrency = if (deferUntilHomeIdle) {
                startupPlan.albumConcurrency
            } else {
                HOME_ALBUM_REMOTE_CONCURRENCY
            }
            val albums = try {
                repository.homeAlbums(
                    languageCode = languageCode,
                    limit = remoteLimit,
                    seeds = seeds,
                    concurrency = remoteConcurrency
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
                uiState.value.languageCode != languageCode
            ) return@launch
            if (albums.isEmpty()) {
                uiState.update { current ->
                    if (current.languageCode == languageCode && current.homeAlbumsLoading) {
                        current.copy(homeAlbumsLoading = false)
                    } else {
                        current
                    }
                }
                return@launch
            }

            val latestState = uiState.value
            val latestInstantAlbums = host.instantAlbumRecommendations(latestState)
            val rankedAlbums = withContext(Dispatchers.Default) {
                host.rankAlbumRecommendations(
                    remote = albums,
                    instant = latestInstantAlbums,
                    state = latestState,
                    limit = HOME_ALBUM_RECOMMENDATION_LIMIT
                ).ifEmpty { latestInstantAlbums.take(HOME_ALBUM_RECOMMENDATION_LIMIT) }
            }
            if (rankedAlbums.isEmpty()) {
                uiState.update { current ->
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
            if (deferUntilHomeIdle && uiState.value.homeAlbums != rankedAlbums) awaitHomeUiIdle()
            if (
                !isActive ||
                homeAlbumsRequestGeneration.get() != requestGeneration ||
                uiState.value.languageCode != languageCode
            ) return@launch

            uiState.update { current ->
                if (current.languageCode != languageCode) current
                else if (current.homeAlbums == rankedAlbums && !current.homeAlbumsLoading) current
                else current.copy(homeAlbums = rankedAlbums, homeAlbumsLoading = false)
            }
        }
    }

    fun refreshAppleTrendingQuickPicks() {
        val snapshot = uiState.value
        if (snapshot.isDeviceOffline) return
        val languageCode = snapshot.languageCode
        val storefront = AppleStorefronts.normalize(ChartsCatalog.defaultRegionForLanguage(languageCode).country)
        appleTrendingJob?.cancel()
        appleTrendingJob = scope.launch {
            awaitHomeUiIdle()
            val apple = AppleMusicEditorialRepository.get(application)
            val appleLanguage = AppleStorefronts.language(languageCode)
            val page = runCatchingPreservingCancellation { apple.newMusic(storefront, appleLanguage)?.page }.getOrNull()
            val songs = com.luc4n3x.levyra.data.apple.appleTrendingSongs(page)
            if (songs.isEmpty()) return@launch
            val detailed = apple.withSongDetails(storefront, appleLanguage, songs)
            val trending = com.luc4n3x.levyra.data.apple.AppleTrendingQuickPicks.match(detailed) { query ->
                repository.searchForPlayback(query, APPLE_TRENDING_SEARCH_LIMIT, languageCode)
            }
            if (trending.isEmpty()) return@launch
            uiState.update { current ->
                if (current.languageCode != languageCode) current
                else current.copy(quickPickSeeds = trending + LevyraStartupCatalog.quickPickSeeds(languageCode))
            }
        }
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
            uiState.value.languageCode != languageCode
        ) return

        if (tracks.isEmpty()) {
            val emptyMessage = LevyraStrings.forCode(languageCode).homeRemoteEmpty
            uiState.update { current ->
                if (current.languageCode != languageCode) current
                else current.copy(
                    isLoadingHome = false,
                    homeError = if (current.homeSections.isEmpty() && current.tracks.isEmpty()) {
                        HomeOfflinePolicy.homeErrorAfterRemoteFailure(
                            deviceOffline = current.isDeviceOffline,
                            fallback = emptyMessage
                        )
                    } else {
                        current.homeError
                    }
                )
            }
            return
        }

        val fallbackSection = com.luc4n3x.levyra.domain.HomeSection(
            LevyraContentLocales.forLanguage(languageCode).quickSectionTitle,
            tracks.take(20)
        )
        val shouldPublishSection = uiState.value.homeSections.isEmpty()
        if (deferUntilHomeIdle && shouldPublishSection) awaitHomeUiIdle()
        if (
            !currentCoroutineContext().isActive ||
            homeFeedRequestGeneration.get() != requestGeneration ||
            uiState.value.languageCode != languageCode
        ) return

        uiState.update { current ->
            if (current.languageCode != languageCode) current
            else current.copy(
                homeSections = if (shouldPublishSection) listOf(fallbackSection) else current.homeSections,
                tracks = tracks,
                isLoadingHome = false,
                homeError = null,
                smartScore = host.calculateSmartScore(moodEngine.buildQueue(current.selectedMood, tracks)),
                cacheReport = repository.cacheReport()
            )
        }
        withContext(Dispatchers.IO) {
            preferences.saveHomeSections(listOf(fallbackSection), languageCode)
        }
        persistHomeSnapshot()
        val startupPlan = homeStartupWorkPlan()
        scope.launch(Dispatchers.IO) {
            if (deferUntilHomeIdle) awaitHomeUiIdle(startupPlan)
            LevyraArtworkCache.preloadHome(
                application.applicationContext,
                tracks,
                startupPlan.refreshedArtworkCount
            )
        }
        host.prefetchTop(tracks, startupPlan.refreshedArtworkCount, respectHomeScroll = deferUntilHomeIdle)
    }
}

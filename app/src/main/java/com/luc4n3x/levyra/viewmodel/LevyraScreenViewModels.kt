package com.luc4n3x.levyra.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.data.HomeContentAvailability
import com.luc4n3x.levyra.data.HomeEditorialEngine
import com.luc4n3x.levyra.data.LevyraStartupCatalog
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.ChartRegion
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.FollowedArtist
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.HomeEditorialCollection
import com.luc4n3x.levyra.domain.HomeSpotlightCandidate
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.ListeningPulse
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.Mood
import com.luc4n3x.levyra.domain.OfflineDownloadTask
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.ReleaseRadarEntry
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.YoutubeEngagementState
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.util.Locale

abstract class LevyraScreenViewModel(
    protected val root: LevyraViewModel,
    projection: (LevyraUiState) -> Any
) : ViewModel() {
    val state: StateFlow<LevyraUiState> = root.state
        .map { it }
        .distinctUntilChanged { previous, current -> projection(previous) == projection(current) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = root.state.value
        )
}

data class HomePlaybackProgress(
    val positionMs: Long,
    val durationMs: Long
)

class HomeViewModel(root: LevyraViewModel) : LevyraScreenViewModel(root, ::homeProjection) {
    private val freezeHomeContent = MutableStateFlow(false)
    private var homeRenderSettleJob: Job? = null

    internal val renderState: StateFlow<HomeRenderSnapshot> = combine(state, freezeHomeContent) { snapshot, freeze ->
        HomeRenderInput(snapshot, freeze)
    }
        .scan(buildHomeRenderSnapshot(root.state.value)) { previous, input ->
            withContext(Dispatchers.Default) {
                buildStableHomeRenderSnapshot(
                    state = input.state,
                    previous = previous,
                    freezeContent = input.freezeContent
                )
            }
        }
        .distinctUntilChanged(::sameHomeRenderSnapshot)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = buildHomeRenderSnapshot(root.state.value)
        )
    val playbackProgress: StateFlow<HomePlaybackProgress> = root.state
        .map { HomePlaybackProgress(it.positionMs, it.durationMs) }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HomePlaybackProgress(root.state.value.positionMs, root.state.value.durationMs)
        )
    fun addToPlaylist(playlistId: String, track: Track) = root.addToPlaylist(playlistId, track)
    fun addToQueue(track: Track) = root.addToQueue(track)
    fun createPlaylist(name: String, firstTrack: Track? = null) = root.createPlaylist(name, firstTrack)
    fun openAlbum(album: AlbumHit) = root.openAlbum(album)
    fun openArtistByName(name: String) = root.openArtistByName(name)
    fun openArtistFromHit(hit: ArtistHit) = root.openArtistFromHit(hit)
    fun openSettings() = root.openSettings()
    fun playAlbumRecommendations(albums: List<AlbumHit>) = root.playAlbumRecommendations(albums)
    fun refreshHomeArtists() = root.refreshHomeArtists()
    fun playAll(tracks: List<Track>) = root.playAll(tracks)
    fun playFrom(list: List<Track>, track: Track, loopOnCompletion: Boolean = false) = root.playFrom(list, track, loopOnCompletion)
    fun searchNow() = root.searchNow()
    fun searchNow(query: String) = root.searchNow(query)
    fun selectChart(regionId: String) = root.selectChart(regionId)
    fun selectMood(mood: Mood) = root.selectMood(mood)
    fun toggleFavorite(track: Track) = root.toggleFavorite(track)
    fun togglePlay() = root.togglePlay()
    fun onHomeEntered(atTop: Boolean) {
        homeRenderSettleJob?.cancel()
        freezeHomeContent.value = shouldFreezeHomeStructure(scrollInProgress = false, atTop = atTop)
        root.onHomeEntered(atTop)
    }

    fun onHomeLeft() {
        homeRenderSettleJob?.cancel()
        freezeHomeContent.value = false
        root.onHomeLeft()
    }

    fun setHomeViewport(scrollInProgress: Boolean, atTop: Boolean) {
        root.setHomeViewport(scrollInProgress, atTop)
        homeRenderSettleJob?.cancel()
        if (shouldFreezeHomeStructure(scrollInProgress, atTop)) {
            freezeHomeContent.value = true
        } else {
            homeRenderSettleJob = viewModelScope.launch {
                delay(HOME_RENDER_SETTLE_MS)
                freezeHomeContent.value = false
            }
        }
    }
}

class SearchViewModel(root: LevyraViewModel) : LevyraScreenViewModel(root, ::searchProjection) {
    fun addToPlaylist(playlistId: String, track: Track) = root.addToPlaylist(playlistId, track)
    fun addToQueue(track: Track) = root.addToQueue(track)
    fun createPlaylist(name: String, firstTrack: Track? = null) = root.createPlaylist(name, firstTrack)
    fun deleteDownload(download: DownloadedTrack) = root.deleteDownload(download)
    fun exportTrack(track: Track) = root.exportTrack(track)
    fun openAlbum(album: AlbumHit) = root.openAlbum(album)
    fun openArtist(track: Track) = root.openArtist(track)
    fun openArtistFromHit(hit: ArtistHit) = root.openArtistFromHit(hit)
    fun play(track: Track) = root.play(track)
    fun playDownloaded(download: DownloadedTrack) = root.playDownloaded(download)
    fun playFrom(list: List<Track>, track: Track, loopOnCompletion: Boolean = false) = root.playFrom(list, track, loopOnCompletion)
    fun playNext(track: Track) = root.playNext(track)
    fun removeRecentSearch(track: Track) = root.removeRecentSearch(track)
    fun searchNow() = root.searchNow()
    fun searchNow(query: String) = root.searchNow(query)
    fun setQuery(query: String) = root.setQuery(query)
    fun setSearchFilter(filter: SearchFilter) = root.setSearchFilter(filter)
    fun toggleFavorite(track: Track) = root.toggleFavorite(track)
}

class ExploreViewModel(root: LevyraViewModel) : LevyraScreenViewModel(root, ::exploreProjection) {
    fun ensureExplore(strings: LevyraStrings) = root.ensureExplore(strings)
    fun playFrom(list: List<Track>, track: Track, loopOnCompletion: Boolean = false) = root.playFrom(list, track, loopOnCompletion)
    fun selectExploreZone(zone: ExploreZone) = root.selectExploreZone(zone)
    fun toggleFavorite(track: Track) = root.toggleFavorite(track)
}

class LibraryViewModel(root: LevyraViewModel) : LevyraScreenViewModel(root, ::libraryProjection) {
    fun addToPlaylist(playlistId: String, track: Track) = root.addToPlaylist(playlistId, track)
    fun addTracksToPlaylist(playlistId: String, tracks: List<Track>) = root.addTracksToPlaylist(playlistId, tracks)
    fun addToQueue(track: Track) = root.addToQueue(track)
    fun addTracksToQueue(tracks: List<Track>) = root.addTracksToQueue(tracks)
    fun cancelDownload(taskKey: String) = root.cancelDownload(taskKey)
    fun closePlaylist() = root.closePlaylist()
    fun createPlaylist(name: String, firstTrack: Track? = null) = root.createPlaylist(name, firstTrack)
    fun createPlaylistWithTracks(name: String, tracks: List<Track>) = root.createPlaylistWithTracks(name, tracks)
    fun deleteDownload(download: DownloadedTrack) = root.deleteDownload(download)
    fun deleteDownloads(downloads: List<DownloadedTrack>) = root.deleteDownloads(downloads)
    fun deletePlaylist(playlistId: String) = root.deletePlaylist(playlistId)
    fun deletePlaylists(playlistIds: Collection<String>) = root.deletePlaylists(playlistIds)
    fun exportOpenPlaylist() = root.exportOpenPlaylist()
    fun exportTrack(track: Track) = root.exportTrack(track)
    fun exportTracks(tracks: List<Track>, label: String) = root.exportTracks(tracks, label)
    fun openAlbum(album: AlbumHit) = root.openAlbum(album)
    fun openArtist(track: Track) = root.openArtist(track)
    fun openArtistByName(name: String) = root.openArtistByName(name)
    fun openArtistReference(name: String, browseId: String, thumbnailUrl: String) = root.openArtistFromHit(
        ArtistHit(
            name = name,
            subscribers = "",
            thumbnailUrl = thumbnailUrl,
            accentStart = 0,
            accentEnd = 0,
            browseId = browseId
        )
    )
    fun openPlayerScreen() = root.openPlayerScreen()
    fun openPlaylist(playlistId: String) = root.openPlaylist(playlistId)
    fun pauseDownload(taskKey: String) = root.pauseDownload(taskKey)
    fun playDownloaded(download: DownloadedTrack) = root.playDownloaded(download)
    fun playFrom(list: List<Track>, track: Track, loopOnCompletion: Boolean = false) = root.playFrom(list, track, loopOnCompletion)
    fun playPlaylist(playlistId: String, startTrackId: String? = null) = root.playPlaylist(playlistId, startTrackId)
    fun removeFavorites(tracks: List<Track>) = root.removeFavorites(tracks)
    fun removeFromPlaylist(playlistId: String, trackId: String) = root.removeFromPlaylist(playlistId, trackId)
    fun removeTracksFromPlaylist(playlistId: String, tracks: List<Track>) = root.removeTracksFromPlaylist(playlistId, tracks)
    fun renamePlaylist(playlistId: String, name: String) = root.renamePlaylist(playlistId, name)
    fun reorderPlaylist(playlistId: String, tracks: List<Track>) = root.reorderPlaylist(playlistId, tracks)
    fun resumeDownload(taskKey: String) = root.resumeDownload(taskKey)
    fun toggleFavorite(track: Track) = root.toggleFavorite(track)
    fun togglePlay() = root.togglePlay()
}

class PlayerViewModel(root: LevyraViewModel) : LevyraScreenViewModel(root, ::playerProjection) {
    fun closePlayer() = root.closePlayer()
    fun cycleSleepTimer() = root.cycleSleepTimer()
    fun cycleSpeed() = root.cycleSpeed()
    fun exportCurrentTrack() = root.exportCurrentTrack()
    fun next() = root.next()
    fun openArtist(track: Track) = root.openArtist(track)
    fun openAudioQualityPanel() = root.openAudioQualityPanel()
    fun openLyrics() = root.openLyrics()
    fun openQueue() = root.openQueue()
    fun openYoutubeComments() = root.openYoutubeComments()
    fun closeYoutubeComments() = root.closeYoutubeComments()
    fun retryYoutubeComments() = root.retryYoutubeComments()
    fun loadMoreYoutubeComments() = root.loadMoreYoutubeComments()
    fun retryYoutubeCommentsPage() = root.retryYoutubeCommentsPage()
    fun toggleYoutubeCommentReplies(commentId: String) = root.toggleYoutubeCommentReplies(commentId)
    fun loadMoreYoutubeCommentReplies(commentId: String) = root.loadMoreYoutubeCommentReplies(commentId)
    fun previous() = root.previous()
    fun seekBy(deltaMs: Long) = root.seekBy(deltaMs)
    fun seekTo(progress: Float) = root.seekTo(progress)
    fun selectTab(tab: LevyraTab) = root.selectTab(tab)
    fun toggleAudioNormalization() = root.toggleAudioNormalization()
    fun toggleFavorite(track: Track) = root.toggleFavorite(track)
    fun togglePlay() = root.togglePlay()
    fun toggleRepeat() = root.toggleRepeat()
    fun toggleShuffle() = root.toggleShuffle()
    fun toggleVideoMode() = root.toggleVideoMode()
    fun setTemporaryPlaybackSpeed(value: Float) = root.setTemporaryPlaybackSpeed(value)
}

class LevyraScreenViewModelFactory(
    private val root: LevyraViewModel
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(root)
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> SearchViewModel(root)
            modelClass.isAssignableFrom(ExploreViewModel::class.java) -> ExploreViewModel(root)
            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> LibraryViewModel(root)
            modelClass.isAssignableFrom(PlayerViewModel::class.java) -> PlayerViewModel(root)
            else -> throw IllegalArgumentException("Unsupported screen ViewModel: ${modelClass.name}")
        } as T
    }
}


private const val HOME_RENDER_SETTLE_MS = 360L

internal fun shouldFreezeHomeStructure(scrollInProgress: Boolean, atTop: Boolean): Boolean {
    return scrollInProgress || !atTop
}

private data class HomeRenderInput(
    val state: LevyraUiState,
    val freezeContent: Boolean
)

internal fun buildStableHomeRenderSnapshot(
    state: LevyraUiState,
    previous: HomeRenderSnapshot,
    freezeContent: Boolean
): HomeRenderSnapshot {
    val visibleState = if (freezeContent) state.withFrozenHomeContent(previous.state) else state
    return buildHomeRenderSnapshot(visibleState, previous)
}

private fun LevyraUiState.withFrozenHomeContent(previous: LevyraUiState): LevyraUiState {
    return copy(
        tracks = previous.tracks,
        recentSearches = previous.recentSearches,
        recentListens = previous.recentListens,
        personalOrbitTracks = previous.personalOrbitTracks,
        favorites = previous.favorites,
        charts = previous.charts,
        isLoadingCharts = previous.isLoadingCharts,
        homeSections = previous.homeSections,
        homeAlbums = previous.homeAlbums,
        homeArtists = previous.homeArtists,
        homeResonanceTracks = previous.homeResonanceTracks,
        homeArtistsLoading = previous.homeArtistsLoading,
        homeAlbumsLoading = previous.homeAlbumsLoading,
        isLoadingHome = previous.isLoadingHome,
        homeError = previous.homeError,
        releaseRadar = previous.releaseRadar,
        similarArtists = previous.similarArtists
    )
}

private fun sameHomeRenderSnapshot(previous: HomeRenderSnapshot, current: HomeRenderSnapshot): Boolean {
    return homeProjection(previous.state) == homeProjection(current.state) && previous.derived == current.derived
}

@Immutable
internal data class HomeRenderSnapshot(
    val state: LevyraUiState,
    val derived: HomeDerivedState
)

@Immutable
internal data class HomeDerivedState(
    val resonanceTracks: List<Track>,
    val artistRefreshFingerprint: String,
    val quickPicks: HomeSection?,
    val newReleases: HomeSection?,
    val otherSections: List<HomeSection>,
    val spotlightCandidates: List<HomeSpotlightCandidate>,
    val editorialCollections: List<HomeEditorialCollection>,
    val chartChunks: List<List<Track>>,
    val contentAvailability: HomeContentAvailability,
    val contentFingerprint: String
)

private data class HomeDerivedInput(
    val languageCode: String,
    val recentListens: List<Track>,
    val personalOrbitTracks: List<Track>,
    val favorites: List<Track>,
    val tracks: List<Track>,
    val homeSections: List<HomeSection>,
    val homeAlbums: List<AlbumHit>,
    val charts: List<Track>,
    val cachedResonanceTracks: List<Track>,
    val releaseRadar: List<ReleaseRadarEntry>,
    val similarArtists: List<ArtistHit>,
    val currentTrack: Track?,
    val selectedMood: Mood?,
    val showNewReleases: Boolean,
    val showPersonalOrbit: Boolean,
    val showResonance: Boolean,
    val showCharts: Boolean
)

internal fun buildHomeRenderSnapshot(state: LevyraUiState): HomeRenderSnapshot {
    return HomeRenderSnapshot(
        state = state,
        derived = buildHomeDerivedState(state.toHomeDerivedInput())
    )
}

internal fun buildHomeRenderSnapshot(
    state: LevyraUiState,
    previous: HomeRenderSnapshot
): HomeRenderSnapshot {
    val derived = if (sameHomeDerivedInputs(previous.state, state)) {
        previous.derived
    } else {
        buildHomeDerivedState(state.toHomeDerivedInput())
    }
    return HomeRenderSnapshot(state = state, derived = derived)
}

private fun sameHomeDerivedInputs(previous: LevyraUiState, current: LevyraUiState): Boolean {
    return previous.languageCode == current.languageCode &&
        previous.currentTrack === current.currentTrack &&
        previous.recentListens === current.recentListens &&
        previous.personalOrbitTracks === current.personalOrbitTracks &&
        previous.favorites === current.favorites &&
        previous.tracks === current.tracks &&
        previous.homeSections === current.homeSections &&
        previous.homeAlbums === current.homeAlbums &&
        previous.charts === current.charts &&
        previous.homeResonanceTracks === current.homeResonanceTracks &&
        previous.releaseRadar === current.releaseRadar &&
        previous.similarArtists === current.similarArtists &&
        previous.selectedMood == current.selectedMood &&
        previous.interfaceSettings.showNewReleases == current.interfaceSettings.showNewReleases &&
        previous.interfaceSettings.showPersonalOrbit == current.interfaceSettings.showPersonalOrbit &&
        previous.interfaceSettings.showResonance == current.interfaceSettings.showResonance &&
        previous.interfaceSettings.showCharts == current.interfaceSettings.showCharts
}

private fun LevyraUiState.toHomeDerivedInput(): HomeDerivedInput {
    return HomeDerivedInput(
        languageCode = languageCode,
        recentListens = recentListens,
        personalOrbitTracks = personalOrbitTracks,
        favorites = favorites,
        tracks = tracks,
        homeSections = homeSections,
        homeAlbums = homeAlbums,
        charts = charts,
        cachedResonanceTracks = homeResonanceTracks,
        releaseRadar = releaseRadar,
        similarArtists = similarArtists,
        currentTrack = currentTrack,
        selectedMood = selectedMood,
        showNewReleases = interfaceSettings.showNewReleases,
        showPersonalOrbit = interfaceSettings.showPersonalOrbit,
        showResonance = interfaceSettings.showResonance,
        showCharts = interfaceSettings.showCharts
    )
}

private fun buildHomeDerivedState(input: HomeDerivedInput): HomeDerivedState {
    val mood = input.selectedMood
    fun moodRank(tracks: List<Track>): List<Track> {
        if (mood == null || tracks.size < 2) return tracks
        return tracks.sortedByDescending { track ->
            val tagMatches = track.moodTags.count { tag ->
                mood.tags.any { moodTag -> moodTag.equals(tag, ignoreCase = true) }
            }
            val energyFit = 100 - kotlin.math.abs(track.energy - mood.energyTarget).coerceIn(0, 100)
            tagMatches * 220 + energyFit * 2 + track.replayScore.coerceIn(0, 100)
        }
    }
    val quickPicks = buildQuickPicks(input)?.let { it.copy(tracks = moodRank(it.tracks)) }
    val newReleases = input.homeSections.firstOrNull {
        isVerifiedHomeReleaseSectionTitle(it.title, input.languageCode)
    }?.let { it.copy(tracks = moodRank(it.tracks)) }
    val otherSections = input.homeSections.filter {
        !isVerifiedHomeReleaseSectionTitle(it.title, input.languageCode) &&
            !isHomeQuickPicksSectionTitle(it.title) &&
            !isHomePersonalOrbitSectionTitle(it.title, input.languageCode)
    }
    val contentAvailability = HomeContentAvailability(
        trackCount = input.tracks.size,
        homeSectionCount = input.homeSections.size,
        homeSectionTrackCount = input.homeSections.sumOf { it.tracks.size },
        albumCount = input.homeAlbums.size,
        chartCount = input.charts.size,
        personalOrbitCount = input.personalOrbitTracks.size,
        releaseRadarCount = input.releaseRadar.size,
        similarArtistCount = input.similarArtists.size,
        hasCurrentTrack = input.currentTrack != null
    )
    val resonanceTracks = input.cachedResonanceTracks.ifEmpty { buildHomeResonanceTracks(input) }
    val spotlightCandidates = HomeEditorialEngine.buildSpotlightCandidates(
        showNewReleases = input.showNewReleases,
        newReleaseTracks = moodRank(newReleases?.tracks.orEmpty()),
        showPersonalOrbit = input.showPersonalOrbit,
        personalTracks = moodRank(input.personalOrbitTracks.take(LevyraPersonalOrbit.DISPLAY_LIMIT)),
        showResonance = input.showResonance,
        resonanceTracks = moodRank(resonanceTracks),
        quickPickTracks = moodRank(quickPicks?.tracks.orEmpty()),
        fallbackSections = otherSections.map { moodRank(it.tracks) },
        chartTracks = if (input.showCharts) moodRank(input.charts) else emptyList()
    )
    val visibleCollectionSections = input.homeSections.filter { section ->
        isHomeSectionVisible(section.title, input)
    }
    val editorialCollections = HomeEditorialEngine.buildCollections(
        homeSections = visibleCollectionSections,
        newReleaseTracks = if (input.showNewReleases) moodRank(newReleases?.tracks.orEmpty()) else emptyList(),
        personalTracks = if (input.showPersonalOrbit) moodRank(input.personalOrbitTracks) else emptyList(),
        resonanceTracks = if (input.showResonance) moodRank(resonanceTracks) else emptyList(),
        quickPickTracks = moodRank(quickPicks?.tracks.orEmpty()),
        chartTracks = if (input.showCharts) moodRank(input.charts) else emptyList(),
        favorites = input.favorites,
        libraryTracks = input.tracks,
        includeFresh = input.showNewReleases
    )
    return HomeDerivedState(
        resonanceTracks = resonanceTracks,
        artistRefreshFingerprint = buildHomeArtistRefreshFingerprint(input),
        quickPicks = quickPicks,
        newReleases = newReleases,
        otherSections = otherSections,
        spotlightCandidates = spotlightCandidates,
        editorialCollections = editorialCollections,
        chartChunks = input.charts.chunked(4),
        contentAvailability = contentAvailability,
        contentFingerprint = buildHomeContentFingerprint(input, contentAvailability)
    )
}

private const val HOME_QUICK_PICKS_LIMIT = 20
private const val HOME_QUICK_PICKS_ARTIST_LIMIT = 2

private fun buildQuickPicks(input: HomeDerivedInput): HomeSection? {
    val orbitKeys = input.personalOrbitTracks
        .asSequence()
        .map { track -> LevyraPersonalOrbit.identityKey(track) }
        .filter(String::isNotBlank)
        .toHashSet()
    val candidates = buildList {
        input.homeSections
            .firstOrNull { isHomeQuickPicksSectionTitle(it.title) }
            ?.tracks
            ?.let(::addAll)
        input.homeSections
            .asSequence()
            .filter { section -> isHomeSectionVisible(section.title, input) }
            .filterNot {
                isHomeQuickPicksSectionTitle(it.title) ||
                    isHomePersonalOrbitSectionTitle(it.title, input.languageCode)
            }
            .forEach { section -> addAll(section.tracks) }
        if (input.showCharts) addAll(input.charts)
        addAll(input.recentListens)
        addAll(input.favorites)
        addAll(input.tracks)
        input.currentTrack?.let(::add)
        LevyraStartupCatalog.homeSections(input.languageCode)
            .firstOrNull { isHomeQuickPicksSectionTitle(it.title) }
            ?.tracks
            ?.let(::addAll)
    }

    val selected = ArrayList<Track>(HOME_QUICK_PICKS_LIMIT)
    val seen = HashSet<String>()
    val artistCounts = HashMap<String, Int>()

    fun addCandidate(track: Track, enforceArtistLimit: Boolean) {
        if (selected.size >= HOME_QUICK_PICKS_LIMIT) return
        if (track.id.length != 11 || !isReliableHomeMusicCandidate(track)) return
        val identity = LevyraPersonalOrbit.identityKey(track)
        if (identity.isBlank() || identity in orbitKeys || !seen.add(identity)) return
        val artistKey = track.artist.trim().lowercase(Locale.ROOT)
        val artistCount = artistCounts[artistKey] ?: 0
        if (enforceArtistLimit && artistKey.isNotBlank() && artistCount >= HOME_QUICK_PICKS_ARTIST_LIMIT) {
            seen.remove(identity)
            return
        }
        selected += track
        if (artistKey.isNotBlank()) artistCounts[artistKey] = artistCount + 1
    }

    candidates.forEach { track -> addCandidate(track, enforceArtistLimit = true) }
    if (selected.size < HOME_QUICK_PICKS_LIMIT) {
        candidates.forEach { track -> addCandidate(track, enforceArtistLimit = false) }
    }
    if (selected.isEmpty()) return null

    return HomeSection(
        title = LevyraContentLocales.forLanguage(input.languageCode).quickSectionTitle,
        tracks = selected
    )
}

private fun buildHomeResonanceTracks(input: HomeDerivedInput): List<Track> {
    return buildList {
        addAll(input.charts)
        input.homeSections.forEach { section -> addAll(section.tracks) }
        addAll(input.favorites)
        addAll(input.tracks)
        input.currentTrack?.let(::add)
    }
        .asSequence()
        .filter { it.id.length == 11 && isReliableHomeMusicCandidate(it) }
        .distinctBy { it.id }
        .sortedWith(
            compareByDescending<Track> { it.replayScore + it.vocal + it.cacheScore / 2 }
                .thenBy { it.title }
        )
        .take(8)
        .toList()
}

internal fun buildHomeResonanceTracks(state: LevyraUiState): List<Track> {
    return buildHomeResonanceTracks(state.toHomeDerivedInput())
}

private fun buildHomeArtistRefreshFingerprint(input: HomeDerivedInput): String {
    val tracks = sequence {
        yieldAll(input.recentListens)
        yieldAll(input.personalOrbitTracks)
        yieldAll(input.favorites)
        yieldAll(input.tracks)
        input.homeSections.forEach { section -> yieldAll(section.tracks) }
        yieldAll(input.charts)
    }
    return buildString {
        append(input.languageCode)
        append('|')
        append(
            tracks
                .filter { it.artistBrowseIds.firstOrNull().orEmpty().isNotBlank() }
                .distinctBy { it.artistBrowseIds.first().lowercase() }
                .take(48)
                .joinToString(",") { track ->
                    "${track.artist}:${track.artistBrowseIds.first()}"
                }
        )
    }
}

private fun buildHomeContentFingerprint(
    input: HomeDerivedInput,
    availability: HomeContentAvailability
): String {
    return buildString {
        append(availability.fingerprint())
        append('|')
        append(input.tracks.take(12).joinToString(",") { it.id })
        append('|')
        append(
            input.homeSections.joinToString(",") { section ->
                "${section.title}:${section.tracks.take(4).joinToString(".") { it.id }}"
            }
        )
        append('|')
        append(input.homeAlbums.take(10).joinToString(",") { it.browseId.ifBlank { "${it.title}:${it.artist}" } })
        append('|')
        append(input.charts.take(12).joinToString(",") { it.id })
    }
}

private fun isReliableHomeMusicCandidate(track: Track): Boolean {
    val title = track.title.trim()
    val artist = track.artist.trim()
    if (title.length < 2 || artist.length < 2) return false
    if (artist.equals("YouTube Music", ignoreCase = true) || artist.equals("YouTube", ignoreCase = true)) return false
    return !isLikelyHomePlaylistOrCompilation(track)
}

private fun isVerifiedHomeReleaseSectionTitle(title: String, languageCode: String = "en"): Boolean {
    val normalized = title.trim().lowercase(Locale.ROOT)
    val localized = LevyraStrings.forCode(languageCode).newReleases.trim().lowercase(Locale.ROOT)
    return normalized == localized ||
        normalized.contains("novità") ||
        normalized.contains("nuove uscite") ||
        normalized.contains("appena usciti") ||
        normalized.contains("ultime uscite") ||
        normalized.contains("nuovi album") ||
        normalized.contains("nuovi singoli") ||
        normalized.contains("new releases") ||
        normalized.contains("new release") ||
        normalized.contains("latest releases") ||
        normalized.contains("latest release") ||
        normalized.contains("new albums") ||
        normalized.contains("new singles")
}

private fun isHomeQuickPicksSectionTitle(title: String): Boolean {
    val normalized = title.lowercase(Locale.ROOT)
    return normalized.contains("scelte rapide") ||
        normalized.contains("quick picks") ||
        normalized.contains("quick pick") ||
        normalized.contains("scelte per te")
}

private fun isHomePersonalOrbitSectionTitle(title: String, languageCode: String = "en"): Boolean {
    val normalized = title.trim().lowercase(Locale.ROOT)
    val localized = LevyraStrings.forCode(languageCode).personalOrbitTitle.trim().lowercase(Locale.ROOT)
    return normalized == localized ||
        normalized.contains("nella tua orbita") ||
        normalized.contains("la tua orbita") ||
        normalized.contains("your orbit") ||
        normalized.contains("in your orbit") ||
        normalized.contains("tu órbita") ||
        normalized.contains("ton orbite") ||
        normalized.contains("deine umlaufbahn") ||
        normalized.contains("jouw baan") ||
        normalized.contains("twoja orbita")
}

private fun isHomeSectionVisible(title: String, input: HomeDerivedInput): Boolean {
    if (!input.showNewReleases && isVerifiedHomeReleaseSectionTitle(title, input.languageCode)) return false
    if (!input.showPersonalOrbit && isHomePersonalOrbitSectionTitle(title, input.languageCode)) return false
    if (!input.showResonance && isHomeResonanceSectionTitle(title, input.languageCode)) return false
    return true
}

private fun isHomeResonanceSectionTitle(title: String, languageCode: String): Boolean {
    val normalized = title.trim().lowercase(Locale.ROOT)
    val localized = LevyraStrings.forCode(languageCode).voicesTitle.trim().lowercase(Locale.ROOT)
    return normalized == localized ||
        normalized.contains("voices that resonate") ||
        normalized.contains("voci che risuonano") ||
        normalized.contains("voces que resuenan") ||
        normalized.contains("voix qui résonnent") ||
        normalized.contains("stimmen, die nachklingen")
}

private fun isLikelyHomePlaylistOrCompilation(track: Track): Boolean {
    val combined = listOf(track.title, track.artist, track.album).joinToString(" ").lowercase()
    return listOf(
        "playlist",
        "mix",
        "top hit",
        "top hits",
        "hit italiane",
        "canzoni italiane",
        "musica italiana",
        "estate mix",
        "summer mix",
        "best of",
        "compilation",
        "classifica",
        "radio edit",
        "sped up",
        "slowed",
        "nightcore"
    ).any(combined::contains)
}

private data class HomeProjection(
    val animationsEnabled: Boolean,
    val chartRegions: List<ChartRegion>,
    val charts: List<Track>,
    val currentTrack: Track?,
    val downloadedTrackIds: Set<String>,
    val downloadingTrackIds: Set<String>,
    val favoriteIds: Set<String>,
    val favorites: List<Track>,
    val homeAlbums: List<AlbumHit>,
    val homeArtists: List<ArtistHit>,
    val homeResonanceTracks: List<Track>,
    val homeArtistsLoading: Boolean,
    val homeAlbumsLoading: Boolean,
    val homeSections: List<HomeSection>,
    val isLoadingCharts: Boolean,
    val isLoadingHome: Boolean,
    val isPlaying: Boolean,
    val isResolving: Boolean,
    val languageCode: String,
    val moods: List<Mood>,
    val personalOrbitTracks: List<Track>,
    val playlists: List<Playlist>,
    val recentListens: List<Track>,
    val recentSearches: List<Track>,
    val releaseRadar: List<ReleaseRadarEntry>,
    val selectedChartId: String,
    val selectedMood: Mood?,
    val homeError: String?,
    val playerError: String?,
    val similarArtists: List<ArtistHit>,
    val tracks: List<Track>,
    val userName: String,
    val interfaceSettings: LevyraInterfaceSettings
)

private fun homeProjection(state: LevyraUiState): HomeProjection = HomeProjection(
    animationsEnabled = state.animationsEnabled,
    chartRegions = state.chartRegions,
    charts = state.charts,
    currentTrack = state.currentTrack,
    downloadedTrackIds = state.downloadedTrackIds,
    downloadingTrackIds = state.downloadingTrackIds,
    favoriteIds = state.favoriteIds,
    favorites = state.favorites,
    homeAlbums = state.homeAlbums,
    homeArtists = state.homeArtists,
    homeResonanceTracks = state.homeResonanceTracks,
    homeArtistsLoading = state.homeArtistsLoading,
    homeAlbumsLoading = state.homeAlbumsLoading,
    homeSections = state.homeSections,
    isLoadingCharts = state.isLoadingCharts,
    isLoadingHome = state.isLoadingHome,
    isPlaying = state.isPlaying,
    isResolving = state.isResolving,
    languageCode = state.languageCode,
    moods = state.moods,
    personalOrbitTracks = state.personalOrbitTracks,
    playlists = state.playlists,
    recentListens = state.recentListens,
    recentSearches = state.recentSearches,
    releaseRadar = state.releaseRadar,
    selectedChartId = state.selectedChartId,
    selectedMood = state.selectedMood,
    homeError = state.homeError,
    playerError = state.playerError,
    similarArtists = state.similarArtists,
    tracks = state.tracks,
    userName = state.userName,
    interfaceSettings = state.interfaceSettings
)

private data class SearchProjection(
    val currentTrack: Track?,
    val downloadProgressByTrackId: Map<String, Int>,
    val downloadedTrackIds: Set<String>,
    val downloadingTrackIds: Set<String>,
    val downloads: List<DownloadedTrack>,
    val favoriteIds: Set<String>,
    val isPlaying: Boolean,
    val isResolving: Boolean,
    val isSearching: Boolean,
    val languageCode: String,
    val playlists: List<Playlist>,
    val query: String,
    val recentSearches: List<Track>,
    val searchData: SearchResults,
    val searchError: String?,
    val searchFilter: SearchFilter,
    val searchResults: List<Track>,
    val searchSuggestions: List<String>
)

private fun searchProjection(state: LevyraUiState): SearchProjection = SearchProjection(
    currentTrack = state.currentTrack,
    downloadProgressByTrackId = state.downloadProgressByTrackId,
    downloadedTrackIds = state.downloadedTrackIds,
    downloadingTrackIds = state.downloadingTrackIds,
    downloads = state.downloads,
    favoriteIds = state.favoriteIds,
    isPlaying = state.isPlaying,
    isResolving = state.isResolving,
    isSearching = state.isSearching,
    languageCode = state.languageCode,
    playlists = state.playlists,
    query = state.query,
    recentSearches = state.recentSearches,
    searchData = state.searchData,
    searchError = state.searchError,
    searchFilter = state.searchFilter,
    searchResults = state.searchResults,
    searchSuggestions = state.searchSuggestions
)

private data class ExploreProjection(
    val currentTrack: Track?,
    val exploreTracks: List<Track>,
    val exploreVideos: List<Track>,
    val exploreZoneId: String?,
    val favoriteIds: Set<String>,
    val isExploreLoading: Boolean,
    val isPlaying: Boolean
)

private fun exploreProjection(state: LevyraUiState): ExploreProjection = ExploreProjection(
    currentTrack = state.currentTrack,
    exploreTracks = state.exploreTracks,
    exploreVideos = state.exploreVideos,
    exploreZoneId = state.exploreZoneId,
    favoriteIds = state.favoriteIds,
    isExploreLoading = state.isExploreLoading,
    isPlaying = state.isPlaying
)

private data class LibraryProjection(
    val currentTrack: Track?,
    val downloadProgressByTrackId: Map<String, Int>,
    val downloadedTrackIds: Set<String>,
    val downloadingTrackIds: Set<String>,
    val downloadQueue: List<OfflineDownloadTask>,
    val downloadStorageBytes: Long,
    val downloads: List<DownloadedTrack>,
    val favoriteIds: Set<String>,
    val favorites: List<Track>,
    val followedArtists: List<FollowedArtist>,
    val isPlaying: Boolean,
    val isResolving: Boolean,
    val listeningPulse: ListeningPulse,
    val openPlaylist: Playlist?,
    val playlists: List<Playlist>,
    val recentListens: List<Track>
)

private fun libraryProjection(state: LevyraUiState): LibraryProjection = LibraryProjection(
    currentTrack = state.currentTrack,
    downloadProgressByTrackId = state.downloadProgressByTrackId,
    downloadedTrackIds = state.downloadedTrackIds,
    downloadingTrackIds = state.downloadingTrackIds,
    downloadQueue = state.downloadQueue,
    downloadStorageBytes = state.downloadStorageBytes,
    downloads = state.downloads,
    favoriteIds = state.favoriteIds,
    favorites = state.favorites,
    followedArtists = state.followedArtists,
    isPlaying = state.isPlaying,
    isResolving = state.isResolving,
    listeningPulse = state.listeningPulse,
    openPlaylist = state.openPlaylist,
    playlists = state.playlists,
    recentListens = state.recentListens
)

private data class PlayerProjection(
    val animationsEnabled: Boolean,
    val audioNormalization: Boolean,
    val currentTrack: Track?,
    val durationMs: Long,
    val favoriteIds: Set<String>,
    val isOfflineExporting: Boolean,
    val isPlaying: Boolean,
    val isResolving: Boolean,
    val isVideoMode: Boolean,
    val lyrics: List<LyricLine>,
    val lyricsLoading: Boolean,
    val playbackSpeed: Float,
    val playerError: String?,
    val positionMs: Long,
    val repeatMode: RepeatMode,
    val shuffleEnabled: Boolean,
    val sleepTimerMinutes: Int,
    val interfaceSettings: LevyraInterfaceSettings,
    val youtubeEngagement: YoutubeEngagementState
)

private fun playerProjection(state: LevyraUiState): PlayerProjection = PlayerProjection(
    animationsEnabled = state.animationsEnabled,
    audioNormalization = state.audioNormalization,
    currentTrack = state.currentTrack,
    durationMs = state.durationMs,
    favoriteIds = state.favoriteIds,
    isOfflineExporting = state.isOfflineExporting,
    isPlaying = state.isPlaying,
    isResolving = state.isResolving,
    isVideoMode = state.isVideoMode,
    lyrics = state.lyrics,
    lyricsLoading = state.lyricsLoading,
    playbackSpeed = state.playbackSpeed,
    playerError = state.playerError,
    positionMs = state.positionMs,
    repeatMode = state.repeatMode,
    shuffleEnabled = state.shuffleEnabled,
    sleepTimerMinutes = state.sleepTimerMinutes,
    interfaceSettings = state.interfaceSettings,
    youtubeEngagement = state.youtubeEngagement
)

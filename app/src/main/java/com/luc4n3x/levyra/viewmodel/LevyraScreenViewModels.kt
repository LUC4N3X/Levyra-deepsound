package com.luc4n3x.levyra.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.data.HomeContentAvailability
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.ChartRegion
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.FollowedArtist
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.ListeningPulse
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.Mood
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.ReleaseRadarEntry
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    internal val renderState: StateFlow<HomeRenderSnapshot> = state
        .scan(buildHomeRenderSnapshot(root.state.value)) { previous, snapshot ->
            withContext(Dispatchers.Default) { buildHomeRenderSnapshot(snapshot, previous) }
        }
        .distinctUntilChanged()
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
    fun onHomeEntered() = root.onHomeEntered()
    fun onHomeLeft() = root.onHomeLeft()
    fun setHomeViewport(scrollInProgress: Boolean, atTop: Boolean) = root.setHomeViewport(scrollInProgress, atTop)
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
    fun addToQueue(track: Track) = root.addToQueue(track)
    fun closePlaylist() = root.closePlaylist()
    fun createPlaylist(name: String, firstTrack: Track? = null) = root.createPlaylist(name, firstTrack)
    fun deletePlaylist(playlistId: String) = root.deletePlaylist(playlistId)
    fun exportOpenPlaylist() = root.exportOpenPlaylist()
    fun exportTrack(track: Track) = root.exportTrack(track)
    fun openArtist(track: Track) = root.openArtist(track)
    fun openArtistByName(name: String) = root.openArtistByName(name)
    fun openPlayerScreen() = root.openPlayerScreen()
    fun openPlaylist(playlistId: String) = root.openPlaylist(playlistId)
    fun playFrom(list: List<Track>, track: Track, loopOnCompletion: Boolean = false) = root.playFrom(list, track, loopOnCompletion)
    fun playPlaylist(playlistId: String, startTrackId: String? = null) = root.playPlaylist(playlistId, startTrackId)
    fun removeFromPlaylist(playlistId: String, trackId: String) = root.removeFromPlaylist(playlistId, trackId)
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

@Immutable
internal data class HomeRenderSnapshot(
    val state: LevyraUiState,
    val derived: HomeDerivedState
)

@Immutable
internal data class HomeDerivedState(
    val resonanceTracks: List<Track>,
    val artistRefreshFingerprint: String,
    val newReleases: HomeSection?,
    val otherSections: List<HomeSection>,
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
    val releaseRadar: List<ReleaseRadarEntry>,
    val similarArtists: List<ArtistHit>,
    val currentTrack: Track?
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
        previous.releaseRadar === current.releaseRadar &&
        previous.similarArtists === current.similarArtists
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
        releaseRadar = releaseRadar,
        similarArtists = similarArtists,
        currentTrack = currentTrack
    )
}

private fun buildHomeDerivedState(input: HomeDerivedInput): HomeDerivedState {
    val newReleases = input.homeSections.firstOrNull { isVerifiedHomeReleaseSectionTitle(it.title) }
    val otherSections = input.homeSections.filter {
        !isVerifiedHomeReleaseSectionTitle(it.title) &&
            !isHomeQuickPicksSectionTitle(it.title) &&
            !isHomePersonalOrbitSectionTitle(it.title)
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
    return HomeDerivedState(
        resonanceTracks = buildHomeResonanceTracks(input),
        artistRefreshFingerprint = buildHomeArtistRefreshFingerprint(input),
        newReleases = newReleases,
        otherSections = otherSections,
        chartChunks = input.charts.chunked(4),
        contentAvailability = contentAvailability,
        contentFingerprint = buildHomeContentFingerprint(input, contentAvailability)
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

private fun isVerifiedHomeReleaseSectionTitle(title: String): Boolean {
    val normalized = title.lowercase(Locale.ROOT)
    return normalized.contains("novità") ||
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

private fun isHomePersonalOrbitSectionTitle(title: String): Boolean {
    val normalized = title.lowercase(Locale.ROOT)
    return normalized.contains("nella tua orbita") ||
        normalized.contains("la tua orbita") ||
        normalized.contains("your orbit") ||
        normalized.contains("in your orbit") ||
        normalized.contains("tu órbita") ||
        normalized.contains("ton orbite") ||
        normalized.contains("deine umlaufbahn") ||
        normalized.contains("jouw baan") ||
        normalized.contains("twoja orbita")
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
    val favoriteIds: Set<String>,
    val favorites: List<Track>,
    val homeAlbums: List<AlbumHit>,
    val homeArtists: List<ArtistHit>,
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
    favoriteIds = state.favoriteIds,
    favorites = state.favorites,
    homeAlbums = state.homeAlbums,
    homeArtists = state.homeArtists,
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
    val interfaceSettings: LevyraInterfaceSettings
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
    interfaceSettings = state.interfaceSettings
)

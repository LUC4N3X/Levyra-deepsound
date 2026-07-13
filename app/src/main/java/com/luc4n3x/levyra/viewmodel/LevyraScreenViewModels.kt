package com.luc4n3x.levyra.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.ChartRegion
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.FollowedArtist
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.LevyraTab
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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

class HomeViewModel(root: LevyraViewModel) : LevyraScreenViewModel(root, ::homeProjection) {
    fun addToPlaylist(playlistId: String, track: Track) = root.addToPlaylist(playlistId, track)
    fun addToQueue(track: Track) = root.addToQueue(track)
    fun createPlaylist(name: String, firstTrack: Track? = null) = root.createPlaylist(name, firstTrack)
    fun openAlbum(album: AlbumHit) = root.openAlbum(album)
    fun openArtistByName(name: String) = root.openArtistByName(name)
    fun openArtistFromHit(hit: ArtistHit) = root.openArtistFromHit(hit)
    fun openSettings() = root.openSettings()
    fun playAlbumRecommendations(albums: List<AlbumHit>) = root.playAlbumRecommendations(albums)
    fun playAll(tracks: List<Track>) = root.playAll(tracks)
    fun playFrom(list: List<Track>, track: Track, loopOnCompletion: Boolean = false) = root.playFrom(list, track, loopOnCompletion)
    fun searchNow() = root.searchNow()
    fun searchNow(query: String) = root.searchNow(query)
    fun selectChart(regionId: String) = root.selectChart(regionId)
    fun selectMood(mood: Mood) = root.selectMood(mood)
    fun toggleFavorite(track: Track) = root.toggleFavorite(track)
    fun togglePlay() = root.togglePlay()
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

private data class HomeProjection(
    val animationsEnabled: Boolean,
    val chartRegions: List<ChartRegion>,
    val charts: List<Track>,
    val currentTrack: Track?,
    val durationMs: Long,
    val favoriteIds: Set<String>,
    val favorites: List<Track>,
    val homeAlbums: List<AlbumHit>,
    val homeAlbumsLoading: Boolean,
    val homeSections: List<HomeSection>,
    val isLoadingCharts: Boolean,
    val isPlaying: Boolean,
    val isResolving: Boolean,
    val moods: List<Mood>,
    val personalOrbitTracks: List<Track>,
    val playlists: List<Playlist>,
    val positionMs: Long,
    val recentSearches: List<Track>,
    val releaseRadar: List<ReleaseRadarEntry>,
    val selectedChartId: String,
    val selectedMood: Mood?,
    val similarArtists: List<ArtistHit>,
    val tracks: List<Track>,
    val userName: String
)

private fun homeProjection(state: LevyraUiState): HomeProjection = HomeProjection(
    animationsEnabled = state.animationsEnabled,
    chartRegions = state.chartRegions,
    charts = state.charts,
    currentTrack = state.currentTrack,
    durationMs = state.durationMs,
    favoriteIds = state.favoriteIds,
    favorites = state.favorites,
    homeAlbums = state.homeAlbums,
    homeAlbumsLoading = state.homeAlbumsLoading,
    homeSections = state.homeSections,
    isLoadingCharts = state.isLoadingCharts,
    isPlaying = state.isPlaying,
    isResolving = state.isResolving,
    moods = state.moods,
    personalOrbitTracks = state.personalOrbitTracks,
    playlists = state.playlists,
    positionMs = state.positionMs,
    recentSearches = state.recentSearches,
    releaseRadar = state.releaseRadar,
    selectedChartId = state.selectedChartId,
    selectedMood = state.selectedMood,
    similarArtists = state.similarArtists,
    tracks = state.tracks,
    userName = state.userName
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
    val durationMs: Long,
    val favoriteIds: Set<String>,
    val favorites: List<Track>,
    val followedArtists: List<FollowedArtist>,
    val isPlaying: Boolean,
    val isResolving: Boolean,
    val listeningPulse: ListeningPulse,
    val openPlaylist: Playlist?,
    val playlists: List<Playlist>,
    val positionMs: Long,
    val recentListens: List<Track>
)

private fun libraryProjection(state: LevyraUiState): LibraryProjection = LibraryProjection(
    currentTrack = state.currentTrack,
    downloadProgressByTrackId = state.downloadProgressByTrackId,
    downloadedTrackIds = state.downloadedTrackIds,
    downloadingTrackIds = state.downloadingTrackIds,
    downloads = state.downloads,
    durationMs = state.durationMs,
    favoriteIds = state.favoriteIds,
    favorites = state.favorites,
    followedArtists = state.followedArtists,
    isPlaying = state.isPlaying,
    isResolving = state.isResolving,
    listeningPulse = state.listeningPulse,
    openPlaylist = state.openPlaylist,
    playlists = state.playlists,
    positionMs = state.positionMs,
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
    val sleepTimerMinutes: Int
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
    sleepTimerMinutes = state.sleepTimerMinutes
)

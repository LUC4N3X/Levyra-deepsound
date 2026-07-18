package com.luc4n3x.levyra.viewmodel

import androidx.compose.runtime.Immutable
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.AlbumDetail
import com.luc4n3x.levyra.domain.CacheReport
import com.luc4n3x.levyra.domain.AppUpdateInfo
import com.luc4n3x.levyra.domain.ChartRegion
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.FollowedArtist
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.LevyraDownloadSettings
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.LevyraIntelligenceSummary
import com.luc4n3x.levyra.domain.OfflineDownloadTask
import com.luc4n3x.levyra.domain.ListeningPulse
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricSection
import com.luc4n3x.levyra.domain.Mood
import com.luc4n3x.levyra.domain.ReleaseRadarEntry
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.SmartMusicProfile
import com.luc4n3x.levyra.domain.Taste
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.theme.LevyraThemes

enum class DetailReturnTarget {
    None,
    Album,
    Artist
}

@Immutable
data class LevyraUiState(
    val selectedTab: LevyraTab = LevyraTab.Home,
    val moods: List<Mood> = emptyList(),
    val tastes: List<Taste> = emptyList(),
    val showOnboarding: Boolean = false,
    val isVideoMode: Boolean = false,
    val showSettings: Boolean = false,
    val animationsEnabled: Boolean = true,
    val dynamicColor: Boolean = true,
    val userName: String = "",
    val languageCode: String = "en",
    val selectedMood: Mood? = null,
    val tracks: List<Track> = emptyList(),
    val queue: List<Track> = emptyList(),
    val queueCurrentIndex: Int = -1,
    val queueUndoAvailable: Boolean = false,
    val queueHistoryCount: Int = 0,
    val radioEnabled: Boolean = true,
    val searchResults: List<Track> = emptyList(),
    val recentSearches: List<Track> = emptyList(),
    val personalOrbitTracks: List<Track> = emptyList(),
    val searchSuggestions: List<String> = emptyList(),
    val charts: List<Track> = emptyList(),
    val chartRegions: List<ChartRegion> = emptyList(),
    val selectedChartId: String = "it",
    val isLoadingCharts: Boolean = false,
    val homeSections: List<HomeSection> = emptyList(),
    val homeAlbums: List<AlbumHit> = emptyList(),
    val homeArtists: List<ArtistHit> = emptyList(),
    val homeResonanceTracks: List<Track> = emptyList(),
    val homeResonanceUpdatedAt: Long = 0L,
    val homeArtistsLoading: Boolean = false,
    val homeAlbumsLoading: Boolean = false,
    val isLoadingHome: Boolean = false,
    val homeError: String? = null,
    val showAlbum: Boolean = false,
    val albumLoading: Boolean = false,
    val albumError: String? = null,
    val albumDetail: AlbumDetail? = null,
    val detailReturnTarget: DetailReturnTarget = DetailReturnTarget.None,
    val favorites: List<Track> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val playlists: List<com.luc4n3x.levyra.domain.Playlist> = emptyList(),
    val openPlaylist: com.luc4n3x.levyra.domain.Playlist? = null,
    val currentTrack: Track? = null,
    val lyrics: List<LyricLine> = emptyList(),
    val lyricsSections: List<LyricSection> = emptyList(),
    val activeLyric: LyricLine? = null,
    val showLyrics: Boolean = false,
    val lyricsLoading: Boolean = false,
    val lyricsSynced: Boolean = false,
    val lyricsProvider: String = "",
    val lyricsConfidence: Int = 0,
    val lyricsCached: Boolean = false,
    val lyricsTranslationEnabled: Boolean = false,
    val smartProfile: SmartMusicProfile = SmartMusicProfile(),
    val cacheReport: CacheReport = CacheReport(0, 0, 0, 0),
    val query: String = "",
    val isPlaying: Boolean = false,
    val isSearching: Boolean = false,
    val isResolving: Boolean = false,
    val searchError: String? = null,
    val playerError: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val smartScore: Int = 94,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val shuffleEnabled: Boolean = false,
    val playbackSpeed: Float = 1f,
    val audioQuality: String = "Auto",
    val showAudioQualityPanel: Boolean = false,
    val audioNormalization: Boolean = false,
    val audioSettings: LevyraAudioSettings = LevyraAudioSettings(),
    val sleepTimerMinutes: Int = 0,
    val sponsorBlockEnabled: Boolean = true,
    val skipSilence: Boolean = false,
    val showQueue: Boolean = false,
    val isOfflineExporting: Boolean = false,
    val offlineExportMessage: String? = null,
    val embeddedMetadataWriterReady: Boolean = false,
    val updateInfo: AppUpdateInfo? = null,
    val isCheckingUpdates: Boolean = false,
    val updateMessage: String? = null,
    val showUpdatePrompt: Boolean = false,
    val downloads: List<DownloadedTrack> = emptyList(),
    val exploreZoneId: String? = null,
    val exploreTracks: List<Track> = emptyList(),
    val exploreVideos: List<Track> = emptyList(),
    val isExploreLoading: Boolean = false,
    val downloadingTrackIds: Set<String> = emptySet(),
    val downloadedTrackIds: Set<String> = emptySet(),
    val downloadProgressByTrackId: Map<String, Int> = emptyMap(),
    val downloadTitleByTrackId: Map<String, String> = emptyMap(),
    val offlineQueueSize: Int = 0,
    val downloadQueue: List<OfflineDownloadTask> = emptyList(),
    val showArtist: Boolean = false,
    val artistLoading: Boolean = false,
    val artistError: String? = null,
    val artistProfile: ArtistProfile? = null,
    val searchData: SearchResults = SearchResults(),
    val searchFilter: SearchFilter = SearchFilter.All,
    val themePreset: String = LevyraThemes.COSMIC,
    val interfaceSettings: LevyraInterfaceSettings = LevyraInterfaceSettings(),
    val downloadSettings: LevyraDownloadSettings = LevyraDownloadSettings(),
    val intelligenceSummary: LevyraIntelligenceSummary = LevyraIntelligenceSummary(),
    val backupMessage: String? = null,
    val playbackDiagnostics: String = "",
    val listeningPulse: ListeningPulse = ListeningPulse(),
    val recentListens: List<Track> = emptyList(),
    val followedArtists: List<FollowedArtist> = emptyList(),
    val followedArtistKeys: Set<String> = emptySet(),
    val releaseRadar: List<ReleaseRadarEntry> = emptyList(),
    val similarArtists: List<ArtistHit> = emptyList()
)

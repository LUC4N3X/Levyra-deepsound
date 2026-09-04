package com.luc4n3x.levyra.viewmodel

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.luc4n3x.levyra.data.VaultPreview
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
import com.luc4n3x.levyra.domain.LevyraBackupSettings
import com.luc4n3x.levyra.domain.LevyraVaultStatus
import com.luc4n3x.levyra.domain.LevyraIntelligenceSummary
import com.luc4n3x.levyra.domain.BatchDownload
import com.luc4n3x.levyra.domain.OfflineDownloadTask
import com.luc4n3x.levyra.domain.ListeningDna
import com.luc4n3x.levyra.domain.ListeningDnaPeriod
import com.luc4n3x.levyra.domain.LevyraMixDefaults
import com.luc4n3x.levyra.domain.LevyraMixSummary
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
import com.luc4n3x.levyra.domain.ResonanceCommentSnippet
import com.luc4n3x.levyra.domain.YoutubeEngagementState
import com.luc4n3x.levyra.data.LyricsRepository
import com.luc4n3x.levyra.domain.LevyraNetworkSettings
import com.luc4n3x.levyra.domain.LevyraNetworkSettingsError
import com.luc4n3x.levyra.domain.LevyraNetworkTestOutcome
import com.luc4n3x.levyra.feature.jam.JamUiState
import com.luc4n3x.levyra.feature.motion.MotionArtwork
import com.luc4n3x.levyra.feature.recognition.RecognitionHistoryEntry
import com.luc4n3x.levyra.feature.sharedmedia.SharedMediaPreview
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
    val pendingVideoMode: Boolean? = null,
    val selectedVideoSubtitleId: String? = null,
    val isSamplesOpen: Boolean = false,
    val showSettings: Boolean = false,
    val animationsEnabled: Boolean = true,
    val motionArtworkEnabled: Boolean = true,
    val recognitionAvailable: Boolean = true,
    val lastFmConfigured: Boolean = false,
    val lastFmAuthorizationPending: Boolean = false,
    val listenBrainzConfigured: Boolean = false,
    val audDConfigured: Boolean = false,
    val recognitionState: com.luc4n3x.levyra.feature.recognition.RecognitionState =
        com.luc4n3x.levyra.feature.recognition.RecognitionState.Idle,
    val showRecognition: Boolean = false,
    val recognitionHistory: List<RecognitionHistoryEntry> = emptyList(),
    val recognitionMatch: Track? = null,
    val recognitionMatching: Boolean = false,
    val recognitionDeviceCaptureSupported: Boolean = false,
    val jam: JamUiState = JamUiState(),
    val jamDisplayName: String = "",
    val showJam: Boolean = false,
    val networkSettings: LevyraNetworkSettings = LevyraNetworkSettings(),
    val networkProxyPasswordSet: Boolean = false,
    val networkTesting: Boolean = false,
    val networkTestOutcome: LevyraNetworkTestOutcome? = null,
    val networkErrors: List<LevyraNetworkSettingsError> = emptyList(),
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
    val homeResonanceComments: Map<String, ResonanceCommentSnippet> = emptyMap(),
    val homeArtistsLoading: Boolean = false,
    val homeAlbumsLoading: Boolean = false,
    val isLoadingHome: Boolean = false,
    val homeError: String? = null,
    val showAlbum: Boolean = false,
    val albumLoading: Boolean = false,
    val albumError: String? = null,
    val albumDetail: AlbumDetail? = null,
    val albumMotionArtwork: MotionArtwork? = null,
    val detailReturnTarget: DetailReturnTarget = DetailReturnTarget.None,
    val favorites: List<Track> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val playlists: List<com.luc4n3x.levyra.domain.Playlist> = emptyList(),
    val openPlaylist: com.luc4n3x.levyra.domain.Playlist? = null,
    val currentTrack: Track? = null,
    val motionArtwork: MotionArtwork? = null,
    val motionArtworkLoading: Boolean = false,
    val youtubeEngagement: YoutubeEngagementState = YoutubeEngagementState(),
    val lyrics: List<LyricLine> = emptyList(),
    val lyricsSections: List<LyricSection> = emptyList(),
    val activeLyric: LyricLine? = null,
    val showLyrics: Boolean = false,
    val lyricsLoading: Boolean = false,
    val lyricsSynced: Boolean = false,
    val lyricsProvider: String = "",
    val lyricsConfidence: Int = 0,
    val lyricsCached: Boolean = false,
    val lyricsVersions: List<LyricsRepository.LyricsVersion> = emptyList(),
    val lyricsVersionsLoading: Boolean = false,
    val lyricsManualSelection: Boolean = false,
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
    val bufferedPositionMs: Long = 0L,
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
    val sleepTimerEndOfTrack: Boolean = false,
    val sleepTimerDeadlineElapsedRealtimeMs: Long = 0L,
    val sleepTimerTotalMs: Long = 0L,
    val showSleepTimer: Boolean = false,
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
    val downloadStorageBytes: Long = 0L,
    val exploreZoneId: String? = null,
    val exploreTracks: List<Track> = emptyList(),
    val exploreFreshTracks: List<Track> = emptyList(),
    val exploreNewReleases: List<AlbumHit> = emptyList(),
    val exploreVideos: List<Track> = emptyList(),
    val exploreSamples: List<Track> = emptyList(),
    val isFreshCurrentsLoading: Boolean = false,
    val isNewReleasesLoading: Boolean = false,
    val newReleasesLoadFailed: Boolean = false,
    val isSamplesLoading: Boolean = false,
    val samplesLoadFailed: Boolean = false,
    val isExploreLoading: Boolean = false,
    val downloadingTrackIds: Set<String> = emptySet(),
    val downloadedTrackIds: Set<String> = emptySet(),
    val downloadProgressByTrackId: Map<String, Int> = emptyMap(),
    val downloadTitleByTrackId: Map<String, String> = emptyMap(),
    val offlineQueueSize: Int = 0,
    val downloadQueue: List<OfflineDownloadTask> = emptyList(),
    val downloadBatches: List<BatchDownload> = emptyList(),
    val showArtist: Boolean = false,
    val artistLoading: Boolean = false,
    val artistError: String? = null,
    val artistProfile: ArtistProfile? = null,
    val artistMotionArtwork: MotionArtwork? = null,
    val artistListStateKey: String = "",
    val searchData: SearchResults = SearchResults(),
    val searchFilter: SearchFilter = SearchFilter.All,
    val searchSectionContinuations: Map<SearchFilter, String> = emptyMap(),
    val searchSectionLoading: Set<SearchFilter> = emptySet(),
    val themePreset: String = LevyraThemes.COSMIC,
    val interfaceSettings: LevyraInterfaceSettings = LevyraInterfaceSettings(),
    val downloadSettings: LevyraDownloadSettings = LevyraDownloadSettings(),
    val backupSettings: LevyraBackupSettings = LevyraBackupSettings(),
    val vaultStatus: LevyraVaultStatus = LevyraVaultStatus.Idle,
    val backupPreview: VaultPreview? = null,
    val pendingRestoreUri: Uri? = null,
    val lastBackupAtMs: Long = 0L,
    val backupLocationUri: String? = null,
    val preUpdateBackupFailed: Boolean = false,
    val sharedMediaPreview: SharedMediaPreview? = null,
    val intelligenceSummary: LevyraIntelligenceSummary = LevyraIntelligenceSummary(),
    val backupMessage: String? = null,
    val playbackDiagnostics: String = "",
    val listeningPulse: ListeningPulse = ListeningPulse(),
    val recentListens: List<Track> = emptyList(),
    val mostPlayedTracks: List<Track> = emptyList(),
    val followedArtists: List<FollowedArtist> = emptyList(),
    val followedArtistKeys: Set<String> = emptySet(),
    val releaseRadar: List<ReleaseRadarEntry> = emptyList(),
    val similarArtists: List<ArtistHit> = emptyList(),
    val mixFamiliarity: Float = LevyraMixDefaults.Familiarity,
    val mixLoading: Boolean = false,
    val activeMix: LevyraMixSummary? = null,
    val mixMessage: String? = null,
    val showYourSound: Boolean = false,
    val listeningDnaPeriod: ListeningDnaPeriod = ListeningDnaPeriod.Month,
    val listeningDna: ListeningDna = ListeningDna(),
    val listeningDnaLoading: Boolean = false
)

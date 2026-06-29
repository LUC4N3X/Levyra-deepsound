package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.CacheReport
import com.luc4n3x.levyra.domain.ChartRegion
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.Mood
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Taste
import com.luc4n3x.levyra.domain.Track

data class LevyraUiState(
    val selectedTab: LevyraTab = LevyraTab.Home,
    val moods: List<Mood> = emptyList(),
    val tastes: List<Taste> = emptyList(),
    val showOnboarding: Boolean = false,
    val showSettings: Boolean = false,
    val animationsEnabled: Boolean = true,
    val dynamicColor: Boolean = true,
    val userName: String = "",
    val selectedMood: Mood? = null,
    val tracks: List<Track> = emptyList(),
    val queue: List<Track> = emptyList(),
    val searchResults: List<Track> = emptyList(),
    val recentSearches: List<Track> = emptyList(),
    val searchSuggestions: List<String> = emptyList(),
    val charts: List<Track> = emptyList(),
    val chartRegions: List<ChartRegion> = emptyList(),
    val selectedChartId: String = "it",
    val isLoadingCharts: Boolean = false,
    val homeSections: List<HomeSection> = emptyList(),
    val favorites: List<Track> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val currentTrack: Track? = null,
    val lyrics: List<LyricLine> = emptyList(),
    val activeLyric: LyricLine? = null,
    val showLyrics: Boolean = false,
    val lyricsLoading: Boolean = false,
    val lyricsSynced: Boolean = false,
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
    val sleepTimerMinutes: Int = 0,
    val sponsorBlockEnabled: Boolean = true,
    val skipSilence: Boolean = false,
    val showQueue: Boolean = false,
    val isOfflineExporting: Boolean = false,
    val offlineExportMessage: String? = null,
    val embeddedMetadataWriterReady: Boolean = false
)

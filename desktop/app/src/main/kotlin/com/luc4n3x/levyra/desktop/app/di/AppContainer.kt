package com.luc4n3x.levyra.desktop.app.di

import com.luc4n3x.levyra.desktop.app.state.CatalogController
import com.luc4n3x.levyra.desktop.app.state.DiscoverController
import com.luc4n3x.levyra.desktop.app.state.LevyraAppModel
import com.luc4n3x.levyra.desktop.app.state.LyricsController
import com.luc4n3x.levyra.desktop.app.state.PlaybackController
import com.luc4n3x.levyra.desktop.core.catalog.CatalogRepository
import com.luc4n3x.levyra.desktop.core.charts.ChartsRepository
import com.luc4n3x.levyra.desktop.core.extractor.ExtractorRuntime
import com.luc4n3x.levyra.desktop.core.lyrics.LyricsRepository
import com.luc4n3x.levyra.desktop.core.storage.AppPaths
import com.luc4n3x.levyra.desktop.core.storage.LibraryStore
import com.luc4n3x.levyra.desktop.core.storage.SessionStore
import com.luc4n3x.levyra.desktop.core.storage.SettingsStore
import com.luc4n3x.levyra.desktop.core.storage.WindowPlacementStore
import com.luc4n3x.levyra.desktop.core.stream.YoutubeStreamResolver
import com.luc4n3x.levyra.desktop.player.AudioPlayer
import com.luc4n3x.levyra.desktop.player.VlcAudioPlayer
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class AppContainer {
    val paths: AppPaths = AppPaths.default()
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsStore: SettingsStore = SettingsStore.create(paths)
    val libraryStore: LibraryStore = LibraryStore.create(paths)
    val sessionStore: SessionStore = SessionStore.create(paths)
    val windowPlacementStore: WindowPlacementStore = WindowPlacementStore.create(paths)

    private val catalogRepository = CatalogRepository()
    private val streamResolver = YoutubeStreamResolver()
    private val chartsRepository = ChartsRepository()
    private val lyricsRepository = LyricsRepository()

    val playbackController: PlaybackController = PlaybackController(
        scope = scope,
        resolver = streamResolver,
        catalog = catalogRepository,
        settingsStore = settingsStore,
        libraryStore = libraryStore,
        sessionStore = sessionStore,
        playerFactory = ::createAudioPlayer
    )

    private val catalogController = CatalogController(
        scope = scope,
        catalog = catalogRepository,
        libraryStore = libraryStore
    )

    private val discoverController = DiscoverController(
        scope = scope,
        charts = chartsRepository
    )

    private val lyricsController = LyricsController(
        scope = scope,
        repository = lyricsRepository
    )

    val appModel: LevyraAppModel = LevyraAppModel(
        scope = scope,
        paths = paths,
        settingsStore = settingsStore,
        libraryStore = libraryStore,
        catalogController = catalogController,
        discoverController = discoverController,
        lyricsController = lyricsController,
        playbackController = playbackController
    )

    init {
        val settings = settingsStore.current
        ExtractorRuntime.ensureInitialized(settings.language, settings.contentCountry)
        if (settings.resumeOnStartup) {
            playbackController.restoreSession()
        }
    }

    fun shutdown() {
        playbackController.shutdown()
        scope.cancel()
    }

    private fun createAudioPlayer(): AudioPlayer = VlcAudioPlayer.create(
        preferredDirectory = settingsStore.current.vlcDirectory,
        bundledDirectory = bundledVlcDirectory()
    )

    companion object {
        fun bundledVlcDirectory(): Path? {
            val resources = System.getProperty("compose.application.resources.dir").orEmpty()
            if (resources.isBlank()) return null
            return Paths.get(resources, "vlc")
        }
    }
}

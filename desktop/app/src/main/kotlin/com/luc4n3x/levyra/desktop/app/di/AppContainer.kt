package com.luc4n3x.levyra.desktop.app.di

import com.luc4n3x.levyra.desktop.app.state.CatalogController
import com.luc4n3x.levyra.desktop.app.state.DesktopUpdateController
import com.luc4n3x.levyra.desktop.app.state.DiscoverController
import com.luc4n3x.levyra.desktop.app.state.LevyraAppModel
import com.luc4n3x.levyra.desktop.app.state.LocalMusicController
import com.luc4n3x.levyra.desktop.app.state.LyricsController
import com.luc4n3x.levyra.desktop.app.state.OfflineDownloadController
import com.luc4n3x.levyra.desktop.app.state.PlaybackController
import com.luc4n3x.levyra.desktop.app.state.SoundtrackController
import com.luc4n3x.levyra.desktop.app.state.MoodsController
import com.luc4n3x.levyra.desktop.app.state.HomeFeedController
import com.luc4n3x.levyra.desktop.app.state.RadioController
import com.luc4n3x.levyra.desktop.app.state.ExploreController
import com.luc4n3x.levyra.desktop.app.state.PodcastController
import com.luc4n3x.levyra.desktop.core.moods.MoodsRepository
import com.luc4n3x.levyra.desktop.core.podcast.PodcastRepository
import com.luc4n3x.levyra.desktop.core.podcast.PodcastLibraryStore
import com.luc4n3x.levyra.desktop.core.catalog.CatalogRepository
import com.luc4n3x.levyra.desktop.core.stream.DirectStreamResolver
import com.luc4n3x.levyra.desktop.core.charts.ChartsRepository
import com.luc4n3x.levyra.desktop.core.charts.EditorialCatalog
import com.luc4n3x.levyra.desktop.core.extractor.ExtractorRuntime
import com.luc4n3x.levyra.desktop.core.localmusic.LocalLibraryStore
import com.luc4n3x.levyra.desktop.core.lyrics.LyricsRepository
import com.luc4n3x.levyra.desktop.core.sponsorblock.SponsorBlockRepository
import com.luc4n3x.levyra.desktop.core.storage.AppPaths
import com.luc4n3x.levyra.desktop.core.storage.DownloadStore
import com.luc4n3x.levyra.desktop.core.storage.LibraryStore
import com.luc4n3x.levyra.desktop.core.storage.SessionStore
import com.luc4n3x.levyra.desktop.core.storage.SettingsStore
import com.luc4n3x.levyra.desktop.core.storage.WindowPlacementStore
import com.luc4n3x.levyra.desktop.core.stream.YoutubeStreamResolver
import com.luc4n3x.levyra.desktop.core.stream.DesktopHighQualityResolver
import com.luc4n3x.levyra.domain.HighQualityAudioMode
import com.luc4n3x.levyra.desktop.player.AudioPlayer
import com.luc4n3x.levyra.desktop.player.VlcAudioPlayer
import com.luc4n3x.levyra.desktop.player.VlcMp3Transcoder
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
    val downloadStore: DownloadStore = DownloadStore.create(paths)
    val windowPlacementStore: WindowPlacementStore = WindowPlacementStore.create(paths)
    val localLibraryStore: LocalLibraryStore = LocalLibraryStore.create(paths)

    private val initialSettings = settingsStore.current.also { settings ->
        ExtractorRuntime.ensureInitialized(settings.language, settings.contentCountry)
    }
    private val catalogRepository = CatalogRepository()
    private val youtubeResolver = YoutubeStreamResolver()
    private val streamResolver = DirectStreamResolver(DesktopHighQualityResolver.create(scope, youtubeResolver, paths.root.resolve("cache"),
        { HighQualityAudioMode.fromStorage(settingsStore.current.highQualityAudioMode) }))
    private val downloadResolver = DirectStreamResolver(DesktopHighQualityResolver.create(scope, YoutubeStreamResolver(), paths.root.resolve("cache"),
        { HighQualityAudioMode.fromStorage(settingsStore.current.highQualityAudioMode) }, forDownload = true))
    private val chartsRepository = ChartsRepository(editorial = EditorialCatalog(paths.root.resolve("cache/editorial.json")))
    private val lyricsRepository = LyricsRepository()
    private val sponsorBlockRepository = SponsorBlockRepository()

    internal val updateController: DesktopUpdateController = DesktopUpdateController(
        scope = scope,
        paths = paths
    )

    val playbackController: PlaybackController = PlaybackController(
        scope = scope,
        resolver = streamResolver,
        catalog = catalogRepository,
        settingsStore = settingsStore,
        libraryStore = libraryStore,
        sessionStore = sessionStore,
        sponsorBlock = sponsorBlockRepository,
        playerFactory = ::createAudioPlayer
    )

    val downloadController: OfflineDownloadController = OfflineDownloadController(
        scope = scope,
        paths = paths,
        resolver = downloadResolver,
        catalog = catalogRepository,
        settingsStore = settingsStore,
        store = downloadStore,
        transcodeToMp3 = { source, target ->
            VlcMp3Transcoder.transcode(
                source = source,
                target = target,
                preferredDirectory = settingsStore.current.vlcDirectory,
                bundledDirectory = bundledVlcDirectory()
            )
        }
    )

    private val localMusicController = LocalMusicController(
        scope = scope,
        store = localLibraryStore,
        paths = paths
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

    private val soundtrackController = SoundtrackController(
        scope = scope,
        catalog = catalogRepository
    )

    private val lyricsController = LyricsController(
        scope = scope,
        repository = lyricsRepository
    )

    private val moodsRepository = MoodsRepository()

    private val moodsController = MoodsController(scope, moodsRepository)

    private val homeFeedController = HomeFeedController(scope, catalogRepository, libraryStore, moodsRepository)

    private val radioController = RadioController(scope, paths)

    private val exploreController = ExploreController(scope, chartsRepository)

    private val podcastController = PodcastController(scope, PodcastRepository(), PodcastLibraryStore.create(paths), playbackController)

    val appModel: LevyraAppModel = LevyraAppModel(
        scope = scope,
        paths = paths,
        settingsStore = settingsStore,
        libraryStore = libraryStore,
        catalogController = catalogController,
        discoverController = discoverController,
        soundtrackController = soundtrackController,
        moodsController = moodsController,
        homeFeedController = homeFeedController,
        radioController = radioController,
        exploreController = exploreController,
        podcastController = podcastController,
        lyricsController = lyricsController,
        playbackController = playbackController,
        downloadController = downloadController,
        localMusicController = localMusicController
    )

    init {
        if (initialSettings.resumeOnStartup) {
            playbackController.restoreSession()
        }
    }

    fun shutdown() {
        localMusicController.shutdown()
        updateController.shutdown()
        downloadController.shutdown()
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

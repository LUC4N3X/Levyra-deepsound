package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.extractor.ExtractorRuntime
import com.luc4n3x.levyra.desktop.core.model.CollectionRef
import com.luc4n3x.levyra.desktop.core.model.DesktopSettings
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.storage.AppPaths
import com.luc4n3x.levyra.desktop.core.storage.LibraryData
import com.luc4n3x.levyra.desktop.core.storage.LibraryStore
import com.luc4n3x.levyra.desktop.core.storage.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Destination {
    HOME,
    DISCOVER,
    SEARCH,
    COLLECTION,
    PLAYLIST,
    SETTINGS,
    NOW_PLAYING
}

class LevyraAppModel(
    private val scope: CoroutineScope,
    val paths: AppPaths,
    private val settingsStore: SettingsStore,
    val libraryStore: LibraryStore,
    val catalogController: CatalogController,
    val discoverController: DiscoverController,
    val lyricsController: LyricsController,
    val playbackController: PlaybackController
) {
    private val destinationState = MutableStateFlow(Destination.HOME)
    private val previousDestination = MutableStateFlow(Destination.HOME)
    private val queueVisibleState = MutableStateFlow(false)
    private val openPlaylistState = MutableStateFlow("")
    private val noticeFlow = MutableSharedFlow<String>(extraBufferCapacity = 16)

    val destination: StateFlow<Destination> = destinationState.asStateFlow()
    val queueVisible: StateFlow<Boolean> = queueVisibleState.asStateFlow()
    val openPlaylistId: StateFlow<String> = openPlaylistState.asStateFlow()
    val notices: SharedFlow<String> = noticeFlow.asSharedFlow()

    val settings: StateFlow<DesktopSettings> = settingsStore.settings
    val library: StateFlow<LibraryData> = libraryStore.library

    init {
        scope.launch {
            settingsStore.settings.collect { value ->
                ExtractorRuntime.ensureInitialized(value.language, value.contentCountry)
                discoverController.load(value.contentCountry)
            }
        }
        scope.launch {
            playbackController.messages.collect { message -> noticeFlow.tryEmit(message) }
        }
    }

    fun navigate(destination: Destination) {
        if (destinationState.value == destination) return
        previousDestination.value = destinationState.value
        destinationState.value = destination
    }

    fun back() {
        destinationState.value = previousDestination.value
    }

    fun toggleQueue() {
        queueVisibleState.value = !queueVisibleState.value
    }

    fun openCollection(ref: CollectionRef) {
        catalogController.openCollection(ref)
        navigate(Destination.COLLECTION)
    }

    fun openCollectionFromUrl(url: String) {
        catalogController.openCollectionFromUrl(url) { navigate(Destination.COLLECTION) }
    }

    fun refreshDiscover() {
        discoverController.load(settingsStore.current.contentCountry, force = true)
    }

    fun openPlaylist(playlistId: String) {
        openPlaylistState.value = playlistId
        navigate(Destination.PLAYLIST)
    }

    fun updateSettings(transform: (DesktopSettings) -> DesktopSettings) {
        settingsStore.update(transform)
    }

    fun toggleFavorite(track: Track) {
        libraryStore.toggleFavorite(track)
    }

    fun notify(message: String) {
        noticeFlow.tryEmit(message)
    }
}

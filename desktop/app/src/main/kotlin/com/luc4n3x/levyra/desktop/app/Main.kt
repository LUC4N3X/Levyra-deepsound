package com.luc4n3x.levyra.desktop.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.request.crossfade
import com.luc4n3x.levyra.desktop.app.di.AppContainer
import com.luc4n3x.levyra.desktop.app.state.Destination
import com.luc4n3x.levyra.desktop.app.state.PlaybackController
import com.luc4n3x.levyra.desktop.app.state.PlaybackUiState
import com.luc4n3x.levyra.desktop.app.ui.DesktopUpdateDialogHost
import com.luc4n3x.levyra.desktop.app.ui.LevyraRoot
import com.luc4n3x.levyra.desktop.app.ui.components.TextInputFocus
import com.luc4n3x.levyra.desktop.app.ui.components.levyraIconPainter
import com.luc4n3x.levyra.desktop.app.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.desktop.app.ui.i18n.stringsFor
import com.luc4n3x.levyra.desktop.app.ui.player.MiniPlayerWindow
import com.luc4n3x.levyra.desktop.core.model.ThemeMode
import com.luc4n3x.levyra.desktop.core.storage.LibraryData
import com.luc4n3x.levyra.desktop.core.storage.WindowPlacement as StoredPlacement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okio.Path.Companion.toOkioPath

fun main(args: Array<String>) {
    DesktopCrashHandler.install()
    val initialPayload = DesktopLinkRouter.launchPayload(args)
    val instanceManager = DesktopInstanceManager.acquire(initialPayload) ?: return

    application {
        val container = remember { AppContainer() }
        val stored = remember { container.windowPlacementStore.read() }
        val windowState = rememberWindowState(
            size = DpSize(stored.width.dp, stored.height.dp),
            position = if (stored.hasPosition) {
                WindowPosition(stored.x.dp, stored.y.dp)
            } else {
                WindowPosition.PlatformDefault
            },
            placement = if (stored.maximized) {
                WindowPlacement.Maximized
            } else {
                WindowPlacement.Floating
            }
        )
        var windowVisible by remember { mutableStateOf(true) }
        var miniPlayerVisible by remember { mutableStateOf(false) }
        var restorePulse by remember { mutableIntStateOf(0) }
        val settings by container.settingsStore.settings.collectAsState()
        val library by container.libraryStore.library.collectAsState()
        val chromePlaybackFlow = remember(container) {
            container.playbackController.state
                .map(PlaybackUiState::withoutTransientUiTicks)
                .distinctUntilChanged()
        }
        val playback by chromePlaybackFlow.collectAsState(
            initial = container.playbackController.state.value.withoutTransientUiTicks()
        )
        val strings = stringsFor(settings.language)

        setSingletonImageLoaderFactory { context ->
            ImageLoader.Builder(context)
                .diskCache {
                    DiskCache.Builder()
                        .directory(container.paths.artworkCache.toOkioPath())
                        .maxSizeBytes(ARTWORK_CACHE_BYTES)
                        .build()
                }
                .crossfade(false)
                .build()
        }

        fun persistWindow() = persistPlacement(container, windowState)

        fun showMainWindow() {
            windowVisible = true
            windowState.isMinimized = false
            restorePulse += 1
        }

        fun quit() {
            persistWindow()
            instanceManager.close()
            container.shutdown()
            exitApplication()
        }

        fun closeWindow() {
            if (settings.minimizeToTray) {
                persistWindow()
                windowVisible = false
            } else {
                quit()
            }
        }

        DisposableEffect(instanceManager) {
            onDispose { instanceManager.close() }
        }

        LaunchedEffect(instanceManager) {
            instanceManager.requests.collect { payload ->
                showMainWindow()
                if (payload.isNotBlank()) {
                    DesktopLinkRouter.route(payload, container.appModel)
                }
            }
        }

        LaunchedEffect(container) {
            withContext(Dispatchers.IO) { DesktopProtocolRegistrar.register() }
            if (initialPayload.isNotBlank()) {
                DesktopLinkRouter.route(initialPayload, container.appModel)
            }
        }

        LaunchedEffect(playback.current?.id) {
            if (playback.current == null) miniPlayerVisible = false
        }

        DisposableEffect(settings.globalMediaKeys) {
            val mediaKeys = if (settings.globalMediaKeys) {
                WindowsMediaKeys { action ->
                    when (action) {
                        MediaKeyAction.PLAY_PAUSE -> container.playbackController.togglePlayPause()
                        MediaKeyAction.NEXT -> container.playbackController.next(automatic = false)
                        MediaKeyAction.PREVIOUS -> container.playbackController.previous()
                        MediaKeyAction.STOP -> container.playbackController.stop()
                    }
                }.takeIf { it.start() }
            } else {
                null
            }
            onDispose { mediaKeys?.stop() }
        }

        LevyraTray(
            strings = strings,
            playbackController = container.playbackController,
            trackTooltip = playback.current?.let { track ->
                listOf(track.title, track.artist)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                    .ifBlank { null }
            },
            miniPlayerVisible = miniPlayerVisible,
            playerAvailable = playback.current != null,
            onShow = ::showMainWindow,
            onToggleMiniPlayer = { miniPlayerVisible = !miniPlayerVisible },
            onQuit = ::quit
        )

        Window(
            onCloseRequest = ::closeWindow,
            state = windowState,
            visible = windowVisible,
            title = strings.appName,
            icon = levyraIconPainter(),
            onKeyEvent = { event ->
                val action = DesktopShortcuts.resolve(event, TextInputFocus.active)
                if (action == null) {
                    false
                } else {
                    applyShortcut(
                        action = action,
                        container = container,
                        playback = container.playbackController.state.value,
                        onToggleMiniPlayer = {
                            if (container.playbackController.state.value.current != null) {
                                miniPlayerVisible = !miniPlayerVisible
                            }
                        }
                    )
                    true
                }
            }
        ) {
            val systemDark = isSystemInDarkTheme()
            val darkWindow = when (settings.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDark
            }

            DisposableEffect(window, darkWindow) {
                WindowsWindowStyling.apply(window, darkWindow)
                onDispose { persistWindow() }
            }

            LaunchedEffect(restorePulse, windowVisible) {
                if (windowVisible) {
                    window.toFront()
                    window.requestFocus()
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                LevyraRoot(model = container.appModel)
                DesktopUpdateDialogHost(
                    controller = container.updateController,
                    language = settings.language,
                    enabled = settings.onboardingCompleted,
                    onInstallReady = ::quit
                )
            }
        }

        if (miniPlayerVisible && playback.current != null) {
            MiniPlayerHost(
                container = container,
                library = library,
                strings = strings,
                themeMode = settings.themeMode,
                onOpenMain = ::showMainWindow,
                onClose = { miniPlayerVisible = false }
            )
        }
    }
}

@Composable
private fun MiniPlayerHost(
    container: AppContainer,
    library: LibraryData,
    strings: LevyraStrings,
    themeMode: ThemeMode,
    onOpenMain: () -> Unit,
    onClose: () -> Unit
) {
    val playback by container.playbackController.state.collectAsState()
    val current = playback.current ?: return
    MiniPlayerWindow(
        state = playback,
        strings = strings,
        themeMode = themeMode,
        isFavorite = library.favorites.any { it.id == current.id },
        onPlayPause = container.playbackController::togglePlayPause,
        onPrevious = container.playbackController::previous,
        onNext = { container.playbackController.next(automatic = false) },
        onToggleFavorite = { container.appModel.toggleFavorite(current) },
        onOpenMain = onOpenMain,
        onClose = onClose
    )
}

@Composable
private fun ApplicationScope.LevyraTray(
    strings: LevyraStrings,
    playbackController: PlaybackController,
    trackTooltip: String?,
    miniPlayerVisible: Boolean,
    playerAvailable: Boolean,
    onShow: () -> Unit,
    onToggleMiniPlayer: () -> Unit,
    onQuit: () -> Unit
) {
    Tray(
        icon = levyraIconPainter(),
        tooltip = trackTooltip?.let { "${strings.appName} · $it" } ?: strings.appName,
        onAction = onShow,
        menu = {
            Item(strings.trayShow, onClick = onShow)
            Item(
                strings.trayPlayPause,
                onClick = playbackController::togglePlayPause
            )
            Item(
                strings.trayNext,
                onClick = { playbackController.next(automatic = false) }
            )
            if (playerAvailable) {
                Item(
                    if (miniPlayerVisible) {
                        "${strings.close} · Mini ${strings.navNowPlaying}"
                    } else {
                        "${strings.settingsOpenFolder} · Mini ${strings.navNowPlaying}"
                    },
                    onClick = onToggleMiniPlayer
                )
            }
            Separator()
            Item(strings.trayQuit, onClick = onQuit)
        }
    )
}

private fun persistPlacement(
    container: AppContainer,
    windowState: WindowState
) {
    runCatching {
        val current = container.windowPlacementStore.read()
        if (windowState.placement == WindowPlacement.Maximized) {
            container.windowPlacementStore.write(current.copy(maximized = true))
            return@runCatching
        }
        val position = windowState.position
        val hasPosition = position.x.isSpecified && position.y.isSpecified
        container.windowPlacementStore.write(
            StoredPlacement(
                width = windowState.size.width.value.toInt().coerceAtLeast(MIN_WINDOW_WIDTH),
                height = windowState.size.height.value.toInt().coerceAtLeast(MIN_WINDOW_HEIGHT),
                x = if (hasPosition) position.x.value.toInt() else current.x,
                y = if (hasPosition) position.y.value.toInt() else current.y,
                maximized = false
            )
        )
    }
}

private fun applyShortcut(
    action: ShortcutAction,
    container: AppContainer,
    playback: PlaybackUiState,
    onToggleMiniPlayer: () -> Unit
) {
    val controller = container.playbackController
    when (action) {
        ShortcutAction.PLAY_PAUSE -> controller.togglePlayPause()
        ShortcutAction.NEXT -> controller.next(automatic = false)
        ShortcutAction.PREVIOUS -> controller.previous()
        ShortcutAction.SEEK_FORWARD -> controller.seekTo(
            seekTarget(playback, DesktopShortcuts.SEEK_STEP_MS)
        )

        ShortcutAction.SEEK_BACKWARD -> controller.seekTo(
            seekTarget(playback, -DesktopShortcuts.SEEK_STEP_MS)
        )

        ShortcutAction.VOLUME_UP -> controller.setVolume(playback.volume + DesktopShortcuts.VOLUME_STEP)
        ShortcutAction.VOLUME_DOWN -> controller.setVolume(playback.volume - DesktopShortcuts.VOLUME_STEP)
        ShortcutAction.TOGGLE_MUTE -> controller.toggleMuted()
        ShortcutAction.TOGGLE_SHUFFLE -> controller.toggleShuffle()
        ShortcutAction.CYCLE_REPEAT -> controller.cycleRepeat()
        ShortcutAction.TOGGLE_QUEUE -> container.appModel.toggleQueue()
        ShortcutAction.TOGGLE_MINI_PLAYER -> onToggleMiniPlayer()
        ShortcutAction.OPEN_SEARCH -> container.appModel.navigate(Destination.SEARCH)
        ShortcutAction.OPEN_NOW_PLAYING -> container.appModel.navigate(Destination.NOW_PLAYING)
    }
}

private fun seekTarget(playback: PlaybackUiState, deltaMs: Long): Long {
    val target = playback.positionMs + deltaMs
    val duration = playback.durationMs
    return if (duration > 0L) target.coerceIn(0L, duration) else target.coerceAtLeast(0L)
}

private fun PlaybackUiState.withoutTransientUiTicks(): PlaybackUiState = copy(
    positionMs = 0L,
    sleepRemainingMs = 0L
)

private const val ARTWORK_CACHE_BYTES = 512L * 1024L * 1024L
private const val MIN_WINDOW_WIDTH = 960
private const val MIN_WINDOW_HEIGHT = 640

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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
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
import com.luc4n3x.levyra.desktop.app.state.PlaybackController
import com.luc4n3x.levyra.desktop.app.ui.DesktopUpdateDialogHost
import com.luc4n3x.levyra.desktop.app.ui.LevyraRoot
import com.luc4n3x.levyra.desktop.app.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.desktop.app.ui.i18n.stringsFor
import com.luc4n3x.levyra.desktop.app.ui.player.MiniPlayerWindow
import com.luc4n3x.levyra.desktop.core.model.ThemeMode
import com.luc4n3x.levyra.desktop.core.storage.WindowPlacement as StoredPlacement
import kotlinx.coroutines.Dispatchers
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
        val playback by container.playbackController.state.collectAsState()
        val strings = stringsFor(settings.language)

        setSingletonImageLoaderFactory { context ->
            ImageLoader.Builder(context)
                .diskCache {
                    DiskCache.Builder()
                        .directory(container.paths.artworkCache.toOkioPath())
                        .maxSizeBytes(ARTWORK_CACHE_BYTES)
                        .build()
                }
                .crossfade(true)
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

        LevyraTray(
            strings = strings,
            playbackController = container.playbackController,
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
            icon = painterResource(APP_ICON),
            onKeyEvent = { event ->
                handleShortcut(event, container.playbackController)
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
            val current = playback.current
            MiniPlayerWindow(
                state = playback,
                strings = strings,
                themeMode = settings.themeMode,
                isFavorite = current?.let { track ->
                    library.favorites.any { it.id == track.id }
                } ?: false,
                onPlayPause = container.playbackController::togglePlayPause,
                onPrevious = container.playbackController::previous,
                onNext = { container.playbackController.next(automatic = false) },
                onToggleFavorite = {
                    current?.let(container.appModel::toggleFavorite)
                },
                onOpenMain = ::showMainWindow,
                onClose = { miniPlayerVisible = false }
            )
        }
    }
}

@Composable
private fun ApplicationScope.LevyraTray(
    strings: LevyraStrings,
    playbackController: PlaybackController,
    miniPlayerVisible: Boolean,
    playerAvailable: Boolean,
    onShow: () -> Unit,
    onToggleMiniPlayer: () -> Unit,
    onQuit: () -> Unit
) {
    Tray(
        icon = painterResource(APP_ICON),
        tooltip = strings.appName,
        onAction = onShow,
        menu = {
            Item(strings.trayShow, onClick = onShow)
            Item(
                strings.trayPlayPause,
                onClick = playbackController::togglePlayPause
            )
            Item(
                strings.trayNext,
                onClick = {
                    playbackController.next(automatic = false)
                }
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
            container.windowPlacementStore.write(
                current.copy(maximized = true)
            )
            return@runCatching
        }
        val position = windowState.position
        val hasPosition = position.x.isSpecified && position.y.isSpecified
        container.windowPlacementStore.write(
            StoredPlacement(
                width = windowState.size.width.value
                    .toInt()
                    .coerceAtLeast(MIN_WINDOW_WIDTH),
                height = windowState.size.height.value
                    .toInt()
                    .coerceAtLeast(MIN_WINDOW_HEIGHT),
                x = if (hasPosition) {
                    position.x.value.toInt()
                } else {
                    current.x
                },
                y = if (hasPosition) {
                    position.y.value.toInt()
                } else {
                    current.y
                },
                maximized = false
            )
        )
    }
}

private fun handleShortcut(
    event: KeyEvent,
    playbackController: PlaybackController
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    return when {
        event.key == Key.Spacebar && !event.isCtrlPressed -> {
            playbackController.togglePlayPause()
            true
        }

        event.key == Key.DirectionRight && event.isCtrlPressed -> {
            playbackController.next(automatic = false)
            true
        }

        event.key == Key.DirectionLeft && event.isCtrlPressed -> {
            playbackController.previous()
            true
        }

        else -> false
    }
}

private const val APP_ICON = "icons/levyra.svg"
private const val ARTWORK_CACHE_BYTES = 512L * 1024L * 1024L
private const val MIN_WINDOW_WIDTH = 960
private const val MIN_WINDOW_HEIGHT = 640

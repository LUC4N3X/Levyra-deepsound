package com.luc4n3x.levyra.desktop.app

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.request.crossfade
import com.luc4n3x.levyra.desktop.app.di.AppContainer
import com.luc4n3x.levyra.desktop.app.ui.LevyraRoot
import com.luc4n3x.levyra.desktop.app.ui.i18n.stringsFor
import com.luc4n3x.levyra.desktop.core.storage.WindowPlacement
import okio.Path.Companion.toOkioPath

fun main() = application {
    val container = remember { AppContainer() }
    val placement = remember { container.windowPlacementStore.read() }
    val windowState = rememberWindowState(
        size = DpSize(placement.width.dp, placement.height.dp),
        position = if (placement.hasPosition) {
            WindowPosition(placement.x.dp, placement.y.dp)
        } else {
            WindowPosition.PlatformDefault
        }
    )
    var windowVisible by remember { mutableStateOf(true) }
    val settings by container.settingsStore.settings.collectAsState()

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

    fun persistWindow() {
        runCatching {
            container.windowPlacementStore.write(
                WindowPlacement(
                    width = windowState.size.width.value.toInt().coerceAtLeast(MIN_WINDOW_WIDTH),
                    height = windowState.size.height.value.toInt().coerceAtLeast(MIN_WINDOW_HEIGHT),
                    x = windowState.position.x.value.toInt(),
                    y = windowState.position.y.value.toInt(),
                    maximized = false
                )
            )
        }
    }

    fun quit() {
        persistWindow()
        container.shutdown()
        exitApplication()
    }

    val strings = stringsFor(settings.language)

    Tray(
        icon = painterResource(TRAY_ICON),
        tooltip = strings.appName,
        onAction = { windowVisible = true },
        menu = {
            Item(strings.trayShow, onClick = { windowVisible = true })
            Item(strings.trayPlayPause, onClick = { container.playbackController.togglePlayPause() })
            Item(strings.trayNext, onClick = { container.playbackController.next(automatic = false) })
            Separator()
            Item(strings.trayQuit, onClick = ::quit)
        }
    )

    Window(
        onCloseRequest = {
            if (settings.minimizeToTray) {
                persistWindow()
                windowVisible = false
            } else {
                quit()
            }
        },
        state = windowState,
        visible = windowVisible,
        title = strings.appName,
        icon = painterResource(TRAY_ICON),
        onKeyEvent = { event ->
            if (event.type != KeyEventType.KeyDown) {
                false
            } else {
                when {
                    event.key == Key.Spacebar && !event.isCtrlPressed -> {
                        container.playbackController.togglePlayPause()
                        true
                    }

                    event.key == Key.DirectionRight && event.isCtrlPressed -> {
                        container.playbackController.next(automatic = false)
                        true
                    }

                    event.key == Key.DirectionLeft && event.isCtrlPressed -> {
                        container.playbackController.previous()
                        true
                    }

                    else -> false
                }
            }
        }
    ) {
        DisposableEffect(Unit) {
            onDispose { persistWindow() }
        }
        LevyraRoot(model = container.appModel)
    }
}

private const val TRAY_ICON = "icons/levyra.png"
private const val ARTWORK_CACHE_BYTES = 256L * 1024L * 1024L
private const val MIN_WINDOW_WIDTH = 960
private const val MIN_WINDOW_HEIGHT = 640

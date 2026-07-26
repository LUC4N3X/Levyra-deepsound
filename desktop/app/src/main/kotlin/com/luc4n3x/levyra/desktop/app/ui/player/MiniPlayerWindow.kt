package com.luc4n3x.levyra.desktop.app.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowDraggableArea
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import com.luc4n3x.levyra.desktop.app.state.PlaybackUiState
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraBrand
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraTheme
import com.luc4n3x.levyra.desktop.app.ui.theme.LocalAccentColor
import com.luc4n3x.levyra.desktop.core.model.ThemeMode
import java.awt.Dimension
import java.awt.Point
import java.util.prefs.Preferences

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayerWindow(
    state: PlaybackUiState,
    strings: LevyraStrings,
    themeMode: ThemeMode,
    isFavorite: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenMain: () -> Unit,
    onClose: () -> Unit
) {
    val preferences = remember { Preferences.userRoot().node(PREFERENCES_NODE) }
    val savedX = preferences.getInt("x", Int.MIN_VALUE)
    val savedY = preferences.getInt("y", Int.MIN_VALUE)
    val savedWidth = preferences.getInt("width", DEFAULT_WIDTH).coerceAtLeast(MIN_WIDTH)
    val savedHeight = preferences.getInt("height", DEFAULT_HEIGHT).coerceAtLeast(MIN_HEIGHT)
    val windowState = remember {
        WindowState(
            placement = WindowPlacement.Floating,
            position = if (savedX == Int.MIN_VALUE || savedY == Int.MIN_VALUE) {
                WindowPosition(Alignment.BottomEnd)
            } else {
                WindowPosition(savedX.dp, savedY.dp)
            },
            size = DpSize(savedWidth.dp, savedHeight.dp)
        )
    }

    LaunchedEffect(windowState.position, windowState.size) {
        val position = windowState.position
        if (position is WindowPosition.Absolute) {
            preferences.putInt("x", position.x.value.toInt().coerceAtLeast(0))
            preferences.putInt("y", position.y.value.toInt().coerceAtLeast(0))
        }
        preferences.putInt("width", windowState.size.width.value.toInt().coerceAtLeast(MIN_WIDTH))
        preferences.putInt("height", windowState.size.height.value.toInt().coerceAtLeast(MIN_HEIGHT))
    }

    Window(
        onCloseRequest = onClose,
        state = windowState,
        title = "Levyra · Mini ${strings.navNowPlaying}",
        icon = painterResource("icons/levyra.svg"),
        alwaysOnTop = true,
        undecorated = true,
        transparent = true,
        resizable = true,
        onKeyEvent = { event ->
            if (event.type != KeyEventType.KeyDown) {
                false
            } else {
                when (event.key) {
                    Key.Spacebar -> {
                        onPlayPause()
                        true
                    }
                    Key.DirectionLeft -> {
                        onPrevious()
                        true
                    }
                    Key.DirectionRight -> {
                        onNext()
                        true
                    }
                    Key.Escape -> {
                        onClose()
                        true
                    }
                    else -> false
                }
            }
        }
    ) {
        DisposableEffect(window) {
            window.minimumSize = Dimension(MIN_WIDTH, MIN_HEIGHT)
            window.background = java.awt.Color(0, 0, 0, 0)
            if (savedX != Int.MIN_VALUE && savedY != Int.MIN_VALUE) {
                window.location = Point(savedX.coerceAtLeast(0), savedY.coerceAtLeast(0))
            }
            onDispose { }
        }

        LevyraTheme(themeMode = themeMode) {
            CompositionLocalProvider(
                LocalStrings provides strings,
                LocalAccentColor provides LevyraBrand.cyan
            ) {
                WindowDraggableArea {
                    MiniPlayerContent(
                        state = state,
                        strings = strings,
                        isFavorite = isFavorite,
                        onPlayPause = onPlayPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onToggleFavorite = onToggleFavorite,
                        onOpenMain = onOpenMain,
                        onClose = onClose
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerContent(
    state: PlaybackUiState,
    strings: LevyraStrings,
    isFavorite: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenMain: () -> Unit,
    onClose: () -> Unit
) {
    val track = state.current
    val progress = if (state.durationMs > 0L) {
        (state.positionMs.toDouble() / state.durationMs.toDouble()).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    Surface(
        modifier = Modifier.fillMaxSize().padding(6.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 24.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Artwork(
                    url = track?.artworkUrl.orEmpty(),
                    modifier = Modifier
                        .size(62.dp)
                        .clickable(onClick = onOpenMain),
                    cornerRadius = 15.dp,
                    iconSize = 24.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track?.title.orEmpty().ifBlank { strings.nowPlayingEmpty },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = track?.displaySubtitle.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onToggleFavorite, enabled = track != null) {
                    Icon(
                        imageVector = if (isFavorite) LevyraIcons.HeartFilled else LevyraIcons.Heart,
                        contentDescription = if (isFavorite) strings.removeFromFavorites else strings.addToFavorites,
                        tint = if (isFavorite) LevyraBrand.cyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onPrevious, enabled = track != null) {
                    Icon(
                        imageVector = LevyraIcons.SkipPrevious,
                        contentDescription = strings.playbackPrevious,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Surface(
                    modifier = Modifier
                        .size(42.dp)
                        .clickable(enabled = track != null, onClick = onPlayPause),
                    shape = CircleShape,
                    color = if (track != null) LevyraBrand.cyan else MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (state.isPlaying) LevyraIcons.Pause else LevyraIcons.Play,
                            contentDescription = if (state.isPlaying) strings.playbackPause else strings.playbackPlay,
                            tint = if (track != null) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                IconButton(onClick = onNext, enabled = track != null) {
                    Icon(
                        imageVector = LevyraIcons.SkipNext,
                        contentDescription = strings.playbackNext,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = LevyraIcons.Close,
                        contentDescription = strings.close,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = LevyraBrand.cyan,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }
    }
}

private const val PREFERENCES_NODE = "Levyra/MiniPlayer"
private const val DEFAULT_WIDTH = 520
private const val DEFAULT_HEIGHT = 104
private const val MIN_WIDTH = 420
private const val MIN_HEIGHT = 92

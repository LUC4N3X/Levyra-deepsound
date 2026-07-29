package com.luc4n3x.levyra.desktop.app

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

enum class ShortcutAction {
    PLAY_PAUSE,
    NEXT,
    PREVIOUS,
    SEEK_FORWARD,
    SEEK_BACKWARD,
    VOLUME_UP,
    VOLUME_DOWN,
    TOGGLE_MUTE,
    TOGGLE_SHUFFLE,
    CYCLE_REPEAT,
    TOGGLE_QUEUE,
    TOGGLE_MINI_PLAYER,
    OPEN_SEARCH,
    OPEN_NOW_PLAYING
}

object DesktopShortcuts {
    const val SEEK_STEP_MS = 5_000L
    const val VOLUME_STEP = 5

    fun resolve(
        key: Key,
        ctrl: Boolean = false,
        shift: Boolean = false,
        alt: Boolean = false,
        textInputFocused: Boolean = false
    ): ShortcutAction? {
        if (alt) return null
        MEDIA_ACTIONS[key]?.let { return it }
        if (textInputFocused && !ctrl) return null
        return when {
            ctrl && shift -> if (key == Key.M) ShortcutAction.TOGGLE_MINI_PLAYER else null
            shift -> null
            ctrl -> CTRL_ACTIONS[key]
            else -> UNMODIFIED_ACTIONS[key]
        }
    }

    fun resolve(event: KeyEvent, textInputFocused: Boolean = false): ShortcutAction? {
        if (event.type != KeyEventType.KeyDown) return null
        return resolve(
            key = event.key,
            ctrl = event.isCtrlPressed,
            shift = event.isShiftPressed,
            alt = event.isAltPressed,
            textInputFocused = textInputFocused
        )
    }

    private val MEDIA_ACTIONS = mapOf(
        Key.MediaPlay to ShortcutAction.PLAY_PAUSE,
        Key.MediaPause to ShortcutAction.PLAY_PAUSE,
        Key.MediaPlayPause to ShortcutAction.PLAY_PAUSE,
        Key.MediaNext to ShortcutAction.NEXT,
        Key.MediaPrevious to ShortcutAction.PREVIOUS
    )

    private val UNMODIFIED_ACTIONS = mapOf(
        Key.Spacebar to ShortcutAction.PLAY_PAUSE,
        Key.DirectionRight to ShortcutAction.SEEK_FORWARD,
        Key.DirectionLeft to ShortcutAction.SEEK_BACKWARD
    )

    private val CTRL_ACTIONS = mapOf(
        Key.DirectionRight to ShortcutAction.NEXT,
        Key.DirectionLeft to ShortcutAction.PREVIOUS,
        Key.DirectionUp to ShortcutAction.VOLUME_UP,
        Key.DirectionDown to ShortcutAction.VOLUME_DOWN,
        Key.M to ShortcutAction.TOGGLE_MUTE,
        Key.S to ShortcutAction.TOGGLE_SHUFFLE,
        Key.R to ShortcutAction.CYCLE_REPEAT,
        Key.Q to ShortcutAction.TOGGLE_QUEUE,
        Key.P to ShortcutAction.OPEN_NOW_PLAYING,
        Key.F to ShortcutAction.OPEN_SEARCH,
        Key.K to ShortcutAction.OPEN_SEARCH
    )
}

package com.luc4n3x.levyra.desktop.app

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType

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
        alt: Boolean = false
    ): ShortcutAction? {
        if (alt) return null
        return when (key) {
            Key.Spacebar -> if (ctrl || shift) null else ShortcutAction.PLAY_PAUSE
            Key.MediaPlay, Key.MediaPause, Key.MediaPlayPause -> ShortcutAction.PLAY_PAUSE
            Key.MediaNext -> ShortcutAction.NEXT
            Key.MediaPrevious -> ShortcutAction.PREVIOUS
            Key.DirectionRight -> when {
                ctrl && !shift -> ShortcutAction.NEXT
                !ctrl && !shift -> ShortcutAction.SEEK_FORWARD
                else -> null
            }

            Key.DirectionLeft -> when {
                ctrl && !shift -> ShortcutAction.PREVIOUS
                !ctrl && !shift -> ShortcutAction.SEEK_BACKWARD
                else -> null
            }

            Key.DirectionUp -> if (ctrl && !shift) ShortcutAction.VOLUME_UP else null
            Key.DirectionDown -> if (ctrl && !shift) ShortcutAction.VOLUME_DOWN else null
            Key.M -> when {
                ctrl && shift -> ShortcutAction.TOGGLE_MINI_PLAYER
                ctrl -> ShortcutAction.TOGGLE_MUTE
                else -> null
            }

            Key.S -> if (ctrl && !shift) ShortcutAction.TOGGLE_SHUFFLE else null
            Key.R -> if (ctrl && !shift) ShortcutAction.CYCLE_REPEAT else null
            Key.Q -> if (ctrl && !shift) ShortcutAction.TOGGLE_QUEUE else null
            Key.P -> if (ctrl && !shift) ShortcutAction.OPEN_NOW_PLAYING else null
            Key.F, Key.K -> if (ctrl && !shift) ShortcutAction.OPEN_SEARCH else null
            else -> null
        }
    }

    fun resolve(event: KeyEvent): ShortcutAction? {
        if (event.type != KeyEventType.KeyDown) return null
        return resolve(
            key = event.key,
            ctrl = event.isCtrlPressed,
            shift = event.isShiftPressed,
            alt = event.isAltPressed
        )
    }
}

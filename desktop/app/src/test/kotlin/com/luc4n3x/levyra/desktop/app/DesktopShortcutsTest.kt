package com.luc4n3x.levyra.desktop.app

import androidx.compose.ui.input.key.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DesktopShortcutsTest {

    @Test
    fun `transport keys follow the documented bindings`() {
        assertEquals(ShortcutAction.PLAY_PAUSE, DesktopShortcuts.resolve(Key.Spacebar))
        assertEquals(ShortcutAction.NEXT, DesktopShortcuts.resolve(Key.DirectionRight, ctrl = true))
        assertEquals(ShortcutAction.PREVIOUS, DesktopShortcuts.resolve(Key.DirectionLeft, ctrl = true))
        assertEquals(ShortcutAction.SEEK_FORWARD, DesktopShortcuts.resolve(Key.DirectionRight))
        assertEquals(ShortcutAction.SEEK_BACKWARD, DesktopShortcuts.resolve(Key.DirectionLeft))
    }

    @Test
    fun `volume and mute need the control modifier`() {
        assertEquals(ShortcutAction.VOLUME_UP, DesktopShortcuts.resolve(Key.DirectionUp, ctrl = true))
        assertEquals(ShortcutAction.VOLUME_DOWN, DesktopShortcuts.resolve(Key.DirectionDown, ctrl = true))
        assertEquals(ShortcutAction.TOGGLE_MUTE, DesktopShortcuts.resolve(Key.M, ctrl = true))
        assertNull(DesktopShortcuts.resolve(Key.DirectionUp))
        assertNull(DesktopShortcuts.resolve(Key.DirectionDown))
        assertNull(DesktopShortcuts.resolve(Key.M))
    }

    @Test
    fun `shift separates mute from the mini player`() {
        assertEquals(
            ShortcutAction.TOGGLE_MINI_PLAYER,
            DesktopShortcuts.resolve(Key.M, ctrl = true, shift = true)
        )
        assertEquals(ShortcutAction.TOGGLE_MUTE, DesktopShortcuts.resolve(Key.M, ctrl = true))
    }

    @Test
    fun `queue navigation and search bindings resolve`() {
        assertEquals(ShortcutAction.TOGGLE_SHUFFLE, DesktopShortcuts.resolve(Key.S, ctrl = true))
        assertEquals(ShortcutAction.CYCLE_REPEAT, DesktopShortcuts.resolve(Key.R, ctrl = true))
        assertEquals(ShortcutAction.TOGGLE_QUEUE, DesktopShortcuts.resolve(Key.Q, ctrl = true))
        assertEquals(ShortcutAction.OPEN_NOW_PLAYING, DesktopShortcuts.resolve(Key.P, ctrl = true))
        assertEquals(ShortcutAction.OPEN_SEARCH, DesktopShortcuts.resolve(Key.F, ctrl = true))
        assertEquals(ShortcutAction.OPEN_SEARCH, DesktopShortcuts.resolve(Key.K, ctrl = true))
    }

    @Test
    fun `hardware media keys map to transport actions`() {
        assertEquals(ShortcutAction.PLAY_PAUSE, DesktopShortcuts.resolve(Key.MediaPlayPause))
        assertEquals(ShortcutAction.NEXT, DesktopShortcuts.resolve(Key.MediaNext))
        assertEquals(ShortcutAction.PREVIOUS, DesktopShortcuts.resolve(Key.MediaPrevious))
    }

    @Test
    fun `alt combinations and unbound keys are ignored`() {
        assertNull(DesktopShortcuts.resolve(Key.Spacebar, alt = true))
        assertNull(DesktopShortcuts.resolve(Key.Spacebar, ctrl = true))
        assertNull(DesktopShortcuts.resolve(Key.DirectionRight, ctrl = true, shift = true))
        assertNull(DesktopShortcuts.resolve(Key.Z, ctrl = true))
        assertNull(DesktopShortcuts.resolve(Key.Escape))
    }

    @Test
    fun `typing in a text field keeps space and arrows out of playback`() {
        assertNull(DesktopShortcuts.resolve(Key.Spacebar, textInputFocused = true))
        assertNull(DesktopShortcuts.resolve(Key.DirectionLeft, textInputFocused = true))
        assertNull(DesktopShortcuts.resolve(Key.DirectionRight, textInputFocused = true))
    }

    @Test
    fun `text field focus still allows modifier and hardware shortcuts`() {
        assertEquals(
            ShortcutAction.NEXT,
            DesktopShortcuts.resolve(Key.DirectionRight, ctrl = true, textInputFocused = true)
        )
        assertEquals(
            ShortcutAction.TOGGLE_QUEUE,
            DesktopShortcuts.resolve(Key.Q, ctrl = true, textInputFocused = true)
        )
        assertEquals(
            ShortcutAction.PLAY_PAUSE,
            DesktopShortcuts.resolve(Key.MediaPlayPause, textInputFocused = true)
        )
        assertEquals(
            ShortcutAction.NEXT,
            DesktopShortcuts.resolve(Key.MediaNext, textInputFocused = true)
        )
    }
}

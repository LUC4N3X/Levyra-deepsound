package com.luc4n3x.levyra.ui.library

import com.luc4n3x.levyra.domain.PlaylistStudioDraft
import com.luc4n3x.levyra.domain.PlaylistStudioEdits
import com.luc4n3x.levyra.viewmodel.PlaylistStudioSession
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistStudioCloseTest {

    private val saved = PlaylistStudioDraft(playlistId = "p1", name = "Mix")

    @Test
    fun `untouched new drafts close without asking`() {
        val fresh = PlaylistStudioEdits.startNew()
        assertFalse(studioCloseNeedsConfirmation(PlaylistStudioSession(1, fresh, fresh)))
    }

    @Test
    fun `clean and saved sessions close without asking`() {
        assertFalse(studioCloseNeedsConfirmation(PlaylistStudioSession(1, saved, saved)))
        assertFalse(studioCloseNeedsConfirmation(PlaylistStudioSession(1, saved, saved, justSaved = true)))
    }

    @Test
    fun `a save in progress never blocks closing`() {
        val edited = saved.copy(name = "Renamed")
        assertFalse(studioCloseNeedsConfirmation(PlaylistStudioSession(1, edited, saved, saving = true)))
    }

    @Test
    fun `unsaved or failed edits ask before discarding`() {
        val edited = saved.copy(name = "Renamed")
        assertTrue(studioCloseNeedsConfirmation(PlaylistStudioSession(1, edited, saved)))
        assertTrue(studioCloseNeedsConfirmation(PlaylistStudioSession(1, saved, saved, failed = true)))
        val named = PlaylistStudioEdits.rename(PlaylistStudioEdits.startNew(), "Draft")
        assertTrue(studioCloseNeedsConfirmation(PlaylistStudioSession(1, named, PlaylistStudioEdits.startNew())))
    }
}

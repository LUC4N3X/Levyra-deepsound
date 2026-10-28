package com.luc4n3x.levyra.ui.selection

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.search.searchTestTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackSelectionActionsTest {
    @Test
    fun `empty selection exposes no batch actions`() {
        val unexpected: (List<Track>) -> Unit = { error("No action should be invoked") }
        assertEquals(
            TrackSelectionActions(),
            trackSelectionActions(TrackSelectionState(), emptyList(), unexpected, unexpected, unexpected, unexpected, unexpected)
        )
    }

    @Test
    fun `completed batch actions preserve order and exit only after the callback`() {
        val state = TrackSelectionState()
        val tracks = listOf(searchTestTrack("b", "Second", "Artist"), searchTestTrack("a", "First", "Artist"))
        var delivered = emptyList<Track>()
        val complete: (List<Track>) -> Unit = {
            assertTrue(state.isActive)
            delivered = it
        }
        val actions = trackSelectionActions(state, tracks, complete, complete, {}, complete, complete)
        listOf(actions.onPlayNext, actions.onAddToQueue, actions.onFavorite, actions.onDownload).forEach { action ->
            state.start("a")
            requireNotNull(action).invoke()
            assertEquals(tracks, delivered)
            assertFalse(state.isActive)
        }
    }

    @Test
    fun `playlist dialog receives valid ids and keeps selection until confirmation`() {
        val state = TrackSelectionState().apply { start("a") }
        val valid = searchTestTrack("a", "Track", "Artist")
        val missingId = valid.copy(id = "")
        var delivered = emptyList<Track>()
        val actions = trackSelectionActions(state, listOf(missingId, valid), {}, {}, { delivered = it }, {}, {})
        requireNotNull(actions.onAddToPlaylist).invoke()
        assertEquals(listOf(valid), delivered)
        assertTrue(state.isActive)
        assertNull(trackSelectionActions(state, listOf(missingId), {}, {}, {}, {}, {}).onAddToPlaylist)
    }
}

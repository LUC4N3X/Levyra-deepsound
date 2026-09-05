package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.feature.jam.JamGuestPermission
import com.luc4n3x.levyra.feature.jam.JamRole
import com.luc4n3x.levyra.feature.jam.JamUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class JamSimilarSongActionTest {

    @Test
    fun collaborativeGuestSelectsATrackTheSharedQueueAlreadyHolds() {
        assertEquals(
            JamSimilarSongAction.SelectExisting,
            jamSimilarSongAction(guest(JamGuestPermission.Collaborative), existingIndex = 4)
        )
    }

    @Test
    fun collaborativeGuestAddsThenSelectsATrackThatIsMissing() {
        assertEquals(
            JamSimilarSongAction.AddThenSelect,
            jamSimilarSongAction(guest(JamGuestPermission.Collaborative), existingIndex = -1)
        )
    }

    @Test
    fun addSongsGuestOnlyProposesTheTrack() {
        assertEquals(
            JamSimilarSongAction.AddOnly,
            jamSimilarSongAction(guest(JamGuestPermission.AddSongs), existingIndex = -1)
        )
        assertEquals(
            JamSimilarSongAction.AddOnly,
            jamSimilarSongAction(guest(JamGuestPermission.AddSongs), existingIndex = 2)
        )
    }

    @Test
    fun hostOnlyGuestIsRejected() {
        assertEquals(
            JamSimilarSongAction.Reject,
            jamSimilarSongAction(guest(JamGuestPermission.HostOnly), existingIndex = -1)
        )
    }

    private fun guest(permission: JamGuestPermission) = JamUiState(
        role = JamRole.Guest,
        permission = permission
    )
}

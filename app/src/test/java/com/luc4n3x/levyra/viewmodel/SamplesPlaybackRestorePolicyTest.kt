package com.luc4n3x.levyra.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SamplesPlaybackRestorePolicyTest {
    @Test
    fun pausedRestoreStartsPausedOnlyForItsOwnRequest() {
        val request = PlaybackResolveRequest(id = 10L, startPaused = true)
        assertTrue(shouldStartPlaybackPaused(request, activeRequestId = 10L))
    }

    @Test
    fun supersededRestoreCannotPauseTheReplacementRequest() {
        val staleRestore = PlaybackResolveRequest(id = 10L, startPaused = true)
        assertFalse(shouldStartPlaybackPaused(staleRestore, activeRequestId = 11L))
    }

    @Test
    fun normalRequestAfterFailedRestoreDoesNotInheritPause() {
        val failedRestore = PlaybackResolveRequest(id = 10L, startPaused = true)
        val normalReplacement = PlaybackResolveRequest(id = 11L)
        assertFalse(shouldStartPlaybackPaused(failedRestore, activeRequestId = 11L))
        assertFalse(shouldStartPlaybackPaused(normalReplacement, activeRequestId = 11L))
    }
}

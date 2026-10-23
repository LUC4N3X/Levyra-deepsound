package com.luc4n3x.levyra.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class PlaybackResolveBudgetTest {
    @Test
    fun editorialTracksUseTheSameIdentityResolutionAsImportedTracks() {
        val track = requireNotNull(com.luc4n3x.levyra.data.apple.AppleCatalogTracks.track("tiny window", "Ólafur Apollo"))
        assertTrue(needsCatalogYoutubeIdentity(track.copy(id = "levyra-discovery")))
        assertTrue(needsCatalogYoutubeIdentity(track.copy(id = "spotify:catalog")))
        assertFalse(needsCatalogYoutubeIdentity(track.copy(audioVideoId = "7sEXyTvcZOE")))
        assertFalse(needsCatalogYoutubeIdentity(track.copy(source = "Offline")))
        assertFalse(needsCatalogYoutubeIdentity(track.copy(streamUrl = "content://audio/1")))
    }

    @Test
    fun spotifyMatchingCannotOutlivePlaybackBudget() = runBlocking {
        val startedAt = System.nanoTime()
        val failure = runCatching {
            withinPlaybackResolveBudget(timeoutMs = 50L) {
                delay(5_000L)
            }
        }.exceptionOrNull()
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L

        assertTrue(failure is PlaybackBlockedException)
        assertTrue("Playback timeout must stop matching promptly, took ${elapsedMs}ms", elapsedMs < 1_000L)
    }
}

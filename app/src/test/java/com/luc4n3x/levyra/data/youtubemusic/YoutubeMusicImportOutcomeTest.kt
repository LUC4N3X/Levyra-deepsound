package com.luc4n3x.levyra.data.youtubemusic

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class YoutubeMusicImportOutcomeTest {
    @Test fun playlistRemainsSuccessfulWhenFavoritesFail() = runBlocking {
        val writes = mutableListOf<String>()
        val result = importYoutubeMusicItems(listOf("song"), {
            writes += "playlist"; 1
        }, {
            writes += "favorites"; throw java.io.IOException("synthetic disk failure")
        })
        assertEquals(YoutubeMusicImportOutcome.PlaylistOnly, result)
        assertEquals(listOf("playlist", "favorites"), writes)
    }
    @Test fun favoritesSuccessPreservesImportedOutcome() = runBlocking {
        val result = importYoutubeMusicItems(listOf("song"), { 0 }, { })
        assertEquals(YoutubeMusicImportOutcome.Imported, result)
    }
    @Test fun unchangedPlaylistIsUpToDate() = runBlocking {
        assertEquals(YoutubeMusicImportOutcome.UpToDate, importYoutubeMusicItems(listOf("song"), { 0 }))
    }
    @Test fun emptySourceDoesNotWriteAnyStore() = runBlocking {
        val result = importYoutubeMusicItems(emptyList<String>(), { fail("Unexpected playlist write"); 0 }, { fail("Unexpected favorite write") })
        assertEquals(YoutubeMusicImportOutcome.Empty, result)
    }
    @Test fun playlistFailureDoesNotAttemptFavorites() = runBlocking {
        try {
            importYoutubeMusicItems(listOf("song"), { throw java.io.IOException("synthetic playlist failure") }, { fail("Unexpected favorite write") })
            fail("Expected failure")
        } catch (_: java.io.IOException) { }
    }
    @Test fun favoriteCancellationPropagates() = runBlocking {
        try {
            importYoutubeMusicItems(listOf("song"), { 1 }, { throw CancellationException("synthetic cancellation") })
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
    }
}

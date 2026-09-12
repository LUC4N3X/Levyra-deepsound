package com.luc4n3x.levyra.player.queue

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistBulkQueueOrderTest {

    @Test
    fun playNextPreservesSelectedPlaylistOrder() {
        val current = listOf(track("X"), track("Y"), track("Z"))
        val selected = listOf(track("B"), track("D"), track("E"))

        val result = queueTracksAfterPlayNext(current, currentIndex = 0, selected)

        assertEquals(listOf("X", "B", "D", "E", "Y", "Z"), result.map { it.id })
    }

    @Test
    fun addToQueuePreservesOrderAndDoesNotDuplicateExistingTracks() {
        val current = listOf(track("X"), track("B"))
        val selected = listOf(track("B"), track("D"), track("E"), track("D"))

        val result = queueTracksAfterAddLast(current, selected)

        assertEquals(listOf("X", "B", "D", "E"), result.map { it.id })
    }

    private fun track(id: String) = Track(
        id = id,
        title = id,
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}

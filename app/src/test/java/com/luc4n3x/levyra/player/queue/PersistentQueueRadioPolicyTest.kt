package com.luc4n3x.levyra.player.queue

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistentQueueRadioPolicyTest {

    @Test
    fun trimPreservesCurrentAndRecentHistory() {
        val history = (0..20).toList()

        val removable = radioHistoryTrimIndices(
            history = history,
            currentIndex = 20,
            slotsNeeded = 5,
            historyReserve = 8
        )

        assertEquals(setOf(0, 1, 2, 3, 4), removable)
        assertTrue(20 !in removable)
        assertTrue((13..20).none(removable::contains))
    }

    @Test
    fun trimNeverInventsSlotsWithoutOldHistory() {
        assertTrue(
            radioHistoryTrimIndices(
                history = listOf(1, 2, 3),
                currentIndex = 3,
                slotsNeeded = 5,
                historyReserve = 8
            ).isEmpty()
        )
    }

    @Test
    fun trimDeduplicatesRepeatedHistoryIndices() {
        val removable = radioHistoryTrimIndices(
            history = listOf(1, 2, 1, 3, 4, 5),
            currentIndex = 5,
            slotsNeeded = 3,
            historyReserve = 2
        )

        assertEquals(setOf(1, 2, 3), removable)
    }

    @Test
    fun candidatePoolSkipsExistingTracksBeforeApplyingBatchLimit() {
        val existing = (1..5).map { track("existing-$it", "Existing $it") }
        val candidates = existing + (1..5).map { track("fresh-$it", "Fresh $it") }

        val selected = radioCandidateTracks(
            existingTracks = existing,
            candidates = candidates,
            limit = 5
        )

        assertEquals(
            listOf("fresh-1", "fresh-2", "fresh-3", "fresh-4", "fresh-5"),
            selected.map(Track::id)
        )
    }

    @Test
    fun candidatePoolRejectsArtistTitleDuplicatesWithDifferentIds() {
        val existing = listOf(track("old", "Same song", artist = "Artist"))

        val selected = radioCandidateTracks(
            existingTracks = existing,
            candidates = listOf(
                track("duplicate", " same SONG ", artist = " artist "),
                track("fresh", "Different song", artist = "Artist")
            ),
            limit = 5
        )

        assertEquals(listOf("fresh"), selected.map(Track::id))
    }

    private fun track(id: String, title: String, artist: String = "Artist") = Track(
        id = id,
        title = title,
        artist = artist,
        album = "",
        durationMs = 0L,
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

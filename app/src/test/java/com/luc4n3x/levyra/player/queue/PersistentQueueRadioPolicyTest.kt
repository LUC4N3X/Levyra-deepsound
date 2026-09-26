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

    @Test
    fun radioInsertsRightAfterTheCurrentTrackWhenStartedFromIt() {
        assertEquals(3, radioInsertionIndex(currentIndex = 2, queueSize = 9, afterCurrent = true))
    }

    @Test
    fun radioAppendsToTheTailWhenItIsAContinuationBatch() {
        assertEquals(9, radioInsertionIndex(currentIndex = 2, queueSize = 9, afterCurrent = false))
    }

    @Test
    fun radioInsertionStaysInsideTheQueueBounds() {
        assertEquals(0, radioInsertionIndex(currentIndex = -1, queueSize = 4, afterCurrent = true))
        assertEquals(4, radioInsertionIndex(currentIndex = 3, queueSize = 4, afterCurrent = true))
        assertEquals(0, radioInsertionIndex(currentIndex = 7, queueSize = 0, afterCurrent = true))
    }

    @Test
    fun queueTracksAfterAddLastAppendsNewTracksAndSkipsDuplicates() {
        val t1 = track("1", "Song 1")
        val t2 = track("2", "Song 2")
        val t3 = track("3", "Song 3")
        val updated = queueTracksAfterAddLast(listOf(t1, t2), listOf(t2, t3))
        assertEquals(listOf("1", "2", "3"), updated.map(Track::id))
    }

    @Test
    fun queueTracksAfterPlayNextInsertsAfterCurrent() {
        val t1 = track("1", "Song 1")
        val t2 = track("2", "Song 2")
        val t3 = track("3", "Song 3")
        val updated = queueTracksAfterPlayNext(listOf(t1, t2), currentIndex = 0, listOf(t3))
        assertEquals(listOf("1", "3", "2"), updated.map(Track::id))
    }

    @Test
    fun candidatePoolRejectsUploadVariantsOfTheSameSong() {
        val existing = listOf(track("seed", "Paradise", artist = "Coldplay"))

        val selected = radioCandidateTracks(
            existingTracks = existing,
            candidates = listOf(
                track("video", "Paradise (Official Video)", artist = "Coldplay"),
                track("lyrics", "Coldplay - Paradise [Lyrics]", artist = "Coldplay - Topic"),
                track("audio", "Paradise - Official Audio", artist = "Coldplay, Rihanna"),
                track("remaster", "Paradise (Remastered 2011)", artist = "COLDPLAY"),
                track("seed", "Paradise", artist = "Coldplay"),
                track("fresh", "Fix You", artist = "Coldplay")
            ),
            limit = 5
        )

        assertEquals(listOf("fresh"), selected.map(Track::id))
    }

    @Test
    fun candidatePoolKeepsDistinctRecordingsAndOtherArtists() {
        val existing = listOf(track("seed", "Paradise", artist = "Coldplay"))

        val selected = radioCandidateTracks(
            existingTracks = existing,
            candidates = listOf(
                track("other-artist", "Paradise", artist = "Sade"),
                track("remix", "Paradise (Tiesto Remix)", artist = "Coldplay"),
                track("live", "Paradise (Live)", artist = "Coldplay"),
                track("generic", "Intro", artist = "The xx"),
                track("generic-other", "Intro", artist = "M83")
            ),
            limit = 5
        )

        assertEquals(
            listOf("other-artist", "remix", "live", "generic", "generic-other"),
            selected.map(Track::id)
        )
    }

    @Test
    fun candidatePoolDeduplicatesVariantsInsideOneBatch() {
        val selected = radioCandidateTracks(
            existingTracks = emptyList(),
            candidates = listOf(
                track("a", "Lose Yourself", artist = "Eminem"),
                track("b", "Lose Yourself (Official Music Video)", artist = "EminemVEVO"),
                track("c", "Stan", artist = "Eminem feat. Dido")
            ),
            limit = 5
        )

        assertEquals(listOf("a", "c"), selected.map(Track::id))
    }

    @Test
    fun candidatePoolDoesNotTreatFeatOrFtPrefixesAsCredits() {
        val existing = listOf(track("feature", "Same Song", artist = "Feature"))

        val selected = radioCandidateTracks(
            existingTracks = existing,
            candidates = listOf(
                track("ftisland", "Same Song", artist = "FTISLAND"),
                track("feature-fresh", "Different Song", artist = "Feature")
            ),
            limit = 5
        )

        assertEquals(listOf("ftisland", "feature-fresh"), selected.map(Track::id))
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

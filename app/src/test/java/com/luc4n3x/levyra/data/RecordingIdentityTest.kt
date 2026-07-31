package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.viewmodel.isPlaybackCandidateCompatible
import com.luc4n3x.levyra.viewmodel.playbackCandidateScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class RecordingIdentityTest {
    @Test
    fun normalizationIsLocaleInvariantForTurkishDevices() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("TRI012345678", normalizedIsrc("tr-i01-23-45678"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun exactIsrcWinsBeforeTextMatching() {
        val target = Track(id = "spotify", title = "Completely different", artist = "A", album = "X", durationMs = 1, streamUrl = "", videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "", source = "Spotify", moodTags = emptySet(), energy = 0, vocal = 0, replayScore = 0, cacheScore = 0, accentStart = 0, accentEnd = 0, isrc = "IT-B00-20-00001")
        val candidate = target.copy(id = "youtube", title = "Other upload", isrc = "ITB002000001")
        assertTrue(isPlaybackCandidateCompatible(target, candidate))
        assertEquals(10_000, playbackCandidateScore(target, candidate))
    }

    @Test
    fun conflictingIsrcRejectsAnOtherwisePerfectCandidate() {
        val target = Track(id = "spotify", title = "Song", artist = "Artist", album = "Album", durationMs = 180000, streamUrl = "", videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "", source = "Spotify", moodTags = emptySet(), energy = 0, vocal = 0, replayScore = 0, cacheScore = 0, accentStart = 0, accentEnd = 0, isrc = "ITB002000001")
        val candidate = target.copy(id = "youtube", isrc = "USAAA2100001")
        assertFalse(isPlaybackCandidateCompatible(target, candidate))
        assertEquals(Int.MIN_VALUE, playbackCandidateScore(target, candidate))
    }
}

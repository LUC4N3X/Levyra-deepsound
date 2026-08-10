package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.viewmodel.isPlaybackCandidateCompatible
import com.luc4n3x.levyra.viewmodel.playbackCandidateScore
import com.luc4n3x.levyra.viewmodel.selectPreferredVideoPlaybackCandidate
import com.luc4n3x.levyra.viewmodel.videoPlaybackCandidateScore
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
        assertEquals(
            Int.MIN_VALUE,
            videoPlaybackCandidateScore(target, candidate.copy(videoType = "MUSIC_VIDEO_TYPE_ATV"))
        )
    }

    @Test
    fun officialMusicVideoOutranksArtTrackForSameRecording() {
        val target = Track(
            id = "catalog", title = "Song", artist = "Artist", album = "Album", durationMs = 180000,
            streamUrl = "", videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "", source = "Catalog",
            moodTags = emptySet(), energy = 0, vocal = 0, replayScore = 0, cacheScore = 0,
            accentStart = 0, accentEnd = 0, isrc = "ITB002000001"
        )
        val artTrack = target.copy(id = "audio123456", videoType = "MUSIC_VIDEO_TYPE_ATV")
        val untypedVideo = target.copy(id = "unknown1234", videoType = "")
        val officialVideo = target.copy(id = "video123456", videoType = "MUSIC_VIDEO_TYPE_OMV")

        assertTrue(
            videoPlaybackCandidateScore(target, artTrack) <
                videoPlaybackCandidateScore(target, untypedVideo)
        )
        assertTrue(videoPlaybackCandidateScore(target, officialVideo) > videoPlaybackCandidateScore(target, artTrack))
    }

    @Test
    fun preferredVideoSelectionRejectsArtTracksAndUnrelatedOfficialVideos() {
        val target = Track(
            id = "catalog", title = "AL MIO PAESE", artist = "Serena Brancale, Levante, DELIA",
            album = "SACRO", durationMs = 197_591, streamUrl = "", videoUrl = "", thumbnailUrl = "",
            largeThumbnailUrl = "", source = "Catalog", moodTags = emptySet(), energy = 0, vocal = 0,
            replayScore = 0, cacheScore = 0, accentStart = 0, accentEnd = 0
        )
        val artTrack = target.copy(
            id = "Y7ztJQaiCZc",
            videoUrl = "https://www.youtube.com/watch?v=Y7ztJQaiCZc",
            videoType = "MUSIC_VIDEO_TYPE_ATV"
        )
        val unrelatedOfficial = target.copy(
            id = "wrong123456",
            title = "Different song",
            videoUrl = "https://www.youtube.com/watch?v=wrong123456",
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )
        val official = target.copy(
            id = "BXpFlt8VwDk",
            videoUrl = "https://www.youtube.com/watch?v=BXpFlt8VwDk",
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )

        val selected = selectPreferredVideoPlaybackCandidate(
            target,
            listOf(artTrack, unrelatedOfficial, official)
        )

        assertEquals("BXpFlt8VwDk", selected?.id)
    }
}

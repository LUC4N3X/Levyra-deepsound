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

    @Test
    fun officialSourceMusicOutranksUserUploadedVideo() {
        val target = Track(
            id = "catalog", title = "Song", artist = "Artist", album = "Album", durationMs = 180_000,
            streamUrl = "", videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "", source = "Catalog",
            moodTags = emptySet(), energy = 0, vocal = 0, replayScore = 0, cacheScore = 0,
            accentStart = 0, accentEnd = 0
        )
        val fanUpload = target.copy(id = "ugc12345678", videoType = "MUSIC_VIDEO_TYPE_UGC")
        val officialSource = target.copy(
            id = "osm12345678",
            videoType = "MUSIC_VIDEO_TYPE_OFFICIAL_SOURCE_MUSIC"
        )

        assertTrue(
            videoPlaybackCandidateScore(target, officialSource) >
                videoPlaybackCandidateScore(target, fanUpload)
        )
        assertEquals(
            "osm12345678",
            selectPreferredVideoPlaybackCandidate(target, listOf(fanUpload, officialSource))?.id
        )
    }

    @Test
    fun sharedArtistChannelOutranksAnUnrelatedChannelForTheSameVideoType() {
        val target = Track(
            id = "catalog", title = "Song", artist = "Artist", album = "Album", durationMs = 180_000,
            streamUrl = "", videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "", source = "Catalog",
            moodTags = emptySet(), energy = 0, vocal = 0, replayScore = 0, cacheScore = 0,
            accentStart = 0, accentEnd = 0, artistBrowseIds = listOf("UCofficialchannel1")
        )
        val otherChannel = target.copy(
            id = "other123456",
            videoType = "MUSIC_VIDEO_TYPE_UGC",
            artistBrowseIds = listOf("UCsomeoneelse999")
        )
        val artistChannel = target.copy(
            id = "artist123456",
            videoType = "MUSIC_VIDEO_TYPE_UGC",
            artistBrowseIds = listOf("UCofficialchannel1")
        )

        assertTrue(
            videoPlaybackCandidateScore(target, artistChannel) >
                videoPlaybackCandidateScore(target, otherChannel)
        )
    }

    @Test
    fun authoritativePairingSurvivesAnOfficialVideoOfAnotherRecording() {
        val target = Track(
            id = "catalog", title = "Song Tonight", artist = "Artist", album = "Album",
            durationMs = 180_000, streamUrl = "", videoUrl = "", thumbnailUrl = "",
            largeThumbnailUrl = "", source = "Catalog", moodTags = emptySet(), energy = 0, vocal = 0,
            replayScore = 0, cacheScore = 0, accentStart = 0, accentEnd = 0
        )
        val pairedLyricVideo = target.copy(
            id = "pairedvid01",
            videoUrl = "https://www.youtube.com/watch?v=pairedvid01",
            videoType = "MUSIC_VIDEO_TYPE_UGC"
        )
        val otherSingleOfficial = target.copy(
            id = "othersing01",
            title = "Song Tonight Forever",
            videoUrl = "https://www.youtube.com/watch?v=othersing01",
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )

        val selected = selectPreferredVideoPlaybackCandidate(
            target,
            listOf(pairedLyricVideo, otherSingleOfficial),
            setOf("pairedvid01")
        )

        assertEquals("pairedvid01", selected?.id)
    }

    @Test
    fun exactIsrcOfficialVideoStillDisplacesTheAuthoritativePairing() {
        val target = Track(
            id = "catalog", title = "Song", artist = "Artist", album = "Album", durationMs = 180_000,
            streamUrl = "", videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "",
            source = "Catalog", moodTags = emptySet(), energy = 0, vocal = 0, replayScore = 0,
            cacheScore = 0, accentStart = 0, accentEnd = 0, isrc = "ITB002000001"
        )
        val pairedLyricVideo = target.copy(
            id = "pairedvid01",
            videoUrl = "https://www.youtube.com/watch?v=pairedvid01",
            videoType = "MUSIC_VIDEO_TYPE_UGC",
            isrc = ""
        )
        val sameRecordingOfficial = target.copy(
            id = "officialvid",
            videoUrl = "https://www.youtube.com/watch?v=officialvid",
            videoType = "MUSIC_VIDEO_TYPE_OMV",
            isrc = "ITB002000001"
        )

        val selected = selectPreferredVideoPlaybackCandidate(
            target,
            listOf(pairedLyricVideo, sameRecordingOfficial),
            setOf("pairedvid01")
        )

        assertEquals("officialvid", selected?.id)
    }

    @Test
    fun equallyScoredCandidatesKeepTheFirstListedWatchEntry() {
        val target = Track(
            id = "catalog", title = "Song", artist = "Artist", album = "Album", durationMs = 180_000,
            streamUrl = "", videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "",
            source = "Catalog", moodTags = emptySet(), energy = 0, vocal = 0, replayScore = 0,
            cacheScore = 0, accentStart = 0, accentEnd = 0
        )
        val watchEntry = target.copy(
            id = "watchvideo1",
            videoUrl = "https://www.youtube.com/watch?v=watchvideo1",
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )
        val searchEntry = target.copy(
            id = "searchvideo",
            videoUrl = "https://www.youtube.com/watch?v=searchvideo",
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )

        assertEquals(
            "watchvideo1",
            selectPreferredVideoPlaybackCandidate(target, listOf(watchEntry, searchEntry))?.id
        )
    }

    @Test
    fun daDioAcceptsReportedOfficialVideoIdentity() {
        val target = Track(
            id = "audio123456",
            title = "Da Dio",
            artist = "Bresh",
            album = "",
            durationMs = 173_000L,
            streamUrl = "",
            videoUrl = "https://www.youtube.com/watch?v=audio123456",
            thumbnailUrl = "",
            largeThumbnailUrl = "",
            source = "YouTube Music",
            moodTags = emptySet(),
            energy = 0,
            vocal = 0,
            replayScore = 0,
            cacheScore = 0,
            accentStart = 0,
            accentEnd = 0,
            videoType = "MUSIC_VIDEO_TYPE_ATV",
            audioVideoId = "audio123456"
        )
        val official = target.copy(
            id = "-ZwDJaZ2coY",
            videoUrl = "https://www.youtube.com/watch?v=-ZwDJaZ2coY",
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )

        assertEquals(
            "-ZwDJaZ2coY",
            selectPreferredVideoPlaybackCandidate(target, listOf(official))?.id
        )
    }
}

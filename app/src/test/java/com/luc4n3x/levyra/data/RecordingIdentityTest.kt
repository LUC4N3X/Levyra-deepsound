package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.parseCompactViewCount
import com.luc4n3x.levyra.domain.videoViewCountBonus
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

    @Test
    fun daDioPrefersTheOfficialVideoOverTheAudioLengthReUpload() {
        val target = artistTrack(
            id = "0Jp_YOOEovg",
            title = "Da Dio",
            artist = "Bresh",
            durationMs = 174_000L,
            videoType = "MUSIC_VIDEO_TYPE_ATV"
        )
        val official = target.copy(
            id = "-ZwDJaZ2coY",
            videoUrl = "https://www.youtube.com/watch?v=-ZwDJaZ2coY",
            durationMs = 179_000L,
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )
        val audioLengthReUpload = target.copy(
            id = "XxSIyhr_bFc",
            videoUrl = "https://www.youtube.com/watch?v=XxSIyhr_bFc",
            durationMs = 174_000L,
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )

        assertEquals(
            "-ZwDJaZ2coY",
            selectPreferredVideoPlaybackCandidate(target, listOf(official, audioLengthReUpload))?.id
        )
    }

    @Test
    fun daiDaiPrefersTheOfficialVideoOverTheAudioLengthReUpload() {
        val target = artistTrack(
            id = "lFQdcPTTzSg",
            title = "Dai Dai",
            artist = "Shakira e Burna Boy",
            durationMs = 224_000L,
            videoType = "MUSIC_VIDEO_TYPE_ATV"
        )
        val official = target.copy(
            id = "fcnDmrtj6Sk",
            videoUrl = "https://www.youtube.com/watch?v=fcnDmrtj6Sk",
            durationMs = 241_000L,
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )
        val audioLengthReUpload = target.copy(
            id = "0QZYJRnv-rA",
            title = "Dai dai",
            videoUrl = "https://www.youtube.com/watch?v=0QZYJRnv-rA",
            durationMs = 224_000L,
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )

        assertEquals(
            "fcnDmrtj6Sk",
            selectPreferredVideoPlaybackCandidate(target, listOf(official, audioLengthReUpload))?.id
        )
    }

    @Test
    fun videoScoringIgnoresAudioDurationProximity() {
        val target = artistTrack(
            id = "lFQdcPTTzSg",
            title = "Dai Dai",
            artist = "Shakira e Burna Boy",
            durationMs = 224_000L,
            videoType = "MUSIC_VIDEO_TYPE_ATV"
        )
        val longerOfficial = target.copy(
            id = "fcnDmrtj6Sk",
            videoUrl = "https://www.youtube.com/watch?v=fcnDmrtj6Sk",
            durationMs = 241_000L,
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )
        val exactLengthOfficial = longerOfficial.copy(
            id = "0QZYJRnv-rA",
            videoUrl = "https://www.youtube.com/watch?v=0QZYJRnv-rA",
            durationMs = 224_000L
        )

        assertEquals(
            videoPlaybackCandidateScore(target, exactLengthOfficial),
            videoPlaybackCandidateScore(target, longerOfficial)
        )
        assertTrue(
            playbackCandidateScore(target, exactLengthOfficial) >
                playbackCandidateScore(target, longerOfficial)
        )
    }

    @Test
    fun featuringCreditsDoNotHideTheOfficialVideo() {
        val target = artistTrack(
            id = "tlmoVWfCejI",
            title = "Baby (feat. J Balvin)",
            artist = "Sfera Ebbasta",
            durationMs = 194_000L,
            videoType = "MUSIC_VIDEO_TYPE_ATV"
        )
        val official = target.copy(
            id = "1RpKdCl2uN4",
            title = "Baby (Official Video)",
            artist = "J Balvin & Sfera Ebbasta",
            videoUrl = "https://www.youtube.com/watch?v=1RpKdCl2uN4",
            durationMs = 201_000L,
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )
        val fanUpload = target.copy(
            id = "fZL9IYLAPHE",
            title = "Sfera Ebbasta - Baby (Feat J Balvin)",
            artist = "Actis",
            videoUrl = "https://www.youtube.com/watch?v=fZL9IYLAPHE",
            durationMs = 191_000L,
            videoType = "MUSIC_VIDEO_TYPE_UGC",
            artistBrowseIds = emptyList()
        )

        assertTrue(isPlaybackCandidateCompatible(target, official))
        assertEquals(
            "1RpKdCl2uN4",
            selectPreferredVideoPlaybackCandidate(target, listOf(official, fanUpload))?.id
        )
    }

    @Test
    fun audioUploadOnTheArtistChannelLosesToTheOfficialVideo() {
        val target = artistTrack(
            id = "PvM79DJ2PmM",
            title = "The Less I Know The Better",
            artist = "Tame Impala",
            durationMs = 217_000L,
            videoType = "MUSIC_VIDEO_TYPE_ATV"
        )
        val audioUpload = target.copy(
            id = "2SUwOgmvzK4",
            title = "The Less I Know The Better (Audio)",
            videoUrl = "https://www.youtube.com/watch?v=2SUwOgmvzK4",
            durationMs = 218_000L,
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )
        val official = target.copy(
            id = "sBzrzS1Ag_g",
            title = "The Less I Know The Better (Official Video)",
            videoUrl = "https://www.youtube.com/watch?v=sBzrzS1Ag_g",
            durationMs = 343_000L,
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )

        assertEquals(
            "sBzrzS1Ag_g",
            selectPreferredVideoPlaybackCandidate(target, listOf(audioUpload, official))?.id
        )
    }

    @Test
    fun audioOnlyOfficialUploadStillBeatsAUserVideo() {
        val target = artistTrack(
            id = "4D7u5KF7SP8",
            title = "Get Lucky (feat. Pharrell Williams and Nile Rodgers)",
            artist = "Daft Punk",
            durationMs = 370_000L,
            videoType = "MUSIC_VIDEO_TYPE_ATV"
        )
        val officialAudio = target.copy(
            id = "5NV6Rdv1a3I",
            title = "Get Lucky (Official Audio) (feat. Nile Rodgers)",
            videoUrl = "https://www.youtube.com/watch?v=5NV6Rdv1a3I",
            durationMs = 249_000L,
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )
        val reUpload = target.copy(
            id = "CCHdMIEGaaM",
            title = "Daft Punk - Get Lucky (Official Video) feat. Pharrell Williams",
            artist = "convar HUN",
            videoUrl = "https://www.youtube.com/watch?v=CCHdMIEGaaM",
            durationMs = 248_000L,
            videoType = "MUSIC_VIDEO_TYPE_UGC",
            artistBrowseIds = emptyList()
        )

        assertEquals(
            "5NV6Rdv1a3I",
            selectPreferredVideoPlaybackCandidate(target, listOf(officialAudio, reUpload))?.id
        )
    }

    @Test
    fun compactViewCountsAreReadInEveryPublishedLabelShape() {
        assertEquals(772_000_000L, parseCompactViewCount("772 Mln di visualizzazioni"))
        assertEquals(772_000_000L, parseCompactViewCount("772M views"))
        assertEquals(2_600_000L, parseCompactViewCount("2,6 Mln di visualizzazioni"))
        assertEquals(2_600_000L, parseCompactViewCount("2.6M views"))
        assertEquals(74_000L, parseCompactViewCount("74K views"))
        assertEquals(772_000_000L, parseCompactViewCount("772 Mio. Aufrufe"))
        assertEquals(1_200L, parseCompactViewCount("1,2 mil visualizações"))
        assertEquals(791L, parseCompactViewCount("791 visualizzazioni"))
        assertEquals(-1L, parseCompactViewCount("4:01"))
        assertEquals(-1L, parseCompactViewCount("2023"))
        assertEquals(-1L, parseCompactViewCount(""))
    }

    @Test
    fun viewCountBonusStaysBelowTheArtistChannelSignal() {
        assertEquals(0, videoViewCountBonus(-1L))
        assertEquals(0, videoViewCountBonus(0L))
        assertTrue(videoViewCountBonus(772_000_000L) > videoViewCountBonus(2_600_000L))
        assertTrue(videoViewCountBonus(Long.MAX_VALUE) < 6_000)
    }

    @Test
    fun viewCountSeparatesTheOfficialVideoFromReUploadsOnTheSameChannel() {
        val target = artistTrack(
            id = "levyra-4500512f5aa38edfe312",
            title = "Dai Dai",
            artist = "Shakira, Burna Boy",
            durationMs = 223_448L,
            videoType = ""
        ).copy(artistBrowseIds = emptyList())
        val reUpload = target.copy(
            id = "ux255_NUR2o",
            title = "Dai dai",
            artist = "Shakira e Burna Boy",
            videoUrl = "https://www.youtube.com/watch?v=ux255_NUR2o",
            durationMs = 220_000L,
            videoType = "MUSIC_VIDEO_TYPE_OMV",
            youtubeViewCount = 2_600_000L
        )
        val official = reUpload.copy(
            id = "fcnDmrtj6Sk",
            title = "Dai Dai",
            videoUrl = "https://www.youtube.com/watch?v=fcnDmrtj6Sk",
            durationMs = 241_000L,
            youtubeViewCount = 772_000_000L
        )

        assertEquals(
            "fcnDmrtj6Sk",
            selectPreferredVideoPlaybackCandidate(target, listOf(reUpload, official))?.id
        )
    }

    @Test
    fun viewCountNeverPromotesAnAudioUploadOverTheOfficialVideo() {
        val target = artistTrack(
            id = "AdEKgwUqPKI",
            title = "Kill Bill",
            artist = "SZA",
            durationMs = 154_000L,
            videoType = "MUSIC_VIDEO_TYPE_ATV"
        )
        val officialAudio = target.copy(
            id = "SQnc1QibapQ",
            title = "Kill Bill (Official Audio)",
            videoUrl = "https://www.youtube.com/watch?v=SQnc1QibapQ",
            durationMs = 156_000L,
            videoType = "MUSIC_VIDEO_TYPE_OMV",
            youtubeViewCount = 167_000_000L
        )
        val official = officialAudio.copy(
            id = "MSRcC626prw",
            title = "Kill Bill (Official Video)",
            videoUrl = "https://www.youtube.com/watch?v=MSRcC626prw",
            durationMs = 276_000L,
            youtubeViewCount = 142_000_000L
        )

        assertEquals(
            "MSRcC626prw",
            selectPreferredVideoPlaybackCandidate(target, listOf(officialAudio, official))?.id
        )
    }

    @Test
    fun chartCollaborationReachesTheOfficialVideoWithoutAnyYoutubeIdentity() {
        val chartEntry = artistTrack(
            id = "chart-daidai",
            title = "Dai Dai",
            artist = "Shakira, Burna Boy",
            durationMs = 223_448L,
            videoType = ""
        ).copy(videoUrl = "", audioVideoId = "", artistBrowseIds = emptyList())
        val official = artistTrack(
            id = "fcnDmrtj6Sk",
            title = "Dai Dai",
            artist = "Shakira, Burna Boy",
            durationMs = 241_000L,
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        ).copy(youtubeViewCount = 774_000_000L)
        val fanUpload = artistTrack(
            id = "cu0SrLCiFqk",
            title = "DAI DAI FIFA WORLD CUP 2026 Shakira, Burna Boy",
            artist = "Godvin Robin",
            durationMs = 68_000L,
            videoType = "MUSIC_VIDEO_TYPE_UGC"
        ).copy(artistBrowseIds = emptyList(), youtubeViewCount = 1_600_000L)

        assertTrue(isPlaybackCandidateCompatible(chartEntry, official))
        assertEquals(
            "fcnDmrtj6Sk",
            selectPreferredVideoPlaybackCandidate(chartEntry, listOf(official, fanUpload))?.id
        )
    }

    @Test
    fun aTruncatedCreditIsRejectedWhileTheFullCollaborationCreditIsAccepted() {
        val chartEntry = artistTrack(
            id = "chart-collab",
            title = "Dai Dai",
            artist = "Shakira, Burna Boy",
            durationMs = 200_000L,
            videoType = ""
        ).copy(videoUrl = "", audioVideoId = "", artistBrowseIds = emptyList())
        val truncatedCredit = artistTrack(
            id = "aaaaaaaaaaa",
            title = "Dai Dai",
            artist = "Shakira",
            durationMs = 210_000L,
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )
        val fullCredit = truncatedCredit.copy(artist = "Shakira, Burna Boy")

        assertFalse(isPlaybackCandidateCompatible(chartEntry, truncatedCredit))
        assertTrue(isPlaybackCandidateCompatible(chartEntry, fullCredit))
    }

    @Test
    fun aSharedArtistChannelSatisfiesTheArtistGate() {
        val target = artistTrack(
            id = "PvM79DJ2PmM",
            title = "Dai Dai",
            artist = "Shakira, Burna Boy",
            durationMs = 200_000L,
            videoType = "MUSIC_VIDEO_TYPE_ATV"
        )
        val truncatedCredit = artistTrack(
            id = "aaaaaaaaaaa",
            title = "Dai Dai",
            artist = "Shakira",
            durationMs = 210_000L,
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )

        assertTrue(isPlaybackCandidateCompatible(target, truncatedCredit))
        assertFalse(
            isPlaybackCandidateCompatible(target, truncatedCredit.copy(artistBrowseIds = listOf("UC_OTHER")))
        )
    }

    private fun artistTrack(
        id: String,
        title: String,
        artist: String,
        durationMs: Long,
        videoType: String
    ): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = "",
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=$id",
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
        videoType = videoType,
        audioVideoId = id,
        artistBrowseIds = listOf("UCo6JijJGA3IvIiPsawDK3Ww")
    )
}

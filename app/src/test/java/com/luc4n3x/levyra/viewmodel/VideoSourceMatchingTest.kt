package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.VideoUploadEvidence
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoSourceMatchingTest {

    private fun track(
        id: String,
        title: String,
        artist: String,
        durationMs: Long,
        videoType: String,
        artistBrowseIds: List<String> = emptyList(),
        views: Long = -1L
    ) = Track(
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
        artistBrowseIds = artistBrowseIds,
        youtubeViewCount = views
    )

    private val montana = listOf("UC4-6_ZDEUvmsdgVNmoqt26Q")

    private val unforgettable = track(
        "48ydDUQ16RE", "Unforgettable (feat. Swae Lee)", "French Montana", 234_000L,
        "MUSIC_VIDEO_TYPE_ATV", montana
    )

    private fun unforgettableVideo(id: String, durationMs: Long, views: Long) = track(
        id, "Unforgettable (feat. Swae Lee)", "French Montana", durationMs,
        "MUSIC_VIDEO_TYPE_OMV", montana, views
    )

    private val lyricVideo = unforgettableVideo("MIcIza4sqaM", 231_000L, 173_000_000L)
    private val officialVideo = unforgettableVideo("CTFtOOh47oo", 287_000L, 1_900_000_000L)
    private val unforgettableSearch = listOf(
        lyricVideo,
        unforgettableVideo("gWy4zh8wQWA", 234_000L, 2_600_000L),
        unforgettableVideo("Bd2ggFjIcRc", 233_000L, 579_000L),
        officialVideo
    )

    private val unforgettableEvidence = listOf(
        VideoUploadEvidence(
            videoId = "MIcIza4sqaM",
            title = "French Montana - Unforgettable (Lyrics) ft. Swae Lee",
            channelName = "7clouds Rock",
            channelVerified = true,
            officialArtistChannel = false,
            durationMs = 231_000L,
            viewCount = 173_459_618L
        ),
        VideoUploadEvidence(
            videoId = "CTFtOOh47oo",
            title = "French Montana - Unforgettable ft. Swae Lee",
            channelName = "French Montana",
            channelVerified = true,
            officialArtistChannel = true,
            durationMs = 287_000L,
            viewCount = 1_987_756_494L
        )
    ).associateBy { it.videoId }

    private val dilrubaArtists = listOf("UCs8PJDBt1jl1p3NpvRpgP-Q", "UCOqb8hsdE1p1f6FiG7nSWpg")

    private val dilruba = track(
        "cw-Jnb6IIEA", "Dilruba Dilnasheen", "Rito Riba & Siddhant kaushal", 174_000L,
        "MUSIC_VIDEO_TYPE_ATV", dilrubaArtists
    )

    private fun dilrubaVideo(id: String, durationMs: Long, views: Long) = track(
        id, "Dilruba Dilnasheen", "Siddhant kaushal & Rito Riba", durationMs,
        "MUSIC_VIDEO_TYPE_OMV", dilrubaArtists, views
    )

    private val dilrubaOfficial = dilrubaVideo("Ddm7VUgw5Vw", 178_000L, 47_000_000L)

    private val dilrubaSpam = track(
        "xJvfCMmK-DY",
        "DILRUBA DILNASHEEN - Dilfek -Rito Riba | Siddhant Kaushal (full song) #dilrubadilnasheen | Harshitmuzicworld",
        "Harshitmuzicworld",
        173_000L,
        "",
        views = 931_249L
    )

    private val dilrubaEvidence = listOf(
        VideoUploadEvidence(
            videoId = "Ddm7VUgw5Vw",
            title = "DILRUBA DILNASHEEN - Dilfek - Rito Riba | Siddhant Kaushal | Arjun & Zahabiya | Anshul Garg | Play DMF",
            channelName = "Play DMF and RITO RIBA",
            channelVerified = false,
            officialArtistChannel = false,
            durationMs = 178_000L,
            viewCount = 47_817_223L
        ),
        VideoUploadEvidence(
            videoId = "_5KLn99WKTI",
            title = "Dilruba Dilnasheen - Rito Riba (Testo)",
            channelName = "7clouds",
            channelVerified = true,
            officialArtistChannel = false,
            durationMs = 174_000L,
            viewCount = 1_202_242L
        ),
        VideoUploadEvidence(
            videoId = "xJvfCMmK-DY",
            title = dilrubaSpam.title,
            channelName = "Harshitmuzicworld",
            channelVerified = false,
            officialArtistChannel = false,
            durationMs = 173_000L,
            viewCount = 931_249L
        )
    ).associateBy { it.videoId }

    @Test
    fun unforgettableOfficialVevoVideoBeatsTheEarlierRankedLyricVideo() {
        val ranked = rankVideoPlaybackCandidates(unforgettable, unforgettableSearch, evidence = unforgettableEvidence)

        assertEquals("CTFtOOh47oo", ranked.first().id)
        assertEquals("MIcIza4sqaM", ranked.last().id)
    }

    @Test
    fun vevoChannelCountsAsOfficialOnlyWhenTheTitleIsClean() {
        val vevo = VideoUploadEvidence(
            videoId = "CTFtOOh47oo",
            title = "French Montana - Unforgettable ft. Swae Lee",
            channelName = "FrenchMontanaVEVO",
            channelVerified = true,
            officialArtistChannel = false,
            durationMs = 287_000L,
            viewCount = 1_987_756_494L
        )

        assertEquals(VIDEO_OFFICIAL_CHANNEL_BONUS, uploaderChannelBonus(unforgettable, vevo))
        assertEquals(0, uploaderChannelBonus(unforgettable, vevo.copy(title = "Unforgettable (Official Audio)")))
        assertTrue(
            uploaderChannelBonus(unforgettable, vevo.copy(channelName = "FanVEVO", channelVerified = false)) < VIDEO_OFFICIAL_CHANNEL_BONUS
        )
        assertEquals(0, uploaderChannelBonus(unforgettable, unforgettableEvidence.getValue("MIcIza4sqaM")))
    }

    @Test
    fun anotherArtistsOfficialChannelCoveringTheSongIsNotTheOfficialVideo() {
        val cover = VideoUploadEvidence(
            videoId = "coverclip01",
            title = "French Montana - Unforgettable ft. Swae Lee | Conor Maynard & Anth",
            channelName = "Conor Maynard",
            channelVerified = true,
            officialArtistChannel = true,
            durationMs = 273_000L,
            viewCount = 20_000_000L
        )

        assertTrue(uploaderChannelBonus(unforgettable, cover) < VIDEO_OFFICIAL_CHANNEL_BONUS)
    }

    @Test
    fun officialBadgeRequiresACleanTitleAndTheArtistsOwnUpload() {
        val cover = track(
            "coverclip01", "French Montana - Unforgettable ft. Swae Lee (Cover)", "Conor Maynard",
            273_000L, "MUSIC_VIDEO_TYPE_OMV", listOf("UCcoverartist")
        )

        assertTrue(isOfficialVideoSource(unforgettable, officialVideo, unforgettableEvidence["CTFtOOh47oo"]))
        assertFalse(isOfficialVideoSource(unforgettable, lyricVideo, unforgettableEvidence["MIcIza4sqaM"]))
        assertFalse(isOfficialVideoSource(unforgettable, cover, null))
        assertTrue(isOfficialVideoSource(unforgettable.copy(artistBrowseIds = emptyList()), officialVideo, null))
    }

    @Test
    fun localizedFeatureCreditsAreLeftOutOfTheVideoQueries() {
        assertEquals("unforgettable", videoSourceQueryTitle("Unforgettable (con Swae Lee)"))
        assertEquals("unforgettable", videoSourceQueryTitle("Unforgettable (feat. Swae Lee)"))
        assertEquals("dilruba dilnasheen", videoSourceQueryTitle("Dilruba Dilnasheen"))
    }

    @Test
    fun theDominantOfficialVideoWinsEvenWithoutWebEvidence() {
        assertEquals(
            "CTFtOOh47oo",
            selectPreferredVideoPlaybackCandidate(unforgettable, unforgettableSearch)?.id
        )
    }

    @Test
    fun dilrubaLabelVideoBeatsAudioLengthReUploadsAndTheHashtagSpamCopy() {
        val search = listOf(
            dilrubaSpam,
            dilrubaVideo("6piyW8LxvK8", 174_000L, 378L),
            dilrubaVideo("_5KLn99WKTI", 174_000L, 1_100_000L),
            dilrubaOfficial
        )

        val ranked = rankVideoPlaybackCandidates(dilruba, search, evidence = dilrubaEvidence)

        assertEquals("Ddm7VUgw5Vw", ranked.first().id)
        assertEquals("xJvfCMmK-DY", ranked.last().id)
    }

    @Test
    fun webOnlyCollaborationUploadWithTheArtistChannelBeatsTheCatalogVisualizer() {
        val webOnlyOfficial = track(
            "Ddm7VUgw5Vw",
            "DILRUBA DILNASHEEN - Dilfek - Rito Riba | Siddhant Kaushal | Arjun & Zahabiya | Anshul Garg | Play DMF",
            "Play DMF e RITO RIBA",
            178_000L,
            "MUSIC_VIDEO_TYPE_UGC",
            views = 47_817_223L
        )
        val evidence = mapOf(
            "Ddm7VUgw5Vw" to dilrubaEvidence.getValue("Ddm7VUgw5Vw").copy(channelName = "Play DMF e RITO RIBA")
        )
        val search = listOf(
            dilrubaVideo("2wah7ZFgER8", 174_000L, 1_100_000L),
            webOnlyOfficial
        )

        assertEquals("Ddm7VUgw5Vw", selectPreferredVideoPlaybackCandidate(dilruba, search, evidence = evidence)?.id)
        assertTrue(isOfficialVideoSource(dilruba, webOnlyOfficial, evidence["Ddm7VUgw5Vw"]))
    }

    @Test
    fun unverifiedSingleChannelNamedLikeTheArtistIsNotOfficial() {
        val impostor = VideoUploadEvidence(
            videoId = "impostor001",
            title = "Dilruba Dilnasheen Rito Riba",
            channelName = "Rito Riba",
            channelVerified = false,
            officialArtistChannel = false,
            durationMs = 174_000L,
            viewCount = 1_000L
        )

        assertTrue(uploaderChannelBonus(dilruba, impostor) < VIDEO_OFFICIAL_CHANNEL_BONUS)
    }

    @Test
    fun webEvidenceRevealsAVersionTheMusicCatalogTitleHides() {
        val remixEvidence = VideoUploadEvidence(
            videoId = "gWy4zh8wQWA",
            title = "French Montana - Unforgettable (Remix) ft. Swae Lee",
            channelName = "Some Channel",
            channelVerified = false,
            officialArtistChannel = false,
            durationMs = 234_000L,
            viewCount = 2_600_000L
        )

        val ranked = rankVideoPlaybackCandidates(
            unforgettable,
            unforgettableSearch,
            evidence = unforgettableEvidence + (remixEvidence.videoId to remixEvidence)
        )

        assertFalse(ranked.any { it.id == "gWy4zh8wQWA" })
    }

    @Test
    fun officialArtistChannelUploadFoundOnlyOnTheWebOutranksAFanUpload() {
        val fanUpload = track(
            "fanupload01", "French Montana - Unforgettable ft. Swae Lee", "Music Uploads",
            234_000L, "MUSIC_VIDEO_TYPE_UGC"
        )
        val oacUpload = track(
            "oacupload01", "French Montana - Unforgettable (Official Music Video) ft. Swae Lee",
            "French Montana", 290_000L, ""
        )
        val evidence = mapOf(
            "oacupload01" to VideoUploadEvidence(
                videoId = "oacupload01",
                title = oacUpload.title,
                channelName = "French Montana",
                channelVerified = true,
                officialArtistChannel = true,
                durationMs = 290_000L,
                viewCount = 10_000_000L
            )
        )

        assertEquals(
            "oacupload01",
            selectPreferredVideoPlaybackCandidate(unforgettable, listOf(fanUpload, oacUpload), evidence = evidence)?.id
        )
    }

    @Test
    fun officialVideosTolerateCinematicIntrosWhileUploadsMustMatchTheRecording() {
        assertEquals(0, videoDurationPenalty(234_000L, 287_000L, official = true))
        assertEquals(0, videoDurationPenalty(234_000L, 340_000L, official = true))
        assertEquals(0, videoDurationPenalty(234_000L, 240_000L, official = false))
        assertTrue(videoDurationPenalty(234_000L, 287_000L, official = false) > 0)
        assertTrue(videoDurationPenalty(234_000L, 150_000L, official = true) > 0)
        assertEquals(0, videoDurationPenalty(0L, 287_000L, official = false))
    }

    @Test
    fun modeSwitchPositionStaysInsideTheTargetMedia() {
        assertEquals(60_000L, modeSwitchPositionMs(60_000L, 287_000L))
        assertEquals(228_000L, modeSwitchPositionMs(260_000L, 234_000L))
        assertEquals(60_000L, modeSwitchPositionMs(60_000L, 0L))
        assertEquals(0L, modeSwitchPositionMs(-5L, 234_000L))
    }
}

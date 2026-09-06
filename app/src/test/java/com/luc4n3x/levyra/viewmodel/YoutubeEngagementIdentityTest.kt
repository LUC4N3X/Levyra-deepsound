package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.YoutubeCommentsState
import com.luc4n3x.levyra.domain.YoutubeEngagementState
import com.luc4n3x.levyra.domain.YoutubeComment
import com.luc4n3x.levyra.data.YoutubeCommentsPage
import com.luc4n3x.levyra.data.YoutubeCommentsResult
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeEngagementIdentityTest {
    @Test
    fun unconfirmedVideoUrlDoesNotOverrideYoutubeMusicSongIdentity() {
        val track = track(
            id = "aaaaaaaaaaa",
            videoUrl = "https://www.youtube.com/watch?v=bbbbbbbbbbb",
            counterpartVideoId = "ccccccccccc"
        )

        assertEquals("aaaaaaaaaaa", youtubeEngagementVideoId(track))
    }

    @Test
    fun confirmedOfficialVideoWinsWhileOriginalAudioIdentityIsPreserved() {
        val track = track(
            id = "audio123456",
            videoUrl = "https://www.youtube.com/watch?v=fcnDmrtj6Sk",
            counterpartVideoId = "fcnDmrtj6Sk",
            audioVideoId = "audio123456"
        ).copy(videoType = "MUSIC_VIDEO_TYPE_OMV")

        assertEquals("fcnDmrtj6Sk", youtubeEngagementVideoId(track))
    }

    @Test
    fun counterpartIsUsedWhenTrackIdIsNotAYouTubeId() {
        val track = track(
            id = "chart-123",
            videoUrl = "",
            counterpartVideoId = "ccccccccccc"
        )

        assertEquals("ccccccccccc", youtubeEngagementVideoId(track))
    }

    @Test
    fun malformedIdentifiersAreRejected() {
        assertEquals("", youtubeEngagementVideoId(track(id = "invalid id!")))
    }

    @Test
    fun rawElevenCharacterIdsAreRejectedForNonYoutubeTracks() {
        assertEquals(
            "",
            youtubeEngagementVideoId(
                track(id = "abcdefghijk", source = "Local library")
            )
        )
    }

    @Test
    fun currentTrackAAllowsCommentsRequestForOpenedTrackBToComplete() {
        val trackA = track(id = "aaaaaaaaaaa")
        val trackB = track(id = "bbbbbbbbbbb")
        val state = LevyraUiState(
            currentTrack = trackA,
            youtubeEngagement = YoutubeEngagementState(
                videoId = trackB.id,
                comments = YoutubeCommentsState(
                    videoId = trackB.id,
                    visible = true,
                    loading = true
                )
            )
        )

        val loaded = state.withYoutubeCommentsResultIfCurrent(
            videoId = trackB.id,
            generation = 7L,
            currentGeneration = 7L,
            result = YoutubeCommentsResult.Available(
                YoutubeCommentsPage(
                    videoId = trackB.id,
                    countText = "1",
                    commentsDisabled = false,
                    items = listOf(YoutubeComment(id = "comment-b", author = "Author", text = "Comment B")),
                    nextToken = "next-b"
                )
            )
        )

        assertEquals(true, loaded.youtubeEngagement.comments.loaded)
        assertEquals("Comment B", loaded.youtubeEngagement.comments.items.single().text)
        assertEquals("next-b", loaded.youtubeEngagement.comments.nextToken)
        assertEquals(trackA.id, loaded.currentTrack?.id)
    }

    @Test
    fun liveChatPollingKeepsSafetyFloorAndHonorsServerDelay() {
        assertEquals(2_000L, youtubeLiveChatPollIntervalMs(0L))
        assertEquals(8_500L, youtubeLiveChatPollIntervalMs(8_500L))
        assertEquals(60_000L, youtubeLiveChatPollIntervalMs(999_999L))
    }

    @Test
    fun continuationCyclesAreStoppedAcrossSuccessfulPages() {
        assertEquals(
            "",
            nextYoutubeCommentsToken(
                requestedToken = "token-b",
                candidateToken = "token-a",
                successfulTokens = setOf("token-a", "token-b")
            )
        )
        assertEquals(
            "token-c",
            nextYoutubeCommentsToken(
                requestedToken = "token-b",
                candidateToken = "token-c",
                successfulTokens = setOf("token-a", "token-b")
            )
        )
    }

    private fun track(
        id: String,
        videoUrl: String = "",
        counterpartVideoId: String = "",
        audioVideoId: String = "",
        source: String = "YouTube Music"
    ) = Track(
        id = id,
        title = "Title",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = videoUrl,
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = source,
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0,
        accentEnd = 0,
        counterpartVideoId = counterpartVideoId,
        audioVideoId = audioVideoId
    )
}

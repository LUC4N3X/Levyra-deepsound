package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeEngagementIdentityTest {
    @Test
    fun exactVideoUrlWinsOverCatalogFallbacks() {
        val track = track(
            id = "aaaaaaaaaaa",
            videoUrl = "https://www.youtube.com/watch?v=bbbbbbbbbbb",
            counterpartVideoId = "ccccccccccc"
        )

        assertEquals("bbbbbbbbbbb", youtubeEngagementVideoId(track))
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
        assertEquals("", youtubeEngagementVideoId(track(id = "not-a-video")))
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
        counterpartVideoId = counterpartVideoId
    )
}

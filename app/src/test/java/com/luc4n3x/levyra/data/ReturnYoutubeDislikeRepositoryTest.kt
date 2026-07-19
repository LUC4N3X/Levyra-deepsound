package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReturnYoutubeDislikeRepositoryTest {
    @Test
    fun parsesValidEstimateForExpectedVideo() {
        val estimate = parseReturnYoutubeDislikeResponse(
            json = """
                {
                  "id": "kxOuG8jMIgI",
                  "likes": 31885,
                  "rawDislikes": 31946,
                  "rawLikes": 457,
                  "dislikes": 579721,
                  "rating": 1.2085329444119253,
                  "viewCount": 3762293,
                  "deleted": false
                }
            """.trimIndent(),
            expectedVideoId = "kxOuG8jMIgI"
        )

        requireNotNull(estimate)
        assertEquals("kxOuG8jMIgI", estimate.videoId)
        assertEquals(31_885L, estimate.likes)
        assertEquals(579_721L, estimate.dislikes)
        assertEquals(31_946L, estimate.rawDislikes)
        assertEquals(3_762_293L, estimate.viewCount)
        assertTrue(estimate.rating > 0.0)
    }

    @Test
    fun acceptsValidCountsWhenOptionalRatingIsMissing() {
        val estimate = parseReturnYoutubeDislikeResponse(
            json = """{"id":"kxOuG8jMIgI","likes":1,"dislikes":2}""",
            expectedVideoId = "kxOuG8jMIgI"
        )

        requireNotNull(estimate)
        assertEquals(2L, estimate.dislikes)
        assertEquals(0.0, estimate.rating, 0.0)
    }

    @Test
    fun rejectsResponseBelongingToAnotherVideo() {
        val estimate = parseReturnYoutubeDislikeResponse(
            json = """{"id":"aaaaaaaaaaa","likes":1,"dislikes":2,"rating":4.0}""",
            expectedVideoId = "bbbbbbbbbbb"
        )

        assertNull(estimate)
    }

    @Test
    fun rejectsNegativeCountsAndMalformedIds() {
        assertNull(
            parseReturnYoutubeDislikeResponse(
                json = """{"id":"kxOuG8jMIgI","likes":1,"dislikes":-2,"rating":4.0}""",
                expectedVideoId = "kxOuG8jMIgI"
            )
        )
        assertNull(
            parseReturnYoutubeDislikeResponse(
                json = """{"id":"kxOuG8jMIgI","likes":1,"dislikes":2,"viewCount":-1}""",
                expectedVideoId = "kxOuG8jMIgI"
            )
        )
        assertNull(
            parseReturnYoutubeDislikeResponse(
                json = "{}",
                expectedVideoId = "not-a-video-id"
            )
        )
    }
}

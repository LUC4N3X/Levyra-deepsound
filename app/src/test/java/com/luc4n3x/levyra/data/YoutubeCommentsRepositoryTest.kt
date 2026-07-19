package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeCommentsRepositoryTest {
    @Test
    fun acceptsOnlyApprovedHttpsAvatarHosts() {
        assertEquals(
            "https://yt3.ggpht.com/avatar=s88-c-k-c0x00ffffff-no-rj",
            sanitizeYoutubeAvatarUrl("https://yt3.ggpht.com/avatar=s88-c-k-c0x00ffffff-no-rj")
        )
        assertEquals("", sanitizeYoutubeAvatarUrl("http://yt3.ggpht.com/avatar"))
        assertEquals("", sanitizeYoutubeAvatarUrl("https://127.0.0.1/avatar"))
        assertEquals("", sanitizeYoutubeAvatarUrl("https://yt3.ggpht.com.evil.example/avatar"))
    }

    @Test
    fun acceptsOnlyCanonicalYoutubeAuthorUrls() {
        assertEquals(
            "https://www.youtube.com/@artist",
            sanitizeYoutubeAuthorUrl("https://www.youtube.com/@artist")
        )
        assertEquals("", sanitizeYoutubeAuthorUrl("https://example.com/@artist"))
        assertEquals("", sanitizeYoutubeAuthorUrl("https://user@youtube.com/@artist"))
    }

    @Test
    fun emptyParsedPagesKeepAdvancingWithoutLooping() {
        assertEquals(
            "next-token",
            nextEmptyCommentsContinuation(
                parsedItemCount = 0,
                nextToken = " next-token ",
                seenTokens = emptySet(),
                hops = 0,
                maxHops = 6
            )
        )
        assertEquals(
            null,
            nextEmptyCommentsContinuation(
                parsedItemCount = 0,
                nextToken = "next-token",
                seenTokens = setOf("next-token"),
                hops = 1,
                maxHops = 6
            )
        )
        assertEquals(
            null,
            nextEmptyCommentsContinuation(
                parsedItemCount = 1,
                nextToken = "next-token",
                seenTokens = emptySet(),
                hops = 0,
                maxHops = 6
            )
        )
    }
}

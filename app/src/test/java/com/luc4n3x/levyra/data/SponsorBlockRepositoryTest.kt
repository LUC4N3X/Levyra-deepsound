package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.SponsorSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SponsorBlockRepositoryTest {

    @Test
    fun olderCompletionCannotOverwriteAlreadyPublishedSegments() {
        val cache = linkedMapOf<String, List<SponsorSegment>>()
        val newerResult = listOf(SponsorSegment(1_000L, 2_000L, "sponsor"))
        val olderResult = listOf(SponsorSegment(3_000L, 4_000L, "intro"))

        val firstPublished = publishSponsorBlockCacheResult(cache, "video", newerResult)
        val staleCompletion = publishSponsorBlockCacheResult(cache, "video", olderResult)

        assertSame(newerResult, firstPublished)
        assertSame(newerResult, staleCompletion)
        assertEquals(newerResult, cache["video"])
    }

    @Test
    fun negativeCacheAlsoCannotReplaceAlreadyPublishedSegments() {
        val cache = linkedMapOf<String, List<SponsorSegment>>()
        val published = listOf(SponsorSegment(5_000L, 6_000L, "outro"))

        publishSponsorBlockCacheResult(cache, "video", published)
        val staleNotFound = publishSponsorBlockCacheResult(cache, "video", emptyList())

        assertSame(published, staleNotFound)
        assertEquals(published, cache["video"])
    }
}

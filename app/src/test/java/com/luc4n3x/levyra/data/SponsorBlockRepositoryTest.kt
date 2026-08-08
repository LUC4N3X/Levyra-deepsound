package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.SponsorSegment
import java.io.ByteArrayInputStream
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class SponsorBlockRepositoryTest {

    @Test
    fun boundedReaderAcceptsBodyExactlyAtByteLimit() {
        val body = "éé"
        val bytes = body.toByteArray(Charsets.UTF_8)

        assertEquals(body, readUtf8Bounded(ByteArrayInputStream(bytes), bytes.size.toLong()))
    }

    @Test
    fun boundedReaderRejectsBodyOneByteOverLimit() {
        val body = "ééx"
        val bytes = body.toByteArray(Charsets.UTF_8)

        assertNull(readUtf8Bounded(ByteArrayInputStream(bytes), (bytes.size - 1).toLong()))
    }

    @Test
    fun rateLimitAndServerFailuresAreNotCached() {
        val fetcher = QueueSponsorBlockFetcher(
            response(429),
            response(500)
        )
        val repository = SponsorBlockRepository(fetcher) { 1_000L }

        assertTrue(repositorySegments(repository, "video").isEmpty())
        assertTrue(repositorySegments(repository, "video").isEmpty())
        assertEquals(2, fetcher.calls)
    }

    @Test
    fun cancellationFromFetcherPropagates() {
        val repository = SponsorBlockRepository(
            SponsorBlockHttpFetcher { throw CancellationException("cancelled") }
        ) { 1_000L }

        try {
            repositorySegments(repository, "video")
            throw AssertionError("Expected CancellationException")
        } catch (error: CancellationException) {
            assertEquals("cancelled", error.message)
        }
    }

    @Test
    fun negativeCacheExpiresAndThenAcceptsPositiveSegments() {
        var now = 10_000L
        val fetcher = QueueSponsorBlockFetcher(
            response(404),
            response(200, """[{"segment":[1.0,2.5],"category":"sponsor"}]""")
        )
        val repository = SponsorBlockRepository(fetcher) { now }

        assertTrue(repositorySegments(repository, "video").isEmpty())
        assertTrue(repositorySegments(repository, "video").isEmpty())
        assertEquals(1, fetcher.calls)

        now += SPONSORBLOCK_NEGATIVE_TTL_MS + 1L
        val refreshed = repositorySegments(repository, "video")
        assertEquals(1, refreshed.size)
        assertEquals(1_000L, refreshed.single().startMs)
        assertEquals(2_500L, refreshed.single().endMs)
        assertEquals(2, fetcher.calls)
    }

    @Test
    fun positivePublicationReplacesAnExistingNegativeEntry() {
        val cache = linkedMapOf<String, SponsorBlockCacheEntry>()
        val positive = listOf(SponsorSegment(2_000L, 3_000L, "intro"))

        publishSponsorBlockCacheResult(cache, "video", emptyList(), 1_000L)
        val published = publishSponsorBlockCacheResult(cache, "video", positive, 1_500L)

        assertEquals(positive, published)
        assertEquals(positive, cache["video"]?.segments)
    }

    @Test
    fun laterNegativePublicationCannotReplacePositiveSegments() {
        val cache = linkedMapOf<String, SponsorBlockCacheEntry>()
        val positive = listOf(SponsorSegment(4_000L, 5_000L, "outro"))

        publishSponsorBlockCacheResult(cache, "video", positive, 1_000L)
        val published = publishSponsorBlockCacheResult(cache, "video", emptyList(), 1_500L)

        assertEquals(positive, published)
        assertEquals(positive, cache["video"]?.segments)
    }

    @Test
    fun oversizedDeclaredBodyIsRejectedWithoutCaching() {
        val fetcher = QueueSponsorBlockFetcher(
            response(200, "[]", SPONSORBLOCK_MAX_RESPONSE_BYTES + 1L),
            response(200, "[]", SPONSORBLOCK_MAX_RESPONSE_BYTES + 1L)
        )
        val repository = SponsorBlockRepository(fetcher) { 1_000L }

        assertTrue(repositorySegments(repository, "video").isEmpty())
        assertTrue(repositorySegments(repository, "video").isEmpty())
        assertEquals(2, fetcher.calls)
    }

    @Test
    fun accessOrderedCacheRemainsBounded() {
        val cache = object : java.util.LinkedHashMap<String, SponsorBlockCacheEntry>(128, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SponsorBlockCacheEntry>?): Boolean =
                size > SPONSORBLOCK_CACHE_LIMIT
        }
        repeat(SPONSORBLOCK_CACHE_LIMIT + 10) { index ->
            publishSponsorBlockCacheResult(cache, "video-$index", emptyList(), index.toLong())
        }
        assertEquals(SPONSORBLOCK_CACHE_LIMIT, cache.size)
    }

    private fun repositorySegments(repository: SponsorBlockRepository, videoId: String): List<SponsorSegment> =
        kotlinx.coroutines.runBlocking { repository.segments(videoId) }

    private class QueueSponsorBlockFetcher(vararg responses: SponsorBlockHttpResponse) : SponsorBlockHttpFetcher {
        private val queue = responses.toMutableList()
        var calls: Int = 0
            private set

        override fun fetch(url: String): SponsorBlockHttpResponse {
            calls += 1
            return queue.removeAt(0)
        }
    }

    private fun response(
        code: Int,
        body: String = "",
        declaredLength: Long = body.toByteArray().size.toLong()
    ): SponsorBlockHttpResponse = SponsorBlockHttpResponse(
        code = code,
        declaredLength = declaredLength,
        body = ByteArrayInputStream(body.toByteArray())
    )
}

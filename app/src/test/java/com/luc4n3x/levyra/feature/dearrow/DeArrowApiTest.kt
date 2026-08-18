package com.luc4n3x.levyra.feature.dearrow

import java.io.ByteArrayInputStream
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeArrowApiTest {

    @Test
    fun invalidVideoIdNeverCallsFetcher() {
        val fetcher = RecordingFetcher()
        val api = DeArrowApi(fetcher)

        assertNull(runBlocking { api.branding("short") })
        assertNull(runBlocking { api.branding("this-id-is-too-long") })
        assertNull(runBlocking { api.branding("bad!id!!!!!") })
        assertEquals(0, fetcher.calls)
    }

    @Test
    fun malformedJsonIsHandledAsFailure() {
        val fetcher = RecordingFetcher(FetchAction.Return(response(200, "not json")))
        val api = DeArrowApi(fetcher)

        assertNull(runBlocking { api.branding("abcdefghijk") })
        assertEquals(1, fetcher.calls)
    }

    @Test
    fun successfulResponseIsParsed() {
        val body = """
            {
              "titles": [{"title": "Real Title", "original": false, "votes": 4, "locked": true}],
              "thumbnails": [{"timestamp": 12.5, "original": false, "votes": 2, "locked": false}]
            }
        """.trimIndent()
        val fetcher = RecordingFetcher(FetchAction.Return(response(200, body)))
        val api = DeArrowApi(fetcher)

        val branding = runBlocking { api.branding("abcdefghijk") }
        assertEquals(1, branding?.titles?.size)
        assertEquals("Real Title", branding?.titles?.single()?.title)
        assertEquals(12.5, branding?.thumbnails?.single()?.timestamp)
    }

    @Test
    fun transientFailureRetriesOnceThenSucceeds() {
        val fetcher = RecordingFetcher(
            FetchAction.Throw(IOException("boom")),
            FetchAction.Return(response(200, """{"titles":[],"thumbnails":[]}"""))
        )
        val api = DeArrowApi(fetcher)

        val branding = runBlocking { api.branding("abcdefghijk") }
        assertTrue(branding != null)
        assertEquals(2, fetcher.calls)
    }

    @Test
    fun repeatedTransientFailureIsBoundedToMaxAttempts() {
        val fetcher = RecordingFetcher(
            FetchAction.Throw(IOException("boom")),
            FetchAction.Throw(IOException("boom again"))
        )
        val api = DeArrowApi(fetcher)

        assertNull(runBlocking { api.branding("abcdefghijk") })
        assertEquals(DeArrowApi.MAX_ATTEMPTS, fetcher.calls)
    }

    @Test
    fun notFoundDoesNotRetry() {
        val fetcher = RecordingFetcher(FetchAction.Return(response(404, "")))
        val api = DeArrowApi(fetcher)

        assertNull(runBlocking { api.branding("abcdefghijk") })
        assertEquals(1, fetcher.calls)
    }

    @Test
    fun oversizedDeclaredLengthIsRejected() {
        val fetcher = RecordingFetcher(
            FetchAction.Return(response(200, "{}", declaredLength = DeArrowApi.MAX_RESPONSE_BYTES + 1))
        )
        val api = DeArrowApi(fetcher)

        assertNull(runBlocking { api.branding("abcdefghijk") })
        assertEquals(1, fetcher.calls)
    }

    @Test
    fun repeatedTransientFailureIsInconclusive() {
        val fetcher = RecordingFetcher(
            FetchAction.Throw(IOException("boom")),
            FetchAction.Throw(IOException("boom again"))
        )
        val api = DeArrowApi(fetcher)

        assertEquals(
            DeArrowBrandingOutcome.Inconclusive,
            runBlocking { api.brandingOutcome("abcdefghijk") }
        )
        assertEquals(DeArrowApi.MAX_ATTEMPTS, fetcher.calls)
    }

    @Test
    fun notFoundIsConclusiveAbsence() {
        val fetcher = RecordingFetcher(FetchAction.Return(response(404, "")))
        val api = DeArrowApi(fetcher)

        assertEquals(
            DeArrowBrandingOutcome.Resolved(null),
            runBlocking { api.brandingOutcome("abcdefghijk") }
        )
        assertEquals(1, fetcher.calls)
    }

    @Test
    fun oversizedStreamedBodyIsRejectedWhenLengthIsUnknown() {
        val body = "A".repeat((DeArrowApi.MAX_RESPONSE_BYTES + 1L).toInt())
        val fetcher = RecordingFetcher(FetchAction.Return(response(200, body, declaredLength = -1L)))
        val api = DeArrowApi(fetcher)

        assertNull(runBlocking { api.branding("abcdefghijk") })
        assertEquals(1, fetcher.calls)
    }

    @Test
    fun thumbnailUrlIsBuiltFromVideoIdAndTimestamp() {
        val url = DeArrowApi.thumbnailUrl("abcdefghijk", 42.0)
        assertEquals(
            "https://dearrow-thumb.ajay.app/api/v1/getThumbnail?videoID=abcdefghijk&time=42.0",
            url
        )
    }

    @Test
    fun thumbnailUrlRejectsInvalidVideoIdOrNegativeTimestamp() {
        assertNull(DeArrowApi.thumbnailUrl("short", 1.0))
        assertNull(DeArrowApi.thumbnailUrl("abcdefghijk", -1.0))
    }

    private fun response(code: Int, body: String, declaredLength: Long = body.toByteArray().size.toLong()) =
        DeArrowHttpResponse(
            code = code,
            declaredLength = declaredLength,
            body = ByteArrayInputStream(body.toByteArray())
        )

    private sealed interface FetchAction {
        data class Return(val response: DeArrowHttpResponse) : FetchAction
        data class Throw(val error: Throwable) : FetchAction
    }

    private class RecordingFetcher(vararg actions: FetchAction) : DeArrowHttpFetcher {
        private val queue = actions.toMutableList()
        var calls: Int = 0
            private set

        override fun fetch(url: String): DeArrowHttpResponse {
            calls += 1
            return when (val action = queue.removeAt(0)) {
                is FetchAction.Return -> action.response
                is FetchAction.Throw -> throw action.error
            }
        }
    }
}

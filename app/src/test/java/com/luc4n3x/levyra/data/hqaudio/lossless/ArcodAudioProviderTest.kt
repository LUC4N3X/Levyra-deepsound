package com.luc4n3x.levyra.data.hqaudio.lossless

import com.luc4n3x.levyra.data.hqaudio.ProviderFailure
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpExchange
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpRequest
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpResponse
import com.luc4n3x.levyra.data.hqaudio.ProviderSearchOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderStreamOutcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArcodAudioProviderTest {
    private val search = """{"success":true,"data":{"tracks":{"items":[
        {"id":7400,"title":"Billie Jean","duration":294,"isrc":"USSM19902991",
         "performer":{"name":"Michael Jackson"},"album":{"title":"Thriller"}}
    ]}}}"""

    private fun response(body: String, code: Int = 200) =
        ProviderHttpResponse(code, emptyMap(), body.toByteArray())

    @Test
    fun resolvesCdFlacFromTheMatchedRecordingAndVerifiesTheBytes() = runBlocking {
        val requests = mutableListOf<ProviderHttpRequest>()
        val provider = ArcodAudioProvider(ProviderHttpExchange { request ->
            requests += request
            when (requests.size) {
                1 -> response(search)
                2 -> response("""{"url":"https://api.arcod.xyz/v2/stream/play?t=synthetic"}""")
                else -> ProviderHttpResponse(206,
                    mapOf("Content-Type" to "audio/flac", "Content-Range" to "bytes 0-8191/33248426"),
                    hex("664c6143000000221000100000001200505e2b1103700316d00dc855b4b636043fa58d765c5be1e3f95f"))
            }
        })
        val candidate = (provider.search("Michael Jackson Billie Jean") as ProviderSearchOutcome.Found).candidates.single()
        assertEquals("arcod", candidate.providerId)
        assertEquals("7400", candidate.providerTrackId)
        assertEquals(294, candidate.durationSeconds)
        assertEquals("USSM19902991", candidate.isrc)
        val result = provider.resolveStream(candidate) as ProviderStreamOutcome.Resolved
        assertEquals("flac", result.stream.codec)
        assertTrue(requests[1].url.endsWith("/7400?quality=6"))
        assertNotNull(requests[2].headers["Range"])
    }

    @Test
    fun malformedSearchIsNotCachedAsAnEmptyCatalog() = runBlocking {
        val provider = ArcodAudioProvider(ProviderHttpExchange { response("""{"success":false}""") })
        assertEquals(ProviderFailure.MALFORMED_RESPONSE, (provider.search("track") as ProviderSearchOutcome.Failed).failure)
    }

    @Test
    fun rejectsLookalikesCredentialsAndUnexpectedStreamRoutes() {
        listOf("http://api.arcod.xyz/v2/stream/play?t=x", "https://api.arcod.xyz.evil.test/v2/stream/play?t=x",
            "https://api.arcod.xyz:8443/v2/stream/play?t=x", "https://user@api.arcod.xyz/v2/stream/play?t=x",
            "https://api.arcod.xyz/admin?t=x", "https://127.0.0.1/v2/stream/play?t=x").forEach {
            assertFalse(it, ArcodPayloadParser.isStreamUrl(it))
        }
        assertTrue(ArcodPayloadParser.isStreamUrl("https://api.arcod.xyz/v2/stream/play?t=synthetic"))
    }

    @Test
    fun rateLimitStopsFurtherRequestsDuringCooldown() = runBlocking {
        var count = 0
        val provider = ArcodAudioProvider(ProviderHttpExchange { count++; response("", 429) })
        assertEquals(ProviderFailure.RATE_LIMITED, (provider.search("one") as ProviderSearchOutcome.Failed).failure)
        assertEquals(ProviderFailure.CIRCUIT_OPEN, (provider.search("two") as ProviderSearchOutcome.Failed).failure)
        assertEquals(1, count)
    }

    @Test
    fun versionMetadataAndNestedRowsKeepRecordingIdentity() {
        val body = """{"success":true,"data":{"tracks":{"items":[{"item":
            {"id":1,"title":"Song","version":"Live","duration":123,"performer":{"name":"Artist"}}},
            {"id":"../x","title":"Bad","duration":123,"performer":{"name":"Artist"}}
        ]}}}"""
        val candidate = ArcodPayloadParser.searchPage(body)!!.single()
        assertEquals("Song (Live)", candidate.title)
        assertEquals(listOf("Artist"), candidate.primaryArtists)
    }

    private fun hex(value: String) = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

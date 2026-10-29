package com.luc4n3x.levyra.data.hqaudio.qobuz

import com.luc4n3x.levyra.data.hqaudio.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class QobuzAudioProviderTest {
    private val account = QobuzCredentials.Account("synthetic-token", "123", "synthetic-secret", "1")
    private fun track(version: String = "") = JSONObject("""{"id":123,"title":"Blinding Lights","version":"$version","duration":200,"performer":{"name":"The Weeknd"},"album":{"title":"After Hours"},"isrc":"USUG11904206","streamable":true,"parental_warning":false}""")
    private fun flac(bits: Int = 24, rate: Int = 96_000, seconds: Int = 200): ByteArray {
        val bytes = ByteArray(42)
        "fLaC".toByteArray().copyInto(bytes)
        bytes[4] = 0x80.toByte(); bytes[7] = 34
        val packed = (rate.toLong() shl 44) or (1L shl 41) or ((bits - 1).toLong() shl 36) or (rate.toLong() * seconds)
        for (i in 0..7) bytes[18 + i] = (packed ushr (56 - 8 * i)).toByte()
        return bytes
    }
    private fun response(code: Int, body: String = "{}") = ProviderHttpResponse(code, emptyMap(), body.toByteArray())
    private fun api(exchange: QobuzExchange) = QobuzApi(exchange, { 1_000_000L }, listOf(account))

    @Test fun preservesRecordingMetadataAndRejectsWrongVersions() {
        val candidate = QobuzPayload.candidate(track("Live"))!!
        assertEquals("qobuz", candidate.providerId)
        assertEquals("USUG11904206", candidate.isrc)
        assertEquals(false, candidate.explicit)
        assertFalse(AlternativeTrackMatcher().evaluateReadings(query(), candidate).accepted)
        assertTrue(AlternativeTrackMatcher().evaluateReadings(query(isrc = candidate.isrc), QobuzPayload.candidate(track())!!).accepted)
    }

    @Test fun restrictedAndIncompleteRowsAreNotCandidates() {
        assertNull(QobuzPayload.candidate(track().put("streamable", false)))
        assertNull(QobuzPayload.candidate(track().put("duration", 0)))
        assertNull(QobuzPayload.candidate(track().put("id", "../123")))
        assertNull(QobuzPayload.candidate(track().put("performer", JSONObject())))
    }

    @Test fun streamUsesFlacHeadersInsteadOfApiQualityAndCachesOnlyVerifiedStreams() = runBlocking {
        for ((bits, rate) in listOf(16 to 44_100, 24 to 44_100, 24 to 48_000, 24 to 96_000, 24 to 192_000)) {
            var probes = 0
            val provider = QobuzAudioProvider(api(QobuzExchange { request, _ ->
                if (request.url.host == "www.qobuz.com") response(200, """{"track_id":123,"format_id":27,"bit_depth":8,"sampling_rate":8,"url":"https://streaming-qobuz-std.akamaized.net/test.flac"}""")
                else {
                    probes++
                    assertNull(request.header("X-User-Auth-Token"))
                    ProviderHttpResponse(206, mapOf("Content-Type" to "audio/flac", "Content-Range" to "bytes 0-8191/10000000"), flac(bits, rate))
                }
            }), { 1_000_000L })
            val candidate = QobuzPayload.candidate(track())!!
            val result = provider.resolveStream(candidate) as ProviderStreamOutcome.Resolved
            assertEquals(bits, result.stream.bitDepth)
            assertEquals(rate, result.stream.sampleRateHz)
            provider.resolveStream(candidate)
            assertEquals(1, probes)
        }
    }

    @Test fun refusesPreviewWrongDurationAndNonFlac() = runBlocking {
        for (body in listOf(flac(seconds = 30), ByteArray(42), flac(seconds = 0))) {
            val provider = QobuzAudioProvider(api(QobuzExchange { request, _ ->
                if (request.url.host == "www.qobuz.com") response(200, """{"track_id":123,"format_id":27,"url":"https://streaming-qobuz-std.akamaized.net/test.flac"}""")
                else ProviderHttpResponse(206, mapOf("Content-Type" to "audio/flac", "Content-Range" to "bytes 0-8191/10000000"), body)
            }))
            assertTrue(provider.resolveStream(QobuzPayload.candidate(track())!!) is ProviderStreamOutcome.Unavailable)
        }
    }

    @Test fun httpFailuresOpenOnlyQobuzCircuitWithoutRetries() = runBlocking {
        for ((code, failure) in listOf(401 to ProviderFailure.FORBIDDEN, 403 to ProviderFailure.FORBIDDEN, 429 to ProviderFailure.RATE_LIMITED, 503 to ProviderFailure.HTTP_ERROR)) {
            var calls = 0
            val provider = QobuzAudioProvider(api(QobuzExchange { _, _ -> calls++; response(code) }))
            assertEquals(failure, (provider.search("test") as ProviderSearchOutcome.Failed).failure)
            assertEquals(ProviderFailure.CIRCUIT_OPEN, (provider.search("next") as ProviderSearchOutcome.Failed).failure)
            assertEquals(1, calls)
        }
    }

    @Test fun cancellationIsNotCachedOrCountedAsAnOutage() = runBlocking {
        val provider = QobuzAudioProvider(api(QobuzExchange { _, _ -> throw CancellationException("synthetic") }))
        try { provider.search("test"); fail("Cancellation swallowed") } catch (_: CancellationException) { }
        assertEquals("CLOSED", provider.health().single().state)
    }

    @Test fun onlyAuthorizedQobuzHostsCanReceiveRequests() {
        assertTrue(QobuzNetwork.allows("https://streaming-qobuz-std.akamaized.net/a".toHttpUrl()))
        for (url in listOf("http://www.qobuz.com/a", "https://www.qobuz.com.evil.test/a", "https://127.0.0.1/a", "https://user@www.qobuz.com/a", "https://www.qobuz.com:8443/a")) {
            assertFalse(QobuzNetwork.allows(url.toHttpUrl()))
        }
    }

    @Test fun signingAndCredentialFallbackAreBounded() = runBlocking {
        val second = QobuzCredentials.Account("second-token", "456", "second-secret", "2")
        val tokens = mutableListOf<String?>()
        val api = QobuzApi(QobuzExchange { request, _ ->
            tokens += request.header("X-User-Auth-Token")
            assertNull(request.url.queryParameter("user_auth_token"))
            val timestamp = request.url.queryParameter("request_ts")!!
            val secret = if (tokens.size == 1) account.appSecret else second.appSecret
            assertEquals(QobuzApi.signature("123", "27", timestamp, secret), request.url.queryParameter("request_sig"))
            response(if (tokens.size == 1) 401 else 200)
        }, { 1_000_000L }, listOf(account, second))
        api.get("track/getFileUrl", mapOf("track_id" to "123", "format_id" to "27", "intent" to "stream"))
        assertEquals(listOf("synthetic-token", "second-token"), tokens)
    }
}

package com.luc4n3x.levyra.data.hqaudio.lossless

import com.luc4n3x.levyra.data.hqaudio.ProviderFailure
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpExchange
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpRequest
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpResponse
import com.luc4n3x.levyra.data.hqaudio.ProviderSearchOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderStreamOutcome
import com.luc4n3x.levyra.data.hqaudio.AlternativeMatchSelection
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackMatcher
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackQuery
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
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
        val candidate = ArcodPayloadParser.searchPage(body)!!.candidates.single()
        assertEquals("Song (Live)", candidate.title)
        assertEquals(listOf("Artist"), candidate.primaryArtists)
    }

    @Test
    fun followsCatalogOffsetsWithinTheExistingSearchBound() = runBlocking {
        val requests = mutableListOf<ProviderHttpRequest>()
        val provider = ArcodAudioProvider(ProviderHttpExchange { request ->
            requests += request
            response("""{"success":true,"data":{"tracks":{"total":151,"items":[
                {"id":7400,"title":"Song","duration":123,"performer":{"name":"Artist"}}
            ]}}}""")
        })

        assertTrue((provider.search("Artist Song", 1) as ProviderSearchOutcome.Found).hasMore)
        assertTrue((provider.search("Artist Song", 2) as ProviderSearchOutcome.Found).hasMore)
        assertFalse((provider.search("Artist Song", 3) as ProviderSearchOutcome.Found).hasMore)
        assertTrue((provider.search("Artist Song", 4) as ProviderSearchOutcome.Found).candidates.isEmpty())
        assertTrue((provider.search("Artist Song", 0) as ProviderSearchOutcome.Found).candidates.isEmpty())
        assertEquals(listOf(0, 50, 100), requests.map { it.url.substringAfter("offset=").substringBefore('&').toInt() })
    }

    @Test
    fun emptyOrExhaustedPagesDoNotAdvertiseMoreResults() {
        assertFalse(ArcodPayloadParser.searchPage(search)!!.hasMore)
        assertFalse(ArcodPayloadParser.searchPage(
            """{"success":true,"data":{"tracks":{"total":1000,"items":[]}}}"""
        )!!.hasMore)
        assertFalse(ArcodPayloadParser.searchPage(
            """{"success":true,"data":{"tracks":{"total":51,"items":[
                {"id":1,"title":"Song","duration":123,"performer":{"name":"Artist"}}
            ]}}}""", offset = 50
        )!!.hasMore)
    }

    @Test
    fun featuredPerformerCreditsDoNotMakeTheSameRecordingAmbiguous() {
        val body = """{"success":true,"data":{"tracks":{"items":[
            {"id":9140031,"title":"Get Lucky","version":"feat. Pharrell Williams and Nile Rodgers",
             "duration":369,"performer":{"name":"Daft Punk feat. Pharrell Williams"},
             "album":{"title":"Random Access Memories"},"isrc":"USQX91300108","parental_warning":false},
            {"id":209446467,"title":"Get Lucky","version":"feat. Pharrell Williams and Nile Rodgers",
             "duration":369,"performer":{"name":"Daft Punk"},
             "album":{"title":"Random Access Memories"},"isrc":"USQX91300108","parental_warning":false}
        ]}}}"""
        val candidates = ArcodPayloadParser.searchPage(body)!!.candidates

        assertEquals(listOf("Daft Punk"), candidates.first().primaryArtists)
        assertEquals(listOf("Pharrell Williams"), candidates.first().featuredArtists)
        assertTrue(AlternativeTrackMatcher().select(
            AlternativeTrackQuery("Get Lucky", "Daft Punk", "Random Access Memories", 369_000L, false), candidates
        ) is AlternativeMatchSelection.Accepted)
    }

    @Test
    fun matchingUnversionedIsrcCorroboratesSingleVersionOnTheOriginalAlbum() {
        val rows = listOf(
            billieRow(7400, "Single Version", 293).put("album", JSONObject().put("title", "Thriller")),
            billieRow(48098088),
            billieRow(171471796).put("album", JSONObject().put("title", "Thriller 40"))
        )
        val candidates = ArcodPayloadParser.searchPage(billieBody(rows))!!.candidates

        val selection = AlternativeTrackMatcher().select(
            AlternativeTrackQuery("Billie Jean", "Michael Jackson", "Thriller", 294_000L, false), candidates
        ) as AlternativeMatchSelection.Accepted

        assertEquals("7400", selection.evaluation.candidate.providerTrackId)
        assertEquals("Billie Jean", candidates.first().title)
        assertEquals("Thriller", candidates.first().album)
        assertEquals("Thriller 25 (Super Deluxe Edition)", candidates[1].album)
    }

    @Test
    fun singleVersionRemainsDistinctWithoutCompatibleUnversionedCorroboration() {
        val peers = listOf(
            null,
            billieRow(48098088).put("isrc", "USSM19902992"),
            billieRow(48098088).put("isrc", ""),
            billieRow(48098088).put("performer", JSONObject().put("name", "Other Artist")),
            billieRow(48098088, duration = 296),
            billieRow(48098088, version = "Live"),
            billieRow(48098088, version = "Remix"),
            billieRow(48098088).put("title", "Billie Jean (Live)"),
            billieRow(48098088).put("parental_warning", true),
            billieRow(48098088).put("performer", JSONObject().put("name", "Michael Jackson feat. Guest"))
        )
        for (peer in peers) {
            val original = billieRow(7400, "Single Version", 293)
                .put("album", JSONObject().put("title", "Thriller"))
            val candidates = ArcodPayloadParser.searchPage(billieBody(listOfNotNull(original, peer)))!!.candidates

            assertEquals("Billie Jean (Single Version)", candidates.first().title)
            assertTrue(AlternativeTrackMatcher().select(
                AlternativeTrackQuery("Billie Jean", "Michael Jackson", "Thriller", 294_000L, false), candidates
            ) is AlternativeMatchSelection.Rejected)
        }
    }

    @Test
    fun singleVersionCorroborationRejectsConflictingTitleExplicitnessWithoutFlags() {
        for ((originalHint, peerHint) in listOf("Clean" to "Explicit", "Explicit" to "Clean")) {
            val original = billieRow(7400, "Single Version", 293)
                .put("title", "Song ($originalHint)")
                .apply { remove("parental_warning") }
            val peer = billieRow(48098088)
                .put("title", "Song ($peerHint)")
                .apply { remove("parental_warning") }

            val candidates = ArcodPayloadParser.searchPage(billieBody(listOf(original, peer)))!!.candidates

            assertEquals("Song ($originalHint) (Single Version)", candidates.first().title)
        }
    }

    @Test
    fun corroborationNeverRemovesOtherVersionLabelsOrMissingIsrc() {
        for (version in listOf("Radio Edit", "Live", "Remix", "Single Version (2020)")) {
            val rows = listOf(billieRow(7400, version, 293), billieRow(48098088))
            val original = ArcodPayloadParser.searchPage(billieBody(rows))!!.candidates.first()
            assertEquals("Billie Jean ($version)", original.title)
        }
        val rows = listOf(billieRow(7400, "Single Version", 293).put("isrc", JSONObject.NULL), billieRow(48098088))
        assertEquals("Billie Jean (Single Version)", ArcodPayloadParser.searchPage(billieBody(rows))!!.candidates.first().title)
    }

    private fun billieRow(id: Int, version: String? = null, duration: Int = 294): JSONObject = JSONObject()
        .put("id", id)
        .put("title", "Billie Jean")
        .put("version", version ?: JSONObject.NULL)
        .put("duration", duration)
        .put("performer", JSONObject().put("name", "Michael Jackson"))
        .put("album", JSONObject().put("title", "Thriller 25 (Super Deluxe Edition)"))
        .put("isrc", "USSM19902991")
        .put("parental_warning", false)

    private fun billieBody(rows: List<JSONObject>): String = JSONObject()
        .put("success", true)
        .put("data", JSONObject().put("tracks", JSONObject().put("items", JSONArray(rows))))
        .toString()

    private fun hex(value: String) = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

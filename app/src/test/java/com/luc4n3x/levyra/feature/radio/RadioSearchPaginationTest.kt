package com.luc4n3x.levyra.feature.radio

import java.io.IOException
import java.util.Collections
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RadioSearchPaginationTest {
    @Test
    fun searchLoadsBeyondSixtyFourAndUsesRawOffsets() = runBlocking {
        val offsets = Collections.synchronizedList(mutableListOf<Int>())
        val api = api { request ->
            if (request.url.queryParameter("name") == null) "[]" else {
                val offset = request.url.queryParameter("offset")!!.toInt()
                offsets.add(offset)
                JSONArray().apply {
                    for (i in offset until minOf(offset + 32, 85)) put(stationJson(i))
                }.toString()
            }
        }
        var page = api.search("Радио")
        val stations = page.stations.toMutableList()
        while (page.next.isNotEmpty()) {
            page = api.search("Радио", page.next)
            stations.addAll(page.stations)
        }
        assertEquals(listOf(0, 32, 64), offsets)
        assertEquals(85, filterAndRankRadioSearchResults(stations, "Радио", false).size)
        assertTrue(stations.all { it.name.startsWith("Радио") })
    }

    @Test
    fun invalidAndDuplicateEntriesDoNotStopPagination() = runBlocking {
        val api = api { request ->
            if (request.url.queryParameter("name") == null) "[]" else {
                val offset = request.url.queryParameter("offset")!!.toInt()
                if (offset == 0) JSONArray().apply {
                    repeat(31) { put(JSONObject().put("stationuuid", "missing-name")) }
                    put(stationJson(1))
                }.toString() else JSONArray().put(stationJson(2)).toString()
            }
        }
        val first = api.search("Радио")
        assertEquals(1, first.stations.size)
        assertEquals(32, first.next.single().offset)
        val second = api.search("Радио", first.next)
        assertEquals("station-2", second.stations.single().uuid)
        assertTrue(second.next.isEmpty())
    }

    @Test
    fun authoritativeServerResultsAreNotRemovedByLocalNameRules() = runBlocking {
        val api = api { request ->
            assertEquals("false", request.url.queryParameter("hidebroken"))
            assertNull(request.url.queryParameter("countrycode"))
            if (request.url.queryParameter("name") != null) JSONArray().put(stationJson(1).put("name", "Я").put("lastcheckok", 0)).toString() else "[]"
        }
        val results = api.search("Я FM").stations
        val ranked = filterAndRankRadioSearchResults(results, "Я FM", requireLocalMatch = false)
        assertEquals("Я", ranked.single().name)
        assertFalse(ranked.single().lastCheckOk)
    }

    @Test
    fun partialFailureKeepsFailedOffsetForRetryWithoutRefetchingCompletedFields() = runBlocking {
        var fail = true
        val api = api { request ->
            when {
                request.url.queryParameter("name") != null && fail -> throw IOException("disconnected")
                request.url.queryParameter("name") != null -> JSONArray().put(stationJson(1)).toString()
                else -> "[]"
            }
        }
        val first = api.search("Радио")
        assertTrue(first.partialFailure)
        assertEquals("name", first.next.single().field)
        assertEquals(0, first.next.single().offset)
        fail = false
        val retried = api.search("Радио", first.next)
        assertFalse(retried.partialFailure)
        assertEquals(1, retried.stations.size)
        assertTrue(retried.next.isEmpty())
    }

    @Test(expected = IOException::class)
    fun completeOutageIsAnErrorRatherThanAnEmptySearch() { runBlocking { api { throw IOException("offline") }.search("Radio") } }

    @Test
    fun requestsKeepOriginalCyrillicAndAddNormalizedFallbacks() {
        val requests = radioSearchRequests("Маяк – Йошкар")
        assertTrue(requests.any { it.value == "Маяк – Йошкар" && !it.tokenFallback })
        assertTrue(requests.any { it.value == normalizeRadioName("Маяк – Йошкар") })
        assertTrue(radioStationMatchesSearch(customRadioStation("Радио Маяк", "https://radio.example/live", ""), "радио МАЯК"))
        assertTrue(radioStationMatchesSearch(customRadioStation("Deejay", "https://radio.example/live", ""), "Radio Deejay"))
    }

    private fun api(reply: (Request) -> String): RadioBrowserApi = RadioBrowserApi(
        client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(reply(chain.request()).toResponseBody()).build()
        }.build(),
        serverPool = RadioBrowserServerPool(lookup = { emptyArray() })
    )

    private fun stationJson(index: Int) = JSONObject().put("stationuuid", "station-$index").put("name", "Радио $index")
        .put("url", "https://radio.example/$index").put("lastcheckok", 1)
}

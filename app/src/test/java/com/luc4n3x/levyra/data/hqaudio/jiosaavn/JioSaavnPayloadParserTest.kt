package com.luc4n3x.levyra.data.hqaudio.jiosaavn

import com.luc4n3x.levyra.data.hqaudio.saavnSong
import com.luc4n3x.levyra.data.hqaudio.searchBody
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JioSaavnPayloadParserTest {
    @Test
    fun searchResultsBecomeCandidatesWithDecodedMetadata() {
        val body = searchBody(
            saavnSong(
                id = "2IuQsex6",
                title = "Levitating (feat. DaBaby)",
                primary = listOf("Dua Lipa"),
                featured = listOf("DaBaby"),
                album = "Future Nostalgia &amp; More",
                duration = 203,
                explicit = "1"
            )
        )
        val candidate = JioSaavnPayloadParser.searchCandidates(body)!!.single()
        assertEquals("jiosaavn", candidate.providerId)
        assertEquals("2IuQsex6", candidate.providerTrackId)
        assertEquals(listOf("Dua Lipa"), candidate.primaryArtists)
        assertEquals(listOf("DaBaby"), candidate.featuredArtists)
        assertEquals("Future Nostalgia & More", candidate.album)
        assertEquals(203, candidate.durationSeconds)
        assertEquals(true, candidate.explicit)
        assertEquals(true, candidate.offers320)
        assertEquals("enc-2IuQsex6", candidate.mediaToken)
    }

    @Test
    fun artistRolesAndReleaseYearAreReadFromSearchMetadata() {
        val song = saavnSong("OFkbNs2Q", "Meri Aashiqui", listOf("Mithoon", "Palak Muchhal", "Arijit Singh"), "Sound Of Bollywood", 267).apply {
            put("year", "2013")
            getJSONObject("more_info").getJSONObject("artistMap").put(
                "artists",
                JSONArray()
                    .put(JSONObject().put("name", "Mithoon").put("role", "music"))
                    .put(JSONObject().put("name", "Palak Muchhal").put("role", "singer"))
                    .put(JSONObject().put("name", "Arijit Singh").put("role", "singer"))
                    .put(JSONObject().put("name", "Irshad Kamil").put("role", "lyricist"))
                    .put(JSONObject().put("name", "Irshad Kamil").put("role", "singer"))
                    .put(JSONObject().put("name", "Shraddha Kapoor").put("role", "starring"))
            )
        }
        val candidate = JioSaavnPayloadParser.searchCandidates(searchBody(song))!!.single()
        assertEquals(setOf("Mithoon", "Shraddha Kapoor"), candidate.nonPerformingArtists)
        assertEquals(setOf("Mithoon", "Irshad Kamil", "Shraddha Kapoor"), candidate.creatorArtists)
        assertEquals(2013, candidate.releaseYear)
    }

    @Test
    fun missingRolesAndMalformedYearStayNeutral() {
        val song = saavnSong("n1", "Song", listOf("Artist"), "Album", 180).apply { put("year", "20xx") }
        val candidate = JioSaavnPayloadParser.searchCandidates(searchBody(song))!!.single()
        assertEquals(emptySet<String>(), candidate.nonPerformingArtists)
        assertEquals(emptySet<String>(), candidate.creatorArtists)
        assertEquals(0, candidate.releaseYear)
    }

    @Test
    fun missingOrUnreadable320FlagIsKeptAsUnknown() {
        val missing = saavnSong("m1", "Missing Flag", listOf("Artist"), "Album", 180).apply {
            getJSONObject("more_info").remove("320kbps")
        }
        val unreadable = saavnSong("m2", "Odd Flag", listOf("Artist"), "Album", 180, offers320 = "maybe")
        val candidates = JioSaavnPayloadParser.searchCandidates(searchBody(missing, unreadable))!!
        assertEquals(listOf(null, null), candidates.map { it.offers320 })
    }

    @Test
    fun songDetailsThatTurnRestrictedAreNotFound() {
        val paywalled = saavnSong("pW-kkdqr", "Blinding Lights", listOf("The Weeknd"), "Blinding Lights", 204).apply {
            getJSONObject("more_info").put("rights", JSONObject().put("code", "2").put("reason", "PRO only"))
        }
        val body = JSONObject().put("songs", JSONArray().put(paywalled)).toString()
        assertEquals(JioSaavnSongDetails.Missing, JioSaavnPayloadParser.songDetails(body, "pW-kkdqr"))
    }

    @Test
    fun nonSongsAndUnplayableEntriesAreSkipped() {
        val album = JSONObject().put("id", "album1").put("title", "An Album").put("type", "album")
        val unplayable = saavnSong("x1", "No Media", listOf("Artist"), "Album", 180, token = "")
        val playable = saavnSong("x2", "Media", listOf("Artist"), "Album", 180, offers320 = "false")
        val candidates = JioSaavnPayloadParser.searchCandidates(searchBody(album, unplayable, playable))!!
        assertEquals(listOf("x2"), candidates.map { it.providerTrackId })
        assertEquals(false, candidates.single().offers320)
    }

    @Test
    fun malformedSearchPayloadIsNull() {
        assertNull(JioSaavnPayloadParser.searchCandidates("<html></html>"))
        assertNull(JioSaavnPayloadParser.searchCandidates("{\"error\":{\"code\":\"INPUT_INVALID\"}}"))
    }

    @Test
    fun emptySearchPayloadIsEmpty() {
        assertTrue(JioSaavnPayloadParser.searchCandidates("{\"total\":0,\"start\":1,\"results\":[]}")!!.isEmpty())
    }

    @Test
    fun songDetailsDistinguishFoundMissingAndMalformed() {
        val found = JSONObject().put("songs", JSONArray().put(saavnSong("pW-kkdqr", "Blinding Lights", listOf("The Weeknd"), "Blinding Lights", 204)))
        assertTrue(JioSaavnPayloadParser.songDetails(found.toString(), "pW-kkdqr") is JioSaavnSongDetails.Found)
        assertEquals(JioSaavnSongDetails.Missing, JioSaavnPayloadParser.songDetails("{\"modules\":{\"reco\":{}}}", "zzzzzzzz"))
        assertEquals(JioSaavnSongDetails.Missing, JioSaavnPayloadParser.songDetails("[]", "zzzzzzzz"))
        assertEquals(JioSaavnSongDetails.Malformed, JioSaavnPayloadParser.songDetails("oops", "zzzzzzzz"))
    }

    @Test
    fun mediaAuthorizationOutcomes() {
        val granted = JioSaavnPayloadParser.mediaAuthorization("{\"auth_url\":\"https://web.saavncdn.com/820/a_320.mp4?Expires=1\",\"status\":\"success\"}")
        assertEquals("https://web.saavncdn.com/820/a_320.mp4?Expires=1", (granted as JioSaavnMediaAuthorization.Granted).url)
        assertEquals(JioSaavnMediaAuthorization.Denied, JioSaavnPayloadParser.mediaAuthorization("{\"auth_url\":false,\"status\":\"success\"}"))
        assertEquals(JioSaavnMediaAuthorization.Denied, JioSaavnPayloadParser.mediaAuthorization("{\"status\":\"failure\"}"))
        assertEquals(JioSaavnMediaAuthorization.Malformed, JioSaavnPayloadParser.mediaAuthorization("nope"))
    }

    @Test
    fun proOnlyAndRestrictedRightsCandidatesAreExplicitlyRejected() {
        val validSong = saavnSong("v1", "Free Song", listOf("Artist"), "Album", 180).apply {
            getJSONObject("more_info").put("rights", JSONObject().put("code", "0").put("cacheable", "true").put("delete_cached_object", "false"))
        }
        val proOnlySong = saavnSong("p1", "Pro Only Song", listOf("Artist"), "Album", 180).apply {
            getJSONObject("more_info").put("is_pro_only", "true")
        }
        val rightsUnavailableSong = saavnSong("r1", "Unavailable Rights Song", listOf("Artist"), "Album", 180).apply {
            getJSONObject("more_info").put("rights", JSONObject().put("code", "1").put("reason", "Unavailable").put("cacheable", "false"))
        }
        val paywalledSong = saavnSong("pw1", "Paywalled Song", listOf("Artist"), "Album", 180).apply {
            getJSONObject("more_info").put("paywalled", "true")
        }
        val candidates = JioSaavnPayloadParser.searchCandidates(searchBody(validSong, proOnlySong, rightsUnavailableSong, paywalledSong))!!
        assertEquals(listOf("v1"), candidates.map { it.providerTrackId })
    }
}

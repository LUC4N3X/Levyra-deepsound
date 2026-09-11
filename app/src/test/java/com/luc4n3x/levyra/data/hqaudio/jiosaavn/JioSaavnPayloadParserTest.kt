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
        assertTrue(candidate.offers320)
        assertEquals("enc-2IuQsex6", candidate.mediaToken)
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
}

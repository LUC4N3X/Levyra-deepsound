package com.luc4n3x.levyra.data.hqaudio.jiosaavn

import com.luc4n3x.levyra.data.hqaudio.saavnSong
import com.luc4n3x.levyra.data.hqaudio.searchBody
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JioSaavnApiDriftGuardTest {
    @Test
    fun searchResultMissingIdIsSkippedNotCrashed() {
        val song = saavnSong("k1", "Some Song", listOf("Artist"), "Album", 180).apply { remove("id") }
        assertTrue(JioSaavnPayloadParser.searchCandidates(searchBody(song))!!.isEmpty())
    }

    @Test
    fun searchResultMissingArtistMapFallsBackToCsvPrimaryArtists() {
        val song = saavnSong("k2", "Some Song", listOf("Ignored"), "Album", 180).apply {
            getJSONObject("more_info").apply {
                remove("artistMap")
                put("primary_artists", "Ariana Grande, The Weeknd")
            }
        }
        val candidate = JioSaavnPayloadParser.searchCandidates(searchBody(song))!!.single()
        assertEquals(listOf("Ariana Grande", "The Weeknd"), candidate.primaryArtists)
    }

    @Test
    fun bitrateFlagAcceptsNumericJsonValueNotOnlyStrings() {
        val song = saavnSong("k3", "Some Song", listOf("Artist"), "Album", 180).apply {
            getJSONObject("more_info").put("320kbps", 1)
        }
        val candidate = JioSaavnPayloadParser.searchCandidates(searchBody(song))!!.single()
        assertEquals(true, candidate.offers320)
    }

    @Test
    fun songDetailsAcceptsIdKeyedObjectEnvelopeNotOnlySongsArray() {
        val song = saavnSong("k4", "Some Song", listOf("Artist"), "Album", 180)
        val body = JSONObject().put("k4", song).toString()
        assertTrue(JioSaavnPayloadParser.songDetails(body, "k4") is JioSaavnSongDetails.Found)
    }

    @Test
    fun songDetailsWithUnrecognizedEnvelopeShapeIsMalformedNotMissing() {
        val body = JSONObject().put("someOtherId", saavnSong("k5", "Song", listOf("Artist"), "Album", 180)).toString()
        assertEquals(JioSaavnSongDetails.Malformed, JioSaavnPayloadParser.songDetails(body, "requested-id"))
    }

    @Test
    fun mediaAuthorizationRejectsNonStringAuthUrl() {
        assertEquals(
            JioSaavnMediaAuthorization.Denied,
            JioSaavnPayloadParser.mediaAuthorization("{\"status\":\"success\",\"auth_url\":12345}")
        )
    }

    @Test
    fun mediaAuthorizationStatusComparisonIsCaseInsensitive() {
        val granted = JioSaavnPayloadParser.mediaAuthorization(
            "{\"status\":\"SUCCESS\",\"auth_url\":\"https://web.saavncdn.com/820/a_320.mp4?Expires=1\"}"
        )
        assertTrue(granted is JioSaavnMediaAuthorization.Granted)
    }

    @Test
    fun mediaAuthorizationWrappedInUnexpectedEnvelopeIsDeniedNotMalformed() {
        val body = JSONObject()
            .put("data", JSONObject().put("status", "success").put("auth_url", "https://web.saavncdn.com/820/a_320.mp4"))
            .toString()
        assertEquals(JioSaavnMediaAuthorization.Denied, JioSaavnPayloadParser.mediaAuthorization(body))
    }

    @Test
    fun malformedTopLevelSearchEnvelopeIsNullNotACrash() {
        assertNull(JioSaavnPayloadParser.searchCandidates("{\"results\":\"not-an-array\"}"))
    }
}

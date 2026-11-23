package com.luc4n3x.levyra.data.spotify

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpotifyArtistAudienceTest {
    @Test
    fun picksFirstRelevantExactArtistMatch() {
        val search = JSONObject(
            """
            {"data":{"searchV2":{"artists":{"items":[
              {"data":{"uri":"spotify:artist:AAAAAAAAAAAAAAAAAAAAAA","profile":{"name":"Someone Else"}}},
              {"data":{"uri":"spotify:artist:6bMul6rmRS03x38tWKYifO","profile":{"name":"883"}}},
              {"data":{"uri":"spotify:artist:2h9G3rNWZkoW7WIC97YtzL","profile":{"name":"883"}}}
            ]}}}}
            """.trimIndent()
        )

        assertEquals("spotify:artist:6bMul6rmRS03x38tWKYifO", spotifyArtistUriFor(search, "883"))
    }

    @Test
    fun rejectsMissingMatchAndMalformedUri() {
        val search = JSONObject(
            """{"data":{"searchV2":{"artists":{"items":[{"data":{"uri":"spotify:track:x","profile":{"name":"883"}}}]}}}}"""
        )

        assertNull(spotifyArtistUriFor(search, "883"))
        assertNull(spotifyArtistUriFor(JSONObject("{}"), "883"))
    }

    @Test
    fun extractsEmbedTokenWithoutScanningWholePage() {
        val html = "<html>" + "x".repeat(50_000) +
            """{"session":{"accessToken":"abc.DEF-123","accessTokenExpirationTimestampMs":1795467660008,"isAnonymous":true}}"""

        val embedded = spotifyEmbedToken(html)

        assertEquals("abc.DEF-123", embedded?.token)
        assertEquals(1_795_467_660_008L, embedded?.expiresAtMs)
        assertNull(spotifyEmbedToken("<html>no token</html>"))
        assertNull(spotifyEmbedToken("""{"accessToken":""}"""))
    }

    @Test
    fun readsPositiveMonthlyListenersOnly() {
        assertEquals(
            2_850_060L,
            spotifyMonthlyListeners(JSONObject("""{"data":{"artistUnion":{"stats":{"monthlyListeners":2850060}}}}"""))
        )
        assertNull(spotifyMonthlyListeners(JSONObject("""{"data":{"artistUnion":{"stats":{"monthlyListeners":0}}}}""")))
        assertNull(spotifyMonthlyListeners(JSONObject("{}")))
    }
}

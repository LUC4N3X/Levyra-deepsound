package com.luc4n3x.levyra.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class YoutubeClientFailureAttributionTest {

    private fun playability(json: String): YoutubeClientFailureScope =
        YoutubeClientFailureAttribution.playabilityScope(JSONObject(json))

    @Test
    fun botCheckIsAttributedToTheClient() {
        assertEquals(
            YoutubeClientFailureScope.CLIENT,
            playability("""{"status":"LOGIN_REQUIRED","reason":"Sign in to confirm you’re not a bot"}""")
        )
    }

    @Test
    fun privateVideoWithGenericSignInReasonIsNotAttributable() {
        assertEquals(
            YoutubeClientFailureScope.NOT_ATTRIBUTABLE,
            playability(
                """{"status":"LOGIN_REQUIRED","reason":"Sign in if you've been granted access to this video","messages":["This video is private"]}"""
            )
        )
    }

    @Test
    fun ageRestrictionIsNotAttributable() {
        assertEquals(
            YoutubeClientFailureScope.NOT_ATTRIBUTABLE,
            playability("""{"status":"LOGIN_REQUIRED","reason":"Sign in to confirm your age"}""")
        )
        assertEquals(
            YoutubeClientFailureScope.NOT_ATTRIBUTABLE,
            playability("""{"status":"LOGIN_REQUIRED","reason":"This video may be inappropriate for some users."}""")
        )
    }

    @Test
    fun removedAndGeoRestrictedVideosAreNotAttributable() {
        assertEquals(
            YoutubeClientFailureScope.NOT_ATTRIBUTABLE,
            playability("""{"status":"UNPLAYABLE","reason":"The uploader has not made this video available in your country"}""")
        )
        assertEquals(
            YoutubeClientFailureScope.NOT_ATTRIBUTABLE,
            playability("""{"status":"UNPLAYABLE","reason":"This video is no longer available due to a copyright claim"}""")
        )
    }

    @Test
    fun ambiguousUnplayableStaysClientScoped() {
        assertEquals(
            YoutubeClientFailureScope.CLIENT,
            playability("""{"status":"UNPLAYABLE","reason":"This content isn't available, try again later."}""")
        )
        assertEquals(YoutubeClientFailureScope.CLIENT, playability("""{"status":"UNPLAYABLE"}"""))
        assertEquals(
            YoutubeClientFailureScope.CLIENT,
            playability("""{"status":"ERROR","reason":"This video is unavailable"}""")
        )
    }

    @Test
    fun structuredPlayabilityScopeWinsOverMessageHeuristics() {
        val error = YoutubePlayerRequestException(
            null,
            "Sign in",
            YoutubeClientFailureScope.NOT_ATTRIBUTABLE
        )

        assertEquals(YoutubeClientFailureScope.NOT_ATTRIBUTABLE, YoutubeClientFailureAttribution.scope(error))
    }

    @Test
    fun blockingHttpStatusesAreClientScopedAndServerErrorsAreTransient() {
        listOf(403, 410, 429).forEach { code ->
            assertEquals(
                YoutubeClientFailureScope.CLIENT,
                YoutubeClientFailureAttribution.scope(YoutubePlayerRequestException(code, "HTTP $code"))
            )
        }
        listOf(408, 500, 503).forEach { code ->
            assertEquals(
                YoutubeClientFailureScope.TRANSIENT,
                YoutubeClientFailureAttribution.scope(YoutubePlayerRequestException(code, "HTTP $code"))
            )
        }
    }

    @Test
    fun transportFailuresAreTransient() {
        assertEquals(
            YoutubeClientFailureScope.TRANSIENT,
            YoutubeClientFailureAttribution.scope(SocketTimeoutException("timeout"))
        )
        assertEquals(
            YoutubeClientFailureScope.TRANSIENT,
            YoutubeClientFailureAttribution.scope(IOException("Connection reset by peer"))
        )
    }

    @Test
    fun localPoTokenRuntimeAndPolicyFailuresAreNotAttributable() {
        assertEquals(
            YoutubeClientFailureScope.NOT_ATTRIBUTABLE,
            YoutubeClientFailureAttribution.scope(
                IllegalStateException("wrapped", YoutubePoTokenRuntimeUnavailableException("WebView unavailable"))
            )
        )
        assertEquals(
            YoutubeClientFailureScope.NOT_ATTRIBUTABLE,
            YoutubeClientFailureAttribution.scope(
                YoutubePlayerRequestException(null, "Client IOS non abilitato alla capability streaming")
            )
        )
    }

    @Test
    fun missingDirectUrlsAndDecipherFailuresStayClientScoped() {
        assertEquals(
            YoutubeClientFailureScope.CLIENT,
            YoutubeClientFailureAttribution.scope(YoutubePlayerRequestException(null, "URL streaming assente"))
        )
        assertEquals(
            YoutubeClientFailureScope.CLIENT,
            YoutubeClientFailureAttribution.scope(IllegalStateException("Invalid local n-transform result"))
        )
    }

    @Test
    fun botCheckStillRotatesTheGuestSession() {
        assertEquals(
            true,
            YoutubePlaybackSecurity.shouldRotateGuestSession("sign in to confirm you're not a bot", null)
        )
    }
}

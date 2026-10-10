package com.luc4n3x.levyra.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GooglevideoMediaNetworkTest {
    @Test
    fun rewritesOnlyTheMediaNetworkLabelForEachDeclaredAlternative() {
        val url = "https://rr5---sn-hpa7knld.googlevideo.com/videoplayback" +
            "?expire=1&itag=140&mn=sn-hpa7knld,sn-hpa7zns6&sig=abc"

        val alternates = GooglevideoMediaNetwork.alternateUrls(url)

        assertEquals(
            listOf(
                "https://rr5---sn-hpa7zns6.googlevideo.com/videoplayback" +
                    "?expire=1&itag=140&mn=sn-hpa7knld,sn-hpa7zns6&sig=abc"
            ),
            alternates
        )
    }

    @Test
    fun preservesPathQueryAndFragmentWhileBoundingCandidates() {
        val url = "https://rr2---sn-a.googlevideo.com/videoplayback/id/x?mn=sn-a,sn-b,sn-c,sn-d,sn-e#f"

        val alternates = GooglevideoMediaNetwork.alternateUrls(url, limit = 2)

        assertEquals(
            listOf(
                "https://rr2---sn-b.googlevideo.com/videoplayback/id/x?mn=sn-a,sn-b,sn-c,sn-d,sn-e#f",
                "https://rr2---sn-c.googlevideo.com/videoplayback/id/x?mn=sn-a,sn-b,sn-c,sn-d,sn-e#f"
            ),
            alternates
        )
    }

    @Test
    fun deduplicatesAndSkipsTheCurrentMediaNetwork() {
        val url = "https://rr1---sn-a.googlevideo.com/videoplayback?mn=sn-a,sn-b,sn-b,SN-A"

        assertEquals(
            listOf("https://rr1---sn-b.googlevideo.com/videoplayback?mn=sn-a,sn-b,sn-b,SN-A"),
            GooglevideoMediaNetwork.alternateUrls(url)
        )
    }

    @Test
    fun rejectsMalformedOrHostileCandidates() {
        val cases = listOf(
            "https://rr1---sn-a.googlevideo.com/videoplayback?mn=sn-a",
            "https://rr1---sn-a.googlevideo.com/videoplayback?mn=",
            "https://rr1---sn-a.googlevideo.com/videoplayback",
            "https://rr1---sn-a.googlevideo.com/videoplayback?mn=sn-a,evil.example.com",
            "https://rr1---sn-a.googlevideo.com/videoplayback?mn=sn-a,sn-b/../x",
            "https://rr1---sn-a.googlevideo.com/videoplayback?mn=sn-a,sn b",
            "https://rr1---sn-a.googlevideo.com.evil.test/videoplayback?mn=sn-a,sn-b",
            "https://user:pass@rr1---sn-a.googlevideo.com/videoplayback?mn=sn-a,sn-b",
            "https://rr1---sn-a.googlevideo.com:8443/videoplayback?mn=sn-a,sn-b",
            "http://rr1---sn-a.googlevideo.com/videoplayback?mn=sn-a,sn-b",
            "https://googlevideo.com/videoplayback?mn=sn-a,sn-b",
            "https://rr1.googlevideo.com/videoplayback?mn=sn-a,sn-b",
            "https://---sn-a.googlevideo.com/videoplayback?mn=sn-a,sn-b",
            "not a url"
        )

        cases.forEach { url ->
            assertTrue(url, GooglevideoMediaNetwork.alternateUrls(url).isEmpty())
        }
    }

    @Test
    fun ignoresQueryParametersThatOnlyLookLikeTheMediaNetworkList() {
        val url = "https://rr1---sn-a.googlevideo.com/videoplayback?mne=sn-a,sn-b&xmn=sn-c"

        assertTrue(GooglevideoMediaNetwork.alternateUrls(url).isEmpty())
    }

    @Test
    fun recognisesGooglevideoHostsOnly() {
        assertTrue(GooglevideoMediaNetwork.isGooglevideoHost("rr1---sn-a.googlevideo.com"))
        assertTrue(GooglevideoMediaNetwork.isGooglevideoHost("RR1---SN-A.GOOGLEVIDEO.COM."))
        assertFalse(GooglevideoMediaNetwork.isGooglevideoHost("googlevideo.com"))
        assertFalse(GooglevideoMediaNetwork.isGooglevideoHost("evil-googlevideo.com"))
        assertFalse(GooglevideoMediaNetwork.isGooglevideoHost("googlevideo.com.evil.test"))
    }

    @Test
    fun neverReturnsMoreThanTheBoundedCandidateCount() {
        val networks = (0 until 12).joinToString(",") { "sn-$it" }
        val url = "https://rr1---sn-a.googlevideo.com/videoplayback?mn=$networks"

        assertEquals(
            GooglevideoMediaNetwork.MAX_ALTERNATE_CANDIDATES,
            GooglevideoMediaNetwork.alternateUrls(url).size
        )
    }

    @Test
    fun failsOverOnlyForEndpointLevelFailures() {
        listOf(null, 404, 500, 502, 503, 504).forEach { status ->
            assertTrue("$status", GooglevideoMediaNetwork.isEndpointFailure(status))
        }
        listOf(200, 206, 301, 401, 403, 410, 416, 429).forEach { status ->
            assertFalse("$status", GooglevideoMediaNetwork.isEndpointFailure(status))
        }
    }
}

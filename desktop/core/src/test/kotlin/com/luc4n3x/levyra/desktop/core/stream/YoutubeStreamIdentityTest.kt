package com.luc4n3x.levyra.desktop.core.stream

import org.junit.Assert.*
import org.junit.Test
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper

class YoutubeStreamIdentityTest {
    @Test fun vrIdentityIsPreservedAtFirstQueryParameterAndLaterParameters() {
        for (query in listOf("c=ANDROID_VR&itag=251", "itag=251&c=ANDROID_VR")) {
            val identity = YoutubeStreamIdentity.forUrl("https://r1.googlevideo.com/videoplayback?$query")!!
            assertEquals(YoutubeParsingHelper.getAndroidVRUserAgent(null), identity.userAgent)
            assertNotEquals(YoutubeParsingHelper.getAndroidUserAgent(null), identity.userAgent)
        }
    }

    @Test fun lookalikeDomainsCannotReceiveYoutubeHeaders() {
        assertNull(YoutubeStreamIdentity.forUrl("https://evilgooglevideo.com/videoplayback?c=ANDROID_VR"))
        assertNull(YoutubeStreamIdentity.forUrl("https://googlevideo.com.evil.test/videoplayback?c=ANDROID_VR"))
    }
}

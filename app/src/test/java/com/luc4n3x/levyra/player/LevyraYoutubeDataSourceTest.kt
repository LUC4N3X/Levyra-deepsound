package com.luc4n3x.levyra.player

import org.junit.Assert.assertEquals
import org.junit.Test

class LevyraYoutubeDataSourceTest {
    @Test
    fun signedGoogleVideoParametersRemainUntouched() {
        val url = googleVideoPlaybackUrl(
            url = "https://rr1.googlevideo.com/videoplayback?clen=2400000&rn=4&range=0-524287&sig=abc",
            requestNumber = 19L
        )

        assertEquals(
            "https://rr1.googlevideo.com/videoplayback?clen=2400000&rn=4&range=0-524287&sig=abc",
            url
        )
    }

    @Test
    fun requestNumberIsOnlyAddedWhenMissing() {
        val url = googleVideoPlaybackUrl(
            url = "https://rr1.googlevideo.com/videoplayback?mime=audio%2Fwebm&range=262144-393215&sig=abc#media",
            requestNumber = 2L
        )

        assertEquals(
            "https://rr1.googlevideo.com/videoplayback?mime=audio%2Fwebm&range=262144-393215&sig=abc&rn=2#media",
            url
        )
    }
}

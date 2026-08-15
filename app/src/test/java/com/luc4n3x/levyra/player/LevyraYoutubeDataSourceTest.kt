package com.luc4n3x.levyra.player

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraYoutubeDataSourceTest {
    @Test
    fun continuationUsesGoogleVideoQueryRangeAndFreshRequestNumber() {
        val request = googleVideoPlaybackRequest(
            url = "https://rr1.googlevideo.com/videoplayback?clen=2400000&rn=4&range=0-524287&sig=abc",
            position = 524_288L,
            requestedLength = C.LENGTH_UNSET.toLong(),
            requestNumber = 19L
        )

        assertEquals(1_875_712L, request.rangeLength)
        assertTrue(request.url.contains("range=524288-2399999"))
        assertTrue(request.url.contains("rn=19"))
        assertFalse(request.url.contains("rn=4"))
        assertFalse(request.url.contains("range=0-524287"))
        assertTrue(request.url.contains("sig=abc"))
    }

    @Test
    fun boundedReadUsesRequestedLengthWithoutInventingAnEnd() {
        val request = googleVideoPlaybackRequest(
            url = "https://rr1.googlevideo.com/videoplayback?mime=audio%2Fwebm",
            position = 262_144L,
            requestedLength = 131_072L,
            requestNumber = 2L
        )

        assertEquals(131_072L, request.rangeLength)
        assertTrue(request.url.contains("range=262144-393215"))
        assertTrue(request.url.contains("rn=2"))
    }

    @Test
    fun unknownLengthKeepsHeaderRangeSemanticsButRefreshesRequestNumber() {
        val request = googleVideoPlaybackRequest(
            url = "https://rr1.googlevideo.com/videoplayback?mime=audio%2Fwebm&rn=1",
            position = 262_144L,
            requestedLength = C.LENGTH_UNSET.toLong(),
            requestNumber = 7L
        )

        assertEquals(C.LENGTH_UNSET.toLong(), request.rangeLength)
        assertFalse(request.url.contains("range="))
        assertTrue(request.url.contains("rn=7"))
    }
}

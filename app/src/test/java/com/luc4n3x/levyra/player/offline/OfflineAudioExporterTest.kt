package com.luc4n3x.levyra.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineAudioExporterTest {
    @Test
    fun longKnownDownloadsAreSplitIntoBoundedRanges() {
        val oneMb = 1024L * 1024L

        val ranges = planParallelAudioRanges(
            contentLength = 10L * oneMb,
            chunkSize = 4L * oneMb,
            minLength = 8L * oneMb
        )

        assertEquals(
            listOf(
                AudioDownloadRange(start = 0L, endInclusive = 4L * oneMb - 1L),
                AudioDownloadRange(start = 4L * oneMb, endInclusive = 8L * oneMb - 1L),
                AudioDownloadRange(start = 8L * oneMb, endInclusive = 10L * oneMb - 1L)
            ),
            ranges
        )
    }

    @Test
    fun smallOrUnknownDownloadsStaySerial() {
        val oneMb = 1024L * 1024L

        assertTrue(
            planParallelAudioRanges(
                contentLength = 6L * oneMb,
                chunkSize = 4L * oneMb,
                minLength = 8L * oneMb
            ).isEmpty()
        )
        assertTrue(planParallelAudioRanges(contentLength = -1L).isEmpty())
    }

    @Test
    fun rangeQueryResponsesCanBeSuccessfulPartialBodies() {
        val range = AudioDownloadRange(start = 1024L, endInclusive = 2047L)

        assertTrue(
            isUsableAudioRangeResponse(
                code = 200,
                bodyLength = 1024L,
                contentRange = "",
                range = range,
                rangeParamApplied = true
            )
        )
    }

    @Test
    fun fullBodyResponsesAreRejectedForRangeChunks() {
        val range = AudioDownloadRange(start = 1024L, endInclusive = 2047L)

        assertFalse(
            isUsableAudioRangeResponse(
                code = 200,
                bodyLength = 4096L,
                contentRange = "",
                range = range,
                rangeParamApplied = true
            )
        )
        assertFalse(
            isUsableAudioRangeResponse(
                code = 200,
                bodyLength = 1024L,
                contentRange = "",
                range = range,
                rangeParamApplied = false
            )
        )
    }

    @Test
    fun contentLengthCanBeReadFromGoogleVideoClenParameter() {
        val url = "https://rr1---sn.googlevideo.com/videoplayback?mime=audio%2Fmp4&clen=73400320&expire=9999999999"

        assertEquals(73400320L, audioContentLengthFromUrl(url))
        assertEquals(-1L, audioContentLengthFromUrl("https://example.com/audio.m4a"))
    }
}

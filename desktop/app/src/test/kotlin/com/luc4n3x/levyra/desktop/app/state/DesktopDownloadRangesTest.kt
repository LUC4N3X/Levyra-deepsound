package com.luc4n3x.levyra.desktop.app.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopDownloadRangesTest {
    @Test
    fun rangesCoverTheWholeFileWithoutGaps() {
        val oneMb = 1024L * 1024L
        val ranges = planDesktopDownloadRanges(
            contentLength = 5L * oneMb,
            chunkSize = oneMb
        )

        assertEquals(5, ranges.size)
        assertEquals(0L, ranges.first().start)
        assertEquals(5L * oneMb - 1L, ranges.last().endInclusive)
        ranges.zipWithNext().forEach { (current, next) ->
            assertEquals(current.endInclusive + 1L, next.start)
        }
        assertEquals(5L * oneMb, ranges.sumOf { it.length })
    }

    @Test
    fun smallFilesStayOnTheSerialFallback() {
        val ranges = planDesktopDownloadRanges(contentLength = 1024L * 1024L)

        assertTrue(ranges.isEmpty())
    }

    @Test
    fun googleVideoRangeResponseAcceptsExactBodyWithStatus200() {
        val range = DesktopDownloadRange(1_000L, 1_999L)

        assertTrue(
            isUsableDesktopRangeResponse(
                code = 200,
                bodyLength = 1_000L,
                contentRange = "",
                range = range,
                rangeParamApplied = true
            )
        )
        assertFalse(
            isUsableDesktopRangeResponse(
                code = 200,
                bodyLength = 1_000L,
                contentRange = "",
                range = range,
                rangeParamApplied = false
            )
        )
    }

    @Test
    fun standardRangeResponseMustMatchRequestedBounds() {
        val range = DesktopDownloadRange(10L, 19L)

        assertTrue(
            isUsableDesktopRangeResponse(
                code = 206,
                bodyLength = 10L,
                contentRange = "bytes 10-19/100",
                range = range,
                rangeParamApplied = false
            )
        )
        assertFalse(
            isUsableDesktopRangeResponse(
                code = 206,
                bodyLength = 10L,
                contentRange = "bytes 0-9/100",
                range = range,
                rangeParamApplied = false
            )
        )
    }

    @Test
    fun rangeUrlReplacesOldRangeAndPreservesOtherParameters() {
        val source = "https://r1.googlevideo.com/videoplayback?itag=140&range=0-99&clen=5000"
        val ranged = desktopRangeUrl(source, DesktopDownloadRange(100L, 199L))

        assertTrue(ranged.contains("itag=140"))
        assertTrue(ranged.contains("clen=5000"))
        assertTrue(ranged.endsWith("range=100-199"))
        assertEquals(5_000L, desktopAudioContentLength(ranged))
    }
}

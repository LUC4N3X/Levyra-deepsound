package com.luc4n3x.levyra.player.sabr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SabrSegmentAssemblerTest {
    @Test
    fun deliversTheInitSegmentAndThenTheFirstMediaSegment() {
        val assembler = SabrSegmentAssembler(targetItag = 140)
        assembler.onMediaHeader(header(id = 0, itag = 140, start = 0L))
        assembler.onMediaHeader(header(id = 1, itag = 140, start = 1_019L))

        assertEquals(SabrMediaWindow(1, 1_019), assembler.onMedia(0, 1_019, position = 0L))
        assertEquals(SabrMediaWindow(1, 162_083), assembler.onMedia(1, 162_083, position = 1_019L))
    }

    @Test
    fun trimsAChunkThatStartsBeforeTheRequestedPosition() {
        val assembler = SabrSegmentAssembler(targetItag = 140)
        assembler.onMediaHeader(header(id = 0, itag = 140, start = 970_940L))

        val window = assembler.onMedia(0, 161_619, position = 1_000_000L)

        assertEquals(SabrMediaWindow(1 + 29_060, 161_619 - 29_060), window)
    }

    @Test
    fun skipsChunksAlreadyBehindThePlayerPosition() {
        val assembler = SabrSegmentAssembler(targetItag = 140)
        assembler.onMediaHeader(header(id = 0, itag = 140, start = 0L))

        assertNull(assembler.onMedia(0, 1_019, position = 5_000L))
    }

    @Test
    fun ignoresChunksBelongingToAnotherFormat() {
        val assembler = SabrSegmentAssembler(targetItag = 133)
        assembler.onMediaHeader(header(id = 0, itag = 140, start = 0L))
        assembler.onMediaHeader(header(id = 1, itag = 133, start = 0L))

        assertNull(assembler.onMedia(0, 1_019, position = 0L))
        assertEquals(SabrMediaWindow(1, 1_228), assembler.onMedia(1, 1_228, position = 0L))
    }

    @Test
    fun consecutivePartsOfOneSegmentStayContiguous() {
        val assembler = SabrSegmentAssembler(targetItag = 140)
        assembler.onMediaHeader(header(id = 0, itag = 140, start = 0L))

        assertEquals(SabrMediaWindow(1, 32_768), assembler.onMedia(0, 32_768, position = 0L))
        assertEquals(SabrMediaWindow(1, 32_768), assembler.onMedia(0, 32_768, position = 32_768L))
        assertEquals(SabrMediaWindow(101, 32_668), assembler.onMedia(0, 32_768, position = 65_636L))
    }

    @Test
    fun mediaWithoutAKnownHeaderIsIgnored() {
        val assembler = SabrSegmentAssembler(targetItag = 140)

        assertNull(assembler.onMedia(7, 1_000, position = 0L))
    }

    @Test
    fun emptyOrNegativePayloadsAreIgnored() {
        val assembler = SabrSegmentAssembler(targetItag = 140)
        assembler.onMediaHeader(header(id = 0, itag = 140, start = 0L))

        assertNull(assembler.onMedia(0, 0, position = 0L))
        assertNull(assembler.onMedia(0, -1, position = 0L))
    }

    @Test
    fun resetClearsHeadersAndCountersBetweenRequests() {
        val assembler = SabrSegmentAssembler(targetItag = 140)
        assembler.onMediaHeader(header(id = 0, itag = 140, start = 0L))
        assembler.onMedia(0, 1_019, position = 0L)

        assembler.reset()
        assertNull(assembler.onMedia(0, 1_019, position = 0L))
    }

    @Test
    fun headerTrackingStaysBounded() {
        val assembler = SabrSegmentAssembler(targetItag = 140, maxTrackedHeaders = 4)
        repeat(64) { index -> assembler.onMediaHeader(header(id = index, itag = 140, start = 0L)) }

        assertEquals(SabrMediaWindow(1, 10), assembler.onMedia(63, 10, position = 0L))
    }

    @Test
    fun playerTimeTracksThePositionAndWalksBackWhenARequestOvershoots() {
        val contentLength = 9_397_248L
        val durationMs = 213_090L

        assertEquals(0L, sabrPlayerTimeMsFor(contentLength, durationMs, position = 0L))
        assertEquals(
            durationMs / 2L,
            sabrPlayerTimeMsFor(contentLength, durationMs, position = contentLength / 2L)
        )
        assertEquals(
            durationMs - 1L,
            sabrPlayerTimeMsFor(contentLength, durationMs, position = contentLength * 2L)
        )
        assertEquals(
            0L,
            sabrPlayerTimeMsFor(contentLength, durationMs, position = 1_000L, unproductiveAttempts = 3)
        )
        assertEquals(0L, sabrPlayerTimeMsFor(0L, durationMs, position = 10L))
        assertEquals(0L, sabrPlayerTimeMsFor(contentLength, 0L, position = 10L))
    }

    private fun header(id: Int, itag: Int, start: Long) = SabrMediaHeader(
        headerId = id,
        itag = itag,
        lastModified = 1L,
        startDataRange = start,
        contentLength = 0L,
        sequenceNumber = 0L,
        durationMs = 0L,
        isInitSegment = false
    )
}

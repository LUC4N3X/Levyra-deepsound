package com.luc4n3x.levyra.player.visualizer

import androidx.media3.common.C
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PcmHistoryTest {
    private fun stereoFloat(frames: Int, start: Int): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(frames * 8).order(ByteOrder.LITTLE_ENDIAN)
        repeat(frames) { index ->
            val value = (start + index).toFloat()
            buffer.putFloat(value)
            buffer.putFloat(value + 2f)
        }
        buffer.flip()
        return buffer
    }

    private fun history(capacityLog2: Int = 18) = PcmHistory(capacityLog2).apply {
        setEnabled(true)
        configure(48_000)
    }

    @Test
    fun mapsMediaPositionToDownmixedFrames() {
        val history = history()
        history.beginSegment("A", 0L)
        val input = stereoFloat(48_000, 0)
        history.append(input, C.ENCODING_PCM_FLOAT, 2)
        assertEquals(0, input.position())
        val location = assertNotNull(history.locate("A", 500_000L, null, 0L))
        assertEquals(24_000L, location.endFrame)
        val out = FloatArray(4)
        assertTrue(history.read(location.endFrame, out))
        assertEquals(23_996f + 1f, out[0], 0f)
        assertEquals(23_999f + 1f, out[3], 0f)
    }

    @Test
    fun flushOffsetAnchorsSeeks() {
        val history = history()
        history.beginSegment("A", 30_000_000L)
        history.append(stereoFloat(9_600, 0), C.ENCODING_PCM_FLOAT, 2)
        assertEquals(4_800L, history.locate("A", 30_100_000L, null, 0L)?.endFrame)
        assertNull(history.locate("A", 29_000_000L, null, 0L))
        assertNull(history.locate("A", 31_000_000L, null, 0L))
    }

    @Test
    fun gaplessContinuationUsesChainOffset() {
        val history = history()
        history.beginSegment("A", 0L)
        history.append(stereoFloat(38_400, 0), C.ENCODING_PCM_FLOAT, 2)
        assertNull(history.locate("B", 100_000L, null, 0L))
        assertEquals(28_800L, history.locate("B", 100_000L, "A", 500_000L)?.endFrame)
    }

    @Test
    fun newSegmentForNextItemWinsOverChain() {
        val history = history()
        history.beginSegment("A", 0L)
        history.append(stereoFloat(4_800, 0), C.ENCODING_PCM_FLOAT, 2)
        history.beginSegment("B", 0L)
        history.append(stereoFloat(4_800, 0), C.ENCODING_PCM_FLOAT, 2)
        assertEquals(4_800L + 2_400L, history.locate("B", 50_000L, "A", 100_000L)?.endFrame)
        assertEquals(2_400L, history.locate("A", 50_000L, null, 0L)?.endFrame)
        assertNull(history.locate("A", 150_000L, null, 0L))
    }

    @Test
    fun evictedRepeatFallsBackToChain() {
        val history = history(capacityLog2 = 12)
        history.beginSegment("A", 0L)
        history.append(stereoFloat(10_000, 0), C.ENCODING_PCM_FLOAT, 2)
        val repeatDurationUs = 8_000L * 1_000_000L / 48_000L
        assertNull(history.locate("A", 20_833L, null, 0L))
        assertEquals(9_000L, history.locate("A", 20_833L, "A", repeatDurationUs)?.endFrame)
    }

    @Test
    fun highRatesAreDecimatedToAtMost48k() {
        val history = PcmHistory().apply {
            setEnabled(true)
            configure(96_000)
        }
        history.beginSegment("A", 0L)
        history.append(stereoFloat(9_600, 0), C.ENCODING_PCM_FLOAT, 2)
        assertEquals(4_800L, history.writeFrame)
        val location = assertNotNull(history.locate("A", 50_000L, null, 0L))
        assertEquals(48_000.0, location.sampleRate, 0.0)
        assertEquals(2_400L, location.endFrame)
    }

    @Test
    fun disabledHistoryTracksFramesWithoutStorage() {
        val history = PcmHistory()
        history.configure(48_000)
        history.beginSegment("A", 0L)
        history.append(stereoFloat(4_800, 0), C.ENCODING_PCM_FLOAT, 2)
        assertEquals(4_800L, history.writeFrame)
        assertFalse(history.enabled)
        assertFalse(history.read(4_800L, FloatArray(16)))
        history.setEnabled(true)
        history.append(stereoFloat(4_800, 0), C.ENCODING_PCM_FLOAT, 2)
        assertNull(history.locate("A", 50_000L, null, 0L))
        assertNotNull(history.locate("A", 150_000L, null, 0L))
        assertFalse(history.read(4_800L, FloatArray(16)))
        assertTrue(history.read(9_600L, FloatArray(16)))
    }

    @Test
    fun pcm16IsScaledToUnitRange() {
        val history = history()
        history.beginSegment("A", 0L)
        val buffer = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(16_384).putShort(-16_384).flip()
        history.append(buffer, C.ENCODING_PCM_16BIT, 1)
        val out = FloatArray(2)
        assertTrue(history.read(2L, out))
        assertEquals(0.5f, out[0], 0f)
        assertEquals(-0.5f, out[1], 0f)
    }

    @Test
    fun repeatedChainKeepsAddingPlayedLengths() {
        val history = history(capacityLog2 = 14)
        history.beginSegment("A", 0L)
        history.append(stereoFloat(60_000, 0), C.ENCODING_PCM_FLOAT, 2)
        val chain = SpectrumChain(history)
        val durationUs = 500_000L
        chain.advance("A", durationUs)
        chain.advance("A", durationUs)
        assertEquals(1_000_000L, chain.offsetUs)
        assertEquals(
            48_000L + 4_800L,
            history.locate("A", 100_000L, chain.root?.periodUid, chain.offsetUs)?.endFrame
        )
    }

    @Test
    fun staleSegmentOfAnEarlierPlayDoesNotRestartTheChain() {
        val history = history()
        history.beginSegment("A", 0L)
        history.append(stereoFloat(4_800, 0), C.ENCODING_PCM_FLOAT, 2)
        history.beginSegment("B", 30_000_000L)
        history.append(stereoFloat(9_600, 0), C.ENCODING_PCM_FLOAT, 2)
        val chain = SpectrumChain(history)
        chain.advance("B", 30_100_000L)
        chain.advance("A", 50_000L)
        assertEquals("B", chain.root?.periodUid)
        assertEquals(30_150_000L, chain.offsetUs)
        assertEquals(
            4_800L + 7_200L,
            history.locate("B", 0L, chain.root?.periodUid, chain.offsetUs)?.endFrame
        )
    }

    @Test
    fun newerSegmentBecomesTheChainRoot() {
        val history = history()
        history.beginSegment("A", 0L)
        history.append(stereoFloat(4_800, 0), C.ENCODING_PCM_FLOAT, 2)
        val chain = SpectrumChain(history)
        chain.advance("A", 100_000L)
        history.beginSegment("A", 20_000_000L)
        history.append(stereoFloat(4_800, 0), C.ENCODING_PCM_FLOAT, 2)
        chain.advance("A", 20_100_000L)
        assertEquals(4_800L, chain.root?.startFrame)
        assertEquals(20_100_000L, chain.offsetUs)
        chain.reset()
        assertNull(chain.root)
        assertEquals(0L, chain.offsetUs)
    }

    private fun PcmHistory.locate(
        periodUid: Any?,
        positionUs: Long,
        chainRootUid: Any?,
        chainOffsetUs: Long
    ): PcmHistory.Location? = PcmHistory.Location().takeIf { locate(periodUid, positionUs, chainRootUid, chainOffsetUs, it) }

    private fun <T> assertNotNull(value: T?): T {
        org.junit.Assert.assertNotNull(value)
        return value!!
    }
}

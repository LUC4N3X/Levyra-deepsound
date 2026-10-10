package com.luc4n3x.levyra.player.sabr

import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.InputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UmpReaderTest {
    @Test
    fun decodesTheFramingObservedOnLiveSabrResponses() {
        // 0x33 0xa0 0x07 is the two-varint header of a 480 byte part observed on a real response.
        val payload = ByteArray(480) { (it % 251).toByte() }
        val response = byteArrayOf(0x33, 0xa0.toByte(), 0x07) + payload
        val reader = UmpReader(ByteArrayInputStream(response))

        assertTrue(reader.next())
        assertEquals(51, reader.partType)
        assertEquals(480, reader.partLength)
        assertArrayEquals(payload, reader.partPayload.copyOf(reader.partLength))
        assertFalse(reader.next())
    }

    @Test
    fun varintRoundTripsAcrossEverySizeClass() {
        val values = listOf(0, 1, 127, 128, 480, 16_383, 16_384, 2_097_151, 2_097_152, 268_435_455, 268_435_456, Int.MAX_VALUE)
        values.forEach { value ->
            val encoded = umpPart(value, ByteArray(0))
            val reader = UmpReader(ByteArrayInputStream(encoded))
            assertTrue("$value", reader.next())
            assertEquals("$value", value, reader.partType)
            assertEquals("$value", 0, reader.partLength)
        }
    }

    @Test
    fun readsSeveralPartsAndReusesOneBuffer() {
        val first = ByteArray(1_000) { 1 }
        val second = ByteArray(32_769) { 2 }
        val response = umpPart(SabrPart.MEDIA_HEADER, first) + umpPart(SabrPart.MEDIA, second)
        val reader = UmpReader(ByteArrayInputStream(response))

        assertTrue(reader.next())
        assertEquals(SabrPart.MEDIA_HEADER, reader.partType)
        assertEquals(1_000, reader.partLength)
        val firstBuffer = reader.partPayload

        assertTrue(reader.next())
        assertEquals(SabrPart.MEDIA, reader.partType)
        assertEquals(32_769, reader.partLength)
        assertEquals(2, reader.partPayload[0].toInt())
        assertFalse(reader.next())
        assertTrue(reader.partPayload.size >= firstBuffer.size)
    }

    @Test
    fun reassemblesPartsSplitAcrossNetworkReads() {
        val payload = ByteArray(5_000) { (it % 97).toByte() }
        val response = umpPart(SabrPart.MEDIA, payload)
        val reader = UmpReader(DripInputStream(response, chunk = 7))

        assertTrue(reader.next())
        assertEquals(SabrPart.MEDIA, reader.partType)
        assertEquals(5_000, reader.partLength)
        assertArrayEquals(payload, reader.partPayload.copyOf(reader.partLength))
    }

    @Test
    fun unknownPartsAreReturnedAndCanBeSkippedByTheCaller() {
        val response = umpPart(9_999, ByteArray(4)) + umpPart(SabrPart.MEDIA_END, byteArrayOf(0))
        val reader = UmpReader(ByteArrayInputStream(response))

        assertTrue(reader.next())
        assertEquals(9_999, reader.partType)
        assertTrue(reader.next())
        assertEquals(SabrPart.MEDIA_END, reader.partType)
        assertFalse(reader.next())
    }

    @Test(expected = SabrProtocolException::class)
    fun oversizedPartsAreRejectedBeforeAllocating() {
        val response = umpPart(SabrPart.MEDIA, ByteArray(0))
        val oversized = umpVarint(SabrPart.MEDIA) + umpVarint(2_000_000) + ByteArray(16)
        assertTrue(response.isNotEmpty())
        UmpReader(ByteArrayInputStream(oversized), maxPartSize = 1_024).next()
    }

    @Test(expected = EOFException::class)
    fun truncatedPayloadsFailInsteadOfReturningPartialMedia() {
        val truncated = umpVarint(SabrPart.MEDIA) + umpVarint(64) + ByteArray(10)
        UmpReader(ByteArrayInputStream(truncated)).next()
    }

    @Test(expected = EOFException::class)
    fun truncatedHeadersFail() {
        UmpReader(ByteArrayInputStream(byteArrayOf(0xa0.toByte()))).next()
    }

    @Test
    fun emptyResponseEndsCleanly() {
        val reader = UmpReader(ByteArrayInputStream(ByteArray(0)))
        assertFalse(reader.next())
        assertEquals(-1, reader.partType)
    }

    private class DripInputStream(private val data: ByteArray, private val chunk: Int) : InputStream() {
        private var position = 0

        override fun read(): Int = if (position >= data.size) -1 else data[position++].toInt() and 0xFF

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (position >= data.size) return -1
            val count = minOf(chunk, length, data.size - position)
            System.arraycopy(data, position, buffer, offset, count)
            position += count
            return count
        }
    }
}

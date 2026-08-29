package com.luc4n3x.levyra.player.sabr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrProtocolTest {
    @Test
    fun mediaHeaderCarriesTheAbsoluteByteOffsetOfTheChunk() {
        val encoded = ProtoWriter()
            .varint(1, 1L)
            .varint(3, 140L)
            .varint(4, 1_766_955_925_572_207L)
            .varint(6, 1_019L)
            .varint(9, 1L)
            .varint(10, 16_233L)
            .varint(14, 162_083L)
            .toByteArray()

        val header = SabrMessages.parseMediaHeader(encoded, encoded.size)

        assertEquals(1, header.headerId)
        assertEquals(140, header.itag)
        assertEquals(1_766_955_925_572_207L, header.lastModified)
        assertEquals(1_019L, header.startDataRange)
        assertEquals(162_083L, header.contentLength)
        assertEquals(1L, header.sequenceNumber)
        assertEquals(16_233L, header.durationMs)
        assertTrue(!header.isInitSegment)
    }

    @Test
    fun initSegmentHeadersAreRecognised() {
        val encoded = ProtoWriter().varint(1, 0L).varint(3, 140L).varint(8, 1L).varint(14, 1_019L).toByteArray()

        val header = SabrMessages.parseMediaHeader(encoded, encoded.size)

        assertTrue(header.isInitSegment)
        assertEquals(0L, header.startDataRange)
    }

    @Test
    fun unknownHeaderFieldsAreSkippedInsteadOfFailing() {
        val encoded = ProtoWriter()
            .varint(3, 140L)
            .string(13, "unexpected")
            .varint(14, 42L)
            .toByteArray()

        val header = SabrMessages.parseMediaHeader(encoded, encoded.size)

        assertEquals(140, header.itag)
        assertEquals(42L, header.contentLength)
    }

    @Test
    fun redirectUrlIsReadFromTheFirstField() {
        val encoded = ProtoWriter()
            .string(1, "https://rr4---sn-b.googlevideo.com/videoplayback?mn=sn-a,sn-b")
            .varint(13, 116L)
            .toByteArray()

        assertEquals(
            "https://rr4---sn-b.googlevideo.com/videoplayback?mn=sn-a,sn-b",
            SabrMessages.parseRedirectUrl(encoded, encoded.size)
        )
    }

    @Test
    fun redirectWithoutAUrlIsRejected() {
        val encoded = ProtoWriter().varint(13, 116L).toByteArray()

        assertNull(SabrMessages.parseRedirectUrl(encoded, encoded.size))
    }

    @Test
    fun audioRequestCarriesTheSelectedFormatAndNoVideoPreference() {
        val body = SabrMessages.playbackRequest(
            ustreamerConfig = byteArrayOf(1, 2, 3),
            playerTimeMs = 60_000L,
            enabledTrackTypes = SabrMessages.TRACK_TYPES_AUDIO_ONLY,
            preferredAudio = SabrFormatId(140, 1_766_955_925_572_207L),
            preferredVideo = null,
            initializedFormats = listOf(SabrFormatId(140, 1_766_955_925_572_207L)),
            alreadyBuffered = emptyList(),
            clientName = 5,
            clientVersion = "20.10.4"
        )

        val fields = topLevelFields(body)
        assertTrue(fields.contains(1))
        assertTrue(fields.contains(2))
        assertTrue(fields.contains(5))
        assertTrue(fields.contains(16))
        assertTrue(!fields.contains(17))
        assertTrue(fields.contains(19))
    }

    @Test
    fun videoRequestDeclaresThePairedAudioAsAlreadyBuffered() {
        val audio = SabrFormatId(140, 2L)
        val body = SabrMessages.playbackRequest(
            ustreamerConfig = byteArrayOf(9),
            playerTimeMs = 0L,
            enabledTrackTypes = SabrMessages.TRACK_TYPES_AUDIO_AND_VIDEO,
            preferredAudio = audio,
            preferredVideo = SabrFormatId(133, 3L),
            initializedFormats = listOf(audio),
            alreadyBuffered = listOf(SabrBufferedRange(audio, 0L, 213_090L, 1L, 1_000L)),
            clientName = 5,
            clientVersion = "20.10.4"
        )

        val fields = topLevelFields(body)
        assertTrue(fields.contains(3))
        assertTrue(fields.contains(16))
        assertTrue(fields.contains(17))
    }

    @Test
    fun playerTimeIsNeverNegative() {
        val body = SabrMessages.playbackRequest(
            ustreamerConfig = byteArrayOf(1),
            playerTimeMs = -5_000L,
            enabledTrackTypes = SabrMessages.TRACK_TYPES_AUDIO_ONLY,
            preferredAudio = SabrFormatId(140, 1L),
            preferredVideo = null,
            initializedFormats = emptyList(),
            alreadyBuffered = emptyList(),
            clientName = 5,
            clientVersion = "1"
        )

        val abrState = nestedMessage(body, 1)
        val reader = ProtoReader(abrState, 0, abrState.size)
        while (reader.next()) {
            if (reader.field == 28) {
                assertEquals(0L, reader.readVarintValue())
                return
            }
            reader.skipValue()
        }
    }

    @Test
    fun protobufReaderSkipsEveryWireTypeItDoesNotUnderstand() {
        val body = ProtoWriter()
            .varint(1, 7L)
            .string(2, "text")
            .message(3, ProtoWriter().varint(1, 1L))
            .toByteArray()

        val reader = ProtoReader(body, 0, body.size)
        var seen = 0
        while (reader.next()) {
            seen++
            reader.skipValue()
        }
        assertEquals(3, seen)
    }

    @Test(expected = SabrProtocolException::class)
    fun protobufLengthBeyondTheBufferIsRejected() {
        val hostile = byteArrayOf(0x12, 0x7F, 0x01, 0x02)

        val reader = ProtoReader(hostile, 0, hostile.size)
        reader.next()
        reader.readBytesValue()
    }

    @Test(expected = SabrProtocolException::class)
    fun truncatedProtobufVarintIsRejected() {
        val hostile = byteArrayOf(0x08, 0x80.toByte())

        val reader = ProtoReader(hostile, 0, hostile.size)
        reader.next()
        reader.readVarintValue()
    }

    @Test(expected = SabrProtocolException::class)
    fun emptyMediaPartIsRejected() {
        SabrMessages.mediaHeaderId(ByteArray(0), 0)
    }

    @Test
    fun requestBytesMatchThePayloadsTheServerAcceptedInProtocolValidation() {
        val audioFormat = SabrFormatId(140, 1_766_955_925_572_207L)
        val videoFormat = SabrFormatId(133, 1_766_961_065_074_107L)
        val ustreamer = byteArrayOf(1, 2, 3)

        val audio = SabrMessages.playbackRequest(
            ustreamerConfig = ustreamer,
            playerTimeMs = 0L,
            enabledTrackTypes = SabrMessages.TRACK_TYPES_AUDIO_ONLY,
            preferredAudio = audioFormat,
            preferredVideo = null,
            initializedFormats = listOf(audioFormat),
            alreadyBuffered = emptyList(),
            clientName = 5,
            clientVersion = "20.10.4"
        )
        assertEquals(
            "0a06e00100c00201120c088c0110ef949ce297e191032a03010203" +
                "82010c088c0110ef949ce297e191039a010f0a0d8001058a010732302e31302e34",
            audio.toHex()
        )

        val seek = SabrMessages.playbackRequest(
            ustreamerConfig = ustreamer,
            playerTimeMs = 60_000L,
            enabledTrackTypes = SabrMessages.TRACK_TYPES_AUDIO_ONLY,
            preferredAudio = audioFormat,
            preferredVideo = null,
            initializedFormats = emptyList(),
            alreadyBuffered = emptyList(),
            clientName = 5,
            clientVersion = "20.10.4"
        )
        assertEquals(
            "0a08e001e0d403c002012a03010203" +
                "82010c088c0110ef949ce297e191039a010f0a0d8001058a010732302e31302e34",
            seek.toHex()
        )

        val video = SabrMessages.playbackRequest(
            ustreamerConfig = ustreamer,
            playerTimeMs = 0L,
            enabledTrackTypes = SabrMessages.TRACK_TYPES_AUDIO_AND_VIDEO,
            preferredAudio = audioFormat,
            preferredVideo = videoFormat,
            initializedFormats = listOf(videoFormat, audioFormat),
            alreadyBuffered = listOf(SabrBufferedRange(audioFormat, 0L, 213_090L, 1L, 1_000_000L)),
            clientName = 5,
            clientVersion = "20.10.4"
        )
        assertEquals(
            "0a06e00100c00200120c08850110bbbbf6f4aae19103120c088c0110ef949ce297e19103" +
                "1a1a0a0c088c0110ef949ce297e19103100018e2800d200128c0843d2a03010203" +
                "82010c088c0110ef949ce297e191038a010c08850110bbbbf6f4aae19103" +
                "9a010f0a0d8001058a010732302e31302e34",
            video.toHex()
        )
    }

    @Test
    fun zeroValuedFieldsStayOnTheWire() {
        val body = ProtoWriter().varint(1, 0L).varint(2, 5L).toByteArray()

        assertEquals(setOf(1, 2), topLevelFields(body))
    }

    private fun ByteArray.toHex(): String = joinToString("") { byte ->
        val value = byte.toInt() and 0xFF
        "0123456789abcdef"[value ushr 4].toString() + "0123456789abcdef"[value and 0x0F]
    }

    private fun topLevelFields(body: ByteArray): Set<Int> {
        val fields = LinkedHashSet<Int>()
        val reader = ProtoReader(body, 0, body.size)
        while (reader.next()) {
            fields += reader.field
            reader.skipValue()
        }
        return fields
    }

    private fun nestedMessage(body: ByteArray, field: Int): ByteArray {
        val reader = ProtoReader(body, 0, body.size)
        while (reader.next()) {
            if (reader.field == field && reader.wireType == PROTO_WIRE_LENGTH_DELIMITED) {
                return reader.readBytesValue()
            }
            reader.skipValue()
        }
        return ByteArray(0)
    }
}

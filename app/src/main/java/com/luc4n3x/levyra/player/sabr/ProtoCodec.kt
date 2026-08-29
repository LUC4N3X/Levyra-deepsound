package com.luc4n3x.levyra.player.sabr

import java.io.ByteArrayOutputStream

internal const val PROTO_WIRE_VARINT = 0
internal const val PROTO_WIRE_FIXED64 = 1
internal const val PROTO_WIRE_LENGTH_DELIMITED = 2
internal const val PROTO_WIRE_FIXED32 = 5

/**
 * Just enough protocol-buffer encoding for the SABR request bodies Levyra sends. A full runtime
 * would add hundreds of generated classes to the APK for the handful of fields this path uses.
 */
internal class ProtoWriter {
    private val output = ByteArrayOutputStream(256)

    /**
     * Zero values are written explicitly. The SABR server treats an omitted buffered-range start or
     * track-type bitfield differently from an explicit zero, and dropping them makes a video session
     * receive the audio bytes the audio session already owns.
     */
    fun varint(field: Int, value: Long): ProtoWriter {
        tag(field, PROTO_WIRE_VARINT)
        writeVarint(value)
        return this
    }

    fun bytes(field: Int, value: ByteArray): ProtoWriter {
        tag(field, PROTO_WIRE_LENGTH_DELIMITED)
        writeVarint(value.size.toLong())
        output.write(value)
        return this
    }

    fun string(field: Int, value: String): ProtoWriter = bytes(field, value.toByteArray(Charsets.UTF_8))

    fun message(field: Int, value: ProtoWriter): ProtoWriter = bytes(field, value.toByteArray())

    fun toByteArray(): ByteArray = output.toByteArray()

    private fun tag(field: Int, wireType: Int) {
        writeVarint(((field.toLong() shl 3) or wireType.toLong()))
    }

    private fun writeVarint(value: Long) {
        var remaining = value
        while (true) {
            val chunk = (remaining and 0x7FL).toInt()
            remaining = remaining ushr 7
            if (remaining == 0L) {
                output.write(chunk)
                return
            }
            output.write(chunk or 0x80)
        }
    }
}

/** Forward-only reader over a single protocol-buffer message held in memory. */
internal class ProtoReader(
    private val data: ByteArray,
    private var position: Int,
    private val limit: Int
) {
    var field: Int = 0
        private set
    var wireType: Int = 0
        private set

    fun next(): Boolean {
        if (position >= limit) return false
        val key = readVarint()
        field = (key ushr 3).toInt()
        wireType = (key and 7L).toInt()
        if (field <= 0) throw SabrProtocolException("invalid protobuf field number")
        return true
    }

    fun readVarintValue(): Long {
        requireWireType(wireType == PROTO_WIRE_VARINT)
        return readVarint()
    }

    fun readBytesValue(): ByteArray {
        val length = readLengthDelimitedLength()
        val value = data.copyOfRange(position, position + length)
        position += length
        return value
    }

    fun readStringValue(): String {
        val length = readLengthDelimitedLength()
        val value = String(data, position, length, Charsets.UTF_8)
        position += length
        return value
    }

    fun skipValue() {
        when (wireType) {
            PROTO_WIRE_VARINT -> readVarint()
            PROTO_WIRE_FIXED64 -> advance(8)
            PROTO_WIRE_LENGTH_DELIMITED -> advance(readLengthDelimitedLength())
            PROTO_WIRE_FIXED32 -> advance(4)
            else -> throw SabrProtocolException("unsupported protobuf wire type $wireType")
        }
    }

    private fun readLengthDelimitedLength(): Int {
        requireWireType(wireType == PROTO_WIRE_LENGTH_DELIMITED)
        val length = readVarint()
        if (length < 0L || length > (limit - position).toLong()) {
            throw SabrProtocolException("protobuf length out of bounds")
        }
        return length.toInt()
    }

    private fun advance(count: Int) {
        if (count < 0 || position + count > limit) throw SabrProtocolException("protobuf value out of bounds")
        position += count
    }

    private fun readVarint(): Long {
        var result = 0L
        var shift = 0
        while (shift <= 63) {
            if (position >= limit) throw SabrProtocolException("truncated protobuf varint")
            val current = data[position++].toInt() and 0xFF
            result = result or ((current and 0x7F).toLong() shl shift)
            if (current and 0x80 == 0) return result
            shift += 7
        }
        throw SabrProtocolException("protobuf varint too long")
    }

    private fun requireWireType(condition: Boolean) {
        if (!condition) throw SabrProtocolException("unexpected protobuf wire type $wireType for field $field")
    }
}

internal class SabrProtocolException(message: String) : java.io.IOException(message)

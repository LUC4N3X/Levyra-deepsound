package com.luc4n3x.levyra.player.sabr

import java.io.EOFException
import java.io.InputStream

internal class UmpReader(
    private val input: InputStream,
    private val maxPartSize: Int = MAX_PART_SIZE
) {
    private val header = ByteArray(MAX_VARINT_BYTES)
    private var payload = ByteArray(INITIAL_PAYLOAD_CAPACITY)

    var partType: Int = -1
        private set

    var partLength: Int = 0
        private set

    val partPayload: ByteArray
        get() = payload

    fun next(): Boolean {
        val first = input.read()
        if (first < 0) {
            partType = -1
            partLength = 0
            return false
        }
        partType = readVarint(first)
        val length = readVarint(readByte())
        if (length < 0 || length > maxPartSize) {
            throw SabrProtocolException("UMP part $partType declares $length bytes")
        }
        partLength = length
        if (length > payload.size) {
            payload = ByteArray(length)
        }
        readFully(payload, length)
        return true
    }

    private fun readByte(): Int {
        val value = input.read()
        if (value < 0) throw EOFException("truncated UMP header")
        return value
    }

    private fun readFully(target: ByteArray, length: Int) {
        var read = 0
        while (read < length) {
            val count = input.read(target, read, length - read)
            if (count < 0) throw EOFException("truncated UMP payload")
            read += count
        }
    }

    private fun readVarint(prefix: Int): Int {
        val size = varintSize(prefix)
        if (size == 1) return prefix
        var index = 0
        while (index < size - 1) {
            header[index] = readByte().toByte()
            index++
        }
        var rest = 0L
        var shift = 0
        for (byteIndex in 0 until size - 1) {
            rest = rest or ((header[byteIndex].toLong() and 0xFFL) shl shift)
            shift += 8
        }
        val value = if (size == MAX_VARINT_BYTES) {
            rest
        } else {
            val maskBits = 8 - size
            (rest shl maskBits) or (prefix and ((1 shl maskBits) - 1)).toLong()
        }
        if (value > Int.MAX_VALUE || value < 0L) throw SabrProtocolException("UMP varint out of range")
        return value.toInt()
    }

    private fun varintSize(prefix: Int): Int = when {
        prefix < 128 -> 1
        prefix < 192 -> 2
        prefix < 224 -> 3
        prefix < 240 -> 4
        else -> MAX_VARINT_BYTES
    }

    companion object {
        const val MAX_PART_SIZE = 512 * 1024
        private const val MAX_VARINT_BYTES = 5
        private const val INITIAL_PAYLOAD_CAPACITY = 8 * 1024
    }
}

internal fun umpVarint(value: Int): ByteArray {
    require(value >= 0) { "UMP varint must be positive" }
    return when {
        value < 128 -> byteArrayOf(value.toByte())
        value < (1 shl 14) -> byteArrayOf(
            (0x80 or (value and 0x3F)).toByte(),
            (value ushr 6).toByte()
        )
        value < (1 shl 21) -> byteArrayOf(
            (0xC0 or (value and 0x1F)).toByte(),
            ((value ushr 5) and 0xFF).toByte(),
            ((value ushr 13) and 0xFF).toByte()
        )
        value < (1 shl 28) -> byteArrayOf(
            (0xE0 or (value and 0x0F)).toByte(),
            ((value ushr 4) and 0xFF).toByte(),
            ((value ushr 12) and 0xFF).toByte(),
            ((value ushr 20) and 0xFF).toByte()
        )
        else -> byteArrayOf(
            0xF0.toByte(),
            (value and 0xFF).toByte(),
            ((value ushr 8) and 0xFF).toByte(),
            ((value ushr 16) and 0xFF).toByte(),
            ((value ushr 24) and 0xFF).toByte()
        )
    }
}

internal fun umpPart(type: Int, payload: ByteArray): ByteArray =
    umpVarint(type) + umpVarint(payload.size) + payload
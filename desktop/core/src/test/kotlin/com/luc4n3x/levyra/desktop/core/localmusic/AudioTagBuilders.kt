package com.luc4n3x.levyra.desktop.core.localmusic

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

internal object AudioTagBuilders {

    val JPEG_BYTES: ByteArray = byteArrayOf(
        0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10, 0x4A, 0x46
    )

    fun beInt(value: Int): ByteArray = byteArrayOf(
        (value ushr 24).toByte(),
        (value ushr 16).toByte(),
        (value ushr 8).toByte(),
        value.toByte()
    )

    fun leInt(value: Int): ByteArray = byteArrayOf(
        value.toByte(),
        (value ushr 8).toByte(),
        (value ushr 16).toByte(),
        (value ushr 24).toByte()
    )

    fun leShort(value: Int): ByteArray = byteArrayOf(value.toByte(), (value ushr 8).toByte())

    fun ascii(value: String): ByteArray = value.toByteArray(StandardCharsets.US_ASCII)

    fun syncSafe(value: Int): ByteArray = byteArrayOf(
        ((value ushr 21) and 0x7F).toByte(),
        ((value ushr 14) and 0x7F).toByte(),
        ((value ushr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte()
    )

    fun id3v23(frames: Map<String, String>, picture: ByteArray? = null): ByteArray {
        val body = ByteArrayOutputStream()
        frames.forEach { (id, value) ->
            val payload = byteArrayOf(3) + value.toByteArray(StandardCharsets.UTF_8)
            body.write(ascii(id))
            body.write(beInt(payload.size))
            body.write(byteArrayOf(0, 0))
            body.write(payload)
        }
        if (picture != null) {
            val payload = ByteArrayOutputStream()
            payload.write(byteArrayOf(0))
            payload.write(ascii("image/jpeg"))
            payload.write(byteArrayOf(0))
            payload.write(byteArrayOf(3))
            payload.write(ascii("cover"))
            payload.write(byteArrayOf(0))
            payload.write(picture)
            val bytes = payload.toByteArray()
            body.write(ascii("APIC"))
            body.write(beInt(bytes.size))
            body.write(byteArrayOf(0, 0))
            body.write(bytes)
        }
        val bodyBytes = body.toByteArray()
        val header = ByteArrayOutputStream()
        header.write(ascii("ID3"))
        header.write(byteArrayOf(3, 0, 0))
        header.write(syncSafe(bodyBytes.size))
        header.write(bodyBytes)
        return header.toByteArray()
    }

    fun mp3(frames: Map<String, String>, picture: ByteArray? = null, frameCount: Int = 400): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(id3v23(frames, picture))
        output.write(byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x00))
        output.write(ByteArray(32))
        output.write(ascii("Xing"))
        output.write(beInt(0x03))
        output.write(beInt(frameCount))
        output.write(beInt(1_044_000))
        output.write(ByteArray(1_024))
        return output.toByteArray()
    }

    fun flac(
        sampleRate: Int,
        channels: Int,
        bitDepth: Int,
        totalSamples: Long,
        comments: List<String>,
        picture: ByteArray? = null
    ): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(ascii("fLaC"))

        val streamInfo = ByteArray(34)
        val packed = (sampleRate.toLong() shl 44) or
            ((channels - 1).toLong() shl 41) or
            ((bitDepth - 1).toLong() shl 36) or
            (totalSamples and 0xFFFFFFFFFL)
        for (index in 0 until 8) {
            streamInfo[10 + index] = ((packed ushr ((7 - index) * 8)) and 0xFF).toByte()
        }
        output.write(byteArrayOf(0))
        output.write(beInt(streamInfo.size).copyOfRange(1, 4))
        output.write(streamInfo)

        val comment = ByteArrayOutputStream()
        val vendor = ascii("levyra-test")
        comment.write(leInt(vendor.size))
        comment.write(vendor)
        comment.write(leInt(comments.size))
        comments.forEach { entry ->
            val bytes = entry.toByteArray(StandardCharsets.UTF_8)
            comment.write(leInt(bytes.size))
            comment.write(bytes)
        }
        val commentBytes = comment.toByteArray()
        output.write(byteArrayOf(if (picture == null) 0x84.toByte() else 0x04))
        output.write(beInt(commentBytes.size).copyOfRange(1, 4))
        output.write(commentBytes)

        if (picture != null) {
            val block = flacPictureBlock(picture)
            output.write(byteArrayOf(0x86.toByte()))
            output.write(beInt(block.size).copyOfRange(1, 4))
            output.write(block)
        }
        output.write(ByteArray(2_048))
        return output.toByteArray()
    }

    fun flacPictureBlock(picture: ByteArray): ByteArray {
        val block = ByteArrayOutputStream()
        block.write(beInt(3))
        val mime = ascii("image/jpeg")
        block.write(beInt(mime.size))
        block.write(mime)
        block.write(beInt(0))
        block.write(beInt(500))
        block.write(beInt(500))
        block.write(beInt(24))
        block.write(beInt(0))
        block.write(beInt(picture.size))
        block.write(picture)
        return block.toByteArray()
    }
}

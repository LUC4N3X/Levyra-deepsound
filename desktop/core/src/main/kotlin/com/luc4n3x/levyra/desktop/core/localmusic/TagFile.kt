package com.luc4n3x.levyra.desktop.core.localmusic

import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

internal class TagFile(private val file: RandomAccessFile) : AutoCloseable {

    val length: Long = file.length()

    fun seek(position: Long) {
        file.seek(position.coerceIn(0L, length))
    }

    fun position(): Long = file.filePointer

    fun read(size: Int): ByteArray {
        if (size <= 0) return ByteArray(0)
        val remaining = (length - file.filePointer).coerceAtLeast(0L)
        val safeSize = minOf(size.toLong(), remaining).toInt()
        val buffer = ByteArray(safeSize)
        file.readFully(buffer)
        return buffer
    }

    fun readAt(position: Long, size: Int): ByteArray {
        seek(position)
        return read(size)
    }

    fun readAscii(size: Int): String = String(read(size), StandardCharsets.US_ASCII)

    fun skip(size: Long) {
        seek(file.filePointer + size)
    }

    override fun close() {
        file.close()
    }

    companion object {
        fun open(path: java.nio.file.Path): TagFile = TagFile(RandomAccessFile(path.toFile(), "r"))
    }
}

internal fun ByteArray.u8(offset: Int): Int = this[offset].toInt() and 0xFF

internal fun ByteArray.u16be(offset: Int): Int = (u8(offset) shl 8) or u8(offset + 1)

internal fun ByteArray.u16le(offset: Int): Int = (u8(offset + 1) shl 8) or u8(offset)

internal fun ByteArray.u24be(offset: Int): Int =
    (u8(offset) shl 16) or (u8(offset + 1) shl 8) or u8(offset + 2)

internal fun ByteArray.u32be(offset: Int): Long =
    (u8(offset).toLong() shl 24) or
        (u8(offset + 1).toLong() shl 16) or
        (u8(offset + 2).toLong() shl 8) or
        u8(offset + 3).toLong()

internal fun ByteArray.u32le(offset: Int): Long =
    (u8(offset + 3).toLong() shl 24) or
        (u8(offset + 2).toLong() shl 16) or
        (u8(offset + 1).toLong() shl 8) or
        u8(offset).toLong()

internal fun ByteArray.u64be(offset: Int): Long {
    var value = 0L
    for (index in 0 until 8) {
        value = (value shl 8) or u8(offset + index).toLong()
    }
    return value
}

internal fun ByteArray.ascii(offset: Int, size: Int): String =
    String(this, offset, size, StandardCharsets.US_ASCII)

internal fun ByteArray.latin1(offset: Int, size: Int): String =
    String(this, offset, size, StandardCharsets.ISO_8859_1)

internal fun ByteArray.utf8(offset: Int, size: Int): String =
    String(this, offset, size, StandardCharsets.UTF_8)

internal fun ByteArray.startsWithAscii(value: String, offset: Int = 0): Boolean {
    if (size < offset + value.length) return false
    for (index in value.indices) {
        if (this[offset + index] != value[index].code.toByte()) return false
    }
    return true
}

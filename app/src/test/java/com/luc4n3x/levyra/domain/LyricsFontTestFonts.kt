package com.luc4n3x.levyra.domain

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

internal object LyricsFontTestFonts {
    fun trueType(family: String = "Test Sans", weightAxis: IntRange? = null): ByteArray =
        sfnt(0x00010000, family, weightAxis, outlines = listOf("glyf", "loca"))

    fun openType(family: String = "Test Serif"): ByteArray =
        sfnt(0x4F54544F, family, null, outlines = listOf("CFF "))

    fun sfnt(version: Int, family: String, weightAxis: IntRange?, outlines: List<String>, headMagic: Int = 0x5F0F3CF5): ByteArray {
        val tables = sortedMapOf(
            "cmap" to ByteArray(8),
            "head" to head(headMagic),
            "hhea" to ByteArray(36),
            "hmtx" to ByteArray(4),
            "maxp" to ByteArray(6),
            "name" to name(family)
        )
        outlines.forEach { tables[it] = ByteArray(8) }
        weightAxis?.let { tables["fvar"] = fvar(it) }
        val out = ByteArrayOutputStream()
        val data = DataOutputStream(out)
        data.writeInt(version)
        data.writeShort(tables.size)
        data.writeShort(0)
        data.writeShort(0)
        data.writeShort(0)
        var offset = 12 + tables.size * 16
        val bodies = mutableListOf<ByteArray>()
        tables.forEach { (tag, body) ->
            data.writeBytes(tag)
            data.writeInt(0)
            data.writeInt(offset)
            data.writeInt(body.size)
            val padded = body.copyOf((body.size + 3) / 4 * 4)
            bodies += padded
            offset += padded.size
        }
        bodies.forEach(data::write)
        return out.toByteArray()
    }

    private fun head(magic: Int): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply {
            writeInt(0x00010000)
            writeInt(0)
            writeInt(0)
            writeInt(magic)
            write(ByteArray(54 - 16))
        }
        return out.toByteArray()
    }

    private fun name(family: String): ByteArray {
        val text = family.toByteArray(Charsets.UTF_16BE)
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply {
            writeShort(0)
            writeShort(1)
            writeShort(6 + 12)
            writeShort(3)
            writeShort(1)
            writeShort(0x0409)
            writeShort(1)
            writeShort(text.size)
            writeShort(0)
            write(text)
        }
        return out.toByteArray()
    }

    private fun fvar(range: IntRange): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply {
            writeShort(1)
            writeShort(0)
            writeShort(16)
            writeShort(2)
            writeShort(1)
            writeShort(20)
            writeShort(0)
            writeShort(0)
            writeBytes("wght")
            writeInt(range.first shl 16)
            writeInt(400 shl 16)
            writeInt(range.last shl 16)
            writeShort(0)
            writeShort(256)
        }
        return out.toByteArray()
    }
}

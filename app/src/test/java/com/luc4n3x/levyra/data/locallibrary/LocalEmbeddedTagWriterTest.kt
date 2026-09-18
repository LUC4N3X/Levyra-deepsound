package com.luc4n3x.levyra.data.locallibrary

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalEmbeddedTagWriterTest {

    @Test
    fun mp3EditPreservesArtworkLyricsAndCustomFrames() {
        val input = File.createTempFile("levyra-tag-input", ".mp3")
        val output = File.createTempFile("levyra-tag-output", ".mp3")
        try {
            val frames = concat(
                textFrame("TIT2", "Old title"),
                textFrame("TCOM", "Old composer"),
                frame("APIC", byteArrayOf(3) + "image/jpeg".toByteArray() + byteArrayOf(0, 3, 0) + byteArrayOf(1, 2, 3, 4)),
                frame("USLT", byteArrayOf(3) + "eng".toByteArray() + byteArrayOf(0) + "Keep these lyrics".toByteArray()),
                frame("TXXX", byteArrayOf(3) + "MOOD".toByteArray() + byteArrayOf(0) + "Night".toByteArray())
            )
            input.writeBytes(id3v24(frames) + byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x64))

            val result = LocalEmbeddedTagWriter.write(
                input = input,
                output = output,
                format = LocalEditableTagFormat.Mp3,
                edits = edits(title = "New title", composer = "New composer")
            )

            val raw = output.readBytes().toString(StandardCharsets.ISO_8859_1)
            assertTrue(result.success)
            assertTrue(raw.contains("New title"))
            assertFalse(raw.contains("Old title"))
            assertTrue(raw.contains("APIC"))
            assertTrue(raw.contains("USLT"))
            assertTrue(raw.contains("Keep these lyrics"))
            assertTrue(raw.contains("MOOD"))
            assertTrue(raw.contains("Night"))
            assertTrue(LocalDeepTagReader.read(output).composer.contains("New composer"))
        } finally {
            input.delete()
            output.delete()
        }
    }

    @Test
    fun mp3V23EditWritesReadableUtf16Comments() {
        val input = File.createTempFile("levyra-v23-input", ".mp3")
        val output = File.createTempFile("levyra-v23-output", ".mp3")
        try {
            val oldFrame = v23Frame(
                "TIT2",
                byteArrayOf(1) + "Old title".toByteArray(StandardCharsets.UTF_16)
            )
            input.writeBytes(id3v23(oldFrame) + byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x64))

            val result = LocalEmbeddedTagWriter.write(
                input = input,
                output = output,
                format = LocalEditableTagFormat.Mp3,
                edits = edits(title = "New title", composer = "Composer").copy(comment = "Commento UTF16")
            )

            assertTrue(result.success)
            val deep = LocalDeepTagReader.read(output)
            assertTrue(deep.comment.contains("Commento UTF16"))
        } finally {
            input.delete()
            output.delete()
        }
    }

    @Test
    fun flacEditPreservesReplayGainLyricsAndUnknownComments() {
        val input = File.createTempFile("levyra-tag-input", ".flac")
        val output = File.createTempFile("levyra-tag-output", ".flac")
        try {
            val comments = vorbisComment(
                "TITLE=Old title",
                "COMPOSER=Old composer",
                "REPLAYGAIN_TRACK_GAIN=-7.1 dB",
                "LYRICS=Keep these lyrics",
                "MOOD=Night"
            )
            input.writeBytes(
                "fLaC".toByteArray(StandardCharsets.ISO_8859_1) +
                    flacBlock(type = 0, last = false, payload = ByteArray(34)) +
                    flacBlock(type = 4, last = false, payload = comments) +
                    flacBlock(type = 6, last = true, payload = byteArrayOf(1, 2, 3, 4, 5)) +
                    ByteArray(64) { 0x55.toByte() }
            )

            val result = LocalEmbeddedTagWriter.write(
                input = input,
                output = output,
                format = LocalEditableTagFormat.Flac,
                edits = edits(title = "New title", composer = "New composer")
            )

            val raw = output.readBytes().toString(StandardCharsets.ISO_8859_1)
            assertTrue(result.success)
            assertTrue(raw.contains("TITLE=New title"))
            assertFalse(raw.contains("TITLE=Old title"))
            assertTrue(raw.contains("COMPOSER=New composer"))
            assertTrue(raw.contains("REPLAYGAIN_TRACK_GAIN=-7.1 dB"))
            assertTrue(raw.contains("LYRICS=Keep these lyrics"))
            assertTrue(raw.contains("MOOD=Night"))
            assertTrue(LocalDeepTagReader.read(output).composer.contains("New composer"))
        } finally {
            input.delete()
            output.delete()
        }
    }

    private fun edits(title: String, composer: String) = LocalTagEdits(
        title = title,
        artist = "Artist",
        album = "Album",
        albumArtist = "Album Artist",
        genre = "Ambient",
        year = "2026",
        trackNumber = "3",
        discNumber = "1",
        composer = composer,
        lyricist = "Lyricist",
        comment = "Comment",
        copyright = "2026 Example"
    )

    private fun id3v23(frames: ByteArray): ByteArray = concat(
        "ID3".toByteArray(StandardCharsets.ISO_8859_1),
        byteArrayOf(3, 0, 0),
        syncSafe(frames.size),
        frames
    )

    private fun v23Frame(id: String, payload: ByteArray): ByteArray = concat(
        id.toByteArray(StandardCharsets.ISO_8859_1),
        int32be(payload.size),
        byteArrayOf(0, 0),
        payload
    )

    private fun id3v24(frames: ByteArray): ByteArray = concat(
        "ID3".toByteArray(StandardCharsets.ISO_8859_1),
        byteArrayOf(4, 0, 0),
        syncSafe(frames.size),
        frames
    )

    private fun textFrame(id: String, value: String): ByteArray =
        frame(id, byteArrayOf(3) + value.toByteArray(StandardCharsets.UTF_8))

    private fun frame(id: String, payload: ByteArray): ByteArray = concat(
        id.toByteArray(StandardCharsets.ISO_8859_1),
        syncSafe(payload.size),
        byteArrayOf(0, 0),
        payload
    )

    private fun syncSafe(value: Int): ByteArray = byteArrayOf(
        ((value ushr 21) and 0x7F).toByte(),
        ((value ushr 14) and 0x7F).toByte(),
        ((value ushr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte()
    )

    private fun vorbisComment(vararg entries: String): ByteArray {
        val vendor = "Levyra Test".toByteArray(StandardCharsets.UTF_8)
        return ByteArrayOutputStream().apply {
            write(int32le(vendor.size))
            write(vendor)
            write(int32le(entries.size))
            entries.forEach { entry ->
                val bytes = entry.toByteArray(StandardCharsets.UTF_8)
                write(int32le(bytes.size))
                write(bytes)
            }
        }.toByteArray()
    }

    private fun flacBlock(type: Int, last: Boolean, payload: ByteArray): ByteArray =
        ByteArrayOutputStream().apply {
            write(type or if (last) 0x80 else 0)
            write((payload.size ushr 16) and 0xFF)
            write((payload.size ushr 8) and 0xFF)
            write(payload.size and 0xFF)
            write(payload)
        }.toByteArray()

    private fun int32be(value: Int): ByteArray = byteArrayOf(
        (value ushr 24).toByte(),
        (value ushr 16).toByte(),
        (value ushr 8).toByte(),
        value.toByte()
    )

    private fun int32le(value: Int): ByteArray = byteArrayOf(
        value.toByte(),
        (value ushr 8).toByte(),
        (value ushr 16).toByte(),
        (value ushr 24).toByte()
    )

    private fun concat(vararg parts: ByteArray): ByteArray =
        ByteArrayOutputStream().apply { parts.forEach { write(it) } }.toByteArray()
}

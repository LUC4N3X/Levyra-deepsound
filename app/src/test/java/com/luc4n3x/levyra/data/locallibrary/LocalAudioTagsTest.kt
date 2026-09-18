package com.luc4n3x.levyra.data.locallibrary

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAudioTagsTest {

    @Test
    fun fullTagSearchIncludesCreditsCommentsAndCustomTags() {
        val row = scanned()
            .toLocalMediaEntity("", 1L)
            .withDeepTags(
                LocalDeepTags(
                    composer = "Ryuichi Sakamoto",
                    lyricist = "Maya Angelou",
                    comment = "Night drive version",
                    copyright = "Example Records",
                    customTags = "MOOD=Nocturnal\nCATALOGNUMBER=LEV-42"
                )
            )

        assertTrue(row.matchesFullTagQuery("sakamoto"))
        assertTrue(row.matchesFullTagQuery("maya angelou"))
        assertTrue(row.matchesFullTagQuery("night drive"))
        assertTrue(row.matchesFullTagQuery("nocturnal"))
        assertTrue(row.matchesFullTagQuery("LEV 42"))
    }

    @Test
    fun id3ReaderFindsCreditsCommentsCopyrightAndUserTags() {
        val file = File.createTempFile("levyra-deep-tags", ".mp3")
        try {
            val frames = concat(
                textFrame("TCOM", "Nils Frahm"),
                textFrame("TEXT", "Jane Doe"),
                commentFrame("Recorded live"),
                textFrame("TCOP", "2026 Levyra Records"),
                userTextFrame("MOOD", "Focus")
            )
            file.writeBytes(id3v23(frames) + ByteArray(128) { 0x55.toByte() })

            val tags = LocalDeepTagReader.read(file)

            assertEquals("Nils Frahm", tags.composer)
            assertEquals("Jane Doe", tags.lyricist)
            assertEquals("Recorded live", tags.comment)
            assertEquals("2026 Levyra Records", tags.copyright)
            assertTrue(tags.customTags.contains("MOOD=Focus"))
        } finally {
            file.delete()
        }
    }

    private fun scanned() = ScannedLocalAudio(
        volumeName = "external_primary",
        mediaStoreId = 7L,
        contentUri = "content://media/external_primary/audio/media/7",
        filePath = "",
        relativePath = "Music/",
        displayName = "Test.mp3",
        title = "Test",
        artist = "Artist",
        album = "Album",
        albumArtist = "Artist",
        genre = "Ambient",
        year = 2026,
        trackField = 1,
        trackText = "1",
        discText = "1",
        durationMs = 180_000L,
        mimeType = "audio/mpeg",
        bitrate = 320_000,
        sizeBytes = 3_000_000L,
        dateAddedMs = 1L,
        dateModifiedMs = 1L,
        albumId = 3L
    )

    private fun id3v23(frames: ByteArray): ByteArray = concat(
        "ID3".toByteArray(StandardCharsets.ISO_8859_1),
        byteArrayOf(3, 0, 0),
        syncSafe(frames.size),
        frames
    )

    private fun textFrame(id: String, value: String): ByteArray =
        frame(id, byteArrayOf(3) + value.toByteArray(StandardCharsets.UTF_8))

    private fun commentFrame(value: String): ByteArray =
        frame(
            "COMM",
            byteArrayOf(3) +
                "eng".toByteArray(StandardCharsets.ISO_8859_1) +
                byteArrayOf(0) +
                value.toByteArray(StandardCharsets.UTF_8)
        )

    private fun userTextFrame(name: String, value: String): ByteArray =
        frame(
            "TXXX",
            byteArrayOf(3) +
                name.toByteArray(StandardCharsets.UTF_8) +
                byteArrayOf(0) +
                value.toByteArray(StandardCharsets.UTF_8)
        )

    private fun frame(id: String, payload: ByteArray): ByteArray = concat(
        id.toByteArray(StandardCharsets.ISO_8859_1),
        int32(payload.size),
        byteArrayOf(0, 0),
        payload
    )

    private fun int32(value: Int): ByteArray = byteArrayOf(
        (value ushr 24).toByte(),
        (value ushr 16).toByte(),
        (value ushr 8).toByte(),
        value.toByte()
    )

    private fun syncSafe(value: Int): ByteArray = byteArrayOf(
        ((value ushr 21) and 0x7F).toByte(),
        ((value ushr 14) and 0x7F).toByte(),
        ((value ushr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte()
    )

    private fun concat(vararg parts: ByteArray): ByteArray =
        ByteArrayOutputStream().apply { parts.forEach { write(it) } }.toByteArray()
}

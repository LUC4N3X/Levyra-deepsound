package com.luc4n3x.levyra.player.offline.tagging

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraM4aTagWriterTest {
    @Test
    fun writesRichStandardAndProvenanceAtoms() {
        val input = File.createTempFile("levyra-input", ".m4a")
        val output = File.createTempFile("levyra-output", ".m4a")
        input.writeBytes(
            atom("ftyp", "M4A ".toByteArray(StandardCharsets.US_ASCII)) +
                atom("moov", byteArrayOf()) +
                atom("mdat", ByteArray(32) { it.toByte() })
        )

        try {
            val result = LevyraM4aTagWriter.write(
                input = input,
                output = output,
                metadata = LevyraM4aMetadata(
                    title = "Song",
                    artist = "Artist",
                    album = "Album",
                    releaseDate = "2026-07-20",
                    genres = listOf("Pop", "Dance"),
                    trackNumber = 4,
                    discNumber = 2,
                    lyrics = "First line\nSecond line",
                    explicit = true,
                    isrc = "ITABC2600001",
                    upc = "123456789012",
                    sourceUrl = "https://www.youtube.com/watch?v=video-123",
                    sourceProvider = "YouTube Music",
                    metadataProvider = "Qobuz",
                    metadataConfidence = 96,
                    trackId = "video-123",
                    albumId = "MPRE-ALBUM",
                    artistIds = listOf("UC-ARTIST"),
                    albumUrl = "https://music.youtube.com/browse/MPRE-ALBUM",
                    counterpartId = "counterpart-1",
                    mediaType = "MUSIC_VIDEO_TYPE_ATV"
                )
            )

            val raw = output.readBytes().toString(StandardCharsets.ISO_8859_1)
            assertTrue(result.success)
            assertTrue(raw.contains("trkn"))
            assertTrue(raw.contains("disk"))
            assertTrue(raw.contains("rtng"))
            assertTrue(raw.contains("----"))
            assertTrue(raw.contains("ISRC"))
            assertTrue(raw.contains("SOURCE_URL"))
            assertTrue(raw.contains("METADATA_CONFIDENCE"))
            assertTrue(raw.contains("ALBUM_ID"))
            assertTrue(raw.contains("ARTIST_IDS"))
            assertTrue(raw.contains("COUNTERPART_ID"))
            assertTrue(raw.contains("MEDIA_TYPE"))
            assertTrue(raw.contains("First line\nSecond line"))
        } finally {
            input.delete()
            output.delete()
        }
    }

    private fun atom(type: String, payload: ByteArray): ByteArray {
        val output = ByteArrayOutputStream(payload.size + 8)
        val size = payload.size + 8
        output.write((size ushr 24) and 0xFF)
        output.write((size ushr 16) and 0xFF)
        output.write((size ushr 8) and 0xFF)
        output.write(size and 0xFF)
        output.write(type.toByteArray(StandardCharsets.US_ASCII))
        output.write(payload)
        return output.toByteArray()
    }
}

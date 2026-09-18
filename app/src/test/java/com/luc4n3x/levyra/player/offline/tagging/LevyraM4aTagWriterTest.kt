package com.luc4n3x.levyra.player.offline.tagging

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertFalse
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

    @Test
    fun selectiveEditorPreservesLyricsArtworkAndLevyraProvenance() {
        val input = File.createTempFile("levyra-original", ".m4a")
        val seeded = File.createTempFile("levyra-seeded", ".m4a")
        val edited = File.createTempFile("levyra-edited", ".m4a")
        input.writeBytes(
            atom("ftyp", "M4A ".toByteArray(StandardCharsets.US_ASCII)) +
                atom("moov", byteArrayOf()) +
                atom("mdat", ByteArray(64) { (it * 3).toByte() })
        )
        val cover = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
            0x01, 0x02, 0x03, 0x04
        )

        try {
            val seededResult = LevyraM4aTagWriter.write(
                input = input,
                output = seeded,
                metadata = LevyraM4aMetadata(
                    title = "Old title",
                    artist = "Old artist",
                    album = "Old album",
                    lyrics = "Do not delete these lyrics",
                    trackId = "keep-track-id",
                    sourceProvider = "YouTube Music",
                    artworkData = cover
                )
            )
            assertTrue(seededResult.success)

            val editResult = LevyraM4aTagWriter.writeTags(
                input = seeded,
                output = edited,
                edits = LevyraM4aTagEdits(
                    title = "New title",
                    artist = "New artist",
                    album = "New album",
                    albumArtist = "New album artist",
                    genre = "Ambient",
                    year = "2026",
                    trackNumber = 7,
                    discNumber = 2,
                    composer = "Composer Name",
                    lyricist = "Lyricist Name",
                    comment = "Edited in Levyra",
                    copyright = "2026 Example"
                )
            )

            val bytes = edited.readBytes()
            val raw = bytes.toString(StandardCharsets.ISO_8859_1)
            assertTrue(editResult.success)
            assertTrue(raw.contains("New title"))
            assertFalse(raw.contains("Old title"))
            assertTrue(raw.contains("Do not delete these lyrics"))
            assertTrue(raw.contains("keep-track-id"))
            assertTrue(raw.contains("SOURCE_PROVIDER"))
            assertTrue(raw.contains("Composer Name"))
            assertTrue(raw.contains("Lyricist Name"))
            assertTrue(indexOf(bytes, cover) >= 0)
        } finally {
            input.delete()
            seeded.delete()
            edited.delete()
        }
    }

    private fun indexOf(haystack: ByteArray, needle: ByteArray): Int {
        if (needle.isEmpty() || haystack.size < needle.size) return -1
        for (start in 0..haystack.size - needle.size) {
            var matches = true
            for (index in needle.indices) {
                if (haystack[start + index] != needle[index]) {
                    matches = false
                    break
                }
            }
            if (matches) return start
        }
        return -1
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

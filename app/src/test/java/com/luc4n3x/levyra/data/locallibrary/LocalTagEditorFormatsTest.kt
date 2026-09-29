package com.luc4n3x.levyra.data.locallibrary

import com.luc4n3x.levyra.player.offline.tagging.LevyraM4aMetadata
import com.luc4n3x.levyra.player.offline.tagging.LevyraM4aTagWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalTagEditorFormatsTest {
    private val temporaryFiles = ArrayList<File>()

    @After
    fun cleanUp() {
        temporaryFiles.forEach(File::delete)
    }

    @Test
    fun mp3EditIsRereadAndKeepsAudioPayload() {
        val payload = mpegPayload()
        val input = temp(".mp3", id3v24(textFrame("TIT2", "Old title") + textFrame("TPE1", "Old artist")) + payload)
        val output = temp(".mp3")
        val edits = edits(title = "New title", artist = "New artist", album = "New album")

        val result = LocalEmbeddedTagWriter.write(input, output, LocalEditableTagFormat.Mp3, edits)

        assertTrue(result.success)
        assertTrue(LocalEmbeddedTagWriter.verify(output, edits))
        val tags = LocalDeepTagReader.readTagMap(output)
        assertEquals("New title", tags["TITLE"])
        assertEquals("New artist", tags["ARTIST"])
        assertEquals("New album", tags["ALBUM"])
        assertEquals("2026", tags["DATE"])
        assertEquals("3", tags["TRACKNUMBER"])
        assertTrue(output.readBytes().endsWith(payload))
    }

    @Test
    fun mp3WithoutAnyTagGetsANewTagAndIdenticalAudio() {
        val payload = mpegPayload()
        val input = temp(".mp3", payload)
        val output = temp(".mp3")

        val result = LocalEmbeddedTagWriter.write(input, output, LocalEditableTagFormat.Mp3, edits(title = "Fresh"))

        assertTrue(result.success)
        assertEquals("Fresh", LocalDeepTagReader.readTagMap(output)["TITLE"])
        assertTrue(output.readBytes().endsWith(payload))
    }

    @Test
    fun mp3ArtworkReplacementRemovalAndLyricsRoundTrip() {
        val oldCover = jpeg(1)
        val newCover = jpeg(2)
        val input = temp(
            ".mp3",
            id3v24(textFrame("TIT2", "Song") + frame("APIC", apicPayload(oldCover))) + mpegPayload()
        )
        val replaced = temp(".mp3")
        val removed = temp(".mp3")
        val withLyrics = edits(title = "Song").copy(lyrics = "First line\n\nSecond line")

        assertTrue(
            LocalEmbeddedTagWriter.write(
                input, replaced, LocalEditableTagFormat.Mp3, withLyrics,
                LocalArtworkWrite.Embed(newCover, "image/jpeg", 600, 600)
            ).success
        )
        val replacedBytes = replaced.readBytes()
        assertTrue(replacedBytes.indexOf(newCover) >= 0)
        assertTrue(replacedBytes.indexOf(oldCover) < 0)
        assertEquals("First line\n\nSecond line", LocalDeepTagReader.readEmbeddedLyrics(replaced))
        assertTrue(LocalEmbeddedTagWriter.verify(replaced, withLyrics))

        val cleared = edits(title = "Song").copy(lyrics = "")
        assertTrue(LocalEmbeddedTagWriter.write(replaced, removed, LocalEditableTagFormat.Mp3, cleared, LocalArtworkWrite.Remove).success)
        val removedBytes = removed.readBytes()
        assertTrue(removedBytes.indexOf(newCover) < 0)
        assertEquals("", LocalDeepTagReader.readEmbeddedLyrics(removed))
    }

    @Test
    fun mp3KeepEditLeavesExistingArtworkAndLyricsUntouched() {
        val cover = jpeg(3)
        val input = temp(
            ".mp3",
            id3v24(frame("APIC", apicPayload(cover)) + frame("USLT", usltV24("Keep me"))) + mpegPayload()
        )
        val output = temp(".mp3")

        assertTrue(LocalEmbeddedTagWriter.write(input, output, LocalEditableTagFormat.Mp3, edits(title = "Other")).success)

        assertTrue(output.readBytes().indexOf(cover) >= 0)
        assertEquals("Keep me", LocalDeepTagReader.readEmbeddedLyrics(output))
    }

    @Test
    fun id3v23LyricsUseReadableUtf16() {
        val input = temp(".mp3", id3v23(v23Frame("TIT2", byteArrayOf(1) + "Old".toByteArray(StandardCharsets.UTF_16))) + mpegPayload())
        val output = temp(".mp3")
        val edited = edits(title = "Nuovo").copy(lyrics = "Città – 東京")

        assertTrue(LocalEmbeddedTagWriter.write(input, output, LocalEditableTagFormat.Mp3, edited).success)

        assertEquals("Città – 東京", LocalDeepTagReader.readEmbeddedLyrics(output))
        assertTrue(LocalEmbeddedTagWriter.verify(output, edited))
    }

    @Test
    fun flacEditArtworkAndLyricsRoundTripWithIdenticalFrames() {
        val frames = ByteArray(96) { (it * 7).toByte() }
        val oldPicture = flacPicturePayload(jpeg(4))
        val input = temp(
            ".flac",
            "fLaC".toByteArray(StandardCharsets.ISO_8859_1) +
                flacBlock(0, false, ByteArray(34)) +
                flacBlock(4, false, vorbisComment("TITLE=Old", "REPLAYGAIN_TRACK_GAIN=-6 dB", "METADATA_BLOCK_PICTURE=abc")) +
                flacBlock(6, false, oldPicture) +
                flacBlock(1, true, ByteArray(16)) +
                frames
        )
        val output = temp(".flac")
        val newCover = jpeg(5)
        val edited = edits(title = "New", artist = "Band", album = "Record").copy(lyrics = "La la la")

        val result = LocalEmbeddedTagWriter.write(
            input, output, LocalEditableTagFormat.Flac, edited,
            LocalArtworkWrite.Embed(newCover, "image/jpeg", 800, 800)
        )

        assertTrue(result.success)
        val bytes = output.readBytes()
        val raw = bytes.toString(StandardCharsets.ISO_8859_1)
        assertEquals("New", LocalDeepTagReader.readTagMap(output)["TITLE"])
        assertEquals("La la la", LocalDeepTagReader.readEmbeddedLyrics(output))
        assertTrue(raw.contains("REPLAYGAIN_TRACK_GAIN=-6 dB"))
        assertFalse(raw.contains("METADATA_BLOCK_PICTURE=abc"))
        assertTrue(bytes.indexOf(newCover) >= 0)
        assertTrue(bytes.indexOf(jpeg(4)) < 0)
        assertTrue(bytes.endsWith(frames))
        assertTrue(LocalEmbeddedTagWriter.verify(output, edited))

        val stripped = temp(".flac")
        assertTrue(LocalEmbeddedTagWriter.write(output, stripped, LocalEditableTagFormat.Flac, edited, LocalArtworkWrite.Remove).success)
        assertTrue(stripped.readBytes().indexOf(newCover) < 0)
        assertTrue(stripped.readBytes().endsWith(frames))
    }

    @Test
    fun m4aEditLyricsAndArtworkRoundTripWithIdenticalMediaData() {
        val media = ByteArray(128) { (it * 5).toByte() }
        val base = temp(".m4a", atom("ftyp", "M4A ".toByteArray(StandardCharsets.US_ASCII)) + atom("moov", byteArrayOf()) + atom("mdat", media))
        val seeded = temp(".m4a")
        assertTrue(
            LevyraM4aTagWriter.write(
                base,
                seeded,
                LevyraM4aMetadata(title = "Old", artist = "Old artist", album = "Old album", lyrics = "Old lyrics", artworkData = jpeg(6))
            ).success
        )
        val edited = temp(".m4a")
        val newCover = jpeg(7)
        val changes = edits(title = "Titolo", artist = "Artista", album = "Album").copy(lyrics = "Nuovo testo\n\nStrofa")

        val result = LocalEmbeddedTagWriter.write(
            seeded, edited, LocalEditableTagFormat.M4a, changes,
            LocalArtworkWrite.Embed(newCover, "image/jpeg", 500, 500)
        )

        assertTrue(result.success)
        val tags = LocalDeepTagReader.readTagMap(edited)
        assertEquals("Titolo", tags["TITLE"])
        assertEquals("Artista", tags["ARTIST"])
        assertEquals("Album", tags["ALBUM"])
        assertEquals("Nuovo testo\n\nStrofa", LocalDeepTagReader.readEmbeddedLyrics(edited))
        val bytes = edited.readBytes()
        assertTrue(bytes.indexOf(newCover) >= 0)
        assertTrue(bytes.indexOf(jpeg(6)) < 0)
        assertTrue(bytes.indexOf(media) >= 0)
        assertTrue(LocalEmbeddedTagWriter.verify(edited, changes))

        val removed = temp(".m4a")
        assertTrue(LocalEmbeddedTagWriter.write(edited, removed, LocalEditableTagFormat.M4a, changes, LocalArtworkWrite.Remove).success)
        assertTrue(removed.readBytes().indexOf(newCover) < 0)
        assertTrue(removed.readBytes().indexOf(media) >= 0)
    }

    @Test
    fun unsupportedFormatAndInvalidArtworkAreRejected() {
        val input = temp(".ogg", "OggS".toByteArray(StandardCharsets.US_ASCII) + ByteArray(64))
        val output = temp(".ogg")

        assertEquals(LocalEditableTagFormat.Unsupported, LocalEmbeddedTagWriter.formatOf("audio/ogg", "song.ogg"))
        assertEquals("unsupported_format", LocalEmbeddedTagWriter.write(input, output, LocalEditableTagFormat.Unsupported, edits("x")).reason)

        val mp3 = temp(".mp3", mpegPayload())
        val notAnImage = LocalArtworkWrite.Embed("GIF89a....".toByteArray(), "image/gif", 10, 10)
        val result = LocalEmbeddedTagWriter.write(mp3, temp(".mp3"), LocalEditableTagFormat.Mp3, edits("x"), notAnImage)
        assertFalse(result.success)
        assertEquals("unsupported_artwork", result.reason)
    }

    @Test
    fun malformedContainersFailWithoutProducingOutput() {
        val truncatedId3 = temp(".mp3", "ID3".toByteArray(StandardCharsets.ISO_8859_1) + byteArrayOf(4, 0, 0, 0x7F, 0x7F, 0x7F, 0x7F) + ByteArray(8))
        val brokenFlac = temp(".flac", "fLaC".toByteArray(StandardCharsets.ISO_8859_1) + byteArrayOf(0, 0, 0x7F.toByte()))
        val brokenM4a = temp(".m4a", atom("ftyp", "M4A ".toByteArray()) + byteArrayOf(0, 0, 0x7F, 0x7F) + "moov".toByteArray())

        listOf(
            truncatedId3 to LocalEditableTagFormat.Mp3,
            brokenFlac to LocalEditableTagFormat.Flac,
            brokenM4a to LocalEditableTagFormat.M4a
        ).forEach { (input, format) ->
            val output = temp(".out")
            val before = input.readBytes()
            val result = LocalEmbeddedTagWriter.write(input, output, format, edits("x"))
            assertFalse("$format should fail", result.success)
            assertArrayEquals(before, input.readBytes())
        }
    }

    @Test
    fun verificationRejectsAWorkingCopyThatLostTheEdit() {
        val output = temp(".mp3", id3v24(textFrame("TIT2", "Stale")) + mpegPayload())

        assertFalse(LocalEmbeddedTagWriter.verify(output, edits(title = "Expected")))
        assertFalse(LocalEmbeddedTagWriter.verify(temp(".mp3"), edits(title = "Expected")))
    }

    @Test
    fun verificationChecksEveryEditedFieldAcrossFormats() {
        val complete = edits(title = "Song").copy(
            composer = "Composer",
            lyricist = "Lyricist",
            comment = "Comment",
            copyright = "2026 Label"
        )
        writtenCopies(complete).forEach { (format, output) ->
            assertTrue("$format should verify", LocalEmbeddedTagWriter.verify(output, complete))
            listOf(
                complete.copy(albumArtist = "Other Album Artist"),
                complete.copy(year = "1999"),
                complete.copy(trackNumber = "4"),
                complete.copy(discNumber = "2"),
                complete.copy(genre = "Jazz"),
                complete.copy(composer = "Other Composer"),
                complete.copy(lyricist = "Other Lyricist"),
                complete.copy(comment = "Other Comment"),
                complete.copy(copyright = "1999 Other")
            ).forEach { stale ->
                assertFalse("$format should reject $stale", LocalEmbeddedTagWriter.verify(output, stale))
            }
        }
    }

    @Test
    fun verificationChecksArtworkReplacementRemovalAndKeep() {
        val newCover = jpeg(8)
        val otherCover = jpeg(9)
        val replaceWith = LocalArtworkWrite.Embed(newCover, "image/jpeg", 400, 400)
        writtenCopies(edits(title = "Song"), replaceWith).forEach { (format, replaced) ->
            assertTrue("$format replaced cover", LocalEmbeddedTagWriter.verify(replaced, edits(title = "Song"), replaceWith))
            assertFalse(
                "$format stale cover",
                LocalEmbeddedTagWriter.verify(replaced, edits(title = "Song"), LocalArtworkWrite.Embed(otherCover, "image/jpeg", 400, 400))
            )
            assertFalse("$format cover still present", LocalEmbeddedTagWriter.verify(replaced, edits(title = "Song"), LocalArtworkWrite.Remove))
            assertTrue("$format keep", LocalEmbeddedTagWriter.verify(replaced, edits(title = "Song"), LocalArtworkWrite.Keep))
            assertArrayEquals(newCover, LocalDeepTagReader.readEmbeddedArtwork(replaced))

            val removed = temp(".${replaced.extension}")
            assertTrue(LocalEmbeddedTagWriter.write(replaced, removed, format, edits(title = "Song"), LocalArtworkWrite.Remove).success)
            assertTrue("$format removed cover", LocalEmbeddedTagWriter.verify(removed, edits(title = "Song"), LocalArtworkWrite.Remove))
            assertFalse("$format missing cover", LocalEmbeddedTagWriter.verify(removed, edits(title = "Song"), replaceWith))

            val kept = temp(".${replaced.extension}")
            assertTrue(LocalEmbeddedTagWriter.write(replaced, kept, format, edits(title = "Other")).success)
            assertTrue("$format kept cover", LocalEmbeddedTagWriter.verify(kept, edits(title = "Other"), LocalArtworkWrite.Keep))
            assertArrayEquals(newCover, LocalDeepTagReader.readEmbeddedArtwork(kept))
        }
    }

    @Test
    fun removalVerificationRejectsCoversThatCannotBeFullyRead() {
        val legacyFlac = temp(
            ".flac",
            "fLaC".toByteArray(StandardCharsets.ISO_8859_1) +
                flacBlock(0, false, ByteArray(34)) +
                flacBlock(4, true, vorbisComment("TITLE=Song", "METADATA_BLOCK_PICTURE=abc")) +
                ByteArray(32)
        )
        val hugeCover = jpeg(11) + ByteArray(5 * 1024 * 1024)
        val oversizedId3 = temp(".mp3", id3v24(textFrame("TIT2", "Song") + frame("APIC", apicPayload(hugeCover))) + mpegPayload())
        val titleOnly = edits(title = "Song", artist = "", album = "").copy(
            albumArtist = "",
            genre = "",
            year = "",
            trackNumber = "",
            discNumber = ""
        )

        assertFalse(LocalEmbeddedTagWriter.verify(legacyFlac, titleOnly, LocalArtworkWrite.Remove))
        assertFalse(LocalEmbeddedTagWriter.verify(oversizedId3, titleOnly, LocalArtworkWrite.Remove))
    }

    @Test
    fun verificationRejectsEmbeddedArtworkThatIsNotAnImage() {
        val output = temp(".mp3", id3v24(textFrame("TIT2", "Song") + frame("APIC", apicPayload("not an image".toByteArray()))) + mpegPayload())
        val titleOnly = edits(title = "Song", artist = "", album = "").copy(
            albumArtist = "",
            genre = "",
            year = "",
            trackNumber = "",
            discNumber = ""
        )
        val claimed = LocalArtworkWrite.Embed("not an image".toByteArray(), "image/jpeg", 1, 1)

        assertTrue(LocalEmbeddedTagWriter.verify(output, titleOnly))
        assertFalse(LocalEmbeddedTagWriter.verify(output, titleOnly, claimed))
    }

    @Test
    fun failedReplacementRestoresTheOriginalBytes() {
        val original = temp(".mp3", ByteArray(4_096) { it.toByte() })
        val edited = temp(".mp3", ByteArray(8_192) { (it * 3).toByte() })
        val target = temp(".mp3", original.readBytes())
        var attempts = 0

        val outcome = replaceWithRollback(edited, original) { source ->
            attempts++
            target.writeBytes(source.readBytes().copyOf(1_000))
            if (attempts == 1) throw IOException("disk full")
            target.writeBytes(source.readBytes())
        }

        assertEquals(LocalReplaceOutcome.RolledBack, outcome)
        assertArrayEquals(original.readBytes(), target.readBytes())
    }

    @Test
    fun failedRollbackIsReported() {
        val outcome = replaceWithRollback(temp(".a"), temp(".b")) { throw IOException("gone") }

        assertEquals(LocalReplaceOutcome.RollbackFailed, outcome)
    }

    @Test
    fun editLimitFollowsAvailableHeap() {
        assertEquals(64L * 1024L * 1024L, localTagEditLimitBytes(256L * 1024L * 1024L))
        assertEquals(LocalEmbeddedTagWriter.MAX_INPUT_BYTES, localTagEditLimitBytes(4L * 1024L * 1024L * 1024L))
        assertTrue(localTagWorkspaceBytes(10L) > 20L)
    }

    @Test
    fun artworkSizingDownscalesHugeImagesAndKeepsSmallOnes() {
        assertEquals(1_600 to 900, localArtworkTargetSize(6_400, 3_600, MAX_ARTWORK_EDGE_PX))
        assertEquals(500 to 500, localArtworkTargetSize(500, 500, MAX_ARTWORK_EDGE_PX))
        assertEquals(4, localArtworkSampleSize(8_000, 8_000, MAX_ARTWORK_EDGE_PX))
        assertEquals(1, localArtworkSampleSize(1_200, 1_200, MAX_ARTWORK_EDGE_PX))
        assertTrue(canEmbedOriginalArtwork("image/jpeg", 1_000, 1_000, orientationNormal = true))
        assertFalse(canEmbedOriginalArtwork("image/jpeg", 1_000, 1_000, orientationNormal = false))
        assertFalse(canEmbedOriginalArtwork("image/heic", 1_000, 1_000, orientationNormal = true))
        assertFalse(canEmbedOriginalArtwork("image/png", 4_000, 4_000, orientationNormal = true))
    }

    private fun writtenCopies(
        changes: LocalTagEdits,
        artwork: LocalArtworkWrite = LocalArtworkWrite.Keep
    ): List<Pair<LocalEditableTagFormat, File>> {
        val mp3 = temp(".mp3", id3v24(textFrame("TIT2", "Old") + frame("APIC", apicPayload(jpeg(10)))) + mpegPayload())
        val flac = temp(
            ".flac",
            "fLaC".toByteArray(StandardCharsets.ISO_8859_1) +
                flacBlock(0, false, ByteArray(34)) +
                flacBlock(4, false, vorbisComment("TITLE=Old")) +
                flacBlock(6, false, flacPicturePayload(jpeg(10))) +
                flacBlock(1, true, ByteArray(16)) +
                ByteArray(32) { it.toByte() }
        )
        val base = temp(".m4a", atom("ftyp", "M4A ".toByteArray(StandardCharsets.US_ASCII)) + atom("moov", byteArrayOf()) + atom("mdat", ByteArray(64)))
        val m4a = temp(".m4a")
        assertTrue(LevyraM4aTagWriter.write(base, m4a, LevyraM4aMetadata(title = "Old", artist = "Old", album = "Old", artworkData = jpeg(10))).success)
        return listOf(
            LocalEditableTagFormat.Mp3 to mp3,
            LocalEditableTagFormat.Flac to flac,
            LocalEditableTagFormat.M4a to m4a
        ).map { (format, input) ->
            val output = temp(".${input.extension}")
            assertTrue("$format write", LocalEmbeddedTagWriter.write(input, output, format, changes, artwork).success)
            format to output
        }
    }

    private fun edits(title: String, artist: String = "Artist", album: String = "Album") = LocalTagEdits(
        title = title,
        artist = artist,
        album = album,
        albumArtist = "Album Artist",
        genre = "Pop",
        year = "2026",
        trackNumber = "3",
        discNumber = "1",
        composer = "",
        lyricist = "",
        comment = "",
        copyright = ""
    )

    private fun temp(suffix: String, content: ByteArray? = null): File =
        File.createTempFile("levyra-editor", suffix).also { file ->
            temporaryFiles += file
            if (content != null) file.writeBytes(content) else file.writeBytes(ByteArray(0))
        }

    private fun mpegPayload(): ByteArray = ByteArray(417) { index ->
        when (index % 417) {
            0 -> 0xFF.toByte()
            1 -> 0xFB.toByte()
            2 -> 0x90.toByte()
            else -> (index * 13).toByte()
        }
    }

    private fun jpeg(seed: Int): ByteArray =
        byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) + ByteArray(48) { (seed * 31 + it).toByte() }

    private fun apicPayload(image: ByteArray): ByteArray =
        byteArrayOf(0) + "image/jpeg".toByteArray(StandardCharsets.ISO_8859_1) + byteArrayOf(0, 3, 0) + image

    private fun usltV24(text: String): ByteArray =
        byteArrayOf(3) + "eng".toByteArray(StandardCharsets.ISO_8859_1) + byteArrayOf(0) + text.toByteArray(StandardCharsets.UTF_8)

    private fun flacPicturePayload(image: ByteArray): ByteArray {
        val mime = "image/jpeg".toByteArray(StandardCharsets.US_ASCII)
        return concat(
            int32be(3), int32be(mime.size), mime, int32be(0),
            int32be(1), int32be(1), int32be(24), int32be(0),
            int32be(image.size), image
        )
    }

    private fun id3v24(frames: ByteArray): ByteArray =
        concat("ID3".toByteArray(StandardCharsets.ISO_8859_1), byteArrayOf(4, 0, 0), syncSafe(frames.size), frames)

    private fun id3v23(frames: ByteArray): ByteArray =
        concat("ID3".toByteArray(StandardCharsets.ISO_8859_1), byteArrayOf(3, 0, 0), syncSafe(frames.size), frames)

    private fun v23Frame(id: String, payload: ByteArray): ByteArray =
        concat(id.toByteArray(StandardCharsets.ISO_8859_1), int32be(payload.size), byteArrayOf(0, 0), payload)

    private fun textFrame(id: String, value: String): ByteArray =
        frame(id, byteArrayOf(3) + value.toByteArray(StandardCharsets.UTF_8))

    private fun frame(id: String, payload: ByteArray): ByteArray =
        concat(id.toByteArray(StandardCharsets.ISO_8859_1), syncSafe(payload.size), byteArrayOf(0, 0), payload)

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

    private fun atom(type: String, payload: ByteArray): ByteArray =
        concat(int32be(payload.size + 8), type.toByteArray(StandardCharsets.ISO_8859_1), payload)

    private fun syncSafe(value: Int): ByteArray = byteArrayOf(
        ((value ushr 21) and 0x7F).toByte(),
        ((value ushr 14) and 0x7F).toByte(),
        ((value ushr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte()
    )

    private fun int32be(value: Int): ByteArray =
        byteArrayOf((value ushr 24).toByte(), (value ushr 16).toByte(), (value ushr 8).toByte(), value.toByte())

    private fun int32le(value: Int): ByteArray =
        byteArrayOf(value.toByte(), (value ushr 8).toByte(), (value ushr 16).toByte(), (value ushr 24).toByte())

    private fun concat(vararg parts: ByteArray): ByteArray =
        ByteArrayOutputStream().apply { parts.forEach { write(it) } }.toByteArray()

    private fun ByteArray.endsWith(suffix: ByteArray): Boolean =
        size >= suffix.size && copyOfRange(size - suffix.size, size).contentEquals(suffix)

    private fun ByteArray.indexOf(needle: ByteArray): Int {
        if (needle.isEmpty() || needle.size > size) return -1
        outer@ for (start in 0..size - needle.size) {
            for (offset in needle.indices) if (this[start + offset] != needle[offset]) continue@outer
            return start
        }
        return -1
    }
}

package com.luc4n3x.levyra.desktop.core.localmusic

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioTagReaderTest {

    @Test
    fun readsId3v23TagsAndVariableBitrateDurationFromMp3() {
        val file = write(
            "song.mp3",
            AudioTagBuilders.mp3(
                frames = linkedMapOf(
                    "TIT2" to "Notturno",
                    "TPE1" to "Ludovico",
                    "TPE2" to "Various Artists",
                    "TALB" to "Nocturnes",
                    "TCON" to "Classical",
                    "TRCK" to "4/12",
                    "TPOS" to "2/2",
                    "TDRC" to "2019"
                ),
                picture = AudioTagBuilders.JPEG_BYTES,
                frameCount = 400
            )
        )

        val tags = AudioTagReader.read(file)

        assertEquals("Notturno", tags.title)
        assertEquals("Ludovico", tags.artist)
        assertEquals("Various Artists", tags.albumArtist)
        assertEquals("Nocturnes", tags.album)
        assertEquals("Classical", tags.genre)
        assertEquals(4, tags.trackNumber)
        assertEquals(2, tags.discNumber)
        assertEquals(2019, tags.year)
        assertEquals(44_100, tags.sampleRateHz)
        assertEquals(2, tags.channels)
        assertEquals("MP3", tags.codec)
        assertEquals(400L * 1152L * 1000L / 44_100L, tags.durationMs)
        assertNotNull(tags.artwork)
        assertEquals("image/jpeg", tags.artwork?.mimeType)
    }

    @Test
    fun readsNumericId3v1GenreReference() {
        val file = write(
            "genre.mp3",
            AudioTagBuilders.mp3(frames = linkedMapOf("TCON" to "(17)"))
        )

        assertEquals("Rock", AudioTagReader.read(file).genre)
    }

    @Test
    fun readsFlacStreamInfoCommentsAndPicture() {
        val file = write(
            "song.flac",
            AudioTagBuilders.flac(
                sampleRate = 96_000,
                channels = 2,
                bitDepth = 24,
                totalSamples = 96_000L * 210L,
                comments = listOf(
                    "TITLE=Aurora",
                    "ARTIST=Kiasmos",
                    "ALBUMARTIST=Kiasmos",
                    "ALBUM=Blurred",
                    "GENRE=Electronic",
                    "DATE=2024-03-01",
                    "TRACKNUMBER=03",
                    "DISCNUMBER=1"
                ),
                picture = AudioTagBuilders.JPEG_BYTES
            )
        )

        val tags = AudioTagReader.read(file)

        assertEquals("Aurora", tags.title)
        assertEquals("Kiasmos", tags.artist)
        assertEquals("Kiasmos", tags.albumArtist)
        assertEquals("Blurred", tags.album)
        assertEquals("Electronic", tags.genre)
        assertEquals(2024, tags.year)
        assertEquals(3, tags.trackNumber)
        assertEquals(1, tags.discNumber)
        assertEquals(96_000, tags.sampleRateHz)
        assertEquals(24, tags.bitDepth)
        assertEquals(2, tags.channels)
        assertEquals("FLAC", tags.codec)
        assertEquals(210_000L, tags.durationMs)
        assertEquals("image/jpeg", tags.artwork?.mimeType)
    }

    @Test
    fun readsWaveFormatAndInfoChunk() {
        val file = write("song.wav", wave())

        val tags = AudioTagReader.read(file)

        assertEquals("Field Recording", tags.title)
        assertEquals("Levyra", tags.artist)
        assertEquals(48_000, tags.sampleRateHz)
        assertEquals(2, tags.channels)
        assertEquals(16, tags.bitDepth)
        assertEquals(1_000L, tags.durationMs)
        assertEquals("PCM", tags.codec)
    }

    @Test
    fun fallsBackToTheFileNameWhenNoTagIsPresent() {
        val file = write("Boards of Canada - Roygbiv.mp3", ByteArray(64))

        val tags = AudioTagReader.read(file)

        assertEquals("Roygbiv", tags.title)
        assertEquals("Boards of Canada", tags.artist)
    }

    @Test
    fun unreadableFileDoesNotThrow() {
        val tags = AudioTagReader.read(Path.of("does-not-exist.mp3"))

        assertEquals("does-not-exist", tags.title)
        assertEquals(0L, tags.durationMs)
    }

    @Test
    fun supportedExtensionsCoverTheCommonOwnedFormats() {
        assertTrue(AudioTagReader.isSupported(Path.of("a.MP3")))
        assertTrue(AudioTagReader.isSupported(Path.of("a.flac")))
        assertTrue(AudioTagReader.isSupported(Path.of("a.opus")))
        assertTrue(!AudioTagReader.isSupported(Path.of("a.txt")))
    }

    private fun wave(): ByteArray {
        val byteRate = 48_000 * 2 * 2
        val dataSize = byteRate
        val info = ByteArrayOutputStream()
        info.write(AudioTagBuilders.ascii("INFO"))
        listOf("INAM" to "Field Recording", "IART" to "Levyra").forEach { (id, value) ->
            val bytes = AudioTagBuilders.ascii(value)
            info.write(AudioTagBuilders.ascii(id))
            info.write(AudioTagBuilders.leInt(bytes.size))
            info.write(bytes)
            if (bytes.size % 2 == 1) info.write(byteArrayOf(0))
        }
        val infoBytes = info.toByteArray()

        val body = ByteArrayOutputStream()
        body.write(AudioTagBuilders.ascii("WAVE"))
        body.write(AudioTagBuilders.ascii("fmt "))
        body.write(AudioTagBuilders.leInt(16))
        body.write(AudioTagBuilders.leShort(1))
        body.write(AudioTagBuilders.leShort(2))
        body.write(AudioTagBuilders.leInt(48_000))
        body.write(AudioTagBuilders.leInt(byteRate))
        body.write(AudioTagBuilders.leShort(4))
        body.write(AudioTagBuilders.leShort(16))
        body.write(AudioTagBuilders.ascii("LIST"))
        body.write(AudioTagBuilders.leInt(infoBytes.size))
        body.write(infoBytes)
        body.write(AudioTagBuilders.ascii("data"))
        body.write(AudioTagBuilders.leInt(dataSize))
        body.write(ByteArray(dataSize))
        val bodyBytes = body.toByteArray()

        val output = ByteArrayOutputStream()
        output.write(AudioTagBuilders.ascii("RIFF"))
        output.write(AudioTagBuilders.leInt(bodyBytes.size))
        output.write(bodyBytes)
        return output.toByteArray()
    }

    private fun write(name: String, bytes: ByteArray): Path {
        val directory = Files.createTempDirectory("levyra-tags")
        directory.toFile().deleteOnExit()
        val file = directory.resolve(name)
        Files.write(file, bytes)
        file.toFile().deleteOnExit()
        return file
    }
}

package com.luc4n3x.levyra.desktop.core.localmusic

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Test

class ContainerTagReaderTest {

    @Test
    fun readsOpusIdentificationCommentsAndDuration() {
        val file = write("song.opus", opus(durationSeconds = 187))

        val tags = AudioTagReader.read(file)

        assertEquals("Sundowner", tags.title)
        assertEquals("Jon Hopkins", tags.artist)
        assertEquals("Ritual", tags.album)
        assertEquals(2, tags.channels)
        assertEquals(48_000, tags.sampleRateHz)
        assertEquals("Opus", tags.codec)
        assertEquals(187_000L, tags.durationMs)
    }

    @Test
    fun readsOpusMetadataBlockPictureWithoutTruncatingIt() {
        val file = write(
            "artwork.opus",
            opus(durationSeconds = 30, picture = AudioTagBuilders.JPEG_BYTES)
        )

        val tags = AudioTagReader.read(file)

        assertEquals("image/jpeg", tags.artwork?.mimeType)
        assertEquals(AudioTagBuilders.JPEG_BYTES.toList(), tags.artwork?.bytes?.toList())
    }

    @Test
    fun expandsTheOggHeaderWindowForLargeEmbeddedArtwork() {
        val largeArtwork = ByteArray(320 * 1024)
        AudioTagBuilders.JPEG_BYTES.copyInto(largeArtwork)
        val file = write(
            "large-artwork.opus",
            opus(durationSeconds = 45, picture = largeArtwork)
        )

        val tags = AudioTagReader.read(file)

        assertEquals("image/jpeg", tags.artwork?.mimeType)
        assertEquals(largeArtwork.size, tags.artwork?.bytes?.size)
        assertEquals(AudioTagBuilders.JPEG_BYTES.toList(), tags.artwork?.bytes?.take(AudioTagBuilders.JPEG_BYTES.size))
    }

    @Test
    fun readsMp4ItemListAndSampleDescription() {
        val file = write("song.m4a", mp4())

        val tags = AudioTagReader.read(file)

        assertEquals("Weightless", tags.title)
        assertEquals("Marconi Union", tags.artist)
        assertEquals("Ambient Works", tags.albumArtist)
        assertEquals("Distance", tags.album)
        assertEquals("Ambient", tags.genre)
        assertEquals(2011, tags.year)
        assertEquals(7, tags.trackNumber)
        assertEquals(1, tags.discNumber)
        assertEquals(2, tags.channels)
        assertEquals(44_100, tags.sampleRateHz)
        assertEquals("AAC", tags.codec)
        assertEquals(495_000L, tags.durationMs)
        assertEquals("image/jpeg", tags.artwork?.mimeType)
    }

    private fun opus(durationSeconds: Int, picture: ByteArray? = null): ByteArray {
        val serial = 0x11223344
        val head = ByteArrayOutputStream()
        head.write(AudioTagBuilders.ascii("OpusHead"))
        head.write(byteArrayOf(1, 2))
        head.write(AudioTagBuilders.leShort(OPUS_PRE_SKIP))
        head.write(AudioTagBuilders.leInt(OPUS_SAMPLE_RATE))
        head.write(AudioTagBuilders.leShort(0))
        head.write(byteArrayOf(0))

        val comments = mutableListOf("TITLE=Sundowner", "ARTIST=Jon Hopkins", "ALBUM=Ritual")
        picture?.let { artwork ->
            val encoded = Base64.getEncoder().encodeToString(AudioTagBuilders.flacPictureBlock(artwork))
            comments += "METADATA_BLOCK_PICTURE=$encoded"
        }
        val tags = ByteArrayOutputStream()
        tags.write(AudioTagBuilders.ascii("OpusTags"))
        val vendor = AudioTagBuilders.ascii("levyra-test")
        tags.write(AudioTagBuilders.leInt(vendor.size))
        tags.write(vendor)
        tags.write(AudioTagBuilders.leInt(comments.size))
        comments.forEach { entry ->
            val bytes = entry.toByteArray(StandardCharsets.UTF_8)
            tags.write(AudioTagBuilders.leInt(bytes.size))
            tags.write(bytes)
        }

        val output = ByteArrayOutputStream()
        var sequence = 0
        output.write(oggPacketPages(serial, sequence, 0L, head.toByteArray()).single())
        sequence += 1
        val tagPages = oggPacketPages(serial, sequence, 0L, tags.toByteArray())
        tagPages.forEach(output::write)
        sequence += tagPages.size
        val finalGranule = OPUS_SAMPLE_RATE.toLong() * durationSeconds + OPUS_PRE_SKIP
        output.write(oggPacketPages(serial, sequence, finalGranule, ByteArray(64)).single())
        return output.toByteArray()
    }

    private fun oggPacketPages(
        serial: Int,
        startSequence: Int,
        granule: Long,
        payload: ByteArray
    ): List<ByteArray> {
        if (payload.isEmpty()) return listOf(oggPage(serial, startSequence, granule, intArrayOf(0), payload))
        val pages = ArrayList<ByteArray>()
        var offset = 0
        var sequence = startSequence
        while (offset < payload.size) {
            val remaining = payload.size - offset
            val take = minOf(remaining, MAX_OGG_PAGE_PAYLOAD)
            val isLast = take == remaining
            val fullSegments = take / 255
            val tail = take % 255
            val segmentLengths = ArrayList<Int>(255)
            repeat(fullSegments) { segmentLengths.add(255) }
            if (isLast) {
                if (tail > 0) {
                    segmentLengths.add(tail)
                } else if (segmentLengths.size < 255) {
                    segmentLengths.add(0)
                }
            }
            val pageGranule = if (isLast) granule else 0L
            val pagePayload = payload.copyOfRange(offset, offset + take)
            pages += oggPage(serial, sequence, pageGranule, segmentLengths.toIntArray(), pagePayload)
            offset += take
            sequence += 1
        }
        return pages
    }

    private fun oggPage(
        serial: Int,
        sequence: Int,
        granule: Long,
        segments: IntArray,
        payload: ByteArray
    ): ByteArray {
        require(segments.size <= 255)
        require(segments.sum() == payload.size)
        val page = ByteArrayOutputStream()
        page.write(AudioTagBuilders.ascii("OggS"))
        page.write(byteArrayOf(0, 0))
        for (index in 0 until 8) {
            page.write(((granule ushr (index * 8)) and 0xFF).toInt())
        }
        page.write(AudioTagBuilders.leInt(serial))
        page.write(AudioTagBuilders.leInt(sequence))
        page.write(AudioTagBuilders.leInt(0))
        page.write(segments.size)
        segments.forEach { page.write(it) }
        page.write(payload)
        return page.toByteArray()
    }

    private fun mp4(): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(atom("ftyp", AudioTagBuilders.ascii("M4A isom")))

        val mvhd = ByteArrayOutputStream()
        mvhd.write(byteArrayOf(0, 0, 0, 0))
        mvhd.write(AudioTagBuilders.beInt(0))
        mvhd.write(AudioTagBuilders.beInt(0))
        mvhd.write(AudioTagBuilders.beInt(1_000))
        mvhd.write(AudioTagBuilders.beInt(495_000))
        mvhd.write(ByteArray(80))

        val mp4a = ByteArrayOutputStream()
        mp4a.write(ByteArray(6))
        mp4a.write(AudioTagBuilders.beInt(1).copyOfRange(2, 4))
        mp4a.write(ByteArray(8))
        mp4a.write(AudioTagBuilders.beInt(2).copyOfRange(2, 4))
        mp4a.write(AudioTagBuilders.beInt(16).copyOfRange(2, 4))
        mp4a.write(ByteArray(4))
        mp4a.write(AudioTagBuilders.beInt(44_100 shl 16))
        val stsd = ByteArrayOutputStream()
        stsd.write(AudioTagBuilders.beInt(0))
        stsd.write(AudioTagBuilders.beInt(1))
        stsd.write(atom("mp4a", mp4a.toByteArray()))

        val ilst = ByteArrayOutputStream()
        ilst.write(item("\u00A9nam", "Weightless"))
        ilst.write(item("\u00A9ART", "Marconi Union"))
        ilst.write(item("aART", "Ambient Works"))
        ilst.write(item("\u00A9alb", "Distance"))
        ilst.write(item("\u00A9gen", "Ambient"))
        ilst.write(item("\u00A9day", "2011"))
        ilst.write(binaryItem("trkn", byteArrayOf(0, 0, 0, 7, 0, 12), 0))
        ilst.write(binaryItem("disk", byteArrayOf(0, 0, 0, 1, 0, 1), 0))
        ilst.write(binaryItem("covr", AudioTagBuilders.JPEG_BYTES, 13))

        val meta = ByteArrayOutputStream()
        meta.write(AudioTagBuilders.beInt(0))
        meta.write(atom("ilst", ilst.toByteArray()))

        val moov = ByteArrayOutputStream()
        moov.write(atom("mvhd", mvhd.toByteArray()))
        moov.write(
            atom("trak", atom("mdia", atom("minf", atom("stbl", atom("stsd", stsd.toByteArray())))))
        )
        moov.write(atom("udta", atom("meta", meta.toByteArray())))
        output.write(atom("moov", moov.toByteArray()))
        return output.toByteArray()
    }

    private fun item(name: String, value: String): ByteArray =
        binaryItem(name, value.toByteArray(StandardCharsets.UTF_8), 1)

    private fun binaryItem(name: String, value: ByteArray, dataType: Int): ByteArray {
        val data = ByteArrayOutputStream()
        data.write(AudioTagBuilders.beInt(dataType))
        data.write(AudioTagBuilders.beInt(0))
        data.write(value)
        return atom(name, atom("data", data.toByteArray()))
    }

    private fun atom(type: String, payload: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(AudioTagBuilders.beInt(payload.size + 8))
        output.write(type.toByteArray(StandardCharsets.ISO_8859_1))
        output.write(payload)
        return output.toByteArray()
    }

    private fun write(name: String, bytes: ByteArray): Path {
        val directory = Files.createTempDirectory("levyra-container")
        directory.toFile().deleteOnExit()
        val file = directory.resolve(name)
        Files.write(file, bytes)
        file.toFile().deleteOnExit()
        return file
    }

    private companion object {
        const val MAX_OGG_PAGE_PAYLOAD = 255 * 255
        const val OPUS_SAMPLE_RATE = 48_000
        const val OPUS_PRE_SKIP = 312
    }
}

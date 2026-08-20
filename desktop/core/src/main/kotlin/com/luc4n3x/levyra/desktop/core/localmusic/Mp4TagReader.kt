package com.luc4n3x.levyra.desktop.core.localmusic

internal object Mp4TagReader {

    private const val MAX_ATOM_BYTES = 24 * 1024 * 1024
    private const val MAX_DEPTH = 8
    private val CONTAINERS = setOf("moov", "udta", "meta", "ilst", "trak", "mdia", "minf", "stbl")

    fun read(file: TagFile): AudioTags {
        val header = file.readAt(0L, 12)
        if (header.size < 12 || !header.startsWithAscii("ftyp", 4)) return AudioTags()
        val collector = Collector()
        walk(file, 0L, file.length, 0, collector)
        return collector.tags
    }

    private class Collector {
        var tags = AudioTags(codec = "AAC")
        var timescale = 0L
        var duration = 0L

        fun finish() {
            if (timescale > 0L && duration > 0L) {
                tags = tags.copy(durationMs = duration * 1000L / timescale)
            }
        }
    }

    private fun walk(file: TagFile, start: Long, end: Long, depth: Int, collector: Collector) {
        if (depth > MAX_DEPTH) return
        var cursor = start
        while (cursor + 8 <= end) {
            val head = file.readAt(cursor, 8)
            if (head.size < 8) return
            var size = head.u32be(0)
            var payloadStart = cursor + 8
            if (size == 1L) {
                val extended = file.readAt(cursor + 8, 8)
                if (extended.size < 8) return
                size = extended.u64be(0)
                payloadStart = cursor + 16
            } else if (size == 0L) {
                size = end - cursor
            }
            if (size < 8L || cursor + size > end) return
            val type = head.latin1(4, 4)
            val payloadEnd = cursor + size
            when {
                type == "mvhd" -> readMovieHeader(file.readAt(payloadStart, 32), collector)
                type == "stsd" -> readSampleDescription(file, payloadStart, payloadEnd, collector)
                type == "ilst" -> readItemList(file, payloadStart, payloadEnd, collector)
                type == "meta" -> walk(file, payloadStart + 4, payloadEnd, depth + 1, collector)
                type in CONTAINERS -> walk(file, payloadStart, payloadEnd, depth + 1, collector)
            }
            cursor = payloadEnd
        }
        if (depth == 0) collector.finish()
    }

    private fun readMovieHeader(payload: ByteArray, collector: Collector) {
        if (payload.size < 24) return
        val version = payload.u8(0)
        if (version == 1) {
            if (payload.size < 32) return
            collector.timescale = payload.u32be(20)
            collector.duration = payload.u64be(24)
        } else {
            collector.timescale = payload.u32be(12)
            collector.duration = payload.u32be(16)
        }
    }

    private fun readSampleDescription(file: TagFile, start: Long, end: Long, collector: Collector) {
        val payload = file.readAt(start, minOf(end - start, 128L).toInt())
        if (payload.size < 44) return
        val format = payload.ascii(12, 4)
        val channels = payload.u16be(32)
        val sampleSize = payload.u16be(34)
        val sampleRate = payload.u16be(40)
        collector.tags = collector.tags.copy(
            channels = if (channels in 1..16) channels else collector.tags.channels,
            bitDepth = if (sampleSize in 8..32) sampleSize else collector.tags.bitDepth,
            sampleRateHz = if (sampleRate > 0) sampleRate else collector.tags.sampleRateHz,
            codec = when (format) {
                "mp4a" -> "AAC"
                "alac" -> "ALAC"
                else -> collector.tags.codec
            }
        )
    }

    private fun readItemList(file: TagFile, start: Long, end: Long, collector: Collector) {
        var cursor = start
        while (cursor + 8 <= end) {
            val head = file.readAt(cursor, 8)
            if (head.size < 8) return
            val size = head.u32be(0)
            if (size < 8L || cursor + size > end) return
            val name = head.latin1(4, 4)
            val payloadSize = (size - 8L).coerceAtMost(MAX_ATOM_BYTES.toLong()).toInt()
            val payload = file.readAt(cursor + 8, payloadSize)
            applyItem(name, payload, collector)
            cursor += size
        }
    }

    private fun applyItem(name: String, payload: ByteArray, collector: Collector) {
        if (payload.size < 16 || !payload.startsWithAscii("data", 4)) return
        val dataType = payload.u32be(8).toInt()
        val body = payload.copyOfRange(16, payload.size)
        val tags = collector.tags
        collector.tags = when (name) {
            "\u00A9nam" -> tags.copy(title = tags.title.ifBlank { text(body) })
            "\u00A9ART" -> tags.copy(artist = tags.artist.ifBlank { text(body) })
            "aART" -> tags.copy(albumArtist = tags.albumArtist.ifBlank { text(body) })
            "\u00A9alb" -> tags.copy(album = tags.album.ifBlank { text(body) })
            "\u00A9gen" -> tags.copy(genre = tags.genre.ifBlank { text(body) })
            "\u00A9day" -> tags.copy(year = if (tags.year > 0) tags.year else TagText.year(text(body)))
            "trkn" -> tags.copy(
                trackNumber = if (tags.trackNumber > 0) tags.trackNumber else pairValue(body)
            )
            "disk" -> tags.copy(
                discNumber = if (tags.discNumber > 0) tags.discNumber else pairValue(body)
            )
            "covr" -> tags.copy(artwork = tags.artwork ?: picture(body, dataType))
            else -> tags
        }
    }

    private fun text(body: ByteArray): String = TagText.clean(body.utf8(0, body.size))

    private fun pairValue(body: ByteArray): Int =
        if (body.size >= 4) body.u16be(2) else 0

    private fun picture(body: ByteArray, dataType: Int): EmbeddedArtwork? {
        if (body.isEmpty()) return null
        val declared = when (dataType) {
            13 -> "image/jpeg"
            14 -> "image/png"
            else -> ""
        }
        val mime = TagText.imageMimeType(body, declared)
        if (mime.isEmpty()) return null
        return EmbeddedArtwork(body, mime)
    }
}

package com.luc4n3x.levyra.desktop.core.localmusic

internal object OggTagReader {

    private const val HEADER_WINDOW = 256 * 1024
    private const val EXTENDED_HEADER_WINDOW = 10 * 1024 * 1024
    private const val TAIL_WINDOW = 64 * 1024
    private const val OPUS_SAMPLE_RATE = 48_000

    fun read(file: TagFile): AudioTags {
        var head = file.readAt(0L, HEADER_WINDOW)
        if (!head.startsWithAscii("OggS")) return AudioTags()
        val serial = head.u32le(14)
        var packets = collectPackets(head, serial, limit = 3)
        if (packets.size < 2 && file.length > head.size) {
            val extendedSize = minOf(file.length, EXTENDED_HEADER_WINDOW.toLong()).toInt()
            if (extendedSize > head.size) {
                head = file.readAt(0L, extendedSize)
                packets = collectPackets(head, serial, limit = 3)
            }
        }
        if (packets.isEmpty()) return AudioTags()

        val identification = packets[0]
        var tags = when {
            identification.startsWithAscii("OpusHead") -> opusIdentification(identification)
            identification.size > 7 && identification.startsWithAscii("vorbis", 1) ->
                vorbisIdentification(identification)
            else -> return AudioTags()
        }
        val comment = packets.getOrNull(1)
        if (comment != null) {
            tags = when {
                comment.startsWithAscii("OpusTags") -> tags.mergedWith(VorbisComment.parse(comment, 8))
                comment.size > 7 && comment.startsWithAscii("vorbis", 1) ->
                    tags.mergedWith(VorbisComment.parse(comment, 7))
                else -> tags
            }
        }
        val granule = lastGranulePosition(file, serial)
        if (granule > 0L && tags.sampleRateHz > 0) {
            val rate = if (tags.codec == "Opus") OPUS_SAMPLE_RATE else tags.sampleRateHz
            tags = tags.copy(durationMs = granule * 1000L / rate)
        }
        if (tags.durationMs > 0L) {
            val bitrate = (file.length * 8L / tags.durationMs).toInt()
            tags = tags.copy(bitrateKbps = bitrate.coerceIn(0, 5_000))
        }
        return tags
    }

    private fun opusIdentification(packet: ByteArray): AudioTags {
        if (packet.size < 19) return AudioTags()
        return AudioTags(
            sampleRateHz = OPUS_SAMPLE_RATE,
            channels = packet.u8(9),
            codec = "Opus"
        )
    }

    private fun vorbisIdentification(packet: ByteArray): AudioTags {
        if (packet.size < 16) return AudioTags()
        val channels = packet.u8(11)
        val sampleRate = packet.u32le(12).toInt()
        if (sampleRate <= 0) return AudioTags()
        return AudioTags(
            sampleRateHz = sampleRate,
            channels = channels,
            codec = "Vorbis"
        )
    }

    private fun collectPackets(window: ByteArray, serial: Long, limit: Int): List<ByteArray> {
        val packets = ArrayList<ByteArray>(limit)
        val current = java.io.ByteArrayOutputStream()
        var cursor = 0
        while (cursor + 27 <= window.size && packets.size < limit) {
            if (!window.startsWithAscii("OggS", cursor)) break
            val segmentCount = window.u8(cursor + 26)
            val tableOffset = cursor + 27
            if (tableOffset + segmentCount > window.size) break
            val pageSerial = window.u32le(cursor + 14)
            var payloadOffset = tableOffset + segmentCount
            for (index in 0 until segmentCount) {
                val length = window.u8(tableOffset + index)
                if (payloadOffset + length > window.size) return packets
                if (pageSerial == serial) {
                    current.write(window, payloadOffset, length)
                }
                payloadOffset += length
                if (pageSerial == serial && length < 255) {
                    if (current.size() > 0) {
                        packets.add(current.toByteArray())
                        current.reset()
                    }
                    if (packets.size >= limit) return packets
                }
            }
            cursor = payloadOffset
        }
        return packets
    }

    private fun lastGranulePosition(file: TagFile, serial: Long): Long {
        val start = (file.length - TAIL_WINDOW).coerceAtLeast(0L)
        val tail = file.readAt(start, TAIL_WINDOW)
        var granule = 0L
        var index = 0
        while (index + 27 <= tail.size) {
            if (tail.startsWithAscii("OggS", index) && tail.u32le(index + 14) == serial) {
                granule = tail.u64le(index + 6)
                index += 27
            } else {
                index += 1
            }
        }
        return granule
    }
}

internal fun ByteArray.u64le(offset: Int): Long {
    var value = 0L
    for (index in 7 downTo 0) {
        value = (value shl 8) or u8(offset + index).toLong()
    }
    return value
}

package com.luc4n3x.levyra.desktop.core.localmusic

internal object FlacTagReader {

    private const val MAX_BLOCK_BYTES = 8 * 1024 * 1024
    private const val MAX_BLOCKS = 64

    fun read(file: TagFile, startOffset: Long): AudioTags {
        val magic = file.readAt(startOffset, 4)
        if (magic.size < 4 || !magic.startsWithAscii("fLaC")) return AudioTags()
        var cursor = startOffset + 4
        var tags = AudioTags(codec = "FLAC")
        var blocks = 0
        while (blocks < MAX_BLOCKS && cursor + 4 <= file.length) {
            val header = file.readAt(cursor, 4)
            if (header.size < 4) break
            val last = header.u8(0) and 0x80 != 0
            val type = header.u8(0) and 0x7F
            val size = header.u24be(1)
            cursor += 4
            if (size < 0 || size > MAX_BLOCK_BYTES || cursor + size > file.length) break
            when (type) {
                0 -> tags = tags.mergedWith(streamInfo(file.readAt(cursor, size)))
                4 -> tags = tags.mergedWith(VorbisComment.parse(file.readAt(cursor, size)))
                6 -> if (tags.artwork == null) {
                    tags = tags.copy(artwork = FlacPicture.parse(file.readAt(cursor, size)))
                }
            }
            cursor += size
            blocks += 1
            if (last) break
        }
        if (tags.durationMs > 0L) {
            val audioBytes = (file.length - cursor).coerceAtLeast(0L)
            val bitrate = (audioBytes * 8L / tags.durationMs).toInt()
            tags = tags.copy(bitrateKbps = bitrate.coerceIn(0, 20_000))
        }
        return tags
    }

    private fun streamInfo(block: ByteArray): AudioTags {
        if (block.size < 18) return AudioTags()
        val packed = block.u64be(10)
        val sampleRate = (packed ushr 44).toInt() and 0xFFFFF
        val channels = ((packed ushr 41).toInt() and 0x07) + 1
        val bitDepth = ((packed ushr 36).toInt() and 0x1F) + 1
        val totalSamples = packed and 0xFFFFFFFFFL
        if (sampleRate <= 0) return AudioTags()
        return AudioTags(
            durationMs = totalSamples * 1000L / sampleRate,
            sampleRateHz = sampleRate,
            channels = channels,
            bitDepth = bitDepth,
            codec = "FLAC"
        )
    }
}

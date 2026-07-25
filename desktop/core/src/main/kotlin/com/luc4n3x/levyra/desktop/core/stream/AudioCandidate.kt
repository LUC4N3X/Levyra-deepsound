package com.luc4n3x.levyra.desktop.core.stream

data class AudioCandidate(
    val url: String,
    val mimeType: String = "",
    val suffix: String = "",
    val itag: Int = -1,
    val averageBitrate: Int = -1
) {
    val isOpus: Boolean
        get() = mimeType.contains("opus", ignoreCase = true) ||
            mimeType.contains("webm", ignoreCase = true) ||
            suffix.equals("opus", ignoreCase = true) ||
            suffix.equals("webm", ignoreCase = true) ||
            itag in OPUS_ITAGS

    val isAac: Boolean
        get() = mimeType.contains("mp4", ignoreCase = true) ||
            mimeType.contains("m4a", ignoreCase = true) ||
            suffix.equals("m4a", ignoreCase = true) ||
            suffix.equals("mp4", ignoreCase = true) ||
            itag in AAC_ITAGS

    val label: String
        get() {
            val codec = when {
                isOpus -> "Opus"
                isAac -> "AAC"
                else -> suffix.uppercase().ifBlank { "Audio" }
            }
            val bitrate = averageBitrate.takeIf { it > 0 }?.let { "$it kbps" }.orEmpty()
            val tag = itag.takeIf { it > 0 }?.let { "itag $it" }.orEmpty()
            return listOf(codec, bitrate, tag).filter { it.isNotBlank() }.joinToString(" · ")
        }

    private companion object {
        val OPUS_ITAGS = setOf(249, 250, 251, 338)
        val AAC_ITAGS = setOf(139, 140, 141, 256, 258)
    }
}

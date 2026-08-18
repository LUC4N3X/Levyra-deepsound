package com.luc4n3x.levyra.feature.recognition

object RecognitionSearchQuery {
    const val MAX_QUERY_LENGTH = 100

    fun from(result: RecognitionResult): String {
        val title = sanitize(result.title)
        val artist = sanitize(result.artist)
        val combined = listOf(title, artist).filter { it.isNotBlank() }.joinToString(" ")
        return combined.take(MAX_QUERY_LENGTH).trim()
    }

    private fun sanitize(value: String): String {
        val builder = StringBuilder()
        var lastWasSeparator = true
        for (char in value) {
            val isSeparator = char.isWhitespace() || char.isISOControl()
            if (isSeparator) {
                if (!lastWasSeparator) {
                    builder.append(' ')
                    lastWasSeparator = true
                }
            } else {
                builder.append(char)
                lastWasSeparator = false
            }
        }
        return builder.toString().trim()
    }
}

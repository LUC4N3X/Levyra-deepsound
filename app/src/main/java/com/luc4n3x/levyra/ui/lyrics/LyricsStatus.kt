package com.luc4n3x.levyra.ui.lyrics

private val PROVIDER_SEPARATORS = arrayOf(" · ", " • ", " - ", " | ")
private const val PROVIDER_ALLOWED_PUNCTUATION = "+&.'-"
private val MULTI_SPACE = Regex(" {2,}")

internal fun lyricsProviderDisplayName(provider: String): String {
    val primary = provider.split(*PROVIDER_SEPARATORS).firstOrNull().orEmpty()
    val cleaned = buildString {
        var index = 0
        while (index < primary.length) {
            val codePoint = primary.codePointAt(index)
            if (Character.isLetterOrDigit(codePoint) ||
                codePoint == ' '.code ||
                (codePoint < Char.MAX_VALUE.code && codePoint.toChar() in PROVIDER_ALLOWED_PUNCTUATION)
            ) {
                appendCodePoint(codePoint)
            }
            index += Character.charCount(codePoint)
        }
    }
    return cleaned.trim().replace(MULTI_SPACE, " ")
}

internal fun lyricsShouldFollowActiveLine(
    autoScrollEnabled: Boolean,
    selectionMode: Boolean
): Boolean = autoScrollEnabled && !selectionMode

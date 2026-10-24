package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.nexus.identity.MusicIdentityText

private val EmbeddedTitleSeparator = Regex("\\s+[-–—]\\s+")

internal fun embeddedTitleArtistCandidates(title: String, artist: String): List<Pair<String, String>> {
    val parts = title.split(EmbeddedTitleSeparator)
    if (parts.size != 2) return emptyList()
    val left = parts[0].trim()
    val right = parts[1].trim()
    if (left.length < 2 || right.length < 2) return emptyList()
    if (MusicIdentityText.title("$left - $right").core == MusicIdentityText.title(left).core) return emptyList()
    val channel = LyricsMatcher.normalize(artist)
    val normalizedLeft = LyricsMatcher.normalize(left)
    val normalizedRight = LyricsMatcher.normalize(right)
    val leftNamesChannel = channel.isNotBlank() && normalizedLeft.contains(channel)
    val rightNamesChannel = channel.isNotBlank() && normalizedRight.contains(channel)
    return when {
        leftNamesChannel && rightNamesChannel -> listOf(left to right, right to left)
        leftNamesChannel -> listOf(right to left)
        rightNamesChannel -> listOf(left to right)
        else -> listOf(left to right, right to left)
    }
}

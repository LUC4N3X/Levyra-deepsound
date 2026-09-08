package com.luc4n3x.levyra.desktop.core.artwork

import java.net.URI
import java.nio.file.Path
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

sealed interface ArtworkSource {

    val cacheKey: String

    data class Remote(val url: String) : ArtworkSource {
        override val cacheKey: String get() = url
    }

    data class LocalFile(val path: Path) : ArtworkSource {
        override val cacheKey: String get() = path.toString()
    }
}

object ArtworkSources {

    fun of(reference: String): ArtworkSource? {
        val trimmed = reference.trim()
        if (trimmed.isEmpty()) return null
        if (isWebReference(trimmed)) {
            return trimmed.toHttpUrlOrNull()?.let { url -> ArtworkSource.Remote(url.toString()) }
        }
        return localPathOf(trimmed)?.let(ArtworkSource::LocalFile)
    }

    fun isWebReference(reference: String): Boolean =
        reference.startsWith("http://", ignoreCase = true) ||
            reference.startsWith("https://", ignoreCase = true)

    private fun localPathOf(reference: String): Path? {
        if (reference.startsWith(FILE_SCHEME, ignoreCase = true)) {
            return runCatching { Path.of(URI(reference)) }.getOrNull()
        }
        if (hasUriScheme(reference)) return null
        return runCatching { Path.of(reference) }.getOrNull()
    }

    private fun hasUriScheme(reference: String): Boolean {
        val separator = reference.indexOf(':')
        if (separator < MIN_SCHEME_LENGTH) return false
        if (!reference[0].isLetter()) return false
        return (1 until separator).all { position ->
            val character = reference[position]
            character.isLetterOrDigit() || character == '+' || character == '-' || character == '.'
        }
    }

    private const val FILE_SCHEME = "file:"
    private const val MIN_SCHEME_LENGTH = 2
}

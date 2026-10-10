package com.luc4n3x.levyra.desktop.core.localmusic

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

data class LocalMediaSource(
    val url: String,
    val label: String
)

fun resolveLocalFile(raw: String): Path? {
    if (raw.isBlank()) return null
    val trimmed = raw.trim()
    val direct = runCatching { Path.of(trimmed) }.getOrNull()
    if (direct != null && Files.isRegularFile(direct)) return direct

    if (trimmed.startsWith("file:", ignoreCase = true)) {
        val uri = runCatching { URI(trimmed) }.getOrNull()
        if (uri != null) {
            val fromUri = runCatching { Path.of(uri) }.getOrNull()
            if (fromUri != null && Files.isRegularFile(fromUri)) return fromUri
        }
        val encoded = runCatching { URI(trimmed.replace(" ", "%20")) }.getOrNull()
        if (encoded != null) {
            val fromEncoded = runCatching { Path.of(encoded) }.getOrNull()
            if (fromEncoded != null && Files.isRegularFile(fromEncoded)) return fromEncoded
        }
        val stripped = trimmed.replaceFirst(Regex("^file:/*", RegexOption.IGNORE_CASE), "")
        val fromStripped = runCatching { Path.of(stripped) }.getOrNull()
        if (fromStripped != null && Files.isRegularFile(fromStripped)) return fromStripped

        val decoded = runCatching { URLDecoder.decode(stripped, StandardCharsets.UTF_8) }.getOrNull()
        if (decoded != null) {
            val fromDecoded = runCatching { Path.of(decoded) }.getOrNull()
            if (fromDecoded != null && Files.isRegularFile(fromDecoded)) return fromDecoded
        }
    }

    val decodedRaw = runCatching { URLDecoder.decode(trimmed, StandardCharsets.UTF_8) }.getOrNull()
    if (decodedRaw != null && decodedRaw != trimmed) {
        val fromDecoded = runCatching { Path.of(decodedRaw) }.getOrNull()
        if (fromDecoded != null && Files.isRegularFile(fromDecoded)) return fromDecoded
    }

    return null
}

fun matchesMediaUrl(a: String, b: String): Boolean {
    if (a.equals(b, ignoreCase = true)) return true
    val normA = normalizeMediaMrl(a)
    val normB = normalizeMediaMrl(b)
    return normA.isNotEmpty() && normA.equals(normB, ignoreCase = true)
}

fun normalizeMediaMrl(raw: String): String {
    val decoded = runCatching { URLDecoder.decode(raw, StandardCharsets.UTF_8) }.getOrDefault(raw)
    return decoded
        .replace('\\', '/')
        .replaceFirst(Regex("^file:/*", RegexOption.IGNORE_CASE), "")
        .trimEnd('/')
}

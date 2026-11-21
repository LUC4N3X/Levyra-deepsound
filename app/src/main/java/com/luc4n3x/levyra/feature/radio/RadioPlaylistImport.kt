package com.luc4n3x.levyra.feature.radio

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal data class RadioPlaylistEntry(val name: String, val streamUrl: String, val imageUrl: String)

internal data class RadioImportResult(val added: Int, val skipped: Int)

internal fun parseRadioPlaylist(text: String): List<RadioPlaylistEntry> {
    val lines = text.removePrefix("﻿").lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
    if (lines.any { it.startsWith("#EXT-X-", ignoreCase = true) }) return emptyList()
    val entries = mutableListOf<RadioPlaylistEntry>()
    var pendingName = ""
    var pendingImage = ""
    for (line in lines) {
        if (line.startsWith("#EXTINF", ignoreCase = true)) {
            pendingName = line.substringAfter(',', "").trim()
            pendingImage = extinfAttribute(line, "tvg-logo")
            continue
        }
        if (line.startsWith("#")) continue
        val stream = canonicalRadioUrl(line)
        if (stream != null) {
            val image = pendingImage.takeIf(String::isNotBlank)?.let(::canonicalRadioUrl).orEmpty()
            entries += RadioPlaylistEntry(radioEntryName(pendingName, stream), stream, image)
        }
        pendingName = ""
        pendingImage = ""
    }
    return entries
}

private fun extinfAttribute(line: String, key: String): String =
    Regex("""$key\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE).find(line.substringBefore(','))?.groupValues?.get(1)?.trim().orEmpty()

private fun radioEntryName(name: String, stream: String): String {
    val clean = name.filterNot(Char::isISOControl).trim().take(160)
    return clean.ifBlank { stream.toHttpUrlOrNull()?.host.orEmpty().ifBlank { stream.take(160) } }
}

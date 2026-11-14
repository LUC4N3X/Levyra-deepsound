package com.luc4n3x.levyra.desktop.app.state

import java.io.IOException

private val serialContentRange = Regex("bytes (\\d+)-(\\d+)/(\\d+)", RegexOption.IGNORE_CASE)

internal fun serialDownloadLength(code: Int, contentRange: String?, bodyLength: Long, offset: Long): Long {
    if (code == 200) return bodyLength.coerceAtLeast(0L)
    if (code != 206) throw IOException("Download non disponibile: HTTP $code")
    val match = contentRange?.trim()?.let(serialContentRange::matchEntire)
        ?: throw IOException("Content-Range non valido")
    val start = match.groupValues[1].toLongOrNull()
    val end = match.groupValues[2].toLongOrNull()
    val total = match.groupValues[3].toLongOrNull()
    if (start != offset || end == null || total == null || total <= 0L || end < offset ||
        end != total - 1L || bodyLength >= 0L && bodyLength != total - offset
    ) throw IOException("Content-Range non corrispondente al file parziale")
    return total
}

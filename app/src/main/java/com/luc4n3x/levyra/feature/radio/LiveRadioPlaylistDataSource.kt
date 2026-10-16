package com.luc4n3x.levyra.feature.radio

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException
import java.net.URI
import java.util.Locale

internal const val LIVE_RADIO_PLAYLIST_MAX_BYTES = 64 * 1024
internal const val LIVE_RADIO_PLAYLIST_MAX_DEPTH = 3

private val liveRadioPlaylistContentTypes = setOf(
    "audio/x-scpls",
    "audio/scpls",
    "application/pls+xml",
    "audio/x-mpegurl",
    "audio/mpegurl",
    "application/x-mpegurl"
)

private val liveRadioPlaylistKeyValue = Regex("""^([A-Za-z]+)\d*\s*=\s*(.*)$""")

internal fun isResolvableRadioPlaylistUrl(value: String): Boolean {
    val path = runCatching { URI(value.trim()).path }.getOrNull().orEmpty().lowercase(Locale.ROOT)
    return path.endsWith(".pls") || path.endsWith(".m3u")
}

internal fun isLiveRadioPlaylistResponse(url: String, contentType: String?): Boolean {
    val path = runCatching { URI(url.trim()).path }.getOrNull().orEmpty().lowercase(Locale.ROOT)
    if (path.endsWith(".m3u8")) return false
    val type = contentType?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT).orEmpty()
    if (type in liveRadioPlaylistContentTypes) return true
    if (type.startsWith("audio/") || type.startsWith("video/") || type == "application/ogg") return false
    return isResolvableRadioPlaylistUrl(url)
}

internal fun liveRadioPlaylistStreamUrl(body: String, baseUrl: String): String? {
    if (body.contains("#EXT-X-", ignoreCase = true)) return null
    val base = runCatching { URI(baseUrl.trim()) }.getOrNull() ?: return null
    return body.lineSequence()
        .map { it.trim().removePrefix("\uFEFF") }
        .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("[") }
        .mapNotNull { line ->
            val entry = liveRadioPlaylistKeyValue.matchEntire(line)
            when {
                entry == null -> line
                entry.groupValues[1].equals("file", ignoreCase = true) -> entry.groupValues[2].trim()
                else -> null
            }
        }
        .mapNotNull { candidate -> runCatching { base.resolve(candidate).toString() }.getOrNull() }
        .firstOrNull { it != baseUrl && RadioUrlPolicy.isAllowed(it) }
}

@UnstableApi
internal class LiveRadioPlaylistDataSource(private val upstream: DataSource) : DataSource {

    class Factory(private val upstreamFactory: DataSource.Factory) : DataSource.Factory {
        override fun createDataSource(): DataSource = LiveRadioPlaylistDataSource(upstreamFactory.createDataSource())
    }

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        if (dataSpec.position != 0L) return upstream.open(dataSpec)
        var spec = dataSpec
        repeat(LIVE_RADIO_PLAYLIST_MAX_DEPTH) {
            val length = upstream.open(spec)
            val url = upstream.uri?.toString() ?: spec.uri.toString()
            if (!isLiveRadioPlaylistResponse(url, contentTypeOf(upstream.responseHeaders))) return length
            val body = readBounded()
            upstream.close()
            val next = liveRadioPlaylistStreamUrl(body, url)
                ?: throw IOException("Live radio playlist has no playable entry")
            spec = spec.buildUpon()
                .setUri(Uri.parse(next))
                .setPosition(0L)
                .setLength(C.LENGTH_UNSET.toLong())
                .build()
        }
        throw IOException("Live radio playlist nesting exceeds $LIVE_RADIO_PLAYLIST_MAX_DEPTH levels")
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = upstream.read(buffer, offset, length)

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        upstream.close()
    }

    private fun readBounded(): String {
        val buffer = ByteArray(LIVE_RADIO_PLAYLIST_MAX_BYTES)
        var total = 0
        while (total < buffer.size) {
            val read = upstream.read(buffer, total, buffer.size - total)
            if (read == C.RESULT_END_OF_INPUT) break
            total += read
        }
        return String(buffer, 0, total, Charsets.UTF_8)
    }

    private fun contentTypeOf(headers: Map<String, List<String>>): String? =
        headers.entries.firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }?.value?.firstOrNull()
}

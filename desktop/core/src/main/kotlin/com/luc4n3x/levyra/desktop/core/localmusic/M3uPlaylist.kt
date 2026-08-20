package com.luc4n3x.levyra.desktop.core.localmusic

import com.luc4n3x.levyra.desktop.core.model.Track
import java.net.URI
import java.nio.file.Path

data class M3uEntry(
    val location: String,
    val title: String = "",
    val artist: String = "",
    val durationMs: Long = 0L
) {
    val isRemote: Boolean
        get() = location.startsWith("http://", ignoreCase = true) ||
            location.startsWith("https://", ignoreCase = true)
}

object M3uPlaylist {

    const val EXTENSION = "m3u8"
    private const val MAX_ENTRIES = 20_000

    fun parse(content: String): List<M3uEntry> {
        val entries = ArrayList<M3uEntry>()
        var pendingTitle = ""
        var pendingArtist = ""
        var pendingDurationMs = 0L
        content.lineSequence().forEach { rawLine ->
            if (entries.size >= MAX_ENTRIES) return entries
            val line = rawLine.trim()
            when {
                line.isEmpty() -> Unit

                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    val payload = line.substringAfter(':')
                    val seconds = payload.substringBefore(',').trim().substringBefore('.')
                    pendingDurationMs = (seconds.toLongOrNull() ?: 0L).coerceAtLeast(0L) * 1000L
                    val label = payload.substringAfter(',', "").trim()
                    val separator = label.indexOf(" - ")
                    if (separator > 0) {
                        pendingArtist = label.substring(0, separator).trim()
                        pendingTitle = label.substring(separator + 3).trim()
                    } else {
                        pendingArtist = ""
                        pendingTitle = label
                    }
                }

                line.startsWith("#") -> Unit

                else -> {
                    entries.add(
                        M3uEntry(
                            location = line,
                            title = pendingTitle,
                            artist = pendingArtist,
                            durationMs = pendingDurationMs
                        )
                    )
                    pendingTitle = ""
                    pendingArtist = ""
                    pendingDurationMs = 0L
                }
            }
        }
        return entries
    }

    fun resolve(entry: M3uEntry, baseDirectory: Path?): Path? {
        if (entry.isRemote) return null
        val location = entry.location.trim().replace('\\', '/')
        if (location.isEmpty()) return null
        val hostPath = location.replace('/', java.io.File.separatorChar)
        val candidate = runCatching { Path.of(hostPath) }.getOrNull() ?: return null
        val resolved = if (candidate.isAbsolute || isWindowsAbsolute(location) || baseDirectory == null) {
            candidate
        } else {
            baseDirectory.resolve(candidate)
        }
        return runCatching { resolved.normalize() }.getOrNull()
    }

    private fun isWindowsAbsolute(location: String): Boolean =
        location.startsWith("//") ||
            (location.length >= 3 && location[0].isLetter() && location[1] == ':' && location[2] == '/')

    fun youtubeVideoId(entry: M3uEntry): String {
        if (!entry.isRemote) return ""
        val uri = runCatching { URI(entry.location.trim()) }.getOrNull() ?: return ""
        if (!uri.scheme.equals("https", ignoreCase = true) && !uri.scheme.equals("http", ignoreCase = true)) {
            return ""
        }
        val host = uri.host.orEmpty().lowercase()
        if (host !in YOUTUBE_HOSTS) return ""
        return Track.videoIdOf(entry.location).takeIf { YOUTUBE_VIDEO_ID.matches(it) }.orEmpty()
    }

    fun render(name: String, tracks: List<Track>): String = buildString {
        append("#EXTM3U")
        append(LINE_SEPARATOR)
        append("#PLAYLIST:")
        append(name)
        append(LINE_SEPARATOR)
        tracks.forEach { track ->
            val location = track.offlinePath.ifBlank { track.videoUrl }
            if (location.isBlank()) return@forEach
            append("#EXTINF:")
            append(track.durationMs / 1000L)
            append(',')
            append(displayLabel(track))
            append(LINE_SEPARATOR)
            append(location)
            append(LINE_SEPARATOR)
        }
    }

    private fun displayLabel(track: Track): String =
        if (track.artist.isBlank()) track.title else "${track.artist} - ${track.title}"

    private const val LINE_SEPARATOR = "\n"
    private val YOUTUBE_HOSTS = setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "music.youtube.com",
        "youtu.be",
        "youtube-nocookie.com",
        "www.youtube-nocookie.com"
    )
    private val YOUTUBE_VIDEO_ID = Regex("[A-Za-z0-9_-]{11}")
}

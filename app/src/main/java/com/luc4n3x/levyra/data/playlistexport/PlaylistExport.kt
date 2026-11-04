package com.luc4n3x.levyra.data.playlistexport

import com.luc4n3x.levyra.domain.Track
import java.net.URLEncoder

enum class PlaylistFileFormat(val extension: String, val mimeType: String) {
    M3U8("m3u8", "audio/x-mpegurl"),
    CSV("csv", "text/csv")
}

data class RemotePlaylistExportPlan(
    val videoIds: List<String>,
    val unmatched: List<Track>,
    val duplicates: Int
)

interface RemotePlaylistExportProgress {
    fun onCreated(url: String)
    fun onAdded(added: Int)
}

interface RemotePlaylistExporter {
    suspend fun export(title: String, videoIds: List<String>, progress: RemotePlaylistExportProgress)
}

private val youtubeVideoId = Regex("^[A-Za-z0-9_-]{11}$")

fun Track.youtubeVideoIdOrNull(): String? =
    listOf(id, audioVideoId).firstOrNull(youtubeVideoId::matches)

fun planRemotePlaylistExport(tracks: List<Track>): RemotePlaylistExportPlan {
    val seen = LinkedHashSet<String>()
    val unmatched = ArrayList<Track>()
    var duplicates = 0
    tracks.forEach { track ->
        val videoId = track.youtubeVideoIdOrNull()
        when {
            videoId == null -> unmatched += track
            !seen.add(videoId) -> duplicates++
        }
    }
    return RemotePlaylistExportPlan(seen.toList(), unmatched, duplicates)
}

fun playlistFileName(title: String, format: PlaylistFileFormat): String {
    val base = title.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), " ").trim().replace(Regex("\\s+"), " ").take(80)
    return "${base.ifBlank { "Levyra playlist" }}.${format.extension}"
}

fun writePlaylistFile(title: String, tracks: List<Track>, format: PlaylistFileFormat): String = when (format) {
    PlaylistFileFormat.M3U8 -> buildString {
        append("#EXTM3U\n")
        append("#PLAYLIST:").append(title.singleLine()).append('\n')
        tracks.forEach { track ->
            val seconds = if (track.durationMs > 0) track.durationMs / 1000 else -1
            val display = listOf(track.artist.singleLine(), track.title.singleLine()).filter(String::isNotBlank).joinToString(" - ")
            append("#EXTINF:").append(seconds).append(',').append(display).append('\n')
            if (track.artist.isNotBlank()) append("#EXTART:").append(track.artist.singleLine()).append('\n')
            if (track.album.isNotBlank()) append("#EXTALB:").append(track.album.singleLine()).append('\n')
            append(track.portableUrl()).append('\n')
        }
    }
    PlaylistFileFormat.CSV -> buildString {
        append("Title,Artist,Album,Duration (ms),ISRC,Video ID,URL\r\n")
        tracks.forEach { track ->
            listOf(
                track.title,
                track.artist,
                track.album,
                track.durationMs.coerceAtLeast(0L).toString(),
                track.isrc,
                track.youtubeVideoIdOrNull().orEmpty(),
                track.portableUrl()
            ).joinTo(this, ",") { it.csvField() }
            append("\r\n")
        }
    }
}

private fun Track.portableUrl(): String {
    youtubeVideoIdOrNull()?.let { return "https://music.youtube.com/watch?v=$it" }
    val query = listOf(artist, title).filter(String::isNotBlank).joinToString(" ")
    return "https://music.youtube.com/search?q=" + URLEncoder.encode(query, "UTF-8")
}

private fun String.singleLine(): String = replace(Regex("[\\r\\n]+"), " ").trim()

private fun String.csvField(): String {
    val clean = singleLine()
    val guarded = if (clean.firstOrNull() in setOf('=', '+', '-', '@')) "'$clean" else clean
    return if (guarded.any { it == ',' || it == '"' }) "\"" + guarded.replace("\"", "\"\"") + "\"" else guarded
}

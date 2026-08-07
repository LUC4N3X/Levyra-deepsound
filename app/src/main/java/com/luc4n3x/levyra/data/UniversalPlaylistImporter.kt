package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.net.URLDecoder

sealed class PlaylistImportResult {
    data class Success(val playlist: Playlist, val importedCount: Int) : PlaylistImportResult()
    data class Failure(val reason: String) : PlaylistImportResult()
}

class UniversalPlaylistImporter(
    context: Context,
    private val playlistStore: PlaylistStore = PlaylistStore(context),
    private val youtubeRepository: YoutubeMusicRepository = YoutubeMusicRepository(context)
) {

    suspend fun importFromUrlOrJson(input: String, customName: String? = null): PlaylistImportResult = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return@withContext PlaylistImportResult.Failure("Input vuoto")

        runCatching {
            when {
                trimmed.startsWith("{") || trimmed.startsWith("[") -> importFromJson(trimmed, customName)
                trimmed.contains("music.youtube.com") || trimmed.contains("youtube.com") -> importFromYoutubeUrl(trimmed, customName)
                trimmed.contains("spotify.com") -> importFromSpotifyUrl(trimmed, customName)
                else -> PlaylistImportResult.Failure("Formato o URL non supportato")
            }
        }.getOrElse { e ->
            Timber.w(e, "Playlist import failed")
            PlaylistImportResult.Failure(e.message ?: "Errore durante l'importazione")
        }
    }

    private suspend fun importFromYoutubeUrl(url: String, customName: String?): PlaylistImportResult {
        val listId = extractQueryParam(url, "list")
        if (listId.isBlank()) return PlaylistImportResult.Failure("ID Playlist YouTube non trovato nel link")

        val fetchedPlaylist = youtubeRepository.playlist(listId)
            ?: return PlaylistImportResult.Failure("Impossibile recuperare la playlist da YouTube")

        if (fetchedPlaylist.tracks.isEmpty()) {
            return PlaylistImportResult.Failure("La playlist YouTube non contiene tracce o non è pubblica")
        }

        val name = customName?.ifBlank { null } ?: fetchedPlaylist.name.ifBlank { "YouTube Playlist" }
        val created = playlistStore.create(name, fetchedPlaylist.tracks.firstOrNull())
        if (fetchedPlaylist.tracks.size > 1) {
            playlistStore.addTracks(created.id, fetchedPlaylist.tracks.drop(1))
        }

        val updated = playlistStore.load(created.id) ?: created
        return PlaylistImportResult.Success(updated, fetchedPlaylist.tracks.size)
    }

    private suspend fun importFromSpotifyUrl(url: String, customName: String?): PlaylistImportResult {
        val playlistId = url.substringAfter("/playlist/").substringBefore("?")
        if (playlistId.isBlank()) return PlaylistImportResult.Failure("ID Playlist Spotify non valido")

        val name = customName?.ifBlank { null } ?: "Spotify Import ($playlistId)"
        val created = playlistStore.create(name, null)
        return PlaylistImportResult.Success(created, 0)
    }

    private suspend fun importFromJson(jsonText: String, customName: String?): PlaylistImportResult {
        val tracks = mutableListOf<Track>()
        var extractedName: String? = null

        if (jsonText.startsWith("[")) {
            val array = JSONArray(jsonText)
            parseJsonArrayToTracks(array, tracks)
        } else {
            val obj = JSONObject(jsonText)
            extractedName = obj.optString("name").ifBlank { obj.optString("title").ifBlank { null } }
            val tracksArray = obj.optJSONArray("tracks") 
                ?: obj.optJSONArray("songs") 
                ?: obj.optJSONArray("queue")
            if (tracksArray != null) {
                parseJsonArrayToTracks(tracksArray, tracks)
            }
        }

        if (tracks.isEmpty()) return PlaylistImportResult.Failure("Nessun brano valido trovato nel file JSON")

        val name = customName?.ifBlank { null } ?: extractedName ?: "Imported Playlist"
        val created = playlistStore.create(name, tracks.firstOrNull())
        if (tracks.size > 1) {
            playlistStore.addTracks(created.id, tracks.drop(1))
        }

        val updated = playlistStore.load(created.id) ?: created
        return PlaylistImportResult.Success(updated, tracks.size)
    }

    private fun parseJsonArrayToTracks(array: JSONArray, outTracks: MutableList<Track>) {
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optString("id").ifBlank { item.optString("videoId") }
            val title = item.optString("title").ifBlank { item.optString("name") }
            if (id.isBlank() || title.isBlank()) continue

            val artist = item.optString("artist").ifBlank { item.optString("artistName", "Unknown Artist") }
            val duration = item.optLong("duration", item.optLong("durationMs", 0L) / 1000L)
            val thumb = item.optString("thumbnailUrl").ifBlank { item.optString("artworkUrl") }

            outTracks.add(
                Track(
                    id = id,
                    title = title,
                    artist = artist,
                    durationSeconds = duration,
                    thumbnailUrl = thumb,
                    largeThumbnailUrl = thumb
                )
            )
        }
    }

    private fun extractQueryParam(url: String, key: String): String {
        val query = url.substringAfter("?", "")
        if (query.isBlank()) return ""
        return query.split("&")
            .map { it.split("=") }
            .firstOrNull { it.size == 2 && it[0] == key }
            ?.get(1)
            ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
            .orEmpty()
    }
}

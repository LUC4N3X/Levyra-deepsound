package com.luc4n3x.levyra.desktop.core.storage

import com.luc4n3x.levyra.desktop.core.model.Track
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

data class PhonePlaylist(val name: String, val tracks: List<Track>)

data class PhoneLibrary(
    val favorites: List<Track> = emptyList(),
    val playlists: List<PhonePlaylist> = emptyList(),
    val history: List<HistoryEntry> = emptyList()
) {
    val isEmpty: Boolean get() = favorites.isEmpty() && playlists.isEmpty() && history.isEmpty()
}

data class PhoneImportResult(val favorites: Int, val playlists: Int, val history: Int)

object PhoneLibraryImport {
    private const val FAVORITES_ENTRY = "data/favorites.json"
    private const val PLAYLISTS_ENTRY = "data/playlists.json"
    private const val HISTORY_ENTRY = "data/history.json"
    private const val LEGACY_PAYLOAD_ENTRY = "payload.json"
    private const val MAX_ZIP_ENTRIES = 544
    private const val MAX_ENTRY_BYTES = 64L * 1024L * 1024L
    private const val MAX_TOTAL_BYTES = 96L * 1024L * 1024L
    private val wanted = setOf(FAVORITES_ENTRY, PLAYLISTS_ENTRY, HISTORY_ENTRY, LEGACY_PAYLOAD_ENTRY)
    private val json = Json { ignoreUnknownKeys = true }

    fun read(input: InputStream): PhoneLibrary {
        val entries = HashMap<String, ByteArray>()
        var count = 0
        var total = 0L
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                count += 1
                if (count > MAX_ZIP_ENTRIES) throw IOException("Too many backup entries")
                val bytes = readBounded(zip, MAX_ENTRY_BYTES)
                total += bytes.size
                if (total > MAX_TOTAL_BYTES) throw IOException("Backup too large")
                if (!entry.isDirectory && entry.name in wanted) entries[entry.name] = bytes
                zip.closeEntry()
            }
        }
        val legacy = entries[LEGACY_PAYLOAD_ENTRY]?.let { parse(it) as? JsonObject }
        val favorites = entries[FAVORITES_ENTRY]?.let(::parse) ?: legacy?.get("favorites")
        val playlists = entries[PLAYLISTS_ENTRY]?.let(::parse) ?: legacy?.get("playlists")
        val history = entries[HISTORY_ENTRY]?.let(::parse) ?: legacy?.get("history")
        if (favorites == null && playlists == null && history == null) throw IOException("Not a Levyra backup")
        return PhoneLibrary(
            favorites = favorites.objects().mapNotNull(::trackOf),
            playlists = playlists.objects().mapNotNull { playlist ->
                val name = playlist.text("name").trim()
                if (name.isEmpty()) null else PhonePlaylist(name, (playlist["tracks"] as? JsonArray).objects().mapNotNull(::trackOf))
            },
            history = history.objects().mapNotNull { event ->
                val track = trackOf(event, idKey = "trackId") ?: return@mapNotNull null
                HistoryEntry(track, event.long("startedAt"))
            }
        )
    }

    fun merge(current: LibraryData, phone: PhoneLibrary, maxHistory: Int, nowMillis: Long, newId: () -> String): Pair<LibraryData, PhoneImportResult> {
        val knownFavorites = current.favorites.mapTo(HashSet()) { it.id }
        val addedFavorites = phone.favorites.distinctBy { it.id }.filterNot { it.id in knownFavorites }
        var playlists = current.playlists
        var addedPlaylists = 0
        phone.playlists.forEach { incoming ->
            val existing = playlists.firstOrNull { it.name.equals(incoming.name, ignoreCase = true) }
            playlists = if (existing == null) {
                addedPlaylists += 1
                playlists + LocalPlaylist(newId(), incoming.name, nowMillis, incoming.tracks.distinctBy { it.id })
            } else {
                val known = existing.tracks.mapTo(HashSet()) { it.id }
                val merged = existing.copy(tracks = existing.tracks + incoming.tracks.distinctBy { it.id }.filterNot { it.id in known })
                playlists.map { if (it.id == existing.id) merged else it }
            }
        }
        val history = (current.history + phone.history)
            .sortedByDescending { it.playedAt }
            .distinctBy { it.track.id }
            .take(maxHistory)
        val addedHistory = history.count { entry -> current.history.none { it.track.id == entry.track.id } }
        val updated = current.copy(favorites = current.favorites + addedFavorites, playlists = playlists, history = history)
        return updated to PhoneImportResult(addedFavorites.size, addedPlaylists, addedHistory)
    }

    private fun trackOf(value: JsonObject, idKey: String = "id"): Track? {
        val id = value.text(idKey).trim()
        val videoUrl = value.text("videoUrl").trim()
        if (id.isEmpty() || !videoUrl.startsWith("https://", ignoreCase = true)) return null
        return Track(
            id = id,
            title = value.text("title"),
            artist = value.text("artist"),
            album = value.text("album"),
            videoUrl = videoUrl,
            artworkUrl = value.text("largeThumbnailUrl").ifBlank { value.text("thumbnailUrl") },
            durationMs = value.long("durationMs"),
            isrc = value.text("isrc")
        )
    }

    private fun parse(bytes: ByteArray): JsonElement = json.parseToJsonElement(bytes.toString(Charsets.UTF_8))

    private fun JsonElement?.objects(): List<JsonObject> = (this as? JsonArray).orEmpty().filterIsInstance<JsonObject>()

    private fun JsonObject.text(key: String): String =
        (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull.orEmpty()

    private fun JsonObject.long(key: String): Long =
        (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.longOrNull ?: 0L

    private fun readBounded(input: InputStream, limit: Long): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var read = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            read += count
            if (read > limit) throw IOException("Backup entry too large")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}

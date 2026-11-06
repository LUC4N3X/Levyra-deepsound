package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.data.local.SavedPlaylistEntity
import com.luc4n3x.levyra.data.local.SavedPlaylistsDao
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.SavedPlaylist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray

class SavedPlaylistsStore(
    private val dao: SavedPlaylistsDao,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val mutex = Mutex()

    fun observe(): Flow<List<SavedPlaylist>> = dao.observeAll().map { rows -> rows.map(SavedPlaylistEntity::toDomain) }

    suspend fun load(): List<SavedPlaylist> = dao.all().map(SavedPlaylistEntity::toDomain)

    suspend fun toggle(playlist: SavedPlaylist): Boolean = mutex.withLock {
        if (!playlist.isStorable()) return@withLock false
        dao.toggle(playlist.copy(savedAt = clock()).toEntity())
    }

    suspend fun remove(key: String) = mutex.withLock { dao.delete(key) }

    suspend fun replaceAll(playlists: List<SavedPlaylist>) = mutex.withLock {
        dao.replaceAll(playlists.filter(SavedPlaylist::isStorable).distinctBy(SavedPlaylist::key).map(SavedPlaylist::toEntity))
    }
}

internal fun SavedPlaylist.isStorable(): Boolean = hit.playlistId.isNotBlank()

internal fun parseSavedPlaylists(array: JSONArray?): List<SavedPlaylist> {
    if (array == null) return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            val json = array.optJSONObject(index) ?: continue
            val saved = SavedPlaylist(
                hit = PlaylistHit(
                    title = json.optString("title"),
                    author = json.optString("author"),
                    thumbnailUrl = json.optString("thumbnailUrl"),
                    playlistId = json.optString("playlistId").trim(),
                    browseId = json.optString("browseId"),
                    trackCountLabel = json.optString("trackCountLabel")
                ),
                appleStorefront = json.optString("appleStorefront"),
                savedAt = json.optLong("savedAt")
            )
            if (saved.isStorable()) add(saved)
        }
    }.distinctBy(SavedPlaylist::key)
}

private fun SavedPlaylist.toEntity() = SavedPlaylistEntity(
    playlistKey = key,
    playlistId = hit.playlistId.trim(),
    browseId = hit.browseId,
    appleStorefront = appleStorefront.trim().lowercase(),
    title = hit.title.trim().ifBlank { hit.playlistId.trim() },
    author = hit.author,
    thumbnailUrl = hit.thumbnailUrl,
    trackCountLabel = hit.trackCountLabel,
    savedAt = savedAt
)

private fun SavedPlaylistEntity.toDomain() = SavedPlaylist(
    hit = PlaylistHit(
        title = title,
        author = author,
        thumbnailUrl = thumbnailUrl,
        playlistId = playlistId,
        browseId = browseId,
        trackCountLabel = trackCountLabel
    ),
    appleStorefront = appleStorefront,
    savedAt = savedAt
)

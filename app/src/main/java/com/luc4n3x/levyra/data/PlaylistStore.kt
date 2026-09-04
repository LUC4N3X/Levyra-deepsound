package com.luc4n3x.levyra.data

import android.content.Context
import androidx.room.withTransaction
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.PlaylistEntity
import com.luc4n3x.levyra.data.local.PlaylistTagEntity
import com.luc4n3x.levyra.data.local.PlaylistTagLinkEntity
import com.luc4n3x.levyra.data.local.toPlaylistTrackEntity
import com.luc4n3x.levyra.data.local.toTrack
import com.luc4n3x.levyra.domain.PLAYLIST_TAG_MAX_PER_PLAYLIST
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.PlaylistTag
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.normalizePlaylistTagName
import com.luc4n3x.levyra.domain.sanitizePlaylistTagName
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class PlaylistStore(context: Context) {
    private val database = LevyraDatabase.get(context.applicationContext)
    private val dao = database.playlistDao()
    private val tagsDao = database.playlistTagsDao()

    suspend fun loadAll(): List<Playlist> = withContext(Dispatchers.IO) {
        runCatching {
            val tagsByPlaylist = tagAssignments()
            dao.allPlaylists().map { entity ->
                val tracks = dao.tracksOf(entity.id).map { it.toTrack() }
                entity.toPlaylist(tracks, tagsByPlaylist[entity.id].orEmpty())
            }
        }.onFailure { Timber.w(it, "Playlist load failed") }.getOrDefault(emptyList())
    }

    suspend fun load(playlistId: String): Playlist? = withContext(Dispatchers.IO) {
        runCatching {
            val entity = dao.playlist(playlistId) ?: return@runCatching null
            val tags = tagAssignments()[playlistId].orEmpty()
            entity.toPlaylist(dao.tracksOf(playlistId).map { it.toTrack() }, tags)
        }.onFailure { Timber.w(it, "Playlist load failed") }.getOrNull()
    }

    suspend fun setHidden(playlistId: String, hidden: Boolean) {
        withContext(Dispatchers.IO) {
            runCatching { dao.setHidden(playlistId, hidden, System.currentTimeMillis()) }
                .onFailure { Timber.w(it, "Playlist visibility update failed") }
        }
    }

    suspend fun allTags(): List<PlaylistTag> = withContext(Dispatchers.IO) {
        runCatching { tagsDao.allTags().map { it.toDomain() } }
            .onFailure { Timber.w(it, "Playlist tag load failed") }
            .getOrDefault(emptyList())
    }

    suspend fun createTag(name: String): PlaylistTag? = withContext(Dispatchers.IO) {
        val display = sanitizePlaylistTagName(name)
        val normalized = normalizePlaylistTagName(name)
        if (normalized.isEmpty()) return@withContext null
        runCatching {
            database.withTransaction {
                val existing = tagsDao.tagByNormalizedName(normalized)
                if (existing != null) {
                    existing.toDomain()
                } else {
                    val tag = PlaylistTagEntity(
                        id = UUID.randomUUID().toString(),
                        name = display,
                        normalizedName = normalized,
                        createdAt = System.currentTimeMillis()
                    )
                    tagsDao.upsertTag(tag)
                    tag.toDomain()
                }
            }
        }.onFailure { Timber.w(it, "Playlist tag creation failed") }.getOrNull()
    }

    suspend fun renameTag(tagId: String, name: String): Boolean = withContext(Dispatchers.IO) {
        val display = sanitizePlaylistTagName(name)
        val normalized = normalizePlaylistTagName(name)
        if (normalized.isEmpty()) return@withContext false
        runCatching {
            database.withTransaction {
                val conflict = tagsDao.tagByNormalizedName(normalized)
                if (conflict != null && conflict.id != tagId) {
                    false
                } else {
                    tagsDao.renameTag(tagId, display, normalized)
                    true
                }
            }
        }.onFailure { Timber.w(it, "Playlist tag rename failed") }.getOrDefault(false)
    }

    suspend fun deleteTag(tagId: String) {
        withContext(Dispatchers.IO) {
            runCatching { tagsDao.deleteTag(tagId) }
                .onFailure { Timber.w(it, "Playlist tag removal failed") }
        }
    }

    suspend fun setPlaylistTags(playlistId: String, tagIds: List<String>) {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val links = tagIds.distinct()
                .take(PLAYLIST_TAG_MAX_PER_PLAYLIST)
                .map { PlaylistTagLinkEntity(playlistId = playlistId, tagId = it, assignedAt = now) }
            runCatching { tagsDao.replaceLinksOf(playlistId, links) }
                .onFailure { Timber.w(it, "Playlist tag assignment failed") }
        }
    }

    suspend fun replaceTagCatalog(tags: List<PlaylistTag>, assignments: Map<String, List<String>>) {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val tagRows = tags
                .filter { it.id.isNotBlank() && it.normalizedName.isNotBlank() }
                .distinctBy { it.normalizedName }
                .map {
                    PlaylistTagEntity(
                        id = it.id,
                        name = it.name.ifBlank { it.normalizedName },
                        normalizedName = it.normalizedName,
                        createdAt = if (it.createdAt > 0L) it.createdAt else now
                    )
                }
            val knownTagIds = tagRows.mapTo(hashSetOf()) { it.id }
            val linkRows = assignments.flatMap { (playlistId, tagIds) ->
                tagIds.distinct()
                    .filter { it in knownTagIds }
                    .take(PLAYLIST_TAG_MAX_PER_PLAYLIST)
                    .map { PlaylistTagLinkEntity(playlistId = playlistId, tagId = it, assignedAt = now) }
            }
            runCatching { tagsDao.replaceAll(tagRows, linkRows) }
                .onFailure { Timber.w(it, "Playlist tag restore failed") }
        }
    }

    private suspend fun tagAssignments(): Map<String, List<PlaylistTag>> {
        val tagsById = tagsDao.allTags().associate { it.id to it.toDomain() }
        if (tagsById.isEmpty()) return emptyMap()
        return tagsDao.allLinks()
            .groupBy(PlaylistTagLinkEntity::playlistId)
            .mapValues { (_, links) ->
                links.mapNotNull { tagsById[it.tagId] }.sortedBy { it.normalizedName }
            }
    }

    suspend fun create(name: String, firstTrack: Track? = null): Playlist = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val cover = firstTrack?.largeThumbnailUrl?.ifBlank { firstTrack.thumbnailUrl }.orEmpty()
        dao.createPlaylistWithTracks(
            playlist = PlaylistEntity(id, name.trim().ifBlank { "Playlist" }, cover, now, now),
            tracks = firstTrack?.let { listOf(it.toPlaylistTrackEntity(id, 0, now)) }.orEmpty()
        )
        val tracks = firstTrack?.let { listOf(it) } ?: emptyList()
        Playlist(id, name.trim().ifBlank { "Playlist" }, cover, tracks, now, now)
    }

    suspend fun createWithTracks(name: String, tracks: List<Track>): Playlist = withContext(Dispatchers.IO) {
        val cleanTracks = tracks
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.id }
        require(cleanTracks.isNotEmpty()) { "Cannot create a playlist without valid tracks" }

        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val cleanName = name.trim().ifBlank { "Playlist" }
        val cover = cleanTracks.firstNotNullOfOrNull { track ->
            track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }.takeIf(String::isNotBlank)
        }.orEmpty()
        val entity = PlaylistEntity(id, cleanName, cover, now, now)
        val trackEntities = cleanTracks.mapIndexed { index, track ->
            track.toPlaylistTrackEntity(id, index, now)
        }

        dao.createPlaylistWithTracks(entity, trackEntities)
        Playlist(id, cleanName, cover, cleanTracks, now, now)
    }

    suspend fun rename(playlistId: String, name: String) = withContext(Dispatchers.IO) {
        dao.rename(playlistId, name.trim().ifBlank { "Playlist" }, System.currentTimeMillis())
    }

    suspend fun delete(playlistId: String) = withContext(Dispatchers.IO) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun addTrack(playlistId: String, track: Track) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val nextPos = (dao.maxPosition(playlistId) ?: -1) + 1
        dao.insertTracks(listOf(track.toPlaylistTrackEntity(playlistId, nextPos, now)))
        val cover = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
        if (cover.isNotBlank()) dao.updateCover(playlistId, cover, now) else dao.touch(playlistId, now)
    }

    suspend fun addTracks(playlistId: String, tracks: List<Track>) = withContext(Dispatchers.IO) {
        val cleanTracks = tracks
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.id }
        if (cleanTracks.isEmpty()) return@withContext
        val existingIds = dao.tracksOf(playlistId).mapTo(hashSetOf()) { it.trackId }
        val pending = cleanTracks.filterNot { it.id in existingIds }
        if (pending.isEmpty()) return@withContext
        val now = System.currentTimeMillis()
        val startPosition = (dao.maxPosition(playlistId) ?: -1) + 1
        dao.insertTracks(pending.mapIndexed { index, track ->
            track.toPlaylistTrackEntity(playlistId, startPosition + index, now)
        })
        val cover = pending.firstNotNullOfOrNull { track ->
            track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }.takeIf(String::isNotBlank)
        }.orEmpty()
        if (cover.isNotBlank()) dao.updateCover(playlistId, cover, now) else dao.touch(playlistId, now)
    }

    suspend fun removeTrack(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        dao.removeTracksAndCompact(playlistId, setOf(trackId))
    }

    suspend fun removeTracks(playlistId: String, trackIds: Set<String>) = withContext(Dispatchers.IO) {
        if (trackIds.isEmpty()) return@withContext
        dao.removeTracksAndCompact(playlistId, trackIds)
    }

    suspend fun reorder(playlistId: String, orderedTracks: List<Track>) = withContext(Dispatchers.IO) {
        val existing = dao.tracksOf(playlistId)
        val existingById = existing.associateBy { it.trackId }
        val orderedIds = orderedTracks.map { it.id }.filter(String::isNotBlank)
        if (orderedIds.size != existing.size || orderedIds.toSet().size != orderedIds.size) return@withContext
        if (orderedIds.toSet() != existingById.keys) return@withContext
        dao.replaceTracks(playlistId, orderedIds.mapIndexed { index, trackId ->
            existingById.getValue(trackId).copy(position = index)
        })
    }

    suspend fun updateTrackMetadata(playlists: List<Playlist>) = withContext(Dispatchers.IO) {
        runCatching {
            playlists.distinctBy { it.id }.forEach { playlist ->
                val existing = dao.tracksOf(playlist.id).associateBy { it.trackId }
                val updates = playlist.tracks.mapNotNull { track ->
                    existing[track.id]?.let { stored ->
                        track.toPlaylistTrackEntity(playlist.id, stored.position, stored.addedAt)
                    }
                }
                if (updates.isNotEmpty()) dao.insertTracks(updates)
            }
        }.onFailure { Timber.w(it, "Playlist metadata update failed") }
    }

    private fun PlaylistEntity.toPlaylist(
        tracks: List<Track>,
        tags: List<PlaylistTag> = emptyList()
    ): Playlist = Playlist(id, name, coverUrl, tracks, createdAt, updatedAt, tags, hidden)
}

private fun PlaylistTagEntity.toDomain() = PlaylistTag(
    id = id,
    name = name,
    normalizedName = normalizedName,
    createdAt = createdAt
)

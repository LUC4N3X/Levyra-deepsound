package com.luc4n3x.levyra.data

import android.content.Context
import androidx.room.withTransaction
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.PlaylistAlbumEntity
import com.luc4n3x.levyra.data.local.albumTrackOffsets
import com.luc4n3x.levyra.data.local.encodePlaylistAlbumTracks
import com.luc4n3x.levyra.data.local.toPlaylistAlbumEntity
import com.luc4n3x.levyra.data.local.toPlaylistAlbumEntry
import com.luc4n3x.levyra.data.local.PlaylistEntity
import com.luc4n3x.levyra.data.local.PlaylistTagEntity
import com.luc4n3x.levyra.data.local.PlaylistTagLinkEntity
import com.luc4n3x.levyra.data.local.PlaylistTrackEntity
import com.luc4n3x.levyra.data.local.toPlaylistTrackEntity
import com.luc4n3x.levyra.data.local.toTrack
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.PLAYLIST_TAG_MAX_PER_PLAYLIST
import com.luc4n3x.levyra.domain.PlaylistAlbumEntry
import com.luc4n3x.levyra.domain.PlaylistLayoutSlot
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.PlaylistCoverMode
import com.luc4n3x.levyra.domain.PlaylistTag
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.normalizePlaylistTagName
import com.luc4n3x.levyra.domain.sanitizePlaylistTagName
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

internal data class PlaylistStudioStoreSnapshot(
    val playlist: PlaylistEntity,
    val tracks: List<PlaylistTrackEntity>,
    val albums: List<PlaylistAlbumEntity> = emptyList()
)

enum class PlaylistAlbumAddResult { Added, AlreadyPresent, Unavailable }

private data class PlaylistContent(
    val tracks: List<Track>,
    val albums: List<PlaylistAlbumEntry>
)

class PlaylistStore(context: Context) {
    private val database = LevyraDatabase.get(context.applicationContext)
    private val dao = database.playlistDao()
    private val tagsDao = database.playlistTagsDao()
    private val coverStore = PlaylistCoverStore(context.applicationContext)
    private val coverMutationLocks = mutableMapOf<String, Mutex>()
    private val stablePlaylistMutex = Mutex()

    private fun coverMutationLock(playlistId: String): Mutex = synchronized(coverMutationLocks) {
        coverMutationLocks.getOrPut(playlistId) { Mutex() }
    }

    suspend fun loadAll(): List<Playlist> = withContext(Dispatchers.IO) {
        runCatching {
            val tagsByPlaylist = tagAssignments()
            dao.allPlaylists().map { entity ->
                val content = content(entity.id)
                entity.toPlaylist(content.tracks, tagsByPlaylist[entity.id].orEmpty(), content.albums)
            }
        }.onFailure { Timber.w(it, "Playlist load failed") }.getOrDefault(emptyList())
    }

    suspend fun load(playlistId: String): Playlist? = withContext(Dispatchers.IO) {
        runCatching {
            val entity = dao.playlist(playlistId) ?: return@runCatching null
            val tags = tagAssignments()[playlistId].orEmpty()
            val content = content(playlistId)
            entity.toPlaylist(content.tracks, tags, content.albums)
        }.onFailure { Timber.w(it, "Playlist load failed") }.getOrNull()
    }

    private suspend fun content(playlistId: String): PlaylistContent {
        val trackRows = dao.tracksOf(playlistId)
        val albumRows = dao.albumsOf(playlistId)
        val offsets = albumTrackOffsets(trackRows, albumRows)
        return PlaylistContent(
            tracks = trackRows.map { it.toTrack() },
            albums = albumRows.map { it.toPlaylistAlbumEntry(offsets[it.entryId] ?: trackRows.size) }
        )
    }

    suspend fun addAlbum(playlistId: String, album: AlbumHit, tracks: List<Track>): PlaylistAlbumAddResult =
        withContext(Dispatchers.IO) {
            val cleanTracks = validPlaylistTracks(tracks).distinctBy { it.id }
            if (cleanTracks.isEmpty() || album.title.isBlank()) return@withContext PlaylistAlbumAddResult.Unavailable
            val now = System.currentTimeMillis()
            val entity = album.toPlaylistAlbumEntity(
                playlistId = playlistId,
                entryId = UUID.randomUUID().toString(),
                position = 0,
                tracks = cleanTracks,
                addedAt = now
            )
            val added = dao.addAlbum(entity)
            if (!added) {
                return@withContext if (dao.playlist(playlistId) == null) PlaylistAlbumAddResult.Unavailable
                else PlaylistAlbumAddResult.AlreadyPresent
            }
            val cover = album.thumbnailUrl.ifBlank {
                cleanTracks.firstNotNullOfOrNull { it.largeThumbnailUrl.ifBlank { it.thumbnailUrl }.takeIf(String::isNotBlank) }.orEmpty()
            }
            if (cover.isNotBlank()) dao.updateAutomaticCover(playlistId, cover, now) else dao.touch(playlistId, now)
            PlaylistAlbumAddResult.Added
        }

    suspend fun createWithAlbum(name: String, album: AlbumHit, tracks: List<Track>): Playlist? = withContext(Dispatchers.IO) {
        if (validPlaylistTracks(tracks).isEmpty() || album.title.isBlank()) return@withContext null
        val created = create(name)
        if (addAlbum(created.id, album, tracks) != PlaylistAlbumAddResult.Added) {
            dao.deletePlaylist(created.id)
            return@withContext null
        }
        load(created.id)
    }

    suspend fun removeEntries(playlistId: String, trackEntryIds: Set<String>, albumEntryIds: Set<String>) =
        withContext(Dispatchers.IO) {
            dao.removeEntries(playlistId, trackEntryIds, albumEntryIds)
        }

    suspend fun reorderItems(playlistId: String, ordered: List<PlaylistLayoutSlot>): Boolean = withContext(Dispatchers.IO) {
        dao.reorderItems(playlistId, ordered)
    }

    suspend fun refreshAlbumTracks(playlistId: String, entryId: String, tracks: List<Track>) = withContext(Dispatchers.IO) {
        val cleanTracks = validPlaylistTracks(tracks).distinctBy { it.id }
        if (cleanTracks.isEmpty()) return@withContext
        runCatching { dao.replaceAlbumTracks(playlistId, entryId, encodePlaylistAlbumTracks(cleanTracks)) }
            .onFailure { Timber.w(it, "Playlist album refresh failed") }
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

    suspend fun replaceTrack(playlistId: String, oldTrackId: String, replacement: Track): Boolean = withContext(Dispatchers.IO) {
        if (replacement.id.isBlank() || replacement.title.isBlank()) return@withContext false
        dao.replaceTrackInPlace(
            playlistId,
            oldTrackId,
            replacement.playlistEntryId.takeIf(String::isNotBlank),
            replacement.copy(streamUrl = "").toPlaylistTrackEntity(playlistId, 0, System.currentTimeMillis())
        )
    }

    suspend fun createWithTracks(name: String, tracks: List<Track>, playlistId: String? = null): Playlist = withContext(Dispatchers.IO) {
        val cleanTracks = validPlaylistTracks(tracks)
        require(cleanTracks.isNotEmpty()) { "Cannot create a playlist without valid tracks" }

        val now = System.currentTimeMillis()
        val id = playlistId ?: UUID.randomUUID().toString()
        val cleanName = name.trim().ifBlank { "Playlist" }
        val cover = cleanTracks.firstNotNullOfOrNull { track ->
            track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }.takeIf(String::isNotBlank)
        }.orEmpty()
        val existing = playlistId?.let { dao.playlist(it) }

        if (existing != null) {
            val existingRows = dao.tracksOf(id)
            val rows = buildPlaylistRows(id, cleanTracks, existingRows, now)
            database.withTransaction {
                dao.replaceTracks(id, rows)
                dao.rename(id, cleanName, now)
                if (cover.isNotBlank()) dao.updateAutomaticCover(id, cover, now) else dao.touch(id, now)
            }
            val refreshed = dao.playlist(id) ?: existing
            val content = content(id)
            return@withContext refreshed.toPlaylist(content.tracks, emptyList(), content.albums)
        }

        val entity = PlaylistEntity(id, cleanName, cover, now, now)
        val trackEntities = buildPlaylistRows(id, cleanTracks, emptyList(), now)
        dao.createPlaylistWithTracks(entity, trackEntities)
        Playlist(id, cleanName, cover, cleanTracks, now, now)
    }

    suspend fun rename(playlistId: String, name: String) = withContext(Dispatchers.IO) {
        dao.rename(playlistId, name.trim().ifBlank { "Playlist" }, System.currentTimeMillis())
    }

    suspend fun delete(playlistId: String) = withContext(Dispatchers.IO) {
        LevyraVaultOperationMutex.withLock vault@ {
            coverMutationLock(playlistId).withLock cover@ {
                val previous = dao.playlist(playlistId)
                dao.deletePlaylist(playlistId)
                if (previous?.coverMode == PlaylistCoverMode.CUSTOM.name) coverStore.delete(previous.coverUrl)
            }
        }
    }

    suspend fun addTrack(playlistId: String, track: Track) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (track.id.isBlank() || track.title.isBlank()) return@withContext
        if (dao.tracksOf(playlistId).any { it.trackId == track.id }) return@withContext
        val nextPos = dao.nextPosition(playlistId)
        dao.insertTracks(listOf(track.toPlaylistTrackEntity(playlistId, nextPos, now)))
        val cover = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
        if (cover.isNotBlank()) dao.updateAutomaticCover(playlistId, cover, now) else dao.touch(playlistId, now)
    }

    suspend fun appendToStablePlaylist(playlistId: String, name: String, track: Track): Boolean = withContext(Dispatchers.IO) {
        require(playlistId.isNotBlank())
        if (track.id.isBlank() || track.title.isBlank()) return@withContext false
        stablePlaylistMutex.withLock {
            val now = System.currentTimeMillis()
            val cover = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
            if (dao.playlist(playlistId) == null) {
                dao.createPlaylistWithTracks(
                    PlaylistEntity(playlistId, name.trim().ifBlank { "Playlist" }, cover, now, now),
                    listOf(track.toPlaylistTrackEntity(playlistId, 0, now))
                )
                return@withLock true
            }
            if (dao.tracksOf(playlistId).any { it.trackId == track.id }) return@withLock false
            val nextPos = dao.nextPosition(playlistId)
            dao.insertTracks(listOf(track.toPlaylistTrackEntity(playlistId, nextPos, now)))
            if (cover.isNotBlank()) dao.updateAutomaticCover(playlistId, cover, now) else dao.touch(playlistId, now)
            true
        }
    }

    suspend fun addTracks(playlistId: String, tracks: List<Track>) = withContext(Dispatchers.IO) {
        val cleanTracks = validPlaylistTracks(tracks).distinctBy { it.id }
        if (cleanTracks.isEmpty()) return@withContext
        val existingIds = dao.tracksOf(playlistId).mapTo(hashSetOf()) { it.trackId }
        val pending = cleanTracks.filterNot { it.id in existingIds }
        if (pending.isEmpty()) return@withContext
        val now = System.currentTimeMillis()
        val startPosition = dao.nextPosition(playlistId)
        dao.insertTracks(pending.mapIndexed { index, track ->
            track.toPlaylistTrackEntity(playlistId, startPosition + index, now)
        })
        val cover = pending.firstNotNullOfOrNull { track ->
            track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }.takeIf(String::isNotBlank)
        }.orEmpty()
        if (cover.isNotBlank()) dao.updateAutomaticCover(playlistId, cover, now) else dao.touch(playlistId, now)
    }

    suspend fun setCustomCover(playlistId: String, source: android.net.Uri, crop: PlaylistCoverCrop) =
        replaceCustomCover(playlistId) { coverStore.save(playlistId, source, crop) }

    suspend fun setRenderedCover(playlistId: String, bitmap: android.graphics.Bitmap) =
        replaceCustomCover(playlistId) { coverStore.saveRendered(playlistId, bitmap) }

    private suspend fun replaceCustomCover(playlistId: String, produce: suspend () -> String) =
        withContext(Dispatchers.IO) {
            LevyraVaultOperationMutex.withLock vault@ {
                coverMutationLock(playlistId).withLock cover@ {
                    val previous = dao.playlist(playlistId) ?: return@cover
                    val reference = produce()
                    try {
                        dao.updateCustomCover(playlistId, reference, System.currentTimeMillis())
                    } catch (error: Throwable) {
                        coverStore.delete(reference)
                        throw error
                    }
                    if (previous.coverMode == PlaylistCoverMode.CUSTOM.name && previous.coverUrl != reference) {
                        coverStore.delete(previous.coverUrl)
                    }
                }
            }
        }

    suspend fun createForStudio(name: String, tracks: List<Track>): Playlist =
        if (tracks.any { it.id.isNotBlank() && it.title.isNotBlank() }) createWithTracks(name, tracks) else create(name)

    internal suspend fun snapshotStudio(playlistId: String): PlaylistStudioStoreSnapshot? = withContext(Dispatchers.IO) {
        val playlist = dao.playlist(playlistId) ?: return@withContext null
        PlaylistStudioStoreSnapshot(playlist = playlist, tracks = dao.tracksOf(playlistId), albums = dao.albumsOf(playlistId))
    }

    internal suspend fun restoreStudio(snapshot: PlaylistStudioStoreSnapshot): Boolean = withContext(Dispatchers.IO) {
        LevyraVaultOperationMutex.withLock vault@ {
            coverMutationLock(snapshot.playlist.id).withLock cover@ {
                dao.restoreLayout(snapshot.playlist, snapshot.tracks, snapshot.albums)
            }
        }
    }

    suspend fun applyStudioEdit(playlistId: String, name: String, tracks: List<Track>): Boolean =
        withContext(Dispatchers.IO) {
            val cleanTracks = validPlaylistTracks(tracks)
            val now = System.currentTimeMillis()
            val existing = dao.tracksOf(playlistId)
            val entities = buildPlaylistRows(playlistId, cleanTracks, existing, now)
            val automaticCover = cleanTracks.firstNotNullOfOrNull { track ->
                track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }.takeIf(String::isNotBlank)
            }.orEmpty()
            dao.applyStudioEdit(
                playlistId = playlistId,
                name = name.trim().ifBlank { "Playlist" },
                tracks = entities,
                automaticCover = automaticCover,
                updatedAt = now
            )
        }

    suspend fun resetCover(playlistId: String) = withContext(Dispatchers.IO) {
        LevyraVaultOperationMutex.withLock vault@ {
            coverMutationLock(playlistId).withLock cover@ {
                val previous = dao.playlist(playlistId) ?: return@cover
                val automatic = dao.tracksOf(playlistId).firstNotNullOfOrNull { entity ->
                    entity.largeThumbnailUrl.ifBlank { entity.thumbnailUrl }.takeIf(String::isNotBlank)
                } ?: dao.albumsOf(playlistId).firstNotNullOfOrNull { album ->
                    album.thumbnailUrl.takeIf(String::isNotBlank)
                }.orEmpty()
                dao.resetCover(playlistId, automatic, System.currentTimeMillis())
                if (previous.coverMode == PlaylistCoverMode.CUSTOM.name) coverStore.delete(previous.coverUrl)
            }
        }
    }

    suspend fun removeTrack(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        dao.removeTracksAndCompact(playlistId, setOf(trackId))
    }

    suspend fun removeTracks(playlistId: String, trackIds: Set<String>) = withContext(Dispatchers.IO) {
        if (trackIds.isEmpty()) return@withContext
        dao.removeTracksAndCompact(playlistId, trackIds)
    }

    suspend fun reorder(playlistId: String, orderedTracks: List<Track>) = withContext(Dispatchers.IO) {
        val orderedIds = orderedTracks.map { it.id }.filter(String::isNotBlank)
        dao.reorderTracks(playlistId, orderedIds)
    }

    suspend fun updateTrackMetadata(playlists: List<Playlist>) = withContext(Dispatchers.IO) {
        runCatching {
            playlists.distinctBy { it.id }.forEach { playlist ->
                val latestById = playlist.tracks.associateBy { it.id }
                val updates = dao.tracksOf(playlist.id).mapNotNull { stored ->
                    latestById[stored.trackId]?.toPlaylistTrackEntity(
                        playlistId = playlist.id,
                        position = stored.position,
                        addedAt = stored.addedAt,
                        entryId = stored.entryId
                    )
                }
                if (updates.isNotEmpty()) dao.insertTracks(updates)
            }
        }.onFailure { Timber.w(it, "Playlist metadata update failed") }
    }

    private fun PlaylistEntity.toPlaylist(
        tracks: List<Track>,
        tags: List<PlaylistTag> = emptyList(),
        albums: List<PlaylistAlbumEntry> = emptyList()
    ): Playlist = Playlist(id, name, coverUrl, tracks, createdAt, updatedAt, tags, hidden, PlaylistCoverMode.from(coverMode), albums)
}

private fun validPlaylistTracks(tracks: List<Track>): List<Track> =
    tracks.filter { it.id.isNotBlank() && it.title.isNotBlank() }

private fun buildPlaylistRows(
    playlistId: String,
    tracks: List<Track>,
    existing: List<PlaylistTrackEntity>,
    now: Long
): List<PlaylistTrackEntity> {
    val pools = existing
        .groupBy { it.trackId }
        .mapValues { (_, rows) -> rows.sortedBy { it.position }.toMutableList() }
        .toMutableMap()
    return tracks.mapIndexed { index, track ->
        val bucket = pools[track.id]
        val previous = if (bucket != null && bucket.isNotEmpty()) bucket.removeAt(0) else null
        track.toPlaylistTrackEntity(
            playlistId = playlistId,
            position = index,
            addedAt = previous?.addedAt ?: now,
            entryId = previous?.entryId ?: UUID.randomUUID().toString()
        )
    }
}

private fun PlaylistTagEntity.toDomain() = PlaylistTag(
    id = id,
    name = name,
    normalizedName = normalizedName,
    createdAt = createdAt
)
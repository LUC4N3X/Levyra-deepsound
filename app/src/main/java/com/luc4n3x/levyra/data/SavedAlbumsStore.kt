package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.local.SavedAlbumEntity
import com.luc4n3x.levyra.data.local.SavedAlbumsDatabase
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.domain.SavedAlbum
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SavedAlbumsStore(context: Context) {
    private val dao = SavedAlbumsDatabase.get(context.applicationContext).savedAlbumsDao()

    fun observeAll(): Flow<List<SavedAlbum>> = dao.observeAll().map { rows -> rows.map(SavedAlbumEntity::toDomain) }

    fun observeContains(album: AlbumHit): Flow<Boolean> = dao.observeContains(savedAlbumIdentityKey(album))

    suspend fun load(): List<SavedAlbum> = dao.all().map(SavedAlbumEntity::toDomain)

    suspend fun contains(album: AlbumHit): Boolean = dao.contains(savedAlbumIdentityKey(album))

    suspend fun toggle(album: AlbumHit): Boolean = mutationMutex.withLock {
        val key = savedAlbumIdentityKey(album)
        if (dao.contains(key)) {
            dao.delete(key)
            false
        } else {
            dao.upsert(album.toEntity(key, System.currentTimeMillis()))
            true
        }
    }

    suspend fun remove(album: AlbumHit) = mutationMutex.withLock {
        dao.delete(savedAlbumIdentityKey(album))
    }

    private companion object {
        val mutationMutex = Mutex()
    }
}

internal fun savedAlbumIdentityKey(album: AlbumHit): String {
    searchAlbumCanonicalKey(album).takeIf(String::isNotBlank)?.let { return it }
    searchAlbumMetadataKey(album).takeIf(String::isNotBlank)?.let { return it }
    return "album:${album.artist.trim().lowercase(Locale.ROOT)}|${album.title.trim().lowercase(Locale.ROOT)}"
}

private fun AlbumHit.toEntity(key: String, savedAt: Long) = SavedAlbumEntity(
    albumKey = key,
    browseId = browseId,
    title = title,
    artist = artist,
    year = year,
    thumbnailUrl = thumbnailUrl,
    query = query,
    artistBrowseId = artistBrowseId,
    audioPlaylistId = audioPlaylistId,
    explicit = explicit,
    releaseDate = releaseDate,
    upc = upc,
    canonicalUrl = canonicalUrl,
    metadataProvider = metadataProvider,
    metadataConfidence = metadataConfidence,
    releaseType = releaseType.name,
    savedAt = savedAt
)

private fun SavedAlbumEntity.toDomain() = SavedAlbum(
    album = AlbumHit(
        title = title,
        artist = artist,
        year = year,
        thumbnailUrl = thumbnailUrl,
        query = query,
        browseId = browseId,
        artistBrowseId = artistBrowseId,
        audioPlaylistId = audioPlaylistId,
        explicit = explicit,
        releaseDate = releaseDate,
        upc = upc,
        canonicalUrl = canonicalUrl,
        metadataProvider = metadataProvider,
        metadataConfidence = metadataConfidence,
        releaseType = ReleaseType.entries.firstOrNull { it.name == releaseType } ?: ReleaseType.Unknown
    ),
    savedAt = savedAt
)

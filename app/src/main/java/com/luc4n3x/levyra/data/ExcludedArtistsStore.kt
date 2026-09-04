package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.local.ExcludedArtistEntity
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.domain.ExcludedArtist
import com.luc4n3x.levyra.domain.excludedArtistKeyOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

class ExcludedArtistsStore(context: Context) {
    private val dao = LevyraDatabase.get(context.applicationContext).excludedArtistsDao()

    suspend fun load(): List<ExcludedArtist> = withContext(Dispatchers.IO) {
        try {
            dao.all().map { it.toDomain() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Excluded artists load failed")
            emptyList()
        }
    }

    suspend fun exclude(browseId: String, name: String, excludedAt: Long = System.currentTimeMillis()) {
        val cleanName = name.trim()
        val key = excludedArtistKeyOf(browseId, cleanName)
        if (key.isBlank() || key == "name:") return
        withContext(Dispatchers.IO) {
            mutationMutex.withLock {
                try {
                    dao.upsert(
                        ExcludedArtistEntity(
                            artistKey = key,
                            browseId = browseId.trim(),
                            name = cleanName,
                            excludedAt = excludedAt
                        )
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Timber.w(error, "Artist exclusion write failed")
                }
            }
        }
    }

    suspend fun include(browseId: String, name: String) {
        val key = excludedArtistKeyOf(browseId, name)
        if (key.isBlank()) return
        withContext(Dispatchers.IO) {
            mutationMutex.withLock {
                try {
                    dao.delete(key)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Timber.w(error, "Artist exclusion removal failed")
                }
            }
        }
    }

    suspend fun replaceAll(artists: List<ExcludedArtist>) = withContext(Dispatchers.IO) {
        mutationMutex.withLock {
            val rows = artists
                .map(ExcludedArtist::toEntity)
                .filter { it.artistKey.isNotBlank() && it.artistKey != "name:" }
                .distinctBy { it.artistKey }
            dao.clear()
            if (rows.isNotEmpty()) dao.upsertAll(rows)
        }
    }

    private companion object {
        val mutationMutex = Mutex()
    }
}

private fun ExcludedArtistEntity.toDomain() = ExcludedArtist(
    browseId = browseId,
    name = name,
    excludedAt = excludedAt
)

private fun ExcludedArtist.toEntity() = ExcludedArtistEntity(
    artistKey = key,
    browseId = browseId.trim(),
    name = name.trim(),
    excludedAt = excludedAt
)

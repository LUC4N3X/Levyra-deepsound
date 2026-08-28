package com.luc4n3x.levyra.feature.recognition

import android.content.Context
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.RecognitionHistoryDao
import com.luc4n3x.levyra.data.local.RecognitionHistoryEntity
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

data class RecognitionHistoryEntry(
    val id: String,
    val recognizedAt: Long,
    val result: RecognitionResult
) {
    val identityKey: String
        get() = recognitionIdentityKey(result)
}

internal fun recognitionIdentityKey(result: RecognitionResult): String {
    val isrc = result.isrc.trim().uppercase(Locale.ROOT)
    if (isrc.isNotBlank()) return "isrc:$isrc"
    val providerId = result.providerTrackId.trim()
    if (providerId.isNotBlank()) return "${result.provider.trim().lowercase(Locale.ROOT)}:$providerId"
    val title = result.title.trim().lowercase(Locale.ROOT)
    val artist = result.artist.trim().lowercase(Locale.ROOT)
    return "text:$title|$artist"
}

class RecognitionHistoryStore(private val dao: RecognitionHistoryDao) {

    fun observe(): Flow<List<RecognitionHistoryEntry>> = dao.observe(MAX_ENTRIES)
        .map { entities -> entities.map(RecognitionHistoryEntity::toEntry) }
        .catch { error ->
            if (error is CancellationException) throw error
            Timber.w(error, "Recognition history read failed")
            emit(emptyList())
        }
        .flowOn(Dispatchers.IO)

    suspend fun record(result: RecognitionResult, recognizedAt: Long = System.currentTimeMillis()) {
        withContext(Dispatchers.IO) {
            runCatching {
                val identity = recognitionIdentityKey(result)
                val latest = dao.latest()
                val entryId = if (latest != null && recognitionIdentityKey(latest.toResult()) == identity) {
                    latest.id
                } else {
                    UUID.randomUUID().toString()
                }
                dao.upsert(result.toEntity(entryId, recognizedAt))
                dao.prune(MAX_ENTRIES)
            }.onFailure { error ->
                if (error is CancellationException) throw error
                Timber.w(error, "Recognition history write failed")
            }
        }
    }

    suspend fun delete(id: String) {
        withContext(Dispatchers.IO) {
            runCatching { dao.delete(id) }.onFailure { error ->
                if (error is CancellationException) throw error
                Timber.w(error, "Recognition history delete failed")
            }
        }
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            runCatching { dao.clear() }.onFailure { error ->
                if (error is CancellationException) throw error
                Timber.w(error, "Recognition history clear failed")
            }
        }
    }

    companion object {
        const val MAX_ENTRIES = 100

        fun from(context: Context): RecognitionHistoryStore =
            RecognitionHistoryStore(LevyraDatabase.get(context.applicationContext).recognitionHistoryDao())
    }
}

private fun RecognitionHistoryEntity.toResult(): RecognitionResult = RecognitionResult(
    title = title,
    artist = artist,
    album = album,
    provider = provider,
    providerTrackId = providerTrackId,
    artworkUrl = artworkUrl,
    isrc = isrc,
    youtubeVideoId = youtubeVideoId,
    year = year
)

private fun RecognitionHistoryEntity.toEntry(): RecognitionHistoryEntry = RecognitionHistoryEntry(
    id = id,
    recognizedAt = recognizedAt,
    result = toResult()
)

private fun RecognitionResult.toEntity(id: String, recognizedAt: Long): RecognitionHistoryEntity =
    RecognitionHistoryEntity(
        id = id,
        recognizedAt = recognizedAt,
        title = title,
        artist = artist,
        album = album,
        artworkUrl = artworkUrl,
        provider = provider,
        providerTrackId = providerTrackId,
        isrc = isrc,
        youtubeVideoId = youtubeVideoId,
        year = year
    )

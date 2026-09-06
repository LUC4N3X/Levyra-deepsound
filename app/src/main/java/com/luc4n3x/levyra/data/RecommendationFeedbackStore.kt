package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.RecommendationFeedbackEntity
import com.luc4n3x.levyra.domain.RecommendationFeedback
import com.luc4n3x.levyra.domain.RecommendationFeedbackEntry
import com.luc4n3x.levyra.domain.RecommendationFeedbackKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

private val ARTIST_KEY_SEPARATOR: Char = 31.toChar()

class RecommendationFeedbackStore(context: Context) {
    private val dao = LevyraDatabase.get(context.applicationContext).recommendationFeedbackDao()

    suspend fun load(): RecommendationFeedback = withContext(Dispatchers.IO) {
        try {
            RecommendationFeedback.from(
                dao.recent(RecommendationFeedback.MAX_ENTRIES).mapNotNull(RecommendationFeedbackEntity::toDomain)
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Recommendation feedback load failed")
            RecommendationFeedback.Empty
        }
    }

    suspend fun record(entry: RecommendationFeedbackEntry): RecommendationFeedback = withContext(Dispatchers.IO) {
        mutationMutex.withLock {
            try {
                dao.upsertAndTrim(entry.toEntity(), RecommendationFeedback.MAX_ENTRIES)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Recommendation feedback write failed")
            }
        }
        load()
    }

    suspend fun clear(trackKey: String): RecommendationFeedback = withContext(Dispatchers.IO) {
        val clean = trackKey.trim()
        if (clean.isNotEmpty()) {
            mutationMutex.withLock {
                try {
                    dao.delete(clean)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Timber.w(error, "Recommendation feedback removal failed")
                }
            }
        }
        load()
    }

    private companion object {
        val mutationMutex = Mutex()
    }
}

internal fun RecommendationFeedbackEntity.toDomain(): RecommendationFeedbackEntry? {
    val parsedKind = RecommendationFeedbackKind.entries.firstOrNull { it.name == kind } ?: return null
    return RecommendationFeedbackEntry(
        trackKey = trackKey,
        artistKeys = artistKeys.split(ARTIST_KEY_SEPARATOR).filter(String::isNotEmpty),
        kind = parsedKind,
        updatedAt = updatedAt
    )
}

internal fun RecommendationFeedbackEntry.toEntity(): RecommendationFeedbackEntity = RecommendationFeedbackEntity(
    trackKey = trackKey,
    artistKeys = artistKeys.filter(String::isNotEmpty).joinToString(ARTIST_KEY_SEPARATOR.toString()),
    kind = kind.name,
    updatedAt = updatedAt
)

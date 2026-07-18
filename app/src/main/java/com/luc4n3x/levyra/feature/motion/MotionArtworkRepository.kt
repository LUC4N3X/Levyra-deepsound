package com.luc4n3x.levyra.feature.motion

import com.luc4n3x.levyra.data.local.MotionArtworkDao
import com.luc4n3x.levyra.data.local.MotionArtworkEntity

sealed interface MotionArtworkCacheResult {
    data class Hit(val artwork: MotionArtwork) : MotionArtworkCacheResult
    data object Negative : MotionArtworkCacheResult
    data object Miss : MotionArtworkCacheResult
}

class MotionArtworkRepository(
    private val dao: MotionArtworkDao,
    private val clock: () -> Long = System::currentTimeMillis
) {
    suspend fun get(identityKey: String, configEpoch: Long): MotionArtworkCacheResult {
        val entity = dao.get(identityKey) ?: return MotionArtworkCacheResult.Miss
        val now = clock()
        if (entity.expiresAt <= now || entity.configEpoch != configEpoch) {
            dao.delete(identityKey)
            return MotionArtworkCacheResult.Miss
        }
        if (entity.negative) return MotionArtworkCacheResult.Negative
        val provider = entity.provider ?: return MotionArtworkCacheResult.Miss
        val url = entity.url ?: return MotionArtworkCacheResult.Miss
        val mimeType = entity.mimeType ?: return MotionArtworkCacheResult.Miss
        return MotionArtworkCacheResult.Hit(
            MotionArtwork(
                identityKey = entity.identityKey,
                provider = provider,
                url = url,
                mimeType = mimeType,
                width = entity.width,
                height = entity.height,
                confidence = entity.confidence,
                expiresAtMs = entity.expiresAt,
                lastVerifiedAtMs = entity.lastVerifiedAt,
                configEpoch = entity.configEpoch
            )
        )
    }

    suspend fun save(artwork: MotionArtwork) {
        dao.upsert(
            MotionArtworkEntity(
                identityKey = artwork.identityKey,
                provider = artwork.provider,
                url = artwork.url,
                mimeType = artwork.mimeType,
                width = artwork.width,
                height = artwork.height,
                confidence = artwork.confidence,
                expiresAt = artwork.expiresAtMs,
                lastVerifiedAt = artwork.lastVerifiedAtMs,
                configEpoch = artwork.configEpoch,
                negative = false
            )
        )
        dao.prune(MAX_CACHE_ENTRIES)
    }

    suspend fun saveNegative(identityKey: String, configEpoch: Long, expiresAt: Long) {
        dao.upsert(
            MotionArtworkEntity(
                identityKey = identityKey,
                provider = null,
                url = null,
                mimeType = null,
                width = null,
                height = null,
                confidence = 0,
                expiresAt = expiresAt,
                lastVerifiedAt = clock(),
                configEpoch = configEpoch,
                negative = true
            )
        )
        dao.prune(MAX_CACHE_ENTRIES)
    }

    suspend fun cleanup(configEpoch: Long) {
        dao.deleteExpiredOrObsolete(clock(), configEpoch)
        dao.prune(MAX_CACHE_ENTRIES)
    }

    private companion object {
        const val MAX_CACHE_ENTRIES = 240
    }
}

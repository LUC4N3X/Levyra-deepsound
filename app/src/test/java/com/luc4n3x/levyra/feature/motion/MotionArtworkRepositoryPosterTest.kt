package com.luc4n3x.levyra.feature.motion

import com.luc4n3x.levyra.data.local.MotionArtworkDao
import com.luc4n3x.levyra.data.local.MotionArtworkEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionArtworkRepositoryPosterTest {

    private class FakeDao : MotionArtworkDao {
        val rows = mutableMapOf<String, MotionArtworkEntity>()
        override suspend fun get(identityKey: String): MotionArtworkEntity? = rows[identityKey]
        override suspend fun upsert(entity: MotionArtworkEntity) { rows[entity.identityKey] = entity }
        override suspend fun delete(identityKey: String) { rows.remove(identityKey) }
        override suspend fun deleteExpiredOrObsolete(now: Long, configEpoch: Long) = Unit
        override suspend fun prune(maxEntries: Int) = Unit
    }

    private fun entity(provider: String, posterUrl: String?) = MotionArtworkEntity(
        identityKey = "key", provider = provider, url = "https://example.invalid/v.m3u8", mimeType = "application/x-mpegURL",
        width = 1080, height = 1440, confidence = 90, expiresAt = 10_000L, lastVerifiedAt = 1L, configEpoch = 3L,
        negative = false, posterUrl = posterUrl
    )

    @Test
    fun applePosterRoundTripsThroughTheCache() = runBlocking {
        val dao = FakeDao()
        val repository = MotionArtworkRepository(dao) { 5L }
        dao.upsert(entity("apple-motion", "https://is1-ssl.mzstatic.com/p/1080x1440bb.jpg"))
        val hit = repository.get("key", 3L) as MotionArtworkCacheResult.Hit
        assertEquals("https://is1-ssl.mzstatic.com/p/1080x1440bb.jpg", hit.artwork.posterUrl)
    }

    @Test
    fun appleEntryCachedBeforePostersIsResolvedAgainOnce() = runBlocking {
        val dao = FakeDao()
        val repository = MotionArtworkRepository(dao) { 5L }
        dao.upsert(entity("apple-motion", null))
        assertEquals(MotionArtworkCacheResult.Miss, repository.get("key", 3L))
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun otherProvidersKeepWorkingWithoutAPoster() = runBlocking {
        val dao = FakeDao()
        val repository = MotionArtworkRepository(dao) { 5L }
        dao.upsert(entity("community-canvas", null))
        val hit = repository.get("key", 3L) as MotionArtworkCacheResult.Hit
        assertNull(hit.artwork.posterUrl)
    }

    @Test
    fun appleVideoWithoutPreviewFrameIsCachedWithoutResolvingAgain() = runBlocking {
        val dao = FakeDao()
        val repository = MotionArtworkRepository(dao) { 5L }
        repository.save(
            MotionArtwork(
                identityKey = "key", provider = "apple-motion", url = "https://example.invalid/v.m3u8",
                mimeType = "application/x-mpegURL", width = null, height = null, confidence = 90,
                expiresAtMs = 10_000L, lastVerifiedAtMs = 1L, configEpoch = 3L, posterUrl = null
            )
        )
        val hit = repository.get("key", 3L) as MotionArtworkCacheResult.Hit
        assertNull(hit.artwork.posterUrl)
        assertEquals(1, dao.rows.size)
    }
}

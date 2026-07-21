package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.data.local.PlaybackSourceMatchDao
import com.luc4n3x.levyra.data.local.PlaybackSourceMatchEntity
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest
import com.luc4n3x.levyra.domain.Track

internal data class StoredPlaybackSourceMatch(
    val entity: PlaybackSourceMatchEntity,
    val manifest: ResolvedPlaybackManifest?
)

internal class PlaybackSourceMatchStore(
    private val dao: PlaybackSourceMatchDao
) {
    suspend fun load(
        track: Track,
        videoMode: Boolean,
        audioQuality: String,
        preferMp4Audio: Boolean = false
    ): StoredPlaybackSourceMatch? {
        val matchKey = PlaybackSourceIdentity.matchKey(track, videoMode, audioQuality, preferMp4Audio)
        val entity = dao.get(matchKey) ?: return null
        return StoredPlaybackSourceMatch(entity, PlaybackManifestCodec.decode(entity.manifestJson))
    }

    suspend fun save(
        original: Track,
        resolved: Track,
        videoMode: Boolean,
        audioQuality: String,
        confidence: Int,
        preferMp4Audio: Boolean = false
    ) {
        val manifest = resolved.playbackManifest ?: return
        val sourceVideoId = manifest.sourceVideoId.ifBlank { PlaybackSourceIdentity.sourceVideoId(resolved) }
        if (sourceVideoId.isBlank()) return
        val now = System.currentTimeMillis()
        val matchKey = PlaybackSourceIdentity.matchKey(original, videoMode, audioQuality, preferMp4Audio)
        val previous = dao.get(matchKey)
        dao.upsert(
            PlaybackSourceMatchEntity(
                matchKey = matchKey,
                canonicalKey = PlaybackSourceIdentity.canonicalKey(original),
                mode = when {
                    videoMode -> "video"
                    preferMp4Audio -> "audio-mp4"
                    else -> "audio"
                },
                audioQuality = audioQuality,
                sourceVideoId = sourceVideoId,
                sourceVideoUrl = resolved.videoUrl.ifBlank { "https://www.youtube.com/watch?v=$sourceVideoId" },
                provider = manifest.provider,
                manifestJson = PlaybackManifestCodec.encode(manifest.compact()),
                confidence = confidence.coerceIn(0, 100),
                successCount = (previous?.successCount ?: 0) + 1,
                failureCount = previous?.failureCount?.let { (it - 1).coerceAtLeast(0) } ?: 0,
                blockedUntil = 0L,
                createdAt = previous?.createdAt ?: now,
                updatedAt = now,
                lastValidatedAt = now
            )
        )
    }

    suspend fun recordSuccess(
        track: Track,
        videoMode: Boolean,
        audioQuality: String,
        preferMp4Audio: Boolean = false
    ) {
        val matchKey = PlaybackSourceIdentity.matchKey(track, videoMode, audioQuality, preferMp4Audio)
        dao.recordSuccess(matchKey, System.currentTimeMillis())
    }

    suspend fun recordFailure(
        track: Track,
        videoMode: Boolean,
        audioQuality: String,
        quarantineMs: Long,
        preferMp4Audio: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        dao.recordFailure(
            PlaybackSourceIdentity.matchKey(track, videoMode, audioQuality, preferMp4Audio),
            now + quarantineMs.coerceAtLeast(0L),
            now
        )
    }

    suspend fun delete(
        track: Track,
        videoMode: Boolean,
        audioQuality: String,
        preferMp4Audio: Boolean = false
    ) {
        dao.delete(PlaybackSourceIdentity.matchKey(track, videoMode, audioQuality, preferMp4Audio))
    }
}

package com.luc4n3x.levyra.player.queue

import android.content.Context
import com.luc4n3x.levyra.data.TrackPayloadCodec
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.PlaybackQueueItemEntity
import com.luc4n3x.levyra.data.local.PlaybackQueueStateEntity
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track

internal class PlaybackQueueStore(context: Context) {
    private val dao = LevyraDatabase.get(context.applicationContext).playbackQueueDao()

    suspend fun load(): PersistentQueueSnapshot? {
        val state = dao.state() ?: return null
        val tracks = dao.items().mapNotNull { TrackPayloadCodec.decode(it.payload) }
        if (tracks.isEmpty()) return null
        return PersistentQueueSnapshot(
            tracks = tracks,
            currentIndex = state.currentIndex.coerceIn(0, tracks.lastIndex),
            positionMs = state.positionMs.coerceAtLeast(0L),
            shuffleEnabled = state.shuffleEnabled,
            shuffleOrder = decodeIndices(state.shuffleOrder).filter { it in tracks.indices }.distinct(),
            shuffleCursor = state.shuffleCursor,
            history = decodeIndices(state.history).filter { it in tracks.indices },
            repeatMode = runCatching { RepeatMode.valueOf(state.repeatMode) }.getOrDefault(RepeatMode.Off),
            radioEnabled = state.radioEnabled,
            generation = state.generation.coerceAtLeast(1L),
            updatedAt = state.updatedAt
        )
    }

    suspend fun updatePosition(positionMs: Long, updatedAt: Long) {
        dao.updatePosition(positionMs.coerceAtLeast(0L), updatedAt)
    }

    suspend fun save(snapshot: PersistentQueueSnapshot) {
        val items = snapshot.tracks.mapIndexed { index, track ->
            PlaybackQueueItemEntity(
                position = index,
                payload = TrackPayloadCodec.encode(track.queueStoredCopy()),
                identity = playbackQueueIdentity(track)
            )
        }
        dao.replace(
            items = items,
            state = PlaybackQueueStateEntity(
                currentIndex = snapshot.currentIndex,
                positionMs = snapshot.positionMs,
                shuffleEnabled = snapshot.shuffleEnabled,
                shuffleOrder = snapshot.shuffleOrder.joinToString(","),
                shuffleCursor = snapshot.shuffleCursor,
                history = snapshot.history.takeLast(200).joinToString(","),
                repeatMode = snapshot.repeatMode.name,
                radioEnabled = snapshot.radioEnabled,
                generation = snapshot.generation,
                updatedAt = snapshot.updatedAt
            )
        )
    }

    private fun decodeIndices(value: String): List<Int> = value
        .split(',')
        .mapNotNull { it.trim().toIntOrNull() }
}

internal data class PersistentQueueSnapshot(
    val tracks: List<Track>,
    val currentIndex: Int,
    val positionMs: Long,
    val shuffleEnabled: Boolean,
    val shuffleOrder: List<Int>,
    val shuffleCursor: Int,
    val history: List<Int>,
    val repeatMode: RepeatMode,
    val radioEnabled: Boolean,
    val generation: Long,
    val updatedAt: Long
)

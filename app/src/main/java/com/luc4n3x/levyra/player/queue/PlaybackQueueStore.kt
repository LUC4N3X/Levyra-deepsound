package com.luc4n3x.levyra.player.queue

import android.content.Context
import com.luc4n3x.levyra.data.TrackPayloadCodec
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.PlaybackQueueDao
import com.luc4n3x.levyra.data.local.PlaybackQueueItemEntity
import com.luc4n3x.levyra.data.local.PlaybackQueueStateEntity
import com.luc4n3x.levyra.data.local.QueueSpaceEntity
import com.luc4n3x.levyra.data.local.QueueSpaceSummaryValues
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal interface QueueSpaceStorage {
    val spaces: Flow<List<QueueSpaceSummary>>
    suspend fun activeSpaceId(): String
    suspend fun load(spaceId: String): PersistentQueueSnapshot?
    suspend fun save(snapshot: PersistentQueueSnapshot): Boolean
    suspend fun updatePosition(spaceId: String, positionMs: Long, updatedAt: Long)
    suspend fun activate(spaceId: String): Boolean
    suspend fun create(name: String): QueueSpaceSummary
    suspend fun rename(spaceId: String, name: String): Boolean
    suspend fun duplicate(sourceId: String, name: String): QueueSpaceSummary?
    suspend fun clear(spaceId: String)
    suspend fun delete(spaceId: String): Boolean
}

internal class PlaybackQueueStore(private val dao: PlaybackQueueDao) : QueueSpaceStorage {
    constructor(context: Context) : this(LevyraDatabase.get(context.applicationContext).playbackQueueDao())

    override val spaces: Flow<List<QueueSpaceSummary>> = dao.observeSpaces()
        .map { rows -> rows.map(QueueSpaceEntity::toSummary) }
        .distinctUntilChanged()

    override suspend fun activeSpaceId(): String =
        dao.ensureActiveSpace(fallbackName = "", now = System.currentTimeMillis()).id

    override suspend fun load(spaceId: String): PersistentQueueSnapshot? {
        if (dao.space(spaceId) == null) return null
        val state = dao.state(spaceId)
        val tracks = dao.items(spaceId).mapNotNull { TrackPayloadCodec.decode(it.payload) }
        if (state == null || tracks.isEmpty()) return emptyPersistentSnapshot(spaceId, state)
        return PersistentQueueSnapshot(
            spaceId = spaceId,
            tracks = tracks,
            currentIndex = state.currentIndex.coerceIn(0, tracks.lastIndex),
            positionMs = state.positionMs.coerceAtLeast(0L),
            shuffleEnabled = state.shuffleEnabled,
            shuffleOrder = decodeQueueIndices(state.shuffleOrder).filter { it in tracks.indices }.distinct(),
            shuffleCursor = state.shuffleCursor,
            history = decodeQueueIndices(state.history).filter { it in tracks.indices },
            repeatMode = runCatching { RepeatMode.valueOf(state.repeatMode) }.getOrDefault(RepeatMode.Off),
            radioEnabled = state.radioEnabled,
            generation = state.generation.coerceAtLeast(1L),
            updatedAt = state.updatedAt
        )
    }

    override suspend fun save(snapshot: PersistentQueueSnapshot): Boolean {
        val items = snapshot.tracks.mapIndexed { index, track ->
            PlaybackQueueItemEntity(
                spaceId = snapshot.spaceId,
                position = index,
                payload = TrackPayloadCodec.encode(track.queueStoredCopy()),
                identity = playbackQueueIdentity(track)
            )
        }
        return dao.replace(
            spaceId = snapshot.spaceId,
            items = items,
            state = PlaybackQueueStateEntity(
                spaceId = snapshot.spaceId,
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
            ),
            summary = queueSpaceSummaryValues(snapshot.tracks, snapshot.updatedAt)
        )
    }

    override suspend fun updatePosition(spaceId: String, positionMs: Long, updatedAt: Long) {
        dao.updatePosition(spaceId, positionMs.coerceAtLeast(0L), updatedAt)
    }

    override suspend fun activate(spaceId: String): Boolean {
        if (dao.space(spaceId) == null) return false
        dao.markActive(spaceId, System.currentTimeMillis())
        return true
    }

    override suspend fun create(name: String): QueueSpaceSummary {
        val entity = newSpaceEntity(name, System.currentTimeMillis())
        dao.insertSpace(entity)
        return entity.toSummary()
    }

    override suspend fun rename(spaceId: String, name: String): Boolean =
        dao.renameSpace(spaceId, normalizeQueueSpaceName(name), System.currentTimeMillis()) > 0

    override suspend fun duplicate(sourceId: String, name: String): QueueSpaceSummary? {
        val source = dao.space(sourceId) ?: return null
        val entity = newSpaceEntity(name, System.currentTimeMillis()).copy(
            trackCount = source.trackCount,
            durationMs = source.durationMs,
            artworkUrls = source.artworkUrls
        )
        return if (dao.duplicateSpace(sourceId, entity)) entity.toSummary() else null
    }

    override suspend fun clear(spaceId: String) {
        dao.clearSpaceContent(spaceId, System.currentTimeMillis())
    }

    override suspend fun delete(spaceId: String): Boolean = dao.deleteSpace(spaceId)

    private fun newSpaceEntity(name: String, now: Long) = QueueSpaceEntity(
        id = UUID.randomUUID().toString(),
        name = normalizeQueueSpaceName(name),
        createdAt = now,
        updatedAt = now,
        lastActiveAt = now,
        isActive = false,
        trackCount = 0,
        durationMs = 0L,
        artworkUrls = ""
    )

    private fun emptyPersistentSnapshot(spaceId: String, state: PlaybackQueueStateEntity?) = PersistentQueueSnapshot(
        spaceId = spaceId,
        tracks = emptyList(),
        currentIndex = -1,
        positionMs = 0L,
        shuffleEnabled = false,
        shuffleOrder = emptyList(),
        shuffleCursor = -1,
        history = emptyList(),
        repeatMode = state?.repeatMode?.let { runCatching { RepeatMode.valueOf(it) }.getOrNull() } ?: RepeatMode.Off,
        radioEnabled = state?.radioEnabled ?: false,
        generation = state?.generation?.coerceAtLeast(1L) ?: 1L,
        updatedAt = state?.updatedAt ?: 0L
    )
}

internal fun decodeQueueIndices(value: String): List<Int> = value
    .split(',')
    .mapNotNull { it.trim().toIntOrNull() }

internal fun normalizeQueueSpaceName(name: String): String =
    name.replace(QUEUE_SPACE_NAME_WHITESPACE, " ").trim().take(MAX_QUEUE_SPACE_NAME_LENGTH)

internal fun queueSpaceSummaryValues(tracks: List<Track>, updatedAt: Long): QueueSpaceSummaryValues {
    val artwork = tracks.asSequence()
        .map { it.thumbnailUrl.ifBlank { it.largeThumbnailUrl }.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .take(QUEUE_SPACE_COLLAGE_SIZE)
        .joinToString("\n")
    return QueueSpaceSummaryValues(
        trackCount = tracks.size,
        durationMs = tracks.sumOf { it.durationMs.coerceAtLeast(0L) },
        artworkUrls = artwork,
        updatedAt = updatedAt
    )
}

private fun QueueSpaceEntity.toSummary() = QueueSpaceSummary(
    id = id,
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastActiveAt = lastActiveAt,
    isActive = isActive,
    trackCount = trackCount,
    durationMs = durationMs,
    artworkUrls = artworkUrls.split('\n').filter { it.isNotBlank() }
)

private val QUEUE_SPACE_NAME_WHITESPACE = Regex("\\s+")
private const val MAX_QUEUE_SPACE_NAME_LENGTH = 48
private const val QUEUE_SPACE_COLLAGE_SIZE = 4

data class QueueSpaceSummary(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastActiveAt: Long,
    val isActive: Boolean,
    val trackCount: Int,
    val durationMs: Long,
    val artworkUrls: List<String>
)

internal data class PersistentQueueSnapshot(
    val spaceId: String,
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

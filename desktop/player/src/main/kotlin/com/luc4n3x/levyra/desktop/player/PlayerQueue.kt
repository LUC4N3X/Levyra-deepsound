package com.luc4n3x.levyra.desktop.player

import com.luc4n3x.levyra.desktop.core.model.Track
import kotlin.random.Random

data class PlayerQueue(
    val items: List<Track> = emptyList(),
    val original: List<Track> = emptyList(),
    val index: Int = -1,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF
) {
    val current: Track? get() = items.getOrNull(index)

    val isEmpty: Boolean get() = items.isEmpty()

    val upcoming: List<Track> get() = if (index < 0) items else items.drop(index + 1)

    val hasNext: Boolean
        get() = when {
            items.isEmpty() -> false
            repeat != RepeatMode.OFF -> true
            else -> index + 1 < items.size
        }

    val hasPrevious: Boolean
        get() = when {
            items.isEmpty() -> false
            repeat == RepeatMode.ALL -> true
            else -> index > 0
        }

    fun replace(tracks: List<Track>, startIndex: Int = 0, random: Random = Random.Default): PlayerQueue {
        val unique = tracks.distinctBy { it.id }
        if (unique.isEmpty()) return copy(items = emptyList(), original = emptyList(), index = -1)
        val safeStart = startIndex.coerceIn(0, unique.lastIndex)
        return if (shuffle) {
            val head = unique[safeStart]
            val rest = unique.filterIndexed { position, _ -> position != safeStart }.shuffled(random)
            copy(items = listOf(head) + rest, original = unique, index = 0)
        } else {
            copy(items = unique, original = unique, index = safeStart)
        }
    }

    fun withShuffle(enabled: Boolean, random: Random = Random.Default): PlayerQueue {
        if (enabled == shuffle) return this
        if (items.isEmpty()) return copy(shuffle = enabled)
        val playing = current
        return if (enabled) {
            val rest = items.filter { it.id != playing?.id }.shuffled(random)
            copy(
                shuffle = true,
                items = listOfNotNull(playing) + rest,
                index = 0
            )
        } else {
            val restored = original.filter { track -> items.any { it.id == track.id } }
            val restoredIndex = restored.indexOfFirst { it.id == playing?.id }
            copy(
                shuffle = false,
                items = restored,
                index = if (restoredIndex >= 0) restoredIndex else 0
            )
        }
    }

    fun withRepeat(mode: RepeatMode): PlayerQueue = copy(repeat = mode)

    fun advance(automatic: Boolean): PlayerQueue? {
        if (items.isEmpty()) return null
        if (automatic && repeat == RepeatMode.ONE) return this
        val nextIndex = index + 1
        return when {
            nextIndex < items.size -> copy(index = nextIndex)
            repeat == RepeatMode.ALL || repeat == RepeatMode.ONE -> copy(index = 0)
            else -> null
        }
    }

    fun rewind(): PlayerQueue? {
        if (items.isEmpty()) return null
        return when {
            index > 0 -> copy(index = index - 1)
            repeat == RepeatMode.ALL -> copy(index = items.lastIndex)
            else -> null
        }
    }

    fun jumpTo(position: Int): PlayerQueue {
        if (position !in items.indices) return this
        return copy(index = position)
    }

    fun jumpToTrack(trackId: String): PlayerQueue {
        val position = items.indexOfFirst { it.id == trackId }
        return if (position >= 0) copy(index = position) else this
    }

    fun enqueueNext(tracks: List<Track>): PlayerQueue {
        val additions = tracks.distinctBy { it.id }.filterNot { track -> items.any { it.id == track.id } }
        if (additions.isEmpty()) return this
        if (items.isEmpty()) return replace(additions)
        val insertAt = (index + 1).coerceIn(0, items.size)
        return copy(
            items = items.toMutableList().apply { addAll(insertAt, additions) },
            original = original + additions
        )
    }

    fun enqueueLast(tracks: List<Track>): PlayerQueue {
        val additions = tracks.distinctBy { it.id }.filterNot { track -> items.any { it.id == track.id } }
        if (additions.isEmpty()) return this
        if (items.isEmpty()) return replace(additions)
        return copy(items = items + additions, original = original + additions)
    }

    fun removeAt(position: Int): PlayerQueue {
        if (position !in items.indices) return this
        val removed = items[position]
        val updatedItems = items.toMutableList().apply { removeAt(position) }
        val updatedOriginal = original.filterNot { it.id == removed.id }
        if (updatedItems.isEmpty()) {
            return copy(items = emptyList(), original = emptyList(), index = -1)
        }
        val updatedIndex = when {
            position < index -> index - 1
            position == index -> index.coerceAtMost(updatedItems.lastIndex)
            else -> index
        }
        return copy(items = updatedItems, original = updatedOriginal, index = updatedIndex)
    }

    fun clear(): PlayerQueue = copy(items = emptyList(), original = emptyList(), index = -1)
}

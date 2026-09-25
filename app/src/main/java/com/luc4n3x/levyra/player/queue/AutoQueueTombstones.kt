package com.luc4n3x.levyra.player.queue

import com.luc4n3x.levyra.domain.Track

internal class AutoQueueTombstones(
    private val maxKeysPerSpace: Int = MAX_KEYS_PER_SPACE,
    private val maxSpaces: Int = MAX_SPACES
) {
    private val automatic = LinkedHashMap<String, LinkedHashSet<String>>()
    private val rejected = LinkedHashMap<String, LinkedHashSet<String>>()

    fun markAutomatic(spaceId: String, tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val keys = bucket(automatic, spaceId)
        tracks.forEach { track -> keys.addBounded(playbackQueueIdentity(track)) }
    }

    fun markManual(spaceId: String, tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val automaticKeys = automatic[spaceId]
        val rejectedKeys = rejected[spaceId]
        tracks.forEach { track ->
            automaticKeys?.remove(playbackQueueIdentity(track))
            rejectedKeys?.removeAll(rejectionKeys(track).toSet())
        }
    }

    fun isAutomatic(spaceId: String, track: Track): Boolean =
        automatic[spaceId]?.contains(playbackQueueIdentity(track)) == true

    fun recordRemoval(spaceId: String, tracks: List<Track>) {
        val automaticKeys = automatic[spaceId] ?: return
        tracks.forEach { track ->
            if (automaticKeys.remove(playbackQueueIdentity(track))) {
                val keys = bucket(rejected, spaceId)
                rejectionKeys(track).forEach { keys.addBounded(it) }
            }
        }
    }

    fun undoRemoval(spaceId: String, track: Track, wasAutomatic: Boolean) {
        if (!wasAutomatic) return
        rejected[spaceId]?.removeAll(rejectionKeys(track).toSet())
        bucket(automatic, spaceId).addBounded(playbackQueueIdentity(track))
    }

    fun isRejected(spaceId: String, track: Track): Boolean {
        val keys = rejected[spaceId] ?: return false
        return rejectionKeys(track).any(keys::contains)
    }

    fun rejectedKeys(spaceId: String): Set<String> = rejected[spaceId]?.toSet().orEmpty()

    fun resetAutomatic(spaceId: String) {
        automatic.remove(spaceId)
    }

    fun clear(spaceId: String) {
        automatic.remove(spaceId)
        rejected.remove(spaceId)
    }

    private fun bucket(target: LinkedHashMap<String, LinkedHashSet<String>>, spaceId: String): LinkedHashSet<String> {
        target.remove(spaceId)?.let { existing ->
            target[spaceId] = existing
            return existing
        }
        while (target.size >= maxSpaces.coerceAtLeast(1)) {
            target.remove(target.keys.first())
        }
        return LinkedHashSet<String>().also { target[spaceId] = it }
    }

    private fun LinkedHashSet<String>.addBounded(key: String) {
        if (key.isBlank()) return
        remove(key)
        add(key)
        while (size > maxKeysPerSpace.coerceAtLeast(1)) {
            remove(first())
        }
    }

    companion object {
        const val MAX_KEYS_PER_SPACE = 160
        const val MAX_SPACES = 8

        fun rejectionKeys(track: Track): List<String> =
            listOf(playbackQueueIdentity(track), "title:${radioTitleKey(track)}")
    }
}

package com.luc4n3x.levyra.data.locallibrary

import com.luc4n3x.levyra.data.local.LocalMediaEntity
import java.util.Locale

internal data class LocalLibraryPlan(
    val inserts: List<LocalMediaEntity>,
    val updates: List<LocalMediaEntity>,
    val missingIds: List<Long>,
    val deleteIds: List<Long>,
    val movedCount: Int
) {
    val isEmpty: Boolean
        get() = inserts.isEmpty() && updates.isEmpty() && missingIds.isEmpty() && deleteIds.isEmpty()
}

internal fun planLocalLibraryReconcile(
    existing: List<LocalMediaEntity>,
    scanned: List<LocalMediaEntity>,
    mountedVolumes: Set<String>,
    fullScan: Boolean,
    now: Long
): LocalLibraryPlan {
    val mounted = mountedVolumes.mapTo(hashSetOf()) { it.lowercase(Locale.ROOT) }
    val existingByKey = existing.associateBy { it.identityKey }
    val seenIds = HashSet<Long>(existing.size)
    val updates = ArrayList<LocalMediaEntity>()
    val fresh = LinkedHashMap<String, LocalMediaEntity>()

    for (candidate in scanned) {
        val previous = existingByKey[candidate.identityKey]
        if (previous == null) {
            fresh.putIfAbsent(candidate.identityKey, candidate)
            continue
        }
        if (!seenIds.add(previous.id)) continue
        val merged = mergeLocalRow(previous, candidate, now)
        if (fullScan || !sameLocalContent(previous, merged)) updates += merged
    }

    val vanished = existing.filter { it.id !in seenIds }
    val vanishedOnMounted = vanished.filter { it.volumeName.lowercase(Locale.ROOT) in mounted }
    val moves = pairMovedLocalFiles(vanishedOnMounted, fresh.values)
    moves.forEach { (previous, candidate) ->
        fresh.remove(candidate.identityKey)
        updates += mergeLocalRow(previous, candidate, now)
    }
    val movedIds = moves.mapTo(hashSetOf()) { it.first.id }

    val missingIds = ArrayList<Long>()
    val deleteIds = ArrayList<Long>()
    for (row in vanished) {
        if (row.id in movedIds) continue
        val volumeMounted = row.volumeName.lowercase(Locale.ROOT) in mounted
        when {
            fullScan && volumeMounted -> deleteIds += row.id
            row.available -> missingIds += row.id
        }
    }

    return LocalLibraryPlan(
        inserts = fresh.values.toList(),
        updates = updates,
        missingIds = missingIds,
        deleteIds = deleteIds,
        movedCount = moves.size
    )
}

private fun mergeLocalRow(previous: LocalMediaEntity, candidate: LocalMediaEntity, now: Long): LocalMediaEntity {
    val levyraTrackId = candidate.levyraTrackId.ifEmpty { previous.levyraTrackId }
    return candidate.copy(
        id = previous.id,
        levyraTrackId = levyraTrackId,
        isLevyraDownload = candidate.isLevyraDownload || levyraTrackId.isNotEmpty(),
        available = true,
        missingSince = 0L,
        lastSeenAt = if (sameLocalFields(previous, candidate, levyraTrackId)) previous.lastSeenAt else now
    )
}

private fun sameLocalFields(previous: LocalMediaEntity, candidate: LocalMediaEntity, levyraTrackId: String): Boolean =
    previous.available &&
        previous.copy(
            id = 0L,
            lastSeenAt = 0L,
            levyraTrackId = "",
            isLevyraDownload = false
        ) == candidate.copy(
            id = 0L,
            lastSeenAt = 0L,
            levyraTrackId = "",
            isLevyraDownload = false
        ) &&
        previous.levyraTrackId == levyraTrackId &&
        previous.isLevyraDownload == (candidate.isLevyraDownload || levyraTrackId.isNotEmpty())

private fun sameLocalContent(previous: LocalMediaEntity, merged: LocalMediaEntity): Boolean = previous == merged

private fun pairMovedLocalFiles(
    vanished: List<LocalMediaEntity>,
    fresh: Collection<LocalMediaEntity>
): List<Pair<LocalMediaEntity, LocalMediaEntity>> {
    if (vanished.isEmpty() || fresh.isEmpty()) return emptyList()
    val vanishedByFingerprint = vanished
        .filter { it.contentFingerprint.isNotEmpty() }
        .groupBy { it.contentFingerprint }
        .filterValues { it.size == 1 }
    if (vanishedByFingerprint.isEmpty()) return emptyList()
    val freshByFingerprint = fresh
        .filter { it.contentFingerprint.isNotEmpty() }
        .groupBy { it.contentFingerprint }
        .filterValues { it.size == 1 }
    return vanishedByFingerprint.mapNotNull { (fingerprint, previous) ->
        val candidate = freshByFingerprint[fingerprint]?.single() ?: return@mapNotNull null
        val old = previous.single()
        if (!old.volumeName.equals(candidate.volumeName, ignoreCase = true)) return@mapNotNull null
        old to candidate
    }
}

internal fun preferredLocalCopies(rows: List<LocalMediaEntity>): List<LocalMediaEntity> {
    if (rows.size < 2) return rows
    val chosen = HashMap<String, LocalMediaEntity>()
    var fingerprinted = 0
    for (row in rows) {
        val key = row.contentFingerprint
        if (key.isEmpty()) continue
        fingerprinted++
        val current = chosen[key]
        if (current == null || prefersLocalCopy(row, current)) chosen[key] = row
    }
    if (chosen.size == fingerprinted) return rows
    return rows.filter { row -> row.contentFingerprint.isEmpty() || chosen[row.contentFingerprint]?.id == row.id }
}

private fun prefersLocalCopy(candidate: LocalMediaEntity, current: LocalMediaEntity): Boolean = when {
    candidate.isLevyraDownload != current.isLevyraDownload -> candidate.isLevyraDownload
    candidate.dateModifiedMs != current.dateModifiedMs -> candidate.dateModifiedMs > current.dateModifiedMs
    else -> candidate.id < current.id
}

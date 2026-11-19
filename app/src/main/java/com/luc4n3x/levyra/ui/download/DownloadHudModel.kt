package com.luc4n3x.levyra.ui.download

import com.luc4n3x.levyra.domain.OfflineDownloadTask

private val HUD_ACTIVE_STATES = setOf("QUEUED", "RUNNING", "RETRYING")

internal data class DownloadHudSnapshot(
    val current: OfflineDownloadTask,
    val currentProgress: Int,
    val completed: Int,
    val total: Int,
    val overallProgress: Int,
    val queued: List<OfflineDownloadTask>
)

internal fun activeHudTasks(queue: List<OfflineDownloadTask>): List<OfflineDownloadTask> =
    queue.filter { it.state in HUD_ACTIVE_STATES }

internal fun extendDownloadSession(session: Map<String, String>, active: List<OfflineDownloadTask>): Map<String, String> =
    if (active.isEmpty()) emptyMap() else session + active.associate { it.taskKey to it.trackId }

internal fun downloadHudSnapshot(
    active: List<OfflineDownloadTask>,
    session: Map<String, String>,
    savedTrackIds: Set<String>
): DownloadHudSnapshot? {
    if (active.isEmpty()) return null
    val current = active.filter { it.state == "RUNNING" }.maxByOrNull { it.progress } ?: active.first()
    val activeKeys = active.mapTo(HashSet()) { it.taskKey }
    val all = session + active.associate { it.taskKey to it.trackId }
    val total = all.size
    val completed = all.count { (key, trackId) -> key !in activeKeys && trackId in savedTrackIds }
    val activeProgress = active.sumOf { it.progress.coerceIn(0, 100) }
    val overall = ((completed * 100 + activeProgress) / total).coerceIn(0, 99)
    return DownloadHudSnapshot(
        current = current,
        currentProgress = current.progress.coerceIn(0, 99),
        completed = completed,
        total = total,
        overallProgress = overall,
        queued = active.filter { it.taskKey != current.taskKey }
    )
}

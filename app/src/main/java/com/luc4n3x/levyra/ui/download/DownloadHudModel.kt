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

internal fun extendDownloadSession(session: Set<String>, active: List<OfflineDownloadTask>): Set<String> =
    if (active.isEmpty()) emptySet() else session + active.map { it.taskKey }

internal fun downloadHudSnapshot(active: List<OfflineDownloadTask>, session: Set<String>): DownloadHudSnapshot? {
    if (active.isEmpty()) return null
    val current = active.filter { it.state == "RUNNING" }.maxByOrNull { it.progress } ?: active.first()
    val total = (session + active.map { it.taskKey }).size
    val completed = (total - active.size).coerceAtLeast(0)
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

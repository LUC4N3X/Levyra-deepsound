package com.luc4n3x.levyra.domain

import androidx.compose.runtime.Immutable

@Immutable
data class OfflineLaneProgress(
    val total: Int,
    val completed: Int,
    val percent: Int
) {
    companion object {
        val Empty = OfflineLaneProgress(0, 0, 0)

        fun of(total: Int, completed: Int, progressSum: Int): OfflineLaneProgress {
            if (total <= 0) return Empty
            val safeCompleted = completed.coerceIn(0, total)
            val percent = (progressSum.coerceIn(0, total * 100) / total).coerceIn(0, 100)
            return OfflineLaneProgress(total, safeCompleted, percent)
        }
    }
}

@Immutable
data class PreloadQueueItem(
    val key: String,
    val title: String,
    val artist: String,
    val progress: Int,
    val complete: Boolean
)

fun List<PreloadQueueItem>.preloadLaneProgress(): OfflineLaneProgress = OfflineLaneProgress.of(
    total = size,
    completed = count { it.complete },
    progressSum = sumOf { if (it.complete) 100 else it.progress.coerceIn(0, 100) }
)

@Immutable
data class OfflineTransferState(
    val preload: List<PreloadQueueItem> = emptyList(),
    val lanes: Map<DownloadOwnership, OfflineLaneProgress> = emptyMap()
)

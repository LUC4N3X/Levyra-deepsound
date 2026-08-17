package com.luc4n3x.levyra.domain

import java.util.Locale

enum class BatchDownloadKind {
    Album,
    Playlist
}

enum class BatchDownloadState {
    Queued,
    Downloading,
    Completed,
    Failed,
    Cancelled
}

data class BatchDownload(
    val key: String,
    val kind: BatchDownloadKind,
    val title: String,
    val artworkUrl: String,
    val total: Int,
    val completed: Int,
    val failed: Int,
    val active: Int,
    val progress: Int,
    val state: BatchDownloadState
) {
    val isFinished: Boolean
        get() = state == BatchDownloadState.Completed ||
            state == BatchDownloadState.Failed ||
            state == BatchDownloadState.Cancelled

    val canRetry: Boolean
        get() = failed > 0 && active == 0
}

fun batchDownloadKindOf(raw: String): BatchDownloadKind =
    if (raw.equals(BatchDownloadKind.Playlist.name, ignoreCase = true)) {
        BatchDownloadKind.Playlist
    } else {
        BatchDownloadKind.Album
    }

fun batchDownloadProgress(total: Int, progressSum: Int): Int {
    if (total <= 0) return 0
    return (progressSum.coerceAtLeast(0) / total).coerceIn(0, 100)
}

fun batchDownloadState(
    total: Int,
    completed: Int,
    failed: Int,
    active: Int,
    progressSum: Int
): BatchDownloadState {
    if (total <= 0) return BatchDownloadState.Cancelled
    if (active > 0) {
        return if (progressSum > 0 || completed > 0) BatchDownloadState.Downloading else BatchDownloadState.Queued
    }
    if (completed >= total) return BatchDownloadState.Completed
    if (failed > 0) return BatchDownloadState.Failed
    return BatchDownloadState.Cancelled
}

fun visibleDownloadBatches(batches: List<BatchDownload>): List<BatchDownload> = batches.filterNot {
    it.state == BatchDownloadState.Completed || it.state == BatchDownloadState.Cancelled
}

fun retainsExistingBatchMembership(
    previousBatchKey: String,
    previousState: String,
    requestedBatchKey: String
): Boolean {
    if (previousBatchKey.isBlank()) return false
    if (previousBatchKey == requestedBatchKey) return false
    if (requestedBatchKey.isBlank()) return true
    return previousState.uppercase(Locale.ROOT) in UNSETTLED_BATCH_CHILD_STATES
}

private val UNSETTLED_BATCH_CHILD_STATES = setOf("QUEUED", "RUNNING", "PAUSED", "RETRYING", "FAILED")

fun batchDownloadKey(kind: BatchDownloadKind, canonicalId: String, fallbackTitle: String): String {
    val identity = canonicalId.trim().lowercase(Locale.ROOT).ifBlank {
        fallbackTitle.trim().lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), "-").trim('-')
    }
    if (identity.isBlank()) return ""
    return "${kind.name.lowercase(Locale.ROOT)}:$identity"
}

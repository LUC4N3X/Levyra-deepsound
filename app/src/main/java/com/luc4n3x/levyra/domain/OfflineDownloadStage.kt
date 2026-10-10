package com.luc4n3x.levyra.domain

enum class OfflineDownloadStage {
    Queued,
    Downloading,
    Paused,
    Retrying,
    Failed,
    Completed,
    Cancelled;

    val isActive: Boolean
        get() = this == Queued || this == Downloading || this == Retrying || this == Paused

    val showsProgress: Boolean
        get() = this == Downloading || this == Paused || this == Retrying
}

fun offlineDownloadStageOf(rawState: String): OfflineDownloadStage =
    when (rawState.trim().uppercase()) {
        "QUEUED", "ENQUEUED", "BLOCKED" -> OfflineDownloadStage.Queued
        "RUNNING", "DOWNLOADING" -> OfflineDownloadStage.Downloading
        "PAUSED" -> OfflineDownloadStage.Paused
        "RETRYING" -> OfflineDownloadStage.Retrying
        "FAILED", "ERROR" -> OfflineDownloadStage.Failed
        "SUCCEEDED", "COMPLETED", "DONE" -> OfflineDownloadStage.Completed
        "CANCELLED", "CANCELED" -> OfflineDownloadStage.Cancelled
        else -> OfflineDownloadStage.Queued
    }

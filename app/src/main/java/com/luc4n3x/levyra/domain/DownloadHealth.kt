package com.luc4n3x.levyra.domain

enum class DownloadHealth { Healthy, Missing, Empty, Unreadable, Incomplete, AccessDenied, Unavailable }

fun downloadAudioHealth(size: Long, hasAudio: Boolean, actualMs: Long, expectedMs: Long): DownloadHealth = when {
    size == 0L -> DownloadHealth.Empty
    !hasAudio -> DownloadHealth.Unreadable
    actualMs > 0L && expectedMs > 0L && expectedMs - actualMs > 5_000L &&
        actualMs.toDouble() / expectedMs < 0.85 -> DownloadHealth.Incomplete
    else -> DownloadHealth.Healthy
}

fun canRepairDownload(trackId: String, health: DownloadHealth): Boolean =
    trackId.isNotBlank() && health in setOf(DownloadHealth.Missing, DownloadHealth.Empty, DownloadHealth.Unreadable, DownloadHealth.Incomplete)

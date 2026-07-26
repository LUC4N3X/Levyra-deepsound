package com.luc4n3x.levyra.desktop.core.storage

import com.luc4n3x.levyra.desktop.core.model.Track
import kotlinx.serialization.Serializable

@Serializable
enum class DownloadStatus {
    QUEUED,
    RESOLVING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Serializable
data class DownloadRecord(
    val id: String,
    val track: Track,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val filePath: String = "",
    val temporaryPath: String = "",
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val mediaLabel: String = "",
    val error: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val progress: Float
        get() = if (totalBytes > 0L) {
            (bytesDownloaded.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }

    val isActive: Boolean
        get() = status == DownloadStatus.QUEUED ||
            status == DownloadStatus.RESOLVING ||
            status == DownloadStatus.DOWNLOADING

    val isPlayable: Boolean
        get() = status == DownloadStatus.COMPLETED && filePath.isNotBlank()

    fun playableTrack(): Track = track.copy(
        offlinePath = filePath,
        offlineMediaLabel = mediaLabel
    )
}

@Serializable
data class DownloadData(
    val records: List<DownloadRecord> = emptyList()
)

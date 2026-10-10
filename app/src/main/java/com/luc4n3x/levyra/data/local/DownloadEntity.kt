package com.luc4n3x.levyra.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.luc4n3x.levyra.domain.DownloadOwnership

@Entity(
    tableName = "downloaded_tracks",
    indices = [
        Index(value = ["trackId"]),
        Index(value = ["savedAt"]),
        Index(value = ["trackId", "downloadPreset", "downloadQuality"]),
        Index(value = ["ownership"])
    ]
)
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val fileName: String,
    val uri: String,
    val mimeType: String,
    val embeddedMetadata: Boolean,
    val downloadPreset: String,
    val downloadQuality: String,
    val savedAt: Long,
    @ColumnInfo(defaultValue = "'MANUAL'") val ownership: String = DownloadOwnership.MANUAL.name
)

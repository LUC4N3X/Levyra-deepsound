package com.luc4n3x.levyra.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "downloaded_tracks",
    indices = [Index(value = ["trackId"]), Index(value = ["savedAt"])]
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
    val savedAt: Long
)

package com.luc4n3x.levyra.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_media",
    indices = [
        Index(value = ["identityKey"], unique = true),
        Index(value = ["contentUri"]),
        Index(value = ["folderKey"]),
        Index(value = ["albumKey"]),
        Index(value = ["artistKey"])
    ]
)
data class LocalMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val identityKey: String,
    val contentUri: String,
    val volumeName: String,
    val mediaStoreId: Long,
    val filePath: String,
    val relativePath: String,
    val displayName: String,
    val folderKey: String,
    val folderName: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val genre: String,
    val composer: String,
    val lyricist: String,
    val comment: String,
    val copyright: String,
    val customTags: String,
    val fullTagSearchText: String,
    val year: Int,
    val trackNumber: Int,
    val discNumber: Int,
    val durationMs: Long,
    val mimeType: String,
    val bitrate: Int,
    val sizeBytes: Long,
    val dateAddedMs: Long,
    val dateModifiedMs: Long,
    val albumId: Long,
    val albumKey: String,
    val artistKey: String,
    val contentFingerprint: String,
    val levyraTrackId: String,
    val isLevyraDownload: Boolean,
    val available: Boolean,
    val missingSince: Long,
    val lastSeenAt: Long
)

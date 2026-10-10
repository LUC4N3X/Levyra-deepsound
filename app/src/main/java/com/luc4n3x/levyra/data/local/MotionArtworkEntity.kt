package com.luc4n3x.levyra.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "motion_artwork",
    indices = [
        Index(value = ["expiresAt"]),
        Index(value = ["lastVerifiedAt"])
    ]
)
data class MotionArtworkEntity(
    @PrimaryKey
    val identityKey: String,
    val provider: String?,
    val url: String?,
    val mimeType: String?,
    val width: Int?,
    val height: Int?,
    val confidence: Int,
    val expiresAt: Long,
    val lastVerifiedAt: Long,
    val configEpoch: Long,
    val negative: Boolean
)

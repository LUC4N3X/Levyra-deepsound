package com.luc4n3x.levyra.feature.motion

interface MotionArtworkProvider {
    val id: String

    suspend fun find(identity: MotionTrackIdentity): List<MotionArtworkCandidate>
}

package com.luc4n3x.levyra.feature.motion

sealed interface MotionArtworkProviderResult {
    data class Found(val candidates: List<MotionArtworkCandidate>) : MotionArtworkProviderResult
    data object NoMatch : MotionArtworkProviderResult
    data class Failed(val cause: Throwable? = null) : MotionArtworkProviderResult
}

interface MotionArtworkProvider {
    val id: String

    suspend fun find(identity: MotionTrackIdentity): MotionArtworkProviderResult
}

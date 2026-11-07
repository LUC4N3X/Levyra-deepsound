package com.luc4n3x.levyra.extraction.youtube

sealed interface YoutubePlayerAnalysis {
    data class Candidates(
        val signatureTimestamp: Int,
        val transforms: List<YoutubePlayerTransform>
    ) : YoutubePlayerAnalysis

    data class Unavailable(val reason: Reason) : YoutubePlayerAnalysis

    enum class Reason {
        EMPTY_SOURCE,
        SOURCE_TOO_LARGE,
        MISSING_SIGNATURE_TIMESTAMP,
        MISSING_SIGNATURE_TRANSFORM,
        MISSING_N_TRANSFORM
    }
}

data class YoutubePlayerTransform(
    val signatureExpression: String,
    val nExpression: String,
    val confidence: Int
)

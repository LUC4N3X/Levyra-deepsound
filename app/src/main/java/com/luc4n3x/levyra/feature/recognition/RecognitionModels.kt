package com.luc4n3x.levyra.feature.recognition

data class RecognitionResult(
    val title: String,
    val artist: String,
    val album: String = "",
    val confidence: Int? = null,
    val externalId: String = ""
)

sealed interface RecognitionOutcome {
    data class Match(val result: RecognitionResult) : RecognitionOutcome
    data object NoMatch : RecognitionOutcome
    data class Error(val kind: RecognitionErrorKind) : RecognitionOutcome
}

enum class RecognitionErrorKind {
    PermissionDenied,
    Timeout,
    Network,
    Fingerprint,
    Cancelled,
    Unavailable
}

package com.luc4n3x.levyra.feature.recognition

enum class RecognitionTileProjectionKind {
    Active,
    Inactive
}

enum class RecognitionTileDescription {
    TapToListen,
    Listening,
    Processing,
    Result,
    None
}

data class RecognitionTileProjection(
    val kind: RecognitionTileProjectionKind,
    val description: RecognitionTileDescription
)

fun recognitionTileProjection(state: RecognitionState): RecognitionTileProjection = when (state) {
    RecognitionState.Idle -> RecognitionTileProjection(RecognitionTileProjectionKind.Inactive, RecognitionTileDescription.TapToListen)
    RecognitionState.Listening -> RecognitionTileProjection(RecognitionTileProjectionKind.Active, RecognitionTileDescription.Listening)
    RecognitionState.Identifying -> RecognitionTileProjection(RecognitionTileProjectionKind.Active, RecognitionTileDescription.Processing)
    is RecognitionState.Result -> RecognitionTileProjection(RecognitionTileProjectionKind.Inactive, RecognitionTileDescription.Result)
    RecognitionState.NoMatch -> RecognitionTileProjection(RecognitionTileProjectionKind.Inactive, RecognitionTileDescription.None)
    is RecognitionState.Error -> RecognitionTileProjection(RecognitionTileProjectionKind.Inactive, RecognitionTileDescription.None)
}

package com.luc4n3x.levyra.feature.recognition

import org.junit.Assert.assertEquals
import org.junit.Test

class RecognitionTileProjectionTest {

    @Test
    fun idleProjectsInactiveWithTapToListen() {
        val projection = recognitionTileProjection(RecognitionState.Idle)

        assertEquals(RecognitionTileProjectionKind.Inactive, projection.kind)
        assertEquals(RecognitionTileDescription.TapToListen, projection.description)
    }

    @Test
    fun listeningProjectsActiveWithListening() {
        val projection = recognitionTileProjection(RecognitionState.Listening)

        assertEquals(RecognitionTileProjectionKind.Active, projection.kind)
        assertEquals(RecognitionTileDescription.Listening, projection.description)
    }

    @Test
    fun identifyingProjectsActiveWithProcessing() {
        val projection = recognitionTileProjection(RecognitionState.Identifying)

        assertEquals(RecognitionTileProjectionKind.Active, projection.kind)
        assertEquals(RecognitionTileDescription.Processing, projection.description)
    }

    @Test
    fun resultProjectsInactiveWithResult() {
        val result = RecognitionResult(title = "Song", artist = "Artist")
        val projection = recognitionTileProjection(RecognitionState.Result(result))

        assertEquals(RecognitionTileProjectionKind.Inactive, projection.kind)
        assertEquals(RecognitionTileDescription.Result, projection.description)
    }

    @Test
    fun noMatchProjectsInactiveWithoutDescription() {
        val projection = recognitionTileProjection(RecognitionState.NoMatch)

        assertEquals(RecognitionTileProjectionKind.Inactive, projection.kind)
        assertEquals(RecognitionTileDescription.None, projection.description)
    }

    @Test
    fun permissionErrorProjectsInactiveWithoutDescription() {
        val projection = recognitionTileProjection(RecognitionState.Error(RecognitionErrorKind.PermissionDenied))

        assertEquals(RecognitionTileProjectionKind.Inactive, projection.kind)
        assertEquals(RecognitionTileDescription.None, projection.description)
    }
}

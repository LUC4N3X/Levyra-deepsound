package com.luc4n3x.levyra.data.locallibrary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LocalArtworkModelTest {
    private val uri = "content://media/external/audio/media/42"

    @Test
    fun revisionChangesTheModelSoImageCachesMiss() {
        assertNotEquals(localArtworkModel(uri, 7L, 1_000L), localArtworkModel(uri, 7L, 2_000L))
    }

    @Test
    fun revisionedAndLegacyModelsParseToTheSameMedia() {
        val current = parseLocalArtworkModel(localArtworkModel(uri, 7L, 1_700_000_000_000L))
        val legacy = parseLocalArtworkModel("levyra-local-art://art?a=7&u=$uri")

        assertEquals(LocalArtworkReference(uri, 7L, 1_700_000_000_000L), current)
        assertEquals(LocalArtworkReference(uri, 7L, 0L), legacy)
    }
}

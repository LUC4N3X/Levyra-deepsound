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

    @Test
    fun downloadArtworkAcceptsOnlyKnownDownloadLocations() {
        val privateFile = "file:///data/user/0/com.luc4n3x.levyra/files/offline_audio/abc.m4a"
        val mediaStore = "content://media/external_primary/audio/media/76"
        val document = "content://com.android.externalstorage.documents/tree/primary%3AMusic/document/primary%3AMusic%2Fa.m4a"

        listOf(privateFile, mediaStore, document).forEach { uri ->
            assertEquals(uri, parseLocalArtworkModel(downloadArtworkModel(uri))?.contentUri)
        }
        listOf(
            "file:///etc/hosts",
            "file:///data/user/0/com.luc4n3x.levyra/files/offline_audio/../databases/levyra.db",
            "content://com.example.provider/secret",
            "https://example.com/a.jpg",
            ""
        ).forEach { uri ->
            assertEquals("", downloadArtworkModel(uri))
            assertEquals(null, parseLocalArtworkModel(localArtworkModel(uri, 0L)))
        }
    }
}

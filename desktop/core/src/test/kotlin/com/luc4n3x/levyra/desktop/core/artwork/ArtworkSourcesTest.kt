package com.luc4n3x.levyra.desktop.core.artwork

import java.nio.file.Path
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkSourcesTest {

    @Test
    fun httpAndHttpsReferencesResolveToRemoteSources() {
        listOf(
            "http://example.com/cover.jpg",
            "https://lh3.googleusercontent.com/cover=w544",
            "HTTPS://EXAMPLE.COM/cover.png"
        ).forEach { reference ->
            assertTrue(reference, ArtworkSources.of(reference) is ArtworkSource.Remote)
        }
    }

    @Test
    fun localArtworkIsNeverHandedToTheHttpClient() {
        val reference = "C:\\Users\\Luca\\AppData\\Roaming\\Levyra\\artwork\\cover.jpg"
        assertNull(reference.toHttpUrlOrNull())
        assertThrows(IllegalArgumentException::class.java) { Request.Builder().url(reference) }
        assertFalse(ArtworkSources.isWebReference(reference))
        assertTrue(ArtworkSources.of(reference) is ArtworkSource.LocalFile)
    }

    @Test
    fun windowsDriveLetterPathIsNeverTreatedAsWebUrl() {
        val reference = "C:\\Users\\Luca\\Music\\Album\\cover.jpg"
        val source = ArtworkSources.of(reference)
        assertTrue(source is ArtworkSource.LocalFile)
        assertEquals(reference, source?.cacheKey)
    }

    @Test
    fun windowsPathsAreNotWebReferences() {
        listOf(
            "C:\\Users\\Luca\\cover.jpg",
            "C:/Users/Luca/cover.jpg",
            "\\\\nas\\music\\cover.jpg",
            "D:\\http\\cover.jpg"
        ).forEach { reference ->
            assertEquals(reference, false, ArtworkSources.isWebReference(reference))
            assertTrue(reference, ArtworkSources.of(reference) is ArtworkSource.LocalFile)
        }
    }

    @Test
    fun fileUrisResolveToTheUnderlyingPath() {
        val cover = Path.of("music", "levyra", "cover.jpg").toAbsolutePath()
        val source = ArtworkSources.of(cover.toUri().toString())
        assertTrue(source is ArtworkSource.LocalFile)
        assertEquals(cover, (source as ArtworkSource.LocalFile).path)
    }

    @Test
    fun posixAndRelativePathsResolveToLocalFiles() {
        listOf("/home/luca/music/cover.jpg", "covers/album.png", "cover.jpg").forEach { reference ->
            assertTrue(reference, ArtworkSources.of(reference) is ArtworkSource.LocalFile)
        }
    }

    @Test
    fun blankReferencesHaveNoSource() {
        listOf("", "   ", "\t\n").forEach { reference ->
            assertNull(ArtworkSources.of(reference))
        }
    }

    @Test
    fun malformedReferencesHaveNoSource() {
        listOf(
            "http://",
            "https://",
            "file:///?invalid uri",
            "cover\u0000.jpg"
        ).forEach { reference ->
            assertNull(reference, ArtworkSources.of(reference))
        }
    }

    @Test
    fun unsupportedSchemesAreRejectedInsteadOfBeingGuessed() {
        listOf(
            "ftp://example.com/cover.jpg",
            "data:image/png;base64,iVBORw0KGgo=",
            "content://media/external/audio/albumart/12",
            "javascript:alert(1)"
        ).forEach { reference ->
            assertNull(reference, ArtworkSources.of(reference))
        }
    }

    @Test
    fun surroundingWhitespaceIsIgnored() {
        assertTrue(ArtworkSources.of("  https://example.com/a.jpg  ") is ArtworkSource.Remote)
        assertTrue(ArtworkSources.of("  C:\\music\\a.jpg  ") is ArtworkSource.LocalFile)
    }
}

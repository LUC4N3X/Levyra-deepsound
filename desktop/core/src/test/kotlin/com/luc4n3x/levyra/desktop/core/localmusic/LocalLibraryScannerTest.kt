package com.luc4n3x.levyra.desktop.core.localmusic

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLibraryScannerTest {

    @Test
    fun scanReadsTagsAndKeepsAlbumOrdering() = runTest {
        val root = Files.createTempDirectory("levyra-scan")
        val folder = LocalFolder(id = "f1", path = root.toString())
        writeFlac(root.resolve("02.flac"), title = "Second", track = 2)
        writeFlac(root.resolve("01.flac"), title = "First", track = 1)

        val result = scanner(root).scan(listOf(folder), emptyList())

        assertEquals(2, result.added)
        assertEquals(listOf("First", "Second"), result.tracks.map { it.title })
        assertEquals("Kiasmos", result.tracks.first().albumArtist)
        assertEquals("f1", result.tracks.first().folderId)

        val index = LocalLibraryIndex.of(result.tracks)
        assertEquals(1, index.albums.size)
        assertEquals("Blurred", index.albums.first().title)
        assertEquals(2, index.albums.first().trackCount)
        assertEquals(listOf("First"), index.search("firs").map { it.title })
    }

    @Test
    fun unchangedFilesAreReusedWithoutRereadingTags() = runTest {
        val root = Files.createTempDirectory("levyra-scan")
        val folder = LocalFolder(id = "f1", path = root.toString())
        writeFlac(root.resolve("01.flac"), title = "First", track = 1)
        var reads = 0
        val counting = LocalLibraryScanner(
            artwork = LocalArtworkCache(root.resolve("art")).prepare(),
            readTags = { path -> reads += 1; AudioTagReader.read(path) }
        )

        val first = counting.scan(listOf(folder), emptyList())
        val second = counting.scan(listOf(folder), first.tracks)

        assertEquals(1, reads)
        assertEquals(1, second.reused)
        assertEquals(0, second.added)
        assertEquals(first.tracks.map { it.id }, second.tracks.map { it.id })
    }

    @Test
    fun deepRescanRereadsUnchangedFiles() = runTest {
        val root = Files.createTempDirectory("levyra-scan")
        val folder = LocalFolder(id = "f1", path = root.toString())
        writeFlac(root.resolve("01.flac"), title = "First", track = 1)
        var reads = 0
        val counting = LocalLibraryScanner(
            artwork = LocalArtworkCache(root.resolve("art")).prepare(),
            readTags = { path -> reads += 1; AudioTagReader.read(path) }
        )

        val first = counting.scan(listOf(folder), emptyList())
        counting.scan(listOf(folder), first.tracks, deep = true)

        assertEquals(2, reads)
    }

    @Test
    fun deletedFilesAreMarkedUnavailableInsteadOfDropped() = runTest {
        val root = Files.createTempDirectory("levyra-scan")
        val folder = LocalFolder(id = "f1", path = root.toString())
        val file = root.resolve("01.flac")
        writeFlac(file, title = "First", track = 1)

        val first = scanner(root).scan(listOf(folder), emptyList())
        Files.delete(file)
        val second = scanner(root).scan(listOf(folder), first.tracks)

        assertEquals(1, second.missing)
        assertEquals(1, second.tracks.size)
        assertFalse(second.tracks.first().available)
        assertTrue(LocalLibraryIndex.of(second.tracks).tracks.isEmpty())
    }

    @Test
    fun tracksOutsideTheScannedFoldersAreLeftUntouched() = runTest {
        val root = Files.createTempDirectory("levyra-scan")
        val other = LocalTrack(id = "x", path = "D:/elsewhere/song.flac", available = true)

        val result = scanner(root).scan(
            folders = listOf(LocalFolder(id = "f1", path = root.toString())),
            existing = listOf(other)
        )

        assertEquals(0, result.missing)
        assertTrue(result.tracks.single().available)
    }

    @Test
    fun unavailableFolderDoesNotMarkPreviouslyIndexedTracksMissing() = runTest {
        val cacheRoot = Files.createTempDirectory("levyra-scan")
        val unavailableRoot = cacheRoot.resolve("missing")
        val folder = LocalFolder(id = "f1", path = unavailableRoot.toString())
        val existing = LocalTrack(
            id = "known",
            path = unavailableRoot.resolve("01.flac").toString(),
            folderId = folder.id,
            available = true
        )

        val second = scanner(cacheRoot).scan(listOf(folder), listOf(existing))

        assertEquals(0, second.missing)
        assertTrue(second.tracks.single().available)
    }

    @Test
    fun folderCoverIsUsedWhenTheFileHasNoEmbeddedPicture() = runTest {
        val root = Files.createTempDirectory("levyra-scan")
        val folder = LocalFolder(id = "f1", path = root.toString())
        writeFlac(root.resolve("01.flac"), title = "First", track = 1, picture = false)
        val cover = root.resolve("cover.jpg")
        Files.write(cover, AudioTagBuilders.JPEG_BYTES)

        val result = scanner(root).scan(listOf(folder), emptyList())

        assertEquals(cover.toString(), result.tracks.single().artworkPath)
    }

    @Test
    fun embeddedPictureIsCachedOnceForIdenticalArtwork() = runTest {
        val root = Files.createTempDirectory("levyra-scan")
        val cacheDirectory = root.resolve("art")
        val folder = LocalFolder(id = "f1", path = root.toString())
        writeFlac(root.resolve("01.flac"), title = "First", track = 1)
        writeFlac(root.resolve("02.flac"), title = "Second", track = 2)

        val result = LocalLibraryScanner(LocalArtworkCache(cacheDirectory).prepare())
            .scan(listOf(folder), emptyList())

        val stored = Files.list(cacheDirectory).use { it.toList() }
        assertEquals(1, stored.size)
        assertEquals(1, result.tracks.mapTo(HashSet()) { it.artworkPath }.size)
    }

    private fun scanner(root: Path) =
        LocalLibraryScanner(LocalArtworkCache(root.resolve("art")).prepare())

    private fun writeFlac(path: Path, title: String, track: Int, picture: Boolean = true) {
        Files.write(
            path,
            AudioTagBuilders.flac(
                sampleRate = 44_100,
                channels = 2,
                bitDepth = 16,
                totalSamples = 44_100L * 120L,
                comments = listOf(
                    "TITLE=$title",
                    "ARTIST=Kiasmos",
                    "ALBUMARTIST=Kiasmos",
                    "ALBUM=Blurred",
                    "TRACKNUMBER=$track"
                ),
                picture = if (picture) AudioTagBuilders.JPEG_BYTES else null
            )
        )
    }
}

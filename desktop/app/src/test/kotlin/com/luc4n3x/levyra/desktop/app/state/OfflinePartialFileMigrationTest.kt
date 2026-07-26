package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.stream.ResolvedAudio
import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OfflinePartialFileMigrationTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun matchingStreamIdentityMovesPartialWithoutLosingProgress() {
        val downloads = temporaryFolder.newFolder("offline").toPath()
        val legacy = downloads.resolve("Artist - Track [legacy].m4a.part")
        val target = downloads.resolve("Artist - Track [0123456789abcdef].m4a.part")
        val content = ByteArray(16_384) { index -> (index % 251).toByte() }
        Files.write(legacy, content)

        val resumedBytes = OfflinePartialFileMigration.prepare(
            downloadsDirectory = downloads,
            recordedPath = legacy.toString(),
            targetPath = target,
            recordedIdentity = "same-stream",
            currentIdentity = "same-stream"
        )

        assertEquals(content.size.toLong(), resumedBytes)
        assertFalse(Files.exists(legacy))
        assertTrue(Files.isRegularFile(target))
        assertArrayEquals(content, Files.readAllBytes(target))
    }

    @Test
    fun mismatchedStreamIdentityDiscardsEveryLocalPartial() {
        val downloads = temporaryFolder.newFolder("offline-mismatch").toPath()
        val legacy = downloads.resolve("old-name.m4a.part")
        val target = downloads.resolve("new-name.m4a.part")
        Files.write(legacy, ByteArray(8_192) { 7 })
        Files.write(target, ByteArray(512) { 3 })

        val resumedBytes = OfflinePartialFileMigration.prepare(
            downloadsDirectory = downloads,
            recordedPath = legacy.toString(),
            targetPath = target,
            recordedIdentity = "old-stream",
            currentIdentity = "new-stream"
        )

        assertEquals(0L, resumedBytes)
        assertFalse(Files.exists(legacy))
        assertFalse(Files.exists(target))
    }

    @Test
    fun missingLegacyIdentityCannotResume() {
        val downloads = temporaryFolder.newFolder("offline-legacy").toPath()
        val legacy = downloads.resolve("old-name.webm.part")
        val target = downloads.resolve("new-name.webm.part")
        Files.write(legacy, ByteArray(4_096) { 4 })

        val resumedBytes = OfflinePartialFileMigration.prepare(
            downloadsDirectory = downloads,
            recordedPath = legacy.toString(),
            targetPath = target,
            recordedIdentity = "",
            currentIdentity = "verified-stream"
        )

        assertEquals(0L, resumedBytes)
        assertFalse(Files.exists(legacy))
        assertFalse(Files.exists(target))
    }

    @Test
    fun largerMatchingPartialReplacesSmallerTarget() {
        val downloads = temporaryFolder.newFolder("offline-conflict").toPath()
        val legacy = downloads.resolve("old-name.webm.part")
        val target = downloads.resolve("new-name.webm.part")
        val legacyContent = ByteArray(8_192) { 7 }
        Files.write(legacy, legacyContent)
        Files.write(target, ByteArray(512) { 3 })

        val resumedBytes = OfflinePartialFileMigration.prepare(
            downloadsDirectory = downloads,
            recordedPath = legacy.toString(),
            targetPath = target,
            recordedIdentity = "same-stream",
            currentIdentity = "same-stream"
        )

        assertEquals(legacyContent.size.toLong(), resumedBytes)
        assertFalse(Files.exists(legacy))
        assertArrayEquals(legacyContent, Files.readAllBytes(target))
    }

    @Test
    fun recordedPathOutsideDownloadDirectoryIsIgnored() {
        val downloads = temporaryFolder.newFolder("offline-safe").toPath()
        val outside = temporaryFolder.newFile("outside.m4a.part").toPath()
        val target = downloads.resolve("safe-name.m4a.part")
        Files.writeString(outside, "do not touch")

        val resumedBytes = OfflinePartialFileMigration.prepare(
            downloadsDirectory = downloads,
            recordedPath = outside.toString(),
            targetPath = target,
            recordedIdentity = "same-stream",
            currentIdentity = "same-stream"
        )

        assertEquals(0L, resumedBytes)
        assertTrue(Files.isRegularFile(outside))
        assertFalse(Files.exists(target))
    }

    @Test
    fun streamIdentityIgnoresExpiringSignatureButTracksMediaVariant() {
        val track = Track(id = "video-id", title = "Track", artist = "Artist", videoUrl = "")
        val first = resolved(
            "https://r1.googlevideo.com/videoplayback?itag=140&clen=123456&lmt=1710000000000000&dur=180.0&mime=audio%2Fmp4&expire=1&sig=first"
        )
        val refreshed = resolved(
            "https://r2.googlevideo.com/videoplayback?sig=second&expire=2&mime=audio%2Fmp4&dur=180.0&lmt=1710000000000000&clen=123456&itag=140"
        )
        val differentItag = resolved(
            "https://r1.googlevideo.com/videoplayback?itag=251&clen=123456&lmt=1710000000000000&dur=180.0&mime=audio%2Fwebm"
        )

        val firstIdentity = OfflineStreamIdentity.from(track, first)
        val refreshedIdentity = OfflineStreamIdentity.from(track, refreshed)
        val differentIdentity = OfflineStreamIdentity.from(track, differentItag)

        assertTrue(firstIdentity.isNotBlank())
        assertEquals(firstIdentity, refreshedIdentity)
        assertNotEquals(firstIdentity, differentIdentity)
    }

    @Test
    fun streamIdentityRequiresFormatAndContentValidator() {
        val track = Track(id = "video-id", title = "Track", artist = "Artist", videoUrl = "")
        val unresolved = resolved("https://example.com/audio?expire=1&sig=value")

        assertEquals("", OfflineStreamIdentity.from(track, unresolved))
    }

    private fun resolved(url: String): ResolvedAudio = ResolvedAudio(
        url = url,
        label = "AAC · M4A",
        expiresAtMillis = 0L,
        durationMs = 180_000L,
        artworkUrl = "",
        title = "Track",
        artist = "Artist"
    )
}

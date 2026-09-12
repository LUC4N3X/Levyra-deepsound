package com.luc4n3x.levyra.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistCoverBackupPolicyTest {

    @Test
    fun generatedCoverEntryIsPortableAndAllowlisted() {
        val entry = playlistCoverBackupEntry("playlist/with unsafe characters")

        assertTrue(entry.startsWith("data/playlist_covers/"))
        assertTrue(entry.endsWith(".jpg"))
        assertTrue(vaultEntryAllowed(entry))
        assertFalse(entry.contains(".."))
    }

    @Test
    fun malformedOrTraversingCoverEntriesAreRejected() {
        assertFalse(vaultEntryAllowed("data/playlist_covers/../secret.jpg"))
        assertFalse(vaultEntryAllowed("data/playlist_covers/not-a-digest.jpg"))
        assertFalse(vaultEntryAllowed("data/playlist_covers/${"a".repeat(64)}.png"))
    }

    @Test
    fun coverPayloadValidationIsBoundedAndChecksImageSignature() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00)
        assertTrue(playlistCoverPayloadAccepted(jpeg))
        assertFalse(playlistCoverPayloadAccepted(ByteArray(0)))
        assertFalse(playlistCoverPayloadAccepted("not an image".toByteArray()))
        assertFalse(playlistCoverPayloadAccepted(ByteArray(MAX_PLAYLIST_COVER_BACKUP_BYTES + 1)))
    }

    @Test
    fun squareCropUsesCenteredVisibleAreaAtMinimumZoom() {
        val crop = playlistCoverCropRect(
            imageWidth = 1600,
            imageHeight = 900,
            crop = PlaylistCoverCrop(viewportSizePx = 450)
        )

        assertEquals(350, crop.left)
        assertEquals(0, crop.top)
        assertEquals(900, crop.size)
    }

    @Test
    fun cropPanAndZoomRemainInsideSourceBounds() {
        val crop = playlistCoverCropRect(
            imageWidth = 800,
            imageHeight = 1200,
            crop = PlaylistCoverCrop(
                viewportSizePx = 400,
                zoom = 2f,
                offsetX = 10_000f,
                offsetY = -10_000f
            )
        )

        assertEquals(0, crop.left)
        assertEquals(800, crop.top)
        assertEquals(400, crop.size)
    }
}

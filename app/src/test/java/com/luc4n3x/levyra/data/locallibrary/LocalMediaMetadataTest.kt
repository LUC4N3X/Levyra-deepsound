package com.luc4n3x.levyra.data.locallibrary

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMediaMetadataTest {

    @Test
    fun missingTagsFallBackToFileNameAndAlbumArtist() {
        val entity = scanned(title = "<unknown>", artist = "", albumArtist = "The Band", displayName = "01 - Opening.flac")
            .toLocalMediaEntity(levyraTrackId = "", now = 1L)

        assertEquals("01 - Opening", entity.title)
        assertEquals("The Band", entity.artist)
    }

    @Test
    fun fileWithoutAnyNameGetsAStableTitle() {
        assertEquals("Audio 42", localTitleFallback("", "", 42L))
        assertEquals("track", localTitleFallback("  ", "track", 42L))
    }

    @Test
    fun unknownArtistStaysEmptyForLocalizedPresentation() {
        val entity = scanned(artist = "<unknown>", albumArtist = "").toLocalMediaEntity("", 1L)

        assertEquals("", entity.artist)
        assertEquals("", entity.artistKey)
    }

    @Test
    fun artistKeysIgnoreCaseWhitespaceAccentsAndFeaturing() {
        assertEquals(localArtistKey("Beyoncé"), localArtistKey("  beyonce "))
        assertEquals(localArtistKey("Dua Lipa"), localArtistKey("Dua Lipa feat. DaBaby"))
        assertEquals(localArtistKey("Dua Lipa"), localArtistKey("Dua Lipa (ft. Someone)"))
        assertNotEquals(localArtistKey("Simon & Garfunkel"), localArtistKey("Simon"))
    }

    @Test
    fun albumsWithTheSameTitleAreSeparatedByAlbumArtistOrFolder() {
        val first = localAlbumKey("Greatest Hits", "Queen", "external_primary:music/queen/")
        val second = localAlbumKey("Greatest Hits", "ABBA", "external_primary:music/abba/")
        val untaggedA = localAlbumKey("Greatest Hits", "", "external_primary:music/a/")
        val untaggedB = localAlbumKey("Greatest Hits", "", "external_primary:music/b/")

        assertNotEquals(first, second)
        assertNotEquals(untaggedA, untaggedB)
        assertEquals(first, localAlbumKey("greatest  hits", "QUEEN", "external_primary:elsewhere/"))
    }

    @Test
    fun discsOfTheSameAlbumShareTheAlbumKey() {
        val discOne = scanned(album = "Double", albumArtist = "Band", trackField = 1_003).toLocalMediaEntity("", 1L)
        val discTwo = scanned(album = "Double", albumArtist = "Band", trackField = 2_001).toLocalMediaEntity("", 1L)

        assertEquals(discOne.albumKey, discTwo.albumKey)
        assertEquals(1, discOne.discNumber)
        assertEquals(3, discOne.trackNumber)
        assertEquals(2, discTwo.discNumber)
        assertEquals(1, discTwo.trackNumber)
    }

    @Test
    fun modernTrackAndDiscColumnsWin() {
        assertEquals(7, localTrackNumber(3, "7/12"))
        assertEquals(2, localDiscNumber(0, "2/2"))
        assertEquals(0, localTrackNumber(0, "n/a"))
    }

    @Test
    fun relativePathIsDerivedFromLegacyFilePaths() {
        assertEquals("Music/Levyra/", relativePathFromFilePath("/storage/emulated/0/Music/Levyra/song.m4a"))
        assertEquals("Albums/Live/", relativePathFromFilePath("/storage/1A2B-3C4D/Albums/Live/song.mp3"))
        assertEquals("", relativePathFromFilePath("song.mp3"))
    }

    @Test
    fun levyraDownloadsAreRecognisedByFolderAndIdentity() {
        assertTrue(scanned(relativePath = "Music/Levyra/Artist/").toLocalMediaEntity("", 1L).isLevyraDownload)
        assertTrue(scanned(relativePath = "Download/").toLocalMediaEntity("yt123", 1L).isLevyraDownload)
        assertFalse(scanned(relativePath = "Music/Other/").toLocalMediaEntity("", 1L).isLevyraDownload)
    }

    @Test
    fun identityDependsOnVolumeAndMediaStoreIdOnly() {
        val a = scanned(mediaId = 5, relativePath = "Music/A/").toLocalMediaEntity("", 1L)
        val b = scanned(mediaId = 5, relativePath = "Music/B/").toLocalMediaEntity("", 1L)
        val sd = scanned(mediaId = 5, volume = "1A2B-3C4D").toLocalMediaEntity("", 1L)

        assertEquals(a.identityKey, b.identityKey)
        assertNotEquals(a.identityKey, sd.identityKey)
        assertEquals("external_primary:music/a/", a.folderKey)
        assertEquals("A", a.folderName)
    }

    @Test
    fun fingerprintRequiresSizeAndDuration() {
        assertEquals("", localContentFingerprint(0L, 1_000L, "x"))
        assertEquals("", localContentFingerprint(10L, 0L, "x"))
        assertEquals("10:1:hello world", localContentFingerprint(10L, 1_999L, "Hello, World"))
    }

    @Test
    fun mediaStoreIdIsParsedOnlyFromMediaUris() {
        assertEquals(12L, mediaStoreIdFromUri("content://media/external_primary/audio/media/12"))
        assertNull(mediaStoreIdFromUri("content://com.android.externalstorage.documents/document/12"))
        assertNull(mediaStoreIdFromUri("file:///sdcard/12"))
    }

    @Test
    fun mediaStoreIdentityIncludesTheVolume() {
        assertEquals(
            "ms:external_primary:12",
            mediaStoreIdentityFromUri("content://media/external_primary/audio/media/12")
        )
        assertEquals(
            "ms:0123-4567:12",
            mediaStoreIdentityFromUri("content://media/0123-4567/audio/media/12")
        )
        assertNull(mediaStoreIdentityFromUri("content://com.android.externalstorage.documents/document/12"))
    }

    @Test
    fun levyraTrackIdIsReadFromTheFreeformAtom() {
        val file = concat(
            box("ftyp", "M4A ".toByteArray()),
            box("mdat", ByteArray(4_096) { 7 }),
            box(
                "moov",
                box("mvhd", ByteArray(20)),
                box(
                    "udta",
                    box(
                        "meta",
                        ByteArray(4),
                        box("hdlr", ByteArray(24)),
                        box(
                            "ilst",
                            freeform("com.apple.iTunes", "ISRC", "XX0000000000"),
                            freeform("com.luc4n3x.levyra", "TRACK_ID", "dQw4w9WgXcQ")
                        )
                    )
                )
            )
        )

        assertEquals("dQw4w9WgXcQ", LevyraTagIdentityReader.readTrackId(ByteArrayInputStream(file), file.size.toLong()))
    }

    @Test
    fun foreignOrCorruptTagsAreIgnored() {
        val foreign = box(
            "moov",
            box("udta", box("meta", ByteArray(4), box("ilst", freeform("other", "TRACK_ID", "x"))))
        )
        val truncated = foreign.copyOf(foreign.size - 5)
        val garbage = ByteArray(64) { 0x7F }

        assertNull(LevyraTagIdentityReader.readTrackId(ByteArrayInputStream(foreign), foreign.size.toLong()))
        assertNull(LevyraTagIdentityReader.readTrackId(ByteArrayInputStream(truncated), truncated.size.toLong()))
        assertNull(LevyraTagIdentityReader.readTrackId(ByteArrayInputStream(garbage), garbage.size.toLong()))
        assertNull(LevyraTagIdentityReader.readTrackId(ByteArrayInputStream(ByteArray(0)), 0L))
    }

    private fun freeform(mean: String, name: String, value: String): ByteArray = box(
        "----",
        box("mean", ByteArray(4), mean.toByteArray()),
        box("name", ByteArray(4), name.toByteArray()),
        box("data", byteArrayOf(0, 0, 0, 1, 0, 0, 0, 0), value.toByteArray())
    )

    private fun concat(vararg parts: ByteArray): ByteArray =
        ByteArrayOutputStream().apply { parts.forEach { write(it) } }.toByteArray()

    private fun box(type: String, vararg parts: ByteArray): ByteArray {
        val payload = concat(*parts)
        val size = payload.size + 8
        return concat(
            byteArrayOf((size ushr 24).toByte(), (size ushr 16).toByte(), (size ushr 8).toByte(), size.toByte()),
            type.toByteArray(Charsets.ISO_8859_1),
            payload
        )
    }

    private fun scanned(
        mediaId: Long = 1L,
        title: String = "Title",
        artist: String = "Artist",
        album: String = "Album",
        albumArtist: String = "",
        displayName: String = "file.mp3",
        relativePath: String = "Music/A/",
        volume: String = "external_primary",
        trackField: Int = 0
    ) = ScannedLocalAudio(
        volumeName = volume,
        mediaStoreId = mediaId,
        contentUri = "content://media/$volume/audio/media/$mediaId",
        filePath = "",
        relativePath = relativePath,
        displayName = displayName,
        title = title,
        artist = artist,
        album = album,
        albumArtist = albumArtist,
        genre = "",
        year = 2020,
        trackField = trackField,
        trackText = "",
        discText = "",
        durationMs = 180_000L,
        mimeType = "audio/mpeg",
        bitrate = 320_000,
        sizeBytes = 1_000L,
        dateAddedMs = 1L,
        dateModifiedMs = 1L,
        albumId = 3L
    )
}

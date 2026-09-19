package com.luc4n3x.levyra.data.apple

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.offline.tagging.LevyraM4aMetadata
import com.luc4n3x.levyra.player.offline.tagging.LevyraM4aTagWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets

class AppleMetadataEnrichmentTest {

    private fun sampleTrack(
        title: String = "Get Lucky",
        artist: String = "Daft Punk",
        album: String = "Random Access Memories",
        durationMs: Long = 369629L,
        isrc: String = "USQX91300108",
        trackNumber: Int = 8,
        explicit: Boolean = false
    ): Track = Track(
        id = "video-sample-1",
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        streamUrl = "https://example.com/stream",
        videoUrl = "https://youtube.com/watch?v=video-sample-1",
        thumbnailUrl = "https://example.com/thumb.jpg",
        largeThumbnailUrl = "https://example.com/large.jpg",
        source = "YouTube Music",
        moodTags = setOf("electronic"),
        energy = 80,
        vocal = 50,
        replayScore = 90,
        cacheScore = 80,
        accentStart = 0,
        accentEnd = 0,
        trackNumber = trackNumber,
        isrc = isrc,
        explicit = explicit
    )

    private fun sampleAppleCandidate(
        name: String = "Get Lucky",
        artistName: String = "Daft Punk, Pharrell Williams & Nile Rodgers",
        albumName: String = "Random Access Memories",
        durationMs: Long = 369629L,
        isrc: String = "USQX91300108",
        trackNumber: Int = 8,
        explicit: Boolean = false,
        isReleaseMatch: Boolean = false
    ): AppleTrackMetadata = AppleTrackMetadata(
        songId = "617154366",
        albumId = "617154241",
        name = name,
        artistName = artistName,
        albumName = albumName,
        albumArtistName = "Daft Punk",
        composerName = "Pharrell Williams, Nile Rodgers, Thomas Bangalter & Guy-Manuel de Homem-Christo",
        genreNames = listOf("Pop", "Electronic"),
        releaseDate = "2013-04-19",
        albumReleaseDate = "2013-05-17",
        trackNumber = trackNumber,
        trackTotal = 13,
        discNumber = 1,
        discTotal = 1,
        isrc = isrc,
        upc = "886443919266",
        copyright = "℗ 2013 Daft Life Limited",
        explicit = explicit,
        canonicalAlbumUrl = "https://music.apple.com/us/album/random-access-memories/617154241",
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/e8/43/5f/e8435ffa-b6b9-b171-40ab-4ff3959ab661/886443919266.jpg/600x600bb.jpg",
        highResArtworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/e8/43/5f/e8435ffa-b6b9-b171-40ab-4ff3959ab661/886443919266.jpg/1400x1400bb.jpg",
        durationMs = durationMs,
        isReleaseMatch = isReleaseMatch
    )

    @Test
    fun exactIsrcMatchAcceptsAndRanksByRelease() {
        val reference = sampleTrack()
        val candidate = sampleAppleCandidate()

        val evaluation = AppleMetadataMatcher.evaluate(reference, candidate)

        assertTrue("Exact ISRC should be accepted", evaluation.accepted)
        assertTrue("Exact ISRC with matching album has high confidence", evaluation.confidence >= 90)
        assertTrue("Release match should be true for matching album", evaluation.isReleaseMatch)
    }

    @Test
    fun sameIsrcPrefersStandardAlbumOverCompilationAndDeluxe() {
        val reference = sampleTrack(album = "Random Access Memories", trackNumber = 8)

        val standardCandidate = sampleAppleCandidate(albumName = "Random Access Memories", trackNumber = 8)
        val deluxeCandidate = sampleAppleCandidate(albumName = "Random Access Memories (10th Anniversary Edition)", trackNumber = 8)
        val compilationCandidate = sampleAppleCandidate(albumName = "Top Hits 2013 (Compilation)", trackNumber = 1)

        val standardEval = AppleMetadataMatcher.evaluate(reference, standardCandidate)
        val deluxeEval = AppleMetadataMatcher.evaluate(reference, deluxeCandidate)
        val compEval = AppleMetadataMatcher.evaluate(reference, compilationCandidate)

        assertTrue("Standard album should be release match", standardEval.isReleaseMatch)
        assertFalse("Deluxe edition should not be marked exact release match", deluxeEval.isReleaseMatch)
        assertFalse("Compilation should not be marked exact release match", compEval.isReleaseMatch)

        assertTrue(
            "Standard album score (${standardEval.confidence}) must exceed deluxe (${deluxeEval.confidence})",
            standardEval.confidence > deluxeEval.confidence
        )
        assertTrue(
            "Deluxe edition score (${deluxeEval.confidence}) must exceed compilation (${compEval.confidence})",
            deluxeEval.confidence > compEval.confidence
        )
    }

    @Test
    fun sameIsrcWithoutKnownAlbumDoesNotOverwriteReleaseMetadata() {
        val reference = sampleTrack(
            album = "",
            trackNumber = 0
        ).copy(
            thumbnailUrl = "https://original.com/thumb.jpg",
            largeThumbnailUrl = "https://original.com/large.jpg",
            upc = "orig_upc"
        )
        val candidate = sampleAppleCandidate(
            albumName = "Random Access Memories",
            isrc = "USQX91300108"
        )

        val evaluation = AppleMetadataMatcher.evaluate(reference, candidate)
        assertTrue("Recording match should be accepted via ISRC", evaluation.accepted)
        assertFalse("Release match must be false when reference has no album", evaluation.isReleaseMatch)

        val merged = AppleMetadataEnricher.mergeWithAppleMetadata(
            reference,
            candidate.copy(confidence = evaluation.confidence, isReleaseMatch = evaluation.isReleaseMatch)
        )

        // Release-specific metadata must NOT be overwritten:
        assertEquals("", merged.album)
        assertEquals("https://original.com/thumb.jpg", merged.thumbnailUrl)
        assertEquals("https://original.com/large.jpg", merged.largeThumbnailUrl)
        assertEquals("orig_upc", merged.upc)
        assertEquals(0, merged.trackNumber)

        // Recording-level metadata MUST be enriched:
        assertEquals("USQX91300108", merged.isrc)
        assertEquals("Pharrell Williams, Nile Rodgers, Thomas Bangalter & Guy-Manuel de Homem-Christo", merged.composer)
        assertEquals("617154366", merged.appleSongId)
    }

    @Test
    fun conflictingIsrcRejectsMatch() {
        val reference = sampleTrack(isrc = "USQX91300108")
        val candidate = sampleAppleCandidate(isrc = "GBAYE0601477")

        val evaluation = AppleMetadataMatcher.evaluate(reference, candidate)

        assertFalse("Conflicting ISRC must be rejected", evaluation.accepted)
        assertEquals(0, evaluation.confidence)
        assertEquals("isrc_conflict", evaluation.rejectionReason)
    }

    @Test
    fun mismatchedVersionMarkersAreRejected() {
        val reference = sampleTrack(title = "Hotel California")
        val liveCandidate = sampleAppleCandidate(name = "Hotel California (Live)")

        val liveEval = AppleMetadataMatcher.evaluate(reference, liveCandidate)
        assertFalse("Live version must not match studio version", liveEval.accepted)
        assertEquals("version_marker_mismatch", liveEval.rejectionReason)

        val remixCandidate = sampleAppleCandidate(name = "Hotel California (Remix)")
        val remixEval = AppleMetadataMatcher.evaluate(reference, remixCandidate)
        assertFalse("Remix version must not match studio version", remixEval.accepted)

        val acousticCandidate = sampleAppleCandidate(name = "Hotel California (Acoustic Version)")
        val acousticEval = AppleMetadataMatcher.evaluate(reference, acousticCandidate)
        assertFalse("Acoustic version must not match studio version", acousticEval.accepted)

        val spedUpCandidate = sampleAppleCandidate(name = "Hotel California (Sped Up)")
        val spedUpEval = AppleMetadataMatcher.evaluate(reference, spedUpCandidate)
        assertFalse("Sped up version must not match studio version", spedUpEval.accepted)

        val instrumentalCandidate = sampleAppleCandidate(name = "Hotel California (Instrumental)")
        val instrumentalEval = AppleMetadataMatcher.evaluate(reference, instrumentalCandidate)
        assertFalse("Instrumental must not match vocal track", instrumentalEval.accepted)
    }

    @Test
    fun durationMismatchRejectsExcessiveDelta() {
        val reference = sampleTrack(durationMs = 200_000L, isrc = "")

        // <= 3s delta: accepted
        val closeCandidate = sampleAppleCandidate(durationMs = 202_000L, isrc = "")
        val closeEval = AppleMetadataMatcher.evaluate(reference, closeCandidate)
        assertTrue("Close duration (2s) should be accepted", closeEval.accepted)

        // > 12s delta without ISRC: rejected
        val farCandidate = sampleAppleCandidate(durationMs = 214_000L, isrc = "")
        val farEval = AppleMetadataMatcher.evaluate(reference, farCandidate)
        assertFalse("Duration delta > 12s without ISRC must be rejected", farEval.accepted)
        assertEquals("duration_out_of_range", farEval.rejectionReason)

        // > 15s delta even with ISRC: rejected
        val extremeCandidate = sampleAppleCandidate(durationMs = 220_000L, isrc = "USQX91300108")
        val extremeEval = AppleMetadataMatcher.evaluate(reference, extremeCandidate)
        assertFalse("Duration delta > 15s even with ISRC must be rejected", extremeEval.accepted)
        assertEquals("duration_out_of_range", extremeEval.rejectionReason)
    }

    @Test
    fun oneSidedArtistSubstringDoesNotMatchTributeBand() {
        val reference = sampleTrack(
            title = "One",
            artist = "U2",
            album = "Achtung Baby",
            durationMs = 276_000L,
            isrc = ""
        )
        val tributeCandidate = sampleAppleCandidate(
            name = "One",
            artistName = "U2 Tribute Band",
            albumName = "Achtung Baby",
            durationMs = 276_000L,
            isrc = ""
        )

        val evaluation = AppleMetadataMatcher.evaluate(reference, tributeCandidate)

        assertFalse("One-sided artist substrings must not match tribute artists", evaluation.accepted)
        assertEquals("artist_mismatch", evaluation.rejectionReason)
    }

    @Test
    fun unicodeArtistMatchingHandlesInternationalNamesCorrectly() {
        // Japanese artist: Utada Hikaru
        val jpRef = sampleTrack(title = "First Love", artist = "宇多田ヒカル", album = "First Love", isrc = "")
        val jpCand = sampleAppleCandidate(name = "First Love", artistName = "宇多田ヒカル", albumName = "First Love", isrc = "")
        val jpEval = AppleMetadataMatcher.evaluate(jpRef, jpCand)
        assertTrue("Matching Japanese artist name must be accepted", jpEval.accepted)

        // Different Japanese artist: Kenshi Yonezu
        val jpDiffCand = sampleAppleCandidate(name = "First Love", artistName = "米津玄師", albumName = "First Love", isrc = "")
        val jpDiffEval = AppleMetadataMatcher.evaluate(jpRef, jpDiffCand)
        assertFalse("Distinct Japanese artists must not match", jpDiffEval.accepted)
        assertEquals("artist_mismatch", jpDiffEval.rejectionReason)

        // Cyrillic artist: Kino
        val cyrRef = sampleTrack(title = "Группа крови", artist = "Кино", album = "Группа крови", isrc = "")
        val cyrCand = sampleAppleCandidate(name = "Группа крови", artistName = "Кино", albumName = "Группа крови", isrc = "")
        val cyrEval = AppleMetadataMatcher.evaluate(cyrRef, cyrCand)
        assertTrue("Matching Cyrillic artist must be accepted", cyrEval.accepted)

        val cyrDiffCand = sampleAppleCandidate(name = "Группа крови", artistName = "Би-2", albumName = "Группа крови", isrc = "")
        val cyrDiffEval = AppleMetadataMatcher.evaluate(cyrRef, cyrDiffCand)
        assertFalse("Distinct Cyrillic artists must not match", cyrDiffEval.accepted)
    }

    @Test
    fun explicitCleanConflictIsRejected() {
        val explicitTrack = sampleTrack(title = "Song (Explicit)", explicit = true)
        val cleanCandidate = sampleAppleCandidate(name = "Song (Clean)", explicit = false)

        val eval = AppleMetadataMatcher.evaluate(explicitTrack, cleanCandidate)
        assertFalse("Explicit vs Clean conflict must be rejected", eval.accepted)
        assertEquals("explicit_mismatch", eval.rejectionReason)
    }

    @Test
    fun featuredOnlyArtistOverlapIsNotCompatible() {
        assertFalse(AppleArtistMatcher.areCompatible("Drake feat. Rihanna", "Future feat. Rihanna"))
        assertTrue(AppleArtistMatcher.areCompatible("Drake feat. Rihanna", "Drake"))
        assertTrue(AppleArtistMatcher.areCompatible("Rihanna", "Drake feat. Rihanna"))
    }

    @Test
    fun matchingWithoutIsrcAcceptsWithHighConfidenceWhenMetadataAligns() {
        val reference = sampleTrack(isrc = "")
        val candidate = sampleAppleCandidate(isrc = "")

        val evaluation = AppleMetadataMatcher.evaluate(reference, candidate)

        assertTrue("Aligning title, artist, album and duration should be accepted", evaluation.accepted)
        assertTrue("Confidence should be high (> 75)", evaluation.confidence >= 75)
    }

    @Test
    fun smartMergePrefersAppleForAuthoritativeFieldsWhilePreservingOriginals() {
        val original = sampleTrack(
            title = "Get Lucky (Original)",
            artist = "Daft Punk"
        ).copy(
            streamUrl = "https://googlevideo.com/videoplayback?id=123",
            videoUrl = "https://youtube.com/watch?v=video-sample-1"
        )
        val apple = sampleAppleCandidate(
            isrc = "USQX91300108",
            isReleaseMatch = true
        )

        val merged = AppleMetadataEnricher.mergeWithAppleMetadata(original, apple)

        // Apple is authoritative for:
        assertEquals("USQX91300108", merged.isrc)
        assertEquals("Pharrell Williams, Nile Rodgers, Thomas Bangalter & Guy-Manuel de Homem-Christo", merged.composer)
        assertEquals("Daft Punk", merged.albumArtist)
        assertEquals("℗ 2013 Daft Life Limited", merged.copyright)
        assertEquals("2013-04-19", merged.releaseDate)
        assertEquals("2013", merged.year)
        assertEquals(8, merged.trackNumber)
        assertEquals(13, merged.trackTotal)
        assertEquals(1, merged.discNumber)
        assertEquals(1, merged.discTotal)
        assertEquals("617154366", merged.appleSongId)
        assertEquals("617154241", merged.appleAlbumId)
        assertEquals("https://music.apple.com/us/album/random-access-memories/617154241", merged.canonicalAlbumUrl)
        assertEquals("Apple Music", merged.metadataProvider)
        assertEquals(apple.highResArtworkUrl, merged.largeThumbnailUrl)
        assertTrue(merged.moodTags.contains("pop"))
        assertTrue(merged.moodTags.contains("electronic"))

        // Original stream data is preserved:
        assertEquals("video-sample-1", merged.id)
        assertEquals("https://googlevideo.com/videoplayback?id=123", merged.streamUrl)
        assertEquals("https://youtube.com/watch?v=video-sample-1", merged.videoUrl)
    }

    @Test
    fun tagWriterWritesComposerAndCopyright() {
        val input = File.createTempFile("levyra-test-in", ".m4a")
        val output = File.createTempFile("levyra-test-out", ".m4a")
        input.writeBytes(
            atom("ftyp", "M4A ".toByteArray(StandardCharsets.US_ASCII)) +
                atom("moov", byteArrayOf()) +
                atom("mdat", ByteArray(32) { it.toByte() })
        )

        try {
            val result = LevyraM4aTagWriter.write(
                input = input,
                output = output,
                metadata = LevyraM4aMetadata(
                    title = "Get Lucky",
                    artist = "Daft Punk",
                    album = "Random Access Memories",
                    albumArtist = "Daft Punk",
                    composer = "Thomas Bangalter, Guy-Manuel de Homem-Christo",
                    copyright = "℗ 2013 Daft Life Limited",
                    trackNumber = 8,
                    trackTotal = 13,
                    discNumber = 1,
                    discTotal = 1,
                    isrc = "USQX91300108",
                    upc = "886443919266"
                )
            )

            assertTrue(result.success)
            val raw = output.readBytes().toString(StandardCharsets.ISO_8859_1)
            assertTrue("Should contain composer text", raw.contains("Thomas Bangalter"))
            assertTrue("Should contain copyright text", raw.contains("Daft Life Limited"))
            assertTrue("Should contain ISRC", raw.contains("USQX91300108"))
            assertTrue("Should contain UPC", raw.contains("886443919266"))
        } finally {
            input.delete()
            output.delete()
        }
    }

    private fun atom(type: String, payload: ByteArray): ByteArray {
        val size = payload.size + 8
        val out = java.io.ByteArrayOutputStream(size)
        out.write((size ushr 24) and 0xFF)
        out.write((size ushr 16) and 0xFF)
        out.write((size ushr 8) and 0xFF)
        out.write(size and 0xFF)
        out.write(type.toByteArray(StandardCharsets.US_ASCII))
        out.write(payload)
        return out.toByteArray()
    }
}

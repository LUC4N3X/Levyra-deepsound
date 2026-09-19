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
        isrc = isrc,
        explicit = explicit
    )

    private fun sampleAppleCandidate(
        name: String = "Get Lucky",
        artistName: String = "Daft Punk, Pharrell Williams & Nile Rodgers",
        albumName: String = "Random Access Memories",
        durationMs: Long = 369629L,
        isrc: String = "USQX91300108",
        explicit: Boolean = false
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
        trackNumber = 8,
        trackTotal = 13,
        discNumber = 1,
        discTotal = 1,
        isrc = isrc,
        upc = "886443919266",
        copyright = "℗ 2013 Daft Life Limited",
        explicit = explicit,
        canonicalSongUrl = "https://music.apple.com/us/album/get-lucky/617154241?i=617154366",
        canonicalAlbumUrl = "https://music.apple.com/us/album/random-access-memories/617154241",
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/e8/43/5f/e8435ffa-b6b9-b171-40ab-4ff3959ab661/886443919266.jpg/600x600bb.jpg",
        highResArtworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/e8/43/5f/e8435ffa-b6b9-b171-40ab-4ff3959ab661/886443919266.jpg/1400x1400bb.jpg",
        durationMs = durationMs
    )

    @Test
    fun exactIsrcMatchAcceptsWithMaximumConfidence() {
        val reference = sampleTrack()
        val candidate = sampleAppleCandidate()

        val evaluation = AppleMetadataMatcher.evaluate(reference, candidate)

        assertTrue("Exact ISRC should be accepted", evaluation.accepted)
        assertEquals(100, evaluation.confidence)
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
            isrc = "USQX91300108"
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

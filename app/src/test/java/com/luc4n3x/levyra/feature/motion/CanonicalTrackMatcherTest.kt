package com.luc4n3x.levyra.feature.motion

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalTrackMatcherTest {
    @Test
    fun artistMotionIdentityUsesStableBrowseId() {
        assertEquals(
            MotionArtworkIdentityKey.forArtist("Old Name", "UC-artist"),
            MotionArtworkIdentityKey.forArtist("New Name", "UC-artist")
        )
        assertNotEquals(
            MotionArtworkIdentityKey.forArtist("Same Name", "UC-first"),
            MotionArtworkIdentityKey.forArtist("Same Name", "UC-second")
        )
    }

    @Test
    fun exactAlbumMotionIsAccepted() {
        val reference = identity(
            title = "Cenere",
            artist = "Lazza",
            album = "Sirio",
            upc = "0602445691132",
            year = "2022"
        )
        val candidate = albumCandidate(
            title = "Cenere",
            artist = "Lazza",
            album = "Sirio",
            upc = "0602445691132",
            year = "2022"
        )

        val match = CanonicalTrackMatcher.match(reference, candidate)

        assertTrue(match.accepted)
        assertEquals(100, match.score)
    }

    @Test
    fun albumMotionAcceptsPrimaryArtistWhenTrackHasFeaturing() {
        val reference = MotionTrackIdentity(
            title = "Brano",
            artists = listOf("Main Artist", "Guest Artist"),
            album = "Album",
            durationMs = 200_000L,
            isrc = "",
            upc = "",
            year = "",
            trackId = "track",
            albumId = "album"
        )
        val candidate = albumCandidate("Brano", "Main Artist", "Album")

        assertTrue(CanonicalTrackMatcher.match(reference, candidate).accepted)
    }

    @Test
    fun groupedArtistNameWithCommasAndAmpersandIsAccepted() {
        val reference = MotionTrackIdentity(
            title = "September",
            artists = listOf("Earth", "Wind", "Fire"),
            album = "The Best of Earth, Wind & Fire, Vol. 1",
            durationMs = 215_000L,
            isrc = "",
            upc = "",
            year = "1978",
            trackId = "track",
            albumId = "album"
        )
        val candidate = MotionArtworkCandidate(
            provider = "tidal-video-cover",
            scope = MotionArtworkScope.TRACK,
            identity = reference.copy(
                artists = listOf("Earth, Wind & Fire"),
                trackId = "tidal-track"
            ),
            url = "https://resources.tidal.com/videos/aa/bb/cc/dd/ee/1280x1280.mp4",
            mimeType = "video/mp4",
            expiresAtMs = Long.MAX_VALUE
        )

        assertTrue(CanonicalTrackMatcher.match(reference, candidate).accepted)
    }


    @Test
    fun tidalArtistMatchingAcceptsUnsplitGroupNames() {
        assertTrue(
            tidalArtistsCompatible(
                requested = listOf("Earth", "Wind", "Fire"),
                returned = listOf("Earth, Wind & Fire")
            )
        )
    }

    @Test
    fun featuredArtistAloneCannotReplacePrimaryArtist() {
        val reference = MotionTrackIdentity(
            title = "Brano",
            artists = listOf("Main Artist", "Guest Artist"),
            album = "Album",
            durationMs = 200_000L,
            isrc = "",
            upc = "",
            year = "",
            trackId = "track",
            albumId = "album"
        )
        val candidate = albumCandidate("Brano", "Guest Artist", "Album")

        assertFalse(CanonicalTrackMatcher.match(reference, candidate).accepted)
        assertFalse(tidalArtistsCompatible(reference.artists, candidate.identity.artists))
    }

    @Test
    fun soloArtistDoesNotMatchLargerGroupBySharedAlias() {
        assertFalse(
            tidalArtistsCompatible(
                requested = listOf("Earth"),
                returned = listOf("Earth, Wind & Fire")
            )
        )
    }

    @Test
    fun ordinaryWordContainingCoverIsNotRejected() {
        val reference = identity("Discover", "Artist", "Discover")
        val candidate = albumCandidate("Discover", "Artist", "Discover")

        assertTrue(CanonicalTrackMatcher.match(reference, candidate).accepted)
    }

    @Test
    fun differentArtistIsRejected() {
        val reference = identity("Mon Amour", "Annalisa", "E poi siamo finiti nel vortice")
        val candidate = albumCandidate("Mon Amour", "Other Artist", "E poi siamo finiti nel vortice")

        assertFalse(CanonicalTrackMatcher.match(reference, candidate).accepted)
    }

    @Test
    fun unexpectedRemixEditionIsRejected() {
        val reference = identity("Casa Mia", "Ghali", "Pizza Kebab Vol. 1")
        val candidate = albumCandidate("Casa Mia", "Ghali", "Pizza Kebab Vol. 1 Remix")

        assertFalse(CanonicalTrackMatcher.match(reference, candidate).accepted)
    }

    @Test
    fun exactIsrcTrackMotionIsAccepted() {
        val reference = identity(
            title = "Sinceramente",
            artist = "Annalisa",
            album = "E poi siamo finiti nel vortice",
            durationMs = 215_000L,
            isrc = "ITB002400001"
        )
        val candidate = MotionArtworkCandidate(
            provider = "apple-motion",
            scope = MotionArtworkScope.TRACK,
            identity = reference.copy(trackId = "apple-track"),
            url = "https://example.test/motion.m3u8",
            mimeType = "application/x-mpegURL",
            expiresAtMs = Long.MAX_VALUE
        )

        assertTrue(CanonicalTrackMatcher.match(reference, candidate).accepted)
    }

    @Test
    fun conflictingIsrcIsRejected() {
        val reference = identity(
            title = "Sinceramente",
            artist = "Annalisa",
            album = "E poi siamo finiti nel vortice",
            isrc = "ITB002400001"
        )
        val candidate = MotionArtworkCandidate(
            provider = "apple-motion",
            scope = MotionArtworkScope.TRACK,
            identity = reference.copy(isrc = "ITB002499999"),
            url = "https://example.test/motion.m3u8",
            mimeType = "application/x-mpegURL",
            expiresAtMs = Long.MAX_VALUE
        )

        assertFalse(CanonicalTrackMatcher.match(reference, candidate).accepted)
    }

    @Test
    fun identityKeyPrefersIsrcAcrossProviderIds() {
        val first = track(id = "youtube-one", isrc = "ITB002400001")
        val second = track(id = "youtube-two", isrc = "itb002400001")
        val different = track(id = "youtube-two", isrc = "ITB002400002")

        assertEquals(MotionArtworkIdentityKey.create(first), MotionArtworkIdentityKey.create(second))
        assertNotEquals(MotionArtworkIdentityKey.create(first), MotionArtworkIdentityKey.create(different))
    }

    @Test
    fun singleWithoutAlbumMetadataStillAcceptsAlbumMotion() {
        val reference = identity(title = "SWAG MUSIC", artist = "Artie 5ive", album = "")
        val candidate = albumCandidate(title = "SWAG MUSIC", artist = "Artie 5ive", album = "SWAG MUSIC")

        val match = CanonicalTrackMatcher.match(reference, candidate)

        assertTrue(match.accepted)
        assertTrue(match.score >= 84)
    }

    @Test
    fun singleWithPlaceholderAlbumStillAcceptsAlbumMotion() {
        val candidate = albumCandidate(title = "SWAG MUSIC", artist = "Artie 5ive", album = "SWAG MUSIC - Single")
        listOf("YouTube Music", "YouTube Music Video", "YouTube Music Charts", "YouTube").forEach { placeholder ->
            val reference = identity(title = "SWAG MUSIC", artist = "Artie 5ive", album = placeholder)

            val match = CanonicalTrackMatcher.match(reference, candidate)

            assertTrue("rejected for placeholder $placeholder", match.accepted)
        }
    }

    @Test
    fun singleWithoutAlbumMetadataRejectsAnUnrelatedRelease() {
        val reference = identity(title = "SWAG MUSIC", artist = "Artie 5ive", album = "")
        val candidate = albumCandidate(title = "SWAG MUSIC", artist = "Artie 5ive", album = "Greatest Hits")

        val match = CanonicalTrackMatcher.match(reference, candidate)

        assertFalse(match.accepted)
    }

    @Test
    fun singleWithoutAlbumMetadataRejectsAnEditionMismatch() {
        val reference = identity(title = "SWAG MUSIC", artist = "Artie 5ive", album = "")
        val candidate = albumCandidate(title = "SWAG MUSIC", artist = "Artie 5ive", album = "SWAG MUSIC (Remix)")

        val match = CanonicalTrackMatcher.match(reference, candidate)

        assertFalse(match.accepted)
    }

    @Test
    fun realAlbumMetadataStillRequiresTheAlbumToMatch() {
        val reference = identity(title = "Cenere", artist = "Lazza", album = "Sirio")
        val candidate = albumCandidate(title = "Cenere", artist = "Lazza", album = "Cenere")

        val match = CanonicalTrackMatcher.match(reference, candidate)

        assertFalse(match.accepted)
    }

    private fun albumCandidate(
        title: String,
        artist: String,
        album: String,
        upc: String = "",
        year: String = ""
    ): MotionArtworkCandidate = MotionArtworkCandidate(
        provider = "tidal-video-cover",
        scope = MotionArtworkScope.ALBUM,
        identity = identity(title, artist, album, upc = upc, year = year),
        url = "https://resources.tidal.com/videos/aa/bb/cc/dd/ee/1280x1280.mp4",
        mimeType = "video/mp4",
        width = 1280,
        height = 1280,
        expiresAtMs = Long.MAX_VALUE
    )

    private fun identity(
        title: String,
        artist: String,
        album: String,
        durationMs: Long = 210_000L,
        isrc: String = "",
        upc: String = "",
        year: String = ""
    ): MotionTrackIdentity = MotionTrackIdentity(
        title = title,
        artists = listOf(artist),
        album = album,
        durationMs = durationMs,
        isrc = isrc,
        upc = upc,
        year = year,
        trackId = "track-id",
        albumId = "album-id"
    )

    private fun track(id: String, isrc: String): Track = Track(
        id = id,
        title = "Sinceramente",
        artist = "Annalisa",
        album = "E poi siamo finiti nel vortice",
        durationMs = 215_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "youtube",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        isrc = isrc
    )
}

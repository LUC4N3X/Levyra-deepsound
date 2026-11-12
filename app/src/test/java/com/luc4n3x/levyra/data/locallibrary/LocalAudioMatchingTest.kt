package com.luc4n3x.levyra.data.locallibrary

import com.luc4n3x.levyra.data.local.LocalMediaMatchRow
import com.luc4n3x.levyra.data.local.toFavoriteTrackEntity
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.radio.LIVE_RADIO_SOURCE
import com.luc4n3x.levyra.player.queue.queueStoredCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAudioMatchingTest {

    @Test
    fun exactMetadataMatches() {
        val track = online("Bohemian Rhapsody", "Queen", "A Night at the Opera", 354_000L)
        assertEquals(1L, match(track, local(1, "Bohemian Rhapsody", "Queen", "A Night at the Opera", 355_200L)))
    }

    @Test
    fun featuringAccentsAndPunctuationAreNormalized() {
        val track = online("Señorita (feat. Camila Cabello)", "Shawn Mendes, Camila Cabello", "Señorita", 190_000L)
        assertEquals(2L, match(track, local(2, "Senorita", "Shawn Mendes & Camila Cabello", "Senorita", 190_500L)))
        assertEquals(3L, match(online("Don't Stop Me Now", "Queen"), local(3, "Dont Stop Me Now", "Queen")))
        assertEquals(4L, match(online("Rock & Roll", "Led Zeppelin"), local(4, "Rock and Roll", "Led Zeppelin")))
        assertEquals(5L, match(online("Song ft. Guest", "Main Artist"), local(5, "Song", "Main Artist feat. Guest")))
        assertEquals(6L, match(online("Radio Ga Ga", "Queen - Topic"), local(6, "Radio Ga Ga", "Queen")))
        assertEquals(7L, match(online("ＨＥＬＬＯ", "Ａｒｔｉｓｔ"), local(7, "Hello", "Artist")))
    }

    @Test
    fun presentationNoiseIsIgnored() {
        assertEquals(1L, match(online("Song (Official Audio)", "Artist"), local(1, "Song", "Artist")))
        assertEquals(2L, match(online("Song [Official Music Video]", "Artist"), local(2, "Song", "Artist")))
        assertEquals(3L, match(online("Song (Original Mix)", "Artist"), local(3, "Song", "Artist")))
    }

    @Test
    fun differentVersionsNeverMatch() {
        val versions = listOf(
            "Song (Live)", "Song - Live at Wembley", "Song (Remastered 2011)", "Song (Acoustic)",
            "Song (Instrumental)", "Song (Radio Edit)", "Song (Remix)", "Song (Club Mix)", "Song (Sped Up)",
            "Song (Taylor's Version)", "Song (Demo)", "Song (Karaoke Version)", "Song (Clean)", "Song (Reprise)",
            "Song feat. Guest - Live at Wembley", "Song ft. Guest - Remix", "Song (feat. Guest - Live)"
        )
        versions.forEach { version ->
            assertNull(version, match(online(version, "Artist"), local(1, "Song", "Artist")))
            assertNull(version, match(online("Song", "Artist"), local(1, version, "Artist")))
        }
    }

    @Test
    fun sameVersionOnBothSidesMatches() {
        assertEquals(1L, match(online("Song - 2011 Remaster", "Artist"), local(1, "Song (Remastered 2009)", "Artist")))
        assertEquals(2L, match(online("Song (Live)", "Artist"), local(2, "Song - Live", "Artist")))
        assertEquals(3L, match(online("Song (Acoustic Version)", "Artist"), local(3, "Song [Acoustic]", "Artist")))
    }

    @Test
    fun explicitAndCleanAreKeptApart() {
        assertEquals(1L, match(online("Song", "Artist", explicit = true), local(1, "Song", "Artist")))
        assertNull(match(online("Song", "Artist", explicit = true), local(1, "Song (Clean)", "Artist")))
        assertNull(match(online("Song", "Artist"), local(1, "Song (Explicit)", "Artist")))
        assertEquals(2L, match(online("Song (Explicit)", "Artist"), local(2, "Song (Explicit)", "Artist")))
    }

    @Test
    fun durationToleranceDependsOnAlbumEvidence() {
        assertEquals(1L, match(online("Song", "Artist", "Album", 200_000L), local(1, "Song", "Artist", "Album", 202_900L)))
        assertNull(match(online("Song", "Artist", "Album", 200_000L), local(1, "Song", "Artist", "Album", 203_500L)))
        assertEquals(2L, match(online("Song", "Artist", "", 200_000L), local(2, "Song", "Artist", "Album", 201_800L)))
        assertNull(match(online("Song", "Artist", "", 200_000L), local(2, "Song", "Artist", "Album", 202_500L)))
        assertEquals(3L, match(online("Song", "Artist", "Hits", 200_000L), local(3, "Song", "Artist", "Album", 201_400L)))
        assertNull(match(online("Song", "Artist", "Hits", 200_000L), local(3, "Song", "Artist", "Album", 201_600L)))
        assertEquals(4L, match(online("Song", "Artist", "Album (Deluxe Edition)", 200_000L), local(4, "Song", "Artist", "Album", 202_500L)))
    }

    @Test
    fun unknownDurationNeedsAlbumEvidence() {
        assertEquals(1L, match(online("Song", "Artist", "Album", 0L), local(1, "Song", "Artist", "Album", 200_000L)))
        assertNull(match(online("Song", "Artist", "", 0L), local(1, "Song", "Artist", "Album", 200_000L)))
        assertNull(match(online("Song", "Artist", "Album", 200_000L), local(1, "Song", "Artist", "Other", 0L)))
    }

    @Test
    fun differentArtistWithSameTitleIsRejected() {
        assertNull(match(online("Hello", "Adele", "25", 295_000L), local(1, "Hello", "Lionel Richie", "Can't Slow Down", 295_000L)))
        assertNull(match(online("Hello", ""), local(1, "Hello", "Adele")))
        assertNull(match(online("Hello", "Adele", "25", 295_000L), local(2, "Hello", "Cover Band", "25", 295_000L, albumArtist = "Adele")))
    }

    @Test
    fun incompleteLocalMetadataIsHandledConservatively() {
        val track = online("Hello", "Adele", "25", 295_000L)
        assertEquals(1L, match(track, local(1, "Adele - Hello", "<unknown>", "", 296_000L)))
        assertEquals(2L, match(track, local(2, "03 - Adele - Hello", "", "", 296_000L)))
        assertEquals(3L, match(track, local(3, "Hello", "<unknown>", "25", 296_500L)))
        assertNull(match(track, local(4, "Hello", "<unknown>", "", 295_000L)))
        assertNull(match(track, local(5, "Hello", "Unknown Artist", "Other", 295_000L)))
        assertEquals(6L, match(track, local(6, "Hello", "", "25", 295_000L, albumArtist = "Adele")))
        assertNull(match(track, local(7, "Hello", "Various Artists", "", 295_000L)))
    }

    @Test
    fun multilingualTitlesMatchOnlyWhenIdentical() {
        assertEquals(1L, match(online("夜に駆ける", "YOASOBI"), local(1, "夜に駆ける", "YOASOBI")))
        assertEquals(2L, match(online("Группа крови", "Кино"), local(2, "Группа крови", "Кино")))
        assertEquals(3L, match(online("Ölüm", "Sanatçı"), local(3, "Olum", "Sanatci")))
        assertNull(match(online("かき", "Artist"), local(4, "がき", "Artist")))
        assertNull(match(online("Группа крови", "Кино"), local(5, "Gruppa krovi", "Kino")))
        assertNull(match(online("Война", "Artist"), local(6, "Воина", "Artist")))
    }

    @Test
    fun genericTitlesRequireTheSameAlbum() {
        assertNull(match(online("Intro", "Artist", "Album A", 60_000L), local(1, "Intro", "Artist", "Album B", 60_000L)))
        assertEquals(2L, match(online("Intro", "Artist", "Album A", 60_000L), local(2, "Intro", "Artist", "Album A", 61_000L)))
    }

    @Test
    fun levyraTagIdentityWinsOverMetadata() {
        val track = online("Renamed Title", "Someone", id = "dQw4w9WgXcQ")
        assertEquals(9L, match(track, local(1, "Other", "Else"), local(9, "Unrelated", "Else", levyraTrackId = "dQw4w9WgXcQ")))
    }

    @Test
    fun selectionIsDeterministic() {
        val track = online("Song", "Artist", "Album", 200_000L)
        assertEquals(10L, match(track, local(11, "Song", "Artist", "", 200_100L), local(10, "Song", "Artist", "Album", 202_000L)))
        assertEquals(20L, match(track, local(21, "Song", "Artist", "Album", 200_000L, bitrate = 128_000), local(20, "Song", "Artist", "Album", 200_000L)))
        assertEquals(30L, match(track, local(31, "Song", "Artist", "Album", 200_000L), local(30, "Song", "Artist", "Album", 200_000L)))
    }

    @Test
    fun emptyLibraryFallsBackToStreaming() {
        assertNull(match(online("Song", "Artist")))
        assertNull(match(online("Song", "Artist"), local(1, "Other Song", "Artist")))
    }

    @Test
    fun onlyOnlineTracksAreEligible() {
        assertTrue(eligibleForPreferredLocalAudio(online("Song", "Artist")))
        assertFalse(eligibleForPreferredLocalAudio(online("Song", "Artist").copy(source = "Offline")))
        assertFalse(eligibleForPreferredLocalAudio(online("Song", "Artist").copy(streamUrl = "content://media/external/audio/media/1")))
        assertFalse(eligibleForPreferredLocalAudio(online("Song", "Artist").copy(streamUrl = "file:///sdcard/a.mp3")))
        assertFalse(eligibleForPreferredLocalAudio(online("Song", "Artist").copy(source = "Spotify Samples")))
        assertFalse(
            eligibleForPreferredLocalAudio(online("Station", "Radio", id = "live-radio:abc").copy(source = LIVE_RADIO_SOURCE))
        )
        assertFalse(eligibleForPreferredLocalAudio(online("", "Artist")))
    }

    @Test
    fun substitutedStreamsNeverLeakIntoStoredCopies() {
        val uri = "content://media/external/audio/media/90001"
        val substituted = online("Song", "Artist").withPreferredLocalStream(uri)
        assertEquals(uri, substituted.streamUrl)
        assertEquals("Song", substituted.title)
        assertEquals("YouTube Music", substituted.source)
        assertEquals("", substituted.withoutPreferredLocalStream().streamUrl)
        assertEquals("", substituted.queueStoredCopy().streamUrl)
        assertEquals("", substituted.toFavoriteTrackEntity(0L).streamUrl)
    }

    @Test
    fun failedSubstitutesAreRejectedOnlyForHandedOutStreams() {
        val uri = "content://media/external/audio/media/90004"
        assertFalse(rejectPreferredLocalStream(uri))
        assertFalse(isRejectedPreferredLocalStream(uri))
        online("Song", "Artist").withPreferredLocalStream(uri)
        assertTrue(rejectPreferredLocalStream(uri))
        assertTrue(isRejectedPreferredLocalStream(uri))
    }

    @Test
    fun trackedStreamsAreBounded() {
        val keys = RecentKeys(2)
        keys += "a"
        keys += "b"
        assertTrue("a" in keys)
        keys += "c"
        assertFalse("b" in keys)
        assertTrue("a" in keys)
        assertTrue("c" in keys)
    }

    @Test
    fun genuineLocalTracksKeepTheirStream() {
        val uri = "content://media/external/audio/media/90002"
        online("Song", "Artist").withPreferredLocalStream(uri)
        val localTrack = online("Song", "Artist").copy(source = "Offline", streamUrl = uri)
        assertEquals(uri, localTrack.withoutPreferredLocalStream().streamUrl)
        assertEquals(uri, localTrack.queueStoredCopy().streamUrl)
        val otherLocal = online("Other", "Artist").copy(streamUrl = "content://media/external/audio/media/90003")
        assertEquals(otherLocal.streamUrl, otherLocal.queueStoredCopy().streamUrl)
    }

    @Test
    fun versionCompatibilityTreatsExplicitAsOneWay() {
        assertTrue(localVersionsCompatible(setOf(VERSION_EXPLICIT), emptySet()))
        assertFalse(localVersionsCompatible(emptySet(), setOf(VERSION_EXPLICIT)))
        assertFalse(localVersionsCompatible(setOf("live"), emptySet()))
        assertTrue(localVersionsCompatible(setOf("live", VERSION_EXPLICIT), setOf("live")))
    }

    private fun match(track: Track, vararg rows: LocalMediaMatchRow): Long? =
        findLocalAudioMatch(track, rows.toList())?.id

    private fun online(
        title: String,
        artist: String,
        album: String = "",
        durationMs: Long = 200_000L,
        explicit: Boolean = false,
        id: String = "abcdefghijk"
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        explicit = explicit
    )

    private fun local(
        id: Long,
        title: String,
        artist: String,
        album: String = "",
        durationMs: Long = 200_000L,
        albumArtist: String = "",
        bitrate: Int = 320_000,
        levyraTrackId: String = ""
    ) = LocalMediaMatchRow(
        id = id,
        contentUri = "content://media/external/audio/media/$id",
        title = title,
        artist = artist,
        album = album,
        albumArtist = albumArtist,
        durationMs = durationMs,
        bitrate = bitrate,
        levyraTrackId = levyraTrackId
    )
}

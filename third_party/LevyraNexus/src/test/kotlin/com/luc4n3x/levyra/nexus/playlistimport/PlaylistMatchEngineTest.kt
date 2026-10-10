package com.luc4n3x.levyra.nexus.playlistimport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistMatchEngineTest {
    private fun source(
        title: String,
        artist: String = "The Weeknd",
        durationMs: Long = 200_000L,
        album: String = "",
        isrc: String = "",
        explicit: Boolean? = null,
        directId: String = ""
    ) = ImportedTrackIdentity(0, title, if (artist.isBlank()) emptyList() else listOf(artist), album = album, durationMs = durationMs, isrc = isrc, explicit = explicit, directCatalogId = directId)

    private fun candidate(
        id: String,
        title: String,
        artist: String = "The Weeknd",
        durationMs: Long = 200_000L,
        album: String = "",
        isrc: String = "",
        explicit: Boolean? = null,
        origin: CandidateOrigin = CandidateOrigin.ONLINE,
        kind: CandidateKind = CandidateKind.SONG
    ) = MatchCandidate(id, title, listOf(artist), album, durationMs, isrc, explicit, origin, kind)

    @Test
    fun exactMetadataIsExcellent() {
        val result = PlaylistMatchEngine.evaluate(source("Blinding Lights", album = "After Hours"), candidate("a", "Blinding Lights", album = "After Hours"))
        assertEquals(MatchConfidence.EXCELLENT, result.confidence)
        assertTrue(result.reasons.any { it.signal == MatchSignal.TITLE_EXACT })
        assertTrue(result.reasons.any { it.signal == MatchSignal.ARTIST_EXACT })
    }

    @Test
    fun isrcAndDirectIdAreExact() {
        val isrc = PlaylistMatchEngine.evaluate(source("Song", isrc = "USUM71900001"), candidate("a", "Other title", isrc = "US-UM7-19-00001"))
        assertEquals(MatchConfidence.EXACT, isrc.confidence)
        val direct = PlaylistMatchEngine.evaluate(source("x", directId = "dQw4w9WgXcQ"), candidate("dQw4w9WgXcQ", "Never Gonna Give You Up", "Rick Astley"))
        assertEquals(MatchConfidence.EXACT, direct.confidence)
    }

    @Test
    fun conflictingIsrcNeverAutoAccepts() {
        val result = PlaylistMatchEngine.evaluate(source("Song Title", isrc = "USUM71900001"), candidate("a", "Song Title", isrc = "GBUM71900002"))
        assertFalse(result.confidence.autoAccepted)
    }

    @Test
    fun liveCandidateRejectedForStudioSource() {
        val result = PlaylistMatchEngine.evaluate(source("Blinding Lights"), candidate("a", "Blinding Lights (Live)"))
        assertEquals(MatchConfidence.UNRESOLVED, result.confidence)
        assertTrue(result.reasons.any { it.signal == MatchSignal.VERSION_CONFLICT })
    }

    @Test
    fun liveSourcePrefersLiveCandidate() {
        val outcome = PlaylistMatchEngine.select(
            source("Bohemian Rhapsody - Live at Wembley", "Queen", 360_000L),
            listOf(
                candidate("studio", "Bohemian Rhapsody", "Queen", 355_000L),
                candidate("live", "Bohemian Rhapsody (Live At Wembley Stadium)", "Queen", 361_000L)
            )
        )
        assertEquals("live", outcome.selected?.candidate?.id)
    }

    @Test
    fun wrongVariantsAreRejected() {
        listOf(
            "Blinding Lights (Remix)",
            "Blinding Lights (Acoustic)",
            "Blinding Lights (Karaoke Version)",
            "Blinding Lights (Sped Up)",
            "Blinding Lights (Slowed + Reverb)",
            "Blinding Lights - Instrumental",
            "Blinding Lights (Nightcore)",
            "Blinding Lights (Cover)",
            "Blinding Lights (Extended Mix)",
            "Blinding Lights (Demo)"
        ).forEach { title ->
            val result = PlaylistMatchEngine.evaluate(source("Blinding Lights"), candidate("a", title))
            assertFalse("$title should not auto-accept", result.confidence.autoAccepted)
        }
    }

    @Test
    fun remasterIsSoftDifference() {
        val result = PlaylistMatchEngine.evaluate(
            source("Hey Jude (Remastered 2015)", "The Beatles", 429_000L),
            candidate("a", "Hey Jude", "The Beatles", 429_000L)
        )
        assertTrue(result.confidence.autoAccepted)
        assertTrue(result.reasons.any { it.signal == MatchSignal.VERSION_SOFT_DIFFERENCE })
    }

    @Test
    fun radioEditGoesToReview() {
        val result = PlaylistMatchEngine.evaluate(source("Song Name"), candidate("a", "Song Name (Radio Edit)"))
        assertEquals(MatchConfidence.REVIEW, result.confidence)
    }

    @Test
    fun explicitVersusCleanGoesToReview() {
        val result = PlaylistMatchEngine.evaluate(source("Song Name", explicit = true), candidate("a", "Song Name", explicit = false))
        assertEquals(MatchConfidence.REVIEW, result.confidence)
        assertTrue(result.reasons.any { it.signal == MatchSignal.EXPLICIT_CONFLICT })
    }

    @Test
    fun sameTitleDifferentArtistIsRejected() {
        val result = PlaylistMatchEngine.evaluate(source("Hello", "Adele", 295_000L), candidate("a", "Hello", "Lionel Richie", 250_000L))
        assertEquals(MatchConfidence.UNRESOLVED, result.confidence)
    }

    @Test
    fun featuredArtistsInTitleStillMatch() {
        val result = PlaylistMatchEngine.evaluate(
            source("Save Your Tears (feat. Ariana Grande)", "The Weeknd"),
            candidate("a", "Save Your Tears (with Ariana Grande)", "The Weeknd")
        )
        assertTrue(result.confidence.autoAccepted)
    }

    @Test
    fun featuredOnlyArtistNeedsReview() {
        val result = PlaylistMatchEngine.evaluate(source("Song Name", "Guest"), candidate("a", "Song Name (feat. Guest)", "Main Artist"))
        assertFalse(result.confidence.autoAccepted)
    }

    @Test
    fun durationToleranceDegradesGracefully() {
        val close = PlaylistMatchEngine.evaluate(source("Song Name"), candidate("a", "Song Name", durationMs = 201_500L))
        val drift = PlaylistMatchEngine.evaluate(source("Song Name"), candidate("a", "Song Name", durationMs = 215_000L))
        val far = PlaylistMatchEngine.evaluate(source("Song Name"), candidate("a", "Song Name", durationMs = 290_000L))
        assertTrue(close.confidence.autoAccepted)
        assertTrue(drift.score < close.score)
        assertFalse(far.confidence.autoAccepted)
    }

    @Test
    fun shortTitlesRequireExactCore() {
        val result = PlaylistMatchEngine.evaluate(source("Go", "Artist"), candidate("a", "Go Go", "Artist"))
        assertEquals(MatchConfidence.UNRESOLVED, result.confidence)
    }

    @Test
    fun nonLatinTitlesMatchExactly() {
        val result = PlaylistMatchEngine.evaluate(source("紅蓮華", "LiSA", 239_000L), candidate("a", "紅蓮華", "LiSA", 239_500L))
        assertTrue(result.confidence.autoAccepted)
        val mismatch = PlaylistMatchEngine.evaluate(source("紅蓮華", "LiSA"), candidate("b", "炎", "LiSA"))
        assertEquals(MatchConfidence.UNRESOLVED, mismatch.confidence)
    }

    @Test
    fun punctuationAndAccentsNormalize() {
        val result = PlaylistMatchEngine.evaluate(source("Don't Stop Me Now", "Beyoncé"), candidate("a", "Dont Stop Me Now", "Beyonce"))
        assertTrue(result.confidence.autoAccepted)
    }

    @Test
    fun missingDurationCapsAtGood() {
        val result = PlaylistMatchEngine.evaluate(source("Song Name", durationMs = 0L), candidate("a", "Song Name"))
        assertEquals(MatchConfidence.GOOD, result.confidence)
    }

    @Test
    fun zeroCandidatesAreUnresolved() {
        val outcome = PlaylistMatchEngine.select(source("Song"), emptyList())
        assertNull(outcome.selected)
        assertEquals(MatchConfidence.UNRESOLVED, outcome.confidence)
    }

    @Test
    fun weakCandidateIsNotSelectedButKeptAsAlternative() {
        val outcome = PlaylistMatchEngine.select(source("Song Name", "Artist"), listOf(candidate("a", "Totally Different", "Nobody")))
        assertNull(outcome.selected)
        assertEquals(1, outcome.alternatives.size)
    }

    @Test
    fun ambiguousDifferentRecordingsGoToReview() {
        val outcome = PlaylistMatchEngine.select(
            source("Intro", "The xx", durationMs = 0L),
            listOf(
                candidate("a", "Intro", "The xx", 128_000L),
                candidate("b", "Intro", "The xx", 200_000L)
            )
        )
        assertTrue(outcome.ambiguous)
        assertEquals(MatchConfidence.REVIEW, outcome.confidence)
    }

    @Test
    fun songEntityBeatsUploadedVideo() {
        val outcome = PlaylistMatchEngine.select(
            source("Song Name"),
            listOf(
                candidate("video", "Song Name", kind = CandidateKind.USER_VIDEO),
                candidate("song", "Song Name", kind = CandidateKind.SONG)
            )
        )
        assertEquals("song", outcome.selected?.candidate?.id)
    }

    @Test
    fun smartPrefersExcellentLocalButNotWeakLocal() {
        val identity = source("Song Name", album = "Album")
        val online = candidate("online", "Song Name", album = "Album")
        val goodLocal = candidate("local", "Song Name", album = "Album", origin = CandidateOrigin.LOCAL, kind = CandidateKind.UNKNOWN)
        assertEquals("local", PlaylistMatchEngine.select(identity, listOf(online, goodLocal)).selected?.candidate?.id)
        val weakLocal = candidate("local2", "Song Name (Live)", origin = CandidateOrigin.LOCAL)
        assertEquals("online", PlaylistMatchEngine.select(identity, listOf(online, weakLocal)).selected?.candidate?.id)
        val preferOnline = PlaylistMatchEngine.select(identity, listOf(online, goodLocal), ResolutionPreference.PREFER_ONLINE)
        assertEquals("online", preferOnline.selected?.candidate?.id)
    }

    @Test
    fun youtubeTopicChannelsNormalize() {
        val result = PlaylistMatchEngine.evaluate(source("Song Name", "Artist"), candidate("a", "Song Name", "Artist - Topic"))
        assertTrue(result.confidence.autoAccepted)
    }

    @Test
    fun albumVersusCompilationStillAccepts() {
        val result = PlaylistMatchEngine.evaluate(
            source("Song Name", album = "Original Album"),
            candidate("a", "Song Name", album = "Greatest Hits")
        )
        assertTrue(result.confidence.autoAccepted)
        assertTrue(result.reasons.any { it.signal == MatchSignal.ALBUM_DIFFERENT })
    }
}

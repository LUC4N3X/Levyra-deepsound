package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.AlternativeMatchVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternativeTrackMatcherTest {
    private val matcher = AlternativeTrackMatcher()

    private fun verdict(query: AlternativeTrackQuery, candidate: AlternativeTrackCandidate) =
        matcher.evaluate(query, candidate)

    private fun assertRejected(reason: MatchRejection, query: AlternativeTrackQuery, candidate: AlternativeTrackCandidate) {
        val evaluation = verdict(query, candidate)
        assertEquals(AlternativeMatchVerdict.REJECTED, evaluation.verdict)
        assertEquals(reason, evaluation.rejection)
    }

    @Test
    fun exactSongIsExact() {
        val evaluation = verdict(query(), candidate())
        assertEquals(AlternativeMatchVerdict.EXACT, evaluation.verdict)
        assertEquals(100, evaluation.confidence)
    }

    @Test
    fun titleCaseDifferenceIsHarmless() {
        assertEquals(AlternativeMatchVerdict.EXACT, verdict(query(), candidate(title = "BLINDING LIGHTS")).verdict)
    }

    @Test
    fun punctuationAndQuoteDifferencesAreHarmless() {
        val query = query(title = "Don’t Start Now", artist = "Dua Lipa", album = "Future Nostalgia")
        val evaluation = verdict(query, candidate(title = "Dont Start Now!", primary = listOf("Dua Lipa"), album = "Future Nostalgia"))
        assertEquals(AlternativeMatchVerdict.EXACT, evaluation.verdict)
    }

    @Test
    fun unicodeNormalizationIsHarmless() {
        val query = query(title = "Café", artist = "Beyoncé", album = "Renaissance")
        val evaluation = verdict(query, candidate(title = "Café", primary = listOf("Beyonce"), album = "RENAISSANCE"))
        assertEquals(AlternativeMatchVerdict.EXACT, evaluation.verdict)
    }

    @Test
    fun htmlEntityDifferenceIsHarmless() {
        val query = query(title = "Rock & Roll", artist = "Led Zeppelin", album = "Led Zeppelin IV")
        val evaluation = verdict(query, candidate(title = "Rock &amp; Roll", primary = listOf("Led Zeppelin"), album = "Led Zeppelin IV"))
        assertEquals(AlternativeMatchVerdict.EXACT, evaluation.verdict)
    }

    @Test
    fun durationWithinOneSecondIsExact() {
        assertEquals(AlternativeMatchVerdict.EXACT, verdict(query(), candidate(duration = 201)).verdict)
    }

    @Test
    fun durationPlusThreeIsAcceptedOnlyWithOtherwiseExactIdentity() {
        val exactAlbum = verdict(query(), candidate(duration = 203))
        assertEquals(AlternativeMatchVerdict.HIGH, exactAlbum.verdict)
        assertRejected(MatchRejection.DURATION_OUT_OF_RANGE, query(), candidate(duration = 203, album = "Blinding Lights"))
    }

    @Test
    fun durationPlusFiveIsTheMaximumTolerance() {
        val evaluation = verdict(query(), candidate(duration = 205))
        assertEquals(AlternativeMatchVerdict.HIGH, evaluation.verdict)
        assertEquals(5, evaluation.durationDeltaSeconds)
    }

    @Test
    fun durationPlusEightIsRejected() {
        assertRejected(MatchRejection.DURATION_OUT_OF_RANGE, query(), candidate(duration = 208))
    }

    @Test
    fun durationPlusFifteenIsRejected() {
        assertRejected(MatchRejection.DURATION_OUT_OF_RANGE, query(), candidate(duration = 215))
    }

    @Test
    fun missingDurationCannotBeVerified() {
        assertRejected(MatchRejection.DURATION_UNKNOWN, query(durationMs = 0L), candidate())
        assertRejected(MatchRejection.DURATION_UNKNOWN, query(), candidate(duration = 0))
    }

    @Test
    fun wrongPrimaryArtistIsRejected() {
        assertRejected(MatchRejection.PRIMARY_ARTIST_MISMATCH, query(), candidate(primary = listOf("The Coverbeats")))
    }

    @Test
    fun onlyFeaturedArtistOverlapIsRejected() {
        val query = query(title = "One Kiss", artist = "Dua Lipa", album = "One Kiss", durationMs = 214_000L)
        val evaluation = verdict(
            query,
            candidate(title = "One Kiss", primary = listOf("Calvin Harris"), featured = listOf("Dua Lipa"), album = "One Kiss", duration = 214)
        )
        assertEquals(MatchRejection.FEATURED_ARTIST_ONLY, evaluation.rejection)
    }

    @Test
    fun candidateLedByAnotherArtistIsRejectedEvenWhenPrimaryOverlaps() {
        val query = query(title = "One Kiss", artist = "Dua Lipa", album = "One Kiss", durationMs = 214_000L)
        assertRejected(
            MatchRejection.PRIMARY_ARTIST_MISMATCH,
            query,
            candidate(title = "One Kiss", primary = listOf("Calvin Harris", "Dua Lipa"), album = "One Kiss", duration = 214)
        )
    }

    @Test
    fun sameTitleDifferentArtistIsRejected() {
        assertRejected(
            MatchRejection.PRIMARY_ARTIST_MISMATCH,
            query(title = "Albachiara", artist = "Vasco Rossi", album = "Non siamo mica gli americani!", durationMs = 243_000L),
            candidate(title = "Albachiara", primary = listOf("Studio Sound Group"), album = "Vasco Rossi Backing Tracks", duration = 250)
        )
    }

    @Test
    fun remixIsNotTheOriginal() {
        assertRejected(MatchRejection.VERSION_MISMATCH, query(), candidate(title = "Blinding Lights (Chromatics Remix)"))
    }

    @Test
    fun liveIsNotTheStudioVersion() {
        assertRejected(MatchRejection.VERSION_MISMATCH, query(), candidate(title = "Blinding Lights (Live)"))
        assertRejected(MatchRejection.VERSION_MISMATCH, query(), candidate(title = "Blinding Lights - Live at the Kia Forum"))
    }

    @Test
    fun acousticIsNotTheOriginal() {
        assertRejected(MatchRejection.VERSION_MISMATCH, query(), candidate(title = "Blinding Lights (Acoustic)"))
    }

    @Test
    fun instrumentalIsNotTheOriginal() {
        assertRejected(MatchRejection.VERSION_MISMATCH, query(), candidate(title = "Blinding Lights (Instrumental)"))
    }

    @Test
    fun karaokeIsNotTheOriginal() {
        assertRejected(MatchRejection.VERSION_MISMATCH, query(), candidate(title = "Blinding Lights (Karaoke Version)"))
    }

    @Test
    fun coverIsNotTheOriginal() {
        assertRejected(MatchRejection.VERSION_MISMATCH, query(), candidate(title = "Blinding Lights (Cover)"))
    }

    @Test
    fun spedUpIsNotTheOriginal() {
        assertRejected(MatchRejection.VERSION_MISMATCH, query(), candidate(title = "blinding lights (sped up)"))
        assertRejected(MatchRejection.VERSION_MISMATCH, query(), candidate(title = "Blinding Lights (Nightcore)"))
    }

    @Test
    fun slowedIsNotTheOriginal() {
        assertRejected(MatchRejection.VERSION_MISMATCH, query(), candidate(title = "Blinding Lights (Slowed)"))
    }

    @Test
    fun reverbIsNotTheOriginal() {
        assertRejected(MatchRejection.VERSION_MISMATCH, query(), candidate(title = "Blinding Lights (slowed + reverb)"))
    }

    @Test
    fun radioEditIsNotTheAlbumVersion() {
        assertRejected(MatchRejection.VERSION_MISMATCH, query(), candidate(title = "Blinding Lights (Radio Edit)"))
    }

    @Test
    fun matchingVersionMarkersAreAccepted() {
        val liveQuery = query(title = "Blinding Lights (Live)")
        assertEquals(AlternativeMatchVerdict.EXACT, verdict(liveQuery, candidate(title = "Blinding Lights (Live)")).verdict)
    }

    @Test
    fun explicitAndCleanVersionsAreNotInterchanged() {
        assertRejected(MatchRejection.EXPLICIT_MISMATCH, query(explicit = true), candidate(explicit = false))
        assertRejected(MatchRejection.EXPLICIT_MISMATCH, query(title = "Blinding Lights (Clean)"), candidate(explicit = true))
    }

    @Test
    fun unknownExplicitStateDoesNotReject() {
        assertEquals(AlternativeMatchVerdict.EXACT, verdict(query(explicit = null), candidate(explicit = true)).verdict)
    }

    @Test
    fun strongAlbumMismatchIsRejected() {
        assertRejected(MatchRejection.ALBUM_MISMATCH, query(), candidate(album = "Ultra Gaming Mode Vol.10"))
    }

    @Test
    fun remasteredAlbumConflictIsRejected() {
        val query = query(title = "Bohemian Rhapsody", artist = "Queen", album = "A Night at the Opera", durationMs = 355_000L)
        val evaluation = verdict(
            query,
            candidate(title = "Bohemian Rhapsody", primary = listOf("Queen"), album = "A Night at the Opera (2011 Remaster)", duration = 355)
        )
        assertEquals(MatchRejection.ALBUM_MISMATCH, evaluation.rejection)
        assertEquals(AlbumRelation.REMASTER_CONFLICT, evaluation.albumRelation)
    }

    @Test
    fun remasteredTrackIsNotTheOriginalRecording() {
        val query = query(title = "Bohemian Rhapsody", artist = "Queen", album = "A Night at the Opera", durationMs = 355_000L)
        assertRejected(
            MatchRejection.VERSION_MISMATCH,
            query,
            candidate(title = "Bohemian Rhapsody - Remastered 2011", primary = listOf("Queen"), album = "A Night at the Opera", duration = 355)
        )
    }

    @Test
    fun singleReleaseOfTheSameRecordingIsHighConfidence() {
        val evaluation = verdict(query(), candidate(album = "Blinding Lights"))
        assertEquals(AlternativeMatchVerdict.HIGH, evaluation.verdict)
        assertEquals(AlbumRelation.SINGLE_RELEASE, evaluation.albumRelation)
    }

    @Test
    fun deluxeEditionIsAnEditionVariant() {
        val evaluation = verdict(query(), candidate(album = "After Hours (Deluxe)"))
        assertEquals(AlternativeMatchVerdict.HIGH, evaluation.verdict)
        assertEquals(AlbumRelation.EDITION_VARIANT, evaluation.albumRelation)
    }

    @Test
    fun featuredArtistCreditsAreUnderstood() {
        val exact = verdict(
            query(title = "Levitating (feat. DaBaby)", artist = "Dua Lipa", album = "Future Nostalgia", durationMs = 203_000L),
            candidate(title = "Levitating (feat. DaBaby)", primary = listOf("Dua Lipa"), featured = listOf("DaBaby"), album = "Future Nostalgia", duration = 203)
        )
        assertEquals(AlternativeMatchVerdict.EXACT, exact.verdict)
        val creditedDifferently = verdict(
            query(title = "Levitating", artist = "Dua Lipa, DaBaby", album = "Future Nostalgia", durationMs = 203_000L),
            candidate(title = "Levitating (feat. DaBaby)", primary = listOf("Dua Lipa"), featured = listOf("DaBaby"), album = "Future Nostalgia", duration = 203)
        )
        assertEquals(AlternativeMatchVerdict.HIGH, creditedDifferently.verdict)
    }

    @Test
    fun isrcDisagreementIsRejectedAndAgreementConfirms() {
        assertRejected(MatchRejection.ISRC_MISMATCH, query(isrc = "USUG11904206"), candidate(isrc = "GBAYE0601690"))
        assertEquals(100, verdict(query(isrc = "USUG11904206"), candidate(isrc = "us-ug1-19-04206", album = "Blinding Lights")).confidence)
    }

    @Test
    fun italianTrackOnCompilationAlbumIsRejected() {
        val query = query(title = "Albachiara", artist = "Vasco Rossi", album = "Non siamo mica gli americani!", durationMs = 243_000L)
        assertRejected(
            MatchRejection.ALBUM_MISMATCH,
            query,
            candidate(title = "Albachiara", primary = listOf("Vasco Rossi"), album = "Vasco Rossi", duration = 243)
        )
    }

    @Test
    fun italianAccentedTitleMatches() {
        val query = query(title = "Perché", artist = "Mahmood", album = "Ghettolimpo", durationMs = 180_000L)
        val evaluation = verdict(query, candidate(title = "Perche", primary = listOf("Mahmood"), album = "Ghettolimpo", duration = 180))
        assertEquals(AlternativeMatchVerdict.EXACT, evaluation.verdict)
    }

    @Test
    fun duplicateCandidatesCollapseAndPrefer320() {
        val selection = matcher.select(
            query(),
            listOf(candidate(id = "first", offers320 = false), candidate(id = "second", offers320 = true))
        )
        assertTrue(selection is AlternativeMatchSelection.Accepted)
        assertEquals("second", (selection as AlternativeMatchSelection.Accepted).evaluation.candidate.providerTrackId)
    }

    @Test
    fun ambiguousCandidatesAreRejected() {
        val selection = matcher.select(
            query(durationMs = 201_000L),
            listOf(candidate(id = "a", duration = 200), candidate(id = "b", duration = 202))
        )
        assertEquals(MatchRejection.AMBIGUOUS, (selection as AlternativeMatchSelection.Rejected).reason)
    }

    @Test
    fun exactAlbumMatchOutranksSingleReleaseOfDifferentCut() {
        val query = query(title = "Levitating (feat. DaBaby)", artist = "Dua Lipa", album = "Future Nostalgia", durationMs = 203_000L)
        val selection = matcher.select(
            query,
            listOf(
                candidate(id = "single", title = "Levitating (feat. DaBaby)", primary = listOf("Dua Lipa"), featured = listOf("DaBaby"), album = "Levitating (feat. DaBaby)", duration = 203, explicit = false),
                candidate(id = "album", title = "Levitating (feat. DaBaby)", primary = listOf("Dua Lipa"), featured = listOf("DaBaby"), album = "Future Nostalgia", duration = 203, explicit = true)
            )
        )
        assertEquals("album", (selection as AlternativeMatchSelection.Accepted).evaluation.candidate.providerTrackId)
    }

    @Test
    fun providerRankingIsNeverTrustedAlone() {
        val selection = matcher.select(
            query(),
            listOf(
                candidate(id = "nightcore", title = "Blinding Lights (Nightcore)", primary = listOf("Nøvacore"), album = "Ultra Gaming Mode Vol.10"),
                candidate(id = "real")
            )
        )
        assertEquals("real", (selection as AlternativeMatchSelection.Accepted).evaluation.candidate.providerTrackId)
    }

    @Test
    fun emptySearchIsRejected() {
        assertEquals(MatchRejection.NO_CANDIDATES, (matcher.select(query(), emptyList()) as AlternativeMatchSelection.Rejected).reason)
    }

    @Test
    fun onlyWrongVersionsAreRejectedWithTheDominantReason() {
        val selection = matcher.select(
            query(),
            listOf(candidate(id = "a", title = "Blinding Lights (Remix)"), candidate(id = "b", title = "Blinding Lights (Live)"))
        )
        assertEquals(MatchRejection.VERSION_MISMATCH, (selection as AlternativeMatchSelection.Rejected).reason)
    }
}

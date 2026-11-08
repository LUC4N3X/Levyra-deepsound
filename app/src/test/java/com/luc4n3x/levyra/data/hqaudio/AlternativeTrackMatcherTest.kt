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
    fun missingExplicitMetadataDoesNotMakeEquivalentRecordingsAmbiguous() {
        val selection = matcher.select(query(explicit = false), listOf(
            candidate(id = "known-clean", explicit = false),
            candidate(id = "unknown-explicit", explicit = null)
        ))
        assertTrue(selection is AlternativeMatchSelection.Accepted)
    }

    @Test
    fun uncensoredEditionIsTakenButCensoredOneNeverReplacesTheExplicitRequest() {
        val selection = matcher.select(query(explicit = false), listOf(candidate(id = "rated-explicit", explicit = true)))
        assertEquals("rated-explicit", (selection as AlternativeMatchSelection.Accepted).evaluation.candidate.providerTrackId)
        val preferred = matcher.select(query(explicit = false), listOf(candidate(id = "clean", explicit = false), candidate(id = "explicit", explicit = true)))
        assertEquals("clean", (preferred as AlternativeMatchSelection.Accepted).evaluation.candidate.providerTrackId)
        assertTrue(matcher.evaluateSelected(query(explicit = false), candidate(explicit = true)).accepted)
        assertTrue(matcher.select(query(explicit = true), listOf(candidate(explicit = false))) is AlternativeMatchSelection.Rejected)
        assertTrue(!matcher.evaluateSelected(query(explicit = true), candidate(explicit = false)).accepted)
    }

    @Test
    fun confirmedExplicitStatusWinsOverUnknownStatusWithHigherBitrate() {
        listOf(false, true).forEach { explicit ->
            val selection = matcher.select(query(explicit = explicit), listOf(
                candidate(id = "confirmed", explicit = explicit).copy(offers320 = false),
                candidate(id = "unknown", explicit = null).copy(offers320 = true)
            )) as AlternativeMatchSelection.Accepted
            assertEquals("confirmed", selection.evaluation.candidate.providerTrackId)
        }
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
    fun remasterSingleMatchesAnUnmarkedCompilationTitleWithTheSameDuration() {
        val query = query(title = "Hotel California", artist = "Eagles", album = "Legacy", durationMs = 391_000L)
        val single = verdict(
            query,
            candidate(title = "Hotel California (2013 Remaster)", primary = listOf("Eagles"), album = "Hotel California (2013 Remaster)", duration = 391)
        )
        val noAlbum = verdict(
            query,
            candidate(title = "Hotel California (2013 Remaster)", primary = listOf("Eagles"), album = "", duration = 391)
        )

        assertEquals(AlternativeMatchVerdict.HIGH, single.verdict)
        assertEquals(AlternativeMatchVerdict.HIGH, noAlbum.verdict)
    }

    @Test
    fun remasterToleranceRequiresSameDurationArtistAndOnlyTheRemasterMarker() {
        val query = query(title = "Hotel California", artist = "Eagles", album = "Legacy", durationMs = 391_000L)
        assertRejected(
            MatchRejection.VERSION_MISMATCH,
            query,
            candidate(title = "Hotel California (2013 Remaster)", primary = listOf("Eagles"), album = "Hotel California (2013 Remaster)", duration = 395)
        )
        assertRejected(
            MatchRejection.VERSION_MISMATCH,
            query,
            candidate(title = "Hotel California (2013 Remaster)", primary = listOf("Eagles"), album = "Their Greatest Hits", duration = 391)
        )
        assertRejected(
            MatchRejection.VERSION_MISMATCH,
            query,
            candidate(title = "Hotel California (Live at the Forum)", primary = listOf("Eagles"), album = "Hotel California (Live at the Forum)", duration = 391)
        )
        assertRejected(
            MatchRejection.VERSION_MISMATCH,
            query,
            candidate(title = "Hotel California - Remastered Live", primary = listOf("Eagles"), album = "Hotel California", duration = 391)
        )
        assertRejected(
            MatchRejection.VERSION_MISMATCH,
            query(title = "Hotel California - Live", artist = "Eagles", album = "Legacy", durationMs = 391_000L),
            candidate(title = "Hotel California (2013 Remaster)", primary = listOf("Eagles"), album = "Hotel California (2013 Remaster)", duration = 391)
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
            query(durationMs = 200_000L),
            listOf(candidate(id = "a", duration = 197), candidate(id = "b", duration = 203))
        )
        assertEquals(MatchRejection.AMBIGUOUS, (selection as AlternativeMatchSelection.Rejected).reason)
    }

    @Test
    fun releasesOfOneRecordingWithSlightlyDifferentLengthsAreNotAmbiguous() {
        val selection = matcher.select(
            query(album = "YouTube Music", durationMs = 202_000L),
            listOf(candidate(id = "album", duration = 200), candidate(id = "single", duration = 204), candidate(id = "deluxe", duration = 204))
        )
        assertTrue(selection is AlternativeMatchSelection.Accepted)
    }

    @Test
    fun soundtrackReissuesTitledFromTheFilmAreTheSameRecording() {
        val query = query(title = "Naattu Koothu", artist = "Rahul Sipligunj", album = "YouTube Music", durationMs = 215_000L)
        val selection = matcher.select(
            query,
            listOf(
                candidate(id = "original", title = "Naattu Koothu", primary = listOf("Rahul Sipligunj"), album = "RRR - Tamil", duration = 214),
                candidate(id = "reissue", title = "Naattu Koothu (From \"Rrr\")", primary = listOf("Rahul Sipligunj"), album = "Top Hits", duration = 214)
            )
        )
        assertEquals("original", (selection as AlternativeMatchSelection.Accepted).evaluation.candidate.providerTrackId)
    }

    @Test
    fun compilationsCreditingExtraPrimaryArtistsAreTheSameRecording() {
        val query = query(title = "Meri Aashiqui", artist = "Palak Muchhal", album = "YouTube Music", durationMs = 266_000L)
        val selection = matcher.select(
            query,
            listOf(
                candidate(id = "KVi55vCq", title = "Meri Aashiqui", primary = listOf("Palak Muchhal", "Arijit Singh"), album = "Rising Star - Palak Muchhal", duration = 266),
                candidate(id = "GTxcx4AR", title = "Meri Aashiqui (From \"Aashiqui 2\")", primary = listOf("Palak Muchhal", "Arijit Singh"), album = "Best Of Palak Muchhal", duration = 266),
                candidate(
                    id = "LQ4Ie1Fm",
                    title = "Meri Aashiqui (From \"Aashiqui 2\")",
                    primary = listOf("Palak Muchhal", "Arijit Singh", "Mithoon", "Irshad Kamil"),
                    album = "Love Forever - Valentine's Day Special",
                    duration = 266
                ).copy(creatorArtists = setOf("Mithoon", "Irshad Kamil"))
            ).map { it.copy(language = "hindi") }
        )
        assertEquals("KVi55vCq", (selection as AlternativeMatchSelection.Accepted).evaluation.candidate.providerTrackId)
    }

    @Test
    fun soloAndDuetReleasesStayAmbiguous() {
        val query = query(title = "Song", artist = "Singer", album = "YouTube Music", durationMs = 200_000L)
        val selection = matcher.select(
            query,
            listOf(
                candidate(id = "solo", title = "Song", primary = listOf("Singer"), album = "One", duration = 200),
                candidate(id = "duet", title = "Song", primary = listOf("Singer", "Partner A"), album = "Two", duration = 200)
            )
        )
        assertEquals(MatchRejection.AMBIGUOUS, (selection as AlternativeMatchSelection.Rejected).reason)
    }

    @Test
    fun everyNearTopPairMustBeTheSameRecording() {
        val query = query(title = "Song", artist = "Singer", album = "YouTube Music", durationMs = 200_000L)
        val selection = matcher.select(
            query,
            listOf(
                candidate(id = "solo", title = "Song", primary = listOf("Singer"), album = "One", duration = 200),
                candidate(id = "with-a", title = "Song", primary = listOf("Singer", "Writer A"), album = "Two", duration = 200)
                    .copy(creatorArtists = setOf("Writer A")),
                candidate(id = "with-b", title = "Song", primary = listOf("Singer", "Writer B"), album = "Three", duration = 200)
                    .copy(creatorArtists = setOf("Writer B"))
            )
        )
        assertEquals(MatchRejection.AMBIGUOUS, (selection as AlternativeMatchSelection.Rejected).reason)
    }

    @Test
    fun composerListedFirstDoesNotHideThePerformingArtist() {
        val query = query(title = "Meri Aashiqui", artist = "Palak Muchhal", album = "YouTube Music", durationMs = 266_000L)
        val composerFirst = candidate(
            id = "OFkbNs2Q",
            title = "Meri Aashiqui",
            primary = listOf("Mithoon", "Palak Muchhal", "Arijit Singh"),
            album = "Sound Of Bollywood - Vol. 2",
            duration = 266
        )
        assertRejected(MatchRejection.PRIMARY_ARTIST_MISMATCH, query, composerFirst)
        val withRoles = verdict(query, composerFirst.copy(nonPerformingArtists = setOf("Mithoon", "Irshad Kamil")))
        assertEquals(AlternativeMatchVerdict.HIGH, withRoles.verdict)
    }

    @Test
    fun composerCreditedAsSourceArtistStillMatches() {
        val query = query(title = "Meri Aashiqui", artist = "Mithoon", album = "YouTube Music", durationMs = 266_000L)
        val candidate = candidate(
            title = "Meri Aashiqui",
            primary = listOf("Mithoon", "Palak Muchhal", "Arijit Singh"),
            album = "Aashiqui 2",
            duration = 266
        ).copy(nonPerformingArtists = setOf("Mithoon"))
        assertTrue(verdict(query, candidate).accepted)
    }

    @Test
    fun sameRecordingPrefersTheEarliestKnownRelease() {
        val query = query(title = "Song", artist = "Singer", album = "YouTube Music", durationMs = 200_000L)
        val selection = matcher.select(
            query,
            listOf(
                candidate(id = "reissue", title = "Song", primary = listOf("Singer"), album = "Hits 2021", duration = 200).copy(releaseYear = 2021),
                candidate(id = "unknown", title = "Song", primary = listOf("Singer"), album = "Hits", duration = 200),
                candidate(id = "original", title = "Song", primary = listOf("Singer"), album = "Debut", duration = 200).copy(releaseYear = 2013)
            )
        )
        assertEquals("original", (selection as AlternativeMatchSelection.Accepted).evaluation.candidate.providerTrackId)
    }

    @Test
    fun differentPrimaryCreditsThatDoNotContainEachOtherStayAmbiguous() {
        val query = query(title = "Song", artist = "Singer", album = "YouTube Music", durationMs = 200_000L)
        val selection = matcher.select(
            query,
            listOf(
                candidate(id = "duet-a", title = "Song", primary = listOf("Singer", "Partner A"), album = "One", duration = 200),
                candidate(id = "duet-b", title = "Song", primary = listOf("Singer", "Partner B"), album = "Two", duration = 200)
            )
        )
        assertEquals(MatchRejection.AMBIGUOUS, (selection as AlternativeMatchSelection.Rejected).reason)
    }

    @Test
    fun dubbedReleasesInDifferentLanguagesStayAmbiguous() {
        val query = query(title = "Srivalli", artist = "Singer", album = "YouTube Music", durationMs = 225_000L)
        val selection = matcher.select(
            query,
            listOf(
                candidate(id = "telugu", title = "Srivalli", primary = listOf("Singer"), album = "Pushpa", duration = 225).copy(language = "telugu"),
                candidate(id = "hindi", title = "Srivalli", primary = listOf("Singer"), album = "Pushpa", duration = 225).copy(language = "hindi")
            )
        )
        assertEquals(MatchRejection.AMBIGUOUS, (selection as AlternativeMatchSelection.Rejected).reason)
    }

    @Test
    fun inconsistentNonIndianLanguageLabelsDoNotSplitTheSameRecording() {
        val query = query(title = "Kelma", artist = "Ramy Sabry", album = "Ghammadt Einy", durationMs = 250_320L)
        val selection = matcher.select(
            query,
            listOf(
                candidate(id = "arabic", title = "Kelma", primary = listOf("Ramy Sabry"), album = "Ghammadt Einy", duration = 250).copy(language = "arabic"),
                candidate(id = "english", title = "Kelma", primary = listOf("Ramy Sabry"), album = "Ghammadt Einy", duration = 249).copy(language = "english")
            )
        )
        assertTrue(selection is AlternativeMatchSelection.Accepted)
    }

    @Test
    fun albumContinuationIsAnEditionOfTheRequestedAlbum() {
        val query = query(title = "CANZONE D'AMORE", artist = "Geolier", album = "TUTTO È POSSIBILE", durationMs = 180_000L)
        val evaluation = verdict(
            query,
            candidate(title = "CANZONE D'AMORE", primary = listOf("Geôlier"), album = "TUTTO È POSSIBILE - ATTO II", duration = 180)
        )
        assertTrue(evaluation.accepted)
        assertEquals(AlbumRelation.EDITION_VARIANT, evaluation.albumRelation)
        assertRejected(
            MatchRejection.ALBUM_MISMATCH,
            query,
            candidate(title = "CANZONE D'AMORE", primary = listOf("Geôlier"), album = "TUTTO", duration = 180)
        )
    }

    @Test
    fun markedLiveRecordingOnCompilationIsAccepted() {
        val query = query(
            title = "Cadeira Cativa - Ao Vivo",
            artist = "Zé Neto & Cristiano",
            album = "Vocês & Deus, Vol. 1 (Ao Vivo no Rio de Janeiro)",
            durationMs = 160_613L
        )
        val compilation = "Zé Neto & Cristiano - Hits 2026 (Ao Vivo)"
        assertTrue(verdict(query, candidate(title = "Cadeira Cativa (Ao Vivo)", primary = listOf("Zé Neto & Cristiano"), album = compilation, duration = 160)).accepted)
        assertRejected(
            MatchRejection.ALBUM_MISMATCH,
            query,
            candidate(title = "Cadeira Cativa (Ao Vivo)", primary = listOf("Zé Neto & Cristiano"), album = compilation, duration = 163)
        )
        assertRejected(
            MatchRejection.ALBUM_MISMATCH,
            query,
            candidate(title = "Cadeira Cativa (Ao Vivo)", primary = listOf("Zé Neto & Cristiano"), album = "Ao Vivo em Goiânia", duration = 160)
        )
        val wembley = query(title = "Song (Live)", artist = "Artist", album = "Live at Wembley", durationMs = 240_000L)
        assertRejected(
            MatchRejection.ALBUM_MISMATCH,
            wembley,
            candidate(title = "Song (Live)", primary = listOf("Artist"), album = "Live at Madison Square Garden", duration = 240)
        )
        assertRejected(
            MatchRejection.VERSION_MISMATCH,
            query.copy(title = "Cadeira Cativa"),
            candidate(title = "Cadeira Cativa (Ao Vivo)", primary = listOf("Zé Neto & Cristiano"), album = compilation, duration = 160)
        )
    }

    @Test
    fun metadataOnlyFeaturedArtistsKeepDifferentRecordingsApart() {
        val selection = matcher.select(
            query(album = "YouTube Music"),
            listOf(
                candidate(id = "with-x", featured = listOf("Artist X")),
                candidate(id = "with-y", featured = listOf("Artist Y"))
            )
        )
        assertEquals(MatchRejection.AMBIGUOUS, (selection as AlternativeMatchSelection.Rejected).reason)
    }

    @Test
    fun featuringInTitleAndInMetadataIsTheSameCredit() {
        val query = query(title = "Levitating (feat. DaBaby)", artist = "Dua Lipa", album = "YouTube Music", durationMs = 203_000L)
        val selection = matcher.select(
            query,
            listOf(
                candidate(id = "title-credit", title = "Levitating (feat. DaBaby)", primary = listOf("Dua Lipa"), album = "Future Nostalgia", duration = 203),
                candidate(id = "both-credits", title = "Levitating (feat. DaBaby)", primary = listOf("Dua Lipa"), featured = listOf("DaBaby"), album = "Levitating", duration = 203)
            )
        )
        assertTrue(selection is AlternativeMatchSelection.Accepted)
    }

    @Test
    fun providerLanguageContradictingTheAlbumLanguageIsRejected() {
        val query = query(title = "Srivalli", artist = "Javed Ali", album = "Pushpa - The Rise (Hindi)", durationMs = 225_000L)
        assertRejected(
            MatchRejection.ALBUM_MISMATCH,
            query,
            candidate(title = "Srivalli", primary = listOf("Javed Ali"), album = "Pushpa - The Rise", duration = 225).copy(language = "telugu")
        )
        val sameLanguage = verdict(
            query,
            candidate(title = "Srivalli", primary = listOf("Javed Ali"), album = "Pushpa - The Rise", duration = 225).copy(language = "hindi")
        )
        assertTrue(sameLanguage.accepted)
        assertEquals(AlbumRelation.SAME, sameLanguage.albumRelation)
        val unknownLanguage = verdict(query, candidate(title = "Srivalli", primary = listOf("Javed Ali"), album = "Pushpa - The Rise", duration = 225))
        assertTrue(unknownLanguage.accepted)
    }

    @Test
    fun differentVersionsAreNeverMergedIntoOneRecording() {
        val selection = matcher.select(
            query(album = "YouTube Music"),
            listOf(candidate(id = "original"), candidate(id = "remix", title = "Blinding Lights (Remix)"))
        )
        assertEquals("original", (selection as AlternativeMatchSelection.Accepted).evaluation.candidate.providerTrackId)
    }

    @Test
    fun unknownAlbumToleratesSmallMasteringDriftOnlyWithExactTitleAndArtists() {
        val query = query(title = "Bohemian Rhapsody", artist = "Queen", album = "YouTube Music", durationMs = 355_000L)
        val exact = verdict(query, candidate(title = "Bohemian Rhapsody", primary = listOf("Queen"), album = "A Night At The Opera", duration = 358))
        assertEquals(AlternativeMatchVerdict.HIGH, exact.verdict)
        assertTrue(exact.confidence < AlternativeTrackMatcher.PERSISTABLE_CONFIDENCE)
        assertRejected(
            MatchRejection.DURATION_OUT_OF_RANGE,
            query,
            candidate(title = "Bohemian Rhapsody (From \"Wayne's World\")", primary = listOf("Queen"), album = "Soundtrack", duration = 358)
        )
        assertRejected(
            MatchRejection.DURATION_OUT_OF_RANGE,
            query,
            candidate(title = "Bohemian Rhapsody", primary = listOf("Queen"), album = "A Night At The Opera", duration = 362)
        )
    }

    @Test
    fun dubbedAlbumInAnotherLanguageIsNeverTheSameAlbum() {
        val query = query(title = "Srivalli", artist = "Javed Ali", album = "Pushpa - The Rise (Hindi)", durationMs = 225_000L)
        assertRejected(
            MatchRejection.ALBUM_MISMATCH,
            query,
            candidate(title = "Srivalli", primary = listOf("Javed Ali"), album = "Pushpa - The Rise (Telugu)", duration = 225)
        )
        val sameLanguage = verdict(query, candidate(title = "Srivalli", primary = listOf("Javed Ali"), album = "Pushpa - The Rise (Hindi)", duration = 225))
        assertEquals(AlbumRelation.SAME, sameLanguage.albumRelation)
    }

    @Test
    fun releaseLanguageSuffixDoesNotMakeTheAlbumDifferent() {
        val query = query(title = "Naatu Naatu", artist = "Rahul Sipligunj", album = "RRR (Original Motion Picture Soundtrack)", durationMs = 214_000L)
        val evaluation = verdict(query, candidate(title = "Naatu Naatu", primary = listOf("Rahul Sipligunj"), album = "RRR - Telugu", duration = 214))
        assertTrue(evaluation.accepted)
        assertEquals(AlbumRelation.EDITION_VARIANT, evaluation.albumRelation)
    }

    @Test
    fun missingExplicitMetadataDoesNotBlockAnOtherwiseVerifiedRecording() {
        val query = query(title = "hate that i made you love me", artist = "Ariana Grande", album = "YouTube Music", durationMs = 198_000L)
        val selection = matcher.select(
            query,
            listOf(
                candidate(id = "single-clean", title = "hate that i made you love me", primary = listOf("Ariana Grande"), album = "hate that i made you love me", duration = 198, explicit = false),
                candidate(id = "single-explicit", title = "hate that i made you love me", primary = listOf("Ariana Grande"), album = "hate that i made you love me", duration = 198, explicit = true),
                candidate(id = "album-clean", title = "hate that i made you love me", primary = listOf("Ariana Grande"), album = "petal", duration = 197, explicit = false),
                candidate(id = "album-explicit", title = "hate that i made you love me", primary = listOf("Ariana Grande"), album = "petal", duration = 197, explicit = true)
            )
        )
        assertTrue(selection is AlternativeMatchSelection.Accepted)
    }

    @Test
    fun anotherAlbumsRatingDoesNotBlockTheRequestedAlbum() {
        val query = query(title = "Without Me", artist = "Eminem", album = "The Eminem Show", durationMs = 290_000L)
        val selection = matcher.select(
            query,
            listOf(
                candidate(id = "clean", title = "Without Me", primary = listOf("Eminem"), album = "The Eminem Show", duration = 290, explicit = false),
                candidate(id = "explicit-compilation", title = "Without Me", primary = listOf("Eminem"), album = "Curtain Call", duration = 290, explicit = true)
            )
        )
        assertEquals("clean", (selection as AlternativeMatchSelection.Accepted).evaluation.candidate.providerTrackId)
    }

    @Test
    fun unspecifiedEditionCanUseTheMatchingRecordingWithATitleRatingHint() {
        val query = query(title = "Without Me", artist = "Eminem", album = "The Eminem Show", durationMs = 290_000L)
        val selection = matcher.select(
            query,
            listOf(
                candidate(id = "explicit", title = "Without Me", primary = listOf("Eminem"), album = "The Eminem Show", duration = 290, explicit = true),
                candidate(id = "clean", title = "Without Me (Clean)", primary = listOf("Eminem"), album = "The Eminem Show", duration = 290, explicit = null)
            )
        )
        assertTrue(selection is AlternativeMatchSelection.Accepted)
    }

    @Test
    fun knownCleanOriginalKeepsTheCleanEdition() {
        val query = query(title = "hate that i made you love me", artist = "Ariana Grande", album = "petal", durationMs = 197_000L, explicit = false)
        val selection = matcher.select(
            query,
            listOf(
                candidate(id = "clean", title = "hate that i made you love me", primary = listOf("Ariana Grande"), album = "petal", duration = 197, explicit = false),
                candidate(id = "explicit", title = "hate that i made you love me", primary = listOf("Ariana Grande"), album = "petal", duration = 197, explicit = true)
            )
        )
        assertEquals("clean", (selection as AlternativeMatchSelection.Accepted).evaluation.candidate.providerTrackId)
    }

    @Test
    fun singleEditionWithUnknownOriginalStateIsStillAccepted() {
        val selection = matcher.select(query(explicit = null), listOf(candidate(id = "only", explicit = false)))
        assertEquals("only", (selection as AlternativeMatchSelection.Accepted).evaluation.candidate.providerTrackId)
    }

    @Test
    fun explicitQueryKeepsTheExplicitEdition() {
        val query = query(title = "hate that i made you love me", artist = "Ariana Grande", album = "petal", durationMs = 197_000L, explicit = true)
        val selection = matcher.select(
            query,
            listOf(
                candidate(id = "clean", title = "hate that i made you love me", primary = listOf("Ariana Grande"), album = "petal", duration = 197, explicit = false),
                candidate(id = "explicit", title = "hate that i made you love me", primary = listOf("Ariana Grande"), album = "petal", duration = 197, explicit = true)
            )
        )
        assertEquals("explicit", (selection as AlternativeMatchSelection.Accepted).evaluation.candidate.providerTrackId)
    }

    @Test
    fun exactAlbumMatchOutranksSingleReleaseOfDifferentCut() {
        val query = query(title = "Levitating (feat. DaBaby)", artist = "Dua Lipa", album = "Future Nostalgia", durationMs = 203_000L)
        val selection = matcher.select(
            query,
            listOf(
                candidate(id = "single", title = "Levitating (feat. DaBaby)", primary = listOf("Dua Lipa"), featured = listOf("DaBaby"), album = "Levitating (feat. DaBaby)", duration = 203, explicit = true),
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
    @Test
    fun missingCreditedCollaboratorIsRejected() {
        assertRejected(
            MatchRejection.PRIMARY_ARTIST_MISMATCH,
            query(title = "One Kiss", artist = "Calvin Harris, Dua Lipa", album = "One Kiss", durationMs = 215_000L),
            candidate(title = "One Kiss", primary = listOf("Calvin Harris"), featured = emptyList(), album = "One Kiss", duration = 215)
        )
    }

    @Test
    fun russianBandDesignatorDoesNotHideTheSameArtist() {
        val evaluation = verdict(
            query(title = "Группа крови", artist = "Группа Кино", album = "Игла Remix", durationMs = 274_000L),
            candidate(id = "Bt-8zakD", title = "Группа крови", primary = listOf("Кино"), album = "Игла Remix", duration = 274)
        )
        assertEquals(AlternativeMatchVerdict.EXACT, evaluation.verdict)
    }

    @Test
    fun cyrillicTrackWithVerifiedDurationMatchesItsJioSaavnRelease() {
        val album = "Виктор Цой и Группа Кино. Полная Дискография"
        val selection = matcher.select(
            query(title = "Группа крови", artist = "Виктор Цой & Группа Кино", album = album, durationMs = 236_000L),
            listOf(
                candidate(id = "20zL7tZU", title = "Группа крови", primary = listOf("Артём Scoop"), album = "Другое кино", duration = 176),
                candidate(id = "wbTqLnmA", title = "Группа крови", primary = listOf("Виктор Цой", "группа Кино"), album = album, duration = 235),
                candidate(id = "pSUfLBwi", title = "Группа крови (Remix)", primary = listOf("Паша Кореец", "DJ Vini"), album = "Последний герой Remix", duration = 394),
                candidate(id = "5Wb0ZbD7", title = "Группа крови (Английская версия)", primary = listOf("Виктор Цой", "группа Кино"), album = album, duration = 285)
            )
        )
        assertEquals("wbTqLnmA", (selection as AlternativeMatchSelection.Accepted).evaluation.candidate.providerTrackId)
    }

    @Test
    fun indianTrackWithPromotionalLyricHookParentheticalMatchesVerifiedAlbumRecording() {
        val query = query(
            title = "Kesariya",
            artist = "Pritam, Arijit Singh, Amitabh Bhattacharya",
            album = "Brahmastra (Original Motion Picture Soundtrack)",
            durationMs = 268_000L,
            explicit = false
        )
        val selection = matcher.select(
            query,
            listOf(
                candidate(
                    id = "EbFWakDs",
                    title = "Kesariya (From \"Brahmastra\") (Tu Fuljhadiyo Wale Tyohaaro Jesi - Happy Diwali)",
                    primary = listOf("Pritam", "Arijit Singh", "Amitabh Bhattacharya"),
                    album = "Kesariya (From \"Brahmastra\") (Tu Fuljhadiyo Wale Tyohaaro Jesi - Happy Diwali)",
                    duration = 268,
                    explicit = false
                ).copy(language = "hindi", releaseYear = 2022),
                candidate(
                    id = "rjkrTnma",
                    title = "Kesariya (Tu Fuljhadiyo Wale Tyohaaro Jesi - Happy Diwali)",
                    primary = listOf("Pritam", "Arijit Singh", "Amitabh Bhattacharya"),
                    album = "Brahmastra",
                    duration = 268,
                    explicit = false
                ).copy(language = "hindi", releaseYear = 2022),
                candidate(
                    id = "lofi",
                    title = "Kesariya (Lofi Flip)",
                    primary = listOf("Pritam", "Arijit Singh", "Amitabh Bhattacharya"),
                    album = "Brahmastra",
                    duration = 268,
                    explicit = false
                ).copy(language = "hindi", releaseYear = 2022),
                candidate(
                    id = "reprise",
                    title = "Kesariya (Reprise)",
                    primary = listOf("Pritam", "Arijit Singh", "Amitabh Bhattacharya"),
                    album = "Brahmastra",
                    duration = 268,
                    explicit = false
                ).copy(language = "hindi", releaseYear = 2022)
            )
        )
        val accepted = selection as AlternativeMatchSelection.Accepted
        assertEquals("rjkrTnma", accepted.evaluation.candidate.providerTrackId)
        assertEquals(
            MatchRejection.VERSION_MISMATCH,
            accepted.evaluations.single { it.candidate.providerTrackId == "lofi" }.rejection
        )
        assertEquals(
            MatchRejection.VERSION_MISMATCH,
            accepted.evaluations.single { it.candidate.providerTrackId == "reprise" }.rejection
        )
    }

    @Test
    fun albumLevelCoverAndKaraokeAndGluedRemixMarkersAreRejected() {
        val oneKiss = query(title = "One Kiss", artist = "Calvin Harris, Dua Lipa", album = "One Kiss", durationMs = 214_000L)
        assertRejected(
            MatchRejection.VERSION_MISMATCH,
            oneKiss,
            candidate(
                id = "D0bh9_V2",
                title = "One Kiss",
                primary = listOf("Calvin Harris", "Dua Lipa"),
                album = "One Kiss (Calvin Harris, Dua Lipa Cover Mix)",
                duration = 214
            )
        )
        assertRejected(
            MatchRejection.VERSION_MISMATCH,
            oneKiss,
            candidate(
                id = "cover-version",
                title = "One Kiss",
                primary = listOf("Calvin Harris", "Dua Lipa"),
                album = "One Kiss (Cover Version)",
                duration = 214
            )
        )
        assertRejected(
            MatchRejection.VERSION_MISMATCH,
            query(title = "Die With A Smile", artist = "Lady Gaga, Bruno Mars", album = "YouTube Music", durationMs = 251_000L),
            candidate(
                id = "karaoke",
                title = "Die With A Smile",
                primary = listOf("Lady Gaga", "Bruno Mars"),
                album = "Pristine Karaoke, Vol. 66",
                duration = 251
            )
        )
        assertRejected(
            MatchRejection.VERSION_MISMATCH,
            query(title = "APT.", artist = "ROSÉ, Bruno Mars", album = "APT.", durationMs = 170_000L),
            candidate(
                id = "AU2KcAhH",
                title = "APT(Dance Rmix)",
                primary = listOf("ROSÉ", "Bruno Mars"),
                album = "APT.",
                duration = 170
            )
        )
    }

    @Test
    fun studioAlbumOutranksCompilationWithSameYearAndBogusFeaturedCredits() {
        val selection = matcher.select(
            query(title = "Tum Hi Ho", artist = "Arijit Singh", album = "YouTube Music", durationMs = 262_000L),
            listOf(
                candidate(
                    id = "compilation",
                    title = "Tum Hi Ho",
                    primary = listOf("Arijit Singh"),
                    album = "Heartbeats - Bollywood Love Songs",
                    duration = 262
                ).copy(releaseYear = 2013, language = "hindi"),
                candidate(
                    id = "studio",
                    title = "Tum Hi Ho",
                    primary = listOf("Arijit Singh"),
                    album = "Aashiqui 2",
                    duration = 262
                ).copy(releaseYear = 2013, language = "hindi")
            )
        )
        assertEquals("studio", (selection as AlternativeMatchSelection.Accepted).evaluation.candidate.providerTrackId)

        val blindingLights = matcher.select(
            query(album = "YouTube Music"),
            listOf(
                candidate(id = "highlights", album = "The Highlights", featured = listOf("Daft Punk")).copy(releaseYear = 2021),
                candidate(id = "deluxe", album = "After Hours (Deluxe)", featured = listOf("Chromatics")).copy(releaseYear = 2020),
                candidate(id = "after-hours", album = "After Hours").copy(releaseYear = 2020)
            )
        )
        assertEquals("after-hours", (blindingLights as AlternativeMatchSelection.Accepted).evaluation.candidate.providerTrackId)
    }

    @Test
    fun singleCharacterTitleAndMultiArtistTyposMatchOnlyWhenConfirmed() {
        val titleTypo = matcher.evaluateReadings(
            query(title = "Kesarya", artist = "Pritam, Arijit Singh", album = "Brahmastra", durationMs = 268_000L),
            candidate(title = "Kesariya", primary = listOf("Pritam", "Arijit Singh"), album = "Brahmastra", duration = 268)
        )
        assertEquals(AlternativeMatchVerdict.HIGH, titleTypo.verdict)

        val multiArtistTypo = matcher.evaluateReadings(
            query(title = "One Kiss", artist = "Calvn Harris, Dua Lipa", album = "One Kiss", durationMs = 214_000L),
            candidate(title = "One Kiss", primary = listOf("Calvin Harris", "Dua Lipa"), album = "One Kiss", duration = 214)
        )
        assertEquals(AlternativeMatchVerdict.HIGH, multiArtistTypo.verdict)

        val sequelNumber = matcher.evaluateReadings(
            query(title = "Symphony 1", artist = "Composer", album = "Symphonies", durationMs = 200_000L),
            candidate(title = "Symphony 2", primary = listOf("Composer"), album = "Symphonies", duration = 200)
        )
        assertEquals(AlternativeMatchVerdict.REJECTED, sequelNumber.verdict)
        assertEquals(MatchRejection.TITLE_MISMATCH, sequelNumber.rejection)
    }

}

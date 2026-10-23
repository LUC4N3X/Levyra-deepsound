package com.luc4n3x.levyra.nexus.playlistimport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistRecordingEvidenceTest {
    @Test
    fun artistTitleUploadFromAnotherChannelIsReadFromTheVideoTitle() {
        val source = ImportedTrackIdentity(0, "The Child", listOf("Out Of Sight"), album = "Wandering Thoughts", durationMs = 227_426)
        val upload = MatchCandidate("upload", "Out Of Sight - The Child (Original Mix)", listOf("MURAT OZSAHIN"),
            durationMs = 228_000, kind = CandidateKind.UNKNOWN)
        val evaluation = PlaylistMatchEngine.evaluate(source, upload)
        assertEquals(MatchConfidence.GOOD, evaluation.confidence)
        assertEquals("Out Of Sight - The Child (Original Mix)", evaluation.candidate.title)
        assertFalse(PlaylistMatchEngine.evaluate(source, upload.copy(durationMs = 236_000)).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, upload.copy(title = "Out Of Sight - The Child (Live)")).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, upload.copy(title = "Other Band - The Child")).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, upload.copy(kind = CandidateKind.SONG)).confidence.autoAccepted)
    }

    @Test
    fun unlabeledUploadWithoutLengthIsProvisionalGood() {
        val source = ImportedTrackIdentity(0, "Tripping Out", listOf("Deelicious"), album = "Neon Disco", durationMs = 372_000)
        val upload = MatchCandidate("upload", "Deelicious - Tripping Out", listOf("MBience"), kind = CandidateKind.USER_VIDEO)
        val evaluation = PlaylistMatchEngine.evaluate(source, upload)
        assertEquals(MatchConfidence.GOOD, evaluation.confidence)
        assertTrue(PlaylistMatchEngine.provisional(evaluation))
        listOf("Deelicious - Tripping Out (Live)", "Deelicious - Tripping Out (Sped Up)", "Deelicious - Tripping Out (Remix)",
            "Deelicious - Tripping Out (Radio Edit)", "Deelicious - Tripping Out @ Club 2022", "Other - Tripping Out").forEach { title ->
            assertFalse(title, PlaylistMatchEngine.evaluate(source, upload.copy(title = title)).confidence.autoAccepted)
        }
        val remix = ImportedTrackIdentity(0, "Mind If I Stay (Astero Remix)", listOf("Kadebostany"), durationMs = 192_000)
        assertFalse(PlaylistMatchEngine.evaluate(remix, upload.copy(title = "Kadebostany - Mind If I Stay (Astero Remix)")).confidence.autoAccepted)
    }

    @Test
    fun sameSongUploadedByDifferentChannelsIsNotAmbiguous() {
        val source = ImportedTrackIdentity(0, "Burattino Telecomandato", listOf("Pupo"), durationMs = 181_000)
        val first = MatchCandidate("first", "Pupo - Burattino Telecomandato", listOf("Gridas1"), durationMs = 181_000)
        val second = MatchCandidate("second", "PUPO - Burattino telecomandato", listOf("Other Channel"), durationMs = 181_500)
        val outcome = PlaylistMatchEngine.select(source, listOf(first, second))
        assertFalse(outcome.ambiguous)
        assertTrue(outcome.confidence.autoAccepted)
        val longer = PlaylistMatchEngine.select(source, listOf(first, second.copy(durationMs = 186_000)))
        assertEquals("first", longer.selected?.candidate?.id)
        assertFalse(PlaylistMatchEngine.evaluate(source, second.copy(durationMs = 186_000)).confidence.autoAccepted)
    }

    @Test
    fun namedRemixUploadWithExactLengthToleratesAMissingCredit() {
        val source = ImportedTrackIdentity(0, "Fou de Toi - Parallells Remix", listOf("Pupkulies & Rebecca", "Parallells", "Guest Singer"),
            durationMs = 371_000)
        val upload = MatchCandidate("upload", "Pupkulies & Rebecca - Fou de Toi (Parallells Remix)", listOf("House Me Up"),
            durationMs = 372_000)
        assertEquals(MatchConfidence.GOOD, PlaylistMatchEngine.evaluate(source, upload).confidence)
        assertFalse(PlaylistMatchEngine.evaluate(source, upload.copy(durationMs = 380_000)).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, upload.copy(durationMs = 0)).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, upload.copy(title = "Pupkulies & Rebecca - Fou de Toi (Other Remix)")).confidence.autoAccepted)
        val unnamed = ImportedTrackIdentity(0, "Fou de Toi", listOf("Pupkulies & Rebecca", "Guest Singer"), durationMs = 371_000)
        assertFalse(PlaylistMatchEngine.evaluate(unnamed, upload.copy(title = "Pupkulies & Rebecca - Fou de Toi")).confidence.autoAccepted)
    }

    @Test
    fun multiArtistUploadTitleKeepsEveryCredit() {
        val source = ImportedTrackIdentity(0, "In the Mood", listOf("Nick Schwenderling", "Tube & Berger"), durationMs = 230_000)
        val upload = MatchCandidate("upload", "Nick Schwenderling, Tube & Berger - In the Mood", listOf("ZEHN Records"),
            durationMs = 231_000, kind = CandidateKind.UNKNOWN)
        assertTrue(PlaylistMatchEngine.evaluate(source, upload).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, upload.copy(title = "Nick Schwenderling - In the Mood")).confidence.autoAccepted)
    }

    @Test
    fun missingSecondaryCreditNeedsTheSameAlbumAndLength() {
        val source = ImportedTrackIdentity(0, "The Sound of Silence - Sos Retter LeBlanc Version",
            listOf("Trinity FM", "Retter LeBlanc"), album = "The Sound of Silence (Sos Retter LeBlanc Version)", durationMs = 197_647)
        val song = MatchCandidate("song", "The Sound of Silence (Sos Retter LeBlanc Version)", listOf("Trinity FM"),
            album = "The Sound of Silence (Sos Retter LeBlanc Version)", durationMs = 198_000, kind = CandidateKind.SONG)
        assertEquals(MatchConfidence.GOOD, PlaylistMatchEngine.evaluate(source, song).confidence)
        assertFalse(PlaylistMatchEngine.evaluate(source, song.copy(album = "")).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, song.copy(durationMs = 206_000)).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, song.copy(kind = CandidateKind.UNKNOWN)).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, song.copy(artists = listOf("Trinity FM", "Guest"), album = "")).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, song.copy(artists = listOf("Guest", "Trinity FM"))).confidence.autoAccepted)
    }

    @Test
    fun uploadsAndReorderedCreditsDoNotMakeTheOfficialTrackAmbiguous() {
        val source = ImportedTrackIdentity(0, "Stars", listOf("Vijay & Sofia Zlatko", "Mauro Valdemi", "Tania Zygar"),
            album = "Stars", durationMs = 172_317)
        val official = MatchCandidate("official", "Stars (feat. Tania Zygar)", listOf("Vijay & Sofia, Mauro Valdemi"),
            album = "Stars", durationMs = 173_000, kind = CandidateKind.SONG)
        val upload = MatchCandidate("upload", "Vijay & Sofia Zlatko & Mauro Valdemi - Stars (ft. Tania Zygar)", listOf("Freetones"),
            durationMs = 173_000)
        val outcome = PlaylistMatchEngine.select(source, listOf(upload, official))
        assertEquals("official", outcome.selected?.candidate?.id)
        assertFalse(outcome.ambiguous)

        val single = MatchCandidate("single", "Labour", listOf("GuitK, Vijay & Sofia Zlatko"), album = "Labour", durationMs = 205_000,
            kind = CandidateKind.SONG)
        val featured = MatchCandidate("featured", "Labour (feat. GuitK)", listOf("Vijay & Sofia Zlatko"), durationMs = 205_000,
            kind = CandidateKind.SONG)
        assertTrue(PlaylistMatchEngine.sameRecording(single, featured))
        assertFalse(PlaylistMatchEngine.sameRecording(single, featured.copy(title = "Labour (Extended Mix) (feat. GuitK)")))
    }

    @Test
    fun greekLookalikeLettersInsideLatinNamesAreFolded() {
        val source = ImportedTrackIdentity(0, "The Owner", listOf("NTEIBINT", "Σtella"), album = "The Owner", durationMs = 233_000)
        val song = MatchCandidate("song", "The Owner", listOf("Stella, NTEIBINT"), album = "The Owner", durationMs = 234_000,
            kind = CandidateKind.SONG)
        assertTrue(PlaylistMatchEngine.evaluate(source, song).confidence.autoAccepted)
        assertEquals("σαν", com.luc4n3x.levyra.nexus.identity.MusicIdentityText.normalize("Σαν"))
    }

    @Test
    fun creditOrderDoesNotMatter() {
        val source = ImportedTrackIdentity(0, "Echoes - Radio Edit", listOf("Oliver Koletzki", "Fran"), durationMs = 198_000)
        val song = MatchCandidate("song", "Echoes (Radio Edit)", listOf("Fran", "Oliver Koletzki"), durationMs = 198_500, kind = CandidateKind.SONG)
        val evaluation = PlaylistMatchEngine.evaluate(source, song)
        assertTrue(evaluation.reasons.any { it.signal == MatchSignal.ARTIST_EXACT })
        assertTrue(evaluation.confidence.autoAccepted)
    }

    @Test
    fun extraCreditOnTheSameAlbumTrackIsTheSameRecording() {
        val source = ImportedTrackIdentity(0, "Bottles & Bay", listOf("Panuma", "Nina Carr"), album = "Bottles & Bay", durationMs = 336_000)
        val song = MatchCandidate("song", "Bottles & Bay", listOf("Panuma", "Lennart Rau", "Nina Carr"),
            album = "Bottles & Bay", durationMs = 336_400, kind = CandidateKind.SONG)
        assertEquals(MatchConfidence.GOOD, PlaylistMatchEngine.evaluate(source, song).confidence)
        assertFalse(PlaylistMatchEngine.evaluate(source, song.copy(album = "Other Album")).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, song.copy(durationMs = 345_000)).confidence.autoAccepted)
        val edition = ImportedTrackIdentity(0, "Full Capacity", listOf("Infinity Ink", "Ali Love"), album = "House Of Infinity", durationMs = 205_700)
        val track = MatchCandidate("track", "Full Capacity", listOf("Infinity Ink"), album = "House Of Infinity (Deluxe)",
            durationMs = 206_000, kind = CandidateKind.SONG)
        assertEquals(MatchConfidence.GOOD, PlaylistMatchEngine.evaluate(edition, track).confidence)
    }

    @Test
    fun guestNamedInTheSourceTitleMustStillBePresent() {
        val source = ImportedTrackIdentity(0, "Song (feat. Guest)", listOf("Main", "Guest"), album = "Album", durationMs = 200_000)
        val solo = MatchCandidate("solo", "Song", listOf("Main"), album = "Album", durationMs = 200_000, kind = CandidateKind.SONG)
        assertFalse(PlaylistMatchEngine.evaluate(source, solo).confidence.autoAccepted)
    }

    @Test
    fun isrcSearchResultMustAgreeWithTheRecording() {
        val source = ImportedTrackIdentity(0, "Hurricane", listOf("Bob Dylan"), durationMs = 512_609, isrc = "USSM17500001")
        val song = MatchCandidate("song", "Hurricane", listOf("Bob Dylan"), durationMs = 513_000, kind = CandidateKind.SONG)
        assertTrue(PlaylistMatchEngine.isrcSearchCorroborates(source, song))
        assertFalse(PlaylistMatchEngine.isrcSearchCorroborates(source, song.copy(title = "Hurricane (Live)")))
        assertFalse(PlaylistMatchEngine.isrcSearchCorroborates(source, song.copy(artists = listOf("Covers Band"))))
        assertFalse(PlaylistMatchEngine.isrcSearchCorroborates(source, song.copy(durationMs = 470_000)))
        assertFalse(PlaylistMatchEngine.isrcSearchCorroborates(source, song.copy(kind = CandidateKind.USER_VIDEO)))
        assertFalse(PlaylistMatchEngine.isrcSearchCorroborates(source.copy(isrc = ""), song))
    }

    @Test
    fun uploadTitleCreditIsProvisionalEvidence() {
        val source = ImportedTrackIdentity(0, "If You Were Me", listOf("Mike D' Jais"), durationMs = 300_000)
        val upload = MatchCandidate("upload", "Mike D' Jais - If You Were Me", listOf("PureDeepMusic"), durationMs = 301_000)
        val evaluation = PlaylistMatchEngine.evaluate(source, upload)
        assertTrue(evaluation.confidence.autoAccepted)
        assertTrue(PlaylistMatchEngine.provisional(evaluation))
        val official = MatchCandidate("official", "If You Were Me", listOf("Mike D' Jais"), durationMs = 300_000, kind = CandidateKind.SONG)
        assertFalse(PlaylistMatchEngine.provisional(PlaylistMatchEngine.evaluate(source, official)))
        assertEquals("official", PlaylistMatchEngine.select(source, listOf(upload, official)).selected?.candidate?.id)
    }

    @Test
    fun closerLengthBreaksATieBetweenSaturatedScores() {
        val source = ImportedTrackIdentity(0, "Puttin' on the Ritz", listOf("Taco"), album = "After Eight", durationMs = 281_000)
        val far = MatchCandidate("far", "Puttin' On The Ritz", listOf("Taco"), album = "After Eight", durationMs = 277_000, kind = CandidateKind.SONG)
        val close = MatchCandidate("close", "Puttin' on the Ritz", listOf("Taco"), album = "After Eight", durationMs = 282_000, kind = CandidateKind.SONG)
        val outcome = PlaylistMatchEngine.select(source, listOf(far, close))
        assertEquals("close", outcome.selected?.candidate?.id)
        assertFalse(outcome.ambiguous)
        val undecided = PlaylistMatchEngine.select(source, listOf(far, close.copy(durationMs = 284_000)))
        assertTrue(undecided.ambiguous)
    }

    @Test
    fun smallSpellingDifferencesInLongTitlesStillMatch() {
        val source = ImportedTrackIdentity(0, "Shala-Lala-La", listOf("Waldeck"), durationMs = 200_000)
        val candidate = MatchCandidate("song", "Shalalala", listOf("Waldeck"), durationMs = 200_500, kind = CandidateKind.SONG)
        assertTrue(PlaylistMatchEngine.evaluate(source, candidate).confidence.autoAccepted)
        val numbered = ImportedTrackIdentity(0, "Symphony No 5", listOf("Orchestra"), durationMs = 200_000)
        assertFalse(PlaylistMatchEngine.evaluate(numbered, candidate.copy(title = "Symphony No 6", artists = listOf("Orchestra"))).confidence.autoAccepted)
    }

    @Test
    fun artistNameAliasNeedsAnotherMatchingCredit() {
        val source = ImportedTrackIdentity(0, "Lily Was Here (feat. Candy Dulfer) - DNA Remix",
            listOf("Dave Stewart", "Candy Dulfer"), durationMs = 267_000)
        val candidate = MatchCandidate("song", "Lily Was Here (DNA Remix) (feat. Candy Dulfer)", listOf("David A. Stewart"),
            durationMs = 268_000, kind = CandidateKind.SONG)
        assertTrue(PlaylistMatchEngine.evaluate(source, candidate).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, candidate.copy(title = "Lily Was Here (DNA Remix)")).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, candidate.copy(artists = listOf("Rod Stewart"))).confidence.autoAccepted)
    }
}

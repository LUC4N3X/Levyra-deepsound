package com.luc4n3x.levyra.nexus.playlistimport

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistRemixerCreditsTest {
    private val source = ImportedTrackIdentity(
        0, "Souvent Longtemps Énormément - Phono Remix", listOf("Diane Tell", "Phono"), durationMs = 363_520
    )
    private val recording = MatchCandidate(
        "recording", "Souvent Longtemps Énormément (Phono Remix)", listOf("Diane Tell"),
        durationMs = 364_000, kind = CandidateKind.SONG
    )

    @Test
    fun remixerCreditedInMatchingVersionTitleCountsAsAnArtistCredit() {
        assertTrue(PlaylistMatchEngine.evaluate(source, recording).confidence.autoAccepted)
        assertTrue(PlaylistMatchEngine.evaluate(source.copy(durationMs = 0), recording).confidence.autoAccepted)
        assertTrue(PlaylistMatchEngine.evaluate(
            source.copy(artists = listOf("Diane Tell")), recording.copy(artists = source.artists)
        ).confidence.autoAccepted)
    }

    @Test
    fun genericNamedVersionDoesNotProveMissingPerformerCredit() {
        val source = ImportedTrackIdentity(0, "The Sound of Silence - Sos Retter LeBlanc Version",
            listOf("Trinity FM", "Retter LeBlanc"), durationMs = 197_647)
        val candidate = MatchCandidate("recording", "The Sound of Silence (Sos Retter LeBlanc Version)",
            listOf("Trinity FM"), durationMs = 198_000, kind = CandidateKind.SONG)
        assertFalse(PlaylistMatchEngine.evaluate(source, candidate).confidence.autoAccepted)
    }

    @Test
    fun matchingNamedMixAndRemixCreditsUseTheSameVersion() {
        val source = ImportedTrackIdentity(0, "I Cant Wait - Freddy Verano Remix",
            listOf("Cocoon", "Freddy Verano"), durationMs = 174_299)
        val candidate = MatchCandidate("recording", "I Can't Wait (Freddy Verano Mix)", listOf("Cocoon"),
            durationMs = 175_000, kind = CandidateKind.SONG)
        assertTrue(PlaylistMatchEngine.evaluate(source, candidate).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, candidate.copy(title = "I Can't Wait (Freddy Verano Extended Mix)")).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, candidate.copy(title = "I Can't Wait (Other Mix)")).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source.copy(artists = listOf("Cocoon", "Verano")), candidate).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source.copy(artists = listOf("Cocoon", "Remix")), candidate).confidence.autoAccepted)
    }

    @Test
    fun differentOrUnnamedRemixAndStudioRecordingStayBelowAutoAccept() {
        listOf("Other Remix", "Remix", "Live", "").forEach { version ->
            val title = "Souvent Longtemps Énormément" + if (version.isEmpty()) "" else " ($version)"
            assertFalse(version, PlaylistMatchEngine.evaluate(source, recording.copy(title = title)).confidence.autoAccepted)
        }
        assertFalse(PlaylistMatchEngine.evaluate(source.copy(title = "Souvent Longtemps Énormément"), recording).confidence.autoAccepted)
    }

    @Test
    fun spacedFeaturedArtistNameKeepsTheSameCredit() {
        val source = ImportedTrackIdentity(0, "You Remain", listOf("Kungs", "RITUAL"), durationMs = 223_800)
        val candidate = MatchCandidate("recording", "You Remain (feat. R I T U A L)", listOf("Kungs"),
            durationMs = 224_000, kind = CandidateKind.SONG)
        assertTrue(PlaylistMatchEngine.evaluate(source, candidate).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, candidate.copy(title = "You Remain (feat. R I T U A L & Guest)")).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, candidate.copy(title = "You Remain (feat. R I T U A)")).confidence.autoAccepted)
    }

    @Test
    fun missingVocalistAndExtraDuetPartnerAreNotExplainedByRemixerCredit() {
        assertFalse(PlaylistMatchEngine.evaluate(source.copy(artists = source.artists + "Guest"), recording).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, recording.copy(artists = recording.artists + "Guest")).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, recording.copy(artists = listOf("Phono"))).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source.copy(artists = listOf("Diane Tell", "Phon")), recording).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source.copy(featuredArtists = listOf("Phono")), recording).confidence.autoAccepted)
    }
}

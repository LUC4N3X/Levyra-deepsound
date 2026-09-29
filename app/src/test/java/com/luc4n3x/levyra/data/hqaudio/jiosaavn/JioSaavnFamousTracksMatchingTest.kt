package com.luc4n3x.levyra.data.hqaudio.jiosaavn

import com.luc4n3x.levyra.data.hqaudio.AlternativeMatchSelection
import com.luc4n3x.levyra.data.hqaudio.AlternativeSearchPlan
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackCandidate
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackMatcher
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackQuery
import com.luc4n3x.levyra.data.hqaudio.MatchRejection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class JioSaavnFamousTracksMatchingTest {
    private val matcher = AlternativeTrackMatcher()

    @Test
    fun rawStarboyResultsParseWithOfficialCredits() {
        val official = starboyCandidates().single { it.providerTrackId == "Paem2Kf1" }
        assertEquals("Starboy", official.title)
        assertEquals(listOf("The Weeknd"), official.primaryArtists)
        assertEquals(listOf("Daft Punk"), official.featuredArtists)
        assertEquals("Starboy", official.album)
        assertEquals(230, official.durationSeconds)
        assertEquals(false, official.explicit)
        assertEquals(true, official.offers320)
        assertEquals(2016, official.releaseYear)
        assertEquals("english", official.language)
    }

    @Test
    fun youtubeMusicStarboyPlansOfficialSearches() {
        assertEquals(
            listOf("Starboy (feat. Daft Punk) The Weeknd", "starboy weeknd"),
            AlternativeSearchPlan.queries(starboy())
        )
    }

    @Test
    fun youtubeMusicStarboySelectsOfficialAlbumRecording() {
        assertAccepted("Paem2Kf1", select(starboy(), starboyCandidates()))
    }

    @Test
    fun explicitStarboySelectsExplicitAlbumRecording() {
        assertAccepted("G5LMYLiK", select(starboy(explicit = true), starboyCandidates()))
    }

    @Test
    fun harmlessArtistCreditDifferencesStillSelectOfficialStarboy() {
        val credits = listOf(
            "The Weeknd, Daft Punk",
            "The Weeknd & Daft Punk",
            "The Weeknd feat. Daft Punk",
            "The Weeknd ft. Daft Punk",
            "THE WEEKND",
            "Weeknd"
        )
        credits.forEach { credit ->
            val selection = select(starboy(title = "Starboy", artist = credit, durationMs = 230_000), starboyCandidates())
            val chosen = accepted(selection, credit)
            assertEquals(credit, "Starboy", chosen.title)
            assertEquals(credit, listOf("The Weeknd"), chosen.primaryArtists)
            assertEquals(credit, 230, chosen.durationSeconds)
        }
    }

    @Test
    fun deluxeReissueOfSameMasterIsAcceptedWhenSourceAlbumIsDeluxe() {
        val chosen = accepted(select(starboy(title = "Starboy", album = "Starboy (Deluxe)"), starboyCandidates()), "deluxe")
        assertEquals("Starboy (Deluxe)", chosen.album)
    }

    @Test
    fun smallDurationDriftKeepsOfficialStarboy() {
        assertAccepted("Paem2Kf1", select(starboy(durationMs = 233_000), starboyCandidates()))
    }

    @Test
    fun officialVideoEditIsNotMatchedToAlbumRecording() {
        val selection = select(
            starboy(title = "Starboy ft. Daft Punk (Official Video)", album = "", durationMs = 274_000),
            starboyCandidates()
        )
        assertRejected(MatchRejection.DURATION_OUT_OF_RANGE, selection)
    }

    @Test
    fun wrongStarboyVersionsAndArtistsAreRejectedForOriginal() {
        val evaluations = select(starboy(), starboyCandidates()).evaluations.associateBy { it.candidate.providerTrackId }
        assertEquals(MatchRejection.VERSION_MISMATCH, evaluations.getValue("YzC9zZTZ").rejection)
        assertEquals(MatchRejection.VERSION_MISMATCH, evaluations.getValue("lG4qCKZ2").rejection)
        assertEquals(MatchRejection.VERSION_MISMATCH, evaluations.getValue("7PgKtCXT").rejection)
        assertEquals(MatchRejection.PRIMARY_ARTIST_MISMATCH, evaluations.getValue("_WBx654g").rejection)
        assertEquals(MatchRejection.TITLE_MISMATCH, evaluations.getValue("w7ms6NSk").rejection)
    }

    @Test
    fun requestedStarboyVersionsSelectOnlyThatVersion() {
        val remix = accepted(
            select(starboy(title = "Starboy (Kygo Remix) (feat. Daft Punk)", durationMs = 245_000, album = ""), starboyCandidates()),
            "remix"
        )
        assertEquals("YzC9zZTZ", remix.providerTrackId)
        val live = accepted(select(starboy(title = "Starboy (Live)", durationMs = 246_000, album = ""), starboyCandidates()), "live")
        assertEquals("lG4qCKZ2", live.providerTrackId)
    }

    @Test
    fun blindingLightsSelectsAlbumRecordingAndRejectsRemixes() {
        val candidates = load("search-blinding-lights-the-weeknd.json")
        val selection = select(
            AlternativeTrackQuery("Blinding Lights", "The Weeknd", "After Hours", 200_000, true),
            candidates
        )
        assertAccepted("fW-Mxsnu", selection)
        val single = accepted(
            select(AlternativeTrackQuery("Blinding Lights", "The Weeknd", "Blinding Lights", 201_000, null), candidates),
            "single"
        )
        assertEquals("Blinding Lights", single.title)
        selection.evaluations
            .filter { it.candidate.title != "Blinding Lights" }
            .forEach { assertEquals(it.candidate.title, MatchRejection.VERSION_MISMATCH, it.rejection) }
    }

    @Test
    fun saveYourTearsSelectsAfterHoursRecordingAndRejectsRemix() {
        val candidates = load("search-save-your-tears-the-weeknd.json")
        val selection = select(
            AlternativeTrackQuery("Save Your Tears", "The Weeknd", "After Hours", 215_000, true),
            candidates
        )
        assertAccepted("XaFAVKX2", selection)
        val unknownAlbum = accepted(
            select(AlternativeTrackQuery("Save Your Tears", "The Weeknd", "YouTube Music", 216_000, null), candidates),
            "unknown album"
        )
        assertEquals("Save Your Tears", unknownAlbum.title)
        assertEquals(
            MatchRejection.VERSION_MISMATCH,
            selection.evaluations.single { it.candidate.providerTrackId == "tlMMxTHp" }.rejection
        )
    }

    private fun starboy(
        title: String = "Starboy (feat. Daft Punk)",
        artist: String = "The Weeknd",
        album: String = "Starboy",
        durationMs: Long = 231_000,
        explicit: Boolean? = null
    ) = AlternativeTrackQuery(title, artist, album, durationMs, explicit)

    private fun starboyCandidates(): List<AlternativeTrackCandidate> = listOf(
        "search-starboy-feat-daft-punk-the-weeknd.json",
        "search-starboy-weeknd.json",
        "search-starboy.json"
    ).flatMap(::load).distinctBy { it.providerTrackId }

    private fun load(name: String): List<AlternativeTrackCandidate> {
        val body = requireNotNull(javaClass.classLoader?.getResource("jiosaavn/$name")) { name }.readText()
        return requireNotNull(JioSaavnPayloadParser.searchCandidates(body)) { name }
    }

    private fun select(query: AlternativeTrackQuery, candidates: List<AlternativeTrackCandidate>) =
        matcher.select(query, candidates)

    private fun accepted(selection: AlternativeMatchSelection, label: String): AlternativeTrackCandidate = when (selection) {
        is AlternativeMatchSelection.Accepted -> selection.evaluation.candidate
        is AlternativeMatchSelection.Rejected -> fail("$label rejected: ${selection.reason}") as Nothing
    }

    private fun assertAccepted(providerTrackId: String, selection: AlternativeMatchSelection) {
        assertEquals(providerTrackId, accepted(selection, providerTrackId).providerTrackId)
    }

    private fun assertRejected(reason: MatchRejection, selection: AlternativeMatchSelection) {
        assertTrue("expected rejection but got $selection", selection is AlternativeMatchSelection.Rejected)
        assertEquals(reason, (selection as AlternativeMatchSelection.Rejected).reason)
    }
}
